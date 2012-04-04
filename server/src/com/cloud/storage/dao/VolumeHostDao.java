package com.cloud.storage.dao;

import com.cloud.storage.VolumeHostVO;
import com.cloud.utils.db.GenericDao;

public interface VolumeHostDao extends GenericDao<VolumeHostVO, Long> {

	VolumeHostVO findByHostVolume(long id, long id2);

}
