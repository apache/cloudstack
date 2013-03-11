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

public interface DataCenterVnetDao extends GenericDao<DataCenterVnetVO, Long> {
    public List<DataCenterVnetVO> listAllocatedVnets(long physicalNetworkId);
    public List<DataCenterVnetVO> findVnet(long dcId, String vnet);   
    public int countZoneVlans(long dcId, boolean onlyCountAllocated);    
    public List<DataCenterVnetVO> findVnet(long dcId, long physicalNetworkId, String vnet);
    
    public void add(long dcId, long physicalNetworkId, int start, int end);
    
    public void delete(long physicalNetworkId);

    public DataCenterVnetVO take(long physicalNetworkId, long accountId, String reservationId);

    public void release(String vnet, long physicalNetworkId, long accountId, String reservationId);

}
