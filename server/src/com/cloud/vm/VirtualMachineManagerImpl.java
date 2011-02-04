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
package com.cloud.vm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.ejb.Local;
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;

import com.cloud.agent.AgentManager;
import com.cloud.agent.AgentManager.OnError;
import com.cloud.agent.Listener;
import com.cloud.agent.api.AgentControlAnswer;
import com.cloud.agent.api.AgentControlCommand;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.CheckVirtualMachineAnswer;
import com.cloud.agent.api.CheckVirtualMachineCommand;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.MigrateAnswer;
import com.cloud.agent.api.MigrateCommand;
import com.cloud.agent.api.PingRoutingCommand;
import com.cloud.agent.api.PrepareForMigrationAnswer;
import com.cloud.agent.api.PrepareForMigrationCommand;
import com.cloud.agent.api.RebootAnswer;
import com.cloud.agent.api.RebootCommand;
import com.cloud.agent.api.StartAnswer;
import com.cloud.agent.api.StartCommand;
import com.cloud.agent.api.StartupCommand;
import com.cloud.agent.api.StartupRoutingCommand;
import com.cloud.agent.api.StopAnswer;
import com.cloud.agent.api.StopCommand;
import com.cloud.agent.api.to.VirtualMachineTO;
import com.cloud.agent.manager.Commands;
import com.cloud.alert.AlertManager;
import com.cloud.capacity.CapacityManager;
import com.cloud.cluster.ClusterManager;
import com.cloud.configuration.Config;
import com.cloud.configuration.ConfigurationManager;
import com.cloud.configuration.dao.ConfigurationDao;
import com.cloud.consoleproxy.ConsoleProxyManager;
import com.cloud.dc.DataCenter;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.HostPodVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.HostPodDao;
import com.cloud.deploy.DataCenterDeployment;
import com.cloud.deploy.DeployDestination;
import com.cloud.deploy.DeploymentPlan;
import com.cloud.deploy.DeploymentPlanner;
import com.cloud.deploy.DeploymentPlanner.ExcludeList;
import com.cloud.domain.dao.DomainDao;
import com.cloud.event.EventTypes;
import com.cloud.event.UsageEventVO;
import com.cloud.event.dao.UsageEventDao;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.ConnectionException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InsufficientServerCapacityException;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.ha.HighAvailabilityManager;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.Status;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.hypervisor.HypervisorGuru;
import com.cloud.maid.StackMaid;
import com.cloud.network.NetworkManager;
import com.cloud.network.NetworkVO;
import com.cloud.org.Cluster;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.storage.DiskOfferingVO;
import com.cloud.storage.Storage.ImageFormat;
import com.cloud.storage.StorageManager;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.Volume;
import com.cloud.storage.Volume.VolumeType;
import com.cloud.storage.dao.GuestOSCategoryDao;
import com.cloud.storage.dao.GuestOSDao;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.User;
import com.cloud.user.dao.AccountDao;
import com.cloud.user.dao.UserDao;
import com.cloud.utils.Journal;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.Pair;
import com.cloud.utils.Ternary;
import com.cloud.utils.component.Adapters;
import com.cloud.utils.component.ComponentLocator;
import com.cloud.utils.component.Inject;
import com.cloud.utils.concurrency.NamedThreadFactory;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.GlobalLock;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.fsm.StateListener;
import com.cloud.utils.fsm.StateMachine2;
import com.cloud.vm.ItWorkVO.Step;
import com.cloud.vm.VirtualMachine.Event;
import com.cloud.vm.VirtualMachine.State;
import com.cloud.vm.dao.ConsoleProxyDao;
import com.cloud.vm.dao.DomainRouterDao;
import com.cloud.vm.dao.NicDao;
import com.cloud.vm.dao.SecondaryStorageVmDao;
import com.cloud.vm.dao.UserVmDao;
import com.cloud.vm.dao.VMInstanceDao;

@Local(value=VirtualMachineManager.class)
public class VirtualMachineManagerImpl implements VirtualMachineManager, StateListener<State, VirtualMachine.Event, VirtualMachine>, Listener {
    private static final Logger s_logger = Logger.getLogger(VirtualMachineManagerImpl.class);
    
    String _name;
    @Inject protected StorageManager _storageMgr;
    @Inject protected NetworkManager _networkMgr;
    @Inject protected AgentManager _agentMgr;
    @Inject protected VMInstanceDao _vmDao;
    @Inject protected ServiceOfferingDao _offeringDao;
    @Inject protected VMTemplateDao _templateDao;
    @Inject protected UserDao _userDao;
    @Inject protected AccountDao _accountDao;
    @Inject protected DomainDao _domainDao;
    @Inject protected ClusterManager _clusterMgr;
    @Inject protected ItWorkDao _workDao;
    @Inject protected UserVmDao _userVmDao;
    @Inject protected DomainRouterDao _routerDao;
    @Inject protected ConsoleProxyDao _consoleDao;
    @Inject protected SecondaryStorageVmDao _secondaryDao;
    @Inject protected UsageEventDao _usageEventDao;
    @Inject protected NicDao _nicsDao;
    @Inject protected AccountManager _accountMgr;
    @Inject protected HostDao _hostDao;
    @Inject protected AlertManager _alertMgr;
    @Inject protected GuestOSCategoryDao _guestOsCategoryDao;
    @Inject protected GuestOSDao _guestOsDao;
    @Inject protected VolumeDao _volsDao;
    @Inject protected ConsoleProxyManager _consoleProxyMgr;
    @Inject protected ConfigurationManager _configMgr;
    @Inject protected CapacityManager _capacityMgr;
    @Inject protected HighAvailabilityManager _haMgr;
    @Inject protected HostPodDao _podDao;
    @Inject protected DataCenterDao _dcDao;
    
    @Inject(adapter=DeploymentPlanner.class)
    protected Adapters<DeploymentPlanner> _planners;
    
    Map<VirtualMachine.Type, VirtualMachineGuru<? extends VMInstanceVO>> _vmGurus = new HashMap<VirtualMachine.Type, VirtualMachineGuru<? extends VMInstanceVO>>();
    Map<HypervisorType, HypervisorGuru> _hvGurus = new HashMap<HypervisorType, HypervisorGuru>();
    protected StateMachine2<State, VirtualMachine.Event, VirtualMachine> _stateMachine;
    
    ScheduledExecutorService _executor = null;
    protected int _operationTimeout;
    
    protected int _retry;
    protected long _nodeId;
    protected long _cleanupWait;
    protected long _cleanupInterval;
    protected long _cancelWait;
    protected long _opWaitInterval;
    protected int _lockStateRetry;

    @Override
    public <T extends VMInstanceVO> void registerGuru(VirtualMachine.Type type, VirtualMachineGuru<T> guru) {
        synchronized(_vmGurus) { 
            _vmGurus.put(type, guru);
        }
    }
    
    @Override @DB
    public <T extends VMInstanceVO> T allocate(T vm,
            VMTemplateVO template,
            ServiceOfferingVO serviceOffering,
            Pair<? extends DiskOfferingVO, Long> rootDiskOffering,
            List<Pair<DiskOfferingVO, Long>> dataDiskOfferings,
            List<Pair<NetworkVO, NicProfile>> networks,
            Map<VirtualMachineProfile.Param, Object> params,
            DeploymentPlan plan,
            HypervisorType hyperType,
            Account owner) throws InsufficientCapacityException {
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Allocating entries for VM: " + vm);
        }
        
        VirtualMachineProfileImpl<T> vmProfile = new VirtualMachineProfileImpl<T>(vm, template, serviceOffering, owner, params);
        
        vm.setDataCenterId(plan.getDataCenterId());
        if (plan.getPodId() != null) {
            vm.setPodId(plan.getPodId());
        }
        assert (plan.getClusterId() == null && plan.getPoolId() == null) : "We currently don't support cluster and pool preset yet";
        
        @SuppressWarnings("unchecked")
        VirtualMachineGuru<T> guru = (VirtualMachineGuru<T>)_vmGurus.get(vm.getType());
        
        Transaction txn = Transaction.currentTxn();
        txn.start();
        vm = guru.persist(vm);

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Allocating nics for " + vm);
        }
        try {
            _networkMgr.allocate(vmProfile, networks);
        } catch (ConcurrentOperationException e) {
            throw new CloudRuntimeException("Concurrent operation while trying to allocate resources for the VM", e);
        }

        if (dataDiskOfferings == null) {
            dataDiskOfferings = new ArrayList<Pair<DiskOfferingVO, Long>>(0);
        }
        
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Allocaing disks for " + vm);
        }
        
        if (template.getFormat() == ImageFormat.ISO) {
            _storageMgr.allocateRawVolume(VolumeType.ROOT, "ROOT-" + vm.getId(), rootDiskOffering.first(), rootDiskOffering.second(), vm, owner);
        } else {
            _storageMgr.allocateTemplatedVolume(VolumeType.ROOT, "ROOT-" + vm.getId(), rootDiskOffering.first(), template, vm, owner);
        }
        for (Pair<DiskOfferingVO, Long> offering : dataDiskOfferings) {
            _storageMgr.allocateRawVolume(VolumeType.DATADISK, "DATA-" + vm.getId(), offering.first(), offering.second(), vm, owner);
        }

        txn.commit();
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Allocation completed for VM: " + vm);
        }
        
        return vm;
    }
    
    
    
    protected void reserveNics(VirtualMachineProfile<? extends VMInstanceVO> vmProfile, DeployDestination dest, ReservationContext context) throws InsufficientCapacityException, ConcurrentOperationException, ResourceUnavailableException {
//        List<NicVO> nics = _nicsDao.listBy(vmProfile.getId());
//        for (NicVO nic : nics) {
//            Pair<NetworkGuru, NetworkVO> implemented = _networkMgr.implementNetwork(nic.getNetworkId(), dest, context);
//            NetworkGuru concierge = implemented.first();
//            NetworkVO network = implemented.second();
//            NicProfile profile = null;
//            if (nic.getReservationStrategy() == ReservationStrategy.Start) {
//                nic.setState(Resource.State.Reserving);
//                nic.setReservationId(context.getReservationId());
//                _nicsDao.update(nic.getId(), nic);
//                URI broadcastUri = nic.getBroadcastUri();
//                if (broadcastUri == null) {
//                    network.getBroadcastUri();
//                }
//
//                URI isolationUri = nic.getIsolationUri();
//
//                profile = new NicProfile(nic, network, broadcastUri, isolationUri);
//                concierge.reserve(profile, network, vmProfile, dest, context);
//                nic.setIp4Address(profile.getIp4Address());
//                nic.setIp6Address(profile.getIp6Address());
//                nic.setMacAddress(profile.getMacAddress());
//                nic.setIsolationUri(profile.getIsolationUri());
//                nic.setBroadcastUri(profile.getBroadCastUri());
//                nic.setReserver(concierge.getName());
//                nic.setState(Resource.State.Reserved);
//                nic.setNetmask(profile.getNetmask());
//                nic.setGateway(profile.getGateway());
//                nic.setAddressFormat(profile.getFormat());
//                _nicsDao.update(nic.getId(), nic);      
//            } else {
//                profile = new NicProfile(nic, network, nic.getBroadcastUri(), nic.getIsolationUri());
//            }
//            
//            for (NetworkElement element : _networkElements) {
//                if (s_logger.isDebugEnabled()) {
//                    s_logger.debug("Asking " + element.getName() + " to prepare for " + nic);
//                }
//                element.prepare(network, profile, vmProfile, dest, context);
//            }
//
//            vmProfile.addNic(profile);
//            _networksDao.changeActiveNicsBy(network.getId(), 1);
//        }
    }
    
    protected void prepareNics(VirtualMachineProfile<? extends VMInstanceVO> vmProfile, DeployDestination dest, ReservationContext context) {
        
    }
    
    
    @Override
    public <T extends VMInstanceVO> T allocate(T vm,
            VMTemplateVO template,
            ServiceOfferingVO serviceOffering,
            Long rootSize,
            Pair<DiskOfferingVO, Long> dataDiskOffering,
            List<Pair<NetworkVO, NicProfile>> networks,
            DeploymentPlan plan,
            HypervisorType hyperType,
            Account owner) throws InsufficientCapacityException {
        List<Pair<DiskOfferingVO, Long>> diskOfferings = new ArrayList<Pair<DiskOfferingVO, Long>>(1);
        if (dataDiskOffering != null) {
            diskOfferings.add(dataDiskOffering);
        }
        return allocate(vm, template, serviceOffering, new Pair<DiskOfferingVO, Long>(serviceOffering, rootSize), diskOfferings, networks, null, plan, hyperType, owner);
    }
    
    @Override
    public <T extends VMInstanceVO> T allocate(T vm,
            VMTemplateVO template,
            ServiceOfferingVO serviceOffering,
            List<Pair<NetworkVO, NicProfile>> networks,
            DeploymentPlan plan, 
            HypervisorType hyperType,
            Account owner) throws InsufficientCapacityException {
        return allocate(vm, template, serviceOffering, new Pair<DiskOfferingVO, Long>(serviceOffering, null), null, networks, null, plan, hyperType, owner);
    }
    
    @SuppressWarnings("unchecked")
    private <T extends VMInstanceVO> VirtualMachineGuru<T> getVmGuru(T vm) {
        return (VirtualMachineGuru<T>)_vmGurus.get(vm.getType());
    }
    
    @Override
    public <T extends VMInstanceVO> boolean expunge(T vm, User caller, Account account) throws ResourceUnavailableException {
        try {
            if (advanceExpunge(vm, caller, account)) {
                //Mark vms as removed
                remove(vm, _accountMgr.getSystemUser(), account);
                return true;
            } else {
                s_logger.info("Did not expunge " + vm);
                return false;
            }
        } catch (OperationTimedoutException e) {
            throw new CloudRuntimeException("Operation timed out", e);
        } catch (ConcurrentOperationException e) {
            throw new CloudRuntimeException("Concurrent operation ", e);
        }
    }
    
    @Override
    public <T extends VMInstanceVO> boolean advanceExpunge(T vm, User caller, Account account) throws ResourceUnavailableException, OperationTimedoutException, ConcurrentOperationException {
        if (vm == null || vm.getRemoved() != null) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Unable to find vm or vm is destroyed: " + vm);
            }
            return true;
        }
        
        if (!this.advanceStop(vm, false, caller, account)) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Unable to stop the VM so we can't expunge it.");
            }
        }

        if (!stateTransitTo(vm, VirtualMachine.Event.ExpungeOperation, vm.getHostId())) {
            s_logger.debug("Unable to destroy the vm because it is not in the correct state: " + vm.toString());
            return false;
        }
        
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Destroying vm " + vm);
        }
        
        VirtualMachineProfile<T> profile = new VirtualMachineProfileImpl<T>(vm);

        _networkMgr.cleanupNics(profile);
    	//Clean up volumes based on the vm's instance id
    	_storageMgr.cleanupVolumes(vm.getId());
    	
        VirtualMachineGuru<T> guru = getVmGuru(vm);
        guru.finalizeExpunge(vm);
    	
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Expunged " + vm);
        }

        UsageEventVO usageEvent = new UsageEventVO(EventTypes.EVENT_VM_DESTROY, vm.getAccountId(), vm.getDataCenterId(), vm.getId(), vm.getName(), vm.getServiceOfferingId(), vm.getTemplateId(), null);
        _usageEventDao.persist(usageEvent);

        
        return true;
    }

    @Override
    public boolean start() {
        _executor.scheduleAtFixedRate(new CleanupTask(), _cleanupInterval, _cleanupInterval, TimeUnit.SECONDS);
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }
    
    @Override
    public boolean configure(String name, Map<String, Object> xmlParams) throws ConfigurationException {
        _name = name;
        
        ComponentLocator locator = ComponentLocator.getCurrentLocator();
        ConfigurationDao configDao = locator.getDao(ConfigurationDao.class);
        Map<String, String> params = configDao.getConfiguration(xmlParams);
        
        _retry = NumbersUtil.parseInt(params.get(Config.StartRetry.key()), 10);
        
        ReservationContextImpl.setComponents(_userDao, _domainDao, _accountDao);
        VirtualMachineProfileImpl.setComponents(_offeringDao, _templateDao, _accountDao);
        
        Adapters<HypervisorGuru> hvGurus = locator.getAdapters(HypervisorGuru.class);
        for (HypervisorGuru guru : hvGurus) {
            _hvGurus.put(guru.getHypervisorType(), guru);
        }
        
        _cancelWait = NumbersUtil.parseLong(params.get(Config.VmOpCancelInterval.key()), 3600);
        _cleanupWait = NumbersUtil.parseLong(params.get(Config.VmOpCleanupWait.key()), 3600);
        _cleanupInterval = NumbersUtil.parseLong(params.get(Config.VmOpCleanupInterval.key()), 86400) * 1000;
        _opWaitInterval = NumbersUtil.parseLong(params.get(Config.VmOpWaitInterval.key()), 120) * 1000;
        _lockStateRetry = NumbersUtil.parseInt(params.get(Config.VmOpLockStateRetry.key()), 5);
        _operationTimeout = NumbersUtil.parseInt(params.get(Config.Wait.key()), 1800) * 2;
        
        _executor = Executors.newScheduledThreadPool(1, new NamedThreadFactory("Vm-Operations-Cleanup"));
        _nodeId = _clusterMgr.getId();
      
        _stateMachine.registerListener(this);
        _agentMgr.registerForHostEvents(this, true, true, true);
        return true;
    }
    
    @Override
    public String getName() {
        return _name;
    }
    
    protected VirtualMachineManagerImpl() {
        setStateMachine();
    }
    
    @Override
    public <T extends VMInstanceVO> T start(T vm, Map<VirtualMachineProfile.Param, Object> params, User caller, Account account) throws InsufficientCapacityException, ResourceUnavailableException {
        try {
            return advanceStart(vm, params, caller, account);
        } catch (ConcurrentOperationException e) {
            throw new CloudRuntimeException("Unable to start a VM due to concurrent operation", e);
        }
    }

    protected boolean checkWorkItems(VMInstanceVO vm, State state) throws ConcurrentOperationException {
        while (true) {
            ItWorkVO vo = _workDao.findByOutstandingWork(vm.getId(), state);
            if (vo == null) {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Unable to find work for " + vm);
                }
                return true;
            }
            
            if (vo.getStep() == Step.Done) {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Work for " + vm + " is " + vo.getStep());
                }
                return true;
            }
            
            if (vo.getSecondsTaskIsInactive() > _cancelWait) {
                s_logger.warn("The task item for vm " + vm + " has been inactive for " + vo.getSecondsTaskIsInactive());
                return false;
            }
            
            try {
                Thread.sleep(_opWaitInterval);
            } catch (InterruptedException e) {
                s_logger.info("Waiting for " + vm + " but is interrupted");
                throw new ConcurrentOperationException("Waiting for " + vm + " but is interrupted");
            }
            s_logger.debug("Waiting some more to make sure there's no activity on " + vm);
        }
        
        
    }
    
    @DB
    protected <T extends VMInstanceVO> Ternary<T, ReservationContext, ItWorkVO> changeToStartState(VirtualMachineGuru<T> vmGuru, T vm, User caller, Account account) throws ConcurrentOperationException {
        long vmId = vm.getId();
        
        ItWorkVO work = new ItWorkVO(UUID.randomUUID().toString(), _nodeId, State.Starting, vm.getId());
        int retry = _lockStateRetry;
        while (retry-- != 0) {
            Transaction txn = Transaction.currentTxn();
            txn.start();
            try {
                if (stateTransitTo(vm, Event.StartRequested, null, work.getId())) {
                    
                    Journal journal = new Journal.LogJournal("Creating " + vm, s_logger);
                    work = _workDao.persist(work);
                    ReservationContextImpl context = new ReservationContextImpl(work.getId(), journal, caller, account);
                    
                    if (s_logger.isDebugEnabled()) {
                        s_logger.debug("Successfully transitioned to start state for " + vm + " reservation id = " + work.getId());
                    }
                    return new Ternary<T, ReservationContext, ItWorkVO>(vmGuru.findById(vmId), context, work);
                }
                
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Determining why we're unable to update the state to Starting for " + vm);
                } 
                
                VMInstanceVO instance = _vmDao.lockRow(vmId, true);
                if (instance == null) {
                    throw new ConcurrentOperationException("Unable to acquire lock on " + vm);
                }
                
                State state = instance.getState();
                if (state == State.Running) {
                    if (s_logger.isDebugEnabled()) {
                        s_logger.debug("VM is already started: " + vm);
                    }
                    return null;
                }
                
                if (state.isTransitional()) {
                    if (!checkWorkItems(vm, state)) {
                        throw new ConcurrentOperationException("There are concurrent operations on the VM " + vm);
                    } else {
                        continue;
                    }
                }
                
                if (state != State.Stopped) {
                    s_logger.debug("VM " + vm + " is not in a state to be started: " + state);
                    return null;
                }
            } finally {
                txn.commit();
            }
        }
        
        throw new ConcurrentOperationException("Unable to change the state of " + vm);
    }
    
    @DB
    protected <T extends VMInstanceVO> boolean changeState(T vm, Event event, Long hostId, ItWorkVO work, Step step) {
        Transaction txn = Transaction.currentTxn();
        txn.start();
        if (!stateTransitTo(vm, event, hostId)) {
            return false;
        }
        _workDao.updateStep(work, step);
        txn.commit();
        return true;
    }
    
    @Override
    public <T extends VMInstanceVO> T advanceStart(T vm, Map<VirtualMachineProfile.Param, Object> params, User caller, Account account) throws InsufficientCapacityException, ConcurrentOperationException, ResourceUnavailableException {
        long vmId = vm.getId();
        
        VirtualMachineGuru<T> vmGuru = getVmGuru(vm);
        vm = vmGuru.findById(vm.getId());        
        Ternary<T, ReservationContext, ItWorkVO> start = changeToStartState(vmGuru, vm, caller, account);
        if (start == null) {
            return vmGuru.findById(vmId);
        }
        
        vm = start.first();
        ReservationContext ctx = start.second();
        ItWorkVO work = start.third();
        
        T startedVm = null;
        ServiceOfferingVO offering = _offeringDao.findById(vm.getServiceOfferingId());
        VMTemplateVO template = _templateDao.findById(vm.getTemplateId());
        DataCenterDeployment plan = new DataCenterDeployment(vm.getDataCenterId(), vm.getPodId(), null, null);
        HypervisorGuru hvGuru = _hvGurus.get(vm.getHypervisorType());
        
        
        try {
            Journal journal = start.second().getJournal();
            
            ExcludeList avoids = new ExcludeList();
            int retry = _retry;
            while (retry-- != 0) { // It's != so that it can match -1.
                
                VirtualMachineProfileImpl<T> vmProfile = new VirtualMachineProfileImpl<T>(vm, template, offering, null, params);
                DeployDestination dest = null;
                for (DeploymentPlanner planner : _planners) {
                    dest = planner.plan(vmProfile, plan, avoids);
                    if (dest != null) {
                        avoids.addHost(dest.getHost().getId());
                        journal.record("Deployment found ", vmProfile, dest);
                        break;
                    }
                }
                
                if (dest == null) {
                    throw new InsufficientServerCapacityException("Unable to create a deployment for " + vmProfile, DataCenter.class, plan.getDataCenterId());
                }
                
                long destHostId = dest.getHost().getId();
                
                if (!changeState(vm, Event.OperationRetry, destHostId, work, Step.Prepare)) {
                    throw new ConcurrentOperationException("Unable to update the state of the Virtual Machine");
                }
                
                try {
                    
                    _storageMgr.prepare(vmProfile, dest);
                    _networkMgr.prepare(vmProfile, dest, ctx);
                    
                    vmGuru.finalizeVirtualMachineProfile(vmProfile, dest, ctx);
                    
                    VirtualMachineTO vmTO = hvGuru.implement(vmProfile);
                    
                    Commands cmds = new Commands(OnError.Revert);
                    cmds.addCommand(new StartCommand(vmTO));
                    
                    vmGuru.finalizeDeployment(cmds, vmProfile, dest, ctx);
                    vm.setPodId(dest.getPod().getId());
                    work = _workDao.findById(work.getId());
                    if (work == null || work.getStep() != Step.Prepare) {
                        throw new ConcurrentOperationException("Work steps have been changed: " + work);
                    }
                    _workDao.updateStep(work, Step.Start);
                
                    _agentMgr.send(destHostId, cmds);
                    _workDao.updateStep(work, Step.Started);
                    Answer startAnswer = cmds.getAnswer(StartAnswer.class);
                    if (startAnswer != null && startAnswer.getResult()) {
                        if (vmGuru.finalizeStart(vmProfile, destHostId, cmds, ctx)) {
                            if (!changeState(vm, Event.OperationSucceeded, destHostId, work, Step.Done)) {
                                throw new ConcurrentOperationException("Unable to transition to a new state.");
                            }
                            startedVm = vm;
                            if (s_logger.isDebugEnabled()) {
                                s_logger.debug("Creation complete for VM " + vm);
                            }
                            return startedVm;
                        }
                    }
                    s_logger.info("Unable to start VM on " + dest.getHost() + " due to " + (startAnswer == null ? " no start answer" : startAnswer.getDetails()));
                } catch (OperationTimedoutException e) {
                    s_logger.debug("Unable to send the start command to host " + dest.getHost());
                    if (e.isActive()) {
                        //TODO: This one is different as we're not sure if the VM is actually started. 
                    }
                    avoids.addHost(destHostId);
                    continue;
                } catch (ResourceUnavailableException e) {
                    if (!avoids.add(e)) {
                        if (e.getScope() == Volume.class || e.getScope() == Nic.class) {
                            throw e;
                        } else {
                            throw new CloudRuntimeException("Resource is not available to start the VM.", e);
                        }
                    }
                    s_logger.info("Unable to contact resource.", e);
                    continue;
                } catch (InsufficientCapacityException e) {
                    if (!avoids.add(e)) {
                        if (e.getScope() == Volume.class || e.getScope() == Nic.class) {
                            throw e;
                        } else {
                            throw new CloudRuntimeException("Insufficient capacity to start the VM.", e);
                        }
                    }
                    s_logger.info("Insufficient capacity ", e);
                    continue;
                } catch (RuntimeException e) {
                    s_logger.warn("Failed to start instance " + vm, e);
                    throw new CloudRuntimeException("Failed to start " + vm, e);
                } finally {
                    if (startedVm == null) {
                        _workDao.updateStep(work, Step.Release);
                        cleanup(vmGuru, vmProfile, work, false, caller, account);
                    }
                }
            } 
        } finally {
            if (startedVm == null) {
                changeState(vm, Event.OperationFailed, null, work, Step.Done);
            }
        }
        
        return startedVm;
    }
    
    @Override
    public <T extends VMInstanceVO> boolean stop(T vm, User user, Account account) throws ResourceUnavailableException {
        try {
            return advanceStop(vm, false, user, account);
        } catch (OperationTimedoutException e) {
            throw new AgentUnavailableException("Unable to stop vm because the operation to stop timed out", vm.getHostId(), e);
        } catch (ConcurrentOperationException e) {
            throw new CloudRuntimeException("Unable to stop vm because of a concurrent operation", e);
        }
    }
    
    protected <T extends VMInstanceVO> boolean sendStop(VirtualMachineGuru<T> guru, VirtualMachineProfile<T> profile, boolean force) {
        VMInstanceVO vm = profile.getVirtualMachine();
        StopCommand stop = new StopCommand(vm, vm.getInstanceName(), null);
        try {
            StopAnswer answer = (StopAnswer)_agentMgr.send(vm.getHostId(), stop);
            if (!answer.getResult()) {
                s_logger.debug("Unable to stop VM due to " + answer.getDetails());
                return false;
            }
            
            guru.finalizeStop(profile, answer);
        } catch (AgentUnavailableException e) {
            if (!force) {
                return false;
            }
        } catch (OperationTimedoutException e) {
            if (!force) {
                return false;
            }
        }
        
        return true;
    }
    
    protected <T extends VMInstanceVO> boolean cleanup(VirtualMachineGuru<T> guru, VirtualMachineProfile<T> profile, ItWorkVO work, boolean force, User user, Account account) {
        T vm = profile.getVirtualMachine();
        State state = vm.getState();
        s_logger.debug("Cleaning up resources for the vm " + vm + " in " + state + " state");
        if (state == State.Starting) {
            Step step = work.getStep();
            if (step == Step.Start && !force) {
                s_logger.warn("Unable to cleanup vm " + vm + "; work state is incorrect: " + step);
                return false;
            }
            
            if (step == Step.Started || step == Step.Start) {
                if (vm.getHostId() != null) {
                    if (!sendStop(guru, profile, force)) {
                        s_logger.warn("Failed to stop vm " + vm + " in " + State.Starting + " state as a part of cleanup process");
                        return false;
                    }
                }
            }
            
            if (step != Step.Release && step != Step.Prepare && step != Step.Started && step != Step.Start) {
                s_logger.debug("Cleanup is not needed for vm " + vm + "; work state is incorrect: " + step);
                return true;
            }
        } else if (state == State.Stopping) {
            if (vm.getHostId() != null) {
                if (!sendStop(guru, profile, force)) {
                    s_logger.warn("Failed to stop vm " + vm + " in " + State.Stopping + " state as a part of cleanup process");
                    return false;
                }
            }
        } else if (state == State.Migrating) {
            if (vm.getHostId() != null) {
                if (!sendStop(guru, profile, force)) {
                    s_logger.warn("Failed to stop vm " + vm + " in " + State.Migrating + " state as a part of cleanup process");
                    return false;
                }
            } 
            if (vm.getLastHostId() != null) {
                if (!sendStop(guru, profile, force)) {
                    s_logger.warn("Failed to stop vm " + vm + " in " + State.Migrating + " state as a part of cleanup process");
                    return false;
                }
            }
        } else if (state == State.Running) {
            if (!sendStop(guru, profile, force)) {
                s_logger.warn("Failed to stop vm " + vm + " in " + State.Running + " state as a part of cleanup process");
                return false;
            }
        }
        
        _networkMgr.release(profile, force);
        _storageMgr.release(profile);
        s_logger.debug("Successfully cleanued up resources for the vm " + vm + " in " + state + " state");
        return true;
    }

    @Override
    public <T extends VMInstanceVO> boolean advanceStop(T vm, boolean forced, User user, Account account) throws AgentUnavailableException, OperationTimedoutException, ConcurrentOperationException {
        long vmId = vm.getId();
        State state = vm.getState();
        if (state == State.Stopped) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("VM is already stopped: " + vm);
            }
            return true;
        }
        
        if (state == State.Destroyed || state == State.Expunging || state == State.Error) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Stopped called on " + vm + " but the state is " + state);
            }
            return true;
        }
        
        VirtualMachineGuru<T> vmGuru = getVmGuru(vm);
        
        if (!stateTransitTo(vm, Event.StopRequested, vm.getHostId())) {
            if (!forced) {
                throw new ConcurrentOperationException("VM is being operated on by someone else.");
            }
            
            vm = vmGuru.findById(vmId);
            if (vm == null) {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Unable to find VM " + vmId);
                }
                return true;
            }
        }
        
        if ((vm.getState() == State.Starting || vm.getState() == State.Stopping || vm.getState() == State.Migrating) && forced) {
            ItWorkVO work = _workDao.findByOutstandingWork(vm.getId(), vm.getState());
            if (work != null) {
                if (cleanup(vmGuru, new VirtualMachineProfileImpl<T>(vm), work, forced, user, account)) {
                    return stateTransitTo(vm, Event.AgentReportStopped, null);
                }
            }
        }
        
        VirtualMachineProfile<T> profile = new VirtualMachineProfileImpl<T>(vm);
        if (vm.getHostId() != null) {
            String routerPrivateIp = null;
            if(vm.getType() == VirtualMachine.Type.DomainRouter){
                routerPrivateIp = vm.getPrivateIpAddress();
            }
            StopCommand stop = new StopCommand(vm, vm.getInstanceName(), null, routerPrivateIp);
            boolean stopped = false;
            StopAnswer answer = null;
            try {
                answer = (StopAnswer)_agentMgr.send(vm.getHostId(), stop);
                stopped = answer.getResult();
                if (!stopped) {
                    throw new CloudRuntimeException("Unable to stop the virtual machine due to " + answer.getDetails());
                }
                vmGuru.finalizeStop(profile, answer);
                    
            } catch (AgentUnavailableException e) {
            } catch (OperationTimedoutException e) {
            } finally {
                if (!stopped) {
                    if (!forced) {
                        stateTransitTo(vm, Event.OperationFailed, vm.getHostId());
                    } else {
                        s_logger.warn("Unable to actually stop " + vm + " but continue with release because it's a force stop");
                    }
                }
            }
        }
        
        if (s_logger.isDebugEnabled()) {
            s_logger.debug(vm + " is stopped on the host.  Proceeding to release resource held.");
        }
        
        try {
            _networkMgr.release(profile, forced);
            s_logger.debug("Successfully released network resources for the vm " + vm);
        } catch (Exception e) {
            s_logger.warn("Unable to release some network resources.", e);
        }
        
        try {
            _storageMgr.release(profile);
            s_logger.debug("Successfully released storage resources for the vm " + vm);
        } catch (Exception e) {
            s_logger.warn("Unable to release storage resources.", e);
        }
         
        vm.setReservationId(null);
        
        return stateTransitTo(vm, Event.OperationSucceeded, null);
    }
    
    private void setStateMachine() {
    	_stateMachine = VirtualMachine.State.getStateMachine();
    }
    
    protected boolean stateTransitTo(VMInstanceVO vm, VirtualMachine.Event e, Long hostId, String reservationId) {
        vm.setReservationId(reservationId);
        return _stateMachine.transitTo(vm, e, hostId, _vmDao);
    }
    
    @Override
    public boolean stateTransitTo(VMInstanceVO vm, VirtualMachine.Event e, Long hostId) {
        return _stateMachine.transitTo(vm, e, hostId, _vmDao);
    }
    
    @Override
    public <T extends VMInstanceVO> boolean remove(T vm, User user, Account caller) {
        //expunge the corresponding nics
        VirtualMachineProfile<T> profile = new VirtualMachineProfileImpl<T>(vm);
        _networkMgr.expungeNics(profile);
        s_logger.trace("Nics of the vm " + vm + " are expunged successfully");
        return _vmDao.remove(vm.getId());
    }
    
    @Override
    public <T extends VMInstanceVO> boolean destroy(T vm, User user, Account caller) throws AgentUnavailableException, OperationTimedoutException, ConcurrentOperationException {
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Destroying vm " + vm.toString());
        }
        if (vm == null || vm.getState() == State.Destroyed || vm.getState() == State.Expunging || vm.getRemoved() != null) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Unable to find vm or vm is destroyed: " + vm);
            }
            return true;
        }
        
        if (!advanceStop(vm, false, user, caller)) {
            s_logger.debug("Unable to stop " + vm);
            return false;
        }
        
        if (!stateTransitTo(vm, VirtualMachine.Event.DestroyRequested, vm.getHostId())) {
            s_logger.debug("Unable to destroy the vm because it is not in the correct state: " + vm.toString());
            return false;
        }

        return true;
    }
    
    
    @Override
    public <T extends VMInstanceVO> T migrate(T vm, long srcHostId, DeployDestination dest) throws ResourceUnavailableException {
        s_logger.info("Migrating " + vm + " to " + dest);
        
        long dstHostId = dest.getHost().getId();
        Host fromHost = _hostDao.findById(srcHostId);
        if (fromHost == null) {
            s_logger.info("Unable to find the host to migrate from: " + srcHostId);
            return null;
        } 
        VirtualMachineGuru<T> vmGuru = getVmGuru(vm);
        
        vm = vmGuru.findById(vm.getId());
        if (vm == null || vm.getRemoved() != null) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Unable to find the vm " + vm);
            }
            return null;
        }
        
        short alertType = AlertManager.ALERT_TYPE_USERVM_MIGRATE;
        if (VirtualMachine.Type.DomainRouter.equals(vm.getType())) {
            alertType = AlertManager.ALERT_TYPE_DOMAIN_ROUTER_MIGRATE;
        } else if (VirtualMachine.Type.ConsoleProxy.equals(vm.getType())) {
            alertType = AlertManager.ALERT_TYPE_CONSOLE_PROXY_MIGRATE;
        }
        
        VirtualMachineProfile<VMInstanceVO> profile = new VirtualMachineProfileImpl<VMInstanceVO>(vm);
        _networkMgr.prepareNicForMigration(profile, dest);
        _storageMgr.prepareForMigration(profile, dest);
        
        HypervisorGuru hvGuru = _hvGurus.get(vm.getHypervisorType());
        VirtualMachineTO to = hvGuru.implement(profile);
        PrepareForMigrationCommand pfmc = new PrepareForMigrationCommand(to);
        
        PrepareForMigrationAnswer pfma;
        try {
            pfma = (PrepareForMigrationAnswer)_agentMgr.send(dstHostId, pfmc);
        } catch (OperationTimedoutException e1) {
            throw new AgentUnavailableException("Operation timed out", dstHostId);
        }
        if (!pfma.getResult()) {
            throw new AgentUnavailableException(pfma.getDetails(), dstHostId);
        }
        
        boolean migrated = false;
        try {
            vm.setLastHostId(srcHostId);
            if (vm == null || vm.getRemoved() != null || vm.getHostId() == null || vm.getHostId() != srcHostId || !stateTransitTo(vm, Event.MigrationRequested, dstHostId)) {
                s_logger.info("Migration cancelled because state has changed: " + vm);
                return null;
            } 
            
            boolean isWindows = _guestOsCategoryDao.findById(_guestOsDao.findById(vm.getGuestOSId()).getCategoryId()).getName().equalsIgnoreCase("Windows");
            MigrateCommand mc = new MigrateCommand(vm.getInstanceName(), dest.getHost().getPrivateIpAddress(), isWindows);
            MigrateAnswer ma = (MigrateAnswer)_agentMgr.send(vm.getLastHostId(), mc);
            if (!ma.getResult()) {
                return null;
            }
            Commands cmds = new Commands(OnError.Revert);
            CheckVirtualMachineCommand cvm = new CheckVirtualMachineCommand(vm.getInstanceName());
            cmds.addCommand(cvm);
            
             _agentMgr.send(dstHostId, cmds);
             CheckVirtualMachineAnswer answer = cmds.getAnswer(CheckVirtualMachineAnswer.class);
            if (!answer.getResult()) {
                s_logger.debug("Unable to complete migration for " + vm.toString());
                stateTransitTo(vm, VirtualMachine.Event.AgentReportStopped, null);
                return null;
            }

            State state = answer.getState();
            if (state == State.Stopped) {
                s_logger.warn("Unable to complete migration as we can not detect it on " + dest.getHost());
                stateTransitTo(vm, VirtualMachine.Event.AgentReportStopped, null);
                return null;
            }

            stateTransitTo(vm, VirtualMachine.Event.OperationSucceeded, dstHostId);
            migrated = true;
            return vm;
        } catch (final OperationTimedoutException e) {
            s_logger.debug("operation timed out");
            if (e.isActive()) {
                // FIXME: scheduleRestart(vm, true);
            }
            throw new AgentUnavailableException("Operation timed out: ", dstHostId);
        } finally {
            if (!migrated) {
                s_logger.info("Migration was unsuccessful.  Cleaning up: " + vm.toString());

                _alertMgr.sendAlert(alertType, fromHost.getDataCenterId(), fromHost.getPodId(), "Unable to migrate vm " + vm.getName() + " from host " + fromHost.getName() + " in zone " + dest.getDataCenter().getName() + " and pod " + dest.getPod().getName(), "Migrate Command failed.  Please check logs.");

                stateTransitTo(vm, Event.MigrationFailedOnSource, srcHostId);
                
                Command cleanup = vmGuru.cleanup(vm, null);
                _agentMgr.easySend(dstHostId, cleanup);
            }
        }
    }
    
    @Override
    public boolean migrateAway(VirtualMachine.Type vmType, long vmId, long srcHostId) throws InsufficientServerCapacityException {
        VirtualMachineGuru<? extends VMInstanceVO> vmGuru = _vmGurus.get(vmType);
        VMInstanceVO vm = vmGuru.findById(vmId);
        if (vm == null) {
            s_logger.debug("Unable to find a VM for " + vmId);
            return true;
        }
        
        VirtualMachineProfile<VMInstanceVO> profile = new VirtualMachineProfileImpl<VMInstanceVO>(vm); 
        
        Long hostId = vm.getHostId();
        if (hostId == null) {
            s_logger.debug("Unable to migrate because the VM doesn't have a host id: " + vm);
            return true;
        }
        
        Host host = _hostDao.findById(hostId);
        
        DataCenterDeployment plan = new DataCenterDeployment(host.getDataCenterId(), host.getPodId(), host.getClusterId(), null);
        ExcludeList excludes = new ExcludeList();
        excludes.addHost(hostId);
        
        DeployDestination dest = null;
        while (true) {
            for (DeploymentPlanner planner : _planners) {
                dest = planner.plan(profile, plan, excludes);
                if (dest != null) {
                    if (s_logger.isDebugEnabled()) {
                        s_logger.debug("Planner " + planner + " found " + dest + " for migrating to.");
                    }
                    break;
                }
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Planner " + planner + " was unable to find anything.");
                }
            }
            
            if (dest == null) {
                throw new InsufficientServerCapacityException("Unable to find a server to migrate to.", host.getClusterId());
            }
            
            excludes.addHost(dest.getHost().getId());
            
            try {
                vm = migrate(vm, srcHostId, dest);
            } catch (ResourceUnavailableException e) {
                s_logger.debug("Unable to migrate to unavailable " + dest);
            }
            if (vm != null) {
                return true;
            }
        } 
    }
    
    protected class CleanupTask implements Runnable {
        @Override
        public void run() {
            s_logger.trace("VM Operation Thread Running");
            try {
                _workDao.cleanup(_cleanupWait);
            } catch (Exception e) {
                s_logger.error("VM Operations failed due to ", e);
            }
        }
    }
    
    @Override
    public <T extends VMInstanceVO> T reboot(T vm, Map<VirtualMachineProfile.Param, Object> params, User caller, Account account) throws InsufficientCapacityException, ResourceUnavailableException {
        try {
            return advanceReboot(vm, params, caller, account);
        } catch (ConcurrentOperationException e) {
            throw new CloudRuntimeException("Unable to reboot a VM due to concurrent operation", e);
        }
    }
    
    @Override
    public <T extends VMInstanceVO> T advanceReboot(T vm, Map<VirtualMachineProfile.Param, Object> params, User caller, Account account) throws InsufficientCapacityException, ConcurrentOperationException, ResourceUnavailableException {  
        T rebootedVm = null;
        
        DataCenter dc = _configMgr.getZone(vm.getDataCenterId());
        HostPodVO pod = _configMgr.getPod(vm.getPodId());
        Host host = _hostDao.findById(vm.getHostId());
        Cluster cluster = null;
        if (host != null) {
            cluster = _configMgr.getCluster(host.getClusterId());
        }
        DeployDestination dest = new DeployDestination(dc, pod, cluster, host);
        ReservationContext ctx = new ReservationContextImpl(null, null, caller, account);
        VirtualMachineProfile<VMInstanceVO> vmProfile = new VirtualMachineProfileImpl<VMInstanceVO>(vm);
            
        try {
            //prepare all network elements (start domR/dhcp if needed)
            _networkMgr.prepare(vmProfile, dest, ctx);
            Commands cmds = new Commands(OnError.Revert);
            cmds.addCommand(new RebootCommand(vm.getName()));
            _agentMgr.send(host.getId(), cmds);
           
            Answer rebootAnswer = cmds.getAnswer(RebootAnswer.class);
            if (rebootAnswer != null && rebootAnswer.getResult()) {
                rebootedVm = vm;
                return rebootedVm;
            }
            s_logger.info("Unable to reboot VM " + vm + " on " + dest.getHost() + " due to " + (rebootAnswer == null ? " no reboot answer" : rebootAnswer.getDetails()));
        } catch (OperationTimedoutException e) {
            s_logger.warn("Unable to send the reboot command to host " + dest.getHost() + " for the vm " + vm + " due to operation timeout", e);
            throw new CloudRuntimeException("Failed to reboot the vm on host " + dest.getHost());
        }
        
        return rebootedVm;
    }
    
    @Override
    public boolean preStateTransitionEvent(State oldState,
            Event event, State newState, VirtualMachine vm, boolean transitionStatus, Long id) {
        s_logger.debug("VM state transitted from :" + oldState + " to " + newState + " with event: " + event +
                "vm's original host id: " + vm.getHostId() + " new host id: " + id);
        if (!transitionStatus) {
            return false;
        }

        if (oldState == State.Starting) {
            if (event == Event.OperationSucceeded) {
                if (vm.getLastHostId() != null && vm.getLastHostId() != id) {
                    /*need to release the reserved capacity on lasthost*/
                    _capacityMgr.releaseVmCapacity(vm, true, false, vm.getLastHostId());
                }
                vm.setLastHostId(id);
            } else if (event == Event.OperationFailed) {
                _capacityMgr.releaseVmCapacity(vm, false, false, vm.getHostId());
            } else if (event == Event.OperationRetry) {
                _capacityMgr.releaseVmCapacity(vm, false, false, vm.getHostId());
            } else if (event == Event.AgentReportStopped) {
                _capacityMgr.releaseVmCapacity(vm, false, true, vm.getHostId());
            }
        } else if (oldState == State.Running) {
            if (event == Event.AgentReportStopped) {
                _capacityMgr.releaseVmCapacity(vm, false, true, vm.getHostId());
            }
        } else if (oldState == State.Migrating) {
            if (event == Event.AgentReportStopped) {
                /*Release capacity from original host*/
                _capacityMgr.releaseVmCapacity(vm, false, true, vm.getHostId());
            } else if (event == Event.MigrationFailedOnSource) {
                /*release capacity from dest host*/
                _capacityMgr.releaseVmCapacity(vm, false, false, id);
                id = vm.getHostId();
            } else if (event == Event.MigrationFailedOnDest) {
                /*release capacify from original host*/
                _capacityMgr.releaseVmCapacity(vm, false, false, vm.getHostId());
            } else if (event == Event.OperationSucceeded) {
                _capacityMgr.releaseVmCapacity(vm, false, false, vm.getHostId());
                /*set lasthost id to migration destination host id*/
                vm.setLastHostId(id);
            }
        } else if (oldState == State.Stopping) {
            if (event == Event.AgentReportStopped || event == Event.OperationSucceeded) {
                _capacityMgr.releaseVmCapacity(vm, false, true, vm.getHostId());
            }
        } else if (oldState == State.Stopped) {
            if (event == Event.DestroyRequested) {
                _capacityMgr.releaseVmCapacity(vm, true, false, vm.getLastHostId());
                vm.setLastHostId(null);
            }
        }
        return transitionStatus;
    }
    
    @Override
    public boolean postStateTransitionEvent(State oldState, Event event, State newState, VirtualMachine vo, boolean status) {
        return true;
    }
    
    @Override
    public VirtualMachine start(VirtualMachine.Type type, long vmId, Map<VirtualMachineProfile.Param, Object> params) {
        
        return null;
    }
    
    @Override
    public VMInstanceVO findById(VirtualMachine.Type type, long vmId) {
        VirtualMachineGuru<? extends VMInstanceVO> guru = _vmGurus.get(type);
        return guru.findById(vmId);
    }
    
    public Command cleanup(String vmName) {
        return new StopCommand(vmName);
    }
    
    public Commands deltaSync(long hostId, Map<String, State> newStates) {
        Map<Long, AgentVmInfo> states = convertToIds(newStates);
        Commands commands = new Commands(OnError.Continue);
        
        boolean nativeHA = _agentMgr.isHostNativeHAEnabled(hostId);
        
        for (final Map.Entry<Long, AgentVmInfo> entry : states.entrySet()) {
            AgentVmInfo info = entry.getValue();

            VMInstanceVO vm = info.vm;

            Command command = null;
            if (vm != null) {
                command = compareState(vm, info, false, nativeHA);
            }  else {
                command = cleanup(info.name);
            }

            if (command != null) {
                commands.addCommand(command);
            }
        }

        return commands;
    }
    
    protected Map<Long, AgentVmInfo> convertToIds(final Map<String, State> states) {
        final HashMap<Long, AgentVmInfo> map = new HashMap<Long, AgentVmInfo>();

        if (states == null) {
            return map;
        }

        Collection<VirtualMachineGuru<? extends VMInstanceVO>> vmGurus = _vmGurus.values();

        for (Map.Entry<String, State> entry : states.entrySet()) {
            for (VirtualMachineGuru<? extends VMInstanceVO> vmGuru : vmGurus) {
                String name = entry.getKey();

                VMInstanceVO vm = vmGuru.findByName(name);

                if (vm != null) {
                    map.put(vm.getId(), new AgentVmInfo(entry.getKey(), vm, entry.getValue()));
                    break;
                }
                
                Long id = vmGuru.convertToId(name);
                if (id != null) {
                    map.put(id, new AgentVmInfo(entry.getKey(), null, entry.getValue()));
                }
            }
        }

        return map;
    }

    /**
     * compareState does as its name suggests and compares the states between
     * management server and agent.  It returns whether something should be
     * cleaned up
     *
     */
    protected Command compareState(VMInstanceVO vm, final AgentVmInfo info, final boolean fullSync, boolean nativeHA) {
        State agentState = info.state;
        final String agentName = info.name;
        final State serverState = vm.getState();
        final String serverName = vm.getName();
        
        Command command = null;

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("VM " + serverName + ": server state = " + serverState.toString() + " and agent state = " + agentState.toString());
        }
        
        if (agentState == State.Error) {
            agentState = State.Stopped;
            
            short alertType = AlertManager.ALERT_TYPE_USERVM;
            if (VirtualMachine.Type.DomainRouter.equals(vm.getType())) {
                alertType = AlertManager.ALERT_TYPE_DOMAIN_ROUTER;
            } else if (VirtualMachine.Type.ConsoleProxy.equals(vm.getType())) {
                alertType = AlertManager.ALERT_TYPE_CONSOLE_PROXY;
            }

            HostPodVO podVO = _podDao.findById(vm.getPodId());
            DataCenterVO dcVO = _dcDao.findById(vm.getDataCenterId());
            HostVO hostVO = _hostDao.findById(vm.getHostId());
            
            String hostDesc = "name: " + hostVO.getName() + " (id:" + hostVO.getId() + "), availability zone: " + dcVO.getName() + ", pod: " + podVO.getName();
            _alertMgr.sendAlert(alertType, vm.getDataCenterId(), vm.getPodId(), "VM (name: " + vm.getName() + ", id: " + vm.getId() + ") stopped on host " + hostDesc + " due to storage failure", "Virtual Machine " + vm.getName() + " (id: " + vm.getId() + ") running on host [" + vm.getHostId() + "] stopped due to storage failure.");
        }
        
        if (serverState == State.Migrating) {
            s_logger.debug("Skipping vm in migrating state: " + vm.toString());
            return null;
        }

        if (agentState == serverState) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Both states are " + agentState.toString() + " for " + serverName);
            }
            assert (agentState == State.Stopped || agentState == State.Running) : "If the states we send up is changed, this must be changed.";
            stateTransitTo(vm, agentState == State.Stopped ? VirtualMachine.Event.AgentReportStopped : VirtualMachine.Event.AgentReportRunning, vm.getHostId());
            if (agentState == State.Stopped) {
                s_logger.debug("State matches but the agent said stopped so let's send a cleanup anyways.");
                return cleanup(agentName);
            }
            return null;
        }
        if (agentState == State.Shutdowned ) {
            if ( serverState == State.Running || serverState == State.Starting || serverState == State.Stopping ) {
                stateTransitTo(vm, VirtualMachine.Event.AgentReportShutdowned, null);
            }
            s_logger.debug("Sending cleanup to a shutdowned vm: " + agentName);            
            command = cleanup(agentName);
        } else if (agentState == State.Stopped) {
            // This state means the VM on the agent was detected previously
            // and now is gone.  This is slightly different than if the VM
            // was never completed but we still send down a Stop Command
            // to ensure there's cleanup.
            if (serverState == State.Running ) {
                if(!nativeHA) {
                    // Our records showed that it should be running so let's restart it.
                    vm = findById(vm.getType(), vm.getId());
                    _haMgr.scheduleRestart(vm, false);
                    command = cleanup(agentName);
                } else {
                    s_logger.info("VM is in runnting state, agent reported as stopped and native HA is enabled => skip sync action");
                    stateTransitTo(vm, Event.AgentReportStopped, null);
                }
            } else if (serverState == State.Stopping) {
                if (fullSync) {
                    s_logger.debug("VM is in stopping state on full sync.  Updating the status to stopped");
                    vm = findById(vm.getType(), vm.getId());
//                    advanceStop(vm, true, _accountMgr.getSystemUser(), _accountMgr.getSystemAccount());
                    command = cleanup(agentName);
                } else {
                    s_logger.debug("Ignoring VM in stopping mode: " + vm.getName());
                }
            } else if (serverState == State.Starting) {
                s_logger.debug("Ignoring VM in starting mode: " + vm.getName());
                stateTransitTo(vm, VirtualMachine.Event.AgentReportStopped, null);
            } else {
                s_logger.debug("Sending cleanup to a stopped vm: " + agentName);            
                stateTransitTo(vm, VirtualMachine.Event.AgentReportStopped, null);
                command = cleanup(agentName);
            }
        } else if (agentState == State.Running) {
            if (serverState == State.Starting) {
                if (fullSync) {
                    s_logger.debug("VM state is starting on full sync so updating it to running");
                    vm = findById(vm.getType(), vm.getId());
                    stateTransitTo(vm, Event.AgentReportRunning, vm.getHostId());
                    //finalizeStart(new VirtualMachineProfileImpl<VMInstanceVO>(vm), vm.getHostId(), null, null);
                }
            } else if (serverState == State.Stopping) {
                if (fullSync) {
                    s_logger.debug("VM state is in stopping on fullsync so resend stop.");
                    vm = findById(vm.getType(), vm.getId());
                    stateTransitTo(vm, Event.AgentReportStopped, null);
                    //finalizeStop(new VirtualMachineProfileImpl<VMInstanceVO>(vm), null);
                    command = cleanup(agentName);
                } else {
                    s_logger.debug("VM is in stopping state so no action.");
                }
            } else if (serverState == State.Destroyed || serverState == State.Stopped || serverState == State.Expunging) {
                s_logger.debug("VM state is in stopped so stopping it on the agent");
                command = cleanup(agentName);
            } else {
                stateTransitTo(vm, VirtualMachine.Event.AgentReportRunning, vm.getHostId());
            }
        } /*else if (agentState == State.Unknown) {
            if (serverState == State.Running) {
                if (fullSync) {
                    vm = info.vmGuru.get(vm.getId());
                }
                scheduleRestart(vm, false);
            } else if (serverState == State.Starting) {
                if (fullSync) {
                    vm = info.vmGuru.get(vm.getId());
                }
                scheduleRestart(vm, false);
            } else if (serverState == State.Stopping) {
                if (fullSync) {
                    s_logger.debug("VM state is stopping in full sync.  Resending stop");
                    command = info.vmGuru.getCleanupCommand(vm, agentName);
                }
            }
        }*/
        return command;
    }

    public Commands fullSync(final long hostId, final Map<String, State> newStates) {
        Commands commands = new Commands(OnError.Continue);
        final List<? extends VMInstanceVO> vms = _vmDao.listByHostId(hostId);
        s_logger.debug("Found " + vms.size() + " VMs for host " + hostId);

        Map<Long, AgentVmInfo> states = convertToIds(newStates);

        boolean nativeHA = _agentMgr.isHostNativeHAEnabled(hostId);

        for (final VMInstanceVO vm : vms) {
            AgentVmInfo info = states.remove(vm.getId());

            if (info == null) {
                info = new AgentVmInfo(vm.getInstanceName(), null, State.Stopped);
            }
            
            VirtualMachineGuru<? extends VMInstanceVO> vmGuru = getVmGuru(vm);
            VMInstanceVO castedVm = vmGuru.findById(vm.getId());
            final Command command = compareState(castedVm, info, true, nativeHA);
            if (command != null) {
                commands.addCommand(command);
            }
        }

        for (final AgentVmInfo left : states.values()) {
            if (nativeHA) {
                for (VirtualMachineGuru<? extends VMInstanceVO> vmGuru : _vmGurus.values()) {
                    VMInstanceVO vm = vmGuru.findByName(left.name);
                    if (vm == null) {
                        s_logger.warn("Stopping a VM that we have no record of: " + left.name);
                        commands.addCommand(cleanup(left.name));
                    } else {
                        Command command = compareState(vm, left, true, nativeHA);
                        if (command != null) {
                            commands.addCommand(command);
                        }
                    }
                }
            } else {
                s_logger.warn("Stopping a VM that we have no record of: " + left.name);
                commands.addCommand(cleanup(left.name));
            }
        }

        return commands;
    }

    
    @Override
    public boolean isRecurring() {
        return false;
    }

    @Override
    public boolean processAnswers(long agentId, long seq, Answer[] answers) {
        for (final Answer answer : answers) {
            if (!answer.getResult()) {
                s_logger.warn("Cleanup failed due to " + answer.getDetails());
            } else {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Cleanup succeeded. Details " + answer.getDetails());
                }
            }
        }
        return true;
    }
    
    @Override
    public boolean processTimeout(long agentId, long seq) {
        return true;
    }
    
    @Override
    public int getTimeout() {
        return -1;
    }

    @Override
    public boolean processCommands(long agentId, long seq, Command[] cmds) {
        boolean processed = false;
        for (Command cmd : cmds) {
            if (cmd instanceof PingRoutingCommand) {
                PingRoutingCommand ping = (PingRoutingCommand)cmd;
                if (ping.getNewStates().size() > 0) {
                    Commands commands = deltaSync(agentId, ping.getNewStates());
                    if (commands.size() > 0) {
                        try {
                            _agentMgr.send(agentId, commands, this);
                        } catch (final AgentUnavailableException e) {
                            s_logger.warn("Agent is now unavailable", e);
                        }
                    }
                }
                processed = true;
            }
        }
        return processed;
    }

    @Override
    public AgentControlAnswer processControlCommand(long agentId, AgentControlCommand cmd) {
        return null;
    }
    
    @Override
    public boolean processDisconnect(long agentId, Status state) {
        return true;
    }
    
    @Override
    public void processConnect(HostVO agent, StartupCommand cmd) throws ConnectionException {
        if (!(cmd instanceof StartupRoutingCommand)) {
            return;
        }
        
        long agentId = agent.getId();
        
        StartupRoutingCommand startup = (StartupRoutingCommand)cmd;
        
        Commands commands = fullSync(agentId, startup.getVmStates());
        
        if (commands.size() > 0) {
            s_logger.debug("Sending clean commands to the agent");

            try {
                boolean error = false;
                Answer[] answers = _agentMgr.send(agentId, commands);
                for (Answer answer : answers) {
                    if (!answer.getResult()) {
                        s_logger.warn("Unable to stop a VM due to " + answer.getDetails());
                        error = true;
                    }
                }
                if (error) {
                    throw new ConnectionException(true, "Unable to stop VMs");
                }
            } catch (final AgentUnavailableException e) {
                s_logger.warn("Agent is unavailable now", e);
                throw new ConnectionException(true, "Unable to sync", e);
            } catch (final OperationTimedoutException e) {
                s_logger.warn("Agent is unavailable now", e);
                throw new ConnectionException(true, "Unable to sync", e);
            }
        }
    }
    
    protected class TransitionTask implements Runnable {
        @Override
        public void run() {
            GlobalLock lock = GlobalLock.getInternLock("TransitionChecking");
            if (lock == null) {
                s_logger.debug("Couldn't get the global lock");
                return;
            }
            
            if (!lock.lock(30)) {
                s_logger.debug("Couldn't lock the db");
                return;
            }
            try {
                lock.addRef();
                List<VMInstanceVO> instances = _vmDao.findVMInTransition(new Date(new Date().getTime() - (_operationTimeout * 1000)), State.Starting, State.Stopping);
                for (VMInstanceVO instance : instances) {
                    State state = instance.getState();
                    if (state == State.Stopping) {
                        _haMgr.scheduleStop(instance, instance.getHostId(), true);
                    } else if (state == State.Starting) {
                        _haMgr.scheduleRestart(instance, true);
                    }
                }
            } catch (Exception e) {
                s_logger.warn("Caught the following exception on transition checking", e);
            } finally {
                StackMaid.current().exitCleanup();
                lock.unlock();
            }
        }
    }
    
    protected class AgentVmInfo {
        public String name;
        public State state;
        public State action;
        public VMInstanceVO vm;

        public AgentVmInfo(String name, VMInstanceVO vm, State state) {
            this.name = name;
            this.state = state;
            this.vm = vm;
        }
    }
}
