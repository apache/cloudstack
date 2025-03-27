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
import com.cloud.agent.api.RunCustomActionAnswer;
import com.cloud.agent.api.RunCustomActionCommand;
import com.cloud.dc.HostPodVO;
import com.cloud.dc.dao.HostPodDao;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.DiscoveryException;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.exception.ResourceInUseException;
import com.cloud.host.DetailVO;
import com.cloud.host.Host;
import com.cloud.host.dao.HostDao;
import com.cloud.host.dao.HostDetailsDao;
import com.cloud.hypervisor.ExternalProvisioner;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.hypervisor.HypervisorGuruManagerImpl;
import com.cloud.hypervisor.external.provisioner.api.ExtensionResponse;
import com.cloud.hypervisor.external.provisioner.api.ListExtensionsCmd;
import com.cloud.hypervisor.external.provisioner.api.RegisterExtensionCmd;
import com.cloud.hypervisor.external.provisioner.api.RunCustomActionCmd;
import com.cloud.hypervisor.external.provisioner.dao.ExternalOrchestratorDao;
import com.cloud.hypervisor.external.provisioner.dao.ExternalOrchestratorDetailDao;
import com.cloud.hypervisor.external.provisioner.dao.ExternalOrchestratorDetailVO;
import com.cloud.hypervisor.external.provisioner.simpleprovisioner.SimpleExternalProvisioner;
import com.cloud.hypervisor.external.provisioner.vo.Extension;
import com.cloud.hypervisor.external.provisioner.vo.ExtensionVO;
import com.cloud.hypervisor.external.resource.ExternalResourceBase;
import com.cloud.org.Cluster;
import com.cloud.resource.ResourceService;
import com.cloud.utils.Pair;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.component.PluggableService;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.exception.CloudRuntimeException;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.command.admin.cluster.AddClusterCmd;
import org.apache.cloudstack.api.command.admin.host.AddHostCmd;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;
import org.apache.commons.lang3.StringUtils;

import javax.inject.Inject;
import javax.naming.ConfigurationException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ExternalAgentManagerImpl extends ManagerBase implements ExternalAgentManager, Configurable, PluggableService {

    @Inject
    AgentManager agentMgr;

    @Inject
    ExternalOrchestratorDao externalOrchestratorDao;

    @Inject
    ExternalOrchestratorDetailDao externalOrchestratorDetailDao;

    @Inject
    HostPodDao podDao;

    @Inject
    HostDao hostDao;

    @Inject
    HostDetailsDao hostDetailsDao;

    @Inject
    public ResourceService resourceService;

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

    @Override
    public List<Class<?>> getCommands() {
        List<Class<?>> cmds = new ArrayList<Class<?>>();
        cmds.add(RunCustomActionCmd.class);
        cmds.add(RegisterExtensionCmd.class);
        cmds.add(ListExtensionsCmd.class);
        return cmds;
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

    @Override
    public RunCustomActionAnswer runCustomAction(RunCustomActionCmd cmd) {
        String action =  cmd.getActionName();
        Long extensionId = cmd.getExtensionId();
        Map<String, String> externalDetails = cmd.getExternalDetails();

        List<DetailVO> hostDetailsByExtension = hostDetailsDao.findByNameAndValue(ApiConstants.EXTENSION_ID, String.valueOf(extensionId));
        DetailVO hostDetail = hostDetailsByExtension.get(0);

        RunCustomActionAnswer answer;
        try {
            RunCustomActionCommand runCustomActionCommand = new RunCustomActionCommand(action, externalDetails);
            answer = (RunCustomActionAnswer) agentMgr.send(hostDetail.getHostId(), runCustomActionCommand);
        } catch (AgentUnavailableException e) {
            throw new CloudRuntimeException("Unable to run custom action");
        } catch (OperationTimedoutException e) {
            throw new CloudRuntimeException("Running custom action timed out, please try again");
        }

        return answer;
    }

    @Override
    public Extension registerExternalOrchestrator(RegisterExtensionCmd cmd) {
        ExtensionVO extension = new ExtensionVO();
        extension.setName(cmd.getName());
        extension.setType(cmd.getType());
        extension.setPodId(cmd.getPodId());
        ExtensionVO savedExtension = externalOrchestratorDao.persist(extension);

        Map<String, String> externalDetails = cmd.getExternalDetails();
        List<ExternalOrchestratorDetailVO> detailsVOList = new ArrayList<>();
        if (externalDetails != null && !externalDetails.isEmpty()) {
            for (Map.Entry<String, String> entry : externalDetails.entrySet()) {
                detailsVOList.add(new ExternalOrchestratorDetailVO(savedExtension.getId(), entry.getKey(), entry.getValue()));
            }
            externalOrchestratorDetailDao.saveDetails(detailsVOList);
        }

        createRequiredResourcesForExtension(savedExtension, cmd);

        return savedExtension;
    }

    @Override
    public List<ExtensionResponse> listExtensions(ListExtensionsCmd cmd) {
        Long id = cmd.getExtensionId();
        String name = cmd.getName();
        String keyword = cmd.getKeyword();
        final SearchBuilder<ExtensionVO> sb = externalOrchestratorDao.createSearchBuilder();
        final Filter searchFilter = new Filter(ExtensionVO.class, "id", false, cmd.getStartIndex(), cmd.getPageSizeVal());

        sb.and("id", sb.entity().getId(), SearchCriteria.Op.EQ);
        sb.and("name", sb.entity().getName(), SearchCriteria.Op.EQ);
        sb.and("keyword", sb.entity().getName(), SearchCriteria.Op.LIKE);
        final SearchCriteria<ExtensionVO> sc = sb.create();

        if (id != null) {
            sc.setParameters("id", id);
        }

        if (name != null) {
            sc.setParameters("name", name);
        }

        if (keyword != null) {
            sc.setParameters("keyword",  "%" + keyword + "%");
        }

        final Pair<List<ExtensionVO>, Integer> result = externalOrchestratorDao.searchAndCount(sc, searchFilter);
        List<ExtensionResponse> responses = new ArrayList<>();
        for (ExtensionVO extension : result.first()) {
            Map<String, String> details = externalOrchestratorDetailDao.listDetailsKeyPairs(extension.getId());
            ExtensionResponse response = new ExtensionResponse(extension.getName(), extension.getType(), extension.getPodId(), extension.getUuid(), details);
            String destinationPath = String.format(SimpleExternalProvisioner.EXTENSION_SCRIPT_PATH, extension.getId());
            response.setScriptPath(destinationPath);
            response.setObjectName(ApiConstants.EXTENSIONS);
            responses.add(response);
        }

        return responses;
    }

    private void createRequiredResourcesForExtension(ExtensionVO extension, RegisterExtensionCmd registerExtensionCmd) {
        String clusterName = extension.getName() + "-" + extension.getId() + "-cluster";
        HostPodVO pod = podDao.findById(extension.getPodId());
        AddClusterCmd clusterCmd = new AddClusterCmd(clusterName, pod.getDataCenterId(), pod.getId(), Cluster.ClusterType.CloudManaged.toString(),
                Hypervisor.HypervisorType.External.toString(), SimpleExternalProvisioner.class.getSimpleName());
        List<? extends Cluster> clusters;
        try {
            clusters = resourceService.discoverCluster(clusterCmd);
        } catch (DiscoveryException | ResourceInUseException e) {
            throw new CloudRuntimeException("Unable to create cluster");
        }

        Cluster cluster = clusters.get(0);

        String hosturl = "http://" + extension.getName() + "-" + extension.getId() + "-host";
        AddHostCmd addHostCmd = new AddHostCmd(pod.getDataCenterId(), pod.getId(), cluster.getId(), Hypervisor.HypervisorType.External.toString(),
                "External", "External", hosturl, registerExtensionCmd.getDetails(), extension.getId());

        List<? extends Host> hosts;
        try {
            hosts = resourceService.discoverHosts(addHostCmd);
        } catch (DiscoveryException e) {
            throw new CloudRuntimeException("Unable to add host");
        }
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
