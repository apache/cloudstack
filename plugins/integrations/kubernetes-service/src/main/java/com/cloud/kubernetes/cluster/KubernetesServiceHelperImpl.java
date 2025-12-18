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

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import javax.inject.Inject;

import com.cloud.exception.InvalidParameterValueException;
import com.cloud.offering.ServiceOffering;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.vm.VmDetailConstants;
import org.apache.cloudstack.acl.ControlledEntity;
import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;

import com.cloud.event.EventTypes;
import com.cloud.kubernetes.cluster.dao.KubernetesClusterDao;
import com.cloud.kubernetes.cluster.dao.KubernetesClusterVmMapDao;
import com.cloud.kubernetes.version.KubernetesSupportedVersion;
import com.cloud.kubernetes.version.KubernetesVersionEventTypes;
import com.cloud.user.Account;
import com.cloud.uservm.UserVm;
import com.cloud.utils.component.AdapterBase;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.UserVmManager;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Component;

@Component
public class KubernetesServiceHelperImpl extends AdapterBase implements KubernetesServiceHelper, Configurable {
    private static final Logger logger = LogManager.getLogger(KubernetesServiceHelperImpl.class);

    @Inject
    private KubernetesClusterDao kubernetesClusterDao;
    @Inject
    private KubernetesClusterVmMapDao kubernetesClusterVmMapDao;
    @Inject
    protected ServiceOfferingDao serviceOfferingDao;
    @Inject
    protected VMTemplateDao vmTemplateDao;
    @Inject
    KubernetesClusterService kubernetesClusterService;

    protected void setEventTypeEntityDetails(Class<?> eventTypeDefinedClass, Class<?> entityClass) {
        Field[] declaredFields = eventTypeDefinedClass.getDeclaredFields();
        for (Field field : declaredFields) {
            int modifiers = field.getModifiers();
            if (!Modifier.isPublic(modifiers) || !Modifier.isStatic(modifiers)) {
                continue;
            }
            try {
                Object value = field.get(null);
                if (ObjectUtils.allNotNull(value, value.toString())) {
                    EventTypes.addEntityEventDetail(value.toString(), entityClass);
                }
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
    }

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
    public void checkVmCanBeDestroyed(UserVm userVm) {
        if (!UserVmManager.CKS_NODE.equals(userVm.getUserVmType())) {
            return;
        }
        KubernetesClusterVmMapVO vmMapVO = kubernetesClusterVmMapDao.findByVmId(userVm.getId());
        if (vmMapVO == null) {
            return;
        }
        KubernetesCluster kubernetesCluster = kubernetesClusterDao.findById(vmMapVO.getClusterId());
        logger.error("VM {} is a part of Kubernetes cluster {} with ID: {}", userVm, kubernetesCluster, vmMapVO.getClusterId());
        String msg = "Instance is a part of a Kubernetes cluster";
        if (kubernetesCluster != null) {
            if (KubernetesCluster.ClusterType.ExternalManaged.equals(kubernetesCluster.getClusterType())) {
                return;
            }
            msg += String.format(": %s", kubernetesCluster.getName());
        }
        msg += ". Use Instance delete option from Kubernetes cluster details or scale API for " +
                "Kubernetes clusters with 'nodeids' to destroy the instance.";
        throw new CloudRuntimeException(msg);
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

    protected void checkNodeTypeOfferingEntryCompleteness(String nodeTypeStr, String serviceOfferingUuid) {
        if (StringUtils.isAnyEmpty(nodeTypeStr, serviceOfferingUuid)) {
            String error = String.format("Incomplete Node Type to Service Offering ID mapping: '%s' -> '%s'", nodeTypeStr, serviceOfferingUuid);
            logger.error(error);
            throw new InvalidParameterValueException(error);
        }
    }

    protected void checkNodeTypeOfferingEntryValues(String nodeTypeStr, ServiceOffering serviceOffering, String serviceOfferingUuid) {
        if (!isValidNodeType(nodeTypeStr)) {
            String error = String.format("The provided value '%s' for Node Type is invalid", nodeTypeStr);
            logger.error(error);
            throw new InvalidParameterValueException(String.format(error));
        }
        if (serviceOffering == null) {
            String error = String.format("Cannot find a service offering with ID %s", serviceOfferingUuid);
            logger.error(error);
            throw new InvalidParameterValueException(error);
        }
    }

    protected void addNodeTypeOfferingEntry(String nodeTypeStr, String serviceOfferingUuid, ServiceOffering serviceOffering, Map<String, Long> mapping) {
        if (logger.isDebugEnabled()) {
            logger.debug("Node Type: '{}' should use Service Offering ID: '{}'", nodeTypeStr, serviceOfferingUuid);
        }
        KubernetesClusterNodeType nodeType = KubernetesClusterNodeType.valueOf(nodeTypeStr.toUpperCase());
        mapping.put(nodeType.name(), serviceOffering.getId());
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

    protected void checkNodeTypeTemplateEntryCompleteness(String nodeTypeStr, String templateUuid) {
        if (StringUtils.isAnyEmpty(nodeTypeStr, templateUuid)) {
            String error = String.format("Incomplete Node Type to template ID mapping: '%s' -> '%s'", nodeTypeStr, templateUuid);
            logger.error(error);
            throw new InvalidParameterValueException(error);
        }
    }

    protected void checkNodeTypeTemplateEntryValues(String nodeTypeStr, VMTemplateVO template, String templateUuid) {
        if (!isValidNodeType(nodeTypeStr)) {
            String error = String.format("The provided value '%s' for Node Type is invalid", nodeTypeStr);
            logger.error(error);
            throw new InvalidParameterValueException(String.format(error));
        }
        if (template == null) {
            String error = String.format("Cannot find a template with ID %s", templateUuid);
            logger.error(error);
            throw new InvalidParameterValueException(error);
        }
    }

    protected void addNodeTypeTemplateEntry(String nodeTypeStr, String templateUuid, VMTemplateVO template, Map<String, Long> mapping) {
        if (logger.isDebugEnabled()) {
            logger.debug(String.format("Node Type: '%s' should use template ID: '%s'", nodeTypeStr, templateUuid));
        }
        KubernetesClusterNodeType nodeType = KubernetesClusterNodeType.valueOf(nodeTypeStr.toUpperCase());
        mapping.put(nodeType.name(), template.getId());
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
    public Map<String, Long> getTemplateNodeTypeMap(Map<String, Map<String, String>> templateNodeTypeMap) {
        Map<String, Long> mapping = new HashMap<>();
        if (MapUtils.isNotEmpty(templateNodeTypeMap)) {
            for (Map<String, String> entry : templateNodeTypeMap.values()) {
                processNodeTypeTemplateEntryAndAddToMappingIfValid(entry, mapping);
            }
        }
        return mapping;
    }

    public void cleanupForAccount(Account account) {
        kubernetesClusterService.cleanupForAccount(account);
    }

    @Override
    public String getConfigComponentName() {
        return KubernetesServiceHelper.class.getSimpleName();
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey<?>[]{};
    }

    @Override
    public boolean start() {
        setEventTypeEntityDetails(KubernetesClusterEventTypes.class, KubernetesCluster.class);
        setEventTypeEntityDetails(KubernetesVersionEventTypes.class, KubernetesSupportedVersion.class);
        ApiCommandResourceType.setClassMapping(ApiCommandResourceType.KubernetesCluster, KubernetesCluster.class);
        ApiCommandResourceType.setClassMapping(ApiCommandResourceType.KubernetesSupportedVersion,
                KubernetesSupportedVersion.class);
        return super.start();
    }
}
