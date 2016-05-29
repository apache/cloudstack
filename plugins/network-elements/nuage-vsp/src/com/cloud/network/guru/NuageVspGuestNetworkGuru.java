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

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.guru.DeallocateVmVspCommand;
import com.cloud.agent.api.guru.ImplementNetworkVspCommand;
import com.cloud.agent.api.guru.ReserveVmInterfaceVspCommand;
import com.cloud.agent.api.guru.TrashNetworkVspCommand;
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
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.network.Network;
import com.cloud.network.Network.GuestType;
import com.cloud.network.Network.State;
import com.cloud.network.NetworkProfile;
import com.cloud.network.Networks;
import com.cloud.network.NuageVspDeviceVO;
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
import com.cloud.utils.StringUtils;
import com.cloud.utils.db.DB;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.NicProfile;
import com.cloud.vm.NicVO;
import com.cloud.vm.ReservationContext;
import com.cloud.vm.VirtualMachineProfile;
import com.google.common.base.Strings;
import com.cloud.util.NuageVspEntityBuilder;
import net.nuage.vsp.acs.client.api.model.VspNetwork;
import net.nuage.vsp.acs.client.api.model.VspNic;
import net.nuage.vsp.acs.client.api.model.VspStaticNat;
import net.nuage.vsp.acs.client.api.model.VspVm;
import org.apache.cloudstack.resourcedetail.VpcDetailVO;
import org.apache.cloudstack.resourcedetail.dao.VpcDetailsDao;
import org.apache.log4j.Logger;

import javax.inject.Inject;
import java.util.List;

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
        _isolationMethods = new IsolationMethod[] {IsolationMethod.VSP};
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

    @Override
    public Network implement(Network network, NetworkOffering offering, DeployDestination dest, ReservationContext context) throws InsufficientVirtualNetworkCapacityException {
        long networkId = network.getId();
        network = _networkDao.acquireInLockTable(network.getId(), 1200);
        if (network == null) {
            throw new ConcurrentOperationException("Unable to acquire lock on network " + networkId);
        }

        NetworkVO implemented = null;
        try {
            if (network.getState() != State.Implementing) {
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

            VspNetwork vspNetwork = _nuageVspEntityBuilder.buildVspNetwork(implemented, true);
            String tenantId = context.getDomain().getName() + "-" + context.getAccount().getAccountId();
            String broadcastUriStr = implemented.getUuid() + "/" + vspNetwork.getVirtualRouterIp();
            implemented.setBroadcastUri(Networks.BroadcastDomainType.Vsp.toUri(broadcastUriStr));
            implemented.setBroadcastDomainType(Networks.BroadcastDomainType.Vsp);

            HostVO nuageVspHost = getNuageVspHost(physicalNetworkId);
            List<String> dnsServers = _nuageVspManager.getDnsDetails(network);
            ImplementNetworkVspCommand cmd = new ImplementNetworkVspCommand(vspNetwork, dnsServers);
            Answer answer = _agentMgr.easySend(nuageVspHost.getId(), cmd);

            if (answer == null || !answer.getResult()) {
                s_logger.error("ImplementNetworkVspCommand for network " + network.getUuid() + " failed on Nuage VSD " + nuageVspHost.getDetail("hostname"));
                if ((null != answer) && (null != answer.getDetails())) {
                    s_logger.error(answer.getDetails());
                }
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

    @Override
    public NicProfile allocate(Network network, NicProfile nic, VirtualMachineProfile vm) throws InsufficientVirtualNetworkCapacityException, InsufficientAddressCapacityException {

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

            nic.setBroadcastUri(network.getBroadcastUri());
            nic.setIsolationUri(network.getBroadcastUri());

            //NicProfile does not contain the NIC UUID. We need this information to set it in the VMInterface and VPort
            //that we create in VSP
            NicVO nicFromDb = _nicDao.findById(nic.getId());
            IPAddressVO staticNatIp = _ipAddressDao.findByVmIdAndNetworkId(network.getId(), vm.getId());

            VspNetwork vspNetwork = _nuageVspEntityBuilder.buildVspNetwork(network, false);
            VspVm vspVm = _nuageVspEntityBuilder.buildVspVm(vm.getVirtualMachine(), network);
            VspNic vspNic = _nuageVspEntityBuilder.buildVspNic(nicFromDb.getUuid(), nic);
            VspStaticNat vspStaticNat = null;
            if (staticNatIp != null) {
                VlanVO staticNatVlan = _vlanDao.findById(staticNatIp.getVlanId());
                vspStaticNat = _nuageVspEntityBuilder.buildVspStaticNat(null, staticNatIp, staticNatVlan, null);
            }

            HostVO nuageVspHost = getNuageVspHost(network.getPhysicalNetworkId());
            ReserveVmInterfaceVspCommand cmd = new ReserveVmInterfaceVspCommand(vspNetwork, vspVm, vspNic, vspStaticNat);
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

    @Override
    protected boolean canHandle(NetworkOffering offering, final NetworkType networkType, final PhysicalNetwork physicalNetwork) {
        if (networkType == NetworkType.Advanced && isMyTrafficType(offering.getTrafficType()) && (offering.getGuestType() == Network.GuestType.Isolated || offering.getGuestType() == Network.GuestType.Shared)
                && isMyIsolationMethod(physicalNetwork)) {
            if (_configMgr.isOfferingForVpc(offering) && !offering.getIsPersistent()) {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("NuageVsp can't handle VPC tiers which use a network offering which are not persistent");
                }
                return false;
            }
            return true;
        } else {
            if (s_logger.isTraceEnabled()) {
                s_logger.trace("We only take care of Guest networks of type   " + GuestType.Isolated + " in zone of type " + NetworkType.Advanced);
            }
            return false;
        }
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

            VspNetwork vspNetwork = _nuageVspEntityBuilder.buildVspNetwork(network, false);
            VspVm vspVm = _nuageVspEntityBuilder.buildVspVm(vm.getVirtualMachine(), network);
            VspNic vspNic = _nuageVspEntityBuilder.buildVspNic(nicFromDb.getUuid(), nic);
            HostVO nuageVspHost = getNuageVspHost(network.getPhysicalNetworkId());

            DeallocateVmVspCommand cmd = new DeallocateVmVspCommand(vspNetwork, vspVm, vspNic);
            Answer answer = _agentMgr.easySend(nuageVspHost.getId(), cmd);
            if (answer == null || !answer.getResult()) {
                s_logger.error("DeallocateVmNuageVspCommand for VM " + vm.getUuid() + " failed on Nuage VSD " + nuageVspHost.getDetail("hostname"));
                if ((null != answer) && (null != answer.getDetails())) {
                    s_logger.error(answer.getDetails());
                }
            }
        } finally {
            if (network != null && lockedNetwork) {
                _networkDao.releaseFromLockTable(network.getId());
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Unlocked network " + network.getId() + " for deallocation of user VM " + vm.getInstanceName());
                }
            }
        }

        super.deallocate(network, nic, vm);
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
            VspNetwork vspNetwork = _nuageVspEntityBuilder.buildVspNetwork(network, false);
            HostVO nuageVspHost = getNuageVspHost(network.getPhysicalNetworkId());
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

    private HostVO getNuageVspHost(long physicalNetworkId) {
        HostVO nuageVspHost;
        List<NuageVspDeviceVO> nuageVspDevices = _nuageVspDao.listByPhysicalNetwork(physicalNetworkId);
        if (nuageVspDevices != null && (!nuageVspDevices.isEmpty())) {
            NuageVspDeviceVO config = nuageVspDevices.iterator().next();
            nuageVspHost = _hostDao.findById(config.getHostId());
            _hostDao.loadDetails(nuageVspHost);
        } else {
            throw new CloudRuntimeException("There is no Nuage VSP device configured on physical network " + physicalNetworkId);
        }
        return nuageVspHost;
    }
}