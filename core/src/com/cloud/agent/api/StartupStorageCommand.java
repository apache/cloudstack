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

import java.util.HashMap;
import java.util.Map;

import com.cloud.host.Host;
import com.cloud.storage.Storage;
import com.cloud.storage.Storage.StoragePoolType;
import com.cloud.storage.template.TemplateProp;

public class StartupStorageCommand extends StartupCommand {

    String parent;
    Map<String, TemplateProp> templateInfo;
    long totalSize;
    StoragePoolInfo poolInfo;
    Storage.StorageResourceType resourceType;
    StoragePoolType fsType;
    Map<String, String> hostDetails = new HashMap<String, String>();
    String nfsShare;

    public StartupStorageCommand() {
        super(Host.Type.Storage);
    }

    public StartupStorageCommand(String parent, StoragePoolType fsType, long totalSize, Map<String, TemplateProp> info) {
        super(Host.Type.Storage);
        this.parent = parent;
        this.totalSize = totalSize;
        this.templateInfo = info;
        this.poolInfo = null;
        this.fsType = fsType;
    }

    public StartupStorageCommand(String parent, StoragePoolType fsType, Map<String, TemplateProp> templateInfo, StoragePoolInfo poolInfo) {
        super(Host.Type.Storage);
        this.parent = parent;
        this.templateInfo = templateInfo;
        this.totalSize = poolInfo.capacityBytes;
        this.poolInfo = poolInfo;
        this.fsType = fsType;
    }

    public String getParent() {
        return parent;
    }

    public void setParent(String parent) {
        this.parent = parent;
    }

    public void setNfsShare(String nfsShare) {
        this.nfsShare = nfsShare;
    }

    public String getNfsShare() {
        return nfsShare;
    }

    public long getTotalSize() {
        return totalSize;
    }

    public Map<String, TemplateProp> getTemplateInfo() {
        return templateInfo;
    }

    public void setTemplateInfo(Map<String, TemplateProp> templateInfo) {
        this.templateInfo = templateInfo;
    }

    public StoragePoolInfo getPoolInfo() {
        return poolInfo;
    }

    public void setPoolInfo(StoragePoolInfo poolInfo) {
        this.poolInfo = poolInfo;
    }

    public Storage.StorageResourceType getResourceType() {
        return resourceType;
    }

    public void setResourceType(Storage.StorageResourceType resourceType) {
        this.resourceType = resourceType;
    }

    /*For secondary storage*/
    public Map<String, String> getHostDetails() {
        return hostDetails;
    }
}
