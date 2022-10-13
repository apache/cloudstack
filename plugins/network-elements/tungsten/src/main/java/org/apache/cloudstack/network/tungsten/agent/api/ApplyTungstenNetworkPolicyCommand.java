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

public class ApplyTungstenNetworkPolicyCommand extends TungstenCommand {
    private final String projectFqn;
    private final String networkPolicyName;
    private final String networkUuid;
    private final String policyUuid;
    private final int majorSequence;
    private final int minorSequence;

    public ApplyTungstenNetworkPolicyCommand(final String networkUuid, final String policyUuid, final int majorSequence,
        final int minorSequence) {
        this.projectFqn = null;
        this.networkPolicyName = null;
        this.networkUuid = networkUuid;
        this.policyUuid = policyUuid;
        this.majorSequence = majorSequence;
        this.minorSequence = minorSequence;
    }

    public ApplyTungstenNetworkPolicyCommand(final String projectFqn, final String networkPolicyName,
        final String networkUuid, final int majorSequence, final int minorSequence) {
        this.projectFqn = projectFqn;
        this.networkPolicyName = networkPolicyName;
        this.networkUuid = networkUuid;
        this.policyUuid = null;
        this.majorSequence = majorSequence;
        this.minorSequence = minorSequence;
    }

    public String getProjectFqn() {
        return projectFqn;
    }

    public String getNetworkPolicyName() {
        return networkPolicyName;
    }

    public String getNetworkUuid() {
        return networkUuid;
    }

    public String getPolicyUuid() {
        return policyUuid;
    }

    public int getMajorSequence() {
        return majorSequence;
    }

    public int getMinorSequence() {
        return minorSequence;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        ApplyTungstenNetworkPolicyCommand that = (ApplyTungstenNetworkPolicyCommand) o;
        return majorSequence == that.majorSequence && minorSequence == that.minorSequence && Objects.equals(projectFqn, that.projectFqn) && Objects.equals(networkPolicyName, that.networkPolicyName) && Objects.equals(networkUuid, that.networkUuid) && Objects.equals(policyUuid, that.policyUuid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), projectFqn, networkPolicyName, networkUuid, policyUuid, majorSequence, minorSequence);
    }
}
