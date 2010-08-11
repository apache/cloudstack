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
package com.cloud.agent.manager;

import org.apache.log4j.Logger;

import com.cloud.agent.Listener;
import com.cloud.agent.api.AgentControlAnswer;
import com.cloud.agent.api.AgentControlCommand;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.StartupCommand;
import com.cloud.host.HostVO;
import com.cloud.host.Status;
import com.cloud.utils.Profiler;

public class SynchronousListener implements Listener {
    private static final Logger s_logger = Logger.getLogger(SynchronousListener.class);
	
    protected Answer[] _answers;
    protected boolean _disconnected;
    protected String _peer;

    public SynchronousListener(Listener listener) {
        _answers = null;
        _peer = null;
    }
    
    public void setPeer(String peer) {
        _peer = peer;
    }
    
    public String getPeer() {
        return _peer;
    }
    
    public Answer[] getAnswers() {
        return _answers;
    }
    
    @Override
    public boolean isRecurring() {
        return false;
    }
    
    public boolean isDisconnected() {
        return _disconnected;
    }

    @Override
    public synchronized boolean processAnswer(long agentId, long seq, Answer[] resp) {
        _answers = resp;
        notifyAll();
        return true;
    }
    
    @Override
    public synchronized boolean processDisconnect(long agentId, Status state) {
    	if(s_logger.isTraceEnabled())
    		s_logger.trace("Agent disconnected, agent id: " + agentId + ", state: " + state + ". Will notify waiters");
    	
        _disconnected = true;
        notifyAll();
        return true;
    }
    
    @Override
    public boolean processConnect(HostVO agent, StartupCommand cmd) {
        return false;
    }

    @Override
    public boolean processCommand(long agentId, long seq, Command[] req) {
        return false;
    }
    
    @Override
    public AgentControlAnswer processControlCommand(long agentId, AgentControlCommand cmd) {
    	return null;
    }
    
    public Answer[] waitFor() throws InterruptedException {
        return waitFor(-1);
    }
    
    public synchronized Answer[] waitFor(int ms) throws InterruptedException {
        if (_disconnected) {
            return null;
        }
        
        if (_answers != null) {
            return _answers;
        }

        Profiler profiler = new Profiler();
        profiler.start();
        if (ms <= 0) {
            wait();
        } else {
            wait(ms);
        }
        profiler.stop();
        
        if(s_logger.isTraceEnabled()) {
        	s_logger.trace("Synchronized command - sending completed, time: " + profiler.getDuration() + ", answer: " +
    			(_answers != null ? _answers[0].toString() : "null"));
        }
        return _answers;
    }
    
    @Override
    public boolean processTimeout(long agentId, long seq) {
    	return true;
    }
    
    @Override
    public int getTimeout() {
    	return -1;
    }
}
