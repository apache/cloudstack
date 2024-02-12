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

package com.cloud.network;

import java.util.List;

import javax.inject.Inject;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.engine.cloud.entity.api.db.VMNetworkMapVO;
import org.apache.cloudstack.engine.cloud.entity.api.db.dao.VMNetworkMapDao;
import org.apache.cloudstack.engine.orchestration.service.NetworkOrchestrationService;
import org.apache.cloudstack.resourcedetail.FirewallRuleDetailVO;
import org.apache.cloudstack.resourcedetail.VpcDetailVO;
import org.apache.cloudstack.resourcedetail.dao.FirewallRuleDetailsDao;
import org.apache.cloudstack.resourcedetail.dao.VpcDetailsDao;

import com.cloud.configuration.Resource;
import com.cloud.dc.DataCenter;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.deploy.DataCenterDeployment;
import com.cloud.deploy.DeployDestination;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InsufficientVirtualNetworkCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.host.Host;
import com.cloud.host.dao.HostDao;
import com.cloud.network.dao.FirewallRulesCidrsDao;
import com.cloud.network.dao.FirewallRulesCidrsVO;
import com.cloud.network.dao.FirewallRulesDao;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.IPAddressVO;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkDetailVO;
import com.cloud.network.dao.NetworkDetailsDao;
import com.cloud.network.dao.NetworkDomainDao;
import com.cloud.network.dao.NetworkDomainVO;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.dao.RouterNetworkDao;
import com.cloud.network.dao.RouterNetworkVO;
import com.cloud.network.guru.NetworkGuru;
import com.cloud.network.router.VirtualRouter;
import com.cloud.network.rules.FirewallRule;
import com.cloud.network.rules.FirewallRuleVO;
import com.cloud.network.rules.RulesManager;
import com.cloud.network.vpc.NetworkACLVO;
import com.cloud.network.vpc.Vpc;
import com.cloud.network.vpc.VpcGatewayVO;
import com.cloud.network.vpc.VpcManager;
import com.cloud.network.vpc.VpcService;
import com.cloud.network.vpc.VpcVO;
import com.cloud.network.vpc.dao.NetworkACLDao;
import com.cloud.network.vpc.dao.VpcDao;
import com.cloud.network.vpc.dao.VpcGatewayDao;
import com.cloud.offering.NetworkOffering;
import com.cloud.offerings.dao.NetworkOfferingDao;
import com.cloud.offerings.dao.NetworkOfferingServiceMapDao;
import com.cloud.server.ResourceTag;
import com.cloud.tags.ResourceTagVO;
import com.cloud.tags.dao.ResourceTagDao;
import com.cloud.user.Account;
import com.cloud.user.AccountVO;
import com.cloud.user.ResourceLimitService;
import com.cloud.user.dao.AccountDao;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.EntityManager;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallback;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.DomainRouterVO;
import com.cloud.vm.Nic;
import com.cloud.vm.NicDetailVO;
import com.cloud.vm.NicProfile;
import com.cloud.vm.NicVO;
import com.cloud.vm.ReservationContext;
import com.cloud.vm.ReservationContextImpl;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineManager;
import com.cloud.vm.VirtualMachineProfile;
import com.cloud.vm.VirtualMachineProfileImpl;
import com.cloud.vm.dao.DomainRouterDao;
import com.cloud.vm.dao.NicDao;
import com.cloud.vm.dao.NicDetailsDao;
import com.cloud.vm.dao.NicIpAliasDao;
import com.cloud.vm.dao.NicSecondaryIpDao;
import com.cloud.vm.dao.UserVmDao;

public class NetworkMigrationManagerImpl implements NetworkMigrationManager {
    protected Logger logger = LogManager.getLogger(getClass());

    @Inject
    private DataCenterDao _dcDao = null;
    @Inject
    private NetworkDetailsDao _networkDetailsDao = null;
    @Inject
    private AccountDao _accountDao = null;
    @Inject
    private NetworkDomainDao _networkDomainDao = null;
    @Inject
    private NetworkOrchestrationService _networkMgr = null;
    @Inject
    private ResourceLimitService _resourceLimitMgr = null;
    @Inject
    private NetworkDao _networksDao = null;
    @Inject
    private NetworkOfferingDao _networkOfferingDao = null;
    @Inject
    private NetworkOfferingServiceMapDao _networkOfferingServiceDao = null;
    @Inject
    private NicDao _nicDao = null;
    @Inject
    private NicSecondaryIpDao _nicSecondaryIpDao = null;
    @Inject
    private NicIpAliasDao _nicIpAliasDao = null;
    @Inject
    private NicDetailsDao _nicDetailsDao = null;
    @Inject
    private FirewallRulesDao _firewallDao = null;
    @Inject
    private FirewallRulesCidrsDao _firewallRulesCidrDao = null;
    @Inject
    private FirewallRuleDetailsDao _firewallRuleDetailsDao = null;
    @Inject
    private EntityManager _entityMgr = null;
    @Inject
    private RouterNetworkDao _routerNetworkDao = null;
    @Inject
    private DomainRouterDao _routerDao = null;
    @Inject
    private NetworkService _networkService = null;
    @Inject
    private UserVmDao _vmDao = null;
    @Inject
    private NetworkModel _networkModel= null;
    @Inject
    private VMNetworkMapDao _vmNetworkMapDao = null;
    @Inject
    private HostDao _hostDao = null;
    @Inject
    private VirtualMachineManager _itMgr = null;
    @Inject
    private IPAddressDao _ipAddressDao = null;
    @Inject
    private RulesManager _rulesMgr = null;
    @Inject
    private VpcDao _vpcDao = null;
    @Inject
    private VpcDetailsDao _vpcDetailsDao = null;
    @Inject
    private IpAddressManager _ipAddressManager = null;
    @Inject
    private VpcService _vpcService = null;
    @Inject
    private NetworkACLDao _networkACLDao = null;
    @Inject
    private VpcManager _vpcManager = null;
    @Inject
    private VpcGatewayDao _vpcGatewayDao = null;
    @Inject
    private ResourceTagDao _resourceTagDao = null;

    @Override public long makeCopyOfNetwork(Network network, NetworkOffering networkOffering, Long vpcId) {
        if (logger.isDebugEnabled()) {
            logger.debug("Making a copy of network with uuid " + network.getUuid() + " and id " + network.getId() + " for migration.");
        }
        long originalNetworkId = network.getId();
        NetworkDomainVO domainNetworkMapByNetworkId = _networkDomainDao.getDomainNetworkMapByNetworkId(originalNetworkId);
        AccountVO networkAccount = _accountDao.findById(network.getAccountId());

        boolean subdomainAccess = true;
        if (domainNetworkMapByNetworkId != null) {
            subdomainAccess = domainNetworkMapByNetworkId.isSubdomainAccess();
        }
        DataCenterDeployment plan = new DataCenterDeployment(network.getDataCenterId(), null, null, null, null, network.getPhysicalNetworkId());

        List<? extends Network> networks = _networkMgr.setupNetwork(networkAccount, networkOffering, network, plan, network.getName(), network.getDisplayText(), true,
                                                                    network.getDomainId(), network.getAclType(), subdomainAccess,
                                                                    vpcId, true);
        _resourceLimitMgr.incrementResourceCount(network.getAccountId(), Resource.ResourceType.network, network.isDisplay());

        long networkCopyId;
        if (networks == null || networks.isEmpty()) {
            throw new CloudRuntimeException("Fail to create a network");
        } else {
            DataCenter zone = _dcDao.findById(network.getDataCenterId());
            String guestNetworkCidr = zone.getGuestNetworkCidr();

            if (networks.get(0).getGuestType() == Network.GuestType.Isolated
                    && networks.get(0).getTrafficType() == Networks.TrafficType.Guest) {
                Network networkCopy = networks.get(0);
                for (final Network nw : networks) {
                    if (nw.getCidr() != null && nw.getCidr().equals(guestNetworkCidr)) {
                        networkCopy = nw;
                    }
                }
                networkCopyId = networkCopy.getId();
            } else {
                // For shared network
                networkCopyId = networks.get(0).getId();
            }
        }

        //Update the related network
        NetworkVO originalNetwork = _networksDao.findById(originalNetworkId);
        originalNetwork.setRelated(networkCopyId);
        _networksDao.update(originalNetworkId, originalNetwork);

        NetworkVO copiedNetwork = _networksDao.findById(networkCopyId);
        copiedNetwork.setRelated(originalNetworkId);
        copiedNetwork.setDisplayNetwork(false);
        copiedNetwork.setBroadcastUri(network.getBroadcastUri());
        copiedNetwork.setState(network.getState());
        _networksDao.update(networkCopyId, copiedNetwork);

        copyNetworkDetails(originalNetworkId, networkCopyId);
        copyFirewallRulesToNewNetwork(network, networkCopyId);
        assignUserNicsToNewNetwork(originalNetworkId, networkCopyId);
        assignRouterNicsToNewNetwork(network.getId(), networkCopyId);

        if (logger.isDebugEnabled()) {
            logger.debug("Successfully created a copy of network  " + originalNetwork.getName() + "(" + originalNetwork.getUuid() + ") id is " + originalNetwork.getId() + " for migration. The network copy has uuid " + network.getUuid() + " and id " + network.getId());
        }
        return networkCopyId;
    }

    @DB
    private void copyNetworkDetails(long srcNetworkId, long dstNetworkId) {
        List<NetworkDetailVO> networkDetails = _networkDetailsDao.listDetails(srcNetworkId);

        for (NetworkDetailVO networkDetail : networkDetails) {
            _networkDetailsDao.persist(new NetworkDetailVO(dstNetworkId, networkDetail.getName(), networkDetail.getValue(), networkDetail.isDisplay()));
        }
    }

    /**
     * reassigns the nics to the new network from the src network.
     * @param srcNetworkId
     * @param dstNetworkId
     */
    private void assignUserNicsToNewNetwork(long srcNetworkId, long dstNetworkId) {
        List<NicVO> nics = _nicDao.listByNetworkId(srcNetworkId);

        for (NicVO nic : nics) {
            if (nic.getVmType() == VirtualMachine.Type.User) {
                nic.setNetworkId(dstNetworkId);
                _nicDao.persist(nic);

                //update the number of active nics in both networks after migration.
                if (nic.getState() == Nic.State.Reserved) {
                    _networksDao.changeActiveNicsBy(srcNetworkId, -1);
                    _networksDao.changeActiveNicsBy(dstNetworkId, 1);
                }
            }
        }

        List<? extends IPAddressVO> publicIps = _ipAddressDao.listByAssociatedNetwork(srcNetworkId, null);

        for (IPAddressVO ipAddress : publicIps) {
                ipAddress.setAssociatedWithNetworkId(dstNetworkId);
                _ipAddressDao.persist(ipAddress);
        }
    }

    @Override
    public Long makeCopyOfVpc(long vpcId, long vpcOfferingId) {
        VpcVO vpc = _vpcDao.findById(vpcId);
        if (logger.isDebugEnabled()) {
            logger.debug("Making a copy of vpc with uuid " + vpc.getUuid() + " and id " + vpc.getId() + " for migration.");
        }
        if (vpc == null) {
            InvalidParameterValueException ex = new InvalidParameterValueException("Specified vpc id doesn't exist in the system");
            ex.addProxyObject(String.valueOf(vpcId), "vpcId");
            throw ex;
        }

        Vpc copyOfVpc;
        long copyOfVpcId;
        try {

            copyOfVpc = _vpcService.createVpc(vpc.getZoneId(), vpcOfferingId, vpc.getAccountId(), vpc.getName(),
                    vpc.getDisplayText(), vpc.getCidr(), vpc.getNetworkDomain(), vpc.getIp4Dns1(), vpc.getIp4Dns2(),
                    vpc.getIp6Dns1(), vpc.getIp6Dns2(), vpc.isDisplay(), vpc.getPublicMtu());

            copyOfVpcId = copyOfVpc.getId();
            //on resume of migration the uuid will be swapped already. So the copy will have the value of the original vpcid.
            _resourceTagDao.persist(new ResourceTagVO(MIGRATION, Long.toString(vpcId), vpc.getAccountId(), vpc.getDomainId(), copyOfVpcId, ResourceTag.ResourceObjectType.Vpc, null, vpc.getUuid()));
            VpcVO copyVpcVO = _vpcDao.findById(copyOfVpcId);
            vpc.setDisplay(false);
            swapUuids(vpc, copyVpcVO);
            reassignACLRulesToNewVpc(vpcId, copyOfVpcId);
            reassignPublicIpsToNewVpc(vpcId, copyOfVpc);
            copyVpcDetails(vpcId, copyOfVpcId);
            reassignGatewayToNewVpc(vpcId, copyOfVpcId);
            copyVpcResourceTagsToNewVpc(vpcId, copyOfVpcId);
            if (logger.isDebugEnabled()) {
                logger.debug("Successfully created a copy of network  " + vpc.getName() + "(" + vpc.getUuid() + ") id is " + vpc.getId() + " for migration. The network copy has uuid " + copyVpcVO.getUuid() + " and id " + copyOfVpc.getId());
            }
        } catch (ResourceAllocationException e) {
            throw new CloudRuntimeException(e.getMessage());
        }

        return copyOfVpcId;
    }

    @Override
    public void startVpc(Vpc vpc) {
        if (vpc.getState() != Vpc.State.Enabled) {
            try {
                _vpcService.startVpc(vpc.getId(), true);
            } catch (ResourceUnavailableException | InsufficientCapacityException e) {
                logger.error("Vpc can not be started. Aborting migration process");
                throw new CloudRuntimeException("Vpc can not be started.", e);
            }
        }
    }


    private void copyVpcDetails(long srcVpcId, long dstVpcId) {
        List<VpcDetailVO> vpcDetails = _vpcDetailsDao.listDetails(srcVpcId);

        for (VpcDetailVO vpcDetail : vpcDetails) {
            _vpcDetailsDao.persist(new VpcDetailVO(dstVpcId, vpcDetail.getName(), vpcDetail.getValue(), vpcDetail.isDisplay()));
        }
    }

    /**
     * Reassign the ACL rules from the original vpc to the new VPC
     * @param srcVpcId
     * @param dstVpcId
     */
    private void reassignACLRulesToNewVpc(long srcVpcId, long dstVpcId){
        List<NetworkACLVO> networkACL = _networkACLDao.listByVpcId(srcVpcId);

        for (NetworkACLVO aclList : networkACL) {
            aclList.setVpcId(dstVpcId);
            _networkACLDao.persist(aclList);
        }
    }

    private void reassignPublicIpsToNewVpc(long srcVpcId, Vpc dstVpc) {
        List<? extends IPAddressVO> publicIps = _ipAddressDao.listByAssociatedVpc(srcVpcId, _vpcManager.isSrcNatIpRequired(dstVpc.getVpcOfferingId()) ? null : false);

        for(IPAddressVO publicIp : publicIps) {
            publicIp.setVpcId(dstVpc.getId());
            _ipAddressDao.persist(publicIp);
        }
    }

    private void reassignGatewayToNewVpc(long srcVpcId, long dstVpcId){
        List<VpcGatewayVO> vpcGateways = _vpcGatewayDao.listByVpcId(srcVpcId);
        for (VpcGatewayVO vpcGateway : vpcGateways) {
            vpcGateway.setVpcId(dstVpcId);
            _vpcGatewayDao.persist(vpcGateway);
        }
    }

    private void copyVpcResourceTagsToNewVpc(long srcVpcId, long dstVpcId){
        List<? extends ResourceTag> resourceTags = _resourceTagDao.listBy(srcVpcId, ResourceTag.ResourceObjectType.Vpc);
        for (ResourceTag resourceTag : resourceTags) {
            resourceTag.setResourceId(dstVpcId);
            _resourceTagDao.persist(
                    new ResourceTagVO(
                            resourceTag.getKey(),
                            resourceTag.getValue(),
                            resourceTag.getAccountId(),
                            resourceTag.getDomainId(),
                            dstVpcId,
                            resourceTag.getResourceType(),
                            resourceTag.getCustomer(),
                            resourceTag.getResourceUuid()));
        }
    }

    private void copyFirewallRulesToNewNetwork(Network srcNetwork, long dstNetworkId) {
        List<FirewallRuleVO> firewallRules = _firewallDao.listByNetworkPurposeTrafficType(srcNetwork.getId(), FirewallRule.Purpose.Firewall, FirewallRule.TrafficType.Egress);
        firewallRules.addAll(_firewallDao.listByNetworkPurposeTrafficType(srcNetwork.getId(), FirewallRule.Purpose.Firewall, FirewallRule.TrafficType.Ingress));
        if (logger.isDebugEnabled()) {
            logger.debug("Copying firewall rules from network with id " + srcNetwork.getId() + " to network with id " + dstNetworkId);
        }

        //Loop over all the firewall rules in the original network and copy all values to a new firewall rule
        //Copy all objects that are dependant on the firewall rules
        for (FirewallRuleVO originalFirewallRule : firewallRules) {
            FirewallRuleVO ruleVO = new FirewallRuleVO(originalFirewallRule.getXid(),
                                                       originalFirewallRule.getSourceIpAddressId(),
                                                       originalFirewallRule.getSourcePortStart(),
                                                       originalFirewallRule.getSourcePortEnd(),
                                                       originalFirewallRule.getProtocol(),
                                                       dstNetworkId,
                                                       srcNetwork.getAccountId(),
                                                       srcNetwork.getDomainId(),
                                                       originalFirewallRule.getPurpose(),
                                                       originalFirewallRule.getSourceCidrList(),
                                                       originalFirewallRule.getDestinationCidrList(),
                                                       originalFirewallRule.getIcmpCode(),
                                                       originalFirewallRule.getIcmpType(),
                                                       originalFirewallRule.getRelated(),
                                                       originalFirewallRule.getTrafficType(),
                                                       originalFirewallRule.getType());

            ruleVO = _firewallDao.persist(ruleVO);

            //Firewall rule cidrs
            List<FirewallRulesCidrsVO> firewallRulesCidrsVOS = _firewallRulesCidrDao.listByFirewallRuleId(originalFirewallRule.getId());
            for (FirewallRulesCidrsVO firewallRulesCidrVO: firewallRulesCidrsVOS) {
                _firewallRulesCidrDao.persist(new FirewallRulesCidrsVO(ruleVO.getId(), firewallRulesCidrVO.getSourceCidrList()));
            }

            //Firewall rules details
            List<FirewallRuleDetailVO> originalFirewallRuleDetails = _firewallRuleDetailsDao.listDetails(originalFirewallRule.getId());
            for (FirewallRuleDetailVO originalFirewallRuleDetail : originalFirewallRuleDetails) {
                _firewallRuleDetailsDao.persist(new FirewallRuleDetailVO(ruleVO.getId(), originalFirewallRuleDetail.getName(), originalFirewallRuleDetail.getValue(), originalFirewallRuleDetail.isDisplay()));
            }
        }
    }

    private void assignRouterNicsToNewNetwork(long srcNetworkId, long dstNetworkId) {
        final List<DomainRouterVO> routers = _routerDao.listByNetworkAndRole(srcNetworkId, VirtualRouter.Role.VIRTUAL_ROUTER);
        for (DomainRouterVO domainRouter : routers) {
            NicVO vrNic = _nicDao.findByNetworkIdAndType(srcNetworkId, VirtualMachine.Type.DomainRouter);
            vrNic.setNetworkId(dstNetworkId);
            _nicDao.update(vrNic.getId(), vrNic);

            RouterNetworkVO routerNetwork = _routerNetworkDao.findByRouterAndNetwork(domainRouter.getId(), srcNetworkId);
            routerNetwork.setNetworkId(dstNetworkId);
            _routerNetworkDao.persist(routerNetwork);
        }
    }

    @Override
    public Network upgradeNetworkToNewNetworkOffering(long networkId, long newPhysicalNetworkId, long networkOfferingId, Long vpcId) {
        logger.debug("upgrading network to network with new offering.");
        NetworkVO network = _networksDao.findById(networkId);
        NetworkOffering newOffering = _networkOfferingDao.findByIdIncludingRemoved(networkOfferingId);
        long gurusImplementing = 0;
        network.setBroadcastUri(null);
        AccountVO networkAccount = _accountDao.findById(network.getAccountId());
        DataCenterDeployment plan = new DataCenterDeployment(network.getDataCenterId(), null, null, null, null, newPhysicalNetworkId);
        for (final NetworkGuru guru : _networkMgr.getNetworkGurus()) {

            final Network designedNetwork = guru.design(newOffering, plan, network, networkAccount);
            if (designedNetwork == null) {
                continue;
            }

            gurusImplementing++;
            if (gurusImplementing > 1) {
                throw new CloudRuntimeException("Failed to migrate network to new physical network. Multiple network guru's for the same network are currently not supported.");
            }

            network.setTrafficType(designedNetwork.getTrafficType());
            network.setMode(designedNetwork.getMode());
            network.setBroadcastDomainType(designedNetwork.getBroadcastDomainType());
            network.setBroadcastUri(designedNetwork.getBroadcastUri());
            network.setNetworkOfferingId(designedNetwork.getNetworkOfferingId());
            network.setState(designedNetwork.getState());
            network.setPhysicalNetworkId(designedNetwork.getPhysicalNetworkId());
            network.setRedundant(designedNetwork.isRedundant());
            network.setGateway(designedNetwork.getGateway());
            network.setCidr(designedNetwork.getCidr());
            network.setGuruName(guru.getName());
            network.setVpcId(vpcId);
        }
        _networksDao.update(network.getId(), network, _networkMgr.finalizeServicesAndProvidersForNetwork(_entityMgr.findById(NetworkOffering.class, networkOfferingId), newPhysicalNetworkId));
        return network;
    }

    @Override
    public void deleteCopyOfNetwork(long networkCopyId, long originalNetworkId) {
        NetworkVO networkCopy = _networksDao.findById(networkCopyId);

        NicVO userNic = _nicDao.findByNetworkIdAndType(networkCopyId, VirtualMachine.Type.User);
        if (userNic != null) {
            logger.error("Something went wrong while migrating nics from the old network to the new network. Failed to delete copy of network. There are still user nics present in the network.");
            throw new CloudRuntimeException("Failed to delete copy of network. There are still user nics present in the network.");
        }

        NetworkVO originalNetwork = _networksDao.findById(originalNetworkId);

        swapUuids(originalNetwork, networkCopy);
        try {
            if (!_networkService.deleteNetwork(networkCopy.getId(), true)) {
                throw new CloudRuntimeException("Failed to delete network. Clean up not successful.");
            }
        } finally {
            swapUuids(networkCopy, originalNetwork);
        }
        originalNetwork.setRelated(originalNetworkId);
        _networksDao.persist(originalNetwork);
    }

    @Override
    public void deleteCopyOfVpc(long vpcCopyId, long originalVpcId) {
        VpcVO copyOfvpc = _vpcDao.findById(vpcCopyId);
        VpcVO originalVpc = _vpcDao.findById(originalVpcId);

        //Be sure that when we delete the vpc, it has the uuid with what it was created.
        swapUuids(copyOfvpc, originalVpc);
        try {
            if(!_vpcService.deleteVpc(vpcCopyId)) {
                throw new CloudRuntimeException("Deletion of VPC failed. Clean up was not successful.");
            }
        } catch (ResourceUnavailableException e) {
            throw new CloudRuntimeException(e.getMessage());
        } finally {
            swapUuids(originalVpc, copyOfvpc);
        }
        _resourceTagDao.removeByResourceIdAndKey(originalVpcId, ResourceTag.ResourceObjectType.Vpc, MIGRATION);
    }

    private Boolean migrateNicsInDB(NicVO originalNic, Network networkInNewPhysicalNet, DataCenter dc, ReservationContext context) {
        logger.debug("migrating nics in database.");
        UserVmVO vmVO = _vmDao.findById(originalNic.getInstanceId());
        VirtualMachineProfile vmProfile = new VirtualMachineProfileImpl(vmVO, null, null, null, null);
        NicProfile nicProfile = new NicProfile(originalNic, networkInNewPhysicalNet, null, null, null, _networkModel.isSecurityGroupSupportedInNetwork(networkInNewPhysicalNet), null);
        try {
            nicProfile = _networkMgr.allocateNic(nicProfile, networkInNewPhysicalNet, originalNic.isDefaultNic(), nicProfile.getDeviceId(), vmProfile).first();
        } catch (InsufficientVirtualNetworkCapacityException | InsufficientAddressCapacityException e) {
            throw new CloudRuntimeException("Allocation of new nicProfile failed during migration", e);
        }

        //Update vm_network_map table
        if (vmProfile.getType() == VirtualMachine.Type.User) {
            final VMNetworkMapVO vno = new VMNetworkMapVO(vmVO.getId(), networkInNewPhysicalNet.getId());
            _vmNetworkMapDao.persist(vno);
        }

        NicVO newNic = _nicDao.findById(nicProfile.getId());

        copyNicDetails(originalNic.getId(), newNic.getId());
        //Update nic uuid here
        moveServices(originalNic, newNic);

        if (originalNic.getState() == Nic.State.Reserved) {
            final VirtualMachine vm = vmProfile.getVirtualMachine();
            final Host host = _hostDao.findById(vm.getHostId());
            final DeployDestination dest = new DeployDestination(dc, null, null, host);

            try {
                nicProfile = _networkMgr.prepareNic(vmProfile, dest, context, nicProfile.getId(), networkInNewPhysicalNet);
                 _itMgr.replugNic(networkInNewPhysicalNet, _itMgr.toNicTO(nicProfile, host.getHypervisorType()), _itMgr.toVmTO(vmProfile), dest.getHost());
            } catch (ResourceUnavailableException | InsufficientCapacityException e) {
                throw new CloudRuntimeException("Migration of Nic failed", e);
            }
        }

        //Mark the old nic as removed
        markAsNonDefault(originalNic);
        _networkMgr.removeNic(vmProfile, originalNic);

        if (logger.isDebugEnabled()) {
            logger.debug("Nic is migrated successfully for vm " + vmVO + " to " + networkInNewPhysicalNet);
        }
        return true;
    }

    @Override
    public void assignNicsToNewPhysicalNetwork(Network srcNetwork, Network networkInNewPhysicalNet) {
        List<NicVO> nics = _nicDao.listByNetworkId(srcNetwork.getId());

        final CallContext cctx = CallContext.current();
        final ReservationContext context = new ReservationContextImpl(null, null, cctx.getCallingUser(), cctx.getCallingAccount());
        final DataCenter dc = _entityMgr.findById(DataCenter.class, networkInNewPhysicalNet.getDataCenterId());

        //For each nic in the old network check if the nic belongs to a guest vm and migrate it to the new network.
        for (NicVO originalNic : nics) {
            if (!VirtualMachine.Type.User.equals(originalNic.getVmType())) {
                continue;
            }
            Transaction.execute((TransactionCallback<Boolean>)
                                            (status) -> migrateNicsInDB(originalNic, networkInNewPhysicalNet, dc, context));
        }

        //Now that nics are migrated we can migrate the static nats on those nics
        reapplyPublicIps(srcNetwork, networkInNewPhysicalNet);
    }

    private void reapplyPublicIps(Network networkInOldPhysicalNetwork, Network networkInNewPhysicalNet) {
        CallContext ctx = CallContext.current();
        long callerUserId = ctx.getCallingUserId();
        Account caller = ctx.getCallingAccount();

        AccountVO networkAccount = _accountDao.findById(networkInNewPhysicalNet.getAccountId());
        List<? extends IPAddressVO> staticNatIps = _ipAddressDao.listStaticNatPublicIps(networkInOldPhysicalNetwork.getId());

        List<String> providers = _networkOfferingServiceDao.listProvidersForServiceForNetworkOffering(networkInNewPhysicalNet.getNetworkOfferingId(), Network.Service.SourceNat);
        boolean isSrcNatIpNeeded = providers.stream().anyMatch(provider -> provider.contains(Network.Provider.VirtualRouter.getName()));

        for (IPAddressVO ipAddress : staticNatIps) {
            if (!ipAddress.isSourceNat() || isSrcNatIpNeeded) {
                ipAddress.setAssociatedWithNetworkId(networkInNewPhysicalNet.getId());
                _ipAddressDao.persist(ipAddress);
            } else {
                _ipAddressManager.disassociatePublicIpAddress(ipAddress.getId(), callerUserId, caller);
            }
        }

        _rulesMgr.applyStaticNatsForNetwork(networkInNewPhysicalNet.getId(), false, networkAccount);
    }

    private void copyNicDetails(long originalNicId, long dstNicId) {
        List<NicDetailVO> nicDetails = _nicDetailsDao.listDetails(originalNicId);

        for (NicDetailVO nicDetail : nicDetails) {
            _nicDetailsDao.persist(new NicDetailVO(dstNicId, nicDetail.getName(), nicDetail.getValue(), nicDetail.isDisplay()));
        }
    }

    private void moveServices(NicVO originalNic, NicVO newNic) {
        _nicIpAliasDao.moveIpAliases(originalNic.getId(), newNic.getId());
        _nicSecondaryIpDao.moveSecondaryIps(originalNic.getId(), newNic.getId());
        swapUuids(originalNic, newNic);
    }

    private void markAsNonDefault(NicVO nic) {
        nic.setDefaultNic(false);
        _nicDao.persist(nic);
    }

    /**
     * Swaps the UUID's of the given nics's
     * @param oldNic
     * @param newNic
     */
    private void swapUuids(NicVO oldNic, NicVO newNic) {
        final String realUuid = oldNic.getUuid();
        final String dummyUuid = newNic.getUuid();

        oldNic.setUuid(dummyUuid.replace("-", "+"));
        newNic.setUuid(realUuid);
        _nicDao.persist(oldNic);
        _nicDao.persist(newNic);

        oldNic.setUuid(dummyUuid);
        _nicDao.persist(oldNic);
    }

    /**
     * Swaps the UUID's of the given networks
     * @param oldNetwork
     * @param newNetwork
     */
    private void swapUuids(NetworkVO oldNetwork, NetworkVO newNetwork) {
        String realUuid = oldNetwork.getUuid();
        String dummyUuid = newNetwork.getUuid();

        oldNetwork.setUuid(dummyUuid.replace("-","+"));
        newNetwork.setUuid(realUuid);
        _networksDao.update(oldNetwork.getId(), oldNetwork);
        _networksDao.update(newNetwork.getId(), newNetwork);

        oldNetwork.setUuid(dummyUuid);
        _networksDao.update(oldNetwork.getId(), oldNetwork);
    }

    /**
     * Swaps the UUID's of the given vpcs
     * @param oldVpc
     * @param newVpc
     */
    private void swapUuids(VpcVO oldVpc, VpcVO newVpc) {
        String realUuid = oldVpc.getUuid();
        String dummyUuid = newVpc.getUuid();

        oldVpc.setUuid(dummyUuid.replace("-","+"));
        newVpc.setUuid(realUuid);
        _vpcDao.update(oldVpc.getId(), oldVpc);
        _vpcDao.update(newVpc.getId(), newVpc);

        oldVpc.setUuid(dummyUuid);
        _vpcDao.update(oldVpc.getId(), oldVpc);
    }

}
