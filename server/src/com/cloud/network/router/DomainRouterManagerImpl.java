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
package com.cloud.network.router;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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

import com.cloud.agent.AgentManager;
import com.cloud.agent.AgentManager.OnError;
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
import com.cloud.agent.api.check.CheckSshAnswer;
import com.cloud.agent.api.check.CheckSshCommand;
import com.cloud.agent.api.routing.DhcpEntryCommand;
import com.cloud.agent.api.routing.IPAssocCommand;
import com.cloud.agent.api.routing.RemoteAccessVpnCfgCommand;
import com.cloud.agent.api.routing.SavePasswordCommand;
import com.cloud.agent.api.routing.SetPortForwardingRulesCommand;
import com.cloud.agent.api.routing.VmDataCommand;
import com.cloud.agent.api.routing.VpnUsersCfgCommand;
import com.cloud.agent.manager.Commands;
import com.cloud.alert.AlertManager;
import com.cloud.api.commands.RebootRouterCmd;
import com.cloud.api.commands.StartRouterCmd;
import com.cloud.api.commands.StopRouterCmd;
import com.cloud.api.commands.UpgradeRouterCmd;
import com.cloud.async.AsyncJobExecutor;
import com.cloud.async.AsyncJobManager;
import com.cloud.async.AsyncJobVO;
import com.cloud.async.BaseAsyncJobExecutor;
import com.cloud.capacity.dao.CapacityDao;
import com.cloud.configuration.Config;
import com.cloud.configuration.ConfigurationManager;
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
import com.cloud.deploy.DataCenterDeployment;
import com.cloud.deploy.DeployDestination;
import com.cloud.domain.DomainVO;
import com.cloud.domain.dao.DomainDao;
import com.cloud.event.Event;
import com.cloud.event.EventTypes;
import com.cloud.event.EventUtils;
import com.cloud.event.EventVO;
import com.cloud.event.dao.EventDao;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InsufficientNetworkCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.exception.StorageUnavailableException;
import com.cloud.ha.HighAvailabilityManager;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.network.DomainRouterService;
import com.cloud.network.IPAddressVO;
import com.cloud.network.IpAddress;
import com.cloud.network.Network;
import com.cloud.network.NetworkManager;
import com.cloud.network.NetworkVO;
import com.cloud.network.Networks.TrafficType;
import com.cloud.network.RemoteAccessVpnVO;
import com.cloud.network.SshKeysDistriMonitor;
import com.cloud.network.VpnUserVO;
import com.cloud.network.dao.FirewallRulesDao;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.LoadBalancerDao;
import com.cloud.network.dao.LoadBalancerVMMapDao;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkRuleConfigDao;
import com.cloud.network.dao.RemoteAccessVpnDao;
import com.cloud.network.dao.VpnUserDao;
import com.cloud.network.router.VirtualRouter.Role;
import com.cloud.network.rules.PortForwardingRule;
import com.cloud.network.rules.PortForwardingRuleVO;
import com.cloud.network.rules.RulesManager;
import com.cloud.offering.NetworkOffering;
import com.cloud.offerings.NetworkOfferingVO;
import com.cloud.offerings.dao.NetworkOfferingDao;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.storage.GuestOSVO;
import com.cloud.storage.StorageManager;
import com.cloud.storage.StoragePoolVO;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.GuestOSDao;
import com.cloud.storage.dao.StoragePoolDao;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.storage.dao.VMTemplateHostDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.AccountService;
import com.cloud.user.AccountVO;
import com.cloud.user.User;
import com.cloud.user.UserContext;
import com.cloud.user.UserStatisticsVO;
import com.cloud.user.UserVO;
import com.cloud.user.dao.AccountDao;
import com.cloud.user.dao.UserDao;
import com.cloud.user.dao.UserStatisticsDao;
import com.cloud.uservm.UserVm;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.Pair;
import com.cloud.utils.component.ComponentLocator;
import com.cloud.utils.component.Inject;
import com.cloud.utils.concurrency.NamedThreadFactory;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.exception.ExecutionException;
import com.cloud.utils.net.NetUtils;
import com.cloud.vm.DomainRouterVO;
import com.cloud.vm.NicProfile;
import com.cloud.vm.NicVO;
import com.cloud.vm.ReservationContext;
import com.cloud.vm.State;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineGuru;
import com.cloud.vm.VirtualMachineManager;
import com.cloud.vm.VirtualMachineName;
import com.cloud.vm.VirtualMachineProfile;
import com.cloud.vm.dao.DomainRouterDao;
import com.cloud.vm.dao.NicDao;
import com.cloud.vm.dao.UserVmDao;

/**
 * NetworkManagerImpl implements NetworkManager.
 */
@Local(value={DomainRouterManager.class, DomainRouterService.class})
public class DomainRouterManagerImpl implements DomainRouterManager, DomainRouterService, VirtualMachineGuru<DomainRouterVO> {
    private static final Logger s_logger = Logger.getLogger(DomainRouterManagerImpl.class);

    String _name;
    @Inject DataCenterDao _dcDao = null;
    @Inject VlanDao _vlanDao = null;
    @Inject FirewallRulesDao _rulesDao = null;
    @Inject LoadBalancerDao _loadBalancerDao = null;
    @Inject LoadBalancerVMMapDao _loadBalancerVMMapDao = null;
    @Inject IPAddressDao _ipAddressDao = null;
    @Inject VMTemplateDao _templateDao =  null;
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
    @Inject AccountService _accountService;
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
    @Inject NetworkDao _networkConfigurationDao = null;
    @Inject NicDao _nicDao;
    @Inject GuestOSDao _guestOSDao = null;
    @Inject NetworkManager _networkMgr;
    @Inject VirtualMachineManager _itMgr;
    @Inject VpnUserDao _vpnUsersDao;
    @Inject RemoteAccessVpnDao _remoteAccessVpnDao;
    @Inject RulesManager _rulesMgr;
    @Inject NetworkDao _networkDao;
    
    long _routerTemplateId = -1;
    int _routerRamSize;
    // String _privateNetmask;
    int _retry = 2;
    String _domain;
    String _instance;
	String _defaultHypervisorType;
	String _mgmt_host;
	
    int _routerCleanupInterval = 3600;
    int _routerStatsInterval = 300;
    private ServiceOfferingVO _offering;
    private int _networkRate;
    private int _multicastRate;
    String _networkDomain;
    
    private VMTemplateVO _template;
    
    ScheduledExecutorService _executor;
    
    Account _systemAcct;
    boolean _useNewNetworking;
	
    @Override
    public DomainRouterVO getRouter(long accountId, long dataCenterId) {
        return _routerDao.findBy(accountId, dataCenterId);
    }
    
    @Override
    public DomainRouterVO getRouter(String publicIpAddress) {
        return _routerDao.findByPublicIpAddress(publicIpAddress);
    }
    
	@Override
	public boolean destroy(DomainRouterVO router) {
		return destroyRouter(router.getId());
	}

    @Override
    public boolean sendSshKeysToHost(Long hostId, String pubKey, String prvKey) {
    	ModifySshKeysCommand cmd = new ModifySshKeysCommand(pubKey, prvKey);
    	final Answer answer = _agentMgr.easySend(hostId, cmd);
    	
    	if (answer != null) {
            return true;
        } else {
            return false;
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
        pod = _podDao.acquireInLockTable(podId, 20*60);
        if (pod == null) {
            	throw new ConcurrentOperationException("Unable to acquire lock on pod " + podId );
        }
        if(s_logger.isDebugEnabled()) {
            s_logger.debug("Lock on pod " + podId + " is acquired");
        }
        
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
            final String guestIp = null;//_ipAddressDao.assignIpAddress(accountIdForDHCPServer, domainIdForDHCPServer, guestVlan.getId(), false).getAddress();

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
            router = _routerDao.acquireInLockTable(router.getId());
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
                event.setDescription("failed to create DHCP Server : " + router.getHostName());
                event.setLevel(EventVO.LEVEL_ERROR);
                _eventDao.persist(event);
                throw new ExecutionException("Unable to create DHCP Server");
            }
            _itMgr.stateTransitTo(router, VirtualMachine.Event.OperationSucceeded, null);

            s_logger.info("DHCP server created: id=" + router.getId() + "; name=" + router.getHostName() + "; vlan=" + guestVlan.getVlanId() + "; pod=" + pod.getName());

            event.setDescription("successfully created DHCP Server : " + router.getHostName() + " with ip : " + router.getGuestIpAddress());
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
                _routerDao.releaseFromLockTable(id);
            }
            if (pod != null) {
                if(s_logger.isDebugEnabled()) {
                    s_logger.debug("Releasing lock on pod " + podId);
                }
            	_podDao.releaseFromLockTable(pod.getId());
            }
        }
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
        
        final AccountVO account = _accountDao.acquireInLockTable(accountId);
        if (account == null) {
        	throw new ConcurrentOperationException("Unable to acquire account " + accountId);
        }
        
        if(s_logger.isDebugEnabled()) {
            s_logger.debug("lock on account " + accountId + " for createRouter is acquired");
        }

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
            event.setState(Event.State.Started);
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
            VlanVO vlan = _vlanDao.findById(ipVO.getVlanId());
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
            _itMgr.stateTransitTo(router, VirtualMachine.Event.OperationSucceeded, null);

            s_logger.debug("Router created: id=" + router.getId() + "; name=" + router.getHostName());
            
            event = new EventVO();
            event.setUserId(1L); // system user performed the action
            event.setAccountId(accountId);
            event.setType(EventTypes.EVENT_ROUTER_CREATE);
            event.setStartId(startEventId);
            event.setDescription("successfully created Domain Router : " + router.getHostName() + " with ip : " + publicIpAddress);
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
            	if(s_logger.isDebugEnabled()) {
                    s_logger.debug("Releasing lock on account " + account.getId() + " for createRouter");
                }
            	_accountDao.releaseFromLockTable(account.getId());
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

        DomainRouterVO router = _routerDao.acquireInLockTable(routerId);

        if (router == null) {
            s_logger.debug("Unable to acquire lock on router " + routerId);
            return false;
        }
        
        EventVO event = new EventVO();
        event.setUserId(User.UID_SYSTEM);
        event.setAccountId(router.getAccountId());
        event.setType(EventTypes.EVENT_ROUTER_DESTROY);
        event.setState(Event.State.Started);
        event.setParameters("id=" + routerId);
        event.setDescription("Starting to destroy router : " + router.getHostName());
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
            if (! _itMgr.stateTransitTo(router, VirtualMachine.Event.DestroyRequested, router.getHostId())) {
                s_logger.debug("VM " + router.toString() + " is not in a state to be destroyed.");
                return false;
            }
        } finally {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Release lock on router " + routerId + " for stop");
            }
            _routerDao.releaseFromLockTable(routerId);
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
        completedEvent.setDescription("successfully destroyed router : " + router.getHostName());
        _eventDao.persist(completedEvent);

        return true;
    }
    
    @Override
    @DB
    public VirtualRouter upgradeRouter(UpgradeRouterCmd cmd) throws InvalidParameterValueException, PermissionDeniedException {
        Long routerId = cmd.getId();
        Long serviceOfferingId = cmd.getServiceOfferingId();
        Account account = UserContext.current().getAccount();

        DomainRouterVO router = _routerDao.findById(routerId);
        if (router == null) {
            throw new InvalidParameterValueException("Unable to find router with id " + routerId);
        }

        if ((account != null) && !_domainDao.isChildDomain(account.getDomainId(), router.getDomainId())) {
            throw new PermissionDeniedException("Invalid domain router id (" + routerId + ") given, unable to stop router.");
        }

        if (router.getServiceOfferingId() == serviceOfferingId) {
            s_logger.debug("Router: " + routerId + "already has service offering: " + serviceOfferingId);
            return _routerDao.findById(routerId);
        }

        ServiceOfferingVO newServiceOffering = _serviceOfferingDao.findById(serviceOfferingId);
        if (newServiceOffering == null) {
            throw new InvalidParameterValueException("Unable to find service offering with id " + serviceOfferingId);
        }

        ServiceOfferingVO currentServiceOffering = _serviceOfferingDao.findById(router.getServiceOfferingId());

        if (!currentServiceOffering.getGuestIpType().equals(newServiceOffering.getGuestIpType())) {
            throw new InvalidParameterValueException("Can't upgrade router, due to the new network type: " + newServiceOffering.getGuestIpType() + " being different from " +
                    "current network type: " + currentServiceOffering.getGuestIpType());
        }
        if (currentServiceOffering.getUseLocalStorage() != newServiceOffering.getUseLocalStorage()) {
            throw new InvalidParameterValueException("Can't upgrade, due to new local storage status : " + newServiceOffering.getGuestIpType() + " is different from " +
                    "curruent local storage status: " + currentServiceOffering.getUseLocalStorage());
        }

        router.setServiceOfferingId(serviceOfferingId);
        if (_routerDao.update(routerId, router)) {
            return _routerDao.findById(routerId);
        } else {
            throw new CloudRuntimeException("Unable to upgrade router " + routerId);
        }
        
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
        final String vmName = router.getHostName();
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
    public VirtualRouter startRouter(StartRouterCmd cmd) throws InvalidParameterValueException, PermissionDeniedException, ResourceUnavailableException, InsufficientCapacityException, ConcurrentOperationException{
        if (_useNewNetworking) {
            return startRouter(cmd.getId());
        }
    	Long routerId = cmd.getId();
    	Account account = UserContext.current().getAccount();
    	
	    //verify parameters
        DomainRouterVO router = _routerDao.findById(routerId);
        if (router == null) {
        	throw new InvalidParameterValueException ("Unable to find router with id " + routerId);
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
            if (s_logger.isInfoEnabled()) {
                s_logger.info("Start router " + routerId + ", update async job-" + job.getId());
            }
            _asyncMgr.updateAsyncJobAttachment(job.getId(), "domain_router", routerId);
        }
    	
    	DomainRouterVO router = _routerDao.acquireInLockTable(routerId);
        if (router == null) {
        	s_logger.debug("Unable to lock the router " + routerId);
        	return router;
        }
        
        if(s_logger.isDebugEnabled()) {
            s_logger.debug("Lock on router " + routerId + " is acquired");
        }
        
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
	        event.setState(Event.State.Started);
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
            StoragePoolVO sp = _storageMgr.getStoragePoolForVm(router.getId());
            
	        HostVO routingHost = (HostVO)_agentMgr.findHost(Host.Type.Routing, dc, pod, sp, offering, template, router, null, avoid);

	        if (routingHost == null) {
	        	s_logger.error("Unable to find a host to start " + router.toString());
	        	return null;
	        }
	        
	        if (! _itMgr.stateTransitTo(router, VirtualMachine.Event.StartRequested, routingHost.getId())) {
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
	            	s_logger.debug("Router: " + router.getHostName() + " discovered vnet: " + vnet + " from existing VMs.");
	            } else {
	            	s_logger.debug("Router: " + router.getHostName() + " was unable to discover vnet from existing VMs. Acquiring new vnet.");
	            }
	
	            String routerMacAddress = null;
	            if (vnet == null && router.getRole() == Role.DHCP_FIREWALL_LB_PASSWD_USERDATA) { // If not found, then get another one.
	                if(USE_POD_VLAN){
	                    vnet = _dcDao.allocatePodVlan(router.getPodId(), router.getAccountId());
	                } else {
	                    vnet = _dcDao.allocateVnet(router.getDataCenterId(), router.getAccountId(), null);
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
	                s_logger.error("Unable to get another vnet while starting router " + router.getHostName());
	                return null;
	            } else {
	            	s_logger.debug("Router: " + router.getHostName() + " is using vnet: " + vnet);
	            }
	           	
	            Answer answer = null;
	            int retry = _retry;

	            do {
	                if (s_logger.isDebugEnabled()) {
	                    s_logger.debug("Trying to start router on host " + routingHost.getName());
	                }
	                
	                String privateIpAddress = null;
	                String privateNetMask = null;
	                
	                if(_defaultHypervisorType == null || !_defaultHypervisorType.equalsIgnoreCase(Hypervisor.HypervisorType.VmWare.toString())) {
	                	privateIpAddress = _dcDao.allocateLinkLocalIpAddress(router.getDataCenterId(), routingHost.getPodId(), router.getId(), null);
	                	privateNetMask = NetUtils.getLinkLocalNetMask();
	                } else {
	                	privateIpAddress = _dcDao.allocatePrivateIpAddress(router.getDataCenterId(), routingHost.getPodId(), router.getId(), null);
	                	privateNetMask = NetUtils.getCidrNetmask(pod.getCidrSize());
	                }
	                
	                if (privateIpAddress == null) {
	                    s_logger.error("Unable to allocate a private ip address while creating router for pod " + routingHost.getPodId());
	                    avoid.add(routingHost);
	                    continue;
	                }

	                if (s_logger.isDebugEnabled()) {
	                    s_logger.debug("Private Ip Address allocated: " + privateIpAddress);
	                }

	                router.setPrivateIpAddress(privateIpAddress);
	                router.setPrivateNetmask(privateNetMask);
	                router.setGuestMacAddress(routerMacAddress);
	                router.setVnet(vnet);
	                /*Ram size can be changed by upgradeRouterCmd*/
	                router.setRamSize(offering.getRamSize());
	                
	                final String name = VirtualMachineName.attachVnet(router.getHostName(), vnet);
	                router.setInstanceName(name);
	                long accountId = router.getAccountId();
	                // Use account level network domain if available
	                String networkDomain = _accountDao.findById(accountId).getNetworkDomain();
	                if(networkDomain == null){
	                    // Use zone level network domain, if account level domain is not available
	                    networkDomain = dc.getDomain();
	                    if(networkDomain == null){
	                        // Use system wide default network domain, if zone wide network domain is also not available
	                        networkDomain = _networkDomain;
	                    }
	                    
	                }
	                router.setDomain(networkDomain);
	                _itMgr.stateTransitTo(router, VirtualMachine.Event.OperationRetry, routingHost.getId());

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
	                            + router.getGuestOSId() + " for vm: " + router.getHostName();
	                        s_logger.debug(msg); 
	                        throw new CloudRuntimeException(msg);
	                    } else {
	                        guestOSDescription = guestOS.getDisplayName();
	                    }
	                    
	                    final StartRouterCommand cmdStartRouter = new StartRouterCommand(router, _networkRate,
	                            _multicastRate, name, storageIps, vols, mirroredVols, guestOSDescription, _mgmt_host);
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
	                                s_logger.debug("Router " + router.getHostName() + " started on " + routingHost.getName());
	                            }
	                            started = true;
	                            break;
	                        } else {
	                            if (s_logger.isDebugEnabled()) {
	                                s_logger.debug("Router " + router.getHostName() + " started on " + routingHost.getName() + " but failed to program rules");
	                            }
	                            sendStopCommand(router);
	                        }
	                    }
	                    s_logger.debug("Unable to start " + router.toString() + " on host " + routingHost.toString() + " due to " + answer.getDetails());
	                } catch (OperationTimedoutException e) {
	                    if (e.isActive()) {
	                        s_logger.debug("Unable to start vm " + router.getHostName() + " due to operation timed out and it is active so scheduling a restart.");
	                        _haMgr.scheduleRestart(router, true);
	                        return null;
	                    }
	                } catch (AgentUnavailableException e) {
	                    s_logger.debug("Agent " + routingHost.toString() + " was unavailable to start VM " + router.getHostName());
	                }
	                avoid.add(routingHost);
	                
	                router.setPrivateIpAddress(null);
	                
	                if(_defaultHypervisorType == null || !_defaultHypervisorType.equalsIgnoreCase(Hypervisor.HypervisorType.VmWare.toString())) {
                        _dcDao.releaseLinkLocalIpAddress(privateIpAddress, router.getDataCenterId(), router.getId());
                    } else {
                        _dcDao.releasePrivateIpAddress(privateIpAddress, router.getDataCenterId(), router.getId());
                    }
	
	                _storageMgr.unshare(router, vols, routingHost);
	            } while (--retry > 0 && (routingHost = (HostVO)_agentMgr.findHost(Host.Type.Routing, dc, pod, sp,  offering, template, router, null, avoid)) != null);

	
	            if (routingHost == null || retry <= 0) {
	                throw new ExecutionException("Couldn't find a routingHost");
	            }
	
	            _itMgr.stateTransitTo(router, VirtualMachine.Event.OperationSucceeded, routingHost.getId());
	            if (s_logger.isDebugEnabled()) {
	                s_logger.debug("Router " + router.toString() + " is now started on " + routingHost.toString());
	            }
	            
	            event.setDescription("successfully started Domain Router: " + router.getHostName());
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
                    _dcDao.releaseVnet(vnet, router.getDataCenterId(), router.getAccountId(), null);
                }

                router.setVnet(null);
                String privateIpAddress = router.getPrivateIpAddress();

                router.setPrivateIpAddress(null);

                if (privateIpAddress != null) {
                	_dcDao.releasePrivateIpAddress(privateIpAddress, router.getDataCenterId(), router.getId());
                }


                if ( _itMgr.stateTransitTo(router, VirtualMachine.Event.OperationFailed, null)) {
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
                if(s_logger.isDebugEnabled()) {
                    s_logger.debug("Releasing lock on router " + routerId);
                }
        		_routerDao.releaseFromLockTable(routerId);
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
        	final List<IPAddressVO> ipAddrs = _networkMgr.listPublicIpAddressesInVirtualNetwork(router.getAccountId(), router.getDataCenterId(), null);
			final List<String> ipAddrList = new ArrayList<String>();
			for (final IPAddressVO ipVO : ipAddrs) {
				ipAddrList.add(ipVO.getAddress());
			}
			if (!ipAddrList.isEmpty()) {
			    try {
    				final boolean success = _networkMgr.associateIP(router, ipAddrList, true, 0);
                    if (!success) {
                        return false;
                    }
                } catch (ConcurrentOperationException e) {
                    s_logger.warn("unable to associate ip due to ", e);
                    return false;
                }
			}
			final List<PortForwardingRuleVO> fwRules = new ArrayList<PortForwardingRuleVO>();
//FIXME:			for (final IPAddressVO ipVO : ipAddrs) {
//				//We need only firewall rules that are either forwarding or for load balancers
//				fwRules.addAll(_rulesDao.listIPForwarding(ipVO.getAddress(), true));
//				fwRules.addAll(_rulesDao.listIpForwardingRulesForLoadBalancers(ipVO.getAddress()));
//			}
//			final List<PortForwardingRuleVO> result = _networkMgr.updateFirewallRules(router
//					.getPublicIpAddress(), fwRules, router);
//			if (result.size() != fwRules.size()) {
//				return false;
//			}
		}
		return resendDhcpEntries(router) && resendVpnServerData(router);
      
    }
    
    private boolean resendDhcpEntries(final DomainRouterVO router){
    	final List<UserVmVO> vms = _vmDao.listBy(router.getId(), State.Creating, State.Starting, State.Running, State.Stopping, State.Stopped, State.Migrating);
    	Commands cmds = new Commands(OnError.Continue);
    	for (UserVmVO vm: vms) {
    		if (vm.getGuestIpAddress() == null || vm.getGuestMacAddress() == null || vm.getHostName() == null) {
                continue;
            }
    		DhcpEntryCommand decmd = new DhcpEntryCommand(vm.getGuestMacAddress(), vm.getGuestIpAddress(), router.getPrivateIpAddress(), vm.getHostName());
    		cmds.addCommand(decmd);
    	}
    	if (cmds.size() > 0) {
            try {
                _agentMgr.send(router.getHostId(), cmds);
            } catch (final AgentUnavailableException e) {
                s_logger.warn("agent unavailable", e);
            } catch (final OperationTimedoutException e) {
                s_logger.warn("Timed Out", e);
            }
            Answer[] answers = cmds.getAnswers();
            if (answers == null ){
                return false;
            }
            int i=0;
            while (i < cmds.size()) {
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
    
    private boolean resendVpnServerData(final DomainRouterVO router) {
    	RemoteAccessVpnVO vpnVO = _remoteAccessVpnDao.findByAccountAndZone(router.getAccountId(), router.getDataCenterId());
    	
    	if (vpnVO != null) {
    		try {
				vpnVO =  startRemoteAccessVpn(vpnVO);
			} catch (ResourceUnavailableException e) {
				s_logger.warn("Unable to resend vpn server information to restarted router: " + router.getInstanceName());
				return false;
			}
    		return (vpnVO  != null);
    	}
    	return true;
    }

    @Override
    public boolean stopRouter(final long routerId, long eventId) {
        AsyncJobExecutor asyncExecutor = BaseAsyncJobExecutor.getCurrentExecutor();
        if (asyncExecutor != null) {
            AsyncJobVO job = asyncExecutor.getJob();

            if (s_logger.isInfoEnabled()) {
                s_logger.info("Stop router " + routerId + ", update async job-" + job.getId());
            }
            _asyncMgr.updateAsyncJobAttachment(job.getId(), "domain_router", routerId);
        }
    	
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Stopping router " + routerId);
        }
        
        return stop(_routerDao.findById(routerId), eventId);
    }
    
    
    @Override
    public VirtualRouter stopRouter(StopRouterCmd cmd) throws InvalidParameterValueException, PermissionDeniedException, ResourceUnavailableException, ConcurrentOperationException{
        if (_useNewNetworking) {
            return stopRouter(cmd.getId());
        }
	    Long routerId = cmd.getId();
        Account account = UserContext.current().getAccount();

	    // verify parameters
        DomainRouterVO router = _routerDao.findById(routerId);
        if (router == null) {
        	throw new InvalidParameterValueException ("Unable to find router with id " + routerId);
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

            if (s_logger.isInfoEnabled()) {
                s_logger.info("Reboot router " + routerId + ", update async job-" + job.getId());
            }
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
        event.setState(Event.State.Started);
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
                event.setDescription("successfully rebooted Domain Router : " + router.getHostName());
                _eventDao.persist(event);
                return true;
            } else {
                event.setDescription("failed to reboot Domain Router : " + router.getHostName());
                event.setLevel(EventVO.LEVEL_ERROR);
                _eventDao.persist(event);
                return false;
            }
        } else {
            return startRouter(routerId, 0) != null;
        }
    }
    
    @Override
    public VirtualRouter rebootRouter(RebootRouterCmd cmd) throws InvalidParameterValueException, PermissionDeniedException{
    	Long routerId = cmd.getId();
    	Account account = UserContext.current().getAccount();
    	
        //verify parameters
        DomainRouterVO router = _routerDao.findById(routerId);
        if (router == null) {
        	throw new InvalidParameterValueException("Unable to find domain router with id " + routerId + ".");
        }

        if ((account != null) && !_domainDao.isChildDomain(account.getDomainId(), router.getDomainId())) {
            throw new PermissionDeniedException("Unable to reboot domain router with id " + routerId + ". Permission denied");
        }
        long eventId = EventUtils.saveScheduledEvent(User.UID_SYSTEM, Account.ACCOUNT_ID_SYSTEM, EventTypes.EVENT_ROUTER_REBOOT, "rebooting Router with Id: "+routerId);
        
    	if (rebootRouter(routerId, eventId)) {
            return _routerDao.findById(routerId);
        } else {
            throw new CloudRuntimeException("Fail to reboot router " + routerId);
        }
    }

    @Override
    public DomainRouterVO getRouter(final long routerId) {
        return _routerDao.findById(routerId);
    }

    @Override
    public List<? extends VirtualRouter> getRouters(final long hostId) {
        return _routerDao.listByHostId(hostId);
    }

    @Override
    public boolean configure(final String name, final Map<String, Object> params) throws ConfigurationException {
        _name = name;

        _executor = Executors.newScheduledThreadPool(1, new NamedThreadFactory("RouterMonitor"));

        final ComponentLocator locator = ComponentLocator.getCurrentLocator();
        
        final Map<String, String> configs = _configDao.getConfiguration("AgentManager", params);

        _mgmt_host = configs.get("host");
        _routerRamSize = NumbersUtil.parseInt(configs.get("router.ram.size"), 128);

//        String value = configs.get("guest.ip.network");
//        _guestIpAddress = value != null ? value : "10.1.1.1";
//
//        value = configs.get("guest.netmask");
//        _guestNetmask = value != null ? value : "255.255.255.0";

        String value = configs.get("start.retry");
        _retry = NumbersUtil.parseInt(value, 2);

		_defaultHypervisorType = _configDao.getValue(Config.HypervisorDefaultType.key());
        
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
        
        _networkDomain = configs.get("domain.suffix");

        s_logger.info("Router configurations: " + "ramsize=" + _routerRamSize + "; templateId=" + _routerTemplateId);

        final UserStatisticsDao statsDao = locator.getDao(UserStatisticsDao.class);
        if (statsDao == null) {
            throw new ConfigurationException("Unable to get " + UserStatisticsDao.class.getName());
        }

        _agentMgr.registerForHostEvents(new SshKeysDistriMonitor(this, _hostDao, _configDao), true, false, false);
        _haMgr.registerHandler(VirtualMachine.Type.DomainRouter, this);
        _itMgr.registerGuru(VirtualMachine.Type.DomainRouter, this);

        boolean useLocalStorage = Boolean.parseBoolean(configs.get(Config.SystemVMUseLocalStorage.key()));
        String networkRateStr = _configDao.getValue("network.throttling.rate");
        String multicastRateStr = _configDao.getValue("multicast.throttling.rate");
        _networkRate = ((networkRateStr == null) ? 200 : Integer.parseInt(networkRateStr));
        _multicastRate = ((multicastRateStr == null) ? 10 : Integer.parseInt(multicastRateStr));
        _offering = new ServiceOfferingVO("System Offering For Software Router", 1, _routerRamSize, 256, 0, 0, false, null, NetworkOffering.GuestIpType.Virtual, useLocalStorage, true, null, true);
        _offering.setUniqueName("Cloud.Com-SoftwareRouter");
        _offering = _serviceOfferingDao.persistSystemServiceOffering(_offering);
        _template = _templateDao.findRoutingTemplate();
        if (_template == null) {
        	s_logger.error("Unable to find system vm template.");
        } else {
        	_routerTemplateId = _template.getId();
        }
        
        _useNewNetworking = Boolean.parseBoolean(configs.get("use.new.networking"));
        
        _systemAcct = _accountService.getSystemAccount();
        
        s_logger.info("DomainRouterManager is configured.");

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

    protected DomainRouterManagerImpl() {
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
    	 _itMgr.stateTransitTo(router, VirtualMachine.Event.AgentReportRunning, router.getHostId());
    }

    @Override
    public void completeStopCommand(final DomainRouterVO router) {
    	completeStopCommand(router, VirtualMachine.Event.AgentReportStopped);
    }
    
    @DB
    public void completeStopCommand(final DomainRouterVO router, final VirtualMachine.Event ev) {
        final long routerId = router.getId();

        final Transaction txn = Transaction.currentTxn();
        try {
            txn.start();
            if (_vmDao.listBy(routerId, State.Starting, State.Running).size() == 0) {
                _dcDao.releaseVnet(router.getVnet(), router.getDataCenterId(), router.getAccountId(), null);
            }

            router.setVnet(null);
            
            String privateIpAddress = router.getPrivateIpAddress();
            
            if (privateIpAddress != null) {
            	if(_defaultHypervisorType == null || !_defaultHypervisorType.equalsIgnoreCase(Hypervisor.HypervisorType.VmWare.toString())) {
                    _dcDao.releaseLinkLocalIpAddress(privateIpAddress, router.getDataCenterId(), router.getId());
                } else {
                    _dcDao.releasePrivateIpAddress(privateIpAddress, router.getDataCenterId(), router.getId());
                }
            }
            router.setPrivateIpAddress(null);

            if (! _itMgr.stateTransitTo(router, ev, null)) {
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
    	
        router = _routerDao.acquireInLockTable(routerId);
        if (router == null) {
            s_logger.debug("Unable to acquire lock on router " + routerId);
            return false;
        }
        
        EventVO event = new EventVO();
        event.setUserId(1L);
        event.setAccountId(router.getAccountId());
        event.setType(EventTypes.EVENT_ROUTER_STOP);
        event.setState(Event.State.Started);
        event.setDescription("Stopping Router with Id: "+routerId);
        event.setStartId(eventId);
        event = _eventDao.persist(event);
        if(eventId == 0){
            eventId = event.getId();
        }
        
        try {
            
        	if(s_logger.isDebugEnabled()) {
                s_logger.debug("Lock on router " + routerId + " for stop is acquired");
            }
        	
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
    
            if (! _itMgr.stateTransitTo(router, VirtualMachine.Event.StopRequested, hostId)) {
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
                    event.setDescription("failed to stop Domain Router : " + router.getHostName());
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
                event.setDescription("failed to stop Domain Router : " + router.getHostName());
                event.setLevel(EventVO.LEVEL_ERROR);
                _eventDao.persist(event);
                _itMgr.stateTransitTo(router, VirtualMachine.Event.OperationFailed, router.getHostId());
                return false;
            }
    
            completeStopCommand(router, VirtualMachine.Event.OperationSucceeded);
            event.setDescription("successfully stopped Domain Router : " + router.getHostName());
            _eventDao.persist(event);
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Router " + router.toString() + " is stopped");
            }
    
            processStopOrRebootAnswer(router, answer);
        } finally {
            if(s_logger.isDebugEnabled()) {
                s_logger.debug("Release lock on router " + routerId + " for stop");
            }
            _routerDao.releaseFromLockTable(routerId);
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
        StoragePoolVO sp = _storageMgr.getStoragePoolForVm(router.getId());

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
                s_logger.warn("Can not share " + vol.getPath() + " to " + router.getHostName() );
                throw new StorageUnavailableException("Can not share " + vol.getPath() + " to " + router.getHostName(), vol);
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

    	if (! _itMgr.stateTransitTo(router, VirtualMachine.Event.MigrationRequested, router.getHostId())) {
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
            _itMgr.stateTransitTo(router, VirtualMachine.Event.AgentReportStopped, null);
            return false;
        }

        final State state = answer.getState();
        if (state == State.Stopped) {
            s_logger.warn("Unable to complete migration as we can not detect it on " + host.getId());
            _itMgr.stateTransitTo(router, VirtualMachine.Event.AgentReportStopped, null);
            return false;
        }

        _itMgr.stateTransitTo(router, VirtualMachine.Event.OperationSucceeded, host.getId());

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
        		s_logger.error("Can't find a domain router to start VM: " + vm.getHostName());
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
	        Commands cmds = new Commands(OnError.Stop);
	        int cmdIndex = 0;
	        int passwordIndex = -1;
	        int vmDataIndex = -1;
	        cmds.addCommand(new DhcpEntryCommand(vm.getGuestMacAddress(), vm.getGuestIpAddress(), router.getPrivateIpAddress(), vm.getHostName()));
	        if (password != null) {
	            final String encodedPassword = rot13(password);
	            cmds.addCommand(new SavePasswordCommand(encodedPassword, vm.getPrivateIpAddress(), router.getPrivateIpAddress(), vm.getHostName()));
	        	passwordIndex = cmdIndex;
	        }
	        	        
	        
	        String serviceOffering = _serviceOfferingDao.findById(vm.getServiceOfferingId()).getDisplayText();
	        String zoneName = _dcDao.findById(vm.getDataCenterId()).getName();
	        String routerPublicIpAddress = (router.getPublicIpAddress() != null) ? router.getPublicIpAddress() : vm.getGuestIpAddress();
	        
	        cmds.addCommand(generateVmDataCommand(router.getPrivateIpAddress(), routerPublicIpAddress, vm.getPrivateIpAddress(), userData, serviceOffering, zoneName, vm.getGuestIpAddress(), vm.getHostName(), vm.getInstanceName(), vm.getId()));
	        vmDataIndex = cmdIndex;
	        
	        Answer[] answers = _agentMgr.send(router.getHostId(), cmds);
	        if (!answers[0].getResult()) {
	        	s_logger.error("Unable to set dhcp entry for " + vm.getId() + " - " + vm.getHostName() +" on domR: " + router.getHostName() + " due to " + answers[0].getDetails());
	        	return null;
	        }
	        
	        if (password != null && !answers[passwordIndex].getResult()) {
	        	s_logger.error("Unable to set password for " + vm.getId() + " - " + vm.getHostName() + " due to " + answers[passwordIndex].getDetails());
	        	return null;
	        }
	        
	        if (vmDataIndex > 0 && !answers[vmDataIndex].getResult()) {
	        	s_logger.error("Unable to set VM data for " + vm.getId() + " - " + vm.getHostName() + " due to " + answers[vmDataIndex].getDetails());
	        	return null;
	        }
	        return router;
        } catch (StorageUnavailableException e) {
        	s_logger.error("Unable to start router " + vm.getDomainRouterId() + " because storage is unavailable.");
        	return null;
        } catch (AgentUnavailableException e) {
        	s_logger.error("Unable to setup the router " + vm.getDomainRouterId() + " for vm " + vm.getId() + " - " + vm.getHostName() + " because agent is unavailable");
        	return null;
		} catch (OperationTimedoutException e) {
        	s_logger.error("Unable to setup the router " + vm.getDomainRouterId() + " for vm " + vm.getId() + " - " + vm.getHostName() + " because agent is too busy");
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
        String zoneVlan = _dcDao.allocateVnet(router.getDataCenterId(), router.getAccountId(), null);
        final DataCenterVO dc = _dcDao.findById(router.getDataCenterId());
        router.setZoneVlan(zoneVlan);
        router.setGuestZoneMacAddress(getRouterMacForZoneVlan(dc, zoneVlan));
        _routerDao.update(router.getId(), router);
        final CreateZoneVlanCommand cmdCreateZoneVlan = new CreateZoneVlanCommand(router);
        CreateZoneVlanAnswer answer = (CreateZoneVlanAnswer) _agentMgr.easySend(router.getHostId(), cmdCreateZoneVlan);
        if(!answer.getResult()){
            s_logger.error("Unable to create zone vlan for router: "+router.getHostName()+ " zoneVlan: "+zoneVlan);
            return null;
        }
        return zoneVlan;
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
                    final NetworkUsageCommand usageCmd = new NetworkUsageCommand(privateIP, router.getHostName());
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

	
	public static boolean isAdmin(short accountType) {
	    return ((accountType == Account.ACCOUNT_TYPE_ADMIN) ||
	            (accountType == Account.ACCOUNT_TYPE_DOMAIN_ADMIN) ||
	            (accountType == Account.ACCOUNT_TYPE_READ_ONLY_ADMIN));
	}
	
	@Override
	public DomainRouterVO deployVirtualRouter(Network guestConfig, DeployDestination dest, Account owner) throws InsufficientCapacityException, StorageUnavailableException, ConcurrentOperationException, ResourceUnavailableException {
	    long dcId = dest.getDataCenter().getId();
	    
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Starting a router for network configurations: virtual="  + guestConfig + " in " + dest);
        }
	    assert guestConfig.getState() == Network.State.Implemented || guestConfig.getState() == Network.State.Setup : "Network is not yet fully implemented: " + guestConfig;
	    assert guestConfig.getTrafficType() == TrafficType.Guest;
	    
        DataCenterDeployment plan = new DataCenterDeployment(dcId);
        
        guestConfig = _networkConfigurationDao.lockRow(guestConfig.getId(), true);
        if (guestConfig == null) {
            throw new ConcurrentOperationException("Unable to get the lock on " + guestConfig);
        }
        
        DomainRouterVO router = _routerDao.findByNetworkConfiguration(guestConfig.getId());
        if (router == null) {
            long id = _routerDao.getNextInSequence(Long.class, "id");
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Creating the router " + id);
            }
            
            //String sourceNatIp = _networkMgr.assignSourceNatIpAddress(owner, dest.getDataCenter());
        
            List<NetworkOfferingVO> offerings = _networkMgr.getSystemAccountNetworkOfferings(NetworkOfferingVO.SystemVmControlNetwork);
            NetworkOfferingVO controlOffering = offerings.get(0);
            NetworkVO controlConfig = _networkMgr.setupNetwork(_systemAcct, controlOffering, plan, null, null, false).get(0);
            
            List<Pair<NetworkVO, NicProfile>> networks = new ArrayList<Pair<NetworkVO, NicProfile>>(3);
            NetworkOfferingVO publicOffering = _networkMgr.getSystemAccountNetworkOfferings(NetworkOfferingVO.SystemVmPublicNetwork).get(0);
            List<NetworkVO> publicConfigs = _networkMgr.setupNetwork(_systemAcct, publicOffering, plan, null, null, false);
            NicProfile defaultNic = new NicProfile();
            defaultNic.setDefaultNic(true);
            //defaultNic.setIp4Address(sourceNatIp);
            defaultNic.setDeviceId(2);
            networks.add(new Pair<NetworkVO, NicProfile>(publicConfigs.get(0), defaultNic));
            NicProfile gatewayNic = new NicProfile();
            gatewayNic.setIp4Address(guestConfig.getGateway());
            gatewayNic.setBroadcastUri(guestConfig.getBroadcastUri());
            gatewayNic.setBroadcastType(guestConfig.getBroadcastDomainType());
            gatewayNic.setIsolationUri(guestConfig.getBroadcastUri());
            gatewayNic.setMode(guestConfig.getMode());
            gatewayNic.setNetmask(NetUtils.getCidrSubNet(guestConfig.getCidr()));
            networks.add(new Pair<NetworkVO, NicProfile>((NetworkVO)guestConfig, gatewayNic));
            networks.add(new Pair<NetworkVO, NicProfile>(controlConfig, null));
            
            router = new DomainRouterVO(id, _offering.getId(), VirtualMachineName.getRouterName(id, _instance), _template.getId(), _template.getGuestOSId(), owner.getDomainId(), owner.getId(), guestConfig.getId(), _offering.getOfferHA());
    	    router = _itMgr.allocate(router, _template, _offering, networks, plan, null, owner);
        }
        
        return _itMgr.start(router, null, _accountService.getSystemUser(), _accountService.getSystemAccount(), null);
	}
	
	
	   @Override
	    public DomainRouterVO deployDhcp(Network guestConfig, DeployDestination dest, Account owner) throws InsufficientCapacityException, StorageUnavailableException, ConcurrentOperationException, ResourceUnavailableException {
	        long dcId = dest.getDataCenter().getId();
	        
	        if (s_logger.isDebugEnabled()) {
	            s_logger.debug("Starting a dhcp for network configurations: dhcp="  + guestConfig + " in " + dest);
	        }
	        assert guestConfig.getState() == Network.State.Implemented || guestConfig.getState() == Network.State.Setup : "Network is not yet fully implemented: " + guestConfig;
	        
	        DataCenterDeployment plan = new DataCenterDeployment(dcId);
	        
	        guestConfig = _networkConfigurationDao.lockRow(guestConfig.getId(), true);
	        if (guestConfig == null) {
	            throw new ConcurrentOperationException("Unable to get the lock on " + guestConfig);
	        }
	        
	        DomainRouterVO router = _routerDao.findByNetworkConfiguration(guestConfig.getId());
	        if (router == null) {
	            long id = _routerDao.getNextInSequence(Long.class, "id");
	            if (s_logger.isDebugEnabled()) {
	                s_logger.debug("Creating the router " + id);
	            }
	        
	            List<NetworkOfferingVO> offerings = _networkMgr.getSystemAccountNetworkOfferings(NetworkOfferingVO.SystemVmControlNetwork);
	            NetworkOfferingVO controlOffering = offerings.get(0);
	            NetworkVO controlConfig = _networkMgr.setupNetwork(_systemAcct, controlOffering, plan, null, null, false).get(0);
	            
	            List<Pair<NetworkVO, NicProfile>> networks = new ArrayList<Pair<NetworkVO, NicProfile>>(3);
	            NicProfile gatewayNic = new NicProfile();
	            gatewayNic.setDefaultNic(true);
	            networks.add(new Pair<NetworkVO, NicProfile>((NetworkVO)guestConfig, gatewayNic));
	            networks.add(new Pair<NetworkVO, NicProfile>(controlConfig, null));
	            
	            router = new DomainRouterVO(id, _offering.getId(), VirtualMachineName.getRouterName(id, _instance), _template.getId(), _template.getGuestOSId(), owner.getDomainId(), owner.getId(), guestConfig.getId(), _offering.getOfferHA());
	            router.setRole(Role.DHCP_USERDATA);
	            router = _itMgr.allocate(router, _template, _offering, networks, plan, null, owner);
	        }
	        
	        return _itMgr.start(router, null, _accountService.getSystemUser(), _accountService.getSystemAccount(), null);
	    }
	
	@Override
    public boolean finalizeVirtualMachineProfile(VirtualMachineProfile<DomainRouterVO> profile, DeployDestination dest, ReservationContext context) {
        
	    DomainRouterVO router = profile.getVirtualMachine();
	    NetworkVO network = _networkDao.findById(router.getNetworkId());
	    
	    String type = null;
	    String dhcpRange = null;

        //get first ip address from network cidr
        String cidr = network.getCidr();
        String[] splitResult = cidr.split("\\/");
        long size = Long.valueOf(splitResult[1]);
        dhcpRange = NetUtils.getIpRangeStartIpFromCidr(splitResult[0], size);

	    
	    String domain = network.getNetworkDomain();
	    if (router.getRole() == Role.DHCP_USERDATA) {
	        type="dhcpsrvr";
	    } else {
	        type = "router";
	    }
	 
	    StringBuilder buf = profile.getBootArgsBuilder();
        buf.append(" template=domP type=" + type);
        buf.append(" name=").append(profile.getHostName());
        NicProfile controlNic = null;
        NicProfile managementNic = null;
        
        for (NicProfile nic : profile.getNics()) {
            int deviceId = nic.getDeviceId();
            buf.append(" eth").append(deviceId).append("ip=").append(nic.getIp4Address());
            buf.append(" eth").append(deviceId).append("mask=").append(nic.getNetmask());
            if (nic.isDefaultNic()) {
                buf.append(" gateway=").append(nic.getGateway());
                buf.append(" dns1=").append(nic.getDns1());
                if (nic.getDns2() != null) {
                    buf.append(" dns2=").append(nic.getDns2());
                }
            }
            if (nic.getTrafficType() == TrafficType.Management) {
                buf.append(" localgw=").append(dest.getPod().getGateway());
                managementNic = nic;
            } else if (nic.getTrafficType() == TrafficType.Control) {
            	// DOMR control command is sent over management server in VMware
            	if(dest.getHost().getHypervisorType() == HypervisorType.VmWare) {
            		buf.append(" mgmtcidr=").append(_mgmt_host);
            		buf.append(" localgw=").append(dest.getPod().getGateway());
            	}
                
                controlNic = nic;
            }
        }
        
        if (dhcpRange != null) {
            buf.append(" dhcprange=" + dhcpRange);
         }
         if (domain != null) {
             buf.append(" domain="+router.getDomain());
         }
  
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Boot Args for " + profile + ": " + buf.toString());
        }
        
        if (controlNic == null) {
            throw new CloudRuntimeException("Didn't start a control port");
        }
        
        profile.setParameter("control.nic", controlNic);
        
        return true;
	}

    @Override
    public boolean finalizeDeployment(Commands cmds, VirtualMachineProfile<DomainRouterVO> profile, DeployDestination dest, ReservationContext context) {
        NicProfile controlNic = (NicProfile)profile.getParameter("control.nic");
        cmds.addCommand("checkSsh", new CheckSshCommand(profile.getInstanceName(), controlNic.getIp4Address(), 3922, 5, 20));
        
        DomainRouterVO router = profile.getVirtualMachine();
        
        List<NicVO> nics = _nicDao.listBy(router.getId());
        for (NicVO nic : nics) {
        	NetworkVO network = _networkDao.findById(nic.getNetworkId());
        	if (network.getTrafficType() == TrafficType.Public) {
        		router.setPublicIpAddress(nic.getIp4Address());
        		router.setPublicNetmask(nic.getNetmask());
        		router.setPublicMacAddress(nic.getMacAddress());
        	} else if (network.getTrafficType() == TrafficType.Guest) {
        		router.setGuestIpAddress(nic.getIp4Address());
        		router.setGuestMacAddress(nic.getMacAddress());
        	} else if (network.getTrafficType() == TrafficType.Control) {
        		router.setPrivateIpAddress(nic.getIp4Address());
        		router.setPrivateNetmask(nic.getNetmask());
        		router.setPrivateMacAddress(nic.getMacAddress());
        	}
        }
      //source NAT address is stored in /proc/cmdline of the domR and gets
		//reassigned upon powerup. Source NAT rule gets configured in StartRouter command
    	final List<IPAddressVO> ipAddrs = _networkMgr.listPublicIpAddressesInVirtualNetwork(router.getAccountId(), router.getDataCenterId(), null);
		final List<String> ipAddrList = new ArrayList<String>();
		for (final IPAddressVO ipVO : ipAddrs) {
			ipAddrList.add(ipVO.getAddress());
		}
		if (!ipAddrList.isEmpty()) {	  
			_networkMgr.getAssociateIPCommands(router, ipAddrList, true, 0, cmds);               
		}
        return true;
    }

    @Override
    public boolean finalizeStart(Commands cmds, VirtualMachineProfile<DomainRouterVO> profile, DeployDestination dest, ReservationContext context) {
        CheckSshAnswer answer = (CheckSshAnswer)cmds.getAnswer("checkSsh");
        if (!answer.getResult()) {
            s_logger.warn("Unable to ssh to the VM: " + answer.getDetails());
            return false;
        }
        
      
        return true;
    }
    
    @Override
    public void finalizeStop(VirtualMachineProfile<DomainRouterVO> profile, long hostId, String reservationId) {
    }

	@Override
	public RemoteAccessVpnVO startRemoteAccessVpn(RemoteAccessVpnVO vpnVO) throws ResourceUnavailableException {
		DomainRouterVO router = getRouter(vpnVO.getAccountId(), vpnVO.getZoneId());
		if (router == null) {
			s_logger.warn("Failed to start remote access VPN: no router found for account and zone");
			return null;
		}
		if (router.getState() != State.Running && router.getState() != State.Starting) {
			s_logger.warn("Failed to start remote access VPN: router not in running state");
			return null;
		}
		List<VpnUserVO> vpnUsers = _vpnUsersDao.listByAccount(vpnVO.getAccountId());
		VpnUsersCfgCommand addUsersCmd = new VpnUsersCfgCommand(router.getPrivateIpAddress(), vpnUsers, new ArrayList<VpnUserVO>());
		RemoteAccessVpnCfgCommand startVpnCmd = new RemoteAccessVpnCfgCommand(true, router.getPrivateIpAddress(), vpnVO.getVpnServerAddress(), vpnVO.getLocalIp(), vpnVO.getIpRange(), vpnVO.getIpsecPresharedKey());
		Commands cmds = new Commands(OnError.Stop);
		cmds.addCommand("users", addUsersCmd);
		cmds.addCommand("startVpn", startVpnCmd);
		try {
			_agentMgr.send(router.getHostId(), cmds);
		} catch (AgentUnavailableException e) {
			s_logger.debug("Failed to start remote access VPN: ", e);
			return null;
		} catch (OperationTimedoutException e) {
			s_logger.debug("Failed to start remote access VPN: ", e);
			return null;		
		}
		Answer answer = cmds.getAnswer("users");
		if (!answer.getResult()) {
            s_logger.error("Unable to start vpn: unable add users to vpn in zone " + vpnVO.getZoneId() + " for account "+ vpnVO.getAccountId() +" on domR: " + router.getInstanceName() + " due to " + answer.getDetails());
            throw new ResourceUnavailableException("Unable to start vpn: Unable to add users to vpn in zone " + vpnVO.getZoneId() + " for account "+ vpnVO.getAccountId() +" on domR: " + router.getInstanceName() + " due to " + answer.getDetails()); 
        }
		answer = cmds.getAnswer("startVpn");
		if (!answer.getResult()) {
            s_logger.error("Unable to start vpn in zone " + vpnVO.getZoneId() + " for account "+ vpnVO.getAccountId() +" on domR: " + router.getInstanceName() + " due to " + answer.getDetails());
            throw new ResourceUnavailableException("Unable to start vpn in zone " + vpnVO.getZoneId() + " for account "+ vpnVO.getAccountId() +" on domR: " + router.getInstanceName() + " due to " + answer.getDetails()); 
        }
		return vpnVO;
	}

	@Override
	public boolean deleteRemoteAccessVpn(RemoteAccessVpnVO vpnVO) {
		DomainRouterVO router = getRouter(vpnVO.getAccountId(), vpnVO.getZoneId());
		if (router == null) {
			s_logger.warn("Failed to delete remote access VPN: no router found for account and zone");
			return false;
		}
		if (router.getState() != State.Running) {
			s_logger.warn("Failed to delete remote access VPN: router not in running state");
			return false;
		}
		try {
			Answer answer = _agentMgr.send(router.getHostId(), new RemoteAccessVpnCfgCommand(false, router.getPrivateIpAddress(), vpnVO.getVpnServerAddress(), vpnVO.getLocalIp(), vpnVO.getIpRange(), vpnVO.getIpsecPresharedKey()));
			if (answer != null && answer.getResult()) {
				return true;
			} else {
				s_logger.debug("Failed to delete remote access VPN: " + answer.getDetails());
				return false;
			}
		} catch (AgentUnavailableException e) {
			s_logger.debug("Failed to delete remote access VPN: ", e);
			return false;
		} catch (OperationTimedoutException e) {
			s_logger.debug("Failed to delete remote access VPN: ", e);
			return false;		
		}
	}
	
	public DomainRouterVO start(long routerId, User user, Account caller) throws StorageUnavailableException, InsufficientCapacityException, ConcurrentOperationException, ResourceUnavailableException {
	    return start(_routerDao.findById(routerId), user, caller);
	}
	
	public DomainRouterVO start(DomainRouterVO router, User user, Account caller) throws StorageUnavailableException, InsufficientCapacityException, ConcurrentOperationException, ResourceUnavailableException {
	    return _itMgr.start(router, null, user, caller, null);
	}

    @Override
    public DomainRouterVO addVirtualMachineIntoNetwork(Network config, NicProfile nic, VirtualMachineProfile<UserVm> profile, DeployDestination dest, ReservationContext context, Boolean startDhcp) throws ConcurrentOperationException, InsufficientNetworkCapacityException, ResourceUnavailableException {
        DomainRouterVO router = _routerDao.findByNetworkConfiguration(config.getId());
        try {
            if (startDhcp) {
                router = this.deployDhcp(config, dest, profile.getOwner());
            } else {
                router = this.deployVirtualRouter(config, dest, profile.getOwner());
            }
            
        } catch (InsufficientNetworkCapacityException e) {
            throw e;
        } catch (InsufficientCapacityException e) {
            throw new ResourceUnavailableException("Unable to start router for " + config, e);
        }
        
        if (router == null) {
            s_logger.error("Can't find a domain router to start VM: " + profile);
            throw new ResourceUnavailableException("Can't find a domain router to start " + profile + " in " + config);
        }

        
        String password = profile.getVirtualMachine().getPassword();
        String userData = profile.getVirtualMachine().getUserData();
        Commands cmds = new Commands(OnError.Stop);
        String routerPublicIpAddress = nic.getIp4Address();
        String routerControlIpAddress = null;
        List<NicVO> nics = _nicDao.listBy(router.getId());
        for (NicVO n : nics) {
            NetworkVO nc = _networkConfigurationDao.findById(n.getNetworkId());
            if (n.getIp4Address() != null && nc.getTrafficType() == TrafficType.Public) {
                routerPublicIpAddress = nic.getIp4Address();
            } else if (nc.getTrafficType() == TrafficType.Control) {
                routerControlIpAddress = n.getIp4Address();
            }
        }
        
        cmds.addCommand("dhcp", new DhcpEntryCommand(nic.getMacAddress(), nic.getIp4Address(), routerControlIpAddress, profile.getVirtualMachine().getHostName()));
        if (password != null) {
            final String encodedPassword = rot13(password);
            cmds.addCommand("password", new SavePasswordCommand(encodedPassword, nic.getIp4Address(), routerControlIpAddress, profile.getVirtualMachine().getHostName()));
        }
        
        String serviceOffering = _serviceOfferingDao.findById(profile.getServiceOfferingId()).getDisplayText();
        String zoneName = _dcDao.findById(config.getDataCenterId()).getName();
        
        
        cmds.addCommand("vmdata", generateVmDataCommand(routerControlIpAddress, routerPublicIpAddress, nic.getIp4Address(), userData, serviceOffering, zoneName, nic.getIp4Address(), profile.getVirtualMachine().getHostName(), profile.getVirtualMachine().getHostName(), profile.getId()));
        
        try {
            _agentMgr.send(router.getHostId(), cmds);
        } catch (AgentUnavailableException e) {
            throw new ResourceUnavailableException("Unable to reach the agent ", e);
        } catch (OperationTimedoutException e) {
            throw new ResourceUnavailableException("Unable to reach the agent ", e);
        }
        
        Answer answer = cmds.getAnswer("dhcp");
        if (!answer.getResult()) {
            s_logger.error("Unable to set dhcp entry for " + profile +" on domR: " + router.getHostName() + " due to " + answer.getDetails());
            throw new ResourceUnavailableException("Unable to set dhcp entry for " + profile + " due to " + answer.getDetails()); 
        }
        
        answer = cmds.getAnswer("password");
        if (answer != null && !answer.getResult()) {
            s_logger.error("Unable to set password for " + profile + " due to " + answer.getDetails());
            throw new ResourceUnavailableException("Unable to set password due to " + answer.getDetails());
        }
        
        answer = cmds.getAnswer("vmdata");
        if (answer != null && !answer.getResult()) {
            s_logger.error("Unable to set VM data for " + profile + " due to " + answer.getDetails());
            throw new ResourceUnavailableException("Unable to set VM data due to " + answer.getDetails());
        }
        return router;
    }
    
    @Override
    public DomainRouterVO persist(DomainRouterVO router) {
        return _routerDao.persist(router);
    }

	@Override
	public boolean addRemoveVpnUsers(RemoteAccessVpnVO vpnVO, List<VpnUserVO> addUsers, List<VpnUserVO> removeUsers) {
		DomainRouterVO router = getRouter(vpnVO.getAccountId(), vpnVO.getZoneId());
		if (router == null) {
			s_logger.warn("Failed to add/remove VPN users: no router found for account and zone");
			return false;
		}
		if (router.getState() != State.Running) {
			s_logger.warn("Failed to add/remove VPN users: router not in running state");
			return false;
		}
		try {
			Answer answer = _agentMgr.send(router.getHostId(), new VpnUsersCfgCommand(router.getPrivateIpAddress(), addUsers, removeUsers));
			if (answer != null && answer.getResult()) {
				return true;
			} else {
				s_logger.debug("Failed to add/remove VPN users: " + answer.getDetails());
				return false;
			}
		} catch (AgentUnavailableException e) {
			s_logger.debug("Failed to add/remove VPN users:: ", e);
			return false;
		} catch (OperationTimedoutException e) {
			s_logger.debug("Failed to add/remove VPN users:: ", e);
			return false;		
		}
	}
    
    @Override
    public DomainRouterVO findById(long id) {
        return _routerDao.findById(id);
    }
    
    @Override
    public DomainRouterVO findByName(String name) {
        if (!VirtualMachineName.isValidRouterName(name)) {
            return null;
        }
        
        return _routerDao.findById(VirtualMachineName.getRouterId(name));
    }
    
    @Override
    public VirtualRouter startRouter(long routerId) throws ResourceUnavailableException, InsufficientCapacityException, ConcurrentOperationException {
        Account account = UserContext.current().getAccount();
        
        //verify parameters
        DomainRouterVO router = _routerDao.findById(routerId);
        if (router == null) {
            throw new PermissionDeniedException ("Unable to start router with id " + routerId + ". Permisssion denied");
        }
        _accountMgr.checkAccess(account, router);
        
        long eventId = EventUtils.saveScheduledEvent(User.UID_SYSTEM, Account.ACCOUNT_ID_SYSTEM, EventTypes.EVENT_ROUTER_START, "starting Router with Id: "+routerId);
        UserVO user = _userDao.findById(UserContext.current().getUserId());
        return this.start(router, user, account);
    }
    
    /**
     * Stops domain router
     * @param cmd the command specifying router's id
     * @return router if successful, null otherwise
     * @throws OperationTimedoutException 
     * @throws ConcurrentOperationException 
     * @throws ResourceUnavailableException 
     * @throws InvalidParameterValueException, PermissionDeniedException
     */
    @Override
    public VirtualRouter stopRouter(long routerId) throws ResourceUnavailableException, ConcurrentOperationException {
        UserContext context = UserContext.current();
        Account account = context.getAccount();
        long accountId = account.getId();
        long userId = context.getUserId();
        

        // verify parameters
        DomainRouterVO router = _routerDao.findById(routerId);
        if (router == null) {
            throw new PermissionDeniedException ("Unable to stop router with id " + routerId + ". Permission denied.");
        }

        _accountMgr.checkAccess(account, router);
        
        long eventId = EventUtils.saveScheduledEvent(userId, accountId, EventTypes.EVENT_ROUTER_STOP, "stopping Router with Id: "+routerId);
        
        UserVO user = _userDao.findById(context.getUserId());

        if (!_itMgr.stop(router, user, account)) {
            return null;
        }
        
        return router;
    }
    
    private void reconstructRouterPortForwardingRules(Commands cmds, List<? extends IpAddress> ipAddrs) {
        List<? extends PortForwardingRule> rules = _rulesMgr.gatherPortForwardingRulesForApplication(ipAddrs);
        if (rules.size() == 0) {
            s_logger.debug("There are not port forwarding rules to send. ");
            return;
        }
        SetPortForwardingRulesCommand pfrCmd = new SetPortForwardingRulesCommand(rules);
        cmds.addCommand(pfrCmd);
    }
    /*
    private List<? extends IpAddress> reconstructRouterIpAssocations(Commands cmds, VirtualRouter router) {
        List<IPAddressVO> ipAddrs = _networkMgr.listPublicIpAddressesInVirtualNetwork(router.getAccountId(), router.getDataCenterId(), null);
        
    }
    */
    
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
    /*
    
    private boolean reconstructRouterState(Network config, DomainRouterVO router, Commands cmds) {
        if (router.getRole() == Role.DHCP_FIREWALL_LB_PASSWD_USERDATA) {
            List<? extends IpAddress> ipAddrs = reconstructRouterIpAssocations(cmds, router);
            reconstructRouterPortForwardingRules(cmds, ipAddrs);
        }
        
        reconstructDhcpEntries(router);
        reconstructVpnServerData(router);
    }
            //source NAT address is stored in /proc/cmdline of the domR and gets
            //reassigned upon powerup. Source NAT rule gets configured in StartRouter command
            List<IPAddressVO> ipAddrs = _networkMgr.listPublicIpAddressesInVirtualNetwork(router.getAccountId(), router.getDataCenterId(), null);
            List<String> ipAddrList = new ArrayList<String>();
            for (final IPAddressVO ipVO : ipAddrs) {
                ipAddrList.add(ipVO.getAddress());
            }
            
            if (!ipAddrList.isEmpty()) {
                try {
                    final boolean success = _networkMgr.associateIP(router, ipAddrList, true, 0);
                    if (!success) {
                        return false;
                    }
                } catch (ConcurrentOperationException e) {
                    s_logger.warn("unable to associate ip due to ", e);
                    return false;
                }
            }
            
            
        return resendDhcpEntries(router) && resendVpnServerData(router);
      
    }
    */
    
    private boolean resendDhcpEntries(Network config, DomainRouterVO router, Commands cmd){
        final List<UserVmVO> vms = _vmDao.listBy(router.getId(), State.Creating, State.Starting, State.Running, State.Stopping, State.Stopped, State.Migrating);
        Commands cmds = new Commands(OnError.Continue);
        for (UserVmVO vm: vms) {
            if (vm.getGuestIpAddress() == null || vm.getGuestMacAddress() == null || vm.getHostName() == null) {
                continue;
            }
            DhcpEntryCommand decmd = new DhcpEntryCommand(vm.getGuestMacAddress(), vm.getGuestIpAddress(), router.getPrivateIpAddress(), vm.getHostName());
            cmds.addCommand(decmd);
        }
        if (cmds.size() > 0) {
            try {
                _agentMgr.send(router.getHostId(), cmds);
            } catch (final AgentUnavailableException e) {
                s_logger.warn("agent unavailable", e);
            } catch (final OperationTimedoutException e) {
                s_logger.warn("Timed Out", e);
            }
            Answer[] answers = cmds.getAnswers();
            if (answers == null ){
                return false;
            }
            int i=0;
            while (i < cmds.size()) {
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
}
