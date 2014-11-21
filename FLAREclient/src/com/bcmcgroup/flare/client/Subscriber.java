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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.security.SecureRandom;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import javax.net.ssl.HttpsURLConnection;
import javax.xml.XMLConstants;
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

import org.mitre.stix.validator.SchemaError;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

/** 
 * @author		Mark Walters <mwalters@bcmcgroup.com>
 * @version		1.0
 */
public class Subscriber {
	private static final Logger logger = Logger.getLogger(Subscriber.class);
	private Properties config = null;

	/**
	 * Sets config.properties for this instance.
	 */
	public void setProperties(Properties config) {
		this.config = config;
	}
	
	/**
	 * Load config.properties into memory space
	 */
	public void loadProperties() {
		InputStream inputStream = null;
		config = new Properties();
		try {
			inputStream = new FileInputStream("config.properties");
			config.load(inputStream);
		} catch (IOException ex) {
			 logger.error("loadProperties IOException  ioe: " + ex.getMessage());
		} finally {
			try {
				if (inputStream != null) {			
					inputStream.close();
					inputStream = null;
				}	
			} catch (IOException e) {
				 logger.error("loadProperties Exception  e: " + e.getMessage());
			}
		}
	}
	
	/**
	 * Strips off the TAXII portion from the supplied TAXII Inbox_Message Document 
	 * and then saves the content as an XML File to the appropriate feed destination.
	 * 
	 * @param taxiiDoc a TAXII 1.0 compliant Document object
	 */
	public void save(Document taxiiDoc) {
		Date date = new Date();
		try {
			if (config == null) {
				loadProperties();
			}
			DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
			Element taxiiRoot = taxiiDoc.getDocumentElement();
			String inclusiveBeginTimestamp = "", inclusiveEndTimestamp = "";
			Element srcSub = (Element) taxiiRoot.getElementsByTagName("Source_Subscription").item(0);
			String savePath = config.getProperty("basePath") + "subscribeFeeds/";
			String subId = "", fN = "";
			if (srcSub != null) {
				fN = srcSub.getAttribute("feed_name");
				savePath = savePath + fN + "/";
				subId = srcSub.getAttribute("subscription_id");
				try {
					Node node = null;
					NodeList nlist = (NodeList) srcSub.getElementsByTagName("Inclusive_Begin_Timestamp");
					logger.debug("save nlist: " + nlist);
					if (nlist != null) {
						logger.debug("save Inclusive_Begin_Timestamp nlist.getLength(): " +  nlist.getLength());
					}
					if (nlist != null && nlist.getLength() > 0) {
					    node = (Node) nlist.item(0);
					    logger.debug("save inclusiveBeginTimestamp node: " + node);
						if (node != null) {
							logger.debug("save inclusiveBeginTimestamp node: " + node);
						    inclusiveBeginTimestamp = node.getNodeValue();
						    if (inclusiveBeginTimestamp == null) {
						    	node = node.getFirstChild();
						    	inclusiveBeginTimestamp = node.getNodeValue();
						    }
						}
					}
					nlist = (NodeList) srcSub.getElementsByTagName("Inclusive_End_Timestamp");
					logger.debug("save nlist: " + nlist);
					if (nlist != null && nlist.getLength() > 0) {
						logger.debug("save Inclusive_End_Timestamp  nlist.getLength(): " +  nlist.getLength());
						node = nlist.item(0);
						logger.debug("save  inclusiveEndTimestamp node: " + node);
						if (node != null) {
							logger.debug("save inclusiveEndTimestamp node: " + node);
						    inclusiveEndTimestamp = node.getNodeValue();
						    if (inclusiveEndTimestamp == null) {
						    	node = node.getFirstChild();
						    	inclusiveEndTimestamp = node.getNodeValue();
						    }
						}
					}
				} catch (DOMException e) {
					logger.error("DOMException! e: " + e.getMessage());
				}
			}
			logger.debug("save fN: " + fN + ", savePath: " + savePath + ", subId: " + subId + ", bTime: " + inclusiveBeginTimestamp + ", eTime: " + inclusiveEndTimestamp);
			NodeList contentBlocks = taxiiRoot.getElementsByTagName("Content_Block");
			Element contentBlock, content;
			String contentBinding;
			Document contentDoc;
			logger.debug("save contentBlocks.getLength(): " + contentBlocks.getLength());
			for (int i = 0; i < contentBlocks.getLength(); i++) {
				contentBlock = (Element) contentBlocks.item(i);
				contentBinding = contentBlock.getElementsByTagName("Content_Binding").item(0).getNodeValue();
				logger.debug("save contentBinding: " + contentBinding);
				content = (Element) contentBlock.getElementsByTagName("Content").item(0);
				removeWhitespaceNodes(content);
				content = (Element) content.getFirstChild();
				contentDoc = docBuilder.newDocument();
				Node contentImport = contentDoc.importNode(content, true);
				contentDoc.appendChild(contentImport);
				Element contentRoot = contentDoc.getDocumentElement();
				if ("http://taxii.mitre.org/messages/taxii_xml_binding-1".equalsIgnoreCase(contentRoot.getNamespaceURI())) {
					contentDoc.renameNode(contentRoot, null, contentRoot.getNodeName());
				}
				
				// save
				TransformerFactory transformerFactory = TransformerFactory.newInstance();
				Transformer transformer = transformerFactory.newTransformer();
				transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
				transformer.transform(new DOMSource(contentDoc), new StreamResult(new File(savePath + date.getTime() + "_" + i + ".xml")));
			}
		} catch (ParserConfigurationException e) {
			logger.error("ParserConfigurationException! e: " + e.getMessage());
		} catch (TransformerConfigurationException e) {
			logger.error("TransformerConfigurationException! e: " + e.getMessage());
		} catch (TransformerException e) {
			logger.error("TransformerException! e: " + e.getMessage());
		} catch(Exception e){
			logger.error("Exception! e: " + e.getMessage());
		} finally {
			logger.debug("Leaving save ...");
		}
	}	
	
	/**
	 * Strips off the TAXII portion from the supplied TAXII Inbox_Message File 
	 * and then saves the content as an XML File to the appropriate feed destination.
	 * 
	 * @param taxiiMessage a TAXII 1.0 compliant XML file
	 * @throws ParserConfigurationException 
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 * @throws SAXException 
	 * @throws TransformerException 
	 */
	public void save(File taxiiMessage) throws ParserConfigurationException, FileNotFoundException, IOException, SAXException, TransformerException {
		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
		docFactory.setValidating(true);
		docFactory.setIgnoringElementContentWhitespace(true);
		docFactory.setNamespaceAware(true);
		docFactory.setIgnoringComments(true);
		docFactory.setFeature("http://xml.org/sax/features/external-general-entities", false);
		docFactory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
		docFactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
		docFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
		save(docBuilder.parse(taxiiMessage));
	}

	/**
	 * Strips off the TAXII portion from the supplied TAXII Inbox_Message String 
	 * and then saves the content as an XML File to the appropriate feed destination.
	 * 
	 * @param taxiiMessage a String containing TAXII 1.0 compliant XML
	 * @throws ParserConfigurationException 
	 * @throws TransformerException 
	 * @throws SAXException 
	 * @throws IOException 
	 * @throws UnsupportedEncodingException 
	 */
	public void save(String taxiiMessage) throws ParserConfigurationException, UnsupportedEncodingException, IOException, SAXException, TransformerException {
			DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
			docFactory.setValidating(true);
			docFactory.setIgnoringElementContentWhitespace(true);
			docFactory.setNamespaceAware(true);
			docFactory.setIgnoringComments(true);
			docFactory.setFeature("http://xml.org/sax/features/external-general-entities", false);
			docFactory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
			docFactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);//
			docFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
			save(docBuilder.parse(new InputSource(new ByteArrayInputStream(taxiiMessage.getBytes("utf-8")))));
	}
	
	/**
	 * Returns a String containing a TAXII XML response - the results from the poll
	 * @param fN feed name
	 * @param subId the subscription id
	 * @param extHdr a HashMap containing all extended headers (can be null)
	 * @param bTime an exclusive begin timestamp
	 * @param eTime an inclusive end timestamp
	 * @return String containing the TAXII XML Poll_Response message
	 */
	public String poll(String fN, String subId, HashMap<String,String> extHdr, String bTime, String eTime, Set<String> cBind, String dsSig) {
		try {			
			logger.debug("poll fN: " + fN + " subId: " + subId + " extHdr: " + extHdr + " bTime: " + bTime + " eTime: " + eTime + " cBind: " + cBind + " dsSig: " + dsSig);
			if (config == null) {
				loadProperties();
			}
			
			// set keystore and truststore properties
			System.setProperty("javax.net.ssl.keyStore", config.getProperty("pathToSubscriberKeyStore"));
			System.setProperty("javax.net.ssl.keyStorePassword", FlareClientUtil.decrypt(config.getProperty("subscriberKeyStorePassword")));
			System.setProperty("javax.net.ssl.trustStore", config.getProperty("pathToTrustStore"));
	
			// taxiiDoc setup
			DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
			Document taxiiDoc = docBuilder.newDocument();
			
			// Poll_Request (root element)
			Element taxiiRoot = taxiiDoc.createElementNS("http://taxii.mitre.org/messages/taxii_xml_binding-1", "Poll_Request");
			taxiiDoc.appendChild(taxiiRoot);
			String msgId = generateMsgId();
            taxiiRoot.setAttribute("message_id", msgId);
            // taxiiRoot.setAttribute("message_id", UUID.randomUUID().toString());  // will use for taxii 1.1
			taxiiRoot.setAttribute("feed_name", fN);
			if (subId != null) {
				taxiiRoot.setAttribute("subscription_id", subId);
			}
			
			// Extended_Headers
			if (extHdr != null && !extHdr.keySet().isEmpty()) {
				Element eHs = taxiiDoc.createElement("Extended_Headers");
				Element eH = taxiiDoc.createElement("Extended_Header");
				for (String name : extHdr.keySet()) {
					eH.setAttribute("name", name);
					eH.appendChild(taxiiDoc.createTextNode(extHdr.get(name)));
					eHs.appendChild(eH);
				}
				taxiiRoot.appendChild(eHs);
			}
			
			// Exclusive_Begin_Timestamp
			if (bTime != null) {
				Element bT = taxiiDoc.createElement("Exclusive_Begin_Timestamp");
				bT.appendChild(taxiiDoc.createTextNode(bTime));
				taxiiRoot.appendChild(bT);
			}
			
			// Inclusive_End_Timestamp
			if (eTime != null) {
				Element eT = taxiiDoc.createElement("Inclusive_End_Timestamp");
				eT.appendChild(taxiiDoc.createTextNode(eTime));
				taxiiRoot.appendChild(eT);
			}
			
			// Content_Binding
			if (cBind != null) {
				Element cB = null;
				Iterator<String> iter = cBind.iterator();
				while (iter.hasNext()) {
					cB = taxiiDoc.createElement("Content_Binding");
					cB.appendChild(taxiiDoc.createTextNode(iter.next()));
					taxiiRoot.appendChild(cB);
				}
				logger.debug("poll cB: " + cB);
			}
				
			// ds:Signature
			if (dsSig != null) {
				Element dS = taxiiDoc.createElement("ds:Signature");
				dS.appendChild(taxiiDoc.createTextNode(dsSig));
				taxiiRoot.appendChild(dS);
			}
				
			Transformer transformer = TransformerFactory.newInstance().newTransformer();
			transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
			OutputStream stream = new ByteArrayOutputStream();
			transformer.transform(new DOMSource(taxiiDoc), new StreamResult(stream));
			String output = stream.toString().replaceAll("\n|\r", "");
			
			String filteredStr = output;			
			filteredStr = filteredStr.replaceAll("\n|\r", "");	
			filteredStr = filteredStr.trim();
			logger.debug("poll request: " + filteredStr);
			try {
				List<SchemaError> errors = FlareClientUtil.validateTaxii(filteredStr);
				if (errors != null) {
					int size = errors.size();
					if (size > 0) {
						for (SchemaError error : errors) {
							logger.debug("SchemaError error getCategory: " + error.getCategory());
							logger.debug("SchemaError error getMessage: " + error.getMessage());
						}
						logger.error("Error with TAXII validation of outbound request, quitting now...");
					} else {
						logger.debug("Validation result: No Error...");
					}
				}
			} catch (Exception e) {
				logger.error("Error with TAXII validation of outbound request, Exception: " + e.getMessage());
			}
			
			if (config == null) {
				loadProperties();
			}

			// open connection to layer 7 and send post request
			URL layer7 = new URL(config.getProperty("layer7Url") + "poll");
			HttpsURLConnection conn = (HttpsURLConnection) layer7.openConnection();
			conn.setRequestMethod("POST");
			conn.setRequestProperty("User-Agent", config.getProperty("subscriberUserAgent"));
			conn.setRequestProperty("Content-Type", config.getProperty("subscriberContentType"));
			conn.setRequestProperty("Accept", config.getProperty("subscriberAccept"));
			conn.setRequestProperty("X-TAXII-Accept", config.getProperty("subscriberXTaxiiAccept"));
			conn.setRequestProperty("X-TAXII-Content-Type", config.getProperty("subscriberXTaxiiContentType"));
			conn.setRequestProperty("X-TAXII-Protocol", config.getProperty("subscriberXTaxiiProtocol"));
			conn.setRequestProperty("X-TAXII-Services", config.getProperty("subscriberXTaxiiServices"));		 
			conn.setDoOutput(true);
			
			OutputStream outputStream = null;
			DataOutputStream wr = null;
			InputStream is = null;
			String response = "";
			try {
				outputStream = conn.getOutputStream();
				wr = new DataOutputStream(outputStream);
			    wr.write(filteredStr.getBytes("UTF-8"));
				wr.flush();				
				is = conn.getInputStream();			
				response = IOUtils.toString(is, "UTF-8");
			} catch (IOException ioe) {
				 logger.error("poll IOException ioe: " + ioe.getMessage());
			} finally {
				if (is != null) {
					try {
					   is.close();
					} catch (IOException e) {
						logger.error("IOException e: " + e.getMessage());
					}
					is = null;
				}
				if (outputStream != null) {
					try {
						outputStream.close();
					} catch (IOException oe) {
						logger.error("IOException oe: " + oe.getMessage());
					}
					outputStream = null;
				}
				if (wr != null) {
					try {
						wr.close();
					} catch (IOException e) {
						logger.error("IOException e: " + e.getMessage());
					}
					wr = null;
				}
			}
			
			// validate inbound response
			try {
				List<SchemaError> errors = FlareClientUtil.validateTaxii(response);
				if (errors != null) {
					int size = errors.size();
					if (size > 0) {
						for (SchemaError error : errors) {
							logger.debug("SchemaError error getCategory: " + error.getCategory());
							logger.debug("SchemaError error getMessage: " + error.getMessage());
						}
						logger.error("Error with validation of inbound poll response");
					} else {
						logger.debug("Validation result: No Error...");
					}
				}
			} catch (Exception e) {
				logger.error("Error with validation of inbound poll response, Exception: " + e.getMessage());
			}
			
			// print poll response length
			if (response != null) {
			   logger.debug("poll response length: " + response.length());
			}
			return response;
		} catch (IOException ioe) {
			logger.error("poll IOException: " + ioe.getMessage());
		} catch (ParserConfigurationException pce) {
			logger.error("poll ParserConfigurationException: " + pce.getMessage());
		} catch (TransformerException tfe) {
			logger.error("poll TransformerException: " + tfe.getMessage());
		} finally {
			logger.debug("Leaving poll...");
		}
		return "";
	}
	

	
	/**
	 * helper function that removes whitespace nodes
	 * from Element objects to allow for easier parsing 
	 * @param e the Element object
	 */
	private static void removeWhitespaceNodes(Element e) {
		NodeList children = e.getChildNodes();
		for (int i = children.getLength()-1; i >= 0; i--) {
			Node child = children.item(i);
			if (child instanceof Text && ((Text) child).getData().trim().length() == 0) {
				e.removeChild(child);
			} else if (child instanceof Element) {
				removeWhitespaceNodes((Element) child);
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
