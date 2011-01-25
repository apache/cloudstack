/**
 *  Copyright (C) 2010 VMOps, Inc.  All rights reserved.
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
package com.cloud.host;


/**
 *  HostEnvironment is a in memory object that describes
 *  the networking environment of the host.  The
 *  information may be incomplete and particularly for
 *  the first host in the server is definitely incomplete.
 *  This allows the server to fill out and compare
 *  the environment.
 */
public class HostEnvironment {
    
    public String managementIpAddress;
    public String managementNetmask;
    public String managementGateway;
    public String managementVlan;
    
    public String[] neighborHosts;
    
    public String storageIpAddress;
    public String storageNetwork;
    public String storageGateway;
    public String storageVlan;
    public String secondaryStroageIpAddress;
    
    public String storage2IpAddress;
    public String storage2Network;
    public String storage2Gateway;
    public String storage2Vlan;
    public String secondaryStorageIpAddress2;
    
    public String[] neighborStorages;
    public String[] neighborStorages2;
    
    public String publicIpAddress;
    public String publicNetmask;
    public String publicGateway;
    public String publicVlan;
}
