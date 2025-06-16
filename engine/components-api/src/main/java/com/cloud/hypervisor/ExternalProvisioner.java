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

import java.util.Map;

import com.cloud.agent.api.HostVmStateReportEntry;
import com.cloud.agent.api.PrepareExternalProvisioningAnswer;
import com.cloud.agent.api.PrepareExternalProvisioningCommand;
import com.cloud.agent.api.RebootAnswer;
import com.cloud.agent.api.RebootCommand;
import com.cloud.agent.api.RunCustomActionAnswer;
import com.cloud.agent.api.RunCustomActionCommand;
import com.cloud.agent.api.StartAnswer;
import com.cloud.agent.api.StartCommand;
import com.cloud.agent.api.StopAnswer;
import com.cloud.agent.api.StopCommand;
import com.cloud.utils.component.Manager;

public interface ExternalProvisioner extends Manager {

    String getExtensionEntryPoint(String relativeEntryPoint);

    String getChecksumForExtensionEntryPoint(String extensionName, String relativeEntryPoint);

    void prepareExtensionEntryPoint(String extensionName, boolean userDefined, String extensionRelativeEntryPoint);

    void cleanupExtensionEntryPoint(String extensionName, String extensionRelativeEntryPoint);

    void cleanupExtensionPayloads(String extensionName, int olderThanDays, boolean cleanupDirectory);

    PrepareExternalProvisioningAnswer prepareExternalProvisioning(String hostGuid, String extensionName, String extensionRelativeEntryPoint, PrepareExternalProvisioningCommand cmd);

    StartAnswer startInstance(String hostGuid, String extensionName, String extensionRelativeEntryPoint, StartCommand cmd);

    StopAnswer stopInstance(String hostGuid, String extensionName, String extensionRelativeEntryPoint, StopCommand cmd);

    RebootAnswer rebootInstance(String hostGuid, String extensionName, String extensionRelativeEntryPoint, RebootCommand cmd);

    StopAnswer expungeInstance(String hostGuid, String extensionName, String extensionRelativeEntryPoint, StopCommand cmd);

    Map<String, HostVmStateReportEntry> getHostVmStateReport(long hostId, String extensionName, String extensionRelativeEntryPoint);

    RunCustomActionAnswer runCustomAction(String hostGuid, String extensionName, String extensionRelativeEntryPoint, RunCustomActionCommand cmd);
}
