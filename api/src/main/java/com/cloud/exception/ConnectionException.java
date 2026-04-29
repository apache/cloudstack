// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package com.cloud.exception;

import com.cloud.utils.SerialVersionUID;

/**
 * ConnectionException is thrown by Listeners while processing the startup
 * command.  There are two uses for this exception and they are distinguished
 *   1. If the flag is set to true, there is an unexpected error during the
 *      processing.  Upon receiving this exception, the AgentManager will
 *      immediately place the agent under alert.  When the function to enable
 *      to disable the agent, the agent is disabled.
 *      should be disconnected and reconnected to "refresh" all resource
 *      information.  This is useful when the Listener needed to perform setup
 *      on the agent and decided it is best to flush connection and reconnect.
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
