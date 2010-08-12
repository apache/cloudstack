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
package com.cloud.vm;

import java.util.Date;

public interface VirtualMachine {
    public enum Event {
    	CreateRequested,
    	StartRequested,
    	StopRequested,
    	DestroyRequested,
    	RecoveryRequested,
    	AgentReportStopped,
    	AgentReportRunning,
    	MigrationRequested,
    	ExpungeOperation,
    	OperationSucceeded,
    	OperationFailed,
    	OperationRetry,
    	OperationCancelled
    };
    
    public enum Type {
        User,
        DomainRouter,
        ConsoleProxy,
        SecondaryStorageVm
    }
    
    public String getInstanceName();
    
    /**
     * @return the id of this virtual machine.  null means the id has not been set.
     */
    public Long getId();
    
    /**
     * @return the name of the virtual machine.
     */
    public String getName();
    
    /**
     * @return the ip address of the virtual machine.
     */
    public String getPrivateIpAddress();
    
    /**
     * @return mac address.
     */
    public String getPrivateMacAddress();
    
    /**
     * @return password of the host for vnc purposes.
     */
    public String getVncPassword();
    
    /**
     * @return the state of the virtual machine
     */
    public State getState();
    
    /**
     * @return template id.
     */
    public long getTemplateId();

    /**
     * return iso id
     * @return
     */
    public Long getIsoId();

    /**
     * update the id of the iso attached to this vm (null = no iso attached)
     * @param isoId
     */
    public void setIsoId(Long isoId);
    
    /**
     * returns the guest OS ID
     * @return guestOSId
     */
    public long getGuestOSId();
    
    /**
     * sets the guest OS ID
     * @param guestOSId
     */
    public void setGuestOSId(long guestOSId);

    /**
     * @return pod id.
     */
    public long getPodId();
    
    /**
     * @return data center id.
     */
    public long getDataCenterId();
    
    /**
     * @return id of the host it is running on.  If not running, returns null.
     */
    public Long getHostId();
    
    /**
     * @return id of the host it was assigned last time.
     */
    public Long getLastHostId();

    /**
     * @return should HA be enabled for this machine?
     */
    public boolean isHaEnabled();

	/**
	 * @return are the volumes on this VM mirrored?
	 */
	public boolean isMirroredVols();
	
	/**
     * @return date when machine was created
     */
	public Date getCreated();
	
	Type getType();
	
    /**
     * @return display name of the vm.
     */
    public String getDisplayName();
    
    /**
     * @return group of the vm.
     */
    public String getGroup();
}
