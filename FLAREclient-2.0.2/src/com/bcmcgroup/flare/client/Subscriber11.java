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

import com.bcmcgroup.flare.xmldsig.Xmldsig;
import com.bcmcgroup.taxii11.TaxiiUtil;
import com.bcmcgroup.taxii11.TaxiiUtil.PollParameters;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.HashMap;
import java.util.Properties;

import javax.net.ssl.HttpsURLConnection;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.apache.log4j.Logger;

/** 
 * @author		Mark Walters <mwalters@bcmcgroup.com>
 * @version		2.0
 */
public class Subscriber11 {
	private static final Logger logger = Logger.getLogger(Subscriber11.class);

	/**
	 * Helper function to loop through the content blocks (fetch the STIX content) and save each xml document locally
	 * @param filePrefix typically "push" or "poll" to declare how this document was obtained
	 * @param root the root xml element
	 * @param savePath the directory path to save the xml file
	 * @param fileName the file name to save the xml file as [defaults to current time stamp]
	 */
	void fetchStixAndSave(String filePrefix, Element root, String savePath, String fileName) {
		try {
			Date date = new Date();
			NodeList contentBlocks = root.getElementsByTagName("Content_Block");
			Element contentBlock, content;
			String contentBinding;
			Document contentDoc;
			DocumentBuilder db = ClientUtil.generateDocumentBuilder();
			logger.debug(filePrefix + " contentBlocks.getLength(): " + contentBlocks.getLength());
			for (int i = 0; i < contentBlocks.getLength(); i++) {
				// capture STIX document
				contentBlock = (Element) contentBlocks.item(i);
				contentBinding = ((Element)contentBlock.getElementsByTagName("Content_Binding").item(0)).getAttribute("binding_id");
				logger.debug(filePrefix + " contentBinding: " + contentBinding);
				content = (Element) contentBlock.getElementsByTagName("Content").item(0);
				ClientUtil.removeWhitespaceNodes(content);
				content = (Element) content.getFirstChild();
				contentDoc = db.newDocument();
				Node contentImport = contentDoc.importNode(content, true);
				contentDoc.appendChild(contentImport);
				
				// save the STIX document
				TransformerFactory tf = TransformerFactory.newInstance();
				tf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
				Transformer t = tf.newTransformer();
				t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
				if (fileName == null) {
					fileName = String.valueOf(date.getTime());
				}
				t.transform(new DOMSource(contentDoc), new StreamResult(new File(savePath + filePrefix + "_" + fileName + "_" + i + ".xml")));
			}
		} catch (TransformerConfigurationException e) {
			logger.error("fetchStixAndSave TransformerConfigurationException: " + e);
		} catch (TransformerFactoryConfigurationError e) {
			logger.error("fetchStixAndSave TransformerFactoryConfigurationError: " + e);
		} catch (TransformerException e) {
			logger.error("poll TransformerException: " + e.getMessage());
		}
	}

	/**
	 * Helper function to return a String containing the particular requested time stamp value from the supplied document
	 * @param function the calling function for debugging purposes
	 * @param tsType String containing the precise tag name for this time stamp
	 * @param element the root xml element
	 * @return String containing the requested time stamp
	 */
	String getTimestampFromElement(String function, String tsType, Element element) {
		Node node = null;
		String ts = "";
		NodeList nlist = element.getElementsByTagName(tsType);
		logger.debug(function + " nlist: " + nlist);
		if (nlist != null) {
			logger.debug(function + " " + tsType + " nlist.getLength(): " + nlist.getLength());
		}
		if (nlist != null && nlist.getLength() > 0) {
		    node = (Node) nlist.item(0);
		    logger.debug(function + " " + tsType + " node: " + node);
			if (node != null) {
				logger.debug(function + " " + tsType + " node: " + node);
			    ts = node.getNodeValue();
			    if (ts == null) {
			    	node = node.getFirstChild();
			    	ts = node.getNodeValue();
			    }
			}
		}
		return ts;
	}
	
	/**
	 * Returns a String containing a TAXII XML response - the results from the poll, also saves the poll results locally
	 * 
	 * @param msgId the message_id
	 * @param cN the collection_name
	 * @param subId the subscription_id
	 * @param extHdrs a HashMap containing all extended headers (can be null)
	 * @param ebt the Exclusive_Begin_Timestamp parameter
	 * @param iet the Inclusive_End_Timestamp parameter
	 * @param cBind a set of content bindings that the subscriber will accept
	 * @param pollParameters TaxiiUtil.PollParameters object containing all necessary poll parameters required for the poll
	 * @param fileName the base file name to be used for saving each content block from the poll response.
	 * @return String containing the TAXII XML Poll_Response message, null if failure
	 * 
	 * Usage Example: 
	 *   Subscriber11 sub = new Subscriber11();
	 *   String subId = "12345678-90ab-cdef-1234-567890abcdef";
	 *   String ebt = "2014-04-28T23:19:44Z";
	 *   String iet = "2014-04-29T23:19:44Z";
	 *   String pollResults = sub.poll(null, "feed1", subId, null, ebt, iet, null, "myPollResults");
	 */
	public String poll(String msgId, String cN, String subId, HashMap<String,String> extHdrs, String ebt, String iet, PollParameters pollParameters, String fileName) {
		try {
			logger.debug("poll msgId: " + msgId + " cN: " + cN + " subId: " + subId + " extHdrs: " + extHdrs + " ebt: " + ebt + " iet: " + iet + " fileName: " + fileName);
			Properties config = ClientUtil.loadProperties();
			
			// set KeyStore and TrustStore properties
			System.setProperty("javax.net.ssl.keyStore", config.getProperty("pathToSubscriberKeyStore"));
			System.setProperty("javax.net.ssl.keyStorePassword", ClientUtil.decrypt(config.getProperty("subscriberKeyStorePassword")));
			System.setProperty("javax.net.ssl.trustStore", config.getProperty("pathToTrustStore"));
			
			Document taxiiDoc = null;
			if (subId != null) {
				taxiiDoc = TaxiiUtil.pollRequest(msgId, cN, subId, extHdrs, ebt, iet);
			} else {
				taxiiDoc = TaxiiUtil.pollRequest(msgId, cN, extHdrs, ebt, iet, pollParameters);
			}
			TaxiiValidator tv = new TaxiiValidator();
			if (!tv.validate(taxiiDoc)) {
				return null;
			}
			String requestString = ClientUtil.convertDocumentToString(taxiiDoc, true);
			HttpsURLConnection conn = TaxiiUtil.buildConnection(config.getProperty("taxii11serverUrlPoll"));
			if (conn != null) {
				String responseString = ClientUtil.sendPost(conn, requestString);
				Document responseDoc = ClientUtil.convertStringToDocument(responseString);
				if (responseDoc == null) {
					logger.error("poll responseDoc is null, error with obtaining response.");
					return "";
				}
				Boolean verifyStatus = false;
				String verifyDS = config.getProperty("verifyDS");
				if ("true".equalsIgnoreCase(verifyDS)) {
					String ts = config.getProperty("pathToTrustStore");
					String tsPw = ClientUtil.decrypt(config.getProperty("trustStorePassword"));
					String vA = config.getProperty("verifyAlias");
					verifyStatus = Xmldsig.verifySignature(responseDoc, ts, tsPw, vA);
					logger.debug("poll inclusiveEndTimestamp validateStatus: " + verifyStatus);
				} else {
					logger.debug("verifyDS is configured as false or not configured, so no validation of digital signature...");
				}
				
				// when verify true or requires no verification, continue, otherwise discard 					
				if (verifyStatus || verifyDS == null || "false".equalsIgnoreCase(verifyDS)) {
					if (!tv.validate(responseDoc) || !ClientUtil.validateStix(responseDoc)) {
						return null;
					}
					
					// print debug statements, fetch STIX content and save
					if (responseString != null) {
						String savePath = config.getProperty("basePath") + "subscribeFeeds/" + cN + "/";
						logger.debug("poll responseString length: " + responseString.length());
						logger.debug("poll responseString: " + responseString);
						Element responseTaxiiRoot = responseDoc.getDocumentElement();
						String responseIBT = getTimestampFromElement("poll", "Inclusive_Begin_Timestamp", responseTaxiiRoot);
						String responseIET = getTimestampFromElement("poll", "Inclusive_End_Timestamp", responseTaxiiRoot);
						String responseCN = responseTaxiiRoot.getAttribute("collection_name");
						String responseSubId = responseTaxiiRoot.getAttribute("subscription_id");
						logger.debug("poll responseCN: " + responseCN + ", responseSubId: " + responseSubId + ", responseIBT: " + responseIBT + ", responseIET: " + responseIET);
						fetchStixAndSave("poll", responseTaxiiRoot, savePath, fileName);
					}
					return responseString;
				} else {					
					logger.error("Error: XML Digital Signature validation returns false, document is discarded...");
				}
			}
			return "";
		} catch (IOException e) {
			logger.error("IOException e: " + e.getMessage());
			return "";
		} catch (SAXException e) {
			logger.error("SAXException e: " + e.getMessage());
			return "";
		}
	}
	
	/**
	 * Strips off the TAXII portion from the supplied TAXII Inbox_Message Document 
	 * and then saves the content as an XML File to the appropriate collection destination.
	 * 
	 * @param taxiiDoc a TAXII 1.1 compliant Document object
	 * @param fileName file name to save as [default is timestamp]
	 * 
	 * Usage Example:
	 *   Subscriber11 sub = new Subscriber11();
	 *   File taxiiMessage = new File("taxiimsg.xml");
	 *   DocumentBuilder db = ClientUtil.generateDocumentBuilder();
	 *   Document taxiiDoc = db.parse(taxiiMessage);
	 *	 sub.save(taxiiDoc, null);
	 */
	public void save(Document taxiiDoc, String fileName) {
		Properties config = ClientUtil.loadProperties();
		
		// reject if request body isn't valid TAXII
		try {
			TaxiiValidator tv = new TaxiiValidator();
			if (!tv.validate(taxiiDoc)) {
				logger.error("save:  TAXII XML failed to validate!  File was not saved.");
				return;
			} else if (!ClientUtil.validateStix(taxiiDoc)) {
				logger.error("save:  STIX XML content failed to validate!  File was not saved.");
				return;
			}				
			logger.debug("save:  taxiiDoc = " + taxiiDoc);				
		} catch (SAXException e) {
			logger.error("save:  XML failed to validate:  " + e.getMessage());
			return;
		} catch (IllegalArgumentException e) {
			logger.error("save: Message body is null:  " + e.getMessage());
			return;
		} catch (IOException e) {
			logger.error("save: IOException:  " + e.getMessage());
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
			String vA = config.getProperty("verifyAlias");
			if (!Xmldsig.verifySignature(taxiiDoc, ts, tsPw, vA)) {
				logger.error("TAXII server digital signature verification failed, discarding message.");
				return;
			}
		} else {
			logger.debug("verifyDS is configured as false or not configured, so no validation of digital signature ... ");
		}
		
		logger.debug("save taxiiDoc: " + ClientUtil.convertDocumentToString(taxiiDoc, true));
		Element taxiiRoot = taxiiDoc.getDocumentElement();
		String ebt = "", iet = "";
		Element srcSub = (Element) taxiiRoot.getElementsByTagName("Source_Subscription").item(0);
		String savePath = config.getProperty("basePath") + "subscribeFeeds/";
		String subId = "", cN = "";
		if (srcSub != null) {
			cN = srcSub.getAttribute("collection_name");
			savePath = savePath + cN + "/";
			subId = srcSub.getAttribute("subscription_id");
			try {
				ebt = getTimestampFromElement("save", "Exclusive_Begin_Timestamp", srcSub);
				iet = getTimestampFromElement("save", "Inclusive_End_Timestamp", srcSub);
				logger.debug("save cN: " + cN + ", savePath: " + savePath + ", subId: " + subId + ", ebt: " + ebt + ", iet: " + iet);
				fetchStixAndSave("push", taxiiRoot, savePath, fileName);
			} catch (DOMException e) {
				logger.error("DOMException! e: " + e.getMessage());
			}
		}
	}
	
	/**
	 * Strips off the TAXII portion from the supplied TAXII Inbox_Message File 
	 * and then saves the content as an XML File to the appropriate collection destination.
	 * 
	 * @param taxiiMessage a TAXII 1.1 compliant XML file
	 * @param fileName file name to save as [default is timestamp]
	 * @throws IOException 
	 * @throws SAXException 
	 * 
	 * Usage Example:
	 *   Subscriber11 sub = new Subscriber11();
	 *   File taxiiMessage = new File("taxiimsg.xml");
	 *   sub.save(taxiiMessage, null);
	 */
	public void save(File taxiiMessage, String fileName) throws SAXException, IOException {
		DocumentBuilder db = ClientUtil.generateDocumentBuilder();
		save(db.parse(taxiiMessage), fileName);
	}

	/**
	 * Strips off the TAXII portion from the supplied TAXII Inbox_Message String 
	 * and then saves the content as an XML File to the appropriate collection destination.
	 * 
	 * @param taxiiMessage a String containing TAXII 1.1 compliant XML
	 * @param fileName file name to save as [default is timestamp]
	 * @throws SAXException 
	 * @throws IOException 
	 * @throws UnsupportedEncodingException
	 * 
	 *  Usage Example:
	 *    Subscriber11 sub = new Subscriber11();
	 *    String taxiiMessage = "<Inbox_Message>.....</Inbox_Message>";   // this is the TAXII message in string format
	 *    sub.save(taxiiMessage, null);
	 */
	public void save(String taxiiMessage, String fileName) throws UnsupportedEncodingException, SAXException, IOException {
		DocumentBuilder db = ClientUtil.generateDocumentBuilder();
		save(db.parse(new InputSource(new ByteArrayInputStream(taxiiMessage.getBytes("utf-8")))), fileName);
	}
	
	/**
	 * Saves the TAXII Poll_Request with Query parameters locally for FLAREmiddleware to retrieve 
	 * 
	 * @param taxiiDoc a TAXII 1.1 compliant Document object with Query parameters
	 * @param resultId file name to save as
	 */
	public void saveQuery(Document taxiiDoc, String resultId) {
		try {
			Properties config = ClientUtil.loadProperties();
			logger.debug("saveQuery taxiiDoc: " + ClientUtil.convertDocumentToString(taxiiDoc, true));
			String savePath = config.getProperty("queryPath");
			TransformerFactory tf = TransformerFactory.newInstance();
			tf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
			Transformer t = tf.newTransformer();
			t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
			t.transform(new DOMSource(taxiiDoc), new StreamResult(new File(savePath + "request_" + resultId + ".xml")));
		} catch (TransformerConfigurationException e) {
			logger.debug("saveQuery TransformerConfigurationException e: " + e.getMessage());
		} catch (TransformerException e) {
			logger.debug("saveQuery TransformerException e: " + e.getMessage());
		}
	}
}