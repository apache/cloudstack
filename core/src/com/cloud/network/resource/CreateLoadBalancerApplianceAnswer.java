/**
 *  Copyright (C) 2011 Citrix Systems, Inc.  All rights reserved.
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

package com.cloud.network.resource;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.resource.ServerResource;

public class CreateLoadBalancerApplianceAnswer extends Answer {
    String deviceName;
    String providerName;
    ServerResource serverResource;
    String username;
    String password;
    String publicInterface;
    String privateInterface;

    public CreateLoadBalancerApplianceAnswer(Command cmd, boolean success, String details, String deviceName, String providerName, ServerResource serverResource, 
    		String publicInterface, String privateInterface, String username, String password) {
        this.deviceName = deviceName;
        this.providerName = providerName;
        this.serverResource = serverResource;
        this.result = success;
        this.details = details;
        this.username = username;
        this.password = password;
        this.publicInterface = publicInterface;
        this.privateInterface = privateInterface;
    }

    public String getDeviceName() {
        return deviceName;
    }
    
    public String getProviderName() {
        return providerName;
    }
    
    public ServerResource getServerResource() {
        return serverResource;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getPublicInterface() {
    	return publicInterface;
    }

    public String getPrivateInterface() {
    	return privateInterface;
    }
}
