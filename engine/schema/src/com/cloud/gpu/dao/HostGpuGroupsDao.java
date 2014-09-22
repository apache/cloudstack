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
package com.cloud.gpu.dao;

import java.util.List;

import com.cloud.gpu.HostGpuGroupsVO;
import com.cloud.utils.db.GenericDao;

public interface HostGpuGroupsDao extends GenericDao<HostGpuGroupsVO, Long> {

    /**
     * Find host device by hostId and groupName
     * @param hostId the host
     * @param groupName GPU group
     * @return HostGpuGroupsVO
     */
    HostGpuGroupsVO findByHostIdGroupName(long hostId, String groupName);

    /**
     * List all the host Ids, that are GPU enabled.
     * @return list of hostIds
     */
    List<Long> listHostIds();

    /**
     * Return a list by hostId.
     * @param hostId the host
     * @return HostGpuGroupsVO
     */
    List<HostGpuGroupsVO> listByHostId(long hostId);

    /**
     * Delete entries by hostId.
     * @param hostId the host
     */
    void deleteGpuEntries(long hostId);

    /**
     * Save the list of GPU groups belonging to a host
     * @param hostId the host
     * @param gpuGroups the list of GPU groups to save
     */
    void persist(long hostId, List<String> gpuGroups);

}
