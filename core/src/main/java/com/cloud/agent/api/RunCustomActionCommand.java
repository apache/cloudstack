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

public class RunCustomActionCommand extends Command {

    String actionName;

    Map<String, String> externalDetails;

    Long clusterId;

    public RunCustomActionCommand(String actionName, Map<String, String> externalDetails) {
        this.actionName = actionName;
        this.externalDetails = externalDetails;
    }

    public String getActionName() {
        return actionName;
    }

    public void setActionName(String actionName) {
        this.actionName = actionName;
    }

    public Map<String, String> getExternalDetails() {
        return externalDetails;
    }

    public void setExternalDetails(Map<String, String> externalDetails) {
        this.externalDetails = externalDetails;
    }

    public Long getClusterId() {
        return clusterId;
    }
    @Override
    public boolean executeInSequence() {
        return false;
    }
}
