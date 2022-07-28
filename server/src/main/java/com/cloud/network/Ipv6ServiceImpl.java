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

package com.cloud.network;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.command.admin.network.CreateGuestNetworkIpv6PrefixCmd;
import org.apache.cloudstack.api.command.admin.network.DeleteGuestNetworkIpv6PrefixCmd;
import org.apache.cloudstack.api.command.admin.network.ListGuestNetworkIpv6PrefixesCmd;
import org.apache.cloudstack.api.command.user.ipv6.CreateIpv6FirewallRuleCmd;
import org.apache.cloudstack.api.command.user.ipv6.DeleteIpv6FirewallRuleCmd;
import org.apache.cloudstack.api.command.user.ipv6.ListIpv6FirewallRulesCmd;
import org.apache.cloudstack.api.command.user.ipv6.UpdateIpv6FirewallRuleCmd;
import org.apache.cloudstack.api.response.Ipv6RouteResponse;
import org.apache.cloudstack.api.response.VpcResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.engine.orchestration.service.NetworkOrchestrationService;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.managed.context.ManagedContextRunnable;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import com.cloud.api.ApiDBUtils;
import com.cloud.configuration.Resource;
import com.cloud.dc.DataCenter;
import com.cloud.dc.DataCenterGuestIpv6Prefix;
import com.cloud.dc.DataCenterGuestIpv6PrefixVO;
import com.cloud.dc.Vlan;
import com.cloud.dc.VlanVO;
import com.cloud.dc.dao.DataCenterGuestIpv6PrefixDao;
import com.cloud.dc.dao.VlanDao;
import com.cloud.event.ActionEvent;
import com.cloud.event.ActionEventUtils;
import com.cloud.event.EventTypes;
import com.cloud.event.EventVO;
import com.cloud.event.UsageEventUtils;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.network.dao.FirewallRulesDao;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.IPAddressVO;
import com.cloud.network.dao.Ipv6GuestPrefixSubnetNetworkMapDao;
import com.cloud.network.dao.NetworkDetailsDao;
import com.cloud.network.firewall.FirewallService;
import com.cloud.network.guru.PublicNetworkGuru;
import com.cloud.network.router.VirtualRouter;
import com.cloud.network.rules.FirewallManager;
import com.cloud.network.rules.FirewallRule;
import com.cloud.network.rules.FirewallRuleVO;
import com.cloud.network.vpc.Vpc;
import com.cloud.offerings.dao.NetworkOfferingDao;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.utils.Pair;
import com.cloud.utils.component.ComponentLifecycleBase;
import com.cloud.utils.concurrency.NamedThreadFactory;
import com.cloud.utils.db.GlobalLock;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallbackNoReturn;
import com.cloud.utils.db.TransactionCallbackWithException;
import com.cloud.utils.db.TransactionStatus;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.net.NetUtils;
import com.cloud.vm.DomainRouterVO;
import com.cloud.vm.Nic;
import com.cloud.vm.NicProfile;
import com.cloud.vm.NicVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.dao.DomainRouterDao;
import com.cloud.vm.dao.NicDao;
import com.googlecode.ipv6.IPv6Address;
import com.googlecode.ipv6.IPv6Network;
import com.googlecode.ipv6.IPv6NetworkMask;

public class Ipv6ServiceImpl extends ComponentLifecycleBase implements Ipv6Service {

    public static final Logger s_logger = Logger.getLogger(Ipv6ServiceImpl.class.getName());
    private static final String s_publicNetworkReserver = PublicNetworkGuru.class.getSimpleName();

    ScheduledExecutorService _ipv6GuestPrefixSubnetNetworkMapStateScanner;

    @Inject
    NetworkOfferingDao networkOfferingDao;
    @Inject
    VlanDao vlanDao;
    @Inject
    DataCenterGuestIpv6PrefixDao dataCenterGuestIpv6PrefixDao;
    @Inject
    Ipv6GuestPrefixSubnetNetworkMapDao ipv6GuestPrefixSubnetNetworkMapDao;
    @Inject
    FirewallRulesDao firewallDao;
    @Inject
    FirewallService firewallService;
    @Inject
    NetworkDetailsDao networkDetailsDao;
    @Inject
    NicDao nicDao;
    @Inject
    DomainRouterDao domainRouterDao;
    @Inject
    AccountManager accountManager;
    @Inject
    NetworkModel networkModel;
    @Inject
    IPAddressDao ipAddressDao;
    @Inject
    FirewallManager firewallManager;
    @Inject
    NetworkOrchestrationService networkOrchestrationService;

    private boolean isPublicIpv6PlaceholderNic(NicVO nic) {
        return  ObjectUtils.allNotNull(nic.getIPv6Address(), nic.getIPv6Cidr(), nic.getIPv6Gateway()) &&
                s_publicNetworkReserver.equals(nic.getReserver());
    }

    private Pair<String, ? extends Vlan> getPublicIpv6FromNetworkPlaceholder(Network network, List<VlanVO> ranges) {
        List<NicVO> placeholderNics = nicDao.listPlaceholderNicsByNetworkIdAndVmType(network.getId(), VirtualMachine.Type.DomainRouter);
        if (CollectionUtils.isEmpty(placeholderNics)) {
            return null;
        }
        Optional<NicVO> nicOptional = placeholderNics.stream().filter(this::isPublicIpv6PlaceholderNic).findFirst();
        if (nicOptional.isEmpty()) {
            return null;
        }
        NicVO nic = nicOptional.get();
        Optional<VlanVO> vlanOptional = ranges.stream().filter(v -> nic.getIPv6Cidr().equals(v.getIp6Cidr()) && nic.getIPv6Gateway().equals(v.getIp6Gateway())).findFirst();
        if (vlanOptional.isEmpty()) {
            s_logger.error(String.format("Public IPv6 placeholder NIC with cidr: %s, gateway: %s for network ID: %d is not present in the allocated VLAN: %s",
                    nic.getIPv6Cidr(), nic.getIPv6Gateway(),network.getId(), ranges.get(0).getVlanTag()));
            return null;
        }
        return new Pair<>(nic.getIPv6Address(), vlanOptional.get());
    }

    private void publishPublicIpv6AssignActionEvent(final Network network, final String ipv6Address) {
        String event = EventTypes.EVENT_NET_IP6_ASSIGN;
        final String description = String.format("Assigned public IPv6 address: %1$s for network ID: %2$s. Subnet: %3$s, gateway: %1$s", ipv6Address,  network.getUuid(), network.getIp6Cidr());
        ActionEventUtils.onCompletedActionEvent(CallContext.current().getCallingUserId(), network.getAccountId(), EventVO.LEVEL_INFO, event, description, network.getId(), ApiCommandResourceType.Network.toString(), 0);
        final boolean usageHidden = networkDetailsDao.isNetworkUsageHidden(network.getId());
        final String guestType = Vlan.VlanType.VirtualNetwork.toString();
        UsageEventUtils.publishUsageEvent(event, network.getAccountId(), network.getDataCenterId(), 0L,
                ipv6Address, false, guestType, false, usageHidden,
                IPv6Network.class.getName(), null);
    }

    private void publishPublicIpv6ReleaseActionEvent(Network network, String nicIpv6Address) {
        String event = EventTypes.EVENT_NET_IP6_RELEASE;
        String description = String.format("Released public IPv6 address: %1$s from network ID: %2$s. Subnet: %3$s, gateway: %1$s", nicIpv6Address,  network.getUuid(), network.getIp6Cidr());
        ActionEventUtils.onCompletedActionEvent(CallContext.current().getCallingUserId(), network.getAccountId(), EventVO.LEVEL_INFO, event, description, network.getId(), ApiCommandResourceType.Network.toString(), 0);
        final boolean usageHidden = networkDetailsDao.isNetworkUsageHidden(network.getId());
        UsageEventUtils.publishUsageEvent(event, network.getAccountId(), network.getDataCenterId(), 0L,
                nicIpv6Address, false, Vlan.VlanType.VirtualNetwork.toString(), false, usageHidden,
                IPv6Address.class.getName(), null);
    }

    private void processPublicIpv6AddressUpdateForVpcTier(final Network network, final Nic nic) {
        if (network.getVpcId() == null || !Network.State.Implementing.equals(network.getState())) {
            return;
        }
        DomainRouterVO router = domainRouterDao.findById(nic.getInstanceId());
        if (router == null) {
            return;
        }
        if (router.getIsRedundantRouter() && !VirtualRouter.RedundantState.PRIMARY.equals(router.getRedundantState())) {
            return;
        }
        networkOrchestrationService.savePlaceholderNic(network, null, nic.getIPv6Address(), nic.getIPv6Cidr(), nic.getIPv6Gateway(), s_publicNetworkReserver, VirtualMachine.Type.DomainRouter);
        publishPublicIpv6AssignActionEvent(network, nic.getIPv6Address());
    }

    private Pair<String, ? extends Vlan> assignPublicIpv6ToNetworkInternal(Network network, String vlanId, String nicMacAddress) throws InsufficientAddressCapacityException {
        final List<VlanVO> ranges = vlanDao.listIpv6RangeByPhysicalNetworkIdAndVlanId(network.getPhysicalNetworkId(), vlanId);
        if (CollectionUtils.isEmpty(ranges)) {
            s_logger.error(String.format("Unable to find IPv6 address for zone ID: %d, physical network ID: %d, VLAN: %s", network.getDataCenterId(), network.getPhysicalNetworkId(), vlanId));
            InsufficientAddressCapacityException ex = new InsufficientAddressCapacityException("Insufficient address capacity", DataCenter.class, network.getDataCenterId());
            ex.addProxyObject(ApiDBUtils.findZoneById(network.getDataCenterId()).getUuid());
            throw ex;
        }
        Pair<String, ? extends Vlan> placeholderResult = getPublicIpv6FromNetworkPlaceholder(network, ranges);
        if (placeholderResult != null) {
            return placeholderResult;
        }
        Pair<String, ? extends Vlan> result = Transaction.execute((TransactionCallbackWithException<Pair<String, ? extends Vlan>, InsufficientAddressCapacityException>) status -> {
            removePublicIpv6PlaceholderNics(network);
            Collections.shuffle(ranges);
            VlanVO selectedVlan = ranges.get(0);
            IPv6Network ipv6Network = IPv6Network.fromString(selectedVlan.getIp6Cidr());
            if (ipv6Network.getNetmask().asPrefixLength() < IPV6_SLAAC_CIDR_NETMASK) {
                Iterator<IPv6Network> splits = ipv6Network.split(IPv6NetworkMask.fromPrefixLength(IPV6_SLAAC_CIDR_NETMASK));
                if (splits.hasNext()) {
                    ipv6Network = splits.next();
                }
            }
            IPv6Address ipv6Addr = NetUtils.EUI64Address(ipv6Network, nicMacAddress);
            networkOrchestrationService.savePlaceholderNic(network, null, ipv6Addr.toString(), selectedVlan.getIp6Cidr(), selectedVlan.getIp6Gateway(), s_publicNetworkReserver, VirtualMachine.Type.DomainRouter);
            return new Pair<>(ipv6Addr.toString(), selectedVlan);
        });
        final String ipv6Address = result.first();
        publishPublicIpv6AssignActionEvent(network, ipv6Address);
        return result;
    }

    private Ipv6GuestPrefixSubnetNetworkMapVO preallocatePrefixSubnetRandomly(DataCenterGuestIpv6PrefixVO prefix) {
        Ipv6GuestPrefixSubnetNetworkMapVO ip6Subnet = null;
        List<Ipv6GuestPrefixSubnetNetworkMapVO> prefixSubnetNetworkMapVOList = ipv6GuestPrefixSubnetNetworkMapDao.listUsedByPrefix(prefix.getId());
        List<String> usedPrefixList = prefixSubnetNetworkMapVOList.stream().map(Ipv6GuestPrefixSubnetNetworkMap::getSubnet).collect(Collectors.toList());
        final IPv6Network ip6Prefix = IPv6Network.fromString(prefix.getPrefix());
        Iterator<IPv6Network> splits = ip6Prefix.split(IPv6NetworkMask.fromPrefixLength(IPV6_SLAAC_CIDR_NETMASK));
        List<IPv6Network> availableSubnets = new ArrayList<>();
        while (splits.hasNext()) {
            IPv6Network i = splits.next();
            if (!usedPrefixList.contains(i.toString())) {
                availableSubnets.add(i);
            }
        }
        if (CollectionUtils.isNotEmpty(availableSubnets)) {
            Random r = new Random();
            IPv6Network subnet = availableSubnets.get(r.nextInt(availableSubnets.size()));
            ip6Subnet = new Ipv6GuestPrefixSubnetNetworkMapVO(prefix.getId(), subnet.toString(), null, Ipv6GuestPrefixSubnetNetworkMap.State.Allocating);
        }
        return ip6Subnet;
    }

    protected void releaseIpv6Subnet(long subnetId) {
        Ipv6GuestPrefixSubnetNetworkMapVO ipv6GuestPrefixSubnetNetworkMapVO = ipv6GuestPrefixSubnetNetworkMapDao.createForUpdate(subnetId);
        ipv6GuestPrefixSubnetNetworkMapVO.setState(Ipv6GuestPrefixSubnetNetworkMap.State.Free);
        ipv6GuestPrefixSubnetNetworkMapVO.setNetworkId(null);
        ipv6GuestPrefixSubnetNetworkMapVO.setUpdated(new Date());
        ipv6GuestPrefixSubnetNetworkMapDao.update(ipv6GuestPrefixSubnetNetworkMapVO.getId(), ipv6GuestPrefixSubnetNetworkMapVO);
    }

    @Override
    public boolean start() {
        _ipv6GuestPrefixSubnetNetworkMapStateScanner.scheduleWithFixedDelay(new Ipv6GuestPrefixSubnetNetworkMapStateScanner(), 300, 30*60, TimeUnit.SECONDS);
        return true;
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        _name = name;
        _configParams = params;
        _ipv6GuestPrefixSubnetNetworkMapStateScanner = Executors.newScheduledThreadPool(1, new NamedThreadFactory("Ipv6GuestPrefixSubnet-State-Scanner"));

        return true;
    }

    @Override
    public List<Class<?>> getCommands() {
        final List<Class<?>> cmdList = new ArrayList<Class<?>>();
        cmdList.add(CreateGuestNetworkIpv6PrefixCmd.class);
        cmdList.add(ListGuestNetworkIpv6PrefixesCmd.class);
        cmdList.add(DeleteGuestNetworkIpv6PrefixCmd.class);
        cmdList.add(CreateIpv6FirewallRuleCmd.class);
        cmdList.add(ListIpv6FirewallRulesCmd.class);
        cmdList.add(UpdateIpv6FirewallRuleCmd.class);
        cmdList.add(DeleteIpv6FirewallRuleCmd.class);
        return cmdList;
    }

    @Override
    public String getConfigComponentName() {
        return Ipv6Service.class.getSimpleName();
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey<?>[] {
                Ipv6OfferingCreationEnabled,
                Ipv6PrefixSubnetCleanupInterval
        };
    }

    @Override
    public Pair<Integer, Integer> getUsedTotalIpv6SubnetForPrefix(DataCenterGuestIpv6Prefix prefix) {
        List<Ipv6GuestPrefixSubnetNetworkMapVO> usedSubnets = ipv6GuestPrefixSubnetNetworkMapDao.listUsedByPrefix(prefix.getId());
        final IPv6Network ip6Prefix = IPv6Network.fromString(prefix.getPrefix());
        Iterator<IPv6Network> splits = ip6Prefix.split(IPv6NetworkMask.fromPrefixLength(IPV6_SLAAC_CIDR_NETMASK));
        int total = 0;
        while(splits.hasNext()) {
            total++;
            splits.next();
        }
        return new Pair<>(usedSubnets.size(), total);
    }

    @Override
    public Pair<Integer, Integer> getUsedTotalIpv6SubnetForZone(long zoneId) {
        int used = 0;
        int total = 0;
        List<DataCenterGuestIpv6PrefixVO> prefixes = dataCenterGuestIpv6PrefixDao.listByDataCenterId(zoneId);
        for (DataCenterGuestIpv6PrefixVO prefix : prefixes) {
            Pair<Integer, Integer> usedTotal = getUsedTotalIpv6SubnetForPrefix(prefix);
            used += usedTotal.first();
            total += usedTotal.second();
        }
        return new Pair<>(used, total);
    }

    public Pair<String, String> preAllocateIpv6SubnetForNetwork(long zoneId) throws ResourceAllocationException {
        return Transaction.execute((TransactionCallbackWithException<Pair<String, String>, ResourceAllocationException>) status -> {
            List<DataCenterGuestIpv6PrefixVO> prefixes = dataCenterGuestIpv6PrefixDao.listByDataCenterId(zoneId);
            if (CollectionUtils.isEmpty(prefixes)) {
                s_logger.error(String.format("IPv6 prefixes not found for the zone ID: %d", zoneId));
                throw new ResourceAllocationException("Unable to allocate IPv6 network", Resource.ResourceType.network);
            }
            Ipv6GuestPrefixSubnetNetworkMapVO ip6Subnet = null;
            for (DataCenterGuestIpv6PrefixVO prefix : prefixes) {
                ip6Subnet = ipv6GuestPrefixSubnetNetworkMapDao.findFirstAvailable(prefix.getId());
                if (ip6Subnet == null) {
                    ip6Subnet = preallocatePrefixSubnetRandomly(prefix);
                }
                if (ip6Subnet != null) {
                    break;
                }
            }
            if (ip6Subnet == null) {
                throw new ResourceAllocationException("Unable to allocate IPv6 guest subnet for the network", Resource.ResourceType.network);
            }
            ip6Subnet.setUpdated(new Date());
            if (Ipv6GuestPrefixSubnetNetworkMap.State.Free.equals(ip6Subnet.getState())) {
                ip6Subnet.setState(Ipv6GuestPrefixSubnetNetworkMap.State.Allocating);
                ipv6GuestPrefixSubnetNetworkMapDao.update(ip6Subnet.getId(), ip6Subnet);
            } else {
                ipv6GuestPrefixSubnetNetworkMapDao.persist(ip6Subnet);
            }
            IPv6Network network = IPv6Network.fromString(ip6Subnet.getSubnet());
            IPv6Address gateway = network.getFirst().add(1);
            return new Pair<>(gateway.toString(), network.toString());
        });
    }

    @Override
    public void assignIpv6SubnetToNetwork(String subnet, long networkId) {
        Transaction.execute(new TransactionCallbackNoReturn() {
            @Override
            public void doInTransactionWithoutResult(TransactionStatus status) {
                Ipv6GuestPrefixSubnetNetworkMapVO ipv6GuestPrefixSubnetNetworkMapVO = ipv6GuestPrefixSubnetNetworkMapDao.findBySubnet(subnet);
                if (ipv6GuestPrefixSubnetNetworkMapVO != null) {
                    ipv6GuestPrefixSubnetNetworkMapVO = ipv6GuestPrefixSubnetNetworkMapDao.createForUpdate(ipv6GuestPrefixSubnetNetworkMapVO.getId());
                    ipv6GuestPrefixSubnetNetworkMapVO.setState(Ipv6GuestPrefixSubnetNetworkMap.State.Allocated);
                    ipv6GuestPrefixSubnetNetworkMapVO.setNetworkId(networkId);
                    ipv6GuestPrefixSubnetNetworkMapVO.setUpdated(new Date());
                    ipv6GuestPrefixSubnetNetworkMapDao.update(ipv6GuestPrefixSubnetNetworkMapVO.getId(), ipv6GuestPrefixSubnetNetworkMapVO);
                }
            }
        });
    }

    @Override
    public void releaseIpv6SubnetForNetwork(long networkId) {
        Transaction.execute(new TransactionCallbackNoReturn() {
            @Override
            public void doInTransactionWithoutResult(TransactionStatus status) {
                Ipv6GuestPrefixSubnetNetworkMapVO ipv6GuestPrefixSubnetNetworkMapVO = ipv6GuestPrefixSubnetNetworkMapDao.findByNetworkId(networkId);
                if (ipv6GuestPrefixSubnetNetworkMapVO != null) {
                    releaseIpv6Subnet(ipv6GuestPrefixSubnetNetworkMapVO.getId());
                }
            }
        });
    }

    @Override
    public List<String> getAllocatedIpv6FromVlanRange(Vlan vlan) {
        if (ObjectUtils.allNull(vlan.getIp6Cidr(), vlan.getIp6Gateway())) {
            return null;
        }
        List<NicVO> nics = nicDao.findNicsByIpv6GatewayIpv6CidrAndReserver(vlan.getIp6Gateway(), vlan.getIp6Cidr(), s_publicNetworkReserver);
        if (CollectionUtils.isNotEmpty(nics)) {
            return nics.stream().map(NicVO::getIPv6Address).collect(Collectors.toList());
        }
        return null;
    }

    @Override
    public Nic assignPublicIpv6ToNetwork(Network network, Nic nic) {
        if (StringUtils.isNotEmpty(nic.getIPv6Address())) {
            processPublicIpv6AddressUpdateForVpcTier(network, nic);
            return nic;
        }
        try {
            Pair<String, ? extends Vlan> publicIpv6AddressVlanPair = assignPublicIpv6ToNetworkInternal(network, nic.getBroadcastUri().toString(), nic.getMacAddress());
            Vlan vlan = publicIpv6AddressVlanPair.second();
            final String ipv6Address = publicIpv6AddressVlanPair.first();
            final String ipv6Gateway = vlan.getIp6Gateway();
            final String ipv6Cidr = vlan.getIp6Cidr();
            NicVO nicVO = nicDao.createForUpdate(nic.getId());
            nicVO.setIPv6Address(ipv6Address);
            nicVO.setIPv6Gateway(ipv6Gateway);
            nicVO.setIPv6Cidr(ipv6Cidr);
            nicDao.update(nic.getId(), nicVO);
            return nicVO;
        } catch (InsufficientAddressCapacityException ex) {
            throw new CloudRuntimeException(ex.getMessage());
        }
    }

    @Override
    public void updateNicIpv6(NicProfile nic, DataCenter dc, Network network) throws InsufficientAddressCapacityException {
        boolean isIpv6Supported = networkOfferingDao.isIpv6Supported(network.getNetworkOfferingId());
        if (nic.getIPv6Address() == null && isIpv6Supported) {
            Pair<String, ? extends Vlan> publicIpv6AddressVlanPair = assignPublicIpv6ToNetworkInternal(network, nic.getBroadCastUri().toString(), nic.getMacAddress());
            final Vlan vlan = publicIpv6AddressVlanPair.second();
            final String routerIpv6 = publicIpv6AddressVlanPair.first();
            final String routerIpv6Gateway = vlan.getIp6Gateway();
            final String routerIpv6Cidr = vlan.getIp6Cidr();
            nic.setIPv6Address(routerIpv6);
            nic.setIPv6Gateway(routerIpv6Gateway);
            nic.setIPv6Cidr(routerIpv6Cidr);
            if (nic.getIPv4Address() != null) {
                nic.setFormat(Networks.AddressFormat.DualStack);
            } else {
                nic.setFormat(Networks.AddressFormat.Ip6);
            }
            nic.setIPv6Dns1(dc.getIp6Dns1());
            nic.setIPv6Dns2(dc.getIp6Dns2());
        }
    }

    @Override
    public List<String> getPublicIpv6AddressesForNetwork(Network network) {
        List<String> addresses = new ArrayList<>();
        List<DomainRouterVO> routers = domainRouterDao.findByNetwork(network.getId());
        for (DomainRouterVO router : routers) {
            List<NicVO> nics = nicDao.listByVmId(router.getId());
            for (NicVO nic : nics) {
                String address = nic.getIPv6Address();
                if (!s_publicNetworkReserver.equals(nic.getReserver()) ||
                        StringUtils.isEmpty(address) ||
                        addresses.contains(address)) {
                    continue;
                }
                addresses.add(address);
            }
        }
        return addresses;
    }

    @Override
    public void updateIpv6RoutesForVpcResponse(Vpc vpc, VpcResponse response) {
        Set<Ipv6RouteResponse> ipv6Routes = new LinkedHashSet<>();
        List<? extends Network> networks = networkModel.listNetworksByVpc(vpc.getId());
        for (Network network : networks) {
            if (networkOfferingDao.isIpv6Supported(network.getNetworkOfferingId())) {
                List<String> networkPublicIpv6 = getPublicIpv6AddressesForNetwork(network);
                for (String address : networkPublicIpv6) {
                    Ipv6RouteResponse route = new Ipv6RouteResponse(network.getIp6Cidr(), address);
                    ipv6Routes.add(route);
                }
            }
        }
        if (CollectionUtils.isNotEmpty(ipv6Routes)) {
            response.setIpv6Routes(ipv6Routes);
        }
    }

    @Override
    public void checkNetworkIpv6Upgrade(Network network) throws InsufficientAddressCapacityException, ResourceAllocationException {
        List<DataCenterGuestIpv6PrefixVO> prefixes = dataCenterGuestIpv6PrefixDao.listByDataCenterId(network.getDataCenterId());
        if (CollectionUtils.isEmpty(prefixes)) {
            s_logger.error(String.format("IPv6 prefixes not found for the zone ID: %d", network.getDataCenterId()));
            throw new ResourceAllocationException("Unable to allocate IPv6 network", Resource.ResourceType.network);
        }
        List<IPAddressVO> addresses = network.getVpcId() == null ?
                ipAddressDao.listByAssociatedNetwork(network.getId(), true) :
                ipAddressDao.listByAssociatedVpc(network.getVpcId(), true);
        for (IPAddressVO address : addresses) {
            VlanVO vlan = vlanDao.findById(address.getVlanId());
            final List<VlanVO> ranges = vlanDao.listIpv6RangeByPhysicalNetworkIdAndVlanId(network.getPhysicalNetworkId(), vlan.getVlanTag());
            if (CollectionUtils.isEmpty(ranges)) {
                s_logger.error(String.format("Unable to find IPv6 address for zone ID: %d, physical network ID: %d, VLAN: %s", network.getDataCenterId(), network.getPhysicalNetworkId(), vlan.getVlanTag()));
                InsufficientAddressCapacityException ex = new InsufficientAddressCapacityException("Insufficient address capacity", DataCenter.class, network.getDataCenterId());
                ex.addProxyObject(ApiDBUtils.findZoneById(network.getDataCenterId()).getUuid());
                throw ex;
            }
        }
    }

    @Override
    public Pair<List<? extends FirewallRule>, Integer> listIpv6FirewallRules(ListIpv6FirewallRulesCmd listIpv6FirewallRulesCmd) {
        return firewallService.listFirewallRules(listIpv6FirewallRulesCmd);
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_FIREWALL_OPEN, eventDescription = "creating IPv6 firewall rule", create = true)
    public FirewallRule createIpv6FirewallRule(CreateIpv6FirewallRuleCmd cmd) throws NetworkRuleConflictException {
        final Account caller = CallContext.current().getCallingAccount();
        final long networkId = cmd.getNetworkId();
        final Integer portStart = cmd.getSourcePortStart();
        final Integer portEnd = cmd.getSourcePortEnd();
        final FirewallRule.TrafficType trafficType = cmd.getTrafficType();
        final String protocol = cmd.getProtocol();
        final Integer icmpCode = cmd.getIcmpCode();
        final Integer icmpType = cmd.getIcmpType();
        final boolean forDisplay = cmd.isDisplay();
        final FirewallRule.FirewallRuleType type = FirewallRule.FirewallRuleType.User;
        final List<String> sourceCidrList = cmd.getSourceCidrList();
        final List<String> destinationCidrList = cmd.getDestinationCidrList();

        for (String cidr : sourceCidrList) {
            if (!NetUtils.isValidIp6Cidr(cidr)) {
                throw new InvalidParameterValueException(String.format("Invalid source IPv6 CIDR: %s", cidr));
            }
        }
        for (String cidr : destinationCidrList) {
            if (!NetUtils.isValidIp6Cidr(cidr)) {
                throw new InvalidParameterValueException(String.format("Invalid destination IPv6 CIDR: %s", cidr));
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

        if (FirewallRule.TrafficType.Egress.equals(trafficType)) {
            accountManager.checkAccess(caller, null, true, network);
        }

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
                        new FirewallRuleVO(null, null, portStart, portEnd, protocol.toLowerCase(), networkId, accountId, domainId, FirewallRule.Purpose.Ipv6Firewall,
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
    @ActionEvent(eventType = EventTypes.EVENT_FIREWALL_CLOSE, eventDescription = "revoking IPv6 firewall rule", async = true)
    public boolean revokeIpv6FirewallRule(Long id) {
        FirewallRuleVO rule = firewallDao.findById(id);
        if (rule == null) {
            throw new InvalidParameterValueException(String.format("Unable to find IPv6 firewall rule with id %d", id));
        }
        if (FirewallRule.TrafficType.Ingress.equals(rule.getTrafficType())) {
            return firewallManager.revokeIngressFirewallRule(rule.getId(), true);
        }
        return firewallManager.revokeEgressFirewallRule(rule.getId(), true);
    }

    @ActionEvent(eventType = EventTypes.EVENT_FIREWALL_UPDATE, eventDescription = "updating IPv6 firewall rule", async = true)
    public FirewallRule updateIpv6FirewallRule(UpdateIpv6FirewallRuleCmd cmd) {
        final long id = cmd.getId();
        final boolean forDisplay = cmd.isDisplay();
        FirewallRuleVO rule = firewallDao.findById(id);
        if (rule == null) {
            throw new InvalidParameterValueException(String.format("Unable to find IPv6 firewall rule with id %d", id));
        }
        if (FirewallRule.TrafficType.Ingress.equals(rule.getTrafficType())) {
            return firewallManager.updateIngressFirewallRule(rule.getId(), null, forDisplay);
        }
        return firewallManager.updateEgressFirewallRule(rule.getId(), null, forDisplay);
    }

    @Override
    public FirewallRule getIpv6FirewallRule(Long entityId) {
        return firewallDao.findById(entityId);
    }

    @Override
    public boolean applyIpv6FirewallRule(long id) {
        FirewallRuleVO rule = firewallDao.findById(id);
        if (rule == null) {
            s_logger.error(String.format("Unable to find IPv6 firewall rule with ID: %d", id));
            return false;
        }
        if (!FirewallRule.Purpose.Ipv6Firewall.equals(rule.getPurpose())) {
            s_logger.error(String.format("Cannot apply IPv6 firewall rule with ID: %d as purpose %s is not %s", id, rule.getPurpose(), FirewallRule.Purpose.Ipv6Firewall));
        }
        s_logger.debug(String.format("Applying IPv6 firewall rules for rule with ID: %s", rule.getUuid()));
        List<FirewallRuleVO> rules = firewallDao.listByNetworkPurposeTrafficType(rule.getNetworkId(), rule.getPurpose(), FirewallRule.TrafficType.Egress);
        rules.addAll(firewallDao.listByNetworkPurposeTrafficType(rule.getNetworkId(), FirewallRule.Purpose.Ipv6Firewall, FirewallRule.TrafficType.Ingress));
        return firewallManager.applyFirewallRules(rules, false, CallContext.current().getCallingAccount());
    }

    @Override
    public void removePublicIpv6PlaceholderNics(Network network) {
        try {
            List<NicVO> nics = nicDao.listPlaceholderNicsByNetworkId(network.getId())
                    .stream().filter(this::isPublicIpv6PlaceholderNic).collect(Collectors.toList());
            if (CollectionUtils.isNotEmpty(nics)) {
                Transaction.execute(new TransactionCallbackNoReturn() {
                    @Override
                    public void doInTransactionWithoutResult(TransactionStatus status) {
                        for (Nic nic : nics) {
                            s_logger.debug("Removing placeholder nic " + nic);
                            nicDao.remove(nic.getId());
                            publishPublicIpv6ReleaseActionEvent(network, nic.getIPv6Address());
                        }
                    }
                });
            }
        } catch (Exception e) {
            String msg = String.format("IPv6 Placeholder Nics trash. Exception: %s", e.getMessage());
            s_logger.error(msg);
            throw new CloudRuntimeException(msg, e);
        }
    }

    public class Ipv6GuestPrefixSubnetNetworkMapStateScanner extends ManagedContextRunnable {
        @Override
        protected void runInContext() {
            GlobalLock gcLock = GlobalLock.getInternLock("Ipv6GuestPrefixSubnetNetworkMap.State.Scanner.Lock");
            try {
                if (gcLock.lock(3)) {
                    try {
                        reallyRun();
                    } finally {
                        gcLock.unlock();
                    }
                }
            } finally {
                gcLock.releaseRef();
            }
        }

        public void reallyRun() {
            try {
                List<Ipv6GuestPrefixSubnetNetworkMapVO> subnets = ipv6GuestPrefixSubnetNetworkMapDao.findPrefixesInStates(Ipv6GuestPrefixSubnetNetworkMap.State.Allocating);
                for (Ipv6GuestPrefixSubnetNetworkMapVO subnet : subnets) {
                    if (s_logger.isInfoEnabled()) {
                        s_logger.info(String.format("Running state scanned on Ipv6GuestPrefixSubnetNetworkMap : %s", subnet.getSubnet()));
                    }
                    try {
                        if ((new Date()).getTime() - subnet.getUpdated().getTime() < Ipv6PrefixSubnetCleanupInterval.value()*1000) {
                            continue;
                        }
                        releaseIpv6Subnet(subnet.getId());
                    } catch (CloudRuntimeException e) {
                        s_logger.warn(String.format("Failed to release IPv6 guest prefix subnet : %s during state scan", subnet.getSubnet()), e);
                    }
                }
            } catch (Exception e) {
                s_logger.warn("Caught exception while running Ipv6GuestPrefixSubnetNetworkMap state scanner: ", e);
            }
        }
    }
}
