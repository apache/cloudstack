package org.apache.cloudstack.storage.datastore;

import java.util.List;

import javax.inject.Inject;

import org.apache.cloudstack.storage.datastore.driver.PrimaryDataStoreDriver;
import org.apache.cloudstack.storage.volume.Volume;
import org.apache.cloudstack.storage.volume.db.VolumeDao;
import org.apache.cloudstack.storage.volume.db.VolumeVO;
import org.apache.cloudstack.storage.volume.disktype.VolumeDiskType;

public class DefaultPrimaryDataStoreImpl implements PrimaryDataStore {
	protected PrimaryDataStoreDriver driver;
	protected List<VolumeDiskType> supportedDiskTypes;
	@Inject
	VolumeDao volumeDao;
	public DefaultPrimaryDataStoreImpl(PrimaryDataStoreDriver driver, List<VolumeDiskType> types) {
		this.driver = driver;
		this.supportedDiskTypes = types;
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
		
		if (!this.getSupportedDiskTypes().contains(diskType)) {
			return null;
		}
		
		vol.setVolumeDiskType(diskType);
		this.driver.createVolume(vol);
		vol.update();
		return vol;
	}

	@Override
	public List<VolumeDiskType> getSupportedDiskTypes() {
		return this.supportedDiskTypes;
	}
}
