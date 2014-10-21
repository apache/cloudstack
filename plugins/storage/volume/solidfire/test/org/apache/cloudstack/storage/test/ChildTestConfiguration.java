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
package org.apache.cloudstack.storage.test;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;

import org.apache.cloudstack.storage.image.motion.ImageMotionService;

import com.cloud.agent.AgentManager;
import com.cloud.host.dao.HostDao;

public class ChildTestConfiguration extends TestConfiguration {

    @Override
    @Bean
    public HostDao hostDao() {
        HostDao dao = super.hostDao();
        HostDao nDao = Mockito.spy(dao);
        return nDao;
    }

    @Bean
    public AgentManager agentMgr() {
        return Mockito.mock(AgentManager.class);
    }

    @Bean
    public ImageMotionService imageMotion() {
        return Mockito.mock(ImageMotionService.class);
    }

    /*
     * @Override
     *
     * @Bean public PrimaryDataStoreDao primaryDataStoreDao() { return
     * Mockito.mock(PrimaryDataStoreDaoImpl.class); }
     */
}
