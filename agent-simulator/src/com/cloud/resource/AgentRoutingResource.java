/**
 *  Copyright (C) 2011 Cloud.com, Inc.  All rights reserved.
 */

package com.cloud.resource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.CheckVirtualMachineAnswer;
import com.cloud.agent.api.CheckVirtualMachineCommand;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.PingCommand;
import com.cloud.agent.api.PingRoutingWithNwGroupsCommand;
import com.cloud.agent.api.PrepareForMigrationCommand;
import com.cloud.agent.api.ReadyAnswer;
import com.cloud.agent.api.ReadyCommand;
import com.cloud.agent.api.ShutdownCommand;
import com.cloud.agent.api.StartAnswer;
import com.cloud.agent.api.StartCommand;
import com.cloud.agent.api.StartupCommand;
import com.cloud.agent.api.StartupRoutingCommand;
import com.cloud.agent.api.StartupStorageCommand;
import com.cloud.agent.api.StopAnswer;
import com.cloud.agent.api.StopCommand;
import com.cloud.agent.api.StoragePoolInfo;
import com.cloud.agent.api.to.VirtualMachineTO;
import com.cloud.agent.manager.SimulatorManager;
import com.cloud.agent.manager.SimulatorManager.AgentType;
import com.cloud.host.Host;
import com.cloud.host.Host.Type;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.network.Networks.RouterPrivateIpStrategy;
import com.cloud.simulator.MockVMVO;
import com.cloud.storage.Storage.StorageResourceType;
import com.cloud.storage.template.TemplateInfo;
import com.cloud.utils.Pair;
import com.cloud.vm.VirtualMachine.State;

public class AgentRoutingResource extends AgentStorageResource {
    private static final Logger s_logger = Logger.getLogger(AgentRoutingResource.class);

    protected Map<String, State> _vms = new HashMap<String, State>();
    private Map<String, Pair<Long, Long>> _runningVms = new HashMap<String, Pair<Long, Long>>();
    long usedCpu = 0;
    long usedMem = 0;
    long totalCpu;
    long totalMem;
    protected String _mountParent;


    public AgentRoutingResource(long instanceId, AgentType agentType, SimulatorManager simMgr, String hostGuid) {
        super(instanceId, agentType, simMgr, hostGuid);
    }

    public AgentRoutingResource() {
        setType(Host.Type.Routing);
    }

    @Override
    public Answer executeRequest(Command cmd) {
        try {
            if (cmd instanceof StartCommand) {
                return execute((StartCommand) cmd);
            } else if (cmd instanceof StopCommand) {
                return execute((StopCommand) cmd);
            } else if (cmd instanceof PrepareForMigrationCommand) {
                return execute((PrepareForMigrationCommand) cmd);
            } else if (cmd instanceof CheckVirtualMachineCommand) {
                return execute((CheckVirtualMachineCommand) cmd);
            } else if (cmd instanceof ReadyCommand) {
                return new ReadyAnswer((ReadyCommand)cmd);
            } else if (cmd instanceof ShutdownCommand) {
                return execute((ShutdownCommand)cmd); 
            } else {
                return _simMgr.simulate(cmd, hostGuid);
            }
        } catch (IllegalArgumentException e) {
            return new Answer(cmd, false, e.getMessage());
        }
    }
	
    @Override
    public Type getType() {
        return Host.Type.Routing;
    }

    @Override
    public PingCommand getCurrentStatus(long id) {
        if (isStopped()) {
            return null;
        }
        synchronized (_vms) {
        	if (_vms.size() == 0) {
        		//load vms state from database
        		_vms.putAll(_simMgr.getVmStates(hostGuid));
        	}
        }
        final HashMap<String, State> newStates = sync();
        HashMap<String, Pair<Long, Long>> nwGrpStates = _simMgr.syncNetworkGroups(hostGuid);
        return new PingRoutingWithNwGroupsCommand(getType(), id, newStates, nwGrpStates);
    }

    @Override
    public StartupCommand[] initialize() {
        synchronized (_vms) {
            _vms.clear();
        }
        Map<String, State> changes = _simMgr.getVmStates(this.hostGuid);
        Map<String, MockVMVO> vmsMaps = _simMgr.getVms(this.hostGuid);
        totalCpu = agentHost.getCpuCount() * agentHost.getCpuSpeed();
        totalMem = agentHost.getMemorySize();
        for (Map.Entry<String, MockVMVO> entry : vmsMaps.entrySet()) {
        	MockVMVO vm = entry.getValue();
        	usedCpu += vm.getCpu();
        	usedMem += vm.getMemory();
        	_runningVms.put(entry.getKey(), new Pair<Long, Long>(Long.valueOf(vm.getCpu()), vm.getMemory()));
        }
        
        List<Object> info = getHostInfo();

        StartupRoutingCommand cmd = new StartupRoutingCommand((Integer) info.get(0), (Long) info.get(1), (Long) info.get(2), (Long) info.get(4), (String) info.get(3), HypervisorType.Simulator,
                RouterPrivateIpStrategy.HostLocal);
        cmd.setStateChanges(changes);
        
        Map<String, String> hostDetails = new HashMap<String, String>();
        hostDetails.put(RouterPrivateIpStrategy.class.getCanonicalName(), RouterPrivateIpStrategy.DcGlobal.toString());

        cmd.setHostDetails(hostDetails);
        cmd.setAgentTag("agent-simulator");
        cmd.setPrivateIpAddress(agentHost.getPrivateIpAddress());
        cmd.setPrivateNetmask(agentHost.getPrivateNetMask());
        cmd.setPrivateMacAddress(agentHost.getPrivateMacAddress());
        cmd.setStorageIpAddress(agentHost.getStorageIpAddress());
        cmd.setStorageNetmask(agentHost.getStorageNetMask());
        cmd.setStorageMacAddress(agentHost.getStorageMacAddress());
        cmd.setStorageIpAddressDeux(agentHost.getStorageIpAddress());
        cmd.setStorageNetmaskDeux(agentHost.getStorageNetMask());
        cmd.setStorageMacAddressDeux(agentHost.getStorageIpAddress());

        cmd.setName(agentHost.getName());
        cmd.setGuid(agentHost.getGuid());
        cmd.setVersion(agentHost.getVersion());
        cmd.setAgentTag("agent-simulator");
        cmd.setDataCenter(String.valueOf(agentHost.getDataCenterId()));
        cmd.setPod(String.valueOf(agentHost.getPodId()));
        cmd.setCluster(String.valueOf(agentHost.getClusterId()));

        StartupStorageCommand ssCmd = initializeLocalSR();
      
        return new StartupCommand[] { cmd, ssCmd };
    }

    private StartupStorageCommand initializeLocalSR() {
        Map<String, TemplateInfo> tInfo = new HashMap<String, TemplateInfo>();
      
        StoragePoolInfo poolInfo = _simMgr.getLocalStorage(hostGuid);

        StartupStorageCommand cmd = new StartupStorageCommand(poolInfo.getHostPath(), poolInfo.getPoolType(), poolInfo.getCapacityBytes(), tInfo);

        cmd.setPoolInfo(poolInfo);
        cmd.setGuid(agentHost.getGuid());
        cmd.setResourceType(StorageResourceType.STORAGE_POOL);
        return cmd;
    }
    
	protected synchronized Answer execute(StartCommand cmd)
			throws IllegalArgumentException {
		VirtualMachineTO vmSpec = cmd.getVirtualMachine();
		String vmName = vmSpec.getName();
		if (this.totalCpu < (vmSpec.getCpus() * vmSpec.getSpeed() + this.usedCpu) ||
			this.totalMem < (vmSpec.getMaxRam() + this.usedMem)) {
			return new StartAnswer(cmd, "No enough resource to start the vm"); 
		}
		State state = State.Stopped;
		synchronized (_vms) {
			_vms.put(vmName, State.Starting);
		}

		try {
		    Answer result = _simMgr.simulate(cmd, hostGuid);
		    if (!result.getResult()) {
		        return new StartAnswer(cmd, result.getDetails());
		    }
		    
		    this.usedCpu += vmSpec.getCpus() * vmSpec.getSpeed();
		    this.usedMem += vmSpec.getMaxRam();
		    _runningVms.put(vmName, new Pair<Long, Long>(Long.valueOf(vmSpec.getCpus() * vmSpec.getSpeed()), vmSpec.getMaxRam()));
		    state = State.Running;

		} finally {
		    synchronized (_vms) {
		        _vms.put(vmName, state);
		    }
		}

		return new StartAnswer(cmd);

	}
	
	protected synchronized StopAnswer execute(StopCommand cmd) {

		StopAnswer answer = null;
		String vmName = cmd.getVmName();

		State state = null;
		synchronized (_vms) {
			state = _vms.get(vmName);
			_vms.put(vmName, State.Stopping);
		}
		try {
		    Answer result = _simMgr.simulate(cmd, hostGuid);
		    
		    if (!result.getResult()) {
		        return new StopAnswer(cmd, result.getDetails());
		    }
		    
			answer = new StopAnswer(cmd, null, 0, new Long(100), new Long(200));
			Pair<Long, Long> data = _runningVms.get(vmName);
			if (data != null) {
				this.usedCpu -= data.first();
				this.usedMem -= data.second();
			}
			state = State.Stopped;
			
		} finally {
			synchronized (_vms) {
				_vms.put(vmName, state);
			}
		}
		
        return answer;
	}
   
    protected CheckVirtualMachineAnswer execute(final CheckVirtualMachineCommand cmd) {
        final String vmName = cmd.getVmName();
        CheckVirtualMachineAnswer result = (CheckVirtualMachineAnswer)_simMgr.simulate(cmd, hostGuid);
        State state = result.getState();
        if (state == State.Running) {
            synchronized (_vms) {
                _vms.put(vmName, State.Running);
            }
        }
        return result;
    }

    protected List<Object> getHostInfo() {
        ArrayList<Object> info = new ArrayList<Object>();
        long speed = agentHost.getCpuSpeed();
        long cpus = agentHost.getCpuCount();
        long ram = agentHost.getMemorySize();
        long dom0Ram = agentHost.getMemorySize()/10;

        info.add((int) cpus);
        info.add(speed);
        info.add(ram);
        info.add(agentHost.getCapabilities());
        info.add(dom0Ram);

        return info;
    }

    protected HashMap<String, State> sync() {
        Map<String, State> newStates;
        Map<String, State> oldStates = null;

        HashMap<String, State> changes = new HashMap<String, State>();

        synchronized (_vms) {
            oldStates = new HashMap<String, State>(_vms.size());
            oldStates.putAll(_vms);
            newStates = new HashMap<String, State>(_vms.size());
            newStates.putAll(_vms);

            for (Map.Entry<String, State> entry : newStates.entrySet()) {
                String vm = entry.getKey();

                State newState = entry.getValue();
                State oldState = oldStates.remove(vm);

                if (s_logger.isTraceEnabled()) {
                    s_logger.trace("VM " + vm + ": has state " + newState + " and we have state " + (oldState != null ? oldState.toString() : "null"));
                }

                if (oldState == null) {
                    _vms.put(vm, newState);
                    changes.put(vm, newState);
                } else if (oldState == State.Starting) {
                    if (newState == State.Running) {
                        _vms.put(vm, newState);
                    } else if (newState == State.Stopped) {
                        s_logger.debug("Ignoring vm " + vm + " because of a lag in starting the vm.");
                    }
                } else if (oldState == State.Stopping) {
                    if (newState == State.Stopped) {
                        _vms.put(vm, newState);
                    } else if (newState == State.Running) {
                        s_logger.debug("Ignoring vm " + vm + " because of a lag in stopping the vm. ");
                    }
                } else if (oldState != newState) {
                    _vms.put(vm, newState);
                    changes.put(vm, newState);
                }
            }

            for (Map.Entry<String, State> entry : oldStates.entrySet()) {
                String vm = entry.getKey();
                State oldState = entry.getValue();

                if (s_logger.isTraceEnabled()) {
                    s_logger.trace("VM " + vm + " is now missing from simulator agent so reporting stopped");
                }

                if (oldState == State.Stopping) {
                    s_logger.debug("Ignoring VM " + vm + " in transition state stopping.");
                    _vms.remove(vm);
                } else if (oldState == State.Starting) {
                    s_logger.debug("Ignoring VM " + vm + " in transition state starting.");
                } else if (oldState == State.Stopped) {
                    _vms.remove(vm);
                } else {
                    changes.put(entry.getKey(), State.Stopped);
                }
            }
        }

        return changes;
    }
    
    private Answer execute(ShutdownCommand cmd) {
        this.stopped = true;
        return new Answer(cmd);
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        if (!super.configure(name, params)) {
            s_logger.warn("Base class was unable to configure");
            return false;
        }
        return true;
    }
}
