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
package org.apache.cloudstack.storage.volume.test;

import static org.junit.Assert.assertTrue;

import java.util.List;
import javax.inject.Inject;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStoreProvider;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.cloud.dc.dao.ClusterDao;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "classpath:/testContext.xml")
public class ConfiguratorTest {

    @Inject
    List<PrimaryDataStoreProvider> providers;

    @Inject
    ClusterDao clusterDao;

    @Before
    public void setup() {
        /*
         * ClusterVO cluster = new ClusterVO();
         * cluster.setHypervisorType(HypervisorType.XenServer.toString());
         * Mockito
         * .when(clusterDao.findById(Mockito.anyLong())).thenReturn(cluster);
         * try { providerMgr.configure("manager", null); } catch
         * (ConfigurationException e) { // TODO Auto-generated catch block
         * e.printStackTrace(); }
         */
    }

    @Test
    public void testLoadConfigurator() {
        /*
         * for (PrimaryDataStoreConfigurator configurator : configurators) {
         * System.out.println(configurator.getClass().getName()); }
         */
    }

    @Test
    public void testProvider() {
        for (PrimaryDataStoreProvider provider : providers) {
            if (provider.getName().startsWith("default")) {
                assertTrue(true);
            }
        }
    }

    @Test
    public void getProvider() {
        // assertNotNull(providerMgr.getDataStoreProvider("sample primary data store provider"));
    }

    @Test
    public void createDataStore() {
        /*
         * PrimaryDataStoreProvider provider =
         * providerMgr.getDataStoreProvider("sample primary data store provider"
         * ); Map<String, String> params = new HashMap<String, String>();
         * params.put("url", "nfs://localhost/mnt"); params.put("clusterId",
         * "1"); params.put("name", "nfsprimary");
         * assertNotNull(provider.registerDataStore(params));
         */
    }
}
