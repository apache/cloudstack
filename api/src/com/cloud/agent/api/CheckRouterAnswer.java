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

import com.cloud.network.router.VirtualRouter.RedundantState;

public class CheckRouterAnswer extends Answer {
    public static final String ROUTER_NAME = "router.name";
    public static final String ROUTER_IP = "router.ip";
    RedundantState state;
    boolean isBumped;
    
    protected CheckRouterAnswer() {
    }
    
    public CheckRouterAnswer(CheckRouterCommand cmd, String details, boolean parse) {
        super(cmd, true, details);
        if (parse) {
            if (!parseDetails(details)) {
                this.result = false;
            }
        }
	}
    
    public CheckRouterAnswer(CheckRouterCommand cmd, String details) {
        super(cmd, false, details);
    }

    protected boolean parseDetails(String details) {
        String[] lines = details.split("&");
        if (lines.length != 2) {
            return false;
        }
        if (lines[0].startsWith("Status: MASTER")) {
            state = RedundantState.MASTER;
        } else if (lines[0].startsWith("Status: BACKUP")) {
            state = RedundantState.BACKUP;
        } else if (lines[0].startsWith("Status: FAULT")) {
            state = RedundantState.FAULT;
        } else {
            state = RedundantState.UNKNOWN;
        }
        if (lines[1].startsWith("Bumped: YES")) {
            isBumped = true;
        } else {
            isBumped = false;
        }
        return true;
    }
    
    public void setState(RedundantState state) {
        this.state = state;
	}
    
    public RedundantState getState() {
        return state;
	}

    public boolean isBumped() {
        return isBumped;
    }
    
    public void setIsBumped(boolean isBumped) {
        this.isBumped = isBumped;
    }
    
}
