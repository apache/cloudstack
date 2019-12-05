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
package com.cloud.kubernetescluster;

import com.cloud.server.ManagementServer;

public enum KubernetesServiceConfig {

    KubernetesServiceEnabled("Advanced", ManagementServer.class, Boolean.class, "cloud.kubernetes.service.enabled", "false", "Indicates whether Kubernetes Service plugin is enabled or not. Management server restart needed on change", null, null),
    KubernetesClusterTemplateName("Advanced", ManagementServer.class, String.class, "cloud.kubernetes.cluster.template.name", "Kubernetes-Service-Template", "Name of the template to be used for creating Kubernetes cluster nodes", null, null),
    KubernetesClusterNetworkOffering("Advanced", ManagementServer.class, String.class, "cloud.kubernetes.cluster.network.offering", "DefaultNetworkOfferingforKubernetesService", "Name of the network offering that will be used to create isolated network in which Kubernetes cluster VMs will be launched.", null, null);

    private final String _category;
    private final Class<?> _componentClass;
    private final Class<?> _type;
    private final String _name;
    private final String _defaultValue;
    private final String _description;
    private final String _range;
    private final String _scope;

    private KubernetesServiceConfig(String category, Class<?> componentClass, Class<?> type, String name, String defaultValue, String description, String range, String scope) {
        _category = category;
        _componentClass = componentClass;
        _type = type;
        _name = name;
        _defaultValue = defaultValue;
        _description = description;
        _range = range;
        _scope = scope;
    }

    public String getCategory() {
        return _category;
    }

    public String key() {
        return _name;
    }

    public String getDescription() {
        return _description;
    }

    public String getDefaultValue() {
        return _defaultValue;
    }

    public Class<?> getType() {
        return _type;
    }

    public Class<?> getComponentClass() {
        return _componentClass;
    }

    public String getScope() {
        return _scope;
    }
}
