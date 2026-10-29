// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// the License.  You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

package com.cloud.network.security;

import com.cloud.network.NetworkModel;
import com.cloud.network.security.SecurityGroupManagerImpl.CidrComparator;
import com.cloud.network.security.SecurityGroupManagerImpl.PortAndProto;
import com.cloud.network.security.SecurityRule.SecurityRuleType;
import com.cloud.network.security.dao.SecurityGroupRuleDao;
import com.cloud.network.security.dao.SecurityGroupVMMapDao;
import com.cloud.vm.Nic;
import com.cloud.vm.VirtualMachine.State;
import junit.framework.TestCase;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.util.ReflectionTestUtils;

import javax.inject.Inject;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static org.mockito.Mockito.when;

/**
 * @author daan
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "classpath:/SecurityGroupManagerTestContext.xml")
public class SecurityGroupManagerImplTest extends TestCase {
    @Inject
    SecurityGroupManagerImpl2 _sgMgr = null;
    Set<String> cidrs;

    private static final long VM_ID = 1L;
    private static final long GROUP_ID = 10L;
    private static final long ALLOWED_GROUP_ID = 20L;
    private static final long MEMBER_VM_ID = 2L;
    private static final SecurityRuleType RULE_TYPE = SecurityRuleType.IngressRule;

    SecurityGroupRuleDao securityGroupRuleDao;
    SecurityGroupVMMapDao securityGroupVMMapDao;
    NetworkModel networkModel;

    @Before
    public void setup() throws Exception {
        cidrs = new TreeSet<String>(new CidrComparator());
    }

    @Test(expected = NumberFormatException.class)
    public void emptyCidrCompareTest() {
        cidrs.add("");
        cidrs.add("");
    }

    @Test(expected = NumberFormatException.class)
    public void faultyCidrCompareTest() {
        cidrs.add("111.222.333.444");
        cidrs.add("111.222.333.444");
    }

    @Test
    public void sameCidrCompareTest() {
        cidrs.add("1.2.3.4/5");
        cidrs.add("1.2.3.4/5");
        assertEquals("only one element expected", 1, cidrs.size());
        CidrComparator cmp = new CidrComparator();
        assertEquals("should be 0", 0, cmp.compare("1.2.3.4/5", "1.2.3.4/5"));
    }

    @Test
    public void CidrCompareTest() {
        cidrs.add("1.2.3.4/5");
        cidrs.add("1.2.3.4/6");
        assertEquals("two element expected", 2, cidrs.size());
        CidrComparator cmp = new CidrComparator();
        assertEquals("should be 1", 1, cmp.compare("1.2.3.4/5", "1.2.3.4/6"));
        assertEquals("should be -2", -2, cmp.compare("1.2.3.4/5", "1.2.3.4/3"));
    }

    /**
     * Wires mocked DAOs into a manager instance and lets the VM belong to a security group
     * with a single rule that references another security group.
     */
    private <T extends SecurityGroupManagerImpl> T managerWithRuleReferencingGroup(T manager) {
        securityGroupRuleDao = Mockito.mock(SecurityGroupRuleDao.class);
        securityGroupVMMapDao = Mockito.mock(SecurityGroupVMMapDao.class);
        networkModel = Mockito.mock(NetworkModel.class);
        ReflectionTestUtils.setField(manager, "_securityGroupRuleDao", securityGroupRuleDao);
        ReflectionTestUtils.setField(manager, "_securityGroupVMMapDao", securityGroupVMMapDao);
        ReflectionTestUtils.setField(manager, "_networkModel", networkModel);

        SecurityGroupVMMapVO groupMap = Mockito.mock(SecurityGroupVMMapVO.class);
        when(groupMap.getSecurityGroupId()).thenReturn(GROUP_ID);
        when(securityGroupVMMapDao.listByInstanceId(VM_ID)).thenReturn(Collections.singletonList(groupMap));

        SecurityGroupRuleVO rule = Mockito.mock(SecurityGroupRuleVO.class);
        when(rule.getProtocol()).thenReturn("tcp");
        when(rule.getStartPort()).thenReturn(80);
        when(rule.getEndPort()).thenReturn(80);
        when(rule.getAllowedNetworkId()).thenReturn(ALLOWED_GROUP_ID);
        when(securityGroupRuleDao.listBySecurityGroupId(GROUP_ID, RULE_TYPE)).thenReturn(Collections.singletonList(rule));
        return manager;
    }

    /**
     * SecurityGroupManagerImpl resolves the member addresses through the default nic.
     */
    private void memberWithDefaultNic(String ipv4, String ipv6) {
        SecurityGroupVMMapVO memberMap = Mockito.mock(SecurityGroupVMMapVO.class);
        when(memberMap.getInstanceId()).thenReturn(MEMBER_VM_ID);
        when(securityGroupVMMapDao.listBySecurityGroup(ALLOWED_GROUP_ID, State.Running)).thenReturn(Collections.singletonList(memberMap));

        Nic nic = Mockito.mock(Nic.class);
        when(nic.getIPv4Address()).thenReturn(ipv4);
        when(nic.getIPv6Address()).thenReturn(ipv6);
        when(networkModel.getDefaultNic(MEMBER_VM_ID)).thenReturn(nic);
    }

    /**
     * SecurityGroupManagerImpl2 reads the member addresses from the join in the VO instead of the nics table.
     */
    private void memberFromVoJoin(String ipv4, String ipv6) {
        SecurityGroupVMMapVO memberMap = Mockito.mock(SecurityGroupVMMapVO.class);
        when(memberMap.getGuestIpAddress()).thenReturn(ipv4);
        when(memberMap.getGuestIpv6Address()).thenReturn(ipv6);
        when(securityGroupVMMapDao.listBySecurityGroup(ALLOWED_GROUP_ID, State.Running)).thenReturn(Collections.singletonList(memberMap));
    }

    private Set<String> generatedCidrs(SecurityGroupManagerImpl manager) {
        Map<PortAndProto, Set<String>> allowed = manager.generateRulesForVM(VM_ID, RULE_TYPE);
        assertEquals("one rule expected", 1, allowed.size());
        return allowed.values().iterator().next();
    }

    private void assertExactHostCidrs(Set<String> generated) {
        assertTrue("IPv4 member should be pinned to /32", generated.contains("10.1.1.5/32"));
        assertTrue("IPv6 member should be pinned to the exact /128 host", generated.contains("2001:db8::5/128"));
        assertFalse("IPv6 member must not open the whole /64 subnet", generated.contains("2001:db8::5/64"));
    }

    private void assertOnlyIpv6Cidr(Set<String> generated) {
        assertEquals("only the IPv6 address of the member should be authorized", Collections.singleton("2001:db8::5/128"), generated);
    }

    @Test
    public void securityGroupMemberRuleUsesExactIpv6HostCidr() {
        SecurityGroupManagerImpl manager = managerWithRuleReferencingGroup(new SecurityGroupManagerImpl());
        memberWithDefaultNic("10.1.1.5", "2001:db8::5");
        assertExactHostCidrs(generatedCidrs(manager));
    }

    @Test
    public void securityGroupMemberRuleUsesExactIpv6HostCidrImpl2() {
        SecurityGroupManagerImpl2 manager = managerWithRuleReferencingGroup(new SecurityGroupManagerImpl2());
        memberFromVoJoin("10.1.1.5", "2001:db8::5");
        assertExactHostCidrs(generatedCidrs(manager));
    }

    @Test
    public void ipv6OnlyMemberDoesNotProduceNullIpv4Cidr() {
        SecurityGroupManagerImpl manager = managerWithRuleReferencingGroup(new SecurityGroupManagerImpl());
        memberWithDefaultNic(null, "2001:db8::5");
        assertOnlyIpv6Cidr(generatedCidrs(manager));
    }

    @Test
    public void ipv6OnlyMemberDoesNotProduceNullIpv4CidrImpl2() {
        SecurityGroupManagerImpl2 manager = managerWithRuleReferencingGroup(new SecurityGroupManagerImpl2());
        memberFromVoJoin(null, "2001:db8::5");
        assertOnlyIpv6Cidr(generatedCidrs(manager));
    }
}
