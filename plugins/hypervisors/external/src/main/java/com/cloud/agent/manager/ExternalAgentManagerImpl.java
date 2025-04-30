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

package com.cloud.agent.manager;

import com.cloud.agent.AgentManager;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.host.dao.HostDao;
import com.cloud.host.dao.HostDetailsDao;
import com.cloud.hypervisor.external.resource.ExternalResourceBase;
import com.cloud.resource.ResourceService;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.component.PluggableService;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;

import javax.inject.Inject;
import javax.naming.ConfigurationException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExternalAgentManagerImpl extends ManagerBase implements ExternalAgentManager, Configurable, PluggableService {

    @Inject
    AgentManager agentMgr;

    @Inject
    ClusterDao clusterDao;

    @Inject
    HostDao hostDao;

    @Inject
    HostDetailsDao hostDetailsDao;

    @Inject
    public ResourceService resourceService;

    public static final ConfigKey<Boolean> expectMacAddressFromExternalProvisioner = new ConfigKey<>(Boolean.class, "expect.macaddress.from.external.provisioner", "Advanced", "true",
            "Sample external provisioning config, any value that has to be sent", true, ConfigKey.Scope.Cluster, null);

    public boolean configure(String name, Map<String, Object> params) {
        return true;
    }

    @Override
    public boolean start() {
        return true;
    }

    @Override
    public List<Class<?>> getCommands() {
        List<Class<?>> cmds = new ArrayList<Class<?>>();
        return cmds;
    }

    public Map<ExternalResourceBase, Map<String, String>> createServerResources(Map<String, Object> params) {

        Map<String, String> args = new HashMap<>();
        Map<ExternalResourceBase, Map<String, String>> newResources = new HashMap<>();
        ExternalResourceBase agentResource;
        synchronized (this) {
            String guid = (String)params.get("guid");
            agentResource = new ExternalResourceBase();
            if (agentResource != null) {
                try {
                    agentResource.start();
                    agentResource.configure("ExternalHost-" + guid, params);
                    newResources.put(agentResource, args);
                } catch (ConfigurationException e) {
                    logger.error("error while configuring server resource" + e.getMessage());
                }
            }
        }
        return newResources;
    }

    @Override
    public String getConfigComponentName() {
        return ExternalAgentManagerImpl.class.getSimpleName();
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey<?>[] {expectMacAddressFromExternalProvisioner};
    }
}
