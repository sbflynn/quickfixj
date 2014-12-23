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

	<xsl:template match="fix:fix[count(fix:messages/fix:message) > 0]">

		<xsl:value-of select="$NEW_LINE" />
		<xsl:value-of select="concat('package ', $messagePackage,';')" />

		<xsl:value-of select="$NEW_LINE" />
		<xsl:value-of select="$NEW_LINE" />
		<xsl:text>@javax.annotation.Generated("org.quickfixj.codegenerator.GenerateMojo")</xsl:text>
		<xsl:value-of select="$NEW_LINE" />
		<xsl:text>public abstract class MessageCracker {</xsl:text>

		<xsl:call-template name="switch-statement" />
		<xsl:call-template name="virtual-functions" />

		<!-- Very basic javadoc -->
		<xsl:value-of select="$NEW_LINE" />
		<xsl:value-of select="$NEW_LINE_INDENT" />
		<xsl:text>/**</xsl:text>
		<xsl:value-of select="$NEW_LINE_INDENT" />
		<xsl:text> * Callback for uncracked {@link org.quickfixj.FIXMessage} instance.</xsl:text>
		<xsl:value-of select="$NEW_LINE_INDENT" />
		<xsl:text> * </xsl:text>
		<xsl:value-of select="$NEW_LINE_INDENT" />
		<xsl:text> * @param message</xsl:text>
		<xsl:value-of select="$NEW_LINE_INDENT" />
		<xsl:text> * @param sessionID</xsl:text>
		<xsl:value-of select="$NEW_LINE_INDENT" />
		<xsl:text> * @throws quickfix.UnsupportedMessageType</xsl:text>
		<xsl:value-of select="$NEW_LINE_INDENT" />
		<xsl:text> **/</xsl:text>

		<!-- Uncracked message fall-through -->
		<xsl:value-of select="$NEW_LINE_INDENT" />
		<xsl:text>public void onMessage(org.quickfixj.FIXMessage message, quickfix.SessionID sessionID) throws quickfix.UnsupportedMessageType {</xsl:text>
		<xsl:value-of select="$NEW_LINE_INDENT2" />
		<xsl:text>throw new quickfix.UnsupportedMessageType();</xsl:text>
		<xsl:value-of select="$NEW_LINE_INDENT" />
		<xsl:text>}</xsl:text>

		<xsl:value-of select="$NEW_LINE" />
		<xsl:text>}</xsl:text>

	</xsl:template>

	<xsl:template name="virtual-functions">
		<xsl:for-each select="fix:messages/fix:message">

			<!-- Very basic javadoc -->
			<xsl:value-of select="$NEW_LINE" />
			<xsl:value-of select="$NEW_LINE_INDENT" />
			<xsl:text>/**</xsl:text>
			<xsl:value-of select="$NEW_LINE_INDENT" />
			<xsl:text> * Callback for FIX {@link </xsl:text>
			<xsl:value-of select="@name" />
			<xsl:text>} message. Subclasses wishing to handle messages of this type should overload this method.</xsl:text>
			<xsl:value-of select="$NEW_LINE_INDENT" />
			<xsl:text> * </xsl:text>
			<xsl:value-of select="$NEW_LINE_INDENT" />
			<xsl:text> * @param message</xsl:text>
			<xsl:value-of select="$NEW_LINE_INDENT" />
			<xsl:text> * @param sessionID</xsl:text>
			<xsl:value-of select="$NEW_LINE_INDENT" />
			<xsl:text> * @throws quickfix.UnsupportedMessageType</xsl:text>
			<xsl:value-of select="$NEW_LINE_INDENT" />
			<xsl:text> **/</xsl:text>

			<!-- The method -->
			<xsl:value-of select="$NEW_LINE_INDENT" />
			<xsl:text>public void onMessage(</xsl:text>
			<xsl:value-of select="@name" />
			<xsl:text> message, quickfix.SessionID sessionID) throws quickfix.UnsupportedMessageType {</xsl:text>

			<xsl:value-of select="$NEW_LINE_INDENT2" />
			<xsl:choose>
				<xsl:when test="@msgcat='admin'">
					<xsl:text>// no-op</xsl:text>
				</xsl:when>
				<xsl:otherwise>
					<xsl:text>throw new quickfix.UnsupportedMessageType();</xsl:text>
				</xsl:otherwise>
			</xsl:choose>

			<xsl:value-of select="$NEW_LINE_INDENT" />
			<xsl:text>}</xsl:text>
		</xsl:for-each>
	</xsl:template>

	<xsl:template name="switch-statement">

		<!-- Very basic javadoc -->
		<xsl:value-of select="$NEW_LINE" />
		<xsl:value-of select="$NEW_LINE_INDENT" />
		<xsl:text>/**</xsl:text>
		<xsl:value-of select="$NEW_LINE_INDENT" />
		<xsl:text> * Entry point for cracking {@link org.quickfixj.FIXMessage} instances.</xsl:text>
		<xsl:value-of select="$NEW_LINE_INDENT" />
		<xsl:text> * </xsl:text>
		<xsl:value-of select="$NEW_LINE_INDENT" />
		<xsl:text> * @param message</xsl:text>
		<xsl:value-of select="$NEW_LINE_INDENT" />
		<xsl:text> * @param sessionID</xsl:text>
		<xsl:value-of select="$NEW_LINE_INDENT" />
		<xsl:text> * @throws quickfix.UnsupportedMessageType</xsl:text>
		<xsl:value-of select="$NEW_LINE_INDENT" />
		<xsl:text> **/</xsl:text>

		<xsl:value-of select="$NEW_LINE_INDENT" />
		<xsl:text>public void crack(org.quickfixj.FIXMessage message, quickfix.SessionID sessionID) throws quickfix.UnsupportedMessageType {</xsl:text>
		<xsl:value-of select="$NEW_LINE" />
		<xsl:value-of select="$NEW_LINE_INDENT2" />
		<xsl:text>String type = message.getMsgType();</xsl:text>

		<xsl:value-of select="$NEW_LINE" />
		<xsl:value-of select="$NEW_LINE_INDENT2" />
		<xsl:for-each select="fix:messages/fix:message">
			<xsl:if test="position()!=1">
				<xsl:text> else </xsl:text>
			</xsl:if>

			<xsl:value-of select="concat('if (type.equals(', @name, '.MSGTYPE)) {')" />
			<xsl:value-of select="$NEW_LINE_INDENT3" />
			<xsl:value-of select="concat('onMessage((', @name, ')message, sessionID);')" />
			<xsl:value-of select="$NEW_LINE_INDENT2" />
			<xsl:text>}</xsl:text>
		</xsl:for-each>
		<xsl:text> else {</xsl:text>
		<xsl:value-of select="$NEW_LINE_INDENT3" />
		<xsl:text>onMessage(message, sessionID);</xsl:text>
		<xsl:value-of select="$NEW_LINE_INDENT2" />
		<xsl:text>}</xsl:text>
		<xsl:value-of select="$NEW_LINE_INDENT" />
		<xsl:text>}</xsl:text>
	</xsl:template>

</xsl:stylesheet>
