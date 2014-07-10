package com.bcmcgroup.taxii;

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

import java.security.SecureRandom;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/** 
 * @author		Mark Walters <mwalters@bcmcgroup.com>
 * @version		1.1
 */
public class StatusMessage {
	private static final Logger logger = Logger.getLogger(StatusMessage.class);
	String msgId, inResponseTo, statusType, statusDetail, message;
	
	/**
	 * Constructor for a StatusMessage
	 * @param mI String containing the attribute "message_id"
	 * @param iRT String containing the attribute "in_response_to"
	 * @param sT String containing the attribute "status_type"
	 * @param sD String containing the Element "Status_Detail"
	 * @param m String containing the Element "Message"
	 */
	public StatusMessage(String mI, String iRT, String sT, String sD, String m) {
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
	 * @return A Document object containing the TAXII Status_Message
	 */
	public Document getDocument() {
		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder = null;
		try {
			docBuilder = docFactory.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			 logger.error("getDocument ParserConfigurationException thrown e: " + e.getMessage() );
		}
		Document responseDoc = docBuilder.newDocument();
		Element responseRoot = responseDoc.createElement("Status_Message");
		responseRoot.setAttribute("message_id", msgId);
		responseRoot.setAttribute("in_response_to", inResponseTo);
		responseRoot.setAttribute("status_type", statusType);
		if (statusDetail != null && statusType != null && ( "RETRY".equalsIgnoreCase(statusType) || statusType.startsWith("UNSUPPORTED"))) {
			Element sDetail = responseDoc.createElement("Status_Detail");
			sDetail.appendChild(responseDoc.createTextNode(statusDetail));
			responseRoot.appendChild(sDetail);
		}
		if (message != null) {
			Element msg = responseDoc.createElement("Message");
			msg.appendChild(responseDoc.createTextNode(message));
			responseRoot.appendChild(msg);
		}
		responseDoc.appendChild(responseRoot);
		return responseDoc;
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

