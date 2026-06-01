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

import com.cloud.gpu.GpuCardVO;
import com.cloud.utils.Pair;
import com.cloud.utils.db.GenericDao;

import java.util.List;

public interface GpuCardDao extends GenericDao<GpuCardVO, Long> {

    /**
     * Find GPU card by vendor and device id
     *
     * @param vendorId the vendor id
     * @param deviceId the device id
     * @return GpuCardVO
     */
    GpuCardVO findByVendorIdAndDeviceId(String vendorId, String deviceId);

    Pair<List<GpuCardVO>, Integer> searchAndCountGpuCards(
            Long id, String keyword, String vendorId, String vendorName,
            String deviceId, String deviceName, boolean activeOnly, Long startIndex, Long pageSize);
}
