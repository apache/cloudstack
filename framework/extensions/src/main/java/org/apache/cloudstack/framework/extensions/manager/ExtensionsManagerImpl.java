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

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.RunCustomActionAnswer;
import com.cloud.agent.api.RunCustomActionCommand;
import com.cloud.dc.ClusterDetailsDao;
import com.cloud.dc.ClusterVO;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.extension.ExtensionCustomAction;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.host.dao.HostDetailsDao;
import com.cloud.hypervisor.ExternalProvisioner;
import com.cloud.utils.Pair;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.dao.VMInstanceDao;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.response.ExtensionCustomActionResponse;
import org.apache.cloudstack.extension.CustomActionResponse;
import org.apache.cloudstack.framework.extensions.api.CreateExtensionCmd;
import org.apache.cloudstack.framework.extensions.api.DeleteCustomActionCmd;
import org.apache.cloudstack.framework.extensions.api.DeleteExtensionCmd;
import org.apache.cloudstack.framework.extensions.api.ListCustomActionCmd;
import org.apache.cloudstack.framework.extensions.api.ListExtensionsCmd;
import org.apache.cloudstack.framework.extensions.api.AddCustomActionCmd;
import org.apache.cloudstack.framework.extensions.api.RunCustomActionCmd;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.component.PluggableService;
import org.apache.cloudstack.framework.extensions.api.RegisterExtensionCmd;
import org.apache.cloudstack.framework.extensions.dao.ExtensionCustomActionDao;
import org.apache.cloudstack.framework.extensions.dao.ExtensionCustomActionDetailsDao;
import org.apache.cloudstack.framework.extensions.dao.ExtensionResourceMapDao;
import org.apache.cloudstack.framework.extensions.dao.ExtensionResourceMapDetailsDao;
import org.apache.cloudstack.framework.extensions.dao.ExtensionDao;
import org.apache.cloudstack.framework.extensions.dao.ExtensionDetailsDao;
import org.apache.cloudstack.api.response.ExtensionResourceMapResponse;
import org.apache.cloudstack.api.response.ExtensionResponse;
import com.cloud.extension.Extension;
import org.apache.cloudstack.framework.extensions.vo.ExtensionCustomActionDetailsVO;
import org.apache.cloudstack.framework.extensions.vo.ExtensionCustomActionVO;
import org.apache.cloudstack.framework.extensions.vo.ExtensionResourceMapDetailsVO;
import org.apache.cloudstack.framework.extensions.vo.ExtensionResourceMapVO;
import org.apache.cloudstack.framework.extensions.vo.ExtensionVO;
import org.apache.cloudstack.framework.extensions.vo.ExtensionDetailsVO;
import org.apache.commons.collections.CollectionUtils;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

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
    ClusterDetailsDao clusterDetailsDao;

    @Inject
    VMInstanceDao vmInstanceDao;


    @Override
    public Extension createExtension(CreateExtensionCmd cmd) {
        String name = cmd.getName();
        String type = cmd.getType();

        ExtensionVO extensionByName = extensionDao.findByName(name);
        if (extensionByName != null) {
            throw new CloudRuntimeException("Extension by name already exists");
        }

        ExtensionVO extension = new ExtensionVO();
        extension.setName(name);
        extension.setType(type);
        String scriptPath = String.format(externalProvisioner.getScriptPath(), name);
        extension.setScript(scriptPath);
        ExtensionVO savedExtension = extensionDao.persist(extension);

        Map<String, String> externalDetails = cmd.getExternalDetails();
        List<ExtensionDetailsVO> detailsVOList = new ArrayList<>();
        if (externalDetails != null && !externalDetails.isEmpty()) {
            for (Map.Entry<String, String> entry : externalDetails.entrySet()) {
                detailsVOList.add(new ExtensionDetailsVO(savedExtension.getId(), entry.getKey(), entry.getValue()));
            }
            extensionDetailsDao.saveDetails(detailsVOList);
        }

        externalProvisioner.prepareScripts(savedExtension.getName());

        return savedExtension;
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
            Map<String, String> details = extensionDetailsDao.listDetailsKeyPairs(extension.getId());
            ExtensionResponse response = new ExtensionResponse(extension.getName(), extension.getType(), extension.getUuid(), details);
            String scriptPath = String.format(externalProvisioner.getScriptPath(), extension.getName());
            response.setScriptPath(scriptPath);

            List<ExtensionResourceMapVO> extensionResourceMapVOS = extensionResourceMapDao.listByExtensionId(extension.getId());
            List<ExtensionResourceMapResponse> resourceMapResponses = new ArrayList<>();

            for (ExtensionResourceMapVO resourceMap : extensionResourceMapVOS) {
                ClusterVO cluster = clusterDao.findById(resourceMap.getResourceId());
                ExtensionResourceMapResponse resourceResponse = new ExtensionResourceMapResponse(extension.getUuid(), cluster.getUuid(), resourceMap.getResourceType());

                Map<String, String> resourceMapDetails = extensionResourceMapDetailsDao.listDetailsKeyPairs(resourceMap.getId());
                resourceResponse.setDetails(resourceMapDetails);

                resourceMapResponses.add(resourceResponse);
            }

            response.setResources(resourceMapResponses);
            response.setObjectName(ApiConstants.EXTENSIONS);
            responses.add(response);
        }

        return responses;
    }

    @Override
    public ExtensionResponse registerExtensionWithResource(RegisterExtensionCmd cmd) {
        String resourceId = cmd.getResourceId();
        Long extensionId = cmd.getExtensionId();
        String resourceType = cmd.getResourceType();
        if ("CLUSTER".equalsIgnoreCase(resourceType)) {
            return registerExtensionWithCluster(resourceId, extensionId, resourceType, cmd.getExternalDetails());
        } else {
            throw new CloudRuntimeException("Currently only cluster can be used to register an extension of type Orchestrator");
        }
    }

    @Override
    public ExtensionResponse registerExtensionWithCluster(String resourceId, Long extensionId, String resourceType, Map<String, String> externalDetails) {
        ClusterVO cluster = clusterDao.findByUuid(resourceId);
        ExtensionResourceMapVO existing = extensionResourceMapDao.findByResourceIdAndType(cluster.getId(), resourceType);
        if (existing != null) {
            throw new CloudRuntimeException("Extension already registered with this resource");
        }

        ExtensionResourceMapVO extensionMap = new ExtensionResourceMapVO();
        extensionMap.setExtensionId(extensionId);
        extensionMap.setResourceId(cluster.getId());
        extensionMap.setResourceType(resourceType);
        ExtensionResourceMapVO savedExtensionMap = extensionResourceMapDao.persist(extensionMap);

        List<ExtensionResourceMapDetailsVO> detailsVOList = new ArrayList<>();
        if (externalDetails != null && !externalDetails.isEmpty()) {
            for (Map.Entry<String, String> entry : externalDetails.entrySet()) {
                detailsVOList.add(new ExtensionResourceMapDetailsVO(savedExtensionMap.getId(), entry.getKey(), entry.getValue()));
            }
            extensionResourceMapDetailsDao.saveDetails(detailsVOList);
        }

        ExtensionVO extension = extensionDao.findById(extensionId);
        Map<String, String> details = extensionDetailsDao.listDetailsKeyPairs(extension.getId());
        externalDetails.putAll(details);
        externalDetails.put(ApiConstants.EXTENSION_ID, String.valueOf(extensionId));
        Map<String, String> clusterDetails = clusterDetailsDao.listDetailsKeyPairs(cluster.getId());
        externalDetails.putAll(clusterDetails);
        clusterDetailsDao.persist(cluster.getId(), externalDetails);

        ExtensionResponse response = new ExtensionResponse(extension.getName(), extension.getType(), extension.getUuid(), details);
        String scriptPath = String.format(externalProvisioner.getScriptPath(), extension.getName());
        response.setScriptPath(scriptPath);

        ExtensionResourceMapVO extensionResourceMapVO = extensionResourceMapDao.findByResourceIdAndType(cluster.getId(), "cluster");
        ExtensionResourceMapResponse resourceResponse = new ExtensionResourceMapResponse(extension.getUuid(), cluster.getUuid(), "cluster");

        Map<String, String> resourceMapDetails = extensionResourceMapDetailsDao.listDetailsKeyPairs(extensionResourceMapVO.getId());
        resourceResponse.setDetails(resourceMapDetails);

        response.setResources(Collections.singletonList(resourceResponse));
        return response;
    }

    @Override
    public void unregisterExtensionWithCluster(Long clusterId, Long extensionId) {
        ClusterVO cluster = clusterDao.findById(clusterId);
        ExtensionResourceMapVO existing = extensionResourceMapDao.findByResourceIdAndType(cluster.getId(), "cluster");
        if (existing == null) {
            return;
        }

        extensionResourceMapDao.remove(existing.getId());
        extensionResourceMapDetailsDao.removeDetails(existing.getId());

        return;
    }

    @Override
    public CustomActionResponse runCustomAction(RunCustomActionCmd cmd) {
        Long customActionId =  cmd.getCustomActionId();
        Long instanceId = cmd.getInstanceId();
        Map<String, String> externalDetails = cmd.getExternalDetails();

        VMInstanceVO vmInstanceVO = vmInstanceDao.findById(instanceId);
        Long hostid;
        if (vmInstanceVO.getState().equals(VirtualMachine.State.Running)) {
            hostid = vmInstanceVO.getHostId();
        } else {
            hostid = vmInstanceVO.getLastHostId();
        }

        if (hostid == null) {
            throw new CloudRuntimeException("Unable to figure out the endpoint to run custom action");
        }

        HostVO host = hostDao.findById(hostid);
        Long clusterId = host.getClusterId();

        ExtensionResourceMapVO existing = extensionResourceMapDao.findByResourceIdAndType(clusterId, "cluster");
        if (existing == null) {
            throw new CloudRuntimeException("Extension is not registered with this resource");
        }

        ExtensionCustomActionVO customActionVO = extensionCustomActionDao.findById(customActionId);
        String action = customActionVO.getName();

        Map<String, String> customActionParameters = extensionCustomActionDetailsDao.listDetailsKeyPairs(customActionVO.getId());

        for (Map.Entry<String, String> entry : externalDetails.entrySet()) {
            String key = entry.getKey();
            if (!customActionParameters.containsKey(key)) {
                throw new IllegalArgumentException(String.format("Parameter %s in not registered with the action ", key));
            }
        }

        RunCustomActionAnswer answer;
        CustomActionResponse response;
        try {
            RunCustomActionCommand runCustomActionCommand = new RunCustomActionCommand(action, externalDetails);
            answer = (RunCustomActionAnswer) agentMgr.send(hostid, runCustomActionCommand);
            response = new CustomActionResponse();
            response.setActionName(action);
            response.setDetails(answer.getRunDetails());
            response.setResponseName("CustomActionResult");
            response.setObjectName("CustomActionResult");
        } catch (AgentUnavailableException e) {
            throw new CloudRuntimeException("Unable to run custom action");
        } catch (OperationTimedoutException e) {
            throw new CloudRuntimeException("Running custom action timed out, please try again");
        }

        return response;
    }

    @Override
    public ExtensionCustomAction addCustomAction(AddCustomActionCmd cmd) {
        String name = cmd.getName();
        String description = cmd.getDescription();
        Long extensionId = cmd.getExtensionId();
        List<String> rolesList = cmd.getRolesList();
        Map<String, String> externalDetails = cmd.getCustomActionParameters();

        ExtensionCustomActionVO customAction = extensionCustomActionDao.findByNameAndExtensionId(extensionId, name);
        if (customAction != null) {
            throw new CloudRuntimeException("Action by name already exists");
        }

        customAction = new ExtensionCustomActionVO();
        customAction.setName(name);
        customAction.setDescription(description);
        customAction.setExtensionId(extensionId);
        if (CollectionUtils.isNotEmpty(rolesList)) {
            customAction.setRolesList(rolesList.toString());
        }
        ExtensionCustomActionVO savedAction = extensionCustomActionDao.persist(customAction);

        List<ExtensionCustomActionDetailsVO> detailsVOList = new ArrayList<>();
        if (externalDetails != null && !externalDetails.isEmpty()) {
            for (Map.Entry<String, String> entry : externalDetails.entrySet()) {
                detailsVOList.add(new ExtensionCustomActionDetailsVO(savedAction.getId(), entry.getKey(), entry.getValue()));
            }
            extensionCustomActionDetailsDao.saveDetails(detailsVOList);
        }

        externalProvisioner.prepareScripts(savedAction.getName());

        return savedAction;
    }

    @Override
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
        final SearchBuilder<ExtensionCustomActionVO> sb = extensionCustomActionDao.createSearchBuilder();
        final Filter searchFilter = new Filter(ExtensionCustomActionVO.class, "id", false, cmd.getStartIndex(), cmd.getPageSizeVal());

        sb.and("id", sb.entity().getId(), SearchCriteria.Op.EQ);
        sb.and("extensionid", sb.entity().getExtensionId(), SearchCriteria.Op.EQ);
        sb.and("name", sb.entity().getName(), SearchCriteria.Op.EQ);
        sb.and("keyword", sb.entity().getName(), SearchCriteria.Op.LIKE);
        final SearchCriteria<ExtensionCustomActionVO> sc = sb.create();

        if (id != null) {
            sc.setParameters("id", id);
        }

        if (extensionId != null) {
            sc.setParameters("extensionid", extensionId);
        }

        if (name != null) {
            sc.setParameters("name", name);
        }

        if (keyword != null) {
            sc.setParameters("keyword",  "%" + keyword + "%");
        }

        final Pair<List<ExtensionCustomActionVO>, Integer> result = extensionCustomActionDao.searchAndCount(sc, searchFilter);
        List<ExtensionCustomActionResponse> responses = new ArrayList<>();
        for (ExtensionCustomActionVO customAction : result.first()) {
            Map<String, String> details = extensionCustomActionDetailsDao.listDetailsKeyPairs(customAction.getId());
            ExtensionCustomActionResponse response = new ExtensionCustomActionResponse(customAction.getUuid(), customAction.getName(), customAction.getDescription(), customAction.getRolesList());
            response.setDetails(details);
            response.setObjectName(ApiConstants.EXTENSION_CUSTOM_ACTION);
            responses.add(response);
        }

        return responses;
    }

    @Override
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
    public List<Class<?>> getCommands() {
        List<Class<?>> cmds = new ArrayList<Class<?>>();
        cmds.add(AddCustomActionCmd.class);
        cmds.add(ListCustomActionCmd.class);
        cmds.add(DeleteCustomActionCmd.class);
        cmds.add(RunCustomActionCmd.class);

        cmds.add(CreateExtensionCmd.class);
        cmds.add(RegisterExtensionCmd.class);
        cmds.add(ListExtensionsCmd.class);
        cmds.add(DeleteExtensionCmd.class);
        return cmds;
    }
}
