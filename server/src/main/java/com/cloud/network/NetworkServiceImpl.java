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

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.security.InvalidParameterException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.cloudstack.acl.ControlledEntity.ACLType;
import org.apache.cloudstack.acl.SecurityChecker.AccessType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.command.admin.address.ReleasePodIpCmdByAdmin;
import org.apache.cloudstack.api.command.admin.network.CreateNetworkCmdByAdmin;
import org.apache.cloudstack.api.command.admin.network.DedicateGuestVlanRangeCmd;
import org.apache.cloudstack.api.command.admin.network.ListDedicatedGuestVlanRangesCmd;
import org.apache.cloudstack.api.command.admin.network.UpdateNetworkCmdByAdmin;
import org.apache.cloudstack.api.command.admin.usage.ListTrafficTypeImplementorsCmd;
import org.apache.cloudstack.api.command.user.network.CreateNetworkCmd;
import org.apache.cloudstack.api.command.user.network.ListNetworksCmd;
import org.apache.cloudstack.api.command.user.network.RestartNetworkCmd;
import org.apache.cloudstack.api.command.user.network.UpdateNetworkCmd;
import org.apache.cloudstack.api.command.user.vm.ListNicsCmd;
import org.apache.cloudstack.api.response.AcquirePodIpCmdResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.engine.orchestration.service.NetworkOrchestrationService;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.framework.messagebus.MessageBus;
import org.apache.cloudstack.framework.messagebus.PublishScope;
import org.apache.cloudstack.network.element.InternalLoadBalancerElementService;
import org.apache.log4j.Logger;

import com.cloud.api.ApiDBUtils;
import com.cloud.configuration.Config;
import com.cloud.configuration.ConfigurationManager;
import com.cloud.configuration.Resource;
import com.cloud.dc.DataCenter;
import com.cloud.dc.DataCenter.NetworkType;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.DataCenterVnetVO;
import com.cloud.dc.Vlan.VlanType;
import com.cloud.dc.VlanVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.DataCenterVnetDao;
import com.cloud.dc.dao.VlanDao;
import com.cloud.deploy.DeployDestination;
import com.cloud.domain.Domain;
import com.cloud.domain.DomainVO;
import com.cloud.domain.dao.DomainDao;
import com.cloud.event.ActionEvent;
import com.cloud.event.EventTypes;
import com.cloud.event.UsageEventUtils;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.exception.UnsupportedServiceException;
import com.cloud.host.dao.HostDao;
import com.cloud.network.IpAddress.State;
import com.cloud.network.Network.Capability;
import com.cloud.network.Network.GuestType;
import com.cloud.network.Network.IpAddresses;
import com.cloud.network.Network.Provider;
import com.cloud.network.Network.Service;
import com.cloud.network.Networks.BroadcastDomainType;
import com.cloud.network.Networks.TrafficType;
import com.cloud.network.PhysicalNetwork.BroadcastDomainRange;
import com.cloud.network.VirtualRouterProvider.Type;
import com.cloud.network.addr.PublicIp;
import com.cloud.network.dao.AccountGuestVlanMapDao;
import com.cloud.network.dao.AccountGuestVlanMapVO;
import com.cloud.network.dao.FirewallRulesDao;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.IPAddressVO;
import com.cloud.network.dao.LoadBalancerDao;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkDetailVO;
import com.cloud.network.dao.NetworkDetailsDao;
import com.cloud.network.dao.NetworkDomainDao;
import com.cloud.network.dao.NetworkDomainVO;
import com.cloud.network.dao.NetworkServiceMapDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.dao.OvsProviderDao;
import com.cloud.network.dao.PhysicalNetworkDao;
import com.cloud.network.dao.PhysicalNetworkServiceProviderDao;
import com.cloud.network.dao.PhysicalNetworkServiceProviderVO;
import com.cloud.network.dao.PhysicalNetworkTrafficTypeDao;
import com.cloud.network.dao.PhysicalNetworkTrafficTypeVO;
import com.cloud.network.dao.PhysicalNetworkVO;
import com.cloud.network.element.NetworkElement;
import com.cloud.network.element.OvsProviderVO;
import com.cloud.network.element.VirtualRouterElement;
import com.cloud.network.element.VpcVirtualRouterElement;
import com.cloud.network.guru.NetworkGuru;
import com.cloud.network.rules.FirewallRule.Purpose;
import com.cloud.network.rules.FirewallRuleVO;
import com.cloud.network.rules.RulesManager;
import com.cloud.network.rules.dao.PortForwardingRulesDao;
import com.cloud.network.security.SecurityGroupService;
import com.cloud.network.vpc.NetworkACL;
import com.cloud.network.vpc.PrivateIpVO;
import com.cloud.network.vpc.Vpc;
import com.cloud.network.vpc.VpcManager;
import com.cloud.network.vpc.dao.NetworkACLDao;
import com.cloud.network.vpc.dao.PrivateIpDao;
import com.cloud.network.vpc.dao.VpcDao;
import com.cloud.network.vpc.dao.VpcOfferingDao;
import com.cloud.offering.NetworkOffering;
import com.cloud.offerings.NetworkOfferingVO;
import com.cloud.offerings.dao.NetworkOfferingDao;
import com.cloud.offerings.dao.NetworkOfferingServiceMapDao;
import com.cloud.org.Grouping;
import com.cloud.projects.Project;
import com.cloud.projects.ProjectManager;
import com.cloud.server.ResourceTag;
import com.cloud.server.ResourceTag.ResourceObjectType;
import com.cloud.tags.ResourceTagVO;
import com.cloud.tags.dao.ResourceTagDao;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.AccountService;
import com.cloud.user.AccountVO;
import com.cloud.user.DomainManager;
import com.cloud.user.ResourceLimitService;
import com.cloud.user.User;
import com.cloud.user.UserVO;
import com.cloud.user.dao.AccountDao;
import com.cloud.user.dao.UserDao;
import com.cloud.utils.Journal;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.Pair;
import com.cloud.utils.StringUtils;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.EntityManager;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.JoinBuilder;
import com.cloud.utils.db.QueryBuilder;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Op;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallback;
import com.cloud.utils.db.TransactionCallbackNoReturn;
import com.cloud.utils.db.TransactionCallbackWithException;
import com.cloud.utils.db.TransactionLegacy;
import com.cloud.utils.db.TransactionStatus;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.exception.ExceptionUtil;
import com.cloud.utils.net.NetUtils;
import com.cloud.vm.Nic;
import com.cloud.vm.NicSecondaryIp;
import com.cloud.vm.NicVO;
import com.cloud.vm.ReservationContext;
import com.cloud.vm.ReservationContextImpl;
import com.cloud.vm.SecondaryStorageVmVO;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.dao.NicDao;
import com.cloud.vm.dao.NicSecondaryIpDao;
import com.cloud.vm.dao.NicSecondaryIpVO;
import com.cloud.vm.dao.UserVmDao;
import com.cloud.vm.dao.VMInstanceDao;

/**
 * NetworkServiceImpl implements NetworkService.
 */
public class NetworkServiceImpl extends ManagerBase implements NetworkService {
    private static final Logger s_logger = Logger.getLogger(NetworkServiceImpl.class);

    private static final long MIN_VLAN_ID = 0L;
    private static final long MAX_VLAN_ID = 4095L; // 2^12 - 1
    private static final long MIN_GRE_KEY = 0L;
    private static final long MAX_GRE_KEY = 4294967295L; // 2^32 -1
    private static final long MIN_VXLAN_VNI = 0L;
    private static final long MAX_VXLAN_VNI = 16777214L; // 2^24 -2
    // MAX_VXLAN_VNI should be 16777215L (2^24-1), but Linux vxlan interface doesn't accept VNI:2^24-1 now.
    // It seems a bug.

    @Inject
    DataCenterDao _dcDao = null;
    @Inject
    VlanDao _vlanDao = null;
    @Inject
    IPAddressDao _ipAddressDao = null;
    @Inject
    AccountDao _accountDao = null;
    @Inject
    DomainDao _domainDao = null;
    @Inject
    UserDao _userDao = null;
    @Inject
    ConfigurationDao _configDao;
    @Inject
    UserVmDao _userVmDao = null;
    @Inject
    AccountManager _accountMgr;
    @Inject
    ConfigurationManager _configMgr;
    @Inject
    NetworkOfferingDao _networkOfferingDao = null;
    @Inject
    NetworkDao _networksDao = null;
    @Inject
    NicDao _nicDao = null;
    @Inject
    RulesManager _rulesMgr;
    List<NetworkGuru> _networkGurus;
    @Inject
    NetworkDomainDao _networkDomainDao;
    @Inject
    VMInstanceDao _vmDao;
    @Inject
    FirewallRulesDao _firewallDao;
    @Inject
    ResourceLimitService _resourceLimitMgr;
    @Inject
    DomainManager _domainMgr;
    @Inject
    ProjectManager _projectMgr;
    @Inject
    NetworkOfferingServiceMapDao _ntwkOfferingSrvcDao;
    @Inject
    PhysicalNetworkDao _physicalNetworkDao;
    @Inject
    PhysicalNetworkServiceProviderDao _pNSPDao;
    @Inject
    PhysicalNetworkTrafficTypeDao _pNTrafficTypeDao;
    @Inject
    NetworkServiceMapDao _ntwkSrvcDao;
    @Inject
    StorageNetworkManager _stnwMgr;
    @Inject
    VpcManager _vpcMgr;
    @Inject
    PrivateIpDao _privateIpDao;
    @Inject
    ResourceTagDao _resourceTagDao;
    @Inject
    NetworkOrchestrationService _networkMgr;
    @Inject
    NetworkModel _networkModel;
    @Inject
    NicSecondaryIpDao _nicSecondaryIpDao;
    @Inject
    PortForwardingRulesDao _portForwardingDao;
    @Inject
    HostDao _hostDao;
    @Inject
    InternalLoadBalancerElementService _internalLbElementSvc;
    @Inject
    DataCenterVnetDao _datacneterVnet;
    @Inject
    AccountGuestVlanMapDao _accountGuestVlanMapDao;
    @Inject
    VpcDao _vpcDao;
    @Inject
    NetworkACLDao _networkACLDao;
    @Inject
    OvsProviderDao _ovsProviderDao;
    @Inject
    IpAddressManager _ipAddrMgr;
    @Inject
    Ipv6AddressManager ipv6AddrMgr;
    @Inject
    EntityManager _entityMgr;
    @Inject
    public SecurityGroupService _securityGroupService;
    @Inject
    MessageBus _messageBus;
    @Inject
    NetworkDetailsDao _networkDetailsDao;
    @Inject
    LoadBalancerDao _loadBalancerDao;
    @Inject
    NetworkMigrationManager _networkMigrationManager;
    @Inject
    VpcOfferingDao _vpcOfferingDao;
    @Inject
    AccountService _accountService;

    int _cidrLimit;
    boolean _allowSubdomainNetworkAccess;

    private Map<String, String> _configs;

    /* Get a list of IPs, classify them by service */
    protected Map<PublicIp, Set<Service>> getIpToServices(List<PublicIp> publicIps, boolean rulesRevoked, boolean includingFirewall) {
        Map<PublicIp, Set<Service>> ipToServices = new HashMap<PublicIp, Set<Service>>();

        if (publicIps != null && !publicIps.isEmpty()) {
            Set<Long> networkSNAT = new HashSet<Long>();
            for (PublicIp ip : publicIps) {
                Set<Service> services = ipToServices.get(ip);
                if (services == null) {
                    services = new HashSet<Service>();
                }
                if (ip.isSourceNat()) {
                    if (!networkSNAT.contains(ip.getAssociatedWithNetworkId())) {
                        services.add(Service.SourceNat);
                        networkSNAT.add(ip.getAssociatedWithNetworkId());
                    } else {
                        CloudRuntimeException ex = new CloudRuntimeException("Multiple generic source NAT IPs provided for network");
                        // see the IPAddressVO.java class.
                        IPAddressVO ipAddr = ApiDBUtils.findIpAddressById(ip.getAssociatedWithNetworkId());
                        String ipAddrUuid = ip.getAssociatedWithNetworkId().toString();
                        if (ipAddr != null) {
                            ipAddrUuid = ipAddr.getUuid();
                        }
                        ex.addProxyObject(ipAddrUuid, "networkId");
                        throw ex;
                    }
                }
                ipToServices.put(ip, services);

                // if IP in allocating state then it will not have any rules attached so skip IPAssoc to network service
                // provider
                if (ip.getState() == State.Allocating) {
                    continue;
                }

                // check if any active rules are applied on the public IP
                Set<Purpose> purposes = getPublicIpPurposeInRules(ip, false, includingFirewall);
                // Firewall rules didn't cover static NAT
                if (ip.isOneToOneNat() && ip.getAssociatedWithVmId() != null) {
                    if (purposes == null) {
                        purposes = new HashSet<Purpose>();
                    }
                    purposes.add(Purpose.StaticNat);
                }
                if (purposes == null || purposes.isEmpty()) {
                    // since no active rules are there check if any rules are applied on the public IP but are in
// revoking state

                    purposes = getPublicIpPurposeInRules(ip, true, includingFirewall);
                    if (ip.isOneToOneNat()) {
                        if (purposes == null) {
                            purposes = new HashSet<Purpose>();
                        }
                        purposes.add(Purpose.StaticNat);
                    }
                    if (purposes == null || purposes.isEmpty()) {
                        // IP is not being used for any purpose so skip IPAssoc to network service provider
                        continue;
                    } else {
                        if (rulesRevoked) {
                            // no active rules/revoked rules are associated with this public IP, so remove the
// association with the provider
                            ip.setState(State.Releasing);
                        } else {
                            if (ip.getState() == State.Releasing) {
                                // rules are not revoked yet, so don't let the network service provider revoke the IP
// association
                                // mark IP is allocated so that IP association will not be removed from the provider
                                ip.setState(State.Allocated);
                            }
                        }
                    }
                }
                if (purposes.contains(Purpose.StaticNat)) {
                    services.add(Service.StaticNat);
                }
                if (purposes.contains(Purpose.LoadBalancing)) {
                    services.add(Service.Lb);
                }
                if (purposes.contains(Purpose.PortForwarding)) {
                    services.add(Service.PortForwarding);
                }
                if (purposes.contains(Purpose.Vpn)) {
                    services.add(Service.Vpn);
                }
                if (purposes.contains(Purpose.Firewall)) {
                    services.add(Service.Firewall);
                }
                if (services.isEmpty()) {
                    continue;
                }
                ipToServices.put(ip, services);
            }
        }
        return ipToServices;
    }

    protected boolean canIpUsedForNonConserveService(PublicIp ip, Service service) {
        // If it's non-conserve mode, then the new ip should not be used by any other services
        List<PublicIp> ipList = new ArrayList<PublicIp>();
        ipList.add(ip);
        Map<PublicIp, Set<Service>> ipToServices = getIpToServices(ipList, false, false);
        Set<Service> services = ipToServices.get(ip);
        // Not used currently, safe
        if (services == null || services.isEmpty()) {
            return true;
        }
        // Since it's non-conserve mode, only one service should used for IP
        if (services.size() != 1) {
            throw new InvalidParameterException("There are multiple services used ip " + ip.getAddress() + ".");
        }
        if (service != null && !((Service)services.toArray()[0] == service || service.equals(Service.Firewall))) {
            throw new InvalidParameterException("The IP " + ip.getAddress() + " is already used as " + ((Service)services.toArray()[0]).getName() + " rather than " + service.getName());
        }
        return true;
    }

    protected boolean canIpsUsedForNonConserve(List<PublicIp> publicIps) {
        boolean result = true;
        for (PublicIp ip : publicIps) {
            result = canIpUsedForNonConserveService(ip, null);
            if (!result) {
                break;
            }
        }
        return result;
    }

    private boolean canIpsUseOffering(List<PublicIp> publicIps, long offeringId) {
        Map<PublicIp, Set<Service>> ipToServices = getIpToServices(publicIps, false, true);
        Map<Service, Set<Provider>> serviceToProviders = _networkModel.getNetworkOfferingServiceProvidersMap(offeringId);
        NetworkOfferingVO offering = _networkOfferingDao.findById(offeringId);
        //For inline mode checking, using firewall provider for LB instead, because public ip would apply on firewall provider
        if (offering.isInline()) {
            Provider firewallProvider = null;
            if (serviceToProviders.containsKey(Service.Firewall)) {
                firewallProvider = (Provider)serviceToProviders.get(Service.Firewall).toArray()[0];
            }
            Set<Provider> p = new HashSet<Provider>();
            p.add(firewallProvider);
            serviceToProviders.remove(Service.Lb);
            serviceToProviders.put(Service.Lb, p);
        }
        for (PublicIp ip : ipToServices.keySet()) {
            Set<Service> services = ipToServices.get(ip);
            Provider provider = null;
            for (Service service : services) {
                Set<Provider> curProviders = serviceToProviders.get(service);
                if (curProviders == null || curProviders.isEmpty()) {
                    continue;
                }
                Provider curProvider = (Provider)curProviders.toArray()[0];
                if (provider == null) {
                    provider = curProvider;
                    continue;
                }
                // We don't support multiple providers for one service now
                if (!provider.equals(curProvider)) {
                    throw new InvalidParameterException("There would be multiple providers for IP " + ip.getAddress() + " with the new network offering!");
                }
            }
        }
        return true;
    }

    private Set<Purpose> getPublicIpPurposeInRules(PublicIp ip, boolean includeRevoked, boolean includingFirewall) {
        Set<Purpose> result = new HashSet<Purpose>();
        List<FirewallRuleVO> rules = null;
        if (includeRevoked) {
            rules = _firewallDao.listByIp(ip.getId());
        } else {
            rules = _firewallDao.listByIpAndNotRevoked(ip.getId());
        }

        if (rules == null || rules.isEmpty()) {
            return null;
        }

        for (FirewallRuleVO rule : rules) {
            if (rule.getPurpose() != Purpose.Firewall || includingFirewall) {
                result.add(rule.getPurpose());
            }
        }

        return result;
    }

    @Override
    public List<? extends Network> getIsolatedNetworksOwnedByAccountInZone(long zoneId, Account owner) {

        return _networksDao.listByZoneAndGuestType(owner.getId(), zoneId, Network.GuestType.Isolated, false);
    }

    @Override
    public List<? extends Network> getIsolatedNetworksWithSourceNATOwnedByAccountInZone(long zoneId, Account owner) {

        return _networksDao.listSourceNATEnabledNetworks(owner.getId(), zoneId, Network.GuestType.Isolated);
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_NET_IP_ASSIGN, eventDescription = "allocating Ip", create = true)
    public IpAddress allocateIP(Account ipOwner, long zoneId, Long networkId, Boolean displayIp)
            throws ResourceAllocationException, InsufficientAddressCapacityException, ConcurrentOperationException {

        Account caller = CallContext.current().getCallingAccount();
        long callerUserId = CallContext.current().getCallingUserId();
        DataCenter zone = _entityMgr.findById(DataCenter.class, zoneId);

        if (networkId != null) {
            Network network = _networksDao.findById(networkId);
            if (network == null) {
                throw new InvalidParameterValueException("Invalid network id is given");
            }

            if (network.getGuestType() == Network.GuestType.Shared) {
                if (zone == null) {
                    throw new InvalidParameterValueException("Invalid zone Id is given");
                }
                // if shared network in the advanced zone, then check the caller against the network for 'AccessType.UseNetwork'
                if (zone.getNetworkType() == NetworkType.Advanced) {
                    if (isSharedNetworkOfferingWithServices(network.getNetworkOfferingId())) {
                        _accountMgr.checkAccess(caller, AccessType.UseEntry, false, network);
                        if (s_logger.isDebugEnabled()) {
                            s_logger.debug("Associate IP address called by the user " + callerUserId + " account " + ipOwner.getId());
                        }
                        return _ipAddrMgr.allocateIp(ipOwner, false, caller, callerUserId, zone, displayIp);
                    } else {
                        throw new InvalidParameterValueException("Associate IP address can only be called on the shared networks in the advanced zone"
                                + " with Firewall/Source Nat/Static Nat/Port Forwarding/Load balancing services enabled");
                    }
                }
            }
        } else {
            _accountMgr.checkAccess(caller, null, false, ipOwner);
        }

        return _ipAddrMgr.allocateIp(ipOwner, false, caller, callerUserId, zone, displayIp);
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_PORTABLE_IP_ASSIGN, eventDescription = "allocating portable public Ip", create = true)
    public IpAddress allocatePortableIP(Account ipOwner, int regionId, Long zoneId, Long networkId, Long vpcId)
            throws ResourceAllocationException, InsufficientAddressCapacityException, ConcurrentOperationException {
        Account caller = CallContext.current().getCallingAccount();
        long callerUserId = CallContext.current().getCallingUserId();
        DataCenter zone = _entityMgr.findById(DataCenter.class, zoneId);

        if ((networkId == null && vpcId == null) || (networkId != null && vpcId != null)) {
            throw new InvalidParameterValueException("One of Network id or VPC is should be passed");
        }

        if (networkId != null) {
            Network network = _networksDao.findById(networkId);
            if (network == null) {
                throw new InvalidParameterValueException("Invalid network id is given");
            }

            if (network.getGuestType() == Network.GuestType.Shared) {
                if (zone == null) {
                    throw new InvalidParameterValueException("Invalid zone Id is given");
                }
                // if shared network in the advanced zone, then check the caller against the network for 'AccessType.UseNetwork'
                if (zone.getNetworkType() == NetworkType.Advanced) {
                    if (isSharedNetworkOfferingWithServices(network.getNetworkOfferingId())) {
                        _accountMgr.checkAccess(caller, AccessType.UseEntry, false, network);
                        if (s_logger.isDebugEnabled()) {
                            s_logger.debug("Associate IP address called by the user " + callerUserId + " account " + ipOwner.getId());
                        }
                        return _ipAddrMgr.allocatePortableIp(ipOwner, caller, zoneId, networkId, null);
                    } else {
                        throw new InvalidParameterValueException("Associate IP address can only be called on the shared networks in the advanced zone"
                                + " with Firewall/Source Nat/Static Nat/Port Forwarding/Load balancing services enabled");
                    }
                }
            }
        }

        if (vpcId != null) {
            Vpc vpc = _vpcDao.findById(vpcId);
            if (vpc == null) {
                throw new InvalidParameterValueException("Invalid vpc id is given");
            }
        }

        _accountMgr.checkAccess(caller, null, false, ipOwner);

        return _ipAddrMgr.allocatePortableIp(ipOwner, caller, zoneId, null, null);
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_PORTABLE_IP_RELEASE, eventDescription = "disassociating portable Ip", async = true)
    public boolean releasePortableIpAddress(long ipAddressId) {
        try {
            return releaseIpAddressInternal(ipAddressId);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    @DB
    public boolean configure(final String name, final Map<String, Object> params) throws ConfigurationException {
        _configs = _configDao.getConfiguration("Network", params);

        _cidrLimit = NumbersUtil.parseInt(_configs.get(Config.NetworkGuestCidrLimit.key()), 22);

        _allowSubdomainNetworkAccess = Boolean.valueOf(_configs.get(Config.SubDomainNetworkAccess.key()));

        s_logger.info("Network Service is configured.");

        return true;
    }

    @Override
    public boolean start() {
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    protected NetworkServiceImpl() {
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_NIC_SECONDARY_IP_CONFIGURE, eventDescription = "Configuring secondary ip " + "rules", async = true)
    public boolean configureNicSecondaryIp(NicSecondaryIp secIp, boolean isZoneSgEnabled) {
        boolean success = false;
        String secondaryIp = secIp.getIp4Address();
        if (secIp.getIp4Address() == null) {
            secondaryIp = secIp.getIp6Address();
        }

        if (isZoneSgEnabled) {
            success = _securityGroupService.securityGroupRulesForVmSecIp(secIp.getNicId(), secondaryIp, true);
            s_logger.info("Associated ip address to NIC : " + secIp.getIp4Address());
        } else {
            success = true;
        }
        return success;
    }

    /**
     * It allocates a secondary IP alias on the NIC. It can be either an Ipv4 or an Ipv6 or even both, according to the the given IpAddresses object.
     */
    @Override
    @ActionEvent(eventType = EventTypes.EVENT_NIC_SECONDARY_IP_ASSIGN, eventDescription = "assigning secondary ip to nic", create = true)
    public NicSecondaryIp allocateSecondaryGuestIP(final long nicId, IpAddresses requestedIpPair) throws InsufficientAddressCapacityException {

        Account caller = CallContext.current().getCallingAccount();
        String ipv4Address = requestedIpPair.getIp4Address();
        String ipv6Address = requestedIpPair.getIp6Address();

        //check whether the nic belongs to user vm.
        NicVO nicVO = _nicDao.findById(nicId);
        if (nicVO == null) {
            throw new InvalidParameterValueException("There is no NIC with the ID:  " + nicId);
        }

        if (nicVO.getVmType() != VirtualMachine.Type.User) {
            throw new InvalidParameterValueException(String.format("The NIC [%s] does not belong to a user VM", nicVO.getUuid()));
        }

        VirtualMachine vm = _userVmDao.findById(nicVO.getInstanceId());
        if (vm == null) {
            throw new InvalidParameterValueException(String.format("There is no VM with the NIC [%s]", nicVO.getUuid()));
        }

        final long networkId = nicVO.getNetworkId();
        final Account ipOwner = _accountMgr.getAccount(vm.getAccountId());

        // verify permissions
        _accountMgr.checkAccess(caller, null, true, vm);

        Network network = _networksDao.findById(networkId);
        if (network == null) {
            throw new InvalidParameterValueException("Invalid network id is given");
        }

        int maxAllowedIpsPerNic = NumbersUtil.parseInt(_configDao.getValue(Config.MaxNumberOfSecondaryIPsPerNIC.key()), 10);
        Long nicWiseIpCount = _nicSecondaryIpDao.countByNicId(nicId);
        if (nicWiseIpCount.intValue() >= maxAllowedIpsPerNic) {
            s_logger.error("Maximum Number of Ips \"vm.network.nic.max.secondary.ipaddresses = \"" + maxAllowedIpsPerNic + " per Nic has been crossed for the nic " + nicId + ".");
            throw new InsufficientAddressCapacityException("Maximum Number of Ips per Nic has been crossed.", Nic.class, nicId);
        }

        s_logger.debug("Calling the ip allocation ...");
        String ipaddr = null;
        String ip6addr = null;
        //Isolated network can exist in Basic zone only, so no need to verify the zone type
        if (network.getGuestType() == Network.GuestType.Isolated) {
            if ((ipv4Address != null || NetUtils.isIpv4(network.getGateway()) && org.apache.commons.lang3.StringUtils.isBlank(ipv6Address))) {
                ipaddr = _ipAddrMgr.allocateGuestIP(network, ipv4Address);
            }
            if (ipv6Address != null) {
                ip6addr = ipv6AddrMgr.allocateGuestIpv6(network, ipv6Address);
            }
        } else if (network.getGuestType() == Network.GuestType.Shared) {
            //for basic zone, need to provide the podId to ensure proper ip alloation
            Long podId = null;
            DataCenter dc = _dcDao.findById(network.getDataCenterId());

            if (dc.getNetworkType() == NetworkType.Basic) {
                VMInstanceVO vmi = (VMInstanceVO)vm;
                podId = vmi.getPodIdToDeployIn();
                if (podId == null) {
                    throw new InvalidParameterValueException("vm pod id is null in Basic zone; can't decide the range for ip allocation");
                }
            }

            try {
                if (ipv6Address != null) {
                    ip6addr = ipv6AddrMgr.allocatePublicIp6ForGuestNic(network, podId, ipOwner, ipv6Address);
                } else {
                    ipaddr = _ipAddrMgr.allocatePublicIpForGuestNic(network, podId, ipOwner, ipv4Address);
                }
                if (ipaddr == null && ipv6Address == null) {
                    throw new InvalidParameterValueException("Allocating ip to guest nic " + nicId + " failed");
                }
            } catch (InsufficientAddressCapacityException e) {
                s_logger.error("Allocating ip to guest nic " + nicId + " failed");
                return null;
            }
        } else {
            s_logger.error("AddIpToVMNic is not supported in this network...");
            return null;
        }

        if (ipaddr != null || ip6addr != null) {
            // we got the ip addr so up the nics table and secodary ip
            final String ip4AddrFinal = ipaddr;
            final String ip6AddrFinal = ip6addr;
            long id = Transaction.execute(new TransactionCallback<Long>() {
                @Override
                public Long doInTransaction(TransactionStatus status) {
                    boolean nicSecondaryIpSet = nicVO.getSecondaryIp();
                    if (!nicSecondaryIpSet) {
                        nicVO.setSecondaryIp(true);
                        // commit when previously set ??
                        s_logger.debug("Setting nics table ...");
                        _nicDao.update(nicId, nicVO);
                    }

                    s_logger.debug("Setting nic_secondary_ip table ...");
                    Long vmId = nicVO.getInstanceId();
                    NicSecondaryIpVO secondaryIpVO = new NicSecondaryIpVO(nicId, ip4AddrFinal, ip6AddrFinal, vmId, ipOwner.getId(), ipOwner.getDomainId(), networkId);
                    _nicSecondaryIpDao.persist(secondaryIpVO);
                    return secondaryIpVO.getId();
                }
            });

            return getNicSecondaryIp(id);
        } else {
            return null;
        }
    }

    @Override
    @DB
    @ActionEvent(eventType = EventTypes.EVENT_NIC_SECONDARY_IP_UNASSIGN, eventDescription = "Removing secondary IP from NIC", async = true)
    public boolean releaseSecondaryIpFromNic(long ipAddressId) {
        Account caller = CallContext.current().getCallingAccount();
        boolean success = false;

        // Verify input parameters
        NicSecondaryIpVO secIpVO = _nicSecondaryIpDao.findById(ipAddressId);
        if (secIpVO == null) {
            throw new InvalidParameterValueException("Unable to find secondary ip address by id");
        }

        VirtualMachine vm = _userVmDao.findById(secIpVO.getVmId());
        if (vm == null) {
            throw new InvalidParameterValueException("There is no vm with the given secondary ip");
        }
        // verify permissions
        _accountMgr.checkAccess(caller, null, true, vm);

        Network network = _networksDao.findById(secIpVO.getNetworkId());

        if (network == null) {
            throw new InvalidParameterValueException("Invalid network id is given");
        }

        // Validate network offering
        NetworkOfferingVO ntwkOff = _networkOfferingDao.findById(network.getNetworkOfferingId());

        Long nicId = secIpVO.getNicId();
        s_logger.debug("ip id = " + ipAddressId + " nic id = " + nicId);
        //check is this the last secondary ip for NIC
        List<NicSecondaryIpVO> ipList = _nicSecondaryIpDao.listByNicId(nicId);
        boolean lastIp = false;
        if (ipList.size() == 1) {
            // this is the last secondary ip to nic
            lastIp = true;
        }

        DataCenter dc = _dcDao.findById(network.getDataCenterId());
        if (dc == null) {
            throw new InvalidParameterValueException("Invalid zone Id is given");
        }

        s_logger.debug("Calling secondary ip " + secIpVO.getIp4Address() + " release ");
        if (dc.getNetworkType() == NetworkType.Advanced && network.getGuestType() == Network.GuestType.Isolated) {
            //check PF or static NAT is configured on this ip address
            String secondaryIp = secIpVO.getIp4Address();
            List<FirewallRuleVO> fwRulesList = _firewallDao.listByNetworkAndPurpose(network.getId(), Purpose.PortForwarding);

            if (fwRulesList.size() != 0) {
                for (FirewallRuleVO rule : fwRulesList) {
                    if (_portForwardingDao.findByIdAndIp(rule.getId(), secondaryIp) != null) {
                        s_logger.debug("VM nic IP " + secondaryIp + " is associated with the port forwarding rule");
                        throw new InvalidParameterValueException("Can't remove the secondary ip " + secondaryIp + " is associate with the port forwarding rule");
                    }
                }
            }
            //check if the secondary ip associated with any static nat rule
            IPAddressVO publicIpVO = _ipAddressDao.findByIpAndNetworkId(secIpVO.getNetworkId(), secondaryIp);
            if (publicIpVO != null) {
                s_logger.debug("VM nic IP " + secondaryIp + " is associated with the static NAT rule public IP address id " + publicIpVO.getId());
                throw new InvalidParameterValueException("Can' remove the ip " + secondaryIp + "is associate with static NAT rule public IP address id " + publicIpVO.getId());
            }

            if (_loadBalancerDao.isLoadBalancerRulesMappedToVmGuestIp(vm.getId(), secondaryIp, network.getId())) {
                s_logger.debug("VM nic IP " + secondaryIp + " is mapped to load balancing rule");
                throw new InvalidParameterValueException("Can't remove the secondary ip " + secondaryIp + " is mapped to load balancing rule");
            }

        } else if (dc.getNetworkType() == NetworkType.Basic || ntwkOff.getGuestType() == Network.GuestType.Shared) {
            final IPAddressVO ip = _ipAddressDao.findByIpAndSourceNetworkId(secIpVO.getNetworkId(), secIpVO.getIp4Address());
            if (ip != null) {
                Transaction.execute(new TransactionCallbackNoReturn() {
                    @Override
                    public void doInTransactionWithoutResult(TransactionStatus status) {
                        _ipAddrMgr.markIpAsUnavailable(ip.getId());
                        _ipAddressDao.unassignIpAddress(ip.getId());
                    }
                });
            }
        } else {
            throw new InvalidParameterValueException("Not supported for this network now");
        }

        success = removeNicSecondaryIP(secIpVO, lastIp);
        return success;
    }

    boolean removeNicSecondaryIP(final NicSecondaryIpVO ipVO, final boolean lastIp) {
        final long nicId = ipVO.getNicId();
        final NicVO nic = _nicDao.findById(nicId);

        Transaction.execute(new TransactionCallbackNoReturn() {
            @Override
            public void doInTransactionWithoutResult(TransactionStatus status) {
                if (lastIp) {
                    nic.setSecondaryIp(false);
                    s_logger.debug("Setting nics secondary ip to false ...");
                    _nicDao.update(nicId, nic);
                }

                s_logger.debug("Revoving nic secondary ip entry ...");
                _nicSecondaryIpDao.remove(ipVO.getId());
            }
        });

        return true;
    }

    NicSecondaryIp getNicSecondaryIp(long id) {
        NicSecondaryIp nicSecIp = _nicSecondaryIpDao.findById(id);
        if (nicSecIp == null) {
            return null;
        }
        return nicSecIp;
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_NET_IP_RELEASE, eventDescription = "disassociating Ip", async = true)
    public boolean releaseIpAddress(long ipAddressId) throws InsufficientAddressCapacityException {
        return releaseIpAddressInternal(ipAddressId);
    }

    @DB
    private boolean releaseIpAddressInternal(long ipAddressId) throws InsufficientAddressCapacityException {
        Long userId = CallContext.current().getCallingUserId();
        Account caller = CallContext.current().getCallingAccount();

        // Verify input parameters
        IPAddressVO ipVO = _ipAddressDao.findById(ipAddressId);
        if (ipVO == null) {
            throw new InvalidParameterValueException("Unable to find ip address by id");
        }

        if (ipVO.getAllocatedTime() == null) {
            s_logger.debug("Ip Address id= " + ipAddressId + " is not allocated, so do nothing.");
            return true;
        }

        // verify permissions
        if (ipVO.getAllocatedToAccountId() != null) {
            _accountMgr.checkAccess(caller, null, true, ipVO);
        }

        Network guestNetwork = null;
        final Long networkId = ipVO.getAssociatedWithNetworkId();
        if (networkId != null) {
            guestNetwork = getNetwork(networkId);
        }
        if (ipVO.isSourceNat() && guestNetwork != null && guestNetwork.getState() != Network.State.Allocated) {
            throw new IllegalArgumentException("ip address is used for source nat purposes and can not be disassociated.");
        }

        VlanVO vlan = _vlanDao.findById(ipVO.getVlanId());
        if (!vlan.getVlanType().equals(VlanType.VirtualNetwork)) {
            throw new IllegalArgumentException("only ip addresses that belong to a virtual network may be disassociated.");
        }

        // don't allow releasing system ip address
        if (ipVO.getSystem()) {
            throwInvalidIdException("Can't release system IP address with specified id", ipVO.getUuid(), "systemIpAddrId");
        }

        boolean success = _ipAddrMgr.disassociatePublicIpAddress(ipAddressId, userId, caller);

        if (success) {
            if (guestNetwork != null) {
                NetworkOffering offering = _entityMgr.findById(NetworkOffering.class, guestNetwork.getNetworkOfferingId());
                Long vmId = ipVO.getAssociatedWithVmId();
                if (offering.isElasticIp() && vmId != null) {
                    _rulesMgr.getSystemIpAndEnableStaticNatForVm(_userVmDao.findById(vmId), true);
                    return true;
                }
            }
        } else {
            s_logger.warn("Failed to release public ip address id=" + ipAddressId);
        }
        return success;
    }

    @Override
    @DB
    public Network getNetwork(long id) {
        return _networksDao.findById(id);
    }

    private void checkSharedNetworkCidrOverlap(Long zoneId, long physicalNetworkId, String cidr) {
        if (zoneId == null || cidr == null) {
            return;
        }

        DataCenter zone = _dcDao.findById(zoneId);
        List<NetworkVO> networks = _networksDao.listByZone(zoneId);
        Map<Long, String> networkToCidr = new HashMap<Long, String>();

        // check for CIDR overlap with all possible CIDR for isolated guest networks
        // in the zone when using external networking
        PhysicalNetworkVO pNetwork = _physicalNetworkDao.findById(physicalNetworkId);
        if (pNetwork.getVnet() != null) {
            List<Pair<Integer, Integer>> vlanList = pNetwork.getVnet();
            for (Pair<Integer, Integer> vlanRange : vlanList) {
                Integer lowestVlanTag = vlanRange.first();
                Integer highestVlanTag = vlanRange.second();
                for (int vlan = lowestVlanTag; vlan <= highestVlanTag; ++vlan) {
                    int offset = vlan - lowestVlanTag;
                    String globalVlanBits = _configDao.getValue(Config.GuestVlanBits.key());
                    int cidrSize = 8 + Integer.parseInt(globalVlanBits);
                    String guestNetworkCidr = zone.getGuestNetworkCidr();
                    String[] cidrTuple = guestNetworkCidr.split("\\/");
                    long newCidrAddress = (NetUtils.ip2Long(cidrTuple[0]) & 0xff000000) | (offset << (32 - cidrSize));
                    if (NetUtils.isNetworksOverlap(NetUtils.long2Ip(newCidrAddress), cidr)) {
                        throw new InvalidParameterValueException("Specified CIDR for shared network conflict with CIDR that is reserved for zone vlan " + vlan);
                    }
                }
            }
        }

        // check for CIDR overlap with all CIDR's of the shared networks in the zone
        for (NetworkVO network : networks) {
            if (network.getGuestType() == GuestType.Isolated) {
                continue;
            }
            if (network.getCidr() != null) {
                networkToCidr.put(network.getId(), network.getCidr());
            }
        }
        if (networkToCidr != null && !networkToCidr.isEmpty()) {
            for (long networkId : networkToCidr.keySet()) {
                String ntwkCidr = networkToCidr.get(networkId);
                if (NetUtils.isNetworksOverlap(ntwkCidr, cidr)) {
                    throw new InvalidParameterValueException("Specified CIDR for shared network conflict with CIDR of a shared network in the zone.");
                }
            }
        }
    }

    @Override
    @DB
    @ActionEvent(eventType = EventTypes.EVENT_NETWORK_CREATE, eventDescription = "creating network")
    public Network createGuestNetwork(CreateNetworkCmd cmd) throws InsufficientCapacityException, ConcurrentOperationException, ResourceAllocationException {
        Long networkOfferingId = cmd.getNetworkOfferingId();
        String gateway = cmd.getGateway();
        String startIP = cmd.getStartIp();
        String endIP = cmd.getEndIp();
        String netmask = cmd.getNetmask();
        String networkDomain = cmd.getNetworkDomain();
        String vlanId = null;
        boolean bypassVlanOverlapCheck = false;
        boolean hideIpAddressUsage = false;
        if (cmd instanceof CreateNetworkCmdByAdmin) {
            vlanId = ((CreateNetworkCmdByAdmin)cmd).getVlan();
            bypassVlanOverlapCheck = ((CreateNetworkCmdByAdmin)cmd).getBypassVlanOverlapCheck();
            hideIpAddressUsage = ((CreateNetworkCmdByAdmin)cmd).getHideIpAddressUsage();
        }

        String name = cmd.getNetworkName();
        String displayText = cmd.getDisplayText();
        Account caller = CallContext.current().getCallingAccount();
        Long physicalNetworkId = cmd.getPhysicalNetworkId();
        Long zoneId = cmd.getZoneId();
        String aclTypeStr = cmd.getAclType();
        Long domainId = cmd.getDomainId();
        boolean isDomainSpecific = false;
        Boolean subdomainAccess = cmd.getSubdomainAccess();
        Long vpcId = cmd.getVpcId();
        String startIPv6 = cmd.getStartIpv6();
        String endIPv6 = cmd.getEndIpv6();
        String ip6Gateway = cmd.getIp6Gateway();
        String ip6Cidr = cmd.getIp6Cidr();
        Boolean displayNetwork = cmd.getDisplayNetwork();
        Long aclId = cmd.getAclId();
        String isolatedPvlan = cmd.getIsolatedPvlan();
        String externalId = cmd.getExternalId();

        // Validate network offering
        NetworkOfferingVO ntwkOff = _networkOfferingDao.findById(networkOfferingId);
        if (ntwkOff == null || ntwkOff.isSystemOnly()) {
            InvalidParameterValueException ex = new InvalidParameterValueException("Unable to find network offering by specified id");
            if (ntwkOff != null) {
                ex.addProxyObject(ntwkOff.getUuid(), "networkOfferingId");
            }
            throw ex;
        }

        Account owner = null;
        if ((cmd.getAccountName() != null && domainId != null) || cmd.getProjectId() != null) {
            owner = _accountMgr.finalizeOwner(caller, cmd.getAccountName(), domainId, cmd.getProjectId());
        } else {
            owner = caller;
        }

        // validate physical network and zone
        // Check if physical network exists
        PhysicalNetwork pNtwk = null;
        if (physicalNetworkId != null) {
            pNtwk = _physicalNetworkDao.findById(physicalNetworkId);
            if (pNtwk == null) {
                throw new InvalidParameterValueException("Unable to find a physical network having the specified physical network id");
            }
        }

        if (zoneId == null) {
            zoneId = pNtwk.getDataCenterId();
        }

        if (displayNetwork == null) {
            displayNetwork = true;
        }

        DataCenter zone = _dcDao.findById(zoneId);
        if (zone == null) {
            throw new InvalidParameterValueException("Specified zone id was not found");
        }

        _accountMgr.checkAccess(owner, ntwkOff, zone);

        if (Grouping.AllocationState.Disabled == zone.getAllocationState() && !_accountMgr.isRootAdmin(caller.getId())) {
            // See DataCenterVO.java
            PermissionDeniedException ex = new PermissionDeniedException("Cannot perform this operation since specified Zone is currently disabled");
            ex.addProxyObject(zone.getUuid(), "zoneId");
            throw ex;
        }

        // Only domain and account ACL types are supported in Acton.
        ACLType aclType = null;
        if (aclTypeStr != null) {
            if (aclTypeStr.equalsIgnoreCase(ACLType.Account.toString())) {
                aclType = ACLType.Account;
            } else if (aclTypeStr.equalsIgnoreCase(ACLType.Domain.toString())) {
                aclType = ACLType.Domain;
            } else {
                throw new InvalidParameterValueException("Incorrect aclType specified. Check the API documentation for supported types");
            }
            // In 3.0 all Shared networks should have aclType == Domain, all Isolated networks aclType==Account
            if (ntwkOff.getGuestType() == GuestType.Isolated) {
                if (aclType != ACLType.Account) {
                    throw new InvalidParameterValueException("AclType should be " + ACLType.Account + " for network of type " + Network.GuestType.Isolated);
                }
            } else if (ntwkOff.getGuestType() == GuestType.Shared) {
                if (!(aclType == ACLType.Domain || aclType == ACLType.Account)) {
                    throw new InvalidParameterValueException("AclType should be " + ACLType.Domain + " or " + ACLType.Account + " for network of type " + Network.GuestType.Shared);
                }
            }
        } else {
            if (ntwkOff.getGuestType() == GuestType.Isolated || ntwkOff.getGuestType() == GuestType.L2) {
                aclType = ACLType.Account;
            } else if (ntwkOff.getGuestType() == GuestType.Shared) {
                aclType = ACLType.Domain;
            }
        }

        // Only Admin can create Shared networks
        if ((ntwkOff.getGuestType() == GuestType.Shared) && !_accountMgr.isAdmin(caller.getId())) {
            throw new InvalidParameterValueException("Only Admins can create network with guest type " + GuestType.Shared);
        }

        // Check if the network is domain specific
        if (aclType == ACLType.Domain) {
            // only Admin can create domain with aclType=Domain
            if (!_accountMgr.isAdmin(caller.getId())) {
                throw new PermissionDeniedException("Only admin can create networks with aclType=Domain");
            }

            // only shared networks can be Domain specific
            if (ntwkOff.getGuestType() != GuestType.Shared) {
                throw new InvalidParameterValueException("Only " + GuestType.Shared + " networks can have aclType=" + ACLType.Domain);
            }

            if (domainId != null) {
                if (ntwkOff.getTrafficType() != TrafficType.Guest || ntwkOff.getGuestType() != Network.GuestType.Shared) {
                    throw new InvalidParameterValueException("Domain level networks are supported just for traffic type " + TrafficType.Guest + " and guest type " + Network.GuestType.Shared);
                }

                DomainVO domain = _domainDao.findById(domainId);
                if (domain == null) {
                    throw new InvalidParameterValueException("Unable to find domain by specified id");
                }
                _accountMgr.checkAccess(caller, domain);
            }
            isDomainSpecific = true;

        } else if (subdomainAccess != null) {
            throw new InvalidParameterValueException("Parameter subDomainAccess can be specified only with aclType=Domain");
        }

        boolean ipv4 = true, ipv6 = false;
        if (startIP != null) {
            ipv4 = true;
        }
        if (startIPv6 != null) {
            ipv6 = true;
        }

        if (gateway != null) {
            try {
                // getByName on a literal representation will only check validity of the address
                // http://docs.oracle.com/javase/6/docs/api/java/net/InetAddress.html#getByName(java.lang.String)
                InetAddress gatewayAddress = InetAddress.getByName(gateway);
                if (gatewayAddress instanceof Inet6Address) {
                    ipv6 = true;
                } else {
                    ipv4 = true;
                }
            } catch (UnknownHostException e) {
                s_logger.error("Unable to convert gateway IP to a InetAddress", e);
                throw new InvalidParameterValueException("Gateway parameter is invalid");
            }
        }

        String cidr = null;
        if (ipv4) {
            // if end ip is not specified, default it to startIp
            if (startIP != null) {
                if (!NetUtils.isValidIp4(startIP)) {
                    throw new InvalidParameterValueException("Invalid format for the startIp parameter");
                }
                if (endIP == null) {
                    endIP = startIP;
                } else if (!NetUtils.isValidIp4(endIP)) {
                    throw new InvalidParameterValueException("Invalid format for the endIp parameter");
                }
            }

            if (startIP != null && endIP != null) {
                if (!(gateway != null && netmask != null)) {
                    throw new InvalidParameterValueException("gateway and netmask should be defined when startIP/endIP are passed in");
                }
            }

            if (gateway != null && netmask != null) {
                if (NetUtils.isNetworkorBroadcastIP(gateway, netmask)) {
                    if (s_logger.isDebugEnabled()) {
                        s_logger.debug("The gateway IP provided is " + gateway + " and netmask is " + netmask + ". The IP is either broadcast or network IP.");
                    }
                    throw new InvalidParameterValueException("Invalid gateway IP provided. Either the IP is broadcast or network IP.");
                }

                if (!NetUtils.isValidIp4(gateway)) {
                    throw new InvalidParameterValueException("Invalid gateway");
                }
                if (!NetUtils.isValidIp4Netmask(netmask)) {
                    throw new InvalidParameterValueException("Invalid netmask");
                }

                cidr = NetUtils.ipAndNetMaskToCidr(gateway, netmask);
            }

        }

        if (ipv6) {
            if (endIPv6 == null) {
                endIPv6 = startIPv6;
            }
            _networkModel.checkIp6Parameters(startIPv6, endIPv6, ip6Gateway, ip6Cidr);

            if (zone.getNetworkType() != NetworkType.Advanced || ntwkOff.getGuestType() != Network.GuestType.Shared) {
                throw new InvalidParameterValueException("Can only support create IPv6 network with advance shared network!");
            }
        }

        if (isolatedPvlan != null && (zone.getNetworkType() != NetworkType.Advanced || ntwkOff.getGuestType() != Network.GuestType.Shared)) {
            throw new InvalidParameterValueException("Can only support create Private VLAN network with advance shared network!");
        }

        if (isolatedPvlan != null && ipv6) {
            throw new InvalidParameterValueException("Can only support create Private VLAN network with IPv4!");
        }

        // Regular user can create Guest Isolated Source Nat enabled network only
        if (_accountMgr.isNormalUser(caller.getId()) && (ntwkOff.getTrafficType() != TrafficType.Guest
                || ntwkOff.getGuestType() != Network.GuestType.Isolated && areServicesSupportedByNetworkOffering(ntwkOff.getId(), Service.SourceNat))) {
            throw new InvalidParameterValueException(
                    String.format("Regular users can only create a network from network offerings having traffic type [%s] and network type [%s] with a service [%s] enabled.", TrafficType.Guest,
                            Network.GuestType.Isolated, Service.SourceNat.getName()));
        }

        // Don't allow to specify vlan if the caller is not ROOT admin
        if (!_accountMgr.isRootAdmin(caller.getId()) && (ntwkOff.isSpecifyVlan() || vlanId != null || bypassVlanOverlapCheck)) {
            throw new InvalidParameterValueException("Only ROOT admin is allowed to specify vlanId or bypass vlan overlap check");
        }

        if (ipv4) {
            // For non-root admins check cidr limit - if it's allowed by global config value
            if (!_accountMgr.isRootAdmin(caller.getId()) && cidr != null) {

                String[] cidrPair = cidr.split("\\/");
                int cidrSize = Integer.parseInt(cidrPair[1]);

                if (cidrSize < _cidrLimit) {
                    throw new InvalidParameterValueException("Cidr size can't be less than " + _cidrLimit);
                }
            }
        }

        Collection<String> ntwkProviders = _networkMgr.finalizeServicesAndProvidersForNetwork(ntwkOff, physicalNetworkId).values();
        if (ipv6 && providersConfiguredForExternalNetworking(ntwkProviders)) {
            throw new InvalidParameterValueException("Cannot support IPv6 on network offering with external devices!");
        }

        if (isolatedPvlan != null && providersConfiguredForExternalNetworking(ntwkProviders)) {
            throw new InvalidParameterValueException("Cannot support private vlan on network offering with external devices!");
        }

        if (cidr != null && providersConfiguredForExternalNetworking(ntwkProviders)) {
            if (ntwkOff.getGuestType() == GuestType.Shared && (zone.getNetworkType() == NetworkType.Advanced) && isSharedNetworkOfferingWithServices(networkOfferingId)) {
                // validate if CIDR specified overlaps with any of the CIDR's allocated for isolated networks and shared networks in the zone
                checkSharedNetworkCidrOverlap(zoneId, pNtwk.getId(), cidr);
            } else {
                // if the guest network is for the VPC, if any External Provider are supported in VPC
                // cidr will not be null as it is generated from the super cidr of vpc.
                // if cidr is not null and network is not part of vpc then throw the exception
                if (vpcId == null) {
                    throw new InvalidParameterValueException("Cannot specify CIDR when using network offering with external devices!");
                }
            }
        }

        // Vlan is created in 1 cases - works in Advance zone only:
        // 1) GuestType is Shared
        boolean createVlan = (startIP != null && endIP != null && zone.getNetworkType() == NetworkType.Advanced && ((ntwkOff.getGuestType() == Network.GuestType.Shared)
                || (ntwkOff.getGuestType() == GuestType.Isolated && !areServicesSupportedByNetworkOffering(ntwkOff.getId(), Service.SourceNat))));

        if (!createVlan) {
            // Only support advance shared network in IPv6, which means createVlan is a must
            if (ipv6) {
                createVlan = true;
            }
        }

        // Can add vlan range only to the network which allows it
        if (createVlan && !ntwkOff.isSpecifyIpRanges()) {
            throwInvalidIdException("Network offering with specified id doesn't support adding multiple ip ranges", ntwkOff.getUuid(), "networkOfferingId");
        }

        Network network = commitNetwork(networkOfferingId, gateway, startIP, endIP, netmask, networkDomain, vlanId, bypassVlanOverlapCheck, name, displayText, caller, physicalNetworkId, zoneId,
                domainId, isDomainSpecific, subdomainAccess, vpcId, startIPv6, endIPv6, ip6Gateway, ip6Cidr, displayNetwork, aclId, isolatedPvlan, ntwkOff, pNtwk, aclType, owner, cidr, createVlan,
                externalId);

        if (hideIpAddressUsage) {
            _networkDetailsDao.persist(new NetworkDetailVO(network.getId(), Network.hideIpAddressUsage, String.valueOf(hideIpAddressUsage), false));
        }

        // if the network offering has persistent set to true, implement the network
        if (ntwkOff.isPersistent()) {
            try {
                if (network.getState() == Network.State.Setup) {
                    s_logger.debug("Network id=" + network.getId() + " is already provisioned");
                    return network;
                }
                DeployDestination dest = new DeployDestination(zone, null, null, null);
                UserVO callerUser = _userDao.findById(CallContext.current().getCallingUserId());
                Journal journal = new Journal.LogJournal("Implementing " + network, s_logger);
                ReservationContext context = new ReservationContextImpl(UUID.randomUUID().toString(), journal, callerUser, caller);
                s_logger.debug("Implementing network " + network + " as a part of network provision for persistent network");
                Pair<? extends NetworkGuru, ? extends Network> implementedNetwork = _networkMgr.implementNetwork(network.getId(), dest, context);
                if (implementedNetwork == null || implementedNetwork.first() == null) {
                    s_logger.warn("Failed to provision the network " + network);
                }
                network = implementedNetwork.second();
            } catch (ResourceUnavailableException ex) {
                s_logger.warn("Failed to implement persistent guest network " + network + "due to ", ex);
                CloudRuntimeException e = new CloudRuntimeException("Failed to implement persistent guest network");
                e.addProxyObject(network.getUuid(), "networkId");
                throw e;
            }
        }
        return network;
    }

    private Network commitNetwork(final Long networkOfferingId, final String gateway, final String startIP, final String endIP, final String netmask, final String networkDomain, final String vlanId,
            final Boolean bypassVlanOverlapCheck, final String name, final String displayText, final Account caller, final Long physicalNetworkId, final Long zoneId, final Long domainId,
            final boolean isDomainSpecific, final Boolean subdomainAccessFinal, final Long vpcId, final String startIPv6, final String endIPv6, final String ip6Gateway, final String ip6Cidr,
            final Boolean displayNetwork, final Long aclId, final String isolatedPvlan, final NetworkOfferingVO ntwkOff, final PhysicalNetwork pNtwk, final ACLType aclType, final Account ownerFinal,
            final String cidr, final boolean createVlan, final String externalId) throws InsufficientCapacityException, ResourceAllocationException {
        try {
            Network network = Transaction.execute(new TransactionCallbackWithException<Network, Exception>() {
                @Override
                public Network doInTransaction(TransactionStatus status) throws InsufficientCapacityException, ResourceAllocationException {
                    Account owner = ownerFinal;
                    Boolean subdomainAccess = subdomainAccessFinal;

                    Long sharedDomainId = null;
                    if (isDomainSpecific) {
                        if (domainId != null) {
                            sharedDomainId = domainId;
                        } else {
                            sharedDomainId = _domainMgr.getDomain(Domain.ROOT_DOMAIN).getId();
                            subdomainAccess = true;
                        }
                    }

                    // default owner to system if network has aclType=Domain
                    if (aclType == ACLType.Domain) {
                        owner = _accountMgr.getAccount(Account.ACCOUNT_ID_SYSTEM);
                    }

                    // Create guest network
                    Network network = null;
                    if (vpcId != null) {
                        if (!_configMgr.isOfferingForVpc(ntwkOff)) {
                            throw new InvalidParameterValueException("Network offering can't be used for VPC networks");
                        }

                        if (aclId != null) {
                            NetworkACL acl = _networkACLDao.findById(aclId);
                            if (acl == null) {
                                throw new InvalidParameterValueException("Unable to find specified NetworkACL");
                            }

                            if (aclId != NetworkACL.DEFAULT_DENY && aclId != NetworkACL.DEFAULT_ALLOW) {
                                // ACL is not default DENY/ALLOW
                                // ACL should be associated with a VPC
                                if (!vpcId.equals(acl.getVpcId())) {
                                    throw new InvalidParameterValueException("ACL: " + aclId + " do not belong to the VPC");
                                }
                            }
                        }
                        network = _vpcMgr.createVpcGuestNetwork(networkOfferingId, name, displayText, gateway, cidr, vlanId, networkDomain, owner, sharedDomainId, pNtwk, zoneId, aclType,
                                subdomainAccess, vpcId, aclId, caller, displayNetwork, externalId);
                    } else {
                        if (_configMgr.isOfferingForVpc(ntwkOff)) {
                            throw new InvalidParameterValueException("Network offering can be used for VPC networks only");
                        }
                        if (ntwkOff.isInternalLb()) {
                            throw new InvalidParameterValueException("Internal Lb can be enabled on vpc networks only");
                        }

                        network = _networkMgr.createGuestNetwork(networkOfferingId, name, displayText, gateway, cidr, vlanId, bypassVlanOverlapCheck, networkDomain, owner, sharedDomainId, pNtwk,
                                zoneId, aclType, subdomainAccess, vpcId, ip6Gateway, ip6Cidr, displayNetwork, isolatedPvlan, externalId);
                    }

                    if (_accountMgr.isRootAdmin(caller.getId()) && createVlan && network != null) {
                        // Create vlan ip range
                        _configMgr.createVlanAndPublicIpRange(pNtwk.getDataCenterId(), network.getId(), physicalNetworkId, false, false, null, startIP, endIP, gateway, netmask, vlanId,
                                bypassVlanOverlapCheck, null, null, startIPv6, endIPv6, ip6Gateway, ip6Cidr);
                    }
                    return network;
                }
            });
            if (domainId != null && aclType == ACLType.Domain) {
                // send event for storing the domain wide resource access
                Map<String, Object> params = new HashMap<String, Object>();
                params.put(ApiConstants.ENTITY_TYPE, Network.class);
                params.put(ApiConstants.ENTITY_ID, network.getId());
                params.put(ApiConstants.DOMAIN_ID, domainId);
                params.put(ApiConstants.SUBDOMAIN_ACCESS, subdomainAccessFinal == null ? Boolean.TRUE : subdomainAccessFinal);
                _messageBus.publish(_name, EntityManager.MESSAGE_ADD_DOMAIN_WIDE_ENTITY_EVENT, PublishScope.LOCAL, params);
            }
            return network;
        } catch (Exception e) {
            ExceptionUtil.rethrowRuntime(e);
            ExceptionUtil.rethrow(e, InsufficientCapacityException.class);
            ExceptionUtil.rethrow(e, ResourceAllocationException.class);
            throw new IllegalStateException(e);
        }
    }

    @Override
    public Pair<List<? extends Network>, Integer> searchForNetworks(ListNetworksCmd cmd) {
        Long id = cmd.getId();
        String keyword = cmd.getKeyword();
        Long zoneId = cmd.getZoneId();
        Account caller = CallContext.current().getCallingAccount();
        Long domainId = cmd.getDomainId();
        String accountName = cmd.getAccountName();
        String guestIpType = cmd.getGuestIpType();
        String trafficType = cmd.getTrafficType();
        Boolean isSystem = cmd.getIsSystem();
        String aclType = cmd.getAclType();
        Long projectId = cmd.getProjectId();
        List<Long> permittedAccounts = new ArrayList<Long>();
        String path = null;
        Long physicalNetworkId = cmd.getPhysicalNetworkId();
        List<String> supportedServicesStr = cmd.getSupportedServices();
        Boolean restartRequired = cmd.isRestartRequired();
        boolean listAll = cmd.listAll();
        boolean isRecursive = cmd.isRecursive();
        Boolean specifyIpRanges = cmd.isSpecifyIpRanges();
        Long vpcId = cmd.getVpcId();
        Boolean canUseForDeploy = cmd.canUseForDeploy();
        Map<String, String> tags = cmd.getTags();
        Boolean forVpc = cmd.getForVpc();
        Boolean display = cmd.getDisplay();

        // 1) default is system to false if not specified
        // 2) reset parameter to false if it's specified by the regular user
        if ((isSystem == null || _accountMgr.isNormalUser(caller.getId())) && id == null) {
            isSystem = false;
        }

        // Account/domainId parameters and isSystem are mutually exclusive
        if (isSystem != null && isSystem && (accountName != null || domainId != null)) {
            throw new InvalidParameterValueException("System network belongs to system, account and domainId parameters can't be specified");
        }

        if (domainId != null) {
            DomainVO domain = _domainDao.findById(domainId);
            if (domain == null) {
                // see DomainVO.java
                throw new InvalidParameterValueException("Specified domain id doesn't exist in the system");
            }

            _accountMgr.checkAccess(caller, domain);
            if (accountName != null) {
                Account owner = _accountMgr.getActiveAccountByName(accountName, domainId);
                if (owner == null) {
                    // see DomainVO.java
                    throw new InvalidParameterValueException("Unable to find account " + accountName + " in specified domain");
                }

                _accountMgr.checkAccess(caller, null, true, owner);
                permittedAccounts.add(owner.getId());
            }
        }

        if (!_accountMgr.isAdmin(caller.getId()) || (projectId != null && projectId.longValue() != -1 && domainId == null)) {
            permittedAccounts.add(caller.getId());
            domainId = caller.getDomainId();
        }

        // set project information
        boolean skipProjectNetworks = true;
        if (projectId != null) {
            if (projectId.longValue() == -1) {
                if (!_accountMgr.isAdmin(caller.getId())) {
                    permittedAccounts.addAll(_projectMgr.listPermittedProjectAccounts(caller.getId()));
                }
            } else {
                permittedAccounts.clear();
                Project project = _projectMgr.getProject(projectId);
                if (project == null) {
                    throw new InvalidParameterValueException("Unable to find project by specified id");
                }
                if (!_projectMgr.canAccessProjectAccount(caller, project.getProjectAccountId())) {
                    // getProject() returns type ProjectVO.
                    throwInvalidIdException("Account " + caller + " cannot access specified project id", project.getUuid(), "projectId");
                }

                //add project account
                permittedAccounts.add(project.getProjectAccountId());
                //add caller account (if admin)
                if (_accountMgr.isAdmin(caller.getId())) {
                    permittedAccounts.add(caller.getId());
                }
            }
            skipProjectNetworks = false;
        }

        if (domainId != null) {
            path = _domainDao.findById(domainId).getPath();
        } else {
            path = _domainDao.findById(caller.getDomainId()).getPath();
        }

        if (listAll && domainId == null) {
            isRecursive = true;
        }

        Filter searchFilter = new Filter(NetworkVO.class, "id", false, null, null);
        SearchBuilder<NetworkVO> sb = _networksDao.createSearchBuilder();

        if (forVpc != null) {
            if (forVpc) {
                sb.and("vpc", sb.entity().getVpcId(), Op.NNULL);
            } else {
                sb.and("vpc", sb.entity().getVpcId(), Op.NULL);
            }
        }

        // Don't display networks created of system network offerings
        SearchBuilder<NetworkOfferingVO> networkOfferingSearch = _networkOfferingDao.createSearchBuilder();
        networkOfferingSearch.and("systemOnly", networkOfferingSearch.entity().isSystemOnly(), SearchCriteria.Op.EQ);
        if (isSystem != null && isSystem) {
            networkOfferingSearch.and("trafficType", networkOfferingSearch.entity().getTrafficType(), SearchCriteria.Op.EQ);
        }
        sb.join("networkOfferingSearch", networkOfferingSearch, sb.entity().getNetworkOfferingId(), networkOfferingSearch.entity().getId(), JoinBuilder.JoinType.INNER);

        SearchBuilder<DataCenterVO> zoneSearch = _dcDao.createSearchBuilder();
        zoneSearch.and("networkType", zoneSearch.entity().getNetworkType(), SearchCriteria.Op.EQ);
        sb.join("zoneSearch", zoneSearch, sb.entity().getDataCenterId(), zoneSearch.entity().getId(), JoinBuilder.JoinType.INNER);
        sb.and("removed", sb.entity().getRemoved(), Op.NULL);

        if (tags != null && !tags.isEmpty()) {
            SearchBuilder<ResourceTagVO> tagSearch = _resourceTagDao.createSearchBuilder();
            for (int count = 0; count < tags.size(); count++) {
                tagSearch.or().op("key" + String.valueOf(count), tagSearch.entity().getKey(), SearchCriteria.Op.EQ);
                tagSearch.and("value" + String.valueOf(count), tagSearch.entity().getValue(), SearchCriteria.Op.EQ);
                tagSearch.cp();
            }
            tagSearch.and("resourceType", tagSearch.entity().getResourceType(), SearchCriteria.Op.EQ);
            sb.groupBy(sb.entity().getId());
            sb.join("tagSearch", tagSearch, sb.entity().getId(), tagSearch.entity().getResourceId(), JoinBuilder.JoinType.INNER);
        }

        if (permittedAccounts.isEmpty()) {
            SearchBuilder<DomainVO> domainSearch = _domainDao.createSearchBuilder();
            domainSearch.and("path", domainSearch.entity().getPath(), SearchCriteria.Op.LIKE);
            sb.join("domainSearch", domainSearch, sb.entity().getDomainId(), domainSearch.entity().getId(), JoinBuilder.JoinType.INNER);
        }

        SearchBuilder<AccountVO> accountSearch = _accountDao.createSearchBuilder();
        accountSearch.and("typeNEQ", accountSearch.entity().getType(), SearchCriteria.Op.NEQ);
        accountSearch.and("typeEQ", accountSearch.entity().getType(), SearchCriteria.Op.EQ);

        sb.join("accountSearch", accountSearch, sb.entity().getAccountId(), accountSearch.entity().getId(), JoinBuilder.JoinType.INNER);

        List<NetworkVO> networksToReturn = new ArrayList<NetworkVO>();

        if (isSystem == null || !isSystem) {
            if (!permittedAccounts.isEmpty()) {
                //get account level networks
                networksToReturn.addAll(listAccountSpecificNetworks(buildNetworkSearchCriteria(sb, keyword, id, isSystem, zoneId, guestIpType, trafficType, physicalNetworkId, aclType,
                        skipProjectNetworks, restartRequired, specifyIpRanges, vpcId, tags, display), searchFilter, permittedAccounts));
                //get domain level networks
                if (domainId != null) {
                    networksToReturn.addAll(listDomainLevelNetworks(buildNetworkSearchCriteria(sb, keyword, id, isSystem, zoneId, guestIpType, trafficType, physicalNetworkId, aclType, true,
                            restartRequired, specifyIpRanges, vpcId, tags, display), searchFilter, domainId, false));
                }
            } else {
                //add account specific networks
                networksToReturn.addAll(listAccountSpecificNetworksByDomainPath(buildNetworkSearchCriteria(sb, keyword, id, isSystem, zoneId, guestIpType, trafficType, physicalNetworkId, aclType,
                        skipProjectNetworks, restartRequired, specifyIpRanges, vpcId, tags, display), searchFilter, path, isRecursive));
                //add domain specific networks of domain + parent domains
                networksToReturn.addAll(listDomainSpecificNetworksByDomainPath(buildNetworkSearchCriteria(sb, keyword, id, isSystem, zoneId, guestIpType, trafficType, physicalNetworkId, aclType,
                        skipProjectNetworks, restartRequired, specifyIpRanges, vpcId, tags, display), searchFilter, path, isRecursive));
                //add networks of subdomains
                if (domainId == null) {
                    networksToReturn.addAll(listDomainLevelNetworks(buildNetworkSearchCriteria(sb, keyword, id, isSystem, zoneId, guestIpType, trafficType, physicalNetworkId, aclType, true,
                            restartRequired, specifyIpRanges, vpcId, tags, display), searchFilter, caller.getDomainId(), true));
                }
            }
        } else {
            networksToReturn = _networksDao.search(buildNetworkSearchCriteria(sb, keyword, id, isSystem, zoneId, guestIpType, trafficType, physicalNetworkId, null, skipProjectNetworks,
                    restartRequired, specifyIpRanges, vpcId, tags, display), searchFilter);
        }

        if (supportedServicesStr != null && !supportedServicesStr.isEmpty() && !networksToReturn.isEmpty()) {
            List<NetworkVO> supportedNetworks = new ArrayList<NetworkVO>();
            Service[] suppportedServices = new Service[supportedServicesStr.size()];
            int i = 0;
            for (String supportedServiceStr : supportedServicesStr) {
                Service service = Service.getService(supportedServiceStr);
                if (service == null) {
                    throw new InvalidParameterValueException("Invalid service specified " + supportedServiceStr);
                } else {
                    suppportedServices[i] = service;
                }
                i++;
            }

            for (NetworkVO network : networksToReturn) {
                if (areServicesSupportedInNetwork(network.getId(), suppportedServices)) {
                    supportedNetworks.add(network);
                }
            }

            networksToReturn = supportedNetworks;
        }

        if (canUseForDeploy != null) {
            List<NetworkVO> networksForDeploy = new ArrayList<NetworkVO>();
            for (NetworkVO network : networksToReturn) {
                if (_networkModel.canUseForDeploy(network) == canUseForDeploy) {
                    networksForDeploy.add(network);
                }
            }

            networksToReturn = networksForDeploy;
        }

        //Now apply pagination
        List<? extends Network> wPagination = StringUtils.applyPagination(networksToReturn, cmd.getStartIndex(), cmd.getPageSizeVal());
        if (wPagination != null) {
            Pair<List<? extends Network>, Integer> listWPagination = new Pair<List<? extends Network>, Integer>(wPagination, networksToReturn.size());
            return listWPagination;
        }

        return new Pair<List<? extends Network>, Integer>(networksToReturn, networksToReturn.size());
    }

    private SearchCriteria<NetworkVO> buildNetworkSearchCriteria(SearchBuilder<NetworkVO> sb, String keyword, Long id, Boolean isSystem, Long zoneId, String guestIpType, String trafficType,
            Long physicalNetworkId, String aclType, boolean skipProjectNetworks, Boolean restartRequired, Boolean specifyIpRanges, Long vpcId, Map<String, String> tags, Boolean display) {

        SearchCriteria<NetworkVO> sc = sb.create();

        if (isSystem != null) {
            sc.setJoinParameters("networkOfferingSearch", "systemOnly", isSystem);
        }

        if (keyword != null) {
            SearchCriteria<NetworkVO> ssc = _networksDao.createSearchCriteria();
            ssc.addOr("name", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            sc.addAnd("name", SearchCriteria.Op.SC, ssc);
        }

        if (display != null) {
            sc.addAnd("displayNetwork", SearchCriteria.Op.EQ, display);
        }

        if (id != null) {
            sc.addAnd("id", SearchCriteria.Op.EQ, id);
        }

        if (zoneId != null) {
            sc.addAnd("dataCenterId", SearchCriteria.Op.EQ, zoneId);
        }

        if (guestIpType != null) {
            sc.addAnd("guestType", SearchCriteria.Op.EQ, guestIpType);
        }

        if (trafficType != null) {
            sc.addAnd("trafficType", SearchCriteria.Op.EQ, trafficType);
        }

        if (aclType != null) {
            sc.addAnd("aclType", SearchCriteria.Op.EQ, aclType.toString());
        }

        if (physicalNetworkId != null) {
            sc.addAnd("physicalNetworkId", SearchCriteria.Op.EQ, physicalNetworkId);
        }

        if (skipProjectNetworks) {
            sc.setJoinParameters("accountSearch", "typeNEQ", Account.ACCOUNT_TYPE_PROJECT);
        } else {
            sc.setJoinParameters("accountSearch", "typeEQ", Account.ACCOUNT_TYPE_PROJECT);
        }

        if (restartRequired != null) {
            sc.addAnd("restartRequired", SearchCriteria.Op.EQ, restartRequired);
        }

        if (specifyIpRanges != null) {
            sc.addAnd("specifyIpRanges", SearchCriteria.Op.EQ, specifyIpRanges);
        }

        if (vpcId != null) {
            sc.addAnd("vpcId", SearchCriteria.Op.EQ, vpcId);
        }

        if (tags != null && !tags.isEmpty()) {
            int count = 0;
            sc.setJoinParameters("tagSearch", "resourceType", ResourceObjectType.Network.toString());
            for (Map.Entry<String, String> entry : tags.entrySet()) {
                sc.setJoinParameters("tagSearch", "key" + String.valueOf(count), entry.getKey());
                sc.setJoinParameters("tagSearch", "value" + String.valueOf(count), entry.getValue());
                count++;
            }
        }

        return sc;
    }

    private List<NetworkVO> listDomainLevelNetworks(SearchCriteria<NetworkVO> sc, Filter searchFilter, long domainId, boolean parentDomainsOnly) {
        List<Long> networkIds = new ArrayList<Long>();
        Set<Long> allowedDomains = _domainMgr.getDomainParentIds(domainId);
        List<NetworkDomainVO> maps = _networkDomainDao.listDomainNetworkMapByDomain(allowedDomains.toArray());

        for (NetworkDomainVO map : maps) {
            if (map.getDomainId() == domainId && parentDomainsOnly) {
                continue;
            }
            boolean subdomainAccess = (map.isSubdomainAccess() != null) ? map.isSubdomainAccess() : getAllowSubdomainAccessGlobal();
            if (map.getDomainId() == domainId || subdomainAccess) {
                networkIds.add(map.getNetworkId());
            }
        }

        if (!networkIds.isEmpty()) {
            SearchCriteria<NetworkVO> domainSC = _networksDao.createSearchCriteria();
            domainSC.addAnd("id", SearchCriteria.Op.IN, networkIds.toArray());
            domainSC.addAnd("aclType", SearchCriteria.Op.EQ, ACLType.Domain.toString());

            sc.addAnd("id", SearchCriteria.Op.SC, domainSC);
            return _networksDao.search(sc, searchFilter);
        } else {
            return new ArrayList<NetworkVO>();
        }
    }

    private List<NetworkVO> listAccountSpecificNetworks(SearchCriteria<NetworkVO> sc, Filter searchFilter, List<Long> permittedAccounts) {
        SearchCriteria<NetworkVO> accountSC = _networksDao.createSearchCriteria();
        if (!permittedAccounts.isEmpty()) {
            accountSC.addAnd("accountId", SearchCriteria.Op.IN, permittedAccounts.toArray());
        }

        accountSC.addAnd("aclType", SearchCriteria.Op.EQ, ACLType.Account.toString());

        sc.addAnd("id", SearchCriteria.Op.SC, accountSC);
        return _networksDao.search(sc, searchFilter);
    }

    private List<NetworkVO> listAccountSpecificNetworksByDomainPath(SearchCriteria<NetworkVO> sc, Filter searchFilter, String path, boolean isRecursive) {
        SearchCriteria<NetworkVO> accountSC = _networksDao.createSearchCriteria();
        accountSC.addAnd("aclType", SearchCriteria.Op.EQ, ACLType.Account.toString());

        if (path != null) {
            if (isRecursive) {
                sc.setJoinParameters("domainSearch", "path", path + "%");
            } else {
                sc.setJoinParameters("domainSearch", "path", path);
            }
        }

        sc.addAnd("id", SearchCriteria.Op.SC, accountSC);
        return _networksDao.search(sc, searchFilter);
    }

    private List<NetworkVO> listDomainSpecificNetworksByDomainPath(SearchCriteria<NetworkVO> sc, Filter searchFilter, String path, boolean isRecursive) {

        Set<Long> allowedDomains = new HashSet<Long>();
        if (path != null) {
            if (isRecursive) {
                allowedDomains = _domainMgr.getDomainChildrenIds(path);
            } else {
                Domain domain = _domainDao.findDomainByPath(path);
                allowedDomains.add(domain.getId());
            }
        }

        List<Long> networkIds = new ArrayList<Long>();

        List<NetworkDomainVO> maps = _networkDomainDao.listDomainNetworkMapByDomain(allowedDomains.toArray());

        for (NetworkDomainVO map : maps) {
            networkIds.add(map.getNetworkId());
        }

        if (!networkIds.isEmpty()) {
            SearchCriteria<NetworkVO> domainSC = _networksDao.createSearchCriteria();
            domainSC.addAnd("id", SearchCriteria.Op.IN, networkIds.toArray());
            domainSC.addAnd("aclType", SearchCriteria.Op.EQ, ACLType.Domain.toString());

            sc.addAnd("id", SearchCriteria.Op.SC, domainSC);
            return _networksDao.search(sc, searchFilter);
        } else {
            return new ArrayList<NetworkVO>();
        }
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_NETWORK_DELETE, eventDescription = "deleting network", async = true)
    public boolean deleteNetwork(long networkId, boolean forced) {

        Account caller = CallContext.current().getCallingAccount();

        // Verify network id
        NetworkVO network = _networksDao.findById(networkId);
        if (network == null) {
            // see NetworkVO.java

            throwInvalidIdException("unable to find network with specified id", String.valueOf(networkId), "networkId");
        }

        // don't allow to delete system network
        if (isNetworkSystem(network)) {
            throwInvalidIdException("Network with specified id is system and can't be removed", network.getUuid(), "networkId");
        }

        Account owner = _accountMgr.getAccount(network.getAccountId());

        // Only Admin can delete Shared networks
        if ((network.getGuestType() == GuestType.Shared) && !_accountMgr.isAdmin(caller.getId())) {
            throw new InvalidParameterValueException("Only Admins can delete network with guest type " + network.getGuestType());
        }

        // Perform permission check
        _accountMgr.checkAccess(caller, null, true, network);

        if (forced && !_accountMgr.isRootAdmin(caller.getId())) {
            throw new InvalidParameterValueException("Delete network with 'forced' option can only be called by root admins");
        }

        User callerUser = _accountMgr.getActiveUser(CallContext.current().getCallingUserId());
        ReservationContext context = new ReservationContextImpl(null, null, callerUser, owner);

        return _networkMgr.destroyNetwork(networkId, context, forced);
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_NETWORK_RESTART, eventDescription = "restarting network", async = true)
    public boolean restartNetwork(RestartNetworkCmd cmd, boolean cleanup, boolean makeRedundant) throws ConcurrentOperationException, ResourceUnavailableException, InsufficientCapacityException {
        // This method restarts all network elements belonging to the network and re-applies all the rules
        Long networkId = cmd.getNetworkId();

        User callerUser = _accountMgr.getActiveUser(CallContext.current().getCallingUserId());
        Account callerAccount = _accountMgr.getActiveAccountById(callerUser.getAccountId());

        // Check if network exists
        NetworkVO network = _networksDao.findById(networkId);
        if (network == null) {
            throwInvalidIdException("Network with specified id doesn't exist", networkId.toString(), "networkId");
        }

        // Don't allow to restart network if it's not in Implemented/Setup state
        if (!(network.getState() == Network.State.Implemented || network.getState() == Network.State.Setup)) {
            throw new InvalidParameterValueException("Network is not in the right state to be restarted. Correct states are: " + Network.State.Implemented + ", " + Network.State.Setup);
        }

        if (network.getBroadcastDomainType() == BroadcastDomainType.Lswitch) {
            /**
             * Unable to restart these networks now.
             * TODO Restarting a SDN based network requires updating the nics and the configuration
             * in the controller. This requires a non-trivial rewrite of the restart procedure.
             */
            throw new InvalidParameterException("Unable to restart a running SDN network.");
        }

        _accountMgr.checkAccess(callerAccount, null, true, network);

        if (!network.isRedundant() && makeRedundant) {
            network.setRedundant(true);
            if (!_networksDao.update(network.getId(), network)) {
                throw new CloudRuntimeException("Failed to update network into a redundant one, please try again");
            }
            cleanup = true;
        }

        boolean success = _networkMgr.restartNetwork(networkId, callerAccount, callerUser, cleanup);

        if (success) {
            s_logger.debug("Network id=" + networkId + " is restarted successfully.");
        } else {
            s_logger.warn("Network id=" + networkId + " failed to restart.");
        }

        return success;
    }

    @Override
    public int getActiveNicsInNetwork(long networkId) {
        return _networksDao.getActiveNicsIn(networkId);
    }

    @Override
    public Map<Capability, String> getNetworkOfferingServiceCapabilities(NetworkOffering offering, Service service) {

        if (!areServicesSupportedByNetworkOffering(offering.getId(), service)) {
            // TBD: We should be sending networkOfferingId and not the offering object itself.
            throw new UnsupportedServiceException("Service " + service.getName() + " is not supported by the network offering " + offering);
        }

        Map<Capability, String> serviceCapabilities = new HashMap<Capability, String>();

        // get the Provider for this Service for this offering
        List<String> providers = _ntwkOfferingSrvcDao.listProvidersForServiceForNetworkOffering(offering.getId(), service);
        if (providers.isEmpty()) {
            // TBD: We should be sending networkOfferingId and not the offering object itself.
            throw new InvalidParameterValueException("Service " + service.getName() + " is not supported by the network offering " + offering);
        }

        // FIXME - in post 3.0 we are going to support multiple providers for the same service per network offering, so
        // we have to calculate capabilities for all of them
        String provider = providers.get(0);

        // FIXME we return the capabilities of the first provider of the service - what if we have multiple providers
        // for same Service?
        NetworkElement element = _networkModel.getElementImplementingProvider(provider);
        if (element != null) {
            Map<Service, Map<Capability, String>> elementCapabilities = element.getCapabilities();
            ;

            if (elementCapabilities == null || !elementCapabilities.containsKey(service)) {
                // TBD: We should be sending providerId and not the offering object itself.
                throw new UnsupportedServiceException("Service " + service.getName() + " is not supported by the element=" + element.getName() + " implementing Provider=" + provider);
            }
            serviceCapabilities = elementCapabilities.get(service);
        }

        return serviceCapabilities;
    }

    @Override
    public IpAddress getIp(long ipAddressId) {
        return _ipAddressDao.findById(ipAddressId);
    }

    protected boolean providersConfiguredForExternalNetworking(Collection<String> providers) {
        for (String providerStr : providers) {
            Provider provider = Network.Provider.getProvider(providerStr);
            if (provider.isExternal()) {
                return true;
            }
        }
        return false;
    }

    protected boolean isSharedNetworkOfferingWithServices(long networkOfferingId) {
        NetworkOfferingVO networkOffering = _networkOfferingDao.findById(networkOfferingId);
        if ((networkOffering.getGuestType() == Network.GuestType.Shared) && (areServicesSupportedByNetworkOffering(networkOfferingId, Service.SourceNat)
                || areServicesSupportedByNetworkOffering(networkOfferingId, Service.StaticNat) || areServicesSupportedByNetworkOffering(networkOfferingId, Service.Firewall)
                || areServicesSupportedByNetworkOffering(networkOfferingId, Service.PortForwarding) || areServicesSupportedByNetworkOffering(networkOfferingId, Service.Lb))) {
            return true;
        }
        return false;
    }

    protected boolean areServicesSupportedByNetworkOffering(long networkOfferingId, Service... services) {
        return (_ntwkOfferingSrvcDao.areServicesSupportedByNetworkOffering(networkOfferingId, services));
    }

    protected boolean areServicesSupportedInNetwork(long networkId, Service... services) {
        return (_ntwkSrvcDao.areServicesSupportedInNetwork(networkId, services));
    }

    private boolean checkForNonStoppedVmInNetwork(long networkId) {
        List<UserVmVO> vms = _userVmDao.listByNetworkIdAndStates(networkId, VirtualMachine.State.Starting, VirtualMachine.State.Running, VirtualMachine.State.Migrating, VirtualMachine.State.Stopping);
        return vms.isEmpty();
    }

    @Override
    @DB
    @ActionEvent(eventType = EventTypes.EVENT_NETWORK_UPDATE, eventDescription = "updating network", async = true)
    public Network updateGuestNetwork(final UpdateNetworkCmd cmd) {
        User callerUser = _accountService.getActiveUser(CallContext.current().getCallingUserId());
        Account callerAccount = _accountService.getActiveAccountById(callerUser.getAccountId());
        final long networkId = cmd.getId();
        String name = cmd.getNetworkName();
        String displayText = cmd.getDisplayText();
        String domainSuffix = cmd.getNetworkDomain();
        final Long networkOfferingId = cmd.getNetworkOfferingId();
        Boolean changeCidr = cmd.getChangeCidr();
        String guestVmCidr = cmd.getGuestVmCidr();
        Boolean displayNetwork = cmd.getDisplayNetwork();
        String customId = cmd.getCustomId();
        boolean updateInSequence = cmd.getUpdateInSequence();
        boolean forced = cmd.getForced();

        boolean restartNetwork = false;

        // verify input parameters
        final NetworkVO network = _networksDao.findById(networkId);
        if (network == null) {
            // see NetworkVO.java
            throwInvalidIdException("Specified network id doesn't exist in the system", String.valueOf(networkId), "networkId");
        }

        //perform below validation if the network is vpc network
        if (network.getVpcId() != null && networkOfferingId != null) {
            Vpc vpc = _entityMgr.findById(Vpc.class, network.getVpcId());
            _vpcMgr.validateNtwkOffForNtwkInVpc(networkId, networkOfferingId, null, null, vpc, null, _accountMgr.getAccount(network.getAccountId()), network.getNetworkACLId());
        }

        // don't allow to update network in Destroy state
        if (network.getState() == Network.State.Destroy) {
            throw new InvalidParameterValueException("Don't allow to update network in state " + Network.State.Destroy);
        }

        // Don't allow to update system network
        NetworkOffering offering = _networkOfferingDao.findByIdIncludingRemoved(network.getNetworkOfferingId());
        if (offering.isSystemOnly()) {
            throw new InvalidParameterValueException("Can't update system networks");
        }

        // allow to upgrade only Guest networks
        if (network.getTrafficType() != Networks.TrafficType.Guest) {
            throw new InvalidParameterValueException("Can't allow networks which traffic type is not " + TrafficType.Guest);
        }

        _accountMgr.checkAccess(callerAccount, null, true, network);
        _accountMgr.checkAccess(_accountMgr.getActiveAccountById(network.getAccountId()), offering, _dcDao.findById(network.getDataCenterId()));

        if (cmd instanceof UpdateNetworkCmdByAdmin) {
            final Boolean hideIpAddressUsage = ((UpdateNetworkCmdByAdmin) cmd).getHideIpAddressUsage();
            if (hideIpAddressUsage != null) {
                final NetworkDetailVO detail = _networkDetailsDao.findDetail(network.getId(), Network.hideIpAddressUsage);
                if (detail != null) {
                    detail.setValue(hideIpAddressUsage.toString());
                    _networkDetailsDao.update(detail.getId(), detail);
                } else {
                    _networkDetailsDao.persist(new NetworkDetailVO(network.getId(), Network.hideIpAddressUsage, hideIpAddressUsage.toString(), false));
                }
            }
        }

        if (name != null) {
            network.setName(name);
        }

        if (displayText != null) {
            network.setDisplayText(displayText);
        }

        if (customId != null) {
            network.setUuid(customId);
        }

        // display flag is not null and has changed
        if (displayNetwork != null && displayNetwork != network.getDisplayNetwork()) {
            // Update resource count if it needs to be updated
            NetworkOffering networkOffering = _networkOfferingDao.findById(network.getNetworkOfferingId());
            if (_networkMgr.resourceCountNeedsUpdate(networkOffering, network.getAclType())) {
                _resourceLimitMgr.changeResourceCount(network.getAccountId(), Resource.ResourceType.network, displayNetwork);
            }

            network.setDisplayNetwork(displayNetwork);
        }

        // network offering and domain suffix can be updated for Isolated networks only in 3.0
        if ((networkOfferingId != null || domainSuffix != null) && network.getGuestType() != GuestType.Isolated) {
            throw new InvalidParameterValueException("NetworkOffering and domain suffix upgrade can be perfomed for Isolated networks only");
        }

        boolean networkOfferingChanged = false;

        final long oldNetworkOfferingId = network.getNetworkOfferingId();
        NetworkOffering oldNtwkOff = _networkOfferingDao.findByIdIncludingRemoved(oldNetworkOfferingId);
        NetworkOfferingVO networkOffering = _networkOfferingDao.findById(networkOfferingId);
        if (networkOfferingId != null) {
            if (networkOffering == null || networkOffering.isSystemOnly()) {
                throwInvalidIdException("Unable to find network offering with specified id", networkOfferingId.toString(), "networkOfferingId");
            }

            // network offering should be in Enabled state
            if (networkOffering.getState() != NetworkOffering.State.Enabled) {
                throwInvalidIdException("Network offering with specified id is not in " + NetworkOffering.State.Enabled + " state, can't upgrade to it", networkOffering.getUuid(),
                        "networkOfferingId");
            }
            //can't update from vpc to non-vpc network offering
            boolean forVpcNew = _configMgr.isOfferingForVpc(networkOffering);
            boolean vorVpcOriginal = _configMgr.isOfferingForVpc(_entityMgr.findById(NetworkOffering.class, oldNetworkOfferingId));
            if (forVpcNew != vorVpcOriginal) {
                String errMsg = forVpcNew ? "a vpc offering " : "not a vpc offering";
                throw new InvalidParameterValueException("Can't update as the new offering is " + errMsg);
            }

            if (networkOfferingId != oldNetworkOfferingId) {
                Collection<String> newProviders = _networkMgr.finalizeServicesAndProvidersForNetwork(networkOffering, network.getPhysicalNetworkId()).values();
                Collection<String> oldProviders = _networkMgr.finalizeServicesAndProvidersForNetwork(oldNtwkOff, network.getPhysicalNetworkId()).values();

                if (providersConfiguredForExternalNetworking(newProviders) != providersConfiguredForExternalNetworking(oldProviders) && !changeCidr) {
                    throw new InvalidParameterValueException("Updating network failed since guest CIDR needs to be changed!");
                }
                if (changeCidr) {
                    if (!checkForNonStoppedVmInNetwork(network.getId())) {
                        throwInvalidIdException("All user vm of network of specified id should be stopped before changing CIDR!", network.getUuid(), "networkId");
                    }
                }
                // check if the network is upgradable
                if (!canUpgrade(network, oldNetworkOfferingId, networkOfferingId)) {
                    throw new InvalidParameterValueException("Can't upgrade from network offering " + oldNtwkOff.getUuid() + " to " + networkOffering.getUuid() + "; check logs for more information");
                }
                restartNetwork = true;
                networkOfferingChanged = true;

                //Setting the new network's isReduntant to the new network offering's RedundantRouter.
                network.setRedundant(_networkOfferingDao.findById(networkOfferingId).isRedundantRouter());
            }
        }

        final Map<String, String> newSvcProviders = networkOfferingChanged
                ? _networkMgr.finalizeServicesAndProvidersForNetwork(_entityMgr.findById(NetworkOffering.class, networkOfferingId), network.getPhysicalNetworkId())
                        : new HashMap<String, String>();

                // don't allow to modify network domain if the service is not supported
                if (domainSuffix != null) {
                    // validate network domain
                    if (!NetUtils.verifyDomainName(domainSuffix)) {
                        throw new InvalidParameterValueException(
                                "Invalid network domain. Total length shouldn't exceed 190 chars. Each domain label must be between 1 and 63 characters long, can contain ASCII letters 'a' through 'z', the digits '0' through '9', "
                                        + "and the hyphen ('-'); can't start or end with \"-\"");
                    }

                    long offeringId = oldNetworkOfferingId;
                    if (networkOfferingId != null) {
                        offeringId = networkOfferingId;
                    }

                    Map<Network.Capability, String> dnsCapabilities = getNetworkOfferingServiceCapabilities(_entityMgr.findById(NetworkOffering.class, offeringId), Service.Dns);
                    String isUpdateDnsSupported = dnsCapabilities.get(Capability.AllowDnsSuffixModification);
                    if (isUpdateDnsSupported == null || !Boolean.valueOf(isUpdateDnsSupported)) {
                        // TBD: use uuid instead of networkOfferingId. May need to hardcode tablename in call to addProxyObject().
                        throw new InvalidParameterValueException("Domain name change is not supported by the network offering id=" + networkOfferingId);
                    }

                    network.setNetworkDomain(domainSuffix);
                    // have to restart the network
                    restartNetwork = true;
                }

                //IP reservation checks
                // allow reservation only to Isolated Guest networks
                DataCenter dc = _dcDao.findById(network.getDataCenterId());
                String networkCidr = network.getNetworkCidr();

                if (guestVmCidr != null) {
                    if (dc.getNetworkType() == NetworkType.Basic) {
                        throw new InvalidParameterValueException("Guest VM CIDR can't be specified for zone with " + NetworkType.Basic + " networking");
                    }
                    if (network.getGuestType() != GuestType.Isolated) {
                        throw new InvalidParameterValueException("Can only allow IP Reservation in networks with guest type " + GuestType.Isolated);
                    }
                    if (networkOfferingChanged) {
                        throw new InvalidParameterValueException("Cannot specify this nework offering change and guestVmCidr at same time. Specify only one.");
                    }
                    if (!(network.getState() == Network.State.Implemented)) {
                        throw new InvalidParameterValueException("The network must be in " + Network.State.Implemented + " state. IP Reservation cannot be applied in " + network.getState() + " state");
                    }
                    if (!NetUtils.isValidIp4Cidr(guestVmCidr)) {
                        throw new InvalidParameterValueException("Invalid format of Guest VM CIDR.");
                    }
                    if (!NetUtils.validateGuestCidr(guestVmCidr)) {
                        throw new InvalidParameterValueException("Invalid format of Guest VM CIDR. Make sure it is RFC1918 compliant. ");
                    }

                    // If networkCidr is null it implies that there was no prior IP reservation, so the network cidr is network.getCidr()
                    // But in case networkCidr is a non null value (IP reservation already exists), it implies network cidr is networkCidr
                    if (networkCidr != null) {
                        if (!NetUtils.isNetworkAWithinNetworkB(guestVmCidr, networkCidr)) {
                            throw new InvalidParameterValueException("Invalid value of Guest VM CIDR. For IP Reservation, Guest VM CIDR  should be a subset of network CIDR : " + networkCidr);
                        }
                    } else {
                        if (!NetUtils.isNetworkAWithinNetworkB(guestVmCidr, network.getCidr())) {
                            throw new InvalidParameterValueException("Invalid value of Guest VM CIDR. For IP Reservation, Guest VM CIDR  should be a subset of network CIDR :  " + network.getCidr());
                        }
                    }

                    // This check makes sure there are no active IPs existing outside the guestVmCidr in the network
                    String[] guestVmCidrPair = guestVmCidr.split("\\/");
                    Long size = Long.valueOf(guestVmCidrPair[1]);
                    List<NicVO> nicsPresent = _nicDao.listByNetworkId(networkId);

                    String cidrIpRange[] = NetUtils.getIpRangeFromCidr(guestVmCidrPair[0], size);
                    s_logger.info("The start IP of the specified guest vm cidr is: " + cidrIpRange[0] + " and end IP is: " + cidrIpRange[1]);
                    long startIp = NetUtils.ip2Long(cidrIpRange[0]);
                    long endIp = NetUtils.ip2Long(cidrIpRange[1]);
                    long range = endIp - startIp + 1;
                    s_logger.info("The specified guest vm cidr has " + range + " IPs");

                    for (NicVO nic : nicsPresent) {
                        long nicIp = NetUtils.ip2Long(nic.getIPv4Address());
                        //check if nic IP is outside the guest vm cidr
                        if ((nicIp < startIp || nicIp > endIp) && nic.getState() != Nic.State.Deallocating) {
                            throw new InvalidParameterValueException("Active IPs like " + nic.getIPv4Address() + " exist outside the Guest VM CIDR. Cannot apply reservation ");
                        }
                    }

                    // In some scenarios even though guesVmCidr and network CIDR do not appear similar but
                    // the IP ranges exactly matches, in these special cases make sure no Reservation gets applied
                    if (network.getNetworkCidr() == null) {
                        if (NetUtils.isSameIpRange(guestVmCidr, network.getCidr()) && !guestVmCidr.equals(network.getCidr())) {
                            throw new InvalidParameterValueException("The Start IP and End IP of guestvmcidr: " + guestVmCidr + " and CIDR: " + network.getCidr() + " are same, "
                                    + "even though both the cidrs appear to be different. As a precaution no IP Reservation will be applied.");
                        }
                    } else {
                        if (NetUtils.isSameIpRange(guestVmCidr, network.getNetworkCidr()) && !guestVmCidr.equals(network.getNetworkCidr())) {
                            throw new InvalidParameterValueException("The Start IP and End IP of guestvmcidr: " + guestVmCidr + " and Network CIDR: " + network.getNetworkCidr() + " are same, "
                                    + "even though both the cidrs appear to be different. As a precaution IP Reservation will not be affected. If you want to reset IP Reservation, "
                                    + "specify guestVmCidr to be: " + network.getNetworkCidr());
                        }
                    }

                    // When reservation is applied for the first time, network_cidr will be null
                    // Populate it with the actual network cidr
                    if (network.getNetworkCidr() == null) {
                        network.setNetworkCidr(network.getCidr());
                    }

                    // Condition for IP Reservation reset : guestVmCidr and network CIDR are same
                    if (network.getNetworkCidr().equals(guestVmCidr)) {
                        s_logger.warn("Guest VM CIDR and Network CIDR both are same, reservation will reset.");
                        network.setNetworkCidr(null);
                    }
                    // Finally update "cidr" with the guestVmCidr
                    // which becomes the effective address space for CloudStack guest VMs
                    network.setCidr(guestVmCidr);
                    _networksDao.update(networkId, network);
                    s_logger.info("IP Reservation has been applied. The new CIDR for Guests Vms is " + guestVmCidr);
                }

                ReservationContext context = new ReservationContextImpl(null, null, callerUser, callerAccount);
                // 1) Shutdown all the elements and cleanup all the rules. Don't allow to shutdown network in intermediate
                // states - Shutdown and Implementing
                int resourceCount = 1;
                if (updateInSequence && restartNetwork && _networkOfferingDao.findById(network.getNetworkOfferingId()).isRedundantRouter()
                        && (networkOfferingId == null || _networkOfferingDao.findById(networkOfferingId).isRedundantRouter()) && network.getVpcId() == null) {
                    _networkMgr.canUpdateInSequence(network, forced);
                    NetworkDetailVO networkDetail = new NetworkDetailVO(network.getId(), Network.updatingInSequence, "true", true);
                    _networkDetailsDao.persist(networkDetail);
                    _networkMgr.configureUpdateInSequence(network);
                    resourceCount = _networkMgr.getResourceCount(network);
                }
                List<String> servicesNotInNewOffering = null;
                if (networkOfferingId != null) {
                    servicesNotInNewOffering = _networkMgr.getServicesNotSupportedInNewOffering(network, networkOfferingId);
                }
                if (!forced && servicesNotInNewOffering != null && !servicesNotInNewOffering.isEmpty()) {
                    NetworkOfferingVO newOffering = _networkOfferingDao.findById(networkOfferingId);
                    throw new CloudRuntimeException("The new offering:" + newOffering.getUniqueName() + " will remove the following services " + servicesNotInNewOffering
                            + "along with all the related configuration currently in use. will not proceed with the network update." + "set forced parameter to true for forcing an update.");
                }
                try {
                    if (servicesNotInNewOffering != null && !servicesNotInNewOffering.isEmpty()) {
                        _networkMgr.cleanupConfigForServicesInNetwork(servicesNotInNewOffering, network);
                    }
                } catch (Throwable e) {
                    s_logger.debug("failed to cleanup config related to unused services error:" + e.getMessage());
                }

                boolean validStateToShutdown = (network.getState() == Network.State.Implemented || network.getState() == Network.State.Setup || network.getState() == Network.State.Allocated);
                try {

                    do {
                        if (restartNetwork) {
                            if (validStateToShutdown) {
                                if (!changeCidr) {
                                    s_logger.debug("Shutting down elements and resources for network id=" + networkId + " as a part of network update");

                                    if (!_networkMgr.shutdownNetworkElementsAndResources(context, true, network)) {
                                        s_logger.warn("Failed to shutdown the network elements and resources as a part of network restart: " + network);
                                        CloudRuntimeException ex = new CloudRuntimeException("Failed to shutdown the network elements and resources as a part of update to network of specified id");
                                        ex.addProxyObject(network.getUuid(), "networkId");
                                        throw ex;
                                    }
                                } else {
                                    // We need to shutdown the network, since we want to re-implement the network.
                                    s_logger.debug("Shutting down network id=" + networkId + " as a part of network update");

                                    //check if network has reservation
                                    if (NetUtils.isNetworkAWithinNetworkB(network.getCidr(), network.getNetworkCidr())) {
                                        s_logger.warn(
                                                "Existing IP reservation will become ineffective for the network with id =  " + networkId + " You need to reapply reservation after network reimplementation.");
                                        //set cidr to the newtork cidr
                                        network.setCidr(network.getNetworkCidr());
                                        //set networkCidr to null to bring network back to no IP reservation state
                                        network.setNetworkCidr(null);
                                    }

                                    if (!_networkMgr.shutdownNetwork(network.getId(), context, true)) {
                                        s_logger.warn("Failed to shutdown the network as a part of update to network with specified id");
                                        CloudRuntimeException ex = new CloudRuntimeException("Failed to shutdown the network as a part of update of specified network id");
                                        ex.addProxyObject(network.getUuid(), "networkId");
                                        throw ex;
                                    }
                                }
                            } else {
                                CloudRuntimeException ex = new CloudRuntimeException(
                                        "Failed to shutdown the network elements and resources as a part of update to network with specified id; network is in wrong state: " + network.getState());
                                ex.addProxyObject(network.getUuid(), "networkId");
                                throw ex;
                            }
                        }

                        // 2) Only after all the elements and rules are shutdown properly, update the network VO
                        // get updated network
                        Network.State networkState = _networksDao.findById(networkId).getState();
                        boolean validStateToImplement = (networkState == Network.State.Implemented || networkState == Network.State.Setup || networkState == Network.State.Allocated);
                        if (restartNetwork && !validStateToImplement) {
                            CloudRuntimeException ex = new CloudRuntimeException(
                                    "Failed to implement the network elements and resources as a part of update to network with specified id; network is in wrong state: " + networkState);
                            ex.addProxyObject(network.getUuid(), "networkId");
                            throw ex;
                        }

                        if (networkOfferingId != null) {
                            if (networkOfferingChanged) {
                                Transaction.execute(new TransactionCallbackNoReturn() {
                                    @Override
                                    public void doInTransactionWithoutResult(TransactionStatus status) {
                                        network.setNetworkOfferingId(networkOfferingId);
                                        _networksDao.update(networkId, network, newSvcProviders);
                                        // get all nics using this network
                                        // log remove usage events for old offering
                                        // log assign usage events for new offering
                                        List<NicVO> nics = _nicDao.listByNetworkId(networkId);
                                        for (NicVO nic : nics) {
                                            long vmId = nic.getInstanceId();
                                            VMInstanceVO vm = _vmDao.findById(vmId);
                                            if (vm == null) {
                                                s_logger.error("Vm for nic " + nic.getId() + " not found with Vm Id:" + vmId);
                                                continue;
                                            }
                                            long isDefault = (nic.isDefaultNic()) ? 1 : 0;
                                            String nicIdString = Long.toString(nic.getId());
                                            UsageEventUtils.publishUsageEvent(EventTypes.EVENT_NETWORK_OFFERING_REMOVE, vm.getAccountId(), vm.getDataCenterId(), vm.getId(), nicIdString, oldNetworkOfferingId,
                                                    null, isDefault, VirtualMachine.class.getName(), vm.getUuid(), vm.isDisplay());
                                            UsageEventUtils.publishUsageEvent(EventTypes.EVENT_NETWORK_OFFERING_ASSIGN, vm.getAccountId(), vm.getDataCenterId(), vm.getId(), nicIdString, networkOfferingId,
                                                    null, isDefault, VirtualMachine.class.getName(), vm.getUuid(), vm.isDisplay());
                                        }
                                    }
                                });
                            } else {
                                network.setNetworkOfferingId(networkOfferingId);
                                _networksDao.update(networkId, network,
                                        _networkMgr.finalizeServicesAndProvidersForNetwork(_entityMgr.findById(NetworkOffering.class, networkOfferingId), network.getPhysicalNetworkId()));
                            }
                        } else {
                            _networksDao.update(networkId, network);
                        }

                        // 3) Implement the elements and rules again
                        if (restartNetwork) {
                            if (network.getState() != Network.State.Allocated) {
                                DeployDestination dest = new DeployDestination(_dcDao.findById(network.getDataCenterId()), null, null, null);
                                s_logger.debug("Implementing the network " + network + " elements and resources as a part of network update");
                                try {
                                    if (!changeCidr) {
                                        _networkMgr.implementNetworkElementsAndResources(dest, context, network, _networkOfferingDao.findById(network.getNetworkOfferingId()));
                                    } else {
                                        _networkMgr.implementNetwork(network.getId(), dest, context);
                                    }
                                } catch (Exception ex) {
                                    s_logger.warn("Failed to implement network " + network + " elements and resources as a part of network update due to ", ex);
                                    CloudRuntimeException e = new CloudRuntimeException("Failed to implement network (with specified id) elements and resources as a part of network update");
                                    e.addProxyObject(network.getUuid(), "networkId");
                                    throw e;
                                }
                            }
                        }

                        // 4) if network has been upgraded from a non persistent ntwk offering to a persistent ntwk offering,
                        // implement the network if its not already
                        if (networkOfferingChanged && !oldNtwkOff.isPersistent() && networkOffering.isPersistent()) {
                            if (network.getState() == Network.State.Allocated) {
                                try {
                                    DeployDestination dest = new DeployDestination(_dcDao.findById(network.getDataCenterId()), null, null, null);
                                    _networkMgr.implementNetwork(network.getId(), dest, context);
                                } catch (Exception ex) {
                                    s_logger.warn("Failed to implement network " + network + " elements and resources as a part o" + "f network update due to ", ex);
                                    CloudRuntimeException e = new CloudRuntimeException("Failed to implement network (with specified" + " id) elements and resources as a part of network update");
                                    e.addProxyObject(network.getUuid(), "networkId");
                                    throw e;
                                }
                            }
                        }
                        resourceCount--;
                    } while (updateInSequence && resourceCount > 0);
                } catch (Exception exception) {
                    if (updateInSequence) {
                        _networkMgr.finalizeUpdateInSequence(network, false);
                    }
                    throw new CloudRuntimeException("failed to update network " + network.getUuid() + " due to " + exception.getMessage());
                } finally {
                    if (updateInSequence) {
                        if (_networkDetailsDao.findDetail(networkId, Network.updatingInSequence) != null) {
                            _networkDetailsDao.removeDetail(networkId, Network.updatingInSequence);
                        }
                    }
                }
                return getNetwork(network.getId());
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_NETWORK_MIGRATE, eventDescription = "migrating network", async = true)
    public Network migrateGuestNetwork(long networkId, long networkOfferingId, Account callerAccount, User callerUser, boolean resume) {
        NetworkVO network = _networksDao.findById(networkId);
        NetworkOffering newNtwkOff = _networkOfferingDao.findById(networkOfferingId);

        //perform below validation if the network is vpc network
        if (network.getVpcId() != null) {
            s_logger.warn("Failed to migrate network as the specified network is a vpc tier. Use migrateVpc.");
            throw new InvalidParameterValueException("Failed to migrate network as the specified network is a vpc tier. Use migrateVpc.");
        }

        if (_configMgr.isOfferingForVpc(newNtwkOff)) {
            s_logger.warn("Failed to migrate network as the specified network offering is a VPC offering");
            throw new InvalidParameterValueException("Failed to migrate network as the specified network offering is a VPC offering");
        }

        verifyNetworkCanBeMigrated(callerAccount, network);

        //Retrieve new Physical NetworkId
        long newPhysicalNetworkId = findPhysicalNetworkId(network.getDataCenterId(), newNtwkOff.getTags(), newNtwkOff.getTrafficType());

        final long oldNetworkOfferingId = network.getNetworkOfferingId();
        NetworkOffering oldNtwkOff = _networkOfferingDao.findByIdIncludingRemoved(oldNetworkOfferingId);

        if (!resume && network.getRelated() != network.getId()) {
            s_logger.warn("Related network is not equal to network id. You might want to re-run migration with resume = true command.");
            throw new CloudRuntimeException("Failed to migrate network as previous migration left this network in transient condition. Specify resume as true.");
        }

        if (networkNeedsMigration(network, newPhysicalNetworkId, oldNtwkOff, newNtwkOff)) {
            return migrateNetworkToPhysicalNetwork(network, oldNtwkOff, newNtwkOff, null, null, newPhysicalNetworkId, callerAccount, callerUser);
        } else {
            s_logger.info("Network does not need migration.");
            return network;
        }
    }

    private class NetworkCopy {
        private Long networkIdInOldPhysicalNet;
        private Network networkInNewPhysicalNet;

        public NetworkCopy(Long networkIdInOldPhysicalNet, Network networkInNewPhysicalNet) {
            this.networkIdInOldPhysicalNet = networkIdInOldPhysicalNet;
            this.networkInNewPhysicalNet = networkInNewPhysicalNet;
        }

        public Long getNetworkIdInOldPhysicalNet() {
            return networkIdInOldPhysicalNet;
        }

        public Network getNetworkInNewPhysicalNet() {
            return networkInNewPhysicalNet;
        }
    }

    private Network migrateNetworkToPhysicalNetwork(Network network, NetworkOffering oldNtwkOff, NetworkOffering newNtwkOff, Long oldVpcId, Long newVpcId, long newPhysicalNetworkId,
            Account callerAccount, User callerUser) {
        boolean resume = network.getRelated() != network.getId();

        NetworkCopy networkCopy;

        // Resume is only true when there is already a copy of the network created
        if (resume) {
            Network networkInNewPhysicalNet = network;
            networkCopy = new NetworkCopy(network.getRelated(), networkInNewPhysicalNet);

            //the new network could already be implemented, check if the already partially upgrade networks has the same network offering as before or check if it still has the original network offering
            //the old network offering uuid should be the one of the already created copy
            if (networkInNewPhysicalNet.getNetworkOfferingId() != newNtwkOff.getId()) {
                throw new InvalidParameterValueException("Failed to resume migrating network as network offering does not match previously specified network offering (" + newNtwkOff.getUuid() + ")");
            }
        } else {
            networkCopy = Transaction.execute((TransactionCallback<NetworkCopy>)(status) -> migrateNetworkInDb(network, oldNtwkOff, newNtwkOff, oldVpcId, newVpcId, newPhysicalNetworkId));
        }

        Long networkIdInOldPhysicalNet = networkCopy.getNetworkIdInOldPhysicalNet();
        Network networkInNewPhysicalNet = networkCopy.getNetworkInNewPhysicalNet();

        ReservationContext context = new ReservationContextImpl(null, null, callerUser, callerAccount);
        DataCenter zone = _dcDao.findById(network.getDataCenterId());
        NetworkVO networkInOldPhysNet = _networksDao.findById(networkIdInOldPhysicalNet);

        boolean shouldImplement = (newNtwkOff.isPersistent() || networkInOldPhysNet.getState() == Network.State.Implemented) && networkInNewPhysicalNet.getState() != Network.State.Implemented;

        if (shouldImplement) {
            DeployDestination dest = new DeployDestination(zone, null, null, null);
            s_logger.debug("Implementing the network " + network + " elements and resources as a part of network update");
            try {
                networkInNewPhysicalNet = _networkMgr.implementNetwork(networkInNewPhysicalNet.getId(), dest, context).second();
            } catch (Exception ex) {
                s_logger.warn("Failed to implement network " + network + " elements and resources as a part of network update due to ", ex);
                CloudRuntimeException e = new CloudRuntimeException("Failed to implement network (with specified id) elements and resources as a part of network update");
                e.addProxyObject(network.getUuid(), "networkId");
                throw e;
            }
        }

        _networkMigrationManager.assignNicsToNewPhysicalNetwork(networkInOldPhysNet, networkInNewPhysicalNet);
        //clean up the old copy of the network
        _networkMigrationManager.deleteCopyOfNetwork(networkIdInOldPhysicalNet, networkInNewPhysicalNet.getId());

        return getNetwork(network.getId());
    }

    private NetworkCopy migrateNetworkInDb(Network network, NetworkOffering oldNtwkOff, NetworkOffering newNtwkOff, Long oldVpcId, Long newVpcId, long newPhysicalNetworkId) {
        //The copy will be the network in the old physical network
        //And we will use it to store tmp data while we upgrade or original network to the new physical network
        Long networkIdInOldPhysicalNet = _networkMigrationManager.makeCopyOfNetwork(network, oldNtwkOff, oldVpcId);
        Network networkInNewPhysicalNet = _networkMigrationManager.upgradeNetworkToNewNetworkOffering(network.getId(), newPhysicalNetworkId, newNtwkOff.getId(), newVpcId);
        return new NetworkCopy(networkIdInOldPhysicalNet, networkInNewPhysicalNet);
    }

    @Override
    public Vpc migrateVpcNetwork(long vpcId, long vpcOfferingId, Map<String, String> networkToOffering, Account account, User callerUser, boolean resume) {
        //Check if a previous migration run failed and try to resume if resume = true
        ResourceTag relatedVpc = _resourceTagDao.findByKey(vpcId, ResourceObjectType.Vpc, NetworkMigrationManager.MIGRATION);
        long vpcCopyId = 0;

        /*
         * In the vpc migration process the newly created Vpc will be used as the new VPC (opposed to network tier migration).
         * In case the copy of the vpc was already created. The uuid where already swapped and the id we receive here is the id of the Copy!
         * The id stored in the resource tag table under the key "migration" is the id of the ORIGINAL vpc!
         */
        if (relatedVpc != null) {
            if (resume) {
                vpcCopyId = vpcId;
                vpcId = Long.parseLong(relatedVpc.getValue());
                //let's check if the user did not change the vpcoffering opposed to the last failed run.
                verifyAlreadyMigratedTiers(vpcCopyId, vpcOfferingId, networkToOffering);
            } else {
                s_logger.warn("This vpc has a migration row in the resource details table. You might want to re-run migration with resume = true command.");
                throw new CloudRuntimeException("Failed to migrate VPC as previous migration left this VPC in transient condition. Specify resume as true.");
            }
        }

        Vpc vpc = _vpcDao.findById(vpcId);
        _accountMgr.checkAccess(account, null, true, vpc);
        _accountMgr.checkAccess(account, _vpcOfferingDao.findById(vpcOfferingId), _dcDao.findById(vpc.getZoneId()));

        if (vpc.getVpcOfferingId() == vpcOfferingId) {
            return vpc;
        }
        //Try to fail fast, check networks in the VPC and if we can migrate them before proceeding.
        List<NetworkVO> tiersInVpc = _networksDao.listByVpc(vpcId);
        vpcTiersCanBeMigrated(tiersInVpc, account, networkToOffering, resume);

        //In case this is the first time we try to migrate this vpc
        if (relatedVpc == null) {
            final long vpcIdFinal = vpcId;
            vpcCopyId = Transaction.execute((TransactionCallback<Long>)(status) -> _networkMigrationManager.makeCopyOfVpc(vpcIdFinal, vpcOfferingId));
        }

        Vpc copyOfVpc = _vpcDao.findById(vpcCopyId);
        _networkMigrationManager.startVpc(copyOfVpc);

        for (Network tier : tiersInVpc) {
            String networkOfferingUuid = networkToOffering.get(tier.getUuid());
            //UUID may be swapped already with a new uuid due to previous migration failure.
            //So we check the related network also in case we don't find the network offering
            Long networkId = null;
            if (resume && networkOfferingUuid == null) {
                tier = _networksDao.findById(tier.getRelated());
                networkOfferingUuid = networkToOffering.get(tier.getUuid());
                //In this case the tier already exists so we need to get the id of the tier so we can validate correctly
                networkId = tier.getId();
            }
            NetworkOfferingVO newNtwkOff = _networkOfferingDao.findByUuid(networkOfferingUuid);

            Account networkAccount = _accountService.getActiveAccountById(tier.getAccountId());
            try {
                _vpcMgr.validateNtwkOffForNtwkInVpc(networkId, newNtwkOff.getId(), tier.getCidr(), tier.getNetworkDomain(), copyOfVpc, tier.getGateway(), networkAccount, tier.getNetworkACLId());
            } catch (InvalidParameterValueException e) {
                s_logger.error("Specified network offering can not be used in combination with specified vpc offering. Aborting migration. You can re-run with resume = true and the correct uuid.");
                throw e;
            }

            long newPhysicalNetworkId = findPhysicalNetworkId(tier.getDataCenterId(), newNtwkOff.getTags(), newNtwkOff.getTrafficType());

            final long oldNetworkOfferingId = tier.getNetworkOfferingId();
            NetworkOffering oldNtwkOff = _networkOfferingDao.findByIdIncludingRemoved(oldNetworkOfferingId);

            if (networkNeedsMigration(tier, newPhysicalNetworkId, oldNtwkOff, newNtwkOff) || (resume && tier.getRelated() != tier.getId())) {
                migrateNetworkToPhysicalNetwork(tier, oldNtwkOff, newNtwkOff, vpcId, vpcCopyId, newPhysicalNetworkId, account, callerUser);
            }
        }
        _networkMigrationManager.deleteCopyOfVpc(vpcId, vpcCopyId);
        return _vpcDao.findById(vpcCopyId);
    }

    private void vpcTiersCanBeMigrated(List<? extends Network> tiersInVpc, Account account, Map<String, String> networkToOffering, boolean resume) {
        for (Network network : tiersInVpc) {
            String networkOfferingUuid = networkToOffering.get(network.getUuid());

            //offering uuid can be a tier where the uuid is previously already swapped in a previous migration
            if (resume && networkOfferingUuid == null) {
                NetworkVO oldVPCtier = _networksDao.findById(network.getRelated());
                networkOfferingUuid = networkToOffering.get(oldVPCtier.getUuid());
            }

            if (networkOfferingUuid == null) {
                throwInvalidIdException("Failed to migrate VPC as the specified tierNetworkOfferings is not complete", String.valueOf(network.getUuid()), "networkUuid");
            }

            NetworkOfferingVO newNtwkOff = _networkOfferingDao.findByUuid(networkOfferingUuid);

            if (newNtwkOff == null) {
                throwInvalidIdException("Failed to migrate VPC as at least one network offering in tierNetworkOfferings does not exist", networkOfferingUuid, "networkOfferingUuid");
            }

            if (!_configMgr.isOfferingForVpc(newNtwkOff)) {
                throw new InvalidParameterValueException(
                        "Network offering " + newNtwkOff.getName() + " (" + newNtwkOff.getUuid() + ") can't be used for VPC networks for network " + network.getName() + "(" + network.getUuid() + ")");
            }

            verifyNetworkCanBeMigrated(account, network);
            long newPhysicalNetworkId = findPhysicalNetworkId(network.getDataCenterId(), newNtwkOff.getTags(), newNtwkOff.getTrafficType());

            final long oldNetworkOfferingId = network.getNetworkOfferingId();
            NetworkOffering oldNtwkOff = _networkOfferingDao.findByIdIncludingRemoved(oldNetworkOfferingId);
            networkNeedsMigration(network, newPhysicalNetworkId, oldNtwkOff, newNtwkOff);
        }
    }

    private void verifyAlreadyMigratedTiers(long migratedVpcId, long vpcOfferingId, Map<String, String> networkToOffering) {
        Vpc migratedVpc = _vpcDao.findById(migratedVpcId);
        if (migratedVpc.getVpcOfferingId() != vpcOfferingId) {
            s_logger.error("The vpc is already partially migrated in a previous run. The provided vpc offering is not the same as the one used during the first migration process.");
            throw new InvalidParameterValueException("Failed to resume migrating VPC as VPC offering does not match previously specified VPC offering (" + migratedVpc.getVpcOfferingId() + ")");
        }

        List<NetworkVO> migratedTiers = _networksDao.listByVpc(migratedVpcId);
        for (Network tier : migratedTiers) {
            String tierNetworkOfferingUuid = networkToOffering.get(tier.getUuid());

            if (!StringUtils.isNotBlank(tierNetworkOfferingUuid)) {
                throwInvalidIdException("Failed to resume migrating VPC as the specified tierNetworkOfferings is not complete", String.valueOf(tier.getUuid()), "networkUuid");
            }

            NetworkOfferingVO newNetworkOffering = _networkOfferingDao.findByUuid(tierNetworkOfferingUuid);
            if (newNetworkOffering == null) {
                throw new InvalidParameterValueException("Failed to migrate VPC as at least one tier offering in tierNetworkOfferings does not exist.");
            }

            if (newNetworkOffering.getId() != tier.getNetworkOfferingId()) {
                NetworkOfferingVO tierNetworkOffering = _networkOfferingDao.findById(tier.getNetworkOfferingId());
                throw new InvalidParameterValueException(
                        "Failed to resume migrating VPC as at least one network offering in tierNetworkOfferings does not match previously specified network offering (network uuid=" + tier.getUuid()
                        + " was previously specified with offering uuid=" + tierNetworkOffering.getUuid() + ")");
            }
        }
    }

    private void throwInvalidIdException(String message, String uuid, String description) {
        InvalidParameterValueException ex = new InvalidParameterValueException(message);
        ex.addProxyObject(uuid, description);
        throw ex;
    }

    private boolean networkNeedsMigration(Network network, long newPhysicalNetworkId, NetworkOffering oldNtwkOff, NetworkOffering newNtwkOff) {

        if (newNtwkOff == null || newNtwkOff.isSystemOnly()) {
            InvalidParameterValueException ex = new InvalidParameterValueException("Unable to find network offering.");
            if (newNtwkOff != null) {
                ex.addProxyObject(String.valueOf(newNtwkOff.getId()), "networkOfferingId");
            }
            throw ex;
        }

        if (newNtwkOff.getId() != oldNtwkOff.getId() || network.getId() != network.getRelated()) {
            Collection<String> newProviders = _networkMgr.finalizeServicesAndProvidersForNetwork(newNtwkOff, newPhysicalNetworkId).values();
            Collection<String> oldProviders = _networkMgr.finalizeServicesAndProvidersForNetwork(oldNtwkOff, network.getPhysicalNetworkId()).values();

            if (providersConfiguredForExternalNetworking(newProviders) != providersConfiguredForExternalNetworking(oldProviders)) {
                throw new InvalidParameterValueException("Updating network failed since guest CIDR needs to be changed!");
            }

            // check if the network is moveable
            if (!canMoveToPhysicalNetwork(network, oldNtwkOff.getId(), newNtwkOff.getId())) {
                throw new InvalidParameterValueException("Can't upgrade from network offering " + oldNtwkOff.getUuid() + " to " + newNtwkOff.getUuid() + "; check logs for more information");
            }

            List<VMInstanceVO> vmInstances = _vmDao.listNonRemovedVmsByTypeAndNetwork(network.getId(), null);
            boolean vmStateIsNotTransitioning = vmInstances.stream().anyMatch(vm -> vm.getState() != VirtualMachine.State.Stopped && vm.getState() != VirtualMachine.State.Running);
            if (vmStateIsNotTransitioning) {
                throw new CloudRuntimeException("Failed to migrate network as at least one VM is not in running or stopped state.");
            }
        } else {
            return false;
        }

        // network offering should be in Enabled state
        if (newNtwkOff.getState() != NetworkOffering.State.Enabled) {
            throw new InvalidParameterValueException("Failed to migrate network as the specified network offering is not enabled.");
        }
        return true;
    }

    private void verifyNetworkCanBeMigrated(Account callerAccount, Network network) {
        // Don't allow to update system network
        NetworkOffering oldOffering = _networkOfferingDao.findByIdIncludingRemoved(network.getNetworkOfferingId());
        if (oldOffering.isSystemOnly()) {
            throw new InvalidParameterValueException("Failed to migrate network as the specified network is a system network.");
        }

        // allow to upgrade only Guest networks
        if (network.getTrafficType() != TrafficType.Guest) {
            throw new InvalidParameterValueException("Can't allow networks which traffic type is not " + TrafficType.Guest);
        }

        _accountMgr.checkAccess(callerAccount, null, true, network);

        boolean validateNetworkReadyToMigrate = (network.getState() == Network.State.Implemented || network.getState() == Network.State.Setup || network.getState() == Network.State.Allocated);
        if (!validateNetworkReadyToMigrate) {
            s_logger.error("Failed to migrate network as it is in invalid state.");
            CloudRuntimeException ex = new CloudRuntimeException("Failed to migrate network as it is in invalid state.");
            ex.addProxyObject(network.getUuid(), "networkId");
            throw ex;
        }
    }

    private boolean canMoveToPhysicalNetwork(Network network, long oldNetworkOfferingId, long newNetworkOfferingId) {
        NetworkOffering oldNetworkOffering = _networkOfferingDao.findByIdIncludingRemoved(oldNetworkOfferingId);
        NetworkOffering newNetworkOffering = _networkOfferingDao.findById(newNetworkOfferingId);

        // can move only Isolated networks for now
        if (oldNetworkOffering.getGuestType() != GuestType.Isolated) {
            throw new InvalidParameterValueException("NetworkOfferingId can be upgraded only for the network of type " + GuestType.Isolated);
        }

        // Type of the network should be the same
        if (oldNetworkOffering.getGuestType() != newNetworkOffering.getGuestType()) {
            s_logger.debug("Network offerings " + newNetworkOfferingId + " and " + oldNetworkOfferingId + " are of different types, can't upgrade");
            return false;
        }

        // Traffic types should be the same
        if (oldNetworkOffering.getTrafficType() != newNetworkOffering.getTrafficType()) {
            s_logger.debug("Network offerings " + newNetworkOfferingId + " and " + oldNetworkOfferingId + " have different traffic types, can't upgrade");
            return false;
        }

        // specify ipRanges should be the same
        if (oldNetworkOffering.isSpecifyIpRanges() != newNetworkOffering.isSpecifyIpRanges()) {
            s_logger.debug("Network offerings " + newNetworkOfferingId + " and " + oldNetworkOfferingId + " have different values for specifyIpRangess, can't upgrade");
            return false;
        }

        // Check all ips
        List<IPAddressVO> userIps = _ipAddressDao.listByAssociatedNetwork(network.getId(), null);
        List<PublicIp> publicIps = new ArrayList<PublicIp>();
        if (userIps != null && !userIps.isEmpty()) {
            for (IPAddressVO userIp : userIps) {
                PublicIp publicIp = PublicIp.createFromAddrAndVlan(userIp, _vlanDao.findById(userIp.getVlanId()));
                publicIps.add(publicIp);
            }
        }
        if (oldNetworkOffering.isConserveMode() && !newNetworkOffering.isConserveMode()) {
            if (!canIpsUsedForNonConserve(publicIps)) {
                return false;
            }
        }

        //can't update from internal LB to public LB
        if (areServicesSupportedByNetworkOffering(oldNetworkOfferingId, Service.Lb) && areServicesSupportedByNetworkOffering(newNetworkOfferingId, Service.Lb)) {
            if (oldNetworkOffering.isPublicLb() != newNetworkOffering.isPublicLb() || oldNetworkOffering.isInternalLb() != newNetworkOffering.isInternalLb()) {
                throw new InvalidParameterValueException("Original and new offerings support different types of LB - Internal vs Public," + " can't upgrade");
            }
        }

        return canIpsUseOffering(publicIps, newNetworkOfferingId);
    }

    protected boolean canUpgrade(Network network, long oldNetworkOfferingId, long newNetworkOfferingId) {
        NetworkOffering oldNetworkOffering = _networkOfferingDao.findByIdIncludingRemoved(oldNetworkOfferingId);
        NetworkOffering newNetworkOffering = _networkOfferingDao.findById(newNetworkOfferingId);

        // security group service should be the same
        if (areServicesSupportedByNetworkOffering(oldNetworkOfferingId, Service.SecurityGroup) != areServicesSupportedByNetworkOffering(newNetworkOfferingId, Service.SecurityGroup)) {
            s_logger.debug("Offerings " + newNetworkOfferingId + " and " + oldNetworkOfferingId + " have different securityGroupProperty, can't upgrade");
            return false;
        }

        // tags should be the same
        if (newNetworkOffering.getTags() != null) {
            if (oldNetworkOffering.getTags() == null) {
                s_logger.debug("New network offering id=" + newNetworkOfferingId + " has tags and old network offering id=" + oldNetworkOfferingId + " doesn't, can't upgrade");
                return false;
            }

            if (!StringUtils.areTagsEqual(oldNetworkOffering.getTags(), newNetworkOffering.getTags())) {
                s_logger.debug("Network offerings " + newNetworkOffering.getUuid() + " and " + oldNetworkOffering.getUuid() + " have different tags, can't upgrade");
                return false;
            }
        }

        // specify vlan should be the same
        if (oldNetworkOffering.isSpecifyVlan() != newNetworkOffering.isSpecifyVlan()) {
            s_logger.debug("Network offerings " + newNetworkOfferingId + " and " + oldNetworkOfferingId + " have different values for specifyVlan, can't upgrade");
            return false;
        }

        return canMoveToPhysicalNetwork(network, oldNetworkOfferingId, newNetworkOfferingId);
    }

    @Override
    @DB
    @ActionEvent(eventType = EventTypes.EVENT_PHYSICAL_NETWORK_CREATE, eventDescription = "Creating Physical Network", create = true)
    public PhysicalNetwork createPhysicalNetwork(final Long zoneId, final String vnetRange, final String networkSpeed, final List<String> isolationMethods, String broadcastDomainRangeStr,
            final Long domainId, final List<String> tags, final String name) {

        // Check if zone exists
        if (zoneId == null) {
            throw new InvalidParameterValueException("Please specify a valid zone.");
        }

        DataCenterVO zone = _dcDao.findById(zoneId);
        if (zone == null) {
            throw new InvalidParameterValueException("Please specify a valid zone.");
        }

        if (Grouping.AllocationState.Enabled == zone.getAllocationState()) {
            // TBD: Send uuid instead of zoneId; may have to hardcode tablename in call to addProxyObject().
            throw new PermissionDeniedException("Cannot create PhysicalNetwork since the Zone is currently enabled, zone Id: " + zoneId);
        }

        NetworkType zoneType = zone.getNetworkType();

        if (zoneType == NetworkType.Basic) {
            if (!_physicalNetworkDao.listByZone(zoneId).isEmpty()) {
                // TBD: Send uuid instead of zoneId; may have to hardcode tablename in call to addProxyObject().
                throw new CloudRuntimeException("Cannot add the physical network to basic zone id: " + zoneId + ", there is a physical network already existing in this basic Zone");
            }
        }
        if (tags != null && tags.size() > 1) {
            throw new InvalidParameterException("Only one tag can be specified for a physical network at this time");
        }

        if (isolationMethods != null && isolationMethods.size() > 1) {
            throw new InvalidParameterException("Only one isolationMethod can be specified for a physical network at this time");
        }

        if (vnetRange != null) {
            // Verify zone type
            if (zoneType == NetworkType.Basic || (zoneType == NetworkType.Advanced && zone.isSecurityGroupEnabled())) {
                throw new InvalidParameterValueException(
                        "Can't add vnet range to the physical network in the zone that supports " + zoneType + " network, Security Group enabled: " + zone.isSecurityGroupEnabled());
            }
        }

        BroadcastDomainRange broadcastDomainRange = null;
        if (broadcastDomainRangeStr != null && !broadcastDomainRangeStr.isEmpty()) {
            try {
                broadcastDomainRange = PhysicalNetwork.BroadcastDomainRange.valueOf(broadcastDomainRangeStr.toUpperCase());
            } catch (IllegalArgumentException ex) {
                throw new InvalidParameterValueException("Unable to resolve broadcastDomainRange '" + broadcastDomainRangeStr + "' to a supported value {Pod or Zone}");
            }

            // in Acton release you can specify only Zone broadcastdomain type in Advance zone, and Pod in Basic
            if (zoneType == NetworkType.Basic && broadcastDomainRange != null && broadcastDomainRange != BroadcastDomainRange.POD) {
                throw new InvalidParameterValueException("Basic zone can have broadcast domain type of value " + BroadcastDomainRange.POD + " only");
            } else if (zoneType == NetworkType.Advanced && broadcastDomainRange != null && broadcastDomainRange != BroadcastDomainRange.ZONE) {
                throw new InvalidParameterValueException("Advance zone can have broadcast domain type of value " + BroadcastDomainRange.ZONE + " only");
            }
        }

        if (broadcastDomainRange == null) {
            if (zoneType == NetworkType.Basic) {
                broadcastDomainRange = PhysicalNetwork.BroadcastDomainRange.POD;
            } else {
                broadcastDomainRange = PhysicalNetwork.BroadcastDomainRange.ZONE;
            }
        }

        try {
            final BroadcastDomainRange broadcastDomainRangeFinal = broadcastDomainRange;
            return Transaction.execute(new TransactionCallback<PhysicalNetworkVO>() {
                @Override
                public PhysicalNetworkVO doInTransaction(TransactionStatus status) {
                    // Create the new physical network in the database
                    long id = _physicalNetworkDao.getNextInSequence(Long.class, "id");
                    PhysicalNetworkVO pNetwork = new PhysicalNetworkVO(id, zoneId, vnetRange, networkSpeed, domainId, broadcastDomainRangeFinal, name);
                    pNetwork.setTags(tags);
                    pNetwork.setIsolationMethods(isolationMethods);

                    pNetwork = _physicalNetworkDao.persist(pNetwork);

                    // Add vnet entries for the new zone if zone type is Advanced
                    if (vnetRange != null) {
                        addOrRemoveVnets(vnetRange.split(","), pNetwork);
                    }

                    // add VirtualRouter as the default network service provider
                    addDefaultVirtualRouterToPhysicalNetwork(pNetwork.getId());

                    if (pNetwork.getIsolationMethods().contains("GRE")) {
                        addDefaultOvsToPhysicalNetwork(pNetwork.getId());
                    }

                    // add security group provider to the physical network
                    addDefaultSecurityGroupProviderToPhysicalNetwork(pNetwork.getId());

                    // add VPCVirtualRouter as the defualt network service provider
                    addDefaultVpcVirtualRouterToPhysicalNetwork(pNetwork.getId());

                    // add baremetal as the defualt network service provider
                    addDefaultBaremetalProvidersToPhysicalNetwork(pNetwork.getId());

                    //Add Internal Load Balancer element as a default network service provider
                    addDefaultInternalLbProviderToPhysicalNetwork(pNetwork.getId());

                    // Add the config drive provider
                    addConfigDriveToPhysicalNetwork(pNetwork.getId());

                    return pNetwork;
                }
            });
        } catch (Exception ex) {
            s_logger.warn("Exception: ", ex);
            throw new CloudRuntimeException("Fail to create a physical network");
        }
    }

    @Override
    public Pair<List<? extends PhysicalNetwork>, Integer> searchPhysicalNetworks(Long id, Long zoneId, String keyword, Long startIndex, Long pageSize, String name) {
        Filter searchFilter = new Filter(PhysicalNetworkVO.class, "id", Boolean.TRUE, startIndex, pageSize);
        SearchCriteria<PhysicalNetworkVO> sc = _physicalNetworkDao.createSearchCriteria();

        if (id != null) {
            sc.addAnd("id", SearchCriteria.Op.EQ, id);
        }

        if (zoneId != null) {
            sc.addAnd("dataCenterId", SearchCriteria.Op.EQ, zoneId);
        }

        if (name != null) {
            sc.addAnd("name", SearchCriteria.Op.LIKE, "%" + name + "%");
        }

        Pair<List<PhysicalNetworkVO>, Integer> result = _physicalNetworkDao.searchAndCount(sc, searchFilter);
        return new Pair<List<? extends PhysicalNetwork>, Integer>(result.first(), result.second());
    }

    @Override
    @DB
    @ActionEvent(eventType = EventTypes.EVENT_PHYSICAL_NETWORK_UPDATE, eventDescription = "updating physical network", async = true)
    public PhysicalNetwork updatePhysicalNetwork(Long id, String networkSpeed, List<String> tags, String newVnetRange, String state) {

        // verify input parameters
        PhysicalNetworkVO network = _physicalNetworkDao.findById(id);
        if (network == null) {
            throwInvalidIdException("Physical Network with specified id doesn't exist in the system", id.toString(), "physicalNetworkId");
        }

        // if zone is of Basic type, don't allow to add vnet range
        DataCenter zone = _dcDao.findById(network.getDataCenterId());
        if (zone == null) {
            throwInvalidIdException("Zone with id=" + network.getDataCenterId() + " doesn't exist in the system", String.valueOf(network.getDataCenterId()), "dataCenterId");
        }
        if (newVnetRange != null) {
            if (zone.getNetworkType() == NetworkType.Basic || (zone.getNetworkType() == NetworkType.Advanced && zone.isSecurityGroupEnabled())) {
                throw new InvalidParameterValueException(
                        "Can't add vnet range to the physical network in the zone that supports " + zone.getNetworkType() + " network, Security Group enabled: " + zone.isSecurityGroupEnabled());
            }
        }

        if (tags != null && tags.size() > 1) {
            throw new InvalidParameterException("Unable to support more than one tag on network yet");
        }

        PhysicalNetwork.State networkState = null;
        if (state != null && !state.isEmpty()) {
            try {
                networkState = PhysicalNetwork.State.valueOf(state);
            } catch (IllegalArgumentException ex) {
                throw new InvalidParameterValueException("Unable to resolve state '" + state + "' to a supported value {Enabled or Disabled}");
            }
        }

        if (state != null) {
            network.setState(networkState);
        }

        if (tags != null) {
            network.setTags(tags);
        }

        if (networkSpeed != null) {
            network.setSpeed(networkSpeed);
        }

        if (newVnetRange != null) {
            String[] listOfRanges = newVnetRange.split(",");
            addOrRemoveVnets(listOfRanges, network);
        }
        _physicalNetworkDao.update(id, network);
        return network;

    }

    @DB
    public void addOrRemoveVnets(String[] listOfRanges, final PhysicalNetworkVO network) {
        List<String> addVnets = null;
        List<String> removeVnets = null;
        HashSet<String> tempVnets = new HashSet<String>();
        HashSet<String> vnetsInDb = new HashSet<String>();
        List<Pair<Integer, Integer>> vnetranges = null;
        String comaSeperatedStingOfVnetRanges = null;
        int i = 0;
        if (listOfRanges.length != 0) {
            _physicalNetworkDao.acquireInLockTable(network.getId(), 10);
            vnetranges = validateVlanRange(network, listOfRanges);

            //computing vnets to be removed.
            removeVnets = getVnetsToremove(network, vnetranges);

            //computing vnets to add
            vnetsInDb.addAll(_datacneterVnet.listVnetsByPhysicalNetworkAndDataCenter(network.getDataCenterId(), network.getId()));
            tempVnets.addAll(vnetsInDb);
            for (Pair<Integer, Integer> vlan : vnetranges) {
                for (i = vlan.first(); i <= vlan.second(); i++) {
                    tempVnets.add(Integer.toString(i));
                }
            }
            tempVnets.removeAll(vnetsInDb);

            //vnets to add in tempVnets.
            //adding and removing vnets from vnetsInDb
            if (removeVnets != null && removeVnets.size() != 0) {
                vnetsInDb.removeAll(removeVnets);
            }

            if (tempVnets.size() != 0) {
                addVnets = new ArrayList<String>();
                addVnets.addAll(tempVnets);
                vnetsInDb.addAll(tempVnets);
            }

            //sorting the vnets in Db to generate a coma seperated list of  the vnet string.
            if (vnetsInDb.size() != 0) {
                comaSeperatedStingOfVnetRanges = generateVnetString(new ArrayList<String>(vnetsInDb));
            }
            network.setVnet(comaSeperatedStingOfVnetRanges);

            final List<String> addVnetsFinal = addVnets;
            final List<String> removeVnetsFinal = removeVnets;
            Transaction.execute(new TransactionCallbackNoReturn() {
                @Override
                public void doInTransactionWithoutResult(TransactionStatus status) {
                    if (addVnetsFinal != null) {
                        s_logger.debug("Adding vnet range " + addVnetsFinal.toString() + " for the physicalNetwork id= " + network.getId() + " and zone id=" + network.getDataCenterId()
                        + " as a part of updatePhysicalNetwork call");
                        //add vnet takes a list of strings to be added. each string is a vnet.
                        _dcDao.addVnet(network.getDataCenterId(), network.getId(), addVnetsFinal);
                    }
                    if (removeVnetsFinal != null) {
                        s_logger.debug("removing vnet range " + removeVnetsFinal.toString() + " for the physicalNetwork id= " + network.getId() + " and zone id=" + network.getDataCenterId()
                        + " as a part of updatePhysicalNetwork call");
                        //deleteVnets  takes a list of strings to be removed. each string is a vnet.
                        _datacneterVnet.deleteVnets(TransactionLegacy.currentTxn(), network.getDataCenterId(), network.getId(), removeVnetsFinal);
                    }
                    _physicalNetworkDao.update(network.getId(), network);
                }
            });

            _physicalNetworkDao.releaseFromLockTable(network.getId());
        }
    }

    private List<Pair<Integer, Integer>> validateVlanRange(PhysicalNetworkVO network, String[] listOfRanges) {
        Integer StartVnet;
        Integer EndVnet;
        List<Pair<Integer, Integer>> vlanTokens = new ArrayList<Pair<Integer, Integer>>();
        for (String vlanRange : listOfRanges) {
            String[] VnetRange = vlanRange.split("-");

            // Init with [min,max] of VLAN. Actually 0x000 and 0xFFF are reserved by IEEE, shoudn't be used.
            long minVnet = MIN_VLAN_ID;
            long maxVnet = MAX_VLAN_ID;

            // for GRE phynets allow up to 32bits
            // TODO: Not happy about this test.
            // What about guru-like objects for physical networs?
            s_logger.debug("ISOLATION METHODS:" + network.getIsolationMethods());
            // Java does not have unsigned types...
            if (network.getIsolationMethods().contains("GRE")) {
                minVnet = MIN_GRE_KEY;
                maxVnet = MAX_GRE_KEY;
            } else if (network.getIsolationMethods().contains("VXLAN")) {
                minVnet = MIN_VXLAN_VNI;
                maxVnet = MAX_VXLAN_VNI;
                // fail if zone already contains VNI, need to be unique per zone.
                // since adding a range adds each VNI to the database, need only check min/max
                for (String vnet : VnetRange) {
                    s_logger.debug("Looking to see if VNI " + vnet + " already exists on another network in zone " + network.getDataCenterId());
                    List<DataCenterVnetVO> vnis = _datacneterVnet.findVnet(network.getDataCenterId(), vnet);
                    if (vnis != null && !vnis.isEmpty()) {
                        for (DataCenterVnetVO vni : vnis) {
                            if (vni.getPhysicalNetworkId() != network.getId()) {
                                s_logger.debug("VNI " + vnet + " already exists on another network in zone, please specify a unique range");
                                throw new InvalidParameterValueException("VNI " + vnet + " already exists on another network in zone, please specify a unique range");
                            }
                        }
                    }
                }
            }
            String rangeMessage = " between " + minVnet + " and " + maxVnet;
            if (VnetRange.length == 1 && VnetRange[0].equals("")) {
                return vlanTokens;
            }
            if (VnetRange.length < 2) {
                throw new InvalidParameterValueException("Please provide valid vnet range. vnet range should be a coma seperated list of vlan ranges. example 500-500,600-601" + rangeMessage);
            }

            if (VnetRange[0] == null || VnetRange[1] == null) {
                throw new InvalidParameterValueException("Please provide valid vnet range" + rangeMessage);
            }

            try {
                StartVnet = Integer.parseInt(VnetRange[0]);
                EndVnet = Integer.parseInt(VnetRange[1]);
            } catch (NumberFormatException e) {
                s_logger.warn("Unable to parse vnet range:", e);
                throw new InvalidParameterValueException("Please provide valid vnet range. The vnet range should be a coma seperated list example 2001-2012,3000-3005." + rangeMessage);
            }
            if (StartVnet < minVnet || EndVnet > maxVnet) {
                throw new InvalidParameterValueException("Vnet range has to be" + rangeMessage);
            }

            if (StartVnet > EndVnet) {
                throw new InvalidParameterValueException("Vnet range has to be" + rangeMessage + " and start range should be lesser than or equal to stop range");
            }
            vlanTokens.add(new Pair<Integer, Integer>(StartVnet, EndVnet));
        }
        return vlanTokens;

    }

    public String generateVnetString(List<String> vnetList) {
        Collections.sort(vnetList, new Comparator<String>() {
            @Override
            public int compare(String s1, String s2) {
                return Integer.valueOf(s1).compareTo(Integer.valueOf(s2));
            }
        });
        int i;
        //build the vlan string form the sorted list.
        String vnetRange = "";
        String startvnet = vnetList.get(0);
        String endvnet = "";
        for (i = 0; i < vnetList.size() - 1; i++) {
            if (Integer.parseInt(vnetList.get(i + 1)) - Integer.parseInt(vnetList.get(i)) > 1) {
                endvnet = vnetList.get(i);
                vnetRange = vnetRange + startvnet + "-" + endvnet + ",";
                startvnet = vnetList.get(i + 1);
            }
        }
        endvnet = vnetList.get(vnetList.size() - 1);
        vnetRange = vnetRange + startvnet + "-" + endvnet + ",";
        vnetRange = vnetRange.substring(0, vnetRange.length() - 1);
        return vnetRange;
    }

    private List<String> getVnetsToremove(PhysicalNetworkVO network, List<Pair<Integer, Integer>> vnetRanges) {
        int i;
        List<String> removeVnets = new ArrayList<String>();
        HashSet<String> vnetsInDb = new HashSet<String>();
        vnetsInDb.addAll(_datacneterVnet.listVnetsByPhysicalNetworkAndDataCenter(network.getDataCenterId(), network.getId()));
        //remove all the vnets from vnets in db to check if there are any vnets that are not there in given list.
        //remove all the vnets not in the list of vnets passed by the user.
        if (vnetRanges.size() == 0) {
            //this implies remove all vlans.
            removeVnets.addAll(vnetsInDb);
            int allocated_vnets = _datacneterVnet.countAllocatedVnets(network.getId());
            if (allocated_vnets > 0) {
                throw new InvalidParameterValueException("physicalnetwork " + network.getId() + " has " + allocated_vnets + " vnets in use");
            }
            return removeVnets;
        }
        for (Pair<Integer, Integer> vlan : vnetRanges) {
            for (i = vlan.first(); i <= vlan.second(); i++) {
                vnetsInDb.remove(Integer.toString(i));
            }
        }
        String vnetRange = null;
        if (vnetsInDb.size() != 0) {
            removeVnets.addAll(vnetsInDb);
            vnetRange = generateVnetString(removeVnets);
        } else {
            return removeVnets;
        }

        for (String vnet : vnetRange.split(",")) {
            String[] range = vnet.split("-");
            Integer start = Integer.parseInt(range[0]);
            Integer end = Integer.parseInt(range[1]);
            _datacneterVnet.lockRange(network.getDataCenterId(), network.getId(), start, end);
            List<DataCenterVnetVO> result = _datacneterVnet.listAllocatedVnetsInRange(network.getDataCenterId(), network.getId(), start, end);
            if (!result.isEmpty()) {
                throw new InvalidParameterValueException("physicalnetwork " + network.getId() + " has allocated vnets in the range " + start + "-" + end);

            }
            // If the range is partially dedicated to an account fail the request
            List<AccountGuestVlanMapVO> maps = _accountGuestVlanMapDao.listAccountGuestVlanMapsByPhysicalNetwork(network.getId());
            for (AccountGuestVlanMapVO map : maps) {
                String[] vlans = map.getGuestVlanRange().split("-");
                Integer dedicatedStartVlan = Integer.parseInt(vlans[0]);
                Integer dedicatedEndVlan = Integer.parseInt(vlans[1]);
                if ((start >= dedicatedStartVlan && start <= dedicatedEndVlan) || (end >= dedicatedStartVlan && end <= dedicatedEndVlan)) {
                    throw new InvalidParameterValueException("Vnet range " + map.getGuestVlanRange() + " is dedicated" + " to an account. The specified range " + start + "-" + end
                            + " overlaps with the dedicated range " + " Please release the overlapping dedicated range before deleting the range");
                }
            }
        }
        return removeVnets;
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_PHYSICAL_NETWORK_DELETE, eventDescription = "deleting physical network", async = true)
    @DB
    public boolean deletePhysicalNetwork(final Long physicalNetworkId) {

        // verify input parameters
        PhysicalNetworkVO pNetwork = _physicalNetworkDao.findById(physicalNetworkId);
        if (pNetwork == null) {
            throwInvalidIdException("Physical Network with specified id doesn't exist in the system", physicalNetworkId.toString(), "physicalNetworkId");
        }

        checkIfPhysicalNetworkIsDeletable(physicalNetworkId);

        return Transaction.execute(new TransactionCallback<Boolean>() {
            @Override
            public Boolean doInTransaction(TransactionStatus status) {
                // delete vlans for this zone
                List<VlanVO> vlans = _vlanDao.listVlansByPhysicalNetworkId(physicalNetworkId);
                for (VlanVO vlan : vlans) {
                    _vlanDao.remove(vlan.getId());
                }

                // Delete networks
                List<NetworkVO> networks = _networksDao.listByPhysicalNetwork(physicalNetworkId);
                if (networks != null && !networks.isEmpty()) {
                    for (NetworkVO network : networks) {
                        _networksDao.remove(network.getId());
                    }
                }

                // delete vnets
                _dcDao.deleteVnet(physicalNetworkId);

                // delete service providers
                List<PhysicalNetworkServiceProviderVO> providers = _pNSPDao.listBy(physicalNetworkId);

                for (PhysicalNetworkServiceProviderVO provider : providers) {
                    try {
                        deleteNetworkServiceProvider(provider.getId());
                    } catch (ResourceUnavailableException e) {
                        s_logger.warn("Unable to complete destroy of the physical network provider: " + provider.getProviderName() + ", id: " + provider.getId(), e);
                        return false;
                    } catch (ConcurrentOperationException e) {
                        s_logger.warn("Unable to complete destroy of the physical network provider: " + provider.getProviderName() + ", id: " + provider.getId(), e);
                        return false;
                    }
                }

                // delete traffic types
                _pNTrafficTypeDao.deleteTrafficTypes(physicalNetworkId);

                return _physicalNetworkDao.remove(physicalNetworkId);
            }
        });
    }

    @DB
    protected void checkIfPhysicalNetworkIsDeletable(Long physicalNetworkId) {
        List<List<String>> tablesToCheck = new ArrayList<List<String>>();

        List<String> vnet = new ArrayList<String>();
        vnet.add(0, "op_dc_vnet_alloc");
        vnet.add(1, "physical_network_id");
        vnet.add(2, "there are allocated vnets for this physical network");
        tablesToCheck.add(vnet);

        List<String> networks = new ArrayList<String>();
        networks.add(0, "networks");
        networks.add(1, "physical_network_id");
        networks.add(2, "there are networks associated to this physical network");
        tablesToCheck.add(networks);

        /*
         * List<String> privateIP = new ArrayList<String>();
         * privateIP.add(0, "op_dc_ip_address_alloc");
         * privateIP.add(1, "data_center_id");
         * privateIP.add(2, "there are private IP addresses allocated for this zone");
         * tablesToCheck.add(privateIP);
         */

        List<String> publicIP = new ArrayList<String>();
        publicIP.add(0, "user_ip_address");
        publicIP.add(1, "physical_network_id");
        publicIP.add(2, "there are public IP addresses allocated for this physical network");
        tablesToCheck.add(publicIP);

        for (List<String> table : tablesToCheck) {
            String tableName = table.get(0);
            String column = table.get(1);
            String errorMsg = table.get(2);

            String dbName = "cloud";

            String selectSql = "SELECT * FROM `" + dbName + "`.`" + tableName + "` WHERE " + column + " = ?";

            if (tableName.equals("networks")) {
                selectSql += " AND removed is NULL";
            }

            if (tableName.equals("op_dc_vnet_alloc")) {
                selectSql += " AND taken IS NOT NULL";
            }

            if (tableName.equals("user_ip_address")) {
                selectSql += " AND state!='Free'";
            }

            if (tableName.equals("op_dc_ip_address_alloc")) {
                selectSql += " AND taken IS NOT NULL";
            }

            TransactionLegacy txn = TransactionLegacy.currentTxn();
            try {
                PreparedStatement stmt = txn.prepareAutoCloseStatement(selectSql);
                stmt.setLong(1, physicalNetworkId);
                ResultSet rs = stmt.executeQuery();
                if (rs != null && rs.next()) {
                    throw new CloudRuntimeException("The Physical Network is not deletable because " + errorMsg);
                }
            } catch (SQLException ex) {
                throw new CloudRuntimeException("The Management Server failed to detect if physical network is deletable. Please contact Cloud Support.");
            }
        }

    }

    @Override
    @DB
    @ActionEvent(eventType = EventTypes.EVENT_GUEST_VLAN_RANGE_DEDICATE, eventDescription = "dedicating guest vlan range", async = false)
    public GuestVlan dedicateGuestVlanRange(DedicateGuestVlanRangeCmd cmd) {
        String vlan = cmd.getVlan();
        String accountName = cmd.getAccountName();
        Long domainId = cmd.getDomainId();
        Long physicalNetworkId = cmd.getPhysicalNetworkId();
        Long projectId = cmd.getProjectId();

        int startVlan, endVlan;
        String updatedVlanRange = null;
        long guestVlanMapId = 0;
        long guestVlanMapAccountId = 0;
        long vlanOwnerId = 0;

        // Verify account is valid
        Account vlanOwner = null;
        if (projectId != null) {
            if (accountName != null) {
                throw new InvalidParameterValueException("accountName and projectId are mutually exclusive");
            }
            Project project = _projectMgr.getProject(projectId);
            if (project == null) {
                throw new InvalidParameterValueException("Unable to find project by id " + projectId);
            }
            vlanOwner = _accountMgr.getAccount(project.getProjectAccountId());
        }

        if ((accountName != null) && (domainId != null)) {
            vlanOwner = _accountDao.findActiveAccount(accountName, domainId);
        }
        if (vlanOwner == null) {
            throw new InvalidParameterValueException("Unable to find account by name " + accountName);
        }
        vlanOwnerId = vlanOwner.getAccountId();

        // Verify physical network isolation type is VLAN
        PhysicalNetworkVO physicalNetwork = _physicalNetworkDao.findById(physicalNetworkId);
        if (physicalNetwork == null) {
            throw new InvalidParameterValueException("Unable to find physical network by id " + physicalNetworkId);
        } else if (!physicalNetwork.getIsolationMethods().isEmpty() && !physicalNetwork.getIsolationMethods().contains("VLAN")) {
            throw new InvalidParameterValueException("Cannot dedicate guest vlan range. " + "Physical isolation type of network " + physicalNetworkId + " is not VLAN");
        }

        // Get the start and end vlan
        String[] vlanRange = vlan.split("-");
        if (vlanRange.length != 2) {
            throw new InvalidParameterValueException("Invalid format for parameter value vlan " + vlan + " .Vlan should be specified as 'startvlan-endvlan'");
        }

        try {
            startVlan = Integer.parseInt(vlanRange[0]);
            endVlan = Integer.parseInt(vlanRange[1]);
        } catch (NumberFormatException e) {
            s_logger.warn("Unable to parse guest vlan range:", e);
            throw new InvalidParameterValueException("Please provide valid guest vlan range");
        }

        // Verify guest vlan range exists in the system
        List<Pair<Integer, Integer>> existingRanges = physicalNetwork.getVnet();
        Boolean exists = false;
        if (!existingRanges.isEmpty()) {
            for (int i = 0; i < existingRanges.size(); i++) {
                int existingStartVlan = existingRanges.get(i).first();
                int existingEndVlan = existingRanges.get(i).second();
                if (startVlan <= endVlan && startVlan >= existingStartVlan && endVlan <= existingEndVlan) {
                    exists = true;
                    break;
                }
            }
            if (!exists) {
                throw new InvalidParameterValueException("Unable to find guest vlan by range " + vlan);
            }
        }

        // Verify guest vlans in the range don't belong to a network of a different account
        for (int i = startVlan; i <= endVlan; i++) {
            List<DataCenterVnetVO> allocatedVlans = _datacneterVnet.listAllocatedVnetsInRange(physicalNetwork.getDataCenterId(), physicalNetwork.getId(), startVlan, endVlan);
            if (allocatedVlans != null && !allocatedVlans.isEmpty()) {
                for (DataCenterVnetVO allocatedVlan : allocatedVlans) {
                    if (allocatedVlan.getAccountId() != vlanOwner.getAccountId()) {
                        throw new InvalidParameterValueException("Guest vlan from this range " + allocatedVlan.getVnet() + " is allocated to a different account."
                                + " Can only dedicate a range which has no allocated vlans or has vlans allocated to the same account ");
                    }
                }
            }
        }

        List<AccountGuestVlanMapVO> guestVlanMaps = _accountGuestVlanMapDao.listAccountGuestVlanMapsByPhysicalNetwork(physicalNetworkId);
        // Verify if vlan range is already dedicated
        for (AccountGuestVlanMapVO guestVlanMap : guestVlanMaps) {
            List<Integer> vlanTokens = getVlanFromRange(guestVlanMap.getGuestVlanRange());
            int dedicatedStartVlan = vlanTokens.get(0).intValue();
            int dedicatedEndVlan = vlanTokens.get(1).intValue();
            if ((startVlan < dedicatedStartVlan & endVlan >= dedicatedStartVlan) || (startVlan >= dedicatedStartVlan & startVlan <= dedicatedEndVlan)) {
                throw new InvalidParameterValueException("Vlan range is already dedicated. Cannot" + " dedicate guest vlan range " + vlan);
            }
        }

        // Sort the existing dedicated vlan ranges
        Collections.sort(guestVlanMaps, new Comparator<AccountGuestVlanMapVO>() {
            @Override
            public int compare(AccountGuestVlanMapVO obj1, AccountGuestVlanMapVO obj2) {
                List<Integer> vlanTokens1 = getVlanFromRange(obj1.getGuestVlanRange());
                List<Integer> vlanTokens2 = getVlanFromRange(obj2.getGuestVlanRange());
                return vlanTokens1.get(0).compareTo(vlanTokens2.get(0));
            }
        });

        // Verify if vlan range extends an already dedicated range
        for (int i = 0; i < guestVlanMaps.size(); i++) {
            guestVlanMapId = guestVlanMaps.get(i).getId();
            guestVlanMapAccountId = guestVlanMaps.get(i).getAccountId();
            List<Integer> vlanTokens1 = getVlanFromRange(guestVlanMaps.get(i).getGuestVlanRange());
            // Range extends a dedicated vlan range to the left
            if (endVlan == (vlanTokens1.get(0).intValue() - 1)) {
                if (guestVlanMapAccountId == vlanOwnerId) {
                    updatedVlanRange = startVlan + "-" + vlanTokens1.get(1).intValue();
                }
                break;
            }
            // Range extends a dedicated vlan range to the right
            if (startVlan == (vlanTokens1.get(1).intValue() + 1) & guestVlanMapAccountId == vlanOwnerId) {
                if (i != (guestVlanMaps.size() - 1)) {
                    List<Integer> vlanTokens2 = getVlanFromRange(guestVlanMaps.get(i + 1).getGuestVlanRange());
                    // Range extends 2 vlan ranges, both to the right and left
                    if (endVlan == (vlanTokens2.get(0).intValue() - 1) && guestVlanMaps.get(i + 1).getAccountId() == vlanOwnerId) {
                        _datacneterVnet.releaseDedicatedGuestVlans(guestVlanMaps.get(i + 1).getId());
                        _accountGuestVlanMapDao.remove(guestVlanMaps.get(i + 1).getId());
                        updatedVlanRange = vlanTokens1.get(0).intValue() + "-" + vlanTokens2.get(1).intValue();
                        break;
                    }
                }
                updatedVlanRange = vlanTokens1.get(0).intValue() + "-" + endVlan;
                break;
            }
        }
        // Dedicate vlan range
        AccountGuestVlanMapVO accountGuestVlanMapVO;
        if (updatedVlanRange != null) {
            accountGuestVlanMapVO = _accountGuestVlanMapDao.findById(guestVlanMapId);
            accountGuestVlanMapVO.setGuestVlanRange(updatedVlanRange);
            _accountGuestVlanMapDao.update(guestVlanMapId, accountGuestVlanMapVO);
        } else {
            accountGuestVlanMapVO = new AccountGuestVlanMapVO(vlanOwner.getAccountId(), physicalNetworkId);
            accountGuestVlanMapVO.setGuestVlanRange(startVlan + "-" + endVlan);
            _accountGuestVlanMapDao.persist(accountGuestVlanMapVO);
        }
        // For every guest vlan set the corresponding account guest vlan map id
        List<Integer> finaVlanTokens = getVlanFromRange(accountGuestVlanMapVO.getGuestVlanRange());
        for (int i = finaVlanTokens.get(0).intValue(); i <= finaVlanTokens.get(1).intValue(); i++) {
            List<DataCenterVnetVO> dataCenterVnet = _datacneterVnet.findVnet(physicalNetwork.getDataCenterId(), physicalNetworkId, Integer.toString(i));
            dataCenterVnet.get(0).setAccountGuestVlanMapId(accountGuestVlanMapVO.getId());
            _datacneterVnet.update(dataCenterVnet.get(0).getId(), dataCenterVnet.get(0));
        }
        return accountGuestVlanMapVO;
    }

    private List<Integer> getVlanFromRange(String vlanRange) {
        // Get the start and end vlan
        String[] vlanTokens = vlanRange.split("-");
        List<Integer> tokens = new ArrayList<Integer>();
        try {
            int startVlan = Integer.parseInt(vlanTokens[0]);
            int endVlan = Integer.parseInt(vlanTokens[1]);
            tokens.add(startVlan);
            tokens.add(endVlan);
        } catch (NumberFormatException e) {
            s_logger.warn("Unable to parse guest vlan range:", e);
            throw new InvalidParameterValueException("Please provide valid guest vlan range");
        }
        return tokens;
    }

    @Override
    public Pair<List<? extends GuestVlan>, Integer> listDedicatedGuestVlanRanges(ListDedicatedGuestVlanRangesCmd cmd) {
        Long id = cmd.getId();
        String accountName = cmd.getAccountName();
        Long domainId = cmd.getDomainId();
        Long projectId = cmd.getProjectId();
        String guestVlanRange = cmd.getGuestVlanRange();
        Long physicalNetworkId = cmd.getPhysicalNetworkId();
        Long zoneId = cmd.getZoneId();

        Long accountId = null;
        if (accountName != null && domainId != null) {
            if (projectId != null) {
                throw new InvalidParameterValueException("Account and projectId can't be specified together");
            }
            Account account = _accountDao.findActiveAccount(accountName, domainId);
            if (account == null) {
                InvalidParameterValueException ex = new InvalidParameterValueException("Unable to find account " + accountName);
                DomainVO domain = ApiDBUtils.findDomainById(domainId);
                String domainUuid = domainId.toString();
                if (domain != null) {
                    domainUuid = domain.getUuid();
                }
                ex.addProxyObject(domainUuid, "domainId");
                throw ex;
            } else {
                accountId = account.getId();
            }
        }

        // set project information
        if (projectId != null) {
            Project project = _projectMgr.getProject(projectId);
            if (project == null) {
                throwInvalidIdException("Unable to find project by id " + projectId, projectId.toString(), "projectId");
            }
            accountId = project.getProjectAccountId();
        }

        SearchBuilder<AccountGuestVlanMapVO> sb = _accountGuestVlanMapDao.createSearchBuilder();
        sb.and("id", sb.entity().getId(), SearchCriteria.Op.EQ);
        sb.and("accountId", sb.entity().getAccountId(), SearchCriteria.Op.EQ);
        sb.and("guestVlanRange", sb.entity().getGuestVlanRange(), SearchCriteria.Op.EQ);
        sb.and("physicalNetworkId", sb.entity().getPhysicalNetworkId(), SearchCriteria.Op.EQ);

        if (zoneId != null) {
            SearchBuilder<PhysicalNetworkVO> physicalnetworkSearch = _physicalNetworkDao.createSearchBuilder();
            physicalnetworkSearch.and("zoneId", physicalnetworkSearch.entity().getDataCenterId(), SearchCriteria.Op.EQ);
            sb.join("physicalnetworkSearch", physicalnetworkSearch, sb.entity().getPhysicalNetworkId(), physicalnetworkSearch.entity().getId(), JoinBuilder.JoinType.INNER);
        }

        SearchCriteria<AccountGuestVlanMapVO> sc = sb.create();
        if (id != null) {
            sc.setParameters("id", id);
        }

        if (accountId != null) {
            sc.setParameters("accountId", accountId);
        }

        if (guestVlanRange != null) {
            sc.setParameters("guestVlanRange", guestVlanRange);
        }

        if (physicalNetworkId != null) {
            sc.setParameters("physicalNetworkId", physicalNetworkId);
        }

        if (zoneId != null) {
            sc.setJoinParameters("physicalnetworkSearch", "zoneId", zoneId);
        }

        Filter searchFilter = new Filter(AccountGuestVlanMapVO.class, "id", true, cmd.getStartIndex(), cmd.getPageSizeVal());
        Pair<List<AccountGuestVlanMapVO>, Integer> result = _accountGuestVlanMapDao.searchAndCount(sc, searchFilter);
        return new Pair<List<? extends GuestVlan>, Integer>(result.first(), result.second());
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_DEDICATED_GUEST_VLAN_RANGE_RELEASE, eventDescription = "releasing" + " dedicated guest vlan range", async = true)
    @DB
    public boolean releaseDedicatedGuestVlanRange(Long dedicatedGuestVlanRangeId) {
        // Verify dedicated range exists
        AccountGuestVlanMapVO dedicatedGuestVlan = _accountGuestVlanMapDao.findById(dedicatedGuestVlanRangeId);
        if (dedicatedGuestVlan == null) {
            throw new InvalidParameterValueException("Dedicated guest vlan with specified" + " id doesn't exist in the system");
        }

        // Remove dedication for the guest vlan
        _datacneterVnet.releaseDedicatedGuestVlans(dedicatedGuestVlan.getId());
        if (_accountGuestVlanMapDao.remove(dedicatedGuestVlanRangeId)) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public List<? extends Service> listNetworkServices(String providerName) {

        Provider provider = null;
        if (providerName != null) {
            provider = Network.Provider.getProvider(providerName);
            if (provider == null) {
                throw new InvalidParameterValueException("Invalid Network Service Provider=" + providerName);
            }
        }

        if (provider != null) {
            NetworkElement element = _networkModel.getElementImplementingProvider(providerName);
            if (element == null) {
                throw new InvalidParameterValueException("Unable to find the Network Element implementing the Service Provider '" + providerName + "'");
            }
            return new ArrayList<Service>(element.getCapabilities().keySet());
        } else {
            return Service.listAllServices();
        }
    }

    @Override
    @DB
    @ActionEvent(eventType = EventTypes.EVENT_SERVICE_PROVIDER_CREATE, eventDescription = "Creating Physical Network ServiceProvider", create = true)
    public PhysicalNetworkServiceProvider addProviderToPhysicalNetwork(Long physicalNetworkId, String providerName, Long destinationPhysicalNetworkId, List<String> enabledServices) {

        // verify input parameters
        PhysicalNetworkVO network = _physicalNetworkDao.findById(physicalNetworkId);
        if (network == null) {
            throwInvalidIdException("Physical Network with specified id doesn't exist in the system", physicalNetworkId.toString(), "physicalNetworkId");
        }

        // verify input parameters
        if (destinationPhysicalNetworkId != null) {
            PhysicalNetworkVO destNetwork = _physicalNetworkDao.findById(destinationPhysicalNetworkId);
            if (destNetwork == null) {
                throwInvalidIdException("Destination Physical Network with specified id doesn't exist in the system", destinationPhysicalNetworkId.toString(), "destinationPhysicalNetworkId");
            }
        }

        if (providerName != null) {
            Provider provider = Network.Provider.getProvider(providerName);
            if (provider == null) {
                throw new InvalidParameterValueException("Invalid Network Service Provider=" + providerName);
            }
        }

        if (_pNSPDao.findByServiceProvider(physicalNetworkId, providerName) != null) {
            // TBD: send uuid instead of physicalNetworkId.
            throw new CloudRuntimeException("The '" + providerName + "' provider already exists on physical network : " + physicalNetworkId);
        }

        // check if services can be turned off
        NetworkElement element = _networkModel.getElementImplementingProvider(providerName);
        if (element == null) {
            throw new InvalidParameterValueException("Unable to find the Network Element implementing the Service Provider '" + providerName + "'");
        }
        List<Service> services = new ArrayList<Service>();

        if (enabledServices != null) {
            if (!element.canEnableIndividualServices()) {
                if (enabledServices.size() != element.getCapabilities().keySet().size()) {
                    throw new InvalidParameterValueException("Cannot enable subset of Services, Please specify the complete list of Services for this Service Provider '" + providerName + "'");
                }
            }

            // validate Services
            boolean addGatewayService = false;
            for (String serviceName : enabledServices) {
                Network.Service service = Network.Service.getService(serviceName);
                if (service == null || service == Service.Gateway) {
                    throw new InvalidParameterValueException("Invalid Network Service specified=" + serviceName);
                } else if (service == Service.SourceNat) {
                    addGatewayService = true;
                }

                // check if the service is provided by this Provider
                if (!element.getCapabilities().containsKey(service)) {
                    throw new InvalidParameterValueException(providerName + " Provider cannot provide this Service specified=" + serviceName);
                }
                services.add(service);
            }

            if (addGatewayService) {
                services.add(Service.Gateway);
            }
        } else {
            // enable all the default services supported by this element.
            services = new ArrayList<Service>(element.getCapabilities().keySet());
        }

        try {
            // Create the new physical network in the database
            PhysicalNetworkServiceProviderVO nsp = new PhysicalNetworkServiceProviderVO(physicalNetworkId, providerName);
            // set enabled services
            nsp.setEnabledServices(services);

            if (destinationPhysicalNetworkId != null) {
                nsp.setDestinationPhysicalNetworkId(destinationPhysicalNetworkId);
            }
            nsp = _pNSPDao.persist(nsp);

            return nsp;
        } catch (Exception ex) {
            s_logger.warn("Exception: ", ex);
            throw new CloudRuntimeException("Fail to add a provider to physical network");
        }

    }

    @Override
    public Pair<List<? extends PhysicalNetworkServiceProvider>, Integer> listNetworkServiceProviders(Long physicalNetworkId, String name, String state, Long startIndex, Long pageSize) {

        Filter searchFilter = new Filter(PhysicalNetworkServiceProviderVO.class, "id", false, startIndex, pageSize);
        SearchBuilder<PhysicalNetworkServiceProviderVO> sb = _pNSPDao.createSearchBuilder();
        SearchCriteria<PhysicalNetworkServiceProviderVO> sc = sb.create();

        if (physicalNetworkId != null) {
            sc.addAnd("physicalNetworkId", Op.EQ, physicalNetworkId);
        }

        if (name != null) {
            sc.addAnd("providerName", Op.EQ, name);
        }

        if (state != null) {
            sc.addAnd("state", Op.EQ, state);
        }

        Pair<List<PhysicalNetworkServiceProviderVO>, Integer> result = _pNSPDao.searchAndCount(sc, searchFilter);
        return new Pair<List<? extends PhysicalNetworkServiceProvider>, Integer>(result.first(), result.second());
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_SERVICE_PROVIDER_UPDATE, eventDescription = "Updating physical network ServiceProvider", async = true)
    public PhysicalNetworkServiceProvider updateNetworkServiceProvider(Long id, String stateStr, List<String> enabledServices) {

        PhysicalNetworkServiceProviderVO provider = _pNSPDao.findById(id);
        if (provider == null) {
            throw new InvalidParameterValueException("Network Service Provider id=" + id + "doesn't exist in the system");
        }

        NetworkElement element = _networkModel.getElementImplementingProvider(provider.getProviderName());
        if (element == null) {
            throw new InvalidParameterValueException("Unable to find the Network Element implementing the Service Provider '" + provider.getProviderName() + "'");
        }

        PhysicalNetworkServiceProvider.State state = null;
        if (stateStr != null && !stateStr.isEmpty()) {
            try {
                state = PhysicalNetworkServiceProvider.State.valueOf(stateStr);
            } catch (IllegalArgumentException ex) {
                throw new InvalidParameterValueException("Unable to resolve state '" + stateStr + "' to a supported value {Enabled or Disabled}");
            }
        }

        boolean update = false;

        if (state != null) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("trying to update the state of the service provider id=" + id + " on physical network: " + provider.getPhysicalNetworkId() + " to state: " + stateStr);
            }
            switch (state) {
            case Enabled:
                if (element != null && element.isReady(provider)) {
                    provider.setState(PhysicalNetworkServiceProvider.State.Enabled);
                    update = true;
                } else {
                    throw new CloudRuntimeException("Provider is not ready, cannot Enable the provider, please configure the provider first");
                }
                break;
            case Disabled:
                // do we need to do anything for the provider instances before disabling?
                provider.setState(PhysicalNetworkServiceProvider.State.Disabled);
                update = true;
                break;
            case Shutdown:
                throw new InvalidParameterValueException("Updating the provider state to 'Shutdown' is not supported");
            }
        }

        if (enabledServices != null) {
            // check if services can be turned of
            if (!element.canEnableIndividualServices()) {
                throw new InvalidParameterValueException("Cannot update set of Services for this Service Provider '" + provider.getProviderName() + "'");
            }

            // validate Services
            List<Service> services = new ArrayList<Service>();
            for (String serviceName : enabledServices) {
                Network.Service service = Network.Service.getService(serviceName);
                if (service == null) {
                    throw new InvalidParameterValueException("Invalid Network Service specified=" + serviceName);
                }
                services.add(service);
            }
            // set enabled services
            provider.setEnabledServices(services);
            update = true;
        }

        if (update) {
            _pNSPDao.update(id, provider);
        }
        return provider;
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_SERVICE_PROVIDER_DELETE, eventDescription = "Deleting physical network ServiceProvider", async = true)
    public boolean deleteNetworkServiceProvider(Long id) throws ConcurrentOperationException, ResourceUnavailableException {
        PhysicalNetworkServiceProviderVO provider = _pNSPDao.findById(id);

        if (provider == null) {
            throw new InvalidParameterValueException("Network Service Provider id=" + id + "doesn't exist in the system");
        }

        // check if there are networks using this provider
        List<NetworkVO> networks = _networksDao.listByPhysicalNetworkAndProvider(provider.getPhysicalNetworkId(), provider.getProviderName());
        if (networks != null && !networks.isEmpty()) {
            throw new CloudRuntimeException("Provider is not deletable because there are active networks using this provider, please upgrade these networks to new network offerings");
        }

        User callerUser = _accountMgr.getActiveUser(CallContext.current().getCallingUserId());
        Account callerAccount = _accountMgr.getActiveAccountById(callerUser.getAccountId());
        // shutdown the provider instances
        ReservationContext context = new ReservationContextImpl(null, null, callerUser, callerAccount);
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Shutting down the service provider id=" + id + " on physical network: " + provider.getPhysicalNetworkId());
        }
        NetworkElement element = _networkModel.getElementImplementingProvider(provider.getProviderName());
        if (element == null) {
            throw new InvalidParameterValueException("Unable to find the Network Element implementing the Service Provider '" + provider.getProviderName() + "'");
        }

        if (element != null && element.shutdownProviderInstances(provider, context)) {
            provider.setState(PhysicalNetworkServiceProvider.State.Shutdown);
        }

        return _pNSPDao.remove(id);
    }

    @Override
    public PhysicalNetwork getPhysicalNetwork(Long physicalNetworkId) {
        return _physicalNetworkDao.findById(physicalNetworkId);
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_PHYSICAL_NETWORK_CREATE, eventDescription = "Creating Physical Network", async = true)
    public PhysicalNetwork getCreatedPhysicalNetwork(Long physicalNetworkId) {
        return getPhysicalNetwork(physicalNetworkId);
    }

    @Override
    public PhysicalNetworkServiceProvider getPhysicalNetworkServiceProvider(Long providerId) {
        return _pNSPDao.findById(providerId);
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_SERVICE_PROVIDER_CREATE, eventDescription = "Creating Physical Network ServiceProvider", async = true)
    public PhysicalNetworkServiceProvider getCreatedPhysicalNetworkServiceProvider(Long providerId) {
        return getPhysicalNetworkServiceProvider(providerId);
    }

    @Override
    public long findPhysicalNetworkId(long zoneId, String tag, TrafficType trafficType) {
        List<PhysicalNetworkVO> pNtwks = new ArrayList<PhysicalNetworkVO>();
        if (trafficType != null) {
            pNtwks = _physicalNetworkDao.listByZoneAndTrafficType(zoneId, trafficType);
        } else {
            pNtwks = _physicalNetworkDao.listByZone(zoneId);
        }

        if (pNtwks.isEmpty()) {
            throw new InvalidParameterValueException("Unable to find physical network in zone id=" + zoneId);
        }

        if (pNtwks.size() > 1) {
            if (tag == null) {
                throw new InvalidParameterValueException("More than one physical networks exist in zone id=" + zoneId + " and no tags are specified in order to make a choice");
            }

            Long pNtwkId = null;
            for (PhysicalNetwork pNtwk : pNtwks) {
                if (pNtwk.getTags().contains(tag)) {
                    s_logger.debug("Found physical network id=" + pNtwk.getId() + " based on requested tags " + tag);
                    pNtwkId = pNtwk.getId();
                    break;
                }
            }
            if (pNtwkId == null) {
                throw new InvalidParameterValueException("Unable to find physical network which match the tags " + tag);
            }
            return pNtwkId;
        } else {
            return pNtwks.get(0).getId();
        }
    }

    @Override
    @DB
    @ActionEvent(eventType = EventTypes.EVENT_TRAFFIC_TYPE_CREATE, eventDescription = "Creating Physical Network TrafficType", create = true)
    public PhysicalNetworkTrafficType addTrafficTypeToPhysicalNetwork(Long physicalNetworkId, String trafficTypeStr, String isolationMethod, String xenLabel, String kvmLabel, String vmwareLabel,
            String simulatorLabel, String vlan, String hypervLabel, String ovm3Label) {

        // verify input parameters
        PhysicalNetworkVO network = _physicalNetworkDao.findById(physicalNetworkId);
        if (network == null) {
            throw new InvalidParameterValueException("Physical Network id=" + physicalNetworkId + "doesn't exist in the system");
        }

        Networks.TrafficType trafficType = null;
        if (trafficTypeStr != null && !trafficTypeStr.isEmpty()) {
            try {
                trafficType = Networks.TrafficType.valueOf(trafficTypeStr);
            } catch (IllegalArgumentException ex) {
                throw new InvalidParameterValueException("Unable to resolve trafficType '" + trafficTypeStr + "' to a supported value");
            }
        }

        if (_pNTrafficTypeDao.isTrafficTypeSupported(physicalNetworkId, trafficType)) {
            throw new CloudRuntimeException("This physical network already supports the traffic type: " + trafficType);
        }
        // For Storage, Control, Management, Public check if the zone has any other physical network with this
        // traffictype already present
        // If yes, we cant add these traffics to one more physical network in the zone.

        if (TrafficType.isSystemNetwork(trafficType) || TrafficType.Public.equals(trafficType) || TrafficType.Storage.equals(trafficType)) {
            if (!_physicalNetworkDao.listByZoneAndTrafficType(network.getDataCenterId(), trafficType).isEmpty()) {
                throw new CloudRuntimeException("Fail to add the traffic type to physical network because Zone already has a physical network with this traffic type: " + trafficType);
            }
        }

        if (TrafficType.Storage.equals(trafficType)) {
            List<SecondaryStorageVmVO> ssvms = _stnwMgr.getSSVMWithNoStorageNetwork(network.getDataCenterId());
            if (!ssvms.isEmpty()) {
                StringBuilder sb = new StringBuilder("Cannot add " + trafficType
                        + " traffic type as there are below secondary storage vm still running. Please stop them all and add Storage traffic type again, then destory them all to allow CloudStack recreate them with storage network(If you have added storage network ip range)");
                sb.append("SSVMs:");
                for (SecondaryStorageVmVO ssvm : ssvms) {
                    sb.append(ssvm.getInstanceName()).append(":").append(ssvm.getState());
                }
                throw new CloudRuntimeException(sb.toString());
            }
        }

        try {
            // Create the new traffic type in the database
            if (xenLabel == null) {
                xenLabel = getDefaultXenNetworkLabel(trafficType);
            }
            PhysicalNetworkTrafficTypeVO pNetworktrafficType = new PhysicalNetworkTrafficTypeVO(physicalNetworkId, trafficType, xenLabel, kvmLabel, vmwareLabel, simulatorLabel, vlan, hypervLabel,
                    ovm3Label);
            pNetworktrafficType = _pNTrafficTypeDao.persist(pNetworktrafficType);

            // For public traffic, get isolation method of physical network and update the public network accordingly
            // each broadcast type will individually need to be qualified for support of public traffic
            if (TrafficType.Public.equals(trafficType)) {
                List<String> isolationMethods = network.getIsolationMethods();
                if ((isolationMethods.size() == 1 && isolationMethods.get(0).toLowerCase().equals("vxlan"))
                        || (isolationMethod != null && isolationMethods.contains(isolationMethod) && isolationMethod.toLowerCase().equals("vxlan"))) {
                    // find row in networks table that is defined as 'Public', created when zone was deployed
                    NetworkVO publicNetwork = _networksDao.listByZoneAndTrafficType(network.getDataCenterId(), TrafficType.Public).get(0);
                    if (publicNetwork != null) {
                        s_logger.debug("setting public network " + publicNetwork + " to broadcast type vxlan");
                        publicNetwork.setBroadcastDomainType(BroadcastDomainType.Vxlan);
                        _networksDao.persist(publicNetwork);
                    }
                }
            }

            return pNetworktrafficType;
        } catch (Exception ex) {
            s_logger.warn("Exception: ", ex);
            throw new CloudRuntimeException("Fail to add a traffic type to physical network");
        }

    }

    private String getDefaultXenNetworkLabel(TrafficType trafficType) {
        String xenLabel = null;
        switch (trafficType) {
        case Public:
            xenLabel = _configDao.getValue(Config.XenServerPublicNetwork.key());
            break;
        case Guest:
            xenLabel = _configDao.getValue(Config.XenServerGuestNetwork.key());
            break;
        case Storage:
            xenLabel = _configDao.getValue(Config.XenServerStorageNetwork1.key());
            break;
        case Management:
            xenLabel = _configDao.getValue(Config.XenServerPrivateNetwork.key());
            break;
        case Control:
            xenLabel = "cloud_link_local_network";
            break;
        case Vpn:
        case None:
            break;
        }
        return xenLabel;
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_TRAFFIC_TYPE_CREATE, eventDescription = "Creating Physical Network TrafficType", async = true)
    public PhysicalNetworkTrafficType getPhysicalNetworkTrafficType(Long id) {
        return _pNTrafficTypeDao.findById(id);
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_TRAFFIC_TYPE_UPDATE, eventDescription = "Updating physical network TrafficType", async = true)
    public PhysicalNetworkTrafficType updatePhysicalNetworkTrafficType(Long id, String xenLabel, String kvmLabel, String vmwareLabel, String hypervLabel, String ovm3Label) {

        PhysicalNetworkTrafficTypeVO trafficType = _pNTrafficTypeDao.findById(id);

        if (trafficType == null) {
            throw new InvalidParameterValueException("Traffic Type with id=" + id + "doesn't exist in the system");
        }

        if (xenLabel != null) {
            if ("".equals(xenLabel)) {
                xenLabel = null;
            }
            trafficType.setXenNetworkLabel(xenLabel);
        }
        if (kvmLabel != null) {
            if ("".equals(kvmLabel)) {
                kvmLabel = null;
            }
            trafficType.setKvmNetworkLabel(kvmLabel);
        }
        if (vmwareLabel != null) {
            if ("".equals(vmwareLabel)) {
                vmwareLabel = null;
            }
            trafficType.setVmwareNetworkLabel(vmwareLabel);
        }

        if (hypervLabel != null) {
            if ("".equals(hypervLabel)) {
                hypervLabel = null;
            }
            trafficType.setHypervNetworkLabel(hypervLabel);
        }

        if (ovm3Label != null) {
            if ("".equals(ovm3Label)) {
                ovm3Label = null;
            }
            trafficType.setOvm3NetworkLabel(ovm3Label);
        }
        _pNTrafficTypeDao.update(id, trafficType);
        return trafficType;
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_TRAFFIC_TYPE_DELETE, eventDescription = "Deleting physical network TrafficType", async = true)
    public boolean deletePhysicalNetworkTrafficType(Long id) {
        PhysicalNetworkTrafficTypeVO trafficType = _pNTrafficTypeDao.findById(id);

        if (trafficType == null) {
            throw new InvalidParameterValueException("Traffic Type with id=" + id + "doesn't exist in the system");
        }

        // check if there are any networks associated to this physical network with this traffic type
        if (TrafficType.Guest.equals(trafficType.getTrafficType())) {
            if (!_networksDao.listByPhysicalNetworkTrafficType(trafficType.getPhysicalNetworkId(), trafficType.getTrafficType()).isEmpty()) {
                throw new CloudRuntimeException("The Traffic Type is not deletable because there are existing networks with this traffic type:" + trafficType.getTrafficType());
            }
        } else if (TrafficType.Storage.equals(trafficType.getTrafficType())) {
            PhysicalNetworkVO pn = _physicalNetworkDao.findById(trafficType.getPhysicalNetworkId());
            if (_stnwMgr.isAnyStorageIpInUseInZone(pn.getDataCenterId())) {
                throw new CloudRuntimeException("The Traffic Type is not deletable because there are still some storage network ip addresses in use:" + trafficType.getTrafficType());
            }
        }
        return _pNTrafficTypeDao.remove(id);
    }

    @Override
    public Pair<List<? extends PhysicalNetworkTrafficType>, Integer> listTrafficTypes(Long physicalNetworkId) {
        PhysicalNetworkVO network = _physicalNetworkDao.findById(physicalNetworkId);
        if (network == null) {
            throwInvalidIdException("Physical Network with specified id doesn't exist in the system", physicalNetworkId.toString(), "physicalNetworkId");
        }

        Pair<List<PhysicalNetworkTrafficTypeVO>, Integer> result = _pNTrafficTypeDao.listAndCountBy(physicalNetworkId);
        return new Pair<List<? extends PhysicalNetworkTrafficType>, Integer>(result.first(), result.second());
    }

    @Override
    //TODO: duplicated in NetworkModel
    public NetworkVO getExclusiveGuestNetwork(long zoneId) {
        List<NetworkVO> networks = _networksDao.listBy(Account.ACCOUNT_ID_SYSTEM, zoneId, GuestType.Shared, TrafficType.Guest);
        if (networks == null || networks.isEmpty()) {
            throw new InvalidParameterValueException("Unable to find network with trafficType " + TrafficType.Guest + " and guestType " + GuestType.Shared + " in zone " + zoneId);
        }

        if (networks.size() > 1) {
            throw new InvalidParameterValueException("Found more than 1 network with trafficType " + TrafficType.Guest + " and guestType " + GuestType.Shared + " in zone " + zoneId);

        }

        return networks.get(0);
    }

    protected PhysicalNetworkServiceProvider addDefaultVirtualRouterToPhysicalNetwork(long physicalNetworkId) {

        PhysicalNetworkServiceProvider nsp = addProviderToPhysicalNetwork(physicalNetworkId, Network.Provider.VirtualRouter.getName(), null, null);
        // add instance of the provider
        NetworkElement networkElement = _networkModel.getElementImplementingProvider(Network.Provider.VirtualRouter.getName());
        if (networkElement == null) {
            throw new CloudRuntimeException("Unable to find the Network Element implementing the VirtualRouter Provider");
        }

        VirtualRouterElement element = (VirtualRouterElement)networkElement;
        element.addElement(nsp.getId(), Type.VirtualRouter);

        return nsp;
    }

    private PhysicalNetworkServiceProvider addDefaultOvsToPhysicalNetwork(long physicalNetworkId) {
        PhysicalNetworkServiceProvider nsp = addProviderToPhysicalNetwork(physicalNetworkId, Network.Provider.Ovs.getName(), null, null);
        NetworkElement networkElement = _networkModel.getElementImplementingProvider(Network.Provider.Ovs.getName());
        if (networkElement == null) {
            throw new CloudRuntimeException("Unable to find the Network Element implementing the Ovs Provider");
        }
        OvsProviderVO element = _ovsProviderDao.findByNspId(nsp.getId());
        if (element != null) {
            s_logger.debug("There is already a Ovs element with service provider id " + nsp.getId());
            return nsp;
        }
        element = new OvsProviderVO(nsp.getId());
        _ovsProviderDao.persist(element);
        return nsp;
    }

    protected PhysicalNetworkServiceProvider addDefaultVpcVirtualRouterToPhysicalNetwork(long physicalNetworkId) {

        PhysicalNetworkServiceProvider nsp = addProviderToPhysicalNetwork(physicalNetworkId, Network.Provider.VPCVirtualRouter.getName(), null, null);

        NetworkElement networkElement = _networkModel.getElementImplementingProvider(Network.Provider.VPCVirtualRouter.getName());
        if (networkElement == null) {
            throw new CloudRuntimeException("Unable to find the Network Element implementing the VPCVirtualRouter Provider");
        }

        VpcVirtualRouterElement element = (VpcVirtualRouterElement)networkElement;
        element.addElement(nsp.getId(), Type.VPCVirtualRouter);

        return nsp;
    }

    protected PhysicalNetworkServiceProvider addDefaultInternalLbProviderToPhysicalNetwork(long physicalNetworkId) {

        PhysicalNetworkServiceProvider nsp = addProviderToPhysicalNetwork(physicalNetworkId, Network.Provider.InternalLbVm.getName(), null, null);

        NetworkElement networkElement = _networkModel.getElementImplementingProvider(Network.Provider.InternalLbVm.getName());
        if (networkElement == null) {
            throw new CloudRuntimeException("Unable to find the Network Element implementing the " + Network.Provider.InternalLbVm.getName() + " Provider");
        }

        _internalLbElementSvc.addInternalLoadBalancerElement(nsp.getId());

        return nsp;
    }

    protected PhysicalNetworkServiceProvider addDefaultSecurityGroupProviderToPhysicalNetwork(long physicalNetworkId) {

        PhysicalNetworkServiceProvider nsp = addProviderToPhysicalNetwork(physicalNetworkId, Network.Provider.SecurityGroupProvider.getName(), null, null);

        return nsp;
    }

    private PhysicalNetworkServiceProvider addDefaultBaremetalProvidersToPhysicalNetwork(long physicalNetworkId) {
        PhysicalNetworkVO pvo = _physicalNetworkDao.findById(physicalNetworkId);
        DataCenterVO dvo = _dcDao.findById(pvo.getDataCenterId());
        if (dvo.getNetworkType() == NetworkType.Basic) {

            Provider provider = Network.Provider.getProvider("BaremetalDhcpProvider");
            if (provider == null) {
                // baremetal is not loaded
                return null;
            }

            addProviderToPhysicalNetwork(physicalNetworkId, "BaremetalDhcpProvider", null, null);
            addProviderToPhysicalNetwork(physicalNetworkId, "BaremetalPxeProvider", null, null);
            addProviderToPhysicalNetwork(physicalNetworkId, "BaremetalUserdataProvider", null, null);
        } else if (dvo.getNetworkType() == NetworkType.Advanced) {
            addProviderToPhysicalNetwork(physicalNetworkId, "BaremetalPxeProvider", null, null);
            enableProvider("BaremetalPxeProvider");
        }

        return null;
    }

    private void enableProvider(String providerName) {
        QueryBuilder<PhysicalNetworkServiceProviderVO> q = QueryBuilder.create(PhysicalNetworkServiceProviderVO.class);
        q.and(q.entity().getProviderName(), SearchCriteria.Op.EQ, providerName);
        PhysicalNetworkServiceProviderVO provider = q.find();
        provider.setState(PhysicalNetworkServiceProvider.State.Enabled);
        _pNSPDao.update(provider.getId(), provider);
    }

    private PhysicalNetworkServiceProvider addConfigDriveToPhysicalNetwork(long physicalNetworkId) {
        PhysicalNetworkVO pvo = _physicalNetworkDao.findById(physicalNetworkId);
        DataCenterVO dvo = _dcDao.findById(pvo.getDataCenterId());
        if (dvo.getNetworkType() == NetworkType.Advanced) {

            Provider provider = Network.Provider.getProvider("ConfigDrive");
            if (provider == null) {
                return null;
            }

            addProviderToPhysicalNetwork(physicalNetworkId, Provider.ConfigDrive.getName(), null, null);
            enableProvider(Provider.ConfigDrive.getName());
        }
        return null;

    }

    protected boolean isNetworkSystem(Network network) {
        NetworkOffering no = _networkOfferingDao.findByIdIncludingRemoved(network.getNetworkOfferingId());
        if (no.isSystemOnly()) {
            return true;
        } else {
            return false;
        }
    }

    private boolean getAllowSubdomainAccessGlobal() {
        return _allowSubdomainNetworkAccess;
    }

    @Override
    public List<Pair<TrafficType, String>> listTrafficTypeImplementor(ListTrafficTypeImplementorsCmd cmd) {
        String type = cmd.getTrafficType();
        List<Pair<TrafficType, String>> results = new ArrayList<Pair<TrafficType, String>>();
        if (type != null) {
            for (NetworkGuru guru : _networkGurus) {
                if (guru.isMyTrafficType(TrafficType.getTrafficType(type))) {
                    results.add(new Pair<TrafficType, String>(TrafficType.getTrafficType(type), guru.getName()));
                    break;
                }
            }
        } else {
            for (NetworkGuru guru : _networkGurus) {
                TrafficType[] allTypes = guru.getSupportedTrafficType();
                for (TrafficType t : allTypes) {
                    results.add(new Pair<TrafficType, String>(t, guru.getName()));
                }
            }
        }

        return results;
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_NET_IP_ASSIGN, eventDescription = "associating Ip", async = true)
    public IpAddress associateIPToNetwork(long ipId, long networkId)
            throws InsufficientAddressCapacityException, ResourceAllocationException, ResourceUnavailableException, ConcurrentOperationException {

        Network network = _networksDao.findById(networkId);
        if (network == null) {
            // release the acquired IP addrress before throwing the exception
            // else it will always be in allocating state
            releaseIpAddress(ipId);
            throw new InvalidParameterValueException("Invalid network id is given");
        }

        if (network.getVpcId() != null) {
            // release the acquired IP addrress before throwing the exception
            // else it will always be in allocating state
            releaseIpAddress(ipId);
            throw new InvalidParameterValueException("Can't assign ip to the network directly when network belongs" + " to VPC.Specify vpcId to associate ip address to VPC");
        }
        return _ipAddrMgr.associateIPToGuestNetwork(ipId, networkId, true);

    }

    @Override
    @DB
    public Network createPrivateNetwork(final String networkName, final String displayText, long physicalNetworkId, String broadcastUriString, final String startIp, String endIp, final String gateway,
            String netmask, final long networkOwnerId, final Long vpcId, final Boolean sourceNat, final Long networkOfferingId)
                    throws ResourceAllocationException, ConcurrentOperationException, InsufficientCapacityException {

        final Account owner = _accountMgr.getAccount(networkOwnerId);

        // Get system network offering
        NetworkOfferingVO ntwkOff = null;
        if (networkOfferingId != null) {
            ntwkOff = _networkOfferingDao.findById(networkOfferingId);
        }
        if (ntwkOff == null) {
            ntwkOff = findSystemNetworkOffering(NetworkOffering.SystemPrivateGatewayNetworkOffering);
        }

        // Validate physical network
        final PhysicalNetwork pNtwk = _physicalNetworkDao.findById(physicalNetworkId);
        if (pNtwk == null) {
            throwInvalidIdException("Unable to find a physical network" + " having the given id", String.valueOf(physicalNetworkId), "physicalNetworkId");
        }

        // VALIDATE IP INFO
        // if end ip is not specified, default it to startIp
        if (!NetUtils.isValidIp4(startIp)) {
            throw new InvalidParameterValueException("Invalid format for the ip address parameter");
        }
        if (endIp == null) {
            endIp = startIp;
        } else if (!NetUtils.isValidIp4(endIp)) {
            throw new InvalidParameterValueException("Invalid format for the endIp address parameter");
        }

        if (!NetUtils.isValidIp4(gateway)) {
            throw new InvalidParameterValueException("Invalid gateway");
        }
        if (!NetUtils.isValidIp4Netmask(netmask)) {
            throw new InvalidParameterValueException("Invalid netmask");
        }

        final String cidr = NetUtils.ipAndNetMaskToCidr(gateway, netmask);

        URI uri = BroadcastDomainType.fromString(broadcastUriString);
        final String uriString = uri.toString();
        BroadcastDomainType tiep = BroadcastDomainType.getSchemeValue(uri);
        // numeric vlan or vlan uri are ok for now
        // TODO make a test for any supported scheme
        if (!(tiep == BroadcastDomainType.Vlan || tiep == BroadcastDomainType.Lswitch)) {
            throw new InvalidParameterValueException("unsupported type of broadcastUri specified: " + broadcastUriString);
        }

        final NetworkOfferingVO ntwkOffFinal = ntwkOff;
        try {
            return Transaction.execute(new TransactionCallbackWithException<Network, Exception>() {
                @Override
                public Network doInTransaction(TransactionStatus status) throws ResourceAllocationException, InsufficientCapacityException {
                    //lock datacenter as we need to get mac address seq from there
                    DataCenterVO dc = _dcDao.lockRow(pNtwk.getDataCenterId(), true);

                    //check if we need to create guest network
                    Network privateNetwork = _networksDao.getPrivateNetwork(uriString, cidr, networkOwnerId, pNtwk.getDataCenterId(), networkOfferingId);
                    if (privateNetwork == null) {
                        //create Guest network
                        privateNetwork = _networkMgr.createGuestNetwork(ntwkOffFinal.getId(), networkName, displayText, gateway, cidr, uriString, false, null, owner, null, pNtwk,
                                pNtwk.getDataCenterId(), ACLType.Account, null, vpcId, null, null, true, null, null);
                        if (privateNetwork != null) {
                            s_logger.debug("Successfully created guest network " + privateNetwork);
                        } else {
                            throw new CloudRuntimeException("Creating guest network failed");
                        }
                    } else {
                        s_logger.debug("Private network already exists: " + privateNetwork);
                        //Do not allow multiple private gateways with same Vlan within a VPC
                        if (vpcId != null && vpcId.equals(privateNetwork.getVpcId())) {
                            throw new InvalidParameterValueException("Private network for the vlan: " + uriString + " and cidr  " + cidr + "  already exists " + "for Vpc " + vpcId + " in zone "
                                    + _entityMgr.findById(DataCenter.class, pNtwk.getDataCenterId()).getName());
                        }
                    }
                    if (vpcId != null) {
                        //add entry to private_ip_address table
                        PrivateIpVO privateIp = _privateIpDao.findByIpAndSourceNetworkIdAndVpcId(privateNetwork.getId(), startIp, vpcId);
                        if (privateIp != null) {
                            throw new InvalidParameterValueException(
                                    "Private ip address " + startIp + " already used for private gateway" + " in zone " + _entityMgr.findById(DataCenter.class, pNtwk.getDataCenterId()).getName());
                        }
                        Long mac = dc.getMacAddress();
                        Long nextMac = mac + 1;
                        dc.setMacAddress(nextMac);
                        privateIp = new PrivateIpVO(startIp, privateNetwork.getId(), nextMac, vpcId, sourceNat);
                        _privateIpDao.persist(privateIp);
                        _dcDao.update(dc.getId(), dc);
                    }

                    s_logger.debug("Private network " + privateNetwork + " is created");

                    return privateNetwork;
                }
            });
        } catch (Exception e) {
            ExceptionUtil.rethrowRuntime(e);
            ExceptionUtil.rethrow(e, ResourceAllocationException.class);
            ExceptionUtil.rethrow(e, InsufficientCapacityException.class);
            throw new IllegalStateException(e);
        }
    }

    private NetworkOfferingVO findSystemNetworkOffering(String offeringName) {
        List<NetworkOfferingVO> allOfferings = _networkOfferingDao.listSystemNetworkOfferings();
        for (NetworkOfferingVO offer : allOfferings) {
            if (offer.getName().equals(offeringName)) {
                return offer;
            }
        }
        return null;
    }

    @Override
    public Network getNetwork(String networkUuid) {
        return _networksDao.findByUuid(networkUuid);
    }

    @Override
    public List<? extends Nic> listNics(ListNicsCmd cmd) {
        Account caller = CallContext.current().getCallingAccount();
        Long nicId = cmd.getNicId();
        long vmId = cmd.getVmId();
        String keyword = cmd.getKeyword();
        Long networkId = cmd.getNetworkId();
        UserVmVO userVm = _userVmDao.findById(vmId);

        if (userVm == null || (!userVm.isDisplayVm() && caller.getType() == Account.ACCOUNT_TYPE_NORMAL)) {
            throwInvalidIdException("Virtual machine id does not exist", Long.valueOf(vmId).toString(), "vmId");
        }

        _accountMgr.checkAccess(caller, null, true, userVm);
        return _networkMgr.listVmNics(vmId, nicId, networkId, keyword);
    }

    @Override
    public List<? extends NicSecondaryIp> listVmNicSecondaryIps(ListNicsCmd cmd) {
        Account caller = CallContext.current().getCallingAccount();
        Long nicId = cmd.getNicId();
        long vmId = cmd.getVmId();
        String keyword = cmd.getKeyword();
        UserVmVO userVm = _userVmDao.findById(vmId);

        if (userVm == null || (!userVm.isDisplayVm() && caller.getType() == Account.ACCOUNT_TYPE_NORMAL)) {
            throwInvalidIdException("Virtual machine id does not exist", Long.valueOf(vmId).toString(), "vmId");
        }

        _accountMgr.checkAccess(caller, null, true, userVm);
        return _nicSecondaryIpDao.listSecondaryIpUsingKeyword(nicId, keyword);
    }

    public List<NetworkGuru> getNetworkGurus() {
        return _networkGurus;
    }

    @Inject
    public void setNetworkGurus(List<NetworkGuru> networkGurus) {
        _networkGurus = networkGurus;
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_NET_IP_UPDATE, eventDescription = "updating public ip address", async = true)
    public IpAddress updateIP(Long id, String customId, Boolean displayIp) {
        Account caller = CallContext.current().getCallingAccount();
        IPAddressVO ipVO = _ipAddressDao.findById(id);
        if (ipVO == null) {
            throw new InvalidParameterValueException("Unable to find ip address by id");
        }

        // verify permissions
        if (ipVO.getAllocatedToAccountId() != null) {
            _accountMgr.checkAccess(caller, null, true, ipVO);
        } else if (caller.getType() != Account.ACCOUNT_TYPE_ADMIN) {
            throw new PermissionDeniedException("Only Root admin can update non-allocated ip addresses");
        }

        if (customId != null) {
            ipVO.setUuid(customId);
        }

        if (displayIp != null) {
            ipVO.setDisplay(displayIp);
        }

        _ipAddressDao.update(id, ipVO);
        return _ipAddressDao.findById(id);
    }

    @Override
    public AcquirePodIpCmdResponse allocatePodIp(Account ipOwner, String zoneId, String podId) throws ResourceAllocationException {

        Account caller = CallContext.current().getCallingAccount();
        long callerUserId = CallContext.current().getCallingUserId();
        DataCenter zone = _entityMgr.findByUuid(DataCenter.class, zoneId);

        if (zone == null) {
            throw new InvalidParameterValueException("Invalid zone Id ");
        }
        if (_accountMgr.checkAccessAndSpecifyAuthority(caller, zone.getId()) != zone.getId()) {
            throw new InvalidParameterValueException("Caller does not have permission for this Zone" + "(" + zoneId + ")");
        }
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Associate IP address called by the user " + callerUserId + " account " + ipOwner.getId());
        }
        return _ipAddrMgr.allocatePodIp(zoneId, podId);

    }

    @Override
    public boolean releasePodIp(ReleasePodIpCmdByAdmin ip) throws CloudRuntimeException {
        _ipAddrMgr.releasePodIp(ip.getId());
        return true;
    }

}
