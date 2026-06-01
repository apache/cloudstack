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

import com.cloud.network.netris.NetrisLbBackend;

import java.util.List;

public class CreateOrUpdateNetrisLoadBalancerRuleCommand extends NetrisCommand {
    private final String publicPort;
    private final String privatePort;
    private final String algorithm;
    private final String protocol;
    List<NetrisLbBackend> lbBackends;
    private String publicIp;
    private final Long lbId;
    private String cidrList;
    private String ruleName;

    public CreateOrUpdateNetrisLoadBalancerRuleCommand(long zoneId, Long accountId, Long domainId, String name, Long id, boolean isVpc,
                                                       List<NetrisLbBackend> lbBackends, long lbId, String publicIp, String publicPort,
                                                       String privatePort, String algorithm, String protocol) {
        super(zoneId, accountId, domainId, name, id, isVpc);
        this.lbId = lbId;
        this.publicIp = publicIp;
        this.publicPort = publicPort;
        this.privatePort = privatePort;
        this.algorithm = algorithm;
        this.protocol = protocol;
        this.lbBackends = lbBackends;
    }

    public String getPublicPort() {
        return publicPort;
    }

    public String getPrivatePort() {
        return privatePort;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public String getProtocol() {
        return protocol;
    }

    public List<NetrisLbBackend> getLbBackends() {
        return lbBackends;
    }

    public Long getLbId() {
        return lbId;
    }

    public String getPublicIp() {
        return publicIp;
    }

    public String getCidrList() {
        return cidrList;
    }

    public void setCidrList(String cidrList) {
        this.cidrList = cidrList;
    }

    public String getRuleName() {
        return ruleName;
    }

    public void setRuleName(String ruleName) {
        this.ruleName = ruleName;
    }
}
