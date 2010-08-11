/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
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

import com.cloud.host.HostStats;

/**
 * @author ajoshi
 *
 */
public class GetHostStatsAnswer extends Answer implements HostStats {
	
	HostStatsEntry hostStats;
	
	double cpuUtilization;
	double networkReadKBs;
	double networkWriteKBs;
	int numCPUs;
	String entityType;
    double totalMemoryKBs;
    double freeMemoryKBs;
    double xapiMemoryUsageKBs;    
	double averageLoad;
	
    protected GetHostStatsAnswer() {
    }
        
	public GetHostStatsAnswer(GetHostStatsCommand cmd, HostStatsEntry hostStatistics) {
		super(cmd);
		this.hostStats = hostStatistics;
	}
	
    public GetHostStatsAnswer(GetHostStatsCommand cmd, double cpuUtilization, double freeMemoryKBs, double totalMemoryKBs, double networkReadKBs, 
    		double networkWriteKBs, String entityType, double xapiMemoryUsageKBs, double averageLoad, int numCPUs) {
        super(cmd);
    
        this.cpuUtilization = cpuUtilization;
        this.freeMemoryKBs = freeMemoryKBs;
        this.totalMemoryKBs = totalMemoryKBs;
        this.networkReadKBs = networkReadKBs;
        this.networkWriteKBs = networkWriteKBs;
        this.entityType = entityType;
        this.xapiMemoryUsageKBs = xapiMemoryUsageKBs;
        this.numCPUs = numCPUs;
    }
    
    @Override
    public double getUsedMemory() {
    	return (totalMemoryKBs - freeMemoryKBs);
    }
    
    @Override
    public double getFreeMemoryKBs() {
        return freeMemoryKBs;
    }
    
    @Override
    public double getTotalMemoryKBs() {
    	return totalMemoryKBs;
    }
    
    @Override
    public double getCpuUtilization() {
        return cpuUtilization;
    }
    
    @Override
    public double getNetworkReadKBs() {
    	return networkReadKBs;
    }
    
    @Override
    public double getNetworkWriteKBs() {
    	return networkWriteKBs;
    }

	@Override
	public double getAverageLoad() {
		return averageLoad;
	}

	@Override
	public String getEntityType() {
		return entityType;
	}

	@Override
	public double getXapiMemoryUsageKBs() {
		return xapiMemoryUsageKBs;
	}

	@Override
	public int getNumCpus(){
		return numCPUs;
	}

	@Override
	public HostStats getHostStats() {
		return hostStats;
	}
}
