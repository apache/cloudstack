//
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
//

package com.cloud.agent.api;

import java.util.ArrayList;
import java.util.List;

public class PingAnswer extends Answer {
    private PingCommand _command = null;

    private boolean sendStartup = false;
    private List<String> avoidMsList;

    private List<String> reconcileCommands = new ArrayList<>();

    protected PingAnswer() {
    }

    public PingAnswer(PingCommand cmd, List<String> avoidMsList, boolean sendStartup) {
        super(cmd);
        _command = cmd;
        this.sendStartup = sendStartup;
        this.avoidMsList = avoidMsList;
    }

    public PingCommand getCommand() {
        return _command;
    }

    public boolean isSendStartup() {
        return sendStartup;
    }

    public void setSendStartup(boolean sendStartup) {
        this.sendStartup = sendStartup;
    }

    public List<String> getReconcileCommands() {
        return reconcileCommands;
    }

    public void setReconcileCommands(List<String> reconcileCommands) {
        this.reconcileCommands = reconcileCommands;
    }

    public void addReconcileCommand(String reconcileCommand) {
        this.reconcileCommands.add(reconcileCommand);
    }

    public List<String> getAvoidMsList() {
        return avoidMsList;
    }
}
