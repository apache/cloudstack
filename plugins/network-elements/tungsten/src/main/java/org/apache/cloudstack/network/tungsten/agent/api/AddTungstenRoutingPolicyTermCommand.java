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

import org.apache.cloudstack.network.tungsten.model.RoutingPolicyFromTerm;
import org.apache.cloudstack.network.tungsten.model.RoutingPolicyThenTerm;

import java.util.List;

public class AddTungstenRoutingPolicyTermCommand extends TungstenCommand {
    private final String routingPolicyUuid;
    private final RoutingPolicyFromTerm routingPolicyFromTerm;
    private final List<RoutingPolicyThenTerm> routingPolicyThenTerms;

    public AddTungstenRoutingPolicyTermCommand(String routingPolicyUuid, RoutingPolicyFromTerm routingPolicyFromTerm,
                                               List<RoutingPolicyThenTerm> routingPolicyThenTerms) {
        this.routingPolicyUuid = routingPolicyUuid;
        this.routingPolicyFromTerm = routingPolicyFromTerm;
        this.routingPolicyThenTerms = routingPolicyThenTerms;

    }

    public RoutingPolicyFromTerm getRoutingPolicyFromTerm() {
        return routingPolicyFromTerm;
    }

    public String getRoutingPolicyUuid() {
        return routingPolicyUuid;
    }

    public List<RoutingPolicyThenTerm> getRoutingPolicyThenTerms() {
        return routingPolicyThenTerms;
    }
}
