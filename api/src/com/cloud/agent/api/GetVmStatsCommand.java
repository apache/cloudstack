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

import java.util.List;

import com.cloud.vm.VirtualMachine;

public class GetVmStatsCommand extends Command {
    List<String> vmNames;
    String hostGuid;
    String hostName;
    
    protected GetVmStatsCommand() {
    }
    
    public GetVmStatsCommand(List<String> vmNames, String hostGuid, String hostName) {
        this.vmNames = vmNames;
        this.hostGuid = hostGuid;
        this.hostName = hostName;
    }
    
    public List<String> getVmNames() {
        return vmNames;
    }
    
    public String getHostGuid(){
    	return this.hostGuid;
    }
    
    public String getHostName(){
    	return this.hostName;
    }
    
    @Override
    public boolean executeInSequence() {
        return false;
    }
}
