<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:ovf="http://www.vmware.com/schema/ovf/1/envelope" xmlns:rasd="http://schemas.dmtf.org/wbem/wscim/1/cim-schema/2/CIM_ResourceAllocationSettingData" xmlns:vssd="http://schemas.dmtf.org/wbem/wscim/1/cim-schema/2/CIM_VirtualSystemSettingData" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:vbox="http://www.virtualbox.org/ovf/machine">

    <xsl:template match="node()|@*">
        <xsl:copy>
            <xsl:apply-templates select="node()|@*"/>
        </xsl:copy>
    </xsl:template>

    <xsl:template match="vssd:VirtualSystemType/text()">vmx-06</xsl:template>
  
    <xsl:template match="ovf:Item[./rasd:ResourceType/text()=20]">
        <ovf:Item>
        <xsl:copy-of select="rasd:Address"/>
        <xsl:copy-of select="rasd:BusNumber"/>
        <rasd:Caption>scsiController0</rasd:Caption>
        <rasd:Description>SCSI Controller</rasd:Description>
        <xsl:copy-of select="rasd:InstanceId"/>
        <rasd:ResourceSubType>lsilogic</rasd:ResourceSubType>
        <rasd:ResourceType>6</rasd:ResourceType>
        </ovf:Item>
    </xsl:template>

</xsl:stylesheet>
