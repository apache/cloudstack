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
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreProviderDao;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreVO;
import org.apache.cloudstack.storage.datastore.provider.PrimaryDataStoreProvider;
import org.springframework.stereotype.Component;

public class DefaultPrimaryDataStoreLifeCycleImpl implements PrimaryDataStoreLifeCycle {
    private final PrimaryDataStoreProvider provider;
    protected PrimaryDataStoreDao dataStoreDao;
    public DefaultPrimaryDataStoreLifeCycleImpl(PrimaryDataStoreProvider provider, PrimaryDataStoreDao dataStoreDao) {
        this.provider = provider;
        this.dataStoreDao = dataStoreDao;
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
    public PrimaryDataStoreInfo registerDataStore(Map<String, String> dsInfos) {
        DataStoreUrlParser parser = new DataStoreUrlParser(dsInfos.get("url"));
        PrimaryDataStoreVO dataStore = new PrimaryDataStoreVO();
        dataStore.setName(dsInfos.get("name"));
        dataStore.setPoolType(parser.getSchema());
        dataStore.setPort(parser.port);
        dataStore.setDataCenterId(Integer.parseInt(dsInfos.get("dcId")));
        dataStore.setHostAddress(parser.getHost());
        dataStore.setPath(parser.getPath());
        dataStore.setStorageProviderId(this.provider.getId());
        dataStore.setClusterId(Long.parseLong(dsInfos.get("clusterId")));
        dataStore = dataStoreDao.persist(dataStore);
        //TODO: add extension point for each data store
        return this.provider.getDataStore(dataStore.getId());
    }

    @Override
    public boolean attach(long scope) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean dettach(long dataStoreId) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean unmanaged(long dataStoreId) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean maintain(long dataStoreId) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean cancelMaintain(long dataStoreId) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean deleteDataStore(long dataStoreId) {
        // TODO Auto-generated method stub
        return false;
    }

}
