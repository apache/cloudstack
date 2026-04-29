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
package com.cloud.hypervisor.kvm.resource.rolling.maintenance;

import com.cloud.utils.Pair;

import java.io.File;

public interface RollingMaintenanceExecutor {

    File getStageScriptFile(String stage);
    Pair<Boolean, String> startStageExecution(String stage, File scriptFile, int timeout, String payload);
    String getStageExecutionOutput(String stage, File scriptFile);
    boolean isStageRunning(String stage, File scriptFile, String payload);
    boolean getStageExecutionSuccess(String stage, File scriptFile);
    boolean getStageAvoidMaintenance(String stage, File scriptFile);
}
