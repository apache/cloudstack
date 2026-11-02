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

import com.cloud.utils.Pair;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.vm.VmwareCbtMigrationVO;
import org.apache.cloudstack.vm.VmwareCbtMigration;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

@Component
public class VmwareCbtMigrationDaoImpl extends GenericDaoBase<VmwareCbtMigrationVO, Long> implements VmwareCbtMigrationDao {

    private static final VmwareCbtMigration.State[] TERMINAL_STATES = {
            VmwareCbtMigration.State.Completed,
            VmwareCbtMigration.State.Failed,
            VmwareCbtMigration.State.Cancelled
    };

    private final SearchBuilder<VmwareCbtMigrationVO> migrationSearch;
    private final SearchBuilder<VmwareCbtMigrationVO> convertHostSearch;
    private final SearchBuilder<VmwareCbtMigrationVO> nonTerminalSearch;

    public VmwareCbtMigrationDaoImpl() {
        migrationSearch = createSearchBuilder();
        migrationSearch.and("id", migrationSearch.entity().getId(), SearchCriteria.Op.EQ);
        migrationSearch.and("zoneId", migrationSearch.entity().getZoneId(), SearchCriteria.Op.EQ);
        migrationSearch.and("accountId", migrationSearch.entity().getAccountId(), SearchCriteria.Op.EQ);
        migrationSearch.and("vcenter", migrationSearch.entity().getVcenter(), SearchCriteria.Op.EQ);
        migrationSearch.and("sourceVmName", migrationSearch.entity().getSourceVmName(), SearchCriteria.Op.EQ);
        migrationSearch.and("state", migrationSearch.entity().getState(), SearchCriteria.Op.EQ);
        migrationSearch.done();

        convertHostSearch = createSearchBuilder();
        convertHostSearch.and("convertHostId", convertHostSearch.entity().getConvertHostId(), SearchCriteria.Op.EQ);
        convertHostSearch.done();

        nonTerminalSearch = createSearchBuilder();
        nonTerminalSearch.and("id", nonTerminalSearch.entity().getId(), SearchCriteria.Op.EQ);
        nonTerminalSearch.and("state", nonTerminalSearch.entity().getState(), SearchCriteria.Op.NOTIN);
        nonTerminalSearch.done();
    }

    @Override
    public boolean updateIfNotTerminal(VmwareCbtMigrationVO migration) {
        SearchCriteria<VmwareCbtMigrationVO> sc = nonTerminalSearch.create();
        sc.setParameters("id", migration.getId());
        sc.setParameters("state", (Object[]) TERMINAL_STATES);
        return update(migration, sc) > 0;
    }

    @Override
    public boolean completeImportIfNotTerminal(long migrationId, long vmId, Date updated) {
        VmwareCbtMigrationVO migration = createImportedVmUpdate(migrationId, vmId, updated);
        migration.setState(VmwareCbtMigration.State.Completed);
        migration.setCurrentStep("CloudStack VM import completed");
        migration.setLastError(null);
        return updateIfNotTerminal(migration);
    }

    @Override
    public boolean recordImportedVmAndClearCredentials(long migrationId, long vmId, Date updated) {
        return update(migrationId, createImportedVmUpdate(migrationId, vmId, updated));
    }

    private VmwareCbtMigrationVO createImportedVmUpdate(long migrationId, long vmId, Date updated) {
        VmwareCbtMigrationVO migration = createForUpdate(migrationId);
        migration.setVmId(vmId);
        migration.setSourceUsername(null);
        migration.setSourcePassword(null);
        migration.setUpdated(updated);
        return migration;
    }

    @Override
    public Pair<List<VmwareCbtMigrationVO>, Integer> listMigrations(Long id, Long zoneId, Long accountId, String vcenter,
                                                                    String sourceVmName, VmwareCbtMigration.State state,
                                                                    Long startIndex, Long pageSizeVal) {
        SearchCriteria<VmwareCbtMigrationVO> sc = migrationSearch.create();
        if (id != null) {
            sc.setParameters("id", id);
        }
        if (zoneId != null) {
            sc.setParameters("zoneId", zoneId);
        }
        if (accountId != null) {
            sc.setParameters("accountId", accountId);
        }
        if (StringUtils.isNotBlank(vcenter)) {
            sc.setParameters("vcenter", vcenter);
        }
        if (StringUtils.isNotBlank(sourceVmName)) {
            sc.setParameters("sourceVmName", sourceVmName);
        }
        if (state != null) {
            sc.setParameters("state", state);
        }
        Filter filter = new Filter(VmwareCbtMigrationVO.class, "created", false, startIndex, pageSizeVal);
        return searchAndCount(sc, filter);
    }

    @Override
    public List<VmwareCbtMigrationVO> listByConvertHostId(Long convertHostId) {
        SearchCriteria<VmwareCbtMigrationVO> sc = convertHostSearch.create();
        sc.setParameters("convertHostId", convertHostId);
        return listBy(sc);
    }
}
