/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
 * 
 * This software is licensed under the GNU General Public License v3 or later.
 * 
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */
package com.cloud.network;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.ejb.Local;
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;

import com.cloud.agent.api.to.NicTO;
import com.cloud.alert.AlertManager;
import com.cloud.api.commands.AssociateIPAddrCmd;
import com.cloud.api.commands.CreateNetworkCmd;
import com.cloud.api.commands.DisassociateIPAddrCmd;
import com.cloud.api.commands.ListNetworksCmd;
import com.cloud.api.commands.RestartNetworkCmd;
import com.cloud.capacity.dao.CapacityDao;
import com.cloud.configuration.Config;
import com.cloud.configuration.ConfigurationManager;
import com.cloud.configuration.ResourceCount.ResourceType;
import com.cloud.configuration.dao.ConfigurationDao;
import com.cloud.configuration.dao.ResourceLimitDao;
import com.cloud.dc.AccountVlanMapVO;
import com.cloud.dc.DataCenter;
import com.cloud.dc.DataCenter.NetworkType;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.PodVlanMapVO;
import com.cloud.dc.Vlan;
import com.cloud.dc.Vlan.VlanType;
import com.cloud.dc.VlanVO;
import com.cloud.dc.dao.AccountVlanMapDao;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.PodVlanMapDao;
import com.cloud.dc.dao.VlanDao;
import com.cloud.deploy.DataCenterDeployment;
import com.cloud.deploy.DeployDestination;
import com.cloud.deploy.DeploymentPlan;
import com.cloud.domain.Domain;
import com.cloud.domain.DomainVO;
import com.cloud.domain.dao.DomainDao;
import com.cloud.event.ActionEvent;
import com.cloud.event.EventTypes;
import com.cloud.event.UsageEventVO;
import com.cloud.event.dao.EventDao;
import com.cloud.event.dao.UsageEventDao;
import com.cloud.exception.AccountLimitException;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.IpAddress.State;
import com.cloud.network.Network.Capability;
import com.cloud.network.Network.GuestIpType;
import com.cloud.network.Network.Service;
import com.cloud.network.Networks.AddressFormat;
import com.cloud.network.Networks.BroadcastDomainType;
import com.cloud.network.Networks.TrafficType;
import com.cloud.network.addr.PublicIp;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkDomainDao;
import com.cloud.network.element.NetworkElement;
import com.cloud.network.guru.NetworkGuru;
import com.cloud.network.lb.LoadBalancingRulesManager;
import com.cloud.network.rules.FirewallRule;
import com.cloud.network.rules.RulesManager;
import com.cloud.network.vpn.PasswordResetElement;
import com.cloud.network.vpn.RemoteAccessVpnElement;
import com.cloud.network.vpn.RemoteAccessVpnService;
import com.cloud.offering.NetworkOffering;
import com.cloud.offering.NetworkOffering.Availability;
import com.cloud.offerings.NetworkOfferingVO;
import com.cloud.offerings.dao.NetworkOfferingDao;
import com.cloud.org.Grouping;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.AccountVO;
import com.cloud.user.User;
import com.cloud.user.UserContext;
import com.cloud.user.dao.AccountDao;
import com.cloud.user.dao.UserStatisticsDao;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.Pair;
import com.cloud.utils.component.Adapters;
import com.cloud.utils.component.Inject;
import com.cloud.utils.component.Manager;
import com.cloud.utils.concurrency.NamedThreadFactory;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.JoinBuilder;
import com.cloud.utils.db.JoinBuilder.JoinType;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Op;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.net.Ip;
import com.cloud.utils.net.NetUtils;
import com.cloud.vm.Nic;
import com.cloud.vm.NicProfile;
import com.cloud.vm.NicVO;
import com.cloud.vm.ReservationContext;
import com.cloud.vm.ReservationContextImpl;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachine.Type;
import com.cloud.vm.VirtualMachineProfile;
import com.cloud.vm.dao.NicDao;
import com.cloud.vm.dao.UserVmDao;

import edu.emory.mathcs.backport.java.util.Collections;

/**
 * NetworkManagerImpl implements NetworkManager.
 */
@Local(value = { NetworkManager.class, NetworkService.class })
public class NetworkManagerImpl implements NetworkManager, NetworkService, Manager {
    private static final Logger s_logger = Logger.getLogger(NetworkManagerImpl.class);

    String _name;
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
    UserStatisticsDao _userStatsDao = null;
    @Inject
    EventDao _eventDao = null;
    @Inject
    ConfigurationDao _configDao;
    @Inject
    UserVmDao _vmDao = null;
    @Inject
    ResourceLimitDao _limitDao = null;
    @Inject
    CapacityDao _capacityDao = null;
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
    UsageEventDao _usageEventDao;
    @Inject
    RemoteAccessVpnService _vpnMgr;
    @Inject
    PodVlanMapDao _podVlanMapDao;
    @Inject(adapter = NetworkGuru.class)
    Adapters<NetworkGuru> _networkGurus;
    @Inject(adapter = NetworkElement.class)
    Adapters<NetworkElement> _networkElements;
    @Inject
    NetworkDomainDao _networkDomainDao;

    private HashMap<String, NetworkOfferingVO> _systemNetworks = new HashMap<String, NetworkOfferingVO>(5);

    ScheduledExecutorService _executor;

    SearchBuilder<AccountVO> AccountsUsingNetworkSearch;
    SearchBuilder<IPAddressVO> AssignIpAddressSearch;
    SearchBuilder<IPAddressVO> AssignIpAddressFromPodVlanSearch;
    SearchBuilder<IPAddressVO> IpAddressSearch;
    SearchBuilder<NicVO> NicForTrafficTypeSearch;

    int _networkGcWait;
    int _networkGcInterval;
    String _networkDomain;
    int _cidrLimit;

    private Map<String, String> _configs;

    HashMap<Long, Long> _lastNetworkIdsToFree = new HashMap<Long, Long>();

    @Override
    public PublicIp assignPublicIpAddress(long dcId, Long podId, Account owner, VlanType type, Long networkId) throws InsufficientAddressCapacityException {
        return fetchNewPublicIp(dcId, podId, null, owner, type, networkId, false, true);
    }

    @DB
    public PublicIp fetchNewPublicIp(long dcId, Long podId, Long vlanDbId, Account owner, VlanType vlanUse, Long networkId, boolean sourceNat, boolean assign) throws InsufficientAddressCapacityException {
        Transaction txn = Transaction.currentTxn();
        txn.start();
        SearchCriteria<IPAddressVO> sc = null;
        if (podId != null) {
            sc = AssignIpAddressFromPodVlanSearch.create();
            sc.setJoinParameters("podVlanMapSB", "podId", podId);
        } else {
            sc = AssignIpAddressSearch.create();
        }

        if (vlanDbId != null) {
            sc.addAnd("vlanId", SearchCriteria.Op.EQ, vlanDbId);
        }

        sc.setParameters("dc", dcId);

        // for direct network take ip addresses only from the vlans belonging to the network
        if (vlanUse == VlanType.DirectAttached) {
            sc.setJoinParameters("vlan", "networkId", networkId);
        }
        sc.setJoinParameters("vlan", "type", vlanUse);

        Filter filter = new Filter(IPAddressVO.class, "vlanId", true, 0l, 1l);

        List<IPAddressVO> addrs = _ipAddressDao.lockRows(sc, filter, true);

        if (addrs.size() == 0) {
            throw new InsufficientAddressCapacityException("Insufficient address capacity", DataCenter.class, dcId);
        }

        assert (addrs.size() == 1) : "Return size is incorrect: " + addrs.size();

        IPAddressVO addr = addrs.get(0);
        addr.setSourceNat(sourceNat);
        addr.setAllocatedTime(new Date());
        addr.setAllocatedInDomainId(owner.getDomainId());
        addr.setAllocatedToAccountId(owner.getId());

        if (assign) {
            markPublicIpAsAllocated(addr);
        } else {
            addr.setState(IpAddress.State.Allocating);
        }
        addr.setState(assign ? IpAddress.State.Allocated : IpAddress.State.Allocating);

        if (vlanUse != VlanType.DirectAttached) {
            addr.setAssociatedWithNetworkId(networkId);
        }
        
        _ipAddressDao.update(addr.getId(), addr);
        
        txn.commit();
        long macAddress = NetUtils.createSequenceBasedMacAddress(addr.getMacAddress());

        return new PublicIp(addr, _vlanDao.findById(addr.getVlanId()), macAddress);
    }
    
    @DB
    protected void markPublicIpAsAllocated(IPAddressVO addr) {
        
        assert (addr.getState() == IpAddress.State.Allocating || addr.getState() == IpAddress.State.Free) : "Unable to transition from state " + addr.getState() + " to " + IpAddress.State.Allocated;
        
        Transaction txn = Transaction.currentTxn();
        
        Account owner = _accountMgr.getAccount(addr.getAccountId());
        long isSourceNat = (addr.isSourceNat()) ? 1 : 0;
        
        txn.start();
        addr.setState(IpAddress.State.Allocated);
        _ipAddressDao.update(addr.getId(), addr);
        
        //Save usage event
        if (owner.getAccountId() != Account.ACCOUNT_ID_SYSTEM) {
            UsageEventVO usageEvent = new UsageEventVO(EventTypes.EVENT_NET_IP_ASSIGN, owner.getId(), addr.getDataCenterId(), addr.getId(), addr.getAddress().toString(), isSourceNat);
            _usageEventDao.persist(usageEvent);
            _accountMgr.incrementResourceCount(owner.getId(), ResourceType.public_ip);
        }   
       
        txn.commit();
    }

    @Override
    @DB
    public PublicIp assignSourceNatIpAddress(Account owner, Network network, long callerId) throws ConcurrentOperationException, InsufficientAddressCapacityException {
        assert (network.getTrafficType() != null) : "You're asking for a source nat but your network can't participate in source nat.  What do you have to say for yourself?";

        long dcId = network.getDataCenterId();
        long ownerId = owner.getId();

        PublicIp ip = null;

        Transaction txn = Transaction.currentTxn();
        try {
            txn.start();

            owner = _accountDao.acquireInLockTable(ownerId);
            if (owner == null) {
                throw new ConcurrentOperationException("Unable to lock account " + ownerId);
            }
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("lock account " + ownerId + " is acquired");
            }

            IPAddressVO sourceNat = null;
            List<IPAddressVO> addrs = listPublicIpAddressesInVirtualNetwork(ownerId, dcId, null, network.getId());
            if (addrs.size() == 0) {
                // Check that the maximum number of public IPs for the given accountId will not be exceeded
                if (_accountMgr.resourceLimitExceeded(owner, ResourceType.public_ip)) {
                    throw new AccountLimitException("Maximum number of public IP addresses for account: " + owner.getAccountName() + " has been exceeded.");
                }

                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("assigning a new ip address in " + dcId + " to " + owner);
                }

                // If account has Account specific ip ranges, try to allocate ip from there
                Long vlanId = null;
                List<AccountVlanMapVO> maps = _accountVlanMapDao.listAccountVlanMapsByAccount(ownerId);
                if (maps != null && !maps.isEmpty()) {
                    vlanId = maps.get(0).getVlanDbId();
                }

                ip = fetchNewPublicIp(dcId, null, vlanId, owner, VlanType.VirtualNetwork, network.getId(), true, false);
                sourceNat = ip.ip();
                
                markPublicIpAsAllocated(sourceNat);
                _ipAddressDao.update(sourceNat.getId(), sourceNat);       
            } else {
                // Account already has ip addresses
                for (IPAddressVO addr : addrs) {
                    if (addr.isSourceNat()) {
                        sourceNat = addr;
                        break;
                    }
                }

                assert (sourceNat != null) : "How do we get a bunch of ip addresses but none of them are source nat? account=" + ownerId + "; dc=" + dcId;
                ip = new PublicIp(sourceNat, _vlanDao.findById(sourceNat.getVlanId()), NetUtils.createSequenceBasedMacAddress(sourceNat.getMacAddress()));
            }

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

    /**
     * Returns the target account for an api command
     * 
     * @param accountName
     *            - non-null if the account name was passed in in the command
     * @param domainId
     *            - non-null if the domainId was passed in in the command.
     * @return
     */
    protected Account getAccountForApiCommand(String accountName, Long domainId){
        Account account = UserContext.current().getCaller();

        if (_accountMgr.isAdmin(account.getType())) {
            // The admin is making the call, determine if it is for someone else or for himself
            if (domainId != null) {
                if ((account != null) && !_domainDao.isChildDomain(account.getDomainId(), domainId)) {
                    throw new PermissionDeniedException("Invalid domain id (" + domainId + ") given, , permission denied");
                }
                if (accountName != null) {
                    Account userAccount = _accountMgr.getActiveAccount(accountName, domainId);
                    if (userAccount != null) {
                        account = userAccount;
                    } else {
                        throw new PermissionDeniedException("Unable to find account " + accountName + " in domain " + domainId + ", permission denied");
                    }
                }
            } else {
                // the admin is calling the api on his own behalf
                return account;
            }
        }
        return account;
    }

    @Override
    public boolean applyIpAssociations(Network network, boolean continueOnError) throws ResourceUnavailableException {
        List<IPAddressVO> userIps = _ipAddressDao.listByAssociatedNetwork(network.getId(), null);
        List<PublicIp> publicIps = new ArrayList<PublicIp>();
        if (userIps != null && !userIps.isEmpty()) {
            for (IPAddressVO userIp : userIps) {
                    PublicIp publicIp = new PublicIp(userIp, _vlanDao.findById(userIp.getVlanId()), NetUtils.createSequenceBasedMacAddress(userIp.getMacAddress()));
                    publicIps.add(publicIp);
            }
        }

        boolean success = true;
        for (NetworkElement element : _networkElements) {
            try {
                element.applyIps(network, publicIps);
            } catch (ResourceUnavailableException e) {
                success = false;
                if (!continueOnError) {
                    throw e;
                } else {
                    s_logger.debug("Resource is not available: " + element.getName(), e);
                }
            }
        }

        if (success) {
            for (IPAddressVO addr : userIps) {
                
                if (addr.getState() == IpAddress.State.Allocating) {
                   
                    addr.setAssociatedWithNetworkId(network.getId());
                    markPublicIpAsAllocated(addr);
                    
                } else if (addr.getState() == IpAddress.State.Releasing) {
                    //Cleanup all the resources for ip address if there are any, and only then unassign ip in the system
                    if (cleanupIpResources(addr.getId(), Account.ACCOUNT_ID_SYSTEM, _accountMgr.getSystemAccount())) {
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
    public List<? extends Network> getVirtualNetworksOwnedByAccountInZone(String accountName, long domainId, long zoneId) {
        Account owner = _accountMgr.getActiveAccount(accountName, domainId);
        if (owner == null) {
            throw new InvalidParameterValueException("Unable to find account " + accountName + " in domain " + domainId + ", permission denied");
        }

        return _networksDao.listBy(owner.getId(), zoneId, GuestIpType.Virtual);
    }

    @Override @DB @ActionEvent (eventType=EventTypes.EVENT_NET_IP_ASSIGN, eventDescription="allocating Ip", create=true)
    public IpAddress allocateIP(AssociateIPAddrCmd cmd) throws ResourceAllocationException, InsufficientAddressCapacityException, ConcurrentOperationException {
        String accountName = cmd.getAccountName();
        long domainId = cmd.getDomainId();
        Long zoneId = cmd.getZoneId();
        Account caller = UserContext.current().getCaller();
        long userId = UserContext.current().getCallerUserId();

        Account ipOwner = _accountMgr.getActiveAccount(accountName, domainId);
        if (ipOwner == null) {
            throw new InvalidParameterValueException("Unable to find account " + accountName + " in domain " + domainId + ", permission denied");
        }

        _accountMgr.checkAccess(caller, ipOwner);
        
        if(zoneId != null){
	        DataCenterVO zone = _dcDao.findById(zoneId);
			if (zone == null) {
				throw new InvalidParameterValueException("Can't find zone by id "
						+ zoneId);
			}
			
			if(Grouping.AllocationState.Disabled == zone.getAllocationState() && !_accountMgr.isRootAdmin(caller.getType())){
				throw new PermissionDeniedException("Cannot perform this operation, Zone is currently disabled: "+ zoneId );
			}
        }
        long ownerId = ipOwner.getId();
        Long networkId = cmd.getNetworkId();
        Network network = null;
        if (networkId != null) {
            network = _networksDao.findById(networkId);
            if (network == null) {
                throw new InvalidParameterValueException("Network id is invalid: " + networkId);
            }
        }
        
        //Check that network belongs to IP owner
        if (network.getAccountId() != ipOwner.getId()) {
            throw new InvalidParameterValueException("The owner of the network is not the same as owner of the IP");
        }

        PublicIp ip = null;

        Transaction txn = Transaction.currentTxn();
        Account accountToLock = null;
        try {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Associate IP address called for user " + userId + " account " + ownerId);
            }
            accountToLock = _accountDao.acquireInLockTable(ownerId);
            if (accountToLock == null) {
                s_logger.warn("Unable to lock account: " + ownerId);
                throw new ConcurrentOperationException("Unable to acquire account lock");
            }

            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Associate IP address lock acquired");
            }

            // Check that the maximum number of public IPs for the given
            // accountId will not be exceeded
            if (_accountMgr.resourceLimitExceeded(accountToLock, ResourceType.public_ip)) {
                ResourceAllocationException rae = new ResourceAllocationException("Maximum number of public IP addresses for account: " + accountToLock.getAccountName() + " has been exceeded.");
                rae.setResourceType("ip");
                throw rae;
            }
            
            boolean isSourceNat = false;

            txn.start();
            //First IP address should be source nat
            List<IPAddressVO> addrs = listPublicIpAddressesInVirtualNetwork(ownerId, zoneId, true, networkId);
            
            if (addrs.isEmpty()) {
                isSourceNat = true;
            }
            
            ip = fetchNewPublicIp(zoneId, null, null, ipOwner, VlanType.VirtualNetwork, network.getId(), isSourceNat, false);

            if (ip == null) {
                throw new InsufficientAddressCapacityException("Unable to find available public IP addresses", DataCenter.class, zoneId);
            }
            UserContext.current().setEventDetails("Ip Id: "+ip.getId());
            Ip ipAddress = ip.getAddress();

            s_logger.debug("Got " + ipAddress + " to assign for account " + ipOwner.getId() + " in zone " + network.getDataCenterId());

            txn.commit();
        } finally {
            if (accountToLock != null) {
                _accountDao.releaseFromLockTable(ownerId);
                s_logger.debug("Associate IP address lock released");
            }
        }

        return ip;
    }

    @Override @DB @ActionEvent (eventType=EventTypes.EVENT_NET_IP_ASSIGN, eventDescription="associating Ip", async=true)
    public IpAddress associateIP(AssociateIPAddrCmd cmd) throws ResourceAllocationException, ResourceUnavailableException, InsufficientAddressCapacityException, ConcurrentOperationException {
        Account caller = UserContext.current().getCaller();
        Account owner = null;

        IpAddress ipToAssoc = getIp(cmd.getEntityId());
        if (ipToAssoc != null) {
            _accountMgr.checkAccess(caller, ipToAssoc);
            owner = _accountMgr.getAccount(ipToAssoc.getAccountId());
        } else {
            s_logger.debug("Unable to find ip address by id: " + cmd.getEntityId());
            return null;
        }

        Network network = _networksDao.findById(ipToAssoc.getAssociatedWithNetworkId());

        IPAddressVO ip = _ipAddressDao.findById(cmd.getEntityId());
        boolean success = false;
        try {
            success = applyIpAssociations(network, false);
            if (success) {
                s_logger.debug("Successfully associated ip address " + ip.getAddress().addr() + " for account " + owner.getId() + " in zone " + network.getDataCenterId());
            } else {
                s_logger.warn("Failed to associate ip address " + ip.getAddress().addr() + " for account " + owner.getId() + " in zone " + network.getDataCenterId());
            }
            return ip;
        } catch (ResourceUnavailableException e) {
            s_logger.error("Unable to associate ip address due to resource unavailable exception", e);
            return null;
        } finally {
            if (!success) {
                if (ip != null) {
                    try {
                        s_logger.warn("Failed to associate ip address " + ip);
                        _ipAddressDao.markAsUnavailable(ip.getId());
                        if (!applyIpAssociations(network, true)) {
                            //if fail to apply ip assciations again, unassign ip address without updating resource count and generating usage event as there is no need to keep it in the db
                            _ipAddressDao.unassignIpAddress(ip.getId());
                        }
                    } catch (Exception e) {
                        s_logger.warn("Unable to disassociate ip address for recovery", e);
                    }
                }
            }
        }
    }

    @Override @DB
    public boolean releasePublicIpAddress(long addrId, long userId, Account caller) {
        
        IPAddressVO ip = markIpAsUnavailable(addrId);
        
        assert (ip != null) : "Unable to mark the ip address id=" + addrId + " as unavailable.";
        if (ip == null) {
            return true;
        }

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Releasing ip id=" + addrId + "; sourceNat = " + ip.isSourceNat());
        }

        boolean success = true;
        
        //Cleanup all ip address resources - PF/LB/Static nat rules
        if (!cleanupIpResources(addrId, userId, caller)) {
            success = false;
            s_logger.warn("Failed to release resources for ip address id=" + addrId);
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
        }

        if (success) {
            s_logger.debug("released a public ip id=" + addrId);
        }

        return success;
    }

    @Override
    public boolean configure(final String name, final Map<String, Object> params) throws ConfigurationException {
        _name = name;

        _configs = _configDao.getConfiguration("AgentManager", params);
        _networkGcWait = NumbersUtil.parseInt(_configs.get(Config.NetworkGcWait.key()), 600);
        _networkGcInterval = NumbersUtil.parseInt(_configs.get(Config.NetworkGcInterval.key()), 600);

        _configs = _configDao.getConfiguration("Network", params);
        _networkDomain = _configs.get(Config.GuestDomainSuffix.key());
        
        _cidrLimit = NumbersUtil.parseInt(_configs.get(Config.NetworkGuestCidrLimit.key()), 22);

        NetworkOfferingVO publicNetworkOffering = new NetworkOfferingVO(NetworkOfferingVO.SystemPublicNetwork, TrafficType.Public);
        publicNetworkOffering = _networkOfferingDao.persistDefaultNetworkOffering(publicNetworkOffering);
        _systemNetworks.put(NetworkOfferingVO.SystemPublicNetwork, publicNetworkOffering);
        NetworkOfferingVO managementNetworkOffering = new NetworkOfferingVO(NetworkOfferingVO.SystemManagementNetwork, TrafficType.Management);
        managementNetworkOffering = _networkOfferingDao.persistDefaultNetworkOffering(managementNetworkOffering);
        _systemNetworks.put(NetworkOfferingVO.SystemManagementNetwork, managementNetworkOffering);
        NetworkOfferingVO controlNetworkOffering = new NetworkOfferingVO(NetworkOfferingVO.SystemControlNetwork, TrafficType.Control);
        controlNetworkOffering = _networkOfferingDao.persistDefaultNetworkOffering(controlNetworkOffering);
        _systemNetworks.put(NetworkOfferingVO.SystemControlNetwork, controlNetworkOffering);
        NetworkOfferingVO storageNetworkOffering = new NetworkOfferingVO(NetworkOfferingVO.SystemStorageNetwork, TrafficType.Storage);
        storageNetworkOffering = _networkOfferingDao.persistDefaultNetworkOffering(storageNetworkOffering);
        _systemNetworks.put(NetworkOfferingVO.SystemStorageNetwork, storageNetworkOffering);
        NetworkOfferingVO guestNetworkOffering = new NetworkOfferingVO(
                NetworkOffering.SystemGuestNetwork, 
                "System Offering for System-Guest-Network", 
                TrafficType.Guest, 
                true, 
                false, 
                null, 
                null, 
                null, 
                true, 
                Availability.Required, 
                //services - all true except for firewall/lb/vpn and gateway services
                true, true, true, false, false,false, false, GuestIpType.Direct);
        guestNetworkOffering = _networkOfferingDao.persistDefaultNetworkOffering(guestNetworkOffering);
        _systemNetworks.put(NetworkOfferingVO.SystemGuestNetwork, guestNetworkOffering);

        NetworkOfferingVO defaultGuestNetworkOffering = new NetworkOfferingVO(
                NetworkOffering.DefaultVirtualizedNetworkOffering, 
                "Virtual Vlan", 
                TrafficType.Guest, 
                false, 
                false, 
                null, 
                null, 
                null, 
                true, 
                Availability.Required, 
                //services
                true, true, true, true,true, true, true, GuestIpType.Virtual);
        
        defaultGuestNetworkOffering = _networkOfferingDao.persistDefaultNetworkOffering(defaultGuestNetworkOffering);
        NetworkOfferingVO defaultGuestDirectNetworkOffering = new NetworkOfferingVO(
                NetworkOffering.DefaultDirectNetworkOffering, 
                "Direct", 
                TrafficType.Guest, 
                false, 
                true, 
                null, 
                null, 
                null, 
                true, 
                Availability.Optional, 
                //services - all true except for firewall/lb/vpn and gateway services
                true, true, true, false, false,false, false, GuestIpType.Direct);
        defaultGuestDirectNetworkOffering = _networkOfferingDao.persistDefaultNetworkOffering(defaultGuestDirectNetworkOffering);

        AccountsUsingNetworkSearch = _accountDao.createSearchBuilder();
        SearchBuilder<NetworkAccountVO> networkAccountSearch = _networksDao.createSearchBuilderForAccount();
        AccountsUsingNetworkSearch.join("nc", networkAccountSearch, AccountsUsingNetworkSearch.entity().getId(), networkAccountSearch.entity().getAccountId(), JoinType.INNER);
        networkAccountSearch.and("config", networkAccountSearch.entity().getNetworkId(), SearchCriteria.Op.EQ);
        networkAccountSearch.and("owner", networkAccountSearch.entity().isOwner(), SearchCriteria.Op.EQ);
        AccountsUsingNetworkSearch.done();

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

        IpAddressSearch = _ipAddressDao.createSearchBuilder();
        IpAddressSearch.and("accountId", IpAddressSearch.entity().getAllocatedToAccountId(), Op.EQ);
        IpAddressSearch.and("dataCenterId", IpAddressSearch.entity().getDataCenterId(), Op.EQ);
        IpAddressSearch.and("associatedWithNetworkId", IpAddressSearch.entity().getAssociatedWithNetworkId(), Op.EQ);
        SearchBuilder<VlanVO> virtualNetworkVlanSB = _vlanDao.createSearchBuilder();
        virtualNetworkVlanSB.and("vlanType", virtualNetworkVlanSB.entity().getVlanType(), Op.EQ);
        IpAddressSearch.join("virtualNetworkVlanSB", virtualNetworkVlanSB, IpAddressSearch.entity().getVlanId(), virtualNetworkVlanSB.entity().getId(), JoinBuilder.JoinType.INNER);
        IpAddressSearch.done();

        NicForTrafficTypeSearch = _nicDao.createSearchBuilder();
        SearchBuilder<NetworkVO> networkSearch = _networksDao.createSearchBuilder();
        NicForTrafficTypeSearch.join("network", networkSearch, networkSearch.entity().getId(), NicForTrafficTypeSearch.entity().getNetworkId(), JoinType.INNER);
        NicForTrafficTypeSearch.and("instance", NicForTrafficTypeSearch.entity().getInstanceId(), Op.EQ);
        networkSearch.and("traffictype", networkSearch.entity().getTrafficType(), Op.EQ);
        NicForTrafficTypeSearch.done();

        _executor = Executors.newScheduledThreadPool(1, new NamedThreadFactory("Network-Scavenger"));

        s_logger.info("Network Manager is configured.");

        return true;
    }

    @Override
    public String getName() {
        return _name;
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
    }

    @Override
    public List<IPAddressVO> listPublicIpAddressesInVirtualNetwork(long accountId, long dcId, Boolean sourceNat, Long associatedNetworkId) {
        SearchCriteria<IPAddressVO> sc = IpAddressSearch.create();
        sc.setParameters("accountId", accountId);
        sc.setParameters("dataCenterId", dcId);
        if (associatedNetworkId != null) {
            sc.setParameters("associatedWithNetworkId", associatedNetworkId);
        }
        
        if (sourceNat != null) {
            sc.addAnd("sourceNat", SearchCriteria.Op.EQ, sourceNat);
        }
        sc.setJoinParameters("virtualNetworkVlanSB", "vlanType", VlanType.VirtualNetwork);

        return _ipAddressDao.search(sc, null);
    }

    @Override
    public List<NetworkVO> setupNetwork(Account owner, NetworkOfferingVO offering, DeploymentPlan plan, String name, String displayText, boolean isShared, boolean isDefault) throws ConcurrentOperationException {
        return setupNetwork(owner, offering, null, plan, name, displayText, isShared, isDefault, false, null);
    }

    @Override
    @DB
    public List<NetworkVO> setupNetwork(Account owner, NetworkOfferingVO offering, Network predefined, DeploymentPlan plan, String name, String displayText, boolean isShared, boolean isDefault, boolean errorIfAlreadySetup, Long domainId) throws ConcurrentOperationException {
        Transaction.currentTxn();
        Account locked = _accountDao.acquireInLockTable(owner.getId());
        if (locked == null) {
            throw new ConcurrentOperationException("Unable to acquire lock on " + owner);
        }
        try {

            if (predefined == null || (predefined.getCidr() == null && predefined.getBroadcastUri() == null && predefined.getBroadcastDomainType() != BroadcastDomainType.Vlan)) {
                List<NetworkVO> configs = _networksDao.listBy(owner.getId(), offering.getId(), plan.getDataCenterId());
                if (configs.size() > 0) {
                    if (s_logger.isDebugEnabled()) {
                        s_logger.debug("Found existing network configuration for offering " + offering + ": " + configs.get(0));
                    }
                    
                    if (errorIfAlreadySetup) {
                        throw new InvalidParameterValueException("Found existing network configuration for offering " + offering + ": " + configs.get(0));
                    } else {
                        return configs;
                    }
                }
            } else if (predefined != null && predefined.getCidr() != null && predefined.getBroadcastUri() == null && predefined.getBroadcastUri() == null) {
                List<NetworkVO> configs = _networksDao.listBy(owner.getId(), offering.getId(), plan.getDataCenterId(), predefined.getCidr());
                if (configs.size() > 0) {
                    if (s_logger.isDebugEnabled()) {
                        s_logger.debug("Found existing network configuration for offering " + offering + ": " + configs.get(0));
                    }
                    
                    if (errorIfAlreadySetup) {
                        throw new InvalidParameterValueException("Found existing network configuration for offering " + offering + ": " + configs.get(0));
                    } else {
                        return configs;
                    }
                }
            }

            List<NetworkVO> configs = new ArrayList<NetworkVO>();

            long related = -1;

            for (NetworkGuru guru : _networkGurus) {
                Network config = guru.design(offering, plan, predefined, owner);
                if (config == null) {
                    continue;
                }

                if (config.getId() != -1) {
                    if (config instanceof NetworkVO) {
                        configs.add((NetworkVO) config);
                    } else {
                        configs.add(_networksDao.findById(config.getId()));
                    }
                    continue;
                }

                long id = _networksDao.getNextInSequence(Long.class, "id");
                if (related == -1) {
                    related = id;
                }

                NetworkVO vo = new NetworkVO(id, config, offering.getId(), plan.getDataCenterId(), guru.getName(), owner.getDomainId(), owner.getId(), related, name, displayText, isShared, isDefault, predefined.isSecurityGroupEnabled());
                configs.add(_networksDao.persist(vo, vo.getGuestType() != null));
                
                if (domainId != null) {
                    _networksDao.addDomainToNetwork(id, domainId);
                }
            }

            if (configs.size() < 1) {
                throw new CloudRuntimeException("Unable to convert network offering to network profile: " + offering.getId());
            }

            return configs;
        } finally {
            s_logger.debug("Releasing lock for " + locked);
            _accountDao.releaseFromLockTable(locked.getId());
        }
    }

    @Override
    public List<NetworkOfferingVO> getSystemAccountNetworkOfferings(String... offeringNames) {
        List<NetworkOfferingVO> offerings = new ArrayList<NetworkOfferingVO>(offeringNames.length);
        for (String offeringName : offeringNames) {
            NetworkOfferingVO network = _systemNetworks.get(offeringName);
            if (network == null) {
                throw new CloudRuntimeException("Unable to find system network profile for " + offeringName);
            }
            offerings.add(network);
        }
        return offerings;
    }

    @Override
    @DB
    public void allocate(VirtualMachineProfile<? extends VMInstanceVO> vm, List<Pair<NetworkVO, NicProfile>> networks) throws InsufficientCapacityException, ConcurrentOperationException {
        Transaction txn = Transaction.currentTxn();
        txn.start();

        int deviceId = 0;

        boolean[] deviceIds = new boolean[networks.size()];
        Arrays.fill(deviceIds, false);

        List<NicVO> nics = new ArrayList<NicVO>(networks.size());
        NicVO defaultNic = null;

        for (Pair<NetworkVO, NicProfile> network : networks) {
            NetworkVO config = network.first();
            NetworkGuru guru = _networkGurus.get(config.getGuruName());
            NicProfile requested = network.second();
            if (requested != null && requested.getMode() == null) {
                requested.setMode(config.getMode());
            }
            NicProfile profile = guru.allocate(config, requested, vm);

            if (vm != null && vm.getVirtualMachine().getType() == Type.User && config.isDefault()) {
                profile.setDefaultNic(true);
            }

            if (profile == null) {
                continue;
            }
            
            if (requested != null && requested.getMode() == null) {
                profile.setMode(requested.getMode());
            } else {
                profile.setMode(config.getMode());
            }
            
            NicVO vo = new NicVO(guru.getName(), vm.getId(), config.getId(), vm.getType());

            while (deviceIds[deviceId] && deviceId < deviceIds.length) {
                deviceId++;
            }

            deviceId = applyProfileToNic(vo, profile, deviceId);

            vo = _nicDao.persist(vo);

            if (vo.isDefaultNic()) {
                if (defaultNic != null) {
                    throw new IllegalArgumentException("You cannot specify two nics as default nics: nic 1 = " + defaultNic + "; nic 2 = " + vo);
                }
                defaultNic = vo;
            }

            int devId = vo.getDeviceId();
            if (devId > deviceIds.length) {
                throw new IllegalArgumentException("Device id for nic is too large: " + vo);
            }
            if (deviceIds[devId]) {
                throw new IllegalArgumentException("Conflicting device id for two different nics: " + devId);
            }

            deviceIds[devId] = true;
            nics.add(vo);

            NetworkOffering no = _configMgr.getNetworkOffering(config.getNetworkOfferingId());
            Integer networkRate = _configMgr.getNetworkRate(no.getId(), vm.getType());
            vm.addNic(new NicProfile(vo, network.first(), vo.getBroadcastUri(), vo.getIsolationUri(), networkRate));
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

    protected Integer applyProfileToNic(NicVO vo, NicProfile profile, Integer deviceId) {
        if (profile.getDeviceId() != null) {
            vo.setDeviceId(profile.getDeviceId());
        } else if (deviceId != null) {
            vo.setDeviceId(deviceId++);
        }

        vo.setReservationStrategy(profile.getReservationStrategy());

        vo.setDefaultNic(profile.isDefaultNic());

        if (profile.getIp4Address() != null) {
            vo.setIp4Address(profile.getIp4Address());
            vo.setAddressFormat(AddressFormat.Ip4);
        }

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
        return deviceId;
    }

    protected void applyProfileToNicForRelease(NicVO vo, NicProfile profile) {
        vo.setGateway(profile.getGateway());
        vo.setAddressFormat(profile.getFormat());
        vo.setIp4Address(profile.getIp4Address());
        vo.setIp6Address(profile.getIp6Address());
        vo.setMacAddress(profile.getMacAddress());
        vo.setReservationStrategy(profile.getReservationStrategy());
        vo.setBroadcastUri(profile.getBroadCastUri());
        vo.setIsolationUri(profile.getIsolationUri());
        vo.setNetmask(profile.getNetmask());
    }
    
    protected void applyProfileToNetwork(NetworkVO network, NetworkProfile profile) {
        network.setBroadcastUri(profile.getBroadcastUri());
        network.setDns1(profile.getDns1());
        network.setDns2(profile.getDns2());
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

        Integer networkRate = _configMgr.getNetworkRate(config.getNetworkOfferingId(), null);
        to.setNetworkRateMbps(networkRate);

        return to;
    }

    @Override
    @DB
    public Pair<NetworkGuru, NetworkVO> implementNetwork(long networkId, DeployDestination dest, ReservationContext context) throws ConcurrentOperationException, ResourceUnavailableException, InsufficientCapacityException {
        Transaction.currentTxn();
        Pair<NetworkGuru, NetworkVO> implemented = new Pair<NetworkGuru, NetworkVO>(null, null);

        NetworkVO network = _networksDao.acquireInLockTable(networkId);
        if (network == null) {
            throw new ConcurrentOperationException("Unable to acquire network configuration: " + networkId);
        }

        try {
            NetworkGuru guru = _networkGurus.get(network.getGuruName());
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
            network.setState(Network.State.Implementing);

            _networksDao.update(networkId, network);

            Network result = guru.implement(network, offering, dest, context);
            network.setCidr(result.getCidr());
            network.setBroadcastUri(result.getBroadcastUri());
            network.setGateway(result.getGateway());
            network.setMode(result.getMode());
            _networksDao.update(networkId, network);
            
            //If network if guest virtual and there is no source nat ip, associate a new one
            if (network.getGuestType() == GuestIpType.Virtual) {
                List<IPAddressVO> ips = _ipAddressDao.listByAssociatedNetwork(networkId, true);
                
                if (ips.isEmpty()) {
                    s_logger.debug("Creating a source natp ip for " + network);
                    Account owner = _accountMgr.getAccount(network.getAccountId());
                    PublicIp sourceNatIp = assignSourceNatIpAddress(owner, network, context.getCaller().getId());
                    if (sourceNatIp == null) {
                        throw new InsufficientAddressCapacityException("Unable to assign source nat ip address to the network " + network, DataCenter.class, network.getDataCenterId());
                    }
                }
            }
            
            for (NetworkElement element : _networkElements) {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Asking " + element.getName() + " to implmenet " + network);
                }
                element.implement(network, offering, dest, context);
            }
            
            //reapply all the firewall/staticNat/lb rules
            s_logger.debug("Applying network rules as a part of network " +  network + " implement...");
            if (!restartNetwork(networkId, false, context.getAccount())) {
                s_logger.warn("Failed to reapply network rules as a part of network " + network + " implement");
                throw new ResourceUnavailableException("Unable to apply network rules as a part of network " + network + " implement", DataCenter.class, network.getDataCenterId());
            } 
            
            network.setState(Network.State.Implemented);
            _networksDao.update(network.getId(), network);
            implemented.set(guru, network);
            return implemented;
        } finally {
            if (implemented.first() == null) {
                s_logger.debug("Cleaning up because we're unable to implement the network " + network);
                network.setState(Network.State.Shutdown);
                _networksDao.update(networkId, network);
                
                shutdownNetwork(networkId, context);
            }
            _networksDao.releaseFromLockTable(networkId);
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
        txn.commit();
    }

    @Override
    public void prepare(VirtualMachineProfile<? extends VMInstanceVO> vmProfile, DeployDestination dest, ReservationContext context) throws InsufficientCapacityException, ConcurrentOperationException, ResourceUnavailableException {
        List<NicVO> nics = _nicDao.listByVmId(vmProfile.getId());
        
        // we have to implement default nics first - to ensure that default network elements start up first in multiple nics case)
        // (need for setting DNS on Dhcp to domR's Ip4 address)
        Collections.sort(nics, new Comparator<NicVO>() {
            
            public int compare(NicVO nic1, NicVO nic2){
                boolean isDefault1 = getNetwork(nic1.getNetworkId()).isDefault();
                boolean isDefault2 = getNetwork(nic2.getNetworkId()).isDefault();
               
                return (isDefault1 ^ isDefault2) ? ((isDefault1 ^ true) ? 1 : -1) : 0;
            }
        });
        
        for (NicVO nic : nics) {
            Pair<NetworkGuru, NetworkVO> implemented = implementNetwork(nic.getNetworkId(), dest, context);
            NetworkGuru guru = implemented.first();
            NetworkVO network = implemented.second();
            NetworkOffering no = _configMgr.getNetworkOffering(network.getNetworkOfferingId());
            Integer networkRate = _configMgr.getNetworkRate(no.getId(), vmProfile.getType());
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

                profile = new NicProfile(nic, network, broadcastUri, isolationUri, networkRate);
                guru.reserve(profile, network, vmProfile, dest, context);
                nic.setIp4Address(profile.getIp4Address());
                nic.setIp6Address(profile.getIp6Address());
                nic.setMacAddress(profile.getMacAddress());
                nic.setIsolationUri(profile.getIsolationUri());
                nic.setBroadcastUri(profile.getBroadCastUri());
                nic.setReserver(guru.getName());
                nic.setState(Nic.State.Reserved);
                nic.setNetmask(profile.getNetmask());
                nic.setGateway(profile.getGateway());
                nic.setAddressFormat(profile.getFormat());
                updateNic(nic, network.getId(), 1);
            } else {
                profile = new NicProfile(nic, network, nic.getBroadcastUri(), nic.getIsolationUri(), networkRate);
                nic.setState(Nic.State.Reserved);
                updateNic(nic, network.getId(), 1);
            }

            for (NetworkElement element : _networkElements) {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Asking " + element.getName() + " to prepare for " + nic);
                }
                element.prepare(network, profile, vmProfile, dest, context);
            }
            profile.setSecurityGroupEnabled(network.isSecurityGroupEnabled());
            guru.updateNicProfile(profile, network);
            vmProfile.addNic(profile);
        }
    }

    @Override
    public <T extends VMInstanceVO> void prepareNicForMigration(VirtualMachineProfile<T> vm, DeployDestination dest) {
        List<NicVO> nics = _nicDao.listByVmId(vm.getId());
        for (NicVO nic : nics) {
            NetworkVO network = _networksDao.findById(nic.getNetworkId());
            NetworkOffering no = _configMgr.getNetworkOffering(network.getNetworkOfferingId());
            Integer networkRate = _configMgr.getNetworkRate(no.getId(), vm.getType());

            NetworkGuru guru = _networkGurus.get(network.getGuruName());
            NicProfile profile = new NicProfile(nic, network, nic.getBroadcastUri(), nic.getIsolationUri(), networkRate);
            guru.updateNicProfile(profile, network);
            vm.addNic(profile);
        }
    }

    @Override
    public void release(VirtualMachineProfile<? extends VMInstanceVO> vmProfile, boolean forced) {
        List<NicVO> nics = _nicDao.listByVmId(vmProfile.getId());
        for (NicVO nic : nics) {
            NetworkVO network = _networksDao.findById(nic.getNetworkId());
            if (nic.getState() == Nic.State.Reserved || nic.getState() == Nic.State.Reserving) {
                Nic.State originalState = nic.getState();
                if (nic.getReservationStrategy() == Nic.ReservationStrategy.Start) {
                    NetworkGuru guru = _networkGurus.get(network.getGuruName());
                    nic.setState(Nic.State.Releasing);
                    _nicDao.update(nic.getId(), nic);
                    NicProfile profile = new NicProfile(nic, network, null, null, null);
                    if (guru.release(profile, vmProfile, nic.getReservationId())) {
                        applyProfileToNicForRelease(nic, profile);
                        nic.setState(Nic.State.Allocated);
                        if (originalState == Nic.State.Reserved) {
                            updateNic(nic, network.getId(), -1);
                        } else {
                            _nicDao.update(nic.getId(), nic);
                        }
                    }
                } else {
                    nic.setState(Nic.State.Allocated);
                    updateNic(nic, network.getId(), -1);
                }
            }
        }
    }
    
    @Override
    public List<? extends Nic> getNics(long vmId) {
        return _nicDao.listByVmId(vmId);
    }
    
    @Override
    public List<? extends Nic> getNicsIncludingRemoved(VirtualMachine vm) {
        return _nicDao.listByVmIdIncludingRemoved(vm.getId());
    }

    @Override
    public List<NicProfile> getNicProfiles(VirtualMachine vm) {
        List<NicVO> nics = _nicDao.listByVmId(vm.getId());
        List<NicProfile> profiles = new ArrayList<NicProfile>();

        if (nics != null) {
            for (Nic nic : nics) {
                NetworkVO network = _networksDao.findById(nic.getNetworkId());
                NetworkOffering no = _configMgr.getNetworkOffering(network.getNetworkOfferingId());
                Integer networkRate = _configMgr.getNetworkRate(no.getId(), vm.getType());

                NetworkGuru guru = _networkGurus.get(network.getGuruName());
                NicProfile profile = new NicProfile(nic, network, nic.getBroadcastUri(), nic.getIsolationUri(), networkRate);
                guru.updateNicProfile(profile, network);
                profiles.add(profile);
            }
        }
        return profiles;
    }

    @Override @DB @ActionEvent (eventType=EventTypes.EVENT_NET_IP_RELEASE, eventDescription="disassociating Ip", async=true)
    public boolean disassociateIpAddress(DisassociateIPAddrCmd cmd){

        Long userId = UserContext.current().getCallerUserId();
        Account caller = UserContext.current().getCaller();
        Long ipAddressId = cmd.getIpAddressId();

        // Verify input parameters
        IPAddressVO ipVO = _ipAddressDao.findById(ipAddressId);
        if (ipVO == null) {
            throw new InvalidParameterValueException("Unable to find ip address by id " + ipAddressId);
        }
        
        if (ipVO.getAllocatedTime() == null) {
            s_logger.debug("Ip Address id= " + ipAddressId + " is not allocated, so do nothing.");
            return true;
        }

        if (ipVO.getAllocatedToAccountId() != null) {
            _accountMgr.checkAccess(caller, ipVO);
        }

        if (ipVO.isSourceNat()) {
            throw new IllegalArgumentException("ip address is used for source nat purposes and can not be disassociated.");
        }

        VlanVO vlan = _vlanDao.findById(ipVO.getVlanId());
        if (!vlan.getVlanType().equals(VlanType.VirtualNetwork)) {
            throw new IllegalArgumentException("only ip addresses that belong to a virtual network may be disassociated.");
        }

        // Check for account wide pool. It will have an entry for account_vlan_map.
        if (_accountVlanMapDao.findAccountVlanMap(ipVO.getAccountId(), ipVO.getVlanId()) != null) {
            throw new InvalidParameterValueException("Ip address id=" + ipAddressId + " belongs to Account wide IP pool and cannot be disassociated");
        }

        return releasePublicIpAddress(ipAddressId, userId, caller);
    }

    @Override
    public List<AccountVO> getAccountsUsingNetwork(long networkId) {
        SearchCriteria<AccountVO> sc = AccountsUsingNetworkSearch.create();
        sc.setJoinParameters("nc", "config", networkId);
        return _accountDao.search(sc, null);
    }

    @Override
    public AccountVO getNetworkOwner(long networkId) {
        SearchCriteria<AccountVO> sc = AccountsUsingNetworkSearch.create();
        sc.setJoinParameters("nc", "config", networkId);
        sc.setJoinParameters("nc", "owner", true);
        List<AccountVO> accounts = _accountDao.search(sc, null);
        return accounts.size() != 0 ? accounts.get(0) : null;
    }

    @Override
    public List<NetworkVO> getNetworksforOffering(long offeringId, long dataCenterId, long accountId) {
        return _networksDao.getNetworksForOffering(offeringId, dataCenterId, accountId);
    }

    @Override
    public List<NetworkOfferingVO> listNetworkOfferings() {
        return _networkOfferingDao.listNonSystemNetworkOfferings();
    }

    @Override
    public String getNextAvailableMacAddressInNetwork(long networkId) throws InsufficientAddressCapacityException {
        String mac = _networksDao.getNextAvailableMacAddress(networkId);
        if (mac == null) {
            throw new InsufficientAddressCapacityException("Unable to create another mac address", Network.class, networkId);
        }

        return mac;
    }

    @Override
    @DB
    public Network getNetwork(long id) {
        return _networksDao.findById(id);
    }

    @Override
    public List<? extends RemoteAccessVpnElement> getRemoteAccessVpnElements() {
        List<RemoteAccessVpnElement> elements = new ArrayList<RemoteAccessVpnElement>();
        for (NetworkElement element : _networkElements) {
            if (element instanceof RemoteAccessVpnElement) {
                elements.add((RemoteAccessVpnElement) element);
            }
        }

        return elements;
    }

    @Override
    public void cleanupNics(VirtualMachineProfile<? extends VMInstanceVO> vm) {
        List<NicVO> nics = _nicDao.listByVmId(vm.getId());
        for (NicVO nic : nics) {
            nic.setState(Nic.State.Deallocating);
            _nicDao.update(nic.getId(), nic);
            NetworkVO network = _networksDao.findById(nic.getNetworkId());
            NicProfile profile = new NicProfile(nic, network, null, null, null);
            NetworkGuru guru = _networkGurus.get(network.getGuruName());
            guru.deallocate(network, profile, vm);
            _nicDao.remove(nic.getId());
        }
    }

    @Override
    public void expungeNics(VirtualMachineProfile<? extends VMInstanceVO> vm) {
        List<NicVO> nics = _nicDao.listIncludingRemovedBy(vm.getId());
        for (NicVO nic : nics) {
            _nicDao.expunge(nic.getId());
        }
    }

    @Override @DB @ActionEvent (eventType=EventTypes.EVENT_NETWORK_CREATE, eventDescription="creating network")
    public Network createNetwork(CreateNetworkCmd cmd) throws InsufficientCapacityException, ConcurrentOperationException {
        Long networkOfferingId = cmd.getNetworkOfferingId();
        Long zoneId = cmd.getZoneId();
        String gateway = cmd.getGateway();
        String startIP = cmd.getStartIp();
        String endIP = cmd.getEndIp();
        String netmask = cmd.getNetmask();
        String networkDomain = cmd.getNetworkDomain();
        String vlanId = cmd.getVlan();
        String name = cmd.getNetworkName();
        String displayText = cmd.getDisplayText();
        Boolean isShared = cmd.getIsShared();
        Boolean isDefault = cmd.isDefault();
        Long userId = UserContext.current().getCallerUserId();
        Account caller = UserContext.current().getCaller();
        boolean isDomainSpecific = false;
        
        Transaction txn = Transaction.currentTxn();
        
        // Check if network offering exists
        NetworkOfferingVO networkOffering = _networkOfferingDao.findById(networkOfferingId);
        if (networkOffering == null || networkOffering.isSystemOnly()) {
            throw new InvalidParameterValueException("Unable to find network offeirng by id " + networkOfferingId);
        }
       
        //Check if the network is domain specific
        if (cmd.getDomainId() != null && cmd.getAccountName() == null) {
            if (networkOffering.getTrafficType() != TrafficType.Guest || networkOffering.getGuestType() != GuestIpType.Direct) {
                throw new InvalidParameterValueException("Domain level networks are supported just for traffic type " + TrafficType.Guest + " and guest Ip type " + GuestIpType.Direct);
            } else if (isShared == null || !isShared) {
                throw new InvalidParameterValueException("Network dedicated to domain should be shared");
            } else {
                DomainVO domain = _domainDao.findById(cmd.getDomainId());
                if (domain == null) {
                    throw new InvalidParameterValueException("Unable to find domain by id " + cmd.getDomainId());
                }
                _accountMgr.checkAccess(caller, domain);
                isDomainSpecific = true;
            }
        }
        
        Account owner = null;
        if (cmd.getAccountName() != null && cmd.getDomainId() != null) {
            owner = _accountMgr.finalizeOwner(caller, cmd.getAccountName(), cmd.getDomainId());
        } else {
            owner = caller;
        }
        
        UserContext.current().setAccountId(owner.getAccountId());
        
        // if end ip is not specified, default it to startIp
        if (endIP == null && startIP != null) {
            endIP = startIP;
        }
        
        // Check if zone exists
        if (zoneId == null) {
            throw new InvalidParameterValueException("Please specify a valid zone.");
        }

		DataCenterVO zone = _dcDao.findById(zoneId);
        if (zone == null) {
            throw new InvalidParameterValueException("Please specify a valid zone.");
        }
        
		if(Grouping.AllocationState.Disabled == zone.getAllocationState() && !_accountMgr.isRootAdmin(caller.getType())){
			throw new PermissionDeniedException("Cannot perform this operation, Zone is currently disabled: "+ zoneId );
		}

        //Check if network offering is Available
        if (networkOffering.getAvailability() == Availability.Unavailable) {
            throw new InvalidParameterValueException("Can't create network; network offering id=" + networkOfferingId + " is " + networkOffering.getAvailability());
        }

        //If one of the following parameters are defined (starIP/endIP/netmask/gateway), all the rest should be defined too
        ArrayList<String> networkConfigs = new ArrayList<String>();
        networkConfigs.add(gateway);
        networkConfigs.add(startIP);
        networkConfigs.add(endIP);
        networkConfigs.add(netmask);
        boolean defineNetworkConfig = false;
        short configElementsCount = 0;
        
        for (String networkConfig : networkConfigs) {
            if (networkConfig != null) {
                configElementsCount++;
            }
        }
        
        if (configElementsCount > 0 && configElementsCount != networkConfigs.size()) {
            throw new InvalidParameterValueException("startIP/endIP/netmask/gateway must be specified together");
        } else if (configElementsCount == networkConfigs.size()) {
            defineNetworkConfig = true;
        }
        
        String cidr = null;
        if (gateway != null && netmask != null) {
            cidr = NetUtils.ipAndNetMaskToCidr(gateway, netmask);
        }
        
        //Regular user can create guest virtual network only
        if (caller.getType() == Account.ACCOUNT_TYPE_NORMAL && (networkOffering.getTrafficType() != TrafficType.Guest || networkOffering.getGuestType() != GuestIpType.Virtual)) {
            throw new InvalidParameterValueException("Regular user can create a network only from the network offering having traffic type " + TrafficType.Guest + " and Guest Ip type " + GuestIpType.Virtual);
        }
        
        //Don't allow to specify cidr if the caller is a regular user
        if (caller.getType() == Account.ACCOUNT_TYPE_NORMAL && (cidr != null || vlanId != null)) {
            throw new InvalidParameterValueException("Regular user is not allowed to specify gateway/netmask/ipRange/vlanId");
        }
        
        //For non-root admins check cidr limit - if it's allowed by global config value
        if (caller.getType() != Account.ACCOUNT_TYPE_ADMIN && cidr != null) {
            
            String[] cidrPair = cidr.split("\\/");
            int cidrSize = Integer.valueOf(cidrPair[1]);
  
            if (cidrSize < _cidrLimit) {
                throw new InvalidParameterValueException("Cidr size can't be less than " + _cidrLimit);
            }
        }
        
        txn.start();
        
        Long domainId = null;
        if (isDomainSpecific) {
            domainId = cmd.getDomainId();
        }
        
        Network network = createNetwork(networkOfferingId, name, displayText, isShared, isDefault, zoneId, gateway, cidr, vlanId, networkDomain, owner, false, domainId);
        
        // Don't pass owner to create vlan when network offering is of type Direct - done to prevent accountVlanMap entry
        // creation when vlan is mapped to network
        if (network.getGuestType() == GuestIpType.Direct) {
            owner = null;
        }

        if (caller.getType() == Account.ACCOUNT_TYPE_ADMIN && network.getGuestType() == GuestIpType.Direct && defineNetworkConfig) {
            // Create vlan ip range
            _configMgr.createVlanAndPublicIpRange(userId, zoneId, null, startIP, endIP, gateway, netmask, false, vlanId, owner, network.getId());
        }
        
        txn.commit();
        
        return network;
    }

    @Override @DB
    public Network createNetwork(long networkOfferingId, String name, String displayText, Boolean isShared, Boolean isDefault, Long zoneId, String gateway, String cidr, String vlanId, String networkDomain, Account owner, boolean isSecurityGroupEnabled, Long domainId)
            throws ConcurrentOperationException, InsufficientCapacityException {
        
        NetworkOfferingVO networkOffering = _networkOfferingDao.findById(networkOfferingId);
        DataCenterVO zone = _dcDao.findById(zoneId);
        
        //Only Direct Account specific networks can be created in Advanced Security Group enabled zone
        if (zone.isSecurityGroupEnabled() && (networkOffering.getGuestType() == GuestIpType.Virtual  || (isShared != null && isShared))) {
            throw new InvalidParameterValueException("Virtual Network and Direct Shared Network creation is not allowed if zone is security group enabled");
        }
        
        if (zone.getNetworkType() == NetworkType.Basic) {
            throw new InvalidParameterValueException("Network creation is not allowed in zone with network type " + NetworkType.Basic);
        }
        
        // allow isDefault/isShared to be set only for Direct network
        if (networkOffering.getGuestType() == GuestIpType.Virtual) {
            if (isDefault != null && !isDefault) {
                throw new InvalidParameterValueException("Can specify isDefault parameter only for Direct network.");
            } else {
                isDefault = true;
            }
            if (isShared != null && isShared) {
                throw new InvalidParameterValueException("Can specify isShared parameter for Direct networks only");
            }
        } else {
            if (isDefault == null) {
                isDefault = false;
            }
        }
        
        //if network is shared, defult its owner to be system
        if (isShared) {
            owner = _accountMgr.getSystemAccount();
        }
        
        // Don't allow to create network with vlan that already exists in the system
        if (vlanId != null) {
            String uri = "vlan://" + vlanId;
            List<NetworkVO> networks = _networksDao.listBy(zoneId, uri);
            if ((networks != null && !networks.isEmpty())) {
                throw new InvalidParameterValueException("Network with vlan " + vlanId + " already exists in zone " + zoneId);
            }
        }
        
        // VlanId can be specified only when network offering supports it
        if (vlanId != null && !networkOffering.getSpecifyVlan()) {
            throw new InvalidParameterValueException("Can't specify vlan because network offering doesn't support it");
        }
        
        //Don't allow to create guest virtual network with Vlan specified
        if (networkOffering.getGuestType() == GuestIpType.Virtual && vlanId != null) {
            throw new InvalidParameterValueException("Can't specify vlan when create network with Guest IP Type " + GuestIpType.Virtual);
        }
        
        // If networkDomain is not specified, take it from the global configuration
        if (networkDomain == null) {
            networkDomain = "cs"+Long.toHexString(owner.getId())+_networkDomain;
        } else {
            //validate network domain
            if (!NetUtils.verifyDomainName(networkDomain)) {
                throw new InvalidParameterValueException("Invalid network domain. Total length shouldn't exceed 190 chars. Each domain label must be between 1 and 63 characters long, can contain ASCII letters 'a' through 'z', the digits '0' through '9', " +  "and the hyphen ('-'); can't start or end with \"-\"");
            }
        }
        
        //Cidr for Direct network can't be NULL - 2.2.x limitation, remove after we introduce support for multiple ip ranges with different Cidrs for the same Shared network
        if (cidr == null && networkOffering.getTrafficType() == TrafficType.Guest && networkOffering.getGuestType() == GuestIpType.Direct) {
            throw new InvalidParameterValueException("StartIp/endIp/gateway/netmask are required for Direct network creation");
        }
        
        //Check if cidr is RFC1918 compliant if the network is Guest Virtual
        if (cidr != null && networkOffering.getGuestType() == GuestIpType.Virtual && networkOffering.getTrafficType() == TrafficType.Guest) {
            if (!NetUtils.validateGuestCidr(cidr)) {
                throw new InvalidParameterValueException("Virtual Guest Cidr " + cidr + " is not RFC1918 compliant");
            }
        }

        Transaction txn = Transaction.currentTxn();
        txn.start();
        
        DataCenterDeployment plan = new DataCenterDeployment(zoneId, null, null, null, null);
        NetworkVO userNetwork = new NetworkVO();
        userNetwork.setNetworkDomain(networkDomain);
        userNetwork.setSecurityGroupEnabled(isSecurityGroupEnabled);

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
        
        List<NetworkVO> networks = setupNetwork(owner, networkOffering, userNetwork, plan, name, displayText, isShared, isDefault, true, domainId);

        Network network = null;
        if (networks == null || networks.isEmpty()) {
            throw new CloudRuntimeException("Fail to create a network");
        } else {
            if (networks.size() > 0 && networks.get(0).getGuestType() == GuestIpType.Virtual && networks.get(0).getTrafficType() == TrafficType.Guest) {
                Network defaultGuestNetwork = networks.get(0);
                for (Network nw : networks) {
                    if (nw.getCidr() != null && nw.getCidr().equals(zone.getGuestNetworkCidr())) {
                        defaultGuestNetwork = nw;
                    }
                }
                network = defaultGuestNetwork;
            } else {
                network = networks.get(0);
            }
        }

        txn.commit();
        UserContext.current().setEventDetails("Network Id: "+ network.getId());
        return network;
    }

    @Override
    public List<? extends Network> searchForNetworks(ListNetworksCmd cmd) {
        Object id = cmd.getId();
        Object keyword = cmd.getKeyword();
        Long zoneId = cmd.getZoneId();
        Account account = UserContext.current().getCaller();
        Long domainId = cmd.getDomainId();
        String accountName = cmd.getAccountName();
        String type = cmd.getType();
        String trafficType = cmd.getTrafficType();
        Boolean isSystem = cmd.getIsSystem();
        Boolean isShared = cmd.getIsShared();
        Boolean isDefault = cmd.isDefault();
        Long accountId = null;
        String path = null;
        List<Long> avoidNetworks = new ArrayList<Long>();
        List<Long> allowedSharedNetworks = new ArrayList<Long>();

        if (isSystem == null) {
            isSystem = false;
        }

        // Account/domainId parameters and isSystem are mutually exclusive
        if (isSystem && (accountName != null || domainId != null)) {
            throw new InvalidParameterValueException("System network belongs to system, account and domainId parameters can't be specified");
        }

        if (_accountMgr.isAdmin(account.getType())) {
            if (domainId != null) {
                if ((account != null) && !_domainDao.isChildDomain(account.getDomainId(), domainId)) {
                    throw new PermissionDeniedException("Invalid domain id (" + domainId + ") given, unable to list networks");
                }

                if (accountName != null) {
                    account = _accountMgr.getActiveAccount(accountName, domainId);
                    if (account == null) {
                        throw new InvalidParameterValueException("Unable to find account " + accountName + " in domain " + domainId);
                    }
                    accountId = account.getId();
                }
            } else {
                accountId = account.getId();
            }
            
            if (account.getType() == Account.ACCOUNT_TYPE_DOMAIN_ADMIN) {
                DomainVO domain = _domainDao.findById(account.getDomainId());
                if (domain != null) {
                    path = domain.getPath();
                }
            }
        } else {
            accountName = account.getAccountName();
            domainId = account.getDomainId();
            accountId = account.getId();
        }

        Filter searchFilter = new Filter(NetworkVO.class, "id", false, cmd.getStartIndex(), cmd.getPageSizeVal());
        SearchBuilder<NetworkVO> sb = _networksDao.createSearchBuilder();

        // Don't display networks created of system network offerings
        SearchBuilder<NetworkOfferingVO> networkOfferingSearch = _networkOfferingDao.createSearchBuilder();
        networkOfferingSearch.and("systemOnly", networkOfferingSearch.entity().isSystemOnly(), SearchCriteria.Op.EQ);
        if (isSystem) {
            networkOfferingSearch.and("trafficType", networkOfferingSearch.entity().getTrafficType(), SearchCriteria.Op.EQ);
        }
        sb.join("networkOfferingSearch", networkOfferingSearch, sb.entity().getNetworkOfferingId(), networkOfferingSearch.entity().getId(), JoinBuilder.JoinType.INNER);

        SearchBuilder<DataCenterVO> zoneSearch = _dcDao.createSearchBuilder();
        zoneSearch.and("networkType", zoneSearch.entity().getNetworkType(), SearchCriteria.Op.EQ);
        sb.join("zoneSearch", zoneSearch, sb.entity().getDataCenterId(), zoneSearch.entity().getId(), JoinBuilder.JoinType.INNER);
        
        
        if (path != null) {
            //for domain admin we should show only subdomains information
            SearchBuilder<DomainVO> domainSearch = _domainDao.createSearchBuilder();
            domainSearch.and("path", domainSearch.entity().getPath(), SearchCriteria.Op.LIKE);
            sb.join("domainSearch", domainSearch, sb.entity().getDomainId(), domainSearch.entity().getId(), JoinBuilder.JoinType.INNER);
        }

        sb.and("removed", sb.entity().getRemoved(), Op.NULL);
        
        SearchCriteria<NetworkVO> sc = sb.create();

        if (!isSystem) {
            if (zoneId != null) {
                DataCenterVO dc = _dcDao.findById(zoneId);
                if (dc != null && !dc.isSecurityGroupEnabled()) {
                    sc.setJoinParameters("networkOfferingSearch", "systemOnly", false);
                }
            } else {
                sc.setJoinParameters("networkOfferingSearch", "systemOnly", false);
            }
        } else {
            sc.setJoinParameters("networkOfferingSearch", "systemOnly", true);
            sc.setJoinParameters("zoneSearch", "networkType", NetworkType.Advanced.toString());
        }

        if (keyword != null) {
            SearchCriteria<NetworkVO> ssc = _networksDao.createSearchCriteria();
            ssc.addOr("name", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            sc.addAnd("name", SearchCriteria.Op.SC, ssc);
        }

        if (id != null) {
            sc.addAnd("id", SearchCriteria.Op.EQ, id);
        }

        if (zoneId != null) {
            sc.addAnd("dataCenterId", SearchCriteria.Op.EQ, zoneId);
        }

        if (type != null) {
            sc.addAnd("guestType", SearchCriteria.Op.EQ, type);
        }

        if (!isSystem) { 
            if (accountName != null && domainId != null) {  
                if (isShared == null) {
                    sc.addAnd("accountId", SearchCriteria.Op.EQ, accountId);
                    //sc.addOr("isShared", SearchCriteria.Op.EQ, true);
                } else if (!isShared) {
                    sc.addAnd("accountId", SearchCriteria.Op.EQ, accountId);
                } else {
                    sc.addAnd("isShared", SearchCriteria.Op.EQ, true);
                }
                
                if (isShared == null || isShared) {
                    List<NetworkVO> allNetworks = _networksDao.listNetworksBy(true);
                    for (NetworkVO network : allNetworks) {
                        if (!isNetworkAvailableInDomain(network.getId(), domainId)) {
                            avoidNetworks.add(network.getId());
                        } else {
                            allowedSharedNetworks.add(network.getId());
                        }
                    }
                }
                
            } else if (isShared != null) {
                sc.addAnd("isShared", SearchCriteria.Op.EQ, isShared);
            }    
        }
        
        //find list of shared networks to avoid
        if (domainId != null && accountName == null) {
            List<NetworkVO> allNetworks = _networksDao.listNetworksBy(true);
            for (NetworkVO network : allNetworks) {
                if (!isNetworkAvailableInDomain(network.getId(), domainId)) {
                    avoidNetworks.add(network.getId());
                }
            }
            sc.addAnd("isShared", SearchCriteria.Op.EQ, true);
        }
        
        for (Long avoidNetwork : avoidNetworks) {
            sc.addAnd("id", SearchCriteria.Op.NOTIN, avoidNetwork);
        }
        
        for (Long allowerdSharedNetwork : allowedSharedNetworks) {
            sc.addOr("id", SearchCriteria.Op.IN, allowerdSharedNetwork);
        }

        if (isDefault != null) {
            sc.addAnd("isDefault", SearchCriteria.Op.EQ, isDefault);
        }

        if (trafficType != null) {
            sc.addAnd("trafficType", SearchCriteria.Op.EQ, trafficType);
        }
        
        if (!isSystem && path != null && (isShared == null || !isShared)) {
            sc.setJoinParameters("domainSearch", "path", path + "%");
        }

        List<NetworkVO> networks = _networksDao.search(sc, searchFilter);

        return networks;
    }

    @Override @ActionEvent (eventType=EventTypes.EVENT_NETWORK_DELETE, eventDescription="deleting network", async=true)
    public boolean deleteNetwork(long networkId){   
        
        Account caller = UserContext.current().getCaller(); 

        // Verify network id
        NetworkVO network = _networksDao.findById(networkId);
        if (network == null) {
            throw new InvalidParameterValueException("unable to find network " + networkId);
        }
        
        Account owner = _accountMgr.getAccount(network.getAccountId());

        // Perform permission check
        if (!_accountMgr.isAdmin(caller.getType())) {
            if (network.getAccountId() != caller.getId()) {
                throw new PermissionDeniedException("Account " + caller.getAccountName() + " does not own network id=" + networkId + ", permission denied");
            }
        } else {
            _accountMgr.checkAccess(caller, owner);
        }

        User callerUser = _accountMgr.getActiveUser(UserContext.current().getCallerUserId());
        ReservationContext context = new ReservationContextImpl(null, null, callerUser, owner);
        
        return destroyNetwork(networkId, context);
    }

    @Override
    @DB
    public void shutdownNetwork(long networkId, ReservationContext context) {
        
        Transaction txn = Transaction.currentTxn();
        txn.start();
        NetworkVO network = _networksDao.lockRow(networkId, true);
        if (network == null) {
            s_logger.debug("Unable to find network with id: " + networkId);
            return;
        }
        if (network.getState() != Network.State.Implemented && network.getState() != Network.State.Shutdown) {
            s_logger.debug("Network is not implemented: " + network);
            return;
        }
        
        network.setState(Network.State.Shutdown);
        _networksDao.update(network.getId(), network);
        txn.commit();

        boolean success = true;
        for (NetworkElement element : _networkElements) {
            try {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Sending network shutdown to " + element.getName());
                }
                
                element.shutdown(network, context);
            } catch (ResourceUnavailableException e) {
                s_logger.warn("Unable to complete shutdown of the network due to element: " + element.getName(), e);
                success = false;
            } catch (ConcurrentOperationException e) {
                s_logger.warn("Unable to complete shutdown of the network due to element: " + element.getName(), e);
                success = false;
            } catch (Exception e) {
                s_logger.warn("Unable to complete shutdown of the network due to element: " + element.getName(), e);
                success = false;
            }
        }

        txn.start();
        if (success) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Network id=" + networkId + " is shutdown successfully, cleaning up corresponding resources now.");
            }
            NetworkGuru guru = _networkGurus.get(network.getGuruName());
            NetworkProfile profile = convertNetworkToNetworkProfile(network.getId());
            guru.shutdown(profile, _networkOfferingDao.findById(network.getNetworkOfferingId()));
            
            applyProfileToNetwork(network, profile);
            
            network.setState(Network.State.Allocated);
            _networksDao.update(network.getId(), network);
            _networksDao.clearCheckForGc(networkId);
            
        } else {
            network.setState(Network.State.Implemented);
            _networksDao.update(network.getId(), network);
        }
        txn.commit();
    }

    @Override @DB
    public boolean destroyNetwork(long networkId, ReservationContext context) {
        Account callerAccount = _accountMgr.getAccount(context.getCaller().getAccountId());
        
        NetworkVO network = _networksDao.findById(networkId);
        if (network == null) {
            s_logger.debug("Unable to find network with id: " + networkId);
            return false;
        }
        
        //Don't allow to delete network via api call when it has vms assigned to it
        int nicCount = getActiveNicsInNetwork(networkId);
        if (nicCount > 0) {
            s_logger.debug("Unable to remove the network id=" + networkId + " as it has active Nics.");
            return false;
        }
        
        //Make sure that there are no user vms in the network that are not Expunged/Error
        List<UserVmVO> userVms = _vmDao.listByNetworkId(networkId);
        
        for (UserVmVO vm : userVms) {
            if (!(vm.getState() == VirtualMachine.State.Error || (vm.getState() == VirtualMachine.State.Expunging && vm.getRemoved() != null))) {
                s_logger.warn("Can't delete the network, not all user vms are expunged. Vm " + vm + " is in " + vm.getState() + " state");
                return false;
            }
        }

        // Shutdown network first
        shutdownNetwork(networkId, context);

        // get updated state for the network
        network = _networksDao.findById(networkId);
        if (network.getState() != Network.State.Allocated && network.getState() != Network.State.Setup) {
            s_logger.debug("Network is not not in the correct state to be destroyed: " + network.getState());
            return false;
        }

        boolean success = true;

        cleanupNetworkResources(networkId, callerAccount, context.getCaller().getId());

        for (NetworkElement element : _networkElements) {
            try {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Sending destroy to " + element);
                }
                element.destroy(network);
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

        if (success) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Network id=" + networkId + " is destroyed successfully, cleaning up corresponding resources now.");
            }
            NetworkGuru guru = _networkGurus.get(network.getGuruName());
            Account owner = _accountMgr.getAccount(network.getAccountId());

            Transaction txn = Transaction.currentTxn();
            txn.start();
            guru.trash(network, _networkOfferingDao.findById(network.getNetworkOfferingId()), owner);

            if (!deleteVlansInNetwork(network.getId(), context.getCaller().getId())) {
                success = false;
                s_logger.warn("Failed to delete network " + network + "; was unable to cleanup corresponding ip ranges");
            } else {
                // commit transaction only when ips and vlans for the network are released successfully
                network.setState(Network.State.Destroy);
                _networksDao.update(network.getId(), network);
                _networksDao.remove(network.getId());
                txn.commit();
            }
        }

        return success;
    }
    
    private boolean cleanupNetworkResources(long networkId, Account caller, long callerUserId) {
        boolean success = true;
        Network network = getNetwork(networkId);
        
        //remove all PF/Static Nat rules for the network
        try {
            if (_rulesMgr.revokeAllRulesForNetwork(networkId, callerUserId, caller)) {
                s_logger.debug("Successfully cleaned up portForwarding/staticNat rules for network id=" + networkId);
            } else {
                success = false;
                s_logger.warn("Failed to release portForwarding/StaticNat rules as a part of network id=" + networkId + " cleanup");
            }
        } catch (ResourceUnavailableException ex) {
            success = false;
            //shouldn't even come here as network is being cleaned up after all network elements are shutdown
            s_logger.warn("Failed to release portForwarding/StaticNat rules as a part of network id=" + networkId + " cleanup due to resourceUnavailable ", ex);
        }
        
        //remove all LB rules for the network
        if (_lbMgr.removeAllLoadBalanacersForNetwork(networkId, caller, callerUserId)) {
            s_logger.debug("Successfully cleaned up load balancing rules for network id=" + networkId);
        } else {
            //shouldn't even come here as network is being cleaned up after all network elements are shutdown
            success = false;
            s_logger.warn("Failed to cleanup LB rules as a part of network id=" + networkId + " cleanup");
        }
        
        //release all ip addresses
        List<IPAddressVO> ipsToRelease = _ipAddressDao.listByAssociatedNetwork(networkId, null);
        for (IPAddressVO ipToRelease : ipsToRelease) {
            IPAddressVO ip = markIpAsUnavailable(ipToRelease.getId());
            assert (ip != null) : "Unable to mark the ip address id=" + ipToRelease.getId() + " as unavailable.";
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

    private boolean deleteVlansInNetwork(long networkId, long userId) {
        List<VlanVO> vlans = _vlanDao.listVlansByNetworkId(networkId);
        boolean result = true;
        for (VlanVO vlan : vlans) {
            if (!_configMgr.deleteVlanAndPublicIpRange(_accountMgr.getSystemUser().getId(), vlan.getId())) {
                s_logger.warn("Failed to delete vlan " + vlan.getId() + ");");
                result = false;
            }
        }
        return result;
    }

    @Override
    public boolean applyRules(List<? extends FirewallRule> rules, boolean continueOnError) throws ResourceUnavailableException {
        if (rules == null || rules.size() == 0) {
            s_logger.debug("There are no rules to forward to the network elements");
            return true;
        }

        boolean success = true;
        Network network = _networksDao.findById(rules.get(0).getNetworkId());
        for (NetworkElement ne : _networkElements) {
            try {
                boolean handled = ne.applyRules(network, rules);
                s_logger.debug("Network Rules for network " + network.getId() + " were " + (handled ? "" : " not") + " handled by " + ne.getName());
            } catch (ResourceUnavailableException e) {
                if (!continueOnError) {
                    throw e;
                }
                s_logger.warn("Problems with " + ne.getName() + " but pushing on", e);
                success = false;
            }
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
                    
                    //If network is removed, unset gc flag for it
                    if (getNetwork(networkId) == null) {
                        s_logger.debug("Network id=" + networkId + " is removed, so clearing up corresponding gc check");
                        _networksDao.clearCheckForGc(networkId);
                    } else {
                        try {
                            
                            User caller = _accountMgr.getSystemUser();
                            Account owner = _accountMgr.getAccount(getNetwork(networkId).getAccountId());
                            
                            ReservationContext context = new ReservationContextImpl(null, null, caller, owner);
                            
                            shutdownNetwork(networkId, context);
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
    
  

    @Override @ActionEvent (eventType=EventTypes.EVENT_NETWORK_RESTART, eventDescription="restarting network", async=true)
    public boolean restartNetwork(RestartNetworkCmd cmd) throws ConcurrentOperationException, ResourceUnavailableException, InsufficientCapacityException {
        // This method restarts all network elements belonging to the network and re-applies all the rules
        Long networkId = cmd.getNetworkId();
        
        User caller = _accountMgr.getActiveUser(UserContext.current().getCallerUserId());
        Account callerAccount = _accountMgr.getActiveAccount(caller.getAccountId());
        
        //Check if network exists
        NetworkVO network = _networksDao.findById(networkId);
        if (network == null) {
            throw new InvalidParameterValueException("Network with id=" + networkId + " doesn't exist");
        }
        
        //Don't allow to restart network if it's not in Implemented/Setup state
        if (!(network.getState() == Network.State.Implemented || network.getState() == Network.State.Setup)) {
            throw new InvalidParameterValueException("Network is not in the right state to be restarted. Correct states are: " + Network.State.Implemented + ", " + Network.State.Setup);
        }
        
        _accountMgr.checkAccess(callerAccount, network);
        
        boolean success = true;

        //Restart network - network elements restart is required
        success = restartNetwork(networkId, true, callerAccount);
        
        if (success) {
            s_logger.debug("Network id=" + networkId + " is restarted successfully.");
        } else {
            s_logger.warn("Network id=" + networkId + " failed to restart.");
        }
        
        return success;
    }
    
    @Override
    public boolean startNetwork(long networkId, DeployDestination dest, ReservationContext context) throws ConcurrentOperationException, ResourceUnavailableException, InsufficientCapacityException{
        
        //Check if network exists
        NetworkVO network = _networksDao.findById(networkId);
        if (network == null) {
            throw new InvalidParameterValueException("Network with id=" + networkId + " doesn't exist");
        }
        
        //implement the network
        s_logger.debug("Starting network " + network + "...");
        Pair<NetworkGuru, NetworkVO> implementedNetwork = implementNetwork(networkId, dest, context);
        if (implementedNetwork.first() == null) {
            s_logger.warn("Failed to start the network " + network);
            return false;
        } else {
            return true;   
        }
    }
    
    private boolean restartNetwork(long networkId, boolean restartElements, Account caller) throws ConcurrentOperationException, ResourceUnavailableException, InsufficientCapacityException {
        boolean success = true;
        
        NetworkVO network = _networksDao.findById(networkId);
        
        s_logger.debug("Restarting network " + networkId + "...");
        
        if (restartElements) {
            s_logger.debug("Restarting network elements for the network " + network);
            for (NetworkElement element : _networkElements) {
                //stop and start the network element
                if (!element.restart(network, null)) {
                    s_logger.warn("Failed to restart network element(s) as a part of network id" + networkId + " restart");
                    success = false;
                }   
            }
        }
            
        //associate all ip addresses
        if (!applyIpAssociations(network, false)) {
            s_logger.warn("Failed to apply ip addresses as a part of network id" + networkId + " restart");
            success = false;
        }
        
        //apply port forwarding rules
        if (!_rulesMgr.applyPortForwardingRulesForNetwork(networkId, false, caller)) {
            s_logger.warn("Failed to reapply port forwarding rule(s) as a part of network id=" + networkId + " restart");
            success = false;
        }
        
        //apply static nat rules
        if (!_rulesMgr.applyStaticNatRulesForNetwork(networkId, false, caller)) {
            s_logger.warn("Failed to reapply static nat rule(s) as a part of network id=" + networkId + " restart");
            success = false;
        }
        
        //apply load balancer rules
        if (!_lbMgr.applyLoadBalancersForNetwork(networkId)) {
            s_logger.warn("Failed to reapply load balancer rules as a part of network id=" + networkId + " restart");
            success = false;
        }
        
        //apply vpn rules
        List<? extends RemoteAccessVpn> vpnsToReapply = _vpnMgr.listRemoteAccessVpns(networkId);
        if (vpnsToReapply != null) {
            for (RemoteAccessVpn vpn : vpnsToReapply) {
                //Start remote access vpn per ip
                if (_vpnMgr.startRemoteAccessVpn(vpn.getServerAddressId()) == null) {
                    s_logger.warn("Failed to reapply vpn rules as a part of network id=" + networkId + " restart");
                    success = false;
                }
            }
        }
        
        return success;
    }

    @Override
    public int getActiveNicsInNetwork(long networkId) {
        return _networksDao.getActiveNicsIn(networkId);
    }

    @Override
    public Map<Service, Map<Capability, String>> getZoneCapabilities(long zoneId) {
        DataCenterVO dc = _dcDao.findById(zoneId);
        if (dc == null) {
            throw new InvalidParameterValueException("Zone id=" + zoneId + " doesn't exist in the system.");
        }

        // Get all service providers from the datacenter
        Map<Service, String> providers = new HashMap<Service, String>();
        providers.put(Service.Firewall, dc.getFirewallProvider());
        providers.put(Service.Lb, dc.getLoadBalancerProvider());
        providers.put(Service.Vpn, dc.getVpnProvider());
        providers.put(Service.Dns, dc.getDnsProvider());
        providers.put(Service.Gateway, dc.getGatewayProvider());
        providers.put(Service.UserData, dc.getUserDataProvider());
        providers.put(Service.Dhcp, dc.getDhcpProvider());

        Map<Service, Map<Capability, String>> zoneCapabilities = new HashMap<Service, Map<Capability, String>>();

        for (NetworkElement element : _networkElements) {
            if (providers.isEmpty()) {
                break;
            }
            Map<Service, Map<Capability, String>> elementCapabilities = element.getCapabilities();
            if (elementCapabilities != null) {
                Iterator<Service> it = providers.keySet().iterator();
                while (it.hasNext()) {
                    Service service = it.next();
                    String zoneProvider = providers.get(service);
                    if (zoneProvider != null) {
                        if (zoneProvider.equalsIgnoreCase(element.getProvider().getName())) {
                            if (elementCapabilities.containsKey(service)) {
                                Map<Capability, String> capabilities = elementCapabilities.get(service);
                                // Verify if Service support capability
                                if (capabilities != null) {
                                    for (Capability capability : capabilities.keySet()) {
                                        assert (service.containsCapability(capability)) : "Capability " + capability.getName() + " is not supported by the service " + service.getName();
                                    }
                                }
                                zoneCapabilities.put(service, capabilities);
                                it.remove();
                            }
                        }
                    }
                }
            }
        }
        return zoneCapabilities;
    }
    
    
    @Override
    public Map<Service, Map<Capability, String>> getNetworkCapabilities(long networkId) {
        Network network = getNetwork(networkId);
        if (network == null) {
            throw new InvalidParameterValueException("Unable to find network by id " + networkId);
        }
        
        Map<Service, Map<Capability, String>> zoneCapabilities = getZoneCapabilities(network.getDataCenterId());
        Map<Service, Map<Capability, String>> networkCapabilities = new HashMap<Service, Map<Capability, String>>();
        
        for (Service service : zoneCapabilities.keySet()) {
            if (isServiceSupported(networkId, service)) {
                networkCapabilities.put(service, zoneCapabilities.get(service));
            }
        }
        
        return networkCapabilities;
    }
    
    @Override
    public Map<Capability, String> getServiceCapability(long zoneId, Service service) {
        Map<Service, Map<Capability, String>> networkCapabilities = getZoneCapabilities(zoneId);
        return networkCapabilities.get(service);
    }

    @Override
    public NetworkVO getSystemNetworkByZoneAndTrafficType(long zoneId, TrafficType trafficType) {
        // find system public network offering
        Long networkOfferingId = null;
        List<NetworkOfferingVO> offerings = _networkOfferingDao.listSystemNetworkOfferings();
        for (NetworkOfferingVO offering : offerings) {
            if (offering.getTrafficType() == trafficType) {
                networkOfferingId = offering.getId();
                break;
            }
        }

        if (networkOfferingId == null) {
            throw new InvalidParameterValueException("Unable to find system network offering with traffic type " + trafficType);
        }

        List<NetworkVO> networks = _networksDao.listBy(Account.ACCOUNT_ID_SYSTEM, networkOfferingId, zoneId);
        if (networks == null || networks.isEmpty()) {
            throw new InvalidParameterValueException("Unable to find network with traffic type " + trafficType + " in zone " + zoneId);
        }
        return networks.get(0);
    }
    
    @Override
    public NetworkVO getNetworkWithSecurityGroupEnabled(Long zoneId) {
        List<NetworkVO> networks = _networksDao.listByZoneSecurityGroup(zoneId);
        if (networks == null || networks.isEmpty()) {
            return null;
        }
        
        if (networks.size() > 1) {
            s_logger.debug("There are multiple network with security group enabled? select one of them...");
        }
        return networks.get(0);
    }

    @Override
    public PublicIpAddress getPublicIpAddress(long ipAddressId) {
        IPAddressVO addr = _ipAddressDao.findById(ipAddressId);
        if (addr == null) {
            return null;
        }

        return new PublicIp(addr, _vlanDao.findById(addr.getVlanId()), NetUtils.createSequenceBasedMacAddress(addr.getMacAddress()));
    }

    @Override
    public List<VlanVO> listPodVlans(long podId) {
        List<VlanVO> vlans = _vlanDao.listVlansForPodByType(podId, VlanType.DirectAttached);
        return vlans;
    }

    @Override
    public List<NetworkVO> listNetworksUsedByVm(long vmId, boolean isSystem) {
        List<NetworkVO> networks = new ArrayList<NetworkVO>();

        List<NicVO> nics = _nicDao.listByVmId(vmId);
        if (nics != null) {
            for (Nic nic : nics) {
                NetworkVO network = _networksDao.findByIdIncludingRemoved(nic.getNetworkId());
                NetworkOffering no = _networkOfferingDao.findByIdIncludingRemoved(network.getNetworkOfferingId());
                if (no.isSystemOnly() == isSystem) {
                    networks.add(network);
                }
            }
        }

        return networks;
    }

    @Override
    public Nic getNicInNetwork(long vmId, long networkId) {
        return _nicDao.findByInstanceIdAndNetworkId(networkId, vmId);
    }
    
    
    @Override
    public Nic getNicInNetworkIncludingRemoved(long vmId, long networkId) {
        return _nicDao.findByInstanceIdAndNetworkIdIncludingRemoved(networkId, vmId);
    }

    @Override @DB
    public boolean associateIpAddressListToAccount(long userId, long accountId, long zoneId, Long vlanId, Network network) throws InsufficientCapacityException, ConcurrentOperationException, ResourceUnavailableException {
        Account owner = _accountMgr.getActiveAccount(accountId);
        boolean createNetwork = false;
        
        Transaction txn= Transaction.currentTxn();
        
        txn.start();
        
        if (network == null) {
            List<? extends Network> networks = getVirtualNetworksOwnedByAccountInZone(owner.getAccountName(), owner.getDomainId(), zoneId);
            if (networks.size() == 0) {
                createNetwork = true;
            } else {
                network = networks.get(0);
            }
        }
         
        // create new Virtual network for the user if it doesn't exist
        if (createNetwork) {
            List<? extends NetworkOffering> offerings = _configMgr.listNetworkOfferings(TrafficType.Guest, false);
            network = createNetwork(offerings.get(0).getId(), owner.getAccountName() + "-network", owner.getAccountName() + "-network", false, null, zoneId, null, null, null, null, owner, false, null);

            if (network == null) {
                s_logger.warn("Failed to create default Virtual network for the account " + accountId + "in zone " + zoneId);
                return false;
            } 
        }
        
        //Check if there is a source nat ip address for this account; if not - we have to allocate one
        boolean allocateSourceNat = false;
        List<IPAddressVO> sourceNat = _ipAddressDao.listByAssociatedNetwork(network.getId(), true);
        if (sourceNat.isEmpty()) {
            allocateSourceNat = true;
        }
        
        //update all ips with a network id, mark them as allocated and update resourceCount/usage
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
                addr.setAssociatedWithNetworkId(network.getId());
                addr.setAllocatedTime(new Date());
                addr.setAllocatedInDomainId(owner.getDomainId());
                addr.setAllocatedToAccountId(owner.getId());
                addr.setState(IpAddress.State.Allocating);
                markPublicIpAsAllocated(addr);
            }
        }
        
        txn.commit();
        return true;
    }

    @Override
    public Nic getNicForTraffic(long vmId, TrafficType type) {
        SearchCriteria<NicVO> sc = NicForTrafficTypeSearch.create();
        sc.setParameters("instance", vmId);
        sc.setJoinParameters("network", "traffictype", type);

        List<NicVO> vos = _nicDao.search(sc, null);
        assert vos.size() <= 1 : "If we have multiple networks of the same type, then this method should no longer be used.";
        return vos.size() == 1 ? vos.get(0) : null;
    }

    @Override
    public IpAddress getIp(long ipAddressId) {
        return _ipAddressDao.findById(ipAddressId);
    }

    @Override
    public NetworkProfile convertNetworkToNetworkProfile(long networkId) {
        NetworkVO network = _networksDao.findById(networkId);
        NetworkGuru guru = _networkGurus.get(network.getGuruName());
        NetworkProfile profile = new NetworkProfile(network);
        guru.updateNetworkProfile(profile);

        return profile;
    }

    @Override
    public Network getDefaultNetworkForVm(long vmId) {
        Nic defaultNic = getDefaultNic(vmId);
        if (defaultNic == null) {
            return null;
        } else {
            return _networksDao.findById(defaultNic.getNetworkId());
        }
    }

    @Override
    public Nic getDefaultNic(long vmId) {
        List<NicVO> nics = _nicDao.listByVmId(vmId);
        Nic defaultNic = null;
        if (nics != null) {
            for (Nic nic : nics) {
                if (nic.isDefaultNic()) {
                    defaultNic = nic;
                    break;
                }
            }
        } else {
            s_logger.debug("Unable to find default network for the vm; vm doesn't have any nics");
            return null;
        }

        if (defaultNic == null) {
            s_logger.debug("Unable to find default network for the vm; vm doesn't have default nic");
        }

        return defaultNic;

    }

    @Override
    public List<? extends PasswordResetElement> getPasswordResetElements() {
        List<PasswordResetElement> elements = new ArrayList<PasswordResetElement>();
        for (NetworkElement element : _networkElements) {
            if (element instanceof PasswordResetElement) {
                elements.add((PasswordResetElement) element);
            }
        }
        return elements;
    }

    @Override
    public boolean zoneIsConfiguredForExternalNetworking(long zoneId) {
        DataCenterVO zone = _dcDao.findById(zoneId);

        return (zone.getGatewayProvider() != null && zone.getGatewayProvider().equals(Network.Provider.JuniperSRX.getName()) && zone.getFirewallProvider().equals(Network.Provider.JuniperSRX.getName()) && zone.getLoadBalancerProvider().equals(
                Network.Provider.F5BigIp.getName()));

    }
    
    @Override
    public boolean isServiceSupported(long networkId, Network.Service service) {
        Network network = getNetwork(networkId);
        NetworkOffering offering = _configMgr.getNetworkOffering(network.getNetworkOfferingId());
        if (service == Service.Lb) {
            return offering.isLbService();
        } else if (service == Service.Dhcp) {
            return offering.isDhcpService();
        } else if (service == Service.Dns) {
            return offering.isDnsService();
        } else if (service == Service.Firewall) {
            return offering.isFirewallService();
        } else if (service == Service.UserData) {
            return offering.isUserdataService();
        } else if (service == Service.Vpn) {
            return offering.isVpnService();
        } else if (service == Service.Gateway) {
            return offering.isGatewayService();
        }
        
        return false;
    }
    
    private boolean cleanupIpResources(long ipId, long userId, Account caller) {
        boolean success = true;
        
        try {
            s_logger.debug("Revoking all PF/StaticNat rules as a part of public IP id=" + ipId + " release...");
            if (!_rulesMgr.revokeAllRulesForIp(ipId, userId, caller)) {
                s_logger.warn("Unable to revoke all the port forwarding rules for ip id=" + ipId + " as a part of ip release");
                success = false;
            }
        } catch (ResourceUnavailableException e) {
            s_logger.warn("Unable to revoke all the port forwarding rules for ip id=" + ipId + " as a part of ip release", e);
            success = false;
        }

        s_logger.debug("Revoking all LB rules as a part of public IP id=" + ipId + " release...");
        if (!_lbMgr.removeAllLoadBalanacersForIp(ipId, caller, userId)) {
            s_logger.warn("Unable to revoke all the load balancer rules for ip id=" + ipId + " as a part of ip release");
            success = false;
        }
        
        //remote access vpn can be enabled only for static nat ip, so this part should never be executed under normal conditions
        //only when ip address failed to be cleaned up as a part of account destroy and was marked as Releasing, this part of the code would be triggered
        s_logger.debug("Cleaning up remote access vpns as a part of public IP id=" + ipId + " release...");
        try {
            _vpnMgr.destroyRemoteAccessVpn(ipId);
        } catch (ResourceUnavailableException e) {
            s_logger.warn("Unable to destroy remote access vpn for ip id=" + ipId + " as a part of ip release", e);
            success = false;
        }
        
        return success;
    }
    
    @Override
    public String getIpOfNetworkElementInVirtualNetwork(long accountId, long dataCenterId) {
   
        List<NetworkVO> virtualNetworks = _networksDao.listBy(accountId, dataCenterId , GuestIpType.Virtual);
        
        if (virtualNetworks.isEmpty()) {
            s_logger.trace("Unable to find default Virtual network account id=" + accountId);
            return null;
        }
        
        NetworkVO virtualNetwork = virtualNetworks.get(0);
        
        NicVO networkElementNic = _nicDao.findByNetworkIdAndType(virtualNetwork.getId(), Type.DomainRouter);
        
        if (networkElementNic != null) {
            return networkElementNic.getIp4Address();
        } else {
            s_logger.warn("Unable to set find network element for the network id=" + virtualNetwork.getId());
            return null;
        }
    }
    
    @Override
    public List<NetworkVO> listNetworksForAccount(long accountId, long zoneId, GuestIpType guestType, Boolean isDefault) {
        List<NetworkVO> accountNetworks = new ArrayList<NetworkVO>();
        List<NetworkVO> zoneNetworks = _networksDao.listByZone(zoneId);
        
        for (NetworkVO network : zoneNetworks) {
            NetworkOfferingVO no = _networkOfferingDao.findById(network.getNetworkOfferingId());
            if (!no.isSystemOnly()) {
                if (network.getIsShared() || !_networksDao.listBy(accountId, network.getId()).isEmpty()) {
                    if ((guestType == null || guestType == network.getGuestType()) && (isDefault == null || isDefault == network.isDefault)) {
                        accountNetworks.add(network); 
                    } 
                }
            }
        }
        return accountNetworks;
    }
    
    @DB
    private IPAddressVO markIpAsUnavailable(long addrId) {
        Transaction txn = Transaction.currentTxn();
 
        IPAddressVO ip = _ipAddressDao.findById(addrId);
        
        if (ip.getState() != State.Releasing) {
            txn.start();
            
            ip = _ipAddressDao.markAsUnavailable(addrId);
            
            _accountMgr.decrementResourceCount(_ipAddressDao.findById(addrId).getAccountId(), ResourceType.public_ip);
            
            long isSourceNat = (ip.isSourceNat()) ? 1 : 0;
            
            if (ip.getAccountId() != Account.ACCOUNT_ID_SYSTEM) {
                UsageEventVO usageEvent = new UsageEventVO(EventTypes.EVENT_NET_IP_RELEASE, ip.getAccountId(), ip.getDataCenterId(), addrId, ip.getAddress().addr(), isSourceNat);
                _usageEventDao.persist(usageEvent);
            }
            
            txn.commit();
        }
        
        return ip;
    }
    
    @Override
    public boolean isNetworkAvailableInDomain(long networkId, long domainId) {
        boolean result = false;
        Long networkDomainId = null;
        Network network = getNetwork(networkId);
        if (!network.getIsShared()) {
            s_logger.trace("Network id=" + networkId + " is not shared");
            return false;
        }
        
        
        List<NetworkDomainVO> networkDomainMap = _networkDomainDao.listDomainNetworkMapByNetworkId(networkId);
        if (networkDomainMap.isEmpty()) {
            s_logger.trace("Network id=" + networkId + " is shared, but not domain specific");
            return true;
        } else {
            networkDomainId = networkDomainMap.get(0).getDomainId();
        }
        
        if (domainId == networkDomainId.longValue()) {
            return true;
        }
        
        Domain domain = _domainDao.findById(domainId);
        while (domain.getParent() != null) {
            if (domain.getParent().longValue() == networkDomainId) {
                return true;
            }
            domain = _domainDao.findById(domain.getParent());
        }
        
        return result;
    }
    
    @Override
    public Long getDedicatedNetworkDomain(long networkId) {
        List<NetworkDomainVO> networkMaps = _networkDomainDao.listDomainNetworkMapByNetworkId(networkId);
        if (!networkMaps.isEmpty()) {
            return networkMaps.get(0).getDomainId();
        } else {
            return null;
        }
    }
    
    @Override @ActionEvent (eventType=EventTypes.EVENT_NETWORK_UPDATE, eventDescription="updating network", async=false)
    public Network updateNetwork(long networkId, String name, String displayText, Account caller) {
        
        //verify input parameters
        NetworkVO network = _networksDao.findById(networkId);
        if (network == null) {
            throw new InvalidParameterValueException("Network id=" + networkId + "doesn't exist in the system");
        }
        
        //Don't allow to update system network
        NetworkOffering offering = _networkOfferingDao.findByIdIncludingRemoved(network.getNetworkOfferingId());
        if (offering.isSystemOnly()) {
            throw new InvalidParameterValueException("Can't update system networks");
        }
        
        _accountMgr.checkAccess(caller, network);
        
        if (name != null) {
            network.setName(name);
        }
        
        if (displayText != null) {
            network.setDisplayText(displayText);
        }
        
        _networksDao.update(networkId, network);
        
        return network;
       
    }
}
