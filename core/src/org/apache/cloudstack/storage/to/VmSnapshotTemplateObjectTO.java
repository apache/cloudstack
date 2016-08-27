/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.cloudstack.storage.to;

import org.apache.cloudstack.engine.subsystem.api.storage.VmSnapshotTemplateInfo;

import com.cloud.agent.api.to.DataObjectType;
import com.cloud.agent.api.to.DataStoreTO;
import com.cloud.agent.api.to.DataTO;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.storage.Storage.ImageFormat;

public class VmSnapshotTemplateObjectTO implements DataTO {
    private String path;
    private String uuid;
    private long id;
    private ImageFormat format;
    private long accountId;
    private boolean hvm;
    private DataStoreTO dataStore;
    private String name;
    private Long size;

    public VmSnapshotTemplateObjectTO(VmSnapshotTemplateInfo o) {
        super();
        path = o.getInstallPath();
        uuid = o.getUuid();
        id = o.getId();
        format = ImageFormat.OVA;
        accountId = o.getAccountId();
        hvm = true;
        dataStore = o.getDataStore().getTO();
        name = o.getUniqueName();
        size = 0L;
        physicalSize = 0L;
        hypervisorType = HypervisorType.VMware;
    }

    public VmSnapshotTemplateObjectTO(String path, String uuid, long id, ImageFormat format, long accountId, boolean hvm, DataStoreTO dataStore, String name,
            Long size, Long physicalSize, HypervisorType hypervisorType) {
        super();
        this.path = path;
        this.uuid = uuid;
        this.id = id;
        this.format = format;
        this.accountId = accountId;
        this.hvm = hvm;
        this.dataStore = dataStore;
        this.name = name;
        this.size = size;
        this.physicalSize = physicalSize;
        this.hypervisorType = hypervisorType;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public ImageFormat getFormat() {
        return format;
    }

    public void setFormat(ImageFormat format) {
        this.format = format;
    }

    public long getAccountId() {
        return accountId;
    }

    public void setAccountId(long accountId) {
        this.accountId = accountId;
    }

    public boolean isHvm() {
        return hvm;
    }

    public void setHvm(boolean hvm) {
        this.hvm = hvm;
    }

    @Override
    public DataStoreTO getDataStore() {
        return dataStore;
    }

    public void setDataStore(DataStoreTO dataStore) {
        this.dataStore = dataStore;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public void setPath(String path) {
        this.path = path;
    }

    public void setId(long id) {
        this.id = id;
    }

    public void setHypervisorType(Hypervisor.HypervisorType hypervisorType) {
        this.hypervisorType = hypervisorType;
    }

    private Long physicalSize;
    private Hypervisor.HypervisorType hypervisorType;

    public VmSnapshotTemplateObjectTO(VmSnapshotTemplateObjectTO vmSnapshotTemplateObject) {

    }

    public VmSnapshotTemplateObjectTO() {
    }

    @Override
    public DataObjectType getObjectType() {
        return DataObjectType.VMSNAPSHOT_TEMPLATE;
    }

    @Override
    public HypervisorType getHypervisorType() {
        return hypervisorType;
    }

    @Override
    public String getPath() {
        return path;
    }

    @Override
    public long getId() {
        return id;
    }

}
