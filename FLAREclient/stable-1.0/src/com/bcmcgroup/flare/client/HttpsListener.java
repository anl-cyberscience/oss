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

import com.bcmcgroup.taxii.StatusMessage;

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
import java.util.List;

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
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import org.mitre.stix.validator.SchemaError;

/** 
 * @author		Mark Walters <mwalters@bcmcgroup.com>
 * @version		1.0
 */
public class HttpsListener {
	private static final Logger logger = Logger.getLogger(HttpsListener.class);
	/** 
	 * @author		Mark Walters <mwalters@bcmcgroup.com>
	 * @version		1.0
	 */
	 public class MyHandler implements HttpHandler {
		
		public DocumentBuilderFactory docFactory;
		public DocumentBuilder docBuilder;
		private Properties config = new Properties();
		
		public MyHandler(Properties configParam) {
			config = configParam;
			try {	
				docFactory = DocumentBuilderFactory.newInstance();
				docFactory.setNamespaceAware(true);
				docBuilder = docFactory.newDocumentBuilder();
			} catch (ParserConfigurationException e) {
				logger.error("MyHandler ParserConfigurationException e: " + e);
			}
		}
		
		/**
		 * Handles incoming http requests
		 * @param t incoming HttpExchange object
		 * @throws IOException
		 */
		 public void handle(HttpExchange t) throws IOException {
			 logger.debug("Incoming message...");
			 
			// set response headers
			Headers responseHeaders = t.getResponseHeaders();
			responseHeaders.add("Accept", config.getProperty("subscriberAccept"));
			responseHeaders.add("X-TAXII-Content-Type", config.getProperty("subscriberXTaxiiContentType"));
			responseHeaders.add("X-TAXII-Protocol", config.getProperty("subscriberXTaxiiProtocol"));
			responseHeaders.add("X-TAXII-Services", config.getProperty("subscriberXTaxiiServices"));			
			StatusMessage sM;
			
			// reject if not post method
			String requestMethod = t.getRequestMethod();
			OutputStream responseBody = t.getResponseBody();
			if (!requestMethod.equals("POST")) {
				try {
					responseHeaders.add("Allow", "POST");
					t.sendResponseHeaders(405, 0);
					String response = "Request method = " + requestMethod + ", only allow POST method!";
					responseBody.write(response.getBytes());
					
				} catch (IOException e) {
					logger.error("IOException e:  " + e.getMessage());
				} finally {
					if (responseBody != null) {
						try {
						   responseBody.close();
						} catch (IOException ioe) {
							logger.error("IOException ioe:  " + ioe.getMessage());
						}
					}
				}
				return;
			}
						
			// reject if it's not from expected source
			String remoteAddress = t.getRemoteAddress().getAddress().toString();
			logger.debug("handle remoteAddress: " + remoteAddress);
			if (!remoteAddress.endsWith(config.getProperty("remoteTaxiiIP"))) {
				logger.debug("remote address (" + remoteAddress + ") does not match with remoteTaxiiIP variable (" + config.getProperty("remoteTaxiiIP") + ")");
				t.sendResponseHeaders(403, 0);
				responseBody.write("Requests only accepted from authorized TAXII Servers!".getBytes());
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
						
			// reject if request body isn't valid taxii
			Document taxiiDoc = null;
			try {
				InputStream istream = t.getRequestBody();
				String filteredStr = FlareClientUtil.convertStreamToString(istream);					
				filteredStr = filteredStr.replaceAll("\n|\r", "");	
				filteredStr = filteredStr.trim();
				logger.debug("handle filteredStr: " + filteredStr);
				List<SchemaError> errors = FlareClientUtil.validateTaxii(filteredStr);
				if (errors != null) {
					int size = errors.size();
					if (size > 0) {
						for (SchemaError error : errors) {
							logger.debug("SchemaError error getCategory: " + error.getCategory());
							logger.debug("SchemaError error getMessage: " + error.getMessage());
						}
						logger.error("Error TAXII validation, ignoring payload ...");
						try {
							sM = new StatusMessage(null, "0", "BAD_MESSAGE", null, "XML failed to validate!  Please alter the message body to conform with the TAXII 1.0 schema.");
							DOMSource source = new DOMSource(sM.getDocument().getDocumentElement());
							StreamResult result = new StreamResult(responseBody);
							Transformer transformer = TransformerFactory.newInstance().newTransformer();
							transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
							t.sendResponseHeaders(200, 0);
							transformer.transform(source, result);
						} catch (TransformerConfigurationException ex) {
							logger.error("TransformerConfigurationException ex: " + ex.getMessage());
						} catch (TransformerException ex) {
							logger.error("TransformerException ex: " + ex.getMessage());
						}
						responseBody.close();
						return;
					} else {
						logger.debug("validation result: No Errors");
					}
				}				
				istream = new ByteArrayInputStream(filteredStr.getBytes("UTF-8"));				
				taxiiDoc = docBuilder.parse(istream);	
				logger.debug("handle taxiiDoc: " + taxiiDoc);				
			} catch (SAXException e) {
				logger.error("XML failed to validate:  " + e.getMessage());
				try {
					sM = new StatusMessage(null, "0", "BAD_MESSAGE", null, "XML failed to validate!  Please alter the message body to conform with the TAXII 1.0 schema.");
					DOMSource source = new DOMSource(sM.getDocument().getDocumentElement());
					StreamResult result = new StreamResult(responseBody);
					Transformer transformer = TransformerFactory.newInstance().newTransformer();
					transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
					t.sendResponseHeaders(200, 0);
					transformer.transform(source, result);
				} catch (TransformerConfigurationException ex) {
					logger.error("TransformerConfigurationException ex: " + ex.getMessage());
				} catch (TransformerException ex) {
					logger.error("TransformerException ex: " + ex.getMessage());
				}
				responseBody.close();
				return;
			} catch (IllegalArgumentException e) {
				logger.error("Message body is null:  " + e.getMessage());
				try {
					sM = new StatusMessage(null, "0", "BAD_MESSAGE", null, "Message body is null");
					DOMSource source = new DOMSource(sM.getDocument().getDocumentElement());
					StreamResult result = new StreamResult(responseBody);
					Transformer transformer = TransformerFactory.newInstance().newTransformer();
					transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
					t.sendResponseHeaders(200, 0);
					transformer.transform(source, result);
				} catch (TransformerConfigurationException ex) {
					logger.error("TransformerConfigurationException ex: " + ex.getMessage());
				} catch (TransformerException ex) {
					logger.error("TransformerException ex: " + ex.getMessage());
				}
				responseBody.close();
				return;
			} catch (Exception e) {
				logger.error("Exception occurred:  " + e.getMessage());
			}
			
			
			// otherwise, parse taxii document and save content
			responseHeaders.add("Content-Type", config.getProperty("subscriberContentType"));
			String inResponseTo = taxiiDoc.getDocumentElement().getAttribute("message_id");
			logger.debug("handle inResponseTo: " + inResponseTo);
			
			try {
				Subscriber sub = new Subscriber();
				sub.setProperties(config);
				logger.debug("handle sub saving taxiiDoc: " + taxiiDoc);
				sub.save(taxiiDoc);
				sM = new StatusMessage(null, inResponseTo, "SUCCESS", null, null);
				DOMSource source = new DOMSource(sM.getDocument().getDocumentElement());
				StreamResult result = new StreamResult(responseBody);
				Transformer transformer = TransformerFactory.newInstance().newTransformer();
				transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
				t.sendResponseHeaders(200, 0);
				transformer.transform(source, result);
			} catch (TransformerConfigurationException e) {
				logger.error("TransformerConfigurationException e: " + e.getMessage());
			} catch (TransformerException e) {
				logger.error("TransformerException e: " + e.getMessage());
			}
			responseBody.close();
			return;
		}
	}

	/**
     * prints usage for errant attempts to run main()
     */
    static void usage() {
        System.err.println("Usage: java HttpsListener [-p port]");
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

        InputStream inputStream = null;
		InputStream trustStoreStream = null;
		InputStream subStoreStream = null;
		try {
			// load properties file
			Properties config = new Properties();
			inputStream = new FileInputStream("config.properties");
			config.load(inputStream);
			//System.setProperty("javax.net.debug", "ssl,handshake"); // use for ssl/handshake debugging
			
			// set keystore & truststore
			String passphrase = config.getProperty("subscriberKeyStorePassword");			
			KeyStore ks = KeyStore.getInstance("JKS");
			subStoreStream = new FileInputStream(config.getProperty("pathToSubscriberKeyStore"));
			String decryptedPw = FlareClientUtil.decrypt(passphrase.trim());
						
			ks.load(subStoreStream, decryptedPw.toCharArray());
			KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
			kmf.init(ks, decryptedPw.toCharArray());
			TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
			KeyStore ts = KeyStore.getInstance("JKS");
	        trustStoreStream = new FileInputStream(config.getProperty("pathToTrustStore"));
			
			passphrase = config.getProperty("trustStorePassword");
			decryptedPw = FlareClientUtil.decrypt(passphrase.trim());
			ts.load(trustStoreStream,  decryptedPw.toCharArray());
			tmf.init(ts);
			
			// create ssl context
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
			
			server.createContext(config.getProperty("listenerEndpoint"), new HttpsListener().new MyHandler(config));
			server.setExecutor(null);
			server.start();
		} catch (javax.net.ssl.SSLHandshakeException e) {
			logger.error("SSLHandshakeException! e: " + e.getMessage());
		} catch (IOException ioe) {
			logger.error("IOException!  ioe: " + ioe.getMessage());
		} catch (NoSuchAlgorithmException nsae) {
			logger.error("NoSuchAlgorithmException!  nsae: " + nsae.getMessage());
		} catch (KeyStoreException e) {
			logger.error("KeyStoreException!  e: " + e.getMessage());
		} catch (CertificateException e) {
			logger.error("CertificateException!  e: " + e.getMessage());
		} catch (UnrecoverableKeyException e) {
			logger.error("UnrecoverableKeyException! e: " + e.getMessage());
		} catch (KeyManagementException e) {
			logger.error("KeyManagementException! e: " + e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("Exception in HttpListener e: " + e.getMessage());	
		} finally {
			try {
				if (inputStream != null) {			
					inputStream.close();
					inputStream = null;
				}
				if (trustStoreStream != null) {
					trustStoreStream.close();
					trustStoreStream = null;
				}
				if (subStoreStream != null) {
					subStoreStream.close();
					subStoreStream = null;
				}
			} catch (IOException e) {
				logger.error("IOException e: " + e.getMessage());
			}
		}
	}
}
