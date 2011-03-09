package com.cloud.cluster;

public interface ClusterManagerMBean {
	public long getMsid();
	public String getLastUpdateTime();
	public String getClusterNodeIP();
	public String getVersion();
	public int getHeartbeatInterval();
	public int getHeartbeatThreshold();
	public void setHeartbeatThreshold(int threshold);
}
