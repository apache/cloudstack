package com.cloud.dc.dao;

import java.util.List;

import com.cloud.dc.StorageNetworkIpRangeVO;
import com.cloud.utils.db.GenericDao;

public interface StorageNetworkIpRangeDao extends GenericDao<StorageNetworkIpRangeVO, Long> {
	List<StorageNetworkIpRangeVO> listByRangeId(long rangeId);

	List<StorageNetworkIpRangeVO> listByPodId(long podId);

	List<StorageNetworkIpRangeVO> listByDataCenterId(long dcId);
	
	long countRanges();
}
