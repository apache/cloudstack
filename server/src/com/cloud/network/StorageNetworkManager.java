package com.cloud.network;

import com.cloud.dc.StorageNetworkIpAddressVO;
import com.cloud.utils.component.Manager;

public interface StorageNetworkManager extends Manager {
	StorageNetworkIpAddressVO acquireIpAddress(long podId);

	void releaseIpAddress(String ip);
	
	boolean isAStorageIpAddress(String ip);
}
