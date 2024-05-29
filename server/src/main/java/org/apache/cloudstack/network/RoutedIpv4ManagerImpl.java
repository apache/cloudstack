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

package org.apache.cloudstack.network;

import com.cloud.event.ActionEvent;
import com.cloud.event.EventTypes;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.network.Network;
import com.cloud.network.Network.Provider;
import com.cloud.network.Network.Service;
import com.cloud.network.NetworkModel;
import com.cloud.network.dao.FirewallRulesDao;
import com.cloud.network.dao.NetworkServiceMapDao;
import com.cloud.network.firewall.FirewallService;
import com.cloud.network.rules.FirewallManager;
import com.cloud.network.rules.FirewallRule;
import com.cloud.network.rules.FirewallRuleVO;
import com.cloud.network.vpc.Vpc;
import com.cloud.network.vpc.dao.VpcOfferingDao;
import com.cloud.offering.NetworkOffering;
import com.cloud.offerings.dao.NetworkOfferingDao;
import com.cloud.offerings.dao.NetworkOfferingServiceMapDao;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.utils.Pair;
import com.cloud.utils.component.ComponentLifecycleBase;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallbackWithException;
import com.cloud.utils.db.TransactionStatus;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.net.NetUtils;

import org.apache.cloudstack.api.command.admin.network.CreateIpv4GuestSubnetCmd;
import org.apache.cloudstack.api.command.admin.network.CreateIpv4SubnetForGuestNetworkCmd;
import org.apache.cloudstack.api.command.admin.network.DedicateIpv4GuestSubnetCmd;
import org.apache.cloudstack.api.command.admin.network.DeleteIpv4GuestSubnetCmd;
import org.apache.cloudstack.api.command.admin.network.DeleteIpv4SubnetForGuestNetworkCmd;
import org.apache.cloudstack.api.command.admin.network.ListIpv4GuestSubnetsCmd;
import org.apache.cloudstack.api.command.admin.network.ListIpv4SubnetsForGuestNetworkCmd;
import org.apache.cloudstack.api.command.admin.network.ReleaseDedicatedIpv4GuestSubnetCmd;
import org.apache.cloudstack.api.command.admin.network.UpdateIpv4GuestSubnetCmd;
import org.apache.cloudstack.api.command.user.network.routing.CreateRoutingFirewallRuleCmd;
import org.apache.cloudstack.api.command.user.network.routing.DeleteRoutingFirewallRuleCmd;
import org.apache.cloudstack.api.command.user.network.routing.ListRoutingFirewallRulesCmd;
import org.apache.cloudstack.api.command.user.network.routing.UpdateRoutingFirewallRuleCmd;
import org.apache.cloudstack.api.response.DataCenterIpv4SubnetResponse;
import org.apache.cloudstack.api.response.Ipv4SubnetForGuestNetworkResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.datacenter.DataCenterIpv4GuestSubnet;
import org.apache.cloudstack.datacenter.dao.DataCenterIpv4GuestSubnetDao;
import org.apache.cloudstack.network.Ipv4GuestSubnetNetworkMap.State;
import org.apache.cloudstack.network.dao.Ipv4GuestSubnetNetworkMapDao;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.commons.lang3.ObjectUtils;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class RoutedIpv4ManagerImpl extends ComponentLifecycleBase implements RoutedIpv4Manager {

    @Inject
    DataCenterIpv4GuestSubnetDao dataCenterIpv4GuestSubnetDao;
    @Inject
    Ipv4GuestSubnetNetworkMapDao ipv4GuestSubnetNetworkMapDao;
    @Inject
    FirewallService firewallService;
    @Inject
    FirewallManager firewallManager;
    @Inject
    FirewallRulesDao firewallDao;
    @Inject
    NetworkServiceMapDao networkServiceMapDao;
    @Inject
    NetworkOfferingServiceMapDao networkOfferingServiceMapDao;
    @Inject
    NetworkOfferingDao networkOfferingDao;
    @Inject
    NetworkModel networkModel;
    @Inject
    AccountManager accountManager;
    @Inject
    VpcOfferingDao vpcOfferingDao;

    @Override
    public String getConfigComponentName() {
        return RoutedIpv4Manager.class.getSimpleName();
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey[] {};
    }

    @Override
    public List<Class<?>> getCommands() {
        final List<Class<?>> cmdList = new ArrayList<Class<?>>();
        cmdList.add(CreateIpv4GuestSubnetCmd.class);
        cmdList.add(DeleteIpv4GuestSubnetCmd.class);
        cmdList.add(ListIpv4GuestSubnetsCmd.class);
        cmdList.add(UpdateIpv4GuestSubnetCmd.class);
        cmdList.add(CreateIpv4SubnetForGuestNetworkCmd.class);
        cmdList.add(ListIpv4SubnetsForGuestNetworkCmd.class);
        cmdList.add(DeleteIpv4SubnetForGuestNetworkCmd.class);
        cmdList.add(CreateRoutingFirewallRuleCmd.class);
        cmdList.add(ListRoutingFirewallRulesCmd.class);
        cmdList.add(UpdateRoutingFirewallRuleCmd.class);
        cmdList.add(DeleteRoutingFirewallRuleCmd.class);
        return cmdList;
    }


    @Override
    public DataCenterIpv4GuestSubnet createDataCenterIpv4GuestSubnet(CreateIpv4GuestSubnetCmd cmd) {
        // TODO
        // check conflicts
        // save to DB by dataCenterIpv4GuestSubnetDao
        return null;
    }

    @Override
    public DataCenterIpv4SubnetResponse createDataCenterIpv4SubnetResponse(DataCenterIpv4GuestSubnet subnet) {
        DataCenterIpv4SubnetResponse response = new DataCenterIpv4SubnetResponse();
        // TODO
        return response;
    }

    @Override
    public boolean deleteDataCenterIpv4GuestSubnet(DeleteIpv4GuestSubnetCmd cmd) {
        // TODO
        // check if subnet is in use
        // remove via dataCenterIpv4GuestSubnetDao
        return true;
    }

    @Override
    public DataCenterIpv4GuestSubnet updateDataCenterIpv4GuestSubnet(UpdateIpv4GuestSubnetCmd cmd) {
        // TODO
        // check if subnet can be updated
        // update via dataCenterIpv4GuestSubnetDao
        return null;
    }

    @Override
    public List<? extends DataCenterIpv4GuestSubnet> listDataCenterIpv4GuestSubnets(ListIpv4GuestSubnetsCmd cmd) {
        // TODO
        // search via dataCenterIpv4GuestSubnetDao
        return new ArrayList<>();
    }

    @Override
    public DataCenterIpv4GuestSubnet dedicateDataCenterIpv4GuestSubnet(DedicateIpv4GuestSubnetCmd cmd) {
        // TODO
        // Mark the subnet as dedicated
        // update domain_id or account_id via dataCenterIpv4GuestSubnetDao
        return null;
    }

    @Override
    public DataCenterIpv4GuestSubnet releaseDedicatedDataCenterIpv4GuestSubnet(ReleaseDedicatedIpv4GuestSubnetCmd cmd) {
        // TODO
        // Mark the subnet as released
        // update domain_id or account_id via dataCenterIpv4GuestSubnetDao
        return null;
    }

    @Override
    public Ipv4GuestSubnetNetworkMap createIpv4SubnetForGuestNetwork(CreateIpv4SubnetForGuestNetworkCmd cmd) {
        if (ObjectUtils.allNotNull(cmd.getSubnet(), cmd.getCidrSize())) {
            throw new InvalidParameterValueException("subnet and cidrsize are mutually exclusive");
        }
        DataCenterIpv4GuestSubnet parent = dataCenterIpv4GuestSubnetDao.findById(cmd.getParentId());
        if (parent == null) {
            throw new InvalidParameterValueException("the parent subnet is invalid");
        }
        if (cmd.getSubnet() != null) {
            return createIpv4SubnetFromParentSubnet(parent, cmd.getSubnet());
        } else if (cmd.getCidrSize() != null) {
            return createIpv4SubnetFromParentSubnet(parent, cmd.getCidrSize());
        }
        return null;
    }

    @Override
    public boolean deleteIpv4SubnetForGuestNetwork(DeleteIpv4SubnetForGuestNetworkCmd cmd) {
        // TODO
        // check if the subnet is not in use
        // update via ipv4GuestSubnetNetworkMapDao
        return false;
    }

    @Override
    public List<? extends Ipv4GuestSubnetNetworkMap> listIpv4GuestSubnetsForGuestNetwork(ListIpv4SubnetsForGuestNetworkCmd cmd) {
        // TODO
        // search via ipv4GuestSubnetNetworkMapDao
        return new ArrayList<>();
    }

    @Override
    public Ipv4SubnetForGuestNetworkResponse createIpv4SubnetForGuestNetworkResponse(Ipv4GuestSubnetNetworkMap subnet) {
        Ipv4SubnetForGuestNetworkResponse response = new Ipv4SubnetForGuestNetworkResponse();
        // TODO
        return response;
    }

    @Override
    public void getOrCreateIpv4SubnetForGuestNetwork(Network network, String networkCidr) {
        Ipv4GuestSubnetNetworkMapVO subnetMap = ipv4GuestSubnetNetworkMapDao.findBySubnet(networkCidr);
        if (subnetMap != null) {
            // TODO
            // check if the subnet is in use
            // check if the subnet accessible by the owner
            // assign to the network, then return
            return;
        }

        // TODO: check if the subnet belongs to a parent subnet
        // TODO: If yes, check if the subnet accessible by the owner
        if (subnetMap != null) {
            // TODO: assign to the network, then return
            return;
        }

        // TODO: if not,
        subnetMap = new Ipv4GuestSubnetNetworkMapVO(null, networkCidr, null, State.Allocated);
        ipv4GuestSubnetNetworkMapDao.persist(subnetMap);
    }

    @Override
    public void getOrCreateIpv4SubnetForGuestNetwork(Network network, Integer networkCidrSize) {
        // TODO
        Ipv4GuestSubnetNetworkMap subnet = getIpv4SubnetForAccount(network.getAccountId(), networkCidrSize);

        if (subnet != null) {
            // TODO: assign to the network, then return
            return;
        }

        subnet = createIpv4SubnetForAccount(network.getAccountId(), networkCidrSize);
        // TODO: assign to the network, then return
        network.setCidr(subnet.getSubnet());
    }

    private Ipv4GuestSubnetNetworkMap getIpv4SubnetForAccount(long accountId, Integer networkCidrSize) {
        // TODO
        // Get dedicated guest subnets for the account
        // Get zone guest subnets for the account
        // find an allocated subnet
        return null;
    }

    private Ipv4GuestSubnetNetworkMap createIpv4SubnetForAccount(long accountId, Integer networkCidrSize) {
        // TODO
        // Get dedicated guest subnets for the account
        // Get zone guest subnets for the account
        // createIpv4SubnetFromParentSubnet
        return null;
    }

    private Ipv4GuestSubnetNetworkMap createIpv4SubnetFromParentSubnet(DataCenterIpv4GuestSubnet parent, Integer networkCidrSize) {
        // TODO
        // Allocate a subnet automatically
        // create DB record
        return null;
    }

    private Ipv4GuestSubnetNetworkMap createIpv4SubnetFromParentSubnet(DataCenterIpv4GuestSubnet parent, String networkCidr) {
        // TODO
        // Validate the network cidr
        // create DB record
        return null;
    }

    @Override
    public void assignIpv4SubnetToNetwork(String cidr, long networkId) {
        Ipv4GuestSubnetNetworkMapVO subnetMap = ipv4GuestSubnetNetworkMapDao.findBySubnet(cidr);
        if (subnetMap != null) {
            subnetMap.setNetworkId(networkId);
            ipv4GuestSubnetNetworkMapDao.update(subnetMap.getId(), subnetMap);
        }
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_ROUTING_IPV4_FIREWALL_RULE_CREATE,
            eventDescription = "creating routing firewall rule", async = true)
    public FirewallRule createRoutingFirewallRule(CreateRoutingFirewallRuleCmd createRoutingFirewallRuleCmd) throws NetworkRuleConflictException {
        final Account caller = CallContext.current().getCallingAccount();
        final long networkId = createRoutingFirewallRuleCmd.getNetworkId();
        final Integer portStart = createRoutingFirewallRuleCmd.getSourcePortStart();
        final Integer portEnd = createRoutingFirewallRuleCmd.getSourcePortEnd();
        final FirewallRule.TrafficType trafficType = createRoutingFirewallRuleCmd.getTrafficType();
        final String protocol = createRoutingFirewallRuleCmd.getProtocol();
        final Integer icmpCode = createRoutingFirewallRuleCmd.getIcmpCode();
        final Integer icmpType = createRoutingFirewallRuleCmd.getIcmpType();
        final boolean forDisplay = createRoutingFirewallRuleCmd.isDisplay();
        final FirewallRule.FirewallRuleType type = FirewallRule.FirewallRuleType.User;
        final List<String> sourceCidrList = createRoutingFirewallRuleCmd.getSourceCidrList();
        final List<String> destinationCidrList = createRoutingFirewallRuleCmd.getDestinationCidrList();

        for (String cidr : sourceCidrList) {
            if (!NetUtils.isValidIp4Cidr(cidr)) {
                throw new InvalidParameterValueException(String.format("Invalid source IPv4 CIDR: %s", cidr));
            }
        }
        for (String cidr : destinationCidrList) {
            if (!NetUtils.isValidIp4Cidr(cidr)) {
                throw new InvalidParameterValueException(String.format("Invalid destination IPv4 CIDR: %s", cidr));
            }
        }
        if (portStart != null && !NetUtils.isValidPort(portStart)) {
            throw new InvalidParameterValueException("publicPort is an invalid value: " + portStart);
        }
        if (portEnd != null && !NetUtils.isValidPort(portEnd)) {
            throw new InvalidParameterValueException("Public port range is an invalid value: " + portEnd);
        }
        if (ObjectUtils.allNotNull(portStart, portEnd) && portStart > portEnd) {
            throw new InvalidParameterValueException("Start port can't be bigger than end port");
        }

        Network network = networkModel.getNetwork(networkId);
        assert network != null : "Can't create rule as network is null?";

        final long accountId = network.getAccountId();
        final long domainId = network.getDomainId();

        accountManager.checkAccess(caller, null, true, network);

        // Verify that the network guru supports the protocol specified
        Map<Network.Capability, String> caps = networkModel.getNetworkServiceCapabilities(network.getId(), Network.Service.Firewall);

        if (caps != null) {
            String supportedProtocols;
            String supportedTrafficTypes = null;
            supportedTrafficTypes = caps.get(Network.Capability.SupportedTrafficDirection).toLowerCase();

            if (trafficType == FirewallRule.TrafficType.Egress) {
                supportedProtocols = caps.get(Network.Capability.SupportedEgressProtocols).toLowerCase();
            } else {
                supportedProtocols = caps.get(Network.Capability.SupportedProtocols).toLowerCase();
            }

            if (!supportedProtocols.contains(protocol.toLowerCase())) {
                throw new InvalidParameterValueException(String.format("Protocol %s is not supported in zone", protocol));
            } else if (!supportedTrafficTypes.contains(trafficType.toString().toLowerCase())) {
                throw new InvalidParameterValueException("Traffic Type " + trafficType + " is currently supported by Firewall in network " + networkId);
            }
        }

        // icmp code and icmp type can't be passed in for any other protocol rather than icmp
        if (!protocol.equalsIgnoreCase(NetUtils.ICMP_PROTO) && (icmpCode != null || icmpType != null)) {
            throw new InvalidParameterValueException("Can specify icmpCode and icmpType for ICMP protocol only");
        }

        if (protocol.equalsIgnoreCase(NetUtils.ICMP_PROTO) && (portStart != null || portEnd != null)) {
            throw new InvalidParameterValueException("Can't specify start/end port when protocol is ICMP");
        }

        return Transaction.execute(new TransactionCallbackWithException<FirewallRuleVO, NetworkRuleConflictException>() {
            @Override
            public FirewallRuleVO doInTransaction(TransactionStatus status) throws NetworkRuleConflictException {
                FirewallRuleVO newRule =
                        new FirewallRuleVO(null, null, portStart, portEnd, protocol.toLowerCase(), networkId, accountId, domainId, FirewallRule.Purpose.Firewall,
                                sourceCidrList, destinationCidrList, icmpCode, icmpType, null, trafficType);
                newRule.setType(type);
                newRule.setDisplay(forDisplay);
                newRule = firewallDao.persist(newRule);

                if (FirewallRule.FirewallRuleType.User.equals(type)) {
                    firewallManager.detectRulesConflict(newRule);
                }

                if (!firewallDao.setStateToAdd(newRule)) {
                    throw new CloudRuntimeException("Unable to update the state to add for " + newRule);
                }
                CallContext.current().setEventDetails("Rule Id: " + newRule.getId());

                return newRule;
            }
        });
    }

    @Override
    public Pair<List<? extends FirewallRule>, Integer> listRoutingFirewallRules(ListRoutingFirewallRulesCmd listRoutingFirewallRulesCmd) {
        return firewallService.listFirewallRules(listRoutingFirewallRulesCmd);
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_ROUTING_IPV4_FIREWALL_RULE_UPDATE,
            eventDescription = "updating routing firewall rule", async = true)
    public FirewallRule updateRoutingFirewallRule(UpdateRoutingFirewallRuleCmd updateRoutingFirewallRuleCmd) {
        final long id = updateRoutingFirewallRuleCmd.getId();
        final boolean forDisplay = updateRoutingFirewallRuleCmd.isDisplay();
        FirewallRuleVO rule = firewallDao.findById(id);
        if (rule == null) {
            throw new InvalidParameterValueException(String.format("Unable to find routing firewall rule with id %d", id));
        }
        if (FirewallRule.TrafficType.Ingress.equals(rule.getTrafficType())) {
            return firewallManager.updateIngressFirewallRule(rule.getId(), null, forDisplay);
        }
        return firewallManager.updateEgressFirewallRule(rule.getId(), null, forDisplay);
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_ROUTING_IPV4_FIREWALL_RULE_DELETE,
            eventDescription = "revoking routing firewall rule", async = true)
    public boolean revokeRoutingFirewallRule(Long id) {
        FirewallRuleVO rule = firewallDao.findById(id);
        if (rule == null) {
            throw new InvalidParameterValueException(String.format("Unable to find routing firewall rule with id %d", id));
        }
        if (FirewallRule.TrafficType.Ingress.equals(rule.getTrafficType())) {
            return firewallManager.revokeIngressFirewallRule(rule.getId(), true);
        }
        return firewallManager.revokeEgressFirewallRule(rule.getId(), true);
    }

    @Override
    public boolean applyRoutingFirewallRule(long id) {
        FirewallRuleVO rule = firewallDao.findById(id);
        if (rule == null) {
            logger.error(String.format("Unable to find routing firewall rule with ID: %d", id));
            return false;
        }
        if (!FirewallRule.Purpose.Firewall.equals(rule.getPurpose())) {
            logger.error(String.format("Cannot apply routing firewall rule with ID: %d as purpose %s is not %s", id, rule.getPurpose(), FirewallRule.Purpose.Firewall));
        }
        logger.debug(String.format("Applying routing firewall rules for rule with ID: %s", rule.getUuid()));
        List<FirewallRuleVO> rules = firewallDao.listByNetworkPurposeTrafficType(rule.getNetworkId(), rule.getPurpose(), FirewallRule.TrafficType.Egress);
        rules.addAll(firewallDao.listByNetworkPurposeTrafficType(rule.getNetworkId(), rule.getPurpose(), FirewallRule.TrafficType.Ingress));
        return firewallManager.applyFirewallRules(rules, false, CallContext.current().getCallingAccount());
    }

    @Override
    public boolean isVirtualRouterGateway(Network network) {
        return networkServiceMapDao.canProviderSupportServiceInNetwork(network.getId(), Service.Gateway, Provider.VirtualRouter)
                || networkServiceMapDao.canProviderSupportServiceInNetwork(network.getId(), Service.Gateway, Provider.VPCVirtualRouter);
    }

    @Override
    public boolean isVirtualRouterGateway(NetworkOffering networkOffering) {
        return networkOfferingServiceMapDao.canProviderSupportServiceInNetworkOffering(networkOffering.getId(), Service.Gateway, Provider.VirtualRouter)
                || networkOfferingServiceMapDao.canProviderSupportServiceInNetworkOffering(networkOffering.getId(), Service.Gateway, Provider.VPCVirtualRouter);
    }

    @Override
    public boolean isRoutedNetwork(Network network) {
        return NetworkOffering.RoutingMode.ROUTED.name().equals(networkOfferingDao.findById(network.getNetworkOfferingId()).getRoutingMode());
    }

    @Override
    public boolean isRoutedVpc(Vpc vpc) {
        return NetworkOffering.RoutingMode.ROUTED.name().equals(vpcOfferingDao.findById(vpc.getVpcOfferingId()).getRoutingMode());
    }
}
