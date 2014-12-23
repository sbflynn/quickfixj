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

import java.io.File;
import java.io.IOException;
import java.net.URL;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.helpers.DefaultValidationEventHandler;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.FileUtils;
import org.quickfixj.xml.dictionary.DataDictionaryConfig;
import org.quickfixj.xml.dictionary.Engine;
import org.quickfixj.xml.dictionary.ObjectFactory;

/**
 * A mojo that uses the quickfix code generator to generate Java source files
 * from a QuickFIX Dictionary.
 *
 * @description QuickFIX/J code generation plugin
 * @author Claudio Bantaloukas <rockdreamer@gmail.com>
 */
@Mojo(name = "generate", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class GenerateMojo extends AbstractMojo {

    /**
     * The dictionary file to use for mapping messages to java.
     */
    @Parameter(required = true)
    private String resource;

    /**
     * The source directory containing *.xsd files.
     */
    @Parameter(defaultValue = "${basedir}/src/main/resources")
    private File resourceDirectory;

    /**
     * The source directory containing *.xsd files.
     */
    @Parameter(
            defaultValue = "${basedir}/src/resources/quickfixj/codegenerator")
    private File transformDirectory;

    /**
     * The directory to output the generated sources to.
     */
    @Parameter(defaultValue = "${project.build.directory}/generated-sources/")
    private File outputDirectory;

    /**
     * The Maven project to act upon.
     */
    @Parameter(property = "project", required = true)
    private MavenProject project;

    private Schema schema;

    /**
     * {@link MessageCodeGenerator} instance used for code generation.
     */
    private MessageCodeGenerator generator;

    /**
     * {@inheritDoc}
     *
     * @see org.apache.maven.plugin.AbstractMojo#execute()
     */
    @Override
    public void execute() throws MojoExecutionException {

        if ("pom".equals(project.getPackaging())) {
            getLog().info("Skipping pom execution");
            return;
        }

        if (!outputDirectory.exists()) {
            FileUtils.mkdir(outputDirectory.getAbsolutePath());
        }

        try {

            SchemaFactory schemaFactory = SchemaFactory
                    .newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);

            schema = schemaFactory.newSchema(getClass().getResource(
                    "/META-INF/xsd/fix-dictionary.xsd"));

            Engine directives = getDirectives();
            DataDictionaryConfig dictionary = getDictionary(directives);

            generator = new MessageCodeGenerator(this,
                    directives.getMessageFactory(), dictionary);

            generator.generate();

        } catch (Throwable t) {
            throw new MojoExecutionException(
                    "QuickFIX code generator execution failed", t);
        }

        if (project != null) {
            project.addCompileSourceRoot(outputDirectory.getAbsolutePath());
        }
    }

    /**
     * Get the resource property.
     *
     * @return Returns the resource.
     * @since 2.0
     */
    public String getResource() {

        return resource;
    }

    /**
     * Set the resource property.
     *
     * @param resource The resource to set.
     * @since 2.0
     */
    public void setResource(String resource) {

        this.resource = resource;
    }

    /**
     * Get the resourceDirectory property.
     *
     * @return Returns the resourceDirectory.
     * @since 2.0
     */
    public File getResourceDirectory() {

        return resourceDirectory;
    }

    /**
     * Set the resourceDirectory property.
     *
     * @param resourceDirectory The resourceDirectory to set.
     * @since 2.0
     */
    public void setResourceDirectory(File resourceDirectory) {

        this.resourceDirectory = resourceDirectory;
    }

    /**
     * Get the transformDirectory property.
     *
     * @return Returns the transformDirectory.
     * @since 2.0
     */
    public File getTransformDirectory() {

        return transformDirectory;
    }

    /**
     * Set the transformDirectory property.
     *
     * @param transformDirectory The transformDirectory to set.
     * @since 2.0
     */
    public void setTransformDirectory(File transformDirectory) {

        this.transformDirectory = transformDirectory;
    }

    /**
     * Returns the destination directory to used during code generation.
     *
     * @return the destination directory to used during code generation.
     */
    public File getOutputDirectory() {

        return outputDirectory;
    }

    /**
     * Sets the destination directory to used during code generation.
     *
     * @param outputDirectory the destination directory to used during code
     *        generation.
     */
    public void setOutputDirectory(final File outputDirectory) {

        this.outputDirectory = outputDirectory;
    }

    /**
     * Returns the {@link MavenProject} instance for which code generation
     * should be executed.
     *
     * @return the {@link MavenProject} instance for which code generation
     *         should be executed.
     */
    public MavenProject getProject() {

        return project;
    }

    /**
     * Sets the {@link MavenProject} instance for which code generation should
     * be executed.
     *
     * @param project the {@link MavenProject} instance for which code
     *        generation should be executed.
     */
    public void setProject(MavenProject project) {

        this.project = project;
    }

    /**
     * Returns the directory containing schemas for code generation.
     *
     * @return the directory containing schemas for code generation.
     */
    public File getSchemaDirectory() {

        return transformDirectory;
    }

    /**
     * Sets the directory containing schemas for code generation.
     *
     * @param schemaDirectory the directory containing schemas for code
     *        generation.
     */
    public void setSchemaDirectory(File schemaDirectory) {

        this.transformDirectory = schemaDirectory;
    }

    private DataDictionaryConfig getDictionary(Engine directives)
            throws IOException, JAXBException {

        File buildFile;

        buildFile = new File(resourceDirectory, "META-INF/quickfix");
        buildFile = new File(buildFile, getResource());

        URL url = new URL(buildFile.toURI().toURL(), directives.getDictionary()
                .get(0).getLocation());
        getLog().info("Data dictionary url : " + url);

        JAXBContext jc = JAXBContext.newInstance(ObjectFactory.class);

        Unmarshaller unmarshaller = jc.createUnmarshaller();

        unmarshaller.setSchema(schema);
        unmarshaller.setEventHandler(new DefaultValidationEventHandler());

        return unmarshaller.unmarshal(new StreamSource(url.openStream()),
                DataDictionaryConfig.class).getValue();
    }

    private Engine getDirectives() throws JAXBException {

        if (getProject().getProperties().containsKey("FIXENGINE")) {
            // use directive created by build engine mojo
            getLog().info("Found built engine");
            return (Engine) getProject().getProperties().get("FIXENGINE");
        }

        File buildFile;

        buildFile = new File(resourceDirectory, "META-INF/quickfix");
        buildFile = new File(buildFile, getResource());

        getLog().info("Factory url : " + buildFile);

        if (!buildFile.exists()) {
            throw new RuntimeException("Build file not found");
        }

        JAXBContext jc = JAXBContext.newInstance(ObjectFactory.class);

        Unmarshaller unmarshaller = jc.createUnmarshaller();

        unmarshaller.setSchema(schema);
        unmarshaller.setEventHandler(new DefaultValidationEventHandler());

        return unmarshaller
                .unmarshal(new StreamSource(buildFile), Engine.class)
                .getValue();
    }
}
