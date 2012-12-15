package org.apache.cloudstack.storage.datastore;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.apache.cloudstack.engine.datacenter.entity.api.DataCenterResourceEntity.State;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStoreInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStoreLifeCycle;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStoreProvider;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.disktype.VolumeDiskType;
import org.apache.cloudstack.storage.EndPoint;
import org.apache.cloudstack.storage.HypervisorHostEndPoint;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreVO;
import org.apache.cloudstack.storage.datastore.driver.DefaultPrimaryDataStoreDriverImpl;
import org.apache.cloudstack.storage.datastore.driver.PrimaryDataStoreDriver;
import org.apache.cloudstack.storage.image.TemplateInfo;
import org.apache.cloudstack.storage.volume.TemplateOnPrimaryDataStoreInfo;
import org.apache.cloudstack.storage.volume.TemplatePrimaryDataStoreManager;
import org.apache.cloudstack.storage.volume.VolumeObject;
import org.apache.cloudstack.storage.volume.db.VolumeDao;
import org.apache.cloudstack.storage.volume.db.VolumeVO;

import org.apache.log4j.Logger;

import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.utils.component.ComponentInject;
import edu.emory.mathcs.backport.java.util.Collections;

public class DefaultPrimaryDataStore implements PrimaryDataStore {
    private static final Logger s_logger = Logger.getLogger(DefaultPrimaryDataStore.class);
    protected PrimaryDataStoreDriver driver;
    protected PrimaryDataStoreVO pdsv;
    protected PrimaryDataStoreInfo pdsInfo;
    protected PrimaryDataStoreLifeCycle lifeCycle;
    protected PrimaryDataStoreProvider provider;
    private HypervisorType supportedHypervisor;
    private boolean isLocalStorageSupported = false;
    @Inject
    private VolumeDao volumeDao;
    @Inject
    HostDao hostDao;
    @Inject
    TemplatePrimaryDataStoreManager templatePrimaryStoreMgr;

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
    
    public static DefaultPrimaryDataStore createDataStore(PrimaryDataStoreVO pdsv) {
        DefaultPrimaryDataStore dataStore = new DefaultPrimaryDataStore(pdsv);
        return ComponentInject.inject(dataStore);
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
    public boolean deleteVolume(VolumeInfo volume) {
        return this.driver.deleteVolume((VolumeObject)volume);
    }

    @Override
    public List<EndPoint> getEndPoints() {
        Long clusterId = pdsv.getClusterId();
        if (clusterId == null) {
            return null;
        }
        List<EndPoint> endpoints = new ArrayList<EndPoint>();
        List<HostVO> hosts = hostDao.findHypervisorHostInCluster(clusterId);
        for (HostVO host : hosts) {
            HypervisorHostEndPoint ep = new HypervisorHostEndPoint(host.getId(), host.getPrivateIpAddress());
            ComponentInject.inject(ep);
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
        // TODO Auto-generated method stub
        return false;
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
    public VolumeObject createVolume(VolumeInfo vi, VolumeDiskType diskType) {
        if (!isVolumeDiskTypeSupported(diskType)) {
            return null;
        }
        VolumeObject vo = (VolumeObject) vi;
        vo.setVolumeDiskType(diskType);
        this.driver.createVolume(vo);
        return vo;
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
        this.driver.createVolumeFromBaseImage(vo, template);
        return volume;
    }

    @Override
    public boolean installTemplate(TemplateOnPrimaryDataStoreInfo template) {
        // TODO Auto-generated method stub
        return true;
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
        return lifeCycle;
    }

    @Override
    public PrimaryDataStoreProvider getProvider() {
        return this.provider;
    }
}
