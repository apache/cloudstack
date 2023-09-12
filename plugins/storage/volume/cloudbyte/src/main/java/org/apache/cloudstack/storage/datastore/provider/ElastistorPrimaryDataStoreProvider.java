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
//

package org.apache.cloudstack.storage.datastore.provider;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreLifeCycle;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreManager;
import org.apache.cloudstack.engine.subsystem.api.storage.HypervisorHostListener;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStoreDriver;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStoreProvider;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.framework.config.impl.ConfigurationVO;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.driver.ElastistorPrimaryDataStoreDriver;
import org.apache.cloudstack.storage.datastore.lifecycle.ElastistorPrimaryDataStoreLifeCycle;
import org.apache.cloudstack.storage.datastore.util.ElastistorUtil;

import com.cloud.agent.AgentManager;
import com.cloud.alert.AlertManager;
import com.cloud.storage.dao.StoragePoolHostDao;
import com.cloud.utils.component.ComponentContext;

/**
 * This is the starting point of the elastistor storage plugin. This bean will
 * be detected by Spring container & initialized. This will be one of the
 * providers available via {@link DataStoreProviderManagerImpl} object.
 */
@Component
public class ElastistorPrimaryDataStoreProvider implements PrimaryDataStoreProvider {

    private static final Logger s_logger = Logger.getLogger(DefaultHostListener.class);

    // these classes will be injected by spring
    private ElastistorPrimaryDataStoreLifeCycle lifecycle;
    private PrimaryDataStoreDriver driver;
    private HypervisorHostListener listener;


    @Inject
    AgentManager agentMgr;
    @Inject
    DataStoreManager dataStoreMgr;
    @Inject
    AlertManager alertMgr;
    @Inject
    StoragePoolHostDao storagePoolHostDao;
    @Inject
    PrimaryDataStoreDao primaryStoreDao;
    @Inject
    ConfigurationDao configurationDao;

    @Override
    public String getName() {
        return ElastistorUtil.ES_PROVIDER_NAME;
    }

    @Override
    public DataStoreLifeCycle getDataStoreLifeCycle() {
        return lifecycle;
    }

    @Override
    public PrimaryDataStoreDriver getDataStoreDriver() {
        return driver;
    }

    @Override
    public HypervisorHostListener getHostListener() {
        return listener;
    }

    @Override
    public boolean configure(Map<String, Object> params) {

        s_logger.info("Will configure elastistor's lifecycle, driver, listener & global configurations.");

        lifecycle = ComponentContext.inject(ElastistorPrimaryDataStoreLifeCycle.class);
        driver = ComponentContext.inject(ElastistorPrimaryDataStoreDriver.class);
        listener = ComponentContext.inject(ElastistorHostListener.class);

        // insert new cloudbyte global config to the configuration table
        setCloudbyteGlobalConfiguration();

        // set the injected configuration object in elastistor util class too!!!
        ElastistorUtil.setConfigurationDao(configurationDao);

        s_logger.info("Successfully configured elastistor's lifecycle, driver, listener & global configurations.");

        return true;
    }

    @Override
    public Set<DataStoreProviderType> getTypes() {
        Set<DataStoreProviderType> types = new HashSet<DataStoreProviderType>();

        types.add(DataStoreProviderType.PRIMARY);

        return types;
    }

    private void setCloudbyteGlobalConfiguration() {

        if (configurationDao.findByName("cloudbyte.management.ip") == null) {
            ConfigurationVO managementIP = new ConfigurationVO("Advanced", "DEFAULT", "management-server",
                    "cloudbyte.management.ip", null, "configure the cloudbyte elasticenter management IP");

            configurationDao.persist(managementIP);
        }

        if (configurationDao.findByName("cloudbyte.management.apikey") == null) {
            ConfigurationVO managementApiKey = new ConfigurationVO("Advanced", "DEFAULT", "management-server",
                    "cloudbyte.management.apikey", null, "configure the cloudbyte elasticenter management API KEY");

            configurationDao.persist(managementApiKey);
        }
    }
}
