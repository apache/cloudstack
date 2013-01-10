package org.apache.cloudstack.storage.datastore;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.apache.cloudstack.engine.datacenter.entity.api.DataCenterResourceEntity;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStoreLifeCycle;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStoreProvider;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.disktype.VolumeDiskType;
import org.apache.cloudstack.framework.async.AsyncCompletionCallback;
import org.apache.cloudstack.storage.EndPoint;
import org.apache.cloudstack.storage.HypervisorHostEndPoint;
import org.apache.cloudstack.storage.command.CommandResult;
import org.apache.cloudstack.storage.datastore.configurator.validator.StorageProtocolTransformer;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreVO;
import org.apache.cloudstack.storage.datastore.driver.PrimaryDataStoreDriver;
import org.apache.cloudstack.storage.image.TemplateInfo;
import org.apache.cloudstack.storage.to.PrimaryDataStoreTO;
import org.apache.cloudstack.storage.to.VolumeTO;
import org.apache.cloudstack.storage.volume.TemplateOnPrimaryDataStoreInfo;
import org.apache.cloudstack.storage.volume.TemplatePrimaryDataStoreManager;
import org.apache.cloudstack.storage.volume.VolumeObject;
import org.apache.cloudstack.storage.volume.db.VolumeDao2;
import org.apache.cloudstack.storage.volume.db.VolumeVO;
import org.apache.log4j.Logger;

import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.utils.component.ComponentContext;
import com.cloud.utils.exception.CloudRuntimeException;

import edu.emory.mathcs.backport.java.util.Collections;

public class DefaultPrimaryDataStore implements PrimaryDataStore {
    private static final Logger s_logger = Logger.getLogger(DefaultPrimaryDataStore.class);
    protected PrimaryDataStoreDriver driver;
    protected PrimaryDataStoreVO pdsv;
    protected PrimaryDataStoreLifeCycle lifeCycle;
    protected PrimaryDataStoreProvider provider;
    protected StorageProtocolTransformer protocalTransformer;
    private HypervisorType supportedHypervisor;
    private boolean isLocalStorageSupported = false;
    @Inject
    private VolumeDao2 volumeDao;
    @Inject
    private HostDao hostDao;
    @Inject
    private PrimaryDataStoreDao dataStoreDao;
    @Inject
    private TemplatePrimaryDataStoreManager templatePrimaryStoreMgr;

    private DefaultPrimaryDataStore(PrimaryDataStoreVO pdsv) {
        this.pdsv = pdsv;
    }

    public void setDriver(PrimaryDataStoreDriver driver) {
        driver.setDataStore(this);
        this.driver = driver;
    }

    public void setLifeCycle(PrimaryDataStoreLifeCycle lifeCycle) {
        lifeCycle.setDataStore(this);
        this.lifeCycle = lifeCycle;
    }

    public void setProvider(PrimaryDataStoreProvider provider) {
        this.provider = provider;
    }

    public void setProtocolTransFormer(StorageProtocolTransformer transformer) {
        this.protocalTransformer = transformer;
    }

    @Override
    public PrimaryDataStoreTO getDataStoreTO() {
        return this.protocalTransformer.getDataStoreTO(this);
    }

    @Override
    public VolumeTO getVolumeTO(VolumeInfo volume) {
        return this.protocalTransformer.getVolumeTO(volume);
    }

    public static DefaultPrimaryDataStore createDataStore(PrimaryDataStoreVO pdsv) {
        DefaultPrimaryDataStore dataStore = new DefaultPrimaryDataStore(pdsv);
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

    @Override
    public void deleteVolumeAsync(VolumeInfo volume, AsyncCompletionCallback<CommandResult> callback) {
        CommandResult result = new CommandResult();
        if (volume.isAttachedVM()) {
            result.setResult("Can't delete volume: " + volume.getId() + ", if it's attached to a VM");
            callback.complete(result);
        }
        this.driver.deleteVolumeAsync((VolumeObject)volume, callback);
    }

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
            HypervisorHostEndPoint ep = new HypervisorHostEndPoint(host.getId(), host.getPrivateIpAddress());
            ComponentContext.inject(ep);
            endpoints.add(ep);
        }
        Collections.shuffle(endpoints);
        return endpoints;
    }

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
    public boolean isVolumeDiskTypeSupported(VolumeDiskType diskType) {
        return true;
    }

    @Override
    public long getCapacity() {
        return this.driver.getCapacity();
    }

    @Override
    public long getAvailableCapacity() {
        return this.driver.getAvailableCapacity();
    }

    @Override
    public void createVolumeAsync(VolumeInfo vi, VolumeDiskType diskType, AsyncCompletionCallback<CommandResult> callback) {
        if (!isVolumeDiskTypeSupported(diskType)) {
            throw new CloudRuntimeException("disk type " + diskType + " is not supported");
        }
        VolumeObject vo = (VolumeObject) vi;
        vo.setVolumeDiskType(diskType);
        this.driver.createVolumeAsync(vo, callback);
    }

    @Override
    public boolean exists(VolumeInfo vi) {
        VolumeVO vol = volumeDao.findByVolumeIdAndPoolId(vi.getId(), this.getId());
        return (vol != null) ? true : false;
    }

    @Override
    public boolean templateExists(TemplateInfo template) {
        return (templatePrimaryStoreMgr.findTemplateOnPrimaryDataStore(template, this) != null) ? true : false;
    }

    @Override
    public VolumeDiskType getDefaultDiskType() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public long getId() {
        return pdsv.getId();
    }

    @Override
    public TemplateOnPrimaryDataStoreInfo getTemplate(TemplateInfo template) {
        return templatePrimaryStoreMgr.findTemplateOnPrimaryDataStore(template, this);
    }

    @Override
    public VolumeInfo createVoluemFromBaseImage(VolumeInfo volume, TemplateOnPrimaryDataStoreInfo template) {
        VolumeObject vo = (VolumeObject) volume;
        vo.setVolumeDiskType(template.getTemplate().getDiskType());
        //this.driver.createVolumeFromBaseImage(vo, template);
        return volume;
    }

    @Override
    public void createVoluemFromBaseImageAsync(VolumeInfo volume, TemplateOnPrimaryDataStoreInfo templateStore, AsyncCompletionCallback<CommandResult> callback) {
        VolumeObject vo = (VolumeObject) volume;
        vo.setVolumeDiskType(templateStore.getTemplate().getDiskType());

        this.driver.createVolumeFromBaseImageAsync(vo, templateStore, callback);
    }

    @Override
    public boolean installTemplate(TemplateOnPrimaryDataStoreInfo template) {
        // TODO Auto-generated method stub
        return true;
    }

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

    @Override
    public PrimaryDataStoreLifeCycle getLifeCycle() {
        return lifeCycle;
    }

    @Override
    public PrimaryDataStoreProvider getProvider() {
        return this.provider;
    }


}
