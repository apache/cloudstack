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

package com.cloud.agent.resource.virtualnetwork;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.Test;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import com.cloud.agent.api.routing.DeleteIpAliasCommand;
import com.cloud.agent.api.routing.DnsMasqConfigCommand;
import com.cloud.agent.api.routing.IpAliasTO;
import com.cloud.agent.api.routing.IpAssocVpcCommand;
import com.cloud.agent.api.routing.LoadBalancerConfigCommand;
import com.cloud.agent.api.routing.NetworkElementCommand;
import com.cloud.agent.api.routing.SetPortForwardingRulesVpcCommand;
import com.cloud.agent.api.to.DhcpTO;
import com.cloud.agent.api.to.IpAddressTO;
import com.cloud.agent.api.to.LoadBalancerTO;
import com.cloud.agent.api.to.NicTO;
import com.cloud.agent.api.to.PortForwardingRuleTO;
import com.cloud.agent.resource.virtualnetwork.facade.AbstractConfigItemFacade;
import com.cloud.agent.resource.virtualnetwork.model.DhcpConfig;
import com.cloud.agent.resource.virtualnetwork.model.DhcpConfigEntry;
import com.cloud.agent.resource.virtualnetwork.model.ForwardingRule;
import com.cloud.agent.resource.virtualnetwork.model.ForwardingRules;
import com.cloud.agent.resource.virtualnetwork.model.IpAddress;
import com.cloud.agent.resource.virtualnetwork.model.IpAddressAlias;
import com.cloud.agent.resource.virtualnetwork.model.IpAliases;
import com.cloud.agent.resource.virtualnetwork.model.IpAssociation;
import com.cloud.agent.resource.virtualnetwork.model.LoadBalancerRule;
import com.cloud.agent.resource.virtualnetwork.model.LoadBalancerRules;
import com.cloud.network.lb.LoadBalancingRule.LbDestination;
import com.cloud.network.Networks.TrafficType;

public class ConfigHelperTest {

    private final static Gson gson = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create();

    private final String ROUTERNAME = "r-4-VM";

    @Test
    public void testGenerateCommandCfgLoadBalancer() {

        final LoadBalancerConfigCommand command = generateLoadBalancerConfigCommand();

        final AbstractConfigItemFacade configItemFacade = AbstractConfigItemFacade.getInstance(command.getClass());

        final List<ConfigItem> config = configItemFacade.generateConfig(command);
        assertTrue(config.size() > 0);

        final ConfigItem fileConfig = config.get(0);
        assertNotNull(fileConfig);
        assertTrue(fileConfig instanceof FileConfigItem);

        final String fileContents = ((FileConfigItem)fileConfig).getFileContents();
        assertNotNull(fileContents);

        final LoadBalancerRules jsonClass = gson.fromJson(fileContents, LoadBalancerRules.class);
        assertNotNull(jsonClass);
        assertEquals(jsonClass.getType(), "loadbalancer");

        final List<LoadBalancerRule> rules = jsonClass.getRules();
        assertNotNull(rules);
        assertTrue(rules.size() == 1);
        assertEquals(rules.get(0).getRouterIp(), "10.1.10.2");

        final ConfigItem scriptConfig = config.get(1);
        assertNotNull(scriptConfig);
        assertTrue(scriptConfig instanceof ScriptConfigItem);
    }

    @Test
    public void testSetPortForwardingRulesVpc() {

        final SetPortForwardingRulesVpcCommand command = generateSetPortForwardingRulesVpcCommand();

        final AbstractConfigItemFacade configItemFacade = AbstractConfigItemFacade.getInstance(command.getClass());

        final List<ConfigItem> config = configItemFacade.generateConfig(command);
        assertTrue(config.size() > 0);

        final ConfigItem fileConfig = config.get(0);
        assertNotNull(fileConfig);
        assertTrue(fileConfig instanceof FileConfigItem);

        final String fileContents = ((FileConfigItem)fileConfig).getFileContents();
        assertNotNull(fileContents);

        final ForwardingRules jsonClass = gson.fromJson(fileContents, ForwardingRules.class);
        assertNotNull(jsonClass);
        assertEquals(jsonClass.getType(), "forwardrules");

        final ForwardingRule [] rules = jsonClass.getRules();
        assertNotNull(rules);
        assertTrue(rules.length == 2);
        assertEquals(rules[0].getSourceIpAddress(), "64.1.1.10");

        final ConfigItem scriptConfig = config.get(1);
        assertNotNull(scriptConfig);
        assertTrue(scriptConfig instanceof ScriptConfigItem);
    }

    @Test
    public void testIpAssocVpc() {

        final IpAssocVpcCommand command = generateIpAssocVpcCommand();

        final AbstractConfigItemFacade configItemFacade = AbstractConfigItemFacade.getInstance(command.getClass());

        final List<ConfigItem> config = configItemFacade.generateConfig(command);
        assertTrue(config.size() > 0);

        final ConfigItem fileConfig = config.get(0);
        assertNotNull(fileConfig);
        assertTrue(fileConfig instanceof FileConfigItem);

        final String fileContents = ((FileConfigItem)fileConfig).getFileContents();
        assertNotNull(fileContents);

        final IpAssociation jsonClass = gson.fromJson(fileContents, IpAssociation.class);
        assertNotNull(jsonClass);
        assertEquals(jsonClass.getType(), "ips");

        final IpAddress [] ips = jsonClass.getIpAddress();
        assertNotNull(ips);
        assertTrue(ips.length == 3);
        assertEquals(ips[0].getPublicIp(), "64.1.1.10");

        final ConfigItem scriptConfig = config.get(1);
        assertNotNull(scriptConfig);
        assertTrue(scriptConfig instanceof ScriptConfigItem);
    }

    @Test
    public void testDnsMasqConfig() {

        final DnsMasqConfigCommand command = generateDnsMasqConfigCommand();

        final AbstractConfigItemFacade configItemFacade = AbstractConfigItemFacade.getInstance(command.getClass());

        final List<ConfigItem> config = configItemFacade.generateConfig(command);
        assertTrue(config.size() > 0);

        final ConfigItem fileConfig = config.get(0);
        assertNotNull(fileConfig);
        assertTrue(fileConfig instanceof FileConfigItem);

        final String fileContents = ((FileConfigItem)fileConfig).getFileContents();
        assertNotNull(fileContents);

        final DhcpConfig jsonClass = gson.fromJson(fileContents, DhcpConfig.class);
        assertNotNull(jsonClass);
        assertEquals(jsonClass.getType(), "dhcpconfig");

        final List<DhcpConfigEntry> entries = jsonClass.getEntries();
        assertNotNull(entries);
        assertTrue(entries.size() == 2);
        assertEquals(entries.get(0).getRouterIpAddress(), "10.1.20.2");

        final ConfigItem scriptConfig = config.get(1);
        assertNotNull(scriptConfig);
        assertTrue(scriptConfig instanceof ScriptConfigItem);
    }

    @Test
    public void testDeleteIpAlias() {

        final DeleteIpAliasCommand command = generateDeleteIpAliasCommand();

        final AbstractConfigItemFacade configItemFacade = AbstractConfigItemFacade.getInstance(command.getClass());

        final List<ConfigItem> config = configItemFacade.generateConfig(command);
        assertTrue(config.size() > 0);

        final ConfigItem fileConfig = config.get(0);
        assertNotNull(fileConfig);
        assertTrue(fileConfig instanceof FileConfigItem);

        final String fileContents = ((FileConfigItem)fileConfig).getFileContents();
        assertNotNull(fileContents);

        final IpAliases jsonClass = gson.fromJson(fileContents, IpAliases.class);
        assertNotNull(jsonClass);
        assertEquals(jsonClass.getType(), "ipaliases");

        final List<IpAddressAlias> aliases = jsonClass.getAliases();
        assertNotNull(aliases);
        assertTrue(aliases.size() == 6);
        assertEquals(aliases.get(0).getIpAddress(), "169.254.3.10");

        final ConfigItem scriptConfig = config.get(1);
        assertNotNull(scriptConfig);
        assertTrue(scriptConfig instanceof ScriptConfigItem);
    }

    protected LoadBalancerConfigCommand generateLoadBalancerConfigCommand() {
        final List<LoadBalancerTO> lbs = new ArrayList<>();
        final List<LbDestination> dests = new ArrayList<>();
        dests.add(new LbDestination(80, 8080, "10.1.10.2", false));
        dests.add(new LbDestination(80, 8080, "10.1.10.2", true));
        lbs.add(new LoadBalancerTO(UUID.randomUUID().toString(), "64.10.1.10", 80, "tcp", "algo", false, false, false, dests));

        final LoadBalancerTO[] arrayLbs = new LoadBalancerTO[lbs.size()];
        lbs.toArray(arrayLbs);

        final NicTO nic = new NicTO();
        final LoadBalancerConfigCommand cmd = new LoadBalancerConfigCommand(arrayLbs, "64.10.2.10", "10.1.10.2", "192.168.1.2", nic, null, "1000", false);
        cmd.setAccessDetail(NetworkElementCommand.ROUTER_IP, "10.1.10.2");
        cmd.setAccessDetail(NetworkElementCommand.ROUTER_NAME, ROUTERNAME);

        return cmd;
    }

    protected SetPortForwardingRulesVpcCommand generateSetPortForwardingRulesVpcCommand() {
        final List<PortForwardingRuleTO> pfRules = new ArrayList<>();
        pfRules.add(new PortForwardingRuleTO(1, "64.1.1.10", 22, 80, "10.10.1.10", 22, 80, "TCP", false, false));
        pfRules.add(new PortForwardingRuleTO(2, "64.1.1.11", 8080, 8080, "10.10.1.11", 8080, 8080, "UDP", true, false));

        final SetPortForwardingRulesVpcCommand cmd = new SetPortForwardingRulesVpcCommand(pfRules);
        cmd.setAccessDetail(NetworkElementCommand.ROUTER_NAME, ROUTERNAME);
        assertEquals(cmd.getAnswersCount(), 2);

        return cmd;
    }

    protected DnsMasqConfigCommand generateDnsMasqConfigCommand() {
        final List<DhcpTO> dhcps = new ArrayList<>();
        dhcps.add(new DhcpTO("10.1.20.2", "10.1.20.1", "255.255.255.0", "10.1.20.5"));
        dhcps.add(new DhcpTO("10.1.21.2", "10.1.21.1", "255.255.255.0", "10.1.21.5"));

        final DnsMasqConfigCommand cmd = new DnsMasqConfigCommand(dhcps);
        cmd.setAccessDetail(NetworkElementCommand.ROUTER_NAME, ROUTERNAME);
        return cmd;
    }

    protected DeleteIpAliasCommand generateDeleteIpAliasCommand() {
        final List<IpAliasTO> aliases = new ArrayList<>();
        aliases.add(new IpAliasTO("169.254.3.10", "255.255.255.0", "1"));
        aliases.add(new IpAliasTO("169.254.3.11", "255.255.255.0", "2"));
        aliases.add(new IpAliasTO("169.254.3.12", "255.255.255.0", "3"));

        final DeleteIpAliasCommand cmd = new DeleteIpAliasCommand("169.254.10.1", aliases, aliases);
        cmd.setAccessDetail(NetworkElementCommand.ROUTER_NAME, ROUTERNAME);
        return cmd;
    }

    protected IpAssocVpcCommand generateIpAssocVpcCommand() {
        final List<IpAddressTO> ips = new ArrayList<IpAddressTO>();
        IpAddressTO ip1 = new IpAddressTO(1, "64.1.1.10", true, true, true, "vlan://64", "64.1.1.1", "255.255.255.0", "01:23:45:67:89:AB", 1000, false);
        IpAddressTO ip2 = new IpAddressTO(2, "64.1.1.11", false, false, true, "vlan://64", "64.1.1.1", "255.255.255.0", "01:23:45:67:89:AB", 1000, false);
        IpAddressTO ip3 = new IpAddressTO(3, "65.1.1.11", true, false, false, "vlan://65", "65.1.1.1", "255.255.255.0", "11:23:45:67:89:AB", 1000, false);
        ip1.setTrafficType(TrafficType.Public);
        ip2.setTrafficType(TrafficType.Public);
        ip3.setTrafficType(TrafficType.Public);
        ips.add(ip1);
        ips.add(ip2);
        ips.add(ip3);

        final IpAddressTO[] ipArray = ips.toArray(new IpAddressTO[ips.size()]);
        final IpAssocVpcCommand cmd = new IpAssocVpcCommand(ipArray);
        cmd.setAccessDetail(NetworkElementCommand.ROUTER_NAME, ROUTERNAME);
        assertEquals(2, cmd.getAnswersCount());

        return cmd;
    }
}
