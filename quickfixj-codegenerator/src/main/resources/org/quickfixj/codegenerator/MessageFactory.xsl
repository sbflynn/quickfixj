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
		<xsl:text>import quickfix.Message;</xsl:text>
		<xsl:value-of select="$NEW_LINE" />
		<xsl:text>import quickfix.Group;</xsl:text>

		<xsl:value-of select="$NEW_LINE" />
		<xsl:value-of select="$NEW_LINE" />
		<xsl:text>@javax.annotation.Generated("org.quickfixj.codegenerator.GenerateMojo")</xsl:text>
		<xsl:value-of select="$NEW_LINE" />
		<xsl:text>public class MessageFactory implements quickfix.MessageFactory {</xsl:text>

		<xsl:value-of select="$NEW_LINE" />
		<xsl:value-of select="$NEW_LINE_INDENT" />
		<xsl:text>@Override</xsl:text>
		<xsl:value-of select="$NEW_LINE_INDENT" />
		<xsl:text>public Message create(String beginString, String msgType) {</xsl:text>

		<xsl:call-template name="if-statement" />

		<xsl:value-of select="$NEW_LINE" />
		<xsl:text>    return new </xsl:text>
		<xsl:value-of select="$messagePackage" />
		<xsl:text>.Message();</xsl:text>
		<xsl:value-of select="$NEW_LINE" />
		<xsl:text>    }</xsl:text>

		<xsl:value-of select="$NEW_LINE" />
		<xsl:value-of select="$NEW_LINE_INDENT" />
		<xsl:text>@Override</xsl:text>
		<xsl:value-of select="$NEW_LINE_INDENT" />
		<xsl:text>public Group create(String beginString, String msgType, int correspondingFieldID) {</xsl:text>

		<xsl:call-template name="group-if-statement" />

		<xsl:text>
        return null;</xsl:text>
		<xsl:value-of select="$NEW_LINE_INDENT2" />
		<xsl:text>}</xsl:text>
		<xsl:value-of select="$NEW_LINE_INDENT" />
		<xsl:text>}</xsl:text>
	</xsl:template>

	<xsl:template name="if-statement">
		<xsl:for-each select="fix:messages/fix:message">
			<xsl:variable name="type" select="concat($messagePackage, '.', @name)" />
			<xsl:value-of select="$NEW_LINE" />
			<xsl:value-of select="$NEW_LINE_INDENT2" />
			<xsl:value-of select="concat('if (', $type,'.MSGTYPE.equals(msgType)) {')" />
			<xsl:value-of select="$NEW_LINE_INDENT2" />
			<xsl:value-of select="concat('return new ', $type, '();')" />
			<xsl:value-of select="$NEW_LINE_INDENT2" />
			<xsl:text>}</xsl:text>
		</xsl:for-each>
	</xsl:template>

	<xsl:template name="group-if-statement">
		<xsl:for-each select="fix:messages/fix:message[fix:group or fix:component]">
			<xsl:variable name="type" select="concat($messagePackage, '.', @name)" />
			<xsl:value-of select="$NEW_LINE" />
			<xsl:value-of select="$NEW_LINE_INDENT" />
			<xsl:value-of select="concat('if (', $type,'.MSGTYPE.equals(msgType)) {')" />
			<xsl:value-of select="$NEW_LINE_INDENT" />
			<xsl:text>switch (correspondingFieldID) {</xsl:text>
			<xsl:apply-templates mode="group-factories" select="fix:group">
				<xsl:with-param name="fullPath" select="@name" />
			</xsl:apply-templates>
			<xsl:apply-templates mode="group-factories" select="fix:component">
				<xsl:with-param name="fullPath" select="@name" />
			</xsl:apply-templates>
			<xsl:value-of select="$NEW_LINE" />
			<xsl:value-of select="$NEW_LINE_INDENT2" />
			<xsl:text>default:</xsl:text>
			<xsl:value-of select="$NEW_LINE_INDENT2" />
			<xsl:text>return null;</xsl:text>
			<xsl:value-of select="$NEW_LINE_INDENT2" />
			<xsl:text>}</xsl:text>
			<xsl:value-of select="$NEW_LINE_INDENT" />
			<xsl:text>}</xsl:text>
		</xsl:for-each>
	</xsl:template>

	<xsl:template mode="group-factories" match="fix:group">
		<xsl:param name="fullPath" />
		<xsl:variable name="groupPath" select="concat($fullPath, '.', @name)" />

		<xsl:value-of select="$NEW_LINE" />
		<xsl:value-of select="$NEW_LINE_INDENT2" />
		<xsl:value-of select="concat('case ', $fieldPackage, '.',@name,'.TAG :')" />
		<xsl:value-of select="$NEW_LINE_INDENT2" />
		<xsl:value-of
			select="concat('return new ', $messagePackage, '.', $fullPath, '.', @name, '();')" />
		<xsl:apply-templates mode="group-factories" select="fix:group">
			<xsl:with-param name="fullPath" select='$groupPath' />
		</xsl:apply-templates>
		<xsl:apply-templates mode="group-factories" select="fix:component">
			<xsl:with-param name="fullPath" select="$groupPath" />
		</xsl:apply-templates>
	</xsl:template>

	<xsl:template mode="group-factories" match="fix:component">
		<xsl:param name="fullPath" />
		<xsl:variable name="name" select="@name" />
		<xsl:apply-templates mode="group-factories"
			select="/fix:fix/fix:components/fix:component[@name=$name]/fix:group"
		>
			<xsl:with-param name="fullPath" select='$fullPath' />
		</xsl:apply-templates>
	</xsl:template>

</xsl:stylesheet>
