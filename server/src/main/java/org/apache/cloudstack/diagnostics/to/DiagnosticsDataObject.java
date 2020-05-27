//
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
package org.apache.cloudstack.diagnostics.to;

import org.apache.cloudstack.engine.subsystem.api.storage.DataObject;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.ObjectInDataStoreStateMachine;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.to.DataObjectType;
import com.cloud.agent.api.to.DataTO;

public class DiagnosticsDataObject implements DataObject {
    private DataTO dataTO;
    private DataStore dataStore;

    public DiagnosticsDataObject(DataTO dataTO, DataStore dataStore) {
        this.dataTO = dataTO;
        this.dataStore = dataStore;
    }

    @Override
    public long getId() {
        return 0;
    }

    @Override
    public String getUri() {
        return null;
    }

    @Override
    public DataTO getTO() {
        return dataTO;
    }

    @Override
    public DataStore getDataStore() {
        return dataStore;
    }

    @Override
    public Long getSize() {
        return null;
    }

    @Override
    public DataObjectType getType() {
        return dataTO.getObjectType();
    }

    @Override
    public String getUuid() {
        return null;
    }

    @Override
    public boolean delete() {
        return false;
    }

    @Override
    public void processEvent(ObjectInDataStoreStateMachine.Event event) {
    }

    @Override
    public void processEvent(ObjectInDataStoreStateMachine.Event event, Answer answer) {
    }

    @Override
    public void incRefCount() {
    }

    @Override
    public void decRefCount() {
    }

    @Override
    public Long getRefCount() {
        return null;
    }
}
