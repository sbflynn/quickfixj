/*******************************************************************************
 * Copyright (c) quickfixengine.org  All rights reserved.
 *
 * This file is part of the QuickFIX FIX Engine
 *
 * This file may be distributed under the terms of the quickfixengine.org
 * license as defined by quickfixengine.org and appearing in the file
 * LICENSE included in the packaging of this file.
 *
 * This file is provided AS IS with NO WARRANTY OF ANY KIND, INCLUDING
 * THE WARRANTY OF DESIGN, MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE.
 *
 * See http://www.quickfixengine.org/LICENSE for licensing information.
 *
 * Contact ask@quickfixengine.org if any conditions of this licensing
 * are not clear to you.
 ******************************************************************************/

package org.quickfixj.codegenerator;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.util.JAXBSource;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.URIResolver;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.maven.plugin.logging.Log;
import org.quickfixj.xml.dictionary.BuildDirective;
import org.quickfixj.xml.dictionary.ComponentDefinition;
import org.quickfixj.xml.dictionary.DataDictionary;
import org.quickfixj.xml.dictionary.Engine.MessageFactory;
import org.quickfixj.xml.dictionary.EnumDecimalType;
import org.quickfixj.xml.dictionary.FieldDefinition;
import org.quickfixj.xml.dictionary.MessageDefinition;
import org.quickfixj.xml.dictionary.ObjectFactory;

/**
 * Generates Message and Field related code for the various FIX versions.
 */
public class MessageCodeGenerator {

	// An arbitrary serial UID which will have to be changed when messages and
	// fields won't be compatible with next versions in terms
	// of java serialization.
	private static final long SERIAL_UID = 20000;

	// The String representation of the UID
	private static final String SERIAL_UID_STR = String.valueOf(SERIAL_UID);

	// The name of the param in the .xsl files to pass the serialVersionUID
	private static final String XSLPARAM_SERIAL_UID = "serialVersionUID";

	private final TransformerFactory transformerFactory = TransformerFactory
			.newInstance("net.sf.saxon.TransformerFactoryImpl", null);

	private final JAXBContext jaxbContext = JAXBContext
			.newInstance(ObjectFactory.class);

	private final GenerateMojo mojo;

	private final MessageFactory directives;

	private final DataDictionary dictionary;

	MessageCodeGenerator(GenerateMojo mojo, MessageFactory directives,
			DataDictionary dictionary) throws JAXBException {

		this.mojo = mojo;
		this.directives = directives;
		this.dictionary = dictionary;

		getLog().info(
				"Successfully created an instance of the QuickFIX source generator");
	}

	private void generateFieldClasses() {

		BuildDirective directive = directives.getFields();

		if (!directive.isBuild()) {
			return;
		}

		File directory = getPackageDirectory(directive);

		logInfo("Generating field classes in directory " + directory);
		writePackageDocumentation(directory, "FIX field definitions for "
				+ directives.getNamespace() + " " + directives.getDecimal());

		Transformer transformer = createTransformer("Fields.xsl");

		for (FieldDefinition field : dictionary.getFields().getField()) {
			if (field.getNumber() < 5000) {
				generateFieldClass(field, directory, transformer);
			}
		}
	}

	private void generateFieldClass(FieldDefinition definition, File directory,
			Transformer transformer) {

		File outputFile = new File(directory, definition.getName() + ".java");
		if (!outputFile.exists()) {
			logDebug("field: " + definition.getName());
			Map<String, String> parameters = createParameters();
			parameters.put("fieldName", definition.getName());
			if (directives.getDecimal() == EnumDecimalType.BD) {
				parameters.put("decimalType", "java.math.BigDecimal");
				parameters.put("decimalConverter", "BigDecimal");
			}
			generateCodeFile(parameters, outputFile, transformer);
		}
	}

	private void generateComponentClasses() {

		BuildDirective directive = directives.getComponents();

		if (!directive.isBuild() || dictionary.getComponents() == null) {
			logInfo("No components to build");
			return;
		}

		File directory = getPackageDirectory(directive);

		if (!dictionary.getComponents().getComponent().isEmpty()) {
			writePackageDocumentation(directory, "Message component classes");

			Transformer transformer = createTransformer("Component.xsl");
			for (ComponentDefinition component : dictionary.getComponents()
					.getComponent()) {
				File outputFile = new File(directory, component.getName()
						+ ".java");
				logDebug("generating component class: " + component.getName());
				Map<String, String> parameters = createParameters();
				parameters.put("itemName", component.getName());
				parameters.put("baseClass", "quickfix.MessageComponent");
				parameters.put("orderedFields", "true");
				generateCodeFile(parameters, outputFile, transformer);
			}
		}
	}

	private int generateMessageSubclasses() {

		BuildDirective directive = directives.getMessages();

		if (!directive.isBuild()) {
			return 0;
		}

		File directory = getPackageDirectory(directive);
		logInfo("Generating field classes in directory " + directory);
		writePackageDocumentation(directory, "FIX message definitions for "
				+ directives.getNamespace() + " " + directives.getDecimal());

		Transformer transformer = createTransformer("MessageSubclass.xsl");

		for (MessageDefinition message : dictionary.getMessages().getMessage()) {
			File outputFile = new File(directory, message.getName() + ".java");
			logDebug("generating message class: " + message.getName());
			Map<String, String> parameters = createParameters();
			parameters.put("itemName", message.getName());
			generateCodeFile(parameters, outputFile, transformer);
		}

		return dictionary.getMessages().getMessage().size();
	}

	private void generateMessageHeaderClass() {

		BuildDirective directive = directives.getMessages();

		if (!directive.isBuild()) {
			return;
		}

		File directory = getPackageDirectory(directive);
		File outputFile = new File(directory, "MessageHeader.java");
		Transformer transformer = createTransformer("MessageHeader.xsl");

		generateCodeFile(createParameters(), outputFile, transformer);
	}

	private void generateMessageTrailerClass() {

		BuildDirective directive = directives.getMessages();

		if (!directive.isBuild()) {
			return;
		}

		File directory = getPackageDirectory(directive);
		File outputFile = new File(directory, "MessageTrailer.java");
		Transformer transformer = createTransformer("MessageTrailer.xsl");

		generateCodeFile(createParameters(), outputFile, transformer);
	}

	private void generateDictionaryServiceClass() {

		BuildDirective directive = directives.getMessages();

		if (!directive.isBuild()) {
			return;
		}

		File directory;
		File outputFile;
		Transformer transformer;

		Map<String, String> parameters = createParameters();
		parameters.put("resource", mojo.getResource());

		directory = getPackageDirectory(directive);
		outputFile = new File(directory, "DictionaryService.java");
		transformer = createTransformer("DictionaryService.xsl");

		generateCodeFile(parameters, outputFile, transformer);

		directory = new File(mojo.getOutputDirectory(), "META-INF/services");
		directory.mkdirs();
		outputFile = new File(directory, "org.quickfixj.spi.DictionaryService");
		transformer = createTransformer("DictionaryServiceLocator.xsl");

		generateCodeFile(parameters, outputFile, transformer);
	}

	private File getPackageDirectory(BuildDirective directive) {

		File directory = mojo.getOutputDirectory();
		String[] path = directive.getPackage().split("\\.");
		for (String string : path) {
			directory = new File(directory, string);
		}
		directory.mkdirs();

		getLog().info(
				String.format("Created directory %s for package %s", directory,
						directive.getPackage()));

		return directory;
	}

	private Transformer createTransformer(String xsltFile) {

		StreamSource styleSource;
		File xslt = new File(mojo.getTransformDirectory(), xsltFile);
		if (xslt.exists()) {

			styleSource = new StreamSource(xslt);

		} else {
			logInfo("Loading predefined xslt file:" + xsltFile);
			transformerFactory.setURIResolver(new URIResolver() {

				@Override
				public Source resolve(String href, String base)
						throws TransformerException {

					return new StreamSource(this.getClass()
							.getResourceAsStream(href));
				}
			});
			styleSource = new StreamSource(this.getClass().getResourceAsStream(
					xsltFile));
		}

		try {
			return transformerFactory.newTransformer(styleSource);
		} catch (TransformerConfigurationException e) {
			throw new RuntimeException(e);
		}
	}

	private void writePackageDocumentation(File outputDirectory,
			String description) {

		try {

			File packageDescription = new File(outputDirectory, "package.html");
			PrintStream out = new PrintStream(new FileOutputStream(
					packageDescription));
			out.println("<html>");
			out.println("<head><title/></head>");
			out.println("<body>" + description + "</body>");
			out.println("</html>");
			out.close();

		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	private void generateCodeFile(Map<String, String> parameters,
			File outputFile, Transformer transformer) {

		try {

			if (parameters != null) {
				for (Map.Entry<String, String> entry : parameters.entrySet()) {
					transformer.setParameter(entry.getKey(), entry.getValue());
				}
			}

			if (!outputFile.getParentFile().exists()) {
				outputFile.getParentFile().mkdirs();
			}

			Source source = new JAXBSource(jaxbContext,
					new ObjectFactory().createFix(dictionary));
			FileOutputStream fos = new FileOutputStream(outputFile);
			BufferedOutputStream bos = new BufferedOutputStream(fos);
			try {
				StreamResult result = new StreamResult(bos);
				transformer.transform(source, result);
			} finally {
				try {
					bos.close();
				} catch (IOException ioe) {
					logError("error closing " + outputFile, ioe);
				}
			}

		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private Map<String, String> createParameters() {

		Map<String, String> parameters = new HashMap<String, String>();

		parameters.put(XSLPARAM_SERIAL_UID, SERIAL_UID_STR);
		parameters.put("messagePackage", directives.getMessages().getPackage());
		parameters.put("fieldPackage", directives.getFields().getPackage());
		parameters.put("componentPackage", directives.getComponents()
				.getPackage());

		return parameters;
	}

	/*
	 * Generate the Message and Field related source code.
	 */
	public void generate() {

		generateFieldClasses();
		generateComponentClasses();
		generateMessageSubclasses();
		if (directives.getTransport() != null) {
			generateMessageHeaderClass();
			generateMessageTrailerClass();
		}
		generateDictionaryServiceClass();
	}

	public Log getLog() {

		return mojo.getLog();
	}

	private void logInfo(String msg) {

		getLog().info(msg);
	}

	private void logDebug(String msg) {

		getLog().debug(msg);
	}

	private void logError(String msg, Throwable e) {

		getLog().error(msg, e);
	}
}
