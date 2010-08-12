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

/**
 * This represents one running virtual machine instance.
 */
public interface UserVm extends VirtualMachine {
    
    /**
     * @return service offering id
     */
    public long getServiceOfferingId();
    
    /**
     * @return the domain router associated with this vm.
     */
    public Long getDomainRouterId();
    
    /**
     * @return the vnet associated with this vm.
     */
    public String getVnet();
    
    /**
     * @return the account this vm instance belongs to.
     */
    public long getAccountId();

    /**
     * @return the domain this vm instance belongs to.
     */
    public long getDomainId();
    
    /**
     * @return ip address within the guest network.
     */
    public String getGuestIpAddress();
    
    /**
     * @return mac address of the guest network.
     */
    public String getGuestMacAddress();
}
