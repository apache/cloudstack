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
import org.apache.cloudstack.veeam.api.DataCentersRouteHandler;
import org.apache.cloudstack.veeam.api.NetworksRouteHandler;
import org.apache.cloudstack.veeam.api.dto.NamedList;
import org.apache.cloudstack.veeam.api.dto.Network;
import org.apache.cloudstack.veeam.api.dto.Ref;

import com.cloud.api.query.vo.DataCenterJoinVO;
import com.cloud.network.dao.NetworkVO;

public class NetworkVOToNetworkConverter {
    public static Network toNetwork(final NetworkVO vo, final Function<Long, DataCenterJoinVO> dcResolver) {
        final Network dto = new Network();

        final String networkUuid = vo.getUuid();
        dto.setId(networkUuid);
        final String basePath = VeeamControlService.ContextPath.value();
        dto.setHref(basePath + NetworksRouteHandler.BASE_ROUTE + "/" + networkUuid);

        String name = vo.getName() != null ? vo.getName() : vo.getTrafficType().name() + "-" + networkUuid;
        dto.setName(name);
        dto.setDescription(vo.getDisplayText());
        dto.setComment("");

        dto.setMtu(String.valueOf(vo.getPrivateMtu() != null ? vo.getPrivateMtu() : 0));
        dto.setPortIsolation("false");
        dto.setStp("false");

        dto.setUsages(NamedList.of("usage", List.of("vm")));

        // Best-effort mapping for vdsm_name
        dto.setVdsmName(dto.getName());

        // zone -> oVirt datacenter ref
        if (dcResolver != null) {
            final DataCenterJoinVO dc = dcResolver.apply(vo.getDataCenterId());
            if (dc != null) {
                final String dcUuid = dc.getUuid();
                if (dcUuid != null && !dcUuid.isEmpty()) {
                    dto.setDataCenter(Ref.of(basePath + DataCentersRouteHandler.BASE_ROUTE + "/" + dcUuid, dcUuid));
                }
            }
        }

        dto.setLink(Collections.emptyList());

        return dto;
    }

    public static List<Network> toNetworkList(final List<? extends NetworkVO> vos,
            final Function<Long, DataCenterJoinVO> dcResolver) {
        return vos.stream()
                .map(vo -> toNetwork(vo, dcResolver))
                .collect(Collectors.toList());
    }
}
