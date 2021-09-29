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
package com.cloud.conf.meta_inf.cloudstack.server_allocator;

import com.cloud.agent.manager.allocator.impl.FirstFitAllocator;
import com.cloud.agent.manager.allocator.impl.FirstFitRoutingAllocator;
import com.cloud.agent.manager.allocator.impl.RecreateHostAllocator;
import com.cloud.agent.manager.allocator.impl.TestingAllocator;
import com.cloud.consoleproxy.ConsoleProxyBalanceAllocator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


/**
 * Generated Java based configuration
 * 
 */
@Configuration
public class SpringServerAllocatorContext {


    @Bean("FirstFitRouting")
    public FirstFitRoutingAllocator FirstFitRouting() {
        FirstFitRoutingAllocator bean = new FirstFitRoutingAllocator();
        bean.setName("FirstFitRouting");
        return bean;
    }

    @Bean("testingAllocator")
    public TestingAllocator testingAllocator() {
        return new TestingAllocator();
    }

    @Bean("ConsoleProxyAllocator")
    public ConsoleProxyBalanceAllocator ConsoleProxyAllocator() {
        ConsoleProxyBalanceAllocator bean = new ConsoleProxyBalanceAllocator();
        bean.setName("Balance");
        return bean;
    }

    @Bean("firstFitAllocator")
    public FirstFitAllocator firstFitAllocator() {
        return new FirstFitAllocator();
    }

    @Bean("recreateHostAllocator")
    public RecreateHostAllocator recreateHostAllocator() {
        return new RecreateHostAllocator();
    }

}
