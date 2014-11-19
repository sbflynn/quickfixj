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

	<xsl:import href="Base.xsl" />

	<xsl:output method="text" encoding="UTF-8" />

	<xsl:param name="fieldName" />

	<xsl:template match="fix:fix/fix:fields/fix:field[@name=$fieldName]">

		<xsl:value-of select="$NEW_LINE" />
		<xsl:value-of select="concat('package ', $fieldPackage,';')" />

		<xsl:value-of select="$NEW_LINE" />
		<xsl:value-of select="$NEW_LINE" />
		<xsl:text>import quickfix.</xsl:text>
		<xsl:call-template name="get-field-type" />
		<xsl:text>Field;</xsl:text>

		<xsl:if
			test="@type='UTCTIMESTAMP' or @type='UTCTIMEONLY' or @type='UTCDATE' or @type='UTCDATEONLY'"
		>
			<xsl:value-of select="$NEW_LINE" />
			<xsl:value-of select="$NEW_LINE" />
			<xsl:text>import java.util.Date;</xsl:text>
		</xsl:if>

		<xsl:value-of select="$NEW_LINE" />
		<xsl:value-of select="$NEW_LINE_INDENT" />
		<xsl:text>public class </xsl:text>
		<xsl:value-of select="@name" />
		<xsl:text> extends </xsl:text>
		<xsl:call-template name="get-field-type" />
		<xsl:text>Field { </xsl:text>
		<xsl:value-of select="$NEW_LINE" />

		<xsl:call-template name="emit-serial-version" />

		<xsl:value-of select="$NEW_LINE" />
		<xsl:value-of select="$NEW_LINE_INDENT" />
		<xsl:value-of select="concat('public static final int FIELD = ', @number, ';')" />

		<xsl:apply-templates select="fix:value" mode="write" />

		<xsl:value-of select="$NEW_LINE" />
		<xsl:value-of select="$NEW_LINE" />
		<xsl:value-of select="concat('public ',@name, '() {')" />
		<xsl:value-of select="$NEW_LINE" />
		<xsl:value-of select="concat('super(', @number, ');')" />
		<xsl:value-of select="$NEW_LINE" />
		<xsl:text>}</xsl:text>

		<xsl:value-of select="$NEW_LINE" />
		<xsl:value-of select="$NEW_LINE" />
		<xsl:text>public </xsl:text>
		<xsl:value-of select="@name" />
		<xsl:text>(</xsl:text>
		<xsl:call-template name="get-type" />
		<xsl:text> data) {</xsl:text>
		<xsl:value-of select="$NEW_LINE" />
		<xsl:text>super(</xsl:text>
		<xsl:value-of select="@number" />
		<xsl:text>, data</xsl:text>
		<xsl:if test="@type='UTCTIMESTAMP' or @type='UTCTIMEONLY'">
			, true
		</xsl:if>
		<xsl:text>);</xsl:text>
		}
		<xsl:variable name="dataType">
			<xsl:call-template name="get-type" />
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
		}
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
		match="fix:value[../@type='STRING' or ../@type='MULTIPLESTRINGVALUE' or ../@type='EXCHANGE' or ../@type='MONTHYEAR']"
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
		<xsl:text>    public static final int </xsl:text>
		<xsl:value-of select="@description" />
		<xsl:text> = </xsl:text>
		<xsl:value-of select="@enum" />
		<xsl:text>;</xsl:text>
	</xsl:template>

	<xsl:template match="fix:value[../@type='BOOLEAN']" mode="write"
		priority="10"
	>
		<xsl:value-of select="$NEW_LINE" />
		<xsl:value-of select="$NEW_LINE_INDENT" />
		<xsl:text>    public static final boolean </xsl:text>
		<xsl:value-of select="@description" />
		<xsl:text> = </xsl:text>
		<xsl:call-template name="y-or-n-to-bool" />
		<xsl:text>;</xsl:text>
	</xsl:template>

	<xsl:template match="fix:value" mode="write">
		<xsl:value-of select="$NEW_LINE" />
		<xsl:value-of select="$NEW_LINE_INDENT" />
		<xsl:text>    public static final char </xsl:text>
		<xsl:value-of select="@description" />
		<xsl:text> = '</xsl:text>
		<xsl:value-of select="@enum" />
		<xsl:text>';</xsl:text>
	</xsl:template>

	<xsl:template name="version">
		fix
		<xsl:value-of select="//fix/@major" />
		<xsl:value-of select="//fix/@minor" />
	</xsl:template>

</xsl:stylesheet>
