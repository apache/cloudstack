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

import com.cloud.gpu.GpuOfferingDetailVO;
import com.cloud.utils.db.GenericDao;
import org.apache.cloudstack.resourcedetail.ResourceDetailsDao;

import java.util.Map;

/**
 * Data Access Object for GPU offering details
 */
public interface GpuOfferingDetailsDao extends GenericDao<GpuOfferingDetailVO, Long>,
        ResourceDetailsDao<GpuOfferingDetailVO> {
    /**
     * Get details for a GPU offering
     *
     * @param gpuOfferingId GPU offering ID
     * @return a map of all details for the GPU offering
     */
    Map<String, String> getDetailsMap(long gpuOfferingId);

    /**
     * Update a detail for a GPU offering
     *
     * @param gpuOfferingId GPU offering ID
     * @param name          detail name
     * @param value         detail value
     * @param display       whether the detail should be displayed to the user
     * @return true if the detail was updated, false otherwise
     */
    boolean update(long gpuOfferingId, String name, String value, boolean display);

    /**
     * Add vGPU profile IDs to a GPU offering
     *
     * @param gpuOfferingId  GPU offering ID
     * @param vgpuProfileIds comma-separated list of vGPU profile IDs
     * @param display        whether the detail should be displayed to the user
     */
    void addVgpuProfileIds(long gpuOfferingId, String vgpuProfileIds, boolean display);

    /**
     * Get vGPU profile IDs for a GPU offering
     *
     * @param gpuOfferingId GPU offering ID
     * @return comma-separated list of vGPU profile IDs
     */
    String getVgpuProfileIds(long gpuOfferingId);
}
