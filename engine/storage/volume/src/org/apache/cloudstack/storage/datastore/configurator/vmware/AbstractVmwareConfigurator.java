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
package org.apache.cloudstack.storage.datastore.configurator.vmware;

import javax.inject.Inject;

import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStoreLifeCycle;
import org.apache.cloudstack.storage.datastore.configurator.AbstractPrimaryDataStoreConfigurator;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.driver.DefaultPrimaryDataStoreDriverImpl;
import org.apache.cloudstack.storage.datastore.driver.PrimaryDataStoreDriver;
import org.apache.cloudstack.storage.datastore.lifecycle.DefaultVmwarePrimaryDataStoreLifeCycle;
import com.cloud.hypervisor.Hypervisor.HypervisorType;

public abstract class AbstractVmwareConfigurator extends AbstractPrimaryDataStoreConfigurator {

    @Inject
    PrimaryDataStoreDao dataStoreDao;
    @Override
    public HypervisorType getSupportedHypervisor() {
        return HypervisorType.VMware;
    }
    
    @Override
    protected PrimaryDataStoreLifeCycle getLifeCycle() {
        return new DefaultVmwarePrimaryDataStoreLifeCycle(dataStoreDao);
    }
    
    @Override
    protected PrimaryDataStoreDriver getDriver() {
        return new DefaultPrimaryDataStoreDriverImpl();
    }
}
