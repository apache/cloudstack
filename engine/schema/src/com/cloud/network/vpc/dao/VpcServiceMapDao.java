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
package com.cloud.network.vpc.dao;

import java.util.List;

import com.cloud.network.Network.Provider;
import com.cloud.network.Network.Service;
import com.cloud.network.dao.NetworkServiceMapVO;
import com.cloud.network.vpc.VpcServiceMapVO;
import com.cloud.utils.db.GenericDao;

/**
 * VpcServiceMapDao deals with searches and operations done on the
 * vpc_service_map table.
 *
 */
public interface VpcServiceMapDao extends GenericDao<VpcServiceMapVO, Long> {
    boolean areServicesSupportedInVpc(long vpcId, Service... services);

    boolean canProviderSupportServiceInVpc(long vpcId, Service service, Provider provider);

    List<NetworkServiceMapVO> getServicesInVpc(long vpcId);

    String getProviderForServiceInVpc(long vpcId, Service service);

    void deleteByVpcId(long vpcId);

    List<String> getDistinctProviders(long vpcId);

    String isProviderForVpc(long vpcId, Provider provider);
}