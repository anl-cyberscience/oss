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


import com.bcmcgroup.flare.client.FlareClientUtil;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;

import javax.xml.crypto.MarshalException;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureException;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMValidateContext;

import org.apache.log4j.Logger;
import org.apache.xerces.impl.dv.util.Base64;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.crypto.dsig.Reference;

/**
 * This class is used to provide the functionality for the verification of a digitally signed XML document.
 * 
 * @author		David Du <ddu@bcmcgroup.com>
 * @version		1.1
 *
 */
public class XmlDigitalSignatureVerifier {	
	private static final Logger logger = Logger.getLogger(XmlDigitalSignatureVerifier.class);

    /**
     * Method used to verify the XML digital signature
     * @param doc Document object containing xml document whose signature is being validated
     * @param keyStorePath Path to java KeyStore containing publishers' public certificates
     * @param keyStorePW The KeyStore password
     * @return true if validated, false otherwise
     */
	public static boolean validateSignature(Element contentBlock, String keyStorePath, String keyStorePW, int blockIndex) {
    	logger.debug("validateSignature keyStorePath: " + keyStorePath + " keyStorePW: " + keyStorePW + " blockIndex: " + blockIndex);
        boolean validFlag = false;
        NodeList contentList = contentBlock.getElementsByTagName("Content");
    	logger.debug("validateSignature contentList.getLength(): " + contentList.getLength());
		
    	Element content = (Element)contentList.item(0);
  	    if (content == null) {
  	    	logger.error("validateSignature Wrong document, Content tag is missing... ");
  	    	return validFlag;
  	    }
  	    
  	    Element signNode = null;
  	    NodeList signList = contentBlock.getElementsByTagNameNS(XMLSignature.XMLNS, "Signature");
  	    if (signList.getLength() > 0) {
  	    	signNode = (Element)signList.item(0);
	    } else {
	    	logger.error("validateSignature No XML Digital Signature Found, document is discarded");
	    	return validFlag;
	    }
	 
        KeyStore ks;
    	FileInputStream is  = null;
		try {
			Node x509cert = contentBlock.getElementsByTagNameNS(XMLSignature.XMLNS, "X509Certificate").item(0);
			logger.debug("validateSignature x509cert: " + x509cert);
			String x509Str =  x509cert.getTextContent().trim();
			logger.debug("validateSignature x509Str : " + x509Str);
			MessageDigest md = MessageDigest.getInstance("SHA-1");
		    md.reset();
		    byte [] decoded = Base64.decode(x509Str);
		    X509Certificate cert = (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(new ByteArrayInputStream(decoded));
		    byte[] der = cert.getEncoded();
		    md.update(der);
	        String hashAlias = FlareClientUtil.hexify(md.digest());
	    	logger.debug("validateSignature SHA1 fingerprint hashAlias: " + hashAlias);	    		
			ks = KeyStore.getInstance("JKS");
			is = new FileInputStream(keyStorePath);
			ks.load(is, keyStorePW.toCharArray());
			logger.debug("validateSignature keyStorePW: " + keyStorePW);
			
			// List the aliases
		    Enumeration<String> enu = ks.aliases();
		    logger.debug("validateSignature enu: " + enu);
		    int counter = 0;
		    while (enu.hasMoreElements()) {
		    	counter++;
		        String alias = (String)enu.nextElement();
		        logger.debug("validateSignature alias: " + alias);
		        
		        // Does alias refer to a private key?
		        boolean b = ks.isKeyEntry(alias);
		        logger.debug("validateSignature refer to a private key: " + b);
		        // Does alias refer to a trusted certificate?
		        b = ks.isCertificateEntry(alias);
		        logger.debug("validateSignature refer to a trusted certificate: " + b);
		    }
		    
		    if (counter == 0) {
		    	logger.debug("validateSignature: No alias found in the key store...");
		    }
			Certificate certificate = ks.getCertificate(hashAlias);
			logger.debug("validateSignature certificate: " + certificate);			
			if (certificate != null) {
				PublicKey pkey = certificate.getPublicKey();
			    logger.debug("validateSignature pkey: " + pkey);
			    logger.debug("validateSignature algorithm: " + pkey.getAlgorithm());	
				content.setAttribute("myID", "contentID" + (blockIndex+1));
			    content.setIdAttribute("myID", true);
		    	DOMValidateContext vc = new DOMValidateContext(pkey,signNode);
		        XMLSignatureFactory xmlsf = XMLSignatureFactory.getInstance("DOM");
		        XMLSignature signature = null;
				try {
					vc.setIdAttributeNS(content, null, "myID");
					vc.setProperty("javax.xml.crypto.dsig.cacheReference", true);
					signature = xmlsf.unmarshalXMLSignature(vc);
					logger.debug("validateSignature signature: " + signature);
					validFlag = signature.validate(vc);
					logger.debug("validateSignature validFlag: " + validFlag);
					if (validFlag == false) {
						logger.error("Signature failed core validation");
						boolean sv = signature.getSignatureValue().validate(vc);
						logger.debug("signature validation status: " + sv);
						
						// for testing/debugging when validation fails...
						@SuppressWarnings("rawtypes")
						Iterator i = signature.getSignedInfo().getReferences().iterator();
						for (int j=0; i.hasNext(); j++) {
							Reference ref = (Reference) i.next();
				    	    logger.debug("validateSignature ref: " + ref);	
				    	    byte[] dvalues = ref.getCalculatedDigestValue();
				    	    String dvaluesStr = FlareClientUtil.hexify(dvalues);
				    	    logger.debug("validateSignature dvaluesStr: " + dvaluesStr);	
				    	    byte[] dvalues2 = ref.getDigestValue();
				    	    String dvaluesStr2 = FlareClientUtil.hexify(dvalues2);
				    	    logger.debug("validateSignature dvaluesStr2: " + dvaluesStr2);	
					        boolean refValid = ref.validate(vc);
					        logger.debug("ref[" + j + "] validity status: " + refValid + " ref.getURI(): " + ref.getURI());
					        InputStream refIS =ref.getDigestInputStream();
					        logger.debug("validateSignature refIS: " + refIS);	
					        if (refIS != null) {
						        String digestStr = FlareClientUtil.convertStreamToString(refIS);
						        logger.debug("validateSignature validated Str: " + digestStr);	
					        }
						}
					}
				} catch (MarshalException e) {
					logger.debug("validateSignature MarshalException: " + e);
				} catch (XMLSignatureException e) {
					logger.debug("validateSignature XMLSignatureException: " + e);
				}
		        content.removeAttribute("myID");
				return validFlag;
			}
		} catch (KeyStoreException ex) {
			logger.error("validateSignature KeyStoreException ex: " + ex);
		} catch (NoSuchAlgorithmException ex) {
			logger.error("validateSignature NoSuchAlgorithmException ex: " + ex);
		} catch (CertificateException ex) {
			logger.error("validateSignature CertificateException ex: " + ex);
		} catch (IOException ex) {
			logger.error("validateSignature IOException ex: " + ex);
		} finally {		
			if (null != is) {
				try {
	                is.close();
	            } catch (IOException e) {
					logger.debug("validateSignature IOException: " + e);
	            }
			}
		}
		return false;
    }
	 
    public static List<Boolean> validateSignature(Document doc, String keyStorePath, String keyStorePW) {
    	logger.debug("validateSignature keyStorePath: " + keyStorePath + " keyStorePW: " + keyStorePW);
    	doc.normalizeDocument();	
        boolean validFlag = false;
        List<Boolean> validList = new ArrayList<Boolean>();
        NodeList contentBlocks = doc.getElementsByTagName("Content_Block");
    	logger.debug("validateSignature contentBlocks.getLength(): " + contentBlocks.getLength());
		for (int ii=0; ii < contentBlocks.getLength(); ii++) {			
			Element contentBlock = (Element)contentBlocks.item(ii);
			validFlag = validateSignature(contentBlock, keyStorePath, keyStorePW, ii);
			validList.add(validFlag);
		}
		logger.debug("validateSignature validList: " +validList);
		return validList;
    }
   
    
  
}
