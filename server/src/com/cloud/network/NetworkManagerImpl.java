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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;

import javax.ejb.Local;
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;

import com.cloud.agent.AgentManager;
import com.cloud.agent.AgentManager.OnError;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.routing.DhcpEntryCommand;
import com.cloud.agent.api.routing.IPAssocCommand;
import com.cloud.agent.api.routing.LoadBalancerCfgCommand;
import com.cloud.agent.api.routing.SetFirewallRuleCommand;
import com.cloud.agent.api.to.NicTO;
import com.cloud.agent.manager.Commands;
import com.cloud.alert.AlertManager;
import com.cloud.api.BaseCmd;
import com.cloud.api.ServerApiException;
import com.cloud.api.commands.AddVpnUserCmd;
import com.cloud.api.commands.AssignToLoadBalancerRuleCmd;
import com.cloud.api.commands.AssociateIPAddrCmd;
import com.cloud.api.commands.CreateLoadBalancerRuleCmd;
import com.cloud.api.commands.CreateNetworkCmd;
import com.cloud.api.commands.CreatePortForwardingRuleCmd;
import com.cloud.api.commands.CreateRemoteAccessVpnCmd;
import com.cloud.api.commands.DeleteLoadBalancerRuleCmd;
import com.cloud.api.commands.DeleteNetworkCmd;
import com.cloud.api.commands.DeleteRemoteAccessVpnCmd;
import com.cloud.api.commands.DisassociateIPAddrCmd;
import com.cloud.api.commands.ListNetworksCmd;
import com.cloud.api.commands.ListPortForwardingRulesCmd;
import com.cloud.api.commands.RemoveFromLoadBalancerRuleCmd;
import com.cloud.api.commands.RemoveVpnUserCmd;
import com.cloud.api.commands.UpdateLoadBalancerRuleCmd;
import com.cloud.async.AsyncJobManager;
import com.cloud.capacity.dao.CapacityDao;
import com.cloud.configuration.Config;
import com.cloud.configuration.ConfigurationManager;
import com.cloud.configuration.ResourceCount.ResourceType;
import com.cloud.configuration.dao.ConfigurationDao;
import com.cloud.configuration.dao.ResourceLimitDao;
import com.cloud.dc.DataCenter;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.HostPodVO;
import com.cloud.dc.Vlan;
import com.cloud.dc.Vlan.VlanType;
import com.cloud.dc.VlanVO;
import com.cloud.dc.dao.AccountVlanMapDao;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.HostPodDao;
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
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.network.Networks.AddressFormat;
import com.cloud.network.Networks.TrafficType;
import com.cloud.network.configuration.NetworkGuru;
import com.cloud.network.dao.FirewallRulesDao;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.LoadBalancerDao;
import com.cloud.network.dao.LoadBalancerVMMapDao;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkRuleConfigDao;
import com.cloud.network.dao.RemoteAccessVpnDao;
import com.cloud.network.dao.VpnUserDao;
import com.cloud.network.element.NetworkElement;
import com.cloud.network.router.DomainRouterManager;
import com.cloud.network.router.VirtualRouter;
import com.cloud.network.rules.FirewallRule;
import com.cloud.offering.NetworkOffering;
import com.cloud.offering.NetworkOffering.GuestIpType;
import com.cloud.offerings.NetworkOfferingVO;
import com.cloud.offerings.dao.NetworkOfferingDao;
import com.cloud.resource.Resource;
import com.cloud.resource.Resource.ReservationStrategy;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.storage.StorageManager;
import com.cloud.storage.dao.GuestOSDao;
import com.cloud.storage.dao.StoragePoolDao;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.storage.dao.VMTemplateHostDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.AccountVO;
import com.cloud.user.User;
import com.cloud.user.UserContext;
import com.cloud.user.UserStatisticsVO;
import com.cloud.user.dao.AccountDao;
import com.cloud.user.dao.UserDao;
import com.cloud.user.dao.UserStatisticsDao;
import com.cloud.uservm.UserVm;
import com.cloud.utils.Pair;
import com.cloud.utils.PasswordGenerator;
import com.cloud.utils.StringUtils;
import com.cloud.utils.Ternary;
import com.cloud.utils.component.Adapters;
import com.cloud.utils.component.Inject;
import com.cloud.utils.component.Manager;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.JoinBuilder;
import com.cloud.utils.db.JoinBuilder.JoinType;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.net.NetUtils;
import com.cloud.vm.DomainRouterVO;
import com.cloud.vm.Nic;
import com.cloud.vm.NicProfile;
import com.cloud.vm.NicVO;
import com.cloud.vm.ReservationContext;
import com.cloud.vm.State;
import com.cloud.vm.UserVmVO;
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
    @Inject FirewallRulesDao _rulesDao = null;
    @Inject LoadBalancerDao _loadBalancerDao = null;
    @Inject LoadBalancerVMMapDao _loadBalancerVMMapDao = null;
    @Inject IPAddressDao _ipAddressDao = null;
    @Inject VMTemplateDao _templateDao =  null;
    @Inject UserDao _userDao = null;
    @Inject AccountDao _accountDao = null;
    @Inject DomainDao _domainDao = null;
    @Inject UserStatisticsDao _userStatsDao = null;
    @Inject VolumeDao _volsDao = null;
    @Inject HostDao _hostDao = null;
    @Inject EventDao _eventDao = null;
    @Inject ConfigurationDao _configDao;
    @Inject HostPodDao _podDao = null;
    @Inject VMTemplateHostDao _vmTemplateHostDao = null;
    @Inject UserVmDao _vmDao = null;
    @Inject ResourceLimitDao _limitDao = null;
    @Inject CapacityDao _capacityDao = null;
    @Inject AgentManager _agentMgr;
    @Inject StorageManager _storageMgr;
    @Inject AlertManager _alertMgr;
    @Inject AccountManager _accountMgr;
    @Inject ConfigurationManager _configMgr;
    @Inject AsyncJobManager _asyncMgr;
    @Inject StoragePoolDao _storagePoolDao = null;
    @Inject ServiceOfferingDao _serviceOfferingDao = null;
    @Inject UserVmDao _userVmDao;
    @Inject FirewallRulesDao _firewallRulesDao;
    @Inject NetworkRuleConfigDao _networkRuleConfigDao;
    @Inject AccountVlanMapDao _accountVlanMapDao;
    @Inject UserStatisticsDao _statsDao = null;
    @Inject NetworkOfferingDao _networkOfferingDao = null;
    @Inject NetworkDao _networkConfigDao = null;
    @Inject NicDao _nicDao = null;
    @Inject GuestOSDao _guestOSDao = null;
    @Inject RemoteAccessVpnDao _remoteAccessVpnDao = null;
    @Inject VpnUserDao _vpnUsersDao = null;
    @Inject DomainRouterManager _routerMgr;

    @Inject(adapter=NetworkGuru.class)
    Adapters<NetworkGuru> _networkGurus;
    @Inject(adapter=NetworkElement.class)
    Adapters<NetworkElement> _networkElements;

    private HashMap<String, NetworkOfferingVO> _systemNetworks = new HashMap<String, NetworkOfferingVO>(5);

    ScheduledExecutorService _executor;

    SearchBuilder<AccountVO> AccountsUsingNetworkConfigurationSearch;

    private Map<String, String> _configs;

    @Override
    public boolean sendSshKeysToHost(Long hostId, String pubKey, String prvKey) {
        return _routerMgr.sendSshKeysToHost(hostId, pubKey, prvKey);
    }

    @Override @DB
    public String assignSourceNatIpAddress(Account account, final DataCenterVO dc, final String domain, final ServiceOfferingVO serviceOffering, long startEventId, HypervisorType hyperType) throws ResourceAllocationException {
        if (serviceOffering.getGuestIpType() == NetworkOffering.GuestIpType.DirectDual || serviceOffering.getGuestIpType() == NetworkOffering.GuestIpType.DirectSingle) {
            return null;
        }
        final long dcId = dc.getId();
        String sourceNat = null;

        final long accountId = account.getId();

        Transaction txn = Transaction.currentTxn();
        try {
            final EventVO event = new EventVO();
            event.setUserId(1L); // system user performed the action...
            event.setAccountId(account.getId());
            event.setType(EventTypes.EVENT_NET_IP_ASSIGN);

            txn.start();

            account = _accountDao.acquireInLockTable(accountId);
            if (account == null) {
                s_logger.warn("Unable to lock account " + accountId);
                return null;
            }
            if(s_logger.isDebugEnabled()) {
                s_logger.debug("lock account " + accountId + " is acquired");
            }

            boolean isAccountIP = false;
            List<IPAddressVO> addrs = listPublicIpAddressesInVirtualNetwork(account.getId(), dcId, true);            
            if (addrs.size() == 0) {

                // Check that the maximum number of public IPs for the given accountId will not be exceeded
                if (_accountMgr.resourceLimitExceeded(account, ResourceType.public_ip)) {
                    ResourceAllocationException rae = new ResourceAllocationException("Maximum number of public IP addresses for account: " + account.getAccountName() + " has been exceeded.");
                    rae.setResourceType("ip");
                    throw rae;
                }

                //check for account specific IP pool.
                addrs = listPublicIpAddressesInVirtualNetwork(account.getId(), dcId, null);
                if (addrs.size() == 0){

                    if (s_logger.isDebugEnabled()) {
                        s_logger.debug("assigning a new ip address");
                    }                
                    Pair<String, VlanVO> ipAndVlan = _vlanDao.assignIpAddress(dc.getId(), accountId, account.getDomainId(), VlanType.VirtualNetwork, true);

                    if (ipAndVlan != null) {
                        sourceNat = ipAndVlan.first();

                        // Increment the number of public IPs for this accountId in the database
                        _accountMgr.incrementResourceCount(accountId, ResourceType.public_ip);
                        event.setParameters("address=" + sourceNat + "\nsourceNat=true\ndcId="+dcId);
                        event.setDescription("Acquired a public ip: " + sourceNat);
                        _eventDao.persist(event);
                    }
                }else{ 
                    isAccountIP = true;
                    sourceNat = addrs.get(0).getAddress();
                    _ipAddressDao.setIpAsSourceNat(sourceNat);
                    s_logger.debug("assigning a new ip address " +sourceNat);

                    // Increment the number of public IPs for this accountId in the database
                    _accountMgr.incrementResourceCount(accountId, ResourceType.public_ip);
                    event.setParameters("address=" + sourceNat + "\nsourceNat=true\ndcId="+dcId);
                    event.setDescription("Acquired a public ip: " + sourceNat);
                    _eventDao.persist(event);
                }

            } else {
                sourceNat = addrs.get(0).getAddress();
            }

            if (sourceNat == null) {
                txn.rollback();
                event.setLevel(EventVO.LEVEL_ERROR);
                event.setParameters("dcId=" + dcId);
                event.setDescription("Failed to acquire a public ip.");
                _eventDao.persist(event);
                s_logger.error("Unable to get source nat ip address for account " + account.getId());
                return null;
            }

            UserStatisticsVO stats = _userStatsDao.findBy(account.getId(), dcId);
            if (stats == null) {
                stats = new UserStatisticsVO(account.getId(), dcId);
                _userStatsDao.persist(stats);
            }

            txn.commit();

            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Source Nat is " + sourceNat);
            }

            DomainRouterVO router = null;
            try {
                router = createRouter(account.getId(), sourceNat, dcId, domain, serviceOffering, startEventId);
            } catch (final Exception e) {
                s_logger.error("Unable to create router for " + account.getAccountName(), e);
            }

            if (router != null) {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Router is " + router.getHostName());
                }
                return sourceNat;
            }

            s_logger.warn("releasing the source nat because router was not created: " + sourceNat);
            txn.start();
            if(isAccountIP){
                _ipAddressDao.unassignIpAsSourceNat(sourceNat);
            }else{
                _ipAddressDao.unassignIpAddress(sourceNat);
            }

            _accountMgr.decrementResourceCount(accountId, ResourceType.public_ip);
            EventVO event2 = new EventVO();
            event2.setUserId(1L);
            event2.setAccountId(account.getId());
            event2.setType(EventTypes.EVENT_NET_IP_RELEASE);
            event2.setParameters("address=" + sourceNat + "\nsourceNat=true");
            event2.setDescription("released source nat ip " + sourceNat + " since router could not be started");
            _eventDao.persist(event2);
            txn.commit();
            return null;
        } finally {
            if (account != null) {
                if(s_logger.isDebugEnabled()) {
                    s_logger.debug("Releasing lock account " + accountId);
                }

                _accountDao.releaseFromLockTable(accountId);
            }
        }
    }

    @Override @DB
    public String assignSourceNatIpAddress(Account account, DataCenter dc) throws InsufficientAddressCapacityException {
        final long dcId = dc.getId();
        final long accountId = account.getId();
        String sourceNat = null;


        Transaction txn = Transaction.currentTxn();
        try {
            final EventVO event = new EventVO();
            event.setUserId(1L); // system user performed the action...
            event.setAccountId(account.getId());
            event.setType(EventTypes.EVENT_NET_IP_ASSIGN);

            txn.start();

            account = _accountDao.acquireInLockTable(accountId);
            if (account == null) {
                s_logger.warn("Unable to lock account " + accountId);
                return null;
            }
            if(s_logger.isDebugEnabled()) {
                s_logger.debug("lock account " + accountId + " is acquired");
            }

            boolean isAccountIP = false;
            List<IPAddressVO> addrs = listPublicIpAddressesInVirtualNetwork(account.getId(), dcId, true);            
            if (addrs.size() == 0) {

                // Check that the maximum number of public IPs for the given accountId will not be exceeded
                if (_accountMgr.resourceLimitExceeded(account, ResourceType.public_ip)) {
                    throw new AccountLimitException("Maximum number of public IP addresses for account: " + account.getAccountName() + " has been exceeded.");
                }

                //check for account specific IP pool.
                addrs = listPublicIpAddressesInVirtualNetwork(account.getId(), dcId, null);
                if (addrs.size() == 0){

                    if (s_logger.isDebugEnabled()) {
                        s_logger.debug("assigning a new ip address");
                    }                
                    Pair<String, VlanVO> ipAndVlan = _vlanDao.assignIpAddress(dc.getId(), accountId, account.getDomainId(), VlanType.VirtualNetwork, true);

                    if (ipAndVlan != null) {
                        sourceNat = ipAndVlan.first();

                        // Increment the number of public IPs for this accountId in the database
                        _accountMgr.incrementResourceCount(accountId, ResourceType.public_ip);
                        event.setParameters("address=" + sourceNat + "\nsourceNat=true\ndcId="+dcId);
                        event.setDescription("Acquired a public ip: " + sourceNat);
                        _eventDao.persist(event);
                    }
                }else{ 
                    isAccountIP = true;
                    sourceNat = addrs.get(0).getAddress();
                    _ipAddressDao.setIpAsSourceNat(sourceNat);
                    s_logger.debug("assigning a new ip address " +sourceNat);

                    // Increment the number of public IPs for this accountId in the database
                    _accountMgr.incrementResourceCount(accountId, ResourceType.public_ip);
                    event.setParameters("address=" + sourceNat + "\nsourceNat=true\ndcId="+dcId);
                    event.setDescription("Acquired a public ip: " + sourceNat);
                    _eventDao.persist(event);
                }

            } else {
                sourceNat = addrs.get(0).getAddress();
            }

            if (sourceNat == null) {
                txn.rollback();
                event.setLevel(EventVO.LEVEL_ERROR);
                event.setParameters("dcId=" + dcId);
                event.setDescription("Failed to acquire a public ip.");
                _eventDao.persist(event);
                s_logger.error("Unable to get source nat ip address for account " + account.getId());
                return null;
            }

            UserStatisticsVO stats = _userStatsDao.findBy(account.getId(), dcId);
            if (stats == null) {
                stats = new UserStatisticsVO(account.getId(), dcId);
                _userStatsDao.persist(stats);
            }

            txn.commit();

            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Source Nat is " + sourceNat);
            }

            return sourceNat;

        } finally {
            if (account != null) {
                if(s_logger.isDebugEnabled()) {
                    s_logger.debug("Releasing lock account " + accountId);
                }

                _accountDao.releaseFromLockTable(accountId);
            }
        }
    }

    @Override
    @DB
    public DomainRouterVO createDhcpServerForDirectlyAttachedGuests(long userId, long accountId, DataCenterVO dc, HostPodVO pod, Long candidateHost, VlanVO guestVlan) throws ConcurrentOperationException{
        return _routerMgr.createDhcpServerForDirectlyAttachedGuests(userId, accountId, dc, pod, candidateHost, guestVlan);
    }

    @Override
    public boolean releaseRouter(final long routerId) {
        return destroyRouter(routerId);
    }

    @Override @DB
    public DomainRouterVO createRouter(final long accountId, final String publicIpAddress, final long dataCenterId,  
            String domain, final ServiceOfferingVO offering, long startEventId) 
    throws ConcurrentOperationException {
        return _routerMgr.createRouter(accountId, publicIpAddress, dataCenterId, domain, offering, startEventId);
    }

    @Override
    public boolean destroyRouter(final long routerId) {
        return _routerMgr.destroyRouter(routerId);
    }

    @Override
    public boolean savePasswordToRouter(final long routerId, final String vmIpAddress, final String password) {
        return _routerMgr.savePasswordToRouter(routerId, vmIpAddress, password);
    }

    @Override
    public DomainRouterVO startRouter(final long routerId, long eventId) {
        return _routerMgr.startRouter(routerId, eventId);
    }

    @Override
    public boolean stopRouter(final long routerId, long eventId) {
        return _routerMgr.stopRouter(routerId, eventId);
    }


    @Override
    public boolean getRouterStatistics(final long vmId, final Map<String, long[]> netStats, final Map<String, long[]> diskStats) {
        return _routerMgr.getRouterStatistics(vmId, netStats, diskStats);
    }


    @Override
    public boolean rebootRouter(final long routerId, long startEventId) {
        return _routerMgr.rebootRouter(routerId, startEventId);
    }

    @Override
    public boolean associateIP(final DomainRouterVO router, final List<String> ipAddrList, final boolean add, long vmId) {
        Commands cmds = new Commands(OnError.Continue);
        boolean sourceNat = false;
        Map<VlanVO, ArrayList<IPAddressVO>> vlanIpMap = new HashMap<VlanVO, ArrayList<IPAddressVO>>();
        for (final String ipAddress: ipAddrList) {
            IPAddressVO ip = _ipAddressDao.findById(ipAddress);

            VlanVO vlan = _vlanDao.findById(ip.getVlanDbId());
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
                sourceNat = ip.getSourceNat();
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

    @Override @DB
    public IPAddressVO associateIP(AssociateIPAddrCmd cmd) throws ResourceAllocationException, InsufficientAddressCapacityException, ConcurrentOperationException  {
        String accountName = cmd.getAccountName();
        Long domainId = cmd.getDomainId();
        Long zoneId = cmd.getZoneId();
        Account account = UserContext.current().getAccount();
        Long userId = UserContext.current().getUserId();
        Long accountId = null;

        if ((account == null) || isAdmin(account.getType())) {
            if (domainId != null) {
                if ((account != null) && !_domainDao.isChildDomain(account.getDomainId(), domainId)) {
                    throw new PermissionDeniedException("Invalid domain id (" + domainId + ") given, unable to associate IP address, permission denied");
                }
                if (accountName != null) {
                    Account userAccount = _accountDao.findActiveAccount(accountName, domainId);
                    if (userAccount != null) {
                        accountId = userAccount.getId();
                    } else {
                        throw new PermissionDeniedException("Unable to find account " + accountName + " in domain " + domainId + ", permission denied");
                    }
                }
            } else if (account != null) {
                // the admin is acquiring an IP address
                accountId = account.getId();
                domainId = account.getDomainId();
            } else {
                throw new InvalidParameterValueException("Account information is not specified.");
            }
        } else {
            accountId = account.getId();
            domainId = account.getDomainId();
        }

        if (userId == null) {
            userId = Long.valueOf(1);
        }


        Transaction txn = Transaction.currentTxn();
        Account accountToLock = null;
        try {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Associate IP address called for user " + userId + " account " + accountId);
            }
            accountToLock = _accountDao.acquireInLockTable(accountId);

            if (accountToLock == null) {
                s_logger.warn("Unable to lock account: " + accountId);
                throw new ConcurrentOperationException("Unable to acquire account lock");
            }

            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Associate IP address lock acquired");
            }

            // Check that the maximum number of public IPs for the given
            // accountId will not be exceeded
            if (_accountMgr.resourceLimitExceeded(accountToLock, ResourceType.public_ip)) {
                ResourceAllocationException rae = new ResourceAllocationException("Maximum number of public IP addresses for account: " + accountToLock.getAccountName()
                        + " has been exceeded.");
                rae.setResourceType("ip");
                throw rae;
            }

            DomainRouterVO router = _routerMgr.getRouter(accountId, zoneId);
            if (router == null) {
                throw new InvalidParameterValueException("No router found for account: " + accountToLock.getAccountName() + ". Please create a VM before acquiring an IP");
            }

            txn.start();

            String ipAddress = null;
            Pair<String, VlanVO> ipAndVlan = _vlanDao.assignIpAddress(zoneId, accountId, domainId, VlanType.VirtualNetwork, false);

            if (ipAndVlan == null) {
                throw new InsufficientAddressCapacityException("Unable to find available public IP addresses", DataCenter.class, zoneId);
            } else {
                ipAddress = ipAndVlan.first();
                _accountMgr.incrementResourceCount(accountId, ResourceType.public_ip);
            }

            boolean success = true;
            String errorMsg = "";

            List<String> ipAddrs = new ArrayList<String>();
            ipAddrs.add(ipAddress);

            if (router.getState() == State.Running) {
                success = associateIP(router, ipAddress, true, 0L);
                if (!success) {
                    errorMsg = "Unable to assign public IP address.";
                }
            }

            EventVO event = new EventVO();
            event.setUserId(userId);
            event.setAccountId(accountId);
            event.setType(EventTypes.EVENT_NET_IP_ASSIGN);
            event.setParameters("address=" + ipAddress + "\nsourceNat=" + false + "\ndcId=" + zoneId);

            if (!success) {
                _ipAddressDao.unassignIpAddress(ipAddress);
                ipAddress = null;
                _accountMgr.decrementResourceCount(accountId, ResourceType.public_ip);

                event.setLevel(EventVO.LEVEL_ERROR);
                event.setDescription(errorMsg);
                _eventDao.persist(event);
                txn.commit();

                throw new CloudRuntimeException(errorMsg);
            } else {
                event.setDescription("Assigned a public IP address: " + ipAddress);
                _eventDao.persist(event);
            }

            txn.commit();
            IPAddressVO ip = _ipAddressDao.findById(ipAddress);

            return ip;
        } catch (ResourceAllocationException rae) {
            s_logger.error("Associate IP threw a ResourceAllocationException.", rae);
            throw rae;
        } catch (InsufficientAddressCapacityException iace) {
            s_logger.error("Associate IP threw an InsufficientAddressCapacityException.", iace);
            throw iace;
        } catch (InvalidParameterValueException ipve) {
            s_logger.error("Associate IP threw an InvalidParameterValueException.", ipve);
            throw ipve;
        } finally {
            if (account != null) {
                _accountDao.releaseFromLockTable(accountId);
                s_logger.debug("Associate IP address lock released");
            }
        }

    }

    @Override
    public boolean associateIP(final DomainRouterVO router, final String ipAddress, final boolean add, long vmId) {
        Commands cmds = new Commands(OnError.Continue);
        IPAddressVO ip = _ipAddressDao.findById(ipAddress);
        VlanVO vlan = _vlanDao.findById(ip.getVlanDbId());
        boolean sourceNat = ip.isSourceNat();
        boolean firstIP = (!sourceNat && (_ipAddressDao.countIPs(vlan.getDataCenterId(), router.getAccountId(), vlan.getVlanId(), vlan.getVlanGateway(), vlan.getVlanNetmask()) == 1));
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

        IPAssocCommand cmd = new IPAssocCommand(router.getInstanceName(), router.getPrivateIpAddress(), ip.getAddress(), add, firstIP, sourceNat, vlanId, vlanGateway, vlanNetmask, vifMacAddress, vmGuestAddress);
        cmds.addCommand(cmd);
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

        if (answers.length != 1) {
            return false;
        }
        return  answers[0].getResult();        
    }

    @Override
    public boolean updateFirewallRule(final FirewallRuleVO rule, String oldPrivateIP, String oldPrivatePort) {

        final IPAddressVO ipVO = _ipAddressDao.findById(rule.getPublicIpAddress());
        if (ipVO == null || ipVO.getAllocated() == null) {
            return false;
        }

        final DomainRouterVO router = _routerMgr.getRouter(ipVO.getAccountId(), ipVO.getDataCenterId());
        Long hostId = router.getHostId();
        if (router == null || router.getHostId() == null) {
            return true;
        }

        if (rule.isForwarding()) {
            return updatePortForwardingRule(rule, router, hostId, oldPrivateIP, oldPrivatePort);
        } else if (rule.getGroupId() != null) {
            final List<FirewallRuleVO> fwRules = _rulesDao.listIPForwardingForLB(ipVO.getAccountId(), ipVO.getDataCenterId());

            return updateLoadBalancerRules(fwRules, router, hostId);
        }
        return true;
    }

    @Override
    public List<FirewallRuleVO> updateFirewallRules(final String publicIpAddress, final List<FirewallRuleVO> fwRules, final DomainRouterVO router) {
        final List<FirewallRuleVO> result = new ArrayList<FirewallRuleVO>();
        if (fwRules.size() == 0) {
            return result;
        }

        if (router == null || router.getHostId() == null) {
            return fwRules;
        } else {
            final HostVO host = _hostDao.findById(router.getHostId());
            return updateFirewallRules(host, router.getInstanceName(), router.getPrivateIpAddress(), fwRules);
        }
    }

    public List<FirewallRuleVO> updateFirewallRules(final HostVO host, final String routerName, final String routerIp, final List<FirewallRuleVO> fwRules) {
        final List<FirewallRuleVO> result = new ArrayList<FirewallRuleVO>();
        if (fwRules.size() == 0) {
            s_logger.debug("There are no firewall rules");
            return result;
        }

        Commands cmds = new Commands(OnError.Continue);
        final List<FirewallRuleVO> lbRules = new ArrayList<FirewallRuleVO>();
        final List<FirewallRuleVO> fwdRules = new ArrayList<FirewallRuleVO>();

        int i=0;
        for (FirewallRuleVO rule : fwRules) {
            // Determine the VLAN ID and netmask of the rule's public IP address
            IPAddressVO ip = _ipAddressDao.findById(rule.getPublicIpAddress());
            VlanVO vlan = _vlanDao.findById(new Long(ip.getVlanDbId()));
            String vlanNetmask = vlan.getVlanNetmask();
            rule.setVlanNetmask(vlanNetmask);

            if (rule.isForwarding()) {
                fwdRules.add(rule);
                final SetFirewallRuleCommand cmd = new SetFirewallRuleCommand(routerName, routerIp, rule, true);
                cmds.addCommand(cmd);
            } else if (rule.getGroupId() != null){
                lbRules.add(rule);
            }

        }
        if (lbRules.size() > 0) { //at least one load balancer rule
            final LoadBalancerConfigurator cfgrtr = new HAProxyConfigurator();
            final String [] cfg = cfgrtr.generateConfiguration(fwRules);
            final String [][] addRemoveRules = cfgrtr.generateFwRules(fwRules);
            final LoadBalancerCfgCommand cmd = new LoadBalancerCfgCommand(cfg, addRemoveRules, routerName, routerIp);
            cmds.addCommand(cmd);
        }
        if (cmds.size() == 0) {
            return result;
        }
        Answer [] answers = null;
        try {
            answers = _agentMgr.send(host.getId(), cmds);
        } catch (final AgentUnavailableException e) {
            s_logger.warn("agent unavailable", e);
        } catch (final OperationTimedoutException e) {
            s_logger.warn("Timed Out", e);
        }
        if (answers == null ){
            return result;
        }
        i=0;
        for (final FirewallRuleVO rule:fwdRules){
            final Answer ans = answers[i++];
            if (ans != null) {
                if (ans.getResult()) {
                    result.add(rule);
                } else {
                    s_logger.warn("Unable to update firewall rule: " + rule.toString());
                }
            }
        }
        if (i == (answers.length-1)) {
            final Answer lbAnswer = answers[i];
            if (lbAnswer.getResult()) {
                result.addAll(lbRules);
            } else {
                s_logger.warn("Unable to update lb rules.");
            }
        }
        return result;
    }

    private boolean updatePortForwardingRule(final FirewallRuleVO rule, final DomainRouterVO router, Long hostId, String oldPrivateIP, String oldPrivatePort) {
        IPAddressVO ip = _ipAddressDao.findById(rule.getPublicIpAddress());
        VlanVO vlan = _vlanDao.findById(new Long(ip.getVlanDbId()));
        rule.setVlanNetmask(vlan.getVlanNetmask());

        final SetFirewallRuleCommand cmd = new SetFirewallRuleCommand(router.getInstanceName(), router.getPrivateIpAddress(), rule, oldPrivateIP, oldPrivatePort);
        final Answer ans = _agentMgr.easySend(hostId, cmd);
        if (ans == null) {
            return false;
        } else {
            return ans.getResult();
        }
    }

    @Override
    public List<FirewallRuleVO>  updatePortForwardingRules(final List<FirewallRuleVO> fwRules, final DomainRouterVO router, Long hostId ){
        final List<FirewallRuleVO> fwdRules = new ArrayList<FirewallRuleVO>();
        final List<FirewallRuleVO> result = new ArrayList<FirewallRuleVO>();

        if (fwRules.size() == 0) {
            return result;
        }

        Commands cmds = new Commands(OnError.Continue);
        int i=0;
        for (final FirewallRuleVO rule: fwRules) {
            IPAddressVO ip = _ipAddressDao.findById(rule.getPublicIpAddress());
            VlanVO vlan = _vlanDao.findById(new Long(ip.getVlanDbId()));
            String vlanNetmask = vlan.getVlanNetmask();
            rule.setVlanNetmask(vlanNetmask);
            if (rule.isForwarding()) {
                fwdRules.add(rule);
                final SetFirewallRuleCommand cmd = new SetFirewallRuleCommand(router.getInstanceName(), router.getPrivateIpAddress(),rule, false);
                cmds.addCommand(cmd);
            }
        }
        try {
            _agentMgr.send(hostId, cmds);
        } catch (final AgentUnavailableException e) {
            s_logger.warn("agent unavailable", e);
        } catch (final OperationTimedoutException e) {
            s_logger.warn("Timed Out", e);
        }
        Answer[] answers = cmds.getAnswers();
        if (answers == null ){
            return result;
        }
        i=0;
        for (final FirewallRuleVO rule:fwdRules){
            final Answer ans = answers[i++];
            if (ans != null) {
                if (ans.getResult()) {
                    result.add(rule);
                }
            }
        }
        return result;
    }

    @Override
    public FirewallRuleVO createPortForwardingRule(CreatePortForwardingRuleCmd cmd) throws InvalidParameterValueException, PermissionDeniedException, NetworkRuleConflictException {
        // validate IP Address exists
        IPAddressVO ipAddress = _ipAddressDao.findById(cmd.getIpAddress());
        if (ipAddress == null) {
            throw new InvalidParameterValueException("Unable to create port forwarding rule on address " + ipAddress + ", invalid IP address specified.");
        }

        // validate user VM exists
        UserVmVO userVM = _vmDao.findById(cmd.getVirtualMachineId());
        if (userVM == null) {
            throw new InvalidParameterValueException("Unable to create port forwarding rule on address " + ipAddress + ", invalid virtual machine id specified (" + cmd.getVirtualMachineId() + ").");
        }

        // validate that IP address and userVM belong to the same account
        if ((ipAddress.getAccountId() == null) || (ipAddress.getAccountId().longValue() != userVM.getAccountId())) {
            throw new InvalidParameterValueException("Unable to create port forwarding rule, IP address " + ipAddress + " owner is not the same as owner of virtual machine " + userVM.toString()); 
        }

        // validate that userVM is in the same availability zone as the IP address
        if (ipAddress.getDataCenterId() != userVM.getDataCenterId()) {
            throw new InvalidParameterValueException("Unable to create port forwarding rule, IP address " + ipAddress + " is not in the same availability zone as virtual machine " + userVM.toString());
        }

        // if an admin account was passed in, or no account was passed in, make sure we honor the accountName/domainId parameters
        Account account = UserContext.current().getAccount();
        if (account != null) {
            if ((account.getType() == Account.ACCOUNT_TYPE_ADMIN) || (account.getType() == Account.ACCOUNT_TYPE_DOMAIN_ADMIN)) {
                if (!_domainDao.isChildDomain(account.getDomainId(), userVM.getDomainId())) {
                    throw new PermissionDeniedException("Unable to create port forwarding rule, IP address " + ipAddress + " to virtual machine " + cmd.getVirtualMachineId() + ", permission denied.");
                }
            } else if (account.getId() != userVM.getAccountId()) {
                throw new PermissionDeniedException("Unable to create port forwarding rule, IP address " + ipAddress + " to virtual machine " + cmd.getVirtualMachineId() + ", permission denied.");
            }
        }

        // set up some local variables
        String protocol = cmd.getProtocol();
        String publicPort = cmd.getPublicPort();
        String privatePort = cmd.getPrivatePort();

        // sanity check that the vm can be applied to the load balancer
        ServiceOfferingVO offering = _serviceOfferingDao.findById(userVM.getServiceOfferingId());
        if ((offering == null) || !GuestIpType.Virtualized.equals(offering.getGuestIpType())) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Unable to create port forwarding rule (" + protocol + ":" + publicPort + "->" + privatePort + ") for virtual machine " + userVM.toString() + ", bad network type (" + ((offering == null) ? "null" : offering.getGuestIpType()) + ")");
            }

            throw new IllegalArgumentException("Unable to create port forwarding rule (" + protocol + ":" + publicPort + "->" + privatePort + ") for virtual machine " + userVM.toString() + ", bad network type (" + ((offering == null) ? "null" : offering.getGuestIpType()) + ")");
        }

        // check for ip address/port conflicts by checking existing forwarding and load balancing rules
        List<FirewallRuleVO> existingRulesOnPubIp = _rulesDao.listIPForwarding(ipAddress.getAddress());

        // FIXME:  The mapped ports should be String, String, List<String> since more than one proto can be mapped...
        Map<String, Ternary<String, String, List<String>>> mappedPublicPorts = new HashMap<String, Ternary<String, String, List<String>>>();

        if (existingRulesOnPubIp != null) {
            for (FirewallRuleVO fwRule : existingRulesOnPubIp) {
                Ternary<String, String, List<String>> portMappings = mappedPublicPorts.get(fwRule.getPublicPort());
                List<String> protocolList = null;
                if (portMappings == null) {
                    protocolList = new ArrayList<String>();
                } else {
                    protocolList = portMappings.third();
                }
                protocolList.add(fwRule.getProtocol());
                mappedPublicPorts.put(fwRule.getPublicPort(), new Ternary<String, String, List<String>>(fwRule.getPrivateIpAddress(), fwRule.getPrivatePort(), protocolList));
            }
        }

        Ternary<String, String, List<String>> privateIpPort = mappedPublicPorts.get(publicPort);
        if (privateIpPort != null) {
            if (privateIpPort.first().equals(userVM.getGuestIpAddress()) && privateIpPort.second().equals(privatePort)) {
                List<String> protocolList = privateIpPort.third();
                for (String mappedProtocol : protocolList) {
                    if (mappedProtocol.equalsIgnoreCase(protocol)) {
                        if (s_logger.isDebugEnabled()) {
                            s_logger.debug("skipping the creating of firewall rule " + ipAddress + ":" + publicPort + " to " + userVM.getGuestIpAddress() + ":" + privatePort + "; rule already exists.");
                        }
                        // already mapped
                        throw new NetworkRuleConflictException("An existing port forwarding service rule for " + ipAddress + ":" + publicPort
                                + " already exists, found while trying to create mapping to " + userVM.getGuestIpAddress() + ":" + privatePort + ".");
                    }
                }
            } else {
                // FIXME:  Will we need to refactor this for both assign port forwarding service and create port forwarding rule?
                //                throw new NetworkRuleConflictException("An existing port forwarding service rule for " + ipAddress + ":" + publicPort
                //                        + " already exists, found while trying to create mapping to " + userVM.getGuestIpAddress() + ":" + privatePort + ((securityGroupId == null) ? "." : " from port forwarding service "
                //                        + securityGroupId.toString() + "."));
                throw new NetworkRuleConflictException("An existing port forwarding service rule for " + ipAddress + ":" + publicPort
                        + " already exists, found while trying to create mapping to " + userVM.getGuestIpAddress() + ":" + privatePort + ".");
            }
        }

        FirewallRuleVO newFwRule = new FirewallRuleVO();
        newFwRule.setEnabled(true);
        newFwRule.setForwarding(true);
        newFwRule.setPrivatePort(privatePort);
        newFwRule.setProtocol(protocol);
        newFwRule.setPublicPort(publicPort);
        newFwRule.setPublicIpAddress(ipAddress.getAddress());
        newFwRule.setPrivateIpAddress(userVM.getGuestIpAddress());
        //        newFwRule.setGroupId(securityGroupId);
        newFwRule.setGroupId(null);

        // In 1.0 the rules were always persisted when a user created a rule.  When the rules get sent down
        // the stopOnError parameter is set to false, so the agent will apply all rules that it can.  That
        // behavior is preserved here by persisting the rule before sending it to the agent.
        _rulesDao.persist(newFwRule);

        boolean success = updateFirewallRule(newFwRule, null, null);

        // Save and create the event
        String description;
        String ruleName = "ip forwarding";
        String level = EventVO.LEVEL_INFO;

        if (success == true) {
            description = "created new " + ruleName + " rule [" + newFwRule.getPublicIpAddress() + ":" + newFwRule.getPublicPort() + "]->["
            + newFwRule.getPrivateIpAddress() + ":" + newFwRule.getPrivatePort() + "]" + " " + newFwRule.getProtocol();
        } else {
            level = EventVO.LEVEL_ERROR;
            description = "failed to create new " + ruleName + " rule [" + newFwRule.getPublicIpAddress() + ":" + newFwRule.getPublicPort() + "]->["
            + newFwRule.getPrivateIpAddress() + ":" + newFwRule.getPrivatePort() + "]" + " " + newFwRule.getProtocol();
        }

        EventUtils.saveEvent(UserContext.current().getUserId(), userVM.getAccountId(), level, EventTypes.EVENT_NET_RULE_ADD, description);

        return newFwRule;
    }

    @Override
    public List<FirewallRuleVO> listPortForwardingRules(ListPortForwardingRulesCmd cmd) throws InvalidParameterValueException, PermissionDeniedException {
        String ipAddress = cmd.getIpAddress();
        Account account = UserContext.current().getAccount();

        IPAddressVO ipAddressVO = _ipAddressDao.findById(ipAddress);
        if (ipAddressVO == null) {
            throw new InvalidParameterValueException("Unable to find IP address " + ipAddress);
        }

        Account addrOwner = _accountDao.findById(ipAddressVO.getAccountId());

        // if an admin account was passed in, or no account was passed in, make sure we honor the accountName/domainId parameters
        if ((account != null) && isAdmin(account.getType())) {
            if (ipAddressVO.getAccountId() != null) {
                if ((addrOwner != null) && !_domainDao.isChildDomain(account.getDomainId(), addrOwner.getDomainId())) {
                    throw new PermissionDeniedException("Unable to list port forwarding rules for address " + ipAddress + ", permission denied for account " + account.getId());
                }
            } 
        } else {
            if (account != null) {
                if ((ipAddressVO.getAccountId() == null) || (account.getId() != ipAddressVO.getAccountId().longValue())) {
                    throw new PermissionDeniedException("Unable to list port forwarding rules for address " + ipAddress + ", permission denied for account " + account.getId());
                }
            }
        }

        return _rulesDao.listIPForwarding(cmd.getIpAddress(), true);
    }

    @Override @DB
    public boolean assignToLoadBalancer(AssignToLoadBalancerRuleCmd cmd)  throws NetworkRuleConflictException {
        Long loadBalancerId = cmd.getLoadBalancerId();
        Long instanceIdParam = cmd.getVirtualMachineId();
        List<Long> instanceIds = cmd.getVirtualMachineIds();

        if ((instanceIdParam == null) && (instanceIds == null)) {
            throw new InvalidParameterValueException("Unable to assign to load balancer " + loadBalancerId + ", no instance id is specified.");
        }

        if ((instanceIds == null) && (instanceIdParam != null)) {
            instanceIds = new ArrayList<Long>();
            instanceIds.add(instanceIdParam);
        }

        // FIXME:  We should probably lock the load balancer here to prevent multiple updates...
        LoadBalancerVO loadBalancer = _loadBalancerDao.findById(loadBalancerId);
        if (loadBalancer == null) {
            throw new InvalidParameterValueException("Failed to assign to load balancer " + loadBalancerId + ", the load balancer was not found.");
        }


        // Permission check...
        Account account = UserContext.current().getAccount();
        if (account != null) {
            if (account.getType() == Account.ACCOUNT_TYPE_DOMAIN_ADMIN) {
                if (!_domainDao.isChildDomain(account.getDomainId(), loadBalancer.getDomainId())) {
                    throw new PermissionDeniedException("Failed to assign to load balancer " + loadBalancerId + ", permission denied.");
                }
            } else if (account.getType() != Account.ACCOUNT_TYPE_ADMIN && account.getId() != loadBalancer.getAccountId()) {
                throw new PermissionDeniedException("Failed to assign to load balancer " + loadBalancerId + ", permission denied.");
            }
        }

        Transaction txn = Transaction.currentTxn();
        List<FirewallRuleVO> firewallRulesToApply = new ArrayList<FirewallRuleVO>();
        long accountId = 0;
        DomainRouterVO router = null;

        List<LoadBalancerVMMapVO> mappedInstances = _loadBalancerVMMapDao.listByLoadBalancerId(loadBalancerId, false);
        Set<Long> mappedInstanceIds = new HashSet<Long>();
        if (mappedInstances != null) {
            for (LoadBalancerVMMapVO mappedInstance : mappedInstances) {
                mappedInstanceIds.add(Long.valueOf(mappedInstance.getInstanceId()));
            }
        }

        List<Long> finalInstanceIds = new ArrayList<Long>();
        for (Long instanceId : instanceIds) {
            if (mappedInstanceIds.contains(instanceId)) {
                continue;
            } else {
                finalInstanceIds.add(instanceId);
            }

            UserVmVO userVm = _vmDao.findById(instanceId);
            if (userVm == null) {
                s_logger.warn("Unable to find virtual machine with id " + instanceId);
                throw new InvalidParameterValueException("Unable to find virtual machine with id " + instanceId);
            } else {
                // sanity check that the vm can be applied to the load balancer
                ServiceOfferingVO offering = _serviceOfferingDao.findById(userVm.getServiceOfferingId());
                if ((offering == null) || !GuestIpType.Virtualized.equals(offering.getGuestIpType())) {
                    // we previously added these instanceIds to the loadBalancerVMMap, so remove them here as we are rejecting the API request
                    // without actually modifying the load balancer
                    _loadBalancerVMMapDao.remove(loadBalancerId, instanceIds, Boolean.TRUE);

                    if (s_logger.isDebugEnabled()) {
                        s_logger.debug("Unable to add virtual machine " + userVm.toString() + " to load balancer " + loadBalancerId + ", bad network type (" + ((offering == null) ? "null" : offering.getGuestIpType()) + ")");
                    }

                    throw new InvalidParameterValueException("Unable to add virtual machine " + userVm.toString() + " to load balancer " + loadBalancerId + ", bad network type (" + ((offering == null) ? "null" : offering.getGuestIpType()) + ")");
                }
            }

            if (accountId == 0) {
                accountId = userVm.getAccountId();
            } else if (accountId != userVm.getAccountId()) {
                s_logger.warn("guest vm " + userVm.getHostName() + " (id:" + userVm.getId() + ") belongs to account " + userVm.getAccountId()
                        + ", previous vm in list belongs to account " + accountId);
                throw new InvalidParameterValueException("guest vm " + userVm.getHostName() + " (id:" + userVm.getId() + ") belongs to account " + userVm.getAccountId()
                        + ", previous vm in list belongs to account " + accountId);
            }

            DomainRouterVO nextRouter = null;
            if (userVm.getDomainRouterId() != null) {
                nextRouter = _routerMgr.getRouter(userVm.getDomainRouterId());
            }
            if (nextRouter == null) {
                s_logger.warn("Unable to find router (" + userVm.getDomainRouterId() + ") for virtual machine with id " + instanceId);
                throw new InvalidParameterValueException("Unable to find router (" + userVm.getDomainRouterId() + ") for virtual machine with id " + instanceId);
            }

            if (router == null) {
                router = nextRouter;

                // Make sure owner of router is owner of load balancer.  Since we are already checking that all VMs belong to the same router, by checking router
                // ownership once we'll make sure all VMs belong to the owner of the load balancer.
                if (router.getAccountId() != loadBalancer.getAccountId()) {
                    throw new InvalidParameterValueException("guest vm " + userVm.getHostName() + " (id:" + userVm.getId() + ") does not belong to the owner of load balancer " +
                            loadBalancer.getName() + " (owner is account id " + loadBalancer.getAccountId() + ")");
                }
            } else if (router.getId() != nextRouter.getId()) {
                throw new InvalidParameterValueException("guest vm " + userVm.getHostName() + " (id:" + userVm.getId() + ") belongs to router " + nextRouter.getHostName()
                        + ", previous vm in list belongs to router " + router.getHostName());
            }

            // check for ip address/port conflicts by checking exising forwarding and loadbalancing rules
            String ipAddress = loadBalancer.getIpAddress();
            String privateIpAddress = userVm.getGuestIpAddress();
            List<FirewallRuleVO> existingRulesOnPubIp = _rulesDao.listIPForwarding(ipAddress);

            if (existingRulesOnPubIp != null) {
                for (FirewallRuleVO fwRule : existingRulesOnPubIp) {
                    if (!(  (fwRule.isForwarding() == false) &&
                            (fwRule.getGroupId() != null) &&
                            (fwRule.getGroupId() == loadBalancer.getId())  )) {
                        // if the rule is not for the current load balancer, check to see if the private IP is our target IP,
                        // in which case we have a conflict
                        if (fwRule.getPublicPort().equals(loadBalancer.getPublicPort())) {
                            throw new NetworkRuleConflictException("An existing port forwarding service rule for " + ipAddress + ":" + loadBalancer.getPublicPort()
                                    + " exists, found while trying to apply load balancer " + loadBalancer.getName() + " (id:" + loadBalancer.getId() + ") to instance "
                                    + userVm.getHostName() + ".");
                        }
                    } else if (fwRule.getPrivateIpAddress().equals(privateIpAddress) && fwRule.getPrivatePort().equals(loadBalancer.getPrivatePort()) && fwRule.isEnabled()) {
                        // for the current load balancer, don't add the same instance to the load balancer more than once
                        continue;
                    }
                }
            }

            FirewallRuleVO newFwRule = new FirewallRuleVO();
            newFwRule.setAlgorithm(loadBalancer.getAlgorithm());
            newFwRule.setEnabled(true);
            newFwRule.setForwarding(false);
            newFwRule.setPrivatePort(loadBalancer.getPrivatePort());
            newFwRule.setPublicPort(loadBalancer.getPublicPort());
            newFwRule.setPublicIpAddress(loadBalancer.getIpAddress());
            newFwRule.setPrivateIpAddress(userVm.getGuestIpAddress());
            newFwRule.setGroupId(loadBalancer.getId());

            firewallRulesToApply.add(newFwRule);
        }

        // if there's no work to do, bail out early rather than reconfiguring the proxy with the existing rules
        if (firewallRulesToApply.isEmpty()) {
            return true;
        }

        //Sync on domR
        if(router == null){
            throw new InvalidParameterValueException("Failed to assign to load balancer " + loadBalancerId + ", the domain router was not found at " + loadBalancer.getIpAddress());
        }
        else{
            cmd.synchronizeCommand("Router", router.getId());
        }

        IPAddressVO ipAddr = _ipAddressDao.findById(loadBalancer.getIpAddress());
        List<IPAddressVO> ipAddrs = listPublicIpAddressesInVirtualNetwork(accountId, ipAddr.getDataCenterId(), null);
        for (IPAddressVO ipv : ipAddrs) {
            List<FirewallRuleVO> rules = _rulesDao.listIpForwardingRulesForLoadBalancers(ipv.getAddress());
            firewallRulesToApply.addAll(rules);
        }

        txn.start();

        List<FirewallRuleVO> updatedRules = null;
        if (router.getState().equals(State.Starting)) {
            // Starting is a special case...if the router is starting that means the IP address hasn't yet been assigned to the domR and the update firewall rules script will fail.
            // In this case, just store the rules and they will be applied when the router state is resent (after the router is started).
            updatedRules = firewallRulesToApply;
        } else {
            updatedRules = updateFirewallRules(loadBalancer.getIpAddress(), firewallRulesToApply, router);
        }

        // Save and create the event
        String description;
        String type = EventTypes.EVENT_NET_RULE_ADD;
        String ruleName = "load balancer";
        String level = EventVO.LEVEL_INFO;

        LoadBalancerVO loadBalancerLock = null;
        try {
            loadBalancerLock = _loadBalancerDao.acquireInLockTable(loadBalancerId);
            if (loadBalancerLock == null) {
                s_logger.warn("assignToLoadBalancer: Failed to lock load balancer " + loadBalancerId + ", proceeding with updating loadBalancerVMMappings...");
            }
            if ((updatedRules != null) && (updatedRules.size() == firewallRulesToApply.size())) {
                // flag the instances as mapped to the load balancer
                for (Long addedInstanceId : finalInstanceIds) {
                    LoadBalancerVMMapVO mappedVM = new LoadBalancerVMMapVO(loadBalancerId, addedInstanceId);
                    _loadBalancerVMMapDao.persist(mappedVM);
                }

                /* We used to add these instances as pending when the API command is received on the server, and once they were applied,
                 * the pending status was removed.  In the 2.2 API framework, this is no longer done and instead the new mappings just
                 * need to be persisted
                List<LoadBalancerVMMapVO> pendingMappedVMs = _loadBalancerVMMapDao.listByLoadBalancerId(loadBalancerId, true);
                for (LoadBalancerVMMapVO pendingMappedVM : pendingMappedVMs) {
                    if (instanceIds.contains(pendingMappedVM.getInstanceId())) {
                        LoadBalancerVMMapVO pendingMappedVMForUpdate = _loadBalancerVMMapDao.createForUpdate();
                        pendingMappedVMForUpdate.setPending(false);
                        _loadBalancerVMMapDao.update(pendingMappedVM.getId(), pendingMappedVMForUpdate);
                    }
                }
                 */

                for (FirewallRuleVO updatedRule : updatedRules) {
                    _rulesDao.persist(updatedRule);

                    description = "created new " + ruleName + " rule [" + updatedRule.getPublicIpAddress() + ":"
                    + updatedRule.getPublicPort() + "]->[" + updatedRule.getPrivateIpAddress() + ":"
                    + updatedRule.getPrivatePort() + "]" + " " + updatedRule.getProtocol();

                    EventUtils.saveEvent(UserContext.current().getUserId(), loadBalancer.getAccountId(), level, type, description);
                }
                txn.commit();
                return true;
            } else {
                // Remove the instanceIds from the load balancer since there was a failure.  Make sure to commit the
                // transaction here, otherwise the act of throwing the internal error exception will cause this
                // remove operation to be rolled back.
                _loadBalancerVMMapDao.remove(loadBalancerId, instanceIds, null);
                txn.commit();

                s_logger.warn("Failed to apply load balancer " + loadBalancer.getName() + " (id:" + loadBalancerId + ") to guest virtual machines " + StringUtils.join(instanceIds, ","));
                throw new CloudRuntimeException("Failed to apply load balancer " + loadBalancer.getName() + " (id:" + loadBalancerId + ") to guest virtual machine " + StringUtils.join(instanceIds, ","));
            }
        } finally {
            if (loadBalancerLock != null) {
                _loadBalancerDao.releaseFromLockTable(loadBalancerId);
            }
        }
    }

    @Override @DB
    public LoadBalancer createLoadBalancerRule(CreateLoadBalancerRuleCmd cmd) throws InvalidParameterValueException, PermissionDeniedException {
        String publicIp = cmd.getPublicIp();

        // make sure ip address exists
        IPAddressVO ipAddr = _ipAddressDao.findById(cmd.getPublicIp());
        if (ipAddr == null) {
            throw new InvalidParameterValueException("Unable to create load balancer rule, invalid IP address " + publicIp);
        }

        VlanVO vlan = _vlanDao.findById(ipAddr.getVlanDbId());
        if (vlan != null) {
            if (!VlanType.VirtualNetwork.equals(vlan.getVlanType())) {
                throw new InvalidParameterValueException("Unable to create load balancer rule for IP address " + publicIp + ", only VirtualNetwork type IP addresses can be used for load balancers.");
            }
        } // else ERROR?

        // Verify input parameters
        if ((ipAddr.getAccountId() == null) || (ipAddr.getAllocated() == null)) {
            throw new InvalidParameterValueException("Unable to create load balancer rule, cannot find account owner for ip " + publicIp);
        }

        Account account = UserContext.current().getAccount();
        if (account != null) {
            if ((account.getType() == Account.ACCOUNT_TYPE_ADMIN) || (account.getType() == Account.ACCOUNT_TYPE_DOMAIN_ADMIN)) {
                if (!_domainDao.isChildDomain(account.getDomainId(), ipAddr.getDomainId())) {
                    throw new PermissionDeniedException("Unable to create load balancer rule on IP address " + publicIp + ", permission denied.");
                }
            } else if (account.getId() != ipAddr.getAccountId().longValue()) {
                throw new PermissionDeniedException("Unable to create load balancer rule, account " + account.getAccountName() + " doesn't own ip address " + publicIp);
            }
        }

        String loadBalancerName = cmd.getLoadBalancerRuleName();
        LoadBalancerVO existingLB = _loadBalancerDao.findByAccountAndName(ipAddr.getAccountId(), loadBalancerName);
        if (existingLB != null) {
            throw new InvalidParameterValueException("Unable to create load balancer rule, an existing load balancer rule with name " + loadBalancerName + " already exists.");
        }

        // validate params
        String publicPort = cmd.getPublicPort();
        String privatePort = cmd.getPrivatePort();
        String algorithm = cmd.getAlgorithm();

        if (!NetUtils.isValidPort(publicPort)) {
            throw new InvalidParameterValueException("publicPort is an invalid value");
        }
        if (!NetUtils.isValidPort(privatePort)) {
            throw new InvalidParameterValueException("privatePort is an invalid value");
        }
        if ((algorithm == null) || !NetUtils.isValidAlgorithm(algorithm)) {
            throw new InvalidParameterValueException("Invalid algorithm");
        }

        boolean locked = false;
        try {
            LoadBalancerVO exitingLB = _loadBalancerDao.findByIpAddressAndPublicPort(publicIp, publicPort);
            if (exitingLB != null) {
                throw new InvalidParameterValueException("IP Address/public port already load balanced by an existing load balancer rule");
            }

            List<FirewallRuleVO> existingFwRules = _rulesDao.listIPForwarding(publicIp, publicPort, true);
            if ((existingFwRules != null) && !existingFwRules.isEmpty()) {
                throw new InvalidParameterValueException("IP Address (" + publicIp + ") and port (" + publicPort + ") already in use");
            }

            ipAddr = _ipAddressDao.acquireInLockTable(publicIp);
            if (ipAddr == null) {
                throw new PermissionDeniedException("User does not own ip address " + publicIp);
            }

            locked = true;

            LoadBalancerVO loadBalancer = new LoadBalancerVO(loadBalancerName, cmd.getDescription(), ipAddr.getAccountId(), publicIp, publicPort, privatePort, algorithm);
            loadBalancer = _loadBalancerDao.persist(loadBalancer);
            Long id = loadBalancer.getId();

            // Save off information for the event that the security group was applied
            Long userId = UserContext.current().getUserId();
            if (userId == null) {
                userId = Long.valueOf(User.UID_SYSTEM);
            }

            EventVO event = new EventVO();
            event.setUserId(userId);
            event.setAccountId(ipAddr.getAccountId());
            event.setType(EventTypes.EVENT_LOAD_BALANCER_CREATE);

            if (id == null) {
                event.setDescription("Failed to create load balancer " + loadBalancer.getName() + " on ip address " + publicIp + "[" + publicPort + "->" + privatePort + "]");
                event.setLevel(EventVO.LEVEL_ERROR);
            } else {
                event.setDescription("Successfully created load balancer " + loadBalancer.getName() + " on ip address " + publicIp + "[" + publicPort + "->" + privatePort + "]");
                String params = "id="+loadBalancer.getId()+"\ndcId="+ipAddr.getDataCenterId();
                event.setParameters(params);
                event.setLevel(EventVO.LEVEL_INFO);
            }
            _eventDao.persist(event);

            return _loadBalancerDao.findById(id);
        } finally {
            if (locked) {
                _ipAddressDao.releaseFromLockTable(publicIp);
            }
        }
    }

    @Override @DB
    public boolean releasePublicIpAddress(long userId, final String ipAddress) {
        IPAddressVO ip = null;
        try {
            ip = _ipAddressDao.acquireInLockTable(ipAddress);

            if (ip == null) {
                s_logger.warn("Unable to find allocated ip: " + ipAddress);
                return false;
            }

            if(s_logger.isDebugEnabled()) {
                s_logger.debug("lock on ip " + ipAddress + " is acquired");
            }

            if (ip.getAllocated() == null) {
                s_logger.warn("ip: " + ipAddress + " is already released");
                return false;
            }

            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Releasing ip " + ipAddress + "; sourceNat = " + ip.isSourceNat());
            }

            final List<String> ipAddrs = new ArrayList<String>();
            ipAddrs.add(ip.getAddress());
            final List<FirewallRuleVO> firewallRules = _rulesDao.listIPForwardingForUpdate(ipAddress);

            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Found firewall rules: " + firewallRules.size());
            }

            for (final FirewallRuleVO fw: firewallRules) {
                fw.setEnabled(false);
            }

            DomainRouterVO router = null;
            if (ip.isSourceNat()) {
                router = _routerMgr.getRouter(ipAddress);
                if (router != null) {
                    if (router.getPublicIpAddress() != null) {
                        return false;
                    }
                }
            } else {
                router = _routerMgr.getRouter(ip.getAccountId(), ip.getDataCenterId());
            }

            // Now send the updates  down to the domR (note: we still hold locks on address and firewall)
            updateFirewallRules(ipAddress, firewallRules, router);

            for (final FirewallRuleVO rule: firewallRules) {
                _rulesDao.remove(rule.getId());

                // Save and create the event
                String ruleName = (rule.isForwarding() ? "ip forwarding" : "load balancer");
                String description = "deleted " + ruleName + " rule [" + rule.getPublicIpAddress() + ":" + rule.getPublicPort()
                + "]->[" + rule.getPrivateIpAddress() + ":" + rule.getPrivatePort() + "]" + " "
                + rule.getProtocol();

                // save off an event for removing the network rule
                EventVO event = new EventVO();
                event.setUserId(userId);
                event.setAccountId(ip.getAccountId());
                event.setType(EventTypes.EVENT_NET_RULE_DELETE);
                event.setDescription(description);
                event.setLevel(EventVO.LEVEL_INFO);
                _eventDao.persist(event);
            }

            List<LoadBalancerVO> loadBalancers = _loadBalancerDao.listByIpAddress(ipAddress);
            for (LoadBalancerVO loadBalancer : loadBalancers) {
                _loadBalancerDao.remove(loadBalancer.getId());

                // save off an event for removing the load balancer
                EventVO event = new EventVO();
                event.setUserId(userId);
                event.setAccountId(ip.getAccountId());
                event.setType(EventTypes.EVENT_LOAD_BALANCER_DELETE);
                String params = "id="+loadBalancer.getId();
                event.setParameters(params);
                event.setDescription("Successfully deleted load balancer " + loadBalancer.getId());
                event.setLevel(EventVO.LEVEL_INFO);
                _eventDao.persist(event);
            }

            if ((router != null) && (router.getState() == State.Running)) {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Disassociate ip " + router.getHostName());
                }

                if (associateIP(router, ip.getAddress(), false, 0)) {
                    _ipAddressDao.unassignIpAddress(ipAddress);
                } else {
                    if (s_logger.isDebugEnabled()) {
                        s_logger.debug("Unable to dissociate IP : " + ipAddress + " due to failing to dissociate with router: " + router.getHostName());
                    }

                    final EventVO event = new EventVO();
                    event.setUserId(userId);
                    event.setAccountId(ip.getAccountId());
                    event.setType(EventTypes.EVENT_NET_IP_RELEASE);
                    event.setLevel(EventVO.LEVEL_ERROR);
                    event.setParameters("address=" + ipAddress + "\nsourceNat="+ip.isSourceNat());
                    event.setDescription("failed to released a public ip: " + ipAddress + " due to failure to disassociate with router " + router.getHostName());
                    _eventDao.persist(event);

                    return false;
                }
            } else {
                _ipAddressDao.unassignIpAddress(ipAddress);
            }
            s_logger.debug("released a public ip: " + ipAddress);
            final EventVO event = new EventVO();
            event.setUserId(userId);
            event.setAccountId(ip.getAccountId());
            event.setType(EventTypes.EVENT_NET_IP_RELEASE);
            event.setParameters("address=" + ipAddress + "\nsourceNat="+ip.isSourceNat());
            event.setDescription("released a public ip: " + ipAddress);
            _eventDao.persist(event);

            return true;
        } catch (final Throwable e) {
            s_logger.warn("ManagementServer error", e);
            return false;
        } finally {
            if(ip != null) {
                if(s_logger.isDebugEnabled()) {
                    s_logger.debug("Releasing lock on ip " + ipAddress);
                }
                _ipAddressDao.releaseFromLockTable(ipAddress);
            }
        }
    }

    @Override
    public DomainRouterVO getRouter(final long routerId) {
        return _routerMgr.getRouter(routerId);
    }

    @Override
    public List<? extends VirtualRouter> getRouters(final long hostId) {
        return _routerMgr.getRouters(hostId);
    }

    @Override
    public boolean updateLoadBalancerRules(final List<FirewallRuleVO> fwRules, final DomainRouterVO router, Long hostId) {

        for (FirewallRuleVO rule : fwRules) {
            // Determine the the VLAN ID and netmask of the rule's public IP address
            IPAddressVO ip = _ipAddressDao.findById(rule.getPublicIpAddress());
            VlanVO vlan = _vlanDao.findById(new Long(ip.getVlanDbId()));
            String vlanNetmask = vlan.getVlanNetmask();

            rule.setVlanNetmask(vlanNetmask);
        }

        final LoadBalancerConfigurator cfgrtr = new HAProxyConfigurator();
        final String [] cfg = cfgrtr.generateConfiguration(fwRules);
        final String [][] addRemoveRules = cfgrtr.generateFwRules(fwRules);
        final LoadBalancerCfgCommand cmd = new LoadBalancerCfgCommand(cfg, addRemoveRules, router.getInstanceName(), router.getPrivateIpAddress());
        final Answer ans = _agentMgr.easySend(hostId, cmd);
        if (ans == null) {
            return false;
        } else {
            return ans.getResult();
        }
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

        NetworkOfferingVO defaultGuestNetworkOffering = new NetworkOfferingVO(NetworkOffering.DefaultVirtualizedNetworkOffering, "Virtual Vlan", TrafficType.Guest, GuestIpType.Virtualized, false, false, rateMbps, multicastRateMbps, null, false, true);
        defaultGuestNetworkOffering = _networkOfferingDao.persistDefaultNetworkOffering(defaultGuestNetworkOffering);
        NetworkOfferingVO defaultGuestDirectNetworkOffering = new NetworkOfferingVO(NetworkOffering.DefaultDirectNetworkOffering, "Direct", TrafficType.Guest, GuestIpType.DirectSingle, false, false, rateMbps, multicastRateMbps, null, false, true);
        defaultGuestNetworkOffering = _networkOfferingDao.persistDefaultNetworkOffering(defaultGuestDirectNetworkOffering);

        AccountsUsingNetworkConfigurationSearch = _accountDao.createSearchBuilder();
        SearchBuilder<NetworkAccountVO> networkAccountSearch = _networkConfigDao.createSearchBuilderForAccount();
        AccountsUsingNetworkConfigurationSearch.join("nc", networkAccountSearch, AccountsUsingNetworkConfigurationSearch.entity().getId(), networkAccountSearch.entity().getAccountId(), JoinType.INNER);
        networkAccountSearch.and("config", networkAccountSearch.entity().getNetworkId(), SearchCriteria.Op.EQ);
        networkAccountSearch.and("owner", networkAccountSearch.entity().isOwner(), SearchCriteria.Op.EQ);
        AccountsUsingNetworkConfigurationSearch.done();

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
    public boolean addDhcpEntry(final long routerHostId, final String routerIp, String vmName, String vmMac, String vmIp) {
        final DhcpEntryCommand dhcpEntry = new DhcpEntryCommand(vmMac, vmIp, routerIp, vmName);


        final Answer answer = _agentMgr.easySend(routerHostId, dhcpEntry);
        return (answer != null && answer.getResult());
    }

    @Override
    public DomainRouterVO addVirtualMachineToGuestNetwork(UserVmVO vm, String password, long startEventId) throws ConcurrentOperationException {
        return _routerMgr.addVirtualMachineToGuestNetwork(vm, password, startEventId);
    }

    public void releaseVirtualMachineFromGuestNetwork(UserVmVO vm) {
    }

    @Override
    public String createZoneVlan(DomainRouterVO router) {
        return _routerMgr.createZoneVlan(router);
    }

    @Override
    public List<IPAddressVO> listPublicIpAddressesInVirtualNetwork(long accountId, long dcId, Boolean sourceNat) {
        SearchBuilder<IPAddressVO> ipAddressSB = _ipAddressDao.createSearchBuilder();
        ipAddressSB.and("accountId", ipAddressSB.entity().getAccountId(), SearchCriteria.Op.EQ);
        ipAddressSB.and("dataCenterId", ipAddressSB.entity().getDataCenterId(), SearchCriteria.Op.EQ);
        if (sourceNat != null) {
            ipAddressSB.and("sourceNat", ipAddressSB.entity().isSourceNat(), SearchCriteria.Op.EQ);
        }

        SearchBuilder<VlanVO> virtualNetworkVlanSB = _vlanDao.createSearchBuilder();
        virtualNetworkVlanSB.and("vlanType", virtualNetworkVlanSB.entity().getVlanType(), SearchCriteria.Op.EQ);
        ipAddressSB.join("virtualNetworkVlanSB", virtualNetworkVlanSB, ipAddressSB.entity().getVlanDbId(), virtualNetworkVlanSB.entity().getId(), JoinBuilder.JoinType.INNER);

        SearchCriteria<IPAddressVO> ipAddressSC = ipAddressSB.create();
        ipAddressSC.setParameters("accountId", accountId);
        ipAddressSC.setParameters("dataCenterId", dcId);
        if (sourceNat != null) {
            ipAddressSC.setParameters("sourceNat", sourceNat);
        }
        ipAddressSC.setJoinParameters("virtualNetworkVlanSB", "vlanType", VlanType.VirtualNetwork);

        return _ipAddressDao.search(ipAddressSC, null);
    }

    @Override
    public List<NetworkVO> setupNetworkConfiguration(Account owner, NetworkOfferingVO offering, DeploymentPlan plan, String name, String displayText) {
        return setupNetworkConfiguration(owner, offering, null, plan, name, displayText);
    }

    @Override
    public List<NetworkVO> setupNetworkConfiguration(Account owner, NetworkOfferingVO offering, Network predefined, DeploymentPlan plan, String name, String displayText) {
        List<NetworkVO> configs = _networkConfigDao.listBy(owner.getId(), offering.getId(), plan.getDataCenterId());
        if (configs.size() > 0) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Found existing network configuration for offering " + offering + ": " + configs.get(0));
            }
            return configs;
        }

        configs = new ArrayList<NetworkVO>();

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
                    configs.add(_networkConfigDao.findById(config.getId()));
                }
                continue;
            }

            long id = _networkConfigDao.getNextInSequence(Long.class, "id");
            if (related == -1) {
                related = id;
            } 

            NetworkVO vo = new NetworkVO(id, config, offering.getId(), plan.getDataCenterId(), guru.getName(), owner.getDomainId(), owner.getId(), related, name, displayText);
            configs.add(_networkConfigDao.persist(vo));
        }

        if (configs.size() < 1) {
            throw new CloudRuntimeException("Unable to convert network offering to network profile: " + offering.getId());
        }

        return configs;
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
    public void allocate(VirtualMachineProfile<? extends VMInstanceVO> vm, List<Pair<NetworkVO, NicProfile>> networks) throws InsufficientCapacityException {
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

        if (defaultNic == null && nics.size() > 2) {
            throw new IllegalArgumentException("Default Nic was not set.");
        } else if (nics.size() == 1) {
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
    protected Pair<NetworkGuru, NetworkVO> implementNetworkConfiguration(long configId, DeployDestination dest, ReservationContext context) throws ConcurrentOperationException, ResourceUnavailableException, InsufficientAddressCapacityException {
        Transaction.currentTxn();
        Pair<NetworkGuru, NetworkVO> implemented = new Pair<NetworkGuru, NetworkVO>(null, null);

        NetworkVO config = _networkConfigDao.acquireInLockTable(configId);
        if (config == null) {
            throw new ConcurrentOperationException("Unable to acquire network configuration: " + configId);
        }

        try {
            NetworkGuru guru = _networkGurus.get(config.getGuruName());
            if (config.getState() == Network.State.Implemented || config.getState() == Network.State.Setup) {
                implemented.set(guru, config);
                return implemented;
            }

            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Asking " + guru + " to implement " + config);
            }

            NetworkOfferingVO offering = _networkOfferingDao.findById(config.getNetworkOfferingId());

            Network result = guru.implement(config, offering, dest, context);
            config.setCidr(result.getCidr());
            config.setBroadcastUri(result.getBroadcastUri());
            config.setGateway(result.getGateway());
            config.setDns1(result.getDns1());
            config.setDns2(result.getDns2());
            config.setMode(result.getMode());
            config.setState(Network.State.Implemented);
            _networkConfigDao.update(configId, config);

            for (NetworkElement element : _networkElements) {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Asking " + element.getName() + " to implmenet " + config);
                }
                try {
                    element.implement(config, offering, dest, context);
                } catch (InsufficientCapacityException e) {
                    throw new ResourceUnavailableException("Unable to start domain router for this VM", e);
                }
            }

            implemented.set(guru, config);
            return implemented;
        } finally {
            if (implemented.first() == null) {
                s_logger.debug("Cleaning up because we're unable to implement network " + config);
            }
            _networkConfigDao.releaseFromLockTable(configId);
        }
    }

    @Override
    public void prepare(VirtualMachineProfile<? extends VMInstanceVO> vmProfile, DeployDestination dest, ReservationContext context) throws InsufficientNetworkCapacityException, ConcurrentOperationException, ResourceUnavailableException {
        List<NicVO> nics = _nicDao.listBy(vmProfile.getId());
        for (NicVO nic : nics) {
            Pair<NetworkGuru, NetworkVO> implemented = implementNetworkConfiguration(nic.getNetworkId(), dest, context);
            NetworkGuru concierge = implemented.first();
            NetworkVO config = implemented.second();
            NicProfile profile = null;
            if (nic.getReservationStrategy() == ReservationStrategy.Start) {
                nic.setState(Resource.State.Reserving);
                _nicDao.update(nic.getId(), nic);
                URI broadcastUri = nic.getBroadcastUri();
                if (broadcastUri == null) {
                    config.getBroadcastUri();
                }

                URI isolationUri = nic.getIsolationUri();

                profile = new NicProfile(nic, config, broadcastUri, isolationUri);
                concierge.reserve(profile, config, vmProfile, dest, context);
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
                for (NetworkElement element : _networkElements) {
                    if (s_logger.isDebugEnabled()) {
                        s_logger.debug("Asking " + element.getName() + " to prepare for " + nic);
                    }
                    element.prepare(config, profile, vmProfile, dest, context);
                }
            } else {
                profile = new NicProfile(nic, config, nic.getBroadcastUri(), nic.getIsolationUri());
            }

            vmProfile.addNic(profile);
        }
    }

    @Override
    public void release(VirtualMachineProfile<? extends VMInstanceVO> vmProfile) {
        List<NicVO> nics = _nicDao.listBy(vmProfile.getId());
        for (NicVO nic : nics) {
            NetworkVO config = _networkConfigDao.findById(nic.getNetworkId());
            if (nic.getReservationStrategy() == ReservationStrategy.Start) {
                NetworkGuru concierge = _networkGurus.get(config.getGuruName());
                nic.setState(Resource.State.Releasing);
                _nicDao.update(nic.getId(), nic);
                NicProfile profile = new NicProfile(nic, config, null, null);
                if (!concierge.release(profile, vmProfile, nic.getReservationId())) {
                    nic.setState(Resource.State.Allocated);
                    _nicDao.update(nic.getId(), nic);
                }
            }
        }
    }

    @Override
    public List<? extends Nic> getNics(VirtualMachine vm) {
        return _nicDao.listBy(vm.getId());
    }

    @Override @DB
    public boolean removeFromLoadBalancer(RemoveFromLoadBalancerRuleCmd cmd) throws InvalidParameterValueException {

        Long userId = UserContext.current().getUserId();
        Account account = UserContext.current().getAccount();
        Long loadBalancerId = cmd.getId();
        Long vmInstanceId = cmd.getVirtualMachineId();
        List<Long> instanceIds = cmd.getVirtualMachineIds();

        if ((vmInstanceId == null) && (instanceIds == null)) {
            throw new ServerApiException(BaseCmd.PARAM_ERROR, "No virtual machine id specified.");
        }

        // if a single instanceId was given, add it to the list so we can always just process the list if instanceIds
        if (instanceIds == null) {
            instanceIds = new ArrayList<Long>();
            instanceIds.add(vmInstanceId);
        }

        if (userId == null) {
            userId = Long.valueOf(1);
        }

        LoadBalancerVO loadBalancer = _loadBalancerDao.findById(Long.valueOf(loadBalancerId));

        if (loadBalancer == null) {
            throw new ServerApiException(BaseCmd.PARAM_ERROR, "Unable to find load balancer rule with id " + loadBalancerId);
        } else if (account != null) {
            if (!isAdmin(account.getType()) && (loadBalancer.getAccountId() != account.getId())) {
                throw new ServerApiException(BaseCmd.PARAM_ERROR, "Account " + account.getAccountName() + " does not own load balancer rule " + loadBalancer.getName() +
                        " (id:" + loadBalancer.getId() + ")");
            } else if (!_domainDao.isChildDomain(account.getDomainId(), loadBalancer.getDomainId())) {
                throw new ServerApiException(BaseCmd.PARAM_ERROR, "Invalid load balancer rule id (" + loadBalancer.getId() + ") given, unable to remove virtual machine instances.");
            }
        }

        Transaction txn = Transaction.currentTxn();
        LoadBalancerVO loadBalancerLock = null;
        boolean success = true;
        try {

            IPAddressVO ipAddress = _ipAddressDao.findById(loadBalancer.getIpAddress());
            if (ipAddress == null) {
                return false;
            }

            DomainRouterVO router = _routerMgr.getRouter(ipAddress.getAccountId(), ipAddress.getDataCenterId());
            if (router == null) {
                return false;
            }

            txn.start();
            for (Long instanceId : instanceIds) {
                UserVm userVm = _userVmDao.findById(instanceId);
                if (userVm == null) {
                    s_logger.warn("Unable to find virtual machine with id " + instanceId);
                    throw new InvalidParameterValueException("Unable to find virtual machine with id " + instanceId);
                }
                FirewallRuleVO fwRule = _rulesDao.findByGroupAndPrivateIp(loadBalancerId, userVm.getGuestIpAddress(), false);
                if (fwRule != null) {
                    fwRule.setEnabled(false);
                    _rulesDao.update(fwRule.getId(), fwRule);
                }
            }

            List<FirewallRuleVO> allLbRules = new ArrayList<FirewallRuleVO>();
            IPAddressVO ipAddr = _ipAddressDao.findById(loadBalancer.getIpAddress());
            List<IPAddressVO> ipAddrs = listPublicIpAddressesInVirtualNetwork(loadBalancer.getAccountId(), ipAddr.getDataCenterId(), null);
            for (IPAddressVO ipv : ipAddrs) {
                List<FirewallRuleVO> rules = _rulesDao.listIPForwarding(ipv.getAddress(), false);
                allLbRules.addAll(rules);
            }

            updateFirewallRules(loadBalancer.getIpAddress(), allLbRules, router);

            // firewall rules are updated, lock the load balancer as mappings are updated
            loadBalancerLock = _loadBalancerDao.acquireInLockTable(loadBalancerId);
            if (loadBalancerLock == null) {
                s_logger.warn("removeFromLoadBalancer: failed to lock load balancer " + loadBalancerId + ", deleting mappings anyway...");
            }

            // remove all the loadBalancer->VM mappings
            _loadBalancerVMMapDao.remove(loadBalancerId, instanceIds, Boolean.FALSE);

            // Save and create the event
            String description;
            String type = EventTypes.EVENT_NET_RULE_DELETE;
            String level = EventVO.LEVEL_INFO;

            for (FirewallRuleVO updatedRule : allLbRules) {
                if (!updatedRule.isEnabled()) {
                    _rulesDao.remove(updatedRule.getId());

                    description = "deleted load balancer rule [" + updatedRule.getPublicIpAddress() + ":" + updatedRule.getPublicPort() + "]->["
                    + updatedRule.getPrivateIpAddress() + ":" + updatedRule.getPrivatePort() + "]" + " " + updatedRule.getProtocol();

                    EventUtils.saveEvent(userId, loadBalancer.getAccountId(), level, type, description);
                }
            }
            txn.commit();
        } catch (Exception ex) {
            s_logger.warn("Failed to delete load balancing rule with exception: ", ex);
            success = false;
            txn.rollback();
        } finally {
            if (loadBalancerLock != null) {
                _loadBalancerDao.releaseFromLockTable(loadBalancerId);
            }
        }
        return success;
    }

    @Override @DB
    public boolean deleteLoadBalancerRule(DeleteLoadBalancerRuleCmd cmd) throws InvalidParameterValueException, PermissionDeniedException{
        Long loadBalancerId = cmd.getId();
        Long userId = UserContext.current().getUserId();
        Account account = UserContext.current().getAccount();

        ///verify input parameters
        LoadBalancerVO loadBalancer = _loadBalancerDao.findById(loadBalancerId);
        if (loadBalancer == null) {
            throw new InvalidParameterValueException ("Unable to find load balancer rule with id " + loadBalancerId);
        }

        if (account != null) {
            if (!isAdmin(account.getType())) {
                if (loadBalancer.getAccountId() != account.getId()) {
                    throw new PermissionDeniedException("Account " + account.getAccountName() + " does not own load balancer rule " + loadBalancer.getName() + " (id:" + loadBalancerId + "), permission denied");
                }
            } else if (!_domainDao.isChildDomain(account.getDomainId(), loadBalancer.getDomainId())) {
                throw new PermissionDeniedException("Unable to delete load balancer rule " + loadBalancer.getName() + " (id:" + loadBalancerId + "), permission denied.");
            }
        }

        if (userId == null) {
            userId = Long.valueOf(1);
        }

        Transaction txn = Transaction.currentTxn();
        LoadBalancerVO loadBalancerLock = null;
        try {

            IPAddressVO ipAddress = _ipAddressDao.findById(loadBalancer.getIpAddress());
            if (ipAddress == null) {
                return false;
            }

            DomainRouterVO router = _routerMgr.getRouter(ipAddress.getAccountId(), ipAddress.getDataCenterId());
            List<FirewallRuleVO> fwRules = _firewallRulesDao.listByLoadBalancerId(loadBalancerId);

            txn.start();

            if ((fwRules != null) && !fwRules.isEmpty()) {
                for (FirewallRuleVO fwRule : fwRules) {
                    fwRule.setEnabled(false);
                    _firewallRulesDao.update(fwRule.getId(), fwRule);
                }

                List<FirewallRuleVO> allLbRules = new ArrayList<FirewallRuleVO>();
                List<IPAddressVO> ipAddrs = listPublicIpAddressesInVirtualNetwork(loadBalancer.getAccountId(), ipAddress.getDataCenterId(), null);
                for (IPAddressVO ipv : ipAddrs) {
                    List<FirewallRuleVO> rules = _firewallRulesDao.listIPForwarding(ipv.getAddress(), false);
                    allLbRules.addAll(rules);
                }

                updateFirewallRules(loadBalancer.getIpAddress(), allLbRules, router);

                // firewall rules are updated, lock the load balancer as the mappings are updated
                loadBalancerLock = _loadBalancerDao.acquireInLockTable(loadBalancerId);
                if (loadBalancerLock == null) {
                    s_logger.warn("deleteLoadBalancer: failed to lock load balancer " + loadBalancerId + ", deleting mappings anyway...");
                }

                // remove all loadBalancer->VM mappings
                List<LoadBalancerVMMapVO> lbVmMap = _loadBalancerVMMapDao.listByLoadBalancerId(loadBalancerId);
                if (lbVmMap != null && !lbVmMap.isEmpty()) {
                    for (LoadBalancerVMMapVO lb : lbVmMap) {
                        _loadBalancerVMMapDao.remove(lb.getId());
                    }
                }

                // Save and create the event
                String description;
                String type = EventTypes.EVENT_NET_RULE_DELETE;
                String ruleName = "load balancer";
                String level = EventVO.LEVEL_INFO;
                Account accountOwner = _accountDao.findById(loadBalancer.getAccountId());

                for (FirewallRuleVO updatedRule : fwRules) {
                    _firewallRulesDao.remove(updatedRule.getId());

                    description = "deleted " + ruleName + " rule [" + updatedRule.getPublicIpAddress() + ":" + updatedRule.getPublicPort() + "]->["
                    + updatedRule.getPrivateIpAddress() + ":" + updatedRule.getPrivatePort() + "]" + " " + updatedRule.getProtocol();

                    EventUtils.saveEvent(userId, accountOwner.getId(), level, type, description);
                }
            }

            txn.commit();
        } catch (Exception ex) {
            txn.rollback();
            s_logger.error("Unexpected exception deleting load balancer " + loadBalancerId, ex);
            return false;
        } finally {
            if (loadBalancerLock != null) {
                _loadBalancerDao.releaseFromLockTable(loadBalancerId);
            }
        }

        boolean success = _loadBalancerDao.remove(loadBalancerId);

        // save off an event for removing the load balancer
        EventVO event = new EventVO();
        event.setUserId(userId);
        event.setAccountId(loadBalancer.getAccountId());
        event.setType(EventTypes.EVENT_LOAD_BALANCER_DELETE);
        if (success) {
            event.setLevel(EventVO.LEVEL_INFO);
            String params = "id="+loadBalancer.getId();
            event.setParameters(params);
            event.setDescription("Successfully deleted load balancer " + loadBalancer.getName() + " (id:" + loadBalancer.getId() + ")");
        } else {
            event.setLevel(EventVO.LEVEL_ERROR);
            event.setDescription("Failed to delete load balancer " + loadBalancer.getName() + " (id:" + loadBalancer.getId() + ")");
        }
        _eventDao.persist(event);
        return success;
    }


    @Override @DB
    public LoadBalancerVO updateLoadBalancerRule(UpdateLoadBalancerRuleCmd cmd) throws InvalidParameterValueException, PermissionDeniedException{
        Long loadBalancerId = cmd.getId();
        String privatePort = cmd.getPrivatePort();
        String algorithm = cmd.getAlgorithm();
        String name = cmd.getLoadBalancerName();
        String description = cmd.getDescription();
        Account account = UserContext.current().getAccount();

        //Verify input parameters
        LoadBalancerVO loadBalancer = _loadBalancerDao.findById(loadBalancerId);
        if (loadBalancer == null) {
            throw new InvalidParameterValueException("Unable to find load balancer rule " + loadBalancerId + " for update.");
        }

        // make sure the name's not already in use
        if (name != null) {
            LoadBalancerVO existingLB = _loadBalancerDao.findByAccountAndName(loadBalancer.getAccountId(), name);
            if ((existingLB != null) && (existingLB.getId() != loadBalancer.getId())) {
                throw new InvalidParameterValueException("Unable to update load balancer " + loadBalancer.getName() + " with new name " + name + ", the name is already in use.");
            }
        }

        Account lbOwner = _accountDao.findById(loadBalancer.getAccountId());
        if (lbOwner == null) {
            throw new InvalidParameterValueException("Unable to update load balancer rule, cannot find owning account");
        }

        Long accountId = lbOwner.getId();
        if (account != null) {
            if (!isAdmin(account.getType())) {
                if (account.getId() != accountId.longValue()) {
                    throw new PermissionDeniedException("Unable to update load balancer rule, permission denied");
                }
            } else if (!_domainDao.isChildDomain(account.getDomainId(), lbOwner.getDomainId())) {
                throw new PermissionDeniedException("Unable to update load balancer rule, permission denied.");
            }
        }

        String updatedPrivatePort = ((privatePort == null) ? loadBalancer.getPrivatePort() : privatePort);
        String updatedAlgorithm = ((algorithm == null) ? loadBalancer.getAlgorithm() : algorithm);
        String updatedName = ((name == null) ? loadBalancer.getName() : name);
        String updatedDescription = ((description == null) ? loadBalancer.getDescription() : description);

        Transaction txn = Transaction.currentTxn();
        try {
            txn.start();
            loadBalancer.setPrivatePort(updatedPrivatePort);
            loadBalancer.setAlgorithm(updatedAlgorithm);
            loadBalancer.setName(updatedName);
            loadBalancer.setDescription(updatedDescription);
            _loadBalancerDao.update(loadBalancer.getId(), loadBalancer);

            List<FirewallRuleVO> fwRules = _firewallRulesDao.listByLoadBalancerId(loadBalancer.getId());
            if ((fwRules != null) && !fwRules.isEmpty()) {
                for (FirewallRuleVO fwRule : fwRules) {
                    fwRule.setPrivatePort(updatedPrivatePort);
                    fwRule.setAlgorithm(updatedAlgorithm);
                    _firewallRulesDao.update(fwRule.getId(), fwRule);
                }
            }
            txn.commit();
        } catch (RuntimeException ex) {
            s_logger.warn("Unhandled exception trying to update load balancer rule", ex);
            txn.rollback();
            throw ex;
        } finally {
            txn.close();
        }

        // now that the load balancer has been updated, reconfigure the HA Proxy on the router with all the LB rules 
        List<FirewallRuleVO> allLbRules = new ArrayList<FirewallRuleVO>();
        IPAddressVO ipAddress = _ipAddressDao.findById(loadBalancer.getIpAddress());
        List<IPAddressVO> ipAddrs = listPublicIpAddressesInVirtualNetwork(loadBalancer.getAccountId(), ipAddress.getDataCenterId(), null);
        for (IPAddressVO ipv : ipAddrs) {
            List<FirewallRuleVO> rules = _firewallRulesDao.listIPForwarding(ipv.getAddress(), false);
            allLbRules.addAll(rules);
        }

        IPAddressVO ip = _ipAddressDao.findById(loadBalancer.getIpAddress());
        DomainRouterVO router = _routerMgr.getRouter(ip.getAccountId(), ip.getDataCenterId());
        updateFirewallRules(loadBalancer.getIpAddress(), allLbRules, router);
        return _loadBalancerDao.findById(loadBalancer.getId());
    }

    public static boolean isAdmin(short accountType) {
        return ((accountType == Account.ACCOUNT_TYPE_ADMIN) ||
                (accountType == Account.ACCOUNT_TYPE_DOMAIN_ADMIN) ||
                (accountType == Account.ACCOUNT_TYPE_READ_ONLY_ADMIN));
    }

    private Account findAccountByIpAddress(String ipAddress) {
        IPAddressVO address = _ipAddressDao.findById(ipAddress);
        if ((address != null) && (address.getAccountId() != null)) {
            return _accountDao.findById(address.getAccountId());
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

            if (ipVO.getAllocated() == null) {
                return true;
            }

            Account Account = _accountDao.findById(accountId);
            if (Account == null) {
                return false;
            }

            if ((ipVO.getAccountId() == null) || (ipVO.getAccountId().longValue() != accountId)) {
                // FIXME: is the user visible in the admin account's domain????
                if (!BaseCmd.isAdmin(Account.getType())) {
                    if (s_logger.isDebugEnabled()) {
                        s_logger.debug("permission denied disassociating IP address " + ipAddress + "; acct: " + accountId + "; ip (acct / dc / dom / alloc): "
                                + ipVO.getAccountId() + " / " + ipVO.getDataCenterId() + " / " + ipVO.getDomainId() + " / " + ipVO.getAllocated());
                    }
                    throw new PermissionDeniedException("User/account does not own supplied address");
                }
            }

            if (ipVO.getAllocated() == null) {
                return true;
            }

            if (ipVO.isSourceNat()) {
                throw new IllegalArgumentException("ip address is used for source nat purposes and can not be disassociated.");
            }

            VlanVO vlan = _vlanDao.findById(ipVO.getVlanDbId());
            if (!vlan.getVlanType().equals(VlanType.VirtualNetwork)) {
                throw new IllegalArgumentException("only ip addresses that belong to a virtual network may be disassociated.");
            }

            //Check for account wide pool. It will have an entry for account_vlan_map. 
            if (_accountVlanMapDao.findAccountVlanMap(accountId,ipVO.getVlanDbId()) != null){
                throw new PermissionDeniedException(ipAddress + " belongs to Account wide IP pool and cannot be disassociated");
            }

            txn.start();
            boolean success = releasePublicIpAddress(userId, ipAddress);
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

    @Override @DB
    public boolean deletePortForwardingRule(Long id, boolean sysContext) {
        Long ruleId = id;
        Long userId = null;
        Account account = null;
        if(sysContext){
            userId = User.UID_SYSTEM;
            account = _accountDao.findById(User.UID_SYSTEM);
        }else{
            userId = UserContext.current().getUserId();
            account = UserContext.current().getAccount();    		
        }


        //verify input parameters here
        FirewallRuleVO rule = _firewallRulesDao.findById(ruleId);
        if (rule == null) {
            throw new InvalidParameterValueException("Unable to find port forwarding rule " + ruleId);
        }

        String publicIp = rule.getPublicIpAddress();
        String privateIp = rule.getPrivateIpAddress();

        IPAddressVO ipAddress = _ipAddressDao.findById(publicIp);
        if (ipAddress == null) {
            throw new InvalidParameterValueException("Unable to find IP address for port forwarding rule " + ruleId);
        }

        // although we are not writing these values to the DB, we will check
        // them out of an abundance
        // of caution (may not be warranted)
        String privatePort = rule.getPrivatePort();
        String publicPort = rule.getPublicPort();
        if (!NetUtils.isValidPort(publicPort) || !NetUtils.isValidPort(privatePort)) {
            throw new InvalidParameterValueException("Invalid value for port");
        }

        String proto = rule.getProtocol();
        if (!NetUtils.isValidProto(proto)) {
            throw new InvalidParameterValueException("Invalid protocol");
        }

        Account ruleOwner = _accountDao.findById(ipAddress.getAccountId());
        if (ruleOwner == null) {
            throw new InvalidParameterValueException("Unable to find owning account for port forwarding rule " + ruleId);
        }

        // if an admin account was passed in, or no account was passed in, make sure we honor the accountName/domainId parameters
        if (account != null) {
            if (isAdmin(account.getType())) {
                if (!_domainDao.isChildDomain(account.getDomainId(), ruleOwner.getDomainId())) {
                    throw new PermissionDeniedException("Unable to delete port forwarding rule " + ruleId + ", permission denied.");
                }
            } else if (account.getId() != ruleOwner.getId()) {
                throw new PermissionDeniedException("Unable to delete port forwarding rule " + ruleId + ", permission denied.");
            }
        }

        Transaction txn = Transaction.currentTxn();
        boolean locked = false;
        boolean success = false;
        try {

            IPAddressVO ipVO = _ipAddressDao.acquireInLockTable(publicIp);
            if (ipVO == null) {
                // throw this exception because hackers can use the api to probe for allocated ips
                throw new PermissionDeniedException("User does not own supplied address");
            }

            locked = true;
            txn.start();
            List<FirewallRuleVO> fwdings = _firewallRulesDao.listIPForwardingForUpdate(publicIp, publicPort, proto);
            FirewallRuleVO fwRule = null;
            if (fwdings.size() == 0) {
                throw new InvalidParameterValueException("No such rule");
            } else if (fwdings.size() == 1) {
                fwRule = fwdings.get(0);
                if (fwRule.getPrivateIpAddress().equalsIgnoreCase(privateIp) && fwRule.getPrivatePort().equals(privatePort)) {
                    _firewallRulesDao.expunge(fwRule.getId());
                } else {
                    throw new InvalidParameterValueException("No such rule");
                }
            } else {
                throw new CloudRuntimeException("Multiple matches. Please contact support");
            }
            fwRule.setEnabled(false);
            success = updateFirewallRule(fwRule, null, null);

            String description;
            String type = EventTypes.EVENT_NET_RULE_DELETE;
            String level = EventVO.LEVEL_INFO;
            String ruleName = rule.isForwarding() ? "ip forwarding" : "load balancer";

            if (success) {
                description = "deleted " + ruleName + " rule [" + publicIp + ":" + rule.getPublicPort() + "]->[" + rule.getPrivateIpAddress() + ":"
                + rule.getPrivatePort() + "] " + rule.getProtocol();
            } else {
                level = EventVO.LEVEL_ERROR;
                description = "Error while deleting " + ruleName + " rule [" + publicIp + ":" + rule.getPublicPort() + "]->[" + rule.getPrivateIpAddress() + ":"
                + rule.getPrivatePort() + "] " + rule.getProtocol();
            }
            EventUtils.saveEvent(userId, ipAddress.getAccountId(), level, type, description);
            txn.commit();
        }catch (Exception ex) {
            txn.rollback();
            s_logger.error("Unexpected exception deleting port forwarding rule " + ruleId, ex);
            return false;
        }finally {
            if (locked) {
                _ipAddressDao.releaseFromLockTable(publicIp);
            }
            txn.close();
        }
        return success;
    }

    @Override
    public List<AccountVO> getAccountsUsingNetworkConfiguration(long configurationId) {
        SearchCriteria<AccountVO> sc = AccountsUsingNetworkConfigurationSearch.create();
        sc.setJoinParameters("nc", "config", configurationId);
        return _accountDao.search(sc, null);
    }

    @Override
    public AccountVO getNetworkConfigurationOwner(long configurationId) {
        SearchCriteria<AccountVO> sc = AccountsUsingNetworkConfigurationSearch.create();
        sc.setJoinParameters("nc", "config", configurationId);
        sc.setJoinParameters("nc", "owner", true);
        List<AccountVO> accounts = _accountDao.search(sc, null);
        return accounts.size() != 0 ? accounts.get(0) : null;
    }

    @Override
    public List<NetworkVO> getNetworkConfigurationsforOffering(long offeringId, long dataCenterId, long accountId) {
        return _networkConfigDao.getNetworkConfigurationsForOffering(offeringId, dataCenterId, accountId);
    }

    @Override
    public List<NetworkVO> setupNetworkConfiguration(Account owner, ServiceOfferingVO offering, DeploymentPlan plan) {
        NetworkOfferingVO networkOffering = _networkOfferingDao.findByServiceOffering(offering);
        return setupNetworkConfiguration(owner, networkOffering, plan, null, null);
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
        String publicIp = cmd.getPublicIp();
        IPAddressVO ipAddr = null;
        Account account = getAccountForApiCommand(cmd.getAccountName(), cmd.getDomainId());
        if (publicIp == null) {
            List<IPAddressVO> accountAddrs = _ipAddressDao.listByAccount(account.getId());
            for (IPAddressVO addr: accountAddrs){
                if (addr.getSourceNat() && addr.getDataCenterId() == cmd.getZoneId()){
                    ipAddr = addr;
                    publicIp = ipAddr.getAddress();
                    break;
                }
            }
            if (ipAddr == null) {
                throw new InvalidParameterValueException("Account " + account.getAccountName() +  " does not have any public ip addresses in zone " + cmd.getZoneId());
            }
        }

        // make sure ip address exists
        ipAddr = _ipAddressDao.findById(publicIp);
        if (ipAddr == null) {
            throw new InvalidParameterValueException("Unable to create remote access vpn, invalid public IP address " + publicIp);
        }

        VlanVO vlan = _vlanDao.findById(ipAddr.getVlanDbId());
        if (vlan != null) {
            if (!VlanType.VirtualNetwork.equals(vlan.getVlanType())) {
                throw new InvalidParameterValueException("Unable to create VPN for IP address " + publicIp + ", only VirtualNetwork type IP addresses can be used for VPN.");
            }
        } 
        assert vlan != null:"Inconsistent DB state -- ip address does not belong to any vlan?";

        if ((ipAddr.getAccountId() == null) || (ipAddr.getAllocated() == null)) {
            throw new PermissionDeniedException("Unable to create VPN, permission denied for ip " + publicIp);
        }

        if (account != null) {
            if ((account.getType() == Account.ACCOUNT_TYPE_ADMIN) || (account.getType() == Account.ACCOUNT_TYPE_DOMAIN_ADMIN)) {
                if (!_domainDao.isChildDomain(account.getDomainId(), ipAddr.getDomainId())) {
                    throw new PermissionDeniedException("Unable to create VPN with public IP address " + publicIp + ", permission denied.");
                }
            } else if (account.getId() != ipAddr.getAccountId().longValue()) {
                throw new PermissionDeniedException("Unable to create VPN for account " + account.getAccountName() + " doesn't own ip address " + publicIp);
            }
        }

        RemoteAccessVpnVO vpnVO = _remoteAccessVpnDao.findByPublicIpAddress(publicIp);
        if (vpnVO != null) {
            throw new InvalidParameterValueException("A Remote Access VPN already exists for this public Ip address");
        }
        //TODO: assumes one virtual network / domr per account per zone
        vpnVO = _remoteAccessVpnDao.findByAccountAndZone(account.getId(), cmd.getZoneId());
        if (vpnVO != null) {
            throw new InvalidParameterValueException("A Remote Access VPN already exists for this account");
        }
        String ipRange = cmd.getIpRange();
        if (ipRange == null) {
            ipRange = _configs.get(Config.RemoteAccessVpnClientIpRange.key());
        }
        String [] range = ipRange.split("-");
        if (range.length != 2) {
            throw new InvalidParameterValueException("Invalid ip range");
        }
        if (!NetUtils.isValidIp(range[0]) || !NetUtils.isValidIp(range[1])){
            throw new InvalidParameterValueException("Invalid ip in range specification " + ipRange);
        }
        if (!NetUtils.validIpRange(range[0], range[1])){
            throw new InvalidParameterValueException("Invalid ip range " + ipRange);
        }
        String [] guestIpRange = getGuestIpRange();
        if (NetUtils.ipRangesOverlap(range[0], range[1], guestIpRange[0], guestIpRange[1])) {
            throw new InvalidParameterValueException("Invalid ip range: " + ipRange + " overlaps with guest ip range " + guestIpRange[0] + "-" + guestIpRange[1]);
        }
        //TODO: check sufficient range
        //TODO: check overlap with private and public ip ranges in datacenter

        long startIp = NetUtils.ip2Long(range[0]);
        String newIpRange = NetUtils.long2Ip(++startIp) + "-" + range[1];
        String sharedSecret = PasswordGenerator.generatePresharedKey(getIntegerConfigValue(Config.RemoteAccessVpnPskLength.key(), 24)); 
        Transaction txn = Transaction.currentTxn();
        txn.start();
        boolean locked = false;
        try {
            ipAddr = _ipAddressDao.acquireInLockTable(publicIp);
            if (ipAddr == null) {
                throw new ConcurrentOperationException("Another operation active, unable to create vpn");
            }
            locked = true;
            //check overlap with port forwarding rules on this ip (udp ports 500, 4500)
            List<FirewallRuleVO> existing = _rulesDao.listIPForwardingByPortAndProto(publicIp, NetUtils.VPN_PORT, NetUtils.UDP_PROTO);
            if (!existing.isEmpty()) {
                throw new InvalidParameterValueException("UDP Port " + NetUtils.VPN_PORT + " is configured for destination NAT");
            }
            existing = _rulesDao.listIPForwardingByPortAndProto(publicIp, NetUtils.VPN_NATT_PORT, NetUtils.UDP_PROTO);
            if (!existing.isEmpty()) {
                throw new InvalidParameterValueException("UDP Port " + NetUtils.VPN_NATT_PORT + " is configured for destination NAT");
            }
            existing = _rulesDao.listIPForwardingByPortAndProto(publicIp, NetUtils.VPN_L2TP_PORT, NetUtils.UDP_PROTO);
            if (!existing.isEmpty()) {
                throw new InvalidParameterValueException("UDP Port " + NetUtils.VPN_L2TP_PORT + " is configured for destination NAT");
            }
            if (_rulesDao.isPublicIpOneToOneNATted(publicIp)) {
                throw new InvalidParameterValueException("Public Ip " + publicIp + " is configured for destination NAT");
            }
            vpnVO = new RemoteAccessVpnVO(account.getId(), cmd.getZoneId(), publicIp, range[0], newIpRange, sharedSecret);
            vpnVO = _remoteAccessVpnDao.persist(vpnVO);
            FirewallRuleVO rule = new FirewallRuleVO(null, publicIp, NetUtils.VPN_PORT, guestIpRange[0], NetUtils.VPN_PORT, true, NetUtils.UDP_PROTO, false, null);
            _rulesDao.persist(rule);
            rule = new FirewallRuleVO(null, publicIp, NetUtils.VPN_NATT_PORT, guestIpRange[0], NetUtils.VPN_NATT_PORT, true, NetUtils.UDP_PROTO, false, null);
            _rulesDao.persist(rule);
            rule = new FirewallRuleVO(null, publicIp, NetUtils.VPN_L2TP_PORT, guestIpRange[0], NetUtils.VPN_L2TP_PORT, true, NetUtils.UDP_PROTO, false, null);
            _rulesDao.persist(rule);
            txn.commit();
            return vpnVO;
        } finally {
            if (locked) {
                _ipAddressDao.releaseFromLockTable(publicIp);
            }
        }
    }

    @Override
    @DB
    public RemoteAccessVpnVO startRemoteAccessVpn(CreateRemoteAccessVpnCmd cmd) throws ConcurrentOperationException, ResourceUnavailableException {
        Long userId = UserContext.current().getUserId();
        Account account = getAccountForApiCommand(cmd.getAccountName(), cmd.getDomainId());
        EventUtils.saveStartedEvent(userId, account.getId(), EventTypes.EVENT_REMOTE_ACCESS_VPN_CREATE, "Creating a Remote Access VPN for account: " + account.getAccountName() + " in zone " + cmd.getZoneId(), cmd.getStartEventId());
        RemoteAccessVpnVO vpnVO = _remoteAccessVpnDao.findById(cmd.getId());
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
        Long userId = UserContext.current().getUserId();
        Account account = getAccountForApiCommand(cmd.getAccountName(), cmd.getDomainId());
        //TODO: assumes one virtual network / domr per account per zone
        RemoteAccessVpnVO vpnVO = _remoteAccessVpnDao.findByAccountAndZone(account.getId(), cmd.getZoneId());
        if (vpnVO == null) {
            throw new InvalidParameterValueException("No VPN found for account " + account.getAccountName() + " in zone " + cmd.getZoneId());
        }
        EventUtils.saveStartedEvent(userId, account.getId(), EventTypes.EVENT_REMOTE_ACCESS_VPN_DESTROY, "Deleting Remote Access VPN for account: " + account.getAccountName() + " in zone " + cmd.getZoneId(), cmd.getStartEventId());
        String publicIp = vpnVO.getVpnServerAddress();
        Long  vpnId = vpnVO.getId();
        Transaction txn = Transaction.currentTxn();
        txn.start();
        boolean locked = false;
        boolean deleted = false;
        try {
            IPAddressVO ipAddr = _ipAddressDao.acquireInLockTable(publicIp);
            if (ipAddr == null) {
                throw new ConcurrentOperationException("Another operation active, unable to create vpn");
            }
            locked = true;

            deleted = _routerMgr.deleteRemoteAccessVpn(vpnVO);
            return deleted;
        } finally {
            if (deleted) {
                _remoteAccessVpnDao.remove(vpnId);
                _rulesDao.deleteIPForwardingByPublicIpAndPort(publicIp, NetUtils.VPN_PORT);
                _rulesDao.deleteIPForwardingByPublicIpAndPort(publicIp, NetUtils.VPN_NATT_PORT);
                _rulesDao.deleteIPForwardingByPublicIpAndPort(publicIp, NetUtils.VPN_L2TP_PORT);
                EventUtils.saveEvent(userId, account.getId(), EventTypes.EVENT_REMOTE_ACCESS_VPN_DESTROY, "Deleted Remote Access VPN for account: " + account.getAccountName() + " in zone " + cmd.getZoneId());
            } else {
                EventUtils.saveEvent(userId, account.getId(), EventVO.LEVEL_ERROR, EventTypes.EVENT_REMOTE_ACCESS_VPN_DESTROY, "Unable to delete Remote Access VPN ", account.getAccountName() + " in zone " + cmd.getZoneId());
            }
            txn.commit();
            if (locked) {
                _ipAddressDao.releaseFromLockTable(publicIp);
            }
        }
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
    public String getNextAvailableMacAddressInNetwork(long networkConfigurationId) throws InsufficientAddressCapacityException {
        String mac = _networkConfigDao.getNextAvailableMacAddress(networkConfigurationId);
        if (mac == null) {
            throw new InsufficientAddressCapacityException("Unable to create another mac address", Network.class, networkConfigurationId);
        }
        return mac;
    }

    @Override @DB
    public Network getNetworkConfiguration(long id) {
        return _networkConfigDao.findById(id);
    }

    @Override @DB
    public FirewallRule createIpForwardingRuleOnDomr(long ruleId) {
        Transaction txn = Transaction.currentTxn();
        txn.start();
        boolean success = false;
        FirewallRuleVO rule = null;
        IPAddressVO ipAddress = null;
        boolean locked = false;
        try {
            //get the rule 
            rule = _rulesDao.findById(ruleId);

            if(rule == null){
                throw new PermissionDeniedException("Cannot create ip forwarding rule in db");
            }

            //get ip address 
            ipAddress = _ipAddressDao.findById(rule.getPublicIpAddress());
            if (ipAddress == null) {
                throw new InvalidParameterValueException("Unable to create ip forwarding rule on address " + ipAddress + ", invalid IP address specified.");
            }

            //sync point
            ipAddress = _ipAddressDao.acquireInLockTable(ipAddress.getAddress());

            if(ipAddress == null){
                s_logger.warn("Unable to acquire lock on ipAddress for creating static NAT rule");
                return rule;
            }else{
                locked = true;
            }

            //get the domain router object
            DomainRouterVO router = _routerMgr.getRouter(ipAddress.getAccountId(), ipAddress.getDataCenterId());
            success = createOrDeleteIpForwardingRuleOnDomr(rule,router,rule.getPrivateIpAddress(),true); //true +> create

            if(!success){
                //corner case; delete record from db as domR rule creation failed
                _rulesDao.remove(ruleId);
                throw new PermissionDeniedException("Cannot create ip forwarding rule on domr, hence deleting created record in db");
            }

            //update the user_ip_address record
            ipAddress.setOneToOneNat(true);
            _ipAddressDao.update(ipAddress.getAddress(),ipAddress);

            // Save and create the event
            String description;
            String ruleName = "ip forwarding";
            String level = EventVO.LEVEL_INFO;

            description = "created new " + ruleName + " rule [" + rule.getPublicIpAddress() + "]->["
            + rule.getPrivateIpAddress() + "]" + ":" + rule.getProtocol();

            EventUtils.saveEvent(UserContext.current().getUserId(), ipAddress.getAccountId(), level, EventTypes.EVENT_NET_RULE_ADD, description);
            txn.commit();
        } catch (Exception e) {
            txn.rollback();
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, e.getMessage());
        }finally{
            if(locked){
                _ipAddressDao.releaseFromLockTable(ipAddress.getAddress());
            }
        }
        return rule;
    }

    @Override @DB
    public FirewallRule createIpForwardingRuleInDb(String ipAddr, long virtualMachineId) {

        Transaction txn = Transaction.currentTxn();
        txn.start();
        UserVmVO userVM = null;
        FirewallRuleVO newFwRule = null;
        boolean locked = false;
        try {
            // validate IP Address exists
            IPAddressVO ipAddress = _ipAddressDao.findById(ipAddr);
            if (ipAddress == null) {
                throw new InvalidParameterValueException("Unable to create ip forwarding rule on address " + ipAddress + ", invalid IP address specified.");
            }

            // validate user VM exists
            userVM = _vmDao.findById(virtualMachineId);
            if (userVM == null) {
                throw new InvalidParameterValueException("Unable to create ip forwarding rule on address " + ipAddress + ", invalid virtual machine id specified (" + virtualMachineId + ").");
            }

            //sync point; cannot lock on rule ; hence sync on vm
            userVM = _vmDao.acquireInLockTable(userVM.getId());

            if(userVM == null){
                s_logger.warn("Unable to acquire lock on user vm for creating static NAT rule");
                return newFwRule;
            }else{
                locked = true;
            }

            // validate that IP address and userVM belong to the same account
            if ((ipAddress.getAccountId() == null) || (ipAddress.getAccountId().longValue() != userVM.getAccountId())) {
                throw new InvalidParameterValueException("Unable to create ip forwarding rule, IP address " + ipAddress + " owner is not the same as owner of virtual machine " + userVM.toString()); 
            }

            // validate that userVM is in the same availability zone as the IP address
            if (ipAddress.getDataCenterId() != userVM.getDataCenterId()) {
                throw new InvalidParameterValueException("Unable to create ip forwarding rule, IP address " + ipAddress + " is not in the same availability zone as virtual machine " + userVM.toString());
            }

            // if an admin account was passed in, or no account was passed in, make sure we honor the accountName/domainId parameters
            Account account = UserContext.current().getAccount();
            if (account != null) {
                if ((account.getType() == Account.ACCOUNT_TYPE_ADMIN) || (account.getType() == Account.ACCOUNT_TYPE_DOMAIN_ADMIN)) {
                    if (!_domainDao.isChildDomain(account.getDomainId(), userVM.getDomainId())) {
                        throw new PermissionDeniedException("Unable to create ip forwarding rule, IP address " + ipAddress + " to virtual machine " + virtualMachineId + ", permission denied.");
                    }
                } else if (account.getId() != userVM.getAccountId()) {
                    throw new PermissionDeniedException("Unable to create ip forwarding rule, IP address " + ipAddress + " to virtual machine " + virtualMachineId + ", permission denied.");
                }
            }

            // check for ip address/port conflicts by checking existing port/ip forwarding rules
            List<FirewallRuleVO> existingFirewallRules = _rulesDao.findRuleByPublicIp(ipAddr);

            if(existingFirewallRules.size() > 0){
                throw new NetworkRuleConflictException("There already exists a firewall rule for public ip:"+ipAddr);
            }

            //check for ip address/port conflicts by checking existing load balancing rules
            List<LoadBalancerVO> existingLoadBalancerRules = _loadBalancerDao.listByIpAddress(ipAddr);

            if(existingLoadBalancerRules.size() > 0){
                throw new NetworkRuleConflictException("There already exists a load balancer rule for public ip:"+ipAddr);
            }
            
            //if given ip address is already source nat, return error
            if(ipAddress.isSourceNat()){
                throw new PermissionDeniedException("Cannot create a static nat rule for the ip:"+ipAddress.getAddress()+" ,this is already a source nat ip address");
            }

            //if given ip address is already static nat, return error
            if(ipAddress.isOneToOneNat()){
                throw new PermissionDeniedException("Cannot create a static nat rule for the ip:"+ipAddress.getAddress()+" ,this is already a static nat ip address");
            }
            
            newFwRule = new FirewallRuleVO();
            newFwRule.setEnabled(true);
            newFwRule.setForwarding(true);
            newFwRule.setPrivatePort(null);
            newFwRule.setProtocol(NetUtils.NAT_PROTO);//protocol cannot be null; adding this as a NAT
            newFwRule.setPublicPort(null);
            newFwRule.setPublicIpAddress(ipAddress.getAddress());
            newFwRule.setPrivateIpAddress(userVM.getGuestIpAddress());
            newFwRule.setGroupId(null);

            _rulesDao.persist(newFwRule);			
            txn.commit();
        } catch (Exception e) {
            s_logger.warn("Unable to create new firewall rule for static NAT");
            txn.rollback();
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR,"Unable to create new firewall rule for static NAT:"+e.getMessage());
        }finally{
            if(locked) {
                _vmDao.releaseFromLockTable(userVM.getId());
            }
        }

        return newFwRule;
    }

    @Override @DB
    public boolean deleteIpForwardingRule(Long id) {
        Long ruleId = id;
        Long userId = UserContext.current().getUserId();
        Account account = UserContext.current().getAccount();

        //verify input parameters here
        FirewallRuleVO rule = _firewallRulesDao.findById(ruleId);
        if (rule == null) {
            throw new InvalidParameterValueException("Unable to find port forwarding rule " + ruleId);
        }

        String publicIp = rule.getPublicIpAddress();


        IPAddressVO ipAddress = _ipAddressDao.findById(publicIp);
        if (ipAddress == null) {
            throw new InvalidParameterValueException("Unable to find IP address for ip forwarding rule " + ruleId);
        }

        // although we are not writing these values to the DB, we will check
        // them out of an abundance
        // of caution (may not be warranted)

        Account ruleOwner = _accountDao.findById(ipAddress.getAccountId());
        if (ruleOwner == null) {
            throw new InvalidParameterValueException("Unable to find owning account for ip forwarding rule " + ruleId);
        }

        // if an admin account was passed in, or no account was passed in, make sure we honor the accountName/domainId parameters
        if (account != null) {
            if (isAdmin(account.getType())) {
                if (!_domainDao.isChildDomain(account.getDomainId(), ruleOwner.getDomainId())) {
                    throw new PermissionDeniedException("Unable to delete ip forwarding rule " + ruleId + ", permission denied.");
                }
            } else if (account.getId() != ruleOwner.getId()) {
                throw new PermissionDeniedException("Unable to delete ip forwarding rule " + ruleId + ", permission denied.");
            }
        }

        Transaction txn = Transaction.currentTxn();
        boolean locked = false;
        boolean success = false;
        try {

            ipAddress = _ipAddressDao.acquireInLockTable(publicIp);
            if (ipAddress == null) {
                throw new PermissionDeniedException("Unable to obtain lock on record for deletion");
            }

            locked = true;
            txn.start();

            final DomainRouterVO router = _routerMgr.getRouter(ipAddress.getAccountId(), ipAddress.getDataCenterId());
            success = createOrDeleteIpForwardingRuleOnDomr(rule, router, rule.getPrivateIpAddress(), false);
            _firewallRulesDao.remove(ruleId);

            //update the ip_address record
            ipAddress.setOneToOneNat(false);
            _ipAddressDao.persist(ipAddress);

            String description;
            String type = EventTypes.EVENT_NET_RULE_DELETE;
            String level = EventVO.LEVEL_INFO;
            String ruleName = rule.isForwarding() ? "ip forwarding" : "load balancer";

            if (success) {
                description = "deleted " + ruleName + " rule [" + publicIp +"]->[" + rule.getPrivateIpAddress() + "] " + rule.getProtocol();
            } else {
                level = EventVO.LEVEL_ERROR;
                description = "Error while deleting " + ruleName + " rule [" + publicIp + "]->[" + rule.getPrivateIpAddress() +"] " + rule.getProtocol();
            }
            EventUtils.saveEvent(userId, ipAddress.getAccountId(), level, type, description);
            txn.commit();
        }catch (Exception ex) {
            txn.rollback();
            s_logger.error("Unexpected exception deleting port forwarding rule " + ruleId, ex);
            return false;
        }finally {
            if (locked) {
                _ipAddressDao.releaseFromLockTable(publicIp);
            }
            txn.close();
        }
        return success;
    }

    private boolean  createOrDeleteIpForwardingRuleOnDomr(FirewallRuleVO fwRule, DomainRouterVO router, String guestIp, boolean create){

        Commands cmds = new Commands(OnError.Continue);
        final SetFirewallRuleCommand cmd = new SetFirewallRuleCommand(router.getInstanceName(), router.getPrivateIpAddress(),fwRule, create);
        cmds.addCommand(cmd);       
        try {
            _agentMgr.send(router.getHostId(), cmds);
        } catch (final AgentUnavailableException e) {
            s_logger.warn("agent unavailable", e);
        } catch (final OperationTimedoutException e) {
            s_logger.warn("Timed Out", e);
        }
        Answer[] answers = cmds.getAnswers();
        if (answers == null || answers[0].getResult() == false ){
            return false;
        }else{
            return true;
        }
    }
    
    @Override @DB
    public Network createNetwork(CreateNetworkCmd cmd) throws InvalidParameterValueException, PermissionDeniedException{
        Account ctxAccount = UserContext.current().getAccount();
        Long userId = UserContext.current().getUserId();
        Long networkOfferingId = cmd.getNetworkOfferingId();
        Long zoneId = cmd.getZoneId();
        Long podId = cmd.getPodId();
        String gateway = cmd.getGateway();
        String cidr = cmd.getCidr();
        String startIP = cmd.getStartIp();
        String endIP = cmd.getEndIp();
        String vlanNetmask = cmd.getNetmask();
        String accountName = cmd.getAccountName();
        Long domainId = cmd.getDomainId();
        String vlanId = cmd.getVlan();
        String name = cmd.getNetworkName();
        String displayText = cmd.getDisplayText();
        Account owner = null;
        
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
       
        if (owner.getId() == Account.ACCOUNT_ID_SYSTEM && !networkOffering.isShared()) {
            throw new InvalidParameterValueException("Non-system account is required when create a network from Dedicated network offering with id=" + networkOfferingId);
        } 
        
       //VlanId can be specified only when network offering supports it
        if (vlanId != null && !networkOffering.getSpecifyVlan()) {
            throw new InvalidParameterValueException("Can't specify vlan because network offering doesn't support it");
        }
        
        //If gateway, startIp, endIp are speicified, cidr should be present as well
        if (gateway != null && startIP != null && endIP != null && cidr == null) {
            throw new InvalidParameterValueException("Cidr is missing");
        }
            
       Transaction txn = Transaction.currentTxn();
       txn.start();
       try {
           //Create network
           DataCenterDeployment plan = new DataCenterDeployment(zoneId, null, null, null);
           NetworkVO userNetwork = new NetworkVO();
           
           //cidr should be set only when the user is admin
           if (ctxAccount.getType() == Account.ACCOUNT_TYPE_ADMIN && cidr != null && gateway != null) {
               userNetwork.setCidr(cidr);
               userNetwork.setGateway(gateway);
               if (vlanId != null) {
                   userNetwork.setBroadcastUri(URI.create("vlan://" + vlanId));
               }
           }
           
           List<NetworkVO> networks = setupNetworkConfiguration(owner, networkOffering, userNetwork, plan, name, displayText);
           Long networkId = null;
           
           if (networks == null || networks.isEmpty()) {
               txn.rollback();
               throw new CloudRuntimeException("Fail to create a network");
           } else {
               networkId = networks.get(0).getId();
           }
           
           //If network offering is shared, don't pass owner account and networkOfferingId for vlan
           if (networkOffering.isShared()) {
               owner = null;
           }
           
           if (ctxAccount.getType() == Account.ACCOUNT_TYPE_ADMIN && networkOffering.getGuestIpType() != GuestIpType.Virtualized && startIP != null && endIP != null && gateway != null) {
               //Create vlan ip range
               Vlan vlan = _configMgr.createVlanAndPublicIpRange(userId, zoneId, podId, startIP, endIP, gateway, vlanNetmask, false, vlanId, owner, networkId);
               if (vlan == null) {
                   txn.rollback();
                   throw new CloudRuntimeException("Fail to create a vlan");
               }
           }  
           txn.commit();
           return networks.get(0);
       } catch (Exception ex) {
           s_logger.warn("Unexpected exception while creating network ", ex);
           txn.rollback();
       }finally {
           txn.close();
       }
       return null;
    }
    
    @Override
    public List<? extends Network> searchForNetworks(ListNetworksCmd cmd) { 
        Object id = cmd.getId(); 
        Object keyword = cmd.getKeyword();
        Account account = UserContext.current().getAccount();
        Long domainId = cmd.getDomainId();
        String accountName = cmd.getAccountName();
        Long accountId = null;
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
            } 
        } else {
            accountId = account.getId();
        }
        
        
        Filter searchFilter = new Filter(NetworkVO.class, "id", false, cmd.getStartIndex(), cmd.getPageSizeVal());
        SearchCriteria<NetworkVO> sc = _networkConfigDao.createSearchCriteria();
        
        if (keyword != null) {
            SearchCriteria<NetworkVO> ssc = _networkConfigDao.createSearchCriteria();
            ssc.addOr("name", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            sc.addAnd("name", SearchCriteria.Op.SC, ssc);
        } 

        if (id != null) {
            sc.addAnd("id", SearchCriteria.Op.EQ, id);
        }
        
        if (accountId != null) {
            sc.addAnd("accountId", SearchCriteria.Op.EQ, accountId);
        }
        return _networkConfigDao.search(sc, searchFilter);
    }
    
    @Override @DB
    public boolean deleteNetwork(DeleteNetworkCmd cmd) throws InvalidParameterValueException, PermissionDeniedException{        
        Long networkId = cmd.getId();
        Long userId = UserContext.current().getUserId();
        Account account = UserContext.current().getAccount();

        //Verify network id
        NetworkVO network = _networkConfigDao.findById(networkId);
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
            if (vm.getState() != State.Destroyed || vm.getState() != State.Expunging || vm.getState() != State.Error) {
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
            _networkConfigDao.remove(networkId);
            
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
}
