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
package org.apache.cloudstack.storage.datastore.provider;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreLifeCycle;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreManager;
import org.apache.cloudstack.engine.subsystem.api.storage.HypervisorHostListener;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStoreDriver;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStoreProvider;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.driver.ElastistorPrimaryDataStoreDriver;
import org.apache.cloudstack.storage.datastore.lifecycle.ElastistorPrimaryDataStoreLifeCycle;
import org.apache.cloudstack.storage.datastore.util.ElastistorUtil;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.agent.AgentManager;
import com.cloud.alert.AlertManager;
import com.cloud.storage.dao.StoragePoolHostDao;
import com.cloud.utils.component.ComponentContext;

@Component
public class ElastistorPrimaryDataStoreProvider implements PrimaryDataStoreProvider {

    private static final Logger s_logger = Logger.getLogger(DefaultHostListener.class);

    //these classes will be injected by spring
    private ElastistorPrimaryDataStoreLifeCycle lifecycle;
    private PrimaryDataStoreDriver driver;
    private HypervisorHostListener listener;

    // these params will be initialized with respective values given in spring-storage-volume-cloudbyte-context.xml bean for the elastistor porpose only.
    private String esmanagementip;
    private String esapikey;
    private String esaccountid;
    private String espoolid;
    private String esdefaultgateway;
    private String essubnet;
    private String estntinterface;

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

        lifecycle = ComponentContext.inject(ElastistorPrimaryDataStoreLifeCycle.class);
        driver = ComponentContext.inject(ElastistorPrimaryDataStoreDriver.class);
        listener = ComponentContext.inject(ElastistorHostListener.class);

        ElastistorUtil.setElastistorAccountId(esaccountid);
        ElastistorUtil.setElastistorApiKey(esapikey);
        ElastistorUtil.setElastistorManagementIp(esmanagementip);
        ElastistorUtil.setElastistorPoolId(espoolid);
        ElastistorUtil.setElastistorGateway(esdefaultgateway);
        ElastistorUtil.setElastistorInterface(estntinterface);
        ElastistorUtil.setElastistorSubnet(essubnet);

        return true;
    }

    @Override
    public Set<DataStoreProviderType> getTypes() {
        Set<DataStoreProviderType> types = new HashSet<DataStoreProviderType>();

        types.add(DataStoreProviderType.PRIMARY);

        return types;
    }
    public String getEspoolid() {
        return espoolid;
    }

    public void setEspoolid(String espoolid) {
        this.espoolid = espoolid;
    }

    public String getEsmanagementip() {
        return esmanagementip;
    }

    public void setEsmanagementip(String esmanagementip) {
        this.esmanagementip = esmanagementip;
    }

    public String getEsaccountid() {
        return esaccountid;
    }

    public void setEsaccountid(String esaccountid) {
        this.esaccountid = esaccountid;
    }

    public String getEsapikey() {
        return esapikey;
    }

    public void setEsapikey(String esapikey) {
        this.esapikey = esapikey;
    }

    public String getesdefaultgateway() {
        return esdefaultgateway;
    }

    public void setesdefaultgateway(String esdefaultgateway) {
        this.esdefaultgateway = esdefaultgateway;
    }
    public String getEssubnet() {
        return essubnet;
    }

    public void setEssubnet(String essubnet) {
        this.essubnet = essubnet;
    }

    public String getEstntinterface(){
     return estntinterface;
    }

    public void setEstntinterface(String estntinterface){
      this.estntinterface = estntinterface;
    }
}
