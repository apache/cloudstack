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

import java.util.ArrayList;
import java.util.Arrays;
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
import com.cloud.api.commands.AssignToLoadBalancerRuleCmd;
import com.cloud.api.commands.AssociateIPAddrCmd;
import com.cloud.api.commands.CreateIPForwardingRuleCmd;
import com.cloud.api.commands.CreateLoadBalancerRuleCmd;
import com.cloud.api.commands.DeleteIPForwardingRuleCmd;
import com.cloud.api.commands.DeleteLoadBalancerRuleCmd;
import com.cloud.api.commands.DeletePortForwardingServiceRuleCmd;
import com.cloud.api.commands.DisassociateIPAddrCmd;
import com.cloud.api.commands.ListPortForwardingRulesCmd;
import com.cloud.api.commands.RebootRouterCmd;
import com.cloud.api.commands.RemoveFromLoadBalancerRuleCmd;
import com.cloud.api.commands.StartRouterCmd;
import com.cloud.api.commands.StopRouterCmd;
import com.cloud.api.commands.UpdateLoadBalancerRuleCmd;
import com.cloud.api.commands.UpgradeRouterCmd;
import com.cloud.async.AsyncJobManager;
import com.cloud.capacity.dao.CapacityDao;
import com.cloud.configuration.ConfigurationManager;
import com.cloud.configuration.ResourceCount.ResourceType;
import com.cloud.configuration.dao.ConfigurationDao;
import com.cloud.configuration.dao.ResourceLimitDao;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.HostPodVO;
import com.cloud.dc.Vlan.VlanType;
import com.cloud.dc.VlanVO;
import com.cloud.dc.dao.AccountVlanMapDao;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.HostPodDao;
import com.cloud.dc.dao.VlanDao;
import com.cloud.deploy.DeployDestination;
import com.cloud.deploy.DeploymentPlan;
import com.cloud.domain.dao.DomainDao;
import com.cloud.event.EventTypes;
import com.cloud.event.EventUtils;
import com.cloud.event.EventVO;
import com.cloud.event.dao.EventDao;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InsufficientVirtualNetworkCapcityException;
import com.cloud.exception.InternalErrorException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.network.Network.TrafficType;
import com.cloud.network.configuration.NetworkGuru;
import com.cloud.network.dao.FirewallRulesDao;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.LoadBalancerDao;
import com.cloud.network.dao.LoadBalancerVMMapDao;
import com.cloud.network.dao.NetworkConfigurationDao;
import com.cloud.network.dao.NetworkRuleConfigDao;
import com.cloud.network.dao.SecurityGroupDao;
import com.cloud.network.dao.SecurityGroupVMMapDao;
import com.cloud.network.element.NetworkElement;
import com.cloud.network.router.DomainRouterManager;
import com.cloud.offering.NetworkOffering;
import com.cloud.offering.NetworkOffering.GuestIpType;
import com.cloud.offerings.NetworkOfferingVO;
import com.cloud.offerings.dao.NetworkOfferingDao;
import com.cloud.resource.Resource;
import com.cloud.resource.Resource.ReservationStrategy;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.storage.StorageManager;
import com.cloud.storage.dao.DiskTemplateDao;
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
import com.cloud.utils.StringUtils;
import com.cloud.utils.Ternary;
import com.cloud.utils.component.Adapters;
import com.cloud.utils.component.Inject;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.JoinBuilder;
import com.cloud.utils.db.JoinBuilder.JoinType;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.net.NetUtils;
import com.cloud.vm.DomainRouter;
import com.cloud.vm.DomainRouterVO;
import com.cloud.vm.NicProfile;
import com.cloud.vm.NicVO;
import com.cloud.vm.State;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachineProfile;
import com.cloud.vm.dao.NicDao;
import com.cloud.vm.dao.UserVmDao;

/**
 * NetworkManagerImpl implements NetworkManager.
 */
@Local(value={NetworkManager.class, DomainRouterService.class})
public class NetworkManagerImpl implements NetworkManager, DomainRouterService {
    private static final Logger s_logger = Logger.getLogger(NetworkManagerImpl.class);

    String _name;
    @Inject DataCenterDao _dcDao = null;
    @Inject VlanDao _vlanDao = null;
    @Inject FirewallRulesDao _rulesDao = null;
    @Inject SecurityGroupVMMapDao _securityGroupVMMapDao = null;
    @Inject LoadBalancerDao _loadBalancerDao = null;
    @Inject LoadBalancerVMMapDao _loadBalancerVMMapDao = null;
    @Inject IPAddressDao _ipAddressDao = null;
    @Inject VMTemplateDao _templateDao =  null;
    @Inject DiskTemplateDao _diskDao = null;
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
    @Inject SecurityGroupDao _securityGroupDao = null;
    @Inject ServiceOfferingDao _serviceOfferingDao = null;
    @Inject UserVmDao _userVmDao;
    @Inject FirewallRulesDao _firewallRulesDao;
    @Inject NetworkRuleConfigDao _networkRuleConfigDao;
    @Inject AccountVlanMapDao _accountVlanMapDao;
    @Inject UserStatisticsDao _statsDao = null;
    @Inject NetworkOfferingDao _networkOfferingDao = null;
    @Inject NetworkConfigurationDao _networkConfigDao = null;
    @Inject NicDao _nicDao;
    @Inject GuestOSDao _guestOSDao = null;
    @Inject DomainRouterManager _routerMgr;
    
    @Inject(adapter=NetworkGuru.class)
    Adapters<NetworkGuru> _networkGurus;
    @Inject(adapter=NetworkElement.class)
    Adapters<NetworkElement> _networkElements;

    private HashMap<String, NetworkOfferingVO> _systemNetworks = new HashMap<String, NetworkOfferingVO>(5);
    
    ScheduledExecutorService _executor;
    
    SearchBuilder<AccountVO> AccountsUsingNetworkConfigurationSearch;
	
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

        	account = _accountDao.acquire(accountId);
            if (account == null) {
                s_logger.warn("Unable to lock account " + accountId);
                return null;
            }
            if(s_logger.isDebugEnabled())
            	s_logger.debug("lock account " + accountId + " is acquired");
            
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
                    s_logger.debug("Router is " + router.getName());
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
        		if(s_logger.isDebugEnabled())
        			s_logger.debug("Releasing lock account " + accountId);
        		
        		_accountDao.release(accountId);
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
    public boolean upgradeRouter(UpgradeRouterCmd cmd) throws InvalidParameterValueException, PermissionDeniedException {
        return _routerMgr.upgradeRouter(cmd);
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
    public DomainRouterVO startRouter(StartRouterCmd cmd) throws InvalidParameterValueException, PermissionDeniedException {
        return _routerMgr.startRouter(cmd);
    }

    @Override
    public boolean stopRouter(final long routerId, long eventId) {
        return _routerMgr.stopRouter(routerId, eventId);
    }
    
    
    @Override
    public DomainRouterVO stopRouter(StopRouterCmd cmd) throws InvalidParameterValueException, PermissionDeniedException{
        return _routerMgr.stopRouter(cmd);
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
    public DomainRouterVO rebootRouter(RebootRouterCmd cmd) throws InvalidParameterValueException, PermissionDeniedException{
        _routerMgr.rebootRouter(cmd);
        return _routerMgr.getRouter(cmd.getId());
    }

    @Override
    public boolean associateIP(final DomainRouterVO router, final List<String> ipAddrList, final boolean add, long vmId) {
        Commands cmds = new Commands(OnError.Continue);
        boolean sourceNat = false;
        for (final String ipAddress: ipAddrList) {
        	if (ipAddress.equalsIgnoreCase(router.getPublicIpAddress()))
        		sourceNat=true;
        	
        	IPAddressVO ip = _ipAddressDao.findById(ipAddress);
        	VlanVO vlan = _vlanDao.findById(ip.getVlanDbId());
			String vlanId = vlan.getVlanId();
			String vlanGateway = vlan.getVlanGateway();
			String vlanNetmask = vlan.getVlanNetmask();
        	boolean firstIP = (!sourceNat && (_ipAddressDao.countIPs(vlan.getDataCenterId(), vlan.getVlanId(), vlan.getVlanGateway(), vlan.getVlanNetmask(), true) == 1));
        				
			String vifMacAddress = null;
			if (firstIP) {
				String[] macAddresses = _dcDao.getNextAvailableMacAddressPair(ip.getDataCenterId());
            	vifMacAddress = macAddresses[1];
			}

			String vmGuestAddress = null;
			if(vmId!=0){
				vmGuestAddress = _vmDao.findById(vmId).getGuestIpAddress();
			}
			
            cmds.addCommand(new IPAssocCommand(router.getInstanceName(), router.getPrivateIpAddress(), ipAddress, add, firstIP, sourceNat, vlanId, vlanGateway, vlanNetmask, vifMacAddress, vmGuestAddress));
            
            sourceNat = false;
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
    
    
    @Override @DB
    public IPAddressVO associateIP(AssociateIPAddrCmd cmd) throws ResourceAllocationException, InsufficientAddressCapacityException, InvalidParameterValueException, InternalErrorException, PermissionDeniedException  {
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
            accountToLock = _accountDao.acquire(accountId);

            if (accountToLock == null) {
                s_logger.warn("Unable to lock account: " + accountId);
                throw new InternalErrorException("Unable to acquire account lock");
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
                throw new InvalidParameterValueException("No router found for account: " + accountToLock.getAccountName() + ".");
            }

            txn.start();

            String ipAddress = null;
            Pair<String, VlanVO> ipAndVlan = _vlanDao.assignIpAddress(zoneId, accountId, domainId, VlanType.VirtualNetwork, false);
            
            if (ipAndVlan == null) {
                throw new InsufficientAddressCapacityException("Unable to find available public IP addresses");
            } else {
            	ipAddress = ipAndVlan.first();
            	_accountMgr.incrementResourceCount(accountId, ResourceType.public_ip);
            }

            boolean success = true;
            String errorMsg = "";

            List<String> ipAddrs = new ArrayList<String>();
            ipAddrs.add(ipAddress);

            if (router.getState() == State.Running) {
                success = associateIP(router, ipAddrs, true, 0L);
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

                throw new InternalErrorException(errorMsg);
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
        } catch (InternalErrorException iee) {
            s_logger.error("Associate IP threw an InternalErrorException.", iee);
            throw iee;
        } catch (Throwable t) {
            s_logger.error("Associate IP address threw an exception.", t);
            throw new InternalErrorException("Associate IP address exception");
        } finally {
            if (account != null) {
                _accountDao.release(accountId);
                s_logger.debug("Associate IP address lock released");
            }
        }
        
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
        } else {
            final List<FirewallRuleVO> fwRules = _rulesDao.listIPForwarding(ipVO.getAccountId(), ipVO.getDataCenterId());
 
            return updateLoadBalancerRules(fwRules, router, hostId);
        }
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
                final SetFirewallRuleCommand cmd = new SetFirewallRuleCommand(routerName, routerIp, rule);
                cmds.addCommand(cmd);
            } else {
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
                final SetFirewallRuleCommand cmd = new SetFirewallRuleCommand(router.getInstanceName(), router.getPrivateIpAddress(), rule);
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
    public FirewallRuleVO createPortForwardingRule(CreateIPForwardingRuleCmd cmd) throws InvalidParameterValueException, PermissionDeniedException, NetworkRuleConflictException {
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
                        return null; // already mapped
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
            } else {
                throw new InvalidParameterValueException("Unable to list port forwarding rules for address " + ipAddress + ", address not in use.");
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
    public void assignToLoadBalancer(AssignToLoadBalancerRuleCmd cmd)  throws NetworkRuleConflictException, InternalErrorException,
                                                                              PermissionDeniedException, InvalidParameterValueException {
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

        DomainRouterVO syncObject = _routerMgr.getRouter(loadBalancer.getIpAddress());
        cmd.synchronizeCommand("Router", syncObject.getId());

        // Permission check...
        Account account = UserContext.current().getAccount();
        if (account != null) {
        	if ((account.getType() == Account.ACCOUNT_TYPE_DOMAIN_ADMIN) || (account.getType() == Account.ACCOUNT_TYPE_READ_ONLY_ADMIN)) {
        		if (!_domainDao.isChildDomain(account.getDomainId(), loadBalancer.getDomainId())) {
            		throw new PermissionDeniedException("Failed to assign to load balancer " + loadBalancerId + ", permission denied.");
        		}
        	} else if (account.getId() != loadBalancer.getAccountId()) {
        		throw new PermissionDeniedException("Failed to assign to load balancer " + loadBalancerId + ", permission denied.");
        	}
        }

        Transaction txn = Transaction.currentTxn();
        try {
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
                    s_logger.warn("guest vm " + userVm.getName() + " (id:" + userVm.getId() + ") belongs to account " + userVm.getAccountId()
                            + ", previous vm in list belongs to account " + accountId);
                    throw new InvalidParameterValueException("guest vm " + userVm.getName() + " (id:" + userVm.getId() + ") belongs to account " + userVm.getAccountId()
                            + ", previous vm in list belongs to account " + accountId);
                }
                
                DomainRouterVO nextRouter = null;
                if (userVm.getDomainRouterId() != null)
                    nextRouter = _routerMgr.getRouter(userVm.getDomainRouterId());
                if (nextRouter == null) {
                    s_logger.warn("Unable to find router (" + userVm.getDomainRouterId() + ") for virtual machine with id " + instanceId);
                    throw new InvalidParameterValueException("Unable to find router (" + userVm.getDomainRouterId() + ") for virtual machine with id " + instanceId);
                }

                if (router == null) {
                    router = nextRouter;

                    // Make sure owner of router is owner of load balancer.  Since we are already checking that all VMs belong to the same router, by checking router
                    // ownership once we'll make sure all VMs belong to the owner of the load balancer.
                    if (router.getAccountId() != loadBalancer.getAccountId()) {
                        throw new InvalidParameterValueException("guest vm " + userVm.getName() + " (id:" + userVm.getId() + ") does not belong to the owner of load balancer " +
                                loadBalancer.getName() + " (owner is account id " + loadBalancer.getAccountId() + ")");
                    }
                } else if (router.getId() != nextRouter.getId()) {
                    throw new InvalidParameterValueException("guest vm " + userVm.getName() + " (id:" + userVm.getId() + ") belongs to router " + nextRouter.getName()
                            + ", previous vm in list belongs to router " + router.getName());
                }

                // check for ip address/port conflicts by checking exising forwarding and loadbalancing rules
                String ipAddress = loadBalancer.getIpAddress();
                String privateIpAddress = userVm.getGuestIpAddress();
                List<FirewallRuleVO> existingRulesOnPubIp = _rulesDao.listIPForwarding(ipAddress);

                if (existingRulesOnPubIp != null) {
                    for (FirewallRuleVO fwRule : existingRulesOnPubIp) {
                        if (!(  (fwRule.isForwarding() == false) &&
                                (fwRule.getGroupId() != null) &&
                                (fwRule.getGroupId() == loadBalancer.getId().longValue())  )) {
                            // if the rule is not for the current load balancer, check to see if the private IP is our target IP,
                            // in which case we have a conflict
                            if (fwRule.getPublicPort().equals(loadBalancer.getPublicPort())) {
                                throw new NetworkRuleConflictException("An existing port forwarding service rule for " + ipAddress + ":" + loadBalancer.getPublicPort()
                                        + " exists, found while trying to apply load balancer " + loadBalancer.getName() + " (id:" + loadBalancer.getId() + ") to instance "
                                        + userVm.getName() + ".");
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
                return;
            }

            IPAddressVO ipAddr = _ipAddressDao.findById(loadBalancer.getIpAddress());
            List<IPAddressVO> ipAddrs = listPublicIpAddressesInVirtualNetwork(accountId, ipAddr.getDataCenterId(), null);
            for (IPAddressVO ipv : ipAddrs) {
                List<FirewallRuleVO> rules = _rulesDao.listIPForwarding(ipv.getAddress(), false);
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
                loadBalancerLock = _loadBalancerDao.acquire(loadBalancerId);
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
                        if (updatedRule.getId() == null) {
                            _rulesDao.persist(updatedRule);

                            description = "created new " + ruleName + " rule [" + updatedRule.getPublicIpAddress() + ":"
                                    + updatedRule.getPublicPort() + "]->[" + updatedRule.getPrivateIpAddress() + ":"
                                    + updatedRule.getPrivatePort() + "]" + " " + updatedRule.getProtocol();

                            EventUtils.saveEvent(UserContext.current().getUserId(), loadBalancer.getAccountId(), level, type, description);
                        }
                    }
                } else {
                    // Remove the instanceIds from the load balancer since there was a failure.  Make sure to commit the
                    // transaction here, otherwise the act of throwing the internal error exception will cause this
                    // remove operation to be rolled back.
                    _loadBalancerVMMapDao.remove(loadBalancerId, instanceIds, null);
                    txn.commit();

                    s_logger.warn("Failed to apply load balancer " + loadBalancer.getName() + " (id:" + loadBalancerId + ") to guest virtual machines " + StringUtils.join(instanceIds, ","));
                    throw new InternalErrorException("Failed to apply load balancer " + loadBalancer.getName() + " (id:" + loadBalancerId + ") to guest virtual machine " + StringUtils.join(instanceIds, ","));
                }
            } finally {
                if (loadBalancerLock != null) {
                    _loadBalancerDao.release(loadBalancerId);
                }
            }

            txn.commit();
        } catch (Throwable e) {
            txn.rollback();
            if (e instanceof NetworkRuleConflictException) {
                throw (NetworkRuleConflictException) e;
            } else if (e instanceof InvalidParameterValueException) {
                throw (InvalidParameterValueException) e;
            } else if (e instanceof PermissionDeniedException) {
                throw (PermissionDeniedException) e;
            } else if (e instanceof InternalErrorException) {
                s_logger.warn("ManagementServer error", e);
                throw (InternalErrorException) e;
            }
            s_logger.warn("ManagementServer error", e);
        }
    }

    @Override @DB
    public LoadBalancerVO createLoadBalancerRule(CreateLoadBalancerRuleCmd cmd) throws InvalidParameterValueException, PermissionDeniedException {
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
                FirewallRuleVO existingFwRule = existingFwRules.get(0);
                String securityGroupName = null;
                if (existingFwRule.getGroupId() != null) {
                    long groupId = existingFwRule.getGroupId();
                    SecurityGroupVO securityGroup = _securityGroupDao.findById(groupId);
                    securityGroupName = securityGroup.getName();
                }
                throw new InvalidParameterValueException("IP Address (" + publicIp + ") and port (" + publicPort + ") already in use" +
                        ((securityGroupName == null) ? "" : " by port forwarding service " + securityGroupName));
            }

            ipAddr = _ipAddressDao.acquire(publicIp);
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
                _ipAddressDao.release(publicIp);
            }
        }
    }

    @Override @DB
    public boolean releasePublicIpAddress(long userId, final String ipAddress) {
        IPAddressVO ip = null;
        try {
            ip = _ipAddressDao.acquire(ipAddress);
            
            if (ip == null) {
                s_logger.warn("Unable to find allocated ip: " + ipAddress);
                return false;
            }

            if(s_logger.isDebugEnabled())
            	s_logger.debug("lock on ip " + ipAddress + " is acquired");
            
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

            // We've deleted all the rules for the given public IP, so remove any security group mappings for that public IP
            List<SecurityGroupVMMapVO> securityGroupMappings = _securityGroupVMMapDao.listByIp(ipAddress);
            for (SecurityGroupVMMapVO securityGroupMapping : securityGroupMappings) {
                _securityGroupVMMapDao.remove(securityGroupMapping.getId());

                // save off an event for removing the security group
                EventVO event = new EventVO();
                event.setUserId(userId);
                event.setAccountId(ip.getAccountId());
                event.setType(EventTypes.EVENT_PORT_FORWARDING_SERVICE_REMOVE);
                String params = "sgId="+securityGroupMapping.getId()+"\nvmId="+securityGroupMapping.getInstanceId();
                event.setParameters(params);
                event.setDescription("Successfully removed security group " + Long.valueOf(securityGroupMapping.getSecurityGroupId()).toString() + " from virtual machine " + Long.valueOf(securityGroupMapping.getInstanceId()).toString());
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
                event.setDescription("Successfully deleted load balancer " + loadBalancer.getId().toString());
                event.setLevel(EventVO.LEVEL_INFO);
                _eventDao.persist(event);
            }

            if ((router != null) && (router.getState() == State.Running)) {
            	if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Disassociate ip " + router.getName());
                }

                if (associateIP(router, ipAddrs, false, 0)) {
                    _ipAddressDao.unassignIpAddress(ipAddress);
                } else {
                	if (s_logger.isDebugEnabled()) {
                		s_logger.debug("Unable to dissociate IP : " + ipAddress + " due to failing to dissociate with router: " + router.getName());
                	}

                	final EventVO event = new EventVO();
                    event.setUserId(userId);
                    event.setAccountId(ip.getAccountId());
                    event.setType(EventTypes.EVENT_NET_IP_RELEASE);
                    event.setLevel(EventVO.LEVEL_ERROR);
                    event.setParameters("address=" + ipAddress + "\nsourceNat="+ip.isSourceNat());
                    event.setDescription("failed to released a public ip: " + ipAddress + " due to failure to disassociate with router " + router.getName());
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
	            if(s_logger.isDebugEnabled())
	            	s_logger.debug("Releasing lock on ip " + ipAddress);
	            _ipAddressDao.release(ipAddress);
        	}
        }
    }

    @Override
    public DomainRouterVO getRouter(final long routerId) {
        return _routerMgr.getRouter(routerId);
    }

    @Override
    public List<? extends DomainRouter> getRouters(final long hostId) {
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

    @Override
    public boolean configure(final String name, final Map<String, Object> params) throws ConfigurationException {
        _name = name;

        final Map<String, String> configs = _configDao.getConfiguration("AgentManager", params);

        NetworkOfferingVO publicNetworkOffering = new NetworkOfferingVO(NetworkOfferingVO.SystemVmPublicNetwork, TrafficType.Public, null);
        publicNetworkOffering = _networkOfferingDao.persistSystemNetworkOffering(publicNetworkOffering);
        _systemNetworks.put(NetworkOfferingVO.SystemVmPublicNetwork, publicNetworkOffering);
        NetworkOfferingVO managementNetworkOffering = new NetworkOfferingVO(NetworkOfferingVO.SystemVmManagementNetwork, TrafficType.Management, null);
        managementNetworkOffering = _networkOfferingDao.persistSystemNetworkOffering(managementNetworkOffering);
        _systemNetworks.put(NetworkOfferingVO.SystemVmManagementNetwork, managementNetworkOffering);
        NetworkOfferingVO controlNetworkOffering = new NetworkOfferingVO(NetworkOfferingVO.SystemVmControlNetwork, TrafficType.Control, null);
        controlNetworkOffering = _networkOfferingDao.persistSystemNetworkOffering(controlNetworkOffering);
        _systemNetworks.put(NetworkOfferingVO.SystemVmControlNetwork, controlNetworkOffering);
        NetworkOfferingVO guestNetworkOffering = new NetworkOfferingVO(NetworkOfferingVO.SystemVmGuestNetwork, TrafficType.Guest, GuestIpType.Virtualized);
        guestNetworkOffering = _networkOfferingDao.persistSystemNetworkOffering(guestNetworkOffering);
        _systemNetworks.put(NetworkOfferingVO.SystemVmGuestNetwork, guestNetworkOffering);
        NetworkOfferingVO storageNetworkOffering = new NetworkOfferingVO(NetworkOfferingVO.SystemVmStorageNetwork, TrafficType.Storage, null);
        storageNetworkOffering = _networkOfferingDao.persistSystemNetworkOffering(storageNetworkOffering);
        _systemNetworks.put(NetworkOfferingVO.SystemVmGuestNetwork, storageNetworkOffering);
        
        AccountsUsingNetworkConfigurationSearch = _accountDao.createSearchBuilder();
        SearchBuilder<NetworkAccountVO> networkAccountSearch = _networkConfigDao.createSearchBuilderForAccount();
        AccountsUsingNetworkConfigurationSearch.join("nc", networkAccountSearch, AccountsUsingNetworkConfigurationSearch.entity().getId(), networkAccountSearch.entity().getAccountId(), JoinType.INNER);
        networkAccountSearch.and("config", networkAccountSearch.entity().getNetworkConfigurationId(), SearchCriteria.Op.EQ);
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
    public List<NetworkConfigurationVO> setupNetworkConfiguration(Account owner, NetworkOfferingVO offering, DeploymentPlan plan) {
        return setupNetworkConfiguration(owner, offering, null, plan);
    }
    
    @Override
    public List<NetworkConfigurationVO> setupNetworkConfiguration(Account owner, NetworkOfferingVO offering, NetworkConfiguration predefined, DeploymentPlan plan) {
        List<NetworkConfigurationVO> configs = _networkConfigDao.listBy(owner.getId(), offering.getId(), plan.getDataCenterId());
        if (configs.size() > 0) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Found existing network configuration for offering " + offering + ": " + configs.get(0));
            }
            return configs;
        }
        
        configs = new ArrayList<NetworkConfigurationVO>();
        
        long related = -1;
        
        for (NetworkGuru guru : _networkGurus) {
            NetworkConfiguration config = guru.design(offering, plan, predefined, owner);
            if (config == null) {
                continue;
            }
            
            if (config.getId() != -1) {
                if (config instanceof NetworkConfigurationVO) {
                    configs.add((NetworkConfigurationVO)config);
                } else {
                    configs.add(_networkConfigDao.findById(config.getId()));
                }
                continue;
            }
            
            long id = _networkConfigDao.getNextInSequence(Long.class, "id");
            if (related == -1) {
                related = id;
            } 
            
            NetworkConfigurationVO vo = new NetworkConfigurationVO(id, config, offering.getId(), plan.getDataCenterId(), guru.getName(), owner.getDomainId(), owner.getId(), related);
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
    public List<NicProfile> allocate(VirtualMachineProfile vm, List<Pair<NetworkConfigurationVO, NicProfile>> networks) throws InsufficientCapacityException {
        List<NicProfile> nicProfiles = new ArrayList<NicProfile>(networks.size());
        
        Transaction txn = Transaction.currentTxn();
        txn.start();
        
        int deviceId = 0;
        
        boolean[] deviceIds = new boolean[networks.size()];
        Arrays.fill(deviceIds, false);
        
        List<NicVO> nics = new ArrayList<NicVO>(networks.size());
        NicVO defaultNic = null;
        
        for (Pair<NetworkConfigurationVO, NicProfile> network : networks) {
            NetworkConfigurationVO config = network.first();
            NetworkGuru concierge = _networkGurus.get(config.getGuruName());
            NicProfile requested = network.second();
            NicProfile profile = concierge.allocate(config, requested, vm);
            if (profile == null) {
                continue;
            }
            NicVO vo = new NicVO(concierge.getName(), vm.getId(), config.getId());
            vo.setMode(network.first().getMode());
            
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
            nicProfiles.add(new NicProfile(vo, network.first(), vo.getBroadcastUri(), vo.getIsolationUri()));
        }
        
        if (defaultNic == null && nics.size() > 2) {
            throw new IllegalArgumentException("Default Nic was not set.");
        } else if (nics.size() == 1) {
            nics.get(0).setDefaultNic(true);
        }
        
        txn.commit();
        
        return nicProfiles;
    }
    
    protected Integer applyProfileToNic(NicVO vo, NicProfile profile, Integer deviceId) {
        if (profile.getDeviceId() != null) {
            vo.setDeviceId(profile.getDeviceId());
        } else if (deviceId != null ) {
            vo.setDeviceId(deviceId++);
        }
        
        vo.setDefaultNic(profile.isDefaultNic());
        
        if (profile.getIp4Address() != null) {
            vo.setIp4Address(profile.getIp4Address());
            vo.setState(NicVO.State.Reserved);
        }
        
        if (profile.getMacAddress() != null) {
            vo.setMacAddress(profile.getMacAddress());
        }
        
        vo.setMode(profile.getMode());
        vo.setNetmask(profile.getNetmask());
        vo.setGateway(profile.getGateway());
        
        return deviceId;
    }
    
    protected NicTO toNicTO(NicVO nic, NicProfile profile, NetworkConfigurationVO config) {
        NicTO to = new NicTO();
        to.setDeviceId(nic.getDeviceId());
        to.setBroadcastType(config.getBroadcastDomainType());
        to.setType(config.getTrafficType());
        to.setIp(nic.getIp4Address());
        to.setNetmask(nic.getNetmask());
        to.setMac(nic.getMacAddress());
        if (config.getDns() != null) {
            String[] tokens = config.getDns().split(",");
            to.setDns1(tokens[0]);
            if (tokens.length > 2) {
                to.setDns2(tokens[1]);
            }
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
    protected Pair<NetworkGuru, NetworkConfigurationVO> implementNetworkConfiguration(long configId, DeployDestination dest) throws ConcurrentOperationException {
        Transaction txn = Transaction.currentTxn();
        NetworkConfigurationVO config = _networkConfigDao.acquire(configId);
        if (config == null) {
            throw new ConcurrentOperationException("Unable to acquire network configuration: " + configId);
        }
        
        try {
            NetworkGuru guru = _networkGurus.get(config.getGuruName());
            if (config.getState() == NetworkConfiguration.State.Implemented || config.getState() == NetworkConfiguration.State.Setup) {
                return new Pair<NetworkGuru, NetworkConfigurationVO>(guru, config);
            }
            
            
            NetworkConfiguration result = guru.implement(config, _networkOfferingDao.findById(config.getNetworkOfferingId()), dest);
            config.setCidr(result.getCidr());
            config.setBroadcastUri(result.getBroadcastUri());
            config.setGateway(result.getGateway());
            config.setDns(result.getDns());
            config.setMode(result.getMode());
            config.setState(NetworkConfiguration.State.Implemented);
            _networkConfigDao.update(configId, config);
            
            return new Pair<NetworkGuru, NetworkConfigurationVO>(guru, config);
        } finally {
            _networkConfigDao.release(configId);
        }
    }
    
    @Override
    public NicTO[] prepare(VirtualMachineProfile vmProfile, DeployDestination dest, Account user) throws InsufficientAddressCapacityException, InsufficientVirtualNetworkCapcityException, ConcurrentOperationException, ResourceUnavailableException {
        List<NicVO> nics = _nicDao.listBy(vmProfile.getId());
        NicTO[] nicTos = new NicTO[nics.size()];
        int i = 0;
        for (NicVO nic : nics) {
            Pair<NetworkGuru, NetworkConfigurationVO> implemented = implementNetworkConfiguration(nic.getNetworkConfigurationId(), dest);
            NetworkGuru concierge = implemented.first();
            NetworkConfigurationVO config = implemented.second();
            NicProfile profile = null;
            if (nic.getReservationStrategy() == ReservationStrategy.Start) {
                nic.setState(Resource.State.Reserving);
                _nicDao.update(nic.getId(), nic);
                profile = toNicProfile(nic);
                String reservationId = concierge.reserve(profile, config, vmProfile, dest);
                nic.setIp4Address(profile.getIp4Address());
                nic.setIp6Address(profile.getIp6Address());
                nic.setMacAddress(profile.getMacAddress());
                nic.setIsolationUri(profile.getIsolationUri());
                nic.setBroadcastUri(profile.getBroadCastUri());
                nic.setReservationId(reservationId);
                nic.setReserver(concierge.getName());
                nic.setState(Resource.State.Reserved);
                nic.setNetmask(profile.getNetmask());
                nic.setGateway(profile.getGateway());
                nic.setAddressFormat(profile.getFormat());
                _nicDao.update(nic.getId(), nic);
                for (NetworkElement element : _networkElements) {
                    if (!element.prepare(config, profile, vmProfile, null, user)) {
                        s_logger.warn("Unable to prepare " + nic + " for element " + element.getName());
                        return null;
                    }
                }
            }
            
            nicTos[i++] = toNicTO(nic, profile, config);
            
        }
        return nicTos;
    }
    
    @Override
    public void release(VirtualMachineProfile vmProfile) {
        List<NicVO> nics = _nicDao.listBy(vmProfile.getId());
        for (NicVO nic : nics) {
            NetworkConfigurationVO config = _networkConfigDao.findById(nic.getNetworkConfigurationId());
            if (nic.getReservationStrategy() == ReservationStrategy.Start) {
                NetworkGuru concierge = _networkGurus.get(config.getGuruName());
                nic.setState(Resource.State.Releasing);
                _nicDao.update(nic.getId(), nic);
                concierge.release(nic.getReservationId());
            }
        }
    }
    
    NicProfile toNicProfile(NicVO nic) {
        NetworkConfiguration config = _networkConfigDao.findById(nic.getNetworkConfigurationId());
        NicProfile profile = new NicProfile(nic, config, nic.getBroadcastUri(), nic.getIsolationUri());
        return profile;
    }
    
    public void release(long vmId) {
        List<NicVO> nics = _nicDao.listBy(vmId);
        
        for (NicVO nic : nics) {
            nic.setState(Resource.State.Releasing);
            _nicDao.update(nic.getId(), nic);
            NetworkGuru concierge = _networkGurus.get(nic.getReserver());
            if (!concierge.release(nic.getReservationId())) {
                s_logger.warn("Unable to release " + nic + " using " + concierge.getName());
            }
            nic.setState(Resource.State.Allocated);
            _nicDao.update(nic.getId(), nic);
        }
    }
    
    @Override
    public <K extends VMInstanceVO> List<NicVO> getNics(K vm) {
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
            loadBalancerLock = _loadBalancerDao.acquire(loadBalancerId);
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
                _loadBalancerDao.release(loadBalancerId);
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
                loadBalancerLock = _loadBalancerDao.acquire(loadBalancerId);
                if (loadBalancerLock == null) {
                    s_logger.warn("deleteLoadBalancer: failed to lock load balancer " + loadBalancerId + ", deleting mappings anyway...");
                }

                // remove all loadBalancer->VM mappings
                _loadBalancerVMMapDao.remove(loadBalancerId);

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
                _loadBalancerDao.release(loadBalancerId);
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
            if ((existingLB != null) && (existingLB.getId().longValue() != loadBalancer.getId().longValue())) {
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

	@Override
	public boolean deleteNetworkRuleConfig(DeletePortForwardingServiceRuleCmd cmd) throws PermissionDeniedException {
        Long userId = UserContext.current().getUserId();
        Long netRuleId = cmd.getId();
        Account account = UserContext.current().getAccount();

        //If command is executed via 8096 port, set userId to the id of System account (1)
        if (userId == null) {
            userId = Long.valueOf(1);
        }

        // do a quick permissions check to make sure the account is either an
        // admin or the owner of the security group to which the network rule
        // belongs
        NetworkRuleConfigVO netRule = _networkRuleConfigDao.findById(netRuleId);
        if (netRule != null) {
            SecurityGroupVO sg = _securityGroupDao.findById(netRule.getSecurityGroupId());
            if ((account == null) || BaseCmd.isAdmin(account.getType())) {
                if ((account != null) && !_domainDao.isChildDomain(account.getDomainId(), sg.getDomainId())) {
                    throw new PermissionDeniedException("Unable to delete port forwarding service rule " + netRuleId + "; account: " + account.getAccountName() + " is not an admin in the domain hierarchy.");
                }
            } else {
                if (sg.getAccountId() != account.getId()) {
                    throw new PermissionDeniedException("Unable to delete port forwarding service rule " + netRuleId + "; account: " + account.getAccountName() + " is not the owner");
                }
            }
        } else {
            return false;  // failed to delete due to netRule not found
        }

        return deleteNetworkRuleConfigInternal(userId, netRuleId);
	}

    private boolean deleteNetworkRuleConfigInternal(long userId, long networkRuleId) {
        try {
            NetworkRuleConfigVO netRule = _networkRuleConfigDao.findById(networkRuleId);
            if (netRule != null) {
                List<SecurityGroupVMMapVO> sgMappings = _securityGroupVMMapDao.listBySecurityGroup(netRule.getSecurityGroupId());
                if ((sgMappings != null) && !sgMappings.isEmpty()) {
                    for (SecurityGroupVMMapVO sgMapping : sgMappings) {
                        UserVm userVm = _userVmDao.findById(sgMapping.getInstanceId());
                        if (userVm != null) {
                            List<FirewallRuleVO> fwRules = _firewallRulesDao.listIPForwarding(sgMapping.getIpAddress(), netRule.getPublicPort(), true);
                            FirewallRuleVO rule = null;
                            for (FirewallRuleVO fwRule : fwRules) {
                                if (fwRule.getPrivatePort().equals(netRule.getPrivatePort()) && fwRule.getPrivateIpAddress().equals(userVm.getGuestIpAddress())) {
                                    rule = fwRule;
                                    break;
                                }
                            }

                            if (rule != null) {
                                rule.setEnabled(false);
                                updateFirewallRule(rule, null, null);

                                // Save and create the event
                                Account account = _accountDao.findById(userVm.getAccountId());

                                _firewallRulesDao.remove(rule.getId());
                                String description = "deleted ip forwarding rule [" + rule.getPublicIpAddress() + ":" + rule.getPublicPort() + "]->[" + rule.getPrivateIpAddress()
                                                     + ":" + rule.getPrivatePort() + "]" + " " + rule.getProtocol();

                                EventUtils.saveEvent(Long.valueOf(userId), account.getId(), EventVO.LEVEL_INFO, EventTypes.EVENT_NET_RULE_DELETE, description);
                            }
                        }
                    }
                }
                _networkRuleConfigDao.remove(netRule.getId());
            }
        } catch (Exception ex) {
            s_logger.error("Unexpected exception deleting port forwarding service rule " + networkRuleId, ex);
            return false;
        }

        return true;
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
            if (success)
            	_accountMgr.decrementResourceCount(accountId, ResourceType.public_ip);
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
    public boolean deleteIpForwardingRule(DeleteIPForwardingRuleCmd cmd) throws PermissionDeniedException, InvalidParameterValueException {
    	Long ruleId = cmd.getId();
    	Long userId = UserContext.current().getUserId();
    	Account account = UserContext.current().getAccount();
    	
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
        	
            IPAddressVO ipVO = _ipAddressDao.acquire(publicIp);
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
                throw new InternalErrorException("Multiple matches. Please contact support");
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
              _ipAddressDao.release(publicIp);
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
    public List<NetworkConfigurationVO> getNetworkConfigurationsforOffering(long offeringId, long dataCenterId, long accountId) {
        return _networkConfigDao.getNetworkConfigurationsForOffering(offeringId, dataCenterId, accountId);
    }
    
    @Override
    public List<NetworkConfigurationVO> setupNetworkConfiguration(Account owner, ServiceOfferingVO offering, DeploymentPlan plan) {
        NetworkOfferingVO networkOffering = _networkOfferingDao.findByServiceOffering(offering);
        return setupNetworkConfiguration(owner, networkOffering, plan);
    }
    
}
