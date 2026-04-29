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

import java.util.List;
import java.util.Objects;

public class RemoveTungstenTagCommand extends TungstenCommand {
    private final List<String> networkUuids;
    private final List<String> vmUuids;
    private final List<String> nicUuids;
    private final String policyUuid;
    private final String applicationPolicySetUuid;
    private final String tagUuid;

    public RemoveTungstenTagCommand(final List<String> networkUuids, final List<String> vmUuids,
        final List<String> nicUuids, final String policyUuid, final String applicationPolicySetUuid, final String tagUuid) {
        this.networkUuids = networkUuids;
        this.vmUuids = vmUuids;
        this.nicUuids = nicUuids;
        this.policyUuid = policyUuid;
        this.applicationPolicySetUuid = applicationPolicySetUuid;
        this.tagUuid = tagUuid;
    }

    public List<String> getNetworkUuids() {
        return networkUuids;
    }

    public List<String> getVmUuids() {
        return vmUuids;
    }

    public List<String> getNicUuids() {
        return nicUuids;
    }

    public String getPolicyUuid() {
        return policyUuid;
    }

    public String getApplicationPolicySetUuid() {
        return applicationPolicySetUuid;
    }

    public String getTagUuid() {
        return tagUuid;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        RemoveTungstenTagCommand that = (RemoveTungstenTagCommand) o;
        return Objects.equals(networkUuids, that.networkUuids) && Objects.equals(vmUuids, that.vmUuids) && Objects.equals(nicUuids, that.nicUuids) && Objects.equals(policyUuid, that.policyUuid) && Objects.equals(applicationPolicySetUuid, that.applicationPolicySetUuid) && Objects.equals(tagUuid, that.tagUuid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), networkUuids, vmUuids, nicUuids, policyUuid, applicationPolicySetUuid, tagUuid);
    }
}
