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

package org.apache.cloudstack.logsws;

import java.util.List;

import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;

import com.cloud.utils.component.PluggableService;

public interface LogsWebSessionManager extends PluggableService, Configurable {
    int WS_PORT = 8822;
    String WS_PATH = "/logger";

    ConfigKey<Boolean> LogsWebServerEnabled = new ConfigKey<>("Advanced", Boolean.class,
            "logs.web.server.enabled", "false",
            "Indicates whether Logs Web Server plugin is enabled or not",
            false);

    ConfigKey<Integer> LogsWebServerConcurrentSessions = new ConfigKey<>("Advanced", Integer.class,
            "logs.web.server.concurrent.sessions", "1",
            "Number of concurrent sessions that can be created at a time. To allow unlimited a value of zero can used",
            true);

    ConfigKey<Integer> LogsWebServerSessionStaleCleanupInterval = new ConfigKey<>("Advanced", Integer.class,
            "logs.web.server.session.stale.cleanup.interval", "600",
            "Time(in seconds) after which a stale (not connected or disconnected) Logs Web Server session will be automatically deleted",
            false);

    ConfigKey<Integer> LogsWebServerPort = new ConfigKey<>("Advanced", Integer.class,
            "logs.web.server.port", String.valueOf(WS_PORT),
            "The port to be used for Logs Web Server",
            false,
            ConfigKey.Scope.ManagementServer);

    ConfigKey<String> LogsWebServerPath = new ConfigKey<>("Advanced", String.class,
            "logs.web.server.path", WS_PATH,
            "The path prefix to be used for Logs Web Server",
            false,
            ConfigKey.Scope.ManagementServer);

    ConfigKey<Integer> LogsWebServerSessionIdleTimeout = new ConfigKey<>("Advanced", Integer.class,
            "logs.web.server.session.idle.timeout", "60",
            "Time(in seconds) after which a Logs Web Server session will be automatically disconnected if in idle state",
            false,
            ConfigKey.Scope.ManagementServer);

    ConfigKey<String> LogsWebServerLogFile = new ConfigKey<>("Advanced", String.class,
            "logs.web.server.log.file", "/var/logs/cloudstack/management/management-server.log",
            "Log file to be used by Logs Web Server",
            true,
            ConfigKey.Scope.ManagementServer);

    ConfigKey<Integer> LogsWebServerSessionTailExistingLines = new ConfigKey<>("Advanced", Integer.class,
            "logs.web.server.session.tail.existing.lines", "512",
            "Number of existing lines to be read from the logs file on session connect",
            true,
            ConfigKey.Scope.ManagementServer);

    void startWebSocketServer();
    void stopWebSocketServer();
    List<LogsWebSessionWebSocket> getLogsWebSessionWebSockets(final LogsWebSession logsWebSession);
    boolean canCreateNewLogsWebSession();
}
