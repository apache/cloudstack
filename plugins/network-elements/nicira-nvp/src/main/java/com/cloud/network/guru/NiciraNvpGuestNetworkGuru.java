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

package com.cloud.network.guru;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import javax.ejb.Local;
import javax.inject.Inject;

import org.apache.log4j.Logger;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.CreateLogicalSwitchAnswer;
import com.cloud.agent.api.CreateLogicalSwitchCommand;
import com.cloud.agent.api.DeleteLogicalSwitchAnswer;
import com.cloud.agent.api.DeleteLogicalSwitchCommand;
import com.cloud.dc.DataCenter;
import com.cloud.dc.DataCenter.NetworkType;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.deploy.DeployDestination;
import com.cloud.deploy.DeploymentPlan;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.InsufficientVirtualNetworkCapacityException;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.host.dao.HostDetailsDao;
import com.cloud.network.Network;
import com.cloud.network.Network.GuestType;
import com.cloud.network.Network.Service;
import com.cloud.network.Network.State;
import com.cloud.network.NetworkModel;
import com.cloud.network.NetworkProfile;
import com.cloud.network.Networks.BroadcastDomainType;
import com.cloud.network.NiciraNvpDeviceVO;
import com.cloud.network.PhysicalNetwork;
import com.cloud.network.PhysicalNetwork.IsolationMethod;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.dao.NiciraNvpDao;
import com.cloud.network.dao.PhysicalNetworkDao;
import com.cloud.network.dao.PhysicalNetworkVO;
import com.cloud.offering.NetworkOffering;
import com.cloud.offerings.dao.NetworkOfferingServiceMapDao;
import com.cloud.resource.ResourceManager;
import com.cloud.user.Account;
import com.cloud.user.dao.AccountDao;
import com.cloud.vm.NicProfile;
import com.cloud.vm.ReservationContext;
import com.cloud.vm.VirtualMachineProfile;

@Local(value = NetworkGuru.class)
public class NiciraNvpGuestNetworkGuru extends GuestNetworkGuru {
    private static final int MAX_NAME_LENGTH = 40;

    private static final Logger s_logger = Logger.getLogger(NiciraNvpGuestNetworkGuru.class);

    @Inject
    protected NetworkModel networkModel;
    @Inject
    protected NetworkDao networkDao;
    @Inject
    protected DataCenterDao zoneDao;
    @Inject
    protected PhysicalNetworkDao physicalNetworkDao;
    @Inject
    protected AccountDao accountDao;
    @Inject
    protected NiciraNvpDao niciraNvpDao;
    @Inject
    protected HostDao hostDao;
    @Inject
    protected ResourceManager resourceMgr;
    @Inject
    protected AgentManager agentMgr;
    @Inject
    protected HostDetailsDao hostDetailsDao;
    @Inject
    protected NetworkOfferingServiceMapDao ntwkOfferingSrvcDao;

    public NiciraNvpGuestNetworkGuru() {
        super();
        _isolationMethods = new IsolationMethod[] { IsolationMethod.STT, IsolationMethod.VXLAN };
    }

    @Override
    protected boolean canHandle(final NetworkOffering offering, final NetworkType networkType, final PhysicalNetwork physicalNetwork) {
        // This guru handles only Guest Isolated network that supports Source nat service
        if (networkType == NetworkType.Advanced && isMyTrafficType(offering.getTrafficType()) && offering.getGuestType() == Network.GuestType.Isolated
                && isMyIsolationMethod(physicalNetwork) && ntwkOfferingSrvcDao.areServicesSupportedByNetworkOffering(offering.getId(), Service.Connectivity)) {
            return true;
        } else {
            s_logger.trace("We only take care of Guest networks of type   " + GuestType.Isolated + " in zone of type " + NetworkType.Advanced);
            return false;
        }
    }

    @Override
    public Network design(final NetworkOffering offering, final DeploymentPlan plan, final Network userSpecified, final Account owner) {
        // Check of the isolation type of the related physical network is supported
        final PhysicalNetworkVO physnet = physicalNetworkDao.findById(plan.getPhysicalNetworkId());
        final DataCenter dc = _dcDao.findById(plan.getDataCenterId());
        if (!canHandle(offering, dc.getNetworkType(), physnet)) {
            s_logger.debug("Refusing to design this network");
            return null;
        }

        final List<NiciraNvpDeviceVO> devices = niciraNvpDao.listByPhysicalNetwork(physnet.getId());
        if (devices.isEmpty()) {
            s_logger.error("No NiciraNvp Controller on physical network " + physnet.getName());
            return null;
        }
        s_logger.debug("Nicira Nvp " + devices.get(0).getUuid() + " found on physical network " + physnet.getId());

        s_logger.debug("Physical isolation type is supported, asking GuestNetworkGuru to design this network");
        final NetworkVO networkObject = (NetworkVO) super.design(offering, plan, userSpecified, owner);
        if (networkObject == null) {
            return null;
        }
        networkObject.setBroadcastDomainType(BroadcastDomainType.Lswitch);

        return networkObject;
    }

    @Override
    public Network implement(final Network network, final NetworkOffering offering, final DeployDestination dest, final ReservationContext context)
            throws InsufficientVirtualNetworkCapacityException {
        assert network.getState() == State.Implementing : "Why are we implementing " + network;

        final long dcId = dest.getDataCenter().getId();

        Long physicalNetworkId = network.getPhysicalNetworkId();

        // physical network id can be null in Guest Network in Basic zone, so locate the physical network
        if (physicalNetworkId == null) {
            physicalNetworkId = networkModel.findPhysicalNetworkId(dcId, offering.getTags(), offering.getTrafficType());
        }

        final NetworkVO implemented = new NetworkVO(network.getTrafficType(), network.getMode(), network.getBroadcastDomainType(), network.getNetworkOfferingId(),
                State.Allocated, network.getDataCenterId(), physicalNetworkId, offering.getRedundantRouter());

        if (network.getGateway() != null) {
            implemented.setGateway(network.getGateway());
        }

        if (network.getCidr() != null) {
            implemented.setCidr(network.getCidr());
        }

        // Name is either the given name or the uuid
        String name = network.getName();
        if (name == null || name.isEmpty()) {
            name = ((NetworkVO) network).getUuid();
        }
        if (name.length() > MAX_NAME_LENGTH) {
            name = name.substring(0, MAX_NAME_LENGTH - 1);
        }

        final List<NiciraNvpDeviceVO> devices = niciraNvpDao.listByPhysicalNetwork(physicalNetworkId);
        if (devices.isEmpty()) {
            s_logger.error("No NiciraNvp Controller on physical network " + physicalNetworkId);
            return null;
        }
        final NiciraNvpDeviceVO niciraNvpDevice = devices.get(0);
        final HostVO niciraNvpHost = hostDao.findById(niciraNvpDevice.getHostId());
        hostDao.loadDetails(niciraNvpHost);
        final String transportzoneuuid = niciraNvpHost.getDetail("transportzoneuuid");
        final String transportzoneisotype = niciraNvpHost.getDetail("transportzoneisotype");

        final CreateLogicalSwitchCommand cmd = new CreateLogicalSwitchCommand(transportzoneuuid, transportzoneisotype, name, context.getDomain().getName() + "-"
                + context.getAccount().getAccountName());
        final CreateLogicalSwitchAnswer answer = (CreateLogicalSwitchAnswer) agentMgr.easySend(niciraNvpHost.getId(), cmd);

        if (answer == null || !answer.getResult()) {
            s_logger.error("CreateLogicalSwitchCommand failed");
            return null;
        }

        try {
            implemented.setBroadcastUri(new URI("lswitch", answer.getLogicalSwitchUuid(), null));
            implemented.setBroadcastDomainType(BroadcastDomainType.Lswitch);
            s_logger.info("Implemented OK, network linked to  = " + implemented.getBroadcastUri().toString());
        } catch (final URISyntaxException e) {
            s_logger.error("Unable to store logical switch id in broadcast uri, uuid = " + implemented.getUuid(), e);
            return null;
        }

        return implemented;
    }

    @Override
    public void reserve(final NicProfile nic, final Network network, final VirtualMachineProfile vm, final DeployDestination dest, final ReservationContext context)
            throws InsufficientVirtualNetworkCapacityException, InsufficientAddressCapacityException {
        super.reserve(nic, network, vm, dest, context);
    }

    @Override
    public boolean release(final NicProfile nic, final VirtualMachineProfile vm, final String reservationId) {
        return super.release(nic, vm, reservationId);
    }

    @Override
    public void shutdown(final NetworkProfile profile, final NetworkOffering offering) {
        final NetworkVO networkObject = networkDao.findById(profile.getId());
        if (networkObject.getBroadcastDomainType() != BroadcastDomainType.Lswitch || networkObject.getBroadcastUri() == null) {
            s_logger.warn("BroadcastUri is empty or incorrect for guestnetwork " + networkObject.getDisplayText());
            return;
        }

        final List<NiciraNvpDeviceVO> devices = niciraNvpDao.listByPhysicalNetwork(networkObject.getPhysicalNetworkId());
        if (devices.isEmpty()) {
            s_logger.error("No NiciraNvp Controller on physical network " + networkObject.getPhysicalNetworkId());
            return;
        }
        final NiciraNvpDeviceVO niciraNvpDevice = devices.get(0);
        final HostVO niciraNvpHost = hostDao.findById(niciraNvpDevice.getHostId());

        final DeleteLogicalSwitchCommand cmd = new DeleteLogicalSwitchCommand(BroadcastDomainType.getValue(networkObject.getBroadcastUri()));
        final DeleteLogicalSwitchAnswer answer = (DeleteLogicalSwitchAnswer) agentMgr.easySend(niciraNvpHost.getId(), cmd);

        if (answer == null || !answer.getResult()) {
            s_logger.error("DeleteLogicalSwitchCommand failed");
        }

        super.shutdown(profile, offering);
    }

    @Override
    public boolean trash(final Network network, final NetworkOffering offering) {
        return super.trash(network, offering);
    }

}
