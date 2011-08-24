/**
 *  Copyright (C) 2011 Cloud.com, Inc.  All rights reserved.
 */

package com.cloud.agent.api;

import java.util.HashMap;
import java.util.Map;

public class ExternalNetworkResourceUsageAnswer extends Answer {		
	public Map<String, long[]> ipBytes;
	public Map<String, long[]> guestVlanBytes;

	protected ExternalNetworkResourceUsageAnswer() {
    }
	
	public ExternalNetworkResourceUsageAnswer(Command command) {
		super(command);
		this.ipBytes = new HashMap<String, long[]>();
		this.guestVlanBytes = new HashMap<String, long[]>();
	}
	
	public ExternalNetworkResourceUsageAnswer(Command command, Exception e) {
		super(command, e);
		this.ipBytes = null;
		this.guestVlanBytes = null;
	}
	
}
