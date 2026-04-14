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
package org.apache.cloudstack.backup.dao;


import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.cloud.utils.db.DB;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.backup.BackupOfferingDetailsVO;
import org.apache.cloudstack.resourcedetail.ResourceDetailsDaoBase;
import org.springframework.stereotype.Component;

@Component
public class BackupOfferingDetailsDaoImpl extends ResourceDetailsDaoBase<BackupOfferingDetailsVO> implements BackupOfferingDetailsDao {

    @Override
    public void addDetail(long resourceId, String key, String value, boolean display) {
        super.addDetail(new BackupOfferingDetailsVO(resourceId, key, value, display));
    }

    @Override
    public List<Long> findDomainIds(long resourceId) {
        final List<Long> domainIds = new ArrayList<>();
        for (final BackupOfferingDetailsVO detail: findDetails(resourceId, ApiConstants.DOMAIN_ID)) {
            final Long domainId = Long.valueOf(detail.getValue());
            if (domainId > 0) {
                domainIds.add(domainId);
            }
        }
        return domainIds;
    }

    @Override
    public List<Long> findZoneIds(long resourceId) {
        final List<Long> zoneIds = new ArrayList<>();
        for (final BackupOfferingDetailsVO detail: findDetails(resourceId, ApiConstants.ZONE_ID)) {
            final Long zoneId = Long.valueOf(detail.getValue());
            if (zoneId > 0) {
                zoneIds.add(zoneId);
            }
        }
        return zoneIds;
    }

    @Override
    public String getDetail(Long backupOfferingId, String key) {
        String detailValue = null;
        BackupOfferingDetailsVO backupOfferingDetail = findDetail(backupOfferingId, key);
        if (backupOfferingDetail != null) {
            detailValue = backupOfferingDetail.getValue();
        }
        return detailValue;
    }

    @Override
    public List<Long> findOfferingIdsByDomainIds(List<Long> domainIds) {
        Object[] dIds = domainIds.stream().map(s -> String.valueOf(s)).collect(Collectors.toList()).toArray();
        return findResourceIdsByNameAndValueIn("domainid", dIds);
    }

    @DB
    @Override
    public void updateBackupOfferingDomainIdsDetail(long backupOfferingId, List<Long> filteredDomainIds) {
    SearchBuilder<BackupOfferingDetailsVO> sb = createSearchBuilder();
        List<BackupOfferingDetailsVO> detailsVO = new ArrayList<>();
        sb.and("offeringId", sb.entity().getResourceId(), SearchCriteria.Op.EQ);
        sb.and("detailName", sb.entity().getName(), SearchCriteria.Op.EQ);
        sb.done();
        SearchCriteria<BackupOfferingDetailsVO> sc = sb.create();
        sc.setParameters("offeringId", String.valueOf(backupOfferingId));
        sc.setParameters("detailName", ApiConstants.DOMAIN_ID);
        remove(sc);
        for (Long domainId : filteredDomainIds) {
            detailsVO.add(new BackupOfferingDetailsVO(backupOfferingId, ApiConstants.DOMAIN_ID, String.valueOf(domainId), false));
        }
        if (!detailsVO.isEmpty()) {
            for (BackupOfferingDetailsVO detailVO : detailsVO) {
                persist(detailVO);
            }
        }
    }
}
