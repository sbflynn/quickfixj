<!-- ***************************************************************************** -->
<!-- Copyright (c) 2001-2004 quickfixengine.org All rights reserved. -->

<!-- This file is part of the QuickFIX FIX Engine -->

<!-- This file may be distributed under the terms of the quickfixengine.org license 
	as defined by quickfixengine.org and appearing in the file LICENSE included in the 
	packaging of this file. -->

<!-- This file is provided AS IS with NO WARRANTY OF ANY KIND, INCLUDING -->
<!-- THE WARRANTY OF DESIGN, MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE. -->

<!-- See http://www.quickfixengine.org/LICENSE for licensing information. -->

<!-- Contact ask@quickfixengine.org if any conditions of this licensing are not clear 
	to you. -->
<!-- ***************************************************************************** -->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	version="1.0" xmlns:fix="http://quickfixj.org/xml/dictionary"
>

	<xsl:import href="parent.xsl" />

	<xsl:output method="text" encoding="UTF-8" />

	<xsl:param name="orderedFields" />
	<xsl:param name="itemName" />
	<xsl:param name="baseClass" />

	<!-- ********************************************************************* -->
	<!-- Main message generation template. This template generates a default -->
	<!-- constructor and, if any fields are required, generates a constructor -->
	<!-- taking those fields as arguments. -->
	<!-- ********************************************************************* -->
	<xsl:template match="fix:fix">
		<xsl:apply-templates select="fix:components/fix:component[@name=$itemName]" />
	</xsl:template>

	<xsl:template match="fix:component">

		<xsl:value-of select="$NEW_LINE" />
		<xsl:value-of select="concat('package ', $componentPackage, ';')" />

		<xsl:value-of select="$NEW_LINE" />
		<xsl:value-of select="$NEW_LINE" />
		<xsl:text>@javax.annotation.Generated("org.quickfixj.codegenerator.GenerateMojo")</xsl:text>
		<xsl:value-of select="$NEW_LINE" />
		<xsl:text>@org.quickfixj.annotation.Component(</xsl:text>
		<xsl:value-of select="concat('name=&#34;', @name,'&#34;')" />
		<xsl:text>)</xsl:text>
		<xsl:value-of select="$NEW_LINE" />
		<xsl:value-of
			select="concat('public interface ', @name, ' extends org.quickfixj.FIXComponent') " />
		<xsl:if test="fix:component">
			<xsl:for-each select="fix:component">
				<xsl:text>, </xsl:text>
				<xsl:value-of select="concat($componentPackage, '.', @name)" />
			</xsl:for-each>
		</xsl:if>
		<xsl:text> {</xsl:text>

		<xsl:value-of select="$NEW_LINE" />
		<xsl:value-of select="$NEW_LINE_INDENT" />
		<xsl:text>public static final java.util.List&lt;Integer&gt; COMPONENT_FIELDS = java.util.Collections.unmodifiableList(java.util.Arrays.asList(new Integer[] {</xsl:text>
		<xsl:apply-templates select="fix:field|fix:group|fix:component"
			mode="component-field-numbers" />
		<xsl:text>}));</xsl:text>

		<xsl:apply-templates select="fix:field" mode="field-accessors-abstract" />

		<xsl:apply-templates select="fix:group" mode="emit-groupfield-accessor" />
		<xsl:apply-templates select="fix:group" mode="emit-groupfield-inner-class" />
		<xsl:apply-templates select="fix:group" mode="emit-group-inner-class" />

		<xsl:value-of select="$NEW_LINE" />
		<xsl:text>}</xsl:text>

	</xsl:template>

	<!-- ********************************************************************* -->
	<!-- FIX repeating group generation template. -->
	<!-- - Find first field (for constructor) -->
	<!-- - Find all fields and their order (for constructor) -->
	<!-- - Generate field accessor methods -->
	<!-- ********************************************************************* -->
	<xsl:template mode="field-accessors-concrete" match="fix:group">
		<xsl:call-template name="emit-field-accessor-concrete">
			<xsl:with-param name="fieldType"
				select="concat($componentPackage,'.', ../@name, '.', @name)" />
		</xsl:call-template>
	</xsl:template>

	<xsl:template mode="emit-groupfield-accessor" match="fix:group">
		<xsl:call-template name="emit-field-accessor-abstract">
			<xsl:with-param name="fieldType"
				select="concat($componentPackage,'.', ../@name, '.', @name)" />
		</xsl:call-template>
	</xsl:template>

	<!-- Find the component numbers and order -->
	<xsl:template mode="component-field-numbers" match="fix:field">
		<xsl:variable name="name" select="@name" />
		<xsl:value-of select="/fix:fix/fix:fields/fix:field[@name=$name]/@number" />
		<xsl:text>,</xsl:text>
	</xsl:template>

	<xsl:template mode="component-field-numbers" match="fix:group">
		<xsl:variable name="name" select="@name" />
		<xsl:value-of select="/fix:fix/fix:fields/fix:field[@name=$name]/@number" />
		<xsl:text>,</xsl:text>
	</xsl:template>

	<xsl:template mode="component-field-numbers" match="fix:component">
		<xsl:variable name="name" select="@name" />
		<xsl:apply-templates select="/fix:fix/fix:components/fix:component[@name=$name]/*"
			mode="component-field-numbers" />
	</xsl:template>

</xsl:stylesheet>
