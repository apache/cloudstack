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

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.cloudstack.veeam.VeeamControlService;
import org.apache.cloudstack.veeam.api.VnicProfilesRouteHandler;
import org.apache.cloudstack.veeam.api.dto.Ip;
import org.apache.cloudstack.veeam.api.dto.Ips;
import org.apache.cloudstack.veeam.api.dto.Mac;
import org.apache.cloudstack.veeam.api.dto.Nic;
import org.apache.cloudstack.veeam.api.dto.Ref;
import org.apache.cloudstack.veeam.api.dto.ReportedDevice;
import org.apache.cloudstack.veeam.api.dto.ReportedDevices;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import com.cloud.network.dao.NetworkVO;
import com.cloud.vm.NicVO;

public class NicVOToNicConverter {

    public static Nic toNic(final NicVO vo, final String vmUuid, final Function<Long, NetworkVO> networkResolver) {
        final String basePath = VeeamControlService.ContextPath.value();
        final Nic nic = new Nic();
        nic.setId(vo.getUuid());
        nic.setName(vo.getReserver());
        Mac mac = new Mac();
        mac.setAddress(vo.getMacAddress());
        nic.setMac(mac);
        nic.setLinked(true);
        nic.setPlugged(true);
        if (StringUtils.isBlank(vmUuid)) {
            nic.setVm(Ref.of(basePath + "/vms/" + vmUuid, vmUuid));
            nic.setHref(nic.getVm().href + "/nics/" + vo.getUuid());
        }
        nic.setInterfaceType("virtio");
        ReportedDevice device = getReportedDevice(vo, mac, nic.getVm());
        nic.setReportedDevices(new ReportedDevices(List.of(device)));
        if (networkResolver != null) {
            final NetworkVO network = networkResolver.apply(vo.getNetworkId());
            if (network != null) {
                nic.setVnicProfile(Ref.of(basePath + VnicProfilesRouteHandler.BASE_ROUTE + "/" + network.getUuid(), network.getUuid()));
            }
        }
        return nic;
    }

    @NotNull
    private static ReportedDevice getReportedDevice(NicVO vo, Mac mac, Ref vm) {
        ReportedDevice device = new ReportedDevice();
        device.setType("network");
        device.setId(vo.getUuid());
        device.setName("eth0");
        device.setMac(mac);
        Ip ip = new Ip();
        if (vo.getIPv4Address() != null) {
            ip.setAddress(vo.getIPv4Address());
            ip.setGateway(vo.getIPv4Gateway());
            ip.setVersion("v4");
        } else if (vo.getIPv6Address() != null) {
            ip.setAddress(vo.getIPv6Address());
            ip.setGateway(vo.getIPv6Gateway());
            ip.setVersion("v6");
        }
        device.setIps(new Ips(List.of(ip)));
        device.setVm(vm);
        return device;
    }

    public static List<Nic> toNicList(final List<NicVO> vos, final String vmUuid, final Function<Long, NetworkVO> networkResolver) {
        return vos.stream()
                .map(vo -> toNic(vo, vmUuid, networkResolver))
                .collect(Collectors.toList());
    }
}
