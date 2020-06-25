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

public class RollingMaintenanceAnswer extends Answer {

    private boolean finished;
    private boolean avoidMaintenance;
    private boolean maintenaceScriptDefined;

    public RollingMaintenanceAnswer(Command command, boolean success, String details, boolean finished) {
        super(command, success, details);
        this.finished = finished;
    }

    public RollingMaintenanceAnswer(Command command, boolean isMaintenanceScript) {
        super(command, true, "");
        this.maintenaceScriptDefined = isMaintenanceScript;
    }

    public boolean isFinished() {
        return finished;
    }

    public boolean isAvoidMaintenance() {
        return avoidMaintenance;
    }

    public void setAvoidMaintenance(boolean avoidMaintenance) {
        this.avoidMaintenance = avoidMaintenance;
    }

    public boolean isMaintenaceScriptDefined() {
        return maintenaceScriptDefined;
    }

    public void setMaintenaceScriptDefined(boolean maintenaceScriptDefined) {
        this.maintenaceScriptDefined = maintenaceScriptDefined;
    }
}
