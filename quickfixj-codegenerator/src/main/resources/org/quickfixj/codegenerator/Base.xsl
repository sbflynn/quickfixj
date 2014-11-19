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

	<xsl:variable name="NEW_LINE" select="'&#10;'" />
	<xsl:variable name="NEW_LINE_INDENT" select="'&#10;&#09;'" />
	<xsl:variable name="NEW_LINE_INDENT2" select="'&#10;&#09;&#09;'" />

	<xsl:param name="serialVersionUID" />
	<xsl:param name="fieldPackage" />
	<xsl:param name="messagePackage" />
	<xsl:param name="decimalType" select="'double'" />
	<xsl:param name="decimalConverter" select="'Double'" />

	<xsl:template match="text()" />

	<xsl:template match="fix:fix/fix:header | fix:fix/fix:trailer" />

	<xsl:template match="/">
		<xsl:text>
/* Generated Java Source File */
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
        </xsl:text>
		<xsl:apply-templates />
	</xsl:template>

	<xsl:template name="get-type">
		<xsl:choose>
			<xsl:when test="@type='STRING'">
				<xsl:text>String</xsl:text>
			</xsl:when>
			<xsl:when test="@type='CHAR'">
				<xsl:text>char</xsl:text>
			</xsl:when>
			<xsl:when test="@type='PRICE'">
				<xsl:value-of select="$decimalType" />
			</xsl:when>
			<xsl:when test="@type='INT'">
				<xsl:text>int</xsl:text>
			</xsl:when>
			<xsl:when test="@type='AMT'">
				<xsl:value-of select="$decimalType" />
			</xsl:when>
			<xsl:when test="@type='QTY'">
				<xsl:value-of select="$decimalType" />
			</xsl:when>
			<xsl:when test="@type='CURRENCY'">
				<xsl:text>String</xsl:text>
			</xsl:when>
			<xsl:when test="@type='UTCTIMESTAMP'">
				<xsl:text>Date</xsl:text>
			</xsl:when>
			<xsl:when test="@type='UTCTIMEONLY'">
				<xsl:text>Date</xsl:text>
			</xsl:when>
			<xsl:when test="@type='UTCDATE'">
				<xsl:text>Date</xsl:text>
			</xsl:when>
			<xsl:when test="@type='UTCDATEONLY'">
				<xsl:text>Date</xsl:text>
			</xsl:when>
			<xsl:when test="@type='BOOLEAN'">
				<xsl:text>boolean</xsl:text>
			</xsl:when>
			<xsl:when test="@type='FLOAT'">
				<xsl:text>double</xsl:text>
			</xsl:when>
			<xsl:when test="@type='PRICEOFFSET'">
				<xsl:value-of select="$decimalType" />
			</xsl:when>
			<xsl:when test="@type='NUMINGROUP'">
				<xsl:text>int</xsl:text>
			</xsl:when>
			<xsl:when test="@type='PERCENTAGE'">
				<xsl:text>double</xsl:text>
			</xsl:when>
			<xsl:when test="@type='SEQNUM'">
				<xsl:text>int</xsl:text>
			</xsl:when>
			<xsl:when test="@type='LENGTH'">
				<xsl:text>int</xsl:text>
			</xsl:when>
			<xsl:when test="@type='COUNTRY'">
				<xsl:text>String</xsl:text>
			</xsl:when>
			<xsl:when test="@type='MULTIPLESTRINGVALUE'">
				<xsl:text>String</xsl:text>
			</xsl:when>
			<xsl:otherwise>
				<xsl:text>String</xsl:text>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>

	<xsl:template name="get-field-type">
		<xsl:choose>
			<xsl:when test="@type='STRING'">
				<xsl:text>String</xsl:text>
			</xsl:when>
			<xsl:when test="@type='CHAR'">
				<xsl:text>Char</xsl:text>
			</xsl:when>
			<xsl:when test="@type='PRICE'">
				<xsl:value-of select="$decimalConverter" />
			</xsl:when>
			<xsl:when test="@type='INT'">
				<xsl:text>Int</xsl:text>
			</xsl:when>
			<xsl:when test="@type='AMT'">
				<xsl:value-of select="$decimalConverter" />
			</xsl:when>
			<xsl:when test="@type='QTY'">
				<xsl:value-of select="$decimalConverter" />
			</xsl:when>
			<xsl:when test="@type='CURRENCY'">
				<xsl:text>String</xsl:text>
			</xsl:when>
			<xsl:when test="@type='UTCTIMESTAMP'">
				<xsl:text>UtcTimeStamp</xsl:text>
			</xsl:when>
			<xsl:when test="@type='UTCTIMEONLY'">
				<xsl:text>UtcTimeOnly</xsl:text>
			</xsl:when>
			<xsl:when test="@type='UTCDATE'">
				<xsl:text>UtcDateOnly</xsl:text>
			</xsl:when>
			<xsl:when test="@type='UTCDATEONLY'">
				<xsl:text>UtcDateOnly</xsl:text>
			</xsl:when>
			<xsl:when test="@type='BOOLEAN'">
				<xsl:text>Boolean</xsl:text>
			</xsl:when>
			<xsl:when test="@type='FLOAT'">
				<xsl:text>Double</xsl:text>
			</xsl:when>
			<xsl:when test="@type='PRICEOFFSET'">
				<xsl:value-of select="$decimalConverter" />
			</xsl:when>
			<xsl:when test="@type='NUMINGROUP'">
				<xsl:text>Int</xsl:text>
			</xsl:when>
			<xsl:when test="@type='PERCENTAGE'">
				<xsl:text>Double</xsl:text>
			</xsl:when>
			<xsl:when test="@type='SEQNUM'">
				<xsl:text>Int</xsl:text>
			</xsl:when>
			<xsl:when test="@type='LENGTH'">
				<xsl:text>Int</xsl:text>
			</xsl:when>
			<xsl:when test="@type='COUNTRY'">
				<xsl:text>String</xsl:text>
			</xsl:when>
			<xsl:when test="@type='MULTIPLESTRINGVALUE'">
				<xsl:text>String</xsl:text>
			</xsl:when>
			<xsl:otherwise>
				<xsl:text>String</xsl:text>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>

	<xsl:template name="emit-serial-version">

		<xsl:value-of select="$NEW_LINE" />
		<xsl:value-of select="$NEW_LINE_INDENT" />
		<xsl:value-of
			select="concat('private static final long serialVersionUID = ', $serialVersionUID,';')" />

	</xsl:template>

	<xsl:template name="field-accessor-template">

		<xsl:variable name="name" select="@name" />
		<xsl:variable name="type" select="concat($fieldPackage,'.',$name)" />
		<xsl:variable name="number"
			select="/fix:fix/fix:fields/fix:field[@name=$name]/@number" />

		<xsl:value-of select="$NEW_LINE" />
		<xsl:value-of select="$NEW_LINE_INDENT" />
		<xsl:value-of select="concat('public void set(', $type,' value) {')" />
		<xsl:value-of select="$NEW_LINE_INDENT2" />
		<xsl:text>setField(value);</xsl:text>
		<xsl:value-of select="$NEW_LINE_INDENT" />
		<xsl:text>}</xsl:text>

		<xsl:value-of select="$NEW_LINE" />
		<xsl:value-of select="$NEW_LINE_INDENT" />
		<xsl:value-of
			select="concat('public ', $type,' get(', $type,' value) throws FieldNotFound {')" />
		<xsl:value-of select="$NEW_LINE_INDENT2" />
		<xsl:text>getField(value);</xsl:text>
		<xsl:value-of select="$NEW_LINE_INDENT2" />
		<xsl:text>return value;</xsl:text>
		<xsl:value-of select="$NEW_LINE_INDENT" />
		<xsl:text>}</xsl:text>

		<xsl:value-of select="$NEW_LINE" />
		<xsl:value-of select="$NEW_LINE_INDENT" />
		<xsl:value-of
			select="concat('public ', $type,' get',@name,'() throws FieldNotFound {')" />
		<xsl:value-of select="$NEW_LINE_INDENT2" />
		<xsl:value-of select="concat('return get(new ',$type,'());')" />
		<xsl:value-of select="$NEW_LINE_INDENT" />
		<xsl:text>}</xsl:text>

		<xsl:value-of select="$NEW_LINE" />
		<xsl:value-of select="$NEW_LINE_INDENT" />
		<xsl:value-of select="concat('public boolean isSet(', $type,' field) {')" />
		<xsl:value-of select="$NEW_LINE_INDENT2" />
		<xsl:text>return isSetField(field);</xsl:text>
		<xsl:value-of select="$NEW_LINE_INDENT" />
		<xsl:text>}</xsl:text>

		<xsl:value-of select="$NEW_LINE" />
		<xsl:value-of select="$NEW_LINE_INDENT" />
		<xsl:value-of select="concat('public boolean isSet', @name, '() {')" />
		<xsl:value-of select="$NEW_LINE_INDENT2" />
		<xsl:value-of select="concat('return isSetField(',$number,');')" />
		<xsl:value-of select="$NEW_LINE_INDENT" />
		<xsl:text>}</xsl:text>

	</xsl:template>

</xsl:stylesheet>
