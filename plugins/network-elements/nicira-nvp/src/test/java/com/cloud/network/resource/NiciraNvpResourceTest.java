//
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
//

package com.cloud.network.resource;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.naming.ConfigurationException;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;

import com.cloud.agent.api.ConfigurePortForwardingRulesOnLogicalRouterAnswer;
import com.cloud.agent.api.ConfigurePortForwardingRulesOnLogicalRouterCommand;
import com.cloud.agent.api.ConfigurePublicIpsOnLogicalRouterAnswer;
import com.cloud.agent.api.ConfigurePublicIpsOnLogicalRouterCommand;
import com.cloud.agent.api.ConfigureStaticNatRulesOnLogicalRouterAnswer;
import com.cloud.agent.api.ConfigureStaticNatRulesOnLogicalRouterCommand;
import com.cloud.agent.api.CreateLogicalRouterAnswer;
import com.cloud.agent.api.CreateLogicalRouterCommand;
import com.cloud.agent.api.CreateLogicalSwitchAnswer;
import com.cloud.agent.api.CreateLogicalSwitchCommand;
import com.cloud.agent.api.CreateLogicalSwitchPortAnswer;
import com.cloud.agent.api.CreateLogicalSwitchPortCommand;
import com.cloud.agent.api.DeleteLogicalRouterAnswer;
import com.cloud.agent.api.DeleteLogicalRouterCommand;
import com.cloud.agent.api.DeleteLogicalSwitchAnswer;
import com.cloud.agent.api.DeleteLogicalSwitchCommand;
import com.cloud.agent.api.DeleteLogicalSwitchPortAnswer;
import com.cloud.agent.api.DeleteLogicalSwitchPortCommand;
import com.cloud.agent.api.FindLogicalSwitchPortAnswer;
import com.cloud.agent.api.FindLogicalSwitchPortCommand;
import com.cloud.agent.api.PingCommand;
import com.cloud.agent.api.StartupCommand;
import com.cloud.agent.api.UpdateLogicalSwitchPortAnswer;
import com.cloud.agent.api.UpdateLogicalSwitchPortCommand;
import com.cloud.agent.api.to.PortForwardingRuleTO;
import com.cloud.agent.api.to.StaticNatRuleTO;
import com.cloud.host.Host;
import com.cloud.network.nicira.Attachment;
import com.cloud.network.nicira.ControlClusterStatus;
import com.cloud.network.nicira.DestinationNatRule;
import com.cloud.network.nicira.LogicalRouter;
import com.cloud.network.nicira.LogicalRouterPort;
import com.cloud.network.nicira.LogicalSwitch;
import com.cloud.network.nicira.LogicalSwitchPort;
import com.cloud.network.nicira.NatRule;
import com.cloud.network.nicira.NiciraNvpApi;
import com.cloud.network.nicira.NiciraNvpApiException;
import com.cloud.network.nicira.SourceNatRule;
import com.cloud.network.utils.CommandRetryUtility;

public class NiciraNvpResourceTest {
    NiciraNvpApi nvpApi = mock(NiciraNvpApi.class);
    NiciraNvpResource resource;
    Map<String, Object> parameters;

    private CommandRetryUtility retryUtility;

    @Before
    public void setUp() {
        resource = new NiciraNvpResource() {
            @Override
            protected NiciraNvpApi createNiciraNvpApi(final String host, final String username, final String password) {
                return nvpApi;
            }
        };

        parameters = new HashMap<String, Object>();
        parameters.put("name", "nvptestdevice");
        parameters.put("ip", "127.0.0.1");
        parameters.put("adminuser", "adminuser");
        parameters.put("guid", "aaaaa-bbbbb-ccccc");
        parameters.put("zoneId", "blublub");
        parameters.put("adminpass", "adminpass");

        retryUtility = CommandRetryUtility.getInstance();
        retryUtility.setServerResource(resource);
    }

    @Test(expected = ConfigurationException.class)
    public void resourceConfigureFailure() throws ConfigurationException {
        resource.configure("NiciraNvpResource", Collections.<String, Object> emptyMap());
    }

    @Test
    public void testInitialization() throws ConfigurationException {
        resource.configure("NiciraNvpResource", parameters);

        final StartupCommand[] sc = resource.initialize();
        assertTrue(sc.length == 1);
        assertTrue("Incorrect startup command GUID", "aaaaa-bbbbb-ccccc".equals(sc[0].getGuid()));
        assertTrue("Incorrect NVP device name", "nvptestdevice".equals(sc[0].getName()));
        assertTrue("Incorrect Data Center", "blublub".equals(sc[0].getDataCenter()));
    }

    @Test
    public void testPingCommandStatusOk() throws ConfigurationException, NiciraNvpApiException {
        resource.configure("NiciraNvpResource", parameters);

        final ControlClusterStatus ccs = mock(ControlClusterStatus.class);
        when(ccs.getClusterStatus()).thenReturn("stable");
        when(nvpApi.getControlClusterStatus()).thenReturn(ccs);

        final PingCommand ping = resource.getCurrentStatus(42);
        assertTrue(ping != null);
        assertTrue(ping.getHostId() == 42);
        assertTrue(ping.getHostType() == Host.Type.L2Networking);
    }

    @Test
    public void testPingCommandStatusFail() throws ConfigurationException, NiciraNvpApiException {
        resource.configure("NiciraNvpResource", parameters);

        final ControlClusterStatus ccs = mock(ControlClusterStatus.class);
        when(ccs.getClusterStatus()).thenReturn("unstable");
        when(nvpApi.getControlClusterStatus()).thenReturn(ccs);

        final PingCommand ping = resource.getCurrentStatus(42);
        assertTrue(ping == null);
    }

    @Test
    public void testPingCommandStatusApiException() throws ConfigurationException, NiciraNvpApiException {
        resource.configure("NiciraNvpResource", parameters);

        final ControlClusterStatus ccs = mock(ControlClusterStatus.class);
        when(ccs.getClusterStatus()).thenReturn("unstable");
        when(nvpApi.getControlClusterStatus()).thenThrow(new NiciraNvpApiException());

        final PingCommand ping = resource.getCurrentStatus(42);
        assertTrue(ping == null);
    }

    @Test
    public void testRetries() throws ConfigurationException, NiciraNvpApiException {
        resource.configure("NiciraNvpResource", parameters);

        final LogicalSwitch ls = mock(LogicalSwitch.class);
        when(ls.getUuid()).thenReturn("cccc").thenReturn("cccc");
        when(nvpApi.createLogicalSwitch((LogicalSwitch) any())).thenThrow(new NiciraNvpApiException()).thenThrow(new NiciraNvpApiException()).thenReturn(ls);

        final CreateLogicalSwitchCommand clsc = new CreateLogicalSwitchCommand((String) parameters.get("guid"), "stt", "loigicalswitch", "owner");
        final CreateLogicalSwitchAnswer clsa = (CreateLogicalSwitchAnswer) resource.executeRequest(clsc);
        assertTrue(clsa.getResult());
    }

    @Test
    public void testCreateLogicalSwitch() throws ConfigurationException, NiciraNvpApiException {
        resource.configure("NiciraNvpResource", parameters);

        final LogicalSwitch ls = mock(LogicalSwitch.class);
        when(ls.getUuid()).thenReturn("cccc").thenReturn("cccc");
        when(nvpApi.createLogicalSwitch((LogicalSwitch) any())).thenReturn(ls);

        final CreateLogicalSwitchCommand clsc = new CreateLogicalSwitchCommand((String) parameters.get("guid"), "stt", "loigicalswitch", "owner");
        final CreateLogicalSwitchAnswer clsa = (CreateLogicalSwitchAnswer) resource.executeRequest(clsc);
        assertTrue(clsa.getResult());
        assertTrue("cccc".equals(clsa.getLogicalSwitchUuid()));
    }

    @Test
    public void testCreateLogicalSwitchApiException() throws ConfigurationException, NiciraNvpApiException {
        resource.configure("NiciraNvpResource", parameters);

        final LogicalSwitch ls = mock(LogicalSwitch.class);
        when(ls.getUuid()).thenReturn("cccc").thenReturn("cccc");
        when(nvpApi.createLogicalSwitch((LogicalSwitch) any())).thenThrow(new NiciraNvpApiException());

        final CreateLogicalSwitchCommand clsc = new CreateLogicalSwitchCommand((String) parameters.get("guid"), "stt", "loigicalswitch", "owner");
        final CreateLogicalSwitchAnswer clsa = (CreateLogicalSwitchAnswer) resource.executeRequest(clsc);
        assertFalse(clsa.getResult());
    }

    @Test
    public void testDeleteLogicalSwitch() throws ConfigurationException {
        resource.configure("NiciraNvpResource", parameters);

        final DeleteLogicalSwitchCommand dlsc = new DeleteLogicalSwitchCommand("cccc");
        final DeleteLogicalSwitchAnswer dlsa = (DeleteLogicalSwitchAnswer) resource.executeRequest(dlsc);
        assertTrue(dlsa.getResult());
    }

    @Test
    public void testDeleteLogicalSwitchApiException() throws ConfigurationException, NiciraNvpApiException {
        resource.configure("NiciraNvpResource", parameters);

        doThrow(new NiciraNvpApiException()).when(nvpApi).deleteLogicalSwitch((String) any());

        final DeleteLogicalSwitchCommand dlsc = new DeleteLogicalSwitchCommand("cccc");
        final DeleteLogicalSwitchAnswer dlsa = (DeleteLogicalSwitchAnswer) resource.executeRequest(dlsc);
        assertFalse(dlsa.getResult());
    }

    @Test
    public void testCreateLogicalSwitchPort() throws ConfigurationException, NiciraNvpApiException {
        resource.configure("NiciraNvpResource", parameters);

        final LogicalSwitchPort lsp = mock(LogicalSwitchPort.class);
        when(lsp.getUuid()).thenReturn("eeee");
        when(nvpApi.createLogicalSwitchPort(eq("cccc"), (LogicalSwitchPort) any())).thenReturn(lsp);

        final CreateLogicalSwitchPortCommand clspc = new CreateLogicalSwitchPortCommand("cccc", "dddd", "owner", "nicname");
        final CreateLogicalSwitchPortAnswer clspa = (CreateLogicalSwitchPortAnswer) resource.executeRequest(clspc);
        assertTrue(clspa.getResult());
        assertTrue("eeee".equals(clspa.getLogicalSwitchPortUuid()));

    }

    @Test
    public void testCreateLogicalSwitchPortApiExceptionInCreate() throws ConfigurationException, NiciraNvpApiException {
        resource.configure("NiciraNvpResource", parameters);

        final LogicalSwitchPort lsp = mock(LogicalSwitchPort.class);
        when(lsp.getUuid()).thenReturn("eeee");
        when(nvpApi.createLogicalSwitchPort(eq("cccc"), (LogicalSwitchPort) any())).thenThrow(new NiciraNvpApiException());

        final CreateLogicalSwitchPortCommand clspc = new CreateLogicalSwitchPortCommand("cccc", "dddd", "owner", "nicname");
        final CreateLogicalSwitchPortAnswer clspa = (CreateLogicalSwitchPortAnswer) resource.executeRequest(clspc);
        assertFalse(clspa.getResult());
    }

    @Test
    public void testCreateLogicalSwitchPortApiExceptionInModify() throws ConfigurationException, NiciraNvpApiException {
        resource.configure("NiciraNvpResource", parameters);

        final LogicalSwitchPort lsp = mock(LogicalSwitchPort.class);
        when(lsp.getUuid()).thenReturn("eeee");
        when(nvpApi.createLogicalSwitchPort(eq("cccc"), (LogicalSwitchPort) any())).thenReturn(lsp);
        doThrow(new NiciraNvpApiException()).when(nvpApi).updateLogicalSwitchPortAttachment((String) any(), (String) any(), (Attachment) any());

        final CreateLogicalSwitchPortCommand clspc = new CreateLogicalSwitchPortCommand("cccc", "dddd", "owner", "nicname");
        final CreateLogicalSwitchPortAnswer clspa = (CreateLogicalSwitchPortAnswer) resource.executeRequest(clspc);
        assertFalse(clspa.getResult());
        verify(nvpApi, atLeastOnce()).deleteLogicalSwitchPort((String) any(), (String) any());
    }

    @Test
    public void testDeleteLogicalSwitchPortException() throws ConfigurationException, NiciraNvpApiException {
        resource.configure("NiciraNvpResource", parameters);

        doThrow(new NiciraNvpApiException()).when(nvpApi).deleteLogicalSwitchPort((String) any(), (String) any());
        final DeleteLogicalSwitchPortAnswer dlspa = (DeleteLogicalSwitchPortAnswer) resource.executeRequest(new DeleteLogicalSwitchPortCommand("aaaa", "bbbb"));
        assertFalse(dlspa.getResult());
    }

    @Test
    public void testUpdateLogicalSwitchPortException() throws ConfigurationException, NiciraNvpApiException {
        resource.configure("NiciraNvpResource", parameters);

        doThrow(new NiciraNvpApiException()).when(nvpApi).updateLogicalSwitchPortAttachment((String) any(), (String) any(), (Attachment) any());
        final UpdateLogicalSwitchPortAnswer dlspa =
                        (UpdateLogicalSwitchPortAnswer) resource.executeRequest(new UpdateLogicalSwitchPortCommand("aaaa", "bbbb", "cccc", "owner", "nicname"));
        assertFalse(dlspa.getResult());
    }

    @Test
    public void testFindLogicalSwitchPort() throws ConfigurationException, NiciraNvpApiException {
        resource.configure("NiciraNvpResource", parameters);

        final List<LogicalSwitchPort> lspl = Arrays.asList(new LogicalSwitchPort());
        when(nvpApi.findLogicalSwitchPortsByUuid("aaaa", "bbbb")).thenReturn(lspl);

        final FindLogicalSwitchPortAnswer flspa = (FindLogicalSwitchPortAnswer) resource.executeRequest(new FindLogicalSwitchPortCommand("aaaa", "bbbb"));
        assertTrue(flspa.getResult());
    }

    @Test
    public void testFindLogicalSwitchPortNotFound() throws ConfigurationException, NiciraNvpApiException {
        resource.configure("NiciraNvpResource", parameters);

        @SuppressWarnings("unchecked")
        final List<LogicalSwitchPort> lspl = Collections.EMPTY_LIST;
        when(nvpApi.findLogicalSwitchPortsByUuid("aaaa", "bbbb")).thenReturn(lspl);

        final FindLogicalSwitchPortAnswer flspa = (FindLogicalSwitchPortAnswer) resource.executeRequest(new FindLogicalSwitchPortCommand("aaaa", "bbbb"));
        assertFalse(flspa.getResult());
    }

    @Test
    public void testFindLogicalSwitchPortApiException() throws ConfigurationException, NiciraNvpApiException {
        resource.configure("NiciraNvpResource", parameters);

        when(nvpApi.findLogicalSwitchPortsByUuid("aaaa", "bbbb")).thenThrow(new NiciraNvpApiException());

        final FindLogicalSwitchPortAnswer flspa = (FindLogicalSwitchPortAnswer) resource.executeRequest(new FindLogicalSwitchPortCommand("aaaa", "bbbb"));
        assertFalse(flspa.getResult());
    }

    @Test
    public void testCreateLogicalRouter() throws ConfigurationException, NiciraNvpApiException {
        resource.configure("NiciraNvpResource", parameters);

        final LogicalRouter lrc = mock(LogicalRouter.class);
        final LogicalRouterPort lrp = mock(LogicalRouterPort.class);
        final LogicalSwitchPort lsp = mock(LogicalSwitchPort.class);
        when(lrc.getUuid()).thenReturn("ccccc");
        when(lrp.getUuid()).thenReturn("ddddd").thenReturn("eeeee");
        when(lsp.getUuid()).thenReturn("fffff");
        when(nvpApi.createLogicalRouter((LogicalRouter) any())).thenReturn(lrc);
        when(nvpApi.createLogicalRouterPort(eq("ccccc"), (LogicalRouterPort) any())).thenReturn(lrp);
        when(nvpApi.createLogicalSwitchPort(eq("bbbbb"), (LogicalSwitchPort) any())).thenReturn(lsp);
        final CreateLogicalRouterCommand clrc = new CreateLogicalRouterCommand("aaaaa", 50, "bbbbb", "lrouter", "publiccidr", "nexthop", "internalcidr", "owner");
        final CreateLogicalRouterAnswer clra = (CreateLogicalRouterAnswer) resource.executeRequest(clrc);

        assertTrue(clra.getResult());
        assertTrue("ccccc".equals(clra.getLogicalRouterUuid()));
        verify(nvpApi, atLeast(1)).createLogicalRouterNatRule((String) any(), (NatRule) any());
    }

    @Test
    public void testCreateLogicalRouterApiException() throws ConfigurationException, NiciraNvpApiException {
        resource.configure("NiciraNvpResource", parameters);

        when(nvpApi.createLogicalRouter((LogicalRouter) any())).thenThrow(new NiciraNvpApiException());
        final CreateLogicalRouterCommand clrc = new CreateLogicalRouterCommand("aaaaa", 50, "bbbbb", "lrouter", "publiccidr", "nexthop", "internalcidr", "owner");
        final CreateLogicalRouterAnswer clra = (CreateLogicalRouterAnswer) resource.executeRequest(clrc);

        assertFalse(clra.getResult());
    }

    @Test
    public void testCreateLogicalRouterApiExceptionRollbackRouter() throws ConfigurationException, NiciraNvpApiException {
        resource.configure("NiciraNvpResource", parameters);

        final LogicalRouter lrc = mock(LogicalRouter.class);
        when(lrc.getUuid()).thenReturn("ccccc");
        when(nvpApi.createLogicalRouter((LogicalRouter) any())).thenReturn(lrc);
        when(nvpApi.createLogicalRouterPort(eq("ccccc"), (LogicalRouterPort) any())).thenThrow(new NiciraNvpApiException());
        final CreateLogicalRouterCommand clrc = new CreateLogicalRouterCommand("aaaaa", 50, "bbbbb", "lrouter", "publiccidr", "nexthop", "internalcidr", "owner");
        final CreateLogicalRouterAnswer clra = (CreateLogicalRouterAnswer) resource.executeRequest(clrc);

        assertFalse(clra.getResult());
        verify(nvpApi, atLeast(1)).deleteLogicalRouter(eq("ccccc"));
    }

    @Test
    public void testCreateLogicalRouterApiExceptionRollbackRouterAndSwitchPort() throws ConfigurationException, NiciraNvpApiException {
        resource.configure("NiciraNvpResource", parameters);

        final LogicalRouter lrc = mock(LogicalRouter.class);
        final LogicalRouterPort lrp = mock(LogicalRouterPort.class);
        final LogicalSwitchPort lsp = mock(LogicalSwitchPort.class);
        when(lrc.getUuid()).thenReturn("ccccc");
        when(lrp.getUuid()).thenReturn("ddddd").thenReturn("eeeee");
        when(lsp.getUuid()).thenReturn("fffff");
        when(nvpApi.createLogicalRouter((LogicalRouter) any())).thenReturn(lrc);
        when(nvpApi.createLogicalRouterPort(eq("ccccc"), (LogicalRouterPort) any())).thenReturn(lrp);
        when(nvpApi.createLogicalSwitchPort(eq("bbbbb"), (LogicalSwitchPort) any())).thenReturn(lsp);
        when(nvpApi.createLogicalRouterNatRule((String) any(), (NatRule) any())).thenThrow(new NiciraNvpApiException());
        final CreateLogicalRouterCommand clrc = new CreateLogicalRouterCommand("aaaaa", 50, "bbbbb", "lrouter", "publiccidr", "nexthop", "internalcidr", "owner");
        final CreateLogicalRouterAnswer clra = (CreateLogicalRouterAnswer) resource.executeRequest(clrc);

        assertFalse(clra.getResult());
        verify(nvpApi, atLeast(1)).deleteLogicalRouter(eq("ccccc"));
        verify(nvpApi, atLeast(1)).deleteLogicalSwitchPort(eq("bbbbb"), eq("fffff"));
    }

    @Test
    public void testDeleteLogicalRouterApiException() throws ConfigurationException, NiciraNvpApiException {
        resource.configure("NiciraNvpResource", parameters);

        doThrow(new NiciraNvpApiException()).when(nvpApi).deleteLogicalRouter(eq("aaaaa"));
        final DeleteLogicalRouterAnswer dlspa = (DeleteLogicalRouterAnswer) resource.executeRequest(new DeleteLogicalRouterCommand("aaaaa"));
        assertFalse(dlspa.getResult());
    }

    @Test
    public void testConfigurePublicIpsOnLogicalRouterApiException() throws ConfigurationException, NiciraNvpApiException {
        resource.configure("NiciraNvpResource", parameters);

        final ConfigurePublicIpsOnLogicalRouterCommand cmd = mock(ConfigurePublicIpsOnLogicalRouterCommand.class);
        @SuppressWarnings("unchecked")
        final List<LogicalRouterPort> list = Collections.EMPTY_LIST;

        when(cmd.getLogicalRouterUuid()).thenReturn("aaaaa");
        when(cmd.getL3GatewayServiceUuid()).thenReturn("bbbbb");
        when(nvpApi.findLogicalRouterPortByGatewayServiceUuid("aaaaa", "bbbbb")).thenReturn(list);
        doThrow(new NiciraNvpApiException()).when(nvpApi).updateLogicalRouterPort((String) any(), (LogicalRouterPort) any());

        final ConfigurePublicIpsOnLogicalRouterAnswer answer = (ConfigurePublicIpsOnLogicalRouterAnswer) resource.executeRequest(cmd);
        assertFalse(answer.getResult());

    }

    @Test
    public void testConfigurePublicIpsOnLogicalRouterRetry() throws ConfigurationException, NiciraNvpApiException {
        resource.configure("NiciraNvpResource", parameters);

        final ConfigurePublicIpsOnLogicalRouterCommand cmd = mock(ConfigurePublicIpsOnLogicalRouterCommand.class);

        when(cmd.getLogicalRouterUuid()).thenReturn("aaaaa");
        when(cmd.getL3GatewayServiceUuid()).thenReturn("bbbbb");
        when(nvpApi.findLogicalRouterPortByGatewayServiceUuid("aaaaa", "bbbbb")).thenThrow(new NiciraNvpApiException("retry 1")).thenThrow(new NiciraNvpApiException("retry 2"));

        final ConfigurePublicIpsOnLogicalRouterAnswer answer = (ConfigurePublicIpsOnLogicalRouterAnswer) resource.executeRequest(cmd);
        assertFalse(answer.getResult());

    }

    @Test
    public void testConfigureStaticNatRulesOnLogicalRouter() throws ConfigurationException, NiciraNvpApiException {
        resource.configure("NiciraNvpResource", parameters);
        /*
         * StaticNat Outside IP: 11.11.11.11 Inside IP: 10.10.10.10
         */

        // Mock the command
        final ConfigureStaticNatRulesOnLogicalRouterCommand cmd = mock(ConfigureStaticNatRulesOnLogicalRouterCommand.class);
        final StaticNatRuleTO rule = new StaticNatRuleTO(1, "11.11.11.11", null, null, "10.10.10.10", null, null, null, false, false);
        final List<StaticNatRuleTO> rules = new ArrayList<StaticNatRuleTO>();
        rules.add(rule);
        when(cmd.getRules()).thenReturn(rules);
        when(cmd.getLogicalRouterUuid()).thenReturn("aaaaa");

        // Mock the api find call
        @SuppressWarnings("unchecked")
        final List<NatRule> storedRules = Collections.EMPTY_LIST;
        when(nvpApi.findNatRulesByLogicalRouterUuid("aaaaa")).thenReturn(storedRules);

        // Mock the api create calls
        final NatRule[] rulepair = resource.generateStaticNatRulePair("10.10.10.10", "11.11.11.11");
        rulepair[0].setUuid(UUID.randomUUID());
        rulepair[1].setUuid(UUID.randomUUID());
        when(nvpApi.createLogicalRouterNatRule(eq("aaaaa"), (NatRule) any())).thenReturn(rulepair[0]).thenReturn(rulepair[1]);

        final ConfigureStaticNatRulesOnLogicalRouterAnswer a = (ConfigureStaticNatRulesOnLogicalRouterAnswer) resource.executeRequest(cmd);

        assertTrue(a.getResult());
        verify(nvpApi, atLeast(2)).createLogicalRouterNatRule(eq("aaaaa"), argThat(new ArgumentMatcher<NatRule>() {
            @Override
            public boolean matches(final Object argument) {
                final NatRule rule = (NatRule) argument;
                if (rule.getType().equals("DestinationNatRule") && ((DestinationNatRule) rule).getToDestinationIpAddress().equals("10.10.10.10")) {
                    return true;
                }
                if (rule.getType().equals("SourceNatRule") && ((SourceNatRule) rule).getToSourceIpAddressMin().equals("11.11.11.11")) {
                    return true;
                }
                return false;
            }
        }));
    }

    @Test
    public void testConfigureStaticNatRulesOnLogicalRouterExistingRules() throws ConfigurationException, NiciraNvpApiException {
        resource.configure("NiciraNvpResource", parameters);
        /*
         * StaticNat Outside IP: 11.11.11.11 Inside IP: 10.10.10.10
         */

        // Mock the command
        final ConfigureStaticNatRulesOnLogicalRouterCommand cmd = mock(ConfigureStaticNatRulesOnLogicalRouterCommand.class);
        final StaticNatRuleTO rule = new StaticNatRuleTO(1, "11.11.11.11", null, null, "10.10.10.10", null, null, null, false, false);
        final List<StaticNatRuleTO> rules = new ArrayList<StaticNatRuleTO>();
        rules.add(rule);
        when(cmd.getRules()).thenReturn(rules);
        when(cmd.getLogicalRouterUuid()).thenReturn("aaaaa");

        // Mock the api create calls
        final NatRule[] rulepair = resource.generateStaticNatRulePair("10.10.10.10", "11.11.11.11");
        rulepair[0].setUuid(UUID.randomUUID());
        rulepair[1].setUuid(UUID.randomUUID());
        when(nvpApi.createLogicalRouterNatRule(eq("aaaaa"), (NatRule) any())).thenReturn(rulepair[0]).thenReturn(rulepair[1]);

        // Mock the api find call
        final List<NatRule> storedRules = Arrays.asList(rulepair);
        when(nvpApi.findNatRulesByLogicalRouterUuid("aaaaa")).thenReturn(storedRules);

        final ConfigureStaticNatRulesOnLogicalRouterAnswer a = (ConfigureStaticNatRulesOnLogicalRouterAnswer) resource.executeRequest(cmd);

        assertTrue(a.getResult());
        verify(nvpApi, never()).createLogicalRouterNatRule(eq("aaaaa"), argThat(new ArgumentMatcher<NatRule>() {
            @Override
            public boolean matches(final Object argument) {
                final NatRule rule = (NatRule) argument;
                if (rule.getType().equals("DestinationNatRule") && ((DestinationNatRule) rule).getToDestinationIpAddress().equals("10.10.10.10")) {
                    return true;
                }
                if (rule.getType().equals("SourceNatRule") && ((SourceNatRule) rule).getToSourceIpAddressMin().equals("11.11.11.11")) {
                    return true;
                }
                return false;
            }
        }));
    }

    @Test
    public void testConfigureStaticNatRulesOnLogicalRouterRemoveRules() throws ConfigurationException, NiciraNvpApiException {
        resource.configure("NiciraNvpResource", parameters);
        /*
         * StaticNat Outside IP: 11.11.11.11 Inside IP: 10.10.10.10
         */

        // Mock the command
        final ConfigureStaticNatRulesOnLogicalRouterCommand cmd = mock(ConfigureStaticNatRulesOnLogicalRouterCommand.class);
        final StaticNatRuleTO rule = new StaticNatRuleTO(1, "11.11.11.11", null, null, "10.10.10.10", null, null, null, true, false);
        final List<StaticNatRuleTO> rules = new ArrayList<StaticNatRuleTO>();
        rules.add(rule);
        when(cmd.getRules()).thenReturn(rules);
        when(cmd.getLogicalRouterUuid()).thenReturn("aaaaa");

        // Mock the api create calls
        final NatRule[] rulepair = resource.generateStaticNatRulePair("10.10.10.10", "11.11.11.11");
        final UUID rule0Uuid = UUID.randomUUID();
        final UUID rule1Uuid = UUID.randomUUID();
        rulepair[0].setUuid(rule0Uuid);
        rulepair[1].setUuid(rule1Uuid);
        when(nvpApi.createLogicalRouterNatRule(eq("aaaaa"), (NatRule) any())).thenReturn(rulepair[0]).thenReturn(rulepair[1]);

        // Mock the api find call
        final List<NatRule> storedRules = Arrays.asList(rulepair);
        when(nvpApi.findNatRulesByLogicalRouterUuid("aaaaa")).thenReturn(storedRules);

        final ConfigureStaticNatRulesOnLogicalRouterAnswer a = (ConfigureStaticNatRulesOnLogicalRouterAnswer) resource.executeRequest(cmd);

        assertTrue(a.getResult());
        verify(nvpApi, atLeast(2)).deleteLogicalRouterNatRule(eq("aaaaa"), argThat(new ArgumentMatcher<UUID>() {
            @Override
            public boolean matches(final Object argument) {
                final UUID uuid = (UUID) argument;
                if (rule0Uuid.equals(uuid) || rule1Uuid.equals(uuid)) {
                    return true;
                }
                return false;
            }
        }));
    }

    @Test
    public void testConfigureStaticNatRulesOnLogicalRouterRollback() throws ConfigurationException, NiciraNvpApiException {
        resource.configure("NiciraNvpResource", parameters);
        /*
         * StaticNat Outside IP: 11.11.11.11 Inside IP: 10.10.10.10
         */

        // Mock the command
        final ConfigureStaticNatRulesOnLogicalRouterCommand cmd = mock(ConfigureStaticNatRulesOnLogicalRouterCommand.class);
        final StaticNatRuleTO rule = new StaticNatRuleTO(1, "11.11.11.11", null, null, "10.10.10.10", null, null, null, false, false);
        final List<StaticNatRuleTO> rules = new ArrayList<StaticNatRuleTO>();
        rules.add(rule);
        when(cmd.getRules()).thenReturn(rules);
        when(cmd.getLogicalRouterUuid()).thenReturn("aaaaa");

        // Mock the api create calls
        final NatRule[] rulepair = resource.generateStaticNatRulePair("10.10.10.10", "11.11.11.11");
        rulepair[0].setUuid(UUID.randomUUID());
        rulepair[1].setUuid(UUID.randomUUID());
        when(nvpApi.createLogicalRouterNatRule(eq("aaaaa"), (NatRule) any())).thenReturn(rulepair[0]).thenThrow(new NiciraNvpApiException());

        // Mock the api find call
        @SuppressWarnings("unchecked")
        final List<NatRule> storedRules = Collections.EMPTY_LIST;
        when(nvpApi.findNatRulesByLogicalRouterUuid("aaaaa")).thenReturn(storedRules);

        final ConfigureStaticNatRulesOnLogicalRouterAnswer a = (ConfigureStaticNatRulesOnLogicalRouterAnswer) resource.executeRequest(cmd);

        assertFalse(a.getResult());
        verify(nvpApi, atLeastOnce()).deleteLogicalRouterNatRule(eq("aaaaa"), eq(rulepair[0].getUuid()));
    }

    @Test
    public void testConfigurePortForwardingRulesOnLogicalRouter() throws ConfigurationException, NiciraNvpApiException {
        resource.configure("NiciraNvpResource", parameters);
        /*
         * StaticNat Outside IP: 11.11.11.11 Inside IP: 10.10.10.10
         */

        // Mock the command
        final ConfigurePortForwardingRulesOnLogicalRouterCommand cmd = mock(ConfigurePortForwardingRulesOnLogicalRouterCommand.class);
        final PortForwardingRuleTO rule = new PortForwardingRuleTO(1, "11.11.11.11", 80, 80, "10.10.10.10", 8080, 8080, "tcp", false, false);
        final List<PortForwardingRuleTO> rules = new ArrayList<PortForwardingRuleTO>();
        rules.add(rule);
        when(cmd.getRules()).thenReturn(rules);
        when(cmd.getLogicalRouterUuid()).thenReturn("aaaaa");

        // Mock the api find call
        @SuppressWarnings("unchecked")
        final List<NatRule> storedRules = Collections.EMPTY_LIST;
        when(nvpApi.findNatRulesByLogicalRouterUuid("aaaaa")).thenReturn(storedRules);

        // Mock the api create calls
        final NatRule[] rulepair = resource.generatePortForwardingRulePair("10.10.10.10", new int[] { 8080, 8080 }, "11.11.11.11", new int[] { 80, 80 }, "tcp");
        rulepair[0].setUuid(UUID.randomUUID());
        rulepair[1].setUuid(UUID.randomUUID());
        when(nvpApi.createLogicalRouterNatRule(eq("aaaaa"), (NatRule) any())).thenReturn(rulepair[0]).thenReturn(rulepair[1]);

        final ConfigurePortForwardingRulesOnLogicalRouterAnswer a = (ConfigurePortForwardingRulesOnLogicalRouterAnswer) resource.executeRequest(cmd);

        assertTrue(a.getResult());
        verify(nvpApi, atLeast(2)).createLogicalRouterNatRule(eq("aaaaa"), argThat(new ArgumentMatcher<NatRule>() {
            @Override
            public boolean matches(final Object argument) {
                final NatRule rule = (NatRule) argument;
                if (rule.getType().equals("DestinationNatRule") && ((DestinationNatRule) rule).getToDestinationIpAddress().equals("10.10.10.10")) {
                    return true;
                }
                if (rule.getType().equals("SourceNatRule") && ((SourceNatRule) rule).getToSourceIpAddressMin().equals("11.11.11.11")) {
                    return true;
                }
                return false;
            }
        }));
    }

    @Test
    public void testConfigurePortForwardingRulesOnLogicalRouterExistingRules() throws ConfigurationException, NiciraNvpApiException {
        resource.configure("NiciraNvpResource", parameters);
        /*
         * StaticNat Outside IP: 11.11.11.11 Inside IP: 10.10.10.10
         */

        // Mock the command
        final ConfigurePortForwardingRulesOnLogicalRouterCommand cmd = mock(ConfigurePortForwardingRulesOnLogicalRouterCommand.class);
        final PortForwardingRuleTO rule = new PortForwardingRuleTO(1, "11.11.11.11", 80, 80, "10.10.10.10", 8080, 8080, "tcp", false, true);
        final List<PortForwardingRuleTO> rules = new ArrayList<PortForwardingRuleTO>();
        rules.add(rule);
        when(cmd.getRules()).thenReturn(rules);
        when(cmd.getLogicalRouterUuid()).thenReturn("aaaaa");

        // Mock the api create calls
        final NatRule[] rulepair = resource.generatePortForwardingRulePair("10.10.10.10", new int[] { 8080, 8080 }, "11.11.11.11", new int[] { 80, 80 }, "tcp");
        rulepair[0].setUuid(UUID.randomUUID());
        rulepair[1].setUuid(UUID.randomUUID());
        when(nvpApi.createLogicalRouterNatRule(eq("aaaaa"), (NatRule) any())).thenReturn(rulepair[0]).thenReturn(rulepair[1]);

        // Mock the api find call
        final List<NatRule> storedRules = Arrays.asList(rulepair);
        when(nvpApi.findNatRulesByLogicalRouterUuid("aaaaa")).thenReturn(storedRules);

        final ConfigurePortForwardingRulesOnLogicalRouterAnswer a = (ConfigurePortForwardingRulesOnLogicalRouterAnswer) resource.executeRequest(cmd);

        assertTrue(a.getResult());
        verify(nvpApi, never()).createLogicalRouterNatRule(eq("aaaaa"), argThat(new ArgumentMatcher<NatRule>() {
            @Override
            public boolean matches(final Object argument) {
                final NatRule rule = (NatRule) argument;
                if (rule.getType().equals("DestinationNatRule") && ((DestinationNatRule) rule).getToDestinationIpAddress().equals("10.10.10.10")) {
                    return true;
                }
                if (rule.getType().equals("SourceNatRule") && ((SourceNatRule) rule).getToSourceIpAddressMin().equals("11.11.11.11")) {
                    return true;
                }
                return false;
            }
        }));
    }

    @Test
    public void testConfigurePortForwardingRulesOnLogicalRouterRemoveRules() throws ConfigurationException, NiciraNvpApiException {
        resource.configure("NiciraNvpResource", parameters);
        /*
         * StaticNat Outside IP: 11.11.11.11 Inside IP: 10.10.10.10
         */

        // Mock the command
        final ConfigurePortForwardingRulesOnLogicalRouterCommand cmd = mock(ConfigurePortForwardingRulesOnLogicalRouterCommand.class);
        final PortForwardingRuleTO rule = new PortForwardingRuleTO(1, "11.11.11.11", 80, 80, "10.10.10.10", 8080, 8080, "tcp", true, true);
        final List<PortForwardingRuleTO> rules = new ArrayList<PortForwardingRuleTO>();
        rules.add(rule);
        when(cmd.getRules()).thenReturn(rules);
        when(cmd.getLogicalRouterUuid()).thenReturn("aaaaa");

        // Mock the api create calls
        final NatRule[] rulepair = resource.generatePortForwardingRulePair("10.10.10.10", new int[] { 8080, 8080 }, "11.11.11.11", new int[] { 80, 80 }, "tcp");
        final UUID rule0Uuid = UUID.randomUUID();
        final UUID rule1Uuid = UUID.randomUUID();
        rulepair[0].setUuid(rule0Uuid);
        rulepair[1].setUuid(rule1Uuid);
        when(nvpApi.createLogicalRouterNatRule(eq("aaaaa"), (NatRule) any())).thenReturn(rulepair[0]).thenReturn(rulepair[1]);

        // Mock the api find call
        final List<NatRule> storedRules = Arrays.asList(rulepair);
        when(nvpApi.findNatRulesByLogicalRouterUuid("aaaaa")).thenReturn(storedRules);

        final ConfigurePortForwardingRulesOnLogicalRouterAnswer a = (ConfigurePortForwardingRulesOnLogicalRouterAnswer) resource.executeRequest(cmd);

        assertTrue(a.getResult());
        verify(nvpApi, atLeast(2)).deleteLogicalRouterNatRule(eq("aaaaa"), argThat(new ArgumentMatcher<UUID>() {
            @Override
            public boolean matches(final Object argument) {
                final UUID uuid = (UUID) argument;
                if (rule0Uuid.equals(uuid) || rule1Uuid.equals(uuid)) {
                    return true;
                }
                return false;
            }
        }));
    }

    @Test
    public void testConfigurePortForwardingRulesOnLogicalRouterRollback() throws ConfigurationException, NiciraNvpApiException {
        resource.configure("NiciraNvpResource", parameters);
        /*
         * StaticNat Outside IP: 11.11.11.11 Inside IP: 10.10.10.10
         */

        // Mock the command
        final ConfigurePortForwardingRulesOnLogicalRouterCommand cmd = mock(ConfigurePortForwardingRulesOnLogicalRouterCommand.class);
        final PortForwardingRuleTO rule = new PortForwardingRuleTO(1, "11.11.11.11", 80, 80, "10.10.10.10", 8080, 8080, "tcp", false, false);
        final List<PortForwardingRuleTO> rules = new ArrayList<PortForwardingRuleTO>();
        rules.add(rule);
        when(cmd.getRules()).thenReturn(rules);
        when(cmd.getLogicalRouterUuid()).thenReturn("aaaaa");

        // Mock the api create calls
        final NatRule[] rulepair = resource.generatePortForwardingRulePair("10.10.10.10", new int[] { 8080, 8080 }, "11.11.11.11", new int[] { 80, 80 }, "tcp");
        rulepair[0].setUuid(UUID.randomUUID());
        rulepair[1].setUuid(UUID.randomUUID());
        when(nvpApi.createLogicalRouterNatRule(eq("aaaaa"), (NatRule) any())).thenReturn(rulepair[0]).thenThrow(new NiciraNvpApiException());

        // Mock the api find call
        @SuppressWarnings("unchecked")
        final List<NatRule> storedRules = Collections.EMPTY_LIST;
        when(nvpApi.findNatRulesByLogicalRouterUuid("aaaaa")).thenReturn(storedRules);

        final ConfigurePortForwardingRulesOnLogicalRouterAnswer a = (ConfigurePortForwardingRulesOnLogicalRouterAnswer) resource.executeRequest(cmd);

        assertFalse(a.getResult());
        verify(nvpApi, atLeastOnce()).deleteLogicalRouterNatRule(eq("aaaaa"), eq(rulepair[0].getUuid()));
    }

    @Test
    public void testConfigurePortForwardingRulesOnLogicalRouterPortRange() throws ConfigurationException, NiciraNvpApiException {
        resource.configure("NiciraNvpResource", parameters);
        /*
         * StaticNat Outside IP: 11.11.11.11 Inside IP: 10.10.10.10
         */

        // Mock the command
        final ConfigurePortForwardingRulesOnLogicalRouterCommand cmd = mock(ConfigurePortForwardingRulesOnLogicalRouterCommand.class);
        final PortForwardingRuleTO rule = new PortForwardingRuleTO(1, "11.11.11.11", 80, 85, "10.10.10.10", 80, 85, "tcp", false, false);
        final List<PortForwardingRuleTO> rules = new ArrayList<PortForwardingRuleTO>();
        rules.add(rule);
        when(cmd.getRules()).thenReturn(rules);
        when(cmd.getLogicalRouterUuid()).thenReturn("aaaaa");

        // Mock the api find call
        @SuppressWarnings("unchecked")
        final List<NatRule> storedRules = Collections.EMPTY_LIST;
        when(nvpApi.findNatRulesByLogicalRouterUuid("aaaaa")).thenReturn(storedRules);

        // Mock the api create calls
        final NatRule[] rulepair = resource.generatePortForwardingRulePair("10.10.10.10", new int[] { 80, 85 }, "11.11.11.11", new int[] { 80, 85 }, "tcp");
        rulepair[0].setUuid(UUID.randomUUID());
        rulepair[1].setUuid(UUID.randomUUID());
        when(nvpApi.createLogicalRouterNatRule(eq("aaaaa"), (NatRule) any())).thenReturn(rulepair[0]).thenReturn(rulepair[1]);

        final ConfigurePortForwardingRulesOnLogicalRouterAnswer a = (ConfigurePortForwardingRulesOnLogicalRouterAnswer) resource.executeRequest(cmd);

        // The expected result is false, Nicira does not support port ranges in DNAT
        assertFalse(a.getResult());

    }

    @Test
    public void testGenerateStaticNatRulePair() {
        final NatRule[] rules = resource.generateStaticNatRulePair("10.10.10.10", "11.11.11.11");
        assertTrue("DestinationNatRule".equals(rules[0].getType()));
        assertTrue("SourceNatRule".equals(rules[1].getType()));

        final DestinationNatRule dnr = (DestinationNatRule) rules[0];
        assertTrue(dnr.getToDestinationIpAddress().equals("10.10.10.10"));
        assertTrue(dnr.getToDestinationPort() == null);
        assertTrue(dnr.getMatch().getDestinationIpAddresses().equals("11.11.11.11"));

        final SourceNatRule snr = (SourceNatRule) rules[1];
        assertTrue(snr.getToSourceIpAddressMin().equals("11.11.11.11") && snr.getToSourceIpAddressMax().equals("11.11.11.11"));
        assertTrue(snr.getToSourcePort() == null);
        assertTrue(snr.getMatch().getSourceIpAddresses().equals("10.10.10.10"));
    }

    @Test
    public void testGeneratePortForwardingRulePair() {
        final NatRule[] rules = resource.generatePortForwardingRulePair("10.10.10.10", new int[] { 8080, 8080 }, "11.11.11.11", new int[] { 80, 80 }, "tcp");
        assertTrue("DestinationNatRule".equals(rules[0].getType()));
        assertTrue("SourceNatRule".equals(rules[1].getType()));

        final DestinationNatRule dnr = (DestinationNatRule) rules[0];
        assertTrue(dnr.getToDestinationIpAddress().equals("10.10.10.10"));
        assertTrue(dnr.getToDestinationPort() == 8080);
        assertTrue(dnr.getMatch().getDestinationIpAddresses().equals("11.11.11.11"));
        assertTrue(dnr.getMatch().getDestinationPort() == 80);
        assertTrue(dnr.getMatch().getEthertype().equals("IPv4") && dnr.getMatch().getProtocol() == 6);

        final SourceNatRule snr = (SourceNatRule) rules[1];
        assertTrue(snr.getToSourceIpAddressMin().equals("11.11.11.11") && snr.getToSourceIpAddressMax().equals("11.11.11.11"));
        assertTrue(snr.getToSourcePort() == 80);
        assertTrue(snr.getMatch().getSourceIpAddresses().equals("10.10.10.10"));
        assertTrue(snr.getMatch().getSourcePort() == 8080);
        assertTrue(snr.getMatch().getEthertype().equals("IPv4") && rules[1].getMatch().getProtocol() == 6);
    }
}
