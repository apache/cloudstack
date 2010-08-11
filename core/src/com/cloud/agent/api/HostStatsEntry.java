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
	double cpuUtilization;
	double networkReadKBs;
	double networkWriteKBs;
	int numCpus;
	String entityType;
    double totalMemoryKBs;
    double freeMemoryKBs;
    double xapiMemoryUsageKBs;    
	double averageLoad;
    
    public HostStatsEntry() {
    }
    
    public HostStatsEntry(long hostId,double cpuUtilization, double networkReadKBs, double networkWriteKBs, int numCPUs, String entityType,
    						double totalMemoryKBs, double freeMemoryKBs, double xapiMemoryUsageKBs, double averageLoad) 
    {
        this.cpuUtilization = cpuUtilization;
        this.networkReadKBs = networkReadKBs;
        this.networkWriteKBs = networkWriteKBs;
        this.numCpus = numCPUs;
        this.entityType = entityType;
        this.totalMemoryKBs = totalMemoryKBs;
        this.freeMemoryKBs = freeMemoryKBs;
        this.xapiMemoryUsageKBs = xapiMemoryUsageKBs;
        this.averageLoad = averageLoad;
        this.hostId = hostId;
    }

	@Override
    public double getAverageLoad() {
    	return averageLoad;
    }
    
    public void setAverageLoad(double averageLoad) {
    	this.averageLoad = averageLoad;
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
        
    public void setNumCpus(int numCpus) {
    	this.numCpus = numCpus;
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
    public double getXapiMemoryUsageKBs(){
    	return this.xapiMemoryUsageKBs;
    }
    
    public void setXapiMemoryUsageKBs(double xapiMemoryUsageKBs){
    	this.xapiMemoryUsageKBs = xapiMemoryUsageKBs;
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
	public int getNumCpus() {
		return numCpus;
	}

	@Override
	public HostStats getHostStats() {
		// TODO Auto-generated method stub
		return null;
	}
}
