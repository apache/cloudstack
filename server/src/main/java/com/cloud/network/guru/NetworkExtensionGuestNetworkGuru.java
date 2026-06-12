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

import java.util.List;

import javax.inject.Inject;

import com.cloud.dc.DataCenter;
import com.cloud.deploy.DeployDestination;
import com.cloud.deploy.DeploymentPlan;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.InsufficientVirtualNetworkCapacityException;
import com.cloud.network.Network;
import com.cloud.network.Network.GuestType;
import com.cloud.network.Network.State;
import com.cloud.network.PhysicalNetwork;
import com.cloud.network.dao.NetworkVO;
import com.cloud.offering.NetworkOffering;
import com.cloud.user.Account;
import com.cloud.vm.NicProfile;
import com.cloud.vm.ReservationContext;
import com.cloud.vm.VirtualMachineProfile;

import org.apache.cloudstack.extension.ExtensionHelper;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * Guest network guru for extension-backed providers on physical networks whose
 * isolation method is set to "Extension".
 *
 * <p>For <b>isolated</b> networks the allocation path is identical to
 * {@link GuestNetworkGuru}: a virtual CIDR is managed internally and IPs are
 * taken from that pool.</p>
 *
 * <p>For <b>shared</b> networks (upstream router as gateway) the
 * allocation is delegated entirely to {@link DirectNetworkGuru}</p>
 */
public class NetworkExtensionGuestNetworkGuru extends GuestNetworkGuru {

    @Inject
    protected ExtensionHelper extensionHelper;

    @Autowired
    @Qualifier("DirectNetworkGuru")
    protected DirectNetworkGuru directNetworkGuru;

    @Override
    protected boolean canHandle(NetworkOffering offering, DataCenter.NetworkType networkType, PhysicalNetwork physicalNetwork) {
        List<String> providers = networkOfferingServiceMapDao.getDistinctProviders(offering.getId());
        if (CollectionUtils.isEmpty(providers)) {
            return false;
        }

        return providers.stream().anyMatch(providerName ->
                extensionHelper.isNetworkExtensionProvider(providerName)
                        && physicalNetwork != null
                        && extensionHelper.usesNetworkExtensionIsolation(providerName));
    }

    /**
     * Designs the network and ensures the state is {@code Allocated}, never {@code Setup}.
     *
     * <p>{@link GuestNetworkGuru#design} sets the state to {@code Setup} when
     * {@code offering.isSpecifyVlan() && !offering.isPersistent()}, which means the
     * parent considers the physical network already in place and skips
     * {@code element.implement()} later.
     * For NetworkExtension-based networks Resetting the state to
     * {@code Allocated} here guarantees {@code implement-network} is always called.</p>
     */
    @Override
    public Network design(final NetworkOffering offering, final DeploymentPlan plan,
                          final Network userSpecified, final String name,
                          final Long vpcId, final Account owner) {
        final Network network = super.design(offering, plan, userSpecified, name, vpcId, owner);
        if (network == null) {
            return null;
        }
        if (network instanceof NetworkVO && network.getState() == State.Setup) {
            NetworkVO networkVO = (NetworkVO) network;
            networkVO.setState(State.Allocated);
            return networkVO;
        }
        return network;
    }

    @Override
    public Network implement(final Network network, final NetworkOffering offering, final DeployDestination dest, final ReservationContext context)
            throws InsufficientVirtualNetworkCapacityException {
        assert network.getState() == Network.State.Implementing : "Why are we implementing " + network;

        final long dcId = dest.getDataCenter().getId();

        // physical network id can be null in Guest Network in Basic zone
        Long physicalNetworkId = network.getPhysicalNetworkId();
        if (physicalNetworkId == null) {
            physicalNetworkId = _networkModel.findPhysicalNetworkId(dcId, offering.getTags(), offering.getTrafficType());
        }

        // The broadcast_domain_type and broadcast_uri are set by the extension script output
        final NetworkVO implemented = getNetworkVO(network, offering, physicalNetworkId);
        return implemented;
    }

    private static NetworkVO getNetworkVO(Network network, NetworkOffering offering, Long physicalNetworkId) {
        final NetworkVO implemented =
                new NetworkVO(network.getTrafficType(), network.getMode(), network.getBroadcastDomainType(), network.getNetworkOfferingId(), Network.State.Allocated,
                        network.getDataCenterId(), physicalNetworkId, offering.isRedundantRouter());

        if (network.getGateway() != null) {
            implemented.setGateway(network.getGateway());
        }
        if (network.getCidr() != null) {
            implemented.setCidr(network.getCidr());
        }
        if (network.getBroadcastUri() != null) {
            implemented.setBroadcastUri(network.getBroadcastUri());
        }
        return implemented;
    }

    /**
     * Allocates a NIC for the given network.
     *
     * <p>For <b>shared</b> networks the call is forwarded to
     * {@link DirectNetworkGuru#allocate}, which handles the
     * {@code user_ip_address} lookup, transaction wrapping, and MAC assignment
     * identically to how direct/shared networks work elsewhere in CloudStack.</p>
     *
     * <p>For <b>isolated</b> networks the standard
     * {@link GuestNetworkGuru#allocate} path is used unchanged.</p>
     */
    @Override
    public NicProfile allocate(final Network network, final NicProfile nic, final VirtualMachineProfile vm)
            throws InsufficientVirtualNetworkCapacityException, InsufficientAddressCapacityException {

        if (network.getGuestType() == GuestType.Shared) {
            // Delegate entirely to DirectNetworkGuru
            return directNetworkGuru.allocate(network, nic, vm);
        }

        // Isolated network: use the standard GuestNetworkGuru CIDR pool allocation.
        return super.allocate(network, nic, vm);
    }
}
