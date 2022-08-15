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
import org.apache.cloudstack.framework.config.Configurable;

public interface ConsoleAccessManager extends Manager, Configurable {


    ConfigKey<String> ConsoleProxySchema = new ConfigKey<>(ConfigKey.CATEGORY_ADVANCED, String.class,
            "consoleproxy.schema", "http",
            "The http/https schema to be used by the console proxy URLs", true);

    ConfigKey<Boolean> ConsoleProxyExtraSecurityHeaderEnabled = new ConfigKey<>(ConfigKey.CATEGORY_ADVANCED, Boolean.class,
            "consoleproxy.extra.security.header.enabled", "false",
            "Enable/disable extra security validation for console proxy using client header", true);

    ConfigKey<String> ConsoleProxyExtraSecurityHeaderName = new ConfigKey<>(ConfigKey.CATEGORY_ADVANCED, String.class,
            "consoleproxy.extra.security.header.name", "SECURITY_TOKEN",
            "A client header for extra security validation when using the console proxy", true);

    ConsoleEndpoint generateConsoleEndpoint(Long vmId, String clientSecurityToken, String clientAddress);

    boolean isSessionAllowed(String sessionUuid);

    void removeSessions(String[] sessionUuids);
}
