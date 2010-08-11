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

package com.cloud.utils;

/**
 * SerialVersionUID is used to group the UIDs created for serialization
 * purposes.  This is purely on an honor system though.  You should always
 * specify the constant in this file and then use the constant variable
 * name in your file for the serial version id.  I'll try to think of
 * something better later but be on your best behavior.
 **/
public interface SerialVersionUID {
    public static final long Base = 0x564D4F70 << 32;  // 100 brownie points if you guess what this is and tell me.
    
    public static final long UUID = Base | 0x1;
    public static final long CloudRuntimeException = Base | 0x2;
    public static final long CloudStartupServlet = Base | 0x3;
    public static final long CloudServiceImpl = Base | 0x4;
    public static final long UserRemote = Base | 0x5;
    public static final long ServiceOfferingRemote = Base | 0x6;
    public static final long VMTemplateRemote = Base | 0x7;
    public static final long VMInstanceRemote = Base | 0x8;
    public static final long IPAddressRemote = Base | 0x9;
    public static final long IPForwardingRemote = Base | 0xa;
    public static final long UnsupportedVersionException = Base | 0xb;
    public static final long DataCenterIpAddressPK = Base | 0xc;
    public static final long UnableToExecuteException = Base | 0xd;
    public static final long ExecutionException = Base | 0xe;
    public static final long VnetKey = Base | 0xf;
    public static final long InsufficientServerCapacityException = Base | 0x10;
    public static final long InsufficientAddressCapacityException = Base | 0x11;
    public static final long ManagementServerException = Base | 0x12;
    public static final long HAStateException = Base | 0x13;
    public static final long InsufficientStorageCapacityException = Base | 0x14;
    public static final long InsufficientCapacityException = Base | 0x15;
    public static final long ConcurrentOperationException = Base | 0x16;
    public static final long AgentUnavailableException = Base | 0x17;
    public static final long OperationTimedoutException = Base | 0x18;
    public static final long StorageUnavailableException = Base | 0x19;
    public static final long InfficientVirtualNetworkCapacityException = Base | 0x1a;
    public static final long DiscoveryException = Base | 0x1b;
}
