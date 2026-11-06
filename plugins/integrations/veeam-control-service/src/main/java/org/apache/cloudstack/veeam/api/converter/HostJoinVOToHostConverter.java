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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.cloudstack.veeam.VeeamControlService;
import org.apache.cloudstack.veeam.api.ClustersRouteHandler;
import org.apache.cloudstack.veeam.api.HostsRouteHandler;
import org.apache.cloudstack.veeam.api.dto.Cpu;
import org.apache.cloudstack.veeam.api.dto.Host;
import org.apache.cloudstack.veeam.api.dto.Link;
import org.apache.cloudstack.veeam.api.dto.NamedList;
import org.apache.cloudstack.veeam.api.dto.Ref;
import org.apache.cloudstack.veeam.api.dto.Statistic;
import org.apache.cloudstack.veeam.api.dto.Topology;

import com.cloud.api.query.vo.HostJoinVO;
import com.cloud.host.HostStats;
import com.cloud.host.Status;
import com.cloud.resource.ResourceState;

public class HostJoinVOToHostConverter {
    public static final String STATISTICS = "statistics";

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    /**
     * Convert CloudStack HostJoinVO -> oVirt-like Host.
     *
     */
    public static Host toHost(final HostJoinVO vo) {
        return toHost(vo, null);
    }

    /**
     * Convert CloudStack HostJoinVO -> oVirt-like Host.
     *
     * @param statsResolver when non-null, the host's statistics are inlined (oVirt follow=statistics)
     */
    public static Host toHost(final HostJoinVO vo, final Function<Long, HostStats> statsResolver) {
        final Host h = new Host();

        final String hostUuid = vo.getUuid();

        h.setId(hostUuid);
        final String basePath = VeeamControlService.ContextPath.value();
        final String href = getHref(hostUuid);
        h.setHref(href);

        final String name = vo.getName() != null ? vo.getName() : ("host-" + hostUuid);
        h.setName(name);

        String addr = vo.getPrivateIpAddress();
        h.setAddress(addr);

        h.setStatus(mapStatus(vo));
        h.setExternalStatus("ok");

        // --- cluster ---
        final String clusterUuid = vo.getClusterUuid();
        h.setCluster(Ref.of(basePath + ClustersRouteHandler.BASE_ROUTE + "/" + clusterUuid, clusterUuid));

        // --- CPU ---
        final Cpu cpu = new Cpu();
        cpu.setSpeed(String.valueOf(Math.toIntExact(vo.getSpeed())));
        final Topology topo = new Topology(vo.getCpuSockets(), vo.getCpus(), 1);
        cpu.setTopology(topo);
        h.setCpu(cpu);

        // --- Memory ---
        h.setMemory(String.valueOf(vo.getTotalMemory()));
        h.setMaxSchedulingMemory(String.valueOf(Math.max(0, vo.getTotalMemory() - vo.getMemUsedCapacity())));

        h.setType("ovirt_node");
        h.setAutoNumaStatus("unknown");
        h.setKdumpStatus("disabled");
        h.setNumaSupported("false");
        h.setReinstallationRequired("false");
        h.setUpdateAvailable("false");


        h.setActions(null);
        h.setLink(Collections.singletonList(Link.of(STATISTICS, href + "/" + STATISTICS)));

        if (statsResolver != null) {
            h.setStatistics(NamedList.of("statistic", toStatistics(vo, statsResolver.apply(vo.getId()))));
        }

        return h;
    }

    public static List<Host> toHostList(final List<HostJoinVO> vos) {
        return toHostList(vos, null);
    }

    public static List<Host> toHostList(final List<HostJoinVO> vos, final Function<Long, HostStats> statsResolver) {
        return vos.stream().map(vo -> toHost(vo, statsResolver)).collect(Collectors.toList());
    }

    /**
     * Maps the collected host stats to oVirt host statistics. Physical memory is used for memory.total (not the
     * overprovisioned value reported by listHosts) so that it is consistent with memory.used.
     * Live values are only reported for hosts that are Up, as the stats cache keeps the last entry of a host that went down.
     */
    public static List<Statistic> toStatistics(final HostJoinVO vo, final HostStats hostStats) {
        final Ref hostRef = Ref.of(getHref(vo.getUuid()), vo.getUuid());
        final List<Statistic> statistics = new ArrayList<>();
        final boolean live = hostStats != null && Status.Up.equals(vo.getStatus());

        long totalMemory = vo.getTotalMemory();
        if (live && hostStats.getTotalMemoryKBs() > 0) {
            totalMemory = kbToBytes(hostStats.getTotalMemoryKBs());
        }
        statistics.add(Statistic.hostGauge(hostRef, "memory.total", "Total memory",
                Statistic.TYPE_INTEGER, Statistic.UNIT_BYTES, BigDecimal.valueOf(totalMemory)));
        if (!live) {
            return statistics;
        }

        statistics.add(Statistic.hostGauge(hostRef, "memory.used", "Used memory",
                Statistic.TYPE_INTEGER, Statistic.UNIT_BYTES, BigDecimal.valueOf(Math.max(0L, (long) hostStats.getUsedMemory()))));
        statistics.add(Statistic.hostGauge(hostRef, "memory.free", "Free memory",
                Statistic.TYPE_INTEGER, Statistic.UNIT_BYTES, BigDecimal.valueOf(kbToBytes(hostStats.getFreeMemoryKBs()))));
        if (Double.isFinite(hostStats.getCpuUtilization())) {
            statistics.add(Statistic.hostGauge(hostRef, "cpu.current.idle", "Idle CPU usage",
                    Statistic.TYPE_DECIMAL, Statistic.UNIT_PERCENT, toIdlePercent(hostStats.getCpuUtilization())));
        }
        return statistics;
    }

    private static String getHref(final String hostUuid) {
        return VeeamControlService.ContextPath.value() + HostsRouteHandler.BASE_ROUTE + "/" + hostUuid;
    }

    private static long kbToBytes(final double kb) {
        return Math.max(0L, (long) (kb * 1024));
    }

    private static BigDecimal toIdlePercent(final double cpuUtilization) {
        BigDecimal idle = HUNDRED.subtract(BigDecimal.valueOf(cpuUtilization));
        if (idle.signum() < 0) {
            idle = BigDecimal.ZERO;
        } else if (idle.compareTo(HUNDRED) > 0) {
            idle = HUNDRED;
        }
        return idle.setScale(2, RoundingMode.HALF_UP);
    }

    private static String mapStatus(final HostJoinVO vo) {
        if (vo.isInMaintenanceStates()) {
            return "maintenance";
        }
        if (Status.Up.equals(vo.getStatus()) &&
                ResourceState.Enabled.equals(vo.getResourceState())) {
            return "up";
        }
        return "down";
    }
}
