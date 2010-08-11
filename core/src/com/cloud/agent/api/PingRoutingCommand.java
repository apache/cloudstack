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

import java.util.Map;

import com.cloud.host.Host;
import com.cloud.vm.State;

public class PingRoutingCommand extends PingCommand {
    Map<String, State> newStates;
    boolean _gatewayAccessible = true;
    boolean _vnetAccessible = true;
   
    protected PingRoutingCommand() {
    }
    
    public PingRoutingCommand(Host.Type type, long id, Map<String, State> states) {
        super(type, id);
        this.newStates = states;
    }
    
    public Map<String, State> getNewStates() {
        return newStates;
    }

    public boolean isGatewayAccessible() {
        return _gatewayAccessible;
    }
    public void setGatewayAccessible(boolean gatewayAccessible) {
        _gatewayAccessible = gatewayAccessible;
    }

    public boolean isVnetAccessible() {
        return _vnetAccessible;
    }
    public void setVnetAccessible(boolean vnetAccessible) {
        _vnetAccessible = vnetAccessible;
    }
}
