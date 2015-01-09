package com.bcmcgroup.flare.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import com.bcmcgroup.flare.client.Subscriber10;

/** 
 * @author		David Du <ddu@bcmcgroup.com>
 * @author		Mark Walters <mwalters@bcmcgroup.com>
 * @version		2.0
 */
public class AutomatedTestPlan {
	public static void main2(String[] args) throws FileNotFoundException, IOException, ParserConfigurationException, TransformerException {
		String fN = "feed1";
		try {
			System.out.println("PollTester main args.length: " +args.length);
			Subscriber10 sub = new Subscriber10();
			if (args.length == 0) {
			    String bTime = "2014-04-28T23:19:44Z";
			    Calendar cal = Calendar.getInstance();
		    	SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
		    	bTime = sdf.format(cal.getTime());
		    	System.out.println("PollTester bTime: " + bTime);
			    System.out.println("PollTester No timestamp case...................... ");
			    String pollResults = sub.poll(null, fN, "0f4f63b8-5a22-11e4-a305-000c2909f434", null, null, null, null, null);
			    System.out.println("PollTester pollResults: " + pollResults);
			} else {
				String subid = args[0];
				System.out.println("PollTester subid: " + subid);
				if (args.length >= 2) {
					String bTime = args[1];
					System.out.println("PollTester bTime: " + bTime);
					if (args.length >= 3) {
						String eTime = args[2];
						System.out.println("PollTester eTime: " + eTime);
						String pollResults = sub.poll(null, fN, subid, null, bTime, eTime, null, null);
					    System.out.println("PollTester pollResults: " + pollResults);
					} else {
						String pollResults = sub.poll(null, fN, subid, null, bTime, null, null, null);
					    System.out.println("PollTester pollResults: " + pollResults);
					}
				} else {
					String pollResults = sub.poll(null, fN, subid, null, null, null, null, null);
				    System.out.println("PollTester pollResults: " + pollResults);
				}
			}
		} catch (Exception e) {
			 System.err.println("PollTester Exception: " + e.getMessage());
		}
	}
	
	public static void main(String[] args) throws FileNotFoundException, IOException, ParserConfigurationException, TransformerException {
		try {
			 System.out.println("PollTester main args.length: " + args.length);
			 if (args.length == 0) {
			     testPollEmpty("1.0");	
			     testPollEmpty("1.1");
			     testPollAll("1.0");
			     testPollAll("1.1");
			     testPollTimestamp("1.0");
			     testPollTimestamp("1.1");
			     testPollContent("1.0");
			 } else {
				 if (args.length == 2) {
					 testPollTimestamp2("1.0", args[1]);
				 } else {
					 String test = args[0];
					 if (test.equals("1")) {
					     testPollEmpty("1.0");
					 } else if (test.equals("2")) {
					    testPollEmpty("1.1");
					 } else if (test.equals("3")) {
					    testPollAll("1.0");
					 } else if (test.equals("4")) {
					    testPollAll("1.1");
					 } else if (test.equals("5")) {
					    testPollTimestamp("1.0");
					 } else if (test.equals("6")) {
					    testPollTimestamp("1.1");
					 } else if (test.equals("7")) {
					    testPollContent("1.0");
					 } else if (test.equals("8")) {
					    testPollContent("1.1");
					 } else {
						 System.out.println("PollTester main no test for args[0]: " + test);
					 }
				 }
			 }
		} catch (Exception e) {
			System.err.println("PollTester Exception: " + e.getMessage());
		}
	}
	
	public static void changeTaxiiVersionConfig(String taxiiVersion, String stixVersion) {
		Properties props = new Properties();
	    InputStream is = null;
	    OutputStream out = null;
	    File f = new File("config.properties");
	    try {
	    	is = new FileInputStream(f);
	        props.load(is);
	        props.setProperty("taxiiVersion", taxiiVersion);
	        if (stixVersion != null) {
	        	props.setProperty("stix_cB", "urn:stix.mitre.org:xml:"+stixVersion);
	        }
	        out = new FileOutputStream(f);
	        props.store(out, "");
	    } catch (IOException e) {
	        System.out.println("IOException e: " + e.getMessage());
	    } finally {
	    	if (is != null) {
	        	try {
	        	    is.close();
	        	} catch (IOException e) {
	        		System.out.println("IOException e: " + e.getMessage());
	        	}
	        }
	        if (out != null) {
	        	try {
	        		out.close();
	        	} catch (IOException e) {
	        		System.out.println("IOException e: " + e.getMessage());
	        	}
	        }
	    }
	}

	static private void testPollEmpty(String taxiiVersion) {
		String fN = "feed1";
		try {
			System.out.println("#####################  PollTester starting testPollEmpty ################## taxiiVersion: " + taxiiVersion);
			changeTaxiiVersionConfig(taxiiVersion, null);
			String bTime = "2014-04-28T23:19:44Z";
		    Calendar cal = Calendar.getInstance();
		    
	      	//get current time
		   	SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
		   	bTime = sdf.format(cal.getTime());
		   	System.out.println("PollTester bTime: " + bTime);
		   	Subscriber10 sub = new Subscriber10();
			String pollResults = sub.poll(null, fN, "0f4f63b8-5a22-11e4-a305-000c2909f434", null, bTime, null, null, null);
		    System.out.println("PollTester testPollEmpty pollResults: " + pollResults);
		} catch(Exception e) {
			 System.err.println("PollTester Exception: " + e.getMessage());
		}
		System.out.println("#####################  PollTester Ending testPollEmpty ##################  taxiiVersion: " + taxiiVersion);
	}

	static private void testPollAll(String taxiiVersion) {
		String fN = "feed1";
		System.out.println("#####################  PollTester starting testPollAll ##################  taxiiVersion: " + taxiiVersion);
		changeTaxiiVersionConfig(taxiiVersion, null);
	   	Subscriber10 sub = new Subscriber10();
		String pollResults = sub.poll(null, fN, "0f4f63b8-5a22-11e4-a305-000c2909f434", null, null, null, null, null);
	    System.out.println("PollTester testPollAll pollResults: " + pollResults);
	    System.out.println("#####################  PollTester Ending testPollAll ################## ");
	}

	static private void testPollTimestamp(String taxiiVersion) {
		String fN = "feed1";
		System.out.println("#####################   PollTester Starting testPollTimestamp ##################  taxiiVersion: " + taxiiVersion);
		changeTaxiiVersionConfig(taxiiVersion, null);
	   	Subscriber10 sub = new Subscriber10();
	   	String bTime = "2014-04-28T23:19:44Z";
		String eTime = "2014-04-28T23:19:44Z";
	    Calendar cal = Calendar.getInstance();
	    
     	//get current time
	   	SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
	   	eTime = sdf.format(cal.getTime());
	   	System.out.println("PollTester bTime: " + eTime);
	   	cal.add(Calendar.DATE, -1);
	    bTime = sdf.format(cal.getTime());
	 	System.out.println("PollTester bTime: " + bTime);
		String pollResults = sub.poll(null, fN, "0f4f63b8-5a22-11e4-a305-000c2909f434", null, bTime, eTime, null, null);
		System.out.println("PollTester testPollAll pollResults: " + pollResults);
		System.out.println("#####################   PollTester Ending testPollTimestamp ##################  taxiiVersion: " + taxiiVersion);
	}

	static private void testPollContent(String taxiiVersion) {
		String fN = "feed1";
		System.out.println("#####################   PollTester starting testPollContent ##################  taxiiVersion: "+taxiiVersion);
		String content_binding = "urn:stix.mitre.org:xml:1.1";
		String content_binding2 = "urn:stix.mitre.org:xml:1.1.1";
		Set<String> bindingSet = new HashSet<String>();
		bindingSet.add(content_binding);
		bindingSet.add(content_binding2);
		changeTaxiiVersionConfig(taxiiVersion, null);
		Subscriber10 sub = new Subscriber10();
		String pollResults = sub.poll(null, fN, "0f4f63b8-5a22-11e4-a305-000c2909f434", null, null, null, bindingSet, null);
		System.out.println("PollTester testPollAll pollResults: " + pollResults);
		System.out.println("#####################   PollTester Ending testPollContent ##################  taxiiVersion: " + taxiiVersion);
	}

	static private void testPollTimestamp2(String taxiiVersion, String subID) {
		String fN = "feed1";
		System.out.println("#####################   PollTester Starting testPollTimestamp2 ##################  taxiiVersion: " + taxiiVersion + " subID: " + subID);
		changeTaxiiVersionConfig(taxiiVersion, null);
	   	Subscriber10 sub = new Subscriber10();
	   	String bTime = "2014-04-28T23:19:44Z";
		String eTime = "2014-04-28T23:19:44Z";
	    Calendar cal = Calendar.getInstance();
	    
     	//get current time
	   	SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
	   	eTime = sdf.format(cal.getTime());
	   	System.out.println("PollTester bTime: " + eTime);
	   	cal.add(Calendar.DATE, -1);
	    bTime = sdf.format(cal.getTime());
	 	System.out.println("PollTester bTime: " + bTime);
		String pollResults = sub.poll(null, fN, subID, null, bTime, eTime, null, null);
		System.out.println("PollTester testPollAll pollResults: " + pollResults);
		System.out.println("#####################   PollTester Ending testPollTimestamp2 ##################  taxiiVersion: " + taxiiVersion);
	}
}