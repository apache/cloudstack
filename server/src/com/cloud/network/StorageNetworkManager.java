package com.cloud.network;

import java.util.List;

import com.cloud.dc.StorageNetworkIpAddressVO;
import com.cloud.utils.component.Manager;
import com.cloud.vm.SecondaryStorageVmVO;

public interface StorageNetworkManager extends Manager {
	StorageNetworkIpAddressVO acquireIpAddress(long podId);

	void releaseIpAddress(String ip);
	
	boolean isStorageIpRangeAvailable(long zoneId);
	
	List<SecondaryStorageVmVO> getSSVMWithNoStorageNetwork(long zoneId);
	
	boolean isAnyStorageIpInUseInZone(long zoneId);
}
