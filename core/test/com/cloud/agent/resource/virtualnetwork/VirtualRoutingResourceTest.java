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
package com.cloud.agent.resource.virtualnetwork;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.BumpUpPriorityCommand;
import com.cloud.agent.api.SetupGuestNetworkCommand;
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
import com.cloud.agent.resource.virtualnetwork.VirtualRoutingResource.VRScripts;
import com.cloud.network.lb.LoadBalancingRule.LbDestination;
import com.cloud.network.rules.FirewallRule.Purpose;
import com.cloud.network.vpc.NetworkACLItem.TrafficType;
import com.cloud.network.vpc.VpcGateway;
import com.cloud.utils.ExecutionResult;
import com.cloud.utils.net.NetUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(loader = AnnotationConfigContextLoader.class)
public class VirtualRoutingResourceTest implements VirtualRouterDeployer {
    VirtualRoutingResource _resource;
    NetworkElementCommand _currentCmd;
    int _count;

    String ROUTERIP = "10.2.3.4";

    @Override
    public ExecutionResult executeInVR(String routerIp, String script, String args) {
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
        List<PortForwardingRuleTO> pfRules = new ArrayList<>();
        pfRules.add(new PortForwardingRuleTO(1, "64.1.1.10", 22, 80, "10.10.1.10", 22, 80, "TCP", false, false));
        pfRules.add(new PortForwardingRuleTO(2, "64.1.1.11", 8080, 8080, "10.10.1.11", 8080, 8080, "UDP", true, false));
        SetPortForwardingRulesVpcCommand cmd = new SetPortForwardingRulesVpcCommand(pfRules);
        assertEquals(cmd.getAnswersCount(), 2);

        // Reset rule check count
        _count = 0;

        Answer answer = _resource.executeRequest(cmd);
        assertTrue(answer instanceof GroupAnswer);
        assertEquals(((GroupAnswer) answer).getResults().length, 2);
        assertTrue(answer.getResult());
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
        List<PortForwardingRuleTO> pfRules = new ArrayList<>();
        pfRules.add(new PortForwardingRuleTO(1, "64.1.1.10", 22, 80, "10.10.1.10", 22, 80, "TCP", false, false));
        pfRules.add(new PortForwardingRuleTO(2, "64.1.1.11", 8080, 8080, "10.10.1.11", 8080, 8080, "UDP", true, false));
        SetPortForwardingRulesCommand cmd = new SetPortForwardingRulesCommand(pfRules);
        assertEquals(cmd.getAnswersCount(), 2);

        // Reset rule check count
        _count = 0;

        Answer answer = _resource.executeRequest(cmd);
        assertTrue(answer instanceof GroupAnswer);
        assertEquals(((GroupAnswer) answer).getResults().length, 2);
        assertTrue(answer.getResult());
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
        List<IpAddressTO> ips = new ArrayList<IpAddressTO>();
        ips.add(new IpAddressTO(1, "64.1.1.10", true, true, true, "vlan://64", "64.1.1.1", "255.255.255.0", "01:23:45:67:89:AB", 1000, false));
        ips.add(new IpAddressTO(2, "64.1.1.11", false, false, false, "vlan://64", "64.1.1.1", "255.255.255.0", "01:23:45:67:89:AB", 1000, false));
        ips.add(new IpAddressTO(3, "65.1.1.11", true, false, false, "vlan://65", "65.1.1.1", "255.255.255.0", "11:23:45:67:89:AB", 1000, false));
        IpAddressTO[] ipArray = ips.toArray(new IpAddressTO[ips.size()]);
        IpAssocCommand cmd = new IpAssocCommand(ipArray);
        assertEquals(cmd.getAnswersCount(), 3);

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

    @Test
    public void testIpAssocVpcCommand() {
        List<IpAddressTO> ips = new ArrayList<IpAddressTO>();
        ips.add(new IpAddressTO(1, "64.1.1.10", true, true, true, "vlan://64", "64.1.1.1", "255.255.255.0", "01:23:45:67:89:AB", 1000, false));
        ips.add(new IpAddressTO(2, "64.1.1.11", false, false, true, "vlan://64", "64.1.1.1", "255.255.255.0", "01:23:45:67:89:AB", 1000, false));
        ips.add(new IpAddressTO(3, "65.1.1.11", true, false, false, "vlan://65", "65.1.1.1", "255.255.255.0", "11:23:45:67:89:AB", 1000, false));
        IpAddressTO[] ipArray = ips.toArray(new IpAddressTO[ips.size()]);
        IpAssocVpcCommand cmd = new IpAssocVpcCommand(ipArray);
        assertEquals(cmd.getAnswersCount(), 6);

        _count = 0;

        Answer answer = _resource.executeRequest(cmd);
        assertTrue(answer instanceof GroupAnswer);
        assertEquals(((GroupAnswer) answer).getResults().length, 6);
        assertTrue(answer.getResult());

    }

    private ExecutionResult prepareNetworkElementCommand(IpAssocVpcCommand cmd) {
        IpAddressTO[] ips = cmd.getIpAddresses();
        for (IpAddressTO ip : ips) {
            ip.setNicDevId(2);
        }
        return new ExecutionResult(true, null);
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
        IpAddressTO ip = new IpAddressTO(1, "64.1.1.10", true, true, true, "vlan://64", "64.1.1.1", "255.255.255.0", "01:23:45:67:89:AB", 1000, false);
        SetSourceNatCommand cmd = new SetSourceNatCommand(ip, true);
        Answer answer = _resource.executeRequest(cmd);
        assertTrue(answer.getResult());
    }

    private ExecutionResult prepareNetworkElementCommand(SetSourceNatCommand cmd) {
        IpAddressTO ip = cmd.getIpAddress();
        ip.setNicDevId(1);
        return new ExecutionResult(true, null);
    }

    private void verifyArgs(SetSourceNatCommand cmd, String script, String args) {
        assertEquals(script, VRScripts.VPC_SOURCE_NAT);
        assertEquals(args, "-A -l 64.1.1.10 -c eth1");
    }

    @Test
    public void testNetworkACLCommand() {
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

        _count = 0;

        SetNetworkACLCommand cmd = new SetNetworkACLCommand(acls, nic);
        Answer answer = _resource.executeRequest(cmd);
        assertTrue(answer.getResult());

        cmd.setAccessDetail(NetworkElementCommand.VPC_PRIVATE_GATEWAY, String.valueOf(VpcGateway.Type.Private));
        answer = _resource.executeRequest(cmd);
        assertTrue(answer.getResult());
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
        NicTO nic = new NicTO();
        nic.setMac("01:23:45:67:89:AB");
        nic.setIp("10.1.1.1");
        nic.setNetmask("255.255.255.0");

        SetupGuestNetworkCommand cmd = new SetupGuestNetworkCommand("10.1.1.10-10.1.1.20", "cloud.test", false, 0, "8.8.8.8", "8.8.4.4", true, nic);
        cmd.setAccessDetail(NetworkElementCommand.ROUTER_GUEST_IP, "10.1.1.2");
        cmd.setAccessDetail(NetworkElementCommand.GUEST_NETWORK_GATEWAY, "10.1.1.1");

        Answer answer = _resource.executeRequest(cmd);
        assertTrue(answer.getResult());

    }

    private ExecutionResult prepareNetworkElementCommand(SetupGuestNetworkCommand cmd) {
        NicTO nic = cmd.getNic();
        nic.setDeviceId(4);
        return new ExecutionResult(true, null);
    }

    private void verifyArgs(SetupGuestNetworkCommand cmd, String script, String args) {
        assertEquals(script, VRScripts.VPC_GUEST_NETWORK);
        assertEquals(args, " -C -M 01:23:45:67:89:AB -d eth4 -i 10.1.1.2 -g 10.1.1.1 -m 24 -n 10.1.1.0 -s 8.8.8.8,8.8.4.4 -e cloud.test");
    }

    @Test
    public void testSetMonitorServiceCommand() {
        List<MonitorServiceTO> services = new ArrayList<>();
        services.add(new MonitorServiceTO("service", "process", "name", "path", "file", true));
        services.add(new MonitorServiceTO("service_2", "process_2", "name_2", "path_2", "file_2", false));

        SetMonitorServiceCommand cmd = new SetMonitorServiceCommand(services);
        Answer answer = _resource.executeRequest(cmd);
        assertTrue(answer.getResult());
    }

    private void verifyArgs(SetMonitorServiceCommand cmd, String script, String args) {
        assertEquals(script, VRScripts.MONITOR_SERVICE);
        assertEquals(args, " -c [service]:processname=process:servicename=name:pidfile=file:,[service_2]:processname=process_2:servicename=name_2:pidfile=file_2:,");
    }

    @Test
    public void testSite2SiteVpnCfgCommand() {
        _count = 0;

        Site2SiteVpnCfgCommand cmd = new Site2SiteVpnCfgCommand(true, "64.10.1.10", "64.10.1.1", "192.168.1.1/16", "124.10.1.10", "192.168.100.1/24", "3des-sha1,aes128-sha1;modp1536", "3des-sha1,aes128-md5", "psk", Long.valueOf(1800), Long.valueOf(1800), true, false);
        Answer answer = _resource.executeRequest(cmd);
        assertTrue(answer.getResult());

        cmd = new Site2SiteVpnCfgCommand(true, "64.10.1.10", "64.10.1.1", "192.168.1.1/16", "124.10.1.10", "192.168.100.1/24", "3des-sha1,aes128-sha1;modp1536", "3des-sha1,aes128-md5", "psk", Long.valueOf(1800), Long.valueOf(1800), false, true);
        answer = _resource.executeRequest(cmd);
        assertTrue(answer.getResult());

        cmd = new Site2SiteVpnCfgCommand(false, "64.10.1.10", "64.10.1.1", "192.168.1.1/16", "124.10.1.10", "192.168.100.1/24", "3des-sha1,aes128-sha1;modp1536", "3des-sha1,aes128-md5", "psk", Long.valueOf(1800), Long.valueOf(1800), false, true);
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

        RemoteAccessVpnCfgCommand cmd = new RemoteAccessVpnCfgCommand(true, "124.10.10.10", "10.10.1.1", "10.10.1.10-10.10.1.20", "sharedkey", false);
        cmd.setLocalCidr("10.1.1.1/24");
        Answer answer = _resource.executeRequest(cmd);
        assertTrue(answer.getResult());

        cmd = new RemoteAccessVpnCfgCommand(false, "124.10.10.10", "10.10.1.1", "10.10.1.10-10.10.1.20", "sharedkey", false);
        cmd.setLocalCidr("10.1.1.1/24");
        answer = _resource.executeRequest(cmd);
        assertTrue(answer.getResult());

        cmd = new RemoteAccessVpnCfgCommand(true, "124.10.10.10", "10.10.1.1", "10.10.1.10-10.10.1.20", "sharedkey", true);
        cmd.setLocalCidr("10.1.1.1/24");
        answer = _resource.executeRequest(cmd);
        assertTrue(answer.getResult());
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

        List<FirewallRuleTO> rules = new ArrayList<>();
        List<String> sourceCidrs = new ArrayList<>();
        sourceCidrs.add("10.10.1.1/24");
        sourceCidrs.add("10.10.1.2/24");
        rules.add(new FirewallRuleTO(1, "64.10.10.10", "TCP", 22, 80, false, false, Purpose.Firewall, sourceCidrs, 0, 0));
        rules.add(new FirewallRuleTO(2, "64.10.10.10", "ICMP", 0, 0, false, false, Purpose.Firewall, sourceCidrs, -1, -1));
        rules.add(new FirewallRuleTO(3, "64.10.10.10", "ICMP", 0, 0, true, true, Purpose.Firewall, sourceCidrs, -1, -1));
        SetFirewallRulesCommand cmd = new SetFirewallRulesCommand(rules);
        Answer answer = _resource.executeRequest(cmd);
        assertTrue(answer.getResult());
        assertTrue(answer instanceof GroupAnswer);
        assertEquals(((GroupAnswer) answer).getResults().length, 3);

        //TODO Didn't test egress rule because not able to generate FirewallRuleVO object
    }

    private void verifyArgs(SetFirewallRulesCommand cmd, String script, String args) {
        assertEquals(script, VRScripts.FIREWALL_INGRESS);
        assertEquals(args, " -F -a 64.10.10.10:ICMP:0:0:10.10.1.1/24-10.10.1.2/24:,64.10.10.10:TCP:22:80:10.10.1.1/24-10.10.1.2/24:,64.10.10.10:reverted:0:0:0:,");
    }

    @Test
    public void testVmDataCommand() {
        VmDataCommand cmd = new VmDataCommand("10.1.10.4", "i-4-VM", true);
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

        Answer answer = _resource.executeRequest(cmd);
        assertTrue(answer.getResult());
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
        SavePasswordCommand cmd = new SavePasswordCommand("123pass", "10.1.10.4", "i-4-VM", true);
        Answer answer = _resource.executeRequest(cmd);
        assertTrue(answer.getResult());
    }

    private void verifyArgs(SavePasswordCommand cmd, String script, String args) {
        assertEquals(script, VRScripts.PASSWORD);
        assertEquals(args, "-v 10.1.10.4 -p 123pass");
    }

    @Test
    public void testDhcpEntryCommand() {
        _count = 0;
        DhcpEntryCommand cmd = new DhcpEntryCommand("12:34:56:78:90:AB", "10.1.10.2", "vm1", null, true);
        Answer answer = _resource.executeRequest(cmd);
        assertTrue(answer.getResult());

        cmd = new DhcpEntryCommand("12:34:56:78:90:AB", null, "vm1", "2001:db8:0:0:0:ff00:42:8329", true);
        cmd.setDuid(NetUtils.getDuidLL(cmd.getVmMac()));
        answer = _resource.executeRequest(cmd);
        assertTrue(answer.getResult());

        cmd = new DhcpEntryCommand("12:34:56:78:90:AB", "10.1.10.2", "vm1", "2001:db8:0:0:0:ff00:42:8329", true);
        cmd.setDuid(NetUtils.getDuidLL(cmd.getVmMac()));
        answer = _resource.executeRequest(cmd);
        assertTrue(answer.getResult());
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
        List<IpAliasTO> aliases = new ArrayList<>();
        aliases.add(new IpAliasTO("169.254.3.10", "255.255.255.0", "1"));
        aliases.add(new IpAliasTO("169.254.3.11", "255.255.255.0", "2"));
        aliases.add(new IpAliasTO("169.254.3.12", "255.255.255.0", "3"));
        CreateIpAliasCommand cmd = new CreateIpAliasCommand("169.254.3.10", aliases);
        Answer answer = _resource.executeRequest(cmd);
        assertTrue(answer.getResult());
    }

    private void verifyArgs(CreateIpAliasCommand cmd, String script, String args) {
        assertEquals(script, VRScripts.IPALIAS_CREATE);
        assertEquals(args, "1:169.254.3.10:255.255.255.0-2:169.254.3.11:255.255.255.0-3:169.254.3.12:255.255.255.0-");
    }

    @Test
    public void testDeleteIpAliasCommand() {
        List<IpAliasTO> aliases = new ArrayList<>();
        aliases.add(new IpAliasTO("169.254.3.10", "255.255.255.0", "1"));
        aliases.add(new IpAliasTO("169.254.3.11", "255.255.255.0", "2"));
        aliases.add(new IpAliasTO("169.254.3.12", "255.255.255.0", "3"));
        DeleteIpAliasCommand cmd = new DeleteIpAliasCommand("169.254.10.1", aliases, aliases);
        Answer answer = _resource.executeRequest(cmd);
        assertTrue(answer.getResult());
    }

    private void verifyArgs(DeleteIpAliasCommand cmd, String script, String args) {
        assertEquals(script, VRScripts.IPALIAS_DELETE);
        assertEquals(args, "1:169.254.3.10:255.255.255.0-2:169.254.3.11:255.255.255.0-3:169.254.3.12:255.255.255.0-- 1:169.254.3.10:255.255.255.0-2:169.254.3.11:255.255.255.0-3:169.254.3.12:255.255.255.0-");
    }

    @Test
    public void testDnsMasqConfigCommand() {
        List<DhcpTO> dhcps = new ArrayList<DhcpTO>();
        dhcps.add(new DhcpTO("10.1.20.2", "10.1.20.1", "255.255.255.0", "10.1.20.5"));
        dhcps.add(new DhcpTO("10.1.21.2", "10.1.21.1", "255.255.255.0", "10.1.21.5"));
        DnsMasqConfigCommand cmd = new DnsMasqConfigCommand(dhcps);
        Answer answer = _resource.executeRequest(cmd);
        assertTrue(answer.getResult());
    }

    private void verifyArgs(DnsMasqConfigCommand cmd, String script, String args) {
        assertEquals(script, VRScripts.DNSMASQ_CONFIG);
        assertEquals(args, "10.1.20.2:10.1.20.1:255.255.255.0:10.1.20.5-10.1.21.2:10.1.21.1:255.255.255.0:10.1.21.5-");
    }

    @Test
    public void testLoadBalancerConfigCommand() {
        _count = 0;

        List<LoadBalancerTO> lbs = new ArrayList<>();
        List<LbDestination> dests = new ArrayList<>();
        dests.add(new LbDestination(22, 80, "10.1.10.2", false));
        dests.add(new LbDestination(22, 80, "10.1.10.2", true));
        lbs.add(new LoadBalancerTO(UUID.randomUUID().toString(), "64.10.1.10", 80, "tcp", "algo", false, false, false, dests));
        LoadBalancerTO[] arrayLbs = new LoadBalancerTO[lbs.size()];
        lbs.toArray(arrayLbs);
        NicTO nic = new NicTO();
        LoadBalancerConfigCommand cmd = new LoadBalancerConfigCommand(arrayLbs, "64.10.2.10", "10.1.10.2", "192.168.1.2", nic, null, "1000", true);
        Answer answer = _resource.executeRequest(cmd);
        assertTrue(answer.getResult());

        cmd = new LoadBalancerConfigCommand(arrayLbs, "64.10.2.10", "10.1.10.2", "192.168.1.2", nic, Long.valueOf(1), "1000", true);
        answer = _resource.executeRequest(cmd);
        assertTrue(answer.getResult());
    }

    private void verifyFile(NetworkElementCommand cmd, String path, String filename, String content) {
        if (!(cmd instanceof LoadBalancerConfigCommand)) {
            fail("Only LB command would call this!");
        }
        _count ++;
        switch (_count) {
            case 1:
            case 3:
                assertEquals(path, "/etc/haproxy/");
                assertEquals(filename, "haproxy.cfg.new");
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
                        "\t#no option set here :<\n" +
                        "\tno option forceclose\n" +
                        "\ttimeout connect    5000\n" +
                        "\ttimeout client     50000\n" +
                        "\ttimeout server     50000\n" +
                        "\n" +
                        "listen stats_on_guest 10.1.10.2:8081\n" +
                        "\tstats enable\n" +
                        "\tstats uri     /admin?stats\n" +
                        "\tstats realm   Haproxy\\ Statistics\n" +
                        "\tstats auth    admin1:AdMiN123\n" +
                        "\n" +
                        "\t \n" +
                        "listen 64_10_1_10-80 64.10.1.10:80\n" +
                        "\tbalance algo\n" +
                        "\tserver 64_10_1_10-80_0 10.1.10.2:22 check\n" +
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
                assertEquals(args, " -i null -a 64.10.1.10:80:, -s 10.1.10.2:8081:0/0:,,");
                break;
            case 4:
                assertEquals(script, VRScripts.VPC_LB);
                assertEquals(args, " -i null -a 64.10.1.10:80:, -s 10.1.10.2:8081:0/0:,,");
                break;
            default:
                fail();
        }
    }

}

