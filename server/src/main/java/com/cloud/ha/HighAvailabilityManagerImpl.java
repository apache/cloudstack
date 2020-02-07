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
package com.cloud.ha;

import com.cloud.agent.AgentManager;
import com.cloud.alert.AlertManager;
import com.cloud.cluster.ClusterManagerListener;
import com.cloud.dc.ClusterDetailsDao;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.HostPodVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.HostPodDao;
import com.cloud.deploy.DeploymentPlanner;
import com.cloud.deploy.HAPlanner;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InsufficientServerCapacityException;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.ha.Investigator.UnknownVM;
import com.cloud.ha.dao.HighAvailabilityDao;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.Status;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.resource.ResourceManager;
import com.cloud.server.ManagementServer;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.storage.StorageManager;
import com.cloud.storage.dao.GuestOSCategoryDao;
import com.cloud.storage.dao.GuestOSDao;
import com.cloud.user.AccountManager;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.concurrency.NamedThreadFactory;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachine.State;
import com.cloud.vm.VirtualMachineManager;
import com.cloud.vm.VirtualMachineProfile;
import com.cloud.vm.dao.VMInstanceDao;
import org.apache.cloudstack.engine.orchestration.service.VolumeOrchestrationService;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.managed.context.ManagedContext;
import org.apache.cloudstack.managed.context.ManagedContextRunnable;
import org.apache.cloudstack.management.ManagementServerHost;
import org.apache.log4j.Logger;
import org.apache.log4j.NDC;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.naming.ConfigurationException;

/**
 * HighAvailabilityManagerImpl coordinates the HA process. VMs are registered with the HA Manager for HA. The request is stored
 * within a database backed work queue. HAManager has a number of workers that pick up these work items to perform HA on the
 * VMs.
 *
 * The HA process goes as follows: 1. Check with the list of Investigators to determine that the VM is no longer running. If a
 * Investigator finds the VM is still alive, the HA process is stopped and the state of the VM reverts back to its previous
 * state. If a Investigator finds the VM is dead, then HA process is started on the VM, skipping step 2. 2. If the list of
 * Investigators can not determine if the VM is dead or alive. The list of FenceBuilders is invoked to fence off the VM so that
 * it won't do any damage to the storage and network. 3. The VM is marked as stopped. 4. The VM is started again via the normal
 * process of starting VMs. Note that once the VM is marked as stopped, the user may have started the VM himself. 5. VMs that
 * have re-started more than the configured number of times are marked as in Error state and the user is not allowed to restart
 * the VM.
 *
 * @config {@table || Param Name | Description | Values | Default || || workers | number of worker threads to spin off to do the
 *         processing | int | 1 || || time.to.sleep | Time to sleep if no work items are found | seconds | 60 || || max.retries
 *         | number of times to retry start | int | 5 || || time.between.failure | Time elapsed between failures before we
 *         consider it as another retry | seconds | 3600 || || time.between.cleanup | Time to wait before the cleanup thread
 *         runs | seconds | 86400 || || force.ha | Force HA to happen even if the VM says no | boolean | false || ||
 *         ha.retry.wait | time to wait before retrying the work item | seconds | 120 || || stop.retry.wait | time to wait
 *         before retrying the stop | seconds | 120 || * }
 **/
public class HighAvailabilityManagerImpl extends ManagerBase implements Configurable, HighAvailabilityManager, ClusterManagerListener {

    private static final int SECONDS_TO_MILLISECONDS_FACTOR = 1000;

    protected static final Logger s_logger = Logger.getLogger(HighAvailabilityManagerImpl.class);
    private ConfigKey<Integer> MaxRetries = new ConfigKey<>("Advanced", Integer.class,
            "max.retries","5",
            "Total number of attempts for trying migration of a VM.",
            true, ConfigKey.Scope.Cluster);

    WorkerThread[] _workers;
    boolean _stopped;
    long _timeToSleep;
    @Inject
    HighAvailabilityDao _haDao;
    @Inject
    VMInstanceDao _instanceDao;
    @Inject
    HostDao _hostDao;
    @Inject
    DataCenterDao _dcDao;
    @Inject
    HostPodDao _podDao;
    @Inject
    ClusterDetailsDao _clusterDetailsDao;

    @Inject
    ServiceOfferingDao _serviceOfferingDao;

    long _serverId;

    @Inject
    ManagedContext _managedContext;

    List<Investigator> investigators;

    public List<Investigator> getInvestigators() {
        return investigators;
    }

    public void setInvestigators(List<Investigator> investigators) {
        this.investigators = investigators;
    }

    List<FenceBuilder> fenceBuilders;

    public List<FenceBuilder> getFenceBuilders() {
        return fenceBuilders;
    }

    public void setFenceBuilders(List<FenceBuilder> fenceBuilders) {
        this.fenceBuilders = fenceBuilders;
    }

    List<HAPlanner> _haPlanners;
    public List<HAPlanner> getHaPlanners() {
        return _haPlanners;
    }

    public void setHaPlanners(List<HAPlanner> haPlanners) {
        _haPlanners = haPlanners;
    }


    @Inject
    AgentManager _agentMgr;
    @Inject
    AlertManager _alertMgr;
    @Inject
    StorageManager _storageMgr;
    @Inject
    GuestOSDao _guestOSDao;
    @Inject
    GuestOSCategoryDao _guestOSCategoryDao;
    @Inject
    VirtualMachineManager _itMgr;
    @Inject
    AccountManager _accountMgr;
    @Inject
    ResourceManager _resourceMgr;
    @Inject
    ManagementServer _msServer;
    @Inject
    ConfigurationDao _configDao;
    @Inject
    VolumeOrchestrationService volumeMgr;

    String _instance;
    ScheduledExecutorService _executor;
    int _stopRetryInterval;
    int _investigateRetryInterval;
    int _migrateRetryInterval;
    int _restartRetryInterval;

    int _maxRetries;
    long _timeBetweenFailures;
    long _timeBetweenCleanups;
    boolean _forceHA;
    String _haTag = null;

    protected HighAvailabilityManagerImpl() {
    }

    @Override
    public Status investigate(final long hostId) {
        final HostVO host = _hostDao.findById(hostId);
        if (host == null) {
            return Status.Alert;
        }

        Status hostState = null;
        for (Investigator investigator : investigators) {
            hostState = investigator.isAgentAlive(host);
            if (hostState != null) {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug(investigator.getName() + " was able to determine host " + hostId + " is in " + hostState.toString());
                }
                return hostState;
            }
            if (s_logger.isDebugEnabled()) {
                s_logger.debug(investigator.getName() + " unable to determine the state of the host.  Moving on.");
            }
        }

        return hostState;
    }

    @Override
    public void scheduleRestartForVmsOnHost(final HostVO host, boolean investigate) {

        if (host.getType() != Host.Type.Routing) {
            return;
        }

        if (host.getHypervisorType() == HypervisorType.VMware || host.getHypervisorType() == HypervisorType.Hyperv) {
            s_logger.info("Don't restart VMs on host " + host.getId() + " as it is a " + host.getHypervisorType().toString() + " host");
            return;
        }

        s_logger.warn("Scheduling restart for VMs on host " + host.getId() + "-" + host.getName());

        final List<VMInstanceVO> vms = _instanceDao.listByHostId(host.getId());
        final DataCenterVO dcVO = _dcDao.findById(host.getDataCenterId());

        // send an email alert that the host is down
        StringBuilder sb = null;
        List<VMInstanceVO> reorderedVMList = new ArrayList<VMInstanceVO>();
        if ((vms != null) && !vms.isEmpty()) {
            sb = new StringBuilder();
            sb.append("  Starting HA on the following VMs:");
            // collect list of vm names for the alert email
            for (int i = 0; i < vms.size(); i++) {
                VMInstanceVO vm = vms.get(i);
                if (vm.getType() == VirtualMachine.Type.User) {
                    reorderedVMList.add(vm);
                } else {
                    reorderedVMList.add(0, vm);
                }
                if (vm.isHaEnabled()) {
                    sb.append(" " + vm.getHostName());
                }
            }
        }

        // send an email alert that the host is down, include VMs
        HostPodVO podVO = _podDao.findById(host.getPodId());
        String hostDesc = "name: " + host.getName() + " (id:" + host.getId() + "), availability zone: " + dcVO.getName() + ", pod: " + podVO.getName();
        _alertMgr.sendAlert(AlertManager.AlertType.ALERT_TYPE_HOST, host.getDataCenterId(), host.getPodId(), "Host is down, " + hostDesc,
                "Host [" + hostDesc + "] is down." + ((sb != null) ? sb.toString() : ""));

        for (VMInstanceVO vm : reorderedVMList) {
            ServiceOfferingVO vmOffering = _serviceOfferingDao.findById(vm.getServiceOfferingId());
            if (vmOffering.isUseLocalStorage()) {
                if (s_logger.isDebugEnabled()){
                    s_logger.debug("Skipping HA on vm " + vm + ", because it uses local storage. Its fate is tied to the host.");
                }
                continue;
            }
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Notifying HA Mgr of to restart vm " + vm.getId() + "-" + vm.getInstanceName());
            }
            vm = _instanceDao.findByUuid(vm.getUuid());
            Long hostId = vm.getHostId();
            if (hostId != null && !hostId.equals(host.getId())) {
                s_logger.debug("VM " + vm.getInstanceName() + " is not on down host " + host.getId() + " it is on other host "
                        + hostId + " VM HA is done");
                continue;
            }
            scheduleRestart(vm, investigate);
        }
    }

    @Override
    public void scheduleStop(VMInstanceVO vm, long hostId, WorkType type) {
        assert (type == WorkType.CheckStop || type == WorkType.ForceStop || type == WorkType.Stop);

        if (_haDao.hasBeenScheduled(vm.getId(), type)) {
            s_logger.info("There's already a job scheduled to stop " + vm);
            return;
        }

        HaWorkVO work = new HaWorkVO(vm.getId(), vm.getType(), type, Step.Scheduled, hostId, vm.getState(), 0, vm.getUpdated());
        _haDao.persist(work);
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Scheduled " + work);
        }
        wakeupWorkers();
    }

    protected void wakeupWorkers() {
        for (WorkerThread worker : _workers) {
            worker.wakup();
        }
    }

    @Override
    public boolean scheduleMigration(final VMInstanceVO vm) {
        if (vm.getHostId() != null) {
            final HaWorkVO work = new HaWorkVO(vm.getId(), vm.getType(), WorkType.Migration, Step.Scheduled, vm.getHostId(), vm.getState(), 0, vm.getUpdated());
            _haDao.persist(work);
            s_logger.info("Scheduled migration work of VM " + vm.getUuid() + " from host " + _hostDao.findById(vm.getHostId()) + " with HAWork " + work);
            wakeupWorkers();
        }
        return true;
    }

    @Override
    public void scheduleRestart(VMInstanceVO vm, boolean investigate) {
        Long hostId = vm.getHostId();
        if (hostId == null) {
            try {
                s_logger.debug("Found a vm that is scheduled to be restarted but has no host id: " + vm);
                _itMgr.advanceStop(vm.getUuid(), true);
            } catch (ResourceUnavailableException e) {
                assert false : "How do we hit this when force is true?";
                throw new CloudRuntimeException("Caught exception even though it should be handled.", e);
            } catch (OperationTimedoutException e) {
                assert false : "How do we hit this when force is true?";
                throw new CloudRuntimeException("Caught exception even though it should be handled.", e);
            } catch (ConcurrentOperationException e) {
                assert false : "How do we hit this when force is true?";
                throw new CloudRuntimeException("Caught exception even though it should be handled.", e);
            }
        }

        if (vm.getHypervisorType() == HypervisorType.VMware || vm.getHypervisorType() == HypervisorType.Hyperv) {
            s_logger.info("Skip HA for VMware VM or Hyperv VM" + vm.getInstanceName());
            return;
        }

        if (!investigate) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("VM does not require investigation so I'm marking it as Stopped: " + vm.toString());
            }

            AlertManager.AlertType alertType = AlertManager.AlertType.ALERT_TYPE_USERVM;
            if (VirtualMachine.Type.DomainRouter.equals(vm.getType())) {
                alertType = AlertManager.AlertType.ALERT_TYPE_DOMAIN_ROUTER;
            } else if (VirtualMachine.Type.ConsoleProxy.equals(vm.getType())) {
                alertType = AlertManager.AlertType.ALERT_TYPE_CONSOLE_PROXY;
            } else if (VirtualMachine.Type.SecondaryStorageVm.equals(vm.getType())) {
                alertType = AlertManager.AlertType.ALERT_TYPE_SSVM;
            }

            if (!(_forceHA || vm.isHaEnabled())) {
                String hostDesc = "id:" + vm.getHostId() + ", availability zone id:" + vm.getDataCenterId() + ", pod id:" + vm.getPodIdToDeployIn();
                _alertMgr.sendAlert(alertType, vm.getDataCenterId(), vm.getPodIdToDeployIn(), "VM (name: " + vm.getHostName() + ", id: " + vm.getId() +
                    ") stopped unexpectedly on host " + hostDesc, "Virtual Machine " + vm.getHostName() + " (id: " + vm.getId() + ") running on host [" + vm.getHostId() +
                    "] stopped unexpectedly.");

                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("VM is not HA enabled so we're done.");
                }
            }

            try {
                _itMgr.advanceStop(vm.getUuid(), true);
                vm = _instanceDao.findByUuid(vm.getUuid());
            } catch (ResourceUnavailableException e) {
                assert false : "How do we hit this when force is true?";
                throw new CloudRuntimeException("Caught exception even though it should be handled.", e);
            } catch (OperationTimedoutException e) {
                assert false : "How do we hit this when force is true?";
                throw new CloudRuntimeException("Caught exception even though it should be handled.", e);
            } catch (ConcurrentOperationException e) {
                assert false : "How do we hit this when force is true?";
                throw new CloudRuntimeException("Caught exception even though it should be handled.", e);
            }
        }

        if (vm.getHypervisorType() == HypervisorType.VMware) {
            s_logger.info("Skip HA for VMware VM " + vm.getInstanceName());
            return;
        }

        List<HaWorkVO> items = _haDao.findPreviousHA(vm.getId());
        int timesTried = 0;
        for (HaWorkVO item : items) {
            if (timesTried < item.getTimesTried() && !item.canScheduleNew(_timeBetweenFailures)) {
                timesTried = item.getTimesTried();
                break;
            }
        }

        if (hostId == null) {
            hostId = vm.getLastHostId();
        }

        HaWorkVO work = new HaWorkVO(vm.getId(), vm.getType(), WorkType.HA, investigate ? Step.Investigating : Step.Scheduled,
                hostId != null ? hostId : 0L, vm.getState(), timesTried, vm.getUpdated());
        _haDao.persist(work);

        if (s_logger.isInfoEnabled()) {
            s_logger.info("Schedule vm for HA:  " + vm);
        }

        wakeupWorkers();

    }

    protected Long restart(final HaWorkVO work) {
        List<HaWorkVO> items = _haDao.listFutureHaWorkForVm(work.getInstanceId(), work.getId());
        if (items.size() > 0) {
            StringBuilder str = new StringBuilder("Cancelling this work item because newer ones have been scheduled.  Work Ids = [");
            for (HaWorkVO item : items) {
                str.append(item.getId()).append(", ");
            }
            str.delete(str.length() - 2, str.length()).append("]");
            s_logger.info(str.toString());
            return null;
        }

        items = _haDao.listRunningHaWorkForVm(work.getInstanceId());
        if (items.size() > 0) {
            StringBuilder str = new StringBuilder("Waiting because there's HA work being executed on an item currently.  Work Ids =[");
            for (HaWorkVO item : items) {
                str.append(item.getId()).append(", ");
            }
            str.delete(str.length() - 2, str.length()).append("]");
            s_logger.info(str.toString());
            return (System.currentTimeMillis() >> 10) + _investigateRetryInterval;
        }

        long vmId = work.getInstanceId();

        VirtualMachine vm = _itMgr.findById(work.getInstanceId());
        if (vm == null) {
            s_logger.info("Unable to find vm: " + vmId);
            return null;
        }

        s_logger.info("HA on " + vm);
        if (vm.getState() != work.getPreviousState() || vm.getUpdated() != work.getUpdateTime()) {
            s_logger.info("VM " + vm + " has been changed.  Current State = " + vm.getState() + " Previous State = " + work.getPreviousState() + " last updated = " +
                vm.getUpdated() + " previous updated = " + work.getUpdateTime());
            return null;
        }

        AlertManager.AlertType alertType = AlertManager.AlertType.ALERT_TYPE_USERVM;
        if (VirtualMachine.Type.DomainRouter.equals(vm.getType())) {
            alertType = AlertManager.AlertType.ALERT_TYPE_DOMAIN_ROUTER;
        } else if (VirtualMachine.Type.ConsoleProxy.equals(vm.getType())) {
            alertType = AlertManager.AlertType.ALERT_TYPE_CONSOLE_PROXY;
        } else if (VirtualMachine.Type.SecondaryStorageVm.equals(vm.getType())) {
            alertType = AlertManager.AlertType.ALERT_TYPE_SSVM;
        }

        HostVO host = _hostDao.findById(work.getHostId());
        boolean isHostRemoved = false;
        if (host == null) {
            host = _hostDao.findByIdIncludingRemoved(work.getHostId());
            if (host != null) {
                s_logger.debug("VM " + vm.toString() + " is now no longer on host " + work.getHostId() + " as the host is removed");
                isHostRemoved = true;
            }
        }

        DataCenterVO dcVO = _dcDao.findById(host.getDataCenterId());
        HostPodVO podVO = _podDao.findById(host.getPodId());
        String hostDesc = "name: " + host.getName() + "(id:" + host.getId() + "), availability zone: " + dcVO.getName() + ", pod: " + podVO.getName();

        Boolean alive = null;
        if (work.getStep() == Step.Investigating) {
            if (!isHostRemoved) {
                if (vm.getHostId() == null || vm.getHostId() != work.getHostId()) {
                    s_logger.info("VM " + vm.toString() + " is now no longer on host " + work.getHostId());
                    return null;
                }

                Investigator investigator = null;
                for (Investigator it : investigators) {
                    investigator = it;
                    try
                    {
                        alive = investigator.isVmAlive(vm, host);
                        s_logger.info(investigator.getName() + " found " + vm + " to be alive? " + alive);
                        break;
                    } catch (UnknownVM e) {
                        s_logger.info(investigator.getName() + " could not find " + vm);
                    }
                }

                boolean fenced = false;
                if (alive == null) {
                    s_logger.debug("Fencing off VM that we don't know the state of");
                    for (FenceBuilder fb : fenceBuilders) {
                        Boolean result = fb.fenceOff(vm, host);
                        s_logger.info("Fencer " + fb.getName() + " returned " + result);
                        if (result != null && result) {
                            fenced = true;
                            break;
                        }
                    }

                } else if (!alive) {
                    fenced = true;
                } else {
                    s_logger.debug("VM " + vm.getInstanceName() + " is found to be alive by " + investigator.getName());
                    if (host.getStatus() == Status.Up) {
                        s_logger.info(vm + " is alive and host is up. No need to restart it.");
                        return null;
                    } else {
                        s_logger.debug("Rescheduling because the host is not up but the vm is alive");
                        return (System.currentTimeMillis() >> 10) + _investigateRetryInterval;
                    }
                }

                if (!fenced) {
                    s_logger.debug("We were unable to fence off the VM " + vm);
                    _alertMgr.sendAlert(alertType, vm.getDataCenterId(), vm.getPodIdToDeployIn(), "Unable to restart " + vm.getHostName() +
                        " which was running on host " + hostDesc, "Insufficient capacity to restart VM, name: " + vm.getHostName() + ", id: " + vmId +
                        " which was running on host " + hostDesc);
                    return (System.currentTimeMillis() >> 10) + _restartRetryInterval;
                }

                try {
                    _itMgr.advanceStop(vm.getUuid(), true);
                } catch (ResourceUnavailableException e) {
                    assert false : "How do we hit this when force is true?";
                    throw new CloudRuntimeException("Caught exception even though it should be handled.", e);
                } catch (OperationTimedoutException e) {
                    assert false : "How do we hit this when force is true?";
                    throw new CloudRuntimeException("Caught exception even though it should be handled.", e);
                } catch (ConcurrentOperationException e) {
                    assert false : "How do we hit this when force is true?";
                    throw new CloudRuntimeException("Caught exception even though it should be handled.", e);
                }

                work.setStep(Step.Scheduled);
                _haDao.update(work.getId(), work);
            } else {
                s_logger.debug("How come that HA step is Investigating and the host is removed? Calling forced Stop on Vm anyways");
                try {
                    _itMgr.advanceStop(vm.getUuid(), true);
                } catch (ResourceUnavailableException e) {
                    assert false : "How do we hit this when force is true?";
                    throw new CloudRuntimeException("Caught exception even though it should be handled.", e);
                } catch (OperationTimedoutException e) {
                    assert false : "How do we hit this when force is true?";
                    throw new CloudRuntimeException("Caught exception even though it should be handled.", e);
                } catch (ConcurrentOperationException e) {
                    assert false : "How do we hit this when force is true?";
                    throw new CloudRuntimeException("Caught exception even though it should be handled.", e);
                }
            }
        }

        vm = _itMgr.findById(vm.getId());

        if (!_forceHA && !vm.isHaEnabled()) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("VM is not HA enabled so we're done.");
            }
            return null; // VM doesn't require HA
        }

        if ((host == null || host.getRemoved() != null || host.getState() != Status.Up)
                 && !volumeMgr.canVmRestartOnAnotherServer(vm.getId())) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("VM can not restart on another server.");
            }
            return null;
        }

        try {
            HashMap<VirtualMachineProfile.Param, Object> params = new HashMap<VirtualMachineProfile.Param, Object>();
            if (_haTag != null) {
                params.put(VirtualMachineProfile.Param.HaTag, _haTag);
            }
            WorkType wt = work.getWorkType();
            if (wt.equals(WorkType.HA)) {
                params.put(VirtualMachineProfile.Param.HaOperation, true);
            }

            try{
                // First try starting the vm with its original planner, if it doesn't succeed send HAPlanner as its an emergency.
                _itMgr.advanceStart(vm.getUuid(), params, null);
            }catch (InsufficientCapacityException e){
                s_logger.warn("Failed to deploy vm " + vmId + " with original planner, sending HAPlanner");
                _itMgr.advanceStart(vm.getUuid(), params, _haPlanners.get(0));
            }

            VMInstanceVO started = _instanceDao.findById(vm.getId());
            if (started != null && started.getState() == VirtualMachine.State.Running) {
                s_logger.info("VM is now restarted: " + vmId + " on " + started.getHostId());
                return null;
            }

            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Rescheduling VM " + vm.toString() + " to try again in " + _restartRetryInterval);
            }
        } catch (final InsufficientCapacityException e) {
            s_logger.warn("Unable to restart " + vm.toString() + " due to " + e.getMessage());
            _alertMgr.sendAlert(alertType, vm.getDataCenterId(), vm.getPodIdToDeployIn(), "Unable to restart " + vm.getHostName() + " which was running on host " +
                hostDesc, "Insufficient capacity to restart VM, name: " + vm.getHostName() + ", id: " + vmId + " which was running on host " + hostDesc);
        } catch (final ResourceUnavailableException e) {
            s_logger.warn("Unable to restart " + vm.toString() + " due to " + e.getMessage());
            _alertMgr.sendAlert(alertType, vm.getDataCenterId(), vm.getPodIdToDeployIn(), "Unable to restart " + vm.getHostName() + " which was running on host " +
                hostDesc, "The Storage is unavailable for trying to restart VM, name: " + vm.getHostName() + ", id: " + vmId + " which was running on host " + hostDesc);
        } catch (ConcurrentOperationException e) {
            s_logger.warn("Unable to restart " + vm.toString() + " due to " + e.getMessage());
            _alertMgr.sendAlert(alertType, vm.getDataCenterId(), vm.getPodIdToDeployIn(), "Unable to restart " + vm.getHostName() + " which was running on host " +
                hostDesc, "The Storage is unavailable for trying to restart VM, name: " + vm.getHostName() + ", id: " + vmId + " which was running on host " + hostDesc);
        } catch (OperationTimedoutException e) {
            s_logger.warn("Unable to restart " + vm.toString() + " due to " + e.getMessage());
            _alertMgr.sendAlert(alertType, vm.getDataCenterId(), vm.getPodIdToDeployIn(), "Unable to restart " + vm.getHostName() + " which was running on host " +
                hostDesc, "The Storage is unavailable for trying to restart VM, name: " + vm.getHostName() + ", id: " + vmId + " which was running on host " + hostDesc);
        }
        vm = _itMgr.findById(vm.getId());
        work.setUpdateTime(vm.getUpdated());
        work.setPreviousState(vm.getState());
        return (System.currentTimeMillis() >> 10) + _restartRetryInterval;
    }

    public Long migrate(final HaWorkVO work) {
        long vmId = work.getInstanceId();
        long srcHostId = work.getHostId();

        VMInstanceVO vm = _instanceDao.findById(vmId);
        if (vm == null) {
            s_logger.info("Unable to find vm: " + vmId + ", skipping migrate.");
            return null;
        }
        s_logger.info("Migration attempt: for VM " + vm.getUuid() + "from host id " + srcHostId +
                ". Starting attempt: " + (1 + work.getTimesTried()) + "/" + _maxRetries + " times.");
        try {
            work.setStep(Step.Migrating);
            _haDao.update(work.getId(), work);

            // First try starting the vm with its original planner, if it doesn't succeed send HAPlanner as its an emergency.
            _itMgr.migrateAway(vm.getUuid(), srcHostId);
            return null;
        } catch (InsufficientServerCapacityException e) {
            s_logger.warn("Migration attempt: Insufficient capacity for migrating a VM " +
                    vm.getUuid() + " from source host id " + srcHostId +
                    ". Exception: " + e.getMessage());
            _resourceMgr.migrateAwayFailed(srcHostId, vmId);
            return (System.currentTimeMillis() >> 10) + _migrateRetryInterval;
        } catch (Exception e) {
            s_logger.warn("Migration attempt: Unexpected exception occurred when attempting migration of " +
                    vm.getUuid() + e.getMessage());
            throw e;
        }
    }

    @Override
    public void scheduleDestroy(VMInstanceVO vm, long hostId) {
        final HaWorkVO work = new HaWorkVO(vm.getId(), vm.getType(), WorkType.Destroy, Step.Scheduled, hostId, vm.getState(), 0, vm.getUpdated());
        _haDao.persist(work);
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Scheduled " + work.toString());
        }
        wakeupWorkers();
    }

    @Override
    public void cancelDestroy(VMInstanceVO vm, Long hostId) {
        _haDao.delete(vm.getId(), WorkType.Destroy);
    }

    protected Long destroyVM(final HaWorkVO work) {
        final VirtualMachine vm = _itMgr.findById(work.getInstanceId());
        s_logger.info("Destroying " + vm.toString());
        try {
            if (vm.getState() != State.Destroyed) {
                s_logger.info("VM is no longer in Destroyed state " + vm.toString());
                return null;
            }

            if (vm.getHostId() != null) {
                _itMgr.destroy(vm.getUuid(), false);
                s_logger.info("Successfully destroy " + vm);
                return null;
            } else {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug(vm + " has already been stopped");
                }
                return null;
            }
        } catch (final AgentUnavailableException e) {
            s_logger.debug("Agnet is not available" + e.getMessage());
        } catch (OperationTimedoutException e) {
            s_logger.debug("operation timed out: " + e.getMessage());
        } catch (ConcurrentOperationException e) {
            s_logger.debug("concurrent operation: " + e.getMessage());
        }

        return (System.currentTimeMillis() >> 10) + _stopRetryInterval;
    }

    protected Long stopVM(final HaWorkVO work) throws ConcurrentOperationException {
        VirtualMachine vm = _itMgr.findById(work.getInstanceId());
        if (vm == null) {
            s_logger.info("No longer can find VM " + work.getInstanceId() + ". Throwing away " + work);
            work.setStep(Step.Done);
            return null;
        }
        s_logger.info("Stopping " + vm);
        try {
            if (work.getWorkType() == WorkType.Stop) {
                _itMgr.advanceStop(vm.getUuid(), false);
                s_logger.info("Successfully stopped " + vm);
                return null;
            } else if (work.getWorkType() == WorkType.CheckStop) {
                if ((vm.getState() != work.getPreviousState()) || vm.getUpdated() != work.getUpdateTime() || vm.getHostId() == null ||
                    vm.getHostId().longValue() != work.getHostId()) {
                    s_logger.info(vm + " is different now.  Scheduled Host: " + work.getHostId() + " Current Host: " +
                        (vm.getHostId() != null ? vm.getHostId() : "none") + " State: " + vm.getState());
                    return null;
                }

                _itMgr.advanceStop(vm.getUuid(), false);
                s_logger.info("Stop for " + vm + " was successful");
                return null;
            } else if (work.getWorkType() == WorkType.ForceStop) {
                if ((vm.getState() != work.getPreviousState()) || vm.getUpdated() != work.getUpdateTime() || vm.getHostId() == null ||
                    vm.getHostId().longValue() != work.getHostId()) {
                    s_logger.info(vm + " is different now.  Scheduled Host: " + work.getHostId() + " Current Host: " +
                        (vm.getHostId() != null ? vm.getHostId() : "none") + " State: " + vm.getState());
                    return null;
                }

                _itMgr.advanceStop(vm.getUuid(), true);
                s_logger.info("Stop for " + vm + " was successful");
                return null;
            } else {
                assert false : "Who decided there's other steps but didn't modify the guy who does the work?";
            }
        } catch (final ResourceUnavailableException e) {
            s_logger.debug("Agnet is not available" + e.getMessage());
        } catch (OperationTimedoutException e) {
            s_logger.debug("operation timed out: " + e.getMessage());
        }

        return (System.currentTimeMillis() >> 10) + _stopRetryInterval;
    }

    @Override
    public void cancelScheduledMigrations(final HostVO host) {
        WorkType type = host.getType() == HostVO.Type.Storage ? WorkType.Stop : WorkType.Migration;
        s_logger.info("Canceling all scheduled migrations from host " + host.getUuid());
        _haDao.deleteMigrationWorkItems(host.getId(), type, _serverId);
    }

    @Override
    public List<VMInstanceVO> findTakenMigrationWork() {
        List<HaWorkVO> works = _haDao.findTakenWorkItems(WorkType.Migration);
        List<VMInstanceVO> vms = new ArrayList<VMInstanceVO>(works.size());
        for (HaWorkVO work : works) {
            VMInstanceVO vm = _instanceDao.findById(work.getInstanceId());
            if (vm != null) {
                vms.add(vm);
            }
        }
        return vms;
    }

    private void rescheduleWork(final HaWorkVO work, final long nextTime) {
        work.setTimeToTry(nextTime);
        work.setTimesTried(work.getTimesTried() + 1);
        work.setServerId(null);
        work.setDateTaken(null);
    }

    private long getRescheduleTime(WorkType workType) {
        switch (workType) {
            case Migration:
                return ((System.currentTimeMillis() >> 10) + _migrateRetryInterval);
            case HA:
                return ((System.currentTimeMillis() >> 10) + _restartRetryInterval);
            case Stop:
            case CheckStop:
            case ForceStop:
                return ((System.currentTimeMillis() >> 10) + _stopRetryInterval);
            case Destroy:
                return ((System.currentTimeMillis() >> 10) + _restartRetryInterval);
        }
        return 0;
    }

    private void processWork(final HaWorkVO work) {
        final WorkType wt = work.getWorkType();
        try {
            Long nextTime = null;
            if (wt == WorkType.Migration) {
                nextTime = migrate(work);
            } else if (wt == WorkType.HA) {
                nextTime = restart(work);
            } else if (wt == WorkType.Stop || wt == WorkType.CheckStop || wt == WorkType.ForceStop) {
                nextTime = stopVM(work);
            } else if (wt == WorkType.Destroy) {
                nextTime = destroyVM(work);
            } else {
                assert false : "How did we get here with " + wt.toString();
                return;
            }

            if (nextTime == null) {
                s_logger.info("Completed work " + work + ". Took " + (work.getTimesTried() + 1) + "/" + _maxRetries + " attempts.");
                work.setStep(Step.Done);
            } else {
                rescheduleWork(work, nextTime.longValue());
            }
        } catch (Exception e) {
            s_logger.warn("Encountered unhandled exception during HA process, reschedule work", e);

            long nextTime = getRescheduleTime(wt);
            rescheduleWork(work, nextTime);

            // if restart failed in the middle due to exception, VM state may has been changed
            // recapture into the HA worker so that it can really continue in it next turn
            VMInstanceVO vm = _instanceDao.findById(work.getInstanceId());
            work.setUpdateTime(vm.getUpdated());
            work.setPreviousState(vm.getState());
        } finally {
            if (!Step.Done.equals(work.getStep())) {
                if (work.getTimesTried() >= _maxRetries) {
                    s_logger.warn("Giving up, retried max " + work.getTimesTried() + "/" + _maxRetries + " times for work: " + work);
                    work.setStep(Step.Done);
                } else {
                    s_logger.warn("Rescheduling work " + work + " to try again at " + new Date(work.getTimeToTry() << 10) +
                            ". Finished attempt " + work.getTimesTried() + "/" + _maxRetries + " times.");
                }
            }
            _haDao.update(work.getId(), work);
        }
    }

    @Override
    public boolean configure(final String name, final Map<String, Object> xmlParams) throws ConfigurationException {
        _serverId = _msServer.getId();

        final Map<String, String> params = _configDao.getConfiguration(Long.toHexString(_serverId),
            xmlParams);

        final int count = HAWorkers.value();
        _workers = new WorkerThread[count];
        for (int i = 0; i < _workers.length; i++) {
            _workers[i] = new WorkerThread("HA-Worker-" + i);
        }

        _forceHA = ForceHA.value();
        _timeToSleep = TimeToSleep.value() * SECONDS_TO_MILLISECONDS_FACTOR;
        _maxRetries = MaxRetries.value();
        _timeBetweenFailures = TimeBetweenFailures.value() * SECONDS_TO_MILLISECONDS_FACTOR;
        _timeBetweenCleanups = TimeBetweenCleanup.value();
        _stopRetryInterval = StopRetryInterval.value();
        _restartRetryInterval = RestartRetryInterval.value();
        _investigateRetryInterval = InvestigateRetryInterval.value();
        _migrateRetryInterval = MigrateRetryInterval.value();

        _instance = params.get("instance");
        if (_instance == null) {
            _instance = "VMOPS";
        }

        _haTag = params.get("ha.tag");

        _haDao.releaseWorkItems(_serverId);

        _stopped = true;

        _executor = Executors.newScheduledThreadPool(count, new NamedThreadFactory("HA"));

        return true;
    }

    @Override
    public boolean start() {
        _stopped = false;

        for (final WorkerThread thread : _workers) {
            thread.start();
        }

        _executor.scheduleAtFixedRate(new CleanupTask(), _timeBetweenCleanups, _timeBetweenCleanups, TimeUnit.SECONDS);

        return true;
    }

    @Override
    public boolean stop() {
        _stopped = true;

        wakeupWorkers();

        _executor.shutdown();

        return true;
    }

    protected class CleanupTask extends ManagedContextRunnable {
        @Override
        protected void runInContext() {
            s_logger.info("HA Cleanup Thread Running");

            try {
                _haDao.cleanup(System.currentTimeMillis() - _timeBetweenFailures);
            } catch (Exception e) {
                s_logger.warn("Error while cleaning up", e);
            }
        }
    }

    protected class WorkerThread extends Thread {
        public WorkerThread(String name) {
            super(name);
        }

        @Override
        public void run() {
            s_logger.info("Starting work");
            while (!_stopped) {
                _managedContext.runWithContext(new Runnable() {
                    @Override
                    public void run() {
                        runWithContext();
                    }
                });
            }
            s_logger.info("Time to go home!");
        }

        private void runWithContext() {
            HaWorkVO work = null;
            try {
                s_logger.trace("Checking the database for work");
                work = _haDao.take(_serverId);
                if (work == null) {
                    try {
                        synchronized (this) {
                            wait(_timeToSleep);
                        }
                        return;
                    } catch (final InterruptedException e) {
                        s_logger.info("Interrupted");
                        return;
                    }
                }

                NDC.push("work-" + work.getId());
                s_logger.info("Processing work " + work);
                processWork(work);
            } catch (final Throwable th) {
                s_logger.error("Caught this throwable, ", th);
            } finally {
                if (work != null) {
                    NDC.pop();
                }
            }
        }

        public synchronized void wakup() {
            notifyAll();
        }
    }

    @Override
    public void onManagementNodeJoined(List<? extends ManagementServerHost> nodeList, long selfNodeId) {
    }

    @Override
    public void onManagementNodeLeft(List<? extends ManagementServerHost> nodeList, long selfNodeId) {
        for (ManagementServerHost node : nodeList) {
            _haDao.releaseWorkItems(node.getMsid());
        }
    }

    @Override
    public void onManagementNodeIsolated() {
    }

    @Override
    public String getHaTag() {
        return _haTag;
    }

    @Override
    public DeploymentPlanner getHAPlanner() {
        return _haPlanners.get(0);
    }

    @Override
    public boolean hasPendingHaWork(long vmId) {
        List<HaWorkVO> haWorks = _haDao.listPendingHaWorkForVm(vmId);
        return haWorks.size() > 0;
    }

    @Override
    public boolean hasPendingMigrationsWork(long vmId) {
        List<HaWorkVO> haWorks = _haDao.listPendingMigrationsForVm(vmId);
        for (HaWorkVO work : haWorks) {
            if (work.getTimesTried() <= _maxRetries) {
                return true;
            } else {
                s_logger.warn("HAWork Job of migration type " + work + " found in database which has max " +
                        "retries more than " + _maxRetries + " but still not in Done, Cancelled, or Error State");
            }
        }
        return false;
    }

    /**
     * @return The name of the component that provided this configuration
     * variable.  This value is saved in the database so someone can easily
     * identify who provides this variable.
     **/
    @Override
    public String getConfigComponentName() {
        return HighAvailabilityManager.class.getSimpleName();
    }

    /**
     * @return The list of config keys provided by this configuable.
     */
    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey[] {TimeBetweenCleanup, MaxRetries, TimeToSleep, TimeBetweenFailures,
            StopRetryInterval, RestartRetryInterval, MigrateRetryInterval, InvestigateRetryInterval,
            HAWorkers, ForceHA};
    }
}
