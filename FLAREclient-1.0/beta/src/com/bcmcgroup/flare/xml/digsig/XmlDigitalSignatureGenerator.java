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
import java.security.KeyStore;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;
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
import javax.xml.crypto.dsig.keyinfo.X509Data;
import javax.xml.crypto.dsig.spec.C14NMethodParameterSpec;
import javax.xml.crypto.dsig.spec.SignatureMethodParameterSpec;
import javax.xml.crypto.dsig.spec.TransformParameterSpec;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.bcmcgroup.flare.client.FlareClientUtil;

/** 
 * This class is used to provide convenient methods to digitally sign an XML document.
 * 
 * @author		David Du <ddu@bcmcgroup.com>
 * @version		1.1
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
    	logger.debug("getXmlDocument xmlFilePath: " + xmlFilePath);
        Document doc = null;
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        try {
            doc = dbf.newDocumentBuilder().parse(new FileInputStream(xmlFilePath));
        } catch (ParserConfigurationException ex) {
            logger.error("ParserConfigurationException ex: " + ex);
        } catch (FileNotFoundException ex) {
            logger.error("FileNotFoundException ex: " + ex);
        } catch (SAXException ex) {
            logger.error("SAXException ex: " + ex);
        } catch (IOException ex) {
            logger.error("IOException ex: " + ex);
        }
        return doc;
    }
 
    /**
     * Method used to create an enveloped digital signature for an XML document
     *
     * @param srcXmlFilePath String containing the source XML document
     * @param keyStorePath Path to KeyStore used for signing document
     * @param keyStorePW KeyStore password
     * @param keyName Alias for private key in KeyStore
     * @param keyPW Private key password
     * @return Signed Document
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
	public static Document generateXmlDigitalSignatureEnveloped(String srcXmlStr, String keyStorePath, String keyStorePW, String keyName, String keyPW) {
        // Load the KeyStore and get the signing key and certificate.
    	KeyStore ks;
    	PrivateKeyEntry keyEntry = null;
		try {
			ks = KeyStore.getInstance("JKS");
			ks.load(new FileInputStream(keyStorePath), keyStorePW.toCharArray());
			keyEntry = (KeyStore.PrivateKeyEntry) ks.getEntry(keyName, new KeyStore.PasswordProtection(keyPW.toCharArray()));
		} catch (KeyStoreException ex) {
			logger.error("KeyStoreException ex: " + ex);
		} catch (NoSuchAlgorithmException ex) {
			logger.error("NoSuchAlgorithmException ex: " + ex);
		} catch (CertificateException ex) {
			logger.error("CertificateException ex: " + ex);
		} catch (IOException ex) {
			logger.error("IOException ex: " + ex);
		} catch (UnrecoverableEntryException ex) {
			logger.error("UnrecoverableEntryException ex: " + ex);
		}
    	
		X509Certificate cert = (X509Certificate) keyEntry.getCertificate();
		PublicKey pkey = cert.getPublicKey();
		String algorithm = pkey.getAlgorithm();
		String algorithmName = cert.getSigAlgName();
		logger.debug("generateXmlDigitalSignature algorithm: " + algorithm);
		logger.debug("generateXmlDigitalSignature algorithmName: " + algorithmName);
        
		// Create reference and signed info
        Reference ref = null;
        SignedInfo si = null;
        XMLSignatureFactory xmlsf = XMLSignatureFactory.getInstance("DOM");
        try {
        	DigestMethod dm = null;
        	SignatureMethod sm = null;
        	if (algorithmName.contains("SHA-1")) {
        		dm = xmlsf.newDigestMethod(DigestMethod.SHA1, null);
	            sm = xmlsf.newSignatureMethod(SignatureMethod.RSA_SHA1, null);
        	} else {
        		dm = xmlsf.newDigestMethod(DigestMethod.SHA256, null);
        		sm =  xmlsf.newSignatureMethod(RSA_SHA256_URI, (SignatureMethodParameterSpec) null);
        	}
    		List transforms = Collections.singletonList(xmlsf.newTransform(Transform.ENVELOPED, (TransformParameterSpec) null));
            ref = xmlsf.newReference("", dm, transforms, null, null);
            CanonicalizationMethod cm = xmlsf.newCanonicalizationMethod(CanonicalizationMethod.INCLUSIVE, (C14NMethodParameterSpec) null);
            List references = Collections.singletonList(ref);
            si = xmlsf.newSignedInfo(cm, sm, references);
        } catch (NoSuchAlgorithmException ex) {
        	 logger.error("NoSuchAlgorithmException ex: " + ex);
        } catch (InvalidAlgorithmParameterException ex) {
        	 logger.error("InvalidAlgorithmParameterException ex: " + ex);
        }
        
    	
    	// Create the KeyInfo containing the X509Data.
    	KeyInfoFactory kif = xmlsf.getKeyInfoFactory();
    	List x509Content = new ArrayList();
    	x509Content.add(cert.getSubjectX500Principal().getName());
    	x509Content.add(cert);
    	X509Data xd = kif.newX509Data(x509Content);
    	KeyInfo ki = kif.newKeyInfo(Collections.singletonList(xd));

    	// Sign and return the document
    	Document doc = FlareClientUtil.convertStringToDocument(srcXmlStr);
        DOMSignContext dsc = new DOMSignContext(keyEntry.getPrivateKey(), doc.getDocumentElement());
        XMLSignature signature = xmlsf.newXMLSignature(si, ki);
        try {
            signature.sign(dsc);
        } catch (MarshalException ex) {
            logger.error("MarshalException ex: " + ex);
        } catch (XMLSignatureException ex) {
        	logger.error("XMLSignatureException ex: " + ex);
        }
        return doc;
    }
    
    /**
     * Method used to create a detached digital signature for an XML document
     *
     * @param srcXmlStr String containing the source XML document
     * @param keyStorePath Path to KeyStore used for signing document
     * @param keyStorePW KeyStore password
     * @param keyName Alias for private key in KeyStore
     * @param keyPW Private key password
     * @return Node containing Signature element
     */
	public static List<Integer> generateXmlDigitalSignatureDetached(String srcXmlStr, String keyStorePath, String keyStorePW, String keyName, String keyPW) {
		logger.debug("generateXmlDigitalSignatureDetached keyStorePath: " + keyStorePath + " keyName: " + keyName);
		logger.debug("generateXmlDigitalSignatureDetached keyStorePW: " + keyStorePW + " keyPW: " + keyPW);
		if (srcXmlStr == null || srcXmlStr.isEmpty()) {
			return null;
		}
		Document doc = FlareClientUtil.convertStringToDocument(srcXmlStr);
  	    return generateXmlDigitalSignatureDetached(doc, keyStorePath, keyStorePW, keyName, keyPW);
    }
	
    /**
     * Method used to create a digital signature for a document
     *
     * @param doc Document containing the source XML document
     * @param keyStorePath Path to KeyStore used for signing document
     * @param keyStorePW KeyStore password
     * @param keyName Alias for private key in KeyStore
     * @param keyPW Private key password
     * @return a list of status: -1 exception occurred; 0: algorithm is not RSA(SHA1 or SHA256); 1: success 
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
	public static List<Integer> generateXmlDigitalSignatureDetached(Document doc, String keyStorePath, String keyStorePW, String keyName, String keyPW) {
  	  logger.debug("generateXmlDigitalSignatureDetached   doc: " + FlareClientUtil.convertDocumentToString(doc));
    	
    	logger.debug("generateXmlDigitalSignatureDetached keyStorePath: " + keyStorePath + " keyName: " + keyName);
    	logger.debug("generateXmlDigitalSignatureDetached keyStorePW: " + keyStorePW + " keyPW: " + keyPW);
    	List<Integer> statusList = new ArrayList<Integer>();
    	    	
    	//doc.normalizeDocument();
    	NodeList contentList = doc.getDocumentElement().getElementsByTagName("Content");
    	logger.debug("generateXmlDigitalSignatureDetached contentList.getLength(): " + contentList.getLength());
		for (int ii=0; ii < contentList.getLength(); ii++) {
		    Element content = (Element) contentList.item(ii);
		  	if (content == null) {
		  		logger.error("generateXmlDigitalSignatureDetached Wrong document, Content tag is missing... ");
		  	    continue;
		  	}
		  	  
		  	// Create a factory that will be used to generate the signature structures
	        XMLSignatureFactory xmlsf = XMLSignatureFactory.getInstance("DOM", new org.apache.jcp.xml.dsig.internal.dom.XMLDSigRI());
	  	    content.setAttribute("myID", "contentID" + (ii+1));
	  	    content.setIdAttribute("myID", true); 
		    Node parent = content.getParentNode(); // signature tag will be under this node
	     
		    // Load the KeyStore and get the signing key and certificate.
	    	KeyStore ks;
	    	PrivateKeyEntry keyEntry = null;
	    	FileInputStream is = null;
			try {
				ks = KeyStore.getInstance("JKS");
				is = new FileInputStream(keyStorePath);
				ks.load(is, keyStorePW.toCharArray());
				logger.debug("generateXmlDigitalSignatureDetach keyStorePW: " + keyStorePW);
				
				keyEntry = (KeyStore.PrivateKeyEntry) ks.getEntry(keyName, new KeyStore.PasswordProtection(keyPW.toCharArray()));
				logger.debug("generateXmlDigitalSignatureDetach keyEntry: " + keyEntry);
			} catch (FileNotFoundException e) {
				logger.error("generateXmlDigitalSignatureDetach FileNotFoundException e: " + e);
				statusList.add(-1);
				return statusList;
			} catch (IOException e) {
				logger.error("generateXmlDigitalSignatureDetach IOException e: " + e);
				statusList.add(-1);
				return statusList;
			} catch (KeyStoreException e) {
				logger.error("generateXmlDigitalSignatureDetach KeyStoreException e: " + e);
				statusList.add(-1);
				return statusList;
			} catch (NoSuchAlgorithmException e) {
				logger.error("generateXmlDigitalSignatureDetach NoSuchAlgorithmException e: " + e);
				statusList.add(-1);
				return statusList;
			} catch (CertificateException e) {
				logger.error("generateXmlDigitalSignatureDetach CertificateException e: " + e);
				statusList.add(-1);
				return statusList;
			} catch (UnrecoverableEntryException e) {
				logger.error("generateXmlDigitalSignatureDetach UnrecoverableEntryException e: " + e);
				statusList.add(-1);
				return statusList;
			} finally {
				if (is != null) {
					try {
				       is.close();
					} catch (IOException ioe) {
						logger.error("generateXmlDigitalSignatureDetach IOException ioe: " + ioe);
					}
				}
			}
			if (keyEntry != null) {
				X509Certificate cert = (X509Certificate) keyEntry.getCertificate();
				PublicKey pkey = cert.getPublicKey();
				String algorithm = pkey.getAlgorithm();
				String algorithmName = cert.getSigAlgName();
				logger.debug("generateXmlDigitalSignature algorithm: " + algorithm);
				logger.debug("generateXmlDigitalSignature Digest algorithmName: " + algorithmName);
		        
				// Create reference and signed info
		        SignedInfo si = null;
		        DigestMethod dm = null;
		        SignatureMethod sm = null;
		        Reference ref = null;
		        try {
		        	if (algorithm.toUpperCase().contains("RSA")) {
			        	if (algorithmName.toUpperCase().contains("SHA1")) {
			        		dm = xmlsf.newDigestMethod(DigestMethod.SHA1, null);
			    		    sm = xmlsf.newSignatureMethod(SignatureMethod.RSA_SHA1, null);
			        	} else if (algorithmName.toUpperCase().contains("SHA2")) {
			        		dm = xmlsf.newDigestMethod(DigestMethod.SHA256, null);
				       	    sm = xmlsf.newSignatureMethod(RSA_SHA256_URI, (SignatureMethodParameterSpec) null);
			        	} else {
			        		logger.error("generateXmlDigitalSignatureDetach Digest method: " + algorithmName + " is not supported");
			        		statusList.add(0);
			        		continue;
			        	}
		        		List transforms = Collections.singletonList(xmlsf.newTransform(CanonicalizationMethod.EXCLUSIVE, (TransformParameterSpec) null));
			            ref = xmlsf.newReference("#contentID" + (ii+1), dm, transforms, null, null);
			            CanonicalizationMethod cm = xmlsf.newCanonicalizationMethod(CanonicalizationMethod.EXCLUSIVE, (C14NMethodParameterSpec) null);
			            List references = Collections.singletonList(ref);
		    		    si = xmlsf.newSignedInfo(cm, sm, references);
		        	} else {
		        		logger.error("generateXmlDigitalSignatureDetach Algorithm: " + algorithm + " is not supported");
		        		statusList.add(0);
		        		continue;
		        	}
		        } catch (NoSuchAlgorithmException ex) {
		        	logger.error("generateXmlDigitalSignatureDetach NoSuchAlgorithmException ex: " + ex);
		        	statusList.add(-1);
					return statusList;
		        } catch (InvalidAlgorithmParameterException ex) {
		        	logger.error("generateXmlDigitalSignatureDetach InvalidAlgorithmParameterException ex: " + ex);
		        	statusList.add(-1);
					return statusList;
		        }
		    	
		    	// Create the KeyInfo containing the X509Data.
		    	KeyInfoFactory kif = xmlsf.getKeyInfoFactory();
		    	List x509Content = new ArrayList();
		    	x509Content.add(cert.getSubjectX500Principal().getName());
		    	x509Content.add(cert);
		    	X509Data xd = kif.newX509Data(x509Content);
		    	KeyInfo ki = kif.newKeyInfo(Collections.singletonList(xd));    	        
			    
		    	// Sign the document
		        DOMSignContext dsc = new DOMSignContext(keyEntry.getPrivateKey(), parent);
		        dsc.setDefaultNamespacePrefix("ds");
		        XMLSignature signature = xmlsf.newXMLSignature(si, ki);
		        try {
		            signature.sign(dsc);
		            statusList.add(1);
		        } catch (MarshalException ex) {
		            logger.error("generateXmlDigitalSignatureDetach MarshalException ex: " + ex);
		            statusList.add(-1);
		        } catch (XMLSignatureException ex) {
		        	logger.error("generateXmlDigitalSignatureDetach XMLSignatureException ex: " + ex);
		        	statusList.add(-1);
		        }
		        logger.debug("generateXmlDigitalSignature doc: " + FlareClientUtil.convertDocumentToString(doc));
			} else {
				statusList.add(-1);
				logger.error("generateXmlDigitalSignatureDetach no signaure sighed, since no key entry found for keyName: " + keyName);
			}
			content.removeAttribute("myID");
		}
		return statusList;
    }
}
