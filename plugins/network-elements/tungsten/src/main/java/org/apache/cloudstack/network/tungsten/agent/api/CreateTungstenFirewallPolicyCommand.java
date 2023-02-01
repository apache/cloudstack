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

import java.util.Objects;
import java.util.UUID;

public class CreateTungstenFirewallPolicyCommand extends TungstenCommand {
    private final String uuid;
    private final String applicationPolicySetUuid;
    private final String name;
    private final int sequence;

    public CreateTungstenFirewallPolicyCommand(final String name, final String applicationPolicySetUuid, final int sequence) {
        this.uuid = UUID.randomUUID().toString();
        this.applicationPolicySetUuid = applicationPolicySetUuid;
        this.name = name;
        this.sequence = sequence;
    }

    public String getUuid() {
        return uuid;
    }

    public String getApplicationPolicySetUuid() {
        return applicationPolicySetUuid;
    }

    public String getName() {
        return name;
    }

    public int getSequence() {
        return sequence;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        CreateTungstenFirewallPolicyCommand that = (CreateTungstenFirewallPolicyCommand) o;
        return sequence == that.sequence && Objects.equals(uuid, that.uuid) && Objects.equals(applicationPolicySetUuid, that.applicationPolicySetUuid) && Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), uuid, applicationPolicySetUuid, name, sequence);
    }
}
