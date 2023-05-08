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
package org.apache.cloudstack.network.tungsten.agent.api;

import org.apache.cloudstack.network.tungsten.model.TungstenLoadBalancerMember;

import java.util.List;
import java.util.Objects;

public class CreateTungstenNetworkLoadbalancerCommand extends TungstenCommand {
    private final String projectFqn;
    private final String networkUuid;
    private final String publicNetworkUuid;
    private final String loadBalancerMethod;
    private final String loadBalancerName;
    private final String loadBalancerListenerName;
    private final String loadBalancerPoolName;
    private final String loadBalancerHealthMonitorName;
    private final String loadBalancerVmiName;
    private final String loadBalancerIiName;
    private final long ruleId;
    private final List<TungstenLoadBalancerMember> listMember;
    private final String protocol;
    private final int srcPort;
    private final int dstPort;
    private final String privateIp;
    private final String fipName;
    private final String fiName;
    private final String monitorType;
    private final int maxRetries;
    private final int delay;
    private final int timeout;
    private final String httpMethod;
    private final String urlPath;
    private final String expectedCodes;

    public CreateTungstenNetworkLoadbalancerCommand(final String projectFqn, final String networkUuid,
        final String publicNetworkUuid, final String loadBalancerMethod, final String loadBalancerName,
        final String loadBalancerListenerName, final String loadBalancerPoolName,
        final String loadBalancerHealthMonitorName, final String loadBalancerVmiName, final String loadBalancerIiName,
        final long ruleId, final List<TungstenLoadBalancerMember> listMember, final String protocol, final int srcPort,
        final int dstPort, final String privateIp, final String fipName, final String fiName, final String monitorType,
        final int maxRetries, final int delay, final int timeout, final String httpMethod, final String urlPath,
        final String expectedCodes) {
        this.projectFqn = projectFqn;
        this.networkUuid = networkUuid;
        this.publicNetworkUuid = publicNetworkUuid;
        this.loadBalancerMethod = loadBalancerMethod;
        this.loadBalancerName = loadBalancerName;
        this.loadBalancerListenerName = loadBalancerListenerName;
        this.loadBalancerPoolName = loadBalancerPoolName;
        this.loadBalancerHealthMonitorName = loadBalancerHealthMonitorName;
        this.loadBalancerVmiName = loadBalancerVmiName;
        this.loadBalancerIiName = loadBalancerIiName;
        this.ruleId = ruleId;
        this.listMember = listMember;
        this.protocol = protocol;
        this.srcPort = srcPort;
        this.dstPort = dstPort;
        this.privateIp = privateIp;
        this.fipName = fipName;
        this.fiName = fiName;
        this.monitorType = monitorType;
        this.maxRetries = maxRetries;
        this.delay = delay;
        this.timeout = timeout;
        this.httpMethod = httpMethod;
        this.urlPath = urlPath;
        this.expectedCodes = expectedCodes;
    }

    public String getProjectFqn() {
        return projectFqn;
    }

    public String getNetworkUuid() {
        return networkUuid;
    }

    public String getPublicNetworkUuid() {
        return publicNetworkUuid;
    }

    public String getLoadBalancerMethod() {
        return loadBalancerMethod;
    }

    public String getLoadBalancerName() {
        return loadBalancerName;
    }

    public String getLoadBalancerListenerName() {
        return loadBalancerListenerName;
    }

    public String getLoadBalancerPoolName() {
        return loadBalancerPoolName;
    }

    public String getLoadBalancerHealthMonitorName() {
        return loadBalancerHealthMonitorName;
    }

    public String getLoadBalancerVmiName() {
        return loadBalancerVmiName;
    }

    public String getLoadBalancerIiName() {
        return loadBalancerIiName;
    }

    public long getRuleId() {
        return ruleId;
    }

    public List<TungstenLoadBalancerMember> getListMember() {
        return listMember;
    }

    public String getProtocol() {
        return protocol;
    }

    public int getSrcPort() {
        return srcPort;
    }

    public int getDstPort() {
        return dstPort;
    }

    public String getPrivateIp() {
        return privateIp;
    }

    public String getFipName() {
        return fipName;
    }

    public String getFiName() {
        return fiName;
    }

    public String getMonitorType() {
        return monitorType;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public int getDelay() {
        return delay;
    }

    public int getTimeout() {
        return timeout;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public String getUrlPath() {
        return urlPath;
    }

    public String getExpectedCodes() {
        return expectedCodes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        CreateTungstenNetworkLoadbalancerCommand that = (CreateTungstenNetworkLoadbalancerCommand) o;
        return ruleId == that.ruleId && srcPort == that.srcPort && dstPort == that.dstPort && maxRetries == that.maxRetries && delay == that.delay && timeout == that.timeout && Objects.equals(projectFqn, that.projectFqn) && Objects.equals(networkUuid, that.networkUuid) && Objects.equals(publicNetworkUuid, that.publicNetworkUuid) && Objects.equals(loadBalancerMethod, that.loadBalancerMethod) && Objects.equals(loadBalancerName, that.loadBalancerName) && Objects.equals(loadBalancerListenerName, that.loadBalancerListenerName) && Objects.equals(loadBalancerPoolName, that.loadBalancerPoolName)
                && Objects.equals(loadBalancerHealthMonitorName, that.loadBalancerHealthMonitorName) && Objects.equals(loadBalancerVmiName, that.loadBalancerVmiName) && Objects.equals(loadBalancerIiName, that.loadBalancerIiName) && Objects.equals(listMember, that.listMember) && Objects.equals(protocol, that.protocol) && Objects.equals(privateIp, that.privateIp) && Objects.equals(fipName, that.fipName) && Objects.equals(fiName, that.fiName) && Objects.equals(monitorType, that.monitorType) && Objects.equals(httpMethod, that.httpMethod) && Objects.equals(urlPath, that.urlPath) && Objects.equals(expectedCodes, that.expectedCodes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), projectFqn, networkUuid, publicNetworkUuid, loadBalancerMethod, loadBalancerName, loadBalancerListenerName, loadBalancerPoolName, loadBalancerHealthMonitorName, loadBalancerVmiName, loadBalancerIiName, ruleId, listMember, protocol, srcPort, dstPort, privateIp, fipName, fiName, monitorType, maxRetries, delay, timeout, httpMethod, urlPath, expectedCodes);
    }
}
