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
package com.cloud.conf.com.cloud.upgrade;

import com.cloud.upgrade.DatabaseUpgradeChecker;
import com.cloud.upgrade.dao.VersionDaoImpl;
import com.cloud.utils.component.ComponentContext;
import com.cloud.utils.component.ComponentInstantiationPostProcessor;
import com.cloud.utils.db.TransactionContextBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;


/**
 * Generated Java based configuration
 * 
 */
@Configuration
public class DatabaseCreatorContext {


    @Bean("instantiatePostProcessor")
    public ComponentInstantiationPostProcessor instantiatePostProcessor(
        @Qualifier("transactionContextBuilder")
        TransactionContextBuilder transactionContextBuilder) {
        ComponentInstantiationPostProcessor bean = new ComponentInstantiationPostProcessor();
        ArrayList list0 = new ArrayList();
        list0 .add(transactionContextBuilder);
        bean.setInterceptors(list0);
        return bean;
    }

    @Bean("databaseUpgradeChecker")
    public DatabaseUpgradeChecker databaseUpgradeChecker() {
        return new DatabaseUpgradeChecker();
    }

    @Bean("componentContext")
    public ComponentContext componentContext() {
        return new ComponentContext();
    }

    @Bean("transactionContextBuilder")
    public TransactionContextBuilder transactionContextBuilder() {
        return new TransactionContextBuilder();
    }

    @Bean("versionDaoImpl")
    public VersionDaoImpl versionDaoImpl() {
        return new VersionDaoImpl();
    }

}
