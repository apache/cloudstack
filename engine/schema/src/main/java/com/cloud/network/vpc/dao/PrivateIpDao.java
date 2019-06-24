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

import com.cloud.network.vpc.PrivateIpVO;
import com.cloud.utils.db.GenericDao;

public interface PrivateIpDao extends GenericDao<PrivateIpVO, Long> {

    /**
     * @param dcId
     * @param networkId
     * @param requestedIp TODO
     * @return
     */
    PrivateIpVO allocateIpAddress(long dcId, long networkId, String requestedIp);

    /**
     * @param ipAddress
     * @param networkId
     */
    void releaseIpAddress(String ipAddress, long networkId);

    /**
     * @param networkId
     * @param ip4Address
     * @return
     */
    PrivateIpVO findByIpAndSourceNetworkId(long networkId, String ip4Address);

    /**
     * @param networkId
     * @return
     */
    List<PrivateIpVO> listByNetworkId(long networkId);

    /**
     * @param ntwkId
     * @return
     */
    int countAllocatedByNetworkId(long ntwkId);

    /**
     * @param networkId
     */
    void deleteByNetworkId(long networkId);

    int countByNetworkId(long ntwkId);

    /**
     * @param vpcId
     * @param ip4Address
     * @return
     */
    PrivateIpVO findByIpAndVpcId(long vpcId, String ip4Address);

    PrivateIpVO findByIpAndSourceNetworkIdAndVpcId(long networkId, String ip4Address, long vpcId);
}
