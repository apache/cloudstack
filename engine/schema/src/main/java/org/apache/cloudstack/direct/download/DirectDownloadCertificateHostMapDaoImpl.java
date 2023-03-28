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

import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

import java.util.List;

public class DirectDownloadCertificateHostMapDaoImpl extends GenericDaoBase<DirectDownloadCertificateHostMapVO, Long> implements DirectDownloadCertificateHostMapDao {
    private final SearchBuilder<DirectDownloadCertificateHostMapVO> mapSearchBuilder;

    public DirectDownloadCertificateHostMapDaoImpl() {
        mapSearchBuilder = createSearchBuilder();
        mapSearchBuilder.and("certificate_id", mapSearchBuilder.entity().getCertificateId(), SearchCriteria.Op.EQ);
        mapSearchBuilder.and("host_id", mapSearchBuilder.entity().getHostId(), SearchCriteria.Op.EQ);
        mapSearchBuilder.and("revoked", mapSearchBuilder.entity().isRevoked(), SearchCriteria.Op.EQ);
        mapSearchBuilder.done();
    }
    @Override
    public DirectDownloadCertificateHostMapVO findByCertificateAndHost(long certificateId, long hostId) {
        SearchCriteria<DirectDownloadCertificateHostMapVO> sc = mapSearchBuilder.create();
        sc.setParameters("certificate_id", certificateId);
        sc.setParameters("host_id", hostId);
        return findOneBy(sc);
    }

    @Override
    public List<DirectDownloadCertificateHostMapVO> listByCertificateId(long certificateId) {
        SearchCriteria<DirectDownloadCertificateHostMapVO> sc = mapSearchBuilder.create();
        sc.setParameters("certificate_id", certificateId);
        return listBy(sc);
    }

    @Override
    public List<DirectDownloadCertificateHostMapVO> listByCertificateIdAndRevoked(long certificateId, boolean revoked) {
        SearchCriteria<DirectDownloadCertificateHostMapVO> sc = mapSearchBuilder.create();
        sc.setParameters("certificate_id", certificateId);
        sc.setParameters("revoked", revoked);
        return listBy(sc);
    }
}
