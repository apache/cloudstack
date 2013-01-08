<?xml version="1.0"?>
<!--
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
-->
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

