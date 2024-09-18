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

import java.util.List;

import com.cloud.utils.db.GenericDao;
import org.apache.cloudstack.network.Ipv4GuestSubnetNetworkMap;
import org.apache.cloudstack.network.Ipv4GuestSubnetNetworkMapVO;

public interface Ipv4GuestSubnetNetworkMapDao extends GenericDao<Ipv4GuestSubnetNetworkMapVO, Long> {
    List<Ipv4GuestSubnetNetworkMapVO> listByParent(long parentId);
    List<Ipv4GuestSubnetNetworkMapVO> listUsedByParent(long parentId);
    List<Ipv4GuestSubnetNetworkMapVO> listUsedByOtherDomains(long parentId, Long domainId);
    List<Ipv4GuestSubnetNetworkMapVO> listUsedByOtherAccounts(long parentId, Long accountId);
    Ipv4GuestSubnetNetworkMapVO findFirstAvailable(long parentId, long cidrSize);
    Ipv4GuestSubnetNetworkMapVO findByNetworkId(long networkId);
    Ipv4GuestSubnetNetworkMapVO findByVpcId(long vpcId);
    Ipv4GuestSubnetNetworkMapVO findBySubnet(String subnet);
    List<Ipv4GuestSubnetNetworkMapVO> findSubnetsInStates(Ipv4GuestSubnetNetworkMap.State... states);
    void deleteByParentId(long parentId);
    List<Ipv4GuestSubnetNetworkMapVO> listAllNoParent();
}
