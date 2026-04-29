//Licensed to the Apache Software Foundation (ASF) under one
//or more contributor license agreements.  See the NOTICE file
//distributed with this work for additional information
//regarding copyright ownership.  The ASF licenses this file
//to you under the Apache License, Version 2.0 (the
//"License"); you may not use this file except in compliance
//with the License.  You may obtain a copy of the License at
//
//http://www.apache.org/licenses/LICENSE-2.0
//
//Unless required by applicable law or agreed to in writing,
//software distributed under the License is distributed on an
//"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
//KIND, either express or implied.  See the License for the
//specific language governing permissions and limitations
//under the License.
package com.cloud.gpu.dao;

import java.util.HashMap;
import java.util.List;

import com.cloud.agent.api.VgpuTypesInfo;
import com.cloud.gpu.VGPUTypesVO;
import com.cloud.utils.db.GenericDao;

public interface VGPUTypesDao extends GenericDao<VGPUTypesVO, Long> {

    /**
     * List zonewide/podwide/clusterwide GPU card capacities.
     * @param zoneId
     * @param podId
     * @param clusterId
     * @return Custom Query result
     */
    List<VgpuTypesInfo> listGPUCapacities(Long zoneId, Long podId, Long clusterId);

    /**
     * Find VGPU types by group Id
     * @param groupId of the GPU group
     * @return list of VGPUTypesVO
     */
    List<VGPUTypesVO> listByGroupId(long groupId);

    /**
     * Find VGPU type by group Id and VGPU type
     * @param groupId of the GPU group
     * @param vgpuType name of VGPU type
     * @return VGPUTypesVO
     */
    VGPUTypesVO findByGroupIdVGPUType(long groupId, String vgpuType);

    /**
     * Save the list of enabled VGPU types
     * @param hostId the host
     * @param groupDetails with enabled VGPU types
     */
    void persist(long hostId, HashMap<String, HashMap<String, VgpuTypesInfo>> groupDetails);
}
