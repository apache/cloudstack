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

package org.apache.cloudstack.api;

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.command.user.zone.ListZonesCmd;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.ZoneResponse;
import org.apache.cloudstack.metrics.MetricsService;
import org.apache.cloudstack.response.ZoneMetricsResponse;

import javax.inject.Inject;
import java.util.List;

@APICommand(name = "listZonesMetrics", description = "Lists zone metrics", responseObject = ZoneMetricsResponse.class,
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false,  responseView = ResponseObject.ResponseView.Full,
        since = "4.9.3", authorized = {RoleType.Admin})
public class ListZonesMetricsCmd extends ListZonesCmd {

    @Inject
    private MetricsService metricsService;

    @Override
    public void execute() {
        ListResponse<ZoneResponse> zones = _queryService.listDataCenters(this);
        final List<ZoneMetricsResponse> metricsResponses = metricsService.listZoneMetrics(zones.getResponses());
        ListResponse<ZoneMetricsResponse> response = new ListResponse<>();
        response.setResponses(metricsResponses, zones.getCount());
        response.setResponseName(getCommandName());
        setResponseObject(response);
    }

}
