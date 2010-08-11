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
package com.cloud.ha;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.ejb.Local;
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.MigrateCommand;
import com.cloud.alert.AlertManager;
import com.cloud.configuration.dao.ConfigurationDao;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.HostPodVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.HostPodDao;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.exception.StorageUnavailableException;
import com.cloud.ha.WorkVO.WorkType;
import com.cloud.ha.dao.HighAvailabilityDao;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.Status;
import com.cloud.host.dao.HostDao;
import com.cloud.maid.StackMaid;
import com.cloud.server.ManagementServer;
import com.cloud.storage.StorageManager;
import com.cloud.storage.dao.GuestOSCategoryDao;
import com.cloud.storage.dao.GuestOSDao;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.component.Adapters;
import com.cloud.utils.component.ComponentLocator;
import com.cloud.utils.component.Inject;
import com.cloud.utils.concurrency.NamedThreadFactory;
import com.cloud.utils.db.GlobalLock;
import com.cloud.utils.exception.ExecutionException;
import com.cloud.vm.State;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineManager;
import com.cloud.vm.VirtualMachine.Event;
import com.cloud.vm.dao.VMInstanceDao;

/**
 * HighAvailabilityManagerImpl coordinates the HA process.  VMs are registered with
 * the HA Coordinator for HA.  The request is stored within a database backed
 * work queue.  HACoordinator then also has a number of workers that pick up
 * these work items to perform HA on the VMs.
 *
 * The HA process goes as follows:
 *   1. Check with the list of Investigators to determine that the VM is
 *      no longer running.  If a Investigator finds the VM is still alive,
 *      the HA process is stopped and the state of the VM reverts back to
 *      its previous state.  If a Investigator finds the VM is dead, then
 *      HA process is started on the VM, skipping step 2.
 *   2. If the list of Investigators can not determine if the VM is dead or
 *      alive.  The list of FenceBuilders is invoked to fence off the VM
 *      so that it won't do any damage to the storage and network.
 *   3. The VM is marked as stopped.
 *   4. The VM is started again via the normal process of starting VMs.  Note
 *      that once the VM is marked as stopped, the user may have started the
 *      VM himself.
 *   5. VMs that have re-started more than the configured number of times are
 *      marked as in Error state and the user is not allowed to restart
 *      the VM.
 *
 * @config
 * {@table
 *    || Param Name | Description | Values | Default ||
 *    || workers | number of worker threads to spin off to do the processing | int | 1 ||
 *    || time.to.sleep | Time to sleep if no work items are found | seconds | 60 ||
 *    || max.retries | number of times to retry start | int | 5 ||
 *    || time.between.failure | Time elapsed between failures before we consider it as another retry | seconds | 3600 ||
 *    || time.between.cleanup | Time to wait before the cleanup thread runs | seconds | 86400 ||
 *    || force.ha | Force HA to happen even if the VM says no | boolean | false ||
 *    || ha.retry.wait | time to wait before retrying the work item | seconds | 120 ||
 *    || stop.retry.wait | time to wait before retrying the stop | seconds | 120 ||
 *  }
 **/
@SuppressWarnings("unchecked")
@Local(value={HighAvailabilityManager.class})
public class HighAvailabilityManagerImpl implements HighAvailabilityManager {
    protected static final Logger s_logger = Logger.getLogger(HighAvailabilityManagerImpl.class);
    String _name;
    WorkerThread[] _workers;
    boolean _stopped;
    long _timeToSleep;
    @Inject HighAvailabilityDao _haDao;
    @Inject VMInstanceDao _instanceDao;
    @Inject HostDao _hostDao;
    @Inject DataCenterDao _dcDao;
    @Inject HostPodDao _podDao;
    long _serverId;
    Adapters<Investigator> _investigators;
    Adapters<FenceBuilder> _fenceBuilders;
    @Inject AgentManager _agentMgr;
    @Inject AlertManager _alertMgr;
    @Inject StorageManager _storageMgr;
    @Inject GuestOSDao _guestOSDao;
    @Inject GuestOSCategoryDao _guestOSCategoryDao;
    
    String _instance;
    ScheduledExecutorService _executor;
    int _operationTimeout;
    int _stopRetryInterval;
    int _investigateRetryInterval;
    int _migrateRetryInterval;
    int _restartRetryInterval;

    HashMap<VirtualMachine.Type, VirtualMachineManager<VMInstanceVO>> _handlers;

    int _maxRetries;
    long _timeBetweenFailures;
    long _timeBetweenCleanups;
    boolean _forceHA;

    protected HighAvailabilityManagerImpl() {
        _handlers = new HashMap<VirtualMachine.Type, VirtualMachineManager<VMInstanceVO>>(11);
    }

    @Override
    public Status investigate(final long hostId) {
        final HostVO host = _hostDao.findById(hostId);
        if (host == null) {
            return null;
        }

        final Enumeration<Investigator> en = _investigators.enumeration();
        Status hostState = null;
        Investigator investigator = null;
        while (en.hasMoreElements()) {
            investigator = en.nextElement();
            hostState = investigator.isAgentAlive(host);
            if (hostState != null) {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug(investigator.getName()+ " was able to determine host " + hostId + " is in " + hostState.toString());
                }
                return hostState;
            }
            if (s_logger.isDebugEnabled()) {
                s_logger.debug(investigator.getName() + " unable to determine the state of the host.  Moving on.");
            }
        }

        return null;
    }


    @Override
    public void scheduleRestartForVmsOnHost(final HostVO host) {
        
        if( host.getType() != Host.Type.Routing) {
            return;
        }
        s_logger.warn("Scheduling restart for VMs on host " + host.getId());

        final List<VMInstanceVO> vms = _instanceDao.listByHostId(host.getId());
        final DataCenterVO dcVO = _dcDao.findById(host.getDataCenterId());

        // send an email alert that the host is down
        StringBuilder sb = null;
        if ((vms != null) && !vms.isEmpty()) {
            sb = new StringBuilder();
            sb.append("  Starting HA on the following VMs: ");
            // collect list of vm names for the alert email
            VMInstanceVO vm = vms.get(0);
            if (vm.isHaEnabled()) {
                sb.append(" " + vm.getName());
            }
            for (int i = 1; i < vms.size(); i++) {
                vm = vms.get(i);
                if (vm.isHaEnabled()) {
                    sb.append(" " + vm.getName());
                }
            }
        }

        // send an email alert that the host is down, include VMs
        HostPodVO podVO = _podDao.findById(host.getPodId());
        String hostDesc = "name: " + host.getName() + " (id:" + host.getId() + "), availability zone: " + dcVO.getName() + ", pod: " + podVO.getName();

        _alertMgr.sendAlert(AlertManager.ALERT_TYPE_HOST, host.getDataCenterId(), host.getPodId(), "Host is down, " + hostDesc, "Host [" + hostDesc + "] is down."  + ((sb != null) ? sb.toString() : ""));

        for (final VMInstanceVO vm : vms) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Notifying HA Mgr of to investigate vm " + vm.getId() + "-" + vm.getName());
            }
            scheduleRestart(vm, true);
        }
    }

    @Override
    public void scheduleStop(final VMInstanceVO vm, long hostId, boolean verifyHost) {
    	
    	if (_haDao.hasBeenScheduled(vm.getId(), verifyHost ? WorkType.CheckStop : WorkType.Stop)) {
    		s_logger.info("There's already a job scheduled to stop " + vm.toString());
    		return;
    	}
    	
        final WorkVO work = new WorkVO(vm.getId(), vm.getType(), verifyHost ? WorkType.CheckStop : WorkType.Stop, Step.Scheduled, hostId, vm.getState(), 0, vm.getUpdated());
        _haDao.persist(work);
        if (s_logger.isDebugEnabled()) {
        	s_logger.debug("Scheduled " + work.toString() + " verifyHost = " + verifyHost);
        }
        wakeupWorkers();
    }

    @Override
    public synchronized void registerHandler(final VirtualMachine.Type type, final VirtualMachineManager<? extends VMInstanceVO> handler) {
        s_logger.info("Registering " + handler.getClass().getSimpleName() + " as the handler for " + type);
        _handlers.put(type, (VirtualMachineManager<VMInstanceVO>)handler);
    }

    @Override
    public synchronized void unregisterHandler(final VirtualMachine.Type type) {
        _handlers.remove(type);
    }
    
    protected void wakeupWorkers() {
    	for (WorkerThread worker : _workers) {
    		worker.wakup();
    	}
    }

    @Override
    public boolean scheduleMigration(final VMInstanceVO vm) {
        final WorkVO work = new WorkVO(vm.getId(), vm.getType(), WorkType.Migration, Step.Scheduled, vm.getHostId(), vm.getState(), 0, vm.getUpdated());
        _haDao.persist(work);
        wakeupWorkers();
        return true;
    }

    @Override
    public void scheduleRestart(VMInstanceVO vm, final boolean investigate) {
    	Long hostId = vm.getHostId();
    	VirtualMachineManager<VMInstanceVO> mgr = findManager(vm.getType());
    	vm = mgr.get(vm.getId());
        if (!investigate) {
        	if (s_logger.isDebugEnabled()) {
        		s_logger.debug("VM does not require investigation so I'm marking it as Stopped: " + vm.toString());
        	}

            short alertType = AlertManager.ALERT_TYPE_USERVM;
            if (VirtualMachine.Type.DomainRouter.equals(vm.getType())) {
                alertType = AlertManager.ALERT_TYPE_DOMAIN_ROUTER;
            } else if (VirtualMachine.Type.ConsoleProxy.equals(vm.getType())) {
                alertType = AlertManager.ALERT_TYPE_CONSOLE_PROXY;
            }

            if (!(_forceHA || vm.isHaEnabled())) {
            	
                _alertMgr.sendAlert(alertType, vm.getDataCenterId(), vm.getPodId(), "VM (name: "
                		+ vm.getName() + ", id: " + vm.getId() + ") stopped unexpectedly on host "
                		+ vm.getHostId(), "Virtual Machine " + vm.getName() + " (id: "
                		+ vm.getId() + ") running on host [" + vm.getHostId()
                		+ "] stopped unexpectedly.");
                		
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("VM is not HA enabled so we're done.");
                }
            }

        	mgr.completeStopCommand(vm);
        }

        final List<WorkVO> items = _haDao.findPreviousHA(vm.getId());
        int maxRetries = 0;
        for (final WorkVO item : items) {
            if (maxRetries < item.getTimesTried() && !item.canScheduleNew(_timeBetweenFailures)) {
                maxRetries = item.getTimesTried();
                break;
            }
        }

        final WorkVO work = new WorkVO(vm.getId(), vm.getType(), WorkType.HA, investigate ? Step.Investigating : Step.Scheduled, hostId, vm.getState(),
                maxRetries + 1, vm.getUpdated());
        _haDao.persist(work);

        if (s_logger.isInfoEnabled()) {
            s_logger.info("Schedule vm for HA:  " + vm.toString());
        }

        wakeupWorkers();
    }

    protected VirtualMachineManager<VMInstanceVO> findManager(final VirtualMachine.Type type) {
        return _handlers.get(type);
    }

    protected Long restart(final WorkVO work) {
        final long vmId = work.getInstanceId();

        final VirtualMachineManager<VMInstanceVO> mgr = findManager(work.getType());
        if (mgr == null) {
            s_logger.warn("Unable to find a handler for " + work.getType().toString() + ", throwing out " + vmId);
            return null;
        }

        VMInstanceVO vm = mgr.get(vmId);
        if (vm == null) {
            s_logger.info("Unable to find vm: " + vmId);
            return null;
        }

        s_logger.info("HA on " + vm.toString());
        if (vm.getState() != work.getPreviousState() || vm.getUpdated() != work.getUpdateTime()) {
        	s_logger.info("VM " + vm.toString() + " has been changed.  Current State = " + vm.getState() + " Previous State = " + work.getPreviousState() + " last updated = " + vm.getUpdated() + " previous updated = " + work.getUpdateTime());
        	return null;
        }

        final HostVO host = _hostDao.findById(work.getHostId());

        DataCenterVO dcVO = _dcDao.findById(host.getDataCenterId());
        HostPodVO podVO = _podDao.findById(host.getPodId());
        String hostDesc = "name: " + host.getName() + "(id:" + host.getId() + "), availability zone: " + dcVO.getName() + ", pod: " + podVO.getName();

        short alertType = AlertManager.ALERT_TYPE_USERVM;
        if (VirtualMachine.Type.DomainRouter.equals(vm.getType())) {
            alertType = AlertManager.ALERT_TYPE_DOMAIN_ROUTER;
        } else if (VirtualMachine.Type.ConsoleProxy.equals(vm.getType())) {
            alertType = AlertManager.ALERT_TYPE_CONSOLE_PROXY;
        }

        Boolean alive = null;
        if (work.getStep() == Step.Investigating) {
            if (vm.getHostId() == null || vm.getHostId() != work.getHostId()) {
                s_logger.info("VM " + vm.toString() + " is now no longer on host " + work.getHostId());
                if (vm.getState() == State.Starting && vm.getUpdated() == work.getUpdateTime()) {
                	_instanceDao.updateIf(vm, Event.AgentReportStopped, null);
                }
                return null;
            }
            
        	Enumeration<Investigator> en = _investigators.enumeration();
            Investigator investigator = null;
            while (en.hasMoreElements()) {
                investigator = en.nextElement();
                alive = investigator.isVmAlive(vm, host);
                if (alive != null) {
                    s_logger.debug(investigator.getName() + " found VM " + vm.getName() + "to be alive? " + alive);
                    break;
                }
            }
            if (alive != null && alive) {
                s_logger.debug("VM " + vm.getName() + " is found to be alive by " + investigator.getName());
                if (host.getStatus() == Status.Up) {
                	compareState(vm, new AgentVmInfo(vm.getInstanceName(), mgr, State.Running), false);
                	return null;
                } else {
                    s_logger.debug("Rescheduling because the host is not up but the vm is alive");
                    return (System.currentTimeMillis() >> 10) + _investigateRetryInterval;
                }
            }
            
            boolean fenced = false;
            if (alive == null || !alive) {
                fenced = true;
                s_logger.debug("Fencing off VM that we don't know the state of");
                Enumeration<FenceBuilder> enfb = _fenceBuilders.enumeration();
                while (enfb.hasMoreElements()) {
                    final FenceBuilder fb = enfb.nextElement();
                    Boolean result = fb.fenceOff(vm, host);
                    if (result != null && !result) {
                    	fenced = false;
                    }
                }
            }
            
            if (alive== null && !fenced) {
            	s_logger.debug("We were unable to fence off the VM " + vm.toString());
                _alertMgr.sendAlert(alertType, vm.getDataCenterId(), vm.getPodId(), "Unable to restart " + vm.getName() + " which was running on host " + hostDesc, "Insufficient capcity to restart VM, name: " + vm.getName() + ", id: " + vmId + " which was running on host " + hostDesc);
            	return (System.currentTimeMillis() >> 10) + _restartRetryInterval;
            }

            mgr.completeStopCommand(vm);
            
            work.setStep(Step.Scheduled);
            _haDao.update(work.getId(), work);
        }

        // send an alert for VMs that stop unexpectedly
        _alertMgr.sendAlert(alertType, vm.getDataCenterId(), vm.getPodId(),
        		"VM (name: " + vm.getName() + ", id: " + vmId + ") stopped unexpectedly on host "
        		+ hostDesc, "Virtual Machine " + vm.getName() + " (id: "
        		+ vm.getId() + ") running on host [" + hostDesc + "] stopped unexpectedly.");
        
        vm = mgr.get(vm.getId());

        if (!_forceHA && !vm.isHaEnabled()) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("VM is not HA enabled so we're done.");
            }
            return null;  // VM doesn't require HA
        }
        
        if (!_storageMgr.canVmRestartOnAnotherServer(vm.getId())) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("VM can not restart on another server.");
            }
            return null;
        }

        if (work.getTimesTried() > _maxRetries) {
            s_logger.warn("Retried to max times so deleting: " + vmId);
            return null;
        }

        try {
            VMInstanceVO started = mgr.start(vm.getId(), 0);
            if (started != null) {
                s_logger.info("VM is now restarted: " + vmId + " on " + started.getHostId());
                return null;
            }

            if (s_logger.isDebugEnabled()) {
            	s_logger.debug("Rescheduling VM " + vm.toString() + " to try again in " + _restartRetryInterval);
            }
            vm = mgr.get(vm.getId());
            work.setUpdateTime(vm.getUpdated());
            work.setPreviousState(vm.getState());
            return (System.currentTimeMillis() >> 10) + _restartRetryInterval;
        } catch (final InsufficientCapacityException e) {
        	s_logger.warn("Unable to restart " + vm.toString() + " due to " + e.getMessage());
            _alertMgr.sendAlert(alertType, vm.getDataCenterId(), vm.getPodId(), "Unable to restart " + vm.getName() + " which was running on host " + hostDesc, "Insufficient capcity to restart VM, name: " + vm.getName() + ", id: " + vmId + " which was running on host " + hostDesc);
            return null;
        } catch (final StorageUnavailableException e) {
        	s_logger.warn("Unable to restart " + vm.toString() + " due to " + e.getMessage());
            _alertMgr.sendAlert(alertType, vm.getDataCenterId(), vm.getPodId(), "Unable to restart " + vm.getName() + " which was running on host " + hostDesc, "The Storage is unavailable for trying to restart VM, name: " + vm.getName() + ", id: " + vmId + " which was running on host " + hostDesc);
            return null;
        } catch (ConcurrentOperationException e) {
        	s_logger.warn("Unable to restart " + vm.toString() + " due to " + e.getMessage());
            _alertMgr.sendAlert(alertType, vm.getDataCenterId(), vm.getPodId(), "Unable to restart " + vm.getName() + " which was running on host " + hostDesc, "The Storage is unavailable for trying to restart VM, name: " + vm.getName() + ", id: " + vmId + " which was running on host " + hostDesc);
            return null;
        } catch (ExecutionException e) {
           	s_logger.warn("Unable to restart " + vm.toString() + " due to " + e.getMessage());
            _alertMgr.sendAlert(alertType, vm.getDataCenterId(), vm.getPodId(), "Unable to restart " + vm.getName() + " which was running on host " + hostDesc, "The Storage is unavailable for trying to restart VM, name: " + vm.getName() + ", id: " + vmId + " which was running on host " + hostDesc);
            return null;
        }
    }


    /**
     * compareState does as its name suggests and compares the states between
     * management server and agent.  It returns whether something should be
     * cleaned up
     *
     */
    protected Command compareState(VMInstanceVO vm, final AgentVmInfo info, final boolean fullSync) {
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
            _instanceDao.updateIf(vm, agentState == State.Stopped ? VirtualMachine.Event.AgentReportStopped : VirtualMachine.Event.AgentReportRunning, vm.getHostId());
            if (agentState == State.Stopped) {
                s_logger.debug("State matches but the agent said stopped so let's send a cleanup anyways.");
                return info.mgr.cleanup(vm, agentName);
            }
            return null;
        }
        
        if (agentState == State.Stopped) {
            // This state means the VM on the agent was detected previously
            // and now is gone.  This is slightly different than if the VM
            // was never completed but we still send down a Stop Command
            // to ensure there's cleanup.
            if (serverState == State.Running ) {
                // Our records showed that it should be running so let's restart it.
                vm = info.mgr.get(vm.getId());
                scheduleRestart(vm, false);
                command = info.mgr.cleanup(vm, agentName);
            } else if (serverState == State.Stopping) {
                if (fullSync) {
                    s_logger.debug("VM is in stopping state on full sync.  Updating the status to stopped");
                    vm = info.mgr.get(vm.getId());
                    info.mgr.completeStopCommand(vm);
                    command = info.mgr.cleanup(vm, agentName);
                } else {
                    s_logger.debug("Ignoring VM in stopping mode: " + vm.getName());
                }
            } else if (serverState == State.Starting) {
                s_logger.debug("Ignoring VM in starting mode: " + vm.getName());
            } else {
                s_logger.debug("Sending cleanup to a stopped vm: " + agentName);
                _instanceDao.updateIf(vm, VirtualMachine.Event.AgentReportStopped, null);
                command = info.mgr.cleanup(vm, agentName);
            }
        } else if (agentState == State.Running) {
            if (serverState == State.Starting) {
                if (fullSync) {
                    s_logger.debug("VM state is starting on full sync so updating it to running");
                    vm = info.mgr.get(vm.getId());
                    info.mgr.completeStartCommand(vm);
                }
            } else if (serverState == State.Stopping) {
                if (fullSync) {
                    s_logger.debug("VM state is in stopping on fullsync so resend stop.");
                    vm = info.mgr.get(vm.getId());
                    info.mgr.completeStopCommand(vm);
                    command = info.mgr.cleanup(vm, agentName);
                } else {
                    s_logger.debug("VM is in stopping state so no action.");
                }
            } else if (serverState == State.Destroyed || serverState == State.Stopped || serverState == State.Expunging) {
            	s_logger.debug("VM state is in stopped so stopping it on the agent");
            	vm = info.mgr.get(vm.getId());
            	command = info.mgr.cleanup(vm, agentName);
            } else {
            	_instanceDao.updateIf(vm, VirtualMachine.Event.AgentReportRunning, vm.getHostId());
            }
        } /*else if (agentState == State.Unknown) {
            if (serverState == State.Running) {
                if (fullSync) {
                    vm = info.handler.get(vm.getId());
                }
                scheduleRestart(vm, false);
            } else if (serverState == State.Starting) {
                if (fullSync) {
                    vm = info.handler.get(vm.getId());
                }
                scheduleRestart(vm, false);
            } else if (serverState == State.Stopping) {
                if (fullSync) {
                    s_logger.debug("VM state is stopping in full sync.  Resending stop");
                    command = info.handler.cleanup(vm, agentName);
                }
            }
        }*/
        return command;
    }

    public List<Command> fullSync(final long hostId, final Map<String, State> newStates) {
        final List<? extends VMInstanceVO> vms = _instanceDao.listByHostId(hostId);
        s_logger.debug("Found " + vms.size() + " VMs for host " + hostId);

        final Map<Long, AgentVmInfo> states = convertToIds(newStates);
        final ArrayList<Command> commands = new ArrayList<Command>();

        for (final VMInstanceVO vm : vms) {
            AgentVmInfo info = states.remove(vm.getId());

            if (info == null) {
                info = new AgentVmInfo(null, findManager(vm.getType()), State.Stopped);
            }
            
            assert info.mgr != null : "How can the manager be null for " + vm.getType();

            VMInstanceVO vmCasted = info.mgr.get(vm.getId());
            final Command command = compareState(vmCasted, info, true);
            if (command != null) {
                commands.add(command);
            }
        }

        for (final AgentVmInfo left : states.values()) {
            s_logger.warn("Stopping a VM that we have no record of: " + left.name);
            commands.add(left.mgr.cleanup(null, left.name));
        }

        return commands;
    }

    protected Map<Long, AgentVmInfo> convertToIds(final Map<String, State> states) {
        final HashMap<Long, AgentVmInfo> map = new HashMap<Long, AgentVmInfo>();

        if (states == null) {
            return map;
        }

        final Collection<VirtualMachineManager<VMInstanceVO>> handlers = _handlers.values();

        for (final Map.Entry<String, State> entry : states.entrySet()) {
            for (final VirtualMachineManager<VMInstanceVO> handler : handlers) {
                final String name = entry.getKey();

                final Long id = handler.convertToId(name);

                if (id != null) {
                    map.put(id, new AgentVmInfo(entry.getKey(), handler, entry.getValue()));
                    break;
                }
            }
        }

        return map;
    }

    public List<Command> deltaSync(final long hostId, final Map<String, State> newStates) {
        final Map<Long, AgentVmInfo> states = convertToIds(newStates);
        final ArrayList<Command> commands = new ArrayList<Command>();

        for (final Map.Entry<Long, AgentVmInfo> entry : states.entrySet()) {
            final AgentVmInfo info = entry.getValue();

            final VMInstanceVO vm = info.mgr.get(entry.getKey());

            Command command = null;
            if (vm != null && vm.getHostId() != null && vm.getHostId() == hostId) {
                command = compareState(vm, info, false);
            } else {
                s_logger.debug("VM is not found.  Stopping " + info.name);
                command = info.mgr.cleanup(null, info.name);
            }

            if (command != null) {
                commands.add(command);
            }
        }

        return commands;
    }

    public Long migrate(final WorkVO work) {
        final long vmId = work.getInstanceId();

        final VirtualMachineManager<VMInstanceVO> mgr = findManager(work.getType());

        VMInstanceVO vm = mgr.get(vmId);
        if (vm == null || vm.getRemoved() != null) {
            s_logger.debug("Unable to find the vm " + vmId);
            return null;
        }

        s_logger.info("Migrating vm: " + vm.toString());
        if (vm.getHostId() == null || vm.getHostId() != work.getHostId()) {
        	s_logger.info("VM is not longer running on the current hostId");
        	return null;
        }

        short alertType = AlertManager.ALERT_TYPE_USERVM_MIGRATE;
        if (VirtualMachine.Type.DomainRouter.equals(vm.getType())) {
            alertType = AlertManager.ALERT_TYPE_DOMAIN_ROUTER_MIGRATE;
        } else if (VirtualMachine.Type.ConsoleProxy.equals(vm.getType())) {
            alertType = AlertManager.ALERT_TYPE_CONSOLE_PROXY_MIGRATE;
        }

        HostVO fromHost = _hostDao.findById(vm.getHostId());
        String fromHostName = ((fromHost == null) ? "unknown" : fromHost.getName());
        HostVO toHost = null;
        if (work.getStep() == Step.Scheduled) {
            if (vm.getState() != State.Running) {
                s_logger.info("VM's state is not ready for migration. " + vm.toString() + " State is " + vm.getState().toString());
                return (System.currentTimeMillis() >> 10) + _migrateRetryInterval;
            }
            
            DataCenterVO dcVO = _dcDao.findById(fromHost.getDataCenterId());
            HostPodVO podVO = _podDao.findById(fromHost.getPodId());

            try {
                toHost = mgr.prepareForMigration(vm);
                if (toHost == null) {
                    if (s_logger.isDebugEnabled()) {
                        s_logger.debug("Unable to find a host for migrating vm " + vmId);
                    }
                    _alertMgr.sendAlert(alertType, vm.getDataCenterId(), vm.getPodId(), "Unable to migrate vm " + vm.getName() + " from host " + fromHostName + " in zone " + dcVO.getName() + " and pod " + podVO.getName(), "Unable to find a suitable host");
                }
            } catch(final InsufficientCapacityException e) {
            	s_logger.warn("Unable to mgirate due to insufficient capcity " + vm.toString());
                _alertMgr.sendAlert(alertType, vm.getDataCenterId(), vm.getPodId(), "Unable to migrate vm " + vm.getName() + " from host " + fromHostName + " in zone " + dcVO.getName() + " and pod " + podVO.getName(), "Insufficient capacity");
            } catch(final StorageUnavailableException e) {
                s_logger.warn("Storage is unavailable: " + vm.toString());
                _alertMgr.sendAlert(alertType, vm.getDataCenterId(), vm.getPodId(), "Unable to migrate vm " + vm.getName() + " from host " + fromHostName + " in zone " + dcVO.getName() + " and pod " + podVO.getName(), "Storage is gone.");
            }
            
            if (toHost == null) {
                _agentMgr.maintenanceFailed(vm.getHostId());
                return null;
            }

            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Migrating from " + work.getHostId() + " to " + toHost.getId());
            }
            work.setStep(Step.Migrating);
            work.setHostId(toHost.getId());
            _haDao.update(work.getId(), work);
        }

        if (work.getStep() == Step.Migrating) {
        	vm = mgr.get(vmId);	// let's see if anything has changed.
            boolean migrated = false;
        	if (vm == null || vm.getRemoved() != null || vm.getHostId() == null || !_instanceDao.updateIf(vm, Event.MigrationRequested, vm.getHostId())) {
            	s_logger.info("Migration cancelled because state has changed: " + vm.toString());
        	} else {
                try {
                    boolean isWindows = _guestOSCategoryDao.findById(_guestOSDao.findById(vm.getGuestOSId()).getCategoryId()).getName().equalsIgnoreCase("Windows");
	                MigrateCommand cmd = new MigrateCommand(vm.getInstanceName(), toHost.getPrivateIpAddress(), isWindows);
	                Answer answer = _agentMgr.send(fromHost.getId(), cmd);
	                if (answer != null && answer.getResult()) {
	                	migrated = true;
	                    _storageMgr.unshare(vm, fromHost);
	                    work.setStep(Step.Investigating);
	                    _haDao.update(work.getId(), work);
	                }
                } catch (final AgentUnavailableException e) {
                    s_logger.debug("host became unavailable");
                } catch (final OperationTimedoutException e) {
                	s_logger.debug("operation timed out");
                	if (e.isActive()) {
                        scheduleRestart(vm, true);
                	}
                }
          	}

            if (!migrated) {
                s_logger.info("Migration was unsuccessful.  Cleaning up: " + vm.toString());

                DataCenterVO dcVO = _dcDao.findById(vm.getDataCenterId());
                HostPodVO podVO = _podDao.findById(vm.getPodId());
                _alertMgr.sendAlert(alertType, fromHost.getDataCenterId(), fromHost.getPodId(), "Unable to migrate vm " + vm.getName() + " from host " + fromHost.getName() + " in zone " + dcVO.getName() + " and pod " + podVO.getName(), "Migrate Command failed.  Please check logs.");

                _instanceDao.updateIf(vm, Event.OperationFailed, vm.getHostId());
                _agentMgr.maintenanceFailed(vm.getHostId());
                
                Command cleanup = mgr.cleanup(vm, null);
                _agentMgr.easySend(toHost.getId(), cleanup);
                _storageMgr.unshare(vm, toHost);
                
                return null;
            }
        }
        
        if (toHost == null) {
            toHost = _hostDao.findById(work.getHostId());
        }
        DataCenterVO dcVO = _dcDao.findById(toHost.getDataCenterId());
        HostPodVO podVO = _podDao.findById(toHost.getPodId());

        try {
            if (!mgr.completeMigration(vm, toHost)) {
                _alertMgr.sendAlert(alertType, toHost.getDataCenterId(), toHost.getPodId(), "Unable to migrate " + vmId + " to host " + toHost.getName() + " in zone " + dcVO.getName() + " and pod " + podVO.getName(), "Migration not completed");
                s_logger.warn("Unable to complete migration: " + vm.toString());
            } else {
            	s_logger.info("Migration is complete: " + vm.toString());
            }
            return null;
        } catch (final AgentUnavailableException e) {
        	s_logger.warn("Agent is unavailable for " + vm.toString());
        } catch (final OperationTimedoutException e) {
        	s_logger.warn("Operation timed outfor " + vm.toString());
        }
    	_instanceDao.updateIf(vm, Event.OperationFailed, toHost.getId());
    	return (System.currentTimeMillis() >> 10) + _migrateRetryInterval;
    }
    
    @Override
    public void scheduleDestroy(VMInstanceVO vm, long hostId) {
        final WorkVO work = new WorkVO(vm.getId(), vm.getType(), WorkType.Destroy, Step.Scheduled, hostId, vm.getState(), 0, vm.getUpdated());
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
    
    protected Long destroyVM(WorkVO work) {
        final VirtualMachineManager<VMInstanceVO> mgr = findManager(work.getType());
        final VMInstanceVO vm = mgr.get(work.getInstanceId());
        s_logger.info("Destroying " + vm.toString());
        try {
        	if (vm.getState() != State.Destroyed) {
        		s_logger.info("VM is no longer in Destroyed state " + vm.toString());
        		return null;
        	}
        	
            if (vm.getHostId() != null) {
    			Command cmd = mgr.cleanup(vm, null);
    			Answer ans = _agentMgr.send(work.getHostId(), cmd);
    			if (ans.getResult()) {
    				mgr.completeStopCommand(vm);
    				if (mgr.destroy(vm)) {
    					s_logger.info("Successfully stopped " + vm.toString());
    					return null;
    				}
    			}
    			s_logger.debug("Stop for " + vm.toString() + " was unsuccessful. Detail: " + ans.getDetails());
            } else {
            	if (s_logger.isDebugEnabled()) {
            		s_logger.debug(vm.toString() + " has already been stopped");
            	}
                return null;
            }
        } catch (final AgentUnavailableException e) {
            s_logger.debug("Agnet is not available" + e.getMessage());
        } catch (OperationTimedoutException e) {
        	s_logger.debug("operation timed out: " + e.getMessage());
		}
        
        work.setTimesTried(work.getTimesTried() + 1);
        return (System.currentTimeMillis() >> 10) + _stopRetryInterval;
    }

    protected Long stopVM(final WorkVO work) {
        final VirtualMachineManager<VMInstanceVO> mgr = findManager(work.getType());
        final VMInstanceVO vm = mgr.get(work.getInstanceId());
        s_logger.info("Stopping " + vm.toString());
        try {
        	if (work.getWorkType() == WorkType.Stop) {
	            if (vm.getHostId() != null) {
	                if (mgr.stop(vm, 0)) {
	                	s_logger.info("Successfully stopped " + vm.toString());
	                    return null;
	                }
	            } else {
	            	if (s_logger.isDebugEnabled()) {
	            		s_logger.debug(vm.toString() + " has already been stopped");
	            	}
	                return null;
	            }
        	} else if (work.getWorkType() == WorkType.CheckStop) {
        		if ((vm.getState() != State.Stopping) || vm.getHostId() == null || vm.getHostId().longValue() != work.getHostId()) {
        			if (s_logger.isDebugEnabled()) {
        				s_logger.debug(vm.toString() + " is different now.  Scheduled Host: " + work.getHostId() + " Current Host: " + (vm.getHostId() != null ? vm.getHostId() : "none") + " State: " + vm.getState());
        			}
        			return null;
        		} else {
        			Command cmd = mgr.cleanup(vm, null);
        			Answer ans = _agentMgr.send(work.getHostId(), cmd);
        			if (ans.getResult()) {
        				mgr.completeStopCommand(vm);
        				s_logger.info("Successfully stopped " + vm.toString());
        				return null;
        			}
        			s_logger.debug("Stop for " + vm.toString() + " was unsuccessful. Detail: " + ans.getDetails());
        		}
        	} else {
        		assert false : "Who decided there's other steps but didn't modify the guy who does the work?";
        	}
        } catch (final AgentUnavailableException e) {
            s_logger.debug("Agnet is not available" + e.getMessage());
        } catch (OperationTimedoutException e) {
        	s_logger.debug("operation timed out: " + e.getMessage());
		}
        
        work.setTimesTried(work.getTimesTried() + 1);
        return (System.currentTimeMillis() >> 10) + _stopRetryInterval;
    }

    @Override
    public void cancelScheduledMigrations(final HostVO host) {
        WorkType type = host.getType() == HostVO.Type.Storage ? WorkType.Stop : WorkType.Migration;

        _haDao.deleteMigrationWorkItems(host.getId(), type, _serverId);
    }
    
    @Override
    public List<VMInstanceVO> findTakenMigrationWork() {
    	List<WorkVO> works = _haDao.findTakenWorkItems(WorkType.Migration);
    	List<VMInstanceVO> vms = new ArrayList<VMInstanceVO>(works.size());
    	for (WorkVO work : works) {
    		vms.add(_instanceDao.findById(work.getInstanceId()));
    	}
    	return vms;
    }

    @Override
    public boolean configure(final String name, final Map<String, Object> xmlParams) throws ConfigurationException {
        _name = name;
        ComponentLocator locator = ComponentLocator.getLocator(ManagementServer.Name);

        /*
        _haDao = locator.getDao(HighAvailabilityDao.class);
        if (_haDao == null) {
            throw new ConfigurationException("Unable to get ha dao");
        }

        _instanceDao = locator.getDao(VMInstanceDao.class);
        if (_instanceDao == null) {
            throw new ConfigurationException("Unable to get vm dao");
        }

        _hostDao = locator.getDao(HostDao.class);
        if (_hostDao == null) {
            throw new ConfigurationException("unable to get host dao");
        }

        _dcDao = locator.getDao(DataCenterDao.class);
        if (_dcDao == null) {
            throw new ConfigurationException("unable to get data center dao");
        }

        _podDao = locator.getDao(HostPodDao.class);
        if (_podDao == null) {
            throw new ConfigurationException("unable to get pod dao");
        }

        _agentMgr = locator.getManager(AgentManager.class);
        if (_agentMgr == null) {
            throw new ConfigurationException("Unable to find " + AgentManager.class.getName());
        }

        _alertMgr = locator.getManager(AlertManager.class);
        if (_alertMgr == null) {
            throw new ConfigurationException("Unable to find " + AlertManager.class.getName());
        }
        
        _storageMgr = locator.getManager(StorageManager.class);
        if (_storageMgr == null) {
        	throw new ConfigurationException("Unable to find " + StorageManager.class.getName());
        }
*/
        _serverId = ((ManagementServer)ComponentLocator.getComponent(ManagementServer.Name)).getId();

        _investigators = locator.getAdapters(Investigator.class);
        _fenceBuilders = locator.getAdapters(FenceBuilder.class);

        Map<String, String> params = new HashMap<String, String>();
        final ConfigurationDao configDao = locator.getDao(ConfigurationDao.class);
        if (configDao != null) {
            params = configDao.getConfiguration(Long.toHexString(_serverId), xmlParams);
        }

        String value = params.get("workers");
        final int count = NumbersUtil.parseInt(value, 1);
        _workers = new WorkerThread[count];
        for (int i = 0; i < _workers.length; i++) {
            _workers[i] = new WorkerThread("HA-Worker-" + i);
        }

        value = params.get("force.ha");
        _forceHA = Boolean.parseBoolean(value);

        value = params.get("time.to.sleep");
        _timeToSleep = NumbersUtil.parseInt(value, 60) * 1000;

        value = params.get("max.retries");
        _maxRetries = NumbersUtil.parseInt(value, 5);

        value = params.get("time.between.failures");
        _timeBetweenFailures = NumbersUtil.parseLong(value, 3600) * 1000;

        value = params.get("time.between.cleanup");
        _timeBetweenCleanups = NumbersUtil.parseLong(value, 3600 * 24);
        
        value = params.get("wait");
        _operationTimeout = NumbersUtil.parseInt(value, 1800) * 2;
        
        value = params.get("stop.retry.interval");
        _stopRetryInterval = NumbersUtil.parseInt(value, 10 * 60);
        
        value = params.get("restart.retry.interval");
        _restartRetryInterval = NumbersUtil.parseInt(value, 10 * 60);
        
        value = params.get("investigate.retry.interval");
        _investigateRetryInterval = NumbersUtil.parseInt(value, 1 * 60);
        
        value = params.get("migrate.retry.interval");
        _migrateRetryInterval = NumbersUtil.parseInt(value, 2 * 60);

        _instance = params.get("instance");
        if (_instance == null) {
            _instance = "VMOPS";
        }

        _stopped = true;
        
        _executor = Executors.newScheduledThreadPool(count, new NamedThreadFactory("HA"));

        _agentMgr.registerForHostEvents(new VmSyncListener(this, _agentMgr), true, true, true);
        
        return true;
    }
    
    @Override
    public String getName() {
        return _name;
    }

    @Override
    public boolean start() {
        _stopped = false;

        for (final WorkerThread thread : _workers) {
            thread.start();
        }

        _executor.scheduleAtFixedRate(new CleanupTask(), _timeBetweenCleanups, _timeBetweenCleanups, TimeUnit.SECONDS);
        _executor.scheduleAtFixedRate(new TransitionTask(), 0, _operationTimeout, TimeUnit.SECONDS);

        return true;
    }
    
    @Override
    public boolean stop() {
        _stopped = true;

        wakeupWorkers();

        _executor.shutdown();

        return true;
    }

    protected class CleanupTask implements Runnable {
        @Override
        public void run() {
            s_logger.info("HA Cleanup Thread Running");

            try {
            	_haDao.cleanup(System.currentTimeMillis() - _timeBetweenFailures);
            } catch (Exception e) {
                s_logger.warn("Error while cleaning up", e);
            } finally {
            	StackMaid.current().exitCleanup();
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
                try {
                    s_logger.trace("Checking the database");
                    final WorkVO work = _haDao.take(_serverId);
                    if (work == null) {
                        try {
                        	synchronized(this) {
                        		wait(_timeToSleep);
                        	}
                            continue;
                        } catch (final InterruptedException e) {
                            s_logger.info("Interrupted");
                            continue;
                        }
                    }
                    
                    s_logger.info("Working on " + work.toString());

                    try {
                        final WorkType wt = work.getWorkType();
                        Long nextTime = null;
                        if (wt == WorkType.Migration) {
                            nextTime = migrate(work);
                        } else if (wt == WorkType.HA) {
                            nextTime = restart(work);
                        } else if (wt == WorkType.Stop || wt == WorkType.CheckStop) {
                            nextTime = stopVM(work);
                        } else if (wt == WorkType.Destroy) {
                        	nextTime = destroyVM(work);
                        } else {
                        	assert false : "How did we get here with " + wt.toString();
                            continue;
                        }
    
                        if (nextTime == null) {
                        	if (s_logger.isDebugEnabled()) {
                        		s_logger.debug(work.toString() + " is complete");
                        	}
                            work.setStep(Step.Done);
                        } else {
                        	if (s_logger.isDebugEnabled()) {
                        		s_logger.debug("Rescheduling " + work.toString() + " for instance " + work.getInstanceId() + " to try again at " + new Date(nextTime << 10));
                        	}
                            work.setTimeToTry(nextTime);
                            work.setServerId(null);
                            work.setDateTaken(null);
                        }
                    } catch (Exception e) {
                        s_logger.error("Caught this exception while processing the work queue.", e);
                        work.setStep(Step.Error);
                    }
                    _haDao.update(work.getId(), work);
                } catch(final Throwable th) {
                    s_logger.error("Caught this throwable, ", th);
                } finally {
                	StackMaid.current().exitCleanup();
                }
            }
            s_logger.info("Time to go home!");
        }
        
        public synchronized void wakup() {
        	notifyAll();
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
	    		List<VMInstanceVO> instances = _instanceDao.findVMInTransition(new Date(new Date().getTime() - (_operationTimeout * 1000)), State.Starting, State.Stopping);
	    		for (VMInstanceVO instance : instances) {
	    			State state = instance.getState();
	    			if (state == State.Stopping) {
	    				scheduleStop(instance, instance.getHostId(), true);
	    			} else if (state == State.Starting) {
	    				scheduleRestart(instance, true);
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
        public VirtualMachineManager mgr;
        public State state;
        public State action;

        public AgentVmInfo(final String name, final VirtualMachineManager handler, final State state) {
            this.name = name;
            this.mgr = handler;
            this.state = state;
        }
    }
}
