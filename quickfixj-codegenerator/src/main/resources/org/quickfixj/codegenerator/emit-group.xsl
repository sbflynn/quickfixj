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

	<xsl:template mode="emit-group-inner-class" match="fix:group">

		<xsl:variable name="groupFieldName" select="@name" />

		<xsl:value-of select="$NEW_LINE" />
		<xsl:value-of select="$NEW_LINE_INDENT" />
		<xsl:value-of
			select="concat('public static class ', @name, 'Group extends quickfix.Group')" />
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
		<xsl:value-of select="$NEW_LINE_INDENT2" />
		<xsl:value-of select="concat('public ', @name, 'Group() {')" />

		<xsl:value-of select="$NEW_LINE_INDENT3" />
		<xsl:text>super(</xsl:text>
		<xsl:value-of select="$groupFieldName" />
		<xsl:text>.TAG, </xsl:text>
		<xsl:value-of select="$groupFieldName" />
		<xsl:text>.DELIMITER, </xsl:text>
		<xsl:text>new int[] {</xsl:text>
		<xsl:apply-templates select="fix:field|fix:component|fix:group"
			mode="emit-group-field-numbers" />
		<xsl:text>0 });</xsl:text>
		<xsl:value-of select="$NEW_LINE_INDENT2" />
		<xsl:text>}</xsl:text>

		<xsl:apply-templates select="fix:field" mode="field-accessors-concrete" />
		<xsl:apply-templates select="fix:component" mode="field-accessors-concrete" />

		<xsl:apply-templates select="fix:group"
			mode="emit-nested-groupfield-accessor" />
		<xsl:apply-templates select="fix:group" mode="emit-groupfield-inner-class" />
		<xsl:apply-templates select="fix:group" mode="emit-group-inner-class" />

		<xsl:value-of select="$NEW_LINE_INDENT" />
		<xsl:text>}</xsl:text>
	</xsl:template>

	<xsl:template mode="emit-groupfield-inner-class" match="fix:group">

		<xsl:variable name="groupFieldName" select="@name" />
		<xsl:variable name="tag"
			select="/fix:fix/fix:fields/fix:field[@name=$groupFieldName]/@number" />

		<xsl:value-of select="$NEW_LINE" />
		<xsl:value-of select="$NEW_LINE_INDENT" />
		<xsl:value-of
			select="concat('public static class ', @name, ' extends org.quickfixj.field.GroupField&lt;',@name,'Group&gt; {')" />

		<xsl:call-template name="emit-serial-version" />

		<xsl:value-of select="$NEW_LINE" />
		<xsl:value-of select="$NEW_LINE_INDENT2" />
		<xsl:value-of select="concat('public static final int TAG = ', $tag[1], ';')" />

		<xsl:call-template name="emit-group-delimiter" />

		<xsl:value-of select="$NEW_LINE" />
		<xsl:value-of select="$NEW_LINE_INDENT2" />
		<xsl:text>public static final java.util.List&lt;Integer&gt; GROUP_FIELDS = java.util.Collections.unmodifiableList(java.util.Arrays.asList(new Integer[] {</xsl:text>
		<xsl:apply-templates select="fix:field|fix:component|fix:group"
			mode="emit-group-field-numbers" />
		<xsl:text>}));</xsl:text>

		<xsl:value-of select="$NEW_LINE" />
		<xsl:value-of select="$NEW_LINE_INDENT2" />
		<xsl:value-of select="concat('public ', @name, '(CharSequence value) {')" />
		<xsl:value-of select="$NEW_LINE_INDENT3" />
		<xsl:text>super(value);</xsl:text>
		<xsl:value-of select="$NEW_LINE_INDENT2" />
		<xsl:text>}</xsl:text>

		<xsl:value-of select="$NEW_LINE" />
		<xsl:value-of select="$NEW_LINE_INDENT2" />
		<xsl:text>@Override</xsl:text>
		<xsl:value-of select="$NEW_LINE_INDENT2" />
		<xsl:text>public int getTag() {</xsl:text>
		<xsl:value-of select="$NEW_LINE_INDENT3" />
		<xsl:text>return TAG;</xsl:text>
		<xsl:value-of select="$NEW_LINE_INDENT2" />
		<xsl:text>}</xsl:text>

		<xsl:value-of select="$NEW_LINE" />
		<xsl:value-of select="$NEW_LINE_INDENT2" />
		<xsl:text>@Override</xsl:text>
		<xsl:value-of select="$NEW_LINE_INDENT2" />
		<xsl:text>public int getDelimiterField() {</xsl:text>
		<xsl:value-of select="$NEW_LINE_INDENT2" />
		<xsl:text>return DELIMITER;</xsl:text>
		<xsl:value-of select="$NEW_LINE_INDENT2" />
		<xsl:text>}</xsl:text>
		<xsl:value-of select="$NEW_LINE_INDENT" />
		<xsl:text>}</xsl:text>

	</xsl:template>

	<xsl:template mode="emit-nested-groupfield-accessor" match="fix:group">
		<xsl:call-template name="emit-field-accessor-concrete">
			<xsl:with-param name="fieldType" select="@name" />
		</xsl:call-template>
	</xsl:template>

	<!-- Find the field numbers and order -->
	<xsl:template match="fix:field|fix:group" mode="emit-group-field-numbers">
		<xsl:variable name="name" select="@name" />
		<xsl:value-of select="/fix:fix/fix:fields/fix:field[@name=$name]/@number" />
		<xsl:text>,</xsl:text>
	</xsl:template>

	<xsl:template match="fix:component" mode="emit-group-field-numbers">
		<xsl:variable name="name" select="@name" />
		<xsl:apply-templates select="/fix:fix/fix:components/fix:component[@name=$name]/*"
			mode="group-field-numbers" />
	</xsl:template>

	<!-- Emit a static delimiter value for a group inner class -->
	<xsl:template name="emit-group-delimiter">
		<xsl:value-of select="$NEW_LINE" />
		<xsl:value-of select="$NEW_LINE_INDENT2" />
		<xsl:text>public static final int DELIMITER = </xsl:text>
		<xsl:apply-templates select="fix:field|fix:group|fix:component"
			mode="group-delimiter" />
		<xsl:text>;</xsl:text>
	</xsl:template>

	<xsl:template match="fix:field" mode="group-delimiter">
		<xsl:if test="position() = 1">
			<xsl:variable name="name" select="@name" />
			<xsl:value-of select="/fix:fix/fix:fields/fix:field[@name=$name]/@number" />
		</xsl:if>
	</xsl:template>

	<xsl:template match="fix:group" mode="group-delimiter">
		<xsl:value-of select="@number" />
	</xsl:template>

	<xsl:template match="fix:group//fix:component" mode="group-delimiter">
		<xsl:if test="position() = 1">
			<xsl:variable name="name" select="@name" />
			<xsl:apply-templates
				select="/fix:fix/fix:components/fix:component[@name=$name]/*[name(.)='field' or name(.)='group' or name(.)='component']"
				mode="group-delimiter" />
		</xsl:if>
	</xsl:template>

</xsl:stylesheet>
