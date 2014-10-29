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
package com.cloud.storage.dao;

import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.storage.GuestOSHypervisorVO;
import com.cloud.utils.db.GenericDao;

public interface GuestOSHypervisorDao extends GenericDao<GuestOSHypervisorVO, Long> {

    HypervisorType findHypervisorTypeByGuestOsId(long guestOsId);

    GuestOSHypervisorVO findByOsIdAndHypervisor(long guestOsId, String hypervisorType, String hypervisorVersion);

    boolean removeGuestOsMapping(Long id);

    GuestOSHypervisorVO findByOsIdAndHypervisorAndUserDefined(long guestOsId, String hypervisorType, String hypervisorVersion, boolean isUserDefined);
}
