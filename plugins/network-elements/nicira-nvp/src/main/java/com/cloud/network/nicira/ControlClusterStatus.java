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

public class ControlClusterStatus {
    private String clusterStatus;
    private Stats nodeStats;
    private Stats queueStats;
    private Stats portStats;
    private Stats routerportStats;
    private Stats switchStats;
    private Stats zoneStats;
    private Stats routerStats;
    private Stats securityProfileStats;
    private ClusterRoleConfig[] configuredRoles;

    public ClusterRoleConfig[] getConfiguredRoles() {
        return configuredRoles;
    }

    public String getClusterStatus() {
        return clusterStatus;
    }

    public Stats getNodeStats() {
        return nodeStats;
    }

    public Stats getLqueueStats() {
        return queueStats;
    }

    public Stats getLportStats() {
        return portStats;
    }

    public Stats getLrouterportStats() {
        return routerportStats;
    }

    public Stats getLswitchStats() {
        return switchStats;
    }

    public Stats getZoneStats() {
        return zoneStats;
    }

    public Stats getLrouterStats() {
        return routerStats;
    }

    public Stats getSecurityProfileStats() {
        return securityProfileStats;
    }

    public class Stats {
        private int errorStateCount;
        private int registeredCount;
        private int activeCount;

        public int getErrorStateCount() {
            return errorStateCount;
        }

        public int getRegisteredCount() {
            return registeredCount;
        }

        public int getActiveCount() {
            return activeCount;
        }

    }

    public class ClusterRoleConfig {
        public String majorityVersion;
        public String role;

        public String getMajorityVersion(){
            return majorityVersion;
        }

        public String getRole(){
            return role;
        }
    }
}
