/**
 *  Copyright (C) 2011 Cloud.com, Inc.  All rights reserved.
 */

package com.cloud.agent.api;

import java.util.Date;
import java.util.List;

public class DirectNetworkUsageCommand extends Command {
    
    private List<String> publicIps;
    private Date start;
    private Date end;

	public DirectNetworkUsageCommand(List<String> publicIps, Date start, Date end) {
	    this.setPublicIps(publicIps);
	    this.setStart(start);
	    this.setEnd(end);
    }
	
	@Override
    public boolean executeInSequence() {
        return false;
    }

    public void setPublicIps(List<String> publicIps) {
        this.publicIps = publicIps;
    }

    public List<String> getPublicIps() {
        return publicIps;
    }

    public void setStart(Date start) {
        this.start = start;
    }

    public Date getStart() {
        return start;
    }

    public void setEnd(Date end) {
        this.end = end;
    }

    public Date getEnd() {
        return end;
    }
}
