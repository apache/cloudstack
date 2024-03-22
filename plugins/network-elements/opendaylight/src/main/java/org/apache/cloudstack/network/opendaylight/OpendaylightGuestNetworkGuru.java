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

package org.apache.cloudstack.network.opendaylight;

import com.cloud.agent.AgentManager;
import com.cloud.dc.DataCenter;
import com.cloud.dc.DataCenter.NetworkType;
import com.cloud.deploy.DeployDestination;
import com.cloud.deploy.DeploymentPlan;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.InsufficientVirtualNetworkCapacityException;
import com.cloud.network.Network;
import com.cloud.network.Network.GuestType;
import com.cloud.network.Network.Provider;
import com.cloud.network.Network.Service;
import com.cloud.network.Network.State;
import com.cloud.network.NetworkModel;
import com.cloud.network.NetworkProfile;
import com.cloud.network.Networks.BroadcastDomainType;
import com.cloud.network.PhysicalNetwork;
import com.cloud.network.PhysicalNetwork.IsolationMethod;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.dao.PhysicalNetworkDao;
import com.cloud.network.dao.PhysicalNetworkVO;
import com.cloud.network.guru.GuestNetworkGuru;
import com.cloud.offering.NetworkOffering;
import com.cloud.offerings.dao.NetworkOfferingServiceMapDao;
import com.cloud.user.Account;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.NicProfile;
import com.cloud.vm.ReservationContext;
import com.cloud.vm.VirtualMachineProfile;
import org.apache.cloudstack.network.opendaylight.agent.commands.AddHypervisorCommand;
import org.apache.cloudstack.network.opendaylight.agent.commands.ConfigureNetworkCommand;
import org.apache.cloudstack.network.opendaylight.agent.commands.ConfigurePortCommand;
import org.apache.cloudstack.network.opendaylight.agent.commands.DestroyNetworkCommand;
import org.apache.cloudstack.network.opendaylight.agent.commands.DestroyPortCommand;
import org.apache.cloudstack.network.opendaylight.agent.responses.AddHypervisorAnswer;
import org.apache.cloudstack.network.opendaylight.agent.responses.ConfigureNetworkAnswer;
import org.apache.cloudstack.network.opendaylight.agent.responses.ConfigurePortAnswer;
import org.apache.cloudstack.network.opendaylight.agent.responses.DestroyNetworkAnswer;
import org.apache.cloudstack.network.opendaylight.agent.responses.DestroyPortAnswer;
import org.apache.cloudstack.network.opendaylight.dao.OpenDaylightControllerMappingDao;
import org.apache.cloudstack.network.opendaylight.dao.OpenDaylightControllerVO;

import javax.inject.Inject;
import java.util.List;
import java.util.UUID;

public class OpendaylightGuestNetworkGuru extends GuestNetworkGuru {

    @Inject
    protected NetworkOfferingServiceMapDao ntwkOfferingSrvcDao;
    @Inject
    PhysicalNetworkDao physicalNetworkDao;
    @Inject
    OpenDaylightControllerMappingDao openDaylightControllerMappingDao;
    @Inject
    NetworkModel networkModel;
    @Inject
    AgentManager agentManager;
    @Inject
    NetworkDao networkDao;

    public OpendaylightGuestNetworkGuru() {
        _isolationMethods = new IsolationMethod[] {new IsolationMethod("ODL")};
    }

    @Override
    protected boolean canHandle(NetworkOffering offering, NetworkType networkType, PhysicalNetwork physicalNetwork) {
        if (networkType == NetworkType.Advanced && isMyTrafficType(offering.getTrafficType()) && offering.getGuestType() == Network.GuestType.Isolated &&
                isMyIsolationMethod(physicalNetwork) && ntwkOfferingSrvcDao.areServicesSupportedByNetworkOffering(offering.getId(), Service.Connectivity)
                && ntwkOfferingSrvcDao.isProviderForNetworkOffering(offering.getId(), Provider.Opendaylight)) {
            return true;
        } else {
            logger.trace("We only take care of Guest networks of type   " + GuestType.Isolated + " in zone of type " + NetworkType.Advanced);
            return false;
        }
    }

    @Override
    public Network design(NetworkOffering offering, DeploymentPlan plan, Network userSpecified, String name, Long vpcId, Account owner) {
        PhysicalNetworkVO physnet = physicalNetworkDao.findById(plan.getPhysicalNetworkId());
        DataCenter dc = _dcDao.findById(plan.getDataCenterId());
        if (!canHandle(offering, dc.getNetworkType(), physnet)) {
            logger.debug("Refusing to design this network");
            return null;
        }

        List<OpenDaylightControllerVO> devices = openDaylightControllerMappingDao.listByPhysicalNetwork(physnet.getId());
        if (devices.isEmpty()) {
            logger.error("No Controller on physical network " + physnet.getName());
            return null;
        }
        logger.debug("Controller " + devices.get(0).getUuid() + " found on physical network " + physnet.getId());
        logger.debug("Physical isolation type is ODL, asking GuestNetworkGuru to design this network");

        NetworkVO networkObject = (NetworkVO)super.design(offering, plan, userSpecified, name, vpcId, owner);
        if (networkObject == null) {
            return null;
        }
        // Override the broadcast domain type
        networkObject.setBroadcastDomainType(BroadcastDomainType.OpenDaylight);

        return networkObject;
    }

    @Override
    public Network implement(Network network, NetworkOffering offering, DeployDestination dest, ReservationContext context) throws InsufficientVirtualNetworkCapacityException {
        assert (network.getState() == State.Implementing) : "Why are we implementing " + network;

        long dcId = dest.getDataCenter().getId();

        //get physical network id
        Long physicalNetworkId = network.getPhysicalNetworkId();

        // physical network id can be null in Guest Network in Basic zone, so locate the physical network
        if (physicalNetworkId == null) {
            physicalNetworkId = networkModel.findPhysicalNetworkId(dcId, offering.getTags(), offering.getTrafficType());
        }

        NetworkVO implemented = new NetworkVO(network.getTrafficType(), network.getMode(), network.getBroadcastDomainType(), network.getNetworkOfferingId(), State.Allocated,
                network.getDataCenterId(), physicalNetworkId, offering.isRedundantRouter());

        if (network.getGateway() != null) {
            implemented.setGateway(network.getGateway());
        }

        if (network.getCidr() != null) {
            implemented.setCidr(network.getCidr());
        }

        // Name is either the given name or the uuid
        String name = network.getName();
        if (name == null || name.isEmpty()) {
            name = ((NetworkVO)network).getUuid();
        }

        List<OpenDaylightControllerVO> devices = openDaylightControllerMappingDao.listByPhysicalNetwork(physicalNetworkId);
        if (devices.isEmpty()) {
            logger.error("No Controller on physical network " + physicalNetworkId);
            return null;
        }
        OpenDaylightControllerVO controller = devices.get(0);

        ConfigureNetworkCommand cmd = new ConfigureNetworkCommand(name, context.getAccount().getAccountName());
        ConfigureNetworkAnswer answer = (ConfigureNetworkAnswer)agentManager.easySend(controller.getHostId(), cmd);

        if (answer == null || !answer.getResult()) {
            logger.error("ConfigureNetworkCommand failed");
            return null;
        }

        implemented.setBroadcastUri(BroadcastDomainType.OpenDaylight.toUri(answer.getNetworkUuid()));
        implemented.setBroadcastDomainType(BroadcastDomainType.OpenDaylight);
        logger.info("Implemented OK, network linked to  = " + implemented.getBroadcastUri().toString());

        return implemented;
    }

    @Override
    public void reserve(NicProfile nic, Network network, VirtualMachineProfile vm, DeployDestination dest, ReservationContext context)
            throws InsufficientVirtualNetworkCapacityException, InsufficientAddressCapacityException {
        super.reserve(nic, network, vm, dest, context);

        //get physical network id
        Long physicalNetworkId = network.getPhysicalNetworkId();

        List<OpenDaylightControllerVO> devices = openDaylightControllerMappingDao.listByPhysicalNetwork(physicalNetworkId);
        if (devices.isEmpty()) {
            logger.error("No Controller on physical network " + physicalNetworkId);
            throw new InsufficientVirtualNetworkCapacityException("No OpenDaylight Controller configured for this network", dest.getPod().getId());
        }
        OpenDaylightControllerVO controller = devices.get(0);

        AddHypervisorCommand addCmd = new AddHypervisorCommand(dest.getHost().getUuid(), dest.getHost().getPrivateIpAddress());
        AddHypervisorAnswer addAnswer = (AddHypervisorAnswer)agentManager.easySend(controller.getHostId(), addCmd);
        if (addAnswer == null || !addAnswer.getResult()) {
            logger.error("Failed to add " + dest.getHost().getName() + " as a node to the controller");
            throw new InsufficientVirtualNetworkCapacityException("Failed to add destination hypervisor to the OpenDaylight Controller", dest.getPod().getId());
        }

        ConfigurePortCommand cmd = new ConfigurePortCommand(UUID.fromString(nic.getUuid()), UUID.fromString(BroadcastDomainType.getValue(network.getBroadcastUri())), context
                .getAccount().getAccountName(), nic.getMacAddress());
        ConfigurePortAnswer answer = (ConfigurePortAnswer)agentManager.easySend(controller.getHostId(), cmd);

        if (answer == null || !answer.getResult()) {
            logger.error("ConfigureNetworkCommand failed");
            throw new InsufficientVirtualNetworkCapacityException("Failed to configure the port on the OpenDaylight Controller", dest.getPod().getId());
        }

    }

    @Override
    public boolean release(NicProfile nic, VirtualMachineProfile vm, String reservationId) {
        boolean success = super.release(nic, vm, reservationId);

        if (success) {
            //get physical network id
            NetworkVO network = _networkDao.findById(nic.getNetworkId());
            Long physicalNetworkId = network.getPhysicalNetworkId();

            List<OpenDaylightControllerVO> devices = openDaylightControllerMappingDao.listByPhysicalNetwork(physicalNetworkId);
            if (devices.isEmpty()) {
                logger.error("No Controller on physical network " + physicalNetworkId);
                throw new CloudRuntimeException("No OpenDaylight controller on this physical network");
            }
            OpenDaylightControllerVO controller = devices.get(0);

            DestroyPortCommand cmd = new DestroyPortCommand(UUID.fromString(nic.getUuid()));
            DestroyPortAnswer answer = (DestroyPortAnswer)agentManager.easySend(controller.getHostId(), cmd);

            if (answer == null || !answer.getResult()) {
                logger.error("DestroyPortCommand failed");
                success = false;
            }
        }

        return success;
    }

    @Override
    public void shutdown(NetworkProfile profile, NetworkOffering offering) {
        NetworkVO networkObject = networkDao.findById(profile.getId());
        if (networkObject.getBroadcastDomainType() != BroadcastDomainType.OpenDaylight || networkObject.getBroadcastUri() == null) {
            logger.warn("BroadcastUri is empty or incorrect for guestnetwork " + networkObject.getDisplayText());
            return;
        }

        List<OpenDaylightControllerVO> devices = openDaylightControllerMappingDao.listByPhysicalNetwork(networkObject.getPhysicalNetworkId());
        if (devices.isEmpty()) {
            logger.error("No Controller on physical network " + networkObject.getPhysicalNetworkId());
            return;
        }
        OpenDaylightControllerVO controller = devices.get(0);

        DestroyNetworkCommand cmd = new DestroyNetworkCommand(BroadcastDomainType.getValue(networkObject.getBroadcastUri()));
        DestroyNetworkAnswer answer = (DestroyNetworkAnswer)agentManager.easySend(controller.getHostId(), cmd);

        if (answer == null || !answer.getResult()) {
            logger.error("DestroyNetworkCommand failed");
        }

        super.shutdown(profile, offering);
    }

    @Override
    public boolean trash(Network network, NetworkOffering offering) {
        // TODO Auto-generated method stub
        return super.trash(network, offering);
    }

}
