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
package org.apache.cloudstack.applicationcluster;

import com.cloud.server.ManagementServer;

public enum ApplicationClusterConfig {

    ApplicationClusterTemplateName("Advanced", ManagementServer.class, String.class, "cloud.applicationcluster.template.name", null, "name of the template used for creating the machines in the cluster", null, null),
    ApplicationClusterMasterCloudConfig("Advanced", ManagementServer.class, String.class, "cloud.applicationcluster.master.cloudconfig", null, "file location path of the cloud config used for creating a cluster master node", null, null),
    ApplicationClusterNodeCloudConfig("Advanced", ManagementServer.class, String.class, "cloud.applicationcluster.node.cloudconfig", null, "file location path of the cloud config used for creating a cluster node", null, null),
    ApplicationClusterNetworkOffering("Advanced", ManagementServer.class, String.class, "cloud.applicationcluster.network.offering", null, "Name of the network offering that will be used to create a isolated network in which the cluster VMs will be launched.", null, null);


    private final String _category;
    private final Class<?> _componentClass;
    private final Class<?> _type;
    private final String _name;
    private final String _defaultValue;
    private final String _description;
    private final String _range;
    private final String _scope;

    private ApplicationClusterConfig(String category, Class<?> componentClass, Class<?> type, String name, String defaultValue, String description, String range, String scope) {
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
