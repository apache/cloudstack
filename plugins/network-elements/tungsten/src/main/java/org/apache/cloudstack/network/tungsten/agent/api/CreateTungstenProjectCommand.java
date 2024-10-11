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

public class CreateTungstenProjectCommand extends TungstenCommand {
    private final String tungstenProjectName;
    private final String tungstenProjectUuid;
    private final String tungstenDomainUuid;
    private final String tungstenDomainName;

    public CreateTungstenProjectCommand(String tungstenProjectName, String tungstenProjectUuid,
                                        String tungstenDomainUuid, String tungstenDomainName) {
        this.tungstenProjectName = tungstenProjectName;
        this.tungstenProjectUuid = tungstenProjectUuid;
        this.tungstenDomainUuid = tungstenDomainUuid;
        this.tungstenDomainName = tungstenDomainName;
    }

    public String getTungstenProjectName() {
        return tungstenProjectName;
    }

    public String getTungstenProjectUuid() {
        return tungstenProjectUuid;
    }

    public String getTungstenDomainUuid() {
        return tungstenDomainUuid;
    }

    public String getTungstenDomainName() {
        return tungstenDomainName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        CreateTungstenProjectCommand that = (CreateTungstenProjectCommand) o;
        return Objects.equals(tungstenProjectName, that.tungstenProjectName) && Objects.equals(tungstenProjectUuid, that.tungstenProjectUuid) && Objects.equals(tungstenDomainUuid, that.tungstenDomainUuid) && Objects.equals(tungstenDomainName, that.tungstenDomainName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), tungstenProjectName, tungstenProjectUuid, tungstenDomainUuid, tungstenDomainName);
    }
}
