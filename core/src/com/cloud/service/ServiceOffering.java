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
package com.cloud.service;

/**
 * ServiceOffering models the different types of service contracts to be 
 * offered.
 */
public interface ServiceOffering {
	public enum GuestIpType {
		Virtualized,
		DirectSingle,
		DirectDual
	}
	
    /**
     * @return user readable description
     */
    String getName();
    
    /**
     * @return # of cpu.
     */
    int getCpu();
    
    /**
     * @return speed in mhz
     */
    int getSpeed();
    
    /**
     * @return ram size in megabytes
     */
    int getRamSize();

    /**
     * @return Does this service plan offer HA?
     */
    boolean getOfferHA();
    
    /**
     * @return the rate in megabits per sec to which a VM's network interface is throttled to
     */
    int getRateMbps();
    
    /**
     * @return the rate megabits per sec to which a VM's multicast&broadcast traffic is throttled to
     */
    int getMulticastRateMbps();
    
    /**
     * @return the type of IP address to allocate as the primary ip address to a guest
     */
    GuestIpType getGuestIpType();
    
    /**
     * @return whether or not the service offering requires local storage
     */
    boolean getUseLocalStorage();

}
