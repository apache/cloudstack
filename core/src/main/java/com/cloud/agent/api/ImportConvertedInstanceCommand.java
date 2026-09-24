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
package com.cloud.agent.api;

import com.cloud.agent.api.to.DataStoreTO;
import com.cloud.agent.api.to.RemoteInstanceTO;
import com.cloud.storage.Storage;

import java.util.List;

public class ImportConvertedInstanceCommand extends Command {

    private RemoteInstanceTO sourceInstance;
    private List<String> destinationStoragePools;
    private List<Storage.StoragePoolType> destinationStoragePoolTypes;
    private DataStoreTO conversionTemporaryLocation;
    private String temporaryConvertUuid;
    private boolean forceConvertToPool;

    public ImportConvertedInstanceCommand() {
    }

    public ImportConvertedInstanceCommand(RemoteInstanceTO sourceInstance,
                                          List<String> destinationStoragePools,
                                          List<Storage.StoragePoolType> destinationStoragePoolTypes,
                                          DataStoreTO conversionTemporaryLocation, String temporaryConvertUuid,
                                          boolean forceConvertToPool) {
        this.sourceInstance = sourceInstance;
        this.destinationStoragePools = destinationStoragePools;
        this.destinationStoragePoolTypes = destinationStoragePoolTypes;
        this.conversionTemporaryLocation = conversionTemporaryLocation;
        this.temporaryConvertUuid = temporaryConvertUuid;
        this.forceConvertToPool = forceConvertToPool;
    }

    public ImportConvertedInstanceCommand(RemoteInstanceTO sourceInstance,
                                          List<String> destinationStoragePools,
                                          DataStoreTO conversionTemporaryLocation, String temporaryConvertUuid,
                                          boolean forceConvertToPool) {
        this(sourceInstance, destinationStoragePools, null, conversionTemporaryLocation, temporaryConvertUuid, forceConvertToPool);
    }

    public RemoteInstanceTO getSourceInstance() {
        return sourceInstance;
    }

    public List<String> getDestinationStoragePools() {
        return destinationStoragePools;
    }

    public List<Storage.StoragePoolType> getDestinationStoragePoolTypes() {
        return destinationStoragePoolTypes;
    }

    public DataStoreTO getConversionTemporaryLocation() {
        return conversionTemporaryLocation;
    }

    public String getTemporaryConvertUuid() {
        return temporaryConvertUuid;
    }

    public boolean isForceConvertToPool() {
        return forceConvertToPool;
    }

    @Override
    public boolean executeInSequence() {
        return false;
    }
}
