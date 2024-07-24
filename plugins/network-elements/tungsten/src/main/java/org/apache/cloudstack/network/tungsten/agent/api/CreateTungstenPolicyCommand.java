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

public class CreateTungstenPolicyCommand extends TungstenCommand {
    private final String uuid;
    private final String name;
    private final String projectFqn;

    public CreateTungstenPolicyCommand(final String name, final String projectFqn) {
        this.uuid = UUID.randomUUID().toString();
        this.name = name;
        this.projectFqn = projectFqn;
    }

    public String getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public String getProjectFqn() {
        return projectFqn;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        CreateTungstenPolicyCommand that = (CreateTungstenPolicyCommand) o;
        return Objects.equals(uuid, that.uuid) && Objects.equals(name, that.name) && Objects.equals(projectFqn, that.projectFqn);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), uuid, name, projectFqn);
    }
}
