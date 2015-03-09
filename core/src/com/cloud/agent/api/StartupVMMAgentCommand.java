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

import com.cloud.host.Host;

/**
 * Implementation of bootstrap command sent from management server to agent running on
 * System Center Virtual Machine Manager host
 **/

public class StartupVMMAgentCommand extends Command {
    Host.Type type;
    long dataCenter;
    Long pod;
    String clusterName;
    String guid;
    String managementServerIP;
    String port;
    String version;

    public StartupVMMAgentCommand() {

    }

    public StartupVMMAgentCommand(long dataCenter, Long pod, String clusterName, String guid, String managementServerIP, String port, String version) {
        super();
        this.dataCenter = dataCenter;
        this.pod = pod;
        this.clusterName = clusterName;
        this.guid = guid;
        this.type = Host.Type.Routing;
        this.managementServerIP = managementServerIP;
        this.port = port;
    }

    public long getDataCenter() {
        return dataCenter;
    }

    public Long getPod() {
        return pod;
    }

    public String getClusterName() {
        return clusterName;
    }

    public String getGuid() {
        return guid;
    }

    public String getManagementServerIP() {
        return managementServerIP;
    }

    public String getport() {
        return port;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    @Override
    public boolean executeInSequence() {
        return false;
    }
}