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
import org.apache.cloudstack.backup.Backup;
import org.apache.cloudstack.backup.NativeBackupJoinVO;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class NativeBackupJoinDaoImpl extends GenericDaoBase<NativeBackupJoinVO, Long> implements NativeBackupJoinDao {

    private static final String ID = "id";
    private static final String VM_ID = "vm_id";
    private static final String STATUS = "status";
    private static final String CREATED_BEFORE = "created_before";
    private static final String CREATED_AFTER = "created_after";
    private static final String CURRENT = "current";
    private static final String ISOLATED = "isolated";
    private static final String PARENT_ID = "parent_id";
    private static final String IMAGE_STORE_ID = "image_store_id";
    private SearchBuilder<NativeBackupJoinVO> backupSearch;
    private SearchBuilder<NativeBackupJoinVO> allBackupsSearch;

    @PostConstruct
    protected void init() {
        backupSearch = createSearchBuilder();
        backupSearch.and(VM_ID, backupSearch.entity().getVmId(), SearchCriteria.Op.EQ);
        backupSearch.and(STATUS, backupSearch.entity().getStatus(), SearchCriteria.Op.IN);
        backupSearch.and(CREATED_BEFORE, backupSearch.entity().getDate(), SearchCriteria.Op.LT);
        backupSearch.and(CREATED_AFTER, backupSearch.entity().getDate(), SearchCriteria.Op.GT);
        backupSearch.and(CURRENT, backupSearch.entity().getCurrent(), SearchCriteria.Op.EQ);
        backupSearch.and(PARENT_ID, backupSearch.entity().getParentId(), SearchCriteria.Op.EQ);
        backupSearch.and(ISOLATED, backupSearch.entity().getIsolated(), SearchCriteria.Op.EQ);
        backupSearch.groupBy(backupSearch.entity().getId());
        backupSearch.done();

        allBackupsSearch = createSearchBuilder();
        allBackupsSearch.and(ID, allBackupsSearch.entity().getId(), SearchCriteria.Op.EQ);
        allBackupsSearch.and(STATUS, allBackupsSearch.entity().getStatus(), SearchCriteria.Op.IN);
        allBackupsSearch.and(PARENT_ID, allBackupsSearch.entity().getParentId(), SearchCriteria.Op.EQ);
        allBackupsSearch.and(IMAGE_STORE_ID, allBackupsSearch.entity().getImageStoreId(), SearchCriteria.Op.EQ);
        allBackupsSearch.done();
    }
    @Override
    public List<NativeBackupJoinVO> listByBackedUpAndVmIdAndDateBeforeOrAfterOrderBy(long vmId, Date date, boolean before, boolean ascending) {
        SearchCriteria<NativeBackupJoinVO> sc = backupSearch.create();
        sc.setParameters(VM_ID, vmId);
        sc.setParameters(STATUS, Backup.Status.BackedUp);
        if (before) {
            sc.setParameters(CREATED_BEFORE, date);
        } else {
            sc.setParameters(CREATED_AFTER, date);
        }
        sc.setParameters(ISOLATED, Boolean.FALSE.toString());
        Filter filter = new Filter(NativeBackupJoinVO.class, "date", ascending);
        return new ArrayList<>(listBy(sc, filter));
    }

    @Override
    public List<NativeBackupJoinVO> listIncludingRemovedByVmIdAndBeforeDateOrderByCreatedDesc(long vmId, Date beforeDate) {
        SearchCriteria<NativeBackupJoinVO> sc = backupSearch.create();
        sc.setParameters(VM_ID, vmId);
        sc.setParameters(STATUS, Backup.Status.BackedUp, Backup.Status.Removed);
        sc.setParameters(CREATED_BEFORE, beforeDate);
        sc.setParameters(ISOLATED, Boolean.FALSE.toString());
        Filter filter = new Filter(NativeBackupJoinVO.class, "date", false);
        return new ArrayList<>(listIncludingRemovedBy(sc, filter));
    }

    @Override
    public NativeBackupJoinVO findCurrent(long vmId) {
        SearchCriteria<NativeBackupJoinVO> sc = backupSearch.create();
        sc.setParameters(VM_ID, vmId);
        sc.setParameters(CURRENT, Boolean.TRUE.toString());
        return findOneBy(sc);
    }

    @Override
    public NativeBackupJoinVO findByParentId(long parentId) {
        SearchCriteria<NativeBackupJoinVO> sc = backupSearch.create();
        sc.setParameters(PARENT_ID, parentId);
        return findOneIncludingRemovedBy(sc);
    }

    @Override
    public List<NativeBackupJoinVO> listByImageStoreId(long imageStoreId) {
        SearchCriteria<NativeBackupJoinVO> sc = allBackupsSearch.create();
        sc.setParameters(IMAGE_STORE_ID, imageStoreId);
        sc.setParameters(STATUS, Backup.Status.BackedUp);
        return listBy(sc);
    }

    @Override
    public List<NativeBackupJoinVO> listById(long id) {
        SearchCriteria<NativeBackupJoinVO> sc = allBackupsSearch.create();
        sc.setParameters(ID, id);
        sc.setParameters(STATUS, Backup.Status.BackedUp);
        return listBy(sc);
    }

    @Override
    public List<NativeBackupJoinVO> listByParentId(long parentId) {
        SearchCriteria<NativeBackupJoinVO> sc = allBackupsSearch.create();
        sc.setParameters(PARENT_ID, parentId);
        sc.setParameters(STATUS, Backup.Status.BackedUp);
        return listBy(sc);
    }
}
