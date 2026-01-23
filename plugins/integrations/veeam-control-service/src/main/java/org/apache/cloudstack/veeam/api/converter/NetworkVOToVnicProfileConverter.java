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
import org.apache.cloudstack.veeam.api.DataCentersRouteHandler;
import org.apache.cloudstack.veeam.api.NetworksRouteHandler;
import org.apache.cloudstack.veeam.api.dto.Ref;
import org.apache.cloudstack.veeam.api.dto.VnicProfile;

import com.cloud.api.query.vo.DataCenterJoinVO;
import com.cloud.network.dao.NetworkVO;

public class NetworkVOToVnicProfileConverter {
    public static VnicProfile toVnicProfile(final NetworkVO vo, final Function<Long, DataCenterJoinVO> dcResolver) {
        final VnicProfile vnicProfile = new VnicProfile();

        final String networkUuid = vo.getUuid();
        vnicProfile.setId(networkUuid);
        final String basePath = VeeamControlService.ContextPath.value();
        vnicProfile.setHref(basePath + NetworksRouteHandler.BASE_ROUTE + "/" + networkUuid);
        vnicProfile.setId(networkUuid);
        String name = vo.getName() != null ? vo.getName() : vo.getTrafficType().name() + "-" + networkUuid;
        vnicProfile.setName(name);
        vnicProfile.setNetwork(Ref.of(basePath + NetworksRouteHandler.BASE_ROUTE + "/" + networkUuid, networkUuid));
        vnicProfile.setDescription(vo.getDisplayText());

        // zone -> oVirt datacenter ref
        if (dcResolver != null) {
            final DataCenterJoinVO dc = dcResolver.apply(vo.getDataCenterId());
            if (dc != null) {
                final String dcUuid = dc.getUuid();
                if (dcUuid != null && !dcUuid.isEmpty()) {
                    vnicProfile.setDataCenter(Ref.of(basePath + DataCentersRouteHandler.BASE_ROUTE + "/" + dcUuid, dcUuid));
                }
            }
        }
        return vnicProfile;
    }

    public static List<VnicProfile> toVnicProfileList(final List<NetworkVO> vos, final Function<Long, DataCenterJoinVO> dcResolver) {
        return vos.stream()
                .map(vo -> toVnicProfile(vo, dcResolver))
                .collect(Collectors.toList());
    }
}
