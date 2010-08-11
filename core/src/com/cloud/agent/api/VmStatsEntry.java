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

import com.cloud.vm.VmStats;

/**
 * @author ahuang
 *
 */
public class VmStatsEntry implements VmStats {
	
	double cpuUtilization;
	double networkReadKBs;
	double networkWriteKBs;
	int numCPUs;	
	String entityType;
	    
    public VmStatsEntry() {
    }
    
    public VmStatsEntry(double cpuUtilization, double networkReadKBs, double networkWriteKBs, int numCPUs, String entityType) 
    {
        this.cpuUtilization = cpuUtilization;
        this.networkReadKBs = networkReadKBs;
        this.networkWriteKBs = networkWriteKBs;
        this.numCPUs = numCPUs;
        this.entityType = entityType;
    }

    public double getCPUUtilization() {
    	return cpuUtilization;
    }
    
    public void setCPUUtilization(double cpuUtilization) {
    	this.cpuUtilization = cpuUtilization;
    }

    public double getNetworkReadKBs() {
    	return networkReadKBs;
    }
    
    public void setNetworkReadKBs(double networkReadKBs) {
    	this.networkReadKBs = networkReadKBs;
    }
    
    public double getNetworkWriteKBs() {
    	return networkWriteKBs;
    }
    
    public void setNetworkWriteKBs(double networkWriteKBs) {
    	this.networkWriteKBs = networkWriteKBs;
    }
    
    public int getNumCPUs() {
    	return numCPUs;
    }
    
    public void setNumCPUs(int numCPUs) {
    	this.numCPUs = numCPUs;
    }

    public String getEntityType(){
    	return this.entityType;
    }
    
    public void setEntityType(String entityType){
    	this.entityType = entityType;
    }
    

}
