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
package com.cloud.bridge.service.core.ec2;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import com.cloud.bridge.util.EC2RestAuth;

public class EC2Instance {

    private String id;
    private String name;
    private String zoneName;
    private String templateId;
    private String group;
    private String state;
    private String previousState;
    private String ipAddress;
    private String privateIpAddress;
    private String instanceType;
    private Calendar created;
    private String accountName;
    private String domainId;
    private String hypervisor;
    private String rootDeviceType;
    private String rootDeviceId;
    private String keyPairName;
    private List<EC2SecurityGroup> groupSet;
    private List<EC2TagKeyValue> tagsSet;

    public EC2Instance() {
        id = null;
        name = null;
        zoneName = null;
        templateId = null;
        group = null;
        state = null;
        previousState = null;
        ipAddress = null;
        privateIpAddress = null;
        created = null;
        instanceType = null;
        accountName = null;
        domainId = null;
        hypervisor = null;
        rootDeviceType = null;
        rootDeviceId = null;
        keyPairName = null;
        groupSet = new ArrayList<EC2SecurityGroup>();
        tagsSet = new ArrayList<EC2TagKeyValue>();
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return this.id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    public void setZoneName(String zoneName) {
        this.zoneName = zoneName;
    }

    public String getZoneName() {
        return this.zoneName;
    }

    public void setTemplateId(String templateId) {
        this.templateId = templateId;
    }

    public String getTemplateId() {
        return this.templateId;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getGroup() {
        return this.group;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getState() {
        return this.state;
    }

    public void setPreviousState(String state) {
        this.previousState = state;
    }

    public String getPreviousState() {
        return this.previousState;
    }

    public void setCreated(String created) {
        this.created = EC2RestAuth.parseDateString(created);
    }

    public Calendar getCreated() {
        return this.created;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getIpAddress() {
        return this.ipAddress;
    }

    public void setPrivateIpAddress(String ipAddress) {
        this.privateIpAddress = ipAddress;
    }

    public String getPrivateIpAddress() {
        return this.privateIpAddress;
    }

    public void setServiceOffering(String instanceType) {
        this.instanceType = instanceType;
    }

    public String getServiceOffering() {
        return this.instanceType;
    }

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public String getDomainId() {
        return domainId;
    }

    public void setDomainId(String domainId) {
        this.domainId = domainId;
    }

    public String getHypervisor() {
        return hypervisor;
    }

    public void setHypervisor(String param) {
        hypervisor = param;
    }

    public String getRootDeviceType() {
        return rootDeviceType;
    }

    public void setRootDeviceType(String param) {
        rootDeviceType = param;
    }

    public String getRootDeviceId() {
        return rootDeviceId;
    }

    public void setRootDeviceId(String param) {
        rootDeviceId = param;
    }

    public String getKeyPairName() {
        return keyPairName;
    }

    public void setKeyPairName(String param) {
        keyPairName = param;
    }

    public void addGroupName(EC2SecurityGroup param) {
        groupSet.add(param);
    }

    public EC2SecurityGroup[] getGroupSet() {
        return groupSet.toArray(new EC2SecurityGroup[0]);
    }

    public void addResourceTag(EC2TagKeyValue param) {
        tagsSet.add(param);
    }

    public EC2TagKeyValue[] getResourceTags() {
        return tagsSet.toArray(new EC2TagKeyValue[0]);
    }

}
