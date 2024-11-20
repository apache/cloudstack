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

import java.util.Map;

import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.cloudstack.framework.config.dao.ConfigurationDao;

import com.cloud.configuration.Config;
import com.cloud.dc.DataCenter;
import com.cloud.dc.Vlan;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.VlanDao;
import com.cloud.event.EventTypes;
import com.cloud.event.UsageEventUtils;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.network.IpAddress.State;
import com.cloud.network.Network.IpAddresses;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.IPAddressVO;
import com.cloud.network.dao.NetworkDetailsDao;
import com.cloud.network.dao.UserIpv6AddressDao;
import com.cloud.user.Account;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.Pair;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.db.DB;
import com.cloud.utils.net.NetUtils;
import com.cloud.vm.NicProfile;
import com.cloud.vm.dao.NicSecondaryIpDao;
import com.cloud.vm.dao.NicSecondaryIpVO;
import com.googlecode.ipv6.IPv6Address;

public class Ipv6AddressManagerImpl extends ManagerBase implements Ipv6AddressManager {

    String _name = null;
    int _ipv6RetryMax = 0;

    @Inject
    DataCenterDao _dcDao;
    @Inject
    VlanDao _vlanDao;
    @Inject
    NetworkModel _networkModel;
    @Inject
    UserIpv6AddressDao _ipv6Dao;
    @Inject
    ConfigurationDao _configDao;
    @Inject
    IpAddressManager ipAddressManager;
    @Inject
    NicSecondaryIpDao nicSecondaryIpDao;
    @Inject
    IPAddressDao ipAddressDao;
    @Inject
    NetworkDetailsDao networkDetailsDao;

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        _name = name;
        Map<String, String> configs = _configDao.getConfiguration(params);
        _ipv6RetryMax = NumbersUtil.parseInt(configs.get(Config.NetworkIPv6SearchRetryMax.key()), 10000);
        return true;
    }

    /**
     * Executes method {@link #acquireGuestIpv6Address(Network, String)} and returns the requested IPv6 (String) in case of successfully allocating the guest IPv6 address.
     */
    @Override
    public String allocateGuestIpv6(Network network, String requestedIpv6) throws InsufficientAddressCapacityException {
        return acquireGuestIpv6Address(network, requestedIpv6);
    }

    /**
     * Allocates a guest IPv6 address for the guest NIC. It will throw exceptions in the following cases:
     * <ul>
     *    <li>there is no IPv6 address available in the network;</li>
     *    <li>IPv6 address is equals to the Gateway;</li>
     *    <li>the network offering is empty;</li>
     *    <li>the IPv6 address is not in the network;</li>
     *    <li>the requested IPv6 address is already in use in the network.</li>
     * </ul>
     */
    @Override
    @DB
    public String acquireGuestIpv6Address(Network network, String requestedIpv6) throws InsufficientAddressCapacityException {
        if (!_networkModel.areThereIPv6AddressAvailableInNetwork(network.getId())) {
            throw new InsufficientAddressCapacityException(
                    String.format("There is no IPv6 address available in the network [name=%s, network id=%s]", network.getName(), network.getId()), DataCenter.class,
                    network.getDataCenterId());
        }

        if (NetUtils.isIPv6EUI64(requestedIpv6)) {
            throw new InsufficientAddressCapacityException(String.format("Requested IPv6 address [%s] may not be a EUI-64 address", requestedIpv6), DataCenter.class,
                    network.getDataCenterId());
        }

        checkIfCanAllocateIpv6Address(network, requestedIpv6);

        IpAddresses requestedIpPair = new IpAddresses(null, requestedIpv6);
        _networkModel.checkRequestedIpAddresses(network.getId(), requestedIpPair);

        IPAddressVO ip = ipAddressDao.findByIpAndSourceNetworkId(network.getId(), requestedIpv6);
        if (ip != null) {
            State ipState = ip.getState();
            if (ipState != State.Free) {
                throw new InsufficientAddressCapacityException(String.format("Requested ip address [%s] is not free [ip state=%]", requestedIpv6, ipState), DataCenter.class,
                        network.getDataCenterId());
            }
        }
        return requestedIpv6;
    }

    /**
     * Allocates a public IPv6 address for the guest NIC. It will throw exceptions in the following cases:
     * <ul>
     *    <li>the requested IPv6 address is already in use in the network;</li>
     *    <li>IPv6 address is equals to the Gateway;</li>
     *    <li>the network offering is empty;</li>
     *    <li>the IPv6 address is not in the network.</li>
     * </ul>
     */
    @Override
    public String allocatePublicIp6ForGuestNic(Network network, Long podId, Account owner, String requestedIpv6) throws InsufficientAddressCapacityException {
        checkIfCanAllocateIpv6Address(network, requestedIpv6);

        return requestedIpv6;
    }

    /**
     * Performs some checks on the given IPv6 address. It will throw exceptions in the following cases:
     * <ul>
     *    <li>the requested IPv6 address is already in use in the network;</li>
     *    <li>IPv6 address is equals to the Gateway;</li>
     *    <li>the network offering is empty;</li>
     *    <li>the IPv6 address is not in the network.</li>
     * </ul>
     */
    protected void checkIfCanAllocateIpv6Address(Network network, String ipv6) throws InsufficientAddressCapacityException {
        if (isIp6Taken(network, ipv6)) {
            throw new InsufficientAddressCapacityException(
                    String.format("The IPv6 address [%s] is already in use in the network [id=%s, name=%s]", ipv6, network.getId(), network.getName()), Network.class,
                    network.getId());
        }

        if (ipAddressManager.isIpEqualsGatewayOrNetworkOfferingsEmpty(network, ipv6)) {
            throw new InvalidParameterValueException(
                    String.format("The network [id=%s] offering is empty or the requested IP address [%s] is equals to the Gateway", network.getId(), ipv6));
        }

        String networkIp6Cidr = network.getIp6Cidr();
        if (!NetUtils.isIp6InNetwork(ipv6, networkIp6Cidr)) {
            throw new InvalidParameterValueException(
                    String.format("The IPv6 address [%s] is not in the network [id=%s, name=%s, ipv6cidr=%s]", ipv6, network.getId(), network.getName(), network.getIp6Cidr()));
        }
    }

    /**
     * Returns false if the requested ipv6 address is taken by some VM, checking on the 'user_ipv6_address' table or 'nic_secondary_ips' table.
     */
    protected boolean isIp6Taken(Network network, String requestedIpv6) {
        UserIpv6AddressVO ip6Vo = _ipv6Dao.findByNetworkIdAndIp(network.getId(), requestedIpv6);
        NicSecondaryIpVO nicSecondaryIpVO = nicSecondaryIpDao.findByIp6AddressAndNetworkId(requestedIpv6, network.getId());
        return ip6Vo != null || nicSecondaryIpVO != null;
    }

    /**
     * Calculate the IPv6 Address the Instance will obtain using SLAAC and IPv6 EUI-64
     *
     * Linux, FreeBSD and Windows all calculate the same IPv6 address when configured properly. (SLAAC)
     *
     * Using Router Advertisements the routers in the network should announce the IPv6 CIDR which is configured
     * for the network.
     *
     * It is up to the network administrator to make sure the IPv6 Routers in the network are sending out Router Advertisements
     * with the correct IPv6 (Prefix, DNS, Lifetime) information.
     *
     * This way the NIC will be populated with a IPv6 address on which the Instance is reachable.
     *
     * This method calculates the IPv6 address the Instance will obtain and updates the Nic object with the correct
     * address information.
     */
    @Override
    public void setNicIp6Address(final NicProfile nic, final DataCenter dc, final Network network) throws InsufficientAddressCapacityException {
        if (network.getIp6Gateway() != null) {
            if (nic.getIPv6Address() == null) {
                logger.debug("Found IPv6 CIDR " + network.getIp6Cidr() + " for Network " + network);
                nic.setIPv6Cidr(network.getIp6Cidr());
                nic.setIPv6Gateway(network.getIp6Gateway());

                setNicPropertiesFromNetwork(nic, network);

                IPv6Address ipv6addr = NetUtils.EUI64Address(network.getIp6Cidr(), nic.getMacAddress());
                logger.info("Calculated IPv6 address " + ipv6addr + " using EUI-64 for NIC " + nic.getUuid());
                nic.setIPv6Address(ipv6addr.toString());

                if (nic.getIPv4Address() != null) {
                    nic.setFormat(Networks.AddressFormat.DualStack);
                } else {
                    nic.setFormat(Networks.AddressFormat.Ip6);
                }
                if (Network.GuestType.Isolated.equals(network.getGuestType())) {
                    final boolean usageHidden = networkDetailsDao.isNetworkUsageHidden(network.getId());
                    UsageEventUtils.publishUsageEvent(EventTypes.EVENT_NET_IP6_ASSIGN, network.getAccountId(), network.getDataCenterId(), 0L,
                            ipv6addr.toString(), false, Vlan.VlanType.VirtualNetwork.toString(), false, usageHidden,
                            IPv6Address.class.getName(), null);
                }
            }
            Pair<String, String> dns = _networkModel.getNetworkIp6Dns(network, dc);
            nic.setIPv6Dns1(dns.first());
            nic.setIPv6Dns2(dns.second());
        }
    }

    private void setNicPropertiesFromNetwork(NicProfile nic, Network network) throws InsufficientAddressCapacityException {
        if (nic.getBroadcastType() == null) {
            nic.setBroadcastType(network.getBroadcastDomainType());
        }
        if (nic.getBroadCastUri() == null) {
            nic.setBroadcastUri(network.getBroadcastUri());
        }
        if (nic.getMacAddress() == null) {
            nic.setMacAddress(_networkModel.getNextAvailableMacAddressInNetwork(network.getId()));
        }
    }
}
