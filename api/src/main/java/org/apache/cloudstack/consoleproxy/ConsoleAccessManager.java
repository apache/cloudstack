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
package org.apache.cloudstack.consoleproxy;

import com.cloud.utils.component.Manager;
import org.apache.cloudstack.api.command.user.consoleproxy.ConsoleEndpoint;
import org.apache.cloudstack.framework.config.ConfigKey;

public interface ConsoleAccessManager extends Manager {

    ConfigKey<Boolean> ConsoleSessionCleanupEnabled = new ConfigKey<>("Advanced", Boolean.class,
            "console.session.cleanup.enabled",
            "true",
            "Enables/disables the console session cleanup thread.",
            true,
            ConfigKey.Scope.Global);

    ConfigKey<Integer> ConsoleSessionCleanupRetentionDays = new ConfigKey<>("Advanced", Integer.class,
            "console.session.cleanup.retention.days",
            "7",
            "Determines the days to keep removed console session records before expunging them",
            true,
            ConfigKey.Scope.Global);

    ConfigKey<Integer> ConsoleSessionCleanupInterval = new ConfigKey<>("Advanced", Integer.class,
            "console.session.cleanup.interval",
            "180",
            "Determines how long (in hours) to wait before actually expunging destroyed console session records",
            true,
            ConfigKey.Scope.Global);

    ConsoleEndpoint generateConsoleEndpoint(Long vmId, String extraSecurityToken, String clientAddress);

    boolean isSessionAllowed(String sessionUuid);

    void removeSessions(String[] sessionUuids);

    void removeSession(String sessionUuid);
}
