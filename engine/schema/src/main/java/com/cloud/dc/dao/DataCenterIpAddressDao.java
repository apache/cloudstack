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

import com.cloud.dc.DataCenterIpAddressVO;
import com.cloud.utils.db.GenericDao;

public interface DataCenterIpAddressDao extends GenericDao<DataCenterIpAddressVO, Long> {

    DataCenterIpAddressVO takeIpAddress(long dcId, long podId, long instanceId, String reservationId, boolean forSystemVms);

    DataCenterIpAddressVO takeDataCenterIpAddress(long dcId, String reservationId);

    void addIpRange(long dcId, long podId, String start, String end, boolean forSystemVms, Integer vlan);

    void releaseIpAddress(String ipAddress, long dcId, Long instanceId);

    void releaseIpAddress(long nicId, String reservationId);

    void releaseIpAddress(long nicId);

    boolean mark(long dcId, long podId, String ip);

    List<DataCenterIpAddressVO> listByPodIdDcIdIpAddress(long podId, long dcId, String ipAddress);

    List<DataCenterIpAddressVO> listByPodIdDcId(long podId, long dcId);

    List<DataCenterIpAddressVO> listIpAddressUsage(final long podId, final long dcId, final boolean onlyListAllocated);

    int countIPs(long podId, long dcId, boolean onlyCountAllocated);

    int countIPs(long dcId, boolean onlyCountAllocated);

    int countIpAddressUsage(final String ipAddress, final long podId, final long dcId, final boolean onlyCountAllocated);

    boolean deleteIpAddressByPod(long podId);

    void releasePodIpAddress(long id);

    boolean deleteIpAddressByPodDc(String ipAddress, long podId, long dcId);
}
