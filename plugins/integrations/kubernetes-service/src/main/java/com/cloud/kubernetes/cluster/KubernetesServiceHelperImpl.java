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

import javax.inject.Inject;

import org.apache.cloudstack.acl.ControlledEntity;
import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Component;

import com.cloud.event.EventTypes;
import com.cloud.kubernetes.cluster.dao.KubernetesClusterDao;
import com.cloud.kubernetes.version.KubernetesSupportedVersion;
import com.cloud.kubernetes.version.KubernetesVersionEventTypes;
import com.cloud.utils.component.AdapterBase;

@Component
public class KubernetesServiceHelperImpl extends AdapterBase implements KubernetesServiceHelper, Configurable {

    @Inject
    private KubernetesClusterDao kubernetesClusterDao;

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
