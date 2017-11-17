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

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import net.nuage.vsp.acs.client.api.model.NetworkRelatedVsdIds;
import net.nuage.vsp.acs.client.api.model.VspDhcpDomainOption;
import net.nuage.vsp.acs.client.api.model.VspDhcpVMOption;
import net.nuage.vsp.acs.client.api.model.VspDomain;
import net.nuage.vsp.acs.client.api.model.VspNetwork;
import net.nuage.vsp.acs.client.api.model.VspNic;
import net.nuage.vsp.acs.client.api.model.VspStaticNat;
import net.nuage.vsp.acs.client.api.model.VspVm;

import org.apache.log4j.Logger;

import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import org.apache.cloudstack.resourcedetail.VpcDetailVO;
import org.apache.cloudstack.resourcedetail.dao.VpcDetailsDao;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.guru.DeallocateVmVspCommand;
import com.cloud.agent.api.guru.ImplementNetworkVspCommand;
import com.cloud.agent.api.guru.ReserveVmInterfaceVspCommand;
import com.cloud.agent.api.guru.TrashNetworkVspCommand;
import com.cloud.agent.api.guru.UpdateDhcpOptionVspCommand;
import com.cloud.agent.api.manager.ImplementNetworkVspAnswer;
import com.cloud.configuration.ConfigurationManager;
import com.cloud.dc.DataCenter;
import com.cloud.dc.DataCenter.NetworkType;
import com.cloud.dc.VlanVO;
import com.cloud.deploy.DeployDestination;
import com.cloud.deploy.DeploymentPlan;
import com.cloud.domain.dao.DomainDao;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.InsufficientVirtualNetworkCapacityException;
import com.cloud.exception.UnsupportedServiceException;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.network.Network;
import com.cloud.network.Network.GuestType;
import com.cloud.network.Network.State;
import com.cloud.network.NetworkProfile;
import com.cloud.network.Networks;
import com.cloud.network.PhysicalNetwork;
import com.cloud.network.PhysicalNetwork.IsolationMethod;
import com.cloud.network.dao.IPAddressVO;
import com.cloud.network.dao.NetworkDetailVO;
import com.cloud.network.dao.NetworkDetailsDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.dao.NuageVspDao;
import com.cloud.network.dao.PhysicalNetworkVO;
import com.cloud.network.manager.NuageVspManager;
import com.cloud.network.vpc.dao.VpcDao;
import com.cloud.offering.NetworkOffering;
import com.cloud.offerings.dao.NetworkOfferingDao;
import com.cloud.offerings.dao.NetworkOfferingServiceMapDao;
import com.cloud.user.Account;
import com.cloud.user.AccountVO;
import com.cloud.user.dao.AccountDao;
import com.cloud.util.NuageVspEntityBuilder;
import com.cloud.utils.StringUtils;
import com.cloud.utils.db.DB;
import com.cloud.utils.net.Ip;
import com.cloud.vm.Nic;
import com.cloud.vm.NicProfile;
import com.cloud.vm.NicVO;
import com.cloud.vm.ReservationContext;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineProfile;
import com.cloud.vm.dao.VMInstanceDao;

public class NuageVspGuestNetworkGuru extends GuestNetworkGuru {
    public static final Logger s_logger = Logger.getLogger(NuageVspGuestNetworkGuru.class);

    @Inject
    NetworkOfferingServiceMapDao _ntwkOfferingSrvcDao;
    @Inject
    NetworkOfferingDao _ntwkOfferingDao;
    @Inject
    DomainDao _domainDao;
    @Inject
    AccountDao _accountDao;
    @Inject
    NuageVspDao _nuageVspDao;
    @Inject
    HostDao _hostDao;
    @Inject
    VpcDao _vpcDao;
    @Inject
    VMInstanceDao _vmInstanceDao;
    @Inject
    AgentManager _agentMgr;
    @Inject
    NuageVspManager _nuageVspManager;
    @Inject
    ConfigurationManager _configMgr;
    @Inject
    NuageVspEntityBuilder _nuageVspEntityBuilder;
    @Inject
    NetworkDetailsDao _networkDetailsDao;
    @Inject
    VpcDetailsDao _vpcDetailsDao;

    public NuageVspGuestNetworkGuru() {
        super();
        _isolationMethods = new IsolationMethod[] {new IsolationMethod("VSP")};
    }

    @Override
    public Network design(NetworkOffering offering, DeploymentPlan plan, Network userSpecified, Account owner) {
        PhysicalNetworkVO physnet = _physicalNetworkDao.findById(plan.getPhysicalNetworkId());
        DataCenter dc = _dcDao.findById(plan.getDataCenterId());
        if (!canHandle(offering, dc.getNetworkType(), physnet)) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Refusing to design network using network offering " +  offering.getId() + (physnet != null ? " on physical network " + physnet.getId() : ""));
            }
            return null;
        }

        NetworkVO networkObject = (NetworkVO)super.design(offering, plan, userSpecified, owner);
        if (networkObject == null) {
            return null;
        }

        networkObject.setBroadcastDomainType(Networks.BroadcastDomainType.Vsp);
        return networkObject;
    }

    /** In case an externalId is specified, we get called here, and store the id the same way as cached data */
    @Override
    public Network implement(Network network, NetworkOffering offering, DeployDestination dest, ReservationContext context) throws InsufficientVirtualNetworkCapacityException {
        long networkId = network.getId();
        network = _networkDao.acquireInLockTable(network.getId(), 1200);
        if (network == null) {
            throw new ConcurrentOperationException("Unable to acquire lock on network " + networkId);
        }

        /* Check if an acl template is used in combination with a pre-configured DT. -> show an error if there is
        Rollback of the network fails in core CS -> networkOrchestrator. */
        if(network.getVpcId() != null) {
            VpcDetailVO detail = _vpcDetailsDao.findDetail(network.getVpcId(), NuageVspManager.nuageDomainTemplateDetailName);
            if (detail != null && network.getNetworkACLId() != null) {
                s_logger.error("Pre-configured DT are used in combination with ACL lists. Which is not supported.");
                throw new IllegalArgumentException("CloudStack ACLs are not supported with Nuage Preconfigured Domain Template");
            }

            if(detail != null && !_nuageVspManager.checkIfDomainTemplateExist(network.getDomainId(),detail.getValue(),network.getDataCenterId(),null)){
                s_logger.error("The provided domain template does not exist on the VSD.");
                throw new IllegalArgumentException("The provided domain template does not exist on the VSD anymore.");
            }
        }

        NetworkVO implemented = null;
        try {
            if (offering.getGuestType() == GuestType.Isolated && network.getState() != State.Implementing) {
                throw new IllegalStateException("Network " + networkId + " is not in expected state Implementing, but is in state " + network.getState());
            }

            //Get the Account details and find the type
            AccountVO networksAccount = _accountDao.findById(network.getAccountId());
            if (networksAccount.getType() == Account.ACCOUNT_TYPE_PROJECT) {
                String errorMessage = "Networks created by account " + networksAccount.getAccountName() + " of type Project (" + Account.ACCOUNT_TYPE_PROJECT + ") " +
                        "are not yet supported by NuageVsp provider";
                s_logger.error(errorMessage);
                throw new InsufficientVirtualNetworkCapacityException(errorMessage, Account.class, network.getAccountId());
            }

            //We don't support a shared network with UserData and multiple IP ranges at the same time.
            checkMultipleSubnetsCombinedWithUseData(network);

            long dcId = dest.getDataCenter().getId();
            //Get physical network id
            Long physicalNetworkId = network.getPhysicalNetworkId();
            //Physical network id can be null in Guest Network in Basic zone, so locate the physical network
            if (physicalNetworkId == null) {
                physicalNetworkId = _networkModel.findPhysicalNetworkId(dcId, offering.getTags(), offering.getTrafficType());
            }

            implemented = new NetworkVO(network.getId(), network, network.getNetworkOfferingId(), network.getGuruName(), network.getDomainId(), network.getAccountId(),
                    network.getRelated(), network.getName(), network.getDisplayText(), network.getNetworkDomain(), network.getGuestType(), network.getDataCenterId(),
                    physicalNetworkId, network.getAclType(), network.getSpecifyIpRanges(), network.getVpcId(), offering.getRedundantRouter());
            implemented.setUuid(network.getUuid());
            implemented.setState(State.Allocated);
            if (network.getGateway() != null) {
                implemented.setGateway(network.getGateway());
            }
            if (network.getCidr() != null) {
                implemented.setCidr(network.getCidr());
            }

            VspNetwork vspNetwork = _nuageVspEntityBuilder.buildVspNetwork(implemented);
            String tenantId = context.getDomain().getName() + "-" + context.getAccount().getAccountId();
            String broadcastUriStr = implemented.getUuid() + "/" + vspNetwork.getVirtualRouterIp();
            implemented.setBroadcastUri(Networks.BroadcastDomainType.Vsp.toUri(broadcastUriStr));
            implemented.setBroadcastDomainType(Networks.BroadcastDomainType.Vsp);

            if (!implement(network.getVpcId(), physicalNetworkId, vspNetwork, _nuageVspEntityBuilder.buildNetworkDhcpOption(network, offering))) {
                return null;
            }

            if (StringUtils.isNotBlank(vspNetwork.getDomainTemplateName())) {
                if (network.getVpcId() != null) {
                    VpcDetailVO vpcDetail = new VpcDetailVO(network.getVpcId(), NuageVspManager.nuageDomainTemplateDetailName, vspNetwork.getDomainTemplateName(), false);
                    _vpcDetailsDao.persist(vpcDetail);
                } else {
                    NetworkDetailVO networkDetail = new NetworkDetailVO(implemented.getId(), NuageVspManager.nuageDomainTemplateDetailName, vspNetwork.getDomainTemplateName(), false);
                    _networkDetailsDao.persist(networkDetail);
                }
            }

            s_logger.info("Implemented OK, network " + implemented.getUuid() + " in tenant " + tenantId + " linked to " + implemented.getBroadcastUri());
        } finally {
            _networkDao.releaseFromLockTable(network.getId());
        }
        return implemented;
    }

    private boolean implement(Long vpcId, long physicalNetworkId, VspNetwork vspNetwork, VspDhcpDomainOption vspDhcpDomainOption) {
        HostVO nuageVspHost = _nuageVspManager.getNuageVspHost(physicalNetworkId);
        ImplementNetworkVspCommand cmd = new ImplementNetworkVspCommand(vspNetwork, vspDhcpDomainOption);
        ImplementNetworkVspAnswer answer = (ImplementNetworkVspAnswer) _agentMgr.easySend(nuageVspHost.getId(), cmd);
        if (answer == null || !answer.getResult()) {
            s_logger.error("ImplementNetworkVspCommand for network " + vspNetwork.getUuid() + " failed on Nuage VSD " + nuageVspHost.getDetail("hostname"));
            if ((null != answer) && (null != answer.getDetails())) {
                s_logger.error(answer.getDetails());
            }
            return false;
        }
        saveNetworkAndVpcDetails(vspNetwork, answer.getNetworkRelatedVsdIds(), vpcId);
        return true;
    }

    private void saveNetworkAndVpcDetails(VspNetwork vspNetwork, NetworkRelatedVsdIds networkRelatedVsdIds, Long vpcId) {


        if (!vspNetwork.isShared() && !vspNetwork.getNetworkRelatedVsdIds().equals(networkRelatedVsdIds)) {
            Map<String, String> networkDetails = constructNetworkDetails(networkRelatedVsdIds, vspNetwork.isVpc());

            for (Map.Entry<String, String> networkDetail : networkDetails.entrySet()) {
                NetworkDetailVO networkDetailVO = new NetworkDetailVO(vspNetwork.getId(), networkDetail.getKey(), networkDetail.getValue(), false);
                _networkDetailsDao.persist(networkDetailVO);
            }

            if(vspNetwork.isVpc()) {
                Map<String, String> vpcDetails = constructVpcDetails(networkRelatedVsdIds);

                for (Map.Entry<String, String> vpcDetail : vpcDetails.entrySet()) {
                    VpcDetailVO vpcDetailVO = new VpcDetailVO(vpcId, vpcDetail.getKey(), vpcDetail.getValue(), false);
                    _vpcDetailsDao.persist(vpcDetailVO);
                }
            }
        }
    }

    private static Map<String, String> constructNetworkDetails(NetworkRelatedVsdIds networkRelatedVsdIds, boolean isVpc) {
        Map<String, String> networkDetails = Maps.newHashMap();

        if(!isVpc) {
            networkRelatedVsdIds.getVsdDomainId().ifPresent(v -> networkDetails.put(NuageVspManager.NETWORK_METADATA_VSD_DOMAIN_ID, v));
            networkRelatedVsdIds.getVsdZoneId().ifPresent(v -> networkDetails.put(NuageVspManager.NETWORK_METADATA_VSD_ZONE_ID, v));
        }
        networkRelatedVsdIds.getVsdSubnetId().ifPresent(v ->  networkDetails.put(NuageVspManager.NETWORK_METADATA_VSD_SUBNET_ID, v));

        return networkDetails;
    }

    private static Map<String, String> constructVpcDetails(NetworkRelatedVsdIds networkRelatedVsdIds) {
        Map<String, String> vpcDetails = Maps.newHashMap();

        networkRelatedVsdIds.getVsdDomainId().ifPresent(v ->  vpcDetails.put(NuageVspManager.NETWORK_METADATA_VSD_DOMAIN_ID, v));
        networkRelatedVsdIds.getVsdZoneId().ifPresent(v ->  vpcDetails.put(NuageVspManager.NETWORK_METADATA_VSD_ZONE_ID, v));

        return vpcDetails;
    }


    @Override
    public NicProfile allocate(Network network, NicProfile nic, VirtualMachineProfile vm) throws InsufficientVirtualNetworkCapacityException, InsufficientAddressCapacityException {
        if (vm.getType() != VirtualMachine.Type.DomainRouter && _nuageVspEntityBuilder.usesVirtualRouter(network.getNetworkOfferingId())) {
            VspNetwork vspNetwork = _nuageVspEntityBuilder.buildVspNetwork(network);
            if (nic != null && nic.getRequestedIPv4() != null && vspNetwork.getVirtualRouterIp().equals(nic.getRequestedIPv4())) {
                DataCenter dc = _dcDao.findById(network.getDataCenterId());
                s_logger.error("Unable to acquire requested Guest IP address " + nic.getRequestedIPv4() + " because it is reserved for the VR in network " + network);
                throw new InsufficientVirtualNetworkCapacityException("Unable to acquire requested Guest IP address " + nic.getRequestedIPv4() + " because it is reserved " +
                        "for the VR in network " + network, DataCenter.class,dc.getId());
            }
        }

        return super.allocate(network, nic, vm);
    }

    @Override
    public void reserve(NicProfile nic, Network network, VirtualMachineProfile vm, DeployDestination dest, ReservationContext context)
            throws InsufficientVirtualNetworkCapacityException, InsufficientAddressCapacityException {
        boolean lockedNetwork = lockNetworkForUserVm(network, vm);
        if (lockedNetwork && s_logger.isDebugEnabled()) {
            s_logger.debug("Locked network " + network.getId() + " for creation of user VM " + vm.getInstanceName());
        }

        try {
            //We don't support a shared network with UserData and multiple IP ranges at the same time.
            checkMultipleSubnetsCombinedWithUseData(network);

            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Handling reserve() call back to with Create a new VM or add an interface to existing VM in network " + network.getName());
            }

            DataCenter dc = _dcDao.findById(network.getDataCenterId());
            AccountVO neworkAccountDetails = _accountDao.findById(network.getAccountId());
            if (neworkAccountDetails.getType() == Account.ACCOUNT_TYPE_PROJECT) {
                throw new InsufficientVirtualNetworkCapacityException("CS project support is not yet implemented in NuageVsp", DataCenter.class, dc.getId());
            }

            if (Strings.isNullOrEmpty(network.getBroadcastUri().getPath()) || !network.getBroadcastUri().getPath().startsWith("/")) {
                throw new IllegalStateException("The broadcast URI path " + network.getBroadcastUri() + " is empty or in an incorrect format.");
            }

            HostVO nuageVspHost = _nuageVspManager.getNuageVspHost(network.getPhysicalNetworkId());
            VspNetwork vspNetwork = _nuageVspEntityBuilder.buildVspNetwork(vm.getVirtualMachine().getDomainId(), network);

            if (vspNetwork.isShared()) {
                vspNetwork = _nuageVspEntityBuilder.updateVspNetworkByPublicIp(vspNetwork, network, nic.getIPv4Address());

                if (VirtualMachine.Type.DomainRouter.equals(vm.getType()) && !nic.getIPv4Address().equals(vspNetwork.getVirtualRouterIp())) {
                    if(s_logger.isDebugEnabled()) {
                        s_logger.debug("VR got spawned with a different IP, releasing the previously allocated public IP " + nic.getIPv4Address());
                    }
                    IPAddressVO oldIpAddress = _ipAddressDao.findByIpAndSourceNetworkId(network.getId(), nic.getIPv4Address());
                    _ipAddressDao.unassignIpAddress(oldIpAddress.getId());
                    _ipAddressDao.mark(network.getDataCenterId(), new Ip(vspNetwork.getVirtualRouterIp()));
                } else if (VirtualMachine.Type.User.equals(vm.getType()) && nic.getIPv4Address().equals(vspNetwork.getVirtualRouterIp())) {
                    s_logger.error("Deploying a user VM with the same IP as the VR is not allowed.");
                    throw new InsufficientVirtualNetworkCapacityException("Deploying a user VM with the same IP " + nic.getIPv4Address() + " as the VR is not allowed.",
                            Network.class, network.getId());
                }

                // Make sure the shared network is present
                NetworkOffering offering = _ntwkOfferingDao.findById(network.getNetworkOfferingId());
                if (!implement(network.getVpcId(), network.getPhysicalNetworkId(), vspNetwork, _nuageVspEntityBuilder.buildNetworkDhcpOption(network, offering))) {
                    s_logger.error("Failed to implement shared network " + network.getUuid() + " under domain " + context.getDomain().getUuid());
                    throw new InsufficientVirtualNetworkCapacityException("Failed to implement shared network " + network.getUuid() + " under domain " +
                            context.getDomain().getUuid(), Network.class, network.getId());
                }
            }

            // Set flags for dhcp options
            boolean networkHasDns = networkHasDns(network);

            Map<Long, Boolean> networkHasDnsCache = Maps.newHashMap();
            networkHasDnsCache.put(network.getId(), networkHasDns);

            // Determine if dhcp options of the other nics in the network need to be updated
            if (vm.getType() == VirtualMachine.Type.DomainRouter && network.getState() != State.Implementing) {
                updateDhcpOptionsForExistingVms(network, nuageVspHost, vspNetwork, networkHasDns, networkHasDnsCache);
            }

            nic.setBroadcastUri(network.getBroadcastUri());
            nic.setIsolationUri(network.getBroadcastUri());

            //NicProfile does not contain the NIC UUID. We need this information to set it in the VMInterface and VPort
            //that we create in VSP
            NicVO nicFromDb = _nicDao.findById(nic.getId());
            IPAddressVO staticNatIp = _ipAddressDao.findByVmIdAndNetworkId(network.getId(), vm.getId());
            VspVm vspVm = _nuageVspEntityBuilder.buildVspVm(vm.getVirtualMachine(), network);
            VspNic vspNic = _nuageVspEntityBuilder.buildVspNic(nicFromDb.getUuid(), nic);
            VspStaticNat vspStaticNat = null;
            if (staticNatIp != null) {
                VlanVO staticNatVlan = _vlanDao.findById(staticNatIp.getVlanId());
                vspStaticNat = _nuageVspEntityBuilder.buildVspStaticNat(null, staticNatIp, staticNatVlan, vspNic);
            }

            boolean defaultHasDns = getDefaultHasDns(networkHasDnsCache, nicFromDb);
            VspDhcpVMOption dhcpOption = _nuageVspEntityBuilder.buildVmDhcpOption(nicFromDb, defaultHasDns, networkHasDns);
            ReserveVmInterfaceVspCommand cmd = new ReserveVmInterfaceVspCommand(vspNetwork, vspVm, vspNic, vspStaticNat, dhcpOption);
            Answer answer = _agentMgr.easySend(nuageVspHost.getId(), cmd);

            if (answer == null || !answer.getResult()) {
                s_logger.error("ReserveVmInterfaceNuageVspCommand failed for NIC " + nic.getId() + " attached to VM " + vm.getId() + " in network " + network.getId());
                if ((null != answer) && (null != answer.getDetails())) {
                    s_logger.error(answer.getDetails());
                }
                throw new InsufficientVirtualNetworkCapacityException("Failed to reserve VM in Nuage VSP.", Network.class, network.getId());
            }

            if (vspVm.getDomainRouter() == Boolean.TRUE) {
                nic.setIPv4Address(vspVm.getDomainRouterIp());
            }

        } finally {
            if (network != null && lockedNetwork) {
                _networkDao.releaseFromLockTable(network.getId());
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Unlocked network " + network.getId() + " for creation of user VM " + vm.getInstanceName());
                }
            }
        }
    }

    private void updateDhcpOptionsForExistingVms(Network network, HostVO nuageVspHost, VspNetwork vspNetwork, boolean networkHasDns, Map<Long, Boolean> networkHasDnsCache)
            throws InsufficientVirtualNetworkCapacityException {
        // Update dhcp options if a VR is added when we are not initiating the network
        if(s_logger.isDebugEnabled()) {
            s_logger.debug(String.format("DomainRouter is added to an existing network: %s in state: %s", network.getName(), network.getState()));
        }

        List<NicVO> userNics = _nicDao.listByNetworkId(network.getId());
        LinkedListMultimap<Long, VspDhcpVMOption> dhcpOptionsPerDomain = LinkedListMultimap.create();

        for (NicVO userNic : userNics) {
            if (userNic.getVmType() == VirtualMachine.Type.DomainRouter) {
                continue;
            }

            VMInstanceVO userVm  = _vmInstanceDao.findById(userNic.getInstanceId());
            boolean defaultHasDns = getDefaultHasDns(networkHasDnsCache, userNic);
            VspDhcpVMOption dhcpOption = _nuageVspEntityBuilder.buildVmDhcpOption(userNic, defaultHasDns, networkHasDns);
            dhcpOptionsPerDomain.put(userVm.getDomainId(), dhcpOption);
        }

        for (Long domainId : dhcpOptionsPerDomain.keySet()) {
            VspDomain vspDomain = _nuageVspEntityBuilder.buildVspDomain(_domainDao.findById(domainId));
            VspNetwork vspNetworkForDomain = new VspNetwork.Builder().fromObject(vspNetwork).domain(vspDomain).build();
            List<VspDhcpVMOption> dhcpOptions = dhcpOptionsPerDomain.get(domainId);
            UpdateDhcpOptionVspCommand cmd = new UpdateDhcpOptionVspCommand(dhcpOptions, vspNetworkForDomain);
            Answer answer = _agentMgr.easySend(nuageVspHost.getId(), cmd);

            if (answer == null || !answer.getResult()) {
                s_logger.error("UpdateDhcpOptionVspCommand failed at \"reserve\" for network " + vspNetwork.getName() + " under domain " + vspNetwork.getVspDomain().getName());
                if ((null != answer) && (null != answer.getDetails())) {
                    s_logger.error(answer.getDetails());
                }
                throw new InsufficientVirtualNetworkCapacityException("Failed to reserve VM in Nuage VSP.", Network.class, network.getId());
            }
        }
    }

    private void checkMultipleSubnetsCombinedWithUseData(Network network) {
        if (_ntwkOfferingSrvcDao.listServicesForNetworkOffering(network.getNetworkOfferingId()).contains(Network.Service.UserData.getName())) {
            List<VlanVO> vlanVOs = _vlanDao.listVlansByNetworkId(network.getId());
            if (vlanVOs.stream()
                       .map(VlanVO::getVlanGateway)
                       .distinct()
                       .count() > 1) {
                        s_logger.error("NuageVsp provider does not support multiple subnets in combination with user data. Network: " + network + ", vlans: " + vlanVOs);
                        throw new UnsupportedServiceException("NuageVsp provider does not support multiple subnets in combination with user data.");
            }
        }
    }

    @Override
    protected boolean canHandle(NetworkOffering offering, final NetworkType networkType, final PhysicalNetwork physicalNetwork) {
        if (networkType == NetworkType.Advanced
                && isMyTrafficType(offering.getTrafficType())
                && isMyIsolationMethod(physicalNetwork)
                && (offering.getGuestType() == GuestType.Isolated || offering.getGuestType() == GuestType.Shared)
                && hasRequiredServices(offering)) {
            if (_configMgr.isOfferingForVpc(offering) && !offering.getIsPersistent()) {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("NuageVsp can't handle VPC tiers which use a network offering which are not persistent");
                }
                return false;
            } else if (offering.getGuestType() == GuestType.Shared) {
                List<String> supportedSharedNetworkServices = Lists.newArrayList(Network.Service.Connectivity.getName(), Network.Service.Dhcp.getName(), Network.Service.UserData.getName());
                List<String> offeringServices = _ntwkOfferingSrvcDao.listServicesForNetworkOffering(offering.getId());
                if (!supportedSharedNetworkServices.containsAll(offeringServices)) {
                    if (s_logger.isDebugEnabled()) {
                        s_logger.debug("We only support " + Iterables.toString(supportedSharedNetworkServices) + " services for shared networks");
                    }
                    return false;
                }
            }
            return true;
        } else {
            if (s_logger.isTraceEnabled()) {
                s_logger.trace("We only take care of networks in zone of type " + NetworkType.Advanced + " without VLAN");
            }
            return false;
        }
    }

    private boolean hasRequiredServices(NetworkOffering networkOffering) {
        final Map<Network.Service, Set<Network.Provider>> serviceProviderMap = _networkModel.getNetworkOfferingServiceProvidersMap(networkOffering.getId());

        if (!serviceProviderMap.get(Network.Service.Connectivity).contains(Network.Provider.NuageVsp)) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("NuageVsp can't handle networks which use a network offering without NuageVsp as Connectivity provider");
            }
            return false;
        }

        if (networkOffering.getGuestType() == GuestType.Isolated
                && !serviceProviderMap.get(Network.Service.SourceNat).contains(Network.Provider.NuageVsp)) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("NuageVsp can't handle networks which use a network offering without NuageVsp as SourceNat provider");
            }
            return false;
        }
        return true;
    }

    @Override
    @DB
    public void deallocate(Network network, NicProfile nic, VirtualMachineProfile vm) {
        boolean lockedNetwork = lockNetworkForUserVm(network, vm);
        if (lockedNetwork && s_logger.isDebugEnabled()) {
            s_logger.debug("Locked network " + network.getId() + " for deallocation of user VM " + vm.getInstanceName());
        }

        try {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Handling deallocate() call back, which is called when a VM is destroyed or interface is removed, " + "to delete VM Interface with IP "
                        + nic.getIPv4Address() + " from a VM " + vm.getInstanceName() + " with state " + vm.getVirtualMachine().getState());
            }

            NicVO nicFromDb = _nicDao.findById(nic.getId());

            VspNetwork vspNetwork = _nuageVspEntityBuilder.buildVspNetwork(vm.getVirtualMachine().getDomainId(), network);
            VspVm vspVm = _nuageVspEntityBuilder.buildVspVm(vm.getVirtualMachine(), network);
            VspNic vspNic = _nuageVspEntityBuilder.buildVspNic(nicFromDb.getUuid(), nic);
            HostVO nuageVspHost = _nuageVspManager.getNuageVspHost(network.getPhysicalNetworkId());

            DeallocateVmVspCommand cmd = new DeallocateVmVspCommand(vspNetwork, vspVm, vspNic);
            Answer answer = _agentMgr.easySend(nuageVspHost.getId(), cmd);
            if (answer == null || !answer.getResult()) {
                s_logger.error("DeallocateVmNuageVspCommand for VM " + vm.getUuid() + " failed on Nuage VSD " + nuageVspHost.getDetail("hostname"));
                if ((null != answer) && (null != answer.getDetails())) {
                    s_logger.error(answer.getDetails());
                }
            }

            // In case of shared network, when a User VM is spawned with the same IP as the VR, and it gets cleaned up, make sure we do not release the public IP
            // because it is still allocated for the VR.
            if (vspNetwork.isShared() && VirtualMachine.Type.User.equals(vm.getType()) && nic.getIPv4Address().equals(vspNetwork.getVirtualRouterIp())) {
                nic.deallocate();
            } else {
                super.deallocate(network, nic, vm);
            }
        } finally {
            if (network != null && lockedNetwork) {
                _networkDao.releaseFromLockTable(network.getId());
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Unlocked network " + network.getId() + " for deallocation of user VM " + vm.getInstanceName());
                }
            }
        }
    }

    private boolean lockNetworkForUserVm(Network network, VirtualMachineProfile vm) {
        if (!vm.getVirtualMachine().getType().isUsedBySystem()) {
            long networkId = network.getId();
            network = _networkDao.acquireInLockTable(network.getId(), 1200);
            if (network == null) {
                throw new ConcurrentOperationException("Unable to acquire lock on network " + networkId);
            }
            return true;
        }
        return false;
    }

    @Override
    public void shutdown(NetworkProfile profile, NetworkOffering offering) {
        super.shutdown(profile, offering);
    }

    @Override
    public boolean trash(Network network, NetworkOffering offering) {
        long networkId = network.getId();
        network = _networkDao.acquireInLockTable(networkId, 1200);
        if (network == null) {
            throw new ConcurrentOperationException("Unable to acquire lock on network " + networkId);
        }

        try {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Handling trash() call back to delete the network " + network.getName() + " with uuid " + network.getUuid() + " from VSP");
            }
            VspNetwork vspNetwork = _nuageVspEntityBuilder.buildVspNetwork(network);
            HostVO nuageVspHost = _nuageVspManager.getNuageVspHost(network.getPhysicalNetworkId());
            TrashNetworkVspCommand cmd = new TrashNetworkVspCommand(vspNetwork);
            Answer answer = _agentMgr.easySend(nuageVspHost.getId(), cmd);
            if (answer == null || !answer.getResult()) {
                s_logger.error("TrashNetworkNuageVspCommand for network " + network.getUuid() + " failed");
                if ((null != answer) && (null != answer.getDetails())) {
                    s_logger.error(answer.getDetails());
                }
                return false;
            }
        } finally {
            _networkDao.releaseFromLockTable(network.getId());
        }
        return super.trash(network, offering);
    }

    private boolean networkHasDns(Network network) {

        if (network != null) {
            List<String> dnsProviders = _ntwkOfferingSrvcDao.listProvidersForServiceForNetworkOffering(network.getNetworkOfferingId(), Network.Service.Dns);
            return dnsProviders.contains(Network.Provider.VirtualRouter.getName())
                || dnsProviders.contains(Network.Provider.VPCVirtualRouter.getName());

        }

        return false;
    }

    private boolean getDefaultHasDns(Map<Long, Boolean> cache, Nic nic) {
        Long networkId = nic.isDefaultNic()
                ? Long.valueOf(nic.getNetworkId())
                : getDefaultNetwork(nic.getInstanceId());

        Boolean hasDns = cache.computeIfAbsent(networkId, k -> networkHasDns(_networkDao.findById(networkId)));
        return hasDns;
    }

    private Long getDefaultNetwork(long vmId) {
        NicVO defaultNic = _nicDao.findDefaultNicForVM(vmId);
        if (defaultNic != null) return defaultNic.getNetworkId();
        return  null;
    }
}
