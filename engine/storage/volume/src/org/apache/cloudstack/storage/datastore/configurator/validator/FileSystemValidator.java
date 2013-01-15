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
package org.apache.cloudstack.storage.datastore.configurator.validator;

import java.util.List;
import java.util.Map;

import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStoreInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.apache.cloudstack.storage.to.PrimaryDataStoreTO;
import org.apache.cloudstack.storage.to.VolumeTO;

public class FileSystemValidator implements StorageProtocolTransformer {

    @Override
    public boolean normalizeUserInput(Map<String, String> params) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public List<String> getInputParamNames() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public PrimaryDataStoreTO getDataStoreTO(PrimaryDataStoreInfo dataStore) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public VolumeTO getVolumeTO(VolumeInfo volume) {
        // TODO Auto-generated method stub
        return null;
    }


}
