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
import java.util.Date;
import java.util.List;

import javax.inject.Inject;

import org.apache.cloudstack.engine.subsystem.api.storage.ClusterScope;
import org.apache.cloudstack.engine.subsystem.api.storage.DataObject;
import org.apache.cloudstack.engine.subsystem.api.storage.DataObjectType;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreDriver;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreProvider;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreRole;
import org.apache.cloudstack.engine.subsystem.api.storage.ImageDataFactory;
import org.apache.cloudstack.engine.subsystem.api.storage.ObjectInDataStoreStateMachine;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStoreDriver;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStoreLifeCycle;
import org.apache.cloudstack.engine.subsystem.api.storage.Scope;
import org.apache.cloudstack.engine.subsystem.api.storage.ScopeType;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotDataFactory;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.TemplateInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.ZoneScope;
import org.apache.cloudstack.engine.subsystem.api.storage.disktype.DiskFormat;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.cloudstack.storage.volume.VolumeObject;
import org.apache.log4j.Logger;

import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.storage.Storage.StoragePoolType;
import com.cloud.storage.StoragePoolStatus;
import com.cloud.storage.VMTemplateStoragePoolVO;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.VMTemplatePoolDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.utils.component.ComponentContext;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.storage.encoding.EncodingType;

public class DefaultPrimaryDataStore implements PrimaryDataStore {
    private static final Logger s_logger = Logger
            .getLogger(DefaultPrimaryDataStore.class);
    protected PrimaryDataStoreDriver driver;
    protected StoragePoolVO pdsv;
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
    VMTemplatePoolDao templatePoolDao;

    private VolumeDao volumeDao;

    public	 DefaultPrimaryDataStore() {				
       
    }
    
    public void configure(StoragePoolVO pdsv,
            PrimaryDataStoreDriver driver, DataStoreProvider provider) {
        this.pdsv = pdsv;
        this.driver = driver;
        this.provider = provider;
    }

    public static DefaultPrimaryDataStore createDataStore(
            StoragePoolVO pdsv, PrimaryDataStoreDriver driver,
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
        return this.driver;
    }

    @Override
    public DataStoreRole getRole() {
        return DataStoreRole.Primary;
    }

    @Override
    public long getId() {
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
        StoragePoolVO vo = dataStoreDao.findById(this.pdsv.getId());
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
        return true;
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
    public String getUuid() {
        return this.pdsv.getUuid();
    }

    @Override
    public String getName() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public PrimaryDataStoreLifeCycle getLifeCycle() {
        return this.lifeCycle;
    }

    @Override
    public boolean exists(DataObject data) {
        return (objectInStoreMgr.findObject(data, data.getDataStore()) != null) ? true : false;
    }

    @Override
    public TemplateInfo getTemplate(long templateId) {
        VMTemplateStoragePoolVO template = templatePoolDao.findByPoolTemplate(this.getId(), templateId);
        if (template == null || template.getState() != ObjectInDataStoreStateMachine.State.Ready) {
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

    @Override
    public DataObject create(DataObject obj) {
        //create template on primary storage
        if (obj.getType() == DataObjectType.TEMPLATE) {
            VMTemplateStoragePoolVO templateStoragePoolRef = templatePoolDao.findByPoolTemplate(this.getId(), obj.getId());
            if (templateStoragePoolRef == null) {
                try {
                templateStoragePoolRef = new VMTemplateStoragePoolVO(this.getId(), obj.getId());
                templateStoragePoolRef = templatePoolDao.persist(templateStoragePoolRef);
                } catch (Throwable t) {
                    templateStoragePoolRef = templatePoolDao.findByPoolTemplate(this.getId(), obj.getId());
                    if (templateStoragePoolRef == null) {
                        throw new CloudRuntimeException("Failed to create template storage pool entry");
                    }
                }
            }
            
        }
        
        return objectInStoreMgr.get(obj, this);
    }

    @Override
    public boolean delete(DataObject obj) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public long getDataCenterId() {
        return this.pdsv.getDataCenterId();
    }

    @Override
    public String getPath() {
        return this.pdsv.getPath();
    }

    @Override
    public StoragePoolType getPoolType() {
       return this.pdsv.getPoolType();
    }

    @Override
    public Date getCreated() {
       return this.pdsv.getCreated();
    }

    @Override
    public Date getUpdateTime() {
       return this.pdsv.getUpdateTime();
    }

    @Override
    public long getCapacityBytes() {
       return this.pdsv.getCapacityBytes();
    }

    @Override
    public long getAvailableBytes() {
        return this.pdsv.getAvailableBytes();
    }

    @Override
    public Long getClusterId() {
        return this.pdsv.getClusterId();
    }

    @Override
    public String getHostAddress() {
        return this.pdsv.getHostAddress();
    }

    @Override
    public String getUserInfo() {
        return this.pdsv.getUserInfo();
    }

    @Override
    public boolean isShared() {
       return this.pdsv.getScope() == ScopeType.HOST ? false : true;
    }

    @Override
    public boolean isLocal() {
        return !this.isShared();
    }

    @Override
    public StoragePoolStatus getStatus() {
        return this.pdsv.getStatus();
    }

    @Override
    public int getPort() {
        return this.pdsv.getPort();
    }

    @Override
    public Long getPodId() {
        return this.pdsv.getPodId();
    }

    @Override
    public Long getStorageProviderId() {
        return this.pdsv.getStorageProviderId();
    }

    @Override
    public boolean isInMaintenance() {
        return this.getStatus() == StoragePoolStatus.Maintenance ? true : false;
    }
}
