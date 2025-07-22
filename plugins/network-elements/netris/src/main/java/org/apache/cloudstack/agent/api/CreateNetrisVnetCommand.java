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

public class CreateNetrisVnetCommand extends NetrisCommand {
    private String vpcName;
    private Long vpcId;
    private String cidr;
    private Integer vxlanId;
    private String gateway;
    private String netrisTag;
    private String ipv6Cidr;
    private Boolean globalRouting;

    public CreateNetrisVnetCommand(Long zoneId, Long accountId, Long domainId, String vpcName, Long vpcId, String vNetName, Long networkId, String cidr, String gateway, boolean isVpc) {
        super(zoneId, accountId, domainId, vNetName, networkId, isVpc);
        this.vpcId = vpcId;
        this.vpcName = vpcName;
        this.cidr = cidr;
        this.gateway = gateway;
    }

    public Long getVpcId() {
        return vpcId;
    }

    public String getVpcName() {
        return vpcName;
    }

    public String getCidr() {
        return cidr;
    }

    public Integer getVxlanId() {
        return vxlanId;
    }

    public void setVxlanId(Integer vxlanId) {
        this.vxlanId = vxlanId;
    }

    public String getGateway() {
        return gateway;
    }

    public String getNetrisTag() {
        return netrisTag;
    }

    public void setNetrisTag(String netrisTag) {
        this.netrisTag = netrisTag;
    }

    public String getIpv6Cidr() {
        return ipv6Cidr;
    }

    public void setIpv6Cidr(String ipv6Cidr) {
        this.ipv6Cidr = ipv6Cidr;
    }

    public Boolean isGlobalRouting() {
        return globalRouting;
    }

    public void setGlobalRouting(Boolean globalRouting) {
        this.globalRouting = globalRouting;
    }
}
