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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import org.apache.log4j.Logger;

/** 
 * @author		David Du <ddu@bcmcgroup.com>
 * @version		1.1
 */
public class XMLFilter {
	private static final Logger logger = Logger.getLogger(XMLFilter.class);
	
	/**
	 * Default constructor
	 */
	public XMLFilter() {}	
		
	/**
	 * Filters out the non-UTF-8 characters
	 * @param xmlStream the xml stream to be filtered
	 * @return the filtered resulting String
	 */
    public String filter(InputStream xmlStream) {
		try {
			CharsetDecoder utf8Decoder = Charset.forName("UTF-8").newDecoder();
			utf8Decoder.onMalformedInput(CodingErrorAction.IGNORE);
			utf8Decoder.onUnmappableCharacter(CodingErrorAction.IGNORE);
			Reader reader = new InputStreamReader(xmlStream, utf8Decoder);
			char[] arr = new char[8*1024];
			StringBuffer buf = new StringBuffer();
			int numChars;
			while ((numChars = reader.read(arr, 0, arr.length)) > 0) {
				buf.append(arr, 0, numChars);
			}
			String buffer = buf.toString();
			return buffer;
		} catch (IOException e) {
			logger.error("IOException e: " + e.getMessage());
		} finally {
			if (xmlStream != null) {
				try {
					xmlStream.close();
				} catch (IOException e) {
					logger.error("IOException e: " + e.getMessage());
				}
			}
			logger.debug("Leaving filter... ");
		}
		return "Exception occurred";
    }
}