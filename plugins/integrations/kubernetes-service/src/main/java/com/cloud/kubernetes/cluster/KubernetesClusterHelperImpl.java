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

import com.cloud.kubernetes.cluster.dao.KubernetesClusterDao;
import com.cloud.utils.component.AdapterBase;
import org.apache.cloudstack.acl.ControlledEntity;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

@Component
public class KubernetesClusterHelperImpl extends AdapterBase implements KubernetesClusterHelper, Configurable {

    @Inject
    private KubernetesClusterDao kubernetesClusterDao;

    @Override
    public ControlledEntity findByUuid(String uuid) {
        return kubernetesClusterDao.findByUuid(uuid);
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
