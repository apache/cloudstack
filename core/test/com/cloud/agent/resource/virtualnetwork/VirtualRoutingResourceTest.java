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
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import javax.naming.ConfigurationException;

import org.junit.Before;
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
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(loader = AnnotationConfigContextLoader.class)
public class VirtualRoutingResourceTest implements VirtualRouterDeployer {
    VirtualRoutingResource _resource;
    NetworkElementCommand _currentCmd;
    int _count;
    String _file;

    String ROUTERIP = "169.254.3.4";
    String ROUTERGUESTIP = "10.200.1.1";
    String ROUTERNAME = "r-4-VM";

    @Override
    public ExecutionResult executeInVR(String routerIp, String script, String args) {
        return executeInVR(routerIp, script, args, 60);
    }

    @Override
    public ExecutionResult executeInVR(String routerIp, String script, String args, int timeout) {
        assertEquals(routerIp, ROUTERIP);
        verifyCommand(_currentCmd, script, args);
        return new ExecutionResult(true, null);
    }

    @Override
    public ExecutionResult createFileInVR(String routerIp, String path, String filename, String content) {
        assertEquals(routerIp, ROUTERIP);
        verifyFile(_currentCmd, path, filename, content);
        return new ExecutionResult(true, null);
    }

    @Override
    public ExecutionResult prepareCommand(NetworkElementCommand cmd) {
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
    public ExecutionResult cleanupCommand(NetworkElementCommand cmd) {
        return new ExecutionResult(true, null);
    }

    @Before
    public void setup() {
        _resource = new VirtualRoutingResource(this);
        try {
            _resource.configure("VRResource", new HashMap<String, Object>());
        } catch (ConfigurationException e) {
            e.printStackTrace();
        }
    }

    private void verifyFile(NetworkElementCommand cmd, String path, String filename, String content) {
        if (cmd instanceof AggregationControlCommand) {
            verifyFile((AggregationControlCommand)cmd, path, filename, content);
        } else if (cmd instanceof LoadBalancerConfigCommand) {
            verifyFile((LoadBalancerConfigCommand)cmd, path, filename, content);
        }
    }

    protected void verifyCommand(NetworkElementCommand cmd, String script, String args) {
        if (cmd instanceof SetPortForwardingRulesVpcCommand) {
            verifyArgs((SetPortForwardingRulesVpcCommand) cmd, script, args);
        } else if (cmd instanceof SetPortForwardingRulesCommand) {
            verifyArgs((SetPortForwardingRulesCommand) cmd, script, args);
        } else if (cmd instanceof SetStaticRouteCommand) {
            verifyArgs((SetStaticRouteCommand) cmd, script, args);
        } else if (cmd instanceof SetStaticNatRulesCommand) {
            verifyArgs((SetStaticNatRulesCommand) cmd, script, args);
        } else if (cmd instanceof LoadBalancerConfigCommand) {
            verifyArgs((LoadBalancerConfigCommand) cmd, script, args);
        } else if (cmd instanceof SavePasswordCommand) {
            verifyArgs((SavePasswordCommand)cmd, script, args);
        } else if (cmd instanceof DhcpEntryCommand) {
            verifyArgs((DhcpEntryCommand)cmd, script, args);
        } else if (cmd instanceof CreateIpAliasCommand) {
            verifyArgs((CreateIpAliasCommand)cmd, script, args);
        } else if (cmd instanceof DnsMasqConfigCommand) {
            verifyArgs((DnsMasqConfigCommand)cmd, script, args);
        } else if (cmd instanceof DeleteIpAliasCommand) {
            verifyArgs((DeleteIpAliasCommand)cmd, script, args);
        } else if (cmd instanceof VmDataCommand) {
            verifyArgs((VmDataCommand)cmd, script, args);
        } else if (cmd instanceof SetFirewallRulesCommand) {
            verifyArgs((SetFirewallRulesCommand)cmd, script, args);
        } else if (cmd instanceof BumpUpPriorityCommand) {
            verifyArgs((BumpUpPriorityCommand)cmd, script, args);
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

    private void verifyArgs(VpnUsersCfgCommand cmd, String script, String args) {
        //To change body of created methods use File | Settings | File Templates.
    }

    private void verifyArgs(SetStaticRouteCommand cmd, String script, String args) {
        //To change body of created methods use File | Settings | File Templates.
    }

    private void verifyArgs(SetStaticNatRulesCommand cmd, String script, String args) {
        //To change body of created methods use File | Settings | File Templates.
    }

    @Test
    public void testBumpUpCommand() {
        BumpUpPriorityCommand cmd = new BumpUpPriorityCommand();
        Answer answer = _resource.executeRequest(cmd);
        assertTrue(answer.getResult());
    }

    private void verifyArgs(BumpUpPriorityCommand cmd, String script, String args) {
        assertEquals(script, VRScripts.RVR_BUMPUP_PRI);
        assertEquals(args, null);
    }

    @Test
    public void testSetPortForwardingRulesVpcCommand() {
        SetPortForwardingRulesVpcCommand cmd = generateSetPortForwardingRulesVpcCommand();

        // Reset rule check count
        _count = 0;

        Answer answer = _resource.executeRequest(cmd);
        assertTrue(answer instanceof GroupAnswer);
        assertEquals(((GroupAnswer) answer).getResults().length, 2);
        assertTrue(answer.getResult());
    }

    protected SetPortForwardingRulesVpcCommand generateSetPortForwardingRulesVpcCommand() {
        List<PortForwardingRuleTO> pfRules = new ArrayList<>();
        pfRules.add(new PortForwardingRuleTO(1, "64.1.1.10", 22, 80, "10.10.1.10", 22, 80, "TCP", false, false));
        pfRules.add(new PortForwardingRuleTO(2, "64.1.1.11", 8080, 8080, "10.10.1.11", 8080, 8080, "UDP", true, false));
        SetPortForwardingRulesVpcCommand cmd = new SetPortForwardingRulesVpcCommand(pfRules);
        cmd.setAccessDetail(NetworkElementCommand.ROUTER_NAME, ROUTERNAME);
        assertEquals(cmd.getAnswersCount(), 2);
        return cmd;
    }

    private void verifyArgs(SetPortForwardingRulesVpcCommand cmd, String script, String args) {
        assertTrue(script.equals(VRScripts.VPC_PORTFORWARDING));
        _count ++;
        switch (_count) {
        case 1:
            assertEquals(args, "-A -P tcp -l 64.1.1.10 -p 22:80 -r 10.10.1.10 -d 22-80");
            break;
        case 2:
            assertEquals(args, "-D -P udp -l 64.1.1.11 -p 8080:8080 -r 10.10.1.11 -d 8080-8080");
            break;
        default:
            fail("Failed to recongize the match!");
        }
    }

    @Test
    public void testSetPortForwardingRulesCommand() {
        SetPortForwardingRulesCommand cmd = generateSetPortForwardingRulesCommand();
        // Reset rule check count
        _count = 0;

        Answer answer = _resource.executeRequest(cmd);
        assertTrue(answer instanceof GroupAnswer);
        assertEquals(((GroupAnswer) answer).getResults().length, 2);
        assertTrue(answer.getResult());
    }

    protected SetPortForwardingRulesCommand generateSetPortForwardingRulesCommand() {
        List<PortForwardingRuleTO> pfRules = new ArrayList<>();
        pfRules.add(new PortForwardingRuleTO(1, "64.1.1.10", 22, 80, "10.10.1.10", 22, 80, "TCP", false, false));
        pfRules.add(new PortForwardingRuleTO(2, "64.1.1.11", 8080, 8080, "10.10.1.11", 8080, 8080, "UDP", true, false));
        SetPortForwardingRulesCommand cmd = new SetPortForwardingRulesCommand(pfRules);
        cmd.setAccessDetail(NetworkElementCommand.ROUTER_NAME, ROUTERNAME);
        assertEquals(cmd.getAnswersCount(), 2);
        return cmd;
    }

    private void verifyArgs(SetPortForwardingRulesCommand cmd, String script, String args) {
        assertTrue(script.equals(VRScripts.FIREWALL_NAT));
        _count ++;
        switch (_count) {
        case 1:
            assertEquals(args, "-A -P tcp -l 64.1.1.10 -p 22:80 -r 10.10.1.10 -d 22:80");
            break;
        case 2:
            assertEquals(args, "-D -P udp -l 64.1.1.11 -p 8080:8080 -r 10.10.1.11 -d 8080:8080");
            break;
        default:
            fail("Failed to recongize the match!");
        }
    }

    @Test
    public void testIpAssocCommand() {
        IpAssocCommand cmd = generateIpAssocCommand();
        _count = 0;

        Answer answer = _resource.executeRequest(cmd);
        assertTrue(answer instanceof GroupAnswer);
        assertEquals(((GroupAnswer) answer).getResults().length, 3);
        assertTrue(answer.getResult());

    }

    private ExecutionResult prepareNetworkElementCommand(IpAssocCommand cmd) {
        IpAddressTO[] ips = cmd.getIpAddresses();
        for (IpAddressTO ip : ips) {
            ip.setNicDevId(2);
        }
        return new ExecutionResult(true, null);
    }

    protected IpAssocCommand generateIpAssocCommand() {
        List<IpAddressTO> ips = new ArrayList<>();
        ips.add(new IpAddressTO(1, "64.1.1.10", true, true, true, "vlan://64", "64.1.1.1", "255.255.255.0", "01:23:45:67:89:AB", 1000, false));
        ips.add(new IpAddressTO(2, "64.1.1.11", false, false, false, "vlan://64", "64.1.1.1", "255.255.255.0", "01:23:45:67:89:AB", 1000, false));
        ips.add(new IpAddressTO(3, "65.1.1.11", true, false, false, "vlan://65", "65.1.1.1", "255.255.255.0", "11:23:45:67:89:AB", 1000, false));
        IpAddressTO[] ipArray = ips.toArray(new IpAddressTO[ips.size()]);
        IpAssocCommand cmd = new IpAssocCommand(ipArray);
        cmd.setAccessDetail(NetworkElementCommand.ROUTER_NAME, ROUTERNAME);
        assertEquals(cmd.getAnswersCount(), 3);

        return cmd;
    }

    @Test
    public void testIpAssocVpcCommand() {
        IpAssocVpcCommand cmd = generateIpAssocVpcCommand();
        _count = 0;

        Answer answer = _resource.executeRequest(cmd);
        assertTrue(answer instanceof GroupAnswer);
        assertEquals(5, ((GroupAnswer)answer).getResults().length);
        assertTrue(answer.getResult());

    }

    private ExecutionResult prepareNetworkElementCommand(IpAssocVpcCommand cmd) {
        IpAddressTO[] ips = cmd.getIpAddresses();
        for (IpAddressTO ip : ips) {
            ip.setNicDevId(2);
        }
        return new ExecutionResult(true, null);
    }

    protected IpAssocVpcCommand generateIpAssocVpcCommand() {
        List<IpAddressTO> ips = new ArrayList<IpAddressTO>();
        ips.add(new IpAddressTO(1, "64.1.1.10", true, true, true, "vlan://64", "64.1.1.1", "255.255.255.0", "01:23:45:67:89:AB", 1000, false));
        ips.add(new IpAddressTO(2, "64.1.1.11", false, false, true, "vlan://64", "64.1.1.1", "255.255.255.0", "01:23:45:67:89:AB", 1000, false));
        ips.add(new IpAddressTO(3, "65.1.1.11", true, false, false, "vlan://65", "65.1.1.1", "255.255.255.0", "11:23:45:67:89:AB", 1000, false));
        IpAddressTO[] ipArray = ips.toArray(new IpAddressTO[ips.size()]);
        IpAssocVpcCommand cmd = new IpAssocVpcCommand(ipArray);
        cmd.setAccessDetail(NetworkElementCommand.ROUTER_NAME, ROUTERNAME);
        assertEquals(6, cmd.getAnswersCount()); // AnswersCount is clearly wrong as it doesn't know enough to tell

        return cmd;
    }

    private void verifyArgs(IpAssocCommand cmd, String script, String args) {
        if (cmd instanceof IpAssocVpcCommand) {
            _count ++;
            switch (_count) {
            case 1:
                assertEquals(script, VRScripts.VPC_IPASSOC);
                assertEquals(args, " -A  -l 64.1.1.10 -c eth2 -g 64.1.1.1 -m 24 -n 64.1.1.0");
                break;
            case 2:
                assertEquals(script, VRScripts.VPC_PRIVATEGW);
                assertEquals(args, " -A  -l 64.1.1.10 -c eth2");
                break;
            case 3:
                assertEquals(script, VRScripts.VPC_IPASSOC);
                assertEquals(args, " -D  -l 64.1.1.11 -c eth2 -g 64.1.1.1 -m 24 -n 64.1.1.0");
                break;
            case 4:
                assertEquals(script, VRScripts.VPC_PRIVATEGW);
                assertEquals(args, " -D  -l 64.1.1.11 -c eth2");
                break;
            case 5:
                assertEquals(script, VRScripts.VPC_IPASSOC);
                assertEquals(args, " -A  -l 65.1.1.11 -c eth2 -g 65.1.1.1 -m 24 -n 65.1.1.0");
                break;
            default:
                fail("Failed to recongize the match!");
            }
        } else {
            assertEquals(script, VRScripts.IPASSOC);
            _count ++;
            switch (_count) {
            case 1:
                assertEquals(args, "-A -s -f -l 64.1.1.10/24 -c eth2 -g 64.1.1.1");
                break;
            case 2:
                assertEquals(args, "-D -l 64.1.1.11/24 -c eth2 -g 64.1.1.1");
                break;
            case 3:
                assertEquals(args, "-A -l 65.1.1.11/24 -c eth2 -g 65.1.1.1");
                break;
            default:
                fail("Failed to recongize the match!");
            }
        }
    }

    @Test
    public void testSourceNatCommand() {
        SetSourceNatCommand cmd = generateSetSourceNatCommand();
        Answer answer = _resource.executeRequest(cmd);
        assertTrue(answer.getResult());
    }

    private ExecutionResult prepareNetworkElementCommand(SetSourceNatCommand cmd) {
        IpAddressTO ip = cmd.getIpAddress();
        ip.setNicDevId(1);
        return new ExecutionResult(true, null);
    }

    protected SetSourceNatCommand generateSetSourceNatCommand() {
        IpAddressTO ip = new IpAddressTO(1, "64.1.1.10", true, true, true, "vlan://64", "64.1.1.1", "255.255.255.0", "01:23:45:67:89:AB", 1000, false);
        SetSourceNatCommand cmd = new SetSourceNatCommand(ip, true);
        cmd.setAccessDetail(NetworkElementCommand.ROUTER_NAME, ROUTERNAME);
        return cmd;
    }

    private void verifyArgs(SetSourceNatCommand cmd, String script, String args) {
        assertEquals(script, VRScripts.VPC_SOURCE_NAT);
        assertEquals(args, "-A -l 64.1.1.10 -c eth1");
    }

    @Test
    public void testNetworkACLCommand() {
        SetNetworkACLCommand cmd = generateSetNetworkACLCommand();
        _count = 0;

        Answer answer = _resource.executeRequest(cmd);
        assertTrue(answer.getResult());

        cmd.setAccessDetail(NetworkElementCommand.VPC_PRIVATE_GATEWAY, String.valueOf(VpcGateway.Type.Private));
        answer = _resource.executeRequest(cmd);
        assertTrue(answer.getResult());
    }

    protected SetNetworkACLCommand generateSetNetworkACLCommand() {
        List<NetworkACLTO> acls = new ArrayList<>();
        List<String> cidrs = new ArrayList<>();
        cidrs.add("192.168.0.1/24");
        cidrs.add("192.168.0.2/24");
        acls.add(new NetworkACLTO(1, "64", "TCP", 20, 80, false, false, cidrs, 0, 0, TrafficType.Ingress, true, 1));
        acls.add(new NetworkACLTO(2, "64", "ICMP", 0, 0, false, false, cidrs, -1, -1, TrafficType.Ingress, false, 2));
        acls.add(new NetworkACLTO(3, "65", "ALL", 0, 0, false, false, cidrs, -1, -1, TrafficType.Egress, true, 3));
        NicTO nic = new NicTO();
        nic.setMac("01:23:45:67:89:AB");
        nic.setIp("192.168.1.1");
        nic.setNetmask("255.255.255.0");
        SetNetworkACLCommand cmd = new SetNetworkACLCommand(acls, nic);
        cmd.setAccessDetail(NetworkElementCommand.ROUTER_NAME, ROUTERNAME);

        return cmd;
    }

    private void verifyArgs(SetNetworkACLCommand cmd, String script, String args) {
        _count ++;
        switch (_count) {
        case 1:
            assertEquals(script, VRScripts.VPC_ACL);
            assertEquals(args, " -d eth3 -M 01:23:45:67:89:AB -i 192.168.1.1 -m 24 -a Egress:ALL:0:0:192.168.0.1/24-192.168.0.2/24:ACCEPT:," +
                    "Ingress:ICMP:0:0:192.168.0.1/24-192.168.0.2/24:DROP:,Ingress:TCP:20:80:192.168.0.1/24-192.168.0.2/24:ACCEPT:,");
            break;
        case 2:
            assertEquals(script, VRScripts.VPC_PRIVATEGW_ACL);
            assertEquals(args, " -d eth3 -M 01:23:45:67:89:AB -a Egress:ALL:0:0:192.168.0.1/24-192.168.0.2/24:ACCEPT:," +
                    "Ingress:ICMP:0:0:192.168.0.1/24-192.168.0.2/24:DROP:,Ingress:TCP:20:80:192.168.0.1/24-192.168.0.2/24:ACCEPT:,");
            break;
        default:
            fail();
        }
    }

    private ExecutionResult prepareNetworkElementCommand(SetNetworkACLCommand cmd) {
        NicTO nic = cmd.getNic();
        nic.setDeviceId(3);
        return new ExecutionResult(true, null);
    }

    @Test
    public void testSetupGuestNetworkCommand() {
        SetupGuestNetworkCommand cmd = generateSetupGuestNetworkCommand();
        Answer answer = _resource.executeRequest(cmd);
        assertTrue(answer.getResult());
    }

    private ExecutionResult prepareNetworkElementCommand(SetupGuestNetworkCommand cmd) {
        NicTO nic = cmd.getNic();
        nic.setDeviceId(4);
        return new ExecutionResult(true, null);
    }

    protected SetupGuestNetworkCommand generateSetupGuestNetworkCommand() {
        NicTO nic = new NicTO();
        nic.setMac("01:23:45:67:89:AB");
        nic.setIp("10.1.1.1");
        nic.setNetmask("255.255.255.0");

        SetupGuestNetworkCommand cmd = new SetupGuestNetworkCommand("10.1.1.10-10.1.1.20", "cloud.test", false, 0, "8.8.8.8", "8.8.4.4", true, nic);
        cmd.setAccessDetail(NetworkElementCommand.ROUTER_GUEST_IP, "10.1.1.2");
        cmd.setAccessDetail(NetworkElementCommand.GUEST_NETWORK_GATEWAY, "10.1.1.1");
        cmd.setAccessDetail(NetworkElementCommand.ROUTER_NAME, ROUTERNAME);

        return cmd;
    }

    private void verifyArgs(SetupGuestNetworkCommand cmd, String script, String args) {
        assertEquals(script, VRScripts.VPC_GUEST_NETWORK);
        assertEquals(args, " -C -M 01:23:45:67:89:AB -d eth4 -i 10.1.1.2 -g 10.1.1.1 -m 24 -n 10.1.1.0 -s 8.8.8.8,8.8.4.4 -e cloud.test");
    }

    @Test
    public void testSetMonitorServiceCommand() {
        SetMonitorServiceCommand cmd = generateSetMonitorServiceCommand();
        Answer answer = _resource.executeRequest(cmd);
        assertTrue(answer.getResult());
    }

    protected SetMonitorServiceCommand generateSetMonitorServiceCommand() {
        List<MonitorServiceTO> services = new ArrayList<>();
        services.add(new MonitorServiceTO("service", "process", "name", "path", "file", true));
        services.add(new MonitorServiceTO("service_2", "process_2", "name_2", "path_2", "file_2", false));

        SetMonitorServiceCommand cmd = new SetMonitorServiceCommand(services);
        cmd.setAccessDetail(NetworkElementCommand.ROUTER_NAME, ROUTERNAME);

        return cmd;
    }

    private void verifyArgs(SetMonitorServiceCommand cmd, String script, String args) {
        assertEquals(script, VRScripts.MONITOR_SERVICE);
        assertEquals(args, " -c [service]:processname=process:servicename=name:pidfile=file:,[service_2]:processname=process_2:servicename=name_2:pidfile=file_2:,");
    }

    @Test
    public void testSite2SiteVpnCfgCommand() {
        _count = 0;

        Site2SiteVpnCfgCommand cmd = new Site2SiteVpnCfgCommand(true, "64.10.1.10", "64.10.1.1", "192.168.1.1/16", "124.10.1.10", "192.168.100.1/24", "3des-sha1,aes128-sha1;modp1536", "3des-sha1,aes128-md5", "psk", Long.valueOf(1800), Long.valueOf(1800), true, false);
        cmd.setAccessDetail(NetworkElementCommand.ROUTER_NAME, ROUTERNAME);
        Answer answer = _resource.executeRequest(cmd);
        assertTrue(answer.getResult());

        cmd = new Site2SiteVpnCfgCommand(true, "64.10.1.10", "64.10.1.1", "192.168.1.1/16", "124.10.1.10", "192.168.100.1/24", "3des-sha1,aes128-sha1;modp1536", "3des-sha1,aes128-md5", "psk", Long.valueOf(1800), Long.valueOf(1800), false, true);
        cmd.setAccessDetail(NetworkElementCommand.ROUTER_NAME, ROUTERNAME);
        answer = _resource.executeRequest(cmd);
        assertTrue(answer.getResult());

        cmd = new Site2SiteVpnCfgCommand(false, "64.10.1.10", "64.10.1.1", "192.168.1.1/16", "124.10.1.10", "192.168.100.1/24", "3des-sha1,aes128-sha1;modp1536", "3des-sha1,aes128-md5", "psk", Long.valueOf(1800), Long.valueOf(1800), false, true);
        cmd.setAccessDetail(NetworkElementCommand.ROUTER_NAME, ROUTERNAME);
        answer = _resource.executeRequest(cmd);
        assertTrue(answer.getResult());
    }

    private void verifyArgs(Site2SiteVpnCfgCommand cmd, String script, String args) {
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
        RemoteAccessVpnCfgCommand cmd = new RemoteAccessVpnCfgCommand(true, "124.10.10.10", "10.10.1.1", "10.10.1.10-10.10.1.20", "sharedkey", false);
        cmd.setAccessDetail(NetworkElementCommand.ROUTER_NAME, ROUTERNAME);
        cmd.setLocalCidr("10.1.1.1/24");
        return cmd;
    }

    protected RemoteAccessVpnCfgCommand generateRemoteAccessVpnCfgCommand2() {
        RemoteAccessVpnCfgCommand cmd = new RemoteAccessVpnCfgCommand(false, "124.10.10.10", "10.10.1.1", "10.10.1.10-10.10.1.20", "sharedkey", false);
        cmd.setAccessDetail(NetworkElementCommand.ROUTER_NAME, ROUTERNAME);
        cmd.setLocalCidr("10.1.1.1/24");
        return cmd;
    }

    protected RemoteAccessVpnCfgCommand generateRemoteAccessVpnCfgCommand3() {
        RemoteAccessVpnCfgCommand cmd = new RemoteAccessVpnCfgCommand(true, "124.10.10.10", "10.10.1.1", "10.10.1.10-10.10.1.20", "sharedkey", true);
        cmd.setAccessDetail(NetworkElementCommand.ROUTER_NAME, ROUTERNAME);
        cmd.setLocalCidr("10.1.1.1/24");
        return cmd;
    }

    private void verifyArgs(RemoteAccessVpnCfgCommand cmd, String script, String args) {
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

        Answer answer = _resource.executeRequest(generateSetFirewallRulesCommand());
        assertTrue(answer.getResult());

        //TODO Didn't test egress rule because not able to generate FirewallRuleVO object
    }

    protected SetFirewallRulesCommand generateSetFirewallRulesCommand() {
        List<FirewallRuleTO> rules = new ArrayList<>();
        List<String> sourceCidrs = new ArrayList<>();
        sourceCidrs.add("10.10.1.1/24");
        sourceCidrs.add("10.10.1.2/24");
        rules.add(new FirewallRuleTO(1, "64.10.10.10", "TCP", 22, 80, false, false, Purpose.Firewall, sourceCidrs, 0, 0));
        rules.add(new FirewallRuleTO(2, "64.10.10.10", "ICMP", 0, 0, false, false, Purpose.Firewall, sourceCidrs, -1, -1));
        rules.add(new FirewallRuleTO(3, "64.10.10.10", "ICMP", 0, 0, true, true, Purpose.Firewall, sourceCidrs, -1, -1));
        SetFirewallRulesCommand cmd = new SetFirewallRulesCommand(rules);
        cmd.setAccessDetail(NetworkElementCommand.ROUTER_NAME, ROUTERNAME);

        return cmd;
    }

    private void verifyArgs(SetFirewallRulesCommand cmd, String script, String args) {
        assertEquals(script, VRScripts.FIREWALL_INGRESS);

        //Since the arguments are generated with a Set
        //one can not make a bet on the order
        assertTrue(args.startsWith(" -F -a "));
        assertTrue(args.contains("64.10.10.10:ICMP:0:0:10.10.1.1/24-10.10.1.2/24:"));
        assertTrue(args.contains("64.10.10.10:reverted:0:0:0:"));
        assertTrue(args.contains("64.10.10.10:TCP:22:80:10.10.1.1/24-10.10.1.2/24:"));
    }

    @Test
    public void testVmDataCommand() {
        Answer answer = _resource.executeRequest(generateVmDataCommand());
        assertTrue(answer.getResult());
    }

    protected VmDataCommand generateVmDataCommand() {
        VmDataCommand cmd = new VmDataCommand("10.1.10.4", "i-4-VM", true);
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

    private void verifyArgs(VmDataCommand cmd, String script, String args) {
        assertEquals(script, VRScripts.VMDATA);
        assertEquals(args, "-d eyIxMC4xLjEwLjQiOltbInVzZXJkYXRhIiwidXNlci1kYXRhIiwidXNlci1kYXRhIl0sWyJtZXRhZGF0YSIsInN" +
                "lcnZpY2Utb2ZmZXJpbmciLCJzZXJ2aWNlT2ZmZXJpbmciXSxbIm1ldGFkYXRhIiwiYXZhaWxhYmlsaXR5LXpvbmUiLCJ6b25lTmFt" +
                "ZSJdLFsibWV0YWRhdGEiLCJsb2NhbC1pcHY0IiwiMTAuMS4xMC40Il0sWyJtZXRhZGF0YSIsImxvY2FsLWhvc3RuYW1lIiwidGVzd" +
                "C12bSJdLFsibWV0YWRhdGEiLCJwdWJsaWMtaXB2NCIsIjExMC4xLjEwLjQiXSxbIm1ldGFkYXRhIiwicHVibGljLWhvc3RuYW1lIi" +
                "wiaG9zdG5hbWUiXSxbIm1ldGFkYXRhIiwiaW5zdGFuY2UtaWQiLCJpLTQtVk0iXSxbIm1ldGFkYXRhIiwidm0taWQiLCI0Il0sWyJ" +
                "tZXRhZGF0YSIsInB1YmxpYy1rZXlzIiwicHVibGlja2V5Il0sWyJtZXRhZGF0YSIsImNsb3VkLWlkZW50aWZpZXIiLCJDbG91ZFN0YWNrLXt0ZXN0fSJdXX0=");
    }

    @Test
    public void testSavePasswordCommand() {
        Answer answer = _resource.executeRequest(generateSavePasswordCommand());
        assertTrue(answer.getResult());
    }

    protected SavePasswordCommand generateSavePasswordCommand() {
        SavePasswordCommand cmd = new SavePasswordCommand("123pass", "10.1.10.4", "i-4-VM", true);
        cmd.setAccessDetail(NetworkElementCommand.ROUTER_NAME, ROUTERNAME);
        return cmd;
    }

    private void verifyArgs(SavePasswordCommand cmd, String script, String args) {
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
        DhcpEntryCommand cmd = new DhcpEntryCommand("12:34:56:78:90:AB", "10.1.10.2", "vm1", null, true);
        cmd.setAccessDetail(NetworkElementCommand.ROUTER_NAME, ROUTERNAME);
        return cmd;
    }

    protected DhcpEntryCommand generateDhcpEntryCommand2() {
        DhcpEntryCommand cmd = new DhcpEntryCommand("12:34:56:78:90:AB", null, "vm1", "2001:db8:0:0:0:ff00:42:8329", true);
        cmd.setAccessDetail(NetworkElementCommand.ROUTER_NAME, ROUTERNAME);
        cmd.setDuid(NetUtils.getDuidLL(cmd.getVmMac()));
        return cmd;
    }

    protected DhcpEntryCommand generateDhcpEntryCommand3() {
        DhcpEntryCommand cmd = new DhcpEntryCommand("12:34:56:78:90:AB", "10.1.10.2", "vm1", "2001:db8:0:0:0:ff00:42:8329", true);
        cmd.setAccessDetail(NetworkElementCommand.ROUTER_NAME, ROUTERNAME);
        cmd.setDuid(NetUtils.getDuidLL(cmd.getVmMac()));
        return cmd;
    }

    private void verifyArgs(DhcpEntryCommand cmd, String script, String args) {
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
        Answer answer = _resource.executeRequest(generateCreateIpAliasCommand());
        assertTrue(answer.getResult());
    }

    protected CreateIpAliasCommand generateCreateIpAliasCommand() {
        List<IpAliasTO> aliases = new ArrayList<>();
        aliases.add(new IpAliasTO("169.254.3.10", "255.255.255.0", "1"));
        aliases.add(new IpAliasTO("169.254.3.11", "255.255.255.0", "2"));
        aliases.add(new IpAliasTO("169.254.3.12", "255.255.255.0", "3"));
        CreateIpAliasCommand cmd = new CreateIpAliasCommand("169.254.3.10", aliases);
        cmd.setAccessDetail(NetworkElementCommand.ROUTER_NAME, ROUTERNAME);

        return cmd;
    }

    private void verifyArgs(CreateIpAliasCommand cmd, String script, String args) {
        assertEquals(script, VRScripts.IPALIAS_CREATE);
        assertEquals(args, "1:169.254.3.10:255.255.255.0-2:169.254.3.11:255.255.255.0-3:169.254.3.12:255.255.255.0-");
    }

    @Test
    public void testDeleteIpAliasCommand() {
        Answer answer = _resource.executeRequest(generateDeleteIpAliasCommand());
        assertTrue(answer.getResult());
    }

    protected DeleteIpAliasCommand generateDeleteIpAliasCommand() {
        List<IpAliasTO> aliases = new ArrayList<>();
        aliases.add(new IpAliasTO("169.254.3.10", "255.255.255.0", "1"));
        aliases.add(new IpAliasTO("169.254.3.11", "255.255.255.0", "2"));
        aliases.add(new IpAliasTO("169.254.3.12", "255.255.255.0", "3"));
        DeleteIpAliasCommand cmd = new DeleteIpAliasCommand("169.254.10.1", aliases, aliases);
        cmd.setAccessDetail(NetworkElementCommand.ROUTER_NAME, ROUTERNAME);
        return cmd;
    }

    private void verifyArgs(DeleteIpAliasCommand cmd, String script, String args) {
        assertEquals(script, VRScripts.IPALIAS_DELETE);
        assertEquals(args, "1:169.254.3.10:255.255.255.0-2:169.254.3.11:255.255.255.0-3:169.254.3.12:255.255.255.0-- 1:169.254.3.10:255.255.255.0-2:169.254.3.11:255.255.255.0-3:169.254.3.12:255.255.255.0-");
    }

    @Test
    public void testDnsMasqConfigCommand() {
        Answer answer = _resource.executeRequest(generateDnsMasqConfigCommand());
        assertTrue(answer.getResult());
    }

    protected DnsMasqConfigCommand generateDnsMasqConfigCommand() {
        List<DhcpTO> dhcps = new ArrayList<>();
        dhcps.add(new DhcpTO("10.1.20.2", "10.1.20.1", "255.255.255.0", "10.1.20.5"));
        dhcps.add(new DhcpTO("10.1.21.2", "10.1.21.1", "255.255.255.0", "10.1.21.5"));
        DnsMasqConfigCommand cmd = new DnsMasqConfigCommand(dhcps);
        cmd.setAccessDetail(NetworkElementCommand.ROUTER_NAME, ROUTERNAME);
        return cmd;
    }

    private void verifyArgs(DnsMasqConfigCommand cmd, String script, String args) {
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
        List<LoadBalancerTO> lbs = new ArrayList<>();
        List<LbDestination> dests = new ArrayList<>();
        dests.add(new LbDestination(80, 8080, "10.1.10.2", false));
        dests.add(new LbDestination(80, 8080, "10.1.10.2", true));
        lbs.add(new LoadBalancerTO(UUID.randomUUID().toString(), "64.10.1.10", 80, "tcp", "algo", false, false, false, dests));
        LoadBalancerTO[] arrayLbs = new LoadBalancerTO[lbs.size()];
        lbs.toArray(arrayLbs);
        NicTO nic = new NicTO();
        LoadBalancerConfigCommand cmd = new LoadBalancerConfigCommand(arrayLbs, "64.10.2.10", "10.1.10.2", "192.168.1.2", nic, null, "1000", false);
        cmd.setAccessDetail(NetworkElementCommand.ROUTER_IP, "10.1.10.2");
        cmd.setAccessDetail(NetworkElementCommand.ROUTER_NAME, ROUTERNAME);
        return cmd;
    }

    protected LoadBalancerConfigCommand generateLoadBalancerConfigCommand2() {
        List<LoadBalancerTO> lbs = new ArrayList<>();
        List<LbDestination> dests = new ArrayList<>();
        dests.add(new LbDestination(80, 8080, "10.1.10.2", false));
        dests.add(new LbDestination(80, 8080, "10.1.10.2", true));
        lbs.add(new LoadBalancerTO(UUID.randomUUID().toString(), "64.10.1.10", 80, "tcp", "algo", false, false, false, dests));
        LoadBalancerTO[] arrayLbs = new LoadBalancerTO[lbs.size()];
        lbs.toArray(arrayLbs);
        NicTO nic = new NicTO();
        nic.setIp("10.1.10.2");
        LoadBalancerConfigCommand cmd = new LoadBalancerConfigCommand(arrayLbs, "64.10.2.10", "10.1.10.2", "192.168.1.2", nic, Long.valueOf(1), "1000", false);
        cmd.setAccessDetail(NetworkElementCommand.ROUTER_IP, "10.1.10.2");
        cmd.setAccessDetail(NetworkElementCommand.ROUTER_NAME, ROUTERNAME);
        return cmd;
    }

    protected void verifyFile(LoadBalancerConfigCommand cmd, String path, String filename, String content) {
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
                    "listen stats_on_guest 10.1.10.2:8081\n" +
                    "\tmode http\n" +
                    "\toption httpclose\n" +
                    "\tstats enable\n" +
                    "\tstats uri     /admin?stats\n" +
                    "\tstats realm   Haproxy\\ Statistics\n" +
                    "\tstats auth    admin1:AdMiN123\n" +
                    "\n" +
                    "\t \n" +
                    "listen 64_10_1_10-80 64.10.1.10:80\n" +
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

    private void verifyArgs(LoadBalancerConfigCommand cmd, String script, String args) {
        _count ++;
        switch (_count) {
        case 2:
            assertEquals(script, VRScripts.LB);
            assertEquals(args, " -i 10.1.10.2 -f " + _file + " -a 64.10.1.10:80:, -s 10.1.10.2:8081:0/0:,,");
            break;
        case 4:
            assertEquals(script, VRScripts.VPC_LB);
            assertEquals(args, " -i 10.1.10.2 -f " + _file + " -a 64.10.1.10:80:, -s 10.1.10.2:8081:0/0:,,");
            break;
        default:
            fail();
        }
    }

    @Test
    public void testAggregationCommands() {
        List<NetworkElementCommand> cmds = new LinkedList<>();
        AggregationControlCommand startCmd = new AggregationControlCommand(Action.Start, ROUTERNAME, ROUTERIP, ROUTERGUESTIP);
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

        AggregationControlCommand finishCmd = new AggregationControlCommand(Action.Finish, ROUTERNAME, ROUTERIP, ROUTERGUESTIP);
        cmds.add(finishCmd);

        for (NetworkElementCommand cmd : cmds) {
            Answer answer = _resource.executeRequest(cmd);
            assertTrue(answer.getResult());
        }
    }

    private void verifyArgs(AggregationControlCommand cmd, String script, String args) {
        assertEquals(script, VRScripts.VR_CFG);
        assertTrue(args.startsWith("-c /var/cache/cloud/VR-"));
        assertTrue(args.endsWith(".cfg"));
    }

    protected void verifyFile(AggregationControlCommand cmd, String path, String filename, String content) {
        assertEquals(path, "/var/cache/cloud/");
        assertTrue(filename.startsWith("VR-"));
        assertTrue(filename.endsWith(".cfg"));
        Collection<String> filteredScripts = Collections2.transform(Collections2.filter (
                Arrays.asList(content.split("</?script>")), new Predicate<String>() {

                    @Override
                    public boolean apply(String str) {
                        return str.trim().startsWith("/opt/cloud");
                    }
                }), new Function<String, String>() {

                    @Override
                    public String apply(String str) {
                        return str.trim();
                    }
                });
        String[] scripts = filteredScripts.toArray(new String[filteredScripts
                .size()]);

        assertEquals(
                "/opt/cloud/bin/ipassoc.sh -A -s -f -l 64.1.1.10/24 -c eth2 -g 64.1.1.1",
                scripts[0]);

        assertEquals(
                "/opt/cloud/bin/ipassoc.sh -D -l 64.1.1.11/24 -c eth2 -g 64.1.1.1",
                scripts[1]);

        assertEquals(
                "/opt/cloud/bin/ipassoc.sh -A -l 65.1.1.11/24 -c eth2 -g 65.1.1.1",
                scripts[2]);
        assertEquals(
                "/opt/cloud/bin/vpc_ipassoc.sh  -A  -l 64.1.1.10 -c eth2 -g 64.1.1.1 -m 24 -n 64.1.1.0",
                scripts[3]);
        assertEquals(
                "/opt/cloud/bin/vpc_privateGateway.sh  -A  -l 64.1.1.10 -c eth2",
                scripts[4]);
        assertEquals(
                "/opt/cloud/bin/vpc_ipassoc.sh  -D  -l 64.1.1.11 -c eth2 -g 64.1.1.1 -m 24 -n 64.1.1.0",
                scripts[5]);
        assertEquals(
                "/opt/cloud/bin/vpc_privateGateway.sh  -D  -l 64.1.1.11 -c eth2",
                scripts[6]);
        assertEquals(
                "/opt/cloud/bin/vpc_ipassoc.sh  -A  -l 65.1.1.11 -c eth2 -g 65.1.1.1 -m 24 -n 65.1.1.0",
                scripts[7]);
        //the list generated by SetFirewallCmd is actually generated through a Set
        //therefore we can not bet on the order of the parameters
        assertTrue(
                scripts[8].matches("/opt/cloud/bin/firewall_ingress.sh  -F -a .*"));
        assertTrue(
                scripts[8].contains("64.10.10.10:ICMP:0:0:10.10.1.1/24-10.10.1.2/24:"));
        assertTrue(
                scripts[8].contains("64.10.10.10:TCP:22:80:10.10.1.1/24-10.10.1.2/24:"));
        assertTrue(
                scripts[8].contains("64.10.10.10:reverted:0:0:0:"));

        assertEquals(
                "/opt/cloud/bin/firewall_nat.sh -A -P tcp -l 64.1.1.10 -p 22:80 -r 10.10.1.10 -d 22:80",
                scripts[9]);
        assertEquals(
                "/opt/cloud/bin/firewall_nat.sh -D -P udp -l 64.1.1.11 -p 8080:8080 -r 10.10.1.11 -d 8080:8080",
                scripts[10]);
        assertEquals(
                "/opt/cloud/bin/vpc_portforwarding.sh -A -P tcp -l 64.1.1.10 -p 22:80 -r 10.10.1.10 -d 22-80",
                scripts[11]);
        assertEquals(
                "/opt/cloud/bin/vpc_portforwarding.sh -D -P udp -l 64.1.1.11 -p 8080:8080 -r 10.10.1.11 -d 8080-8080",
                scripts[12]);
        assertEquals(
                "/opt/cloud/bin/createIpAlias.sh 1:169.254.3.10:255.255.255.0-2:169.254.3.11:255.255.255.0-3:169.254.3.12:255.255.255.0-",
                scripts[13]);
        assertEquals(
                "/opt/cloud/bin/deleteIpAlias.sh 1:169.254.3.10:255.255.255.0-2:169.254.3.11:255.255.255.0-3:169.254.3.12:255.255.255.0-- 1:169.254.3.10:255.255.255.0-2:169.254.3.11:255.255.255.0-3:169.254.3.12:255.255.255.0-",
                scripts[14]);
        assertEquals(
                "/opt/cloud/bin/dnsmasq.sh 10.1.20.2:10.1.20.1:255.255.255.0:10.1.20.5-10.1.21.2:10.1.21.1:255.255.255.0:10.1.21.5-",
                scripts[15]);
        assertEquals(
                "/opt/cloud/bin/vpn_l2tp.sh -r 10.10.1.10-10.10.1.20 -p sharedkey -s 124.10.10.10 -l 10.10.1.1 -c  -C 10.1.1.1/24 -i eth2",
                scripts[16]);
        assertEquals(
                "/opt/cloud/bin/vpn_l2tp.sh -d  -s 124.10.10.10 -C 10.1.1.1/24 -i eth2",
                scripts[17]);
        assertEquals(
                "/opt/cloud/bin/vpn_l2tp.sh -r 10.10.1.10-10.10.1.20 -p sharedkey -s 124.10.10.10 -l 10.10.1.1 -c  -C 10.1.1.1/24 -i eth1",
                scripts[18]);
        assertEquals(
                "/opt/cloud/bin/firewall_nat.sh -A -P tcp -l 64.1.1.10 -p 22:80 -r 10.10.1.10 -d 22:80",
                scripts[19]);
        assertEquals(
                "/opt/cloud/bin/firewall_nat.sh -D -P udp -l 64.1.1.11 -p 8080:8080 -r 10.10.1.11 -d 8080:8080",
                scripts[20]);
        assertEquals(
                "/opt/cloud/bin/vpc_portforwarding.sh -A -P tcp -l 64.1.1.10 -p 22:80 -r 10.10.1.10 -d 22-80",
                scripts[21]);
        assertEquals(
                "/opt/cloud/bin/vpc_portforwarding.sh -D -P udp -l 64.1.1.11 -p 8080:8080 -r 10.10.1.11 -d 8080-8080",
                scripts[22]);
        assertEquals(
                "/opt/cloud/bin/edithosts.sh  -m 12:34:56:78:90:AB -4 10.1.10.2 -h vm1",
                scripts[23]);
        assertEquals(
                "/opt/cloud/bin/edithosts.sh  -m 12:34:56:78:90:AB -h vm1 -6 2001:db8:0:0:0:ff00:42:8329 -u 00:03:00:01:12:34:56:78:90:AB",
                scripts[24]);
        assertEquals(
                "/opt/cloud/bin/edithosts.sh  -m 12:34:56:78:90:AB -4 10.1.10.2 -h vm1 -6 2001:db8:0:0:0:ff00:42:8329 -u 00:03:00:01:12:34:56:78:90:AB",
                scripts[25]);
        assertEquals("/opt/cloud/bin/savepassword.sh -v 10.1.10.4 -p 123pass",
                scripts[26]);
        assertEquals(
                "/opt/cloud/bin/vmdata.py -d eyIxMC4xLjEwLjQiOltbInVzZXJkYXRhIiwidXNlci1kYXRhIiwidXNlci1kYXRhIl0sWyJtZXRhZGF0YSIsInNlcnZpY2Utb2ZmZXJpbmciLCJzZXJ2aWNlT2ZmZXJpbmciXSxbIm1ldGFkYXRhIiwiYXZhaWxhYmlsaXR5LXpvbmUiLCJ6b25lTmFtZSJdLFsibWV0YWRhdGEiLCJsb2NhbC1pcHY0IiwiMTAuMS4xMC40Il0sWyJtZXRhZGF0YSIsImxvY2FsLWhvc3RuYW1lIiwidGVzdC12bSJdLFsibWV0YWRhdGEiLCJwdWJsaWMtaXB2NCIsIjExMC4xLjEwLjQiXSxbIm1ldGFkYXRhIiwicHVibGljLWhvc3RuYW1lIiwiaG9zdG5hbWUiXSxbIm1ldGFkYXRhIiwiaW5zdGFuY2UtaWQiLCJpLTQtVk0iXSxbIm1ldGFkYXRhIiwidm0taWQiLCI0Il0sWyJtZXRhZGF0YSIsInB1YmxpYy1rZXlzIiwicHVibGlja2V5Il0sWyJtZXRhZGF0YSIsImNsb3VkLWlkZW50aWZpZXIiLCJDbG91ZFN0YWNrLXt0ZXN0fSJdXX0=",
                scripts[27]);
    }

}

