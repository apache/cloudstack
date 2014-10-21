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

import java.util.Map;

import com.cloud.agent.api.to.VirtualMachineTO;

public class StartAnswer extends Answer {
    VirtualMachineTO vm;
    String hostGuid;
    Map<String, String> _iqnToPath;

    protected StartAnswer() {
    }

    public StartAnswer(StartCommand cmd, String msg) {
        super(cmd, false, msg);
        this.vm = cmd.getVirtualMachine();
    }

    public StartAnswer(StartCommand cmd, Exception e) {
        super(cmd, false, e.getMessage());
        this.vm = cmd.getVirtualMachine();
    }

    public StartAnswer(StartCommand cmd) {
        super(cmd, true, null);
        this.vm = cmd.getVirtualMachine();
        this.hostGuid = null;
    }

    public StartAnswer(StartCommand cmd, String msg, String guid) {
        super(cmd, true, msg);
        this.vm = cmd.getVirtualMachine();
        this.hostGuid = guid;
    }

    public VirtualMachineTO getVirtualMachine() {
        return vm;
    }

    public String getHost_guid() {
        return hostGuid;
    }

    public void setIqnToPath(Map<String, String> iqnToPath) {
        _iqnToPath = iqnToPath;
    }

    public Map<String, String> getIqnToPath() {
        return _iqnToPath;
    }
}
