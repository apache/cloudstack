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

public class UpdateTungstenLoadBalancerMemberCommand extends TungstenCommand {
    private final String projectFqn;
    private final String networkUuid;
    private final String lbPoolName;
    private final List<TungstenLoadBalancerMember> listTungstenLoadBalancerMember;

    public UpdateTungstenLoadBalancerMemberCommand(final String projectFqn, final String networkUuid,
        final String lbPoolName, final List<TungstenLoadBalancerMember> listTungstenLoadBalancerMember) {
        this.projectFqn = projectFqn;
        this.networkUuid = networkUuid;
        this.lbPoolName = lbPoolName;
        this.listTungstenLoadBalancerMember = listTungstenLoadBalancerMember;
    }

    public String getProjectFqn() {
        return projectFqn;
    }

    public String getNetworkUuid() {
        return networkUuid;
    }

    public String getLbPoolName() {
        return lbPoolName;
    }

    public List<TungstenLoadBalancerMember> getListTungstenLoadBalancerMember() {
        return listTungstenLoadBalancerMember;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        UpdateTungstenLoadBalancerMemberCommand that = (UpdateTungstenLoadBalancerMemberCommand) o;
        return Objects.equals(projectFqn, that.projectFqn) && Objects.equals(networkUuid, that.networkUuid) && Objects.equals(lbPoolName, that.lbPoolName) && Objects.equals(listTungstenLoadBalancerMember, that.listTungstenLoadBalancerMember);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), projectFqn, networkUuid, lbPoolName, listTungstenLoadBalancerMember);
    }
}
