// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// the License.  You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

package com.cloud.network.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.cloud.agent.AgentManager;
import com.cloud.agent.MockAgentManagerImpl;
import com.cloud.network.NetworkManager;
import com.cloud.projects.MockProjectManagerImpl;
import com.cloud.projects.ProjectManager;
import com.cloud.user.AccountManager;
import com.cloud.user.DomainManager;
import com.cloud.user.MockAccountManagerImpl;
import com.cloud.user.MockDomainManagerImpl;
import com.cloud.utils.component.ComponentContext;
import com.cloud.vm.MockUserVmManagerImpl;
import com.cloud.vm.MockVirtualMachineManagerImpl;
import com.cloud.vm.UserVmManager;
import com.cloud.vm.VirtualMachineManager;
import com.cloud.vpc.MockNetworkManagerImpl;

@Configuration
public class SecurityGroupManagerTestConfiguration {

    @Bean 
    public AgentManager agentManager() {
        return ComponentContext.inject(MockAgentManagerImpl.class);
    }

    @Bean
    public VirtualMachineManager virtualMachineManager(){
        return ComponentContext.inject(MockVirtualMachineManagerImpl.class);
    }

    @Bean
    public UserVmManager userVmManager() {
        return ComponentContext.inject(MockUserVmManagerImpl.class);
    }

    @Bean
    public NetworkManager networkManager(){
        return ComponentContext.inject(MockNetworkManagerImpl.class);
    }

    @Bean
    public AccountManager accountManager() {
        return ComponentContext.inject(MockAccountManagerImpl.class);
    }

    @Bean
    public DomainManager domainManager() {
        return ComponentContext.inject(MockDomainManagerImpl.class);
    }

    @Bean
    public ProjectManager projectManager() {
        return ComponentContext.inject(MockProjectManagerImpl.class);
    }
}
