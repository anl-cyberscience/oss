package com.bcmcgroup.taxii;

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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;
/** 
 * @author		Mark Walters <mwalters@bcmcgroup.com>
 * @version		1.0
 */
public class ContentBlock {
	private static final Logger logger = Logger.getLogger(ContentBlock.class);
	
	private static final int DEFAULT_BUFFER_SIZE = 65536;  // TODO may want to mess with this number to see how it affects performance
	private String content_binding, content, timestamp_label, padding, ds_signature;
	
	/**
	 * Default constructor
	 */
	public ContentBlock() {}
	
	/**
	 * Constructor to be used for Content in a String
	 * @param cB String containing Content_Binding
	 * @param c String containing Content
	 * @param tsL String containing Timestamp_Label
	 * @param padd String containing Padding
	 * @param dsS String containing ds:Signature
	 */
	public ContentBlock(String cB, String c, String tsL, String padd, String dsS) {
		content_binding = cB;
		content = c;
		timestamp_label = tsL;
		padding = padd;
		ds_signature = dsS;
	}
	
	/**
	 * Constructor to be used for Content in a File
	 * @param cB String containing Content_Binding
	 * @param c File containing Content
	 * @param tsL String containing Timestamp_Label
	 * @param padd String containing Padding
	 * @param dsS String containing ds:Signature
	 */
	public ContentBlock(String cB, File c, String tsL, String padd, String dsS) throws IOException {
		logger.debug("ContentBlock cB: " + cB + " File c: " + c + " tsL: " + tsL + " padd: " + padd + " dsS: " + dsS);	
		content_binding = cB;
		if (c.exists()) {
			logger.debug("ContentBlock reading file c.canRead(): " + c.canRead());
			logger.debug("ContentBlock reading file c.length(): " + c.length()); 
			logger.debug("ContentBlock reading file c: " + c);
			FileInputStream fis = null;
			try {
				fis = new FileInputStream(c);
			    content = readFileToString(fis);
			} catch (IOException e) {
				logger.error("ContentBlock IOException e: " + e.getMessage());
				throw new IOException (e);
			} finally {	 
				if (fis != null) {
					try {
						fis.close();
					} catch (IOException ioe) {
						logger.error("ContentBlock IOException ioe: " + ioe.getMessage());
					}
					fis = null;
				}
			}	
			logger.debug("ContentBlock content: " + content);
		} else {
			throw new IOException("File " + c + " does not exist");
		}
		timestamp_label = tsL;
		padding = padd;
		ds_signature = dsS;		
	}
	
	public String getContent() {
		return this.content;
	}
	
	public void printStuff() {
		logger.debug("cB = " + this.content_binding);
		logger.debug("tsL = " + this.timestamp_label);
		logger.debug("padd = " + this.padding);
		logger.debug("dsS = " + this.ds_signature);
	}

	/**
	 * Appends the calling ContentBlock object to the provided TAXII Document
	 * @param taxiiDoc TAXII Document object
	 * @throws SAXException
	 * @throws IOException
	 * @throws ParserConfigurationException
	 */
	public void appendToDocument(Document taxiiDoc) throws SAXException, IOException, ParserConfigurationException {
		logger.debug("appendToDocument taxiiDoc: " + taxiiDoc);
		if (this.content_binding == null) {
			logger.debug("Content_Binding cannot be null!");
			return;
		} else if (this.content == null) {
			logger.debug("Content cannot be null!");
			return;
		}		
		Element cBlock = taxiiDoc.createElement("Content_Block");
		
		// Content_Binding
		Element cBinding = taxiiDoc.createElement("Content_Binding");
		cBinding.appendChild(taxiiDoc.createTextNode(this.content_binding));
		cBlock.appendChild(cBinding);
		logger.debug("appendToDocument content.length(): " + content.length());		
		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
		docFactory.setValidating(true);
		docFactory.setIgnoringElementContentWhitespace(true);
		docFactory.setNamespaceAware(true);
		docFactory.setIgnoringComments(true);
		docFactory.setFeature("http://xml.org/sax/features/external-general-entities", false);
		docFactory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);		
		docFactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
		docFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
		
		// Content
		Element c = taxiiDoc.createElement("Content");		
		Element element = docBuilder.parse(new ByteArrayInputStream(this.content.getBytes())).getDocumentElement();
		c.appendChild(taxiiDoc.importNode(element, true));
		cBlock.appendChild(c);
		
		// Timestamp_Label (optional)
		if (this.timestamp_label != null) {
			Element tsL = taxiiDoc.createElement("Timestamp_Label");
			tsL.appendChild(taxiiDoc.createTextNode(this.timestamp_label));
			cBlock.appendChild(tsL);
		}
		
		// Padding
		if (this.padding != null) {
			Element padd = taxiiDoc.createElement("Padding");
			padd.appendChild(taxiiDoc.createTextNode(this.padding));
			cBlock.appendChild(padd);
		}
		
		// ds:Signature
		if (this.ds_signature != null) {
			Element dsSig = taxiiDoc.createElement("ds:Signature");
			dsSig.appendChild(taxiiDoc.createTextNode(this.ds_signature));
			cBlock.appendChild(dsSig);
		}
		
		taxiiDoc.getDocumentElement().appendChild(cBlock);
	}
	
	/**
	 * Converts a FileInputStream object into a String
	 * @param input FileInputStream object containing desired input
	 * @return String containing converted input, using UTF-8
	 * @throws IOException
	 */
	private static String readFileToString(FileInputStream input) throws IOException {
		try {
			ByteArrayOutputStream output = new ByteArrayOutputStream();
			byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
			int n = 0;
			int available = input.available();
			logger.debug("readFileToString available: " + available);
			while (-1 != (n = input.read(buffer))) {
				output.write(buffer, 0, n);
			}
			byte[] barray = output.toByteArray();
			output.flush();
			output.close();
			input.close();
			return new String(barray, StandardCharsets.UTF_8);
		} catch (IOException io) {
			logger.error("readFileToString Exception io: " + io.getMessage());
			throw new IOException(io);
		}
	}
}