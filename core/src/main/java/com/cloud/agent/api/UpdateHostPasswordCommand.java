//
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
//

package com.cloud.agent.api;

import com.cloud.agent.api.LogLevel.Log4jLevel;

public class UpdateHostPasswordCommand extends Command {

    @LogLevel(Log4jLevel.Off)
    protected String username;
    @LogLevel(Log4jLevel.Off)
    protected String newPassword;
    @LogLevel(Log4jLevel.Off)
    protected String hostIp;


    protected UpdateHostPasswordCommand() {
    }

    public UpdateHostPasswordCommand(final String username, final String newPassword) {
        this(username, newPassword, null);
    }

    public UpdateHostPasswordCommand(final String username, final String newPassword, final String hostIp) {
        this.username = username;
        this.newPassword = newPassword;
        this.hostIp = hostIp;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public String getUsername() {
        return username;
    }

    public String getHostIp() {
        return hostIp;
    }

    @Override
    public boolean executeInSequence() {
        return false;
    }
}