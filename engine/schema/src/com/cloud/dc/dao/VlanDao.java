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

import com.cloud.dc.Vlan;
import com.cloud.dc.Vlan.VlanType;
import com.cloud.dc.VlanVO;
import com.cloud.utils.db.GenericDao;

public interface VlanDao extends GenericDao<VlanVO, Long> {

    VlanVO findByZoneAndVlanId(long zoneId, String vlanId);

    List<VlanVO> listByZone(long zoneId);

    List<VlanVO> listByType(Vlan.VlanType vlanType);

    List<VlanVO> listByZoneAndType(long zoneId, Vlan.VlanType vlanType);

    List<VlanVO> listVlansForPod(long podId);

    List<VlanVO> listVlansForPodByType(long podId, Vlan.VlanType vlanType);

    void addToPod(long podId, long vlanDbId);

    List<VlanVO> listVlansForAccountByType(Long zoneId, long accountId, VlanType vlanType);

    boolean zoneHasDirectAttachUntaggedVlans(long zoneId);

    List<VlanVO> listZoneWideVlans(long zoneId, VlanType vlanType, String vlanId);

    List<VlanVO> searchForZoneWideVlans(long dcId, String vlanType, String vlanId);

    List<VlanVO> listVlansByNetworkId(long networkId);

    List<VlanVO> listVlansByPhysicalNetworkId(long physicalNetworkId);

    List<VlanVO> listZoneWideNonDedicatedVlans(long zoneId);

    List<VlanVO> listVlansByNetworkIdAndGateway(long networkid, String gateway);

    List<VlanVO> listDedicatedVlans(long accountId);
}
