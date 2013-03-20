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

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.ejb.Local;
import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.cloudstack.acl.ControlledEntity.ACLType;
import org.apache.cloudstack.acl.SecurityChecker.AccessType;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.agent.AgentManager;
import com.cloud.agent.Listener;
import com.cloud.agent.api.*;
import com.cloud.agent.api.to.NicTO;
import com.cloud.alert.AlertManager;
import com.cloud.api.ApiDBUtils;
import com.cloud.configuration.Config;
import com.cloud.configuration.ConfigurationManager;
import com.cloud.configuration.Resource.ResourceType;
import com.cloud.configuration.dao.ConfigurationDao;
import com.cloud.dc.*;
import com.cloud.dc.DataCenter.NetworkType;
import com.cloud.dc.Vlan.VlanType;
import com.cloud.dc.dao.AccountVlanMapDao;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.PodVlanMapDao;
import com.cloud.dc.dao.VlanDao;
import com.cloud.deploy.DataCenterDeployment;
import com.cloud.deploy.DeployDestination;
import com.cloud.deploy.DeploymentPlan;
import com.cloud.domain.Domain;
import com.cloud.domain.dao.DomainDao;
import com.cloud.event.EventTypes;
import com.cloud.event.UsageEventUtils;
import com.cloud.event.dao.UsageEventDao;
import com.cloud.exception.*;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.Status;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.network.IpAddress.State;
import com.cloud.network.Network.*;
import com.cloud.network.Networks.AddressFormat;
import com.cloud.network.Networks.BroadcastDomainType;
import com.cloud.network.Networks.IsolationType;
import com.cloud.network.Networks.TrafficType;
import com.cloud.network.addr.PublicIp;
import com.cloud.network.dao.FirewallRulesDao;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.IPAddressVO;
import com.cloud.network.dao.LoadBalancerDao;
import com.cloud.network.dao.LoadBalancerVO;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkDomainDao;
import com.cloud.network.dao.NetworkServiceMapDao;
import com.cloud.network.dao.NetworkServiceMapVO;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.dao.PhysicalNetworkDao;
import com.cloud.network.dao.PhysicalNetworkServiceProviderDao;
import com.cloud.network.dao.PhysicalNetworkTrafficTypeDao;
import com.cloud.network.dao.PhysicalNetworkTrafficTypeVO;
import com.cloud.network.dao.PhysicalNetworkVO;
import com.cloud.network.dao.UserIpv6AddressDao;
import com.cloud.network.element.DhcpServiceProvider;
import com.cloud.network.element.IpDeployer;
import com.cloud.network.element.IpDeployingRequester;
import com.cloud.network.element.LoadBalancingServiceProvider;
import com.cloud.network.element.NetworkElement;
import com.cloud.network.element.StaticNatServiceProvider;
import com.cloud.network.element.UserDataServiceProvider;
import com.cloud.network.guru.NetworkGuru;
import com.cloud.network.lb.LoadBalancingRule;
import com.cloud.network.lb.LoadBalancingRule.LbDestination;
import com.cloud.network.lb.LoadBalancingRule.LbHealthCheckPolicy;
import com.cloud.network.lb.LoadBalancingRule.LbStickinessPolicy;
import com.cloud.network.lb.LoadBalancingRulesManager;
import com.cloud.network.rules.*;
import com.cloud.network.rules.FirewallRule.Purpose;
import com.cloud.network.rules.dao.PortForwardingRulesDao;
import com.cloud.network.vpc.NetworkACLManager;
import com.cloud.network.vpc.VpcManager;
import com.cloud.network.vpc.dao.PrivateIpDao;
import com.cloud.network.vpn.RemoteAccessVpnService;
import com.cloud.offering.NetworkOffering;
import com.cloud.offering.NetworkOffering.Availability;
import com.cloud.offerings.NetworkOfferingServiceMapVO;
import com.cloud.offerings.NetworkOfferingVO;
import com.cloud.offerings.dao.NetworkOfferingDao;
import com.cloud.offerings.dao.NetworkOfferingServiceMapDao;
import com.cloud.org.Grouping;
import com.cloud.user.*;
import com.cloud.user.dao.AccountDao;
import com.cloud.user.dao.UserDao;
import com.cloud.utils.Journal;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.Pair;
import com.cloud.utils.component.AdapterBase;
import com.cloud.utils.component.ComponentContext;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.concurrency.NamedThreadFactory;
import com.cloud.utils.db.*;
import com.cloud.utils.db.JoinBuilder.JoinType;
import com.cloud.utils.db.SearchCriteria.Op;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.fsm.NoTransitionException;
import com.cloud.utils.fsm.StateMachine2;
import com.cloud.utils.net.Ip;
import com.cloud.utils.net.NetUtils;
import com.cloud.vm.*;
import com.cloud.vm.Nic;
import com.cloud.vm.NicProfile;
import com.cloud.vm.NicSecondaryIp;
import com.cloud.vm.NicVO;
import com.cloud.vm.ReservationContext;
import com.cloud.vm.ReservationContextImpl;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachine.Type;
import com.cloud.vm.dao.NicDao;
import com.cloud.vm.dao.NicSecondaryIpDao;
import com.cloud.vm.dao.NicSecondaryIpVO;
import com.cloud.vm.dao.UserVmDao;
import com.cloud.vm.dao.VMInstanceDao;

/**
 * NetworkManagerImpl implements NetworkManager.
 */
@Component
@Local(value = { NetworkManager.class})
public class NetworkManagerImpl extends ManagerBase implements NetworkManager, Listener {
    static final Logger s_logger = Logger.getLogger(NetworkManagerImpl.class);

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
    AlertManager _alertMgr;
    @Inject
    AccountManager _accountMgr;
    @Inject
    ConfigurationManager _configMgr;
    @Inject
    AccountVlanMapDao _accountVlanMapDao;
    @Inject
    NetworkOfferingDao _networkOfferingDao = null;
    @Inject
    NetworkDao _networksDao = null;
    @Inject
    NicDao _nicDao = null;
    @Inject
    RulesManager _rulesMgr;
    @Inject
    LoadBalancingRulesManager _lbMgr;
    @Inject
    RemoteAccessVpnService _vpnMgr;
    @Inject
    PodVlanMapDao _podVlanMapDao;

    @Inject 
    List<NetworkGuru> _networkGurus;

    @Inject  protected List<NetworkElement> _networkElements;

    @Inject NetworkDomainDao _networkDomainDao;
    @Inject List<IpDeployer> _ipDeployers;
    @Inject List<DhcpServiceProvider> _dhcpProviders;
 
    @Inject
    VMInstanceDao _vmDao;
    @Inject
    FirewallManager _firewallMgr;
    @Inject
    FirewallRulesDao _firewallDao;
    @Inject
    ResourceLimitService _resourceLimitMgr;
   
    @Inject
    NetworkOfferingServiceMapDao _ntwkOfferingSrvcDao;
    @Inject
    PhysicalNetworkDao _physicalNetworkDao;
    @Inject
    PhysicalNetworkServiceProviderDao _pNSPDao;
    @Inject
    PortForwardingRulesDao _portForwardingRulesDao;
    @Inject
    LoadBalancerDao _lbDao;
    @Inject
    PhysicalNetworkTrafficTypeDao _pNTrafficTypeDao;
    @Inject
    AgentManager _agentMgr;
    @Inject
    HostDao _hostDao;
    @Inject
    NetworkServiceMapDao _ntwkSrvcDao;
    @Inject
    StorageNetworkManager _stnwMgr;
    @Inject
    VpcManager _vpcMgr;
    @Inject
    PrivateIpDao _privateIpDao;
    @Inject
    NetworkACLManager _networkACLMgr;
    @Inject
    UsageEventDao _usageEventDao;
    @Inject
    NetworkModel _networkModel;
   @Inject
    NicSecondaryIpDao _nicSecondaryIpDao;
    @Inject
    UserIpv6AddressDao _ipv6Dao;
    @Inject
    Ipv6AddressManager _ipv6Mgr;

    protected StateMachine2<Network.State, Network.Event, Network> _stateMachine;
    private final HashMap<String, NetworkOfferingVO> _systemNetworks = new HashMap<String, NetworkOfferingVO>(5);
    private static Long _privateOfferingId = null;

    ScheduledExecutorService _executor;

    SearchBuilder<IPAddressVO> AssignIpAddressSearch;
    SearchBuilder<IPAddressVO> AssignIpAddressFromPodVlanSearch;

    int _networkGcWait;
    int _networkGcInterval;
    String _networkDomain;
    int _networkLockTimeout;

    private Map<String, String> _configs;

    HashMap<Long, Long> _lastNetworkIdsToFree = new HashMap<Long, Long>();


    @Override
    public PublicIp assignPublicIpAddress(long dcId, Long podId, Account owner, VlanType type, Long networkId, String requestedIp, boolean isSystem) throws InsufficientAddressCapacityException {
        return fetchNewPublicIp(dcId, podId, null, owner, type, networkId, false, true, requestedIp, isSystem, null);
    }

    @DB
    public PublicIp fetchNewPublicIp(long dcId, Long podId, Long vlanDbId, Account owner, VlanType vlanUse,
            Long guestNetworkId, boolean sourceNat, boolean assign, String requestedIp, boolean isSystem, Long vpcId)
            throws InsufficientAddressCapacityException {
        StringBuilder errorMessage = new StringBuilder("Unable to get ip adress in ");
        Transaction txn = Transaction.currentTxn();
        txn.start();
        SearchCriteria<IPAddressVO> sc = null;
        if (podId != null) {
            sc = AssignIpAddressFromPodVlanSearch.create();
            sc.setJoinParameters("podVlanMapSB", "podId", podId);
            errorMessage.append(" pod id=" + podId);
        } else {
            sc = AssignIpAddressSearch.create();
            errorMessage.append(" zone id=" + dcId);
        }

        if (vlanDbId != null) {
            sc.addAnd("vlanId", SearchCriteria.Op.EQ, vlanDbId);
            errorMessage.append(", vlanId id=" + vlanDbId);
        }

        sc.setParameters("dc", dcId);

        DataCenter zone = _configMgr.getZone(dcId);

        // for direct network take ip addresses only from the vlans belonging to the network
        if (vlanUse == VlanType.DirectAttached) {
            sc.setJoinParameters("vlan", "networkId", guestNetworkId);
            errorMessage.append(", network id=" + guestNetworkId);
        }
        sc.setJoinParameters("vlan", "type", vlanUse);

        if (requestedIp != null) {
            sc.addAnd("address", SearchCriteria.Op.EQ, requestedIp);
            errorMessage.append(": requested ip " + requestedIp + " is not available");
        }

        Filter filter = new Filter(IPAddressVO.class, "vlanId", true, 0l, 1l);

        List<IPAddressVO> addrs = _ipAddressDao.lockRows(sc, filter, true);

        if (addrs.size() == 0) {
            if (podId != null) {
                InsufficientAddressCapacityException ex = new InsufficientAddressCapacityException
                        ("Insufficient address capacity", Pod.class, podId);
                // for now, we hardcode the table names, but we should ideally do a lookup for the tablename from the VO object.
                ex.addProxyObject(ApiDBUtils.findPodById(podId).getUuid());
                throw ex;
            }
            s_logger.warn(errorMessage.toString());
            InsufficientAddressCapacityException ex = new InsufficientAddressCapacityException
                    ("Insufficient address capacity", DataCenter.class, dcId);
            ex.addProxyObject(ApiDBUtils.findZoneById(dcId).getUuid());
            throw ex;
        }

        assert (addrs.size() == 1) : "Return size is incorrect: " + addrs.size();

        IPAddressVO addr = addrs.get(0);
        addr.setSourceNat(sourceNat);
        addr.setAllocatedTime(new Date());
        addr.setAllocatedInDomainId(owner.getDomainId());
        addr.setAllocatedToAccountId(owner.getId());
        addr.setSystem(isSystem);

        if (assign) {
            markPublicIpAsAllocated(addr);
        } else {
            addr.setState(IpAddress.State.Allocating);
        }
        addr.setState(assign ? IpAddress.State.Allocated : IpAddress.State.Allocating);

        if (vlanUse != VlanType.DirectAttached || zone.getNetworkType() == NetworkType.Basic) {
            addr.setAssociatedWithNetworkId(guestNetworkId);
            addr.setVpcId(vpcId);
        }

        _ipAddressDao.update(addr.getId(), addr);

        txn.commit();

        if (vlanUse == VlanType.VirtualNetwork) {
            _firewallMgr.addSystemFirewallRules(addr, owner);
        }

        return PublicIp.createFromAddrAndVlan(addr, _vlanDao.findById(addr.getVlanId()));
    }

    @DB
    @Override
    public void markPublicIpAsAllocated(IPAddressVO addr) {

        assert (addr.getState() == IpAddress.State.Allocating || addr.getState() == IpAddress.State.Free) :
            "Unable to transition from state " + addr.getState() + " to " + IpAddress.State.Allocated;

        Transaction txn = Transaction.currentTxn();

        Account owner = _accountMgr.getAccount(addr.getAllocatedToAccountId());

        txn.start();
        addr.setState(IpAddress.State.Allocated);
        _ipAddressDao.update(addr.getId(), addr);

        // Save usage event
        if (owner.getAccountId() != Account.ACCOUNT_ID_SYSTEM) {
            VlanVO vlan = _vlanDao.findById(addr.getVlanId());

            String guestType = vlan.getVlanType().toString();
            UsageEventUtils.publishUsageEvent(EventTypes.EVENT_NET_IP_ASSIGN, owner.getId(),
                    addr.getDataCenterId(), addr.getId(), addr.getAddress().toString(), addr.isSourceNat(), guestType,
                    addr.getSystem(), addr.getClass().getName(), addr.getUuid());
            // don't increment resource count for direct ip addresses
            if (addr.getAssociatedWithNetworkId() != null) {
                _resourceLimitMgr.incrementResourceCount(owner.getId(), ResourceType.public_ip);
            }
        }

        txn.commit();
    }


    @Override
    public PublicIp assignSourceNatIpAddressToGuestNetwork(Account owner, Network guestNetwork)
            throws InsufficientAddressCapacityException, ConcurrentOperationException {
        assert (guestNetwork.getTrafficType() != null) : "You're asking for a source nat but your network " +
                "can't participate in source nat.  What do you have to say for yourself?";
        long dcId = guestNetwork.getDataCenterId();

        IPAddressVO sourceNatIp = getExistingSourceNatInNetwork(owner.getId(), guestNetwork.getId());

        PublicIp ipToReturn = null;
        if (sourceNatIp != null) {
            ipToReturn = PublicIp.createFromAddrAndVlan(sourceNatIp, _vlanDao.findById(sourceNatIp.getVlanId()));
        } else {
            ipToReturn = assignDedicateIpAddress(owner, guestNetwork.getId(), null, dcId, true);
        }

        return ipToReturn;
    }

    @Override
    public PublicIp assignVpnGatewayIpAddress(long dcId, Account owner, long vpcId) throws InsufficientAddressCapacityException, ConcurrentOperationException {
        return assignDedicateIpAddress(owner, null, vpcId, dcId, false);
    }


    @DB
    @Override
    public PublicIp assignDedicateIpAddress(Account owner, Long guestNtwkId, Long vpcId, long dcId, boolean isSourceNat)
            throws ConcurrentOperationException, InsufficientAddressCapacityException {

        long ownerId = owner.getId();

        // Check that the maximum number of public IPs for the given accountId will not be exceeded
        try {
            _resourceLimitMgr.checkResourceLimit(owner, ResourceType.public_ip);
        } catch (ResourceAllocationException ex) {
            s_logger.warn("Failed to allocate resource of type " + ex.getResourceType() + " for account " + owner);
            throw new AccountLimitException("Maximum number of public IP addresses for account: " + owner.getAccountName() + " has been exceeded.");
        }

        PublicIp ip = null;
        Transaction txn = Transaction.currentTxn();
        try {
            txn.start();

            owner = _accountDao.acquireInLockTable(ownerId);

            if (owner == null) {
                // this ownerId comes from owner or type Account. See the class "AccountVO" and the annotations in that class
                // to get the table name and field name that is queried to fill this ownerid.
                ConcurrentOperationException ex = new ConcurrentOperationException("Unable to lock account");
            }
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("lock account " + ownerId + " is acquired");
            }

            // If account has Account specific ip ranges, try to allocate ip from there
            Long vlanId = null;
            List<AccountVlanMapVO> maps = _accountVlanMapDao.listAccountVlanMapsByAccount(ownerId);
            if (maps != null && !maps.isEmpty()) {
                vlanId = maps.get(0).getVlanDbId();
            }


            ip = fetchNewPublicIp(dcId, null, vlanId, owner, VlanType.VirtualNetwork, guestNtwkId,
                    isSourceNat, false, null, false, vpcId);
            IPAddressVO publicIp = ip.ip();

            markPublicIpAsAllocated(publicIp);
            _ipAddressDao.update(publicIp.getId(), publicIp);

            txn.commit();
            return ip;
        } finally {
            if (owner != null) {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Releasing lock account " + ownerId);
                }

                _accountDao.releaseFromLockTable(ownerId);
            }
            if (ip == null) {
                txn.rollback();
                s_logger.error("Unable to get source nat ip address for account " + ownerId);
            }
        }
    }

    

    @Override
    public boolean applyIpAssociations(Network network, boolean continueOnError) throws ResourceUnavailableException {
        List<IPAddressVO> userIps = _ipAddressDao.listByAssociatedNetwork(network.getId(), null);
        List<PublicIp> publicIps = new ArrayList<PublicIp>();
        if (userIps != null && !userIps.isEmpty()) {
            for (IPAddressVO userIp : userIps) {
                PublicIp publicIp = PublicIp.createFromAddrAndVlan(userIp, _vlanDao.findById(userIp.getVlanId()));
                publicIps.add(publicIp);
            }
        }

        boolean success = applyIpAssociations(network, false, continueOnError, publicIps);

        if (success) {
            for (IPAddressVO addr : userIps) {
                if (addr.getState() == IpAddress.State.Allocating) {
                    markPublicIpAsAllocated(addr);
                } else if (addr.getState() == IpAddress.State.Releasing) {
                    // Cleanup all the resources for ip address if there are any, and only then un-assign ip in the
                    // system
                    if (cleanupIpResources(addr.getId(), Account.ACCOUNT_ID_SYSTEM, _accountMgr.getSystemAccount())) {
                        s_logger.debug("Unassiging ip address " + addr);
                        _ipAddressDao.unassignIpAddress(addr.getId());
                    } else {
                        success = false;
                        s_logger.warn("Failed to release resources for ip address id=" + addr.getId());
                    }
                }
            }
        }

        return success;
    }

   

    @Override
    public boolean applyIpAssociations(Network network, boolean rulesRevoked, boolean continueOnError, 
            List<? extends PublicIpAddress> publicIps) throws ResourceUnavailableException {
        boolean success = true;

        Map<PublicIpAddress, Set<Service>> ipToServices = _networkModel.getIpToServices(publicIps, rulesRevoked, true);
        Map<Provider, ArrayList<PublicIpAddress>> providerToIpList = _networkModel.getProviderToIpList(network, ipToServices);

        for (Provider provider : providerToIpList.keySet()) {
            try {
                ArrayList<PublicIpAddress> ips = providerToIpList.get(provider);
                if (ips == null || ips.isEmpty()) {
                    continue;
                }
                IpDeployer deployer = null;
                NetworkElement element = _networkModel.getElementImplementingProvider(provider.getName());
                if (!(ComponentContext.getTargetObject(element) instanceof IpDeployingRequester)) {
                    throw new CloudRuntimeException("Element " + element + " is not a IpDeployingRequester!");
                }
                deployer = ((IpDeployingRequester)ComponentContext.getTargetObject(element)).getIpDeployer(network);
                if (deployer == null) {
                    throw new CloudRuntimeException("Fail to get ip deployer for element: " + element);
                }
                Set<Service> services = new HashSet<Service>();
                for (PublicIpAddress ip : ips) {
                    if (!ipToServices.containsKey(ip)) {
                        continue;
                    }
                    services.addAll(ipToServices.get(ip));
                }
                deployer.applyIps(network, ips, services);
            } catch (ResourceUnavailableException e) {
                success = false;
                if (!continueOnError) {
                    throw e;
                } else {
                    s_logger.debug("Resource is not available: " + provider.getName(), e);
                }
            }
        }

        return success;
    }


    
    
    protected List<? extends Network> getIsolatedNetworksWithSourceNATOwnedByAccountInZone(long zoneId, Account owner) {

        return _networksDao.listSourceNATEnabledNetworks(owner.getId(), zoneId, Network.GuestType.Isolated);
    }

    

    private IpAddress allocateIP(Account ipOwner, boolean isSystem, long zoneId) 
            throws ResourceAllocationException, InsufficientAddressCapacityException, ConcurrentOperationException {
        Account caller = UserContext.current().getCaller();
        long callerUserId = UserContext.current().getCallerUserId();
        // check permissions
        _accountMgr.checkAccess(caller, null, false, ipOwner);

        DataCenter zone = _configMgr.getZone(zoneId);
        
        return allocateIp(ipOwner, isSystem, caller, callerUserId, zone);
    }

    @DB
    @Override
    public IpAddress allocateIp(Account ipOwner, boolean isSystem, Account caller, long callerUserId, DataCenter zone) 
            throws ConcurrentOperationException, ResourceAllocationException,
            InsufficientAddressCapacityException {

        VlanType vlanType = VlanType.VirtualNetwork;
        boolean assign = false;

        if (Grouping.AllocationState.Disabled == zone.getAllocationState() && !_accountMgr.isRootAdmin(caller.getType())) {
            // zone is of type DataCenter. See DataCenterVO.java.
            PermissionDeniedException ex = new PermissionDeniedException("Cannot perform this operation, " +
                    "Zone is currently disabled");
            ex.addProxyObject("data_center", zone.getId(), "zoneId");
            throw ex;
        }

        PublicIp ip = null;

        Transaction txn = Transaction.currentTxn();
        Account accountToLock = null;
        try {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Associate IP address called by the user " + callerUserId + " account " + ipOwner.getId());
            }
            accountToLock = _accountDao.acquireInLockTable(ipOwner.getId());
            if (accountToLock == null) {
                s_logger.warn("Unable to lock account: " + ipOwner.getId());
                throw new ConcurrentOperationException("Unable to acquire account lock");
            }

            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Associate IP address lock acquired");
            }

            // Check that the maximum number of public IPs for the given
            // accountId will not be exceeded
            _resourceLimitMgr.checkResourceLimit(accountToLock, ResourceType.public_ip);

            txn.start();

            ip = fetchNewPublicIp(zone.getId(), null, null, ipOwner, vlanType, null,
                    false, assign, null, isSystem, null);

            if (ip == null) {

                InsufficientAddressCapacityException ex = new InsufficientAddressCapacityException
                        ("Unable to find available public IP addresses", DataCenter.class, zone.getId());
                ex.addProxyObject(ApiDBUtils.findZoneById(zone.getId()).getUuid());
                throw ex;
            }
            UserContext.current().setEventDetails("Ip Id: " + ip.getId());
            Ip ipAddress = ip.getAddress();

            s_logger.debug("Got " + ipAddress + " to assign for account " + ipOwner.getId() + " in zone " + zone.getId());

            txn.commit();
        } finally {
            if (accountToLock != null) {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Releasing lock account " + ipOwner);
                }
                _accountDao.releaseFromLockTable(ipOwner.getId());
                s_logger.debug("Associate IP address lock released");
            }
        }
        return ip;
    }

    protected IPAddressVO getExistingSourceNatInNetwork(long ownerId, Long networkId) {
        
        List<? extends IpAddress> addrs = _networkModel.listPublicIpsAssignedToGuestNtwk(ownerId, networkId, true);
    
        IPAddressVO sourceNatIp = null;
        if (addrs.isEmpty()) {
            return null;
        } else {
            // Account already has ip addresses
            for (IpAddress addr : addrs) {
                if (addr.isSourceNat()) {
                    sourceNatIp = _ipAddressDao.findById(addr.getId());
                    return sourceNatIp;
                }
            }
    
            assert (sourceNatIp != null) : "How do we get a bunch of ip addresses but none of them are source nat? " +
                    "account=" + ownerId + "; networkId=" + networkId;
        }

        return sourceNatIp;
    }
    
    @DB
    @Override
    public IPAddressVO associateIPToGuestNetwork(long ipId, long networkId, boolean releaseOnFailure)
            throws ResourceAllocationException, ResourceUnavailableException,
    InsufficientAddressCapacityException, ConcurrentOperationException {
        Account caller = UserContext.current().getCaller();
        Account owner = null;

        IPAddressVO ipToAssoc = _ipAddressDao.findById(ipId);
        if (ipToAssoc != null) {
            _accountMgr.checkAccess(caller, null, true, ipToAssoc);
            owner = _accountMgr.getAccount(ipToAssoc.getAllocatedToAccountId());
        } else {
            s_logger.debug("Unable to find ip address by id: " + ipId);
            return null;
        }

        if (ipToAssoc.getAssociatedWithNetworkId() != null) {
            s_logger.debug("IP " + ipToAssoc + " is already assocaited with network id" + networkId);
            return ipToAssoc;
        }

        Network network = _networksDao.findById(networkId);
        if (network != null) {
            _accountMgr.checkAccess(owner, AccessType.UseNetwork, false, network);
        } else {
            s_logger.debug("Unable to find ip address by id: " + ipId);
            return null;
        }

        DataCenter zone = _configMgr.getZone(network.getDataCenterId());

        // allow associating IP addresses to guest network only
        if (network.getTrafficType() != TrafficType.Guest) {
            throw new InvalidParameterValueException("Ip address can be associated to the network with trafficType " + TrafficType.Guest);
        }

        // Check that network belongs to IP owner - skip this check for Basic zone as there is just one guest network,
        // and it belongs to the system
        if (zone.getNetworkType() != NetworkType.Basic && network.getAccountId() != owner.getId()) {
            throw new InvalidParameterValueException("The owner of the network is not the same as owner of the IP");
        }

        // In Advance zone only allow to do IP assoc for Isolated networks with source nat service enabled
        if (zone.getNetworkType() == NetworkType.Advanced &&
            !(network.getGuestType() == GuestType.Isolated && _networkModel.areServicesSupportedInNetwork(network.getId(),
                Service.SourceNat))) {
            throw new InvalidParameterValueException("In zone of type " + NetworkType.Advanced +
                    " ip address can be associated only to the network of guest type " + GuestType.Isolated + " with the "
                    + Service.SourceNat.getName() + " enabled");
        }

        NetworkOffering offering = _networkOfferingDao.findById(network.getNetworkOfferingId());
        boolean sharedSourceNat = offering.getSharedSourceNat();
        boolean isSourceNat = false;
        if (!sharedSourceNat) {
            if (getExistingSourceNatInNetwork(owner.getId(), networkId) == null) {
                if (network.getGuestType() == GuestType.Isolated && network.getVpcId() == null) {
                    isSourceNat = true;
                }
            }
        }

        s_logger.debug("Associating ip " + ipToAssoc + " to network " + network);

        IPAddressVO ip = _ipAddressDao.findById(ipId);
        //update ip address with networkId
        ip.setAssociatedWithNetworkId(networkId);
        ip.setSourceNat(isSourceNat);
        _ipAddressDao.update(ipId, ip);

        boolean success = false;
        try {
            success = applyIpAssociations(network, false);
            if (success) {
                s_logger.debug("Successfully associated ip address " + ip.getAddress().addr() + " to network " + network);
            } else {
                s_logger.warn("Failed to associate ip address " + ip.getAddress().addr() + " to network " + network);
            }
            return ip;
        } finally {
            if (!success && releaseOnFailure) {
                if (ip != null) {
                    try {
                        s_logger.warn("Failed to associate ip address, so releasing ip from the database " + ip);
                        _ipAddressDao.markAsUnavailable(ip.getId());
                        if (!applyIpAssociations(network, true)) {
                            // if fail to apply ip assciations again, unassign ip address without updating resource
                            // count and generating usage event as there is no need to keep it in the db
                            _ipAddressDao.unassignIpAddress(ip.getId());
                        }
                    } catch (Exception e) {
                        s_logger.warn("Unable to disassociate ip address for recovery", e);
                    }
                }
            }
        }
    }


    @Override
    @DB
    public boolean disassociatePublicIpAddress(long addrId, long userId, Account caller) {

        boolean success = true;
        // Cleanup all ip address resources - PF/LB/Static nat rules
        if (!cleanupIpResources(addrId, userId, caller)) {
            success = false;
            s_logger.warn("Failed to release resources for ip address id=" + addrId);
        }

        IPAddressVO ip = markIpAsUnavailable(addrId);

        assert (ip != null) : "Unable to mark the ip address id=" + addrId + " as unavailable.";
        if (ip == null) {
            return true;
        }

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Releasing ip id=" + addrId + "; sourceNat = " + ip.isSourceNat());
        }

        if (ip.getAssociatedWithNetworkId() != null) {
            Network network = _networksDao.findById(ip.getAssociatedWithNetworkId());
            try {
                if (!applyIpAssociations(network, true)) {
                    s_logger.warn("Unable to apply ip address associations for " + network);
                    success = false;
                }
            } catch (ResourceUnavailableException e) {
                throw new CloudRuntimeException("We should never get to here because we used true when applyIpAssociations", e);
            }
        } else {
            if (ip.getState() == IpAddress.State.Releasing) {
                _ipAddressDao.unassignIpAddress(ip.getId());
            }
        }

        if (success) {
            s_logger.debug("Released a public ip id=" + addrId);
        }

        return success;
    }

    @Override
    @DB
    public boolean configure(final String name, final Map<String, Object> params) throws ConfigurationException {
        _configs = _configDao.getConfiguration("AgentManager", params);
        _networkGcWait = NumbersUtil.parseInt(_configs.get(Config.NetworkGcWait.key()), 600);
        _networkGcInterval = NumbersUtil.parseInt(_configs.get(Config.NetworkGcInterval.key()), 600);

        _configs = _configDao.getConfiguration("Network", params);
        _networkDomain = _configs.get(Config.GuestDomainSuffix.key());

        _networkLockTimeout = NumbersUtil.parseInt(_configs.get(Config.NetworkLockTimeout.key()), 600);

        


        // populate providers
        Map<Network.Service, Set<Network.Provider>> defaultSharedNetworkOfferingProviders = new HashMap<Network.Service, Set<Network.Provider>>();
        Set<Network.Provider> defaultProviders = new HashSet<Network.Provider>();

        defaultProviders.add(Network.Provider.VirtualRouter);
        defaultSharedNetworkOfferingProviders.put(Service.Dhcp, defaultProviders);
        defaultSharedNetworkOfferingProviders.put(Service.Dns, defaultProviders);
        defaultSharedNetworkOfferingProviders.put(Service.UserData, defaultProviders);

        Map<Network.Service, Set<Network.Provider>> defaultIsolatedNetworkOfferingProviders = defaultSharedNetworkOfferingProviders;

        Map<Network.Service, Set<Network.Provider>> defaultSharedSGEnabledNetworkOfferingProviders = new HashMap<Network.Service, Set<Network.Provider>>();
        defaultSharedSGEnabledNetworkOfferingProviders.put(Service.Dhcp, defaultProviders);
        defaultSharedSGEnabledNetworkOfferingProviders.put(Service.Dns, defaultProviders);
        defaultSharedSGEnabledNetworkOfferingProviders.put(Service.UserData, defaultProviders);
        Set<Provider> sgProviders = new HashSet<Provider>();
        sgProviders.add(Provider.SecurityGroupProvider);
        defaultSharedSGEnabledNetworkOfferingProviders.put(Service.SecurityGroup, sgProviders);

        Map<Network.Service, Set<Network.Provider>> defaultIsolatedSourceNatEnabledNetworkOfferingProviders =
                new HashMap<Network.Service, Set<Network.Provider>>();
        defaultProviders.clear();
        defaultProviders.add(Network.Provider.VirtualRouter);
        defaultIsolatedSourceNatEnabledNetworkOfferingProviders.put(Service.Dhcp, defaultProviders);
        defaultIsolatedSourceNatEnabledNetworkOfferingProviders.put(Service.Dns, defaultProviders);
        defaultIsolatedSourceNatEnabledNetworkOfferingProviders.put(Service.UserData, defaultProviders);
        defaultIsolatedSourceNatEnabledNetworkOfferingProviders.put(Service.Firewall, defaultProviders);
        defaultIsolatedSourceNatEnabledNetworkOfferingProviders.put(Service.Gateway, defaultProviders);
        defaultIsolatedSourceNatEnabledNetworkOfferingProviders.put(Service.Lb, defaultProviders);
        defaultIsolatedSourceNatEnabledNetworkOfferingProviders.put(Service.SourceNat, defaultProviders);
        defaultIsolatedSourceNatEnabledNetworkOfferingProviders.put(Service.StaticNat, defaultProviders);
        defaultIsolatedSourceNatEnabledNetworkOfferingProviders.put(Service.PortForwarding, defaultProviders);
        defaultIsolatedSourceNatEnabledNetworkOfferingProviders.put(Service.Vpn, defaultProviders);


        Map<Network.Service, Set<Network.Provider>> defaultVPCOffProviders =
                new HashMap<Network.Service, Set<Network.Provider>>();
        defaultProviders.clear();
        defaultProviders.add(Network.Provider.VirtualRouter);
        defaultVPCOffProviders.put(Service.Dhcp, defaultProviders);
        defaultVPCOffProviders.put(Service.Dns, defaultProviders);
        defaultVPCOffProviders.put(Service.UserData, defaultProviders);
        defaultVPCOffProviders.put(Service.NetworkACL, defaultProviders);
        defaultVPCOffProviders.put(Service.Gateway, defaultProviders);
        defaultVPCOffProviders.put(Service.Lb, defaultProviders);
        defaultVPCOffProviders.put(Service.SourceNat, defaultProviders);
        defaultVPCOffProviders.put(Service.StaticNat, defaultProviders);
        defaultVPCOffProviders.put(Service.PortForwarding, defaultProviders);
        defaultVPCOffProviders.put(Service.Vpn, defaultProviders);

        Transaction txn = Transaction.currentTxn();
        txn.start();
        // diff between offering #1 and #2 - securityGroup is enabled for the first, and disabled for the third

        NetworkOfferingVO offering = null;
        if (_networkOfferingDao.findByUniqueName(NetworkOffering.DefaultSharedNetworkOfferingWithSGService) == null) {
            offering = _configMgr.createNetworkOffering(NetworkOffering.DefaultSharedNetworkOfferingWithSGService, "Offering for Shared Security group enabled networks", TrafficType.Guest, null,
                    true, Availability.Optional, null, defaultSharedNetworkOfferingProviders, true, Network.GuestType.Shared, false, null, true, null, true, false);
            offering.setState(NetworkOffering.State.Enabled);
            _networkOfferingDao.update(offering.getId(), offering);
        }

        if (_networkOfferingDao.findByUniqueName(NetworkOffering.DefaultSharedNetworkOffering) == null) {
            offering = _configMgr.createNetworkOffering(NetworkOffering.DefaultSharedNetworkOffering, "Offering for Shared networks", TrafficType.Guest, null, true, Availability.Optional, null,
                    defaultSharedNetworkOfferingProviders, true, Network.GuestType.Shared, false, null, true, null, true, false);
            offering.setState(NetworkOffering.State.Enabled);
            _networkOfferingDao.update(offering.getId(), offering);
        }

        Map<Network.Service, Set<Network.Provider>> defaultINetworkOfferingProvidersForVpcNetwork = new HashMap<Network.Service, Set<Network.Provider>>();
        defaultProviders.clear();
        defaultProviders.add(Network.Provider.VPCVirtualRouter);
        defaultINetworkOfferingProvidersForVpcNetwork.put(Service.Dhcp, defaultProviders);
        defaultINetworkOfferingProvidersForVpcNetwork.put(Service.Dns, defaultProviders);
        defaultINetworkOfferingProvidersForVpcNetwork.put(Service.UserData, defaultProviders);
        defaultINetworkOfferingProvidersForVpcNetwork.put(Service.Firewall, defaultProviders);
        defaultINetworkOfferingProvidersForVpcNetwork.put(Service.Gateway, defaultProviders);
        defaultINetworkOfferingProvidersForVpcNetwork.put(Service.Lb, defaultProviders);
        defaultINetworkOfferingProvidersForVpcNetwork.put(Service.SourceNat, defaultProviders);
        defaultINetworkOfferingProvidersForVpcNetwork.put(Service.StaticNat, defaultProviders);
        defaultINetworkOfferingProvidersForVpcNetwork.put(Service.PortForwarding, defaultProviders);
        defaultINetworkOfferingProvidersForVpcNetwork.put(Service.Vpn, defaultProviders);

        if (_networkOfferingDao.findByUniqueName(NetworkOffering.DefaultIsolatedNetworkOfferingWithSourceNatService) == null) {
            offering = _configMgr.createNetworkOffering(NetworkOffering.DefaultIsolatedNetworkOfferingWithSourceNatService,
                    "Offering for Isolated networks with Source Nat service enabled", TrafficType.Guest,
                    null, false, Availability.Required, null, defaultINetworkOfferingProvidersForVpcNetwork,
                    true, Network.GuestType.Isolated, false, null, true, null, false, false);
            offering.setState(NetworkOffering.State.Enabled);
            _networkOfferingDao.update(offering.getId(), offering);
        }

        if (_networkOfferingDao.findByUniqueName(NetworkOffering.DefaultIsolatedNetworkOfferingForVpcNetworks) == null) {
            offering = _configMgr.createNetworkOffering(NetworkOffering.DefaultIsolatedNetworkOfferingForVpcNetworks,
                    "Offering for Isolated VPC networks with Source Nat service enabled", TrafficType.Guest,
                    null, false, Availability.Optional, null, defaultVPCOffProviders,
                    true, Network.GuestType.Isolated, false, null, false, null, false, false);
            offering.setState(NetworkOffering.State.Enabled);
            _networkOfferingDao.update(offering.getId(), offering);
        }

        if (_networkOfferingDao.findByUniqueName(NetworkOffering.DefaultIsolatedNetworkOfferingForVpcNetworksNoLB) == null) {
            //remove LB service
            defaultVPCOffProviders.remove(Service.Lb);
            offering = _configMgr.createNetworkOffering(NetworkOffering.DefaultIsolatedNetworkOfferingForVpcNetworksNoLB,
                    "Offering for Isolated VPC networks with Source Nat service enabled and LB service disabled", TrafficType.Guest,
                    null, false, Availability.Optional, null, defaultVPCOffProviders,
                    true, Network.GuestType.Isolated, false, null, false, null, false, false);
            offering.setState(NetworkOffering.State.Enabled);
            _networkOfferingDao.update(offering.getId(), offering);
        }

        if (_networkOfferingDao.findByUniqueName(NetworkOffering.DefaultIsolatedNetworkOffering) == null) {
            offering = _configMgr.createNetworkOffering(NetworkOffering.DefaultIsolatedNetworkOffering,
                    "Offering for Isolated networks with no Source Nat service", TrafficType.Guest, null, true,
                    Availability.Optional, null, defaultIsolatedNetworkOfferingProviders, true, Network.GuestType.Isolated,
                    false, null, true, null, true, false);
            offering.setState(NetworkOffering.State.Enabled);
            _networkOfferingDao.update(offering.getId(), offering);
        }

        Map<Network.Service, Set<Network.Provider>> netscalerServiceProviders = new HashMap<Network.Service, Set<Network.Provider>>();
        Set<Network.Provider> vrProvider = new HashSet<Network.Provider>();
        vrProvider.add(Provider.VirtualRouter);
        Set<Network.Provider> sgProvider = new HashSet<Network.Provider>();
        sgProvider.add(Provider.SecurityGroupProvider);
        Set<Network.Provider> nsProvider = new HashSet<Network.Provider>();
        nsProvider.add(Provider.Netscaler);
        netscalerServiceProviders.put(Service.Dhcp, vrProvider);
        netscalerServiceProviders.put(Service.Dns, vrProvider);
        netscalerServiceProviders.put(Service.UserData, vrProvider);
        netscalerServiceProviders.put(Service.SecurityGroup, sgProvider);
        netscalerServiceProviders.put(Service.StaticNat, nsProvider);
        netscalerServiceProviders.put(Service.Lb, nsProvider);

        Map<Service, Map<Capability, String>> serviceCapabilityMap = new HashMap<Service, Map<Capability, String>>();
        Map<Capability, String> elb = new HashMap<Capability, String>();
        elb.put(Capability.ElasticLb, "true");
        Map<Capability, String> eip = new HashMap<Capability, String>();
        eip.put(Capability.ElasticIp, "true");
        serviceCapabilityMap.put(Service.Lb, elb);
        serviceCapabilityMap.put(Service.StaticNat, eip);

        if (_networkOfferingDao.findByUniqueName(NetworkOffering.DefaultSharedEIPandELBNetworkOffering) == null) {
            offering = _configMgr.createNetworkOffering(NetworkOffering.DefaultSharedEIPandELBNetworkOffering, "Offering for Shared networks with Elastic IP and Elastic LB capabilities", TrafficType.Guest, null, true,
                    Availability.Optional, null, netscalerServiceProviders, true, Network.GuestType.Shared, false, null, true, serviceCapabilityMap, true, false);
            offering.setState(NetworkOffering.State.Enabled);
            offering.setDedicatedLB(false);
            _networkOfferingDao.update(offering.getId(), offering);
        }

        txn.commit();

       
        AssignIpAddressSearch = _ipAddressDao.createSearchBuilder();
        AssignIpAddressSearch.and("dc", AssignIpAddressSearch.entity().getDataCenterId(), Op.EQ);
        AssignIpAddressSearch.and("allocated", AssignIpAddressSearch.entity().getAllocatedTime(), Op.NULL);
        AssignIpAddressSearch.and("vlanId", AssignIpAddressSearch.entity().getVlanId(), Op.EQ);
        SearchBuilder<VlanVO> vlanSearch = _vlanDao.createSearchBuilder();
        vlanSearch.and("type", vlanSearch.entity().getVlanType(), Op.EQ);
        vlanSearch.and("networkId", vlanSearch.entity().getNetworkId(), Op.EQ);
        AssignIpAddressSearch.join("vlan", vlanSearch, vlanSearch.entity().getId(), AssignIpAddressSearch.entity().getVlanId(), JoinType.INNER);
        AssignIpAddressSearch.done();

        AssignIpAddressFromPodVlanSearch = _ipAddressDao.createSearchBuilder();
        AssignIpAddressFromPodVlanSearch.and("dc", AssignIpAddressFromPodVlanSearch.entity().getDataCenterId(), Op.EQ);
        AssignIpAddressFromPodVlanSearch.and("allocated", AssignIpAddressFromPodVlanSearch.entity().getAllocatedTime(), Op.NULL);
        SearchBuilder<VlanVO> podVlanSearch = _vlanDao.createSearchBuilder();
        podVlanSearch.and("type", podVlanSearch.entity().getVlanType(), Op.EQ);
        podVlanSearch.and("networkId", podVlanSearch.entity().getNetworkId(), Op.EQ);
        SearchBuilder<PodVlanMapVO> podVlanMapSB = _podVlanMapDao.createSearchBuilder();
        podVlanMapSB.and("podId", podVlanMapSB.entity().getPodId(), Op.EQ);
        AssignIpAddressFromPodVlanSearch.join("podVlanMapSB", podVlanMapSB, podVlanMapSB.entity().getVlanDbId(), AssignIpAddressFromPodVlanSearch.entity().getVlanId(), JoinType.INNER);
        AssignIpAddressFromPodVlanSearch.join("vlan", podVlanSearch, podVlanSearch.entity().getId(), AssignIpAddressFromPodVlanSearch.entity().getVlanId(), JoinType.INNER);
        AssignIpAddressFromPodVlanSearch.done();

        _executor = Executors.newScheduledThreadPool(1, new NamedThreadFactory("Network-Scavenger"));

        _agentMgr.registerForHostEvents(this, true, false, true);

        Network.State.getStateMachine().registerListener(new NetworkStateListener(_usageEventDao, _networksDao));

        s_logger.info("Network Manager is configured.");

        return true;
    }

    @Override
    public boolean start() {
        _executor.scheduleWithFixedDelay(new NetworkGarbageCollector(), _networkGcInterval, _networkGcInterval, TimeUnit.SECONDS);
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    protected NetworkManagerImpl() {
        setStateMachine();
    }

    @Override
    public List<NetworkVO> setupNetwork(Account owner, NetworkOffering offering, DeploymentPlan plan, String name, 
            String displayText, boolean isDefault)
            throws ConcurrentOperationException {
        return setupNetwork(owner, offering, null, plan, name, displayText, false, null, null, null, null);
    }

    @Override
    @DB
    public List<NetworkVO> setupNetwork(Account owner, NetworkOffering offering, Network predefined, DeploymentPlan 
            plan, String name, String displayText, boolean errorIfAlreadySetup, Long domainId,
            ACLType aclType, Boolean subdomainAccess, Long vpcId) throws ConcurrentOperationException {

        Account locked = _accountDao.acquireInLockTable(owner.getId());
        if (locked == null) {
            throw new ConcurrentOperationException("Unable to acquire lock on " + owner);
        }

        try {
            if (predefined == null
                    || (offering.getTrafficType() != TrafficType.Guest && predefined.getCidr() == null && predefined.getBroadcastUri() == null &&
                    !(predefined.getBroadcastDomainType() == BroadcastDomainType.Vlan || predefined.getBroadcastDomainType() == BroadcastDomainType.Lswitch))) {
                List<NetworkVO> configs = _networksDao.listBy(owner.getId(), offering.getId(), plan.getDataCenterId());
                if (configs.size() > 0) {
                    if (s_logger.isDebugEnabled()) {
                        s_logger.debug("Found existing network configuration for offering " + offering + ": " + configs.get(0));
                    }

                    if (errorIfAlreadySetup) {
                        InvalidParameterValueException ex = new InvalidParameterValueException("Found existing network configuration (with specified id) for offering (with specified id)");
                        ex.addProxyObject(offering, offering.getId(), "offeringId");
                        ex.addProxyObject(configs.get(0), configs.get(0).getId(), "networkConfigId");
                        throw ex;
                    } else {
                        return configs;
                    }
                }
            } else if (predefined != null && predefined.getCidr() != null && predefined.getBroadcastUri() == null && vpcId == null) {
                // don't allow to have 2 networks with the same cidr in the same zone for the account
                List<NetworkVO> configs = _networksDao.listBy(owner.getId(), plan.getDataCenterId(), predefined.getCidr(), true);
                if (configs.size() > 0) {
                    if (s_logger.isDebugEnabled()) {
                        s_logger.debug("Found existing network configuration for offering " + offering + ": " + configs.get(0));
                    }

                    if (errorIfAlreadySetup) {
                        InvalidParameterValueException ex = new InvalidParameterValueException("Found existing network configuration (with specified id) for offering (with specified id)");
                        ex.addProxyObject(offering, offering.getId(), "offeringId");
                        ex.addProxyObject(configs.get(0), configs.get(0).getId(), "networkConfigId");
                        throw ex;
                    } else {
                        return configs;
                    }
                }
            }

            List<NetworkVO> networks = new ArrayList<NetworkVO>();

            long related = -1;

            for (NetworkGuru guru : _networkGurus) {
                Network network = guru.design(offering, plan, predefined, owner);
                if (network == null) {
                    continue;
                }

                if (network.getId() != -1) {
                    if (network instanceof NetworkVO) {
                        networks.add((NetworkVO) network);
                    } else {
                        networks.add(_networksDao.findById(network.getId()));
                    }
                    continue;
                }

                long id = _networksDao.getNextInSequence(Long.class, "id");
                if (related == -1) {
                    related = id;
                }

                Transaction txn = Transaction.currentTxn();
                txn.start();

                NetworkVO vo = new NetworkVO(id, network, offering.getId(), guru.getName(), owner.getDomainId(), owner.getId(),
                        related, name, displayText, predefined.getNetworkDomain(), offering.getGuestType(),
                        plan.getDataCenterId(), plan.getPhysicalNetworkId(), aclType, offering.getSpecifyIpRanges(), vpcId);
                networks.add(_networksDao.persist(vo, vo.getGuestType() == Network.GuestType.Isolated,
                        finalizeServicesAndProvidersForNetwork(offering, plan.getPhysicalNetworkId())));

                if (domainId != null && aclType == ACLType.Domain) {
                    _networksDao.addDomainToNetwork(id, domainId, subdomainAccess);
                }

                txn.commit();
            }

            if (networks.size() < 1) {
                // see networkOfferingVO.java
                CloudRuntimeException ex = new CloudRuntimeException("Unable to convert network offering with specified id to network profile");
                ex.addProxyObject(offering, offering.getId(), "offeringId");
                throw ex;
            }

            return networks;
        } finally {
            s_logger.debug("Releasing lock for " + locked);
            _accountDao.releaseFromLockTable(locked.getId());
        }
    }

    

    @Override
    @DB
    public void allocate(VirtualMachineProfile<? extends VMInstanceVO> vm, List<Pair<NetworkVO, NicProfile>> networks)
            throws InsufficientCapacityException, ConcurrentOperationException {
        Transaction txn = Transaction.currentTxn();
        txn.start();

        int deviceId = 0;

        boolean[] deviceIds = new boolean[networks.size()];
        Arrays.fill(deviceIds, false);

        List<NicProfile> nics = new ArrayList<NicProfile>(networks.size());
        NicProfile defaultNic = null;

        for (Pair<NetworkVO, NicProfile> network : networks) {
            NetworkVO config = network.first();
            NicProfile requested = network.second();

            Boolean isDefaultNic = false;
            if (vm != null && (requested != null && requested.isDefaultNic())) {
                isDefaultNic = true;
            }

            while (deviceIds[deviceId] && deviceId < deviceIds.length) {
                deviceId++;
            }

            Pair<NicProfile,Integer> vmNicPair = allocateNic(requested, config, isDefaultNic,
                    deviceId, vm);

            NicProfile vmNic = vmNicPair.first();
            if (vmNic == null) {
                continue;
            }

            deviceId = vmNicPair.second();

            int devId = vmNic.getDeviceId();
            if (devId > deviceIds.length) {
                throw new IllegalArgumentException("Device id for nic is too large: " + vmNic);
            }
            if (deviceIds[devId]) {
                throw new IllegalArgumentException("Conflicting device id for two different nics: " + vmNic);
            }

            deviceIds[devId] = true;

            if (vmNic.isDefaultNic()) {
                if (defaultNic != null) {
                    throw new IllegalArgumentException("You cannot specify two nics as default nics: nic 1 = " +
                defaultNic + "; nic 2 = " + vmNic);
                }
                defaultNic = vmNic;
            }

            nics.add(vmNic);
            vm.addNic(vmNic);

        }

        if (nics.size() != networks.size()) {
            s_logger.warn("Number of nics " + nics.size() + " doesn't match number of requested networks " + networks.size());
            throw new CloudRuntimeException("Number of nics " + nics.size() + " doesn't match number of requested networks " + networks.size());
        }

        if (nics.size() == 1) {
            nics.get(0).setDefaultNic(true);
        }

        txn.commit();
    }


    @DB
    @Override
    public Pair<NicProfile,Integer> allocateNic(NicProfile requested, Network network, Boolean isDefaultNic,
            int deviceId, VirtualMachineProfile<? extends VMInstanceVO> vm) throws InsufficientVirtualNetworkCapcityException,
            InsufficientAddressCapacityException, ConcurrentOperationException{

        NetworkVO ntwkVO = _networksDao.findById(network.getId());
        s_logger.debug("Allocating nic for vm " + vm.getVirtualMachine() + " in network " + network + " with requested profile " + requested);
        NetworkGuru guru = AdapterBase.getAdapterByName(_networkGurus, ntwkVO.getGuruName());

        if (requested != null && requested.getMode() == null) {
            requested.setMode(network.getMode());
        }
        NicProfile profile = guru.allocate(network, requested, vm);
        if (isDefaultNic != null) {
            profile.setDefaultNic(isDefaultNic);
        }

        if (profile == null) {
            return null;
        }

        if (requested != null && requested.getMode() == null) {
            profile.setMode(requested.getMode());
        } else {
            profile.setMode(network.getMode());
        }

        NicVO vo = new NicVO(guru.getName(), vm.getId(), network.getId(), vm.getType());

        deviceId = applyProfileToNic(vo, profile, deviceId);

        vo = _nicDao.persist(vo);
    
        Integer networkRate = _networkModel.getNetworkRate(network.getId(), vm.getId());
        NicProfile vmNic = new NicProfile(vo, network, vo.getBroadcastUri(), vo.getIsolationUri(), networkRate, 
                _networkModel.isSecurityGroupSupportedInNetwork(network), _networkModel.getNetworkTag(vm.getHypervisorType(),
                network));

        return new Pair<NicProfile,Integer>(vmNic, Integer.valueOf(deviceId));
    }

    protected Integer applyProfileToNic(NicVO vo, NicProfile profile, Integer deviceId) {
        if (profile.getDeviceId() != null) {
            vo.setDeviceId(profile.getDeviceId());
        } else if (deviceId != null) {
            vo.setDeviceId(deviceId++);
        }

        if (profile.getReservationStrategy() != null) {
            vo.setReservationStrategy(profile.getReservationStrategy());
        }

        vo.setDefaultNic(profile.isDefaultNic());

        vo.setIp4Address(profile.getIp4Address());
        vo.setAddressFormat(profile.getFormat());

        if (profile.getMacAddress() != null) {
            vo.setMacAddress(profile.getMacAddress());
        }

        vo.setMode(profile.getMode());
        vo.setNetmask(profile.getNetmask());
        vo.setGateway(profile.getGateway());

        if (profile.getBroadCastUri() != null) {
            vo.setBroadcastUri(profile.getBroadCastUri());
        }

        if (profile.getIsolationUri() != null) {
            vo.setIsolationUri(profile.getIsolationUri());
        }

        vo.setState(Nic.State.Allocated);
        
        vo.setIp6Address(profile.getIp6Address());
        vo.setIp6Gateway(profile.getIp6Gateway());
        vo.setIp6Cidr(profile.getIp6Cidr());
        
        return deviceId;
    }

    protected void applyProfileToNicForRelease(NicVO vo, NicProfile profile) {
        vo.setGateway(profile.getGateway());
        vo.setAddressFormat(profile.getFormat());
        vo.setIp4Address(profile.getIp4Address());
        vo.setIp6Address(profile.getIp6Address());
        vo.setMacAddress(profile.getMacAddress());
        if (profile.getReservationStrategy() != null) {
            vo.setReservationStrategy(profile.getReservationStrategy());
        }
        vo.setBroadcastUri(profile.getBroadCastUri());
        vo.setIsolationUri(profile.getIsolationUri());
        vo.setNetmask(profile.getNetmask());
    }

    protected void applyProfileToNetwork(NetworkVO network, NetworkProfile profile) {
        network.setBroadcastUri(profile.getBroadcastUri());
        network.setDns1(profile.getDns1());
        network.setDns2(profile.getDns2());
        network.setPhysicalNetworkId(profile.getPhysicalNetworkId());
    }

    protected NicTO toNicTO(NicVO nic, NicProfile profile, NetworkVO config) {
        NicTO to = new NicTO();
        to.setDeviceId(nic.getDeviceId());
        to.setBroadcastType(config.getBroadcastDomainType());
        to.setType(config.getTrafficType());
        to.setIp(nic.getIp4Address());
        to.setNetmask(nic.getNetmask());
        to.setMac(nic.getMacAddress());
        to.setDns1(profile.getDns1());
        to.setDns2(profile.getDns2());
        if (nic.getGateway() != null) {
            to.setGateway(nic.getGateway());
        } else {
            to.setGateway(config.getGateway());
        }
        to.setDefaultNic(nic.isDefaultNic());
        to.setBroadcastUri(nic.getBroadcastUri());
        to.setIsolationuri(nic.getIsolationUri());
        if (profile != null) {
            to.setDns1(profile.getDns1());
            to.setDns2(profile.getDns2());
        }

        Integer networkRate = _networkModel.getNetworkRate(config.getId(), null);
        to.setNetworkRateMbps(networkRate);

        to.setUuid(config.getUuid());

        return to;
    }

    @Override
    @DB
    public Pair<NetworkGuru, NetworkVO> implementNetwork(long networkId, DeployDestination dest, ReservationContext context)
            throws ConcurrentOperationException, ResourceUnavailableException,
            InsufficientCapacityException {
        Transaction.currentTxn();
        Pair<NetworkGuru, NetworkVO> implemented = new Pair<NetworkGuru, NetworkVO>(null, null);

        NetworkVO network = _networksDao.acquireInLockTable(networkId, _networkLockTimeout);
        if (network == null) {
            // see NetworkVO.java
            ConcurrentOperationException ex = new ConcurrentOperationException("Unable to acquire network configuration");
            ex.addProxyObject(ApiDBUtils.findNetworkById(networkId).getUuid());
            throw ex;
        }

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Lock is acquired for network id " + networkId + " as a part of network implement");
        }

        try {
            NetworkGuru guru = AdapterBase.getAdapterByName(_networkGurus, network.getGuruName());
            Network.State state = network.getState();
            if (state == Network.State.Implemented || state == Network.State.Setup || state == Network.State.Implementing) {
                s_logger.debug("Network id=" + networkId + " is already implemented");
                implemented.set(guru, network);
                return implemented;
            }

            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Asking " + guru.getName() + " to implement " + network);
            }

            NetworkOfferingVO offering = _networkOfferingDao.findById(network.getNetworkOfferingId());

            network.setReservationId(context.getReservationId());
            stateTransitTo(network, Event.ImplementNetwork);

            Network result = guru.implement(network, offering, dest, context);
            network.setCidr(result.getCidr());
            network.setBroadcastUri(result.getBroadcastUri());
            network.setGateway(result.getGateway());
            network.setMode(result.getMode());
            network.setPhysicalNetworkId(result.getPhysicalNetworkId());
            _networksDao.update(networkId, network);

            // implement network elements and re-apply all the network rules
            implementNetworkElementsAndResources(dest, context, network, offering);

            stateTransitTo(network,Event.OperationSucceeded);

            network.setRestartRequired(false);
            _networksDao.update(network.getId(), network);
            implemented.set(guru, network);
            return implemented;
        } catch (NoTransitionException e) {
            s_logger.error(e.getMessage());
            return null;
        } finally {
            if (implemented.first() == null) {
                s_logger.debug("Cleaning up because we're unable to implement the network " + network);
                try {
                    stateTransitTo(network,Event.OperationFailed);
                } catch (NoTransitionException e) {
                    s_logger.error(e.getMessage());
                }

                shutdownNetwork(networkId, context, false);
            }

            _networksDao.releaseFromLockTable(networkId);
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Lock is released for network id " + networkId + " as a part of network implement");
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        return super.equals(o);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public void implementNetworkElementsAndResources(DeployDestination dest, ReservationContext context,
                                                      NetworkVO network, NetworkOfferingVO offering)
            throws ConcurrentOperationException, InsufficientAddressCapacityException, ResourceUnavailableException, InsufficientCapacityException {

        // If this is a 1) guest virtual network 2) network has sourceNat service 3) network offering does not support a
        // Shared source NAT rule,
        // associate a source NAT IP (if one isn't already associated with the network)

        boolean sharedSourceNat = offering.getSharedSourceNat();
        DataCenter zone = _dcDao.findById(network.getDataCenterId());
        if (network.getGuestType() == Network.GuestType.Isolated
               && _networkModel.areServicesSupportedInNetwork(network.getId(), Service.SourceNat)
               && !sharedSourceNat) {

            List<IPAddressVO> ips = null;
            if (network.getVpcId() != null) {
                ips = _ipAddressDao.listByAssociatedVpc(network.getVpcId(), true);
                if (ips.isEmpty()) {
                    throw new CloudRuntimeException("Vpc is not implemented; there is no source nat ip");
                }
            } else {
                ips = _ipAddressDao.listByAssociatedNetwork(network.getId(), true);
            }

            if (ips.isEmpty()) {
                s_logger.debug("Creating a source nat ip for network " + network);
                Account owner = _accountMgr.getAccount(network.getAccountId());
                assignSourceNatIpAddressToGuestNetwork(owner, network);
            }
        }

        // get providers to implement
        List<Provider> providersToImplement = getNetworkProviders(network.getId());
        for (NetworkElement element : _networkElements) {
            if (providersToImplement.contains(element.getProvider())) {
                if (!_networkModel.isProviderEnabledInPhysicalNetwork(_networkModel.getPhysicalNetworkId(network), element.getProvider().getName())) {
                    // The physicalNetworkId will not get translated into a uuid by the reponse serializer,
                    // because the serializer would look up the NetworkVO class's table and retrieve the
                    // network id instead of the physical network id.
                    // So just throw this exception as is. We may need to TBD by changing the serializer.
                    throw new CloudRuntimeException("Service provider " + element.getProvider().getName() +
                            " either doesn't exist or is not enabled in physical network id: " + network.getPhysicalNetworkId());
                }

                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Asking " + element.getName() + " to implemenet " + network);
                }

                if (!element.implement(network, offering, dest, context)) {
                    CloudRuntimeException ex = new CloudRuntimeException("Failed to implement provider " + element.getProvider().getName() + " for network with specified id");
                    ex.addProxyObject(network, network.getId(), "networkId");
                    throw ex;
                }
            }
        }

        // reapply all the firewall/staticNat/lb rules
        s_logger.debug("Reprogramming network " + network + " as a part of network implement");
        if (!reprogramNetworkRules(network.getId(), UserContext.current().getCaller(), network)) {
            s_logger.warn("Failed to re-program the network as a part of network " + network + " implement");
            // see DataCenterVO.java
            ResourceUnavailableException ex = new ResourceUnavailableException("Unable to apply network rules as a part of network " + network + " implement", DataCenter.class, network.getDataCenterId());
            ex.addProxyObject(ApiDBUtils.findZoneById(network.getDataCenterId()).getUuid());
            throw ex;
        }
    }

    protected void prepareElement(NetworkElement element, NetworkVO network,
            NicProfile profile, VirtualMachineProfile<? extends VMInstanceVO> vmProfile,
            DeployDestination dest, ReservationContext context) throws InsufficientCapacityException,
            ConcurrentOperationException, ResourceUnavailableException {
        element.prepare(network, profile, vmProfile, dest, context);
        if (vmProfile.getType() == Type.User && element.getProvider() != null) {
            if (_networkModel.areServicesSupportedInNetwork(network.getId(), Service.Dhcp) &&
                    _networkModel.isProviderSupportServiceInNetwork(network.getId(), Service.Dhcp, element.getProvider()) &&
                    (ComponentContext.getTargetObject(element) instanceof DhcpServiceProvider)) {
                DhcpServiceProvider sp = (DhcpServiceProvider) element;
                sp.addDhcpEntry(network, profile, vmProfile, dest, context);
            }
            if (_networkModel.areServicesSupportedInNetwork(network.getId(), Service.UserData) &&
                    _networkModel.isProviderSupportServiceInNetwork(network.getId(), Service.UserData, element.getProvider()) &&
                    (ComponentContext.getTargetObject(element) instanceof UserDataServiceProvider)) {
                UserDataServiceProvider sp = (UserDataServiceProvider) element;
                sp.addPasswordAndUserdata(network, profile, vmProfile, dest, context);
            }
        }
    }

    @DB
    protected void updateNic(NicVO nic, long networkId, int count) {
        Transaction txn = Transaction.currentTxn();
        txn.start();
        _nicDao.update(nic.getId(), nic);

        if (nic.getVmType() == VirtualMachine.Type.User) {
            s_logger.debug("Changing active number of nics for network id=" + networkId + " on " + count);
            _networksDao.changeActiveNicsBy(networkId, count);
        }

        if (nic.getVmType() == VirtualMachine.Type.User || (nic.getVmType() == VirtualMachine.Type.DomainRouter && _networksDao.findById(networkId).getTrafficType() == TrafficType.Guest)) {
            _networksDao.setCheckForGc(networkId);
        }

        txn.commit();
    }

    @Override
    public void prepare(VirtualMachineProfile<? extends VMInstanceVO> vmProfile, DeployDestination dest, ReservationContext context) throws InsufficientCapacityException,
            ConcurrentOperationException, ResourceUnavailableException {
        List<NicVO> nics = _nicDao.listByVmId(vmProfile.getId());

        // we have to implement default nics first - to ensure that default network elements start up first in multiple
        //nics case
        // (need for setting DNS on Dhcp to domR's Ip4 address)
        Collections.sort(nics, new Comparator<NicVO>() {

            @Override
            public int compare(NicVO nic1, NicVO nic2) {
                boolean isDefault1 = nic1.isDefaultNic();
                boolean isDefault2 = nic2.isDefaultNic();

                return (isDefault1 ^ isDefault2) ? ((isDefault1 ^ true) ? 1 : -1) : 0;
            }
        });

        for (NicVO nic : nics) {
            Pair<NetworkGuru, NetworkVO> implemented = implementNetwork(nic.getNetworkId(), dest, context);

            NetworkVO network = implemented.second();
            NicProfile profile = prepareNic(vmProfile, dest, context, nic.getId(), network);
            vmProfile.addNic(profile);
        }
    }

    @Override
    public NicProfile prepareNic(VirtualMachineProfile<? extends VMInstanceVO> vmProfile, DeployDestination
            dest, ReservationContext context, long nicId, NetworkVO network)
            throws InsufficientVirtualNetworkCapcityException, InsufficientAddressCapacityException,
            ConcurrentOperationException, InsufficientCapacityException, ResourceUnavailableException {
        
        Integer networkRate = _networkModel.getNetworkRate(network.getId(), vmProfile.getId());
        NetworkGuru guru = AdapterBase.getAdapterByName(_networkGurus, network.getGuruName());
        NicVO nic = _nicDao.findById(nicId);

        NicProfile profile = null;
        if (nic.getReservationStrategy() == Nic.ReservationStrategy.Start) {
            nic.setState(Nic.State.Reserving);
            nic.setReservationId(context.getReservationId());
            _nicDao.update(nic.getId(), nic);
            URI broadcastUri = nic.getBroadcastUri();
            if (broadcastUri == null) {
                broadcastUri = network.getBroadcastUri();
            }

            URI isolationUri = nic.getIsolationUri();

            profile = new NicProfile(nic, network, broadcastUri, isolationUri,

            networkRate, _networkModel.isSecurityGroupSupportedInNetwork(network), _networkModel.getNetworkTag(vmProfile.getHypervisorType(), network));
            guru.reserve(profile, network, vmProfile, dest, context);
            nic.setIp4Address(profile.getIp4Address());
            nic.setAddressFormat(profile.getFormat());
            nic.setIp6Address(profile.getIp6Address());
            nic.setMacAddress(profile.getMacAddress());
            nic.setIsolationUri(profile.getIsolationUri());
            nic.setBroadcastUri(profile.getBroadCastUri());
            nic.setReserver(guru.getName());
            nic.setState(Nic.State.Reserved);
            nic.setNetmask(profile.getNetmask());
            nic.setGateway(profile.getGateway());

            if (profile.getStrategy() != null) {
                nic.setReservationStrategy(profile.getStrategy());
            }

            updateNic(nic, network.getId(), 1);
        } else {
            profile = new NicProfile(nic, network, nic.getBroadcastUri(), nic.getIsolationUri(), 
                        networkRate, _networkModel.isSecurityGroupSupportedInNetwork(network), _networkModel.getNetworkTag(vmProfile.getHypervisorType(), network));
            guru.updateNicProfile(profile, network);
            nic.setState(Nic.State.Reserved);
            updateNic(nic, network.getId(), 1);
        }

        for (NetworkElement element : _networkElements) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Asking " + element.getName() + " to prepare for " + nic);
            }
            prepareElement(element, network, profile, vmProfile, dest, context);
        }

        profile.setSecurityGroupEnabled(_networkModel.isSecurityGroupSupportedInNetwork(network));
        guru.updateNicProfile(profile, network);
        return profile;
    }

    @Override
    public <T extends VMInstanceVO> void prepareNicForMigration(VirtualMachineProfile<T> vm, DeployDestination dest) {
        List<NicVO> nics = _nicDao.listByVmId(vm.getId());
        for (NicVO nic : nics) {
            NetworkVO network = _networksDao.findById(nic.getNetworkId());
            Integer networkRate = _networkModel.getNetworkRate(network.getId(), vm.getId());

            NetworkGuru guru = AdapterBase.getAdapterByName(_networkGurus, network.getGuruName());
            NicProfile profile = new NicProfile(nic, network, nic.getBroadcastUri(), nic.getIsolationUri(), networkRate, 
                    _networkModel.isSecurityGroupSupportedInNetwork(network), _networkModel.getNetworkTag(vm.getHypervisorType(), network));
            guru.updateNicProfile(profile, network);
            vm.addNic(profile);
        }
    }

    @Override
    @DB
    public void release(VirtualMachineProfile<? extends VMInstanceVO> vmProfile, boolean forced) throws
            ConcurrentOperationException, ResourceUnavailableException {
        List<NicVO> nics = _nicDao.listByVmId(vmProfile.getId());
        for (NicVO nic : nics) {
            releaseNic(vmProfile, nic);
        }
    }


    @Override
    @DB
    public void releaseNic(VirtualMachineProfile<? extends VMInstanceVO> vmProfile, Nic nic)
            throws ConcurrentOperationException, ResourceUnavailableException {
        NicVO nicVO = _nicDao.findById(nic.getId());
        releaseNic(vmProfile, nicVO);
    }

    @DB
    protected void releaseNic(VirtualMachineProfile<? extends VMInstanceVO> vmProfile, NicVO nicVO)
            throws ConcurrentOperationException, ResourceUnavailableException {
        //lock the nic
        Transaction txn = Transaction.currentTxn();
        txn.start();

        NicVO nic = _nicDao.lockRow(nicVO.getId(), true);
        if (nic == null) {
            throw new ConcurrentOperationException("Unable to acquire lock on nic " + nic);
        }

        Nic.State originalState = nic.getState();
        NetworkVO network = _networksDao.findById(nicVO.getNetworkId());

        if (originalState == Nic.State.Reserved || originalState == Nic.State.Reserving) {
            if (nic.getReservationStrategy() == Nic.ReservationStrategy.Start) {
                NetworkGuru guru = AdapterBase.getAdapterByName(_networkGurus, network.getGuruName());
                nic.setState(Nic.State.Releasing);
                _nicDao.update(nic.getId(), nic);
                NicProfile profile = new NicProfile(nic, network, nic.getBroadcastUri(), nic.getIsolationUri(), null,
                        _networkModel.isSecurityGroupSupportedInNetwork(network), _networkModel.getNetworkTag(vmProfile.getHypervisorType(), network));
                if (guru.release(profile, vmProfile, nic.getReservationId())) {
                    applyProfileToNicForRelease(nic, profile);
                    nic.setState(Nic.State.Allocated);
                    if (originalState == Nic.State.Reserved) {
                        updateNic(nic, network.getId(), -1);
                    } else {
                        _nicDao.update(nic.getId(), nic);
                    }
                }
                //commit the transaction before proceeding releasing nic profile on the network elements
                txn.commit();

                // Perform release on network elements
                for (NetworkElement element : _networkElements) {
                    if (s_logger.isDebugEnabled()) {
                        s_logger.debug("Asking " + element.getName() + " to release " + nic);
                    }
                    //NOTE: Context appear to never be used in release method
                    //implementations. Consider removing it from interface Element
                    element.release(network, profile, vmProfile, null);
                }

            } else {
                nic.setState(Nic.State.Allocated);
                updateNic(nic, network.getId(), -1);
                txn.commit();
            }
        }
    }

    @Override
    public void cleanupNics(VirtualMachineProfile<? extends VMInstanceVO> vm) {
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Cleaning network for vm: " + vm.getId());
        }

        List<NicVO> nics = _nicDao.listByVmId(vm.getId());
        for (NicVO nic : nics) {
            removeNic(vm, nic);
        }
    }

    @Override
    public void removeNic(VirtualMachineProfile<? extends VMInstanceVO> vm, Nic nic) {
        removeNic(vm, _nicDao.findById(nic.getId()));
    }

    protected void removeNic(VirtualMachineProfile<? extends VMInstanceVO> vm, NicVO nic) {
        nic.setState(Nic.State.Deallocating);
        _nicDao.update(nic.getId(), nic);
        NetworkVO network = _networksDao.findById(nic.getNetworkId());
        NicProfile profile = new NicProfile(nic, network, null, null, null,
                _networkModel.isSecurityGroupSupportedInNetwork(network), _networkModel.getNetworkTag(vm.getHypervisorType(), network));
        NetworkGuru guru = AdapterBase.getAdapterByName(_networkGurus, network.getGuruName());
        guru.deallocate(network, profile, vm);
        _nicDao.remove(nic.getId());
        s_logger.debug("Removed nic id=" + nic.getId());
        //remove the secondary ip addresses corresponding to to this nic
        if (!removeVmSecondaryIpsOfNic(nic.getId())) {
            s_logger.debug("Removing nic " + nic.getId() + " secondary ip addreses failed");
        }
    }

    @Override
    public void expungeNics(VirtualMachineProfile<? extends VMInstanceVO> vm) {
        List<NicVO> nics = _nicDao.listByVmIdIncludingRemoved(vm.getId());
        for (NicVO nic : nics) {
            _nicDao.expunge(nic.getId());
        }
    }

   

    @Override
    @DB
    public Network createGuestNetwork(long networkOfferingId, String name, String displayText, String gateway, 
            String cidr, String vlanId, String networkDomain, Account owner, Long domainId,
            PhysicalNetwork pNtwk, long zoneId, ACLType aclType, Boolean subdomainAccess, Long vpcId, String ip6Gateway, String ip6Cidr)
                    throws ConcurrentOperationException, InsufficientCapacityException, ResourceAllocationException {

        NetworkOfferingVO ntwkOff = _networkOfferingDao.findById(networkOfferingId);
        // this method supports only guest network creation
        if (ntwkOff.getTrafficType() != TrafficType.Guest) {
            s_logger.warn("Only guest networks can be created using this method");
            return null;
        }

        boolean updateResourceCount = resourceCountNeedsUpdate(ntwkOff, aclType);
        //check resource limits
        if (updateResourceCount) {
            _resourceLimitMgr.checkResourceLimit(owner, ResourceType.network);
        }

        // Validate network offering
        if (ntwkOff.getState() != NetworkOffering.State.Enabled) {
            // see NetworkOfferingVO
            InvalidParameterValueException ex = new InvalidParameterValueException("Can't use specified network offering id as its stat is not " + NetworkOffering.State.Enabled);
            ex.addProxyObject(ntwkOff, ntwkOff.getId(), "networkOfferingId");
            throw ex;
        }

        // Validate physical network
        if (pNtwk.getState() != PhysicalNetwork.State.Enabled) {
            // see PhysicalNetworkVO.java
            InvalidParameterValueException ex = new InvalidParameterValueException("Specified physical network id is" +
                    " in incorrect state:" + pNtwk.getState());
            ex.addProxyObject("physical_network", pNtwk.getId(), "physicalNetworkId");
            throw ex;
        }

        boolean ipv6 = false;
        
        if (ip6Gateway != null && ip6Cidr != null) {
        	ipv6 = true;
        }
        // Validate zone
        DataCenterVO zone = _dcDao.findById(zoneId);
        if (zone.getNetworkType() == NetworkType.Basic) {
        	if (ipv6) {
                throw new InvalidParameterValueException("IPv6 is not supported in Basic zone");
        	}
        	
            // In Basic zone the network should have aclType=Domain, domainId=1, subdomainAccess=true
            if (aclType == null || aclType != ACLType.Domain) {
                throw new InvalidParameterValueException("Only AclType=Domain can be specified for network creation in Basic zone");
            }

            // Only one guest network is supported in Basic zone
            List<NetworkVO> guestNetworks = _networksDao.listByZoneAndTrafficType(zone.getId(), TrafficType.Guest);
            if (!guestNetworks.isEmpty()) {
                throw new InvalidParameterValueException("Can't have more than one Guest network in zone with network type "
                                                        + NetworkType.Basic);
            }

            // if zone is basic, only Shared network offerings w/o source nat service are allowed
            if (!(ntwkOff.getGuestType() == GuestType.Shared && 
                    !_networkModel.areServicesSupportedByNetworkOffering(ntwkOff.getId(), Service.SourceNat))) {
                throw new InvalidParameterValueException("For zone of type " + NetworkType.Basic + " only offerings of " +
                        "guestType " + GuestType.Shared + " with disabled " + Service.SourceNat.getName()
                        + " service are allowed");
            }

            if (domainId == null || domainId != Domain.ROOT_DOMAIN) {
                throw new InvalidParameterValueException("Guest network in Basic zone should be dedicated to ROOT domain");
            }

            if (subdomainAccess == null) {
                subdomainAccess = true;
            } else if (!subdomainAccess) {
                throw new InvalidParameterValueException("Subdomain access should be set to true for the" +
                        " guest network in the Basic zone");
            }

            if (vlanId == null) {
                vlanId = Vlan.UNTAGGED;
            } else {
                if (!vlanId.equalsIgnoreCase(Vlan.UNTAGGED)) {
                    throw new InvalidParameterValueException("Only vlan " + Vlan.UNTAGGED + " can be created in " +
                            "the zone of type " + NetworkType.Basic);
                }
            }

        } else if (zone.getNetworkType() == NetworkType.Advanced) {
            if (zone.isSecurityGroupEnabled()) {
            	if (ipv6) {
            		throw new InvalidParameterValueException("IPv6 is not supported with security group!");
            	}
                // Only Account specific Isolated network with sourceNat service disabled are allowed in security group
                // enabled zone
                if ( ntwkOff.getGuestType() != GuestType.Shared ){
                    throw new InvalidParameterValueException("Only shared guest network can be created in security group enabled zone");
                }
                if ( _networkModel.areServicesSupportedByNetworkOffering(ntwkOff.getId(), Service.SourceNat)) {
                    throw new InvalidParameterValueException("Service SourceNat is not allowed in security group enabled zone");
                }
                if ( _networkModel.areServicesSupportedByNetworkOffering(ntwkOff.getId(), Service.SecurityGroup)) {
                    throw new InvalidParameterValueException("network must have SecurityGroup provider in security group enabled zone");
                }
            }

            //don't allow eip/elb networks in Advance zone
            if (ntwkOff.getElasticIp() || ntwkOff.getElasticLb()) {
                throw new InvalidParameterValueException("Elastic IP and Elastic LB services are supported in zone of type " + NetworkType.Basic);
            }
        }

        // VlanId can be specified only when network offering supports it
        boolean vlanSpecified = (vlanId != null);
        if (vlanSpecified != ntwkOff.getSpecifyVlan()) {
            if (vlanSpecified) {
                throw new InvalidParameterValueException("Can't specify vlan; corresponding offering says specifyVlan=false");
            } else {
                throw new InvalidParameterValueException("Vlan has to be specified; corresponding offering says specifyVlan=true");
            }
        }

        if (vlanId != null) {
            String uri = "vlan://" + vlanId;
            // For Isolated networks, don't allow to create network with vlan that already exists in the zone
            if (ntwkOff.getGuestType() == GuestType.Isolated) {
                if (_networksDao.countByZoneAndUri(zoneId, uri) > 0) {
                throw new InvalidParameterValueException("Network with vlan " + vlanId + " already exists in zone " + zoneId);
            }
            } else {
                //don't allow to create Shared network with Vlan that already exists in the zone for Isolated networks
                if (_networksDao.countByZoneUriAndGuestType(zoneId, uri, GuestType.Isolated) > 0) {
                    throw new InvalidParameterValueException("Isolated network with vlan " + vlanId + " already exists " +
                            "in zone " + zoneId);
                }
        }
        }

        // If networkDomain is not specified, take it from the global configuration
        if (_networkModel.areServicesSupportedByNetworkOffering(networkOfferingId, Service.Dns)) {
            Map<Network.Capability, String> dnsCapabilities = _networkModel.getNetworkOfferingServiceCapabilities
                    (_configMgr.getNetworkOffering(networkOfferingId), Service.Dns);
            String isUpdateDnsSupported = dnsCapabilities.get(Capability.AllowDnsSuffixModification);
            if (isUpdateDnsSupported == null || !Boolean.valueOf(isUpdateDnsSupported)) {
                if (networkDomain != null) {
                    // TBD: NetworkOfferingId and zoneId. Send uuids instead.
                    throw new InvalidParameterValueException("Domain name change is not supported by network offering id="
                            + networkOfferingId + " in zone id=" + zoneId);
                }
            } else {
                if (networkDomain == null) {
                    // 1) Get networkDomain from the corresponding account/domain/zone
                    if (aclType == ACLType.Domain) {
                        networkDomain = _networkModel.getDomainNetworkDomain(domainId, zoneId);
                    } else if (aclType == ACLType.Account) {
                        networkDomain = _networkModel.getAccountNetworkDomain(owner.getId(), zoneId);
                    }

                    // 2) If null, generate networkDomain using domain suffix from the global config variables
                    if (networkDomain == null) {
                        networkDomain = "cs" + Long.toHexString(owner.getId()) + _networkDomain;
                    }

                } else {
                    // validate network domain
                    if (!NetUtils.verifyDomainName(networkDomain)) {
                        throw new InvalidParameterValueException(
                                "Invalid network domain. Total length shouldn't exceed 190 chars. Each domain " +
                                "label must be between 1 and 63 characters long, can contain ASCII letters 'a' through 'z', the digits '0' through '9', "
                                        + "and the hyphen ('-'); can't start or end with \"-\"");
                    }
                }
            }
        }
        
        // In Advance zone Cidr for Shared networks and Isolated networks w/o source nat service can't be NULL - 2.2.x
        // limitation, remove after we introduce support for multiple ip ranges
        // with different Cidrs for the same Shared network
        boolean cidrRequired = zone.getNetworkType() == NetworkType.Advanced && ntwkOff.getTrafficType() == TrafficType.Guest
                && (ntwkOff.getGuestType() == GuestType.Shared || (ntwkOff.getGuestType() == GuestType.Isolated 
                && !_networkModel.areServicesSupportedByNetworkOffering(ntwkOff.getId(), Service.SourceNat)));
        if (cidr == null && ip6Cidr == null  && cidrRequired) {
            throw new InvalidParameterValueException("StartIp/endIp/gateway/netmask are required when create network of" +
                    " type " + Network.GuestType.Shared + " and network of type " + GuestType.Isolated + " with service "
                    + Service.SourceNat.getName() + " disabled");
        }

        // No cidr can be specified in Basic zone
        if (zone.getNetworkType() == NetworkType.Basic && cidr != null) {
            throw new InvalidParameterValueException("StartIp/endIp/gateway/netmask can't be specified for zone of type " + NetworkType.Basic);
        }

        // Check if cidr is RFC1918 compliant if the network is Guest Isolated for IPv4
        if (cidr != null && ntwkOff.getGuestType() == Network.GuestType.Isolated && ntwkOff.getTrafficType() == TrafficType.Guest) {
        	if (!NetUtils.validateGuestCidr(cidr)) {
        		throw new InvalidParameterValueException("Virtual Guest Cidr " + cidr + " is not RFC1918 compliant");
        	}
        }

        Transaction txn = Transaction.currentTxn();
        txn.start();

        Long physicalNetworkId = null;
        if (pNtwk != null) {
            physicalNetworkId = pNtwk.getId();
        }
        DataCenterDeployment plan = new DataCenterDeployment(zoneId, null, null, null, null, physicalNetworkId);
        NetworkVO userNetwork = new NetworkVO();
        userNetwork.setNetworkDomain(networkDomain);

        if (cidr != null && gateway != null) {
            userNetwork.setCidr(cidr);
            userNetwork.setGateway(gateway);
            if (vlanId != null) {
                userNetwork.setBroadcastUri(URI.create("vlan://" + vlanId));
                userNetwork.setBroadcastDomainType(BroadcastDomainType.Vlan);
                if (!vlanId.equalsIgnoreCase(Vlan.UNTAGGED)) {
                    userNetwork.setBroadcastDomainType(BroadcastDomainType.Vlan);
                } else {
                    userNetwork.setBroadcastDomainType(BroadcastDomainType.Native);
                }
            }
        }
        
        if (ip6Cidr != null && ip6Gateway != null) {
            userNetwork.setIp6Cidr(ip6Cidr);
            userNetwork.setIp6Gateway(ip6Gateway);
            if (vlanId != null) {
                userNetwork.setBroadcastUri(URI.create("vlan://" + vlanId));
                userNetwork.setBroadcastDomainType(BroadcastDomainType.Vlan);
                if (!vlanId.equalsIgnoreCase(Vlan.UNTAGGED)) {
                    userNetwork.setBroadcastDomainType(BroadcastDomainType.Vlan);
                } else {
                    userNetwork.setBroadcastDomainType(BroadcastDomainType.Native);
                }
            }
        }
        
        List<NetworkVO> networks = setupNetwork(owner, ntwkOff, userNetwork, plan, name, displayText, true, domainId,
                aclType, subdomainAccess, vpcId);

        Network network = null;
        if (networks == null || networks.isEmpty()) {
            throw new CloudRuntimeException("Fail to create a network");
        } else {
            if (networks.size() > 0 && networks.get(0).getGuestType() == Network.GuestType.Isolated &&
                    networks.get(0).getTrafficType() == TrafficType.Guest) {
                Network defaultGuestNetwork = networks.get(0);
                for (Network nw : networks) {
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
            _resourceLimitMgr.incrementResourceCount(owner.getId(), ResourceType.network);
        }

        txn.commit();
        UserContext.current().setEventDetails("Network Id: " + network.getId());
        return network;
    }

    @Override
    @DB
    public boolean shutdownNetwork(long networkId, ReservationContext context, boolean cleanupElements) {
        boolean result = false;

        NetworkVO network = _networksDao.lockRow(networkId, true);
        if (network == null) {
            s_logger.debug("Unable to find network with id: " + networkId);
            return false;
        }
        if (network.getState() != Network.State.Implemented && network.getState() != Network.State.Shutdown) {
            s_logger.debug("Network is not implemented: " + network);
            return false;
        }
        try {
            stateTransitTo(network, Event.DestroyNetwork);
        } catch (NoTransitionException e) {
            network.setState(Network.State.Shutdown);
            _networksDao.update(network.getId(), network);
        }

        boolean success = shutdownNetworkElementsAndResources(context, cleanupElements, network);

        Transaction txn = Transaction.currentTxn();
        txn.start();
        if (success) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Network id=" + networkId + " is shutdown successfully, cleaning up corresponding resources now.");
            }
            NetworkGuru guru = AdapterBase.getAdapterByName(_networkGurus, network.getGuruName());
            NetworkProfile profile = convertNetworkToNetworkProfile(network.getId());
            guru.shutdown(profile, _networkOfferingDao.findById(network.getNetworkOfferingId()));

            applyProfileToNetwork(network, profile);
            try {
                stateTransitTo(network, Event.OperationSucceeded);
            } catch (NoTransitionException e) {
                network.setState(Network.State.Allocated);
                network.setRestartRequired(false);
            }
            _networksDao.update(network.getId(), network);
            _networksDao.clearCheckForGc(networkId);
            result = true;
        } else {
            try {
                stateTransitTo(network, Event.OperationFailed);
            } catch (NoTransitionException e) {
                network.setState(Network.State.Implemented);
                _networksDao.update(network.getId(), network);
            }
            result = false;
        }
        txn.commit();
        return result;
    }

    @Override
    public boolean shutdownNetworkElementsAndResources(ReservationContext context, boolean cleanupElements, NetworkVO network) {
        // 1) Cleanup all the rules for the network. If it fails, just log the failure and proceed with shutting down
        // the elements
        boolean cleanupResult = true;
        try {
            cleanupResult = shutdownNetworkResources(network.getId(), context.getAccount(), context.getCaller().getId());
        } catch (Exception ex) {
            s_logger.warn("shutdownNetworkRules failed during the network " + network + " shutdown due to ", ex);
        } finally {
            // just warn the administrator that the network elements failed to shutdown
            if (!cleanupResult) {
                s_logger.warn("Failed to cleanup network id=" + network.getId() + " resources as a part of shutdownNetwork");
            }
        }

        // 2) Shutdown all the network elements
        // get providers to shutdown
        List<Provider> providersToShutdown = getNetworkProviders(network.getId());
        boolean success = true;
        for (NetworkElement element : _networkElements) {
            if (providersToShutdown.contains(element.getProvider())) {
                try {
                    if (!_networkModel.isProviderEnabledInPhysicalNetwork(_networkModel.getPhysicalNetworkId(network), element.getProvider().getName())) {
                        s_logger.warn("Unable to complete shutdown of the network elements due to element: " + element.getName() + " either doesn't exist or not enabled in the physical network "
                                + _networkModel.getPhysicalNetworkId(network));
                        success = false;
                    }
                    if (s_logger.isDebugEnabled()) {
                        s_logger.debug("Sending network shutdown to " + element.getName());
                    }
                    if (!element.shutdown(network, context, cleanupElements)) {
                        s_logger.warn("Unable to complete shutdown of the network elements due to element: " + element.getName());
                        success = false;
                    }
                } catch (ResourceUnavailableException e) {
                    s_logger.warn("Unable to complete shutdown of the network elements due to element: " + element.getName(), e);
                    success = false;
                } catch (ConcurrentOperationException e) {
                    s_logger.warn("Unable to complete shutdown of the network elements due to element: " + element.getName(), e);
                    success = false;
                } catch (Exception e) {
                    s_logger.warn("Unable to complete shutdown of the network elements due to element: " + element.getName(), e);
                    success = false;
                }
            }
        }
        return success;
    }

    @Override
    @DB
    public boolean destroyNetwork(long networkId, ReservationContext context) {
        Account callerAccount = _accountMgr.getAccount(context.getCaller().getAccountId());

        NetworkVO network = _networksDao.findById(networkId);
        if (network == null) {
            s_logger.debug("Unable to find network with id: " + networkId);
            return false;
        }

        // Don't allow to delete network via api call when it has vms assigned to it
        int nicCount = getActiveNicsInNetwork(networkId);
        if (nicCount > 0) {
            s_logger.debug("Unable to remove the network id=" + networkId + " as it has active Nics.");
            return false;
        }

        // Make sure that there are no user vms in the network that are not Expunged/Error
        List<UserVmVO> userVms = _userVmDao.listByNetworkIdAndStates(networkId);

        for (UserVmVO vm : userVms) {
            if (!(vm.getState() == VirtualMachine.State.Expunging && vm.getRemoved() != null)) {
                s_logger.warn("Can't delete the network, not all user vms are expunged. Vm " + vm + " is in " + vm.getState() + " state");
                return false;
            }
        }

        //In Basic zone, make sure that there are no non-removed console proxies and SSVMs using the network
        DataCenter zone = _configMgr.getZone(network.getDataCenterId());
        if (zone.getNetworkType() == NetworkType.Basic) {
            List<VMInstanceVO> systemVms = _vmDao.listNonRemovedVmsByTypeAndNetwork(network.getId(),
                    Type.ConsoleProxy, Type.SecondaryStorageVm);
            if (systemVms != null && !systemVms.isEmpty()) {
                s_logger.warn("Can't delete the network, not all consoleProxy/secondaryStorage vms are expunged");
                return false;
            }
        }

        // Shutdown network first
        shutdownNetwork(networkId, context, false);

        // get updated state for the network
        network = _networksDao.findById(networkId);
        if (network.getState() != Network.State.Allocated && network.getState() != Network.State.Setup) {
            s_logger.debug("Network is not not in the correct state to be destroyed: " + network.getState());
            return false;
        }

        boolean success = true;
        if (!cleanupNetworkResources(networkId, callerAccount, context.getCaller().getId())) {
            s_logger.warn("Unable to delete network id=" + networkId + ": failed to cleanup network resources");
            return false;
        }

        // get providers to destroy
        List<Provider> providersToDestroy = getNetworkProviders(network.getId());
        for (NetworkElement element : _networkElements) {
            if (providersToDestroy.contains(element.getProvider())) {
                try {
                    if (!_networkModel.isProviderEnabledInPhysicalNetwork(_networkModel.getPhysicalNetworkId(network), element.getProvider().getName())) {
                        s_logger.warn("Unable to complete destroy of the network elements due to element: " + element.getName() + " either doesn't exist or not enabled in the physical network "
                                + _networkModel.getPhysicalNetworkId(network));
                        success = false;
                    }

                    if (s_logger.isDebugEnabled()) {
                        s_logger.debug("Sending destroy to " + element);
                    }

                    if (!element.destroy(network, context)) {
                        success = false;
                        s_logger.warn("Unable to complete destroy of the network: failed to destroy network element " + element.getName());
                    }
                } catch (ResourceUnavailableException e) {
                    s_logger.warn("Unable to complete destroy of the network due to element: " + element.getName(), e);
                    success = false;
                } catch (ConcurrentOperationException e) {
                    s_logger.warn("Unable to complete destroy of the network due to element: " + element.getName(), e);
                    success = false;
                } catch (Exception e) {
                    s_logger.warn("Unable to complete destroy of the network due to element: " + element.getName(), e);
                    success = false;
                }
            }
        }

        if (success) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Network id=" + networkId + " is destroyed successfully, cleaning up corresponding resources now.");
            }
            NetworkGuru guru = AdapterBase.getAdapterByName(_networkGurus, network.getGuruName());
            Account owner = _accountMgr.getAccount(network.getAccountId());

            Transaction txn = Transaction.currentTxn();
            txn.start();
            guru.trash(network, _networkOfferingDao.findById(network.getNetworkOfferingId()), owner);

            if (!deleteVlansInNetwork(network.getId(), context.getCaller().getId(), callerAccount)) {
                success = false;
                s_logger.warn("Failed to delete network " + network + "; was unable to cleanup corresponding ip ranges");
            } else {
                // commit transaction only when ips and vlans for the network are released successfully
                try {
                    stateTransitTo(network, Event.DestroyNetwork);
                 } catch (NoTransitionException e) {
                     s_logger.debug(e.getMessage());
                 }
                _networksDao.remove(network.getId());

                NetworkOffering ntwkOff = _configMgr.getNetworkOffering(network.getNetworkOfferingId());
                boolean updateResourceCount = resourceCountNeedsUpdate(ntwkOff, network.getAclType());
                if (updateResourceCount) {
                    _resourceLimitMgr.decrementResourceCount(owner.getId(), ResourceType.network);
                }
                txn.commit();
            }
        }

        return success;
    }

    private boolean resourceCountNeedsUpdate(NetworkOffering ntwkOff, ACLType aclType) {
        boolean updateResourceCount = (!ntwkOff.getSpecifyVlan() && aclType == ACLType.Account);
        return updateResourceCount;
    }

    protected boolean deleteVlansInNetwork(long networkId, long userId, Account callerAccount) {

        //cleanup Public vlans
        List<VlanVO> publicVlans = _vlanDao.listVlansByNetworkId(networkId);
        boolean result = true;
        for (VlanVO vlan : publicVlans) {
            if (!_configMgr.deleteVlanAndPublicIpRange(_accountMgr.getSystemUser().getId(), vlan.getId(), callerAccount)) {
                s_logger.warn("Failed to delete vlan " + vlan.getId() + ");");
                result = false;
            }
        }

        //cleanup private vlans
        int privateIpAllocCount = _privateIpDao.countAllocatedByNetworkId(networkId);
        if (privateIpAllocCount > 0) {
            s_logger.warn("Can't delete Private ip range for network " + networkId + " as it has allocated ip addresses");
            result = false;
        } else {
            _privateIpDao.deleteByNetworkId(networkId);
            s_logger.debug("Deleted ip range for private network id=" + networkId);
        }
        return result;
    }

    @Override
    public boolean applyRules(List<? extends FirewallRule> rules, FirewallRule.Purpose purpose,
            NetworkRuleApplier applier, boolean continueOnError) throws ResourceUnavailableException {
        if (rules == null || rules.size() == 0) {
            s_logger.debug("There are no rules to forward to the network elements");
            return true;
        }

        boolean success = true;
        Network network = _networksDao.findById(rules.get(0).getNetworkId());
        FirewallRuleVO.TrafficType trafficType = rules.get(0).getTrafficType();
        List<PublicIp> publicIps = new ArrayList<PublicIp>();

        if (!(rules.get(0).getPurpose() == FirewallRule.Purpose.Firewall && trafficType == FirewallRule.TrafficType.Egress)) {
            // get the list of public ip's owned by the network
            List<IPAddressVO> userIps = _ipAddressDao.listByAssociatedNetwork(network.getId(), null);
            if (userIps != null && !userIps.isEmpty()) {
                for (IPAddressVO userIp : userIps) {
                    PublicIp publicIp = PublicIp.createFromAddrAndVlan(userIp, _vlanDao.findById(userIp.getVlanId()));
                    publicIps.add(publicIp);
                }
            }

            // rules can not programmed unless IP is associated with network
            // service provider, so run IP assoication for
            // the network so as to ensure IP is associated before applying
            // rules (in add state)
            applyIpAssociations(network, false, continueOnError, publicIps);
        }

        try {
            applier.applyRules(network, purpose, rules);
        } catch (ResourceUnavailableException e) {
            if (!continueOnError) {
                throw e;
            }
            s_logger.warn("Problems with applying " + purpose + " rules but pushing on", e);
            success = false;
        }

        if (!(rules.get(0).getPurpose() == FirewallRule.Purpose.Firewall && trafficType == FirewallRule.TrafficType.Egress)) {
            // if all the rules configured on public IP are revoked then
            // dis-associate IP with network service provider
            applyIpAssociations(network, true, continueOnError, publicIps);
        }

        return success;
    }

    public class NetworkGarbageCollector implements Runnable {

        @Override
        public void run() {
            try {
                List<Long> shutdownList = new ArrayList<Long>();
                long currentTime = System.currentTimeMillis() >> 10;
                HashMap<Long, Long> stillFree = new HashMap<Long, Long>();

                List<Long> networkIds = _networksDao.findNetworksToGarbageCollect();
                for (Long networkId : networkIds) {
                    Long time = _lastNetworkIdsToFree.remove(networkId);
                    if (time == null) {
                        if (s_logger.isDebugEnabled()) {
                            s_logger.debug("We found network " + networkId + " to be free for the first time.  Adding it to the list: " + currentTime);
                        }
                        stillFree.put(networkId, currentTime);
                    } else if (time > (currentTime - _networkGcWait)) {
                        if (s_logger.isDebugEnabled()) {
                            s_logger.debug("Network " + networkId + " is still free but it's not time to shutdown yet: " + time);
                        }
                        stillFree.put(networkId, time);
                    } else {
                        shutdownList.add(networkId);
                    }
                }

                _lastNetworkIdsToFree = stillFree;

                for (Long networkId : shutdownList) {

                    // If network is removed, unset gc flag for it
                    if (_networksDao.findById(networkId) == null) {
                        s_logger.debug("Network id=" + networkId + " is removed, so clearing up corresponding gc check");
                        _networksDao.clearCheckForGc(networkId);
                    } else {
                        try {

                            User caller = _accountMgr.getSystemUser();
                            Account owner = _accountMgr.getAccount(_networksDao.findById(networkId).getAccountId());

                            ReservationContext context = new ReservationContextImpl(null, null, caller, owner);

                            shutdownNetwork(networkId, context, false);
                        } catch (Exception e) {
                            s_logger.warn("Unable to shutdown network: " + networkId);
                        }
                    }
                }
            } catch (Exception e) {
                s_logger.warn("Caught exception while running network gc: ", e);
            }
        }
    }

    

    @Override
    public boolean startNetwork(long networkId, DeployDestination dest, ReservationContext context) throws ConcurrentOperationException, ResourceUnavailableException, InsufficientCapacityException {

        // Check if network exists
        NetworkVO network = _networksDao.findById(networkId);
        if (network == null) {
            InvalidParameterValueException ex = new InvalidParameterValueException("Network with specified id doesn't exist");
            ex.addProxyObject(network, networkId, "networkId");
            throw ex;
        }

        // implement the network
        s_logger.debug("Starting network " + network + "...");
        Pair<NetworkGuru, NetworkVO> implementedNetwork = implementNetwork(networkId, dest, context);
        if (implementedNetwork.first() == null) {
            s_logger.warn("Failed to start the network " + network);
            return false;
        } else {
            return true;
        }
    }

    @Override
    public boolean restartNetwork(Long networkId, Account callerAccount, User callerUser, boolean cleanup) throws ConcurrentOperationException, ResourceUnavailableException, InsufficientCapacityException {

        NetworkVO network = _networksDao.findById(networkId);

        s_logger.debug("Restarting network " + networkId + "...");

        ReservationContext context = new ReservationContextImpl(null, null, callerUser, callerAccount);

        if (cleanup) {
            // shutdown the network
            s_logger.debug("Shutting down the network id=" + networkId + " as a part of network restart");

            if (!shutdownNetworkElementsAndResources(context, true, network)) {
                s_logger.debug("Failed to shutdown the network elements and resources as a part of network restart: " + network.getState());
                setRestartRequired(network, true);
                return false;
            }
        } else {
            s_logger.debug("Skip the shutting down of network id=" + networkId);
        }

        // implement the network elements and rules again
        DeployDestination dest = new DeployDestination(_dcDao.findById(network.getDataCenterId()), null, null, null);

        s_logger.debug("Implementing the network " + network + " elements and resources as a part of network restart");
        NetworkOfferingVO offering = _networkOfferingDao.findById(network.getNetworkOfferingId());

        try {
            implementNetworkElementsAndResources(dest, context, network, offering);
            setRestartRequired(network, true);
        } catch (Exception ex) {
            s_logger.warn("Failed to implement network " + network + " elements and resources as a part of network restart due to ", ex);
            return false;
        }
        setRestartRequired(network, false);
        return true;
    }

    private void setRestartRequired(NetworkVO network, boolean restartRequired) {
        s_logger.debug("Marking network " + network + " with restartRequired=" + restartRequired);
        network.setRestartRequired(restartRequired);
        _networksDao.update(network.getId(), network);
    }

    // This method re-programs the rules/ips for existing network
    protected boolean reprogramNetworkRules(long networkId, Account caller, NetworkVO network) throws ResourceUnavailableException {
        boolean success = true;
        // associate all ip addresses
        if (!applyIpAssociations(network, false)) {
            s_logger.warn("Failed to apply ip addresses as a part of network id" + networkId + " restart");
            success = false;
        }

        // apply static nat
        if (!_rulesMgr.applyStaticNatsForNetwork(networkId, false, caller)) {
            s_logger.warn("Failed to apply static nats a part of network id" + networkId + " restart");
            success = false;
        }

        // apply firewall rules
        List<FirewallRuleVO> firewallIngressRulesToApply = _firewallDao.listByNetworkPurposeTrafficType(networkId, Purpose.Firewall, FirewallRule.TrafficType.Ingress);
        if (!_firewallMgr.applyFirewallRules(firewallIngressRulesToApply, false, caller)) {
            s_logger.warn("Failed to reapply Ingress firewall rule(s) as a part of network id=" + networkId + " restart");
            success = false;
        }

        List<FirewallRuleVO> firewallEgressRulesToApply = _firewallDao.listByNetworkPurposeTrafficType(networkId, Purpose.Firewall, FirewallRule.TrafficType.Egress);
        if (!_firewallMgr.applyFirewallRules(firewallEgressRulesToApply, false, caller)) {
            s_logger.warn("Failed to reapply firewall Egress rule(s) as a part of network id=" + networkId + " restart");
            success = false;
        }

        // apply port forwarding rules
        if (!_rulesMgr.applyPortForwardingRulesForNetwork(networkId, false, caller)) {
            s_logger.warn("Failed to reapply port forwarding rule(s) as a part of network id=" + networkId + " restart");
            success = false;
        }

        // apply static nat rules
        if (!_rulesMgr.applyStaticNatRulesForNetwork(networkId, false, caller)) {
            s_logger.warn("Failed to reapply static nat rule(s) as a part of network id=" + networkId + " restart");
            success = false;
        }

        // apply load balancer rules
        if (!_lbMgr.applyLoadBalancersForNetwork(networkId)) {
            s_logger.warn("Failed to reapply load balancer rules as a part of network id=" + networkId + " restart");
            success = false;
        }

        // apply vpn rules
        List<? extends RemoteAccessVpn> vpnsToReapply = _vpnMgr.listRemoteAccessVpns(networkId);
        if (vpnsToReapply != null) {
            for (RemoteAccessVpn vpn : vpnsToReapply) {
                // Start remote access vpn per ip
                if (_vpnMgr.startRemoteAccessVpn(vpn.getServerAddressId(), false) == null) {
                    s_logger.warn("Failed to reapply vpn rules as a part of network id=" + networkId + " restart");
                    success = false;
                }
            }
        }

        //apply network ACLs
        if (!_networkACLMgr.applyNetworkACLs(networkId, caller)) {
            s_logger.warn("Failed to reapply network ACLs as a part of  of network id=" + networkId + " restart");
            success = false;
        }

        return success;
    }

    
    protected int getActiveNicsInNetwork(long networkId) {
        return _networksDao.getActiveNicsIn(networkId);
    }

    
    @Override
    @DB
    public boolean associateIpAddressListToAccount(long userId, long accountId, long zoneId, Long vlanId, Network guestNetwork)
            throws InsufficientCapacityException, ConcurrentOperationException,
            ResourceUnavailableException, ResourceAllocationException {
        Account owner = _accountMgr.getActiveAccountById(accountId);
        boolean createNetwork = false;

        if (guestNetwork != null && guestNetwork.getTrafficType() != TrafficType.Guest) {
            throw new InvalidParameterValueException("Network " + guestNetwork + " is not of a type " + TrafficType.Guest);
        }

        Transaction txn = Transaction.currentTxn();
        txn.start();

        if (guestNetwork == null) {
            List<? extends Network> networks = getIsolatedNetworksWithSourceNATOwnedByAccountInZone(zoneId, owner);
            if (networks.size() == 0) {
                createNetwork = true;
            } else if (networks.size() == 1)  {
                guestNetwork = networks.get(0);
            } else {
                throw new InvalidParameterValueException("Error, more than 1 Guest Isolated Networks with SourceNAT " +
                        "service enabled found for this account, cannot assosiate the IP range, please provide the network ID");
            }
        }

        // create new Virtual network (Isolated with SourceNAT) for the user if it doesn't exist
        List<NetworkOfferingVO> requiredOfferings = _networkOfferingDao.listByAvailability(Availability.Required, false);
        if (requiredOfferings.size() < 1) {
            throw new CloudRuntimeException("Unable to find network offering with availability=" +
        Availability.Required + " to automatically create the network as part of createVlanIpRange");
        }
        if (createNetwork) {
            if (requiredOfferings.get(0).getState() == NetworkOffering.State.Enabled) {
                long physicalNetworkId = _networkModel.findPhysicalNetworkId(zoneId, requiredOfferings.get(0).getTags(), requiredOfferings.get(0).getTrafficType());
                // Validate physical network
                PhysicalNetwork physicalNetwork = _physicalNetworkDao.findById(physicalNetworkId);
                if (physicalNetwork == null) {
                    throw new InvalidParameterValueException("Unable to find physical network with id: "+physicalNetworkId   + " and tag: " +requiredOfferings.get(0).getTags());
                }

                s_logger.debug("Creating network for account " + owner + " from the network offering id=" +
            requiredOfferings.get(0).getId() + " as a part of createVlanIpRange process");
                guestNetwork = createGuestNetwork(requiredOfferings.get(0).getId(), owner.getAccountName() + "-network"
                        , owner.getAccountName() + "-network", null, null, null, null, owner, null, physicalNetwork,
                        zoneId, ACLType.Account,
                        null, null, null, null);
                if (guestNetwork == null) {
                    s_logger.warn("Failed to create default Virtual network for the account " + accountId + "in zone " + zoneId);
                    throw new CloudRuntimeException("Failed to create a Guest Isolated Networks with SourceNAT " +
                            "service enabled as a part of createVlanIpRange, for the account " + accountId + "in zone " + zoneId);
                }
            } else {
                throw new CloudRuntimeException("Required network offering id=" + requiredOfferings.get(0).getId()
                        + " is not in " + NetworkOffering.State.Enabled);
            }
        }

        // Check if there is a source nat ip address for this account; if not - we have to allocate one
        boolean allocateSourceNat = false;
        List<IPAddressVO> sourceNat = _ipAddressDao.listByAssociatedNetwork(guestNetwork.getId(), true);
        if (sourceNat.isEmpty()) {
            allocateSourceNat = true;
        }

        // update all ips with a network id, mark them as allocated and update resourceCount/usage
        List<IPAddressVO> ips = _ipAddressDao.listByVlanId(vlanId);
        boolean isSourceNatAllocated = false;
        for (IPAddressVO addr : ips) {
            if (addr.getState() != State.Allocated) {
                if (!isSourceNatAllocated && allocateSourceNat) {
                    addr.setSourceNat(true);
                    isSourceNatAllocated = true;
                } else {
                    addr.setSourceNat(false);
                }
                addr.setAssociatedWithNetworkId(guestNetwork.getId());
                addr.setVpcId(guestNetwork.getVpcId());
                addr.setAllocatedTime(new Date());
                addr.setAllocatedInDomainId(owner.getDomainId());
                addr.setAllocatedToAccountId(owner.getId());
                addr.setSystem(false);
                addr.setState(IpAddress.State.Allocating);
                markPublicIpAsAllocated(addr);
            }
        }

        txn.commit();

        // if the network offering has persistent set to true, implement the network
        if ( createNetwork && requiredOfferings.get(0).getIsPersistent() ) {
            DataCenter zone = _dcDao.findById(zoneId);
            DeployDestination dest = new DeployDestination(zone, null, null, null);
            Account callerAccount = UserContext.current().getCaller();
            UserVO callerUser = _userDao.findById(UserContext.current().getCallerUserId());
            Journal journal = new Journal.LogJournal("Implementing " + guestNetwork, s_logger);
            ReservationContext context = new ReservationContextImpl(UUID.randomUUID().toString(), journal, callerUser, callerAccount);
            s_logger.debug("Implementing network " + guestNetwork + " as a part of network provision for persistent network");
            try {
                Pair<NetworkGuru, NetworkVO> implementedNetwork = implementNetwork(guestNetwork.getId(), dest, context);
                if (implementedNetwork.first() == null) {
                    s_logger.warn("Failed to implement the network " + guestNetwork);
                }
                guestNetwork = implementedNetwork.second();
            } catch (Exception ex) {
                s_logger.warn("Failed to implement network " + guestNetwork + " elements and resources as a part of" +
                        " network provision due to ", ex);
                CloudRuntimeException e = new CloudRuntimeException("Failed to implement network (with specified id)" +
                        " elements and resources as a part of network provision for persistent network");
                e.addProxyObject(guestNetwork, guestNetwork.getId(), "networkId");
                throw e;
            }
        }
        return true;
    }

    @Override
    public NetworkProfile convertNetworkToNetworkProfile(long networkId) {
        NetworkVO network = _networksDao.findById(networkId);
        NetworkGuru guru = AdapterBase.getAdapterByName(_networkGurus, network.getGuruName());
        NetworkProfile profile = new NetworkProfile(network);
        guru.updateNetworkProfile(profile);

        return profile;
    }

    @Override
    public UserDataServiceProvider getPasswordResetProvider(Network network) {
        String passwordProvider = _ntwkSrvcDao.getProviderForServiceInNetwork(network.getId(), Service.UserData);

        if (passwordProvider == null) {
            s_logger.debug("Network " + network + " doesn't support service " + Service.UserData.getName());
            return null;
        }
        
        return (UserDataServiceProvider)_networkModel.getElementImplementingProvider(passwordProvider);
    }

    @Override
    public UserDataServiceProvider getSSHKeyResetProvider(Network network) {
        String SSHKeyProvider = _ntwkSrvcDao.getProviderForServiceInNetwork(network.getId(), Service.UserData);

        if (SSHKeyProvider == null) {
            s_logger.debug("Network " + network + " doesn't support service " + Service.UserData.getName());
            return null;
        }

        return (UserDataServiceProvider)_networkModel.getElementImplementingProvider(SSHKeyProvider);
    }

    protected boolean isSharedNetworkOfferingWithServices(long networkOfferingId) {
        NetworkOfferingVO networkOffering = _networkOfferingDao.findById(networkOfferingId);
        if ( (networkOffering.getGuestType()  == Network.GuestType.Shared) && (
                _networkModel.areServicesSupportedByNetworkOffering(networkOfferingId, Service.SourceNat) ||
                _networkModel.areServicesSupportedByNetworkOffering(networkOfferingId, Service.StaticNat) ||
                _networkModel.areServicesSupportedByNetworkOffering(networkOfferingId, Service.Firewall) ||
                _networkModel.areServicesSupportedByNetworkOffering(networkOfferingId, Service.PortForwarding) ||
                _networkModel.areServicesSupportedByNetworkOffering(networkOfferingId, Service.Lb))) {
            return true;
        }
        return false;
    }

    @Override
    public boolean cleanupIpResources(long ipId, long userId, Account caller) {
        boolean success = true;

        // Revoke all firewall rules for the ip
        try {
            s_logger.debug("Revoking all " + Purpose.Firewall + "rules as a part of public IP id=" + ipId + " release...");
            if (!_firewallMgr.revokeFirewallRulesForIp(ipId, userId, caller)) {
                s_logger.warn("Unable to revoke all the firewall rules for ip id=" + ipId + " as a part of ip release");
                success = false;
            }
        } catch (ResourceUnavailableException e) {
            s_logger.warn("Unable to revoke all firewall rules for ip id=" + ipId + " as a part of ip release", e);
            success = false;
        }

        // Revoke all PF/Static nat rules for the ip
        try {
            s_logger.debug("Revoking all " + Purpose.PortForwarding + "/" + Purpose.StaticNat + " rules as a part of public IP id=" + ipId + " release...");
            if (!_rulesMgr.revokeAllPFAndStaticNatRulesForIp(ipId, userId, caller)) {
                s_logger.warn("Unable to revoke all the port forwarding rules for ip id=" + ipId + " as a part of ip release");
                success = false;
            }
        } catch (ResourceUnavailableException e) {
            s_logger.warn("Unable to revoke all the port forwarding rules for ip id=" + ipId + " as a part of ip release", e);
            success = false;
        }

        s_logger.debug("Revoking all " + Purpose.LoadBalancing + " rules as a part of public IP id=" + ipId + " release...");
        if (!_lbMgr.removeAllLoadBalanacersForIp(ipId, caller, userId)) {
            s_logger.warn("Unable to revoke all the load balancer rules for ip id=" + ipId + " as a part of ip release");
            success = false;
        }

        // remote access vpn can be enabled only for static nat ip, so this part should never be executed under normal
        // conditions
        // only when ip address failed to be cleaned up as a part of account destroy and was marked as Releasing, this part of
        // the code would be triggered
        s_logger.debug("Cleaning up remote access vpns as a part of public IP id=" + ipId + " release...");
        try {
            _vpnMgr.destroyRemoteAccessVpn(ipId, caller);
        } catch (ResourceUnavailableException e) {
            s_logger.warn("Unable to destroy remote access vpn for ip id=" + ipId + " as a part of ip release", e);
            success = false;
        }

        return success;
    }

    @DB
    @Override
    public IPAddressVO markIpAsUnavailable(long addrId) {
        Transaction txn = Transaction.currentTxn();

        IPAddressVO ip = _ipAddressDao.findById(addrId);

        if (ip.getAllocatedToAccountId() == null && ip.getAllocatedTime() == null) {
            s_logger.trace("Ip address id=" + addrId + " is already released");
            return ip;
        }

        if (ip.getState() != State.Releasing) {
            txn.start();

            // don't decrement resource count for direct ips
            if (ip.getAssociatedWithNetworkId() != null) {
                _resourceLimitMgr.decrementResourceCount(_ipAddressDao.findById(addrId).getAllocatedToAccountId(), ResourceType.public_ip);
            }

            // Save usage event
            if (ip.getAllocatedToAccountId() != Account.ACCOUNT_ID_SYSTEM) {
                VlanVO vlan = _vlanDao.findById(ip.getVlanId());

                String guestType = vlan.getVlanType().toString();

                UsageEventUtils.publishUsageEvent(EventTypes.EVENT_NET_IP_RELEASE,
                        ip.getAllocatedToAccountId(), ip.getDataCenterId(), addrId, ip.getAddress().addr(),
                        ip.isSourceNat(), guestType, ip.getSystem(), ip.getClass().getName(), ip.getUuid());
            }

            ip = _ipAddressDao.markAsUnavailable(addrId);

            txn.commit();
        }

        return ip;
    }

    

    Random _rand = new Random(System.currentTimeMillis());

    public List<? extends Nic> listVmNics(Long vmId, Long nicId) {
        List<NicVO> result = null;
        if (nicId == null) {
            result = _nicDao.listByVmId(vmId);
        } else {
            result = _nicDao.listByVmIdAndNicId(vmId, nicId);
        }
        return result;
    }

    public String allocateGuestIP(Account ipOwner, boolean isSystem, long zoneId, Long networkId, String requestedIp)
    throws InsufficientAddressCapacityException {
        String ipaddr = null;
        Account caller = UserContext.current().getCaller();
        long callerUserId = UserContext.current().getCallerUserId();
        // check permissions
        DataCenter zone = _configMgr.getZone(zoneId);
        Network network = _networksDao.findById(networkId);

        _accountMgr.checkAccess(caller, null, false, network);

        ipaddr = acquireGuestIpAddress(network, requestedIp);
        return ipaddr;
    }


    @Override
    @DB
    public String acquireGuestIpAddress(Network network, String requestedIp) {
        if (requestedIp != null && requestedIp.equals(network.getGateway())) {
            s_logger.warn("Requested ip address " + requestedIp + " is used as a gateway address in network " + network);
            return null;
        }

        Set<Long> availableIps = _networkModel.getAvailableIps(network, requestedIp);

        if (availableIps == null || availableIps.isEmpty()) {
            return null;
        }

        Long[] array = availableIps.toArray(new Long[availableIps.size()]);

        if (requestedIp != null) {
            // check that requested ip has the same cidr
            String[] cidr = network.getCidr().split("/");
            boolean isSameCidr = NetUtils.sameSubnetCIDR(requestedIp, NetUtils.long2Ip(array[0]), Integer.parseInt(cidr[1]));
            if (!isSameCidr) {
                s_logger.warn("Requested ip address " + requestedIp + " doesn't belong to the network " + network + " cidr");
                return null;
            } else {
                return requestedIp;
            }
        }

        String result;
        do {
            result = NetUtils.long2Ip(array[_rand.nextInt(array.length)]);
        } while (result.split("\\.")[3].equals("1"));
        return result;
    }

    
    @Override
    public boolean applyStaticNats(List<? extends StaticNat> staticNats, boolean continueOnError) throws ResourceUnavailableException {
        Network network = _networksDao.findById(staticNats.get(0).getNetworkId());
        boolean success = true;

        if (staticNats == null || staticNats.size() == 0) {
            s_logger.debug("There are no static nat rules for the network elements");
            return true;
        }

        // get the list of public ip's owned by the network
        List<IPAddressVO> userIps = _ipAddressDao.listByAssociatedNetwork(network.getId(), null);
        List<PublicIp> publicIps = new ArrayList<PublicIp>();
        if (userIps != null && !userIps.isEmpty()) {
            for (IPAddressVO userIp : userIps) {
                PublicIp publicIp = PublicIp.createFromAddrAndVlan(userIp, _vlanDao.findById(userIp.getVlanId()));
                publicIps.add(publicIp);
            }
        }

        // static NAT rules can not programmed unless IP is associated with network service provider, so run IP
        // association for the network so as to ensure IP is associated before applying rules (in add state)
        applyIpAssociations(network, false, continueOnError, publicIps);

        // get provider
        StaticNatServiceProvider element = getStaticNatProviderForNetwork(network);
        try {
            success = element.applyStaticNats(network, staticNats);
        } catch (ResourceUnavailableException e) {
            if (!continueOnError) {
                throw e;
            }
            s_logger.warn("Problems with " + element.getName() + " but pushing on", e);
            success = false;
        }

        // For revoked static nat IP, set the vm_id to null, indicate it should be revoked
        for (StaticNat staticNat : staticNats) {
            if (staticNat.isForRevoke()) {
                for (PublicIp publicIp : publicIps) {
                    if (publicIp.getId() == staticNat.getSourceIpAddressId()) {
                        publicIps.remove(publicIp);
                        IPAddressVO ip = _ipAddressDao.findByIdIncludingRemoved(staticNat.getSourceIpAddressId());
                        // ip can't be null, otherwise something wrong happened
                        ip.setAssociatedWithVmId(null);
                        publicIp = PublicIp.createFromAddrAndVlan(ip, _vlanDao.findById(ip.getVlanId()));
                        publicIps.add(publicIp);
                        break;
                    }
                }
            }
        }

        // if all the rules configured on public IP are revoked then, dis-associate IP with network service provider
        applyIpAssociations(network, true, continueOnError, publicIps);

        return success;
    }

    @DB
    @Override
    public boolean reallocate(VirtualMachineProfile<? extends VMInstanceVO> vm, DataCenterDeployment dest) throws InsufficientCapacityException, ConcurrentOperationException {
        VMInstanceVO vmInstance = _vmDao.findById(vm.getId());
        DataCenterVO dc = _dcDao.findById(vmInstance.getDataCenterId());
        if (dc.getNetworkType() == NetworkType.Basic) {
            List<NicVO> nics = _nicDao.listByVmId(vmInstance.getId());
            NetworkVO network = _networksDao.findById(nics.get(0).getNetworkId());
            Pair<NetworkVO, NicProfile> profile = new Pair<NetworkVO, NicProfile>(network, null);
            List<Pair<NetworkVO, NicProfile>> profiles = new ArrayList<Pair<NetworkVO, NicProfile>>();
            profiles.add(profile);

            Transaction txn = Transaction.currentTxn();
            txn.start();

            try {
                this.cleanupNics(vm);
                this.allocate(vm, profiles);
            } finally {
                txn.commit();
            }
        }
        return true;
    }

    private boolean cleanupNetworkResources(long networkId, Account caller, long callerUserId) {
        boolean success = true;
        Network network = _networksDao.findById(networkId);

        //remove all PF/Static Nat rules for the network
        try {
            if (_rulesMgr.revokeAllPFStaticNatRulesForNetwork(networkId, callerUserId, caller)) {
                s_logger.debug("Successfully cleaned up portForwarding/staticNat rules for network id=" + networkId);
            } else {
                success = false;
                s_logger.warn("Failed to release portForwarding/StaticNat rules as a part of network id=" + networkId + " cleanup");
            }
        } catch (ResourceUnavailableException ex) {
            success = false;
            // shouldn't even come here as network is being cleaned up after all network elements are shutdown
            s_logger.warn("Failed to release portForwarding/StaticNat rules as a part of network id=" + networkId + " cleanup due to resourceUnavailable ", ex);
        }

        //remove all LB rules for the network
        if (_lbMgr.removeAllLoadBalanacersForNetwork(networkId, caller, callerUserId)) {
            s_logger.debug("Successfully cleaned up load balancing rules for network id=" + networkId);
        } else {
            // shouldn't even come here as network is being cleaned up after all network elements are shutdown
            success = false;
            s_logger.warn("Failed to cleanup LB rules as a part of network id=" + networkId + " cleanup");
        }

        //revoke all firewall rules for the network
        try {
            if (_firewallMgr.revokeAllFirewallRulesForNetwork(networkId, callerUserId, caller)) {
                s_logger.debug("Successfully cleaned up firewallRules rules for network id=" + networkId);
            } else {
                success = false;
                s_logger.warn("Failed to cleanup Firewall rules as a part of network id=" + networkId + " cleanup");
            }
        } catch (ResourceUnavailableException ex) {
            success = false;
            // shouldn't even come here as network is being cleaned up after all network elements are shutdown
            s_logger.warn("Failed to cleanup Firewall rules as a part of network id=" + networkId + " cleanup due to resourceUnavailable ", ex);
        }

        //revoke all network ACLs for network
        try {
            if (_networkACLMgr.revokeAllNetworkACLsForNetwork(networkId, callerUserId, caller)) {
                s_logger.debug("Successfully cleaned up NetworkACLs for network id=" + networkId);
            } else {
                success = false;
                s_logger.warn("Failed to cleanup NetworkACLs as a part of network id=" + networkId + " cleanup");
            }
        } catch (ResourceUnavailableException ex) {
            success = false;
            s_logger.warn("Failed to cleanup Network ACLs as a part of network id=" + networkId +
                    " cleanup due to resourceUnavailable ", ex);
        }

        //release all ip addresses
        List<IPAddressVO> ipsToRelease = _ipAddressDao.listByAssociatedNetwork(networkId, null);
        for (IPAddressVO ipToRelease : ipsToRelease) {
            if (ipToRelease.getVpcId() == null) {
                IPAddressVO ip = markIpAsUnavailable(ipToRelease.getId());
                assert (ip != null) : "Unable to mark the ip address id=" + ipToRelease.getId() + " as unavailable.";
            } else {
                _vpcMgr.unassignIPFromVpcNetwork(ipToRelease.getId(), network.getId());
            }
        }

        try {
            if (!applyIpAssociations(network, true)) {
                s_logger.warn("Unable to apply ip address associations for " + network);
                success = false;
            }
        } catch (ResourceUnavailableException e) {
            throw new CloudRuntimeException("We should never get to here because we used true when applyIpAssociations", e);
        }

        return success;
    }

    private boolean shutdownNetworkResources(long networkId, Account caller, long callerUserId) {
        // This method cleans up network rules on the backend w/o touching them in the DB
        boolean success = true;
        Network network = _networksDao.findById(networkId);

        // Mark all PF rules as revoked and apply them on the backend (not in the DB)
        List<PortForwardingRuleVO> pfRules = _portForwardingRulesDao.listByNetwork(networkId);
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Releasing " + pfRules.size() + " port forwarding rules for network id=" + networkId + " as a part of shutdownNetworkRules");
        }

        for (PortForwardingRuleVO pfRule : pfRules) {
            s_logger.trace("Marking pf rule " + pfRule + " with Revoke state");
            pfRule.setState(FirewallRule.State.Revoke);
        }

        try {
            if (!_firewallMgr.applyRules(pfRules, true, false)) {
                s_logger.warn("Failed to cleanup pf rules as a part of shutdownNetworkRules");
                success = false;
            }
        } catch (ResourceUnavailableException ex) {
            s_logger.warn("Failed to cleanup pf rules as a part of shutdownNetworkRules due to ", ex);
            success = false;
        }

        // Mark all static rules as revoked and apply them on the backend (not in the DB)
        List<FirewallRuleVO> firewallStaticNatRules = _firewallDao.listByNetworkAndPurpose(networkId, Purpose.StaticNat);
        List<StaticNatRule> staticNatRules = new ArrayList<StaticNatRule>();
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Releasing " + firewallStaticNatRules.size() + " static nat rules for network id=" + networkId + " as a part of shutdownNetworkRules");
        }

        for (FirewallRuleVO firewallStaticNatRule : firewallStaticNatRules) {
            s_logger.trace("Marking static nat rule " + firewallStaticNatRule + " with Revoke state");
            IpAddress ip = _ipAddressDao.findById(firewallStaticNatRule.getSourceIpAddressId());
            FirewallRuleVO ruleVO = _firewallDao.findById(firewallStaticNatRule.getId());

            if (ip == null || !ip.isOneToOneNat() || ip.getAssociatedWithVmId() == null) {
                throw new InvalidParameterValueException("Source ip address of the rule id=" + firewallStaticNatRule.getId() + " is not static nat enabled");
            }

            //String dstIp = _networkModel.getIpInNetwork(ip.getAssociatedWithVmId(), firewallStaticNatRule.getNetworkId());
            ruleVO.setState(FirewallRule.State.Revoke);
            staticNatRules.add(new StaticNatRuleImpl(ruleVO, ip.getVmIp()));
        }

        try {
            if (!_firewallMgr.applyRules(staticNatRules, true, false)) {
                s_logger.warn("Failed to cleanup static nat rules as a part of shutdownNetworkRules");
                success = false;
            }
        } catch (ResourceUnavailableException ex) {
            s_logger.warn("Failed to cleanup static nat rules as a part of shutdownNetworkRules due to ", ex);
            success = false;
        }

        // remove all LB rules for the network
        List<LoadBalancerVO> lbs = _lbDao.listByNetworkId(networkId);
        List<LoadBalancingRule> lbRules = new ArrayList<LoadBalancingRule>();
        for (LoadBalancerVO lb : lbs) {
            s_logger.trace("Marking lb rule " + lb + " with Revoke state");
            lb.setState(FirewallRule.State.Revoke);
            List<LbDestination> dstList = _lbMgr.getExistingDestinations(lb.getId());
            List<LbStickinessPolicy> policyList = _lbMgr.getStickinessPolicies(lb.getId());
            List<LbHealthCheckPolicy> hcPolicyList =  _lbMgr.getHealthCheckPolicies (lb.getId());
            // mark all destination with revoke state
            for (LbDestination dst : dstList) {
                s_logger.trace("Marking lb destination " + dst + " with Revoke state");
                dst.setRevoked(true);
            }

            LoadBalancingRule loadBalancing = new LoadBalancingRule(lb, dstList, policyList, hcPolicyList);
            lbRules.add(loadBalancing);
        }

        try {
            if (!_lbMgr.applyRules(network, Purpose.LoadBalancing, lbRules)) {
                s_logger.warn("Failed to cleanup lb rules as a part of shutdownNetworkRules");
                success = false;
            }
        } catch (ResourceUnavailableException ex) {
            s_logger.warn("Failed to cleanup lb rules as a part of shutdownNetworkRules due to ", ex);
            success = false;
        }

        // revoke all firewall rules for the network w/o applying them on the DB
        List<FirewallRuleVO> firewallRules = _firewallDao.listByNetworkPurposeTrafficType(networkId, Purpose.Firewall, FirewallRule.TrafficType.Ingress);
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Releasing " + firewallRules.size() + " firewall ingress rules for network id=" + networkId + " as a part of shutdownNetworkRules");
        }

        for (FirewallRuleVO firewallRule : firewallRules) {
            s_logger.trace("Marking firewall ingress rule " + firewallRule + " with Revoke state");
            firewallRule.setState(FirewallRule.State.Revoke);
        }

        try {
            if (!_firewallMgr.applyRules(firewallRules, true, false)) {
                s_logger.warn("Failed to cleanup firewall ingress rules as a part of shutdownNetworkRules");
                success = false;
            }
        } catch (ResourceUnavailableException ex) {
            s_logger.warn("Failed to cleanup firewall ingress rules as a part of shutdownNetworkRules due to ", ex);
            success = false;
        }

        List<FirewallRuleVO> firewallEgressRules = _firewallDao.listByNetworkPurposeTrafficType(networkId, Purpose.Firewall, FirewallRule.TrafficType.Egress);
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Releasing " + firewallEgressRules.size() + " firewall egress rules for network id=" + networkId + " as a part of shutdownNetworkRules");
        }

        for (FirewallRuleVO firewallRule : firewallEgressRules) {
            s_logger.trace("Marking firewall egress rule " + firewallRule + " with Revoke state");
            firewallRule.setState(FirewallRule.State.Revoke);
        }

        try {
            if (!_firewallMgr.applyRules(firewallEgressRules, true, false)) {
                s_logger.warn("Failed to cleanup firewall egress rules as a part of shutdownNetworkRules");
                success = false;
            }
        } catch (ResourceUnavailableException ex) {
            s_logger.warn("Failed to cleanup firewall egress rules as a part of shutdownNetworkRules due to ", ex);
            success = false;
        }

        //revoke all Network ACLs for the network w/o applying them in the DB
        List<FirewallRuleVO> networkACLs = _firewallDao.listByNetworkAndPurpose(networkId, Purpose.NetworkACL);
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Releasing " + networkACLs.size() + " Network ACLs for network id=" + networkId +
                    " as a part of shutdownNetworkRules");
        }

        for (FirewallRuleVO networkACL : networkACLs) {
            s_logger.trace("Marking network ACL " + networkACL + " with Revoke state");
            networkACL.setState(FirewallRule.State.Revoke);
        }

        try {
            if (!_firewallMgr.applyRules(networkACLs, true, false)) {
                s_logger.warn("Failed to cleanup network ACLs as a part of shutdownNetworkRules");
                success = false;
            }
        } catch (ResourceUnavailableException ex) {
            s_logger.warn("Failed to cleanup network ACLs as a part of shutdownNetworkRules due to ", ex);
            success = false;
        }

        //release all static nats for the network
        if (!_rulesMgr.applyStaticNatForNetwork(networkId, false, caller, true)) {
            s_logger.warn("Failed to disable static nats as part of shutdownNetworkRules for network id " + networkId);
            success = false;
        }

        // Get all ip addresses, mark as releasing and release them on the backend
        List<IPAddressVO> userIps = _ipAddressDao.listByAssociatedNetwork(networkId, null);
        List<PublicIp> publicIpsToRelease = new ArrayList<PublicIp>();
        if (userIps != null && !userIps.isEmpty()) {
            for (IPAddressVO userIp : userIps) {
                userIp.setState(State.Releasing);
                PublicIp publicIp = PublicIp.createFromAddrAndVlan(userIp, _vlanDao.findById(userIp.getVlanId()));
                publicIpsToRelease.add(publicIp);
            }
        }

        try {
            if (!applyIpAssociations(network, true, true, publicIpsToRelease)) {
                s_logger.warn("Unable to apply ip address associations for " + network + " as a part of shutdownNetworkRules");
                success = false;
            }
        } catch (ResourceUnavailableException e) {
            throw new CloudRuntimeException("We should never get to here because we used true when applyIpAssociations", e);
        }

        return success;
    }

    @Override
    public boolean processAnswers(long agentId, long seq, Answer[] answers) {
        return false;
    }

    @Override
    public boolean processCommands(long agentId, long seq, Command[] commands) {
        return false;
    }

    @Override
    public AgentControlAnswer processControlCommand(long agentId, AgentControlCommand cmd) {
        return null;
    }

    @Override
    public void processConnect(HostVO host, StartupCommand cmd, boolean forRebalance) throws ConnectionException {
        if (!(cmd instanceof StartupRoutingCommand)) {
            return;
        }
        long hostId = host.getId();
        StartupRoutingCommand startup = (StartupRoutingCommand) cmd;

        String dataCenter = startup.getDataCenter();

        long dcId = -1;
        DataCenterVO dc = _dcDao.findByName(dataCenter);
        if (dc == null) {
            try {
                dcId = Long.parseLong(dataCenter);
                dc = _dcDao.findById(dcId);
            } catch (final NumberFormatException e) {
            }
        }
        if (dc == null) {
            throw new IllegalArgumentException("Host " + startup.getPrivateIpAddress() + " sent incorrect data center: " + dataCenter);
        }
        dcId = dc.getId();
        HypervisorType hypervisorType = startup.getHypervisorType();

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Host's hypervisorType is: " + hypervisorType);
        }

        List<PhysicalNetworkSetupInfo> networkInfoList = new ArrayList<PhysicalNetworkSetupInfo>();

        // list all physicalnetworks in the zone & for each get the network names
        List<PhysicalNetworkVO> physicalNtwkList = _physicalNetworkDao.listByZone(dcId);
        for (PhysicalNetworkVO pNtwk : physicalNtwkList) {
            String publicName = _pNTrafficTypeDao.getNetworkTag(pNtwk.getId(), TrafficType.Public, hypervisorType);
            String privateName = _pNTrafficTypeDao.getNetworkTag(pNtwk.getId(), TrafficType.Management, hypervisorType);
            String guestName = _pNTrafficTypeDao.getNetworkTag(pNtwk.getId(), TrafficType.Guest, hypervisorType);
            String storageName = _pNTrafficTypeDao.getNetworkTag(pNtwk.getId(), TrafficType.Storage, hypervisorType);
            // String controlName = _pNTrafficTypeDao._networkModel.getNetworkTag(pNtwk.getId(), TrafficType.Control, hypervisorType);
            PhysicalNetworkSetupInfo info = new PhysicalNetworkSetupInfo();
            info.setPhysicalNetworkId(pNtwk.getId());
            info.setGuestNetworkName(guestName);
            info.setPrivateNetworkName(privateName);
            info.setPublicNetworkName(publicName);
            info.setStorageNetworkName(storageName);
            PhysicalNetworkTrafficTypeVO mgmtTraffic = _pNTrafficTypeDao.findBy(pNtwk.getId(), TrafficType.Management);
            if (mgmtTraffic != null) {
                String vlan = mgmtTraffic.getVlan();
                info.setMgmtVlan(vlan);
            }
            networkInfoList.add(info);
        }

        // send the names to the agent
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Sending CheckNetworkCommand to check the Network is setup correctly on Agent");
        }
        CheckNetworkCommand nwCmd = new CheckNetworkCommand(networkInfoList);

        CheckNetworkAnswer answer = (CheckNetworkAnswer) _agentMgr.easySend(hostId, nwCmd);

        if (answer == null) {
            s_logger.warn("Unable to get an answer to the CheckNetworkCommand from agent:" + host.getId());
            throw new ConnectionException(true, "Unable to get an answer to the CheckNetworkCommand from agent: " + host.getId());
        }

        if (!answer.getResult()) {
            s_logger.warn("Unable to setup agent " + hostId + " due to " + ((answer != null) ? answer.getDetails() : "return null"));
            String msg = "Incorrect Network setup on agent, Reinitialize agent after network names are setup, details : " + answer.getDetails();
            _alertMgr.sendAlert(AlertManager.ALERT_TYPE_HOST, dcId, host.getPodId(), msg, msg);
            throw new ConnectionException(true, msg);
        } else {
            if (answer.needReconnect()) {
                throw new ConnectionException(false, "Reinitialize agent after network setup.");
            }
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Network setup is correct on Agent");
            }
            return;
        }
    }

    @Override
    public boolean processDisconnect(long agentId, Status state) {
        return false;
    }

    @Override
    public boolean isRecurring() {
        return false;
    }

    @Override
    public int getTimeout() {
        return 0;
    }

    @Override
    public boolean processTimeout(long agentId, long seq) {
        return false;
    }

    @Override
	public Map<String, String> finalizeServicesAndProvidersForNetwork(NetworkOffering offering, Long physicalNetworkId) {
        Map<String, String> svcProviders = new HashMap<String, String>();
        Map<String, List<String>> providerSvcs = new HashMap<String, List<String>>();
        List<NetworkOfferingServiceMapVO> servicesMap = _ntwkOfferingSrvcDao.listByNetworkOfferingId(offering.getId());

        boolean checkPhysicalNetwork = (physicalNetworkId != null) ? true : false;

        for (NetworkOfferingServiceMapVO serviceMap : servicesMap) {
            if (svcProviders.containsKey(serviceMap.getService())) {
                // FIXME - right now we pick up the first provider from the list, need to add more logic based on
                // provider load, etc
                continue;
            }

            String service = serviceMap.getService();
            String provider = serviceMap.getProvider();

            if (provider == null) {
                provider = _networkModel.getDefaultUniqueProviderForService(service).getName();
            }

            // check that provider is supported
            if (checkPhysicalNetwork) {
                if (!_pNSPDao.isServiceProviderEnabled(physicalNetworkId, provider, service)) {
                    throw new UnsupportedServiceException("Provider " + provider + " is either not enabled or doesn't " +
                            "support service " + service + " in physical network id=" + physicalNetworkId);
                }
            }

            svcProviders.put(service, provider);
            List<String> l = providerSvcs.get(provider);
            if (l == null) {
                providerSvcs.put(provider, l = new ArrayList<String>());
            }
            l.add(service);
        }

        return svcProviders;
    }

    private List<Provider> getNetworkProviders(long networkId) {
        List<String> providerNames = _ntwkSrvcDao.getDistinctProviders(networkId);
        List<Provider> providers = new ArrayList<Provider>();
        for (String providerName : providerNames) {
            providers.add(Network.Provider.getProvider(providerName));
        }

        return providers;
    }

    @Override
    public IpAddress assignSystemIp(long networkId, Account owner, boolean forElasticLb, boolean forElasticIp) 
            throws InsufficientAddressCapacityException {
        Network guestNetwork = _networksDao.findById(networkId);
        NetworkOffering off = _configMgr.getNetworkOffering(guestNetwork.getNetworkOfferingId());
        IpAddress ip = null;
        if ((off.getElasticLb() && forElasticLb) || (off.getElasticIp() && forElasticIp)) {

            try {
                s_logger.debug("Allocating system IP address for load balancer rule...");
                // allocate ip
                ip = allocateIP(owner, true, guestNetwork.getDataCenterId());
                // apply ip associations
                ip = associateIPToGuestNetwork(ip.getId(), networkId, true);;
            } catch (ResourceAllocationException ex) {
                throw new CloudRuntimeException("Failed to allocate system ip due to ", ex);
            } catch (ConcurrentOperationException ex) {
                throw new CloudRuntimeException("Failed to allocate system lb ip due to ", ex);
            } catch (ResourceUnavailableException ex) {
                throw new CloudRuntimeException("Failed to allocate system lb ip due to ", ex);
            }

            if (ip == null) {
                throw new CloudRuntimeException("Failed to allocate system ip");
            }
        }

        return ip;
    }

    @Override
    public boolean handleSystemIpRelease(IpAddress ip) {
        boolean success = true;
        Long networkId = ip.getAssociatedWithNetworkId();
        if (networkId != null) {
            if (ip.getSystem()) {
                UserContext ctx = UserContext.current();
                if (!disassociatePublicIpAddress(ip.getId(), ctx.getCallerUserId(), ctx.getCaller())) {
                    s_logger.warn("Unable to release system ip address id=" + ip.getId());
                    success = false;
                } else {
                    s_logger.warn("Successfully released system ip address id=" + ip.getId());
                }
            }
        }
        return success;
    }

    public void allocateDirectIp(NicProfile nic, DataCenter dc, VirtualMachineProfile<? extends VirtualMachine> vm, Network network,
    							 String requestedIpv4, String requestedIpv6) throws InsufficientVirtualNetworkCapcityException,
            InsufficientAddressCapacityException {
    	boolean ipv4 = false, ipv6 = false;
    	if (network.getGateway() != null) {
    		if (nic.getIp4Address() == null) {
    			ipv4 = true;
    			PublicIp ip = assignPublicIpAddress(dc.getId(), null, vm.getOwner(), VlanType.DirectAttached, network.getId(), requestedIpv4, false);
    			nic.setIp4Address(ip.getAddress().toString());
    			nic.setGateway(ip.getGateway());
    			nic.setNetmask(ip.getNetmask());
    			nic.setIsolationUri(IsolationType.Vlan.toUri(ip.getVlanTag()));
    			nic.setBroadcastType(BroadcastDomainType.Vlan);
    			nic.setBroadcastUri(BroadcastDomainType.Vlan.toUri(ip.getVlanTag()));
    			nic.setFormat(AddressFormat.Ip4);
    			nic.setReservationId(String.valueOf(ip.getVlanTag()));
    			nic.setMacAddress(ip.getMacAddress());
    		}
    		nic.setDns1(dc.getDns1());
    		nic.setDns2(dc.getDns2());
    	}
    	
    	if (network.getIp6Gateway() != null) {
    		if (nic.getIp6Address() == null) {
    			ipv6 = true;
    			UserIpv6Address ip = _ipv6Mgr.assignDirectIp6Address(dc.getId(), vm.getOwner(), network.getId(), requestedIpv6);
    			Vlan vlan = _vlanDao.findById(ip.getVlanId());
    			nic.setIp6Address(ip.getAddress().toString());
    			nic.setIp6Gateway(vlan.getIp6Gateway());
    			nic.setIp6Cidr(vlan.getIp6Cidr());
    			if (ipv4) {
    				nic.setFormat(AddressFormat.DualStack);
    			} else {
    				nic.setIsolationUri(IsolationType.Vlan.toUri(vlan.getVlanTag()));
    				nic.setBroadcastType(BroadcastDomainType.Vlan);
    				nic.setBroadcastUri(BroadcastDomainType.Vlan.toUri(vlan.getVlanTag()));
    				nic.setFormat(AddressFormat.Ip6);
    				nic.setReservationId(String.valueOf(vlan.getVlanTag()));
    				nic.setMacAddress(ip.getMacAddress());
    			}
    		}
    		nic.setIp6Dns1(dc.getIp6Dns1());
    		nic.setIp6Dns2(dc.getIp6Dns2());
    	}

    }

	@Override
    public boolean setupDns(Network network, Provider provider) {
        boolean dnsProvided = _networkModel.isProviderSupportServiceInNetwork(network.getId(), Service.Dns, provider );
        boolean dhcpProvided =_networkModel.isProviderSupportServiceInNetwork(network.getId(), Service.Dhcp, 
                provider);

        boolean setupDns = dnsProvided || dhcpProvided;
        return setupDns;
    }
    
    
    protected NicProfile getNicProfileForVm(Network network, NicProfile requested, VirtualMachine vm) {
        NicProfile nic = null;
        if (requested != null && requested.getBroadCastUri() != null) {
            String broadcastUri = requested.getBroadCastUri().toString();
            String ipAddress = requested.getIp4Address();
            NicVO nicVO = _nicDao.findByNetworkIdInstanceIdAndBroadcastUri(network.getId(), vm.getId(), broadcastUri);
            if (nicVO != null) {
                if (ipAddress == null || nicVO.getIp4Address().equals(ipAddress)) {
                    nic = _networkModel.getNicProfile(vm, network.getId(), broadcastUri);
                }
            }
        } else {
            NicVO nicVO = _nicDao.findByInstanceIdAndNetworkId(network.getId(), vm.getId());
            if (nicVO != null) {
                nic = _networkModel.getNicProfile(vm, network.getId(), null);
            }
        }
        return nic;
    }
    
    @Override
    public NicProfile createNicForVm(Network network, NicProfile requested, ReservationContext context, VirtualMachineProfile<? extends VMInstanceVO> vmProfile, boolean prepare)
            throws InsufficientVirtualNetworkCapcityException, InsufficientAddressCapacityException,
            ConcurrentOperationException, InsufficientCapacityException, ResourceUnavailableException {
                
                VirtualMachine vm = vmProfile.getVirtualMachine();
                DataCenter dc = _configMgr.getZone(network.getDataCenterId());
                Host host = _hostDao.findById(vm.getHostId()); 
                DeployDestination dest = new DeployDestination(dc, null, null, host);
                
                NicProfile nic = getNicProfileForVm(network, requested, vm);
                
                //1) allocate nic (if needed) Always allocate if it is a user vm
                if (nic == null || (vmProfile.getType() == VirtualMachine.Type.User)) {
                    int deviceId = _nicDao.countNics(vm.getId());
                    
                    nic = allocateNic(requested, network, false, 
                            deviceId, vmProfile).first();
                    
                    if (nic == null) {
                        throw new CloudRuntimeException("Failed to allocate nic for vm " + vm + " in network " + network);
                    }
                    
                    s_logger.debug("Nic is allocated successfully for vm " + vm + " in network " + network); 
                }
                
                //2) prepare nic
                if (prepare) {
                    NetworkVO networkVO = _networksDao.findById(network.getId());
                    nic = prepareNic(vmProfile, dest, context, nic.getId(), networkVO);
                    s_logger.debug("Nic is prepared successfully for vm " + vm + " in network " + network);
                }
                
                return nic;
            }
    @Override
    public List<NicProfile> getNicProfiles(VirtualMachine vm) {
        List<NicVO> nics = _nicDao.listByVmId(vm.getId());
        List<NicProfile> profiles = new ArrayList<NicProfile>();
    
        if (nics != null) {
            for (Nic nic : nics) {
                NetworkVO network = _networksDao.findById(nic.getNetworkId());
                Integer networkRate = _networkModel.getNetworkRate(network.getId(), vm.getId());
    
                NetworkGuru guru = AdapterBase.getAdapterByName(_networkGurus, network.getGuruName());
                NicProfile profile = new NicProfile(nic, network, nic.getBroadcastUri(), nic.getIsolationUri(), 
                        networkRate, _networkModel.isSecurityGroupSupportedInNetwork(network), _networkModel.getNetworkTag(vm.getHypervisorType(), network));
                guru.updateNicProfile(profile, network);
                profiles.add(profile);
            }
        }
        return profiles;
    }


    @Override
    public int getNetworkLockTimeout() {
        return _networkLockTimeout;
    }


    protected boolean stateTransitTo(NetworkVO network, Network.Event e) throws NoTransitionException {
        return _stateMachine.transitTo(network, e, null, _networksDao);
    }

    private void setStateMachine() {
        _stateMachine = Network.State.getStateMachine();
    }

    private Map<Service, Set<Provider>> getServiceProvidersMap(long networkId) {
        Map<Service, Set<Provider>> map = new HashMap<Service, Set<Provider>>();
        List<NetworkServiceMapVO> nsms = _ntwkSrvcDao.getServicesInNetwork(networkId);
        for (NetworkServiceMapVO nsm : nsms) {
            Set<Provider> providers = map.get(Service.getService(nsm.getService()));
            if (providers == null) {
                providers = new HashSet<Provider>();
            }
            providers.add(Provider.getProvider(nsm.getProvider()));
            map.put(Service.getService(nsm.getService()), providers);
        }
        return map;
    }

    @Override
    public List<Provider> getProvidersForServiceInNetwork(Network network, Service service) {
        Map<Service, Set<Provider>> service2ProviderMap = getServiceProvidersMap(network.getId());
        if (service2ProviderMap.get(service) != null) {
            List<Provider> providers = new ArrayList<Provider>(service2ProviderMap.get(service));
            return providers;
        }
        return null;
    }

    protected NetworkElement getElementForServiceInNetwork(Network network, Service service) {
        List<Provider> providers = getProvidersForServiceInNetwork(network, service);
        //Only support one provider now
        if (providers == null)  {
            s_logger.error("Cannot find " + service.getName() + " provider for network " + network.getId());
            return null;
        }
        if (providers.size() != 1) {
            s_logger.error("Found " + providers.size() + " " + service.getName() + " providers for network!" + network.getId());
            return null;
        }
        NetworkElement element = _networkModel.getElementImplementingProvider(providers.get(0).getName());
        s_logger.info("Let " + element.getName() + " handle " + service.getName() + " in network " + network.getId());
        return element;
    }
    
    @Override
    public StaticNatServiceProvider getStaticNatProviderForNetwork(Network network) {
        NetworkElement element = getElementForServiceInNetwork(network, Service.StaticNat);
        assert ComponentContext.getTargetObject(element) instanceof StaticNatServiceProvider;
        return (StaticNatServiceProvider)ComponentContext.getTargetObject(element);
    }

    @Override
    public LoadBalancingServiceProvider getLoadBalancingProviderForNetwork(Network network) {
        NetworkElement element = getElementForServiceInNetwork(network, Service.Lb);
        assert ComponentContext.getTargetObject(element) instanceof LoadBalancingServiceProvider; 
        return ( LoadBalancingServiceProvider)ComponentContext.getTargetObject(element);
    }
    @Override
    public boolean isNetworkInlineMode(Network network) {
        NetworkOfferingVO offering = _networkOfferingDao.findById(network.getNetworkOfferingId());
        return offering.isInline();
    }

    @Override
    public int getRuleCountForIp(Long addressId, FirewallRule.Purpose purpose, FirewallRule.State state) {
        List<FirewallRuleVO> rules = _firewallDao.listByIpAndPurposeWithState(addressId, purpose, state);
        if (rules == null) {
            return 0;
        }
        return rules.size();
    }

         @Override
    public boolean isSecondaryIpSetForNic(long nicId) {
        NicVO nic = _nicDao.findById(nicId);
        return nic.getSecondaryIp();
    }

         @Override
        public boolean removeVmSecondaryIpsOfNic(long nicId) {
           Transaction txn = Transaction.currentTxn();
           txn.start();
           List <NicSecondaryIpVO> ipList = _nicSecondaryIpDao.listByNicId(nicId);
           if (ipList != null) {
               for (NicSecondaryIpVO ip: ipList) {
                   _nicSecondaryIpDao.remove(ip.getId());
               }
               s_logger.debug("Revoving nic secondary ip entry ...");
           }
           txn.commit();
           return true;
        }

        @Override
        public String allocatePublicIpForGuestNic(Long networkId, DataCenter dc, Pod pod,Account owner,
                String requestedIp) throws InsufficientAddressCapacityException {
            PublicIp ip = assignPublicIpAddress(dc.getId(), null, owner, VlanType.DirectAttached, networkId, requestedIp, false);
            if (ip == null) {
                s_logger.debug("There is no free public ip address");
                return null;
            }
            Ip ipAddr = ip.getAddress();
            return ipAddr.addr();
        }

 }
