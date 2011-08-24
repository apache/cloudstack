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

package com.cloud.agent.api;

import com.cloud.agent.api.Command;
import com.cloud.host.Host;

/**
 * Implementation of bootstrap command sent from management server to agent running on
 * System Center Virtual Machine Manager host 
 **/

public class StartupVMMAgentCommand extends Command {
    Host.Type type;
    long dataCenter;
    Long pod;
    String clusterName;
    String guid;
    String managementServerIP;
    String port;
    String version;
    
    public StartupVMMAgentCommand() {
    	
    }

    public StartupVMMAgentCommand(long dataCenter, Long pod, String clusterName, String guid, String managementServerIP, String port, String version) {
        super();
        this.dataCenter = dataCenter;
        this.pod = pod;
        this.clusterName = clusterName;
        this.guid = guid;
        this.type = Host.Type.Routing;
        this.managementServerIP = managementServerIP;
        this.port = port;
    }

    public long getDataCenter() {
    	return dataCenter;
    }
 
    public Long getPod() {
    	return pod;
    }

    public String getClusterName() {
    	return clusterName;
    }
    
    public String getGuid() {
    	return guid;
    }

    public String getManagementServerIP() {
    	return managementServerIP;
    }

    public String getport() {
    	return port;
    }
    
    public void setVersion(String version) {
        this.version = version;
    }

    @Override
    public boolean executeInSequence() {
        return false;
    }
}