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

import com.cloud.storage.VMTemplateStoragePoolVO;
import com.cloud.utils.db.GenericDao;
import com.cloud.utils.fsm.StateDao;

public interface VMTemplatePoolDao extends GenericDao<VMTemplateStoragePoolVO, Long>,
        StateDao<ObjectInDataStoreStateMachine.State, ObjectInDataStoreStateMachine.Event, DataObjectInStore> {
    List<VMTemplateStoragePoolVO> listByPoolId(long id);

    List<VMTemplateStoragePoolVO> listByTemplateId(long templateId);

    VMTemplateStoragePoolVO findByPoolTemplate(long poolId, long templateId, String configuration);

    List<VMTemplateStoragePoolVO> listByPoolIdAndState(long poolId, ObjectInDataStoreStateMachine.State state);

    List<VMTemplateStoragePoolVO> listByTemplateStatus(long templateId, VMTemplateStoragePoolVO.Status downloadState);

    List<VMTemplateStoragePoolVO> listByTemplateStatus(long templateId, VMTemplateStoragePoolVO.Status downloadState, long poolId);

    List<VMTemplateStoragePoolVO> listByTemplateStatus(long templateId, long datacenterId, VMTemplateStoragePoolVO.Status downloadState);

    List<VMTemplateStoragePoolVO> listByTemplateStatus(long templateId, long datacenterId, long podId, VMTemplateStoragePoolVO.Status downloadState);

    List<VMTemplateStoragePoolVO> listByTemplateStates(long templateId, VMTemplateStoragePoolVO.Status... states);

    boolean templateAvailable(long templateId, long poolId);

    VMTemplateStoragePoolVO findByHostTemplate(Long hostId, Long templateId, String configuration);

    VMTemplateStoragePoolVO findByPoolPath(Long poolId, String path);

    List<VMTemplateStoragePoolVO> listByTemplatePath(String templatePath);
}
