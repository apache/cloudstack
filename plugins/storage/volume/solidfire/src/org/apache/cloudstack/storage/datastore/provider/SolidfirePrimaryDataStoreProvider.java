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
package org.apache.cloudstack.storage.datastore.provider;

import java.util.Map;
import java.util.Set;

import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreDriver;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreLifeCycle;
import org.apache.cloudstack.engine.subsystem.api.storage.HypervisorHostListener;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStoreProvider;
import org.springframework.stereotype.Component;

@Component
public class SolidfirePrimaryDataStoreProvider implements PrimaryDataStoreProvider {
    private final String name = "Solidfire Primary Data Store Provider";

    public SolidfirePrimaryDataStoreProvider() {

        // TODO Auto-generated constructor stub
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public DataStoreLifeCycle getDataStoreLifeCycle() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public DataStoreDriver getDataStoreDriver() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public HypervisorHostListener getHostListener() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean configure(Map<String, Object> params) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public Set<DataStoreProviderType> getTypes() {
        // TODO Auto-generated method stub
        return null;
    }

}
