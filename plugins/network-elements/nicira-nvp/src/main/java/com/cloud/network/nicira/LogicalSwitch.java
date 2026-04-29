//
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
//

package com.cloud.network.nicira;

import java.util.List;

public class LogicalSwitch extends BaseNiciraNamedEntity {
    public static final String REPLICATION_MODE_SERVICE = "service";
    public static final String REPLICATION_MODE_SOURCE = "source";

    private final String type = "LogicalSwitchConfig";
    private boolean portIsolationEnabled;
    private List<TransportZoneBinding> transportZones;
    private String replicationMode;

    public boolean isPortIsolationEnabled() {
        return portIsolationEnabled;
    }

    public void setPortIsolationEnabled(final boolean portIsolationEnabled) {
        this.portIsolationEnabled = portIsolationEnabled;
    }

    public String getType() {
        return type;
    }

    public List<TransportZoneBinding> getTransportZones() {
        return transportZones;
    }

    public void setTransportZones(final List<TransportZoneBinding> transportZones) {
        this.transportZones = transportZones;
    }

    public String getReplicationMode() {
        return replicationMode;
    }

    public void setReplicationMode(final String replicationMode) {
        this.replicationMode = replicationMode;
    }
}
