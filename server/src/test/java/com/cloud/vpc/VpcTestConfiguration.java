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

package com.cloud.vpc;

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
import org.apache.cloudstack.test.utils.SpringUtils;

import com.cloud.alert.AlertManager;
import com.cloud.cluster.agentlb.dao.HostTransferMapDaoImpl;
import com.cloud.configuration.dao.ResourceCountDaoImpl;
import com.cloud.configuration.dao.ResourceLimitDaoImpl;
import com.cloud.dao.EntityManagerImpl;
import com.cloud.dc.dao.AccountVlanMapDaoImpl;
import com.cloud.dc.dao.ClusterDaoImpl;
import com.cloud.dc.dao.DataCenterDaoImpl;
import com.cloud.dc.dao.DataCenterDetailsDaoImpl;
import com.cloud.dc.dao.DataCenterIpAddressDaoImpl;
import com.cloud.dc.dao.DataCenterLinkLocalIpAddressDaoImpl;
import com.cloud.dc.dao.DataCenterVnetDaoImpl;
import com.cloud.dc.dao.DomainVlanMapDaoImpl;
import com.cloud.dc.dao.HostPodDaoImpl;
import com.cloud.dc.dao.PodVlanDaoImpl;
import com.cloud.dc.dao.PodVlanMapDaoImpl;
import com.cloud.dc.dao.VlanDaoImpl;
import com.cloud.domain.dao.DomainDaoImpl;
import com.cloud.event.dao.UsageEventDao;
import com.cloud.host.dao.HostDaoImpl;
import com.cloud.host.dao.HostDetailsDaoImpl;
import com.cloud.host.dao.HostTagsDaoImpl;
import com.cloud.network.Ipv6AddressManagerImpl;
import com.cloud.network.StorageNetworkManager;
import com.cloud.network.dao.FirewallRulesCidrsDaoImpl;
import com.cloud.network.dao.FirewallRulesDaoImpl;
import com.cloud.network.dao.IPAddressDaoImpl;
import com.cloud.network.dao.LoadBalancerDaoImpl;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.PhysicalNetworkDaoImpl;
import com.cloud.network.dao.PhysicalNetworkTrafficTypeDaoImpl;
import com.cloud.network.dao.RouterNetworkDaoImpl;
import com.cloud.network.dao.Site2SiteVpnGatewayDaoImpl;
import com.cloud.network.dao.UserIpv6AddressDaoImpl;
import com.cloud.network.dao.VirtualRouterProviderDaoImpl;
import com.cloud.network.element.NetworkElement;
import com.cloud.network.element.Site2SiteVpnServiceProvider;
import com.cloud.network.lb.LoadBalancingRulesManager;
import com.cloud.network.rules.RulesManager;
import com.cloud.network.vpc.VpcManagerImpl;
import com.cloud.network.vpc.dao.PrivateIpDaoImpl;
import com.cloud.network.vpc.dao.StaticRouteDaoImpl;
import com.cloud.network.vpc.dao.VpcGatewayDaoImpl;
import com.cloud.network.vpc.dao.VpcOfferingDao;
import com.cloud.network.vpc.dao.VpcServiceMapDaoImpl;
import com.cloud.network.vpn.RemoteAccessVpnService;
import com.cloud.network.vpn.Site2SiteVpnManager;
import com.cloud.projects.dao.ProjectAccountDaoImpl;
import com.cloud.projects.dao.ProjectDaoImpl;
import com.cloud.resourcelimit.ResourceLimitManagerImpl;
import com.cloud.service.dao.ServiceOfferingDaoImpl;
import com.cloud.storage.dao.SnapshotDaoImpl;
import com.cloud.storage.dao.VMTemplateDaoImpl;
import com.cloud.storage.dao.VMTemplateDetailsDaoImpl;
import com.cloud.storage.dao.VMTemplateZoneDaoImpl;
import com.cloud.storage.dao.VolumeDaoImpl;
import com.cloud.tags.dao.ResourceTagsDaoImpl;
import com.cloud.user.AccountManager;
import com.cloud.user.dao.AccountDaoImpl;
import com.cloud.user.dao.UserStatisticsDaoImpl;
import com.cloud.vm.UserVmManager;
import com.cloud.vm.dao.DomainRouterDaoImpl;
import com.cloud.vm.dao.NicDaoImpl;
import com.cloud.vm.dao.NicSecondaryIpDaoImpl;
import com.cloud.vm.dao.UserVmDaoImpl;
import com.cloud.vm.dao.UserVmDetailsDaoImpl;
import com.cloud.vm.dao.VMInstanceDaoImpl;
import com.cloud.vpc.dao.MockNetworkOfferingDaoImpl;
import com.cloud.vpc.dao.MockNetworkOfferingServiceMapDaoImpl;
import com.cloud.vpc.dao.MockNetworkServiceMapDaoImpl;
import com.cloud.vpc.dao.MockVpcDaoImpl;
import com.cloud.vpc.dao.MockVpcOfferingDaoImpl;
import com.cloud.vpc.dao.MockVpcOfferingServiceMapDaoImpl;

@Configuration
@ComponentScan(basePackageClasses = {VpcManagerImpl.class, NetworkElement.class, VpcOfferingDao.class, ConfigurationDaoImpl.class, IPAddressDaoImpl.class,
    DomainRouterDaoImpl.class, VpcGatewayDaoImpl.class, PrivateIpDaoImpl.class, StaticRouteDaoImpl.class, PhysicalNetworkDaoImpl.class,
    ResourceTagsDaoImpl.class, FirewallRulesDaoImpl.class, VlanDaoImpl.class, AccountDaoImpl.class, ResourceCountDaoImpl.class,
    Site2SiteVpnGatewayDaoImpl.class, PodVlanMapDaoImpl.class, AccountVlanMapDaoImpl.class, DomainVlanMapDaoImpl.class, HostDaoImpl.class, HostDetailsDaoImpl.class,
    HostTagsDaoImpl.class, HostTransferMapDaoImpl.class, ClusterDaoImpl.class, HostPodDaoImpl.class, RouterNetworkDaoImpl.class,
    UserStatisticsDaoImpl.class, PhysicalNetworkTrafficTypeDaoImpl.class, FirewallRulesCidrsDaoImpl.class, ResourceLimitManagerImpl.class,
    ResourceLimitDaoImpl.class, ResourceCountDaoImpl.class, DomainDaoImpl.class, UserVmDaoImpl.class, UserVmDetailsDaoImpl.class, NicDaoImpl.class,
    SnapshotDaoImpl.class, VMInstanceDaoImpl.class, VolumeDaoImpl.class, UserIpv6AddressDaoImpl.class, NicSecondaryIpDaoImpl.class,
    VpcServiceMapDaoImpl.class, ServiceOfferingDaoImpl.class, MockVpcDaoImpl.class, VMTemplateDaoImpl.class,
    VMTemplateZoneDaoImpl.class, VMTemplateDetailsDaoImpl.class, DataCenterDaoImpl.class, DataCenterIpAddressDaoImpl.class,
    DataCenterLinkLocalIpAddressDaoImpl.class, DataCenterVnetDaoImpl.class, PodVlanDaoImpl.class, DataCenterDetailsDaoImpl.class,
    MockNetworkManagerImpl.class, MockVpcVirtualNetworkApplianceManager.class, EntityManagerImpl.class, LoadBalancerDaoImpl.class,
    FirewallRulesCidrsDaoImpl.class, VirtualRouterProviderDaoImpl.class, ProjectDaoImpl.class, ProjectAccountDaoImpl.class, MockVpcOfferingDaoImpl.class,
    MockConfigurationManagerImpl.class, MockNetworkOfferingServiceMapDaoImpl.class, MockNetworkServiceMapDaoImpl.class,
    MockVpcOfferingServiceMapDaoImpl.class, MockNetworkOfferingDaoImpl.class, MockNetworkModelImpl.class, Ipv6AddressManagerImpl.class},
               includeFilters = {@Filter(value = VpcTestConfiguration.VpcLibrary.class, type = FilterType.CUSTOM)},
               useDefaultFilters = false)
public class VpcTestConfiguration {

    @Bean
    public RulesManager rulesManager() {
        return Mockito.mock(RulesManager.class);
    }

    @Bean
    public StorageNetworkManager storageNetworkManager() {
        return Mockito.mock(StorageNetworkManager.class);
    }

    @Bean
    public LoadBalancingRulesManager loadBalancingRulesManager() {
        return Mockito.mock(LoadBalancingRulesManager.class);
    }

    @Bean
    public AlertManager alertManager() {
        return Mockito.mock(AlertManager.class);
    }

    @Bean
    public UserVmManager userVmManager() {
        return Mockito.mock(UserVmManager.class);
    }

    @Bean
    public AccountManager accountManager() {
        return Mockito.mock(AccountManager.class);
    }

    @Bean
    public Site2SiteVpnServiceProvider site2SiteVpnServiceProvider() {
        return Mockito.mock(Site2SiteVpnServiceProvider.class);
    }

    @Bean
    public Site2SiteVpnManager site2SiteVpnManager() {
        return Mockito.mock(Site2SiteVpnManager.class);
    }

    @Bean
    public UsageEventDao usageEventDao() {
        return Mockito.mock(UsageEventDao.class);
    }

    @Bean
    public RemoteAccessVpnService remoteAccessVpnService() {
        return Mockito.mock(RemoteAccessVpnService.class);
    }

//    @Bean
//    public VpcDao vpcDao() {
//        return Mockito.mock(VpcDao.class);
//    }

    @Bean
    public NetworkDao networkDao() {
        return Mockito.mock(NetworkDao.class);
    }

    public static class VpcLibrary implements TypeFilter {
        @Override
        public boolean match(MetadataReader mdr, MetadataReaderFactory arg1) throws IOException {
            mdr.getClassMetadata().getClassName();
            ComponentScan cs = VpcTestConfiguration.class.getAnnotation(ComponentScan.class);
            return SpringUtils.includedInBasePackageClasses(mdr.getClassMetadata().getClassName(), cs);
        }
    }
}
