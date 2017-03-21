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
package org.apache.cloudstack.network.dao;

import java.util.List;

import org.apache.cloudstack.network.VpcInlineLbMappingVO;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

public class VpcInlineLbMappingDaoImpl extends GenericDaoBase<VpcInlineLbMappingVO, Long> implements VpcInlineLbMappingDao {

    private final SearchBuilder<VpcInlineLbMappingVO> publicIpIdSearch;
    private final SearchBuilder<VpcInlineLbMappingVO> nicSecondaryIpIdSearch;
    private final SearchBuilder<VpcInlineLbMappingVO> nicIdSearch;
    private final SearchBuilder<VpcInlineLbMappingVO> vmIdSearch;

    public VpcInlineLbMappingDaoImpl() {
        publicIpIdSearch = createSearchBuilder();
        publicIpIdSearch.and("publicIpId", publicIpIdSearch.entity().getPublicIpId(), SearchCriteria.Op.EQ);
        publicIpIdSearch.done();

        nicSecondaryIpIdSearch = createSearchBuilder();
        nicSecondaryIpIdSearch.and("nicSecondaryIpId", nicSecondaryIpIdSearch.entity().getNicSecondaryIpId(), SearchCriteria.Op.EQ);
        nicSecondaryIpIdSearch.done();

        nicIdSearch = createSearchBuilder();
        nicIdSearch.and("nicId", nicIdSearch.entity().getNicId(), SearchCriteria.Op.EQ);
        nicIdSearch.done();

        vmIdSearch = createSearchBuilder();
        vmIdSearch.and("vmId", vmIdSearch.entity().getVmId(), SearchCriteria.Op.EQ);
        vmIdSearch.done();
    }

    @Override
    public VpcInlineLbMappingVO findByPublicIpAddress(long publicIpId) {
        SearchCriteria<VpcInlineLbMappingVO> sc = publicIpIdSearch.create();
        sc.setParameters("publicIpId", publicIpId);
        return findOneBy(sc);
    }

    @Override
    public VpcInlineLbMappingVO findByNicSecondaryIp(long nicSecondaryIpId) {
        SearchCriteria<VpcInlineLbMappingVO> sc = nicSecondaryIpIdSearch.create();
        sc.setParameters("nicSecondaryIpId", nicSecondaryIpId);
        return findOneBy(sc);
    }

    @Override
    public List<VpcInlineLbMappingVO> listByNicId(long nicId) {
        SearchCriteria<VpcInlineLbMappingVO> sc = nicIdSearch.create();
        sc.setParameters("nicId", nicId);
        return listBy(sc);
    }

    @Override
    public List<VpcInlineLbMappingVO> listByVmId(long vmId) {
        SearchCriteria<VpcInlineLbMappingVO> sc = vmIdSearch.create();
        sc.setParameters("vmId", vmId);
        return listBy(sc);
    }
}
