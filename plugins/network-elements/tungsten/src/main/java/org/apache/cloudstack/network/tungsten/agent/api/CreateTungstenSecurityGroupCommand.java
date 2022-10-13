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

public class CreateTungstenSecurityGroupCommand extends TungstenCommand {
    private final String securityGroupUuid;
    private final String securityGroupName;
    private final String securityGroupDescription;
    private final String projectFqn;

    public CreateTungstenSecurityGroupCommand(String securityGroupUuid, String securityGroupName,
                                              String securityGroupDescription, String projectFqn) {
        this.securityGroupUuid = securityGroupUuid;
        this.securityGroupName = securityGroupName;
        this.securityGroupDescription = securityGroupDescription;
        this.projectFqn = projectFqn;
    }

    public String getSecurityGroupUuid() {
        return securityGroupUuid;
    }

    public String getSecurityGroupName() {
        return securityGroupName;
    }

    public String getSecurityGroupDescription() {
        return securityGroupDescription;
    }

    public String getProjectFqn() {
        return projectFqn;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        CreateTungstenSecurityGroupCommand that = (CreateTungstenSecurityGroupCommand) o;
        return Objects.equals(securityGroupUuid, that.securityGroupUuid) && Objects.equals(securityGroupName, that.securityGroupName) && Objects.equals(securityGroupDescription, that.securityGroupDescription) && Objects.equals(projectFqn, that.projectFqn);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), securityGroupUuid, securityGroupName, securityGroupDescription, projectFqn);
    }
}
