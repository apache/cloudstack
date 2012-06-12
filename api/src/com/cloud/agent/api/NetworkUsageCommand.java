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
package com.cloud.agent.api;

import com.cloud.agent.api.LogLevel.Log4jLevel;

@LogLevel(Log4jLevel.Trace)
public class NetworkUsageCommand extends Command {
    private String privateIP;
    private String domRName;
    private String option;

    protected NetworkUsageCommand() {

    }

    public NetworkUsageCommand(String privateIP, String domRName)
    {
        this.privateIP = privateIP;
        this.domRName = domRName;
    }

    public NetworkUsageCommand(String privateIP, String domRName, String option)
    {
        this.privateIP = privateIP;
        this.domRName = domRName;
        this.option = option;
    }

    public String getPrivateIP() {
        return privateIP;
    }

    public String getDomRName() {
        return domRName;
    }

    public String getOption() {
        return option;
    }

    @Override
    public boolean executeInSequence() {
        return false;
    }
}