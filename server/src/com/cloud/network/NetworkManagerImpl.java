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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.ejb.Local;
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.CheckVirtualMachineAnswer;
import com.cloud.agent.api.CheckVirtualMachineCommand;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.CreateZoneVlanAnswer;
import com.cloud.agent.api.CreateZoneVlanCommand;
import com.cloud.agent.api.MigrateCommand;
import com.cloud.agent.api.ModifySshKeysCommand;
import com.cloud.agent.api.NetworkUsageAnswer;
import com.cloud.agent.api.NetworkUsageCommand;
import com.cloud.agent.api.PrepareForMigrationCommand;
import com.cloud.agent.api.RebootAnswer;
import com.cloud.agent.api.RebootRouterCommand;
import com.cloud.agent.api.StartRouterAnswer;
import com.cloud.agent.api.StartRouterCommand;
import com.cloud.agent.api.StopCommand;
import com.cloud.agent.api.routing.DhcpEntryCommand;
import com.cloud.agent.api.routing.IPAssocCommand;
import com.cloud.agent.api.routing.LoadBalancerCfgCommand;
import com.cloud.agent.api.routing.SavePasswordCommand;
import com.cloud.agent.api.routing.SetFirewallRuleCommand;
import com.cloud.agent.api.routing.VmDataCommand;
import com.cloud.agent.api.to.NicTO;
import com.cloud.agent.manager.AgentManager;
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
import com.cloud.async.AsyncJobExecutor;
import com.cloud.async.AsyncJobManager;
import com.cloud.async.AsyncJobVO;
import com.cloud.async.BaseAsyncJobExecutor;
import com.cloud.capacity.dao.CapacityDao;
import com.cloud.configuration.Config;
import com.cloud.configuration.ConfigurationManager;
import com.cloud.configuration.ResourceCount.ResourceType;
import com.cloud.configuration.dao.ConfigurationDao;
import com.cloud.configuration.dao.ResourceLimitDao;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.HostPodVO;
import com.cloud.dc.Vlan;
import com.cloud.dc.Vlan.VlanType;
import com.cloud.dc.VlanVO;
import com.cloud.dc.dao.AccountVlanMapDao;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.HostPodDao;
import com.cloud.dc.dao.VlanDao;
import com.cloud.deploy.DeployDestination;
import com.cloud.deploy.DeploymentPlan;
import com.cloud.domain.DomainVO;
import com.cloud.domain.dao.DomainDao;
import com.cloud.event.EventState;
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
import com.cloud.exception.StorageUnavailableException;
import com.cloud.ha.HighAvailabilityManager;
import com.cloud.host.Host;
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
import com.cloud.offering.NetworkOffering;
import com.cloud.offering.NetworkOffering.GuestIpType;
import com.cloud.offerings.NetworkOfferingVO;
import com.cloud.offerings.dao.NetworkOfferingDao;
import com.cloud.resource.Resource;
import com.cloud.resource.Resource.ReservationStrategy;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.storage.GuestOSVO;
import com.cloud.storage.StorageManager;
import com.cloud.storage.StoragePoolVO;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.VolumeVO;
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
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.Pair;
import com.cloud.utils.StringUtils;
import com.cloud.utils.component.Adapters;
import com.cloud.utils.component.ComponentLocator;
import com.cloud.utils.component.Inject;
import com.cloud.utils.concurrency.NamedThreadFactory;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.JoinBuilder;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.exception.ExecutionException;
import com.cloud.utils.net.NetUtils;
import com.cloud.vm.DomainRouter;
import com.cloud.vm.DomainRouter.Role;
import com.cloud.vm.DomainRouterVO;
import com.cloud.vm.NicProfile;
import com.cloud.vm.NicVO;
import com.cloud.vm.State;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachine.Event;
import com.cloud.vm.VirtualMachineManager;
import com.cloud.vm.VirtualMachineName;
import com.cloud.vm.VirtualMachineProfile;
import com.cloud.vm.dao.DomainRouterDao;
import com.cloud.vm.dao.NicDao;
import com.cloud.vm.dao.UserVmDao;

/**
 * NetworkManagerImpl implements NetworkManager.
 */
@Local(value={NetworkManager.class})
public class NetworkManagerImpl implements NetworkManager, VirtualMachineManager<DomainRouterVO> {
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
    @Inject DomainRouterDao _routerDao = null;
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
    @Inject HighAvailabilityManager _haMgr;
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
    @Inject NetworkConfigurationDao _networkProfileDao = null;
    @Inject NicDao _nicDao;
    @Inject GuestOSDao _guestOSDao = null;
    
    @Inject(adapter=NetworkGuru.class)
    Adapters<NetworkGuru> _networkGurus;
    @Inject(adapter=NetworkElement.class)
    Adapters<NetworkElement> _networkElements;

    long _routerTemplateId = -1;
    int _routerRamSize;
    // String _privateNetmask;
    int _retry = 2;
    String _domain;
    String _instance;
    int _routerCleanupInterval = 3600;
    int _routerStatsInterval = 300;
    private ServiceOfferingVO _offering;
    private int _networkRate;
    private int _multicastRate;
    private HashMap<String, NetworkOfferingVO> _systemNetworks = new HashMap<String, NetworkOfferingVO>(5);
    
    private VMTemplateVO _template;
    
    ScheduledExecutorService _executor;
	
	@Override
	public boolean destroy(DomainRouterVO router) {
		return destroyRouter(router.getId());
	}

    @Override
    public boolean sendSshKeysToHost(Long hostId, String pubKey, String prvKey) {
    	ModifySshKeysCommand cmd = new ModifySshKeysCommand(pubKey, prvKey);
    	final Answer answer = _agentMgr.easySend(hostId, cmd);
    	
    	if (answer != null)
    		return true;
    	else
    		return false;
    }
    
    @Override @DB
    public String assignSourceNatIpAddress(AccountVO account, final DataCenterVO dc, final String domain, final ServiceOfferingVO serviceOffering, long startEventId, HypervisorType hyperType) throws ResourceAllocationException {
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
 
        final AccountVO account = _accountDao.findById(accountId);
        boolean podVlan = guestVlan.getVlanType().equals(VlanType.DirectAttached) && guestVlan.getVlanId().equals(Vlan.UNTAGGED);
        long accountIdForDHCPServer = podVlan ? Account.ACCOUNT_ID_SYSTEM : accountId;
        long domainIdForDHCPServer = podVlan ? DomainVO.ROOT_DOMAIN : account.getDomainId();
        String domainNameForDHCPServer = podVlan ? "root" : _domainDao.findById(account.getDomainId()).getName();

        final VMTemplateVO rtrTemplate = _templateDao.findRoutingTemplate();
        
        final Transaction txn = Transaction.currentTxn();
        DomainRouterVO router = null;
        Long podId = pod.getId();
        pod = _podDao.acquire(podId, 20*60);
        if (pod == null) {
            	throw new ConcurrentOperationException("Unable to acquire lock on pod " + podId );
        }
        if(s_logger.isDebugEnabled())
        	s_logger.debug("Lock on pod " + podId + " is acquired");
        
        final long id = _routerDao.getNextInSequence(Long.class, "id");
        final String[] macAddresses = _dcDao.getNextAvailableMacAddressPair(dc.getId());
        final String mgmtMacAddress = macAddresses[0];
        final String guestMacAddress = macAddresses[1];
        final String name = VirtualMachineName.getRouterName(id, _instance).intern();

        boolean routerLockAcquired = false;
        try {
            List<DomainRouterVO> rtrs = _routerDao.listByVlanDbId(guestVlan.getId());
            assert rtrs.size() < 2 : "How did we get more than one router per vlan?";
            if (rtrs.size() == 1) {
            	return rtrs.get(0);
            }
            String mgmtNetmask = NetUtils.getCidrNetmask(pod.getCidrSize());
            final String guestIp = _ipAddressDao.assignIpAddress(accountIdForDHCPServer, domainIdForDHCPServer, guestVlan.getId(), false);

            router =
                new DomainRouterVO(id,
                        _offering.getId(),
                        name,
                        mgmtMacAddress,
                        null,
                        mgmtNetmask,
                        _routerTemplateId,
                        rtrTemplate.getGuestOSId(),
                        guestMacAddress,
                        guestIp,
                        guestVlan.getVlanNetmask(),
                        accountIdForDHCPServer,
                        domainIdForDHCPServer,
                        "FE:FF:FF:FF:FF:FF",
                        null,
                        "255.255.255.255",
                        guestVlan.getId(),
                        guestVlan.getVlanId(),
                        pod.getId(),
                        dc.getId(),
                        _routerRamSize,
                        guestVlan.getVlanGateway(),
                        domainNameForDHCPServer,
                        dc.getDns1(),
                        dc.getDns2());
            router.setRole(Role.DHCP_USERDATA);
            router.setVnet(guestVlan.getVlanId());

            router.setLastHostId(candidateHost);
            
            txn.start();
            router = _routerDao.persist(router);
            router = _routerDao.acquire(router.getId());
            if (router == null) {
                s_logger.debug("Unable to acquire lock on router " + id);
                throw new CloudRuntimeException("Unable to acquire lock on router " + id);
            }
            
            routerLockAcquired = true;
            
            txn.commit();

            List<VolumeVO> vols = _storageMgr.create(account, router, rtrTemplate, dc, pod, _offering, null,0);
            if (vols == null){
            	_ipAddressDao.unassignIpAddress(guestIp);
            	_routerDao.expunge(router.getId());
            	if (s_logger.isDebugEnabled()) {
            		s_logger.debug("Unable to create dhcp server in storage host or pool in pod " + pod.getName() + " (id:" + pod.getId() + ")");
            	}
            }

            final EventVO event = new EventVO();
            event.setUserId(userId);
            event.setAccountId(accountIdForDHCPServer);
            event.setType(EventTypes.EVENT_ROUTER_CREATE);

            if (vols == null) {
                event.setDescription("failed to create DHCP Server : " + router.getName());
                event.setLevel(EventVO.LEVEL_ERROR);
                _eventDao.persist(event);
                throw new ExecutionException("Unable to create DHCP Server");
            }
            _routerDao.updateIf(router, Event.OperationSucceeded, null);

            s_logger.info("DHCP server created: id=" + router.getId() + "; name=" + router.getName() + "; vlan=" + guestVlan.getVlanId() + "; pod=" + pod.getName());

            event.setDescription("successfully created DHCP Server : " + router.getName() + " with ip : " + router.getGuestIpAddress());
            _eventDao.persist(event);

            return router;
        } catch (final Throwable th) {
            if (th instanceof ExecutionException) {
                s_logger.error("Error while starting router due to " + th.getMessage());
            } else {
                s_logger.error("Unable to create router", th);
            }
            txn.rollback();

            if (router.getState() == State.Creating) {
                _routerDao.expunge(router.getId());
            }
            return null;
        } finally {
            if (routerLockAcquired) {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Releasing lock on router " + id);
                }
                _routerDao.release(id);
            }
            if (pod != null) {
                if(s_logger.isDebugEnabled())
                	s_logger.debug("Releasing lock on pod " + podId);
            	_podDao.release(pod.getId());
            }
        }
	}

	@Override
    public DomainRouterVO assignRouter(final long userId, final long accountId, final long dataCenterId, final long podId, final String domain, final String instance) throws InsufficientCapacityException {
        return null;
    }

    @Override
    public boolean releaseRouter(final long routerId) {
        return destroyRouter(routerId);
    }
    
    @Override @DB
    public DomainRouterVO createRouter(final long accountId, final String publicIpAddress, final long dataCenterId,  
                                       String domain, final ServiceOfferingVO offering, long startEventId) 
                                       throws ConcurrentOperationException {
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Creating a router for account=" + accountId + "; publicIpAddress=" + publicIpAddress + "; dc=" + dataCenterId + "domain=" + domain);
        }
        
        final AccountVO account = _accountDao.acquire(accountId);
        if (account == null) {
        	throw new ConcurrentOperationException("Unable to acquire account " + accountId);
        }
        
        if(s_logger.isDebugEnabled())
        	s_logger.debug("lock on account " + accountId + " for createRouter is acquired");

        final Transaction txn = Transaction.currentTxn();
        DomainRouterVO router = null;
        boolean success = false;
        try {
            router = _routerDao.findBy(accountId, dataCenterId);
            if (router != null && router.getState() != State.Creating) {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Router " + router.toString() + " found for account " + accountId + " in data center " + dataCenterId);
                }
                success = true;
                return router;
            }
            EventVO event = new EventVO();
            event.setUserId(1L);
            event.setAccountId(accountId);
            event.setType(EventTypes.EVENT_ROUTER_CREATE);
            event.setState(EventState.Started);
            event.setStartId(startEventId);
            event.setDescription("Creating Router for account with Id: "+accountId);
            event = _eventDao.persist(event);

            final DataCenterVO dc = _dcDao.findById(dataCenterId);
            final VMTemplateVO template = _templateDao.findRoutingTemplate();

            String[] macAddresses = getMacAddressPair(dataCenterId);
            String privateMacAddress = macAddresses[0];
            String publicMacAddress = macAddresses[1];

            final long id = _routerDao.getNextInSequence(Long.class, "id");

            if (domain == null) {
                domain = "v" + Long.toHexString(accountId) + "." + _domain;
            }

            final String name = VirtualMachineName.getRouterName(id, _instance).intern();
            long routerMacAddress = NetUtils.mac2Long(dc.getRouterMacAddress()) | ((dc.getId() & 0xff) << 32);

            //set the guestNetworkCidr from the dc obj
            String guestNetworkCidr = dc.getGuestNetworkCidr();
            String[] cidrTuple = guestNetworkCidr.split("\\/");
            String guestIpAddress = NetUtils.getIpRangeStartIpFromCidr(cidrTuple[0], Long.parseLong(cidrTuple[1]));
            String guestNetmask = NetUtils.getCidrNetmask(Long.parseLong(cidrTuple[1]));

//            String path = null;
//            final int numVolumes = offering.isMirroredVolumes()?2:1;
//            long routerId = 0;

            // Find the VLAN ID, VLAN gateway, and VLAN netmask for publicIpAddress
            IPAddressVO ipVO = _ipAddressDao.findById(publicIpAddress);
            VlanVO vlan = _vlanDao.findById(ipVO.getVlanDbId());
            String vlanId = vlan.getVlanId();
            String vlanGateway = vlan.getVlanGateway();
            String vlanNetmask = vlan.getVlanNetmask();

            Pair<HostPodVO, Long> pod = null;
            Set<Long> avoids = new HashSet<Long>();
            boolean found = false;
            while ((pod = _agentMgr.findPod(template, offering, dc, accountId, avoids)) != null) {
                
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Attempting to create in pod " + pod.first().getName());
                }

                router = new DomainRouterVO(id,
                            _offering.getId(),
                            name,
                            privateMacAddress,
                            null,
                            null,
                            _routerTemplateId,
                            template.getGuestOSId(),
                            NetUtils.long2Mac(routerMacAddress),
                            guestIpAddress,
                            guestNetmask,
                            accountId,
                            account.getDomainId(),
                            publicMacAddress,
                            publicIpAddress,
                            vlanNetmask,
                            vlan.getId(),
                            vlanId,
                            pod.first().getId(),
                            dataCenterId,
                            _routerRamSize,
                            vlanGateway,
                            domain,
                            dc.getDns1(),
                            dc.getDns2());
                router.setMirroredVols(offering.isMirrored());

                router.setLastHostId(pod.second());
                router = _routerDao.persist(router);

                List<VolumeVO> vols = _storageMgr.create(account, router, template, dc, pod.first(), _offering, null,0);
                if(vols != null) {
                    found = true;
                    break;
                }

                _routerDao.expunge(router.getId());
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Unable to find storage host or pool in pod " + pod.first().getName() + " (id:" + pod.first().getId() + "), checking other pods");
                }
                avoids.add(pod.first().getId());
            }
            
            if (!found) {
                event.setDescription("failed to create Domain Router : " + name);
                event.setLevel(EventVO.LEVEL_ERROR);
                _eventDao.persist(event);
                throw new ExecutionException("Unable to create DomainRouter");
            }
            _routerDao.updateIf(router, Event.OperationSucceeded, null);

            s_logger.debug("Router created: id=" + router.getId() + "; name=" + router.getName());
            
            event = new EventVO();
            event.setUserId(1L); // system user performed the action
            event.setAccountId(accountId);
            event.setType(EventTypes.EVENT_ROUTER_CREATE);
            event.setStartId(startEventId);
            event.setDescription("successfully created Domain Router : " + router.getName() + " with ip : " + publicIpAddress);
            _eventDao.persist(event);
            success = true;
            return router;
        } catch (final Throwable th) {
            if (th instanceof ExecutionException) {
                s_logger.error("Error while starting router due to " + th.getMessage());
            } else {
                s_logger.error("Unable to create router", th);
            }
            txn.rollback();

            if (router != null && router.getState() == State.Creating) {
                _routerDao.expunge(router.getId());
            }
            return null;
        } finally {
            if (account != null) {
            	if(s_logger.isDebugEnabled())
            		s_logger.debug("Releasing lock on account " + account.getId() + " for createRouter");
            	_accountDao.release(account.getId());
            }
            if(!success){
                EventVO event = new EventVO();
                event.setUserId(1L); // system user performed the action
                event.setAccountId(accountId);
                event.setType(EventTypes.EVENT_ROUTER_CREATE);
                event.setStartId(startEventId);
                event.setLevel(EventVO.LEVEL_ERROR);
                event.setDescription("Failed to create router for account " + accountId + " in data center " + dataCenterId);
                _eventDao.persist(event);                
            }
        }
    }

    @Override
    public boolean destroyRouter(final long routerId) {
        
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Attempting to destroy router " + routerId);
        }

        DomainRouterVO router = _routerDao.acquire(routerId);

        if (router == null) {
            s_logger.debug("Unable to acquire lock on router " + routerId);
            return false;
        }
        
        EventVO event = new EventVO();
        event.setUserId(User.UID_SYSTEM);
        event.setAccountId(router.getAccountId());
        event.setType(EventTypes.EVENT_ROUTER_DESTROY);
        event.setState(EventState.Started);
        event.setParameters("id=" + routerId);
        event.setDescription("Starting to destroy router : " + router.getName());
        event = _eventDao.persist(event);

        try {
            if (router.getState() == State.Destroyed || router.getState() == State.Expunging || router.getRemoved() != null) {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Unable to find router or router is destroyed: " + routerId);
                }
                return true;
            }

            if (!stop(router, 0)) {
                s_logger.debug("Unable to stop the router: " + routerId);
                return false;
            }
            router = _routerDao.findById(routerId);
            if (!_routerDao.updateIf(router, Event.DestroyRequested, router.getHostId())) {
                s_logger.debug("VM " + router.toString() + " is not in a state to be destroyed.");
                return false;
            }
        } finally {
            if (s_logger.isDebugEnabled())
                s_logger.debug("Release lock on router " + routerId + " for stop");
            _routerDao.release(routerId);
        }
                        
        router.setPublicIpAddress(null);
        router.setVlanDbId(null);
        _routerDao.update(router.getId(), router);
        _routerDao.remove(router.getId());

        List<VolumeVO> vols = _volsDao.findByInstance(routerId);
        _storageMgr.destroy(router, vols);
        
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Successfully destroyed router: " + routerId);
        }
        
        EventVO completedEvent = new EventVO();
        completedEvent.setUserId(User.UID_SYSTEM);
        completedEvent.setAccountId(router.getAccountId());
        completedEvent.setType(EventTypes.EVENT_ROUTER_DESTROY);
        completedEvent.setStartId(event.getId());
        completedEvent.setParameters("id=" + routerId);        
        completedEvent.setDescription("successfully destroyed router : " + router.getName());
        _eventDao.persist(completedEvent);

        return true;
    }
    
    @Override
    @DB
    public boolean upgradeRouter(UpgradeRouterCmd cmd) throws InvalidParameterValueException, PermissionDeniedException {
        Long routerId = cmd.getId();
        Long serviceOfferingId = cmd.getServiceOfferingId();
        Account account = (Account)UserContext.current().getAccountObject();

        DomainRouterVO router = _routerDao.findById(routerId);
        if (router == null) {
            throw new InvalidParameterValueException("Unable to find router with id " + routerId);
        }

        if ((account != null) && !_domainDao.isChildDomain(account.getDomainId(), router.getDomainId())) {
            throw new PermissionDeniedException("Invalid domain router id (" + routerId + ") given, unable to stop router.");
        }

        if (router.getServiceOfferingId() == serviceOfferingId) {
            s_logger.debug("Router: " + routerId + "already has service offering: " + serviceOfferingId);
            return true;
        }

        ServiceOfferingVO newServiceOffering = _serviceOfferingDao.findById(serviceOfferingId);
        if (newServiceOffering == null) {
            throw new InvalidParameterValueException("Unable to find service offering with id " + serviceOfferingId);
        }

        ServiceOfferingVO currentServiceOffering = _serviceOfferingDao.findById(router.getServiceOfferingId());

        if (!currentServiceOffering.getGuestIpType().equals(newServiceOffering.getGuestIpType())) {
            throw new InvalidParameterValueException("Can't upgrade, due to new newtowrk type: " + newServiceOffering.getGuestIpType() + " is different from " +
                    "curruent network type: " + currentServiceOffering.getGuestIpType());
        }
        if (currentServiceOffering.getUseLocalStorage() != newServiceOffering.getUseLocalStorage()) {
            throw new InvalidParameterValueException("Can't upgrade, due to new local storage status : " + newServiceOffering.getGuestIpType() + " is different from " +
                    "curruent local storage status: " + currentServiceOffering.getUseLocalStorage());
        }

    	 router = _routerDao.acquire(routerId);
    	 
    	 router.setServiceOfferingId(serviceOfferingId);
    	 return _routerDao.update(routerId, router);
    }

    private String rot13(final String password) {
        final StringBuffer newPassword = new StringBuffer("");

        for (int i = 0; i < password.length(); i++) {
            char c = password.charAt(i);

            if ((c >= 'a' && c <= 'm') || ((c >= 'A' && c <= 'M'))) {
                c += 13;
            } else if  ((c >= 'n' && c <= 'z') || (c >= 'N' && c <= 'Z')) {
                c -= 13;
            }

            newPassword.append(c);
        }

        return newPassword.toString();
    }

    @Override
    public boolean savePasswordToRouter(final long routerId, final String vmIpAddress, final String password) {

        final DomainRouterVO router = _routerDao.findById(routerId);
        final String routerPrivateIpAddress = router.getPrivateIpAddress();
        final String vmName = router.getName();
        final String encodedPassword = rot13(password);
        final SavePasswordCommand cmdSavePassword = new SavePasswordCommand(encodedPassword, vmIpAddress, routerPrivateIpAddress, vmName);

        if (router != null && router.getHostId() != null) {
            final Answer answer = _agentMgr.easySend(router.getHostId(), cmdSavePassword);
            return (answer != null && answer.getResult());
        } else {
        	// either the router doesn't exist or router isn't running at all
        	return false;
        }
    }

    @Override
    public DomainRouterVO startRouter(final long routerId, long eventId) {
        try {
            return start(routerId, eventId);
        } catch (final StorageUnavailableException e) {
        	s_logger.debug(e.getMessage());
            return null;
        } catch (final ConcurrentOperationException e) {
        	s_logger.debug(e.getMessage());
        	return null;
        }
    }
    
    @Override
    public DomainRouterVO startRouter(StartRouterCmd cmd) throws InvalidParameterValueException, PermissionDeniedException{
    	Long routerId = cmd.getId();
    	Account account = (Account)UserContext.current().getAccountObject();
    	
	    //verify parameters
        DomainRouterVO router = _routerDao.findById(routerId);
        if (router == null) {
        	throw new PermissionDeniedException ("Unable to start router with id " + routerId + ". Permisssion denied");
        }
        if ((account != null) && !_domainDao.isChildDomain(account.getDomainId(), router.getDomainId())) {
            throw new PermissionDeniedException ("Unable to start router with id " + routerId + ". Permission denied.");
        }
        
    	long eventId = EventUtils.saveScheduledEvent(User.UID_SYSTEM, Account.ACCOUNT_ID_SYSTEM, EventTypes.EVENT_ROUTER_START, "starting Router with Id: "+routerId);
    	return startRouter(routerId, eventId);
    }

    @Override @DB
    public DomainRouterVO start(long routerId, long startEventId) throws StorageUnavailableException, ConcurrentOperationException {
        AsyncJobExecutor asyncExecutor = BaseAsyncJobExecutor.getCurrentExecutor();
        if (asyncExecutor != null) {
            AsyncJobVO job = asyncExecutor.getJob();
            if (s_logger.isInfoEnabled())
                s_logger.info("Start router " + routerId + ", update async job-" + job.getId());
            _asyncMgr.updateAsyncJobAttachment(job.getId(), "domain_router", routerId);
        }
    	
    	DomainRouterVO router = _routerDao.acquire(routerId);
        if (router == null) {
        	s_logger.debug("Unable to lock the router " + routerId);
        	return router;
        }
        
        if(s_logger.isDebugEnabled())
        	s_logger.debug("Lock on router " + routerId + " is acquired");
        
        boolean started = false;
        String vnet = null;
        boolean vnetAllocated = false;
        try {
	        final State state = router.getState();
	        if (state == State.Running) {
	            if (s_logger.isDebugEnabled()) {
	                s_logger.debug("Router is already started: " + router.toString());
	            }
	            started = true;
	            return router;
	        }
	        
	        EventVO event = new EventVO();
	        event.setUserId(1L);
	        event.setAccountId(router.getAccountId());
	        event.setType(EventTypes.EVENT_ROUTER_START);
	        event.setState(EventState.Started);
	        event.setDescription("Starting Router with Id: "+routerId);
	        event.setStartId(startEventId);
	        event = _eventDao.persist(event);
	        
	        if(startEventId == 0){
	            // When route start is not asynchronous, use id of the Started event instead of Scheduled event
	            startEventId = event.getId();
	        }
	        
	        
	        if (state == State.Destroyed || state == State.Expunging || router.getRemoved() != null) {
	        	s_logger.debug("Starting a router that's can not be started: " + router.toString());
	        	return null;
	        }
	
	        if (state.isTransitional()) {
	        	throw new ConcurrentOperationException("Someone else is starting the router: " + router.toString());
	        }
	
            final HostPodVO pod = _podDao.findById(router.getPodId());
            final HashSet<Host> avoid = new HashSet<Host>();
            final VMTemplateVO template = _templateDao.findById(router.getTemplateId());
            final DataCenterVO dc = _dcDao.findById(router.getDataCenterId());
            ServiceOfferingVO offering = _serviceOfferingDao.findById(router.getServiceOfferingId());
            List<StoragePoolVO> sps = _storageMgr.getStoragePoolsForVm(router.getId());
            StoragePoolVO sp = sps.get(0); // FIXME
            
	        HostVO routingHost = (HostVO)_agentMgr.findHost(Host.Type.Routing, dc, pod, sp, offering, template, router, null, avoid);

	        if (routingHost == null) {
	        	s_logger.error("Unable to find a host to start " + router.toString());
	        	return null;
	        }
	        
	        if (!_routerDao.updateIf(router, Event.StartRequested, routingHost.getId())) {
	            s_logger.debug("Unable to start router " + router.toString() + " because it is not in a startable state");
	            throw new ConcurrentOperationException("Someone else is starting the router: " + router.toString());
	        }
	
	        final boolean mirroredVols = router.isMirroredVols();
	        try {
	            event = new EventVO();
	            event.setUserId(1L);
	            event.setAccountId(router.getAccountId());
	            event.setType(EventTypes.EVENT_ROUTER_START);
	            event.setStartId(startEventId);
	
	            final List<UserVmVO> vms = _vmDao.listBy(routerId, State.Starting, State.Running, State.Stopped, State.Stopping);
	            if (vms.size() != 0) { // Find it in the existing network.
	                for (final UserVmVO vm : vms) {
	                    if (vm.getVnet() != null) {
	                        vnet = vm.getVnet();
	                        break;
	                    }
	                }
	            }
	            
	            if (vnet != null) {
	            	s_logger.debug("Router: " + router.getName() + " discovered vnet: " + vnet + " from existing VMs.");
	            } else {
	            	s_logger.debug("Router: " + router.getName() + " was unable to discover vnet from existing VMs. Acquiring new vnet.");
	            }
	
	            String routerMacAddress = null;
	            if (vnet == null && router.getRole() == Role.DHCP_FIREWALL_LB_PASSWD_USERDATA) { // If not found, then get another one.
	                if(USE_POD_VLAN){
	                    vnet = _dcDao.allocatePodVlan(router.getPodId(), router.getAccountId());
	                } else {
	                    vnet = _dcDao.allocateVnet(router.getDataCenterId(), router.getAccountId());
	                }
	                vnetAllocated = true;
	                if(vnet != null){
	                    routerMacAddress = getRouterMacForVnet(dc, vnet);
	                }
	            } else if (router.getRole() == Role.DHCP_USERDATA) {
	            	if (!Vlan.UNTAGGED.equals(router.getVlanId())) {
	            		vnet = router.getVlanId().trim();
	            	} else {
	            		vnet = Vlan.UNTAGGED;
	            	}
	            	routerMacAddress = router.getGuestMacAddress();
	            } else if (vnet != null && router.getRole() == Role.DHCP_FIREWALL_LB_PASSWD_USERDATA) {
	                routerMacAddress = getRouterMacForVnet(dc, vnet);
	            }
	
	            if (vnet == null) {
	                s_logger.error("Unable to get another vnet while starting router " + router.getName());
	                return null;
	            } else {
	            	s_logger.debug("Router: " + router.getName() + " is using vnet: " + vnet);
	            }
	           	
	            Answer answer = null;
	            int retry = _retry;

	            do {
	                if (s_logger.isDebugEnabled()) {
	                    s_logger.debug("Trying to start router on host " + routingHost.getName());
	                }
	                
	                String privateIpAddress = _dcDao.allocateLinkLocalPrivateIpAddress(router.getDataCenterId(), routingHost.getPodId(), router.getId());
	                if (privateIpAddress == null) {
	                    s_logger.error("Unable to allocate a private ip address while creating router for pod " + routingHost.getPodId());
	                    avoid.add(routingHost);
	                    continue;
	                }

	                if (s_logger.isDebugEnabled()) {
	                    s_logger.debug("Private Ip Address allocated: " + privateIpAddress);
	                }

	                router.setPrivateIpAddress(privateIpAddress);
	                router.setPrivateNetmask(NetUtils.getLinkLocalNetMask());
	                router.setGuestMacAddress(routerMacAddress);
	                router.setVnet(vnet);
	                final String name = VirtualMachineName.attachVnet(router.getName(), vnet);
	                router.setInstanceName(name);
	                
	                _routerDao.updateIf(router, Event.OperationRetry, routingHost.getId());

	                List<VolumeVO> vols = _storageMgr.prepare(router, routingHost);
	                if (vols == null) {
	                    s_logger.debug("Couldn't get storage working for " + routingHost);
	                    continue;
	                }
	                /*
	                if( !_storageMgr.share(router, vols, routingHost, true) ) {
	                	s_logger.debug("Unable to share volumes to host " + routingHost.getId());
	                	continue;
	                }
	                */

	                try {
	                    String[] storageIps = new String[2];

	                    // Determine the VM's OS description
	                    String guestOSDescription;
	                    GuestOSVO guestOS = _guestOSDao.findById(router.getGuestOSId());
	                    if (guestOS == null) {
	                        String msg = "Could not find guest OS description for OSId " 
	                            + router.getGuestOSId() + " for vm: " + router.getName();
	                        s_logger.debug(msg); 
	                        throw new CloudRuntimeException(msg);
	                    } else {
	                        guestOSDescription = guestOS.getDisplayName();
	                    }
	                    
	                    final StartRouterCommand cmdStartRouter = new StartRouterCommand(router, _networkRate,
	                            _multicastRate, name, storageIps, vols, mirroredVols, guestOSDescription);
	                    answer = _agentMgr.send(routingHost.getId(), cmdStartRouter);
	                    if (answer != null && answer.getResult()) {
	                        if (answer instanceof StartRouterAnswer){
	                            StartRouterAnswer rAnswer = (StartRouterAnswer)answer;
	                            if (rAnswer.getPrivateIpAddress() != null) {
	                                router.setPrivateIpAddress(rAnswer.getPrivateIpAddress());
	                            }
	                            if (rAnswer.getPrivateMacAddress() != null) {
	                                router.setPrivateMacAddress(rAnswer.getPrivateMacAddress());
	                            }
	                        }
	                        if (resendRouterState(router)) {
	                            if (s_logger.isDebugEnabled()) {
	                                s_logger.debug("Router " + router.getName() + " started on " + routingHost.getName());
	                            }
	                            started = true;
	                            break;
	                        } else {
	                            if (s_logger.isDebugEnabled()) {
	                                s_logger.debug("Router " + router.getName() + " started on " + routingHost.getName() + " but failed to program rules");
	                            }
	                            sendStopCommand(router);
	                        }
	                    }
	                    s_logger.debug("Unable to start " + router.toString() + " on host " + routingHost.toString() + " due to " + answer.getDetails());
	                } catch (OperationTimedoutException e) {
	                    if (e.isActive()) {
	                        s_logger.debug("Unable to start vm " + router.getName() + " due to operation timed out and it is active so scheduling a restart.");
	                        _haMgr.scheduleRestart(router, true);
	                        return null;
	                    }
	                } catch (AgentUnavailableException e) {
	                    s_logger.debug("Agent " + routingHost.toString() + " was unavailable to start VM " + router.getName());
	                }
	                avoid.add(routingHost);
	                
	                router.setPrivateIpAddress(null);
	                _dcDao.releaseLinkLocalPrivateIpAddress(privateIpAddress, router.getDataCenterId(), router.getId());
	
	                _storageMgr.unshare(router, vols, routingHost);
	            } while (--retry > 0 && (routingHost = (HostVO)_agentMgr.findHost(Host.Type.Routing, dc, pod, sp,  offering, template, router, null, avoid)) != null);

	
	            if (routingHost == null || retry <= 0) {
	                throw new ExecutionException("Couldn't find a routingHost");
	            }
	
	            _routerDao.updateIf(router, Event.OperationSucceeded, routingHost.getId());
	            if (s_logger.isDebugEnabled()) {
	                s_logger.debug("Router " + router.toString() + " is now started on " + routingHost.toString());
	            }
	            
	            event.setDescription("successfully started Domain Router: " + router.getName());
	            _eventDao.persist(event);
	
	            return _routerDao.findById(routerId);
	        } catch (final Throwable th) {
	        	
	            if (th instanceof ExecutionException) {
	                s_logger.error("Error while starting router due to " + th.getMessage());
	            } else if (th instanceof ConcurrentOperationException) {
	            	throw (ConcurrentOperationException)th;
	            } else if (th instanceof StorageUnavailableException) {
	            	throw (StorageUnavailableException)th;
	            } else {
	                s_logger.error("Error while starting router", th);
	            }
	            return null;
	        }
        } finally {
            
            if (!started){
                Transaction txn = Transaction.currentTxn();
                txn.start();
                if (vnetAllocated == true && vnet != null) {
                    _dcDao.releaseVnet(vnet, router.getDataCenterId(), router.getAccountId());
                }

                router.setVnet(null);
                String privateIpAddress = router.getPrivateIpAddress();

                router.setPrivateIpAddress(null);

                if (privateIpAddress != null) {
                    _dcDao.releasePrivateIpAddress(privateIpAddress, router.getDataCenterId(), router.getId());
                }


                if (_routerDao.updateIf(router, Event.OperationFailed, null)) {
                    txn.commit();
                }
                
                EventVO event = new EventVO();
                event.setUserId(1L);
                event.setAccountId(router.getAccountId());
                event.setType(EventTypes.EVENT_ROUTER_START);
                event.setDescription("Failed to start Router with Id: "+routerId);
                event.setLevel(EventVO.LEVEL_ERROR);
                event.setStartId(startEventId);
                _eventDao.persist(event);
            }
            
        	if (router != null) {
                if(s_logger.isDebugEnabled())
                	s_logger.debug("Releasing lock on router " + routerId);
        		_routerDao.release(routerId);
        	}

        }
    }

	private String getRouterMacForVnet(final DataCenterVO dc, final String vnet) {
		final long vnetId = Long.parseLong(vnet);
		//ToDo: There could be 2 DomR in 2 diff pods of the zone with same vnet. Add podId to the mix to make them unique
		final long routerMac = (NetUtils.mac2Long(dc.getRouterMacAddress()) & (0x00ffff0000ffffl)) | ((vnetId & 0xffff) << 16);
		return NetUtils.long2Mac(routerMac);
	}
	
	private String getRouterMacForZoneVlan(final DataCenterVO dc, final String vlan) {
        final long vnetId = Long.parseLong(vlan);
        final long routerMac = (NetUtils.mac2Long(dc.getRouterMacAddress()) & (0x00ffff0000ffffl)) | ((vnetId & 0xffff) << 16);
        return NetUtils.long2Mac(routerMac);
    }
	
	private String[] getMacAddressPair(long dataCenterId) {
		return _dcDao.getNextAvailableMacAddressPair(dataCenterId);
	}

    private boolean resendRouterState(final DomainRouterVO router) {
        if (router.getRole() == Role.DHCP_FIREWALL_LB_PASSWD_USERDATA) {
			//source NAT address is stored in /proc/cmdline of the domR and gets
			//reassigned upon powerup. Source NAT rule gets configured in StartRouter command
        	final List<IPAddressVO> ipAddrs = listPublicIpAddressesInVirtualNetwork(router.getAccountId(), router.getDataCenterId(), null);
			final List<String> ipAddrList = new ArrayList<String>();
			for (final IPAddressVO ipVO : ipAddrs) {
				ipAddrList.add(ipVO.getAddress());
			}
			if (!ipAddrList.isEmpty()) {
				final boolean success = associateIP(router, ipAddrList, true, 0);
				if (!success) {
					return false;
				}
			}
			final List<FirewallRuleVO> fwRules = new ArrayList<FirewallRuleVO>();
			for (final IPAddressVO ipVO : ipAddrs) {
				fwRules.addAll(_rulesDao.listIPForwarding(ipVO.getAddress()));
			}
			final List<FirewallRuleVO> result = updateFirewallRules(router
					.getPublicIpAddress(), fwRules, router);
			if (result.size() != fwRules.size()) {
				return false;
			}
		}
		return resendDhcpEntries(router);
      
    }
    
    private boolean resendDhcpEntries(final DomainRouterVO router){
    	final List<UserVmVO> vms = _vmDao.listBy(router.getId(), State.Creating, State.Starting, State.Running, State.Stopping, State.Stopped, State.Migrating);
    	final List<Command> cmdList = new ArrayList<Command>();
    	for (UserVmVO vm: vms) {
    		if (vm.getGuestIpAddress() == null || vm.getGuestMacAddress() == null || vm.getName() == null)
    			continue;
    		DhcpEntryCommand decmd = new DhcpEntryCommand(vm.getGuestMacAddress(), vm.getGuestIpAddress(), router.getPrivateIpAddress(), vm.getName());
    		cmdList.add(decmd);
    	}
    	if (cmdList.size() > 0) {
            final Command [] cmds = new Command[cmdList.size()];
            Answer [] answers = null;
            try {
                answers = _agentMgr.send(router.getHostId(), cmdList.toArray(cmds), false);
            } catch (final AgentUnavailableException e) {
                s_logger.warn("agent unavailable", e);
            } catch (final OperationTimedoutException e) {
                s_logger.warn("Timed Out", e);
            }
            if (answers == null ){
                return false;
            }
            int i=0;
            while (i < cmdList.size()) {
                Answer ans = answers[i];
                i++;
                if ((ans != null) && (ans.getResult())) {
                    continue;
                } else {
                    return false;
                }
            }
    	}
        return true;
    }

    /*
    private boolean resendUserData(final DomainRouterVO router){
    	final List<UserVmVO> vms = _vmDao.listByRouterId(router.getId());
    	final List<Command> cmdList = new ArrayList<Command>();
    	for (UserVmVO vm: vms) {
    		if (vm.getGuestIpAddress() == null || vm.getGuestMacAddress() == null || vm.getName() == null)
    			continue;
    		if (vm.getUserData() == null)
    			continue;
    		UserDataCommand userDataCmd = new UserDataCommand(vm.getUserData(), vm.getGuestIpAddress(), router.getPrivateIpAddress(), vm.getName());
    		cmdList.add(userDataCmd);
    	}
        final Command [] cmds = new Command[cmdList.size()];
        Answer [] answers = null;
        try {
            answers = _agentMgr.send(router.getHostId(), cmdList.toArray(cmds), false);
        } catch (final AgentUnavailableException e) {
            s_logger.warn("agent unavailable", e);
        } catch (final OperationTimedoutException e) {
            s_logger.warn("Timed Out", e);
        }
        if (answers == null ){
            return false;
        }
        int i=0;
        while (i < cmdList.size()) {
        	Answer ans = answers[i];
        	i++;
        	if ((ans != null) && (ans.getResult())) {
        		continue;
        	} else {
        		return false;
        	}
        }
        return true;
    }
    */

    @Override
    public boolean stopRouter(final long routerId, long eventId) {
        AsyncJobExecutor asyncExecutor = BaseAsyncJobExecutor.getCurrentExecutor();
        if (asyncExecutor != null) {
            AsyncJobVO job = asyncExecutor.getJob();

            if (s_logger.isInfoEnabled())
                s_logger.info("Stop router " + routerId + ", update async job-" + job.getId());
            _asyncMgr.updateAsyncJobAttachment(job.getId(), "domain_router", routerId);
        }
    	
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Stopping router " + routerId);
        }
        
        return stop(_routerDao.findById(routerId), eventId);
    }
    
    
    @Override
    public DomainRouterVO stopRouter(StopRouterCmd cmd) throws InvalidParameterValueException, PermissionDeniedException{
	    Long routerId = cmd.getId();
        Account account = (Account)UserContext.current().getAccountObject();

	    // verify parameters
        DomainRouterVO router = _routerDao.findById(routerId);
        if (router == null) {
        	throw new PermissionDeniedException ("Unable to stop router with id " + routerId + ". Permission denied.");
        }

        if ((account != null) && !_domainDao.isChildDomain(account.getDomainId(), router.getDomainId())) {
            throw new PermissionDeniedException ("Unable to stop router with id " + routerId + ". Permission denied");
        }
        
        long eventId = EventUtils.saveScheduledEvent(User.UID_SYSTEM, Account.ACCOUNT_ID_SYSTEM, EventTypes.EVENT_ROUTER_STOP, "stopping Router with Id: "+routerId);
        
        boolean success = stopRouter(routerId, eventId);

        if (success) {
            return _routerDao.findById(routerId);
        }
        return null;
    }

    @DB
	public void processStopOrRebootAnswer(final DomainRouterVO router, Answer answer) {
		final Transaction txn = Transaction.currentTxn();
        try {
            txn.start();
            final UserStatisticsVO userStats = _userStatsDao.lock(router.getAccountId(), router.getDataCenterId());
            if (userStats != null) {
                final RebootAnswer sa = (RebootAnswer)answer;
                final Long received = sa.getBytesReceived();
                long netBytes = 0;
                if (received != null) {
                	if (received.longValue() >= userStats.getCurrentBytesReceived()) {
                    	 netBytes = received.longValue();
                	} else {
                		netBytes = userStats.getCurrentBytesReceived() + received;
                	}
                } else {
                	netBytes = userStats.getCurrentBytesReceived();
                }
                userStats.setCurrentBytesReceived(0);
                userStats.setNetBytesReceived(userStats.getNetBytesReceived() + netBytes);
                
                final Long sent = sa.getBytesSent();
                
                if (sent != null) {
                	if (sent.longValue() >= userStats.getCurrentBytesSent()) {
                   	 	netBytes = sent.longValue();
                	} else {
                		netBytes = userStats.getCurrentBytesSent() + sent;
                	}
               } else {
               		netBytes = userStats.getCurrentBytesSent();
                }
                userStats.setNetBytesSent(userStats.getNetBytesSent() + netBytes);
                userStats.setCurrentBytesSent(0);
                _userStatsDao.update(userStats.getId(), userStats);
            } else {
                s_logger.warn("User stats were not created for account " + router.getAccountId() + " and dc " + router.getDataCenterId());
            }
            txn.commit();
        } catch (final Exception e) {
            throw new CloudRuntimeException("Problem getting stats after reboot/stop ", e);
        }
	}

    @Override
    public boolean getRouterStatistics(final long vmId, final Map<String, long[]> netStats, final Map<String, long[]> diskStats) {
        final DomainRouterVO router = _routerDao.findById(vmId);

        if (router == null || router.getState() != State.Running || router.getHostId() == null) {
            return true;
        }

        /*
        final GetVmStatsCommand cmd = new GetVmStatsCommand(router, router.getInstanceName());
        final Answer answer = _agentMgr.easySend(router.getHostId(), cmd);
        if (answer == null) {
            return false;
        }

        final GetVmStatsAnswer stats = (GetVmStatsAnswer)answer;

        netStats.putAll(stats.getNetworkStats());
        diskStats.putAll(stats.getDiskStats());
        */

        return true;
    }


    @Override
    public boolean rebootRouter(final long routerId, long startEventId) {
        AsyncJobExecutor asyncExecutor = BaseAsyncJobExecutor.getCurrentExecutor();
        if (asyncExecutor != null) {
            AsyncJobVO job = asyncExecutor.getJob();

            if (s_logger.isInfoEnabled())
                s_logger.info("Reboot router " + routerId + ", update async job-" + job.getId());
            _asyncMgr.updateAsyncJobAttachment(job.getId(), "domain_router", routerId);
        }
    	
        final DomainRouterVO router = _routerDao.findById(routerId);

        if (router == null || router.getState() == State.Destroyed) {
            return false;
        }
        
        EventVO event = new EventVO();
        event.setUserId(1L);
        event.setAccountId(router.getAccountId());
        event.setType(EventTypes.EVENT_ROUTER_REBOOT);
        event.setState(EventState.Started);
        event.setDescription("Rebooting Router with Id: "+routerId);
        event.setStartId(startEventId);
        _eventDao.persist(event);
        

        event = new EventVO();
        event.setUserId(1L);
        event.setAccountId(router.getAccountId());
        event.setType(EventTypes.EVENT_ROUTER_REBOOT);
        event.setStartId(startEventId);

        if (router.getState() == State.Running && router.getHostId() != null) {
            final RebootRouterCommand cmd = new RebootRouterCommand(router.getInstanceName(), router.getPrivateIpAddress());
            final RebootAnswer answer = (RebootAnswer)_agentMgr.easySend(router.getHostId(), cmd);

            if (answer != null &&  resendRouterState(router)) {
            	processStopOrRebootAnswer(router, answer);
                event.setDescription("successfully rebooted Domain Router : " + router.getName());
                _eventDao.persist(event);
                return true;
            } else {
                event.setDescription("failed to reboot Domain Router : " + router.getName());
                event.setLevel(EventVO.LEVEL_ERROR);
                _eventDao.persist(event);
                return false;
            }
        } else {
            return startRouter(routerId, 0) != null;
        }
    }
    
    @Override
    public boolean rebootRouter(RebootRouterCmd cmd) throws InvalidParameterValueException, PermissionDeniedException{
    	Long routerId = cmd.getId();
    	Account account = (Account)UserContext.current().getAccountObject();
    	
        //verify parameters
        DomainRouterVO router = _routerDao.findById(routerId);
        if (router == null) {
        	throw new PermissionDeniedException("Unable to reboot domain router with id " + routerId + ". Permission denied");
        }

        if ((account != null) && !_domainDao.isChildDomain(account.getDomainId(), router.getDomainId())) {
            throw new PermissionDeniedException("Unable to reboot domain router with id " + routerId + ". Permission denied");
        }
        long eventId = EventUtils.saveScheduledEvent(User.UID_SYSTEM, Account.ACCOUNT_ID_SYSTEM, EventTypes.EVENT_ROUTER_REBOOT, "rebooting Router with Id: "+routerId);
        
    	return rebootRouter(routerId, eventId);
    }

    @Override
    public boolean associateIP(final DomainRouterVO router, final List<String> ipAddrList, final boolean add, long vmId) {
        final Command [] cmds = new Command[ipAddrList.size()];
        int i=0;
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
			
            cmds[i++] = new IPAssocCommand(router.getInstanceName(), router.getPrivateIpAddress(), ipAddress, add, firstIP, sourceNat, vlanId, vlanGateway, vlanNetmask, vifMacAddress, vmGuestAddress);
            
            sourceNat = false;
        }

        Answer[] answers = null;
        try {
            answers = _agentMgr.send(router.getHostId(), cmds, false);
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
    	Account account = (Account)UserContext.current().getAccountObject();
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
        AccountVO accountToLock = null;
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

            DomainRouterVO router = _routerDao.findBy(accountId, zoneId);
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

        final DomainRouterVO router = _routerDao.findBy(ipVO.getAccountId(), ipVO.getDataCenterId());
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

        final List<Command> cmdList = new ArrayList<Command>();
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
                cmdList.add(cmd);
            } else {
                lbRules.add(rule);
            }
            
        }
        if (lbRules.size() > 0) { //at least one load balancer rule
            final LoadBalancerConfigurator cfgrtr = new HAProxyConfigurator();
            final String [] cfg = cfgrtr.generateConfiguration(fwRules);
            final String [][] addRemoveRules = cfgrtr.generateFwRules(fwRules);
            final LoadBalancerCfgCommand cmd = new LoadBalancerCfgCommand(cfg, addRemoveRules, routerName, routerIp);
            cmdList.add(cmd);
        }
        final Command [] cmds = new Command[cmdList.size()];
        Answer [] answers = null;
        try {
            answers = _agentMgr.send(host.getId(), cmdList.toArray(cmds), false);
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

        final Command [] cmds = new Command[fwRules.size()];
        int i=0;
        for (final FirewallRuleVO rule: fwRules) {
        	IPAddressVO ip = _ipAddressDao.findById(rule.getPublicIpAddress());
        	VlanVO vlan = _vlanDao.findById(new Long(ip.getVlanDbId()));
        	String vlanNetmask = vlan.getVlanNetmask();
        	rule.setVlanNetmask(vlanNetmask);
            if (rule.isForwarding()) {
                fwdRules.add(rule);
                final SetFirewallRuleCommand cmd = new SetFirewallRuleCommand(router.getInstanceName(), router.getPrivateIpAddress(), rule);
                cmds[i++] = cmd;
            }
        }

        Answer [] answers = null;
        try {
        	answers = _agentMgr.send(hostId, cmds, false);
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
        Account account = (Account)UserContext.current().getAccountObject();
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
        Map<String, Pair<String, String>> mappedPublicPorts = new HashMap<String, Pair<String, String>>();

        if (existingRulesOnPubIp != null) {
            for (FirewallRuleVO fwRule : existingRulesOnPubIp) {
                mappedPublicPorts.put(fwRule.getPublicPort(), new Pair<String, String>(fwRule.getPrivateIpAddress(), fwRule.getPrivatePort()));
            }
        }

        Pair<String, String> privateIpPort = mappedPublicPorts.get(publicPort);
        if (privateIpPort != null) {
            if (privateIpPort.first().equals(userVM.getGuestIpAddress()) && privateIpPort.second().equals(privatePort)) {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("skipping the creating of firewall rule " + ipAddress + ":" + publicPort + " to " + userVM.getGuestIpAddress() + ":" + privatePort + "; rule already exists.");
                }
                return null; // already mapped
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
        Account account = (Account)UserContext.current().getAccountObject();

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

        LoadBalancerVO loadBalancer = _loadBalancerDao.findById(loadBalancerId);
        if (loadBalancer == null) {
        	throw new InvalidParameterValueException("Failed to assign to load balancer " + loadBalancerId + ", the load balancer was not found.");
        }

        DomainRouterVO syncObject = _routerDao.findByPublicIpAddress(loadBalancer.getIpAddress());
        cmd.synchronizeCommand("Router", syncObject.getId());

        // Permission check...
        Account account = (Account)UserContext.current().getAccountObject();
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

            for (Long instanceId : instanceIds) {
                if (mappedInstanceIds.contains(instanceId)) {
                    continue;
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
                    nextRouter = _routerDao.findById(userVm.getDomainRouterId());
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
                    List<LoadBalancerVMMapVO> pendingMappedVMs = _loadBalancerVMMapDao.listByLoadBalancerId(loadBalancerId, true);
                    for (LoadBalancerVMMapVO pendingMappedVM : pendingMappedVMs) {
                        if (instanceIds.contains(pendingMappedVM.getInstanceId())) {
                            LoadBalancerVMMapVO pendingMappedVMForUpdate = _loadBalancerVMMapDao.createForUpdate();
                            pendingMappedVMForUpdate.setPending(false);
                            _loadBalancerVMMapDao.update(pendingMappedVM.getId(), pendingMappedVMForUpdate);
                        }
                    }

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

        Account account = (Account)UserContext.current().getAccountObject();
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
                router = _routerDao.findByPublicIpAddress(ipAddress);
                if (router != null) {
                	if (router.getPublicIpAddress() != null) {
                		return false;
                	}
                }
            } else {
                router = _routerDao.findBy(ip.getAccountId(), ip.getDataCenterId());
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
        return _routerDao.findById(routerId);
    }

    @Override
    public List<? extends DomainRouter> getRouters(final long hostId) {
        return _routerDao.listByHostId(hostId);
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

        _executor = Executors.newScheduledThreadPool(1, new NamedThreadFactory("RouterMonitor"));

        final ComponentLocator locator = ComponentLocator.getCurrentLocator();
        
        final Map<String, String> configs = _configDao.getConfiguration("AgentManager", params);

        _routerRamSize = NumbersUtil.parseInt(configs.get("router.ram.size"), 128);

//        String value = configs.get("guest.ip.network");
//        _guestIpAddress = value != null ? value : "10.1.1.1";
//
//        value = configs.get("guest.netmask");
//        _guestNetmask = value != null ? value : "255.255.255.0";

        String value = configs.get("start.retry");
        _retry = NumbersUtil.parseInt(value, 2);

        value = configs.get("router.stats.interval");
        _routerStatsInterval = NumbersUtil.parseInt(value, 300);

        value = configs.get("router.cleanup.interval");
        _routerCleanupInterval = NumbersUtil.parseInt(value, 3600);

        _domain = configs.get("domain");
        if (_domain == null) {
            _domain = "foo.com";
        }

        _instance = configs.get("instance.name");
        if (_instance == null) {
            _instance = "DEFAULT";
        }

        s_logger.info("Router configurations: " + "ramsize=" + _routerRamSize + "; templateId=" + _routerTemplateId);

        final UserStatisticsDao statsDao = locator.getDao(UserStatisticsDao.class);
        if (statsDao == null) {
            throw new ConfigurationException("Unable to get " + UserStatisticsDao.class.getName());
        }

        _agentMgr.registerForHostEvents(new SshKeysDistriMonitor(this, _hostDao, _configDao), true, false, false);
        _haMgr.registerHandler(VirtualMachine.Type.DomainRouter, this);

        boolean useLocalStorage = Boolean.parseBoolean((String)params.get(Config.SystemVMUseLocalStorage.key()));
        String networkRateStr = _configDao.getValue("network.throttling.rate");
        String multicastRateStr = _configDao.getValue("multicast.throttling.rate");
        _networkRate = ((networkRateStr == null) ? 200 : Integer.parseInt(networkRateStr));
        _multicastRate = ((multicastRateStr == null) ? 10 : Integer.parseInt(multicastRateStr));
        _offering = new ServiceOfferingVO("Fake Offering For DomR", 1, _routerRamSize, 0, 0, 0, false, null, NetworkOffering.GuestIpType.Virtualized, useLocalStorage, true, null);
        _offering.setUniqueName("Cloud.Com-SoftwareRouter");
        _offering = _serviceOfferingDao.persistSystemServiceOffering(_offering);
        _template = _templateDao.findRoutingTemplate();
        if (_template == null) {
        	s_logger.error("Unable to find system vm template.");
        } else {
        	_routerTemplateId = _template.getId();
        }
        
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
        
        s_logger.info("Network Manager is configured.");

        return true;
    }
    
    @Override
    public String getName() {
        return _name;
    }

    @Override
    public boolean start() {
        _executor.scheduleAtFixedRate(new RouterCleanupTask(), _routerCleanupInterval, _routerCleanupInterval, TimeUnit.SECONDS);
        _executor.scheduleAtFixedRate(new NetworkUsageTask(), _routerStatsInterval, _routerStatsInterval, TimeUnit.SECONDS);
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    protected NetworkManagerImpl() {
    }

    @Override
    public Command cleanup(final DomainRouterVO vm, final String vmName) {
        if (vmName != null) {
            return new StopCommand(vm, vmName, VirtualMachineName.getVnet(vmName));
        } else if (vm != null) {
            final DomainRouterVO vo = vm;
            return new StopCommand(vo, vo.getVnet());
        } else {
            throw new CloudRuntimeException("Shouldn't even be here!");
        }
    }

    @Override
    public void completeStartCommand(final DomainRouterVO router) {
        _routerDao.updateIf(router, Event.AgentReportRunning, router.getHostId());
    }

    @Override
    public void completeStopCommand(final DomainRouterVO router) {
    	completeStopCommand(router, Event.AgentReportStopped);
    }
    
    @DB
    public void completeStopCommand(final DomainRouterVO router, final Event ev) {
        final long routerId = router.getId();

        final Transaction txn = Transaction.currentTxn();
        try {
            txn.start();
            if (_vmDao.listBy(routerId, State.Starting, State.Running).size() == 0) {
                _dcDao.releaseVnet(router.getVnet(), router.getDataCenterId(), router.getAccountId());
            }

            router.setVnet(null);
            
            String privateIpAddress = router.getPrivateIpAddress();
            
            if (privateIpAddress != null) {
            	_dcDao.releaseLinkLocalPrivateIpAddress(privateIpAddress, router.getDataCenterId(), router.getId());
            }
            router.setPrivateIpAddress(null);

            if (!_routerDao.updateIf(router, ev, null)) {
            	s_logger.debug("Router is not updated");
            	return;
            }
            txn.commit();
        } catch (final Exception e) {
            throw new CloudRuntimeException("Unable to complete stop", e);
        }

        if (_storageMgr.unshare(router, null) == null) {
            s_logger.warn("Unable to set share to false for " + router.getId() + " on host ");
        }
    }

    @Override
    public DomainRouterVO get(final long id) {
        return getRouter(id);
    }

    @Override
    public Long convertToId(final String vmName) {
        if (!VirtualMachineName.isValidRouterName(vmName, _instance)) {
            return null;
        }

        return VirtualMachineName.getRouterId(vmName);
    }
    
    private boolean sendStopCommand(DomainRouterVO router) {
        final StopCommand stop = new StopCommand(router, router.getInstanceName(), router.getVnet());
    	
        Answer answer = null;
        boolean stopped = false;
        try {
            answer = _agentMgr.send(router.getHostId(), stop);
            if (!answer.getResult()) {
                s_logger.error("Unable to stop router");
            } else {
            	stopped = true;
            }
        } catch (AgentUnavailableException e) {
            s_logger.warn("Unable to reach agent to stop vm: " + router.getId());
        } catch (OperationTimedoutException e) {
            s_logger.warn("Unable to reach agent to stop vm: " + router.getId());
            s_logger.error("Unable to stop router");
        }
        
        return stopped;
    }

    @Override
    @DB
    public boolean stop(DomainRouterVO router, long eventId) {
    	long routerId = router.getId();
    	
        router = _routerDao.acquire(routerId);
        if (router == null) {
            s_logger.debug("Unable to acquire lock on router " + routerId);
            return false;
        }
        
        EventVO event = new EventVO();
        event.setUserId(1L);
        event.setAccountId(router.getAccountId());
        event.setType(EventTypes.EVENT_ROUTER_STOP);
        event.setState(EventState.Started);
        event.setDescription("Stopping Router with Id: "+routerId);
        event.setStartId(eventId);
        event = _eventDao.persist(event);
        if(eventId == 0){
            eventId = event.getId();
        }
        
        try {
            
        	if(s_logger.isDebugEnabled())
        		s_logger.debug("Lock on router " + routerId + " for stop is acquired");
        	
            if (router.getRemoved() != null) {
                s_logger.debug("router " + routerId + " is removed");
                return false;
            }
    
            final Long hostId = router.getHostId();
            final State state = router.getState();
            if (state == State.Stopped || state == State.Destroyed || state == State.Expunging || router.getRemoved() != null) {
                s_logger.debug("Router was either not found or the host id is null");
                return true;
            }
    
            event = new EventVO();
            event.setUserId(1L);
            event.setAccountId(router.getAccountId());
            event.setType(EventTypes.EVENT_ROUTER_STOP);
            event.setStartId(eventId);
    
            if (!_routerDao.updateIf(router, Event.StopRequested, hostId)) {
                s_logger.debug("VM " + router.toString() + " is not in a state to be stopped.");
                return false;
            }
    
            if (hostId == null) {
                s_logger.debug("VM " + router.toString() + " doesn't have a host id");
                return false;
            }
    
            final StopCommand stop = new StopCommand(router, router.getInstanceName(), router.getVnet(), router.getPrivateIpAddress());
    
            Answer answer = null;
            boolean stopped = false;
            try {
                answer = _agentMgr.send(hostId, stop);
                if (!answer.getResult()) {
                    s_logger.error("Unable to stop router");
                    event.setDescription("failed to stop Domain Router : " + router.getName());
                    event.setLevel(EventVO.LEVEL_ERROR);
                    _eventDao.persist(event);
                } else {
                    stopped = true;
                }
            } catch (AgentUnavailableException e) {
                s_logger.warn("Unable to reach agent to stop vm: " + router.getId());
            } catch (OperationTimedoutException e) {
                s_logger.warn("Unable to reach agent to stop vm: " + router.getId());
                s_logger.error("Unable to stop router");
            }
    
            if (!stopped) {
                event.setDescription("failed to stop Domain Router : " + router.getName());
                event.setLevel(EventVO.LEVEL_ERROR);
                _eventDao.persist(event);
                _routerDao.updateIf(router, Event.OperationFailed, router.getHostId());
                return false;
            }
    
            completeStopCommand(router, Event.OperationSucceeded);
            event.setDescription("successfully stopped Domain Router : " + router.getName());
            _eventDao.persist(event);
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Router " + router.toString() + " is stopped");
            }
    
            processStopOrRebootAnswer(router, answer);
        } finally {
            if(s_logger.isDebugEnabled())
                s_logger.debug("Release lock on router " + routerId + " for stop");
            _routerDao.release(routerId);
        }
        return true;
    }

    @Override
    public HostVO prepareForMigration(final DomainRouterVO router) throws StorageUnavailableException {
        final long routerId = router.getId();
        final boolean mirroredVols = router.isMirroredVols();
        final DataCenterVO dc = _dcDao.findById(router.getDataCenterId());
        final HostPodVO pod = _podDao.findById(router.getPodId());
        final ServiceOfferingVO offering = _serviceOfferingDao.findById(router.getServiceOfferingId());
        List<StoragePoolVO> sps = _storageMgr.getStoragePoolsForVm(router.getId());
        StoragePoolVO sp = sps.get(0); // FIXME

        final List<VolumeVO> vols = _volsDao.findCreatedByInstance(routerId);

        final String [] storageIps = new String[2];
        final VolumeVO vol = vols.get(0);
        storageIps[0] = vol.getHostIp();
        if (mirroredVols && (vols.size() == 2)) {
            storageIps[1] = vols.get(1).getHostIp();
        }

        final PrepareForMigrationCommand cmd = new PrepareForMigrationCommand(router.getInstanceName(), router.getVnet(), storageIps, vols, mirroredVols);

        HostVO routingHost = null;
        final HashSet<Host> avoid = new HashSet<Host>();

        final HostVO fromHost = _hostDao.findById(router.getHostId());
        if (fromHost.getHypervisorType() != HypervisorType.KVM && fromHost.getClusterId() == null) {
            s_logger.debug("The host is not in a cluster");
            return null;
        }
        avoid.add(fromHost);

        while ((routingHost = (HostVO)_agentMgr.findHost(Host.Type.Routing, dc, pod, sp, offering, _template, router, fromHost, avoid)) != null) {
            avoid.add(routingHost);
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Trying to migrate router to host " + routingHost.getName());
            }

            if( ! _storageMgr.share(router, vols, routingHost, false) ) {
                s_logger.warn("Can not share " + vol.getPath() + " to " + router.getName() );
                throw new StorageUnavailableException("Can not share " + vol.getPath() + " to " + router.getName(), vol);
            }

            final Answer answer = _agentMgr.easySend(routingHost.getId(), cmd);
            if (answer != null && answer.getResult()) {
                return routingHost;
            }

            _storageMgr.unshare(router, vols, routingHost);
        }

        return null;
    }

    @Override
    public boolean migrate(final DomainRouterVO router, final HostVO host) {
        final HostVO fromHost = _hostDao.findById(router.getHostId());

    	if (!_routerDao.updateIf(router, Event.MigrationRequested, router.getHostId())) {
    		s_logger.debug("State for " + router.toString() + " has changed so migration can not take place.");
    		return false;
    	}
    	
        final MigrateCommand cmd = new MigrateCommand(router.getInstanceName(), host.getPrivateIpAddress(), false);
        final Answer answer = _agentMgr.easySend(fromHost.getId(), cmd);
        if (answer == null) {
            return false;
        }

        final List<VolumeVO> vols = _volsDao.findCreatedByInstance(router.getId());
        if (vols.size() == 0) {
            return true;
        }

        _storageMgr.unshare(router, vols, fromHost);

        return true;
    }

    @Override
    public boolean completeMigration(final DomainRouterVO router, final HostVO host) throws OperationTimedoutException, AgentUnavailableException {
        final CheckVirtualMachineCommand cvm = new CheckVirtualMachineCommand(router.getInstanceName());
        final CheckVirtualMachineAnswer answer = (CheckVirtualMachineAnswer)_agentMgr.send(host.getId(), cvm);
        if (answer == null || !answer.getResult()) {
            s_logger.debug("Unable to complete migration for " + router.getId());
            _routerDao.updateIf(router, Event.AgentReportStopped, null);
            return false;
        }

        final State state = answer.getState();
        if (state == State.Stopped) {
            s_logger.warn("Unable to complete migration as we can not detect it on " + host.getId());
            _routerDao.updateIf(router, Event.AgentReportStopped, null);
            return false;
        }

        _routerDao.updateIf(router, Event.OperationSucceeded, host.getId());

        return true;
    }

    protected class RouterCleanupTask implements Runnable {

        public RouterCleanupTask() {
        }

        @Override
        public void run() {
            try {
                final List<Long> ids = _routerDao.findLonelyRouters();
                s_logger.info("Found " + ids.size() + " routers to stop. ");
    
                for (final Long id : ids) {
                    stopRouter(id, 0);
                }
                s_logger.info("Done my job.  Time to rest.");
            } catch (Exception e) {
                s_logger.warn("Unable to stop routers.  Will retry. ", e);
            }
        }
    }

	@Override
	public boolean addDhcpEntry(final long routerHostId, final String routerIp, String vmName, String vmMac, String vmIp) {
        final DhcpEntryCommand dhcpEntry = new DhcpEntryCommand(vmMac, vmIp, routerIp, vmName);


        final Answer answer = _agentMgr.easySend(routerHostId, dhcpEntry);
        return (answer != null && answer.getResult());
	}
	
	@Override
	public DomainRouterVO addVirtualMachineToGuestNetwork(UserVmVO vm, String password, long startEventId) throws ConcurrentOperationException {
        try {
        	DomainRouterVO router = start(vm.getDomainRouterId(), 0);
	        if (router == null) {
        		s_logger.error("Can't find a domain router to start VM: " + vm.getName());
        		return null;
	        }
	        
	        if (vm.getGuestMacAddress() == null){
	            String routerGuestMacAddress = null;
	            if(USE_POD_VLAN){
	                if((vm.getPodId() == router.getPodId())){
	                    routerGuestMacAddress = router.getGuestMacAddress();
	                } else {
	                    //Not in the same pod use guest zone mac address
	                    routerGuestMacAddress = router.getGuestZoneMacAddress();
	                }
	                String vmMacAddress = NetUtils.long2Mac((NetUtils.mac2Long(routerGuestMacAddress) & 0xffffffff0000L) | (NetUtils.ip2Long(vm.getGuestIpAddress()) & 0xffff));
	                vm.setGuestMacAddress(vmMacAddress);
	            }
	            else {
	                String vmMacAddress = NetUtils.long2Mac((NetUtils.mac2Long(router.getGuestMacAddress()) & 0xffffffff0000L) | (NetUtils.ip2Long(vm.getGuestIpAddress()) & 0xffff));
	                vm.setGuestMacAddress(vmMacAddress);
	            }
	        }
	        String userData = vm.getUserData();
	        int cmdsLength = (password == null ? 0:1) + 1;
	        Command[] cmds = new Command[++cmdsLength];
	        int cmdIndex = 0;
	        int passwordIndex = -1;
	        int vmDataIndex = -1;
	        cmds[cmdIndex] = new DhcpEntryCommand(vm.getGuestMacAddress(), vm.getGuestIpAddress(), router.getPrivateIpAddress(), vm.getName());
	        if (password != null) {
	            final String encodedPassword = rot13(password);
	        	cmds[++cmdIndex] = new SavePasswordCommand(encodedPassword, vm.getPrivateIpAddress(), router.getPrivateIpAddress(), vm.getName());
	        	passwordIndex = cmdIndex;
	        }
	        	        
	        
	        String serviceOffering = _serviceOfferingDao.findById(vm.getServiceOfferingId()).getDisplayText();
	        String zoneName = _dcDao.findById(vm.getDataCenterId()).getName();
	        String routerPublicIpAddress = (router.getPublicIpAddress() != null) ? router.getPublicIpAddress() : vm.getGuestIpAddress();
	        
	        cmds[++cmdIndex] = generateVmDataCommand(router.getPrivateIpAddress(), routerPublicIpAddress, vm.getPrivateIpAddress(), userData, serviceOffering, zoneName, vm.getGuestIpAddress(), vm.getName(), vm.getInstanceName(), vm.getId());
	        vmDataIndex = cmdIndex;
	        
	        Answer[] answers = _agentMgr.send(router.getHostId(), cmds, true);
	        if (!answers[0].getResult()) {
	        	s_logger.error("Unable to set dhcp entry for " + vm.getId() + " - " + vm.getName() +" on domR: " + router.getName() + " due to " + answers[0].getDetails());
	        	return null;
	        }
	        
	        if (password != null && !answers[passwordIndex].getResult()) {
	        	s_logger.error("Unable to set password for " + vm.getId() + " - " + vm.getName() + " due to " + answers[passwordIndex].getDetails());
	        	return null;
	        }
	        
	        if (vmDataIndex > 0 && !answers[vmDataIndex].getResult()) {
	        	s_logger.error("Unable to set VM data for " + vm.getId() + " - " + vm.getName() + " due to " + answers[vmDataIndex].getDetails());
	        	return null;
	        }
	        return router;
        } catch (StorageUnavailableException e) {
        	s_logger.error("Unable to start router " + vm.getDomainRouterId() + " because storage is unavailable.");
        	return null;
        } catch (AgentUnavailableException e) {
        	s_logger.error("Unable to setup the router " + vm.getDomainRouterId() + " for vm " + vm.getId() + " - " + vm.getName() + " because agent is unavailable");
        	return null;
		} catch (OperationTimedoutException e) {
        	s_logger.error("Unable to setup the router " + vm.getDomainRouterId() + " for vm " + vm.getId() + " - " + vm.getName() + " because agent is too busy");
        	return null;
		}
	}
	
	private VmDataCommand generateVmDataCommand(String routerPrivateIpAddress, String routerPublicIpAddress,
												String vmPrivateIpAddress, String userData, String serviceOffering, String zoneName,
												String guestIpAddress, String vmName, String vmInstanceName, long vmId) {
		VmDataCommand cmd = new VmDataCommand(routerPrivateIpAddress, vmPrivateIpAddress);
		
		cmd.addVmData("userdata", "user-data", userData);
		cmd.addVmData("metadata", "service-offering", serviceOffering);
		cmd.addVmData("metadata", "availability-zone", zoneName);
    	cmd.addVmData("metadata", "local-ipv4", guestIpAddress);
    	cmd.addVmData("metadata", "local-hostname", vmName);
    	cmd.addVmData("metadata", "public-ipv4", routerPublicIpAddress);
    	cmd.addVmData("metadata", "public-hostname", routerPublicIpAddress);
    	cmd.addVmData("metadata", "instance-id", vmInstanceName);
    	cmd.addVmData("metadata", "vm-id", String.valueOf(vmId));
    	
    	return cmd;
	}
	
	public void releaseVirtualMachineFromGuestNetwork(UserVmVO vm) {
	}

    @Override
    public String createZoneVlan(DomainRouterVO router) {
        String zoneVlan = _dcDao.allocateVnet(router.getDataCenterId(), router.getAccountId());
        final DataCenterVO dc = _dcDao.findById(router.getDataCenterId());
        router.setZoneVlan(zoneVlan);
        router.setGuestZoneMacAddress(getRouterMacForZoneVlan(dc, zoneVlan));
        _routerDao.update(router.getId(), router);
        final CreateZoneVlanCommand cmdCreateZoneVlan = new CreateZoneVlanCommand(router);
        CreateZoneVlanAnswer answer = (CreateZoneVlanAnswer) _agentMgr.easySend(router.getHostId(), cmdCreateZoneVlan);
        if(!answer.getResult()){
            s_logger.error("Unable to create zone vlan for router: "+router.getName()+ " zoneVlan: "+zoneVlan);
            return null;
        }
        return zoneVlan;
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
    public NetworkConfigurationVO setupNetworkConfiguration(AccountVO owner, NetworkOfferingVO offering, DeploymentPlan plan) {
        return setupNetworkConfiguration(owner, offering, null, plan);
    }
    
    @Override
    public NetworkConfigurationVO setupNetworkConfiguration(AccountVO owner, NetworkOfferingVO offering, NetworkConfiguration predefined, DeploymentPlan plan) {
        List<NetworkConfigurationVO> configs = _networkProfileDao.listBy(owner.getId(), offering.getId(), plan.getDataCenterId());
        if (configs.size() > 0) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Found existing network configuration for offering " + offering + ": " + configs.get(0));
            }
            return configs.get(0);
        }
        
        for (NetworkGuru guru : _networkGurus) {
            NetworkConfiguration config = guru.design(offering, plan, predefined, owner);
            if (config == null) {
                continue;
            }
            
            if (config.getId() != null) {
                if (config instanceof NetworkConfigurationVO) {
                    return (NetworkConfigurationVO)config;
                } else {
                    return _networkProfileDao.findById(config.getId());
                }
            } 
            
            NetworkConfigurationVO vo = new NetworkConfigurationVO(config, offering.getId(), plan.getDataCenterId(), guru.getName());
            return _networkProfileDao.persist(vo, owner.getId());
        }

        throw new CloudRuntimeException("Unable to convert network offering to network profile: " + offering.getId());
    }

    @Override
    public List<NetworkConfigurationVO> setupNetworkConfigurations(AccountVO owner, List<NetworkOfferingVO> offerings, DeploymentPlan plan) {
        List<NetworkConfigurationVO> profiles = new ArrayList<NetworkConfigurationVO>(offerings.size());
        for (NetworkOfferingVO offering : offerings) {
            profiles.add(setupNetworkConfiguration(owner, offering, plan));
        }
        return profiles;
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
    
    public NetworkConfigurationVO createNetworkConfiguration(NetworkOfferingVO offering, DeploymentPlan plan, AccountVO owner) {
        return null;
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
    
    @Override
    public NicTO[] prepare(VirtualMachineProfile vmProfile, DeployDestination dest) throws InsufficientAddressCapacityException, InsufficientVirtualNetworkCapcityException {
        List<NicVO> nics = _nicDao.listBy(vmProfile.getId());
        NicTO[] nicTos = new NicTO[nics.size()];
        int i = 0;
        for (NicVO nic : nics) {
            NetworkConfigurationVO config = _networkProfileDao.findById(nic.getNetworkConfigurationId());
            NicProfile profile = null;
            if (nic.getReservationStrategy() == ReservationStrategy.Start) {
                NetworkGuru concierge = _networkGurus.get(config.getGuruName());
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
                    if (!element.prepare(config, profile, vmProfile, null)) {
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
            NetworkConfigurationVO config = _networkProfileDao.findById(nic.getNetworkConfigurationId());
            if (nic.getReservationStrategy() == ReservationStrategy.Start) {
                NetworkGuru concierge = _networkGurus.get(config.getGuruName());
                nic.setState(Resource.State.Releasing);
                _nicDao.update(nic.getId(), nic);
                concierge.release(nic.getReservationId());
            }
        }
    }
    
    NicProfile toNicProfile(NicVO nic) {
        NetworkConfiguration config = _networkProfileDao.findById(nic.getNetworkConfigurationId());
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
    public <K extends VMInstanceVO> void create(K vm) {
    }
    
    @Override
    public <K extends VMInstanceVO> List<NicVO> getNics(K vm) {
        return _nicDao.listBy(vm.getId());
    }
    
    protected class NetworkUsageTask implements Runnable {

        public NetworkUsageTask() {
        }

        @Override
        public void run() {
            final List<DomainRouterVO> routers = _routerDao.listUpByHostId(null);
            s_logger.debug("Found " + routers.size() + " running routers. ");

            for (DomainRouterVO router : routers) {
                String privateIP = router.getPrivateIpAddress();
                if(privateIP != null){
                    final NetworkUsageCommand usageCmd = new NetworkUsageCommand(privateIP, router.getName());
                    final NetworkUsageAnswer answer = (NetworkUsageAnswer)_agentMgr.easySend(router.getHostId(), usageCmd);
                    if(answer != null){
                        Transaction txn = Transaction.open(Transaction.CLOUD_DB);
                        try {
                            if ((answer.getBytesReceived() == 0) && (answer.getBytesSent() == 0)) {
                                s_logger.debug("Recieved and Sent bytes are both 0. Not updating user_statistics");
                                continue;
                            }
                            txn.start();
                            UserStatisticsVO stats = _statsDao.lock(router.getAccountId(), router.getDataCenterId());
                            if (stats == null) {
                                s_logger.warn("unable to find stats for account: " + router.getAccountId());
                                continue;
                            }
                            if (stats.getCurrentBytesReceived() > answer.getBytesReceived()) {
                                if (s_logger.isDebugEnabled()) {
                                    s_logger.debug("Received # of bytes that's less than the last one.  Assuming something went wrong and persisting it.  Reported: " + answer.getBytesReceived() + " Stored: " + stats.getCurrentBytesReceived());
                                }
                                stats.setNetBytesReceived(stats.getNetBytesReceived() + stats.getCurrentBytesReceived());
                            }
                            stats.setCurrentBytesReceived(answer.getBytesReceived());
                            if (stats.getCurrentBytesSent() > answer.getBytesSent()) {
                                if (s_logger.isDebugEnabled()) {
                                    s_logger.debug("Received # of bytes that's less than the last one.  Assuming something went wrong and persisting it.  Reported: " + answer.getBytesSent() + " Stored: " + stats.getCurrentBytesSent());
                                }
                                stats.setNetBytesSent(stats.getNetBytesSent() + stats.getCurrentBytesSent());
                            }
                            stats.setCurrentBytesSent(answer.getBytesSent());
                            _statsDao.update(stats.getId(), stats);
                            txn.commit();
                        } catch(Exception e) {
                            txn.rollback();
                            s_logger.warn("Unable to update user statistics for account: " + router.getAccountId() + " Rx: " + answer.getBytesReceived() + "; Tx: " + answer.getBytesSent());
                        } finally {
                            txn.close();
                        }
                    }
                }
            }
        }
    }

	@Override @DB
	public boolean removeFromLoadBalancer(RemoveFromLoadBalancerRuleCmd cmd) throws InvalidParameterValueException {
		
        Long userId = UserContext.current().getUserId();
        Account account = (Account)UserContext.current().getAccountObject();
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

            DomainRouterVO router = _routerDao.findBy(ipAddress.getAccountId(), ipAddress.getDataCenterId());
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
    	Account account = (Account)UserContext.current().getAccountObject();
    	
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

            DomainRouterVO router = _routerDao.findBy(ipAddress.getAccountId(), ipAddress.getDataCenterId());
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
    	String name = cmd.getName();
    	String description = cmd.getDescription();
    	Account account = (Account)UserContext.current().getAccountObject();
    	
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
        
        Account lbOwner = _accountDao.findById(loadBalancer.getId());
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
        DomainRouterVO router = _routerDao.findBy(ip.getAccountId(), ip.getDataCenterId());
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
        Account account = (Account)UserContext.current().getAccountObject();

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
        Account account = (Account)UserContext.current().getAccountObject();
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

            AccountVO accountVO = _accountDao.findById(accountId);
            if (accountVO == null) {
                return false;
            }
          
            if ((ipVO.getAccountId() == null) || (ipVO.getAccountId().longValue() != accountId)) {
                // FIXME: is the user visible in the admin account's domain????
                if (!BaseCmd.isAdmin(accountVO.getType())) {
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
    	Account account = (Account)UserContext.current().getAccountObject();
    	
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
                description = "deleted " + ruleName + " rule [" + publicIp + ":" + rule.getPublicPort() + "]->[" + rule.getPrivateIpAddress() + ":"
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

}
