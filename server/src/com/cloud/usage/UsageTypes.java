/**
 * *  Copyright (C) 2011 Citrix Systems, Inc.  All rights reserved
*
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

package com.cloud.usage;

import java.util.ArrayList;
import java.util.List;

import com.cloud.server.api.response.UsageTypeResponse;

public class UsageTypes {
    public static final int RUNNING_VM = 1;
    public static final int ALLOCATED_VM = 2; // used for tracking how long storage has been allocated for a VM
    public static final int IP_ADDRESS = 3;
    public static final int NETWORK_BYTES_SENT = 4;
    public static final int NETWORK_BYTES_RECEIVED = 5;
    public static final int VOLUME = 6;
    public static final int TEMPLATE = 7;
    public static final int ISO = 8;
    public static final int SNAPSHOT = 9;
    public static final int SECURITY_GROUP = 10;
    public static final int LOAD_BALANCER_POLICY = 11;
    public static final int PORT_FORWARDING_RULE = 12;
    public static final int NETWORK_OFFERING = 13;
    public static final int VPN_USERS = 14;
    
    public static List<UsageTypeResponse> listUsageTypes(){
    	List<UsageTypeResponse> responseList = new ArrayList<UsageTypeResponse>();
    	responseList.add(new UsageTypeResponse(RUNNING_VM, "Running Vm Usage"));
    	responseList.add(new UsageTypeResponse(ALLOCATED_VM, "Allocated Vm Usage"));
    	responseList.add(new UsageTypeResponse(IP_ADDRESS, "IP Address Usage"));
    	responseList.add(new UsageTypeResponse(NETWORK_BYTES_SENT, "Network Usage (Bytes Sent)"));
    	responseList.add(new UsageTypeResponse(NETWORK_BYTES_RECEIVED, "Network Usage (Bytes Received)"));
    	responseList.add(new UsageTypeResponse(VOLUME, "Volume Usage"));
    	responseList.add(new UsageTypeResponse(TEMPLATE, "Template Usage"));
    	responseList.add(new UsageTypeResponse(ISO, "ISO Usage"));
    	responseList.add(new UsageTypeResponse(SNAPSHOT, "Snapshot Usage"));
    	responseList.add(new UsageTypeResponse(SECURITY_GROUP, "Security Group Usage"));
    	responseList.add(new UsageTypeResponse(LOAD_BALANCER_POLICY, "Load Balancer Usage"));
    	responseList.add(new UsageTypeResponse(PORT_FORWARDING_RULE, "Port Forwarding Usage"));
    	responseList.add(new UsageTypeResponse(NETWORK_OFFERING, "Network Offering Usage"));
    	responseList.add(new UsageTypeResponse(VPN_USERS, "VPN users usage"));
    	return responseList;
    }
}
