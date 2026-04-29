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

public class UpdateTungstenLoadBalancerPoolCommand extends TungstenCommand {
    private final String projectFqn;
    private final String lbPoolName;
    private final String lbMethod;
    private final String lbSessionPersistence;
    private final String lbPersistenceCookieName;
    private final String lbProtocol;
    private final boolean lbStatsEnable;
    private final String lbStatsPort;
    private final String lbStatsUri;
    private final String lbStatsAuth;

    public UpdateTungstenLoadBalancerPoolCommand(final String projectFqn, final String lbPoolName,
        final String lbMethod, final String lbSessionPersistence, final String lbPersistenceCookieName,
        final String lbProtocol, final boolean lbStatsEnable, final String lbStatsPort, final String lbStatsUri,
        final String lbStatsAuth) {
        this.projectFqn = projectFqn;
        this.lbPoolName = lbPoolName;
        this.lbMethod = lbMethod;
        this.lbSessionPersistence = lbSessionPersistence;
        this.lbPersistenceCookieName = lbPersistenceCookieName;
        this.lbProtocol = lbProtocol;
        this.lbStatsEnable = lbStatsEnable;
        this.lbStatsPort = lbStatsPort;
        this.lbStatsUri = lbStatsUri;
        this.lbStatsAuth = lbStatsAuth;
    }

    public String getProjectFqn() {
        return projectFqn;
    }

    public String getLbPoolName() {
        return lbPoolName;
    }

    public String getLbMethod() {
        return lbMethod;
    }

    public String getLbSessionPersistence() {
        return lbSessionPersistence;
    }

    public String getLbPersistenceCookieName() {
        return lbPersistenceCookieName;
    }

    public String getLbProtocol() {
        return lbProtocol;
    }

    public boolean isLbStatsEnable() {
        return lbStatsEnable;
    }

    public String getLbStatsPort() {
        return lbStatsPort;
    }

    public String getLbStatsUri() {
        return lbStatsUri;
    }

    public String getLbStatsAuth() {
        return lbStatsAuth;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        UpdateTungstenLoadBalancerPoolCommand that = (UpdateTungstenLoadBalancerPoolCommand) o;
        return lbStatsEnable == that.lbStatsEnable && Objects.equals(projectFqn, that.projectFqn) && Objects.equals(lbPoolName, that.lbPoolName) && Objects.equals(lbMethod, that.lbMethod) && Objects.equals(lbSessionPersistence, that.lbSessionPersistence) && Objects.equals(lbPersistenceCookieName, that.lbPersistenceCookieName) && Objects.equals(lbProtocol, that.lbProtocol) && Objects.equals(lbStatsPort, that.lbStatsPort) && Objects.equals(lbStatsUri, that.lbStatsUri) && Objects.equals(lbStatsAuth, that.lbStatsAuth);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), projectFqn, lbPoolName, lbMethod, lbSessionPersistence, lbPersistenceCookieName, lbProtocol, lbStatsEnable, lbStatsPort, lbStatsUri, lbStatsAuth);
    }
}
