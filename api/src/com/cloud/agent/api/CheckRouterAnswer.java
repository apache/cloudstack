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

public class CheckRouterAnswer extends Answer {
    public static final String ROUTER_NAME = "router.name";
    public static final String ROUTER_IP = "router.ip";
    boolean isMaster;
    
    protected CheckRouterAnswer() {
    }
    
    public CheckRouterAnswer(CheckRouterCommand cmd, boolean isMaster, String details) {
        super(cmd, true, details);
		this.isMaster = isMaster;
    }

    public CheckRouterAnswer(CheckRouterCommand cmd, String details) {
        super(cmd, false, details);
	}

    public boolean getIsMaster() {
        return isMaster;
	}
    
    public void setIsMaster(boolean isMaster) {
        this.isMaster = isMaster;
    }
}
