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
import java.util.List;
import java.util.Set;

import com.cloud.acl.ControlledEntity;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.utils.fsm.FiniteState;
import com.cloud.utils.fsm.StateMachine;

/**
 * VirtualMachine describes the properties held by a virtual machine 
 *
 */
public interface VirtualMachine extends RunningOn, ControlledEntity {
    public enum State implements FiniteState<State, Event> {
        Creating(true, "VM is being created"),
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
        
        @Override
        public String getDescription() {
            return _description;
        }
        
        public boolean isTransitional() {
            return _transitional;
        }
        
        @Override
        public State getNextState(VirtualMachine.Event e) {
            return s_fsm.getNextState(this, e);
        }

        @Override
        public List<State> getFromStates(VirtualMachine.Event e) {
            return s_fsm.getFromStates(this, e);
        }
        
        @Override
        public Set<Event> getPossibleEvents() {
            return s_fsm.getPossibleEvents(this);
        }
        
        @Override
        public StateMachine<State, Event> getStateMachine() {
            return s_fsm;
        }
        
        
        protected static final StateMachine<State, VirtualMachine.Event> s_fsm = new StateMachine<State, VirtualMachine.Event>();
        static {
            s_fsm.addTransition(null, VirtualMachine.Event.CreateRequested, State.Creating); 
            s_fsm.addTransition(State.Creating, VirtualMachine.Event.OperationSucceeded, State.Stopped); 
            s_fsm.addTransition(State.Creating, VirtualMachine.Event.OperationFailed, State.Error); 
            s_fsm.addTransition(State.Stopped, VirtualMachine.Event.StartRequested, State.Starting); 
            s_fsm.addTransition(State.Stopped, VirtualMachine.Event.DestroyRequested, State.Destroyed); 
            s_fsm.addTransition(State.Error, VirtualMachine.Event.DestroyRequested, State.Destroyed); 
            s_fsm.addTransition(State.Stopped, VirtualMachine.Event.StopRequested, State.Stopped); 
            s_fsm.addTransition(State.Stopped, VirtualMachine.Event.AgentReportStopped, State.Stopped); 
            s_fsm.addTransition(State.Stopped, VirtualMachine.Event.AgentReportShutdowned, State.Stopped); 
            s_fsm.addTransition(State.Starting, VirtualMachine.Event.OperationRetry, State.Starting); 
            s_fsm.addTransition(State.Starting, VirtualMachine.Event.OperationSucceeded, State.Running); 
            s_fsm.addTransition(State.Starting, VirtualMachine.Event.OperationFailed, State.Stopped); 
            s_fsm.addTransition(State.Starting, VirtualMachine.Event.AgentReportRunning, State.Running); 
            s_fsm.addTransition(State.Starting, VirtualMachine.Event.AgentReportStopped, State.Stopped); 
            s_fsm.addTransition(State.Starting, VirtualMachine.Event.AgentReportShutdowned, State.Stopped); 
            s_fsm.addTransition(State.Destroyed, VirtualMachine.Event.RecoveryRequested, State.Stopped); 
            s_fsm.addTransition(State.Destroyed, VirtualMachine.Event.ExpungeOperation, State.Expunging); 
            s_fsm.addTransition(State.Creating, VirtualMachine.Event.MigrationRequested, State.Destroyed); 
            s_fsm.addTransition(State.Running, VirtualMachine.Event.MigrationRequested, State.Migrating); 
            s_fsm.addTransition(State.Running, VirtualMachine.Event.AgentReportRunning, State.Running); 
            s_fsm.addTransition(State.Running, VirtualMachine.Event.AgentReportStopped, State.Stopped); 
            s_fsm.addTransition(State.Running, VirtualMachine.Event.StopRequested, State.Stopping); 
            s_fsm.addTransition(State.Running, VirtualMachine.Event.AgentReportShutdowned, State.Stopped); 
            s_fsm.addTransition(State.Migrating, VirtualMachine.Event.MigrationRequested, State.Migrating); 
            s_fsm.addTransition(State.Migrating, VirtualMachine.Event.OperationSucceeded, State.Running); 
            s_fsm.addTransition(State.Migrating, VirtualMachine.Event.OperationFailed, State.Running); 
            s_fsm.addTransition(State.Migrating, VirtualMachine.Event.MigrationFailedOnSource, State.Running); 
            s_fsm.addTransition(State.Migrating, VirtualMachine.Event.MigrationFailedOnDest, State.Running); 
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
    	ExpungeOperation,
    	OperationSucceeded,
    	OperationFailed,
    	MigrationFailedOnSource,
    	MigrationFailedOnDest,
    	OperationRetry,
    	OperationCancelled,
    	AgentReportShutdowned
    };
    
    public enum Type {
        User,
        DomainRouter,
        ConsoleProxy,
        SecondaryStorageVm
    }
    
    public String getInstanceName();
    
    /**
     * @return the id of this virtual machine.  null means the id has not been set.
     */
    public long getId();
    
    /**
     * @return the name of the virtual machine.
     */
    public String getName();
    
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
    public State getState();
    
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
    public Long getPodId();
    
    /**
     * @return data center id.
     */
    public long getDataCenterId();
    
    /**
     * @return id of the host it was assigned last time.
     */
    public Long getLastHostId();

    /**
     * @return should HA be enabled for this machine?
     */
    public boolean isHaEnabled();
    
	/**
     * @return date when machine was created
     */
	public Date getCreated();
	
	public long getServiceOfferingId();
	
	Type getType();
	
	HypervisorType getHypervisorType();
}
