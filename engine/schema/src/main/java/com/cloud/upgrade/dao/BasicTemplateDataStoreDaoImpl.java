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

package com.cloud.upgrade.dao;

import java.util.List;
import java.util.Map;

import javax.naming.ConfigurationException;

import com.cloud.utils.db.Filter;
import org.apache.cloudstack.engine.subsystem.api.storage.DataObjectInStore;
import org.apache.cloudstack.engine.subsystem.api.storage.ObjectInDataStoreStateMachine;
import org.apache.cloudstack.storage.datastore.db.TemplateDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.TemplateDataStoreVO;

import com.cloud.storage.DataStoreRole;
import com.cloud.storage.VMTemplateStorageResourceAssoc;
import com.cloud.template.VirtualMachineTemplate;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

public class BasicTemplateDataStoreDaoImpl extends GenericDaoBase<TemplateDataStoreVO, Long> implements TemplateDataStoreDao {
    private SearchBuilder<TemplateDataStoreVO> templateRoleSearch;
    private SearchBuilder<TemplateDataStoreVO> storeTemplateSearch;

    public BasicTemplateDataStoreDaoImpl() {
        super();
        templateRoleSearch = createSearchBuilder();
        templateRoleSearch.and("template_id", templateRoleSearch.entity().getTemplateId(), SearchCriteria.Op.EQ);
        templateRoleSearch.and("store_role", templateRoleSearch.entity().getDataStoreRole(), SearchCriteria.Op.EQ);
        templateRoleSearch.and("destroyed", templateRoleSearch.entity().getDestroyed(), SearchCriteria.Op.EQ);
        templateRoleSearch.and("state", templateRoleSearch.entity().getState(), SearchCriteria.Op.EQ);
        templateRoleSearch.done();

        storeTemplateSearch = createSearchBuilder();
        storeTemplateSearch.and("template_id", storeTemplateSearch.entity().getTemplateId(), SearchCriteria.Op.EQ);
        storeTemplateSearch.and("store_id", storeTemplateSearch.entity().getDataStoreId(), SearchCriteria.Op.EQ);
        storeTemplateSearch.and("destroyed", storeTemplateSearch.entity().getDestroyed(), SearchCriteria.Op.EQ);
        storeTemplateSearch.done();
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        super.configure(name, params);
        return true;
    }

    @Override
    public List<TemplateDataStoreVO> listByStoreId(long id) {
        return null;
    }

    @Override
    public List<TemplateDataStoreVO> listDestroyed(long storeId) {
        return null;
    }

    @Override
    public List<TemplateDataStoreVO> listActiveOnCache(long id) {
        return null;
    }

    @Override
    public void deletePrimaryRecordsForStore(long id) {

    }

    @Override
    public void deletePrimaryRecordsForTemplate(long templateId) {

    }

    @Override
    public List<TemplateDataStoreVO> listByTemplateStore(long templateId, long storeId) {
        return null;
    }

    @Override
    public List<TemplateDataStoreVO> listByTemplateStoreStatus(long templateId, long storeId, ObjectInDataStoreStateMachine.State... states) {
        return null;
    }

    @Override
    public List<TemplateDataStoreVO> listByTemplateStoreDownloadStatus(long templateId, long storeId, VMTemplateStorageResourceAssoc.Status... status) {
        return null;
    }

    @Override
    public List<TemplateDataStoreVO> listByTemplateZoneDownloadStatus(long templateId, Long zoneId, VMTemplateStorageResourceAssoc.Status... status) {
        return null;
    }

    @Override
    public TemplateDataStoreVO findByTemplateZoneDownloadStatus(long templateId, Long zoneId, VMTemplateStorageResourceAssoc.Status... status) {
        return null;
    }

    @Override
    public TemplateDataStoreVO findByTemplateZoneStagingDownloadStatus(long templateId, Long zoneId, VMTemplateStorageResourceAssoc.Status... status) {
        return null;
    }

    @Override
    public TemplateDataStoreVO findByStoreTemplate(long storeId, long templateId) {
        SearchCriteria<TemplateDataStoreVO> sc = storeTemplateSearch.create();
        sc.setParameters("store_id", storeId);
        sc.setParameters("template_id", templateId);
        sc.setParameters("destroyed", false);
        Filter filter = new Filter(TemplateDataStoreVO.class, "id", false, 0L, 1L);
        List<TemplateDataStoreVO> templates = listBy(sc, filter);
        if ((templates != null) && !templates.isEmpty()) {
            return templates.get(0);
        }
        return null;
    }

    @Override
    public TemplateDataStoreVO findByStoreTemplate(long storeId, long templateId, boolean lock) {
        return null;
    }

    @Override
    public TemplateDataStoreVO findByTemplate(long templateId, DataStoreRole role) {
        SearchCriteria<TemplateDataStoreVO> sc = templateRoleSearch.create();
        sc.setParameters("template_id", templateId);
        sc.setParameters("store_role", role);
        sc.setParameters("destroyed", false);
        return findOneIncludingRemovedBy(sc);
    }

    @Override
    public TemplateDataStoreVO findReadyByTemplate(long templateId, DataStoreRole role) {
        return null;
    }

    @Override
    public TemplateDataStoreVO findByTemplateZone(long templateId, Long zoneId, DataStoreRole role) {
        return null;
    }

    @Override
    public List<TemplateDataStoreVO> listByTemplate(long templateId) {
        return null;
    }

    @Override
    public List<TemplateDataStoreVO> listByTemplateNotBypassed(long templateId, Long... storeIds) {
        return null;
    }

    @Override
    public TemplateDataStoreVO findByTemplateZoneReady(long templateId, Long zoneId) {
        return null;
    }

    @Override
    public void duplicateCacheRecordsOnRegionStore(long storeId) {

    }

    @Override
    public TemplateDataStoreVO findReadyOnCache(long templateId) {
        return null;
    }

    @Override
    public List<TemplateDataStoreVO> listOnCache(long templateId) {
        return null;
    }

    @Override
    public void updateStoreRoleToCachce(long storeId) {

    }

    @Override
    public List<TemplateDataStoreVO> listTemplateDownloadUrls() {
        return null;
    }

    @Override
    public void removeByTemplateStore(long templateId, long imageStoreId) {

    }

    @Override
    public void expireDnldUrlsForZone(Long dcId) {

    }

    @Override
    public List<TemplateDataStoreVO> listByTemplateState(VirtualMachineTemplate.State... states) {
        return null;
    }

    @Override
    public TemplateDataStoreVO createTemplateDirectDownloadEntry(long templateId, Long size) {
        return null;
    }

    @Override
    public TemplateDataStoreVO getReadyBypassedTemplate(long templateId) {
        return null;
    }

    @Override
    public boolean isTemplateMarkedForDirectDownload(long templateId) {
        return false;
    }

    @Override
    public List<TemplateDataStoreVO> listTemplateDownloadUrlsByStoreId(long storeId) {
        return null;
    }

    @Override
    public boolean updateState(ObjectInDataStoreStateMachine.State currentState, ObjectInDataStoreStateMachine.Event event, ObjectInDataStoreStateMachine.State nextState, DataObjectInStore vo, Object data) {
        return false;
    }
}
