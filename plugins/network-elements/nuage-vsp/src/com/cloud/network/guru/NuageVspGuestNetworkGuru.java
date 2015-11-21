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
import com.cloud.dc.DataCenter;
import com.cloud.dc.DataCenter.NetworkType;
import com.cloud.dc.VlanVO;
import com.cloud.deploy.DeployDestination;
import com.cloud.deploy.DeploymentPlan;
import com.cloud.domain.Domain;
import com.cloud.domain.DomainVO;
import com.cloud.domain.dao.DomainDao;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.InsufficientVirtualNetworkCapacityException;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.network.IpAddress;
import com.cloud.network.Network;
import com.cloud.network.Network.GuestType;
import com.cloud.network.Network.Service;
import com.cloud.network.Network.State;
import com.cloud.network.NetworkProfile;
import com.cloud.network.Networks;
import com.cloud.network.NuageVspDeviceVO;
import com.cloud.network.PhysicalNetwork;
import com.cloud.network.PhysicalNetwork.IsolationMethod;
import com.cloud.network.dao.IPAddressVO;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.dao.NuageVspDao;
import com.cloud.network.dao.PhysicalNetworkVO;
import com.cloud.network.manager.NuageVspManager;
import com.cloud.network.vpc.Vpc;
import com.cloud.network.vpc.dao.VpcDao;
import com.cloud.offering.NetworkOffering;
import com.cloud.offerings.dao.NetworkOfferingDao;
import com.cloud.offerings.dao.NetworkOfferingServiceMapDao;
import com.cloud.user.Account;
import com.cloud.user.AccountVO;
import com.cloud.user.dao.AccountDao;
import com.cloud.util.NuageVspUtil;
import com.cloud.utils.db.DB;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.net.NetUtils;
import com.cloud.vm.NicProfile;
import com.cloud.vm.NicVO;
import com.cloud.vm.ReservationContext;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineProfile;
import com.google.common.base.Strings;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import javax.inject.Inject;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

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

            long dcId = dest.getDataCenter().getId();
            //Get physical network id
            Long physicalNetworkId = network.getPhysicalNetworkId();
            //Physical network id can be null in Guest Network in Basic zone, so locate the physical network
            if (physicalNetworkId == null) {
                physicalNetworkId = _networkModel.findPhysicalNetworkId(dcId, offering.getTags(), offering.getTrafficType());
            }
            implemented = new NetworkVO(network.getTrafficType(), network.getMode(), network.getBroadcastDomainType(), network.getNetworkOfferingId(), State.Allocated,
                    network.getDataCenterId(), physicalNetworkId, offering.getRedundantRouter());
            if (network.getGateway() != null) {
                implemented.setGateway(network.getGateway());
            }
            if (network.getCidr() != null) {
                implemented.setCidr(network.getCidr());
            }
            List<String[]> ipAddressRanges = new ArrayList<String[]>();
            String virtualRouterIp = getVirtualRouterIP(network, ipAddressRanges);
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
                String errorMessage = "Networks created by account " + networksAccount.getAccountName() + " of type Project (" + Account.ACCOUNT_TYPE_PROJECT + ") " +
                        "are not yet supported by NuageVsp provider";
                s_logger.error(errorMessage);
                throw new InsufficientVirtualNetworkCapacityException(errorMessage, Account.class, network.getAccountId());
            }
            String vpcName = null;
            String vpcUuid = null;
            String preConfiguredDomainTemplateName = NuageVspUtil.getPreConfiguredDomainTemplateName(_configDao, network, offering);
            boolean isSharedNetwork = offering.getGuestType() == GuestType.Shared;
            boolean isL3Network = !isVpc && (isSharedNetwork || isL3Network(network));

            if (isVpc) {
                Vpc vpcObj = _vpcDao.findById(vpcId);
                vpcName = vpcObj.getName();
                vpcUuid = vpcObj.getUuid();
            }

            if (isSharedNetwork) {
                List<VlanVO> vlans = _vlanDao.listVlansByNetworkId(network.getId());
                for (VlanVO vlan : vlans) {
                    boolean isIpv4 = StringUtils.isNotBlank(vlan.getIpRange());
                    String[] range = isIpv4 ? vlan.getIpRange().split("-") : vlan.getIp6Range().split("-");
                    ipAddressRanges.add(range);
                }
            }

            HostVO nuageVspHost = getNuageVspHost(physicalNetworkId);
            List<String> dnsServers = _nuageVspManager.getDnsDetails(network);
            List<String> gatewaySystemIds = _nuageVspManager.getGatewaySystemIds();
            ImplementNetworkVspCommand.Builder cmdBuilder = new ImplementNetworkVspCommand.Builder()
                    .networkDomainName(networksDomain.getName())
                    .networkDomainPath(networksDomain.getPath())
                    .networkDomainUuid(networksDomain.getUuid())
                    .networkAccountName(networksAccount.getAccountName())
                    .networkAccountUuid(networksAccount.getUuid())
                    .networkName(network.getName())
                    .networkCidr(network.getCidr())
                    .networkGateway(network.getGateway())
                    .networkAclId(network.getNetworkACLId())
                    .dnsServers(dnsServers)
                    .gatewaySystemIds(gatewaySystemIds)
                    .networkUuid(network.getUuid())
                    .isL3Network(isL3Network)
                    .isVpc(isVpc)
                    .isSharedNetwork(isSharedNetwork)
                    .vpcName(vpcName)
                    .vpcUuid(vpcUuid)
                    .defaultEgressPolicy(offering.getEgressDefaultPolicy())
                    .ipAddressRange(ipAddressRanges)
                    .domainTemplateName(preConfiguredDomainTemplateName);
            Answer answer = _agentMgr.easySend(nuageVspHost.getId(), cmdBuilder.build());

            if (answer == null || !answer.getResult()) {
                s_logger.error("ImplementNetworkVspCommand for network " + network.getUuid() + " failed on Nuage VSD " + nuageVspHost.getDetail("hostname"));
                if ((null != answer) && (null != answer.getDetails())) {
                    s_logger.error(answer.getDetails());
                }
                return null;
            }
            s_logger.info("Implemented OK, network " + networkUuid + " in tenant " + tenantId + " linked to " + implemented.getBroadcastUri());
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
            nic.setBroadcastUri(network.getBroadcastUri());
            nic.setIsolationUri(network.getBroadcastUri());
            DataCenter dc = _dcDao.findById(network.getDataCenterId());
            Account networksAccount = _accountDao.findById(network.getAccountId());
            DomainVO networksDomain = _domainDao.findById(network.getDomainId());
            //Get the Account details and find the type
            long networkOwnedBy = network.getAccountId();
            AccountVO neworkAccountDetails = _accountDao.findById(networkOwnedBy);
            if (neworkAccountDetails.getType() == Account.ACCOUNT_TYPE_PROJECT) {
                throw new InsufficientVirtualNetworkCapacityException("CS project support is not yet implemented in NuageVsp", DataCenter.class, dc.getId());
            }

            //NicProfile does not contain the NIC UUID. We need this information to set it in the VMInterface and VPort
            //that we create in VSP
            NicVO nicFrmDB = _nicDao.findById(nic.getId());
            NetworkOffering networkOffering = _ntwkOfferingDao.findById(network.getNetworkOfferingId());
            boolean isDomainRouter = vm.getType().equals(VirtualMachine.Type.DomainRouter);
            URI broadcastUri = network.getBroadcastUri();
            if (Strings.isNullOrEmpty(broadcastUri.getPath()) || !broadcastUri.getPath().startsWith("/")) {
                throw new IllegalStateException("The broadcast URI path " + network.getBroadcastUri() + " is empty or in an incorrect format.");
            }
            String domainRouterIp = network.getBroadcastUri().getPath().substring(1);
            boolean isL3Network = isL3Network(network);
            boolean isSharedNetwork = networkOffering.getGuestType() == GuestType.Shared;
            Long vpcId = network.getVpcId();
            String vpcUuid = null;
            if (vpcId != null) {
                Vpc vpcObj = _vpcDao.findById(vpcId);
                vpcUuid = vpcObj.getUuid();
            }
            HostVO nuageVspHost = getNuageVspHost(network.getPhysicalNetworkId());
            IPAddressVO staticNatIp = _ipAddressDao.findByVmIdAndNetworkId(network.getId(), vm.getId());

            ReserveVmInterfaceVspCommand.Builder cmdBuilder = new ReserveVmInterfaceVspCommand.Builder()
                    .nicUuid(nicFrmDB.getUuid())
                    .nicMacAddress(nic.getMacAddress())
                    .networkUuid(network.getUuid())
                    .isL3Network(isL3Network)
                    .isSharedNetwork(isSharedNetwork)
                    .vpcUuid(vpcUuid)
                    .networkDomainUuid(networksDomain.getUuid())
                    .networksAccountUuid(networksAccount.getUuid())
                    .isDomainRouter(isDomainRouter)
                    .domainRouterIp(domainRouterIp)
                    .vmInstanceName(vm.getInstanceName())
                    .vmUuid(vm.getUuid())
                    .vmUserName(networksDomain.getUuid())
                    .vmUserDomainName(networksAccount.getUuid())
                    .useStaticIp(true)
                    .staticIp(nic.getIPv4Address());
            if (staticNatIp != null) {
                VlanVO staticNatVlan = _vlanDao.findById(staticNatIp.getVlanId());
                cmdBuilder = cmdBuilder.staticNatIpUuid(staticNatIp.getUuid())
                        .staticNatIpAddress(staticNatIp.getAddress().addr())
                        .isStaticNatIpAllocated(staticNatIp.getState().equals(IpAddress.State.Allocated))
                        .isOneToOneNat(staticNatIp.isOneToOneNat())
                        .staticNatVlanUuid(staticNatVlan.getUuid())
                        .staticNatVlanGateway(staticNatVlan.getVlanGateway())
                        .staticNatVlanNetmask(staticNatVlan.getVlanNetmask());
            }

            Answer answer = _agentMgr.easySend(nuageVspHost.getId(), cmdBuilder.build());
            if (answer == null || !answer.getResult()) {
                s_logger.error("ReserveVmInterfaceNuageVspCommand failed for NIC " + nic.getId() + " attached to VM " + vm.getId() + " in network " + network.getId());
                if ((null != answer) && (null != answer.getDetails())) {
                    s_logger.error(answer.getDetails());
                }
                throw new InsufficientVirtualNetworkCapacityException("Failed to reserve VM in Nuage VSP.", Network.class, network.getId());
            }

            if (isDomainRouter) {
                nic.setIPv4Address(domainRouterIp);
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
            DomainVO networksDomain = _domainDao.findById(network.getDomainId());
            NicVO nicFrmDd = _nicDao.findById(nic.getId());
            NetworkOffering networkOffering = _ntwkOfferingDao.findById(network.getNetworkOfferingId());
            boolean isL3Network = isL3Network(network);
            boolean isSharedNetwork = networkOffering.getGuestType() == GuestType.Shared;
            boolean isExpunging = vm.getVirtualMachine().getState() == VirtualMachine.State.Expunging;
            Long vpcId = network.getVpcId();
            String vpcUuid = null;
            if (vpcId != null) {
                Vpc vpcObj = _vpcDao.findById(vpcId);
                vpcUuid = vpcObj.getUuid();
            }
            HostVO nuageVspHost = getNuageVspHost(network.getPhysicalNetworkId());
            DeallocateVmVspCommand.Builder cmdBuilder = new DeallocateVmVspCommand.Builder()
                    .networkUuid(network.getUuid())
                    .nicFromDbUuid(nicFrmDd.getUuid())
                    .nicMacAddress(nic.getMacAddress())
                    .nicIp4Address(nic.getIPv4Address())
                    .isL3Network(isL3Network)
                    .isSharedNetwork(isSharedNetwork)
                    .vpcUuid(vpcUuid)
                    .networksDomainUuid(networksDomain.getUuid())
                    .vmInstanceName(vm.getInstanceName())
                    .vmUuid(vm.getUuid())
                    .isExpungingState(isExpunging);
            Answer answer = _agentMgr.easySend(nuageVspHost.getId(), cmdBuilder.build());
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
            long domainId = network.getDomainId();
            Domain domain = _domainDao.findById(domainId);
            boolean isL3Network = isL3Network(network);
            boolean isSharedNetwork = offering.getGuestType() == GuestType.Shared;
            Long vpcId = network.getVpcId();
            String vpcUuid = null;
            if (vpcId != null) {
                Vpc vpcObj = _vpcDao.findById(vpcId);
                vpcUuid = vpcObj.getUuid();
            }

            String preConfiguredDomainTemplateName = NuageVspUtil.getPreConfiguredDomainTemplateName(_configDao, network, offering);
            HostVO nuageVspHost = getNuageVspHost(network.getPhysicalNetworkId());
            TrashNetworkVspCommand.Builder cmdBuilder = new TrashNetworkVspCommand.Builder()
                    .domainUuid(domain.getUuid())
                    .networkUuid(network.getUuid())
                    .isL3Network(isL3Network)
                    .isSharedNetwork(isSharedNetwork)
                    .vpcUuid(vpcUuid)
                    .domainTemplateName(preConfiguredDomainTemplateName);
            Answer answer = _agentMgr.easySend(nuageVspHost.getId(), cmdBuilder.build());
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

    private String getVirtualRouterIP(Network network, Collection<String[]> ipAddressRanges) throws InsufficientVirtualNetworkCapacityException {
        String virtualRouterIp;
        //Check if the subnet has minimum 5 host in it.
        String subnet = NetUtils.getCidrSubNet(network.getCidr());
        String netmask = NetUtils.getCidrNetmask(network.getCidr());
        long cidrSize = NetUtils.getCidrSize(netmask);

        Set<Long> allIPsInCidr = NetUtils.getAllIpsFromCidr(subnet, cidrSize, new HashSet<Long>());
        if (allIPsInCidr == null || !(allIPsInCidr instanceof TreeSet)) {
            throw new IllegalStateException("The IPs in CIDR for subnet " + subnet + " where null or returned in a non-ordered set.");
        }

        if (allIPsInCidr.size() > 3) {
            //get the second IP and see if it the networks GatewayIP
            Iterator<Long> ipIterator = allIPsInCidr.iterator();
            long vip = ipIterator.next();
            if (NetUtils.ip2Long(network.getGateway()) == vip) {
                vip = ipIterator.next();
                virtualRouterIp = NetUtils.long2Ip(vip);
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("1st IP is used as gateway IP. Reserving " + virtualRouterIp + " for the Virtual Router IP for Network(" + network.getName() + ")");
                }
            } else {
                virtualRouterIp = NetUtils.long2Ip(vip);
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("1st IP is not used as the gateway IP. Reserving" + virtualRouterIp + " for the Virtual Router IP for Network(" + network.getName() + ")");
                }
            }
            ipAddressRanges.add(new String[] {
                NetUtils.long2Ip(ipIterator.next()),
                NetUtils.getIpRangeEndIpFromCidr(subnet, cidrSize)
            });
            return virtualRouterIp;
        }

        throw new InsufficientVirtualNetworkCapacityException("VSP allocates an IP for VirtualRouter." + " So, subnet should have atleast minimum 4 hosts ", Network.class,
                network.getId());
    }

    private boolean isL3Network(Network network) {
        return _ntwkOfferingSrvcDao.areServicesSupportedByNetworkOffering(network.getNetworkOfferingId(), Service.SourceNat)
                || _ntwkOfferingSrvcDao.areServicesSupportedByNetworkOffering(network.getNetworkOfferingId(), Service.StaticNat)
                || network.getGuestType() == GuestType.Shared;
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