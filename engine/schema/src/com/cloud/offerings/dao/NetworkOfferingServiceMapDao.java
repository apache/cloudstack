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
package com.cloud.offerings.dao;

import java.util.List;

import com.cloud.network.Network.Provider;
import com.cloud.network.Network.Service;
import com.cloud.offerings.NetworkOfferingServiceMapVO;
import com.cloud.utils.db.GenericDao;

/**
 * NetworkOfferingServiceDao deals with searches and operations done on the
 * ntwk_offering_service_map table.
 *
 */
public interface NetworkOfferingServiceMapDao extends GenericDao<NetworkOfferingServiceMapVO, Long> {
    boolean areServicesSupportedByNetworkOffering(long networkOfferingId, Service... services);

    boolean canProviderSupportServiceInNetworkOffering(long networkOfferingId, Service service, Provider provider);

    List<NetworkOfferingServiceMapVO> listByNetworkOfferingId(long networkOfferingId);

    void deleteByOfferingId(long networkOfferingId);

    List<String> listProvidersForServiceForNetworkOffering(long networkOfferingId, Service service);

    boolean isProviderForNetworkOffering(long networkOfferingId, Provider provider);

    List<String> listServicesForNetworkOffering(long networkOfferingId);

    List<String> getDistinctProviders(long offId);
}
