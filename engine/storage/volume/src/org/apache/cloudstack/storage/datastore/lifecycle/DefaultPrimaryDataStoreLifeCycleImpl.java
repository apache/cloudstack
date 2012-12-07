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
package org.apache.cloudstack.storage.datastore.lifecycle;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

import javax.inject.Inject;

import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStoreInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStoreLifeCycle;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStoreProvider;
import org.apache.cloudstack.engine.subsystem.api.storage.Scope;
import org.apache.cloudstack.engine.subsystem.api.storage.ScopeType;
import org.apache.cloudstack.storage.datastore.DataStoreStatus;
import org.apache.cloudstack.storage.datastore.PrimaryDataStore;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreProviderDao;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreVO;
import org.springframework.stereotype.Component;

public class DefaultPrimaryDataStoreLifeCycleImpl implements PrimaryDataStoreLifeCycle {
    protected PrimaryDataStoreInfo dataStore;
    protected PrimaryDataStoreDao dataStoreDao;
    public DefaultPrimaryDataStoreLifeCycleImpl(PrimaryDataStoreDao dataStoreDao, PrimaryDataStore dataStore) {
        this.dataStoreDao = dataStoreDao;
        this.dataStore = dataStore;
    }
    
    protected class DataStoreUrlParser {
        private String schema;
        private String host;
        private String path;
        private int port;
        
        public DataStoreUrlParser(String url) {
            try {
                URI uri = new URI(url);
                schema = uri.getScheme();
                host = uri.getHost();
                path = uri.getPath();
                port = (uri.getPort() == -1) ? 0 : uri.getPort();
            } catch (URISyntaxException e) {
               
            }
        }
        
        public String getSchema() {
            return this.schema;
        }
        
        public String getHost() {
            return this.host;
        }
        
        public String getPath() {
            return this.path;
        }
        
        public int getPort() {
            return this.port;
        }
    }
    
    @Override
    public boolean initialize(Map<String, String> dsInfos) {
        DataStoreUrlParser parser = new DataStoreUrlParser(dsInfos.get("url"));
        PrimaryDataStoreVO dataStore = dataStoreDao.findById(this.dataStore.getId());
        dataStore.setName(dsInfos.get("name"));
        dataStore.setPoolType(parser.getSchema());
        dataStore.setPort(parser.port);
        dataStore.setHostAddress(parser.getHost());
        dataStore.setPath(parser.getPath());
        dataStore.setStatus(DataStoreStatus.Initialized);
        dataStoreDao.update(this.dataStore.getId(), dataStore);
        //TODO: add extension point for each data store
        
        this.dataStore = this.dataStore.getProvider().getDataStore(dataStore.getId());
        return true;
    }

    @Override
    public boolean attach(Scope scope) {
        //if (scope.getScopeType() == ScopeType.CLUSTER) 
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
