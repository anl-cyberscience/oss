package com.bcmcgroup.flare.client;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.log4j.Logger;

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

/**
 * @author		David Du <ddu@bcmcgroup.com> 
 * @author		Mark Walters <mwalters@bcmcgroup.com>
 * @version		2.0
 */
public class PollFeed10 {
	private static final Logger logger = Logger.getLogger(PollFeed10.class);

	/**
     * prints usage for errant attempts to run main()
     */
    static void usage() {
        System.err.println("Usage: java PollFeed10 feed_name [-b Exclusive_Begin_Timestamp] [-e Inclusive_End_Timestamp]");
        System.exit(-1);
    }

    /**
	 * Runs the PollFeed10 using the arguments provided and prints the Poll_Response xml to the console
	 * 
	 * @param args
	 */
	public static void main(String[] args) throws IOException {
		String feedName = null, bTime = null, eTime = null;
		if (args.length == 1) {
			feedName = args[0];
		} else if (args.length == 3 && (args[1].equals("-b") || args[1].equals("-e"))) {
			feedName = args[0];
			if (args[1].equals("-b")) {
				bTime = args[2];
			} else {
				eTime = args[2];
			}
		} else if (args.length == 5 && ((args[1].equals("-b") && args[3].equals("-e")) || (args[1].equals("-e") || args[3].equals("-b")))) {
			feedName = args[0];
			if (args[1].equals("-b")) {
				bTime = args[2];
				eTime = args[4];
			} else {
				bTime = args[4];
				eTime = args[2];
			}
		} else {
			usage();
		}
        InputStream inputStream = null;
		try {
			Properties config = ClientUtil.loadProperties();
			//System.setProperty("javax.net.debug", "ssl,handshake"); // use for ssl/handshake debugging
			String subId = config.getProperty(feedName + "_subId");
			Subscriber10 sub = new Subscriber10();
			String pollResults = sub.poll(null, feedName, subId, null, bTime, eTime, null, null);
			System.out.println(pollResults);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("Exception in HttpListener e: " + e.getMessage());	
		} finally {
			try {
				if (inputStream != null) {			
					inputStream.close();
					inputStream = null;
				}
			} catch (IOException e) {
				logger.error("IOException e: " + e.getMessage());
			}
		}
	}
}