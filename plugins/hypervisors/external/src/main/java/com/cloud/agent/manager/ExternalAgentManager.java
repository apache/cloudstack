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

package com.cloud.agent.manager;

import com.cloud.agent.api.RunCustomActionAnswer;
import com.cloud.hypervisor.ExternalProvisioner;
import com.cloud.hypervisor.external.provisioner.api.ExtensionResponse;
import com.cloud.hypervisor.external.provisioner.api.ListExtensionsCmd;
import com.cloud.hypervisor.external.provisioner.api.CreateExtensionCmd;
import com.cloud.hypervisor.external.provisioner.api.RegisterExtensionCmd;
import com.cloud.hypervisor.external.provisioner.api.RunCustomActionCmd;
import com.cloud.hypervisor.external.provisioner.vo.Extension;
import com.cloud.hypervisor.external.resource.ExternalResourceBase;
import com.cloud.utils.component.Manager;

import javax.naming.ConfigurationException;
import java.util.List;
import java.util.Map;

public interface ExternalAgentManager extends Manager {

    boolean configure(String name, Map<String, Object> params) throws ConfigurationException;

    Map<ExternalResourceBase, Map<String, String>> createServerResources(Map<String, Object> params);

    ExternalProvisioner getExternalProvisioner(String provisioner);

    List<ExternalProvisioner> listExternalProvisioners();

    RunCustomActionAnswer runCustomAction(RunCustomActionCmd cmd);

    Extension createExtension(CreateExtensionCmd cmd);

   List<ExtensionResponse> listExtensions(ListExtensionsCmd cmd);

   ExtensionResponse registerExtensionWithResource(RegisterExtensionCmd cmd);
}
