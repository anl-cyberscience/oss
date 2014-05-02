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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.LinkOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;

import com.bcmcgroup.taxii.ContentBlock;

/** 
 * @author		Mark Walters <mwalters@bcmcgroup.com>
 * @version		1.0
 */
public class PubDirectory {
	private static final Logger logger = Logger.getLogger(PubDirectory.class);
    private final WatchService watcher;
    private final Map<WatchKey,Path> keys;
    private final boolean recursive;
    private boolean initial = true;
    private static Properties config = null;

    /**
     * Returns the given event casted to the proper type
     * @param event event to be casted
     * @return event properly type-casted event
     */
    @SuppressWarnings("unchecked")
	static <T> WatchEvent<T> cast(WatchEvent<?> event) {
        return (WatchEvent<T>) event;
    }

    /**
     * Register the given directory with the invoking WatchService
     * @param dir path of directory to register
     * @throws IOException
     */
    private void register(Path dir) throws IOException {
        WatchKey key = dir.register(watcher, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY);
        if (!initial) {
            Path prev = keys.get(key);
            if (prev == null) {
                logger.debug("Register: " + dir);
            } else {
                if (!dir.equals(prev)) {
                	 logger.debug("Update: " + prev + " -> " + dir);
                }
            }
        }
        keys.put(key, dir);
    }

    /**
     * Register the given directory, and all its sub-directories, with the
     * WatchService.
     * @param parent the parent directory from which the recursive application begins
     * @throws IOException
     */
    private void registerAll(final Path parent) throws IOException {
        Files.walkFileTree(parent, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                register(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    /**
     * Creates a WatchService and registers the given directory
     * @param dir path of directory to register
     * @param recursive flag to indicate whether we want to include all sub-directories
     * @throws IOException
     */
    public PubDirectory(Path dir, boolean recursive) throws IOException {
    	if (config == null)
    		loadProperties();
        this.watcher = FileSystems.getDefault().newWatchService();
        this.keys = new HashMap<WatchKey,Path>();
        this.recursive = recursive;

        if (recursive) {
        	logger.debug("Scanning to register all subdirectories of:  " + dir);
            registerAll(dir);
            logger.debug("Complete.");
        } else {
            register(dir);
        }

        // set initial to false after initial registration
        this.initial = false;
    }

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
    
    /**
     * Process all events for keys queued to the watcher
     * @throws IOException 
     */
   public void processEvents() throws IOException {
		    	
        for (;;) {
   	        logger.debug("processEvents...: ");
   	    
            // wait for key to be signaled
            WatchKey key;
            try {
                key = watcher.take();              
                logger.debug("processEvents key: " + key);                
            } catch (InterruptedException x) {
            	logger.debug("processEvents Exception: " + x.getMessage());
                return;
            }
            logger.debug("processEvents...: keys: " + keys);
            Path dir = keys.get(key);
            if (dir == null) {
            	logger.debug("WatchKey not recognized!");
                continue;
            }
            logger.debug("processEvents dir.getFileName: " + dir.getFileName());
            // process events
            for (WatchEvent<?> event: key.pollEvents()) {
                @SuppressWarnings("rawtypes")
				WatchEvent.Kind kind = event.kind();
                logger.debug("processEvents dir.name(): " + kind.name());

                // overflow event - events may have been lost or discarded
                if (kind == StandardWatchEventKinds.OVERFLOW) {
                	logger.debug("Overflow! Some files may not have been published.  Check logs and resubmit those that failed.");
                    continue;
                }            
      	   	   	 
                // context for directory entry event is the file name of entry
                WatchEvent<Path> e = cast(event);
                Path child = dir.resolve(e.context());
                logger.debug("processEvents child.toString(): " + child.toString());
                // if directory is created, and watching recursively, then register it and its sub-directories
                if (Files.isDirectory(child, LinkOption.NOFOLLOW_LINKS)) {
                	if (kind == StandardWatchEventKinds.ENTRY_CREATE && recursive) {
                		registerAll(child);
                	}
                	logger.debug("processEvents kind: " + kind);
                	continue;
                }  	   	   	 

                // fetch feed name
                String c = child.toString();
                logger.debug("processEvents c: " + c);
                int lastSlash = c.lastIndexOf(File.separator);
                logger.debug("processEvents lastSlash: " + lastSlash);
                logger.debug("processEvents c.lastIndexOf(File.separator, lastSlash-1): " + c.lastIndexOf(File.separator, lastSlash-1));
                String fN = c.substring(c.lastIndexOf(File.separator, lastSlash-1)+1, lastSlash);
                logger.debug("processEvents fN: " + fN);
                // construct content blocks
                List<ContentBlock> contentBlocks = new ArrayList<ContentBlock>();
                ContentBlock contentBlock = new ContentBlock(config.getProperty("stix_cB"), child.toFile(), null, null, null);
                contentBlocks.add(contentBlock);
                
                // publish message
            	Publisher pub = new Publisher();
            	pub.publish(fN, null, null, contentBlocks, null);
            }

            // reset key and remove from set if directory no longer accessible
            boolean valid = key.reset();
            if (!valid) {
                keys.remove(key);

                // quit if all directories are inaccessible
                if (keys.isEmpty()) {
                    break;
                }
            }
        }
    }

    /**
     * prints usage for errant attempts to run main()
     */
    static void usage() {
    	logger.debug("Usage: java PubDirectory [-r] dir");
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
	            logger.debug("Publish directory is :  "+file);
	            if (file.isDirectory() && file.exists()) {
	            	new PubDirectory(Paths.get(pubPath), false).processEvents();
	            } else {
	             	logger.debug("Input error, directory:  " + file + " is not valid");
	            }
	        } else if (args[0].equals("-r")) {
	        	//if -r provided, but no publish dir, use from config file, otherwise, use from command line arg
	            if (args.length == 1) {
	            	pubPath = config.getProperty("publishDirectory");
	            } else {
	            	pubPath = args[1];
	            }
	            File file = new File(pubPath);
	            logger.debug("Publish directory is :  " + file);
	            if (file.isDirectory() && file.exists()) {
	            	new PubDirectory(Paths.get(pubPath), true).processEvents();
	            } else {
	            	logger.debug("Input error, directory:  "+file +" is not valid");
	            }
	        } else {
	        	//if no -r, use publish dir provided from command line arg
	        	pubPath = args[0];
	        	File file = new File(pubPath);
	        	logger.debug("Publish directory is :  " + file);
	        	if (file.isDirectory() && file.exists()) {
	        		new PubDirectory(Paths.get(pubPath), false).processEvents();
	        	} else {
	        		logger.debug("Input error, directory:  " + file + " is not valid");
	        	}
	        }
    	} catch (IOException e) {
    		logger.error("EOException main: " + e.getMessage());
    	}
    }
}
