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
package org.apache.cloudstack.storage.datastore;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.apache.cloudstack.engine.datacenter.entity.api.DataCenterResourceEntity.State;
import org.apache.cloudstack.engine.subsystem.api.storage.ClusterScope;
import org.apache.cloudstack.engine.subsystem.api.storage.DataObject;
import org.apache.cloudstack.engine.subsystem.api.storage.DataObjectType;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreDriver;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreRole;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStoreLifeCycle;
import org.apache.cloudstack.engine.subsystem.api.storage.Scope;
import org.apache.cloudstack.engine.subsystem.api.storage.ScopeType;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.ZoneScope;
import org.apache.cloudstack.engine.subsystem.api.storage.disktype.DiskFormat;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreVO;
import org.apache.cloudstack.storage.datastore.provider.DataStoreProvider;
import org.apache.cloudstack.storage.db.ObjectInDataStoreVO;
import org.apache.cloudstack.storage.image.ImageDataFactory;
import org.apache.cloudstack.storage.image.TemplateInfo;
import org.apache.cloudstack.storage.snapshot.SnapshotDataFactory;
import org.apache.cloudstack.storage.snapshot.SnapshotInfo;
import org.apache.cloudstack.storage.volume.PrimaryDataStoreDriver;
import org.apache.cloudstack.storage.volume.VolumeObject;
import org.apache.cloudstack.storage.volume.db.VolumeDao2;
import org.apache.cloudstack.storage.volume.db.VolumeVO;
import org.apache.log4j.Logger;

import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.utils.component.ComponentContext;
import com.cloud.utils.storage.encoding.EncodingType;

public class DefaultPrimaryDataStore implements PrimaryDataStore {
    private static final Logger s_logger = Logger
            .getLogger(DefaultPrimaryDataStore.class);
    protected PrimaryDataStoreDriver driver;
    protected PrimaryDataStoreVO pdsv;
    @Inject
    protected PrimaryDataStoreDao dataStoreDao;
    protected PrimaryDataStoreLifeCycle lifeCycle;
    @Inject
    private ObjectInDataStoreManager objectInStoreMgr;
    @Inject
    ImageDataFactory imageDataFactory;
    @Inject
    SnapshotDataFactory snapshotFactory;
    protected DataStoreProvider provider;

    @Inject
    private VolumeDao2 volumeDao;

    protected DefaultPrimaryDataStore() {
       
    }
    
    public void configure(PrimaryDataStoreVO pdsv,
            PrimaryDataStoreDriver driver, DataStoreProvider provider) {
        this.pdsv = pdsv;
        this.driver = driver;
        this.provider = provider;
    }

    public static DefaultPrimaryDataStore createDataStore(
            PrimaryDataStoreVO pdsv, PrimaryDataStoreDriver driver,
            DataStoreProvider provider) {
        DefaultPrimaryDataStore dataStore = (DefaultPrimaryDataStore)ComponentContext.inject(DefaultPrimaryDataStore.class);
        dataStore.configure(pdsv, driver, provider);
        return dataStore;
    }

    @Override
    public VolumeInfo getVolume(long id) {
        VolumeVO volumeVO = volumeDao.findById(id);
        VolumeObject vol = VolumeObject.getVolumeObject(this, volumeVO);
        return vol;
    }

    @Override
    public List<VolumeInfo> getVolumes() {
        List<VolumeVO> volumes = volumeDao.findByPoolId(this.getId());
        List<VolumeInfo> volumeInfos = new ArrayList<VolumeInfo>();
        for (VolumeVO volume : volumes) {
            volumeInfos.add(VolumeObject.getVolumeObject(this, volume));
        }
        return volumeInfos;
    }

    @Override
    public DataStoreDriver getDriver() {
        // TODO Auto-generated method stub
        return this.driver;
    }

    @Override
    public DataStoreRole getRole() {
        // TODO Auto-generated method stub
        return DataStoreRole.Primary;
    }

    @Override
    public long getId() {
        // TODO Auto-generated method stub
        return this.pdsv.getId();
    }

    @Override
    public String getUri() {
        String path = this.pdsv.getPath();
        path.replaceFirst("/*", "");
        StringBuilder builder = new StringBuilder();
        builder.append(this.pdsv.getPoolType());
        builder.append("://");
        builder.append(this.pdsv.getHostAddress());
        builder.append(File.separator);
        builder.append(this.pdsv.getPath());
        builder.append(File.separator);
        builder.append("?" + EncodingType.ROLE + "=" + this.getRole());
        builder.append("&" + EncodingType.STOREUUID + "=" + this.pdsv.getUuid());
        return builder.toString();
    }

    @Override
    public Scope getScope() {
        PrimaryDataStoreVO vo = dataStoreDao.findById(this.pdsv.getId());
        if (vo.getScope() == ScopeType.CLUSTER) {
            return new ClusterScope(vo.getClusterId(), vo.getPodId(),
                    vo.getDataCenterId());
        } else if (vo.getScope() == ScopeType.ZONE) {
            return new ZoneScope(vo.getDataCenterId());
        }
        return null;
    }

    @Override
    public boolean isHypervisorSupported(HypervisorType hypervisor) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isLocalStorageSupported() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isVolumeDiskTypeSupported(DiskFormat diskType) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public long getCapacity() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public long getAvailableCapacity() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public String getUuid() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public State getManagedState() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getName() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getType() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public PrimaryDataStoreLifeCycle getLifeCycle() {
        return this.lifeCycle;
    }

    @Override
    public boolean exists(DataObject data) {
        return (objectInStoreMgr.findObject(data.getId(), data.getType(),
                this.getId(), this.getRole()) != null) ? true : false;
    }

    @Override
    public TemplateInfo getTemplate(long templateId) {
        ObjectInDataStoreVO obj = objectInStoreMgr.findObject(templateId, DataObjectType.TEMPLATE, this.getId(), this.getRole());
        if (obj == null) {
            return null;
        }
        return imageDataFactory.getTemplate(templateId, this);
    }

    @Override
    public SnapshotInfo getSnapshot(long snapshotId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public DiskFormat getDefaultDiskType() {
        // TODO Auto-generated method stub
        return null;
    }
}
