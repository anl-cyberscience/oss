package com.bcmcgroup.flare.xml.digsig;

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

import com.bcmcgroup.flare.client.ClientUtil;

import java.io.InputStream;
import java.security.PublicKey;
import java.util.Iterator;
import javax.xml.crypto.MarshalException;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureException;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMValidateContext;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import javax.xml.crypto.dsig.Reference;

/**
 * This class is used to provide the functionality for the verification of a digitally signed XML document.
 * 
 * @author		David Du <ddu@bcmcgroup.com>
 * @version		2.0
 *
 */
public class XmlDigitalSignatureVerifier {	
	private static final Logger logger = Logger.getLogger(XmlDigitalSignatureVerifier.class);
		
	/**
	 * Used to verify an enveloped digital signature
	 * 
	 * @param doc a Document object containing the xml with the signature
	 * @param keyStorePath a String containing the path to the KeyStore
	 * @param keyStorePW a String containing the KeyStore password
	 * @param hubAlias a String containing the alias of the public key used for verification
	 * @return
	 */
public static boolean verifySignatureEnveloped(Document doc, String keyStorePath, String keyStorePW, String hubAlias) {
		logger.debug("************************ Entering verifySignatureEnvelopedRoot**********************************");
    	logger.debug("verifySignatureEnvelopedRoot keyStorePath: " + keyStorePath + " keyStorePW: " + keyStorePW + " hubAlias: " + hubAlias);
        boolean validFlag = false;
  	    logger.debug("verifySignatureEnvelopedRoot doc: " + ClientUtil.convertDocumentToString(doc, true));
		PublicKey pubkey = ClientUtil.getPublicKeyByAlias(keyStorePath, keyStorePW, hubAlias);
	    validFlag = isXmlDigitalSignatureValid(doc, pubkey);
		return validFlag;
    }
    
	 /**
     * Method used to verify the XML digital signature
     * 
     * @param doc Document object containing xml document whose signature is being verified
     * @param publicKey PublicKey object containing public key to use for verification
     * @return true if verified, false otherwise
     */
    public static boolean isXmlDigitalSignatureValid(Document doc, PublicKey publicKey) {
		boolean validFlag = false;
		try {
			NodeList nl = doc.getElementsByTagNameNS(XMLSignature.XMLNS, "Signature");
			if (nl.getLength() == 0) {
				logger.error("Error: No XML Digital Signature Found, document is discarded...");
				return validFlag;
			}
			DOMValidateContext valContext = new DOMValidateContext(publicKey, nl.item(0));
			XMLSignatureFactory fac = XMLSignatureFactory.getInstance("DOM");
			XMLSignature signature = fac.unmarshalXMLSignature(valContext);
			validFlag = signature.validate(valContext);
			logger.debug("isXmlDigitalSignatureValid validFlag: " + validFlag);
			if (validFlag == false) {
				logger.error("isXmlDigitalSignatureValid failed core validation");
				boolean sv = signature.getSignatureValue().validate(valContext);
				logger.debug("isXmlDigitalSignatureValid validation status: " + sv);

				// for testing/debugging when validation fails...
				@SuppressWarnings("rawtypes")
				Iterator i = signature.getSignedInfo().getReferences().iterator();
				for (int j = 0; i.hasNext(); j++) {
					Reference ref = (Reference) i.next();
					logger.debug("isXmlDigitalSignatureValid ref: " + ref);
					byte[] dvalues2 = ref.getDigestValue();
					String dvaluesStr2 = ClientUtil.hexify(dvalues2);
					logger.debug("isXmlDigitalSignatureValid getDigestValue dvaluesStr2: " + dvaluesStr2);
					boolean refValid = ref.validate(valContext);
					logger.debug("ref[" + j + "] validity status: " + refValid + " ref.getURI(): " + ref.getURI());
					byte[] dvalues = ref.getCalculatedDigestValue();
					String dvaluesStr = ClientUtil.hexify(dvalues);
					logger.debug("isXmlDigitalSignatureValid getCalculatedDigestValue dvaluesStr: " + dvaluesStr);
					logger.debug("isXmlDigitalSignatureValid getDigestValue dvaluesStr2: " + dvaluesStr2);
					InputStream refIS = ref.getDigestInputStream();
					logger.debug("isXmlDigitalSignatureValid refIS: " + refIS);
					if (refIS != null) {
						String digestStr = ClientUtil.convertStreamToString(refIS);
						logger.debug("isXmlDigitalSignatureValid verified Str: " + digestStr);
					}
				}
			}
		} catch (MarshalException e) {
			logger.error("isXmlDigitalSignatureValid MarshalException: " + e);
		} catch (XMLSignatureException e) {
			logger.error("isXmlDigitalSignatureValid XMLSignatureException: " + e);
		}
		return validFlag;
	}
}