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
import java.io.IOException;
import java.io.InputStream;
import java.security.SecureRandom;
import java.util.Properties;
import javax.xml.parsers.DocumentBuilder;

import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.bcmcgroup.flare.client.ClientUtil;
import com.bcmcgroup.flare.client.Subscriber10;
import com.bcmcgroup.flare.client.Subscriber11;
import com.bcmcgroup.flare.client.TaxiiValidator;
import com.bcmcgroup.flare.xml.digsig.XmlDigitalSignatureVerifier;

/** 
 * @author Mark Walters <mwalters@bcmcgroup.com>
 * @author David Du <ddu@bcmcgroup.com>
 * @version		2.0
 */
public class UnitTests  {
	Subscriber10 subscriber10;
	Subscriber11 subscriber11;
	Properties config;
	boolean saveStatus = false;
	String savePath = "/Users/mwalters/tmp/FLAREclientUnitTests/SubscribeTest";
	String configPath = "/Users/mwalters/tmp/FLAREclientUnitTests/config.properties";
	String randomNumberString = null;
	File sampleStix10File = new File("/Users/mwalters/tmp/FLAREclientUnitTests/sampleStix10.xml");
	File sampleStix101File = new File("/Users/mwalters/tmp/FLAREclientUnitTests/sampleStix101.xml");
	File sampleStix11File = new File("/Users/mwalters/tmp/FLAREclientUnitTests/sampleStix11.xml");
	File sampleStix111File = new File("/Users/mwalters/tmp/FLAREclientUnitTests/sampleStix111.xml");
	File validTaxii10File = new File("/Users/mwalters/tmp/FLAREclientUnitTests/validTaxii10.xml"); // needs to be digitally signed (whole TAXII message) by the FLAREhub
	File validTaxii11File = new File("/Users/mwalters/tmp/FLAREclientUnitTests/validTaxii11.xml"); // needs to be digitally signed (whole TAXII message) by the FLAREhub
	File invalidTaxii10File = new File("/Users/mwalters/tmp/FLAREclientUnitTests/invalidTaxii10.xml");
	File invalidTaxii11File = new File("/Users/mwalters/tmp/FLAREclientUnitTests/invalidTaxii11.xml");
	Document sampleStix10Doc = null, sampleStix101Doc = null, sampleStix11Doc = null, sampleStix111Doc = null, validTaxii10Doc = null, validTaxii11Doc = null, invalidTaxii10Doc = null, invalidTaxii11Doc = null;
	
	@Before
	public void setup() {
		InputStream inputStream = null;
		try {
			config = new Properties();
			inputStream = new FileInputStream(configPath);
			config.load(inputStream);
		    subscriber10 = new Subscriber10();
		    subscriber11 = new Subscriber11();
		    randomNumberString = Integer.toString(Math.abs((new SecureRandom()).nextInt()));
			DocumentBuilder db = ClientUtil.generateDocumentBuilder();
			sampleStix10Doc = db.parse(sampleStix10File);
			sampleStix101Doc = db.parse(sampleStix101File);
			sampleStix11Doc = db.parse(sampleStix11File);
			sampleStix111Doc = db.parse(sampleStix111File);
			validTaxii10Doc = db.parse(validTaxii10File);
			validTaxii11Doc = db.parse(validTaxii11File);
			invalidTaxii10Doc = db.parse(invalidTaxii10File);
			invalidTaxii11Doc = db.parse(invalidTaxii11File);
		} catch (IOException ioe) {
			System.err.println("setup IOException ioe: " + ioe.getMessage());
		} catch (Exception e) {
			System.err.println("setup IOException e: " + e.getMessage());
		} finally {
			try {
				if (inputStream != null) {			
					inputStream.close();
					inputStream = null;
				}	
			} catch (IOException e) {
				 System.err.println("loadProperties Exception  e: " + e.getMessage());
			}
		}
	}
	
	@Test
	public void testDocumentConversion() {
		System.out.println("DOCUMENT CONVERSION");
		String docString1 = ClientUtil.convertDocumentToString(sampleStix10Doc, true);
		Document doc = ClientUtil.convertStringToDocument(docString1);
		String docString2 = ClientUtil.convertDocumentToString(doc, true);
	    assertEquals(docString1, docString2);
	}
	
	@Test
	public void testEncryptionCycle() {
		System.out.println("ENCRYPTION CYCLE");
		System.out.println("randomNumberString = " + randomNumberString);
		String encrypted = ClientUtil.encrypt(randomNumberString);
		String decrypted = ClientUtil.decrypt(encrypted);
	    assertEquals(randomNumberString, decrypted);
	}

	/*
	@Test
	public void testSave10() throws SAXException, IOException {
		FileInputStream tobeSavedIn = null;
		FileInputStream savedIn = null;
		System.out.println("SAVE TAXII 1.0");		
		File saveFile = sampleStix111File;
		tobeSavedIn = new FileInputStream(saveFile);
        long tobeSavedInSize = tobeSavedIn.getChannel().size();
        tobeSavedIn.close();
        subscriber10.save(saveFile, "savedFile");
        savedIn = new FileInputStream(savePath+"/push_savedFile_0.xml");
        long savedInSize = savedIn.getChannel().size();
        savedIn.close();
        assertEquals(tobeSavedInSize, savedInSize);
	}
	
	@Test
	public void testSave11() throws SAXException, IOException {
		FileInputStream tobeSavedIn = null;
		FileInputStream savedIn = null;
		System.out.println("SAVE TAXII 1.1");		
		File saveFile = sampleStix111File;
		tobeSavedIn = new FileInputStream(saveFile);
        long tobeSavedInSize = tobeSavedIn.getChannel().size();
        tobeSavedIn.close();
        subscriber11.save(saveFile, null);
        savedIn = new FileInputStream(savePath);
        long savedInSize = savedIn.getChannel().size();
        savedIn.close();
        assertEquals(tobeSavedInSize, savedInSize);
	} 
	*/
	
	@Test
	public void testValidateTaxii10() throws SAXException, IOException {
		System.out.println("VALIDATE TAXII 1.0");
		TaxiiValidator tv = new TaxiiValidator();
		assertEquals(true, tv.validate(validTaxii10Doc));
		assertEquals(false, tv.validate(invalidTaxii10Doc));
	}
	
	@Test
	public void testValidateTaxii11() throws SAXException, IOException {
		System.out.println("VALIDATE TAXII 1.1");
		TaxiiValidator tv = new TaxiiValidator();
		assertEquals(true, tv.validate(validTaxii11Doc));
		assertEquals(false, tv.validate(invalidTaxii11Doc));
	}
		
	@Test
	public void testVerifySignature10() {
		System.out.println("VERIFY SIGNATURE TAXII 1.0");
        String ts = config.getProperty("pathToTrustStore");
        String tsPw = ClientUtil.decrypt(config.getProperty("trustStorePassword"));
        String hA = config.getProperty("hubAlias");
        System.out.println("testValidateSignature trustStorePath: " + ts + " trustStorePW: " + tsPw + " hubAlias: " + hA);
        boolean validFlag = XmlDigitalSignatureVerifier.verifySignatureEnveloped(validTaxii10Doc, ts, tsPw, hA);
        assertEquals(true, validFlag);
	}
	
	@Test
	public void testVerifySignature11() {
		System.out.println("VERIFY SIGNATURE TAXII 1.1");
        String ts = config.getProperty("pathToTrustStore");
        String tsPw = ClientUtil.decrypt(config.getProperty("trustStorePassword"));
        String hA = config.getProperty("hubAlias");
        System.out.println("testValidateSignature trustStorePath: " + ts + " trustStorePW: " + tsPw + " hubAlias: " + hA);
        boolean validFlag = XmlDigitalSignatureVerifier.verifySignatureEnveloped(validTaxii11Doc, ts, tsPw, hA);
        assertEquals(true, validFlag);
	}	
}