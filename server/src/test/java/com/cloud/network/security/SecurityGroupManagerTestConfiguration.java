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

import com.cloud.agent.AgentManager;
import com.cloud.api.query.dao.SecurityGroupJoinDaoImpl;
import com.cloud.cluster.agentlb.dao.HostTransferMapDaoImpl;
import com.cloud.dc.dao.ClusterDaoImpl;
import com.cloud.dc.dao.DataCenterDaoImpl;
import com.cloud.dc.dao.DataCenterDetailsDaoImpl;
import com.cloud.dc.dao.DataCenterIpAddressDaoImpl;
import com.cloud.dc.dao.DataCenterLinkLocalIpAddressDaoImpl;
import com.cloud.dc.dao.DataCenterVnetDaoImpl;
import com.cloud.dc.dao.HostPodDaoImpl;
import com.cloud.dc.dao.PodVlanDaoImpl;
import com.cloud.domain.dao.DomainDaoImpl;
import com.cloud.event.dao.UsageEventDaoImpl;
import com.cloud.host.dao.HostDaoImpl;
import com.cloud.host.dao.HostDetailsDaoImpl;
import com.cloud.host.dao.HostTagsDaoImpl;
import com.cloud.network.NetworkModel;
import com.cloud.network.security.SecurityGroupManagerTestConfiguration.Library;
import com.cloud.network.security.dao.SecurityGroupDaoImpl;
import com.cloud.network.security.dao.SecurityGroupRuleDaoImpl;
import com.cloud.network.security.dao.SecurityGroupRulesDaoImpl;
import com.cloud.network.security.dao.SecurityGroupVMMapDaoImpl;
import com.cloud.network.security.dao.SecurityGroupWorkDaoImpl;
import com.cloud.network.security.dao.VmRulesetLogDaoImpl;
import com.cloud.projects.ProjectManager;
import com.cloud.tags.dao.ResourceTagsDaoImpl;
import com.cloud.user.AccountManager;
import com.cloud.user.DomainManager;
import com.cloud.user.dao.AccountDaoImpl;
import com.cloud.vm.UserVmManager;
import com.cloud.vm.VirtualMachineManager;
import com.cloud.vm.dao.NicDaoImpl;
import com.cloud.vm.dao.UserVmDaoImpl;
import com.cloud.vm.dao.UserVmDetailsDaoImpl;
import com.cloud.vm.dao.VMInstanceDaoImpl;
import org.apache.cloudstack.engine.orchestration.service.NetworkOrchestrationService;
import org.apache.cloudstack.framework.config.dao.ConfigurationDaoImpl;
import org.apache.cloudstack.framework.config.dao.ConfigurationGroupDaoImpl;
import org.apache.cloudstack.framework.config.dao.ConfigurationSubGroupDaoImpl;
import org.apache.cloudstack.test.utils.SpringUtils;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.filter.TypeFilter;

import java.io.IOException;

@Configuration
@ComponentScan(basePackageClasses = {SecurityGroupRulesDaoImpl.class, UserVmDaoImpl.class, AccountDaoImpl.class, ConfigurationDaoImpl.class, ConfigurationGroupDaoImpl.class,
    ConfigurationSubGroupDaoImpl.class, SecurityGroupWorkDaoImpl.class, VmRulesetLogDaoImpl.class, VMInstanceDaoImpl.class, DomainDaoImpl.class, UsageEventDaoImpl.class,
    ResourceTagsDaoImpl.class, HostDaoImpl.class, HostDetailsDaoImpl.class, HostTagsDaoImpl.class, ClusterDaoImpl.class, HostPodDaoImpl.class,
    DataCenterDaoImpl.class, DataCenterIpAddressDaoImpl.class, HostTransferMapDaoImpl.class, SecurityGroupManagerImpl2.class, SecurityGroupDaoImpl.class,
    SecurityGroupVMMapDaoImpl.class, UserVmDetailsDaoImpl.class, DataCenterIpAddressDaoImpl.class, DataCenterLinkLocalIpAddressDaoImpl.class,
    DataCenterVnetDaoImpl.class, PodVlanDaoImpl.class, DataCenterDetailsDaoImpl.class, SecurityGroupRuleDaoImpl.class, NicDaoImpl.class,
    SecurityGroupJoinDaoImpl.class},
               includeFilters = {@Filter(value = Library.class, type = FilterType.CUSTOM)},
               useDefaultFilters = false)
public class SecurityGroupManagerTestConfiguration {

    @Bean
    public NetworkModel networkModel() {
        return Mockito.mock(NetworkModel.class);
    }

    @Bean
    public AgentManager agentManager() {
        return Mockito.mock(AgentManager.class);
    }

    @Bean
    public VirtualMachineManager virtualMachineManager() {
        return Mockito.mock(VirtualMachineManager.class);
    }

    @Bean
    public UserVmManager userVmManager() {
        return Mockito.mock(UserVmManager.class);
    }

    @Bean
    public NetworkOrchestrationService networkManager() {
        return Mockito.mock(NetworkOrchestrationService.class);
    }

    @Bean
    public AccountManager accountManager() {
        return Mockito.mock(AccountManager.class);
    }

    @Bean
    public DomainManager domainManager() {
        return Mockito.mock(DomainManager.class);
    }

    @Bean
    public ProjectManager projectManager() {
        return Mockito.mock(ProjectManager.class);
    }

    public static class Library implements TypeFilter {

        @Override
        public boolean match(MetadataReader mdr, MetadataReaderFactory arg1) throws IOException {
            mdr.getClassMetadata().getClassName();
            ComponentScan cs = SecurityGroupManagerTestConfiguration.class.getAnnotation(ComponentScan.class);
            return SpringUtils.includedInBasePackageClasses(mdr.getClassMetadata().getClassName(), cs);
        }

    }
}
