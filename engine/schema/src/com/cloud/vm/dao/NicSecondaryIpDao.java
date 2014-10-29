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

public interface NicSecondaryIpDao extends GenericDao<NicSecondaryIpVO, Long> {
    List<NicSecondaryIpVO> listByVmId(long instanceId);

    List<String> listSecondaryIpAddressInNetwork(long networkConfigId);

    List<NicSecondaryIpVO> listByNetworkId(long networkId);

    NicSecondaryIpVO findByInstanceIdAndNetworkId(long networkId, long instanceId);

    //    void removeNicsForInstance(long instanceId);
    //    void removeSecondaryIpForNic(long nicId);

    NicSecondaryIpVO findByIp4AddressAndNetworkId(String ip4Address, long networkId);

    /**
     * @param networkId
     * @param instanceId
     * @return
     */

    List<NicSecondaryIpVO> getSecondaryIpAddressesForVm(long vmId);

    List<NicSecondaryIpVO> listByNicId(long nicId);

    List<NicSecondaryIpVO> listByNicIdAndVmid(long nicId, long vmId);

    NicSecondaryIpVO findByIp4AddressAndNicId(String ip4Address, long nicId);

    NicSecondaryIpVO findByIp4AddressAndNetworkIdAndInstanceId(long networkId, Long vmId, String vmIp);

    List<String> getSecondaryIpAddressesForNic(long nicId);

    Long countByNicId(long nicId);
}
