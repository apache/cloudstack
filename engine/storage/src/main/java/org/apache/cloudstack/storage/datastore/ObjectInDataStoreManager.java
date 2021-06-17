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
package org.apache.cloudstack.storage.datastore;

import org.apache.cloudstack.engine.subsystem.api.storage.DataObject;
import org.apache.cloudstack.engine.subsystem.api.storage.DataObjectInStore;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.ObjectInDataStoreStateMachine.Event;

import com.cloud.agent.api.to.DataObjectType;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.storage.DataStoreRole;
import com.cloud.utils.fsm.NoTransitionException;

public interface ObjectInDataStoreManager {
    DataObject create(DataObject dataObj, DataStore dataStore);

    boolean delete(DataObject dataObj);

    boolean deleteIfNotReady(DataObject dataObj);

    DataObject get(DataObject dataObj, DataStore store, String configuration);

    boolean update(DataObject vo, Event event) throws NoTransitionException, ConcurrentOperationException;

    DataObjectInStore findObject(long objId, DataObjectType type, long dataStoreId, DataStoreRole role, String deployAsIsConfiguration);

    DataObjectInStore findObject(DataObject obj, DataStore store);

    DataStore findStore(long objId, DataObjectType type, DataStoreRole role);
}
