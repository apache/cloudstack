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
package com.cloud.capacity;

/**
 * @author ahuang
 * 
 */
public interface Capacity {
    public static final short CAPACITY_TYPE_MEMORY = 0;
    public static final short CAPACITY_TYPE_CPU = 1;
    public static final short CAPACITY_TYPE_STORAGE = 2;
    public static final short CAPACITY_TYPE_STORAGE_ALLOCATED = 3;
    public static final short CAPACITY_TYPE_VIRTUAL_NETWORK_PUBLIC_IP = 4;
    public static final short CAPACITY_TYPE_PRIVATE_IP = 5;
    public static final short CAPACITY_TYPE_SECONDARY_STORAGE = 6;
    public static final short CAPACITY_TYPE_VLAN = 7;
    public static final short CAPACITY_TYPE_DIRECT_ATTACHED_PUBLIC_IP = 8;
    public static final short CAPACITY_TYPE_LOCAL_STORAGE = 9;

    public long getId();

    public Long getHostOrPoolId();

    public Long getDataCenterId();

    public Long getPodId();

    public Long getClusterId();

    public long getUsedCapacity();

    public long getTotalCapacity();

    public short getCapacityType();    

    public long getReservedCapacity();
    
    public Float getUsedPercentage();


}
