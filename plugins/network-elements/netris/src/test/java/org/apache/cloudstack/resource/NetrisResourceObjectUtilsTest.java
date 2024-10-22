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

import org.apache.cloudstack.agent.api.CreateNetrisVpcCommand;
import org.apache.cloudstack.agent.api.DeleteNetrisVpcCommand;
import org.junit.Assert;
import org.junit.Test;

public class NetrisResourceObjectUtilsTest {

    private static final long zoneId = 1;
    private static final long accountId = 2;
    private static final long domainId = 2;

    private static final long vpcId = 10;
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
}
