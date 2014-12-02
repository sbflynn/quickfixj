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

	<xsl:template match="fix:fix">

		<xsl:value-of select="$NEW_LINE" />
		<xsl:value-of select="concat('package ', $messagePackage,';')" />

		<xsl:value-of select="$NEW_LINE" />
		<xsl:value-of select="$NEW_LINE" />
		<xsl:text>@javax.annotation.Generated("org.quickfixj.codegenerator.GenerateMojo")</xsl:text>
		<xsl:value-of select="$NEW_LINE" />
		<xsl:text>public final class MessageHeader extends quickfix.Message.Header</xsl:text>
		<xsl:if test="fix:header/fix:component">
			<xsl:text> implements </xsl:text>
			<xsl:for-each select="fix:header/fix:component">
				<xsl:if test="position() > 1">
					<xsl:text>, </xsl:text>
				</xsl:if>
				<xsl:value-of select="concat($componentPackage, '.', @name)" />
			</xsl:for-each>
		</xsl:if>
		<xsl:text> {</xsl:text>

		<xsl:call-template name="emit-serial-version" />

		<xsl:apply-templates select="fix:header/fix:field" mode="field-accessors-concrete" />
		<xsl:apply-templates select="fix:header/fix:component" mode="field-accessors-concrete" />
		<xsl:apply-templates select="fix:header/fix:group" mode="emit-groupfield-accessor" />
		<xsl:apply-templates select="fix:header/fix:group" mode="emit-groupfield-inner-class" />
		<xsl:apply-templates select="fix:header/fix:group" mode="emit-group-inner-class" />

		<xsl:value-of select="$NEW_LINE" />
		<xsl:text>}</xsl:text>
	</xsl:template>

</xsl:stylesheet>
