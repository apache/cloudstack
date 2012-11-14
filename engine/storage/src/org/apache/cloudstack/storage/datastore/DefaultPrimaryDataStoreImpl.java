package org.apache.cloudstack.storage.datastore;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.inject.Inject;

import org.apache.cloudstack.engine.cloud.entity.api.VolumeEntity;
import org.apache.cloudstack.engine.subsystem.api.storage.EndPoint;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStoreInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.disktype.VolumeDiskType;
import org.apache.cloudstack.storage.HypervisorHostEndPoint;
import org.apache.cloudstack.storage.datastore.db.DataStoreVO;
import org.apache.cloudstack.storage.datastore.driver.PrimaryDataStoreDriver;

import org.apache.cloudstack.storage.volume.VolumeEntityImpl;
import org.apache.cloudstack.storage.volume.VolumeEvent;
import org.apache.cloudstack.storage.volume.VolumeObject;
import org.apache.cloudstack.storage.volume.db.VolumeDao;
import org.apache.cloudstack.storage.volume.db.VolumeVO;

import org.apache.log4j.Logger;

import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.utils.component.ComponentInject;
import com.cloud.utils.exception.CloudRuntimeException;

import edu.emory.mathcs.backport.java.util.Collections;

public class DefaultPrimaryDataStoreImpl implements PrimaryDataStore {
	private static final Logger s_logger = Logger.getLogger(DefaultPrimaryDataStoreImpl.class);
	protected PrimaryDataStoreDriver driver;
	protected DataStoreVO pdsv;
	protected PrimaryDataStoreInfo pdsInfo;
	@Inject
	private VolumeDao volumeDao;
	@Inject
	private HostDao hostDao;
	public DefaultPrimaryDataStoreImpl(PrimaryDataStoreDriver driver, DataStoreVO pdsv, PrimaryDataStoreInfo pdsInfo) {
		this.driver = driver;
		this.pdsv = pdsv;
		this.pdsInfo = pdsInfo;
	}
	
	@Override
	public VolumeInfo getVolume(long id) {
		VolumeVO volumeVO = volumeDao.findById(id);
		VolumeObject vol = new VolumeObject(this, volumeVO);
		return vol;
	}

	@Override
	public List<VolumeInfo> getVolumes() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean deleteVolume(long id) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public VolumeInfo createVolume(VolumeInfo vol, VolumeDiskType diskType) {
		
		
	
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
			HypervisorHostEndPoint ep = new HypervisorHostEndPoint(host.getId());
			ComponentInject.inject(ep);
			endpoints.add(ep);
		}
		Collections.shuffle(endpoints);
		return endpoints;
	}

	@Override
	public PrimaryDataStoreInfo getDataStoreInfo() {
		// TODO Auto-generated method stub
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
	public boolean isVolumeDiskTypeSupported(VolumeDiskType diskType) {
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
	public VolumeObject createVolume(VolumeObject vo, VolumeDiskType diskType) {
		if (!pdsInfo.isVolumeDiskTypeSupported(diskType)) {
			return null;
		}

		vo.setVolumeDiskType(diskType);
		this.driver.createVolume(vo);
		return vo;
	}

	@Override
	public boolean exists(VolumeInfo vi) {
		// TODO Auto-generated method stub
		return false;
	}
}
