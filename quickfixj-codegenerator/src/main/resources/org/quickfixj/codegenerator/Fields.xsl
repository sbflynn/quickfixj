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

	<xsl:param name="fieldName" />

	<xsl:template match="fix:fix/fix:fields/fix:field[@name=$fieldName]">

		<xsl:value-of select="$NEW_LINE" />
		<xsl:value-of select="concat('package ', $fieldPackage,';')" />

		<xsl:if
			test="@type='UTCTIMESTAMP' or @type='UTCTIMEONLY' or @type='UTCDATE' or @type='UTCDATEONLY'"
		>
			<xsl:value-of select="$NEW_LINE" />
			<xsl:value-of select="$NEW_LINE" />
			<xsl:text>import java.util.Date;</xsl:text>
		</xsl:if>

		<xsl:value-of select="$NEW_LINE" />
		<xsl:value-of select="$NEW_LINE" />
		<xsl:text>@javax.annotation.Generated("org.quickfixj.codegenerator.GenerateMojo")</xsl:text>
		<xsl:value-of select="$NEW_LINE" />
		<xsl:text>@org.quickfixj.annotation.Field(</xsl:text>
		<xsl:value-of select="concat('name=&#34;', @name,'&#34;, ')" />
		<xsl:value-of select="concat('tag=', @number,', ')" />
		<xsl:value-of select="concat('type=org.quickfixj.FIXFieldType.', @type)" />
		<xsl:text>)</xsl:text>
		<xsl:value-of select="$NEW_LINE" />
		<xsl:text>public class </xsl:text>
		<xsl:value-of select="@name" />
		<xsl:text> extends </xsl:text>
		<xsl:call-template name="emit-field-superclass" />
		<xsl:text> { </xsl:text>

		<xsl:call-template name="emit-serial-version" />

		<xsl:value-of select="$NEW_LINE" />
		<xsl:value-of select="$NEW_LINE_INDENT" />
		<xsl:value-of select="concat('public static final int TAG = ', @number, ';')" />

		<xsl:value-of select="$NEW_LINE" />
		<xsl:value-of select="$NEW_LINE_INDENT" />
		<xsl:value-of
			select="concat('protected static final String TAG_CHARACTERS = &#34;', @number, '&#34;;')" />

		<xsl:apply-templates select="fix:value" mode="write" />

		<!-- Type constructor -->
		<xsl:value-of select="$NEW_LINE" />
		<xsl:value-of select="$NEW_LINE_INDENT" />
		<xsl:value-of select="concat('public ', @name,'(')" />
		<xsl:call-template name="emit-type" />
		<xsl:text> data) {</xsl:text>
		<xsl:value-of select="$NEW_LINE_INDENT2" />
		<xsl:text>super(data</xsl:text>
		<xsl:if test="@type='UTCTIMESTAMP' or @type='UTCTIMEONLY'">
			, true
		</xsl:if>
		<xsl:text>);</xsl:text>
		<xsl:value-of select="$NEW_LINE_INDENT" />
		<xsl:text>}</xsl:text>

		<!-- Character constructor -->
		<xsl:value-of select="$NEW_LINE" />
		<xsl:value-of select="$NEW_LINE_INDENT" />
		<xsl:value-of select="concat('public ', @name,'(CharSequence charSequence) {')" />
		<xsl:value-of select="$NEW_LINE_INDENT2" />
		<xsl:text>super(charSequence);</xsl:text>
		<xsl:value-of select="$NEW_LINE_INDENT" />
		<xsl:text>}</xsl:text>

		<xsl:variable name="dataType">
			<xsl:call-template name="emit-type" />
		</xsl:variable>

		<xsl:if test="$dataType = 'java.math.BigDecimal'">
			public
			<xsl:value-of select="@name" />
			(double data) {
			super(
			<xsl:value-of select="@number" />
			, new
			<xsl:value-of select="$dataType" />
			(data));
			}
		</xsl:if>

		<xsl:value-of select="$NEW_LINE" />
		<xsl:value-of select="$NEW_LINE_INDENT" />
		<xsl:text>@Override</xsl:text>
		<xsl:value-of select="$NEW_LINE_INDENT" />
		<xsl:text>public int getTag() {</xsl:text>
		<xsl:value-of select="$NEW_LINE_INDENT2" />
		<xsl:text>return TAG;</xsl:text>
		<xsl:value-of select="$NEW_LINE_INDENT" />
		<xsl:text>}</xsl:text>

		<xsl:value-of select="$NEW_LINE" />
		<xsl:value-of select="$NEW_LINE_INDENT" />
		<xsl:text>@Override</xsl:text>
		<xsl:value-of select="$NEW_LINE_INDENT" />
		<xsl:text>public CharSequence getTagCharacters() {</xsl:text>
		<xsl:value-of select="$NEW_LINE_INDENT2" />
		<xsl:text>return TAG_CHARACTERS;</xsl:text>
		<xsl:value-of select="$NEW_LINE_INDENT" />
		<xsl:text>}</xsl:text>

		<xsl:value-of select="$NEW_LINE" />
		<xsl:text>}</xsl:text>

	</xsl:template>

	<xsl:template name="y-or-n-to-bool">
		<xsl:choose>
			<xsl:when test="@enum='Y'">
				<xsl:text>true</xsl:text>
			</xsl:when>
			<xsl:when test="@enum='N'">
				<xsl:text>false</xsl:text>
			</xsl:when>
		</xsl:choose>
	</xsl:template>

	<xsl:template
		match="fix:value[../@type='MULTIPLESTRINGVALUE' or ../@type='MULTIPLECHARVALUE']"
		mode="write" priority="10"
	>
		<xsl:value-of select="$NEW_LINE" />
		<xsl:value-of select="$NEW_LINE_INDENT" />
		<xsl:text>    public static final String </xsl:text>
		<xsl:value-of select="@description" />
		<xsl:text> = "</xsl:text>
		<xsl:value-of select="@enum" />
		<xsl:text>";</xsl:text>
	</xsl:template>

	<xsl:template match="fix:value[../@type='INT' or ../@type='NUMINGROUP']"
		mode="write" priority="10"
	>
		<xsl:value-of select="$NEW_LINE" />
		<xsl:value-of select="$NEW_LINE_INDENT" />
		<xsl:text>public static final </xsl:text>
		<xsl:value-of select="../@name" />
		<xsl:text> </xsl:text>
		<xsl:value-of select="@description" />
		<xsl:text> = new </xsl:text>
		<xsl:value-of select="../@name" />
		<xsl:text>(</xsl:text>
		<xsl:value-of select="@enum" />
		<xsl:text>);</xsl:text>
	</xsl:template>

	<xsl:template match="fix:value[../@type='STRING' or ../@type='BOOLEAN']"
		mode="write" priority="10"
	>
		<xsl:value-of select="$NEW_LINE" />
		<xsl:value-of select="$NEW_LINE_INDENT" />
		<xsl:text>public static final </xsl:text>
		<xsl:value-of select="../@name" />
		<xsl:text> </xsl:text>
		<xsl:value-of select="@description" />
		<xsl:text> = new </xsl:text>
		<xsl:value-of select="../@name" />
		<xsl:text>("</xsl:text>
		<xsl:value-of select="@enum" />
		<xsl:text>");</xsl:text>
	</xsl:template>

	<xsl:template match="fix:value[../@type='CHAR']" mode="write"
		priority="10"
	>
		<xsl:value-of select="$NEW_LINE" />
		<xsl:value-of select="$NEW_LINE_INDENT" />
		<xsl:text>public static final </xsl:text>
		<xsl:value-of select="../@name" />
		<xsl:text> </xsl:text>
		<xsl:value-of select="@description" />
		<xsl:text> = new </xsl:text>
		<xsl:value-of select="../@name" />
		<xsl:text>('</xsl:text>
		<xsl:value-of select="@enum" />
		<xsl:text>');</xsl:text>
	</xsl:template>

	<xsl:template match="fix:value" mode="write">
		<xsl:value-of select="$NEW_LINE" />
		<xsl:value-of select="$NEW_LINE_INDENT" />
		<xsl:text>    public static final String </xsl:text>
		<xsl:value-of select="@description" />
		<xsl:text> = "</xsl:text>
		<xsl:value-of select="@enum" />
		<xsl:text>";</xsl:text>
	</xsl:template>

	<xsl:template name="version">
		fix
		<xsl:value-of select="//fix/@major" />
		<xsl:value-of select="//fix/@minor" />
	</xsl:template>

</xsl:stylesheet>
