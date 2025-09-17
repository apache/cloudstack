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

package org.apache.cloudstack.framework.extensions.manager;


import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.response.ExtensionCustomActionResponse;
import org.apache.cloudstack.api.response.ExtensionResponse;
import org.apache.cloudstack.extension.CustomActionResultResponse;
import org.apache.cloudstack.extension.Extension;
import org.apache.cloudstack.extension.ExtensionCustomAction;
import org.apache.cloudstack.extension.ExtensionResourceMap;
import org.apache.cloudstack.framework.extensions.api.AddCustomActionCmd;
import org.apache.cloudstack.framework.extensions.api.CreateExtensionCmd;
import org.apache.cloudstack.framework.extensions.api.DeleteCustomActionCmd;
import org.apache.cloudstack.framework.extensions.api.DeleteExtensionCmd;
import org.apache.cloudstack.framework.extensions.api.ListCustomActionCmd;
import org.apache.cloudstack.framework.extensions.api.ListExtensionsCmd;
import org.apache.cloudstack.framework.extensions.api.RegisterExtensionCmd;
import org.apache.cloudstack.framework.extensions.api.RunCustomActionCmd;
import org.apache.cloudstack.framework.extensions.api.UnregisterExtensionCmd;
import org.apache.cloudstack.framework.extensions.api.UpdateCustomActionCmd;
import org.apache.cloudstack.framework.extensions.api.UpdateExtensionCmd;
import org.apache.cloudstack.framework.extensions.command.ExtensionServerActionBaseCommand;

import com.cloud.host.Host;
import com.cloud.org.Cluster;
import com.cloud.utils.Pair;
import com.cloud.utils.component.Manager;

public interface ExtensionsManager extends Manager {

    String getExtensionsPath();

    Extension createExtension(CreateExtensionCmd cmd);

    boolean prepareExtensionPathAcrossServers(Extension extension);

    List<ExtensionResponse> listExtensions(ListExtensionsCmd cmd);

    boolean deleteExtension(DeleteExtensionCmd cmd);

    Extension unregisterExtensionWithResource(UnregisterExtensionCmd cmd);

    Extension updateExtension(UpdateExtensionCmd cmd);

    Extension registerExtensionWithResource(RegisterExtensionCmd cmd);

    ExtensionResponse createExtensionResponse(Extension extension, EnumSet<ApiConstants.ExtensionDetails> viewDetails);

    ExtensionResourceMap registerExtensionWithCluster(Cluster cluster, Extension extension, Map<String, String> externalDetails);

    void unregisterExtensionWithCluster(Cluster cluster, Long extensionId);

    CustomActionResultResponse runCustomAction(RunCustomActionCmd cmd);

    ExtensionCustomAction addCustomAction(AddCustomActionCmd cmd);

    boolean deleteCustomAction(DeleteCustomActionCmd cmd);

    List<ExtensionCustomActionResponse> listCustomActions(ListCustomActionCmd cmd);

    ExtensionCustomAction updateCustomAction(UpdateCustomActionCmd cmd);

    ExtensionCustomActionResponse createCustomActionResponse(ExtensionCustomAction customAction);

    Map<String, Map<String, String>> getExternalAccessDetails(Host host, Map<String, String> vmDetails);

    String handleExtensionServerCommands(ExtensionServerActionBaseCommand cmd);

    Pair<Boolean, ExtensionResourceMap> extensionResourceMapDetailsNeedUpdate(final long resourceId,
                      final ExtensionResourceMap.ResourceType resourceType, final Map<String, String> details);

    void updateExtensionResourceMapDetails(final long extensionResourceMapId, final Map<String, String> details);
}
