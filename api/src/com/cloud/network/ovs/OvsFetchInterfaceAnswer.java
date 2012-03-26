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

package com.cloud.network.ovs;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;

public class OvsFetchInterfaceAnswer extends Answer {
	String ip;
    String netmask;
    String mac;
    String label;

    public OvsFetchInterfaceAnswer(Command cmd, boolean success, String details) {
        super(cmd, success, details);
        this.label = ((OvsFetchInterfaceCommand)cmd).getLabel();
    }

    public OvsFetchInterfaceAnswer(Command cmd, boolean success,
            String details, String ip, String netmask, String mac) {
        super(cmd, success, details);
        this.ip = ip;
        this.netmask = netmask;
        this.mac = mac;
        this.label = ((OvsFetchInterfaceCommand)cmd).getLabel();
    }

    public String getIp() {
        return ip;
    }

    public String getNetmask() {
        return netmask;
    }

    public String getMac() {
        return mac;
    }

    public String getLabel() {
    	return label;
    }
}
