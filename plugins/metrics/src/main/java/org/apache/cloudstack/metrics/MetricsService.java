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

package org.apache.cloudstack.metrics;

import com.cloud.utils.Pair;
import com.cloud.utils.component.PluggableService;

import org.apache.cloudstack.api.ListVMsUsageHistoryCmd;
import org.apache.cloudstack.api.response.ClusterResponse;
import org.apache.cloudstack.api.response.HostResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.StoragePoolResponse;
import org.apache.cloudstack.api.response.UserVmResponse;
import org.apache.cloudstack.api.response.VolumeResponse;
import org.apache.cloudstack.api.response.ZoneResponse;
import org.apache.cloudstack.response.ClusterMetricsResponse;
import org.apache.cloudstack.response.HostMetricsResponse;
import org.apache.cloudstack.response.InfrastructureResponse;
import org.apache.cloudstack.response.StoragePoolMetricsResponse;
import org.apache.cloudstack.response.VmMetricsResponse;
import org.apache.cloudstack.response.VmMetricsStatsResponse;
import org.apache.cloudstack.response.VolumeMetricsResponse;
import org.apache.cloudstack.response.ZoneMetricsResponse;

import java.util.List;

public interface MetricsService extends PluggableService {
    InfrastructureResponse listInfrastructure();

    ListResponse<VmMetricsStatsResponse> searchForVmMetricsStats(ListVMsUsageHistoryCmd cmd);
    List<VolumeMetricsResponse> listVolumeMetrics(List<VolumeResponse> volumeResponses);
    List<VmMetricsResponse> listVmMetrics(List<UserVmResponse> vmResponses);
    List<StoragePoolMetricsResponse> listStoragePoolMetrics(List<StoragePoolResponse> poolResponses);
    List<HostMetricsResponse> listHostMetrics(List<HostResponse> poolResponses);
    List<ClusterMetricsResponse> listClusterMetrics(Pair<List<ClusterResponse>, Integer> clusterResponses);
    List<ZoneMetricsResponse> listZoneMetrics(List<ZoneResponse> poolResponses);
}
