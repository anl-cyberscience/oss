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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;

import com.bcmcgroup.flare.client.Publisher;
import com.bcmcgroup.taxii.ContentBlock;


public class PublisherTest {
	private static final Logger logger = Logger.getLogger(PublisherTest.class);  
    private static Properties config = null;
    private String fileDirectory;

    public PublisherTest(Path dir) throws IOException {
    	if (config == null) {
    		loadProperties();
    	}
    	fileDirectory = dir.toString();
    }
  
    /**
     * Load config.properties
     */
    private static void loadProperties() {
    	InputStream inputStream = null;
    	if (config == null) {
		   config = new Properties();
    	}
		try {
			inputStream = new FileInputStream("config.properties");
			config.load(inputStream);
			String clientVersion = config.getProperty("clientVersion");
			if (clientVersion != null && !clientVersion.isEmpty()) {
				logger.debug("loadProperties clientVersion: " + clientVersion);
			} else {
				logger.debug("loadProperties flare client version is not configured... ");
			}		 
		} catch (FileNotFoundException ex) {
			 logger.error("loadProperties IOException  ex: " + ex.getMessage());
		} catch (IOException ex) {
			 logger.error("loadProperties IOException  ioe: " + ex.getMessage());
		} finally {
			try {
				if (inputStream != null) {			
					inputStream.close();
					inputStream = null;
				}	
			} catch (IOException e) {
				 logger.error("loadProperties IOException  e: " + e.getMessage());
			}
		}    	
    }
    
    public List<String> getAllFiles(final File folder) {
    	List<String> files = new ArrayList<String>();
        for (final File fileEntry : folder.listFiles()) {
            if (fileEntry.isDirectory()) {
            	getAllFiles(fileEntry);
            } else {
            	files.add(fileEntry.getAbsolutePath());
            }
        }
        logger.debug("getAllFiles: " + files);
        return files;
    }
    
    public void processFiles() throws IOException {
    	logger.debug("processFiles...: ");
   	    List<ContentBlock> contentBlocks = new ArrayList<ContentBlock>();
   	    List<String> files = getAllFiles(new File(fileDirectory));
   	    String fN = null;
        logger.debug("processFiles files.size: " + files.size());
            
        // process events
        for (int ii=0; ii < files.size(); ii++) {
        	String filename = files.get(ii);
            logger.debug("processFiles filename: " + filename);
            if (new File(filename).isDirectory() || !filename.toLowerCase().endsWith(".xml")) {
            	logger.debug("processFiles filename: " + filename + " is a directory, not a file.");
                continue;
            }  	   	   	 
            
            // fetch feed name
            int lastSlash = filename.lastIndexOf(File.separator);
            logger.debug("processFiles lastSlash: " + lastSlash);
            logger.debug("processFiles c.lastIndexOf(File.separator, lastSlash-1): " + filename.lastIndexOf(File.separator, lastSlash-1));
            if (fN == null) {
            	fN = filename.substring(filename.lastIndexOf(File.separator, lastSlash-1)+1, lastSlash);
                logger.debug("processFiles fN: " + fN);
            }
                
            // construct content blocks
            ContentBlock contentBlock = new ContentBlock(config.getProperty("stix_cB"), new File(filename), null, null, null);
            contentBlocks.add(contentBlock);
        }
        // publish message
    	Publisher pub = new Publisher();
    	pub.publish(fN, null, null, contentBlocks);
    }

    /**
     * prints usage for errant attempts to run main()
     */
    static void usage() {
    	logger.debug("Usage: java PublisherTest dir");
        System.exit(-1);
    }

    /**
     * Runs the PubDirectory server until killed with Ctrl-C
     * @param args optional recursive argument followed by directory path
     * @throws IOException
     */
    public static void main(String[] args) {
    	try {
	        if (args.length > 2) {
	            usage();
	        }
	        loadProperties();
	        String pubPath = null;
	        //if publish dir not provided, get from config file
	        if (args.length == 0) {
	        	pubPath = config.getProperty("publishDirectory");
	        	File file = new File(pubPath);
	            logger.debug("Publish directory is :  " + file);
	            if (file.isDirectory() && file.exists()) {
	            	new PublisherTest(Paths.get(pubPath)).processFiles();
	            } else {
	             	logger.debug("Input error, directory:  " + file + " is not valid");
	            }
	        } else {
	        	//if no -r, use publish dir provided from command line arg
	        	pubPath = args[0];
	        	File file = new File(pubPath);
	        	logger.debug("Publish directory is :  " + file);
	        	if (file.isDirectory() && file.exists()) {
	        		new PublisherTest(Paths.get(pubPath)).processFiles();
	        	} else {
	        		logger.debug("Input error, directory:  " + file + " is not valid");
	        	}
	        }
    	} catch (IOException e) {
    		logger.error("EOException main: " + e.getMessage());
    	}
    }
}
