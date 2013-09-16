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
package org.apache.cloudstack.engine.cloud.entity.api.db.dao;


import java.util.List;

import javax.annotation.PostConstruct;
import javax.ejb.Local;
import javax.inject.Inject;

import org.apache.cloudstack.engine.cloud.entity.api.db.VMReservationVO;
import org.apache.cloudstack.engine.cloud.entity.api.db.VolumeReservationVO;
import org.springframework.stereotype.Component;

import com.cloud.host.dao.HostTagsDaoImpl;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@Component
@Local(value = { VolumeReservationDao.class })
public class VolumeReservationDaoImpl extends GenericDaoBase<VolumeReservationVO, Long> implements VolumeReservationDao {

    protected SearchBuilder<VolumeReservationVO> VmIdSearch;
    protected SearchBuilder<VolumeReservationVO> VmReservationIdSearch;

    public VolumeReservationDaoImpl() {
    }

    @PostConstruct
    public void init() {
        VmIdSearch = createSearchBuilder();
        VmIdSearch.and("vmId", VmIdSearch.entity().getVmId(), SearchCriteria.Op.EQ);
        VmIdSearch.done();

        VmReservationIdSearch = createSearchBuilder();
        VmReservationIdSearch.and("vmReservationId", VmReservationIdSearch.entity().getVmReservationId(), SearchCriteria.Op.EQ);
        VmReservationIdSearch.done();
    }

    @Override
    public VolumeReservationVO findByVmId(long vmId) {
        SearchCriteria<VolumeReservationVO> sc = VmIdSearch.create("vmId", vmId);
        return findOneBy(sc);
    }

    @Override
    public List<VolumeReservationVO> listVolumeReservation(long vmReservationId) {
        SearchCriteria<VolumeReservationVO> sc = VmReservationIdSearch.create("vmReservationId", vmReservationId);
        return listBy(sc);
    }

}
