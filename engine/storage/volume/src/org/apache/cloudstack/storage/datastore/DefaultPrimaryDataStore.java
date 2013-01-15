package org.apache.cloudstack.storage.datastore;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.apache.cloudstack.engine.datacenter.entity.api.DataCenterResourceEntity;
import org.apache.cloudstack.engine.subsystem.api.storage.ClusterScope;
import org.apache.cloudstack.engine.subsystem.api.storage.DataObject;
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
import org.apache.cloudstack.storage.image.ImageDataFactory;
import org.apache.cloudstack.storage.image.TemplateInfo;
import org.apache.cloudstack.storage.snapshot.SnapshotDataFactory;
import org.apache.cloudstack.storage.snapshot.SnapshotInfo;
import org.apache.cloudstack.storage.volume.PrimaryDataStoreDriver;
import org.apache.cloudstack.storage.volume.VolumeObject;
import org.apache.cloudstack.storage.volume.db.VolumeDao2;
import org.apache.cloudstack.storage.volume.db.VolumeVO;
import org.apache.log4j.Logger;

import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.utils.component.ComponentContext;

public class DefaultPrimaryDataStore implements PrimaryDataStore {
    private static final Logger s_logger = Logger
            .getLogger(DefaultPrimaryDataStore.class);
    protected PrimaryDataStoreDriver driver;
    protected PrimaryDataStoreVO pdsv;
    protected PrimaryDataStoreLifeCycle lifeCycle;
    protected DataStoreProvider provider;
    //protected StorageProtocolTransformer protocalTransformer;
    private HypervisorType supportedHypervisor;
    private boolean isLocalStorageSupported = false;
    @Inject
    private VolumeDao2 volumeDao;
    @Inject
    private HostDao hostDao;
    @Inject
    private PrimaryDataStoreDao dataStoreDao;
    @Inject
    private ObjectInDataStoreManager objectInStoreMgr;
    @Inject
    ImageDataFactory imageDataFactory;
    @Inject
    SnapshotDataFactory snapshotFactory;

    private DefaultPrimaryDataStore(PrimaryDataStoreVO pdsv,
            PrimaryDataStoreDriver driver,
            DataStoreProvider provider) {
        this.pdsv = pdsv;
        this.driver = driver;
        this.provider = provider;
    }

    @Override
    public PrimaryDataStoreDriver getDriver() {
        return this.driver;
    }

    public static DefaultPrimaryDataStore createDataStore(
            PrimaryDataStoreVO pdsv,
            PrimaryDataStoreDriver driver,
            DataStoreProvider provider) {
        DefaultPrimaryDataStore dataStore = new DefaultPrimaryDataStore(pdsv, driver, provider);
        return ComponentContext.inject(dataStore);
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

/*    @Override
    public void deleteAsync(DataObject volume,
            AsyncCompletionCallback<CommandResult> callback) {
        this.driver.deleteAsync((VolumeObject) volume, callback);
    }
*/
    /*
    @Override
    public List<EndPoint> getEndPoints() {
        Long clusterId = pdsv.getClusterId();
        if (clusterId == null) {
            pdsv = dataStoreDao.findById(pdsv.getId());
            clusterId = pdsv.getClusterId();
            if (clusterId == null) {
                return new ArrayList<EndPoint>();
            }
        }

        List<EndPoint> endpoints = new ArrayList<EndPoint>();
        List<HostVO> hosts = hostDao.findHypervisorHostInCluster(clusterId);
        for (HostVO host : hosts) {
            HypervisorHostEndPoint ep = new HypervisorHostEndPoint(
                    host.getId(), host.getPrivateIpAddress());
            ComponentInject.inject(ep);
            endpoints.add(ep);
        }
        Collections.shuffle(endpoints);
        return endpoints;
    }*/

    public void setSupportedHypervisor(HypervisorType type) {
        this.supportedHypervisor = type;
    }

    @Override
    public boolean isHypervisorSupported(HypervisorType hypervisor) {
        return (this.supportedHypervisor == hypervisor) ? true : false;
    }

    public void setLocalStorageFlag(boolean supported) {
        this.isLocalStorageSupported = supported;
    }

    @Override
    public boolean isLocalStorageSupported() {
        return this.isLocalStorageSupported;
    }

    @Override
    public boolean isVolumeDiskTypeSupported(DiskFormat diskType) {
        return true;
    }

    @Override
    public long getCapacity() {
        return 0;
    }

    @Override
    public long getAvailableCapacity() {
        //return this.driver.getAvailableCapacity();
        return 0;
    }

/*    @Override
    public void createAsync(DataObject data,
            AsyncCompletionCallback<CommandResult> callback) {
        this.provider.getVolumeDriver().createAsync(data, callback);
    }
*/
/*    @Override
    public void takeSnapshot(SnapshotInfo snapshot,
            AsyncCompletionCallback<CommandResult> callback) {
        this.provider.getSnapshotDriver().takeSnapshot(snapshot, callback);
    }
*/
/*    @Override
    public void revertSnapshot(SnapshotInfo snapshot,
            AsyncCompletionCallback<CommandResult> callback) {
        this.provider.getSnapshotDriver().revertSnapshot(snapshot, callback);
    }

    @Override
    public void deleteSnapshot(SnapshotInfo snapshot,
            AsyncCompletionCallback<CommandResult> callback) {
        this.provider.getSnapshotDriver().deleteSnapshot(snapshot, callback);
    }
*/
    @Override
    public boolean exists(DataObject data) {
        return (objectInStoreMgr.findObject(data.getId(), data.getType(), this.getId(), this.getRole()) != null) ? true
                : false;
    }

    @Override
    public DiskFormat getDefaultDiskType() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public long getId() {
        return pdsv.getId();
    }

    @Override
    public TemplateInfo getTemplate(long templateId) {
       return imageDataFactory.getTemplate(templateId, this);
    }

/*    @Override
    public void createVoluemFromBaseImageAsync(VolumeInfo volume,
            TemplateInfo template,
            AsyncCompletionCallback<CommandResult> callback) {
        VolumeObject vo = (VolumeObject) volume;
        vo.setVolumeDiskType(template.getDiskType());
        this.driver.createVolumeFromBaseImageAsync(vo, template, callback);
    }
*/

    @Override
    public String getUuid() {
        return this.pdsv.getUuid();
    }

    @Override
    public DataCenterResourceEntity.State getManagedState() {
        return null;
    }

    @Override
    public String getName() {
        return this.pdsv.getName();
    }

    @Override
    public String getType() {
        return this.pdsv.getPoolType();
    }

    public DataStoreProvider getProvider() {
        return this.provider;
    }

    @Override
    public DataStoreRole getRole() {
        return DataStoreRole.Primary;
    }

    @Override
    public String getUri() {
        return this.pdsv.getPoolType() + File.separator
                + this.pdsv.getHostAddress() + File.separator
                + this.pdsv.getPath();
    }

    @Override
    public PrimaryDataStoreLifeCycle getLifeCycle() {
        return this.lifeCycle;
    }

    @Override
    public SnapshotInfo getSnapshot(long snapshotId) {
        return snapshotFactory.getSnapshot(snapshotId, this);
    }

    @Override
    public Scope getScope() {
        if (pdsv.getScope() == ScopeType.CLUSTER) {
            return new ClusterScope(pdsv.getClusterId(), pdsv.getPodId(), pdsv.getDataCenterId());
        } else if (pdsv.getScope() == ScopeType.ZONE) {
            return new ZoneScope(pdsv.getDataCenterId());
        }
        
        return null;
    }
}
