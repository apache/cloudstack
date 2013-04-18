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

import javax.ejb.Local;

import org.springframework.stereotype.Component;

import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.storage.GuestOSHypervisorVO;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@Component
@Local(value = { GuestOSHypervisorDao.class })
public class GuestOSHypervisorDaoImpl extends GenericDaoBase<GuestOSHypervisorVO, Long> implements GuestOSHypervisorDao {

    protected final SearchBuilder<GuestOSHypervisorVO> guestOsSearch;

    protected GuestOSHypervisorDaoImpl() {
        guestOsSearch = createSearchBuilder();
        guestOsSearch.and("guest_os_id", guestOsSearch.entity().getGuestOsId(), SearchCriteria.Op.EQ);
        guestOsSearch.done();
    }

    @Override
    public HypervisorType findHypervisorTypeByGuestOsId(long guestOsId) {
        SearchCriteria<GuestOSHypervisorVO> sc = guestOsSearch.create();
        sc.setParameters("guest_os_id", guestOsId);
        GuestOSHypervisorVO goh = findOneBy(sc);
        return HypervisorType.getType(goh.getHypervisorType());
    }

}
