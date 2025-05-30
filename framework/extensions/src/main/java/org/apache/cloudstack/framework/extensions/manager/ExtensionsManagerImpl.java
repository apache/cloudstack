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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.cloudstack.api.ApiCommandResourceType;
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
import org.apache.commons.lang3.StringUtils;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.RunCustomActionAnswer;
import com.cloud.agent.api.RunCustomActionCommand;
import com.cloud.dc.ClusterVO;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.event.ActionEvent;
import com.cloud.event.EventTypes;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.host.dao.HostDetailsDao;
import com.cloud.hypervisor.ExternalProvisioner;
import com.cloud.org.Cluster;
import com.cloud.utils.Pair;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.component.PluggableService;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallbackWithException;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
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

    private static final List<String> CUSTOM_ACTION_VALID_RESOURCE_TYPES = Arrays.asList(
            ApiCommandResourceType.VirtualMachine.name(),
            ApiCommandResourceType.Host.name(),
            ApiCommandResourceType.Cluster.name());

    protected String getExternalDetailKey(String key) {
        if (key.startsWith(VmDetailConstants.EXTERNAL_DETAIL_PREFIX)) {
            return key;
        }
        return VmDetailConstants.EXTERNAL_DETAIL_PREFIX + key;
    }

    protected Map<String, String> convertExternalDetailsMap(Map<String, String> details) {
        Map<String, String> externalDetails = new HashMap<>();
        if (MapUtils.isEmpty(details)) {
            return externalDetails;
        }
        for (Map.Entry<String, String> entry : details.entrySet()) {
            externalDetails.put(getExternalDetailKey(entry.getKey()), entry.getValue());
        }
        return externalDetails;
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_EXTENSION_CREATE, eventDescription = "creating extension")
    public Extension createExtension(CreateExtensionCmd cmd) {
        final String name = cmd.getName();
        final String description = cmd.getDescription();
        final String type = cmd.getType();

        ExtensionVO extensionByName = extensionDao.findByName(name);
        if (extensionByName != null) {
            throw new CloudRuntimeException("Extension by name already exists");
        }
        String scriptPath = externalProvisioner.getExtensionScriptPath(name);
        return Transaction.execute((TransactionCallbackWithException<Extension, CloudRuntimeException>) status -> {
            ExtensionVO extension = new ExtensionVO(name, description, type, scriptPath);
            extension = extensionDao.persist(extension);

            Map<String, String> details = cmd.getDetails();
            List<ExtensionDetailsVO> detailsVOList = new ArrayList<>();
            if (MapUtils.isNotEmpty(details)) {
                for (Map.Entry<String, String> entry : details.entrySet()) {
                    detailsVOList.add(new ExtensionDetailsVO(extension.getId(), entry.getKey(), entry.getValue()));
                }
                extensionDetailsDao.saveDetails(detailsVOList);
            }
            externalProvisioner.prepareScripts(extension.getName());
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
            if (MapUtils.isNotEmpty(details)) {
                List<ExtensionDetailsVO> detailsVOList = new ArrayList<>();
                extensionDetailsDao.removeDetails(extensionVO.getId());
                for (Map.Entry<String, String> entry : details.entrySet()) {
                    detailsVOList.add(new ExtensionDetailsVO(extensionVO.getId(), entry.getKey(), entry.getValue()));
                }
                extensionDetailsDao.saveDetails(detailsVOList);
            }
            return extensionVO;
        });
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_EXTENSION_DELETE, eventDescription = "deleting extension")
    public boolean deleteExtension(DeleteExtensionCmd cmd) {
        Long extensionId = cmd.getExtensionId();
        ExtensionVO extension = extensionDao.findById(extensionId);
        if (extension == null) {
            throw new InvalidParameterValueException("Unable to find the extension with the specified id");
        }

        List<ExtensionResourceMapVO> registeredResources = extensionResourceMapDao.listByExtensionId(extensionId);
        if (CollectionUtils.isNotEmpty(registeredResources)) {
            throw new CloudRuntimeException("There are resources registered with this extension, unregister the extension from them");
        }

        extensionDao.remove(extensionId);

        return true;
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_EXTENSION_RESOURCE_REGISTER, eventDescription = "registering extension resource")
    public Extension registerExtensionWithResource(RegisterExtensionCmd cmd) {
        String resourceId = cmd.getResourceId();
        Long extensionId = cmd.getExtensionId();
        String resourceType = cmd.getResourceType();
        if (!ExtensionResourceMap.ResourceType.Cluster.name().equalsIgnoreCase(resourceType)) {
            throw new InvalidParameterValueException("Currently only cluster can be used to register an extension of type Orchestrator");
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
        final ExtensionResourceMap.ResourceType resourceType = ExtensionResourceMap.ResourceType.Cluster;
        ExtensionResourceMapVO existing =
                extensionResourceMapDao.findByResourceIdAndType(cluster.getId(), resourceType);
        if (existing != null) {
            throw new CloudRuntimeException("Extension already registered with this resource");
        }
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
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_EXTENSION_RESOURCE_UNREGISTER, eventDescription = "unregistering extension resource")
    public Extension unregisterExtensionWithResource(UnregisterExtensionCmd cmd) {
        final String resourceId = cmd.getResourceId();
        final Long extensionId = cmd.getExtensionId();
        final String resourceType = cmd.getResourceType();
        if (ExtensionResourceMap.ResourceType.Cluster.name().equalsIgnoreCase(resourceType)) {
            unregisterExtensionWithCluster(resourceId, extensionId);
        } else {
            throw new CloudRuntimeException("Currently only cluster can be used to register an extension of type Orchestrator");
        }
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
        ExtensionVO extensionVO;
        if (extension instanceof ExtensionVO) {
            extensionVO = (ExtensionVO) extension;
        } else {
            extensionVO = extensionDao.findById(extension.getId());
        }
        ExtensionResponse response = new ExtensionResponse(extensionVO.getUuid(), extensionVO.getName(), extensionVO.getDescription(), extensionVO.getType());
        response.setCreated(extensionVO.getCreated());
        String scriptPath = externalProvisioner.getExtensionScriptPath(extensionVO.getName());
        response.setScriptPath(scriptPath);
        if (viewDetails.contains(ApiConstants.ExtensionDetails.all) ||
                viewDetails.contains(ApiConstants.ExtensionDetails.resource)) {
            List<ExtensionResourceResponse> resourcesResponse = new ArrayList<>();
            List<ExtensionResourceMapVO> extensionResourceMapVOs =
                    extensionResourceMapDao.listByExtensionId(extensionVO.getId());
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
            Map<String, String> extensionDetails = extensionDetailsDao.listDetailsKeyPairs(extensionVO.getId());
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
            String name = entry.get("name");
            String type = entry.get("type");
            String required = entry.get("required");
            if (StringUtils.isBlank(name)) {
                throw new InvalidParameterValueException("Invalid parameter specified with empty name");
            }
            ExtensionCustomAction.Parameter parameter = new ExtensionCustomAction.Parameter(name,
                    ExtensionCustomAction.Parameter.Type.fromString(type), Boolean.getBoolean(required));
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
        String resourceType = cmd.getResourceType();
        List<String> rolesList = cmd.getRolesList();
        final boolean enabled = cmd.isEnabled();
        Map parametersMap = cmd.getParametersMap();
        Map<String, String> details = cmd.getDetails();

        ExtensionCustomActionVO existingCustomAction = extensionCustomActionDao.findByNameAndExtensionId(extensionId, name);
        if (existingCustomAction != null) {
            throw new CloudRuntimeException("Action by name already exists");
        }
        List<ExtensionCustomAction.Parameter> parameters = getParametersListFromMap(name, parametersMap);

        final ExtensionCustomActionVO customAction =
                new ExtensionCustomActionVO(name, description, extensionId, enabled);
        if (StringUtils.isNotBlank(resourceType)) {
            if (!CUSTOM_ACTION_VALID_RESOURCE_TYPES.contains(resourceType)) {
                throw new InvalidParameterValueException(String.format("Invalid %s specified. Valid options are: %s",
                        ApiConstants.RESOURCE_TYPE,
                        StringUtils.join(CUSTOM_ACTION_VALID_RESOURCE_TYPES, ", ")));
            }
            customAction.setResourceType(resourceType);
        }
        if (CollectionUtils.isNotEmpty(rolesList)) {
            customAction.setRolesList(rolesList.toString());
        }
        return Transaction.execute((TransactionCallbackWithException<ExtensionCustomActionVO, CloudRuntimeException>) status -> {
            ExtensionCustomActionVO savedAction = extensionCustomActionDao.persist(customAction);
            List<ExtensionCustomActionDetailsVO> detailsVOList = new ArrayList<>();
            detailsVOList.add(new ExtensionCustomActionDetailsVO(
                    savedAction.getId(),
                    ApiConstants.PARAMETERS,
                    ExtensionCustomAction.Parameter.toJsonFromList(parameters),
                    false
            ));
            if (MapUtils.isNotEmpty(details)) {
                for (Map.Entry<String, String> entry : details.entrySet()) {
                    detailsVOList.add(new ExtensionCustomActionDetailsVO(savedAction.getId(), entry.getKey(), entry.getValue()));
                }
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
        Long customActionId = cmd.getCustomActionId();
        ExtensionCustomActionVO customActionVO = extensionCustomActionDao.findById(customActionId);
        if (customActionVO == null) {
            throw new InvalidParameterValueException("Unable to find the custom action with the specified id");
        }

        extensionCustomActionDetailsDao.removeDetails(customActionId);

        extensionCustomActionDao.remove(customActionId);

        return true;
    }

    @Override
    public List<ExtensionCustomActionResponse> listCustomActions(ListCustomActionCmd cmd) {
        Long id = cmd.getId();
        String name = cmd.getName();
        Long extensionId = cmd.getExtensionId();
        String keyword = cmd.getKeyword();
        final String resourceType = cmd.getResourceType();
        final Boolean enabled = cmd.isEnabled();
        final SearchBuilder<ExtensionCustomActionVO> sb = extensionCustomActionDao.createSearchBuilder();
        final Filter searchFilter = new Filter(ExtensionCustomActionVO.class, "id", false, cmd.getStartIndex(), cmd.getPageSizeVal());

        sb.and("id", sb.entity().getId(), SearchCriteria.Op.EQ);
        sb.and("extensionid", sb.entity().getExtensionId(), SearchCriteria.Op.EQ);
        sb.and("name", sb.entity().getName(), SearchCriteria.Op.EQ);
        sb.and("keyword", sb.entity().getName(), SearchCriteria.Op.LIKE);
        sb.and("enabled", sb.entity().isEnabled(), SearchCriteria.Op.EQ);
        if (StringUtils.isNotBlank(resourceType)) {
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
        if (StringUtils.isNotBlank(resourceType)) {
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
        String resourceType = cmd.getResourceType();
        List<String> rolesList = cmd.getRolesList();
        Boolean enabled = cmd.isEnabled();
        Map parametersMap = cmd.getParametersMap();
        Boolean cleanupParameters = cmd.getCleanupParameters();
        Map<String, String> details = cmd.getDetails();
        Boolean cleanupDetails = cmd.getCleanupDetails();

        ExtensionCustomActionVO customAction = extensionCustomActionDao.findById(id);
        if (customAction == null) {
            throw new CloudRuntimeException("Action not found");
        }

        boolean needUpdate = false;
        if (StringUtils.isNotBlank(description)) {
            customAction.setDescription(description);
            needUpdate = true;
        }
        if (resourceType != null) {
            if (StringUtils.isNotBlank(resourceType) && !CUSTOM_ACTION_VALID_RESOURCE_TYPES.contains(resourceType)) {
                throw new InvalidParameterValueException(String.format("Invalid %s specified. Valid options are: %s",
                        ApiConstants.RESOURCE_TYPE,
                        StringUtils.join(CUSTOM_ACTION_VALID_RESOURCE_TYPES, ", ")));
            }
            customAction.setResourceType(StringUtils.isNotBlank(resourceType) ? resourceType : null);
            needUpdate = true;
        }
        if (CollectionUtils.isNotEmpty(rolesList)) {
            customAction.setRolesList(rolesList.toString());
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
        if (needUpdate) {
            boolean result = extensionCustomActionDao.update(id, customAction);
            if (!result) {
                throw new CloudRuntimeException(String.format("Failed to update custom action: %s",
                        customAction.getName()));
            }
        }
        List<ExtensionCustomActionDetailsVO> detailsVOList = new ArrayList<>();
        if (Boolean.TRUE.equals(cleanupParameters) || CollectionUtils.isNotEmpty(parameters)) {
            extensionCustomActionDetailsDao.removeDetail(customAction.getId(), ApiConstants.PARAMETERS);
            if (CollectionUtils.isNotEmpty(parameters)) {
                detailsVOList.add(new ExtensionCustomActionDetailsVO(
                        customAction.getId(),
                        ApiConstants.PARAMETERS,
                        ExtensionCustomAction.Parameter.toJsonFromList(parameters),
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
    }

    @Override
    public CustomActionResultResponse runCustomAction(RunCustomActionCmd cmd) {
        Long customActionId =  cmd.getCustomActionId();
        Long instanceId = cmd.getInstanceId();
        Map<String, String> parameters = cmd.getParameters();

        VMInstanceVO vmInstanceVO = vmInstanceDao.findById(instanceId);
        Long hostId;
        if (vmInstanceVO.getState().equals(VirtualMachine.State.Running)) {
            hostId = vmInstanceVO.getHostId();
        } else {
            hostId = vmInstanceVO.getLastHostId();
        }
        if (hostId == null) {
            throw new CloudRuntimeException("Unable to figure out the endpoint to run custom action");
        }

        HostVO host = hostDao.findById(hostId);
        Long clusterId = host.getClusterId();

        ExtensionResourceMapVO existing = extensionResourceMapDao.findByResourceIdAndType(clusterId,
                ExtensionResourceMap.ResourceType.Cluster);
        if (existing == null) {
            throw new CloudRuntimeException("Extension is not registered with this resource");
        }

        ExtensionCustomActionVO customActionVO = extensionCustomActionDao.findById(customActionId);
        String action = customActionVO.getName();

        Map<String, String> customActionParameters = extensionCustomActionDetailsDao.listDetailsKeyPairs(customActionVO.getId());

        for (Map.Entry<String, String> entry : parameters.entrySet()) {
            String key = entry.getKey();
            if (!customActionParameters.containsKey(key)) {
                throw new IllegalArgumentException(String.format("Parameter %s in not registered with the action ", key));
            }
        }

        RunCustomActionAnswer answer;
        CustomActionResultResponse response = new CustomActionResultResponse();
        response.setId(customActionVO.getUuid());
        response.setName(action);
        response.setObjectName("CustomActionResult");
        Map<String, String> details = new HashMap<>();
        details.put(ApiConstants.SUCCESS, String.valueOf(false));
        try {
            RunCustomActionCommand runCustomActionCommand = new RunCustomActionCommand(action, parameters);
            answer = (RunCustomActionAnswer) agentMgr.send(hostId, runCustomActionCommand);
            details = answer.getRunDetails();
        } catch (AgentUnavailableException e) {
            String msg = "Unable to run custom action";
            logger.error("{} due to {}", msg, e.getMessage(), e);
            details.put(ApiConstants.RESULT1, msg);
        } catch (OperationTimedoutException e) {
            String msg = "Running custom action timed out, please try again";
            logger.error(msg, e);
            details.put(ApiConstants.RESULT1, msg);
        }
        response.setDetails(details);

        return response;
    }

    @Override
    public ExtensionCustomActionResponse createCustomActionResponse(ExtensionCustomAction customAction) {
        ExtensionCustomActionResponse response = new ExtensionCustomActionResponse( customAction.getUuid(),
                customAction.getName(), customAction.getDescription(), customAction.getRolesList());
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
                                    p.getType().name(), p.isRequired()))
                            .collect(Collectors.toSet());
                    response.setParameters(paramResponses);
                });
        Map<String, String> details =
                extensionCustomActionDetailsDao.listDetailsKeyPairs(customAction.getId(), true);
        response.setDetails(details);
        response.setObjectName(ApiConstants.EXTENSION_CUSTOM_ACTION);
        return response;
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
