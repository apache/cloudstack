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
package com.cloud.network.security;

import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.network.security.SecurityGroupManagerImpl.PortAndProto;
import com.cloud.network.security.SecurityRule.SecurityRuleType;
import com.cloud.network.security.dao.SecurityGroupRuleDao;
import com.cloud.network.security.dao.SecurityGroupVMMapDao;
import com.cloud.vm.VirtualMachine.State;

@RunWith(MockitoJUnitRunner.Silent.class)
public class SecurityGroupManagerImpl2Ipv6RuleTest {

    @Mock
    SecurityGroupRuleDao _securityGroupRuleDao;
    @Mock
    SecurityGroupVMMapDao _securityGroupVMMapDao;

    @InjectMocks
    SecurityGroupManagerImpl2 manager = new SecurityGroupManagerImpl2();

    @Test
    public void securityGroupMemberRuleUsesExactIpv6HostCidr() {
        Long vmId = 1L;
        SecurityRuleType type = SecurityRuleType.IngressRule;

        // The VM belongs to security group 10, which has one rule referencing another security group (20).
        SecurityGroupVMMapVO groupMap = Mockito.mock(SecurityGroupVMMapVO.class);
        when(groupMap.getSecurityGroupId()).thenReturn(10L);
        when(_securityGroupVMMapDao.listByInstanceId(vmId)).thenReturn(Collections.singletonList(groupMap));

        SecurityGroupRuleVO rule = Mockito.mock(SecurityGroupRuleVO.class);
        when(rule.getProtocol()).thenReturn("tcp");
        when(rule.getStartPort()).thenReturn(80);
        when(rule.getEndPort()).thenReturn(80);
        when(rule.getAllowedNetworkId()).thenReturn(20L);
        when(_securityGroupRuleDao.listBySecurityGroupId(10L, type)).thenReturn(Collections.singletonList(rule));

        // Unlike the superclass, Impl2 reads the member addresses straight from the VO join, not the nics table.
        SecurityGroupVMMapVO memberMap = Mockito.mock(SecurityGroupVMMapVO.class);
        when(memberMap.getGuestIpAddress()).thenReturn("10.1.1.5");
        when(memberMap.getGuestIpv6Address()).thenReturn("2001:db8::5");
        when(_securityGroupVMMapDao.listBySecurityGroup(20L, State.Running)).thenReturn(Collections.singletonList(memberMap));

        Map<PortAndProto, Set<String>> allowed = manager.generateRulesForVM(vmId, type);

        Assert.assertEquals(1, allowed.size());
        Set<String> cidrs = allowed.values().iterator().next();
        // The member must be authorized as an exact host, matching the IPv4 /32 behaviour.
        Assert.assertTrue("IPv4 member should be pinned to /32", cidrs.contains("10.1.1.5/32"));
        Assert.assertTrue("IPv6 member should be pinned to the exact /128 host", cidrs.contains("2001:db8::5/128"));
        Assert.assertFalse("IPv6 member must not open the whole /64 subnet", cidrs.contains("2001:db8::5/64"));
    }
}
