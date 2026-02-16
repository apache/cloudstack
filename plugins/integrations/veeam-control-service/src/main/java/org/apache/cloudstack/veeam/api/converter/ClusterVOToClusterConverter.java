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
import org.apache.cloudstack.veeam.api.dto.Cpu;
import org.apache.cloudstack.veeam.api.dto.Link;
import org.apache.cloudstack.veeam.api.dto.Ref;
import org.apache.cloudstack.veeam.api.dto.Version;

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
        c.setId(clusterId);
        c.setHref(basePath + ClustersRouteHandler.BASE_ROUTE + "/" + clusterId);

        c.setName(vo.getName());

        // --- sensible defaults (match your sample)
        c.setBallooningEnabled("true");
        c.setBiosType("q35_ovmf"); // or "q35_secure_boot" if you want to align with VM BIOS you saw
        c.setFipsMode("disabled");
        c.setFirewallType("firewalld");
        c.setGlusterService("false");
        c.setHaReservation("false");
        c.setSwitchType("legacy");
        c.setThreadsAsCores("false");
        c.setTrustedService("false");
        c.setTunnelMigration("false");
        c.setUpgradeInProgress("false");
        c.setUpgradePercentComplete("0");
        c.setVirtService("true");
        c.setVncEncryption("false");
        c.setLogMaxMemoryUsedThreshold("95");
        c.setLogMaxMemoryUsedThresholdType("percentage");

        // --- cpu (best-effort defaults)
        final Cpu cpu = new Cpu();
        cpu.setArchitecture("x86_64");
        cpu.setType("x86_64"); // replace if you can detect host cpu model
        c.setCpu(cpu);

        // --- version (ovirt engine version; keep fixed unless you want to expose something else)
        final Version ver = new Version();
        ver.setMajor("4");
        ver.setMinor("8");
        c.setVersion(ver);

        // --- ksm / memory policy (defaults)
        c.setKsm(new Cluster.Ksm());
        c.getKsm().enabled = "true";
        c.getKsm().mergeAcrossNodes = "true";

        c.setMemoryPolicy(new Cluster.MemoryPolicy());
        c.getMemoryPolicy().overCommit = new Cluster.OverCommit();
        c.getMemoryPolicy().overCommit.percent = "100";
        c.getMemoryPolicy().transparentHugepages = new Cluster.TransparentHugepages();
        c.getMemoryPolicy().transparentHugepages.enabled = "true";

        // --- migration defaults
        c.setMigration(new Cluster.Migration());
        c.getMigration().autoConverge = "inherit";
        c.getMigration().bandwidth = new Cluster.Bandwidth();
        c.getMigration().bandwidth.assignmentMethod = "auto";
        c.getMigration().compressed = "inherit";
        c.getMigration().encrypted = "inherit";
        c.getMigration().parallelMigrationsPolicy = "disabled";
        // policy ref (dummy but valid shape)
        c.getMigration().policy = Ref.of(basePath + "/migrationpolicies/" + stableUuid("migrationpolicy:default"),
                stableUuid("migrationpolicy:default")
        );

        // --- rng sources
        c.setRequiredRngSources(new Cluster.RequiredRngSources());
        c.getRequiredRngSources().requiredRngSource = Collections.singletonList("urandom");

        // --- error handling
        c.setErrorHandling(new Cluster.ErrorHandling());
        c.getErrorHandling().onError = "migrate";

        // --- fencing policy defaults
        c.setFencingPolicy(new Cluster.FencingPolicy());
        c.getFencingPolicy().enabled = "true";
        c.getFencingPolicy().skipIfConnectivityBroken = new Cluster.SkipIfConnectivityBroken();
        c.getFencingPolicy().skipIfConnectivityBroken.enabled = "false";
        c.getFencingPolicy().skipIfConnectivityBroken.threshold = "50";
        c.getFencingPolicy().skipIfGlusterBricksUp = "false";
        c.getFencingPolicy().skipIfGlusterQuorumNotMet = "false";
        c.getFencingPolicy().skipIfSdActive = new Cluster.SkipIfSdActive();
        c.getFencingPolicy().skipIfSdActive.enabled = "false";

        // --- scheduling policy props (optional; dummy ok)
        c.setCustomSchedulingPolicyProperties(new Cluster.CustomSchedulingPolicyProperties());
        final Cluster.Property p1 = new Cluster.Property(); p1.name = "HighUtilization"; p1.value = "80";
        final Cluster.Property p2 = new Cluster.Property(); p2.name = "CpuOverCommitDurationMinutes"; p2.value = "2";
        c.getCustomSchedulingPolicyProperties().property = List.of(p1, p2);

        // --- data_center ref mapping (CloudStack cluster -> pod -> zone)
        if (dataCenterResolver != null) {
            final DataCenterJoinVO zone = dataCenterResolver.apply(vo.getDataCenterId());
            if (zone != null) {
                c.setDataCenter(Ref.of(basePath + DataCentersRouteHandler.BASE_ROUTE + "/" + zone.getUuid(), zone.getUuid()));
            }
        }

        // --- mac pool & scheduling policy refs (dummy but consistent)
        c.setMacPool(Ref.of(basePath + "/macpools/" + stableUuid("macpool:default"),
                stableUuid("macpool:default")));
        c.setSchedulingPolicy(Ref.of(basePath + "/schedulingpolicies/" + stableUuid("schedpolicy:default"),
                stableUuid("schedpolicy:default")));

        // --- actions.links (can be omitted; but Veeam sometimes expects actions to exist)
        final Actions actions = new Actions();
        actions.setLink(Collections.emptyList());
        c.setActions(actions);

        // --- related links (optional)
        c.setLink(List.of(
                Link.of("networks", c.getHref() + "/networks")
        ));

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
