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

import java.util.Date;
import java.util.Map;

import com.cloud.acl.ControlledEntity;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.utils.fsm.StateMachine2;
import com.cloud.utils.fsm.StateObject;


/**
 * VirtualMachine describes the properties held by a virtual machine
 *
 */
public interface VirtualMachine extends RunningOn, ControlledEntity, StateObject<VirtualMachine.State> {
	
    public enum State {
        Starting(true, "VM is being started.  At this state, you should find host id filled which means it's being started on that host."),
        Running(false, "VM is running.  host id has the host that it is running on."),
        Stopping(true, "VM is being stopped.  host id has the host that it is being stopped on."),
        Stopped(false, "VM is stopped.  host id should be null."),
        Destroyed(false, "VM is marked for destroy."),
        Expunging(true, "VM is being   expunged."),
        Migrating(true, "VM is being migrated.  host id holds to from host"),
        Error(false, "VM is in error"),
        Unknown(false, "VM state is unknown."),
        Shutdowned(false, "VM is shutdowned from inside");

        
        private final boolean _transitional;
        String _description;
        
        private State(boolean transitional, String description) {
            _transitional = transitional;
            _description = description;
        }
        
        public String getDescription() {
            return _description;
        }
        
        public boolean isTransitional() {
            return _transitional;
        }
        
        public static StateMachine2<State, VirtualMachine.Event, VirtualMachine> getStateMachine() {
            return s_fsm;
        }
        
        protected static final StateMachine2<State, VirtualMachine.Event, VirtualMachine> s_fsm = new StateMachine2<State, VirtualMachine.Event, VirtualMachine>();
        static {
            s_fsm.addTransition(State.Stopped, VirtualMachine.Event.StartRequested, State.Starting);
            s_fsm.addTransition(State.Stopped, VirtualMachine.Event.DestroyRequested, State.Destroyed);
            s_fsm.addTransition(State.Stopped, VirtualMachine.Event.StopRequested, State.Stopped);
            s_fsm.addTransition(State.Stopped, VirtualMachine.Event.AgentReportStopped, State.Stopped);
            
            // please pay attention about state transition to Error state, there should be only one case (failed in VM creation process)
            // that can have such transition
            s_fsm.addTransition(State.Stopped, VirtualMachine.Event.OperationFailedToError, State.Error);
            
            s_fsm.addTransition(State.Stopped, VirtualMachine.Event.OperationFailed, State.Stopped);
            s_fsm.addTransition(State.Stopped, VirtualMachine.Event.ExpungeOperation, State.Expunging);
            s_fsm.addTransition(State.Stopped, VirtualMachine.Event.AgentReportShutdowned, State.Stopped);
            s_fsm.addTransition(State.Stopped, VirtualMachine.Event.StorageMigrationRequested, State.Migrating);
            s_fsm.addTransition(State.Starting, VirtualMachine.Event.OperationRetry, State.Starting);
            s_fsm.addTransition(State.Starting, VirtualMachine.Event.OperationSucceeded, State.Running);
            s_fsm.addTransition(State.Starting, VirtualMachine.Event.OperationFailed, State.Stopped);
            s_fsm.addTransition(State.Starting, VirtualMachine.Event.AgentReportRunning, State.Running);
            s_fsm.addTransition(State.Starting, VirtualMachine.Event.AgentReportStopped, State.Stopped);
            s_fsm.addTransition(State.Starting, VirtualMachine.Event.AgentReportShutdowned, State.Stopped);
            s_fsm.addTransition(State.Destroyed, VirtualMachine.Event.RecoveryRequested, State.Stopped);
            s_fsm.addTransition(State.Destroyed, VirtualMachine.Event.ExpungeOperation, State.Expunging);
            s_fsm.addTransition(State.Running, VirtualMachine.Event.MigrationRequested, State.Migrating);
            s_fsm.addTransition(State.Running, VirtualMachine.Event.AgentReportRunning, State.Running);
            s_fsm.addTransition(State.Running, VirtualMachine.Event.AgentReportStopped, State.Stopped);
            s_fsm.addTransition(State.Running, VirtualMachine.Event.StopRequested, State.Stopping);
            s_fsm.addTransition(State.Running, VirtualMachine.Event.AgentReportShutdowned, State.Stopped);
            s_fsm.addTransition(State.Running, VirtualMachine.Event.AgentReportMigrated, State.Running);
            s_fsm.addTransition(State.Migrating, VirtualMachine.Event.MigrationRequested, State.Migrating);
            s_fsm.addTransition(State.Migrating, VirtualMachine.Event.OperationSucceeded, State.Running);
            s_fsm.addTransition(State.Migrating, VirtualMachine.Event.OperationFailed, State.Running);
            s_fsm.addTransition(State.Migrating, VirtualMachine.Event.AgentReportRunning, State.Running);
            s_fsm.addTransition(State.Migrating, VirtualMachine.Event.AgentReportStopped, State.Stopped);
            s_fsm.addTransition(State.Migrating, VirtualMachine.Event.AgentReportShutdowned, State.Stopped);
            s_fsm.addTransition(State.Stopping, VirtualMachine.Event.OperationSucceeded, State.Stopped);
            s_fsm.addTransition(State.Stopping, VirtualMachine.Event.OperationFailed, State.Running);
            s_fsm.addTransition(State.Stopping, VirtualMachine.Event.AgentReportRunning, State.Running);
            s_fsm.addTransition(State.Stopping, VirtualMachine.Event.AgentReportStopped, State.Stopped);
            s_fsm.addTransition(State.Stopping, VirtualMachine.Event.StopRequested, State.Stopping);
            s_fsm.addTransition(State.Stopping, VirtualMachine.Event.AgentReportShutdowned, State.Stopped);
            s_fsm.addTransition(State.Expunging, VirtualMachine.Event.OperationFailed, State.Expunging);
            s_fsm.addTransition(State.Expunging, VirtualMachine.Event.ExpungeOperation, State.Expunging);
            s_fsm.addTransition(State.Error, VirtualMachine.Event.DestroyRequested, State.Expunging);
            s_fsm.addTransition(State.Error, VirtualMachine.Event.ExpungeOperation, State.Expunging);
        }
        
        public static boolean isVmStarted(State oldState, Event e, State newState) {
            if (oldState == State.Starting && newState == State.Running) {
                return true;
            }
            return false;
        }
        
        public static boolean isVmStopped(State oldState, Event e, State newState) {
            if (oldState == State.Stopping && newState == State.Stopped) {
                return true;
            }
            return false;
        }
        
        public static boolean isVmMigrated(State oldState, Event e, State newState) {
            if (oldState == State.Migrating && newState == State.Running && (e == Event.OperationSucceeded || e == Event.AgentReportRunning)) {
                return true;
            }
            return false;
        }
        
        public static boolean isVmCreated(State oldState, Event e, State newState) {
            if (oldState == State.Destroyed && newState == State.Stopped) {
                // VM recover
                return true;
            }
            return false;
        }
        
        public static boolean isVmDestroyed(State oldState, Event e, State newState) {
            if (oldState == State.Stopped && newState == State.Destroyed) {
                return true;
            }
            if (oldState == State.Stopped && newState == State.Error) {
                return true;
            }
            return false;
        }
    }
    
    public enum Event {
    	CreateRequested,
    	StartRequested,
    	StopRequested,
    	DestroyRequested,
    	RecoveryRequested,
    	AgentReportStopped,
    	AgentReportRunning,
    	MigrationRequested,
    	StorageMigrationRequested,
    	ExpungeOperation,
    	OperationSucceeded,
    	OperationFailed,
    	OperationFailedToError,
    	OperationRetry,
    	AgentReportShutdowned,
    	AgentReportMigrated
    };
    
    public enum Type {
        User,
        DomainRouter,
        ConsoleProxy,
        SecondaryStorageVm,
        ElasticIpVm,
        ElasticLoadBalancerVm,
        
        /*
         * UserBareMetal is only used for selecting VirtualMachineGuru, there is no
         * VM with this type. UserBareMetal should treat exactly as User.
         */
        UserBareMetal;
        
        public static boolean isSystemVM(VirtualMachine.Type vmtype) {
        	if(DomainRouter.equals(vmtype)
        			|| ConsoleProxy.equals(vmtype)
        			|| SecondaryStorageVm.equals(vmtype)){
        		return true;
        	}
            return false;
        }
    }
    
    /**
     * @return The name of the vm instance used by the cloud stack to uniquely
     * reference this VM.  You can build names that starts with this name and it
     * guarantees uniqueness for things related to the VM.
     */
    public String getInstanceName();
    
    /**
     * @return the id of this virtual machine.  null means the id has not been set.
     */
    public long getId();
    
    /**
     * @return the host name of the virtual machine.  If the user did not
     * specify the host name when creating the virtual machine then it is
     * defaults to the instance name.
     */
    public String getHostName();
    
    /**
     * @return the ip address of the virtual machine.
     */
    public String getPrivateIpAddress();
    
    /**
     * @return mac address.
     */
    public String getPrivateMacAddress();
    
    /**
     * @return password of the host for vnc purposes.
     */
    public String getVncPassword();
    
    /**
     * @return the state of the virtual machine
     */
    //public State getState();
    
    /**
     * @return template id.
     */
    public long getTemplateId();
    
    /**
     * returns the guest OS ID
     * @return guestOSId
     */
    public long getGuestOSId();
    
    /**
     * @return pod id.
     */
    public Long getPodIdToDeployIn();
    
    /**
     * @return data center id.
     */
    public long getDataCenterIdToDeployIn();
    
    /**
     * @return id of the host it was assigned last time.
     */
    public Long getLastHostId();
    @Override
    public Long getHostId();
    
    /**
     * @return should HA be enabled for this machine?
     */
    public boolean isHaEnabled();

    /**
     * @return should limit CPU usage to the service offering?
     */
    public boolean limitCpuUse();
	/**
     * @return date when machine was created
     */
	public Date getCreated();
	
	public long getServiceOfferingId();
	
	Type getType();
	
	HypervisorType getHypervisorType();
	
	public Map<String, String> getDetails();
}
