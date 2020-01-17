// Licensed to the Apacohe Software Foundation (ASF) under one
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
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.cloudstack.affinity.dao.AffinityGroupVMMapDao;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.command.admin.vm.MigrateVMCmd;
import org.apache.cloudstack.api.command.admin.volume.MigrateVolumeCmdByAdmin;
import org.apache.cloudstack.api.command.user.volume.MigrateVolumeCmd;
import org.apache.cloudstack.ca.CAManager;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.engine.orchestration.service.NetworkOrchestrationService;
import org.apache.cloudstack.engine.orchestration.service.VolumeOrchestrationService;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreManager;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStoreInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.StoragePoolAllocator;
import org.apache.cloudstack.framework.ca.Certificate;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.framework.jobs.AsyncJob;
import org.apache.cloudstack.framework.jobs.AsyncJobExecutionContext;
import org.apache.cloudstack.framework.jobs.AsyncJobManager;
import org.apache.cloudstack.framework.jobs.Outcome;
import org.apache.cloudstack.framework.jobs.dao.VmWorkJobDao;
import org.apache.cloudstack.framework.jobs.impl.AsyncJobVO;
import org.apache.cloudstack.framework.jobs.impl.JobSerializerHelper;
import org.apache.cloudstack.framework.jobs.impl.OutcomeImpl;
import org.apache.cloudstack.framework.jobs.impl.VmWorkJobVO;
import org.apache.cloudstack.framework.messagebus.MessageBus;
import org.apache.cloudstack.framework.messagebus.MessageDispatcher;
import org.apache.cloudstack.framework.messagebus.MessageHandler;
import org.apache.cloudstack.jobs.JobInfo;
import org.apache.cloudstack.managed.context.ManagedContextRunnable;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.cloudstack.storage.to.VolumeObjectTO;
import org.apache.cloudstack.utils.identity.ManagementServerNode;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.log4j.Logger;

import com.cloud.agent.AgentManager;
import com.cloud.agent.Listener;
import com.cloud.agent.api.AgentControlAnswer;
import com.cloud.agent.api.AgentControlCommand;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.AttachOrDettachConfigDriveCommand;
import com.cloud.agent.api.CheckVirtualMachineAnswer;
import com.cloud.agent.api.CheckVirtualMachineCommand;
import com.cloud.agent.api.ClusterVMMetaDataSyncAnswer;
import com.cloud.agent.api.ClusterVMMetaDataSyncCommand;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.MigrateCommand;
import com.cloud.agent.api.MigrateVmToPoolAnswer;
import com.cloud.agent.api.ModifyTargetsCommand;
import com.cloud.agent.api.PingRoutingCommand;
import com.cloud.agent.api.PlugNicAnswer;
import com.cloud.agent.api.PlugNicCommand;
import com.cloud.agent.api.PrepareForMigrationAnswer;
import com.cloud.agent.api.PrepareForMigrationCommand;
import com.cloud.agent.api.RebootAnswer;
import com.cloud.agent.api.RebootCommand;
import com.cloud.agent.api.ReplugNicAnswer;
import com.cloud.agent.api.ReplugNicCommand;
import com.cloud.agent.api.RestoreVMSnapshotAnswer;
import com.cloud.agent.api.RestoreVMSnapshotCommand;
import com.cloud.agent.api.ScaleVmCommand;
import com.cloud.agent.api.StartAnswer;
import com.cloud.agent.api.StartCommand;
import com.cloud.agent.api.StartupCommand;
import com.cloud.agent.api.StartupRoutingCommand;
import com.cloud.agent.api.StopAnswer;
import com.cloud.agent.api.StopCommand;
import com.cloud.agent.api.UnPlugNicAnswer;
import com.cloud.agent.api.UnPlugNicCommand;
import com.cloud.agent.api.UnregisterVMCommand;
import com.cloud.agent.api.routing.NetworkElementCommand;
import com.cloud.agent.api.to.DiskTO;
import com.cloud.agent.api.to.DpdkTO;
import com.cloud.agent.api.to.GPUDeviceTO;
import com.cloud.agent.api.to.NicTO;
import com.cloud.agent.api.to.VirtualMachineTO;
import com.cloud.agent.manager.Commands;
import com.cloud.agent.manager.allocator.HostAllocator;
import com.cloud.alert.AlertManager;
import com.cloud.capacity.CapacityManager;
import com.cloud.dc.ClusterDetailsDao;
import com.cloud.dc.ClusterDetailsVO;
import com.cloud.dc.DataCenter;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.HostPodVO;
import com.cloud.dc.Pod;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.HostPodDao;
import com.cloud.deploy.DataCenterDeployment;
import com.cloud.deploy.DeployDestination;
import com.cloud.deploy.DeploymentPlan;
import com.cloud.deploy.DeploymentPlanner;
import com.cloud.deploy.DeploymentPlanner.ExcludeList;
import com.cloud.deploy.DeploymentPlanningManager;
import com.cloud.event.EventTypes;
import com.cloud.event.UsageEventUtils;
import com.cloud.exception.AffinityConflictException;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.ConnectionException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InsufficientServerCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.exception.StorageUnavailableException;
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
import com.cloud.network.NetworkModel;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.router.VirtualRouter;
import com.cloud.offering.DiskOffering;
import com.cloud.offering.DiskOfferingInfo;
import com.cloud.offering.NetworkOffering;
import com.cloud.offering.ServiceOffering;
import com.cloud.offerings.dao.NetworkOfferingDetailsDao;
import com.cloud.org.Cluster;
import com.cloud.resource.ResourceManager;
import com.cloud.resource.ResourceState;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.storage.DiskOfferingVO;
import com.cloud.storage.ScopeType;
import com.cloud.storage.Storage.ImageFormat;
import com.cloud.storage.StorageManager;
import com.cloud.storage.StoragePool;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.Volume;
import com.cloud.storage.Volume.Type;
import com.cloud.storage.VolumeApiService;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.DiskOfferingDao;
import com.cloud.storage.dao.GuestOSCategoryDao;
import com.cloud.storage.dao.GuestOSDao;
import com.cloud.storage.dao.StoragePoolHostDao;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.template.VirtualMachineTemplate;
import com.cloud.user.Account;
import com.cloud.user.User;
import com.cloud.utils.DateUtil;
import com.cloud.utils.Journal;
import com.cloud.utils.Pair;
import com.cloud.utils.Predicate;
import com.cloud.utils.ReflectionUse;
import com.cloud.utils.StringUtils;
import com.cloud.utils.Ternary;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.concurrency.NamedThreadFactory;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.EntityManager;
import com.cloud.utils.db.GlobalLock;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallbackWithException;
import com.cloud.utils.db.TransactionCallbackWithExceptionNoReturn;
import com.cloud.utils.db.TransactionLegacy;
import com.cloud.utils.db.TransactionStatus;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.exception.ExecutionException;
import com.cloud.utils.fsm.NoTransitionException;
import com.cloud.utils.fsm.StateMachine2;
import com.cloud.vm.ItWorkVO.Step;
import com.cloud.vm.VirtualMachine.Event;
import com.cloud.vm.VirtualMachine.PowerState;
import com.cloud.vm.VirtualMachine.State;
import com.cloud.vm.dao.NicDao;
import com.cloud.vm.dao.UserVmDao;
import com.cloud.vm.dao.UserVmDetailsDao;
import com.cloud.vm.dao.VMInstanceDao;
import com.cloud.vm.snapshot.VMSnapshotManager;
import com.cloud.vm.snapshot.VMSnapshotVO;
import com.cloud.vm.snapshot.dao.VMSnapshotDao;
import com.google.common.base.Strings;

public class VirtualMachineManagerImpl extends ManagerBase implements VirtualMachineManager, VmWorkJobHandler, Listener, Configurable {
    private static final Logger s_logger = Logger.getLogger(VirtualMachineManagerImpl.class);

    public static final String VM_WORK_JOB_HANDLER = VirtualMachineManagerImpl.class.getSimpleName();

    private static final String VM_SYNC_ALERT_SUBJECT = "VM state sync alert";

    @Inject
    private DataStoreManager dataStoreMgr;
    @Inject
    private NetworkOrchestrationService _networkMgr;
    @Inject
    private NetworkModel _networkModel;
    @Inject
    private AgentManager _agentMgr;
    @Inject
    private VMInstanceDao _vmDao;
    @Inject
    private ServiceOfferingDao _offeringDao;
    @Inject
    private DiskOfferingDao _diskOfferingDao;
    @Inject
    private VMTemplateDao _templateDao;
    @Inject
    private ItWorkDao _workDao;
    @Inject
    private UserVmDao _userVmDao;
    @Inject
    private UserVmService _userVmService;
    @Inject
    private CapacityManager _capacityMgr;
    @Inject
    private NicDao _nicsDao;
    @Inject
    private HostDao _hostDao;
    @Inject
    private AlertManager _alertMgr;
    @Inject
    private GuestOSCategoryDao _guestOsCategoryDao;
    @Inject
    private GuestOSDao _guestOsDao;
    @Inject
    private VolumeDao _volsDao;
    @Inject
    private HighAvailabilityManager _haMgr;
    @Inject
    private HostPodDao _podDao;
    @Inject
    private DataCenterDao _dcDao;
    @Inject
    private ClusterDao _clusterDao;
    @Inject
    private PrimaryDataStoreDao _storagePoolDao;
    @Inject
    private HypervisorGuruManager _hvGuruMgr;
    @Inject
    private NetworkDao _networkDao;
    @Inject
    private StoragePoolHostDao _poolHostDao;
    @Inject
    private VMSnapshotDao _vmSnapshotDao;
    @Inject
    private AffinityGroupVMMapDao _affinityGroupVMMapDao;
    @Inject
    private EntityManager _entityMgr;
    @Inject
    private GuestOSCategoryDao _guestOSCategoryDao;
    @Inject
    private GuestOSDao _guestOSDao;
    @Inject
    private ServiceOfferingDao _serviceOfferingDao;
    @Inject
    private CAManager caManager;
    @Inject
    private ResourceManager _resourceMgr;
    @Inject
    private VMSnapshotManager _vmSnapshotMgr;
    @Inject
    private ClusterDetailsDao _clusterDetailsDao;
    @Inject
    private UserVmDetailsDao userVmDetailsDao;
    @Inject
    private ConfigurationDao _configDao;
    @Inject
    private VolumeOrchestrationService volumeMgr;
    @Inject
    private DeploymentPlanningManager _dpMgr;
    @Inject
    private MessageBus _messageBus;
    @Inject
    private VirtualMachinePowerStateSync _syncMgr;
    @Inject
    private VmWorkJobDao _workJobDao;
    @Inject
    private AsyncJobManager _jobMgr;
    @Inject
    private StorageManager storageMgr;
    @Inject
    private NetworkOfferingDetailsDao networkOfferingDetailsDao;

    VmWorkJobHandlerProxy _jobHandlerProxy = new VmWorkJobHandlerProxy(this);

    Map<VirtualMachine.Type, VirtualMachineGuru> _vmGurus = new HashMap<VirtualMachine.Type, VirtualMachineGuru>();
    protected StateMachine2<State, VirtualMachine.Event, VirtualMachine> _stateMachine;

    static final ConfigKey<Integer> StartRetry = new ConfigKey<Integer>("Advanced", Integer.class, "start.retry", "10",
            "Number of times to retry create and start commands", true);
    static final ConfigKey<Integer> VmOpWaitInterval = new ConfigKey<Integer>("Advanced", Integer.class, "vm.op.wait.interval", "120",
            "Time (in seconds) to wait before checking if a previous operation has succeeded", true);

    static final ConfigKey<Integer> VmOpLockStateRetry = new ConfigKey<Integer>("Advanced", Integer.class, "vm.op.lock.state.retry", "5",
            "Times to retry locking the state of a VM for operations, -1 means forever", true);
    static final ConfigKey<Long> VmOpCleanupInterval = new ConfigKey<Long>("Advanced", Long.class, "vm.op.cleanup.interval", "86400",
            "Interval to run the thread that cleans up the vm operations (in seconds)", false);
    static final ConfigKey<Long> VmOpCleanupWait = new ConfigKey<Long>("Advanced", Long.class, "vm.op.cleanup.wait", "3600",
            "Time (in seconds) to wait before cleanuping up any vm work items", true);
    static final ConfigKey<Long> VmOpCancelInterval = new ConfigKey<Long>("Advanced", Long.class, "vm.op.cancel.interval", "3600",
            "Time (in seconds) to wait before cancelling a operation", false);
    static final ConfigKey<Boolean> VmDestroyForcestop = new ConfigKey<Boolean>("Advanced", Boolean.class, "vm.destroy.forcestop", "false",
            "On destroy, force-stop takes this value ", true);
    static final ConfigKey<Integer> ClusterDeltaSyncInterval = new ConfigKey<Integer>("Advanced", Integer.class, "sync.interval", "60",
            "Cluster Delta sync interval in seconds",
            false);
    static final ConfigKey<Integer> ClusterVMMetaDataSyncInterval = new ConfigKey<Integer>("Advanced", Integer.class, "vmmetadata.sync.interval", "180", "Cluster VM metadata sync interval in seconds",
            false);

    static final ConfigKey<Long> VmJobCheckInterval = new ConfigKey<Long>("Advanced",
            Long.class, "vm.job.check.interval", "3000",
            "Interval in milliseconds to check if the job is complete", false);
    static final ConfigKey<Long> VmJobTimeout = new ConfigKey<Long>("Advanced",
            Long.class, "vm.job.timeout", "600000",
            "Time in milliseconds to wait before attempting to cancel a job", false);
    static final ConfigKey<Integer> VmJobStateReportInterval = new ConfigKey<Integer>("Advanced",
            Integer.class, "vm.job.report.interval", "60",
            "Interval to send application level pings to make sure the connection is still working", false);

    static final ConfigKey<Boolean> HaVmRestartHostUp = new ConfigKey<Boolean>("Advanced", Boolean.class, "ha.vm.restart.hostup", "true",
            "If an out-of-band stop of a VM is detected and its host is up, then power on the VM", true);

    ScheduledExecutorService _executor = null;

    private long _nodeId;

    private List<StoragePoolAllocator> _storagePoolAllocators;

    private List<HostAllocator> hostAllocators;

    public List<HostAllocator> getHostAllocators() {
        return hostAllocators;
    }

    public void setHostAllocators(final List<HostAllocator> hostAllocators) {
        this.hostAllocators = hostAllocators;
    }

    @Override
    public void registerGuru(final VirtualMachine.Type type, final VirtualMachineGuru guru) {
        synchronized (_vmGurus) {
            _vmGurus.put(type, guru);
        }
    }

    @Override
    @DB
    public void allocate(final String vmInstanceName, final VirtualMachineTemplate template, final ServiceOffering serviceOffering,
            final DiskOfferingInfo rootDiskOfferingInfo, final List<DiskOfferingInfo> dataDiskOfferings,
            final LinkedHashMap<? extends Network, List<? extends NicProfile>> auxiliaryNetworks, final DeploymentPlan plan, final HypervisorType hyperType, final Map<String, Map<Integer, String>> extraDhcpOptions, final Map<Long, DiskOffering> datadiskTemplateToDiskOfferingMap)
                    throws InsufficientCapacityException {

        final VMInstanceVO vm = _vmDao.findVMByInstanceName(vmInstanceName);
        final Account owner = _entityMgr.findById(Account.class, vm.getAccountId());

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Allocating entries for VM: " + vm);
        }

        vm.setDataCenterId(plan.getDataCenterId());
        if (plan.getPodId() != null) {
            vm.setPodIdToDeployIn(plan.getPodId());
        }
        assert plan.getClusterId() == null && plan.getPoolId() == null : "We currently don't support cluster and pool preset yet";
        final VMInstanceVO vmFinal = _vmDao.persist(vm);

        final VirtualMachineProfileImpl vmProfile = new VirtualMachineProfileImpl(vmFinal, template, serviceOffering, null, null);

        Transaction.execute(new TransactionCallbackWithExceptionNoReturn<InsufficientCapacityException>() {
            @Override
            public void doInTransactionWithoutResult(final TransactionStatus status) throws InsufficientCapacityException {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Allocating nics for " + vmFinal);
                }

                try {
                    if (!vmProfile.getBootArgs().contains("ExternalLoadBalancerVm")) {
                        _networkMgr.allocate(vmProfile, auxiliaryNetworks, extraDhcpOptions);
                    }
                } catch (final ConcurrentOperationException e) {
                    throw new CloudRuntimeException("Concurrent operation while trying to allocate resources for the VM", e);
                }

                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Allocating disks for " + vmFinal);
                }

                if (template.getFormat() == ImageFormat.ISO) {
                    volumeMgr.allocateRawVolume(Type.ROOT, "ROOT-" + vmFinal.getId(), rootDiskOfferingInfo.getDiskOffering(), rootDiskOfferingInfo.getSize(),
                            rootDiskOfferingInfo.getMinIops(), rootDiskOfferingInfo.getMaxIops(), vmFinal, template, owner, null);
                } else if (template.getFormat() == ImageFormat.BAREMETAL) {
                    // Do nothing
                } else {
                    volumeMgr.allocateTemplatedVolume(Type.ROOT, "ROOT-" + vmFinal.getId(), rootDiskOfferingInfo.getDiskOffering(), rootDiskOfferingInfo.getSize(),
                            rootDiskOfferingInfo.getMinIops(), rootDiskOfferingInfo.getMaxIops(), template, vmFinal, owner);
                }

                if (dataDiskOfferings != null) {
                    for (final DiskOfferingInfo dataDiskOfferingInfo : dataDiskOfferings) {
                        volumeMgr.allocateRawVolume(Type.DATADISK, "DATA-" + vmFinal.getId(), dataDiskOfferingInfo.getDiskOffering(), dataDiskOfferingInfo.getSize(),
                                dataDiskOfferingInfo.getMinIops(), dataDiskOfferingInfo.getMaxIops(), vmFinal, template, owner, null);
                    }
                }
                if (datadiskTemplateToDiskOfferingMap != null && !datadiskTemplateToDiskOfferingMap.isEmpty()) {
                    int diskNumber = 1;
                    for (Entry<Long, DiskOffering> dataDiskTemplateToDiskOfferingMap : datadiskTemplateToDiskOfferingMap.entrySet()) {
                        DiskOffering diskOffering = dataDiskTemplateToDiskOfferingMap.getValue();
                        long diskOfferingSize = diskOffering.getDiskSize() / (1024 * 1024 * 1024);
                        VMTemplateVO dataDiskTemplate = _templateDao.findById(dataDiskTemplateToDiskOfferingMap.getKey());
                        volumeMgr.allocateRawVolume(Type.DATADISK, "DATA-" + vmFinal.getId() + "-" + String.valueOf(diskNumber), diskOffering, diskOfferingSize, null, null,
                                vmFinal, dataDiskTemplate, owner, Long.valueOf(diskNumber));
                        diskNumber++;
                    }
                }
            }
        });

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Allocation completed for VM: " + vmFinal);
        }
    }

    @Override
    public void allocate(final String vmInstanceName, final VirtualMachineTemplate template, final ServiceOffering serviceOffering,
            final LinkedHashMap<? extends Network, List<? extends NicProfile>> networks, final DeploymentPlan plan, final HypervisorType hyperType) throws InsufficientCapacityException {
        allocate(vmInstanceName, template, serviceOffering, new DiskOfferingInfo(serviceOffering), new ArrayList<DiskOfferingInfo>(), networks, plan, hyperType, null, null);
    }

    private VirtualMachineGuru getVmGuru(final VirtualMachine vm) {
        if(vm != null) {
            return _vmGurus.get(vm.getType());
        }
        return null;
    }

    @Override
    public void expunge(final String vmUuid) throws ResourceUnavailableException {
        try {
            advanceExpunge(vmUuid);
        } catch (final OperationTimedoutException e) {
            throw new CloudRuntimeException("Operation timed out", e);
        } catch (final ConcurrentOperationException e) {
            throw new CloudRuntimeException("Concurrent operation ", e);
        }
    }

    @Override
    public void advanceExpunge(final String vmUuid) throws ResourceUnavailableException, OperationTimedoutException, ConcurrentOperationException {
        final VMInstanceVO vm = _vmDao.findByUuid(vmUuid);
        advanceExpunge(vm);
    }

    protected void advanceExpunge(VMInstanceVO vm) throws ResourceUnavailableException, OperationTimedoutException, ConcurrentOperationException {
        if (vm == null || vm.getRemoved() != null) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Unable to find vm or vm is destroyed: " + vm);
            }
            return;
        }

        advanceStop(vm.getUuid(), false);
        vm = _vmDao.findByUuid(vm.getUuid());

        try {
            if (!stateTransitTo(vm, VirtualMachine.Event.ExpungeOperation, vm.getHostId())) {
                s_logger.debug("Unable to destroy the vm because it is not in the correct state: " + vm);
                throw new CloudRuntimeException("Unable to destroy " + vm);

            }
        } catch (final NoTransitionException e) {
            s_logger.debug("Unable to destroy the vm because it is not in the correct state: " + vm);
            throw new CloudRuntimeException("Unable to destroy " + vm, e);
        }

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Destroying vm " + vm);
        }

        final VirtualMachineProfile profile = new VirtualMachineProfileImpl(vm);

        final HypervisorGuru hvGuru = _hvGuruMgr.getGuru(vm.getHypervisorType());

        s_logger.debug("Cleaning up NICS");
        final List<Command> nicExpungeCommands = hvGuru.finalizeExpungeNics(vm, profile.getNics());
        _networkMgr.cleanupNics(profile);

        s_logger.debug("Cleaning up hypervisor data structures (ex. SRs in XenServer) for managed storage");

        final List<Command> volumeExpungeCommands = hvGuru.finalizeExpungeVolumes(vm);

        final Long hostId = vm.getHostId() != null ? vm.getHostId() : vm.getLastHostId();

        List<Map<String, String>> targets = getTargets(hostId, vm.getId());

        if (volumeExpungeCommands != null && volumeExpungeCommands.size() > 0 && hostId != null) {
            final Commands cmds = new Commands(Command.OnError.Stop);

            for (final Command volumeExpungeCommand : volumeExpungeCommands) {
                cmds.addCommand(volumeExpungeCommand);
            }

            _agentMgr.send(hostId, cmds);

            if (!cmds.isSuccessful()) {
                for (final Answer answer : cmds.getAnswers()) {
                    if (!answer.getResult()) {
                        s_logger.warn("Failed to expunge vm due to: " + answer.getDetails());

                        throw new CloudRuntimeException("Unable to expunge " + vm + " due to " + answer.getDetails());
                    }
                }
            }
        }

        if (hostId != null) {
            volumeMgr.revokeAccess(vm.getId(), hostId);
        }

        // Clean up volumes based on the vm's instance id
        volumeMgr.cleanupVolumes(vm.getId());

        if (hostId != null && CollectionUtils.isNotEmpty(targets)) {
            removeDynamicTargets(hostId, targets);
        }

        final VirtualMachineGuru guru = getVmGuru(vm);
        guru.finalizeExpunge(vm);
        //remove the overcommit details from the uservm details
        userVmDetailsDao.removeDetails(vm.getId());

        // send hypervisor-dependent commands before removing
        final List<Command> finalizeExpungeCommands = hvGuru.finalizeExpunge(vm);
        if (finalizeExpungeCommands != null && finalizeExpungeCommands.size() > 0) {
            if (hostId != null) {
                final Commands cmds = new Commands(Command.OnError.Stop);
                for (final Command command : finalizeExpungeCommands) {
                    cmds.addCommand(command);
                }
                if (nicExpungeCommands != null) {
                    for (final Command command : nicExpungeCommands) {
                        cmds.addCommand(command);
                    }
                }
                _agentMgr.send(hostId, cmds);
                if (!cmds.isSuccessful()) {
                    for (final Answer answer : cmds.getAnswers()) {
                        if (!answer.getResult()) {
                            s_logger.warn("Failed to expunge vm due to: " + answer.getDetails());
                            throw new CloudRuntimeException("Unable to expunge " + vm + " due to " + answer.getDetails());
                        }
                    }
                }
            }
        }

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Expunged " + vm);
        }

    }

    private List<Map<String, String>> getTargets(Long hostId, long vmId) {
        List<Map<String, String>> targets = new ArrayList<>();

        HostVO hostVO = _hostDao.findById(hostId);

        if (hostVO == null || hostVO.getHypervisorType() != HypervisorType.VMware) {
            return targets;
        }

        List<VolumeVO> volumes = _volsDao.findByInstance(vmId);

        if (CollectionUtils.isEmpty(volumes)) {
            return targets;
        }

        for (VolumeVO volume : volumes) {
            StoragePoolVO storagePoolVO = _storagePoolDao.findById(volume.getPoolId());

            if (storagePoolVO != null && storagePoolVO.isManaged()) {
                Map<String, String> target = new HashMap<>();

                target.put(ModifyTargetsCommand.STORAGE_HOST, storagePoolVO.getHostAddress());
                target.put(ModifyTargetsCommand.STORAGE_PORT, String.valueOf(storagePoolVO.getPort()));
                target.put(ModifyTargetsCommand.IQN, volume.get_iScsiName());

                targets.add(target);
            }
        }

        return targets;
    }

    private void removeDynamicTargets(long hostId, List<Map<String, String>> targets) {
        ModifyTargetsCommand cmd = new ModifyTargetsCommand();

        cmd.setTargets(targets);
        cmd.setApplyToAllHostsInCluster(true);
        cmd.setAdd(false);
        cmd.setTargetTypeToRemove(ModifyTargetsCommand.TargetTypeToRemove.DYNAMIC);

        sendModifyTargetsCommand(cmd, hostId);
    }

    private void sendModifyTargetsCommand(ModifyTargetsCommand cmd, long hostId) {
        Answer answer = _agentMgr.easySend(hostId, cmd);

        if (answer == null) {
            String msg = "Unable to get an answer to the modify targets command";

            s_logger.warn(msg);
        }
        else if (!answer.getResult()) {
            String msg = "Unable to modify target on the following host: " + hostId;

            s_logger.warn(msg);
        }
    }

    @Override
    public boolean start() {
        // TODO, initial delay is hardcoded
        _executor.scheduleAtFixedRate(new CleanupTask(), 5, VmJobStateReportInterval.value(), TimeUnit.SECONDS);
        _executor.scheduleAtFixedRate(new TransitionTask(),  VmOpCleanupInterval.value(), VmOpCleanupInterval.value(), TimeUnit.SECONDS);
        cancelWorkItems(_nodeId);

        volumeMgr.cleanupStorageJobs();
        // cleanup left over place holder works
        _workJobDao.expungeLeftoverWorkJobs(ManagementServerNode.getManagementServerId());
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public boolean configure(final String name, final Map<String, Object> xmlParams) throws ConfigurationException {
        ReservationContextImpl.init(_entityMgr);
        VirtualMachineProfileImpl.init(_entityMgr);
        VmWorkMigrate.init(_entityMgr);

        _executor = Executors.newScheduledThreadPool(1, new NamedThreadFactory("Vm-Operations-Cleanup"));
        _nodeId = ManagementServerNode.getManagementServerId();

        _agentMgr.registerForHostEvents(this, true, true, true);

        _messageBus.subscribe(VirtualMachineManager.Topics.VM_POWER_STATE, MessageDispatcher.getDispatcher(this));

        return true;
    }

    protected VirtualMachineManagerImpl() {
        setStateMachine();
    }

    @Override
    public void start(final String vmUuid, final Map<VirtualMachineProfile.Param, Object> params) {
        start(vmUuid, params, null, null);
    }

    @Override
    public void start(final String vmUuid, final Map<VirtualMachineProfile.Param, Object> params, final DeploymentPlan planToDeploy, final DeploymentPlanner planner) {
        try {
            advanceStart(vmUuid, params, planToDeploy, planner);
        } catch (final ConcurrentOperationException e) {
            throw new CloudRuntimeException("Unable to start a VM due to concurrent operation", e).add(VirtualMachine.class, vmUuid);
        } catch (final InsufficientCapacityException e) {
            throw new CloudRuntimeException("Unable to start a VM due to insufficient capacity", e).add(VirtualMachine.class, vmUuid);
        } catch (final ResourceUnavailableException e) {
            if(e.getScope() != null && e.getScope().equals(VirtualRouter.class)){
                throw new CloudRuntimeException("Network is unavailable. Please contact administrator", e).add(VirtualMachine.class, vmUuid);
            }
            throw new CloudRuntimeException("Unable to start a VM due to unavailable resources", e).add(VirtualMachine.class, vmUuid);
        }

    }

    protected boolean checkWorkItems(final VMInstanceVO vm, final State state) throws ConcurrentOperationException {
        while (true) {
            final ItWorkVO vo = _workDao.findByOutstandingWork(vm.getId(), state);
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

            // also check DB to get latest VM state to detect vm update from concurrent process before idle waiting to get an early exit
            final VMInstanceVO instance = _vmDao.findById(vm.getId());
            if (instance != null && instance.getState() == State.Running) {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("VM is already started in DB: " + vm);
                }
                return true;
            }

            if (vo.getSecondsTaskIsInactive() > VmOpCancelInterval.value()) {
                s_logger.warn("The task item for vm " + vm + " has been inactive for " + vo.getSecondsTaskIsInactive());
                return false;
            }

            try {
                Thread.sleep(VmOpWaitInterval.value()*1000);
            } catch (final InterruptedException e) {
                s_logger.info("Waiting for " + vm + " but is interrupted");
                throw new ConcurrentOperationException("Waiting for " + vm + " but is interrupted");
            }
            s_logger.debug("Waiting some more to make sure there's no activity on " + vm);
        }

    }

    @DB
    protected Ternary<VMInstanceVO, ReservationContext, ItWorkVO> changeToStartState(final VirtualMachineGuru vmGuru, final VMInstanceVO vm, final User caller,
            final Account account) throws ConcurrentOperationException {
        final long vmId = vm.getId();

        ItWorkVO work = new ItWorkVO(UUID.randomUUID().toString(), _nodeId, State.Starting, vm.getType(), vm.getId());
        int retry = VmOpLockStateRetry.value();
        while (retry-- != 0) {
            try {
                final ItWorkVO workFinal = work;
                final Ternary<VMInstanceVO, ReservationContext, ItWorkVO> result =
                        Transaction.execute(new TransactionCallbackWithException<Ternary<VMInstanceVO, ReservationContext, ItWorkVO>, NoTransitionException>() {
                            @Override
                            public Ternary<VMInstanceVO, ReservationContext, ItWorkVO> doInTransaction(final TransactionStatus status) throws NoTransitionException {
                                final Journal journal = new Journal.LogJournal("Creating " + vm, s_logger);
                                final ItWorkVO work = _workDao.persist(workFinal);
                                final ReservationContextImpl context = new ReservationContextImpl(work.getId(), journal, caller, account);

                                if (stateTransitTo(vm, Event.StartRequested, null, work.getId())) {
                                    if (s_logger.isDebugEnabled()) {
                                        s_logger.debug("Successfully transitioned to start state for " + vm + " reservation id = " + work.getId());
                                    }
                                    return new Ternary<VMInstanceVO, ReservationContext, ItWorkVO>(vm, context, work);
                                }

                                return new Ternary<VMInstanceVO, ReservationContext, ItWorkVO>(null, null, work);
                            }
                        });

                work = result.third();
                if (result.first() != null) {
                    return result;
                }
            } catch (final NoTransitionException e) {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Unable to transition into Starting state due to " + e.getMessage());
                }
            }

            final VMInstanceVO instance = _vmDao.findById(vmId);
            if (instance == null) {
                throw new ConcurrentOperationException("Unable to acquire lock on " + vm);
            }

            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Determining why we're unable to update the state to Starting for " + instance + ".  Retry=" + retry);
            }

            final State state = instance.getState();
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

    protected <T extends VMInstanceVO> boolean changeState(final T vm, final Event event, final Long hostId, final ItWorkVO work, final Step step) throws NoTransitionException {
        // FIXME: We should do this better.
        Step previousStep = null;
        if (work != null) {
            previousStep = work.getStep();
            _workDao.updateStep(work, step);
        }
        boolean result = false;
        try {
            result = stateTransitTo(vm, event, hostId);
            return result;
        } finally {
            if (!result && work != null) {
                _workDao.updateStep(work, previousStep);
            }
        }
    }

    protected boolean areAffinityGroupsAssociated(final VirtualMachineProfile vmProfile) {
        final VirtualMachine vm = vmProfile.getVirtualMachine();
        final long vmGroupCount = _affinityGroupVMMapDao.countAffinityGroupsForVm(vm.getId());

        if (vmGroupCount > 0) {
            return true;
        }
        return false;
    }

    @Override
    public void advanceStart(final String vmUuid, final Map<VirtualMachineProfile.Param, Object> params, final DeploymentPlanner planner)
            throws InsufficientCapacityException, ConcurrentOperationException, ResourceUnavailableException {
        advanceStart(vmUuid, params, null, planner);
    }

    @Override
    public void advanceStart(final String vmUuid, final Map<VirtualMachineProfile.Param, Object> params, final DeploymentPlan planToDeploy, final DeploymentPlanner planner)
            throws InsufficientCapacityException, ConcurrentOperationException, ResourceUnavailableException {

        final AsyncJobExecutionContext jobContext = AsyncJobExecutionContext.getCurrentExecutionContext();
        if ( jobContext.isJobDispatchedBy(VmWorkConstants.VM_WORK_JOB_DISPATCHER)) {
            // avoid re-entrance
            VmWorkJobVO placeHolder = null;
            final VirtualMachine vm = _vmDao.findByUuid(vmUuid);
            placeHolder = createPlaceHolderWork(vm.getId());
            try {
                orchestrateStart(vmUuid, params, planToDeploy, planner);
            } finally {
                if (placeHolder != null) {
                    _workJobDao.expunge(placeHolder.getId());
                }
            }
        } else {
            final Outcome<VirtualMachine> outcome = startVmThroughJobQueue(vmUuid, params, planToDeploy, planner);

            try {
                final VirtualMachine vm = outcome.get();
            } catch (final InterruptedException e) {
                throw new RuntimeException("Operation is interrupted", e);
            } catch (final java.util.concurrent.ExecutionException e) {
                throw new RuntimeException("Execution excetion", e);
            }

            final Object jobResult = _jobMgr.unmarshallResultObject(outcome.getJob());
            if (jobResult != null) {
                if (jobResult instanceof ConcurrentOperationException) {
                    throw (ConcurrentOperationException)jobResult;
                } else if (jobResult instanceof ResourceUnavailableException) {
                    throw (ResourceUnavailableException)jobResult;
                } else if (jobResult instanceof InsufficientCapacityException) {
                    throw (InsufficientCapacityException)jobResult;
                } else if (jobResult instanceof RuntimeException) {
                    throw (RuntimeException)jobResult;
                } else if (jobResult instanceof Throwable) {
                    throw new RuntimeException("Unexpected exception", (Throwable)jobResult);
                }
            }
        }
    }

    private void setupAgentSecurity(final Host vmHost, final Map<String, String> sshAccessDetails, final VirtualMachine vm) throws AgentUnavailableException, OperationTimedoutException {
        final String csr = caManager.generateKeyStoreAndCsr(vmHost, sshAccessDetails);
        if (!Strings.isNullOrEmpty(csr)) {
            final Map<String, String> ipAddressDetails = new HashMap<>(sshAccessDetails);
            ipAddressDetails.remove(NetworkElementCommand.ROUTER_NAME);
            final Certificate certificate = caManager.issueCertificate(csr, Arrays.asList(vm.getHostName(), vm.getInstanceName()),
                    new ArrayList<>(ipAddressDetails.values()), CAManager.CertValidityPeriod.value(), null);
            final boolean result = caManager.deployCertificate(vmHost, certificate, false, sshAccessDetails);
            if (!result) {
                s_logger.error("Failed to setup certificate for system vm: " + vm.getInstanceName());
            }
        } else {
            s_logger.error("Failed to setup keystore and generate CSR for system vm: " + vm.getInstanceName());
        }
    }

    @Override
    public void orchestrateStart(final String vmUuid, final Map<VirtualMachineProfile.Param, Object> params, final DeploymentPlan planToDeploy, final DeploymentPlanner planner)
            throws InsufficientCapacityException, ConcurrentOperationException, ResourceUnavailableException {

        final CallContext cctxt = CallContext.current();
        final Account account = cctxt.getCallingAccount();
        final User caller = cctxt.getCallingUser();

        VMInstanceVO vm = _vmDao.findByUuid(vmUuid);

        final VirtualMachineGuru vmGuru = getVmGuru(vm);

        final Ternary<VMInstanceVO, ReservationContext, ItWorkVO> start = changeToStartState(vmGuru, vm, caller, account);
        if (start == null) {
            return;
        }

        vm = start.first();
        final ReservationContext ctx = start.second();
        ItWorkVO work = start.third();

        VMInstanceVO startedVm = null;
        final ServiceOfferingVO offering = _offeringDao.findById(vm.getId(), vm.getServiceOfferingId());
        final VirtualMachineTemplate template = _entityMgr.findByIdIncludingRemoved(VirtualMachineTemplate.class, vm.getTemplateId());

        DataCenterDeployment plan = new DataCenterDeployment(vm.getDataCenterId(), vm.getPodIdToDeployIn(), null, null, null, null, ctx);
        if (planToDeploy != null && planToDeploy.getDataCenterId() != 0) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("advanceStart: DeploymentPlan is provided, using dcId:" + planToDeploy.getDataCenterId() + ", podId: " + planToDeploy.getPodId() +
                        ", clusterId: " + planToDeploy.getClusterId() + ", hostId: " + planToDeploy.getHostId() + ", poolId: " + planToDeploy.getPoolId());
            }
            plan =
                    new DataCenterDeployment(planToDeploy.getDataCenterId(), planToDeploy.getPodId(), planToDeploy.getClusterId(), planToDeploy.getHostId(),
                            planToDeploy.getPoolId(), planToDeploy.getPhysicalNetworkId(), ctx);
        }

        final HypervisorGuru hvGuru = _hvGuruMgr.getGuru(vm.getHypervisorType());

        boolean canRetry = true;
        ExcludeList avoids = null;
        try {
            final Journal journal = start.second().getJournal();

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
            final DataCenterDeployment originalPlan = plan;

            int retry = StartRetry.value();
            while (retry-- != 0) { // It's != so that it can match -1.

                if (reuseVolume) {
                    // edit plan if this vm's ROOT volume is in READY state already
                    final List<VolumeVO> vols = _volsDao.findReadyRootVolumesByInstance(vm.getId());
                    for (final VolumeVO vol : vols) {
                        // make sure if the templateId is unchanged. If it is changed,
                        // let planner
                        // reassign pool for the volume even if it ready.
                        final Long volTemplateId = vol.getTemplateId();
                        if (volTemplateId != null && volTemplateId.longValue() != template.getId()) {
                            if (s_logger.isDebugEnabled()) {
                                s_logger.debug(vol + " of " + vm + " is READY, but template ids don't match, let the planner reassign a new pool");
                            }
                            continue;
                        }

                        final StoragePool pool = (StoragePool)dataStoreMgr.getPrimaryDataStore(vol.getPoolId());
                        if (!pool.isInMaintenance()) {
                            if (s_logger.isDebugEnabled()) {
                                s_logger.debug("Root volume is ready, need to place VM in volume's cluster");
                            }
                            final long rootVolDcId = pool.getDataCenterId();
                            final Long rootVolPodId = pool.getPodId();
                            final Long rootVolClusterId = pool.getClusterId();
                            if (planToDeploy != null && planToDeploy.getDataCenterId() != 0) {
                                final Long clusterIdSpecified = planToDeploy.getClusterId();
                                if (clusterIdSpecified != null && rootVolClusterId != null) {
                                    if (rootVolClusterId.longValue() != clusterIdSpecified.longValue()) {
                                        // cannot satisfy the plan passed in to the
                                        // planner
                                        if (s_logger.isDebugEnabled()) {
                                            s_logger.debug("Cannot satisfy the deployment plan passed in since the ready Root volume is in different cluster. volume's cluster: " +
                                                    rootVolClusterId + ", cluster specified: " + clusterIdSpecified);
                                        }
                                        throw new ResourceUnavailableException(
                                                "Root volume is ready in different cluster, Deployment plan provided cannot be satisfied, unable to create a deployment for " +
                                                        vm, Cluster.class, clusterIdSpecified);
                                    }
                                }
                                plan =
                                        new DataCenterDeployment(planToDeploy.getDataCenterId(), planToDeploy.getPodId(), planToDeploy.getClusterId(),
                                                planToDeploy.getHostId(), vol.getPoolId(), null, ctx);
                            } else {
                                plan = new DataCenterDeployment(rootVolDcId, rootVolPodId, rootVolClusterId, null, vol.getPoolId(), null, ctx);
                                if (s_logger.isDebugEnabled()) {
                                    s_logger.debug(vol + " is READY, changing deployment plan to use this pool's dcId: " + rootVolDcId + " , podId: " + rootVolPodId +
                                            " , and clusterId: " + rootVolClusterId);
                                }
                                planChangedByVolume = true;
                            }
                        }
                    }
                }

                final Account owner = _entityMgr.findById(Account.class, vm.getAccountId());
                final VirtualMachineProfileImpl vmProfile = new VirtualMachineProfileImpl(vm, template, offering, owner, params);
                DeployDestination dest = null;
                try {
                    dest = _dpMgr.planDeployment(vmProfile, plan, avoids, planner);
                } catch (final AffinityConflictException e2) {
                    s_logger.warn("Unable to create deployment, affinity rules associted to the VM conflict", e2);
                    throw new CloudRuntimeException("Unable to create deployment, affinity rules associted to the VM conflict");

                }

                if (dest == null) {
                    if (planChangedByVolume) {
                        plan = originalPlan;
                        planChangedByVolume = false;
                        //do not enter volume reuse for next retry, since we want to look for resources outside the volume's cluster
                        reuseVolume = false;
                        continue;
                    }
                    throw new InsufficientServerCapacityException("Unable to create a deployment for " + vmProfile, DataCenter.class, plan.getDataCenterId(),
                            areAffinityGroupsAssociated(vmProfile));
                }

                if (dest != null) {
                    avoids.addHost(dest.getHost().getId());
                    journal.record("Deployment found ", vmProfile, dest);
                }

                long destHostId = dest.getHost().getId();
                vm.setPodIdToDeployIn(dest.getPod().getId());
                final Long cluster_id = dest.getCluster().getId();
                final ClusterDetailsVO cluster_detail_cpu = _clusterDetailsDao.findDetail(cluster_id, VmDetailConstants.CPU_OVER_COMMIT_RATIO);
                final ClusterDetailsVO cluster_detail_ram = _clusterDetailsDao.findDetail(cluster_id, VmDetailConstants.MEMORY_OVER_COMMIT_RATIO);
                //storing the value of overcommit in the user_vm_details table for doing a capacity check in case the cluster overcommit ratio is changed.
                if (userVmDetailsDao.findDetail(vm.getId(), VmDetailConstants.CPU_OVER_COMMIT_RATIO) == null &&
                        (Float.parseFloat(cluster_detail_cpu.getValue()) > 1f || Float.parseFloat(cluster_detail_ram.getValue()) > 1f)) {
                    userVmDetailsDao.addDetail(vm.getId(), VmDetailConstants.CPU_OVER_COMMIT_RATIO, cluster_detail_cpu.getValue(), true);
                    userVmDetailsDao.addDetail(vm.getId(), VmDetailConstants.MEMORY_OVER_COMMIT_RATIO, cluster_detail_ram.getValue(), true);
                } else if (userVmDetailsDao.findDetail(vm.getId(), VmDetailConstants.CPU_OVER_COMMIT_RATIO) != null) {
                    userVmDetailsDao.addDetail(vm.getId(), VmDetailConstants.CPU_OVER_COMMIT_RATIO, cluster_detail_cpu.getValue(), true);
                    userVmDetailsDao.addDetail(vm.getId(), VmDetailConstants.MEMORY_OVER_COMMIT_RATIO, cluster_detail_ram.getValue(), true);
                }

                vmProfile.setCpuOvercommitRatio(Float.parseFloat(cluster_detail_cpu.getValue()));
                vmProfile.setMemoryOvercommitRatio(Float.parseFloat(cluster_detail_ram.getValue()));
                StartAnswer startAnswer = null;

                try {
                    if (!changeState(vm, Event.OperationRetry, destHostId, work, Step.Prepare)) {
                        throw new ConcurrentOperationException("Unable to update the state of the Virtual Machine "+vm.getUuid()+" oldstate: "+vm.getState()+ "Event :"+Event.OperationRetry);
                    }
                } catch (final NoTransitionException e1) {
                    throw new ConcurrentOperationException(e1.getMessage());
                }

                try {
                    _networkMgr.prepare(vmProfile, new DeployDestination(dest.getDataCenter(), dest.getPod(), null, null, dest.getStorageForDisks()), ctx);
                    if (vm.getHypervisorType() != HypervisorType.BareMetal) {
                        volumeMgr.prepare(vmProfile, dest);
                    }

                    //since StorageMgr succeeded in volume creation, reuse Volume for further tries until current cluster has capacity
                    if (!reuseVolume) {
                        reuseVolume = true;
                    }

                    Commands cmds = null;
                    vmGuru.finalizeVirtualMachineProfile(vmProfile, dest, ctx);

                    final VirtualMachineTO vmTO = hvGuru.implement(vmProfile);

                    handlePath(vmTO.getDisks(), vm.getHypervisorType());

                    cmds = new Commands(Command.OnError.Stop);

                    cmds.addCommand(new StartCommand(vmTO, dest.getHost(), getExecuteInSequence(vm.getHypervisorType())));

                    vmGuru.finalizeDeployment(cmds, vmProfile, dest, ctx);

                    // Get VM extraConfig from DB and set to VM TO
                    addExtraConfig(vmTO);

                    work = _workDao.findById(work.getId());
                    if (work == null || work.getStep() != Step.Prepare) {
                        throw new ConcurrentOperationException("Work steps have been changed: " + work);
                    }

                    _workDao.updateStep(work, Step.Starting);

                    _agentMgr.send(destHostId, cmds);

                    _workDao.updateStep(work, Step.Started);

                    startAnswer = cmds.getAnswer(StartAnswer.class);
                    if (startAnswer != null && startAnswer.getResult()) {
                        handlePath(vmTO.getDisks(), startAnswer.getIqnToData());

                        final String host_guid = startAnswer.getHost_guid();

                        if (host_guid != null) {
                            final HostVO finalHost = _resourceMgr.findHostByGuid(host_guid);
                            if (finalHost == null) {
                                throw new CloudRuntimeException("Host Guid " + host_guid + " doesn't exist in DB, something went wrong while processing start answer: "+startAnswer);
                            }
                            destHostId = finalHost.getId();
                        }
                        if (vmGuru.finalizeStart(vmProfile, destHostId, cmds, ctx)) {
                            syncDiskChainChange(startAnswer);

                            if (!changeState(vm, Event.OperationSucceeded, destHostId, work, Step.Done)) {
                                s_logger.error("Unable to transition to a new state. VM uuid: "+vm.getUuid()+    "VM oldstate:"+vm.getState()+"Event:"+Event.OperationSucceeded);
                                throw new ConcurrentOperationException("Failed to deploy VM"+ vm.getUuid());
                            }

                            // Update GPU device capacity
                            final GPUDeviceTO gpuDevice = startAnswer.getVirtualMachine().getGpuDevice();
                            if (gpuDevice != null) {
                                _resourceMgr.updateGPUDetails(destHostId, gpuDevice.getGroupDetails());
                            }

                            // Remove the information on whether it was a deploy vm request.The deployvm=true information
                            // is set only when the vm is being deployed. When a vm is started from a stop state the
                            // information isn't set,
                            if (userVmDetailsDao.findDetail(vm.getId(), VmDetailConstants.DEPLOY_VM) != null) {
                                userVmDetailsDao.removeDetail(vm.getId(), VmDetailConstants.DEPLOY_VM);
                            }

                            startedVm = vm;
                            if (s_logger.isDebugEnabled()) {
                                s_logger.debug("Start completed for VM " + vm);
                            }
                            final Host vmHost = _hostDao.findById(destHostId);
                            if (vmHost != null && (VirtualMachine.Type.ConsoleProxy.equals(vm.getType()) ||
                                    VirtualMachine.Type.SecondaryStorageVm.equals(vm.getType())) && caManager.canProvisionCertificates()) {
                                final Map<String, String> sshAccessDetails = _networkMgr.getSystemVMAccessDetails(vm);
                                for (int retries = 3; retries > 0; retries--) {
                                    try {
                                        setupAgentSecurity(vmHost, sshAccessDetails, vm);
                                        return;
                                    } catch (final Exception e) {
                                        s_logger.error("Retrying after catching exception while trying to secure agent for systemvm id=" + vm.getId(), e);
                                    }
                                }
                                throw new CloudRuntimeException("Failed to setup and secure agent for systemvm id=" + vm.getId());
                            }
                            return;
                        } else {
                            if (s_logger.isDebugEnabled()) {
                                s_logger.info("The guru did not like the answers so stopping " + vm);
                            }
                            StopCommand stopCmd = new StopCommand(vm, getExecuteInSequence(vm.getHypervisorType()), false);
                            stopCmd.setControlIp(getControlNicIpForVM(vm));
                            final StopCommand cmd = stopCmd;
                            final Answer answer = _agentMgr.easySend(destHostId, cmd);
                            if (answer != null && answer instanceof StopAnswer) {
                                final StopAnswer stopAns = (StopAnswer)answer;
                                if (vm.getType() == VirtualMachine.Type.User) {
                                    final String platform = stopAns.getPlatform();
                                    if (platform != null) {
                                        final Map<String,String> vmmetadata = new HashMap<String,String>();
                                        vmmetadata.put(vm.getInstanceName(), platform);
                                        syncVMMetaData(vmmetadata);
                                    }
                                }
                            }

                            if (answer == null || !answer.getResult()) {
                                s_logger.warn("Unable to stop " + vm + " due to " + (answer != null ? answer.getDetails() : "no answers"));
                                _haMgr.scheduleStop(vm, destHostId, WorkType.ForceStop);
                                throw new ExecutionException("Unable to stop this VM, "+vm.getUuid()+" so we are unable to retry the start operation");
                            }
                            throw new ExecutionException("Unable to start  VM:"+vm.getUuid()+" due to error in finalizeStart, not retrying");
                        }
                    }
                    s_logger.info("Unable to start VM on " + dest.getHost() + " due to " + (startAnswer == null ? " no start answer" : startAnswer.getDetails()));
                    if (startAnswer != null && startAnswer.getContextParam("stopRetry") != null) {
                        break;
                    }

                } catch (OperationTimedoutException e) {
                    s_logger.debug("Unable to send the start command to host " + dest.getHost()+" failed to start VM: "+vm.getUuid());
                    if (e.isActive()) {
                        _haMgr.scheduleStop(vm, destHostId, WorkType.CheckStop);
                    }
                    canRetry = false;
                    throw new AgentUnavailableException("Unable to start " + vm.getHostName(), destHostId, e);
                } catch (final ResourceUnavailableException e) {
                    s_logger.info("Unable to contact resource.", e);
                    if (!avoids.add(e)) {
                        if (e.getScope() == Volume.class || e.getScope() == Nic.class) {
                            throw e;
                        } else {
                            s_logger.warn("unexpected ResourceUnavailableException : " + e.getScope().getName(), e);
                            throw e;
                        }
                    }
                } catch (final InsufficientCapacityException e) {
                    s_logger.info("Insufficient capacity ", e);
                    if (!avoids.add(e)) {
                        if (e.getScope() == Volume.class || e.getScope() == Nic.class) {
                            throw e;
                        } else {
                            s_logger.warn("unexpected InsufficientCapacityException : " + e.getScope().getName(), e);
                        }
                    }
                } catch (final ExecutionException e) {
                    s_logger.error("Failed to start instance " + vm, e);
                    throw new AgentUnavailableException("Unable to start instance due to " + e.getMessage(), destHostId, e);
                } catch (final NoTransitionException e) {
                    s_logger.error("Failed to start instance " + vm, e);
                    throw new AgentUnavailableException("Unable to start instance due to " + e.getMessage(), destHostId, e);
                } finally {
                    if (startedVm == null && canRetry) {
                        final Step prevStep = work.getStep();
                        _workDao.updateStep(work, Step.Release);
                        // If previous step was started/ing && we got a valid answer
                        if ((prevStep == Step.Started || prevStep == Step.Starting) && startAnswer != null && startAnswer.getResult()) {  //TODO check the response of cleanup and record it in DB for retry
                            cleanup(vmGuru, vmProfile, work, Event.OperationFailed, false);
                        } else {
                            //if step is not starting/started, send cleanup command with force=true
                            cleanup(vmGuru, vmProfile, work, Event.OperationFailed, true);
                        }
                    }
                }
            }
        } finally {
            if (startedVm == null) {
                if (canRetry) {
                    try {
                        changeState(vm, Event.OperationFailed, null, work, Step.Done);
                    } catch (final NoTransitionException e) {
                        throw new ConcurrentOperationException(e.getMessage());
                    }
                }
            }

            if (planToDeploy != null) {
                planToDeploy.setAvoids(avoids);
            }
        }

        if (startedVm == null) {
            throw new CloudRuntimeException("Unable to start instance '" + vm.getHostName() + "' (" + vm.getUuid() + "), see management server log for details");
        }
    }

    // Add extra config data to the vmTO as a Map
    private void addExtraConfig(VirtualMachineTO vmTO) {
        Map<String, String> details = vmTO.getDetails();
        for (String key : details.keySet()) {
            if (key.startsWith(ApiConstants.EXTRA_CONFIG)) {
                vmTO.addExtraConfig(key, details.get(key));
            }
        }
    }

    // for managed storage on KVM, need to make sure the path field of the volume in question is populated with the IQN
    private void handlePath(final DiskTO[] disks, final HypervisorType hypervisorType) {
        if (hypervisorType != HypervisorType.KVM) {
            return;
        }

        if (disks != null) {
            for (final DiskTO disk : disks) {
                final Map<String, String> details = disk.getDetails();
                final boolean isManaged = details != null && Boolean.parseBoolean(details.get(DiskTO.MANAGED));

                if (isManaged && disk.getPath() == null) {
                    final Long volumeId = disk.getData().getId();
                    final VolumeVO volume = _volsDao.findById(volumeId);

                    disk.setPath(volume.get_iScsiName());

                    if (disk.getData() instanceof VolumeObjectTO) {
                        final VolumeObjectTO volTo = (VolumeObjectTO)disk.getData();

                        volTo.setPath(volume.get_iScsiName());
                    }

                    volume.setPath(volume.get_iScsiName());

                    _volsDao.update(volumeId, volume);
                }
            }
        }
    }

    // for managed storage on XenServer and VMware, need to update the DB with a path if the VDI/VMDK file was newly created
    private void handlePath(final DiskTO[] disks, final Map<String, Map<String, String>> iqnToData) {
        if (disks != null && iqnToData != null) {
            for (final DiskTO disk : disks) {
                final Map<String, String> details = disk.getDetails();
                final boolean isManaged = details != null && Boolean.parseBoolean(details.get(DiskTO.MANAGED));

                if (isManaged) {
                    final Long volumeId = disk.getData().getId();
                    final VolumeVO volume = _volsDao.findById(volumeId);
                    final String iScsiName = volume.get_iScsiName();

                    boolean update = false;

                    final Map<String, String> data = iqnToData.get(iScsiName);

                    if (data != null) {
                        final String path = data.get(StartAnswer.PATH);

                        if (path != null) {
                            volume.setPath(path);

                            update = true;
                        }

                        final String imageFormat = data.get(StartAnswer.IMAGE_FORMAT);

                        if (imageFormat != null) {
                            volume.setFormat(ImageFormat.valueOf(imageFormat));

                            update = true;
                        }

                        if (update) {
                            _volsDao.update(volumeId, volume);
                        }
                    }
                }
            }
        }
    }

    private void syncDiskChainChange(final StartAnswer answer) {
        final VirtualMachineTO vmSpec = answer.getVirtualMachine();

        for (final DiskTO disk : vmSpec.getDisks()) {
            if (disk.getType() != Volume.Type.ISO) {
                final VolumeObjectTO vol = (VolumeObjectTO)disk.getData();
                final VolumeVO volume = _volsDao.findById(vol.getId());

                // Use getPath() from VolumeVO to get a fresh copy of what's in the DB.
                // Before doing this, in a certain situation, getPath() from VolumeObjectTO
                // returned null instead of an actual path (because it was out of date with the DB).
                if(vol.getPath() != null) {
                    volumeMgr.updateVolumeDiskChain(vol.getId(), vol.getPath(), vol.getChainInfo());
                } else {
                    volumeMgr.updateVolumeDiskChain(vol.getId(), volume.getPath(), vol.getChainInfo());
                }
            }
        }
    }

    @Override
    public void stop(final String vmUuid) throws ResourceUnavailableException {
        try {
            advanceStop(vmUuid, false);
        } catch (final OperationTimedoutException e) {
            throw new AgentUnavailableException("Unable to stop vm because the operation to stop timed out", e.getAgentId(), e);
        } catch (final ConcurrentOperationException e) {
            throw new CloudRuntimeException("Unable to stop vm because of a concurrent operation", e);
        }

    }

    @Override
    public void stopForced(String vmUuid) throws ResourceUnavailableException {
        try {
            advanceStop(vmUuid, true);
        } catch (final OperationTimedoutException e) {
            throw new AgentUnavailableException("Unable to stop vm because the operation to stop timed out", e.getAgentId(), e);
        } catch (final ConcurrentOperationException e) {
            throw new CloudRuntimeException("Unable to stop vm because of a concurrent operation", e);
        }
    }

    @Override
    public boolean getExecuteInSequence(final HypervisorType hypervisorType) {
        if (HypervisorType.KVM == hypervisorType || HypervisorType.XenServer == hypervisorType || HypervisorType.Hyperv == hypervisorType || HypervisorType.LXC == hypervisorType) {
            return false;
        } else if (HypervisorType.VMware == hypervisorType) {
            final Boolean fullClone = HypervisorGuru.VmwareFullClone.value();
            return fullClone;
        } else {
            return ExecuteInSequence.value();
        }
    }

    private List<Map<String, String>> getVolumesToDisconnect(VirtualMachine vm) {
        List<Map<String, String>> volumesToDisconnect = new ArrayList<>();

        List<VolumeVO> volumes = _volsDao.findByInstance(vm.getId());

        if (CollectionUtils.isEmpty(volumes)) {
            return volumesToDisconnect;
        }

        for (VolumeVO volume : volumes) {
            StoragePoolVO storagePool = _storagePoolDao.findById(volume.getPoolId());

            if (storagePool != null && storagePool.isManaged()) {
                Map<String, String> info = new HashMap<>(3);

                info.put(DiskTO.STORAGE_HOST, storagePool.getHostAddress());
                info.put(DiskTO.STORAGE_PORT, String.valueOf(storagePool.getPort()));
                info.put(DiskTO.IQN, volume.get_iScsiName());

                volumesToDisconnect.add(info);
            }
        }

        return volumesToDisconnect;
    }

    protected boolean sendStop(final VirtualMachineGuru guru, final VirtualMachineProfile profile, final boolean force, final boolean checkBeforeCleanup) {
        final VirtualMachine vm = profile.getVirtualMachine();
        StopCommand stpCmd = new StopCommand(vm, getExecuteInSequence(vm.getHypervisorType()), checkBeforeCleanup);
        stpCmd.setControlIp(getControlNicIpForVM(vm));
        stpCmd.setVolumesToDisconnect(getVolumesToDisconnect(vm));
        final StopCommand stop = stpCmd;
        try {
            Answer answer = null;
            if(vm.getHostId() != null) {
                answer = _agentMgr.send(vm.getHostId(), stop);
            }
            if (answer != null && answer instanceof StopAnswer) {
                final StopAnswer stopAns = (StopAnswer)answer;
                if (vm.getType() == VirtualMachine.Type.User) {
                    final String platform = stopAns.getPlatform();
                    if (platform != null) {
                        final UserVmVO userVm = _userVmDao.findById(vm.getId());
                        _userVmDao.loadDetails(userVm);
                        userVm.setDetail(VmDetailConstants.PLATFORM, platform);
                        _userVmDao.saveDetails(userVm);
                    }
                }

                final GPUDeviceTO gpuDevice = stop.getGpuDevice();
                if (gpuDevice != null) {
                    _resourceMgr.updateGPUDetails(vm.getHostId(), gpuDevice.getGroupDetails());
                }
                if (!answer.getResult()) {
                    final String details = answer.getDetails();
                    s_logger.debug("Unable to stop VM due to " + details);
                    return false;
                }

                guru.finalizeStop(profile, answer);
            } else {
                s_logger.error("Invalid answer received in response to a StopCommand for " + vm.getInstanceName());
                return false;
            }

        } catch (final AgentUnavailableException e) {
            if (!force) {
                return false;
            }
        } catch (final OperationTimedoutException e) {
            if (!force) {
                return false;
            }
        }

        return true;
    }

    protected boolean cleanup(final VirtualMachineGuru guru, final VirtualMachineProfile profile, final ItWorkVO work, final Event event, final boolean cleanUpEvenIfUnableToStop) {
        final VirtualMachine vm = profile.getVirtualMachine();
        final State state = vm.getState();
        s_logger.debug("Cleaning up resources for the vm " + vm + " in " + state + " state");
        try {
            if (state == State.Starting) {
                if (work != null) {
                    final Step step = work.getStep();
                    if (step == Step.Starting && !cleanUpEvenIfUnableToStop) {
                        s_logger.warn("Unable to cleanup vm " + vm + "; work state is incorrect: " + step);
                        return false;
                    }

                    if (step == Step.Started || step == Step.Starting || step == Step.Release) {
                        if (vm.getHostId() != null) {
                            if (!sendStop(guru, profile, cleanUpEvenIfUnableToStop, false)) {
                                s_logger.warn("Failed to stop vm " + vm + " in " + State.Starting + " state as a part of cleanup process");
                                return false;
                            }
                        }
                    }

                    if (step != Step.Release && step != Step.Prepare && step != Step.Started && step != Step.Starting) {
                        s_logger.debug("Cleanup is not needed for vm " + vm + "; work state is incorrect: " + step);
                        return true;
                    }
                } else {
                    if (vm.getHostId() != null) {
                        if (!sendStop(guru, profile, cleanUpEvenIfUnableToStop, false)) {
                            s_logger.warn("Failed to stop vm " + vm + " in " + State.Starting + " state as a part of cleanup process");
                            return false;
                        }
                    }
                }

            } else if (state == State.Stopping) {
                if (vm.getHostId() != null) {
                    if (!sendStop(guru, profile, cleanUpEvenIfUnableToStop, false)) {
                        s_logger.warn("Failed to stop vm " + vm + " in " + State.Stopping + " state as a part of cleanup process");
                        return false;
                    }
                }
            } else if (state == State.Migrating) {
                if (vm.getHostId() != null) {
                    if (!sendStop(guru, profile, cleanUpEvenIfUnableToStop, false)) {
                        s_logger.warn("Failed to stop vm " + vm + " in " + State.Migrating + " state as a part of cleanup process");
                        return false;
                    }
                }
                if (vm.getLastHostId() != null) {
                    if (!sendStop(guru, profile, cleanUpEvenIfUnableToStop, false)) {
                        s_logger.warn("Failed to stop vm " + vm + " in " + State.Migrating + " state as a part of cleanup process");
                        return false;
                    }
                }
            } else if (state == State.Running) {
                if (!sendStop(guru, profile, cleanUpEvenIfUnableToStop, false)) {
                    s_logger.warn("Failed to stop vm " + vm + " in " + State.Running + " state as a part of cleanup process");
                    return false;
                }
            }
        } finally {
            try {
                _networkMgr.release(profile, cleanUpEvenIfUnableToStop);
                s_logger.debug("Successfully released network resources for the vm " + vm);
            } catch (final Exception e) {
                s_logger.warn("Unable to release some network resources.", e);
            }

            volumeMgr.release(profile);
            s_logger.debug(String.format("Successfully cleaned up resources for the VM %s in %s state", vm, state));
        }

        return true;
    }

    @Override
    public void advanceStop(final String vmUuid, final boolean cleanUpEvenIfUnableToStop)
            throws AgentUnavailableException, OperationTimedoutException, ConcurrentOperationException {

        final AsyncJobExecutionContext jobContext = AsyncJobExecutionContext.getCurrentExecutionContext();
        if (jobContext.isJobDispatchedBy(VmWorkConstants.VM_WORK_JOB_DISPATCHER)) {
            // avoid re-entrance

            VmWorkJobVO placeHolder = null;
            final VirtualMachine vm = _vmDao.findByUuid(vmUuid);
            placeHolder = createPlaceHolderWork(vm.getId());
            try {
                orchestrateStop(vmUuid, cleanUpEvenIfUnableToStop);
            } finally {
                if (placeHolder != null) {
                    _workJobDao.expunge(placeHolder.getId());
                }
            }

        } else {
            final Outcome<VirtualMachine> outcome = stopVmThroughJobQueue(vmUuid, cleanUpEvenIfUnableToStop);

            try {
                final VirtualMachine vm = outcome.get();
            } catch (final InterruptedException e) {
                throw new RuntimeException("Operation is interrupted", e);
            } catch (final java.util.concurrent.ExecutionException e) {
                throw new RuntimeException("Execution excetion", e);
            }

            final Object jobResult = _jobMgr.unmarshallResultObject(outcome.getJob());
            if (jobResult != null) {
                if (jobResult instanceof AgentUnavailableException) {
                    throw (AgentUnavailableException)jobResult;
                } else if (jobResult instanceof ConcurrentOperationException) {
                    throw (ConcurrentOperationException)jobResult;
                } else if (jobResult instanceof OperationTimedoutException) {
                    throw (OperationTimedoutException)jobResult;
                } else if (jobResult instanceof RuntimeException) {
                    throw (RuntimeException)jobResult;
                } else if (jobResult instanceof Throwable) {
                    throw new RuntimeException("Unexpected exception", (Throwable)jobResult);
                }
            }
        }
    }

    private void orchestrateStop(final String vmUuid, final boolean cleanUpEvenIfUnableToStop) throws AgentUnavailableException, OperationTimedoutException, ConcurrentOperationException {
        final VMInstanceVO vm = _vmDao.findByUuid(vmUuid);

        advanceStop(vm, cleanUpEvenIfUnableToStop);
    }

    private void advanceStop(final VMInstanceVO vm, final boolean cleanUpEvenIfUnableToStop) throws AgentUnavailableException, OperationTimedoutException,
    ConcurrentOperationException {
        final State state = vm.getState();
        if (state == State.Stopped) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("VM is already stopped: " + vm);
            }
            return;
        }

        if (state == State.Destroyed || state == State.Expunging || state == State.Error) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Stopped called on " + vm + " but the state is " + state);
            }
            return;
        }
        // grab outstanding work item if any
        final ItWorkVO work = _workDao.findByOutstandingWork(vm.getId(), vm.getState());
        if (work != null) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Found an outstanding work item for this vm " + vm + " with state:" + vm.getState() + ", work id:" + work.getId());
            }
        }
        final Long hostId = vm.getHostId();
        if (hostId == null) {
            if (!cleanUpEvenIfUnableToStop) {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("HostId is null but this is not a forced stop, cannot stop vm " + vm + " with state:" + vm.getState());
                }
                throw new CloudRuntimeException("Unable to stop " + vm);
            }
            try {
                stateTransitTo(vm, Event.AgentReportStopped, null, null);
            } catch (final NoTransitionException e) {
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
            return;
        } else {
            HostVO host = _hostDao.findById(hostId);
            if (!cleanUpEvenIfUnableToStop && vm.getState() == State.Running && host.getResourceState() == ResourceState.PrepareForMaintenance) {
                s_logger.debug("Host is in PrepareForMaintenance state - Stop VM operation on the VM id: " + vm.getId() + " is not allowed");
                throw new CloudRuntimeException("Stop VM operation on the VM id: " + vm.getId() + " is not allowed as host is preparing for maintenance mode");
            }
        }

        final VirtualMachineGuru vmGuru = getVmGuru(vm);
        final VirtualMachineProfile profile = new VirtualMachineProfileImpl(vm);

        try {
            if (!stateTransitTo(vm, Event.StopRequested, vm.getHostId())) {
                throw new ConcurrentOperationException("VM is being operated on.");
            }
        } catch (final NoTransitionException e1) {
            if (!cleanUpEvenIfUnableToStop) {
                throw new CloudRuntimeException("We cannot stop " + vm + " when it is in state " + vm.getState());
            }
            final boolean doCleanup = true;
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Unable to transition the state but we're moving on because it's forced stop");
            }

            if (doCleanup) {
                if (cleanup(vmGuru, new VirtualMachineProfileImpl(vm), work, Event.StopRequested, cleanUpEvenIfUnableToStop)) {
                    try {
                        if (s_logger.isDebugEnabled() && work != null) {
                            s_logger.debug("Updating work item to Done, id:" + work.getId());
                        }
                        if (!changeState(vm, Event.AgentReportStopped, null, work, Step.Done)) {
                            throw new CloudRuntimeException("Unable to stop " + vm);
                        }

                    } catch (final NoTransitionException e) {
                        s_logger.warn("Unable to cleanup " + vm);
                        throw new CloudRuntimeException("Unable to stop " + vm, e);
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

        final StopCommand stop = new StopCommand(vm, getExecuteInSequence(vm.getHypervisorType()), false, cleanUpEvenIfUnableToStop);
        stop.setControlIp(getControlNicIpForVM(vm));

        boolean stopped = false;
        Answer answer = null;
        try {
            answer = _agentMgr.send(vm.getHostId(), stop);
            if (answer != null) {
                if (answer instanceof StopAnswer) {
                    final StopAnswer stopAns = (StopAnswer)answer;
                    if (vm.getType() == VirtualMachine.Type.User) {
                        final String platform = stopAns.getPlatform();
                        if (platform != null) {
                            final UserVmVO userVm = _userVmDao.findById(vm.getId());
                            _userVmDao.loadDetails(userVm);
                            userVm.setDetail(VmDetailConstants.PLATFORM, platform);
                            _userVmDao.saveDetails(userVm);
                        }
                    }
                }
                stopped = answer.getResult();
                if (!stopped) {
                    throw new CloudRuntimeException("Unable to stop the virtual machine due to " + answer.getDetails());
                }
                vmGuru.finalizeStop(profile, answer);
                final GPUDeviceTO gpuDevice = stop.getGpuDevice();
                if (gpuDevice != null) {
                    _resourceMgr.updateGPUDetails(vm.getHostId(), gpuDevice.getGroupDetails());
                }
            } else {
                throw new CloudRuntimeException("Invalid answer received in response to a StopCommand on " + vm.instanceName);
            }

        } catch (final AgentUnavailableException e) {
            s_logger.warn("Unable to stop vm, agent unavailable: " + e.toString());
        } catch (final OperationTimedoutException e) {
            s_logger.warn("Unable to stop vm, operation timed out: " + e.toString());
        } finally {
            if (!stopped) {
                if (!cleanUpEvenIfUnableToStop) {
                    s_logger.warn("Unable to stop vm " + vm);
                    try {
                        stateTransitTo(vm, Event.OperationFailed, vm.getHostId());
                    } catch (final NoTransitionException e) {
                        s_logger.warn("Unable to transition the state " + vm);
                    }
                    throw new CloudRuntimeException("Unable to stop " + vm);
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
            _networkMgr.release(profile, cleanUpEvenIfUnableToStop);
            s_logger.debug("Successfully released network resources for the vm " + vm);
        } catch (final Exception e) {
            s_logger.warn("Unable to release some network resources.", e);
        }

        try {
            if (vm.getHypervisorType() != HypervisorType.BareMetal) {
                volumeMgr.release(profile);
                s_logger.debug("Successfully released storage resources for the vm " + vm);
            }
        } catch (final Exception e) {
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

            if (!stateTransitTo(vm, Event.OperationSucceeded, null)) {
                throw new CloudRuntimeException("unable to stop " + vm);
            }
        } catch (final NoTransitionException e) {
            s_logger.warn(e.getMessage());
            throw new CloudRuntimeException("Unable to stop " + vm);
        }
    }

    private void setStateMachine() {
        _stateMachine = VirtualMachine.State.getStateMachine();
    }

    protected boolean stateTransitTo(final VMInstanceVO vm, final VirtualMachine.Event e, final Long hostId, final String reservationId) throws NoTransitionException {
        // if there are active vm snapshots task, state change is not allowed

        vm.setReservationId(reservationId);
        return _stateMachine.transitTo(vm, e, new Pair<Long, Long>(vm.getHostId(), hostId), _vmDao);
    }

    @Override
    public boolean stateTransitTo(final VirtualMachine vm1, final VirtualMachine.Event e, final Long hostId) throws NoTransitionException {
        final VMInstanceVO vm = (VMInstanceVO)vm1;

        final State oldState = vm.getState();
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
    public void destroy(final String vmUuid, final boolean expunge) throws AgentUnavailableException, OperationTimedoutException, ConcurrentOperationException {
        VMInstanceVO vm = _vmDao.findByUuid(vmUuid);
        if (vm == null || vm.getState() == State.Destroyed || vm.getState() == State.Expunging || vm.getRemoved() != null) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Unable to find vm or vm is destroyed: " + vm);
            }
            return;
        }

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Destroying vm " + vm + ", expunge flag " + (expunge ? "on" : "off"));
        }

        advanceStop(vmUuid, VmDestroyForcestop.value());

        deleteVMSnapshots(vm, expunge);

        Transaction.execute(new TransactionCallbackWithExceptionNoReturn<CloudRuntimeException>() {
            @Override
            public void doInTransactionWithoutResult(final TransactionStatus status) throws CloudRuntimeException {
                VMInstanceVO vm = _vmDao.findByUuid(vmUuid);
                try {
                    if (!stateTransitTo(vm, VirtualMachine.Event.DestroyRequested, vm.getHostId())) {
                        s_logger.debug("Unable to destroy the vm because it is not in the correct state: " + vm);
                        throw new CloudRuntimeException("Unable to destroy " + vm);
                    } else {
                        if (expunge) {
                            if (!stateTransitTo(vm, VirtualMachine.Event.ExpungeOperation, vm.getHostId())) {
                                s_logger.debug("Unable to expunge the vm because it is not in the correct state: " + vm);
                                throw new CloudRuntimeException("Unable to expunge " + vm);
                            }
                        }
                    }
                } catch (final NoTransitionException e) {
                    s_logger.debug(e.getMessage());
                    throw new CloudRuntimeException("Unable to destroy " + vm, e);
                }
            }
        });
    }

    /**
     * Delete vm snapshots depending on vm's hypervisor type. For Vmware, vm snapshots removal is delegated to vm cleanup thread
     * to reduce tasks sent to hypervisor (one tasks to delete vm snapshots and vm itself
     * instead of one task for each vm snapshot plus another for the vm)
     * @param vm vm
     * @param expunge indicates if vm should be expunged
     */
    private void deleteVMSnapshots(VMInstanceVO vm, boolean expunge) {
        if (! vm.getHypervisorType().equals(HypervisorType.VMware)) {
            if (!_vmSnapshotMgr.deleteAllVMSnapshots(vm.getId(), null)) {
                s_logger.debug("Unable to delete all snapshots for " + vm);
                throw new CloudRuntimeException("Unable to delete vm snapshots for " + vm);
            }
        }
        else {
            if (expunge) {
                _vmSnapshotMgr.deleteVMSnapshotsFromDB(vm.getId());
            }
        }
    }

    protected boolean checkVmOnHost(final VirtualMachine vm, final long hostId) throws AgentUnavailableException, OperationTimedoutException {
        final Answer answer = _agentMgr.send(hostId, new CheckVirtualMachineCommand(vm.getInstanceName()));
        if (answer == null || !answer.getResult()) {
            return false;
        }
        if (answer instanceof CheckVirtualMachineAnswer) {
            final CheckVirtualMachineAnswer vmAnswer = (CheckVirtualMachineAnswer)answer;
            if (vmAnswer.getState() == PowerState.PowerOff) {
                return false;
            }
        }

        UserVmVO userVm = _userVmDao.findById(vm.getId());
        if (userVm != null) {
            List<VMSnapshotVO> vmSnapshots = _vmSnapshotDao.findByVm(vm.getId());
            RestoreVMSnapshotCommand command = _vmSnapshotMgr.createRestoreCommand(userVm, vmSnapshots);
            if (command != null) {
                RestoreVMSnapshotAnswer restoreVMSnapshotAnswer = (RestoreVMSnapshotAnswer) _agentMgr.send(hostId, command);
                if (restoreVMSnapshotAnswer == null || !restoreVMSnapshotAnswer.getResult()) {
                    s_logger.warn("Unable to restore the vm snapshot from image file after live migration of vm with vmsnapshots: " + restoreVMSnapshotAnswer.getDetails());
                }
            }
        }

        return true;
    }

    @Override
    public void storageMigration(final String vmUuid, final StoragePool destPool) {
        final AsyncJobExecutionContext jobContext = AsyncJobExecutionContext.getCurrentExecutionContext();
        if (jobContext.isJobDispatchedBy(VmWorkConstants.VM_WORK_JOB_DISPATCHER)) {
            // avoid re-entrance
            VmWorkJobVO placeHolder = null;
            final VirtualMachine vm = _vmDao.findByUuid(vmUuid);
            placeHolder = createPlaceHolderWork(vm.getId());
            try {
                orchestrateStorageMigration(vmUuid, destPool);
            } finally {
                if (placeHolder != null) {
                    _workJobDao.expunge(placeHolder.getId());
                }
            }
        } else {
            final Outcome<VirtualMachine> outcome = migrateVmStorageThroughJobQueue(vmUuid, destPool);

            try {
                final VirtualMachine vm = outcome.get();
            } catch (final InterruptedException e) {
                throw new RuntimeException("Operation is interrupted", e);
            } catch (final java.util.concurrent.ExecutionException e) {
                throw new RuntimeException("Execution excetion", e);
            }

            final Object jobResult = _jobMgr.unmarshallResultObject(outcome.getJob());
            if (jobResult != null) {
                if (jobResult instanceof RuntimeException) {
                    throw (RuntimeException)jobResult;
                } else if (jobResult instanceof Throwable) {
                    throw new RuntimeException("Unexpected exception", (Throwable)jobResult);
                }
            }
        }
    }

    private void orchestrateStorageMigration(final String vmUuid, final StoragePool destPool) {
        final VMInstanceVO vm = _vmDao.findByUuid(vmUuid);

        preStorageMigrationStateCheck(destPool, vm);

        try {
            if(s_logger.isDebugEnabled()) {
                s_logger.debug(String.format("Offline migration of %s vm %s with volumes",
                                vm.getHypervisorType().toString(),
                                vm.getInstanceName()));
            }

            migrateThroughHypervisorOrStorage(destPool, vm);

        } catch (ConcurrentOperationException
                | InsufficientCapacityException // possibly InsufficientVirtualNetworkCapacityException or InsufficientAddressCapacityException
                | StorageUnavailableException e) {
            String msg = String.format("Failed to migrate VM: %s", vmUuid);
            s_logger.debug(msg);
            throw new CloudRuntimeException(msg, e);
        } finally {
            try {
                stateTransitTo(vm, Event.AgentReportStopped, null);
            } catch (final NoTransitionException e) {
                String anotherMEssage = String.format("failed to change vm state of VM: %s", vmUuid);
                s_logger.debug(anotherMEssage);
                throw new CloudRuntimeException(anotherMEssage, e);
            }
        }
    }

    private Answer[] attemptHypervisorMigration(StoragePool destPool, VMInstanceVO vm) {
        final HypervisorGuru hvGuru = _hvGuruMgr.getGuru(vm.getHypervisorType());
        // OfflineVmwareMigration: in case of vmware call vcenter to do it for us.
        // OfflineVmwareMigration: should we check the proximity of source and destination
        // OfflineVmwareMigration: if we are in the same cluster/datacentre/pool or whatever?
        // OfflineVmwareMigration: we are checking on success to optionally delete an old vm if we are not
        List<Command> commandsToSend = hvGuru.finalizeMigrate(vm, destPool);

        Long hostId = vm.getHostId();
        // OfflineVmwareMigration: probably this is null when vm is stopped
        if(hostId == null) {
            hostId = vm.getLastHostId();
            if (s_logger.isDebugEnabled()) {
                s_logger.debug(String.format("host id is null, using last host id %d", hostId) );
            }
        }

        if(CollectionUtils.isNotEmpty(commandsToSend)) {
            Commands commandsContainer = new Commands(Command.OnError.Stop);
            commandsContainer.addCommands(commandsToSend);
            try {
                // OfflineVmwareMigration: change to the call back variety?
                // OfflineVmwareMigration: getting a Long seq to be filled with _agentMgr.send(hostId, commandsContainer, this)
                return  _agentMgr.send(hostId, commandsContainer);
            } catch (AgentUnavailableException | OperationTimedoutException e) {
                throw new CloudRuntimeException(String.format("Failed to migrate VM: %s", vm.getUuid()),e);
            }
        }
        return null;
    }

    private void afterHypervisorMigrationCleanup(StoragePool destPool, VMInstanceVO vm, HostVO srcHost, Long srcClusterId, Answer[] hypervisorMigrationResults) throws InsufficientCapacityException {
        boolean isDebugEnabled = s_logger.isDebugEnabled();
        if(isDebugEnabled) {
            String msg = String.format("cleaning up after hypervisor pool migration volumes for VM %s(%s) to pool %s(%s)", vm.getInstanceName(), vm.getUuid(), destPool.getName(), destPool.getUuid());
            s_logger.debug(msg);
        }
        setDestinationPoolAndReallocateNetwork(destPool, vm);
        // OfflineVmwareMigration: don't set this to null or have another way to address the command; twice migrating will lead to an NPE
        Long destPodId = destPool.getPodId();
        Long vmPodId = vm.getPodIdToDeployIn();
        if (destPodId == null || ! destPodId.equals(vmPodId)) {
            if(isDebugEnabled) {
                String msg = String.format("resetting lasHost for VM %s(%s) as pod (%s) is no good.", vm.getInstanceName(), vm.getUuid(), destPodId);
                s_logger.debug(msg);
            }

            vm.setLastHostId(null);
            vm.setPodIdToDeployIn(destPodId);
            // OfflineVmwareMigration: a consecutive migration will fail probably (no host not pod)
        }// else keep last host set for this vm
        markVolumesInPool(vm,destPool, hypervisorMigrationResults);
        // OfflineVmwareMigration: deal with answers, if (hypervisorMigrationResults.length > 0)
        // OfflineVmwareMigration: iterate over the volumes for data updates
    }

    private void markVolumesInPool(VMInstanceVO vm, StoragePool destPool, Answer[] hypervisorMigrationResults) {
        MigrateVmToPoolAnswer relevantAnswer = null;
        for (Answer answer : hypervisorMigrationResults) {
            if (s_logger.isTraceEnabled()) {
                s_logger.trace(String.format("received an %s: %s", answer.getClass().getSimpleName(), answer));
            }
            if (answer instanceof MigrateVmToPoolAnswer) {
                relevantAnswer = (MigrateVmToPoolAnswer) answer;
            }
        }
        if (relevantAnswer == null) {
            throw new CloudRuntimeException("no relevant migration results found");
        }
        List<VolumeVO> volumes = _volsDao.findUsableVolumesForInstance(vm.getId());
        if(s_logger.isDebugEnabled()) {
            String msg = String.format("found %d volumes for VM %s(uuid:%s, id:%d)", volumes.size(), vm.getInstanceName(), vm.getUuid(), vm.getId());
            s_logger.debug(msg);
        }
        for (VolumeObjectTO result : relevantAnswer.getVolumeTos() ) {
            if(s_logger.isDebugEnabled()) {
                s_logger.debug(String.format("updating volume (%d) with path '%s' on pool '%d'", result.getId(), result.getPath(), destPool.getId()));
            }
            VolumeVO volume = _volsDao.findById(result.getId());
            volume.setPath(result.getPath());
            volume.setPoolId(destPool.getId());
            _volsDao.update(volume.getId(), volume);
        }
    }

    private void migrateThroughHypervisorOrStorage(StoragePool destPool, VMInstanceVO vm) throws StorageUnavailableException, InsufficientCapacityException {
        final VirtualMachineProfile profile = new VirtualMachineProfileImpl(vm);
        final Long srchostId = vm.getHostId() != null ? vm.getHostId() : vm.getLastHostId();
        final HostVO srcHost = _hostDao.findById(srchostId);
        final Long srcClusterId = srcHost.getClusterId();
        Answer[] hypervisorMigrationResults = attemptHypervisorMigration(destPool, vm);
        boolean migrationResult = false;
        if (hypervisorMigrationResults == null) {
            // OfflineVmwareMigration: if the HypervisorGuru can't do it, let the volume manager take care of it.
            migrationResult = volumeMgr.storageMigration(profile, destPool);
            if (migrationResult) {
                afterStorageMigrationCleanup(destPool, vm, srcHost, srcClusterId);
            } else {
                s_logger.debug("Storage migration failed");
            }
        } else {
            afterHypervisorMigrationCleanup(destPool, vm, srcHost, srcClusterId, hypervisorMigrationResults);
        }
    }

    private void preStorageMigrationStateCheck(StoragePool destPool, VMInstanceVO vm) {
        if (destPool == null) {
            throw new CloudRuntimeException("Unable to migrate vm: missing destination storage pool");
        }

        checkDestinationForTags(destPool, vm);
        try {
            stateTransitTo(vm, Event.StorageMigrationRequested, null);
        } catch (final NoTransitionException e) {
            String msg = String.format("Unable to migrate vm: %s", vm.getUuid());
            s_logger.debug(msg);
            throw new CloudRuntimeException(msg, e);
        }
    }

    private void checkDestinationForTags(StoragePool destPool, VMInstanceVO vm) {
        List<VolumeVO> vols = _volsDao.findUsableVolumesForInstance(vm.getId());
        // OfflineVmwareMigration: iterate over volumes
        // OfflineVmwareMigration: get disk offering
        List<String> storageTags = storageMgr.getStoragePoolTagList(destPool.getId());
        for(Volume vol : vols) {
            DiskOfferingVO diskOffering = _diskOfferingDao.findById(vol.getDiskOfferingId());
            List<String> volumeTags = StringUtils.csvTagsToList(diskOffering.getTags());
            if(! matches(volumeTags, storageTags)) {
                String msg = String.format("destination pool '%s' with tags '%s', does not support the volume diskoffering for volume '%s' (tags: '%s') ",
                        destPool.getName(),
                        StringUtils.listToCsvTags(storageTags),
                        vol.getName(),
                        StringUtils.listToCsvTags(volumeTags)
                );
                throw new CloudRuntimeException(msg);
            }
        }
    }

    static boolean matches(List<String> volumeTags, List<String> storagePoolTags) {
        // OfflineVmwareMigration: commons collections 4 allows for Collections.containsAll(volumeTags,storagePoolTags);
        boolean result = true;
        if (volumeTags != null) {
            for (String tag : volumeTags) {
                // there is a volume tags so
                if (storagePoolTags == null || !storagePoolTags.contains(tag)) {
                    result = false;
                    break;
                }
            }
        }
        return result;
    }


    private void afterStorageMigrationCleanup(StoragePool destPool, VMInstanceVO vm, HostVO srcHost, Long srcClusterId) throws InsufficientCapacityException {
        setDestinationPoolAndReallocateNetwork(destPool, vm);

        //when start the vm next time, don;'t look at last_host_id, only choose the host based on volume/storage pool
        vm.setLastHostId(null);
        vm.setPodIdToDeployIn(destPool.getPodId());

        // If VM was cold migrated between clusters belonging to two different VMware DCs,
        // unregister the VM from the source host and cleanup the associated VM files.
        if (vm.getHypervisorType().equals(HypervisorType.VMware)) {
            afterStorageMigrationVmwareVMcleanup(destPool, vm, srcHost, srcClusterId);
        }
    }

    private void setDestinationPoolAndReallocateNetwork(StoragePool destPool, VMInstanceVO vm) throws InsufficientCapacityException {
        //if the vm is migrated to different pod in basic mode, need to reallocate ip

        if (destPool.getPodId() != null && !destPool.getPodId().equals(vm.getPodIdToDeployIn())) {
            if (s_logger.isDebugEnabled()) {
                String msg = String.format("as the pod for vm %s has changed we are reallocating its network", vm.getInstanceName());
                s_logger.debug(msg);
            }
            final DataCenterDeployment plan = new DataCenterDeployment(vm.getDataCenterId(), destPool.getPodId(), null, null, null, null);
            final VirtualMachineProfileImpl vmProfile = new VirtualMachineProfileImpl(vm, null, null, null, null);
            _networkMgr.reallocate(vmProfile, plan);
        }
    }

    private void afterStorageMigrationVmwareVMcleanup(StoragePool destPool, VMInstanceVO vm, HostVO srcHost, Long srcClusterId) {
        // OfflineVmwareMigration: this should only happen on storage migration, else the guru would already have issued the command
        final Long destClusterId = destPool.getClusterId();
        if (srcClusterId != null && destClusterId != null && ! srcClusterId.equals(destClusterId)) {
            final String srcDcName = _clusterDetailsDao.getVmwareDcName(srcClusterId);
            final String destDcName = _clusterDetailsDao.getVmwareDcName(destClusterId);
            if (srcDcName != null && destDcName != null && !srcDcName.equals(destDcName)) {
                removeStaleVmFromSource(vm, srcHost);
            }
        }
    }

    // OfflineVmwareMigration: on port forward refator this to be done in two
    // OfflineVmwareMigration: command creation in the guru.migrat method
    // OfflineVmwareMigration: sending up in the attemptHypevisorMigration with execute in sequence (responsibility of the guru)
    private void removeStaleVmFromSource(VMInstanceVO vm, HostVO srcHost) {
        s_logger.debug("Since VM's storage was successfully migrated across VMware Datacenters, unregistering VM: " + vm.getInstanceName() +
                " from source host: " + srcHost.getId());
        final UnregisterVMCommand uvc = new UnregisterVMCommand(vm.getInstanceName());
        uvc.setCleanupVmFiles(true);
        try {
            _agentMgr.send(srcHost.getId(), uvc);
        } catch (final Exception e) {
            throw new CloudRuntimeException("Failed to unregister VM: " + vm.getInstanceName() + " from source host: " + srcHost.getId() +
                    " after successfully migrating VM's storage across VMware Datacenters");
        }
    }

    @Override
    public void migrate(final String vmUuid, final long srcHostId, final DeployDestination dest)
            throws ResourceUnavailableException, ConcurrentOperationException {

        final AsyncJobExecutionContext jobContext = AsyncJobExecutionContext.getCurrentExecutionContext();
        if (jobContext.isJobDispatchedBy(VmWorkConstants.VM_WORK_JOB_DISPATCHER)) {
            // avoid re-entrance
            VmWorkJobVO placeHolder = null;
            final VirtualMachine vm = _vmDao.findByUuid(vmUuid);
            placeHolder = createPlaceHolderWork(vm.getId());
            try {
                orchestrateMigrate(vmUuid, srcHostId, dest);
            } finally {
                if (placeHolder != null) {
                    _workJobDao.expunge(placeHolder.getId());
                }
            }
        } else {
            final Outcome<VirtualMachine> outcome = migrateVmThroughJobQueue(vmUuid, srcHostId, dest);

            try {
                final VirtualMachine vm = outcome.get();
            } catch (final InterruptedException e) {
                throw new RuntimeException("Operation is interrupted", e);
            } catch (final java.util.concurrent.ExecutionException e) {
                throw new RuntimeException("Execution excetion", e);
            }

            final Object jobResult = _jobMgr.unmarshallResultObject(outcome.getJob());
            if (jobResult != null) {
                if (jobResult instanceof ResourceUnavailableException) {
                    throw (ResourceUnavailableException)jobResult;
                } else if (jobResult instanceof ConcurrentOperationException) {
                    throw (ConcurrentOperationException)jobResult;
                } else if (jobResult instanceof RuntimeException) {
                    throw (RuntimeException)jobResult;
                } else if (jobResult instanceof Throwable) {
                    throw new RuntimeException("Unexpected exception", (Throwable)jobResult);
                }

            }
        }
    }

    private void orchestrateMigrate(final String vmUuid, final long srcHostId, final DeployDestination dest) throws ResourceUnavailableException, ConcurrentOperationException {
        final VMInstanceVO vm = _vmDao.findByUuid(vmUuid);
        if (vm == null) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Unable to find the vm " + vmUuid);
            }
            throw new CloudRuntimeException("Unable to find a virtual machine with id " + vmUuid);
        }
        migrate(vm, srcHostId, dest);
    }

    protected void migrate(final VMInstanceVO vm, final long srcHostId, final DeployDestination dest) throws ResourceUnavailableException, ConcurrentOperationException {
        s_logger.info("Migrating " + vm + " to " + dest);

        final long dstHostId = dest.getHost().getId();
        final Host fromHost = _hostDao.findById(srcHostId);
        if (fromHost == null) {
            s_logger.info("Unable to find the host to migrate from: " + srcHostId);
            throw new CloudRuntimeException("Unable to find the host to migrate from: " + srcHostId);
        }

        if (fromHost.getClusterId().longValue() != dest.getCluster().getId()) {
            final List<VolumeVO> volumes = _volsDao.findCreatedByInstance(vm.getId());
            for (final VolumeVO volume : volumes) {
                if (!_storagePoolDao.findById(volume.getPoolId()).getScope().equals(ScopeType.ZONE)) {
                    s_logger.info("Source and destination host are not in same cluster and all volumes are not on zone wide primary store, unable to migrate to host: "
                            + dest.getHost().getId());
                    throw new CloudRuntimeException(
                            "Source and destination host are not in same cluster and all volumes are not on zone wide primary store, unable to migrate to host: "
                                    + dest.getHost().getId());
                }
            }
        }

        final VirtualMachineGuru vmGuru = getVmGuru(vm);

        if (vm.getState() != State.Running) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("VM is not Running, unable to migrate the vm " + vm);
            }
            throw new CloudRuntimeException("VM is not Running, unable to migrate the vm currently " + vm + " , current state: " + vm.getState().toString());
        }

        AlertManager.AlertType alertType = AlertManager.AlertType.ALERT_TYPE_USERVM_MIGRATE;
        if (VirtualMachine.Type.DomainRouter.equals(vm.getType())) {
            alertType = AlertManager.AlertType.ALERT_TYPE_DOMAIN_ROUTER_MIGRATE;
        } else if (VirtualMachine.Type.ConsoleProxy.equals(vm.getType())) {
            alertType = AlertManager.AlertType.ALERT_TYPE_CONSOLE_PROXY_MIGRATE;
        }

        final VirtualMachineProfile vmSrc = new VirtualMachineProfileImpl(vm);
        for (final NicProfile nic : _networkMgr.getNicProfiles(vm)) {
            vmSrc.addNic(nic);
        }

        final VirtualMachineProfile profile = new VirtualMachineProfileImpl(vm, null, _offeringDao.findById(vm.getId(), vm.getServiceOfferingId()), null, null);
        _networkMgr.prepareNicForMigration(profile, dest);
        volumeMgr.prepareForMigration(profile, dest);
        profile.setConfigDriveLabel(VmConfigDriveLabel.value());

        final VirtualMachineTO to = toVmTO(profile);
        final PrepareForMigrationCommand pfmc = new PrepareForMigrationCommand(to);

        ItWorkVO work = new ItWorkVO(UUID.randomUUID().toString(), _nodeId, State.Migrating, vm.getType(), vm.getId());
        work.setStep(Step.Prepare);
        work.setResourceType(ItWorkVO.ResourceType.Host);
        work.setResourceId(dstHostId);
        work = _workDao.persist(work);

        Answer pfma = null;
        try {
            pfma = _agentMgr.send(dstHostId, pfmc);
            if (pfma == null || !pfma.getResult()) {
                final String details = pfma != null ? pfma.getDetails() : "null answer returned";
                final String msg = "Unable to prepare for migration due to " + details;
                pfma = null;
                throw new AgentUnavailableException(msg, dstHostId);
            }
        } catch (final OperationTimedoutException e1) {
            throw new AgentUnavailableException("Operation timed out", dstHostId);
        } finally {
            if (pfma == null) {
                _networkMgr.rollbackNicForMigration(vmSrc, profile);
                work.setStep(Step.Done);
                _workDao.update(work.getId(), work);
            }
        }

        vm.setLastHostId(srcHostId);
        try {
            if (vm == null || vm.getHostId() == null || vm.getHostId() != srcHostId || !changeState(vm, Event.MigrationRequested, dstHostId, work, Step.Migrating)) {
                _networkMgr.rollbackNicForMigration(vmSrc, profile);
                s_logger.info("Migration cancelled because state has changed: " + vm);
                throw new ConcurrentOperationException("Migration cancelled because state has changed: " + vm);
            }
        } catch (final NoTransitionException e1) {
            _networkMgr.rollbackNicForMigration(vmSrc, profile);
            s_logger.info("Migration cancelled because " + e1.getMessage());
            throw new ConcurrentOperationException("Migration cancelled because " + e1.getMessage());
        }

        boolean migrated = false;
        Map<String, DpdkTO> dpdkInterfaceMapping = null;
        try {
            final boolean isWindows = _guestOsCategoryDao.findById(_guestOsDao.findById(vm.getGuestOSId()).getCategoryId()).getName().equalsIgnoreCase("Windows");
            final MigrateCommand mc = new MigrateCommand(vm.getInstanceName(), dest.getHost().getPrivateIpAddress(), isWindows, to, getExecuteInSequence(vm.getHypervisorType()));

            boolean kvmAutoConvergence = StorageManager.KvmAutoConvergence.value();
            mc.setAutoConvergence(kvmAutoConvergence);
            mc.setHostGuid(dest.getHost().getGuid());

            dpdkInterfaceMapping = ((PrepareForMigrationAnswer) pfma).getDpdkInterfaceMapping();
            if (MapUtils.isNotEmpty(dpdkInterfaceMapping)) {
                mc.setDpdkInterfaceMapping(dpdkInterfaceMapping);
            }

            try {
                final Answer ma = _agentMgr.send(vm.getLastHostId(), mc);
                if (ma == null || !ma.getResult()) {
                    final String details = ma != null ? ma.getDetails() : "null answer returned";
                    throw new CloudRuntimeException(details);
                }
            } catch (final OperationTimedoutException e) {
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
            } catch (final NoTransitionException e1) {
                throw new ConcurrentOperationException("Unable to change state due to " + e1.getMessage());
            }

            try {
                if (!checkVmOnHost(vm, dstHostId)) {
                    s_logger.error("Unable to complete migration for " + vm);
                    try {
                        _agentMgr.send(srcHostId, new Commands(cleanup(vm, dpdkInterfaceMapping)), null);
                    } catch (final AgentUnavailableException e) {
                        s_logger.error("AgentUnavailableException while cleanup on source host: " + srcHostId);
                    }
                    cleanup(vmGuru, new VirtualMachineProfileImpl(vm), work, Event.AgentReportStopped, true);
                    throw new CloudRuntimeException("Unable to complete migration for " + vm);
                }
            } catch (final OperationTimedoutException e) {
                s_logger.debug("Error while checking the vm " + vm + " on host " + dstHostId, e);
            }

            migrated = true;
        } finally {
            if (!migrated) {
                s_logger.info("Migration was unsuccessful.  Cleaning up: " + vm);
                _networkMgr.rollbackNicForMigration(vmSrc, profile);

                _alertMgr.sendAlert(alertType, fromHost.getDataCenterId(), fromHost.getPodId(),
                        "Unable to migrate vm " + vm.getInstanceName() + " from host " + fromHost.getName() + " in zone " + dest.getDataCenter().getName() + " and pod " +
                                dest.getPod().getName(), "Migrate Command failed.  Please check logs.");
                try {
                    _agentMgr.send(dstHostId, new Commands(cleanup(vm, dpdkInterfaceMapping)), null);
                } catch (final AgentUnavailableException ae) {
                    s_logger.info("Looks like the destination Host is unavailable for cleanup");
                }

                try {
                    stateTransitTo(vm, Event.OperationFailed, srcHostId);
                } catch (final NoTransitionException e) {
                    s_logger.warn(e.getMessage());
                }
            } else {
                _networkMgr.commitNicForMigration(vmSrc, profile);
            }

            work.setStep(Step.Done);
            _workDao.update(work.getId(), work);
        }
    }

    /**
     * We create the mapping of volumes and storage pool to migrate the VMs according to the information sent by the user.
     * If the user did not enter a complete mapping, the volumes that were left behind will be auto mapped using {@link #createStoragePoolMappingsForVolumes(VirtualMachineProfile, Host, Map, List)}
     */
    protected Map<Volume, StoragePool> createMappingVolumeAndStoragePool(VirtualMachineProfile profile, Host targetHost, Map<Long, Long> userDefinedMapOfVolumesAndStoragePools) {
        Map<Volume, StoragePool> volumeToPoolObjectMap = buildMapUsingUserInformation(profile, targetHost, userDefinedMapOfVolumesAndStoragePools);

        List<Volume> volumesNotMapped = findVolumesThatWereNotMappedByTheUser(profile, volumeToPoolObjectMap);
        createStoragePoolMappingsForVolumes(profile, targetHost, volumeToPoolObjectMap, volumesNotMapped);
        return volumeToPoolObjectMap;
    }

    /**
     *  Given the map of volume to target storage pool entered by the user, we check for other volumes that the VM might have and were not configured.
     *  This map can be then used by CloudStack to find new target storage pools according to the target host.
     */
    protected List<Volume> findVolumesThatWereNotMappedByTheUser(VirtualMachineProfile profile, Map<Volume, StoragePool> volumeToStoragePoolObjectMap) {
        List<VolumeVO> allVolumes = _volsDao.findUsableVolumesForInstance(profile.getId());
        List<Volume> volumesNotMapped = new ArrayList<>();
        for (Volume volume : allVolumes) {
            if (!volumeToStoragePoolObjectMap.containsKey(volume)) {
                volumesNotMapped.add(volume);
            }
        }
        return volumesNotMapped;
    }

    /**
     *  Builds the map of storage pools and volumes with the information entered by the user. Before creating the an entry we validate if the migration is feasible checking if the migration is allowed and if the target host can access the defined target storage pool.
     */
    protected Map<Volume, StoragePool> buildMapUsingUserInformation(VirtualMachineProfile profile, Host targetHost, Map<Long, Long> userDefinedVolumeToStoragePoolMap) {
        Map<Volume, StoragePool> volumeToPoolObjectMap = new HashMap<>();
        if (MapUtils.isEmpty(userDefinedVolumeToStoragePoolMap)) {
            return volumeToPoolObjectMap;
        }
        for(Long volumeId: userDefinedVolumeToStoragePoolMap.keySet()) {
            VolumeVO volume = _volsDao.findById(volumeId);

            Long poolId = userDefinedVolumeToStoragePoolMap.get(volumeId);
            StoragePoolVO targetPool = _storagePoolDao.findById(poolId);
            StoragePoolVO currentPool = _storagePoolDao.findById(volume.getPoolId());

            executeManagedStorageChecksWhenTargetStoragePoolProvided(currentPool, volume, targetPool);
            if (_poolHostDao.findByPoolHost(targetPool.getId(), targetHost.getId()) == null) {
                throw new CloudRuntimeException(
                        String.format("Cannot migrate the volume [%s] to the storage pool [%s] while migrating VM [%s] to target host [%s]. The host does not have access to the storage pool entered.",
                                volume.getUuid(), targetPool.getUuid(), profile.getUuid(), targetHost.getUuid()));
            }
            if (currentPool.getId() == targetPool.getId()) {
                s_logger.info(String.format("The volume [%s] is already allocated in storage pool [%s].", volume.getUuid(), targetPool.getUuid()));
            }
            volumeToPoolObjectMap.put(volume, targetPool);
        }
        return volumeToPoolObjectMap;
    }

    /**
     *  Executes the managed storage checks for the mapping<volume, storage pool> entered by the user. The checks execute by this method are the following.
     *  <ul>
     *      <li> If the current storage pool of the volume is not a managed storage, we do not need to validate anything here.
     *      <li> If the current storage pool is a managed storage and the target storage pool ID is different from the current one, we throw an exception.
     *  </ul>
     */
    protected void executeManagedStorageChecksWhenTargetStoragePoolProvided(StoragePoolVO currentPool, VolumeVO volume, StoragePoolVO targetPool) {
        if (!currentPool.isManaged()) {
            return;
        }
        if (currentPool.getId() == targetPool.getId()) {
            return;
        }
        throw new CloudRuntimeException(String.format("Currently, a volume on managed storage can only be 'migrated' to itself " + "[volumeId=%s, currentStoragePoolId=%s, targetStoragePoolId=%s].",
                volume.getUuid(), currentPool.getUuid(), targetPool.getUuid()));
    }

    /**
     * For each one of the volumes we will map it to a storage pool that is available via the target host.
     * An exception is thrown if we cannot find a storage pool that is accessible in the target host to migrate the volume to.
     */
    protected void createStoragePoolMappingsForVolumes(VirtualMachineProfile profile, Host targetHost, Map<Volume, StoragePool> volumeToPoolObjectMap, List<Volume> allVolumes) {
        for (Volume volume : allVolumes) {
            StoragePoolVO currentPool = _storagePoolDao.findById(volume.getPoolId());

            executeManagedStorageChecksWhenTargetStoragePoolNotProvided(targetHost, currentPool, volume);
            if (ScopeType.HOST.equals(currentPool.getScope()) || isStorageCrossClusterMigration(targetHost, currentPool)) {
                createVolumeToStoragePoolMappingIfPossible(profile, targetHost, volumeToPoolObjectMap, volume, currentPool);
            } else {
                volumeToPoolObjectMap.put(volume, currentPool);
            }
        }
    }

    /**
     *  Executes the managed storage checks for the volumes that the user has not entered a mapping of <volume, storage pool>. The following checks are performed.
     *   <ul>
     *      <li> If the current storage pool is not a managed storage, we do not need to proceed with this method;
     *      <li> We check if the target host has access to the current managed storage pool. If it does not have an exception will be thrown.
     *   </ul>
     */
    protected void executeManagedStorageChecksWhenTargetStoragePoolNotProvided(Host targetHost, StoragePoolVO currentPool, Volume volume) {
        if (!currentPool.isManaged()) {
            return;
        }
        if (_poolHostDao.findByPoolHost(currentPool.getId(), targetHost.getId()) == null) {
            throw new CloudRuntimeException(String.format("The target host does not have access to the volume's managed storage pool. [volumeId=%s, storageId=%s, targetHostId=%s].", volume.getUuid(),
                    currentPool.getUuid(), targetHost.getUuid()));
        }
    }

    /**
     *  Return true if the VM migration is a cross cluster migration. To execute that, we check if the volume current storage pool cluster is different from the target host cluster.
     */
    protected boolean isStorageCrossClusterMigration(Host targetHost, StoragePoolVO currentPool) {
        return ScopeType.CLUSTER.equals(currentPool.getScope()) && currentPool.getClusterId() != targetHost.getClusterId();
    }

    /**
     * We will add a mapping of volume to storage pool if needed. The conditions to add a mapping are the following:
     * <ul>
     *  <li> The candidate storage pool where the volume is to be allocated can be accessed by the target host
     *  <li> If no storage pool is found to allocate the volume we throw an exception.
     * </ul>
     *
     * Side note: this method should only be called if the volume is on local storage or if we are executing a cross cluster migration.
     */
    protected void createVolumeToStoragePoolMappingIfPossible(VirtualMachineProfile profile, Host targetHost, Map<Volume, StoragePool> volumeToPoolObjectMap, Volume volume,
            StoragePoolVO currentPool) {
        List<StoragePool> storagePoolList = getCandidateStoragePoolsToMigrateLocalVolume(profile, targetHost, volume);

        if (CollectionUtils.isEmpty(storagePoolList)) {
            throw new CloudRuntimeException(String.format("There is not storage pools available at the target host [%s] to migrate volume [%s]", targetHost.getUuid(), volume.getUuid()));
        }

        Collections.shuffle(storagePoolList);
        boolean canTargetHostAccessVolumeCurrentStoragePool = false;
        for (StoragePool storagePool : storagePoolList) {
            if (storagePool.getId() == currentPool.getId()) {
                canTargetHostAccessVolumeCurrentStoragePool = true;
                break;
            }

        }
        if (!canTargetHostAccessVolumeCurrentStoragePool) {
            volumeToPoolObjectMap.put(volume, _storagePoolDao.findByUuid(storagePoolList.get(0).getUuid()));
        }
    }

    /**
     * We use {@link StoragePoolAllocator} objects to find storage pools connected to the targetHost where we would be able to allocate the given volume.
     */
    protected List<StoragePool> getCandidateStoragePoolsToMigrateLocalVolume(VirtualMachineProfile profile, Host targetHost, Volume volume) {
        List<StoragePool> poolList = new ArrayList<>();

        DiskOfferingVO diskOffering = _diskOfferingDao.findById(volume.getDiskOfferingId());
        DiskProfile diskProfile = new DiskProfile(volume, diskOffering, profile.getHypervisorType());
        DataCenterDeployment plan = new DataCenterDeployment(targetHost.getDataCenterId(), targetHost.getPodId(), targetHost.getClusterId(), targetHost.getId(), null, null);
        ExcludeList avoid = new ExcludeList();

        StoragePoolVO volumeStoragePool = _storagePoolDao.findById(volume.getPoolId());
        if (volumeStoragePool.isLocal()) {
            diskProfile.setUseLocalStorage(true);
        }
        for (StoragePoolAllocator allocator : _storagePoolAllocators) {
            List<StoragePool> poolListFromAllocator = allocator.allocateToPool(diskProfile, profile, plan, avoid, StoragePoolAllocator.RETURN_UPTO_ALL);
            if (CollectionUtils.isEmpty(poolListFromAllocator)) {
                continue;
            }
            for (StoragePool pool : poolListFromAllocator) {
                if (pool.isLocal() || isStorageCrossClusterMigration(targetHost, volumeStoragePool)) {
                    poolList.add(pool);
                }
            }
        }
        return poolList;
    }

    private <T extends VMInstanceVO> void moveVmToMigratingState(final T vm, final Long hostId, final ItWorkVO work) throws ConcurrentOperationException {
        // Put the vm in migrating state.
        try {
            if (!changeState(vm, Event.MigrationRequested, hostId, work, Step.Migrating)) {
                s_logger.info("Migration cancelled because state has changed: " + vm);
                throw new ConcurrentOperationException("Migration cancelled because state has changed: " + vm);
            }
        } catch (final NoTransitionException e) {
            s_logger.info("Migration cancelled because " + e.getMessage());
            throw new ConcurrentOperationException("Migration cancelled because " + e.getMessage());
        }
    }

    private <T extends VMInstanceVO> void moveVmOutofMigratingStateOnSuccess(final T vm, final Long hostId, final ItWorkVO work) throws ConcurrentOperationException {
        // Put the vm in running state.
        try {
            if (!changeState(vm, Event.OperationSucceeded, hostId, work, Step.Started)) {
                s_logger.error("Unable to change the state for " + vm);
                throw new ConcurrentOperationException("Unable to change the state for " + vm);
            }
        } catch (final NoTransitionException e) {
            s_logger.error("Unable to change state due to " + e.getMessage());
            throw new ConcurrentOperationException("Unable to change state due to " + e.getMessage());
        }
    }

    @Override
    public void migrateWithStorage(final String vmUuid, final long srcHostId, final long destHostId, final Map<Long, Long> volumeToPool)
            throws ResourceUnavailableException, ConcurrentOperationException {

        final AsyncJobExecutionContext jobContext = AsyncJobExecutionContext.getCurrentExecutionContext();
        if (jobContext.isJobDispatchedBy(VmWorkConstants.VM_WORK_JOB_DISPATCHER)) {
            // avoid re-entrance

            VmWorkJobVO placeHolder = null;
            final VirtualMachine vm = _vmDao.findByUuid(vmUuid);
            placeHolder = createPlaceHolderWork(vm.getId());
            try {
                orchestrateMigrateWithStorage(vmUuid, srcHostId, destHostId, volumeToPool);
            } finally {
                if (placeHolder != null) {
                    _workJobDao.expunge(placeHolder.getId());
                }
            }

        } else {
            final Outcome<VirtualMachine> outcome = migrateVmWithStorageThroughJobQueue(vmUuid, srcHostId, destHostId, volumeToPool);

            try {
                final VirtualMachine vm = outcome.get();
            } catch (final InterruptedException e) {
                throw new RuntimeException("Operation is interrupted", e);
            } catch (final java.util.concurrent.ExecutionException e) {
                throw new RuntimeException("Execution excetion", e);
            }

            final Object jobException = _jobMgr.unmarshallResultObject(outcome.getJob());
            if (jobException != null) {
                if (jobException instanceof ResourceUnavailableException) {
                    throw (ResourceUnavailableException)jobException;
                } else if (jobException instanceof ConcurrentOperationException) {
                    throw (ConcurrentOperationException)jobException;
                } else if (jobException instanceof RuntimeException) {
                    throw (RuntimeException)jobException;
                } else if (jobException instanceof Throwable) {
                    throw new RuntimeException("Unexpected exception", (Throwable)jobException);
                }
            }
        }
    }

    private void orchestrateMigrateWithStorage(final String vmUuid, final long srcHostId, final long destHostId, final Map<Long, Long> volumeToPool) throws ResourceUnavailableException,
    ConcurrentOperationException {

        final VMInstanceVO vm = _vmDao.findByUuid(vmUuid);

        final HostVO srcHost = _hostDao.findById(srcHostId);
        final HostVO destHost = _hostDao.findById(destHostId);
        final VirtualMachineGuru vmGuru = getVmGuru(vm);

        final DataCenterVO dc = _dcDao.findById(destHost.getDataCenterId());
        final HostPodVO pod = _podDao.findById(destHost.getPodId());
        final Cluster cluster = _clusterDao.findById(destHost.getClusterId());
        final DeployDestination destination = new DeployDestination(dc, pod, cluster, destHost);

        // Create a map of which volume should go in which storage pool.
        final VirtualMachineProfile profile = new VirtualMachineProfileImpl(vm);
        final Map<Volume, StoragePool> volumeToPoolMap = createMappingVolumeAndStoragePool(profile, destHost, volumeToPool);

        // If none of the volumes have to be migrated, fail the call. Administrator needs to make a call for migrating
        // a vm and not migrating a vm with storage.
        if (volumeToPoolMap == null || volumeToPoolMap.isEmpty()) {
            throw new InvalidParameterValueException("Migration of the vm " + vm + "from host " + srcHost + " to destination host " + destHost +
                    " doesn't involve migrating the volumes.");
        }

        AlertManager.AlertType alertType = AlertManager.AlertType.ALERT_TYPE_USERVM_MIGRATE;
        if (VirtualMachine.Type.DomainRouter.equals(vm.getType())) {
            alertType = AlertManager.AlertType.ALERT_TYPE_DOMAIN_ROUTER_MIGRATE;
        } else if (VirtualMachine.Type.ConsoleProxy.equals(vm.getType())) {
            alertType = AlertManager.AlertType.ALERT_TYPE_CONSOLE_PROXY_MIGRATE;
        }

        _networkMgr.prepareNicForMigration(profile, destination);
        volumeMgr.prepareForMigration(profile, destination);
        final HypervisorGuru hvGuru = _hvGuruMgr.getGuru(vm.getHypervisorType());
        final VirtualMachineTO to = hvGuru.implement(profile);

        ItWorkVO work = new ItWorkVO(UUID.randomUUID().toString(), _nodeId, State.Migrating, vm.getType(), vm.getId());
        work.setStep(Step.Prepare);
        work.setResourceType(ItWorkVO.ResourceType.Host);
        work.setResourceId(destHostId);
        work = _workDao.persist(work);


        // Put the vm in migrating state.
        vm.setLastHostId(srcHostId);
        vm.setPodIdToDeployIn(destHost.getPodId());
        moveVmToMigratingState(vm, destHostId, work);

        boolean migrated = false;
        try {

            // config drive: Detach the config drive at source host
            // After migration successful attach the config drive in destination host
            // On migration failure VM will be stopped, So configIso will be deleted

            Nic defaultNic = _networkModel.getDefaultNic(vm.getId());

            List<String[]> vmData = null;
            if (defaultNic != null) {
                UserVmVO userVm = _userVmDao.findById(vm.getId());
                Map<String, String> details = userVmDetailsDao.listDetailsKeyPairs(vm.getId());
                userVm.setDetails(details);

                Network network = _networkModel.getNetwork(defaultNic.getNetworkId());
                if (_networkModel.isSharedNetworkWithoutServices(network.getId())) {
                    final String serviceOffering = _serviceOfferingDao.findByIdIncludingRemoved(vm.getId(), vm.getServiceOfferingId()).getDisplayText();
                    boolean isWindows = _guestOSCategoryDao.findById(_guestOSDao.findById(vm.getGuestOSId()).getCategoryId()).getName().equalsIgnoreCase("Windows");

                    vmData = _networkModel.generateVmData(userVm.getUserData(), serviceOffering, vm.getDataCenterId(), vm.getInstanceName(), vm.getHostName(), vm.getId(),
                            vm.getUuid(), defaultNic.getMacAddress(), userVm.getDetail("SSH.PublicKey"), (String) profile.getParameter(VirtualMachineProfile.Param.VmPassword), isWindows);
                    String vmName = vm.getInstanceName();
                    String configDriveIsoRootFolder = "/tmp";
                    String isoFile = configDriveIsoRootFolder + "/" + vmName + "/configDrive/" + vmName + ".iso";
                    profile.setVmData(vmData);
                    profile.setConfigDriveLabel(VmConfigDriveLabel.value());
                    profile.setConfigDriveIsoRootFolder(configDriveIsoRootFolder);
                    profile.setConfigDriveIsoFile(isoFile);

                    // At source host detach the config drive iso.
                    AttachOrDettachConfigDriveCommand dettachCommand = new AttachOrDettachConfigDriveCommand(vm.getInstanceName(), vmData, VmConfigDriveLabel.value(), false);
                    try {
                        _agentMgr.send(srcHost.getId(), dettachCommand);
                        s_logger.debug("Deleted config drive ISO for  vm " + vm.getInstanceName() + " In host " + srcHost);
                    } catch (OperationTimedoutException e) {
                        s_logger.debug("TIme out occured while exeuting command AttachOrDettachConfigDrive " + e.getMessage());

                    }

                }
            }

            // Migrate the vm and its volume.
            volumeMgr.migrateVolumes(vm, to, srcHost, destHost, volumeToPoolMap);

            // Put the vm back to running state.
            moveVmOutofMigratingStateOnSuccess(vm, destHost.getId(), work);

            try {
                if (!checkVmOnHost(vm, destHostId)) {
                    s_logger.error("Vm not found on destination host. Unable to complete migration for " + vm);
                    try {
                        _agentMgr.send(srcHostId, new Commands(cleanup(vm.getInstanceName())), null);
                    } catch (final AgentUnavailableException e) {
                        s_logger.error("AgentUnavailableException while cleanup on source host: " + srcHostId);
                    }
                    cleanup(vmGuru, new VirtualMachineProfileImpl(vm), work, Event.AgentReportStopped, true);
                    throw new CloudRuntimeException("VM not found on desintation host. Unable to complete migration for " + vm);
                }
            } catch (final OperationTimedoutException e) {
                s_logger.warn("Error while checking the vm " + vm + " is on host " + destHost, e);
            }

            migrated = true;
        } finally {
            if (!migrated) {
                s_logger.info("Migration was unsuccessful.  Cleaning up: " + vm);
                _alertMgr.sendAlert(alertType, srcHost.getDataCenterId(), srcHost.getPodId(),
                        "Unable to migrate vm " + vm.getInstanceName() + " from host " + srcHost.getName() + " in zone " + dc.getName() + " and pod " + dc.getName(),
                        "Migrate Command failed.  Please check logs.");
                try {
                    _agentMgr.send(destHostId, new Commands(cleanup(vm.getInstanceName())), null);
                    vm.setPodIdToDeployIn(srcHost.getPodId());
                    stateTransitTo(vm, Event.OperationFailed, srcHostId);
                } catch (final AgentUnavailableException e) {
                    s_logger.warn("Looks like the destination Host is unavailable for cleanup.", e);
                } catch (final NoTransitionException e) {
                    s_logger.error("Error while transitioning vm from migrating to running state.", e);
                }
            }

            work.setStep(Step.Done);
            _workDao.update(work.getId(), work);
        }
    }

    @Override
    public VirtualMachineTO toVmTO(final VirtualMachineProfile profile) {
        final HypervisorGuru hvGuru = _hvGuruMgr.getGuru(profile.getVirtualMachine().getHypervisorType());
        final VirtualMachineTO to = hvGuru.implement(profile);
        return to;
    }

    protected void cancelWorkItems(final long nodeId) {
        final GlobalLock scanLock = GlobalLock.getInternLock("vmmgr.cancel.workitem");

        try {
            if (scanLock.lock(3)) {
                try {
                    final List<ItWorkVO> works = _workDao.listWorkInProgressFor(nodeId);
                    for (final ItWorkVO work : works) {
                        s_logger.info("Handling unfinished work item: " + work);
                        try {
                            final VMInstanceVO vm = _vmDao.findById(work.getInstanceId());
                            if (vm != null) {
                                if (work.getType() == State.Starting) {
                                    _haMgr.scheduleRestart(vm, true);
                                    work.setManagementServerId(_nodeId);
                                    work.setStep(Step.Done);
                                    _workDao.update(work.getId(), work);
                                } else if (work.getType() == State.Stopping) {
                                    _haMgr.scheduleStop(vm, vm.getHostId(), WorkType.CheckStop);
                                    work.setManagementServerId(_nodeId);
                                    work.setStep(Step.Done);
                                    _workDao.update(work.getId(), work);
                                } else if (work.getType() == State.Migrating) {
                                    _haMgr.scheduleMigration(vm);
                                    work.setStep(Step.Done);
                                    _workDao.update(work.getId(), work);
                                }
                            }
                        } catch (final Exception e) {
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
    public void migrateAway(final String vmUuid, final long srcHostId) throws InsufficientServerCapacityException {
        final AsyncJobExecutionContext jobContext = AsyncJobExecutionContext.getCurrentExecutionContext();
        if (jobContext.isJobDispatchedBy(VmWorkConstants.VM_WORK_JOB_DISPATCHER)) {
            // avoid re-entrance

            VmWorkJobVO placeHolder = null;
            final VirtualMachine vm = _vmDao.findByUuid(vmUuid);
            placeHolder = createPlaceHolderWork(vm.getId());
            try {
                try {
                    orchestrateMigrateAway(vmUuid, srcHostId, null);
                } catch (final InsufficientServerCapacityException e) {
                    s_logger.warn("Failed to deploy vm " + vmUuid + " with original planner, sending HAPlanner");
                    orchestrateMigrateAway(vmUuid, srcHostId, _haMgr.getHAPlanner());
                }
            } finally {
                _workJobDao.expunge(placeHolder.getId());
            }
        } else {
            final Outcome<VirtualMachine> outcome = migrateVmAwayThroughJobQueue(vmUuid, srcHostId);

            try {
                final VirtualMachine vm = outcome.get();
            } catch (final InterruptedException e) {
                throw new RuntimeException("Operation is interrupted", e);
            } catch (final java.util.concurrent.ExecutionException e) {
                throw new RuntimeException("Execution excetion", e);
            }

            final Object jobException = _jobMgr.unmarshallResultObject(outcome.getJob());
            if (jobException != null) {
                if (jobException instanceof InsufficientServerCapacityException) {
                    throw (InsufficientServerCapacityException)jobException;
                } else if (jobException instanceof ConcurrentOperationException) {
                    throw (ConcurrentOperationException)jobException;
                } else if (jobException instanceof RuntimeException) {
                    throw (RuntimeException)jobException;
                } else if (jobException instanceof Throwable) {
                    throw new RuntimeException("Unexpected exception", (Throwable)jobException);
                }
            }
        }
    }

    private void orchestrateMigrateAway(final String vmUuid, final long srcHostId, final DeploymentPlanner planner) throws InsufficientServerCapacityException {
        final VMInstanceVO vm = _vmDao.findByUuid(vmUuid);
        if (vm == null) {
            s_logger.debug("Unable to find a VM for " + vmUuid);
            throw new CloudRuntimeException("Unable to find " + vmUuid);
        }

        ServiceOfferingVO offeringVO = _offeringDao.findById(vm.getId(), vm.getServiceOfferingId());
        final VirtualMachineProfile profile = new VirtualMachineProfileImpl(vm, null, offeringVO, null, null);

        final Long hostId = vm.getHostId();
        if (hostId == null) {
            s_logger.debug("Unable to migrate because the VM doesn't have a host id: " + vm);
            throw new CloudRuntimeException("Unable to migrate " + vmUuid);
        }

        final Host host = _hostDao.findById(hostId);
        Long poolId = null;
        final List<VolumeVO> vols = _volsDao.findReadyRootVolumesByInstance(vm.getId());
        for (final VolumeVO rootVolumeOfVm : vols) {
            final StoragePoolVO rootDiskPool = _storagePoolDao.findById(rootVolumeOfVm.getPoolId());
            if (rootDiskPool != null) {
                poolId = rootDiskPool.getId();
            }
        }

        final DataCenterDeployment plan = new DataCenterDeployment(host.getDataCenterId(), host.getPodId(), host.getClusterId(), null, poolId, null);
        final ExcludeList excludes = new ExcludeList();
        excludes.addHost(hostId);

        DeployDestination dest = null;
        while (true) {

            try {
                dest = _dpMgr.planDeployment(profile, plan, excludes, planner);
            } catch (final AffinityConflictException e2) {
                s_logger.warn("Unable to create deployment, affinity rules associted to the VM conflict", e2);
                throw new CloudRuntimeException("Unable to create deployment, affinity rules associted to the VM conflict");
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
            try {
                migrate(vm, srcHostId, dest);
                return;
            } catch (final ResourceUnavailableException e) {
                s_logger.debug("Unable to migrate to unavailable " + dest);
            } catch (final ConcurrentOperationException e) {
                s_logger.debug("Unable to migrate VM due to: " + e.getMessage());
            }

            try {
                advanceStop(vmUuid, true);
                throw new CloudRuntimeException("Unable to migrate " + vm);
            } catch (final ResourceUnavailableException e) {
                s_logger.debug("Unable to stop VM due to " + e.getMessage());
                throw new CloudRuntimeException("Unable to migrate " + vm);
            } catch (final ConcurrentOperationException e) {
                s_logger.debug("Unable to stop VM due to " + e.getMessage());
                throw new CloudRuntimeException("Unable to migrate " + vm);
            } catch (final OperationTimedoutException e) {
                s_logger.debug("Unable to stop VM due to " + e.getMessage());
                throw new CloudRuntimeException("Unable to migrate " + vm);
            }
        }
    }

    protected class CleanupTask extends ManagedContextRunnable {
        @Override
        protected void runInContext() {
            s_logger.trace("VM Operation Thread Running");
            try {
                _workDao.cleanup(VmOpCleanupWait.value());
                final Date cutDate = new Date(DateUtil.currentGMTTime().getTime() - VmOpCleanupInterval.value() * 1000);
                _workJobDao.expungeCompletedWorkJobs(cutDate);
            } catch (final Exception e) {
                s_logger.error("VM Operations failed due to ", e);
            }
        }
    }

    @Override
    public boolean isVirtualMachineUpgradable(final VirtualMachine vm, final ServiceOffering offering) {
        boolean isMachineUpgradable = true;
        for (final HostAllocator allocator : hostAllocators) {
            isMachineUpgradable = allocator.isVirtualMachineUpgradable(vm, offering);
            if (isMachineUpgradable) {
                continue;
            } else {
                break;
            }
        }

        return isMachineUpgradable;
    }

    @Override
    public void reboot(final String vmUuid, final Map<VirtualMachineProfile.Param, Object> params) throws InsufficientCapacityException, ResourceUnavailableException {
        try {
            advanceReboot(vmUuid, params);
        } catch (final ConcurrentOperationException e) {
            throw new CloudRuntimeException("Unable to reboot a VM due to concurrent operation", e);
        }
    }

    @Override
    public void advanceReboot(final String vmUuid, final Map<VirtualMachineProfile.Param, Object> params)
            throws InsufficientCapacityException, ConcurrentOperationException, ResourceUnavailableException {

        final AsyncJobExecutionContext jobContext = AsyncJobExecutionContext.getCurrentExecutionContext();
        if ( jobContext.isJobDispatchedBy(VmWorkConstants.VM_WORK_JOB_DISPATCHER)) {
            // avoid re-entrance
            VmWorkJobVO placeHolder = null;
            final VirtualMachine vm = _vmDao.findByUuid(vmUuid);
            placeHolder = createPlaceHolderWork(vm.getId());
            try {
                orchestrateReboot(vmUuid, params);
            } finally {
                if (placeHolder != null) {
                    _workJobDao.expunge(placeHolder.getId());
                }
            }
        } else {
            final Outcome<VirtualMachine> outcome = rebootVmThroughJobQueue(vmUuid, params);

            try {
                final VirtualMachine vm = outcome.get();
            } catch (final InterruptedException e) {
                throw new RuntimeException("Operation is interrupted", e);
            } catch (final java.util.concurrent.ExecutionException e) {
                throw new RuntimeException("Execution excetion", e);
            }

            final Object jobResult = _jobMgr.unmarshallResultObject(outcome.getJob());
            if (jobResult != null) {
                if (jobResult instanceof ResourceUnavailableException) {
                    throw (ResourceUnavailableException)jobResult;
                } else if (jobResult instanceof ConcurrentOperationException) {
                    throw (ConcurrentOperationException)jobResult;
                } else if (jobResult instanceof InsufficientCapacityException) {
                    throw (InsufficientCapacityException)jobResult;
                } else if (jobResult instanceof RuntimeException) {
                    throw (RuntimeException)jobResult;
                } else if (jobResult instanceof Throwable) {
                    throw new RuntimeException("Unexpected exception", (Throwable)jobResult);
                }
            }
        }
    }

    private void orchestrateReboot(final String vmUuid, final Map<VirtualMachineProfile.Param, Object> params) throws InsufficientCapacityException, ConcurrentOperationException,
    ResourceUnavailableException {
        final VMInstanceVO vm = _vmDao.findByUuid(vmUuid);
        // if there are active vm snapshots task, state change is not allowed
        if(_vmSnapshotMgr.hasActiveVMSnapshotTasks(vm.getId())){
            s_logger.error("Unable to reboot VM " + vm + " due to: " + vm.getInstanceName() + " has active VM snapshots tasks");
            throw new CloudRuntimeException("Unable to reboot VM " + vm + " due to: " + vm.getInstanceName() + " has active VM snapshots tasks");
        }
        final DataCenter dc = _entityMgr.findById(DataCenter.class, vm.getDataCenterId());
        final Host host = _hostDao.findById(vm.getHostId());
        if (host == null) {
            // Should findById throw an Exception is the host is not found?
            throw new CloudRuntimeException("Unable to retrieve host with id " + vm.getHostId());
        }
        final Cluster cluster = _entityMgr.findById(Cluster.class, host.getClusterId());
        final Pod pod = _entityMgr.findById(Pod.class, host.getPodId());
        final DeployDestination dest = new DeployDestination(dc, pod, cluster, host);

        try {

            final Commands cmds = new Commands(Command.OnError.Stop);
            cmds.addCommand(new RebootCommand(vm.getInstanceName(), getExecuteInSequence(vm.getHypervisorType())));
            _agentMgr.send(host.getId(), cmds);

            final Answer rebootAnswer = cmds.getAnswer(RebootAnswer.class);
            if (rebootAnswer != null && rebootAnswer.getResult()) {
                return;
            }
            s_logger.info("Unable to reboot VM " + vm + " on " + dest.getHost() + " due to " + (rebootAnswer == null ? " no reboot answer" : rebootAnswer.getDetails()));
        } catch (final OperationTimedoutException e) {
            s_logger.warn("Unable to send the reboot command to host " + dest.getHost() + " for the vm " + vm + " due to operation timeout", e);
            throw new CloudRuntimeException("Failed to reboot the vm on host " + dest.getHost());
        }
    }

    public Command cleanup(final VirtualMachine vm, Map<String, DpdkTO> dpdkInterfaceMapping) {
        StopCommand cmd = new StopCommand(vm, getExecuteInSequence(vm.getHypervisorType()), false);
        cmd.setControlIp(getControlNicIpForVM(vm));
        if (MapUtils.isNotEmpty(dpdkInterfaceMapping)) {
            cmd.setDpdkInterfaceMapping(dpdkInterfaceMapping);
        }
        return cmd;
    }

    private String getControlNicIpForVM(VirtualMachine vm) {
        if (vm.getType() == VirtualMachine.Type.ConsoleProxy || vm.getType() == VirtualMachine.Type.SecondaryStorageVm) {
            NicVO nic = _nicsDao.getControlNicForVM(vm.getId());
            return nic.getIPv4Address();
        } else if (vm.getType() == VirtualMachine.Type.DomainRouter) {
            return vm.getPrivateIpAddress();
        } else {
            return null;
        }
    }
    public Command cleanup(final String vmName) {
        VirtualMachine vm = _vmDao.findVMByInstanceName(vmName);

        StopCommand cmd = new StopCommand(vmName, getExecuteInSequence(null), false);
        cmd.setControlIp(getControlNicIpForVM(vm));
        return cmd;
    }


    // this is XenServer specific
    public void syncVMMetaData(final Map<String, String> vmMetadatum) {
        if (vmMetadatum == null || vmMetadatum.isEmpty()) {
            return;
        }
        List<Pair<Pair<String, VirtualMachine.Type>, Pair<Long, String>>> vmDetails = _userVmDao.getVmsDetailByNames(vmMetadatum.keySet(), "platform");
        for (final Map.Entry<String, String> entry : vmMetadatum.entrySet()) {
            final String name = entry.getKey();
            final String platform = entry.getValue();
            if (platform == null || platform.isEmpty()) {
                continue;
            }

            boolean found = false;
            for(Pair<Pair<String, VirtualMachine.Type>, Pair<Long, String>> vmDetail : vmDetails ) {
                Pair<String, VirtualMachine.Type> vmNameTypePair = vmDetail.first();
                if(vmNameTypePair.first().equals(name)) {
                    found = true;
                    if(vmNameTypePair.second() == VirtualMachine.Type.User) {
                        Pair<Long, String> detailPair = vmDetail.second();
                        String platformDetail = detailPair.second();

                        if (platformDetail != null && platformDetail.equals(platform)) {
                            break;
                        }
                        updateVmMetaData(detailPair.first(), platform);
                    }
                    break;
                }
            }

            if(!found) {
                VMInstanceVO vm = _vmDao.findVMByInstanceName(name);
                if(vm != null && vm.getType() == VirtualMachine.Type.User) {
                    updateVmMetaData(vm.getId(), platform);
                }
            }
        }
    }

    // this is XenServer specific
    private void updateVmMetaData(Long vmId, String platform) {
        UserVmVO userVm = _userVmDao.findById(vmId);
        _userVmDao.loadDetails(userVm);
        if ( userVm.details.containsKey(VmDetailConstants.TIME_OFFSET)) {
            userVm.details.remove(VmDetailConstants.TIME_OFFSET);
        }
        userVm.setDetail(VmDetailConstants.PLATFORM,  platform);
        String pvdriver = "xenserver56";
        if ( platform.contains("device_id")) {
            pvdriver = "xenserver61";
        }
        if (!userVm.details.containsKey(VmDetailConstants.HYPERVISOR_TOOLS_VERSION) || !userVm.details.get(VmDetailConstants.HYPERVISOR_TOOLS_VERSION).equals(pvdriver)) {
            userVm.setDetail(VmDetailConstants.HYPERVISOR_TOOLS_VERSION, pvdriver);
        }
        _userVmDao.saveDetails(userVm);
    }

    @Override
    public boolean isRecurring() {
        return true;
    }

    @Override
    public boolean processAnswers(final long agentId, final long seq, final Answer[] answers) {
        for (final Answer answer : answers) {
            if ( answer instanceof ClusterVMMetaDataSyncAnswer) {
                final ClusterVMMetaDataSyncAnswer cvms = (ClusterVMMetaDataSyncAnswer)answer;
                if (!cvms.isExecuted()) {
                    syncVMMetaData(cvms.getVMMetaDatum());
                    cvms.setExecuted();
                }
            }
        }
        return true;
    }

    @Override
    public boolean processTimeout(final long agentId, final long seq) {
        return true;
    }

    @Override
    public int getTimeout() {
        return -1;
    }

    @Override
    public boolean processCommands(final long agentId, final long seq, final Command[] cmds) {
        boolean processed = false;
        for (final Command cmd : cmds) {
            if (cmd instanceof PingRoutingCommand) {
                final PingRoutingCommand ping = (PingRoutingCommand)cmd;
                if (ping.getHostVmStateReport() != null) {
                    _syncMgr.processHostVmStatePingReport(agentId, ping.getHostVmStateReport());
                }

                // take the chance to scan VMs that are stuck in transitional states
                // and are missing from the report
                scanStalledVMInTransitionStateOnUpHost(agentId);
                processed = true;
            }
        }
        return processed;
    }

    @Override
    public AgentControlAnswer processControlCommand(final long agentId, final AgentControlCommand cmd) {
        return null;
    }

    @Override
    public boolean processDisconnect(final long agentId, final Status state) {
        return true;
    }

    @Override
    public void processHostAboutToBeRemoved(long hostId) {
    }

    @Override
    public void processHostRemoved(long hostId, long clusterId) {
    }

    @Override
    public void processHostAdded(long hostId) {
    }

    @Override
    public void processConnect(final Host agent, final StartupCommand cmd, final boolean forRebalance) throws ConnectionException {
        if (!(cmd instanceof StartupRoutingCommand)) {
            return;
        }

        if(s_logger.isDebugEnabled()) {
            s_logger.debug("Received startup command from hypervisor host. host id: " + agent.getId());
        }

        _syncMgr.resetHostSyncState(agent.getId());

        if (forRebalance) {
            s_logger.debug("Not processing listener " + this + " as connect happens on rebalance process");
            return;
        }
        final Long clusterId = agent.getClusterId();
        final long agentId = agent.getId();

        if (agent.getHypervisorType() == HypervisorType.XenServer) { // only for Xen
            // initiate the cron job
            final ClusterVMMetaDataSyncCommand syncVMMetaDataCmd = new ClusterVMMetaDataSyncCommand(ClusterVMMetaDataSyncInterval.value(), clusterId);
            try {
                final long seq_no = _agentMgr.send(agentId, new Commands(syncVMMetaDataCmd), this);
                s_logger.debug("Cluster VM metadata sync started with jobid " + seq_no);
            } catch (final AgentUnavailableException e) {
                s_logger.fatal("The Cluster VM metadata sync process failed for cluster id " + clusterId + " with ", e);
            }
        }
    }

    protected class TransitionTask extends ManagedContextRunnable {
        @Override
        protected void runInContext() {
            final GlobalLock lock = GlobalLock.getInternLock("TransitionChecking");
            if (lock == null) {
                s_logger.debug("Couldn't get the global lock");
                return;
            }

            if (!lock.lock(30)) {
                s_logger.debug("Couldn't lock the db");
                return;
            }
            try {
                scanStalledVMInTransitionStateOnDisconnectedHosts();

                final List<VMInstanceVO> instances = _vmDao.findVMInTransition(new Date(DateUtil.currentGMTTime().getTime() - AgentManager.Wait.value() * 1000), State.Starting, State.Stopping);
                for (final VMInstanceVO instance : instances) {
                    final State state = instance.getState();
                    if (state == State.Stopping) {
                        _haMgr.scheduleStop(instance, instance.getHostId(), WorkType.CheckStop);
                    } else if (state == State.Starting) {
                        _haMgr.scheduleRestart(instance, true);
                    }
                }
            } catch (final Exception e) {
                s_logger.warn("Caught the following exception on transition checking", e);
            } finally {
                lock.unlock();
            }
        }
    }

    @Override
    public VMInstanceVO findById(final long vmId) {
        return _vmDao.findById(vmId);
    }

    @Override
    public void checkIfCanUpgrade(final VirtualMachine vmInstance, final ServiceOffering newServiceOffering) {
        if (newServiceOffering == null) {
            throw new InvalidParameterValueException("Invalid parameter, newServiceOffering can't be null");
        }

        // Check that the VM is stopped / running
        if (!(vmInstance.getState().equals(State.Stopped) || vmInstance.getState().equals(State.Running))) {
            s_logger.warn("Unable to upgrade virtual machine " + vmInstance.toString() + " in state " + vmInstance.getState());
            throw new InvalidParameterValueException("Unable to upgrade virtual machine " + vmInstance.toString() + " " + " in state " + vmInstance.getState() +
                    "; make sure the virtual machine is stopped/running");
        }

        // Check if the service offering being upgraded to is what the VM is already running with
        if (!newServiceOffering.isDynamic() && vmInstance.getServiceOfferingId() == newServiceOffering.getId()) {
            if (s_logger.isInfoEnabled()) {
                s_logger.info("Not upgrading vm " + vmInstance.toString() + " since it already has the requested " + "service offering (" + newServiceOffering.getName() +
                        ")");
            }

            throw new InvalidParameterValueException("Not upgrading vm " + vmInstance.toString() + " since it already " + "has the requested service offering (" +
                    newServiceOffering.getName() + ")");
        }

        final ServiceOfferingVO currentServiceOffering = _offeringDao.findByIdIncludingRemoved(vmInstance.getId(), vmInstance.getServiceOfferingId());

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
        if (currentServiceOffering.isUseLocalStorage() != newServiceOffering.isUseLocalStorage()) {
            throw new InvalidParameterValueException("Unable to upgrade virtual machine " + vmInstance.toString() +
                    ", cannot switch between local storage and shared storage service offerings.  Current offering " + "useLocalStorage=" +
                    currentServiceOffering.isUseLocalStorage() + ", target offering useLocalStorage=" + newServiceOffering.isUseLocalStorage());
        }

        // if vm is a system vm, check if it is a system service offering, if yes return with error as it cannot be used for user vms
        if (currentServiceOffering.isSystemUse() != newServiceOffering.isSystemUse()) {
            throw new InvalidParameterValueException("isSystem property is different for current service offering and new service offering");
        }

        // Check that there are enough resources to upgrade the service offering
        if (!isVirtualMachineUpgradable(vmInstance, newServiceOffering)) {
            throw new InvalidParameterValueException("Unable to upgrade virtual machine, not enough resources available " + "for an offering of " +
                    newServiceOffering.getCpu() + " cpu(s) at " + newServiceOffering.getSpeed() + " Mhz, and " + newServiceOffering.getRamSize() + " MB of memory");
        }

        // Check that the service offering being upgraded to has all the tags of the current service offering.
        final List<String> currentTags = StringUtils.csvTagsToList(currentServiceOffering.getTags());
        final List<String> newTags = StringUtils.csvTagsToList(newServiceOffering.getTags());
        if (!newTags.containsAll(currentTags)) {
            throw new InvalidParameterValueException("Unable to upgrade virtual machine; the current service offering " + " should have tags as subset of " +
                    "the new service offering tags. Current service offering tags: " + currentTags + "; " + "new service " + "offering tags: " + newTags);
        }
    }

    @Override
    public boolean upgradeVmDb(final long vmId, final long serviceOfferingId) {
        final VMInstanceVO vmForUpdate = _vmDao.createForUpdate();
        vmForUpdate.setServiceOfferingId(serviceOfferingId);
        final ServiceOffering newSvcOff = _entityMgr.findById(ServiceOffering.class, serviceOfferingId);
        vmForUpdate.setHaEnabled(newSvcOff.isOfferHA());
        vmForUpdate.setLimitCpuUse(newSvcOff.getLimitCpuUse());
        vmForUpdate.setServiceOfferingId(newSvcOff.getId());
        return _vmDao.update(vmId, vmForUpdate);
    }

    @Override
    public NicProfile addVmToNetwork(final VirtualMachine vm, final Network network, final NicProfile requested)
            throws ConcurrentOperationException, ResourceUnavailableException, InsufficientCapacityException {

        final AsyncJobExecutionContext jobContext = AsyncJobExecutionContext.getCurrentExecutionContext();
        if (jobContext.isJobDispatchedBy(VmWorkConstants.VM_WORK_JOB_DISPATCHER)) {
            // avoid re-entrance
            VmWorkJobVO placeHolder = null;
            placeHolder = createPlaceHolderWork(vm.getId());
            try {
                return orchestrateAddVmToNetwork(vm, network, requested);
            } finally {
                if (placeHolder != null) {
                    _workJobDao.expunge(placeHolder.getId());
                }
            }
        } else {
            final Outcome<VirtualMachine> outcome = addVmToNetworkThroughJobQueue(vm, network, requested);

            try {
                outcome.get();
            } catch (final InterruptedException e) {
                throw new RuntimeException("Operation is interrupted", e);
            } catch (final java.util.concurrent.ExecutionException e) {
                throw new RuntimeException("Execution exception", e);
            }

            final Object jobException = _jobMgr.unmarshallResultObject(outcome.getJob());
            if (jobException != null) {
                if (jobException instanceof ResourceUnavailableException) {
                    throw (ResourceUnavailableException)jobException;
                } else if (jobException instanceof ConcurrentOperationException) {
                    throw (ConcurrentOperationException)jobException;
                } else if (jobException instanceof InsufficientCapacityException) {
                    throw (InsufficientCapacityException)jobException;
                } else if (jobException instanceof RuntimeException) {
                    throw (RuntimeException)jobException;
                } else if (jobException instanceof Throwable) {
                    throw new RuntimeException("Unexpected exception", (Throwable)jobException);
                } else if (jobException instanceof NicProfile) {
                    return (NicProfile)jobException;
                }
            }

            throw new RuntimeException("Unexpected job execution result");
        }
    }

    private NicProfile orchestrateAddVmToNetwork(final VirtualMachine vm, final Network network, final NicProfile requested) throws ConcurrentOperationException, ResourceUnavailableException,
    InsufficientCapacityException {
        final CallContext cctx = CallContext.current();

        s_logger.debug("Adding vm " + vm + " to network " + network + "; requested nic profile " + requested);
        final VMInstanceVO vmVO = _vmDao.findById(vm.getId());
        final ReservationContext context = new ReservationContextImpl(null, null, cctx.getCallingUser(), cctx.getCallingAccount());

        final VirtualMachineProfileImpl vmProfile = new VirtualMachineProfileImpl(vmVO, null, null, null, null);

        final DataCenter dc = _entityMgr.findById(DataCenter.class, network.getDataCenterId());
        final Host host = _hostDao.findById(vm.getHostId());
        final DeployDestination dest = new DeployDestination(dc, null, null, host);

        //check vm state
        if (vm.getState() == State.Running) {
            //1) allocate and prepare nic
            final NicProfile nic = _networkMgr.createNicForVm(network, requested, context, vmProfile, true);

            //2) Convert vmProfile to vmTO
            final HypervisorGuru hvGuru = _hvGuruMgr.getGuru(vmProfile.getVirtualMachine().getHypervisorType());
            final VirtualMachineTO vmTO = hvGuru.implement(vmProfile);

            //3) Convert nicProfile to NicTO
            final NicTO nicTO = toNicTO(nic, vmProfile.getVirtualMachine().getHypervisorType());

            if (network != null) {
                final Map<NetworkOffering.Detail, String> details = networkOfferingDetailsDao.getNtwkOffDetails(network.getNetworkOfferingId());
                if (details != null) {
                    details.putIfAbsent(NetworkOffering.Detail.PromiscuousMode, NetworkOrchestrationService.PromiscuousMode.value().toString());
                    details.putIfAbsent(NetworkOffering.Detail.MacAddressChanges, NetworkOrchestrationService.MacAddressChanges.value().toString());
                    details.putIfAbsent(NetworkOffering.Detail.ForgedTransmits, NetworkOrchestrationService.ForgedTransmits.value().toString());
                }
                nicTO.setDetails(details);
            }

            //4) plug the nic to the vm
            s_logger.debug("Plugging nic for vm " + vm + " in network " + network);

            boolean result = false;
            try {
                result = plugNic(network, nicTO, vmTO, context, dest);
                if (result) {
                    s_logger.debug("Nic is plugged successfully for vm " + vm + " in network " + network + ". Vm  is a part of network now");
                    final long isDefault = nic.isDefaultNic() ? 1 : 0;
                    // insert nic's Id into DB as resource_name
                    if(VirtualMachine.Type.User.equals(vmVO.getType())) {
                        //Log usage event for user Vms only
                        UsageEventUtils.publishUsageEvent(EventTypes.EVENT_NETWORK_OFFERING_ASSIGN, vmVO.getAccountId(), vmVO.getDataCenterId(), vmVO.getId(),
                                Long.toString(nic.getId()), network.getNetworkOfferingId(), null, isDefault, VirtualMachine.class.getName(), vmVO.getUuid(), vm.isDisplay());
                    }
                    return nic;
                } else {
                    s_logger.warn("Failed to plug nic to the vm " + vm + " in network " + network);
                    return null;
                }
            } finally {
                if (!result) {
                    s_logger.debug("Removing nic " + nic + " from vm " + vmProfile.getVirtualMachine() + " as nic plug failed on the backend");
                    _networkMgr.removeNic(vmProfile, _nicsDao.findById(nic.getId()));
                }
            }
        } else if (vm.getState() == State.Stopped) {
            //1) allocate nic
            return _networkMgr.createNicForVm(network, requested, context, vmProfile, false);
        } else {
            s_logger.warn("Unable to add vm " + vm + " to network  " + network);
            throw new ResourceUnavailableException("Unable to add vm " + vm + " to network, is not in the right state", DataCenter.class, vm.getDataCenterId());
        }
    }

    @Override
    public NicTO toNicTO(final NicProfile nic, final HypervisorType hypervisorType) {
        final HypervisorGuru hvGuru = _hvGuruMgr.getGuru(hypervisorType);

        final NicTO nicTO = hvGuru.toNicTO(nic);
        return nicTO;
    }

    @Override
    public boolean removeNicFromVm(final VirtualMachine vm, final Nic nic)
            throws ConcurrentOperationException, ResourceUnavailableException {

        final AsyncJobExecutionContext jobContext = AsyncJobExecutionContext.getCurrentExecutionContext();
        if (jobContext.isJobDispatchedBy(VmWorkConstants.VM_WORK_JOB_DISPATCHER)) {
            // avoid re-entrance
            VmWorkJobVO placeHolder = null;
            placeHolder = createPlaceHolderWork(vm.getId());
            try {
                return orchestrateRemoveNicFromVm(vm, nic);
            } finally {
                if (placeHolder != null) {
                    _workJobDao.expunge(placeHolder.getId());
                }
            }

        } else {
            final Outcome<VirtualMachine> outcome = removeNicFromVmThroughJobQueue(vm, nic);

            try {
                outcome.get();
            } catch (final InterruptedException e) {
                throw new RuntimeException("Operation is interrupted", e);
            } catch (final java.util.concurrent.ExecutionException e) {
                throw new RuntimeException("Execution excetion", e);
            }

            final Object jobResult = _jobMgr.unmarshallResultObject(outcome.getJob());
            if (jobResult != null) {
                if (jobResult instanceof ResourceUnavailableException) {
                    throw (ResourceUnavailableException)jobResult;
                } else if (jobResult instanceof ConcurrentOperationException) {
                    throw (ConcurrentOperationException)jobResult;
                } else if (jobResult instanceof RuntimeException) {
                    throw (RuntimeException)jobResult;
                } else if (jobResult instanceof Throwable) {
                    throw new RuntimeException("Unexpected exception", (Throwable)jobResult);
                } else if (jobResult instanceof Boolean) {
                    return (Boolean)jobResult;
                }
            }

            throw new RuntimeException("Job failed with un-handled exception");
        }
    }

    private boolean orchestrateRemoveNicFromVm(final VirtualMachine vm, final Nic nic) throws ConcurrentOperationException, ResourceUnavailableException {
        final CallContext cctx = CallContext.current();
        final VMInstanceVO vmVO = _vmDao.findById(vm.getId());
        final NetworkVO network = _networkDao.findById(nic.getNetworkId());
        final ReservationContext context = new ReservationContextImpl(null, null, cctx.getCallingUser(), cctx.getCallingAccount());

        final VirtualMachineProfileImpl vmProfile = new VirtualMachineProfileImpl(vmVO, null, null, null, null);

        final DataCenter dc = _entityMgr.findById(DataCenter.class, network.getDataCenterId());
        final Host host = _hostDao.findById(vm.getHostId());
        final DeployDestination dest = new DeployDestination(dc, null, null, host);
        final HypervisorGuru hvGuru = _hvGuruMgr.getGuru(vmProfile.getVirtualMachine().getHypervisorType());
        final VirtualMachineTO vmTO = hvGuru.implement(vmProfile);

        final NicProfile nicProfile =
                new NicProfile(nic, network, nic.getBroadcastUri(), nic.getIsolationUri(), _networkModel.getNetworkRate(network.getId(), vm.getId()),
                        _networkModel.isSecurityGroupSupportedInNetwork(network), _networkModel.getNetworkTag(vmProfile.getVirtualMachine().getHypervisorType(), network));

        //1) Unplug the nic
        if (vm.getState() == State.Running) {
            final NicTO nicTO = toNicTO(nicProfile, vmProfile.getVirtualMachine().getHypervisorType());
            s_logger.debug("Un-plugging nic " + nic + " for vm " + vm + " from network " + network);
            final boolean result = unplugNic(network, nicTO, vmTO, context, dest);
            if (result) {
                s_logger.debug("Nic is unplugged successfully for vm " + vm + " in network " + network);
                final long isDefault = nic.isDefaultNic() ? 1 : 0;
                UsageEventUtils.publishUsageEvent(EventTypes.EVENT_NETWORK_OFFERING_REMOVE, vm.getAccountId(), vm.getDataCenterId(), vm.getId(),
                        Long.toString(nic.getId()), network.getNetworkOfferingId(), null, isDefault, VirtualMachine.class.getName(), vm.getUuid(), vm.isDisplay());
            } else {
                s_logger.warn("Failed to unplug nic for the vm " + vm + " from network " + network);
                return false;
            }
        } else if (vm.getState() != State.Stopped) {
            s_logger.warn("Unable to remove vm " + vm + " from network  " + network);
            throw new ResourceUnavailableException("Unable to remove vm " + vm + " from network, is not in the right state", DataCenter.class, vm.getDataCenterId());
        }

        //2) Release the nic
        _networkMgr.releaseNic(vmProfile, nic);
        s_logger.debug("Successfully released nic " + nic + "for vm " + vm);

        //3) Remove the nic
        _networkMgr.removeNic(vmProfile, nic);
        _nicsDao.expunge(nic.getId());
        return true;
    }

    @Override
    @DB
    public boolean removeVmFromNetwork(final VirtualMachine vm, final Network network, final URI broadcastUri) throws ConcurrentOperationException, ResourceUnavailableException {
        // TODO will serialize on the VM object later to resolve operation conflicts
        return orchestrateRemoveVmFromNetwork(vm, network, broadcastUri);
    }

    @DB
    private boolean orchestrateRemoveVmFromNetwork(final VirtualMachine vm, final Network network, final URI broadcastUri) throws ConcurrentOperationException, ResourceUnavailableException {
        final CallContext cctx = CallContext.current();
        final VMInstanceVO vmVO = _vmDao.findById(vm.getId());
        final ReservationContext context = new ReservationContextImpl(null, null, cctx.getCallingUser(), cctx.getCallingAccount());

        final VirtualMachineProfileImpl vmProfile = new VirtualMachineProfileImpl(vmVO, null, null, null, null);

        final DataCenter dc = _entityMgr.findById(DataCenter.class, network.getDataCenterId());
        final Host host = _hostDao.findById(vm.getHostId());
        final DeployDestination dest = new DeployDestination(dc, null, null, host);
        final HypervisorGuru hvGuru = _hvGuruMgr.getGuru(vmProfile.getVirtualMachine().getHypervisorType());
        final VirtualMachineTO vmTO = hvGuru.implement(vmProfile);

        Nic nic = null;
        if (broadcastUri != null) {
            nic = _nicsDao.findByNetworkIdInstanceIdAndBroadcastUri(network.getId(), vm.getId(), broadcastUri.toString());
        } else {
            nic = _networkModel.getNicInNetwork(vm.getId(), network.getId());
        }

        if (nic == null) {
            s_logger.warn("Could not get a nic with " + network);
            return false;
        }

        // don't delete default NIC on a user VM
        if (nic.isDefaultNic() && vm.getType() == VirtualMachine.Type.User) {
            s_logger.warn("Failed to remove nic from " + vm + " in " + network + ", nic is default.");
            throw new CloudRuntimeException("Failed to remove nic from " + vm + " in " + network + ", nic is default.");
        }

        //Lock on nic is needed here
        final Nic lock = _nicsDao.acquireInLockTable(nic.getId());
        if (lock == null) {
            //check if nic is still there. Return if it was released already
            if (_nicsDao.findById(nic.getId()) == null) {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Not need to remove the vm " + vm + " from network " + network + " as the vm doesn't have nic in this network");
                }
                return true;
            }
            throw new ConcurrentOperationException("Unable to lock nic " + nic.getId());
        }

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Lock is acquired for nic id " + lock.getId() + " as a part of remove vm " + vm + " from network " + network);
        }

        try {
            final NicProfile nicProfile =
                    new NicProfile(nic, network, nic.getBroadcastUri(), nic.getIsolationUri(), _networkModel.getNetworkRate(network.getId(), vm.getId()),
                            _networkModel.isSecurityGroupSupportedInNetwork(network), _networkModel.getNetworkTag(vmProfile.getVirtualMachine().getHypervisorType(), network));

            //1) Unplug the nic
            if (vm.getState() == State.Running) {
                final NicTO nicTO = toNicTO(nicProfile, vmProfile.getVirtualMachine().getHypervisorType());
                s_logger.debug("Un-plugging nic for vm " + vm + " from network " + network);
                final boolean result = unplugNic(network, nicTO, vmTO, context, dest);
                if (result) {
                    s_logger.debug("Nic is unplugged successfully for vm " + vm + " in network " + network);
                } else {
                    s_logger.warn("Failed to unplug nic for the vm " + vm + " from network " + network);
                    return false;
                }
            } else if (vm.getState() != State.Stopped) {
                s_logger.warn("Unable to remove vm " + vm + " from network  " + network);
                throw new ResourceUnavailableException("Unable to remove vm " + vm + " from network, is not in the right state", DataCenter.class, vm.getDataCenterId());
            }

            //2) Release the nic
            _networkMgr.releaseNic(vmProfile, nic);
            s_logger.debug("Successfully released nic " + nic + "for vm " + vm);

            //3) Remove the nic
            _networkMgr.removeNic(vmProfile, nic);
            return true;
        } finally {
            if (lock != null) {
                _nicsDao.releaseFromLockTable(lock.getId());
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Lock is released for nic id " + lock.getId() + " as a part of remove vm " + vm + " from network " + network);
                }
            }
        }
    }

    @Override
    public void findHostAndMigrate(final String vmUuid, final Long newSvcOfferingId, final ExcludeList excludes) throws InsufficientCapacityException, ConcurrentOperationException,
    ResourceUnavailableException {

        final VMInstanceVO vm = _vmDao.findByUuid(vmUuid);
        if (vm == null) {
            throw new CloudRuntimeException("Unable to find " + vmUuid);
        }

        final VirtualMachineProfile profile = new VirtualMachineProfileImpl(vm);

        final Long srcHostId = vm.getHostId();
        final Long oldSvcOfferingId = vm.getServiceOfferingId();
        if (srcHostId == null) {
            throw new CloudRuntimeException("Unable to scale the vm because it doesn't have a host id");
        }
        final Host host = _hostDao.findById(srcHostId);
        final DataCenterDeployment plan = new DataCenterDeployment(host.getDataCenterId(), host.getPodId(), host.getClusterId(), null, null, null);
        excludes.addHost(vm.getHostId());
        vm.setServiceOfferingId(newSvcOfferingId); // Need to find the destination host based on new svc offering

        DeployDestination dest = null;

        try {
            dest = _dpMgr.planDeployment(profile, plan, excludes, null);
        } catch (final AffinityConflictException e2) {
            s_logger.warn("Unable to create deployment, affinity rules associted to the VM conflict", e2);
            throw new CloudRuntimeException("Unable to create deployment, affinity rules associted to the VM conflict");
        }

        if (dest != null) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug(" Found " + dest + " for scaling the vm to.");
            }
        }

        if (dest == null) {
            throw new InsufficientServerCapacityException("Unable to find a server to scale the vm to.", host.getClusterId());
        }

        excludes.addHost(dest.getHost().getId());
        try {
            migrateForScale(vm.getUuid(), srcHostId, dest, oldSvcOfferingId);
        } catch (final ResourceUnavailableException e) {
            s_logger.debug("Unable to migrate to unavailable " + dest);
            throw e;
        } catch (final ConcurrentOperationException e) {
            s_logger.debug("Unable to migrate VM due to: " + e.getMessage());
            throw e;
        }
    }

    @Override
    public void migrateForScale(final String vmUuid, final long srcHostId, final DeployDestination dest, final Long oldSvcOfferingId)
            throws ResourceUnavailableException, ConcurrentOperationException {
        final AsyncJobExecutionContext jobContext = AsyncJobExecutionContext.getCurrentExecutionContext();
        if (jobContext.isJobDispatchedBy(VmWorkConstants.VM_WORK_JOB_DISPATCHER)) {
            // avoid re-entrance
            VmWorkJobVO placeHolder = null;
            final VirtualMachine vm = _vmDao.findByUuid(vmUuid);
            placeHolder = createPlaceHolderWork(vm.getId());
            try {
                orchestrateMigrateForScale(vmUuid, srcHostId, dest, oldSvcOfferingId);
            } finally {
                if (placeHolder != null) {
                    _workJobDao.expunge(placeHolder.getId());
                }
            }
        } else {
            final Outcome<VirtualMachine> outcome = migrateVmForScaleThroughJobQueue(vmUuid, srcHostId, dest, oldSvcOfferingId);

            try {
                final VirtualMachine vm = outcome.get();
            } catch (final InterruptedException e) {
                throw new RuntimeException("Operation is interrupted", e);
            } catch (final java.util.concurrent.ExecutionException e) {
                throw new RuntimeException("Execution excetion", e);
            }

            final Object jobResult = _jobMgr.unmarshallResultObject(outcome.getJob());
            if (jobResult != null) {
                if (jobResult instanceof ResourceUnavailableException) {
                    throw (ResourceUnavailableException)jobResult;
                } else if (jobResult instanceof ConcurrentOperationException) {
                    throw (ConcurrentOperationException)jobResult;
                } else if (jobResult instanceof RuntimeException) {
                    throw (RuntimeException)jobResult;
                } else if (jobResult instanceof Throwable) {
                    throw new RuntimeException("Unexpected exception", (Throwable)jobResult);
                }
            }
        }
    }

    private void orchestrateMigrateForScale(final String vmUuid, final long srcHostId, final DeployDestination dest, final Long oldSvcOfferingId)
            throws ResourceUnavailableException, ConcurrentOperationException {

        VMInstanceVO vm = _vmDao.findByUuid(vmUuid);
        s_logger.info("Migrating " + vm + " to " + dest);

        vm.getServiceOfferingId();
        final long dstHostId = dest.getHost().getId();
        final Host fromHost = _hostDao.findById(srcHostId);
        if (fromHost == null) {
            s_logger.info("Unable to find the host to migrate from: " + srcHostId);
            throw new CloudRuntimeException("Unable to find the host to migrate from: " + srcHostId);
        }

        if (fromHost.getClusterId().longValue() != dest.getCluster().getId()) {
            s_logger.info("Source and destination host are not in same cluster, unable to migrate to host: " + dstHostId);
            throw new CloudRuntimeException("Source and destination host are not in same cluster, unable to migrate to host: " + dest.getHost().getId());
        }

        final VirtualMachineGuru vmGuru = getVmGuru(vm);

        final long vmId = vm.getId();
        vm = _vmDao.findByUuid(vmUuid);
        if (vm == null) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Unable to find the vm " + vm);
            }
            throw new CloudRuntimeException("Unable to find a virtual machine with id " + vmId);
        }

        if (vm.getState() != State.Running) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("VM is not Running, unable to migrate the vm " + vm);
            }
            throw new CloudRuntimeException("VM is not Running, unable to migrate the vm currently " + vm + " , current state: " + vm.getState().toString());
        }

        AlertManager.AlertType alertType = AlertManager.AlertType.ALERT_TYPE_USERVM_MIGRATE;
        if (VirtualMachine.Type.DomainRouter.equals(vm.getType())) {
            alertType = AlertManager.AlertType.ALERT_TYPE_DOMAIN_ROUTER_MIGRATE;
        } else if (VirtualMachine.Type.ConsoleProxy.equals(vm.getType())) {
            alertType = AlertManager.AlertType.ALERT_TYPE_CONSOLE_PROXY_MIGRATE;
        }

        final VirtualMachineProfile profile = new VirtualMachineProfileImpl(vm);
        _networkMgr.prepareNicForMigration(profile, dest);

        volumeMgr.prepareForMigration(profile, dest);

        final VirtualMachineTO to = toVmTO(profile);
        final PrepareForMigrationCommand pfmc = new PrepareForMigrationCommand(to);

        ItWorkVO work = new ItWorkVO(UUID.randomUUID().toString(), _nodeId, State.Migrating, vm.getType(), vm.getId());
        work.setStep(Step.Prepare);
        work.setResourceType(ItWorkVO.ResourceType.Host);
        work.setResourceId(dstHostId);
        work = _workDao.persist(work);

        Answer pfma = null;
        try {
            pfma = _agentMgr.send(dstHostId, pfmc);
            if (pfma == null || !pfma.getResult()) {
                final String details = pfma != null ? pfma.getDetails() : "null answer returned";
                final String msg = "Unable to prepare for migration due to " + details;
                pfma = null;
                throw new AgentUnavailableException(msg, dstHostId);
            }
        } catch (final OperationTimedoutException e1) {
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
        } catch (final NoTransitionException e1) {
            s_logger.info("Migration cancelled because " + e1.getMessage());
            throw new ConcurrentOperationException("Migration cancelled because " + e1.getMessage());
        }

        boolean migrated = false;
        try {
            final boolean isWindows = _guestOsCategoryDao.findById(_guestOsDao.findById(vm.getGuestOSId()).getCategoryId()).getName().equalsIgnoreCase("Windows");
            final MigrateCommand mc = new MigrateCommand(vm.getInstanceName(), dest.getHost().getPrivateIpAddress(), isWindows, to, getExecuteInSequence(vm.getHypervisorType()));

            boolean kvmAutoConvergence = StorageManager.KvmAutoConvergence.value();
            mc.setAutoConvergence(kvmAutoConvergence);
            mc.setHostGuid(dest.getHost().getGuid());

            try {
                final Answer ma = _agentMgr.send(vm.getLastHostId(), mc);
                if (ma == null || !ma.getResult()) {
                    final String details = ma != null ? ma.getDetails() : "null answer returned";
                    final String msg = "Unable to migrate due to " + details;
                    s_logger.error(msg);
                    throw new CloudRuntimeException(msg);
                }
            } catch (final OperationTimedoutException e) {
                if (e.isActive()) {
                    s_logger.warn("Active migration command so scheduling a restart for " + vm);
                    _haMgr.scheduleRestart(vm, true);
                }
                throw new AgentUnavailableException("Operation timed out on migrating " + vm, dstHostId);
            }

            try {
                final long newServiceOfferingId = vm.getServiceOfferingId();
                vm.setServiceOfferingId(oldSvcOfferingId); // release capacity for the old service offering only
                if (!changeState(vm, VirtualMachine.Event.OperationSucceeded, dstHostId, work, Step.Started)) {
                    throw new ConcurrentOperationException("Unable to change the state for " + vm);
                }
                vm.setServiceOfferingId(newServiceOfferingId);
            } catch (final NoTransitionException e1) {
                throw new ConcurrentOperationException("Unable to change state due to " + e1.getMessage());
            }

            try {
                if (!checkVmOnHost(vm, dstHostId)) {
                    s_logger.error("Unable to complete migration for " + vm);
                    try {
                        _agentMgr.send(srcHostId, new Commands(cleanup(vm.getInstanceName())), null);
                    } catch (final AgentUnavailableException e) {
                        s_logger.error("AgentUnavailableException while cleanup on source host: " + srcHostId);
                    }
                    cleanup(vmGuru, new VirtualMachineProfileImpl(vm), work, Event.AgentReportStopped, true);
                    throw new CloudRuntimeException("Unable to complete migration for " + vm);
                }
            } catch (final OperationTimedoutException e) {
                s_logger.debug("Error while checking the vm " + vm + " on host " + dstHostId, e);
            }

            migrated = true;
        } finally {
            if (!migrated) {
                s_logger.info("Migration was unsuccessful.  Cleaning up: " + vm);

                _alertMgr.sendAlert(alertType, fromHost.getDataCenterId(), fromHost.getPodId(),
                        "Unable to migrate vm " + vm.getInstanceName() + " from host " + fromHost.getName() + " in zone " + dest.getDataCenter().getName() + " and pod " +
                                dest.getPod().getName(), "Migrate Command failed.  Please check logs.");
                try {
                    _agentMgr.send(dstHostId, new Commands(cleanup(vm.getInstanceName())), null);
                } catch (final AgentUnavailableException ae) {
                    s_logger.info("Looks like the destination Host is unavailable for cleanup");
                }

                try {
                    stateTransitTo(vm, Event.OperationFailed, srcHostId);
                } catch (final NoTransitionException e) {
                    s_logger.warn(e.getMessage());
                }
            }

            work.setStep(Step.Done);
            _workDao.update(work.getId(), work);
        }
    }

    @Override
    public boolean replugNic(final Network network, final NicTO nic, final VirtualMachineTO vm, final ReservationContext context, final DeployDestination dest) throws ConcurrentOperationException,
    ResourceUnavailableException, InsufficientCapacityException {
        boolean result = true;

        final VMInstanceVO router = _vmDao.findById(vm.getId());
        if (router.getState() == State.Running) {
            try {
                final ReplugNicCommand replugNicCmd = new ReplugNicCommand(nic, vm.getName(), vm.getType(), vm.getDetails());
                final Commands cmds = new Commands(Command.OnError.Stop);
                cmds.addCommand("replugnic", replugNicCmd);
                _agentMgr.send(dest.getHost().getId(), cmds);
                final ReplugNicAnswer replugNicAnswer = cmds.getAnswer(ReplugNicAnswer.class);
                if (replugNicAnswer == null || !replugNicAnswer.getResult()) {
                    s_logger.warn("Unable to replug nic for vm " + vm.getName());
                    result = false;
                }
            } catch (final OperationTimedoutException e) {
                throw new AgentUnavailableException("Unable to plug nic for router " + vm.getName() + " in network " + network, dest.getHost().getId(), e);
            }
        } else {
            s_logger.warn("Unable to apply ReplugNic, vm " + router + " is not in the right state " + router.getState());

            throw new ResourceUnavailableException("Unable to apply ReplugNic on the backend," + " vm " + vm + " is not in the right state", DataCenter.class,
                    router.getDataCenterId());
        }

        return result;
    }

    public boolean plugNic(final Network network, final NicTO nic, final VirtualMachineTO vm, final ReservationContext context, final DeployDestination dest) throws ConcurrentOperationException,
    ResourceUnavailableException, InsufficientCapacityException {
        boolean result = true;

        final VMInstanceVO router = _vmDao.findById(vm.getId());
        if (router.getState() == State.Running) {
            try {
                final PlugNicCommand plugNicCmd = new PlugNicCommand(nic, vm.getName(), vm.getType(), vm.getDetails());
                final Commands cmds = new Commands(Command.OnError.Stop);
                cmds.addCommand("plugnic", plugNicCmd);
                _agentMgr.send(dest.getHost().getId(), cmds);
                final PlugNicAnswer plugNicAnswer = cmds.getAnswer(PlugNicAnswer.class);
                if (plugNicAnswer == null || !plugNicAnswer.getResult()) {
                    s_logger.warn("Unable to plug nic for vm " + vm.getName());
                    result = false;
                }
            } catch (final OperationTimedoutException e) {
                throw new AgentUnavailableException("Unable to plug nic for router " + vm.getName() + " in network " + network, dest.getHost().getId(), e);
            }
        } else {
            s_logger.warn("Unable to apply PlugNic, vm " + router + " is not in the right state " + router.getState());

            throw new ResourceUnavailableException("Unable to apply PlugNic on the backend," + " vm " + vm + " is not in the right state", DataCenter.class,
                    router.getDataCenterId());
        }

        return result;
    }

    public boolean unplugNic(final Network network, final NicTO nic, final VirtualMachineTO vm, final ReservationContext context, final DeployDestination dest) throws ConcurrentOperationException,
    ResourceUnavailableException {

        boolean result = true;
        final VMInstanceVO router = _vmDao.findById(vm.getId());

        if (router.getState() == State.Running) {
            // collect vm network statistics before unplug a nic
            UserVmVO userVm = _userVmDao.findById(vm.getId());
            if (userVm != null && userVm.getType() == VirtualMachine.Type.User) {
                _userVmService.collectVmNetworkStatistics(userVm);
            }
            try {
                final Commands cmds = new Commands(Command.OnError.Stop);
                final UnPlugNicCommand unplugNicCmd = new UnPlugNicCommand(nic, vm.getName());
                cmds.addCommand("unplugnic", unplugNicCmd);
                _agentMgr.send(dest.getHost().getId(), cmds);

                final UnPlugNicAnswer unplugNicAnswer = cmds.getAnswer(UnPlugNicAnswer.class);
                if (unplugNicAnswer == null || !unplugNicAnswer.getResult()) {
                    s_logger.warn("Unable to unplug nic from router " + router);
                    result = false;
                }
            } catch (final OperationTimedoutException e) {
                throw new AgentUnavailableException("Unable to unplug nic from rotuer " + router + " from network " + network, dest.getHost().getId(), e);
            }
        } else if (router.getState() == State.Stopped || router.getState() == State.Stopping) {
            s_logger.debug("Vm " + router.getInstanceName() + " is in " + router.getState() + ", so not sending unplug nic command to the backend");
        } else {
            s_logger.warn("Unable to apply unplug nic, Vm " + router + " is not in the right state " + router.getState());

            throw new ResourceUnavailableException("Unable to apply unplug nic on the backend," + " vm " + router + " is not in the right state", DataCenter.class,
                    router.getDataCenterId());
        }

        return result;
    }

    @Override
    public VMInstanceVO reConfigureVm(final String vmUuid, final ServiceOffering oldServiceOffering,
            final boolean reconfiguringOnExistingHost)
                    throws ResourceUnavailableException, InsufficientServerCapacityException, ConcurrentOperationException {

        final AsyncJobExecutionContext jobContext = AsyncJobExecutionContext.getCurrentExecutionContext();
        if (jobContext.isJobDispatchedBy(VmWorkConstants.VM_WORK_JOB_DISPATCHER)) {
            // avoid re-entrance
            VmWorkJobVO placeHolder = null;
            final VirtualMachine vm = _vmDao.findByUuid(vmUuid);
            placeHolder = createPlaceHolderWork(vm.getId());
            try {
                return orchestrateReConfigureVm(vmUuid, oldServiceOffering, reconfiguringOnExistingHost);
            } finally {
                if (placeHolder != null) {
                    _workJobDao.expunge(placeHolder.getId());
                }
            }
        } else {
            final Outcome<VirtualMachine> outcome = reconfigureVmThroughJobQueue(vmUuid, oldServiceOffering, reconfiguringOnExistingHost);

            VirtualMachine vm = null;
            try {
                vm = outcome.get();
            } catch (final InterruptedException e) {
                throw new RuntimeException("Operation is interrupted", e);
            } catch (final java.util.concurrent.ExecutionException e) {
                throw new RuntimeException("Execution excetion", e);
            }

            final Object jobResult = _jobMgr.unmarshallResultObject(outcome.getJob());
            if (jobResult != null) {
                if (jobResult instanceof ResourceUnavailableException) {
                    throw (ResourceUnavailableException)jobResult;
                } else if (jobResult instanceof ConcurrentOperationException) {
                    throw (ConcurrentOperationException)jobResult;
                } else if (jobResult instanceof InsufficientServerCapacityException) {
                    throw (InsufficientServerCapacityException)jobResult;
                } else if (jobResult instanceof Throwable) {
                    s_logger.error("Unhandled exception", (Throwable)jobResult);
                    throw new RuntimeException("Unhandled exception", (Throwable)jobResult);
                }
            }

            return (VMInstanceVO)vm;
        }
    }

    private VMInstanceVO orchestrateReConfigureVm(final String vmUuid, final ServiceOffering oldServiceOffering, final boolean reconfiguringOnExistingHost) throws ResourceUnavailableException,
    ConcurrentOperationException {
        final VMInstanceVO vm = _vmDao.findByUuid(vmUuid);

        final long newServiceofferingId = vm.getServiceOfferingId();
        final ServiceOffering newServiceOffering = _offeringDao.findById(vm.getId(), newServiceofferingId);
        final HostVO hostVo = _hostDao.findById(vm.getHostId());

        final Float memoryOvercommitRatio = CapacityManager.MemOverprovisioningFactor.valueIn(hostVo.getClusterId());
        final Float cpuOvercommitRatio = CapacityManager.CpuOverprovisioningFactor.valueIn(hostVo.getClusterId());
        final long minMemory = (long)(newServiceOffering.getRamSize() / memoryOvercommitRatio);
        final ScaleVmCommand reconfigureCmd =
                new ScaleVmCommand(vm.getInstanceName(), newServiceOffering.getCpu(), (int)(newServiceOffering.getSpeed() / cpuOvercommitRatio),
                        newServiceOffering.getSpeed(), minMemory * 1024L * 1024L, newServiceOffering.getRamSize() * 1024L * 1024L, newServiceOffering.getLimitCpuUse());

        final Long dstHostId = vm.getHostId();
        if(vm.getHypervisorType().equals(HypervisorType.VMware)) {
            final HypervisorGuru hvGuru = _hvGuruMgr.getGuru(vm.getHypervisorType());
            Map<String, String> details = null;
            details = hvGuru.getClusterSettings(vm.getId());
            reconfigureCmd.getVirtualMachine().setDetails(details);
        }

        final ItWorkVO work = new ItWorkVO(UUID.randomUUID().toString(), _nodeId, State.Running, vm.getType(), vm.getId());

        work.setStep(Step.Prepare);
        work.setResourceType(ItWorkVO.ResourceType.Host);
        work.setResourceId(vm.getHostId());
        _workDao.persist(work);
        boolean success = false;
        try {
            if (reconfiguringOnExistingHost) {
                vm.setServiceOfferingId(oldServiceOffering.getId());
                _capacityMgr.releaseVmCapacity(vm, false, false, vm.getHostId()); //release the old capacity
                vm.setServiceOfferingId(newServiceofferingId);
                _capacityMgr.allocateVmCapacity(vm, false); // lock the new capacity
            }

            final Answer reconfigureAnswer = _agentMgr.send(vm.getHostId(), reconfigureCmd);
            if (reconfigureAnswer == null || !reconfigureAnswer.getResult()) {
                s_logger.error("Unable to scale vm due to " + (reconfigureAnswer == null ? "" : reconfigureAnswer.getDetails()));
                throw new CloudRuntimeException("Unable to scale vm due to " + (reconfigureAnswer == null ? "" : reconfigureAnswer.getDetails()));
            }

            success = true;
        } catch (final OperationTimedoutException e) {
            throw new AgentUnavailableException("Operation timed out on reconfiguring " + vm, dstHostId);
        } catch (final AgentUnavailableException e) {
            throw e;
        } finally {
            if (!success) {
                _capacityMgr.releaseVmCapacity(vm, false, false, vm.getHostId()); // release the new capacity
                vm.setServiceOfferingId(oldServiceOffering.getId());
                _capacityMgr.allocateVmCapacity(vm, false); // allocate the old capacity
            }
        }

        return vm;

    }

    @Override
    public String getConfigComponentName() {
        return VirtualMachineManager.class.getSimpleName();
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey<?>[] {ClusterDeltaSyncInterval, StartRetry, VmDestroyForcestop, VmOpCancelInterval, VmOpCleanupInterval, VmOpCleanupWait,
            VmOpLockStateRetry,
            VmOpWaitInterval, ExecuteInSequence, VmJobCheckInterval, VmJobTimeout, VmJobStateReportInterval, VmConfigDriveLabel, VmConfigDriveOnPrimaryPool, HaVmRestartHostUp};
    }

    public List<StoragePoolAllocator> getStoragePoolAllocators() {
        return _storagePoolAllocators;
    }

    @Inject
    public void setStoragePoolAllocators(final List<StoragePoolAllocator> storagePoolAllocators) {
        _storagePoolAllocators = storagePoolAllocators;
    }

    //
    // PowerState report handling for out-of-band changes and handling of left-over transitional VM states
    //

    @MessageHandler(topic = Topics.VM_POWER_STATE)
    protected void HandlePowerStateReport(final String subject, final String senderAddress, final Object args) {
        assert args != null;
        final Long vmId = (Long)args;

        final List<VmWorkJobVO> pendingWorkJobs = _workJobDao.listPendingWorkJobs(
                VirtualMachine.Type.Instance, vmId);
        if (pendingWorkJobs.size() == 0 && !_haMgr.hasPendingHaWork(vmId)) {
            // there is no pending operation job
            final VMInstanceVO vm = _vmDao.findById(vmId);
            if (vm != null) {
                switch (vm.getPowerState()) {
                case PowerOn:
                    handlePowerOnReportWithNoPendingJobsOnVM(vm);
                    break;

                case PowerOff:
                case PowerReportMissing: // rigorously set to Migrating? or just do nothing until...? or create a missing state?
                    // for now handle in line with legacy as power off
                    handlePowerOffReportWithNoPendingJobsOnVM(vm);
                    break;

                    // PowerUnknown shouldn't be reported, it is a derived
                    // VM power state from host state (host un-reachable)
                case PowerUnknown:
                default:
                    assert false;
                    break;
                }
            } else {
                s_logger.warn("VM " + vmId + " no longer exists when processing VM state report");
            }
        } else {
            s_logger.info("There is pending job or HA tasks working on the VM. vm id: " + vmId + ", postpone power-change report by resetting power-change counters");

            // reset VM power state tracking so that we won't lost signal when VM has
            // been translated to
            _vmDao.resetVmPowerStateTracking(vmId);
        }
    }

    private void handlePowerOnReportWithNoPendingJobsOnVM(final VMInstanceVO vm) {
        //
        //    1) handle left-over transitional VM states
        //    2) handle out of band VM live migration
        //    3) handle out of sync stationary states, marking VM from Stopped to Running with
        //       alert messages
        //
        switch (vm.getState()) {
        case Starting:
            s_logger.info("VM " + vm.getInstanceName() + " is at " + vm.getState() + " and we received a power-on report while there is no pending jobs on it");

            try {
                stateTransitTo(vm, VirtualMachine.Event.FollowAgentPowerOnReport, vm.getPowerHostId());
            } catch (final NoTransitionException e) {
                s_logger.warn("Unexpected VM state transition exception, race-condition?", e);
            }

            s_logger.info("VM " + vm.getInstanceName() + " is sync-ed to at Running state according to power-on report from hypervisor");

            // we need to alert admin or user about this risky state transition
            _alertMgr.sendAlert(AlertManager.AlertType.ALERT_TYPE_SYNC, vm.getDataCenterId(), vm.getPodIdToDeployIn(),
                    VM_SYNC_ALERT_SUBJECT, "VM " + vm.getHostName() + "(" + vm.getInstanceName()
                    + ") state is sync-ed (Starting -> Running) from out-of-context transition. VM network environment may need to be reset");
            break;

        case Running:
            try {
                if (vm.getHostId() != null && vm.getHostId().longValue() != vm.getPowerHostId().longValue()) {
                    s_logger.info("Detected out of band VM migration from host " + vm.getHostId() + " to host " + vm.getPowerHostId());
                }
                stateTransitTo(vm, VirtualMachine.Event.FollowAgentPowerOnReport, vm.getPowerHostId());
            } catch (final NoTransitionException e) {
                s_logger.warn("Unexpected VM state transition exception, race-condition?", e);
            }

            break;

        case Stopping:
        case Stopped:
            s_logger.info("VM " + vm.getInstanceName() + " is at " + vm.getState() + " and we received a power-on report while there is no pending jobs on it");

            try {
                stateTransitTo(vm, VirtualMachine.Event.FollowAgentPowerOnReport, vm.getPowerHostId());
            } catch (final NoTransitionException e) {
                s_logger.warn("Unexpected VM state transition exception, race-condition?", e);
            }
            _alertMgr.sendAlert(AlertManager.AlertType.ALERT_TYPE_SYNC, vm.getDataCenterId(), vm.getPodIdToDeployIn(),
                    VM_SYNC_ALERT_SUBJECT, "VM " + vm.getHostName() + "(" + vm.getInstanceName() + ") state is sync-ed (" + vm.getState()
                    + " -> Running) from out-of-context transition. VM network environment may need to be reset");

            s_logger.info("VM " + vm.getInstanceName() + " is sync-ed to at Running state according to power-on report from hypervisor");
            break;

        case Destroyed:
        case Expunging:
            s_logger.info("Receive power on report when VM is in destroyed or expunging state. vm: "
                    + vm.getId() + ", state: " + vm.getState());
            break;

        case Migrating:
            s_logger.info("VM " + vm.getInstanceName() + " is at " + vm.getState() + " and we received a power-on report while there is no pending jobs on it");
            try {
                stateTransitTo(vm, VirtualMachine.Event.FollowAgentPowerOnReport, vm.getPowerHostId());
            } catch (final NoTransitionException e) {
                s_logger.warn("Unexpected VM state transition exception, race-condition?", e);
            }
            s_logger.info("VM " + vm.getInstanceName() + " is sync-ed to at Running state according to power-on report from hypervisor");
            break;

        case Error:
        default:
            s_logger.info("Receive power on report when VM is in error or unexpected state. vm: "
                    + vm.getId() + ", state: " + vm.getState());
            break;
        }
    }

    private void handlePowerOffReportWithNoPendingJobsOnVM(final VMInstanceVO vm) {

        //    1) handle left-over transitional VM states
        //    2) handle out of sync stationary states, schedule force-stop to release resources
        //
        switch (vm.getState()) {
        case Starting:
        case Stopping:
        case Running:
        case Stopped:
        case Migrating:
            if (s_logger.isInfoEnabled()) {
                s_logger.info(
                        String.format("VM %s is at %s and we received a %s report while there is no pending jobs on it"
                                , vm.getInstanceName(), vm.getState(), vm.getPowerState()));
            }
            if(vm.isHaEnabled() && vm.getState() == State.Running
                    && HaVmRestartHostUp.value()
                    && vm.getHypervisorType() != HypervisorType.VMware
                    && vm.getHypervisorType() != HypervisorType.Hyperv) {
                s_logger.info("Detected out-of-band stop of a HA enabled VM " + vm.getInstanceName() + ", will schedule restart");
                if(!_haMgr.hasPendingHaWork(vm.getId())) {
                    _haMgr.scheduleRestart(vm, true);
                } else {
                    s_logger.info("VM " + vm.getInstanceName() + " already has an pending HA task working on it");
                }
                return;
            }

            // not when report is missing
            if(PowerState.PowerOff.equals(vm.getPowerState())) {
                final VirtualMachineGuru vmGuru = getVmGuru(vm);
                final VirtualMachineProfile profile = new VirtualMachineProfileImpl(vm);
                if (!sendStop(vmGuru, profile, true, true)) {
                    // In case StopCommand fails, don't proceed further
                    return;
                }
            }

            try {
                stateTransitTo(vm, VirtualMachine.Event.FollowAgentPowerOffReport, null);
            } catch (final NoTransitionException e) {
                s_logger.warn("Unexpected VM state transition exception, race-condition?", e);
            }

            _alertMgr.sendAlert(AlertManager.AlertType.ALERT_TYPE_SYNC, vm.getDataCenterId(), vm.getPodIdToDeployIn(),
                    VM_SYNC_ALERT_SUBJECT, "VM " + vm.getHostName() + "(" + vm.getInstanceName() + ") state is sync-ed (" + vm.getState()
                    + " -> Stopped) from out-of-context transition.");

            s_logger.info("VM " + vm.getInstanceName() + " is sync-ed to at Stopped state according to power-off report from hypervisor");

            break;

        case Destroyed:
        case Expunging:
            break;

        case Error:
        default:
            break;
        }
    }

    private void scanStalledVMInTransitionStateOnUpHost(final long hostId) {
        //
        // Check VM that is stuck in Starting, Stopping, Migrating states, we won't check
        // VMs in expunging state (this need to be handled specially)
        //
        // checking condition
        //    1) no pending VmWork job
        //    2) on hostId host and host is UP
        //
        // When host is UP, soon or later we will get a report from the host about the VM,
        // however, if VM is missing from the host report (it may happen in out of band changes
        // or from designed behave of XS/KVM), the VM may not get a chance to run the state-sync logic
        //
        // Therefore, we will scan thoses VMs on UP host based on last update timestamp, if the host is UP
        // and a VM stalls for status update, we will consider them to be powered off
        // (which is relatively safe to do so)

        final long stallThresholdInMs = VmJobStateReportInterval.value() + (VmJobStateReportInterval.value() >> 1);
        final Date cutTime = new Date(DateUtil.currentGMTTime().getTime() - stallThresholdInMs);
        final List<Long> mostlikelyStoppedVMs = listStalledVMInTransitionStateOnUpHost(hostId, cutTime);
        for (final Long vmId : mostlikelyStoppedVMs) {
            final VMInstanceVO vm = _vmDao.findById(vmId);
            assert vm != null;
            handlePowerOffReportWithNoPendingJobsOnVM(vm);
        }

        final List<Long> vmsWithRecentReport = listVMInTransitionStateWithRecentReportOnUpHost(hostId, cutTime);
        for (final Long vmId : vmsWithRecentReport) {
            final VMInstanceVO vm = _vmDao.findById(vmId);
            assert vm != null;
            if (vm.getPowerState() == PowerState.PowerOn) {
                handlePowerOnReportWithNoPendingJobsOnVM(vm);
            } else {
                handlePowerOffReportWithNoPendingJobsOnVM(vm);
            }
        }
    }

    private void scanStalledVMInTransitionStateOnDisconnectedHosts() {
        final Date cutTime = new Date(DateUtil.currentGMTTime().getTime() - VmOpWaitInterval.value() * 1000);
        final List<Long> stuckAndUncontrollableVMs = listStalledVMInTransitionStateOnDisconnectedHosts(cutTime);
        for (final Long vmId : stuckAndUncontrollableVMs) {
            final VMInstanceVO vm = _vmDao.findById(vmId);

            // We now only alert administrator about this situation
            _alertMgr.sendAlert(AlertManager.AlertType.ALERT_TYPE_SYNC, vm.getDataCenterId(), vm.getPodIdToDeployIn(),
                    VM_SYNC_ALERT_SUBJECT, "VM " + vm.getHostName() + "(" + vm.getInstanceName() + ") is stuck in " + vm.getState()
                    + " state and its host is unreachable for too long");
        }
    }

    // VMs that in transitional state without recent power state report
    private List<Long> listStalledVMInTransitionStateOnUpHost(final long hostId, final Date cutTime) {
        final String sql = "SELECT i.* FROM vm_instance as i, host as h WHERE h.status = 'UP' " +
                "AND h.id = ? AND i.power_state_update_time < ? AND i.host_id = h.id " +
                "AND (i.state ='Starting' OR i.state='Stopping' OR i.state='Migrating') " +
                "AND i.id NOT IN (SELECT w.vm_instance_id FROM vm_work_job AS w JOIN async_job AS j ON w.id = j.id WHERE j.job_status = ?)" +
                "AND i.removed IS NULL";

        final List<Long> l = new ArrayList<Long>();
        TransactionLegacy txn = null;
        try {
            txn = TransactionLegacy.open(TransactionLegacy.CLOUD_DB);

            PreparedStatement pstmt = null;
            try {
                pstmt = txn.prepareAutoCloseStatement(sql);

                pstmt.setLong(1, hostId);
                pstmt.setString(2, DateUtil.getDateDisplayString(TimeZone.getTimeZone("GMT"), cutTime));
                pstmt.setInt(3, JobInfo.Status.IN_PROGRESS.ordinal());
                final ResultSet rs = pstmt.executeQuery();
                while (rs.next()) {
                    l.add(rs.getLong(1));
                }
            } catch (final SQLException e) {
            } catch (final Throwable e) {
            }

        } finally {
            if (txn != null) {
                txn.close();
            }
        }
        return l;
    }

    // VMs that in transitional state and recently have power state update
    private List<Long> listVMInTransitionStateWithRecentReportOnUpHost(final long hostId, final Date cutTime) {
        final String sql = "SELECT i.* FROM vm_instance as i, host as h WHERE h.status = 'UP' " +
                "AND h.id = ? AND i.power_state_update_time > ? AND i.host_id = h.id " +
                "AND (i.state ='Starting' OR i.state='Stopping' OR i.state='Migrating') " +
                "AND i.id NOT IN (SELECT w.vm_instance_id FROM vm_work_job AS w JOIN async_job AS j ON w.id = j.id WHERE j.job_status = ?)" +
                "AND i.removed IS NULL";

        final List<Long> l = new ArrayList<Long>();
        TransactionLegacy txn = null;
        try {
            txn = TransactionLegacy.open(TransactionLegacy.CLOUD_DB);
            PreparedStatement pstmt = null;
            try {
                pstmt = txn.prepareAutoCloseStatement(sql);

                pstmt.setLong(1, hostId);
                pstmt.setString(2, DateUtil.getDateDisplayString(TimeZone.getTimeZone("GMT"), cutTime));
                pstmt.setInt(3, JobInfo.Status.IN_PROGRESS.ordinal());
                final ResultSet rs = pstmt.executeQuery();
                while (rs.next()) {
                    l.add(rs.getLong(1));
                }
            } catch (final SQLException e) {
            } catch (final Throwable e) {
            }
            return l;
        } finally {
            if (txn != null) {
                txn.close();
            }
        }
    }

    private List<Long> listStalledVMInTransitionStateOnDisconnectedHosts(final Date cutTime) {
        final String sql = "SELECT i.* FROM vm_instance as i, host as h WHERE h.status != 'UP' " +
                "AND i.power_state_update_time < ? AND i.host_id = h.id " +
                "AND (i.state ='Starting' OR i.state='Stopping' OR i.state='Migrating') " +
                "AND i.id NOT IN (SELECT w.vm_instance_id FROM vm_work_job AS w JOIN async_job AS j ON w.id = j.id WHERE j.job_status = ?)" +
                "AND i.removed IS NULL";

        final List<Long> l = new ArrayList<Long>();
        TransactionLegacy txn = null;
        try {
            txn = TransactionLegacy.open(TransactionLegacy.CLOUD_DB);
            PreparedStatement pstmt = null;
            try {
                pstmt = txn.prepareAutoCloseStatement(sql);

                pstmt.setString(1, DateUtil.getDateDisplayString(TimeZone.getTimeZone("GMT"), cutTime));
                pstmt.setInt(2, JobInfo.Status.IN_PROGRESS.ordinal());
                final ResultSet rs = pstmt.executeQuery();
                while (rs.next()) {
                    l.add(rs.getLong(1));
                }
            } catch (final SQLException e) {
            } catch (final Throwable e) {
            }
            return l;
        } finally {
            if (txn != null) {
                txn.close();
            }
        }
    }

    //
    // VM operation based on new sync model
    //

    public class VmStateSyncOutcome extends OutcomeImpl<VirtualMachine> {
        private long _vmId;

        public VmStateSyncOutcome(final AsyncJob job, final PowerState desiredPowerState, final long vmId, final Long srcHostIdForMigration) {
            super(VirtualMachine.class, job, VmJobCheckInterval.value(), new Predicate() {
                @Override
                public boolean checkCondition() {
                    final AsyncJobVO jobVo = _entityMgr.findById(AsyncJobVO.class, job.getId());
                    assert jobVo != null;
                    if (jobVo == null || jobVo.getStatus() != JobInfo.Status.IN_PROGRESS) {
                        return true;
                    }
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

    public class VmJobVirtualMachineOutcome extends OutcomeImpl<VirtualMachine> {
        private long _vmId;

        public VmJobVirtualMachineOutcome(final AsyncJob job, final long vmId) {
            super(VirtualMachine.class, job, VmJobCheckInterval.value(), new Predicate() {
                @Override
                public boolean checkCondition() {
                    final AsyncJobVO jobVo = _entityMgr.findById(AsyncJobVO.class, job.getId());
                    assert jobVo != null;
                    if (jobVo == null || jobVo.getStatus() != JobInfo.Status.IN_PROGRESS) {
                        return true;
                    }

                    return false;
                }
            }, AsyncJob.Topics.JOB_STATE);
            _vmId = vmId;
        }

        @Override
        protected VirtualMachine retrieve() {
            return _vmDao.findById(_vmId);
        }
    }

    //
    // TODO build a common pattern to reduce code duplication in following methods
    // no time for this at current iteration
    //
    public Outcome<VirtualMachine> startVmThroughJobQueue(final String vmUuid,
            final Map<VirtualMachineProfile.Param, Object> params,
            final DeploymentPlan planToDeploy, final DeploymentPlanner planner) {

        final CallContext context = CallContext.current();
        final User callingUser = context.getCallingUser();
        final Account callingAccount = context.getCallingAccount();

        final VMInstanceVO vm = _vmDao.findByUuid(vmUuid);

        VmWorkJobVO workJob = null;
        final List<VmWorkJobVO> pendingWorkJobs = _workJobDao.listPendingWorkJobs(VirtualMachine.Type.Instance,
                vm.getId(), VmWorkStart.class.getName());

        if (pendingWorkJobs.size() > 0) {
            assert pendingWorkJobs.size() == 1;
            workJob = pendingWorkJobs.get(0);
        } else {
            workJob = new VmWorkJobVO(context.getContextId());

            workJob.setDispatcher(VmWorkConstants.VM_WORK_JOB_DISPATCHER);
            workJob.setCmd(VmWorkStart.class.getName());

            workJob.setAccountId(callingAccount.getId());
            workJob.setUserId(callingUser.getId());
            workJob.setStep(VmWorkJobVO.Step.Starting);
            workJob.setVmType(VirtualMachine.Type.Instance);
            workJob.setVmInstanceId(vm.getId());
            workJob.setRelated(AsyncJobExecutionContext.getOriginJobId());

            // save work context info (there are some duplications)
            final VmWorkStart workInfo = new VmWorkStart(callingUser.getId(), callingAccount.getId(), vm.getId(), VirtualMachineManagerImpl.VM_WORK_JOB_HANDLER);
            workInfo.setPlan(planToDeploy);
            workInfo.setParams(params);
            if (planner != null) {
                workInfo.setDeploymentPlanner(planner.getName());
            }
            workJob.setCmdInfo(VmWorkSerializer.serialize(workInfo));

            _jobMgr.submitAsyncJob(workJob, VmWorkConstants.VM_WORK_QUEUE, vm.getId());
        }

        AsyncJobExecutionContext.getCurrentExecutionContext().joinJob(workJob.getId());

        return new VmStateSyncOutcome(workJob,
                VirtualMachine.PowerState.PowerOn, vm.getId(), null);
    }

    public Outcome<VirtualMachine> stopVmThroughJobQueue(final String vmUuid, final boolean cleanup) {
        final CallContext context = CallContext.current();
        final Account account = context.getCallingAccount();
        final User user = context.getCallingUser();

        final VMInstanceVO vm = _vmDao.findByUuid(vmUuid);

        final List<VmWorkJobVO> pendingWorkJobs = _workJobDao.listPendingWorkJobs(
                vm.getType(), vm.getId(),
                VmWorkStop.class.getName());

        VmWorkJobVO workJob = null;
        if (pendingWorkJobs != null && pendingWorkJobs.size() > 0) {
            assert pendingWorkJobs.size() == 1;
            workJob = pendingWorkJobs.get(0);
        } else {
            workJob = new VmWorkJobVO(context.getContextId());

            workJob.setDispatcher(VmWorkConstants.VM_WORK_JOB_DISPATCHER);
            workJob.setCmd(VmWorkStop.class.getName());

            workJob.setAccountId(account.getId());
            workJob.setUserId(user.getId());
            workJob.setStep(VmWorkJobVO.Step.Prepare);
            workJob.setVmType(VirtualMachine.Type.Instance);
            workJob.setVmInstanceId(vm.getId());
            workJob.setRelated(AsyncJobExecutionContext.getOriginJobId());

            // save work context info (there are some duplications)
            final VmWorkStop workInfo = new VmWorkStop(user.getId(), account.getId(), vm.getId(), VirtualMachineManagerImpl.VM_WORK_JOB_HANDLER, cleanup);
            workJob.setCmdInfo(VmWorkSerializer.serialize(workInfo));

            _jobMgr.submitAsyncJob(workJob, VmWorkConstants.VM_WORK_QUEUE, vm.getId());
        }

        AsyncJobExecutionContext.getCurrentExecutionContext().joinJob(workJob.getId());

        return new VmStateSyncOutcome(workJob,
                VirtualMachine.PowerState.PowerOff, vm.getId(), null);
    }

    public Outcome<VirtualMachine> rebootVmThroughJobQueue(final String vmUuid,
            final Map<VirtualMachineProfile.Param, Object> params) {

        final CallContext context = CallContext.current();
        final Account account = context.getCallingAccount();
        final User user = context.getCallingUser();

        final VMInstanceVO vm = _vmDao.findByUuid(vmUuid);

        final List<VmWorkJobVO> pendingWorkJobs = _workJobDao.listPendingWorkJobs(
                VirtualMachine.Type.Instance, vm.getId(),
                VmWorkReboot.class.getName());

        VmWorkJobVO workJob = null;
        if (pendingWorkJobs != null && pendingWorkJobs.size() > 0) {
            assert pendingWorkJobs.size() == 1;
            workJob = pendingWorkJobs.get(0);
        } else {
            workJob = new VmWorkJobVO(context.getContextId());

            workJob.setDispatcher(VmWorkConstants.VM_WORK_JOB_DISPATCHER);
            workJob.setCmd(VmWorkReboot.class.getName());

            workJob.setAccountId(account.getId());
            workJob.setUserId(user.getId());
            workJob.setStep(VmWorkJobVO.Step.Prepare);
            workJob.setVmType(VirtualMachine.Type.Instance);
            workJob.setVmInstanceId(vm.getId());
            workJob.setRelated(AsyncJobExecutionContext.getOriginJobId());

            // save work context info (there are some duplications)
            final VmWorkReboot workInfo = new VmWorkReboot(user.getId(), account.getId(), vm.getId(), VirtualMachineManagerImpl.VM_WORK_JOB_HANDLER, params);
            workJob.setCmdInfo(VmWorkSerializer.serialize(workInfo));

            _jobMgr.submitAsyncJob(workJob, VmWorkConstants.VM_WORK_QUEUE, vm.getId());
        }

        AsyncJobExecutionContext.getCurrentExecutionContext().joinJob(workJob.getId());

        return new VmJobVirtualMachineOutcome(workJob,
                vm.getId());
    }

    public Outcome<VirtualMachine> migrateVmThroughJobQueue(final String vmUuid, final long srcHostId, final DeployDestination dest) {
        final CallContext context = CallContext.current();
        final User user = context.getCallingUser();
        final Account account = context.getCallingAccount();

        Map<Volume, StoragePool> volumeStorageMap = dest.getStorageForDisks();
        if (volumeStorageMap != null) {
            for (Volume vol : volumeStorageMap.keySet()) {
                checkConcurrentJobsPerDatastoreThreshhold(volumeStorageMap.get(vol));
            }
        }

        final VMInstanceVO vm = _vmDao.findByUuid(vmUuid);

        final List<VmWorkJobVO> pendingWorkJobs = _workJobDao.listPendingWorkJobs(
                VirtualMachine.Type.Instance, vm.getId(),
                VmWorkMigrate.class.getName());

        VmWorkJobVO workJob = null;
        if (pendingWorkJobs != null && pendingWorkJobs.size() > 0) {
            assert pendingWorkJobs.size() == 1;
            workJob = pendingWorkJobs.get(0);
        } else {

            workJob = new VmWorkJobVO(context.getContextId());

            workJob.setDispatcher(VmWorkConstants.VM_WORK_JOB_DISPATCHER);
            workJob.setCmd(VmWorkMigrate.class.getName());

            workJob.setAccountId(account.getId());
            workJob.setUserId(user.getId());
            workJob.setVmType(VirtualMachine.Type.Instance);
            workJob.setVmInstanceId(vm.getId());
            workJob.setRelated(AsyncJobExecutionContext.getOriginJobId());

            // save work context info (there are some duplications)
            final VmWorkMigrate workInfo = new VmWorkMigrate(user.getId(), account.getId(), vm.getId(), VirtualMachineManagerImpl.VM_WORK_JOB_HANDLER, srcHostId, dest);
            workJob.setCmdInfo(VmWorkSerializer.serialize(workInfo));

            _jobMgr.submitAsyncJob(workJob, VmWorkConstants.VM_WORK_QUEUE, vm.getId());
        }

        AsyncJobExecutionContext.getCurrentExecutionContext().joinJob(workJob.getId());

        return new VmStateSyncOutcome(workJob,
                VirtualMachine.PowerState.PowerOn, vm.getId(), vm.getPowerHostId());
    }

    public Outcome<VirtualMachine> migrateVmAwayThroughJobQueue(final String vmUuid, final long srcHostId) {
        final CallContext context = CallContext.current();
        final User user = context.getCallingUser();
        final Account account = context.getCallingAccount();

        final VMInstanceVO vm = _vmDao.findByUuid(vmUuid);

        final List<VmWorkJobVO> pendingWorkJobs = _workJobDao.listPendingWorkJobs(
                VirtualMachine.Type.Instance, vm.getId(),
                VmWorkMigrateAway.class.getName());

        VmWorkJobVO workJob = null;
        if (pendingWorkJobs != null && pendingWorkJobs.size() > 0) {
            assert pendingWorkJobs.size() == 1;
            workJob = pendingWorkJobs.get(0);
        } else {
            workJob = new VmWorkJobVO(context.getContextId());

            workJob.setDispatcher(VmWorkConstants.VM_WORK_JOB_DISPATCHER);
            workJob.setCmd(VmWorkMigrateAway.class.getName());

            workJob.setAccountId(account.getId());
            workJob.setUserId(user.getId());
            workJob.setVmType(VirtualMachine.Type.Instance);
            workJob.setVmInstanceId(vm.getId());
            workJob.setRelated(AsyncJobExecutionContext.getOriginJobId());

            // save work context info (there are some duplications)
            final VmWorkMigrateAway workInfo = new VmWorkMigrateAway(user.getId(), account.getId(), vm.getId(), VirtualMachineManagerImpl.VM_WORK_JOB_HANDLER, srcHostId);
            workJob.setCmdInfo(VmWorkSerializer.serialize(workInfo));
        }

        _jobMgr.submitAsyncJob(workJob, VmWorkConstants.VM_WORK_QUEUE, vm.getId());

        AsyncJobExecutionContext.getCurrentExecutionContext().joinJob(workJob.getId());

        return new VmStateSyncOutcome(workJob, VirtualMachine.PowerState.PowerOn, vm.getId(), vm.getPowerHostId());
    }

    public Outcome<VirtualMachine> migrateVmWithStorageThroughJobQueue(
            final String vmUuid, final long srcHostId, final long destHostId,
            final Map<Long, Long> volumeToPool) {

        final CallContext context = CallContext.current();
        final User user = context.getCallingUser();
        final Account account = context.getCallingAccount();

        final VMInstanceVO vm = _vmDao.findByUuid(vmUuid);

        final List<VmWorkJobVO> pendingWorkJobs = _workJobDao.listPendingWorkJobs(
                VirtualMachine.Type.Instance, vm.getId(),
                VmWorkMigrateWithStorage.class.getName());

        VmWorkJobVO workJob = null;
        if (pendingWorkJobs != null && pendingWorkJobs.size() > 0) {
            assert pendingWorkJobs.size() == 1;
            workJob = pendingWorkJobs.get(0);
        } else {

            workJob = new VmWorkJobVO(context.getContextId());

            workJob.setDispatcher(VmWorkConstants.VM_WORK_JOB_DISPATCHER);
            workJob.setCmd(VmWorkMigrateWithStorage.class.getName());

            workJob.setAccountId(account.getId());
            workJob.setUserId(user.getId());
            workJob.setVmType(VirtualMachine.Type.Instance);
            workJob.setVmInstanceId(vm.getId());
            workJob.setRelated(AsyncJobExecutionContext.getOriginJobId());

            // save work context info (there are some duplications)
            final VmWorkMigrateWithStorage workInfo = new VmWorkMigrateWithStorage(user.getId(), account.getId(), vm.getId(),
                    VirtualMachineManagerImpl.VM_WORK_JOB_HANDLER, srcHostId, destHostId, volumeToPool);
            workJob.setCmdInfo(VmWorkSerializer.serialize(workInfo));

            _jobMgr.submitAsyncJob(workJob, VmWorkConstants.VM_WORK_QUEUE, vm.getId());
        }
        AsyncJobExecutionContext.getCurrentExecutionContext().joinJob(workJob.getId());

        return new VmStateSyncOutcome(workJob,
                VirtualMachine.PowerState.PowerOn, vm.getId(), destHostId);
    }

    public Outcome<VirtualMachine> migrateVmForScaleThroughJobQueue(
            final String vmUuid, final long srcHostId, final DeployDestination dest, final Long newSvcOfferingId) {

        final CallContext context = CallContext.current();
        final User user = context.getCallingUser();
        final Account account = context.getCallingAccount();

        final VMInstanceVO vm = _vmDao.findByUuid(vmUuid);

        final List<VmWorkJobVO> pendingWorkJobs = _workJobDao.listPendingWorkJobs(
                VirtualMachine.Type.Instance, vm.getId(),
                VmWorkMigrateForScale.class.getName());

        VmWorkJobVO workJob = null;
        if (pendingWorkJobs != null && pendingWorkJobs.size() > 0) {
            assert pendingWorkJobs.size() == 1;
            workJob = pendingWorkJobs.get(0);
        } else {

            workJob = new VmWorkJobVO(context.getContextId());

            workJob.setDispatcher(VmWorkConstants.VM_WORK_JOB_DISPATCHER);
            workJob.setCmd(VmWorkMigrateForScale.class.getName());

            workJob.setAccountId(account.getId());
            workJob.setUserId(user.getId());
            workJob.setVmType(VirtualMachine.Type.Instance);
            workJob.setVmInstanceId(vm.getId());
            workJob.setRelated(AsyncJobExecutionContext.getOriginJobId());

            // save work context info (there are some duplications)
            final VmWorkMigrateForScale workInfo = new VmWorkMigrateForScale(user.getId(), account.getId(), vm.getId(),
                    VirtualMachineManagerImpl.VM_WORK_JOB_HANDLER, srcHostId, dest, newSvcOfferingId);
            workJob.setCmdInfo(VmWorkSerializer.serialize(workInfo));

            _jobMgr.submitAsyncJob(workJob, VmWorkConstants.VM_WORK_QUEUE, vm.getId());
        }
        AsyncJobExecutionContext.getCurrentExecutionContext().joinJob(workJob.getId());

        return new VmJobVirtualMachineOutcome(workJob, vm.getId());
    }

    private void checkConcurrentJobsPerDatastoreThreshhold(final StoragePool destPool) {
        final Long threshold = VolumeApiService.ConcurrentMigrationsThresholdPerDatastore.value();
        if (threshold != null && threshold > 0) {
            long count = _jobMgr.countPendingJobs("\"storageid\":\"" + destPool.getUuid() + "\"", MigrateVMCmd.class.getName(), MigrateVolumeCmd.class.getName(), MigrateVolumeCmdByAdmin.class.getName());
            if (count > threshold) {
                throw new CloudRuntimeException("Number of concurrent migration jobs per datastore exceeded the threshold: " + threshold.toString() + ". Please try again after some time.");
            }
        }
    }

    public Outcome<VirtualMachine> migrateVmStorageThroughJobQueue(
            final String vmUuid, final StoragePool destPool) {

        final CallContext context = CallContext.current();
        final User user = context.getCallingUser();
        final Account account = context.getCallingAccount();

        final VMInstanceVO vm = _vmDao.findByUuid(vmUuid);

        final List<VmWorkJobVO> pendingWorkJobs = _workJobDao.listPendingWorkJobs(
                VirtualMachine.Type.Instance, vm.getId(),
                VmWorkStorageMigration.class.getName());

        VmWorkJobVO workJob = null;
        if (pendingWorkJobs != null && pendingWorkJobs.size() > 0) {
            assert pendingWorkJobs.size() == 1;
            workJob = pendingWorkJobs.get(0);
        } else {

            workJob = new VmWorkJobVO(context.getContextId());

            workJob.setDispatcher(VmWorkConstants.VM_WORK_JOB_DISPATCHER);
            workJob.setCmd(VmWorkStorageMigration.class.getName());

            workJob.setAccountId(account.getId());
            workJob.setUserId(user.getId());
            workJob.setVmType(VirtualMachine.Type.Instance);
            workJob.setVmInstanceId(vm.getId());
            workJob.setRelated(AsyncJobExecutionContext.getOriginJobId());

            // save work context info (there are some duplications)
            final VmWorkStorageMigration workInfo = new VmWorkStorageMigration(user.getId(), account.getId(), vm.getId(),
                    VirtualMachineManagerImpl.VM_WORK_JOB_HANDLER, destPool.getId());
            workJob.setCmdInfo(VmWorkSerializer.serialize(workInfo));

            _jobMgr.submitAsyncJob(workJob, VmWorkConstants.VM_WORK_QUEUE, vm.getId());
        }
        AsyncJobExecutionContext.getCurrentExecutionContext().joinJob(workJob.getId());

        return new VmJobVirtualMachineOutcome(workJob, vm.getId());
    }

    public Outcome<VirtualMachine> addVmToNetworkThroughJobQueue(
            final VirtualMachine vm, final Network network, final NicProfile requested) {

        final CallContext context = CallContext.current();
        final User user = context.getCallingUser();
        final Account account = context.getCallingAccount();

        final List<VmWorkJobVO> pendingWorkJobs = _workJobDao.listPendingWorkJobs(
                VirtualMachine.Type.Instance, vm.getId(),
                VmWorkAddVmToNetwork.class.getName());

        VmWorkJobVO workJob = null;
        if (pendingWorkJobs != null && pendingWorkJobs.size() > 0) {
            assert pendingWorkJobs.size() == 1;
            workJob = pendingWorkJobs.get(0);
        } else {

            workJob = new VmWorkJobVO(context.getContextId());

            workJob.setDispatcher(VmWorkConstants.VM_WORK_JOB_DISPATCHER);
            workJob.setCmd(VmWorkAddVmToNetwork.class.getName());

            workJob.setAccountId(account.getId());
            workJob.setUserId(user.getId());
            workJob.setVmType(VirtualMachine.Type.Instance);
            workJob.setVmInstanceId(vm.getId());
            workJob.setRelated(AsyncJobExecutionContext.getOriginJobId());

            // save work context info (there are some duplications)
            final VmWorkAddVmToNetwork workInfo = new VmWorkAddVmToNetwork(user.getId(), account.getId(), vm.getId(),
                    VirtualMachineManagerImpl.VM_WORK_JOB_HANDLER, network.getId(), requested);
            workJob.setCmdInfo(VmWorkSerializer.serialize(workInfo));

            _jobMgr.submitAsyncJob(workJob, VmWorkConstants.VM_WORK_QUEUE, vm.getId());
        }
        AsyncJobExecutionContext.getCurrentExecutionContext().joinJob(workJob.getId());

        return new VmJobVirtualMachineOutcome(workJob, vm.getId());
    }

    public Outcome<VirtualMachine> removeNicFromVmThroughJobQueue(
            final VirtualMachine vm, final Nic nic) {

        final CallContext context = CallContext.current();
        final User user = context.getCallingUser();
        final Account account = context.getCallingAccount();

        final List<VmWorkJobVO> pendingWorkJobs = _workJobDao.listPendingWorkJobs(
                VirtualMachine.Type.Instance, vm.getId(),
                VmWorkRemoveNicFromVm.class.getName());

        VmWorkJobVO workJob = null;
        if (pendingWorkJobs != null && pendingWorkJobs.size() > 0) {
            assert pendingWorkJobs.size() == 1;
            workJob = pendingWorkJobs.get(0);
        } else {

            workJob = new VmWorkJobVO(context.getContextId());

            workJob.setDispatcher(VmWorkConstants.VM_WORK_JOB_DISPATCHER);
            workJob.setCmd(VmWorkRemoveNicFromVm.class.getName());

            workJob.setAccountId(account.getId());
            workJob.setUserId(user.getId());
            workJob.setVmType(VirtualMachine.Type.Instance);
            workJob.setVmInstanceId(vm.getId());
            workJob.setRelated(AsyncJobExecutionContext.getOriginJobId());

            // save work context info (there are some duplications)
            final VmWorkRemoveNicFromVm workInfo = new VmWorkRemoveNicFromVm(user.getId(), account.getId(), vm.getId(),
                    VirtualMachineManagerImpl.VM_WORK_JOB_HANDLER, nic.getId());
            workJob.setCmdInfo(VmWorkSerializer.serialize(workInfo));

            _jobMgr.submitAsyncJob(workJob, VmWorkConstants.VM_WORK_QUEUE, vm.getId());
        }
        AsyncJobExecutionContext.getCurrentExecutionContext().joinJob(workJob.getId());

        return new VmJobVirtualMachineOutcome(workJob, vm.getId());
    }

    public Outcome<VirtualMachine> removeVmFromNetworkThroughJobQueue(
            final VirtualMachine vm, final Network network, final URI broadcastUri) {

        final CallContext context = CallContext.current();
        final User user = context.getCallingUser();
        final Account account = context.getCallingAccount();

        final List<VmWorkJobVO> pendingWorkJobs = _workJobDao.listPendingWorkJobs(
                VirtualMachine.Type.Instance, vm.getId(),
                VmWorkRemoveVmFromNetwork.class.getName());

        VmWorkJobVO workJob = null;
        if (pendingWorkJobs != null && pendingWorkJobs.size() > 0) {
            assert pendingWorkJobs.size() == 1;
            workJob = pendingWorkJobs.get(0);
        } else {

            workJob = new VmWorkJobVO(context.getContextId());

            workJob.setDispatcher(VmWorkConstants.VM_WORK_JOB_DISPATCHER);
            workJob.setCmd(VmWorkRemoveVmFromNetwork.class.getName());

            workJob.setAccountId(account.getId());
            workJob.setUserId(user.getId());
            workJob.setVmType(VirtualMachine.Type.Instance);
            workJob.setVmInstanceId(vm.getId());
            workJob.setRelated(AsyncJobExecutionContext.getOriginJobId());

            // save work context info (there are some duplications)
            final VmWorkRemoveVmFromNetwork workInfo = new VmWorkRemoveVmFromNetwork(user.getId(), account.getId(), vm.getId(),
                    VirtualMachineManagerImpl.VM_WORK_JOB_HANDLER, network, broadcastUri);
            workJob.setCmdInfo(VmWorkSerializer.serialize(workInfo));

            _jobMgr.submitAsyncJob(workJob, VmWorkConstants.VM_WORK_QUEUE, vm.getId());
        }

        AsyncJobExecutionContext.getCurrentExecutionContext().joinJob(workJob.getId());

        return new VmJobVirtualMachineOutcome(workJob, vm.getId());
    }

    public Outcome<VirtualMachine> reconfigureVmThroughJobQueue(
            final String vmUuid, final ServiceOffering newServiceOffering, final boolean reconfiguringOnExistingHost) {

        final CallContext context = CallContext.current();
        final User user = context.getCallingUser();
        final Account account = context.getCallingAccount();

        final VMInstanceVO vm = _vmDao.findByUuid(vmUuid);

        final List<VmWorkJobVO> pendingWorkJobs = _workJobDao.listPendingWorkJobs(
                VirtualMachine.Type.Instance, vm.getId(),
                VmWorkReconfigure.class.getName());

        VmWorkJobVO workJob = null;
        if (pendingWorkJobs != null && pendingWorkJobs.size() > 0) {
            assert pendingWorkJobs.size() == 1;
            workJob = pendingWorkJobs.get(0);
        } else {

            workJob = new VmWorkJobVO(context.getContextId());

            workJob.setDispatcher(VmWorkConstants.VM_WORK_JOB_DISPATCHER);
            workJob.setCmd(VmWorkReconfigure.class.getName());

            workJob.setAccountId(account.getId());
            workJob.setUserId(user.getId());
            workJob.setVmType(VirtualMachine.Type.Instance);
            workJob.setVmInstanceId(vm.getId());
            workJob.setRelated(AsyncJobExecutionContext.getOriginJobId());

            // save work context info (there are some duplications)
            final VmWorkReconfigure workInfo = new VmWorkReconfigure(user.getId(), account.getId(), vm.getId(),
                    VirtualMachineManagerImpl.VM_WORK_JOB_HANDLER, newServiceOffering.getId(), reconfiguringOnExistingHost);
            workJob.setCmdInfo(VmWorkSerializer.serialize(workInfo));

            _jobMgr.submitAsyncJob(workJob, VmWorkConstants.VM_WORK_QUEUE, vm.getId());
        }
        AsyncJobExecutionContext.getCurrentExecutionContext().joinJob(workJob.getId());

        return new VmJobVirtualMachineOutcome(workJob, vm.getId());
    }

    @ReflectionUse
    private Pair<JobInfo.Status, String> orchestrateStart(final VmWorkStart work) throws Exception {
        final VMInstanceVO vm = _entityMgr.findById(VMInstanceVO.class, work.getVmId());
        if (vm == null) {
            s_logger.info("Unable to find vm " + work.getVmId());
        }
        assert vm != null;

        try{
            orchestrateStart(vm.getUuid(), work.getParams(), work.getPlan(), _dpMgr.getDeploymentPlannerByName(work.getDeploymentPlanner()));
        }
        catch (CloudRuntimeException e){
            e.printStackTrace();
            s_logger.info("Caught CloudRuntimeException, returning job failed " + e);
            CloudRuntimeException ex = new CloudRuntimeException("Unable to start VM instance");
            return new Pair<JobInfo.Status, String>(JobInfo.Status.FAILED, JobSerializerHelper.toObjectSerializedString(ex));
        }
        return new Pair<JobInfo.Status, String>(JobInfo.Status.SUCCEEDED, null);
    }

    @ReflectionUse
    private Pair<JobInfo.Status, String> orchestrateStop(final VmWorkStop work) throws Exception {
        final VMInstanceVO vm = _entityMgr.findById(VMInstanceVO.class, work.getVmId());
        if (vm == null) {
            s_logger.info("Unable to find vm " + work.getVmId());
            throw new CloudRuntimeException("Unable to find VM id=" + work.getVmId());
        }

        orchestrateStop(vm.getUuid(), work.isCleanup());
        return new Pair<JobInfo.Status, String>(JobInfo.Status.SUCCEEDED, null);
    }

    @ReflectionUse
    private Pair<JobInfo.Status, String> orchestrateMigrate(final VmWorkMigrate work) throws Exception {
        final VMInstanceVO vm = _entityMgr.findById(VMInstanceVO.class, work.getVmId());
        if (vm == null) {
            s_logger.info("Unable to find vm " + work.getVmId());
        }
        assert vm != null;

        orchestrateMigrate(vm.getUuid(), work.getSrcHostId(), work.getDeployDestination());
        return new Pair<JobInfo.Status, String>(JobInfo.Status.SUCCEEDED, null);
    }

    @ReflectionUse
    private Pair<JobInfo.Status, String> orchestrateMigrateAway(final VmWorkMigrateAway work) throws Exception {
        final VMInstanceVO vm = _entityMgr.findById(VMInstanceVO.class, work.getVmId());
        if (vm == null) {
            s_logger.info("Unable to find vm " + work.getVmId());
        }
        assert vm != null;

        try {
            orchestrateMigrateAway(vm.getUuid(), work.getSrcHostId(), null);
        } catch (final InsufficientServerCapacityException e) {
            s_logger.warn("Failed to deploy vm " + vm.getId() + " with original planner, sending HAPlanner");
            orchestrateMigrateAway(vm.getUuid(), work.getSrcHostId(), _haMgr.getHAPlanner());
        }

        return new Pair<JobInfo.Status, String>(JobInfo.Status.SUCCEEDED, null);
    }

    @ReflectionUse
    private Pair<JobInfo.Status, String> orchestrateMigrateWithStorage(final VmWorkMigrateWithStorage work) throws Exception {
        final VMInstanceVO vm = _entityMgr.findById(VMInstanceVO.class, work.getVmId());
        if (vm == null) {
            s_logger.info("Unable to find vm " + work.getVmId());
        }
        assert vm != null;
        orchestrateMigrateWithStorage(vm.getUuid(),
                work.getSrcHostId(),
                work.getDestHostId(),
                work.getVolumeToPool());
        return new Pair<JobInfo.Status, String>(JobInfo.Status.SUCCEEDED, null);
    }

    @ReflectionUse
    private Pair<JobInfo.Status, String> orchestrateMigrateForScale(final VmWorkMigrateForScale work) throws Exception {
        final VMInstanceVO vm = _entityMgr.findById(VMInstanceVO.class, work.getVmId());
        if (vm == null) {
            s_logger.info("Unable to find vm " + work.getVmId());
        }
        assert vm != null;
        orchestrateMigrateForScale(vm.getUuid(),
                work.getSrcHostId(),
                work.getDeployDestination(),
                work.getNewServiceOfferringId());
        return new Pair<JobInfo.Status, String>(JobInfo.Status.SUCCEEDED, null);
    }

    @ReflectionUse
    private Pair<JobInfo.Status, String> orchestrateReboot(final VmWorkReboot work) throws Exception {
        final VMInstanceVO vm = _entityMgr.findById(VMInstanceVO.class, work.getVmId());
        if (vm == null) {
            s_logger.info("Unable to find vm " + work.getVmId());
        }
        assert vm != null;
        orchestrateReboot(vm.getUuid(), work.getParams());
        return new Pair<JobInfo.Status, String>(JobInfo.Status.SUCCEEDED, null);
    }

    @ReflectionUse
    private Pair<JobInfo.Status, String> orchestrateAddVmToNetwork(final VmWorkAddVmToNetwork work) throws Exception {
        final VMInstanceVO vm = _entityMgr.findById(VMInstanceVO.class, work.getVmId());
        if (vm == null) {
            s_logger.info("Unable to find vm " + work.getVmId());
        }
        assert vm != null;

        final Network network = _networkDao.findById(work.getNetworkId());
        final NicProfile nic = orchestrateAddVmToNetwork(vm, network,
                work.getRequestedNicProfile());

        return new Pair<JobInfo.Status, String>(JobInfo.Status.SUCCEEDED, _jobMgr.marshallResultObject(nic));
    }

    @ReflectionUse
    private Pair<JobInfo.Status, String> orchestrateRemoveNicFromVm(final VmWorkRemoveNicFromVm work) throws Exception {
        final VMInstanceVO vm = _entityMgr.findById(VMInstanceVO.class, work.getVmId());
        if (vm == null) {
            s_logger.info("Unable to find vm " + work.getVmId());
        }
        assert vm != null;
        final NicVO nic = _entityMgr.findById(NicVO.class, work.getNicId());
        final boolean result = orchestrateRemoveNicFromVm(vm, nic);
        return new Pair<JobInfo.Status, String>(JobInfo.Status.SUCCEEDED,
                _jobMgr.marshallResultObject(result));
    }

    @ReflectionUse
    private Pair<JobInfo.Status, String> orchestrateRemoveVmFromNetwork(final VmWorkRemoveVmFromNetwork work) throws Exception {
        final VMInstanceVO vm = _entityMgr.findById(VMInstanceVO.class, work.getVmId());
        if (vm == null) {
            s_logger.info("Unable to find vm " + work.getVmId());
        }
        assert vm != null;
        final boolean result = orchestrateRemoveVmFromNetwork(vm,
                work.getNetwork(), work.getBroadcastUri());
        return new Pair<JobInfo.Status, String>(JobInfo.Status.SUCCEEDED,
                _jobMgr.marshallResultObject(result));
    }

    @ReflectionUse
    private Pair<JobInfo.Status, String> orchestrateReconfigure(final VmWorkReconfigure work) throws Exception {
        final VMInstanceVO vm = _entityMgr.findById(VMInstanceVO.class, work.getVmId());
        if (vm == null) {
            s_logger.info("Unable to find vm " + work.getVmId());
        }
        assert vm != null;

        final ServiceOffering newServiceOffering = _offeringDao.findById(vm.getId(), work.getNewServiceOfferingId());

        reConfigureVm(vm.getUuid(), newServiceOffering,
                work.isSameHost());
        return new Pair<JobInfo.Status, String>(JobInfo.Status.SUCCEEDED, null);
    }

    @ReflectionUse
    private Pair<JobInfo.Status, String> orchestrateStorageMigration(final VmWorkStorageMigration work) throws Exception {
        final VMInstanceVO vm = _entityMgr.findById(VMInstanceVO.class, work.getVmId());
        if (vm == null) {
            s_logger.info("Unable to find vm " + work.getVmId());
        }
        assert vm != null;
        final StoragePool pool = (PrimaryDataStoreInfo)dataStoreMgr.getPrimaryDataStore(work.getDestStoragePoolId());
        orchestrateStorageMigration(vm.getUuid(), pool);

        return new Pair<JobInfo.Status, String>(JobInfo.Status.SUCCEEDED, null);
    }

    @Override
    public Pair<JobInfo.Status, String> handleVmWorkJob(final VmWork work) throws Exception {
        return _jobHandlerProxy.handleVmWorkJob(work);
    }

    private VmWorkJobVO createPlaceHolderWork(final long instanceId) {
        final VmWorkJobVO workJob = new VmWorkJobVO("");

        workJob.setDispatcher(VmWorkConstants.VM_WORK_JOB_PLACEHOLDER);
        workJob.setCmd("");
        workJob.setCmdInfo("");

        workJob.setAccountId(0);
        workJob.setUserId(0);
        workJob.setStep(VmWorkJobVO.Step.Starting);
        workJob.setVmType(VirtualMachine.Type.Instance);
        workJob.setVmInstanceId(instanceId);
        workJob.setInitMsid(ManagementServerNode.getManagementServerId());

        _workJobDao.persist(workJob);

        return workJob;
    }
}
