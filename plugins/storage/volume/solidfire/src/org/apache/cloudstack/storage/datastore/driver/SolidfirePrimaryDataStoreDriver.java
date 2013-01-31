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
package org.apache.cloudstack.storage.datastore.driver;

import java.util.Set;

import org.apache.cloudstack.engine.subsystem.api.storage.CommandResult;
import org.apache.cloudstack.engine.subsystem.api.storage.CopyCommandResult;
import org.apache.cloudstack.engine.subsystem.api.storage.CreateCmdResult;
import org.apache.cloudstack.engine.subsystem.api.storage.DataObject;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.EndPoint;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotInfo;
import org.apache.cloudstack.framework.async.AsyncCompletionCallback;
import org.apache.cloudstack.storage.volume.PrimaryDataStoreDriver;

public class SolidfirePrimaryDataStoreDriver implements PrimaryDataStoreDriver {

    @Override
    public String grantAccess(DataObject data, EndPoint ep) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean revokeAccess(DataObject data, EndPoint ep) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public Set<DataObject> listObjects(DataStore store) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void createAsync(DataObject data, AsyncCompletionCallback<CreateCmdResult> callback) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void deleteAsync(DataObject data, AsyncCompletionCallback<CommandResult> callback) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void copyAsync(DataObject srcdata, DataObject destData, AsyncCompletionCallback<CopyCommandResult> callback) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public boolean canCopy(DataObject srcData, DataObject destData) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void takeSnapshot(SnapshotInfo snapshot, AsyncCompletionCallback<CommandResult> callback) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void revertSnapshot(SnapshotInfo snapshot, AsyncCompletionCallback<CommandResult> callback) {
        // TODO Auto-generated method stub
        
    }


}
