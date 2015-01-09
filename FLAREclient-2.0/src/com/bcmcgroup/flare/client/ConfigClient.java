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

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import org.apache.log4j.Logger;

/** 
 * @author		Mark Walters <mwalters@bcmcgroup.com>
 * @version		2.0
 */
public class ConfigClient {	
	private static final Logger logger = Logger.getLogger(ConfigClient.class);	
    
	public static void main(String[] args) {
    	Properties config = new Properties();
    	FileOutputStream fos = null;
    	try {
    		
    		// general
    		config.setProperty("clientVersion", "2.0");
    		config.setProperty("taxii10NS", "http://taxii.mitre.org/messages/taxii_xml_binding-1");
    		config.setProperty("taxii11NS", "http://taxii.mitre.org/messages/taxii_xml_binding-1.1");
    		config.setProperty("taxiiQuery10NS", "http://taxii.mitre.org/query/taxii_default_query-1");
        	config.setProperty("taxii10serverUrl", "https://FLAREgateway:8443/flare/taxii/");
    		config.setProperty("taxii11serverUrl", "https://FLAREgateway:8443/flare/taxii11/");
    		config.setProperty("httpHeaderUserAgent", "FLAREclient application");
    		config.setProperty("httpHeaderContentType", "application/xml");
    		config.setProperty("httpHeaderAccept", "application/xml");
    		config.setProperty("pathToTrustStore", "/path/to/TrustStore.jks"); // this TrustStore must contain the FLAREgateway.pem certificate
    		config.setProperty("trustStorePassword", "hashedPasswordGoesHere");
    		config.setProperty("basePath", "/home/user/FLAREclient-2.0/");

    		// publisher
    		config.setProperty("pathToPublisherKeyStore", "/path/to/KeyStore.jks");
    		config.setProperty("publisherKeyStorePassword", "hashedPasswordGoesHere");
    		config.setProperty("publisherKeyName", "publisher"); // used for applying digital signatures, must have RSA public key
    		config.setProperty("publisherKeyPassword", "hashedPasswordGoesHere"); // used for applying digital signatures
    		config.setProperty("stix_cB", "urn:stix.mitre.org:xml:1.1.1"); // change depending on which version of STIX you are publishing

    		// subscriber
    		config.setProperty("pathToSubscriberKeyStore", "/path/to/KeyStore.jks");
    		config.setProperty("subscriberKeyStorePassword", "hashedPasswordGoesHere");
    		config.setProperty("listenerEndpoint", "/");
    		config.setProperty("verifyDS", "false");
    		config.setProperty("hubAlias", "FLAREhub");
    		config.setProperty("feedName_subId", "12345678-90ab-cdef-1234-567890abcdef"); // example: create one of these for each feed to indicate subscription_id for polling
    		
    		//save properties to project root folder
    		fos = new FileOutputStream("config.properties");
    		config.store(fos, null);
 
    	} catch (IOException ex) {
    		logger.error("main IOException  ex: " + ex.getMessage());
        } finally {
        	if (fos != null) {
        		try {
        			fos.close();
        		} catch (IOException e) {
        			logger.error("main IOException  e: " + e.getMessage());
        		}
        		fos = null;
        	}
        }
    }
}
