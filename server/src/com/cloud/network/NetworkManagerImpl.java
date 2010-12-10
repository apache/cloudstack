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
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ScheduledExecutorService;

import javax.ejb.Local;
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;

import com.cloud.agent.AgentManager;
import com.cloud.agent.AgentManager.OnError;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.routing.IPAssocCommand;
import com.cloud.agent.api.to.NicTO;
import com.cloud.agent.manager.Commands;
import com.cloud.alert.AlertManager;
import com.cloud.api.BaseCmd;
import com.cloud.api.ServerApiException;
import com.cloud.api.commands.AddVpnUserCmd;
import com.cloud.api.commands.AssociateIPAddrCmd;
import com.cloud.api.commands.CreateNetworkCmd;
import com.cloud.api.commands.CreateRemoteAccessVpnCmd;
import com.cloud.api.commands.DeleteNetworkCmd;
import com.cloud.api.commands.DeleteRemoteAccessVpnCmd;
import com.cloud.api.commands.DisassociateIPAddrCmd;
import com.cloud.api.commands.ListNetworksCmd;
import com.cloud.api.commands.RemoveVpnUserCmd;
import com.cloud.capacity.dao.CapacityDao;
import com.cloud.configuration.Config;
import com.cloud.configuration.ConfigurationManager;
import com.cloud.configuration.ResourceCount.ResourceType;
import com.cloud.configuration.dao.ConfigurationDao;
import com.cloud.configuration.dao.ResourceLimitDao;
import com.cloud.dc.DataCenter;
import com.cloud.dc.DataCenter.NetworkType;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.Vlan;
import com.cloud.dc.Vlan.VlanType;
import com.cloud.dc.VlanVO;
import com.cloud.dc.dao.AccountVlanMapDao;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.VlanDao;
import com.cloud.deploy.DataCenterDeployment;
import com.cloud.deploy.DeployDestination;
import com.cloud.deploy.DeploymentPlan;
import com.cloud.domain.dao.DomainDao;
import com.cloud.event.EventTypes;
import com.cloud.event.EventUtils;
import com.cloud.event.EventVO;
import com.cloud.event.dao.EventDao;
import com.cloud.exception.AccountLimitException;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InsufficientNetworkCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.Networks.AddressFormat;
import com.cloud.network.Networks.BroadcastDomainType;
import com.cloud.network.Networks.TrafficType;
import com.cloud.network.addr.PublicIp;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.RemoteAccessVpnDao;
import com.cloud.network.dao.VpnUserDao;
import com.cloud.network.element.NetworkElement;
import com.cloud.network.guru.NetworkGuru;
import com.cloud.network.lb.LoadBalancingRulesManager;
import com.cloud.network.router.DomainRouterManager;
import com.cloud.network.rules.FirewallRule;
import com.cloud.network.rules.RulesManager;
import com.cloud.offering.NetworkOffering;
import com.cloud.offering.NetworkOffering.GuestIpType;
import com.cloud.offerings.NetworkOfferingVO;
import com.cloud.offerings.dao.NetworkOfferingDao;
import com.cloud.resource.Resource;
import com.cloud.resource.Resource.ReservationStrategy;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.AccountVO;
import com.cloud.user.UserContext;
import com.cloud.user.UserStatisticsVO;
import com.cloud.user.dao.AccountDao;
import com.cloud.user.dao.UserStatisticsDao;
import com.cloud.uservm.UserVm;
import com.cloud.utils.Pair;
import com.cloud.utils.component.Adapters;
import com.cloud.utils.component.Inject;
import com.cloud.utils.component.Manager;
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
import com.cloud.vm.State;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
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
    @Inject AgentManager _agentMgr;
    @Inject AlertManager _alertMgr;
    @Inject AccountManager _accountMgr;
    @Inject ConfigurationManager _configMgr;
    @Inject ServiceOfferingDao _serviceOfferingDao = null;
    @Inject AccountVlanMapDao _accountVlanMapDao;
    @Inject UserStatisticsDao _statsDao = null;
    @Inject NetworkOfferingDao _networkOfferingDao = null;
    @Inject NetworkDao _networksDao = null;
    @Inject NicDao _nicDao = null;
    @Inject RemoteAccessVpnDao _remoteAccessVpnDao = null;
    @Inject VpnUserDao _vpnUsersDao = null;
    @Inject DomainRouterManager _routerMgr;
    @Inject RulesManager _rulesMgr;
    @Inject LoadBalancingRulesManager _lbMgr;

    @Inject(adapter=NetworkGuru.class)
    Adapters<NetworkGuru> _networkGurus;
    @Inject(adapter=NetworkElement.class)
    Adapters<NetworkElement> _networkElements;

    private HashMap<String, NetworkOfferingVO> _systemNetworks = new HashMap<String, NetworkOfferingVO>(5);
    Random _rand = new Random(System.currentTimeMillis());

    ScheduledExecutorService _executor;

    SearchBuilder<AccountVO> AccountsUsingNetworkSearch;
    SearchBuilder<IPAddressVO> AssignIpAddressSearch;
    SearchBuilder<IPAddressVO> IpAddressSearch;

    private Map<String, String> _configs;
    
    HashMap<Long, Long> _lastNetworkIdsToFree = new HashMap<Long, Long>();

    @Override
    public PublicIp assignPublicIpAddress(long dcId, Account owner, VlanType type, Long networkId) throws InsufficientAddressCapacityException {
        return fetchNewPublicIp(dcId, owner, type, networkId, false, true); 
    }
    
    @DB 
    public PublicIp fetchNewPublicIp(long dcId, Account owner, VlanType vlanUse, Long networkId, boolean sourceNat, boolean assign) throws InsufficientAddressCapacityException {
        Transaction txn = Transaction.currentTxn();
        txn.start();
        SearchCriteria<IPAddressVO> sc = AssignIpAddressSearch.create();
        sc.setParameters("dc", dcId);
        
        //for direct network take ip addresses only from the vlans belonging to the network
        if (vlanUse == VlanType.DirectAttached) {
            sc.setJoinParameters("vlan", "networkId", networkId);
        }
        sc.setJoinParameters("vlan", "type", vlanUse);
         
        Filter filter = new Filter(IPAddressVO.class, "vlanId", true, 0l, 1l);
        
        List<IPAddressVO> addrs = _ipAddressDao.lockRows(sc, filter, true);
        assert (addrs.size() == 1) : "Return size is incorrect: " + addrs.size();
        
        if (addrs.size() == 0) {
            throw new InsufficientAddressCapacityException("Insufficient address capacity", DataCenter.class, dcId);
        }
        
        IPAddressVO addr = addrs.get(0);
        addr.setSourceNat(sourceNat);
        addr.setAllocatedTime(new Date());
        addr.setAllocatedInDomainId(owner.getDomainId());
        addr.setAllocatedToAccountId(owner.getId());
        addr.setState(assign ? IpAddress.State.Allocated : IpAddress.State.Allocating);
        
        if (vlanUse == VlanType.DirectAttached) {
            addr.setState(IpAddress.State.Allocated);
        } else {
            addr.setAssociatedNetworkId(networkId);
        }
        
        if (!_ipAddressDao.update(addr.getAddress(), addr)) {
            throw new CloudRuntimeException("Found address to allocate but unable to update: " + addr);
        }
        
        txn.commit();
        long macAddress = addr.getMacAddress() | 0x060000000000l | (((long)_rand.nextInt(32768) << 25) & 0x00fffe000000l);

        return new PublicIp(addr, _vlanDao.findById(addr.getVlanId()), macAddress);
    }
    
    @Override
    public PublicIp assignSourceNatIpAddress(Account owner, Network network, long callerId) throws ConcurrentOperationException, InsufficientAddressCapacityException {
        assert ((network.getTrafficType() == TrafficType.Public) || (network.getTrafficType() == TrafficType.Guest && network.getGuestType() == GuestIpType.Direct)) : "You're asking for a source nat but your network can't participate in source nat.  What do you have to say for yourself?";
        
        long dcId = network.getDataCenterId();
        long ownerId = owner.getId();

        final EventVO event = new EventVO();
        event.setUserId(callerId); // system user performed the action...
        event.setAccountId(ownerId);
        event.setType(EventTypes.EVENT_NET_IP_ASSIGN);

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
                
                ip = fetchNewPublicIp(dcId, owner, VlanType.VirtualNetwork, network.getId(), true, false);
                sourceNat = ip.ip();
                sourceNat.setState(IpAddress.State.Allocated);
                _ipAddressDao.update(sourceNat.getAddress(), sourceNat);

                // Increment the number of public IPs for this accountId in the database
                _accountMgr.incrementResourceCount(ownerId, ResourceType.public_ip);
                event.setParameters("address=" + ip.getAddress() + "\nsourceNat=true\ndcId="+dcId);
                event.setDescription("Acquired a public ip: " + ip.getAddress());
                _eventDao.persist(event);
            } else {
                // Account already has ip addresses
                
                for (IPAddressVO addr : addrs) {
                    if (addr.isSourceNat()) {
                        sourceNat = addr;
                        break;
                    }
                }
                
                assert(sourceNat != null) : "How do we get a bunch of ip addresses but none of them are source nat? account=" + ownerId + "; dc=" + dcId;
                ip = new PublicIp(sourceNat, _vlanDao.findById(sourceNat.getVlanId()), sourceNat.getMacAddress() | 0x060000000000l | (((long)_rand.nextInt(32768) << 25) & 0x00fffe000000l));
            }
            
            UserStatisticsVO stats = _userStatsDao.findBy(ownerId, dcId);
            if (stats == null) {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Creating statistics for the owner: " + ownerId);
                }
                stats = new UserStatisticsVO(ownerId, dcId);
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
                event.setLevel(EventVO.LEVEL_ERROR);
                event.setParameters("dcId=" + dcId);
                event.setDescription("Failed to acquire a public ip.");
                _eventDao.persist(event);
                s_logger.error("Unable to get source nat ip address for account " + ownerId);
            }
        }
    }
    
    @Override
    public Commands getAssociateIPCommands(final DomainRouterVO router, final List<String> ipAddrList, final boolean add, long vmId, Commands cmds) {   	 
         boolean sourceNat = false;
         Map<VlanVO, ArrayList<IPAddressVO>> vlanIpMap = new HashMap<VlanVO, ArrayList<IPAddressVO>>();
         for (final String ipAddress: ipAddrList) {
             IPAddressVO ip = _ipAddressDao.findById(ipAddress);

             VlanVO vlan = _vlanDao.findById(ip.getVlanId());
             ArrayList<IPAddressVO> ipList = vlanIpMap.get(vlan.getId());
             if (ipList == null) {
                 ipList = new ArrayList<IPAddressVO>();
             }
             ipList.add(ip);
             vlanIpMap.put(vlan, ipList);
         }
         for (Map.Entry<VlanVO, ArrayList<IPAddressVO>> vlanAndIp: vlanIpMap.entrySet()) {
             boolean firstIP = true;
             ArrayList<IPAddressVO> ipList = vlanAndIp.getValue();
             Collections.sort(ipList, new Comparator<IPAddressVO>() {
                 @Override
                 public int compare(IPAddressVO o1, IPAddressVO o2) {
                     return o1.getAddress().compareTo(o2.getAddress());
                 } });

             for (final IPAddressVO ip: ipList) {
                 sourceNat = ip.isSourceNat();
                 VlanVO vlan = vlanAndIp.getKey();
                 String vlanId = vlan.getVlanId();
                 String vlanGateway = vlan.getVlanGateway();
                 String vlanNetmask = vlan.getVlanNetmask();

                 String vifMacAddress = null;
                 if (firstIP && add) {
                     String[] macAddresses = _dcDao.getNextAvailableMacAddressPair(ip.getDataCenterId());
                     vifMacAddress = macAddresses[1];
                 }
                 String vmGuestAddress = null;
                 if(vmId!=0){
                     vmGuestAddress = _vmDao.findById(vmId).getGuestIpAddress();
                 }

                 cmds.addCommand(new IPAssocCommand(router.getInstanceName(), router.getPrivateIpAddress(), ip.getAddress(), add, firstIP, sourceNat, vlanId, vlanGateway, vlanNetmask, vifMacAddress, vmGuestAddress));

                 firstIP = false;
             }
         }
         return cmds;
    }
    
    @Override
    public boolean associateIP(final DomainRouterVO router, final List<String> ipAddrList, final boolean add, long vmId) {
        Commands cmds = new Commands(OnError.Continue);
        boolean sourceNat = false;
        Map<VlanVO, ArrayList<IPAddressVO>> vlanIpMap = new HashMap<VlanVO, ArrayList<IPAddressVO>>();
        for (final String ipAddress: ipAddrList) {
            IPAddressVO ip = _ipAddressDao.findById(ipAddress);

            VlanVO vlan = _vlanDao.findById(ip.getVlanId());
            ArrayList<IPAddressVO> ipList = vlanIpMap.get(vlan.getId());
            if (ipList == null) {
                ipList = new ArrayList<IPAddressVO>();
            }
            ipList.add(ip);
            vlanIpMap.put(vlan, ipList);
        }
        for (Map.Entry<VlanVO, ArrayList<IPAddressVO>> vlanAndIp: vlanIpMap.entrySet()) {
            boolean firstIP = true;
            ArrayList<IPAddressVO> ipList = vlanAndIp.getValue();
            Collections.sort(ipList, new Comparator<IPAddressVO>() {
                @Override
                public int compare(IPAddressVO o1, IPAddressVO o2) {
                    return o1.getAddress().compareTo(o2.getAddress());
                } });

            for (final IPAddressVO ip: ipList) {
                sourceNat = ip.isSourceNat();
                VlanVO vlan = vlanAndIp.getKey();
                String vlanId = vlan.getVlanId();
                String vlanGateway = vlan.getVlanGateway();
                String vlanNetmask = vlan.getVlanNetmask();

                String vifMacAddress = null;
                if (firstIP && add) {
                    String[] macAddresses = _dcDao.getNextAvailableMacAddressPair(ip.getDataCenterId());
                    vifMacAddress = macAddresses[1];
                }
                String vmGuestAddress = null;
                if(vmId!=0){
                    vmGuestAddress = _vmDao.findById(vmId).getGuestIpAddress();
                }

                cmds.addCommand(new IPAssocCommand(router.getInstanceName(), router.getPrivateIpAddress(), ip.getAddress(), add, firstIP, sourceNat, vlanId, vlanGateway, vlanNetmask, vifMacAddress, vmGuestAddress));

                firstIP = false;
            }
        }

        Answer[] answers = null;
        try {
            answers = _agentMgr.send(router.getHostId(), cmds);
        } catch (final AgentUnavailableException e) {
            s_logger.warn("Agent unavailable", e);
            return false;
        } catch (final OperationTimedoutException e) {
            s_logger.warn("Timed Out", e);
            return false;
        }

        if (answers == null) {
            return false;
        }

        if (answers.length != ipAddrList.size()) {
            return false;
        }

        // FIXME:  this used to be a loop for all answers, but then we always returned the
        //         first one in the array, so what should really be done here?
        if (answers.length > 0) {
            Answer ans = answers[0];
            return ans.getResult();
        }

        return true;
    }

    /** Returns the target account for an api command
     * @param accountName - non-null if the account name was passed in in the command
     * @param domainId - non-null if the domainId was passed in in the command.
     * @return
     */
    protected Account getAccountForApiCommand(String accountName, Long domainId) throws InvalidParameterValueException, PermissionDeniedException{
        Account account = UserContext.current().getAccount();

        if ((account == null) || isAdmin(account.getType())) {
            //The admin is making the call, determine if it is for someone else or for himself
            if (domainId != null) {
                if ((account != null) && !_domainDao.isChildDomain(account.getDomainId(), domainId)) {
                    throw new PermissionDeniedException("Invalid domain id (" + domainId + ") given, , permission denied");
                }
                if (accountName != null) {
                    Account userAccount = _accountDao.findActiveAccount(accountName, domainId);
                    if (userAccount != null) {
                        account = userAccount;
                    } else {
                        throw new PermissionDeniedException("Unable to find account " + accountName + " in domain " + domainId + ", permission denied");
                    }
                }
            } else if (account != null) {
                // the admin is calling the api on his own behalf
                return account;
            } else {
                throw new InvalidParameterValueException("Account information is not specified.");
            }
        } 
        return account;
    }
    
    public boolean applyIpAssociations(Network network, boolean continueOnError) throws ResourceUnavailableException {
        List<IPAddressVO> userIps = _ipAddressDao.listByNetwork(network.getId());
        
        boolean success = true;
        for (NetworkElement element : _networkElements) {
            try {
                element.associate(network, userIps);
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
                    addr.setAssociatedNetworkId(network.getId());
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
        Account owner = _accountDao.findActiveAccount(accountName, domainId);
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
        Account caller = UserContext.current().getAccount();
        long userId = UserContext.current().getUserId();

        Account owner = _accountDao.findActiveAccount(accountName, domainId);
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

        EventVO event = new EventVO();
        event.setUserId(userId);
        event.setAccountId(ownerId);
        event.setType(EventTypes.EVENT_NET_IP_ASSIGN);

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
            ip = fetchNewPublicIp(zoneId, owner, VlanType.VirtualNetwork, network.getId(), false, false);
           
            if (ip == null) {
                throw new InsufficientAddressCapacityException("Unable to find available public IP addresses", DataCenter.class, zoneId);
            } 
            
            _accountMgr.incrementResourceCount(ownerId, ResourceType.public_ip);

            String ipAddress = ip.getAddress();
            event.setParameters("address=" + ipAddress + "\nsourceNat=" + false + "\ndcId=" + zoneId);
            event.setDescription("Assigned a public IP address: " + ipAddress);
            _eventDao.persist(event);

            txn.commit();

            success = applyIpAssociations(network, false);
            
            return ip;
        } catch (ResourceUnavailableException e) {
            s_logger.error("Unable to associate ip address due to resource unavailable exception", e);
            return null;
        } finally {
            if (caller != null) {
                _accountDao.releaseFromLockTable(ownerId);
                s_logger.debug("Associate IP address lock released");
            }
            
            if (!success) {
                if (ip != null) {
                    try {
                        _ipAddressDao.markAsUnavailable(ip.getAddress(), ip.getAccountId());
                        applyIpAssociations(network, true);
                    } catch (Exception e) {
                        s_logger.warn("Unable to disassociate ip address for recovery", e);
                    }
                    txn.start();
                    _ipAddressDao.unassignIpAddress(ip.getAddress());
                    _accountMgr.decrementResourceCount(ownerId, ResourceType.public_ip);

                    event.setLevel(EventVO.LEVEL_ERROR);
                    event.setDescription("");
                    _eventDao.persist(event);
                    txn.commit();
                }
            }
        }
    }

    @Override
    public boolean releasePublicIpAddress(String ipAddress, long ownerId, long userId) {
        IPAddressVO ip = _ipAddressDao.markAsUnavailable(ipAddress, ownerId);
        assert (ip != null) : "Unable to mark the ip address " + ipAddress + " owned by " + ownerId + " as unavailable.";
        if (ip == null) {
            return true;
        }
        
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Releasing ip " + ipAddress + "; sourceNat = " + ip.isSourceNat());
        }
        
        Ip addr = new Ip(ip.getAddress());

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
        
        if (ip.getAssociatedNetworkId() != null) {
            Network network = _networksDao.findById(ip.getAssociatedNetworkId());
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
            _ipAddressDao.unassignIpAddress(ipAddress);
            s_logger.debug("released a public ip: " + ipAddress);
        }
        
        final EventVO event = new EventVO();
        event.setUserId(userId);
        event.setAccountId(ip.getAllocatedToAccountId());
        event.setType(EventTypes.EVENT_NET_IP_RELEASE);
        event.setParameters("address=" + ipAddress + "\nsourceNat="+ip.isSourceNat());
        event.setDescription("released a public ip: " + ipAddress);
        _eventDao.persist(event);
        
        return success;
    }

    private Integer getIntegerConfigValue(String configKey, Integer dflt) {
        String value = _configs.get(configKey);
        if (value != null) {
            return Integer.parseInt(value);
        }
        return dflt;
    }

    private void validateRemoteAccessVpnConfiguration() throws ConfigurationException {
        String ipRange = _configs.get(Config.RemoteAccessVpnClientIpRange.key());
        if (ipRange == null) {
            s_logger.warn("Remote Access VPN configuration missing client ip range -- ignoring");
            return;
        }
        Integer pskLength = getIntegerConfigValue(Config.RemoteAccessVpnPskLength.key(), 24);
        if (pskLength != null && (pskLength < 8 || pskLength > 256)) {
            throw new ConfigurationException("Remote Access VPN: IPSec preshared key length should be between 8 and 256");
        } else if (pskLength == null) {
            s_logger.warn("Remote Access VPN configuration missing Preshared Key Length -- ignoring");
            return;
        }

        String [] range = ipRange.split("-");
        if (range.length != 2) {
            throw new ConfigurationException("Remote Access VPN: Invalid ip range " + ipRange);
        }
        if (!NetUtils.isValidIp(range[0]) || !NetUtils.isValidIp(range[1])){
            throw new ConfigurationException("Remote Access VPN: Invalid ip in range specification " + ipRange);
        }
        if (!NetUtils.validIpRange(range[0], range[1])){
            throw new ConfigurationException("Remote Access VPN: Invalid ip range " + ipRange);
        }
        String [] guestIpRange = getGuestIpRange();
        if (NetUtils.ipRangesOverlap(range[0], range[1], guestIpRange[0], guestIpRange[1])) {
            throw new ConfigurationException("Remote Access VPN: Invalid ip range: " + ipRange + " overlaps with guest ip range " + guestIpRange[0] + "-" + guestIpRange[1]);
        }
    }

    @Override
    public boolean configure(final String name, final Map<String, Object> params) throws ConfigurationException {
        _name = name;

        _configs = _configDao.getConfiguration("AgentManager", params);
        validateRemoteAccessVpnConfiguration();
        Integer rateMbps = getIntegerConfigValue(Config.NetworkThrottlingRate.key(), null);  
        Integer multicastRateMbps = getIntegerConfigValue(Config.MulticastThrottlingRate.key(), null);

        NetworkOfferingVO publicNetworkOffering = new NetworkOfferingVO(NetworkOfferingVO.SystemVmPublicNetwork, TrafficType.Public, null);
        publicNetworkOffering = _networkOfferingDao.persistDefaultNetworkOffering(publicNetworkOffering);
        _systemNetworks.put(NetworkOfferingVO.SystemVmPublicNetwork, publicNetworkOffering);
        NetworkOfferingVO managementNetworkOffering = new NetworkOfferingVO(NetworkOfferingVO.SystemVmManagementNetwork, TrafficType.Management, null);
        managementNetworkOffering = _networkOfferingDao.persistDefaultNetworkOffering(managementNetworkOffering);
        _systemNetworks.put(NetworkOfferingVO.SystemVmManagementNetwork, managementNetworkOffering);
        NetworkOfferingVO controlNetworkOffering = new NetworkOfferingVO(NetworkOfferingVO.SystemVmControlNetwork, TrafficType.Control, null);
        controlNetworkOffering = _networkOfferingDao.persistDefaultNetworkOffering(controlNetworkOffering);
        _systemNetworks.put(NetworkOfferingVO.SystemVmControlNetwork, controlNetworkOffering);
        //        NetworkOfferingVO guestNetworkOffering = new NetworkOfferingVO(NetworkOfferingVO.SystemVmGuestNetwork, TrafficType.Guest, GuestIpType.Virtualized);
        //        guestNetworkOffering = _networkOfferingDao.persistSystemNetworkOffering(guestNetworkOffering);
        //        _systemNetworks.put(NetworkOfferingVO.SystemVmGuestNetwork, guestNetworkOffering);
        NetworkOfferingVO storageNetworkOffering = new NetworkOfferingVO(NetworkOfferingVO.SystemVmStorageNetwork, TrafficType.Storage, null);
        storageNetworkOffering = _networkOfferingDao.persistDefaultNetworkOffering(storageNetworkOffering);
        _systemNetworks.put(NetworkOfferingVO.SystemVmStorageNetwork, storageNetworkOffering);

        NetworkOfferingVO defaultGuestNetworkOffering = new NetworkOfferingVO(NetworkOffering.DefaultVirtualizedNetworkOffering, "Virtual Vlan", TrafficType.Guest, GuestIpType.Virtual, false, false, rateMbps, multicastRateMbps, null, true);
        defaultGuestNetworkOffering = _networkOfferingDao.persistDefaultNetworkOffering(defaultGuestNetworkOffering);
        NetworkOfferingVO defaultGuestDirectNetworkOffering = new NetworkOfferingVO(NetworkOffering.DefaultDirectNetworkOffering, "Direct", TrafficType.Public, GuestIpType.Direct, false, false, rateMbps, multicastRateMbps, null, true);
        defaultGuestNetworkOffering = _networkOfferingDao.persistDefaultNetworkOffering(defaultGuestDirectNetworkOffering);
        NetworkOfferingVO defaultGuestDirectPodBasedNetworkOffering = new NetworkOfferingVO(NetworkOffering.DefaultDirectPodBasedNetworkOffering, "DirectPodBased", TrafficType.Public, GuestIpType.DirectPodBased, true, false, rateMbps, multicastRateMbps, null, true);
        defaultGuestNetworkOffering = _networkOfferingDao.persistDefaultNetworkOffering(defaultGuestDirectPodBasedNetworkOffering);
        
        AccountsUsingNetworkSearch = _accountDao.createSearchBuilder();
        SearchBuilder<NetworkAccountVO> networkAccountSearch = _networksDao.createSearchBuilderForAccount();
        AccountsUsingNetworkSearch.join("nc", networkAccountSearch, AccountsUsingNetworkSearch.entity().getId(), networkAccountSearch.entity().getAccountId(), JoinType.INNER);
        networkAccountSearch.and("config", networkAccountSearch.entity().getNetworkId(), SearchCriteria.Op.EQ);
        networkAccountSearch.and("owner", networkAccountSearch.entity().isOwner(), SearchCriteria.Op.EQ);
        AccountsUsingNetworkSearch.done();
        
        AssignIpAddressSearch = _ipAddressDao.createSearchBuilder();
        SearchBuilder<VlanVO> vlanSearch = _vlanDao.createSearchBuilder();
        AssignIpAddressSearch.and("dc", AssignIpAddressSearch.entity().getDataCenterId(), Op.EQ);
        AssignIpAddressSearch.and("allocated", AssignIpAddressSearch.entity().getAllocatedTime(), Op.NULL);
        AssignIpAddressSearch.join("vlan", vlanSearch, vlanSearch.entity().getId(), AssignIpAddressSearch.entity().getVlanId(), JoinType.INNER);
        vlanSearch.and("type", vlanSearch.entity().getVlanType(), Op.EQ);
        vlanSearch.and("networkId", vlanSearch.entity().getNetworkId(), Op.EQ);
        AssignIpAddressSearch.done();
        
        IpAddressSearch = _ipAddressDao.createSearchBuilder();
        IpAddressSearch.and("accountId", IpAddressSearch.entity().getAllocatedToAccountId(), Op.EQ);
        IpAddressSearch.and("dataCenterId", IpAddressSearch.entity().getDataCenterId(), Op.EQ);
        SearchBuilder<VlanVO> virtualNetworkVlanSB = _vlanDao.createSearchBuilder();
        virtualNetworkVlanSB.and("vlanType", virtualNetworkVlanSB.entity().getVlanType(), Op.EQ);
        IpAddressSearch.join("virtualNetworkVlanSB", virtualNetworkVlanSB, IpAddressSearch.entity().getVlanId(), virtualNetworkVlanSB.entity().getId(), JoinBuilder.JoinType.INNER);
        IpAddressSearch.done();

        s_logger.info("Network Manager is configured.");

        return true;
    }
    
    @Override
    public String getName() {
        return _name;
    }

    @Override
    public boolean start() {
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
    public List<NetworkVO> setupNetwork(Account owner, NetworkOfferingVO offering, DeploymentPlan plan, String name, String displayText, boolean isShared) throws ConcurrentOperationException {
        return setupNetwork(owner, offering, null, plan, name, displayText, isShared);
    }

    @Override @DB
    public List<NetworkVO> setupNetwork(Account owner, NetworkOfferingVO offering, Network predefined, DeploymentPlan plan, String name, String displayText, boolean isShared) throws ConcurrentOperationException {
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
    
                NetworkVO vo = new NetworkVO(id, config, offering.getId(), plan.getDataCenterId(), guru.getName(), owner.getDomainId(), owner.getId(), related, name, displayText, isShared);
                configs.add(_networksDao.persist(vo));
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
            vo.setState(NicVO.State.Reserved);
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

    @DB
    protected Pair<NetworkGuru, NetworkVO> implementNetwork(long networkId, DeployDestination dest, ReservationContext context) throws ConcurrentOperationException, ResourceUnavailableException, InsufficientAddressCapacityException {
        Transaction.currentTxn();
        Pair<NetworkGuru, NetworkVO> implemented = new Pair<NetworkGuru, NetworkVO>(null, null);

        NetworkVO network = _networksDao.acquireInLockTable(networkId);
        if (network == null) {
            throw new ConcurrentOperationException("Unable to acquire network configuration: " + networkId);
        }

        try {
            NetworkGuru guru = _networkGurus.get(network.getGuruName());
            Network.State state = network.getState();
            if (state == Network.State.Implemented || state == Network.State.Setup) {
                implemented.set(guru, network);
                return implemented;
            }

            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Asking " + guru + " to implement " + network);
            }

            NetworkOfferingVO offering = _networkOfferingDao.findById(network.getNetworkOfferingId());

            Network result = guru.implement(network, offering, dest, context);
            network.setCidr(result.getCidr());
            network.setBroadcastUri(result.getBroadcastUri());
            network.setGateway(result.getGateway());
            network.setDns1(result.getDns1());
            network.setDns2(result.getDns2());
            network.setMode(result.getMode());
            network.setState(Network.State.Implemented);
            _networksDao.update(networkId, network);

            for (NetworkElement element : _networkElements) {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Asking " + element.getName() + " to implmenet " + network);
                }
                try {
                    element.implement(network, offering, dest, context);
                } catch (InsufficientCapacityException e) {
                    throw new ResourceUnavailableException("Unable to start domain router for this VM", e);
                }
            }

            implemented.set(guru, network);
            return implemented;
        } finally {
            if (implemented.first() == null) {
                s_logger.debug("Cleaning up because we're unable to implement network " + network);
            }
            _networksDao.releaseFromLockTable(networkId);
        }
    }

    @Override
    public void prepare(VirtualMachineProfile<? extends VMInstanceVO> vmProfile, DeployDestination dest, ReservationContext context) throws InsufficientNetworkCapacityException, ConcurrentOperationException, ResourceUnavailableException {
        List<NicVO> nics = _nicDao.listBy(vmProfile.getId());
        for (NicVO nic : nics) {
            Pair<NetworkGuru, NetworkVO> implemented = implementNetwork(nic.getNetworkId(), dest, context);
            NetworkGuru concierge = implemented.first();
            NetworkVO network = implemented.second();
            NicProfile profile = null;
            if (nic.getReservationStrategy() == ReservationStrategy.Start) {
                nic.setState(Resource.State.Reserving);
                _nicDao.update(nic.getId(), nic);
                URI broadcastUri = nic.getBroadcastUri();
                if (broadcastUri == null) {
                    network.getBroadcastUri();
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
                _nicDao.update(nic.getId(), nic);      
            } else {
                profile = new NicProfile(nic, network, nic.getBroadcastUri(), nic.getIsolationUri());
            }
            
            for (NetworkElement element : _networkElements) {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Asking " + element.getName() + " to prepare for " + nic);
                }
                element.prepare(network, profile, vmProfile, dest, context);
            }

            vmProfile.addNic(profile);
            _networksDao.changeActiveNicsBy(network.getId(), 1);
        }
    }

    @Override
    public void release(VirtualMachineProfile<? extends VMInstanceVO> vmProfile) {
        List<NicVO> nics = _nicDao.listBy(vmProfile.getId());
        for (NicVO nic : nics) {
            NetworkVO network = _networksDao.findById(nic.getNetworkId());
            if (nic.getReservationStrategy() == ReservationStrategy.Start) {
                NetworkGuru concierge = _networkGurus.get(network.getGuruName());
                nic.setState(Resource.State.Releasing);
                _nicDao.update(nic.getId(), nic);
                NicProfile profile = new NicProfile(nic, network, null, null);
                if (!concierge.release(profile, vmProfile, nic.getReservationId())) {
                    nic.setState(Resource.State.Allocated);
                    _nicDao.update(nic.getId(), nic);
                }
            }
            _networksDao.changeActiveNicsBy(network.getId(), -1);
        }
    }

    @Override
    public List<? extends Nic> getNics(VirtualMachine vm) {
        return _nicDao.listBy(vm.getId());
    }

    public static boolean isAdmin(short accountType) {
        return ((accountType == Account.ACCOUNT_TYPE_ADMIN) ||
                (accountType == Account.ACCOUNT_TYPE_DOMAIN_ADMIN) ||
                (accountType == Account.ACCOUNT_TYPE_READ_ONLY_ADMIN));
    }

    private Account findAccountByIpAddress(String ipAddress) {
        IPAddressVO address = _ipAddressDao.findById(ipAddress);
        if ((address != null) && (address.getAllocatedToAccountId() != null)) {
            return _accountDao.findById(address.getAllocatedToAccountId());
        }
        return null;
    }

    @Override
    @DB
    public boolean disassociateIpAddress(DisassociateIPAddrCmd cmd) throws PermissionDeniedException, IllegalArgumentException {
        Transaction txn = Transaction.currentTxn();

        Long userId = UserContext.current().getUserId();
        Account account = UserContext.current().getAccount();
        String ipAddress = cmd.getIpAddress();

        // Verify input parameters
        Account accountByIp = findAccountByIpAddress(ipAddress);
        if(accountByIp == null) { 
            throw new ServerApiException(BaseCmd.PARAM_ERROR, "Unable to find account owner for ip " + ipAddress);
        }

        Long accountId = accountByIp.getId();
        if (account != null) {
            if (!isAdmin(account.getType())) {
                if (account.getId() != accountId.longValue()) {
                    throw new ServerApiException(BaseCmd.PARAM_ERROR, "account " + account.getAccountName() + " doesn't own ip address " + ipAddress);
                }
            } else if (!_domainDao.isChildDomain(account.getDomainId(), accountByIp.getDomainId())) {
                throw new ServerApiException(BaseCmd.ACCOUNT_ERROR, "Unable to disassociate IP address " + ipAddress + ", permission denied.");
            }
        }

        // If command is executed via 8096 port, set userId to the id of System account (1)
        if (userId == null) {
            userId = Long.valueOf(1);
        }

        try {
            IPAddressVO ipVO = _ipAddressDao.findById(ipAddress);
            if (ipVO == null) {
                return false;
            }

            if (ipVO.getAllocatedTime() == null) {
                return true;
            }

            Account Account = _accountDao.findById(accountId);
            if (Account == null) {
                return false;
            }

            if ((ipVO.getAllocatedToAccountId() == null) || (ipVO.getAllocatedToAccountId().longValue() != accountId)) {
                // FIXME: is the user visible in the admin account's domain????
                if (!BaseCmd.isAdmin(Account.getType())) {
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

            txn.start();
            boolean success = releasePublicIpAddress(ipAddress, accountId, userId);
            if (success) {
                _accountMgr.decrementResourceCount(accountId, ResourceType.public_ip);
            }
            txn.commit();
            return success;

        } catch (PermissionDeniedException pde) {
            throw pde;
        } catch (IllegalArgumentException iae) {
            throw iae;
        } catch (Throwable t) {
            s_logger.error("Disassociate IP address threw an exception.");
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
    public List<NetworkVO> setupNetwork(Account owner, ServiceOfferingVO offering, DeploymentPlan plan) throws ConcurrentOperationException {
        NetworkOfferingVO networkOffering = _networkOfferingDao.findByServiceOffering(offering);
        return setupNetwork(owner, networkOffering, plan, null, null, false);
    }

    private String [] getGuestIpRange() {
        String guestRouterIp = _configs.get(Config.GuestIpNetwork.key());
        String guestNetmask = _configs.get(Config.GuestNetmask.key());
        return NetUtils.ipAndNetMaskToRange(guestRouterIp, guestNetmask);
    }


    @Override
    @DB
    public RemoteAccessVpnVO createRemoteAccessVpn(CreateRemoteAccessVpnCmd cmd)
    throws InvalidParameterValueException, PermissionDeniedException, ConcurrentOperationException {
        return null;
//        String publicIp = cmd.getPublicIp();
//        IPAddressVO ipAddr = null;
//        Account account = getAccountForApiCommand(cmd.getAccountName(), cmd.getDomainId());
//        if (publicIp == null) {
//            List<IPAddressVO> accountAddrs = _ipAddressDao.listByAccount(account.getId());
//            for (IPAddressVO addr: accountAddrs){
//                if (addr.getSourceNat() && addr.getDataCenterId() == cmd.getZoneId()){
//                    ipAddr = addr;
//                    publicIp = ipAddr.getAddress();
//                    break;
//                }
//            }
//            if (ipAddr == null) {
//                throw new InvalidParameterValueException("Account " + account.getAccountName() +  " does not have any public ip addresses in zone " + cmd.getZoneId());
//            }
//        }
//
//        // make sure ip address exists
//        ipAddr = _ipAddressDao.findById(publicIp);
//        if (ipAddr == null) {
//            throw new InvalidParameterValueException("Unable to create remote access vpn, invalid public IP address " + publicIp);
//        }
//
//        VlanVO vlan = _vlanDao.findById(ipAddr.getVlanDbId());
//        if (vlan != null) {
//            if (!VlanType.VirtualNetwork.equals(vlan.getVlanType())) {
//                throw new InvalidParameterValueException("Unable to create VPN for IP address " + publicIp + ", only VirtualNetwork type IP addresses can be used for VPN.");
//            }
//        } 
//        assert vlan != null:"Inconsistent DB state -- ip address does not belong to any vlan?";
//
//        if ((ipAddr.getAccountId() == null) || (ipAddr.getAllocated() == null)) {
//            throw new PermissionDeniedException("Unable to create VPN, permission denied for ip " + publicIp);
//        }
//
//        if (account != null) {
//            if ((account.getType() == Account.ACCOUNT_TYPE_ADMIN) || (account.getType() == Account.ACCOUNT_TYPE_DOMAIN_ADMIN)) {
//                if (!_domainDao.isChildDomain(account.getDomainId(), ipAddr.getDomainId())) {
//                    throw new PermissionDeniedException("Unable to create VPN with public IP address " + publicIp + ", permission denied.");
//                }
//            } else if (account.getId() != ipAddr.getAccountId().longValue()) {
//                throw new PermissionDeniedException("Unable to create VPN for account " + account.getAccountName() + " doesn't own ip address " + publicIp);
//            }
//        }
//
//        RemoteAccessVpnVO vpnVO = _remoteAccessVpnDao.findByPublicIpAddress(publicIp);
//        if (vpnVO != null) {
//            throw new InvalidParameterValueException("A Remote Access VPN already exists for this public Ip address");
//        }
//        //TODO: assumes one virtual network / domr per account per zone
//        vpnVO = _remoteAccessVpnDao.findByAccountAndZone(account.getId(), cmd.getZoneId());
//        if (vpnVO != null) {
//            throw new InvalidParameterValueException("A Remote Access VPN already exists for this account");
//        }
//        String ipRange = cmd.getIpRange();
//        if (ipRange == null) {
//            ipRange = _configs.get(Config.RemoteAccessVpnClientIpRange.key());
//        }
//        String [] range = ipRange.split("-");
//        if (range.length != 2) {
//            throw new InvalidParameterValueException("Invalid ip range");
//        }
//        if (!NetUtils.isValidIp(range[0]) || !NetUtils.isValidIp(range[1])){
//            throw new InvalidParameterValueException("Invalid ip in range specification " + ipRange);
//        }
//        if (!NetUtils.validIpRange(range[0], range[1])){
//            throw new InvalidParameterValueException("Invalid ip range " + ipRange);
//        }
//        String [] guestIpRange = getGuestIpRange();
//        if (NetUtils.ipRangesOverlap(range[0], range[1], guestIpRange[0], guestIpRange[1])) {
//            throw new InvalidParameterValueException("Invalid ip range: " + ipRange + " overlaps with guest ip range " + guestIpRange[0] + "-" + guestIpRange[1]);
//        }
//        //TODO: check sufficient range
//        //TODO: check overlap with private and public ip ranges in datacenter
//
//        long startIp = NetUtils.ip2Long(range[0]);
//        String newIpRange = NetUtils.long2Ip(++startIp) + "-" + range[1];
//        String sharedSecret = PasswordGenerator.generatePresharedKey(getIntegerConfigValue(Config.RemoteAccessVpnPskLength.key(), 24)); 
//        Transaction txn = Transaction.currentTxn();
//        txn.start();
//        boolean locked = false;
//        try {
//            ipAddr = _ipAddressDao.acquireInLockTable(publicIp);
//            if (ipAddr == null) {
//                throw new ConcurrentOperationException("Another operation active, unable to create vpn");
//            }
//            locked = true;
//            //check overlap with port forwarding rules on this ip (udp ports 500, 4500)
//            List<PortForwardingRuleVO> existing = _rulesDao.listIPForwardingByPortAndProto(publicIp, NetUtils.VPN_PORT, NetUtils.UDP_PROTO);
//            if (!existing.isEmpty()) {
//                throw new InvalidParameterValueException("UDP Port " + NetUtils.VPN_PORT + " is configured for destination NAT");
//            }
//            existing = _rulesDao.listIPForwardingByPortAndProto(publicIp, NetUtils.VPN_NATT_PORT, NetUtils.UDP_PROTO);
//            if (!existing.isEmpty()) {
//                throw new InvalidParameterValueException("UDP Port " + NetUtils.VPN_NATT_PORT + " is configured for destination NAT");
//            }
//            existing = _rulesDao.listIPForwardingByPortAndProto(publicIp, NetUtils.VPN_L2TP_PORT, NetUtils.UDP_PROTO);
//            if (!existing.isEmpty()) {
//                throw new InvalidParameterValueException("UDP Port " + NetUtils.VPN_L2TP_PORT + " is configured for destination NAT");
//            }
//            if (_rulesDao.isPublicIpOneToOneNATted(publicIp)) {
//                throw new InvalidParameterValueException("Public Ip " + publicIp + " is configured for destination NAT");
//            }
//            vpnVO = new RemoteAccessVpnVO(account.getId(), cmd.getZoneId(), publicIp, range[0], newIpRange, sharedSecret);
//            vpnVO = _remoteAccessVpnDao.persist(vpnVO);
//            PortForwardingRuleVO rule = new PortForwardingRuleVO(null, publicIp, NetUtils.VPN_PORT, guestIpRange[0], NetUtils.VPN_PORT, true, NetUtils.UDP_PROTO, false, null);
//            _rulesDao.persist(rule);
//            rule = new PortForwardingRuleVO(null, publicIp, NetUtils.VPN_NATT_PORT, guestIpRange[0], NetUtils.VPN_NATT_PORT, true, NetUtils.UDP_PROTO, false, null);
//            _rulesDao.persist(rule);
//            rule = new PortForwardingRuleVO(null, publicIp, NetUtils.VPN_L2TP_PORT, guestIpRange[0], NetUtils.VPN_L2TP_PORT, true, NetUtils.UDP_PROTO, false, null);
//            _rulesDao.persist(rule);
//            txn.commit();
//            return vpnVO;
//        } finally {
//            if (locked) {
//                _ipAddressDao.releaseFromLockTable(publicIp);
//            }
//        }
    }

    @Override
    @DB
    public RemoteAccessVpnVO startRemoteAccessVpn(CreateRemoteAccessVpnCmd cmd) throws ConcurrentOperationException, ResourceUnavailableException {
        Long userId = UserContext.current().getUserId();
        Account account = getAccountForApiCommand(cmd.getAccountName(), cmd.getDomainId());
        EventUtils.saveStartedEvent(userId, account.getId(), EventTypes.EVENT_REMOTE_ACCESS_VPN_CREATE, "Creating a Remote Access VPN for account: " + account.getAccountName() + " in zone " + cmd.getZoneId(), cmd.getStartEventId());
        RemoteAccessVpnVO vpnVO = _remoteAccessVpnDao.findById(cmd.getEntityId());
        String publicIp = vpnVO.getVpnServerAddress();
        Long  vpnId = vpnVO.getId();
        Transaction txn = Transaction.currentTxn();
        txn.start();
        boolean locked = false;
        boolean created = false;
        try {
            IPAddressVO ipAddr = _ipAddressDao.acquireInLockTable(publicIp);
            if (ipAddr == null) {
                throw new ConcurrentOperationException("Another operation active, unable to create vpn");
            }
            locked = true;

            vpnVO = _routerMgr.startRemoteAccessVpn(vpnVO);
            created = (vpnVO != null);

            return vpnVO;
        } finally {
            if (created) {
                EventUtils.saveEvent(userId, account.getId(), EventTypes.EVENT_REMOTE_ACCESS_VPN_CREATE, "Created a Remote Access VPN for account: " + account.getAccountName() + " in zone " + cmd.getZoneId());
            } else {
                EventUtils.saveEvent(userId, account.getId(), EventVO.LEVEL_ERROR, EventTypes.EVENT_REMOTE_ACCESS_VPN_CREATE, "Unable to create Remote Access VPN ", account.getAccountName() + " in zone " + cmd.getZoneId());
                _remoteAccessVpnDao.remove(vpnId);
            }
            txn.commit();
            if (locked) {
                _ipAddressDao.releaseFromLockTable(publicIp);
            }
        }
    }

    @Override
    @DB
    public boolean destroyRemoteAccessVpn(DeleteRemoteAccessVpnCmd cmd) throws ConcurrentOperationException {
//        Long userId = UserContext.current().getUserId();
//        Account account = getAccountForApiCommand(cmd.getAccountName(), cmd.getDomainId());
//        //TODO: assumes one virtual network / domr per account per zone
//        RemoteAccessVpnVO vpnVO = _remoteAccessVpnDao.findByAccountAndZone(account.getId(), cmd.getZoneId());
//        if (vpnVO == null) {
//            throw new InvalidParameterValueException("No VPN found for account " + account.getAccountName() + " in zone " + cmd.getZoneId());
//        }
//        EventUtils.saveStartedEvent(userId, account.getId(), EventTypes.EVENT_REMOTE_ACCESS_VPN_DESTROY, "Deleting Remote Access VPN for account: " + account.getAccountName() + " in zone " + cmd.getZoneId(), cmd.getStartEventId());
//        String publicIp = vpnVO.getVpnServerAddress();
//        Long  vpnId = vpnVO.getId();
//        Transaction txn = Transaction.currentTxn();
//        txn.start();
//        boolean locked = false;
//        boolean deleted = false;
//        try {
//            IPAddressVO ipAddr = _ipAddressDao.acquireInLockTable(publicIp);
//            if (ipAddr == null) {
//                throw new ConcurrentOperationException("Another operation active, unable to create vpn");
//            }
//            locked = true;
//
//            deleted = _routerMgr.deleteRemoteAccessVpn(vpnVO);
//            return deleted;
//        } finally {
//            if (deleted) {
//                _remoteAccessVpnDao.remove(vpnId);
//                _rulesDao.deleteIPForwardingByPublicIpAndPort(publicIp, NetUtils.VPN_PORT);
//                _rulesDao.deleteIPForwardingByPublicIpAndPort(publicIp, NetUtils.VPN_NATT_PORT);
//                _rulesDao.deleteIPForwardingByPublicIpAndPort(publicIp, NetUtils.VPN_L2TP_PORT);
//                EventUtils.saveEvent(userId, account.getId(), EventTypes.EVENT_REMOTE_ACCESS_VPN_DESTROY, "Deleted Remote Access VPN for account: " + account.getAccountName() + " in zone " + cmd.getZoneId());
//            } else {
//                EventUtils.saveEvent(userId, account.getId(), EventVO.LEVEL_ERROR, EventTypes.EVENT_REMOTE_ACCESS_VPN_DESTROY, "Unable to delete Remote Access VPN ", account.getAccountName() + " in zone " + cmd.getZoneId());
//            }
//            txn.commit();
//            if (locked) {
//                _ipAddressDao.releaseFromLockTable(publicIp);
//            }
//        }
        return false; // FIXME
    }

    @Override
    @DB
    public VpnUserVO addVpnUser(AddVpnUserCmd cmd) throws ConcurrentOperationException, InvalidParameterValueException, AccountLimitException {
        Long userId = UserContext.current().getUserId();
        Account account = getAccountForApiCommand(cmd.getAccountName(), cmd.getDomainId());
        EventUtils.saveStartedEvent(userId, account.getId(), EventTypes.EVENT_VPN_USER_ADD, "Add VPN user for account: " + account.getAccountName(), cmd.getStartEventId());

        if (!cmd.getUserName().matches("^[a-zA-Z0-9][a-zA-Z0-9@._-]{2,63}$")) {
            throw new InvalidParameterValueException("Username has to be begin with an alphabet have 3-64 characters including alphabets, numbers and the set '@.-_'");
        }
        if (!cmd.getPassword().matches("^[a-zA-Z0-9][a-zA-Z0-9@#+=._-]{2,31}$")) {
            throw new InvalidParameterValueException("Password has to be 3-32 characters including alphabets, numbers and the set '@#+=.-_'");
        }
        account = _accountDao.acquireInLockTable(account.getId());
        if (account == null) {
            throw new ConcurrentOperationException("Unable to add vpn user: Another operation active");
        }
        try {
            long userCount = _vpnUsersDao.getVpnUserCount(account.getId());
            Integer userLimit = getIntegerConfigValue(Config.RemoteAccessVpnUserLimit.key(), 8);
            if (userCount >= userLimit) {
                throw new AccountLimitException("Cannot add more than " + userLimit + " remote access vpn users");
            }
            VpnUserVO user = addRemoveVpnUser(account, cmd.getUserName(), cmd.getPassword(), true);
            if (user != null) {
                EventUtils.saveEvent(userId, account.getId(), EventTypes.EVENT_VPN_USER_ADD, "Added a VPN user for account: " + account.getAccountName() + " username= " + cmd.getUserName());
                return user;
            } else {
                EventUtils.saveEvent(userId, account.getId(), EventVO.LEVEL_ERROR, EventTypes.EVENT_VPN_USER_ADD, "Unable to add VPN user for account: ", account.getAccountName() + " username= " + cmd.getUserName());
                throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Unable to add VPN user for account: "+ account.getAccountName() + " username= " + cmd.getUserName());
            }
        } finally {
            if (account != null) {
                _accountDao.releaseFromLockTable(account.getId());
            }
        }


    }

    @Override
    public boolean removeVpnUser(RemoveVpnUserCmd cmd) throws ConcurrentOperationException {
        Long userId = UserContext.current().getUserId();
        Account account = getAccountForApiCommand(cmd.getAccountName(), cmd.getDomainId());
        EventUtils.saveStartedEvent(userId, account.getId(), EventTypes.EVENT_VPN_USER_REMOVE, "Remove VPN user for account: " + account.getAccountName(), cmd.getStartEventId());

        VpnUserVO user = addRemoveVpnUser(account, cmd.getUserName(), null, false);
        if (user != null) {
            EventUtils.saveEvent(userId, account.getId(), EventTypes.EVENT_VPN_USER_REMOVE, "Removed a VPN user for account: " + account.getAccountName() + " username= " + cmd.getUserName());
        } else {
            EventUtils.saveEvent(userId, account.getId(), EventVO.LEVEL_ERROR, EventTypes.EVENT_VPN_USER_ADD, "Unable to remove VPN user for account: ", account.getAccountName() + " username= " + cmd.getUserName());
        }
        return (user != null);

    }

    @DB
    protected VpnUserVO addRemoveVpnUser(Account account, String username, String password, boolean add) throws ConcurrentOperationException {
        List<RemoteAccessVpnVO> vpnVOList = _remoteAccessVpnDao.findByAccount(account.getId());

        Transaction txn = Transaction.currentTxn();
        txn.start();
        boolean locked = false;
        boolean success = true;
        VpnUserVO user = null;
        final String op = add ? "add" : "remove";
        try {
            account = _accountDao.acquireInLockTable(account.getId());
            if (account == null) {
                throw new ConcurrentOperationException("Unable to " +  op + " vpn user: Another operation active");
            }
            locked = true;
            List<VpnUserVO> addVpnUsers = new ArrayList<VpnUserVO>();
            List<VpnUserVO> removeVpnUsers = new ArrayList<VpnUserVO>();
            if (add) {

                user = _vpnUsersDao.persist(new VpnUserVO(account.getId(), username, password));
                addVpnUsers.add(user);

            } else {
                user = _vpnUsersDao.findByAccountAndUsername(account.getId(), username);
                if (user == null) {
                    s_logger.debug("Could not find vpn user " + username);
                    throw new InvalidParameterValueException("Could not find vpn user " + username);
                }
                _vpnUsersDao.remove(user.getId());
                removeVpnUsers.add(user);
            }
            for (RemoteAccessVpnVO vpn : vpnVOList) {
                success = success && _routerMgr.addRemoveVpnUsers(vpn, addVpnUsers, removeVpnUsers);
            }

            // Note: If the router was successfully updated, we then return the user.
            if (success) {
                return user;
            } else {
                return null;
            }
        } finally {
            if (success) {
                txn.commit();
            } else {
                txn.rollback();
            }
            if (locked) {
                _accountDao.releaseFromLockTable(account.getId());
            }
        }
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

    @Override @DB
    public Network createNetwork(CreateNetworkCmd cmd) throws InvalidParameterValueException, PermissionDeniedException{
        Account ctxAccount = UserContext.current().getAccount();
        Long userId = UserContext.current().getUserId();
        Long networkOfferingId = cmd.getNetworkOfferingId();
        Long zoneId = cmd.getZoneId();
        String gateway = cmd.getGateway();
        String startIP = cmd.getStartIp();
        String endIP = cmd.getEndIp();
        String vlanNetmask = cmd.getNetmask();
        String cidr = null;
        if (gateway != null && vlanNetmask != null) {
            cidr = NetUtils.ipAndNetMaskToCidr(gateway, vlanNetmask);
        }
        String accountName = cmd.getAccountName();
        Long domainId = cmd.getDomainId();
        String vlanId = cmd.getVlan();
        String name = cmd.getNetworkName();
        String displayText = cmd.getDisplayText();
        Boolean isShared = cmd.getIsShared();
        Account owner = null;
        
        //if end ip is not specified, default it to startIp 
        if (endIP == null && startIP != null) {
            endIP = startIP;
        }
        
        //Check if network offering exists
        NetworkOfferingVO networkOffering = _networkOfferingDao.findById(networkOfferingId);
        if (networkOffering == null || networkOffering.isSystemOnly()) {
            throw new InvalidParameterValueException("Unable to find network offeirng by id " + networkOfferingId);
        }
        
        //Check if zone exists
        if (zoneId == null || ((_dcDao.findById(zoneId)) == null)) {
            throw new InvalidParameterValueException("Please specify a valid zone.");
        }
        
        //Check permissions
        if (isAdmin(ctxAccount.getType())) {
            if (domainId != null) {
                if ((ctxAccount != null) && !_domainDao.isChildDomain(ctxAccount.getDomainId(), domainId)) {
                    throw new PermissionDeniedException("Failed to create a newtwork, invalid domain id (" + domainId + ") given.");
                }
                if (accountName != null) {
                    owner = _accountDao.findActiveAccount(accountName, domainId);
                    if (owner == null) {
                        throw new InvalidParameterValueException("Unable to find account " + accountName + " in domain " + domainId);
                    }
                }
            } else {
                owner = ctxAccount;
            }
        } else {
            owner = ctxAccount;
        }
        
        //Don't allow to create network with vlan that already exists in the system
        if (networkOffering.getGuestIpType() == GuestIpType.Direct && vlanId != null) {
            String uri ="vlan://" + vlanId;
            List<NetworkVO> networks = _networksDao.listBy(zoneId, uri);
            if ((networks != null && !networks.isEmpty())) {
                throw new InvalidParameterValueException("Network with vlan " + vlanId + " already exists in zone " + zoneId);
            }
        }
        
       //if VlanId is Direct untagged, verify if there is already network of this type in the zone
        if (networkOffering.getGuestIpType() == GuestIpType.DirectPodBased && vlanId != null && vlanId.equalsIgnoreCase(Vlan.UNTAGGED)) {
            SearchBuilder<NetworkVO> sb = _networksDao.createSearchBuilder();
            sb.and("broadcastDomainType", sb.entity().getBroadcastDomainType(), SearchCriteria.Op.EQ);
            sb.and("dataCenterId", sb.entity().getDataCenterId(), SearchCriteria.Op.EQ);
            
            SearchBuilder<NetworkOfferingVO> networkSearch = _networkOfferingDao.createSearchBuilder();
            networkSearch.and("guestIpType", networkSearch.entity().getGuestIpType(), SearchCriteria.Op.EQ);
            sb.join("networkSearch", networkSearch, sb.entity().getNetworkOfferingId(), networkSearch.entity().getId(), JoinBuilder.JoinType.INNER);
         
            SearchCriteria<NetworkVO> sc = sb.create();
            sc.setParameters("broadcastDomainType", BroadcastDomainType.Native);
            sc.setParameters("dataCenterId", zoneId);
            sc.setJoinParameters("networkSearch", "guestIpType", GuestIpType.DirectPodBased);

            List<NetworkVO> networks = _networksDao.search(sc, null);
            if (networks!= null && !networks.isEmpty()) {
                throw new InvalidParameterValueException("Network with untagged vlan already exists for the zone " + zoneId);
            }
        }
        
        //Regular user can create only network of Virtual type
        if (ctxAccount.getType() == Account.ACCOUNT_TYPE_NORMAL && networkOffering.getGuestIpType() != GuestIpType.Virtual) {
            throw new InvalidParameterValueException("Regular user can create only networ of type " + GuestIpType.Virtual);
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
           
           List<NetworkVO> networks = setupNetwork(owner, networkOffering, userNetwork, plan, name, displayText, isShared);
           Long networkId = null;
           
           if (networks == null || networks.isEmpty()) {
               txn.rollback();
               throw new CloudRuntimeException("Fail to create a network");
           } else {
               networkId = networks.get(0).getId();
           }

           for (Network network : networks) {
               if (network.getGuestType() == GuestIpType.Virtual) {
                   s_logger.debug("Creating a source natp ip for " + network);
                   PublicIp ip = assignSourceNatIpAddress(owner, network, userId);
                   if (ip == null) {
                       throw new InsufficientAddressCapacityException("Unable to assign source nat ip address to owner for this network", DataCenter.class, zoneId);
                   }
               }
           }
           
           //Don't pass owner to create vlan when network offering is of type Direct
           if (networkOffering.getGuestIpType() == GuestIpType.Direct) {
               owner = null;
           }
           
           if (ctxAccount.getType() == Account.ACCOUNT_TYPE_ADMIN && networkOffering.getGuestIpType() == GuestIpType.Direct && startIP != null && endIP != null && gateway != null) {
               //Create vlan ip range
               Vlan vlan = _configMgr.createVlanAndPublicIpRange(userId, zoneId, null, startIP, endIP, gateway, vlanNetmask, false, vlanId, owner, networkId);
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
        Account account = UserContext.current().getAccount();
        Long domainId = cmd.getDomainId();
        String accountName = cmd.getAccountName();
        String type = cmd.getType();
        Boolean isSystem = cmd.getIsSystem();
        Long accountId = null;
        
        if (isSystem == null) {
            isSystem = false;
        }
        
        if (isAdmin(account.getType())) {
            if (domainId != null) {
                if ((account != null) && !_domainDao.isChildDomain(account.getDomainId(), domainId)) {
                    throw new PermissionDeniedException("Invalid domain id (" + domainId + ") given, unable to list networks");
                }

                if (accountName != null) {
                    account = _accountDao.findActiveAccount(accountName, domainId);
                    if (account == null) {
                        throw new InvalidParameterValueException("Unable to find account " + accountName + " in domain " + domainId);
                    }
                    accountId = account.getId();
                }
            } else {
                accountId = account.getId();
            }
        } else {
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
            sc.setJoinParameters("networkOfferingSearch", "trafficType", TrafficType.Public);
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
        
        SearchCriteria<NetworkVO> ssc = _networksDao.createSearchCriteria();
        ssc.addOr("accountId", SearchCriteria.Op.EQ, accountId);
        if (accountName == null && domainId == null) {
            ssc.addOr("accountId", SearchCriteria.Op.EQ, 1L);
        }
        sc.addAnd("accountId", SearchCriteria.Op.SC, ssc);
        
        return _networksDao.search(sc, searchFilter);
    }
    
    @Override @DB
    public boolean deleteNetwork(DeleteNetworkCmd cmd) throws InvalidParameterValueException, PermissionDeniedException{        
        Long networkId = cmd.getId();
        Long userId = UserContext.current().getUserId();
        Account account = UserContext.current().getAccount();

        //Verify network id
        NetworkVO network = _networksDao.findById(networkId);
        if (network == null) {
            throw new InvalidParameterValueException("unable to find network " + networkId);
        } 
        
        //Perform permission check
        if (account != null) {
            if (!isAdmin(account.getType())) {
                if (network.getAccountId() != account.getId()) {
                    throw new PermissionDeniedException("Account " + account.getAccountName() + " does not own network id=" + networkId + ", permission denied");
                }
            } else if (!(account.getType() == Account.ACCOUNT_TYPE_ADMIN) && !_domainDao.isChildDomain(account.getDomainId(), _accountDao.findById(network.getAccountId()).getId())) {
                throw new PermissionDeniedException("Unable to delete network " + networkId + ", permission denied.");
            }
        }
        
        //Don't allow to remove network if there are non-destroyed vms using it
        List<NicVO> nics = _nicDao.listByNetworkId(networkId);
        for (NicVO nic : nics) {
            UserVm vm = _vmDao.findById(nic.getId());
            if (vm != null && (vm.getState() != State.Destroyed || vm.getState() != State.Expunging || vm.getState() != State.Error)) {
                throw new CloudRuntimeException("Can't delete a network; make sure that all vms using the network are destroyed");
            }
        }
        
        //for regular user don't allow to remove network when it's in any other states but allocated
        if (account.getType() != Account.ACCOUNT_TYPE_ADMIN) {
            if (network.getState() != Network.State.Allocated) {
                throw new InvalidParameterValueException("Non-admin user can delete network in " + Network.State.Allocated + " state only.");
            }    
        } else {
            if (!(network.getState() == Network.State.Allocated || network.getState() == Network.State.Setup)) {
                throw new InvalidParameterValueException("Can delete network in " + Network.State.Allocated + " and " + Network.State.Setup + " states only.");
            }    
        }
        
        //remove all the vlans associated with the network
        Transaction txn = Transaction.currentTxn();
        try {
            //remove corresponding vlans
            List<VlanVO> vlans = _vlanDao.listVlansByNetworkId(networkId);
            for (VlanVO vlan : vlans) {
                boolean result = _configMgr.deleteVlanAndPublicIpRange(userId, vlan.getId());
                if (result == false) {
                    txn.rollback();
                    throw new CloudRuntimeException("Unable to delete a network: failed to delete corresponding vlan with id " + vlan.getId());
                }
            }
            
            //remove networks
            _networksDao.remove(networkId);
            
            txn.commit();
            return true;
        } catch (Exception ex) {
            txn.rollback();
            s_logger.warn("Unexpected exception during deleting a network ", ex);
            return false;
        } finally {
            txn.close();
        }
        
    }
    
    @DB 
    public void shutdownNetwork(long networkId) {
        Transaction txn = Transaction.currentTxn();
        txn.start();
        NetworkVO network = _networksDao.lockRow(networkId, true);
        if (network == null) {
            s_logger.debug("Unable to find network with id: " + networkId);
            return;
        }
        if (network.getState() != Network.State.Implemented && network.getState() != Network.State.Destroying) {
            s_logger.debug("Network is not implemented: " + network);
            return;
        }
        network.setState(Network.State.Destroying);
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
        
        if (success) {
            NetworkGuru guru = _networkGurus.get(network.getGuruName());
            guru.destroy(network, _networkOfferingDao.findById(network.getNetworkOfferingId()));
            network.setState(Network.State.Allocated);
            _networksDao.update(network.getId(), network);
        } else {
            network.setState(Network.State.Implemented);
            _networksDao.update(network.getId(), network);
        }
        
        
    }
    
    @Override
    public boolean applyRules(Ip ip, List<? extends FirewallRule> rules, boolean continueOnError) throws ResourceUnavailableException {
        if (rules.size() == 0) {
            s_logger.debug("There are no rules to forward to the network elements");
            return true;
        }
        
        boolean success = true;
        Network network = _networksDao.findById(rules.get(0).getNetworkId());
        for (NetworkElement ne : _networkElements) {
            try {
                boolean handled = ne.applyRules(network, rules);
                s_logger.debug("Network Rules for " + ip + " were " + (handled ? "" : " not") + " handled by " + ne.getName());
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
            List<Long> shutdownList = new ArrayList<Long>();
            long currentTime = System.currentTimeMillis() >> 10;
            HashMap<Long, Long> stillFree = new HashMap<Long, Long>();
            
            List<Long> networkIds = _nicDao.listNetworksWithNoActiveNics();
            for (Long networkId : networkIds) {
                Long time = _lastNetworkIdsToFree.remove(networkId);
                if (time == null) {
                    if (s_logger.isDebugEnabled()) {
                        s_logger.debug("We found network " + networkId + " to be free for the first time.  Adding it to the list: " + currentTime);
                    }
                    stillFree.put(networkId, currentTime);
                } else if (time < (currentTime + 600)) {
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
                shutdownNetwork(networkId);
            }
        }
        
    }
}
