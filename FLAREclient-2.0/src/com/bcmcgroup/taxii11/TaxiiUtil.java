package com.bcmcgroup.taxii11;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;

import javax.net.ssl.HttpsURLConnection;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import com.bcmcgroup.flare.client.ClientUtil;
import com.bcmcgroup.flare.client.Subscriber11;
import com.bcmcgroup.taxii11.ContentBlock;

public class TaxiiUtil {
	private static final Logger logger = Logger.getLogger(Subscriber11.class);
	private static Properties config = ClientUtil.loadProperties();
	private static final String taxiiNS = config.getProperty("taxii11NS");
	private static final String taxiiQueryNS = config.getProperty("taxiiQuery10NS");
	
	/**
	 * Utility used for capturing pairs of Strings
	 */
	public static class Pair {
	    private String l;
	    private String r;
	    
	    /**
	     * Constructor
	     * @param l left string
	     * @param r right string
	     */
	    public Pair(String l, String r) {
	        this.l = l;
	        this.r = r;
	    }
	}
	
	/**
	 * Used to construct a Poll_Parameters object for a poll request without a Subscription_ID
	 */
	public static class PollParameters {
		boolean allowAsynch;
		String respType, pBinding, addr, mBinding;
		Set<String> cBinding;
		TaxiiDefaultQuery tdq;
		
		/**
		 * Constructor
		 * @param allowAsynch if true then the allow_asynch attribute will be set to true
		 * @param respType optional Response_Type tag
		 * @param cBinding Set of allowable content bindings
		 * @param tdq an optional TaxiiDefaultQuery object 
		 * @param pBinding if using Delivery_Parameters, the Protocol_Binding for delivery (ignored if either addr or mBinding is null)
		 * @param addr if using Delivery_Parameters, the Address for delivery (ignored if either pBinding or mBinding is null)
		 * @param mBinding if using Delivery_Parameters, the Message_Binding for delivery (ignored if either addr or pBinding is null)
		 */
		PollParameters(boolean allowAsynch, String respType, Set<String> cBinding, TaxiiDefaultQuery tdq, String pBinding, String addr, String mBinding) {
			this.allowAsynch = allowAsynch;
			this.respType = respType;
			this.cBinding = cBinding;
			this.tdq = tdq;
			this.pBinding = pBinding;
			this.addr = addr;
			this.mBinding = mBinding;
		}
		
		public void appendToDocument(Document taxiiDoc) {
			try {
				logger.debug("appendToDocument taxiiDoc: " + taxiiDoc);
				Element pp = taxiiDoc.createElementNS(taxiiNS, "Poll_Parameters");
				
				// allow_asynch
				if (this.allowAsynch) {
					pp.setAttribute("allow_asynch", "true");
				} else {
					pp.setAttribute("allow_asynch", "false");
				}
				
				// Response_Type
				if (this.respType != null && this.respType == "COUNT_ONLY") {
					Element rt = taxiiDoc.createElementNS(taxiiNS, "Response_Type");
					rt.appendChild(taxiiDoc.createTextNode("COUNT_ONLY"));
					taxiiDoc.appendChild(rt);
				} else {
					Element rt = taxiiDoc.createElementNS(taxiiNS, "Response_Type");
					rt.appendChild(taxiiDoc.createTextNode("FULL"));
					taxiiDoc.appendChild(rt);
				}
				
				// Content_Binding
				if (this.cBinding != null && !this.cBinding.isEmpty()) {
					for (String binding : this.cBinding) {
						Element cb = taxiiDoc.createElementNS(taxiiNS, "Content_Binding");
						cb.setAttribute("binding_id", binding);
						taxiiDoc.appendChild(cb);
					}
				}
				
				// Query
				if (this.tdq != null) {
					tdq.appendToDocument(taxiiDoc);
				}
				
				// Delivery_Parameters
				if (this.pBinding != null && this.addr != null && this.mBinding != null) {
					Element dp = taxiiDoc.createElementNS(taxiiNS, "Delivery_Parameters");
					Element pb = taxiiDoc.createElementNS(taxiiNS, "Protocol_Binding");
					pb.appendChild(taxiiDoc.createTextNode(this.pBinding));
					dp.appendChild(pb);
					Element a = taxiiDoc.createElementNS(taxiiNS, "Address");
					a.appendChild(taxiiDoc.createTextNode(this.addr));
					dp.appendChild(a);
					Element mb = taxiiDoc.createElementNS(taxiiNS, "Message_Binding");
					mb.appendChild(taxiiDoc.createTextNode(this.mBinding));
					dp.appendChild(mb);
					taxiiDoc.appendChild(dp);
				}
			} catch (SAXException e) {
				logger.error("appendToDocument SAXException: " + e.getMessage());
			} catch (IOException e) {
				logger.error("appendToDocument IOException: " + e.getMessage());
			} catch (ParserConfigurationException e) {
				logger.error("appendToDocument ParserConfigurationException: " + e.getMessage());
			}
		}
	}
	
	/**
	 * Used to construct a Record_Count object for an Inbox_Message
	 */
	private class RecordCount {
		private int Record_Count;
		private boolean partial_count;

		/**
		 * Overloaded Constructor
		 * @param recordCount indicating the record count
		 */
		private RecordCount(int recordCount) {
			partial_count = false;
			Record_Count = recordCount;
		}
		
		/**
		 * Overloaded Constructor
		 * @param recordCount indicating the record count
		 * @param partialCount if true, then the partial_count attribute will be set to true
		 */
		private RecordCount(int recordCount, boolean partialCount) {
			partial_count = partialCount;
			Record_Count = recordCount;
		}
		
		public boolean getPartialCount() {
			return this.partial_count;
		}
		
		public int getRecordCount() {
			return this.Record_Count;
		}
	}
	
	/**
	 * Used to construct a TAXII Default Query Criterion object for a TaxiiDefaultQuery instance
	 */
	public static class QueryCriterion {
		boolean negate;
		String target, capId, relationship;
		List<Pair> paramList;
		
		/**
		 * Constructor
		 * @param negate if true, then the negate attribute will be set true in this Criterion tag
		 * @param target the Target tag
		 * @param capId the capability_id attribute for the Test tag
		 * @param relationship the relationship attribute for the Test tag
		 * @param paramList a List of pairs (name, value) for the Parameter tags
		 */
		QueryCriterion(boolean negate, String target, String capId, String relationship, List<Pair> paramList) {
			this.negate = negate;
			this.target = target;
			this.capId = capId;
			this.relationship = relationship;
			this.paramList = paramList;
		}
	}
	
	/**
	 * Used to construct a TAXII Default Query object for a Poll_Request
	 */
	public static class TaxiiDefaultQuery {
		String targetExpId;
		boolean operatorAND;
		List<QueryCriterion> criteriaList;
		
		/**
		 * Constructor
		 * @param targetExpId the target_expression_id
		 * @param operatorAND a boolean indicating true for AND and false for OR
		 * @param criteriaList a list of QueryCriterion objects to construct the Criterion tags for the query
		 */
		TaxiiDefaultQuery(String targetExpId, boolean operatorAND, List<QueryCriterion> criteriaList) {
			this.targetExpId = targetExpId;
			this.operatorAND = operatorAND;
			this.criteriaList = criteriaList;
		}
		
		public void appendToDocument(Document taxiiDoc) throws SAXException, IOException, ParserConfigurationException {
			Element q = taxiiDoc.createElementNS(taxiiNS, "Query");
			q.setAttribute("format_id", "urn:taxii.mitre.org:query:default:1.0");
			Element dq = taxiiDoc.createElementNS(taxiiQueryNS, "Default_Query");
			dq.setAttribute("targeting_expression_id", this.targetExpId);
			
			// Criteria
			Element criteria = taxiiDoc.createElementNS(taxiiQueryNS, "Criteria");
			if (this.operatorAND) {
				criteria.setAttribute("operator", "AND");
			} else {
				criteria.setAttribute("operator", "OR");
			}
			for (QueryCriterion c : this.criteriaList) {
				Element criterion = taxiiDoc.createElementNS(taxiiQueryNS, "Criterion");
				if (c.negate) {
					criterion.setAttribute("negate", "true");
				}
				Element tgt = taxiiDoc.createElementNS(taxiiQueryNS, "Target");
				tgt.appendChild(taxiiDoc.createTextNode(c.target));
				criterion.appendChild(tgt);
				Element tst = taxiiDoc.createElementNS(taxiiQueryNS, "Test");
				tst.setAttribute("capability_id", c.capId);
				tst.setAttribute("relationship", c.relationship);
				for (Pair p : c.paramList) {
					Element param = taxiiDoc.createElementNS(taxiiQueryNS, "Parameter");
					param.setAttribute("name", p.l);
					param.appendChild(taxiiDoc.createTextNode(p.r));
					tst.appendChild(param);
				}
				criterion.appendChild(tst);
				criteria.appendChild(criterion);
			}
			dq.appendChild(criteria);
			q.appendChild(dq);
		}
	}
	
	/**
	 * Builds a connection to TAXII server
	 * @param requestType String containing "poll" or "inbox" for poll requests or inbox requests, respectively
	 * @return HttpsURLConnection object connected to TAXII server
	 */
	public static HttpsURLConnection buildConnection(String requestType) {
		try {
			URL url = new URL(config.getProperty("taxii11serverUrl") + requestType);
			logger.debug("buildConnection url: " + url);
			HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
			conn.setRequestMethod("POST");
			conn.setRequestProperty("User-Agent", config.getProperty("httpHeaderUserAgent"));
			conn.setRequestProperty("Content-Type", config.getProperty("httpHeaderContentType"));
			conn.setRequestProperty("Accept", config.getProperty("httpHeaderAccept"));
			conn.setRequestProperty("X-TAXII-Accept", "urn:taxii.mitre.org:message:xml:1.1");
			conn.setRequestProperty("X-TAXII-Content-Type", "urn:taxii.mitre.org:message:xml:1.1");
			conn.setRequestProperty("X-TAXII-Protocol", "urn:taxii.mitre.org:protocol:https:1.0");
			conn.setRequestProperty("X-TAXII-Services", "urn:taxii.mitre.org:services:1.1");
			conn.setDoOutput(true);
			return conn;
		} catch (MalformedURLException ioe) {
			logger.error("poll IOException ioe: " + ioe.getMessage());
		} catch (IOException ioe) {
			logger.error("poll IOException ioe: " + ioe.getMessage());
		}
		return null;
	}
	
	/**
	 * Generate a message ID
	 * @return a String containing a UUID
	 */
	private static String generateMsgId() {
		return UUID.randomUUID().toString();
	}
	
	/**
	 * Constructs a TAXII Inbox_Message XML message and returns the Document object
	 * @param msgId the message_id
	 * @param resultId the result_id
	 * @param extHdrs a HashMap containing all extended headers
	 * @param destCollectionName the destination collection name
	 * @param message text for the Message tag
	 * @param sourceSubscription a HashMap containing all Source_Subscription attributes and tags (collection_name, Subscription_ID, Exclusive_Begin_Timestamp, Inclusive_End_Timestamp)
	 * @param recordCount a RecordCount object containing info for the Record_Count tag
	 * @param contentBlocks an ArrayList of ContentBlock objects
	 * 
	 * Usage Example:
	 *   Document im = inboxMessage("12345", null, "Here is my message", null, contentBlocks);
	 */
	public static Document inboxMessage(String msgId, String resultId, HashMap<String,String> extHdrs, String destCollectionName, String message, 
			HashMap<String,String> sourceSubscription, RecordCount recordCount, List<ContentBlock> contentBlocks) {
		DocumentBuilder db = ClientUtil.generateDocumentBuilder();
		Document taxiiDoc = db.newDocument();
		
		// Inbox_Message (root element)
		Element taxiiRoot = taxiiDoc.createElementNS(taxiiNS, "Inbox_Message");
		taxiiDoc.appendChild(taxiiRoot);
		if (msgId == null) {
		    taxiiRoot.setAttribute("message_id", generateMsgId());
		} else {
			taxiiRoot.setAttribute("message_id", msgId);
		}
		if (resultId != null) {
		    taxiiRoot.setAttribute("result_id", resultId);
		}
		
		// Extended_Headers
		if (extHdrs != null && !extHdrs.keySet().isEmpty()) {
			Element eHs = taxiiDoc.createElementNS(taxiiNS, "Extended_Headers");
			Element eH = taxiiDoc.createElementNS(taxiiNS, "Extended_Header");
			for (String name : extHdrs.keySet()) {
				eH.setAttribute("name", name);
				eH.appendChild(taxiiDoc.createTextNode(extHdrs.get(name)));
				eHs.appendChild(eH);
			}
			taxiiRoot.appendChild(eHs);
		}
		
		// Destination_Collection_Name
		if (destCollectionName != null) {
			Element dcn = taxiiDoc.createElementNS(taxiiNS, "Destination_Collection_Name");
			dcn.appendChild(taxiiDoc.createTextNode(destCollectionName));
			taxiiRoot.appendChild(dcn);
		}
		
		// Message
		if (message != null) {
			Element msg = taxiiDoc.createElementNS(taxiiNS, "Message");
			msg.appendChild(taxiiDoc.createTextNode(message));
			taxiiRoot.appendChild(msg);
		}
		
		// Source_Subscription
		if (sourceSubscription != null && !sourceSubscription.keySet().isEmpty()) {
			Element ss = taxiiDoc.createElementNS(taxiiNS, "Source_Subscription");
			Set<String> ssKeys = sourceSubscription.keySet();
			if (ssKeys.contains("collection_name")) {
				ss.setAttribute("collection_name", sourceSubscription.get("collection_name"));
			}
			if (ssKeys.contains("Subscription_ID")) {
				Element si = taxiiDoc.createElementNS(taxiiNS, "Subscription_ID");
				si.appendChild(taxiiDoc.createTextNode(sourceSubscription.get("Subscription_ID")));
				ss.appendChild(si);
			}
			if (ssKeys.contains("Exclusive_Begin_Timestamp")) {
				Element ebt = taxiiDoc.createElementNS(taxiiNS, "Exclusive_Begin_Timestamp");
				ebt.appendChild(taxiiDoc.createTextNode(sourceSubscription.get("Exclusive_Begin_Timestamp")));
				ss.appendChild(ebt);
			}
			if (ssKeys.contains("Inclusive_End_Timestamp")) {
				Element iet = taxiiDoc.createElementNS(taxiiNS, "Inclusive_End_Timestamp");
				iet.appendChild(taxiiDoc.createTextNode(sourceSubscription.get("Inclusive_End_Timestamp")));
				ss.appendChild(iet);
			}
			taxiiRoot.appendChild(ss);
		}
		
		// Record_Count
		if (recordCount != null) {
			Element rc = taxiiDoc.createElementNS(taxiiNS, "Record_Count");
			if (recordCount.getPartialCount()) {
				rc.setAttribute("partial_count", "true");
			} else {
				rc.setAttribute("partial_count", "false");
			}
			rc.appendChild(taxiiDoc.createTextNode(Integer.toString(recordCount.getRecordCount())));
			taxiiRoot.appendChild(rc);
		}
				
		// Content_Block
		for (ContentBlock cBlock : contentBlocks) {
			cBlock.appendToDocument(taxiiDoc);
		}
		return taxiiDoc;
	}
	
	/**
	 * Constructs a TAXII Poll_Request XML message (with Subscription_ID) and returns the Document object
	 * 
	 * @param msgId the message_id
	 * @param cN the collection name
	 * @param subId the subscription_id
	 * @param extHdrs a HashMap containing all extended headers (can be null)
	 * @param ebt the Exclusive_Begin_Timestamp value
	 * @param iet the Inclusive_End_Timestamp value
	 * 
	 * Usage Example:
	 *   Document pr = pollRequest("12345", "myCollection", "12345678-90ab-cdef-1234-567890abcdef", null, "2014-05-24T22:23:00.000000Z", null);
	 */
	public static Document pollRequest(String msgId, String cN, String subId, HashMap<String,String> extHdrs, String ebt, String iet) {
		DocumentBuilder db = ClientUtil.generateDocumentBuilder();
		Document taxiiDoc = db.newDocument();

		// Poll_Request (root element)
		Element taxiiRoot = taxiiDoc.createElementNS(taxiiNS, "Poll_Request");
		taxiiDoc.appendChild(taxiiRoot);
		if (msgId == null) {
		    taxiiRoot.setAttribute("message_id", generateMsgId());
		} else {
			taxiiRoot.setAttribute("message_id", msgId);
		}
		if (cN != null) {
			taxiiRoot.setAttribute("collection_name", cN);
		} else {
			logger.error("PollRequest cN cannot be null, returning null document");
			return null;
		}
		
		// Extended_Headers
		if (extHdrs != null && !extHdrs.keySet().isEmpty()) {
			Element eHs = taxiiDoc.createElementNS(taxiiNS, "Extended_Headers");
			Element eH = taxiiDoc.createElementNS(taxiiNS, "Extended_Header");
			for (String name : extHdrs.keySet()) {
				eH.setAttribute("name", name);
				eH.appendChild(taxiiDoc.createTextNode(extHdrs.get(name)));
				eHs.appendChild(eH);
			}
			taxiiRoot.appendChild(eHs);
		}
				
		// Exclusive_Begin_Timestamp
		if (ebt != null) {
			Element bT = taxiiDoc.createElementNS(taxiiNS, "Exclusive_Begin_Timestamp");
			bT.appendChild(taxiiDoc.createTextNode(ebt));
			taxiiRoot.appendChild(bT);
		}
				
		// Inclusive_End_Timestamp
		if (iet != null) {
			Element eT = taxiiDoc.createElementNS(taxiiNS, "Inclusive_End_Timestamp");
			eT.appendChild(taxiiDoc.createTextNode(iet));
			taxiiRoot.appendChild(eT);
		}
		
		// Subscription_ID
		if (subId != null) {
			Element si = taxiiDoc.createElementNS(taxiiNS, "Subscription_ID");
			si.appendChild(taxiiDoc.createTextNode(subId));
			taxiiRoot.appendChild(si);
		}
		return taxiiDoc;
	}

	/**
	 * Constructs a TAXII Poll_Request XML message (with Poll_Parameters) and returns the Document object
	 * 
	 * @param msgId the message_id
	 * @param cN the collection name
	 * @param extHdrs a HashMap containing all extended headers (can be null)
	 * @param ebt the Exclusive_Begin_Timestamp value
	 * @param iet the Inclusive_End_Timestamp value
	 * 
	 * Usage Example:
	 *   Document pr = pollRequest("12345", "myCollection", null, "2014-05-24T22:23:00.000000Z", null);
	 */
	public static Document pollRequest(String msgId, String cN, HashMap<String,String> extHdrs, String ebt, String iet, PollParameters pp) {
		DocumentBuilder db = ClientUtil.generateDocumentBuilder();
		Document taxiiDoc = db.newDocument();

		// Poll_Request (root element)
		Element taxiiRoot = taxiiDoc.createElementNS(taxiiNS, "Poll_Request");
		taxiiDoc.appendChild(taxiiRoot);
		if (msgId == null) {
		    taxiiRoot.setAttribute("message_id", generateMsgId());
		} else {
			taxiiRoot.setAttribute("message_id", msgId);
		}
		if (cN != null) {
			taxiiRoot.setAttribute("collection_name", cN);
		} else {
			logger.error("PollRequest cN cannot be null, returning null document");
			return null;
		}
		
		// Extended_Headers
		if (extHdrs != null && !extHdrs.keySet().isEmpty()) {
			Element eHs = taxiiDoc.createElementNS(taxiiNS, "Extended_Headers");
			Element eH = taxiiDoc.createElementNS(taxiiNS, "Extended_Header");
			for (String name : extHdrs.keySet()) {
				eH.setAttribute("name", name);
				eH.appendChild(taxiiDoc.createTextNode(extHdrs.get(name)));
				eHs.appendChild(eH);
			}
			taxiiRoot.appendChild(eHs);
		}
				
		// Exclusive_Begin_Timestamp
		if (ebt != null) {
			Element bT = taxiiDoc.createElementNS(taxiiNS, "Exclusive_Begin_Timestamp");
			bT.appendChild(taxiiDoc.createTextNode(ebt));
			taxiiRoot.appendChild(bT);
		}
				
		// Inclusive_End_Timestamp
		if (iet != null) {
			Element eT = taxiiDoc.createElementNS(taxiiNS, "Inclusive_End_Timestamp");
			eT.appendChild(taxiiDoc.createTextNode(iet));
			taxiiRoot.appendChild(eT);
		}
		
		// Poll_Parameters
		pp.appendToDocument(taxiiDoc);
		
		return taxiiDoc;
	}
}