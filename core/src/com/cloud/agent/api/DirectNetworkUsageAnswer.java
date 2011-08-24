/**
 * *  Copyright (C) 2011 Citrix Systems, Inc.  All rights reserved
*
 *
 * This software is licensed under the GNU General Public License v3 or later.
 *
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
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
