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

import com.cloud.vm.ConsoleProxyVO;
import com.cloud.vm.VirtualMachine;

public class StopCommand extends RebootCommand {
    String vnet;
    boolean mirroredVolumes=false;
    private boolean isProxy=false;
    private String vncPort=null;
    private String urlPort=null;
    private String publicConsoleProxyIpAddress=null;
    
    protected StopCommand() {
    }
    
    public StopCommand(VirtualMachine vm, boolean isProxy, String vncPort, String urlPort, String publicConsoleProxyIpAddress) {
    	super(vm);
    	this.isProxy = isProxy;
    	this.vncPort = vncPort;
    	this.urlPort = urlPort;
    	this.publicConsoleProxyIpAddress = publicConsoleProxyIpAddress;
    }
    
    public StopCommand(VirtualMachine vm, String vnet) {
        super(vm);
        this.vnet = vnet;
        this.mirroredVolumes = vm.isMirroredVols();
    }
    
    public StopCommand(VirtualMachine vm, String vmName, String vnet) {
        super(vmName);
        this.vnet = vnet;
        if (vm != null) {
            this.mirroredVolumes = vm.isMirroredVols();
        }
    }
    
    public String getVnet() {
        return vnet;
    }
    
    @Override
    public boolean executeInSequence() {
    	//Temporary relaxing serialization
        return false;
    }

	public boolean isMirroredVolumes() {
		return mirroredVolumes;
	}

	public void setMirroredVolumes(boolean mirroredVolumes) {
		this.mirroredVolumes = mirroredVolumes;
	}
	
	public boolean isProxy() {
		return this.isProxy;
	}
	
	public String getVNCPort() {
		return this.vncPort;
	}
	
	public String getURLPort() {
		return this.urlPort;
	}
	
	public String getPublicConsoleProxyIpAddress() {
		return this.publicConsoleProxyIpAddress;
	}
    
}
