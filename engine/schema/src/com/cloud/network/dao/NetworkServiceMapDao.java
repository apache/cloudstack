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

import com.cloud.network.Network.Provider;
import com.cloud.network.Network.Service;
import com.cloud.utils.db.GenericDao;

/**
 * NetworkServiceDao deals with searches and operations done on the
 * ntwk_service_map table.
 *
 */
public interface NetworkServiceMapDao extends GenericDao<NetworkServiceMapVO, Long> {
    boolean areServicesSupportedInNetwork(long networkId, Service... services);

    boolean canProviderSupportServiceInNetwork(long networkId, Service service, Provider provider);

    List<NetworkServiceMapVO> getServicesInNetwork(long networkId);

    String getProviderForServiceInNetwork(long networkid, Service service);

    void deleteByNetworkId(long networkId);

    List<String> getDistinctProviders(long networkId);

    String isProviderForNetwork(long networkId, Provider provider);

    List<String> getProvidersForServiceInNetwork(long networkId, Service service);
}
