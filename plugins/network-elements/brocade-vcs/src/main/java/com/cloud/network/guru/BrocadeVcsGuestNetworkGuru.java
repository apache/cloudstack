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


import com.cloud.agent.AgentManager;
import com.cloud.agent.api.AssociateMacToNetworkAnswer;
import com.cloud.agent.api.AssociateMacToNetworkCommand;
import com.cloud.agent.api.CreateNetworkAnswer;
import com.cloud.agent.api.CreateNetworkCommand;
import com.cloud.agent.api.DeleteNetworkAnswer;
import com.cloud.agent.api.DeleteNetworkCommand;
import com.cloud.agent.api.DisassociateMacFromNetworkAnswer;
import com.cloud.agent.api.DisassociateMacFromNetworkCommand;
import com.cloud.dc.DataCenter;
import com.cloud.dc.DataCenter.NetworkType;
import com.cloud.deploy.DeployDestination;
import com.cloud.deploy.DeploymentPlan;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.InsufficientVirtualNetworkCapacityException;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.network.BrocadeVcsDeviceVO;
import com.cloud.network.BrocadeVcsNetworkVlanMappingVO;
import com.cloud.network.Network;
import com.cloud.network.Network.GuestType;
import com.cloud.network.Network.Service;
import com.cloud.network.Network.State;
import com.cloud.network.NetworkProfile;
import com.cloud.network.Networks.BroadcastDomainType;
import com.cloud.network.PhysicalNetwork;
import com.cloud.network.PhysicalNetwork.IsolationMethod;
import com.cloud.network.dao.BrocadeVcsDao;
import com.cloud.network.dao.BrocadeVcsNetworkVlanMappingDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.dao.PhysicalNetworkServiceProviderDao;
import com.cloud.network.dao.PhysicalNetworkVO;
import com.cloud.offering.NetworkOffering;
import com.cloud.offerings.dao.NetworkOfferingServiceMapDao;
import com.cloud.resource.ResourceManager;
import com.cloud.user.Account;
import com.cloud.vm.NicProfile;
import com.cloud.vm.ReservationContext;
import com.cloud.vm.VirtualMachineProfile;

import javax.inject.Inject;
import java.util.List;

public class BrocadeVcsGuestNetworkGuru extends GuestNetworkGuru {

    @Inject
    NetworkOfferingServiceMapDao _ntwkOfferingSrvcDao;
    @Inject
    ResourceManager _resourceMgr;
    @Inject
    AgentManager _agentMgr;
    @Inject
    protected BrocadeVcsDao _brocadeVcsDao;
    @Inject
    protected BrocadeVcsNetworkVlanMappingDao _brocadeVcsNetworkVlanDao;
    @Inject
    PhysicalNetworkServiceProviderDao _pNSPDao;
    @Inject
    HostDao _hostDao;

    public BrocadeVcsGuestNetworkGuru() {
        super();
        _isolationMethods = new IsolationMethod[] {new IsolationMethod("VCS")};
    }

    @Override
    protected boolean canHandle(NetworkOffering offering, final NetworkType networkType, final PhysicalNetwork physicalNetwork) {
        // This guru handles only Guest Isolated network that supports L2 connectivity service
        if (networkType == NetworkType.Advanced && isMyTrafficType(offering.getTrafficType()) && offering.getGuestType() == Network.GuestType.Isolated
                && isMyIsolationMethod(physicalNetwork) && _ntwkOfferingSrvcDao.areServicesSupportedByNetworkOffering(offering.getId(), Service.Connectivity)) {
            return true;
        } else {
            logger.trace("We only take care of Guest networks of type   " + GuestType.Isolated + " in zone of type " + NetworkType.Advanced);
            return false;
        }
    }

    @Override
    public Network design(NetworkOffering offering, DeploymentPlan plan, Network userSpecified, Account owner) {
        // Check of the isolation type of the related physical network is VLAN
        PhysicalNetworkVO physnet = _physicalNetworkDao.findById(plan.getPhysicalNetworkId());
        DataCenter dc = _dcDao.findById(plan.getDataCenterId());
        if (!canHandle(offering, dc.getNetworkType(), physnet)) {
            logger.debug("Refusing to design this network");
            return null;
        }
        logger.debug("Physical isolation type is VCS, asking GuestNetworkGuru to design this network");
        NetworkVO networkObject = (NetworkVO)super.design(offering, plan, userSpecified, owner);
        if (networkObject == null) {
            return null;
        }
        // Override the broadcast domain type
        networkObject.setBroadcastDomainType(BroadcastDomainType.Vcs);
        networkObject.setState(State.Allocated);

        return networkObject;
    }

    @Override
    public Network implement(Network network, NetworkOffering offering, DeployDestination dest, ReservationContext context) throws InsufficientVirtualNetworkCapacityException {
        assert (network.getState() == State.Implementing) : "Why are we implementing " + network;

        Network implemented = super.implement(network, offering, dest, context);

        int vlanTag = Integer.parseInt(BroadcastDomainType.getValue(implemented.getBroadcastUri()));

        //get physical network id
        Long physicalNetworkId = network.getPhysicalNetworkId();

        List<BrocadeVcsDeviceVO> devices = _brocadeVcsDao.listByPhysicalNetwork(physicalNetworkId);
        if (devices.isEmpty()) {
            logger.error("No Brocade VCS Switch on physical network " + physicalNetworkId);
            return null;
        }

        for (BrocadeVcsDeviceVO brocadeVcsDevice : devices) {
            HostVO brocadeVcsHost = _hostDao.findById(brocadeVcsDevice.getHostId());

            // create createNetworkCmd instance and agentMgr execute it.
            CreateNetworkCommand cmd = new CreateNetworkCommand(vlanTag, network.getId(), context.getDomain().getName() + "-" + context.getAccount().getAccountName());
            CreateNetworkAnswer answer = (CreateNetworkAnswer)_agentMgr.easySend(brocadeVcsHost.getId(), cmd);

            if (answer == null || !answer.getResult()) {
                logger.error("CreateNetworkCommand failed");
                logger.error("Unable to create network " + network.getId());
                return null;
            }

        }

        // Persist the network-vlan mapping from db
        BrocadeVcsNetworkVlanMappingVO brocadeVcsNetworkVlanMapping = new BrocadeVcsNetworkVlanMappingVO(network.getId(), vlanTag);
        _brocadeVcsNetworkVlanDao.persist(brocadeVcsNetworkVlanMapping);

        return implemented;
    }

    @Override
    public void reserve(NicProfile nic, Network network, VirtualMachineProfile vm, DeployDestination dest, ReservationContext context)
            throws InsufficientVirtualNetworkCapacityException, InsufficientAddressCapacityException {
        super.reserve(nic, network, vm, dest, context);

        DataCenter dc = _dcDao.findById(network.getDataCenterId());

        String interfaceMac = nic.getMacAddress();

        List<BrocadeVcsDeviceVO> devices = _brocadeVcsDao.listByPhysicalNetwork(network.getPhysicalNetworkId());
        if (devices.isEmpty()) {
            logger.error("No Brocade VCS Switch on physical network " + network.getPhysicalNetworkId());
            return;
        }
        for (BrocadeVcsDeviceVO brocadeVcsDevice : devices) {
            HostVO brocadeVcsHost = _hostDao.findById(brocadeVcsDevice.getHostId());

            // create AssociateMacToNetworkCmd instance and agentMgr execute it.
            AssociateMacToNetworkCommand cmd = new AssociateMacToNetworkCommand(network.getId(), interfaceMac, context.getDomain().getName() + "-"
                    + context.getAccount().getAccountName());
            AssociateMacToNetworkAnswer answer = (AssociateMacToNetworkAnswer)_agentMgr.easySend(brocadeVcsHost.getId(), cmd);

            if (answer == null || !answer.getResult()) {
                logger.error("AssociateMacToNetworkCommand failed");
                throw new InsufficientVirtualNetworkCapacityException("Unable to associate mac " + interfaceMac + " to network " + network.getId(), DataCenter.class, dc.getId());
            }
        }

    }

    @Override
    public void deallocate(Network network, NicProfile nic, VirtualMachineProfile vm) {

        String interfaceMac = nic.getMacAddress();

        List<BrocadeVcsDeviceVO> devices = _brocadeVcsDao.listByPhysicalNetwork(network.getPhysicalNetworkId());
        if (devices.isEmpty()) {
            logger.error("No Brocade VCS Switch on physical network " + network.getPhysicalNetworkId());
            return;
        }
        for (BrocadeVcsDeviceVO brocadeVcsDevice : devices) {
            HostVO brocadeVcsHost = _hostDao.findById(brocadeVcsDevice.getHostId());

            // create DisassociateMacFromNetworkCmd instance and agentMgr execute it.
            DisassociateMacFromNetworkCommand cmd = new DisassociateMacFromNetworkCommand(network.getId(), interfaceMac);
            DisassociateMacFromNetworkAnswer answer = (DisassociateMacFromNetworkAnswer)_agentMgr.easySend(brocadeVcsHost.getId(), cmd);

            if (answer == null || !answer.getResult()) {
                logger.error("DisassociateMacFromNetworkCommand failed");
                logger.error("Unable to disassociate mac " + interfaceMac + " from network " + network.getId());
                return;
            }
        }
        super.deallocate(network, nic, vm);
    }

    @Override
    public boolean release(NicProfile nic, VirtualMachineProfile vm, String reservationId) {
        return super.release(nic, vm, reservationId);
    }

    @Override
    public void shutdown(NetworkProfile profile, NetworkOffering offering) {

        super.shutdown(profile, offering);
    }

    @Override
    public boolean trash(Network network, NetworkOffering offering) {

        int vlanTag = 0;
        // Get the network-vlan mapping from db
        BrocadeVcsNetworkVlanMappingVO brocadeVcsNetworkVlanMapping = _brocadeVcsNetworkVlanDao.findByNetworkId(network.getId());

        if (brocadeVcsNetworkVlanMapping != null) {
            vlanTag = brocadeVcsNetworkVlanMapping.getVlanId();
        } else {
            logger.error("Not able to find vlanId for network " + network.getId());
            return false;
        }

        List<BrocadeVcsDeviceVO> devices = _brocadeVcsDao.listByPhysicalNetwork(network.getPhysicalNetworkId());
        if (devices.isEmpty()) {
            logger.error("No Brocade VCS Switch on physical network " + network.getPhysicalNetworkId());
            return false;
        }
        for (BrocadeVcsDeviceVO brocadeVcsDevice : devices) {
            HostVO brocadeVcsHost = _hostDao.findById(brocadeVcsDevice.getHostId());

            // create deleteNetworkCmd instance and agentMgr execute it.
            DeleteNetworkCommand cmd = new DeleteNetworkCommand(vlanTag, network.getId());
            DeleteNetworkAnswer answer = (DeleteNetworkAnswer)_agentMgr.easySend(brocadeVcsHost.getId(), cmd);

            if (answer == null || !answer.getResult()) {
                logger.error("DeleteNetworkCommand failed");
                logger.error("Unable to delete network " + network.getId());
                return false;
            }
        }

        // Remove the network-vlan mapping from db
        _brocadeVcsNetworkVlanDao.remove(brocadeVcsNetworkVlanMapping.getId());
        return super.trash(network, offering);
    }

}
