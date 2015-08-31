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

import java.util.List;

import javax.ejb.Local;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.storage.VMTemplateZoneVO;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.TransactionLegacy;

@Component
@Local(value = {VMTemplateZoneDao.class})
public class VMTemplateZoneDaoImpl extends GenericDaoBase<VMTemplateZoneVO, Long> implements VMTemplateZoneDao {
    public static final Logger s_logger = Logger.getLogger(VMTemplateZoneDaoImpl.class.getName());

    protected final SearchBuilder<VMTemplateZoneVO> ZoneSearch;
    protected final SearchBuilder<VMTemplateZoneVO> TemplateSearch;
    protected final SearchBuilder<VMTemplateZoneVO> ZoneTemplateSearch;

    public VMTemplateZoneDaoImpl() {
        ZoneSearch = createSearchBuilder();
        ZoneSearch.and("zone_id", ZoneSearch.entity().getZoneId(), SearchCriteria.Op.EQ);
        ZoneSearch.done();

        TemplateSearch = createSearchBuilder();
        TemplateSearch.and("template_id", TemplateSearch.entity().getTemplateId(), SearchCriteria.Op.EQ);
        TemplateSearch.done();

        ZoneTemplateSearch = createSearchBuilder();
        ZoneTemplateSearch.and("zone_id", ZoneTemplateSearch.entity().getZoneId(), SearchCriteria.Op.EQ);
        ZoneTemplateSearch.and("template_id", ZoneTemplateSearch.entity().getTemplateId(), SearchCriteria.Op.EQ);
        ZoneTemplateSearch.done();
    }

    @Override
    public List<VMTemplateZoneVO> listByZoneId(long id) {
        SearchCriteria<VMTemplateZoneVO> sc = ZoneSearch.create();
        sc.setParameters("zone_id", id);
        return listIncludingRemovedBy(sc);
    }

    @Override
    public List<VMTemplateZoneVO> listByTemplateId(long templateId) {
        SearchCriteria<VMTemplateZoneVO> sc = TemplateSearch.create();
        sc.setParameters("template_id", templateId);
        return listIncludingRemovedBy(sc);
    }

    @Override
    public VMTemplateZoneVO findByZoneTemplate(long zoneId, long templateId) {
        SearchCriteria<VMTemplateZoneVO> sc = ZoneTemplateSearch.create();
        sc.setParameters("zone_id", zoneId);
        sc.setParameters("template_id", templateId);
        return findOneIncludingRemovedBy(sc);
    }

    @Override
    public List<VMTemplateZoneVO> listByZoneTemplate(Long zoneId, long templateId) {
        SearchCriteria<VMTemplateZoneVO> sc = ZoneTemplateSearch.create();
        if (zoneId != null) {
            sc.setParameters("zone_id", zoneId);
        }
        sc.setParameters("template_id", templateId);
        return listBy(sc);
    }

    @Override
    public void deletePrimaryRecordsForTemplate(long templateId) {
        SearchCriteria<VMTemplateZoneVO> sc = TemplateSearch.create();
        sc.setParameters("template_id", templateId);
        TransactionLegacy txn = TransactionLegacy.currentTxn();
        txn.start();
        remove(sc);
        txn.commit();

    }

}
