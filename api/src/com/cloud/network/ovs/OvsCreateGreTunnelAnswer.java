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

public class OvsCreateGreTunnelAnswer extends Answer {
    String hostIp;
    String remoteIp;
    String bridge;
    String key;
    long from;
    long to;
    int port;

    public OvsCreateGreTunnelAnswer(Command cmd, boolean success, String details) {
        super(cmd, success, details);
    }

    public OvsCreateGreTunnelAnswer(Command cmd, boolean success,
            String details, String hostIp, String bridge) {
        super(cmd, success, details);
        OvsCreateGreTunnelCommand c = (OvsCreateGreTunnelCommand) cmd;
        this.hostIp = hostIp;
        this.bridge = bridge;
        this.remoteIp = c.getRemoteIp();
        this.key = c.getKey();
        this.port = -1;
        this.from = c.getFrom();
        this.to = c.getTo();
    }

    public OvsCreateGreTunnelAnswer(Command cmd, boolean success,
            String details, String hostIp, String bridge, int port) {
        this(cmd, success, details, hostIp, bridge);
        this.port = port;
    }

    public String getHostIp() {
        return hostIp;
    }

    public String getRemoteIp() {
        return remoteIp;
    }

    public String getBridge() {
        return bridge;
    }

    public String getKey() {
        return key;
    }

    public long getFrom() {
        return from;
    }

    public long getTo() {
        return to;
    }

    public int getPort() {
        return port;
    }
}
