// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

package com.cloud.vm;

import java.net.URI;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.ejb.Local;
import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;

import org.apache.cloudstack.affinity.dao.AffinityGroupVMMapDao;
import org.apache.cloudstack.config.ConfigDepot;
import org.apache.cloudstack.config.ConfigKey;
import org.apache.cloudstack.config.ConfigValue;
import org.apache.cloudstack.config.Configurable;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.engine.service.api.OrchestrationService;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreManager;
import org.apache.cloudstack.engine.subsystem.api.storage.StoragePoolAllocator;
import org.apache.cloudstack.framework.jobs.AsyncJob;
import org.apache.cloudstack.framework.jobs.AsyncJobExecutionContext;
import org.apache.cloudstack.framework.jobs.AsyncJobManager;
import org.apache.cloudstack.framework.jobs.Outcome;
import org.apache.cloudstack.framework.jobs.impl.OutcomeImpl;
import org.apache.cloudstack.framework.messagebus.MessageBus;
import org.apache.cloudstack.framework.messagebus.MessageDispatcher;
import org.apache.cloudstack.framework.messagebus.MessageHandler;
import org.apache.cloudstack.jobs.JobInfo;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.cloudstack.vm.jobs.VmWorkJobDao;
import org.apache.cloudstack.vm.jobs.VmWorkJobVO;
import org.apache.cloudstack.vm.jobs.VmWorkJobVO.Step;

import com.cloud.agent.AgentManager;
import com.cloud.agent.AgentManager.OnError;
import com.cloud.agent.Listener;
import com.cloud.agent.api.AgentControlAnswer;
import com.cloud.agent.api.AgentControlCommand;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.CheckVirtualMachineAnswer;
import com.cloud.agent.api.CheckVirtualMachineCommand;
import com.cloud.agent.api.ClusterSyncAnswer;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.MigrateAnswer;
import com.cloud.agent.api.MigrateCommand;
import com.cloud.agent.api.PingRoutingCommand;
import com.cloud.agent.api.PlugNicAnswer;
import com.cloud.agent.api.PlugNicCommand;
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
import com.cloud.agent.api.UnPlugNicAnswer;
import com.cloud.agent.api.UnPlugNicCommand;
import com.cloud.agent.api.to.NicTO;
import com.cloud.agent.api.to.VirtualMachineTO;
import com.cloud.agent.manager.Commands;
import com.cloud.agent.manager.allocator.HostAllocator;
import com.cloud.alert.AlertManager;
import com.cloud.dc.ClusterDetailsDao;
import com.cloud.dc.ClusterDetailsVO;
import com.cloud.dc.ClusterVO;
import com.cloud.dc.DataCenter;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.HostPodVO;
import com.cloud.deploy.DataCenterDeployment;
import com.cloud.deploy.DeployDestination;
import com.cloud.deploy.DeploymentPlan;
import com.cloud.deploy.DeploymentPlanner;
import com.cloud.deploy.DeploymentPlanner.ExcludeList;
import com.cloud.deploy.DeploymentPlanningManager;
import com.cloud.exception.AffinityConflictException;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.ConnectionException;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InsufficientServerCapacityException;
import com.cloud.exception.InsufficientVirtualNetworkCapcityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ManagementServerException;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.exception.VirtualMachineMigrationException;
import com.cloud.ha.HighAvailabilityManager;
import com.cloud.ha.HighAvailabilityManager.WorkType;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.Status;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.hypervisor.HypervisorGuru;
import com.cloud.hypervisor.HypervisorGuruManager;
import com.cloud.network.Network;
import com.cloud.network.NetworkManager;
import com.cloud.network.NetworkModel;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.rules.RulesManager;
import com.cloud.offering.ServiceOffering;
import com.cloud.org.Cluster;
import com.cloud.resource.ResourceManager;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.storage.DiskOfferingVO;
import com.cloud.storage.GuestOSVO;
import com.cloud.storage.GuestOsCategory;
import com.cloud.storage.Storage.ImageFormat;
import com.cloud.storage.StoragePool;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.Volume;
import com.cloud.storage.Volume.Type;
import com.cloud.storage.VolumeManager;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.StoragePoolHostDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.user.Account;
import com.cloud.user.User;
import com.cloud.utils.DateUtil;
import com.cloud.utils.Journal;
import com.cloud.utils.Pair;
import com.cloud.utils.Predicate;
import com.cloud.utils.StringUtils;
import com.cloud.utils.Ternary;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.concurrency.NamedThreadFactory;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.EntityManager;
import com.cloud.utils.db.GlobalLock;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.exception.ExecutionException;
import com.cloud.utils.fsm.NoTransitionException;
import com.cloud.utils.fsm.StateMachine;
import com.cloud.utils.fsm.StateMachine2;
import com.cloud.vm.VirtualMachine.Event;
import com.cloud.vm.VirtualMachine.PowerState;
import com.cloud.vm.VirtualMachine.State;
import com.cloud.vm.dao.NicDao;
import com.cloud.vm.dao.UserVmDetailsDao;
import com.cloud.vm.dao.VMInstanceDao;
import com.cloud.vm.snapshot.VMSnapshotManager;

@Local(value = VirtualMachineManager.class)
public class VirtualMachineManagerImpl extends ManagerBase implements VirtualMachineManager, Listener, Configurable {
    private static final Logger s_logger = Logger.getLogger(VirtualMachineManagerImpl.class);

    private static final String VM_SYNC_ALERT_SUBJECT = "VM state sync alert";
    
    protected static final ConfigKey<Integer> StartRetry = new ConfigKey<Integer>(
            Integer.class, "start.retry", "Advanced", OrchestrationService.class, "10", "Number of times to retry create and start commands", true, null);
    protected static final ConfigKey<Long> VmOpWaitInterval = new ConfigKey<Long>(
            Long.class, "vm.op.wait.interval", "Advanced", OrchestrationService.class, "120", "Time (in seconds) to wait before checking if a previous operation has succeeded",
            true, null);
    protected static final ConfigKey<Integer> VmOpLockStateRetry = new ConfigKey<Integer>(
            Integer.class, "vm.op.lock.state.retry", "Advanced", OrchestrationService.class, "5", "Times to retry locking the state of a VM for operations",
            true, "-1 means try forever");
    protected static final ConfigKey<Long> VmOpCleanupInterval = new ConfigKey<Long>(
            Long.class, "vm.op.cleanup.interval", "Advanced", OrchestrationService.class, "86400", "Interval to run the thread that cleans up the vm operations (in seconds)",
            false, "Seconds");
    protected static final ConfigKey<Long> VmOpCleanupWait = new ConfigKey<Long>(
            Long.class, "vm.op.cleanup.wait", "Advanced", OrchestrationService.class, "3600", "Time (in seconds) to wait before cleanuping up any vm work items", false, "Seconds");
    protected static final ConfigKey<Integer> VmOpCancelInterval = new ConfigKey<Integer>(
            Integer.class, "vm.op.cancel.interval", "Advanced", OrchestrationService.class, "3600", "Time (in seconds) to wait before cancelling a operation", false, "Seconds");
    protected static final ConfigKey<Integer> Wait = new ConfigKey<Integer>(
            Integer.class, "wait", "Advanced", OrchestrationService.class, "1800", "Time in seconds to wait for control commands to return", false, null);
    protected static final ConfigKey<Boolean> VmDestroyForceStop = new ConfigKey<Boolean>(
            Boolean.class, "vm.destroy.forcestop", "Advanced", OrchestrationService.class, "false", "On destroy, force-stop takes this value ", true, null);

    // New
    protected static final ConfigKey<Long> VmJobCheckInterval = new ConfigKey<Long>(
            Long.class, "vm.job.check.interval", "VM Orchestration", OrchestrationService.class, "3000", "Interval in milliseconds to check if the job is complete", true,
            "Milliseconds");
    protected static final ConfigKey<Long> VmJobTimeout = new ConfigKey<Long>(
            Long.class, "vm.job.timeout", "VM Orchestration", OrchestrationService.class, "600000", "Time in milliseconds to wait before attempting to cancel a job", true,
            "Milliseconds");
    protected static final ConfigKey<Long> PingInterval = new ConfigKey<Long>(
            Long.class, "ping.interval", "Advanced", OrchestrationService.class, "60", "Ping interval in seconds", false, null);

    protected static final StateMachine<Step, VirtualMachine.Event> MigrationStateMachine = new StateMachine<Step, VirtualMachine.Event>();
    static {
        MigrationStateMachine.addTransition(Step.Filed, VirtualMachine.Event.MigrationRequested, Step.Prepare);
        MigrationStateMachine.addTransition(Step.Prepare, VirtualMachine.Event.OperationSucceeded, Step.Migrating);
        MigrationStateMachine.addTransition(Step.Prepare, VirtualMachine.Event.OperationFailed, Step.Error);
        MigrationStateMachine.addTransition(Step.Migrating, VirtualMachine.Event.OperationSucceeded, Step.Started);
        MigrationStateMachine.addTransition(Step.Migrating, VirtualMachine.Event.OperationFailed, Step.Error);
        MigrationStateMachine.addTransition(Step.Started, VirtualMachine.Event.OperationSucceeded, Step.Done);
        MigrationStateMachine.addTransition(Step.Started, VirtualMachine.Event.OperationFailed, Step.Error);
    }

    @Inject
    protected EntityManager _entityMgr;
    @Inject
    ConfigDepot _configDepot;
    @Inject
    DataStoreManager _dataStoreMgr;
    @Inject
    protected NetworkManager _networkMgr;
    @Inject
    protected NetworkModel _networkModel;
    @Inject
    protected AgentManager _agentMgr;
    @Inject
    protected VMInstanceDao _vmDao;
    @Inject
    protected NicDao _nicsDao;
    @Inject
    protected HostDao _hostDao;
    @Inject
    protected AlertManager _alertMgr;
    @Inject
    protected VolumeDao _volsDao;
    @Inject
    protected HighAvailabilityManager _haMgr;
    @Inject
    protected HypervisorGuruManager _hvGuruMgr;
    @Inject
    protected StoragePoolHostDao _poolHostDao;
    @Inject
    protected RulesManager _rulesMgr;
    @Inject
    protected AffinityGroupVMMapDao _affinityGroupVMMapDao;

    @Inject
    protected List<DeploymentPlanner> _planners;

    protected List<HostAllocator> _hostAllocators;

    public List<HostAllocator> getHostAllocators() {
        return _hostAllocators;
    }

    public void setHostAllocators(List<HostAllocator> hostAllocators) {
        _hostAllocators = hostAllocators;
    }

	@Inject
    protected List<StoragePoolAllocator> _storagePoolAllocators;

    @Inject
    protected ResourceManager _resourceMgr;
    
    @Inject
    protected VMSnapshotManager _vmSnapshotMgr = null;
    @Inject
    protected ClusterDetailsDao  _clusterDetailsDao;
    @Inject
    protected UserVmDetailsDao _uservmDetailsDao;
    
    @Inject
    VolumeManager _volumeMgr;
    
    @Inject protected MessageBus _messageBus;
    @Inject protected VirtualMachinePowerStateSync _syncMgr;
    @Inject protected VmWorkJobDao _workJobDao;
    @Inject protected AsyncJobManager _jobMgr;
    @Inject
    DeploymentPlanningManager _dpMgr;

    Map<VirtualMachine.Type, VirtualMachineGuru> _vmGurus = new HashMap<VirtualMachine.Type, VirtualMachineGuru>();
    protected StateMachine2<State, VirtualMachine.Event, VirtualMachine> _stateMachine;

    ScheduledExecutorService _executor = null;

    protected ConfigValue<Integer> _retry;
    protected ConfigValue<Integer> _cancelWait;
    protected ConfigValue<Long> _cleanupWait;
    protected ConfigValue<Long> _cleanupInterval;
    protected ConfigValue<Long> _opWaitInterval;
    protected ConfigValue<Integer> _lockStateRetry;
    protected ConfigValue<Integer> _operationTimeout;
    protected ConfigValue<Boolean> _forceStop;
    protected ConfigValue<Long> _pingInterval;
    protected ConfigValue<Long> _jobCheckInterval;
    protected ConfigValue<Long> _jobTimeout;

    protected long _nodeId;

    SearchBuilder<VolumeVO> RootVolumeSearch;

    @Override
    public void registerGuru(VirtualMachine.Type type, VirtualMachineGuru guru) {
        synchronized (_vmGurus) {
            _vmGurus.put(type, guru);
        }
    }
    
    private VirtualMachineGuru getVmGuru(VirtualMachine vm) {
        return _vmGurus.get(vm.getType());
    }

    @Override
    @DB
    public boolean allocate(String vmInstanceName, VMTemplateVO template, ServiceOfferingVO serviceOffering, Pair<? extends DiskOfferingVO, Long> rootDiskOffering,
            List<Pair<DiskOfferingVO, Long>> dataDiskOfferings, List<Pair<NetworkVO, NicProfile>> networks, Map<VirtualMachineProfile.Param, Object> params, DeploymentPlan plan,
            HypervisorType hyperType, Account owner) {
        assert (plan.getClusterId() == null && plan.getPoolId() == null) : "We currently don't support cluster and pool preset yet";

        VMInstanceVO vm = _vmDao.findVMByInstanceName(vmInstanceName);
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Allocating entries for VM: " + vm);
        }

        VirtualMachineProfileImpl vmProfile = new VirtualMachineProfileImpl(vm, template, serviceOffering, owner, params);

        vm.setDataCenterId(plan.getDataCenterId());
        if (plan.getPodId() != null) {
            vm.setPodId(plan.getPodId());
        }

        Transaction txn = Transaction.currentTxn();
        txn.start();
        _vmDao.update(vm.getId(), vm);

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Allocating nics for " + vm);
        }

        try {
            _networkMgr.allocate(vmProfile, networks);
        } catch (ConcurrentOperationException e) {
            throw new CloudRuntimeException("Concurrent operation while trying to allocate resources for the VM", e);
        } catch (InsufficientCapacityException e) {
            throw new CloudRuntimeException("Insufficient Capacity to create a vm ", e);
        }

        if (dataDiskOfferings == null) {
            dataDiskOfferings = new ArrayList<Pair<DiskOfferingVO, Long>>(0);
        }

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Allocating disks for " + vm);
        }

        if (template.getFormat() == ImageFormat.ISO) {
            _volumeMgr.allocateRawVolume(Type.ROOT, "ROOT-" + vm.getId(), rootDiskOffering.first(), rootDiskOffering.second(), vm, owner);
        } else if (template.getFormat() == ImageFormat.BAREMETAL) {
            // Do nothing
        } else {
            _volumeMgr.allocateTemplatedVolume(Type.ROOT, "ROOT-" + vm.getId(), rootDiskOffering.first(), template, vm, owner);
        }

        for (Pair<DiskOfferingVO, Long> offering : dataDiskOfferings) {
            _volumeMgr.allocateRawVolume(Type.DATADISK, "DATA-" + vm.getId(), offering.first(), offering.second(), vm, owner);
        }

        txn.commit();
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Allocation completed for VM: " + vm);
        }

        return true;
    }

    @Override
    public boolean allocate(String vmInstanceName, VMTemplateVO template, ServiceOfferingVO serviceOffering, List<Pair<NetworkVO, NicProfile>> networks,
            DeploymentPlan plan, HypervisorType hyperType, Account owner) {
        return allocate(vmInstanceName, template, serviceOffering, new Pair<DiskOfferingVO, Long>(serviceOffering, null), null, networks, null, plan, hyperType, owner);
    }

    @Override
    public void expunge(String vmUuid) {
        try {
            advanceExpunge(vmUuid);
        } catch (OperationTimedoutException e) {
            throw new CloudRuntimeException("Operation timed out", e);
        } catch (ConcurrentOperationException e) {
            throw new CloudRuntimeException("Concurrent operation ", e);
        } catch (ResourceUnavailableException e) {
            throw new CloudRuntimeException("Resource is unavailable ", e);
        }
    }

    @Override
    public void advanceExpunge(String vmUuid) throws ResourceUnavailableException, OperationTimedoutException,
            ConcurrentOperationException {
        VMInstanceVO vm = _vmDao.findByUuid(vmUuid);
        if (vm == null || vm.getRemoved() != null) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Unable to find vm or vm is destroyed: " + vm);
            }
            return;
        }

        stop(vmUuid, false);

        vm = _vmDao.findByUuid(vmUuid);
        try {
            if (!stateTransitTo(vm, VirtualMachine.Event.ExpungeOperation, vm.getHostId())) {
                throw new CloudRuntimeException("Unable to detroy the VM because it is not in the correct state: " + vm);
            }
        } catch (NoTransitionException e) {
            throw new CloudRuntimeException("Unable to destroy the vm because it is not in the correct state: " + vm);
        }

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Destroying vm " + vm);
        }

        VirtualMachineProfile profile = new VirtualMachineProfileImpl(vm);
        s_logger.debug("Cleaning up NICS");
        _networkMgr.cleanupNics(profile);
        // Clean up volumes based on the vm's instance id
        _volumeMgr.cleanupVolumes(vm.getId());

        VirtualMachineGuru guru = getVmGuru(vm);
        guru.finalizeExpunge(vm);
        //remove the overcommit detials from the uservm details
        _uservmDetailsDao.deleteDetails(vm.getId());

        // send hypervisor-dependent commands before removing
        HypervisorGuru hvGuru = _hvGuruMgr.getGuru(vm.getHypervisorType());
        List<Command> finalizeExpungeCommands = hvGuru.finalizeExpunge(vm);
        if(finalizeExpungeCommands != null && finalizeExpungeCommands.size() > 0){
            Long hostId = vm.getHostId() != null ? vm.getHostId() : vm.getLastHostId();
            if(hostId != null){
                Commands cmds = new Commands(OnError.Stop);
                for (Command command : finalizeExpungeCommands) {
                    cmds.addCommand(command);
                }
                _agentMgr.send(hostId, cmds);
                if(!cmds.isSuccessful()){
                    for (Answer answer : cmds.getAnswers()){
                        if(answer != null && !answer.getResult()){
                            s_logger.warn("Failed to expunge vm due to: " + answer.getDetails());
                            break;
                        }
                    }
                    throw new CloudRuntimeException("Failed the expunge the vm on the hypervisor");
                }
            }
        }

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Expunged " + vm);
        }
        
        _vmDao.remove(vm.getId());
    }
    
    @Override
    public boolean start() {
        _executor.scheduleAtFixedRate(new TransitionTask(), _pingInterval.value(), _pingInterval.value(), TimeUnit.SECONDS);
        _executor.scheduleAtFixedRate(new CleanupTask(), _pingInterval.value()*2, _pingInterval.value()*2, TimeUnit.SECONDS);

        // cancel jobs left-over from last run
        cancelWorkItems(_nodeId);
        
        return true;
    }
 
    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public boolean configure(String name, Map<String, Object> xmlParams) throws ConfigurationException {
        _retry = _configDepot.get(StartRetry);

        _pingInterval = _configDepot.get(PingInterval).setMultiplier(1000);
        _cancelWait = _configDepot.get(VmOpCancelInterval);
        _cleanupWait = _configDepot.get(VmOpCleanupWait);
        _cleanupInterval = _configDepot.get(VmOpCleanupInterval).setMultiplier(1000);
        _opWaitInterval = _configDepot.get(VmOpWaitInterval).setMultiplier(1000);
        _lockStateRetry = _configDepot.get(VmOpLockStateRetry);
        _operationTimeout = _configDepot.get(Wait).setMultiplier(2);
        _forceStop = _configDepot.get(VmDestroyForceStop);
        _jobCheckInterval = _configDepot.get(VmJobCheckInterval);
        _jobTimeout = _configDepot.get(VmJobTimeout);

        ReservationContextImpl.setComponents(_entityMgr);
        VirtualMachineProfileImpl.setComponents(_entityMgr);
        VmWorkMigrate.init(_entityMgr);

        _executor = Executors.newScheduledThreadPool(1, new NamedThreadFactory("Vm-Operations-Cleanup"));

        _agentMgr.registerForHostEvents(this, true, true, true);
        
        RootVolumeSearch = _entityMgr.createSearchBuilder(VolumeVO.class);
        VolumeVO rvsEntity = RootVolumeSearch.entity();
        RootVolumeSearch.and(rvsEntity.getVolumeType(), SearchCriteria.Op.EQ).values(Volume.Type.ROOT)
                .and(rvsEntity.getState(), SearchCriteria.Op.EQ).values(Volume.State.Ready)
                .and(rvsEntity.getInstanceId(), SearchCriteria.Op.EQ, "instance")
                .and(rvsEntity.getDeviceId(), SearchCriteria.Op.EQ).values(0)
                .done();

        
        _messageBus.subscribe(Topics.VM_POWER_STATE, MessageDispatcher.getDispatcher(this));

        return true;
    }

    protected VirtualMachineManagerImpl() {
        setStateMachine();
    }

    @Override
    public void easyStart(String vmUuid, Map<VirtualMachineProfile.Param, Object> params) {
        easyStart(vmUuid, params, null);
    }

    @Override
    public void easyStart(String vmUuid, Map<VirtualMachineProfile.Param, Object> params, DeploymentPlan planToDeploy) {
        Outcome<VirtualMachine> outcome = start(vmUuid, params, planToDeploy);
        try {
            outcome.get(_jobTimeout.value(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            // FIXME: What to do
        } catch (java.util.concurrent.ExecutionException e) {
            // FIXME: What to do
        } catch (TimeoutException e) {
            // FIXME: What to do
        }
    }

    /*
        protected boolean checkWorkItems(VMInstanceVO vm, State state) throws ConcurrentOperationException {
            while (true) {
                VmWorkJobVO vo = _workDao.findByOutstandingWork(vm.getId(), state);
                if (vo == null) {
                    if (s_logger.isDebugEnabled()) {
                        s_logger.debug("Unable to find work for VM: " + vm + " and state: " + state);
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
    */
    
    /*
        @DB
        protected VirtualMachineernary<T, ReservationContext, VmWorkJobVO> changeToStartState(VirtualMachineGuru<T> vmGuru, String vmUuid, User caller, Account account)
                throws ConcurrentOperationException {
            long vmId = vm.getId();

            VmWorkJobVO work = new VmWorkJobVO(UUID.randomUUID().toString(), _nodeId, State.Starting, vm.getType(), vm.getId());
            int retry = _lockStateRetry;
            while (retry-- != 0) {
                Transaction txn = Transaction.currentTxn();
                Ternary<T, ReservationContext, VmWorkJobVO> result = null;
                txn.start();
                try {
                    Journal journal = new Journal.LogJournal("Creating " + vm, s_logger);
                    work = _workDao.persist(work);
                    ReservationContextImpl context = new ReservationContextImpl(work.getId(), journal, caller, account);

                    if (stateTransitTo(vm, VirtualMachine.Event.StartRequested, null, work.getId())) {
                        if (s_logger.isDebugEnabled()) {
                            s_logger.debug("Successfully transitioned to start state for " + vm + " reservation id = " + work.getId());
                        }
                        result = new Ternary<T, ReservationContext, VmWorkJobVO>(vmGuru.findById(vmId), context, work);
                        txn.commit();
                        return result;
                    }
                } catch (NoTransitionException e) {
                    if (s_logger.isDebugEnabled()) {
                        s_logger.debug("Unable to transition into Starting state due to " + e.getMessage());
                    }
                } finally {
                    if (result == null) {
                        txn.rollback();
                    }
                }

                VMInstanceVO instance = _vmDao.findById(vmId);
                if (instance == null) {
                    throw new ConcurrentOperationException("Unable to acquire lock on " + vm);
                }

                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Determining why we're unable to update the state to Starting for " + instance + ".  Retry=" + retry);
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
                        throw new ConcurrentOperationException("There are concurrent operations on " + vm);
                    } else {
                        continue;
                    }
                }

                if (state != State.Stopped) {
                    s_logger.debug("VM " + vm + " is not in a state to be started: " + state);
                    return null;
                }
            }

            throw new ConcurrentOperationException("Unable to change the state of " + vm);
        }
    */

    @DB
    protected Ternary<VMInstanceVO, ReservationContext, VmWorkJobVO> changeToStartState(VirtualMachineGuru vmGuru, VMInstanceVO vm, User caller, Account account)
        throws ConcurrentOperationException {
    	
        Ternary<VMInstanceVO, ReservationContext, VmWorkJobVO> result = null;
        Transaction txn = Transaction.currentTxn();
        txn.start();
        try {
	        VmWorkJobVO work = _workJobDao.findById(AsyncJobExecutionContext.getCurrentExecutionContext().getJob().getId());
	        
	        Journal journal = new Journal.LogJournal("Creating " + vm, s_logger);
	        ReservationContextImpl context = new ReservationContextImpl(work.getUuid(), journal, caller, account);
            if (stateTransitTo(vm, VirtualMachine.Event.StartRequested, null, work.getUuid())) {
	            if (s_logger.isDebugEnabled()) {
	                s_logger.debug("Successfully transitioned to start state for " + vm + " reservation id = " + work.getId());
	            }
                result = new Ternary<VMInstanceVO, ReservationContext, VmWorkJobVO>(_vmDao.findById(vm.getId()), context, work);
	            txn.commit();
	            return result;
	        }
        } catch (NoTransitionException e) {
            s_logger.warn("Unable to transition into Starting state due to " + e.getMessage());
        } finally {
        	if(result == null)
        		txn.rollback();
        }
        
        throw new ConcurrentOperationException("Unable to change the state of " + vm);
    }
    
    @DB
    protected boolean changeState(VMInstanceVO vm, Event event, Long hostId, VmWorkJobVO work, Step step) throws NoTransitionException {
        VmWorkJobVO.Step previousStep = work.getStep();
        
        Transaction txn = Transaction.currentTxn();

        txn.start();
        work.setStep(step);
        boolean result = stateTransitTo(vm, event, hostId);
        if (!result) {
            work.setStep(previousStep);
        }
        _workJobDao.update(work.getId(), work);
        txn.commit();
        return result;
    }
    
    @DB
    protected void changeState2(VMInstanceVO vm, VirtualMachine.Event vmEvent, Long hostId, VmWorkJobVO work, VirtualMachine.Event workEvent) throws NoTransitionException {
        VmWorkJobVO.Step currentStep = work.getStep();
        StateMachine<Step, Event> sm = work.getCmd() == VmWorkJobDispatcher.Migrate ? MigrationStateMachine : null;

        Transaction txn = Transaction.currentTxn();

        txn.start();
        if (vmEvent != null) {
            if (!stateTransitTo(vm, vmEvent, hostId)) {
                throw new NoTransitionException("Unable to transit the vm state");
            }
        }
        work.setStep(sm.getNextState(currentStep, workEvent));
        _workJobDao.update(work.getId(), work);
        txn.commit();
        return;
    }

    protected boolean areAffinityGroupsAssociated(VirtualMachineProfile vmProfile) {
        VirtualMachine vm = vmProfile.getVirtualMachine();
        long vmGroupCount = _affinityGroupVMMapDao.countAffinityGroupsForVm(vm.getId());

        if (vmGroupCount > 0) {
            return true;
        }
        return false;
    }

    @Override
    @DB
    public Outcome<VirtualMachine> start(String vmUuid, Map<VirtualMachineProfile.Param, Object> params, DeploymentPlan planToDeploy) {
        CallContext context = CallContext.current();
        User callingUser = context.getCallingUser();
        Account callingAccount = context.getCallingAccount();

        final VMInstanceVO vm = _vmDao.findByUuid(vmUuid);
    	
    	VmWorkJobVO workJob = null;
    	Transaction txn = Transaction.currentTxn();
        txn.start();

        _vmDao.lockRow(vm.getId(), true);

        List<VmWorkJobVO> pendingWorkJobs = _workJobDao.listPendingWorkJobs(VirtualMachine.Type.Instance, vm.getId(), VmWorkJobDispatcher.Start);

        if (pendingWorkJobs.size() > 0) {
            assert (pendingWorkJobs.size() == 1);
            workJob = pendingWorkJobs.get(0);
        } else {
            workJob = new VmWorkJobVO(context.getContextId());

            workJob.setDispatcher(VmWorkJobDispatcher.VM_WORK_JOB_DISPATCHER);
            workJob.setCmd(VmWorkJobDispatcher.Start);

            workJob.setAccountId(callingAccount.getId());
            workJob.setUserId(callingUser.getId());
            workJob.setStep(VmWorkJobVO.Step.Starting);
            workJob.setVmType(vm.getType());
            workJob.setVmInstanceId(vm.getId());

            // save work context info (there are some duplications)
            VmWorkStart workInfo = new VmWorkStart(callingUser.getId(), callingAccount.getId(), vm.getId());
            workInfo.setPlan(planToDeploy);
            workInfo.setParams(params);
            workJob.setCmdInfo(VmWorkJobDispatcher.serialize(workInfo));

            _jobMgr.submitAsyncJob(workJob, VmWorkJobDispatcher.VM_WORK_QUEUE, vm.getId());
    	}

        txn.commit();
    	final long jobId = workJob.getId();
    	AsyncJobExecutionContext.getCurrentExecutionContext().joinJob(jobId);
        return new VmOutcome(workJob, VirtualMachine.PowerState.PowerOn, vm.getId(), null);
    }

    private Pair<DeploymentPlan, DeployDestination> findDestination(VirtualMachineProfileImpl profile, DeploymentPlan planRequested, boolean reuseVolume,
            ReservationContext reservation, AsyncJobExecutionContext job) throws InsufficientCapacityException, ResourceUnavailableException {
        VirtualMachine vm = profile.getVirtualMachine();

        DataCenterDeployment plan = null;
        if (planRequested != null) {
            plan = new DataCenterDeployment(planRequested);
        } else {
            plan = new DataCenterDeployment(vm.getDataCenterId(), vm.getPodIdToDeployIn(), null, null, null, null, reservation);
        }
        
        job.log(s_logger, "Starting " + vm + " with requested " + plan);

        boolean planChangedByVolume = false;
        List<VolumeVO> vols = _entityMgr.search(VolumeVO.class, RootVolumeSearch.create("instance", vm.getId()));
        
        if (vols.size() > 0 && reuseVolume) {
            // edit plan if this vm's ROOT volume is in READY state already
            
            VolumeVO vol = vols.get(0);
            // make sure if the templateId is unchanged. If it is changed,
            // let planner reassign pool for the volume even if it ready.
            Long volTemplateId = vol.getTemplateId();
            if (vol.isRecreatable() && volTemplateId != null &&
                    vm.getTemplateId() != -1 && volTemplateId.longValue() != vm.getTemplateId()) {
                job.log(s_logger, "Recreating" + vol + " of " + vm + " because its template has changed.");
                plan.setPoolId(null);
            } else {
                StoragePool pool = (StoragePool)_dataStoreMgr.getPrimaryDataStore(vol.getPoolId());
                Long rootVolPodId = pool.getPodId();
                Long rootVolClusterId = pool.getClusterId();
                Long clusterIdSpecified = plan.getClusterId();
                if (clusterIdSpecified != null && rootVolClusterId != null &&
                    clusterIdSpecified.longValue() != rootVolClusterId.longValue()) {
                    job.log(s_logger, "Unable to satisfy the deployment plan because it is requesting cluster " + clusterIdSpecified + " but the root volume is in cluster " + rootVolClusterId);
                    throw new ResourceUnavailableException("Unable to satisfy the deployment plan because it is requesting cluster " + clusterIdSpecified
                            + " but the root volume is in cluster " + rootVolClusterId, Cluster.class, clusterIdSpecified);
                }
                plan.setPoolId(vol.getPoolId());
                plan.setClusterId(rootVolClusterId);
                plan.setPodId(rootVolPodId);
                planChangedByVolume = true;
                job.log(s_logger, "Deployment plan has been adjusted to " + plan);
            }
        }

        DeployDestination dest = null;
        try {
            dest = _dpMgr.planDeployment(profile, plan, plan.getAvoids());
        } catch (AffinityConflictException e2) {
            throw new CloudRuntimeException("Unable to create deployment, affinity rules associted to the VM conflict", e2);
        }

        if (dest == null && planChangedByVolume) {
            job.log(s_logger, "Unable to find a deploy destination using the adjusted deployment plan.  Replanning with ");
            return findDestination(profile, planRequested, false, reservation, job);
        }

        if (dest == null) {
            throw new InsufficientServerCapacityException("Unable to create a deployment for " + vm,
                    DataCenter.class, plan.getDataCenterId(), areAffinityGroupsAssociated(profile));
        }

        plan.getAvoids().addHost(dest.getHost().getId());
        job.log(s_logger, "Final deploy destination: " + dest);
        return new Pair<DeploymentPlan, DeployDestination>(plan, dest);
    }

    /**
     * orchestrateStart orchestrates the vm start process.  Note that this
     * method is not in the interface.
     * 
     * @param vmUuid uuid for the vm
     * @param params additional parameters passed
     * @param planRequested deployment requested
     * @throws InsufficientCapacityException when there's not enough infrastructure capacity to ensure successful start of a vm.
     * @throws ConcurrentOperationException when there are multiple operations on the vm.
     * @throws ResourceUnavailableException when the resource being used to start the vm is not available.
     */
    public void orchestrateStart(String vmUuid, Map<VirtualMachineProfile.Param, Object> params, DeploymentPlan planRequested)
            throws InsufficientCapacityException, ConcurrentOperationException, ResourceUnavailableException {
        CallContext context = CallContext.current();
        User caller = context.getCallingUser();
        Account account = context.getCallingAccount();
        AsyncJobExecutionContext job = AsyncJobExecutionContext.getCurrentExecutionContext();

        VMInstanceVO vm = _vmDao.findByUuid(vmUuid);
        if (vm == null) {
            throw new ConcurrentOperationException("Unable to find vm by " + vmUuid);
        }

        VirtualMachineGuru vmGuru = getVmGuru(vm);
  
        Ternary<VMInstanceVO, ReservationContext, VmWorkJobVO> start = changeToStartState(vmGuru, vm, caller, account);
        assert(start != null);

        ReservationContext reservation = start.second();
        VmWorkJobVO work = start.third();

        VMInstanceVO startedVm = null;
        ServiceOfferingVO offering = _entityMgr.findById(ServiceOfferingVO.class, vm.getServiceOfferingId());
        VMTemplateVO template = _entityMgr.findById(VMTemplateVO.class, vm.getTemplateId());

        HypervisorGuru hvGuru = _hvGuruMgr.getGuru(vm.getHypervisorType());

        boolean canRetry = true;
        try {

            boolean reuseVolume = true;
            DeploymentPlan plan = planRequested;

            int retry = _retry.value();
            while (retry-- != 0) { // It's != so that it can match -1.

                VirtualMachineProfileImpl vmProfile = new VirtualMachineProfileImpl(vm, template, offering, account, params);
                Pair<DeploymentPlan, DeployDestination> result = findDestination(vmProfile, plan, reuseVolume, reservation, job);
                plan = result.first();
                DeployDestination dest = result.second();

                long destHostId = dest.getHost().getId();
                vm.setPodId(dest.getPod().getId());
                Long cluster_id = dest.getCluster().getId();
                ClusterDetailsVO cluster_detail_cpu = _clusterDetailsDao.findDetail(cluster_id, "cpuOvercommitRatio");
                ClusterDetailsVO cluster_detail_ram = _clusterDetailsDao.findDetail(cluster_id, "memoryOvercommitRatio");
                vmProfile.setCpuOvercommitRatio(Float.parseFloat(cluster_detail_cpu.getValue()));
                vmProfile.setMemoryOvercommitRatio(Float.parseFloat(cluster_detail_ram.getValue()));

                if (!changeState(vm, VirtualMachine.Event.OperationRetry, destHostId, work, Step.Prepare)) {
                    throw new ConcurrentOperationException("Unable to update the state of the Virtual Machine");
                }

                try {
                    _networkMgr.prepare(vmProfile, dest, reservation);
                    _volumeMgr.prepare(vmProfile, dest);

                    reuseVolume = true;

                    Commands cmds = null;
                    vmGuru.finalizeVirtualMachineProfile(vmProfile, dest, reservation);

                    VirtualMachineTO vmTO = hvGuru.implement(vmProfile);

                    cmds = new Commands(OnError.Stop);
                    cmds.addCommand(new StartCommand(vmTO, dest.getHost(), true));

                    vmGuru.finalizeDeployment(cmds, vmProfile, dest, reservation);

                    work.setStep(VmWorkJobVO.Step.Starting);
                    _workJobDao.update(work.getId(), work);

                    _agentMgr.send(destHostId, cmds);

                    work.setStep(VmWorkJobVO.Step.Started);
                    _workJobDao.update(work.getId(), work);

                    StartAnswer startAnswer = cmds.getAnswer(StartAnswer.class);
                    if (startAnswer != null && startAnswer.getResult()) {
                        String host_guid = startAnswer.getHost_guid();
                        if( host_guid != null ) {
                            HostVO finalHost = _resourceMgr.findHostByGuid(host_guid);
                            if (finalHost == null ) {
                                throw new CloudRuntimeException("Host Guid " + host_guid + " doesn't exist in DB, something wrong here");
                            }
                            destHostId = finalHost.getId();
                        }
                        if (vmGuru.finalizeStart(vmProfile, destHostId, cmds, reservation)) {
                            if (!changeState(vm, VirtualMachine.Event.OperationSucceeded, destHostId, work, Step.Done)) {
                                throw new ConcurrentOperationException("Unable to transition to a new state.");
                            }
                            startedVm = vm;
                            if (s_logger.isDebugEnabled()) {
                                s_logger.debug("Start completed for VM " + vm);
                            }
                            return;
                        } else {
                            if (s_logger.isDebugEnabled()) {
                                s_logger.info("The guru did not like the answers so stopping " + vm);
                            }

                            StopCommand cmd = new StopCommand(vm, true);
                            StopAnswer answer = (StopAnswer) _agentMgr.easySend(destHostId, cmd);
                            if (answer == null || !answer.getResult()) {
                                s_logger.warn("Unable to stop " + vm + " due to " + (answer != null ? answer.getDetails() : "no answers"));
                                _haMgr.scheduleStop(vm, destHostId, WorkType.ForceStop);
                                throw new ExecutionException("Unable to stop " + vm + " so we are unable to retry the start operation");
                            }
                            throw new ExecutionException("Unable to start " + vm + " due to error in finalizeStart, not retrying");
                        }
                    }
                    s_logger.info("Unable to start VM on " + dest.getHost() + " due to " + (startAnswer == null ? " no start answer" : startAnswer.getDetails()));

                } catch (OperationTimedoutException e) {
                    s_logger.debug("Unable to send the start command to host " + dest.getHost());
                    if (e.isActive()) {
                        _haMgr.scheduleStop(vm, destHostId, WorkType.CheckStop);
                    }
                    canRetry = false;
                    throw new AgentUnavailableException("Unable to start " + vm.getHostName(), destHostId, e);
                } catch (ResourceUnavailableException e) {
                    s_logger.info("Unable to contact resource.", e);
                    if (!plan.getAvoids().add(e)) {
                        if (e.getScope() == Volume.class || e.getScope() == Nic.class) {
                            throw e;
                        } else {
                            s_logger.warn("unexpected ResourceUnavailableException : " + e.getScope().getName(), e);
                            throw e;
                        }
                    }
                } catch (InsufficientCapacityException e) {
                    s_logger.info("Insufficient capacity ", e);
                    if (!plan.getAvoids().add(e)) {
                        if (e.getScope() == Volume.class || e.getScope() == Nic.class) {
                            throw e;
                        } else {
                            s_logger.warn("unexpected InsufficientCapacityException : " + e.getScope().getName(), e);
                        }
                    }
                } catch (Exception e) {
                    s_logger.error("Failed to start instance " + vm, e);
                    throw new CloudRuntimeException("Unable to start instance due to " + e.getMessage(), e);
                } finally {
                    if (startedVm == null && canRetry) {
                        VmWorkJobVO.Step prevStep = work.getStep();
                        _workJobDao.updateStep(work.getId(), VmWorkJobVO.Step.Release);
                        if (prevStep == VmWorkJobVO.Step.Started || prevStep == VmWorkJobVO.Step.Starting) {
                            cleanup(vmGuru, vmProfile, work, false);
                        } else {
                            //if step is not starting/started, send cleanup command with force=true
                            cleanup(vmGuru, vmProfile, work, true);
                        }
                    }
                }
            }
        } catch (NoTransitionException e1) {
            throw new CloudRuntimeException(e1.getMessage());
        } finally {
            if (startedVm == null) {
                try {
                    changeState(vm, VirtualMachine.Event.OperationFailed, null, work, Step.Done);
                } catch (NoTransitionException e) {
                    throw new ConcurrentOperationException(e.getMessage());
                }
            }
        }

        if (startedVm == null) {
            throw new CloudRuntimeException("Unable to start instance '" + vm.getHostName()
                            + "' (" + vm.getUuid() + "), see management server log for details");
        }
    }
    
    @Override
    public void easyStop(String vmUuid) {
        Outcome<VirtualMachine> outcome = stop(vmUuid, false);
        try {
            outcome.get(_jobTimeout.value(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            throw new CloudRuntimeException("Interrupted while stopping vm " + vmUuid, e);
        } catch (java.util.concurrent.ExecutionException e) {
            throw new CloudRuntimeException("Unable to stop the VM", e);
        } catch (TimeoutException e) {
            throw new CloudRuntimeException("Unable to stop the VM due to timeout", e);
        }
    }

    protected boolean sendStop(VirtualMachineGuru guru, VirtualMachineProfile profile, boolean force) {
        VirtualMachine vm = profile.getVirtualMachine();
        StopCommand stop = new StopCommand(vm, true);
        try {
            Answer answer = _agentMgr.send(vm.getHostId(), stop);
            if (!answer.getResult()) {
                s_logger.debug("Unable to stop VM due to " + answer.getDetails());
                return false;
            }

            guru.finalizeStop(profile, (StopAnswer) answer);
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

    protected boolean cleanup(VirtualMachineGuru guru, VirtualMachineProfile profile, VmWorkJobVO work, boolean force) {
        VirtualMachine vm = profile.getVirtualMachine();
        State state = vm.getState();
        s_logger.debug("Cleaning up resources for the vm " + vm + " in " + state + " state");
        if (state == State.Starting) {
            VmWorkJobVO.Step step = work.getStep();
            if (step == VmWorkJobVO.Step.Starting && !force) {
                s_logger.warn("Unable to cleanup vm " + vm + "; work state is incorrect: " + step);
                return false;
            }

            if (step == VmWorkJobVO.Step.Started || step == VmWorkJobVO.Step.Starting || step == VmWorkJobVO.Step.Release) {
                if (vm.getHostId() != null) {
                    if (!sendStop(guru, profile, force)) {
                        s_logger.warn("Failed to stop vm " + vm + " in " + State.Starting + " state as a part of cleanup process");
                        return false;
                    }
                }
            }

            if (step != VmWorkJobVO.Step.Release && step != VmWorkJobVO.Step.Prepare && step != VmWorkJobVO.Step.Started && step != VmWorkJobVO.Step.Starting) {
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

        try {
            _networkMgr.release(profile, force);
            s_logger.debug("Successfully released network resources for the vm " + vm);
        } catch (Exception e) {
            s_logger.warn("Unable to release some network resources.", e);
        }

        _volumeMgr.release(profile);
        s_logger.debug("Successfully cleanued up resources for the vm " + vm + " in " + state + " state");
        return true;
    }
    
    @Override
    @DB
    public Outcome<VirtualMachine> stop(final String vmUuid, boolean cleanup) {
        CallContext cc = CallContext.current();
        Account account = cc.getCallingAccount();
        User user = cc.getCallingUser();

        final VMInstanceVO vm = _vmDao.findByUuid(vmUuid);
    	VmWorkJobVO workJob = null;
    	Transaction txn = Transaction.currentTxn();
        txn.start();

        _vmDao.lockRow(vm.getId(), true);

        List<VmWorkJobVO> pendingWorkJobs = _workJobDao.listPendingWorkJobs(VirtualMachine.Type.Instance, vm.getId(), VmWorkJobDispatcher.Start);

        if (pendingWorkJobs != null && pendingWorkJobs.size() > 0) {
            assert (pendingWorkJobs.size() == 1);
            workJob = pendingWorkJobs.get(0);
        } else {
            workJob = new VmWorkJobVO(cc.getContextId());

            workJob.setDispatcher(VmWorkJobDispatcher.VM_WORK_JOB_DISPATCHER);
            workJob.setCmd(VmWorkJobDispatcher.Stop);

            workJob.setAccountId(account.getId());
            workJob.setUserId(user.getId());
            workJob.setStep(VmWorkJobVO.Step.Prepare);
            workJob.setVmType(vm.getType());
            workJob.setVmInstanceId(vm.getId());

            // save work context info (there are some duplications)
            VmWorkStop workInfo = new VmWorkStop(user.getId(), account.getId(), vm.getId(), cleanup);
            workJob.setCmdInfo(VmWorkJobDispatcher.serialize(workInfo));

            _jobMgr.submitAsyncJob(workJob, VmWorkJobDispatcher.VM_WORK_QUEUE, vm.getId());
    	}

        txn.commit();

    	final long jobId = workJob.getId();
    	AsyncJobExecutionContext.getCurrentExecutionContext().joinJob(jobId);
    	
        return new VmOutcome(workJob, VirtualMachine.PowerState.PowerOff, vm.getId(), null);
    }

    public void orchestrateStop(String vmUuid, boolean forced) throws AgentUnavailableException,
            OperationTimedoutException, ConcurrentOperationException {
        CallContext context = CallContext.current();

        VmWorkJobVO work = _workJobDao.findById(AsyncJobExecutionContext.getCurrentExecutionContext().getJob().getId());

        final VMInstanceVO vm = _vmDao.findByUuid(vmUuid);
        Long hostId = vm.getHostId();
        if (hostId == null) {
            if (!forced) {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("HostId is null but this is not a forced stop, cannot stop vm " + vm + " with state:" + vm.getState());
                }
                return;
            }
            
            try {
                stateTransitTo(vm, VirtualMachine.Event.AgentReportStopped, null, null);
            } catch (NoTransitionException e) {
                s_logger.warn(e.getMessage());
            }
  
            _workJobDao.updateStep(work.getId(), VmWorkJobVO.Step.Done);
            return;
        }

        VirtualMachineGuru vmGuru = getVmGuru(vm);
        VirtualMachineProfile profile = new VirtualMachineProfileImpl(vm);

        try {
            if (!stateTransitTo(vm, VirtualMachine.Event.StopRequested, vm.getHostId())) {
                throw new ConcurrentOperationException("VM is being operated on.");
            }
        } catch (NoTransitionException e1) {
            if (!forced) {
                throw new CloudRuntimeException("We cannot stop " + vm + " when it is in state " + vm.getState());
            }
            
            State state = vm.getState();
            boolean doCleanup = false;
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Unable to transition the state but we're moving on because it's forced stop");
            }
            if (state == State.Starting || state == State.Migrating) {
                doCleanup = true;
            } else if (state == State.Stopping) {
                doCleanup = true;
            }

            if (doCleanup) {
                if (cleanup(vmGuru, new VirtualMachineProfileImpl(vm), work, forced)) {
                    try {
                        if (s_logger.isDebugEnabled()) {
                            s_logger.debug("Updating work item to Done, id:" + work.getId());
                        }
                        changeState(vm, VirtualMachine.Event.AgentReportStopped, null, work, Step.Done);
                        return;
                    } catch (NoTransitionException e) {
                        s_logger.warn("Unable to cleanup " + vm);
                        return;
                    }
                } else {
                    if (s_logger.isDebugEnabled()) {
                        s_logger.debug("Failed to cleanup VM: " + vm);
                    }
                    throw new CloudRuntimeException("Failed to cleanup " + vm + " , current state " + vm.getState());
                }
            }
        }

        vmGuru.prepareStop(profile);
        
        StopCommand stop = new StopCommand(vm, true);
        boolean stopped = false;
        StopAnswer answer = null;
        try {
            answer = (StopAnswer) _agentMgr.send(vm.getHostId(), stop);
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
                    s_logger.warn("Unable to stop vm " + vm);
                    try {
                        stateTransitTo(vm, VirtualMachine.Event.OperationFailed, vm.getHostId());
                    } catch (NoTransitionException e) {
                        s_logger.warn("Unable to transition the state " + vm);
                    }
                    return;
                } else {
                    s_logger.warn("Unable to actually stop " + vm + " but continue with release because it's a force stop");
                    vmGuru.finalizeStop(profile, answer);
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
            if (vm.getHypervisorType() != HypervisorType.BareMetal) {
                _volumeMgr.release(profile);
                s_logger.debug("Successfully released storage resources for the vm " + vm);
            }
        } catch (Exception e) {
            s_logger.warn("Unable to release storage resources.", e);
        }

        try {
            if (work != null) {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Updating the outstanding work item to Done, id:" + work.getId());
                }
                _workJobDao.updateStep(work.getId(), VmWorkJobVO.Step.Done);
            }

            stateTransitTo(vm, VirtualMachine.Event.OperationSucceeded, null, null);
            return;
        } catch (NoTransitionException e) {
            s_logger.warn(e.getMessage());
            return;
        }
    }

    private void setStateMachine() {
        _stateMachine = VirtualMachine.State.getStateMachine();
    }

    protected boolean stateTransitTo(VMInstanceVO vm, VirtualMachine.Event e, Long hostId, String reservationId) throws NoTransitionException {
        vm.setReservationId(reservationId);
        return _stateMachine.transitTo(vm, e, new Pair<Long, Long>(vm.getHostId(), hostId), _vmDao);
    }

    @Override
    public boolean stateTransitTo(VMInstanceVO vm, VirtualMachine.Event e, Long hostId) throws NoTransitionException {
        State oldState = vm.getState();
        if (oldState == State.Starting) {
            if (e == VirtualMachine.Event.OperationSucceeded) {
                vm.setLastHostId(hostId);
            }
        } else if (oldState == State.Stopping) {
            if (e == VirtualMachine.Event.OperationSucceeded) {
                vm.setLastHostId(vm.getHostId());
            }
        }
        return _stateMachine.transitTo(vm, e, new Pair<Long, Long>(vm.getHostId(), hostId), _vmDao);
    }

    @Override
    public boolean destroy(String vmUuid) throws ResourceUnavailableException, OperationTimedoutException,
            ConcurrentOperationException {
        VMInstanceVO vm = _vmDao.findByUuid(vmUuid);
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Destroying vm " + vm);
        }
        if (vm == null || vm.getState() == State.Destroyed || vm.getState() == State.Expunging || vm.getRemoved() != null) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Unable to find vm or vm is destroyed: " + vm);
            }
            return true;
        }

        stop(vmUuid, _forceStop.value());
        
        vm = _vmDao.findById(vm.getId());

        if (!_vmSnapshotMgr.deleteAllVMSnapshots(vm.getId(),null)){
            s_logger.debug("Unable to delete all snapshots for " + vm);
            return false;
        }
        
        try {
            if (!stateTransitTo(vm, VirtualMachine.Event.DestroyRequested, vm.getHostId())) {
                s_logger.debug("Unable to destroy the vm because it is not in the correct state: " + vm);
                return false;
            }
        } catch (NoTransitionException e) {
            s_logger.debug(e.getMessage());
            return false;
        }

        return true;
    }

    protected boolean checkVmOnHost(VirtualMachine vm, long hostId) throws AgentUnavailableException, OperationTimedoutException {
        CheckVirtualMachineAnswer answer = (CheckVirtualMachineAnswer) _agentMgr.send(hostId, new CheckVirtualMachineCommand(vm.getInstanceName()));
        if (!answer.getResult() || answer.getState() == PowerState.PowerOff) {
            return false;
        }

        return true;
    }

    @Override
    public VirtualMachine storageMigration(String vmUuid, StoragePool destPool) {
        VMInstanceVO vm = _vmDao.findByUuid(vmUuid);

        try {
            stateTransitTo(vm, VirtualMachine.Event.StorageMigrationRequested, null);
        } catch (NoTransitionException e) {
            s_logger.debug("Unable to migrate vm: " + e.toString());
            throw new CloudRuntimeException("Unable to migrate vm: " + e.toString());
        }

        VirtualMachineProfile profile = new VirtualMachineProfileImpl(vm);
        boolean migrationResult = false;
        try {
            migrationResult = _volumeMgr.storageMigration(profile, destPool);

            if (migrationResult) {
                //if the vm is migrated to different pod in basic mode, need to reallocate ip

                if (!vm.getPodIdToDeployIn().equals(destPool.getPodId())) {
                    DataCenterDeployment plan = new DataCenterDeployment(vm.getDataCenterId(), destPool.getPodId(), null, null, null, null);
                    VirtualMachineProfileImpl vmProfile = new VirtualMachineProfileImpl(vm, null, null, null, null);
                    _networkMgr.reallocate(vmProfile, plan);
                }

                //when start the vm next time, don;'t look at last_host_id, only choose the host based on volume/storage pool
                vm.setLastHostId(null);
                vm.setPodId(destPool.getPodId());
            } else {
                s_logger.debug("Storage migration failed");
            }
        } catch (ConcurrentOperationException e) {
            s_logger.debug("Failed to migration: " + e.toString());
            throw new CloudRuntimeException("Failed to migration: " + e.toString());
        } catch (InsufficientVirtualNetworkCapcityException e) {
            s_logger.debug("Failed to migration: " + e.toString());
            throw new CloudRuntimeException("Failed to migration: " + e.toString());
        } catch (InsufficientAddressCapacityException e) {
            s_logger.debug("Failed to migration: " + e.toString());
            throw new CloudRuntimeException("Failed to migration: " + e.toString());
        } catch (InsufficientCapacityException e) {
            s_logger.debug("Failed to migration: " + e.toString());
            throw new CloudRuntimeException("Failed to migration: " + e.toString());
        } finally {
            try {
                stateTransitTo(vm, VirtualMachine.Event.AgentReportStopped, null);
            } catch (NoTransitionException e) {
                s_logger.debug("Failed to change vm state: " + e.toString());
                throw new CloudRuntimeException("Failed to change vm state: " + e.toString());
            }
        }

        return vm;
    }

    @Override
    public Outcome<VirtualMachine> migrate(String vmUuid, long srcHostId, DeployDestination dest) {
        CallContext context = CallContext.current();
        User user = context.getCallingUser();
        Account account = context.getCallingAccount();

        final VMInstanceVO vm = _vmDao.findByUuid(vmUuid);

        VmWorkJobVO workJob = null;
        Transaction txn = Transaction.currentTxn();
        txn.start();

        _vmDao.lockRow(vm.getId(), true);

        List<VmWorkJobVO> pendingWorkJobs = _workJobDao.listPendingWorkJobs(VirtualMachine.Type.Instance, vm.getId(), VmWorkJobDispatcher.Start);

        if (pendingWorkJobs != null && pendingWorkJobs.size() > 0) {
            assert (pendingWorkJobs.size() == 1);
            workJob = pendingWorkJobs.get(0);
        } else {
                    
            workJob = new VmWorkJobVO(context.getContextId());

            workJob.setDispatcher(VmWorkJobDispatcher.VM_WORK_JOB_DISPATCHER);
            workJob.setCmd(VmWorkJobDispatcher.Migrate);

            workJob.setAccountId(account.getId());
            workJob.setUserId(user.getId());
            workJob.setVmType(vm.getType());
            workJob.setVmInstanceId(vm.getId());

            // save work context info (there are some duplications)
            VmWorkMigrate workInfo = new VmWorkMigrate(user.getId(), account.getId(), vm.getId(), srcHostId, dest);
            workJob.setCmdInfo(VmWorkJobDispatcher.serialize(workInfo));

            _jobMgr.submitAsyncJob(workJob, VmWorkJobDispatcher.VM_WORK_QUEUE, vm.getId());
        }

        txn.commit();
        final long jobId = workJob.getId();
        AsyncJobExecutionContext.getCurrentExecutionContext().joinJob(jobId);
        return new VmOutcome(workJob, VirtualMachine.PowerState.PowerOn, vm.getId(), vm.getPowerHostId());

    }

    public void orchestrateMigrate(String vmUuid, long srcHostId, DeployDestination dest) throws AgentUnavailableException, OperationTimedoutException {
        AsyncJobExecutionContext jc = AsyncJobExecutionContext.getCurrentExecutionContext();

        VMInstanceVO vm = _vmDao.findByUuid(vmUuid);
        if (vm == null) {
            throw new CloudRuntimeException("Unable to find the vm " + vm);
        }

        if (vm.getState() != State.Running || vm.getHostId() == null || vm.getHostId() != srcHostId ) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Proper conditions to migrate " + vm + " is not met.");
            }
            return;
        }
        
        Host fromHost = _entityMgr.findById(Host.class, srcHostId);
        if (fromHost == null) {
            throw new CloudRuntimeException("Unable to find the host to migrate from: " + srcHostId);
        }
        long dstHostId = dest.getHost().getId();

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Migrating " + vm + " to " + dest);
        }

        short alertType = AlertManager.ALERT_TYPE_USERVM_MIGRATE;
        if (VirtualMachine.Type.DomainRouter.equals(vm.getType())) {
            alertType = AlertManager.ALERT_TYPE_DOMAIN_ROUTER_MIGRATE;
        } else if (VirtualMachine.Type.ConsoleProxy.equals(vm.getType())) {
            alertType = AlertManager.ALERT_TYPE_CONSOLE_PROXY_MIGRATE;
        }

        VirtualMachineProfile srcVm = new VirtualMachineProfileImpl(vm);
        for (NicProfile nic : _networkMgr.getNicProfiles(vm)) {
            srcVm.addNic(nic);
        }
        
        VirtualMachineProfile dstVm = new VirtualMachineProfileImpl(vm);
        _networkMgr.prepareNicForMigration(dstVm, dest);
        _volumeMgr.prepareForMigration(dstVm, dest);

        VirtualMachineTO to = toVmTO(dstVm);

        VmWorkJobVO work = _workJobDao.findById(jc.getJob().getId());
        work.setStep(MigrationStateMachine.getNextState(null, VirtualMachine.Event.MigrationRequested));
        work = _workJobDao.persist(work);

        PrepareForMigrationCommand pfmc = new PrepareForMigrationCommand(to);
        PrepareForMigrationAnswer pfma = null;
        try {
            try {
                pfma = (PrepareForMigrationAnswer)_agentMgr.send(dstHostId, pfmc);
                if (!pfma.getResult()) {
                    String msg = "Unable to prepare for migration due to " + pfma.getDetails();
                    throw new AgentUnavailableException(msg, dstHostId);
                }
            } catch (OperationTimedoutException e) {
                throw new AgentUnavailableException("Unable to prepare host " + dstHostId, dstHostId);
            }
    
            vm.setLastHostId(srcHostId);
            changeState2(vm, VirtualMachine.Event.MigrationRequested, dstHostId, work, VirtualMachine.Event.OperationSucceeded);
    
            boolean isWindows = _entityMgr.findById(GuestOsCategory.class, _entityMgr.findById(GuestOSVO.class, vm.getGuestOSId()).getCategoryId()).getName()
                    .equalsIgnoreCase("Windows");
            MigrateCommand mc = new MigrateCommand(vm.getInstanceName(), dest.getHost().getPrivateIpAddress(), isWindows);
            mc.setHostGuid(dest.getHost().getGuid());

            try {
                MigrateAnswer ma = (MigrateAnswer)_agentMgr.send(vm.getLastHostId(), mc);
                if (!ma.getResult()) {
                    throw new CloudRuntimeException("Unable to migrate due to " + ma.getDetails());
                }
            } catch (OperationTimedoutException e) {
                if (!e.isActive()) {
                    s_logger.warn("Active migration command so scheduling a restart for " + vm);
                    _haMgr.scheduleRestart(vm, true);
                }
                throw new CloudRuntimeException("Operation timed out on migrating " + vm);
            }

            if (!changeState(vm, VirtualMachine.Event.OperationSucceeded, dstHostId, work, Step.Started)) {
                throw new CloudRuntimeException("Unable to change the state for " + vm);
            }

            try {
                if (!checkVmOnHost(vm, dstHostId)) {
                    throw new CloudRuntimeException("Unable to complete migration for " + vm);
                }
            } catch (OperationTimedoutException e) {
                s_logger.warn("Unable to verify that " + vm + " has migrated but since the migrate command worked, it is assumed to have worked");
            }
            
            _networkMgr.commitNicForMigration(srcVm, dstVm);
            changeState2(vm, null, dstHostId, work, VirtualMachine.Event.OperationSucceeded);

        } catch (NoTransitionException e) {
            throw new CloudRuntimeException("Unable to change state", e);
        } finally {
            Step step = work.getStep();
            
            if (step != Step.Done) {
                s_logger.debug("Migration was unsuccessful.  Cleaning up: " + vm + " Step was at " + step);

                _alertMgr.sendAlert(alertType, fromHost.getDataCenterId(), fromHost.getPodId(), "Unable to migrate vm " + vm.getInstanceName() + " from host " + fromHost.getName()
                        + " in zone " + dest.getDataCenter().getName() + " and pod " + dest.getPod().getName(), "Migrate Command failed.  Please check logs.");

                boolean cleanup = false;
                try {
                    cleanupMigration(work, dstVm, vm, false);
                    cleanup = true;
                } catch (Exception ae) {
                    s_logger.warn("Unable to cleanup migration for " + vm);
                }

                if (cleanup) {
                    _networkMgr.rollbackNicForMigration(srcVm, dstVm);

                    work.setStep(Step.Done);
                    _workJobDao.update(work.getId(), work);
                }
            }
        }
    }

    private void determineVmLocation(VirtualMachineProfile vm, VMInstanceVO vo, boolean confirmedStopped) {
        try {
            if (checkVmOnHost(vo, vo.getLastHostId())) {
                stateTransitTo(vo, VirtualMachine.Event.AgentReportRunning, vo.getLastHostId());
            } else if (checkVmOnHost(vo, vo.getHostId())) {
                stateTransitTo(vo, VirtualMachine.Event.AgentReportRunning, vo.getHostId());
            } else {
                s_logger.warn("Unable to find " + vo + " on source " + vo.getLastHostId() + " or on destination " + vo.getHostId()
                        + (!confirmedStopped ? ".  Either call Stop with cleanup option or wait for vmsync to check where the VM is." : "."));
                stateTransitTo(vo, VirtualMachine.Event.AgentReportStopped, null);
            }
        } catch (AgentUnavailableException e) {
            if (confirmedStopped) {
                s_logger.debug("Agent is unavailable to determine state for " + vo + " but continuing with confirmed stopped option");
            } else {
                throw new CloudRuntimeException("Unable to determine the state for " + vo).add(VirtualMachine.class, vo.getUuid());
            }
        } catch (OperationTimedoutException e) {
            if (confirmedStopped) {
                s_logger.debug("Operation timedout while determining state for " + vo + " but continuing with confirmed stopped option");
            } else {
                throw new CloudRuntimeException("Unable to determine the state for " + vo).add(VirtualMachine.class, vo.getUuid());
            }
        } catch (NoTransitionException e) {
            throw new CloudRuntimeException("Unable to change VM state ", e).add(VirtualMachine.class, vo.getUuid());
        }

        if (confirmedStopped) {
            s_logger.info("Cleanup was requested on " + vo);
            cleanup(getVmGuru(vo), vm, null, true);
        }
    }
    /**
     * Migration goes through the following steps.
     *   Prepare - nics and storage are prepared
     *   Migrating - migrating command was issued
     *   Started - migrating command returned and was successful
     *   Done - the vm was verified on the destination host.
     * 
     * In order to cleanup, we have to go through the above steps in reverse.
     * 
     * @param job - job that was recording the status
     * @param vm - vm profile that needs to be cleaned up.
     * @param vo - vo object representing the VM.
     */
    private void cleanupMigration(VmWorkJobVO job, VirtualMachineProfile vm, VMInstanceVO vo, boolean confirmedStopped) {
        if (job == null) {
            s_logger.info("Cleaning up " + vo + " with no job to track progress");
            if (vo.getState() != VirtualMachine.State.Migrating) {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Requesting to clean up a vm that's not migrating: " + vo);
                }
                return;
            }
            determineVmLocation(vm, vo, confirmedStopped);
            return;
        }
        
        Step step = job.getStep();
        
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Migration operation was at " + step + " for " + vo);
        }
        if (step == Step.Started) {
            determineVmLocation(vm, vo, confirmedStopped);
        } else if (step == Step.Migrating) {
            determineVmLocation(vm, vo, confirmedStopped);
        }
    }


    private void filterPoolListForVolumesForMigration(VirtualMachineProfile profile, Host host, Map<Volume, StoragePool> volumeToPool) {
        List<VolumeVO> allVolumes = _volsDao.findUsableVolumesForInstance(profile.getId());
        for (VolumeVO volume : allVolumes) {
            StoragePool dstPool = volumeToPool.get(volume);
            DiskOfferingVO diskOffering = _entityMgr.findById(DiskOfferingVO.class, volume.getDiskOfferingId());
            StoragePoolVO srcPool = _entityMgr.findById(StoragePoolVO.class, volume.getPoolId());
            if (dstPool != null) {
                // Check if pool is accessible from the destination host and disk offering with which the volume was
                // created is compliant with the pool type.
                if (_poolHostDao.findByPoolHost(dstPool.getId(), host.getId()) == null ||
                        dstPool.isLocal() != diskOffering.getUseLocalStorage()) {
                    // Cannot find a pool for the volume. Throw an exception.
                    throw new CloudRuntimeException("Cannot migrate volume " + volume + " to storage pool " + dstPool +
                            " while migrating vm to host " + host + ". Either the pool is not accessible from the " +
                            "host or because of the offering with which the volume is created it cannot be placed on " +
                            "the given pool.");
                } else if (dstPool.getId() == srcPool.getId()){
                    // If the pool to migrate too is the same as current pool, remove the volume from the list of
                    // volumes to be migrated.
                    volumeToPool.remove(volume);
                }
            } else {
                // Find a suitable pool for the volume. Call the storage pool allocator to find the list of pools.
                DiskProfile diskProfile = new DiskProfile(volume, diskOffering, profile.getHypervisorType());
                DataCenterDeployment plan = new DataCenterDeployment(host.getDataCenterId(), host.getPodId(),
                        host.getClusterId(), host.getId(), null, null);
                ExcludeList avoid = new ExcludeList();
                boolean currentPoolAvailable = false;

                for (StoragePoolAllocator allocator : _storagePoolAllocators) {
                    List<StoragePool> poolList = allocator.allocateToPool(diskProfile, profile, plan, avoid,
                            StoragePoolAllocator.RETURN_UPTO_ALL);
                    if (poolList != null && !poolList.isEmpty()) {
                        // Volume needs to be migrated. Pick the first pool from the list. Add a mapping to migrate the
                        // volume to a pool only if it is required; that is the current pool on which the volume resides
                        // is not available on the destination host.
                        if (poolList.contains(srcPool)) {
                            currentPoolAvailable = true;
                        } else {
                            volumeToPool.put(volume, poolList.get(0));
                        }

                        break;
                    }
                }

                if (!currentPoolAvailable && !volumeToPool.containsKey(volume)) {
                    // Cannot find a pool for the volume. Throw an exception.
                    throw new CloudRuntimeException("Cannot find a storage pool which is available for volume " +
                            volume + " while migrating virtual machine " + profile.getVirtualMachine() + " to host " +
                            host);
                }
            }
        }
    }

    private void moveVmToMigratingState(VMInstanceVO vm, Long hostId, VmWorkJobVO work)
            throws ConcurrentOperationException {
        // Put the vm in migrating state.
        try {
            if (!changeState(vm, VirtualMachine.Event.MigrationRequested, hostId, work, Step.Migrating)) {
                s_logger.info("Migration cancelled because state has changed: " + vm);
                throw new ConcurrentOperationException("Migration cancelled because state has changed: " + vm);
            }
        } catch (NoTransitionException e) {
            s_logger.info("Migration cancelled because " + e.getMessage());
            throw new ConcurrentOperationException("Migration cancelled because " + e.getMessage());
        }
    }

    private void moveVmOutofMigratingStateOnSuccess(VMInstanceVO vm, Long hostId, VmWorkJobVO work)
            throws ConcurrentOperationException {
        // Put the vm in running state.
        try {
            if (!changeState(vm, VirtualMachine.Event.OperationSucceeded, hostId, work, Step.Started)) {
                s_logger.error("Unable to change the state for " + vm);
                throw new ConcurrentOperationException("Unable to change the state for " + vm);
            }
        } catch (NoTransitionException e) {
            s_logger.error("Unable to change state due to " + e.getMessage());
            throw new ConcurrentOperationException("Unable to change state due to " + e.getMessage());
        }
    }

    @Override
    public VirtualMachine migrateWithStorage(String vmUuid, long srcHostId, long destHostId,
            Map<Volume, StoragePool> volumeToPool) throws ResourceUnavailableException, ConcurrentOperationException,
            ManagementServerException, VirtualMachineMigrationException {
        CallContext context = CallContext.current();

        VMInstanceVO vm = _vmDao.findByUuid(vmUuid);
        HostVO srcHost = _entityMgr.findById(HostVO.class, srcHostId);
        HostVO destHost = _entityMgr.findById(HostVO.class, destHostId);
        VirtualMachineGuru vmGuru = getVmGuru(vm);

        DataCenterVO dc = _entityMgr.findById(DataCenterVO.class, destHost.getDataCenterId());
        HostPodVO pod = _entityMgr.findById(HostPodVO.class, destHost.getPodId());
        Cluster cluster = _entityMgr.findById(ClusterVO.class, destHost.getClusterId());
        DeployDestination destination = new DeployDestination(dc, pod, cluster, destHost);

        // Create a map of which volume should go in which storage pool.
        VirtualMachineProfile profile = new VirtualMachineProfileImpl(vm);
        filterPoolListForVolumesForMigration(profile, destHost, volumeToPool);

        // If none of the volumes have to be migrated, fail the call. Administrator needs to make a call for migrating
        // a vm and not migrating a vm with storage.
        if (volumeToPool.isEmpty()) {
            throw new InvalidParameterValueException("Migration of the vm " + vm + "from host " + srcHost +
                    " to destination host " + destHost + " doesn't involve migrating the volumes.");
        }

        short alertType = AlertManager.ALERT_TYPE_USERVM_MIGRATE;
        if (VirtualMachine.Type.DomainRouter.equals(vm.getType())) {
            alertType = AlertManager.ALERT_TYPE_DOMAIN_ROUTER_MIGRATE;
        } else if (VirtualMachine.Type.ConsoleProxy.equals(vm.getType())) {
            alertType = AlertManager.ALERT_TYPE_CONSOLE_PROXY_MIGRATE;
        }

        _networkMgr.prepareNicForMigration(profile, destination);
        _volumeMgr.prepareForMigration(profile, destination);
        HypervisorGuru hvGuru = _hvGuruMgr.getGuru(vm.getHypervisorType());
        VirtualMachineTO to = hvGuru.implement(profile);

        VmWorkJobVO work = new VmWorkJobVO(context.getContextId());
//        VmWorkJobVO work = new VmWorkJobVO(UUID.randomUUID().toString(), _nodeId, State.Migrating, vm.getType(), vm.getId());
//        work.setStep(Step.Prepare);
//        work.setResourceType(ItWorkVO.ResourceType.Host);
//        work.setResourceId(destHostId);
//        work = _workDao.persist(work);

        // Put the vm in migrating state.
        vm.setLastHostId(srcHostId);
        moveVmToMigratingState(vm, destHostId, work);

        boolean migrated = false;
        try {
            // Migrate the vm and its volume.
            _volumeMgr.migrateVolumes(vm, to, srcHost, destHost, volumeToPool);

            // Put the vm back to running state.
            moveVmOutofMigratingStateOnSuccess(vm, destHost.getId(), work);

            try {
                if (!checkVmOnHost(vm, destHostId)) {
                    s_logger.error("Vm not found on destination host. Unable to complete migration for " + vm);
                    try {
                        _agentMgr.send(srcHostId, new Commands(cleanup(vm.getInstanceName())), null);
                    } catch (AgentUnavailableException e) {
                        s_logger.error("AgentUnavailableException while cleanup on source host: " + srcHostId);
                    }
                    cleanup(vmGuru, new VirtualMachineProfileImpl(vm), work, true);
                    return null;
                }
            } catch (OperationTimedoutException e) {
                s_logger.warn("Error while checking the vm " + vm + " is on host " + destHost, e);
            }

            migrated = true;
            return vm;
        } finally {
            if (!migrated) {
                s_logger.info("Migration was unsuccessful.  Cleaning up: " + vm);
                _alertMgr.sendAlert(alertType, srcHost.getDataCenterId(), srcHost.getPodId(), "Unable to migrate vm " +
                        vm.getInstanceName() + " from host " + srcHost.getName() + " in zone " + dc.getName() +
                        " and pod " + dc.getName(), "Migrate Command failed.  Please check logs.");
                try {
                    _agentMgr.send(destHostId, new Commands(cleanup(vm.getInstanceName())), null);
                    stateTransitTo(vm, VirtualMachine.Event.OperationFailed, srcHostId);
                } catch (AgentUnavailableException e) {
                    s_logger.warn("Looks like the destination Host is unavailable for cleanup.", e);
                } catch (NoTransitionException e) {
                    s_logger.error("Error while transitioning vm from migrating to running state.", e);
                }
            }

            work.setStep(Step.Done);
            // FIXME  _workDao.update(work.getId(), work);
        }
    }

    @Override
    public VirtualMachineTO toVmTO(VirtualMachineProfile profile) {
        HypervisorGuru hvGuru = _hvGuruMgr.getGuru(profile.getVirtualMachine().getHypervisorType());
        VirtualMachineTO to = hvGuru.implement(profile);
        return to;
    }

    protected void cancelWorkItems(long nodeId) {
        /*
            GlobalLock scanLock = GlobalLock.getInternLock("vmmgr.cancel.workitem");

            try {
                if (scanLock.lock(3)) {
                    try {
                        List<VmWorkJobVO> works = _workDao.listWorkInProgressFor(nodeId);
                        for (VmWorkJobVO work : works) {
                            s_logger.info("Handling unfinished work item: " + work);
                            try {
                                VMInstanceVO vm = _vmDao.findById(work.getInstanceId());
                                if (vm != null) {
                                    if (work.getType() == State.Starting) {
                                        _haMgr.scheduleRestart(vm, true);
                                        work.setManagementServerId(_nodeId);
                                        _workDao.update(work.getId(), work);
                                    } else if (work.getType() == State.Stopping) {
                                        _haMgr.scheduleStop(vm, vm.getHostId(), WorkType.CheckStop);
                                        work.setManagementServerId(_nodeId);
                                        _workDao.update(work.getId(), work);
                                    } else if (work.getType() == State.Migrating) {
                                        _haMgr.scheduleMigration(vm);
                                        work.setStep(Step.Done);
                                        _workDao.update(work.getId(), work);
                                    }
                                }
                            } catch (Exception e) {
                                s_logger.error("Error while handling " + work, e);
                            }
                        }
                    } finally {
                        scanLock.unlock();
                    }
                }
            } finally {
                scanLock.releaseRef();
            }
        */
    }

    @Override
    public boolean migrateAway(VirtualMachine.Type vmType, long vmId, long srcHostId) throws InsufficientServerCapacityException, VirtualMachineMigrationException {
        CallContext cc = CallContext.current();
        VMInstanceVO vm = _vmDao.findById(vmId);
        if (vm == null) {
            s_logger.debug("Unable to find a VM for " + vmId);
            return true;
        }

        VirtualMachineProfile profile = new VirtualMachineProfileImpl(vm);

        Long hostId = vm.getHostId();
        if (hostId == null) {
            s_logger.debug("Unable to migrate because the VM doesn't have a host id: " + vm);
            return true;
        }

        Host host = _hostDao.findById(hostId);

        DataCenterDeployment plan = new DataCenterDeployment(host.getDataCenterId(), host.getPodId(), host.getClusterId(), null, null, null);
        ExcludeList excludes = new ExcludeList();
        excludes.addHost(hostId);

        DeployDestination dest = null;
        while (true) {

            try {
                dest = _dpMgr.planDeployment(profile, plan, excludes);
            } catch (AffinityConflictException e2) {
                s_logger.warn("Unable to create deployment, affinity rules associted to the VM conflict", e2);
                throw new CloudRuntimeException(
                        "Unable to create deployment, affinity rules associted to the VM conflict");
                }

                if (dest != null) {
                    if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Found destination " + dest + " for migrating to.");
                }
            } else {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Unable to find destination for migrating the vm " + profile);
                }
                throw new InsufficientServerCapacityException("Unable to find a server to migrate to.", host.getClusterId());
            }

            excludes.addHost(dest.getHost().getId());
            VirtualMachine vmInstance = null;
            Outcome<VirtualMachine> outcome = migrate(vm.getUuid(), srcHostId, dest);
            try {
                vmInstance = outcome.get();
            } catch (InterruptedException e1) {
                s_logger.warn("Unable to migrate teh VM", e1);
            } catch (java.util.concurrent.ExecutionException e1) {
                s_logger.warn("Unable to migrate the VM", e1);
            }
            if (vmInstance != null) {
                return true;
            }
            outcome = stop(vm.getUuid(), true);
            try {
                outcome.get(_jobTimeout.value(), TimeUnit.MILLISECONDS);
                return true;
            } catch (InterruptedException e) {
                s_logger.warn("Unable to migrate the VM", e);
            } catch (java.util.concurrent.ExecutionException e) {
                s_logger.warn("Unable to migrate the VM", e);
            } catch (TimeoutException e) {
                s_logger.warn("Unable to migrate the VM", e);
            }

            return false;
        }
    }

    protected class CleanupTask implements Runnable {
    	
        @Override
        public void run() {
            s_logger.trace("VM Operation Thread Running");
            
            try {
	            Date cutDate = DateUtil.currentGMTTime();
	            cutDate = new Date(cutDate.getTime() - 60000);
	            _workJobDao.expungeCompletedWorkJobs(cutDate);
            } catch(Throwable e) {
            	s_logger.error("Unexpected exception", e);
            }
/*
            try {
                _workDao.cleanup(_cleanupWait);
            } catch (Exception e) {
                s_logger.error("VM Operations failed due to ", e);
            }
*/
        }
    }

    private boolean isVirtualMachineUpgradable(VirtualMachine vm, ServiceOffering offering) {
        boolean isMachineUpgradable = true;
        for(HostAllocator allocator : _hostAllocators) {
            isMachineUpgradable = allocator.isVirtualMachineUpgradable(vm, offering);
            if(isMachineUpgradable)
                continue;
            else
                break;
        }

        return isMachineUpgradable;
    }

    @Override
    public void reboot(String vmUuid) {
        try {
            advanceReboot(vmUuid);
        } catch (ConcurrentOperationException e) {
            throw new CloudRuntimeException("Unable to reboot a VM due to concurrent operation", e).add(VirtualMachine.class, vmUuid);
        } catch (InsufficientCapacityException e) {
            throw new CloudRuntimeException("Unable to reboot a VM due to insufficient capacity", e).add(VirtualMachine.class, vmUuid);
        } catch (ResourceUnavailableException e) {
            throw new CloudRuntimeException("Unable to reboot a VM due to resource unavailable", e).add(VirtualMachine.class, vmUuid);
        } catch (OperationTimedoutException e) {
            throw new CloudRuntimeException("Unable to reboot a VM due to reboot operation timed out", e).add(VirtualMachine.class, vmUuid);
        }
    }

    @Override
    public void advanceReboot(String vmUuid) throws InsufficientCapacityException,
            ConcurrentOperationException, ResourceUnavailableException, OperationTimedoutException {
        VMInstanceVO vm = _vmDao.findByUuid(vmUuid);
        if (vm.getHostId() == null) {
            s_logger.debug("No need to reboot " + vm + " when it doesn't have a host id");
            return;
        }

        Host host = _hostDao.findById(vm.getHostId());


        Commands cmds = new Commands(OnError.Stop);
        cmds.addCommand(new RebootCommand(vm.getInstanceName()));
        _agentMgr.send(host.getId(), cmds);

        Answer rebootAnswer = cmds.getAnswer(RebootAnswer.class);
        if (!rebootAnswer.getResult()) {
            throw new CloudRuntimeException("Unable to reboot " + vm + " on " + host + " due to " + rebootAnswer.getDetails()).add(VirtualMachine.class, vmUuid);
        }
    }

    public Command cleanup(VirtualMachine vm) {
        return new StopCommand(vm, true);
    }

    public Command cleanup(String vmName) {
        return new StopCommand(vmName, true);
    }
/*
    public Commands fullHostSync(final long hostId, StartupRoutingCommand startup) {
        Commands commands = new Commands(OnError.Continue);

        Map<Long, AgentVmInfo> infos = convertToInfos(startup);
        final List<? extends VMInstanceVO> vms = _vmDao.listByHostId(hostId);
        s_logger.debug("Found " + vms.size() + " VMs for host " + hostId);
        for (VMInstanceVO vm : vms) {
            AgentVmInfo info = infos.remove(vm.getId());
            
            // sync VM Snapshots related transient states
            List<VMSnapshotVO> vmSnapshotsInTrasientStates = _vmSnapshotDao.listByInstanceId(vm.getId(), VMSnapshot.State.Expunging,VMSnapshot.State.Reverting, VMSnapshot.State.Creating);
            if(vmSnapshotsInTrasientStates.size() > 1){
                s_logger.info("Found vm " + vm.getInstanceName() + " with VM snapshots in transient states, needs to sync VM snapshot state");
                if(!_vmSnapshotMgr.syncVMSnapshot(vm, hostId)){
                    s_logger.warn("Failed to sync VM in a transient snapshot related state: " + vm.getInstanceName());
                    continue;
                }else{
                    s_logger.info("Successfully sync VM with transient snapshot: " + vm.getInstanceName());
                }
            }
            
            VMInstanceVO castedVm = null;
            if (info == null) {
                info = new AgentVmInfo(vm.getInstanceName(), getVmGuru(vm), vm, State.Stopped);
            }
            castedVm = info.guru.findById(vm.getId());

            HypervisorGuru hvGuru = _hvGuruMgr.getGuru(castedVm.getHypervisorType());
            Command command = compareState(hostId, castedVm, info, true, hvGuru.trackVmHostChange());
            if (command != null) {
                commands.addCommand(command);
            }
        }

        for (final AgentVmInfo left : infos.values()) {
            boolean found = false;
            for (VirtualMachineGuru<? extends VMInstanceVO> vmGuru : _vmGurus.values()) {
                VMInstanceVO vm = vmGuru.findByName(left.name);
                if (vm != null) {
                    found = true;
                    HypervisorGuru hvGuru = _hvGuruMgr.getGuru(vm.getHypervisorType());
                    if(hvGuru.trackVmHostChange()) {
                        Command command = compareState(hostId, vm, left, true, true);
                        if (command != null) {
                            commands.addCommand(command);
                        }
                    } else {
                        s_logger.warn("Stopping a VM, VM " + left.name + " migrate from Host " + vm.getHostId() + " to Host " + hostId );
                        commands.addCommand(cleanup(left.name));
                    }
                    break;
                }
            }
            if ( ! found ) {
                s_logger.warn("Stopping a VM that we have no record of <fullHostSync>: " + left.name);
                commands.addCommand(cleanup(left.name));
            }
        }

        return commands;
    }

    public Commands deltaHostSync(long hostId, Map<String, State> newStates) {
        Map<Long, AgentVmInfo> states = convertDeltaToInfos(newStates);
        Commands commands = new Commands(OnError.Continue);

        for (Map.Entry<Long, AgentVmInfo> entry : states.entrySet()) {
            AgentVmInfo info = entry.getValue();

            VMInstanceVO vm = info.vm;

            Command command = null;
            if (vm != null) {
                HypervisorGuru hvGuru = _hvGuruMgr.getGuru(vm.getHypervisorType());
                command = compareState(hostId, vm, info, false, hvGuru.trackVmHostChange());
            } else {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Cleaning up a VM that is no longer found: " + info.name);
                }
                command = cleanup(info.name);
            }

            if (command != null) {
                commands.addCommand(command);
            }
        }

        return commands;
    }

    public void deltaSync(Map<String, Pair<String, State>> newStates) {
        Map<Long, AgentVmInfo> states = convertToInfos(newStates);

        for (Map.Entry<Long, AgentVmInfo> entry : states.entrySet()) {
            AgentVmInfo info = entry.getValue();
            VMInstanceVO vm = info.vm;
            Command command = null;
            if (vm != null) {
                Host host = _resourceMgr.findHostByGuid(info.getHostUuid());
                long hId = host.getId();

                HypervisorGuru hvGuru = _hvGuruMgr.getGuru(vm.getHypervisorType());
                command = compareState(hId, vm, info, false, hvGuru.trackVmHostChange());
            } else {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Cleaning up a VM that is no longer found <deltaSync>: " + info.name);
                }
                command = cleanup(info.name);
            }
            if (command != null){
                try {
                    Host host = _resourceMgr.findHostByGuid(info.getHostUuid());
                    if (host != null){
                        Answer answer = _agentMgr.send(host.getId(), cleanup(info.name));
                        if (!answer.getResult()) {
                            s_logger.warn("Unable to stop a VM due to " + answer.getDetails());
                        }
                    }
                } catch (Exception e) {
                    s_logger.warn("Unable to stop a VM due to " + e.getMessage());
                }
            }
        }
    }
*/

    public void fullSync(final long clusterId, Map<String, Pair<String, State>> newStates) {
        /*
                if (newStates==null)return;
                Map<Long, AgentVmInfo> infos = convertToInfos(newStates);
                Set<VMInstanceVO> set_vms = Collections.synchronizedSet(new HashSet<VMInstanceVO>());
                set_vms.addAll(_vmDao.listByClusterId(clusterId));
                set_vms.addAll(_vmDao.listLHByClusterId(clusterId));

                for (VMInstanceVO vm : set_vms) {
                    AgentVmInfo info =  infos.remove(vm.getId());
                    VMInstanceVO castedVm = null;
                    
                    // sync VM Snapshots related transient states
                    List<VMSnapshotVO> vmSnapshotsInExpungingStates = _vmSnapshotDao.listByInstanceId(vm.getId(), VMSnapshot.State.Expunging, VMSnapshot.State.Creating,VMSnapshot.State.Reverting);
                    if(vmSnapshotsInExpungingStates.size() > 0){
                        s_logger.info("Found vm " + vm.getInstanceName() + " in state. " + vm.getState() + ", needs to sync VM snapshot state");
                        Long hostId = null;
                        Host host = null;
                        if(info != null && info.getHostUuid() != null){
                            host = _hostDao.findByGuid(info.getHostUuid());
                        }
                        hostId = host == null ? (vm.getHostId() == null ? vm.getLastHostId() : vm.getHostId()) : host.getId();
                        if(!_vmSnapshotMgr.syncVMSnapshot(vm, hostId)){
                            s_logger.warn("Failed to sync VM with transient snapshot: " + vm.getInstanceName());
                            continue;
                        }else{
                            s_logger.info("Successfully sync VM with transient snapshot: " + vm.getInstanceName());
                        }
                    }
                    
                    if ((info == null && (vm.getState() == State.Running || vm.getState() == State.Starting ))
                    		||  (info != null && (info.state == State.Running && vm.getState() == State.Starting)))
                    {
                        s_logger.info("Found vm " + vm.getInstanceName() + " in inconsistent state. " + vm.getState() + " on CS while " +  (info == null ? "Stopped" : "Running") + " on agent");
                        info = new AgentVmInfo(vm.getInstanceName(), getVmGuru(vm), vm, State.Stopped);

                        // Bug 13850- grab outstanding work item if any for this VM state so that we mark it as DONE after we change VM state, else it will remain pending
                        VmWorkJobVO work = _workDao.findByOutstandingWork(vm.getId(), vm.getState());
                        if (work != null) {
                            if (s_logger.isDebugEnabled()) {
                                s_logger.debug("Found an outstanding work item for this vm " + vm + " in state:" + vm.getState() + ", work id:" + work.getId());
                            }
                        }
                        vm.setState(State.Running); // set it as running and let HA take care of it
                        _vmDao.persist(vm);

                        if (work != null) {
                            if (s_logger.isDebugEnabled()) {
                                s_logger.debug("Updating outstanding work item to Done, id:" + work.getId());
                            }
                            work.setStep(Step.Done);
                            _workDao.update(work.getId(), work);
                        }

                        castedVm = info.guru.findById(vm.getId());
                        try {
                            Host host = _hostDao.findByGuid(info.getHostUuid());
                            long hostId = host == null ? (vm.getHostId() == null ? vm.getLastHostId() : vm.getHostId()) : host.getId();
                            HypervisorGuru hvGuru = _hvGuruMgr.getGuru(castedVm.getHypervisorType());
                            Command command = compareState(hostId, castedVm, info, true, hvGuru.trackVmHostChange());
                            if (command != null){
                                Answer answer = _agentMgr.send(hostId, command);
                                if (!answer.getResult()) {
                                    s_logger.warn("Failed to update state of the VM due to " + answer.getDetails());
                                }
                            }
                        } catch (Exception e) {
                            s_logger.warn("Unable to update state of the VM due to exception " + e.getMessage());
                            e.printStackTrace();
                        }
                    }
                    else if (info != null && (vm.getState() == State.Stopped || vm.getState() == State.Stopping
                            || vm.isRemoved() || vm.getState() == State.Destroyed || vm.getState() == State.Expunging )) {
                        Host host = _hostDao.findByGuid(info.getHostUuid());
                        if (host != null){
                            s_logger.warn("Stopping a VM which is stopped/stopping/destroyed/expunging " + info.name);
                            if (vm.getState() == State.Stopped || vm.getState() == State.Stopping) {
                                vm.setState(State.Stopped); // set it as stop and clear it from host
                                vm.setHostId(null);
                                _vmDao.persist(vm);
                            }
                            try {
                                Answer answer = _agentMgr.send(host.getId(), cleanup(info.name));
                                if (!answer.getResult()) {
                                    s_logger.warn("Unable to stop a VM due to " + answer.getDetails());
                                }
                            }
                            catch (Exception e) {
                                s_logger.warn("Unable to stop a VM due to " + e.getMessage());
                            }
                        }
                    }
                    else
                        // host id can change
                        if (info != null && vm.getState() == State.Running){
                            // check for host id changes
                            Host host = _hostDao.findByGuid(info.getHostUuid());
                            if (host != null && (vm.getHostId() == null || host.getId() != vm.getHostId())){
                                s_logger.info("Found vm " + vm.getInstanceName() + " with inconsistent host in db, new host is " +  host.getId());
                                try {
                                    stateTransitTo(vm, VirtualMachine.Event.AgentReportMigrated, host.getId());
                                } catch (NoTransitionException e) {
                                    s_logger.warn(e.getMessage());
                                }
                            }
                        }
                }

                for (final AgentVmInfo left : infos.values()) {
                    if (!VirtualMachineName.isValidVmName(left.name)) continue;  // if the vm doesn't follow CS naming ignore it for stopping
                    try {
                        Host host = _hostDao.findByGuid(left.getHostUuid());
                        if (host != null){
                            s_logger.warn("Stopping a VM which we do not have any record of " + left.name);
                            Answer answer = _agentMgr.send(host.getId(), cleanup(left.name));
                            if (!answer.getResult()) {
                                s_logger.warn("Unable to stop a VM due to " + answer.getDetails());
                            }
                        }
                    } catch (Exception e) {
                        s_logger.warn("Unable to stop a VM due to " + e.getMessage());
                    }
                }
        */
    }



    protected Map<Long, AgentVmInfo> convertToInfos(final Map<String, Pair<String, State>> newStates) {
        final HashMap<Long, AgentVmInfo> map = new HashMap<Long, AgentVmInfo>();
        if (newStates == null) {
            return map;
        }
        Collection<VirtualMachineGuru> vmGurus = _vmGurus.values();
        boolean is_alien_vm = true;
        long alien_vm_count = -1;
        for (Map.Entry<String, Pair<String, State>> entry : newStates.entrySet()) {
            is_alien_vm = true;
            for (VirtualMachineGuru vmGuru : vmGurus) {
                String name = entry.getKey();
                VMInstanceVO vm = _vmDao.findVMByInstanceName(name);
                if (vm != null) {
                    map.put(vm.getId(), new AgentVmInfo(entry.getKey(), vmGuru, vm, entry.getValue().second(), entry.getValue().first()));
                    is_alien_vm = false;
                    break;
                }
            }
            // alien VMs
            if (is_alien_vm){
                map.put(alien_vm_count--, new AgentVmInfo(entry.getKey(), null, null, entry.getValue().second(), entry.getValue().first()));
                s_logger.warn("Found an alien VM " + entry.getKey());
            }
        }
        return map;
    }

/*
    protected Map<Long, AgentVmInfo> convertToInfos(StartupRoutingCommand cmd) {
        final Map<String, HostVmStateReportEntry> states = cmd.getVmStates();
        final HashMap<Long, AgentVmInfo> map = new HashMap<Long, AgentVmInfo>();
        if (states == null) {
            return map;
        }
        Collection<VirtualMachineGuru<? extends VMInstanceVO>> vmGurus = _vmGurus.values();

        for (Map.Entry<String, HostVmStateReportEntry> entry : states.entrySet()) {
            for (VirtualMachineGuru<? extends VMInstanceVO> vmGuru : vmGurus) {
                String name = entry.getKey();
                VMInstanceVO vm = vmGuru.findByName(name);
                if (vm != null) {
                    map.put(vm.getId(), new AgentVmInfo(entry.getKey(), vmGuru, vm, entry.getValue().getState(), entry.getValue().getHost() ));
                    break;
                }
                Long id = vmGuru.convertToId(name);
                if (id != null) {
                    map.put(id, new AgentVmInfo(entry.getKey(), vmGuru, null,entry.getValue().getState(), entry.getValue().getHost() ));
                    break;
                }
            }
        }

        return map;
    }
*/
    protected Map<Long, AgentVmInfo> convertDeltaToInfos(final Map<String, State> states) {
        final HashMap<Long, AgentVmInfo> map = new HashMap<Long, AgentVmInfo>();

        if (states == null) {
            return map;
        }

        Collection<VirtualMachineGuru> vmGurus = _vmGurus.values();

        for (Map.Entry<String, State> entry : states.entrySet()) {
            for (VirtualMachineGuru vmGuru : vmGurus) {
                String name = entry.getKey();

                VMInstanceVO vm = _vmDao.findVMByInstanceName(name);

                if (vm != null) {
                    map.put(vm.getId(), new AgentVmInfo(entry.getKey(), vmGuru, vm, entry.getValue()));
                    break;
                }
            }
        }

        return map;
    }

    /**
     * compareState does as its name suggests and compares the states between
     * management server and agent. It returns whether something should be
     * cleaned up
     * 
     */
    protected Command compareState(long hostId, VMInstanceVO vm, final AgentVmInfo info, final boolean fullSync, boolean trackExternalChange) {
    	return null;
        /*
                State agentState = info.state;
                final State serverState = vm.getState();
                final String serverName = vm.getInstanceName();

                Command command = null;
                s_logger.debug("VM " + serverName + ": cs state = " + serverState + " and realState = " + agentState);
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("VM " + serverName + ": cs state = " + serverState + " and realState = " + agentState);
                }

                if (agentState == State.Error) {
                    agentState = State.Stopped;

                    short alertType = AlertManager.ALERT_TYPE_USERVM;
                    if (VirtualMachine.Type.DomainRouter.equals(vm.getType())) {
                        alertType = AlertManager.ALERT_TYPE_DOMAIN_ROUTER;
                    } else if (VirtualMachine.Type.ConsoleProxy.equals(vm.getType())) {
                        alertType = AlertManager.ALERT_TYPE_CONSOLE_PROXY;
                    } else if (VirtualMachine.Type.SecondaryStorageVm.equals(vm.getType())) {
                        alertType = AlertManager.ALERT_TYPE_SSVM;
                    }

                    HostPodVO podVO = _podDao.findById(vm.getPodIdToDeployIn());
                    DataCenterVO dcVO = _dcDao.findById(vm.getDataCenterId());
                    HostVO hostVO = _hostDao.findById(vm.getHostId());

                    String hostDesc = "name: " + hostVO.getName() + " (id:" + hostVO.getId() + "), availability zone: " + dcVO.getName() + ", pod: " + podVO.getName();
                    _alertMgr.sendAlert(alertType, vm.getDataCenterId(), vm.getPodIdToDeployIn(), "VM (name: " + vm.getInstanceName() + ", id: " + vm.getId() + ") stopped on host " + hostDesc
                            + " due to storage failure", "Virtual Machine " + vm.getInstanceName() + " (id: " + vm.getId() + ") running on host [" + vm.getHostId() + "] stopped due to storage failure.");
                }

                if (trackExternalChange) {
                    if (serverState == State.Starting) {
                        if (vm.getHostId() != null && vm.getHostId() != hostId) {
                            s_logger.info("CloudStack is starting VM on host " + vm.getHostId() + ", but status report comes from a different host " + hostId + ", skip status sync for vm: "
                                    + vm.getInstanceName());
                            return null;
                        }
                    }
                    if (vm.getHostId() == null || hostId != vm.getHostId()) {
                        try {
                            VmWorkJobVO workItem = _workDao.findByOutstandingWork(vm.getId(), State.Migrating);
                            if(workItem == null){
                                stateTransitTo(vm, VirtualMachine.Event.AgentReportMigrated, hostId);
                            }
                        } catch (NoTransitionException e) {
                        }
                    }
                }

                // during VM migration time, don't sync state will agent status update
                if (serverState == State.Migrating) {
                    s_logger.debug("Skipping vm in migrating state: " + vm);
                    return null;
                }

                if (trackExternalChange) {
                    if (serverState == State.Starting) {
                        if (vm.getHostId() != null && vm.getHostId() != hostId) {
                            s_logger.info("CloudStack is starting VM on host " + vm.getHostId() + ", but status report comes from a different host " + hostId + ", skip status sync for vm: "
                                    + vm.getInstanceName());
                            return null;
                        }
                    }

                    if (serverState == State.Running) {
                        try {
                            //
                            // we had a bug that sometimes VM may be at Running State
                            // but host_id is null, we will cover it here.
                            // means that when CloudStack DB lost of host information,
                            // we will heal it with the info reported from host
                            //
                            if (vm.getHostId() == null || hostId != vm.getHostId()) {
                                if (s_logger.isDebugEnabled()) {
                                    s_logger.debug("detected host change when VM " + vm + " is at running state, VM could be live-migrated externally from host " + vm.getHostId() + " to host " + hostId);
                                }

                                stateTransitTo(vm, VirtualMachine.Event.AgentReportMigrated, hostId);
                            }
                        } catch (NoTransitionException e) {
                            s_logger.warn(e.getMessage());
                        }
                    }
                }

                if (agentState == serverState) {
                    if (s_logger.isDebugEnabled()) {
                        s_logger.debug("Both states are " + agentState + " for " + vm);
                    }
                    assert (agentState == State.Stopped || agentState == State.Running) : "If the states we send up is changed, this must be changed.";
                    if (agentState == State.Running) {
                        try {
                            stateTransitTo(vm, VirtualMachine.Event.AgentReportRunning, hostId);
                        } catch (NoTransitionException e) {
                            s_logger.warn(e.getMessage());
                        }
                        // FIXME: What if someone comes in and sets it to stopping? Then
                        // what?
                        return null;
                    }

                    s_logger.debug("State matches but the agent said stopped so let's send a cleanup command anyways.");
                    return cleanup(vm);
                }

                if (agentState == State.Shutdowned) {
                    if (serverState == State.Running || serverState == State.Starting || serverState == State.Stopping) {
                        try {
                            advanceStop(vm, true, _accountMgr.getSystemUser(), _accountMgr.getSystemAccount());
                        } catch (AgentUnavailableException e) {
                            assert (false) : "How do we hit this with forced on?";
                            return null;
                        } catch (OperationTimedoutException e) {
                            assert (false) : "How do we hit this with forced on?";
                            return null;
                        } catch (ConcurrentOperationException e) {
                            assert (false) : "How do we hit this with forced on?";
                            return null;
                        }
                    } else {
                        s_logger.debug("Sending cleanup to a shutdowned vm: " + vm.getInstanceName());
                        command = cleanup(vm);
                    }
                } else if (agentState == State.Stopped) {
                    // This state means the VM on the agent was detected previously
                    // and now is gone. This is slightly different than if the VM
                    // was never completed but we still send down a Stop Command
                    // to ensure there's cleanup.
                    if (serverState == State.Running) {
                        // Our records showed that it should be running so let's restart
                        // it.
                        _haMgr.scheduleRestart(vm, false);
                    } else if (serverState == State.Stopping) {
                        _haMgr.scheduleStop(vm, hostId, WorkType.ForceStop);
                        s_logger.debug("Scheduling a check stop for VM in stopping mode: " + vm);
                    } else if (serverState == State.Starting) {
                        s_logger.debug("Ignoring VM in starting mode: " + vm.getInstanceName());
                        _haMgr.scheduleRestart(vm, false);
                    }
                    command = cleanup(vm);
                } else if (agentState == State.Running) {
                    if (serverState == State.Starting) {
                        if (fullSync) {
                            try {
                                ensureVmRunningContext(hostId, vm, VirtualMachine.Event.AgentReportRunning);
                            } catch (OperationTimedoutException e) {
                                s_logger.error("Exception during update for running vm: " + vm, e);
                                return null;
                            } catch (ResourceUnavailableException e) {
                                s_logger.error("Exception during update for running vm: " + vm, e);
                                return null;
                            }catch (InsufficientAddressCapacityException e) {
                                s_logger.error("Exception during update for running vm: " + vm, e);
                                return null;
                            }catch (NoTransitionException e) {
                                s_logger.warn(e.getMessage());
                            }
                        }
                    } else if (serverState == State.Stopping) {
                        s_logger.debug("Scheduling a stop command for " + vm);
                        _haMgr.scheduleStop(vm, hostId, WorkType.Stop);
                    } else {
                        s_logger.debug("server VM state " + serverState + " does not meet expectation of a running VM report from agent");

                        // just be careful not to stop VM for things we don't handle
                        // command = cleanup(vm);
                    }
                }
                return command;
        */
    }

    private void ensureVmRunningContext(long hostId, VMInstanceVO vm, Event cause) throws OperationTimedoutException, ResourceUnavailableException, NoTransitionException, InsufficientAddressCapacityException {
        /*
          	VirtualMachineGuru<VMInstanceVO> vmGuru = getVmGuru(vm);

              s_logger.debug("VM state is starting on full sync so updating it to running");
              vm = findByIdAndType(vm.getType(), vm.getId());

              // grab outstanding work item if any
              VmWorkJobVO work = _workDao.findByOutstandingWork(vm.getId(), vm.getState());
              if (work != null) {
                  if (s_logger.isDebugEnabled()) {
                      s_logger.debug("Found an outstanding work item for this vm " + vm + " in state:" + vm.getState() + ", work id:" + work.getId());
                  }
              }

              try {
                  stateTransitTo(vm, cause, hostId);
              } catch (NoTransitionException e1) {
                  s_logger.warn(e1.getMessage());
              }

              s_logger.debug("VM's " + vm + " state is starting on full sync so updating it to Running");
              vm = vmGuru.findById(vm.getId()); // this should ensure vm has the most
              // up to date info

              VirtualMachineProfile<VMInstanceVO> profile = new VirtualMachineProfileImpl<VMInstanceVO>(vm);
              List<NicVO> nics = _nicsDao.listByVmId(profile.getId());
              for (NicVO nic : nics) {
                  Network network = _networkModel.getNetwork(nic.getNetworkId());
                  NicProfile nicProfile = new NicProfile(nic, network, nic.getBroadcastUri(), nic.getIsolationUri(), null,
                          _networkModel.isSecurityGroupSupportedInNetwork(network), _networkModel.getNetworkTag(profile.getHypervisorType(), network));
                  profile.addNic(nicProfile);
              }

              Commands cmds = new Commands(OnError.Stop);
              s_logger.debug("Finalizing commands that need to be send to complete Start process for the vm " + vm);

              if (vmGuru.finalizeCommandsOnStart(cmds, profile)) {
                  if (cmds.size() != 0) {
                      _agentMgr.send(vm.getHostId(), cmds);
                  }

                  if (vmGuru.finalizeStart(profile, vm.getHostId(), cmds, null)) {
                      stateTransitTo(vm, cause, vm.getHostId());
                  } else {
                      s_logger.error("Unable to finish finialization for running vm: " + vm);
                  }
              } else {
                  s_logger.error("Unable to finalize commands on start for vm: " + vm);
              }

              if (work != null) {
                  if (s_logger.isDebugEnabled()) {
                      s_logger.debug("Updating outstanding work item to Done, id:" + work.getId());
                  }
                  work.setStep(Step.Done);
                  _workDao.update(work.getId(), work);
              }
        */
    }

    @Override
    public boolean isRecurring() {
        return true;
    }

    @Override
    public boolean processAnswers(long agentId, long seq, Answer[] answers) {
        for (final Answer answer : answers) {
            if (answer instanceof ClusterSyncAnswer) {
                ClusterSyncAnswer hs = (ClusterSyncAnswer) answer;
                if (!hs.isExceuted()){
                	
/* TODO
                    deltaSync(hs.getNewStates());
*/
                    hs.setExecuted();
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
                PingRoutingCommand ping = (PingRoutingCommand) cmd;
                if (ping.getNewStates() != null && ping.getNewStates().size() > 0) {
                	_syncMgr.processHostVmStatePingReport(agentId, ping.getNewStates());
                }
                
                // take the chance to scan VMs that are stuck in transitional states and are missing from the report
                scanStalledVMInTransitionStateOnUpHost(agentId);
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
    public void processConnect(Host agent, StartupCommand cmd, boolean forRebalance) throws ConnectionException {
        if (!(cmd instanceof StartupRoutingCommand)) {
            return;
        }
        
        if(s_logger.isDebugEnabled())
        	s_logger.debug("Received startup command from hypervisor host. host id: " + agent.getId());
        _syncMgr.resetHostSyncState(agent.getId());
    }
    
/*
    @Override
    public void processConnect(HostVO agent, StartupCommand cmd, boolean forRebalance) throws ConnectionException {
        if (!(cmd instanceof StartupRoutingCommand)) {
            return;
        }

        if (forRebalance) {
            s_logger.debug("Not processing listener " + this + " as connect happens on rebalance process");
            return;
        }

        if (forRebalance) {
            s_logger.debug("Not processing listener " + this + " as connect happens on rebalance process");
            return;
        }

        Long clusterId = agent.getClusterId();
        long agentId = agent.getId();

        if (agent.getHypervisorType() == HypervisorType.XenServer) { // only for Xen
            StartupRoutingCommand startup = (StartupRoutingCommand) cmd;

            HashMap<String, Pair<String, State>> allStates = startup.getClusterVMStateChanges();
            if (allStates != null){
                this.fullSync(clusterId, allStates);
            }
            // initiate the cron job
            ClusterSyncCommand syncCmd = new ClusterSyncCommand(Integer.parseInt(Config.ClusterDeltaSyncInterval.getDefaultValue()), clusterId);
            try {
                long seq_no = _agentMgr.send(agentId, new Commands(syncCmd), this);
                s_logger.debug("Cluster VM sync started with jobid " + seq_no);
            } catch (AgentUnavailableException e) {
                s_logger.fatal("The Cluster VM sync process failed for cluster id " + clusterId + " with ", e);
            }
        }
        else { // for others KVM and VMWare
            StartupRoutingCommand startup = (StartupRoutingCommand) cmd;

            Commands commands = fullHostSync(agentId, startup);

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
    }
*/
    
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
/*
                lock.addRef();
                List<VMInstanceVO> instances = _vmDao.findVMInTransition(new Date(new Date().getTime() - (_operationTimeout.value() * 1000)), State.Starting, State.Stopping);
                for (VMInstanceVO instance : instances) {
                    State state = instance.getState();
                    if (state == State.Stopping) {
                        _haMgr.scheduleStop(instance, instance.getHostId(), WorkType.CheckStop);
                    } else if (state == State.Starting) {
                        _haMgr.scheduleRestart(instance, true);
                    }
                }
*/
            	scanStalledVMInTransitionStateOnDisconnectedHosts();
                
            } catch (Exception e) {
                s_logger.warn("Caught the following exception on transition checking", e);
            } finally {
                lock.unlock();
            }
        }
    }

    protected class AgentVmInfo {
        public String name;
        public State state;
        public String hostUuid;
        public VMInstanceVO vm;
        public VirtualMachineGuru guru;

        public AgentVmInfo(String name, VirtualMachineGuru guru, VMInstanceVO vm, State state, String host) {
            this.name = name;
            this.state = state;
            this.vm = vm;
            this.guru = guru;
            hostUuid = host;
        }

        public AgentVmInfo(String name, VirtualMachineGuru guru, VMInstanceVO vm, State state) {
            this(name, guru, vm, state, null);
        }

        public String getHostUuid() {
            return hostUuid;
        }
    }

    @Override
    public void checkIfCanUpgrade(VirtualMachine vmInstance, long newServiceOfferingId) {
        ServiceOfferingVO newServiceOffering = _entityMgr.findById(ServiceOfferingVO.class, newServiceOfferingId);
        if (newServiceOffering == null) {
            throw new InvalidParameterValueException("Unable to find a service offering with id " + newServiceOfferingId);
        }

        // Check that the VM is stopped / running
        if (!(vmInstance.getState().equals(State.Stopped) || vmInstance.getState().equals(State.Running) )) {
            s_logger.warn("Unable to upgrade virtual machine " + vmInstance.toString() + " in state " + vmInstance.getState());
            throw new InvalidParameterValueException("Unable to upgrade virtual machine " + vmInstance.toString() + " " + " in state " + vmInstance.getState()
                    + "; make sure the virtual machine is stopped/running");
        }

        // Check if the service offering being upgraded to is what the VM is already running with
        if (vmInstance.getServiceOfferingId() == newServiceOffering.getId()) {
            if (s_logger.isInfoEnabled()) {
                s_logger.info("Not upgrading vm " + vmInstance.toString() + " since it already has the requested " +
                        "service offering (" + newServiceOffering.getName() + ")");
            }

            throw new InvalidParameterValueException("Not upgrading vm " + vmInstance.toString() + " since it already " +
                    "has the requested service offering (" + newServiceOffering.getName() + ")");
        }

        ServiceOfferingVO currentServiceOffering = _entityMgr.findByIdIncludingRemoved(ServiceOfferingVO.class, vmInstance.getServiceOfferingId());

        // Check that the service offering being upgraded to has the same Guest IP type as the VM's current service offering
        // NOTE: With the new network refactoring in 2.2, we shouldn't need the check for same guest IP type anymore.
        /*
         * if (!currentServiceOffering.getGuestIpType().equals(newServiceOffering.getGuestIpType())) { String errorMsg =
         * "The service offering being upgraded to has a guest IP type: " + newServiceOffering.getGuestIpType(); errorMsg +=
         * ". Please select a service offering with the same guest IP type as the VM's current service offering (" +
         * currentServiceOffering.getGuestIpType() + ")."; throw new InvalidParameterValueException(errorMsg); }
         */

        // Check that the service offering being upgraded to has the same storage pool preference as the VM's current service
        // offering
        if (currentServiceOffering.getUseLocalStorage() != newServiceOffering.getUseLocalStorage()) {
            throw new InvalidParameterValueException("Unable to upgrade virtual machine " + vmInstance.toString()
                    + ", cannot switch between local storage and shared storage service offerings.  Current offering " +
                    "useLocalStorage=" + currentServiceOffering.getUseLocalStorage()
                    + ", target offering useLocalStorage=" + newServiceOffering.getUseLocalStorage());
        }

        // if vm is a system vm, check if it is a system service offering, if yes return with error as it cannot be used for user vms
        if (currentServiceOffering.getSystemUse() != newServiceOffering.getSystemUse()) {
            throw new InvalidParameterValueException("isSystem property is different for current service offering and new service offering");
        }

        // Check that there are enough resources to upgrade the service offering
        if (!isVirtualMachineUpgradable(vmInstance, newServiceOffering)) {
            throw new InvalidParameterValueException("Unable to upgrade virtual machine, not enough resources available " +
                    "for an offering of " + newServiceOffering.getCpu() + " cpu(s) at "
                    + newServiceOffering.getSpeed() + " Mhz, and " + newServiceOffering.getRamSize() + " MB of memory");
        }

        // Check that the service offering being upgraded to has all the tags of the current service offering
        List<String> currentTags = StringUtils.csvTagsToList(currentServiceOffering.getTags());
        List<String> newTags = StringUtils.csvTagsToList(newServiceOffering.getTags());
        if (!newTags.containsAll(currentTags)) {
            throw new InvalidParameterValueException("Unable to upgrade virtual machine; the new service offering " +
                    "does not have all the tags of the "
                    + "current service offering. Current service offering tags: " + currentTags + "; " + "new service " +
                    "offering tags: " + newTags);
        }
    }

    @Override
    public boolean upgradeVmDb(long vmId, long serviceOfferingId) {
        VMInstanceVO vmForUpdate = _vmDao.createForUpdate();
        vmForUpdate.setServiceOfferingId(serviceOfferingId);
        ServiceOffering newSvcOff = _entityMgr.findById(ServiceOffering.class, serviceOfferingId);
        vmForUpdate.setHaEnabled(newSvcOff.getOfferHA());
        vmForUpdate.setLimitCpuUse(newSvcOff.getLimitCpuUse());
        vmForUpdate.setServiceOfferingId(newSvcOff.getId());
        return _vmDao.update(vmId, vmForUpdate);
    }

    public boolean plugNic(Network network, NicTO nic, VirtualMachineTO to, VMInstanceVO vm,
            ReservationContext context, DeployDestination dest) throws ConcurrentOperationException, ResourceUnavailableException,
            InsufficientCapacityException {
        boolean result = true;

        if (vm.getState() == State.Running) {
            try {
                PlugNicCommand plugNicCmd = new PlugNicCommand(nic, to.getName(), vm.getType());

                Commands cmds = new Commands(OnError.Stop);
                cmds.addCommand("plugnic", plugNicCmd);
                _agentMgr.send(dest.getHost().getId(), cmds);
                PlugNicAnswer plugNicAnswer = cmds.getAnswer(PlugNicAnswer.class);
                if (!(plugNicAnswer != null && plugNicAnswer.getResult())) {
                    s_logger.warn("Unable to plug nic for vm " + to.getName());
                    result = false;
                }
            } catch (OperationTimedoutException e) {
                throw new AgentUnavailableException("Unable to plug nic for router " + to.getName() + " in network " + network,
                        dest.getHost().getId(), e);
            }
        } else {
            s_logger.warn("Unable to apply PlugNic, vm " + vm + " is not in the right state " + vm.getState());

            throw new ResourceUnavailableException("Unable to apply PlugNic on the backend," +
                    " vm " + to + " is not in the right state", DataCenter.class, vm.getDataCenterId());
        }

        return result;
    }

    protected boolean unplugNic(Network network, NicTO nic, VirtualMachineTO to, VMInstanceVO vm,
            ReservationContext context, DeployDestination dest) throws ConcurrentOperationException, ResourceUnavailableException {

        boolean result = true;

        if (vm.getState() == State.Running) {
            try {
                Commands cmds = new Commands(OnError.Stop);
                UnPlugNicCommand unplugNicCmd = new UnPlugNicCommand(nic, to.getName());
                cmds.addCommand("unplugnic", unplugNicCmd);
                _agentMgr.send(dest.getHost().getId(), cmds);

                UnPlugNicAnswer unplugNicAnswer = cmds.getAnswer(UnPlugNicAnswer.class);
                if (!(unplugNicAnswer != null && unplugNicAnswer.getResult())) {
                    s_logger.warn("Unable to unplug nic from router " + vm);
                    result = false;
                }
            } catch (OperationTimedoutException e) {
                throw new AgentUnavailableException("Unable to unplug nic from rotuer " + vm + " from network " + network,
                        dest.getHost().getId(), e);
            }
        } else if (vm.getState() == State.Stopped || vm.getState() == State.Stopping) {
            s_logger.debug("Vm " + vm.getInstanceName() + " is in " + vm.getState() +
                    ", so not sending unplug nic command to the backend");
        } else {
            s_logger.warn("Unable to apply unplug nic, Vm " + vm + " is not in the right state " + vm.getState());

            throw new ResourceUnavailableException("Unable to apply unplug nic on the backend," +
                    " vm " + vm + " is not in the right state", DataCenter.class, vm.getDataCenterId());
        }

        return result;
    }

    @Override
    public NicProfile addVmToNetwork(VirtualMachine vm, Network network, NicProfile requested) throws ConcurrentOperationException,
    ResourceUnavailableException, InsufficientCapacityException {
        CallContext cc = CallContext.current();

        s_logger.debug("Adding vm " + vm + " to network " + network + "; requested nic profile " + requested);
        VMInstanceVO vmVO = _vmDao.findById(vm.getId());
        ReservationContext context = new ReservationContextImpl(null, null, cc.getCallingUser(), cc.getCallingAccount());

        VirtualMachineProfileImpl vmProfile = new VirtualMachineProfileImpl(vmVO, null, null, null, null);

        DataCenter dc = _entityMgr.findById(DataCenter.class, network.getDataCenterId());
        Host host = _hostDao.findById(vm.getHostId());
        DeployDestination dest = new DeployDestination(dc, null, null, host);

        //check vm state
        if (vm.getState() == State.Running) {
            //1) allocate and prepare nic
            NicProfile nic = _networkMgr.createNicForVm(network, requested, context, vmProfile, true);

            //2) Convert vmProfile to vmTO
            HypervisorGuru hvGuru = _hvGuruMgr.getGuru(vmProfile.getVirtualMachine().getHypervisorType());
            VirtualMachineTO vmTO = hvGuru.implement(vmProfile);

            //3) Convert nicProfile to NicTO
            NicTO nicTO = toNicTO(nic, vmProfile.getVirtualMachine().getHypervisorType());

            s_logger.debug("Plugging nic for vm " + vm + " in network " + network);
            
            boolean result = false;
            try{
                result = plugNic(network, nicTO, vmTO, vmVO, context, dest);
                if (result) {
                s_logger.debug("Nic is plugged successfully for vm " + vm + " in network " + network + ". Vm  is a part of network now");
                    long isDefault = (nic.isDefaultNic()) ? 1 : 0;
                    // insert nic's Id into DB as resource_name
                    //FIXME
//                    UsageEventUtils.publishUsageEvent(EventTypes.EVENT_NETWORK_OFFERING_ASSIGN, vmVO.getAccountId(),
//                            vmVO.getDataCenterId(), vmVO.getId(), Long.toString(nic.getId()), network.getNetworkOfferingId(),
//                            null, isDefault, VirtualMachine.class.getName(), vmVO.getUuid());
                return nic;
            } else {
                s_logger.warn("Failed to plug nic to the vm " + vm + " in network " + network);
                return null;
            }
            }finally{
                if(!result){
                    _networkMgr.removeNic(vmProfile, _nicsDao.findById(nic.getId()));
                }
            }
        } else if (vm.getState() == State.Stopped) {
            //1) allocate nic
            return _networkMgr.createNicForVm(network, requested, context, vmProfile, false);
        } else {
            s_logger.warn("Unable to add vm " + vm + " to network  " + network);
            throw new ResourceUnavailableException("Unable to add vm " + vm + " to network, is not in the right state",
                    DataCenter.class, vm.getDataCenterId());
        }
    }

    @Override
    public NicTO toNicTO(NicProfile nic, HypervisorType hypervisorType) {
        HypervisorGuru hvGuru = _hvGuruMgr.getGuru(hypervisorType);

        NicTO nicTO = hvGuru.toNicTO(nic);
        return nicTO;
    }

    @Override
    public boolean removeNicFromVm(VirtualMachine vm, NicVO nic) throws ConcurrentOperationException, ResourceUnavailableException {
        CallContext cc = CallContext.current();

        VMInstanceVO vmVO = _vmDao.findById(vm.getId());
        NetworkVO network = _entityMgr.findById(NetworkVO.class, nic.getNetworkId());
        ReservationContext context = new ReservationContextImpl(null, null, cc.getCallingUser(), cc.getCallingAccount());

        VirtualMachineProfileImpl vmProfile = new VirtualMachineProfileImpl(vmVO, null,
                null, null, null);

        DataCenter dc = _entityMgr.findById(DataCenter.class, network.getDataCenterId());
        Host host = _hostDao.findById(vm.getHostId());
        DeployDestination dest = new DeployDestination(dc, null, null, host);
        HypervisorGuru hvGuru = _hvGuruMgr.getGuru(vmProfile.getVirtualMachine().getHypervisorType());
        VirtualMachineTO vmTO = hvGuru.implement(vmProfile);

        // don't delete default NIC on a user VM
        if (nic.isDefaultNic() && vm.getType() == VirtualMachine.Type.User ) {
            s_logger.warn("Failed to remove nic from " + vm + " in " + network + ", nic is default.");
            throw new CloudRuntimeException("Failed to remove nic from " + vm + " in " + network + ", nic is default.");
        }

        // if specified nic is associated with PF/LB/Static NAT
        if(_rulesMgr.listAssociatedRulesForGuestNic(nic).size() > 0){
            throw new CloudRuntimeException("Failed to remove nic from " + vm + " in " + network
                    + ", nic has associated Port forwarding or Load balancer or Static NAT rules.");
        }

        NicProfile nicProfile = new NicProfile(nic, network, nic.getBroadcastUri(), nic.getIsolationUri(),
                _networkModel.getNetworkRate(network.getId(), vm.getId()),
                _networkModel.isSecurityGroupSupportedInNetwork(network),
                _networkModel.getNetworkTag(vmProfile.getVirtualMachine().getHypervisorType(), network));

        //1) Unplug the nic
        if (vm.getState() == State.Running) {
            NicTO nicTO = toNicTO(nicProfile, vmProfile.getVirtualMachine().getHypervisorType());
            s_logger.debug("Un-plugging nic " + nic + " for vm " + vm + " from network " + network);
            boolean result = unplugNic(network, nicTO, vmTO, vmVO, context, dest);
            if (result) {
                s_logger.debug("Nic is unplugged successfully for vm " + vm + " in network " + network );
                long isDefault = (nic.isDefaultNic()) ? 1 : 0;
                //FIXME
//                UsageEventUtils.publishUsageEvent(EventTypes.EVENT_NETWORK_OFFERING_REMOVE, vm.getAccountId(), vm.getDataCenterId(),
//                        vm.getId(), Long.toString(nic.getId()), network.getNetworkOfferingId(), null,
//                        isDefault, VirtualMachine.class.getName(), vm.getUuid());
            } else {
                s_logger.warn("Failed to unplug nic for the vm " + vm + " from network " + network);
                return false;
            }
        } else if (vm.getState() != State.Stopped) {
            s_logger.warn("Unable to remove vm " + vm + " from network  " + network);
            throw new ResourceUnavailableException("Unable to remove vm " + vm + " from network, is not in the right state",
                    DataCenter.class, vm.getDataCenterId());
        }

        //2) Release the nic
        _networkMgr.releaseNic(vmProfile, nic);
        s_logger.debug("Successfully released nic " + nic +  "for vm " + vm);

        //3) Remove the nic
        _networkMgr.removeNic(vmProfile, nic);
        _nicsDao.expunge(nic.getId());
        return true;
    }

    @Override
    public boolean removeVmFromNetwork(VirtualMachine vm, Network network, URI broadcastUri) throws ConcurrentOperationException, ResourceUnavailableException {
        CallContext cc = CallContext.current();

        VMInstanceVO vmVO = _vmDao.findById(vm.getId());
        ReservationContext context = new ReservationContextImpl(null, null, cc.getCallingUser(), cc.getCallingAccount());

        VirtualMachineProfileImpl vmProfile = new VirtualMachineProfileImpl(vmVO, null,
                null, null, null);

        DataCenter dc = _entityMgr.findById(DataCenter.class, network.getDataCenterId());
        Host host = _hostDao.findById(vm.getHostId());
        DeployDestination dest = new DeployDestination(dc, null, null, host);
        HypervisorGuru hvGuru = _hvGuruMgr.getGuru(vmProfile.getVirtualMachine().getHypervisorType());
        VirtualMachineTO vmTO = hvGuru.implement(vmProfile);

        Nic nic = null;

        if (broadcastUri != null) {
            nic = _nicsDao.findByNetworkIdInstanceIdAndBroadcastUri(network.getId(), vm.getId(), broadcastUri.toString());
        } else {
            nic = _networkModel.getNicInNetwork(vm.getId(), network.getId());
        }

        if (nic == null){
            s_logger.warn("Could not get a nic with " + network);
            return false;
        }

        // don't delete default NIC on a user VM
        if (nic.isDefaultNic() && vm.getType() == VirtualMachine.Type.User ) {
            s_logger.warn("Failed to remove nic from " + vm + " in " + network + ", nic is default.");
            throw new CloudRuntimeException("Failed to remove nic from " + vm + " in " + network + ", nic is default.");
        }

        NicProfile nicProfile = new NicProfile(nic, network, nic.getBroadcastUri(), nic.getIsolationUri(),
                _networkModel.getNetworkRate(network.getId(), vm.getId()),
                _networkModel.isSecurityGroupSupportedInNetwork(network),
                _networkModel.getNetworkTag(vmProfile.getVirtualMachine().getHypervisorType(), network));

        //1) Unplug the nic
        if (vm.getState() == State.Running) {
            NicTO nicTO = toNicTO(nicProfile, vmProfile.getVirtualMachine().getHypervisorType());
            s_logger.debug("Un-plugging nic for vm " + vm + " from network " + network);
            boolean result = unplugNic(network, nicTO, vmTO, vmVO, context, dest);
            if (result) {
                s_logger.debug("Nic is unplugged successfully for vm " + vm + " in network " + network );
            } else {
                s_logger.warn("Failed to unplug nic for the vm " + vm + " from network " + network);
                return false;
            }
        } else if (vm.getState() != State.Stopped) {
            s_logger.warn("Unable to remove vm " + vm + " from network  " + network);
            throw new ResourceUnavailableException("Unable to remove vm " + vm + " from network, is not in the right state",
                    DataCenter.class, vm.getDataCenterId());
        }

        //2) Release the nic
        _networkMgr.releaseNic(vmProfile, nic);
        s_logger.debug("Successfully released nic " + nic +  "for vm " + vm);

        //3) Remove the nic
        _networkMgr.removeNic(vmProfile, nic);
        return true;
    }

    @Override
    public boolean findHostAndMigrate(String vmUuid, Long newSvcOfferingId)
            throws InsufficientCapacityException, ConcurrentOperationException, ResourceUnavailableException, VirtualMachineMigrationException, ManagementServerException {
        VMInstanceVO vm = _vmDao.findByUuid(vmUuid);

        VirtualMachineProfile profile = new VirtualMachineProfileImpl(vm);

        Long srcHostId = vm.getHostId();
        Long oldSvcOfferingId = vm.getServiceOfferingId();
        if (srcHostId == null) {
            throw new CloudRuntimeException("Unable to scale the vm because it doesn't have a host id");
        }
        Host host = _hostDao.findById(srcHostId);
        DataCenterDeployment plan = new DataCenterDeployment(host.getDataCenterId(), host.getPodId(), host.getClusterId(), null, null, null);
        ExcludeList excludes = new ExcludeList();
        excludes.addHost(vm.getHostId());
        vm.setServiceOfferingId(newSvcOfferingId); // Need to find the destination host based on new svc offering

        DeployDestination dest = null;

        for (DeploymentPlanner planner : _planners) {
            if (planner.canHandle(profile, plan, excludes)) {
                dest = planner.plan(profile, plan, excludes);
            } else {
                continue;
            }

            if (dest != null) {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Planner " + planner + " found " + dest + " for scaling the vm to.");
                }
                break;
            }
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Planner " + planner + " was unable to find anything.");
            }
        }

        if (dest == null) {
            throw new InsufficientServerCapacityException("Unable to find a server to scale the vm to.", host.getClusterId());
        }

        excludes.addHost(dest.getHost().getId());
        try {
            return migrateForScale(vm.getUuid(), srcHostId, dest, oldSvcOfferingId);
        } catch (ResourceUnavailableException e) {
            s_logger.debug("Unable to migrate to unavailable " + dest);
            throw e;
        } catch (ConcurrentOperationException e) {
            s_logger.debug("Unable to migrate VM due to: " + e.getMessage());
            throw e;
        } catch (ManagementServerException e) {
            s_logger.debug("Unable to migrate VM: " + e.getMessage());
            throw e;
        } catch (VirtualMachineMigrationException e) {
            s_logger.debug("Got VirtualMachineMigrationException, Unable to migrate: " + e.getMessage());
            if (vm.getState() == State.Starting) {
                s_logger.debug("VM seems to be still Starting, we should retry migration later");
                throw e;
            } else {
                s_logger.debug("Unable to migrate VM, VM is not in Running or even Starting state, current state: " + vm.getState().toString());
                return false;
            }
        }
    }

        @Override
    public boolean migrateForScale(String vmUuid, long srcHostId, DeployDestination dest, Long oldSvcOfferingId) throws ResourceUnavailableException,
            ConcurrentOperationException, ManagementServerException,
                VirtualMachineMigrationException {
//            s_logger.info("Migrating " + vm + " to " + dest);
        return false;
        /*
                    Long newSvcOfferingId = vm.getServiceOfferingId();
                    long dstHostId = dest.getHost().getId();
                    Host fromHost = _hostDao.findById(srcHostId);
                    if (fromHost == null) {
                        s_logger.info("Unable to find the host to migrate from: " + srcHostId);
                        throw new CloudRuntimeException("Unable to find the host to migrate from: " + srcHostId);
                    }

                    if (fromHost.getClusterId().longValue() != dest.getCluster().getId()) {
                        s_logger.info("Source and destination host are not in same cluster, unable to migrate to host: " + dest.getHost().getId());
                        throw new CloudRuntimeException("Source and destination host are not in same cluster, unable to migrate to host: " + dest.getHost().getId());
                    }

                    VirtualMachineGuru<T> vmGuru = getVmGuru(vm);

                    long vmId = vm.getId();
                    vm = vmGuru.findById(vmId);
                    if (vm == null) {
                        if (s_logger.isDebugEnabled()) {
                            s_logger.debug("Unable to find the vm " + vm);
                        }
                        throw new ManagementServerException("Unable to find a virtual machine with id " + vmId);
                    }

                    if (vm.getState() != State.Running) {
                        if (s_logger.isDebugEnabled()) {
                            s_logger.debug("VM is not Running, unable to migrate the vm " + vm);
                        }
                        throw new VirtualMachineMigrationException("VM is not Running, unable to migrate the vm currently " + vm + " , current state: " + vm.getState().toString());
                    }

                    short alertType = AlertManager.ALERT_TYPE_USERVM_MIGRATE;
                    if (VirtualMachine.Type.DomainRouter.equals(vm.getType())) {
                        alertType = AlertManager.ALERT_TYPE_DOMAIN_ROUTER_MIGRATE;
                    } else if (VirtualMachine.Type.ConsoleProxy.equals(vm.getType())) {
                        alertType = AlertManager.ALERT_TYPE_CONSOLE_PROXY_MIGRATE;
                    }

                    VirtualMachineProfile<VMInstanceVO> profile = new VirtualMachineProfileImpl<VMInstanceVO>(vm);
                    _networkMgr.prepareNicForMigration(profile, dest);
                    this.volumeMgr.prepareForMigration(profile, dest);

                    VirtualMachineTO to = toVmTO(profile);
                    PrepareForMigrationCommand pfmc = new PrepareForMigrationCommand(to);

                    VmWorkJobVO work = new VmWorkJobVO(UUID.randomUUID().toString(), _nodeId, State.Migrating, vm.getType(), vm.getId());
                    work.setStep(Step.Prepare);
                    work.setResourceType(VmWorkJobVO.ResourceType.Host);
                    work.setResourceId(dstHostId);
                    work = _workDao.persist(work);

                    PrepareForMigrationAnswer pfma = null;
                    try {
                        pfma = (PrepareForMigrationAnswer) _agentMgr.send(dstHostId, pfmc);
                        if (!pfma.getResult()) {
                            String msg = "Unable to prepare for migration due to " + pfma.getDetails();
                            pfma = null;
                            throw new AgentUnavailableException(msg, dstHostId);
                        }
                    } catch (OperationTimedoutException e1) {
                        throw new AgentUnavailableException("Operation timed out", dstHostId);
                    } finally {
                        if (pfma == null) {
                            work.setStep(Step.Done);
                            _workDao.update(work.getId(), work);
                        }
                    }

                    vm.setLastHostId(srcHostId);
                    try {
                        if (vm == null || vm.getHostId() == null || vm.getHostId() != srcHostId || !changeState(vm, VirtualMachine.Event.MigrationRequested, dstHostId, work, Step.Migrating)) {
                            s_logger.info("Migration cancelled because state has changed: " + vm);
                            throw new ConcurrentOperationException("Migration cancelled because state has changed: " + vm);
                        }
                    } catch (NoTransitionException e1) {
                        s_logger.info("Migration cancelled because " + e1.getMessage());
                        throw new ConcurrentOperationException("Migration cancelled because " + e1.getMessage());
                    }

                    boolean migrated = false;
                    try {
                        boolean isWindows = _guestOsCategoryDao.findById(_guestOsDao.findById(vm.getGuestOSId()).getCategoryId()).getName().equalsIgnoreCase("Windows");
                        MigrateCommand mc = new MigrateCommand(vm.getInstanceName(), dest.getHost().getPrivateIpAddress(), isWindows);
                        mc.setHostGuid(dest.getHost().getGuid());

                        try {
                            MigrateAnswer ma = (MigrateAnswer) _agentMgr.send(vm.getLastHostId(), mc);
                            if (!ma.getResult()) {
                                s_logger.error("Unable to migrate due to " + ma.getDetails());
                                return null;
                            }
                        } catch (OperationTimedoutException e) {
                            if (e.isActive()) {
                                s_logger.warn("Active migration command so scheduling a restart for " + vm);
                                _haMgr.scheduleRestart(vm, true);
                            }
                            throw new AgentUnavailableException("Operation timed out on migrating " + vm, dstHostId);
                        }

                        try {
                            long newServiceOfferingId = vm.getServiceOfferingId();
                            vm.setServiceOfferingId(oldSvcOfferingId); // release capacity for the old service offering only
                            if (!changeState(vm, VirtualMachine.Event.OperationSucceeded, dstHostId, work, Step.Started)) {
                                throw new ConcurrentOperationException("Unable to change the state for " + vm);
                            }
                            vm.setServiceOfferingId(newServiceOfferingId);
                        } catch (NoTransitionException e1) {
                            throw new ConcurrentOperationException("Unable to change state due to " + e1.getMessage());
                        }

                        try {
                            if (!checkVmOnHost(vm, dstHostId)) {
                                s_logger.error("Unable to complete migration for " + vm);
                                try {
                                    _agentMgr.send(srcHostId, new Commands(cleanup(vm.getInstanceName())), null);
                                } catch (AgentUnavailableException e) {
                                    s_logger.error("AgentUnavailableException while cleanup on source host: " + srcHostId);
                                }
                                cleanup(vmGuru, new VirtualMachineProfileImpl(vm), work, VirtualMachine.Event.AgentReportStopped, true, _accountMgr.getSystemUser(), _accountMgr.getSystemAccount());
                                return null;
                            }
                        } catch (OperationTimedoutException e) {
                        }

                        migrated = true;
                        return vm;
                    } finally {
                        if (!migrated) {
                            s_logger.info("Migration was unsuccessful.  Cleaning up: " + vm);

                            _alertMgr.sendAlert(alertType, fromHost.getDataCenterId(), fromHost.getPodId(), "Unable to migrate vm " + vm.getInstanceName() + " from host " + fromHost.getName() + " in zone "
                                    + dest.getDataCenter().getName() + " and pod " + dest.getPod().getName(), "Migrate Command failed.  Please check logs.");
                            try {
                                _agentMgr.send(dstHostId, new Commands(cleanup(vm.getInstanceName())), null);
                            } catch (AgentUnavailableException ae) {
                                s_logger.info("Looks like the destination Host is unavailable for cleanup");
                            }

                            try {
                                stateTransitTo(vm, VirtualMachine.Event.OperationFailed, srcHostId);
                            } catch (NoTransitionException e) {
                                s_logger.warn(e.getMessage());
                            }
                        }

                        work.setStep(Step.Done);
                        _workDao.update(work.getId(), work);
                    }
        */
        }
    @Override
    public boolean reConfigureVm(VirtualMachine vm, ServiceOffering oldServiceOffering, boolean reconfiguringOnExistingHost) throws ResourceUnavailableException,
            ConcurrentOperationException {
        /*
                long newServiceofferingId = vm.getServiceOfferingId();
                ServiceOffering newServiceOffering = _configMgr.getServiceOffering(newServiceofferingId);
                ScaleVmCommand reconfigureCmd = new ScaleVmCommand(vm.getInstanceName(), newServiceOffering.getCpu(),
                        newServiceOffering.getSpeed(), newServiceOffering.getRamSize(), newServiceOffering.getRamSize(), newServiceOffering.getLimitCpuUse());

                Long dstHostId = vm.getHostId();
                VmWorkJobVO work = new VmWorkJobVO(UUID.randomUUID().toString(), _nodeId, State.Running, vm.getType(), vm.getId());
                work.setStep(Step.Prepare);
                work.setResourceType(VmWorkJobVO.ResourceType.Host);
                work.setResourceId(vm.getHostId());
                work = _workDao.persist(work);
                boolean success = false;
                try {
                    if(reconfiguringOnExistingHost){
                        vm.setServiceOfferingId(oldServiceOffering.getId());
                        _capacityMgr.releaseVmCapacity(vm, false, false, vm.getHostId()); //release the old capacity
                        vm.setServiceOfferingId(newServiceofferingId);
                        _capacityMgr.allocateVmCapacity(vm, false); // lock the new capacity
                    }

                    Answer reconfigureAnswer = _agentMgr.send(vm.getHostId(), reconfigureCmd);
                    if (reconfigureAnswer == null || !reconfigureAnswer.getResult()) {
                        s_logger.error("Unable to scale vm due to " + (reconfigureAnswer == null ? "" : reconfigureAnswer.getDetails()));
                        throw new CloudRuntimeException("Unable to scale vm due to " + (reconfigureAnswer == null ? "" : reconfigureAnswer.getDetails()));
                    }

                    success = true;
                } catch (OperationTimedoutException e) {
                    throw new AgentUnavailableException("Operation timed out on reconfiguring " + vm, dstHostId);
                } catch (AgentUnavailableException e) {
                    throw e;
                } finally{
                   // work.setStep(Step.Done);
                    //_workDao.update(work.getId(), work);
                    if(!success){
                        _capacityMgr.releaseVmCapacity(vm, false, false, vm.getHostId()); // release the new capacity
                        vm.setServiceOfferingId(oldServiceOffering.getId());
                        _capacityMgr.allocateVmCapacity(vm, false); // allocate the old capacity
                    }
                }
        */
        return false;

    }
    
    //
    // PowerState report handling for out-of-band changes and handling of left-over transitional VM states
    //
    
    @MessageHandler(topic = Topics.VM_POWER_STATE)
    private void HandlePownerStateReport(Object target, String subject, String senderAddress, Object args) {
    	assert(args != null);
    	Long vmId = (Long)args;
    	
    	List<VmWorkJobVO> pendingWorkJobs = _workJobDao.listPendingWorkJobs(
    		VirtualMachine.Type.Instance, vmId);
    	if(pendingWorkJobs.size() == 0) {
    		// there is no pending operation job
            VMInstanceVO vm = _vmDao.findById(vmId);
    		if(vm != null) {
    			switch(vm.getPowerState()) {
    			case PowerOn :
    				handlePowerOnReportWithNoPendingJobsOnVM(vm);
    				break;
    				
    			case PowerOff :
    				handlePowerOffReportWithNoPendingJobsOnVM(vm);
    				break;

    			// PowerUnknown shouldn't be reported, it is a derived
    			// VM power state from host state (host un-reachable
    			case PowerUnknown :
    			default :
    				assert(false);
    				break;
    			}
    		} else {
    			s_logger.warn("VM " + vmId + " no longer exists when processing VM state report");
    		}
    	} else {
    		// TODO, do job wake-up signalling, since currently async job wake-up is not in use
    		// we will skip it for nows
    	}
    }
    
    private void handlePowerOnReportWithNoPendingJobsOnVM(VMInstanceVO vm) {
    	//
    	// 	1) handle left-over transitional VM states
    	//	2) handle out of band VM live migration
    	//	3) handle out of sync stationary states, marking VM from Stopped to Running with
    	//	   alert messages
    	//
    	switch(vm.getState()) {
    	case Starting :
    		try {
    			stateTransitTo(vm, VirtualMachine.Event.FollowAgentPowerOnReport, vm.getPowerHostId());
    		} catch(NoTransitionException e) {
    			s_logger.warn("Unexpected VM state transition exception, race-condition?", e);
    		}
    		
    		// we need to alert admin or user about this risky state transition
    		_alertMgr.sendAlert(AlertManager.ALERT_TYPE_SYNC, vm.getDataCenterId(), vm.getPodIdToDeployIn(),
    			VM_SYNC_ALERT_SUBJECT, "VM " + vm.getHostName() + "(" + vm.getInstanceName() + ") state is sync-ed (Starting -> Running) from out-of-context transition. VM network environment may need to be reset");
    		break;
    		
    	case Running :
    		try {
    			if(vm.getHostId() != null && vm.getHostId().longValue() != vm.getPowerHostId().longValue())
    				s_logger.info("Detected out of band VM migration from host " + vm.getHostId() + " to host " + vm.getPowerHostId());
    			stateTransitTo(vm, VirtualMachine.Event.FollowAgentPowerOnReport, vm.getPowerHostId());
    		} catch(NoTransitionException e) {
    			s_logger.warn("Unexpected VM state transition exception, race-condition?", e);
    		}
    		break;
    		
    	case Stopping :
    	case Stopped :
    		try {
    			stateTransitTo(vm, VirtualMachine.Event.FollowAgentPowerOnReport, vm.getPowerHostId());
    		} catch(NoTransitionException e) {
    			s_logger.warn("Unexpected VM state transition exception, race-condition?", e);
    		}
      		_alertMgr.sendAlert(AlertManager.ALERT_TYPE_SYNC, vm.getDataCenterId(), vm.getPodIdToDeployIn(),
        			VM_SYNC_ALERT_SUBJECT, "VM " + vm.getHostName() + "(" + vm.getInstanceName() + ") state is sync-ed (" + vm.getState() + " -> Running) from out-of-context transition. VM network environment may need to be reset");
          	break;
    		
    	case Destroyed :
    	case Expunging :
    		s_logger.info("Receive power on report when VM is in destroyed or expunging state. vm: "
        		    + vm.getId() + ", state: " + vm.getState());
    		break;
    		
    	case Migrating :
    		try {
    			stateTransitTo(vm, VirtualMachine.Event.FollowAgentPowerOnReport, vm.getPowerHostId());
    		} catch(NoTransitionException e) {
    			s_logger.warn("Unexpected VM state transition exception, race-condition?", e);
    		}
    		break;
    		
    	case Error :
    	default :
    		s_logger.info("Receive power on report when VM is in error or unexpected state. vm: "
    		    + vm.getId() + ", state: " + vm.getState());
    		break;
    	}
    }
    
    private void handlePowerOffReportWithNoPendingJobsOnVM(VMInstanceVO vm) {

    	// 	1) handle left-over transitional VM states
    	//	2) handle out of sync stationary states, schedule force-stop to release resources
    	//
    	switch(vm.getState()) {
    	case Starting :
    	case Stopping :
    	case Stopped :
    	case Migrating :
    		try {
    			stateTransitTo(vm, VirtualMachine.Event.FollowAgentPowerOffReport, vm.getPowerHostId());
    		} catch(NoTransitionException e) {
    			s_logger.warn("Unexpected VM state transition exception, race-condition?", e);
    		}
      		_alertMgr.sendAlert(AlertManager.ALERT_TYPE_SYNC, vm.getDataCenterId(), vm.getPodIdToDeployIn(),
        			VM_SYNC_ALERT_SUBJECT, "VM " + vm.getHostName() + "(" + vm.getInstanceName() + ") state is sync-ed (" + vm.getState() + " -> Stopped) from out-of-context transition.");
      		// TODO: we need to forcely release all resource allocation
          	break;
    		
    	case Running :
    	case Destroyed :
    	case Expunging :
    		break;
    		
    	case Error :
    	default :
    		break;
    	}
    }
    
    private void scanStalledVMInTransitionStateOnUpHost(long hostId) {
    	//
    	// Check VM that is stuck in Starting, Stopping, Migrating states, we won't check
    	// VMs in expunging state (this need to be handled specially)
    	//
    	// checking condition
    	//	1) no pending VmWork job
    	//	2) on hostId host and host is UP
    	//
    	// When host is UP, soon or later we will get a report from the host about the VM,
    	// however, if VM is missing from the host report (it may happen in out of band changes
    	// or from designed behave of XS/KVM), the VM may not get a chance to run the state-sync logic
    	//
    	// Therefor, we will scan thoses VMs on UP host based on last update timestamp, if the host is UP
    	// and a VM stalls for status update, we will consider them to be powered off
    	// (which is relatively safe to do so)
    	
    	long stallThresholdInMs = _pingInterval.value() + (_pingInterval.value() >> 1);
    	Date cutTime = new Date(DateUtil.currentGMTTime().getTime() - stallThresholdInMs);
    	List<Long> mostlikelyStoppedVMs = listStalledVMInTransitionStateOnUpHost(hostId, cutTime);
    	for(Long vmId : mostlikelyStoppedVMs) {
    		VMInstanceVO vm = _vmDao.findById(vmId);
    		assert(vm != null);
    		handlePowerOffReportWithNoPendingJobsOnVM(vm);
    	}
    	
    	List<Long> vmsWithRecentReport = listVMInTransitionStateWithRecentReportOnUpHost(hostId, cutTime);
    	for(Long vmId : vmsWithRecentReport) {
    		VMInstanceVO vm = _vmDao.findById(vmId);
    		assert(vm != null);
    		if(vm.getPowerState() == PowerState.PowerOn)
    			handlePowerOnReportWithNoPendingJobsOnVM(vm);
    		else
    			handlePowerOffReportWithNoPendingJobsOnVM(vm);
    	}
    }
    
    private void scanStalledVMInTransitionStateOnDisconnectedHosts() {
    	Date cutTime = new Date(DateUtil.currentGMTTime().getTime() - _operationTimeout.value()*1000);
    	List<Long> stuckAndUncontrollableVMs = listStalledVMInTransitionStateOnDisconnectedHosts(cutTime);
    	for(Long vmId : stuckAndUncontrollableVMs) {
    		VMInstanceVO vm = _vmDao.findById(vmId);
    		
    		// We now only alert administrator about this situation
      		_alertMgr.sendAlert(AlertManager.ALERT_TYPE_SYNC, vm.getDataCenterId(), vm.getPodIdToDeployIn(),
        		VM_SYNC_ALERT_SUBJECT, "VM " + vm.getHostName() + "(" + vm.getInstanceName() + ") is stuck in " + vm.getState() + " state and its host is unreachable for too long");
    	}
    }
    
    
    // VMs that in transitional state without recent power state report
    private List<Long> listStalledVMInTransitionStateOnUpHost(long hostId, Date cutTime) {
    	String sql = "SELECT i.* FROM vm_instance as i, host as h WHERE h.status = 'UP' " +
                     "AND h.id = ? AND i.power_state_update_time < ? AND i.host_id = h.id " +
    			     "AND (i.state ='Starting' OR i.state='Stopping' OR i.state='Migrating') " +
    			     "AND i.id NOT IN (SELECT w.vm_instance_id FROM vm_work_job AS w JOIN async_job AS j ON w.id = j.id WHERE j.job_status = ?)";
    	
    	List<Long> l = new ArrayList<Long>();
    	Transaction txn = null;
    	try {
    		txn = Transaction.open(Transaction.CLOUD_DB);
    	
	        PreparedStatement pstmt = null;
	        try {
	            pstmt = txn.prepareAutoCloseStatement(sql);
	            
	            pstmt.setLong(1, hostId);
	 	        pstmt.setString(2, DateUtil.getDateDisplayString(TimeZone.getTimeZone("GMT"), cutTime));
	 	        pstmt.setInt(3, JobInfo.Status.IN_PROGRESS.ordinal());
	            ResultSet rs = pstmt.executeQuery();
	            while(rs.next()) {
	            	l.add(rs.getLong(1));
	            }
	        } catch (SQLException e) {
	        } catch (Throwable e) {
	        }
        
    	} finally {
    		if(txn != null)
    			txn.close();
    	}
        return l;
    }
    
    // VMs that in transitional state and recently have power state update
    private List<Long> listVMInTransitionStateWithRecentReportOnUpHost(long hostId, Date cutTime) {
    	String sql = "SELECT i.* FROM vm_instance as i, host as h WHERE h.status = 'UP' " +
                     "AND h.id = ? AND i.power_state_update_time > ? AND i.host_id = h.id " +
    			     "AND (i.state ='Starting' OR i.state='Stopping' OR i.state='Migrating') " +
    			     "AND i.id NOT IN (SELECT w.vm_instance_id FROM vm_work_job AS w JOIN async_job AS j ON w.id = j.id WHERE j.job_status = ?)";
    	
    	List<Long> l = new ArrayList<Long>();
    	Transaction txn = null;
    	try {
    		txn = Transaction.open(Transaction.CLOUD_DB);
	        PreparedStatement pstmt = null;
	        try {
	            pstmt = txn.prepareAutoCloseStatement(sql);
	            
	            pstmt.setLong(1, hostId);
	 	        pstmt.setString(2, DateUtil.getDateDisplayString(TimeZone.getTimeZone("GMT"), cutTime));
	 	        pstmt.setInt(3, JobInfo.Status.IN_PROGRESS.ordinal());
	            ResultSet rs = pstmt.executeQuery();
	            while(rs.next()) {
	            	l.add(rs.getLong(1));
	            }
	        } catch (SQLException e) {
	        } catch (Throwable e) {
	        }
	        return l;
    	} finally {
    		if(txn != null)
    			txn.close();
    	}
    }
    
    private List<Long> listStalledVMInTransitionStateOnDisconnectedHosts(Date cutTime) {
    	String sql = "SELECT i.* FROM vm_instance as i, host as h WHERE h.status != 'UP' " +
                 "AND i.power_state_update_time < ? AND i.host_id = h.id " +
			     "AND (i.state ='Starting' OR i.state='Stopping' OR i.state='Migrating') " +
			     "AND i.id NOT IN (SELECT w.vm_instance_id FROM vm_work_job AS w JOIN async_job AS j ON w.id = j.id WHERE j.job_status = ?)";
	
    	List<Long> l = new ArrayList<Long>();
    	Transaction txn = null;
    	try {
    		txn = Transaction.open(Transaction.CLOUD_DB);
	    	PreparedStatement pstmt = null;
	    	try {
		       pstmt = txn.prepareAutoCloseStatement(sql);
		       
		       pstmt.setString(1, DateUtil.getDateDisplayString(TimeZone.getTimeZone("GMT"), cutTime));
		       pstmt.setInt(2, JobInfo.Status.IN_PROGRESS.ordinal());
		       ResultSet rs = pstmt.executeQuery();
		       while(rs.next()) {
		       	l.add(rs.getLong(1));
		       }
	    	} catch (SQLException e) {
	    	} catch (Throwable e) {
	    	}
	    	return l;
    	} finally {
    		if(txn != null)
    			txn.close();
    	}
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey<?>[] {StartRetry, VmOpWaitInterval, VmOpLockStateRetry, VmOpCleanupInterval, VmOpCleanupWait, VmOpCancelInterval, VmDestroyForceStop,
                VmJobCheckInterval, VmJobTimeout, PingInterval};
    }

    public class VmOutcome extends OutcomeImpl<VirtualMachine> {
        private long _vmId;

        public VmOutcome(final AsyncJob job, final PowerState desiredPowerState, final long vmId, final Long srcHostIdForMigration) {
            super(VirtualMachine.class, job, _jobCheckInterval.value(), new Predicate() {
                @Override
                public boolean checkCondition() {
                    VMInstanceVO instance = _vmDao.findById(vmId);
                    if (instance.getPowerState() == desiredPowerState && (srcHostIdForMigration != null && instance.getPowerHostId() != srcHostIdForMigration))
                        return true;



                    return false;
                }
            }, Topics.VM_POWER_STATE, AsyncJob.Topics.JOB_STATE);
            _vmId = vmId;
        }

        @Override
        protected VirtualMachine retrieve() {
            return _vmDao.findById(_vmId);
        }
    }

}
