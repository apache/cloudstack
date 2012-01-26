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
package com.cloud.agent.api.check;

import com.cloud.agent.api.Command;

public class CheckSshCommand extends Command {
    String ip;
    int port;
    int interval;
    int retries;
    String name;
    
    protected CheckSshCommand() {
        super();
    }
    
    public CheckSshCommand(String instanceName, String ip, int port) {
        super();
        this.ip = ip;
        this.port = port;
        this.interval = 6;
        this.retries = 100;
        this.name = instanceName;
    }
    
    public String getName() {
        return name;
    }

    public String getIp() {
        return ip;
    }

    public int getPort() {
        return port;
    }

    public int getInterval() {
        return interval;
    }

    public int getRetries() {
        return retries;
    }

    @Override
    public boolean executeInSequence() {
        return false;
    }
}
