package com.bcmcgroup.flare.xmldsig;

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

import java.io.InputStream;
import java.io.Serializable;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
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
import javax.xml.crypto.dsig.dom.DOMValidateContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.KeyInfoFactory;
import javax.xml.crypto.dsig.keyinfo.X509Data;
import javax.xml.crypto.dsig.spec.C14NMethodParameterSpec;
import javax.xml.crypto.dsig.spec.SignatureMethodParameterSpec;
import javax.xml.crypto.dsig.spec.TransformParameterSpec;
import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.bcmcgroup.flare.client.ClientUtil;
import com.bcmcgroup.flare.xmldsig.MyURIDereferencer;

/** 
 * This class is used to provide convenient methods to digitally sign an XML document.
 * 
 * @author		David Du <ddu@bcmcgroup.com>
 * @author		Mark Walters <mwalters@bcmcgroup.com>
 * @version		2.0
 *
 */
public class Xmldsig {
	private static final Logger logger = Logger.getLogger(Xmldsig.class);
	private static final String RSA_SHA256_URI = "http://www.w3.org/2001/04/xmldsig-more#rsa-sha256";
    
    /**
     * Method used to create enveloped digital signatures for an for a TAXII Inbox_Message (Content_Block and entire Inbox_Message)
     *
     * @param doc Document object containing the source XML document
     * @param keyStorePath Path to KeyStore used for signing document
     * @param keyStorePW KeyStore password
     * @param keyName Alias for private key in KeyStore
     * @param keyPW Private key password
     * @return true if successful signing, false otherwise
     * 
     * Usage Example:
     *   String pks = config.getProperty("pathToPublisherKeyStore");
	 *	 String pksPw = FLAREclientUtil.decrypt(config.getProperty("publisherKeyStorePassword"));
	 *	 String pk = config.getProperty("publisherKeyName");
	 *	 String pkPw = FLAREclientUtil.decrypt(config.getProperty("publisherKeyPassword"));
	 *	 boolean result = Xmldsig.signInboxMessage(taxiiDocument, pks, pksPw, pk, pkPw);	
     */
   	public static boolean signInboxMessage(Document doc, String keyStorePath, String keyStorePW, String keyName, String keyPW) {
   		doc.normalizeDocument();
     	logger.debug("signInboxMessage doc: " + ClientUtil.convertDocumentToString(doc, true));
       	logger.debug("signInboxMessage keyStorePath: " + keyStorePath + " keyName: " + keyName);
        PrivateKeyEntry keyEntry = ClientUtil.getKeyEntry(keyStorePath, keyStorePW, keyName, keyPW);
        if (keyEntry == null) {
			logger.error("signInboxMessage: No key entry found for supplied keyName parameter!");
			return false;
		}
       	NodeList contentBlockList = doc.getDocumentElement().getElementsByTagName("Content_Block");
       	if (contentBlockList.getLength() == 0) {
		  	logger.error("signInboxMessage: <Content_Block> is missing!");
		  	return false;
       	} else {
           	logger.debug("signInboxMessage contentBlockList.getLength(): " + contentBlockList.getLength());
           	for (int i=0; i < contentBlockList.getLength(); i++) {
       		    Element contentBlock = (Element) contentBlockList.item(i);
       			if (!sign(contentBlock, keyEntry, i+1)) {
       				return false;
       			}
       		}
           	if (!sign(doc.getDocumentElement(), keyEntry, -1)) {
           		return false;
           	}
       	}
       	return true;
   	}
   	
   	/**
     * Method used to create an enveloped digital signature for an element of a TAXII document.
     *
     * @param element the element to be signed
     * @param keyEntry the PrivateKeyEntry
     * @param cbIndex the index of the Content_Block if we're signing a Content_Block, otherwise set to -1 if we're signing the root element
     * @return the status of the operation
     * 
     * Usage Example:
     *   String pks = config.getProperty("pathToPublisherKeyStore");
	 *	 String pksPw = FLAREclientUtil.decrypt(config.getProperty("publisherKeyStorePassword"));
	 *	 String keyName = config.getProperty("publisherKeyName");
	 *	 String keyPW = FLAREclientUtil.decrypt(config.getProperty("publisherKeyPassword"));
	 *   PrivateKeyEntry keyEntry =  FLAREclientUtil.getKeyEntry(pks, pksPw, keyName, keyPW);
	 *   List<Integer> statusList = Xmldsig.sign(rootElement, keyEntry, -1);
     */
	public static boolean sign(Element element, PrivateKeyEntry keyEntry, int cbIndex) {
		element.normalize();
    	boolean status = false;
    	
        //Create XML Signature Factory
        XMLSignatureFactory xmlSigFactory = XMLSignatureFactory.getInstance("DOM");
        PublicKey publicKey = ClientUtil.getPublicKey(keyEntry);
		PrivateKey privateKey = keyEntry.getPrivateKey();
        DOMSignContext dsc = new DOMSignContext(privateKey, element);
        dsc.setDefaultNamespacePrefix("ds");
        dsc.setURIDereferencer(new MyURIDereferencer(element));
        SignedInfo si = null;
        DigestMethod dm = null;
        SignatureMethod sm = null;
        KeyInfo ki = null;
        X509Data xd = null;
        List<Serializable> x509Content = new ArrayList<Serializable>();
        try {
        	String algorithm = publicKey.getAlgorithm();
        	X509Certificate cert = (X509Certificate) keyEntry.getCertificate();
        	x509Content.add(cert.getSubjectX500Principal().getName());
            x509Content.add(cert);
			String algorithmName = cert.getSigAlgName();
			logger.debug("sign algorithm: " + algorithm);
			logger.debug("sign Digest algorithmName: " + algorithmName);
        	if (algorithm.toUpperCase().contains("RSA")) {
        		if (algorithmName.toUpperCase().contains("SHA1")) {
        			dm = xmlSigFactory.newDigestMethod(DigestMethod.SHA1, null);
		    		sm = xmlSigFactory.newSignatureMethod(SignatureMethod.RSA_SHA1, null);
		        } else if (algorithmName.toUpperCase().contains("SHA2")) {
		        	dm = xmlSigFactory.newDigestMethod(DigestMethod.SHA256, null);
			       	sm = xmlSigFactory.newSignatureMethod(RSA_SHA256_URI, (SignatureMethodParameterSpec) null);
		        } else {
		        	logger.error("sign Digest method: " + algorithmName + " is not supported");
		        }
		        CanonicalizationMethod cm = null;
		        if (cbIndex != -1) {
		        	cm = xmlSigFactory.newCanonicalizationMethod(CanonicalizationMethod.INCLUSIVE_WITH_COMMENTS, (C14NMethodParameterSpec) null);
		        	String refUri = "#xpointer(//*[local-name()='Content_Block'][" + cbIndex + "]/*[local-name()='Content'][1]/*)";
		        	List<Reference> references = Collections.singletonList(xmlSigFactory.newReference(refUri, dm));
		        	si = xmlSigFactory.newSignedInfo(cm, sm, references);
		        } else {
		        	List<Transform> transforms = new ArrayList<Transform>(2);
					transforms.add(xmlSigFactory.newTransform(Transform.ENVELOPED, (TransformParameterSpec) null));
			        transforms.add(xmlSigFactory.newTransform(CanonicalizationMethod.EXCLUSIVE, (TransformParameterSpec) null));
		        	cm = xmlSigFactory.newCanonicalizationMethod(CanonicalizationMethod.EXCLUSIVE, (C14NMethodParameterSpec) null);
		        	String refUri = "#xpointer(/*)";
		        	List<Reference> references = Collections.singletonList(xmlSigFactory.newReference(refUri, dm, transforms, null, null));
		        	si = xmlSigFactory.newSignedInfo(cm, sm, references);
		        }
		        KeyInfoFactory kif = xmlSigFactory.getKeyInfoFactory();
		        xd = kif.newX509Data(x509Content);
		        ki = kif.newKeyInfo(Collections.singletonList(xd));
	        } else {
	        	logger.error("sign Algorithm: " + algorithm + " is not supported");
	        }
        } catch (NoSuchAlgorithmException ex) {
        	 logger.error("sign: NoSuchAlgorithmException ex: " + ex);
        } catch (InvalidAlgorithmParameterException ex) {
        	 logger.error("sign: InvalidAlgorithmParameterException ex: " + ex);
        }
        
        // Create a new XML Signature
        XMLSignature signature = xmlSigFactory.newXMLSignature(si, ki);
        try {
            // Sign the document
            signature.sign(dsc);
            status = true;
        } catch (MarshalException ex) {
        	logger.error("sign:  MarshalException ex: " + ex);
        } catch (XMLSignatureException ex) {
        	logger.error("sign:  XMLSignatureException ex: " + ex);
        } catch (Exception e) {
        	logger.error("sign:  Exception e: " + e);
        }
        logger.debug("sign: signing status: " + status);      
        return status;
	}
	
	/**
	 * Used to verify an enveloped digital signature
	 * 
	 * @param doc a Document object containing the xml with the signature
	 * @param keyStorePath a String containing the path to the KeyStore
	 * @param keyStorePW a String containing the KeyStore password
	 * @param verifyAlias a String containing the alias of the public key used for verification
	 * @return
	 */
	public static boolean verifySignature(Document doc, String keyStorePath, String keyStorePW, String verifyAlias) {
    	logger.debug("verifySignature keyStorePath: " + keyStorePath + " verifyAlias: " + verifyAlias);
        boolean coreValidation = false;
  	    logger.debug("verifySignature doc: " + ClientUtil.convertDocumentToString(doc, true));
		PublicKey publicKey = ClientUtil.getPublicKeyByAlias(keyStorePath, keyStorePW, verifyAlias);
	    try {
			NodeList nl = doc.getElementsByTagNameNS(XMLSignature.XMLNS, "Signature");
			if (nl.getLength() == 0) {
				logger.error("Error: No XML Digital Signature Found, document is discarded...");
				return false;
			}
			logger.debug("nl.getLength() = " + nl.getLength());
			Node signatureNode = nl.item(nl.getLength()-1);
			logger.debug("signatureNode.getParentNode() = " + signatureNode.getParentNode().getLocalName());
			DOMValidateContext valContext = new DOMValidateContext(publicKey, signatureNode);
			valContext.setURIDereferencer(new MyURIDereferencer(signatureNode.getParentNode()));
			XMLSignatureFactory fac = XMLSignatureFactory.getInstance("DOM");
			XMLSignature signature = fac.unmarshalXMLSignature(valContext);
			coreValidation = signature.validate(valContext);
			logger.debug("verifySignature coreValidation: " + coreValidation);
			if (!coreValidation) {
				// for testing/debugging when validation fails...
				logger.error("verifySignature failed core validation");
				boolean signatureValidation = signature.getSignatureValue().validate(valContext);
				logger.debug("verifySignature signature validation: " + signatureValidation);
				@SuppressWarnings("rawtypes")
				Iterator i = signature.getSignedInfo().getReferences().iterator();
				for (int j = 0; i.hasNext(); j++) {
					Reference ref = (Reference) i.next();
					logger.debug("verifySignature ref: " + ref);
					boolean referenceValidation = ref.validate(valContext);
					logger.debug("referenceValidation[" + j + "]: " + referenceValidation + ", reference URI = " + ref.getURI());
					byte[] calculatedDigestValue = ref.getCalculatedDigestValue();
					byte[] digestValue = ref.getDigestValue();
					String cdvString = new String(Base64.encodeBase64(calculatedDigestValue));
					String dvString = new String(Base64.encodeBase64(digestValue));
					logger.debug("verifySignature getCalculatedDigestValue: " + cdvString);
					logger.debug("verifySignature getDigestValue: " + dvString);
					InputStream digestInputStream = ref.getDigestInputStream();
					if (digestInputStream != null) {
						logger.debug("verifySignature getDigestInputStream: " + ClientUtil.convertStreamToString(digestInputStream));
					} else {
						logger.debug("verifySignature getDigestInputStream: null");
					}
				}
			}
		} catch (MarshalException e) {
			logger.error("verifySignature MarshalException: " + e);
		} catch (XMLSignatureException e) {
			logger.error("verifySignature XMLSignatureException: " + e);
		}
		return coreValidation;
    }
}