package com.bcmcgroup.taxii11;

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

import java.io.IOException;
import java.io.OutputStream;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.bcmcgroup.flare.client.ClientUtil;
import com.sun.net.httpserver.HttpExchange;

/** 
 * @author		Mark Walters <mwalters@bcmcgroup.com>
 * @version		2.0
 */
public class StatusMessage {
	private static final Logger logger = Logger.getLogger(StatusMessage.class);
	String msgId, inResponseTo, statusType, message;
	HashMap<String,String> statusDetail;
	private static Properties config = ClientUtil.loadProperties();
	private static final String taxiiNS = config.getProperty("taxii11NS");
	
	/**
	 * Constructor for a StatusMessage
	 * @param mI String containing the attribute "message_id"
	 * @param iRT String containing the attribute "in_response_to"
	 * @param sT String containing the attribute "status_type"
	 * @param sD Set<String> containing the elements for "Status_Detail"
	 * @param m String containing the Element "Message"
	 */
	public StatusMessage(String mI, String iRT, String sT, HashMap<String,String> sD, String m) {
		if (mI != null) {
			msgId = mI;
		} else {
			msgId = generateMsgId();
			// msgId = UUID.randomUUID().toString(); // we will use this once TAXII 1.1 releases
		}
		inResponseTo = iRT;
		statusType = sT;
		statusDetail = sD;
		message = m;
	}
	
	/**
	 * Constructs and returns the Document containing the response
	 * 
	 * @return A Document object containing the TAXII Status_Message
	 */
	@SuppressWarnings("rawtypes")
	public Document getDocument() {
		DocumentBuilder db = ClientUtil.generateDocumentBuilder();
		Document responseDoc = db.newDocument();
		Element responseRoot = responseDoc.createElementNS(taxiiNS, "Status_Message");
		responseRoot.setAttribute("message_id", msgId);
		responseRoot.setAttribute("in_response_to", inResponseTo);
		responseRoot.setAttribute("status_type", statusType);
		if (statusDetail != null && statusType != null && ( "RETRY".equalsIgnoreCase(statusType) || statusType.startsWith("UNSUPPORTED"))) {
			Element sDetail = responseDoc.createElementNS(taxiiNS, "Status_Detail");
			Iterator it = statusDetail.entrySet().iterator();
			Element detail;
			while (it.hasNext()) {
				detail = responseDoc.createElementNS(taxiiNS, "Detail");
				Map.Entry pairs = (Map.Entry) it.next();
				detail.setAttribute("name", (String) pairs.getKey());
				detail.appendChild(responseDoc.createTextNode((String) pairs.getValue()));
				sDetail.appendChild(detail);
			}
			responseRoot.appendChild(sDetail);
		}
		if (message != null) {
			Element msg = responseDoc.createElementNS(taxiiNS, "Message");
			msg.appendChild(responseDoc.createTextNode(message));
			responseRoot.appendChild(msg);
		}
		responseDoc.appendChild(responseRoot);
		return responseDoc;
	}
	
	/**
	 * Sends the Status_Message HTTPS response
	 * 
	 * @param responseBody the OutputStream object containing the response body
	 * @param exchange the HttpExchange object required to send the response
	 */
    public void sendResponse(OutputStream responseBody, HttpExchange exchange) {
    	try {
			DOMSource source = new DOMSource(this.getDocument().getDocumentElement());
			StreamResult result = new StreamResult(responseBody);
			TransformerFactory tf = TransformerFactory.newInstance();
			tf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
			Transformer t = tf.newTransformer();
			t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
			exchange.sendResponseHeaders(200, 0);
			t.transform(source, result);
			responseBody.close();
	    } catch (TransformerConfigurationException e) {
			logger.error("sendResponse TransformerConfigurationException e: " + e.getMessage());
		} catch (TransformerException e) {
			logger.error("sendResponse TransformerException e: " + e.getMessage());
		} catch (IOException e) {
			logger.error("sendResponse IOException e: " + e.getMessage());
		}
    	return;
    }
	
	/**
	 * Generate a message ID (note this won't guarantee any uniqueness).  
	 * We expect to convert to UUID when TAXII 1.1 is released.
	 * 
	 * @return a string containing a random integer between 0 and 2,000,000,000
	 */
	private String generateMsgId() {
		SecureRandom r = new SecureRandom();
		return Integer.toString(r.nextInt(2000000000));
	}
}