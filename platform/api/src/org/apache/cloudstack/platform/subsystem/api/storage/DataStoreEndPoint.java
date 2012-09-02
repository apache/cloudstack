package org.apache.cloudstack.platform.subsystem.api.storage;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;

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
	
	public Answer sendCommand(Command cmd) {
		return new Answer(cmd);
	}
}
