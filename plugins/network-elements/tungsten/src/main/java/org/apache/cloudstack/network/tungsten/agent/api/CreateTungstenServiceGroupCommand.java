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
package org.apache.cloudstack.network.tungsten.agent.api;

import java.util.Objects;
import java.util.UUID;

public class CreateTungstenServiceGroupCommand extends TungstenCommand {
    private final String uuid;
    private final String name;
    private final String protocol;
    private final int startPort;
    private final int endPort;

    public CreateTungstenServiceGroupCommand(final String name, final String protocol, final int startPort,
        final int endPort) {
        this.uuid = UUID.randomUUID().toString();
        this.name = name;
        this.protocol = protocol;
        this.startPort = startPort;
        this.endPort = endPort;
    }

    public String getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public String getProtocol() {
        return protocol;
    }

    public int getStartPort() {
        return startPort;
    }

    public int getEndPort() {
        return endPort;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        CreateTungstenServiceGroupCommand that = (CreateTungstenServiceGroupCommand) o;
        return startPort == that.startPort && endPort == that.endPort && Objects.equals(uuid, that.uuid) && Objects.equals(name, that.name) && Objects.equals(protocol, that.protocol);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), uuid, name, protocol, startPort, endPort);
    }
}
