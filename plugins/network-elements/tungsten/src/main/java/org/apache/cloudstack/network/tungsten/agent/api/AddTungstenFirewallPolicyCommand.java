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

public class AddTungstenFirewallPolicyCommand extends TungstenCommand {
    private final String applicationPolicySetUuid;
    private final String firewallPolicyUuid;
    private final String tagUuid;
    private final int sequence;

    public AddTungstenFirewallPolicyCommand(final String applicationPolicySetUuid, final String firewallPolicyUuid,
        final String tagUuid, final int sequence) {
        this.applicationPolicySetUuid = applicationPolicySetUuid;
        this.firewallPolicyUuid = firewallPolicyUuid;
        this.tagUuid = tagUuid;
        this.sequence = sequence;
    }

    public String getApplicationPolicySetUuid() {
        return applicationPolicySetUuid;
    }

    public String getFirewallPolicyUuid() {
        return firewallPolicyUuid;
    }

    public String getTagUuid() {
        return tagUuid;
    }

    public int getSequence() {
        return sequence;
    }
}
