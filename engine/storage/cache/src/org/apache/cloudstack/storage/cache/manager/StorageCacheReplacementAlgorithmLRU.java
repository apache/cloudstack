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
import com.cloud.configuration.Config;
import com.cloud.utils.DateUtil;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria2;
import com.cloud.utils.db.SearchCriteriaService;

import org.apache.cloudstack.engine.subsystem.api.storage.*;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.storage.datastore.db.SnapshotDataStoreVO;
import org.apache.cloudstack.storage.datastore.db.TemplateDataStoreVO;
import org.apache.cloudstack.storage.datastore.db.VolumeDataStoreVO;

import org.apache.commons.lang.math.NumberUtils;

import java.util.Calendar;
import java.util.Date;

import javax.annotation.PostConstruct;
import javax.inject.Inject;



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
        unusedTimeInterval = NumbersUtil.parseInt(configDao.getValue(Config.StorageCacheReplacementLRUTimeInterval.key()), 30);
    }

    public void setUnusedTimeInterval(Integer interval) {
        unusedTimeInterval = interval;
    }

    @Override
    public DataObject chooseOneToBeReplaced(DataStore store) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(DateUtil.now());
        cal.add(Calendar.DAY_OF_MONTH, -unusedTimeInterval.intValue());
        Date bef = cal.getTime();

        SearchCriteriaService<TemplateDataStoreVO, TemplateDataStoreVO> sc = SearchCriteria2.create(TemplateDataStoreVO.class);
        sc.addAnd(sc.getEntity().getLastUpdated(), SearchCriteria.Op.LT, bef);
        sc.addAnd(sc.getEntity().getState(), SearchCriteria.Op.EQ, ObjectInDataStoreStateMachine.State.Ready);
        sc.addAnd(sc.getEntity().getDataStoreId(), SearchCriteria.Op.EQ, store.getId());
        sc.addAnd(sc.getEntity().getDataStoreRole(), SearchCriteria.Op.EQ, store.getRole());
        sc.addAnd(sc.getEntity().getRefCnt(), SearchCriteria.Op.EQ, 0);
        TemplateDataStoreVO template = sc.find();
        if (template != null) {
            return templateFactory.getTemplate(template.getTemplateId(), store);
        }

        SearchCriteriaService<VolumeDataStoreVO, VolumeDataStoreVO> volSc = SearchCriteria2.create(VolumeDataStoreVO.class);
        volSc.addAnd(volSc.getEntity().getLastUpdated(), SearchCriteria.Op.LT, bef);
        volSc.addAnd(volSc.getEntity().getState(), SearchCriteria.Op.EQ, ObjectInDataStoreStateMachine.State.Ready);
        volSc.addAnd(volSc.getEntity().getDataStoreId(), SearchCriteria.Op.EQ, store.getId());
        volSc.addAnd(volSc.getEntity().getRefCnt(), SearchCriteria.Op.EQ, 0);
        VolumeDataStoreVO volume = volSc.find();
        if (volume != null) {
            return volumeFactory.getVolume(volume.getVolumeId(), store);
        }

        SearchCriteriaService<SnapshotDataStoreVO, SnapshotDataStoreVO> snapshotSc = SearchCriteria2.create(SnapshotDataStoreVO.class);
        snapshotSc.addAnd(snapshotSc.getEntity().getLastUpdated(), SearchCriteria.Op.LT, bef);
        snapshotSc.addAnd(snapshotSc.getEntity().getState(), SearchCriteria.Op.EQ, ObjectInDataStoreStateMachine.State.Ready);
        snapshotSc.addAnd(snapshotSc.getEntity().getDataStoreId(), SearchCriteria.Op.EQ, store.getId());
        snapshotSc.addAnd(snapshotSc.getEntity().getRole(), SearchCriteria.Op.EQ, store.getRole());
        snapshotSc.addAnd(snapshotSc.getEntity().getRefCnt(), SearchCriteria.Op.EQ, 0);
        SnapshotDataStoreVO snapshot = snapshotSc.find();
        if (snapshot != null) {
            return snapshotFactory.getSnapshot(snapshot.getSnapshotId(), store);
        }

        return null;
    }
}
