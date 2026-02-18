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

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.cloudstack.veeam.VeeamControlService;
import org.apache.cloudstack.veeam.api.ApiService;
import org.apache.cloudstack.veeam.api.VmsRouteHandler;
import org.apache.cloudstack.veeam.api.dto.BaseDto;
import org.apache.cloudstack.veeam.api.dto.Bios;
import org.apache.cloudstack.veeam.api.dto.Cpu;
import org.apache.cloudstack.veeam.api.dto.DiskAttachment;
import org.apache.cloudstack.veeam.api.dto.EmptyElement;
import org.apache.cloudstack.veeam.api.dto.NamedList;
import org.apache.cloudstack.veeam.api.dto.Nic;
import org.apache.cloudstack.veeam.api.dto.Nics;
import org.apache.cloudstack.veeam.api.dto.Os;
import org.apache.cloudstack.veeam.api.dto.Ref;
import org.apache.cloudstack.veeam.api.dto.Topology;
import org.apache.cloudstack.veeam.api.dto.Vm;
import org.apache.commons.lang3.StringUtils;

import com.cloud.api.query.vo.HostJoinVO;
import com.cloud.api.query.vo.UserVmJoinVO;
import com.cloud.vm.VirtualMachine;

public final class UserVmJoinVOToVmConverter {

    private UserVmJoinVOToVmConverter() {
    }

    /**
     * Convert CloudStack UserVmJoinVO -> oVirt-like Vm DTO.
     *
     * @param src      UserVmJoinVO
     */
    public static Vm toVm(final UserVmJoinVO src, final Function<Long, HostJoinVO> hostResolver,
                          final Function<Long, List<DiskAttachment>> disksResolver, final Function<UserVmJoinVO, List<Nic>> nicsResolver) {
        if (src == null) {
            return null;
        }
        final String basePath = VeeamControlService.ContextPath.value();
        final Vm dst = new Vm();

        dst.setId(src.getUuid());
        dst.setName(StringUtils.firstNonBlank(src.getName(), src.getInstanceName()));
        // CloudStack doesn't really have "description" for VM; displayName is closest
        dst.setDescription(src.getDisplayName());
        dst.setHref(basePath + VmsRouteHandler.BASE_ROUTE + "/" + src.getUuid());
        dst.setStatus(mapStatus(src.getState()));
        dst.setCreationTime(src.getCreated().getTime());
        final Date lastUpdated = src.getLastUpdated() != null ? src.getLastUpdated() : src.getCreated();
        if ("down".equals(dst.getStatus())) {
            dst.setStopTime(lastUpdated.getTime());
        }
        if ("up".equals(dst.getStatus())) {
            dst.setStartTime(lastUpdated.getTime());
        }
        final Ref template = buildRef(
                basePath + ApiService.BASE_ROUTE,
                "templates",
                src.getTemplateUuid()
        );
        dst.setTemplate(template);
        dst.setOriginalTemplate(template);
        if (StringUtils.isNotBlank(src.getHostUuid())) {
            dst.setHost(buildRef(
                    basePath + ApiService.BASE_ROUTE,
                    "hosts",
                    src.getHostUuid()));

        }
        if (hostResolver != null) {
            HostJoinVO hostVo = hostResolver.apply(src.getHostId() == null ? src.getLastHostId() : src.getHostId());
            if (hostVo != null) {
                dst.setHost(buildRef(
                        basePath + ApiService.BASE_ROUTE,
                        "hosts",
                        hostVo.getUuid()));
                dst.setCluster(buildRef(
                        basePath + ApiService.BASE_ROUTE,
                        "clusters",
                        hostVo.getClusterUuid()));
            }
        }

        dst.setMemory(String.valueOf(src.getRamSize() * 1024L * 1024L));
        Cpu cpu = new Cpu();
        cpu.setArchitecture(src.getArch());
        cpu.setTopology(new Topology(src.getCpu(), 1, 1));
        dst.setCpu(cpu);
        Os os = new Os();
        os.setType(src.getGuestOsId() % 2 == 0
                ? "windows"
                : "linux");
        dst.setOs(os);
        Bios bios = new Bios();
        bios.setType("q35_secure_boot");
        dst.setBios(bios);
        dst.setType("desktop");
        dst.setOrigin("ovirt");
        dst.setStateless("false");

        if (disksResolver != null) {
            List<DiskAttachment> diskAttachments = disksResolver.apply(src.getId());
            dst.setDiskAttachments(NamedList.of("disk_attachment", diskAttachments));
        }

        if (disksResolver != null) {
            List<Nic> nics = nicsResolver.apply(src);
            dst.setNics(new Nics(nics));
        }

        dst.setActions(NamedList.of("link", List.of(
                BaseDto.getActionLink("start", dst.getHref()),
                BaseDto.getActionLink("stop", dst.getHref()),
                BaseDto.getActionLink("shutdown", dst.getHref())
        )));
        dst.setLink(List.of(
                BaseDto.getActionLink("diskattachments", dst.getHref()),
                BaseDto.getActionLink("nics", dst.getHref()),
                BaseDto.getActionLink("reporteddevices", dst.getHref()),
                BaseDto.getActionLink("snapshots", dst.getHref())
        ));
        dst.setTags(new EmptyElement());

        return dst;
    }

    public static List<Vm> toVmList(final List<UserVmJoinVO> srcList, final Function<Long, HostJoinVO> hostResolver) {
        return srcList.stream()
                .map(v -> toVm(v, hostResolver, null, null))
                .collect(Collectors.toList());
    }

    private static String mapStatus(final VirtualMachine.State state) {
        // CloudStack-ish states -> oVirt-ish up/down
        if (Arrays.asList(VirtualMachine.State.Running,
                VirtualMachine.State.Migrating, VirtualMachine.State.Restoring).contains(state)) {
            return "up";
        }
        return "down";
    }

    private static Ref buildRef(final String baseHref, final String suffix, final String id) {
        if (StringUtils.isBlank(id)) {
            return null;
        }
        return Ref.of((baseHref != null) ? (baseHref + "/" + suffix + "/" + id) : null, id);
    }
}
