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
package com.cloud.agent.api.to.deployasis;

/**
 * container for the network prerequisites as found in the appliance template
 *
 * for OVA:
 * {code}
 * <Network ovf:name="Management0-0">
 *   <Description>Management Network Interface</Description>
 * </Network>
 * {code}
 * {code}
 * <Item>
 *   <rasd:AddressOnParent>7</rasd:AddressOnParent>
 *   <rasd:AutomaticAllocation>true</rasd:AutomaticAllocation>
 *   <rasd:Connection>Management0-0</rasd:Connection>
 *   <rasd:Description>E1000 Ethernet adapter on "Management Network"</rasd:Description>
 *   <rasd:ElementName>Network adapter 1</rasd:ElementName>
 *   <rasd:InstanceID>6</rasd:InstanceID>
 *   <rasd:ResourceSubType>E1000</rasd:ResourceSubType>
 *   <rasd:ResourceType>10</rasd:ResourceType>
 * </Item>
 * {code}
 */
public class OVFNetworkTO implements TemplateDeployAsIsInformationTO {
    String name;
    String networkDescription;

    int addressOnParent;
    boolean automaticAllocation;
    String nicDescription;
    String elementName;
    int InstanceID;
    String resourceSubType;
    String resourceType;

    public int getAddressOnParent() {
        return addressOnParent;
    }

    public void setAddressOnParent(int addressOnParent) {
        this.addressOnParent = addressOnParent;
    }

    public boolean isAutomaticAllocation() {
        return automaticAllocation;
    }

    public void setAutomaticAllocation(boolean automaticAllocation) {
        this.automaticAllocation = automaticAllocation;
    }

    public String getNicDescription() {
        return nicDescription;
    }

    public void setNicDescription(String nicDescription) {
        this.nicDescription = nicDescription;
    }

    public String getElementName() {
        return elementName;
    }

    public void setElementName(String elementName) {
        this.elementName = elementName;
    }

    public int getInstanceID() {
        return InstanceID;
    }

    public void setInstanceID(int instanceID) {
        InstanceID = instanceID;
    }

    public String getResourceSubType() {
        return resourceSubType;
    }

    public void setResourceSubType(String resourceSubType) {
        this.resourceSubType = resourceSubType;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNetworkDescription() {
        return networkDescription;
    }

    public void setNetworkDescription(String networkDescription) {
        this.networkDescription = networkDescription;
    }
}
