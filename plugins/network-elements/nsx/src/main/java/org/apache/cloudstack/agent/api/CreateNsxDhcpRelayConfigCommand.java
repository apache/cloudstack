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

import java.util.List;
import java.util.Objects;

public class CreateNsxDhcpRelayConfigCommand extends NsxCommand {

    private Long vpcId;
    private String vpcName;
    private long networkId;
    private String networkName;
    private List<String> addresses;

    public CreateNsxDhcpRelayConfigCommand(long domainId, long accountId, long zoneId,
                                           Long vpcId, String vpcName, long networkId, String networkName,
                                           List<String> addresses) {
        super(domainId, accountId, zoneId);
        this.vpcId = vpcId;
        this.vpcName = vpcName;
        this.networkId = networkId;
        this.networkName = networkName;
        this.addresses = addresses;
    }

    public Long getVpcId() {
        return vpcId;
    }

    public String getVpcName() {
        return vpcName;
    }

    public long getNetworkId() {
        return networkId;
    }

    public String getNetworkName() {
        return networkName;
    }

    public List<String> getAddresses() {
        return addresses;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass() || !super.equals(o)) {
            return false;
        }
        CreateNsxDhcpRelayConfigCommand that = (CreateNsxDhcpRelayConfigCommand) o;
        return networkId == that.networkId && Objects.equals(vpcId, that.vpcId) && Objects.equals(vpcName, that.vpcName) && Objects.equals(networkName, that.networkName) && Objects.equals(addresses, that.addresses);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), vpcId, vpcName, networkId, networkName, addresses);
    }
}
