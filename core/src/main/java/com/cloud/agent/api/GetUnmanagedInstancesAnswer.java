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

import java.util.HashMap;

import org.apache.cloudstack.vm.UnmanagedInstanceTO;

public class GetUnmanagedInstancesAnswer extends Answer {

    private String instanceName;
    @LogLevel(LogLevel.Log4jLevel.Trace)
    private HashMap<String, UnmanagedInstanceTO> unmanagedInstances;

    GetUnmanagedInstancesAnswer() {
    }

    public GetUnmanagedInstancesAnswer(GetUnmanagedInstancesCommand cmd, String details) {
        super(cmd, false, details);
    }

    public GetUnmanagedInstancesAnswer(GetUnmanagedInstancesCommand cmd, String details, HashMap<String, UnmanagedInstanceTO> unmanagedInstances) {
        super(cmd, true, details);
        this.instanceName = cmd.getInstanceName();
        this.unmanagedInstances = unmanagedInstances;
    }

    public String getInstanceName() {
        return instanceName;
    }

    public void setInstanceName(String instanceName) {
        this.instanceName = instanceName;
    }

    public HashMap<String, UnmanagedInstanceTO> getUnmanagedInstances() {
        return unmanagedInstances;
    }

    public void setUnmanagedInstances(HashMap<String, UnmanagedInstanceTO> unmanagedInstances) {
        this.unmanagedInstances = unmanagedInstances;
    }

    public String getString() {
        return "GetUnmanagedInstancesAnswer [instanceName=" + instanceName + "]";
    }
}
