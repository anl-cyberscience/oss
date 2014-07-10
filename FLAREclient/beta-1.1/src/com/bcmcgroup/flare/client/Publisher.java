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

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLEncoder;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.mitre.stix.validator.SchemaError;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import com.bcmcgroup.flare.xml.digsig.XmlDigitalSignatureGenerator;
import com.bcmcgroup.taxii.ContentBlock;

import javax.net.ssl.HttpsURLConnection;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

/** 
 * @author		Mark Walters <mwalters@bcmcgroup.com>
 * @version		1.1
 */
public class Publisher {
	private static final Logger logger = Logger.getLogger(Publisher.class);
	Properties config = null;
	
	/**
	 * Wraps up a list of content blocks as a TAXII message and publishes
	 * them to the flare TAXII server
	 * @param fN feed name
	 * @param extHdr a HashMap containing all extended headers (can be null)
	 * @param msg string containing optional "Message" element (can be null)
	 * @param cBlocks list of ContentBlock objects, each containing an individual content
	 * block for the TAXII message
	 */
	public void publish(String fN, HashMap<String,String> extHdr, String msg, List<ContentBlock> cBlocks) {	
		InputStream istream = null;
		try {
			if (config == null) {
				config = new Properties();
				istream = new FileInputStream("config.properties");
				config.load(istream);

				// set KeyStore and TrustStore properties
				System.setProperty("javax.net.ssl.keyStore", config.getProperty("pathToPublisherKeyStore"));
				System.setProperty("javax.net.ssl.keyStorePassword",  FlareClientUtil.decrypt(config.getProperty("publisherKeyStorePassword")));
				System.setProperty("javax.net.ssl.trustStore", config.getProperty("pathToTrustStore"));
				//System.setProperty("javax.net.debug", "ssl,handshake");  // use this for debugging ssl/handshake issues
			}
					
			// taxiiDoc setup
			DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
			Document taxiiDoc = docBuilder.newDocument();
			
			// Inbox_Message (root element)
			Element taxiiRoot = taxiiDoc.createElementNS("http://taxii.mitre.org/messages/taxii_xml_binding-1", "Inbox_Message");
			taxiiDoc.appendChild(taxiiRoot);
			String msgId = generateMsgId();
			taxiiRoot.setAttribute("message_id", msgId);
			logger.debug("publish msgId: " + msgId + " fN: " + fN + " extHdr: " + extHdr + " msg: " + msg + " cBlocks count: " + cBlocks.size());
			//taxiiRoot.setAttribute("message_id", UUID.randomUUID().toString());  // will change to UUID for taxii 1.1
			
			// Extended_Headers (optional)
			if (extHdr != null && !extHdr.keySet().isEmpty()) {
				Element eHs = taxiiDoc.createElement("Extended_Headers");
				Element eH = taxiiDoc.createElement("Extended_Header");
				for (String name : extHdr.keySet()) {
					logger.debug("Publish Extended Header: " + name);
					eH.setAttribute("name", name);
					eH.appendChild(taxiiDoc.createTextNode(extHdr.get(name)));
					eHs.appendChild(eH);
				}
				taxiiRoot.appendChild(eHs);
			}
			
			// Message (optional)
			if (msg != null) {
				Element m = taxiiDoc.createElement("Message");
				m.appendChild(taxiiDoc.createTextNode(msg));
				taxiiRoot.appendChild(m);
			}
					
			// Content_Block
			Iterator<ContentBlock> iter = cBlocks.iterator();
			while (iter.hasNext()) {
				iter.next().appendToDocument(taxiiDoc);
			}
			String pks = config.getProperty("pathToPublisherKeyStore");
			String pksPw = FlareClientUtil.decrypt(config.getProperty("publisherKeyStorePassword"));
			String pk = config.getProperty("publisherKeyName");
			String pkPw = FlareClientUtil.decrypt(config.getProperty("publisherKeyPassword"));
			String taxiiContent = FlareClientUtil.convertDocumentToString(taxiiDoc);
			logger.debug("publish taxiiContent: " + taxiiContent);
			Document doc = FlareClientUtil.convertStringToDocument(taxiiContent);
			List<Integer> statusList = XmlDigitalSignatureGenerator.generateXmlDigitalSignatureDetached(doc, pks, pksPw, pk, pkPw);	
			logger.debug("publish generateXmlDigitalSignatureDetached statusList: " + statusList);
			
			boolean overallStatus = true;
			if (statusList != null) {
				for (int status: statusList) {
					if (status != 1) {
						overallStatus = false;
					}
				}
			}
			if (statusList != null && overallStatus) {	
				Transformer transformer = TransformerFactory.newInstance().newTransformer();
				transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
				ByteArrayOutputStream stream = new ByteArrayOutputStream();
				transformer.transform(new DOMSource(doc), new StreamResult(stream));
				String output = stream.toString();

				// validate outgoing TAXII message
				List<SchemaError> errors = FlareClientUtil.validateTaxii(output);
				if (errors != null) {
					int size = errors.size();
					if (size > 0) {
						for (SchemaError error : errors) {
							logger.debug("SchemaError error getCategory: " + error.getCategory());
							logger.debug("SchemaError error getMessage: " + error.getMessage());
						}
						logger.error("Message was not published due to content validation errors!  Please check content and try again.");
						return;
					} else {
						logger.debug("validation result: No Error....error size: " + size);
					}
				} else {
					logger.debug("validation result: No Error....");
				}
	
				// open connection to layer 7 and send post request
				URL layer7 = new URL(config.getProperty("layer7Url") + "inbox/" + URLEncoder.encode(fN, "UTF-8").replace("+", "%20"));
				HttpsURLConnection conn = (HttpsURLConnection) layer7.openConnection();
				conn.setRequestMethod("POST");
				conn.setRequestProperty("User-Agent", config.getProperty("publisherUserAgent"));
				conn.setRequestProperty("Content-Type", config.getProperty("publisherContentType"));
				conn.setRequestProperty("Accept", config.getProperty("publisherAccept"));
				conn.setRequestProperty("X-TAXII-Accept", config.getProperty("publisherXTaxiiAccept"));
				conn.setRequestProperty("X-TAXII-Content-Type", config.getProperty("publisherXTaxiiContentType"));
				conn.setRequestProperty("X-TAXII-Protocol", config.getProperty("publisherXTaxiiProtocol"));
				conn.setRequestProperty("X-TAXII-Services", config.getProperty("publisherXTaxiiServices"));
				conn.setDoOutput(true);
	
				OutputStream outputStream = null;
				DataOutputStream wr = null;
				InputStream is = null;
				String response = "";
				try {
					outputStream = conn.getOutputStream();
					wr = new DataOutputStream(outputStream);
				    wr.write(output.getBytes("UTF-8"));
					wr.flush();				
					is = conn.getInputStream();			
					response = IOUtils.toString(is, "UTF-8");
				} catch (IOException e) {
					 logger.error("publish IOException e: " + e.getMessage());
				} finally {
					if (is != null) {
						try {
						   is.close();
						} catch (IOException e) {
							logger.error("publish IOException is: " + e.getMessage());
						}
						is = null;
					}
					if (outputStream != null) {
						try {
							outputStream.close();
						} catch (IOException e) {
							logger.error("publish IOException outputStream: " + e.getMessage());
						}
						outputStream = null;
					}
					if (wr != null) {
						try {
							wr.close();
						} catch (IOException e) {
							logger.error("publish IOException wr: " + e.getMessage());
						}
						wr = null;
					}
				}
	
				// print http response
				if (response != null) {
					logger.debug("publish response length: " + response.length());
					logger.debug("publish response: " + response);
					if (response.length() != 0) {
						try {
							Document responseDoc = FlareClientUtil.convertStringToDocument(response);
							Element element = responseDoc.getDocumentElement();
							String status = element.getAttributes().getNamedItem("status_type").getNodeValue();
							logger.debug("publish response status: " + status);
						} catch (Exception e) {
							logger.error("XPathExpressionException  e: " + e.getMessage());		
						}
					}
				}
			} else {
				logger.debug("Message not published since digital signature was not generated.  Please check log message for errors.");
			}
		} catch (SAXException se) {
			logger.error("publish SAXException: " + se.getMessage());
			
		} catch (IOException ioe) {
			logger.error("publish IOException: " + ioe.getMessage());
		} catch (ParserConfigurationException pce) {
			logger.error("publish ParserConfigurationException: " + pce.getMessage());
		} catch (TransformerException tfe) {
			logger.error("publish TransformerException: " + tfe.getMessage());
		} finally {
			if (istream != null) {
				try {
				   istream.close();
				} catch (IOException e) {
					logger.error("publish IOException: " + e.getMessage());
				}
				istream = null;
			}
		}
		
	}
	
	/**
	 * Generate a message ID (note this won't guarantee any uniqueness).  
	 * We expect to convert to UUID when TAXII 1.1 is released.
	 * @return a string containing a random integer between 0 and 2,000,000,000
	 */
	private String generateMsgId() {
		SecureRandom r = new SecureRandom();
		return Integer.toString(r.nextInt(2000000000));
	}

}
