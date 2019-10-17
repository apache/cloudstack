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
package org.apache.cloudstack.affinity.dao;

import java.util.List;

import org.apache.cloudstack.affinity.AffinityGroupVMMapVO;

import com.cloud.utils.Pair;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GenericDao;

public interface AffinityGroupVMMapDao extends GenericDao<AffinityGroupVMMapVO, Long> {

    List<AffinityGroupVMMapVO> listByInstanceId(long instanceId);

    Pair<List<AffinityGroupVMMapVO>, Integer> listByInstanceId(long instanceId, Filter filter);

    List<AffinityGroupVMMapVO> listByAffinityGroup(long affinityGroupId);

    List<Long> listVmIdsByAffinityGroup(long affinityGroupId);

    AffinityGroupVMMapVO findByVmIdGroupId(long instanceId, long affinityGroupId);

    long countAffinityGroupsForVm(long instanceId);

    int deleteVM(long instanceId);

    List<AffinityGroupVMMapVO> findByVmIdType(long instanceId, String type);

    void updateMap(Long vmId, List<Long> affinityGroupIds);

    List<Long> listAffinityGroupIdsByVmId(long instanceId);
}
