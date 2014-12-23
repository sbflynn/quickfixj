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
	<xsl:param name="baseClass" select="'quickfix.AbstractMessage'" />
	<xsl:param name="subpackage" />

	<!-- ********************************************************************* -->
	<!-- Main message generation template. This template generates a default -->
	<!-- constructor and, if any fields are required, generates a constructor -->
	<!-- taking those fields as arguments. -->
	<!-- ********************************************************************* -->
	<xsl:template match="fix:fix">
		<xsl:apply-templates select="fix:messages/fix:message[@name=$itemName]" />
	</xsl:template>

	<xsl:template match="fix:message">
		<xsl:variable name="package" select="concat($messagePackage,$subpackage)" />

		<xsl:value-of select="$NEW_LINE" />
		<xsl:value-of select="concat('package ', $package, ';')" />

		<xsl:value-of select="$NEW_LINE" />
		<xsl:value-of select="$NEW_LINE" />
		<xsl:text>@javax.annotation.Generated("org.quickfixj.codegenerator.GenerateMojo")</xsl:text>
		<xsl:value-of select="$NEW_LINE" />
		<xsl:text>@org.quickfixj.annotation.Message(</xsl:text>
		<xsl:value-of select="concat('name=&#34;', @name,'&#34;, ')" />
		<xsl:value-of select="concat('msgtype=&#34;', @msgtype,'&#34;, ')" />
		<xsl:value-of select="concat('msgcat=&#34;', @msgcat,'&#34;')" />
		<xsl:text>)</xsl:text>
		<xsl:value-of select="$NEW_LINE" />
		<xsl:value-of select="concat('public class ', @name, ' extends ', $baseClass)" />
		<xsl:if test="fix:component">
			<xsl:text> implements </xsl:text>
			<xsl:for-each select="fix:component">
				<xsl:if test="position() > 1">
					<xsl:text>, </xsl:text>
				</xsl:if>
				<xsl:value-of select="concat($componentPackage, '.', @name)" />
			</xsl:for-each>
		</xsl:if>
		<xsl:text> {</xsl:text>

		<xsl:call-template name="emit-serial-version" />

		<xsl:value-of select="$NEW_LINE" />
		<xsl:value-of select="$NEW_LINE_INDENT" />
		<xsl:value-of
			select="concat('public static final String MSGTYPE = &#34;', @msgtype, '&#34;;')" />

		<xsl:value-of select="$NEW_LINE" />
		<xsl:value-of select="$NEW_LINE_INDENT" />
		<xsl:value-of select="concat('public ', @name, '() {')" />
		<xsl:value-of select="$NEW_LINE_INDENT2" />
		<xsl:text>super();</xsl:text>
		<xsl:value-of select="$NEW_LINE_INDENT" />
		<xsl:text>}</xsl:text>

		<xsl:if test="count(fix:field[@required='Y']) > 0">
			<xsl:value-of select="$NEW_LINE" />
			<xsl:value-of select="$NEW_LINE_INDENT" />
			<xsl:value-of select="concat('public ', @name, '(')" />
			<xsl:for-each select="fix:field[@required='Y']">
				<xsl:variable name="varname"
					select="concat(translate(substring(@name, 1, 1), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), substring(@name, 2, string-length(@name)-1))" />
				<xsl:if test="position() > 1">
					<xsl:text>,</xsl:text>
				</xsl:if>
				<xsl:value-of select="$NEW_LINE_INDENT3" />
				<xsl:value-of select="concat($fieldPackage, '.', @name, ' ', $varname)" />
			</xsl:for-each>
			<xsl:value-of select="$NEW_LINE_INDENT2" />
			<xsl:text>) {</xsl:text>
			<xsl:value-of select="$NEW_LINE_INDENT2" />
			<xsl:text>super();</xsl:text>
			<xsl:for-each select="fix:field[@required='Y']">
				<xsl:variable name="varname"
					select="concat(translate(substring(@name, 1, 1), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), substring(@name, 2, string-length(@name)-1))" />
				<xsl:value-of select="$NEW_LINE_INDENT2" />
				<xsl:value-of select="concat('setField(', $varname, ');')" />
			</xsl:for-each>
			<xsl:value-of select="$NEW_LINE_INDENT" />
			<xsl:text>}</xsl:text>
		</xsl:if>

		<xsl:value-of select="$NEW_LINE" />
		<xsl:value-of select="$NEW_LINE_INDENT" />
		<xsl:text>@Override</xsl:text>
		<xsl:value-of select="$NEW_LINE_INDENT" />
		<xsl:text>public String getMsgType() {</xsl:text>
		<xsl:value-of select="$NEW_LINE_INDENT2" />
		<xsl:text>return MSGTYPE;</xsl:text>
		<xsl:value-of select="$NEW_LINE_INDENT" />
		<xsl:text>}</xsl:text>

		<xsl:apply-templates select="fix:field" mode="field-accessors-concrete" />
		<xsl:apply-templates select="fix:component" mode="field-accessors-concrete" />

		<xsl:apply-templates select="fix:group" mode="emit-groupfield-accessor" />
		<xsl:apply-templates select="fix:group" mode="emit-groupfield-inner-class" />
		<xsl:apply-templates select="fix:group" mode="emit-group-inner-class" />

		<xsl:value-of select="$NEW_LINE" />
		<xsl:text>}</xsl:text>

	</xsl:template>

	<xsl:template mode="emit-groupfield-accessor" match="fix:group">
		<xsl:call-template name="emit-field-accessor-concrete">
			<xsl:with-param name="fieldType" select="@name" />
		</xsl:call-template>
	</xsl:template>

</xsl:stylesheet>
