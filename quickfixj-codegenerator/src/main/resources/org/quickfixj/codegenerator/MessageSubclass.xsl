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

	<xsl:param name="orderedFields" />
	<xsl:param name="itemName" />
	<xsl:param name="baseClass" select="'Message'" />
	<xsl:param name="subpackage" />

	<!-- ********************************************************************* Main message 
		generation template. This template generates a default constructor and, if any fields 
		are required, generates a constructor taking those fields as arguments. ********************************************************************* -->
	<xsl:template match="fix:fix">
		<xsl:if test="$baseClass = 'Message'">
			<xsl:apply-templates select="fix:messages/fix:message[@name=$itemName]" />
		</xsl:if>
		<xsl:if test="$baseClass = 'quickfix.MessageComponent'">
			<xsl:apply-templates select="fix:components/fix:component[@name=$itemName]" />
		</xsl:if>
	</xsl:template>

	<xsl:template match="fix:message|fix:component">
		<xsl:variable name="package" select="concat($messagePackage,$subpackage)" />

		<xsl:value-of select="$NEW_LINE" />
		<xsl:value-of select="concat('package ', $package, ';')" />

		<xsl:value-of select="$NEW_LINE" />
		<xsl:value-of select="$NEW_LINE" />
		<xsl:text>import quickfix.FieldNotFound;</xsl:text>
		<xsl:call-template name="extra-imports" />

		<xsl:value-of select="$NEW_LINE" />
		<xsl:value-of select="$NEW_LINE" />
		<xsl:value-of select="concat('public class ',@name, ' extends ', $baseClass, ' {')" />

		<xsl:call-template name="emit-serial-version" />

		<xsl:value-of select="$NEW_LINE" />
		<xsl:value-of select="$NEW_LINE_INDENT" />
		<xsl:value-of
			select="concat('public static final String MSGTYPE = &#34;', @msgtype, '&#34;;')" />

		<xsl:if test="$baseClass = 'quickfix.MessageComponent'">
			private int[] componentFields = {
			<xsl:apply-templates select="fix:field|fix:component" mode="component-field-numbers" />
			};
			protected int[] getFields() { return componentFields; }
			private int[]
			componentGroups = {
			<xsl:apply-templates select="fix:group" mode="component-field-numbers" />
			};
			protected int[] getGroupFields() { return componentGroups; }
		</xsl:if>

		<xsl:value-of select="$NEW_LINE" />
		<xsl:value-of select="$NEW_LINE_INDENT" />
		<xsl:value-of select="concat('public ', @name, '() {')" />

		<xsl:choose>
			<xsl:when test="$orderedFields = 'true'">
				super(new int[] {
				<xsl:apply-templates select="fix:field|fix:component|fix:group"
					mode="group-field-numbers" />
				0 });
			</xsl:when>
			<xsl:otherwise>
				super();
			</xsl:otherwise>
		</xsl:choose>
		<xsl:if test="$baseClass = 'Message'">
			<xsl:value-of
				select="concat('getHeader().setField(new ',$fieldPackage,'.MsgType(MSGTYPE));')" />
		</xsl:if>
		}
		<xsl:if test="count(fix:field[@required='Y']) > 0">
			public
			<xsl:value-of select="@name" />
			(
			<xsl:for-each select="fix:field[@required='Y']">
				<xsl:variable name="varname"
					select="concat(translate(substring(@name, 1, 1),
			'ABCDEFGHIJKLMNOPQRSTUVWXYZ',
			'abcdefghijklmnopqrstuvwxyz'),
		substring(@name, 2, string-length(@name)-1))" />
				<xsl:if test="position() > 1">
					,
				</xsl:if>
				<xsl:value-of select="$fieldPackage" />
				.
				<xsl:value-of select="concat(@name, ' ', $varname)" />
			</xsl:for-each>
			) {
			this();
			<xsl:for-each select="fix:field[@required='Y']">
				<xsl:variable name="varname"
					select="concat(translate(substring(@name, 1, 1),
			'ABCDEFGHIJKLMNOPQRSTUVWXYZ',
			'abcdefghijklmnopqrstuvwxyz'),
		substring(@name, 2, string-length(@name)-1))" />
				setField(
				<xsl:value-of select="$varname" />
				);
			</xsl:for-each>
			}
		</xsl:if>
		<xsl:apply-templates select="fix:field|fix:component|fix:group"
			mode="field-accessors" />
		}
	</xsl:template>

	<!-- ********************************************************************* -->
	<!-- Determine extra imports - Group-related import -->
	<!-- ********************************************************************* -->
	<xsl:template name="extra-imports">

		<xsl:variable name="groups" select="fix:group" />
		<xsl:choose>
			<xsl:when test="count($groups) > 0">
				<xsl:value-of select="$NEW_LINE" />
				<xsl:text>import quickfix.Group;</xsl:text>
			</xsl:when>
			<xsl:otherwise>
				<xsl:variable name="exgroup">
					<xsl:for-each select="fix:component">
						<xsl:variable name="myname" select="@name" />
						<xsl:for-each select="//fix:component[@name=$myname]">
							<xsl:call-template name="extra-imports-component" />
						</xsl:for-each>
					</xsl:for-each>
				</xsl:variable>
				<xsl:if test="normalize-space($exgroup)">
					<xsl:value-of select="$NEW_LINE" />
					<xsl:text>import quickfix.Group;</xsl:text>
				</xsl:if>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>

	<!-- Modified this template to recurse across components in search of groups -->
	<xsl:template name="extra-imports-component">
		<xsl:choose>
			<xsl:when test="count(fix:group)">
				Group
			</xsl:when>
			<xsl:otherwise>
				<xsl:for-each select="fix:component">
					<xsl:variable name="myname" select="@name" />
					<xsl:for-each select="//fix:component[@name=$myname]">
						<xsl:call-template name="extra-imports-component" />
					</xsl:for-each>
				</xsl:for-each>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>

	<!-- ********************************************************************* FIX repeating 
		group generation template. - Find first field (for constructor) - Find all fields and 
		their order (for constructor) - Generate field accessor methods ********************************************************************* -->
	<xsl:template mode="field-accessors" match="fix:group">
		<xsl:call-template name="field-accessor-template" />
		<xsl:variable name="groupFieldName" select="@name" />

		<xsl:value-of select="$NEW_LINE" />
		<xsl:value-of select="$NEW_LINE_INDENT" />
		<xsl:value-of select="concat('public static class ', @name, ' extends Group {')" />

		<xsl:call-template name="emit-serial-version" />

		<xsl:value-of select="$NEW_LINE" />
		<xsl:value-of select="$NEW_LINE_INDENT2" />
		<xsl:value-of select="concat('public ', @name, '() {')" />

		super(
		<xsl:value-of select="/fix:fix/fix:fields/fix:field[@name=$groupFieldName]/@number" />
		,
		<xsl:apply-templates select="fix:field|fix:component|fix:group"
			mode="group-delimeter" />
		,
		new int[] {
		<xsl:apply-templates select="fix:field|fix:component|fix:group"
			mode="group-field-numbers" />
		0 });
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

	<!-- ********************************************************************* -->
	<!-- Field accessor method generation. -->
	<!-- ********************************************************************* -->
	<xsl:template mode="field-accessors" match="fix:field">
		<xsl:call-template name="field-accessor-template" />
	</xsl:template>

	<xsl:template name="component-accessor-template">

		<xsl:variable name="type" select="concat($messagePackage,'.component.',@name)" />

		<xsl:value-of select="$NEW_LINE" />
		<xsl:value-of select="$NEW_LINE_INDENT" />
		<xsl:value-of select="concat('public void set(',$type,' component) {')" />
		<xsl:value-of select="$NEW_LINE_INDENT" />
		setComponent(component);
		<xsl:value-of select="$NEW_LINE_INDENT" />
		}

		<xsl:value-of select="$NEW_LINE" />
		<xsl:value-of select="$NEW_LINE_INDENT" />
		<xsl:value-of
			select="concat('public ', $type, ' get(', $type, ' component) throws FieldNotFound {')" />
		<xsl:value-of select="$NEW_LINE_INDENT" />
		getComponent(component);
		<xsl:value-of select="$NEW_LINE_INDENT" />
		return component;
		<xsl:value-of select="$NEW_LINE_INDENT" />
		}

		<xsl:value-of select="$NEW_LINE" />
		<xsl:value-of select="$NEW_LINE_INDENT" />
		<xsl:value-of
			select="concat('public ',$type,' get', @name,'() throws FieldNotFound {')" />
		<xsl:value-of select="$NEW_LINE_INDENT" />
		<xsl:value-of select="concat('return get(new ',$type,'());')" />
		<xsl:value-of select="$NEW_LINE_INDENT" />
		<xsl:text>}</xsl:text>

	</xsl:template>

	<xsl:template mode="field-accessors" match="fix:component">
		<xsl:call-template name="component-accessor-template" />
		<xsl:variable name="name" select="@name" />
		<xsl:apply-templates
			select="/fix:fix/fix:components/fix:component[@name=$name]/*[name(.)='field' or name(.)='group' or name(.)='component']"
			mode="field-accessors" />
	</xsl:template>
</xsl:stylesheet>
