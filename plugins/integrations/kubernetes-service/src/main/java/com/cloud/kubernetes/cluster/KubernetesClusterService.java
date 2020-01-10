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

import org.apache.cloudstack.api.command.user.kubernetes.cluster.CreateKubernetesClusterCmd;
import org.apache.cloudstack.api.command.user.kubernetes.cluster.GetKubernetesClusterConfigCmd;
import org.apache.cloudstack.api.command.user.kubernetes.cluster.ListKubernetesClustersCmd;
import org.apache.cloudstack.api.command.user.kubernetes.cluster.ScaleKubernetesClusterCmd;
import org.apache.cloudstack.api.command.user.kubernetes.cluster.UpgradeKubernetesClusterCmd;
import org.apache.cloudstack.api.response.KubernetesClusterConfigResponse;
import org.apache.cloudstack.api.response.KubernetesClusterResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;

import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.ManagementServerException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.utils.component.PluggableService;
import com.cloud.utils.exception.CloudRuntimeException;

public interface KubernetesClusterService extends PluggableService, Configurable {
    static final String MIN_KUBERNETES_VERSION_HA_SUPPORT = "1.16.0";

    static final ConfigKey<Boolean> KubernetesServiceEnabled = new ConfigKey<Boolean>("Advanced", Boolean.class,
            "cloud.kubernetes.service.enabled",
            "false",
            "Indicates whether Kubernetes Service plugin is enabled or not. Management server restart needed on change",
            false);
    static final ConfigKey<String> KubernetesClusterTemplateName = new ConfigKey<String>("Advanced", String.class,
            "cloud.kubernetes.cluster.template.name",
            "Kubernetes-Service-Template",
            "Name of the template to be used for creating Kubernetes cluster nodes",
            true);
    static final ConfigKey<String> KubernetesClusterNetworkOffering = new ConfigKey<String>("Advanced", String.class,
            "cloud.kubernetes.cluster.network.offering",
            "DefaultNetworkOfferingforKubernetesService",
            "Name of the network offering that will be used to create isolated network in which Kubernetes cluster VMs will be launched",
            false);

    KubernetesCluster findById(final Long id);

    KubernetesCluster createKubernetesCluster(CreateKubernetesClusterCmd cmd) throws InsufficientCapacityException,
            ManagementServerException, CloudRuntimeException;

    boolean startKubernetesCluster(long kubernetesClusterId, boolean onCreate) throws ManagementServerException,
            ResourceUnavailableException, InsufficientCapacityException, CloudRuntimeException;

    boolean stopKubernetesCluster(long kubernetesClusterId) throws CloudRuntimeException;

    boolean deleteKubernetesCluster(Long kubernetesClusterId) throws ManagementServerException;

    ListResponse<KubernetesClusterResponse> listKubernetesClusters(ListKubernetesClustersCmd cmd);

    KubernetesClusterConfigResponse getKubernetesClusterConfig(GetKubernetesClusterConfigCmd cmd);

    KubernetesClusterResponse createKubernetesClusterResponse(long kubernetesClusterId);

    boolean scaleKubernetesCluster(ScaleKubernetesClusterCmd cmd) throws CloudRuntimeException;

    boolean upgradeKubernetesCluster(UpgradeKubernetesClusterCmd cmd) throws CloudRuntimeException;
}
