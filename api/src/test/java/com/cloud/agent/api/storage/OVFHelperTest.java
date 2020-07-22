// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package com.cloud.agent.api.storage;

import org.junit.Assert;
import org.junit.Test;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.List;

public class OVFHelperTest {

    private String ovfFileProductSection =
        "<ProductSection>" +
            "<Info>VM Arguments</Info>" +
            "<Property ovf:key=\"va-ssh-public-key\" ovf:type=\"string\" ovf:userConfigurable=\"true\" ovf:value=\"\">" +
                "<Label>Set the SSH public key allowed to access the appliance</Label>" +
                "<Description>This will enable the SSHD service and configure the specified public key</Description>" +
            "</Property>" +
            "<Property ovf:key=\"user-data\" ovf:type=\"string\" ovf:userConfigurable=\"true\" ovf:value=\"\">" +
                "<Label>User data to be made available inside the instance</Label>" +
                "<Description>This allows to pass any text to the appliance. The value should be encoded in base64</Description>" +
            "</Property>" +
        "</ProductSection>";

    private String ovfFileDeploymentOptionsSection =
            "<DeploymentOptionSection>\n" +
            "    <Info>Deployment Configuration information</Info>\n" +
            "    <Configuration ovf:id=\"ASAv5\">\n" +
            "      <Label>100 Mbps (ASAv5)</Label>\n" +
            "      <Description>Use this option to deploy an ASAv with a maximum throughput of 100 Mbps (uses 1 vCPU and 2 GB of memory).</Description>\n" +
            "    </Configuration>\n" +
            "    <Configuration ovf:id=\"ASAv10\">\n" +
            "      <Label>1 Gbps (ASAv10)</Label>\n" +
            "      <Description>Use this option to deploy an ASAv with a maximum throughput of 1 Gbps (uses 1 vCPU and 2 GB of memory).</Description>\n" +
            "    </Configuration>\n" +
            "    <Configuration ovf:id=\"ASAv30\">\n" +
            "      <Label>2 Gbps (ASAv30)</Label>\n" +
            "      <Description>Use this option to deploy an ASAv with a maximum throughput of 2 Gbps (uses 4 vCPUs and 8 GB of memory).</Description>\n" +
            "    </Configuration>\n" +
            "  </DeploymentOptionSection>";

    private String ovfFileVirtualHardwareSection =
            "<VirtualHardwareSection ovf:transport=\"iso\">\n" +
        "      <Info>Virtual hardware requirements</Info>\n" +
        "      <System>\n" +
        "        <vssd:ElementName>Virtual Hardware Family</vssd:ElementName>\n" +
        "        <vssd:InstanceID>0</vssd:InstanceID>\n" +
        "        <vssd:VirtualSystemIdentifier>ASAv</vssd:VirtualSystemIdentifier>\n" +
        "        <vssd:VirtualSystemType>vmx-08,vmx-09</vssd:VirtualSystemType>\n" +
        "      </System>\n" +
        "      <Item ovf:configuration=\"ASAv5 ASAv10\">\n" +
        "        <rasd:AllocationUnits>hertz * 10^6</rasd:AllocationUnits>\n" +
        "        <rasd:Description>Number of Virtual CPUs</rasd:Description>\n" +
        "        <rasd:ElementName>1 virtual CPU(s)</rasd:ElementName>\n" +
        "        <rasd:InstanceID>1</rasd:InstanceID>\n" +
        "        <rasd:Limit>5000</rasd:Limit>\n" +
        "        <rasd:Reservation>1000</rasd:Reservation>\n" +
        "        <rasd:ResourceType>3</rasd:ResourceType>\n" +
        "        <rasd:VirtualQuantity>1</rasd:VirtualQuantity>\n" +
        "      </Item>\n" +
        "      <Item ovf:configuration=\"ASAv30\">\n" +
        "        <rasd:AllocationUnits>hertz * 10^6</rasd:AllocationUnits>\n" +
        "        <rasd:Description>Number of Virtual CPUs</rasd:Description>\n" +
        "        <rasd:ElementName>4 virtual CPU(s)</rasd:ElementName>\n" +
        "        <rasd:InstanceID>1</rasd:InstanceID>\n" +
        "        <rasd:Limit>20000</rasd:Limit>\n" +
        "        <rasd:Reservation>1000</rasd:Reservation>\n" +
        "        <rasd:ResourceType>3</rasd:ResourceType>\n" +
        "        <rasd:VirtualQuantity>4</rasd:VirtualQuantity>\n" +
        "      </Item>\n" +
        "      <Item ovf:configuration=\"ASAv5 ASAv10\">\n" +
        "        <rasd:AllocationUnits>byte * 2^20</rasd:AllocationUnits>\n" +
        "        <rasd:Description>Memory Size</rasd:Description>\n" +
        "        <rasd:ElementName>2048MB of memory</rasd:ElementName>\n" +
        "        <rasd:InstanceID>2</rasd:InstanceID>\n" +
        "        <rasd:Limit>2048</rasd:Limit>\n" +
        "        <rasd:Reservation>2048</rasd:Reservation>\n" +
        "        <rasd:ResourceType>4</rasd:ResourceType>\n" +
        "        <rasd:VirtualQuantity>2048</rasd:VirtualQuantity>\n" +
        "      </Item>\n" +
        "      <Item ovf:configuration=\"ASAv30\">\n" +
        "        <rasd:AllocationUnits>byte * 2^20</rasd:AllocationUnits>\n" +
        "        <rasd:Description>Memory Size</rasd:Description>\n" +
        "        <rasd:ElementName>8192MB of memory</rasd:ElementName>\n" +
        "        <rasd:InstanceID>2</rasd:InstanceID>\n" +
        "        <rasd:Limit>8192</rasd:Limit>\n" +
        "        <rasd:Reservation>8192</rasd:Reservation>\n" +
        "        <rasd:ResourceType>4</rasd:ResourceType>\n" +
        "        <rasd:VirtualQuantity>8192</rasd:VirtualQuantity>\n" +
        "      </Item>\n" +
        "      <Item>\n" +
        "        <rasd:Address>0</rasd:Address>\n" +
        "        <rasd:Description>SCSI Controller</rasd:Description>\n" +
        "        <rasd:ElementName>SCSI controller 0</rasd:ElementName>\n" +
        "        <rasd:InstanceID>3</rasd:InstanceID>\n" +
        "        <rasd:ResourceSubType>lsilogic</rasd:ResourceSubType>\n" +
        "        <rasd:ResourceType>6</rasd:ResourceType>\n" +
        "      </Item>\n" +
        "      <Item>\n" +
        "        <rasd:Address>0</rasd:Address>\n" +
        "        <rasd:Description>IDE Controller</rasd:Description>\n" +
        "        <rasd:ElementName>IDE 0</rasd:ElementName>\n" +
        "        <rasd:InstanceID>4</rasd:InstanceID>\n" +
        "        <rasd:ResourceType>5</rasd:ResourceType>\n" +
        "      </Item>\n" +
        "      <Item ovf:required=\"false\">\n" +
        "        <rasd:AddressOnParent>0</rasd:AddressOnParent>\n" +
        "        <rasd:AutomaticAllocation>true</rasd:AutomaticAllocation>\n" +
        "        <rasd:ElementName>CD/DVD Drive</rasd:ElementName>\n" +
        "        <rasd:InstanceID>5</rasd:InstanceID>\n" +
        "        <rasd:Parent>4</rasd:Parent>\n" +
        "        <rasd:ResourceType>15</rasd:ResourceType>\n" +
        "      </Item>\n" +
        "      <Item ovf:required=\"false\">\n" +
        "        <rasd:AddressOnParent>1</rasd:AddressOnParent>\n" +
        "        <rasd:AutomaticAllocation>true</rasd:AutomaticAllocation>\n" +
        "        <rasd:ElementName>CD/DVD Drive</rasd:ElementName>\n" +
        "        <rasd:HostResource>ovf:/file/file3</rasd:HostResource>\n" +
        "        <rasd:InstanceID>18</rasd:InstanceID>\n" +
        "        <rasd:Parent>4</rasd:Parent>\n" +
        "        <rasd:ResourceType>15</rasd:ResourceType>\n" +
        "      </Item>\n" +
        "      <Item>\n" +
        "        <rasd:AddressOnParent>7</rasd:AddressOnParent>\n" +
        "        <rasd:AutomaticAllocation>true</rasd:AutomaticAllocation>\n" +
        "        <rasd:Connection>Management0-0</rasd:Connection>\n" +
        "        <rasd:Description>E1000 Ethernet adapter on \"Management Network\"</rasd:Description>\n" +
        "        <rasd:ElementName>Network adapter 1</rasd:ElementName>\n" +
        "        <rasd:InstanceID>6</rasd:InstanceID>\n" +
        "        <rasd:ResourceSubType>E1000</rasd:ResourceSubType>\n" +
        "        <rasd:ResourceType>10</rasd:ResourceType>\n" +
        "      </Item>\n" +
        "      <Item>\n" +
        "        <rasd:AddressOnParent>0</rasd:AddressOnParent>\n" +
        "        <rasd:ElementName>Hard disk 1</rasd:ElementName>\n" +
        "        <rasd:HostResource>ovf:/disk/vmdisk1</rasd:HostResource>\n" +
        "        <rasd:InstanceID>7</rasd:InstanceID>\n" +
        "        <rasd:Parent>3</rasd:Parent>\n" +
        "        <rasd:ResourceType>17</rasd:ResourceType>\n" +
        "      </Item>\n" +
        "      <Item>\n" +
        "        <rasd:AddressOnParent>1</rasd:AddressOnParent>\n" +
        "        <rasd:ElementName>Hard disk 2</rasd:ElementName>\n" +
        "        <rasd:HostResource>ovf:/disk/vmdisk2</rasd:HostResource>\n" +
        "        <rasd:InstanceID>8</rasd:InstanceID>\n" +
        "        <rasd:Parent>3</rasd:Parent>\n" +
        "        <rasd:ResourceType>17</rasd:ResourceType>\n" +
        "      </Item>\n" +
        "      <Item>\n" +
        "        <rasd:AddressOnParent>8</rasd:AddressOnParent>\n" +
        "        <rasd:AutomaticAllocation>true</rasd:AutomaticAllocation>\n" +
        "        <rasd:Connection>GigabitEthernet0-0</rasd:Connection>\n" +
        "        <rasd:Description>General purpose E1000 Ethernet adapter</rasd:Description>\n" +
        "        <rasd:ElementName>Network adapter 2</rasd:ElementName>\n" +
        "        <rasd:InstanceID>9</rasd:InstanceID>\n" +
        "        <rasd:ResourceSubType>E1000</rasd:ResourceSubType>\n" +
        "        <rasd:ResourceType>10</rasd:ResourceType>\n" +
        "      </Item>\n" +
        "      <Item>\n" +
        "        <rasd:AddressOnParent>9</rasd:AddressOnParent>\n" +
        "        <rasd:AutomaticAllocation>true</rasd:AutomaticAllocation>\n" +
        "        <rasd:Connection>GigabitEthernet0-1</rasd:Connection>\n" +
        "        <rasd:Description>General purpose E1000 Ethernet adapter</rasd:Description>\n" +
        "        <rasd:ElementName>Network adapter 3</rasd:ElementName>\n" +
        "        <rasd:InstanceID>10</rasd:InstanceID>\n" +
        "        <rasd:ResourceSubType>E1000</rasd:ResourceSubType>\n" +
        "        <rasd:ResourceType>10</rasd:ResourceType>\n" +
        "      </Item>\n" +
        "      <Item>\n" +
        "        <rasd:AddressOnParent>10</rasd:AddressOnParent>\n" +
        "        <rasd:AutomaticAllocation>true</rasd:AutomaticAllocation>\n" +
        "        <rasd:Connection>GigabitEthernet0-2</rasd:Connection>\n" +
        "        <rasd:Description>General purpose E1000 Ethernet adapter</rasd:Description>\n" +
        "        <rasd:ElementName>Network adapter 4</rasd:ElementName>\n" +
        "        <rasd:InstanceID>11</rasd:InstanceID>\n" +
        "        <rasd:ResourceSubType>E1000</rasd:ResourceSubType>\n" +
        "        <rasd:ResourceType>10</rasd:ResourceType>\n" +
        "      </Item>\n" +
        "      <Item>\n" +
        "        <rasd:AddressOnParent>11</rasd:AddressOnParent>\n" +
        "        <rasd:AutomaticAllocation>true</rasd:AutomaticAllocation>\n" +
        "        <rasd:Connection>GigabitEthernet0-3</rasd:Connection>\n" +
        "        <rasd:Description>General purpose E1000 Ethernet adapter</rasd:Description>\n" +
        "        <rasd:ElementName>Network adapter 5</rasd:ElementName>\n" +
        "        <rasd:InstanceID>12</rasd:InstanceID>\n" +
        "        <rasd:ResourceSubType>E1000</rasd:ResourceSubType>\n" +
        "        <rasd:ResourceType>10</rasd:ResourceType>\n" +
        "      </Item>\n" +
        "      <Item>\n" +
        "        <rasd:AddressOnParent>12</rasd:AddressOnParent>\n" +
        "        <rasd:AutomaticAllocation>true</rasd:AutomaticAllocation>\n" +
        "        <rasd:Connection>GigabitEthernet0-4</rasd:Connection>\n" +
        "        <rasd:Description>General purpose E1000 Ethernet adapter</rasd:Description>\n" +
        "        <rasd:ElementName>Network adapter 6</rasd:ElementName>\n" +
        "        <rasd:InstanceID>13</rasd:InstanceID>\n" +
        "        <rasd:ResourceSubType>E1000</rasd:ResourceSubType>\n" +
        "        <rasd:ResourceType>10</rasd:ResourceType>\n" +
        "      </Item>\n" +
        "      <Item>\n" +
        "        <rasd:AddressOnParent>13</rasd:AddressOnParent>\n" +
        "        <rasd:AutomaticAllocation>true</rasd:AutomaticAllocation>\n" +
        "        <rasd:Connection>GigabitEthernet0-5</rasd:Connection>\n" +
        "        <rasd:Description>General purpose E1000 Ethernet adapter</rasd:Description>\n" +
        "        <rasd:ElementName>Network adapter 7</rasd:ElementName>\n" +
        "        <rasd:InstanceID>14</rasd:InstanceID>\n" +
        "        <rasd:ResourceSubType>E1000</rasd:ResourceSubType>\n" +
        "        <rasd:ResourceType>10</rasd:ResourceType>\n" +
        "      </Item>\n" +
        "      <Item>\n" +
        "        <rasd:AddressOnParent>14</rasd:AddressOnParent>\n" +
        "        <rasd:AutomaticAllocation>true</rasd:AutomaticAllocation>\n" +
        "        <rasd:Connection>GigabitEthernet0-6</rasd:Connection>\n" +
        "        <rasd:Description>General purpose E1000 Ethernet adapter</rasd:Description>\n" +
        "        <rasd:ElementName>Network adapter 8</rasd:ElementName>\n" +
        "        <rasd:InstanceID>15</rasd:InstanceID>\n" +
        "        <rasd:ResourceSubType>E1000</rasd:ResourceSubType>\n" +
        "        <rasd:ResourceType>10</rasd:ResourceType>\n" +
        "      </Item>\n" +
        "      <Item>\n" +
        "        <rasd:AddressOnParent>15</rasd:AddressOnParent>\n" +
        "        <rasd:AutomaticAllocation>true</rasd:AutomaticAllocation>\n" +
        "        <rasd:Connection>GigabitEthernet0-7</rasd:Connection>\n" +
        "        <rasd:Description>General purpose E1000 Ethernet adapter</rasd:Description>\n" +
        "        <rasd:ElementName>Network adapter 9</rasd:ElementName>\n" +
        "        <rasd:InstanceID>16</rasd:InstanceID>\n" +
        "        <rasd:ResourceSubType>E1000</rasd:ResourceSubType>\n" +
        "        <rasd:ResourceType>10</rasd:ResourceType>\n" +
        "      </Item>\n" +
        "      <Item>\n" +
        "        <rasd:AddressOnParent>16</rasd:AddressOnParent>\n" +
        "        <rasd:AutomaticAllocation>true</rasd:AutomaticAllocation>\n" +
        "        <rasd:Connection>GigabitEthernet0-8</rasd:Connection>\n" +
        "        <rasd:Description>Default HA failover E1000 Ethernet adapter, or additional standalone general purpose adapter</rasd:Description>\n" +
        "        <rasd:ElementName>Network adapter 10</rasd:ElementName>\n" +
        "        <rasd:InstanceID>17</rasd:InstanceID>\n" +
        "        <rasd:ResourceSubType>E1000</rasd:ResourceSubType>\n" +
        "        <rasd:ResourceType>10</rasd:ResourceType>\n" +
        "      </Item>\n" +
        "      <vmw:ExtraConfig vmw:key=\"monitor_control.pseudo_perfctr\" vmw:value=\"TRUE\"></vmw:ExtraConfig>\n" +
        "    </VirtualHardwareSection>";

    private OVFHelper ovfHelper = new OVFHelper();

    @Test
    public void testGetOVFPropertiesValidOVF() throws IOException, SAXException, ParserConfigurationException {
        List<OVFPropertyTO> props = ovfHelper.getOVFPropertiesFromXmlString(ovfFileProductSection);
        Assert.assertEquals(2, props.size());
    }

    @Test(expected = SAXParseException.class)
    public void testGetOVFPropertiesInvalidOVF() throws IOException, SAXException, ParserConfigurationException {
        ovfHelper.getOVFPropertiesFromXmlString(ovfFileProductSection + "xxxxxxxxxxxxxxxxx");
    }

    @Test
    public void testGetOVFDeploymentOptionsValidOVF() throws IOException, SAXException, ParserConfigurationException {
        List<OVFConfigurationTO> options = ovfHelper.getOVFDeploymentOptionsFromXmlString(ovfFileDeploymentOptionsSection);
        Assert.assertEquals(3, options.size());
    }

    @Test
    public void testGetOVFVirtualHardwareSectionValidOVF() throws IOException, SAXException, ParserConfigurationException {
        List<OVFVirtualHardwareItemTO> items = ovfHelper.getOVFVirtualHardwareSectionFromXmlString(ovfFileVirtualHardwareSection);
        Assert.assertEquals(20, items.size());
    }
}
