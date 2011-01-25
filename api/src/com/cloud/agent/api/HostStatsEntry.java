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
public class HostStatsEntry implements HostStats {
	
	long hostId;
	String entityType;
	double cpuUtilization;
	double networkReadKBs;
	double networkWriteKBs;
    double totalMemoryKBs;
    double freeMemoryKBs;
    
    public HostStatsEntry() {
    }
    
    public HostStatsEntry(long hostId,double cpuUtilization, double networkReadKBs, double networkWriteKBs, String entityType,
		double totalMemoryKBs, double freeMemoryKBs, double xapiMemoryUsageKBs, double averageLoad) 
    {
        this.hostId = hostId;
        this.entityType = entityType;
        this.cpuUtilization = cpuUtilization;
        this.networkReadKBs = networkReadKBs;
        this.networkWriteKBs = networkWriteKBs;
        this.totalMemoryKBs = totalMemoryKBs;
        this.freeMemoryKBs = freeMemoryKBs;
    }

	@Override
    public double getNetworkReadKBs() {
    	return networkReadKBs;
    }
    
    public void setNetworkReadKBs(double networkReadKBs) {
    	this.networkReadKBs = networkReadKBs;
    }
    
	@Override
    public double getNetworkWriteKBs() {
    	return networkWriteKBs;
    }
    
    public void setNetworkWriteKBs(double networkWriteKBs) {
    	this.networkWriteKBs = networkWriteKBs;
    }

	@Override
    public String getEntityType(){
    	return this.entityType;
    }
    
    public void setEntityType(String entityType){
    	this.entityType = entityType;
    }
        
	@Override
    public double getTotalMemoryKBs(){
    	return this.totalMemoryKBs;
    }
    
    public void setTotalMemoryKBs(double totalMemoryKBs){
    	this.totalMemoryKBs = totalMemoryKBs;
    }

	@Override
    public double getFreeMemoryKBs(){
    	return this.freeMemoryKBs;
    }
    
    public void setFreeMemoryKBs(double freeMemoryKBs){
    	this.freeMemoryKBs = freeMemoryKBs;
    }
    
	@Override
	public double getCpuUtilization() {
		return this.cpuUtilization;
	}

	public void setCpuUtilization(double cpuUtilization) {
		this.cpuUtilization = cpuUtilization;
	}

	@Override
	public double getUsedMemory() {
		return (totalMemoryKBs-freeMemoryKBs);
	}

	@Override
	public HostStats getHostStats() {
		return this;
	}
	
	public void setHostId(long hostId) {
		this.hostId = hostId;
	}
}
