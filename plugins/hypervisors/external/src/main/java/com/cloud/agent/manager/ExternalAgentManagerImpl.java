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

import com.cloud.hypervisor.ExternalProvisioner;
import com.cloud.hypervisor.HypervisorGuruManagerImpl;
import com.cloud.hypervisor.external.resource.ExternalResourceBase;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.exception.CloudRuntimeException;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import javax.naming.ConfigurationException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ExternalAgentManagerImpl extends ManagerBase implements ExternalAgentManager, Configurable {
    private static final Logger logger = Logger.getLogger(ExternalAgentManagerImpl.class);

    public static final ConfigKey<Boolean> expectMacAddressFromExternalProvisioner = new ConfigKey<>(Boolean.class, "expect.macaddress.from.external.provisioner", "Advanced", "true",
            "Sample external provisioning config, any value that has to be sent", true, ConfigKey.Scope.Cluster, null);

    private List<ExternalProvisioner> externalProvisioners;

    protected static Map<String, ExternalProvisioner> externalProvisionerMap = new HashMap<>();

    public List<ExternalProvisioner> getExternalProvisioners() {
        return externalProvisioners;
    }

    public void setExternalProvisioners(final List<ExternalProvisioner> externalProvisioners) {
        this.externalProvisioners = externalProvisioners;
    }

    public boolean configure(String name, Map<String, Object> params) {
        return true;
    }

    @Override
    public boolean start() {
        initializeExternalProvisionerMap();
        return true;
    }

    protected void initializeExternalProvisionerMap() {
        logger.info("Initializing the external providers");
        if (StringUtils.isNotEmpty(HypervisorGuruManagerImpl.ExternalProvisioners.value())) {
            if (externalProvisioners != null) {
                List<String> externalProvisionersListFromConfig = Arrays.stream(HypervisorGuruManagerImpl.ExternalProvisioners.value().split(","))
                        .map(String::trim)
                        .map(String::toLowerCase)
                        .collect(Collectors.toList());
                logger.info(String.format("Found these external provisioners from global setting %s", externalProvisionersListFromConfig));
                logger.info(String.format("Found these external provisioners from the available plugins %s", externalProvisioners));
                for (final ExternalProvisioner externalProvisioner : externalProvisioners) {
                    if (externalProvisionersListFromConfig.contains(externalProvisioner.getName().toLowerCase())) {
                        externalProvisionerMap.put(externalProvisioner.getName().toLowerCase(), externalProvisioner);
                    }
                }
                logger.info(String.format("List of external providers that are enabled are %s", externalProvisionerMap));
            } else {
                logger.info("No external provisioners found to initialize");
            }
        } else {
            logger.info("No external provisioners found to initialise, please check global setting external.provisioners and available plugins");
        }
    }

    @Override
    public ExternalProvisioner getExternalProvisioner(String provisioner) {
        if (StringUtils.isEmpty(provisioner)) {
            throw new CloudRuntimeException("External provisioner name cannot be empty");
        }
        if (!externalProvisionerMap.containsKey(provisioner.toLowerCase())) {
            throw new CloudRuntimeException(String.format("Failed to find external provisioner by the name: %s.", provisioner));
        }
        return externalProvisionerMap.get(provisioner.toLowerCase());
    }

    @Override
    public List<ExternalProvisioner> listExternalProvisioners() {
        return externalProvisioners;
    }

    public Map<ExternalResourceBase, Map<String, String>> createServerResources(Map<String, Object> params) {

        Map<String, String> args = new HashMap<>();
        Map<ExternalResourceBase, Map<String, String>> newResources = new HashMap<>();
        ExternalResourceBase agentResource;
        String provisionerName = (String) params.get(ApiConstants.EXTERNAL_PROVISIONER);
        logger.debug("Checking if the provided external provisioner is valid before ");
        ExternalProvisioner externalProvisioner = getExternalProvisioner(provisionerName);
        if (externalProvisioner == null) {
            throw new CloudRuntimeException(String.format("Unable to find the provisioner with the name %s", provisionerName));
        }
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
