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

public class GetRemoteVmsCommand extends Command {

    String remoteIp;
    String username;
    @LogLevel(LogLevel.Log4jLevel.Off)
    String password;

    public GetRemoteVmsCommand(String remoteIp, String username, String password) {
        this.remoteIp = remoteIp;
        this.username = username;
        this.password = password;
    }

    public String getRemoteIp() {
        return remoteIp;
    }

    public void setRemoteIp(String remoteIp) {
        this.remoteIp = remoteIp;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public GetRemoteVmsCommand() {
    }

    @Override
    public boolean executeInSequence() {
        return false;
    }

    public String getString() {
        return "GetRemoteVmsCommand [remoteIp=" + remoteIp + "]";
    }
}
