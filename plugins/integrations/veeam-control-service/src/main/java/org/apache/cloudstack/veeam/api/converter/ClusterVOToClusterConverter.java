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

package org.apache.cloudstack.veeam.api.converter;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.cloudstack.veeam.VeeamControlService;
import org.apache.cloudstack.veeam.api.ClustersRouteHandler;
import org.apache.cloudstack.veeam.api.DataCentersRouteHandler;
import org.apache.cloudstack.veeam.api.dto.Actions;
import org.apache.cloudstack.veeam.api.dto.Cluster;
import org.apache.cloudstack.veeam.api.dto.Link;
import org.apache.cloudstack.veeam.api.dto.Ref;

import com.cloud.api.query.vo.DataCenterJoinVO;
import com.cloud.dc.ClusterVO;
import com.cloud.utils.UuidUtils;

public class ClusterVOToClusterConverter {
    public static Cluster toCluster(final ClusterVO vo, final Function<Long, DataCenterJoinVO> dataCenterResolver) {
        final Cluster c = new Cluster();
        final String basePath = VeeamControlService.ContextPath.value();

        // NOTE: oVirt uses UUIDs. If your ClusterVO id is numeric, generate a stable UUID:
        // - Prefer: store a UUID in details table and reuse it
        // - Fallback: name-based UUID from "cluster:<id>"
        final String clusterId = vo.getUuid();
        c.id = clusterId;
        c.href = basePath + ClustersRouteHandler.BASE_ROUTE + "/" + clusterId;

        c.name = vo.getName();
        c.description = vo.getName();
        c.comment = "";

        // --- sensible defaults (match your sample)
        c.ballooningEnabled = "true";
        c.biosType = "q35_ovmf"; // or "q35_secure_boot" if you want to align with VM BIOS you saw
        c.fipsMode = "disabled";
        c.firewallType = "firewalld";
        c.glusterService = "false";
        c.haReservation = "false";
        c.switchType = "legacy";
        c.threadsAsCores = "false";
        c.trustedService = "false";
        c.tunnelMigration = "false";
        c.upgradeInProgress = "false";
        c.upgradePercentComplete = "0";
        c.virtService = "true";
        c.vncEncryption = "false";
        c.logMaxMemoryUsedThreshold = "95";
        c.logMaxMemoryUsedThresholdType = "percentage";

        // --- cpu (best-effort defaults)
        final Cluster.ClusterCpu cpu = new Cluster.ClusterCpu();
        cpu.architecture = "x86_64";
        cpu.type = "x86_64"; // replace if you can detect host cpu model
        c.cpu = cpu;

        // --- version (ovirt engine version; keep fixed unless you want to expose something else)
        final Cluster.Version ver = new Cluster.Version();
        ver.major = "4";
        ver.minor = "8";
        c.version = ver;

        // --- ksm / memory policy (defaults)
        c.ksm = new Cluster.Ksm();
        c.ksm.enabled = "true";
        c.ksm.mergeAcrossNodes = "true";

        c.memoryPolicy = new Cluster.MemoryPolicy();
        c.memoryPolicy.overCommit = new Cluster.OverCommit();
        c.memoryPolicy.overCommit.percent = "100";
        c.memoryPolicy.transparentHugepages = new Cluster.TransparentHugepages();
        c.memoryPolicy.transparentHugepages.enabled = "true";

        // --- migration defaults
        c.migration = new Cluster.Migration();
        c.migration.autoConverge = "inherit";
        c.migration.bandwidth = new Cluster.Bandwidth();
        c.migration.bandwidth.assignmentMethod = "auto";
        c.migration.compressed = "inherit";
        c.migration.encrypted = "inherit";
        c.migration.parallelMigrationsPolicy = "disabled";
        // policy ref (dummy but valid shape)
        c.migration.policy = Ref.of(basePath + "/migrationpolicies/" + stableUuid("migrationpolicy:default"),
                stableUuid("migrationpolicy:default")
        );

        // --- rng sources
        c.requiredRngSources = new Cluster.RequiredRngSources();
        c.requiredRngSources.requiredRngSource = Collections.singletonList("urandom");

        // --- error handling
        c.errorHandling = new Cluster.ErrorHandling();
        c.errorHandling.onError = "migrate";

        // --- fencing policy defaults
        c.fencingPolicy = new Cluster.FencingPolicy();
        c.fencingPolicy.enabled = "true";
        c.fencingPolicy.skipIfConnectivityBroken = new Cluster.SkipIfConnectivityBroken();
        c.fencingPolicy.skipIfConnectivityBroken.enabled = "false";
        c.fencingPolicy.skipIfConnectivityBroken.threshold = "50";
        c.fencingPolicy.skipIfGlusterBricksUp = "false";
        c.fencingPolicy.skipIfGlusterQuorumNotMet = "false";
        c.fencingPolicy.skipIfSdActive = new Cluster.SkipIfSdActive();
        c.fencingPolicy.skipIfSdActive.enabled = "false";

        // --- scheduling policy props (optional; dummy ok)
        c.customSchedulingPolicyProperties = new Cluster.CustomSchedulingPolicyProperties();
        final Cluster.Property p1 = new Cluster.Property(); p1.name = "HighUtilization"; p1.value = "80";
        final Cluster.Property p2 = new Cluster.Property(); p2.name = "CpuOverCommitDurationMinutes"; p2.value = "2";
        c.customSchedulingPolicyProperties.property = List.of(p1, p2);

        // --- data_center ref mapping (CloudStack cluster -> pod -> zone)
        if (dataCenterResolver != null) {
            final DataCenterJoinVO zone = dataCenterResolver.apply(vo.getDataCenterId());
            if (zone != null) {
                c.dataCenter = Ref.of(basePath + DataCentersRouteHandler.BASE_ROUTE + "/" + zone.getUuid(), zone.getUuid());
            }
        }

        // --- mac pool & scheduling policy refs (dummy but consistent)
        c.macPool = Ref.of(basePath + "/macpools/" + stableUuid("macpool:default"),
                stableUuid("macpool:default"));
        c.schedulingPolicy = Ref.of(basePath + "/schedulingpolicies/" + stableUuid("schedpolicy:default"),
                stableUuid("schedpolicy:default"));

        // --- actions.links (can be omitted; but Veeam sometimes expects actions to exist)
        final Actions actions = new Actions();
        actions.link = Collections.emptyList();
        c.actions = actions;

        // --- related links (optional)
        c.link = List.of(
                new Link("networks", c.href + "/networks")
        );

        return c;
    }

    public static List<Cluster> toClusterList(final List<ClusterVO> voList,
            final Function<Long, DataCenterJoinVO> dataCenterResolver) {
        return voList.stream()
                .map(vo -> toCluster(vo, dataCenterResolver))
                .collect(Collectors.toList());
    }

    private static String stableUuid(final String key) {
        // deterministic UUID, so the same ClusterVO maps to same "ovirt id" every time
        return UuidUtils.nameUUIDFromBytes(key.getBytes()).toString();
    }
}
