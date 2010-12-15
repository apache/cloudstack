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

import javax.ejb.Local;
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;

import com.cloud.agent.AgentManager;
import com.cloud.agent.AgentManager.OnError;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Start2Command;
import com.cloud.agent.api.StopAnswer;
import com.cloud.agent.api.StopCommand;
import com.cloud.agent.api.to.VirtualMachineTO;
import com.cloud.agent.manager.Commands;
import com.cloud.capacity.dao.CapacityDao;
import com.cloud.cluster.ClusterManager;
import com.cloud.cluster.ClusterManagerListener;
import com.cloud.cluster.ManagementServerHostVO;
import com.cloud.configuration.Config;
import com.cloud.configuration.dao.ConfigurationDao;
import com.cloud.dc.DataCenter;
import com.cloud.deploy.DataCenterDeployment;
import com.cloud.deploy.DeployDestination;
import com.cloud.deploy.DeploymentPlan;
import com.cloud.deploy.DeploymentPlanner;
import com.cloud.deploy.DeploymentPlanner.ExcludeList;
import com.cloud.domain.dao.DomainDao;
import com.cloud.event.EventTypes;
import com.cloud.event.EventVO;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InsufficientNetworkCapacityException;
import com.cloud.exception.InsufficientServerCapacityException;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.hypervisor.HypervisorGuru;
import com.cloud.network.NetworkManager;
import com.cloud.network.NetworkVO;
import com.cloud.network.dao.NetworkDao;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.storage.DiskOfferingVO;
import com.cloud.storage.Storage.ImageFormat;
import com.cloud.storage.StorageManager;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.Volume.VolumeType;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.user.Account;
import com.cloud.user.User;
import com.cloud.user.dao.AccountDao;
import com.cloud.user.dao.UserDao;
import com.cloud.utils.Journal;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.Pair;
import com.cloud.utils.component.Adapters;
import com.cloud.utils.component.ComponentLocator;
import com.cloud.utils.component.Inject;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.fsm.StateMachine2;
import com.cloud.vm.ItWorkVO.Type;
import com.cloud.vm.VirtualMachine.Event;
import com.cloud.vm.dao.ConsoleProxyDao;
import com.cloud.vm.dao.DomainRouterDao;
import com.cloud.vm.dao.NicDao;
import com.cloud.vm.dao.SecondaryStorageVmDao;
import com.cloud.vm.dao.UserVmDao;
import com.cloud.vm.dao.VMInstanceDao;

@Local(value=VirtualMachineManager.class)
public class VirtualMachineManagerImpl implements VirtualMachineManager, ClusterManagerListener {
    private static final Logger s_logger = Logger.getLogger(VirtualMachineManagerImpl.class);
    
    String _name;
    @Inject private StorageManager _storageMgr;
    @Inject private NetworkManager _networkMgr;
    @Inject private AgentManager _agentMgr;
    @Inject private VMInstanceDao _vmDao;
    @Inject private ServiceOfferingDao _offeringDao;
    @Inject private VMTemplateDao _templateDao;
    @Inject private UserDao _userDao;
    @Inject private AccountDao _accountDao;
    @Inject private DomainDao _domainDao;
    @Inject private ClusterManager _clusterMgr;
    @Inject private ItWorkDao _workDao;
    @Inject private CapacityDao _capacityDao;
    @Inject private UserVmDao _userVmDao;
    @Inject private DomainRouterDao _routerDao;
    @Inject private ConsoleProxyDao _consoleDao;
    @Inject private SecondaryStorageVmDao _secondaryDao;
    
    @Inject(adapter=DeploymentPlanner.class)
    private Adapters<DeploymentPlanner> _planners;
    private boolean _useNewNetworking;
    
    Map<VirtualMachine.Type, VirtualMachineGuru<? extends VMInstanceVO>> _vmGurus = new HashMap<VirtualMachine.Type, VirtualMachineGuru<? extends VMInstanceVO>>();
    Map<HypervisorType, HypervisorGuru> _hvGurus = new HashMap<HypervisorType, HypervisorGuru>();
    private StateMachine2<State, VirtualMachine.Event, VMInstanceVO> _stateMachine;
    
    private int _retry;
    private long _nodeId;

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
        
        VirtualMachineProfileImpl<T> vmProfile = new VirtualMachineProfileImpl<T>(vm, template, serviceOffering, owner, params, hyperType);
        
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

        stateTransitTo(vm, Event.OperationSucceeded, null);
        txn.commit();
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Allocation completed for VM: " + vm);
        }
        
        return vm;
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
    public <T extends VMInstanceVO> boolean destroy(T vm, User caller, Account account) throws ResourceUnavailableException {
        try {
            return advanceDestroy(vm, caller, account);
        } catch (OperationTimedoutException e) {
            throw new CloudRuntimeException("Operation timed out", e);
        } catch (ConcurrentOperationException e) {
            throw new CloudRuntimeException("Concurrent operation ", e);
        }
    }
    
    @Override
    public <T extends VMInstanceVO> boolean advanceDestroy(T vm, User caller, Account account) throws ResourceUnavailableException, OperationTimedoutException, ConcurrentOperationException {
        if (vm == null || vm.getState() == State.Destroyed || vm.getState() == State.Expunging || vm.getRemoved() != null) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Unable to find vm or vm is destroyed: " + vm);
            }
            return true;
        }

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Destroying vm " + vm);
        }
        
        if (!stop(vm, caller, account)) {
            s_logger.error("Unable to stop vm so we can't destroy it: " + vm);
            return false;
        }
        
        VirtualMachineGuru<T> guru = getVmGuru(vm);
        vm = guru.findById(vm.getId());

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Destroying vm " + vm);
        }
        if (!stateTransitTo(vm, VirtualMachine.Event.DestroyRequested, vm.getHostId())) {
            s_logger.debug("Unable to destroy the vm because it is not in the correct state: " + vm.toString());
            return false;
        }

        return true;
    }

    @Override
    public boolean start() {
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
        
        _nodeId = _clusterMgr.getId();
        _clusterMgr.registerListener(this);
        _useNewNetworking = Boolean.parseBoolean(configDao.getValue("use.new.networking"));
        
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
    public <T extends VMInstanceVO> T start(T vm, Map<String, Object> params, User caller, Account account, HypervisorType hyperType) throws InsufficientCapacityException, ResourceUnavailableException {
        try {
            return advanceStart(vm, params, caller, account, hyperType);
        } catch (ConcurrentOperationException e) {
            throw new CloudRuntimeException("Unable to start a VM due to concurrent operation", e);
        }
    }

    @Override
    public <T extends VMInstanceVO> T advanceStart(T vm, Map<String, Object> params, User caller, Account account, HypervisorType hyperType) throws InsufficientCapacityException, ConcurrentOperationException, ResourceUnavailableException {
        State state = vm.getState();
        if (state == State.Starting || state == State.Running) {
            s_logger.debug("VM is already started: " + vm);
            return vm;
        }
        
        if (state != State.Stopped) {
            s_logger.debug("VM " + vm + " is not in a state to be started: " + state);
            return null;
        }
        
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Creating actual resources for VM " + vm);
        }
        
        Journal journal = new Journal.LogJournal("Creating " + vm, s_logger);
        
        ItWorkVO work = new ItWorkVO(UUID.randomUUID().toString(), _nodeId, ItWorkVO.Type.Start);
        work = _workDao.persist(work);
        
        ReservationContextImpl context = new ReservationContextImpl(work.getId(), journal, caller, account);
        
        ServiceOfferingVO offering = _offeringDao.findById(vm.getServiceOfferingId());
        VMTemplateVO template = _templateDao.findById(vm.getTemplateId());
        
        DataCenterDeployment plan = new DataCenterDeployment(vm.getDataCenterId(), vm.getPodId(), null, null);
        
        HypervisorGuru hvGuru;
        if (hyperType != null && !hyperType.equals(HypervisorType.None)) {
        	hvGuru = _hvGurus.get(hyperType);
        } else {
        	hvGuru = _hvGurus.get(template.getHypervisorType());
        }
        @SuppressWarnings("unchecked")
        VirtualMachineGuru<T> vmGuru = (VirtualMachineGuru<T>)_vmGurus.get(vm.getType());
        
        
        vm.setReservationId(work.getId());
        
        if (!stateTransitTo(vm, Event.StartRequested, null)) {
            throw new ConcurrentOperationException("Unable to start vm "  + vm + " due to concurrent operations");
        }

        VirtualMachineProfileImpl<T> vmProfile = new VirtualMachineProfileImpl<T>(vm, template, offering, null, params, hyperType);
        
        ExcludeList avoids = new ExcludeList();
        int retry = _retry;
        DeployDestination dest = null;
        while (retry-- != 0) { // It's != so that it can match -1.      	
        	/*this will release resource allocated on dest host*/
        	if (retry < (_retry -1)) {
        		stateTransitTo(vm, Event.OperationRetry, dest.getHost().getId());
        	}
        	
            for (DeploymentPlanner planner : _planners) {
                dest = planner.plan(vmProfile, plan, avoids);
                if (dest != null) {
                    avoids.addHost(dest.getHost().getId());
                    journal.record("Deployment found ", vmProfile, dest);
                    break;
                }
            }
            
            if (dest == null) {
            	stateTransitTo(vm, Event.OperationFailed, null);
                throw new InsufficientServerCapacityException("Unable to create a deployment for " + vmProfile, DataCenter.class, plan.getDataCenterId());
            }
            
            vm.setDataCenterId(dest.getDataCenter().getId());
            vm.setPodId(dest.getPod().getId());         

            try {
                _storageMgr.prepare(vmProfile, dest);
                _networkMgr.prepare(vmProfile, dest, context);
            } catch (ConcurrentOperationException e) {
            	stateTransitTo(vm, Event.OperationFailed, dest.getHost().getId());
                throw e;
            } catch (ResourceUnavailableException e) {
                s_logger.warn("Unable to contact storage.", e);
                avoids.addCluster(dest.getCluster().getId());
                continue;
            } catch (InsufficientCapacityException e) {
                s_logger.warn("Insufficient capacity ", e);
                avoids.add(e);
            }
            
            vmGuru.finalizeVirtualMachineProfile(vmProfile, dest, context);
            
            VirtualMachineTO vmTO = hvGuru.implement(vmProfile);
            
            Commands cmds = new Commands(OnError.Revert);
            cmds.addCommand(new Start2Command(vmTO));
            
            vmGuru.finalizeDeployment(cmds, vmProfile, dest, context);
            try {
                Answer[] answers = _agentMgr.send(dest.getHost().getId(), cmds);
                if (answers[0].getResult() && vmGuru.finalizeStart(cmds, vmProfile, dest, context)) {
                    if (!stateTransitTo(vm, Event.OperationSucceeded, dest.getHost().getId())) {
                        throw new CloudRuntimeException("Unable to transition to a new state.");
                    }
                    return vm;
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
        
        stateTransitTo(vm, Event.OperationFailed, dest.getHost().getId());
        
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Creation complete for VM " + vm);
        }
        
        return null;
    }
    
    @Override
    public <T extends VMInstanceVO> boolean stop(T vm, User user, Account account) throws ResourceUnavailableException {
        try {
            return advanceStop(vm, user, account);
        } catch (OperationTimedoutException e) {
            throw new ResourceUnavailableException("Unable to stop vm because the operation to stop timed out", e);
        } catch (ConcurrentOperationException e) {
            throw new CloudRuntimeException("Unable to stop vm because of a concurrent operation", e);
        }
    }

    @Override
    public <T extends VMInstanceVO> boolean advanceStop(T vm, User user, Account account) throws AgentUnavailableException, OperationTimedoutException, ConcurrentOperationException {
        State state = vm.getState();
        if (state == State.Stopped) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("VM is already stopped: " + vm);
            }
            return true;
        }
        
        if (state == State.Creating || state == State.Destroyed || state == State.Expunging || state == State.Error) {
            s_logger.warn("Stopped called on " + vm.toString() + " but the state is " + state.toString());
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

        EventVO event = new EventVO();
        event.setUserId(user.getId());
        event.setAccountId(vm.getAccountId());
        event.setType(EventTypes.EVENT_VM_STOP);
        event.setStartId(1); // FIXME:
        event.setParameters("id="+vm.getId() + "\n" + "vmName=" + vm.getName() + "\nsoId=" + vm.getServiceOfferingId() + "\ntId=" + vm.getTemplateId() + "\ndcId=" + vm.getDataCenterId());

        StopCommand stop = new StopCommand(vm, vm.getInstanceName(), null);

        boolean stopped = false;
        try {
            StopAnswer answer = (StopAnswer)_agentMgr.send(vm.getHostId(), stop);
            stopped = answer.getResult();
            if (!stopped) {
                throw new CloudRuntimeException("Unable to stop the virtual machine due to " + answer.getDetails());
            }
        } finally {
            if (!stopped) {
            	stateTransitTo(vm, Event.OperationFailed, vm.getHostId());
            }
        }
        
        if (s_logger.isDebugEnabled()) {
            s_logger.debug(vm + " is stopped on the host.  Proceeding to release resource held.");
        }
        
        boolean cleanup = false;
        
        VirtualMachineProfile<T> profile = new VirtualMachineProfileImpl<T>(vm);
        try {
            _networkMgr.release(profile);
        } catch (Exception e) {
            s_logger.warn("Unable to release some network resources.", e);
            cleanup = true;
        }
        
        try {
            _storageMgr.release(profile);
        } catch (Exception e) {
            s_logger.warn("Unable to release storage resources.", e);
            cleanup = true;
        }
         
        @SuppressWarnings("unchecked")
        VirtualMachineGuru<T> guru = (VirtualMachineGuru<T>)_vmGurus.get(vm.getType());
        try {
            guru.finalizeStop(profile, vm.getHostId(), vm.getReservationId());
        } catch (Exception e) {
            s_logger.warn("Guru " + guru.getClass() + " has trouble processing stop ");
            cleanup = true;
        }
            
        vm.setReservationId(null);
        
        stateTransitTo(vm, Event.OperationSucceeded, null);

        if (cleanup) {
            ItWorkVO work = new ItWorkVO(reservationId, _nodeId, Type.Cleanup);
            _workDao.persist(work);
        }
        
        return stopped;
    }
    
    @Override
    public void onManagementNodeJoined(List<ManagementServerHostVO> nodeList, long selfNodeId) {
        
    }
    
    @Override
    public void onManagementNodeLeft(List<ManagementServerHostVO> nodeList, long selfNodeId) {
    }
    
    private void setStateMachine() {
    	_stateMachine = new StateMachine2<State, VirtualMachine.Event, VMInstanceVO>();

    	_stateMachine.addTransition(null, VirtualMachine.Event.CreateRequested, State.Creating);
    	_stateMachine.addTransition(State.Creating, VirtualMachine.Event.OperationSucceeded, State.Stopped);
    	_stateMachine.addTransition(State.Creating, VirtualMachine.Event.OperationFailed, State.Error);
    	_stateMachine.addTransition(State.Stopped, VirtualMachine.Event.StartRequested, State.Starting);
    	_stateMachine.addTransition(State.Error, VirtualMachine.Event.DestroyRequested, State.Expunging);
    	_stateMachine.addTransition(State.Stopped, VirtualMachine.Event.DestroyRequested, State.Destroyed);
    	_stateMachine.addTransition(State.Stopped, VirtualMachine.Event.StopRequested, State.Stopped);
    	_stateMachine.addTransition(State.Stopped, VirtualMachine.Event.AgentReportStopped, State.Stopped);
    	_stateMachine.addTransition(State.Starting, VirtualMachine.Event.OperationRetry, State.Starting);
    	_stateMachine.addTransition(State.Starting, VirtualMachine.Event.OperationSucceeded, State.Running);
    	_stateMachine.addTransition(State.Starting, VirtualMachine.Event.OperationFailed, State.Stopped);
    	_stateMachine.addTransition(State.Starting, VirtualMachine.Event.AgentReportRunning, State.Running);
    	_stateMachine.addTransition(State.Starting, VirtualMachine.Event.AgentReportStopped, State.Stopped);
    	_stateMachine.addTransition(State.Destroyed, VirtualMachine.Event.RecoveryRequested, State.Stopped);
    	_stateMachine.addTransition(State.Destroyed, VirtualMachine.Event.ExpungeOperation, State.Expunging);
    	_stateMachine.addTransition(State.Creating, VirtualMachine.Event.MigrationRequested, State.Destroyed);
    	_stateMachine.addTransition(State.Running, VirtualMachine.Event.MigrationRequested, State.Migrating);
    	_stateMachine.addTransition(State.Running, VirtualMachine.Event.AgentReportRunning, State.Running);
    	_stateMachine.addTransition(State.Running, VirtualMachine.Event.AgentReportStopped, State.Stopped);
    	_stateMachine.addTransition(State.Running, VirtualMachine.Event.StopRequested, State.Stopping);
    	_stateMachine.addTransition(State.Migrating, VirtualMachine.Event.MigrationRequested, State.Migrating);
    	_stateMachine.addTransition(State.Migrating, VirtualMachine.Event.OperationSucceeded, State.Running);
    	_stateMachine.addTransition(State.Migrating, VirtualMachine.Event.OperationFailed, State.Running);
    	_stateMachine.addTransition(State.Migrating, VirtualMachine.Event.MigrationFailedOnSource, State.Running);
    	_stateMachine.addTransition(State.Migrating, VirtualMachine.Event.MigrationFailedOnDest, State.Running);
    	_stateMachine.addTransition(State.Migrating, VirtualMachine.Event.AgentReportRunning, State.Running);
    	_stateMachine.addTransition(State.Migrating, VirtualMachine.Event.AgentReportStopped, State.Stopped);
    	_stateMachine.addTransition(State.Stopping, VirtualMachine.Event.OperationSucceeded, State.Stopped);
    	_stateMachine.addTransition(State.Stopping, VirtualMachine.Event.OperationFailed, State.Running);
    	_stateMachine.addTransition(State.Stopping, VirtualMachine.Event.AgentReportRunning, State.Running);
    	_stateMachine.addTransition(State.Stopping, VirtualMachine.Event.AgentReportStopped, State.Stopped);
    	_stateMachine.addTransition(State.Stopping, VirtualMachine.Event.StopRequested, State.Stopping);
    	_stateMachine.addTransition(State.Expunging, VirtualMachine.Event.OperationFailed, State.Expunging);
    	_stateMachine.addTransition(State.Expunging, VirtualMachine.Event.ExpungeOperation, State.Expunging);
    	
    	_stateMachine.registerListener(new VMStateListener(_capacityDao, _offeringDao));
    }
    
    @Override
    public boolean stateTransitTo(VMInstanceVO vm, VirtualMachine.Event e, Long id) {
    	if (_useNewNetworking) {
    		if (vm instanceof UserVmVO) {
    			return _stateMachine.transitTO(vm, e, id, _userVmDao);
    		} else if (vm instanceof ConsoleProxyVO) {
    			return _stateMachine.transitTO(vm, e, id, _consoleDao);
    		} else if (vm instanceof SecondaryStorageVmVO) {
    			return _stateMachine.transitTO(vm, e, id, _secondaryDao);
    		} else if (vm instanceof DomainRouterVO) {
    			return _stateMachine.transitTO(vm, e, id, _routerDao);
    		} else {
    			return _stateMachine.transitTO(vm, e, id, _vmDao);
    		}
    	} else {
    		if (vm instanceof UserVmVO) {
    			return _userVmDao.updateIf((UserVmVO)vm, e, id);
    		} else if (vm instanceof ConsoleProxyVO) {
    			return _consoleDao.updateIf((ConsoleProxyVO)vm, e, id);
    		} else if (vm instanceof SecondaryStorageVmVO) {
    			return _secondaryDao.updateIf((SecondaryStorageVmVO)vm, e, id);
    		} else if (vm instanceof DomainRouterVO) {
    			return _routerDao.updateIf((DomainRouterVO)vm, e, id);
    		} else {
    			return _vmDao.updateIf(vm, e, id);
    		}
    	}
    }
}
