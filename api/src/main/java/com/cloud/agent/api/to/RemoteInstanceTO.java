/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.cloud.agent.api.to;

import com.cloud.agent.api.LogLevel;
import com.cloud.hypervisor.Hypervisor;

import java.io.Serializable;

public class RemoteInstanceTO implements Serializable {

    private Hypervisor.HypervisorType hypervisorType;
    private String hostName;
    private String instanceName;

    // Vmware Remote Instances parameters
    // TODO: cloud.agent.transport.Request#getCommands() cannot handle gsoc decode for polymorphic classes
    private String vcenterUsername;
    @LogLevel(LogLevel.Log4jLevel.Off)
    private String vcenterPassword;
    private String vcenterHost;
    private String datacenterName;
    private String clusterName;

    public RemoteInstanceTO() {
    }

    public RemoteInstanceTO(String hostName, String instanceName, String vcenterHost,
                            String datacenterName, String clusterName,
                            String vcenterUsername, String vcenterPassword) {
        this.hypervisorType = Hypervisor.HypervisorType.VMware;
        this.hostName = hostName;
        this.instanceName = instanceName;
        this.vcenterHost = vcenterHost;
        this.datacenterName = datacenterName;
        this.clusterName = clusterName;
        this.vcenterUsername = vcenterUsername;
        this.vcenterPassword = vcenterPassword;
    }

    public Hypervisor.HypervisorType getHypervisorType() {
        return this.hypervisorType;
    }

    public String getInstanceName() {
        return this.instanceName;
    }

    public String getHostName() {
        return this.hostName;
    }

    public String getVcenterUsername() {
        return vcenterUsername;
    }

    public String getVcenterPassword() {
        return vcenterPassword;
    }

    public String getVcenterHost() {
        return vcenterHost;
    }

    public String getDatacenterName() {
        return datacenterName;
    }

    public String getClusterName() {
        return clusterName;
    }
}
