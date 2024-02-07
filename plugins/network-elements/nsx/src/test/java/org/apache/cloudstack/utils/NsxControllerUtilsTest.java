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
package org.apache.cloudstack.utils;

import com.cloud.agent.AgentManager;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.network.dao.NsxProviderDao;
import com.cloud.network.element.NsxProviderVO;
import org.apache.cloudstack.NsxAnswer;
import org.apache.cloudstack.agent.api.NsxCommand;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class NsxControllerUtilsTest {

    private static final long domainId = 2L;
    private static final long accountId = 10L;
    private static final long zoneId = 1L;
    private static final long nsxProviderHostId = 1L;

    private static final String commonPrefix = String.format("D%s-A%s-Z%s", domainId, accountId, zoneId);

    @Mock
    private NsxProviderDao nsxProviderDao;
    @Mock
    private AgentManager agentMgr;

    @Spy
    @InjectMocks
    private NsxControllerUtils nsxControllerUtils = new NsxControllerUtils();

    @Mock
    private NsxProviderVO nsxProviderVO;

    @Before
    public void setup() {
        Mockito.when(nsxProviderDao.findByZoneId(zoneId)).thenReturn(nsxProviderVO);
        Mockito.when(nsxProviderVO.getHostId()).thenReturn(nsxProviderHostId);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testSendCommandAnswerFailure() {
        NsxCommand cmd = Mockito.mock(NsxCommand.class);
        Mockito.when(nsxProviderDao.findByZoneId(zoneId)).thenReturn(null);
        nsxControllerUtils.sendNsxCommand(cmd, zoneId);
    }

    @Test(expected = InvalidParameterValueException.class)
    public void testSendCommandNoNsxProvider() {
        NsxCommand cmd = Mockito.mock(NsxCommand.class);
        Mockito.when(agentMgr.easySend(nsxProviderHostId, cmd)).thenReturn(null);
        nsxControllerUtils.sendNsxCommand(cmd, zoneId);
    }

    @Test
    public void testSendCommand() {
        NsxCommand cmd = Mockito.mock(NsxCommand.class);
        NsxAnswer answer = Mockito.mock(NsxAnswer.class);
        Mockito.when(answer.getResult()).thenReturn(true);
        Mockito.when(agentMgr.easySend(nsxProviderHostId, cmd)).thenReturn(answer);
        NsxAnswer nsxAnswer = nsxControllerUtils.sendNsxCommand(cmd, zoneId);
        Assert.assertNotNull(nsxAnswer);
    }

    @Test
    public void testGetNsxNatRuleIdForVpc() {
        long vpcId = 5L;
        String nsxNatRuleId = NsxControllerUtils.getNsxNatRuleId(domainId, accountId, zoneId, vpcId, true);
        String ruleIdPart = String.format("V%s-NAT", vpcId);
        String expected = String.format("%s-%s", commonPrefix, ruleIdPart);
        Assert.assertEquals(expected, nsxNatRuleId);
    }

    @Test
    public void testGetNsxNatRuleIdForNetwork() {
        long networkId = 5L;
        String nsxNatRuleId = NsxControllerUtils.getNsxNatRuleId(domainId, accountId, zoneId, networkId, false);
        String ruleIdPart = String.format("N%s-NAT", networkId);
        String expected = String.format("%s-%s", commonPrefix, ruleIdPart);
        Assert.assertEquals(expected, nsxNatRuleId);
    }

    @Test
    public void testGetNsxSegmentIdForVpcNetwork() {
        long vpcId = 5L;
        long networkId = 2L;
        String nsxSegmentName = NsxControllerUtils.getNsxSegmentId(domainId, accountId, zoneId, vpcId, networkId);
        String segmentPart = String.format("V%s-S%s", vpcId, networkId);
        String expected = String.format("%s-%s", commonPrefix, segmentPart);
        Assert.assertEquals(expected, nsxSegmentName);
    }

    @Test
    public void testGetNsxSegmentIdForNonVpcNetwork() {
        Long vpcId = null;
        long networkId = 2L;
        String nsxSegmentName = NsxControllerUtils.getNsxSegmentId(domainId, accountId, zoneId, vpcId, networkId);
        String segmentPart = String.format("S%s", networkId);
        String expected = String.format("%s-%s", commonPrefix, segmentPart);
        Assert.assertEquals(expected, nsxSegmentName);
    }

    @Test
    public void testGetNsxDistributedFirewallPolicyRuleIdForVpcNetwork() {
        long vpcId = 5L;
        long networkId = 2L;
        long ruleId = 1L;
        String nsxSegmentName = NsxControllerUtils.getNsxSegmentId(domainId, accountId, zoneId, vpcId, networkId);
        String expected = String.format("%s-R%s", nsxSegmentName, ruleId);
        Assert.assertEquals(expected, NsxControllerUtils.getNsxDistributedFirewallPolicyRuleId(nsxSegmentName, ruleId));
    }

    @Test
    public void testGetTier1GatewayNameForVpcNetwork() {
        long networkOnVpcId = 5L;
        String networkPart = String.format("V%s", networkOnVpcId);
        String expected = String.format("%s-%s", commonPrefix, networkPart);
        Assert.assertEquals(expected, NsxControllerUtils.getTier1GatewayName(domainId, accountId, zoneId, networkOnVpcId, true));
    }

    @Test
    public void testGetTier1GatewayNameForNetwork() {
        long networkId = 5L;
        String networkPart = String.format("N%s", networkId);
        String expected = String.format("%s-%s", commonPrefix, networkPart);
        Assert.assertEquals(expected, NsxControllerUtils.getTier1GatewayName(domainId, accountId, zoneId, networkId, false));
    }

    @Test
    public void testGetNsxDhcpRelayConfigIdForVpcNetwork() {
        long vpcId = 5L;
        long networkId = 2L;
        String relayPart = String.format("V%s-S%s-Relay", vpcId, networkId);
        String expected = String.format("%s-%s", commonPrefix, relayPart);
        String dhcpRelayConfigId = NsxControllerUtils.getNsxDhcpRelayConfigId(zoneId, domainId, accountId, vpcId, networkId);
        Assert.assertEquals(expected, dhcpRelayConfigId);
    }

    @Test
    public void testGetNsxDhcpRelayConfigIdForNetwork() {
        Long vpcId = null;
        long networkId = 2L;
        String relayPart = String.format("S%s-Relay", networkId);
        String expected = String.format("%s-%s", commonPrefix, relayPart);
        String dhcpRelayConfigId = NsxControllerUtils.getNsxDhcpRelayConfigId(zoneId, domainId, accountId, vpcId, networkId);
        Assert.assertEquals(expected, dhcpRelayConfigId);
    }

    @Test
    public void testGetStaticNatRuleNameForVpc() {
        long vpcId = 5L;
        String rulePart = String.format("V%s-STATICNAT", vpcId);
        String expected = String.format("%s-%s", commonPrefix, rulePart);
        String staticNatRuleName = NsxControllerUtils.getStaticNatRuleName(domainId, accountId, zoneId, vpcId, true);
        Assert.assertEquals(expected, staticNatRuleName);
    }

    @Test
    public void testGetStaticNatRuleNameForNetwork() {
        long network = 5L;
        String rulePart = String.format("N%s-STATICNAT", network);
        String expected = String.format("%s-%s", commonPrefix, rulePart);
        String staticNatRuleName = NsxControllerUtils.getStaticNatRuleName(domainId, accountId, zoneId, network, false);
        Assert.assertEquals(expected, staticNatRuleName);
    }

    @Test
    public void testGetPortForwardRuleName() {
        long vpcId = 5L;
        long ruleId = 2L;
        String rulePart = String.format("V%s-PF%s", vpcId, ruleId);
        String expected = String.format("%s-%s", commonPrefix, rulePart);
        String portForwardRuleName = NsxControllerUtils.getPortForwardRuleName(domainId, accountId, zoneId, vpcId, ruleId, true);
        Assert.assertEquals(expected, portForwardRuleName);
    }
}
