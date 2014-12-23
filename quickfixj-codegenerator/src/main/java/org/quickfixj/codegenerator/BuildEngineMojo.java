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

import javax.xml.bind.JAXB;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.FileUtils;
import org.quickfixj.FIXApplication;
import org.quickfixj.FIXBeginString;
import org.quickfixj.xml.dictionary.BuildDirective;
import org.quickfixj.xml.dictionary.Engine;
import org.quickfixj.xml.dictionary.Engine.ApplicationDictionary;
import org.quickfixj.xml.dictionary.Engine.ApplicationDictionary.Transport;
import org.quickfixj.xml.dictionary.Engine.Dictionary;
import org.quickfixj.xml.dictionary.Engine.MessageFactory;
import org.quickfixj.xml.dictionary.Engine.TransportDictionary;
import org.quickfixj.xml.dictionary.EnumDecimalType;

/**
 * A mojo that generates a default engine directive.
 *
 * @author stephen.flynn@jftechnology.com
 * @since 2.0
 */
@Mojo(name = "build-engine", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class BuildEngineMojo extends AbstractMojo {

    /**
     * The file of the resource to create.
     */
    @Parameter(required = true)
    private String resource;

    /**
     * The dictionary file to use for mapping messages to java.
     */
    @Parameter(required = true)
    private String namespace;

    /**
     * The dictionary file to use for mapping messages to java.
     */
    @Parameter(required = true)
    private String dictionaryId;

    /**
     * The dictionary file to use for mapping messages to java.
     */
    @Parameter(required = true)
    private String dictionaryLocation;

    /**
     * The dictionary file to use for mapping messages to java.
     */
    @Parameter(required = false)
    private String beginString;

    /**
     * The dictionary file to use for mapping messages to java.
     */
    @Parameter(required = false)
    private String application;

    /**
     * The dictionary file to use for mapping messages to java.
     */
    @Parameter(required = false)
    private String packageRoot;

    /**
     * The dictionary file to use for mapping messages to java.
     */
    @Parameter(required = true)
    private String decimal;

    /**
     * The dictionary file to use for mapping messages to java.
     */
    @Parameter(required = false)
    private String[] applicationTransports;

    /**
     * The directory to output the generated sources to.
     */
    @Parameter(
            defaultValue = "${project.build.directory}/generated-resources/META-INF/quickfix")
    private File outputDirectory;

    /**
     * The Maven project to act upon.
     */
    @Parameter(property = "project", required = true)
    private MavenProject project;

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

            Engine engine = new Engine();

            Dictionary dictionary = new Dictionary();
            dictionary.setId(this.dictionaryId);
            dictionary.setLocation(this.dictionaryLocation);
            engine.getDictionary().add(dictionary);

            if (beginString != null) {
                engine.setTransportDictionary(new TransportDictionary());
                engine.getTransportDictionary().setNamespace(namespace);
                engine.getTransportDictionary().setDictionary(dictionaryId);
                engine.getTransportDictionary().setBeginString(
                        FIXBeginString.parse(beginString));
            }

            if (application != null) {
                engine.setApplicationDictionary(new ApplicationDictionary());
                engine.getApplicationDictionary().setNamespace(namespace);
                engine.getApplicationDictionary().setDictionary(dictionaryId);
                engine.getApplicationDictionary().setApplication(
                        FIXApplication.parse(application));

                // add common fixt transport
                Transport transport = new Transport();
                transport.setNamespace("org.quickfixj.messages.fixt");
                transport.setBeginString(FIXBeginString.FIXT11);
                engine.getApplicationDictionary().getTransport().add(transport);

                if (applicationTransports != null) {
                    for (String applicationTransport : applicationTransports) {
                        transport = new Transport();
                        transport.setNamespace(namespace);
                        transport.setBeginString(FIXBeginString
                                .parse(applicationTransport));
                        engine.getApplicationDictionary().getTransport()
                                .add(transport);
                    }
                }
            }

            engine.setMessageFactory(new MessageFactory());

            if (application != null) {
                engine.getMessageFactory().setApplication(
                        FIXApplication.parse(application));
            }
            if (beginString != null) {
                engine.getMessageFactory().setTransport(
                        FIXBeginString.parse(beginString));
            }
            engine.getMessageFactory().setNamespace(namespace);
            engine.getMessageFactory().setDecimal(
                    EnumDecimalType.valueOf(decimal));
            engine.getMessageFactory().setDictionary(dictionaryId);

            engine.getMessageFactory().setMessages(
                    createBuildDirective(packageRoot));
            engine.getMessageFactory().setFields(
                    createBuildDirective(packageRoot + ".field"));
            engine.getMessageFactory().setComponents(
                    createBuildDirective(packageRoot + ".component"));

            JAXB.marshal(engine, new File(outputDirectory, resource));

            if (project != null) {
                project.getProperties().put("FIXENGINE", engine);
            }

        } catch (Throwable t) {
            throw new MojoExecutionException(
                    "QuickFIX code generator execution failed", t);
        }
    }

    private BuildDirective createBuildDirective(String packageName) {

        BuildDirective directive = new BuildDirective();
        directive.setBuild(true);
        directive.setPackage(packageName);

        return directive;
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
     * Get the namespace property.
     *
     * @return Returns the namespace.
     * @since 2.0
     */
    public String getNamespace() {

        return namespace;
    }

    /**
     * Set the namespace property.
     *
     * @param namespace The namespace to set.
     * @since 2.0
     */
    public void setNamespace(String namespace) {

        this.namespace = namespace;
    }

    /**
     * Get the dictionaryId property.
     *
     * @return Returns the dictionaryId.
     * @since 2.0
     */
    public String getDictionaryId() {

        return dictionaryId;
    }

    /**
     * Set the dictionaryId property.
     *
     * @param dictionaryId The dictionaryId to set.
     * @since 2.0
     */
    public void setDictionaryId(String dictionaryId) {

        this.dictionaryId = dictionaryId;
    }

    /**
     * Get the dictionaryLocation property.
     *
     * @return Returns the dictionaryLocation.
     * @since 2.0
     */
    public String getDictionaryLocation() {

        return dictionaryLocation;
    }

    /**
     * Set the dictionaryLocation property.
     *
     * @param dictionaryLocation The dictionaryLocation to set.
     * @since 2.0
     */
    public void setDictionaryLocation(String dictionaryLocation) {

        this.dictionaryLocation = dictionaryLocation;
    }

    /**
     * Get the beginString property.
     *
     * @return Returns the beginString.
     * @since 2.0
     */
    public String getBeginString() {

        return beginString;
    }

    /**
     * Set the beginString property.
     *
     * @param beginString The beginString to set.
     * @since 2.0
     */
    public void setBeginString(String beginString) {

        this.beginString = beginString;
    }

    /**
     * Get the application property.
     *
     * @return Returns the application.
     * @since 2.0
     */
    public String getApplication() {

        return application;
    }

    /**
     * Set the application property.
     *
     * @param application The application to set.
     * @since 2.0
     */
    public void setApplication(String application) {

        this.application = application;
    }

    /**
     * Get the packageRoot property.
     *
     * @return Returns the packageRoot.
     * @since 2.0
     */
    public String getPackageRoot() {

        return packageRoot;
    }

    /**
     * Set the packageRoot property.
     *
     * @param packageRoot The packageRoot to set.
     * @since 2.0
     */
    public void setPackageRoot(String packageRoot) {

        this.packageRoot = packageRoot;
    }

    /**
     * Get the decimal property.
     *
     * @return Returns the decimal.
     * @since 2.0
     */
    public String getDecimal() {

        return decimal;
    }

    /**
     * Set the decimal property.
     *
     * @param decimal The decimal to set.
     * @since 2.0
     */
    public void setDecimal(String decimal) {

        this.decimal = decimal;
    }

    /**
     * Get the applicationTransports property.
     *
     * @return Returns the applicationTransports.
     * @since 2.0
     */
    public String[] getApplicationTransports() {

        return applicationTransports;
    }

    /**
     * Set the applicationTransports property.
     *
     * @param applicationTransports The applicationTransports to set.
     * @since 2.0
     */
    public void setApplicationTransports(String[] applicationTransports) {

        this.applicationTransports = applicationTransports;
    }
}
