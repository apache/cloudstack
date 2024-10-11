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
package org.apache.cloudstack.framework.jobs;

import org.apache.cloudstack.framework.config.ConfigDepot;
import org.apache.cloudstack.framework.config.ScopedConfigStorage;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.framework.config.dao.ConfigurationDaoImpl;
import org.apache.cloudstack.framework.config.dao.ConfigurationGroupDao;
import org.apache.cloudstack.framework.config.dao.ConfigurationGroupDaoImpl;
import org.apache.cloudstack.framework.config.dao.ConfigurationSubGroupDao;
import org.apache.cloudstack.framework.config.dao.ConfigurationSubGroupDaoImpl;
import org.apache.cloudstack.framework.config.impl.ConfigDepotImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.cloud.storage.dao.StoragePoolDetailsDaoImpl;

@Configuration
public class AsyncJobManagerTestConfiguration {

    @Bean
    public ConfigDepot configDepot() {
        return new ConfigDepotImpl();
    }

    @Bean
    public ConfigurationDao configDao() {
        return new ConfigurationDaoImpl();
    }

    @Bean
    public ConfigurationGroupDao configGroupDao() {
        return new ConfigurationGroupDaoImpl();
    }

    @Bean
    public ConfigurationSubGroupDao configSubGroupDao() {
        return new ConfigurationSubGroupDaoImpl();
    }

    @Bean
    public ScopedConfigStorage scopedConfigStorage() {
        return new StoragePoolDetailsDaoImpl();
    }

    @Bean
    public AsyncJobTestDashboard testDashboard() {
        return new AsyncJobTestDashboard();
    }
}
