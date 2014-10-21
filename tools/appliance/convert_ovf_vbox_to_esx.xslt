<?xml version="1.0" encoding="utf-8"?>
<!--
  Licensed to the Apache Software Foundation (ASF) under one
  or more contributor license agreements. See the NOTICE file
  distributed with this work for additional information
  regarding copyright ownership. The ASF licenses this file
  to you under the Apache License, Version 2.0 (the
  "License"); you may not use this file except in compliance
  with the License. You may obtain a copy of the License at
  
  http://www.apache.org/licenses/LICENSE-2.0
  
  Unless required by applicable law or agreed to in writing,
  software distributed under the License is distributed on an
  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
  KIND, either express or implied. See the License for the
  specific language governing permissions and limitations
  under the License.
-->
<xsl:stylesheet version="1.0" xmlns="http://schemas.dmtf.org/ovf/envelope/1" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:ovf="http://schemas.dmtf.org/ovf/envelope/1" xmlns:rasd="http://schemas.dmtf.org/wbem/wscim/1/cim-schema/2/CIM_ResourceAllocationSettingData" xmlns:vssd="http://schemas.dmtf.org/wbem/wscim/1/cim-schema/2/CIM_VirtualSystemSettingData" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:vbox="http://www.virtualbox.org/ovf/machine">

    <xsl:template match="vssd:VirtualSystemType/text()">vmx-07</xsl:template>
  
    <xsl:template match="ovf:Item[./rasd:ResourceType/text()=20]">
        <Item>
        <xsl:copy-of select="rasd:Address"/>
        <rasd:Caption>scsiController0</rasd:Caption>
        <rasd:Description>SCSI Controller</rasd:Description>
        <rasd:ElementName>scsiController0</rasd:ElementName>
        <xsl:copy-of select="rasd:InstanceID"/>
        <rasd:ResourceSubType>lsilogic</rasd:ResourceSubType>
        <rasd:ResourceType>6</rasd:ResourceType>
        </Item>
    </xsl:template>

    <xsl:template match="node()|@*">
        <xsl:copy>
            <xsl:apply-templates select="node()|@*"/>
        </xsl:copy>
    </xsl:template>
    
</xsl:stylesheet>
