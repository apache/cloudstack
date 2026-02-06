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
import org.apache.cloudstack.veeam.api.JobsRouteHandler;
import org.apache.cloudstack.veeam.api.VmsRouteHandler;
import org.apache.cloudstack.veeam.api.dto.Actions;
import org.apache.cloudstack.veeam.api.dto.Bios;
import org.apache.cloudstack.veeam.api.dto.Cpu;
import org.apache.cloudstack.veeam.api.dto.DiskAttachment;
import org.apache.cloudstack.veeam.api.dto.DiskAttachments;
import org.apache.cloudstack.veeam.api.dto.EmptyElement;
import org.apache.cloudstack.veeam.api.dto.Link;
import org.apache.cloudstack.veeam.api.dto.Nic;
import org.apache.cloudstack.veeam.api.dto.Nics;
import org.apache.cloudstack.veeam.api.dto.Os;
import org.apache.cloudstack.veeam.api.dto.Ref;
import org.apache.cloudstack.veeam.api.dto.Topology;
import org.apache.cloudstack.veeam.api.dto.Vm;
import org.apache.cloudstack.veeam.api.dto.VmAction;
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

        dst.id = src.getUuid();
        dst.name = StringUtils.firstNonBlank(src.getName(), src.getInstanceName());
        // CloudStack doesn't really have "description" for VM; displayName is closest
        dst.description = src.getDisplayName();
        dst.href = basePath + VmsRouteHandler.BASE_ROUTE + "/" + src.getUuid();
        dst.status = mapStatus(src.getState());
        final Date lastUpdated = src.getLastUpdated() != null ? src.getLastUpdated() : src.getCreated();
        if ("down".equals(dst.status)) {
            dst.stopTime = lastUpdated.getTime();
        }
        if ("up".equals(dst.status)) {
            dst.setStartTime(lastUpdated.getTime());
        }
        final Ref template = buildRef(
                basePath + ApiService.BASE_ROUTE,
                "templates",
                src.getTemplateUuid()
        );
        dst.template = template;
        dst.originalTemplate = template;
        if (StringUtils.isNotBlank(src.getHostUuid())) {
            dst.host = buildRef(
                    basePath + ApiService.BASE_ROUTE,
                    "hosts",
                    src.getHostUuid());

        }
        if (hostResolver != null) {
            HostJoinVO hostVo = hostResolver.apply(src.getHostId() == null ? src.getLastHostId() : src.getHostId());
            if (hostVo != null) {
                dst.host = buildRef(
                        basePath + ApiService.BASE_ROUTE,
                        "hosts",
                        hostVo.getUuid());
                dst.cluster = buildRef(
                        basePath + ApiService.BASE_ROUTE,
                        "clusters",
                        hostVo.getClusterUuid());
            }
        }

        dst.memory = src.getRamSize() * 1024L * 1024L;

        dst.cpu = new Cpu(src.getArch(), new Topology(src.getCpu(), 1, 1));
        dst.os = new Os();
        dst.os.type = src.getGuestOsId() % 2 == 0
                ? "windows"
                : "linux";
        dst.bios = new Bios();
        dst.bios.type = "q35_secure_boot";
        dst.type = "desktop";
        dst.origin = "ovirt";

        if (disksResolver != null) {
            List<DiskAttachment> diskAttachments = disksResolver.apply(src.getId());
            dst.setDiskAttachments(new DiskAttachments(diskAttachments));
        }

        if (disksResolver != null) {
            List<Nic> nics = nicsResolver.apply(src);
            dst.setNics(new Nics(nics));
        }

        dst.actions = new Actions(List.of(
                new Link("start", dst.href + "/start"),
                new Link("stop", dst.href + "/stop"),
                new Link("shutdown", dst.href + "/shutdown")
        ));
        dst.link = List.of(
                new Link("diskattachments",
                        dst.href + "/diskattachments"),
                new Link("nics",
                        dst.href + "/nics"),
                new Link("snapshots",
                        dst.href + "/snapshots")
        );
        dst.tags = new EmptyElement();

        return dst;
    }

    public static List<Vm> toVmList(final List<UserVmJoinVO> srcList, final Function<Long, HostJoinVO> hostResolver) {
        return srcList.stream()
                .map(v -> toVm(v, hostResolver, null, null))
                .collect(Collectors.toList());
    }

    public static VmAction toVmAction(final UserVmJoinVO vm) {
        VmAction action = new VmAction();
        final String basePath = VeeamControlService.ContextPath.value();
        action.setVm(toVm(vm, null, null, null));
        action.setJob(Ref.of(basePath + JobsRouteHandler.BASE_ROUTE + vm.getUuid(), vm.getUuid()));
        action.setStatus("complete");
        return action;
    }

    private static String mapStatus(final VirtualMachine.State state) {
        if (state == null) {
            return null;
        }

        // CloudStack-ish states -> oVirt-ish up/down
        if (Arrays.asList(VirtualMachine.State.Running, VirtualMachine.State.Starting,
                VirtualMachine.State.Migrating, VirtualMachine.State.Restoring).contains(state)) {
            return "up";
        }
        if (Arrays.asList(VirtualMachine.State.Stopped, VirtualMachine.State.Stopping,
                VirtualMachine.State.Shutdown, VirtualMachine.State.Error,
                VirtualMachine.State.Expunging).contains(state)) {
            return "down";
        }
        return null;
    }

    private static Ref buildRef(final String baseHref, final String suffix, final String id) {
        if (StringUtils.isBlank(id)) {
            return null;
        }
        final Ref r = new Ref();
        r.id = id;
        r.href = (baseHref != null) ? (baseHref + "/" + suffix + "/" + id) : null;
        return r;
    }
}
