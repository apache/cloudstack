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
package com.cloud.network;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import org.apache.cloudstack.framework.config.dao.ConfigurationDao;

import com.cloud.agent.AgentManager;
import com.cloud.agent.Listener;
import com.cloud.agent.api.AgentControlAnswer;
import com.cloud.agent.api.AgentControlCommand;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.ModifySshKeysCommand;
import com.cloud.agent.api.StartupCommand;
import com.cloud.agent.api.StartupRoutingCommand;
import com.cloud.agent.manager.Commands;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.ConnectionException;
import com.cloud.host.Host;
import com.cloud.host.Status;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor.HypervisorType;

public class SshKeysDistriMonitor implements Listener {
    protected Logger logger = LogManager.getLogger(getClass());
    AgentManager _agentMgr;
    private ConfigurationDao _configDao;

    public SshKeysDistriMonitor(AgentManager mgr, HostDao host, ConfigurationDao config) {
        _agentMgr = mgr;
        _configDao = config;
    }

    @Override
    public boolean isRecurring() {
        return false;
    }

    @Override
    public synchronized boolean processAnswers(long agentId, long seq, Answer[] resp) {
        return true;
    }

    @Override
    public synchronized boolean processDisconnect(long agentId, Status state) {
        if (logger.isTraceEnabled())
            logger.trace("Agent disconnected, agent id: " + agentId + ", state: " + state + ". Will notify waiters");

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
    public void processConnect(Host host, StartupCommand cmd, boolean forRebalance) throws ConnectionException {
        if (cmd instanceof StartupRoutingCommand) {
            if (((StartupRoutingCommand)cmd).getHypervisorType() == HypervisorType.KVM || ((StartupRoutingCommand)cmd).getHypervisorType() == HypervisorType.XenServer ||
                ((StartupRoutingCommand)cmd).getHypervisorType() == HypervisorType.LXC) {
                /*TODO: Get the private/public keys here*/

                String pubKey = _configDao.getValue("ssh.publickey");
                String prvKey = _configDao.getValue("ssh.privatekey");

                try {
                    ModifySshKeysCommand cmds = new ModifySshKeysCommand(pubKey, prvKey);
                    Commands c = new Commands(cmds);
                    _agentMgr.send(host.getId(), c, this);
                } catch (AgentUnavailableException e) {
                    logger.debug("Failed to send keys to agent: " + host.getId());
                }
            }
        }
    }

    @Override
    public int getTimeout() {
        // TODO Auto-generated method stub
        return -1;
    }

    @Override
    public boolean processCommands(long agentId, long seq, Command[] commands) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public AgentControlAnswer processControlCommand(long agentId, AgentControlCommand cmd) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean processTimeout(long agentId, long seq) {
        // TODO Auto-generated method stub
        return false;
    }
}
