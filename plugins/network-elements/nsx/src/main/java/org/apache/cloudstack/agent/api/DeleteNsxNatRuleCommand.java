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
package org.apache.cloudstack.agent.api;

import com.cloud.network.Network;

import java.util.Objects;

public class DeleteNsxNatRuleCommand extends NsxNetworkCommand {
    private Long ruleId;
    private Network.Service service;

    private String privatePort;
    private String protocol;
    public DeleteNsxNatRuleCommand(long domainId, long accountId, long zoneId, Long networkResourceId, String networkResourceName,
                                   boolean isResourceVpc, Long vmId, Long ruleId, String privatePort, String protocol) {
        super(domainId, accountId, zoneId, networkResourceId, networkResourceName, isResourceVpc, vmId);
        this.ruleId = ruleId;
        this.privatePort = privatePort;
        this.protocol = protocol;
    }

    public Long getRuleId() {
        return ruleId;
    }

    public Network.Service getService() {
        return service;
    }

    public void setService(Network.Service service) {
        this.service = service;
    }

    public String getPrivatePort() {
        return privatePort;
    }

    public String getProtocol() {
        return protocol;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass() || !super.equals(o)) {
            return false;
        }
        DeleteNsxNatRuleCommand that = (DeleteNsxNatRuleCommand) o;
        return Objects.equals(ruleId, that.ruleId) && Objects.equals(service, that.service) && Objects.equals(privatePort, that.privatePort) && Objects.equals(protocol, that.protocol);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), ruleId, service, privatePort, protocol);
    }
}
