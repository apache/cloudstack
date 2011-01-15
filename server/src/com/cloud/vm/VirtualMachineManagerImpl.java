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
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.CheckVirtualMachineAnswer;
import com.cloud.agent.api.CheckVirtualMachineCommand;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.MigrateAnswer;
import com.cloud.agent.api.MigrateCommand;
import com.cloud.agent.api.PrepareForMigrationAnswer;
import com.cloud.agent.api.PrepareForMigrationCommand;
import com.cloud.agent.api.StartAnswer;
import com.cloud.agent.api.StartCommand;
import com.cloud.agent.api.StopAnswer;
import com.cloud.agent.api.StopCommand;
import com.cloud.agent.api.to.VirtualMachineTO;
import com.cloud.agent.manager.Commands;
import com.cloud.alert.AlertManager;
import com.cloud.cluster.ClusterManager;
import com.cloud.configuration.Config;
import com.cloud.configuration.dao.ConfigurationDao;
import com.cloud.consoleproxy.ConsoleProxyManager;
import com.cloud.dc.DataCenter;
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
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InsufficientServerCapacityException;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.host.Host;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.hypervisor.HypervisorGuru;
import com.cloud.network.NetworkManager;
import com.cloud.network.NetworkVO;
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
public class VirtualMachineManagerImpl implements VirtualMachineManager {
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
    
    @Inject(adapter=DeploymentPlanner.class)
    protected Adapters<DeploymentPlanner> _planners;
    @Inject(adapter=StateListener.class)
    protected Adapters<StateListener<State, VirtualMachine.Event, VMInstanceVO>> _stateListner;
    

    Map<VirtualMachine.Type, VirtualMachineGuru<? extends VMInstanceVO>> _vmGurus = new HashMap<VirtualMachine.Type, VirtualMachineGuru<? extends VMInstanceVO>>();
    Map<HypervisorType, HypervisorGuru> _hvGurus = new HashMap<HypervisorType, HypervisorGuru>();
    protected StateMachine2<State, VirtualMachine.Event, VMInstanceVO> _stateMachine;
    
    ScheduledExecutorService _executor = null;
    
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
            Map<String, Object> params,
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
    	
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Expunged " + vm);
        }

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
        
        _executor = Executors.newScheduledThreadPool(1, new NamedThreadFactory("Vm-Operations-Cleanup"));
        _nodeId = _clusterMgr.getId();
      
        setStateMachine();
        
        return true;
    }
    
    @Override
    public String getName() {
        return _name;
    }
    
    protected VirtualMachineManagerImpl() {
    }
    
    @Override
    public <T extends VMInstanceVO> T start(T vm, Map<String, Object> params, User caller, Account account) throws InsufficientCapacityException, ResourceUnavailableException {
        try {
            return advanceStart(vm, params, caller, account);
        } catch (ConcurrentOperationException e) {
            throw new CloudRuntimeException("Unable to start a VM due to concurrent operation", e);
        }
    }

    private Answer getStartAnswer(Answer[] answers) {
    	for (Answer ans : answers) {
    		if (ans instanceof StartAnswer) {
    			return ans;
    		}
    	}
    	
    	assert false : "Why there is no Start Answer???";
    	return null;
    }
    
    protected boolean checkWorkItems(VMInstanceVO vm, State state) throws ConcurrentOperationException {
        while (true) {
            ItWorkVO vo = _workDao.findByInstance(vm.getId(), state);
            if (vo == null) {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Unable to find work for " + vm);
                }
                return true;
            }
            
            if (vo.getStep() == Step.Done || vo.getStep() == Step.Cancelled) {
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
            if (stateTransitTo(vm, Event.StartRequested, null, work.getId())) {
                
                Journal journal = new Journal.LogJournal("Creating " + vm, s_logger);
                work = _workDao.persist(work);
                ReservationContextImpl context = new ReservationContextImpl(work.getId(), journal, caller, account);
                
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Successfully transitioned to start state for " + vm + " reservation id = " + work.getId());
                }
                txn.commit();
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
                txn.commit();
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
                txn.commit();
                return null;
            }
        }
        
        throw new ConcurrentOperationException("Unable to change the state of " + vm);
    }
    
    @Override
    public <T extends VMInstanceVO> T advanceStart(T vm, Map<String, Object> params, User caller, Account account) throws InsufficientCapacityException, ConcurrentOperationException, ResourceUnavailableException {
        long vmId = vm.getId();
        
        VirtualMachineGuru<T> vmGuru = getVmGuru(vm);
        
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
        VirtualMachineProfileImpl<T> vmProfile = new VirtualMachineProfileImpl<T>(vm, template, offering, null, params);
        
        
        try {
            Journal journal = start.second().getJournal();
            
            ExcludeList avoids = new ExcludeList();
            int retry = _retry;
            while (retry-- != 0) { // It's != so that it can match -1.
                
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
                
            	stateTransitTo(vm, Event.OperationRetry, dest.getHost().getId());
                
                try {
                    _storageMgr.prepare(vmProfile, dest);
                    _networkMgr.prepare(vmProfile, dest, ctx);
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
                }
                
                vmGuru.finalizeVirtualMachineProfile(vmProfile, dest, ctx);
                
                VirtualMachineTO vmTO = hvGuru.implement(vmProfile);
                
                Commands cmds = new Commands(OnError.Revert);
                cmds.addCommand(new StartCommand(vmTO));
                
                vmGuru.finalizeDeployment(cmds, vmProfile, dest, ctx);
                vm.setPodId(dest.getPod().getId());
                try {
                    Answer[] answers = _agentMgr.send(dest.getHost().getId(), cmds);
                    if (getStartAnswer(answers).getResult() && vmGuru.finalizeStart(cmds, vmProfile, dest, ctx)) {
                        if (!stateTransitTo(vm, Event.OperationSucceeded, dest.getHost().getId())) {
                            throw new CloudRuntimeException("Unable to transition to a new state.");
                        }
                        startedVm = vm;
                        break;
                    }
                    s_logger.info("Unable to start VM on " + dest.getHost() + " due to " + answers[0].getDetails());
                } catch (AgentUnavailableException e) {
                    s_logger.debug("Unable to send the start command to host " + dest.getHost());
                    continue;
                } catch (OperationTimedoutException e) {
                    s_logger.debug("Unable to send the start command to host " + dest.getHost());
                    continue;
                }
            }
            
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Creation complete for VM " + vm);
            }
        } finally {
            if (startedVm == null) {
                stateTransitTo(vm, Event.OperationFailed, null);
            }
            work.setStep(Step.Done);
            _workDao.update(work.getId(), work);
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

    @Override
    public <T extends VMInstanceVO> boolean advanceStop(T vm, boolean forced, User user, Account account) throws AgentUnavailableException, OperationTimedoutException, ConcurrentOperationException {
        State state = vm.getState();
        if (state == State.Stopped) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("VM is already stopped: " + vm);
            }
            return true;
        }
        
        if (state == State.Destroyed || state == State.Expunging || state == State.Error) {
            s_logger.debug("Stopped called on " + vm + " but the state is " + state);
            return true;
        }
        
        if (!stateTransitTo(vm, Event.StopRequested, vm.getHostId())) {
            throw new ConcurrentOperationException("VM is being operated on by someone else.");
        }
        
        if (vm.getHostId() == null) {
            s_logger.debug("Host id is null so we can't stop it.  How did we get into here?");
            return false;
        }
        
        String reservationId = vm.getReservationId();

        StopCommand stop = new StopCommand(vm, vm.getInstanceName(), null);

        boolean stopped = false;
        StopAnswer answer = null;
        try {
            answer = (StopAnswer)_agentMgr.send(vm.getHostId(), stop);
            stopped = answer.getResult();
            if (!stopped) {
                throw new CloudRuntimeException("Unable to stop the virtual machine due to " + answer.getDetails());
            } else {
                UsageEventVO usageEvent = new UsageEventVO(EventTypes.EVENT_VM_STOP, vm.getAccountId(), vm.getDataCenterId(), vm.getId(), vm.getName(), vm.getServiceOfferingId(), vm.getTemplateId(), null);
                _usageEventDao.persist(usageEvent);
            }
        } finally {
            if (!stopped) {
                if (!forced) {
                    stateTransitTo(vm, Event.OperationFailed, vm.getHostId());
                } else {
                    s_logger.warn("Unable to actually stop " + vm + " but continue with release because it's a force stop");
                }
            }
        }
        
        if (s_logger.isDebugEnabled()) {
            s_logger.debug(vm + " is stopped on the host.  Proceeding to release resource held.");
        }
        
        boolean cleanup = false;
        
        VirtualMachineProfile<T> profile = new VirtualMachineProfileImpl<T>(vm);
        try {
            _networkMgr.release(profile, forced);
            s_logger.debug("Successfully released network resources for the vm " + vm);
        } catch (Exception e) {
            s_logger.warn("Unable to release some network resources.", e);
            cleanup = true;
        }
        
        try {
            _storageMgr.release(profile);
            s_logger.debug("Successfully released storage resources for the vm " + vm);
        } catch (Exception e) {
            s_logger.warn("Unable to release storage resources.", e);
            cleanup = true;
        }
         
        @SuppressWarnings("unchecked")
        VirtualMachineGuru<T> guru = (VirtualMachineGuru<T>)_vmGurus.get(vm.getType());
        try {
            guru.finalizeStop(profile, vm.getHostId(), vm.getReservationId(), answer);
        } catch (Exception e) {
            s_logger.warn("Guru " + guru.getClass() + " has trouble processing stop ");
            cleanup = true;
        }
            
        vm.setReservationId(null);
        
        stateTransitTo(vm, Event.OperationSucceeded, null);

        if (cleanup) {
            ItWorkVO work = new ItWorkVO(reservationId, _nodeId, State.Stopping, vm.getId());
            _workDao.persist(work);
        }
        
        return stopped;
    }
    
    private void setStateMachine() {
    	_stateMachine = new StateMachine2<State, VirtualMachine.Event, VMInstanceVO>();

    	_stateMachine.addTransition(State.Stopped, VirtualMachine.Event.StartRequested, State.Starting);
    	_stateMachine.addTransition(State.Stopped, VirtualMachine.Event.DestroyRequested, State.Destroyed);
    	_stateMachine.addTransition(State.Stopped, VirtualMachine.Event.StopRequested, State.Stopped);
    	_stateMachine.addTransition(State.Stopped, VirtualMachine.Event.AgentReportStopped, State.Stopped);
    	_stateMachine.addTransition(State.Stopped, VirtualMachine.Event.OperationFailed, State.Error);
    	_stateMachine.addTransition(State.Stopped, VirtualMachine.Event.ExpungeOperation, State.Expunging);
    	_stateMachine.addTransition(State.Stopped, VirtualMachine.Event.AgentReportShutdowned, State.Stopped); 
    	_stateMachine.addTransition(State.Starting, VirtualMachine.Event.OperationRetry, State.Starting);
    	_stateMachine.addTransition(State.Starting, VirtualMachine.Event.OperationSucceeded, State.Running);
    	_stateMachine.addTransition(State.Starting, VirtualMachine.Event.OperationFailed, State.Stopped);
    	_stateMachine.addTransition(State.Starting, VirtualMachine.Event.AgentReportRunning, State.Running);
    	_stateMachine.addTransition(State.Starting, VirtualMachine.Event.AgentReportStopped, State.Stopped);
    	_stateMachine.addTransition(State.Starting, VirtualMachine.Event.AgentReportShutdowned, State.Stopped); 
    	_stateMachine.addTransition(State.Destroyed, VirtualMachine.Event.RecoveryRequested, State.Stopped);
    	_stateMachine.addTransition(State.Destroyed, VirtualMachine.Event.ExpungeOperation, State.Expunging);
    	_stateMachine.addTransition(State.Running, VirtualMachine.Event.MigrationRequested, State.Migrating);
    	_stateMachine.addTransition(State.Running, VirtualMachine.Event.AgentReportRunning, State.Running);
    	_stateMachine.addTransition(State.Running, VirtualMachine.Event.AgentReportStopped, State.Stopped);
    	_stateMachine.addTransition(State.Running, VirtualMachine.Event.StopRequested, State.Stopping);
    	_stateMachine.addTransition(State.Running, VirtualMachine.Event.AgentReportShutdowned, State.Stopped); 
    	_stateMachine.addTransition(State.Migrating, VirtualMachine.Event.MigrationRequested, State.Migrating);
    	_stateMachine.addTransition(State.Migrating, VirtualMachine.Event.OperationSucceeded, State.Running);
    	_stateMachine.addTransition(State.Migrating, VirtualMachine.Event.OperationFailed, State.Running);
    	_stateMachine.addTransition(State.Migrating, VirtualMachine.Event.MigrationFailedOnSource, State.Running);
    	_stateMachine.addTransition(State.Migrating, VirtualMachine.Event.MigrationFailedOnDest, State.Running);
    	_stateMachine.addTransition(State.Migrating, VirtualMachine.Event.AgentReportRunning, State.Running);
    	_stateMachine.addTransition(State.Migrating, VirtualMachine.Event.AgentReportStopped, State.Stopped);
        _stateMachine.addTransition(State.Migrating, VirtualMachine.Event.AgentReportShutdowned, State.Stopped); 
    	_stateMachine.addTransition(State.Stopping, VirtualMachine.Event.OperationSucceeded, State.Stopped);
    	_stateMachine.addTransition(State.Stopping, VirtualMachine.Event.OperationFailed, State.Running);
    	_stateMachine.addTransition(State.Stopping, VirtualMachine.Event.AgentReportRunning, State.Running);
    	_stateMachine.addTransition(State.Stopping, VirtualMachine.Event.AgentReportStopped, State.Stopped);
    	_stateMachine.addTransition(State.Stopping, VirtualMachine.Event.StopRequested, State.Stopping);
    	_stateMachine.addTransition(State.Stopping, VirtualMachine.Event.AgentReportShutdowned, State.Stopped); 
    	_stateMachine.addTransition(State.Expunging, VirtualMachine.Event.OperationFailed, State.Expunging);
    	_stateMachine.addTransition(State.Expunging, VirtualMachine.Event.ExpungeOperation, State.Expunging);
        _stateMachine.addTransition(State.Error, VirtualMachine.Event.DestroyRequested, State.Expunging);
        _stateMachine.addTransition(State.Error, VirtualMachine.Event.ExpungeOperation, State.Expunging);
    	
    	_stateMachine.registerListeners(_stateListner);
    }
    
    protected boolean stateTransitTo(VMInstanceVO vm, VirtualMachine.Event e, Long hostId, String reservationId) {
        vm.setReservationId(reservationId);
		if (vm instanceof UserVmVO) {
			return _stateMachine.transitTO(vm, e, hostId, _userVmDao);
		} else if (vm instanceof ConsoleProxyVO) {
			return _stateMachine.transitTO(vm, e, hostId, _consoleDao);
		} else if (vm instanceof SecondaryStorageVmVO) {
			return _stateMachine.transitTO(vm, e, hostId, _secondaryDao);
		} else if (vm instanceof DomainRouterVO) {
			return _stateMachine.transitTO(vm, e, hostId, _routerDao);
		} else {
			return _stateMachine.transitTO(vm, e, hostId, _vmDao);
		}
    }
    
    @Override
    public boolean stateTransitTo(VMInstanceVO vm, VirtualMachine.Event e, Long hostId) {
        if (vm instanceof UserVmVO) {
            return _stateMachine.transitTO(vm, e, hostId, _userVmDao);
        } else if (vm instanceof ConsoleProxyVO) {
            return _stateMachine.transitTO(vm, e, hostId, _consoleDao);
        } else if (vm instanceof SecondaryStorageVmVO) {
            return _stateMachine.transitTO(vm, e, hostId, _secondaryDao);
        } else if (vm instanceof DomainRouterVO) {
            return _stateMachine.transitTO(vm, e, hostId, _routerDao);
        } else {
            return _stateMachine.transitTO(vm, e, hostId, _vmDao);
        }
    }
    
    @Override
    public <T extends VMInstanceVO> boolean remove(T vm, User user, Account caller) {
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
            
            CheckVirtualMachineCommand cvm = new CheckVirtualMachineCommand(vm.getInstanceName());
            CheckVirtualMachineAnswer answer = (CheckVirtualMachineAnswer)_agentMgr.send(dstHostId, cvm);
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

            // FIXME:          _networkGroupMgr.handleVmStateTransition(vm, State.Running);
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
}
