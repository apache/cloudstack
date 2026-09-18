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

import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import org.apache.cloudstack.backup.BackupReportKbossJoinVO;

import java.util.Date;
import java.util.List;

public class BackupReportKbossJoinDaoImpl extends GenericDaoBase<BackupReportKbossJoinVO, Long> implements BackupReportKbossJoinDao {
    private static final String ZONE_ID = "zone_id";
    private static final String DOMAIN_ID = "domain_id";
    private static final String ACCOUNT_ID = "account_id";
    private static final String REMOVED = "removed";

    private SearchBuilder<BackupReportKbossJoinVO> listByZoneAndDomainAndAccountAndDateBetween;

    public BackupReportKbossJoinDaoImpl() {
        listByZoneAndDomainAndAccountAndDateBetween = createSearchBuilder();
        listByZoneAndDomainAndAccountAndDateBetween.and(ZONE_ID, listByZoneAndDomainAndAccountAndDateBetween.entity().getZoneId(), SearchCriteria.Op.EQ);
        listByZoneAndDomainAndAccountAndDateBetween.and(DOMAIN_ID, listByZoneAndDomainAndAccountAndDateBetween.entity().getDomainId(), SearchCriteria.Op.EQ);
        listByZoneAndDomainAndAccountAndDateBetween.and(ACCOUNT_ID, listByZoneAndDomainAndAccountAndDateBetween.entity().getAccountId(), SearchCriteria.Op.EQ);
        listByZoneAndDomainAndAccountAndDateBetween.and(REMOVED, listByZoneAndDomainAndAccountAndDateBetween.entity().getRemoved(), SearchCriteria.Op.BETWEEN);
        listByZoneAndDomainAndAccountAndDateBetween.done();
    }

    @Override
    public List<BackupReportKbossJoinVO> listByZoneAndDomainAndAccountAndBetweenDates(Long zoneId, Long domainId, Long accountId, Date start, Date end) {
        SearchCriteria<BackupReportKbossJoinVO> sc = listByZoneAndDomainAndAccountAndDateBetween.create();
        sc.setParametersIfNotNull(ZONE_ID, zoneId);
        sc.setParametersIfNotNull(DOMAIN_ID, domainId);
        sc.setParametersIfNotNull(ACCOUNT_ID, accountId);
        sc.setParameters(REMOVED, start, end);
        Filter filter = new Filter(BackupReportKbossJoinVO.class, "domainId", true);
        filter.addOrderBy(BackupReportKbossJoinVO.class, "accountId", true);
        filter.addOrderBy(BackupReportKbossJoinVO.class, "vmId", true);

        return searchIncludingRemoved(sc, filter, null, false);
    }
}
