/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cloudstack.storage.cache.manager;

import java.util.Calendar;
import java.util.Date;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.apache.cloudstack.engine.subsystem.api.storage.DataObject;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.ObjectInDataStoreStateMachine;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotDataFactory;
import org.apache.cloudstack.engine.subsystem.api.storage.TemplateDataFactory;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeDataFactory;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.storage.datastore.db.SnapshotDataStoreVO;
import org.apache.cloudstack.storage.datastore.db.TemplateDataStoreVO;
import org.apache.cloudstack.storage.datastore.db.VolumeDataStoreVO;

import com.cloud.configuration.Config;
import com.cloud.utils.DateUtil;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.db.QueryBuilder;
import com.cloud.utils.db.SearchCriteria;

public class StorageCacheReplacementAlgorithmLRU implements StorageCacheReplacementAlgorithm {
    @Inject
    ConfigurationDao configDao;
    @Inject
    TemplateDataFactory templateFactory;
    @Inject
    VolumeDataFactory volumeFactory;
    @Inject
    SnapshotDataFactory snapshotFactory;

    Integer unusedTimeInterval;

    public StorageCacheReplacementAlgorithmLRU() {

    }

    @PostConstruct
    public void initialize() {
        /* Avoid using configDao at this time, we can't be sure that the database is already upgraded
         * and there might be fatal errors when using a dao.
         */
        //unusedTimeInterval = NumbersUtil.parseInt(configDao.getValue(Config.StorageCacheReplacementLRUTimeInterval.key()), 30);
    }

    public void setUnusedTimeInterval(Integer interval) {
        unusedTimeInterval = interval;
    }

    @Override
    public DataObject chooseOneToBeReplaced(DataStore store) {
        if (unusedTimeInterval == null) {
            unusedTimeInterval = NumbersUtil.parseInt(configDao.getValue(Config.StorageCacheReplacementLRUTimeInterval.key()), 30);
        }
        Calendar cal = Calendar.getInstance();
        cal.setTime(DateUtil.now());
        cal.add(Calendar.DAY_OF_MONTH, -unusedTimeInterval.intValue());
        Date bef = cal.getTime();

        QueryBuilder<TemplateDataStoreVO> sc = QueryBuilder.create(TemplateDataStoreVO.class);
        sc.and(sc.entity().getLastUpdated(), SearchCriteria.Op.LT, bef);
        sc.and(sc.entity().getState(), SearchCriteria.Op.EQ, ObjectInDataStoreStateMachine.State.Ready);
        sc.and(sc.entity().getDataStoreId(), SearchCriteria.Op.EQ, store.getId());
        sc.and(sc.entity().getDataStoreRole(), SearchCriteria.Op.EQ, store.getRole());
        sc.and(sc.entity().getRefCnt(), SearchCriteria.Op.EQ, 0);
        TemplateDataStoreVO template = sc.find();
        if (template != null) {
            return templateFactory.getTemplate(template.getTemplateId(), store);
        }

        QueryBuilder<VolumeDataStoreVO> volSc = QueryBuilder.create(VolumeDataStoreVO.class);
        volSc.and(volSc.entity().getLastUpdated(), SearchCriteria.Op.LT, bef);
        volSc.and(volSc.entity().getState(), SearchCriteria.Op.EQ, ObjectInDataStoreStateMachine.State.Ready);
        volSc.and(volSc.entity().getDataStoreId(), SearchCriteria.Op.EQ, store.getId());
        volSc.and(volSc.entity().getRefCnt(), SearchCriteria.Op.EQ, 0);
        VolumeDataStoreVO volume = volSc.find();
        if (volume != null) {
            return volumeFactory.getVolume(volume.getVolumeId(), store);
        }

        QueryBuilder<SnapshotDataStoreVO> snapshotSc = QueryBuilder.create(SnapshotDataStoreVO.class);
        snapshotSc.and(snapshotSc.entity().getLastUpdated(), SearchCriteria.Op.LT, bef);
        snapshotSc.and(snapshotSc.entity().getState(), SearchCriteria.Op.EQ, ObjectInDataStoreStateMachine.State.Ready);
        snapshotSc.and(snapshotSc.entity().getDataStoreId(), SearchCriteria.Op.EQ, store.getId());
        snapshotSc.and(snapshotSc.entity().getRole(), SearchCriteria.Op.EQ, store.getRole());
        snapshotSc.and(snapshotSc.entity().getRefCnt(), SearchCriteria.Op.EQ, 0);
        SnapshotDataStoreVO snapshot = snapshotSc.find();
        if (snapshot != null) {
            return snapshotFactory.getSnapshot(snapshot.getSnapshotId(), store);
        }

        return null;
    }
}
