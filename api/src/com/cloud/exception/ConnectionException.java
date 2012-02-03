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
package com.cloud.exception;

import com.cloud.utils.SerialVersionUID;

/**
 * ConnectionException is thrown by Listeners while processing the startup
 * command.  There are two uses for this exception and they are distinguished
 * by the boolean flag.
 *   1. If the flag is set to true, there is an unexpected error during the
 *      processing.  Upon receiving this exception, the AgentManager will
 *      immediately place the agent under alert.  When the function to enable
 *      to disable the agent, the agent is disabled.
 *   2. If the flag is set to false, the listener has decided that the resource
 *      should be disconnected and reconnected to "refresh" all resource 
 *      information.  This is useful when the Listener needed to perform setup
 *      on the agent and decided it is best to flush connection and reconnect.
 *      It is important that the Listener does not fall into a loop in this
 *      situation where it keeps throwing ConnectionException.
 */
public class ConnectionException extends CloudException {
    
    private static final long serialVersionUID = SerialVersionUID.ConnectionException;
    boolean _error;

    public ConnectionException(boolean setupError, String msg) {
        this(setupError, msg, null);
    }
    
    public ConnectionException(boolean setupError, String msg, Throwable cause) {
        super(msg, cause);
        _error = setupError;
        
    }
    
    public boolean isSetupError() {
        return _error;
    }

}
