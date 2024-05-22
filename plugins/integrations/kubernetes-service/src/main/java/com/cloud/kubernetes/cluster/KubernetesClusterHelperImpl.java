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
package com.cloud.kubernetes.cluster;

import com.cloud.exception.InvalidParameterValueException;
import com.cloud.kubernetes.cluster.dao.KubernetesClusterDao;
import com.cloud.kubernetes.cluster.dao.KubernetesClusterVmMapDao;
import com.cloud.offering.ServiceOffering;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.utils.component.AdapterBase;
import com.cloud.vm.VmDetailConstants;
import org.apache.cloudstack.acl.ControlledEntity;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Component
public class KubernetesClusterHelperImpl extends AdapterBase implements KubernetesClusterHelper, Configurable {

    public static final Logger LOGGER = LogManager.getLogger(KubernetesClusterHelperImpl.class.getName());

    @Inject
    private KubernetesClusterDao kubernetesClusterDao;
    @Inject
    private KubernetesClusterVmMapDao kubernetesClusterVmMapDao;
    @Inject
    protected ServiceOfferingDao serviceOfferingDao;
    @Inject
    protected VMTemplateDao vmTemplateDao;

    @Override
    public ControlledEntity findByUuid(String uuid) {
        return kubernetesClusterDao.findByUuid(uuid);
    }

    @Override
    public ControlledEntity findByVmId(long vmId) {
        KubernetesClusterVmMapVO clusterVmMapVO = kubernetesClusterVmMapDao.getClusterMapFromVmId(vmId);
        if (Objects.isNull(clusterVmMapVO)) {
            return null;
        }
        return kubernetesClusterDao.findById(clusterVmMapVO.getClusterId());
    }

    @Override
    public boolean isValidNodeType(String nodeType) {
        if (StringUtils.isBlank(nodeType)) {
            return false;
        }
        try {
            KubernetesClusterNodeType.valueOf(nodeType.toUpperCase());
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    protected void checkNodeTypeOfferingEntryCompleteness(String nodeTypeStr, String serviceOfferingUuid) {
        if (StringUtils.isAnyEmpty(nodeTypeStr, serviceOfferingUuid)) {
            String error = String.format("Incomplete Node Type to Service Offering ID mapping: '%s' -> '%s'", nodeTypeStr, serviceOfferingUuid);
            LOGGER.error(error);
            throw new InvalidParameterValueException(error);
        }
    }

    protected void checkNodeTypeTemplateEntryCompleteness(String nodeTypeStr, String templateUuid) {
        if (StringUtils.isAnyEmpty(nodeTypeStr, templateUuid)) {
            String error = String.format("Incomplete Node Type to template ID mapping: '%s' -> '%s'", nodeTypeStr, templateUuid);
            LOGGER.error(error);
            throw new InvalidParameterValueException(error);
        }
    }

    protected void checkNodeTypeOfferingEntryValues(String nodeTypeStr, ServiceOffering serviceOffering, String serviceOfferingUuid) {
        if (!isValidNodeType(nodeTypeStr)) {
            String error = String.format("The provided value '%s' for Node Type is invalid", nodeTypeStr);
            LOGGER.error(error);
            throw new InvalidParameterValueException(String.format(error));
        }
        if (serviceOffering == null) {
            String error = String.format("Cannot find a service offering with ID %s", serviceOfferingUuid);
            LOGGER.error(error);
            throw new InvalidParameterValueException(error);
        }
    }

    protected void checkNodeTypeTemplateEntryValues(String nodeTypeStr, VMTemplateVO template, String templateUuid) {
        if (!isValidNodeType(nodeTypeStr)) {
            String error = String.format("The provided value '%s' for Node Type is invalid", nodeTypeStr);
            LOGGER.error(error);
            throw new InvalidParameterValueException(String.format(error));
        }
        if (template == null) {
            String error = String.format("Cannot find a template with ID %s", templateUuid);
            LOGGER.error(error);
            throw new InvalidParameterValueException(error);
        }
    }

    protected void addNodeTypeOfferingEntry(String nodeTypeStr, String serviceOfferingUuid, ServiceOffering serviceOffering, Map<String, Long> mapping) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(String.format("Node Type: '%s' should use Service Offering ID: '%s'", nodeTypeStr, serviceOfferingUuid));
        }
        KubernetesClusterNodeType nodeType = KubernetesClusterNodeType.valueOf(nodeTypeStr.toUpperCase());
        mapping.put(nodeType.name(), serviceOffering.getId());
    }

    protected void addNodeTypeTemplateEntry(String nodeTypeStr, String templateUuid, VMTemplateVO template, Map<String, Long> mapping) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(String.format("Node Type: '%s' should use template ID: '%s'", nodeTypeStr, templateUuid));
        }
        KubernetesClusterNodeType nodeType = KubernetesClusterNodeType.valueOf(nodeTypeStr.toUpperCase());
        mapping.put(nodeType.name(), template.getId());
    }

    protected void processNodeTypeOfferingEntryAndAddToMappingIfValid(Map<String, String> entry, Map<String, Long> mapping) {
        if (MapUtils.isEmpty(entry)) {
            return;
        }
        String nodeTypeStr = entry.get(VmDetailConstants.CKS_NODE_TYPE);
        String serviceOfferingUuid = entry.get(VmDetailConstants.OFFERING);
        checkNodeTypeOfferingEntryCompleteness(nodeTypeStr, serviceOfferingUuid);

        ServiceOffering serviceOffering = serviceOfferingDao.findByUuid(serviceOfferingUuid);
        checkNodeTypeOfferingEntryValues(nodeTypeStr, serviceOffering, serviceOfferingUuid);

        addNodeTypeOfferingEntry(nodeTypeStr, serviceOfferingUuid, serviceOffering, mapping);
    }

    protected void processNodeTypeTemplateEntryAndAddToMappingIfValid(Map<String, String> entry, Map<String, Long> mapping) {
        if (MapUtils.isEmpty(entry)) {
            return;
        }
        String nodeTypeStr = entry.get(VmDetailConstants.CKS_NODE_TYPE);
        String templateUuid = entry.get(VmDetailConstants.TEMPLATE);
        checkNodeTypeTemplateEntryCompleteness(nodeTypeStr, templateUuid);

        VMTemplateVO template = vmTemplateDao.findByUuid(templateUuid);
        checkNodeTypeTemplateEntryValues(nodeTypeStr, template, templateUuid);

        addNodeTypeTemplateEntry(nodeTypeStr, templateUuid, template, mapping);
    }

    @Override
    public Map<String, Long> getServiceOfferingNodeTypeMap(Map<String, Map<String, String>> serviceOfferingNodeTypeMap) {
        Map<String, Long> mapping = new HashMap<>();
        if (MapUtils.isNotEmpty(serviceOfferingNodeTypeMap)) {
            for (Map<String, String> entry : serviceOfferingNodeTypeMap.values()) {
                processNodeTypeOfferingEntryAndAddToMappingIfValid(entry, mapping);
            }
        }
        return mapping;
    }

    @Override
    public Map<String, Long> getTemplateNodeTypeMap(Map<String, Map<String, String>> templateNodeTypeMap) {
        Map<String, Long> mapping = new HashMap<>();
        if (MapUtils.isNotEmpty(templateNodeTypeMap)) {
            for (Map<String, String> entry : templateNodeTypeMap.values()) {
                processNodeTypeTemplateEntryAndAddToMappingIfValid(entry, mapping);
            }
        }
        return mapping;
    }

    @Override
    public String getConfigComponentName() {
        return KubernetesClusterHelper.class.getSimpleName();
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey<?>[]{};
    }
}
