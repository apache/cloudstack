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
package org.apache.cloudstack.network.dao;

import com.cloud.utils.db.GenericDao;
import org.apache.cloudstack.network.BgpPeerNetworkMapVO;

import java.util.List;


public interface BgpPeerNetworkMapDao extends GenericDao<BgpPeerNetworkMapVO, Long> {

    void persist(long networkId, List<Long> bgpPeerIds);

    BgpPeerNetworkMapVO findByBgpPeerIdAndNetworkId(long bgpPeerId, long networkId);

    List<BgpPeerNetworkMapVO> listByBgpPeerId(long bgpPeerId);

    List<BgpPeerNetworkMapVO> listByNetworkId(long networkId);

    List<BgpPeerNetworkMapVO> listUsedByOtherDomains(long bgpPeerId, Long domainId);

    List<BgpPeerNetworkMapVO> listUsedByOtherAccounts(long bgpPeerId, Long accountId);

    int removeByNetworkId(long networkId);
}
