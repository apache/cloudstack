//Licensed to the Apache Software Foundation (ASF) under one
//or more contributor license agreements.  See the NOTICE file
//distributed with this work for additional information
//regarding copyright ownership.  The ASF licenses this file
//to you under the Apache License, Version 2.0 (the
//"License"); you may not use this file except in compliance
//the License.  You may obtain a copy of the License at
//
//http://www.apache.org/licenses/LICENSE-2.0
//
//Unless required by applicable law or agreed to in writing,
//software distributed under the License is distributed on an
//"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
//KIND, either express or implied.  See the License for the
//specific language governing permissions and limitations
//under the License.
package org.apache.cloudstack.backup.dao;

import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.UpdateBuilder;
import org.apache.cloudstack.backup.InternalBackupDataStoreVO;

import javax.annotation.PostConstruct;
import java.util.List;

public class InternalBackupDataStoreDaoImpl extends GenericDaoBase<InternalBackupDataStoreVO, Long> implements InternalBackupDataStoreDao {

    private SearchBuilder<InternalBackupDataStoreVO> backupSearch;

    private static final String BACKUP_ID = "backup_id";
    private static final String VOLUME_ID = "volume_id";

    @PostConstruct
    protected void init() {
        backupSearch = createSearchBuilder();
        backupSearch.and(BACKUP_ID, backupSearch.entity().getBackupId(), SearchCriteria.Op.EQ);
        backupSearch.and(VOLUME_ID, backupSearch.entity().getVolumeId(), SearchCriteria.Op.EQ);
        backupSearch.done();
    }

    @Override
    public List<InternalBackupDataStoreVO> listByBackupId(long backupId) {
        SearchCriteria<InternalBackupDataStoreVO> sc = backupSearch.create();
        sc.setParameters(BACKUP_ID, backupId);
        return listBy(sc);
    }

    @Override
    public InternalBackupDataStoreVO findByBackupIdAndVolumeId(long backupId, long volumeId) {
        SearchCriteria<InternalBackupDataStoreVO> sc = backupSearch.create();
        sc.setParameters(BACKUP_ID, backupId);
        sc.setParameters(VOLUME_ID, volumeId);
        return findOneBy(sc);
    }

    @Override
    public void expungeByBackupId(long backupId) {
        SearchCriteria<InternalBackupDataStoreVO> sc = backupSearch.create();
        sc.setParameters(BACKUP_ID, backupId);
        expunge(sc);
    }

    @Override
    public void updateVolumeId(long oldVolumeId, long newVolumeId) {
        SearchCriteria<InternalBackupDataStoreVO> sc = backupSearch.create();
        sc.setParameters(VOLUME_ID, oldVolumeId);
        InternalBackupDataStoreVO delta = createForUpdate();
        delta.setVolumeId(newVolumeId);
        UpdateBuilder ub = getUpdateBuilder(delta);
        update(ub, sc, null);
    }
}
