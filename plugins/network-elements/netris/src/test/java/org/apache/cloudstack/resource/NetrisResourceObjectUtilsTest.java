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
package org.apache.cloudstack.resource;

import org.apache.cloudstack.agent.api.CreateNetrisVnetCommand;
import org.apache.cloudstack.agent.api.CreateNetrisVpcCommand;
import org.apache.cloudstack.agent.api.CreateOrUpdateNetrisNatCommand;
import org.apache.cloudstack.agent.api.DeleteNetrisVpcCommand;
import org.apache.cloudstack.service.NetrisServiceImpl;
import org.junit.Assert;
import org.junit.Test;

public class NetrisResourceObjectUtilsTest {

    private static final long zoneId = 1;
    private static final long accountId = 2;
    private static final long domainId = 2;

    private static final long vpcId = 8;
    private static final String vpcName = "testVpc";
    private static final String vpcCidr = "10.10.0.0/16";

    @Test
    public void testCreateVpcName() {
        CreateNetrisVpcCommand cmd = new CreateNetrisVpcCommand(zoneId, accountId, domainId, vpcName, vpcCidr, vpcId, true);
        String netrisVpcName = NetrisResourceObjectUtils.retrieveNetrisResourceObjectName(cmd, NetrisResourceObjectUtils.NetrisObjectType.VPC);
        String expectedNetrisVpcName = String.format("D%s-A%s-Z%s-V%s-%s", domainId, accountId, zoneId, vpcId, vpcName);
        Assert.assertEquals(expectedNetrisVpcName, netrisVpcName);
    }

    @Test
    public void testCreateVpcNameWithSuffix() {
        CreateNetrisVpcCommand cmd = new CreateNetrisVpcCommand(zoneId, accountId, domainId, vpcName, vpcCidr, vpcId, true);
        String netrisVpcName = NetrisResourceObjectUtils.retrieveNetrisResourceObjectName(cmd, NetrisResourceObjectUtils.NetrisObjectType.VPC, String.valueOf(vpcId));
        String expectedNetrisVpcName = String.format("D%s-A%s-Z%s-V%s", domainId, accountId, zoneId, vpcId);
        Assert.assertEquals(expectedNetrisVpcName, netrisVpcName);
    }

    @Test
    public void testCreateVpcIpamAllocationName() {
        CreateNetrisVpcCommand cmd = new CreateNetrisVpcCommand(zoneId, accountId, domainId, vpcName, vpcCidr, vpcId, true);
        String ipamAllocationName = NetrisResourceObjectUtils.retrieveNetrisResourceObjectName(cmd, NetrisResourceObjectUtils.NetrisObjectType.IPAM_ALLOCATION, vpcCidr);
        String expectedNetrisVpcName = String.format("D%s-A%s-Z%s-V%s-%s", domainId, accountId, zoneId, vpcId, vpcCidr);
        Assert.assertEquals(expectedNetrisVpcName, ipamAllocationName);
    }

    @Test
    public void testDeleteVpcName() {
        DeleteNetrisVpcCommand cmd = new DeleteNetrisVpcCommand(zoneId, accountId, domainId, vpcName, vpcCidr, vpcId, true);
        String netrisVpcName = NetrisResourceObjectUtils.retrieveNetrisResourceObjectName(cmd, NetrisResourceObjectUtils.NetrisObjectType.VPC);
        String expectedNetrisVpcName = String.format("D%s-A%s-Z%s-V%s-%s", domainId, accountId, zoneId, vpcId, vpcName);
        Assert.assertEquals(expectedNetrisVpcName, netrisVpcName);
    }

    @Test
    public void testSuffixesForDNAT() {
        CreateOrUpdateNetrisNatCommand cmd = new CreateOrUpdateNetrisNatCommand(zoneId, accountId, domainId, vpcName, vpcId, vpcName, null, true, vpcCidr);
        cmd.setNatRuleType("DNAT");
        long ruleId = 23L;
        String ruleName = NetrisResourceObjectUtils.retrieveNetrisResourceObjectName(cmd, NetrisResourceObjectUtils.NetrisObjectType.DNAT,
                String.valueOf(vpcId), String.format("R%s", ruleId));
        String expectedNetrisRuleName = String.format("D%s-A%s-Z%s-V%s-DNAT-R%s", domainId, accountId, zoneId, vpcId, ruleId);
        Assert.assertEquals(expectedNetrisRuleName, ruleName);
    }

    @Test
    public void testSubnetName() {
        String vNetName = "<NETRIS_VNET_NAME>";
        Long vpcTierNetworkId = 240L;
        String vpcTierNetworkCidr = "10.10.30.0/24";
        String vpcTierNetworkGateway = "10.10.30.1";
        CreateNetrisVnetCommand cmd = new CreateNetrisVnetCommand(zoneId, accountId, domainId, vpcName, vpcId, vNetName, vpcTierNetworkId, vpcTierNetworkCidr, vpcTierNetworkGateway, true);
        String subnetName = NetrisResourceObjectUtils.retrieveNetrisResourceObjectName(cmd, NetrisResourceObjectUtils.NetrisObjectType.IPAM_SUBNET, String.valueOf(vpcId), vpcTierNetworkCidr);
        String expectedName = String.format("D%s-A%s-Z%s-V%s-%s", domainId, accountId, zoneId, vpcId, vpcTierNetworkCidr);
        Assert.assertEquals(expectedName, subnetName);
    }

    @Test
    public void testSourceNatName() {
        CreateOrUpdateNetrisNatCommand cmd = new CreateOrUpdateNetrisNatCommand(zoneId, accountId, domainId, vpcName, vpcId, null, null, true, vpcCidr);
        String snatRuleName = NetrisResourceObjectUtils.retrieveNetrisResourceObjectName(cmd, NetrisResourceObjectUtils.NetrisObjectType.SNAT, String.valueOf(vpcId));
        String expectedName = String.format("D%s-A%s-Z%s-V%s-SNAT", domainId, accountId, zoneId, vpcId);
        Assert.assertEquals(expectedName, snatRuleName);
    }

    @Test
    public void testStaticNatName() {
        long vmId = 1234L;
        CreateOrUpdateNetrisNatCommand cmd = new CreateOrUpdateNetrisNatCommand(zoneId, accountId, domainId, vpcName, vpcId, null, null, true, vpcCidr);
        String[] suffixes = NetrisServiceImpl.getStaticNatResourceSuffixes(vpcId, null, true, vmId);
        String staticNatRuleName = NetrisResourceObjectUtils.retrieveNetrisResourceObjectName(cmd, NetrisResourceObjectUtils.NetrisObjectType.STATICNAT, suffixes);
        String expectedName = String.format("D%s-A%s-Z%s-V%s-VM%s-STATICNAT", domainId, accountId, zoneId, vpcId, vmId);
        Assert.assertEquals(expectedName, staticNatRuleName);
    }
}
