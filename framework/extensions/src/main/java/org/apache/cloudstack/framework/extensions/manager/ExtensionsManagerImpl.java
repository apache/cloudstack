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

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.cloudstack.acl.Role;
import org.apache.cloudstack.acl.RoleService;
import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.response.ExtensionCustomActionParameterResponse;
import org.apache.cloudstack.api.response.ExtensionCustomActionResponse;
import org.apache.cloudstack.api.response.ExtensionResourceResponse;
import org.apache.cloudstack.api.response.ExtensionResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.extension.CustomActionResultResponse;
import org.apache.cloudstack.extension.Extension;
import org.apache.cloudstack.extension.ExtensionCustomAction;
import org.apache.cloudstack.extension.ExtensionHelper;
import org.apache.cloudstack.extension.NetworkCustomActionProvider;
import org.apache.cloudstack.extension.ExtensionResourceMap;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;
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
import org.apache.cloudstack.framework.extensions.api.UpdateRegisteredExtensionCmd;
import org.apache.cloudstack.framework.extensions.command.CleanupExtensionFilesCommand;
import org.apache.cloudstack.framework.extensions.command.ExtensionRoutingUpdateCommand;
import org.apache.cloudstack.framework.extensions.command.ExtensionServerActionBaseCommand;
import org.apache.cloudstack.framework.extensions.command.GetExtensionPathChecksumCommand;
import org.apache.cloudstack.framework.extensions.command.PrepareExtensionPathCommand;
import org.apache.cloudstack.framework.extensions.dao.ExtensionCustomActionDao;
import org.apache.cloudstack.framework.extensions.dao.ExtensionCustomActionDetailsDao;
import org.apache.cloudstack.framework.extensions.dao.ExtensionDao;
import org.apache.cloudstack.framework.extensions.dao.ExtensionDetailsDao;
import org.apache.cloudstack.framework.extensions.dao.ExtensionResourceMapDao;
import org.apache.cloudstack.framework.extensions.dao.ExtensionResourceMapDetailsDao;
import org.apache.cloudstack.framework.extensions.vo.ExtensionCustomActionDetailsVO;
import org.apache.cloudstack.framework.extensions.vo.ExtensionCustomActionVO;
import org.apache.cloudstack.framework.extensions.vo.ExtensionDetailsVO;
import org.apache.cloudstack.framework.extensions.vo.ExtensionResourceMapDetailsVO;
import org.apache.cloudstack.framework.extensions.vo.ExtensionResourceMapVO;
import org.apache.cloudstack.framework.extensions.vo.ExtensionVO;
import org.apache.cloudstack.managed.context.ManagedContextRunnable;
import org.apache.cloudstack.management.ManagementServerHost;
import org.apache.cloudstack.utils.identity.ManagementServerNode;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.GetExternalConsoleCommand;
import com.cloud.agent.api.RunCustomActionAnswer;
import com.cloud.agent.api.RunCustomActionCommand;
import com.cloud.agent.api.to.VirtualMachineTO;
import com.cloud.alert.AlertManager;
import com.cloud.cluster.ClusterManager;
import com.cloud.cluster.ManagementServerHostVO;
import com.cloud.cluster.dao.ManagementServerHostDao;
import com.cloud.dc.ClusterVO;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.event.ActionEvent;
import com.cloud.event.EventTypes;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.host.dao.HostDetailsDao;
import com.cloud.hypervisor.ExternalProvisioner;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.network.Network;
import com.cloud.network.Network.Capability;
import com.cloud.network.Network.Service;
import com.cloud.network.NetworkModel;
import com.cloud.network.PhysicalNetworkServiceProvider;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkServiceMapDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.dao.PhysicalNetworkServiceProviderDao;
import com.cloud.network.dao.PhysicalNetworkServiceProviderVO;
import com.cloud.network.element.NetworkElement;
import com.cloud.network.dao.PhysicalNetworkDao;
import com.cloud.network.dao.PhysicalNetworkVO;
import com.cloud.org.Cluster;
import com.cloud.serializer.GsonHelper;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.user.Account;
import com.cloud.user.AccountService;
import com.cloud.utils.Pair;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.component.PluggableService;
import com.cloud.utils.concurrency.NamedThreadFactory;
import com.cloud.utils.db.EntityManager;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GlobalLock;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallbackWithException;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineManager;
import com.cloud.vm.VirtualMachineProfile;
import com.cloud.vm.VirtualMachineProfileImpl;
import com.cloud.vm.VmDetailConstants;
import com.cloud.vm.dao.VMInstanceDao;

public class ExtensionsManagerImpl extends ManagerBase implements ExtensionsManager, ExtensionHelper, PluggableService, Configurable {

    ConfigKey<Integer> PathStateCheckInterval = new ConfigKey<>("Advanced", Integer.class,
            "extension.path.state.check.interval", "300",
            "Interval (in seconds) for checking state of extensions path",
            false, ConfigKey.Scope.Global);

    @Inject
    ExtensionDao extensionDao;

    @Inject
    ExtensionDetailsDao extensionDetailsDao;

    @Inject
    ExtensionResourceMapDao extensionResourceMapDao;

    @Inject
    ExtensionResourceMapDetailsDao extensionResourceMapDetailsDao;

    @Inject
    ClusterDao clusterDao;

    @Inject
    PhysicalNetworkDao physicalNetworkDao;

    @Inject
    PhysicalNetworkServiceProviderDao physicalNetworkServiceProviderDao;

    @Inject
    AgentManager agentMgr;

    @Inject
    HostDao hostDao;

    @Inject
    HostDetailsDao hostDetailsDao;

    @Inject
    ExternalProvisioner externalProvisioner;

    @Inject
    ExtensionCustomActionDao extensionCustomActionDao;

    @Inject
    ExtensionCustomActionDetailsDao extensionCustomActionDetailsDao;

    @Inject
    VMInstanceDao vmInstanceDao;

    @Inject
    VirtualMachineManager virtualMachineManager;

    @Inject
    EntityManager entityManager;

    @Inject
    ManagementServerHostDao managementServerHostDao;

    @Inject
    ClusterManager clusterManager;

    @Inject
    AlertManager alertManager;

    @Inject
    VMTemplateDao templateDao;

    @Inject
    NetworkDao networkDao;

    @Inject
    NetworkServiceMapDao networkServiceMapDao;

    @Inject
    NetworkModel networkModel;

    @Inject
    RoleService roleService;

    @Inject
    AccountService accountService;

    // Map of in-built extension names and their reserved resource details that shouldn't be accessible to end-users
    protected static final Map<String, List<String>> INBUILT_RESERVED_RESOURCE_DETAILS = Map.of(
            "proxmox", List.of("proxmox_vmid")
    );

    private ScheduledExecutorService extensionPathStateCheckExecutor;

    protected String getDefaultExtensionRelativePath(String name) {
        String safeName = Extension.getDirectoryName(name);
        return String.format("%s%s%s.sh", safeName, File.separator, safeName);
    }

    protected String getValidatedExtensionRelativePath(String name, String relativePathPath) {
        String safeName = Extension.getDirectoryName(name);
        String normalizedPath = relativePathPath.replace("\\", "/");
        if (normalizedPath.startsWith("/")) {
            normalizedPath = normalizedPath.substring(1);
        }
        if (normalizedPath.equals(safeName)) {
            normalizedPath = safeName + "/" + safeName;
        } else if (!normalizedPath.startsWith(safeName + "/")) {
            normalizedPath = safeName + "/" + normalizedPath;
        }
        Path pathObj = Paths.get(normalizedPath);
        int subDirCount = pathObj.getNameCount() - 1;
        if (subDirCount > 2) {
            throw new InvalidParameterException("Entry point path cannot be nested more than two sub-directories deep");
        }
        return normalizedPath;
    }

    protected Pair<Boolean, String> getResultFromAnswersString(String answersStr, Extension extension,
                   ManagementServerHostVO msHost, String op) {
        Answer[] answers = null;
        try {
            answers = GsonHelper.getGson().fromJson(answersStr, Answer[].class);
        } catch (Exception e) {
            logger.error("Failed to parse answer JSON during {} for {} on {}: {}",
                    op, extension, msHost, e.getMessage(), e);
            return new Pair<>(false, e.getMessage());
        }
        Answer answer = answers != null && answers.length > 0 ? answers[0] : null;
        boolean result = false;
        String details = "Unknown error";
        if (answer != null) {
            result = answer.getResult();
            details = answer.getDetails();
        }
        if (!result) {
            logger.error("Failed to {} for {} on {} due to {}", op, extension, msHost, details);
            return new Pair<>(false, details);
        }
        return new Pair<>(true, details);
    }

    protected boolean prepareExtensionPathOnMSPeer(Extension extension, ManagementServerHostVO msHost) {
        final String msPeer = Long.toString(msHost.getMsid());
        logger.debug("Sending prepare extension path for {} command to MS: {}", extension, msPeer);
        final Command[] commands = new Command[1];
        commands[0] = new PrepareExtensionPathCommand(ManagementServerNode.getManagementServerId(), extension);
        String answersStr = clusterManager.execute(msPeer, 0L, GsonHelper.getGson().toJson(commands), true);
        return getResultFromAnswersString(answersStr, extension, msHost, "prepare path").first();
    }

    protected Pair<Boolean, String> prepareExtensionPathOnCurrentServer(String name, boolean userDefined,
                                                                              String relativePath) {
        try {
            externalProvisioner.prepareExtensionPath(name, userDefined, relativePath);
        } catch (CloudRuntimeException e) {
            logger.error("Failed to prepare path for Extension [name: {}, userDefined: {}, relativePath: {}] on this server",
                    name, userDefined, relativePath, e);
            return new Pair<>(false, e.getMessage());
        }
        return new Pair<>(true, null);
    }

    protected boolean cleanupExtensionFilesOnMSPeer(Extension extension, ManagementServerHostVO msHost) {
        final String msPeer = Long.toString(msHost.getMsid());
        logger.debug("Sending cleanup extension files for {} command to MS: {}", extension, msPeer);
        final Command[] commands = new Command[1];
        commands[0] = new CleanupExtensionFilesCommand(ManagementServerNode.getManagementServerId(), extension);
        String answersStr = clusterManager.execute(msPeer, 0L, GsonHelper.getGson().toJson(commands), true);
        return getResultFromAnswersString(answersStr, extension, msHost, "cleanup files").first();
    }

    protected Pair<Boolean, String> cleanupExtensionFilesOnCurrentServer(String name, String relativePath) {
        try {
            externalProvisioner.cleanupExtensionPath(name, relativePath);
            externalProvisioner.cleanupExtensionData(name, 0, true);
        } catch (CloudRuntimeException e) {
            logger.error("Failed to cleanup files for Extension [name: {}, relativePath: {}] on this server",
                    name, relativePath, e);
            return new Pair<>(false, e.getMessage());
        }
        return new Pair<>(true, null);
    }

    protected void cleanupExtensionFilesAcrossServers(Extension extension) {
        boolean cleanedUp = true;
        List<ManagementServerHostVO> msHosts = managementServerHostDao.listBy(ManagementServerHost.State.Up);
        for (ManagementServerHostVO msHost : msHosts) {
            if (msHost.getMsid() == ManagementServerNode.getManagementServerId()) {
                cleanedUp = cleanedUp && cleanupExtensionFilesOnCurrentServer(extension.getName(),
                        extension.getRelativePath()).first();
                continue;
            }
            cleanedUp = cleanedUp && cleanupExtensionFilesOnMSPeer(extension, msHost);
        }
        if (!cleanedUp) {
            throw new CloudRuntimeException("Extension is deleted but its files are not cleaned up across servers");
        }
    }

    protected Pair<Boolean, String> getChecksumForExtensionPathOnMSPeer(Extension extension, ManagementServerHostVO msHost) {
        final String msPeer = Long.toString(msHost.getMsid());
        logger.debug("Retrieving checksum for {} from MS: {}", extension, msPeer);
        final Command[] cmds = new Command[1];
        cmds[0] = new GetExtensionPathChecksumCommand(ManagementServerNode.getManagementServerId(),
                extension);
        String answersStr = clusterManager.execute(msPeer, 0L, GsonHelper.getGson().toJson(cmds), true);
        return getResultFromAnswersString(answersStr, extension, msHost, "get path checksum");
    }

    protected List<ExtensionResourceMapDetailsVO> buildExtensionResourceDetailsArray(long extensionResourceMapId,
            Map<String, String> details) {
        List<ExtensionResourceMapDetailsVO> detailsList = new ArrayList<>();
        if (MapUtils.isEmpty(details)) {
            return detailsList;
        }
        for (Map.Entry<String, String> entry : details.entrySet()) {
            boolean display = !SENSITIVE_DETAIL_KEYS.contains(entry.getKey().toLowerCase());
            detailsList.add(new ExtensionResourceMapDetailsVO(extensionResourceMapId, entry.getKey(),
                    entry.getValue(), display));
        }
        return detailsList;
    }

    protected void appendHiddenExtensionResourceDetails(long extensionResourceMapId,
            List<ExtensionResourceMapDetailsVO> detailsList) {
        if (CollectionUtils.isEmpty(detailsList)) {
            return;
        }
        Map<String, String> hiddenDetails = extensionResourceMapDetailsDao.listDetailsKeyPairs(extensionResourceMapId, false);
        if (MapUtils.isEmpty(hiddenDetails)) {
            return;
        }
        Set<String> requestedKeys = detailsList.stream()
                .map(ExtensionResourceMapDetailsVO::getName)
                .collect(Collectors.toSet());
        hiddenDetails.forEach((key, value) -> {
            if (!requestedKeys.contains(key)) {
                detailsList.add(new ExtensionResourceMapDetailsVO(extensionResourceMapId, key, value, false));
            }
        });
    }

    protected List<ExtensionCustomAction.Parameter> getParametersListFromMap(String actionName, Map parametersMap) {
        if (MapUtils.isEmpty(parametersMap)) {
            return Collections.emptyList();
        }
        List<ExtensionCustomAction.Parameter> parameters = new ArrayList<>();
        for (Map<String, String> entry : (Collection<Map<String, String>>)parametersMap.values()) {
            ExtensionCustomAction.Parameter parameter = ExtensionCustomAction.Parameter.fromMap(entry);
            logger.debug("Adding {} for custom action [{}]", parameter, actionName);
            parameters.add(parameter);
        }
        return parameters;
    }

    protected void unregisterExtensionWithCluster(String clusterUuid, Long extensionId) {
        ClusterVO cluster = clusterDao.findByUuid(clusterUuid);
        if (cluster == null) {
            throw new InvalidParameterValueException("Unable to find cluster with given ID");
        }
        unregisterExtensionWithCluster(cluster, extensionId);
    }

    protected Extension getExtensionFromResource(ExtensionCustomAction.ResourceType resourceType, String resourceUuid) {
        Object object = entityManager.findByUuid(resourceType.getAssociatedClass(), resourceUuid);
        if (object == null) {
            return null;
        }
        Long clusterId = null;
        if (resourceType == ExtensionCustomAction.ResourceType.VirtualMachine) {
            VirtualMachine vm = (VirtualMachine) object;
            Pair<Long, Long> clusterHostId = virtualMachineManager.findClusterAndHostIdForVm(vm, false);
            clusterId = clusterHostId.first();
            if (clusterId == null) {
                return null;
            }
            ExtensionResourceMapVO mapVO =
                    extensionResourceMapDao.findByResourceIdAndType(clusterId, ExtensionResourceMap.ResourceType.Cluster);
            if (mapVO == null) {
                return null;
            }
            return extensionDao.findById(mapVO.getExtensionId());
        } else if (resourceType == ExtensionCustomAction.ResourceType.Network) {
            Network network = (Network) object;
            Long physicalNetworkId = network.getPhysicalNetworkId();
            if (physicalNetworkId == null) {
                return null;
            }
            // Use provider-based lookup: match the network's service-map providers
            // against extension names registered on the physical network.
            // This correctly handles multiple different extensions on the same physical network.
            List<String> providers = networkServiceMapDao.getDistinctProviders(network.getId());
            if (CollectionUtils.isNotEmpty(providers)) {
                for (String providerName : providers) {
                    Extension ext = getExtensionForPhysicalNetworkAndProvider(physicalNetworkId, providerName);
                    if (ext != null) {
                        return ext;
                    }
                }
            }
            // Fallback: return the first extension registered on the physical network
            List<ExtensionResourceMapVO> maps = extensionResourceMapDao.listByResourceIdAndType(
                    physicalNetworkId, ExtensionResourceMap.ResourceType.PhysicalNetwork);
            if (CollectionUtils.isEmpty(maps)) {
                return null;
            }
            return extensionDao.findById(maps.get(0).getExtensionId());
        }
        return null;
    }

    protected String getActionMessage(boolean success, ExtensionCustomAction action, Extension extension,
                  ExtensionCustomAction.ResourceType resourceType, Object resource) {
        String  msg = success ? action.getSuccessMessage() : action.getErrorMessage();
        if (StringUtils.isBlank(msg)) {
            return success ? String.format("Successfully completed %s", action.getName()) :
                    String.format("Failed to complete %s", action.getName());
        }
        Map<String, String> values = new HashMap<>();
        values.put("actionName", action.getName());
        values.put("extensionName", extension.getName());
        if (msg.contains("{{resourceName}}")) {
            String resourceName = resourceType.name();
            try {
                Method getNameMethod = resource.getClass().getMethod("getName");
                Object result = getNameMethod.invoke(resource);
                if (result instanceof String) {
                    resourceName = (String) result;
                }
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                logger.trace("Failed to get name for given resource of type: {}", resourceType, e);
            }
            values.put("resourceName", resourceName);
        }
        String result = msg;
        for (Map.Entry<String, String> entry : values.entrySet()) {
            result = result.replace("{{" + entry.getKey() + "}}", entry.getValue());
        }
        return result;
    }

    protected Map<String, String> getFilteredExternalDetails(Map<String, String> details) {
        if (MapUtils.isEmpty(details)) {
            return new HashMap<>();
        }
        return details.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(VmDetailConstants.EXTERNAL_DETAIL_PREFIX))
                .collect(Collectors.toMap(
                        entry -> entry.getKey().substring(VmDetailConstants.EXTERNAL_DETAIL_PREFIX.length()),
                        Map.Entry::getValue
                ));
    }

    protected void sendExtensionPathNotReadyAlert(Extension extension) {
        String msg = String.format("Path for %s not ready across management servers",
                extension);
        if (!Extension.State.Enabled.equals(extension.getState())) {
            logger.warn(msg);
            return;
        }
        alertManager.sendAlert(AlertManager.AlertType.ALERT_TYPE_EXTENSION_PATH_NOT_READY, 0L, 0L, msg, msg);
    }

    protected void updateExtensionPathReady(Extension extension, boolean ready) {
        if (!ready) {
            sendExtensionPathNotReadyAlert(extension);
        }
        if (extension.isPathReady() == ready) {
            return;
        }
        ExtensionVO extensionVO = extensionDao.createForUpdate(extension.getId());
        extensionVO.setPathReady(ready);
        extensionDao.update(extension.getId(), extensionVO);
        updateAllExtensionHosts(extension, null, false);
    }

    protected void disableExtension(long extensionId) {
        ExtensionVO extensionVO = extensionDao.createForUpdate(extensionId);
        extensionVO.setState(Extension.State.Disabled);
        extensionDao.update(extensionId, extensionVO);
    }

    protected void updateAllExtensionHosts(Extension extension, Long clusterId, boolean remove) {
        List<Long> hostIds = new ArrayList<>();
        List<Long> clusterIds = clusterId == null ?
                extensionResourceMapDao.listResourceIdsByExtensionIdAndType(extension.getId(),
                        ExtensionResourceMap.ResourceType.Cluster) :
                Collections.singletonList(clusterId);
        for (Long cId : clusterIds) {
            hostIds.addAll(hostDao.listIdsByClusterId(cId));
        }
        if (CollectionUtils.isEmpty(hostIds)) {
            return;
        }
        ConcurrentHashMap<Long, Future<Void>> futures = new ConcurrentHashMap<>();
        ExecutorService executorService = Executors.newFixedThreadPool(3, new NamedThreadFactory("ExtensionHostUpdateWorker"));
        for (Long hostId : hostIds) {
            futures.put(hostId, executorService.submit(() -> {
                ExtensionRoutingUpdateCommand cmd = new ExtensionRoutingUpdateCommand(extension, remove);
                agentMgr.send(hostId, cmd);
                return null;
            }));
        }
        for (Map.Entry<Long, Future<Void>> entry: futures.entrySet()) {
            try {
                entry.getValue().get();
            } catch (InterruptedException | ExecutionException e) {
                logger.error(String.format("Error during updating %s for host: %d due to : %s",
                        extension, entry.getKey(), e.getMessage()), e);
            }
        }
        executorService.shutdown();
    }

    protected Map<String, String> getCallerDetails() {
        Account caller = CallContext.current().getCallingAccount();
        if (caller == null) {
            return null;
        }
        Map<String, String> callerDetails = new HashMap<>();
        callerDetails.put(ApiConstants.ID, caller.getUuid());
        callerDetails.put(ApiConstants.NAME, caller.getAccountName());
        if (caller.getType() != null) {
            callerDetails.put(ApiConstants.TYPE, caller.getType().name());
        }
        Role role = roleService.findRole(caller.getRoleId());
        if (role == null) {
            return callerDetails;
        }
        callerDetails.put(ApiConstants.ROLE_ID, role.getUuid());
        callerDetails.put(ApiConstants.ROLE_NAME, role.getName());
        if (role.getRoleType() != null) {
            callerDetails.put(ApiConstants.ROLE_TYPE, role.getRoleType().name());
        }
        return callerDetails;
    }

    protected Map<String, Map<String, String>> getExternalAccessDetails(Map<String, String> actionDetails, long hostId,
                           ExtensionResourceMap resourceMap) {
        Map<String, Map<String, String>> externalDetails = new HashMap<>();
        if (MapUtils.isNotEmpty(actionDetails)) {
            externalDetails.put(ApiConstants.ACTION, actionDetails);
        }
        Map<String, String> hostDetails = getFilteredExternalDetails(hostDetailsDao.findDetails(hostId));
        if (MapUtils.isNotEmpty(hostDetails)) {
            externalDetails.put(ApiConstants.HOST, hostDetails);
        }
        if (resourceMap == null) {
            return externalDetails;
        }
        Map<String, String> resourceDetails = extensionResourceMapDetailsDao.listDetailsKeyPairs(resourceMap.getId(), true);
        if (MapUtils.isNotEmpty(resourceDetails)) {
            externalDetails.put(ApiConstants.RESOURCE_MAP, resourceDetails);
        }
        Map<String, String> extensionDetails = extensionDetailsDao.listDetailsKeyPairs(resourceMap.getExtensionId(), true);
        if (MapUtils.isNotEmpty(extensionDetails)) {
            externalDetails.put(ApiConstants.EXTENSION, extensionDetails);
        }
        Map<String, String> callerDetails = getCallerDetails();
        if (MapUtils.isNotEmpty(callerDetails)) {
            externalDetails.put(ApiConstants.CALLER, callerDetails);
        }
        return externalDetails;
    }

    protected void checkOrchestratorTemplates(Long extensionId) {
        List<Long> extensionTemplateIds = templateDao.listIdsByExtensionId(extensionId);
        if (CollectionUtils.isNotEmpty(extensionTemplateIds)) {
            throw new CloudRuntimeException("Orchestrator extension has associated templates, remove them to delete the extension");
        }
    }

    protected void checkExtensionPathState(Extension extension, List<ManagementServerHostVO> msHosts) {
        String checksum = externalProvisioner.getChecksumForExtensionPath(extension.getName(),
                extension.getRelativePath());
        if (StringUtils.isBlank(checksum)) {
            updateExtensionPathReady(extension, false);
            return;
        }
        if (CollectionUtils.isEmpty(msHosts)) {
            updateExtensionPathReady(extension, true);
            return;
        }
        for (ManagementServerHostVO msHost : msHosts) {
            final Pair<Boolean, String> msPeerChecksumResult = getChecksumForExtensionPathOnMSPeer(extension,
                    msHost);
            if (!msPeerChecksumResult.first() || !checksum.equals(msPeerChecksumResult.second())) {
                logger.error("Path checksum for {} is different [msid: {}, checksum: {}] and [msid: {}, checksum: {}]",
                        extension, ManagementServerNode.getManagementServerId(), checksum, msHost.getMsid(),
                        (msPeerChecksumResult.first() ? msPeerChecksumResult.second() : "unknown"));
                updateExtensionPathReady(extension, false);
                return;
            }
        }
        updateExtensionPathReady(extension, true);
    }

    protected void addInbuiltExtensionReservedResourceDetails(long extensionId, List<String> reservedResourceDetails) {
        ExtensionVO vo = extensionDao.findById(extensionId);
        if (vo == null || vo.isUserDefined()) {
            return;
        }
        String lowerName = StringUtils.defaultString(vo.getName()).toLowerCase();
        Optional<Map.Entry<String, List<String>>> match = INBUILT_RESERVED_RESOURCE_DETAILS.entrySet().stream()
                .filter(e -> lowerName.contains(e.getKey().toLowerCase()))
                .findFirst();
        if (match.isPresent()) {
            Set<String> existing = new HashSet<>(reservedResourceDetails);
            for (String detailKey : match.get().getValue()) {
                if (existing.add(detailKey)) {
                    reservedResourceDetails.add(detailKey);
                }
            }
        }
    }

    @Override
    public String getExtensionsPath() {
        return externalProvisioner.getExtensionsPath();
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_EXTENSION_CREATE, eventDescription = "creating extension")
    public Extension createExtension(CreateExtensionCmd cmd) {
        final String name = cmd.getName();
        final String description = cmd.getDescription();
        final String typeStr = cmd.getType();
        String relativePath = cmd.getPath();
        final Boolean orchestratorRequiresPrepareVm = cmd.isOrchestratorRequiresPrepareVm();
        final String stateStr = cmd.getState();
        final String reservedResourceDetails = cmd.getReservedResourceDetails();
        ExtensionVO extensionByName = extensionDao.findByName(name);
        if (extensionByName != null) {
            throw new CloudRuntimeException("Extension by name already exists");
        }
        final Extension.Type type = EnumUtils.getEnum(Extension.Type.class, typeStr);
        if (type == null) {
            throw new CloudRuntimeException(String.format("Invalid type specified - %s", typeStr));
        }
        if (StringUtils.isBlank(relativePath)) {
            relativePath = getDefaultExtensionRelativePath(name);
        } else {
            relativePath = getValidatedExtensionRelativePath(name, relativePath);
        }
        Extension.State state = Extension.State.Enabled;
        if (StringUtils.isNotEmpty(stateStr)) {
            try {
                state = Extension.State.valueOf(stateStr);
            } catch (IllegalArgumentException iae) {
                throw new InvalidParameterValueException("Invalid state specified");
            }
        }
        if (orchestratorRequiresPrepareVm != null && !Extension.Type.Orchestrator.equals(type)) {
            throw new InvalidParameterValueException(String.format("%s is applicable only with %s type",
                    ApiConstants.ORCHESTRATOR_REQUIRES_PREPARE_VM, type.name()));
        }
        final String relativePathFinal = relativePath;
        final Extension.State stateFinal = state;
        ExtensionVO extensionVO = Transaction.execute((TransactionCallbackWithException<ExtensionVO, CloudRuntimeException>) status -> {
            ExtensionVO extension = new ExtensionVO(name, description, type,
                    relativePathFinal, stateFinal);
            if (!Extension.State.Enabled.equals(stateFinal)) {
                extension.setPathReady(false);
            }
            extension = extensionDao.persist(extension);

            Map<String, String> details = cmd.getDetails();
            List<ExtensionDetailsVO> detailsVOList = new ArrayList<>();
            if (MapUtils.isNotEmpty(details)) {
                for (Map.Entry<String, String> entry : details.entrySet()) {
                    detailsVOList.add(new ExtensionDetailsVO(extension.getId(), entry.getKey(), entry.getValue()));
                }
            }
            if (orchestratorRequiresPrepareVm != null) {
                detailsVOList.add(new ExtensionDetailsVO(extension.getId(),
                        ApiConstants.ORCHESTRATOR_REQUIRES_PREPARE_VM, String.valueOf(orchestratorRequiresPrepareVm),
                        false));
            }
            if (StringUtils.isNotBlank(reservedResourceDetails)) {
                detailsVOList.add(new ExtensionDetailsVO(extension.getId(),
                        ApiConstants.RESERVED_RESOURCE_DETAILS, reservedResourceDetails, false));
            }
            if (CollectionUtils.isNotEmpty(detailsVOList)) {
                extensionDetailsDao.saveDetails(detailsVOList);
            }
            CallContext.current().setEventResourceId(extension.getId());
            return extension;
        });
        if (Extension.State.Enabled.equals(extensionVO.getState()) &&
                !prepareExtensionPathAcrossServers(extensionVO)) {
            disableExtension(extensionVO.getId());
            throw new CloudRuntimeException(String.format(
                    "Failed to enable extension: %s as its path is not ready",
                    extensionVO.getName()));
        }
        return extensionVO;
    }

    @Override
    public boolean prepareExtensionPathAcrossServers(Extension extension) {
        boolean prepared = true;
        List<ManagementServerHostVO> msHosts = managementServerHostDao.listBy(ManagementServerHost.State.Up);
        for (ManagementServerHostVO msHost : msHosts) {
            if (msHost.getMsid() == ManagementServerNode.getManagementServerId()) {
                prepared = prepared && prepareExtensionPathOnCurrentServer(extension.getName(), extension.isUserDefined(),
                        extension.getRelativePath()).first();
                continue;
            }
            prepared = prepared && prepareExtensionPathOnMSPeer(extension, msHost);
        }
        if (extension.isPathReady() != prepared) {
            ExtensionVO updateExtension = extensionDao.createForUpdate(extension.getId());
            updateExtension.setPathReady(prepared);
            extensionDao.update(extension.getId(), updateExtension);
        }
        return prepared;
    }

    @Override
    public List<ExtensionResponse> listExtensions(ListExtensionsCmd cmd) {
        Long id = cmd.getExtensionId();
        String name = cmd.getName();
        String keyword = cmd.getKeyword();
        String typeStr = cmd.getType();
        String resourceIdStr = cmd.getResourceId();
        String resourceTypeStr = cmd.getResourceType();

        // If resourceId + resourceType are specified, return only extensions registered to that resource
        if (StringUtils.isNotBlank(resourceIdStr) && StringUtils.isNotBlank(resourceTypeStr)) {
            if (!EnumUtils.isValidEnum(ExtensionResourceMap.ResourceType.class, resourceTypeStr)) {
                throw new InvalidParameterValueException("Invalid resourcetype: " + resourceTypeStr);
            }
            ExtensionResourceMap.ResourceType resType = ExtensionResourceMap.ResourceType.valueOf(resourceTypeStr);
            // Resolve resourceId to a DB id
            long resolvedResourceId;
            if (ExtensionResourceMap.ResourceType.PhysicalNetwork.equals(resType)) {
                PhysicalNetworkVO pn = physicalNetworkDao.findByUuid(resourceIdStr);
                if (pn == null) {
                    try { pn = physicalNetworkDao.findById(Long.parseLong(resourceIdStr)); } catch (NumberFormatException ignored) {}
                }
                if (pn == null) throw new InvalidParameterValueException("Invalid physical network ID: " + resourceIdStr);
                resolvedResourceId = pn.getId();
            } else {
                try { resolvedResourceId = Long.parseLong(resourceIdStr); } catch (NumberFormatException e) {
                    throw new InvalidParameterValueException("Invalid resource ID: " + resourceIdStr);
                }
            }
            List<ExtensionResourceMapVO> maps = extensionResourceMapDao.listByResourceIdAndType(resolvedResourceId, resType);
            List<ExtensionResponse> responses = new ArrayList<>();
            for (ExtensionResourceMapVO map : maps) {
                ExtensionVO ext = extensionDao.findById(map.getExtensionId());
                if (ext == null) continue;
                if (typeStr != null && !typeStr.equalsIgnoreCase(ext.getType().name())) continue;
                if (name != null && !name.equalsIgnoreCase(ext.getName())) continue;
                responses.add(createExtensionResponse(ext, cmd.getDetails()));
            }
            return responses;
        }

        final SearchBuilder<ExtensionVO> sb = extensionDao.createSearchBuilder();
        final Filter searchFilter = new Filter(ExtensionVO.class, "id", false, cmd.getStartIndex(), cmd.getPageSizeVal());

        sb.and("id", sb.entity().getId(), SearchCriteria.Op.EQ);
        sb.and("name", sb.entity().getName(), SearchCriteria.Op.EQ);
        sb.and("keyword", sb.entity().getName(), SearchCriteria.Op.LIKE);
        sb.and("type", sb.entity().getType(), SearchCriteria.Op.EQ);
        sb.done();
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
        if (typeStr != null) {
            Extension.Type type = EnumUtils.getEnum(Extension.Type.class, typeStr);
            if (type == null) {
                throw new InvalidParameterValueException("Invalid type: " + typeStr);
            }
            sc.setParameters("type", type);
        }

        final Pair<List<ExtensionVO>, Integer> result = extensionDao.searchAndCount(sc, searchFilter);
        List<ExtensionResponse> responses = new ArrayList<>();
        for (ExtensionVO extension : result.first()) {
            ExtensionResponse response = createExtensionResponse(extension, cmd.getDetails());
            responses.add(response);
        }

        return responses;
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_EXTENSION_UPDATE, eventDescription = "updating extension")
    public Extension updateExtension(UpdateExtensionCmd cmd) {
        final long id = cmd.getId();
        final String description = cmd.getDescription();
        final Boolean orchestratorRequiresPrepareVm = cmd.isOrchestratorRequiresPrepareVm();
        final String stateStr = cmd.getState();
        final Map<String, String> details = cmd.getDetails();
        final Boolean cleanupDetails = cmd.isCleanupDetails();
        final String reservedResourceDetails = cmd.getReservedResourceDetails();
        final ExtensionVO extensionVO = extensionDao.findById(id);
        if (extensionVO == null) {
            throw new InvalidParameterValueException("Failed to find the extension");
        }
        boolean updateNeeded = false;
        if (description != null && !description.equals(extensionVO.getDescription())) {
            extensionVO.setDescription(description);
            updateNeeded = true;
        }
        if (orchestratorRequiresPrepareVm != null && !Extension.Type.Orchestrator.equals(extensionVO.getType())) {
            throw new InvalidParameterValueException(String.format("%s is applicable only with %s type",
                    ApiConstants.ORCHESTRATOR_REQUIRES_PREPARE_VM, extensionVO.getType()));
        }
        if (StringUtils.isNotBlank(stateStr) && !stateStr.equalsIgnoreCase(extensionVO.getState().name())) {
            try {
                Extension.State state = Extension.State.valueOf(stateStr);
                extensionVO.setState(state);
                updateNeeded = true;
            } catch (IllegalArgumentException iae) {
                throw new InvalidParameterValueException("Invalid state specified");
            }
        }
        final boolean updateNeededFinal = updateNeeded;
        ExtensionVO result = Transaction.execute((TransactionCallbackWithException<ExtensionVO, CloudRuntimeException>) status -> {
            if (updateNeededFinal && !extensionDao.update(id, extensionVO)) {
                throw new CloudRuntimeException(String.format("Failed to updated the extension: %s",
                        extensionVO.getName()));
            }
            updateExtensionsDetails(cleanupDetails, details, orchestratorRequiresPrepareVm, reservedResourceDetails,
                    id);
            return extensionVO;
        });
        if (StringUtils.isNotBlank(stateStr)) {
            if (Extension.State.Enabled.equals(result.getState()) &&
                !prepareExtensionPathAcrossServers(result)) {
                disableExtension(result.getId());
                throw new CloudRuntimeException(String.format(
                        "Failed to enable extension: %s as it path is not ready",
                        extensionVO.getName()));
            }
            updateAllExtensionHosts(extensionVO, null, false);
        }
        return result;
    }

    protected void updateExtensionsDetails(Boolean cleanupDetails, Map<String, String> details,
               Boolean orchestratorRequiresPrepareVm, String reservedResourceDetails, long id) {
        final boolean needToUpdateAllDetails = Boolean.TRUE.equals(cleanupDetails) || MapUtils.isNotEmpty(details);
        if (!needToUpdateAllDetails && orchestratorRequiresPrepareVm == null &&
                StringUtils.isBlank(reservedResourceDetails)) {
            return;
        }
        if (needToUpdateAllDetails) {
            Map<String, String> hiddenDetails =
                    extensionDetailsDao.listDetailsKeyPairs(id, false);
            List<ExtensionDetailsVO> detailsVOList = new ArrayList<>();
            if (orchestratorRequiresPrepareVm != null) {
                hiddenDetails.put(ApiConstants.ORCHESTRATOR_REQUIRES_PREPARE_VM,
                        String.valueOf(orchestratorRequiresPrepareVm));
            }
            if (StringUtils.isNotBlank(reservedResourceDetails)) {
                hiddenDetails.put(ApiConstants.RESERVED_RESOURCE_DETAILS, reservedResourceDetails);
            }
            if (MapUtils.isNotEmpty(hiddenDetails)) {
                hiddenDetails.forEach((key, value) -> detailsVOList.add(
                        new ExtensionDetailsVO(id, key, value, false)));
            }
            if (!Boolean.TRUE.equals(cleanupDetails) && MapUtils.isNotEmpty(details)) {
                details.forEach((key, value) -> detailsVOList.add(
                        new ExtensionDetailsVO(id, key, value)));
            }
            if (CollectionUtils.isNotEmpty(detailsVOList)) {
                extensionDetailsDao.saveDetails(detailsVOList);
            } else if (Boolean.TRUE.equals(cleanupDetails)) {
                extensionDetailsDao.removeDetails(id);
            }
        } else {
            if (orchestratorRequiresPrepareVm != null) {
                ExtensionDetailsVO detailsVO = extensionDetailsDao.findDetail(id,
                        ApiConstants.ORCHESTRATOR_REQUIRES_PREPARE_VM);
                if (detailsVO == null) {
                    extensionDetailsDao.persist(new ExtensionDetailsVO(id,
                            ApiConstants.ORCHESTRATOR_REQUIRES_PREPARE_VM,
                            String.valueOf(orchestratorRequiresPrepareVm), false));
                } else if (Boolean.parseBoolean(detailsVO.getValue()) != orchestratorRequiresPrepareVm) {
                    detailsVO.setValue(String.valueOf(orchestratorRequiresPrepareVm));
                    extensionDetailsDao.update(detailsVO.getId(), detailsVO);
                }
            }
            if (StringUtils.isNotBlank(reservedResourceDetails)) {
                ExtensionDetailsVO detailsVO = extensionDetailsDao.findDetail(id,
                        ApiConstants.RESERVED_RESOURCE_DETAILS);
                if (detailsVO == null) {
                    extensionDetailsDao.persist(new ExtensionDetailsVO(id,
                            ApiConstants.RESERVED_RESOURCE_DETAILS,
                            reservedResourceDetails, false));
                } else if (!reservedResourceDetails.equals(detailsVO.getValue())) {
                    detailsVO.setValue(reservedResourceDetails);
                    extensionDetailsDao.update(detailsVO.getId(), detailsVO);
                }
            }
        }
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_EXTENSION_DELETE, eventDescription = "deleting extension")
    public boolean deleteExtension(DeleteExtensionCmd cmd) {
        Long extensionId = cmd.getId();
        final boolean cleanup = cmd.isCleanup();
        ExtensionVO extension = extensionDao.findById(extensionId);
        if (extension == null) {
            throw new InvalidParameterValueException("Unable to find the extension with the specified id");
        }
        if (!extension.isUserDefined()) {
            throw new InvalidParameterValueException("System extension can not be deleted");
        }
        List<ExtensionResourceMapVO> registeredResources = extensionResourceMapDao.listByExtensionId(extensionId);
        if (CollectionUtils.isNotEmpty(registeredResources)) {
            throw new CloudRuntimeException("Extension has associated resources, unregister them to delete the extension");
        }
        List<Long> customActionIds = extensionCustomActionDao.listIdsByExtensionId(extensionId);
        if (CollectionUtils.isNotEmpty(customActionIds)) {
            throw new CloudRuntimeException(String.format("Extension has %d custom actions, delete them to delete the extension",
                    customActionIds.size()));
        }
        checkOrchestratorTemplates(extensionId);

        boolean result = Transaction.execute((TransactionCallbackWithException<Boolean, CloudRuntimeException>) status -> {
            extensionDetailsDao.removeDetails(extensionId);
            extensionDao.remove(extensionId);
            return true;
        });
        if (result && cleanup) {
            cleanupExtensionFilesAcrossServers(extension);
        }
        return true;
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_EXTENSION_RESOURCE_REGISTER, eventDescription = "registering extension resource")
    public Extension registerExtensionWithResource(RegisterExtensionCmd cmd) {
        String resourceId = cmd.getResourceId();
        Long extensionId = cmd.getExtensionId();
        String resourceType = cmd.getResourceType();
        if (!EnumUtils.isValidEnum(ExtensionResourceMap.ResourceType.class, resourceType)) {
            throw new InvalidParameterValueException(
                    String.format("Currently only [%s] can be used to register an extension",
                            EnumSet.allOf(ExtensionResourceMap.ResourceType.class)));
        }
        ExtensionVO extension = extensionDao.findById(extensionId);
        if (extension == null) {
            throw new InvalidParameterValueException("Invalid extension specified");
        }
        ExtensionResourceMap.ResourceType resType = ExtensionResourceMap.ResourceType.valueOf(resourceType);
        if (ExtensionResourceMap.ResourceType.PhysicalNetwork.equals(resType)) {
            PhysicalNetworkVO physicalNetwork = physicalNetworkDao.findByUuid(resourceId);
            if (physicalNetwork == null) {
                physicalNetwork = physicalNetworkDao.findById(Long.parseLong(resourceId));
            }
            if (physicalNetwork == null) {
                throw new InvalidParameterValueException("Invalid physical network ID specified");
            }
            ExtensionResourceMap extensionResourceMap = registerExtensionWithPhysicalNetwork(physicalNetwork, extension, cmd.getDetails());
            return extensionDao.findById(extensionResourceMap.getExtensionId());
        }
        ClusterVO clusterVO = clusterDao.findByUuid(resourceId);
        if (clusterVO == null) {
            throw new InvalidParameterValueException("Invalid cluster ID specified");
        }
        ExtensionResourceMap extensionResourceMap = registerExtensionWithCluster(clusterVO, extension, cmd.getDetails());
        return extensionDao.findById(extensionResourceMap.getExtensionId());
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_EXTENSION_RESOURCE_UPDATE, eventDescription = "updating extension resource")
    public Extension updateRegisteredExtensionWithResource(UpdateRegisteredExtensionCmd cmd) {
        final String resourceId = cmd.getResourceId();
        final Long extensionId = cmd.getExtensionId();
        final String resourceType = cmd.getResourceType();
        final Map<String, String> details = cmd.getDetails();
        final Boolean cleanupDetails = cmd.isCleanupDetails();

        if (!EnumUtils.isValidEnum(ExtensionResourceMap.ResourceType.class, resourceType)) {
            throw new InvalidParameterValueException(
                    String.format("Currently only [%s] can be used to update an extension registration",
                            EnumSet.allOf(ExtensionResourceMap.ResourceType.class)));
        }
        ExtensionVO extension = extensionDao.findById(extensionId);
        if (extension == null) {
            throw new InvalidParameterValueException("Invalid extension specified");
        }

        ExtensionResourceMap.ResourceType resType = ExtensionResourceMap.ResourceType.valueOf(resourceType);
        long resolvedResourceId;
        if (ExtensionResourceMap.ResourceType.PhysicalNetwork.equals(resType)) {
            PhysicalNetworkVO physicalNetwork = physicalNetworkDao.findByUuid(resourceId);
            if (physicalNetwork == null) {
                try {
                    physicalNetwork = physicalNetworkDao.findById(Long.parseLong(resourceId));
                } catch (NumberFormatException ignored) {
                }
            }
            if (physicalNetwork == null) {
                throw new InvalidParameterValueException("Invalid physical network ID specified");
            }
            resolvedResourceId = physicalNetwork.getId();
        } else {
            ClusterVO clusterVO = clusterDao.findByUuid(resourceId);
            if (clusterVO == null) {
                throw new InvalidParameterValueException("Invalid cluster ID specified");
            }
            resolvedResourceId = clusterVO.getId();
        }

        List<ExtensionResourceMapVO> mappings = extensionResourceMapDao.listByResourceIdAndType(resolvedResourceId, resType);
        ExtensionResourceMapVO targetMapping = null;
        if (CollectionUtils.isNotEmpty(mappings)) {
            for (ExtensionResourceMapVO mapping : mappings) {
                if (mapping.getExtensionId() == extensionId) {
                    targetMapping = mapping;
                    break;
                }
            }
        }
        if (targetMapping == null) {
            throw new InvalidParameterValueException(String.format(
                    "Extension '%s' is not registered with resource %s (%s)",
                    extension.getName(), resourceId, resourceType));
        }

        if (Boolean.TRUE.equals(cleanupDetails)) {
            extensionResourceMapDetailsDao.removeDetails(targetMapping.getId());
        } else {
            List<ExtensionResourceMapDetailsVO> detailsList = buildExtensionResourceDetailsArray(targetMapping.getId(), details);
            if (CollectionUtils.isNotEmpty(detailsList)) {
                appendHiddenExtensionResourceDetails(targetMapping.getId(), detailsList);
            }
            detailsList = detailsList.stream()
                    .filter(detail -> detail.getValue() != null)
                    .collect(Collectors.toList());
            if (CollectionUtils.isNotEmpty(detailsList)) {
                extensionResourceMapDetailsDao.saveDetails(detailsList);
            } else {
                extensionResourceMapDetailsDao.removeDetails(targetMapping.getId());
            }
        }

        return extensionDao.findById(extensionId);
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_EXTENSION_RESOURCE_REGISTER, eventDescription = "registering extension resource")
    public ExtensionResourceMap registerExtensionWithCluster(Cluster cluster, Extension extension,
                  Map<String, String> details) {
        if (!Hypervisor.HypervisorType.External.equals(cluster.getHypervisorType())) {
            throw new CloudRuntimeException(
                    String.format("Cluster ID: %s is not of %s hypervisor type", cluster.getId(),
                            cluster.getHypervisorType()));
        }
        final ExtensionResourceMap.ResourceType resourceType = ExtensionResourceMap.ResourceType.Cluster;
        ExtensionResourceMapVO existing =
                extensionResourceMapDao.findByResourceIdAndType(cluster.getId(), resourceType);
        if (existing != null) {
            if (existing.getExtensionId() == extension.getId()) {
                throw new CloudRuntimeException(String.format(
                        "Extension: %s is already registered with this cluster: %s",
                        extension.getName(), cluster.getName()));
            } else {
                throw new CloudRuntimeException(String.format(
                        "An extension is already registered with this cluster: %s", cluster.getName()));
            }
        }
        ExtensionResourceMap result = Transaction.execute((TransactionCallbackWithException<ExtensionResourceMap, CloudRuntimeException>) status -> {
            ExtensionResourceMapVO extensionMap = new ExtensionResourceMapVO(extension.getId(), cluster.getId(), resourceType);
            ExtensionResourceMapVO savedExtensionMap = extensionResourceMapDao.persist(extensionMap);
            List<ExtensionResourceMapDetailsVO> detailsVOList = new ArrayList<>();
            if (MapUtils.isNotEmpty(details)) {
                for (Map.Entry<String, String> entry : details.entrySet()) {
                    boolean display = !SENSITIVE_DETAIL_KEYS.contains(entry.getKey().toLowerCase());
                    detailsVOList.add(new ExtensionResourceMapDetailsVO(savedExtensionMap.getId(),
                            entry.getKey(), entry.getValue(), display));
                }
                extensionResourceMapDetailsDao.saveDetails(detailsVOList);
            }
            return extensionMap;
        });
        updateAllExtensionHosts(extension, cluster.getId(), false);
        return result;
    }

    protected ExtensionResourceMap registerExtensionWithPhysicalNetwork(PhysicalNetworkVO physicalNetwork,
                  Extension extension, Map<String, String> details) {
        // Only NetworkOrchestrator extensions can be registered with physical networks
        if (!Extension.Type.NetworkOrchestrator.equals(extension.getType())) {
            throw new InvalidParameterValueException(String.format(
                    "Only extensions of type %s can be registered with a physical network. "
                    + "Extension '%s' is of type %s.",
                    Extension.Type.NetworkOrchestrator.name(),
                    extension.getName(), extension.getType().name()));
        }

        // Block registering the exact same extension twice on the same physical network
        final ExtensionResourceMap.ResourceType resourceType = ExtensionResourceMap.ResourceType.PhysicalNetwork;
        List<ExtensionResourceMapVO> existing = extensionResourceMapDao.listByResourceIdAndType(
                physicalNetwork.getId(), resourceType);
        if (existing != null) {
            for (ExtensionResourceMapVO ex : existing) {
                if (ex.getExtensionId() == extension.getId()) {
                    throw new CloudRuntimeException(String.format(
                            "Extension '%s' is already registered with physical network %s",
                            extension.getName(), physicalNetwork.getId()));
                }
            }
        }

        // Resolve which services this extension provides from its network.services detail
        Set<String> services = resolveExtensionServices(extension);

        return Transaction.execute((TransactionCallbackWithException<ExtensionResourceMap, CloudRuntimeException>) status -> {
            // 1. Persist the extension<->physical-network mapping
            ExtensionResourceMapVO extensionMap = new ExtensionResourceMapVO(extension.getId(),
                    physicalNetwork.getId(), resourceType);
            ExtensionResourceMapVO savedExtensionMap = extensionResourceMapDao.persist(extensionMap);

            // 2. Persist device credentials / details
            List<ExtensionResourceMapDetailsVO> detailsVOList = new ArrayList<>();
            if (MapUtils.isNotEmpty(details)) {
                for (Map.Entry<String, String> entry : details.entrySet()) {
                    boolean display = !SENSITIVE_DETAIL_KEYS.contains(entry.getKey().toLowerCase());
                    detailsVOList.add(new ExtensionResourceMapDetailsVO(savedExtensionMap.getId(),
                            entry.getKey(), entry.getValue(), display));
                }
                extensionResourceMapDetailsDao.saveDetails(detailsVOList);
            }

            // 3. Auto-create the NetworkServiceProvider entry for this extension so that
            //    the services are visible in the UI and in listSupportedNetworkServices.
            //    The NSP name equals the extension name; state is Enabled by default.
            PhysicalNetworkServiceProviderVO existingNsp =
                    physicalNetworkServiceProviderDao.findByServiceProvider(
                            physicalNetwork.getId(), extension.getName());
            if (existingNsp == null) {
                PhysicalNetworkServiceProviderVO nsp =
                        new PhysicalNetworkServiceProviderVO(physicalNetwork.getId(), extension.getName());
                applyServicesToNsp(nsp, services);
                nsp.setState(PhysicalNetworkServiceProvider.State.Enabled);
                physicalNetworkServiceProviderDao.persist(nsp);
                logger.info("Auto-created NetworkServiceProvider '{}' (Enabled) for physical network {} "
                        + "with services {}", extension.getName(), physicalNetwork.getId(), services);
            }

            return extensionMap;
        });
    }

    /**
     * Resolves the set of network service names declared in the extension's
     * {@code network.services} detail. Falls back to an empty set if not present
     */
    private Set<String> resolveExtensionServices(Extension extension) {
        Map<String, String> extDetails = extensionDetailsDao.listDetailsKeyPairs(extension.getId());
        Set<String> parsed = parseNetworkServicesFromDetailKeys(extDetails);
        if (!parsed.isEmpty()) {
            return parsed;
        }
        // Default: the full set of services NetworkExtensionElement supports
        return new HashSet<>();
    }

    /**
     * Resolves the set of service names from the extension detail map.
     * From {@code network.services} comma-separated key.
     */
    private Set<String> parseNetworkServicesFromDetailKeys(Map<String, String> extDetails) {
        if (extDetails == null) {
            return Collections.emptySet();
        }
        // New format: "network.services" = "SourceNat,StaticNat,..."
        if (extDetails.containsKey(ExtensionHelper.NETWORK_SERVICES_DETAIL_KEY)) {
            String value = extDetails.get(ExtensionHelper.NETWORK_SERVICES_DETAIL_KEY);
            if (StringUtils.isNotBlank(value)) {
                Set<String> services = new HashSet<>();
                for (String s : value.split(",")) {
                    String trimmed = s.trim();
                    if (!trimmed.isEmpty()) {
                        services.add(trimmed);
                    }
                }
                if (!services.isEmpty()) {
                    return services;
                }
            }
        }

        return Collections.emptySet();
    }

    /**
     * Builds a full {@code Map<Service, Map<Capability, String>>} from the
     * extension detail map.  From the split keys
     * {@code network.services} + {@code network.service.capabilities}.
     */
    private Map<Service, Map<Capability, String>> buildCapabilitiesFromDetailKeys(
            Map<String, String> extDetails) {
        if (extDetails == null) {
            return new HashMap<>();
        }
        // New split format
        if (extDetails.containsKey(ExtensionHelper.NETWORK_SERVICES_DETAIL_KEY)) {
            Set<String> serviceNames = parseNetworkServicesFromDetailKeys(extDetails);
            if (!serviceNames.isEmpty()) {
                JsonObject capsObj = null;
                if (extDetails.containsKey(ExtensionHelper.NETWORK_SERVICE_CAPABILITIES_DETAIL_KEY)) {
                    try {
                        capsObj = JsonParser.parseString(
                                extDetails.get(ExtensionHelper.NETWORK_SERVICE_CAPABILITIES_DETAIL_KEY))
                                .getAsJsonObject();
                    } catch (Exception e) {
                        logger.warn("Failed to parse network.service.capabilities JSON: {}", e.getMessage());
                    }
                }
                Map<Service, Map<Capability, String>> result = new HashMap<>();
                for (String svcName : serviceNames) {
                    Service service = Service.getService(svcName);
                    if (service == null) {
                        logger.warn("Unknown network service '{}' in network.services — skipping", svcName);
                        continue;
                    }
                    Map<Capability, String> capMap = new HashMap<>();
                    if (capsObj != null && capsObj.has(svcName)) {
                        JsonObject svcCaps = capsObj.getAsJsonObject(svcName);
                        for (Map.Entry<String, JsonElement> entry : svcCaps.entrySet()) {
                            Capability cap = Capability.getCapability(entry.getKey());
                            if (cap != null) {
                                capMap.put(cap, entry.getValue().getAsString());
                            }
                        }
                    }
                    result.put(service, capMap);
                }
                return result;
            }
        }

        return new HashMap<>();
    }

    /**
     * Sets the boolean service-provided flags on a {@link PhysicalNetworkServiceProviderVO}
     * based on a set of service names.
     */
    private void applyServicesToNsp(PhysicalNetworkServiceProviderVO nsp, Set<String> services) {
        nsp.setSourcenatServiceProvided(services.contains("SourceNat"));
        nsp.setStaticnatServiceProvided(services.contains("StaticNat"));
        nsp.setPortForwardingServiceProvided(services.contains("PortForwarding"));
        nsp.setFirewallServiceProvided(services.contains("Firewall"));
        nsp.setGatewayServiceProvided(services.contains("Gateway"));
        nsp.setDnsServiceProvided(services.contains("Dns"));
        nsp.setDhcpServiceProvided(services.contains("Dhcp"));
        nsp.setUserdataServiceProvided(services.contains("UserData"));
        nsp.setLbServiceProvided(services.contains("Lb"));
        nsp.setVpnServiceProvided(services.contains("Vpn"));
        nsp.setSecuritygroupServiceProvided(services.contains("SecurityGroup"));
        nsp.setNetworkAclServiceProvided(services.contains("NetworkACL"));
    }

    /** Keys that are always stored with display=false (sensitive). */
    private static final Set<String> SENSITIVE_DETAIL_KEYS =
            Set.of("password", "sshkey");

    /**
     * Validates that the comma-separated or JSON-array {@code servicesValue} is a
     * subset of the services declared in the extension's {@code network.services}
     * Throws {@link InvalidParameterValueException} if any service in the request is not
     * offered by the extension.
     */
    protected void validateNetworkServicesSubset(Extension extension, String servicesValue) {
        if (StringUtils.isBlank(servicesValue)) {
            return;
        }
        Map<String, String> extDetails = extensionDetailsDao.listDetailsKeyPairs(extension.getId());
        Set<String> allowedServices = parseNetworkServicesFromDetailKeys(extDetails);
        if (allowedServices.isEmpty()) {
            // No services declared → accept any
            return;
        }

        // Parse the requested services: either comma-separated string or JSON array
        List<String> requested = parseServicesList(servicesValue);
        List<String> invalid = requested.stream()
                .filter(s -> !allowedServices.contains(s))
                .collect(Collectors.toList());
        if (!invalid.isEmpty()) {
            throw new InvalidParameterValueException(String.format(
                    "The following services are not supported by extension '%s': %s. "
                    + "Supported services are: %s",
                    extension.getName(), invalid, allowedServices));
        }
    }

    /**
     * Parses a services list from either a comma-separated string (e.g.
     * {@code "SourceNat,StaticNat"}) or a JSON array (e.g.
     * {@code ["SourceNat","StaticNat"]}).
     */
    private List<String> parseServicesList(String value) {
        if (StringUtils.isBlank(value)) {
            return Collections.emptyList();
        }
        value = value.trim();
        if (value.startsWith("[")) {
            try {
                JsonArray arr = JsonParser.parseString(value).getAsJsonArray();
                List<String> result = new ArrayList<>();
                for (JsonElement el : arr) {
                    result.add(el.getAsString().trim());
                }
                return result;
            } catch (Exception e) {
                // fall through to comma-split
            }
        }
        // Comma-separated
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_EXTENSION_RESOURCE_UNREGISTER, eventDescription = "unregistering extension resource")
    public Extension unregisterExtensionWithResource(UnregisterExtensionCmd cmd) {
        final String resourceId = cmd.getResourceId();
        final Long extensionId = cmd.getExtensionId();
        final String resourceType = cmd.getResourceType();
        if (!EnumUtils.isValidEnum(ExtensionResourceMap.ResourceType.class, resourceType)) {
            throw new InvalidParameterValueException(
                    String.format("Currently only [%s] can be used to unregister an extension",
                            EnumSet.allOf(ExtensionResourceMap.ResourceType.class)));
        }
        ExtensionResourceMap.ResourceType resType = ExtensionResourceMap.ResourceType.valueOf(resourceType);
        if (ExtensionResourceMap.ResourceType.PhysicalNetwork.equals(resType)) {
            unregisterExtensionWithPhysicalNetwork(resourceId, extensionId);
        } else {
            unregisterExtensionWithCluster(resourceId, extensionId);
        }
        return extensionDao.findById(extensionId);
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_EXTENSION_RESOURCE_UNREGISTER, eventDescription = "unregistering extension resource")
    public void unregisterExtensionWithCluster(Cluster cluster, Long extensionId) {
        ExtensionResourceMapVO existing = extensionResourceMapDao.findByResourceIdAndType(cluster.getId(),
                ExtensionResourceMap.ResourceType.Cluster);
        if (existing == null) {
            return;
        }
        extensionResourceMapDao.remove(existing.getId());
        extensionResourceMapDetailsDao.removeDetails(existing.getId());
        ExtensionVO extensionVO = extensionDao.findById(extensionId);
        if (extensionVO != null) {
            updateAllExtensionHosts(extensionVO, cluster.getId(), true);
        }
    }

    protected void unregisterExtensionWithPhysicalNetwork(String resourceId, Long extensionId) {
        PhysicalNetworkVO physicalNetwork = physicalNetworkDao.findByUuid(resourceId);
        if (physicalNetwork == null) {
            try {
                physicalNetwork = physicalNetworkDao.findById(Long.parseLong(resourceId));
            } catch (NumberFormatException ignored) {
            }
        }
        if (physicalNetwork == null) {
            throw new InvalidParameterValueException("Invalid physical network ID specified");
        }
        // Find the specific map entry for this extension+physical-network combination
        List<ExtensionResourceMapVO> existingList = extensionResourceMapDao.listByResourceIdAndType(
                physicalNetwork.getId(), ExtensionResourceMap.ResourceType.PhysicalNetwork);
        if (existingList == null || existingList.isEmpty()) {
            return;
        }
        final long physNetId = physicalNetwork.getId();
        for (ExtensionResourceMapVO existing : existingList) {
            if (extensionId == null || existing.getExtensionId() == extensionId) {
                ExtensionVO ext = extensionDao.findById(existing.getExtensionId());
                if (ext != null) {
                    List<NetworkVO> networksUsingProvider = networkDao.listByPhysicalNetworkAndProvider(
                            physNetId, ext.getName());
                    if (CollectionUtils.isNotEmpty(networksUsingProvider)) {
                        throw new CloudRuntimeException(String.format(
                                "Cannot unregister extension '%s' from physical network %s. "
                                        + "Provider is used by %d existing network(s)",
                                ext.getName(), physNetId, networksUsingProvider.size()));
                    }
                }

                extensionResourceMapDao.remove(existing.getId());
                extensionResourceMapDetailsDao.removeDetails(existing.getId());

                // Also remove the auto-created NSP for this extension
                if (ext != null) {
                    PhysicalNetworkServiceProviderVO nsp =
                            physicalNetworkServiceProviderDao.findByServiceProvider(physNetId, ext.getName());
                    if (nsp != null) {
                        physicalNetworkServiceProviderDao.remove(nsp.getId());
                        logger.info("Removed NetworkServiceProvider '{}' from physical network {} "
                                + "on extension unregister", ext.getName(), physNetId);
                    }
                }
            }
        }
    }

    @Override
    public ExtensionResponse createExtensionResponse(Extension extension,
                 EnumSet<ApiConstants.ExtensionDetails> viewDetails) {
        ExtensionResponse response = new ExtensionResponse(extension.getUuid(), extension.getName(),
                extension.getDescription(), extension.getType().name());
        response.setCreated(extension.getCreated());
        response.setPath(externalProvisioner.getExtensionPath(extension.getRelativePath()));
        response.setPathReady(extension.isPathReady());
        response.setUserDefined(extension.isUserDefined());
        response.setState(extension.getState().name());
        if (viewDetails.contains(ApiConstants.ExtensionDetails.all) ||
                viewDetails.contains(ApiConstants.ExtensionDetails.resource)) {
            List<ExtensionResourceResponse> resourcesResponse = new ArrayList<>();
            List<ExtensionResourceMapVO> extensionResourceMapVOs =
                    extensionResourceMapDao.listByExtensionId(extension.getId());
            for (ExtensionResourceMapVO extensionResourceMapVO : extensionResourceMapVOs) {
                ExtensionResourceResponse extensionResourceResponse = new ExtensionResourceResponse();
                extensionResourceResponse.setType(extensionResourceMapVO.getResourceType().name());
                extensionResourceResponse.setCreated(extensionResourceMapVO.getCreated());
                if (ExtensionResourceMap.ResourceType.Cluster.equals(extensionResourceMapVO.getResourceType())) {
                    Cluster cluster = clusterDao.findById(extensionResourceMapVO.getResourceId());
                    extensionResourceResponse.setId(cluster.getUuid());
                    extensionResourceResponse.setName(cluster.getName());
                } else if (ExtensionResourceMap.ResourceType.PhysicalNetwork.equals(extensionResourceMapVO.getResourceType())) {
                    PhysicalNetworkVO pn = physicalNetworkDao.findById(extensionResourceMapVO.getResourceId());
                    if (pn != null) {
                        extensionResourceResponse.setId(pn.getUuid());
                        extensionResourceResponse.setName(pn.getName());
                    }
                }
                Map<String, String> details = extensionResourceMapDetailsDao.listDetailsKeyPairs(
                        extensionResourceMapVO.getId(), true);
                if (MapUtils.isNotEmpty(details)) {
                    extensionResourceResponse.setDetails(details);
                }
                resourcesResponse.add(extensionResourceResponse);
            }
            if (CollectionUtils.isNotEmpty(resourcesResponse)) {
                response.setResources(resourcesResponse);
            }
        }
        Map<String, String> hiddenDetails;
        if (viewDetails.contains(ApiConstants.ExtensionDetails.all) ||
                viewDetails.contains(ApiConstants.ExtensionDetails.external)) {
            Pair<Map<String, String>, Map<String, String>> extensionDetails =
                    extensionDetailsDao.listDetailsKeyPairsWithVisibility(extension.getId());
            if (MapUtils.isNotEmpty(extensionDetails.first())) {
                response.setDetails(extensionDetails.first());
            }
            hiddenDetails = extensionDetails.second();
        } else {
            hiddenDetails = extensionDetailsDao.listDetailsKeyPairs(extension.getId(),
                    List.of(ApiConstants.ORCHESTRATOR_REQUIRES_PREPARE_VM,
                            ApiConstants.RESERVED_RESOURCE_DETAILS));
        }
        if (hiddenDetails.containsKey(ApiConstants.ORCHESTRATOR_REQUIRES_PREPARE_VM)) {
            response.setOrchestratorRequiresPrepareVm(Boolean.parseBoolean(
                    hiddenDetails.get(ApiConstants.ORCHESTRATOR_REQUIRES_PREPARE_VM)));
        }
        if (hiddenDetails.containsKey(ApiConstants.RESERVED_RESOURCE_DETAILS)) {
            response.setReservedResourceDetails(hiddenDetails.get(ApiConstants.RESERVED_RESOURCE_DETAILS));
        }
        response.setObjectName(Extension.class.getSimpleName().toLowerCase());
        return response;
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_EXTENSION_CUSTOM_ACTION_ADD, eventDescription = "adding extension custom action")
    public ExtensionCustomAction addCustomAction(AddCustomActionCmd cmd) {
        String name = cmd.getName();
        String description = cmd.getDescription();
        Long extensionId = cmd.getExtensionId();
        String resourceTypeStr = cmd.getResourceType();
        List<String> rolesStrList = cmd.getAllowedRoleTypes();
        final int timeout = ObjectUtils.defaultIfNull(cmd.getTimeout(), 3);
        final boolean enabled = cmd.isEnabled();
        Map parametersMap = cmd.getParametersMap();
        final String successMessage = cmd.getSuccessMessage();
        final String errorMessage = cmd.getErrorMessage();
        Map<String, String> details = cmd.getDetails();
        if (name == null || !name.matches("^[a-zA-Z0-9 _-]+$")) {
            throw new InvalidParameterValueException(String.format("Invalid action name: %s. It can contain " +
                    "only alphabets, numbers, hyphen, underscore and space", name));
        }
        ExtensionCustomActionVO existingCustomAction = extensionCustomActionDao.findByNameAndExtensionId(extensionId, name);
        if (existingCustomAction != null) {
            throw new CloudRuntimeException("Action by name already exists");
        }
        ExtensionVO extensionVO = extensionDao.findById(extensionId);
        if (extensionVO == null) {
            throw new InvalidParameterValueException("Specified extension can not be found");
        }
        List<ExtensionCustomAction.Parameter> parameters = getParametersListFromMap(name, parametersMap);
        ExtensionCustomAction.ResourceType resourceType = null;
        if (StringUtils.isNotBlank(resourceTypeStr)) {
            resourceType = EnumUtils.getEnumIgnoreCase(ExtensionCustomAction.ResourceType.class, resourceTypeStr);
            if (resourceType == null) {
                throw new InvalidParameterValueException(
                        String.format("Invalid resource type specified: %s. Valid values are: %s", resourceTypeStr,
                        EnumSet.allOf(ExtensionCustomAction.ResourceType.class)));
            }
        }
        if (resourceType == null && Extension.Type.Orchestrator.equals(extensionVO.getType())) {
            resourceType = ExtensionCustomAction.ResourceType.VirtualMachine;
        }
        final Set<RoleType> roleTypes = new HashSet<>();
        if (CollectionUtils.isNotEmpty(rolesStrList)) {
            for (String roleTypeStr : rolesStrList) {
                try {
                    RoleType roleType = RoleType.fromString(roleTypeStr);
                    roleTypes.add(roleType);
                } catch (IllegalStateException ignored) {
                    throw new InvalidParameterValueException(String.format("Invalid role specified - %s", roleTypeStr));
                }
            }
        }
        roleTypes.add(RoleType.Admin);
        final ExtensionCustomAction.ResourceType resourceTypeFinal = resourceType;
        return Transaction.execute((TransactionCallbackWithException<ExtensionCustomActionVO, CloudRuntimeException>) status -> {
            ExtensionCustomActionVO customAction =
                    new ExtensionCustomActionVO(name, description, extensionId, successMessage, errorMessage, timeout, enabled);
            if (resourceTypeFinal != null) {
                customAction.setResourceType(resourceTypeFinal);
            }
            customAction.setAllowedRoleTypes(RoleType.toCombinedMask(roleTypes));
            ExtensionCustomActionVO savedAction = extensionCustomActionDao.persist(customAction);
            List<ExtensionCustomActionDetailsVO> detailsVOList = new ArrayList<>();
            detailsVOList.add(new ExtensionCustomActionDetailsVO(
                    savedAction.getId(),
                    ApiConstants.PARAMETERS,
                    ExtensionCustomAction.Parameter.toJsonFromList(parameters),
                    false
            ));
            if (MapUtils.isNotEmpty(details)) {
                details.forEach((key, value) -> detailsVOList.add(
                        new ExtensionCustomActionDetailsVO(savedAction.getId(), key, value)));
            }
            if (CollectionUtils.isNotEmpty(detailsVOList)) {
                extensionCustomActionDetailsDao.saveDetails(detailsVOList);
            }
            CallContext.current().setEventResourceId(savedAction.getId());
            return savedAction;
        });
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_EXTENSION_CUSTOM_ACTION_DELETE, eventDescription = "deleting extension custom action")
    public boolean deleteCustomAction(DeleteCustomActionCmd cmd) {
        Long customActionId = cmd.getId();
        ExtensionCustomActionVO customActionVO = extensionCustomActionDao.findById(customActionId);
        if (customActionVO == null) {
            throw new InvalidParameterValueException("Unable to find the custom action with the specified id");
        }
        return Transaction.execute((TransactionCallbackWithException<Boolean, CloudRuntimeException>) status -> {
            extensionCustomActionDetailsDao.removeDetails(customActionId);
            if (!extensionCustomActionDao.remove(customActionId)) {
                throw new CloudRuntimeException("Failed to delete custom action");
            }
            return true;
        });
    }

    @Override
    public List<ExtensionCustomActionResponse> listCustomActions(ListCustomActionCmd cmd) {
        Long id = cmd.getId();
        String name = cmd.getName();
        Long extensionId = cmd.getExtensionId();
        String keyword = cmd.getKeyword();
        final String resourceTypeStr = cmd.getResourceType();
        final String resourceId = cmd.getResourceId();
        final Boolean enabled = cmd.isEnabled();
        final SearchBuilder<ExtensionCustomActionVO> sb = extensionCustomActionDao.createSearchBuilder();
        final Filter searchFilter = new Filter(ExtensionCustomActionVO.class, "id", false, cmd.getStartIndex(), cmd.getPageSizeVal());
        final Account caller = CallContext.current().getCallingAccount();

        ExtensionCustomAction.ResourceType resourceType = null;
        if (StringUtils.isNotBlank(resourceTypeStr)) {
            resourceType = EnumUtils.getEnum(ExtensionCustomAction.ResourceType.class, resourceTypeStr);
            if (resourceType == null) {
                throw new InvalidParameterValueException("Invalid resource type specified");
            }
        }

        if (extensionId == null && resourceType != null && StringUtils.isNotBlank(resourceId)) {
            Extension extension = getExtensionFromResource(resourceType, resourceId);
            if (extension == null) {
                logger.error("No extension found for the specified resource [type: {}, id: {}]", resourceTypeStr, resourceId);
                throw new InvalidParameterValueException("Internal error listing custom actions with specified resource");
            }
            extensionId = extension.getId();
        }

        final Role role = roleService.findRole(caller.getRoleId());

        sb.and("id", sb.entity().getId(), SearchCriteria.Op.EQ);
        sb.and("extensionId", sb.entity().getExtensionId(), SearchCriteria.Op.EQ);
        sb.and("name", sb.entity().getName(), SearchCriteria.Op.EQ);
        sb.and("keyword", sb.entity().getName(), SearchCriteria.Op.LIKE);
        sb.and("enabled", sb.entity().isEnabled(), SearchCriteria.Op.EQ);
        if (resourceType != null) {
            sb.and().op("resourceTypeNull", sb.entity().getResourceType(), SearchCriteria.Op.NULL);
            sb.or("resourceType", sb.entity().getResourceType(), SearchCriteria.Op.EQ);
            sb.cp();
        }
        if (!RoleType.Admin.equals(role.getRoleType())) {
            sb.and("roleType", sb.entity().getAllowedRoleTypes(), SearchCriteria.Op.BINARY_OR);
        }
        sb.done();
        final SearchCriteria<ExtensionCustomActionVO> sc = sb.create();
        if (id != null) {
            sc.setParameters("id", id);
        }
        if (extensionId != null) {
            sc.setParameters("extensionId", extensionId);
        }
        if (StringUtils.isNotBlank(name)) {
            sc.setParameters("name", name);
        }
        if (StringUtils.isNotBlank(keyword)) {
            sc.setParameters("keyword",  "%" + keyword + "%");
        }
        if (enabled != null) {
            sc.setParameters("enabled",  true);
        }
        if (resourceType != null) {
            sc.setParameters("resourceType",  resourceType);
        }
        if (!RoleType.Admin.equals(role.getRoleType())) {
            sc.setParameters("roleType", role.getRoleType().getMask());
        }
        final Pair<List<ExtensionCustomActionVO>, Integer> result = extensionCustomActionDao.searchAndCount(sc, searchFilter);
        List<ExtensionCustomActionResponse> responses = new ArrayList<>();
        for (ExtensionCustomActionVO customAction : result.first()) {
            responses.add(createCustomActionResponse(customAction));
        }

        return responses;
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_EXTENSION_CUSTOM_ACTION_UPDATE, eventDescription = "updating extension custom action")
    public ExtensionCustomAction updateCustomAction(UpdateCustomActionCmd cmd) {
        final long id = cmd.getId();
        String description = cmd.getDescription();
        String resourceTypeStr = cmd.getResourceType();
        List<String> rolesStrList = cmd.getAllowedRoleTypes();
        Boolean enabled = cmd.isEnabled();
        Map parametersMap = cmd.getParametersMap();
        Boolean cleanupParameters = cmd.isCleanupParameters();
        final String successMessage = cmd.getSuccessMessage();
        final String errorMessage = cmd.getErrorMessage();
        final Integer timeout = cmd.getTimeout();
        Map<String, String> details = cmd.getDetails();
        Boolean cleanupDetails = cmd.isCleanupDetails();

        ExtensionCustomActionVO customAction = extensionCustomActionDao.findById(id);
        if (customAction == null) {
            throw new CloudRuntimeException("Action not found");
        }

        boolean needUpdate = false;
        if (StringUtils.isNotBlank(description)) {
            customAction.setDescription(description);
            needUpdate = true;
        }
        if (resourceTypeStr != null) {
            ExtensionCustomAction.ResourceType resourceType =
                    EnumUtils.getEnumIgnoreCase(ExtensionCustomAction.ResourceType.class, resourceTypeStr);
            if (resourceType == null) {
                throw new InvalidParameterValueException(
                        String.format("Invalid resource type specified: %s. Valid values are: %s", resourceTypeStr,
                                EnumSet.allOf(ExtensionCustomAction.ResourceType.class)));
            }
            customAction.setResourceType(resourceType);
            needUpdate = true;
        }
        if (CollectionUtils.isNotEmpty(rolesStrList)) {
            Set<RoleType> roles = new HashSet<>();
            for (String roleTypeStr : rolesStrList) {
                try {
                    RoleType roleType = RoleType.fromString(roleTypeStr);
                    roles.add(roleType);
                } catch (IllegalStateException ignored) {
                    throw new InvalidParameterValueException(String.format("Invalid role specified - %s", roleTypeStr));
                }
            }
            customAction.setAllowedRoleTypes(RoleType.toCombinedMask(roles));
            needUpdate = true;
        }
        if (successMessage != null) {
            customAction.setSuccessMessage(successMessage);
            needUpdate = true;
        }
        if (errorMessage != null) {
            customAction.setErrorMessage(errorMessage);
            needUpdate = true;
        }
        if (timeout != null) {
            customAction.setTimeout(timeout);
            needUpdate = true;
        }
        if (enabled != null) {
            customAction.setEnabled(enabled);
            needUpdate = true;
        }

        List<ExtensionCustomAction.Parameter> parameters = null;
        if (!Boolean.TRUE.equals(cleanupParameters) && MapUtils.isNotEmpty(parametersMap)) {
            parameters = getParametersListFromMap(customAction.getName(), parametersMap);
        }

        final boolean needUpdateFinal = needUpdate;
        final List<ExtensionCustomAction.Parameter> parametersFinal = parameters;
        return Transaction.execute((TransactionCallbackWithException<ExtensionCustomAction, CloudRuntimeException>) status -> {
            if (needUpdateFinal) {
                boolean result = extensionCustomActionDao.update(id, customAction);
                if (!result) {
                    throw new CloudRuntimeException(String.format("Failed to update custom action: %s",
                            customAction.getName()));
                }
            }
            updatedCustomActionDetails(id, cleanupDetails, details, cleanupParameters, parametersFinal);
            return customAction;
        });
    }

    protected void updatedCustomActionDetails(long id, Boolean cleanupDetails, Map<String, String> details,
            Boolean cleanupParameters, List<ExtensionCustomAction.Parameter> parametersFinal) {
        final boolean needToUpdateAllDetails = Boolean.TRUE.equals(cleanupDetails) || MapUtils.isNotEmpty(details);
        final boolean needToUpdateParameters = Boolean.TRUE.equals(cleanupParameters) || CollectionUtils.isNotEmpty(parametersFinal);
        if (!needToUpdateAllDetails && !needToUpdateParameters) {
            return;
        }
        if (needToUpdateAllDetails) {
            Map<String, String> hiddenDetails =
                    extensionCustomActionDetailsDao.listDetailsKeyPairs(id, false);
            List<ExtensionCustomActionDetailsVO> detailsVOList = new ArrayList<>();
            if (Boolean.TRUE.equals(cleanupParameters)) {
                hiddenDetails.remove(ApiConstants.PARAMETERS);
            } else if (CollectionUtils.isNotEmpty(parametersFinal)) {
                hiddenDetails.put(ApiConstants.PARAMETERS,
                        ExtensionCustomAction.Parameter.toJsonFromList(parametersFinal));
            }
            if (MapUtils.isNotEmpty(hiddenDetails)) {
                hiddenDetails.forEach((key, value) -> detailsVOList.add(
                        new ExtensionCustomActionDetailsVO(id, key, value, false)));
            }
            if (!Boolean.TRUE.equals(cleanupDetails) && MapUtils.isNotEmpty(details)) {
                details.forEach((key, value) -> detailsVOList.add(
                        new ExtensionCustomActionDetailsVO(id, key, value)));
            }
            if (CollectionUtils.isNotEmpty(detailsVOList)) {
                extensionCustomActionDetailsDao.saveDetails(detailsVOList);
            } else if (Boolean.TRUE.equals(cleanupDetails)) {
                extensionCustomActionDetailsDao.removeDetails(id);
            }
        } else {
            if (Boolean.TRUE.equals(cleanupParameters)) {
                extensionCustomActionDetailsDao.removeDetail(id, ApiConstants.PARAMETERS);
            } else if (CollectionUtils.isNotEmpty(parametersFinal)) {
                ExtensionCustomActionDetailsVO detailsVO = extensionCustomActionDetailsDao.findDetail(id,
                        ApiConstants.PARAMETERS);
                if (detailsVO == null) {
                    extensionCustomActionDetailsDao.persist(new ExtensionCustomActionDetailsVO(id,
                            ApiConstants.PARAMETERS,
                            ExtensionCustomAction.Parameter.toJsonFromList(parametersFinal), false));
                } else {
                    detailsVO.setValue(ExtensionCustomAction.Parameter.toJsonFromList(parametersFinal));
                    extensionCustomActionDetailsDao.update(detailsVO.getId(), detailsVO);
                }
            }
        }
    }

    @Override
    public CustomActionResultResponse runCustomAction(RunCustomActionCmd cmd) {
        final Long id =  cmd.getCustomActionId();
        final String resourceTypeStr = cmd.getResourceType();
        final String resourceUuid = cmd.getResourceId();
        Map<String, String> cmdParameters = cmd.getParameters();
        final Account caller = CallContext.current().getCallingAccount();

        String error = "Internal error running action";
        ExtensionCustomActionVO customActionVO = extensionCustomActionDao.findById(id);
        if (customActionVO == null) {
            logger.error("Invalid custom action specified with ID: {}", id);
            throw new InvalidParameterValueException(error);
        }
        final Role role = roleService.findRole(caller.getRoleId());
        if (!RoleType.Admin.equals(role.getRoleType())) {
            final Set<RoleType> allowedRoles = RoleType.fromCombinedMask(customActionVO.getAllowedRoleTypes());
            if (!allowedRoles.contains(role.getRoleType())) {
                logger.error("Caller does not have permission to run {} with {} having role: {}",
                        customActionVO, caller, role.getRoleType().name());
                throw new InvalidParameterValueException(error);
            }
        }
        if (!customActionVO.isEnabled()) {
            logger.error("Failed to run {} as it is not enabled", customActionVO);
            throw new InvalidParameterValueException(error);
        }
        final String actionName = customActionVO.getName();
        RunCustomActionCommand runCustomActionCommand = new RunCustomActionCommand(actionName);
        final long extensionId = customActionVO.getExtensionId();
        final ExtensionVO extensionVO = extensionDao.findById(extensionId);
        if (extensionVO == null) {
            logger.error("Unable to find extension for {}", customActionVO);
            throw new CloudRuntimeException(error);
        }
        if (!Extension.State.Enabled.equals(extensionVO.getState())) {
            logger.error("{} is not in enabled state for running {}", extensionVO, customActionVO);
            throw new CloudRuntimeException(error);
        }
        ExtensionCustomAction.ResourceType actionResourceType = customActionVO.getResourceType();
        if (actionResourceType == null && StringUtils.isBlank(resourceTypeStr)) {
            throw new InvalidParameterValueException("Resource type not specified for the action");
        }
        boolean validType = true;
        if (StringUtils.isNotBlank(resourceTypeStr)) {
            ExtensionCustomAction.ResourceType cmdResourceType =
                    EnumUtils.getEnumIgnoreCase(ExtensionCustomAction.ResourceType.class, resourceTypeStr);
            validType = cmdResourceType != null && (actionResourceType == null || actionResourceType.equals(cmdResourceType));
            actionResourceType = cmdResourceType;
        }
        if (!validType || actionResourceType == null) {
            logger.error("Invalid resource type - {} specified for {}", resourceTypeStr, customActionVO);
            throw new CloudRuntimeException(error);
        }
        Object entity = entityManager.findByUuid(actionResourceType.getAssociatedClass(), resourceUuid);
        if (entity == null) {
            logger.error("Specified resource does not exist for running {}", customActionVO);
            throw new CloudRuntimeException(error);
        }
        Long clusterId = null;
        Long hostId = null;
        if (entity instanceof Cluster) {
            clusterId = ((Cluster)entity).getId();
            List<HostVO> hosts = hostDao.listByClusterAndHypervisorType(clusterId, Hypervisor.HypervisorType.External);
            if (CollectionUtils.isEmpty(hosts)) {
                logger.error("No hosts found for {} for running {}", entity, customActionVO);
                throw new CloudRuntimeException(error);
            }
            hostId = hosts.get(0).getId();
        } else if (entity instanceof Host) {
            Host host = (Host)entity;
            if (!Hypervisor.HypervisorType.External.equals(host.getHypervisorType())) {
                logger.error("Invalid {} specified as host resource for running {}", entity, customActionVO);
                throw new InvalidParameterValueException(error);
            }
            hostId = host.getId();
            clusterId = host.getClusterId();
        } else if (entity instanceof VirtualMachine) {
            VirtualMachine virtualMachine = (VirtualMachine)entity;
            accountService.checkAccess(caller, null, true, virtualMachine);
            if (!Hypervisor.HypervisorType.External.equals(virtualMachine.getHypervisorType())) {
                logger.error("Invalid {} specified as VM resource for running {}", entity, customActionVO);
                throw new InvalidParameterValueException(error);
            }
            VirtualMachineProfile vmProfile = new VirtualMachineProfileImpl(virtualMachine);
            VirtualMachineTO virtualMachineTO = virtualMachineManager.toVmTO(vmProfile);
            runCustomActionCommand.setVmTO(virtualMachineTO);
            Pair<Long, Long> clusterAndHostId = virtualMachineManager.findClusterAndHostIdForVm(virtualMachine, false);
            clusterId = clusterAndHostId.first();
            hostId = clusterAndHostId.second();
        } else if (entity instanceof Network) {
            // Network custom action: dispatched directly to NetworkCustomActionProvider (no agent)
            Network network = (Network) entity;
            return runNetworkCustomAction(network, customActionVO, extensionVO, actionResourceType, cmdParameters);
        }

        if (clusterId == null || hostId == null) {
            logger.error(
                    "Unable to find cluster or host with the specified resource - cluster ID: {}, host ID: {}",
                    clusterId, hostId);
            throw new CloudRuntimeException(error);
        }

        ExtensionResourceMapVO extensionResource = extensionResourceMapDao.findByResourceIdAndType(clusterId,
                ExtensionResourceMap.ResourceType.Cluster);
        if (extensionResource == null) {
            logger.error("No extension registered with cluster ID: {}", clusterId);
            throw new CloudRuntimeException(error);
        }

        List<ExtensionCustomAction.Parameter> actionParameters = null;
        Pair<Map<String, String>, Map<String, String>> allDetails =
                extensionCustomActionDetailsDao.listDetailsKeyPairsWithVisibility(customActionVO.getId());
        if (allDetails.second().containsKey(ApiConstants.PARAMETERS)) {
            actionParameters =
                    ExtensionCustomAction.Parameter.toListFromJson(allDetails.second().get(ApiConstants.PARAMETERS));
        }
        Map<String, Object> parameters = null;
        if (CollectionUtils.isNotEmpty(actionParameters)) {
            parameters = ExtensionCustomAction.Parameter.validateParameterValues(actionParameters, cmdParameters);
        }

        CustomActionResultResponse response = new CustomActionResultResponse();
        response.setId(customActionVO.getUuid());
        response.setName(actionName);
        response.setObjectName("customactionresult");
        Map<String, String> result = new HashMap<>();
        response.setSuccess(false);
        result.put(ApiConstants.MESSAGE, getActionMessage(false, customActionVO, extensionVO,
                actionResourceType, entity));
        Map<String, Map<String, String>> externalDetails =
                getExternalAccessDetails(allDetails.first(), hostId, extensionResource);
        Map<String, String> vmExternalDetails = null;
        if (runCustomActionCommand.getVmTO() != null) {
            vmExternalDetails = runCustomActionCommand.getVmTO().getExternalDetails();
        }
        if (MapUtils.isNotEmpty(vmExternalDetails)) {
            externalDetails.put(ApiConstants.VIRTUAL_MACHINE, vmExternalDetails);
        }
        runCustomActionCommand.setParameters(parameters);
        runCustomActionCommand.setExternalDetails(externalDetails);
        runCustomActionCommand.setWait(customActionVO.getTimeout());
        try {
            logger.info("Running custom action: {} with {} parameters", actionName,
                    (parameters != null ? parameters.keySet().size() : 0));
            Answer answer = agentMgr.send(hostId, runCustomActionCommand);
            if (!(answer instanceof RunCustomActionAnswer)) {
                logger.error("Unexpected answer [{}] received for {}", answer.getClass().getSimpleName(),
                        RunCustomActionCommand.class.getSimpleName());
                result.put(ApiConstants.DETAILS, error);
            } else {
                RunCustomActionAnswer customActionAnswer = (RunCustomActionAnswer) answer;
                response.setSuccess(answer.getResult());
                result.put(ApiConstants.MESSAGE, getActionMessage(answer.getResult(), customActionVO, extensionVO,
                        actionResourceType, entity));
                result.put(ApiConstants.DETAILS, customActionAnswer.getDetails());
            }
        } catch (AgentUnavailableException e) {
            String msg = "Unable to run custom action";
            logger.error("{} due to {}", msg, e.getMessage(), e);
            result.put(ApiConstants.DETAILS, msg);
        } catch (OperationTimedoutException e) {
            String msg = "Running custom action timed out, please try again";
            logger.error(msg, e);
            result.put(ApiConstants.DETAILS, msg);
        }
        response.setResult(result);
        return response;
    }

    /**
     * Executes a custom action for a Network resource by delegating to an
     * available {@link NetworkCustomActionProvider} (e.g. NetworkExtensionElement).
     * This path does NOT go through the agent framework.
     */
    protected CustomActionResultResponse runNetworkCustomAction(Network network,
            ExtensionCustomActionVO customActionVO, ExtensionVO extensionVO,
            ExtensionCustomAction.ResourceType actionResourceType,
            Map<String, String> cmdParameters) {

        final String actionName = customActionVO.getName();
        CustomActionResultResponse response = new CustomActionResultResponse();
        response.setId(customActionVO.getUuid());
        response.setName(actionName);
        response.setObjectName("customactionresult");
        Map<String, String> result = new HashMap<>();
        response.setSuccess(false);
        result.put(ApiConstants.MESSAGE, getActionMessage(false, customActionVO, extensionVO, actionResourceType, network));

        // Resolve action parameters
        List<ExtensionCustomAction.Parameter> actionParameters = null;
        Pair<Map<String, String>, Map<String, String>> allDetails =
                extensionCustomActionDetailsDao.listDetailsKeyPairsWithVisibility(customActionVO.getId());
        if (allDetails.second().containsKey(ApiConstants.PARAMETERS)) {
            actionParameters = ExtensionCustomAction.Parameter.toListFromJson(
                    allDetails.second().get(ApiConstants.PARAMETERS));
        }
        Map<String, Object> parameters = null;
        if (CollectionUtils.isNotEmpty(actionParameters)) {
            parameters = ExtensionCustomAction.Parameter.validateParameterValues(actionParameters, cmdParameters);
        }

        // Find the provider name for this network (try each service until we find one)
        String providerName = null;
        for (Service service : new Service[]{Service.SourceNat, Service.StaticNat,
                Service.PortForwarding, Service.Firewall, Service.Gateway}) {
            providerName = networkServiceMapDao.getProviderForServiceInNetwork(network.getId(), service);
            if (StringUtils.isNotBlank(providerName)) {
                break;
            }
        }
        if (StringUtils.isBlank(providerName)) {
            logger.error("No network service provider found for network {}", network.getId());
            result.put(ApiConstants.DETAILS, "No network service provider found for this network");
            response.setResult(result);
            return response;
        }

        // Get the network element implementing that provider
        NetworkElement element = networkModel.getElementImplementingProvider(providerName);
        if (element == null) {
            logger.error("No NetworkElement found implementing provider '{}' for network {}", providerName, network.getId());
            result.put(ApiConstants.DETAILS, "No network element found for provider: " + providerName);
            response.setResult(result);
            return response;
        }

        // The element must implement NetworkCustomActionProvider
        if (!(element instanceof NetworkCustomActionProvider)) {
            logger.error("Network element '{}' for provider '{}' does not support custom actions",
                    element.getClass().getSimpleName(), providerName);
            result.put(ApiConstants.DETAILS, "Provider '" + providerName + "' does not support custom actions");
            response.setResult(result);
            return response;
        }

        NetworkCustomActionProvider provider = (NetworkCustomActionProvider) element;
        try {
            if (!provider.canHandleCustomAction(network)) {
                throw new CloudRuntimeException("Provider '" + providerName + "' cannot handle custom action for this network");
            }
            logger.info("Running network custom action '{}' on network {} via {} (provider: {})",
                    actionName, network.getId(), element.getClass().getSimpleName(), providerName);
            String output = provider.runCustomAction(network, actionName, parameters);
            boolean success = output != null;
            response.setSuccess(success);
            result.put(ApiConstants.MESSAGE, getActionMessage(success, customActionVO, extensionVO, actionResourceType, network));
            result.put(ApiConstants.DETAILS, success ? output : "Action failed — check management server logs for details");
        } catch (Exception e) {
            logger.error("Network custom action '{}' threw exception: {}", actionName, e.getMessage(), e);
            result.put(ApiConstants.DETAILS, "Action failed: " + e.getMessage());
        }
        response.setResult(result);
        return response;
    }

    @Override
    public ExtensionCustomActionResponse createCustomActionResponse(ExtensionCustomAction customAction) {
        ExtensionCustomActionResponse response = new ExtensionCustomActionResponse(customAction.getUuid(),
                customAction.getName(), customAction.getDescription());
        if (customAction.getResourceType() != null) {
            response.setResourceType(customAction.getResourceType().name());
        }
        Integer roles = ObjectUtils.defaultIfNull(customAction.getAllowedRoleTypes(), RoleType.Admin.getMask());
        response.setAllowedRoleTypes(RoleType.fromCombinedMask(roles)
                .stream()
                .map(Enum::name)
                .collect(Collectors.toList()));
        response.setSuccessMessage(customAction.getSuccessMessage());
        response.setErrorMessage(customAction.getErrorMessage());
        response.setTimeout(customAction.getTimeout());
        response.setEnabled(customAction.isEnabled());
        response.setCreated(customAction.getCreated());
        Optional.ofNullable(extensionDao.findById(customAction.getExtensionId())).ifPresent(extensionVO -> {
            response.setExtensionId(extensionVO.getUuid());
            response.setExtensionName(extensionVO.getName());
        });
        Pair<Map<String, String>, Map<String, String>> allDetails =
                extensionCustomActionDetailsDao.listDetailsKeyPairsWithVisibility(customAction.getId());
        Optional.ofNullable(allDetails.second().get(ApiConstants.PARAMETERS))
                .map(ExtensionCustomAction.Parameter::toListFromJson)
                .ifPresent(parameters -> {
                    List<ExtensionCustomActionParameterResponse> paramResponses = parameters.stream()
                            .map(p -> new ExtensionCustomActionParameterResponse(p.getName(),
                                    p.getType().name(), p.getValidationFormat().name(), p.getValueOptions(), p.isRequired()))
                            .collect(Collectors.toList());
                    response.setParameters(paramResponses);
                });
        response.setDetails(allDetails.first());
        response.setObjectName(ExtensionCustomAction.class.getSimpleName().toLowerCase());
        return response;
    }

    @Override
    public Map<String, Map<String, String>> getExternalAccessDetails(Host host, Map<String, String> vmDetails) {
        long clusterId = host.getClusterId();
        ExtensionResourceMapVO resourceMap = extensionResourceMapDao.findByResourceIdAndType(clusterId,
                ExtensionResourceMap.ResourceType.Cluster);
        Map<String, Map<String, String>> details = getExternalAccessDetails(null, host.getId(), resourceMap);
        if (MapUtils.isNotEmpty(vmDetails)) {
            details.put(ApiConstants.VIRTUAL_MACHINE, vmDetails);
        }
        return details;
    }

    @Override
    public String handleExtensionServerCommands(ExtensionServerActionBaseCommand command) {
        final String extensionName = command.getExtensionName();
        final String extensionRelativePath = command.getExtensionRelativePath();
        logger.debug("Received {} from MS: {} for extension [id: {}, name: {}, relativePath: {}]",
                command.getClass().getSimpleName(), command.getMsId(), command.getExtensionId(),
                extensionName, extensionRelativePath);
        Answer answer = new Answer(command, false, "Unsupported command");
        if (command instanceof GetExtensionPathChecksumCommand) {
            final GetExtensionPathChecksumCommand cmd = (GetExtensionPathChecksumCommand)command;
            String checksum = externalProvisioner.getChecksumForExtensionPath(extensionName,
                    extensionRelativePath);
            answer = new Answer(cmd, StringUtils.isNotBlank(checksum), checksum);
        } else if (command instanceof PrepareExtensionPathCommand) {
            final PrepareExtensionPathCommand cmd = (PrepareExtensionPathCommand)command;
            Pair<Boolean, String> result = prepareExtensionPathOnCurrentServer(
                    extensionName, cmd.isExtensionUserDefined(), extensionRelativePath);
            answer = new Answer(cmd, result.first(), result.second());
        } else if (command instanceof CleanupExtensionFilesCommand) {
            final CleanupExtensionFilesCommand cmd = (CleanupExtensionFilesCommand)command;
            Pair<Boolean, String> result = cleanupExtensionFilesOnCurrentServer(extensionName,
                    extensionRelativePath);
            answer = new Answer(cmd, result.first(), result.second());
        }
        final Answer[] answers = new Answer[1];
        answers[0] = answer;
        return GsonHelper.getGson().toJson(answers);
    }

    @Override
    public Pair<Boolean, ExtensionResourceMap> extensionResourceMapDetailsNeedUpdate(long resourceId,
                     ExtensionResourceMap.ResourceType resourceType, Map<String, String> externalDetails) {
        if (MapUtils.isEmpty(externalDetails)) {
            return new Pair<>(false, null);
        }
        ExtensionResourceMapVO extensionResourceMapVO =
                extensionResourceMapDao.findByResourceIdAndType(resourceId, resourceType);
        if (extensionResourceMapVO == null) {
            return new Pair<>(true, null);
        }
        Map<String, String> mapDetails =
                extensionResourceMapDetailsDao.listDetailsKeyPairs(extensionResourceMapVO.getId());
        if (MapUtils.isEmpty(mapDetails) || mapDetails.size() != externalDetails.size()) {
            return new Pair<>(true, extensionResourceMapVO);
        }
        for (Map.Entry<String, String> entry : externalDetails.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (!value.equals(mapDetails.get(key))) {
                return new Pair<>(true, extensionResourceMapVO);
            }
        }
        return new Pair<>(false, extensionResourceMapVO);
    }

    @Override
    public void updateExtensionResourceMapDetails(long extensionResourceMapId, Map<String, String> details) {
        if (MapUtils.isEmpty(details)) {
            return;
        }
        List<ExtensionResourceMapDetailsVO> detailsList =
                buildExtensionResourceDetailsArray(extensionResourceMapId, details);
        extensionResourceMapDetailsDao.saveDetails(detailsList);
    }

    @Override
    public Answer getInstanceConsole(VirtualMachine vm, Host host) {
        Extension extension = getExtensionForCluster(host.getClusterId());
        if (extension == null || !Extension.Type.Orchestrator.equals(extension.getType()) ||
                !Extension.State.Enabled.equals(extension.getState())) {
            logger.error("No enabled orchestrator {} found for the {} while trying to get console for {}",
                    extension == null ? "extension" : extension, host, vm);
            return new Answer(null, false,
                    String.format("No enabled orchestrator extension found for the host: %s", host.getName()));
        }
        VirtualMachineProfile vmProfile = new VirtualMachineProfileImpl(vm);
        VirtualMachineTO virtualMachineTO = virtualMachineManager.toVmTO(vmProfile);
        GetExternalConsoleCommand cmd = new GetExternalConsoleCommand(vm.getInstanceName(), virtualMachineTO);
        Map<String, Map<String, String>> externalAccessDetails =
                getExternalAccessDetails(host, virtualMachineTO.getExternalDetails());
        cmd.setExternalDetails(externalAccessDetails);
        return agentMgr.easySend(host.getId(), cmd);
    }

    @Override
    public Long getExtensionIdForCluster(long clusterId) {
        ExtensionResourceMapVO map = extensionResourceMapDao.findByResourceIdAndType(clusterId,
                ExtensionResourceMap.ResourceType.Cluster);
        if (map == null) {
            return null;
        }
        return map.getExtensionId();
    }

    @Override
    public Extension getExtension(long id) {
        return extensionDao.findById(id);
    }

    @Override
    public Extension getExtensionForCluster(long clusterId) {
        Long extensionId = getExtensionIdForCluster(clusterId);
        if (extensionId == null) {
            return null;
        }
        return extensionDao.findById(extensionId);
    }

    @Override
    public List<String> getExtensionReservedResourceDetails(long extensionId) {
        ExtensionDetailsVO detailsVO = extensionDetailsDao.findDetail(extensionId,
                ApiConstants.RESERVED_RESOURCE_DETAILS);
        if (detailsVO == null || !StringUtils.isNotBlank(detailsVO.getValue())) {
            return Collections.emptyList();
        }
        List<String> reservedDetails = new ArrayList<>();
        String[] parts = detailsVO.getValue().split(",");
        for (String part : parts) {
            if (StringUtils.isNotBlank(part)) {
                reservedDetails.add(part.trim());
            }
        }
        addInbuiltExtensionReservedResourceDetails(extensionId, reservedDetails);
        return reservedDetails;
    }

    @Override
    public Long getExtensionIdForPhysicalNetwork(long physicalNetworkId) {
        // Returns the first (primary) extension for backward compatibility
        List<ExtensionResourceMapVO> maps = extensionResourceMapDao.listByResourceIdAndType(physicalNetworkId,
                ExtensionResourceMap.ResourceType.PhysicalNetwork);
        if (maps == null || maps.isEmpty()) {
            return null;
        }
        return maps.get(0).getExtensionId();
    }

    @Override
    public Extension getExtensionForPhysicalNetwork(long physicalNetworkId) {
        Long extensionId = getExtensionIdForPhysicalNetwork(physicalNetworkId);
        if (extensionId == null) {
            return null;
        }
        return extensionDao.findById(extensionId);
    }

    @Override
    public boolean start() {
        long pathStateCheckInterval = PathStateCheckInterval.value();
        long pathStateCheckInitialDelay = Math.min(60, pathStateCheckInterval);
        logger.debug("Scheduling extensions path state check task with initial delay={}s and interval={}s",
                pathStateCheckInitialDelay, pathStateCheckInterval);
        extensionPathStateCheckExecutor.scheduleWithFixedDelay(new PathStateCheckWorker(),
                pathStateCheckInitialDelay, pathStateCheckInterval, TimeUnit.SECONDS);
        return true;
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        try {
            extensionPathStateCheckExecutor = Executors.newScheduledThreadPool(1,
                    new NamedThreadFactory("Extension-Path-State-Check"));
        } catch (final Exception e) {
            throw new ConfigurationException("Unable to to configure ExtensionsManagerImpl");
        }
        return true;
    }

    @Override
    public List<Class<?>> getCommands() {
        List<Class<?>> cmds = new ArrayList<>();
        cmds.add(AddCustomActionCmd.class);
        cmds.add(ListCustomActionCmd.class);
        cmds.add(DeleteCustomActionCmd.class);
        cmds.add(UpdateCustomActionCmd.class);
        cmds.add(RunCustomActionCmd.class);

        cmds.add(CreateExtensionCmd.class);
        cmds.add(ListExtensionsCmd.class);
        cmds.add(DeleteExtensionCmd.class);
        cmds.add(UpdateExtensionCmd.class);
        cmds.add(RegisterExtensionCmd.class);
        cmds.add(UnregisterExtensionCmd.class);
        cmds.add(UpdateRegisteredExtensionCmd.class);
        return cmds;
    }

    @Override
    public String getConfigComponentName() {
        return ExtensionsManager.class.getSimpleName();
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey[]{
                PathStateCheckInterval
        };
    }

    public class PathStateCheckWorker extends ManagedContextRunnable {

        protected void runCheckUsingLongestRunningManagementServer() {
            try {
                List<ManagementServerHostVO> msHosts = managementServerHostDao.listBy(ManagementServerHost.State.Up);
                msHosts.sort(Comparator.comparingLong(ManagementServerHostVO::getRunid));
                ManagementServerHostVO msHost = msHosts.remove(0);
                if (msHost == null || (msHost.getMsid() != ManagementServerNode.getManagementServerId())) {
                    logger.debug("Skipping the extensions path state check on this management server");
                    return;
                }
                List<ExtensionVO> extensions = extensionDao.listAll();
                for (ExtensionVO extension : extensions) {
                    checkExtensionPathState(extension, msHosts);
                }
            } catch (Exception e) {
                logger.warn("Extensions path state check failed", e);
            }
        }

        @Override
        protected void runInContext() {
            GlobalLock gcLock = GlobalLock.getInternLock("ExtensionPathStateCheck");
            try {
                if (gcLock.lock(3)) {
                    try {
                        runCheckUsingLongestRunningManagementServer();
                    } finally {
                        gcLock.unlock();
                    }
                }
            } finally {
                gcLock.releaseRef();
            }
        }
    }

    @Override
    public String getExtensionScriptPath(Extension extension) {
        if (extension == null) {
            return null;
        }
        return externalProvisioner.getExtensionPath(extension.getRelativePath());
    }

    @Override
    public Map<String, String> getExtensionDetails(long extensionId) {
        return extensionDetailsDao.listDetailsKeyPairs(extensionId);
    }

    @Override
    public Extension getExtensionForPhysicalNetworkAndProvider(long physicalNetworkId, String providerName) {
        if (StringUtils.isBlank(providerName)) {
            return null;
        }
        List<ExtensionResourceMapVO> maps = extensionResourceMapDao.listByResourceIdAndType(
                physicalNetworkId, ExtensionResourceMap.ResourceType.PhysicalNetwork);
        if (maps == null || maps.isEmpty()) {
            return null;
        }
        for (ExtensionResourceMapVO map : maps) {
            ExtensionVO ext = extensionDao.findById(map.getExtensionId());
            if (ext != null && providerName.equalsIgnoreCase(ext.getName())) {
                return ext;
            }
        }
        return null;
    }

    @Override
    public Map<String, String> getAllResourceMapDetailsForExtensionOnPhysicalNetwork(long physicalNetworkId, long extensionId) {
        List<ExtensionResourceMapVO> maps = extensionResourceMapDao.listByResourceIdAndType(
                physicalNetworkId, ExtensionResourceMap.ResourceType.PhysicalNetwork);
        if (maps == null || maps.isEmpty()) {
            return new HashMap<>();
        }
        for (ExtensionResourceMapVO map : maps) {
            if (map.getExtensionId() == extensionId) {
                Map<String, String> details = extensionResourceMapDetailsDao.listDetailsKeyPairs(map.getId());
                return details != null ? details : new HashMap<>();
            }
        }
        return new HashMap<>();
    }

    @Override
    public boolean isNetworkExtensionProvider(String providerName) {
        if (StringUtils.isBlank(providerName)) {
            return false;
        }
        List<ExtensionVO> networkOrchExtensions = extensionDao.listByType(Extension.Type.NetworkOrchestrator);
        if (networkOrchExtensions == null || networkOrchExtensions.isEmpty()) {
            return false;
        }
        return networkOrchExtensions.stream()
                .anyMatch(ext -> providerName.equalsIgnoreCase(ext.getName()));
    }

    @Override
    public List<Extension> listExtensionsByType(Extension.Type type) {
        if (type == null) {
            return new ArrayList<>();
        }
        List<ExtensionVO> extensions = extensionDao.listByType(type);
        if (extensions == null || extensions.isEmpty()) {
            return new ArrayList<>();
        }
        return new ArrayList<>(extensions);
    }

    @Override
    public Map<Service, Map<Capability, String>> getNetworkCapabilitiesForProvider(Long physicalNetworkId,
            String providerName) {
        if (StringUtils.isBlank(providerName)) {
            return new HashMap<>();
        }
        Extension extension = null;
        if (physicalNetworkId != null) {
            extension = getExtensionForPhysicalNetworkAndProvider(physicalNetworkId, providerName);
        }
        if (extension == null) {
            // Search across all physical networks
            List<ExtensionVO> networkOrchExtensions = extensionDao.listByType(Extension.Type.NetworkOrchestrator);
            if (networkOrchExtensions != null) {
                for (ExtensionVO ext : networkOrchExtensions) {
                    if (providerName.equalsIgnoreCase(ext.getName())) {
                        extension = ext;
                        break;
                    }
                }
            }
        }
        if (extension == null) {
            return new HashMap<>();
        }
        Map<String, String> extDetails = extensionDetailsDao.listDetailsKeyPairs(extension.getId());
        return buildCapabilitiesFromDetailKeys(extDetails);
    }
}
