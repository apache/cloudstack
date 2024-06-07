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
import com.cloud.kubernetes.cluster.dao.KubernetesClusterVmMapDao;
import com.cloud.uservm.UserVm;
import com.cloud.utils.component.AdapterBase;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.UserVmManager;

import javax.inject.Inject;

import org.apache.cloudstack.acl.ControlledEntity;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;
import org.springframework.stereotype.Component;

import java.util.Objects;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

@Component
public class KubernetesClusterHelperImpl extends AdapterBase implements KubernetesClusterHelper, Configurable {
    private static final Logger logger = LogManager.getLogger(KubernetesClusterHelperImpl.class);

    @Inject
    private KubernetesClusterDao kubernetesClusterDao;
    @Inject
    private KubernetesClusterVmMapDao kubernetesClusterVmMapDao;

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
            msg += String.format(": %s", kubernetesCluster.getName());
        }
        msg += ". Use Instance delete option from Kubernetes cluster details or scale API for " +
                "Kubernetes clusters with 'nodeids' to destroy the instance.";
        throw new CloudRuntimeException(msg);
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
