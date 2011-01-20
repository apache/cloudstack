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
import com.cloud.api.BaseCmd;
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
import com.cloud.domain.dao.DomainDao;
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
import com.cloud.network.Network.Capability;
import com.cloud.network.Network.GuestIpType;
import com.cloud.network.Network.Service;
import com.cloud.network.Networks.AddressFormat;
import com.cloud.network.Networks.BroadcastDomainType;
import com.cloud.network.Networks.TrafficType;
import com.cloud.network.addr.PublicIp;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.element.NetworkElement;
import com.cloud.network.guru.NetworkGuru;
import com.cloud.network.lb.LoadBalancingRule;
import com.cloud.network.lb.LoadBalancingRulesManager;
import com.cloud.network.rules.FirewallRule;
import com.cloud.network.rules.PortForwardingRule;
import com.cloud.network.rules.RulesManager;
import com.cloud.network.vpn.RemoteAccessVpnElement;
import com.cloud.offering.NetworkOffering;
import com.cloud.offering.NetworkOffering.Availability;
import com.cloud.offerings.NetworkOfferingVO;
import com.cloud.offerings.dao.NetworkOfferingDao;
import com.cloud.resource.Resource;
import com.cloud.resource.Resource.ReservationStrategy;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.AccountVO;
import com.cloud.user.UserContext;
import com.cloud.user.UserStatisticsVO;
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
import com.cloud.vm.DomainRouterVO;
import com.cloud.vm.Nic;
import com.cloud.vm.NicProfile;
import com.cloud.vm.NicVO;
import com.cloud.vm.ReservationContext;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachine.Type;
import com.cloud.vm.VirtualMachineProfile;
import com.cloud.vm.dao.NicDao;
import com.cloud.vm.dao.UserVmDao;

/**
 * NetworkManagerImpl implements NetworkManager.
 */
@Local(value={NetworkManager.class, NetworkService.class})
public class NetworkManagerImpl implements NetworkManager, NetworkService, Manager {
    private static final Logger s_logger = Logger.getLogger(NetworkManagerImpl.class);

    String _name;
    @Inject DataCenterDao _dcDao = null;
    @Inject VlanDao _vlanDao = null;
    @Inject IPAddressDao _ipAddressDao = null;
    @Inject AccountDao _accountDao = null;
    @Inject DomainDao _domainDao = null;
    @Inject UserStatisticsDao _userStatsDao = null;
    @Inject EventDao _eventDao = null;
    @Inject ConfigurationDao _configDao;
    @Inject UserVmDao _vmDao = null;
    @Inject ResourceLimitDao _limitDao = null;
    @Inject CapacityDao _capacityDao = null;
    @Inject AlertManager _alertMgr;
    @Inject AccountManager _accountMgr;
    @Inject ConfigurationManager _configMgr;
    @Inject AccountVlanMapDao _accountVlanMapDao;
    @Inject NetworkOfferingDao _networkOfferingDao = null;
    @Inject NetworkDao _networksDao = null;
    @Inject NicDao _nicDao = null;
    @Inject RulesManager _rulesMgr;
    @Inject LoadBalancingRulesManager _lbMgr;
    @Inject UsageEventDao _usageEventDao;
    @Inject PodVlanMapDao _podVlanMapDao;
    @Inject(adapter=NetworkGuru.class)
    Adapters<NetworkGuru> _networkGurus;
    @Inject(adapter=NetworkElement.class)
    Adapters<NetworkElement> _networkElements;

    private HashMap<String, NetworkOfferingVO> _systemNetworks = new HashMap<String, NetworkOfferingVO>(5);

    ScheduledExecutorService _executor;

    SearchBuilder<AccountVO> AccountsUsingNetworkSearch;
    SearchBuilder<IPAddressVO> AssignIpAddressSearch;
    SearchBuilder<IPAddressVO> AssignIpAddressFromPodVlanSearch;
    SearchBuilder<IPAddressVO> IpAddressSearch;
    
    int _networkGcWait;
    int _networkGcInterval;
    String _networkDomain;

    private Map<String, String> _configs;
    
    HashMap<Long, Long> _lastNetworkIdsToFree = new HashMap<Long, Long>();

    @Override
    public PublicIp assignPublicIpAddress(long dcId, Long podId, Account owner, VlanType type, Long networkId) throws InsufficientAddressCapacityException {
        return fetchNewPublicIp(dcId, podId, owner, type, networkId, false, true); 
    }
    
    @DB 
    public PublicIp fetchNewPublicIp(long dcId, Long podId, Account owner, VlanType vlanUse, Long networkId, boolean sourceNat, boolean assign) throws InsufficientAddressCapacityException {
        Transaction txn = Transaction.currentTxn();
        txn.start();
        SearchCriteria<IPAddressVO> sc = null;
        if (podId != null) {
            sc = AssignIpAddressFromPodVlanSearch.create();
            sc.setJoinParameters("podVlanMapSB", "podId", podId);
        } else {
            sc = AssignIpAddressSearch.create();
        }
        
        sc.setParameters("dc", dcId);
        
        //for direct network take ip addresses only from the vlans belonging to the network
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
        addr.setState(assign ? IpAddress.State.Allocated : IpAddress.State.Allocating);
        
        if (vlanUse == VlanType.DirectAttached) {
            addr.setState(IpAddress.State.Allocated);
        } else {
            addr.setAssociatedWithNetworkId(networkId);
        }
        
        if (!_ipAddressDao.update(addr.getAddress(), addr)) {
            throw new CloudRuntimeException("Found address to allocate but unable to update: " + addr);
        }
        if(!sourceNat  &&  (owner.getAccountId() != Account.ACCOUNT_ID_SYSTEM)){
            UsageEventVO usageEvent = new UsageEventVO(EventTypes.EVENT_NET_IP_ASSIGN, owner.getAccountId(), dcId, 0, addr.getAddress().toString());
            _usageEventDao.persist(usageEvent);
        }
        
        txn.commit();
        long macAddress = NetUtils.createSequenceBasedMacAddress(addr.getMacAddress());

        return new PublicIp(addr, _vlanDao.findById(addr.getVlanId()), macAddress);
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
            List<IPAddressVO> addrs = listPublicIpAddressesInVirtualNetwork(ownerId, dcId, null);            
            if (addrs.size() == 0) {
                // Check that the maximum number of public IPs for the given accountId will not be exceeded
                if (_accountMgr.resourceLimitExceeded(owner, ResourceType.public_ip)) {
                    throw new AccountLimitException("Maximum number of public IP addresses for account: " + owner.getAccountName() + " has been exceeded.");
                }
                
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("assigning a new ip address in " + dcId + " to " + owner);
                }                
                
                ip = fetchNewPublicIp(dcId, null, owner, VlanType.VirtualNetwork, network.getId(), true, false);
                sourceNat = ip.ip();
                sourceNat.setState(IpAddress.State.Allocated);
                _ipAddressDao.update(sourceNat.getAddress(), sourceNat);

                // Increment the number of public IPs for this accountId in the database
                _accountMgr.incrementResourceCount(ownerId, ResourceType.public_ip);
            } else {
                // Account already has ip addresses
                
                for (IPAddressVO addr : addrs) {
                    if (addr.isSourceNat()) {
                        sourceNat = addr;
                        break;
                    }
                }
                
                assert(sourceNat != null) : "How do we get a bunch of ip addresses but none of them are source nat? account=" + ownerId + "; dc=" + dcId;
                ip = new PublicIp(sourceNat, _vlanDao.findById(sourceNat.getVlanId()), NetUtils.createSequenceBasedMacAddress(sourceNat.getMacAddress()));
            }
            
            UserStatisticsVO stats = _userStatsDao.findBy(ownerId, dcId, null, null);
            if (stats == null) {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Creating statistics for the owner: " + ownerId);
                }
                stats = new UserStatisticsVO(ownerId, dcId, null, null);
                _userStatsDao.persist(stats);
            }
            txn.commit();
            return ip;
        } finally {
            if (owner != null) {
                if(s_logger.isDebugEnabled()) {
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
    public boolean associateIP(final DomainRouterVO router, final List<String> ipAddrList, final boolean add, long vmId) {
//        Commands cmds = new Commands(OnError.Continue);
//        boolean sourceNat = false;
//        Map<VlanVO, ArrayList<IPAddressVO>> vlanIpMap = new HashMap<VlanVO, ArrayList<IPAddressVO>>();
//        for (final String ipAddress: ipAddrList) {
//            IPAddressVO ip = _ipAddressDao.findById(new Ip(ipAddress));
//
//            VlanVO vlan = _vlanDao.findById(ip.getVlanId());
//            ArrayList<IPAddressVO> ipList = vlanIpMap.get(vlan.getId());
//            if (ipList == null) {
//                ipList = new ArrayList<IPAddressVO>();
//            }
//            ipList.add(ip);
//            vlanIpMap.put(vlan, ipList);
//        }
//        for (Map.Entry<VlanVO, ArrayList<IPAddressVO>> vlanAndIp: vlanIpMap.entrySet()) {
//            boolean firstIP = true;
//            ArrayList<IPAddressVO> ipList = vlanAndIp.getValue();
//            Collections.sort(ipList, new Comparator<IPAddressVO>() {
//                @Override
//                public int compare(IPAddressVO o1, IPAddressVO o2) {
//                    return o1.getAddress().compareTo(o2.getAddress());
//                } });
//
//            for (final IPAddressVO ip: ipList) {
//                sourceNat = ip.isSourceNat();
//                VlanVO vlan = vlanAndIp.getKey();
//                String vlanId = vlan.getVlanTag();
//                String vlanGateway = vlan.getVlanGateway();
//                String vlanNetmask = vlan.getVlanNetmask();
//
//                String vifMacAddress = null;
//                if (firstIP && add) {
//                    String[] macAddresses = _dcDao.getNextAvailableMacAddressPair(ip.getDataCenterId());
//                    vifMacAddress = macAddresses[1];
//                }
//                String vmGuestAddress = null;
//                if(vmId!=0){
//                    vmGuestAddress = _vmDao.findById(vmId).getGuestIpAddress();
//                }
//
//                //cmds.addCommand(new IPAssocCommand(router.getInstanceName(), router.getPrivateIpAddress(), ip.getAddress(), add, firstIP, sourceNat, vlanId, vlanGateway, vlanNetmask, vifMacAddress, vmGuestAddress));
//
//                firstIP = false;
//            }
//        }
//
//        Answer[] answers = null;
//        try {
//            answers = _agentMgr.send(router.getHostId(), cmds);
//        } catch (final AgentUnavailableException e) {
//            s_logger.warn("Agent unavailable", e);
//            return false;
//        } catch (final OperationTimedoutException e) {
//            s_logger.warn("Timed Out", e);
//            return false;
//        }
//
//        if (answers == null) {
//            return false;
//        }
//
//        if (answers.length != ipAddrList.size()) {
//            return false;
//        }
//
//        // FIXME:  this used to be a loop for all answers, but then we always returned the
//        //         first one in the array, so what should really be done here?
//        if (answers.length > 0) {
//            Answer ans = answers[0];
//            return ans.getResult();
//        }

        return true;
    }

    /** Returns the target account for an api command
     * @param accountName - non-null if the account name was passed in in the command
     * @param domainId - non-null if the domainId was passed in in the command.
     * @return
     */
    protected Account getAccountForApiCommand(String accountName, Long domainId) throws InvalidParameterValueException, PermissionDeniedException{
        Account account = UserContext.current().getCaller();

        if (_accountMgr.isAdmin(account.getType())) {
            //The admin is making the call, determine if it is for someone else or for himself
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
    
    public boolean applyIpAssociations(Network network, boolean continueOnError) throws ResourceUnavailableException {
        List<IPAddressVO> userIps = _ipAddressDao.listByNetwork(network.getId());
        List<PublicIp> publicIps = new ArrayList<PublicIp>();
        if (userIps != null && !userIps.isEmpty()) {
            for (IPAddressVO userIp : userIps) {
                PublicIp publicIp = new PublicIp(userIp, _vlanDao.findById(userIp.getVlanId()), userIp.getMacAddress());
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
                    addr.setState(IpAddress.State.Allocated);
                    addr.setAssociatedWithNetworkId(network.getId());
                    _ipAddressDao.update(addr.getAddress(), addr);
                } else if (addr.getState() == IpAddress.State.Releasing) {
                    _ipAddressDao.unassignIpAddress(addr.getAddress());
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

    @Override @DB
    public IpAddress associateIP(AssociateIPAddrCmd cmd) throws ResourceAllocationException, ResourceUnavailableException, InsufficientAddressCapacityException, ConcurrentOperationException  {
        String accountName = cmd.getAccountName();
        long domainId = cmd.getDomainId();
        Long zoneId = cmd.getZoneId();
        Account caller = UserContext.current().getCaller();
        long userId = UserContext.current().getCallerUserId();

        Account owner = _accountMgr.getActiveAccount(accountName, domainId);
        if (owner == null) {
            throw new InvalidParameterValueException("Unable to find account " + accountName + " in domain " + domainId + ", permission denied");
        }
        
        _accountMgr.checkAccess(caller, owner);
        
        long ownerId = owner.getId();
        Long networkId = cmd.getNetworkId();
        Network network = null;
        if (networkId != null) {
            network = _networksDao.findById(networkId);
            if (network == null) {
                throw new InvalidParameterValueException("Network id is invalid: " + networkId);
            }
        }

        PublicIp ip = null;
        boolean success = false;
        
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

            txn.start();
            ip = fetchNewPublicIp(zoneId, null, owner, VlanType.VirtualNetwork, network.getId(), false, false);
           
            if (ip == null) {
                throw new InsufficientAddressCapacityException("Unable to find available public IP addresses", DataCenter.class, zoneId);
            } 
            
            _accountMgr.incrementResourceCount(ownerId, ResourceType.public_ip);

            Ip ipAddress = ip.getAddress();
            
            s_logger.debug("Got " + ipAddress + " to assign for account " + owner.getId() + " in zone " + network.getDataCenterId());

            txn.commit();

            success = applyIpAssociations(network, false);
            if (success) {
                s_logger.debug("Successfully associated ip address " + ip + " for account " + owner.getId() + " in zone " + network.getDataCenterId());
            } else {
                s_logger.warn("Failed to associate ip address " + ip + " for account " + owner.getId() + " in zone " + network.getDataCenterId());
            }
            
            return ip;
        } catch (ResourceUnavailableException e) {
            s_logger.error("Unable to associate ip address due to resource unavailable exception", e);
            return null;
        } finally {
            if (accountToLock != null) {
                _accountDao.releaseFromLockTable(ownerId);
                s_logger.debug("Associate IP address lock released");
            }
            
            if (!success) {
                if (ip != null) {
                    try {
                        s_logger.warn("Failed to associate ip address " + ip);
                        _ipAddressDao.markAsUnavailable(ip.getAddress(), ip.getAccountId());
                        applyIpAssociations(network, true);
                    } catch (Exception e) {
                        s_logger.warn("Unable to disassociate ip address for recovery", e);
                    }
                    txn.start();
                    _ipAddressDao.unassignIpAddress(ip.getAddress());
                    _accountMgr.decrementResourceCount(ownerId, ResourceType.public_ip);

                    txn.commit();
                }
            }
        }
    }

    @Override
    public boolean releasePublicIpAddress(Ip addr, long ownerId, long userId) {
        IPAddressVO ip = _ipAddressDao.markAsUnavailable(addr, ownerId);
        assert (ip != null) : "Unable to mark the ip address " + addr + " owned by " + ownerId + " as unavailable.";
        if (ip == null) {
            return true;
        }
        
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Releasing ip " + addr + "; sourceNat = " + ip.isSourceNat());
        }
        
        boolean success = true;
        try {
            if (!_rulesMgr.revokeAllRules(addr, userId)) {
                s_logger.warn("Unable to revoke all the port forwarding rules for ip " + ip);
                success = false;
            }
        } catch (ResourceUnavailableException e) {
            s_logger.warn("Unable to revoke all the port forwarding rules for ip " + ip, e);
            success = false;
        }
        
        if (!_lbMgr.removeAllLoadBalanacers(addr)) {
            s_logger.warn("Unable to revoke all the load balancer rules for ip " + ip);
            success = false;
        }
        
        if (ip.getAssociatedWithNetworkId() != null) {
            Network network = _networksDao.findById(ip.getAssociatedWithNetworkId());
            try {
                if (!applyIpAssociations(network, true)) {
                    s_logger.warn("Unable to apply ip address associations for " + network);
                    success = false;
                }
            } catch (ResourceUnavailableException e) {
                throw new CloudRuntimeException("We should nver get to here because we used true when applyIpAssociations", e);
            }
        }
        
        if (success) {
            _ipAddressDao.unassignIpAddress(addr);
            s_logger.debug("released a public ip: " + addr);    
            if(!ip.isSourceNat() && (ownerId != Account.ACCOUNT_ID_SYSTEM)){       
                UsageEventVO usageEvent = new UsageEventVO(EventTypes.EVENT_NET_IP_RELEASE, ownerId, ip.getDataCenterId(), 0, addr.toString());
                _usageEventDao.persist(usageEvent);
            }
        }
        
        return success;
    }

    private Integer getIntegerConfigValue(String configKey, Integer dflt) {
        String value = _configs.get(configKey);
        if (value != null) {
            return Integer.parseInt(value);
        }
        return dflt;
    }

    @Override
    public boolean configure(final String name, final Map<String, Object> params) throws ConfigurationException {
        _name = name;

        _configs = _configDao.getConfiguration("AgentManager", params);
        Integer rateMbps = getIntegerConfigValue(Config.NetworkThrottlingRate.key(), null);  
        Integer multicastRateMbps = getIntegerConfigValue(Config.MulticastThrottlingRate.key(), null);
        _networkGcWait = NumbersUtil.parseInt(_configs.get(Config.NetworkGcWait.key()), 600);
        _networkGcInterval = NumbersUtil.parseInt(_configs.get(Config.NetworkGcInterval.key()), 600);
        
        _configs = _configDao.getConfiguration("Network", params);
        _networkDomain = _configs.get(Config.GuestDomainSuffix.key());

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
        NetworkOfferingVO guestNetworkOffering = new NetworkOfferingVO(NetworkOfferingVO.SysteGuestNetwork, TrafficType.Guest);
        guestNetworkOffering = _networkOfferingDao.persistDefaultNetworkOffering(guestNetworkOffering);
        _systemNetworks.put(NetworkOfferingVO.SysteGuestNetwork, guestNetworkOffering);
        
        NetworkOfferingVO defaultGuestNetworkOffering = new NetworkOfferingVO(NetworkOffering.DefaultVirtualizedNetworkOffering, "Virtual Vlan", TrafficType.Guest, false, false, rateMbps, multicastRateMbps, null, true, Availability.Required, false, false, false, false, false, false, false);
        defaultGuestNetworkOffering = _networkOfferingDao.persistDefaultNetworkOffering(defaultGuestNetworkOffering);
        NetworkOfferingVO defaultGuestDirectNetworkOffering = new NetworkOfferingVO(NetworkOffering.DefaultDirectNetworkOffering, "Direct", TrafficType.Public, false, false, rateMbps, multicastRateMbps, null, true, Availability.Required, false, false, false, false, false, false, false);
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
        SearchBuilder<VlanVO> virtualNetworkVlanSB = _vlanDao.createSearchBuilder();
        virtualNetworkVlanSB.and("vlanType", virtualNetworkVlanSB.entity().getVlanType(), Op.EQ);
        IpAddressSearch.join("virtualNetworkVlanSB", virtualNetworkVlanSB, IpAddressSearch.entity().getVlanId(), virtualNetworkVlanSB.entity().getId(), JoinBuilder.JoinType.INNER);
        IpAddressSearch.done();
        
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
    public List<IPAddressVO> listPublicIpAddressesInVirtualNetwork(long accountId, long dcId, Boolean sourceNat) {
        SearchCriteria<IPAddressVO> sc = IpAddressSearch.create();
        sc.setParameters("accountId", accountId);
        sc.setParameters("dataCenterId", dcId);
        if (sourceNat != null) {
            sc.addAnd("sourceNat", SearchCriteria.Op.EQ, sourceNat);
        }
        sc.setJoinParameters("virtualNetworkVlanSB", "vlanType", VlanType.VirtualNetwork);

        return _ipAddressDao.search(sc, null);
    }

    @Override
    public List<NetworkVO> setupNetwork(Account owner, NetworkOfferingVO offering, DeploymentPlan plan, String name, String displayText, boolean isShared, boolean isDefault) throws ConcurrentOperationException {
        return setupNetwork(owner, offering, null, plan, name, displayText, isShared, isDefault);
    }

    @Override @DB
    public List<NetworkVO> setupNetwork(Account owner, NetworkOfferingVO offering, Network predefined, DeploymentPlan plan, String name, String displayText, boolean isShared, boolean isDefault) throws ConcurrentOperationException {
        Transaction.currentTxn();
        Account locked = _accountDao.acquireInLockTable(owner.getId());
        if (locked == null) {
            throw new ConcurrentOperationException("Unable to acquire lock on " + owner);
        }
        try {
            
            if (predefined == null || (predefined.getBroadcastUri() == null && predefined.getBroadcastDomainType() != BroadcastDomainType.Vlan)) {
                List<NetworkVO> configs = _networksDao.listBy(owner.getId(), offering.getId(), plan.getDataCenterId());
                if (configs.size() > 0) {
                    if (s_logger.isDebugEnabled()) {
                        s_logger.debug("Found existing network configuration for offering " + offering + ": " + configs.get(0));
                    }
                    return configs;
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
                        configs.add((NetworkVO)config);
                    } else {
                        configs.add(_networksDao.findById(config.getId()));
                    }
                    continue;
                }
    
                long id = _networksDao.getNextInSequence(Long.class, "id");
                if (related == -1) {
                    related = id;
                } 
    
                NetworkVO vo = new NetworkVO(id, config, offering.getId(), plan.getDataCenterId(), guru.getName(), owner.getDomainId(), owner.getId(), related, name, displayText, isShared, isDefault);
                configs.add(_networksDao.persist(vo, vo.getGuestType() != null));
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

    @Override @DB
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
            NetworkGuru concierge = _networkGurus.get(config.getGuruName());
            NicProfile requested = network.second();
            if (requested != null && requested.getMode() == null) {
                requested.setMode(config.getMode());
            }
            NicProfile profile = concierge.allocate(config, requested, vm);
            
            if (vm != null && vm.getVirtualMachine().getType() == Type.User && config.isDefault()) {
                profile.setDefaultNic(true);
            }
            
            if (profile == null) {
                continue;
            }
            NicVO vo = new NicVO(concierge.getName(), vm.getId(), config.getId());

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
            vm.addNic(new NicProfile(vo, network.first(), vo.getBroadcastUri(), vo.getIsolationUri()));
        }

        if (nics.size() == 1) {
            nics.get(0).setDefaultNic(true);
        }

        txn.commit();
    }

    protected Integer applyProfileToNic(NicVO vo, NicProfile profile, Integer deviceId) {
        if (profile.getDeviceId() != null) {
            vo.setDeviceId(profile.getDeviceId());
        } else if (deviceId != null ) {
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

    protected NicTO toNicTO(NicVO nic, NicProfile profile, NetworkVO config) {
        NicTO to = new NicTO();
        to.setDeviceId(nic.getDeviceId());
        to.setBroadcastType(config.getBroadcastDomainType());
        to.setType(config.getTrafficType());
        to.setIp(nic.getIp4Address());
        to.setNetmask(nic.getNetmask());
        to.setMac(nic.getMacAddress());
        if (config.getDns1() != null) {
            to.setDns1(config.getDns1());
        }
        if (config.getDns2() != null) {
            to.setDns2(config.getDns2());
        }
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
                implemented.set(guru, network);
                return implemented;
            }

            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Asking " + guru + " to implement " + network);
            }

            NetworkOfferingVO offering = _networkOfferingDao.findById(network.getNetworkOfferingId());
            network.setReservationId(context.getReservationId());
            network.setState(Network.State.Implementing);
            
            _networksDao.update(networkId, network);

            Network result = guru.implement(network, offering, dest, context);
            network.setCidr(result.getCidr());
            network.setBroadcastUri(result.getBroadcastUri());
            network.setGateway(result.getGateway());
            network.setDns1(result.getDns1());
            network.setDns2(result.getDns2());
            network.setMode(result.getMode());
            _networksDao.update(networkId, network);

            for (NetworkElement element : _networkElements) {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Asking " + element.getName() + " to implmenet " + network);
                }
                element.implement(network, offering, dest, context);
            }
            
            network.setState(Network.State.Implemented);
            _networksDao.update(network.getId(), network);
            implemented.set(guru, network);
            return implemented;
        } finally {
            if (implemented.first() == null) {
                s_logger.debug("Cleaning up because we're unable to implement network " + network);
                network.setState(Network.State.Shutdown);
                _networksDao.update(networkId, network);
                shutdownNetwork(networkId);
            }
            _networksDao.releaseFromLockTable(networkId);
        }
    }
    
    @DB
    protected void updateNic(NicVO nic, long networkId, int count) {
        Transaction txn = Transaction.currentTxn();
        txn.start();
        _nicDao.update(nic.getId(), nic);
        _networksDao.changeActiveNicsBy(networkId, count);
        txn.commit();
    }

    @Override
    public void prepare(VirtualMachineProfile<? extends VMInstanceVO> vmProfile, DeployDestination dest, ReservationContext context) throws InsufficientCapacityException, ConcurrentOperationException, ResourceUnavailableException {
        List<NicVO> nics = _nicDao.listBy(vmProfile.getId());
        for (NicVO nic : nics) {
            Pair<NetworkGuru, NetworkVO> implemented = implementNetwork(nic.getNetworkId(), dest, context);
            NetworkGuru concierge = implemented.first();
            NetworkVO network = implemented.second();
            NicProfile profile = null;
            if (nic.getReservationStrategy() == ReservationStrategy.Start) {
                nic.setState(Resource.State.Reserving);
                nic.setReservationId(context.getReservationId());
                _nicDao.update(nic.getId(), nic);
                URI broadcastUri = nic.getBroadcastUri();
                if (broadcastUri == null) {
                    broadcastUri = network.getBroadcastUri();
                }

                URI isolationUri = nic.getIsolationUri();

                profile = new NicProfile(nic, network, broadcastUri, isolationUri);
                concierge.reserve(profile, network, vmProfile, dest, context);
                nic.setIp4Address(profile.getIp4Address());
                nic.setIp6Address(profile.getIp6Address());
                nic.setMacAddress(profile.getMacAddress());
                nic.setIsolationUri(profile.getIsolationUri());
                nic.setBroadcastUri(profile.getBroadCastUri());
                nic.setReserver(concierge.getName());
                nic.setState(Resource.State.Reserved);
                nic.setNetmask(profile.getNetmask());
                nic.setGateway(profile.getGateway());
                nic.setAddressFormat(profile.getFormat());
                updateNic(nic, network.getId(), 1);
            } else {
                profile = new NicProfile(nic, network, nic.getBroadcastUri(), nic.getIsolationUri());
                nic.setState(Nic.State.Reserved);
                updateNic(nic, network.getId(), 1);
            }
            
            for (NetworkElement element : _networkElements) {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Asking " + element.getName() + " to prepare for " + nic);
                }
                element.prepare(network, profile, vmProfile, dest, context);
            }

            vmProfile.addNic(profile);
        }
    }
    
    @Override
    public <T extends VMInstanceVO> void prepareNicForMigration(VirtualMachineProfile<T> vm, DeployDestination dest) {
        List<NicVO> nics = _nicDao.listBy(vm.getId());
        for (NicVO nic : nics) {
            Network network = _networksDao.findById(nic.getNetworkId());
            
            NicProfile profile = new NicProfile(nic, network, nic.getBroadcastUri(), nic.getIsolationUri());
            
            vm.addNic(profile);
        }
    }
    

    @Override
    public void release(VirtualMachineProfile<? extends VMInstanceVO> vmProfile, boolean forced) {
        List<NicVO> nics = _nicDao.listBy(vmProfile.getId());
        for (NicVO nic : nics) {
            NetworkVO network = _networksDao.findById(nic.getNetworkId());
            if (nic.getState() == Nic.State.Reserved || nic.getState() == Nic.State.Reserving) {
                Nic.State originalState = nic.getState();
                if (nic.getReservationStrategy() == ReservationStrategy.Start) {
                    NetworkGuru concierge = _networkGurus.get(network.getGuruName());
                    nic.setState(Resource.State.Releasing);
                    _nicDao.update(nic.getId(), nic);
                    NicProfile profile = new NicProfile(nic, network, null, null);
                    if (concierge.release(profile, vmProfile, nic.getReservationId())) {
                        nic.setState(Resource.State.Allocated);
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
    public List<? extends Nic> getNics(VirtualMachine vm) {
        return _nicDao.listBy(vm.getId());
    }

    private Account findAccountByIpAddress(Ip ipAddress) {
        IPAddressVO address = _ipAddressDao.findById(ipAddress);
        if ((address != null) && (address.getAllocatedToAccountId() != null)) {
            return _accountMgr.getActiveAccount(address.getAllocatedToAccountId());
        }
        return null;
    }

    @Override
    @DB
    public boolean disassociateIpAddress(DisassociateIPAddrCmd cmd) throws PermissionDeniedException, IllegalArgumentException {

        Long userId = UserContext.current().getCallerUserId();
        Account caller = UserContext.current().getCaller();
        Ip ipAddress = cmd.getIpAddress();

        // Verify input parameters
        Account accountByIp = findAccountByIpAddress(ipAddress);
        if(accountByIp == null) { 
            throw new InvalidParameterValueException("Unable to find account owner for ip " + ipAddress);
        }

        Long accountId = accountByIp.getId();
        if (!_accountMgr.isAdmin(caller.getType())) {
            if (caller.getId() != accountId.longValue()) {
                throw new PermissionDeniedException("account " + caller.getAccountName() + " doesn't own ip address " + ipAddress);
            }
        } else {
            Domain domain = _domainDao.findById(accountByIp.getDomainId());
            _accountMgr.checkAccess(caller, domain);
        }

        try {
            IPAddressVO ipVO = _ipAddressDao.findById(ipAddress);
            if (ipVO == null) {
                return false;
            }

            if (ipVO.getAllocatedTime() == null) {
                return true;
            }

            Account account = _accountMgr.getAccount(accountId);
            if (account == null) {
                return false;
            }

            if ((ipVO.getAllocatedToAccountId() == null) || (ipVO.getAllocatedToAccountId().longValue() != accountId)) {
                // FIXME: is the user visible in the admin account's domain????
                if (!BaseCmd.isAdmin(account.getType())) {
                    if (s_logger.isDebugEnabled()) {
                        s_logger.debug("permission denied disassociating IP address " + ipAddress + "; acct: " + accountId + "; ip (acct / dc / dom / alloc): "
                                + ipVO.getAllocatedToAccountId() + " / " + ipVO.getDataCenterId() + " / " + ipVO.getAllocatedInDomainId() + " / " + ipVO.getAllocatedTime());
                    }
                    throw new PermissionDeniedException("User/account does not own supplied address");
                }
            }

            if (ipVO.getAllocatedTime() == null) {
                return true;
            }

            if (ipVO.isSourceNat()) {
                throw new IllegalArgumentException("ip address is used for source nat purposes and can not be disassociated.");
            }

            VlanVO vlan = _vlanDao.findById(ipVO.getVlanId());
            if (!vlan.getVlanType().equals(VlanType.VirtualNetwork)) {
                throw new IllegalArgumentException("only ip addresses that belong to a virtual network may be disassociated.");
            }

            //Check for account wide pool. It will have an entry for account_vlan_map. 
            if (_accountVlanMapDao.findAccountVlanMap(accountId,ipVO.getVlanId()) != null){
                throw new PermissionDeniedException(ipAddress + " belongs to Account wide IP pool and cannot be disassociated");
            }

            boolean success = releasePublicIpAddress(ipAddress, accountId, userId);
            if (success) {
                _accountMgr.decrementResourceCount(accountId, ResourceType.public_ip);
            }
            return success;

        } catch (PermissionDeniedException pde) {
            throw pde;
        } catch (IllegalArgumentException iae) {
            throw iae;
        } catch (Throwable t) {
            s_logger.error("Disassociate IP address threw an exception.", t);
            throw new IllegalArgumentException("Disassociate IP address threw an exception");
        }
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

    @Override @DB
    public Network getNetwork(long id) {
        return _networksDao.findById(id);
    }
    
    @Override
    public List<? extends RemoteAccessVpnElement> getRemoteAccessVpnElements() {
        List<RemoteAccessVpnElement> elements = new ArrayList<RemoteAccessVpnElement>();
        for (NetworkElement element : _networkElements) {
            if (element instanceof RemoteAccessVpnElement) {
                elements.add((RemoteAccessVpnElement)element);
            }
        }
        
        return elements;
    }
    
    @Override
    public void cleanupNics(VirtualMachineProfile<? extends VMInstanceVO> vm) {
        List<NicVO> nics = _nicDao.listBy(vm.getId());
        for (NicVO nic : nics) {
            nic.setState(Nic.State.Deallocating);
            _nicDao.update(nic.getId(), nic);
            NetworkVO network = _networksDao.findById(nic.getNetworkId());
            NicProfile profile = new NicProfile(nic, network, null, null);
            NetworkGuru guru = _networkGurus.get(network.getGuruName());
            guru.deallocate(network, profile, vm);
            _nicDao.remove(nic.getId());
        }
    }

    @Override @DB
    public Network createNetwork(CreateNetworkCmd cmd) throws InvalidParameterValueException, PermissionDeniedException{
        Account ctxAccount = UserContext.current().getCaller();
        Long userId = UserContext.current().getCallerUserId();
        Long networkOfferingId = cmd.getNetworkOfferingId();
        Long zoneId = cmd.getZoneId();
        String gateway = cmd.getGateway();
        String startIP = cmd.getStartIp();
        String endIP = cmd.getEndIp();
        String netmask = cmd.getNetmask();
        String networkDomain = cmd.getNetworkDomain();
        String cidr = null;
        Boolean isDefault = cmd.isDefault();
        if (gateway != null && netmask != null) {
            cidr = NetUtils.ipAndNetMaskToCidr(gateway, netmask);
        }
        String accountName = cmd.getAccountName();
        Long domainId = cmd.getDomainId();
        String vlanId = cmd.getVlan();
        String name = cmd.getNetworkName();
        String displayText = cmd.getDisplayText();
        Boolean isShared = cmd.getIsShared();
        
        //if end ip is not specified, default it to startIp 
        if (endIP == null && startIP != null) {
            endIP = startIP;
        }
        
        //Check if network offering exists
        NetworkOfferingVO networkOffering = _networkOfferingDao.findById(networkOfferingId);
        if (networkOffering == null || networkOffering.isSystemOnly()) {
            throw new InvalidParameterValueException("Unable to find network offeirng by id " + networkOfferingId);
        }
        
        //allow isDefault to be set only for Virtual network
        if (networkOffering.getTrafficType() == TrafficType.Guest) {
            if (isDefault != null) {
                throw new InvalidParameterValueException("Can specify isDefault parameter only for Public network. ");
            } else {
                isDefault = true;
            }
        } else {
            if (isDefault == null) {
                isDefault = false;
            }
        }
        
        //If networkDomain is not specified, take it from the global configuration
        if (networkDomain == null) {
            networkDomain = _networkDomain;
        }
        
        //Check if zone exists
        if (zoneId == null || ((_dcDao.findById(zoneId)) == null)) {
            throw new InvalidParameterValueException("Please specify a valid zone.");
        } 
        
        DataCenter zone = _dcDao.findById(zoneId);
        if (zone.getNetworkType() == NetworkType.Basic) {
            throw new InvalidParameterValueException("Network creation is not allowed in zone with network type " + NetworkType.Basic);
        }
        
        Account owner = _accountMgr.finalizeOwner(ctxAccount, accountName, domainId);
        
        //Don't allow to create network with vlan that already exists in the system
        if (vlanId != null) {
            String uri ="vlan://" + vlanId;
            List<NetworkVO> networks = _networksDao.listBy(zoneId, uri);
            if ((networks != null && !networks.isEmpty())) {
                throw new InvalidParameterValueException("Network with vlan " + vlanId + " already exists in zone " + zoneId);
            }
        }
      
       //VlanId can be specified only when network offering supports it
        if (ctxAccount.getType() == Account.ACCOUNT_TYPE_NORMAL && vlanId != null && !networkOffering.getSpecifyVlan()) {
            throw new InvalidParameterValueException("Can't specify vlan because network offering doesn't support it");
        }
       
       Transaction txn = Transaction.currentTxn();
       txn.start();
       try {
           //Create network
           DataCenterDeployment plan = new DataCenterDeployment(zoneId, null, null, null);
           NetworkVO userNetwork = new NetworkVO();
           userNetwork.setNetworkDomain(networkDomain);
           
           //cidr should be set only when the user is admin
           if (ctxAccount.getType() == Account.ACCOUNT_TYPE_ADMIN) {
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
           }   
           
           List<NetworkVO> networks = setupNetwork(owner, networkOffering, userNetwork, plan, name, displayText, isShared, isDefault);
           Long networkId = null;
           
           Network network = null;
           if (networks == null || networks.isEmpty()) {
               txn.rollback();
               throw new CloudRuntimeException("Fail to create a network");
           } else {
               network  = networks.get(0);
               networkId = networks.get(0).getId();
               if (network.getGuestType() == GuestIpType.Virtual) {
                   s_logger.debug("Creating a source natp ip for " + network);
                   PublicIp ip = assignSourceNatIpAddress(owner, network, userId);
                   if (ip == null) {
                       throw new InsufficientAddressCapacityException("Unable to assign source nat ip address to owner for this network", DataCenter.class, zoneId);
                   }
               }
           }
           
           Long ownerId = owner.getId();
           //Don't pass owner to create vlan when network offering is of type Direct - done to prevent accountVlanMap entry creation when vlan is mapped to network
           if (network.getGuestType() == GuestIpType.Direct) {
               owner = null;
           }
           
           if (ctxAccount.getType() == Account.ACCOUNT_TYPE_ADMIN && network.getGuestType() == GuestIpType.Direct && startIP != null && endIP != null && gateway != null) {
               //Create vlan ip range
               Vlan vlan = _configMgr.createVlanAndPublicIpRange(userId, zoneId, null, startIP, endIP, gateway, netmask, false, vlanId, owner, networkId);
               if (vlan == null) {
                   txn.rollback();
                   throw new CloudRuntimeException("Failed to create a vlan");
               }
           }  
           txn.commit(); 
           
           return networks.get(0);
       } catch (Exception ex) {
           s_logger.warn("Unexpected exception while creating network ", ex);
           txn.rollback();
       } finally {
           txn.close();
       }
       return null;
    }
    
    @Override
    public List<? extends Network> searchForNetworks(ListNetworksCmd cmd) { 
        Object id = cmd.getId(); 
        Object keyword = cmd.getKeyword();
        Long zoneId= cmd.getZoneId();
        Account account = UserContext.current().getCaller();
        Long domainId = cmd.getDomainId();
        String accountName = cmd.getAccountName();
        String type = cmd.getType();
        String trafficType = cmd.getTrafficType();
        Boolean isSystem = cmd.getIsSystem();
        Boolean isShared = cmd.getIsShared();
        Boolean isDefault = cmd.isDefault();
        Long accountId = null;
        
        if (isSystem == null) {
            isSystem = false;
        }
        
        //Account/domainId parameters and isSystem are mutually exclusive
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
        } else {
            accountName = account.getAccountName();
            domainId = account.getDomainId();
            accountId = account.getId();
        }
        
        Filter searchFilter = new Filter(NetworkVO.class, "id", false, cmd.getStartIndex(), cmd.getPageSizeVal());
        SearchBuilder<NetworkVO> sb = _networksDao.createSearchBuilder();
        
        //Don't display networks created of system network offerings
        SearchBuilder<NetworkOfferingVO> networkOfferingSearch  = _networkOfferingDao.createSearchBuilder();
        networkOfferingSearch.and("systemOnly", networkOfferingSearch.entity().isSystemOnly(), SearchCriteria.Op.EQ);
        if (isSystem) {
            networkOfferingSearch.and("trafficType", networkOfferingSearch.entity().getTrafficType(), SearchCriteria.Op.EQ);
        } 
        sb.join("networkOfferingSearch", networkOfferingSearch, sb.entity().getNetworkOfferingId(), networkOfferingSearch.entity().getId(), JoinBuilder.JoinType.INNER); 
        
        SearchBuilder<DataCenterVO> zoneSearch  = _dcDao.createSearchBuilder();
        zoneSearch.and("networkType", zoneSearch.entity().getNetworkType(), SearchCriteria.Op.EQ);
        sb.join("zoneSearch", zoneSearch, sb.entity().getDataCenterId(), zoneSearch.entity().getId(), JoinBuilder.JoinType.INNER);
      
        SearchCriteria<NetworkVO> sc = sb.create();
        
        if (!isSystem) {
            sc.setJoinParameters("networkOfferingSearch", "systemOnly", false);
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
        
        if (!isSystem && (isShared == null || !isShared) && accountName != null && domainId != null) {
        	sc.addAnd("accountId", SearchCriteria.Op.EQ, accountId);
        	sc.addAnd("isShared", SearchCriteria.Op.EQ, false);
        }
        
        if (isShared != null) {
            sc.addAnd("isShared", SearchCriteria.Op.EQ, isShared);
        }
        
        if (isDefault != null) {
            sc.addAnd("isDefault", SearchCriteria.Op.EQ, isDefault);
        }
        
        if (trafficType != null) {
            sc.addAnd("trafficType", SearchCriteria.Op.EQ, trafficType);
        }
        
        List<NetworkVO> networks =  _networksDao.search(sc, searchFilter);
        
        return networks;
    }
    
    @Override @DB
    public boolean deleteNetwork(long networkId) throws InvalidParameterValueException, PermissionDeniedException{        
        Long userId = UserContext.current().getCallerUserId();
        Account caller = UserContext.current().getCaller();

        //Verify network id
        NetworkVO network = _networksDao.findById(networkId);
        if (network == null) {
            throw new InvalidParameterValueException("unable to find network " + networkId);
        } 
        
        //Perform permission check
        if (!_accountMgr.isAdmin(caller.getType())) {
            if (network.getAccountId() != caller.getId()) {
                throw new PermissionDeniedException("Account " + caller.getAccountName() + " does not own network id=" + networkId + ", permission denied");
            }
        } else {
            Account owner = _accountMgr.getAccount(network.getAccountId());
            _accountMgr.checkAccess(caller, owner);
        }
        
       return this.destroyNetwork(networkId, userId);
    }
    
    @Override @DB 
    public void shutdownNetwork(long networkId) {
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
                    s_logger.debug("Sending network shutdown to " + element);
                }
                element.shutdown(network, null);
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
            guru.destroy(network, _networkOfferingDao.findById(network.getNetworkOfferingId()));
            network.setBroadcastUri(null);
            network.setState(Network.State.Allocated);
            _networksDao.update(network.getId(), network);
            _networksDao.clearCheckForGc(networkId);
        } else {
            network.setState(Network.State.Implemented);
            _networksDao.update(network.getId(), network);
        }
        txn.commit();
    }
    
    
    @DB @Override
    public boolean destroyNetwork(long networkId, long callerUserId) {
        NetworkVO network = _networksDao.findById(networkId);
        if (network == null) {
            s_logger.debug("Unable to find network with id: " + networkId);
            return false;
        }
        
        //Shutdown network first
        shutdownNetwork(networkId);
        
        //get updated state for the network
        network = _networksDao.findById(networkId);
        if (network.getState() != Network.State.Allocated && network.getState() != Network.State.Setup) {
            s_logger.debug("Network is not not in the correct state to be destroyed: " + network.getState());
            return false;
        }

        boolean success = true;
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
            if (!deleteVlansInNetwork(network.getId(), callerUserId)) {
                success = false;
                s_logger.warn("Failed to delete network " + network + "; was unable to cleanup corresponding ip ranges");
            } else {
                //commit transaction only when ips and vlans for the network are released successfully
                network.setState(Network.State.Destroy);
                _networksDao.update(network.getId(), network);
                _networksDao.remove(network.getId());
                txn.commit();
            }
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
                    try {
                        shutdownNetwork(networkId);
                    } catch (Exception e) {
                        s_logger.warn("Unable to shutdown network: " + networkId);
                    }
                }
            } catch (Exception e) {
                s_logger.warn("Caught exception while running network gc: ", e);
            }
        }
    }
    
    @Override
    public boolean restartNetwork(RestartNetworkCmd cmd) throws ConcurrentOperationException, ResourceUnavailableException {
        //This method reapplies Ip addresses, LoadBalancer and PortForwarding rules
        Account caller = UserContext.current().getCaller();        
        Long networkId = cmd.getNetworkId();
        Network network = null;
        if (networkId != null) {
            network = _networksDao.findById(networkId);
            if (network == null) {
                throw new InvalidParameterValueException("Network id is invalid: " + networkId);
            }
        }
        
        Account owner = _accountMgr.getActiveAccount(cmd.getEntityOwnerId());
        if (!_accountMgr.isAdmin(caller.getType())) {
            _accountMgr.checkAccess(caller, network);
        } else {
            Domain domain = _domainDao.findById(owner.getDomainId());
            _accountMgr.checkAccess(caller, domain);
        }
        
        s_logger.debug("Restarting network " + networkId + "...");
        
        if (!applyIpAssociations(network, false)) {
            s_logger.warn("Failed to apply ips as a part of network " + networkId + " restart");
            return false;
        } else {
            s_logger.debug("Ip addresses are reapplied successfully as a part of network " + networkId + " restart");
        }
        
        List<LoadBalancingRule> lbRules = _lbMgr.listByNetworkId(networkId);
         
        if (!applyRules(lbRules, true)) {
            s_logger.warn("Failed to apply load balancing rules as a part of network " + network.getId() + " restart");
            return false;
        } else {
            s_logger.debug("Load balancing rules are reapplied successfully as a part of network " + networkId + " restart");
        }
        
        //Reapply pf rules
        List<? extends PortForwardingRule> pfRules = _rulesMgr.listByNetworkId(networkId);
        if (!applyRules(pfRules, true)) {
            s_logger.warn("Failed to apply port forwarding rules as a part of network " + network.getId() + " restart");
            return false;
        } else {
            s_logger.debug("Port forwarding rules are reapplied successfully as a part of network " + networkId + " restart");
        }
        
        s_logger.debug("Network " + networkId + " is restarted successfully.");
        return true;
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
        
        //Get all service providers from the datacenter
        Map<Service,String> providers = new HashMap<Service,String>();
        providers.put(Service.Firewall, dc.getFirewallProvider());
        providers.put(Service.Lb, dc.getLoadBalancerProvider());
        providers.put(Service.Vpn, dc.getVpnProvider());
        providers.put(Service.Dns, dc.getDnsProvider());
        providers.put(Service.Gateway, dc.getGatewayProvider());
        providers.put(Service.UserData, dc.getUserDataProvider());
        providers.put(Service.Dhcp, dc.getDhcpProvider());
        
        Map<Service, Map<Capability, String>> networkCapabilities = new HashMap<Service, Map<Capability, String>>();
        
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
                                //Verify if Service support capability
                                if (capabilities != null) {
                                    for (Capability capability : capabilities.keySet()) {          
                                        assert(service.containsCapability(capability)) : "Capability " + capability.getName() + " is not supported by the service " + service.getName();
                                    }
                                }
                                networkCapabilities.put(service, capabilities);
                                it.remove();
                            }
                        }
                    } 
                }
            }
        }
        return networkCapabilities;
    }
    
    @Override
    public Network getSystemNetworkByZoneAndTrafficType(long zoneId, TrafficType trafficType) {
        //find system public network offering
        Long networkOfferingId = null;
        List<NetworkOfferingVO> offerings = _networkOfferingDao.listSystemNetworkOfferings();
        for (NetworkOfferingVO offering: offerings) {
            if (offering.getTrafficType() == trafficType) {
                networkOfferingId = offering.getId();
                break;
            }
        }
        
        if (networkOfferingId == null) {
            throw new InvalidParameterValueException("Unable to find system network offering with traffic type " + trafficType);
        }
        
        List<NetworkVO> networks = _networksDao.listBy(Account.ACCOUNT_ID_SYSTEM, networkOfferingId, zoneId);
        if (networks == null) {
            throw new InvalidParameterValueException("Unable to find network with traffic type " + trafficType + " in zone " + zoneId);
        }
        return networks.get(0);
    }
    
    @Override
    public PublicIpAddress getPublicIpAddress(Ip ip) {
        IPAddressVO addr = _ipAddressDao.findById(ip);
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
        
        List<NicVO> nics = _nicDao.listBy(vmId);
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

}
