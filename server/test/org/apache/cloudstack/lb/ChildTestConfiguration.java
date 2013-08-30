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
package org.apache.cloudstack.lb;

import java.io.IOException;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.filter.TypeFilter;

import org.apache.cloudstack.lb.dao.ApplicationLoadBalancerRuleDao;
import org.apache.cloudstack.test.utils.SpringUtils;

import com.cloud.event.dao.UsageEventDao;
import com.cloud.network.IpAddressManager;
import com.cloud.network.NetworkManager;
import com.cloud.network.NetworkModel;
import com.cloud.network.dao.FirewallRulesDao;
import com.cloud.network.lb.LoadBalancingRulesManager;
import com.cloud.tags.dao.ResourceTagDao;
import com.cloud.user.AccountManager;
import com.cloud.utils.net.NetUtils;


@Configuration
@ComponentScan(
    basePackageClasses={
        NetUtils.class
    },
    includeFilters={@Filter(value=ChildTestConfiguration.Library.class, type=FilterType.CUSTOM)},
    useDefaultFilters=false
    )

    public class ChildTestConfiguration {
        
        public static class Library implements TypeFilter {
            
            @Bean
            public ApplicationLoadBalancerRuleDao applicationLoadBalancerDao() {
                return Mockito.mock(ApplicationLoadBalancerRuleDao.class);
            }
            
        @Bean
        IpAddressManager ipAddressManager() {
            return Mockito.mock(IpAddressManager.class);
        }

            @Bean
            public NetworkModel networkModel() {
                return Mockito.mock(NetworkModel.class);
            }
            
            @Bean
            public AccountManager accountManager() {
                return Mockito.mock(AccountManager.class);
            }
            
            @Bean
            public LoadBalancingRulesManager loadBalancingRulesManager() {
                return Mockito.mock(LoadBalancingRulesManager.class);
            }
            
            @Bean
            public FirewallRulesDao firewallRulesDao() {
                return Mockito.mock(FirewallRulesDao.class);
            }
            
            @Bean
            public ResourceTagDao resourceTagDao() {
                return Mockito.mock(ResourceTagDao.class);
            }
            
            @Bean
            public NetworkManager networkManager() {
                return Mockito.mock(NetworkManager.class);
            }
            
            @Bean
            public UsageEventDao UsageEventDao() {
                return Mockito.mock(UsageEventDao.class);
            }
            
            @Override
            public boolean match(MetadataReader mdr, MetadataReaderFactory arg1) throws IOException {
                mdr.getClassMetadata().getClassName();
                ComponentScan cs = ChildTestConfiguration.class.getAnnotation(ComponentScan.class);
                return SpringUtils.includedInBasePackageClasses(mdr.getClassMetadata().getClassName(), cs);
            }
    
        }
}
