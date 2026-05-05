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

package org.apache.cloudstack.veeam;

import java.util.List;

import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;
import org.apache.cloudstack.utils.CloudStackVersion;

import com.cloud.utils.component.PluggableService;

public interface VeeamControlService extends PluggableService, Configurable {
    String PLUGIN_NAME = "CloudStack Veeam Control Service";

    ConfigKey<Boolean> Enabled = new ConfigKey<>("Advanced", Boolean.class, "integration.veeam.control.enabled",
            "false", "Enable the Veeam Integration REST API server", false);
    ConfigKey<String> BindAddress = new ConfigKey<>("Advanced", String.class, "integration.veeam.control.bind.address",
            "", "Bind address for Veeam Integration REST API server", false,
            ConfigKey.Scope.ManagementServer);
    ConfigKey<Integer> Port = new ConfigKey<>("Advanced", Integer.class, "integration.veeam.control.port",
            "8090", "Port for Veeam Integration REST API server", false);
    ConfigKey<String> ContextPath = new ConfigKey<>("Advanced", String.class, "integration.veeam.control.context.path",
            "/ovirt-engine", "Context path for Veeam Integration REST API server", false);
    ConfigKey<String> Username = new ConfigKey<>("Secure", String.class, "integration.veeam.control.api.username",
            "veeam", "Username for Basic Auth on Veeam Integration REST API server", true);
    ConfigKey<String> Password = new ConfigKey<>("Secure", String.class, "integration.veeam.control.api.password",
            "change-me", "Password for Basic Auth on Veeam Integration REST API server", true);
    ConfigKey<String> ServiceAccountId = new ConfigKey<>("Advanced", String.class,
            "integration.veeam.control.service.account", "",
            "ID of the service account used to perform operations on resources. " +
                    "Preferably an admin-level account with permissions to access resources across the environment " +
                    "and optionally assign them to other users.",
            true);
    ConfigKey<Boolean> InstanceRestoreAssignOwner = new ConfigKey<>("Advanced", Boolean.class,
            "integration.veeam.control.instance.restore.assign.owner",
            "false", "Attempt to assign restored Instance to the owner based on OVF and network " +
            "details. If the assignment fails or set to false then the Instance will remain owned by the service " +
            "account", true);
    ConfigKey<String> AllowedClientCidrs = new ConfigKey<>("Advanced", String.class,
            "integration.veeam.control.allowed.client.cidrs",
            "", "Comma-separated list of CIDR blocks representing clients allowed to access the API. " +
                    "If empty, all clients will be allowed. Example: '192.168.1.1/24,192.168.2.100/32", true);

    ConfigKey<Boolean> DeveloperLogs = new ConfigKey<>("Developer", Boolean.class, "integration.veeam.control.developer.logs",
            "false", "Enable verbose logging for development and troubleshooting purposes. " +
            "Logs will include detailed information about API requests, responses, and internal operations.", false);

    long getCurrentManagementServerHostId();

    List<String> getAllowedClientCidrs();

    String getInstanceId();

    boolean validateCredentials(String username, String password);

    String getHmacSecret();

    static String getPackageVersion() {
        return VeeamControlService.class.getPackage().getImplementationVersion();
    }

    static CloudStackVersion getCSVersion() {
        try {
            return CloudStackVersion.parse(getPackageVersion());
        } catch (Exception e) {
            return null;
        }
    }
}
