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
import java.util.Objects;

import javax.inject.Inject;

import org.apache.cloudstack.acl.ControlledEntity;
import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;

import com.cloud.event.EventTypes;
import com.cloud.kubernetes.cluster.dao.KubernetesClusterDao;
import com.cloud.kubernetes.cluster.dao.KubernetesClusterVmMapDao;
import com.cloud.kubernetes.version.KubernetesSupportedVersion;
import com.cloud.kubernetes.version.KubernetesVersionEventTypes;
import com.cloud.uservm.UserVm;
import com.cloud.utils.component.AdapterBase;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.UserVmManager;

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
        logger.error(String.format("VM ID: %s is a part of Kubernetes cluster ID: %d", userVm.getId(), vmMapVO.getClusterId()));
        KubernetesCluster kubernetesCluster = kubernetesClusterDao.findById(vmMapVO.getClusterId());
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
