<?xml version="1.0"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
version="1.0">
<xsl:template match="/">
<html>
<head><title>Cloudstack API</title></head>
<body>
<table border="1">
<tr>
<th>Name</th>
<th>Description</th>
<th>Request Parameters</th>
<th>Response Parameters</th>
</tr>
<xsl:for-each select="command/command">
<tr>
<td><xsl:value-of select="name"/></td>
<td><xsl:value-of select="description"/></td>
<td>
<xsl:for-each select="./request/arg">
<br><b>Name:</b><xsl:value-of select="name"/></br>
<b>Description:</b><xsl:value-of select="description"/>
<br><b>Required:</b><xsl:value-of select="required"/></br>
</xsl:for-each>
</td>
<td>
<xsl:for-each select="./response/arg">
<br><b>Name:</b><xsl:value-of select="name"/></br>
<b>Description:</b><xsl:value-of select="description"/>
<xsl:for-each select="./arguments/arg">
<br><b>Name:</b><xsl:value-of select="name"/></br>
<b>Description:</b><xsl:value-of select="description"/>
<xsl:for-each select="./arguments/arg">
<br><b>Name:</b><xsl:value-of select="name"/></br>
<b>Description:</b><xsl:value-of select="description"/>
</xsl:for-each>
</xsl:for-each>
<br></br>
</xsl:for-each>
</td>
</tr>
</xsl:for-each>
</table>
</body></html>
</xsl:template>
</xsl:stylesheet>

