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

import org.apache.cloudstack.vm.UnmanagedInstanceTO;

import java.util.HashMap;
import java.util.List;

public class GetRemoteVmsAnswer extends Answer {

    private String remoteIp;
    @LogLevel(LogLevel.Log4jLevel.Trace)
    private HashMap<String, UnmanagedInstanceTO> unmanagedInstances;

    List<String> vmNames;

    GetRemoteVmsAnswer() {
    }

    public GetRemoteVmsAnswer(GetRemoteVmsCommand cmd, String details, HashMap<String, UnmanagedInstanceTO> unmanagedInstances) {
        super(cmd, true, details);
        this.remoteIp = cmd.getRemoteIp();
        this.unmanagedInstances = unmanagedInstances;
    }

    public GetRemoteVmsAnswer(GetRemoteVmsCommand cmd, String details, List<String> vmNames) {
        super(cmd, true, details);
        this.remoteIp = cmd.getRemoteIp();
        this.vmNames = vmNames;
    }

    public String getRemoteIp() {
        return remoteIp;
    }

    public void setRemoteIp(String remoteIp) {
        this.remoteIp = remoteIp;
    }

    public HashMap<String, UnmanagedInstanceTO> getUnmanagedInstances() {
        return unmanagedInstances;
    }

    public void setUnmanagedInstances(HashMap<String, UnmanagedInstanceTO> unmanagedInstances) {
        this.unmanagedInstances = unmanagedInstances;
    }

    public List<String> getVmNames() {
        return vmNames;
    }

    public void setVmNames(List<String> vmNames) {
        this.vmNames = vmNames;
    }

    public String getString() {
        return "GetRemoteVmsAnswer [remoteIp=" + remoteIp + "]";
    }
}
