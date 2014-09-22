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
package com.cloud.network.rules.dao;

import java.util.List;

import com.cloud.network.rules.PortForwardingRuleVO;
import com.cloud.utils.db.GenericDao;

public interface PortForwardingRulesDao extends GenericDao<PortForwardingRuleVO, Long> {
    List<PortForwardingRuleVO> listForApplication(long ipId);

    /**
     * Find all port forwarding rules for the ip address that have not been revoked.
     *
     * @param ip ip address
     * @return List of PortForwardingRuleVO
     */
    List<PortForwardingRuleVO> listByIpAndNotRevoked(long ipId);

    List<PortForwardingRuleVO> listByNetworkAndNotRevoked(long networkId);

    List<PortForwardingRuleVO> listByIp(long ipId);

    List<PortForwardingRuleVO> listByVm(Long vmId);

    List<PortForwardingRuleVO> listByNetwork(long networkId);

    List<PortForwardingRuleVO> listByAccount(long accountId);

    List<PortForwardingRuleVO> listByDestIpAddr(String ip4Address);

    PortForwardingRuleVO findByIdAndIp(long id, String secondaryIp);

    List<PortForwardingRuleVO> listByNetworkAndDestIpAddr(String ip4Address, long networkId);
}
