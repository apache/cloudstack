/**
 *  Copyright (C) 2011 Cloud.com, Inc.  All rights reserved.
 */

package com.cloud.agent.api;


public class RecurringNetworkUsageAnswer extends Answer {
	

	protected RecurringNetworkUsageAnswer() {
    }
	
	public RecurringNetworkUsageAnswer(Command command) {
		super(command);
	}
	
	public RecurringNetworkUsageAnswer(Command command, Exception e) {
		super(command, e);
	}
	
}
