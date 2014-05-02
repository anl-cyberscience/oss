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

import java.io.FileNotFoundException;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

/** 
 * @author		Mark Walters <mwalters@bcmcgroup.com>
 * @version		1.0
 */
public class PollTester {
	public static void main(String[] args) throws FileNotFoundException, IOException, ParserConfigurationException, TransformerException {
		Subscriber sub = new Subscriber();
		String pollResults = sub.poll("feed1", "3ed217aa-d149-11e3-b788-000c29914862", null, null, null, null, null);
		System.out.println(pollResults);
	}
}