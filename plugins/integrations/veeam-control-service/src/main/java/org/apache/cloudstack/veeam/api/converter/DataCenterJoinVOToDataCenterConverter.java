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
import java.util.List;
import java.util.stream.Collectors;

import org.apache.cloudstack.veeam.VeeamControlService;
import org.apache.cloudstack.veeam.api.DataCentersRouteHandler;
import org.apache.cloudstack.veeam.api.dto.DataCenter;
import org.apache.cloudstack.veeam.api.dto.Link;
import org.apache.cloudstack.veeam.api.dto.Ref;
import org.apache.cloudstack.veeam.api.dto.SupportedVersions;
import org.apache.cloudstack.veeam.api.dto.Version;

import com.cloud.api.query.vo.DataCenterJoinVO;
import com.cloud.org.Grouping;

public class DataCenterJoinVOToDataCenterConverter {
    public static DataCenter toDataCenter(final DataCenterJoinVO zone) {
        final String id = zone.getUuid();
        final String basePath = VeeamControlService.ContextPath.value();
        final String href = basePath + DataCentersRouteHandler.BASE_ROUTE + DataCentersRouteHandler.BASE_ROUTE + "/" + id;

        final DataCenter dc = new DataCenter();

        // ---- Identity ----
        dc.setId(id);
        dc.setHref(href);
        dc.setName(zone.getName());
        dc.setDescription(zone.getDescription());

        // ---- State ----
        dc.setStatus(Grouping.AllocationState.Enabled.equals(zone.getAllocationState()) ? "up" : "down");
        dc.setLocal("false");
        dc.setQuotaMode("disabled");
        dc.setStorageFormat("v5");

        // ---- Versions (static but valid) ----
        final Version v48 = new Version();
        v48.setMajor(4);
        v48.setMinor(8);
        dc.setVersion(v48);
        dc.setSupportedVersions(new SupportedVersions(List.of(v48)));

        // ---- mac_pool (static placeholder) ----
        dc.setMacPool(Ref.of(basePath + "/macpools/default", "default"));

        // ---- Related links ----
        dc.link = Arrays.asList(
                Link.of(href + "/clusters", "clusters"),
                Link.of(href + "/networks", "networks"),
                Link.of(href + "/storagedomains", "storagedomains")
        );

        return dc;
    }

    public static List<DataCenter> toDCList(final List<DataCenterJoinVO> srcList) {
        return srcList.stream()
                .map(DataCenterJoinVOToDataCenterConverter::toDataCenter)
                .collect(Collectors.toList());
    }
}
