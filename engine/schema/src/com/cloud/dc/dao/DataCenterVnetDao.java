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
package com.cloud.dc.dao;

import java.util.List;

import com.cloud.dc.DataCenterVnetVO;
import com.cloud.utils.db.GenericDao;
import com.cloud.utils.db.TransactionLegacy;

public interface DataCenterVnetDao extends GenericDao<DataCenterVnetVO, Long> {
    public List<DataCenterVnetVO> listAllocatedVnets(long physicalNetworkId);

    public List<DataCenterVnetVO> listAllocatedVnetsInRange(long dcId, long physicalNetworkId, Integer start, Integer end);

    public List<DataCenterVnetVO> findVnet(long dcId, String vnet);

    public int countZoneVlans(long dcId, boolean onlyCountAllocated);

    public List<DataCenterVnetVO> findVnet(long dcId, long physicalNetworkId, String vnet);

    public void add(long dcId, long physicalNetworkId, List<String> vnets);

    public void delete(long physicalNetworkId);

    public void deleteVnets(TransactionLegacy txn, long dcId, long physicalNetworkId, List<String> vnets);

    public void lockRange(long dcId, long physicalNetworkId, Integer start, Integer end);

    public DataCenterVnetVO take(long physicalNetworkId, long accountId, String reservationId, List<Long> vlanDbIds);

    public void release(String vnet, long physicalNetworkId, long accountId, String reservationId);

    public void releaseDedicatedGuestVlans(Long dedicatedGuestVlanRangeId);

    public int countVnetsAllocatedToAccount(long dcId, long accountId);

    public int countVnetsDedicatedToAccount(long dcId, long accountId);

    List<String> listVnetsByPhysicalNetworkAndDataCenter(long dcId, long physicalNetworkId);

    int countAllocatedVnets(long physicalNetworkId);
}
