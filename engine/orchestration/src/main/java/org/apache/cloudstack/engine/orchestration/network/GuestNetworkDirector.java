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

package org.apache.cloudstack.engine.orchestration.network;

import com.cloud.configuration.ConfigurationManager;
import com.cloud.configuration.Resource;
import com.cloud.dc.DataCenter;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.DataCenterVnetVO;
import com.cloud.dc.Vlan;
import com.cloud.dc.VlanVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.DataCenterVnetDao;
import com.cloud.dc.dao.VlanDao;
import com.cloud.deploy.DataCenterDeployment;
import com.cloud.domain.Domain;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.Network;
import com.cloud.network.NetworkModel;
import com.cloud.network.Networks;
import com.cloud.network.PhysicalNetwork;
import com.cloud.network.dao.AccountGuestVlanMapDao;
import com.cloud.network.dao.AccountGuestVlanMapVO;
import com.cloud.network.dao.NetworkAccountDao;
import com.cloud.network.dao.NetworkAccountVO;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkDomainDao;
import com.cloud.network.dao.NetworkDomainVO;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.element.NetworkElement;
import com.cloud.network.guru.NetworkGuru;
import com.cloud.network.vpc.dao.PrivateIpDao;
import com.cloud.offering.NetworkOffering;
import com.cloud.offerings.NetworkOfferingVO;
import com.cloud.offerings.dao.NetworkOfferingDao;
import com.cloud.user.Account;
import com.cloud.user.ResourceLimitService;
import com.cloud.utils.StringUtils;
import com.cloud.utils.UuidUtils;
import com.cloud.utils.component.AdapterBase;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.EntityManager;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallback;
import com.cloud.utils.db.TransactionCallbackNoReturn;
import com.cloud.utils.db.TransactionStatus;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.fsm.NoTransitionException;
import com.cloud.utils.net.NetUtils;
import com.cloud.vm.ReservationContext;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.dao.UserVmDao;
import com.cloud.vm.dao.VMInstanceDao;
import org.apache.cloudstack.acl.ControlledEntity;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.engine.orchestration.NetworkOrchestrator;
import org.apache.log4j.Logger;

import javax.inject.Inject;
import java.net.URI;
import java.util.List;
import java.util.Map;

public class GuestNetworkDirector {
    private static final Logger LOG = Logger.getLogger(GuestNetworkDirector.class);
    private final NetworkOrchestrator networkOrchestrator;
    private NetworkOrchestrationUtility networkOrchestrationUtility;

    @Inject AccountGuestVlanMapDao accountGuestVlanMapDao;
    @Inject ConfigurationManager configMgr;
    @Inject DataCenterDao dcDao;
    @Inject DataCenterVnetDao datacenterVnetDao;
    @Inject
    EntityManager entityMgr;
    @Inject NetworkAccountDao networkAccountDao;
    @Inject NetworkDomainDao networkDomainDao;
    @Inject NetworkOfferingDao networkOfferingDao;
    @Inject NetworkDao networksDao;
    @Inject NetworkModel networkModel;
    @Inject PrivateIpDao privateIpDao;
    @Inject ResourceLimitService resourceLimitMgr;
    @Inject UserVmDao userVmDao;
    @Inject VlanDao vlanDao;
    @Inject VMInstanceDao vmDao;

    public GuestNetworkDirector(NetworkOrchestrator networkOrchestrator) {
        this.networkOrchestrator = networkOrchestrator;
        networkOrchestrationUtility = new NetworkOrchestrationUtility();
    }

    @DB public Network createGuestNetwork(final long networkOfferingId, final String name, final String displayText, final String gateway, final String cidr, String vlanId,
            boolean bypassVlanOverlapCheck, String networkDomain, final Account owner, final Long domainId, final PhysicalNetwork pNtwk, final long zoneId,
            final ControlledEntity.ACLType aclType, Boolean subdomainAccess, final Long vpcId, final String ip6Gateway, final String ip6Cidr, final Boolean isDisplayNetworkEnabled,
            final String isolatedPvlan, Network.PVlanType isolatedPvlanType, String externalId, final Boolean isPrivateNetwork)
            throws ConcurrentOperationException, InsufficientCapacityException, ResourceAllocationException {

        final NetworkOfferingVO ntwkOff = networkOfferingDao.findById(networkOfferingId);
        final DataCenterVO zone = dcDao.findById(zoneId);
        // this method supports only guest network creation
        if (ntwkOff.getTrafficType() != Networks.TrafficType.Guest) {
            LOG.warn("Only guest networks can be created using this method");
            return null;
        }

        final boolean updateResourceCount = networkOrchestrator.resourceCountNeedsUpdate(ntwkOff, aclType);
        //check resource limits
        if (updateResourceCount) {
            resourceLimitMgr.checkResourceLimit(owner, Resource.ResourceType.network, isDisplayNetworkEnabled);
        }

        // Validate network offering
        if (ntwkOff.getState() != NetworkOffering.State.Enabled) {
            // see NetworkOfferingVO
            final InvalidParameterValueException ex = new InvalidParameterValueException("Can't use specified network offering id as its stat is not " + NetworkOffering.State.Enabled);
            ex.addProxyObject(ntwkOff.getUuid(), "networkOfferingId");
            throw ex;
        }

        // Validate physical network
        if (pNtwk.getState() != PhysicalNetwork.State.Enabled) {
            // see PhysicalNetworkVO.java
            final InvalidParameterValueException ex = new InvalidParameterValueException("Specified physical network id is" + " in incorrect state:" + pNtwk.getState());
            ex.addProxyObject(pNtwk.getUuid(), "physicalNetworkId");
            throw ex;
        }

        boolean ipv6 = false;

        if (StringUtils.isNotBlank(ip6Gateway) && StringUtils.isNotBlank(ip6Cidr)) {
            ipv6 = true;
        }
        // Validate zone
        if (zone.getNetworkType() == DataCenter.NetworkType.Basic) {
            // In Basic zone the network should have aclType=Domain, domainId=1, subdomainAccess=true
            if (aclType == null || aclType != ControlledEntity.ACLType.Domain) {
                throw new InvalidParameterValueException("Only AclType=Domain can be specified for network creation in Basic zone");
            }

            // Only one guest network is supported in Basic zone
            final List<NetworkVO> guestNetworks = networksDao.listByZoneAndTrafficType(zone.getId(), Networks.TrafficType.Guest);
            if (!guestNetworks.isEmpty()) {
                throw new InvalidParameterValueException("Can't have more than one Guest network in zone with network type " + DataCenter.NetworkType.Basic);
            }

            // if zone is basic, only Shared network offerings w/o source nat service are allowed
            if (!(ntwkOff.getGuestType() == Network.GuestType.Shared && !networkModel.areServicesSupportedByNetworkOffering(ntwkOff.getId(), Network.Service.SourceNat))) {
                throw new InvalidParameterValueException(
                        "For zone of type " + DataCenter.NetworkType.Basic + " only offerings of " + "guestType " + Network.GuestType.Shared + " with disabled " + Network.Service.SourceNat.getName() + " service are allowed");
            }

            if (domainId == null || domainId != Domain.ROOT_DOMAIN) {
                throw new InvalidParameterValueException("Guest network in Basic zone should be dedicated to ROOT domain");
            }

            if (subdomainAccess == null) {
                subdomainAccess = true;
            } else if (!subdomainAccess) {
                throw new InvalidParameterValueException("Subdomain access should be set to true for the" + " guest network in the Basic zone");
            }

            if (vlanId == null) {
                vlanId = Vlan.UNTAGGED;
            } else {
                if (!vlanId.equalsIgnoreCase(Vlan.UNTAGGED)) {
                    throw new InvalidParameterValueException("Only vlan " + Vlan.UNTAGGED + " can be created in " + "the zone of type " + DataCenter.NetworkType.Basic);
                }
            }

        } else if (zone.getNetworkType() == DataCenter.NetworkType.Advanced) {
            if (zone.isSecurityGroupEnabled()) {
                if (isolatedPvlan != null) {
                    throw new InvalidParameterValueException("Isolated Private VLAN is not supported with security group!");
                }
                // Only Account specific Isolated network with sourceNat service disabled are allowed in security group
                // enabled zone
                if (ntwkOff.getGuestType() != Network.GuestType.Shared) {
                    throw new InvalidParameterValueException("Only shared guest network can be created in security group enabled zone");
                }
                if (networkModel.areServicesSupportedByNetworkOffering(ntwkOff.getId(), Network.Service.SourceNat)) {
                    throw new InvalidParameterValueException("Service SourceNat is not allowed in security group enabled zone");
                }
            }

            //don't allow eip/elb networks in Advance zone
            if (ntwkOff.isElasticIp() || ntwkOff.isElasticLb()) {
                throw new InvalidParameterValueException("Elastic IP and Elastic LB services are supported in zone of type " + DataCenter.NetworkType.Basic);
            }
        }

        if (ipv6 && NetUtils.getIp6CidrSize(ip6Cidr) != 64) {
            throw new InvalidParameterValueException("IPv6 subnet should be exactly 64-bits in size");
        }

        //TODO(VXLAN): Support VNI specified
        // VlanId can be specified only when network offering supports it
        final boolean vlanSpecified = vlanId != null;
        if (vlanSpecified != ntwkOff.isSpecifyVlan()) {
            if (vlanSpecified) {
                throw new InvalidParameterValueException("Can't specify vlan; corresponding offering says specifyVlan=false");
            } else {
                throw new InvalidParameterValueException("Vlan has to be specified; corresponding offering says specifyVlan=true");
            }
        }

        if (vlanSpecified) {
            URI uri = Networks.BroadcastDomainType.fromString(vlanId);
            // Aux: generate secondary URI for secondary VLAN ID (if provided) for performing checks
            URI secondaryUri = org.apache.commons.lang.StringUtils.isNotBlank(isolatedPvlan) ? Networks.BroadcastDomainType.fromString(isolatedPvlan) : null;
            //don't allow to specify vlan tag used by physical network for dynamic vlan allocation
            if (!(bypassVlanOverlapCheck && ntwkOff.getGuestType() == Network.GuestType.Shared)
                    && dcDao.findVnet(zoneId, pNtwk.getId(), Networks.BroadcastDomainType.getValue(uri)).size() > 0) {
                throw new InvalidParameterValueException("The VLAN tag " + vlanId + " is already being used for dynamic vlan allocation for the guest network in zone " + zone.getName());
            }
            if (secondaryUri != null && !(bypassVlanOverlapCheck && ntwkOff.getGuestType() == Network.GuestType.Shared)
                    && dcDao.findVnet(zoneId, pNtwk.getId(), Networks.BroadcastDomainType.getValue(secondaryUri)).size() > 0) {
                throw new InvalidParameterValueException("The VLAN tag " + isolatedPvlan + " is already being used for dynamic vlan allocation for the guest network in zone " + zone.getName());
            }
            if (!UuidUtils.validateUUID(vlanId)) {
                // For Isolated and L2 networks, don't allow to create network with vlan that already exists in the zone
                if (!networkOrchestrator.hasGuestBypassVlanOverlapCheck(bypassVlanOverlapCheck, ntwkOff, isPrivateNetwork)) {
                    if (networksDao.listByZoneAndUriAndGuestType(zoneId, uri.toString(), null).size() > 0) {
                        throw new InvalidParameterValueException("Network with vlan " + vlanId + " already exists or overlaps with other network vlans in zone " + zoneId);
                    } else if (secondaryUri != null && networksDao.listByZoneAndUriAndGuestType(zoneId, secondaryUri.toString(), null).size() > 0) {
                        throw new InvalidParameterValueException("Network with vlan " + isolatedPvlan + " already exists or overlaps with other network vlans in zone " + zoneId);
                    } else {
                        final List<DataCenterVnetVO> dcVnets = datacenterVnetDao.findVnet(zoneId, Networks.BroadcastDomainType.getValue(uri));
                        //for the network that is created as part of private gateway,
                        //the vnet is not coming from the data center vnet table, so the list can be empty
                        if (!dcVnets.isEmpty()) {
                            final DataCenterVnetVO dcVnet = dcVnets.get(0);
                            // Fail network creation if specified vlan is dedicated to a different account
                            if (dcVnet.getAccountGuestVlanMapId() != null) {
                                final Long accountGuestVlanMapId = dcVnet.getAccountGuestVlanMapId();
                                final AccountGuestVlanMapVO map = accountGuestVlanMapDao.findById(accountGuestVlanMapId);
                                if (map.getAccountId() != owner.getAccountId()) {
                                    throw new InvalidParameterValueException("Vlan " + vlanId + " is dedicated to a different account");
                                }
                                // Fail network creation if owner has a dedicated range of vlans but the specified vlan belongs to the system pool
                            } else {
                                final List<AccountGuestVlanMapVO> maps = accountGuestVlanMapDao.listAccountGuestVlanMapsByAccount(owner.getAccountId());
                                if (maps != null && !maps.isEmpty()) {
                                    final int vnetsAllocatedToAccount = datacenterVnetDao.countVnetsAllocatedToAccount(zoneId, owner.getAccountId());
                                    final int vnetsDedicatedToAccount = datacenterVnetDao.countVnetsDedicatedToAccount(zoneId, owner.getAccountId());
                                    if (vnetsAllocatedToAccount < vnetsDedicatedToAccount) {
                                        throw new InvalidParameterValueException("Specified vlan " + vlanId + " doesn't belong" + " to the vlan range dedicated to the owner " + owner.getAccountName());
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // don't allow to creating shared network with given Vlan ID, if there already exists a isolated network or
                    // shared network with same Vlan ID in the zone
                    if (!bypassVlanOverlapCheck && networksDao.listByZoneAndUriAndGuestType(zoneId, uri.toString(), Network.GuestType.Isolated).size() > 0) {
                        throw new InvalidParameterValueException("There is an existing isolated/shared network that overlaps with vlan id:" + vlanId + " in zone " + zoneId);
                    }
                }
            }

        }

        // If networkDomain is not specified, take it from the global configuration
        if (networkModel.areServicesSupportedByNetworkOffering(networkOfferingId, Network.Service.Dns)) {
            final Map<Network.Capability, String> dnsCapabilities = networkModel
                    .getNetworkOfferingServiceCapabilities(entityMgr.findById(NetworkOffering.class, networkOfferingId), Network.Service.Dns);
            final String isUpdateDnsSupported = dnsCapabilities.get(Network.Capability.AllowDnsSuffixModification);
            if (isUpdateDnsSupported == null || !Boolean.valueOf(isUpdateDnsSupported)) {
                if (networkDomain != null) {
                    // TBD: NetworkOfferingId and zoneId. Send uuids instead.
                    throw new InvalidParameterValueException("Domain name change is not supported by network offering id=" + networkOfferingId + " in zone id=" + zoneId);
                }
            } else {
                if (networkDomain == null) {
                    // 1) Get networkDomain from the corresponding account/domain/zone
                    if (aclType == ControlledEntity.ACLType.Domain) {
                        networkDomain = networkModel.getDomainNetworkDomain(domainId, zoneId);
                    } else if (aclType == ControlledEntity.ACLType.Account) {
                        networkDomain = networkModel.getAccountNetworkDomain(owner.getId(), zoneId);
                    }

                    // 2) If null, generate networkDomain using domain suffix from the global config variables
                    if (networkDomain == null) {
                        networkDomain = "cs" + Long.toHexString(owner.getId()) + NetworkOrchestrator.GuestDomainSuffix.valueIn(zoneId);
                    }

                } else {
                    // validate network domain
                    if (!NetUtils.verifyDomainName(networkDomain)) {
                        throw new InvalidParameterValueException("Invalid network domain. Total length shouldn't exceed 190 chars. Each domain "
                                + "label must be between 1 and 63 characters long, can contain ASCII letters 'a' through 'z', the digits '0' through '9', "
                                + "and the hyphen ('-'); can't start or end with \"-\"");
                    }
                }
            }
        }

        // In Advance zone Cidr for Shared networks and Isolated networks w/o source nat service can't be NULL - 2.2.x
        // limitation, remove after we introduce support for multiple ip ranges
        // with different Cidrs for the same Shared network
        final boolean cidrRequired = zone.getNetworkType() == DataCenter.NetworkType.Advanced && ntwkOff.getTrafficType() == Networks.TrafficType.Guest && (
                ntwkOff.getGuestType() == Network.GuestType.Shared || (ntwkOff.getGuestType() == Network.GuestType.Isolated && !networkModel
                        .areServicesSupportedByNetworkOffering(ntwkOff.getId(), Network.Service.SourceNat)));
        if (cidr == null && ip6Cidr == null && cidrRequired) {
            throw new InvalidParameterValueException(
                    "StartIp/endIp/gateway/netmask are required when create network of" + " type " + Network.GuestType.Shared + " and network of type " + Network.GuestType.Isolated
                            + " with service " + Network.Service.SourceNat.getName() + " disabled");
        }

        networkOrchestrator.checkL2OfferingServices(ntwkOff);

        // No cidr can be specified in Basic zone
        if (zone.getNetworkType() == DataCenter.NetworkType.Basic && cidr != null) {
            throw new InvalidParameterValueException("StartIp/endIp/gateway/netmask can't be specified for zone of type " + DataCenter.NetworkType.Basic);
        }

        // Check if cidr is RFC1918 compliant if the network is Guest Isolated for IPv4
        if (cidr != null && ntwkOff.getGuestType() == Network.GuestType.Isolated && ntwkOff.getTrafficType() == Networks.TrafficType.Guest) {
            if (!NetUtils.validateGuestCidr(cidr)) {
                throw new InvalidParameterValueException("Virtual Guest Cidr " + cidr + " is not RFC 1918 or 6598 compliant");
            }
        }

        final String networkDomainFinal = networkDomain;
        final String vlanIdFinal = vlanId;
        final Boolean subdomainAccessFinal = subdomainAccess;
        final Network network = Transaction.execute(new TransactionCallback<Network>() {
            @Override public Network doInTransaction(final TransactionStatus status) {
                Long physicalNetworkId = null;
                if (pNtwk != null) {
                    physicalNetworkId = pNtwk.getId();
                }
                final DataCenterDeployment plan = new DataCenterDeployment(zoneId, null, null, null, null, physicalNetworkId);
                final NetworkVO userNetwork = new NetworkVO();
                userNetwork.setNetworkDomain(networkDomainFinal);

                if (cidr != null && gateway != null) {
                    userNetwork.setCidr(cidr);
                    userNetwork.setGateway(gateway);
                }

                if (StringUtils.isNotBlank(ip6Gateway) && StringUtils.isNotBlank(ip6Cidr)) {
                    userNetwork.setIp6Cidr(ip6Cidr);
                    userNetwork.setIp6Gateway(ip6Gateway);
                }

                if (externalId != null) {
                    userNetwork.setExternalId(externalId);
                }

                if (vlanIdFinal != null) {
                    if (isolatedPvlan == null) {
                        URI uri = null;
                        if (UuidUtils.validateUUID(vlanIdFinal)) {
                            //Logical router's UUID provided as VLAN_ID
                            userNetwork.setVlanIdAsUUID(vlanIdFinal); //Set transient field
                        } else {
                            uri = Networks.BroadcastDomainType.fromString(vlanIdFinal);
                        }
                        userNetwork.setBroadcastUri(uri);
                        if (!vlanIdFinal.equalsIgnoreCase(Vlan.UNTAGGED)) {
                            userNetwork.setBroadcastDomainType(Networks.BroadcastDomainType.Vlan);
                        } else {
                            userNetwork.setBroadcastDomainType(Networks.BroadcastDomainType.Native);
                        }
                    } else {
                        if (vlanIdFinal.equalsIgnoreCase(Vlan.UNTAGGED)) {
                            throw new InvalidParameterValueException("Cannot support pvlan with untagged primary vlan!");
                        }
                        URI uri = NetUtils.generateUriForPvlan(vlanIdFinal, isolatedPvlan);
                        if (networksDao.listByPhysicalNetworkPvlan(physicalNetworkId, uri.toString(), isolatedPvlanType).size() > 0) {
                            throw new InvalidParameterValueException("Network with primary vlan " + vlanIdFinal + " and secondary vlan " + isolatedPvlan + " type " + isolatedPvlanType
                                    + " already exists or overlaps with other network pvlans in zone " + zoneId);
                        }
                        userNetwork.setBroadcastUri(uri);
                        userNetwork.setBroadcastDomainType(Networks.BroadcastDomainType.Pvlan);
                        userNetwork.setPvlanType(isolatedPvlanType);
                    }
                }

                final List<? extends Network> networks = networkOrchestrator
                        .setupNetwork(owner, ntwkOff, userNetwork, plan, name, displayText, true, domainId, aclType, subdomainAccessFinal, vpcId, isDisplayNetworkEnabled);

                Network network = null;
                if (networks == null || networks.isEmpty()) {
                    throw new CloudRuntimeException("Fail to create a network");
                } else {
                    if (networks.size() > 0 && networks.get(0).getGuestType() == Network.GuestType.Isolated && networks.get(0).getTrafficType() == Networks.TrafficType.Guest) {
                        Network defaultGuestNetwork = networks.get(0);
                        for (final Network nw : networks) {
                            if (nw.getCidr() != null && nw.getCidr().equals(zone.getGuestNetworkCidr())) {
                                defaultGuestNetwork = nw;
                            }
                        }
                        network = defaultGuestNetwork;
                    } else {
                        // For shared network
                        network = networks.get(0);
                    }
                }

                if (updateResourceCount) {
                    resourceLimitMgr.incrementResourceCount(owner.getId(), Resource.ResourceType.network, isDisplayNetworkEnabled);
                }

                return network;
            }
        });

        CallContext.current().setEventDetails("Network Id: " + network.getId());
        CallContext.current().putContextParameter(Network.class, network.getUuid());
        return network;
    }

    public boolean destroyNetwork(final long networkId, final ReservationContext context, final boolean forced) {
        final Account callerAccount = context.getAccount();

        NetworkVO network = networksDao.findById(networkId);
        if (network == null) {
            LOG.debug("Unable to find network with id: " + networkId);
            return false;
        }
        // Make sure that there are no user vms in the network that are not Expunged/Error
        final List<UserVmVO> userVms = userVmDao.listByNetworkIdAndStates(networkId);

        for (final UserVmVO vm : userVms) {
            if (!(vm.getState() == VirtualMachine.State.Expunging && vm.getRemoved() != null)) {
                LOG.warn("Can't delete the network, not all user vms are expunged. Vm " + vm + " is in " + vm.getState() + " state");
                return false;
            }
        }

        // Don't allow to delete network via api call when it has vms assigned to it
        final int nicCount = getActiveNicsInNetwork(networkId);
        if (nicCount > 0) {
            LOG.debug("The network id=" + networkId + " has active Nics, but shouldn't.");
            // at this point we have already determined that there are no active user vms in network
            // if the op_networks table shows active nics, it's a bug in releasing nics updating op_networks
            networksDao.changeActiveNicsBy(networkId, -1 * nicCount);
        }

        //In Basic zone, make sure that there are no non-removed console proxies and SSVMs using the network
        final DataCenter zone = entityMgr.findById(DataCenter.class, network.getDataCenterId());
        if (zone.getNetworkType() == DataCenter.NetworkType.Basic) {
            final List<VMInstanceVO> systemVms = vmDao.listNonRemovedVmsByTypeAndNetwork(network.getId(), VirtualMachine.Type.ConsoleProxy, VirtualMachine.Type.SecondaryStorageVm);
            if (systemVms != null && !systemVms.isEmpty()) {
                LOG.warn("Can't delete the network, not all consoleProxy/secondaryStorage vms are expunged");
                return false;
            }
        }

        // Shutdown network first
        networkOrchestrator.shutdownNetwork(networkId, context, false);

        // get updated state for the network
        network = networksDao.findById(networkId);
        if (network.getState() != Network.State.Allocated && network.getState() != Network.State.Setup && !forced) {
            LOG.debug("Network is not not in the correct state to be destroyed: " + network.getState());
            return false;
        }

        boolean success = true;
        if (!networkOrchestrator.cleanupNetworkResources(networkId, callerAccount, context.getCaller().getId())) {
            LOG.warn("Unable to delete network id=" + networkId + ": failed to cleanup network resources");
            return false;
        }

        // get providers to destroy
        final List<Network.Provider> providersToDestroy = networkOrchestrationUtility.getNetworkProviders(network.getId());
        for (final NetworkElement element : networkOrchestrator.getNetworkElements()) {
            if (providersToDestroy.contains(element.getProvider())) {
                try {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Sending destroy to " + element);
                    }

                    if (!element.destroy(network, context)) {
                        success = false;
                        LOG.warn("Unable to complete destroy of the network: failed to destroy network element " + element.getName());
                    }
                } catch (final ResourceUnavailableException e) {
                    LOG.warn("Unable to complete destroy of the network due to element: " + element.getName(), e);
                    success = false;
                } catch (final ConcurrentOperationException e) {
                    LOG.warn("Unable to complete destroy of the network due to element: " + element.getName(), e);
                    success = false;
                } catch (final Exception e) {
                    LOG.warn("Unable to complete destroy of the network due to element: " + element.getName(), e);
                    success = false;
                }
            }
        }

        if (success) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Network id=" + networkId + " is destroyed successfully, cleaning up corresponding resources now.");
            }

            final NetworkVO networkFinal = network;
            try {
                Transaction.execute(new TransactionCallbackNoReturn() {
                    @Override public void doInTransactionWithoutResult(final TransactionStatus status) {
                        final NetworkGuru guru = AdapterBase.getAdapterByName(networkOrchestrator.getNetworkGurus(), networkFinal.getGuruName());

                        if (!guru.trash(networkFinal, networkOfferingDao.findById(networkFinal.getNetworkOfferingId()))) {
                            throw new CloudRuntimeException("Failed to trash network.");
                        }

                        if (!networkOrchestrator.guestNetworkDirector.deleteVlansInNetwork(networkFinal.getId(), context.getCaller().getId(), callerAccount, networkOrchestrator)) {
                            LOG.warn("Failed to delete network " + networkFinal + "; was unable to cleanup corresponding ip ranges");
                            throw new CloudRuntimeException("Failed to delete network " + networkFinal + "; was unable to cleanup corresponding ip ranges");
                        } else {
                            // commit transaction only when ips and vlans for the network are released successfully
                            try {
                                networkOrchestrator.stateTransitTo(networkFinal, Network.Event.DestroyNetwork);
                            } catch (final NoTransitionException e) {
                                LOG.debug(e.getMessage());
                            }
                            if (networksDao.remove(networkFinal.getId())) {
                                final NetworkDomainVO networkDomain = networkDomainDao.getDomainNetworkMapByNetworkId(networkFinal.getId());
                                if (networkDomain != null) {
                                    networkDomainDao.remove(networkDomain.getId());
                                }

                                final NetworkAccountVO networkAccount = networkAccountDao.getAccountNetworkMapByNetworkId(networkFinal.getId());
                                if (networkAccount != null) {
                                    networkAccountDao.remove(networkAccount.getId());
                                }
                            }

                            final NetworkOffering ntwkOff = entityMgr.findById(NetworkOffering.class, networkFinal.getNetworkOfferingId());
                            final boolean updateResourceCount = networkOrchestrator.resourceCountNeedsUpdate(ntwkOff, networkFinal.getAclType());
                            if (updateResourceCount) {
                                resourceLimitMgr.decrementResourceCount(networkFinal.getAccountId(), Resource.ResourceType.network, networkFinal.getDisplayNetwork());
                            }
                        }
                    }
                });
                if (networksDao.findById(network.getId()) == null) {
                    networkOrchestrator.publishNetworkRemoval(networkFinal.getId());
                }
                return true;
            } catch (final CloudRuntimeException e) {
                LOG.error("Failed to delete network", e);
                return false;
            }
        }

        return success;
    }

    protected boolean deleteVlansInNetwork(final long networkId, final long userId, final Account callerAccount, NetworkOrchestrator networkOrchestrator) {

        //cleanup Public vlans
        final List<VlanVO> publicVlans = vlanDao.listVlansByNetworkId(networkId);
        boolean result = true;
        for (final VlanVO vlan : publicVlans) {
            if (!configMgr.deleteVlanAndPublicIpRange(userId, vlan.getId(), callerAccount)) {
                LOG.warn("Failed to delete vlan " + vlan.getId() + ");");
                result = false;
            }
        }

        //cleanup private vlans
        final int privateIpAllocCount = privateIpDao.countAllocatedByNetworkId(networkId);
        if (privateIpAllocCount > 0) {
            LOG.warn("Can't delete Private ip range for network " + networkId + " as it has allocated ip addresses");
            result = false;
        } else {
            privateIpDao.deleteByNetworkId(networkId);
            LOG.debug("Deleted ip range for private network id=" + networkId);
        }
        return result;
    }

    protected int getActiveNicsInNetwork(final long networkId) {
        return networksDao.getActiveNicsIn(networkId);
    }
}