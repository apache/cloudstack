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

import java.util.ArrayList;

import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.apache.commons.lang.ArrayUtils;

import com.cloud.agent.api.to.DataObjectType;
import com.cloud.agent.api.to.DataStoreTO;
import com.cloud.agent.api.to.DataTO;
import com.cloud.hypervisor.Hypervisor.HypervisorType;

public class SnapshotObjectTO extends DownloadableObjectTO implements DataTO {
    private String path;
    private VolumeObjectTO volume;
    private String parentSnapshotPath;
    private DataStoreTO dataStore;
    private String vmName;
    private String name;
    private HypervisorType hypervisorType;
    private long id;
    private boolean quiescevm;
    private String[] parents;
    private Long physicalSize = (long) 0;
    private long accountId;


    public SnapshotObjectTO() {

    }

    public SnapshotObjectTO(SnapshotInfo snapshot) {
        this.path = snapshot.getPath();
        this.setId(snapshot.getId());
        this.accountId = snapshot.getAccountId();
        VolumeInfo vol = snapshot.getBaseVolume();
        if (vol != null) {
            this.volume = (VolumeObjectTO)vol.getTO();
            this.setVmName(vol.getAttachedVmName());
        }

        SnapshotInfo parentSnapshot = snapshot.getParent();
        ArrayList<String> parentsArry = new ArrayList<String>();
        if (parentSnapshot != null) {
            this.parentSnapshotPath = parentSnapshot.getPath();
            while(parentSnapshot != null) {
                parentsArry.add(parentSnapshot.getPath());
                parentSnapshot = parentSnapshot.getParent();
            }
            parents =  parentsArry.toArray(new String[parentsArry.size()]);
            ArrayUtils.reverse(parents);
        }

        this.dataStore = snapshot.getDataStore().getTO();
        this.setName(snapshot.getName());
        this.hypervisorType = snapshot.getHypervisorType();
        this.quiescevm = false;
    }

    @Override
    public DataObjectType getObjectType() {
        return DataObjectType.SNAPSHOT;
    }

    @Override
    public DataStoreTO getDataStore() {
        return this.dataStore;
    }

    public void setDataStore(DataStoreTO store) {
        this.dataStore = store;
    }

    @Override
    public String getPath() {
        return this.path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Long getPhysicalSize() {
        return this.physicalSize;
    }

    public void setPhysicalSize(Long physicalSize ) {
        this.physicalSize = physicalSize;
    }

    public VolumeObjectTO getVolume() {
        return volume;
    }

    public void setVolume(VolumeObjectTO volume) {
        this.volume = volume;
    }

    public String getParentSnapshotPath() {
        return parentSnapshotPath;
    }

    public void setParentSnapshotPath(String parentSnapshotPath) {
        this.parentSnapshotPath = parentSnapshotPath;
    }

    public String getVmName() {
        return vmName;
    }

    public void setVmName(String vmName) {
        this.vmName = vmName;
    }

    @Override
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public HypervisorType getHypervisorType() {
        return hypervisorType;
    }

    public void setHypervisorType(HypervisorType hypervisorType) {
        this.hypervisorType = hypervisorType;
    }

    public boolean getquiescevm() {
        return this.quiescevm;
    }

    public void setQuiescevm(boolean quiescevm) {
        this.quiescevm = quiescevm;
    }

    public String[] getParents() {
        return parents;
    }

    public long getAccountId() {
        return accountId;
    }

    public void setAccountId(long accountId) {
        this.accountId = accountId;
    }

    @Override
    public String toString() {
        return new StringBuilder("SnapshotTO[datastore=").append(dataStore).append("|volume=").append(volume).append("|path").append(path).append("]").toString();
    }
}
