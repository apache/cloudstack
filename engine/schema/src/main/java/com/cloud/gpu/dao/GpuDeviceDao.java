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

import com.cloud.gpu.GpuDeviceVO;
import com.cloud.utils.Pair;
import com.cloud.utils.db.GenericDao;

import java.util.List;

public interface GpuDeviceDao extends GenericDao<GpuDeviceVO, Long> {

    /**
     * Find GPU device by host ID and bus address
     *
     * @param hostId     the host ID
     * @param busAddress the PCI bus address
     * @return GpuDeviceVO
     */
    GpuDeviceVO findByHostIdAndBusAddress(long hostId, String busAddress);

    /**
     * List GPU devices by host ID
     *
     * @param hostId the ID of the host
     * @return a list of GPU devices for the host
     */
    List<GpuDeviceVO> listByHostId(long hostId);

    /**
     * List GPU devices by VM ID
     *
     * @param vmId the VM ID
     * @return list of GpuDeviceVO
     */
    List<GpuDeviceVO> listByVmId(long vmId);

    /**
     * List GPU devices by card ID
     *
     * @param cardId the GPU card ID
     * @return list of GpuDeviceVO
     */
    List<GpuDeviceVO> listByCardId(long cardId);

    /**
     * List vGPU devices by parent GPU device ID
     *
     * @param parentGpuDeviceId the parent GPU device ID
     * @return list of GpuDeviceVO
     */
    List<GpuDeviceVO> listByParentGpuDeviceId(long parentGpuDeviceId);

    boolean isVgpuProfileInUse(long vgpuProfileId);

    boolean isGpuCardInUse(long cardId);

    List<GpuDeviceVO> listByHostAndVm(Long hostId, long vmId);

    List<GpuDeviceVO> listDevicesForAllocation(Long hostId, List<Long> vgpuProfileIdList);

    Pair<List<GpuDeviceVO>, Integer> searchAndCountGpuDevices(
            Long id, String keyword, Long hostId, Long vmId, Long gpuCardId, Long vgpuProfileId,
            Long startIndex, Long pageSize);
}
