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
package org.apache.cloudstack.api.command.user.firewall;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import com.cloud.utils.net.NetUtils;

@RunWith(MockitoJUnitRunner.class)
public class CreateFirewallRuleCmdTest {

    private void validateAllIp4Cidr(final CreateFirewallRuleCmd cmd) {
        Assert.assertTrue(CollectionUtils.isNotEmpty(cmd.getSourceCidrList()));
        Assert.assertEquals(1, cmd.getSourceCidrList().size());
        Assert.assertEquals(NetUtils.ALL_IP4_CIDRS, cmd.getSourceCidrList().get(0));
    }

    @Test
    public void testGetSourceCidrList_Null() {
        final CreateFirewallRuleCmd cmd = new CreateFirewallRuleCmd();
        ReflectionTestUtils.setField(cmd, "cidrlist", null);
        validateAllIp4Cidr(cmd);
    }

    @Test
    public void testGetSourceCidrList_Empty() {
        final CreateFirewallRuleCmd cmd = new CreateFirewallRuleCmd();
        ReflectionTestUtils.setField(cmd, "cidrlist", new ArrayList<>());
        validateAllIp4Cidr(cmd);
    }

    @Test
    public void testGetSourceCidrList_NullFirstElement() {
        final CreateFirewallRuleCmd cmd = new CreateFirewallRuleCmd();
        List<String> list = new ArrayList<>();
        list.add(null);
        ReflectionTestUtils.setField(cmd, "cidrlist", list);
        validateAllIp4Cidr(cmd);
    }

    @Test
    public void testGetSourceCidrList_EmptyFirstElement() {
        final CreateFirewallRuleCmd cmd = new CreateFirewallRuleCmd();
        ReflectionTestUtils.setField(cmd, "cidrlist", Collections.singletonList("  "));
        validateAllIp4Cidr(cmd);
    }

    @Test
    public void testGetSourceCidrList_Valid() {
        final CreateFirewallRuleCmd cmd = new CreateFirewallRuleCmd();
        String cidr = "10.1.1.1/22";
        ReflectionTestUtils.setField(cmd, "cidrlist", Collections.singletonList(cidr));
        Assert.assertTrue(CollectionUtils.isNotEmpty(cmd.getSourceCidrList()));
        Assert.assertEquals(1, cmd.getSourceCidrList().size());
        Assert.assertEquals(cidr, cmd.getSourceCidrList().get(0));
    }

    @Test
    public void testGetSourceCidrList_EmptyFirstElementButMore() {
        final CreateFirewallRuleCmd cmd = new CreateFirewallRuleCmd();
        String cidr = "10.1.1.1/22";
        ReflectionTestUtils.setField(cmd, "cidrlist", Arrays.asList("  ", cidr));
        Assert.assertTrue(CollectionUtils.isNotEmpty(cmd.getSourceCidrList()));
        Assert.assertEquals(2, cmd.getSourceCidrList().size());
        Assert.assertEquals(cidr, cmd.getSourceCidrList().get(1));
    }
}
