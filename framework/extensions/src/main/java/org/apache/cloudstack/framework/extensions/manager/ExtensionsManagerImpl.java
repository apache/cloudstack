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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

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
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.RunCustomActionAnswer;
import com.cloud.agent.api.RunCustomActionCommand;
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
import com.cloud.org.Cluster;
import com.cloud.utils.Pair;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.component.PluggableService;
import com.cloud.utils.db.EntityManager;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallbackWithException;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineManager;
import com.cloud.vm.VmDetailConstants;
import com.cloud.vm.dao.VMInstanceDao;

public class ExtensionsManagerImpl extends ManagerBase implements ExtensionsManager, PluggableService {

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

    protected String getExtensionSafeName(String name) {
        return  name.replaceAll("[^a-zA-Z0-9._-]", "_").toLowerCase();
    }

    protected String getDefaultExtensionRelativeEntryPoint(String name) {
        String safeName = getExtensionSafeName(name);
        return String.format("%s%s%s.sh", safeName, File.separator, safeName);
    }

    protected String getValidatedExtensionRelativeEntryPoint(String name, String relativeEntryPointPath) {
        String safeName = getExtensionSafeName(name);
        String normalizedPath = relativeEntryPointPath.replace("\\", "/");
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

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_EXTENSION_CREATE, eventDescription = "creating extension")
    public Extension createExtension(CreateExtensionCmd cmd) {
        final String name = cmd.getName();
        final String description = cmd.getDescription();
        final String typeStr = cmd.getType();
        String entryPoint = cmd.getEntryPoint();
        ExtensionVO extensionByName = extensionDao.findByName(name);
        if (extensionByName != null) {
            throw new CloudRuntimeException("Extension by name already exists");
        }
        if (!EnumUtils.isValidEnum(Extension.Type.class, typeStr)) {
            throw new CloudRuntimeException(String.format("Invalid type specified - %s", typeStr));
        }
        if (StringUtils.isBlank(entryPoint)) {
            entryPoint = getDefaultExtensionRelativeEntryPoint(name);
        } else {
            entryPoint = getValidatedExtensionRelativeEntryPoint(name, entryPoint);
        }
        final String entryPointFinal = entryPoint;
        return Transaction.execute((TransactionCallbackWithException<Extension, CloudRuntimeException>) status -> {
            ExtensionVO extension = new ExtensionVO(name, description, EnumUtils.getEnum(Extension.Type.class, typeStr),
                    entryPointFinal);
            extension = extensionDao.persist(extension);

            Map<String, String> details = cmd.getDetails();
            List<ExtensionDetailsVO> detailsVOList = new ArrayList<>();
            if (MapUtils.isNotEmpty(details)) {
                for (Map.Entry<String, String> entry : details.entrySet()) {
                    detailsVOList.add(new ExtensionDetailsVO(extension.getId(), entry.getKey(), entry.getValue()));
                }
                extensionDetailsDao.saveDetails(detailsVOList);
            }
            externalProvisioner.prepareExtensionEntryPoint(extension.getName(), extension.isUserDefined(), extension.getRelativeEntryPoint());
            CallContext.current().setEventResourceId(extension.getId());
            return extension;
        });
    }

    @Override
    public List<ExtensionResponse> listExtensions(ListExtensionsCmd cmd) {
        Long id = cmd.getExtensionId();
        String name = cmd.getName();
        String keyword = cmd.getKeyword();
        final SearchBuilder<ExtensionVO> sb = extensionDao.createSearchBuilder();
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
        final Map<String, String> details = cmd.getDetails();
        final Boolean cleanupDetails = cmd.isCleanupDetails();
        ExtensionVO extensionVO = extensionDao.findById(id);
        if (extensionVO == null) {
            throw new InvalidParameterValueException("Failed to find the extension");
        }
        return Transaction.execute((TransactionCallbackWithException<ExtensionVO, CloudRuntimeException>) status -> {
            if (description != null) {
                extensionVO.setDescription(description);
                if (!extensionDao.update(id, extensionVO)) {
                    throw new CloudRuntimeException(String.format("Failed to updated the extension: %s",
                            extensionVO.getName()));
                }
            }
            if (Boolean.TRUE.equals(cleanupDetails) || MapUtils.isNotEmpty(details)) {
                extensionDetailsDao.removeDetails(extensionVO.getId());
                List<ExtensionDetailsVO> detailsVOList = new ArrayList<>();
                if (!Boolean.TRUE.equals(cleanupDetails) && MapUtils.isNotEmpty(details)) {
                    for (Map.Entry<String, String> entry : details.entrySet()) {
                        detailsVOList.add(new ExtensionDetailsVO(extensionVO.getId(), entry.getKey(), entry.getValue()));
                    }
                }
                extensionDetailsDao.saveDetails(detailsVOList);
            }
            return extensionVO;
        });
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_EXTENSION_DELETE, eventDescription = "deleting extension")
    public boolean deleteExtension(DeleteExtensionCmd cmd) {
        Long extensionId = cmd.getId();
        ExtensionVO extension = extensionDao.findById(extensionId);
        if (extension == null) {
            throw new InvalidParameterValueException("Unable to find the extension with the specified id");
        }
        List<ExtensionResourceMapVO> registeredResources = extensionResourceMapDao.listByExtensionId(extensionId);
        if (CollectionUtils.isNotEmpty(registeredResources)) {
            throw new CloudRuntimeException("There are resources registered with this extension, unregister the extension from them");
        }

        return Transaction.execute((TransactionCallbackWithException<Boolean, CloudRuntimeException>) status -> {
            extensionDetailsDao.removeDetails(extensionId);
            extensionDao.remove(extensionId);
            return true;
        });
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_EXTENSION_RESOURCE_REGISTER, eventDescription = "registering extension resource")
    public Extension registerExtensionWithResource(RegisterExtensionCmd cmd) {
        String resourceId = cmd.getResourceId();
        Long extensionId = cmd.getExtensionId();
        String resourceType = cmd.getResourceType();
        if (!EnumUtils.isValidEnum(ExtensionResourceMap.ResourceType.class, resourceType)) {
            throw new InvalidParameterValueException(
                    String.format("Currently only [%s] can be used to register an extension of type Orchestrator",
                            EnumSet.allOf(ExtensionResourceMap.ResourceType.class)));
        }
        ClusterVO clusterVO = clusterDao.findByUuid(resourceId);
        if (clusterVO == null) {
            throw new InvalidParameterValueException("Invalid cluster ID specified");
        }
        ExtensionResourceMap extensionResourceMap = registerExtensionWithCluster(clusterVO, extensionId, cmd.getDetails());
        return extensionDao.findById(extensionResourceMap.getExtensionId());
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_EXTENSION_RESOURCE_REGISTER, eventDescription = "registering extension resource")
    public ExtensionResourceMap registerExtensionWithCluster(Cluster cluster, long extensionId,
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
            throw new CloudRuntimeException("Extension already registered with this resource");
        }
        return Transaction.execute((TransactionCallbackWithException<ExtensionResourceMap, CloudRuntimeException>) status -> {
            ExtensionResourceMapVO extensionMap = new ExtensionResourceMapVO(extensionId, cluster.getId(), resourceType);
            ExtensionResourceMapVO savedExtensionMap = extensionResourceMapDao.persist(extensionMap);
            List<ExtensionResourceMapDetailsVO> detailsVOList = new ArrayList<>();
            if (MapUtils.isNotEmpty(details)) {
                for (Map.Entry<String, String> entry : details.entrySet()) {
                    detailsVOList.add(new ExtensionResourceMapDetailsVO(savedExtensionMap.getId(),
                            entry.getKey(), entry.getValue()));
                }
                extensionResourceMapDetailsDao.saveDetails(detailsVOList);
            }
            return extensionMap;
        });
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_EXTENSION_RESOURCE_UNREGISTER, eventDescription = "unregistering extension resource")
    public Extension unregisterExtensionWithResource(UnregisterExtensionCmd cmd) {
        final String resourceId = cmd.getResourceId();
        final Long extensionId = cmd.getExtensionId();
        final String resourceType = cmd.getResourceType();
        if (!EnumUtils.isValidEnum(ExtensionResourceMap.ResourceType.class, resourceType)) {
            throw new InvalidParameterValueException(
                    String.format("Currently only [%s] can be used to unregister an extension of type Orchestrator",
                            EnumSet.allOf(ExtensionResourceMap.ResourceType.class)));
        }
        unregisterExtensionWithCluster(resourceId, extensionId);
        return extensionDao.findById(extensionId);
    }

    protected void unregisterExtensionWithCluster(String clusterUuid, Long extensionId) {
        ClusterVO cluster = clusterDao.findByUuid(clusterUuid);
        if (cluster == null) {
            throw new InvalidParameterValueException("Unable to find cluster with given ID");
        }
        unregisterExtensionWithCluster(cluster, extensionId);
    }

    @Override
    public void unregisterExtensionWithCluster(Cluster cluster, Long extensionId) {
        ExtensionResourceMapVO existing = extensionResourceMapDao.findByResourceIdAndType(cluster.getId(),
                ExtensionResourceMap.ResourceType.Cluster);
        if (existing == null) {
            return;
        }
        extensionResourceMapDao.remove(existing.getId());
        extensionResourceMapDetailsDao.removeDetails(existing.getId());
    }

    @Override
    public ExtensionResponse createExtensionResponse(Extension extension,
                 EnumSet<ApiConstants.ExtensionDetails> viewDetails) {
        ExtensionResponse response = new ExtensionResponse(extension.getUuid(), extension.getName(),
                extension.getDescription(), extension.getType().name());
        response.setCreated(extension.getCreated());
        response.setEntryPoint(externalProvisioner.getExtensionEntryPoint(extension.getRelativeEntryPoint()));
        response.setUserDefined(extension.isUserDefined());
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
                }
                Map<String, String> details = extensionResourceMapDetailsDao.listDetailsKeyPairs(
                        extensionResourceMapVO.getId());
                if (MapUtils.isNotEmpty(details)) {
                    extensionResourceResponse.setDetails(details);
                }
                resourcesResponse.add(extensionResourceResponse);
            }
            if (CollectionUtils.isNotEmpty(resourcesResponse)) {
                response.setResources(resourcesResponse);
            }
        }
        if (viewDetails.contains(ApiConstants.ExtensionDetails.all) ||
                viewDetails.contains(ApiConstants.ExtensionDetails.external)) {
            Map<String, String> extensionDetails = extensionDetailsDao.listDetailsKeyPairs(extension.getId());
            if (MapUtils.isNotEmpty(extensionDetails)) {
                response.setDetails(extensionDetails);
            }
        }
        response.setObjectName(Extension.class.getSimpleName().toLowerCase());
        return response;
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

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_EXTENSION_CUSTOM_ACTION_ADD, eventDescription = "adding extension custom action")
    public ExtensionCustomAction addCustomAction(AddCustomActionCmd cmd) {
        String name = cmd.getName();
        String description = cmd.getDescription();
        Long extensionId = cmd.getExtensionId();
        String resourceTypeStr = cmd.getResourceType();
        List<String> rolesStrList = cmd.getRolesList();
        final boolean enabled = cmd.isEnabled();
        Map parametersMap = cmd.getParametersMap();
        final String successMessage = cmd.getSuccessMessage();
        final String errorMessage = cmd.getErrorMessage();
        Map<String, String> details = cmd.getDetails();
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
                    new ExtensionCustomActionVO(name, description, extensionId, successMessage, errorMessage, enabled);
            if (resourceTypeFinal != null) {
                customAction.setResourceType(resourceTypeFinal);
            }
            customAction.setRoles(RoleType.toCombinedMask(roleTypes));
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
            extensionCustomActionDao.remove(customActionId);
            return true;
        });
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
        }
        if (clusterId == null) {
            return null;
        }
        ExtensionResourceMapVO mapVO =
                extensionResourceMapDao.findByResourceIdAndType(clusterId, ExtensionResourceMap.ResourceType.Cluster);
        if (mapVO == null) {
            return null;
        }
        return extensionDao.findById(mapVO.getExtensionId());
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

        sb.and("id", sb.entity().getId(), SearchCriteria.Op.EQ);
        sb.and("extensionid", sb.entity().getExtensionId(), SearchCriteria.Op.EQ);
        sb.and("name", sb.entity().getName(), SearchCriteria.Op.EQ);
        sb.and("keyword", sb.entity().getName(), SearchCriteria.Op.LIKE);
        sb.and("enabled", sb.entity().isEnabled(), SearchCriteria.Op.EQ);
        if (resourceType != null) {
            sb.and().op("resourceTypeNull", sb.entity().getResourceType(), SearchCriteria.Op.NULL);
            sb.or("resourceType", sb.entity().getResourceType(), SearchCriteria.Op.EQ);
            sb.cp();
        }
        sb.done();
        final SearchCriteria<ExtensionCustomActionVO> sc = sb.create();
        if (id != null) {
            sc.setParameters("id", id);
        }
        if (extensionId != null) {
            sc.setParameters("extensionid", extensionId);
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
        List<String> rolesStrList = cmd.getRoles();
        Boolean enabled = cmd.isEnabled();
        Map parametersMap = cmd.getParametersMap();
        Boolean cleanupParameters = cmd.isCleanupParameters();
        final String successMessage = cmd.getSuccessMessage();
        final String errorMessage = cmd.getErrorMessage();
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
            customAction.setRoles(RoleType.toCombinedMask(roles));
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
            List<ExtensionCustomActionDetailsVO> detailsVOList = new ArrayList<>();
            if (Boolean.TRUE.equals(cleanupParameters) || CollectionUtils.isNotEmpty(parametersFinal)) {
                extensionCustomActionDetailsDao.removeDetail(customAction.getId(), ApiConstants.PARAMETERS);
                if (CollectionUtils.isNotEmpty(parametersFinal)) {
                    detailsVOList.add(new ExtensionCustomActionDetailsVO(
                            customAction.getId(),
                            ApiConstants.PARAMETERS,
                            ExtensionCustomAction.Parameter.toJsonFromList(parametersFinal),
                            false
                    ));
                }
            }

            if (Boolean.TRUE.equals(cleanupDetails) || MapUtils.isNotEmpty(details)) {
                if (CollectionUtils.isNotEmpty(detailsVOList)) {
                    ExtensionCustomActionDetailsVO paramDetails =
                            extensionCustomActionDetailsDao.findDetail(customAction.getId(), ApiConstants.PARAMETERS);
                    if (paramDetails != null) {
                        detailsVOList.add(paramDetails);
                    }
                }
                extensionCustomActionDetailsDao.removeDetails(customAction.getId());
                if (!Boolean.TRUE.equals(cleanupDetails) && MapUtils.isNotEmpty(details)) {
                    details.forEach((key, value) -> detailsVOList.add(
                            new ExtensionCustomActionDetailsVO(customAction.getId(), key, value)));
                }
            }
            if (CollectionUtils.isNotEmpty(detailsVOList)) {
                extensionCustomActionDetailsDao.saveDetails(detailsVOList);
            }

            return customAction;
        });
    }

    protected String getActionMessage(boolean success, ExtensionCustomAction action, Extension extension) {
        String  msg = success ? action.getSuccessMessage() : action.getErrorMessage();
        if (StringUtils.isNotBlank(msg)) {
            Map<String, String> values = new HashMap<>();
            values.put("actionName", action.getName());
            values.put("extensionName", extension.getName());
            String result = msg;
            for (Map.Entry<String, String> entry : values.entrySet()) {
                result = result.replace("{{" + entry.getKey() + "}}", entry.getValue());
            }
            return result;
        }
        return success ? String.format("Successfully completed %s", action.getName()) :
                String.format("Failed to complete %s", action.getName());
    }

    @Override
    public CustomActionResultResponse runCustomAction(RunCustomActionCmd cmd) {
        final Long id =  cmd.getCustomActionId();
        final String resourceTypeStr = cmd.getResourceType();
        final String resourceUuid = cmd.getResourceId();
        Map<String, String> cmdParameters = cmd.getParameters();

        ExtensionCustomActionVO customActionVO = extensionCustomActionDao.findById(id);
        if (customActionVO == null) {
            throw new InvalidParameterValueException("Invalid custom action specified");
        }
        final String actionName = customActionVO.getName();
        RunCustomActionCommand runCustomActionCommand = new RunCustomActionCommand(actionName);
        final long extensionId = customActionVO.getExtensionId();
        final ExtensionVO extensionVO = extensionDao.findById(extensionId);
        if (extensionVO == null) {
            logger.error("Unable to find extension for {}", customActionVO);
            throw new CloudRuntimeException("Internal error running action");
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
            throw new CloudRuntimeException("Internal error running action");
        }
        Object entity = entityManager.findByUuid(actionResourceType.getAssociatedClass(), resourceUuid);
        if (entity == null) {
            logger.error("Specified resource does not exist for running {}", customActionVO);
            throw new CloudRuntimeException("Internal error running action");
        }
        Long clusterId = null;
        Long hostId = null;
        if (entity instanceof Cluster) {
            clusterId = ((Cluster)entity).getId();
            List<HostVO> hosts = hostDao.listByClusterAndHypervisorType(clusterId, Hypervisor.HypervisorType.External);
            if (CollectionUtils.isEmpty(hosts)) {
                logger.error("No hosts found for {} for running {}", entity, customActionVO);
                throw new CloudRuntimeException("Internal error running action");
            }
            hostId = hosts.get(0).getId();
        } else if (entity instanceof Host) {
            Host host = (Host)entity;
            if (!Hypervisor.HypervisorType.External.equals(host.getHypervisorType())) {
                logger.error("Invalid {} specified as resource for running {}", entity, customActionVO);
                throw new InvalidParameterValueException("Invalid resource specified");
            }
            hostId = host.getId();
            clusterId = host.getClusterId();
        } else if (entity instanceof VirtualMachine) {
            VirtualMachine virtualMachine = (VirtualMachine)entity;
            runCustomActionCommand.setVmId(virtualMachine.getId());
            if (!Hypervisor.HypervisorType.External.equals(virtualMachine.getHypervisorType())) {
                logger.error("Invalid {} specified as resource for running {}", entity, customActionVO);
                throw new InvalidParameterValueException("Invalid resource specified");
            }
            Pair<Long, Long> clusterAndHostId = virtualMachineManager.findClusterAndHostIdForVm(virtualMachine, false);
            clusterId = clusterAndHostId.first();
            hostId = clusterAndHostId.second();
        }

        if (clusterId == null || hostId == null) {
            logger.error(
                    "Unable to find cluster or host with the specified resource - cluster ID: {}, host ID: {}",
                    clusterId, hostId);
            throw new CloudRuntimeException("Internal resource specified");
        }

        ExtensionResourceMapVO extensionResource = extensionResourceMapDao.findByResourceIdAndType(clusterId,
                ExtensionResourceMap.ResourceType.Cluster);
        if (extensionResource == null) {
            logger.error("No extension registered with cluster ID: {}", clusterId);
            throw new CloudRuntimeException("Internal error running action");
        }

        List<ExtensionCustomAction.Parameter> actionParameters = null;
        Map<String, String> details =
                extensionCustomActionDetailsDao.listDetailsKeyPairs(customActionVO.getId());
        if (details.containsKey(ApiConstants.PARAMETERS)) {
            actionParameters =
                    ExtensionCustomAction.Parameter.toListFromJson(details.get(ApiConstants.PARAMETERS));
            details.remove(ApiConstants.PARAMETERS);
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
        result.put(ApiConstants.MESSAGE, getActionMessage(false, customActionVO, extensionVO));
        Map<String, Object> externalDetails = getExternalAccessDetails(details, hostId, extensionResource, extensionVO);
        runCustomActionCommand.setParameters(parameters);
        runCustomActionCommand.setExternalDetails(externalDetails);
        try {
            Answer answer = agentMgr.send(hostId, runCustomActionCommand);
            if (!(answer instanceof RunCustomActionAnswer)) {
                logger.error("Unexpected answer [{}] received for {}", answer.getClass().getSimpleName(), RunCustomActionCommand.class.getSimpleName());
                result.put(ApiConstants.DETAILS, "Internal error running action");
            } else {
                RunCustomActionAnswer customActionAnswer = (RunCustomActionAnswer) answer;
                response.setSuccess(answer.getResult());
                result.put(ApiConstants.MESSAGE, getActionMessage(answer.getResult(), customActionVO, extensionVO));
                // ToDo: Check if we should pass the details for an errored action or pass it at all
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

    @Override
    public ExtensionCustomActionResponse createCustomActionResponse(ExtensionCustomAction customAction) {
        ExtensionCustomActionResponse response = new ExtensionCustomActionResponse(customAction.getUuid(),
                customAction.getName(), customAction.getDescription());
        if (customAction.getResourceType() != null) {
            response.setResourceType(customAction.getResourceType().name());
        }
        Integer roles = ObjectUtils.defaultIfNull(customAction.getRoles(), RoleType.Admin.getMask());
        response.setRoles(RoleType.fromCombinedMask(roles).stream().map(Enum::name).collect(Collectors.toList()));
        response.setSuccessMessage(customAction.getSuccessMessage());
        response.setErrorMessage(customAction.getErrorMessage());
        response.setEnabled(customAction.isEnabled());
        response.setCreated(customAction.getCreated());
        Optional.ofNullable(extensionDao.findById(customAction.getExtensionId())).ifPresent(extensionVO -> {
            response.setExtensionId(extensionVO.getUuid());
            response.setExtensionName(extensionVO.getName());
        });
        Optional.ofNullable(extensionCustomActionDetailsDao.findDetail(customAction.getId(), ApiConstants.PARAMETERS))
                .map(ExtensionCustomActionDetailsVO::getValue)
                .map(ExtensionCustomAction.Parameter::toListFromJson)
                .ifPresent(parameters -> {
                    Set<ExtensionCustomActionParameterResponse> paramResponses = parameters.stream()
                            .map(p -> new ExtensionCustomActionParameterResponse(p.getName(),
                                    p.getType().name(), p.getFormat().name(), p.getOptions(), p.isRequired()))
                            .collect(Collectors.toSet());
                    response.setParameters(paramResponses);
                });
        Map<String, String> details =
                extensionCustomActionDetailsDao.listDetailsKeyPairs(customAction.getId(), true);
        response.setDetails(details);
        response.setObjectName(ExtensionCustomAction.class.getSimpleName().toLowerCase());
        return response;
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

    @Override
    public Map<String, Object> getExternalAccessDetails(Host host) {
        Map<String, Object> externalDetails = new HashMap<>();
        Map<String, String> hostDetails = getFilteredExternalDetails(hostDetailsDao.findDetails(host.getId()));
        externalDetails.put(ApiConstants.HOST_ID, hostDetails);
        long clusterId = host.getClusterId();
        ExtensionResourceMapVO resourceMap = extensionResourceMapDao.findByResourceIdAndType(clusterId,
                ExtensionResourceMap.ResourceType.Cluster);
        if (resourceMap == null) {
            return externalDetails;
        }
        Map<String, String> resourceDetails = extensionResourceMapDetailsDao.listDetailsKeyPairs(resourceMap.getId());
        externalDetails.put(ApiConstants.RESOURCE_ID, resourceDetails);
        Map<String, String> extensionDetails = extensionDetailsDao.listDetailsKeyPairs(resourceMap.getExtensionId());
        externalDetails.put(ApiConstants.EXTENSION_ID, extensionDetails);
        return externalDetails;
    }

    private Map<String, Object> getExternalAccessDetails(Map<String, String> actionDetails, long hostId, ExtensionResourceMap resourceMap, Extension extension) {
        Map<String, Object> externalDetails = new HashMap<>();
        externalDetails.put(ApiConstants.CUSTOM_ACTION_ID, actionDetails);
        Map<String, String> hostDetails = getFilteredExternalDetails(hostDetailsDao.findDetails(hostId));
        externalDetails.put(ApiConstants.HOST_ID, hostDetails);
        if (resourceMap == null) {
            return externalDetails;
        }
        Map<String, String> resourceDetails = extensionResourceMapDetailsDao.listDetailsKeyPairs(resourceMap.getId());
        externalDetails.put(ApiConstants.RESOURCE_ID, resourceDetails);
        if (extension == null) {
            return externalDetails;
        }
        Map<String, String> extensionDetails = extensionDetailsDao.listDetailsKeyPairs(resourceMap.getExtensionId());
        externalDetails.put(ApiConstants.EXTENSION_ID, extensionDetails);
        return externalDetails;
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
        return cmds;
    }
}
