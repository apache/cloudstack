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
package com.cloud.agent.api;

import com.cloud.host.HostEnvironment;

public class SetupCommand extends Command {

    HostEnvironment env;
    boolean multipath;
    boolean needSetup;
    String secondaryStorage;
    String systemVmIso;

    public boolean needSetup() {
        return needSetup;
    }

    public void setNeedSetup(boolean setup) {
        this.needSetup = setup;
    }

    public SetupCommand(HostEnvironment env) {
        this.env = env;
        this.multipath = false;
        this.needSetup = false;
        secondaryStorage = null;
        systemVmIso = null;
    }

    public HostEnvironment getEnvironment() {
        return env;
    }

    protected SetupCommand() {
    }

    public void setMultipathOn() {
        this.multipath = true;
    }

    public boolean useMultipath() {
        return multipath;
    }

    public void setSecondaryStorage(String secondaryStorage) {
        this.secondaryStorage = secondaryStorage;
    }

    public String getSecondaryStorage() {
        return this.secondaryStorage;
    }

    public void setSystemVmIso(String systemVmIso) {
        this.systemVmIso = systemVmIso;
    }

    public String getSystemVmIso() {
        return this.systemVmIso;
    }

    @Override
    public boolean executeInSequence() {
        return true;
    }
}
