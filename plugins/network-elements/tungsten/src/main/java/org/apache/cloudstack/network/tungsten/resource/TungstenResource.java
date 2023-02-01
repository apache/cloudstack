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
package org.apache.cloudstack.network.tungsten.resource;

import com.cloud.agent.IAgentControl;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.MaintainAnswer;
import com.cloud.agent.api.MaintainCommand;
import com.cloud.agent.api.PingCommand;
import com.cloud.agent.api.ReadyAnswer;
import com.cloud.agent.api.ReadyCommand;
import com.cloud.agent.api.StartupCommand;
import com.cloud.host.Host;
import com.cloud.resource.ServerResource;
import com.cloud.utils.TungstenUtils;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.net.NetUtils;
import net.juniper.tungsten.api.ApiConnectorFactory;
import net.juniper.tungsten.api.ApiObjectBase;
import net.juniper.tungsten.api.ApiPropertyBase;
import net.juniper.tungsten.api.ObjectReference;
import net.juniper.tungsten.api.types.AddressGroup;
import net.juniper.tungsten.api.types.ApplicationPolicySet;
import net.juniper.tungsten.api.types.FirewallPolicy;
import net.juniper.tungsten.api.types.FirewallRule;
import net.juniper.tungsten.api.types.FirewallSequence;
import net.juniper.tungsten.api.types.FloatingIp;
import net.juniper.tungsten.api.types.FloatingIpPool;
import net.juniper.tungsten.api.types.InstanceIp;
import net.juniper.tungsten.api.types.Loadbalancer;
import net.juniper.tungsten.api.types.LoadbalancerHealthmonitor;
import net.juniper.tungsten.api.types.LoadbalancerListener;
import net.juniper.tungsten.api.types.LoadbalancerMember;
import net.juniper.tungsten.api.types.LoadbalancerPool;
import net.juniper.tungsten.api.types.LogicalRouter;
import net.juniper.tungsten.api.types.NetworkPolicy;
import net.juniper.tungsten.api.types.Project;
import net.juniper.tungsten.api.types.SecurityGroup;
import net.juniper.tungsten.api.types.SequenceType;
import net.juniper.tungsten.api.types.ServiceGroup;
import net.juniper.tungsten.api.types.Tag;
import net.juniper.tungsten.api.types.TagType;
import net.juniper.tungsten.api.types.VirtualMachine;
import net.juniper.tungsten.api.types.VirtualMachineInterface;
import net.juniper.tungsten.api.types.VirtualNetwork;
import net.juniper.tungsten.api.types.VirtualNetworkPolicyType;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.network.tungsten.agent.api.AddTungstenNetworkGatewayToLogicalRouterCommand;
import org.apache.cloudstack.network.tungsten.agent.api.AddTungstenNetworkSubnetCommand;
import org.apache.cloudstack.network.tungsten.agent.api.AddTungstenPolicyRuleCommand;
import org.apache.cloudstack.network.tungsten.agent.api.AddTungstenSecondaryIpAddressCommand;
import org.apache.cloudstack.network.tungsten.agent.api.AddTungstenSecurityGroupRuleCommand;
import org.apache.cloudstack.network.tungsten.agent.api.AddTungstenVmToSecurityGroupCommand;
import org.apache.cloudstack.network.tungsten.agent.api.ApplyTungstenNetworkPolicyCommand;
import org.apache.cloudstack.network.tungsten.agent.api.ApplyTungstenPortForwardingCommand;
import org.apache.cloudstack.network.tungsten.agent.api.ApplyTungstenTagCommand;
import org.apache.cloudstack.network.tungsten.agent.api.AssignTungstenFloatingIpCommand;
import org.apache.cloudstack.network.tungsten.agent.api.ClearTungstenNetworkGatewayCommand;
import org.apache.cloudstack.network.tungsten.agent.api.CreateTungstenAddressGroupCommand;
import org.apache.cloudstack.network.tungsten.agent.api.CreateTungstenApplicationPolicySetCommand;
import org.apache.cloudstack.network.tungsten.agent.api.CreateTungstenDefaultProjectCommand;
import org.apache.cloudstack.network.tungsten.agent.api.CreateTungstenDomainCommand;
import org.apache.cloudstack.network.tungsten.agent.api.CreateTungstenFirewallPolicyCommand;
import org.apache.cloudstack.network.tungsten.agent.api.CreateTungstenFirewallRuleCommand;
import org.apache.cloudstack.network.tungsten.agent.api.CreateTungstenFloatingIpCommand;
import org.apache.cloudstack.network.tungsten.agent.api.CreateTungstenFloatingIpPoolCommand;
import org.apache.cloudstack.network.tungsten.agent.api.CreateTungstenLogicalRouterCommand;
import org.apache.cloudstack.network.tungsten.agent.api.CreateTungstenNetworkCommand;
import org.apache.cloudstack.network.tungsten.agent.api.CreateTungstenNetworkLoadbalancerCommand;
import org.apache.cloudstack.network.tungsten.agent.api.CreateTungstenNetworkPolicyCommand;
import org.apache.cloudstack.network.tungsten.agent.api.CreateTungstenPolicyCommand;
import org.apache.cloudstack.network.tungsten.agent.api.CreateTungstenProjectCommand;
import org.apache.cloudstack.network.tungsten.agent.api.CreateTungstenRoutingLogicalRouterCommand;
import org.apache.cloudstack.network.tungsten.agent.api.CreateTungstenSecurityGroupCommand;
import org.apache.cloudstack.network.tungsten.agent.api.CreateTungstenServiceGroupCommand;
import org.apache.cloudstack.network.tungsten.agent.api.CreateTungstenTagCommand;
import org.apache.cloudstack.network.tungsten.agent.api.CreateTungstenTagTypeCommand;
import org.apache.cloudstack.network.tungsten.agent.api.CreateTungstenVirtualMachineCommand;
import org.apache.cloudstack.network.tungsten.agent.api.DeleteTungstenAddressGroupCommand;
import org.apache.cloudstack.network.tungsten.agent.api.DeleteTungstenApplicationPolicySetCommand;
import org.apache.cloudstack.network.tungsten.agent.api.DeleteTungstenDomainCommand;
import org.apache.cloudstack.network.tungsten.agent.api.DeleteTungstenFirewallPolicyCommand;
import org.apache.cloudstack.network.tungsten.agent.api.DeleteTungstenFirewallRuleCommand;
import org.apache.cloudstack.network.tungsten.agent.api.DeleteTungstenFloatingIpCommand;
import org.apache.cloudstack.network.tungsten.agent.api.DeleteTungstenFloatingIpPoolCommand;
import org.apache.cloudstack.network.tungsten.agent.api.DeleteTungstenLoadBalancerCommand;
import org.apache.cloudstack.network.tungsten.agent.api.DeleteTungstenLoadBalancerListenerCommand;
import org.apache.cloudstack.network.tungsten.agent.api.DeleteTungstenNetworkCommand;
import org.apache.cloudstack.network.tungsten.agent.api.DeleteTungstenNetworkPolicyCommand;
import org.apache.cloudstack.network.tungsten.agent.api.DeleteTungstenObjectCommand;
import org.apache.cloudstack.network.tungsten.agent.api.DeleteTungstenPolicyCommand;
import org.apache.cloudstack.network.tungsten.agent.api.DeleteTungstenProjectCommand;
import org.apache.cloudstack.network.tungsten.agent.api.DeleteTungstenRoutingLogicalRouterCommand;
import org.apache.cloudstack.network.tungsten.agent.api.DeleteTungstenSecurityGroupCommand;
import org.apache.cloudstack.network.tungsten.agent.api.DeleteTungstenServiceGroupCommand;
import org.apache.cloudstack.network.tungsten.agent.api.DeleteTungstenTagCommand;
import org.apache.cloudstack.network.tungsten.agent.api.DeleteTungstenTagTypeCommand;
import org.apache.cloudstack.network.tungsten.agent.api.DeleteTungstenVRouterPortCommand;
import org.apache.cloudstack.network.tungsten.agent.api.DeleteTungstenVmCommand;
import org.apache.cloudstack.network.tungsten.agent.api.DeleteTungstenVmInterfaceCommand;
import org.apache.cloudstack.network.tungsten.agent.api.GetTungstenFabricNetworkCommand;
import org.apache.cloudstack.network.tungsten.agent.api.GetTungstenFloatingIpsCommand;
import org.apache.cloudstack.network.tungsten.agent.api.GetTungstenLoadBalancerCommand;
import org.apache.cloudstack.network.tungsten.agent.api.GetTungstenNatIpCommand;
import org.apache.cloudstack.network.tungsten.agent.api.GetTungstenNetworkDnsCommand;
import org.apache.cloudstack.network.tungsten.agent.api.GetTungstenPolicyCommand;
import org.apache.cloudstack.network.tungsten.agent.api.GetTungstenSecurityGroupCommand;
import org.apache.cloudstack.network.tungsten.agent.api.ListTungstenAddressGroupCommand;
import org.apache.cloudstack.network.tungsten.agent.api.ListTungstenApplicationPolicySetCommand;
import org.apache.cloudstack.network.tungsten.agent.api.ListTungstenConnectedNetworkFromLogicalRouterCommand;
import org.apache.cloudstack.network.tungsten.agent.api.ListTungstenFirewallPolicyCommand;
import org.apache.cloudstack.network.tungsten.agent.api.ListTungstenFirewallRuleCommand;
import org.apache.cloudstack.network.tungsten.agent.api.ListTungstenNetworkCommand;
import org.apache.cloudstack.network.tungsten.agent.api.ListTungstenNicCommand;
import org.apache.cloudstack.network.tungsten.agent.api.ListTungstenPolicyCommand;
import org.apache.cloudstack.network.tungsten.agent.api.ListTungstenPolicyRuleCommand;
import org.apache.cloudstack.network.tungsten.agent.api.ListTungstenRoutingLogicalRouterCommand;
import org.apache.cloudstack.network.tungsten.agent.api.ListTungstenServiceGroupCommand;
import org.apache.cloudstack.network.tungsten.agent.api.ListTungstenTagCommand;
import org.apache.cloudstack.network.tungsten.agent.api.ListTungstenTagTypeCommand;
import org.apache.cloudstack.network.tungsten.agent.api.ListTungstenVmCommand;
import org.apache.cloudstack.network.tungsten.agent.api.ReleaseTungstenFloatingIpCommand;
import org.apache.cloudstack.network.tungsten.agent.api.RemoveTungstenNetworkGatewayFromLogicalRouterCommand;
import org.apache.cloudstack.network.tungsten.agent.api.RemoveTungstenNetworkSubnetCommand;
import org.apache.cloudstack.network.tungsten.agent.api.RemoveTungstenPolicyCommand;
import org.apache.cloudstack.network.tungsten.agent.api.RemoveTungstenPolicyRuleCommand;
import org.apache.cloudstack.network.tungsten.agent.api.RemoveTungstenSecondaryIpAddressCommand;
import org.apache.cloudstack.network.tungsten.agent.api.RemoveTungstenSecurityGroupRuleCommand;
import org.apache.cloudstack.network.tungsten.agent.api.RemoveTungstenTagCommand;
import org.apache.cloudstack.network.tungsten.agent.api.RemoveTungstenVmFromSecurityGroupCommand;
import org.apache.cloudstack.network.tungsten.agent.api.SetTungstenNetworkGatewayCommand;
import org.apache.cloudstack.network.tungsten.agent.api.StartupTungstenCommand;
import org.apache.cloudstack.network.tungsten.agent.api.TungstenAnswer;
import org.apache.cloudstack.network.tungsten.agent.api.UpdateLoadBalancerServiceInstanceCommand;
import org.apache.cloudstack.network.tungsten.agent.api.UpdateTungstenDefaultSecurityGroupCommand;
import org.apache.cloudstack.network.tungsten.agent.api.UpdateTungstenLoadBalancerHealthMonitorCommand;
import org.apache.cloudstack.network.tungsten.agent.api.UpdateTungstenLoadBalancerListenerCommand;
import org.apache.cloudstack.network.tungsten.agent.api.UpdateTungstenLoadBalancerMemberCommand;
import org.apache.cloudstack.network.tungsten.agent.api.UpdateTungstenLoadBalancerPoolCommand;
import org.apache.cloudstack.network.tungsten.agent.api.UpdateTungstenVrouterConfigCommand;
import org.apache.cloudstack.network.tungsten.model.TungstenLoadBalancerMember;
import org.apache.cloudstack.network.tungsten.model.TungstenLogicalRouter;
import org.apache.cloudstack.network.tungsten.model.TungstenModel;
import org.apache.cloudstack.network.tungsten.model.TungstenNetworkPolicy;
import org.apache.cloudstack.network.tungsten.model.TungstenTag;
import org.apache.cloudstack.network.tungsten.service.TungstenApi;
import org.apache.cloudstack.network.tungsten.service.TungstenVRouterApi;
import org.apache.cloudstack.network.tungsten.vrouter.Port;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.naming.ConfigurationException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TungstenResource implements ServerResource {

    protected Logger logger = LogManager.getLogger(getClass());

    private String name;
    private String guid;
    private String zoneId;
    private int numRetries;
    private String vrouterPort;

    protected TungstenApi tungstenApi;

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {

        this.name = (String) params.get("name");
        if (this.name == null) {
            throw new ConfigurationException("Unable to find name");
        }

        guid = (String) params.get("guid");
        if (guid == null) {
            throw new ConfigurationException("Unable to find the guid");
        }

        zoneId = (String) params.get("zoneId");
        if (zoneId == null) {
            throw new ConfigurationException("Unable to find zone");
        }

        numRetries = 2;

        String hostname = (String) params.get("hostname");
        if (hostname == null) {
            throw new ConfigurationException("Missing Tungsten-Fabric hostname from params: " + params);
        }

        String port = (String) params.get("port");
        if (port == null) {
            throw new ConfigurationException("Missing Tungsten-Fabric port from params: " + params);
        }

        vrouterPort = (String) params.get("vrouterPort");
        if (vrouterPort == null) {
            throw new ConfigurationException("Missing Tungsten-Fabric vrouter port from params: " + params);
        }

        String introspectPort = (String) params.get("introspectPort");
        if (introspectPort == null) {
            throw new ConfigurationException("Missing Tungsten-Fabric introspect port from params: " + params);
        }

        tungstenApi = new TungstenApi();
        tungstenApi.setHostname(hostname);
        tungstenApi.setPort(port);
        tungstenApi.setApiConnector(ApiConnectorFactory.build(hostname, Integer.parseInt(port)));
        return true;
    }

    @Override
    public Host.Type getType() {
        return Host.Type.L2Networking;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public StartupCommand[] initialize() {
        StartupTungstenCommand sc = new StartupTungstenCommand();
        sc.setGuid(guid);
        sc.setName(name);
        sc.setDataCenter(zoneId);
        sc.setPod("");
        sc.setPrivateIpAddress("");
        sc.setStorageIpAddress("");
        sc.setVersion("");
        return new StartupCommand[] {sc};
    }

    @Override
    public PingCommand getCurrentStatus(long id) {
        try {
            tungstenApi.checkTungstenProviderConnection();
        } catch (ServerApiException e) {
            logger.error("Check Tungsten-Fabric provider connection failed", e);
            return null;
        }
        return new PingCommand(Host.Type.L2Networking, id);
    }

    @Override
    public Answer executeRequest(Command cmd) {
        return executeRequestGroup1(cmd, numRetries);
    }

    private Answer executeRequestGroup1(Command cmd, int numRetries) {
        if (cmd instanceof ReadyCommand) {
            return executeRequest((ReadyCommand) cmd);
        } else if (cmd instanceof MaintainCommand) {
            return executeRequest((MaintainCommand) cmd);
        } else if (cmd instanceof CreateTungstenNetworkCommand) {
            return executeRequest((CreateTungstenNetworkCommand) cmd, numRetries);
        } else if (cmd instanceof DeleteTungstenNetworkCommand) {
            return executeRequest((DeleteTungstenNetworkCommand) cmd, numRetries);
        } else if (cmd instanceof DeleteTungstenVmInterfaceCommand) {
            return executeRequest((DeleteTungstenVmInterfaceCommand) cmd, numRetries);
        } else if (cmd instanceof DeleteTungstenVmCommand) {
            return executeRequest((DeleteTungstenVmCommand) cmd, numRetries);
        } else if (cmd instanceof CreateTungstenLogicalRouterCommand) {
            return executeRequest((CreateTungstenLogicalRouterCommand) cmd, numRetries);
        } else if (cmd instanceof CreateTungstenVirtualMachineCommand) {
            return executeRequest((CreateTungstenVirtualMachineCommand) cmd);
        } else if (cmd instanceof SetTungstenNetworkGatewayCommand) {
            return executeRequest((SetTungstenNetworkGatewayCommand) cmd);
        } else if (cmd instanceof DeleteTungstenVRouterPortCommand) {
            return executeRequest((DeleteTungstenVRouterPortCommand) cmd);
        } else if (cmd instanceof GetTungstenNetworkDnsCommand) {
            return executeRequest((GetTungstenNetworkDnsCommand) cmd);
        } else if (cmd instanceof GetTungstenPolicyCommand) {
            return executeRequest((GetTungstenPolicyCommand) cmd);
        } else if (cmd instanceof ClearTungstenNetworkGatewayCommand) {
            return executeRequest((ClearTungstenNetworkGatewayCommand) cmd);
        } else {
            return executeRequestGroup2(cmd, numRetries);
        }
    }

    private Answer executeRequestGroup2(Command cmd, int numRetries) {
        if (cmd instanceof CreateTungstenFloatingIpPoolCommand) {
            return executeRequest((CreateTungstenFloatingIpPoolCommand) cmd);
        } else if (cmd instanceof CreateTungstenFloatingIpCommand) {
            return executeRequest((CreateTungstenFloatingIpCommand) cmd);
        } else if (cmd instanceof DeleteTungstenFloatingIpCommand) {
            return executeRequest((DeleteTungstenFloatingIpCommand) cmd);
        } else if (cmd instanceof DeleteTungstenFloatingIpPoolCommand) {
            return executeRequest((DeleteTungstenFloatingIpPoolCommand) cmd);
        } else if (cmd instanceof AssignTungstenFloatingIpCommand) {
            return executeRequest((AssignTungstenFloatingIpCommand) cmd);
        } else if (cmd instanceof ReleaseTungstenFloatingIpCommand) {
            return executeRequest((ReleaseTungstenFloatingIpCommand) cmd);
        } else if (cmd instanceof GetTungstenNatIpCommand) {
            return executeRequest((GetTungstenNatIpCommand) cmd, numRetries);
        } else if (cmd instanceof CreateTungstenNetworkPolicyCommand) {
            return executeRequest((CreateTungstenNetworkPolicyCommand) cmd);
        } else if (cmd instanceof ApplyTungstenNetworkPolicyCommand) {
            return executeRequest((ApplyTungstenNetworkPolicyCommand) cmd);
        } else if (cmd instanceof DeleteTungstenNetworkPolicyCommand) {
            return executeRequest((DeleteTungstenNetworkPolicyCommand) cmd, numRetries);
        } else if (cmd instanceof GetTungstenFloatingIpsCommand) {
            return executeRequest((GetTungstenFloatingIpsCommand) cmd);
        } else if (cmd instanceof GetTungstenFabricNetworkCommand) {
            return executeRequest((GetTungstenFabricNetworkCommand) cmd);
        } else if (cmd instanceof CreateTungstenNetworkLoadbalancerCommand) {
            return executeRequest((CreateTungstenNetworkLoadbalancerCommand) cmd);
        } else if (cmd instanceof UpdateLoadBalancerServiceInstanceCommand) {
            return executeRequest((UpdateLoadBalancerServiceInstanceCommand) cmd, numRetries);
        } else {
            return executeRequestGroup3(cmd, numRetries);
        }
    }

    private Answer executeRequestGroup3(Command cmd, int numRetries) {
        if (cmd instanceof DeleteTungstenLoadBalancerCommand) {
            return executeRequest((DeleteTungstenLoadBalancerCommand) cmd, numRetries);
        } else if (cmd instanceof DeleteTungstenLoadBalancerListenerCommand) {
            return executeRequest((DeleteTungstenLoadBalancerListenerCommand) cmd, numRetries);
        } else if (cmd instanceof UpdateTungstenLoadBalancerPoolCommand) {
            return executeRequest((UpdateTungstenLoadBalancerPoolCommand) cmd, numRetries);
        } else if (cmd instanceof UpdateTungstenLoadBalancerMemberCommand) {
            return executeRequest((UpdateTungstenLoadBalancerMemberCommand) cmd, numRetries);
        } else if (cmd instanceof UpdateTungstenLoadBalancerListenerCommand) {
            return executeRequest((UpdateTungstenLoadBalancerListenerCommand) cmd, numRetries);
        } else if (cmd instanceof UpdateTungstenLoadBalancerHealthMonitorCommand) {
            return executeRequest((UpdateTungstenLoadBalancerHealthMonitorCommand) cmd, numRetries);
        } else if (cmd instanceof GetTungstenLoadBalancerCommand) {
            return executeRequest((GetTungstenLoadBalancerCommand) cmd, numRetries);
        } else if (cmd instanceof CreateTungstenDomainCommand) {
            return executeRequest((CreateTungstenDomainCommand) cmd);
        } else if (cmd instanceof DeleteTungstenDomainCommand) {
            return executeRequest((DeleteTungstenDomainCommand) cmd, numRetries);
        } else if (cmd instanceof CreateTungstenProjectCommand) {
            return executeRequest((CreateTungstenProjectCommand) cmd);
        } else if (cmd instanceof DeleteTungstenProjectCommand) {
            return executeRequest((DeleteTungstenProjectCommand) cmd, numRetries);
        } else if (cmd instanceof ApplyTungstenPortForwardingCommand) {
            return executeRequest((ApplyTungstenPortForwardingCommand) cmd, numRetries);
        } else if (cmd instanceof DeleteTungstenObjectCommand) {
            return executeRequest((DeleteTungstenObjectCommand) cmd, numRetries);
        } else if (cmd instanceof AddTungstenNetworkSubnetCommand) {
            return executeRequest((AddTungstenNetworkSubnetCommand) cmd, numRetries);
        } else {
            return executeRequestGroup4(cmd, numRetries);
        }
    }

    private Answer executeRequestGroup4(Command cmd, int numRetries) {
        if (cmd instanceof RemoveTungstenNetworkSubnetCommand) {
            return executeRequest((RemoveTungstenNetworkSubnetCommand) cmd, numRetries);
        } else if (cmd instanceof CreateTungstenSecurityGroupCommand) {
            return executeRequest((CreateTungstenSecurityGroupCommand) cmd, numRetries);
        } else if (cmd instanceof DeleteTungstenSecurityGroupCommand) {
            return executeRequest((DeleteTungstenSecurityGroupCommand) cmd, numRetries);
        } else if (cmd instanceof AddTungstenSecurityGroupRuleCommand) {
            return executeRequest((AddTungstenSecurityGroupRuleCommand) cmd, numRetries);
        } else if (cmd instanceof AddTungstenVmToSecurityGroupCommand) {
            return executeRequest((AddTungstenVmToSecurityGroupCommand) cmd, numRetries);
        } else if (cmd instanceof RemoveTungstenVmFromSecurityGroupCommand) {
            return executeRequest((RemoveTungstenVmFromSecurityGroupCommand) cmd, numRetries);
        } else if (cmd instanceof RemoveTungstenSecurityGroupRuleCommand) {
            return executeRequest((RemoveTungstenSecurityGroupRuleCommand) cmd, numRetries);
        } else if (cmd instanceof AddTungstenSecondaryIpAddressCommand) {
            return executeRequest((AddTungstenSecondaryIpAddressCommand) cmd, numRetries);
        } else if (cmd instanceof RemoveTungstenSecondaryIpAddressCommand) {
            return executeRequest((RemoveTungstenSecondaryIpAddressCommand) cmd, numRetries);
        } else if (cmd instanceof GetTungstenSecurityGroupCommand) {
            return executeRequest((GetTungstenSecurityGroupCommand) cmd);
        } else if (cmd instanceof CreateTungstenPolicyCommand) {
            return executeRequest((CreateTungstenPolicyCommand) cmd, numRetries);
        } else if (cmd instanceof AddTungstenPolicyRuleCommand) {
            return executeRequest((AddTungstenPolicyRuleCommand) cmd, numRetries);
        } else if (cmd instanceof ListTungstenPolicyCommand) {
            return executeRequest((ListTungstenPolicyCommand) cmd, numRetries);
        } else if (cmd instanceof ListTungstenNetworkCommand) {
            return executeRequest((ListTungstenNetworkCommand) cmd, numRetries);
        } else {
            return executeRequestGroup5(cmd, numRetries);
        }
    }

    private Answer executeRequestGroup5(Command cmd, int numRetries) {
        if (cmd instanceof ListTungstenNicCommand) {
            return executeRequest((ListTungstenNicCommand) cmd, numRetries);
        } else if (cmd instanceof ListTungstenVmCommand) {
            return executeRequest((ListTungstenVmCommand) cmd, numRetries);
        } else if (cmd instanceof ListTungstenPolicyRuleCommand) {
            return executeRequest((ListTungstenPolicyRuleCommand) cmd, numRetries);
        } else if (cmd instanceof DeleteTungstenPolicyCommand) {
            return executeRequest((DeleteTungstenPolicyCommand) cmd, numRetries);
        } else if (cmd instanceof RemoveTungstenPolicyRuleCommand) {
            return executeRequest((RemoveTungstenPolicyRuleCommand) cmd, numRetries);
        } else if (cmd instanceof CreateTungstenTagCommand) {
            return executeRequest((CreateTungstenTagCommand) cmd, numRetries);
        } else if (cmd instanceof CreateTungstenTagTypeCommand) {
            return executeRequest((CreateTungstenTagTypeCommand) cmd, numRetries);
        } else if (cmd instanceof DeleteTungstenTagCommand) {
            return executeRequest((DeleteTungstenTagCommand) cmd, numRetries);
        } else if (cmd instanceof DeleteTungstenTagTypeCommand) {
            return executeRequest((DeleteTungstenTagTypeCommand) cmd, numRetries);
        } else if (cmd instanceof ListTungstenTagCommand) {
            return executeRequest((ListTungstenTagCommand) cmd, numRetries);
        } else if (cmd instanceof ListTungstenTagTypeCommand) {
            return executeRequest((ListTungstenTagTypeCommand) cmd, numRetries);
        } else if (cmd instanceof ApplyTungstenTagCommand) {
            return executeRequest((ApplyTungstenTagCommand) cmd, numRetries);
        } else if (cmd instanceof RemoveTungstenPolicyCommand) {
            return executeRequest((RemoveTungstenPolicyCommand) cmd, numRetries);
        } else if (cmd instanceof RemoveTungstenTagCommand) {
            return executeRequest((RemoveTungstenTagCommand) cmd, numRetries);
        } else {
            return executeRequestGroup6(cmd, numRetries);
        }
    }

    private Answer executeRequestGroup6(Command cmd, int numRetries) {
        if (cmd instanceof CreateTungstenApplicationPolicySetCommand) {
            return executeRequest((CreateTungstenApplicationPolicySetCommand) cmd, numRetries);
        } else if (cmd instanceof CreateTungstenFirewallPolicyCommand) {
            return executeRequest((CreateTungstenFirewallPolicyCommand) cmd, numRetries);
        } else if (cmd instanceof CreateTungstenFirewallRuleCommand) {
            return executeRequest((CreateTungstenFirewallRuleCommand) cmd, numRetries);
        } else if (cmd instanceof CreateTungstenServiceGroupCommand) {
            return executeRequest((CreateTungstenServiceGroupCommand) cmd, numRetries);
        } else if (cmd instanceof CreateTungstenAddressGroupCommand) {
            return executeRequest((CreateTungstenAddressGroupCommand) cmd, numRetries);
        } else if (cmd instanceof ListTungstenApplicationPolicySetCommand) {
            return executeRequest((ListTungstenApplicationPolicySetCommand) cmd, numRetries);
        } else if (cmd instanceof ListTungstenFirewallPolicyCommand) {
            return executeRequest((ListTungstenFirewallPolicyCommand) cmd, numRetries);
        } else if (cmd instanceof ListTungstenFirewallRuleCommand) {
            return executeRequest((ListTungstenFirewallRuleCommand) cmd, numRetries);
        } else if (cmd instanceof ListTungstenServiceGroupCommand) {
            return executeRequest((ListTungstenServiceGroupCommand) cmd, numRetries);
        } else if (cmd instanceof ListTungstenAddressGroupCommand) {
            return executeRequest((ListTungstenAddressGroupCommand) cmd, numRetries);
        } else if (cmd instanceof DeleteTungstenApplicationPolicySetCommand) {
            return executeRequest((DeleteTungstenApplicationPolicySetCommand) cmd, numRetries);
        } else if (cmd instanceof DeleteTungstenFirewallPolicyCommand) {
            return executeRequest((DeleteTungstenFirewallPolicyCommand) cmd, numRetries);
        } else if (cmd instanceof DeleteTungstenFirewallRuleCommand) {
            return executeRequest((DeleteTungstenFirewallRuleCommand) cmd, numRetries);
        } else if (cmd instanceof DeleteTungstenServiceGroupCommand) {
            return executeRequest((DeleteTungstenServiceGroupCommand) cmd, numRetries);
        } else {
            return executeRequestGroup7(cmd, numRetries);
        }
    }

    private Answer executeRequestGroup7(Command cmd, int numRetries) {
        if (cmd instanceof DeleteTungstenAddressGroupCommand) {
            return executeRequest((DeleteTungstenAddressGroupCommand) cmd, numRetries);
        } else if (cmd instanceof UpdateTungstenVrouterConfigCommand) {
            return executeRequest((UpdateTungstenVrouterConfigCommand) cmd, numRetries);
        } else if (cmd instanceof UpdateTungstenDefaultSecurityGroupCommand) {
            return executeRequest((UpdateTungstenDefaultSecurityGroupCommand) cmd, numRetries);
        } else if (cmd instanceof CreateTungstenRoutingLogicalRouterCommand) {
            return executeRequest((CreateTungstenRoutingLogicalRouterCommand) cmd, numRetries);
        } else if (cmd instanceof AddTungstenNetworkGatewayToLogicalRouterCommand) {
            return executeRequest((AddTungstenNetworkGatewayToLogicalRouterCommand) cmd);
        } else if (cmd instanceof ListTungstenRoutingLogicalRouterCommand) {
            return executeRequest((ListTungstenRoutingLogicalRouterCommand) cmd, numRetries);
        } else if (cmd instanceof RemoveTungstenNetworkGatewayFromLogicalRouterCommand) {
            return executeRequest((RemoveTungstenNetworkGatewayFromLogicalRouterCommand) cmd);
        } else if (cmd instanceof DeleteTungstenRoutingLogicalRouterCommand) {
            return executeRequest((DeleteTungstenRoutingLogicalRouterCommand) cmd, numRetries);
        } else if (cmd instanceof ListTungstenConnectedNetworkFromLogicalRouterCommand) {
            return executeRequest((ListTungstenConnectedNetworkFromLogicalRouterCommand) cmd);
        } else if (cmd instanceof CreateTungstenDefaultProjectCommand) {
            return executeRequest((CreateTungstenDefaultProjectCommand) cmd);
        }

        logger.debug("Received unsupported command " + cmd.toString());
        return Answer.createUnsupportedCommandAnswer(cmd);
    }

    private Answer executeRequest(CreateTungstenNetworkCommand cmd, int numRetries) {
        Project project = (Project) tungstenApi.getTungstenProjectByFqn(cmd.getProjectFqn());
        if (project == null) {
            if (numRetries > 0) {
                return retry(cmd, --numRetries);
            } else {
                return new TungstenAnswer(cmd, new IOException());
            }
        }

        VirtualNetwork virtualNetwork = tungstenApi.createTungstenNetwork(cmd.getUuid(), cmd.getName(),
            cmd.getDisplayName(), project.getUuid(), cmd.isRouterExternal(), cmd.isShared(), cmd.getIpPrefix(),
            cmd.getIpPrefixLen(), cmd.getGateway(), cmd.isDhcpEnable(), cmd.getDnsServer(), cmd.getAllocationStart(),
            cmd.getAllocationEnd(), cmd.isIpFromStart(), cmd.isManagementNetwork(), cmd.getSubnetName());

        if (virtualNetwork != null)
            return new TungstenAnswer(cmd, virtualNetwork, true, "Tungsten-Fabric network created");
        else {
            if (numRetries > 0) {
                return retry(cmd, --numRetries);
            } else {
                return new TungstenAnswer(cmd, new IOException());
            }
        }
    }

    private Answer executeRequest(DeleteTungstenVmInterfaceCommand cmd, int numRetries) {
        Project project = (Project) tungstenApi.getTungstenProjectByFqn(cmd.getProjectFqn());
        VirtualMachineInterface vmi = (VirtualMachineInterface) tungstenApi.getTungstenObjectByName(
            VirtualMachineInterface.class, project.getQualifiedName(), cmd.getName());
        if (vmi != null) {
            cleanUpVmiFloatingIp(vmi);
            boolean deleted = tungstenApi.deleteTungstenVmInterface(vmi);
            if (deleted)
                return new TungstenAnswer(cmd, true, "Tungsten-Fabric virtual machine interface deleted");
            else {
                if (numRetries > 0) {
                    return retry(cmd, --numRetries);
                } else {
                    return new TungstenAnswer(cmd, new IOException());
                }
            }
        }

        return new TungstenAnswer(cmd, true, "Tungsten-Fabric virtual machine interface is not exist");
    }

    private void cleanUpVmiFloatingIp(VirtualMachineInterface vmi) {
        List<ObjectReference<ApiPropertyBase>> objectReferenceList = vmi.getFloatingIpBackRefs();
        if (objectReferenceList != null) {
            for(ObjectReference<ApiPropertyBase> objectReference : objectReferenceList) {
                FloatingIp floatingIp = (FloatingIp) tungstenApi.getTungstenObject(FloatingIp.class, objectReference.getUuid());
                floatingIp.clearVirtualMachineInterface();
                boolean cleared = tungstenApi.updateTungstenObject(floatingIp);
                if (!cleared) {
                    throw new CloudRuntimeException("Can not clear virtual machine interface from floating ip ");
                }
            }
        }
    }

    private Answer executeRequest(DeleteTungstenVmCommand cmd, int numRetries) {
        VirtualMachine vm = (VirtualMachine) tungstenApi.getTungstenObject(VirtualMachine.class, cmd.getVirtualMachineUuid());
        if(vm != null && vm.getVirtualMachineInterfaceBackRefs() != null) {
            return new TungstenAnswer(cmd, true, "Tungsten-Fabric virtual machine is not deleted because there are nics attached to this vm");
        }
        boolean deleted = tungstenApi.deleteTungstenObject(VirtualMachine.class, cmd.getVirtualMachineUuid());
        if (deleted)
            return new TungstenAnswer(cmd, true, "Tungsten-Fabric virtual machine deleted");
        else {
            if (numRetries > 0) {
                return retry(cmd, --numRetries);
            } else {
                return new TungstenAnswer(cmd, new IOException());
            }
        }
    }

    private Answer executeRequest(DeleteTungstenNetworkCommand cmd, int numRetries) {
        boolean deleted = tungstenApi.deleteTungstenObject(VirtualNetwork.class, cmd.getNetworkUuid());
        if (deleted)
            return new TungstenAnswer(cmd, true, "Tungsten-Fabric network deleted");
        else {
            if (numRetries > 0) {
                return retry(cmd, --numRetries);
            } else {
                return new TungstenAnswer(cmd, new IOException());
            }
        }
    }

    private Answer executeRequest(CreateTungstenLogicalRouterCommand cmd, int numRetries) {
        Project project = (Project) tungstenApi.getTungstenProjectByFqn(cmd.getProjectFqn());
        LogicalRouter logicalRouter = (LogicalRouter) tungstenApi.createTungstenLogicalRouter(cmd.getName(),
            project.getUuid(), cmd.getPubNetworkUuid());
        if (logicalRouter != null)
            return new TungstenAnswer(cmd, logicalRouter, true, "Tungsten-Fabric logical router created");
        else {
            if (numRetries > 0) {
                return retry(cmd, --numRetries);
            } else {
                return new TungstenAnswer(cmd, new IOException());
            }
        }
    }

    private Answer executeRequest(CreateTungstenVirtualMachineCommand cmd) {
        Project project = (Project) tungstenApi.getTungstenProjectByFqn(cmd.getProjectFqn());
        if (project == null) {
            return new TungstenAnswer(cmd, new IOException());
        }

        VirtualNetwork virtualNetwork = (VirtualNetwork) tungstenApi.getTungstenObject(VirtualNetwork.class,
            cmd.getVnUuid());
        if (virtualNetwork == null) {
            return new TungstenAnswer(cmd, new IOException());
        }

        VirtualMachine virtualMachine = (VirtualMachine) tungstenApi.getTungstenObject(VirtualMachine.class,
            cmd.getVmUuid());
        if (virtualMachine == null) {
            virtualMachine = tungstenApi.createTungstenVirtualMachine(cmd.getVmUuid(), cmd.getVmName());
        }
        if (virtualMachine == null) {
            return new TungstenAnswer(cmd, new IOException());
        }

        String vmiName = TungstenUtils.getVmiName(cmd.getTrafficType(), cmd.getVmType(), cmd.getVmName(),
            cmd.getNicId());
        VirtualMachineInterface virtualMachineInterface =
            (VirtualMachineInterface) tungstenApi.getTungstenObjectByName(
            VirtualMachineInterface.class, project.getQualifiedName(), vmiName);
        if (virtualMachineInterface == null) {
            virtualMachineInterface = tungstenApi.createTungstenVmInterface(cmd.getNicUuid(), vmiName, cmd.getMac(),
                cmd.getVnUuid(), cmd.getVmUuid(), project.getUuid(), cmd.getGateway(), cmd.isDefaultNic());
        }
        if (virtualMachineInterface == null) {
            return new TungstenAnswer(cmd, new IOException());
        }

        String iiName = TungstenUtils.getInstanceIpName(cmd.getTrafficType(), cmd.getVmType(), cmd.getVmName(),
            cmd.getNicId());
        InstanceIp instanceIp = (InstanceIp) tungstenApi.getTungstenObjectByName(InstanceIp.class, null, iiName);
        if (instanceIp == null) {
            instanceIp = tungstenApi.createTungstenInstanceIp(iiName, cmd.getIp(), cmd.getVnUuid(),
                virtualMachineInterface.getUuid());
        }
        if (instanceIp == null) {
            return new TungstenAnswer(cmd, new IOException());
        }

        if (cmd.getIpv6() != null) {
            String iiV6Name = TungstenUtils.getV6InstanceIpName(cmd.getTrafficType(), cmd.getVmType(), cmd.getVmName(),
                cmd.getNicId());
            InstanceIp instanceV6Ip = (InstanceIp) tungstenApi.getTungstenObjectByName(InstanceIp.class, null,
                iiV6Name);
            if (instanceV6Ip == null) {
                instanceV6Ip = tungstenApi.createTungstenInstanceIp(iiV6Name, cmd.getIpv6(), cmd.getVnUuid(),
                    virtualMachineInterface.getUuid());
            }
            if (instanceV6Ip == null) {
                return new TungstenAnswer(cmd, new IOException());
            }
        }
        if (!addTungstenVrouterPort(virtualMachineInterface.getUuid()
                , virtualNetwork.getUuid()
                , virtualMachine.getName()
                , project.getUuid()
                , cmd.getMac()
                , cmd.getIp()
                , cmd.getIpv6()
                , virtualMachine.getUuid()
                , cmd.getHost()
        )) {
            return new TungstenAnswer(cmd, new IOException());
        }

        return new TungstenAnswer(cmd, virtualMachine, true, null);
    }

    private boolean addTungstenVrouterPort(String vmiUuid, String networkUuid, String vmName, String projectUuid,
                                           String mac, String ip, String ipv6, String vmUuid, String host) {
        Port vPort = new Port();
        vPort.setId(vmiUuid);
        vPort.setVnId(networkUuid);
        vPort.setDisplayName(vmName);
        vPort.setVmProjectId(projectUuid);
        vPort.setMacAddress(mac);
        if (NetUtils.isValidIp4(ip)) {
            vPort.setIpAddress(ip);
        } else {
            vPort.setIpv6Address(ip);
        }

        if (ipv6 != null) {
            vPort.setIpv6Address(ipv6);
        }

        vPort.setInstanceId(vmUuid);
        vPort.setTapInterfaceName(TungstenUtils.getTapName(mac));
        return TungstenVRouterApi.addTungstenVrouterPort(host, vrouterPort, vPort);
    }

    private Answer executeRequest(SetTungstenNetworkGatewayCommand cmd) {
        Project project = (Project) tungstenApi.getTungstenProjectByFqn(cmd.getProjectFqn());
        if (project == null) {
            return new TungstenAnswer(cmd, new IOException());
        }

        LogicalRouter logicalRouter = (LogicalRouter) tungstenApi.getTungstenObjectByName(LogicalRouter.class,
            project.getQualifiedName(), cmd.getRouterName());
        if (logicalRouter == null) {
            return new TungstenAnswer(cmd, new IOException());
        }

        VirtualMachineInterface virtualMachineInterface =
            (VirtualMachineInterface) tungstenApi.getTungstenObjectByName(
            VirtualMachineInterface.class, project.getQualifiedName(),
            TungstenUtils.getNetworkGatewayVmiName(cmd.getVnId()));
        if (virtualMachineInterface == null) {
            virtualMachineInterface = (VirtualMachineInterface) tungstenApi.createTungstenGatewayVmi(
                TungstenUtils.getNetworkGatewayVmiName(cmd.getVnId()), project.getUuid(), cmd.getVnUuid());
        }
        if (virtualMachineInterface == null) {
            return new TungstenAnswer(cmd, new IOException());
        }

        InstanceIp instanceIp = (InstanceIp) tungstenApi.getTungstenObjectByName(InstanceIp.class, null,
            TungstenUtils.getNetworkGatewayIiName(cmd.getVnId()));
        if (instanceIp == null) {
            instanceIp = tungstenApi.createTungstenInstanceIp(TungstenUtils.getNetworkGatewayIiName(cmd.getVnId()),
                cmd.getVnGatewayIp(), cmd.getVnUuid(), virtualMachineInterface.getUuid());
        }
        if (instanceIp == null) {
            return new TungstenAnswer(cmd, new IOException());
        }

        logicalRouter.addVirtualMachineInterface(virtualMachineInterface);
        boolean updateLRResult = tungstenApi.updateTungstenObject(logicalRouter);
        if (!updateLRResult) {
            return new TungstenAnswer(cmd, new IOException());
        }

        return new TungstenAnswer(cmd, virtualMachineInterface, true, null);
    }

    private Answer executeRequest(GetTungstenNetworkDnsCommand cmd) {
        String dnsIpAddress = tungstenApi.getTungstenNetworkDns(cmd.getUuid(), cmd.getSubnetName());
        return new TungstenAnswer(cmd, true, dnsIpAddress);
    }

    private Answer executeRequest(GetTungstenPolicyCommand cmd) {
        Project project = (Project) tungstenApi.getTungstenProjectByFqn(cmd.getProjectFqn());
        if (project == null) {
            return new TungstenAnswer(cmd, new IOException());
        }

        ApiObjectBase apiObjectBase = tungstenApi.getTungstenObjectByName(NetworkPolicy.class,
            project.getQualifiedName(), cmd.getName());
        return new TungstenAnswer(cmd, apiObjectBase, true, null);
    }

    private Answer executeRequest(ClearTungstenNetworkGatewayCommand cmd) {
        Project project = (Project) tungstenApi.getTungstenProjectByFqn(cmd.getProjectFqn());
        if (project == null) {
            return new TungstenAnswer(cmd, new IOException());
        }

        LogicalRouter logicalRouter = (LogicalRouter) tungstenApi.getTungstenObjectByName(LogicalRouter.class,
            project.getQualifiedName(), cmd.getRouterName());
        if (logicalRouter == null) {
            return new TungstenAnswer(cmd, true, null);
        }

        boolean deleteLRResult = tungstenApi.deleteTungstenObject(logicalRouter);
        if (!deleteLRResult) {
            return new TungstenAnswer(cmd, new IOException());
        }

        VirtualMachineInterface virtualMachineInterface =
            (VirtualMachineInterface) tungstenApi.getTungstenObjectByName(
            VirtualMachineInterface.class, project.getQualifiedName(),
            TungstenUtils.getNetworkGatewayVmiName(cmd.getVnId()));

        if (virtualMachineInterface != null) {
            boolean deleteVmi = tungstenApi.deleteTungstenVmInterface(virtualMachineInterface);
            if (!deleteVmi) {
                return new TungstenAnswer(cmd, new IOException());
            }
        }

        return new TungstenAnswer(cmd, true, null);
    }

    private Answer executeRequest(CreateTungstenFloatingIpPoolCommand cmd) {
        ApiObjectBase apiObjectBase = tungstenApi.createTungstenFloatingIpPool(cmd.getNetworkUuid(), cmd.getFipName());
        if (apiObjectBase != null) {
            return new TungstenAnswer(cmd, apiObjectBase, true, null);
        } else {
            return new TungstenAnswer(cmd, new IOException());
        }
    }

    private Answer executeRequest(CreateTungstenFloatingIpCommand cmd) {
        Project project = (Project) tungstenApi.getTungstenProjectByFqn(cmd.getProjectFqn());
        if (project == null) {
            return new TungstenAnswer(cmd, new IOException());
        }

        ApiObjectBase apiObjectBase = tungstenApi.createTungstenFloatingIp(project.getUuid(), cmd.getNetworkUuid(),
            cmd.getFipName(), cmd.getName(), cmd.getPublicIp());
        if (apiObjectBase != null) {
            return new TungstenAnswer(cmd, apiObjectBase, true, null);
        } else {
            return new TungstenAnswer(cmd, new IOException());
        }
    }

    private Answer executeRequest(AssignTungstenFloatingIpCommand cmd) {
        boolean result = tungstenApi.assignTungstenFloatingIp(cmd.getNetworkUuid(), cmd.getVmiUuid(), cmd.getFipName(),
            cmd.getName(), cmd.getPrivateIp());
        if (result) {
            return new TungstenAnswer(cmd, true, null);
        } else {
            return new TungstenAnswer(cmd, new IOException());
        }
    }

    private Answer executeRequest(ReleaseTungstenFloatingIpCommand cmd) {
        boolean result = tungstenApi.releaseTungstenFloatingIp(cmd.getVnUuid(), cmd.getFipName(), cmd.getName());
        if (result) {
            return new TungstenAnswer(cmd, true, null);
        } else {
            return new TungstenAnswer(cmd, new IOException());
        }
    }

    private Answer executeRequest(GetTungstenNatIpCommand cmd, int numRetries) {
        Project project = (Project) tungstenApi.getTungstenProjectByFqn(cmd.getProjectFqn());
        String natIp = tungstenApi.getTungstenNatIp(project.getUuid(), cmd.getLogicalRouterUuid());
        if (natIp != null) {
            return new TungstenAnswer(cmd, true, natIp);
        } else {
            if (numRetries > 0) {
                return retry(cmd, --numRetries);
            } else {
                return new TungstenAnswer(cmd, new IOException());
            }
        }
    }

    private Answer executeRequest(DeleteTungstenVRouterPortCommand cmd) {
        boolean deletePort = TungstenVRouterApi.deleteTungstenVrouterPort(cmd.getHost(), vrouterPort, cmd.getPortId());
        return new TungstenAnswer(cmd, deletePort, null);
    }

    private Answer executeRequest(DeleteTungstenFloatingIpCommand cmd) {
        VirtualNetwork virtualNetwork = (VirtualNetwork) tungstenApi.getTungstenObject(VirtualNetwork.class,
            cmd.getVnUuid());
        FloatingIpPool floatingIpPool = (FloatingIpPool) tungstenApi.getTungstenObjectByName(FloatingIpPool.class,
            virtualNetwork.getQualifiedName(), cmd.getFipName());
        FloatingIp floatingIp = (FloatingIp) tungstenApi.getTungstenObjectByName(FloatingIp.class,
            floatingIpPool.getQualifiedName(), cmd.getName());
        boolean deleteFip = tungstenApi.deleteTungstenObject(floatingIp);
        return new TungstenAnswer(cmd, deleteFip, null);
    }

    private Answer executeRequest(DeleteTungstenFloatingIpPoolCommand cmd) {
        boolean deleteFip = true;
        VirtualNetwork virtualNetwork = (VirtualNetwork) tungstenApi.getTungstenObject(VirtualNetwork.class,
            cmd.getVnUuid());
        if (virtualNetwork != null) {
            FloatingIpPool floatingIpPool = (FloatingIpPool) tungstenApi.getTungstenObjectByName(FloatingIpPool.class,
                virtualNetwork.getQualifiedName(), cmd.getFipName());
            if (floatingIpPool != null) {
                deleteFip = tungstenApi.deleteTungstenObject(floatingIpPool);
            }
        }
        return new TungstenAnswer(cmd, deleteFip, null);
    }

    private Answer executeRequest(CreateTungstenNetworkPolicyCommand cmd) {
        Project project = (Project) tungstenApi.getTungstenProjectByFqn(cmd.getProjectFqn());
        ApiObjectBase apiObjectBase = tungstenApi.createOrUpdateTungstenNetworkPolicy(cmd.getName(), project.getUuid(),
            cmd.getTungstenRuleList());
        if (apiObjectBase != null) {
            return new TungstenAnswer(cmd, apiObjectBase, true, null);
        } else {
            return new TungstenAnswer(cmd, false, null);
        }
    }

    private Answer executeRequest(DeleteTungstenNetworkPolicyCommand cmd, int numRetries) {
        Project project = (Project) tungstenApi.getTungstenProjectByFqn(cmd.getProjectFqn());
        VirtualNetwork virtualNetwork = (VirtualNetwork) tungstenApi.getTungstenObject(VirtualNetwork.class,
            cmd.getNetworkUuid());
        NetworkPolicy networkPolicy = (NetworkPolicy) tungstenApi.getTungstenObjectByName(NetworkPolicy.class,
            project.getQualifiedName(), cmd.getName());
        if (networkPolicy != null) {
            virtualNetwork.removeNetworkPolicy(networkPolicy, new VirtualNetworkPolicyType(new SequenceType(-1, -1)));
            boolean updated = tungstenApi.updateTungstenObject(virtualNetwork);
            if (!updated) {
                if (numRetries > 0) {
                    return retry(cmd, --numRetries);
                } else {
                    return new TungstenAnswer(cmd, new IOException());
                }
            }

            boolean deleted = tungstenApi.deleteTungstenObject(networkPolicy);
            if (deleted)
                return new TungstenAnswer(cmd, true, "Tungsten-Fabric network policy deleted");
            else {
                if (numRetries > 0) {
                    return retry(cmd, --numRetries);
                } else {
                    return new TungstenAnswer(cmd, new IOException());
                }
            }
        }
        return new TungstenAnswer(cmd, true, "Tungsten-Fabric network policy is not exist");
    }

    private Answer executeRequest(GetTungstenFloatingIpsCommand cmd) {
        VirtualNetwork virtualNetwork = (VirtualNetwork) tungstenApi.getTungstenObject(VirtualNetwork.class,
            cmd.getVnUuid());
        List<ApiObjectBase> floatingIpList;
        if (virtualNetwork != null) {
            FloatingIpPool floatingIpPool = (FloatingIpPool) tungstenApi.getTungstenObjectByName(FloatingIpPool.class,
                virtualNetwork.getQualifiedName(), cmd.getFipName());
            floatingIpList = tungstenApi.getTungstenListObject(FloatingIp.class, floatingIpPool,null);
        } else {
            floatingIpList = new ArrayList<>();
        }
        return new TungstenAnswer(cmd, floatingIpList, true, null);
    }

    private Answer executeRequest(ApplyTungstenNetworkPolicyCommand cmd) {
        ApiObjectBase apiObjectBase;
        String networkPolicyUuid = cmd.getPolicyUuid();

        if (networkPolicyUuid == null) {
            Project project = (Project) tungstenApi.getTungstenProjectByFqn(cmd.getProjectFqn());
            ApiObjectBase networkPolicy = tungstenApi.getTungstenObjectByName(NetworkPolicy.class, project.getQualifiedName(), cmd.getNetworkPolicyName());
            if (networkPolicy == null) {
                return new TungstenAnswer(cmd, new IOException());
            }
            networkPolicyUuid = networkPolicy.getUuid();
        }

        apiObjectBase = tungstenApi.applyTungstenNetworkPolicy(networkPolicyUuid, cmd.getNetworkUuid(),
            cmd.getMajorSequence(), cmd.getMinorSequence());

        if (apiObjectBase == null) {
            return new TungstenAnswer(cmd, new IOException());
        }

        NetworkPolicy networkPolicy = (NetworkPolicy) apiObjectBase;
        List<VirtualNetwork> virtualNetworkList = tungstenApi.getNetworksFromNetworkPolicy(networkPolicy);
        TungstenNetworkPolicy tungstenModel = new TungstenNetworkPolicy(networkPolicy, virtualNetworkList);
        return new TungstenAnswer(cmd, tungstenModel, true, "Tungsten-Fabric network policy is applied");
    }

    private Answer executeRequest(GetTungstenFabricNetworkCommand cmd) {
        ApiObjectBase fabricNetwork = tungstenApi.getTungstenFabricNetwork();
        if (fabricNetwork != null) {
            return new TungstenAnswer(cmd, fabricNetwork, true, null);
        } else {
            return new TungstenAnswer(cmd, false, "Tungsten-Fabric fabric network doesn't exist");
        }
    }

    private Answer executeRequest(CreateTungstenDomainCommand cmd) {
        ApiObjectBase tungstenDomain = tungstenApi.createTungstenDomain(cmd.getTungstenDomainName(),
            cmd.getTungstenDomainUuid());
        if (tungstenDomain != null) {
            return new TungstenAnswer(cmd, tungstenDomain, true, null);
        } else {
            return new TungstenAnswer(cmd, false, null);
        }
    }

    private Answer executeRequest(CreateTungstenProjectCommand cmd) {
        ApiObjectBase tungstenProject = tungstenApi.createTungstenProject(cmd.getTungstenProjectName(),
            cmd.getTungstenProjectUuid(), cmd.getTungstenDomainUuid(), cmd.getTungstenDomainName());
        if (tungstenProject != null) {
            return new TungstenAnswer(cmd, tungstenProject, true, null);
        } else {
            return new TungstenAnswer(cmd, false, null);
        }
    }

    private Answer executeRequest(DeleteTungstenDomainCommand cmd, int numRetries) {
        boolean deleted = tungstenApi.deleteTungstenDomain(cmd.getTungstenDomainUuid());
        if (deleted) {
            return new TungstenAnswer(cmd, true, "Tungsten-Fabric domain deleted");
        } else {
            if (numRetries > 0) {
                return retry(cmd, --numRetries);
            } else {
                return new TungstenAnswer(cmd, new IOException());
            }
        }
    }

    private Answer executeRequest(DeleteTungstenProjectCommand cmd, int numRetries) {
        boolean deleted = tungstenApi.deleteTungstenProject(cmd.getTungstenProjectUuid());
        if (deleted) {
            return new TungstenAnswer(cmd, true, "Tungsten-Fabric project deleted");
        } else {
            if (numRetries > 0) {
                return retry(cmd, --numRetries);
            } else {
                return new TungstenAnswer(cmd, new IOException());
            }
        }
    }

    private Answer executeRequest(CreateTungstenNetworkLoadbalancerCommand cmd) {
        Project project = (Project) tungstenApi.getTungstenProjectByFqn(cmd.getProjectFqn());
        if (project == null) {
            return new TungstenAnswer(cmd, new IOException());
        }

        VirtualNetwork virtualNetwork = (VirtualNetwork) tungstenApi.getTungstenObject(VirtualNetwork.class,
            cmd.getNetworkUuid());
        if (virtualNetwork == null) {
            return new TungstenAnswer(cmd, new IOException());
        }

        String subnetUuid = tungstenApi.getSubnetUuid(cmd.getNetworkUuid());
        if (subnetUuid == null) {
            return new TungstenAnswer(cmd, new IOException());
        }

        // create loadbalancer vmi
        VirtualMachineInterface virtualMachineInterface =
            (VirtualMachineInterface) tungstenApi.getTungstenObjectByName(
            VirtualMachineInterface.class, project.getQualifiedName(), cmd.getLoadBalancerVmiName());
        if (virtualMachineInterface == null) {
            virtualMachineInterface = (VirtualMachineInterface) tungstenApi.createTungstenLbVmi(
                cmd.getLoadBalancerVmiName(), project.getUuid(), virtualNetwork.getUuid());
        }

        if (virtualMachineInterface == null) {
            return new TungstenAnswer(cmd, new IOException());
        }

        // create loadbalancer ii
        InstanceIp instanceIp = (InstanceIp) tungstenApi.getTungstenObjectByName(InstanceIp.class, null,
            cmd.getLoadBalancerIiName());
        if (instanceIp == null) {
            instanceIp = tungstenApi.createTungstenInstanceIp(cmd.getLoadBalancerIiName(), cmd.getPrivateIp(),
                cmd.getNetworkUuid(), virtualMachineInterface.getUuid(), subnetUuid);
        }
        if (instanceIp == null) {
            return new TungstenAnswer(cmd, new IOException());
        }

        // update loadbalancer floating ip
        // must not source nat ip or static nat ip
        // UI : don't allow add loadbalancer on source nat & static nat ip
        // check this on tungsten create load balance rule
        boolean result = tungstenApi.assignTungstenFloatingIp(cmd.getPublicNetworkUuid(),
            virtualMachineInterface.getUuid(), cmd.getFipName(), cmd.getFiName(), cmd.getPrivateIp());
        if (!result) {
            return new TungstenAnswer(cmd, new IOException());
        }

        Loadbalancer loadbalancer = (Loadbalancer) tungstenApi.getTungstenObjectByName(Loadbalancer.class,
            project.getQualifiedName(), cmd.getLoadBalancerName());
        if (loadbalancer == null) {
            loadbalancer = (Loadbalancer) tungstenApi.createTungstenLoadbalancer(project.getUuid(),
                cmd.getLoadBalancerName(), virtualMachineInterface.getUuid(), subnetUuid, cmd.getPrivateIp());
        }

        if (loadbalancer == null) {
            return new TungstenAnswer(cmd, new IOException());
        }

        LoadbalancerListener loadbalancerListener = (LoadbalancerListener) tungstenApi.getTungstenObjectByName(
            LoadbalancerListener.class, project.getQualifiedName(), cmd.getLoadBalancerListenerName());
        if (loadbalancerListener == null) {
            loadbalancerListener = (LoadbalancerListener) tungstenApi.createTungstenLoadbalancerListener(
                project.getUuid(), loadbalancer.getUuid(), cmd.getLoadBalancerListenerName(), cmd.getProtocol(),
                cmd.getSrcPort());
        }

        if (loadbalancerListener == null) {
            return new TungstenAnswer(cmd, new IOException());
        }

        LoadbalancerHealthmonitor loadbalancerHealthmonitor =
            (LoadbalancerHealthmonitor) tungstenApi.getTungstenObjectByName(
            LoadbalancerHealthmonitor.class, project.getQualifiedName(), cmd.getLoadBalancerHealthMonitorName());
        if (loadbalancerHealthmonitor == null) {
            loadbalancerHealthmonitor =
                (LoadbalancerHealthmonitor) tungstenApi.createTungstenLoadbalancerHealthMonitor(
                project.getUuid(), cmd.getLoadBalancerHealthMonitorName(), cmd.getMonitorType(), cmd.getMaxRetries(),
                cmd.getDelay(), cmd.getTimeout(), cmd.getHttpMethod(), cmd.getUrlPath(), cmd.getExpectedCodes());
        }

        if (loadbalancerHealthmonitor == null) {
            return new TungstenAnswer(cmd, new IOException());
        }

        LoadbalancerPool loadbalancerPool = createLoadBalancerPool(cmd.getLoadBalancerPoolName(),
                cmd.getLoadBalancerMethod(),
                cmd.getProtocol(), project.getUuid(), project.getQualifiedName(), loadbalancerListener.getUuid(),
                 loadbalancerHealthmonitor.getUuid());

        createLoadBalancerMember(cmd.getListMember(), loadbalancerPool, subnetUuid);

        return new TungstenAnswer(cmd, loadbalancer, true, null);
    }

    private LoadbalancerPool createLoadBalancerPool(String poolName, String method, String protocol, String projectUuid,
                                                    List<String> projectQualifiedName,
                                                    String loadbalancerListenerUuid,
                                                    String loadbalancerHealthmonitorUuid) {
        LoadbalancerPool loadbalancerPool = (LoadbalancerPool) tungstenApi.getTungstenObjectByName(
                LoadbalancerPool.class, projectQualifiedName, poolName);

        if (loadbalancerPool == null) {
            loadbalancerPool = (LoadbalancerPool) tungstenApi.createTungstenLoadbalancerPool(projectUuid,
                    loadbalancerListenerUuid, loadbalancerHealthmonitorUuid, poolName,
                    method, protocol);
        }

        if (loadbalancerPool == null) {
            throw new CloudRuntimeException("Can not create load balancer pool");
        }

        return loadbalancerPool;
    }

    private void createLoadBalancerMember(List<TungstenLoadBalancerMember> members, LoadbalancerPool loadbalancerPool, String subnetUuid) {
        for (TungstenLoadBalancerMember member : members) {
            LoadbalancerMember loadbalancerMember = (LoadbalancerMember) tungstenApi.getTungstenObjectByName(
                    LoadbalancerMember.class, loadbalancerPool.getQualifiedName(), member.getName());
            if (loadbalancerMember == null) {
                loadbalancerMember = (LoadbalancerMember) tungstenApi.createTungstenLoadbalancerMember(
                        loadbalancerPool.getUuid(), member.getName(), member.getIpAddress(), subnetUuid, member.getPort(),
                        member.getWeight());
            }

            if (loadbalancerMember == null) {
                throw new CloudRuntimeException("Can not create load balancer members");
            }
        }
    }

    private Answer executeRequest(UpdateLoadBalancerServiceInstanceCommand cmd, int numRetries) {
        boolean result = tungstenApi.updateLBServiceInstanceFatFlow(cmd.getPublicNetworkUuid(),
            cmd.getFloatingPoolName(), cmd.getFloatingIpName());

        if (result) {
            return new TungstenAnswer(cmd, true, null);
        } else {
            if (numRetries > 0) {
                return retry(cmd, --numRetries);
            } else {
                return new TungstenAnswer(cmd, new IOException());
            }
        }
    }

    private Answer executeRequest(DeleteTungstenLoadBalancerCommand cmd, int numRetries) {
        boolean result;
        Project project = (Project) tungstenApi.getTungstenProjectByFqn(cmd.getProjectFqn());
        Loadbalancer loadbalancer = (Loadbalancer) tungstenApi.getTungstenObjectByName(Loadbalancer.class,
            project.getQualifiedName(), cmd.getLoadBalancerName());
        if (loadbalancer != null) {
            result= deleteLoadBalancerListener(loadbalancer);

            // delete load balancer
            result = result && tungstenApi.deleteTungstenObject(loadbalancer);

            // release floating ip
            result = result && tungstenApi.releaseTungstenFloatingIp(cmd.getPublicNetworkUuid(), cmd.getFipName(),
                cmd.getFiName());

            // delete load balancer vmi
            VirtualMachineInterface virtualMachineInterface =
                (VirtualMachineInterface) tungstenApi.getTungstenObjectByName(
                VirtualMachineInterface.class, project.getQualifiedName(), cmd.getLoadBalancerVmiName());

            result = result && tungstenApi.deleteTungstenVmInterface(virtualMachineInterface);

            // delete load balancer health monitor
            LoadbalancerHealthmonitor loadbalancerHealthmonitor =
                (LoadbalancerHealthmonitor) tungstenApi.getTungstenObjectByName(
                LoadbalancerHealthmonitor.class, project.getQualifiedName(), cmd.getLoadBalancerHealthMonitorName());
            result = result && tungstenApi.deleteTungstenObject(loadbalancerHealthmonitor);

            if (result)
                return new TungstenAnswer(cmd, true, "Tungsten-Fabric loadbalancer deleted");
            else {
                if (numRetries > 0) {
                    return retry(cmd, --numRetries);
                } else {
                    return new TungstenAnswer(cmd, new IOException());
                }
            }
        }
        return new TungstenAnswer(cmd, true, "Tungsten-Fabric loadbalancer is not exist");
    }

    private boolean deleteLoadBalancerListener(Loadbalancer loadBalancer) {
        boolean result = true;
        List<ObjectReference<ApiPropertyBase>> listLoadBalancerListener =
                loadBalancer.getLoadbalancerListenerBackRefs();
        if (listLoadBalancerListener != null && !listLoadBalancerListener.isEmpty()) {
            for (ObjectReference<ApiPropertyBase> listener : listLoadBalancerListener) {
                LoadbalancerListener loadbalancerListener = (LoadbalancerListener) tungstenApi.getTungstenObject(
                        LoadbalancerListener.class, listener.getUuid());
                result = result && deleteLoadBalancerListener(loadbalancerListener);
            }
        }
        return result;
    }

    private Answer executeRequest(DeleteTungstenLoadBalancerListenerCommand cmd, int numRetries) {
        boolean result;
        Project project = (Project) tungstenApi.getTungstenProjectByFqn(cmd.getProjectFqn());
        LoadbalancerListener loadbalancerListener = (LoadbalancerListener) tungstenApi.getTungstenObjectByName(
            LoadbalancerListener.class, project.getQualifiedName(), cmd.getLoadBalancerListenerName());
        if (loadbalancerListener != null) {
            result = deleteLoadBalancerListener(loadbalancerListener);
            if (result)
                return new TungstenAnswer(cmd, true, "Tungsten-Fabric loadbalancer listener deleted");
            else {
                if (numRetries > 0) {
                    return retry(cmd, --numRetries);
                } else {
                    return new TungstenAnswer(cmd, new IOException());
                }
            }
        }

        return new TungstenAnswer(cmd, true, "Tungsten-Fabric loadbalancer listener is not exist");
    }

    private Answer executeRequest(UpdateTungstenLoadBalancerPoolCommand cmd, int numRetries) {
        Project project = (Project) tungstenApi.getTungstenProjectByFqn(cmd.getProjectFqn());
        boolean result = tungstenApi.updateLoadBalancerPool(project.getUuid(), cmd.getLbPoolName(), cmd.getLbMethod(),
            cmd.getLbSessionPersistence(), cmd.getLbPersistenceCookieName(), cmd.getLbProtocol(), cmd.isLbStatsEnable(),
            cmd.getLbStatsPort(), cmd.getLbStatsUri(), cmd.getLbStatsAuth());
        if (result)
            return new TungstenAnswer(cmd, true, "Tungsten-Fabric loadbalancer pool updated");
        else {
            if (numRetries > 0) {
                return retry(cmd, --numRetries);
            } else {
                return new TungstenAnswer(cmd, new IOException());
            }
        }
    }

    private Answer executeRequest(UpdateTungstenLoadBalancerListenerCommand cmd, int numRetries) {
        Project project = (Project) tungstenApi.getTungstenProjectByFqn(cmd.getProjectFqn());
        boolean result = tungstenApi.updateLoadBalancerListener(project.getUuid(), cmd.getListenerName(),
            cmd.getProtocol(), cmd.getPort(), cmd.getUrl());
        if (result)
            return new TungstenAnswer(cmd, true, "Tungsten-Fabric loadbalancer listener updated");
        else {
            if (numRetries > 0) {
                return retry(cmd, --numRetries);
            } else {
                return new TungstenAnswer(cmd, new IOException());
            }
        }
    }

    private Answer executeRequest(UpdateTungstenLoadBalancerHealthMonitorCommand cmd, int numRetries) {
        Project project = (Project) tungstenApi.getTungstenProjectByFqn(cmd.getProjectFqn());
        boolean result = tungstenApi.updateLoadBalancerHealthMonitor(project.getUuid(), cmd.getHealthMonitorName()
        , cmd.getType(), cmd.getRetry(), cmd.getTimeout(), cmd.getInterval(), cmd.getHttpMethod(), cmd.getExpectedCode(), cmd.getUrlPath());
        if (result)
            return new TungstenAnswer(cmd, true, "Tungsten-Fabric loadbalancer health monitor updated");
        else {
            if (numRetries > 0) {
                return retry(cmd, --numRetries);
            } else {
                return new TungstenAnswer(cmd, new IOException());
            }
        }
    }

    private Answer executeRequest(UpdateTungstenLoadBalancerMemberCommand cmd, int numRetries) {
        Project project = (Project) tungstenApi.getTungstenProjectByFqn(cmd.getProjectFqn());
        String subnetUuid = tungstenApi.getSubnetUuid(cmd.getNetworkUuid());
        if (subnetUuid == null) {
            return new TungstenAnswer(cmd, new IOException());
        }

        boolean result = tungstenApi.updateLoadBalancerMember(project.getUuid(), cmd.getLbPoolName(),
            cmd.getListTungstenLoadBalancerMember(), subnetUuid);

        if (result)
            return new TungstenAnswer(cmd, true, "Tungsten-Fabric loadbalancer member updated");
        else {
            if (numRetries > 0) {
                return retry(cmd, --numRetries);
            } else {
                return new TungstenAnswer(cmd, new IOException());
            }
        }
    }

    private boolean deleteLoadBalancerListener(LoadbalancerListener loadbalancerListener) {
        boolean result = true;
        List<ObjectReference<ApiPropertyBase>> listPool = loadbalancerListener.getLoadbalancerPoolBackRefs();
        if (listPool != null && !listPool.isEmpty()) {
            for (ObjectReference<ApiPropertyBase> pool : listPool) {
                LoadbalancerPool loadbalancerPool = (LoadbalancerPool) tungstenApi.getTungstenObject(
                    LoadbalancerPool.class, pool.getUuid());
                List<ObjectReference<ApiPropertyBase>> listMember = loadbalancerPool.getLoadbalancerMembers();
                if (listMember != null && !listMember.isEmpty()) {
                    for (ObjectReference<ApiPropertyBase> member : listMember) {
                        LoadbalancerMember loadbalancerMember = (LoadbalancerMember) tungstenApi.getTungstenObject(
                            LoadbalancerMember.class, member.getUuid());
                        result = result && tungstenApi.deleteTungstenObject(loadbalancerMember);
                    }
                }
                result = result && tungstenApi.deleteTungstenObject(loadbalancerPool);
            }
        }

        result = result && tungstenApi.deleteTungstenObject(loadbalancerListener);
        return result;
    }

    private Answer executeRequest(GetTungstenLoadBalancerCommand cmd, int numRetries) {
        Project project = (Project) tungstenApi.getTungstenProjectByFqn(cmd.getProjectFqn());
        ApiObjectBase apiObjectBase = tungstenApi.getTungstenObjectByName(Loadbalancer.class,
            project.getQualifiedName(), cmd.getLbName());
        if (apiObjectBase != null) {
            return new TungstenAnswer(cmd, apiObjectBase, true, "");
        } else {
            if (numRetries > 0) {
                return retry(cmd, --numRetries);
            } else {
                return new TungstenAnswer(cmd, new IOException());
            }
        }
    }

    private Answer executeRequest(ApplyTungstenPortForwardingCommand cmd, int numRetries) {
        boolean result = tungstenApi.applyTungstenPortForwarding(cmd.isAdd(), cmd.getPublicNetworkUuid(),
            cmd.getFloatingIpPoolName(), cmd.getFloatingIpName(), cmd.getVmiUuid(), cmd.getProtocol(),
            cmd.getPublicPort(), cmd.getPrivatePort());

        if (result)
            return new TungstenAnswer(cmd, true, "Tungsten-Fabric port forwarding floating ip enabled");
        else {
            if (numRetries > 0) {
                return retry(cmd, --numRetries);
            } else {
                return new TungstenAnswer(cmd, new IOException());
            }
        }
    }

    private Answer executeRequest(DeleteTungstenObjectCommand cmd, int numRetries) {
        boolean result = tungstenApi.deleteTungstenObject(cmd.getApiObjectBase());

        if (result)
            return new TungstenAnswer(cmd, true, "Deleted Tungsten-Fabric object");
        else {
            if (numRetries > 0) {
                return retry(cmd, --numRetries);
            } else {
                return new TungstenAnswer(cmd, new IOException());
            }
        }
    }

    private Answer executeRequest(AddTungstenNetworkSubnetCommand cmd, int numRetries) {
        boolean result = tungstenApi.addTungstenNetworkSubnetCommand(cmd.getNetworkUuid(), cmd.getIpPrefix(),
            cmd.getIpPrefixLen(), cmd.getGateway(), cmd.isDhcpEnable(), cmd.getDnsServer(), cmd.getAllocationStart(),
            cmd.getAllocationEnd(), cmd.isIpFromStart(), cmd.getSubnetName());

        if (result)
            return new TungstenAnswer(cmd, true, "Tungsten-Fabric network subnet is added");
        else {
            if (numRetries > 0) {
                return retry(cmd, --numRetries);
            } else {
                return new TungstenAnswer(cmd, new IOException());
            }
        }
    }

    private Answer executeRequest(RemoveTungstenNetworkSubnetCommand cmd, int numRetries) {
        boolean result = tungstenApi.removeTungstenNetworkSubnetCommand(cmd.getNetworkUuid(), cmd.getSubnetName());

        if (result)
            return new TungstenAnswer(cmd, true, "Tungsten-Fabric network subnet is deleted");
        else {
            if (numRetries > 0) {
                return retry(cmd, --numRetries);
            } else {
                return new TungstenAnswer(cmd, new IOException());
            }
        }
    }

    private Answer executeRequest(CreateTungstenSecurityGroupCommand cmd, int numRetries) {
        ApiObjectBase apiObjectBase = tungstenApi.createTungstenSecurityGroup(cmd.getSecurityGroupUuid(),
            cmd.getSecurityGroupName(), cmd.getSecurityGroupDescription(), cmd.getProjectFqn());
        if (apiObjectBase != null) {
            return new TungstenAnswer(cmd, apiObjectBase, true, "Tungsten-Fabric security group created");
        } else {
            if (numRetries > 0) {
                return retry(cmd, --numRetries);
            } else {
                return new TungstenAnswer(cmd, new IOException());
            }
        }
    }

    private Answer executeRequest(DeleteTungstenSecurityGroupCommand cmd, int numRetries) {
        boolean result = tungstenApi.deleteTungstenObject(SecurityGroup.class, cmd.getTungstenSecurityGroupUuid());
        if (result)
            return new TungstenAnswer(cmd, true, "Tungsten-Fabric security group deleted");
        else {
            if (numRetries > 0) {
                return retry(cmd, --numRetries);
            } else {
                return new TungstenAnswer(cmd, new IOException());
            }
        }
    }

    private Answer executeRequest(GetTungstenSecurityGroupCommand cmd) {
        ApiObjectBase apiObjectBase = tungstenApi.getTungstenObject(SecurityGroup.class,
            cmd.getTungstenSecurityGroupUuid());
        return new TungstenAnswer(cmd, apiObjectBase, true, "Get Tungsten-Fabric security group");
    }

    private Answer executeRequest(AddTungstenSecurityGroupRuleCommand cmd, int numRetries) {
        boolean result = tungstenApi.addTungstenSecurityGroupRule(cmd.getTungstenSecurityGroupUuid(),
            cmd.getTungstenGroupRuleUuid(), cmd.getSecurityGroupRuleType(), cmd.getStartPort(), cmd.getEndPort(),
            cmd.getTarget(), cmd.getEtherType(), cmd.getProtocol());
        if (result)
            return new TungstenAnswer(cmd, true, "Tungsten-Fabric security group rule added");
        else {
            if (numRetries > 0) {
                return retry(cmd, --numRetries);
            } else {
                return new TungstenAnswer(cmd, new IOException());
            }
        }
    }

    private Answer executeRequest(AddTungstenVmToSecurityGroupCommand cmd, int numRetries) {
        boolean result = tungstenApi.addInstanceToSecurityGroup(cmd.getNicUuid(), cmd.getSecurityGroupUuidList());
        if (result)
            return new TungstenAnswer(cmd, true, "Tungsten-Fabric instance added to security groups");
        else {
            if (numRetries > 0) {
                return retry(cmd, --numRetries);
            } else {
                return new TungstenAnswer(cmd, new IOException());
            }
        }
    }

    private Answer executeRequest(RemoveTungstenVmFromSecurityGroupCommand cmd, int numRetries) {
        boolean result = tungstenApi.removeInstanceFromSecurityGroup(cmd.getNicUuid(), cmd.getSecurityGroupUuidList());
        if (result)
            return new TungstenAnswer(cmd, true, "removed Tungsten-Fabric instance from security groups");
        else {
            if (numRetries > 0) {
                return retry(cmd, --numRetries);
            } else {
                return new TungstenAnswer(cmd, new IOException());
            }
        }
    }

    private Answer executeRequest(RemoveTungstenSecurityGroupRuleCommand cmd, int numRetries) {
        boolean tungstenSecurityGroupRuleRemoved = tungstenApi.removeTungstenSecurityGroupRule(
            cmd.getSecurityGroupUuid(), cmd.getSecurityGroupRuleUuid());
        if (tungstenSecurityGroupRuleRemoved)
            return new TungstenAnswer(cmd, true, "Tungsten-Fabric security group rule removed");
        else {
            if (numRetries > 0) {
                return retry(cmd, --numRetries);
            } else {
                return new TungstenAnswer(cmd, new IOException());
            }
        }
    }

    private Answer executeRequest(AddTungstenSecondaryIpAddressCommand cmd, int numRetries) {
        boolean addSecondaryIpAddress = tungstenApi.addSecondaryIpAddress(cmd.getNetworkUuid(), cmd.getNicUuid(),
            cmd.getIiName(), cmd.getAddress());
        if (addSecondaryIpAddress)
            return new TungstenAnswer(cmd, true, "Tungsten-Fabric secondary ip is added");
        else {
            if (numRetries > 0) {
                return retry(cmd, --numRetries);
            } else {
                return new TungstenAnswer(cmd, new IOException());
            }
        }
    }

    private Answer executeRequest(RemoveTungstenSecondaryIpAddressCommand cmd, int numRetries) {
        boolean removeSecondaryIpAddress = tungstenApi.removeSecondaryIpAddress(cmd.getIiName());
        if (removeSecondaryIpAddress)
            return new TungstenAnswer(cmd, true, "Tungsten-Fabric secondary ip is removed");
        else {
            if (numRetries > 0) {
                return retry(cmd, --numRetries);
            } else {
                return new TungstenAnswer(cmd, new IOException());
            }
        }
    }

    private Answer executeRequest(CreateTungstenPolicyCommand cmd, int numRetries) {
        Project project = (Project) tungstenApi.getTungstenProjectByFqn(cmd.getProjectFqn());
        if (project == null) {
            return new TungstenAnswer(cmd, new IOException());
        }

        ApiObjectBase apiObjectBase = tungstenApi.createTungstenPolicy(cmd.getUuid(), cmd.getName(), project.getUuid());

        if (apiObjectBase != null) {
            NetworkPolicy networkPolicy = (NetworkPolicy) apiObjectBase;
            List<VirtualNetwork> virtualNetworkList = tungstenApi.getNetworksFromNetworkPolicy(networkPolicy);
            TungstenModel tungstenModel = new TungstenNetworkPolicy(networkPolicy, virtualNetworkList);
            return new TungstenAnswer(cmd, tungstenModel, true, "Tungsten-Fabric policy is created");
        } else {
            if (numRetries > 0) {
                return retry(cmd, --numRetries);
            } else {
                return new TungstenAnswer(cmd, new IOException());
            }
        }
    }

    private Answer executeRequest(AddTungstenPolicyRuleCommand cmd, int numRetries) {
        ApiObjectBase apiObjectBase = tungstenApi.addTungstenPolicyRule(cmd.getUuid(), cmd.getPolicyUuid(),
            cmd.getAction(), cmd.getProtocol(), cmd.getDirection(), cmd.getSrcNetwork(), cmd.getSrcIpPrefix(),
            cmd.getSrcIpPrefixLen(), cmd.getSrcStartPort(), cmd.getSrcEndPort(), cmd.getDestNetwork(),
            cmd.getDestIpPrefix(), cmd.getDestIpPrefixLen(), cmd.getDestStartPort(), cmd.getDestEndPort());

        if (apiObjectBase != null)
            return new TungstenAnswer(cmd, apiObjectBase, true, "Tungsten-Fabric policy rule is added");
        else {
            if (numRetries > 0) {
                return retry(cmd, --numRetries);
            } else {
                return new TungstenAnswer(cmd, new IOException());
            }
        }
    }

    private Answer executeRequest(RemoveTungstenPolicyRuleCommand cmd, int numRetries) {
        ApiObjectBase apiObjectBase = tungstenApi.removeTungstenNetworkPolicyRule(cmd.getPolicyUuid(),
            cmd.getRuleUuid());

        if (apiObjectBase != null) {
            NetworkPolicy networkPolicy = (NetworkPolicy) apiObjectBase;
            List<VirtualNetwork> virtualNetworkList = tungstenApi.getNetworksFromNetworkPolicy(networkPolicy);
            TungstenModel tungstenModel = new TungstenNetworkPolicy(networkPolicy, virtualNetworkList);
            return new TungstenAnswer(cmd, tungstenModel, true, "Tungsten-Fabric policy rule is removed");
        } else {
            if (numRetries > 0) {
                return retry(cmd, --numRetries);
            } else {
                return new TungstenAnswer(cmd, new IOException());
            }
        }
    }

    private Answer executeRequest(DeleteTungstenPolicyCommand cmd, int numRetries) {
        ApiObjectBase apiObjectBase = tungstenApi.getTungstenObject(NetworkPolicy.class, cmd.getPolicyUuid());
        if (apiObjectBase == null) {
            return new TungstenAnswer(cmd, true, "Tungsten-Fabric policy is not founded");
        }

        boolean deleted = tungstenApi.deleteTungstenObject(apiObjectBase);

        if (deleted)
            return new TungstenAnswer(cmd, true, "Tungsten-Fabric policy is delete");
        else {
            if (numRetries > 0) {
                return retry(cmd, --numRetries);
            } else {
                return new TungstenAnswer(cmd, new IOException());
            }
        }
    }

    private Answer executeRequest(ListTungstenPolicyRuleCommand cmd, int numRetries) {
        ApiObjectBase apiObjectBase = tungstenApi.getTungstenObject(NetworkPolicy.class, cmd.getPolicyUuid());

        if (apiObjectBase != null)
            return new TungstenAnswer(cmd, apiObjectBase, true, "Tungsten-Fabric policy rule is listed");
        else {
            if (numRetries > 0) {
                return retry(cmd, --numRetries);
            } else {
                return new TungstenAnswer(cmd, new IOException());
            }
        }
    }

    private Answer executeRequest(ListTungstenPolicyCommand cmd, int numRetries) {
        Project project = (Project) tungstenApi.getTungstenProjectByFqn(cmd.getProjectFqn());
        if (project == null) {
            return new TungstenAnswer(cmd, new IOException());
        }

        List<? extends ApiObjectBase> apiObjectBaseList;
        if (cmd.getPolicyName() != null) {
            apiObjectBaseList = tungstenApi.listTungstenAddressPolicy(project.getUuid(), cmd.getPolicyName());
        } else if (cmd.getNetworkUuid() != null) {
            apiObjectBaseList = tungstenApi.listTungstenNetworkPolicy(cmd.getNetworkUuid(), cmd.getPolicyUuid());
        } else {
            apiObjectBaseList = tungstenApi.listTungstenPolicy(project.getUuid(), cmd.getPolicyUuid());
        }

        if (apiObjectBaseList != null) {
            List<TungstenModel> tungstenModelList = new ArrayList<>();
            for(ApiObjectBase apiObjectBase : apiObjectBaseList) {
                NetworkPolicy networkPolicy = (NetworkPolicy) apiObjectBase;
                List<VirtualNetwork> virtualNetworkList = tungstenApi.getNetworksFromNetworkPolicy(networkPolicy);
                TungstenModel tungstenModel = new TungstenNetworkPolicy(networkPolicy, virtualNetworkList);
                tungstenModelList.add(tungstenModel);
            }
            return new TungstenAnswer(cmd, true, tungstenModelList, "Tungsten-Fabric policy is listed");
        } else {
            if (numRetries > 0) {
                return retry(cmd, --numRetries);
            } else {
                return new TungstenAnswer(cmd, new IOException());
            }
        }
    }

    private Answer executeRequest(ListTungstenNetworkCommand cmd, int numRetries) {
        Project project = (Project) tungstenApi.getTungstenProjectByFqn(cmd.getProjectFqn());
        if (project == null) {
            return new TungstenAnswer(cmd, new IOException());
        }

        List<ApiObjectBase> apiObjectBaseList = tungstenApi.listTungstenNetwork(project.getUuid(),
            cmd.getNetworkUuid());

        if (apiObjectBaseList != null)
            return new TungstenAnswer(cmd, apiObjectBaseList, true, "Tungsten-Fabric network is listed");
        else {
            if (numRetries > 0) {
                return retry(cmd, --numRetries);
            } else {
                return new TungstenAnswer(cmd, new IOException());
            }
        }
    }

    private Answer executeRequest(ListTungstenVmCommand cmd, int numRetries) {
        Project project = (Project) tungstenApi.getTungstenProjectByFqn(cmd.getProjectFqn());
        if (project == null) {
            return new TungstenAnswer(cmd, new IOException());
        }

        List<ApiObjectBase> apiObjectBaseList = tungstenApi.listTungstenVm(project.getUuid(), cmd.getVmUuid());

        if (apiObjectBaseList != null)
            return new TungstenAnswer(cmd, apiObjectBaseList, true, "Tungsten-Fabric vm is listed");
        else {
            if (numRetries > 0) {
                return retry(cmd, --numRetries);
            } else {
                return new TungstenAnswer(cmd, new IOException());
            }
        }
    }

    private Answer executeRequest(ListTungstenNicCommand cmd, int numRetries) {
        Project project = (Project) tungstenApi.getTungstenProjectByFqn(cmd.getProjectFqn());
        if (project == null) {
            return new TungstenAnswer(cmd, new IOException());
        }

        List<ApiObjectBase> apiObjectBaseList = tungstenApi.listTungstenNic(project.getUuid(),
            cmd.getNicUuid());

        if (apiObjectBaseList != null)
            return new TungstenAnswer(cmd, apiObjectBaseList, true, "Tungsten-Fabric nic is listed");
        else {
            if (numRetries > 0) {
                return retry(cmd, --numRetries);
            } else {
                return new TungstenAnswer(cmd, new IOException());
            }
        }
    }

    private Answer executeRequest(CreateTungstenTagCommand cmd, int numRetries) {
        ApiObjectBase apiObjectBase = tungstenApi.createTungstenTag(cmd.getUuid(), cmd.getTagType(), cmd.getTagValue(), null);

        if (apiObjectBase != null) {
            Tag tag = (Tag) apiObjectBase;
            List<VirtualNetwork> virtualNetworkList = tungstenApi.getBackRefFromVirtualNetwork(VirtualNetwork.class, tag.getVirtualNetworkBackRefs());
            List<VirtualMachine> virtualMachineList = tungstenApi.getBackRefFromVirtualMachine(VirtualMachine.class, tag.getVirtualMachineBackRefs());
            List<VirtualMachineInterface> virtualMachineInterfaceList = tungstenApi.getBackRefFromVirtualMachineInterface(VirtualMachineInterface.class, tag.getVirtualMachineInterfaceBackRefs());
            List<NetworkPolicy> networkPolicyList = tungstenApi.getBackRefFromNetworkPolicy(NetworkPolicy.class, tag.getNetworkPolicyBackRefs());
            List<ApplicationPolicySet> applicationPolicySetList = tungstenApi.getBackRefFromApplicationPolicySet(ApplicationPolicySet.class, tag.getApplicationPolicySetBackRefs());
            TungstenModel tungstenModel = new TungstenTag((Tag) apiObjectBase, virtualNetworkList, virtualMachineList, virtualMachineInterfaceList, networkPolicyList, applicationPolicySetList);
            return new TungstenAnswer(cmd, tungstenModel, true, "Tungsten-Fabric tag is created");
        } else {
            if (numRetries > 0) {
                return retry(cmd, --numRetries);
            } else {
                return new TungstenAnswer(cmd, new IOException());
            }
        }
    }

    private Answer executeRequest(CreateTungstenTagTypeCommand cmd, int numRetries) {
        ApiObjectBase apiObjectBase = tungstenApi.createTungstenTagType(cmd.getUuid(), cmd.getName());

        if (apiObjectBase != null)
            return new TungstenAnswer(cmd, apiObjectBase, true, "Tungsten-Fabric tag type is created");
        else {
            if (numRetries > 0) {
                return retry(cmd, --numRetries);
            } else {
                return new TungstenAnswer(cmd, new IOException());
            }
        }
    }

    private Answer executeRequest(DeleteTungstenTagCommand cmd, int numRetries) {
        ApiObjectBase apiObjectBase = tungstenApi.getTungstenObject(Tag.class, cmd.getTagUuid());
        if (apiObjectBase == null) {
            return new TungstenAnswer(cmd, true, "Tungsten-Fabric tag is not founded");
        }

        boolean deleted = tungstenApi.deleteTungstenObject(apiObjectBase);

        if (deleted)
            return new TungstenAnswer(cmd, true, "Tungsten-Fabric tag is deleted");
        else {
            if (numRetries > 0) {
                return retry(cmd, --numRetries);
            } else {
                return new TungstenAnswer(cmd, new IOException());
            }
        }
    }

    private Answer executeRequest(DeleteTungstenTagTypeCommand cmd, int numRetries) {
        ApiObjectBase apiObjectBase = tungstenApi.getTungstenObject(TagType.class, cmd.getTagTypeUuid());
        if (apiObjectBase == null) {
            return new TungstenAnswer(cmd, true, "Tungsten-Fabric tag type is not founded");
        }

        boolean deleted = tungstenApi.deleteTungstenObject(apiObjectBase);

        if (deleted)
            return new TungstenAnswer(cmd, true, "Tungsten-Fabric tag type is deleted");
        else {
            if (numRetries > 0) {
                return retry(cmd, --numRetries);
            } else {
                return new TungstenAnswer(cmd, new IOException());
            }
        }
    }

    private Answer executeRequest(ListTungstenTagCommand cmd, int numRetries) {
        List<? extends ApiObjectBase> apiObjectBaseList = tungstenApi.listTungstenTag(cmd.getNetworkUuid(),
            cmd.getVmUuid(), cmd.getNicUuid(), cmd.getPolicyUuid(), cmd.getApplicationPolicySetUuid(), cmd.getTagUuid());

        if (apiObjectBaseList != null) {
            List<TungstenModel> tungstenModelList = new ArrayList<>();
            for (ApiObjectBase apiObjectBase : apiObjectBaseList) {
                Tag tag = (Tag) apiObjectBase;
                List<VirtualNetwork> virtualNetworkList = tungstenApi.getBackRefFromVirtualNetwork(VirtualNetwork.class, tag.getVirtualNetworkBackRefs());
                List<VirtualMachine> virtualMachineList = tungstenApi.getBackRefFromVirtualMachine(VirtualMachine.class, tag.getVirtualMachineBackRefs());
                List<VirtualMachineInterface> virtualMachineInterfaceList = tungstenApi.getBackRefFromVirtualMachineInterface(VirtualMachineInterface.class, tag.getVirtualMachineInterfaceBackRefs());
                List<NetworkPolicy> networkPolicyList = tungstenApi.getBackRefFromNetworkPolicy(NetworkPolicy.class, tag.getNetworkPolicyBackRefs());
                List<ApplicationPolicySet> applicationPolicySetList = tungstenApi.getBackRefFromApplicationPolicySet(ApplicationPolicySet.class, tag.getApplicationPolicySetBackRefs());
                TungstenModel tungstenModel = new TungstenTag((Tag) apiObjectBase, virtualNetworkList, virtualMachineList, virtualMachineInterfaceList, networkPolicyList, applicationPolicySetList);
                tungstenModelList.add(tungstenModel);
            }
            return new TungstenAnswer(cmd, true, tungstenModelList, "Tungsten-Fabric tag list is got");
        } else {
            if (numRetries > 0) {
                return retry(cmd, --numRetries);
            } else {
                return new TungstenAnswer(cmd, new IOException());
            }
        }
    }

    private Answer executeRequest(ListTungstenTagTypeCommand cmd, int numRetries) {
        List<ApiObjectBase> apiObjectBaseList = tungstenApi.listTungstenTagType(cmd.getTagTypeUuid());

        if (apiObjectBaseList != null)
            return new TungstenAnswer(cmd, apiObjectBaseList, true, "Tungsten-Fabric tag type list is got");
        else {
            if (numRetries > 0) {
                return retry(cmd, --numRetries);
            } else {
                return new TungstenAnswer(cmd, new IOException());
            }
        }
    }

    private Answer executeRequest(ApplyTungstenTagCommand cmd, int numRetries) {
        boolean applied = true;

        if (cmd.getNetworkUuids() != null) {
            applied = tungstenApi.applyTungstenNetworkTag(cmd.getNetworkUuids(), cmd.getTagUuid());
        }

        if (cmd.getVmUuids() != null) {
            applied = applied && tungstenApi.applyTungstenVmTag(cmd.getVmUuids(), cmd.getTagUuid());
        }

        if (cmd.getNicUuids() != null) {
            applied = applied && tungstenApi.applyTungstenNicTag(cmd.getNicUuids(), cmd.getTagUuid());
        }

        if (cmd.getPolicyUuid() != null) {
            applied = applied && tungstenApi.applyTungstenPolicyTag(cmd.getPolicyUuid(), cmd.getTagUuid());
        }

        if (cmd.getApplicationPolicySetUuid() != null) {
            applied = applied && tungstenApi.applyTungstenApplicationPolicySetTag(cmd.getApplicationPolicySetUuid(), cmd.getTagUuid());
        }

        if (applied) {
            Tag tag = (Tag) tungstenApi.getTungstenObject(Tag.class, cmd.getTagUuid());
            List<VirtualNetwork> virtualNetworkList = tungstenApi.getBackRefFromVirtualNetwork(VirtualNetwork.class, tag.getVirtualNetworkBackRefs());
            List<VirtualMachine> virtualMachineList = tungstenApi.getBackRefFromVirtualMachine(VirtualMachine.class, tag.getVirtualMachineBackRefs());
            List<VirtualMachineInterface> virtualMachineInterfaceList = tungstenApi.getBackRefFromVirtualMachineInterface(VirtualMachineInterface.class, tag.getVirtualMachineInterfaceBackRefs());
            List<NetworkPolicy> networkPolicyList = tungstenApi.getBackRefFromNetworkPolicy(NetworkPolicy.class, tag.getNetworkPolicyBackRefs());
            List<ApplicationPolicySet> applicationPolicySetList = tungstenApi.getBackRefFromApplicationPolicySet(ApplicationPolicySet.class, tag.getApplicationPolicySetBackRefs());
            TungstenModel tungstenModel = new TungstenTag(tag, virtualNetworkList, virtualMachineList, virtualMachineInterfaceList, networkPolicyList, applicationPolicySetList);
            return new TungstenAnswer(cmd, tungstenModel,true, "Tungsten-Fabric tag is applied");
        } else {
            if (numRetries > 0) {
                return retry(cmd, --numRetries);
            } else {
                return new TungstenAnswer(cmd, new IOException());
            }
        }
    }

    private Answer executeRequest(RemoveTungstenPolicyCommand cmd, int numRetries) {
        ApiObjectBase apiObjectBase = tungstenApi.removeTungstenPolicy(cmd.getNetworkUuid(), cmd.getPolicyUuid());

        if (apiObjectBase != null) {
            NetworkPolicy networkPolicy = (NetworkPolicy) apiObjectBase;
            List<VirtualNetwork> virtualNetworkList = tungstenApi.getNetworksFromNetworkPolicy(networkPolicy);
            TungstenModel tungstenModel = new TungstenNetworkPolicy(networkPolicy, virtualNetworkList);
            return new TungstenAnswer(cmd, tungstenModel, true, "Tungsten-Fabric policy is removed");
        } else {
            if (numRetries > 0) {
                return retry(cmd, --numRetries);
            } else {
                return new TungstenAnswer(cmd, new IOException());
            }
        }
    }

    private Answer executeRequest(RemoveTungstenTagCommand cmd, int numRetries) {
        ApiObjectBase apiObjectBase = tungstenApi.removeTungstenTag(cmd.getNetworkUuids(), cmd.getVmUuids(),
            cmd.getNicUuids(), cmd.getPolicyUuid(), cmd.getApplicationPolicySetUuid(), cmd.getTagUuid());

        if (apiObjectBase != null) {
            Tag tag = (Tag) apiObjectBase;
            List<VirtualNetwork> virtualNetworkList = tungstenApi.getBackRefFromVirtualNetwork(VirtualNetwork.class, tag.getVirtualNetworkBackRefs());
            List<VirtualMachine> virtualMachineList = tungstenApi.getBackRefFromVirtualMachine(VirtualMachine.class, tag.getVirtualMachineBackRefs());
            List<VirtualMachineInterface> virtualMachineInterfaceList = tungstenApi.getBackRefFromVirtualMachineInterface(VirtualMachineInterface.class, tag.getVirtualMachineInterfaceBackRefs());
            List<NetworkPolicy> networkPolicyList = tungstenApi.getBackRefFromNetworkPolicy(NetworkPolicy.class, tag.getNetworkPolicyBackRefs());
            List<ApplicationPolicySet> applicationPolicySetList = tungstenApi.getBackRefFromApplicationPolicySet(ApplicationPolicySet.class, tag.getApplicationPolicySetBackRefs());
            TungstenModel tungstenModel = new TungstenTag(tag, virtualNetworkList, virtualMachineList, virtualMachineInterfaceList, networkPolicyList, applicationPolicySetList);
            return new TungstenAnswer(cmd, tungstenModel,true, "Tungsten-Fabric tag is removed");
        } else {
            if (numRetries > 0) {
                return retry(cmd, --numRetries);
            } else {
                return new TungstenAnswer(cmd, new IOException());
            }
        }
    }

    private Answer executeRequest(CreateTungstenApplicationPolicySetCommand cmd, int numRetries) {
        ApiObjectBase apiObjectBase = tungstenApi.createTungstenApplicationPolicySet(cmd.getUuid(), cmd.getName());

        if (apiObjectBase != null)
            return new TungstenAnswer(cmd, apiObjectBase, true, "Tungsten-Fabric application policy set is created");
        else {
            if (numRetries > 0) {
                return retry(cmd, --numRetries);
            } else {
                return new TungstenAnswer(cmd, new IOException());
            }
        }
    }

    private Answer executeRequest(CreateTungstenFirewallPolicyCommand cmd, int numRetries) {
        ApiObjectBase apiObjectBase = tungstenApi.createTungstenFirewallPolicy(cmd.getUuid(), cmd.getApplicationPolicySetUuid(), cmd.getName(), cmd.getSequence());

        if (apiObjectBase != null)
            return new TungstenAnswer(cmd, apiObjectBase, true, "Tungsten-Fabric firewall policy is created");
        else {
            if (numRetries > 0) {
                return retry(cmd, --numRetries);
            } else {
                return new TungstenAnswer(cmd, new IOException());
            }
        }
    }

    private Answer executeRequest(CreateTungstenFirewallRuleCommand cmd, int numRetries) {
        ApiObjectBase apiObjectBase = tungstenApi.createTungstenFirewallRule(cmd.getUuid(), cmd.getFirewallPolicyUuid(), cmd.getName(),
            cmd.getAction(), cmd.getServiceGroupUuid(), cmd.getSrcTagUuid(), cmd.getSrcAddressGroupUuid(), cmd.getSrcNetworkUuid(),
            cmd.getDirection(), cmd.getDestTagUuid(), cmd.getDestAddressGroupUuid(), cmd.getDestNetworkUuid(), cmd.getTagTypeUuid(), cmd.getSequence());

        if (apiObjectBase != null)
            return new TungstenAnswer(cmd, apiObjectBase, true, "Tungsten-Fabric firewall rule is created");
        else {
            if (numRetries > 0) {
                return retry(cmd, --numRetries);
            } else {
                return new TungstenAnswer(cmd, new IOException());
            }
        }
    }

    private Answer executeRequest(CreateTungstenServiceGroupCommand cmd, int numRetries) {
        ApiObjectBase apiObjectBase = tungstenApi.createTungstenServiceGroup(cmd.getUuid(), cmd.getName(),
            cmd.getProtocol(), cmd.getStartPort(), cmd.getEndPort());

        if (apiObjectBase != null)
            return new TungstenAnswer(cmd, apiObjectBase, true, "Tungsten-Fabric service group is created");
        else {
            if (numRetries > 0) {
                return retry(cmd, --numRetries);
            } else {
                return new TungstenAnswer(cmd, new IOException());
            }
        }
    }

    private Answer executeRequest(CreateTungstenAddressGroupCommand cmd, int numRetries) {
        ApiObjectBase apiObjectBase = tungstenApi.createTungstenAddressGroup(cmd.getUuid(), cmd.getName(),
            cmd.getIpPrefix(), cmd.getIpPrefixLen());

        if (apiObjectBase != null)
            return new TungstenAnswer(cmd, apiObjectBase, true, "Tungsten-Fabric address group is created");
        else {
            if (numRetries > 0) {
                return retry(cmd, --numRetries);
            } else {
                return new TungstenAnswer(cmd, new IOException());
            }
        }
    }

    private Answer executeRequest(ListTungstenApplicationPolicySetCommand cmd, int numRetries) {
        List<ApiObjectBase> apiObjectBaseList = tungstenApi.listTungstenApplicationPolicySet(
            cmd.getApplicationPolicySetUuid());

        if (apiObjectBaseList != null)
            return new TungstenAnswer(cmd, apiObjectBaseList, true, "Tungsten-Fabric application policy set is listed");
        else {
            if (numRetries > 0) {
                return retry(cmd, --numRetries);
            } else {
                return new TungstenAnswer(cmd, new IOException());
            }
        }
    }

    private Answer executeRequest(ListTungstenFirewallPolicyCommand cmd, int numRetries) {
        List<ApiObjectBase> apiObjectBaseList = tungstenApi.listTungstenFirewallPolicy(
            cmd.getApplicationPolicySetUuid(), cmd.getFirewallPolicyUuid());

        if (apiObjectBaseList != null)
            return new TungstenAnswer(cmd, apiObjectBaseList, true, "Tungsten-Fabric firewall policy is listed");
        else {
            if (numRetries > 0) {
                return retry(cmd, --numRetries);
            } else {
                return new TungstenAnswer(cmd, new IOException());
            }
        }
    }

    private Answer executeRequest(ListTungstenFirewallRuleCommand cmd, int numRetries) {
        List<ApiObjectBase> apiObjectBaseList = tungstenApi.listTungstenFirewallRule(
            cmd.getFirewallPolicyUuid(), cmd.getFirewallRuleUuid());

        if (apiObjectBaseList != null)
            return new TungstenAnswer(cmd, apiObjectBaseList, true, "Tungsten-Fabric firewall rule is listed");
        else {
            if (numRetries > 0) {
                return retry(cmd, --numRetries);
            } else {
                return new TungstenAnswer(cmd, new IOException());
            }
        }
    }

    private Answer executeRequest(ListTungstenServiceGroupCommand cmd, int numRetries) {
        List<ApiObjectBase> apiObjectBaseList = tungstenApi.listTungstenServiceGroup(
            cmd.getServiceGroupUuid());

        if (apiObjectBaseList != null)
            return new TungstenAnswer(cmd, apiObjectBaseList, true, "Tungsten-Fabric service group is listed");
        else {
            if (numRetries > 0) {
                return retry(cmd, --numRetries);
            } else {
                return new TungstenAnswer(cmd, new IOException());
            }
        }
    }

    private Answer executeRequest(ListTungstenAddressGroupCommand cmd, int numRetries) {
        List<ApiObjectBase> apiObjectBaseList = tungstenApi.listTungstenAddressGroup(
            cmd.getAddressGroupUuid());

        if (apiObjectBaseList != null)
            return new TungstenAnswer(cmd, apiObjectBaseList, true, "Tungsten-Fabric address group is listed");
        else {
            if (numRetries > 0) {
                return retry(cmd, --numRetries);
            } else {
                return new TungstenAnswer(cmd, new IOException());
            }
        }
    }

    private Answer executeRequest(DeleteTungstenApplicationPolicySetCommand cmd, int numRetries) {
        ApiObjectBase apiObjectBase = tungstenApi.getTungstenObject(ApplicationPolicySet.class,
            cmd.getApplicationPolicySetUuid());
        if (apiObjectBase == null) {
            return new TungstenAnswer(cmd, true, "Tungsten-Fabric application policy set is not founded");
        }

        boolean deleted = tungstenApi.deleteTungstenObject(apiObjectBase);

        if (deleted)
            return new TungstenAnswer(cmd, true, "Tungsten-Fabric application policy set is deleted");
        else {
            if (numRetries > 0) {
                return retry(cmd, --numRetries);
            } else {
                return new TungstenAnswer(cmd, new IOException());
            }
        }
    }

    private Answer executeRequest(DeleteTungstenFirewallPolicyCommand cmd, int numRetries) {
        FirewallPolicy firewallPolicy = (FirewallPolicy) tungstenApi.getTungstenObject(FirewallPolicy.class, cmd.getFirewallPolicyUuid());
        if (firewallPolicy == null) {
            return new TungstenAnswer(cmd, true, "Tungsten-Fabric firewall policy is not founded");
        }

        List<ObjectReference<FirewallSequence>> objectReferenceList = firewallPolicy.getApplicationPolicySetBackRefs();
        if (objectReferenceList != null) {
            for(ObjectReference<FirewallSequence> objectReference : objectReferenceList) {
                ApplicationPolicySet applicationPolicySet = (ApplicationPolicySet) tungstenApi.getTungstenObject(ApplicationPolicySet.class,
                    objectReference.getUuid());
                applicationPolicySet.removeFirewallPolicy(firewallPolicy, new FirewallSequence());
                boolean updated = tungstenApi.updateTungstenObject(applicationPolicySet);
                if (!updated) {
                    return new TungstenAnswer(cmd, new IOException());
                }
            }
        }

        boolean deleted = tungstenApi.deleteTungstenObject(firewallPolicy);

        if (deleted)
            return new TungstenAnswer(cmd, true, "Tungsten-Fabric firewall policy is deleted");
        else {
            if (numRetries > 0) {
                return retry(cmd, --numRetries);
            } else {
                return new TungstenAnswer(cmd, new IOException());
            }
        }
    }

    private Answer executeRequest(DeleteTungstenFirewallRuleCommand cmd, int numRetries) {
        FirewallRule firewallRule = (FirewallRule) tungstenApi.getTungstenObject(FirewallRule.class, cmd.getFirewallRuleUuid());
        if (firewallRule == null) {
            return new TungstenAnswer(cmd, true, "Tungsten-Fabric firewall rule is not founded");
        }

        List<ObjectReference<FirewallSequence>> objectReferenceList = firewallRule.getFirewallPolicyBackRefs();
        if (objectReferenceList != null) {
            for(ObjectReference<FirewallSequence> objectReference : objectReferenceList) {
                FirewallPolicy firewallPolicy = (FirewallPolicy) tungstenApi.getTungstenObject(FirewallPolicy.class, objectReference.getUuid());
                firewallPolicy.removeFirewallRule(firewallRule, new FirewallSequence());
                boolean updated = tungstenApi.updateTungstenObject(firewallPolicy);
                if (!updated) {
                    return new TungstenAnswer(cmd, new IOException());
                }
            }
        }

        boolean deleted = tungstenApi.deleteTungstenObject(firewallRule);

        if (deleted)
            return new TungstenAnswer(cmd, true, "Tungsten-Fabric firewall rule is deleted");
        else {
            if (numRetries > 0) {
                return retry(cmd, --numRetries);
            } else {
                return new TungstenAnswer(cmd, new IOException());
            }
        }
    }

    private Answer executeRequest(DeleteTungstenServiceGroupCommand cmd, int numRetries) {
        ApiObjectBase apiObjectBase = tungstenApi.getTungstenObject(ServiceGroup.class, cmd.getServiceGroupUuid());
        if (apiObjectBase == null) {
            return new TungstenAnswer(cmd, true, "Tungsten-Fabric service group is not founded");
        }

        boolean deleted = tungstenApi.deleteTungstenObject(apiObjectBase);

        if (deleted)
            return new TungstenAnswer(cmd, true, "Tungsten-Fabric service group is deleted");
        else {
            if (numRetries > 0) {
                return retry(cmd, --numRetries);
            } else {
                return new TungstenAnswer(cmd, new IOException());
            }
        }
    }

    private Answer executeRequest(DeleteTungstenAddressGroupCommand cmd, int numRetries) {
        ApiObjectBase apiObjectBase = tungstenApi.getTungstenObject(AddressGroup.class, cmd.getAddressGroupUuid());
        if (apiObjectBase == null) {
            return new TungstenAnswer(cmd, true, "Tungsten-Fabric address group is not founded");
        }

        boolean deleted = tungstenApi.deleteTungstenObject(apiObjectBase);

        if (deleted)
            return new TungstenAnswer(cmd, true, "Tungsten-Fabric address group is deleted");
        else {
            if (numRetries > 0) {
                return retry(cmd, --numRetries);
            } else {
                return new TungstenAnswer(cmd, new IOException());
            }
        }
    }

    private Answer executeRequest(UpdateTungstenVrouterConfigCommand cmd, int numRetries) {
        ApiObjectBase apiObjectBase = tungstenApi.updateTungstenVrouterConfig(cmd.getForwardingMode());
        if (apiObjectBase != null) {
            return new TungstenAnswer(cmd, apiObjectBase, true,
                "Update Tungsten-Fabric vrouter config is successfully");
        } else {
            if (numRetries > 0) {
                return retry(cmd, --numRetries);
            } else {
                return new TungstenAnswer(cmd, new IOException());
            }
        }
    }

    private Answer executeRequest(UpdateTungstenDefaultSecurityGroupCommand cmd, int numRetries) {
        boolean isUpdated = tungstenApi.updateTungstenDefaultSecurityGroup(cmd.getProjectFqn());
        if (isUpdated)
            return new TungstenAnswer(cmd, true, "Update Tungsten-Fabric default security group is successfully");
        else {
            if (numRetries > 0) {
                return retry(cmd, --numRetries);
            } else {
                return new TungstenAnswer(cmd, new IOException());
            }
        }
    }

    private Answer executeRequest(CreateTungstenRoutingLogicalRouterCommand cmd, int numRetries) {
        Project project = (Project) tungstenApi.getTungstenProjectByFqn(cmd.getProjectFqn());
        if (project == null) {
            return new TungstenAnswer(cmd, new IOException());
        }

        ApiObjectBase apiObjectBase = tungstenApi.createRoutingLogicalRouter(project.getUuid(), cmd.getUuid(), cmd.getName());

        if (apiObjectBase != null) {
            List<VirtualNetwork> virtualNetworkList = tungstenApi.listConnectedNetworkFromLogicalRouter((LogicalRouter) apiObjectBase);
            TungstenModel tungstenModel = new TungstenLogicalRouter((LogicalRouter) apiObjectBase, virtualNetworkList);
            return new TungstenAnswer(cmd, tungstenModel, true, "Tungsten-Fabric logical router is created");
        } else {
            if (numRetries > 0) {
                return retry(cmd, --numRetries);
            } else {
                return new TungstenAnswer(cmd, new IOException());
            }
        }
    }

    private Answer executeRequest(AddTungstenNetworkGatewayToLogicalRouterCommand cmd) {
        ApiObjectBase apiObjectBase = tungstenApi.addNetworkGatewayToLogicalRouter(cmd.getNetworkUuid(), cmd.getLogicalRouterUuid(), cmd.getIpAddress());
        if (apiObjectBase != null) {
            List<VirtualNetwork> virtualNetworkList = tungstenApi.listConnectedNetworkFromLogicalRouter((LogicalRouter) apiObjectBase);
            TungstenModel tungstenModel = new TungstenLogicalRouter((LogicalRouter) apiObjectBase, virtualNetworkList);
            return new TungstenAnswer(cmd, tungstenModel, true, "Tungsten-Fabric network was added to logical router");
        } else {
            return new TungstenAnswer(cmd, new IOException());
        }
    }

    private Answer executeRequest(RemoveTungstenNetworkGatewayFromLogicalRouterCommand cmd) {
        ApiObjectBase apiObjectBase = tungstenApi.removeNetworkGatewayFromLogicalRouter(cmd.getNetworkUuid(), cmd.getLogicalRouterUuid());

        if (apiObjectBase != null) {
            List<VirtualNetwork> virtualNetworkList = tungstenApi.listConnectedNetworkFromLogicalRouter((LogicalRouter) apiObjectBase);
            TungstenModel tungstenModel = new TungstenLogicalRouter((LogicalRouter) apiObjectBase, virtualNetworkList);
            return new TungstenAnswer(cmd, tungstenModel, true, "Tungsten-Fabric network was removed from logical router");
        } else {
            return new TungstenAnswer(cmd, new IOException());
        }
    }

    private Answer executeRequest(ListTungstenRoutingLogicalRouterCommand cmd, int numRetries) {
        List<? extends ApiObjectBase> apiObjectBaseList = tungstenApi.listRoutingLogicalRouter(cmd.getLogicalRouterUuid());

        if (apiObjectBaseList != null) {
            List<TungstenModel> tungstenModelList = new ArrayList<>();
            for(ApiObjectBase apiObjectBase : apiObjectBaseList) {
                tungstenModelList.addAll(getTungstenLogicalRouterFromVirtualNetwork(apiObjectBase, cmd.getNetworkUuid()));
            }
            return new TungstenAnswer(cmd, true, tungstenModelList, "Tungsten-Fabric logical router is listed");
        } else {
            if (numRetries > 0) {
                return retry(cmd, --numRetries);
            } else {
                return new TungstenAnswer(cmd, new IOException());
            }
        }
    }

    private List<TungstenModel> getTungstenLogicalRouterFromVirtualNetwork(ApiObjectBase apiObjectBase, String networkUuid) {
        List<TungstenModel> tungstenModelList = new ArrayList<>();
        List<VirtualNetwork> virtualNetworkList = tungstenApi.listConnectedNetworkFromLogicalRouter((LogicalRouter) apiObjectBase);
        if (networkUuid != null) {
            for(VirtualNetwork virtualNetwork : virtualNetworkList) {
                if (virtualNetwork.getUuid().equals(networkUuid)) {
                    tungstenModelList.add(
                            new TungstenLogicalRouter((LogicalRouter) apiObjectBase, virtualNetworkList));
                }
            }
        } else {
            tungstenModelList.add(new TungstenLogicalRouter((LogicalRouter) apiObjectBase, virtualNetworkList));
        }
        return tungstenModelList;
    }

    private Answer executeRequest(DeleteTungstenRoutingLogicalRouterCommand cmd, int numRetries) {
        LogicalRouter logicalRouter = (LogicalRouter) tungstenApi.getTungstenObject(LogicalRouter.class, cmd.getLogicalRouterUuid());
        if (logicalRouter == null) {
            return new TungstenAnswer(cmd, true, "Tungsten-Fabric logical router is not founded");
        }

        boolean deleted = tungstenApi.deleteTungstenObject(logicalRouter);

        if (deleted)
            return new TungstenAnswer(cmd, true, "Tungsten-Fabric logical router is deleted");
        else {
            if (numRetries > 0) {
                return retry(cmd, --numRetries);
            } else {
                return new TungstenAnswer(cmd, new IOException());
            }
        }
    }

    private Answer executeRequest(ListTungstenConnectedNetworkFromLogicalRouterCommand cmd) {
        LogicalRouter logicalRouter = (LogicalRouter) tungstenApi.getTungstenObject(LogicalRouter.class, cmd.getLogicalRouterUuid());
        List<VirtualNetwork> networkList = tungstenApi.listConnectedNetworkFromLogicalRouter(logicalRouter);
        List<ApiObjectBase> apiObjectBaseList = new ArrayList<>(networkList);
        return new TungstenAnswer(cmd, apiObjectBaseList, true, "Tungsten-Fabric logical router connected network is listed");
    }

    private Answer executeRequest(CreateTungstenDefaultProjectCommand cmd) {
        Project project = tungstenApi.createDefaultTungstenProject();
        return new TungstenAnswer(cmd, project, true, "Tungsten-Fabric default project is created");
    }

    private Answer retry(Command cmd, int numRetries) {
        logger.warn("Retrying " + cmd.getClass().getSimpleName() + ". Number of retries remaining: " + numRetries);
        return executeRequestGroup1(cmd, numRetries);
    }

    @Override
    public void disconnected() {
        // Do nothing
    }

    @Override
    public boolean start() {
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public IAgentControl getAgentControl() {
        return null;
    }

    @Override
    public void setAgentControl(IAgentControl agentControl) {
        // Do nothing
    }

    private Answer executeRequest(ReadyCommand cmd) {
        return new ReadyAnswer(cmd);
    }

    private Answer executeRequest(MaintainCommand cmd) {
        return new MaintainAnswer(cmd);
    }

    @Override
    public void setName(final String name) {
        this.name = name;
    }

    @Override
    public void setConfigParams(final Map<String, Object> params) {
        // Do nothing
    }

    @Override
    public Map<String, Object> getConfigParams() {
        return new HashMap<>();
    }

    @Override
    public int getRunLevel() {
        return 0;
    }

    @Override
    public void setRunLevel(final int level) {
        // Do nothing
    }
}
