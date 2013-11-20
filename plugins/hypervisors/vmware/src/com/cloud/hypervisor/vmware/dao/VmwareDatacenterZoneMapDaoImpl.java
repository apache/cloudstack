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

package com.cloud.hypervisor.vmware.dao;

import javax.ejb.Local;

import org.springframework.stereotype.Component;

import com.cloud.hypervisor.vmware.VmwareDatacenterZoneMapVO;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Op;

@Component
@Local(value = VmwareDatacenterZoneMapDao.class)
public class VmwareDatacenterZoneMapDaoImpl extends GenericDaoBase<VmwareDatacenterZoneMapVO, Long> implements VmwareDatacenterZoneMapDao {

    protected final SearchBuilder<VmwareDatacenterZoneMapVO> zoneSearch;
    protected final SearchBuilder<VmwareDatacenterZoneMapVO> vmwareDcSearch;

    public VmwareDatacenterZoneMapDaoImpl() {
        zoneSearch = createSearchBuilder();
        zoneSearch.and("zoneId", zoneSearch.entity().getZoneId(), Op.EQ);
        zoneSearch.done();

        vmwareDcSearch = createSearchBuilder();
        vmwareDcSearch.and("vmwareDcId", vmwareDcSearch.entity().getVmwareDcId(), Op.EQ);
        vmwareDcSearch.done();
    }

    @Override
    public VmwareDatacenterZoneMapVO findByZoneId(long zoneId) {
        SearchCriteria<VmwareDatacenterZoneMapVO> sc = zoneSearch.create();
        sc.setParameters("zoneId", zoneId);
        return findOneBy(sc);
    }

    @Override
    public VmwareDatacenterZoneMapVO findByVmwareDcId(long vmwareDcId) {
        SearchCriteria<VmwareDatacenterZoneMapVO> sc = vmwareDcSearch.create();
        sc.setParameters("vmwareDcId", vmwareDcId);
        return findOneBy(sc);
    }
}
