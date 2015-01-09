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

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.Charset;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.UnrecoverableEntryException;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;

import org.xml.sax.InputSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import javax.net.ssl.HttpsURLConnection;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.mitre.stix.validator.SchemaError;
import org.mitre.stix.validator.StixValidator;
import org.w3c.dom.Attr;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.SAXException;

/** 
 * @author		David Du <ddu@bcmcgroup.com>
 * @author		Mark Walters <mwalters@bcmcgroup.com>
 * @version		2.0
 */
public class ClientUtil {
	private static final Logger logger = Logger.getLogger(ClientUtil.class);
    private static final char[] seeds = "enfldsgbnlsngdlksdsgm".toCharArray();
    private static byte[] ivBytes = "0102030405060708".getBytes();
    private static String salt ="a9v5n38s";
    private static int pswdIterations = 65536;
    private static int keySize = 256;
    private static ArrayList<String> stixVersions = new ArrayList<String>(Arrays.asList("1.0", "1.0.1", "1.1", "1.1.1"));
     
    /**
     * Save the value of a property for a specific property name
     * @param propName the property name with its value to be set in the config.properties
     * @param value the value to be set in the config.properties
     * 
     * Usage example:  FLAREclientUtil.changeProperty(propName, value);
     */
    public static void changeProperty(String propName, String value) {
        Properties props = new Properties();
        InputStream is = null;
        OutputStream out = null;
        File f = new File("config.properties");
        try {
            is = new FileInputStream(f);
            props.load(is);
            props.setProperty(propName, value);
            out = new FileOutputStream(f);
            props.store(out, "");
        } catch (IOException e) {
        	logger.error("IOException e: " + e.getMessage());
        } finally {
        	if (is != null) {
        		try {
        		    is.close();
        		} catch (IOException e) {
        			logger.error("IOException e: " + e.getMessage());
        		}
        	}
        	if (out != null) {
        		try {
        			out.close();
        		} catch (IOException e) {
        			logger.error("IOException e: " + e.getMessage());
        		}
        	}
        }
    }
    
    /**
     * Convert a Document into a byte array
     * 
     * @param node the document to be converted to bytes
     * @return the byte array converted from the node
     * 
     * Usage example: 
     *   byte[] bytes = FLAREclientUtil.convertDocumentToBytes(node);
     */
    public static byte[] convertDocumentToBytes(Node node) {
    	try {
    		Source source = new DOMSource(node);
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			StreamResult result = new StreamResult(bos);
			TransformerFactory tf = TransformerFactory.newInstance();
			tf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
			Transformer t = tf.newTransformer();
			t.transform(source, result);
			return bos.toByteArray();
		} catch (TransformerException e) {
			logger.error("convertDocumentToBytes e == " + e.getMessage());
			logger.error("convertDocumentToBytes stack: " + getStackTrace(e));
		}
		return null;
	}
    
    /**
     * Convert a Document into a String
     * 
     * @param doc the Document to be converted to String
     * @param omitXmlDeclaration set to true if you'd like to omit the XML declaration, false otherwise
     * @return the String converted from doc
     * 
     * Usage example: 
     *   String nodeStr = FLAREclientUtil.convertDocumentToString(doc);
     */
    public static String convertDocumentToString(Document doc, boolean omitXmlDeclaration) {
		try {
			StringWriter sw = new StringWriter();
			TransformerFactory tf = TransformerFactory.newInstance();
			tf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
			Transformer t = tf.newTransformer();
			if (omitXmlDeclaration) {
				t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
			} else {
				t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
			}
			t.setOutputProperty(OutputKeys.METHOD, "xml");
			t.setOutputProperty(OutputKeys.INDENT, "yes");
			t.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
			t.transform(new DOMSource(doc), new StreamResult(sw));
			return sw.toString();
		} catch (TransformerException e) {
			logger.error("convertDocumentToString TransformerException e == " + e.getMessage());
		}
		return null;
    }
    
    private static String convertNodeToString(Node node) {
    	StringWriter sw = new StringWriter();
    	try {
    		TransformerFactory tf = TransformerFactory.newInstance();
    		tf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
    		Transformer t = tf.newTransformer();
    		t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
    		t.setOutputProperty(OutputKeys.INDENT, "yes");
			t.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
    		t.transform(new DOMSource(node), new StreamResult(sw));
        	return sw.toString();
    	} catch (TransformerException e) {
    		logger.error("convertNodeToString TransformerException e == " + e.getMessage());
    	}
    	return null;
    }
    
    /**
     * Convert an input stream to a String
     * 
     * @param is the input stream to be converted into String
     * @return the converted string result 
     * 
     * Usage example:
     *   String digestStr = FLAREclientUtil.convertStreamToString(refIS);
     */
    public static String convertStreamToString(InputStream is) {
		logger.info("convertStreamToString is == " + is);
	        /*
	         * To convert the InputStream to String we use the
	         * Reader.read(char[] buffer) method. We iterate until the
	         * Reader return -1 which means there's no more data to
	         * read. We use the StringWriter class to produce the string.
	        */
	        if (is != null) {
	            Writer writer = new StringWriter();	
	            char[] buffer = new char[1024];
	            try {
	            	Reader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
		            int n;
		            while ((n = reader.read(buffer)) != -1) {
		                   writer.write(buffer, 0, n);
		            }
	            } catch (UnsupportedEncodingException e) {
	            	logger.info("Exception e: " + e.getMessage());
	            } catch (IOException e) {
	            	logger.info("IOException e: " + e.getMessage());
	            } finally {	            	
	            	try {
	            		is.close();
	            	} catch(IOException e) {
	            		logger.error("IOException close e: " + e.getMessage());
	            	}
	            }
	            return writer.toString();
	        } else {       
	           return "";
	        }
	    }
    
    /**
     * Convert xml String into a document
     * 
     * @param xml the xml string to be converted to document
     * @return the document converted from the xml string
     * 
     * Usage example:
     *   Document doc = FLAREclientUtil.convertStringToDocument(xmlString);
     */
    public static Document convertStringToDocument(String xmlString) {
		try {
			DocumentBuilder db = ClientUtil.generateDocumentBuilder();
			Document document = db.parse(new InputSource(new StringReader(xmlString)));
			return document;
		 } catch (SAXException e) {
			 logger.error("convertStringToDocument SAXException e == " + e.getMessage());
		 } catch (IOException e) {
			 logger.error("convertStringToDocument IOException e == " + e.getMessage());
		 }
		 return null;
	 }
    
    /**
     * Decrypt a previously encrypted password to plain text
     * 
     * @param encryptedText the text String to be decrypted
     * @return the decrypted String
     * 
     * Usage example: 
     *   String pksPw = FLAREclientUtil.decrypt("9cBB801VfbB95YRdi0fNg");
     */
    public static String decrypt(String encryptedText) {
    	try {
    		byte[] saltBytes = salt.getBytes("UTF-8");
			byte[] encryptedTextBytes = Base64.decodeBase64(encryptedText);
		    SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
		    PBEKeySpec spec = new PBEKeySpec(seeds, saltBytes, pswdIterations, keySize);
		    SecretKey secretKey = factory.generateSecret(spec);
		    SecretKeySpec secret = new SecretKeySpec(secretKey.getEncoded(), "AES");
		     
	        // Decrypt the message, given derived key and initialization vector.
	        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
	        cipher.init(Cipher.DECRYPT_MODE, secret, new IvParameterSpec(ivBytes));	     
	        byte[] decryptedTextBytes = null;
	        try {
	            decryptedTextBytes = cipher.doFinal(encryptedTextBytes);
	        } catch (IllegalBlockSizeException e) {
	        	logger.error("IllegalBlockSizeException e: " + e.getMessage());
	        } catch (BadPaddingException e) {
	        	logger.error("BadPaddingException e: " + e.getMessage());
	        }    
	        return new String(decryptedTextBytes);
	    } catch (NoSuchAlgorithmException e) {
			logger.error("NoSuchAlgorithmException e: " + e.getMessage());
		} catch (InvalidKeySpecException e) {
			logger.error("InvalidKeySpecException e: " + e.getMessage());
		} catch (NoSuchPaddingException e) {
			logger.error("NoSuchPaddingException e: " + e.getMessage());
		} catch (UnsupportedEncodingException e) {
			logger.error("UnsupportedEncodingException e: " + e.getMessage());
		} catch (InvalidKeyException e) {
			logger.error("InvalidKeyException e: " + e.getMessage());
		} catch (InvalidAlgorithmParameterException e) {
			logger.error("InvalidAlgorithmParameterException e: " + e.getMessage());
		}
		return null;
    }
    
    /**
     * Encrypt a plain text password in AES
     * 
     * @param plainText the String text to be encrypted in AES
     * @return the encrypted text String
     *  
     * Usage example:
     *   String encryptedPassword = FLAREclientUtil.encrypt("clearTextPassword");
     */
    public static String encrypt(String plainText) {  
    	try {
	        byte[] saltBytes = salt.getBytes("UTF-8");              
	        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
	        PBEKeySpec spec = new PBEKeySpec(seeds, saltBytes, pswdIterations, keySize);
	        SecretKey secretKey = factory.generateSecret(spec);
	        SecretKeySpec secret = new SecretKeySpec(secretKey.getEncoded(), "AES");
	        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
	        cipher.init(Cipher.ENCRYPT_MODE, secret, new IvParameterSpec(ivBytes));
	        byte[] encryptedTextBytes = cipher.doFinal(plainText.getBytes("UTF-8"));
	        return new Base64().encodeAsString(encryptedTextBytes);
    	} catch (NoSuchAlgorithmException e) {
    		logger.error("NoSuchAlgorithmException e: " + e.getMessage());
    	} catch (InvalidKeySpecException e) {
    		logger.error("InvalidKeySpecException e: " + e.getMessage());
    	} catch (NoSuchPaddingException e) {
    		logger.error("NoSuchPaddingException e: " + e.getMessage());
    	} catch (IllegalBlockSizeException e) {
    		logger.error("IllegalBlockSizeException e: " + e.getMessage());
    	} catch (BadPaddingException e) {
    		logger.error("BadPaddingException e: " + e.getMessage());
    	} catch (UnsupportedEncodingException e) {
    		logger.error("UnsupportedEncodingException e: " + e.getMessage());
    	} catch (InvalidKeyException e) {
    		logger.error("InvalidKeyException e: " + e.getMessage());
    	} catch (InvalidAlgorithmParameterException e) {
    		logger.error("InvalidAlgorithmParameterException e: " + e.getMessage());
    	}
    	return null;    
    }
     
    /**
     * Fetch private key from KeyStore
     * 
     * @param keyStorePath a String containing the path to the KeyStore
     * @param keyStorePW a String containing the KeyStore password
     * @param keyName a String containing the alias of targeted certificate
     * @param keyPW a String containing the key password
     * @return The PrivateKeyEntry object containing the targeted private key
     * 
     * Usage example:
     *   String keyStorePath = "/path/to/myKeyStore.jks";
     *   String keyStorePW = "passwordForMyKeyStore";
     *   String keyName = "FLAREclient";
     *   String keyPW = "passwordForMyKey";
     *   PrivateKeyEntry myKey = FLAREclientUtil.getKeyEntry(keyStorePath, keyStorePW, keyName, keyPW);
     */    
    public static PrivateKeyEntry getKeyEntry(String keyStorePath, String keyStorePW, String keyName, String keyPW) {
    	KeyStore ks;
    	PrivateKeyEntry keyEntry = null;
    	FileInputStream is = null;
		try {
			ks = KeyStore.getInstance("JKS");
			is = new FileInputStream(keyStorePath);
			ks.load(is, keyStorePW.toCharArray());
			logger.debug("getKeyEntry keyStorePW: " + keyStorePW);
			keyEntry = (KeyStore.PrivateKeyEntry) ks.getEntry(keyName, new KeyStore.PasswordProtection(keyPW.toCharArray()));
			logger.debug("getKeyEntry keyEntry: " + keyEntry);
		} catch (FileNotFoundException e) {
			logger.error("getKeyEntry FileNotFoundException e: " + e);
		} catch (IOException e) {
			logger.error("getKeyEntry IOException e: " + e);
		} catch (KeyStoreException e) {
			logger.error("getKeyEntry KeyStoreException e: " + e);
		} catch (NoSuchAlgorithmException e) {
			logger.error("getKeyEntry NoSuchAlgorithmException e: " + e);
		} catch (CertificateException e) {
			logger.error("getKeyEntry CertificateException e: " + e);
		} catch (UnrecoverableEntryException e) {
			logger.error("getKeyEntry UnrecoverableEntryException e: " + e);
		} finally {
			if (is != null) {
				try {
			       is.close();
				} catch (IOException ioe) {
					logger.error("getKeyEntry IOException ioe: " + ioe);
				}
			}
		}
    	return keyEntry;
    }
    
    /**
     * constructs a DocumentBuilder object for XML documents
     * 
     * @return DocumentBuilder object with the proper initializations
     */
    public static DocumentBuilder generateDocumentBuilder() {
    	try {
	    	DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			dbf.setNamespaceAware(true);
			dbf.setIgnoringElementContentWhitespace(true);
			dbf.setIgnoringComments(true);
			dbf.setFeature("http://xml.org/sax/features/external-general-entities", false);
			dbf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
			dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
			DocumentBuilder db = dbf.newDocumentBuilder();
			return db;
    	} catch (ParserConfigurationException e) {
    		logger.error("ParserConfigurationException : " + e.getMessage());
    	}
    	return null;
    }
    
    /**
     * Fetch public key (certificate) from PrivateKeyEntry object
     * 
     * @param keyEntry a PrivateKeyEntry object containing key of interest
     * @return The PublicKey object containing the targeted public key
     * 
     * Usage example:
     *   PublicKey myKey = FLAREclientUtil.getPublicKey(privateKeyEntry);
     */
    public static PublicKey getPublicKey(PrivateKeyEntry keyEntry) {
    	if (keyEntry != null) {
    		X509Certificate cert = (X509Certificate) keyEntry.getCertificate();
			PublicKey pkey = cert.getPublicKey();
			String algorithm = pkey.getAlgorithm();
			String algorithmName = cert.getSigAlgName();
			logger.debug("getPublicKey algorithm: " + algorithm);
			logger.debug("getPublicKey Digest algorithmName: " + algorithmName);
			return pkey;
		}
    	return null;
    }
    
    /**
     * Fetch public key (certificate) from KeyStore
     * 
     * @param keyStorePath a String containing the path to the KeyStore
     * @param keyStorePW a String containing the KeyStore password
     * @param hubAlias a String containing the alias of targeted certificate
     * @return The PublicKey object containing the targeted public key
     * 
     * Usage example:
     *   String keyStorePath = "/path/to/myKeyStore.jks";
     *   String keyStorePW = "passwordForMyKeyStore";
     *   String hubAlias = "FLAREhub";
     *   PublicKey hubCertificate = FLAREclientUtil.getPublicKeyByAlias(keyStorePath, keyStorePW, hubAlias);
     */    
    public static PublicKey getPublicKeyByAlias(String keyStorePath, String keyStorePW, String hubAlias) {
    	logger.debug("getPublicKeyByAlias keyStorePath: " + keyStorePath + " keyStorePW: " + keyStorePW + " hubAlias: " + hubAlias);
    	KeyStore ks;
     	FileInputStream is  = null;
 		try {
 			ks = KeyStore.getInstance("JKS");
 			is = new FileInputStream(keyStorePath);
 			ks.load(is, keyStorePW.toCharArray());
 			Certificate certificate = ks.getCertificate(hubAlias);
 			logger.debug("getPublicKeyByAlias certificate: " + certificate);			
 			if (certificate != null) {
 				PublicKey pkey = certificate.getPublicKey();
 			    logger.debug("getPublicKeyByAlias pkey: " + pkey);
 			    logger.debug("getPublicKeyByAlias algorithm: " + pkey.getAlgorithm());
 			    return pkey;
 			}
 		} catch (FileNotFoundException e) {
			logger.error("getPublicKeyByAlias FileNotFoundException e: " + e);
		} catch (IOException e) {
			logger.error("getPublicKeyByAlias IOException e: " + e);
		} catch (KeyStoreException e) {
			logger.error("getPublicKeyByAlias KeyStoreException e: " + e);
		} catch (NoSuchAlgorithmException e) {
			logger.error("getPublicKeyByAlias NoSuchAlgorithmException e: " + e);
		} catch (CertificateException e) {
			logger.error("getPublicKeyByAlias CertificateException e: " + e);
		} finally {
			if (is != null) {
				try {
			       is.close();
				} catch (IOException ioe) {
					logger.error("getPublicKeyByAlias IOException ioe: " + ioe);
				}
			}
		}
 		return null;
    }
    
    /**
     * Convert a stack trace from an exception into a String object so it can be logged into a log file
     * 
     * @param e the exception occurred
     * @return the stack trace as a String
     */
    public static String getStackTrace(Exception e) {
    	StringWriter sw = new StringWriter();
    	e.printStackTrace(new PrintWriter(sw));
    	String stacktrace = sw.toString();
    	return stacktrace;
	}
    
    /**
     * Obtain the STIX version from the Content_Binding urn
     * 
     * @param node Content_Binding node
     * @return a String containing the STIX version 
     * @throws DOMException
     * 
     * Usage example: 
     *   String stixVersion = FLAREclientUtil.getStixVersion(contentBindingNode);
     */
    public static String getStixVersion(Node node) throws DOMException {
    	try {
    		String urnStr = "";
    		if (node.hasAttributes()) {
                 Attr attr = (Attr) node.getAttributes().getNamedItem("binding_id");
                 if (attr != null) {
                	 urnStr = attr.getValue();                      
                     logger.debug("attribute urnStr: " + urnStr);                      
                 }
            }
    		if (urnStr.isEmpty()) {
    			urnStr = node.getTextContent();
    		}
			logger.debug("getStixVersion urnStr == " + urnStr);
			if (urnStr != null && !urnStr.isEmpty()) {
				int lastIndex = urnStr.lastIndexOf(":");
				String version = "";
				if (lastIndex >= 0) {
					version = urnStr.substring(lastIndex + 1);
				}
				logger.debug("getStixVersion version == " + version);
				return version;
			}
    	}
		catch (DOMException e) {
        	logger.error("DOMException e == " + e.getMessage());
        	throw e;
        }
    	return "";
    }
    
    /**
     * Convert byte array into hex string
     * 
     * @param bytes[] the byte array to be converted
     * @return the String containing the converted hex
     * 
     * Usage example:
     *   String hexString = FLAREclientUtil.hexify(byteArray);
     */
    public static String hexify(byte bytes[]) {
    	if (bytes != null) {
	    	char[] hexDigits = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
	    	StringBuffer buf = new StringBuffer(bytes.length * 2);
	        for (int i = 0; i < bytes.length; ++i) {
	        	buf.append(hexDigits[(bytes[i] & 0xf0) >> 4]);
	            buf.append(hexDigits[bytes[i] & 0x0f]);
	        }
	        return buf.toString();
    	}
    	return null;
    }
    
    /**
     * Evaluate a byte array to check its validity
     * 
     * @param input the byte array to be evaluated
     * @return true or false: valid or not valid
     */
    public static boolean isValidUTF8(byte[] input) {
        CharsetDecoder cs = Charset.forName("UTF-8").newDecoder();
        try {
            cs.decode(ByteBuffer.wrap(input));
            return true;
        } catch (CharacterCodingException e) {
        	logger.error("CharacterCodingException e == " + e.getMessage());
            return false;
        }       
    }
    
	/**
	 * Load config.properties into memory space
	 * 
	 * @return Properties object containing config properties
	 */
	public static Properties loadProperties() {
		InputStream inputStream = null;
		Properties config = new Properties();
		try {
			inputStream = new FileInputStream("config.properties");
			config.load(inputStream);
		} catch (IOException e) {
			 logger.error("loadProperties IOException  ioe: " + e.getMessage());
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
		return config;
	}
    
    /**
     * Prints (in the debug log) the HTTPS response from the remote server
     * @param response The response in string format
     */
    public static void printHttpsResponse(String response) {
		if (response != null) {
			logger.debug("printHttpsResponse response length: " + response.length());
			logger.debug("printHttpsResponse response: " + response);
			if (response.length() != 0) {
				try {
					Document responseDoc = ClientUtil.convertStringToDocument(response);
					Element element = responseDoc.getDocumentElement();
					String status = element.getAttributes().getNamedItem("status_type").getNodeValue();
					logger.debug("printHttpsResponse response status: " + status);
				} catch (Exception e) {
					logger.error("printHttpsResponse Exception e: " + e.getMessage());		
				}
			}
		}
    }
    
    /**
     * Removes the ds:Signature tag from a node
     * 
     * @param node A node that may contain a ds:Signature tag
     * @return the node object with the ds:Signature tag removed or null if ds:Signature block isn't found
     * 
     * Usage example:
     *   Node newNode = FLAREclientUtil.removeDSSignatureTag(oldNode);
     */
    public static Node removeDSSignatureTag(Node node) {
		Node contentBlock = node.cloneNode(true);
		NodeList children = contentBlock.getChildNodes();
		Node current = null;
	    int count = children.getLength();
	    for (int i = 0; i < count; i++) {
	    	current = children.item(i);
	    	if (current.getNodeType() == Node.ELEMENT_NODE) {
	    		Element element = (Element) current;
	    		if (element.getTagName().equalsIgnoreCase("ds:Signature")) {
	    			break;
	    		}
	    	}
	    }
	    if (current != null) {
	        contentBlock.removeChild(current);
    	    return contentBlock;
	    } else {
	    	return null;
	    }
    }
    
	/**
	 * helper function that removes whitespace nodes from Element objects to allow for easier parsing
	 * 
	 * @param e the Element object
	 */
	public static void removeWhitespaceNodes(Element e) {
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
     * Sends an HTTPS POST request and returns the response
     * @param conn the HTTPS connection object
     * @param payload the payload for the POST request
     * @return the response from the remote server in string format
     * 
     * Usage example:
     *   String response = FLAREclientUtil.sendPost(conn, payload);
     */
    public static String sendPost(HttpsURLConnection conn, String payload) {
		OutputStream outputStream = null;
		DataOutputStream wr = null;
		InputStream is = null;
		String response = "";
		try {
			outputStream = conn.getOutputStream();
			wr = new DataOutputStream(outputStream);
		    wr.write(payload.getBytes("UTF-8"));
			wr.flush();
			is = conn.getInputStream();
			response = IOUtils.toString(is, "UTF-8");
			logger.debug("sendPost request: " + payload);
			logger.debug("sendPost response: " + response);
		} catch (IOException e) {
			logger.error("sendRequest IOException e: " + e.getMessage());
		} finally {
			if (is != null) {
				try {
				   is.close();
				} catch (IOException e) {
					logger.error("sendRequest IOException is: " + e.getMessage());
				}
				is = null;
			}
			if (outputStream != null) {
				try {
					outputStream.close();
				} catch (IOException e) {
					logger.error("sendRequest IOException outputStream: " + e.getMessage());
				}
				outputStream = null;
			}
			if (wr != null) {
				try {
					wr.close();
				} catch (IOException e) {
					logger.error("sendRequest IOException wr: " + e.getMessage());
				}
				wr = null;
			}
		}
		return response;
    }
    
    /**
     * Validate each Content block in a TAXII document (against the STIX schema files)
     * @param taxiiDoc The TAXII document containing the STIX to be validated
     * @return true if all STIX validates, false otherwise
     * 
     * Usage example: 
     *   boolean result = FLAREclientUtil.validateStix(taxiiDoc);
     */
    public static boolean validateStix(Document taxiiDoc) {
    	try {
    		logger.debug("validateStix doc == " + taxiiDoc);
    		NodeList blocks = taxiiDoc.getElementsByTagName("Content_Block");
    		int numBlocks = blocks.getLength();
    		logger.debug("validateStix numBlocks == " + numBlocks);
    		for (int i = 0; i < numBlocks; i++) {
    			Node block = (Node) blocks.item(i); 
    			Node stixNode = block.getFirstChild().getNextSibling().getFirstChild();
    			String stixString = convertNodeToString(stixNode);
    			logger.debug("validateStix stixString = \n" + stixString);
	    		Source source = new DOMSource(stixNode);
	    		Node contentBindingNode =  block.getFirstChild();
	    		String stixVersion = getStixVersion(contentBindingNode);
	    		logger.debug("validateStix version == " + stixVersion);
	    		if (!stixVersion.isEmpty() && stixVersions.contains(stixVersion)) {
		    	    StixValidator sv = new StixValidator(stixVersion);
			    	List<SchemaError> errors = sv.validate(source);
					if (errors.size() > 0) {
						logger.debug("errors.size() == " + errors.size());
						for (SchemaError error : errors) {
							logger.debug("SchemaError error getCategory: " + error.getCategory());
							logger.debug("SchemaError error getMessage: " + error.getMessage());
						}
						logger.error("Message was not published due to STIX " + stixVersion + " content validation errors!  Please check content and try again.");
						return false;
					}
	    		} else {
	    			throw new SAXException("Error: No STIX version number is specified by the Content_Binding urn.");
	    		}
    		}
    		logger.debug("All STIX has been validated successfully.");
    		return true;
	    } catch (SAXException e) {
	    	logger.error("SAXException e == " + e.getMessage());
	    	return false;
        } catch (IOException e) {
        	logger.error("IOException e == " + e.getMessage());
        	return false;
        }
    }
}
