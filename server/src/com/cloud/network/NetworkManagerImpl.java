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

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.CheckVirtualMachineAnswer;
import com.cloud.agent.api.CheckVirtualMachineCommand;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.CreateZoneVlanAnswer;
import com.cloud.agent.api.CreateZoneVlanCommand;
import com.cloud.agent.api.MigrateCommand;
import com.cloud.agent.api.ModifySshKeysCommand;
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
import com.cloud.alert.AlertManager;
import com.cloud.api.BaseCmd;
import com.cloud.async.AsyncJobExecutor;
import com.cloud.async.AsyncJobManager;
import com.cloud.async.AsyncJobResult;
import com.cloud.async.AsyncJobVO;
import com.cloud.async.BaseAsyncJobExecutor;
import com.cloud.async.executor.AssignToLoadBalancerExecutor;
import com.cloud.async.executor.LoadBalancerParam;
import com.cloud.capacity.dao.CapacityDao;
import com.cloud.configuration.Config;
import com.cloud.configuration.ConfigurationManager;
import com.cloud.configuration.ResourceCount.ResourceType;
import com.cloud.configuration.dao.ConfigurationDao;
import com.cloud.configuration.dao.ResourceLimitDao;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.HostPodVO;
import com.cloud.dc.Vlan;
import com.cloud.dc.VlanVO;
import com.cloud.dc.Vlan.VlanType;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.HostPodDao;
import com.cloud.dc.dao.VlanDao;
import com.cloud.domain.DomainVO;
import com.cloud.domain.dao.DomainDao;
import com.cloud.event.EventState;
import com.cloud.event.EventTypes;
import com.cloud.event.EventVO;
import com.cloud.event.dao.EventDao;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
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
import com.cloud.hypervisor.Hypervisor;
import com.cloud.network.dao.FirewallRulesDao;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.LoadBalancerDao;
import com.cloud.network.dao.SecurityGroupVMMapDao;
import com.cloud.network.listener.RouterStatsListener;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.service.ServiceOffering.GuestIpType;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.storage.StorageManager;
import com.cloud.storage.StoragePoolVO;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.DiskTemplateDao;
import com.cloud.storage.dao.StoragePoolDao;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.storage.dao.VMTemplateHostDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.AccountVO;
import com.cloud.user.User;
import com.cloud.user.UserStatisticsVO;
import com.cloud.user.dao.AccountDao;
import com.cloud.user.dao.UserDao;
import com.cloud.user.dao.UserStatisticsDao;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.Pair;
import com.cloud.utils.StringUtils;
import com.cloud.utils.component.ComponentLocator;
import com.cloud.utils.concurrency.NamedThreadFactory;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.exception.ExecutionException;
import com.cloud.utils.net.NetUtils;
import com.cloud.vm.DomainRouter;
import com.cloud.vm.DomainRouterVO;
import com.cloud.vm.State;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineManager;
import com.cloud.vm.VirtualMachineName;
import com.cloud.vm.DomainRouter.Role;
import com.cloud.vm.VirtualMachine.Event;
import com.cloud.vm.dao.DomainRouterDao;
import com.cloud.vm.dao.UserVmDao;

/**
 * NetworkManagerImpl implements NetworkManager.
 */
@Local(value={NetworkManager.class})
public class NetworkManagerImpl implements NetworkManager, VirtualMachineManager<DomainRouterVO> {
    private static final Logger s_logger = Logger.getLogger(NetworkManagerImpl.class);

    String _name;
    DataCenterDao _dcDao = null;
    VlanDao _vlanDao = null;
    FirewallRulesDao _rulesDao = null;
    SecurityGroupVMMapDao _securityGroupVMMapDao = null;
    LoadBalancerDao _loadBalancerDao = null;
    IPAddressDao _ipAddressDao = null;
    VMTemplateDao _templateDao =  null;
    DiskTemplateDao _diskDao = null;
    DomainRouterDao _routerDao = null;
    UserDao _userDao = null;
    AccountDao _accountDao = null;
    DomainDao _domainDao = null;
    UserStatisticsDao _userStatsDao = null;
    VolumeDao _volsDao = null;
    HostDao _hostDao = null;
    EventDao _eventDao = null;
    ConfigurationDao _configDao;
    HostPodDao _podDao = null;
    VMTemplateHostDao _vmTemplateHostDao = null;
    UserVmDao _vmDao = null;
    ResourceLimitDao _limitDao = null;
    CapacityDao _capacityDao = null;
    AgentManager _agentMgr;
    StorageManager _storageMgr;
    HighAvailabilityManager _haMgr;
    AlertManager _alertMgr;
    AccountManager _accountMgr;
    ConfigurationManager _configMgr;
    AsyncJobManager _asyncMgr;
    StoragePoolDao _storagePoolDao = null;
    ServiceOfferingDao _serviceOfferingDao = null;
    FirewallRulesDao _firewallRulesDao;
    IPAddressDao _publicIpAddressDao;

    long _routerTemplateId = -1;
    int _routerRamSize;
    // String _privateNetmask;
    int _retry = 2;
    String _domain;
    String _instance;
    int _routerCleanupInterval = 3600;
    private ServiceOfferingVO _offering;
    private int _networkRate;
    private int _multicastRate;
    private VMTemplateVO _template;
    
    ScheduledExecutorService _executor;
	
	@Override
	public boolean destroy(DomainRouterVO router) {
		return destroyRouter(router.getId());
	}

    public boolean sendSshKeysToHost(Long hostId, String pubKey, String prvKey) {
    	ModifySshKeysCommand cmd = new ModifySshKeysCommand(pubKey, prvKey);
    	final Answer answer = _agentMgr.easySend(hostId, cmd);
    	
    	if (answer != null)
    		return true;
    	else
    		return false;
    }
    
    @Override @DB
    public String assignSourceNatIpAddress(AccountVO account, final DataCenterVO dc, final String domain, final ServiceOfferingVO serviceOffering) throws ResourceAllocationException {
    	if (serviceOffering.getGuestIpType() == GuestIpType.DirectDual || serviceOffering.getGuestIpType() == GuestIpType.DirectSingle) {
    		return null;
    	}
        final long dcId = dc.getId();
        String sourceNat = null;

        final long accountId = account.getId();
        
        // use a dedicated account object for locking test, so that when we get SQL exception
        // we won't be misled to release an actually un-owned lock
        AccountVO lockedAccount = null;
        Transaction txn = Transaction.currentTxn();
        try {
        	final EventVO event = new EventVO();
            event.setUserId(1L); // system user performed the action...
            event.setAccountId(account.getId());
            event.setType(EventTypes.EVENT_NET_IP_ASSIGN);
            
            txn.start();

            lockedAccount = _accountDao.acquire(accountId);
            if (lockedAccount == null) {
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
                router = createRouter(account.getId(), sourceNat, dcId, domain, serviceOffering);
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
        	if (lockedAccount != null) {
        		if(s_logger.isDebugEnabled())
        			s_logger.debug("Releasing lock account " + accountId);
        		
        		_accountDao.release(accountId);
        	}
        }
    }

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

                List<VolumeVO> vols = _storageMgr.create(account, router, rtrTemplate, dc, pod, _offering, null);
                if (vols == null){
                	_ipAddressDao.unassignIpAddress(guestIp);
                	_routerDao.delete(router.getId());
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
                _routerDao.delete(router.getId());
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
    public DomainRouterVO createRouter(final long accountId, final String publicIpAddress, final long dataCenterId, String domain, final ServiceOfferingVO offering) throws ConcurrentOperationException {
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
        try {
            router = _routerDao.findBy(accountId, dataCenterId);
            if (router != null && router.getState() != State.Creating) {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Router " + router.toString() + " found for account " + accountId + " in data center " + dataCenterId);
                }
                return router;
            }
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
                avoids.add(pod.first().getId());
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Attempting to create in pod " + pod.first().getName());
                }

                router = new DomainRouterVO(id,
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
                            account.getDomainId().longValue(),
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

                List<VolumeVO> vols = _storageMgr.create(account, router, template, dc, pod.first(), _offering, null);
                if(vols != null) {
                    found = true;
                    break;
                }

                _routerDao.delete(router.getId());
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Unable to find storage host or pool in pod " + pod.first().getName() + " (id:" + pod.first().getId() + "), checking other pods");
                }
            }
            
            final EventVO event = new EventVO();
            event.setUserId(1L); // system user performed the action
            event.setAccountId(accountId);
            event.setType(EventTypes.EVENT_ROUTER_CREATE);

            if (!found) {
                event.setDescription("failed to create Domain Router : " + name);
                event.setLevel(EventVO.LEVEL_ERROR);
                _eventDao.persist(event);
                throw new ExecutionException("Unable to create DomainRouter");
            }
            _routerDao.updateIf(router, Event.OperationSucceeded, null);

            s_logger.debug("Router created: id=" + router.getId() + "; name=" + router.getName());

            event.setDescription("successfully created Domain Router : " + router.getName() + " with ip : " + publicIpAddress);
            _eventDao.persist(event);

            return router;
        } catch (final Throwable th) {
            if (th instanceof ExecutionException) {
                s_logger.error("Error while starting router due to " + th.getMessage());
            } else {
                s_logger.error("Unable to create router", th);
            }
            txn.rollback();

            if (router != null && router.getState() == State.Creating) {
                _routerDao.delete(router.getId());
            }
            return null;
        } finally {
            if (account != null) {
            	if(s_logger.isDebugEnabled())
            		s_logger.debug("Releasing lock on account " + account.getId() + " for createRouter");
            	_accountDao.release(account.getId());
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
        
        final EventVO event = new EventVO();
        event.setUserId(User.UID_SYSTEM);
        event.setAccountId(router.getAccountId());
        event.setType(EventTypes.EVENT_ROUTER_DESTROY);
        event.setParameters("id=" + router.getId());
        event.setDescription("successfully destroyed router : " + router.getName());
        _eventDao.persist(event);

        return true;
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

    @Override @DB
    public DomainRouterVO start(long routerId, long eventId) throws StorageUnavailableException, ConcurrentOperationException {
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
	        event.setStartId(eventId);
	        _eventDao.persist(event);
	        
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
            List<StoragePoolVO> sps = _storageMgr.getStoragePoolsForVm(router.getId());
            StoragePoolVO sp = sps.get(0); // FIXME
            
	        HostVO routingHost = (HostVO)_agentMgr.findHost(Host.Type.Routing, dc, pod, sp, _offering, template, router, null, avoid);

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
	            event.setStartId(eventId);
	
	            final List<UserVmVO> vms = _vmDao.listBy(routerId, State.Starting, State.Running, State.Stopped, State.Stopping);
	            if (vms.size() != 0) { // Find it in the existing network.
	                for (final UserVmVO vm : vms) {
	                    if (vm.getVnet() != null) {
	                        vnet = vm.getVnet();
	                        break;
	                    }
	                }
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
	                    final StartRouterCommand cmdStartRouter = new StartRouterCommand(router, _networkRate,
                                   _multicastRate, name, storageIps, vols, mirroredVols);
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
	            } while (--retry > 0 && (routingHost = (HostVO)_agentMgr.findHost(Host.Type.Routing, dc, pod, sp,  _offering, template, router, null, avoid)) != null);

	
	            if (routingHost == null || retry <= 0) {
	                event.setDescription("unable to start Domain Router: " + router.getName());
	                event.setLevel(EventVO.LEVEL_ERROR);
	                _eventDao.persist(event);
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
				final boolean success = associateIP(router, ipAddrList, true);
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
    public boolean associateIP(final DomainRouterVO router, final List<String> ipAddrList, final boolean add) {
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

            cmds[i++] = new IPAssocCommand(router.getInstanceName(), router.getPrivateIpAddress(), ipAddress, add, firstIP, sourceNat, vlanId, vlanGateway, vlanNetmask, vifMacAddress);
            
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
        
        for (int i1=0; i1 < answers.length; i1++) {
            Answer ans = answers[i1];
            return ans.getResult();
        }

        return true;
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
        final Answer [] answers = null;
        try {
            _agentMgr.send(hostId, cmds, false);
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
    public boolean executeAssignToLoadBalancer(AssignToLoadBalancerExecutor executor, LoadBalancerParam param) {
        try {
            executor.getAsyncJobMgr().getExecutorContext().getManagementServer().assignToLoadBalancer(param.getUserId(), param.getLoadBalancerId(), param.getInstanceIdList());
            executor.getAsyncJobMgr().completeAsyncJob(executor.getJob().getId(), AsyncJobResult.STATUS_SUCCEEDED, 0, "success");
        } catch (InvalidParameterValueException e) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Unable to assign vms " + StringUtils.join(param.getInstanceIdList(), ",") + " to load balancer " + param.getLoadBalancerId() + ": " + e.getMessage());
            }

            executor.getAsyncJobMgr().completeAsyncJob(executor.getJob().getId(), AsyncJobResult.STATUS_FAILED, BaseCmd.PARAM_ERROR, "Unable to assign vms " + StringUtils.join(param.getInstanceIdList(), ",") + " to load balancer " + param.getLoadBalancerId());
        } catch (NetworkRuleConflictException e) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Unable to assign vms " + StringUtils.join(param.getInstanceIdList(), ",") + " to load balancer " + param.getLoadBalancerId() + ": " + e.getMessage());
            }

            executor.getAsyncJobMgr().completeAsyncJob(executor.getJob().getId(), AsyncJobResult.STATUS_FAILED, BaseCmd.NET_CONFLICT_LB_RULE_ERROR, e.getMessage());
        } catch (InternalErrorException e) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Unable to assign vms " + StringUtils.join(param.getInstanceIdList(), ",") + " to load balancer " + param.getLoadBalancerId() + ": " + e.getMessage());
            }

            executor.getAsyncJobMgr().completeAsyncJob(executor.getJob().getId(), AsyncJobResult.STATUS_FAILED, BaseCmd.INTERNAL_ERROR, e.getMessage());
        } catch (PermissionDeniedException e) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Unable to assign vms " + StringUtils.join(param.getInstanceIdList(), ",") + " to load balancer " + param.getLoadBalancerId() + ": " + e.getMessage());
            }
            
            executor.getAsyncJobMgr().completeAsyncJob(executor.getJob().getId(),
                AsyncJobResult.STATUS_FAILED, BaseCmd.ACCOUNT_ERROR, e.getMessage());
        } catch(Exception e) {
            s_logger.warn("Unable to assign vms " + StringUtils.join(param.getInstanceIdList(), ",") + " to load balancer " + param.getLoadBalancerId() + ": " + e.getMessage(), e);
            executor.getAsyncJobMgr().completeAsyncJob(executor.getJob().getId(),
                    AsyncJobResult.STATUS_FAILED, BaseCmd.INTERNAL_ERROR, e.getMessage());
        }
        return true;
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

                if (associateIP(router, ipAddrs, false)) {
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
        _configDao = locator.getDao(ConfigurationDao.class);
        if (_configDao == null) {
            throw new ConfigurationException("Unable to get the configuration dao.");
        }
        
        _firewallRulesDao = locator.getDao(FirewallRulesDao.class);
        if (_firewallRulesDao == null) {
            throw new ConfigurationException("Unable to get the firewall rules dao.");
        }
        
        _publicIpAddressDao = locator.getDao(IPAddressDao.class);
        if (_publicIpAddressDao == null) {
            throw new ConfigurationException("Unable to get the ip address dao.");
        }
        
        _configMgr = locator.getManager(ConfigurationManager.class);
        if (_configMgr == null) {
        	throw new ConfigurationException("Unable to get the configuration manager.");
        }

        final Map<String, String> configs = _configDao.getConfiguration("AgentManager", params);

        _routerTemplateId = NumbersUtil.parseInt(configs.get("router.template.id"), 1);

        _routerRamSize = NumbersUtil.parseInt(configs.get("router.ram.size"), 128);

//        String value = configs.get("guest.ip.network");
//        _guestIpAddress = value != null ? value : "10.1.1.1";
//
//        value = configs.get("guest.netmask");
//        _guestNetmask = value != null ? value : "255.255.255.0";

        String value = configs.get("start.retry");
        _retry = NumbersUtil.parseInt(value, 2);

        value = configs.get("router.stats.interval");
        final int routerStatsInterval = NumbersUtil.parseInt(value, 300);

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

        _hostDao = locator.getDao(HostDao.class);
        if (_hostDao == null) {
            throw new ConfigurationException("Unable to get " + HostDao.class.getName());
        }

        _routerDao = locator.getDao(DomainRouterDao.class);
        if (_hostDao == null) {
            throw new ConfigurationException("Unable to get " + DomainRouterDao.class.getName());
        }
        
        _storagePoolDao = locator.getDao(StoragePoolDao.class);
        if (_storagePoolDao == null) {
            throw new ConfigurationException("Unable to find " + StoragePoolDao.class);
        }
        
        _serviceOfferingDao = locator.getDao(ServiceOfferingDao.class);
        if (_serviceOfferingDao == null) {
        	throw new ConfigurationException("Unable to find " + ServiceOfferingDao.class);
        }

        _podDao = locator.getDao(HostPodDao.class);
        if (_podDao == null) {
            throw new ConfigurationException("Unable to get " + HostPodDao.class.getName());
        }

        _userDao = locator.getDao(UserDao.class);
        if (_userDao == null) {
            throw new ConfigurationException("Unable to get " + UserDao.class.getName());
        }

        _accountDao = locator.getDao(AccountDao.class);
        if (_accountDao == null) {
            throw new ConfigurationException("Unable to get " + AccountDao.class.getName());
        }
        
        _domainDao = locator.getDao(DomainDao.class);
        if (_domainDao == null) {
            throw new ConfigurationException("Unable to get " + DomainDao.class.getName());
        }
        
        _limitDao = locator.getDao(ResourceLimitDao.class);
        if (_limitDao == null) {
            throw new ConfigurationException("Unable to get " + ResourceLimitDao.class.getName());
        }

        _userStatsDao = locator.getDao(UserStatisticsDao.class);
        if (_userStatsDao == null) {
            throw new ConfigurationException("Unable to get " + UserStatisticsDao.class.getName());
        }

        _rulesDao = locator.getDao(FirewallRulesDao.class);
        if (_rulesDao == null) {
            throw new ConfigurationException("Unable to get " + FirewallRulesDao.class.getName());
        }

        _securityGroupVMMapDao = locator.getDao(SecurityGroupVMMapDao.class);
        if (_securityGroupVMMapDao == null) {
            throw new ConfigurationException("Unable to get " + SecurityGroupVMMapDao.class.getName());
        }

        _loadBalancerDao = locator.getDao(LoadBalancerDao.class);
        if (_loadBalancerDao == null) {
            throw new ConfigurationException("Unable to get " + LoadBalancerDao.class.getName());
        }

        _ipAddressDao = locator.getDao(IPAddressDao.class);
        if (_ipAddressDao == null) {
            throw new ConfigurationException("Unable to get " + IPAddressDao.class.getName());
        }

        _dcDao = locator.getDao(DataCenterDao.class);
        if (_dcDao == null) {
            throw new ConfigurationException("Unable to get " + DataCenterDao.class.getName());
        }
        
        _vlanDao = locator.getDao(VlanDao.class);
        if (_vlanDao == null) {
        	throw new ConfigurationException("Unable to get " + VlanDao.class.getName());
        }

        _volsDao = locator.getDao(VolumeDao.class);
        if (_volsDao == null) {
            throw new ConfigurationException("Unable to get " + VolumeDao.class.getName());
        }

        _templateDao = locator.getDao(VMTemplateDao.class);
        if (_templateDao == null) {
            throw new ConfigurationException("Unable to get " + VMTemplateDao.class.getName());
        }

        _diskDao = locator.getDao(DiskTemplateDao.class);
        if (_diskDao == null) {
            throw new ConfigurationException("Unable to get " + DiskTemplateDao.class.getName());
        }

        _vmTemplateHostDao = locator.getDao(VMTemplateHostDao.class);
        if (_vmTemplateHostDao == null) {
            throw new ConfigurationException("Unable to get " + VMTemplateHostDao.class.getName());
        }

        _eventDao = locator.getDao(EventDao.class);
        if (_eventDao == null) {
            throw new ConfigurationException("unable to get " + EventDao.class.getName());
        }

        _vmDao = locator.getDao(UserVmDao.class);
        if (_vmDao == null) {
            throw new ConfigurationException("Unable to get " + UserVmDao.class.getName());
        }

        final UserStatisticsDao statsDao = locator.getDao(UserStatisticsDao.class);
        if (statsDao == null) {
            throw new ConfigurationException("Unable to get " + UserStatisticsDao.class.getName());
        }

        _capacityDao = locator.getDao(CapacityDao.class);
        if (_capacityDao == null) {
            throw new ConfigurationException("Unable to get " + CapacityDao.class.getName());
        }

        _agentMgr = locator.getManager(AgentManager.class);
        if (_agentMgr == null) {
            throw new ConfigurationException("Unable to get " + AgentManager.class.getName());
        }

        _agentMgr.registerForHostEvents(ComponentLocator.inject(RouterStatsListener.class, routerStatsInterval), true, false, false);
        _agentMgr.registerForHostEvents(new SshKeysDistriMonitor(this, _hostDao, _configDao), true, false, false);
        
        _storageMgr = locator.getManager(StorageManager.class);
        if (_storageMgr == null) {
        	throw new ConfigurationException("Unable to get " + StorageManager.class.getName());
        }

        _haMgr = locator.getManager(HighAvailabilityManager.class);
        if (_haMgr == null) {
        	throw new ConfigurationException("Unable to get " + HighAvailabilityManager.class.getName());
        }
        
        _haMgr.registerHandler(VirtualMachine.Type.DomainRouter, this);

        _alertMgr = locator.getManager(AlertManager.class);
        if (_alertMgr == null) {
            throw new ConfigurationException("Unable to get " + AlertManager.class.getName());
        }
        
        _accountMgr = locator.getManager(AccountManager.class);
        if (_accountMgr == null) {
            throw new ConfigurationException("Unable to get " + AccountManager.class.getName());
        }
        
        _asyncMgr = locator.getManager(AsyncJobManager.class);
		if (_asyncMgr == null) {
			throw new ConfigurationException("Unable to get " + AsyncJobManager.class.getName());
		}
		
        boolean useLocalStorage = Boolean.parseBoolean((String)params.get(Config.SystemVMUseLocalStorage.key()));
        String networkRateStr = _configDao.getValue("network.throttling.rate");
        String multicastRateStr = _configDao.getValue("multicast.throttling.rate");
        _networkRate = ((networkRateStr == null) ? 200 : Integer.parseInt(networkRateStr));
        _multicastRate = ((multicastRateStr == null) ? 10 : Integer.parseInt(multicastRateStr));
        _offering = new ServiceOfferingVO("Fake Offering For DomR", 1, _routerRamSize, 0, 0, 0, false, null, GuestIpType.Virtualized, useLocalStorage, true, null);
        _offering.setUniqueName("Cloud.Com-SoftwareRouter");
        _offering = _serviceOfferingDao.persistSystemServiceOffering(_offering);
        _template = _templateDao.findById(_routerTemplateId);
        if (_template == null) {
            throw new ConfigurationException("Unable to find the template for the router: " + _routerTemplateId);
        }
        
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
            router.setStorageIp(null);
            
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
        _eventDao.persist(event);
        
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
    
            final StopCommand stop = new StopCommand(router, router.getInstanceName(), router.getVnet());
    
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
        if (fromHost.getHypervisorType() != Hypervisor.Type.KVM && fromHost.getClusterId() == null) {
            s_logger.debug("The host is not in a cluster");
            return null;
        }
        avoid.add(fromHost);

        while ((routingHost = (HostVO)_agentMgr.findHost(Host.Type.Routing, dc, pod, sp, _offering, _template, router, fromHost, avoid)) != null) {
            avoid.add(routingHost);
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Trying to migrate router to host " + routingHost.getName());
            }

            if( ! _storageMgr.share(router, vols, routingHost, false) ) {
                s_logger.warn("Can not share " + vol.getPath() + " to " + router.getName() );
                throw new StorageUnavailableException(vol.getPoolId());
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
	public DomainRouterVO addVirtualMachineToGuestNetwork(UserVmVO vm, String password) throws ConcurrentOperationException {
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
    	ipAddressSB.join("virtualNetworkVlanSB", virtualNetworkVlanSB, ipAddressSB.entity().getVlanDbId(), virtualNetworkVlanSB.entity().getId());
    	
    	SearchCriteria ipAddressSC = ipAddressSB.create();
    	ipAddressSC.setParameters("accountId", accountId);
    	ipAddressSC.setParameters("dataCenterId", dcId);
    	if (sourceNat != null) {
    		ipAddressSC.setParameters("sourceNat", sourceNat);
    	}
    	ipAddressSC.setJoinParameters("virtualNetworkVlanSB", "vlanType", VlanType.VirtualNetwork);
		
		return _ipAddressDao.search(ipAddressSC, null);
    }

	@Override
	public void deleteRule(long ruleId, long userId, long accountId)throws InvalidParameterValueException, PermissionDeniedException,InternalErrorException 
	{
        Exception e = null;
        try {
            FirewallRuleVO rule = _firewallRulesDao.findById(ruleId);
            if (rule != null) {
                boolean success = false;

                try {
                    if (rule.isForwarding()) {
                        success = deleteIpForwardingRule(userId, accountId, rule.getPublicIpAddress(), rule.getPublicPort(), rule.getPrivateIpAddress(), rule.getPrivatePort(),
                                rule.getProtocol());
                    } else {
                        success = deleteLoadBalancingRule(userId, accountId, rule.getPublicIpAddress(), rule.getPublicPort(), rule.getPrivateIpAddress(), rule.getPrivatePort(),
                                rule.getAlgorithm());
                    }
                } catch (Exception ex) {
                    e = ex;
                }

                String description;
                String type = EventTypes.EVENT_NET_RULE_DELETE;
                String level = EventVO.LEVEL_INFO;
                String ruleName = rule.isForwarding() ? "ip forwarding" : "load balancer";

                if (success) {
                    String desc = "deleted " + ruleName + " rule [" + rule.getPublicIpAddress() + ":" + rule.getPublicPort() + "]->[" + rule.getPrivateIpAddress() + ":"
                            + rule.getPrivatePort() + "] " + rule.getProtocol();
                    if (!rule.isForwarding()) {
                        desc = desc + ", algorithm = " + rule.getAlgorithm();
                    }
                    description = desc;
                } else {
                    level = EventVO.LEVEL_ERROR;
                    String desc = "deleted " + ruleName + " rule [" + rule.getPublicIpAddress() + ":" + rule.getPublicPort() + "]->[" + rule.getPrivateIpAddress() + ":"
                            + rule.getPrivatePort() + "] " + rule.getProtocol();
                    if (!rule.isForwarding()) {
                        desc = desc + ", algorithm = " + rule.getAlgorithm();
                    }
                    description = desc;
                }
                
                IPAddressVO ipAddr = _ipAddressDao.findById(rule.getPublicIpAddress());
                
                saveEvent(userId, ipAddr!=null?ipAddr.getAccountId():null, level, type, description);
            }
        } finally {
            if (e != null) {
                if (e instanceof InvalidParameterValueException) {
                    throw (InvalidParameterValueException) e;
                } else if (e instanceof PermissionDeniedException) {
                    throw (PermissionDeniedException) e;
                } else if (e instanceof InternalErrorException) {
                    throw (InternalErrorException) e;
                }
            }
        }
	}
	
    @DB
    protected boolean deleteIpForwardingRule(long userId, long accountId, String publicIp, String publicPort, String privateIp, String privatePort, String proto)
            throws PermissionDeniedException, InvalidParameterValueException, InternalErrorException {

        Transaction txn = Transaction.currentTxn();
        boolean locked = false;
        try {
            AccountVO accountVO = _accountDao.findById(accountId);
            if (accountVO == null) {
                // throw this exception because hackers can use the api to probe
                // for existing user ids
                throw new PermissionDeniedException("Account does not own supplied address");
            }
 
            // although we are not writing these values to the DB, we will check
            // them out of an abundance
            // of caution (may not be warranted)
            if (!NetUtils.isValidPort(publicPort) || !NetUtils.isValidPort(privatePort)) {
                throw new InvalidParameterValueException("Invalid value for port");
            }
//            if (!NetUtils.isValidPrivateIp(privateIp, _configs.get("guest.ip.network"))) {
//                throw new InvalidParameterValueException("Invalid private ip address");
//            }
            if (!NetUtils.isValidProto(proto)) {
                throw new InvalidParameterValueException("Invalid protocol");
            }
            IPAddressVO ipVO = _publicIpAddressDao.acquire(publicIp);
            if (ipVO == null) {
                // throw this exception because hackers can use the api to probe for allocated ips
                throw new PermissionDeniedException("User does not own supplied address");
            }

            locked = true;
            if ((ipVO.getAllocated() == null) || (ipVO.getAccountId() == null) || (ipVO.getAccountId().longValue() != accountId)) {
                // FIXME: if admin account, make sure the user is visible in the
                // admin's domain, or has that checking been done by this point?
                if (!BaseCmd.isAdmin(accountVO.getType())) {
                    throw new PermissionDeniedException("User/account does not own supplied address");
                }
            }

            txn.start();

            List<FirewallRuleVO> fwdings = _firewallRulesDao.listIPForwardingForUpdate(publicIp, publicPort, proto);
            FirewallRuleVO fwRule = null;
            if (fwdings.size() == 0) {
                throw new InvalidParameterValueException("No such rule");
            } else if (fwdings.size() == 1) {
                fwRule = fwdings.get(0);
                if (fwRule.getPrivateIpAddress().equalsIgnoreCase(privateIp) && fwRule.getPrivatePort().equals(privatePort)) {
                    _firewallRulesDao.delete(fwRule.getId());
                } else {
                    throw new InvalidParameterValueException("No such rule");
                }
            } else {
                throw new InternalErrorException("Multiple matches. Please contact support");
            }
            fwRule.setEnabled(false);
            boolean success = updateFirewallRule(fwRule, null, null);
            if (!success) {
                throw new InternalErrorException("Failed to update router");
            }
            txn.commit();
            return success;
        } catch (Throwable e) {
            if (e instanceof InvalidParameterValueException) {
                throw (InvalidParameterValueException) e;
            } else if (e instanceof PermissionDeniedException) {
                throw (PermissionDeniedException) e;
            } else if (e instanceof InternalErrorException) {
                s_logger.warn("ManagementServer error", e);
                throw (InternalErrorException) e;
            }
            s_logger.warn("ManagementServer error", e);
        } finally {
            if (locked) {
                _publicIpAddressDao.release(publicIp);
            }
        }
        return false;
    }

    @DB
    private boolean deleteLoadBalancingRule(long userId, long accountId, String publicIp, String publicPort, String privateIp, String privatePort, String algo)
            throws PermissionDeniedException, InvalidParameterValueException, InternalErrorException {
        Transaction txn = Transaction.currentTxn();
        boolean locked = false;
        try {
            AccountVO accountVO = _accountDao.findById(accountId);
            if (accountVO == null) {
                // throw this exception because hackers can use the api to probe
                // for existing user ids
                throw new PermissionDeniedException("Account does not own supplied address");
            }
            // although we are not writing these values to the DB, we will check
            // them out of an abundance
            // of caution (may not be warranted)
            if (!NetUtils.isValidPort(publicPort) || !NetUtils.isValidPort(privatePort)) {
                throw new InvalidParameterValueException("Invalid value for port");
            }
//            if (!NetUtils.isValidPrivateIp(privateIp, _configs.get("guest.ip.network"))) {
//                throw new InvalidParameterValueException("Invalid private ip address");
//            }
            if (!NetUtils.isValidAlgorithm(algo)) {
                throw new InvalidParameterValueException("Invalid protocol");
            }

            IPAddressVO ipVO = _publicIpAddressDao.acquire(publicIp);

            if (ipVO == null) {
                // throw this exception because hackers can use the api to probe
                // for allocated ips
                throw new PermissionDeniedException("User does not own supplied address");
            }

            locked = true;
            if ((ipVO.getAllocated() == null) || (ipVO.getAccountId() == null) || (ipVO.getAccountId().longValue() != accountId)) {
                // FIXME: the user visible from the admin account's domain? has
                // that check been done already?
                if (!BaseCmd.isAdmin(accountVO.getType())) {
                    throw new PermissionDeniedException("User does not own supplied address");
                }
            }

            List<FirewallRuleVO> fwdings = _firewallRulesDao.listLoadBalanceRulesForUpdate(publicIp, publicPort, algo);
            FirewallRuleVO fwRule = null;
            if (fwdings.size() == 0) {
                throw new InvalidParameterValueException("No such rule");
            }
            for (FirewallRuleVO frv : fwdings) {
                if (frv.getPrivateIpAddress().equalsIgnoreCase(privateIp) && frv.getPrivatePort().equals(privatePort)) {
                    fwRule = frv;
                    break;
                }
            }

            if (fwRule == null) {
                throw new InvalidParameterValueException("No such rule");
            }

            txn.start();

            fwRule.setEnabled(false);
            _firewallRulesDao.update(fwRule.getId(), fwRule);

            boolean success = updateFirewallRule(fwRule, null, null);
            if (!success) {
                throw new InternalErrorException("Failed to update router");
            }
            _firewallRulesDao.delete(fwRule.getId());

            txn.commit();
            return success;
        } catch (Throwable e) {
            if (e instanceof InvalidParameterValueException) {
                throw (InvalidParameterValueException) e;
            } else if (e instanceof PermissionDeniedException) {
                throw (PermissionDeniedException) e;
            } else if (e instanceof InternalErrorException) {
                s_logger.warn("ManagementServer error", e);
                throw (InternalErrorException) e;
            }
            s_logger.warn("ManagementServer error", e);
        } finally {
            if (locked) {
                _publicIpAddressDao.release(publicIp);
            }
        }
        return false;
    }

    private Long saveEvent(Long userId, Long accountId, String level, String type, String description) {
        EventVO event = new EventVO();
        event.setUserId(userId);
        event.setAccountId(accountId);
        event.setType(type);
        event.setDescription(description);
        event.setLevel(level);
        event = _eventDao.persist(event);
        return event.getId();
    }

}
