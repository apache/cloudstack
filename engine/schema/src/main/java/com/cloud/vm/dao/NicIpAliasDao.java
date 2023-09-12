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
package com.cloud.vm.dao;

import java.util.List;

import com.cloud.utils.db.GenericDao;
import com.cloud.vm.NicIpAlias;

public interface NicIpAliasDao extends GenericDao<NicIpAliasVO, Long> {
    List<NicIpAliasVO> listByVmId(long instanceId);

    List<String> listAliasIpAddressInNetwork(long networkConfigId);

    List<NicIpAliasVO> listByNetworkId(long networkId);

    NicIpAliasVO findByInstanceIdAndNetworkId(long networkId, long instanceId);

    NicIpAliasVO findByIp4AddressAndNetworkId(String ip4Address, long networkId);

    /**
     * @param networkId
     * @param instanceId
     * @return
     */

    List<NicIpAliasVO> getAliasIpForVm(long vmId);

    List<NicIpAliasVO> listByNicId(long nicId);

    List<NicIpAliasVO> listByNicIdAndVmid(long nicId, long vmId);

    NicIpAliasVO findByIp4AddressAndNicId(String ip4Address, long nicId);

    NicIpAliasVO findByIp4AddressAndNetworkIdAndInstanceId(long networkId, Long vmId, String vmIp);

    List<String> getAliasIpAddressesForNic(long nicId);

    Integer countAliasIps(long nicId);

    public NicIpAliasVO findByIp4AddressAndVmId(String ip4Address, long vmId);

    NicIpAliasVO findByGatewayAndNetworkIdAndState(String gateway, long networkId, NicIpAlias.State state);

    List<NicIpAliasVO> listByNetworkIdAndState(long networkId, NicIpAlias.State state);

    int moveIpAliases(long fromNicId, long toNicId);
}
