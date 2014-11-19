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

	<xsl:template match="fix:fix">

		<xsl:value-of select="concat('package ', $messagePackage,';')" />
		<xsl:value-of select="$NEW_LINE" />
		<xsl:value-of select="$NEW_LINE" />

		<xsl:if test="@major='4' or @type='FIXT'">
			<xsl:text>import quickfix.FieldNotFound;</xsl:text>
			<xsl:value-of select="$NEW_LINE" />
		</xsl:if>

		<xsl:text>import quickfix.field.*;</xsl:text>
		<xsl:value-of select="$NEW_LINE" />

		<xsl:call-template name="extra-imports" />

		<xsl:text>
public class Message extends quickfix.Message {
        </xsl:text>

		<xsl:call-template name="emit-serial-version" />

		<xsl:text>
    public Message() {
        this(null);
    }
        </xsl:text>

		<xsl:text>
    protected Message(int[] fieldOrder) {
        super(fieldOrder);
		header = new Header(this);
		trailer = new Trailer();
        </xsl:text>

		<xsl:choose>
			<xsl:when test="@major='4'">
				<xsl:text>getHeader().setField(new BeginString("</xsl:text>
				<xsl:value-of select="concat('FIX.',@major,'.',@minor)" />
				<xsl:text>"));</xsl:text>
			</xsl:when>
			<xsl:when test="@major='5' or @type='FIXT'">
				getHeader().setField(new BeginString("FIXT.1.1"));
			</xsl:when>
		</xsl:choose>

		<xsl:text>
    }
        </xsl:text>

		public static class Header extends quickfix.Message.Header {

		<xsl:call-template name="emit-serial-version" />

		public Header(Message msg) {
		// JNI compatibility
		}
		<xsl:apply-templates select="fix:header/fix:field" mode="field-accessors" />
		<xsl:apply-templates select="fix:header/fix:group" mode="field-accessors" />
		<xsl:apply-templates select="fix:header/fix:component" mode="field-accessors" />
		}
		<!-- TODO Must talk to Oren about why these are defined at the message level -->
		<xsl:apply-templates select="fix:trailer/fix:field" mode="field-accessors" />
		<xsl:apply-templates select="fix:trailer/fix:group" mode="field-accessors" />
		<xsl:apply-templates select="fix:trailer/fix:component"
			mode="field-accessors" />
		}
	</xsl:template>

	<!-- The following templates are almost duplicated from Message.xsl. However, there 
		are a few slight differences with how header groups are handled. -->

	<!-- ********************************************************************* FIX repeating 
		group generation template. - Find first field (for constructor) - Find all fields and 
		their order (for constructor) - Generate field accessor methods ********************************************************************* -->

	<xsl:template mode="field-accessors" match="fix:group">
		<xsl:call-template name="field-accessor-template" />
		<xsl:variable name="groupFieldName" select="@name" />
		public static class
		<xsl:value-of select="@name" />
		extends Group {

		<xsl:call-template name="emit-serial-version" />

		public
		<xsl:value-of select="@name" />
		() {
		super(
		<xsl:value-of select="/fix:fix/fix:fields/fix:field[@name=$groupFieldName]/@number" />
		,
		<xsl:apply-templates select="fix:field|fix:component|fix:group"
			mode="group-delimeter" />
		,
		new int[] {
		<xsl:apply-templates select="fix:field|fix:component|fix:group"
			mode="group-field-numbers" />
		0});
		}
		<xsl:apply-templates select="fix:field|fix:component|fix:group"
			mode="field-accessors" />
		}
	</xsl:template>

	<!-- Find the group delimeter (first field) -->

	<xsl:template mode="group-delimeter" match="fix:field">
		<xsl:if test="position() = 1">
			<xsl:variable name="name" select="@name" />
			<xsl:value-of select="/fix:fix/fix:fields/fix:field[@name=$name]/@number" />
		</xsl:if>
	</xsl:template>

	<xsl:template mode="group-delimeter" match="fix:group">
		<xsl:value-of select="@number" />
	</xsl:template>

	<xsl:template mode="group-delimeter" match="fix:group//fix:component">
		<xsl:if test="position() = 1">
			<xsl:variable name="name" select="@name" />
			<xsl:apply-templates
				select="/fix:fix/fix:components/fix:component[@name=$name]/*[name(.)='field' or name(.)='group' or name(.)='component']"
				mode="group-delimeter" />
		</xsl:if>
	</xsl:template>

	<!-- Find the component numbers and order -->

	<xsl:template mode="component-field-numbers" match="fix:field">
		<xsl:variable name="name" select="@name" />
		<xsl:value-of select="/fix:fix/fix:fields/fix:field[@name=$name]/@number" />
		,
	</xsl:template>

	<xsl:template mode="component-field-numbers" match="fix:group">
		<xsl:variable name="name" select="@name" />
		<xsl:value-of select="/fix:fix/fix:fields/fix:field[@name=$name]/@number" />
		,
	</xsl:template>

	<xsl:template mode="component-field-numbers" match="fix:component">
		<xsl:variable name="name" select="@name" />
		<xsl:apply-templates select="/fix:fix/fix:components/fix:component[@name=$name]/*"
			mode="component-field-numbers" />
	</xsl:template>

	<!-- ================================================================= -->

	<!-- Find the field numbers and order -->

	<xsl:template mode="group-field-numbers" match="fix:field|fix:group">
		<xsl:variable name="name" select="@name" />
		<xsl:value-of select="/fix:fix/fix:fields/fix:field[@name=$name]/@number" />
		,
	</xsl:template>

	<xsl:template mode="group-field-numbers" match="fix:component">
		<xsl:variable name="name" select="@name" />
		<xsl:apply-templates select="/fix:fix/fix:components/fix:component[@name=$name]/*"
			mode="group-field-numbers" />
	</xsl:template>

	<!-- ********************************************************************* Field accessor 
		method generation. ********************************************************************* -->
	<xsl:template mode="field-accessors" match="fix:field">
		<xsl:call-template name="field-accessor-template" />
	</xsl:template>

	<xsl:template name="component-accessor-template">
		<xsl:variable name="type" select="concat($messagePackage,'.component.',@name)" />
		public void set(
		<xsl:value-of select="$type" />
		component) {
		setComponent(component);
		}

		public
		<xsl:value-of select="$type" />
		get(
		<xsl:value-of select="$type" />
		component) throws FieldNotFound {
		getComponent(component);
		return component;
		}

		public
		<xsl:value-of select="$type" />
		get
		<xsl:value-of select="@name" />
		() throws FieldNotFound {
		return get(new
		<xsl:value-of select="$type" />
		());
		}
	</xsl:template>

	<xsl:template mode="field-accessors" match="fix:message//fix:component">
		<xsl:call-template name="component-accessor-template" />
		<xsl:variable name="name" select="@name" />
		<xsl:apply-templates
			select="/fix:fix/fix:components/fix:component[@name=$name]/*[name(.)='field' or name(.)='group' or name(.)='component']"
			mode="field-accessors" />
	</xsl:template>

	<xsl:template name="extra-imports">
		<xsl:variable name="groups" select="/fix:fix/fix:header/fix:group" />
		<xsl:choose>
			<xsl:when test="count($groups) > 0">
				import quickfix.Group;
			</xsl:when>
			<xsl:otherwise>
				<xsl:call-template name="extra-imports-component">
					<xsl:with-param name="components" select="fix:component" />
					<xsl:with-param name="position" select="1" />
				</xsl:call-template>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>

	<!-- TODO Remove this duplication from MessageSubclass.xsl -->
	<xsl:template name="extra-imports-component">
		<xsl:param name="components" />
		<xsl:param name="position" />
		<xsl:if test="$position &lt;= count($components)">
			<xsl:variable name="name" select="$components[$position]/@name" />
			<xsl:variable name="group"
				select="/fix:fix/fix:components/fix:component[@name=$name]/fix:group[1]" />
			<xsl:choose>
				<xsl:when test="$group">
					import quickfix.Group;
				</xsl:when>
				<xsl:otherwise>
					<xsl:call-template name="extra-imports-component">
						<xsl:with-param name="components" select="$components" />
						<xsl:with-param name="position" select="$position + 1" />
					</xsl:call-template>
				</xsl:otherwise>
			</xsl:choose>
		</xsl:if>
	</xsl:template>

</xsl:stylesheet>
