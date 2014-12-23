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

	<xsl:import href="emit-group.xsl" />

	<xsl:variable name="NEW_LINE" select="'&#10;'" />
	<xsl:variable name="NEW_LINE_INDENT" select="'&#10;&#09;'" />
	<xsl:variable name="NEW_LINE_INDENT2" select="'&#10;&#09;&#09;'" />
	<xsl:variable name="NEW_LINE_INDENT3" select="'&#10;&#09;&#09;&#09;'" />
	<xsl:variable name="INDENT" select="'&#09;'" />

	<xsl:param name="serialVersionUID" />
	<xsl:param name="messagePackage" />
	<xsl:param name="fieldPackage" select="concat($messagePackage, '.field')" />
	<xsl:param name="componentPackage" select="concat($messagePackage, '.component')" />
	<xsl:param name="decimalType" select="'double'" />
	<xsl:param name="decimalConverter" select="'Double'" />

	<xsl:template match="text()" />

	<xsl:template match="fix:fix/fix:header | fix:fix/fix:trailer" />

	<xsl:template match="/">
		<xsl:text>/*******************************************************************************
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
******************************************************************************/</xsl:text>
		<xsl:apply-templates />
	</xsl:template>

	<xsl:template name="emit-type">
		<xsl:choose>
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
			<xsl:when test="@type='DATA'">
				<xsl:text>byte[]</xsl:text>
			</xsl:when>
			<xsl:otherwise>
				<xsl:text>String</xsl:text>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>

	<xsl:template name="emit-field-superclass">
		<xsl:choose>
			<xsl:when test="@type='CHAR'">
				<xsl:text>org.quickfixj.field.CharField</xsl:text>
			</xsl:when>
			<xsl:when test="@type='PRICE'">
				<xsl:text>org.quickfixj.field.</xsl:text>
				<xsl:value-of select="$decimalConverter" />
				<xsl:text>Field</xsl:text>
			</xsl:when>
			<xsl:when test="@type='INT'">
				<xsl:text>org.quickfixj.field.IntField</xsl:text>
			</xsl:when>
			<xsl:when test="@type='AMT'">
				<xsl:text>org.quickfixj.field.</xsl:text>
				<xsl:value-of select="$decimalConverter" />
				<xsl:text>Field</xsl:text>
			</xsl:when>
			<xsl:when test="@type='QTY'">
				<xsl:text>org.quickfixj.field.</xsl:text>
				<xsl:value-of select="$decimalConverter" />
				<xsl:text>Field</xsl:text>
			</xsl:when>
			<xsl:when test="@type='UTCTIMESTAMP'">
				<xsl:text>org.quickfixj.field.UtcTimestampField</xsl:text>
			</xsl:when>
			<xsl:when test="@type='UTCTIMEONLY'">
				<xsl:text>org.quickfixj.field.UtcTimeOnlyField</xsl:text>
			</xsl:when>
			<xsl:when test="@type='UTCDATE'">
				<xsl:text>org.quickfixj.field.UtcDateOnlyField</xsl:text>
			</xsl:when>
			<xsl:when test="@type='UTCDATEONLY'">
				<xsl:text>org.quickfixj.field.UtcDateOnlyField</xsl:text>
			</xsl:when>
			<xsl:when test="@type='BOOLEAN'">
				<xsl:text>org.quickfixj.field.BooleanField</xsl:text>
			</xsl:when>
			<xsl:when test="@type='FLOAT'">
				<xsl:text>org.quickfixj.field.DoubleField</xsl:text>
			</xsl:when>
			<xsl:when test="@type='PRICEOFFSET'">
				<xsl:text>org.quickfixj.field.</xsl:text>
				<xsl:value-of select="$decimalConverter" />
				<xsl:text>Field</xsl:text>
			</xsl:when>
			<xsl:when test="@type='NUMINGROUP'">
				<xsl:text>org.quickfixj.field.IntField</xsl:text>
			</xsl:when>
			<xsl:when test="@type='PERCENTAGE'">
				<xsl:text>org.quickfixj.field.DoubleField</xsl:text>
			</xsl:when>
			<xsl:when test="@type='SEQNUM'">
				<xsl:text>org.quickfixj.field.IntField</xsl:text>
			</xsl:when>
			<xsl:when test="@type='LENGTH'">
				<xsl:text>org.quickfixj.field.IntField</xsl:text>
			</xsl:when>
			<xsl:when test="@type='DATA'">
				<xsl:text>org.quickfixj.field.BytesField</xsl:text>
			</xsl:when>
			<xsl:otherwise>
				<xsl:text>org.quickfixj.field.StringField</xsl:text>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>

	<xsl:template name="emit-serial-version">

		<xsl:value-of select="$NEW_LINE" />
		<xsl:value-of select="$NEW_LINE_INDENT" />
		<xsl:value-of
			select="concat('private static final long serialVersionUID = ', $serialVersionUID,';')" />

	</xsl:template>

	<!-- ********************************************************************* -->
	<!-- Field accessor method generation. -->
	<!-- ********************************************************************* -->

	<xsl:template mode="field-accessors-concrete" match="fix:field">
		<xsl:call-template name="emit-field-accessor-concrete">
			<xsl:with-param name="fieldType" select="concat($fieldPackage,'.',@name)" />
		</xsl:call-template>
	</xsl:template>

	<xsl:template mode="field-accessors-abstract" match="fix:field">
		<xsl:call-template name="emit-field-accessor-abstract">
			<xsl:with-param name="fieldType" select="concat($fieldPackage,'.',@name)" />
		</xsl:call-template>
	</xsl:template>

	<xsl:template name="emit-field-accessor-concrete">

		<xsl:param name="fieldType" select="concat($fieldPackage,'.',@name)" />
		<xsl:param name="override" select="false()" />

		<xsl:variable name="name" select="@name" />
		<xsl:variable name="number"
			select="/fix:fix/fix:fields/fix:field[@name=$name]/@number" />

		<xsl:value-of select="$NEW_LINE" />
		<xsl:if test="$override">
			<xsl:value-of select="$NEW_LINE_INDENT" />
			<xsl:text>@Override</xsl:text>
		</xsl:if>
		<xsl:value-of select="$NEW_LINE_INDENT" />
		<xsl:value-of select="concat('public ', $fieldType, ' get', @name, '() {')" />
		<xsl:value-of select="$NEW_LINE_INDENT2" />
		<xsl:value-of
			select="concat('return getField(', $fieldType, '.TAG, ', $fieldType, '.class);')" />
		<xsl:value-of select="$NEW_LINE_INDENT" />
		<xsl:text>}</xsl:text>

		<xsl:value-of select="$NEW_LINE" />
		<xsl:if test="$override">
			<xsl:value-of select="$NEW_LINE_INDENT" />
			<xsl:text>@Override</xsl:text>
		</xsl:if>
		<xsl:value-of select="$NEW_LINE_INDENT" />
		<xsl:value-of select="concat('public void set', $name, '(', $fieldType,' value) {')" />
		<xsl:value-of select="$NEW_LINE_INDENT2" />
		<xsl:text>setField(value);</xsl:text>
		<xsl:value-of select="$NEW_LINE_INDENT" />
		<xsl:text>}</xsl:text>

	</xsl:template>

	<xsl:template name="emit-field-accessor-abstract">

		<xsl:param name="fieldType" select="concat($fieldPackage,'.',@name)" />

		<xsl:variable name="name" select="@name" />

		<xsl:value-of select="$NEW_LINE" />
		<xsl:value-of select="$NEW_LINE_INDENT" />
		<xsl:value-of select="concat('public ', $fieldType, ' get', @name, '();')" />

		<xsl:value-of select="$NEW_LINE" />
		<xsl:value-of select="$NEW_LINE_INDENT" />
		<xsl:value-of select="concat('public void set', $name, '(', $fieldType,' value);')" />

	</xsl:template>

	<xsl:template match="fix:component" mode="field-accessors-concrete">
		<xsl:variable name="name" select="@name" />
		<xsl:apply-templates select="/fix:fix/fix:components/fix:component[@name=$name]"
			mode="field-accessors-concrete" />
	</xsl:template>

	<xsl:template match="/fix:fix/fix:components/fix:component" mode="field-accessors-concrete">
		<xsl:for-each select="fix:field">
			<xsl:value-of select="$NEW_LINE_INDENT" />
			<xsl:call-template name="emit-field-accessor-concrete">
				<xsl:with-param name="fieldType" select="concat($fieldPackage,'.',@name)" />
				<xsl:with-param name="override" select="true()" />
			</xsl:call-template>
		</xsl:for-each>
		<xsl:for-each select="fix:group">
			<xsl:call-template name="emit-field-accessor-concrete">
				<xsl:with-param name="fieldType"
					select="concat($componentPackage,'.',../@name,'.',@name)" />
				<xsl:with-param name="override" select="true()" />
			</xsl:call-template>
		</xsl:for-each>
		<xsl:for-each select="fix:component">
			<xsl:variable name="name" select="@name" />
			<xsl:apply-templates select="/fix:fix/fix:components/fix:component[@name=$name]"
				mode="field-accessors-concrete" />
		</xsl:for-each>

		<xsl:value-of select="$NEW_LINE" />
		<xsl:value-of select="$NEW_LINE_INDENT" />
		<xsl:value-of
			select="concat('public void copyValues(', $componentPackage, '.', @name, ' component) {')" />
		<xsl:value-of select="$NEW_LINE_INDENT2" />
		<xsl:value-of
			select="concat('super.copyValues(component, ', $componentPackage, '.', @name, '.COMPONENT_FIELDS);')" />
		<xsl:value-of select="$NEW_LINE_INDENT" />
		<xsl:text>}</xsl:text>

	</xsl:template>

	<xsl:template match="fix:component" mode="field-accessors-abstract">
		<xsl:variable name="name" select="@name" />
		<xsl:apply-templates select="/fix:fix/fix:components/fix:component[@name=$name]"
			mode="field-accessors-abstract" />
	</xsl:template>

	<xsl:template match="/fix:fix/fix:components/fix:component" mode="field-accessors-abstract">
		<xsl:for-each select="fix:field">
			<xsl:value-of select="$NEW_LINE_INDENT" />
			<xsl:call-template name="emit-field-accessor-abstract">
				<xsl:with-param name="fieldType" select="concat($fieldPackage,'.',@name)" />
			</xsl:call-template>
		</xsl:for-each>
		<xsl:for-each select="fix:group">
			<xsl:call-template name="emit-field-accessor-abstract">
				<xsl:with-param name="fieldType"
					select="concat($componentPackage,'.',../@name,'.',@name)" />
			</xsl:call-template>
		</xsl:for-each>
	</xsl:template>

</xsl:stylesheet>
