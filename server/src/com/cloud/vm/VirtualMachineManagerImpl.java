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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.ejb.Local;
import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreManager;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;

import com.cloud.dc.*;
import org.apache.log4j.Logger;

import com.cloud.agent.AgentManager;
import com.cloud.agent.AgentManager.OnError;
import com.cloud.agent.Listener;
import com.cloud.agent.api.AgentControlAnswer;
import com.cloud.agent.api.AgentControlCommand;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.CheckVirtualMachineAnswer;
import com.cloud.agent.api.CheckVirtualMachineCommand;
import com.cloud.agent.api.ClusterSyncAnswer;
import com.cloud.agent.api.ClusterSyncCommand;
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
import com.cloud.agent.api.StartupRoutingCommand.VmState;
import com.cloud.agent.api.StopAnswer;
import com.cloud.agent.api.StopCommand;
import com.cloud.agent.api.to.NicTO;
import com.cloud.agent.api.to.VirtualMachineTO;
import com.cloud.agent.manager.Commands;
import com.cloud.agent.manager.allocator.HostAllocator;
import com.cloud.alert.AlertManager;
import com.cloud.cluster.ClusterManager;
import com.cloud.configuration.Config;
import com.cloud.configuration.ConfigurationManager;
import com.cloud.configuration.dao.ConfigurationDao;
import com.cloud.dc.DataCenter;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.HostPodVO;
import com.cloud.consoleproxy.ConsoleProxyManager;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.HostPodDao;
import com.cloud.deploy.DataCenterDeployment;
import com.cloud.deploy.DeployDestination;
import com.cloud.deploy.DeploymentPlan;
import com.cloud.deploy.DeploymentPlanner;
import com.cloud.deploy.DeploymentPlanner.ExcludeList;
import com.cloud.domain.dao.DomainDao;
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
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.offering.ServiceOffering;
import com.cloud.org.Cluster;
import com.cloud.resource.ResourceManager;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.storage.DiskOfferingVO;
import com.cloud.storage.Storage.ImageFormat;
import com.cloud.storage.StorageManager;
import com.cloud.storage.StoragePool;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.Volume;
import com.cloud.storage.Volume.Type;
import com.cloud.storage.VolumeManager;
import com.cloud.storage.VolumeVO;
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
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.concurrency.NamedThreadFactory;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.GlobalLock;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.exception.ExecutionException;
import com.cloud.utils.fsm.NoTransitionException;
import com.cloud.utils.fsm.StateMachine2;
import com.cloud.vm.ItWorkVO.Step;
import com.cloud.vm.VirtualMachine.Event;
import com.cloud.vm.VirtualMachine.State;
import com.cloud.vm.dao.NicDao;
import com.cloud.vm.dao.UserVmDao;
import com.cloud.vm.dao.VMInstanceDao;
import com.cloud.vm.snapshot.VMSnapshot;
import com.cloud.vm.snapshot.VMSnapshotManager;
import com.cloud.vm.snapshot.VMSnapshotVO;
import com.cloud.vm.snapshot.dao.VMSnapshotDao;
import com.cloud.vm.dao.UserVmDetailsDao;

@Local(value = VirtualMachineManager.class)
public class VirtualMachineManagerImpl extends ManagerBase implements VirtualMachineManager, Listener {
    private static final Logger s_logger = Logger.getLogger(VirtualMachineManagerImpl.class);

    @Inject
    protected StorageManager _storageMgr;
    @Inject
    DataStoreManager dataStoreMgr;
    @Inject
    protected NetworkManager _networkMgr;
    @Inject
    protected NetworkModel _networkModel;
    @Inject
    protected AgentManager _agentMgr;
    @Inject
    protected VMInstanceDao _vmDao;
    @Inject
    protected ServiceOfferingDao _offeringDao;
    @Inject
    protected VMTemplateDao _templateDao;
    @Inject
    protected UserDao _userDao;
    @Inject
    protected AccountDao _accountDao;
    @Inject
    protected DomainDao _domainDao;
    @Inject
    protected ClusterManager _clusterMgr;
    @Inject
    protected ItWorkDao _workDao;
    @Inject
    protected UserVmDao _userVmDao;
    @Inject
    protected NicDao _nicsDao;
    @Inject
    protected AccountManager _accountMgr;
    @Inject
    protected HostDao _hostDao;
    @Inject
    protected AlertManager _alertMgr;
    @Inject
    protected GuestOSCategoryDao _guestOsCategoryDao;
    @Inject
    protected GuestOSDao _guestOsDao;
    @Inject
    protected VolumeDao _volsDao;
    @Inject
    protected ConfigurationManager _configMgr;
    @Inject
    protected HighAvailabilityManager _haMgr;
    @Inject
    protected HostPodDao _podDao;
    @Inject
    protected DataCenterDao _dcDao;
    @Inject
    protected PrimaryDataStoreDao _storagePoolDao;
    @Inject
    protected HypervisorGuruManager _hvGuruMgr;
    @Inject
    protected NetworkDao _networkDao;
    @Inject
    protected VMSnapshotDao _vmSnapshotDao;

    @Inject
    protected List<DeploymentPlanner> _planners;

    @Inject
    protected List<HostAllocator> _hostAllocators;

    @Inject
    protected ResourceManager _resourceMgr;
    
    @Inject 
    protected VMSnapshotManager _vmSnapshotMgr = null;
    @Inject
    protected ClusterDetailsDao  _clusterDetailsDao;
    @Inject
    protected UserVmDetailsDao _uservmDetailsDao;

    @Inject
    protected ConfigurationDao _configDao;
    @Inject
    VolumeManager volumeMgr;

    Map<VirtualMachine.Type, VirtualMachineGuru<? extends VMInstanceVO>> _vmGurus = new HashMap<VirtualMachine.Type, VirtualMachineGuru<? extends VMInstanceVO>>();
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
    protected boolean _forceStop;

    @Override
    public <T extends VMInstanceVO> void registerGuru(VirtualMachine.Type type, VirtualMachineGuru<T> guru) {
        synchronized (_vmGurus) {
            _vmGurus.put(type, guru);
        }
    }

    @Override
    @DB
    public <T extends VMInstanceVO> T allocate(T vm, VMTemplateVO template, ServiceOfferingVO serviceOffering, Pair<? extends DiskOfferingVO, Long> rootDiskOffering,
            List<Pair<DiskOfferingVO, Long>> dataDiskOfferings, List<Pair<NetworkVO, NicProfile>> networks, Map<VirtualMachineProfile.Param, Object> params, DeploymentPlan plan,
            HypervisorType hyperType, Account owner) throws InsufficientCapacityException {
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
        VirtualMachineGuru<T> guru = (VirtualMachineGuru<T>) _vmGurus.get(vm.getType());

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
            this.volumeMgr.allocateRawVolume(Type.ROOT, "ROOT-" + vm.getId(), rootDiskOffering.first(), rootDiskOffering.second(), vm, owner);
        } else if (template.getFormat() == ImageFormat.BAREMETAL) {
            // Do nothing
        } else {
            this.volumeMgr.allocateTemplatedVolume(Type.ROOT, "ROOT-" + vm.getId(), rootDiskOffering.first(), template, vm, owner);
        }

        for (Pair<DiskOfferingVO, Long> offering : dataDiskOfferings) {
            this.volumeMgr.allocateRawVolume(Type.DATADISK, "DATA-" + vm.getId(), offering.first(), offering.second(), vm, owner);
        }

        txn.commit();
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Allocation completed for VM: " + vm);
        }

        return vm;
    }

    @Override
    public <T extends VMInstanceVO> T allocate(T vm, VMTemplateVO template, ServiceOfferingVO serviceOffering, Long rootSize, Pair<DiskOfferingVO, Long> dataDiskOffering,
            List<Pair<NetworkVO, NicProfile>> networks, DeploymentPlan plan, HypervisorType hyperType, Account owner) throws InsufficientCapacityException {
        List<Pair<DiskOfferingVO, Long>> diskOfferings = new ArrayList<Pair<DiskOfferingVO, Long>>(1);
        if (dataDiskOffering != null) {
            diskOfferings.add(dataDiskOffering);
        }
        return allocate(vm, template, serviceOffering, new Pair<DiskOfferingVO, Long>(serviceOffering, rootSize), diskOfferings, networks, null, plan, hyperType, owner);
    }

    @Override
    public <T extends VMInstanceVO> T allocate(T vm, VMTemplateVO template, ServiceOfferingVO serviceOffering, List<Pair<NetworkVO, NicProfile>> networks, DeploymentPlan plan,
            HypervisorType hyperType, Account owner) throws InsufficientCapacityException {
        return allocate(vm, template, serviceOffering, new Pair<DiskOfferingVO, Long>(serviceOffering, null), null, networks, null, plan, hyperType, owner);
    }

    @SuppressWarnings("unchecked")
    private <T extends VMInstanceVO> VirtualMachineGuru<T> getVmGuru(T vm) {
        return (VirtualMachineGuru<T>) _vmGurus.get(vm.getType());
    }

    @Override
    public <T extends VMInstanceVO> boolean expunge(T vm, User caller, Account account) throws ResourceUnavailableException {
        try {
            if (advanceExpunge(vm, caller, account)) {
                // Mark vms as removed
                remove(vm, caller, account);
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

        try {
            if (!stateTransitTo(vm, VirtualMachine.Event.ExpungeOperation, vm.getHostId())) {
                s_logger.debug("Unable to destroy the vm because it is not in the correct state: " + vm);
                return false;
            }
        } catch (NoTransitionException e) {
            s_logger.debug("Unable to destroy the vm because it is not in the correct state: " + vm);
            return false;
        }

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Destroying vm " + vm);
        }

        VirtualMachineProfile<T> profile = new VirtualMachineProfileImpl<T>(vm);
        s_logger.debug("Cleaning up NICS");
        _networkMgr.cleanupNics(profile);
        // Clean up volumes based on the vm's instance id
        this.volumeMgr.cleanupVolumes(vm.getId());

        VirtualMachineGuru<T> guru = getVmGuru(vm);
        guru.finalizeExpunge(vm);
        //remove the overcommit detials from the uservm details
        _uservmDetailsDao.deleteDetails(vm.getId());

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Expunged " + vm);
        }

        return true;
    }

    @Override
    public boolean start() {
        _executor.scheduleAtFixedRate(new CleanupTask(), _cleanupInterval, _cleanupInterval, TimeUnit.SECONDS);
        cancelWorkItems(_nodeId);
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public boolean configure(String name, Map<String, Object> xmlParams) throws ConfigurationException {
        Map<String, String> params = _configDao.getConfiguration(xmlParams);

        _retry = NumbersUtil.parseInt(params.get(Config.StartRetry.key()), 10);

        ReservationContextImpl.setComponents(_userDao, _domainDao, _accountDao);
        VirtualMachineProfileImpl.setComponents(_offeringDao, _templateDao, _accountDao);

        _cancelWait = NumbersUtil.parseLong(params.get(Config.VmOpCancelInterval.key()), 3600);
        _cleanupWait = NumbersUtil.parseLong(params.get(Config.VmOpCleanupWait.key()), 3600);
        _cleanupInterval = NumbersUtil.parseLong(params.get(Config.VmOpCleanupInterval.key()), 86400) * 1000;
        _opWaitInterval = NumbersUtil.parseLong(params.get(Config.VmOpWaitInterval.key()), 120) * 1000;
        _lockStateRetry = NumbersUtil.parseInt(params.get(Config.VmOpLockStateRetry.key()), 5);
        _operationTimeout = NumbersUtil.parseInt(params.get(Config.Wait.key()), 1800) * 2;
        _forceStop = Boolean.parseBoolean(params.get(Config.VmDestroyForcestop.key()));

        _executor = Executors.newScheduledThreadPool(1, new NamedThreadFactory("Vm-Operations-Cleanup"));
        _nodeId = _clusterMgr.getManagementNodeId();

        _agentMgr.registerForHostEvents(this, true, true, true);

        return true;
    }

    protected VirtualMachineManagerImpl() {
        setStateMachine();
    }

    @Override
    public <T extends VMInstanceVO> T start(T vm, Map<VirtualMachineProfile.Param, Object> params, User caller, Account account) throws InsufficientCapacityException, ResourceUnavailableException {
        return start(vm, params, caller, account, null);
    }

    @Override
    public <T extends VMInstanceVO> T start(T vm, Map<VirtualMachineProfile.Param, Object> params, User caller, Account account, DeploymentPlan planToDeploy) throws InsufficientCapacityException,
    ResourceUnavailableException {
        try {
            return advanceStart(vm, params, caller, account, planToDeploy);
        } catch (ConcurrentOperationException e) {
            throw new CloudRuntimeException("Unable to start a VM due to concurrent operation", e);
        }
    }

    protected boolean checkWorkItems(VMInstanceVO vm, State state) throws ConcurrentOperationException {
        while (true) {
            ItWorkVO vo = _workDao.findByOutstandingWork(vm.getId(), state);
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

    @DB
    protected <T extends VMInstanceVO> Ternary<T, ReservationContext, ItWorkVO> changeToStartState(VirtualMachineGuru<T> vmGuru, T vm, User caller, Account account)
            throws ConcurrentOperationException {
        long vmId = vm.getId();

        ItWorkVO work = new ItWorkVO(UUID.randomUUID().toString(), _nodeId, State.Starting, vm.getType(), vm.getId());
        int retry = _lockStateRetry;
        while (retry-- != 0) {
            Transaction txn = Transaction.currentTxn();
            Ternary<T, ReservationContext, ItWorkVO> result = null;
            txn.start();
            try {
                Journal journal = new Journal.LogJournal("Creating " + vm, s_logger);
                work = _workDao.persist(work);
                ReservationContextImpl context = new ReservationContextImpl(work.getId(), journal, caller, account);

                if (stateTransitTo(vm, Event.StartRequested, null, work.getId())) {
                    if (s_logger.isDebugEnabled()) {
                        s_logger.debug("Successfully transitioned to start state for " + vm + " reservation id = " + work.getId());
                    }
                    result = new Ternary<T, ReservationContext, ItWorkVO>(vmGuru.findById(vmId), context, work);
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

    protected <T extends VMInstanceVO> boolean changeState(T vm, Event event, Long hostId, ItWorkVO work, Step step) throws NoTransitionException {
        // FIXME: We should do this better.
        Step previousStep = work.getStep();
        _workDao.updateStep(work, step);
        boolean result = false;
        try {
            result = stateTransitTo(vm, event, hostId);
            return result;
        } finally {
            if (!result) {
                _workDao.updateStep(work, previousStep);
            }
        }
    }

    @Override
    public <T extends VMInstanceVO> T advanceStart(T vm, Map<VirtualMachineProfile.Param, Object> params, User caller, Account account) throws InsufficientCapacityException,
    ConcurrentOperationException, ResourceUnavailableException {
        return advanceStart(vm, params, caller, account, null);
    }
    
    @Override
    public <T extends VMInstanceVO> T advanceStart(T vm, Map<VirtualMachineProfile.Param, Object> params, User caller, Account account, DeploymentPlan planToDeploy)
            throws InsufficientCapacityException, ConcurrentOperationException, ResourceUnavailableException {
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

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Trying to deploy VM, vm has dcId: " + vm.getDataCenterId() + " and podId: " + vm.getPodIdToDeployIn());
        }
        DataCenterDeployment plan = new DataCenterDeployment(vm.getDataCenterId(), vm.getPodIdToDeployIn(), null, null, null, null, ctx);
        if(planToDeploy != null && planToDeploy.getDataCenterId() != 0){
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("advanceStart: DeploymentPlan is provided, using dcId:" + planToDeploy.getDataCenterId() + ", podId: " + planToDeploy.getPodId() + ", clusterId: "
                        + planToDeploy.getClusterId() + ", hostId: " + planToDeploy.getHostId() + ", poolId: " + planToDeploy.getPoolId());
            }
            plan = new DataCenterDeployment(planToDeploy.getDataCenterId(), planToDeploy.getPodId(), planToDeploy.getClusterId(), planToDeploy.getHostId(), planToDeploy.getPoolId(), planToDeploy.getPhysicalNetworkId(), ctx);
        }

        HypervisorGuru hvGuru = _hvGuruMgr.getGuru(vm.getHypervisorType());

        boolean canRetry = true;
        try {
            Journal journal = start.second().getJournal();

            ExcludeList avoids = null;
            if (planToDeploy != null) {
                avoids = planToDeploy.getAvoids();
            }
            if (avoids == null) {
                avoids = new ExcludeList();
            }
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Deploy avoids pods: " + avoids.getPodsToAvoid() + ", clusters: " + avoids.getClustersToAvoid() + ", hosts: " + avoids.getHostsToAvoid());
            }


            boolean planChangedByVolume = false;
            boolean reuseVolume = true;
            DataCenterDeployment originalPlan = plan;

            int retry = _retry;
            while (retry-- != 0) { // It's != so that it can match -1.

                if(reuseVolume){
                    // edit plan if this vm's ROOT volume is in READY state already
                    List<VolumeVO> vols = _volsDao.findReadyRootVolumesByInstance(vm.getId());
                    for (VolumeVO vol : vols) {
                        // make sure if the templateId is unchanged. If it is changed,
                        // let planner
                        // reassign pool for the volume even if it ready.
                        Long volTemplateId = vol.getTemplateId();
                        if (volTemplateId != null && volTemplateId.longValue() != template.getId()) {
                            if (s_logger.isDebugEnabled()) {
                                s_logger.debug(vol + " of " + vm + " is READY, but template ids don't match, let the planner reassign a new pool");
                            }
                            continue;
                        }

                        StoragePool pool = (StoragePool)dataStoreMgr.getPrimaryDataStore(vol.getPoolId());
                        if (!pool.isInMaintenance()) {
                            if (s_logger.isDebugEnabled()) {
                                s_logger.debug("Root volume is ready, need to place VM in volume's cluster");
                            }
                            long rootVolDcId = pool.getDataCenterId();
                            Long rootVolPodId = pool.getPodId();
                            Long rootVolClusterId = pool.getClusterId();
                            if (planToDeploy != null && planToDeploy.getDataCenterId() != 0) {
                                Long clusterIdSpecified = planToDeploy.getClusterId();
                                if (clusterIdSpecified != null && rootVolClusterId != null) {
                                    if (rootVolClusterId.longValue() != clusterIdSpecified.longValue()) {
                                        // cannot satisfy the plan passed in to the
                                        // planner
                                        if (s_logger.isDebugEnabled()) {
                                            s_logger.debug("Cannot satisfy the deployment plan passed in since the ready Root volume is in different cluster. volume's cluster: " + rootVolClusterId
                                                    + ", cluster specified: " + clusterIdSpecified);
                                        }
                                        throw new ResourceUnavailableException("Root volume is ready in different cluster, Deployment plan provided cannot be satisfied, unable to create a deployment for "
                                                + vm, Cluster.class, clusterIdSpecified);
                                    }
                                }
                                plan = new DataCenterDeployment(planToDeploy.getDataCenterId(), planToDeploy.getPodId(), planToDeploy.getClusterId(), planToDeploy.getHostId(), vol.getPoolId(), null, ctx);
                            }else{
                                plan = new DataCenterDeployment(rootVolDcId, rootVolPodId, rootVolClusterId, null, vol.getPoolId(), null, ctx);
                                if (s_logger.isDebugEnabled()) {
                                    s_logger.debug(vol + " is READY, changing deployment plan to use this pool's dcId: " + rootVolDcId + " , podId: " + rootVolPodId + " , and clusterId: " + rootVolClusterId);
                                }
                                planChangedByVolume = true;
                            }
                        }
                    }
                }

                VirtualMachineProfileImpl<T> vmProfile = new VirtualMachineProfileImpl<T>(vm, template, offering, account, params);
                DeployDestination dest = null;
                for (DeploymentPlanner planner : _planners) {
                    if (planner.canHandle(vmProfile, plan, avoids)) {
                        dest = planner.plan(vmProfile, plan, avoids);
                    } else {
                        continue;
                    }
                    if (dest != null) {
                        avoids.addHost(dest.getHost().getId());
                        journal.record("Deployment found ", vmProfile, dest);
                        break;
                    }
                }

                if (dest == null) {
                    if (planChangedByVolume) {
                        plan = originalPlan;
                        planChangedByVolume = false;
                        //do not enter volume reuse for next retry, since we want to look for resorces outside the volume's cluster
                        reuseVolume = false;
                        continue;
                    }
                    throw new InsufficientServerCapacityException("Unable to create a deployment for " + vmProfile, DataCenter.class, plan.getDataCenterId());
                }

                long destHostId = dest.getHost().getId();
                vm.setPodId(dest.getPod().getId());
                Long cluster_id = dest.getCluster().getId();
                ClusterDetailsVO cluster_detail_cpu =  _clusterDetailsDao.findDetail(cluster_id,"cpuOvercommitRatio");
                ClusterDetailsVO cluster_detail_ram =  _clusterDetailsDao.findDetail(cluster_id,"memoryOvercommitRatio");
                vmProfile.setcpuOvercommitRatio(Float.parseFloat(cluster_detail_cpu.getValue()));
                vmProfile.setramOvercommitRatio(Float.parseFloat(cluster_detail_ram.getValue()));

                try {
                    if (!changeState(vm, Event.OperationRetry, destHostId, work, Step.Prepare)) {
                        throw new ConcurrentOperationException("Unable to update the state of the Virtual Machine");
                    }
                } catch (NoTransitionException e1) {
                    throw new ConcurrentOperationException(e1.getMessage());
                }

                try {
                    if (s_logger.isDebugEnabled()) {
                        s_logger.debug("VM is being created in podId: " + vm.getPodIdToDeployIn());
                    }
                    _networkMgr.prepare(vmProfile, dest, ctx); 
                    if (vm.getHypervisorType() != HypervisorType.BareMetal) {
                        this.volumeMgr.prepare(vmProfile, dest);
                    }
                    //since StorageMgr succeeded in volume creation, reuse Volume for further tries until current cluster has capacity
                    if(!reuseVolume){
                        reuseVolume = true;
                    }

                    Commands cmds = null;
                    vmGuru.finalizeVirtualMachineProfile(vmProfile, dest, ctx);

                    VirtualMachineTO vmTO = hvGuru.implement(vmProfile);

                    cmds = new Commands(OnError.Stop);
                    cmds.addCommand(new StartCommand(vmTO, dest.getHost()));

                    vmGuru.finalizeDeployment(cmds, vmProfile, dest, ctx);


                    work = _workDao.findById(work.getId());
                    if (work == null || work.getStep() != Step.Prepare) {
                        throw new ConcurrentOperationException("Work steps have been changed: " + work);
                    }
                    _workDao.updateStep(work, Step.Starting);

                    _agentMgr.send(destHostId, cmds);

                    _workDao.updateStep(work, Step.Started);


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
                        if (vmGuru.finalizeStart(vmProfile, destHostId, cmds, ctx)) {
                            if (!changeState(vm, Event.OperationSucceeded, destHostId, work, Step.Done)) {
                                throw new ConcurrentOperationException("Unable to transition to a new state.");
                            }
                            startedVm = vm;
                            if (s_logger.isDebugEnabled()) {
                                s_logger.debug("Start completed for VM " + vm);
                            }
                            return startedVm;
                        } else {
                            if (s_logger.isDebugEnabled()) {
                                s_logger.info("The guru did not like the answers so stopping " + vm);
                            }

                            StopCommand cmd = new StopCommand(vm.getInstanceName());
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
                    if (!avoids.add(e)) {
                        if (e.getScope() == Volume.class || e.getScope() == Nic.class) {
                            throw e;
                        } else {
                            s_logger.warn("unexpected ResourceUnavailableException : " + e.getScope().getName(), e);
                            throw e;
                        }
                    }
                } catch (InsufficientCapacityException e) {
                    s_logger.info("Insufficient capacity ", e);
                    if (!avoids.add(e)) {
                        if (e.getScope() == Volume.class || e.getScope() == Nic.class) {
                            throw e;
                        } else {
                            s_logger.warn("unexpected InsufficientCapacityException : " + e.getScope().getName(), e);
                        }
                    }
                } catch (Exception e) {
                    s_logger.error("Failed to start instance " + vm, e);
                    throw new AgentUnavailableException("Unable to start instance due to " + e.getMessage(), destHostId, e);
                } finally {
                    if (startedVm == null && canRetry) {
                        Step prevStep = work.getStep();
                        _workDao.updateStep(work, Step.Release);
                        if (prevStep == Step.Started || prevStep == Step.Starting) {
                            cleanup(vmGuru, vmProfile, work, Event.OperationFailed, false, caller, account);
                        } else {
                            //if step is not starting/started, send cleanup command with force=true
                            cleanup(vmGuru, vmProfile, work, Event.OperationFailed, true, caller, account);
                        }
                    }
                }
            }
        } finally {
            if (startedVm == null) {
                if (canRetry) {
                    try {
                        changeState(vm, Event.OperationFailed, null, work, Step.Done);
                    } catch (NoTransitionException e) {
                        throw new ConcurrentOperationException(e.getMessage());
                    }
                }
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

    protected <T extends VMInstanceVO> boolean cleanup(VirtualMachineGuru<T> guru, VirtualMachineProfile<T> profile, ItWorkVO work, Event event, boolean force, User user, Account account) {
        T vm = profile.getVirtualMachine();
        State state = vm.getState();
        s_logger.debug("Cleaning up resources for the vm " + vm + " in " + state + " state");
        if (state == State.Starting) {
            Step step = work.getStep();
            if (step == Step.Starting && !force) {
                s_logger.warn("Unable to cleanup vm " + vm + "; work state is incorrect: " + step);
                return false;
            }

            if (step == Step.Started || step == Step.Starting || step == Step.Release) {
                if (vm.getHostId() != null) {
                    if (!sendStop(guru, profile, force)) {
                        s_logger.warn("Failed to stop vm " + vm + " in " + State.Starting + " state as a part of cleanup process");
                        return false;
                    }
                }
            }

            if (step != Step.Release && step != Step.Prepare && step != Step.Started && step != Step.Starting) {
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

        this.volumeMgr.release(profile);
        s_logger.debug("Successfully cleanued up resources for the vm " + vm + " in " + state + " state");
        return true;
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
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Stopped called on " + vm + " but the state is " + state);
            }
            return true;
        }
        // grab outstanding work item if any
        ItWorkVO work = _workDao.findByOutstandingWork(vm.getId(), vm.getState());
        if (work != null) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Found an outstanding work item for this vm " + vm + " with state:" + vm.getState() + ", work id:" + work.getId());
            }
        }
        Long hostId = vm.getHostId();
        if (hostId == null) {
            if (!forced) {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("HostId is null but this is not a forced stop, cannot stop vm " + vm + " with state:" + vm.getState());
                }
                return false;
            }
            try {
                stateTransitTo(vm, Event.AgentReportStopped, null, null);
            } catch (NoTransitionException e) {
                s_logger.warn(e.getMessage());
            }
            // mark outstanding work item if any as done
            if (work != null) {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Updating work item to Done, id:" + work.getId());
                }
                work.setStep(Step.Done);
                _workDao.update(work.getId(), work);
            }
            return true;
        }

        VirtualMachineGuru<T> vmGuru = getVmGuru(vm);
        VirtualMachineProfile<T> profile = new VirtualMachineProfileImpl<T>(vm);

        try {
            if (!stateTransitTo(vm, Event.StopRequested, vm.getHostId())) {
                throw new ConcurrentOperationException("VM is being operated on.");
            }
        } catch (NoTransitionException e1) {
            if (!forced) {
                throw new CloudRuntimeException("We cannot stop " + vm + " when it is in state " + vm.getState());
            }
            boolean doCleanup = false;
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Unable to transition the state but we're moving on because it's forced stop");
            }
            if (state == State.Starting || state == State.Migrating) {
                if (work != null) {
                    doCleanup = true;
                } else {
                    if (s_logger.isDebugEnabled()) {
                        s_logger.debug("Unable to cleanup VM: " + vm + " ,since outstanding work item is not found");
                    }
                    throw new CloudRuntimeException("Work item not found, We cannot stop " + vm + " when it is in state " + vm.getState());
                }
            } else if (state == State.Stopping) {
                doCleanup = true;
            }

            if (doCleanup) {
                if (cleanup(vmGuru, new VirtualMachineProfileImpl<T>(vm), work, Event.StopRequested, forced, user, account)) {
                    try {
                        if (s_logger.isDebugEnabled()) {
                            s_logger.debug("Updating work item to Done, id:" + work.getId());
                        }
                        return changeState(vm, Event.AgentReportStopped, null, work, Step.Done);
                    } catch (NoTransitionException e) {
                        s_logger.warn("Unable to cleanup " + vm);
                        return false;
                    }
                } else {
                    if (s_logger.isDebugEnabled()) {
                        s_logger.debug("Failed to cleanup VM: " + vm);
                    }
                    throw new CloudRuntimeException("Failed to cleanup " + vm + " , current state " + vm.getState());
                }
            }
        }

        if (vm.getState() != State.Stopping) {
            throw new CloudRuntimeException("We cannot proceed with stop VM " + vm + " since it is not in 'Stopping' state, current state: " + vm.getState());
        }

        vmGuru.prepareStop(profile);

        StopCommand stop = new StopCommand(vm, vm.getInstanceName(), null);
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
                        stateTransitTo(vm, Event.OperationFailed, vm.getHostId());
                    } catch (NoTransitionException e) {
                        s_logger.warn("Unable to transition the state " + vm);
                    }
                    return false;
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
                this.volumeMgr.release(profile);
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
                work.setStep(Step.Done);
                _workDao.update(work.getId(), work);
            }

            return stateTransitTo(vm, Event.OperationSucceeded, null, null);
        } catch (NoTransitionException e) {
            s_logger.warn(e.getMessage());
            return false;
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
        // if there are active vm snapshots task, state change is not allowed
        if(_vmSnapshotMgr.hasActiveVMSnapshotTasks(vm.getId())){
            s_logger.error("State transit with event: " + e + " failed due to: " + vm.getInstanceName() + " has active VM snapshots tasks");
            return false;
        }
        
        State oldState = vm.getState();
        if (oldState == State.Starting) {
            if (e == Event.OperationSucceeded) {
                vm.setLastHostId(hostId);
            }
        } else if (oldState == State.Stopping) {
            if (e == Event.OperationSucceeded) {
                vm.setLastHostId(vm.getHostId());
            }
        }
        return _stateMachine.transitTo(vm, e, new Pair<Long, Long>(vm.getHostId(), hostId), _vmDao);
    }

    @Override
    public <T extends VMInstanceVO> boolean remove(T vm, User user, Account caller) {
        return _vmDao.remove(vm.getId());
    }

    @Override
    public <T extends VMInstanceVO> boolean destroy(T vm, User user, Account caller) throws AgentUnavailableException, OperationTimedoutException, ConcurrentOperationException {
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Destroying vm " + vm);
        }
        if (vm == null || vm.getState() == State.Destroyed || vm.getState() == State.Expunging || vm.getRemoved() != null) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Unable to find vm or vm is destroyed: " + vm);
            }
            return true;
        }

        if (!advanceStop(vm, _forceStop, user, caller)) {
            s_logger.debug("Unable to stop " + vm);
            return false;
        }
        
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
        if (!answer.getResult() || answer.getState() == State.Stopped) {
            return false;
        }

        return true;
    }

    @Override
    public <T extends VMInstanceVO> T storageMigration(T vm, StoragePool destPool) {
        VirtualMachineGuru<T> vmGuru = getVmGuru(vm);

        long vmId = vm.getId();
        vm = vmGuru.findById(vmId);

        try {
            stateTransitTo(vm, VirtualMachine.Event.StorageMigrationRequested, null);
        } catch (NoTransitionException e) {
            s_logger.debug("Unable to migrate vm: " + e.toString());
            throw new CloudRuntimeException("Unable to migrate vm: " + e.toString());
        }

        VirtualMachineProfile<VMInstanceVO> profile = new VirtualMachineProfileImpl<VMInstanceVO>(vm);
        boolean migrationResult = false;
        try {
            migrationResult = this.volumeMgr.storageMigration(profile, destPool);

            if (migrationResult) {
                //if the vm is migrated to different pod in basic mode, need to reallocate ip

                if (vm.getPodIdToDeployIn() != destPool.getPodId()) {
                    DataCenterDeployment plan = new DataCenterDeployment(vm.getDataCenterId(), destPool.getPodId(), null, null, null, null);
                    VirtualMachineProfileImpl<T> vmProfile = new VirtualMachineProfileImpl<T>(vm, null, null, null, null);
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
    public <T extends VMInstanceVO> T migrate(T vm, long srcHostId, DeployDestination dest) throws ResourceUnavailableException, ConcurrentOperationException, ManagementServerException,
    VirtualMachineMigrationException {
        s_logger.info("Migrating " + vm + " to " + dest);

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

        ItWorkVO work = new ItWorkVO(UUID.randomUUID().toString(), _nodeId, State.Migrating, vm.getType(), vm.getId());
        work.setStep(Step.Prepare);
        work.setResourceType(ItWorkVO.ResourceType.Host);
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
            if (vm == null || vm.getHostId() == null || vm.getHostId() != srcHostId || !changeState(vm, Event.MigrationRequested, dstHostId, work, Step.Migrating)) {
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
                if (!changeState(vm, VirtualMachine.Event.OperationSucceeded, dstHostId, work, Step.Started)) {
                    throw new ConcurrentOperationException("Unable to change the state for " + vm);
                }
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
                    cleanup(vmGuru, new VirtualMachineProfileImpl<T>(vm), work, Event.AgentReportStopped, true, _accountMgr.getSystemUser(), _accountMgr.getSystemAccount());
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
                    stateTransitTo(vm, Event.OperationFailed, srcHostId);
                } catch (NoTransitionException e) {
                    s_logger.warn(e.getMessage());
                }
            }

            work.setStep(Step.Done);
            _workDao.update(work.getId(), work);
        }
    }

    @Override
    public VirtualMachineTO toVmTO(VirtualMachineProfile<? extends VMInstanceVO> profile) {
        HypervisorGuru hvGuru = _hvGuruMgr.getGuru(profile.getVirtualMachine().getHypervisorType());
        VirtualMachineTO to = hvGuru.implement(profile);
        return to;
    }

    protected void cancelWorkItems(long nodeId) {
        GlobalLock scanLock = GlobalLock.getInternLock("vmmgr.cancel.workitem");

        try {
            if (scanLock.lock(3)) {
                try {
                    List<ItWorkVO> works = _workDao.listWorkInProgressFor(nodeId);
                    for (ItWorkVO work : works) {
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
    }

    @Override
    public boolean migrateAway(VirtualMachine.Type vmType, long vmId, long srcHostId) throws InsufficientServerCapacityException, VirtualMachineMigrationException {
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

        DataCenterDeployment plan = new DataCenterDeployment(host.getDataCenterId(), host.getPodId(), host.getClusterId(), null, null, null);
        ExcludeList excludes = new ExcludeList();
        excludes.addHost(hostId);

        DeployDestination dest = null;
        while (true) {
            for (DeploymentPlanner planner : _planners) {
                if (planner.canHandle(profile, plan, excludes)) {
                    dest = planner.plan(profile, plan, excludes);
                } else {
                    continue;
                }

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
            VMInstanceVO vmInstance = null;
            try {
                vmInstance = migrate(vm, srcHostId, dest);
            } catch (ResourceUnavailableException e) {
                s_logger.debug("Unable to migrate to unavailable " + dest);
            } catch (ConcurrentOperationException e) {
                s_logger.debug("Unable to migrate VM due to: " + e.getMessage());
            } catch (ManagementServerException e) {
                s_logger.debug("Unable to migrate VM: " + e.getMessage());
            } catch (VirtualMachineMigrationException e) {
                s_logger.debug("Got VirtualMachineMigrationException, Unable to migrate: " + e.getMessage());
                if (vm.getState() == State.Starting) {
                    s_logger.debug("VM seems to be still Starting, we should retry migration later");
                    throw e;
                } else {
                    s_logger.debug("Unable to migrate VM, VM is not in Running or even Starting state, current state: " + vm.getState().toString());
                }
            }
            if (vmInstance != null) {
                return true;
            }
            try {
                boolean result = advanceStop(vm, true, _accountMgr.getSystemUser(), _accountMgr.getSystemAccount());
                return result;
            } catch (ResourceUnavailableException e) {
                s_logger.debug("Unable to stop VM due to " + e.getMessage());
            } catch (ConcurrentOperationException e) {
                s_logger.debug("Unable to stop VM due to " + e.getMessage());
            } catch (OperationTimedoutException e) {
                s_logger.debug("Unable to stop VM due to " + e.getMessage());
            }
            return false;
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
    public boolean isVirtualMachineUpgradable(VirtualMachine vm, ServiceOffering offering) {
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
    public <T extends VMInstanceVO> T reboot(T vm, Map<VirtualMachineProfile.Param, Object> params, User caller, Account account) throws InsufficientCapacityException, ResourceUnavailableException {
        try {
            return advanceReboot(vm, params, caller, account);
        } catch (ConcurrentOperationException e) {
            throw new CloudRuntimeException("Unable to reboot a VM due to concurrent operation", e);
        }
    }

    @Override
    public <T extends VMInstanceVO> T advanceReboot(T vm, Map<VirtualMachineProfile.Param, Object> params, User caller, Account account) throws InsufficientCapacityException,
    ConcurrentOperationException, ResourceUnavailableException {
        T rebootedVm = null;

        DataCenter dc = _configMgr.getZone(vm.getDataCenterId());
        Host host = _hostDao.findById(vm.getHostId());
        Cluster cluster = null;
        if (host != null) {
            cluster = _configMgr.getCluster(host.getClusterId());
        }
        HostPodVO pod = _configMgr.getPod(host.getPodId());
        DeployDestination dest = new DeployDestination(dc, pod, cluster, host);

        try {

            Commands cmds = new Commands(OnError.Stop);
            cmds.addCommand(new RebootCommand(vm.getInstanceName()));
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
    public VMInstanceVO findByIdAndType(VirtualMachine.Type type, long vmId) {
        VirtualMachineGuru<? extends VMInstanceVO> guru = _vmGurus.get(type);
        return guru.findById(vmId);
    }

    public Command cleanup(String vmName) {
        return new StopCommand(vmName);
    }

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


    public void fullSync(final long clusterId, Map<String, Pair<String, State>> newStates) {
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
                ItWorkVO work = _workDao.findByOutstandingWork(vm.getId(), vm.getState());
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
            /* else if(info == null && vm.getState() == State.Stopping) { //Handling CS-13376
                        s_logger.warn("Marking the VM as Stopped as it was still stopping on the CS" +vm.getName());
                        vm.setState(State.Stopped); // Setting the VM as stopped on the DB and clearing it from the host
                        vm.setLastHostId(vm.getHostId());
                        vm.setHostId(null);
                        _vmDao.persist(vm);
                 }*/
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

    }



    protected Map<Long, AgentVmInfo> convertToInfos(final Map<String, Pair<String, State>> newStates) {
        final HashMap<Long, AgentVmInfo> map = new HashMap<Long, AgentVmInfo>();
        if (newStates == null) {
            return map;
        }
        Collection<VirtualMachineGuru<? extends VMInstanceVO>> vmGurus = _vmGurus.values();
        boolean is_alien_vm = true;
        long alien_vm_count = -1;
        for (Map.Entry<String, Pair<String, State>> entry : newStates.entrySet()) {
            is_alien_vm = true;
            for (VirtualMachineGuru<? extends VMInstanceVO> vmGuru : vmGurus) {
                String name = entry.getKey();
                VMInstanceVO vm = vmGuru.findByName(name);
                if (vm != null) {
                    map.put(vm.getId(), new AgentVmInfo(entry.getKey(), vmGuru, vm, entry.getValue().second(), entry.getValue().first()));
                    is_alien_vm = false;
                    break;
                }
                Long id = vmGuru.convertToId(name);
                if (id != null) {
                    map.put(id, new AgentVmInfo(entry.getKey(), vmGuru, null, entry.getValue().second(), entry.getValue().first()));
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

    protected Map<Long, AgentVmInfo> convertToInfos(StartupRoutingCommand cmd) { 
        final Map<String, VmState> states = cmd.getVmStates();
        final HashMap<Long, AgentVmInfo> map = new HashMap<Long, AgentVmInfo>();
        if (states == null) {
            return map;
        }
        Collection<VirtualMachineGuru<? extends VMInstanceVO>> vmGurus = _vmGurus.values();

        for (Map.Entry<String, VmState> entry : states.entrySet()) {
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

    protected Map<Long, AgentVmInfo> convertDeltaToInfos(final Map<String, State> states) {
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
                    map.put(vm.getId(), new AgentVmInfo(entry.getKey(), vmGuru, vm, entry.getValue()));
                    break;
                }

                Long id = vmGuru.convertToId(name);
                if (id != null) {
                    map.put(id, new AgentVmInfo(entry.getKey(), vmGuru, null,entry.getValue()));
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
        State agentState = info.state;
        final String agentName = info.name;
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
                    ItWorkVO workItem = _workDao.findByOutstandingWork(vm.getId(), State.Migrating);
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
            return cleanup(agentName);
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
                s_logger.debug("Sending cleanup to a shutdowned vm: " + agentName);
                command = cleanup(agentName);
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
            command = cleanup(agentName);
        } else if (agentState == State.Running) {
            if (serverState == State.Starting) {
                if (fullSync) {
                    try {
                        ensureVmRunningContext(hostId, vm, Event.AgentReportRunning);
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
                // command = cleanup(agentName);
            }
        }
        return command;
    }

    private void ensureVmRunningContext(long hostId, VMInstanceVO vm, Event cause) throws OperationTimedoutException, ResourceUnavailableException, NoTransitionException, InsufficientAddressCapacityException {
        VirtualMachineGuru<VMInstanceVO> vmGuru = getVmGuru(vm);

        s_logger.debug("VM state is starting on full sync so updating it to running");
        vm = findByIdAndType(vm.getType(), vm.getId());

        // grab outstanding work item if any
        ItWorkVO work = _workDao.findByOutstandingWork(vm.getId(), vm.getState());
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
                    deltaSync(hs.getNewStates());
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
                    Commands commands = deltaHostSync(agentId, ping.getNewStates());
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
                        _haMgr.scheduleStop(instance, instance.getHostId(), WorkType.CheckStop);
                    } else if (state == State.Starting) {
                        _haMgr.scheduleRestart(instance, true);
                    }
                }
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
        public VirtualMachineGuru<VMInstanceVO> guru;

        @SuppressWarnings("unchecked")
        public AgentVmInfo(String name, VirtualMachineGuru<? extends VMInstanceVO> guru, VMInstanceVO vm, State state, String host) {
            this.name = name;
            this.state = state;
            this.vm = vm;
            this.guru = (VirtualMachineGuru<VMInstanceVO>) guru;
            this.hostUuid = host;
        }

        public AgentVmInfo(String name, VirtualMachineGuru<? extends VMInstanceVO> guru, VMInstanceVO vm, State state) {
            this(name, guru, vm, state, null);
        }

        public String getHostUuid() {
            return hostUuid;
        }
    }

    @Override
    public VMInstanceVO findById(long vmId) {
        return _vmDao.findById(vmId);
    }

    @Override
    public void checkIfCanUpgrade(VirtualMachine vmInstance, long newServiceOfferingId) {
        ServiceOfferingVO newServiceOffering = _offeringDao.findById(newServiceOfferingId);
        if (newServiceOffering == null) {
            throw new InvalidParameterValueException("Unable to find a service offering with id " + newServiceOfferingId);
        }
		
        // Check that the VM is stopped
        if (!vmInstance.getState().equals(State.Stopped)) {
            s_logger.warn("Unable to upgrade virtual machine " + vmInstance.toString() + " in state " + vmInstance.getState());
            throw new InvalidParameterValueException("Unable to upgrade virtual machine " + vmInstance.toString() + " " +
                    "in state " + vmInstance.getState()
                    + "; make sure the virtual machine is stopped and not in an error state before upgrading.");
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

        ServiceOfferingVO currentServiceOffering = _offeringDao.findByIdIncludingRemoved(vmInstance.getServiceOfferingId());

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
        List<String> currentTags = _configMgr.csvTagsToList(currentServiceOffering.getTags());
        List<String> newTags = _configMgr.csvTagsToList(newServiceOffering.getTags());
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
        ServiceOffering newSvcOff = _configMgr.getServiceOffering(serviceOfferingId);
        vmForUpdate.setHaEnabled(newSvcOff.getOfferHA());
        vmForUpdate.setLimitCpuUse(newSvcOff.getLimitCpuUse());
        vmForUpdate.setServiceOfferingId(newSvcOff.getId());
        return _vmDao.update(vmId, vmForUpdate);
    }

    @Override
    public NicProfile addVmToNetwork(VirtualMachine vm, Network network, NicProfile requested) throws ConcurrentOperationException, 
    ResourceUnavailableException, InsufficientCapacityException {

        s_logger.debug("Adding vm " + vm + " to network " + network + "; requested nic profile " + requested);
        VMInstanceVO vmVO;
        if (vm.getType() == VirtualMachine.Type.User) {
            vmVO = _userVmDao.findById(vm.getId());
        } else {
            vmVO = _vmDao.findById(vm.getId());
        }
        ReservationContext context = new ReservationContextImpl(null, null, _accountMgr.getActiveUser(User.UID_SYSTEM), 
                _accountMgr.getAccount(Account.ACCOUNT_ID_SYSTEM));

        VirtualMachineProfileImpl<VMInstanceVO> vmProfile = new VirtualMachineProfileImpl<VMInstanceVO>(vmVO, null, 
                null, null, null);

        DataCenter dc = _configMgr.getZone(network.getDataCenterId());
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

            //4) plug the nic to the vm
            VirtualMachineGuru<VMInstanceVO> vmGuru = getVmGuru(vmVO);

            s_logger.debug("Plugging nic for vm " + vm + " in network " + network);
            if (vmGuru.plugNic(network, nicTO, vmTO, context, dest)) {
                s_logger.debug("Nic is plugged successfully for vm " + vm + " in network " + network + ". Vm  is a part of network now");
                return nic;
            } else {
                s_logger.warn("Failed to plug nic to the vm " + vm + " in network " + network);
                return null;
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
        VMInstanceVO vmVO = _vmDao.findById(vm.getId());
        NetworkVO network = _networkDao.findById(nic.getNetworkId());
        ReservationContext context = new ReservationContextImpl(null, null, _accountMgr.getActiveUser(User.UID_SYSTEM), 
                _accountMgr.getAccount(Account.ACCOUNT_ID_SYSTEM));

        VirtualMachineProfileImpl<VMInstanceVO> vmProfile = new VirtualMachineProfileImpl<VMInstanceVO>(vmVO, null, 
                null, null, null);

        DataCenter dc = _configMgr.getZone(network.getDataCenterId());
        Host host = _hostDao.findById(vm.getHostId()); 
        DeployDestination dest = new DeployDestination(dc, null, null, host);
        VirtualMachineGuru<VMInstanceVO> vmGuru = getVmGuru(vmVO);
        HypervisorGuru hvGuru = _hvGuruMgr.getGuru(vmProfile.getVirtualMachine().getHypervisorType());
        VirtualMachineTO vmTO = hvGuru.implement(vmProfile);

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
            s_logger.debug("Un-plugging nic " + nic + " for vm " + vm + " from network " + network);
            boolean result = vmGuru.unplugNic(network, nicTO, vmTO, context, dest);
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
        _nicsDao.expunge(nic.getId());
        return true;
    }

    @Override
    public boolean removeVmFromNetwork(VirtualMachine vm, Network network, URI broadcastUri) throws ConcurrentOperationException, ResourceUnavailableException {
        VMInstanceVO vmVO = _vmDao.findById(vm.getId());
        ReservationContext context = new ReservationContextImpl(null, null, _accountMgr.getActiveUser(User.UID_SYSTEM), 
                _accountMgr.getAccount(Account.ACCOUNT_ID_SYSTEM));

        VirtualMachineProfileImpl<VMInstanceVO> vmProfile = new VirtualMachineProfileImpl<VMInstanceVO>(vmVO, null, 
                null, null, null);

        DataCenter dc = _configMgr.getZone(network.getDataCenterId());
        Host host = _hostDao.findById(vm.getHostId()); 
        DeployDestination dest = new DeployDestination(dc, null, null, host);
        VirtualMachineGuru<VMInstanceVO> vmGuru = getVmGuru(vmVO);
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
            boolean result = vmGuru.unplugNic(network, nicTO, vmTO, context, dest);
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

}
