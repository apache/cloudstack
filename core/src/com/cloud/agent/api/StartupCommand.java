//
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
//

package com.cloud.agent.api;

import com.cloud.host.Host;

public class StartupCommand extends Command {
    Host.Type type;
    String dataCenter;
    String pod;
    String cluster;
    String guid;
    String name;
    Long id;
    String version;
    String iqn;
    String publicIpAddress;
    String publicNetmask;
    String publicMacAddress;
    String privateIpAddress;
    String privateMacAddress;
    String privateNetmask;
    String storageIpAddress;
    String storageNetmask;
    String storageMacAddress;
    String storageIpAddressDeux;
    String storageMacAddressDeux;
    String storageNetmaskDeux;
    String agentTag;
    String resourceName;
    String gatewayIpAddress;

    public StartupCommand(Host.Type type) {
        this.type = type;
    }

    public StartupCommand(Long id, Host.Type type, String name, String dataCenter, String pod, String guid, String version) {
        super();
        this.id = id;
        this.dataCenter = dataCenter;
        this.pod = pod;
        this.guid = guid;
        this.name = name;
        this.version = version;
        this.type = type;
    }

    public StartupCommand(Long id, Host.Type type, String name, String dataCenter, String pod, String guid, String version, String gatewayIpAddress) {
        this(id, type, name, dataCenter, pod, guid, version);
        this.gatewayIpAddress = gatewayIpAddress;
    }

    public Host.Type getHostType() {
        return type;
    }

    public void setHostType(Host.Type type) {
        this.type = type;
    }

    public String getIqn() {
        return iqn;
    }

    public void setCluster(String cluster) {
        this.cluster = cluster;
    }

    public String getCluster() {
        return cluster;
    }

    public void setIqn(String iqn) {
        this.iqn = iqn;
    }

    public String getDataCenter() {
        return dataCenter;
    }

    public String getPod() {
        return pod;
    }

    public Long getId() {
        return id;
    }

    public String getStorageIpAddressDeux() {
        return storageIpAddressDeux;
    }

    public void setStorageIpAddressDeux(String storageIpAddressDeux) {
        this.storageIpAddressDeux = storageIpAddressDeux;
    }

    public String getStorageMacAddressDeux() {
        return storageMacAddressDeux;
    }

    public void setStorageMacAddressDeux(String storageMacAddressDeux) {
        this.storageMacAddressDeux = storageMacAddressDeux;
    }

    public String getStorageNetmaskDeux() {
        return storageNetmaskDeux;
    }

    public void setStorageNetmaskDeux(String storageNetmaskDeux) {
        this.storageNetmaskDeux = storageNetmaskDeux;
    }

    public String getGuid() {
        return guid;
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    public void setDataCenter(String dataCenter) {
        this.dataCenter = dataCenter;
    }

    public void setPod(String pod) {
        this.pod = pod;
    }

    public void setGuid(String guid) {
        this.guid = guid;
    }

    public void setGuid(String guid, String resourceName) {
        this.resourceName = resourceName;
        this.guid = guid + "-" + resourceName;
    }

    public String getPublicNetmask() {
        return publicNetmask;
    }

    public void setPublicNetmask(String publicNetmask) {
        this.publicNetmask = publicNetmask;
    }

    public String getPublicMacAddress() {
        return publicMacAddress;
    }

    public void setPublicMacAddress(String publicMacAddress) {
        this.publicMacAddress = publicMacAddress;
    }

    public String getPrivateIpAddress() {
        return privateIpAddress;
    }

    public void setPrivateIpAddress(String privateIpAddress) {
        this.privateIpAddress = privateIpAddress;
    }

    public String getPrivateMacAddress() {
        return privateMacAddress;
    }

    public void setPrivateMacAddress(String privateMacAddress) {
        this.privateMacAddress = privateMacAddress;
    }

    public String getPrivateNetmask() {
        return privateNetmask;
    }

    public void setPrivateNetmask(String privateNetmask) {
        this.privateNetmask = privateNetmask;
    }

    public String getStorageIpAddress() {
        return storageIpAddress;
    }

    public void setStorageIpAddress(String storageIpAddress) {
        this.storageIpAddress = storageIpAddress;
    }

    public String getStorageNetmask() {
        return storageNetmask;
    }

    public void setStorageNetmask(String storageNetmask) {
        this.storageNetmask = storageNetmask;
    }

    public String getStorageMacAddress() {
        return storageMacAddress;
    }

    public void setStorageMacAddress(String storageMacAddress) {
        this.storageMacAddress = storageMacAddress;
    }

    public String getPublicIpAddress() {
        return publicIpAddress;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public void setPublicIpAddress(String publicIpAddress) {
        this.publicIpAddress = publicIpAddress;
    }

    public String getAgentTag() {
        return agentTag;
    }

    public void setAgentTag(String tag) {
        agentTag = tag;
    }

    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }

    public String getGuidWithoutResource() {
        if (resourceName == null) {
            return guid;
        } else {
            int hyph = guid.lastIndexOf('-');
            if (hyph == -1) {
                return guid;
            }
            String tmpResource = guid.substring(hyph + 1, guid.length());
            if (resourceName.equals(tmpResource)) {
                return guid.substring(0, hyph);
            } else {
                return guid;
            }
        }
    }

    public String getResourceName() {
        return resourceName;
    }

    public String getGatewayIpAddress() {
        return gatewayIpAddress;
    }

    public void setGatewayIpAddress(String gatewayIpAddress) {
        this.gatewayIpAddress = gatewayIpAddress;
    }

    @Override
    public boolean executeInSequence() {
        return false;
    }
}
