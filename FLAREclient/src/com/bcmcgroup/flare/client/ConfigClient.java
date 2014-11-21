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
 * @version		1.0
 */
public class ConfigClient 
{
	private static final Logger logger = Logger.getLogger(ConfigClient.class);
	
    public static void main( String[] args )
    {
    	Properties config = new Properties();
    	FileOutputStream fos = null;
    	try {
    		// general
    		config.setProperty("clientVersion", "1.0");
    		config.setProperty("layer7Url", "https://FlareGateway:8443/cybershare/taxii/");
    		config.setProperty("pathToTrustStore", "/path/to/truststore.jks"); // this truststore must contain the certificate for FlareGateway
    		config.setProperty("trustStorePassword", "hashedPasswordGoesHere");
    		config.setProperty("basePath", "/root/FlareClient/");

    		// publisher
    		config.setProperty("pathToPublisherKeyStore", "/path/to/publisherKeyStore.jks");
    		config.setProperty("publisherKeyStorePassword",  "hashedPasswordGoesHere");
    		config.setProperty("publisherUserAgent", "flare client publisher application");
    		config.setProperty("publisherContentType", "application/xml");
    		config.setProperty("publisherAccept", "application/xml");
    		config.setProperty("publisherXTaxiiAccept", "urn:taxii.mitre.org:message:xml:1.0");
    		config.setProperty("publisherXTaxiiContentType", "urn:taxii.mitre.org:message:xml:1.0");
    		config.setProperty("publisherXTaxiiProtocol", "urn:taxii.mitre.org:protocol:https:1.0");
    		config.setProperty("publisherXTaxiiServices", "urn:taxii.mitre.org:services:1.0");
    		config.setProperty("stix_cB", "urn:stix.mitre.org:xml:1.1");
    		
    		// subscriber
    		config.setProperty("pathToSubscriberKeyStore", "/path/to/subscriberKeyStore.jks");
    		config.setProperty("subscriberKeyStorePassword",  "hashedPasswordGoesHere");
    		config.setProperty("subscriberUserAgent", "flare client subscriber application");
    		config.setProperty("subscriberContentType", "application/xml");
    		config.setProperty("subscriberAccept", "application/xml");
    		config.setProperty("subscriberXTaxiiAccept", "urn:taxii.mitre.org:message:xml:1.0");
    		config.setProperty("subscriberXTaxiiContentType", "urn:taxii.mitre.org:message:xml:1.0");
    		config.setProperty("subscriberXTaxiiProtocol", "urn:taxii.mitre.org:protocol:https:1.0");
    		config.setProperty("subscriberXTaxiiServices", "urn:taxii.mitre.org:services:1.0");
    		config.setProperty("listenerEndpoint", "/");
    		config.setProperty("remoteTaxiiIP", "remoteTaxiiIpGoesHere");
    		
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
