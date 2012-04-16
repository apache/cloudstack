package com.cloud.storage.dao;

import java.util.List;

import com.cloud.host.HostVO;
import com.cloud.storage.VolumeHostVO;
import com.cloud.utils.db.GenericDao;

public interface VolumeHostDao extends GenericDao<VolumeHostVO, Long> {

	VolumeHostVO findByHostVolume(long hostId, long volumeId);

	VolumeHostVO findByVolumeId(long volumeId);

	List<VolumeHostVO> listBySecStorage(long sserverId);

}
