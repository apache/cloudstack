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
import java.util.stream.Collectors;

import org.apache.cloudstack.veeam.VeeamControlService;
import org.apache.cloudstack.veeam.api.ClustersRouteHandler;
import org.apache.cloudstack.veeam.api.HostsRouteHandler;
import org.apache.cloudstack.veeam.api.dto.Cpu;
import org.apache.cloudstack.veeam.api.dto.Host;
import org.apache.cloudstack.veeam.api.dto.Ref;
import org.apache.cloudstack.veeam.api.dto.Topology;

import com.cloud.api.query.vo.HostJoinVO;
import com.cloud.host.Status;
import com.cloud.resource.ResourceState;

public class HostJoinVOToHostConverter {

    /**
     * Convert CloudStack HostJoinVO -> oVirt-like Host.
     *
     * @param vo HostJoinVO from listHosts (join query)
     */
    public static Host toHost(final HostJoinVO vo) {
        final Host h = new Host();

        final String hostUuid = vo.getUuid();

        h.setId(hostUuid);
        final String basePath = VeeamControlService.ContextPath.value();
        h.setHref(basePath + HostsRouteHandler.BASE_ROUTE + "/" + hostUuid);

        // --- name / address ---
        // Prefer DNS name if set; otherwise fall back to IP
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
        cpu.setSpeed(Math.toIntExact(vo.getSpeed()));
        final Topology topo = new Topology(vo.getCpuSockets(), vo.getCpus(), 1);
        cpu.setTopology(topo);
        h.setCpu(cpu);

        // --- Memory ---
        h.setMemory(String.valueOf(vo.getTotalMemory()));
        h.setMaxSchedulingMemory(String.valueOf(vo.getTotalMemory() - vo.getMemUsedCapacity())); // ToDo: check

        // --- OS / versions (optional placeholders) ---
        // If you want, you can set conservative defaults to match oVirt shape.
        h.setType("rhel");
        h.setAutoNumaStatus("unknown");
        h.setKdumpStatus("disabled");
        h.setNumaSupported("false");
        h.setReinstallationRequired("false");
        h.setUpdateAvailable("false");


        // --- links/actions ---
        // Start minimal (empty). Add actions only if Veeam tries to follow them.
        h.setActions(null);
        h.setLink(Collections.emptyList());

        return h;
    }

    public static List<Host> toHostList(final List<HostJoinVO> vos) {
        return vos.stream().map(HostJoinVOToHostConverter::toHost).collect(Collectors.toList());
    }

    private static String mapStatus(final HostJoinVO vo) {
        // CloudStack examples:
        // state: Up/Down/Maintenance/Error/Disconnected
        // status: Up/Down/Connecting/etc
        if (vo.isInMaintenanceStates()) return "maintenance";
        if (Status.Up.equals(vo.getStatus()) && ResourceState.Enabled.equals(vo.getResourceState())) return "up";

        // Default
        return "down";
    }
}
