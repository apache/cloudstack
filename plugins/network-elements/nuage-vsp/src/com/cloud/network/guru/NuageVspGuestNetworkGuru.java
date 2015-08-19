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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ejb.Local;
import javax.inject.Inject;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.guru.DeallocateVmVspAnswer;
import com.cloud.agent.api.guru.DeallocateVmVspCommand;
import com.cloud.agent.api.guru.ImplementNetworkVspAnswer;
import com.cloud.agent.api.guru.ImplementNetworkVspCommand;
import com.cloud.agent.api.guru.ReleaseVmVspAnswer;
import com.cloud.agent.api.guru.ReleaseVmVspCommand;
import com.cloud.agent.api.guru.ReserveVmInterfaceVspAnswer;
import com.cloud.agent.api.guru.ReserveVmInterfaceVspCommand;
import com.cloud.agent.api.guru.TrashNetworkVspAnswer;
import com.cloud.agent.api.guru.TrashNetworkVspCommand;
import com.cloud.dc.DataCenter;
import com.cloud.dc.DataCenter.NetworkType;
import com.cloud.deploy.DeployDestination;
import com.cloud.deploy.DeploymentPlan;
import com.cloud.domain.Domain;
import com.cloud.domain.DomainVO;
import com.cloud.domain.dao.DomainDao;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.InsufficientVirtualNetworkCapacityException;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.network.Network;
import com.cloud.network.Network.GuestType;
import com.cloud.network.Network.Service;
import com.cloud.network.Network.State;
import com.cloud.network.NetworkProfile;
import com.cloud.network.Networks;
import com.cloud.network.NuageVspDeviceVO;
import com.cloud.network.PhysicalNetwork;
import com.cloud.network.PhysicalNetwork.IsolationMethod;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.dao.NuageVspDao;
import com.cloud.network.dao.PhysicalNetworkVO;
import com.cloud.network.vpc.Vpc;
import com.cloud.network.vpc.dao.VpcDao;
import com.cloud.offering.NetworkOffering;
import com.cloud.offerings.dao.NetworkOfferingDao;
import com.cloud.offerings.dao.NetworkOfferingServiceMapDao;
import com.cloud.user.Account;
import com.cloud.user.AccountVO;
import com.cloud.user.dao.AccountDao;
import com.cloud.utils.db.DB;
import com.cloud.utils.net.NetUtils;
import com.cloud.vm.NicProfile;
import com.cloud.vm.NicVO;
import com.cloud.vm.ReservationContext;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineProfile;

@Local(value = NetworkGuru.class)
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

    public NuageVspGuestNetworkGuru() {
        super();
        _isolationMethods = new IsolationMethod[] {IsolationMethod.VSP};
    }

    @Override
    public Network design(NetworkOffering offering, DeploymentPlan plan, Network userSpecified, Account owner) {
        PhysicalNetworkVO physnet = _physicalNetworkDao.findById(plan.getPhysicalNetworkId());
        DataCenter dc = _dcDao.findById(plan.getDataCenterId());
        if (!canHandle(offering, dc.getNetworkType(), physnet)) {
            s_logger.debug("Refusing to design this network");
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

        assert (network.getState() == State.Implementing) : "Why are we implementing " + network;

        long dcId = dest.getDataCenter().getId();
        //Get physical network id
        Long physicalNetworkId = network.getPhysicalNetworkId();
        //Physical network id can be null in Guest Network in Basic zone, so locate the physical network
        if (physicalNetworkId == null) {
            physicalNetworkId = _networkModel.findPhysicalNetworkId(dcId, offering.getTags(), offering.getTrafficType());
        }
        NetworkVO implemented = new NetworkVO(network.getTrafficType(), network.getMode(), network.getBroadcastDomainType(), network.getNetworkOfferingId(), State.Allocated,
                network.getDataCenterId(), physicalNetworkId, offering.getRedundantRouter());
        if (network.getGateway() != null) {
            implemented.setGateway(network.getGateway());
        }
        if (network.getCidr() != null) {
            implemented.setCidr(network.getCidr());
        }
        Collection<String> ipAddressRange = new ArrayList<String>();
        String virtualRouterIp = getVirtualRouterIP(network, ipAddressRange);
        String networkUuid = implemented.getUuid();
        String tenantId = context.getDomain().getName() + "-" + context.getAccount().getAccountId();
        String broadcastUriStr = networkUuid + "/" + virtualRouterIp;
        implemented.setBroadcastUri(Networks.BroadcastDomainType.Vsp.toUri(broadcastUriStr));
        implemented.setBroadcastDomainType(Networks.BroadcastDomainType.Vsp);
        //Check if the network is associated to a VPC
        Long vpcId = network.getVpcId();
        boolean isVpc = (vpcId != null);
        //Check owner of the Network
        Domain networksDomain = _domainDao.findById(network.getDomainId());
        //Get the Account details and find the type
        AccountVO networksAccount = _accountDao.findById(network.getAccountId());
        if (networksAccount.getType() == Account.ACCOUNT_TYPE_PROJECT) {
            String errorMessage = "CS project support is not yet implemented in NuageVsp";
            s_logger.debug(errorMessage);
            throw new InsufficientVirtualNetworkCapacityException(errorMessage, Account.class, network.getAccountId());
        }
        boolean isL3Network = isL3Network(offering.getId());
        String vpcName = null;
        String vpcUuid = null;
        if (isVpc) {
            Vpc vpcObj = _vpcDao.findById(vpcId);
            vpcName = vpcObj.getName();
            vpcUuid = vpcObj.getUuid();
        }

        HostVO nuageVspHost = getNuageVspHost(physicalNetworkId);
        ImplementNetworkVspCommand cmd = new ImplementNetworkVspCommand(networksDomain.getName(), networksDomain.getPath(), networksDomain.getUuid(),
                networksAccount.getAccountName(), networksAccount.getUuid(), network.getName(), network.getCidr(), network.getGateway(), network.getUuid(), isL3Network, vpcName,
                vpcUuid, offering.getEgressDefaultPolicy(), ipAddressRange);
        ImplementNetworkVspAnswer answer = (ImplementNetworkVspAnswer)_agentMgr.easySend(nuageVspHost.getId(), cmd);

        if (answer == null || !answer.getResult()) {
            s_logger.error("ImplementNetworkNuageVspCommand failed");
            if ((null != answer) && (null != answer.getDetails())) {
                s_logger.error(answer.getDetails());
            }
            return null;
        }
        s_logger.info("Implemented OK, network " + networkUuid + " in tenant " + tenantId + " linked to " + implemented.getBroadcastUri().toString());
        return implemented;
    }

    @Override
    public NicProfile allocate(Network network, NicProfile nic, VirtualMachineProfile vm) throws InsufficientVirtualNetworkCapacityException, InsufficientAddressCapacityException {

        return super.allocate(network, nic, vm);
    }

    @Override
    public void reserve(NicProfile nic, Network network, VirtualMachineProfile vm, DeployDestination dest, ReservationContext context)
            throws InsufficientVirtualNetworkCapacityException, InsufficientAddressCapacityException {
        nic.setBroadcastUri(network.getBroadcastUri());
        nic.setIsolationUri(network.getBroadcastUri());

        s_logger.debug("Handling reserve() call back to with Create a new VM or add an interface to existing VM in network " + network.getName());
        DataCenter dc = _dcDao.findById(network.getDataCenterId());
        Account networksAccount = _accountDao.findById(network.getAccountId());
        DomainVO networksDomain = _domainDao.findById(network.getDomainId());
        //Get the Account details and find the type
        long networkOwnedBy = network.getAccountId();
        AccountVO neworkAccountDetails = _accountDao.findById(networkOwnedBy);
        if (neworkAccountDetails.getType() == Account.ACCOUNT_TYPE_PROJECT) {
            throw new InsufficientVirtualNetworkCapacityException("CS project support is " + "not yet implemented in NuageVsp", DataCenter.class, dc.getId());
        }

        //NicProfile does not contain the NIC UUID. We need this information to set it in the VMInterface and VPort
        //that we create in VSP
        NicVO nicFrmDB = _nicDao.findById(nic.getId());
        long networkOfferingId = _ntwkOfferingDao.findById(network.getNetworkOfferingId()).getId();
        boolean isL3Network = isL3Network(networkOfferingId);
        Long vpcId = network.getVpcId();
        String vpcUuid = null;
        if (vpcId != null) {
            Vpc vpcObj = _vpcDao.findById(vpcId);
            vpcUuid = vpcObj.getUuid();
        }
        HostVO nuageVspHost = getNuageVspHost(network.getPhysicalNetworkId());
        ReserveVmInterfaceVspCommand cmd = new ReserveVmInterfaceVspCommand(nicFrmDB.getUuid(), nic.getMacAddress(), network.getUuid(), isL3Network, vpcUuid,
                networksDomain.getUuid(), networksAccount.getUuid(), vm.getType().equals(VirtualMachine.Type.DomainRouter), network.getBroadcastUri().getPath().substring(1),
                vm.getInstanceName(), vm.getUuid(), networksDomain.getUuid(), networksAccount.getUuid());
        ReserveVmInterfaceVspAnswer answer = (ReserveVmInterfaceVspAnswer)_agentMgr.easySend(nuageVspHost.getId(), cmd);

        if (answer == null || !answer.getResult()) {
            s_logger.error("ReserveVmInterfaceNuageVspCommand failed");
            if ((null != answer) && (null != answer.getDetails())) {
                s_logger.error(answer.getDetails());
            }
            throw new InsufficientVirtualNetworkCapacityException("Failed to reserve VM in Nuage VSP.", Network.class, network.getId());
        }
        List<Map<String, String>> vmInterfacesDetails = answer.getInterfaceDetails();
        setIPGatewayMaskInfo(network, nic, vmInterfacesDetails);
    }

    @Override
    protected boolean canHandle(NetworkOffering offering, final NetworkType networkType, final PhysicalNetwork physicalNetwork) {
        if (networkType == NetworkType.Advanced && isMyTrafficType(offering.getTrafficType()) && offering.getGuestType() == Network.GuestType.Isolated
                && isMyIsolationMethod(physicalNetwork)) {
            return true;
        } else {
            s_logger.trace("We only take care of Guest networks of type   " + GuestType.Isolated + " in zone of type " + NetworkType.Advanced);
            return false;
        }
    }

    @Override
    public boolean release(NicProfile nic, VirtualMachineProfile vm, String reservationId) {
        long networkId = nic.getNetworkId();
        Network network = _networkDao.findById(networkId);
        s_logger.debug("Handling release() call back, which is called when a VM is stopped or destroyed, to delete the VM with state " + vm.getVirtualMachine().getState()
                + " from netork " + network.getName());
        if (vm.getVirtualMachine().getState().equals(VirtualMachine.State.Stopping)) {
            try {
                HostVO nuageVspHost = getNuageVspHost(network.getPhysicalNetworkId());
                ReleaseVmVspCommand cmd = new ReleaseVmVspCommand(network.getUuid(), vm.getUuid(), vm.getInstanceName());
                ReleaseVmVspAnswer answer = (ReleaseVmVspAnswer)_agentMgr.easySend(nuageVspHost.getId(), cmd);
                if (answer == null || !answer.getResult()) {
                    s_logger.error("ReleaseVmNuageVspCommand for VM " + vm.getUuid() + " failed");
                    if ((null != answer) && (null != answer.getDetails())) {
                        s_logger.error(answer.getDetails());
                    }
                }
            } catch (InsufficientVirtualNetworkCapacityException e) {
                s_logger.debug("Handling release() call back. Failed to delete CS VM " + vm.getInstanceName() + " in VSP. " + e.getMessage());
            }
        } else {
            s_logger.debug("Handling release() call back. VM " + vm.getInstanceName() + " is in " + vm.getVirtualMachine().getState() + " state. So, the CS VM is not deleted."
                    + " This could be a case where VM interface is deleted. deallocate() call back should be called later");
        }

        return super.release(nic, vm, reservationId);
    }

    @Override
    @DB
    public void deallocate(Network network, NicProfile nic, VirtualMachineProfile vm) {

        try {
            s_logger.debug("Handling deallocate() call back, which is called when a VM is destroyed or interface is removed, " + "to delete VM Interface with IP "
                    + nic.getIPv4Address() + " from a VM " + vm.getInstanceName() + " with state " + vm.getVirtualMachine().getState());
            DomainVO networksDomain = _domainDao.findById(network.getDomainId());
            NicVO nicFrmDd = _nicDao.findById(nic.getId());
            long networkOfferingId = _ntwkOfferingDao.findById(network.getNetworkOfferingId()).getId();
            Long vpcId = network.getVpcId();
            String vpcUuid = null;
            if (vpcId != null) {
                Vpc vpcObj = _vpcDao.findById(vpcId);
                vpcUuid = vpcObj.getUuid();
            }
            HostVO nuageVspHost = getNuageVspHost(network.getPhysicalNetworkId());
            DeallocateVmVspCommand cmd = new DeallocateVmVspCommand(network.getUuid(), nicFrmDd.getUuid(), nic.getMacAddress(), nic.getIPv4Address(),
                    isL3Network(networkOfferingId), vpcUuid, networksDomain.getUuid(), vm.getInstanceName(), vm.getUuid());
            DeallocateVmVspAnswer answer = (DeallocateVmVspAnswer)_agentMgr.easySend(nuageVspHost.getId(), cmd);
            if (answer == null || !answer.getResult()) {
                s_logger.error("DeallocateVmNuageVspCommand for VM " + vm.getUuid() + " failed");
                if ((null != answer) && (null != answer.getDetails())) {
                    s_logger.error(answer.getDetails());
                }
            }
        } catch (InsufficientVirtualNetworkCapacityException e) {
            s_logger.error("Handling deallocate(). VM " + vm.getInstanceName() + " with NIC IP " + nic.getIPv4Address()
                    + " is getting destroyed. REST API failed to update the VM state in NuageVsp", e);
        }
        super.deallocate(network, nic, vm);
    }

    @Override
    public void shutdown(NetworkProfile profile, NetworkOffering offering) {
        super.shutdown(profile, offering);
    }

    @Override
    public boolean trash(Network network, NetworkOffering offering) {

        s_logger.debug("Handling trash() call back to delete the network " + network.getName() + " with uuid " + network.getUuid() + " from VSP");
        long domainId = network.getDomainId();
        Domain domain = _domainDao.findById(domainId);
        Long vpcId = network.getVpcId();
        String vpcUuid = null;
        if (vpcId != null) {
            Vpc vpcObj = _vpcDao.findById(vpcId);
            vpcUuid = vpcObj.getUuid();
        }
        try {
            HostVO nuageVspHost = getNuageVspHost(network.getPhysicalNetworkId());
            TrashNetworkVspCommand cmd = new TrashNetworkVspCommand(domain.getUuid(), network.getUuid(), isL3Network(offering.getId()), vpcUuid);
            TrashNetworkVspAnswer answer = (TrashNetworkVspAnswer)_agentMgr.easySend(nuageVspHost.getId(), cmd);
            if (answer == null || !answer.getResult()) {
                s_logger.error("TrashNetworkNuageVspCommand for network " + network.getUuid() + " failed");
                if ((null != answer) && (null != answer.getDetails())) {
                    s_logger.error(answer.getDetails());
                }
            }
        } catch (Exception e) {
            s_logger.warn("Failed to clean up network information in Vsp " + e.getMessage());
        }

        return super.trash(network, offering);
    }

    private String getVirtualRouterIP(Network network, Collection<String> addressRange) throws InsufficientVirtualNetworkCapacityException {
        String virtualRouterIp;
        String subnet = NetUtils.getCidrSubNet(network.getCidr());
        String netmask = NetUtils.getCidrNetmask(network.getCidr());
        long cidrSize = NetUtils.getCidrSize(netmask);

        Set<Long> allIPsInCidr = NetUtils.getAllIpsFromCidr(subnet, cidrSize, new HashSet<Long>());

        if (allIPsInCidr.size() > 3) {
            Iterator<Long> ipIterator = allIPsInCidr.iterator();
            long vip = ipIterator.next();
            if (NetUtils.ip2Long(network.getGateway()) == vip) {
                s_logger.debug("Gateway of the Network(" + network.getUuid() + ") has the first IP " + NetUtils.long2Ip(vip));
                vip = ipIterator.next();
                virtualRouterIp = NetUtils.long2Ip(vip);
                s_logger.debug("So, reserving the 2nd IP " + virtualRouterIp + " for the Virtual Router IP in Network(" + network.getUuid() + ")");
            } else {
                virtualRouterIp = NetUtils.long2Ip(vip);
                s_logger.debug("1nd IP is not used as the gateway IP. So, reserving" + virtualRouterIp + " for the Virtual Router IP for " + "Network(" + network.getUuid() + ")");
            }
            addressRange.add(NetUtils.long2Ip(ipIterator.next()));
            addressRange.add(NetUtils.long2Ip((Long)allIPsInCidr.toArray()[allIPsInCidr.size() - 1]));
            return virtualRouterIp;
        }

        throw new InsufficientVirtualNetworkCapacityException("VSP allocates an IP for VirtualRouter." + " So, subnet should have atleast minimum 4 hosts ", Network.class,
                network.getId());
    }

    private void setIPGatewayMaskInfo(Network network, NicProfile nic, List<Map<String, String>> vmInterfacesDetails) throws InsufficientVirtualNetworkCapacityException {
        try {
            for (Map<String, String> interfaces : vmInterfacesDetails) {
                String macFromNuage = interfaces.get("mac");
                if (StringUtils.equals(macFromNuage, nic.getMacAddress())) {
                    nic.setIPv4Address(interfaces.get("ip4Address"));
                    nic.setIPv4Gateway(interfaces.get("gateway"));
                    nic.setIPv4Netmask(interfaces.get("netmask"));
                    break;
                }
            }
        } catch (Exception e) {
            s_logger.error("Failed to parse the VM interface Json response from VSP REST API. VM interface json string is  " + vmInterfacesDetails, e);
            throw new InsufficientVirtualNetworkCapacityException("Failed to parse the VM interface Json response from VSP REST API. VM interface Json " + "string is  "
                    + vmInterfacesDetails + ". So. failed to get IP for the VM from VSP address for network " + network, Network.class, network.getId());
        }
    }

    private boolean isL3Network(Long offeringId) {
        return _ntwkOfferingSrvcDao.areServicesSupportedByNetworkOffering(offeringId, Service.SourceNat)
                || _ntwkOfferingSrvcDao.areServicesSupportedByNetworkOffering(offeringId, Service.StaticNat);
    }

    private HostVO getNuageVspHost(long physicalNetworkId) throws InsufficientVirtualNetworkCapacityException {
        HostVO nuageVspHost;
        List<NuageVspDeviceVO> nuageVspDevices = _nuageVspDao.listByPhysicalNetwork(physicalNetworkId);
        if (nuageVspDevices != null && (!nuageVspDevices.isEmpty())) {
            NuageVspDeviceVO config = nuageVspDevices.iterator().next();
            nuageVspHost = _hostDao.findById(config.getHostId());
            _hostDao.loadDetails(nuageVspHost);
        } else {
            throw new InsufficientVirtualNetworkCapacityException("Nuage VSD is not configured on physical network ", PhysicalNetwork.class, physicalNetworkId);
        }
        return nuageVspHost;
    }
}
