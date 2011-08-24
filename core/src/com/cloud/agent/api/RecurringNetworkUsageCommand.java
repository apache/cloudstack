/**
 *  Copyright (C) 2011 Cloud.com, Inc.  All rights reserved.
 */

package com.cloud.agent.api;


public class RecurringNetworkUsageCommand extends Command implements CronCommand{
    int interval;
    
	public RecurringNetworkUsageCommand(int interval) {
        this.interval = interval;
    }
	
	public int getInterval() {
        return interval;
    }
	
	@Override
    public boolean executeInSequence() {
        return false;
    }

}
