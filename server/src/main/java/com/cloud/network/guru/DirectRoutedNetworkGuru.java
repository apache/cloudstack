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

import org.apache.commons.lang3.StringUtils;

import com.cloud.dc.DataCenter;
import com.cloud.dc.DataCenter.NetworkType;
import com.cloud.deploy.DeployDestination;
import com.cloud.deploy.DeploymentPlan;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.InsufficientVirtualNetworkCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.network.Network;
import com.cloud.network.Network.GuestType;
import com.cloud.network.Network.State;
import com.cloud.network.Networks.BroadcastDomainType;
import com.cloud.network.Networks.Mode;
import com.cloud.network.PhysicalNetwork;
import com.cloud.network.PhysicalNetwork.IsolationMethod;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.dao.PhysicalNetworkVO;
import com.cloud.offering.NetworkOffering;
import com.cloud.user.Account;
import com.cloud.utils.net.NetUtils;
import com.cloud.vm.NicProfile;
import com.cloud.vm.ReservationContext;
import com.cloud.vm.VirtualMachineProfile;

/**
 * Network guru for L3 (Direct Routed) guest networks: the hypervisor routes a public IPv4
 * and/or IPv6 address directly to the Instance. There is no Virtual Router, no NAT and no
 * DHCP; the Instance learns its addressing exclusively from ConfigDrive.
 *
 * Address allocation is inherited from {@link DirectNetworkGuru}: the operator supplies a
 * subnet, CloudStack assigns individual addresses out of it. What differs is the form those
 * addresses take on the NIC — an IPv4 address is a /32 and an IPv6 address a /128, with the
 * shared, host-independent on-link gateway (169.254.0.1 / fe80::1) that every hypervisor
 * carries on the network's bridge.
 *
 * These networks live on a dedicated physical network whose isolation method is ROUTED — the
 * network operator's explicit, zone-level opt-in. Each network carries a broadcast domain of
 * type routed://&lt;id&gt;; the id is a label naming the per-network, uplink-less bridge on every
 * host (brdr-&lt;id&gt;), never an encapsulation. It is chosen by the operator at network creation
 * (specifyVlan offerings, via the vlan parameter) or allocated from the physical network's vnet
 * range (otherwise), and is stable for the network's life.
 */
public class DirectRoutedNetworkGuru extends DirectNetworkGuru {

    public DirectRoutedNetworkGuru() {
        super();
        // Selection follows the standard guru contract: a network is direct routed when its
        // offering's guest type is L3 AND its physical network carries the ROUTED isolation
        // method. The id conveyed in routed://<id> consumes nothing on the wire.
        _isolationMethods = new IsolationMethod[] {new IsolationMethod("ROUTED")};
    }

    @Override
    protected boolean canHandle(NetworkOffering offering, DataCenter dc, PhysicalNetwork physnet) {
        if (dc.getNetworkType() == NetworkType.Advanced && isMyTrafficType(offering.getTrafficType()) && offering.getGuestType() == GuestType.L3
                && isMyIsolationMethod(physnet)) {
            return true;
        }
        logger.trace("We only take care of {} guest networks in zones of type {} on physical networks with isolation method ROUTED", GuestType.L3, NetworkType.Advanced);
        return false;
    }

    @Override
    public Network design(NetworkOffering offering, DeploymentPlan plan, Network userSpecified, String name, Long vpcId, Account owner) {
        DataCenter dc = _dcDao.findById(plan.getDataCenterId());
        PhysicalNetworkVO physnet = _physicalNetworkDao.findById(plan.getPhysicalNetworkId());

        if (!canHandle(offering, dc, physnet)) {
            return null;
        }

        if (vpcId != null) {
            throw new InvalidParameterValueException(String.format("%s networks are never part of a VPC", GuestType.L3));
        }

        // Mode.Static: the Instance is statically configured via ConfigDrive, there is no DHCP.
        // BroadcastDomainType.Routed: routed://<id> names the per-network bridge (brdr-<id>);
        // no VLAN/VXLAN is consumed and nothing appears on the wire.
        NetworkVO config = new NetworkVO(offering.getTrafficType(), Mode.Static, BroadcastDomainType.Routed, offering.getId(), State.Allocated,
                plan.getDataCenterId(), plan.getPhysicalNetworkId(), false);

        if (userSpecified != null) {
            if ((userSpecified.getCidr() == null && userSpecified.getGateway() != null) || (userSpecified.getCidr() != null && userSpecified.getGateway() == null)) {
                throw new InvalidParameterValueException("cidr and gateway must be specified together.");
            }

            // The routed id — operator-specified or allocated from the physical network's vnet
            // range — arrives as the broadcast URI, stamped by the orchestrator before design.
            // It is set once here and never changes: the bridge name derives from it.
            if (userSpecified.getBroadcastUri() != null) {
                if (BroadcastDomainType.getSchemeValue(userSpecified.getBroadcastUri()) != BroadcastDomainType.Routed) {
                    throw new InvalidParameterValueException(String.format("A %s network requires a routed://<id> broadcast domain, got %s",
                            GuestType.L3, userSpecified.getBroadcastUri()));
                }
                config.setBroadcastUri(userSpecified.getBroadcastUri());
                config.setState(State.Setup);
            }

            if ((userSpecified.getIp6Cidr() == null && userSpecified.getIp6Gateway() != null) ||
                (userSpecified.getIp6Cidr() != null && userSpecified.getIp6Gateway() == null)) {
                throw new InvalidParameterValueException("cidrv6 and gatewayv6 must be specified together.");
            }

            // The subnet is an allocation pool routed to the hypervisors, not a broadcast domain.
            // Its gateway is stored but never used: the Instance's gateway is always the shared
            // link-local address.
            if (userSpecified.getCidr() != null) {
                config.setCidr(userSpecified.getCidr());
                config.setGateway(userSpecified.getGateway());
            }

            if (userSpecified.getIp6Cidr() != null) {
                config.setIp6Cidr(userSpecified.getIp6Cidr());
                config.setIp6Gateway(userSpecified.getIp6Gateway());
            }

            if (userSpecified.getPublicMtu() != null) {
                config.setPublicMtu(userSpecified.getPublicMtu());
            }
            if (userSpecified.getPrivateMtu() != null) {
                config.setPrivateMtu(userSpecified.getPrivateMtu());
            }

            if (StringUtils.isNotBlank(userSpecified.getDns1())) {
                config.setDns1(userSpecified.getDns1());
            }
            if (StringUtils.isNotBlank(userSpecified.getDns2())) {
                config.setDns2(userSpecified.getDns2());
            }
            if (StringUtils.isNotBlank(userSpecified.getIp6Dns1())) {
                config.setIp6Dns1(userSpecified.getIp6Dns1());
            }
            if (StringUtils.isNotBlank(userSpecified.getIp6Dns2())) {
                config.setIp6Dns2(userSpecified.getIp6Dns2());
            }
        }

        return config;
    }

    @Override
    public NicProfile allocate(Network network, NicProfile nic, VirtualMachineProfile vm) throws InsufficientVirtualNetworkCapacityException,
        InsufficientAddressCapacityException, ConcurrentOperationException {
        NicProfile profile = super.allocate(network, nic, vm);
        applyDirectRoutedAddressing(profile);
        return profile;
    }

    @Override
    public void reserve(NicProfile nic, Network network, VirtualMachineProfile vm, DeployDestination dest, ReservationContext context)
        throws InsufficientVirtualNetworkCapacityException, InsufficientAddressCapacityException, ConcurrentOperationException {
        super.reserve(nic, network, vm, dest, context);
        applyDirectRoutedAddressing(nic);
    }

    /**
     * The inherited allocation ({@code IpAddressManagerImpl.allocateDirectIp()}) sets the NIC's
     * gateway and netmask from the vlan row, as a Shared network needs. Here the address is a
     * host route, not a subnet membership: force the /32 (or /128) form and the shared on-link
     * gateway over whatever the vlan row provided. This is also the signature by which the KVM
     * agent recognises a direct routed NIC.
     */
    protected void applyDirectRoutedAddressing(NicProfile nic) {
        if (nic == null) {
            return;
        }
        // The inherited allocation labels the NIC's isolation URI vlan://<tag> from the vlan
        // row, as a Shared network needs; here there is no VLAN — the only isolation-shaped
        // fact about this NIC is its routed://<id> broadcast domain.
        if (nic.getBroadCastUri() != null && BroadcastDomainType.getSchemeValue(nic.getBroadCastUri()) == BroadcastDomainType.Routed) {
            nic.setIsolationUri(nic.getBroadCastUri());
        }
        if (nic.getIPv4Address() != null) {
            nic.setIPv4Netmask(NetUtils.IPV4_HOST_NETMASK);
            nic.setIPv4Gateway(NetUtils.getLinkLocalGateway());
        }
        if (nic.getIPv6Address() != null) {
            nic.setIPv6Cidr(nic.getIPv6Address() + "/" + NetUtils.IPV6_HOST_PREFIX_LENGTH);
            nic.setIPv6Gateway(NetUtils.getIpv6LinkLocalGateway());
        }
    }
}
