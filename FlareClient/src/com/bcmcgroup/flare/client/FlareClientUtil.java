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
import java.io.File;
import java.io.FileInputStream;
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
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import org.xml.sax.InputSource;

import java.util.List;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;
import org.mitre.stix.validator.SchemaError;
import org.mitre.stix.validator.StixValidator;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

/** 
 * @author		David Du <ddu@bcmcgroup.com>
 * @version		1.0
 */
public class FlareClientUtil {
	 
	private static final Logger logger = Logger.getLogger(FlareClientUtil.class);        
    private static final char[] seeds = "enfldsgbnlsngdlksdsgm".toCharArray();
    private static byte[] ivBytes = "0102030405060708".getBytes();
    private static String salt ="a9v5n38s";
    private static int pswdIterations = 65536;
    private static int keySize = 256;
     
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
       
    public static String getStackTrace(Exception e) {
		 StringWriter sw = new StringWriter();
		 e.printStackTrace(new PrintWriter(sw));
		 String stacktrace = sw.toString();
		 return stacktrace;
	}
    
    public static boolean isValidUTF8(byte[] input) {
        CharsetDecoder cs = Charset.forName("UTF-8").newDecoder();
        try {
            cs.decode(ByteBuffer.wrap(input));
            return true;
        } catch (CharacterCodingException e) {
            return false;
        }       
    }
    
    public static List<SchemaError> validateTaxii(String content) throws SAXException {
    	try {   		    		
    		logger.debug("Calling validateTaxii on content: " + content);
	    	Source xmlFile = new StreamSource(new java.io.StringReader(content));
	    	StixValidator stixValidator = new StixValidator("1.1");	    	
	    	List<SchemaError> errors =  stixValidator.validate(xmlFile);
	    	logger.debug("validateTaxii errors == " + errors);
	    	return errors;
	    } catch (SAXException e) {
	    	logger.error("SAXException e == " + e.getMessage());
	    	throw e;
        } catch (Exception e) {
        	logger.error("Exception e == " + e.getMessage());
        	throw new SAXException(e.getMessage());
        }
    }     
    
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
	            } catch (Exception e) {
	            	logger.info("Exception e: " + e.getMessage());
	            } finally {	            	
	            	try {
	            		is.close();
	            	} catch(Exception e) {
	            		logger.error("Exception close e: " + e.getMessage());
	            	}
	            }
	            return writer.toString();
	        } else {       
	           return "";
	        }
	    }
    
    public static String convertDocumentToString(Node node) {
		 try {
			 Source source = new DOMSource(node);
			 StringWriter stringWriter = new StringWriter();
			 Result result = new StreamResult(stringWriter);
			 TransformerFactory factory = TransformerFactory.newInstance();
			 Transformer transformer = factory.newTransformer();
			 transformer.transform(source, result);
			 return stringWriter.getBuffer().toString();
		 } catch (Exception e) {
			 logger.error("convertStringToDocument e == " + e.getMessage());
			 logger.error("convertDocumentToString stack: " + getStackTrace(e));
		 }
		 return null;
    }
    
    public static byte[] convertDocumentToBytes(Node node) {
    	try {
    		Source source = new DOMSource(node);
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			StreamResult result = new StreamResult(bos);
			TransformerFactory factory = TransformerFactory.newInstance();
			Transformer transformer = factory.newTransformer();
			transformer.transform(source, result);
			return bos.toByteArray();
		} catch (Exception e) {
			logger.error("convertDocumentToBytes e == " + e.getMessage());
			logger.error("convertDocumentToBytes stack: " + getStackTrace(e));
		}
		return null;
	}
    
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
        		} catch (IOException e) {}
        	}
        	if (out != null) {
        		try {
        			out.close();
        		} catch (IOException e) {}
        	}
        }
    }
    
    public static Document convertStringToDocument(String xml) {
		 try{
			 logger.info("convertStringToDocument xml == " + xml);
			 DocumentBuilderFactory factory =  DocumentBuilderFactory.newInstance();
			 factory.setNamespaceAware(true);
			 DocumentBuilder builder = factory.newDocumentBuilder();
			 Document document = builder.parse(new InputSource(new StringReader(xml)));
			 logger.info("convertStringToDocument document=="+document);
			 return document;
		 } catch (Exception e) {
			 logger.error("convertStringToDocument e == " + e.getMessage());
			 logger.error("convertStringToDocument stack: " + getStackTrace(e));
		 }
		 return null;
	 }

	public static void main(String[] args) {
		String originalPassword  = null;
		try {
	    	if (args.length == 2) {
	    		 originalPassword = args[1];
	    	} else {
	    		logger.error("Usage: java FlareClientUtil [property name in config.properties file] [password to be encrypted]");
	    	    System.exit(0);
	    	}
	    	String propName = args[0];
	        String encryptedPassword = encrypt(originalPassword);
	        changeProperty(propName, encryptedPassword);
		} catch (Exception e) {
			System.err.println("Exception occurred e: " + e.getMessage());
		}
	}
}
