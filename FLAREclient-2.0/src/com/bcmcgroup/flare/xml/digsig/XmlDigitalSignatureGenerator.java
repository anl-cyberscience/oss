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

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.KeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.xml.crypto.MarshalException;
import javax.xml.crypto.dsig.CanonicalizationMethod;
import javax.xml.crypto.dsig.DigestMethod;
import javax.xml.crypto.dsig.Reference;
import javax.xml.crypto.dsig.SignatureMethod;
import javax.xml.crypto.dsig.SignedInfo;
import javax.xml.crypto.dsig.Transform;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureException;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.KeyInfoFactory;
import javax.xml.crypto.dsig.keyinfo.KeyValue;
import javax.xml.crypto.dsig.spec.C14NMethodParameterSpec;
import javax.xml.crypto.dsig.spec.SignatureMethodParameterSpec;
import javax.xml.crypto.dsig.spec.TransformParameterSpec;
import javax.xml.parsers.DocumentBuilder;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.bcmcgroup.flare.client.ClientUtil;

/** 
 * This class is used to provide convenient methods to digitally sign an XML document.
 * 
 * @author		David Du <ddu@bcmcgroup.com>
 * @version		2.0
 *
 */
public class XmlDigitalSignatureGenerator {
	private static final Logger logger = Logger.getLogger(XmlDigitalSignatureGenerator.class);
	private static final String RSA_SHA256_URI = "http://www.w3.org/2001/04/xmldsig-more#rsa-sha256";
    
	/**
     * Method used to get the XML document by parsing
     *
     * @param xmlFilePath file path of the XML document
     * @return Document
     */
    public static Document getXmlDocument(String xmlFilePath) {
    	FileInputStream fis = null;
        try {
	    	logger.debug("getXmlDocument xmlFilePath: " + xmlFilePath);
			DocumentBuilder db = ClientUtil.generateDocumentBuilder();
			fis = new FileInputStream(xmlFilePath);
			Document doc = db.parse(fis);
			return doc;
        } catch (FileNotFoundException e) {
            logger.error("FileNotFoundException e: " + e);
        } catch (SAXException e) {
            logger.error("SAXException e: " + e);
        } catch (IOException e) {
            logger.error("IOException e: " + e);
        } finally {
        	if (fis != null) {
        		try {
					fis.close();
				} catch (IOException e) {
					logger.error("IOException e: " + e);
				}
        		fis = null;
        	}
        }
        return null;
    }

    /**
     * Method used to create an enveloped digital signature for an XML document
     *
     * @param srcXmlStr String containing the source XML document
     * @param keyStorePath Path to KeyStore used for signing document
     * @param keyStorePW KeyStore password
     * @param keyName Alias for private key in KeyStore
     * @param keyPW Private key password
     * @return Signed Document
     * 
     * Usage Example:
     *   String pks = config.getProperty("pathToPublisherKeyStore");
	 *	 String pksPw = FLAREclientUtil.decrypt(config.getProperty("publisherKeyStorePassword"));
	 *	 String pk = config.getProperty("publisherKeyName");
	 *	 String pkPw = FLAREclientUtil.decrypt(config.getProperty("publisherKeyPassword"));
	 *	 List<Integer> statusList = XmlDigitalSignatureGenerator.generateXmlDigitalSignatureEnveloped(taxiiString, pks, pksPw, pk, pkPw);	
     */
    public static List<Integer> generateXmlDigitalSignatureEnveloped(String srcXmlStr, String keyStorePath, String keyStorePW, String keyName, String keyPW) {
		logger.debug("generateXmlDigitalSignatureEnveloped keyStorePath: " + keyStorePath + " keyName: " + keyName);
		logger.debug("generateXmlDigitalSignatureEnveloped keyStorePW: " + keyStorePW + " keyPW: " + keyPW);
		if (srcXmlStr == null || srcXmlStr.isEmpty()) {
			return null;
		}
		Document doc = ClientUtil.convertStringToDocument(srcXmlStr);
  	    return generateXmlDigitalSignatureEnveloped(doc, keyStorePath, keyStorePW, keyName, keyPW);
    }
    
    /**
     * Method used to create an enveloped digital signature for an XML document
     *
     * @param doc Document object containing the source XML document
     * @param keyStorePath Path to KeyStore used for signing document
     * @param keyStorePW KeyStore password
     * @param keyName Alias for private key in KeyStore
     * @param keyPW Private key password
     * @return Signed Document
     * 
     * Usage Example:
     *   String pks = config.getProperty("pathToPublisherKeyStore");
	 *	 String pksPw = FLAREclientUtil.decrypt(config.getProperty("publisherKeyStorePassword"));
	 *	 String pk = config.getProperty("publisherKeyName");
	 *	 String pkPw = FLAREclientUtil.decrypt(config.getProperty("publisherKeyPassword"));
	 *	 List<Integer> statusList = XmlDigitalSignatureGenerator.generateXmlDigitalSignatureEnveloped(taxiiDocument, pks, pksPw, pk, pkPw);	
     */
   	public static List<Integer> generateXmlDigitalSignatureEnveloped(Document doc, String keyStorePath, String keyStorePW, String keyName, String keyPW) {
    	logger.debug("******************Entering generateXmlDigitalSignatureEnveloped ************************");
     	logger.debug("generateXmlDigitalSignatureEnveloped   doc: " + ClientUtil.convertDocumentToString(doc, true));
       	logger.debug("generateXmlDigitalSignatureEnveloped keyStorePath: " + keyStorePath + " keyName: " + keyName);
       	logger.debug("generateXmlDigitalSignatureEnveloped keyStorePW: " + keyStorePW + " keyPW: " + keyPW);
       	List<Integer> statusList = new ArrayList<Integer>();
        PrivateKeyEntry keyEntry =  ClientUtil.getKeyEntry(keyStorePath, keyStorePW, keyName, keyPW);
       	NodeList contentBlockList = doc.getDocumentElement().getElementsByTagName("Content_Block");
       	logger.debug("generateXmlDigitalSignatureEnveloped contentBLList.getLength(): " + contentBlockList.getLength());
   		for (int i=0; i < contentBlockList.getLength(); i++) {
   		    Element contentBlock = (Element) contentBlockList.item(i);
   		  	if (contentBlock == null) {
   		  		logger.error("generateXmlDigitalSignatureEnveloped Wrong document, Content tag is missing... ");
   		  	    continue;
   		  	}
   			if (keyEntry != null) {
   				boolean resultStatus = signElement(contentBlock, keyEntry);
   				if (resultStatus) {
   					statusList.add(1);
   				} else {
   					statusList.add(-1);
   				}
   			} else {
   				statusList.add(-1);
   				logger.error("generateXmlDigitalSignatureEnveloped no signature signed, since no key entry found for supplied keyName parameter");
   			}
   			logger.debug("generateXmlDigitalSignatureEnveloped doc: " + ClientUtil.convertDocumentToString(doc, true));
   			logger.debug("******************Leaving generateXmlDigitalSignatureEnveloped ************************");
   		}
   		return statusList;
   	}
   
    /**
     * Method used to create an enveloped digital signature for an XML document
     *
     * @param signElement the signature element containing the signature XML document
     * @param keyEntry the PrivateKeyEntry
     * @return the status of the operation
     * 
     * Usage Example:
     *   String pks = config.getProperty("pathToPublisherKeyStore");
	 *	 String pksPw = FLAREclientUtil.decrypt(config.getProperty("publisherKeyStorePassword"));
	 *	 String keyName = config.getProperty("publisherKeyName");
	 *	 String keyPW = FLAREclientUtil.decrypt(config.getProperty("publisherKeyPassword"));
	 *   PrivateKeyEntry keyEntry =  FLAREclientUtil.getKeyEntry(pks, pksPw, keyName, keyPW);
	 *   Element contentBL = ...
	 *   List<Integer> statusList = XmlDigitalSignatureGenerator.generateXmlDigitalSignatureEnveloped(contentBL, keyEntry);	
     */
    public static boolean signElement(Element element, PrivateKeyEntry keyEntry) {
    	boolean status = false;
    	
        //Create XML Signature Factory
        XMLSignatureFactory xmlSigFactory = XMLSignatureFactory.getInstance("DOM");
        PublicKey publicKey = ClientUtil.getPublicKey(keyEntry);
		PrivateKey privateKey = keyEntry.getPrivateKey();
        DOMSignContext domSignCtx = new DOMSignContext(privateKey, element);
        domSignCtx.setDefaultNamespacePrefix("ds");
        Reference ref = null;
        SignedInfo signedInfo = null;
        DigestMethod dm = null;
        SignatureMethod sm = null;
        try {
        	String algorithm = publicKey.getAlgorithm();
        	X509Certificate cert = (X509Certificate) keyEntry.getCertificate();
			String algorithmName = cert.getSigAlgName();
			logger.debug("signElement algorithm: " + algorithm);
			logger.debug("signElement Digest algorithmName: " + algorithmName);
        	if (algorithm.toUpperCase().contains("RSA")) {
        		if (algorithmName.toUpperCase().contains("SHA1")) {
        			dm = xmlSigFactory.newDigestMethod(DigestMethod.SHA1, null);
		    		sm = xmlSigFactory.newSignatureMethod(SignatureMethod.RSA_SHA1, null);
		        } else if (algorithmName.toUpperCase().contains("SHA2")) {
		        	dm = xmlSigFactory.newDigestMethod(DigestMethod.SHA256, null);
			       	sm = xmlSigFactory.newSignatureMethod(RSA_SHA256_URI, (SignatureMethodParameterSpec) null);
		        } else {
		        	logger.error("signElement Digest method: " + algorithmName + " is not supported");
		        }
		        @SuppressWarnings("rawtypes")
				List transforms = Collections.singletonList(xmlSigFactory.newTransform(Transform.ENVELOPED, (TransformParameterSpec) null));
		        ref = xmlSigFactory.newReference("", dm, transforms, null, null);
		        CanonicalizationMethod cm = xmlSigFactory.newCanonicalizationMethod(CanonicalizationMethod.INCLUSIVE, (C14NMethodParameterSpec) null);
		        @SuppressWarnings("rawtypes")
		        List references = Collections.singletonList(ref);
		        signedInfo = xmlSigFactory.newSignedInfo(cm, sm, references);
	        } else {
	        	logger.error("signElement Algorithm: " + algorithm + " is not supported");
	        }
        } catch (NoSuchAlgorithmException ex) {
        	 logger.error("NoSuchAlgorithmException ex: " + ex);
        } catch (InvalidAlgorithmParameterException ex) {
        	 logger.error("InvalidAlgorithmParameterException ex: " + ex);
        }
        KeyInfo keyInfo = null;
        KeyValue keyValue = null;
        KeyInfoFactory keyInfoFact = xmlSigFactory.getKeyInfoFactory();
        try {
            keyValue = keyInfoFact.newKeyValue(publicKey);
        } catch (KeyException ex) {
            logger.error("KeyException ex: " + ex);
        }
        keyInfo = keyInfoFact.newKeyInfo(Collections.singletonList(keyValue));
        
        // Create a new XML Signature
        XMLSignature xmlSignature = xmlSigFactory.newXMLSignature(signedInfo, keyInfo);
        try {
            // Sign the document
            xmlSignature.sign(domSignCtx);
            status = true;
        } catch (MarshalException ex) {
        	logger.error("MarshalException ex: " + ex);
        } catch (XMLSignatureException ex) {
        	logger.error("XMLSignatureException ex: " + ex);
        }
        logger.debug("signElement signing status: " + status);      
        return status;
    }
}