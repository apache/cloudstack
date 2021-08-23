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
import java.util.Map;

import javax.inject.Inject;

import org.apache.cloudstack.engine.subsystem.api.storage.ClusterScope;
import org.apache.cloudstack.engine.subsystem.api.storage.DataObject;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreCapabilities;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreDriver;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreProvider;
import org.apache.cloudstack.engine.subsystem.api.storage.HostScope;
import org.apache.cloudstack.engine.subsystem.api.storage.ObjectInDataStoreStateMachine;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStoreDriver;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStoreLifeCycle;
import org.apache.cloudstack.engine.subsystem.api.storage.Scope;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotDataFactory;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.TemplateDataFactory;
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
import com.cloud.storage.DataStoreRole;
import com.cloud.storage.ScopeType;
import com.cloud.storage.Storage.StoragePoolType;
import com.cloud.storage.StoragePoolHostVO;
import com.cloud.storage.StoragePoolStatus;
import com.cloud.storage.VMTemplateStoragePoolVO;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.StoragePoolHostDao;
import com.cloud.storage.dao.VMTemplatePoolDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.utils.component.ComponentContext;
import com.cloud.utils.db.GlobalLock;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.storage.encoding.EncodingType;

@SuppressWarnings("serial")
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
    private Map<String, String> _details;

    public PrimaryDataStoreImpl() {

    }

    public void configure(StoragePoolVO pdsv, PrimaryDataStoreDriver driver, DataStoreProvider provider) {
        this.pdsv = pdsv;
        this.driver = driver;
        this.provider = provider;
    }

    public static PrimaryDataStoreImpl createDataStore(StoragePoolVO pdsv, PrimaryDataStoreDriver driver, DataStoreProvider provider) {
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
        List<VolumeVO> volumes = volumeDao.findByPoolId(getId());
        List<VolumeInfo> volumeInfos = new ArrayList<VolumeInfo>();
        for (VolumeVO volume : volumes) {
            volumeInfos.add(VolumeObject.getVolumeObject(this, volume));
        }
        return volumeInfos;
    }

    @Override
    public DataStoreDriver getDriver() {
        return driver;
    }

    @Override
    public DataStoreRole getRole() {
        return DataStoreRole.Primary;
    }

    @Override
    public long getId() {
        return pdsv.getId();
    }

    @Override
    public void setDetails(Map<String, String> details) {
        _details = details;
    }

    @Override
    public Map<String, String> getDetails() {
        return _details;
    }

    @Override
    public String getUri() {
        String path = pdsv.getPath().replaceFirst("/*", "");
        StringBuilder builder = new StringBuilder();
        builder.append(pdsv.getPoolType());
        builder.append("://");
        builder.append(pdsv.getHostAddress());
        builder.append(File.separator);
        builder.append(path);
        builder.append(File.separator);
        builder.append("?" + EncodingType.ROLE + "=" + getRole());
        builder.append("&" + EncodingType.STOREUUID + "=" + pdsv.getUuid());
        return builder.toString();
    }

    @Override
    public Scope getScope() {
        StoragePoolVO vo = dataStoreDao.findById(pdsv.getId());
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
        return pdsv.getUuid();
    }

    @Override
    public String getName() {
        return pdsv.getName();
    }

    @Override
    public PrimaryDataStoreLifeCycle getLifeCycle() {
        return lifeCycle;
    }

    @Override
    public boolean exists(DataObject data) {
        return (objectInStoreMgr.findObject(data, data.getDataStore()) != null) ? true : false;
    }

    @Override
    public TemplateInfo getTemplate(long templateId, String configuration) {
        VMTemplateStoragePoolVO template = templatePoolDao.findByPoolTemplate(getId(), templateId, configuration);
        if (template == null || template.getState() != ObjectInDataStoreStateMachine.State.Ready) {
            return null;
        }
        return imageDataFactory.getTemplateOnPrimaryStorage(templateId, this, configuration);
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
    public boolean isManaged() {
        return pdsv.isManaged();
    }

    @Override
    public Long getParent() {
        return pdsv.getParent();
    }

    private boolean canCloneVolume() {
        return Boolean.valueOf(getDriver().getCapabilities().get(DataStoreCapabilities.CAN_CREATE_VOLUME_FROM_VOLUME.toString()));
    }

    /**
     * The parameter createEntryInTempSpoolRef in the overloaded create(DataObject, boolean) method only applies to managed storage. We pass
     * in "true" here.
     *
     * In the case of managed storage that can create a volume from a volume (clone), if the DataObject passed in is a TemplateInfo,
     * we do want to create an entry in the cloud.template_spool_ref table (so that multiple uses of the template can be leveraged from
     * the one copy on managed storage).
     *
     * In cases where UUID resigning is not available, then the code calling "create" should invoke the overloaded "create" method whose second
     * parameter is a boolean. This code can pass in "false" so that an entry in the cloud.template_spool_ref table is not created (no template to share
     * on the primary storage).
     */
    @Override
    public DataObject create(DataObject dataObject) {
        return create(dataObject, true, null);
    }

    @Override
    public DataObject create(DataObject dataObject, String configuration) {
        return create(dataObject, true, configuration);
    }

    /**
     * Please read the comment for the create(DataObject) method if you are planning on passing in "false" for createEntryInTempSpoolRef.
     *
     * The parameter configuration allows storing multiple configurations of the
     * base template appliance in primary storage (VMware supported) - null by default or no configurations
     */
    @Override
    public DataObject create(DataObject obj, boolean createEntryInTempSpoolRef, String configuration) {
        // create template on primary storage
        if (obj.getType() == DataObjectType.TEMPLATE && (!isManaged() || (createEntryInTempSpoolRef && canCloneVolume()))) {
            try {
                String templateIdPoolIdString = "templateId:" + obj.getId() + "poolId:" + getId() + "conf:" + configuration;
                VMTemplateStoragePoolVO templateStoragePoolRef;
                GlobalLock lock = GlobalLock.getInternLock(templateIdPoolIdString);
                if (!lock.lock(5)) {
                    s_logger.debug("Couldn't lock the db on the string " + templateIdPoolIdString);
                    return null;
                }
                try {
                    templateStoragePoolRef = templatePoolDao.findByPoolTemplate(getId(), obj.getId(), configuration);
                    if (templateStoragePoolRef == null) {

                        if (s_logger.isDebugEnabled()) {
                            s_logger.debug("Not found (" + templateIdPoolIdString + ") in template_spool_ref, persisting it");
                        }
                        templateStoragePoolRef = new VMTemplateStoragePoolVO(getId(), obj.getId(), configuration);
                        templateStoragePoolRef = templatePoolDao.persist(templateStoragePoolRef);
                    }
                } catch (Throwable t) {
                    if (s_logger.isDebugEnabled()) {
                        s_logger.debug("Failed to insert (" + templateIdPoolIdString + ") to template_spool_ref", t);
                    }
                    templateStoragePoolRef = templatePoolDao.findByPoolTemplate(getId(), obj.getId(), configuration);
                    if (templateStoragePoolRef == null) {
                        throw new CloudRuntimeException("Failed to create template storage pool entry");
                    } else {
                        if (s_logger.isDebugEnabled()) {
                            s_logger.debug("Another thread already inserts " + templateStoragePoolRef.getId() + " to template_spool_ref", t);
                        }
                    }
                } finally {
                    lock.unlock();
                    lock.releaseRef();
                }
            } catch (Exception e) {
                s_logger.debug("Caught exception ", e);
            }
        } else if (obj.getType() == DataObjectType.SNAPSHOT) {
            return objectInStoreMgr.create(obj, this);
        } else if (obj.getType() == DataObjectType.VOLUME) {
            VolumeVO vol = volumeDao.findById(obj.getId());
            if (vol != null) {
                vol.setPoolId(getId());
                volumeDao.update(vol.getId(), vol);
            }
        }

        return objectInStoreMgr.get(obj, this, configuration);
    }

    @Override
    public boolean delete(DataObject obj) {
        //TODO: clean up through driver
        objectInStoreMgr.delete(obj);
        return true;
    }

    @Override
    public long getDataCenterId() {
        return pdsv.getDataCenterId();
    }

    @Override
    public String getPath() {
        return pdsv.getPath();
    }

    @Override
    public StoragePoolType getPoolType() {
        return pdsv.getPoolType();
    }

    @Override
    public Date getCreated() {
        return pdsv.getCreated();
    }

    @Override
    public Date getUpdateTime() {
        return pdsv.getUpdateTime();
    }

    @Override
    public long getCapacityBytes() {
        return pdsv.getCapacityBytes();
    }

    @Override
    public long getUsedBytes() {
        return pdsv.getUsedBytes();
    }

    @Override
    public Long getCapacityIops() {
        return pdsv.getCapacityIops();
    }

    @Override
    public Long getClusterId() {
        return pdsv.getClusterId();
    }

    @Override
    public String getHostAddress() {
        return pdsv.getHostAddress();
    }

    @Override
    public String getUserInfo() {
        return pdsv.getUserInfo();
    }

    @Override
    public boolean isShared() {
        return pdsv.getScope() == ScopeType.HOST ? false : true;
    }

    @Override
    public boolean isLocal() {
        return !isShared();
    }

    @Override
    public StoragePoolStatus getStatus() {
        return pdsv.getStatus();
    }

    @Override
    public int getPort() {
        return pdsv.getPort();
    }

    @Override
    public Long getPodId() {
        return pdsv.getPodId();
    }

    public Date getRemoved() {
        return pdsv.getRemoved();
    }

    @Override
    public boolean isInMaintenance() {
        return getStatus() == StoragePoolStatus.PrepareForMaintenance || getStatus() == StoragePoolStatus.Maintenance ||
            getStatus() == StoragePoolStatus.ErrorInMaintenance || getRemoved() != null;
    }

    @Override
    public HypervisorType getHypervisor() {
       return pdsv.getHypervisor();
    }

    @Override
    public String getStorageProviderName() {
        return pdsv.getStorageProviderName();
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
