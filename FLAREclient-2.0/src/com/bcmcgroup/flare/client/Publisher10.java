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

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.bcmcgroup.flare.xml.digsig.XmlDigitalSignatureGenerator;
import com.bcmcgroup.taxii10.ContentBlock;
import com.bcmcgroup.taxii10.TaxiiUtil;

import javax.net.ssl.HttpsURLConnection;

/** 
 * @author		Mark Walters <mwalters@bcmcgroup.com>
 * @version		2.0
 */
public class Publisher10 {
	private static final Logger logger = Logger.getLogger(Publisher10.class);
	
	/**
	 * Wraps up a list of content blocks as a TAXII message and publishes
	 * them to the flare TAXII server
	 * @param fN feed name
	 * @param extHdrs a HashMap containing all extended headers (can be null)
	 * @param msg string containing optional "Message" element (can be null)
	 * @param cBlocks list of ContentBlock10 objects, each containing an individual content
	 * block for the TAXII message
	 * 
	 * Usage Example:
	 *   Publisher10 pub = new Publisher();
	 *   List<ContentBlock10> contentBlocks = new ArrayList<ContentBlock10>();
     *   ContentBlock10 contentBlock = new ContentBlock10(config.getProperty("stix_cB"), child.toFile(), null, null, null);
     *   contentBlocks.add(contentBlock);
     *   pub.publish(fN, null, null, contentBlocks);
	 */
	public void publish(String fN, HashMap<String,String> extHdrs, String msg, List<ContentBlock> cBlocks) throws IOException {	
		Properties config = ClientUtil.loadProperties();
		
		// set KeyStore and TrustStore properties
		System.setProperty("javax.net.ssl.keyStore", config.getProperty("pathToPublisherKeyStore"));
		System.setProperty("javax.net.ssl.keyStorePassword",  ClientUtil.decrypt(config.getProperty("publisherKeyStorePassword")));
		System.setProperty("javax.net.ssl.trustStore", config.getProperty("pathToTrustStore"));
		//System.setProperty("javax.net.debug", "ssl,handshake");  // use this for debugging ssl/handshake issues	
		
		Document taxiiDoc = TaxiiUtil.inboxMessage(null, extHdrs, msg, null, cBlocks);
		logger.debug("after creation... taxiiDoc = " + ClientUtil.convertDocumentToString(taxiiDoc, true));
		String pks = config.getProperty("pathToPublisherKeyStore");
		String pksPw = ClientUtil.decrypt(config.getProperty("publisherKeyStorePassword"));
		String pk = config.getProperty("publisherKeyName");
		String pkPw = ClientUtil.decrypt(config.getProperty("publisherKeyPassword"));
		String taxiiString = ClientUtil.convertDocumentToString(taxiiDoc, true);
		logger.debug("Publisher10 taxiiString: " + taxiiString);
		List<Integer> statusList = XmlDigitalSignatureGenerator.generateXmlDigitalSignatureEnveloped(taxiiDoc, pks, pksPw, pk, pkPw);
		logger.debug("Publisher10 generateXmlDigitalSignatureEnveloped statusList: " + statusList);
		boolean overallStatus = true;
		if (statusList != null) {
			for (int status: statusList) {
				if (status != 1) {
					overallStatus = false;
					break;
				}
			}
		}
		if (statusList != null && overallStatus) {
			// validate outgoing TAXII 1.0 message
			try {
				TaxiiValidator tv = new TaxiiValidator();
				if (!tv.validate(taxiiDoc) || !ClientUtil.validateStix(taxiiDoc)) {
					return;
				}
			} catch (SAXException e) {
				logger.error("Publisher10 SAXException e: " + e.getMessage());
				return;
			}
			String payload = ClientUtil.convertDocumentToString(taxiiDoc, false);
			logger.debug("Publisher10 message payload: " + payload);
			HttpsURLConnection conn = TaxiiUtil.buildConnection("inbox", fN);
			String response = ClientUtil.sendPost(conn, payload);
			ClientUtil.printHttpsResponse(response);
		} else {
			logger.debug("Publisher10 Message not published since digital signature was not generated.  Please check log message for errors.");
		} 
	}
}