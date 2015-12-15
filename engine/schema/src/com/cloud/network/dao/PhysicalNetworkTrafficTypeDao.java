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
package com.cloud.network.dao;

import java.util.List;

import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.network.Networks.TrafficType;
import com.cloud.utils.Pair;
import com.cloud.utils.db.GenericDao;

public interface PhysicalNetworkTrafficTypeDao extends GenericDao<PhysicalNetworkTrafficTypeVO, Long> {
    Pair<List<PhysicalNetworkTrafficTypeVO>, Integer> listAndCountBy(long physicalNetworkId);

    boolean isTrafficTypeSupported(long physicalNetworkId, TrafficType trafficType);

    String getNetworkTag(long physicalNetworkId, TrafficType trafficType, HypervisorType hType);

    PhysicalNetworkTrafficTypeVO findBy(long physicalNetworkId, TrafficType trafficType);

    void deleteTrafficTypes(long physicalNetworkId);
}
