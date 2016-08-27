/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.cloud.storage.dao;

import java.util.List;

import org.apache.cloudstack.engine.subsystem.api.storage.DataObjectInStore;
import org.apache.cloudstack.engine.subsystem.api.storage.ObjectInDataStoreStateMachine;

import com.cloud.storage.VMSnapshotTemplateStoragePoolVO;
import com.cloud.utils.db.GenericDao;
import com.cloud.utils.fsm.StateDao;

public interface VMSnapshotTemplatePoolDao extends GenericDao<VMSnapshotTemplateStoragePoolVO, Long>,
        StateDao<ObjectInDataStoreStateMachine.State, ObjectInDataStoreStateMachine.Event, DataObjectInStore> {
    public List<VMSnapshotTemplateStoragePoolVO> listByPoolId(long id);

    public List<VMSnapshotTemplateStoragePoolVO> listByVmSnapshotId(long vmSnapshotId);

    public VMSnapshotTemplateStoragePoolVO findByPoolVmSnapshot(long poolId, long vmSnapshotId);

    boolean isVmSnapshottemplateAvailableInPool(long vmSnapshotId, long poolId);

}
