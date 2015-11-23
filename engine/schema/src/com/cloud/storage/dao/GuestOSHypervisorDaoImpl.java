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

import java.util.Date;


import org.springframework.stereotype.Component;

import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.storage.GuestOSHypervisorVO;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@Component
public class GuestOSHypervisorDaoImpl extends GenericDaoBase<GuestOSHypervisorVO, Long> implements GuestOSHypervisorDao {

    protected final SearchBuilder<GuestOSHypervisorVO> guestOsSearch;
    protected final SearchBuilder<GuestOSHypervisorVO> mappingSearch;
    protected final SearchBuilder<GuestOSHypervisorVO> userDefinedMappingSearch;

    protected GuestOSHypervisorDaoImpl() {
        guestOsSearch = createSearchBuilder();
        guestOsSearch.and("guest_os_id", guestOsSearch.entity().getGuestOsId(), SearchCriteria.Op.EQ);
        guestOsSearch.done();

        mappingSearch = createSearchBuilder();
        mappingSearch.and("guest_os_id", mappingSearch.entity().getGuestOsId(), SearchCriteria.Op.EQ);
        mappingSearch.and("hypervisor_type", mappingSearch.entity().getHypervisorType(), SearchCriteria.Op.EQ);
        mappingSearch.and("hypervisor_version", mappingSearch.entity().getHypervisorVersion(), SearchCriteria.Op.EQ);
        mappingSearch.done();

        userDefinedMappingSearch = createSearchBuilder();
        userDefinedMappingSearch.and("guest_os_id", userDefinedMappingSearch.entity().getGuestOsId(), SearchCriteria.Op.EQ);
        userDefinedMappingSearch.and("hypervisor_type", userDefinedMappingSearch.entity().getHypervisorType(), SearchCriteria.Op.EQ);
        userDefinedMappingSearch.and("hypervisor_version", userDefinedMappingSearch.entity().getHypervisorVersion(), SearchCriteria.Op.EQ);
        userDefinedMappingSearch.and("is_user_defined", userDefinedMappingSearch.entity().getIsUserDefined(), SearchCriteria.Op.EQ);
        userDefinedMappingSearch.done();
    }

    @Override
    public HypervisorType findHypervisorTypeByGuestOsId(long guestOsId) {
        SearchCriteria<GuestOSHypervisorVO> sc = guestOsSearch.create();
        sc.setParameters("guest_os_id", guestOsId);
        GuestOSHypervisorVO goh = findOneBy(sc);
        return HypervisorType.getType(goh.getHypervisorType());
    }

    @Override
    public GuestOSHypervisorVO findByOsIdAndHypervisor(long guestOsId, String hypervisorType, String hypervisorVersion) {
        SearchCriteria<GuestOSHypervisorVO> sc = mappingSearch.create();
        String version = "default";
        if (!(hypervisorVersion == null || hypervisorVersion.isEmpty())) {
            version = hypervisorVersion;
        }
        sc.setParameters("guest_os_id", guestOsId);
        sc.setParameters("hypervisor_type", hypervisorType);
        sc.setParameters("hypervisor_version", version);
        return findOneBy(sc);
    }

    @Override
    public GuestOSHypervisorVO findByOsIdAndHypervisorAndUserDefined(long guestOsId, String hypervisorType, String hypervisorVersion, boolean isUserDefined) {
        SearchCriteria<GuestOSHypervisorVO> sc = userDefinedMappingSearch.create();
        String version = "default";
        if (!(hypervisorVersion == null || hypervisorVersion.isEmpty())) {
            version = hypervisorVersion;
        }
        sc.setParameters("guest_os_id", guestOsId);
        sc.setParameters("hypervisor_type", hypervisorType);
        sc.setParameters("hypervisor_version", version);
        sc.setParameters("is_user_defined", isUserDefined);
        return findOneBy(sc);
    }

    @Override
    public boolean removeGuestOsMapping(Long id) {
        GuestOSHypervisorVO guestOsHypervisor = findById(id);
        createForUpdate(id);
        guestOsHypervisor.setRemoved(new Date());
        update(id, guestOsHypervisor);
        return super.remove(id);
    }

}
