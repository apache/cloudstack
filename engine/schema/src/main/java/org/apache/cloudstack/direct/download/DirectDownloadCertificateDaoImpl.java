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
package org.apache.cloudstack.direct.download;

import com.cloud.hypervisor.Hypervisor;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

import java.util.List;

public class DirectDownloadCertificateDaoImpl extends GenericDaoBase<DirectDownloadCertificateVO, Long> implements DirectDownloadCertificateDao {

    private final SearchBuilder<DirectDownloadCertificateVO> certificateSearchBuilder;

    public DirectDownloadCertificateDaoImpl() {
        certificateSearchBuilder = createSearchBuilder();
        certificateSearchBuilder.and("alias", certificateSearchBuilder.entity().getAlias(), SearchCriteria.Op.EQ);
        certificateSearchBuilder.and("hypervisor_type", certificateSearchBuilder.entity().getHypervisorType(), SearchCriteria.Op.EQ);
        certificateSearchBuilder.and("zone_id", certificateSearchBuilder.entity().getZoneId(), SearchCriteria.Op.EQ);
        certificateSearchBuilder.done();
    }

    @Override
    public DirectDownloadCertificateVO findByAlias(String alias, Hypervisor.HypervisorType hypervisorType, long zoneId) {
        SearchCriteria<DirectDownloadCertificateVO> sc = certificateSearchBuilder.create();
        sc.setParameters("alias", alias);
        sc.setParameters("hypervisor_type", hypervisorType);
        sc.setParameters("zone_id", zoneId);
        return findOneBy(sc);
    }

    @Override
    public List<DirectDownloadCertificateVO> listByZone(long zoneId) {
        SearchCriteria<DirectDownloadCertificateVO> sc = certificateSearchBuilder.create();
        sc.setParameters("zone_id", zoneId);
        return listBy(sc);
    }
}
