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
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.cloudstack.api.command.user.ipv6.CreateIpv6FirewallRuleCmd;
import org.apache.cloudstack.api.command.user.ipv6.DeleteIpv6FirewallRuleCmd;
import org.apache.cloudstack.api.command.user.ipv6.ListIpv6FirewallRulesCmd;
import org.apache.cloudstack.api.command.user.ipv6.UpdateIpv6FirewallRuleCmd;
import org.apache.cloudstack.managed.context.ManagedContextRunnable;
import org.apache.commons.collections.CollectionUtils;
import org.apache.log4j.Logger;

import com.cloud.configuration.Resource;
import com.cloud.dc.DataCenter;
import com.cloud.dc.DataCenterGuestIpv6PrefixVO;
import com.cloud.dc.Vlan;
import com.cloud.dc.VlanVO;
import com.cloud.dc.dao.DataCenterGuestIpv6PrefixDao;
import com.cloud.dc.dao.VlanDao;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.network.dao.FirewallRulesDao;
import com.cloud.network.dao.Ipv6GuestPrefixSubnetNetworkMapDao;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.dao.PublicIpv6AddressNetworkMapDao;
import com.cloud.network.firewall.FirewallService;
import com.cloud.network.rules.FirewallRule;
import com.cloud.offerings.dao.NetworkOfferingDao;
import com.cloud.utils.Pair;
import com.cloud.utils.component.ComponentLifecycleBase;
import com.cloud.utils.concurrency.NamedThreadFactory;
import com.cloud.utils.db.GlobalLock;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.NicProfile;
import com.googlecode.ipv6.IPv6Address;
import com.googlecode.ipv6.IPv6AddressRange;
import com.googlecode.ipv6.IPv6Network;
import com.googlecode.ipv6.IPv6NetworkMask;

public class Ipv6ServiceImpl extends ComponentLifecycleBase implements Ipv6Service {

    public static final Logger s_logger = Logger.getLogger(Ipv6ServiceImpl.class.getName());

    ScheduledExecutorService _ipv6GuestPrefixSubnetNetworkMapStateScanner;

    ScheduledExecutorService _ipv6GuestNetworkRoutesLogger;

    @Inject
    NetworkOfferingDao networkOfferingDao;
    @Inject
    VlanDao vlanDao;
    @Inject
    DataCenterGuestIpv6PrefixDao dataCenterGuestIpv6PrefixDao;
    @Inject
    Ipv6GuestPrefixSubnetNetworkMapDao ipv6GuestPrefixSubnetNetworkMapDao;
    @Inject
    PublicIpv6AddressNetworkMapDao publicIpv6AddressNetworkMapDao;
    @Inject
    FirewallRulesDao firewallDao;
    @Inject
    FirewallService firewallService;
    @Inject
    NetworkDao networkDao;

    private IPv6AddressRange getIpv6AddressRangeFromIpv6Vlan(VlanVO vlanVO) {
        String[] rangeArr = vlanVO.getIp6Range().split("-");
        String firstStr = rangeArr[0].trim();
        String lastStr = rangeArr[1].trim();
        IPv6AddressRange range = null;
        try {
            IPv6Address first = IPv6Address.fromString(firstStr);
            IPv6Address last = IPv6Address.fromString(lastStr);
            range = IPv6AddressRange.fromFirstAndLast(first, last);
        } catch (IllegalArgumentException ex) {
            s_logger.warn(String.format("Unable to retrieve IPv6 address range for vlan ID: %d, range: %s", vlanVO.getId(), vlanVO.getIp6Range()), ex);
        }
        return range;
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
        _ipv6GuestNetworkRoutesLogger.scheduleWithFixedDelay(new Ipv6GuestNetworkRoutesLogger(), 2*60, 60, TimeUnit.SECONDS);
        return true;
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        _name = name;
        _configParams = params;
        _ipv6GuestPrefixSubnetNetworkMapStateScanner = Executors.newScheduledThreadPool(1, new NamedThreadFactory("Ipv6GuestPrefixSubnet-State-Scanner"));
        _ipv6GuestNetworkRoutesLogger = Executors.newScheduledThreadPool(1, new NamedThreadFactory("Ipv6GuestNetwork-Routes-Logger"));

        return true;
    }

    @Override
    public List<Class<?>> getCommands() {
        final List<Class<?>> cmdList = new ArrayList<Class<?>>();
        cmdList.add(CreateIpv6FirewallRuleCmd.class);
        cmdList.add(ListIpv6FirewallRulesCmd.class);
        cmdList.add(UpdateIpv6FirewallRuleCmd.class);
        cmdList.add(DeleteIpv6FirewallRuleCmd.class);
        return cmdList;
    }

    public Pair<String, String> preAllocateIpv6SubnetForNetwork(long zoneId) throws ResourceAllocationException {
        List<DataCenterGuestIpv6PrefixVO> prefixes = dataCenterGuestIpv6PrefixDao.listByDataCenterId(zoneId);
        if (CollectionUtils.isEmpty(prefixes)) {
            s_logger.error(String.format("IPv6 prefixes not found for the zone ID: %d", zoneId));
            throw new ResourceAllocationException("Unable to allocate IPv6 network", Resource.ResourceType.network);
        }
        Ipv6GuestPrefixSubnetNetworkMapVO ip6Subnet = null;
        for (DataCenterGuestIpv6PrefixVO prefix : prefixes) {
            ip6Subnet = ipv6GuestPrefixSubnetNetworkMapDao.findFirstAvailable(prefix.getId());
            if (ip6Subnet == null) {
                Ipv6GuestPrefixSubnetNetworkMapVO last = ipv6GuestPrefixSubnetNetworkMapDao.findLast(prefix.getId());
                String lastUsedSubnet = last != null ? last.getSubnet() : null;
                final IPv6Network ip6Prefix = IPv6Network.fromString(prefix.getPrefix());
                Iterator<IPv6Network> splits = ip6Prefix.split(IPv6NetworkMask.fromPrefixLength(IPV6_GUEST_SUBNET_NETMASK));
                if (splits.hasNext()) {
                    splits.next();
                }
                while (splits.hasNext()) {
                    IPv6Network i = splits.next();
                    if (lastUsedSubnet == null) {
                        ip6Subnet = new Ipv6GuestPrefixSubnetNetworkMapVO(prefix.getId(), i.toString(), null, Ipv6GuestPrefixSubnetNetworkMap.State.Allocating);
                        break;
                    }
                    if (i.toString().equals(lastUsedSubnet)) {
                        lastUsedSubnet = null;
                    }
                }
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
        return new Pair<>(network.getFirst().toString(), network.toString());
    }

    @Override
    public void assignIpv6SubnetToNetwork(String subnet, long networkId) {
        Ipv6GuestPrefixSubnetNetworkMapVO ipv6GuestPrefixSubnetNetworkMapVO  = ipv6GuestPrefixSubnetNetworkMapDao.findBySubnet(subnet);
        if (ipv6GuestPrefixSubnetNetworkMapVO != null) {
            ipv6GuestPrefixSubnetNetworkMapVO = ipv6GuestPrefixSubnetNetworkMapDao.createForUpdate(ipv6GuestPrefixSubnetNetworkMapVO.getId());
            ipv6GuestPrefixSubnetNetworkMapVO.setState(Ipv6GuestPrefixSubnetNetworkMap.State.Allocated);
            ipv6GuestPrefixSubnetNetworkMapVO.setNetworkId(networkId);
            ipv6GuestPrefixSubnetNetworkMapVO.setUpdated(new Date());
            ipv6GuestPrefixSubnetNetworkMapDao.update(ipv6GuestPrefixSubnetNetworkMapVO.getId(), ipv6GuestPrefixSubnetNetworkMapVO);
        }
    }

    @Override
    public void releaseIpv6SubnetForNetwork(long networkId) {
        Ipv6GuestPrefixSubnetNetworkMapVO ipv6GuestPrefixSubnetNetworkMapVO  = ipv6GuestPrefixSubnetNetworkMapDao.findByNetworkId(networkId);
        if (ipv6GuestPrefixSubnetNetworkMapVO != null) {
            releaseIpv6Subnet(ipv6GuestPrefixSubnetNetworkMapVO.getId());
        }
    }

    @Override
    public Pair<? extends PublicIpv6AddressNetworkMap, ? extends Vlan> assignPublicIpv6ToNetwork(Network network) {
        PublicIpv6AddressNetworkMapVO ip6NetworkMap = null;
        VlanVO selectedVlan = null;
        final List<VlanVO> ranges = vlanDao.listVlansWithIpV6RangeByPhysicalNetworkId(network.getPhysicalNetworkId());
        if (ranges == null) {
            s_logger.error(String.format("Unable to find IPv6 range for the zone ID: %d", network.getDataCenterId()));
            throw new CloudRuntimeException(String.format("Cannot find IPv6 range for network %s", network.getName()));
        }
        for (VlanVO range : ranges) {
            ip6NetworkMap = publicIpv6AddressNetworkMapDao.findFirstAvailable(range.getId());
            if (ip6NetworkMap == null) {
                PublicIpv6AddressNetworkMapVO last = publicIpv6AddressNetworkMapDao.findLast(range.getId());
                String lastUsedIp6Address = last != null ? last.getIp6Address() : null;
                final IPv6AddressRange ip6Range = getIpv6AddressRangeFromIpv6Vlan(range);
                IPv6Address ip = null;
                if (ip6Range != null) {
                    if (lastUsedIp6Address == null) {
                        ip = ip6Range.getFirst();
                    } else {
                        IPv6Address lastAddress = IPv6Address.fromString(lastUsedIp6Address);
                        lastAddress = lastAddress.add(1);
                        if (ip6Range.contains(lastAddress)) {
                            ip = lastAddress;
                        }
                    }
                    if (ip != null) {
                        selectedVlan = range;
                        ip6NetworkMap = new PublicIpv6AddressNetworkMapVO(range.getId(), ip.toString(), network.getId(), PublicIpv6AddressNetworkMap.State.Allocated);
                        break;
                    }
                }
            }
        }
        if (ip6NetworkMap == null) {
            s_logger.error(String.format("Unable to find an IPv6 address available for allocation for network %s in zone ID: %d", network.getName(), network.getDataCenterId()));
            throw new CloudRuntimeException(String.format("Cannot find available IPv6 address for network %s", network.getName()));
        }
        if (PublicIpv6AddressNetworkMap.State.Free.equals(ip6NetworkMap.getState())) {
            ip6NetworkMap.setState(PublicIpv6AddressNetworkMap.State.Allocated);
            publicIpv6AddressNetworkMapDao.update(ip6NetworkMap.getId(), ip6NetworkMap);
        } else {
            publicIpv6AddressNetworkMapDao.persist(ip6NetworkMap);
        }
        return new Pair<>(ip6NetworkMap, selectedVlan);
    }

    @Override
    public void updateNicIpv6(NicProfile nic, DataCenter dc, Network network) {
        boolean isIpv6Supported = networkOfferingDao.isIpv6Supported(network.getNetworkOfferingId());
        if (nic.getIPv6Address() == null && isIpv6Supported) {
            Pair<? extends PublicIpv6AddressNetworkMap, ? extends Vlan> publicIpv6AddressNetworkMapVlanPair = assignPublicIpv6ToNetwork(network);
            final PublicIpv6AddressNetworkMap publicIpv6AddressNetworkMapVO = publicIpv6AddressNetworkMapVlanPair.first();
            final Vlan vlan = publicIpv6AddressNetworkMapVlanPair.second();
            final String routerIpv6 = publicIpv6AddressNetworkMapVO.getIp6Address();
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
    public void releasePublicIpv6ForNetwork(long networkId) {
        PublicIpv6AddressNetworkMapVO publicIpv6AddressNetworkMapVO  = publicIpv6AddressNetworkMapDao.findByNetworkId(networkId);
        if (publicIpv6AddressNetworkMapVO != null) {
            publicIpv6AddressNetworkMapVO = publicIpv6AddressNetworkMapDao.createForUpdate(publicIpv6AddressNetworkMapVO.getId());
            publicIpv6AddressNetworkMapVO.setState(PublicIpv6AddressNetworkMap.State.Free);
            publicIpv6AddressNetworkMapVO.setNetworkId(null);
            publicIpv6AddressNetworkMapDao.update(publicIpv6AddressNetworkMapVO.getId(), publicIpv6AddressNetworkMapVO);
        }
    }

    public FirewallRule updateIpv6FirewallRule(UpdateIpv6FirewallRuleCmd updateIpv6FirewallRuleCmd) {
        // TODO
        return firewallDao.findById(updateIpv6FirewallRuleCmd.getId());
    }

    @Override
    public Pair<List<? extends FirewallRule>, Integer> listIpv6FirewallRules(ListIpv6FirewallRulesCmd listIpv6FirewallRulesCmd) {
        return firewallService.listFirewallRules(listIpv6FirewallRulesCmd);
    }

    @Override
    public boolean revokeIpv6FirewallRule(Long id) {
        // TODO
        return true;
    }

    @Override
    public FirewallRule createIpv6FirewallRule(CreateIpv6FirewallRuleCmd createIpv6FirewallRuleCmd) {
        return null;
    }

    @Override
    public FirewallRule getIpv6FirewallRule(Long entityId) {
        return firewallDao.findById(entityId);
    }

    @Override
    public boolean applyIpv6FirewallRule(long id) {
        return false;
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
                        if ((new Date()).getTime() - subnet.getUpdated().getTime() < 30*60*1000) {
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

    public class Ipv6GuestNetworkRoutesLogger extends ManagedContextRunnable {
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
                List<NetworkVO> isolatedNetworks = networkDao.listByGuestType(Network.GuestType.Isolated);
                for (NetworkVO network : isolatedNetworks) {
                    if (Network.State.Implemented.equals(network.getState()) && networkOfferingDao.isIpv6Supported(network.getNetworkOfferingId())) {
                        PublicIpv6AddressNetworkMapVO ipv6AddressNetworkMap = publicIpv6AddressNetworkMapDao.findByNetworkId(network.getId());
                        if (ipv6AddressNetworkMap != null && s_logger.isInfoEnabled()) {
                            s_logger.info(String.format("Add upstream IPv6 route for %s via: %s", network.getIp6Cidr(), ipv6AddressNetworkMap.getIp6Address()));
                        }
                    }
                }
            } catch (Exception e) {
                s_logger.warn("Caught exception while logging IPv6 guest network routes: ", e);
            }
        }
    }
}
