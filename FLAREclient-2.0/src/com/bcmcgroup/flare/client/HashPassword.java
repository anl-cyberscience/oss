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

import org.apache.log4j.Logger;

/** 
 * @author		Mark Walters <mwalters@bcmcgroup.com>
 * @version		2.0
 */
public class HashPassword {
	private static final Logger logger = Logger.getLogger(HashPassword.class);
	
	/**
	* the main method to run for encryption of plaintext password, the result value will be
	* saved into config.property file for the value field of the passed in property name
	* 
	* @param args property name, password to be encrypted
	*/
	public static void main(String[] args) {
		String originalPassword = null;
		try {
			if (args.length == 2) {
				originalPassword = args[1];
			} else {
				logger.error("Usage: java HashPassword <property name in config.properties file> <password to be encrypted>");
				System.exit(0);
			}
			String propName = args[0];
			String encryptedPassword = ClientUtil.encrypt(originalPassword);
			ClientUtil.changeProperty(propName, encryptedPassword);
		} catch (Exception e) {
			System.err.println("Exception occurred e: " + e.getMessage());
		}
	}
}