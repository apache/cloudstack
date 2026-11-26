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
    ConfigKey<Boolean> KvmHostMaintenanceRunHooks = new ConfigKey<>("Advanced", Boolean.class,
            "kvm.host.maintenance.run.hooks", "false",
            "Run the rolling maintenance hook scripts on KVM hosts (agent.properties: rolling.maintenance.hooks.dir) " +
                    "when a host is put into or taken out of maintenance with prepareHostForMaintenance and " +
                    "cancelHostMaintenance. Stages: PreFlight and PreMaintenance before VMs are moved off the host " +
                    "(a failure or exit code 70 refuses maintenance), PostMigrate once the host has reached " +
                    "Maintenance, PostMaintenance on cancel before the host is enabled again (failures of the last " +
                    "two raise an alert but do not change the host state; cancel needs the agent connected while " +
                    "this is enabled). Stages never overlap on a host. The Maintenance stage is not run and no " +
                    "payload argument is passed. A stage without a script is skipped. Each stage is limited by " +
                    "kvm.rolling.maintenance.stage.timeout.",
            true, ConfigKey.Scope.Cluster);

    String HOST_MAINTENANCE_HOOKS_DETAIL = "maintenance.hooks";

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
        PreFlight, PreMaintenance, PostMigrate, Maintenance, PostMaintenance;

        public Stage next() {
            switch (this) {
                case PreFlight:
                    return PreMaintenance;
                case PreMaintenance:
                    return PostMigrate;
                case PostMigrate:
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

    /**
     * Runs the PreFlight and PreMaintenance hooks on a host about to enter maintenance outside rolling maintenance,
     * if {@link #KvmHostMaintenanceRunHooks} is enabled for its cluster.
     * @return true if the hooks ran, false if they are disabled or already ran for this maintenance
     * @throws CloudRuntimeException if the host is not Up, or a hook fails or asks to avoid maintenance
     */
    boolean runPreMaintenanceHooks(Host host);

    /**
     * Starts the PostMigrate hook in the background on a host that has just entered maintenance,
     * if its maintenance was started with {@link #runPreMaintenanceHooks} and not cancelled since.
     */
    void runPostMigrateHook(Host host);

    /**
     * Runs the PostMaintenance hook on a host leaving maintenance, if its maintenance was started with
     * {@link #runPreMaintenanceHooks}, and forgets that it was. A hook failure raises an alert and is not thrown.
     * Waits up to the stage timeout for the agent to connect.
     * @throws CloudRuntimeException if the host is not Up while the setting is still enabled, or another hook
     * on the host does not finish in time
     */
    void runPostMaintenanceHook(Host host);

    /**
     * @return true if the host's current maintenance was started with {@link #runPreMaintenanceHooks}
     */
    boolean isMaintenanceHooksActive(Host host);

    /**
     * Forgets that the host's maintenance was started with {@link #runPreMaintenanceHooks}.
     */
    void clearMaintenanceHooks(Host host);
}
