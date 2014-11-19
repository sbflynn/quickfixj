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

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.FileUtils;

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
    @Parameter(
            defaultValue = "${basedir}/src/main/resources/META-INF/dictionary.xml")
    private File dictFile;

    /**
     * The source directory containing *.xsd files.
     */
    @Parameter(
            defaultValue = "${basedir}/src/resources/quickfixj/codegenerator")
    private File schemaDirectory;

    /**
     * The directory to output the generated sources to.
     */
    @Parameter(defaultValue = "${project.build.directory}/generated-sources/")
    private File outputDirectory;

    /**
     * Enable BigDecimal representation.
     */
    @Parameter(defaultValue = "false")
    private boolean decimal;

    /**
     * Enable BigDecimal representation.
     */
    @Parameter(defaultValue = "true")
    private boolean validate;

    /**
     * Enable orderedFields.
     */
    @Parameter(defaultValue = "false")
    private boolean orderedFields;

    /**
     * The package for the generated source.
     */
    @Parameter
    private String packaging;

    /**
     * The base field class to use.
     */
    @Parameter(defaultValue = "quickfix.field")
    private String fieldPackage = "quickfix.field";

    /**
     * The Maven project to act upon.
     */
    @Parameter(property = "project", required = true)
    private MavenProject project;

    /**
     * {@link MessageCodeGenerator} instance used for code generation.
     */
    private MavenMessageCodeGenerator generator;

    /**
     * {@inheritDoc}
     *
     * @see org.apache.maven.plugin.AbstractMojo#execute()
     */
    @Override
    public void execute() throws MojoExecutionException {

        if (!outputDirectory.exists()) {
            FileUtils.mkdir(outputDirectory.getAbsolutePath());
        }

        try {
            generator = new MavenMessageCodeGenerator();
            if (getLog().isInfoEnabled()) {
                getLog().info(
                        "Successfully created an instance of the QuickFIX source generator");
            }
            generator.setLog(getLog());

            MessageCodeGenerator.Task task = new MessageCodeGenerator.Task();
            if (getLog().isInfoEnabled()) {
                getLog().info("Initialising code generator task");
            }

            if (dictFile != null && dictFile.exists()) {
                task.setSpecification(dictFile);
            } else {
                getLog().error("Cannot find file " + dictFile);
            }

            log("Processing " + dictFile);

            task.setName(dictFile.getName());
            task.setTransformDirectory(schemaDirectory);
            task.setMessagePackage(packaging);
            task.setOutputBaseDirectory(outputDirectory);
            task.setFieldPackage(fieldPackage);
            task.setOverwrite(true);
            task.setOrderedFields(orderedFields);
            task.setDecimalGenerated(decimal);
            task.setValidate(validate);

            generator.generate(task);
        } catch (Exception e) {
            throw new MojoExecutionException(
                    "QuickFIX code generator execution failed", e);
        } catch (Throwable t) {
            throw new MojoExecutionException(
                    "QuickFIX code generator execution failed", t);
        }

        if (project != null) {
            project.addCompileSourceRoot(outputDirectory.getAbsolutePath());
        }
    }

    /**
     * Logs a message to the logger.
     *
     * @param msg The message ot be logged.
     */
    private void log(final String msg) {

        getLog().info(msg);
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
     * Returns the default package to be used during code generation.
     *
     * @return the default package to be used during code generation.
     */
    public String getPackaging() {

        return packaging;
    }

    /**
     * Sets the default package to be used during code generation.
     *
     * @param packaging the default package to be used during code generation.
     */
    public void setPackaging(final String packaging) {

        this.packaging = packaging;
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
     * Returns the dictionary file for which code generation should be executed.
     *
     * @return the dictionary file for which code generation should be executed.
     */
    public File getDictFile() {

        return dictFile;
    }

    /**
     * Sets the dictionary file for which code generation should be executed.
     *
     * @param dictFile the dictionary file for which code generation should be
     *        executed.
     */
    public void setDictFile(File dictFile) {

        this.dictFile = dictFile;
    }

    /**
     * Returns the directory containing schemas for code generation.
     *
     * @return the directory containing schemas for code generation.
     */
    public File getSchemaDirectory() {

        return schemaDirectory;
    }

    /**
     * Sets the directory containing schemas for code generation.
     *
     * @param schemaDirectory the directory containing schemas for code
     *        generation.
     */
    public void setSchemaDirectory(File schemaDirectory) {

        this.schemaDirectory = schemaDirectory;
    }

    /**
     * Get the validate property.
     *
     * @return Returns the validate.
     * @since 2.0
     */
    public boolean isValidate() {

        return validate;
    }

    /**
     * Set the validate property.
     *
     * @param validate The validate to set.
     * @since 2.0
     */
    public void setValidate(boolean validate) {

        this.validate = validate;
    }
}
