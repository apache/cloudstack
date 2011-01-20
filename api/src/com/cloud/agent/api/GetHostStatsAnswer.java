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

import com.cloud.host.HostStats;

/**
 * @author ajoshi
 *
 */
public class GetHostStatsAnswer extends Answer implements HostStats {
	
	HostStatsEntry hostStats;

    protected GetHostStatsAnswer() {
    	hostStats = new HostStatsEntry();
    }
        
	public GetHostStatsAnswer(GetHostStatsCommand cmd, HostStatsEntry hostStatistics) {
		super(cmd);
		this.hostStats = hostStatistics;
	}
	
    public GetHostStatsAnswer(GetHostStatsCommand cmd, double cpuUtilization, double freeMemoryKBs, double totalMemoryKBs, double networkReadKBs, 
    		double networkWriteKBs, String entityType) {
        super(cmd);
        hostStats = new HostStatsEntry();
        
        hostStats.setCpuUtilization(cpuUtilization);
        hostStats.setFreeMemoryKBs(freeMemoryKBs);
        hostStats.setTotalMemoryKBs(totalMemoryKBs);
        hostStats.setNetworkReadKBs(networkReadKBs);
        hostStats.setNetworkWriteKBs(networkWriteKBs);
        hostStats.setEntityType(entityType);
    }
    
    @Override
    public double getUsedMemory() {
    	return hostStats.getUsedMemory();
    }
    
    @Override
    public double getFreeMemoryKBs() {
        return hostStats.getFreeMemoryKBs();
    }
    
    @Override
    public double getTotalMemoryKBs() {
    	return hostStats.getTotalMemoryKBs();
    }
    
    @Override
    public double getCpuUtilization() {
        return hostStats.getCpuUtilization();
    }
    
    @Override
    public double getNetworkReadKBs() {
    	return hostStats.getNetworkReadKBs();
    }
    
    @Override
    public double getNetworkWriteKBs() {
    	return hostStats.getNetworkWriteKBs();
    }

	@Override
	public String getEntityType() {
		return hostStats.getEntityType();
	}

	@Override
	public HostStats getHostStats() {
		return hostStats;
	}
}
