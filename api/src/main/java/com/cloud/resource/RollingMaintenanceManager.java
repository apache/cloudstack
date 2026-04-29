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
package com.cloud.resource;

import com.cloud.host.Host;
import com.cloud.utils.Pair;
import com.cloud.utils.Ternary;
import com.cloud.utils.exception.CloudRuntimeException;
import org.apache.cloudstack.api.command.admin.resource.StartRollingMaintenanceCmd;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;

import java.util.Date;
import java.util.List;

public interface RollingMaintenanceManager extends Configurable {

    ConfigKey<Integer> KvmRollingMaintenanceStageTimeout = new ConfigKey<>("Advanced", Integer.class,
            "kvm.rolling.maintenance.stage.timeout", "1800",
            "Wait timeout (in seconds) for a rolling maintenance stage update from hosts",
            true, ConfigKey.Scope.Global);
    ConfigKey<Integer> KvmRollingMaintenancePingInterval = new ConfigKey<>("Advanced", Integer.class,
            "kvm.rolling.maintenance.ping.interval", "10",
            "Ping interval in seconds between management server and hosts performing stages during rolling maintenance",
            true, ConfigKey.Scope.Global);
    ConfigKey<Integer> KvmRollingMaintenanceWaitForMaintenanceTimeout = new ConfigKey<>("Advanced", Integer.class,
            "kvm.rolling.maintenance.wait.maintenance.timeout", "1800",
            "Timeout (in seconds) to wait for a host preparing to enter maintenance mode",
            true, ConfigKey.Scope.Global);

    class HostSkipped {
        private Host host;
        private String reason;

        public HostSkipped(Host host, String reason) {
            this.host = host;
            this.reason = reason;
        }

        public Host getHost() {
            return host;
        }

        public void setHost(Host host) {
            this.host = host;
        }

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }
    }

    class HostUpdated {
        private Host host;
        private Date start;
        private Date end;
        private String outputMsg;

        public HostUpdated(Host host, Date start, Date end, String outputMsg) {
            this.host = host;
            this.start = start;
            this.end = end;
            this.outputMsg = outputMsg;
        }

        public Host getHost() {
            return host;
        }

        public void setHost(Host host) {
            this.host = host;
        }

        public Date getStart() {
            return start;
        }

        public void setStart(Date start) {
            this.start = start;
        }

        public Date getEnd() {
            return end;
        }

        public void setEnd(Date end) {
            this.end = end;
        }

        public String getOutputMsg() {
            return outputMsg;
        }

        public void setOutputMsg(String outputMsg) {
            this.outputMsg = outputMsg;
        }
    }

    enum Stage {
        PreFlight, PreMaintenance, Maintenance, PostMaintenance;

        public Stage next() {
            switch (this) {
                case PreFlight:
                    return PreMaintenance;
                case PreMaintenance:
                    return Maintenance;
                case Maintenance:
                    return PostMaintenance;
                case PostMaintenance:
                    return null;
            }
            throw new CloudRuntimeException("Unexpected stage: " + this);
        }
    }

    enum ResourceType {
        Pod, Cluster, Zone, Host
    }

    /**
     * Starts rolling maintenance as specified in cmd
     * @param cmd command
     * @return tuple: (SUCCESS, DETAILS, (HOSTS_UPDATED, HOSTS_SKIPPED))
     */
    Ternary<Boolean, String, Pair<List<HostUpdated>, List<HostSkipped>>> startRollingMaintenance(StartRollingMaintenanceCmd cmd);
    Pair<ResourceType, List<Long>> getResourceTypeIdPair(StartRollingMaintenanceCmd cmd);
}
