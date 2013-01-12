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

import java.util.Map;

import org.apache.cloudstack.framework.async.AsyncCompletionCallback;
import org.apache.cloudstack.storage.EndPoint;
import org.apache.cloudstack.storage.command.CommandResult;
import org.apache.cloudstack.storage.datastore.PrimaryDataStore;
import org.apache.cloudstack.storage.volume.TemplateOnPrimaryDataStoreInfo;
import org.apache.cloudstack.storage.volume.VolumeObject;

public class SolidfirePrimaryDataStoreDriver implements PrimaryDataStoreDriver {


	@Override
	public String grantAccess(VolumeObject vol, EndPoint ep) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean revokeAccess(VolumeObject vol, EndPoint ep) {
		// TODO Auto-generated method stub
		return false;
	}

    @Override
    public long getCapacity() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public long getAvailableCapacity() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public boolean initialize(Map<String, String> params) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean grantAccess(EndPoint ep) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean revokeAccess(EndPoint ep) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void setDataStore(PrimaryDataStore dataStore) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void createVolumeFromBaseImageAsync(VolumeObject volume, TemplateOnPrimaryDataStoreInfo template, AsyncCompletionCallback<CommandResult> callback) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void createVolumeAsync(VolumeObject vol, AsyncCompletionCallback<CommandResult> callback) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void deleteVolumeAsync(VolumeObject vo, AsyncCompletionCallback<CommandResult> callback) {
        // TODO Auto-generated method stub
        
    }

}
