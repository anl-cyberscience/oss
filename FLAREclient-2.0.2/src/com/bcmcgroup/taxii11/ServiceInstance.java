package com.bcmcgroup.taxii11;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

import com.bcmcgroup.flare.client.ClientUtil;

	/**
	 * Used to construct a Service_Instance object for a Discovery_Response
	 */
	public class ServiceInstance {
		private static final Logger logger = Logger.getLogger(ServiceInstance.class);
		private static final String taxiiQueryNS = "http://taxii.mitre.org/query/taxii_default_query-1";
		String serviceType;
		String serviceVersion;
		boolean available;
		String protocolBinding;
		String address;
		String messageBinding;
		String formatId;
		String capabilityModuleId;
		List<TargetingExpressionInfo> targetingInfoList = new ArrayList<TargetingExpressionInfo>();
		Properties config = ClientUtil.loadProperties();
		String taxiiNS = config.getProperty("taxii11NS");
		
		public ServiceInstance (String serviceType) {
			this.serviceType = serviceType;
		}
		
		public void setServiceVersion(String version) {
			this.serviceVersion = version;
		}
		
		public void setAvailable(boolean available) {
			this.available = available;
		}
		
		public void setProtocolBinding(String pBinding)	{
			this.protocolBinding = pBinding;
		}
		
		public void setAddress(String address) {
			this.address = address;
		}
		
		public void setMessageBinding(String messageBinding) {
			this.messageBinding = messageBinding;
		}

		public void setFormatId(String formatId) {
			this.formatId = formatId;
		}

		public void setCapabilityModuleId(String capabilityModuleId) {
			this.capabilityModuleId = capabilityModuleId;
		}

		public void addTargetingInfo(TargetingExpressionInfo info) {
			this.targetingInfoList.add(info);
		}

		public boolean appendToDocument(Document taxiiDoc) {
			try {
				logger.debug("appendToDocument taxiiDoc: " + taxiiDoc);
				
				// root element
				Element serviceInstance = taxiiDoc.createElementNS(taxiiNS, "Service_Instance");
				if (serviceType != null) {
					serviceInstance.setAttribute("service_type", this.serviceType);
				} else {
					logger.error("Discovery_Response Service_Instance requires service_type attribute! No response sent!");
					return false;
				}
				if (serviceVersion != null) {
					serviceInstance.setAttribute("service_version", this.serviceVersion);
				} else {
					logger.error("Discovery_Response Service_Instance requires service_version attribute! No response sent!");
					return false;
				}
				
				// Protocol_Binding
				Element protocolBinding = taxiiDoc.createElementNS(taxiiNS, "Protocol_Binding");
				if (this.protocolBinding != null) {
					Text pText = taxiiDoc.createTextNode(this.protocolBinding);
					protocolBinding.appendChild(pText);
					serviceInstance.appendChild(protocolBinding);
				} else {
					logger.error("Discovery_Response Service_Instance requires Protocol_Binding element! No response sent!");
					return false;
				}
				
				// Address
				Element address = taxiiDoc.createElementNS(taxiiNS, "Address");
				if (this.address != null) {
					Text addressText = taxiiDoc.createTextNode(this.address);
					address.appendChild(addressText);
					serviceInstance.appendChild(address);
				} else {
					logger.error("Discovery_Response Service_Instance requires Address element! No response sent!");
					return false;
				}
				
				// Message_Binding
				Element messageBinding = taxiiDoc.createElementNS(taxiiNS, "Message_Binding");
				if (this.messageBinding != null) {
					Text messageBindingText = taxiiDoc.createTextNode(this.messageBinding);
					messageBinding.appendChild(messageBindingText);
					serviceInstance.appendChild(messageBinding);
				} else {
					logger.error("Discovery_Response Service_Instance requires Message_Binding element! No response sent!");
					return false;
				}
				
				// Supported Query
				if (this.formatId != null) {
					Element query = taxiiDoc.createElementNS(taxiiNS, "Supported_Query");
					query.setAttribute("format_id", this.formatId);
					
					Element queryInfo = taxiiDoc.createElementNS(taxiiQueryNS, "Default_Query_Info");
	
					// Targeting_Expression_Info
					for (TargetingExpressionInfo info : targetingInfoList) {
						Element targetingInfo = taxiiDoc.createElementNS(taxiiQueryNS, "Targeting_Expression_Info");
						targetingInfo.setAttribute("targeting_expression_id", info.getTargetingExpressionId());
						List<String> scopeList = info.getAllowedScopeList();
						for (String scope : scopeList) {
							Element allowedScope = taxiiDoc.createElementNS(taxiiQueryNS, "Allowed_Scope");
							allowedScope.appendChild(taxiiDoc.createTextNode(scope));
							targetingInfo.appendChild(allowedScope);
						}
						queryInfo.appendChild(targetingInfo);
					}
					
					// Capability_Module
					if (this.capabilityModuleId != null) {
						Element capabilityModule = taxiiDoc.createElementNS(taxiiQueryNS, "Capability_Module");
						Text moduleText = taxiiDoc.createTextNode(this.capabilityModuleId);
						capabilityModule.appendChild(moduleText);
						queryInfo.appendChild(capabilityModule);
					}
					
					query.appendChild(queryInfo);
					serviceInstance.appendChild(query);
				}
				taxiiDoc.getDocumentElement().appendChild(serviceInstance);
				return true;
			} catch (DOMException e) {
				logger.error("appendToDocument SAXException: " + e.getMessage());
				return false;
			}
		}
		
		public class TargetingExpressionInfo {
			List<String> allowedScopeList = new ArrayList<String>();
			String targetingExpressionId;

			public void addAllowedScope(String scope) {
				allowedScopeList.add(scope);
			}

			public List<String> getAllowedScopeList() {
				return allowedScopeList;
			}

			public void setTargetingExpressionId(String id) {
				this.targetingExpressionId = id;
			}

			public String getTargetingExpressionId() {
				return this.targetingExpressionId;
			}
		}
	}
	
