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
package com.cloud.network.security;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

import com.cloud.agent.AgentManager;
import com.cloud.agent.Listener;
import com.cloud.agent.api.AgentControlAnswer;
import com.cloud.agent.api.AgentControlCommand;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.CleanupNetworkRulesCmd;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.PingRoutingWithNwGroupsCommand;
import com.cloud.agent.api.SecurityGroupRuleAnswer;
import com.cloud.agent.api.SecurityGroupRuleAnswer.FailureReason;
import com.cloud.agent.api.StartupCommand;
import com.cloud.agent.api.StartupRoutingCommand;
import com.cloud.agent.manager.Commands;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.host.Host;
import com.cloud.host.Status;
import com.cloud.network.security.SecurityGroupWork.Step;
import com.cloud.network.security.dao.SecurityGroupWorkDao;

/**
 * Listens for answers to ingress rules modification commands
 *
 */
public class SecurityGroupListener implements Listener {
    public static final Logger s_logger = Logger.getLogger(SecurityGroupListener.class.getName());

    private static final int MAX_RETRIES_ON_FAILURE = 3;
    private static final int MIN_TIME_BETWEEN_CLEANUPS = 30 * 60;//30 minutes
    private final Random _cleanupRandom = new Random();

    SecurityGroupManagerImpl _securityGroupManager;
    AgentManager _agentMgr;
    SecurityGroupWorkDao _workDao;
    Map<Long, Integer> _vmFailureCounts = new ConcurrentHashMap<Long, Integer>();

    private SecurityGroupWorkTracker _workTracker;

    public SecurityGroupListener(SecurityGroupManagerImpl securityGroupManager, AgentManager agentMgr, SecurityGroupWorkDao workDao) {
        super();
        _securityGroupManager = securityGroupManager;
        _agentMgr = agentMgr;
        _workDao = workDao;
    }

    @Override
    public int getTimeout() {
        return -1;
    }

    @Override
    public boolean isRecurring() {
        return true;
    }

    @Override
    public boolean processAnswers(long agentId, long seq, Answer[] answers) {
        List<Long> affectedVms = new ArrayList<Long>();

        for (Answer ans : answers) {
            if (ans instanceof SecurityGroupRuleAnswer) {
                SecurityGroupRuleAnswer ruleAnswer = (SecurityGroupRuleAnswer)ans;
                if (ans.getResult()) {
                    s_logger.debug("Successfully programmed rule " + ruleAnswer.toString() + " into host " + agentId);
                    _workDao.updateStep(ruleAnswer.getVmId(), ruleAnswer.getLogSequenceNumber(), Step.Done);
                    recordSuccess(ruleAnswer.getVmId());
                } else {
                    _workDao.updateStep(ruleAnswer.getVmId(), ruleAnswer.getLogSequenceNumber(), Step.Error);
                    ;
                    s_logger.debug("Failed to program rule " + ruleAnswer.toString() + " into host " + agentId + " due to " + ruleAnswer.getDetails() +
                        " and updated  jobs");
                    if (ruleAnswer.getReason() == FailureReason.CANNOT_BRIDGE_FIREWALL) {
                        s_logger.debug("Not retrying security group rules for vm " + ruleAnswer.getVmId() + " on failure since host " + agentId +
                            " cannot do bridge firewalling");
                    } else if (ruleAnswer.getReason() == FailureReason.PROGRAMMING_FAILED) {
                        if (checkShouldRetryOnFailure(ruleAnswer.getVmId())) {
                            s_logger.debug("Retrying security group rules on failure for vm " + ruleAnswer.getVmId());
                            affectedVms.add(ruleAnswer.getVmId());
                        } else {
                            s_logger.debug("Not retrying security group rules for vm " + ruleAnswer.getVmId() + " on failure: too many retries");
                        }
                    }
                }

                if (_workTracker != null)
                    _workTracker.processAnswers(agentId, seq, answers);
            }
        }

        if (affectedVms.size() > 0) {
            _securityGroupManager.scheduleRulesetUpdateToHosts(affectedVms, false, new Long(10 * 1000l));
        }

        return true;
    }

    protected boolean checkShouldRetryOnFailure(long vmId) {
        Integer currCount = _vmFailureCounts.get(vmId);
        if (currCount == null)
            currCount = 0;

        if (currCount.intValue() < MAX_RETRIES_ON_FAILURE) {
            _vmFailureCounts.put(vmId, ++currCount);
            return true;
        }

        return false;
    }

    protected void recordSuccess(long vmId) {
        _vmFailureCounts.remove(vmId);
    }

    @Override
    public boolean processCommands(long agentId, long seq, Command[] commands) {
        boolean processed = false;
        for (Command cmd : commands) {
            if (cmd instanceof PingRoutingWithNwGroupsCommand) {
                PingRoutingWithNwGroupsCommand ping = (PingRoutingWithNwGroupsCommand)cmd;
                if (ping.getNewGroupStates().size() > 0) {
                    _securityGroupManager.fullSync(agentId, ping.getNewGroupStates());
                }
                processed = true;
            }
        }
        return processed;
    }

    @Override
    public void processHostAdded(long hostId) {
    }

    @Override
    public void processConnect(Host host, StartupCommand cmd, boolean forRebalance) {
        if (s_logger.isInfoEnabled())
            s_logger.info("Received a host startup notification");

        if (cmd instanceof StartupRoutingCommand) {
            //if (Boolean.toString(true).equals(host.getDetail("can_bridge_firewall"))) {
            try {
                int interval = MIN_TIME_BETWEEN_CLEANUPS + _cleanupRandom.nextInt(MIN_TIME_BETWEEN_CLEANUPS / 2);
                CleanupNetworkRulesCmd cleanupCmd = new CleanupNetworkRulesCmd(interval);
                Commands c = new Commands(cleanupCmd);
                _agentMgr.send(host.getId(), c, this);
                if (s_logger.isInfoEnabled())
                    s_logger.info("Scheduled network rules cleanup, interval=" + cleanupCmd.getInterval());
            } catch (AgentUnavailableException e) {
                //usually hypervisors that do not understand sec group rules.
                s_logger.debug("Unable to schedule network rules cleanup for host " + host.getId(), e);
            }
            if (_workTracker != null) {
                _workTracker.processConnect(host.getId());
            }
        }
    }

    @Override
    public AgentControlAnswer processControlCommand(long agentId, AgentControlCommand cmd) {
        return null;
    }

    @Override
    public boolean processDisconnect(long agentId, Status state) {
        if (_workTracker != null) {
            _workTracker.processDisconnect(agentId);
        }
        return true;
    }

    @Override
    public void processHostAboutToBeRemoved(long hostId) {
    }

    @Override
    public void processHostRemoved(long hostId, long clusterId) {
    }

    @Override
    public boolean processTimeout(long agentId, long seq) {
        if (_workTracker != null) {
            _workTracker.processTimeout(agentId, seq);
        }
        return true;
    }

    public void setWorkDispatcher(SecurityGroupWorkTracker workDispatcher) {
        this._workTracker = workDispatcher;
    }
}
