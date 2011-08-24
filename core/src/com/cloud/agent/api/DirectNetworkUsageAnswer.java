/**
 *  Copyright (C) 2011 Cloud.com, Inc.  All rights reserved.
 */

package com.cloud.agent.api;

import java.util.HashMap;
import java.util.Map;

public class DirectNetworkUsageAnswer extends Answer {
	
	Map<String, long[]> ipBytesSentAndReceived;

	protected DirectNetworkUsageAnswer() {
    }
	
	public DirectNetworkUsageAnswer(Command command) {
		super(command);
		this.ipBytesSentAndReceived = new HashMap<String, long[]>();
	}
	
	public DirectNetworkUsageAnswer(Command command, Exception e) {
		super(command, e);
		this.ipBytesSentAndReceived = null;
	}
	
	public void put(String ip, long[] bytesSentAndReceived) {
		this.ipBytesSentAndReceived.put(ip, bytesSentAndReceived);
	}
	
	public long[] get(String ip) {
		long[] entry = ipBytesSentAndReceived.get(ip);
		if (entry == null) {
			ipBytesSentAndReceived.put(ip, new long[]{0, 0});
			return ipBytesSentAndReceived.get(ip);
		} else {
			return entry;
		}
	}
	
	public Map<String, long[]> getIpBytesSentAndReceived() {
		return ipBytesSentAndReceived;
	}
}
