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
