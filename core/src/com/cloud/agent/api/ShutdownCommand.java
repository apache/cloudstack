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

/**
 * This command informs the server that the agent is shutting down.
 *
 */
public class ShutdownCommand extends Command {
    public static final String Requested = "sig.kill";
    public static final String Update = "update";
    public static final String Unknown = "unknown";
    
    private String reason;
    private String detail;

    protected ShutdownCommand() {
    }
    
    public ShutdownCommand(String reason, String detail) {
        this.reason = reason;
        this.detail = detail;
    }
    
    /**
     * @return return the reason the agent shutdown.  If Unknown, call getDetail() for any details. 
     */
    public String getReason() {
        return reason;
    }
    
    public String getDetail() {
        return detail;
    }
    
    @Override
    public boolean executeInSequence() {
        return false;
    }
}
