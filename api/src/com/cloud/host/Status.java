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
package com.cloud.host;

import java.util.List;
import java.util.Set;

import com.cloud.utils.fsm.NoTransitionException;
import com.cloud.utils.fsm.StateMachine;
import com.cloud.utils.fsm.StateMachine2;

public enum Status {
	Creating(true, false, false),
    Connecting(true, false, false),
    Up(true, false, false),
    Down(true, true, true),
    Disconnected(true, true, true),
    Alert(true, true, true),
    Removed(true, false, true),
    Error(true, false, true),
    Rebalancing(false, false, false);
    
    private final boolean updateManagementServer;
    private final boolean checkManagementServer;
    private final boolean lostConnection;
    
    private Status(boolean updateConnection, boolean checkManagementServer, boolean lostConnection) {
    	this.updateManagementServer = updateConnection;
    	this.checkManagementServer = checkManagementServer;
    	this.lostConnection = lostConnection;
    }
    
    public boolean updateManagementServer() {
    	return updateManagementServer;
    }
    
    public boolean checkManagementServer() {
    	return checkManagementServer;
    }
    
    public boolean lostConnection() {
        return lostConnection;
    }

    public enum Event {
        AgentConnected(false, "Agent connected"),
        PingTimeout(false, "Agent is behind on ping"),
        ShutdownRequested(false, "Shutdown requested by the agent"),
        AgentDisconnected(false, "Agent disconnected"),
        HostDown(false, "Host is found to be down by the investigator"),
        Ping(false, "Ping is received from the host"),
        ManagementServerDown(false, "Management Server that the agent is connected is going down"),
        WaitedTooLong(false, "Waited too long from the agent to reconnect on its own.  Time to do HA"),
        Remove(true, "Host is removed"),
        Ready(false, "Host is ready for commands"),
        RequestAgentRebalance(false, "Request rebalance for the certain host"),
        StartAgentRebalance(false, "Start rebalance for the certain host"),
        RebalanceCompleted(false, "Host is rebalanced successfully"),
        RebalanceFailed(false, "Failed to rebalance the host"),
        Error(false, "An internal error happened");

        private final boolean isUserRequest;
        private final String comment;
        private Event(boolean isUserRequest, String comment) {
        	this.isUserRequest = isUserRequest;
            this.comment = comment;
        }

        public String getDescription() {
            return comment;
        }
        
        public boolean isUserRequest() {
        	return isUserRequest;
        }
    }
    
    public static StateMachine2<Status, Event, Host> getStateMachine() {
        return s_fsm;
    }
    
    public Status getNextStatus(Event e) throws NoTransitionException {
        return s_fsm.getNextState(this, e);
    }

    public Status[] getFromStates(Event e) {
        List<Status> from = s_fsm.getFromStates(this, e);
        return from.toArray(new Status[from.size()]);
    }
    
    public Set<Event> getPossibleEvents() {
        return s_fsm.getPossibleEvents(this);
    }

    public static String[] toStrings(Status... states) {
        String[] strs = new String[states.length];
        for (int i = 0; i < states.length; i++) {
            strs[i] = states[i].toString();
        }
        return strs;
    }

    protected static final StateMachine2<Status, Event, Host> s_fsm = new StateMachine2<Status, Event, Host>();
    static {
        s_fsm.addTransition(null, Event.AgentConnected, Status.Connecting);
        s_fsm.addTransition(Status.Creating, Event.AgentConnected, Status.Connecting);
        s_fsm.addTransition(Status.Creating, Event.Error, Status.Error);
        s_fsm.addTransition(Status.Connecting, Event.AgentConnected, Status.Connecting);
        s_fsm.addTransition(Status.Connecting, Event.Ready, Status.Up);
        s_fsm.addTransition(Status.Connecting, Event.PingTimeout, Status.Alert);
        s_fsm.addTransition(Status.Connecting, Event.ShutdownRequested, Status.Disconnected);
        s_fsm.addTransition(Status.Connecting, Event.HostDown, Status.Alert);
        s_fsm.addTransition(Status.Connecting, Event.Ping, Status.Connecting);
        s_fsm.addTransition(Status.Connecting, Event.ManagementServerDown, Status.Disconnected);
        s_fsm.addTransition(Status.Connecting, Event.AgentDisconnected, Status.Alert);
        s_fsm.addTransition(Status.Up, Event.PingTimeout, Status.Alert);
        s_fsm.addTransition(Status.Up, Event.AgentDisconnected, Status.Alert);
        s_fsm.addTransition(Status.Up, Event.ShutdownRequested, Status.Disconnected);
        s_fsm.addTransition(Status.Up, Event.HostDown, Status.Down);
        s_fsm.addTransition(Status.Up, Event.Ping, Status.Up);
        s_fsm.addTransition(Status.Up, Event.AgentConnected, Status.Connecting);
        s_fsm.addTransition(Status.Up, Event.ManagementServerDown, Status.Disconnected);
        s_fsm.addTransition(Status.Up, Event.StartAgentRebalance, Status.Rebalancing);
        s_fsm.addTransition(Status.Up, Event.Remove, Status.Removed);
        s_fsm.addTransition(Status.Disconnected, Event.PingTimeout, Status.Alert);
        s_fsm.addTransition(Status.Disconnected, Event.AgentConnected, Status.Connecting);
        s_fsm.addTransition(Status.Disconnected, Event.Ping, Status.Up);
        s_fsm.addTransition(Status.Disconnected, Event.HostDown, Status.Down);
        s_fsm.addTransition(Status.Disconnected, Event.ManagementServerDown, Status.Disconnected);
        s_fsm.addTransition(Status.Disconnected, Event.WaitedTooLong, Status.Alert);
        s_fsm.addTransition(Status.Disconnected, Event.Remove, Status.Removed);
        s_fsm.addTransition(Status.Disconnected, Event.AgentDisconnected, Status.Disconnected);
        s_fsm.addTransition(Status.Down, Event.AgentConnected, Status.Connecting);
        s_fsm.addTransition(Status.Down, Event.Remove, Status.Removed);
        s_fsm.addTransition(Status.Down, Event.ManagementServerDown, Status.Down);
        s_fsm.addTransition(Status.Down, Event.AgentDisconnected, Status.Down);
        s_fsm.addTransition(Status.Alert, Event.AgentConnected, Status.Connecting);
        s_fsm.addTransition(Status.Alert, Event.Ping, Status.Up);
        s_fsm.addTransition(Status.Alert, Event.Remove, Status.Removed);
        s_fsm.addTransition(Status.Alert, Event.ManagementServerDown, Status.Alert);
        s_fsm.addTransition(Status.Alert, Event.AgentDisconnected, Status.Alert);
        s_fsm.addTransition(Status.Alert, Event.ShutdownRequested, Status.Disconnected);
        s_fsm.addTransition(Status.Rebalancing, Event.RebalanceFailed, Status.Disconnected);
        s_fsm.addTransition(Status.Rebalancing, Event.RebalanceCompleted, Status.Connecting);
        s_fsm.addTransition(Status.Rebalancing, Event.AgentConnected, Status.Connecting);
        s_fsm.addTransition(Status.Error, Event.AgentConnected, Status.Connecting);
    }

    public static void main(String[] args) {
        System.out.println("Finite State Machine for Host:");
        System.out.println(s_fsm.toString());
    }
}
