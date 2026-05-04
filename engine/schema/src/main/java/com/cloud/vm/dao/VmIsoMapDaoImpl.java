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
package com.cloud.vm.dao;

import java.util.List;

import org.springframework.stereotype.Component;

import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.vm.VmIsoMapVO;

@Component
public class VmIsoMapDaoImpl extends GenericDaoBase<VmIsoMapVO, Long> implements VmIsoMapDao {

    private SearchBuilder<VmIsoMapVO> ListByVmId;
    private SearchBuilder<VmIsoMapVO> ByVmIdDeviceSeq;
    private SearchBuilder<VmIsoMapVO> ByVmIdIsoId;

    protected VmIsoMapDaoImpl() {
        ListByVmId = createSearchBuilder();
        ListByVmId.and("vmId", ListByVmId.entity().getVmId(), SearchCriteria.Op.EQ);
        ListByVmId.done();

        ByVmIdDeviceSeq = createSearchBuilder();
        ByVmIdDeviceSeq.and("vmId", ByVmIdDeviceSeq.entity().getVmId(), SearchCriteria.Op.EQ);
        ByVmIdDeviceSeq.and("deviceSeq", ByVmIdDeviceSeq.entity().getDeviceSeq(), SearchCriteria.Op.EQ);
        ByVmIdDeviceSeq.done();

        ByVmIdIsoId = createSearchBuilder();
        ByVmIdIsoId.and("vmId", ByVmIdIsoId.entity().getVmId(), SearchCriteria.Op.EQ);
        ByVmIdIsoId.and("isoId", ByVmIdIsoId.entity().getIsoId(), SearchCriteria.Op.EQ);
        ByVmIdIsoId.done();
    }

    @Override
    public List<VmIsoMapVO> listByVmId(long vmId) {
        SearchCriteria<VmIsoMapVO> sc = ListByVmId.create();
        sc.setParameters("vmId", vmId);
        return listBy(sc);
    }

    @Override
    public VmIsoMapVO findByVmIdDeviceSeq(long vmId, int deviceSeq) {
        SearchCriteria<VmIsoMapVO> sc = ByVmIdDeviceSeq.create();
        sc.setParameters("vmId", vmId);
        sc.setParameters("deviceSeq", deviceSeq);
        return findOneBy(sc);
    }

    @Override
    public VmIsoMapVO findByVmIdIsoId(long vmId, long isoId) {
        SearchCriteria<VmIsoMapVO> sc = ByVmIdIsoId.create();
        sc.setParameters("vmId", vmId);
        sc.setParameters("isoId", isoId);
        return findOneBy(sc);
    }

    @Override
    public int removeByVmId(long vmId) {
        SearchCriteria<VmIsoMapVO> sc = ListByVmId.create();
        sc.setParameters("vmId", vmId);
        return remove(sc);
    }
}
