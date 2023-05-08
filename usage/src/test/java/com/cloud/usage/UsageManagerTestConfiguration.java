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

import org.apache.cloudstack.framework.config.dao.ConfigurationDaoImpl;
import org.apache.cloudstack.framework.config.dao.ConfigurationGroupDaoImpl;
import org.apache.cloudstack.framework.config.dao.ConfigurationSubGroupDaoImpl;
import org.apache.cloudstack.test.utils.SpringUtils;

import com.cloud.alert.AlertManager;
import com.cloud.event.dao.UsageEventDao;
import com.cloud.usage.UsageManagerTestConfiguration.Library;
import com.cloud.usage.dao.UsageDaoImpl;
import com.cloud.usage.dao.UsageIPAddressDaoImpl;
import com.cloud.usage.dao.UsageJobDaoImpl;
import com.cloud.usage.dao.UsageLoadBalancerPolicyDaoImpl;
import com.cloud.usage.dao.UsageNetworkDaoImpl;
import com.cloud.usage.dao.UsageNetworkOfferingDaoImpl;
import com.cloud.usage.dao.UsagePortForwardingRuleDaoImpl;
import com.cloud.usage.dao.UsageSecurityGroupDaoImpl;
import com.cloud.usage.dao.UsageStorageDaoImpl;
import com.cloud.usage.dao.UsageVMInstanceDaoImpl;
import com.cloud.usage.dao.UsageVPNUserDaoImpl;
import com.cloud.usage.dao.UsageVmDiskDaoImpl;
import com.cloud.usage.dao.UsageVolumeDaoImpl;
import com.cloud.usage.parser.IPAddressUsageParser;
import com.cloud.usage.parser.LoadBalancerUsageParser;
import com.cloud.usage.parser.NetworkOfferingUsageParser;
import com.cloud.usage.parser.NetworkUsageParser;
import com.cloud.usage.parser.PortForwardingUsageParser;
import com.cloud.usage.parser.SecurityGroupUsageParser;
import com.cloud.usage.parser.StorageUsageParser;
import com.cloud.usage.parser.VMInstanceUsageParser;
import com.cloud.usage.parser.VPNUserUsageParser;
import com.cloud.usage.parser.VmDiskUsageParser;
import com.cloud.usage.parser.VolumeUsageParser;
import com.cloud.user.dao.AccountDaoImpl;
import com.cloud.user.dao.UserStatisticsDaoImpl;

@Configuration
@ComponentScan(basePackageClasses = {AccountDaoImpl.class, UsageDaoImpl.class, UsageJobDaoImpl.class, UsageVMInstanceDaoImpl.class, UsageIPAddressDaoImpl.class,
    UsageNetworkDaoImpl.class, UsageVolumeDaoImpl.class, UsageStorageDaoImpl.class, UsageLoadBalancerPolicyDaoImpl.class,
    UsagePortForwardingRuleDaoImpl.class, UsageNetworkOfferingDaoImpl.class, UsageVPNUserDaoImpl.class, UsageVmDiskDaoImpl.class,
    UsageSecurityGroupDaoImpl.class, ConfigurationDaoImpl.class, ConfigurationGroupDaoImpl.class, ConfigurationSubGroupDaoImpl.class, UsageManagerImpl.class, VMInstanceUsageParser.class, IPAddressUsageParser.class,
    LoadBalancerUsageParser.class, NetworkOfferingUsageParser.class, NetworkUsageParser.class, PortForwardingUsageParser.class,
    SecurityGroupUsageParser.class, StorageUsageParser.class, VmDiskUsageParser.class, VolumeUsageParser.class, VPNUserUsageParser.class,
    UserStatisticsDaoImpl.class},
               includeFilters = {@Filter(value = Library.class, type = FilterType.CUSTOM)},
               useDefaultFilters = false)
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
