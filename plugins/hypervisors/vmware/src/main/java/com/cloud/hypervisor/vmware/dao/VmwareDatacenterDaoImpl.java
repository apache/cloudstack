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

package com.cloud.hypervisor.vmware.dao;

import java.util.List;


import org.springframework.stereotype.Component;

import com.cloud.hypervisor.vmware.VmwareDatacenterVO;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Op;

@Component
@DB
public class VmwareDatacenterDaoImpl extends GenericDaoBase<VmwareDatacenterVO, Long> implements VmwareDatacenterDao {

    final SearchBuilder<VmwareDatacenterVO> nameSearch;
    final SearchBuilder<VmwareDatacenterVO> guidSearch;
    final SearchBuilder<VmwareDatacenterVO> vcSearch;
    final SearchBuilder<VmwareDatacenterVO> nameVcSearch;
    final SearchBuilder<VmwareDatacenterVO> fullTableSearch;

    public VmwareDatacenterDaoImpl() {
        super();

        nameSearch = createSearchBuilder();
        nameSearch.and("name", nameSearch.entity().getVmwareDatacenterName(), Op.EQ);
        nameSearch.done();

        nameVcSearch = createSearchBuilder();
        nameVcSearch.and("name", nameVcSearch.entity().getVmwareDatacenterName(), Op.EQ);
        nameVcSearch.and("vCenterHost", nameVcSearch.entity().getVcenterHost(), Op.EQ);
        nameVcSearch.done();

        vcSearch = createSearchBuilder();
        vcSearch.and("vCenterHost", vcSearch.entity().getVcenterHost(), Op.EQ);
        vcSearch.done();

        guidSearch = createSearchBuilder();
        guidSearch.and("guid", guidSearch.entity().getGuid(), Op.EQ);
        guidSearch.done();

        fullTableSearch = createSearchBuilder();
        fullTableSearch.done();
    }

    @Override
    public VmwareDatacenterVO getVmwareDatacenterByGuid(String guid) {
        SearchCriteria<VmwareDatacenterVO> sc = guidSearch.create();
        sc.setParameters("guid", guid);
        return findOneBy(sc);
    }

    @Override
    public List<VmwareDatacenterVO> getVmwareDatacenterByNameAndVcenter(String name, String vCenterHost) {
        SearchCriteria<VmwareDatacenterVO> sc = nameVcSearch.create();
        sc.setParameters("name", name);
        sc.setParameters("vCenterHost", vCenterHost);
        return search(sc, null);
    }

    @Override
    public List<VmwareDatacenterVO> listVmwareDatacenterByName(String name) {
        SearchCriteria<VmwareDatacenterVO> sc = nameSearch.create();
        sc.setParameters("name", name);
        return search(sc, null);
    }

    @Override
    public List<VmwareDatacenterVO> listVmwareDatacenterByVcenter(String vCenterHost) {
        SearchCriteria<VmwareDatacenterVO> sc = vcSearch.create();
        sc.setParameters("vCenterHost", vCenterHost);
        return search(sc, null);
    }

    @Override
    public List<VmwareDatacenterVO> listAllVmwareDatacenters() {
        SearchCriteria<VmwareDatacenterVO> sc = fullTableSearch.create();
        return search(sc, null);
    }

}
