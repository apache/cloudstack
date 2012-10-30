package org.apache.cloudstack.storage.datastore;

import java.util.List;

import javax.inject.Inject;

import org.apache.cloudstack.storage.datastore.db.DataStoreVO;
import org.apache.cloudstack.storage.datastore.driver.PrimaryDataStoreDriver;
import org.apache.cloudstack.storage.volume.Volume;
import org.apache.cloudstack.storage.volume.db.VolumeDao;
import org.apache.cloudstack.storage.volume.db.VolumeVO;
import org.apache.cloudstack.storage.volume.disktype.VolumeDiskType;

public class DefaultPrimaryDataStoreImpl implements PrimaryDataStore {
	protected PrimaryDataStoreDriver driver;
	protected DataStoreVO pdsv;
	protected PrimaryDataStoreInfo pdsInfo;
	@Inject
	public VolumeDao volumeDao;
	public DefaultPrimaryDataStoreImpl(PrimaryDataStoreDriver driver, DataStoreVO pdsv, PrimaryDataStoreInfo pdsInfo) {
		this.driver = driver;
		this.pdsv = pdsv;
		this.pdsInfo = pdsInfo;
	}
	
	@Override
	public Volume getVolume(long id) {
		VolumeVO volumeVO = volumeDao.findById(id);
		Volume vol = new Volume(this, volumeVO);
		return vol;
	}

	@Override
	public List<Volume> getVolumes() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean deleteVolume(long id) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Volume createVolume(long id, VolumeDiskType diskType) {
		Volume vol = this.getVolume(id);
		if (vol == null) {
			return null;
		}
		
		if (!pdsInfo.isVolumeDiskTypeSupported(diskType)) {
			return null;
		}
		
		vol.setVolumeDiskType(diskType);
		this.driver.createVolume(vol);
		vol.update();
		return vol;
	}
}
