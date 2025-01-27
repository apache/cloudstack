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

import com.cloud.agent.api.Command;

import java.util.Objects;

public class UpdateTungstenLoadbalancerStatsCommand extends Command {
    private final String lbUuid;
    private final String lbStatsPort;
    private final String lbStatsUri;
    private final String lbStatsAuth;

    public UpdateTungstenLoadbalancerStatsCommand(final String lbUuid, final String lbStatsPort,
        final String lbStatsUri, final String lbStatsAuth) {
        this.lbUuid = lbUuid;
        this.lbStatsPort = lbStatsPort;
        this.lbStatsUri = lbStatsUri;
        this.lbStatsAuth = lbStatsAuth;
    }

    public String getLbUuid() {
        return lbUuid;
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
    public boolean executeInSequence() {
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        UpdateTungstenLoadbalancerStatsCommand that = (UpdateTungstenLoadbalancerStatsCommand) o;
        return Objects.equals(lbUuid, that.lbUuid) && Objects.equals(lbStatsPort, that.lbStatsPort) && Objects.equals(lbStatsUri, that.lbStatsUri) && Objects.equals(lbStatsAuth, that.lbStatsAuth);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), lbUuid, lbStatsPort, lbStatsUri, lbStatsAuth);
    }
}
