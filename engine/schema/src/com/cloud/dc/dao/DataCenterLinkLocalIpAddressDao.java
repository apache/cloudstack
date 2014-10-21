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

import com.cloud.dc.DataCenterLinkLocalIpAddressVO;
import com.cloud.utils.db.GenericDao;

public interface DataCenterLinkLocalIpAddressDao extends GenericDao<DataCenterLinkLocalIpAddressVO, Long> {
    public DataCenterLinkLocalIpAddressVO takeIpAddress(long dcId, long podId, long instanceId, String reservationId);

    public boolean deleteIpAddressByPod(long podId);

    public void addIpRange(long dcId, long podId, String start, String end);

    public void releaseIpAddress(String ipAddress, long dcId, long instanceId);

    public void releaseIpAddress(long nicId, String reservationId);

    public List<DataCenterLinkLocalIpAddressVO> listByPodIdDcId(long podId, long dcId);

    public int countIPs(long podId, long dcId, boolean onlyCountAllocated);
}
