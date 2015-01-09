package com.bcmcgroup.flare.client;

import org.apache.log4j.Logger;
import org.mitre.stix.validator.SchemaError;
import org.w3c.dom.Document;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class TaxiiValidator {
	private static final Logger logger = Logger.getLogger(TaxiiValidator.class);
	final SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
	private Schema schema;
	
	public TaxiiValidator() throws SAXException {
		schema = schemaFactory.newSchema(Paths.get("schemas/uber_schema.xsd").toFile());
	}
	
	public boolean validate(Document taxiiDoc) throws SAXException, IOException {
		Source source = new DOMSource(taxiiDoc);
		Validator validator = schema.newValidator();
		TaxiiErrorHandler errorHandler = new TaxiiValidator.TaxiiErrorHandler();
		validator.setErrorHandler(errorHandler);
		try {
			validator.validate(source);
		} catch (SAXParseException e) {}
		
		List<SchemaError> errors = errorHandler.getErrors();
		if (errors.size() > 0) {
			for (SchemaError error : errors) {
				logger.debug("SchemaError error getCategory: " + error.getCategory());
				logger.debug("SchemaError error getMessage: " + error.getMessage());
			}
			logger.error("Message was not published due to TAXII validation errors!  Please check content and try again.");
			return false;
		} else {
			logger.debug("validateTaxii result: No Errors");
			return true;
		}
	}

	public class TaxiiErrorHandler implements ErrorHandler {
		List<SchemaError> errors = new ArrayList<SchemaError>();
		
		@Override
		public void warning(SAXParseException exception) throws SAXException {
			errors.add(SchemaError.fromException(exception, SchemaError.Categories.WARNING));
		}

		@Override
		public void error(SAXParseException exception) throws SAXException {
			errors.add(SchemaError.fromException(exception, SchemaError.Categories.ERROR));
		}

		@Override
		public void fatalError(SAXParseException exception) throws SAXException {
			errors.add(SchemaError.fromException(exception, SchemaError.Categories.FATAL_ERROR));
		}

		public List<SchemaError> getErrors() {
			return errors;
		}
	}
}