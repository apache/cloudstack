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

package org.apache.cloudstack.storage.to;

import org.apache.cloudstack.engine.subsystem.api.storage.TemplateInfo;

import com.cloud.agent.api.to.DataObjectType;
import com.cloud.agent.api.to.DataStoreTO;
import com.cloud.agent.api.to.DataTO;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.storage.Storage.ImageFormat;
import com.cloud.template.VirtualMachineTemplate;

public class TemplateObjectTO implements DataTO {
    private String path;
    private String origUrl;
    private String uuid;
    private long id;
    private ImageFormat format;
    private long accountId;
    private String checksum;
    private boolean hvm;
    private String displayText;
    private DataStoreTO imageDataStore;
    private String name;
    private String guestOsType;
    private Long size;
    private Long physicalSize;
    private Hypervisor.HypervisorType hypervisorType;
    private boolean bootable;
    private String uniqueName;
    private boolean directDownload;
    private boolean deployAsIs;
    private String deployAsIsConfiguration;

    public TemplateObjectTO() {

    }

    public TemplateObjectTO(VirtualMachineTemplate template) {
        this.uuid = template.getUuid();
        this.id = template.getId();
        this.origUrl = template.getUrl();
        this.displayText = template.getDisplayText();
        this.checksum = template.getChecksum();
        this.hvm = template.isRequiresHvm();
        this.accountId = template.getAccountId();
        this.name = template.getUniqueName();
        this.format = template.getFormat();
        this.hypervisorType = template.getHypervisorType();
    }

    public TemplateObjectTO(TemplateInfo template) {
        this.path = template.getInstallPath();
        this.uuid = template.getUuid();
        this.id = template.getId();
        this.origUrl = template.getUrl();
        this.displayText = template.getDisplayText();
        this.checksum = template.getChecksum();
        this.hvm = template.isRequiresHvm();
        this.accountId = template.getAccountId();
        this.name = template.getUniqueName();
        this.format = template.getFormat();
        this.uniqueName = template.getUniqueName();
        this.size = template.getSize();
        if (template.getDataStore() != null) {
            this.imageDataStore = template.getDataStore().getTO();
        }
        this.hypervisorType = template.getHypervisorType();
        this.deployAsIs = template.isDeployAsIs();
        this.deployAsIsConfiguration = template.getDeployAsIsConfiguration();
    }

    @Override
    public String getPath() {
        return this.path;
    }

    public String getUuid() {
        return this.uuid;
    }

    @Override
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public ImageFormat getFormat() {
        return format;
    }

    public long getAccountId() {
        return accountId;
    }

    public String getChecksum() {
        return checksum;
    }

    public boolean isRequiresHvm() {
        return hvm;
    }

    public void setRequiresHvm(boolean hvm) {
        this.hvm = hvm;
    }

    public String getDescription() {
        return displayText;
    }

    public void setDescription(String desc) {
        this.displayText = desc;
    }

    @Override
    public DataObjectType getObjectType() {
        return DataObjectType.TEMPLATE;
    }

    @Override
    public DataStoreTO getDataStore() {
        return this.imageDataStore;
    }

    public void setHypervisorType(Hypervisor.HypervisorType hypervisorType) {
        this.hypervisorType = hypervisorType;
    }

    @Override
    public Hypervisor.HypervisorType getHypervisorType() {
        return this.hypervisorType;
    }

    public void setDataStore(DataStoreTO store) {
        this.imageDataStore = store;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getOrigUrl() {
        return origUrl;
    }

    public void setOrigUrl(String origUrl) {
        this.origUrl = origUrl;
    }

    public void setFormat(ImageFormat format) {
        this.format = format;
    }

    public void setAccountId(long accountId) {
        this.accountId = accountId;
    }

    public void setChecksum(String checksum) {
        this.checksum = checksum;
    }

    public void setImageDataStore(DataStoreTO imageDataStore) {
        this.imageDataStore = imageDataStore;
    }

    public String getGuestOsType() {
        return guestOsType;
    }

    public void setGuestOsType(String guestOsType) {
        this.guestOsType = guestOsType;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public Long getPhysicalSize() {
        return physicalSize;
    }

    public void setPhysicalSize(Long physicalSize) {
        this.physicalSize = physicalSize;
    }

    public void setIsBootable(boolean bootable) {
        this.bootable = bootable;
    }

    public boolean isBootable() {
        return bootable;
    }

    public String getUniqueName() {
        return this.uniqueName;
    }

    public void setUniqueName(String uniqueName) {
        this.uniqueName = uniqueName;
    }

    public boolean isDirectDownload() {
        return directDownload;
    }

    public void setDirectDownload(boolean directDownload) {
        this.directDownload = directDownload;
    }

    public boolean isDeployAsIs() {
        return deployAsIs;
    }

    public String getDeployAsIsConfiguration() {
        return deployAsIsConfiguration;
    }

    public void setDeployAsIsConfiguration(String deployAsIsConfiguration) {
        this.deployAsIsConfiguration = deployAsIsConfiguration;
    }

    @Override
    public String toString() {
        return new StringBuilder("TemplateTO[id=").append(id).append("|origUrl=").append(origUrl).append("|name").append(name).append("]").toString();
    }
}
