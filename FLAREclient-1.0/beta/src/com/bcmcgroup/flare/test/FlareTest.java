package com.bcmcgroup.flare.test;

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

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;
import org.mitre.stix.validator.SchemaError;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.bcmcgroup.flare.client.FlareClientUtil;
import com.bcmcgroup.flare.client.PubDirectory;
import com.bcmcgroup.flare.client.Subscriber;
import com.bcmcgroup.flare.client.XMLFilter;
import com.bcmcgroup.flare.xml.digsig.XmlDigitalSignatureGenerator;
import com.bcmcgroup.flare.xml.digsig.XmlDigitalSignatureVerifier;

/** 
 * @author		David Du <ddu@bcmcgroup.com>
 * @version		1.1
 */
public class FlareTest  {
	private static final Logger logger = Logger.getLogger(FlareTest.class);
	PubDirectory pubDir;
	Subscriber subscriber;
	Properties config;
	boolean publishStatus = false;
	boolean saveStatus = false;
	String basePath = null;
	String pubPath = null;
	String savePath = null;
	
	@Before
	public void setup() {
		try {
			logger.info("Calling setup...");			
	        String pubPath = null;
	        config = new Properties();
	        InputStream inputStream = null;
			try {
				inputStream = new FileInputStream("config.properties");
				config.load(inputStream); 
			} catch (FileNotFoundException ex) {
				 logger.error("setup IOException  ex: " + ex.getMessage());
			} catch (IOException ex) {
				 logger.error("setup IOException  ioe: " + ex.getMessage());
			} finally {
				try {
					if (inputStream != null) {			
						inputStream.close();
						inputStream = null;
					}	
				} catch (IOException e) {
					logger.error("setup IOException  e: " + e.getMessage());
				}
			}
        	pubPath = config.getProperty("publishDirectory");
        	savePath = config.getProperty("subscribeDirectory");
        	basePath = config.getProperty("basePath");
        	File file = new File(pubPath);
            logger.debug("setup directory is pubPath: " + file);
	        assertEquals(file.isDirectory(), true);
	        assertEquals(file.exists(), true);      
			logger.info("setup pubPath == " + pubPath);
		    pubDir = new PubDirectory(Paths.get(pubPath), true);
		    logger.debug("setup Saved File is savePath: " + savePath);
		    subscriber = new Subscriber();
		    subscriber.setProperties(config);
		} catch (IOException ioe) {
			logger.error("setup IOException ioe: " + ioe.getMessage());
		}
	}
	
	@Test
	public void testFilter() {
		XMLFilter filter = new XMLFilter();
		try {
			FileInputStream xmlStream = new FileInputStream(new File("filterTest.txt"));
			filter.filter(xmlStream);
		} catch (FileNotFoundException e) {
			logger.error("FileNotFoundException e: " + e.getMessage());
		}
	}
	
	@Test
	public void testLoadProperties() {		
		File file = new File(pubPath);
        logger.debug("testLoadProperties directory pubPath:  " + file);
	    assertEquals(file.isDirectory(), true);
	    assertEquals(file.exists(), true);
	}
	
	@Test
	public void testPaths() {
		File file = new File(pubPath);
        logger.debug("testPaths directory pubPath is : " + file);
        assertEquals("Publish path ", file.isDirectory(), true);
        assertEquals(file.exists(), true);
	}
	
	@Test
	public void testSave() {
		FileInputStream tobeSavedIn = null;
		FileInputStream savedIn = null;
		try {
			logger.info("In testSave ... ");			
			File saveFile = new File(basePath + File.separator + "xml" + File.separator + "test" + File.separator + "testsave.xml");
			tobeSavedIn = new FileInputStream(saveFile);
	        long tobeSavedInSize = tobeSavedIn.getChannel().size();
            subscriber.save(saveFile);
            savedIn = new FileInputStream(savePath);
	        long savedInSize = savedIn.getChannel().size();
	        assertEquals("Saved size equal test: ", tobeSavedInSize, savedInSize);
			saveStatus = true;
		} catch (ParserConfigurationException e) {
			saveStatus = false;
			logger.error("ParserConfigurationException e: " + e.getMessage());
		}  catch (FileNotFoundException e) {
			saveStatus = false;
			logger.error("FileNotFoundException e: " + e.getMessage());
		}  catch (TransformerException e) {
			saveStatus = false;
			logger.error("TransformerException e: " + e.getMessage());
		} catch (SAXException e) {
			saveStatus = false;
			logger.error("SAXException e: " + e.getMessage());
		} catch (IOException e) {
			saveStatus = false;
			logger.error("IOException e: " + e.getMessage());
		}  finally {
			if (tobeSavedIn != null) {
				try {
					tobeSavedIn.close();
				} catch (IOException e) {
					logger.error("IOException ioe: " + e.getMessage());
				}
			}
			if (savedIn != null) {
				try {
					savedIn.close();
				} catch (IOException e) {
					logger.error("IOException ioe: " + e.getMessage());
				}
			}
		}
		assertEquals(publishStatus, true);
	}
	
	@Test
	public void testValidateTaxii() {
		FileInputStream fis = null;
		try {
			File file = new File(basePath + File.separator + "xml" + File.separator + "test" + File.separator + "testxmlpub.xml");
			System.out.println("testValidateTaxii file: " + file);
			fis = new FileInputStream(file);
			StringBuilder builder = new StringBuilder();
			int ch;
			while ((ch = fis.read()) != -1) {
			    builder.append((char) ch);
			}
			String output = builder.toString();
			List<SchemaError> errors = FlareClientUtil.validateTaxii(output);
			if (errors.size() == 0) {
				System.out.println("testValidateTaxii Validated with No error, errors.size(): " + errors.size());
			} else {
				for (SchemaError error: errors) {
					System.out.println("testValidateTaxii error.getMessage(): " + error.getMessage());
				}
			}
		} catch (FileNotFoundException e) {
			logger.error("FileNotFoundException e: " + e.getMessage());
		}  catch (IOException e) {
			logger.error("IOException e: " + e.getMessage());
		} catch (SAXException e) {
			logger.error("SAXException e: " + e.getMessage());
		} finally {
			if (fis != null) {
				try {
				    fis.close();
				} catch (IOException e) {
					logger.error("IOException e: " + e.getMessage());
				}
			}
		}
	}
	
	@Test
	public void testValidateSignature() {
		logger.debug("Calling testValidateSignature...");
        try {
	        File file = new File(basePath + File.separator + "xml" + File.separator + "test" + File.separator + "testds2.xml");
	        logger.debug("testValidateSignature file: " + file);
			FileInputStream fis = new FileInputStream(file);
			String output =FlareClientUtil.convertStreamToString(fis);
			logger.debug("Signed XML output : " + output);
	        
			// Verify the signed XML Document
	        Document doc = FlareClientUtil.convertStringToDocument(output);
	        
	        String pks = config.getProperty("pathToPublisherKeyStore");
	        String pksPw = FlareClientUtil.decrypt(config.getProperty("publisherKeyStorePassword"));
	        String pk = config.getProperty("publisherKeyName");
	        String pkPw = FlareClientUtil.decrypt(config.getProperty("publisherKeyPassword"));
	        List<Integer> statusList = XmlDigitalSignatureGenerator.generateXmlDigitalSignatureDetached(doc, pks, pksPw, pk, pkPw);
	        logger.debug(" Generating XML Digital Signature statusList : " + statusList);
	        String sks = config.getProperty("pathToSubscriberKeyStore");
	        sks = basePath + File.separator + sks + ".jks";
	        String sksPw = FlareClientUtil.decrypt(config.getProperty("subscriberKeyStorePassword"));
	        logger.debug("testValidateSignature keyStorePath: " + sks);
	        logger.debug("testValidateSignature keyStorePW: " + sksPw);
            List<Boolean> validFlags = XmlDigitalSignatureVerifier.validateSignature(doc, sks,  sksPw);
            logger.debug("Validity of XML Digital Signature : " + validFlags);
        } catch (FileNotFoundException ex) {
        	logger.error("FileNotFoundException ex: " + ex.getMessage());
        }
	}
}