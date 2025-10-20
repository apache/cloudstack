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

import org.apache.cloudstack.resource.NsxLoadBalancerMember;

import java.util.List;
import java.util.Objects;

public class CreateNsxLoadBalancerRuleCommand extends NsxNetworkCommand {

    private final String publicPort;
    private final String privatePort;
    private final String algorithm;
    private final String protocol;
    List<NsxLoadBalancerMember> memberList;

    private final long lbId;
    public CreateNsxLoadBalancerRuleCommand(long domainId, long accountId, long zoneId, Long networkResourceId,
                                            String networkResourceName, boolean isResourceVpc,
                                            List<NsxLoadBalancerMember> memberList, long lbId, String publicPort,
                                            String privatePort, String algorithm, String protocol) {
        super(domainId, accountId, zoneId, networkResourceId, networkResourceName, isResourceVpc);
        this.lbId = lbId;
        this.memberList = memberList;
        this.publicPort = publicPort;
        this.privatePort = privatePort;
        this.algorithm = algorithm;
        this.protocol = protocol;
    }


    public long getLbId() {
        return lbId;
    }

    public String getPublicPort() {
        return publicPort;
    }

    public String getPrivatePort() {
        return privatePort;
    }

    public List<NsxLoadBalancerMember> getMemberList() {
        return memberList;
    }

    public String getAlgorithm() {
        return algorithm;
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
        CreateNsxLoadBalancerRuleCommand command = (CreateNsxLoadBalancerRuleCommand) o;
        return lbId == command.lbId && Objects.equals(publicPort, command.publicPort) && Objects.equals(privatePort, command.privatePort) && Objects.equals(algorithm, command.algorithm) && Objects.equals(protocol, command.protocol) && Objects.equals(memberList, command.memberList);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), publicPort, privatePort, algorithm, protocol, memberList, lbId);
    }
}
