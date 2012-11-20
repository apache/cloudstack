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
package org.apache.cloudstack.storage.volume;

import javax.inject.Inject;

import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStoreInfo;
import org.apache.cloudstack.storage.image.TemplateInfo;
import org.apache.cloudstack.storage.volume.db.TemplatePrimaryDataStoreDao;
import org.apache.cloudstack.storage.volume.db.TemplatePrimaryDataStoreVO;
import org.springframework.stereotype.Component;

import com.cloud.storage.VMTemplateStorageResourceAssoc;
import com.cloud.utils.db.SearchCriteria2;
import com.cloud.utils.db.SearchCriteriaService;
import com.cloud.utils.db.SearchCriteria.Op;

@Component
public class TemplatePrimaryDataStoreManagerImpl implements TemplatePrimaryDataStoreManager {
    @Inject
    TemplatePrimaryDataStoreDao templateStoreDao;

    @Override
    public TemplateOnPrimaryDataStoreObject createTemplateOnPrimaryDataStore(TemplateInfo template, PrimaryDataStoreInfo dataStore) {

        TemplatePrimaryDataStoreVO templateStoreVO = new TemplatePrimaryDataStoreVO(dataStore.getId(), template.getId());
        templateStoreVO = templateStoreDao.persist(templateStoreVO);
        TemplateOnPrimaryDataStoreObject templateStoreObject = new TemplateOnPrimaryDataStoreObject(dataStore, template, templateStoreVO);
        return templateStoreObject;
    }

    @Override
    public TemplateOnPrimaryDataStoreObject findTemplateOnPrimaryDataStore(TemplateInfo template, PrimaryDataStoreInfo dataStore) {
        SearchCriteriaService<TemplatePrimaryDataStoreVO, TemplatePrimaryDataStoreVO> sc = SearchCriteria2.create(TemplatePrimaryDataStoreVO.class);
        sc.addAnd(sc.getEntity().getTemplateId(), Op.EQ, template.getId());
        sc.addAnd(sc.getEntity().getPoolId(), Op.EQ, dataStore.getId());
        sc.addAnd(sc.getEntity().getDownloadState(), Op.EQ, VMTemplateStorageResourceAssoc.Status.DOWNLOADED);
        TemplatePrimaryDataStoreVO templateStoreVO = sc.find();
        TemplateOnPrimaryDataStoreObject templateStoreObject = new TemplateOnPrimaryDataStoreObject(dataStore, template, templateStoreVO);
        return templateStoreObject;
    }
}
