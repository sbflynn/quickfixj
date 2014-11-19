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

		<xsl:value-of select="$NEW_LINE" />
		<xsl:value-of select="concat('package ', $messagePackage,';')" />
		<xsl:value-of select="$NEW_LINE" />
		<xsl:value-of select="$NEW_LINE" />

		<xsl:text>import quickfix.*;</xsl:text>
		<xsl:value-of select="$NEW_LINE" />
		<xsl:value-of select="concat('import ', $fieldPackage,'.*;')" />
		<xsl:value-of select="$NEW_LINE" />

		public class MessageCracker {

		/**
		* Callback for quickfix.Message message.
		*
		*
		@param message
		* @param sessionID
		*
		* @throws FieldNotFound
		* @throws
		UnsupportedMessageType
		* @throws IncorrectTagValue
		*/
		public void
		onMessage(quickfix.Message message, SessionID sessionID) throws
		FieldNotFound,
		UnsupportedMessageType, IncorrectTagValue {
		throw new
		UnsupportedMessageType();
		}
		<xsl:call-template name="virtual-functions" />
		<xsl:call-template name="switch-statement" />
		}
	</xsl:template>

	<xsl:template name="virtual-functions">
		<xsl:for-each select="fix:messages/fix:message">
			/**
			* Callback for FIX
			<xsl:value-of select="@name" />
			message.
			*
			* @param message
			* @param sessionID
			*
			* @throws FieldNotFound
			* @throws
			UnsupportedMessageType
			* @throws IncorrectTagValue
			*/
			public void onMessage(
			<xsl:value-of select="@name" />
			message, SessionID sessionID) throws FieldNotFound, UnsupportedMessageType,
			IncorrectTagValue {
			<xsl:text />
			<xsl:choose>
				<xsl:when
					test="(@msgcat='app' or @msgcat='Common') and @name='BusinessMessageReject'"
				>
					}
				</xsl:when>
				<xsl:when test="@msgcat='admin'">
					}
				</xsl:when>
				<xsl:when test="@msgcat='Session'">
					}
				</xsl:when>
				<xsl:otherwise>
					throw new UnsupportedMessageType();
					}
				</xsl:otherwise>
			</xsl:choose>
		</xsl:for-each>
	</xsl:template>

	<xsl:template name="switch-statement">
		<xsl:value-of select="$NEW_LINE_INDENT" />
		public void crack(quickfix.Message message, SessionID sessionID)
		throws
		UnsupportedMessageType, FieldNotFound, IncorrectTagValue {
		<xsl:value-of select="$NEW_LINE_INDENT" />
		<xsl:value-of
			select="concat('crack', //fix:fix/@major, //fix:fix/@minor,'((Message) message, sessionID);')" />
		}

		/**
		* Cracker method for
		<xsl:value-of select="//fix:fix/@major" />
		<xsl:value-of select="//fix:fix/@minor" />
		messages.
		*
		* @throws FieldNotFound
		* @throws UnsupportedMessageType
		* @throws
		IncorrectTagValue
		*/
		<xsl:value-of select="$NEW_LINE_INDENT" />
		<xsl:value-of select="concat('public void crack', //fix:fix/@major, //fix:fix/@minor)" />
		<xsl:text>(Message message, SessionID sessionID) throws UnsupportedMessageType, FieldNotFound, IncorrectTagValue {</xsl:text>

		<xsl:value-of select="$NEW_LINE_INDENT" />
		<xsl:text>String type = message.getHeader().getString(MsgType.FIELD);</xsl:text>

		<xsl:for-each select="fix:messages/fix:message">
			<xsl:if test="position()!=1">
				else
			</xsl:if>

			<xsl:value-of select="$NEW_LINE_INDENT" />
			<xsl:value-of select="concat('if (type.equals(',@name,'.MSGTYPE)) {')" />
			<xsl:value-of select="$NEW_LINE_INDENT" />
			<xsl:value-of select="concat('onMessage((', @name, ')message, sessionID);')" />
			<xsl:value-of select="$NEW_LINE_INDENT" />
			<xsl:text>}</xsl:text>
		</xsl:for-each>
		else
		onMessage(message, sessionID);
		}
	</xsl:template>

	<xsl:template name="base-class">
		<xsl:if test="//fix:fix/@major='4'">
			<xsl:if test="//fix:fix/@minor='1'">
				extends quickfix.fix40.MessageCracker
			</xsl:if>
			<xsl:if test="//fix:fix/@minor='2'">
				extends quickfix.fix41.MessageCracker
			</xsl:if>
			<xsl:if test="//fix:fix/@minor='3'">
				extends quickfix.fix42.MessageCracker
			</xsl:if>
			<xsl:if test="//fix:fix/@minor='4'">
				extends quickfix.fix43.MessageCracker
			</xsl:if>
		</xsl:if>
		<xsl:if test="//fix:fix/@major='5'">
			<xsl:if test="//fix:fix/@minor='0'">
				extends quickfix.fix44.MessageCracker
			</xsl:if>
		</xsl:if>
		<xsl:if test="//fix:fix/@type='FIXT'">
			extends quickfix.fix50.MessageCracker
		</xsl:if>
	</xsl:template>

</xsl:stylesheet>
