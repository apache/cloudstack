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

import java.util.List;

import com.cloud.utils.fsm.StateMachine;

public enum State {
    Creating(true),
    Starting(true),
    Running(false),
    Stopping(true),
    Stopped(false),
    Destroyed(false),
    Expunging(true),
    Migrating(true),
    Error(false),
    Unknown(false);
    
    private final boolean _transitional;
    
    private State(boolean transitional) {
    	_transitional = transitional;
    }
    
    public boolean isTransitional() {
    	return _transitional;
    }
    
    public static String[] toStrings(State... states) {
        String[] strs = new String[states.length];
        for (int i = 0; i < states.length; i++) {
            strs[i] = states[i].toString();
        }
        
        return strs;
    }
    
    public State getNextState(VirtualMachine.Event e) {
        return s_fsm.getNextState(this, e);
    }

    public State[] getFromStates(VirtualMachine.Event e) {
        List<State> from = s_fsm.getFromStates(this, e);
        return from.toArray(new State[from.size()]);
    }
    
    protected static final StateMachine<State, VirtualMachine.Event> s_fsm = new StateMachine<State, VirtualMachine.Event>();
    static {
    	s_fsm.addTransition(null, VirtualMachine.Event.CreateRequested, State.Creating);
    	s_fsm.addTransition(State.Creating, VirtualMachine.Event.OperationSucceeded, State.Stopped);
    	s_fsm.addTransition(State.Creating, VirtualMachine.Event.OperationFailed, State.Error);
    	s_fsm.addTransition(State.Stopped, VirtualMachine.Event.StartRequested, State.Starting);
    	s_fsm.addTransition(State.Stopped, VirtualMachine.Event.DestroyRequested, State.Destroyed);
    	s_fsm.addTransition(State.Stopped, VirtualMachine.Event.StopRequested, State.Stopped);
    	s_fsm.addTransition(State.Stopped, VirtualMachine.Event.AgentReportStopped, State.Stopped);
    	s_fsm.addTransition(State.Starting, VirtualMachine.Event.OperationRetry, State.Starting);
    	s_fsm.addTransition(State.Starting, VirtualMachine.Event.OperationSucceeded, State.Running);
    	s_fsm.addTransition(State.Starting, VirtualMachine.Event.OperationFailed, State.Stopped);
    	s_fsm.addTransition(State.Starting, VirtualMachine.Event.AgentReportRunning, State.Running);
    	s_fsm.addTransition(State.Starting, VirtualMachine.Event.AgentReportStopped, State.Stopped);
    	s_fsm.addTransition(State.Destroyed, VirtualMachine.Event.RecoveryRequested, State.Stopped);
    	s_fsm.addTransition(State.Destroyed, VirtualMachine.Event.ExpungeOperation, State.Expunging);
    	s_fsm.addTransition(State.Creating, VirtualMachine.Event.MigrationRequested, State.Destroyed);
    	s_fsm.addTransition(State.Running, VirtualMachine.Event.MigrationRequested, State.Migrating);
    	s_fsm.addTransition(State.Running, VirtualMachine.Event.AgentReportRunning, State.Running);
    	s_fsm.addTransition(State.Running, VirtualMachine.Event.AgentReportStopped, State.Stopped);
    	s_fsm.addTransition(State.Running, VirtualMachine.Event.StopRequested, State.Stopping);
    	s_fsm.addTransition(State.Migrating, VirtualMachine.Event.MigrationRequested, State.Migrating);
    	s_fsm.addTransition(State.Migrating, VirtualMachine.Event.OperationSucceeded, State.Running);
    	s_fsm.addTransition(State.Migrating, VirtualMachine.Event.OperationFailed, State.Running);
    	s_fsm.addTransition(State.Migrating, VirtualMachine.Event.AgentReportRunning, State.Running);
    	s_fsm.addTransition(State.Migrating, VirtualMachine.Event.AgentReportStopped, State.Stopped);
    	s_fsm.addTransition(State.Stopping, VirtualMachine.Event.OperationSucceeded, State.Stopped);
    	s_fsm.addTransition(State.Stopping, VirtualMachine.Event.OperationFailed, State.Running);
    	s_fsm.addTransition(State.Stopping, VirtualMachine.Event.AgentReportRunning, State.Running);
    	s_fsm.addTransition(State.Stopping, VirtualMachine.Event.AgentReportStopped, State.Stopped);
    	s_fsm.addTransition(State.Stopping, VirtualMachine.Event.StopRequested, State.Stopping);
    	s_fsm.addTransition(State.Expunging, VirtualMachine.Event.OperationFailed, State.Expunging);
    	s_fsm.addTransition(State.Expunging, VirtualMachine.Event.ExpungeOperation, State.Expunging);
    }
}
