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

package org.apache.cloudstack.veeam.api.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JacksonXmlRootElement(localName = "cluster")
public final class Cluster {

    // --- common identity
    public String href;
    public String id;
    public String name;
    public String description;
    public String comment;

    // --- oVirt-ish knobs (strings in oVirt JSON)
    @JsonProperty("ballooning_enabled")
    @JacksonXmlProperty(localName = "ballooning_enabled")
    public String ballooningEnabled; // "true"/"false"

    @JsonProperty("bios_type")
    @JacksonXmlProperty(localName = "bios_type")
    public String biosType; // e.g. "q35_ovmf"

    public ClusterCpu cpu;

    @JsonProperty("custom_scheduling_policy_properties")
    @JacksonXmlProperty(localName = "custom_scheduling_policy_properties")
    public CustomSchedulingPolicyProperties customSchedulingPolicyProperties;

    @JsonProperty("error_handling")
    @JacksonXmlProperty(localName = "error_handling")
    public ErrorHandling errorHandling;

    @JsonProperty("fencing_policy")
    @JacksonXmlProperty(localName = "fencing_policy")
    public FencingPolicy fencingPolicy;

    @JsonProperty("fips_mode")
    @JacksonXmlProperty(localName = "fips_mode")
    public String fipsMode; // "disabled"

    @JsonProperty("firewall_type")
    @JacksonXmlProperty(localName = "firewall_type")
    public String firewallType; // "firewalld"

    @JsonProperty("gluster_service")
    @JacksonXmlProperty(localName = "gluster_service")
    public String glusterService;

    @JsonProperty("ha_reservation")
    @JacksonXmlProperty(localName = "ha_reservation")
    public String haReservation;

    public Ksm ksm;

    @JsonProperty("log_max_memory_used_threshold")
    @JacksonXmlProperty(localName = "log_max_memory_used_threshold")
    public String logMaxMemoryUsedThreshold;

    @JsonProperty("log_max_memory_used_threshold_type")
    @JacksonXmlProperty(localName = "log_max_memory_used_threshold_type")
    public String logMaxMemoryUsedThresholdType;

    @JsonProperty("memory_policy")
    @JacksonXmlProperty(localName = "memory_policy")
    public MemoryPolicy memoryPolicy;

    public Migration migration;

    @JsonProperty("required_rng_sources")
    @JacksonXmlProperty(localName = "required_rng_sources")
    public RequiredRngSources requiredRngSources;

    @JsonProperty("switch_type")
    @JacksonXmlProperty(localName = "switch_type")
    public String switchType;

    @JsonProperty("threads_as_cores")
    @JacksonXmlProperty(localName = "threads_as_cores")
    public String threadsAsCores;

    @JsonProperty("trusted_service")
    @JacksonXmlProperty(localName = "trusted_service")
    public String trustedService;

    @JsonProperty("tunnel_migration")
    @JacksonXmlProperty(localName = "tunnel_migration")
    public String tunnelMigration;

    @JsonProperty("upgrade_in_progress")
    @JacksonXmlProperty(localName = "upgrade_in_progress")
    public String upgradeInProgress;

    @JsonProperty("upgrade_percent_complete")
    @JacksonXmlProperty(localName = "upgrade_percent_complete")
    public String upgradePercentComplete;

    public Version version;

    @JsonProperty("virt_service")
    @JacksonXmlProperty(localName = "virt_service")
    public String virtService;

    @JsonProperty("vnc_encryption")
    @JacksonXmlProperty(localName = "vnc_encryption")
    public String vncEncryption;

    // --- references
    @JsonProperty("data_center")
    @JacksonXmlProperty(localName = "data_center")
    public Ref dataCenter;

    @JsonProperty("mac_pool")
    @JacksonXmlProperty(localName = "mac_pool")
    public Ref macPool;

    @JsonProperty("scheduling_policy")
    @JacksonXmlProperty(localName = "scheduling_policy")
    public Ref schedulingPolicy;

    // --- actions + links
    public Actions actions;

    @JacksonXmlElementWrapper(useWrapping = false)
    public List<Link> link;

    public Cluster() {}

    // ===== nested DTOs =====

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static final class ClusterCpu {
        public String architecture;
        public String type;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static final class CustomSchedulingPolicyProperties {
        @JacksonXmlElementWrapper(useWrapping = false)
        @JsonProperty("property")
        public List<Property> property;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static final class Property {
        public String name;
        public String value;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static final class ErrorHandling {
        @JsonProperty("on_error")
        @JacksonXmlProperty(localName = "on_error")
        public String onError; // "migrate"
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static final class FencingPolicy {
        public String enabled;

        @JsonProperty("skip_if_connectivity_broken")
        @JacksonXmlProperty(localName = "skip_if_connectivity_broken")
        public SkipIfConnectivityBroken skipIfConnectivityBroken;

        @JsonProperty("skip_if_gluster_bricks_up")
        @JacksonXmlProperty(localName = "skip_if_gluster_bricks_up")
        public String skipIfGlusterBricksUp;

        @JsonProperty("skip_if_gluster_quorum_not_met")
        @JacksonXmlProperty(localName = "skip_if_gluster_quorum_not_met")
        public String skipIfGlusterQuorumNotMet;

        @JsonProperty("skip_if_sd_active")
        @JacksonXmlProperty(localName = "skip_if_sd_active")
        public SkipIfSdActive skipIfSdActive;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static final class SkipIfConnectivityBroken {
        public String enabled;
        public String threshold;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static final class SkipIfSdActive {
        public String enabled;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static final class Ksm {
        public String enabled;

        @JsonProperty("merge_across_nodes")
        @JacksonXmlProperty(localName = "merge_across_nodes")
        public String mergeAcrossNodes;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static final class MemoryPolicy {
        @JsonProperty("over_commit")
        @JacksonXmlProperty(localName = "over_commit")
        public OverCommit overCommit;

        @JsonProperty("transparent_hugepages")
        @JacksonXmlProperty(localName = "transparent_hugepages")
        public TransparentHugepages transparentHugepages;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static final class OverCommit {
        public String percent;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static final class TransparentHugepages {
        public String enabled;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static final class Migration {
        @JsonProperty("auto_converge")
        @JacksonXmlProperty(localName = "auto_converge")
        public String autoConverge;

        public Bandwidth bandwidth;

        public String compressed;
        public String encrypted;

        @JsonProperty("parallel_migrations_policy")
        @JacksonXmlProperty(localName = "parallel_migrations_policy")
        public String parallelMigrationsPolicy;

        public Ref policy;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static final class Bandwidth {
        @JsonProperty("assignment_method")
        @JacksonXmlProperty(localName = "assignment_method")
        public String assignmentMethod;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static final class RequiredRngSources {
        @JsonProperty("required_rng_source")
        @JacksonXmlElementWrapper(useWrapping = false)
        public List<String> requiredRngSource;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static final class Version {
        public String major;
        public String minor;
    }
}
