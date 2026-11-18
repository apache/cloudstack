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
package com.cloud.network.element;

import java.util.List;
import java.util.Objects;

import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.Network;
import com.cloud.network.rules.FirewallRule;
import com.cloud.network.rules.PortForwardingRule;
import com.cloud.network.vpc.NetworkACLItem;

public interface PortForwardingServiceProvider extends NetworkElement, IpDeployingRequester {

    static String getPublicPortRange(PortForwardingRule rule) {
        return Objects.equals(rule.getSourcePortStart(), rule.getSourcePortEnd()) ?
                String.valueOf(rule.getSourcePortStart()) :
                String.valueOf(rule.getSourcePortStart()).concat("-").concat(String.valueOf(rule.getSourcePortEnd()));
    }

    static String getPrivatePFPortRange(PortForwardingRule rule) {
        return rule.getDestinationPortStart() == rule.getDestinationPortEnd() ?
                String.valueOf(rule.getDestinationPortStart()) :
                String.valueOf(rule.getDestinationPortStart()).concat("-").concat(String.valueOf(rule.getDestinationPortEnd()));
    }

    static String getPrivatePortRange(FirewallRule rule) {
        return Objects.equals(rule.getSourcePortStart(), rule.getSourcePortEnd()) ?
                String.valueOf(rule.getSourcePortStart()) :
                String.valueOf(rule.getSourcePortStart()).concat("-").concat(String.valueOf(rule.getSourcePortEnd()));
    }

    static String getPrivatePortRangeForACLRule(NetworkACLItem rule) {
        return Objects.equals(rule.getSourcePortStart(), rule.getSourcePortEnd()) ?
                String.valueOf(rule.getSourcePortStart()) :
                String.valueOf(rule.getSourcePortStart()).concat("-").concat(String.valueOf(rule.getSourcePortEnd()));
    }

    /**
     * Apply rules
     * @param network
     * @param rules
     * @return
     * @throws ResourceUnavailableException
     */
    boolean applyPFRules(Network network, List<PortForwardingRule> rules) throws ResourceUnavailableException;
}
