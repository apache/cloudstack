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
package com.cloud.network.guru;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;

import com.cloud.dc.dao.VlanDetailsDao;
import com.cloud.network.vpc.dao.VpcDao;
import com.cloud.network.vpc.dao.VpcOfferingDao;
import com.cloud.network.vpc.dao.VpcOfferingServiceMapDao;
import org.apache.cloudstack.engine.orchestration.service.NetworkOrchestrationService;

import com.cloud.dc.DataCenter;
import com.cloud.dc.Vlan.VlanType;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.VlanDao;
import com.cloud.deploy.DeployDestination;
import com.cloud.deploy.DeploymentPlan;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.InsufficientVirtualNetworkCapacityException;
import com.cloud.network.IpAddressManager;
import com.cloud.network.Ipv6Service;
import com.cloud.network.Network;
import com.cloud.network.Network.State;
import com.cloud.network.NetworkModel;
import com.cloud.network.NetworkProfile;
import com.cloud.network.Networks.AddressFormat;
import com.cloud.network.Networks.BroadcastDomainType;
import com.cloud.network.Networks.IsolationType;
import com.cloud.network.Networks.Mode;
import com.cloud.network.Networks.TrafficType;
import com.cloud.network.addr.PublicIp;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.IPAddressVO;
import com.cloud.network.dao.NetworkVO;
import com.cloud.offering.NetworkOffering;
import com.cloud.user.Account;
import com.cloud.utils.Pair;
import com.cloud.utils.component.AdapterBase;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallbackNoReturn;
import com.cloud.utils.db.TransactionStatus;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.net.NetUtils;
import com.cloud.vm.Nic.ReservationStrategy;
import com.cloud.vm.NicProfile;
import com.cloud.vm.ReservationContext;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineProfile;
import com.googlecode.ipv6.IPv6Address;

public class PublicNetworkGuru extends AdapterBase implements NetworkGuru {

    @Inject
    DataCenterDao _dcDao;
    @Inject
    VlanDao _vlanDao;
    @Inject
    NetworkOrchestrationService _networkMgr;
    @Inject
    protected IPAddressDao _ipAddressDao;
    @Inject
    IpAddressManager _ipAddrMgr;
    @Inject
    Ipv6Service ipv6Service;
    @Inject
    protected NetworkModel networkModel;
    @Inject
    protected VpcDao vpcDao;
    @Inject
    protected VlanDetailsDao vlanDetailsDao;
    @Inject
    protected VpcOfferingDao vpcOfferingDao;
    @Inject
    protected VpcOfferingServiceMapDao vpcOfferingServiceMapDao;

    private static final TrafficType[] TrafficTypes = {TrafficType.Public};

    @Override
    public boolean isMyTrafficType(TrafficType type) {
        for (TrafficType t : TrafficTypes) {
            if (t == type) {
                return true;
            }
        }
        return false;
    }

    @Override
    public TrafficType[] getSupportedTrafficType() {
        return TrafficTypes;
    }

    protected boolean canHandle(NetworkOffering offering) {
        return isMyTrafficType(offering.getTrafficType()) && offering.isSystemOnly();
    }

    @Override
    public Network design(NetworkOffering offering, DeploymentPlan plan, Network network, String name, Long vpcId, Account owner) {
        if (!canHandle(offering)) {
            return null;
        }

        if (offering.getTrafficType() == TrafficType.Public) {
            NetworkVO ntwk =
                new NetworkVO(offering.getTrafficType(), Mode.Static, network.getBroadcastDomainType(), offering.getId(), State.Setup, plan.getDataCenterId(),
                    plan.getPhysicalNetworkId(), offering.isRedundantRouter());
            return ntwk;
        } else {
            return null;
        }
    }

    @Override
    public void setup(Network network, long networkId) {
        // do nothing
    }

    protected PublicNetworkGuru() {
        super();
    }

    protected void getIp(NicProfile nic, DataCenter dc, VirtualMachineProfile vm, Network network) throws InsufficientVirtualNetworkCapacityException,
        InsufficientAddressCapacityException, ConcurrentOperationException {
        if (nic.getIPv4Address() == null) {
            boolean forSystemVms = false;
            if (vm.getType().equals(VirtualMachine.Type.ConsoleProxy) || vm.getType().equals(VirtualMachine.Type.SecondaryStorageVm)) {
                forSystemVms = true;
            }
            PublicIp ip = _ipAddrMgr.assignPublicIpAddress(dc.getId(), null, vm.getOwner(), VlanType.VirtualNetwork, null, null, forSystemVms, forSystemVms);
            nic.setIPv4Address(ip.getAddress().toString());
            nic.setIPv4Gateway(ip.getGateway());
            nic.setIPv4Netmask(ip.getNetmask());
            if (isRoutedRange(ip)) {
                // Direct routed public range (SystemVMs on a ROUTED physical network): the
                // address is a host route, not a subnet membership. Same form as a guest NIC on
                // a direct routed network: /32 with the shared on-link gateway, and the
                // routed://<id> broadcast domain that names the bridge on the host.
                nic.setIPv4Netmask(NetUtils.IPV4_HOST_NETMASK);
                nic.setIPv4Gateway(NetUtils.getLinkLocalGateway());
                nic.setIsolationUri(BroadcastDomainType.Routed.toUri(ip.getVlanTag()));
                nic.setBroadcastUri(BroadcastDomainType.Routed.toUri(ip.getVlanTag()));
                nic.setBroadcastType(BroadcastDomainType.Routed);
                setRoutedRangeIpv6(nic, dc, ip, network);
            } else if (network.getBroadcastDomainType() == BroadcastDomainType.Vxlan) {
                nic.setIsolationUri(BroadcastDomainType.Vxlan.toUri(ip.getVlanTag()));
                nic.setBroadcastUri(BroadcastDomainType.Vxlan.toUri(ip.getVlanTag()));
                nic.setBroadcastType(BroadcastDomainType.Vxlan);
            } else {
                nic.setIsolationUri(IsolationType.Vlan.toUri(ip.getVlanTag()));
                nic.setBroadcastUri(BroadcastDomainType.Vlan.toUri(ip.getVlanTag()));
                nic.setBroadcastType(BroadcastDomainType.Vlan);
            }
            nic.setFormat(AddressFormat.Ip4);
            nic.setReservationId(String.valueOf(ip.getVlanTag()));
            nic.setMacAddress(ip.getMacAddress());
        }

        Pair<String, String> dns = networkModel.getNetworkIp4Dns(network, dc);
        nic.setIPv4Dns1(dns.first());
        nic.setIPv4Dns2(dns.second());

        ipv6Service.updateNicIpv6(nic, dc, network);
    }

    /**
     * A public IP range on a ROUTED physical network is created with routed://<id> as its "vlan";
     * the tag flowing through the vlan row is what marks the addresses it holds as direct routed.
     */
    private boolean isRoutedRange(PublicIp ip) {
        String vlanTag = ip.getVlanTag();
        return vlanTag != null && vlanTag.startsWith(BroadcastDomainType.Routed.scheme() + "://");
    }

    /**
     * IPv6 on a direct routed public range is computed with EUI-64 from the range's subnet and
     * the NIC's MAC — the same stateless model guest NICs use (no pool, no reservation, only the
     * result on the NIC) — and takes host-route form: a /128 with the shared link-local gateway.
     *
     * Deliberately not {@code ipv6Service.updateNicIpv6()}: that path is gated on the public
     * network offering's internet protocol, and its per-network placeholder reservation would
     * hand every SystemVM on the shared Public network the same address. Neither the gate nor
     * the reservation has anything to decide here — the MAC makes the address unique.
     */
    private void setRoutedRangeIpv6(NicProfile nic, DataCenter dc, PublicIp ip, Network network) {
        String ip6Cidr = ip.vlan() != null ? ip.vlan().getIp6Cidr() : null;
        if (StringUtils.isBlank(ip6Cidr) || nic.getIPv6Address() != null) {
            return;
        }
        IPv6Address ipv6Address = NetUtils.EUI64Address(ip6Cidr, nic.getMacAddress());
        logger.info("Calculated IPv6 address {} using EUI-64 for direct routed public NIC {}", ipv6Address, nic);
        nic.setIPv6Address(ipv6Address.toString());
        nic.setIPv6Cidr(ipv6Address + "/" + NetUtils.IPV6_HOST_PREFIX_LENGTH);
        nic.setIPv6Gateway(NetUtils.getIpv6LinkLocalGateway());
        nic.setFormat(AddressFormat.DualStack);
        Pair<String, String> ip6Dns = networkModel.getNetworkIp6Dns(network, dc);
        nic.setIPv6Dns1(ip6Dns.first());
        nic.setIPv6Dns2(ip6Dns.second());
    }

    @Override
    public void updateNicProfile(NicProfile profile, Network network) {
        DataCenter dc = _dcDao.findById(network.getDataCenterId());
        Pair<String, String> dns = networkModel.getNetworkIp4Dns(network, dc);
        Pair<String, String> ip6Dns = networkModel.getNetworkIp6Dns(network, dc);
        if (profile != null) {
            profile.setIPv4Dns1(dns.first());
            profile.setIPv4Dns2(dns.second());
            profile.setIPv6Dns1(ip6Dns.first());
            profile.setIPv6Dns2(ip6Dns.second());
        }
    }

    @Override
    public NicProfile allocate(Network network, NicProfile nic, VirtualMachineProfile vm) throws InsufficientVirtualNetworkCapacityException,
        InsufficientAddressCapacityException, ConcurrentOperationException {

        DataCenter dc = _dcDao.findById(network.getDataCenterId());

        if (nic != null && nic.getRequestedIPv4() != null) {
            throw new CloudRuntimeException("Does not support custom ip allocation at this time: " + nic);
        }

        if (nic == null) {
            nic = new NicProfile(ReservationStrategy.Create, null, null, null, null);
        }

        getIp(nic, dc, vm, network);

        if (nic.getIPv4Address() == null) {
            nic.setReservationStrategy(ReservationStrategy.Start);
        } else if (vm.getVirtualMachine().getType() == VirtualMachine.Type.DomainRouter) {
            nic.setReservationStrategy(ReservationStrategy.Managed);
        } else {
            nic.setReservationStrategy(ReservationStrategy.Create);
        }

        return nic;
    }

    @Override
    public void reserve(NicProfile nic, Network network, VirtualMachineProfile vm, DeployDestination dest, ReservationContext context)
        throws InsufficientVirtualNetworkCapacityException, InsufficientAddressCapacityException, ConcurrentOperationException {
        if (nic.getIPv4Address() == null) {
            getIp(nic, dest.getDataCenter(), vm, network);
        }
    }

    @Override
    public boolean release(NicProfile nic, VirtualMachineProfile vm, String reservationId) {
        return true;
    }

    @Override
    public Network implement(Network network, NetworkOffering offering, DeployDestination destination, ReservationContext context)
        throws InsufficientVirtualNetworkCapacityException {
        return network;
    }

    @Override
    @DB
    public void deallocate(Network network, NicProfile nic, VirtualMachineProfile vm) {
        if (logger.isDebugEnabled()) {
            logger.debug("Public Network deallocate Network: " + nic.getNetworkId() + ", IP: " + nic.getIPv4Address());
        }

        final IPAddressVO ip = _ipAddressDao.findByIpAndSourceNetworkId(nic.getNetworkId(), nic.getIPv4Address());
        if (ip != null && nic.getReservationStrategy() != ReservationStrategy.Managed) {
            Transaction.execute(new TransactionCallbackNoReturn() {
                @Override
                public void doInTransactionWithoutResult(TransactionStatus status) {
                    _ipAddrMgr.markIpAsUnavailable(ip.getId());
                    _ipAddressDao.unassignIpAddress(ip.getId());
                }
            });
        }
        nic.deallocate();

        if (logger.isDebugEnabled()) {
            logger.debug("Deallocated nic: " + nic);
        }
    }

    @Override
    public void shutdown(NetworkProfile network, NetworkOffering offering) {
    }

    @Override
    public boolean trash(Network network, NetworkOffering offering) {
        return true;
    }

    @Override
    public void updateNetworkProfile(NetworkProfile networkProfile) {
        DataCenter dc = _dcDao.findById(networkProfile.getDataCenterId());
        Network network = networkModel.getNetwork(networkProfile.getId());
        Pair<String, String> dns = networkModel.getNetworkIp4Dns(network, dc);
        networkProfile.setDns1(dns.first());
        networkProfile.setDns2(dns.second());
        dns = networkModel.getNetworkIp6Dns(network, dc);
        networkProfile.setIp6Dns1(dns.first());
        networkProfile.setIp6Dns2(dns.second());
    }

}
