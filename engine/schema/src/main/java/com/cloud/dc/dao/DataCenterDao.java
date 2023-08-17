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
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.DataCenterVnetVO;
import com.cloud.utils.db.GenericDao;

public interface DataCenterDao extends GenericDao<DataCenterVO, Long> {

    class PrivateAllocationData {

        private String ipAddress;
        private Long macAddress;
        private Integer vlan;

        public PrivateAllocationData(final String ipAddress, final Long macAddress, final Integer vlan) {
            this.ipAddress = ipAddress;
            this.macAddress = macAddress;
            this.vlan = vlan;
        }

        public String getIpAddress() {
            return ipAddress;
        }

        public Long getMacAddress() {
            return macAddress;
        }

        public Integer getVlan() {
            return vlan;
        }
    }

    DataCenterVO findByName(String name);

    PrivateAllocationData allocatePrivateIpAddress(long id, long podId, long instanceId, String reservationId, boolean forSystemVms);

    DataCenterIpAddressVO allocatePrivateIpAddress(long id, String reservationId);

    String allocateLinkLocalIpAddress(long id, long podId, long instanceId, String reservationId);

    String allocateVnet(long dcId, long physicalNetworkId, long accountId, String reservationId, boolean canUseSystemGuestVlans);

    void releaseVnet(String vnet, long dcId, long physicalNetworkId, long accountId, String reservationId);

    void releasePrivateIpAddress(String ipAddress, long dcId, Long instanceId);

    void releasePrivateIpAddress(long nicId, String reservationId);

    void releaseLinkLocalIpAddress(String ipAddress, long dcId, Long instanceId);

    void releaseLinkLocalIpAddress(long nicId, String reservationId);

    boolean deletePrivateIpAddressByPod(long podId);

    boolean deleteLinkLocalIpAddressByPod(long podId);

    void addPrivateIpAddress(long dcId, long podId, String start, String end, boolean forSystemVms, Integer vlan);

    void addLinkLocalIpAddress(long dcId, long podId, String start, String end);

    List<DataCenterVnetVO> findVnet(long dcId, long physicalNetworkId, String vnet);

    String allocatePodVlan(long podId, long accountId);

    List<DataCenterVO> findZonesByDomainId(Long domainId);

    List<DataCenterVO> listPublicZones(String keyword);

    List<DataCenterVO> findChildZones(Object[] ids, String keyword);

    void loadDetails(DataCenterVO zone);

    void saveDetails(DataCenterVO zone);

    List<DataCenterVO> listDisabledZones();

    List<DataCenterVO> listEnabledZones();

    List<Long> listEnabledNonEdgeZoneIds();

    DataCenterVO findByToken(String zoneToken);

    DataCenterVO findByTokenOrIdOrName(String tokenIdOrName);

    int countZoneVlans(long dcId, boolean onlyCountAllocated);

    void addVnet(long dcId, long physicalNetworkId, List<String> vnets);

    void deleteVnet(long physicalNetworkId);

    List<DataCenterVnetVO> listAllocatedVnets(long physicalNetworkId);

    List<DataCenterVO> findZonesByDomainId(Long domainId, String keyword);

    List<DataCenterVO> findByKeyword(String keyword);

    List<DataCenterVO> listAllZones();
}
