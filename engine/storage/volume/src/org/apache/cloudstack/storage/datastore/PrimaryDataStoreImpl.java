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

import com.cloud.utils.db.GlobalLock;
import org.apache.cloudstack.engine.subsystem.api.storage.ClusterScope;
import org.apache.cloudstack.engine.subsystem.api.storage.DataObject;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreDriver;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreProvider;
import org.apache.cloudstack.engine.subsystem.api.storage.HostScope;
import org.apache.cloudstack.engine.subsystem.api.storage.TemplateDataFactory;
import org.apache.cloudstack.engine.subsystem.api.storage.ObjectInDataStoreStateMachine;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStoreDriver;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStoreLifeCycle;
import org.apache.cloudstack.engine.subsystem.api.storage.Scope;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotDataFactory;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.TemplateInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.ZoneScope;
import org.apache.cloudstack.engine.subsystem.api.storage.disktype.DiskFormat;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.cloudstack.storage.to.PrimaryDataStoreTO;
import org.apache.cloudstack.storage.volume.VolumeObject;
import org.apache.log4j.Logger;

import com.cloud.agent.api.to.DataObjectType;
import com.cloud.agent.api.to.DataStoreTO;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.storage.Storage.StoragePoolType;
import com.cloud.storage.DataStoreRole;
import com.cloud.storage.ScopeType;
import com.cloud.storage.StoragePoolHostVO;
import com.cloud.storage.StoragePoolStatus;
import com.cloud.storage.VMTemplateStoragePoolVO;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.StoragePoolHostDao;
import com.cloud.storage.dao.VMTemplatePoolDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.utils.component.ComponentContext;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.storage.encoding.EncodingType;

public class PrimaryDataStoreImpl implements PrimaryDataStore {
    private static final Logger s_logger = Logger.getLogger(PrimaryDataStoreImpl.class);
    protected PrimaryDataStoreDriver driver;
    protected StoragePoolVO pdsv;
    @Inject
    protected PrimaryDataStoreDao dataStoreDao;
    protected PrimaryDataStoreLifeCycle lifeCycle;
    @Inject
    private ObjectInDataStoreManager objectInStoreMgr;
    @Inject
    TemplateDataFactory imageDataFactory;
    @Inject
    SnapshotDataFactory snapshotFactory;
    protected DataStoreProvider provider;
    @Inject
    VMTemplatePoolDao templatePoolDao;
    @Inject
    StoragePoolHostDao poolHostDao;

    @Inject
    private VolumeDao volumeDao;

    public PrimaryDataStoreImpl() {

    }

    public void configure(StoragePoolVO pdsv, PrimaryDataStoreDriver driver, DataStoreProvider provider) {
        this.pdsv = pdsv;
        this.driver = driver;
        this.provider = provider;
    }

    public static PrimaryDataStoreImpl createDataStore(StoragePoolVO pdsv, PrimaryDataStoreDriver driver,
            DataStoreProvider provider) {
        PrimaryDataStoreImpl dataStore = ComponentContext.inject(PrimaryDataStoreImpl.class);
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
            return new ClusterScope(vo.getClusterId(), vo.getPodId(), vo.getDataCenterId());
        } else if (vo.getScope() == ScopeType.ZONE) {
            return new ZoneScope(vo.getDataCenterId());
        } else if (vo.getScope() == ScopeType.HOST) {
            List<StoragePoolHostVO> poolHosts = poolHostDao.listByPoolId(vo.getId());
            if (poolHosts.size() > 0) {
                return new HostScope(poolHosts.get(0).getHostId(), vo.getClusterId(), vo.getDataCenterId());
            }
            s_logger.debug("can't find a local storage in pool host table: " + vo.getId());
        }
        return null;
    }

    @Override
    public boolean isHypervisorSupported(HypervisorType hypervisor) {
        return true;
    }

    @Override
    public boolean isLocalStorageSupported() {
        return false;
    }

    @Override
    public boolean isVolumeDiskTypeSupported(DiskFormat diskType) {
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
        return null;
    }

    @Override
    public DiskFormat getDefaultDiskType() {
        return null;
    }

    @Override
    public DataObject create(DataObject obj) {
        // create template on primary storage
        if (obj.getType() == DataObjectType.TEMPLATE) {
            try{
                String templateIdPoolIdString = "templateId:" + obj.getId() + "poolId:" + this.getId();
                VMTemplateStoragePoolVO templateStoragePoolRef;
                GlobalLock lock = GlobalLock.getInternLock(templateIdPoolIdString);
                if (!lock.lock(5)) {
                    s_logger.debug("Couldn't lock the db on the string " + templateIdPoolIdString);
                    return null;
                }
                try {
                    templateStoragePoolRef = templatePoolDao.findByPoolTemplate(this.getId(),
                            obj.getId());
                    if (templateStoragePoolRef == null) {

                        if (s_logger.isDebugEnabled()) {
                            s_logger.debug("Not found (" + templateIdPoolIdString + ") in template_spool_ref, persisting it");
                        }
                        templateStoragePoolRef = new VMTemplateStoragePoolVO(this.getId(), obj.getId());
                        templateStoragePoolRef = templatePoolDao.persist(templateStoragePoolRef);
                    }
                } catch (Throwable t) {
                        if (s_logger.isDebugEnabled()) {
                            s_logger.debug("Failed to insert (" + templateIdPoolIdString +  ") to template_spool_ref", t);
                        }
                        templateStoragePoolRef = templatePoolDao.findByPoolTemplate(this.getId(), obj.getId());
                        if (templateStoragePoolRef == null) {
                            throw new CloudRuntimeException("Failed to create template storage pool entry");
                        } else {
                            if (s_logger.isDebugEnabled()) {
                                s_logger.debug("Another thread already inserts " + templateStoragePoolRef.getId() + " to template_spool_ref", t);
                            }
                        }
                }finally {
                        lock.unlock();
                        lock.releaseRef();
                }
            } catch (Exception e){
                s_logger.debug("Caught exception ", e);
            }
        } else if (obj.getType() == DataObjectType.SNAPSHOT) {
            return objectInStoreMgr.create(obj, this);
        }

        return objectInStoreMgr.get(obj, this);
    }

    @Override
    public boolean delete(DataObject obj) {
        //TODO: clean up through driver
        objectInStoreMgr.delete(obj);
        return true;
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
    public long getUsedBytes() {
        return this.pdsv.getUsedBytes();
    }

    @Override
    public Long getCapacityIops() {
        return this.pdsv.getCapacityIops();
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
    public boolean isInMaintenance() {
        return this.getStatus() == StoragePoolStatus.Maintenance ? true : false;
    }

    @Override
    public String getStorageProviderName() {
        return this.pdsv.getStorageProviderName();
    }

    @Override
    public DataStoreTO getTO() {
        DataStoreTO to = getDriver().getStoreTO(this);
        if (to == null) {
            PrimaryDataStoreTO primaryTO = new PrimaryDataStoreTO(this);
            return primaryTO;
        }
        return to;
    }
}
