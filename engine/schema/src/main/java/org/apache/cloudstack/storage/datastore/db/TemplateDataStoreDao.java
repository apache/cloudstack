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
package org.apache.cloudstack.storage.datastore.db;

import java.util.List;

import org.apache.cloudstack.engine.subsystem.api.storage.DataObjectInStore;
import org.apache.cloudstack.engine.subsystem.api.storage.ObjectInDataStoreStateMachine;
import org.apache.cloudstack.engine.subsystem.api.storage.ObjectInDataStoreStateMachine.State;

import com.cloud.storage.DataStoreRole;
import com.cloud.storage.VMTemplateStorageResourceAssoc;
import com.cloud.storage.VMTemplateStorageResourceAssoc.Status;
import com.cloud.template.VirtualMachineTemplate;
import com.cloud.utils.db.GenericDao;
import com.cloud.utils.fsm.StateDao;

public interface TemplateDataStoreDao extends GenericDao<TemplateDataStoreVO, Long>,
        StateDao<ObjectInDataStoreStateMachine.State, ObjectInDataStoreStateMachine.Event, DataObjectInStore> {

    List<TemplateDataStoreVO> listByStoreId(long id);

    List<TemplateDataStoreVO> listDestroyed(long storeId);

    List<TemplateDataStoreVO> listActiveOnCache(long id);

    void deletePrimaryRecordsForStore(long id);

    void deletePrimaryRecordsForTemplate(long templateId);

    List<TemplateDataStoreVO> listByTemplateStore(long templateId, long storeId);

    List<TemplateDataStoreVO> listByTemplateStoreStatus(long templateId, long storeId, State... states);

    List<TemplateDataStoreVO> listByTemplateStoreDownloadStatus(long templateId, long storeId, VMTemplateStorageResourceAssoc.Status... status);

    List<TemplateDataStoreVO> listByTemplateZoneDownloadStatus(long templateId, Long zoneId, VMTemplateStorageResourceAssoc.Status... status);

    TemplateDataStoreVO findByTemplateZoneDownloadStatus(long templateId, Long zoneId, VMTemplateStorageResourceAssoc.Status... status);

    TemplateDataStoreVO findByTemplateZoneStagingDownloadStatus(long templateId, Long zoneId, Status... status);

    TemplateDataStoreVO findByStoreTemplate(long storeId, long templateId);

    TemplateDataStoreVO findByStoreTemplate(long storeId, long templateId, boolean lock);

    TemplateDataStoreVO findByTemplate(long templateId, DataStoreRole role);

    TemplateDataStoreVO findReadyByTemplate(long templateId, DataStoreRole role);

    TemplateDataStoreVO findByTemplateZone(long templateId, Long zoneId, DataStoreRole role);

    List<TemplateDataStoreVO> listByTemplate(long templateId);

    List<TemplateDataStoreVO> listByTemplateNotBypassed(long templateId);

    TemplateDataStoreVO findByTemplateZoneReady(long templateId, Long zoneId);

    void duplicateCacheRecordsOnRegionStore(long storeId);

    TemplateDataStoreVO findReadyOnCache(long templateId);

    List<TemplateDataStoreVO> listOnCache(long templateId);

    void updateStoreRoleToCachce(long storeId);

    List<TemplateDataStoreVO> listTemplateDownloadUrls();

    void removeByTemplateStore(long templateId, long imageStoreId);

    void expireDnldUrlsForZone(Long dcId);

    List<TemplateDataStoreVO> listByTemplateState(VirtualMachineTemplate.State... states);

    TemplateDataStoreVO createTemplateDirectDownloadEntry(long templateId, Long size);

    TemplateDataStoreVO getReadyBypassedTemplate(long templateId);

    boolean isTemplateMarkedForDirectDownload(long templateId);

    List<TemplateDataStoreVO> listTemplateDownloadUrlsByStoreId(long storeId);
}
