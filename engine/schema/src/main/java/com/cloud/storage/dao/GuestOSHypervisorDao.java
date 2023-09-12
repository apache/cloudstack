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

import com.cloud.storage.GuestOSHypervisorVO;
import com.cloud.utils.db.GenericDao;

import java.util.List;

public interface GuestOSHypervisorDao extends GenericDao<GuestOSHypervisorVO, Long> {

    /**
     * list all the mappings for a guesos id
     * @param guestOsId the guestos to look for
     * @return a list of mappings
     */
    List<GuestOSHypervisorVO> listByGuestOsId(long guestOsId);

    GuestOSHypervisorVO findByOsIdAndHypervisor(long guestOsId, String hypervisorType, String hypervisorVersion);

    boolean removeGuestOsMapping(Long id);

    GuestOSHypervisorVO findByOsIdAndHypervisorAndUserDefined(long guestOsId, String hypervisorType, String hypervisorVersion, boolean isUserDefined);

    GuestOSHypervisorVO findByOsNameAndHypervisor(String guestOsName, String hypervisorType, String hypervisorVersion);

    GuestOSHypervisorVO findByOsNameAndHypervisorOrderByCreatedDesc(String guestOsName, String hypervisorType, String hypervisorVersion);

    List<GuestOSHypervisorVO> listByOsNameAndHypervisorMinimumVersion(String guestOsName, String hypervisorType,
                                                                      String minHypervisorVersion);

    List<String> listHypervisorSupportedVersionsFromMinimumVersion(String hypervisorType, String hypervisorVersion);

    List<GuestOSHypervisorVO> listByHypervisorTypeAndVersion(String hypervisorType, String hypervisorVersion);
}
