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
package com.cloud.storage.secondary;

import org.apache.log4j.Logger;

import com.cloud.agent.Listener;
import com.cloud.agent.api.AgentControlAnswer;
import com.cloud.agent.api.AgentControlCommand;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.StartupCommand;
import com.cloud.agent.api.StartupSecondaryStorageCommand;
import com.cloud.agent.api.StartupStorageCommand;
import com.cloud.host.Host;
import com.cloud.host.Status;
import com.cloud.storage.Storage;

public class SecondaryStorageListener implements Listener {
    private final static Logger s_logger = Logger.getLogger(SecondaryStorageListener.class);

    SecondaryStorageVmManager _ssVmMgr = null;

    public SecondaryStorageListener(SecondaryStorageVmManager ssVmMgr) {
        _ssVmMgr = ssVmMgr;
    }

    @Override
    public boolean isRecurring() {
        return true;
    }

    @Override
    public boolean processAnswers(long agentId, long seq, Answer[] answers) {
        boolean processed = false;
        if (answers != null) {
            for (int i = 0; i < answers.length; i++) {
            }
        }

        return processed;
    }

    @Override
    public boolean processCommands(long agentId, long seq, Command[] commands) {
        return false;
    }

    @Override
    public AgentControlAnswer processControlCommand(long agentId, AgentControlCommand cmd) {
        return null;
    }

    @Override
    public void processHostAdded(long hostId) {
    }

    @Override
    public void processConnect(Host agent, StartupCommand cmd, boolean forRebalance) {
        if ((cmd instanceof StartupStorageCommand)) {
            StartupStorageCommand scmd = (StartupStorageCommand)cmd;
            if (scmd.getResourceType() == Storage.StorageResourceType.SECONDARY_STORAGE) {
                _ssVmMgr.generateSetupCommand(agent.getId());
                return;
            }
        } else if (cmd instanceof StartupSecondaryStorageCommand) {
            if (s_logger.isInfoEnabled()) {
                s_logger.info("Received a host startup notification " + cmd);
            }
            _ssVmMgr.onAgentConnect(agent.getDataCenterId(), cmd);
            _ssVmMgr.generateSetupCommand(agent.getId());
            _ssVmMgr.generateFirewallConfiguration(agent.getId());
            _ssVmMgr.generateVMSetupCommand(agent.getId());
            return;
        }
        return;
    }

    @Override
    public boolean processDisconnect(long agentId, Status state) {
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
        return true;
    }

    @Override
    public int getTimeout() {
        return -1;
    }
}
