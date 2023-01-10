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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;


import com.cloud.agent.AgentManager;
import com.cloud.agent.api.CreateLogicalSwitchAnswer;
import com.cloud.agent.api.CreateLogicalSwitchCommand;
import com.cloud.agent.api.DeleteLogicalRouterPortAnswer;
import com.cloud.agent.api.DeleteLogicalRouterPortCommand;
import com.cloud.agent.api.DeleteLogicalSwitchAnswer;
import com.cloud.agent.api.DeleteLogicalSwitchCommand;
import com.cloud.agent.api.FindL2GatewayServiceAnswer;
import com.cloud.agent.api.FindL2GatewayServiceCommand;
import com.cloud.agent.api.FindLogicalRouterPortAnswer;
import com.cloud.agent.api.FindLogicalRouterPortCommand;
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
import com.cloud.network.NiciraNvpNicMappingVO;
import com.cloud.network.NiciraNvpRouterMappingVO;
import com.cloud.network.PhysicalNetwork;
import com.cloud.network.PhysicalNetwork.IsolationMethod;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.dao.NiciraNvpDao;
import com.cloud.network.dao.NiciraNvpNicMappingDao;
import com.cloud.network.dao.NiciraNvpRouterMappingDao;
import com.cloud.network.dao.PhysicalNetworkDao;
import com.cloud.network.dao.PhysicalNetworkVO;
import com.cloud.network.nicira.L2GatewayServiceConfig;
import com.cloud.offering.NetworkOffering;
import com.cloud.offerings.dao.NetworkOfferingServiceMapDao;
import com.cloud.resource.ResourceManager;
import com.cloud.user.Account;
import com.cloud.user.dao.AccountDao;
import com.cloud.vm.NicProfile;
import com.cloud.vm.ReservationContext;
import com.cloud.vm.VirtualMachineProfile;

public class NiciraNvpGuestNetworkGuru extends GuestNetworkGuru implements NetworkGuruAdditionalFunctions{
    private static final int MAX_NAME_LENGTH = 40;


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
    @Inject
    protected NiciraNvpRouterMappingDao niciraNvpRouterMappingDao;
    @Inject
    protected NiciraNvpNicMappingDao niciraNvpNicMappingDao;

    public NiciraNvpGuestNetworkGuru() {
        super();
        _isolationMethods = new IsolationMethod[] { new IsolationMethod("STT", "NiciraNvp"), new IsolationMethod("VXLAN","NiciraNvp") };
    }

    @Override
    protected boolean canHandle(final NetworkOffering offering, final NetworkType networkType, final PhysicalNetwork physicalNetwork) {
        // This guru handles only Guest Isolated network that supports Source nat service
        if (networkType == NetworkType.Advanced && isMyTrafficType(offering.getTrafficType())
                && supportedGuestTypes(offering, Network.GuestType.Isolated, Network.GuestType.Shared)
                && isMyIsolationMethod(physicalNetwork) && ntwkOfferingSrvcDao.areServicesSupportedByNetworkOffering(offering.getId(), Service.Connectivity)) {
            return true;
        } else {
            return false;
        }
    }

    private boolean supportedGuestTypes(NetworkOffering offering, GuestType... types) {
        for (GuestType guestType : types) {
            if (offering.getGuestType().equals(guestType)){
                return true;
            }
        }
        return false;
    }

    @Override
    public Network design(final NetworkOffering offering, final DeploymentPlan plan, final Network userSpecified, final Account owner) {
        // Check of the isolation type of the related physical network is supported
        final PhysicalNetworkVO physnet = physicalNetworkDao.findById(plan.getPhysicalNetworkId());
        final DataCenter dc = _dcDao.findById(plan.getDataCenterId());
        if (!canHandle(offering, dc.getNetworkType(), physnet)) {
            logger.debug("Refusing to design this network");
            return null;
        }

        final List<NiciraNvpDeviceVO> devices = niciraNvpDao.listByPhysicalNetwork(physnet.getId());
        if (devices.isEmpty()) {
            logger.error("No NiciraNvp Controller on physical network " + physnet.getName());
            return null;
        }
        logger.debug("Nicira Nvp " + devices.get(0).getUuid() + " found on physical network " + physnet.getId());

        logger.debug("Physical isolation type is supported, asking GuestNetworkGuru to design this network");
        final NetworkVO networkObject = (NetworkVO) super.design(offering, plan, userSpecified, owner);
        if (networkObject == null) {
            return null;
        }
        networkObject.setBroadcastDomainType(BroadcastDomainType.Lswitch);
        if (offering.getGuestType().equals(GuestType.Shared)){
            networkObject.setState(State.Allocated);
        }

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
                State.Allocated, network.getDataCenterId(), physicalNetworkId, offering.isRedundantRouter());

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
            logger.error("No NiciraNvp Controller on physical network " + physicalNetworkId);
            return null;
        }
        final NiciraNvpDeviceVO niciraNvpDevice = devices.get(0);
        final HostVO niciraNvpHost = hostDao.findById(niciraNvpDevice.getHostId());
        hostDao.loadDetails(niciraNvpHost);
        final String transportzoneuuid = niciraNvpHost.getDetail("transportzoneuuid");
        final String transportzoneisotype = niciraNvpHost.getDetail("transportzoneisotype");

        if (offering.getGuestType().equals(GuestType.Shared)) {
            try {
                checkL2GatewayServiceSharedNetwork(niciraNvpHost);
            }
            catch (Exception e){
                logger.error("L2 Gateway Service Issue: " + e.getMessage());
                return null;
            }
        }

        final CreateLogicalSwitchCommand cmd = new CreateLogicalSwitchCommand(transportzoneuuid, transportzoneisotype, name, context.getDomain().getName() + "-"
                + context.getAccount().getAccountName());
        final CreateLogicalSwitchAnswer answer = (CreateLogicalSwitchAnswer) agentMgr.easySend(niciraNvpHost.getId(), cmd);

        if (answer == null || !answer.getResult()) {
            logger.error("CreateLogicalSwitchCommand failed");
            return null;
        }

        try {
            implemented.setBroadcastUri(new URI("lswitch", answer.getLogicalSwitchUuid(), null));
            implemented.setBroadcastDomainType(BroadcastDomainType.Lswitch);
            logger.info("Implemented OK, network linked to  = " + implemented.getBroadcastUri().toString());
        } catch (final URISyntaxException e) {
            logger.error("Unable to store logical switch id in broadcast uri, uuid = " + implemented.getUuid(), e);
            return null;
        }

        return implemented;
    }

    private void checkL2GatewayServiceSharedNetwork(HostVO niciraNvpHost) throws Exception {
        String l2GatewayServiceUuid = niciraNvpHost.getDetail("l2gatewayserviceuuid");
        if (l2GatewayServiceUuid == null){
            throw new Exception("No L2 Gateway Service found");
        }
        else {
            final FindL2GatewayServiceCommand cmdL2GWService = new FindL2GatewayServiceCommand(new L2GatewayServiceConfig(l2GatewayServiceUuid));
            final FindL2GatewayServiceAnswer answerL2GWService = (FindL2GatewayServiceAnswer) agentMgr.easySend(niciraNvpHost.getId(), cmdL2GWService);
            if (answerL2GWService == null || !answerL2GWService.getResult()){
                throw new Exception("No L2 Gateway Service found with uuid " + l2GatewayServiceUuid);
            }
            else {
                String uuidFound = answerL2GWService.getGatewayServiceUuid();
                if (! uuidFound.equals(l2GatewayServiceUuid)){
                    throw new Exception("Found L2 Gateway Service " + uuidFound + " instead of " + l2GatewayServiceUuid);
                }
            }
        }
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
            logger.warn("BroadcastUri is empty or incorrect for guestnetwork " + networkObject.getDisplayText());
            return;
        }

        final List<NiciraNvpDeviceVO> devices = niciraNvpDao.listByPhysicalNetwork(networkObject.getPhysicalNetworkId());
        if (devices.isEmpty()) {
            logger.error("No NiciraNvp Controller on physical network " + networkObject.getPhysicalNetworkId());
            return;
        }
        final NiciraNvpDeviceVO niciraNvpDevice = devices.get(0);
        final HostVO niciraNvpHost = hostDao.findById(niciraNvpDevice.getHostId());

        String logicalSwitchUuid = BroadcastDomainType.getValue(networkObject.getBroadcastUri());

        if (offering.getGuestType().equals(GuestType.Shared)){
            sharedNetworksCleanup(networkObject, logicalSwitchUuid, niciraNvpHost);
        }

        final DeleteLogicalSwitchCommand cmd = new DeleteLogicalSwitchCommand(logicalSwitchUuid);
        final DeleteLogicalSwitchAnswer answer = (DeleteLogicalSwitchAnswer) agentMgr.easySend(niciraNvpHost.getId(), cmd);

        if (answer == null || !answer.getResult()) {
            logger.error("DeleteLogicalSwitchCommand failed");
        }

        super.shutdown(profile, offering);
    }

    private void sharedNetworksCleanup(NetworkVO networkObject, String logicalSwitchUuid, HostVO niciraNvpHost) {
        NiciraNvpRouterMappingVO routermapping = niciraNvpRouterMappingDao.findByNetworkId(networkObject.getId());
        if (routermapping == null) {
            // Case 1: Numerical Vlan Provided -> No lrouter used.
            logger.info("Shared Network " + networkObject.getDisplayText() + " didn't use Logical Router");
        }
        else {
            //Case 2: Logical Router's UUID provided as Vlan id -> Remove lrouter port but not lrouter.
            String lRouterUuid = routermapping.getLogicalRouterUuid();
            logger.debug("Finding Logical Router Port on Logical Router " + lRouterUuid + " with attachment_lswitch_uuid=" + logicalSwitchUuid + " to delete it");
            final FindLogicalRouterPortCommand cmd = new FindLogicalRouterPortCommand(lRouterUuid, logicalSwitchUuid);
            final FindLogicalRouterPortAnswer answer = (FindLogicalRouterPortAnswer) agentMgr.easySend(niciraNvpHost.getId(), cmd);

            if (answer != null && answer.getResult()) {
                String logicalRouterPortUuid = answer.getLogicalRouterPortUuid();
                logger.debug("Found Logical Router Port " + logicalRouterPortUuid + ", deleting it");
                final DeleteLogicalRouterPortCommand cmdDeletePort = new DeleteLogicalRouterPortCommand(lRouterUuid, logicalRouterPortUuid);
                final DeleteLogicalRouterPortAnswer answerDelete = (DeleteLogicalRouterPortAnswer) agentMgr.easySend(niciraNvpHost.getId(), cmdDeletePort);

                if (answerDelete != null && answerDelete.getResult()){
                    logger.info("Successfully deleted Logical Router Port " + logicalRouterPortUuid);
                }
                else {
                    logger.error("Could not delete Logical Router Port " + logicalRouterPortUuid);
                }
            }
            else {
                logger.error("Find Logical Router Port failed");
            }
        }
    }

    @Override
    public boolean trash(final Network network, final NetworkOffering offering) {
        //Since NVP Plugin supports Shared networks, remove mapping when deleting network implemented or allocated
        if (network.getGuestType() == GuestType.Shared && niciraNvpRouterMappingDao.existsMappingForNetworkId(network.getId())){
            NiciraNvpRouterMappingVO mappingVO = niciraNvpRouterMappingDao.findByNetworkId(network.getId());
            niciraNvpRouterMappingDao.remove(mappingVO.getId());
        }
        return super.trash(network, offering);
    }

    @Override
    public void finalizeNetworkDesign(long networkId, String vlanIdAsUUID) {
        if (vlanIdAsUUID == null) return;
        NiciraNvpRouterMappingVO routermapping = new NiciraNvpRouterMappingVO(vlanIdAsUUID, networkId);
        niciraNvpRouterMappingDao.persist(routermapping);
    }

    @Override
    public Map<String, ? extends Object> listAdditionalNicParams(String nicUuid) {
        NiciraNvpNicMappingVO mapping = niciraNvpNicMappingDao.findByNicUuid(nicUuid);
        if (mapping != null){
            Map<String, String> result = new HashMap<String, String>();
            result.put(NetworkGuruAdditionalFunctions.NSX_LSWITCH_UUID, mapping.getLogicalSwitchUuid());
            result.put(NetworkGuruAdditionalFunctions.NSX_LSWITCHPORT_UUID, mapping.getLogicalSwitchPortUuid());
            return result;
        }
        return null;
    }

}
