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

import java.util.Objects;

public class CreateNsxSegmentCommand extends NsxCommand {

    private Long vpcId;
    private String vpcName;
    private long networkId;
    private String networkName;
    private String networkGateway;
    private String networkCidr;
    private String ipDiscoveryProfileId;
    private String macDiscoveryProfileId;
    private String segmentSecurityProfileId;

    public CreateNsxSegmentCommand(long domainId, long accountId, long zoneId,
                                   Long vpcId, String vpcName, long networkId, String networkName,
                                   String networkGateway, String networkCidr) {
        this(domainId, accountId, zoneId, vpcId, vpcName, networkId, networkName, networkGateway,
                networkCidr, null, null, null);
    }

    public CreateNsxSegmentCommand(long domainId, long accountId, long zoneId,
                                   Long vpcId, String vpcName, long networkId, String networkName,
                                   String networkGateway, String networkCidr, String ipDiscoveryProfileId,
                                   String macDiscoveryProfileId, String segmentSecurityProfileId) {
        super(domainId, accountId, zoneId);
        this.vpcId = vpcId;
        this.vpcName = vpcName;
        this.networkId = networkId;
        this.networkName = networkName;
        this.networkGateway = networkGateway;
        this.networkCidr = networkCidr;
        this.ipDiscoveryProfileId = ipDiscoveryProfileId;
        this.macDiscoveryProfileId = macDiscoveryProfileId;
        this.segmentSecurityProfileId = segmentSecurityProfileId;
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

    public String getNetworkGateway() {
        return networkGateway;
    }

    public String getNetworkCidr() {
        return networkCidr;
    }

    public String getIpDiscoveryProfileId() {
        return ipDiscoveryProfileId;
    }

    public String getMacDiscoveryProfileId() {
        return macDiscoveryProfileId;
    }

    public String getSegmentSecurityProfileId() {
        return segmentSecurityProfileId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        CreateNsxSegmentCommand command = (CreateNsxSegmentCommand) o;
        return Objects.equals(networkName, command.networkName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), networkName);
    }
}
