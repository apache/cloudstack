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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import javax.naming.ConfigurationException;

import org.joda.time.Duration;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.BumpUpPriorityCommand;
import com.cloud.agent.api.SetupGuestNetworkCommand;
import com.cloud.agent.api.routing.AggregationControlCommand;
import com.cloud.agent.api.routing.AggregationControlCommand.Action;
import com.cloud.agent.api.routing.CreateIpAliasCommand;
import com.cloud.agent.api.routing.DeleteIpAliasCommand;
import com.cloud.agent.api.routing.DhcpEntryCommand;
import com.cloud.agent.api.routing.DnsMasqConfigCommand;
import com.cloud.agent.api.routing.GroupAnswer;
import com.cloud.agent.api.routing.IpAliasTO;
import com.cloud.agent.api.routing.IpAssocCommand;
import com.cloud.agent.api.routing.IpAssocVpcCommand;
import com.cloud.agent.api.routing.LoadBalancerConfigCommand;
import com.cloud.agent.api.routing.NetworkElementCommand;
import com.cloud.agent.api.routing.RemoteAccessVpnCfgCommand;
import com.cloud.agent.api.routing.SavePasswordCommand;
import com.cloud.agent.api.routing.SetFirewallRulesCommand;
import com.cloud.agent.api.routing.SetMonitorServiceCommand;
import com.cloud.agent.api.routing.SetNetworkACLCommand;
import com.cloud.agent.api.routing.SetPortForwardingRulesCommand;
import com.cloud.agent.api.routing.SetPortForwardingRulesVpcCommand;
import com.cloud.agent.api.routing.SetSourceNatCommand;
import com.cloud.agent.api.routing.SetStaticNatRulesCommand;
import com.cloud.agent.api.routing.SetStaticRouteCommand;
import com.cloud.agent.api.routing.Site2SiteVpnCfgCommand;
import com.cloud.agent.api.routing.VmDataCommand;
import com.cloud.agent.api.routing.VpnUsersCfgCommand;
import com.cloud.agent.api.to.DhcpTO;
import com.cloud.agent.api.to.FirewallRuleTO;
import com.cloud.agent.api.to.IpAddressTO;
import com.cloud.agent.api.to.LoadBalancerTO;
import com.cloud.agent.api.to.MonitorServiceTO;
import com.cloud.agent.api.to.NetworkACLTO;
import com.cloud.agent.api.to.NicTO;
import com.cloud.agent.api.to.PortForwardingRuleTO;
import com.cloud.network.lb.LoadBalancingRule.LbDestination;
import com.cloud.network.rules.FirewallRule.Purpose;
import com.cloud.network.vpc.NetworkACLItem.TrafficType;
import com.cloud.network.vpc.VpcGateway;
import com.cloud.utils.ExecutionResult;
import com.cloud.utils.net.NetUtils;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(loader = AnnotationConfigContextLoader.class)
@Ignore("Just forget until the rewrite is a little more done")
public class VirtualRoutingResourceTest implements VirtualRouterDeployer {
    VirtualRoutingResource _resource;
    NetworkElementCommand _currentCmd;
    int _count;
    String _file;

    String ROUTERIP = "169.254.3.4";
    String ROUTERGUESTIP = "10.200.1.1";
    String ROUTERNAME = "r-4-VM";

    @Override
    public ExecutionResult executeInVR(final String routerIp, final String script, final String args) {
        return executeInVR(routerIp, script, args, Duration.standardSeconds(60L));
    }

    @Override
    public ExecutionResult executeInVR(final String routerIp, final String script, final String args, final Duration timeout) {
        assertEquals(routerIp, ROUTERIP);
        verifyCommand(_currentCmd, script, args);
        return new ExecutionResult(true, null);
    }

    @Override
    public ExecutionResult createFileInVR(final String routerIp, final String path, final String filename, final String content) {
        assertEquals(routerIp, ROUTERIP);
        verifyFile(_currentCmd, path, filename, content);
        return new ExecutionResult(true, null);
    }

    @Override
    public ExecutionResult prepareCommand(final NetworkElementCommand cmd) {
        cmd.setRouterAccessIp(ROUTERIP);
        _currentCmd = cmd;
        if (cmd instanceof IpAssocVpcCommand) {
            return prepareNetworkElementCommand((IpAssocVpcCommand)cmd);
        } else if (cmd instanceof IpAssocCommand) {
            return prepareNetworkElementCommand((IpAssocCommand)cmd);
        } else if (cmd instanceof SetupGuestNetworkCommand) {
            return prepareNetworkElementCommand((SetupGuestNetworkCommand)cmd);
        } else if (cmd instanceof SetSourceNatCommand) {
            return prepareNetworkElementCommand((SetSourceNatCommand)cmd);
        } else if (cmd instanceof SetNetworkACLCommand) {
            return prepareNetworkElementCommand((SetNetworkACLCommand)cmd);
        }
        return new ExecutionResult(true, null);
    }

    @Override
    public ExecutionResult cleanupCommand(final NetworkElementCommand cmd) {
        return new ExecutionResult(true, null);
    }

    @Before
    public void setup() {
        _resource = new VirtualRoutingResource(this);
        try {
            _resource.configure("VRResource", new HashMap<String, Object>());
        } catch (final ConfigurationException e) {
            e.printStackTrace();
        }
    }

    private void verifyFile(final NetworkElementCommand cmd, final String path, final String filename, final String content) {
        if (cmd instanceof AggregationControlCommand) {
            verifyFile(cmd, path, filename, content);
        } else if (cmd instanceof LoadBalancerConfigCommand) {
            verifyFile((LoadBalancerConfigCommand)cmd, path, filename, content);
        }
    }

    protected void verifyCommand(final NetworkElementCommand cmd, final String script, final String args) {
        if (cmd instanceof SetStaticRouteCommand) {
            verifyArgs((SetStaticRouteCommand) cmd, script, args);
        } else if (cmd instanceof SetStaticNatRulesCommand) {
            verifyArgs((SetStaticNatRulesCommand) cmd, script, args);
        } else if (cmd instanceof LoadBalancerConfigCommand) {
            verifyArgs((LoadBalancerConfigCommand) cmd, script, args);
        } else if (cmd instanceof SavePasswordCommand) {
            verifyArgs((SavePasswordCommand)cmd, script, args);
        } else if (cmd instanceof DhcpEntryCommand) {
            verifyArgs((DhcpEntryCommand)cmd, script, args);
        } else if (cmd instanceof DnsMasqConfigCommand) {
            verifyArgs((DnsMasqConfigCommand)cmd, script, args);
        } else if (cmd instanceof VmDataCommand) {
            verifyArgs((VmDataCommand)cmd, script, args);
        } else if (cmd instanceof RemoteAccessVpnCfgCommand) {
            verifyArgs((RemoteAccessVpnCfgCommand)cmd, script, args);
        } else if (cmd instanceof VpnUsersCfgCommand) {
            verifyArgs((VpnUsersCfgCommand)cmd, script, args);
        } else if (cmd instanceof Site2SiteVpnCfgCommand) {
            verifyArgs((Site2SiteVpnCfgCommand)cmd, script, args);
        } else if (cmd instanceof SetMonitorServiceCommand) {
            verifyArgs((SetMonitorServiceCommand)cmd, script, args);
        } else if (cmd instanceof SetupGuestNetworkCommand) {
            verifyArgs((SetupGuestNetworkCommand)cmd, script, args);
        } else if (cmd instanceof SetNetworkACLCommand) {
            verifyArgs((SetNetworkACLCommand)cmd, script, args);
        } else if (cmd instanceof SetSourceNatCommand) {
            verifyArgs((SetSourceNatCommand)cmd, script, args);
        } else if (cmd instanceof IpAssocCommand) {
            verifyArgs((IpAssocCommand)cmd, script, args);
        }

        if (cmd instanceof AggregationControlCommand) {
            verifyArgs((AggregationControlCommand)cmd, script, args);
        }
    }

    private void verifyArgs(final VpnUsersCfgCommand cmd, final String script, final String args) {
        //To change body of created methods use File | Settings | File Templates.
    }

    private void verifyArgs(final SetStaticRouteCommand cmd, final String script, final String args) {
        //To change body of created methods use File | Settings | File Templates.
    }

    private void verifyArgs(final SetStaticNatRulesCommand cmd, final String script, final String args) {
        //To change body of created methods use File | Settings | File Templates.
    }

    @Test
    public void testBumpUpCommand() {
        final BumpUpPriorityCommand cmd = new BumpUpPriorityCommand();
        final Answer answer = _resource.executeRequest(cmd);
        assertTrue(answer.getResult());
    }

    @Test
    public void testSetPortForwardingRulesVpcCommand() {
        final SetPortForwardingRulesVpcCommand cmd = generateSetPortForwardingRulesVpcCommand();

        // Reset rule check count
        _count = 0;

        final Answer answer = _resource.executeRequest(cmd);
        assertTrue(answer instanceof GroupAnswer);
        assertEquals(((GroupAnswer) answer).getResults().length, 2);
        assertTrue(answer.getResult());
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

    @Test
    public void testSetPortForwardingRulesCommand() {
        final SetPortForwardingRulesCommand cmd = generateSetPortForwardingRulesCommand();
        // Reset rule check count
        _count = 0;

        final Answer answer = _resource.executeRequest(cmd);
        assertTrue(answer instanceof GroupAnswer);
        assertEquals(((GroupAnswer) answer).getResults().length, 2);
        assertTrue(answer.getResult());
    }

    protected SetPortForwardingRulesCommand generateSetPortForwardingRulesCommand() {
        final List<PortForwardingRuleTO> pfRules = new ArrayList<>();
        pfRules.add(new PortForwardingRuleTO(1, "64.1.1.10", 22, 80, "10.10.1.10", 22, 80, "TCP", false, false));
        pfRules.add(new PortForwardingRuleTO(2, "64.1.1.11", 8080, 8080, "10.10.1.11", 8080, 8080, "UDP", true, false));
        final SetPortForwardingRulesCommand cmd = new SetPortForwardingRulesCommand(pfRules);
        cmd.setAccessDetail(NetworkElementCommand.ROUTER_NAME, ROUTERNAME);
        assertEquals(cmd.getAnswersCount(), 2);
        return cmd;
    }

    @Test
    public void testIpAssocCommand() {
        final IpAssocCommand cmd = generateIpAssocCommand();
        _count = 0;

        final Answer answer = _resource.executeRequest(cmd);
        assertTrue(answer instanceof GroupAnswer);
        assertEquals(2, ((GroupAnswer)answer).getResults().length);
        assertTrue(answer.getResult());

    }

    private ExecutionResult prepareNetworkElementCommand(final IpAssocCommand cmd) {
        final IpAddressTO[] ips = cmd.getIpAddresses();
        for (final IpAddressTO ip : ips) {
            ip.setNicDevId(2);
        }
        return new ExecutionResult(true, null);
    }

    protected IpAssocCommand generateIpAssocCommand() {
        final List<IpAddressTO> ips = new ArrayList<>();
        ips.add(new IpAddressTO(1, "64.1.1.10", true, true, true, "vlan://64", "64.1.1.1", "255.255.255.0", "01:23:45:67:89:AB", 1000, false));
        ips.add(new IpAddressTO(2, "64.1.1.11", false, false, false, "vlan://64", "64.1.1.1", "255.255.255.0", "01:23:45:67:89:AB", 1000, false));
        ips.add(new IpAddressTO(3, "65.1.1.11", true, false, false, "vlan://65", "65.1.1.1", "255.255.255.0", "11:23:45:67:89:AB", 1000, false));
        final IpAddressTO[] ipArray = ips.toArray(new IpAddressTO[ips.size()]);
        final IpAssocCommand cmd = new IpAssocCommand(ipArray);
        cmd.setAccessDetail(NetworkElementCommand.ROUTER_NAME, ROUTERNAME);
        assertEquals(cmd.getAnswersCount(), 3);

        return cmd;
    }

    @Test
    public void testIpAssocVpcCommand() {
        final IpAssocVpcCommand cmd = generateIpAssocVpcCommand();
        _count = 0;

        final Answer answer = _resource.executeRequest(cmd);
        assertTrue(answer instanceof GroupAnswer);
        assertEquals(2, ((GroupAnswer)answer).getResults().length);
        assertTrue(answer.getResult());

    }

    private ExecutionResult prepareNetworkElementCommand(final IpAssocVpcCommand cmd) {
        final IpAddressTO[] ips = cmd.getIpAddresses();
        for (final IpAddressTO ip : ips) {
            ip.setNicDevId(2);
        }
        return new ExecutionResult(true, null);
    }

    protected IpAssocVpcCommand generateIpAssocVpcCommand() {
        final List<IpAddressTO> ips = new ArrayList<IpAddressTO>();
        ips.add(new IpAddressTO(1, "64.1.1.10", true, true, true, "vlan://64", "64.1.1.1", "255.255.255.0", "01:23:45:67:89:AB", 1000, false));
        ips.add(new IpAddressTO(2, "64.1.1.11", false, false, true, "vlan://64", "64.1.1.1", "255.255.255.0", "01:23:45:67:89:AB", 1000, false));
        ips.add(new IpAddressTO(3, "65.1.1.11", true, false, false, "vlan://65", "65.1.1.1", "255.255.255.0", "11:23:45:67:89:AB", 1000, false));
        final IpAddressTO[] ipArray = ips.toArray(new IpAddressTO[ips.size()]);
        final IpAssocVpcCommand cmd = new IpAssocVpcCommand(ipArray);
        cmd.setAccessDetail(NetworkElementCommand.ROUTER_NAME, ROUTERNAME);
        assertEquals(6, cmd.getAnswersCount()); // AnswersCount is clearly wrong as it doesn't know enough to tell

        return cmd;
    }

    private void verifyArgs(final IpAssocCommand cmd, final String script, final String args) {
        if (cmd instanceof IpAssocVpcCommand) {
            _count ++;
            switch (_count) {
            case 1:
                assertEquals(VRScripts.UPDATE_CONFIG, script);
                assertEquals(VRScripts.IP_ASSOCIATION_CONFIG, args);
                break;
            default:
                fail("Failed to recongize the match!");
            }
        } else {
            assertEquals(script, VRScripts.UPDATE_CONFIG);
            _count ++;
            switch (_count) {
            case 1:
                assertEquals(VRScripts.IP_ASSOCIATION_CONFIG, args);
                break;
            case 2:
                assertEquals(VRScripts.IP_ASSOCIATION_CONFIG, args);
                break;
            case 3:
                assertEquals(VRScripts.IP_ASSOCIATION_CONFIG, args);
                break;
            default:
                fail("Failed to recongize the match!");
            }
        }
    }

    @Test
    public void testSourceNatCommand() {
        final SetSourceNatCommand cmd = generateSetSourceNatCommand();
        final Answer answer = _resource.executeRequest(cmd);
        assertTrue(answer.getResult());
    }

    private ExecutionResult prepareNetworkElementCommand(final SetSourceNatCommand cmd) {
        final IpAddressTO ip = cmd.getIpAddress();
        ip.setNicDevId(1);
        return new ExecutionResult(true, null);
    }

    protected SetSourceNatCommand generateSetSourceNatCommand() {
        final IpAddressTO ip = new IpAddressTO(1, "64.1.1.10", true, true, true, "vlan://64", "64.1.1.1", "255.255.255.0", "01:23:45:67:89:AB", 1000, false);
        final SetSourceNatCommand cmd = new SetSourceNatCommand(ip, true);
        cmd.setAccessDetail(NetworkElementCommand.ROUTER_NAME, ROUTERNAME);
        return cmd;
    }

    private void verifyArgs(final SetSourceNatCommand cmd, final String script, final String args) {
        assertEquals(script, VRScripts.VPC_SOURCE_NAT);
        assertEquals(args, "-A -l 64.1.1.10 -c eth1");
    }

    @Test
    public void testNetworkACLCommand() {
        final SetNetworkACLCommand cmd = generateSetNetworkACLCommand();
        _count = 0;

        Answer answer = _resource.executeRequest(cmd);
        assertTrue(answer.getResult());

        cmd.setAccessDetail(NetworkElementCommand.VPC_PRIVATE_GATEWAY, String.valueOf(VpcGateway.Type.Private));
        answer = _resource.executeRequest(cmd);
        assertTrue(answer.getResult());
    }

    protected SetNetworkACLCommand generateSetNetworkACLCommand() {
        final List<NetworkACLTO> acls = new ArrayList<>();
        final List<String> cidrs = new ArrayList<>();
        cidrs.add("192.168.0.1/24");
        cidrs.add("192.168.0.2/24");
        acls.add(new NetworkACLTO(1, "64", "TCP", 20, 80, false, false, cidrs, 0, 0, TrafficType.Ingress, true, 1));
        acls.add(new NetworkACLTO(2, "64", "ICMP", 0, 0, false, false, cidrs, -1, -1, TrafficType.Ingress, false, 2));
        acls.add(new NetworkACLTO(3, "65", "ALL", 0, 0, false, false, cidrs, -1, -1, TrafficType.Egress, true, 3));
        final NicTO nic = new NicTO();
        nic.setMac("01:23:45:67:89:AB");
        nic.setIp("192.168.1.1");
        nic.setNetmask("255.255.255.0");
        final SetNetworkACLCommand cmd = new SetNetworkACLCommand(acls, nic);
        cmd.setAccessDetail(NetworkElementCommand.ROUTER_NAME, ROUTERNAME);

        return cmd;
    }

    private void verifyArgs(final SetNetworkACLCommand cmd, final String script, final String args) {
        _count ++;
        switch (_count) {
        case 1:
            // FIXME Check the json content
            assertEquals(VRScripts.UPDATE_CONFIG, script);
            assertEquals(VRScripts.NETWORK_ACL_CONFIG, args);
            // assertEquals(args, " -d eth3 -M 01:23:45:67:89:AB -i 192.168.1.1 -m 24 -a Egress:ALL:0:0:192.168.0.1/24-192.168.0.2/24:ACCEPT:," +
            //        "Ingress:ICMP:0:0:192.168.0.1/24-192.168.0.2/24:DROP:,Ingress:TCP:20:80:192.168.0.1/24-192.168.0.2/24:ACCEPT:,");
            break;
        case 2:
            assertEquals(VRScripts.UPDATE_CONFIG, script);
            assertEquals(VRScripts.NETWORK_ACL_CONFIG, args);
            break;
        default:
            fail();
        }
    }

    private ExecutionResult prepareNetworkElementCommand(final SetNetworkACLCommand cmd) {
        final NicTO nic = cmd.getNic();
        nic.setDeviceId(3);
        return new ExecutionResult(true, null);
    }

    @Test
    public void testSetupGuestNetworkCommand() {
        final SetupGuestNetworkCommand cmd = generateSetupGuestNetworkCommand();
        final Answer answer = _resource.executeRequest(cmd);
        assertTrue(answer.getResult());
    }

    private ExecutionResult prepareNetworkElementCommand(final SetupGuestNetworkCommand cmd) {
        final NicTO nic = cmd.getNic();
        nic.setDeviceId(4);
        return new ExecutionResult(true, null);
    }

    protected SetupGuestNetworkCommand generateSetupGuestNetworkCommand() {
        final NicTO nic = new NicTO();
        nic.setMac("01:23:45:67:89:AB");
        nic.setIp("10.1.1.1");
        nic.setNetmask("255.255.255.0");

        final SetupGuestNetworkCommand cmd = new SetupGuestNetworkCommand("10.1.1.10-10.1.1.20", "cloud.test", false, "8.8.8.8", "8.8.4.4", true, nic);
        cmd.setAccessDetail(NetworkElementCommand.ROUTER_GUEST_IP, "10.1.1.2");
        cmd.setAccessDetail(NetworkElementCommand.GUEST_NETWORK_GATEWAY, "10.1.1.1");
        cmd.setAccessDetail(NetworkElementCommand.ROUTER_NAME, ROUTERNAME);

        return cmd;
    }

    private void verifyArgs(final SetupGuestNetworkCommand cmd, final String script, final String args) {
        // TODO Check the contents of the json file
        //assertEquals(script, VRScripts.VPC_GUEST_NETWORK);
        //assertEquals(args, " -C -M 01:23:45:67:89:AB -d eth4 -i 10.1.1.2 -g 10.1.1.1 -m 24 -n 10.1.1.0 -s 8.8.8.8,8.8.4.4 -e cloud.test");
    }

    @Test
    public void testSetMonitorServiceCommand() {
        final SetMonitorServiceCommand cmd = generateSetMonitorServiceCommand();
        final Answer answer = _resource.executeRequest(cmd);
        assertTrue(answer.getResult());
    }

    protected SetMonitorServiceCommand generateSetMonitorServiceCommand() {
        final List<MonitorServiceTO> services = new ArrayList<>();
        services.add(new MonitorServiceTO("service", "process", "name", "path", "file", true));
        services.add(new MonitorServiceTO("service_2", "process_2", "name_2", "path_2", "file_2", false));

        final SetMonitorServiceCommand cmd = new SetMonitorServiceCommand(services);
        cmd.setAccessDetail(NetworkElementCommand.ROUTER_NAME, ROUTERNAME);

        return cmd;
    }

    private void verifyArgs(final SetMonitorServiceCommand cmd, final String script, final String args) {
        assertEquals(script, VRScripts.MONITOR_SERVICE);
        assertEquals(args, " -c [service]:processname=process:servicename=name:pidfile=file:,[service_2]:processname=process_2:servicename=name_2:pidfile=file_2:,");
    }

    @Test
    public void testSite2SiteVpnCfgCommand() {
        _count = 0;

        Site2SiteVpnCfgCommand cmd = new Site2SiteVpnCfgCommand(true, "64.10.1.10", "64.10.1.1", "192.168.1.1/16", "124.10.1.10", "192.168.100.1/24", "3des-sha1,aes128-sha1;modp1536", "3des-sha1,aes128-md5", "psk", Long.valueOf(1800), Long.valueOf(1800), true, false, false, false, "ike");
        cmd.setAccessDetail(NetworkElementCommand.ROUTER_NAME, ROUTERNAME);
        Answer answer = _resource.executeRequest(cmd);
        assertTrue(answer.getResult());

        cmd = new Site2SiteVpnCfgCommand(true, "64.10.1.10", "64.10.1.1", "192.168.1.1/16", "124.10.1.10", "192.168.100.1/24", "3des-sha1,aes128-sha1;modp1536", "3des-sha1,aes128-md5", "psk", Long.valueOf(1800), Long.valueOf(1800), false, true, false, false, "ike");
        cmd.setAccessDetail(NetworkElementCommand.ROUTER_NAME, ROUTERNAME);
        answer = _resource.executeRequest(cmd);
        assertTrue(answer.getResult());

        cmd = new Site2SiteVpnCfgCommand(false, "64.10.1.10", "64.10.1.1", "192.168.1.1/16", "124.10.1.10", "192.168.100.1/24", "3des-sha1,aes128-sha1;modp1536", "3des-sha1,aes128-md5", "psk", Long.valueOf(1800), Long.valueOf(1800), false, true, false, false, "ike");
        cmd.setAccessDetail(NetworkElementCommand.ROUTER_NAME, ROUTERNAME);
        answer = _resource.executeRequest(cmd);
        assertTrue(answer.getResult());
    }

    private void verifyArgs(final Site2SiteVpnCfgCommand cmd, final String script, final String args) {
        _count ++;

        assertEquals(script, VRScripts.S2SVPN_IPSEC);
        switch (_count) {
        case 1:
            assertEquals(args, "-A -l 64.10.1.10 -n 192.168.1.1/16 -g 64.10.1.1 -r 124.10.1.10 -N 192.168.100.1/24 -e \"3des-sha1,aes128-md5\" -i \"3des-sha1,aes128-sha1;modp1536\" -t 1800 -T 1800 -s \"psk\" -d 1");
            break;
        case 2:
            assertEquals(args, "-A -l 64.10.1.10 -n 192.168.1.1/16 -g 64.10.1.1 -r 124.10.1.10 -N 192.168.100.1/24 -e \"3des-sha1,aes128-md5\" -i \"3des-sha1,aes128-sha1;modp1536\" -t 1800 -T 1800 -s \"psk\" -d 0 -p ");
            break;
        case 3:
            assertEquals(args, "-D -r 124.10.1.10 -n 192.168.1.1/16 -N 192.168.100.1/24");
            break;
        default:
            fail();
        }
    }

    @Test
    public void testRemoteAccessVpnCfgCommand() {
        _count = 0;

        Answer answer = _resource.executeRequest(generateRemoteAccessVpnCfgCommand1());
        assertTrue(answer.getResult());

        answer = _resource.executeRequest(generateRemoteAccessVpnCfgCommand2());
        assertTrue(answer.getResult());

        answer = _resource.executeRequest(generateRemoteAccessVpnCfgCommand3());
        assertTrue(answer.getResult());
    }

    protected RemoteAccessVpnCfgCommand generateRemoteAccessVpnCfgCommand1() {
        final RemoteAccessVpnCfgCommand cmd = new RemoteAccessVpnCfgCommand(true, "124.10.10.10", "10.10.1.1", "10.10.1.10-10.10.1.20", "sharedkey", false);
        cmd.setAccessDetail(NetworkElementCommand.ROUTER_NAME, ROUTERNAME);
        cmd.setLocalCidr("10.1.1.1/24");
        return cmd;
    }

    protected RemoteAccessVpnCfgCommand generateRemoteAccessVpnCfgCommand2() {
        final RemoteAccessVpnCfgCommand cmd = new RemoteAccessVpnCfgCommand(false, "124.10.10.10", "10.10.1.1", "10.10.1.10-10.10.1.20", "sharedkey", false);
        cmd.setAccessDetail(NetworkElementCommand.ROUTER_NAME, ROUTERNAME);
        cmd.setLocalCidr("10.1.1.1/24");
        return cmd;
    }

    protected RemoteAccessVpnCfgCommand generateRemoteAccessVpnCfgCommand3() {
        final RemoteAccessVpnCfgCommand cmd = new RemoteAccessVpnCfgCommand(true, "124.10.10.10", "10.10.1.1", "10.10.1.10-10.10.1.20", "sharedkey", true);
        cmd.setAccessDetail(NetworkElementCommand.ROUTER_NAME, ROUTERNAME);
        cmd.setLocalCidr("10.1.1.1/24");
        return cmd;
    }

    private void verifyArgs(final RemoteAccessVpnCfgCommand cmd, final String script, final String args) {
        _count ++;

        assertEquals(script, VRScripts.VPN_L2TP);
        switch (_count) {
        case 1:
            assertEquals(args, "-r 10.10.1.10-10.10.1.20 -p sharedkey -s 124.10.10.10 -l 10.10.1.1 -c  -C 10.1.1.1/24 -i eth2");
            break;
        case 2:
            assertEquals(args, "-d  -s 124.10.10.10 -C 10.1.1.1/24 -i eth2");
            break;
        case 3:
            assertEquals(args, "-r 10.10.1.10-10.10.1.20 -p sharedkey -s 124.10.10.10 -l 10.10.1.1 -c  -C 10.1.1.1/24 -i eth1");
            break;
        default:
            fail();

        }
    }

    @Test
    public void testFirewallRulesCommand() {
        _count = 0;

        final Answer answer = _resource.executeRequest(generateSetFirewallRulesCommand());
        assertTrue(answer.getResult());

        //TODO Didn't test egress rule because not able to generate FirewallRuleVO object
    }

    protected SetFirewallRulesCommand generateSetFirewallRulesCommand() {
        final List<FirewallRuleTO> rules = new ArrayList<>();
        final List<String> sourceCidrs = new ArrayList<>();
        sourceCidrs.add("10.10.1.1/24");
        sourceCidrs.add("10.10.1.2/24");
        rules.add(new FirewallRuleTO(1, "64.10.10.10", "TCP", 22, 80, false, false, Purpose.Firewall, sourceCidrs, 0, 0));
        rules.add(new FirewallRuleTO(2, "64.10.10.10", "ICMP", 0, 0, false, false, Purpose.Firewall, sourceCidrs, -1, -1));
        rules.add(new FirewallRuleTO(3, "64.10.10.10", "ICMP", 0, 0, true, true, Purpose.Firewall, sourceCidrs, -1, -1));
        final SetFirewallRulesCommand cmd = new SetFirewallRulesCommand(rules);
        cmd.setAccessDetail(NetworkElementCommand.ROUTER_NAME, ROUTERNAME);

        return cmd;
    }

    @Test
    public void testVmDataCommand() {
        final Answer answer = _resource.executeRequest(generateVmDataCommand());
        assertTrue(answer.getResult());
    }

    protected VmDataCommand generateVmDataCommand() {
        final VmDataCommand cmd = new VmDataCommand("10.1.10.4", "i-4-VM", true);
        // if you add new metadata files, also edit systemvm/patches/debian/config/var/www/html/latest/.htaccess
        cmd.addVmData("userdata", "user-data", "user-data");
        cmd.addVmData("metadata", "service-offering", "serviceOffering");
        cmd.addVmData("metadata", "availability-zone", "zoneName");
        cmd.addVmData("metadata", "local-ipv4", "10.1.10.4");
        cmd.addVmData("metadata", "local-hostname", "test-vm");
        cmd.addVmData("metadata", "public-ipv4", "110.1.10.4");
        cmd.addVmData("metadata", "public-hostname", "hostname");
        cmd.addVmData("metadata", "instance-id", "i-4-VM");
        cmd.addVmData("metadata", "vm-id", "4");
        cmd.addVmData("metadata", "public-keys", "publickey");
        cmd.addVmData("metadata", "cloud-identifier", "CloudStack-{test}");

        cmd.setAccessDetail(NetworkElementCommand.ROUTER_NAME, ROUTERNAME);

        return cmd;
    }

    private void verifyArgs(final VmDataCommand cmd, final String script, final String args) {
        assertEquals(script, VRScripts.UPDATE_CONFIG);
        assertEquals(args, VRScripts.VM_METADATA_CONFIG);
    }

    @Test
    public void testSavePasswordCommand() {
        final Answer answer = _resource.executeRequest(generateSavePasswordCommand());
        assertTrue(answer.getResult());
    }

    protected SavePasswordCommand generateSavePasswordCommand() {
        final SavePasswordCommand cmd = new SavePasswordCommand("123pass", "10.1.10.4", "i-4-VM", true);
        cmd.setAccessDetail(NetworkElementCommand.ROUTER_NAME, ROUTERNAME);
        return cmd;
    }

    private void verifyArgs(final SavePasswordCommand cmd, final String script, final String args) {
        assertEquals(script, VRScripts.PASSWORD);
        assertEquals(args, "-v 10.1.10.4 -p 123pass");
    }

    @Test
    public void testDhcpEntryCommand() {
        _count = 0;

        Answer answer = _resource.executeRequest(generateDhcpEntryCommand1());
        assertTrue(answer.getResult());

        answer = _resource.executeRequest(generateDhcpEntryCommand2());
        assertTrue(answer.getResult());

        answer = _resource.executeRequest(generateDhcpEntryCommand3());
        assertTrue(answer.getResult());
    }

    protected DhcpEntryCommand generateDhcpEntryCommand1() {
        final DhcpEntryCommand cmd = new DhcpEntryCommand("12:34:56:78:90:AB", "10.1.10.2", "vm1", null, true);
        cmd.setAccessDetail(NetworkElementCommand.ROUTER_NAME, ROUTERNAME);
        return cmd;
    }

    protected DhcpEntryCommand generateDhcpEntryCommand2() {
        final DhcpEntryCommand cmd = new DhcpEntryCommand("12:34:56:78:90:AB", null, "vm1", "2001:db8:0:0:0:ff00:42:8329", true);
        cmd.setAccessDetail(NetworkElementCommand.ROUTER_NAME, ROUTERNAME);
        cmd.setDuid(NetUtils.getDuidLL(cmd.getVmMac()));
        return cmd;
    }

    protected DhcpEntryCommand generateDhcpEntryCommand3() {
        final DhcpEntryCommand cmd = new DhcpEntryCommand("12:34:56:78:90:AB", "10.1.10.2", "vm1", "2001:db8:0:0:0:ff00:42:8329", true);
        cmd.setAccessDetail(NetworkElementCommand.ROUTER_NAME, ROUTERNAME);
        cmd.setDuid(NetUtils.getDuidLL(cmd.getVmMac()));
        return cmd;
    }

    private void verifyArgs(final DhcpEntryCommand cmd, final String script, final String args) {
        _count ++;
        assertEquals(script, VRScripts.DHCP);
        switch (_count) {
        case 1:
            assertEquals(args, " -m 12:34:56:78:90:AB -4 10.1.10.2 -h vm1");
            break;
        case 2:
            assertEquals(args, " -m 12:34:56:78:90:AB -h vm1 -6 2001:db8:0:0:0:ff00:42:8329 -u 00:03:00:01:12:34:56:78:90:AB");
            break;
        case 3:
            assertEquals(args, " -m 12:34:56:78:90:AB -4 10.1.10.2 -h vm1 -6 2001:db8:0:0:0:ff00:42:8329 -u 00:03:00:01:12:34:56:78:90:AB");
            break;
        default:
            fail();
        }
    }

    @Test
    public void testCreateIpAliasCommand() {
        final Answer answer = _resource.executeRequest(generateCreateIpAliasCommand());
        assertTrue(answer.getResult());
    }

    protected CreateIpAliasCommand generateCreateIpAliasCommand() {
        final List<IpAliasTO> aliases = new ArrayList<>();
        aliases.add(new IpAliasTO("169.254.3.10", "255.255.255.0", "1"));
        aliases.add(new IpAliasTO("169.254.3.11", "255.255.255.0", "2"));
        aliases.add(new IpAliasTO("169.254.3.12", "255.255.255.0", "3"));
        final CreateIpAliasCommand cmd = new CreateIpAliasCommand("169.254.3.10", aliases);
        cmd.setAccessDetail(NetworkElementCommand.ROUTER_NAME, ROUTERNAME);

        return cmd;
    }

    @Test
    public void testDeleteIpAliasCommand() {
        final Answer answer = _resource.executeRequest(generateDeleteIpAliasCommand());
        assertTrue(answer.getResult());
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

    @Test
    public void testDnsMasqConfigCommand() {
        final Answer answer = _resource.executeRequest(generateDnsMasqConfigCommand());
        assertTrue(answer.getResult());
    }

    protected DnsMasqConfigCommand generateDnsMasqConfigCommand() {
        final List<DhcpTO> dhcps = new ArrayList<>();
        dhcps.add(new DhcpTO("10.1.20.2", "10.1.20.1", "255.255.255.0", "10.1.20.5"));
        dhcps.add(new DhcpTO("10.1.21.2", "10.1.21.1", "255.255.255.0", "10.1.21.5"));
        final DnsMasqConfigCommand cmd = new DnsMasqConfigCommand(dhcps);
        cmd.setAccessDetail(NetworkElementCommand.ROUTER_NAME, ROUTERNAME);
        return cmd;
    }

    private void verifyArgs(final DnsMasqConfigCommand cmd, final String script, final String args) {
        assertEquals(script, VRScripts.DNSMASQ_CONFIG);
        assertEquals(args, "10.1.20.2:10.1.20.1:255.255.255.0:10.1.20.5-10.1.21.2:10.1.21.1:255.255.255.0:10.1.21.5-");
    }

    @Test
    public void testLoadBalancerConfigCommand() {
        _count = 0;
        _file = "";

        Answer answer = _resource.executeRequest(generateLoadBalancerConfigCommand1());
        assertTrue(answer.getResult());

        answer = _resource.executeRequest(generateLoadBalancerConfigCommand2());
        assertTrue(answer.getResult());
    }

    protected LoadBalancerConfigCommand generateLoadBalancerConfigCommand1() {
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

    protected LoadBalancerConfigCommand generateLoadBalancerConfigCommand2() {
        final List<LoadBalancerTO> lbs = new ArrayList<>();
        final List<LbDestination> dests = new ArrayList<>();
        dests.add(new LbDestination(80, 8080, "10.1.10.2", false));
        dests.add(new LbDestination(80, 8080, "10.1.10.2", true));
        lbs.add(new LoadBalancerTO(UUID.randomUUID().toString(), "64.10.1.10", 80, "tcp", "algo", false, false, false, dests));
        final LoadBalancerTO[] arrayLbs = new LoadBalancerTO[lbs.size()];
        lbs.toArray(arrayLbs);
        final NicTO nic = new NicTO();
        nic.setIp("10.1.10.2");
        final LoadBalancerConfigCommand cmd = new LoadBalancerConfigCommand(arrayLbs, "64.10.2.10", "10.1.10.2", "192.168.1.2", nic, Long.valueOf(1), "1000", false);
        cmd.setAccessDetail(NetworkElementCommand.ROUTER_IP, "10.1.10.2");
        cmd.setAccessDetail(NetworkElementCommand.ROUTER_NAME, ROUTERNAME);
        return cmd;
    }

    protected void verifyFile(final LoadBalancerConfigCommand cmd, final String path, final String filename, final String content) {
        _count ++;
        switch (_count) {
        case 1:
        case 3:
            _file = path + filename;
            assertEquals(path, "/etc/haproxy/");
            assertTrue(filename.startsWith("haproxy.cfg.new"));
            assertEquals(content, "global\n" +
                    "\tlog 127.0.0.1:3914   local0 warning\n" +
                    "\tmaxconn 1000\n" +
                    "\tmaxpipes 250\n" +
                    "\tchroot /var/lib/haproxy\n" +
                    "\tuser haproxy\n" +
                    "\tgroup haproxy\n" +
                    "\tdaemon\n" +
                    "\t \n" +
                    "defaults\n" +
                    "\tlog     global\n" +
                    "\tmode    tcp\n" +
                    "\toption  dontlognull\n" +
                    "\tretries 3\n" +
                    "\toption redispatch\n" +
                    "\toption forwardfor\n" +
                    "\toption forceclose\n" +
                    "\ttimeout connect    5000\n" +
                    "\ttimeout client     50000\n" +
                    "\ttimeout server     50000\n" +
                    "\n" +
                    "listen stats_on_guest\n" +
                    "\tbind 10.1.10.2:8081\n" +
                    "\tmode http\n" +
                    "\toption httpclose\n" +
                    "\tstats enable\n" +
                    "\tstats uri     /admin?stats\n" +
                    "\tstats realm   Haproxy\\ Statistics\n" +
                    "\tstats auth    admin1:AdMiN123\n" +
                    "\n" +
                    "\t \n" +
                    "listen 64_10_1_10-80\n" +
                    "\tbind 64.10.1.10:80\n" +
                    "\tbalance algo\n" +
                    "\tserver 64_10_1_10-80_0 10.1.10.2:80 check\n" +
                    "\tmode http\n" +
                    "\toption httpclose\n" +
                    "\t \n" +
                    "\t \n");
            break;
        default:
            fail();
        }
    }

    private void verifyArgs(final LoadBalancerConfigCommand cmd, final String script, final String args) {
        _count ++;
        switch (_count) {
        case 2:
            assertEquals(script, VRScripts.LB);
            assertEquals(args, " -i 10.1.10.2 -f " + _file + " -a 64.10.1.10:80:, -s 10.1.10.2:8081:0/0:,,");
            break;
        default:
            fail();
        }
    }

    @Test
    @Ignore("Ignore this test while we are experimenting with the commands.")
    public void testAggregationCommands() {
        final List<NetworkElementCommand> cmds = new LinkedList<>();
        final AggregationControlCommand startCmd = new AggregationControlCommand(Action.Start, ROUTERNAME, ROUTERIP, ROUTERGUESTIP);
        cmds.add(startCmd);
        cmds.add(generateIpAssocCommand());
        cmds.add(generateIpAssocVpcCommand());

        cmds.add(generateSetFirewallRulesCommand());

        cmds.add(generateSetPortForwardingRulesCommand());
        cmds.add(generateSetPortForwardingRulesVpcCommand());

        cmds.add(generateCreateIpAliasCommand());
        cmds.add(generateDeleteIpAliasCommand());
        cmds.add(generateDnsMasqConfigCommand());

        cmds.add(generateRemoteAccessVpnCfgCommand1());
        cmds.add(generateRemoteAccessVpnCfgCommand2());
        cmds.add(generateRemoteAccessVpnCfgCommand3());

        //cmds.add(generateLoadBalancerConfigCommand1());
        //cmds.add(generateLoadBalancerConfigCommand2());

        cmds.add(generateSetPortForwardingRulesCommand());
        cmds.add(generateSetPortForwardingRulesVpcCommand());

        cmds.add(generateDhcpEntryCommand1());
        cmds.add(generateDhcpEntryCommand2());
        cmds.add(generateDhcpEntryCommand3());

        cmds.add(generateSavePasswordCommand());
        cmds.add(generateVmDataCommand());

        final AggregationControlCommand finishCmd = new AggregationControlCommand(Action.Finish, ROUTERNAME, ROUTERIP, ROUTERGUESTIP);
        cmds.add(finishCmd);

        for (final NetworkElementCommand cmd : cmds) {
            final Answer answer = _resource.executeRequest(cmd);
            assertTrue(answer.getResult());
        }
    }

    private void verifyArgs(final AggregationControlCommand cmd, final String script, final String args) {
        assertEquals(script, VRScripts.VR_CFG);
        assertTrue(args.startsWith("-c /var/cache/cloud/VR-"));
        assertTrue(args.endsWith(".cfg"));
    }
}
