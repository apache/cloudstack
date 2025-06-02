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
    Map<String, Object> parameters;
    Map<String, String> details;

    public RunCustomActionCommand(String actionName, Map<String, Object> parameters, Map<String, String> details) {
        this.actionName = actionName;
        this.parameters = parameters;
        this.details = details;
    }

    public String getActionName() {
        return actionName;
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }

    public Map<String, String> getDetails() {
        return details;
    }

    @Override
    public boolean executeInSequence() {
        return false;
    }
}
