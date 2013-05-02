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

import org.apache.cloudstack.engine.subsystem.api.storage.DataObjectInStore;
import org.apache.cloudstack.engine.subsystem.api.storage.ObjectInDataStoreStateMachine;

import com.cloud.storage.VMTemplateHostVO;
import com.cloud.storage.VMTemplateStorageResourceAssoc.Status;
import com.cloud.utils.db.GenericDao;
import com.cloud.utils.fsm.StateDao;

public interface VMTemplateHostDao extends GenericDao<VMTemplateHostVO, Long>, StateDao<ObjectInDataStoreStateMachine.State, ObjectInDataStoreStateMachine.Event, DataObjectInStore> {
    List<VMTemplateHostVO> listByHostId(long id);

    List<VMTemplateHostVO> listByTemplateId(long templateId);
    
    List<VMTemplateHostVO> listByOnlyTemplateId(long templateId);

    VMTemplateHostVO findByHostTemplate(long hostId, long templateId);
    
    VMTemplateHostVO findByTemplateId(long templateId);

    VMTemplateHostVO findByHostTemplate(long hostId, long templateId, boolean lock);

    List<VMTemplateHostVO> listByHostTemplate(long hostId, long templateId);

    void update(VMTemplateHostVO instance);    

    List<VMTemplateHostVO> listByTemplateStatus(long templateId, VMTemplateHostVO.Status downloadState);

    List<VMTemplateHostVO> listByTemplateStatus(long templateId, long datacenterId, VMTemplateHostVO.Status downloadState);

    List<VMTemplateHostVO> listByTemplateStatus(long templateId, long datacenterId, long podId, VMTemplateHostVO.Status downloadState);

    List<VMTemplateHostVO> listByTemplateStates(long templateId, VMTemplateHostVO.Status... states);

    List<VMTemplateHostVO> listDestroyed(long hostId);

    boolean templateAvailable(long templateId, long hostId);

    List<VMTemplateHostVO> listByZoneTemplate(long dcId, long templateId, boolean readyOnly);

    void deleteByHost(Long hostId);

    VMTemplateHostVO findLocalSecondaryStorageByHostTemplate(long hostId, long templateId);

    List<VMTemplateHostVO> listByTemplateHostStatus(long templateId, long hostId, Status... states);

    List<VMTemplateHostVO> listByState(Status state);
}
