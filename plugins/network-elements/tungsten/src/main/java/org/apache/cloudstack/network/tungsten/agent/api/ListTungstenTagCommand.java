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

public class ListTungstenTagCommand extends TungstenCommand {
    private final String networkUuid;
    private final String vmUuid;
    private final String nicUuid;
    private final String policyUuid;
    private final String applicationPolicySetUuid;
    private final String tagUuid;

    public ListTungstenTagCommand(final String networkUuid, final String vmUuid, final String nicUuid,
        final String policyUuid, final String applicationPolicySetUuid, final String tagUuid) {
        this.networkUuid = networkUuid;
        this.vmUuid = vmUuid;
        this.nicUuid = nicUuid;
        this.policyUuid = policyUuid;
        this.applicationPolicySetUuid = applicationPolicySetUuid;
        this.tagUuid = tagUuid;
    }

    public String getNetworkUuid() {
        return networkUuid;
    }

    public String getVmUuid() {
        return vmUuid;
    }

    public String getNicUuid() {
        return nicUuid;
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
        ListTungstenTagCommand that = (ListTungstenTagCommand) o;
        return Objects.equals(networkUuid, that.networkUuid) && Objects.equals(vmUuid, that.vmUuid) && Objects.equals(nicUuid, that.nicUuid) && Objects.equals(policyUuid, that.policyUuid) && Objects.equals(applicationPolicySetUuid, that.applicationPolicySetUuid) && Objects.equals(tagUuid, that.tagUuid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), networkUuid, vmUuid, nicUuid, policyUuid, applicationPolicySetUuid, tagUuid);
    }
}
