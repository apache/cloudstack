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

package com.cloud.usage;

import com.cloud.alert.AlertManager;
import com.cloud.event.dao.UsageEventDao;
import com.cloud.usage.UsageManagerTestConfiguration.Library;
import com.cloud.usage.dao.*;
import com.cloud.usage.parser.*;
import com.cloud.user.dao.AccountDaoImpl;
import com.cloud.user.dao.UserStatisticsDaoImpl;

import org.apache.cloudstack.framework.config.dao.ConfigurationDaoImpl;
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
@ComponentScan(basePackageClasses={
        AccountDaoImpl.class,
        UsageDaoImpl.class,
        UsageJobDaoImpl.class,
        UsageVMInstanceDaoImpl.class,
        UsageIPAddressDaoImpl.class,
        UsageNetworkDaoImpl.class,
        UsageVolumeDaoImpl.class,
        UsageStorageDaoImpl.class,
        UsageLoadBalancerPolicyDaoImpl.class,
        UsagePortForwardingRuleDaoImpl.class,
        UsageNetworkOfferingDaoImpl.class,
        UsageVPNUserDaoImpl.class,
        UsageVmDiskDaoImpl.class,
        UsageSecurityGroupDaoImpl.class,
        ConfigurationDaoImpl.class,
        UsageManagerImpl.class,
        VMInstanceUsageParser.class,
        IPAddressUsageParser.class,
        LoadBalancerUsageParser.class,
        NetworkOfferingUsageParser.class,
        NetworkUsageParser.class,
        PortForwardingUsageParser.class,
        SecurityGroupUsageParser.class,
        StorageUsageParser.class,
        VmDiskUsageParser.class,
        VolumeUsageParser.class,
        VPNUserUsageParser.class,
        UserStatisticsDaoImpl.class},
        includeFilters={@Filter(value=Library.class, type=FilterType.CUSTOM)},
        useDefaultFilters=false
        )
public class UsageManagerTestConfiguration {

    @Bean
    public AlertManager alertManager() {
        return Mockito.mock(AlertManager.class);
    }

    @Bean
    public UsageEventDao usageEventDao() {
        return Mockito.mock(UsageEventDao.class);
    }

    public static class Library implements TypeFilter {

        @Override
        public boolean match(MetadataReader mdr, MetadataReaderFactory arg1) throws IOException {
            mdr.getClassMetadata().getClassName();
            ComponentScan cs = UsageManagerTestConfiguration.class.getAnnotation(ComponentScan.class);
            return SpringUtils.includedInBasePackageClasses(mdr.getClassMetadata().getClassName(), cs);
        }

    }
}
