package com.cloud.dc;

public interface StorageNetworkIpRange {
	long getId();
	
	int getVlan();

	long getPodId();

	String getStartIp();

	String getEndIp();

	long getNetworkId();

	long getDataCenterId();
}
