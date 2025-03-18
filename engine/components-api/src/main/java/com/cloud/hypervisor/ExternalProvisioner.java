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
package com.cloud.hypervisor;

import com.cloud.agent.api.HostVmStateReportEntry;
import com.cloud.agent.api.PrepareExternalProvisioningAnswer;
import com.cloud.agent.api.PrepareExternalProvisioningCommand;
import com.cloud.agent.api.PostExternalProvisioningAnswer;
import com.cloud.agent.api.PostExternalProvisioningCommand;
import com.cloud.agent.api.RebootAnswer;
import com.cloud.agent.api.RebootCommand;
import com.cloud.agent.api.RunCustomActionAnswer;
import com.cloud.agent.api.RunCustomActionCommand;
import com.cloud.agent.api.StartAnswer;
import com.cloud.agent.api.StartCommand;
import com.cloud.agent.api.StopAnswer;
import com.cloud.agent.api.StopCommand;
import com.cloud.utils.component.Adapter;

import java.util.HashMap;

public interface ExternalProvisioner extends Adapter {
    /**
     * Returns the unique name of the provider
     * @return returns provider name
     */
    String getName();

    /**
     * Returns description about the provider
     * @return returns description
     */
    String getDescription();

    PrepareExternalProvisioningAnswer prepareExternalProvisioning(PrepareExternalProvisioningCommand cmd);

    StartAnswer startInstance(StartCommand cmd);

    StopAnswer stopInstance(StopCommand cmd);

    RebootAnswer rebootInstance(RebootCommand cmd);

    StopAnswer expungeInstance(StopCommand cmd);

    PostExternalProvisioningAnswer postsetupInstance(PostExternalProvisioningCommand cmd);

    HashMap<String, HostVmStateReportEntry> getHostVmStateReport(Long hostId);

    RunCustomActionAnswer runCustomAction(RunCustomActionCommand cmd);
}
