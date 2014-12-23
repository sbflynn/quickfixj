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
	<xsl:param name="resource" />

	<xsl:template match="/">
		<xsl:value-of select="$messagePackage" />
		<xsl:text>.DictionaryService</xsl:text>
	</xsl:template>

</xsl:stylesheet>
