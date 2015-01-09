package com.bcmcgroup.flare.client;

/*
Copyright 2014 BCMC

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

import com.bcmcgroup.flare.xml.digsig.XmlDigitalSignatureVerifier;
import com.bcmcgroup.taxii10.StatusMessage;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Properties;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsParameters;
import com.sun.net.httpserver.HttpsServer;
import com.sun.net.httpserver.Headers;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManagerFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 * @author		Mark Walters <mwalters@bcmcgroup.com>
 * @version		2.0
 */
public class Listener10 {
	private static final Logger logger = Logger.getLogger(Listener10.class);
	/** 
	 * @author		Mark Walters <mwalters@bcmcgroup.com>
	 * @version		2.0
	 */
	 public class MyHandler implements HttpHandler {
		public DocumentBuilder db;
		private Properties config = new Properties();
		
		public MyHandler(Properties configParam) throws ParserConfigurationException {
			config = configParam;
	    	db = ClientUtil.generateDocumentBuilder();
		}
		
		/**
		 * Handles incoming http requests
		 * @param t incoming HttpExchange object
		 * @throws IOException
		 */
		public void handle(HttpExchange t) throws IOException {
			
			// set response headers
			Headers responseHeaders = t.getResponseHeaders();
			responseHeaders.add("Accept", config.getProperty("httpHeaderAccept"));
			responseHeaders.add("X-TAXII-Content-Type", "urn:taxii.mitre.org:message:xml:1.0");
			responseHeaders.add("X-TAXII-Protocol", "urn:taxii.mitre.org:protocol:https:1.0");
			responseHeaders.add("X-TAXII-Services", "urn:taxii.mitre.org:services:1.0");
			StatusMessage sM;
			
			// reject if not post method
			String requestMethod = t.getRequestMethod();
			OutputStream responseBody = t.getResponseBody();
			if (!requestMethod.equals("POST")) {
				responseHeaders.add("Allow", "POST");
				t.sendResponseHeaders(405, 0);
				String response = "Request method = " + requestMethod + ", only allow POST method!";
				responseBody.write(response.getBytes());
				responseBody.close();
				return;
			}
						
			// reject if the post does not contain xml
			Headers requestHeaders = t.getRequestHeaders();
			String contentType = requestHeaders.getFirst("Content-Type");
			if (!contentType.startsWith("application/xml")) {
				t.sendResponseHeaders(406, 0);
				responseBody.write("POST request must be XML! Please set the Content-Type header to 'application/xml'".getBytes());
				responseBody.close();
				return;
			}
						
			// reject if request body isn't valid TAXII
			Document taxiiDoc = null;
			try {
				InputStream istream = t.getRequestBody();
				taxiiDoc = db.parse(istream);								
				String taxiiString = ClientUtil.convertDocumentToString(taxiiDoc, true);
				TaxiiValidator tv = new TaxiiValidator();
				if (!tv.validate(taxiiDoc)) {
					sM = new StatusMessage(null, "0", "BAD_MESSAGE", null, "TAXII XML failed to validate!  Please alter the message body to conform with the TAXII 1.0 schema.");
					sM.sendResponse(responseBody, t);
					return;
				} else if (!ClientUtil.validateStix(taxiiDoc)) {
					sM = new StatusMessage(null, "0", "BAD_MESSAGE", null, "STIX XML content failed to validate!  Please alter the message contents to conform with the appropriate STIX schemas.");
					sM.sendResponse(responseBody, t);
					return;
				}
				istream = new ByteArrayInputStream(taxiiString.getBytes("UTF-8"));						
				logger.debug("handle taxiiDoc: " + taxiiDoc);
			} catch (SAXException e) {
				logger.error("XML failed to validate:  " + e.getMessage());
				sM = new StatusMessage(null, "0", "BAD_MESSAGE", null, "SAXException: " + e.getMessage());
				sM.sendResponse(responseBody, t);
				return;
			} catch (IllegalArgumentException e) {
				logger.error("Message body is null:  " + e.getMessage());
				sM = new StatusMessage(null, "0", "BAD_MESSAGE", null, "IllegalArgumentException: " + e.getMessage());
				sM.sendResponse(responseBody, t);
				return;
			}
			
			// fetch message_id for in_response_to in response
			String inResponseTo = taxiiDoc.getDocumentElement().getAttribute("message_id");
			logger.debug("handle inResponseTo: " + inResponseTo);
			
			// reject if digital signature doesn't verify
			String verifyDS = config.getProperty("verifyDS");
			if ("true".equalsIgnoreCase(verifyDS)) {
				String ts = config.getProperty("pathToTrustStore");
				String tsPw = ClientUtil.decrypt(config.getProperty("trustStorePassword"));
				String hA = config.getProperty("hubAlias");
				if (!XmlDigitalSignatureVerifier.verifySignatureEnveloped(taxiiDoc, ts, tsPw, hA)) {
					logger.error("TAXII server digital signature verification failed, discarding message.");
					sM = new StatusMessage(null, inResponseTo, "BAD_MESSAGE", null, "TAXII server digital signature verification failed, discarding message.");
					sM.sendResponse(responseBody, t);
					return;
				}
			} else {
				logger.debug("verifyDS is configured as false or not configured, so no validation of digital signature ... ");
			}
			
			// otherwise, parse TAXII document and save content
			responseHeaders.add("Content-Type", config.getProperty("httpHeaderContentType"));
			Subscriber10 sub = new Subscriber10();
			logger.debug("handle sub saving taxiiDoc: " + taxiiDoc);
			sub.save(taxiiDoc, null);
			sM = new StatusMessage(null, inResponseTo, "SUCCESS", null, null);
			sM.sendResponse(responseBody, t);
			return;
		}
	}

	/**
     * prints usage for errant attempts to run main()
     */
    static void usage() {
        System.err.println("Usage: java Listener10 [-p port]");
        System.exit(-1);
    }

    /**
	 * Runs the HTTPS Listener
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		int PORT = 8000;
		if (args.length != 0) {
			if ((args.length != 2) || (!args[0].equals("-p"))) {
				usage();
			}
			try {
				PORT = Integer.parseInt(args[1]);
				if (PORT < 0 || PORT > 65535) {
					throw new NumberFormatException();
				}
			} catch (NumberFormatException nfe) {
				logger.error("Port must be an integer 0-65535");
				System.exit(-1);
			}
		}

		InputStream trustStoreStream = null;
		InputStream subStoreStream = null;
		try {
			Properties config = ClientUtil.loadProperties();
			// System.setProperty("javax.net.debug", "ssl,handshake"); // use for ssl/handshake debugging
			
			// set KeyStore & TrustStore
			String passphrase = config.getProperty("subscriberKeyStorePassword");			
			KeyStore ks = KeyStore.getInstance("JKS");
			subStoreStream = new FileInputStream(config.getProperty("pathToSubscriberKeyStore"));
			String decryptedPw = ClientUtil.decrypt(passphrase.trim());
			ks.load(subStoreStream, decryptedPw.toCharArray());
			KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
			kmf.init(ks, decryptedPw.toCharArray());
			TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
			KeyStore ts = KeyStore.getInstance("JKS");
	        trustStoreStream = new FileInputStream(config.getProperty("pathToTrustStore"));
			passphrase = config.getProperty("trustStorePassword");
			decryptedPw = ClientUtil.decrypt(passphrase.trim());
			ts.load(trustStoreStream,  decryptedPw.toCharArray());
			tmf.init(ts);
			
			// create SSL context
			final SSLContext sslContext = SSLContext.getInstance("TLS");
			sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
			
			// create and configure server
		    final SSLEngine sslEngine = sslContext.createSSLEngine();
			HttpsServer server = HttpsServer.create(new InetSocketAddress(PORT), 0);
			server.setHttpsConfigurator(new HttpsConfigurator(sslContext) {
				@Override
				public void configure(HttpsParameters params) {
					SSLParameters sslparams = getSSLContext().getDefaultSSLParameters();
					sslparams.setNeedClientAuth(true);
					String[] cipherSuites = sslEngine.getEnabledCipherSuites();
					sslparams.setCipherSuites(cipherSuites);
					sslparams.setProtocols(sslEngine.getEnabledProtocols());					
					params.setSSLParameters(sslparams);
				}
			});
			
			server.createContext(config.getProperty("listenerEndpoint"), new Listener10().new MyHandler(config));
			server.setExecutor(null);
			server.start();
			logger.info("TAXII 1.0 listener started, now waiting for incoming requests. Press CTRL-C to kill.");
		} catch (javax.net.ssl.SSLHandshakeException e) {
			logger.error("SSLHandshakeException! e: " + e.getMessage());
		} catch (IOException e) {
			logger.error("IOException!  ioe: " + e.getMessage());
		} catch (NoSuchAlgorithmException e) {
			logger.error("NoSuchAlgorithmException!  nsae: " + e.getMessage());
		} catch (KeyStoreException e) {
			logger.error("KeyStoreException!  e: " + e.getMessage());
		} catch (CertificateException e) {
			logger.error("CertificateException!  e: " + e.getMessage());
		} catch (UnrecoverableKeyException e) {
			logger.error("UnrecoverableKeyException! e: " + e.getMessage());
		} catch (KeyManagementException e) {
			logger.error("KeyManagementException! e: " + e.getMessage());
		} catch (Exception e) {	
			logger.error("Exception in HttpListener e: " + e.getMessage());	
		} finally {
			try {
				if (trustStoreStream != null) {
					trustStoreStream.close();
				}
			} catch (IOException e) {
				logger.error("IOException closing trustStoreStream, e: " + e.getMessage());
			}
			try {
				if (subStoreStream != null) {
					subStoreStream.close();
				}
			} catch (IOException e) {
				logger.error("IOException closing subStoreStream, e: " + e.getMessage());
			}
		}
	}
}