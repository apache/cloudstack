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

import com.cloud.utils.db.GenericDao;

public interface NetworkExternalFirewallDao extends GenericDao<NetworkExternalFirewallVO, Long> {

    /**
     * find the network to firewall device mapping corresponding to a network
     * @param lbDeviceId guest network Id
     * @return return NetworkExternalFirewallDao for the guest network
     */
    NetworkExternalFirewallVO findByNetworkId(long networkId);

    /**
     * list all network to firewall device mappings corresponding to a firewall device Id
     * @param lbDeviceId firewall device Id
     * @return list of NetworkExternalFirewallVO mappings corresponding to the networks mapped to the firewall device
     */
    List<NetworkExternalFirewallVO> listByFirewallDeviceId(long lbDeviceId);
}
