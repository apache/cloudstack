package com.cloud.dc.dao;

import java.util.List;

import com.cloud.dc.StorageNetworkIpAddressVO;
import com.cloud.utils.db.GenericDao;

public interface StorageNetworkIpAddressDao extends GenericDao<StorageNetworkIpAddressVO, Long> {
	long countInUseIpByRangeId(long rangeId);

	List<String> listInUseIpByRangeId(long rangeId);
	
	StorageNetworkIpAddressVO takeIpAddress(long rangeId);
	
	void releaseIpAddress(String ip);
}
