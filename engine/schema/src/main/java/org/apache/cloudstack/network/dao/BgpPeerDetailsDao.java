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

import org.apache.cloudstack.network.BgpPeer;
import org.apache.cloudstack.network.BgpPeerDetailsVO;
import org.apache.cloudstack.resourcedetail.ResourceDetailsDao;

import java.util.List;
import java.util.Map;

public interface BgpPeerDetailsDao extends ResourceDetailsDao<BgpPeerDetailsVO> {
    Map<BgpPeer.Detail, String> getBgpPeerDetails(long bgpPeerId);
    String getDetail(long offeringId, BgpPeer.Detail detailName);
    List<Long> findDomainIds(final long resourceId);
    List<Long> findZoneIds(final long resourceId);

    int removeByBgpPeerId(long bgpPeerId);
}
