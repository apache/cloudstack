<!--
 Licensed to the Apache Software Foundation (ASF) under one or more
 contributor license agreements.  See the NOTICE file distributed with
 this work for additional information regarding copyright ownership.
 The ASF licenses this file to You under the Apache License, Version 2.0
 (the "License"); you may not use this file except in compliance with
 the License.  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
-->
<xsl:transform xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
               xmlns:log4j="http://jakarta.apache.org/log4j/"
               xsl:version="1.0">

   <xsl:output method="xml" indent="yes" encoding="US-ASCII"/>

   <xsl:apply-templates select="/"/>

   <xsl:template match="/">
       <xsl:apply-templates/>
   </xsl:template>

    <xsl:template match="log4j:event">
        <log4j:event logger="{@logger}" time="{@time}" timestamp="{@timestamp}"
                     level="{@level}" thread="{@thread}">
            <xsl:apply-templates/>
        </log4j:event>
    </xsl:template>

    <xsl:template match="log4j:message">
        <log4j:message>
            <xsl:apply-templates/>
        </log4j:message>
    </xsl:template>

    <xsl:template match="log4j:NDC">
        <log4j:NDC>
            <xsl:apply-templates/>
        </log4j:NDC>
    </xsl:template>

    <xsl:template match="log4j:throwable">
        <log4j:throwable>
            <xsl:apply-templates/>
        </log4j:throwable>
    </xsl:template>

    <xsl:template match="log4j:locationInfo">
        <log4j:locationInfo class="{@class}" method="{@method}"
                            file="{@file}"  line="{@line}"/>
    </xsl:template>

    <xsl:template match="log4j:properties">
        <log4j:properties>
            <xsl:apply-templates/>
        </log4j:properties>
    </xsl:template>

    <xsl:template match="log4j:data">
        <log4j:data name="{@name}" value="{@value}"/>
    </xsl:template>

    <xsl:template match="*">
        <xsl:element name="{name()}" namespace="{namespace-uri()}">
            <xsl:apply-templates select="@*"/>
            <xsl:apply-templates select="*|text()"/>
        </xsl:element>
    </xsl:template>

    <xsl:template match="@*">
        <xsl:attribute name="{name()}" namespace="{namespace-uri()}">
            <xsl:value-of select="."/>
        </xsl:attribute>
    </xsl:template>
    
    <xsl:template match="text()">
        <xsl:value-of select="."/>
    </xsl:template>

</xsl:transform>
