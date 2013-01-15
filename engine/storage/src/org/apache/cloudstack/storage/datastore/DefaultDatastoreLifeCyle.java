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
package org.apache.cloudstack.storage.datastore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.cloudstack.engine.subsystem.api.storage.ClusterScope;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreLifeCycle;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreRole;
import org.apache.cloudstack.engine.subsystem.api.storage.ZoneScope;
import org.apache.cloudstack.storage.image.datastore.ImageDataStoreHelper;
import org.apache.cloudstack.storage.volume.datastore.PrimaryDataStoreHelper;
import org.springframework.stereotype.Component;

import edu.emory.mathcs.backport.java.util.Arrays;

public class DefaultDatastoreLifeCyle implements DataStoreLifeCycle {
    @Inject
    PrimaryDataStoreHelper primaryStoreHelper;
    @Inject
    ImageDataStoreHelper imageStoreHelper;
    @Override
    public boolean initialize(DataStore store, Map<String, String> dsInfos) {
        String roles = dsInfos.get("roles");
        List<String> roleArry = Arrays.asList(roles.split(";"));
        List<DataStoreRole> storeRoles = new ArrayList<DataStoreRole>();
        for (String role : roleArry) {
            storeRoles.add(DataStoreRole.getRole(role));
        }

        if (storeRoles.contains(DataStoreRole.Primary)) {
            primaryStoreHelper.createPrimaryDataStore(dsInfos); 
        }
        
        if (storeRoles.contains(DataStoreRole.Image)) {
            imageStoreHelper.createImageDataStore(dsInfos);
        }
        
        //TODO: add more roles
       
        return true;
    }

    @Override
    public boolean attachCluster(DataStore dataStore, ClusterScope scope) {
        if (dataStore.getRole() == DataStoreRole.Primary) {
            primaryStoreHelper.attachCluster(dataStore);
        }
        // TODO Auto-generated method stub
        return true;
    }
    
    @Override
    public boolean attachZone(DataStore dataStore, ZoneScope scope) {
        return false;
    }

    @Override
    public boolean dettach() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean unmanaged() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean maintain() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean cancelMaintain() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean deleteDataStore() {
        // TODO Auto-generated method stub
        return false;
    }

}
