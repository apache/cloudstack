/**
 *  Copyright (C) 2011 Cloud.com, Inc.  All rights reserved.
 */

package com.cloud.agent.api;

public class ExternalNetworkResourceUsageCommand extends Command {

	public ExternalNetworkResourceUsageCommand() {        
    }
	
	@Override
    public boolean executeInSequence() {
        return false;
    }
}
