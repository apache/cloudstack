package org.apache.cloudstack.platform.subsystem.api.storage;

public class DataStoreEndPoint {
	protected long hostId;
	protected String privIp;
	
	public DataStoreEndPoint(long host, String ip) {
		hostId = host;
		privIp = ip;
	}
	
	public long getHostId() {
		return hostId;
	}
	
	public String getPrivateIp() {
		return privIp;
	}
}
