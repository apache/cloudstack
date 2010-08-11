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

public class GetVncPortAnswer extends Answer {
    int port;
    
    protected GetVncPortAnswer() {
    }
    
    public GetVncPortAnswer(GetVncPortCommand cmd, int port) {
        super(cmd, true, null);
        this.port = port;
    }
    
    public GetVncPortAnswer(GetVncPortCommand cmd, String details) {
        super(cmd, false, details);
    }
    
    public int getPort() {
        return port;
    }
}
