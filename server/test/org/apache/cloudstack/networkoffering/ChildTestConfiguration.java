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

package org.apache.cloudstack.networkoffering;

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

import org.apache.cloudstack.acl.SecurityChecker;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreManager;
import org.apache.cloudstack.region.PortableIpDaoImpl;
import org.apache.cloudstack.region.PortableIpRangeDaoImpl;
import org.apache.cloudstack.region.dao.RegionDaoImpl;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDaoImpl;
import org.apache.cloudstack.test.utils.SpringUtils;

import com.cloud.agent.AgentManager;
import com.cloud.alert.AlertManager;
import com.cloud.api.query.dao.UserAccountJoinDaoImpl;
import com.cloud.capacity.dao.CapacityDaoImpl;
import com.cloud.cluster.agentlb.dao.HostTransferMapDaoImpl;
import com.cloud.configuration.dao.ConfigurationDao;
import com.cloud.dc.ClusterDetailsDao;
import com.cloud.dc.dao.AccountVlanMapDaoImpl;
import com.cloud.dc.dao.ClusterDaoImpl;
import com.cloud.dc.dao.DataCenterDaoImpl;
import com.cloud.dc.dao.DataCenterIpAddressDaoImpl;
import com.cloud.dc.dao.DataCenterLinkLocalIpAddressDao;
import com.cloud.dc.dao.DataCenterVnetDaoImpl;
import com.cloud.dc.dao.DcDetailsDaoImpl;
import com.cloud.dc.dao.DedicatedResourceDao;
import com.cloud.dc.dao.HostPodDaoImpl;
import com.cloud.dc.dao.PodVlanDaoImpl;
import com.cloud.dc.dao.PodVlanMapDaoImpl;
import com.cloud.dc.dao.VlanDaoImpl;
import com.cloud.domain.dao.DomainDaoImpl;
import com.cloud.event.dao.UsageEventDaoImpl;
import com.cloud.host.dao.HostDaoImpl;
import com.cloud.host.dao.HostDetailsDaoImpl;
import com.cloud.host.dao.HostTagsDaoImpl;
import com.cloud.network.Ipv6AddressManager;
import com.cloud.network.NetworkManager;
import com.cloud.network.NetworkModel;
import com.cloud.network.NetworkService;
import com.cloud.network.StorageNetworkManager;
import com.cloud.network.dao.AccountGuestVlanMapDaoImpl;
import com.cloud.network.dao.FirewallRulesCidrsDaoImpl;
import com.cloud.network.dao.FirewallRulesDaoImpl;
import com.cloud.network.dao.IPAddressDaoImpl;
import com.cloud.network.dao.LoadBalancerDaoImpl;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkDomainDaoImpl;
import com.cloud.network.dao.NetworkServiceMapDaoImpl;
import com.cloud.network.dao.PhysicalNetworkDaoImpl;
import com.cloud.network.dao.PhysicalNetworkServiceProviderDaoImpl;
import com.cloud.network.dao.PhysicalNetworkTrafficTypeDaoImpl;
import com.cloud.network.dao.UserIpv6AddressDaoImpl;
import com.cloud.network.element.DhcpServiceProvider;
import com.cloud.network.element.IpDeployer;
import com.cloud.network.element.NetworkElement;
import com.cloud.network.guru.NetworkGuru;
import com.cloud.network.lb.LoadBalancingRulesManager;
import com.cloud.network.rules.FirewallManager;
import com.cloud.network.rules.RulesManager;
import com.cloud.network.rules.dao.PortForwardingRulesDaoImpl;
import com.cloud.network.vpc.NetworkACLManager;
import com.cloud.network.vpc.VpcManager;
import com.cloud.network.vpc.dao.PrivateIpDaoImpl;
import com.cloud.network.vpn.RemoteAccessVpnService;
import com.cloud.offerings.dao.NetworkOfferingDao;
import com.cloud.offerings.dao.NetworkOfferingServiceMapDao;
import com.cloud.projects.ProjectManager;
import com.cloud.server.ConfigurationServer;
import com.cloud.server.ManagementService;
import com.cloud.service.dao.ServiceOfferingDaoImpl;
import com.cloud.service.dao.ServiceOfferingDetailsDaoImpl;
import com.cloud.storage.dao.DiskOfferingDaoImpl;
import com.cloud.storage.dao.S3DaoImpl;
import com.cloud.storage.dao.SnapshotDaoImpl;
import com.cloud.storage.dao.StoragePoolDetailsDaoImpl;
import com.cloud.storage.dao.VolumeDaoImpl;
import com.cloud.storage.secondary.SecondaryStorageVmManager;
import com.cloud.tags.dao.ResourceTagsDaoImpl;
import com.cloud.user.AccountDetailsDao;
import com.cloud.user.AccountManager;
import com.cloud.user.ResourceLimitService;
import com.cloud.user.dao.AccountDaoImpl;
import com.cloud.user.dao.UserDaoImpl;
import com.cloud.vm.dao.InstanceGroupDaoImpl;
import com.cloud.vm.dao.NicDaoImpl;
import com.cloud.vm.dao.NicSecondaryIpDaoImpl;
import com.cloud.vm.dao.UserVmDao;
import com.cloud.vm.dao.VMInstanceDaoImpl;

@Configuration
@ComponentScan(basePackageClasses={
        AccountVlanMapDaoImpl.class,
        VolumeDaoImpl.class,
        HostPodDaoImpl.class,
        DomainDaoImpl.class,
        ServiceOfferingDaoImpl.class,
        ServiceOfferingDetailsDaoImpl.class,
        VlanDaoImpl.class,
        IPAddressDaoImpl.class,
        ResourceTagsDaoImpl.class,
        AccountDaoImpl.class,
        InstanceGroupDaoImpl.class,
        UserAccountJoinDaoImpl.class,
        CapacityDaoImpl.class,
        SnapshotDaoImpl.class,
        HostDaoImpl.class,
        VMInstanceDaoImpl.class,
        HostTransferMapDaoImpl.class,
        PortForwardingRulesDaoImpl.class,
        PrivateIpDaoImpl.class,
        UsageEventDaoImpl.class,
        PodVlanMapDaoImpl.class,
        DiskOfferingDaoImpl.class,
        DataCenterDaoImpl.class,
        DataCenterIpAddressDaoImpl.class,
        DataCenterVnetDaoImpl.class,
        PodVlanDaoImpl.class,
        DcDetailsDaoImpl.class,
        NicSecondaryIpDaoImpl.class,
        UserIpv6AddressDaoImpl.class,
        S3DaoImpl.class,
        UserDaoImpl.class,
        NicDaoImpl.class,
        NetworkDomainDaoImpl.class,
        HostDetailsDaoImpl.class,
        HostTagsDaoImpl.class,
        ClusterDaoImpl.class,
        FirewallRulesDaoImpl.class,
        FirewallRulesCidrsDaoImpl.class,
        PhysicalNetworkDaoImpl.class,
        PhysicalNetworkTrafficTypeDaoImpl.class,
        PhysicalNetworkServiceProviderDaoImpl.class,
        LoadBalancerDaoImpl.class,
        NetworkServiceMapDaoImpl.class,
        PrimaryDataStoreDaoImpl.class,
        StoragePoolDetailsDaoImpl.class,
        PortableIpRangeDaoImpl.class,
        RegionDaoImpl.class,
        PortableIpDaoImpl.class,
        AccountGuestVlanMapDaoImpl.class
    },
includeFilters={@Filter(value=ChildTestConfiguration.Library.class, type=FilterType.CUSTOM)},
useDefaultFilters=false
        )

public class ChildTestConfiguration {

    @Bean
    public ManagementService managementService() {
        return Mockito.mock(ManagementService.class);
    }

    @Bean
    public AccountManager acctMgr() {
        return Mockito.mock(AccountManager.class);
    }

    @Bean
    public NetworkService ntwkSvc() {
        return Mockito.mock(NetworkService.class);
    }

    @Bean
    public NetworkModel ntwkMdl() {
        return Mockito.mock(NetworkModel.class);
    }

    @Bean
    public AlertManager alertMgr() {
        return Mockito.mock(AlertManager.class);
    }

    @Bean
    public SecurityChecker securityChkr() {
        return Mockito.mock(SecurityChecker.class);
    }

    @Bean
    public ResourceLimitService resourceSvc() {
        return Mockito.mock(ResourceLimitService.class);
    }

    @Bean
    public ProjectManager projectMgr() {
        return Mockito.mock(ProjectManager.class);
    }

    @Bean
    public SecondaryStorageVmManager ssvmMgr() {
        return Mockito.mock(SecondaryStorageVmManager.class);
    }

    @Bean
    public VpcManager vpcMgr() {
        return Mockito.mock(VpcManager.class);
    }

    @Bean
    public UserVmDao userVMDao() {
        return Mockito.mock(UserVmDao.class);
    }

    @Bean
    public RulesManager rulesMgr() {
        return Mockito.mock(RulesManager.class);
    }

    @Bean
    public LoadBalancingRulesManager lbRulesMgr() {
        return Mockito.mock(LoadBalancingRulesManager.class);
    }

    @Bean
    public RemoteAccessVpnService vpnMgr() {
        return Mockito.mock(RemoteAccessVpnService.class);
    }

    @Bean
    public NetworkGuru ntwkGuru() {
        return Mockito.mock(NetworkGuru.class);
    }

    @Bean
    public NetworkElement ntwkElement() {
        return Mockito.mock(NetworkElement.class);
    }

    @Bean
    public IpDeployer ipDeployer() {
        return Mockito.mock(IpDeployer.class);
    }

    @Bean
    public DhcpServiceProvider dhcpProvider() {
        return Mockito.mock(DhcpServiceProvider.class);
    }

    @Bean
    public FirewallManager firewallMgr() {
        return Mockito.mock(FirewallManager.class);
    }

    @Bean
    public AgentManager agentMgr() {
        return Mockito.mock(AgentManager.class);
    }

    @Bean
    public StorageNetworkManager storageNtwkMgr() {
        return Mockito.mock(StorageNetworkManager.class);
    }

    @Bean
    public NetworkACLManager ntwkAclMgr() {
        return Mockito.mock(NetworkACLManager.class);
    }

    @Bean
    public Ipv6AddressManager ipv6Mgr() {
        return Mockito.mock(Ipv6AddressManager.class);
    }

    @Bean
    public ConfigurationDao configDao() {
        return Mockito.mock(ConfigurationDao.class);
    }

    @Bean
    public CallContext userContext() {
        return Mockito.mock(CallContext.class);
    }

    @Bean
    public NetworkManager networkManager() {
        return Mockito.mock(NetworkManager.class);
    }

    @Bean
    public NetworkOfferingDao networkOfferingDao() {
        return Mockito.mock(NetworkOfferingDao.class);
    }

    @Bean
    public NetworkDao networkDao() {
        return Mockito.mock(NetworkDao.class);
    }

    @Bean
    public DedicatedResourceDao DedicatedResourceDao() {
        return Mockito.mock(DedicatedResourceDao.class);
    }

    @Bean
    public NetworkOfferingServiceMapDao networkOfferingServiceMapDao() {
        return Mockito.mock(NetworkOfferingServiceMapDao.class);
    }

    @Bean
    public DataCenterLinkLocalIpAddressDao datacenterLinkLocalIpAddressDao() {
    	return Mockito.mock(DataCenterLinkLocalIpAddressDao.class);
    }

    @Bean
    public ConfigurationServer configurationServer() {
        return Mockito.mock(ConfigurationServer.class);
    }

    @Bean
    public ClusterDetailsDao clusterDetailsDao() {
        return Mockito.mock(ClusterDetailsDao.class);
    }

    @Bean
    public AccountDetailsDao accountDetailsDao() {
        return Mockito.mock(AccountDetailsDao.class);
    }

    @Bean
    public DataStoreManager dataStoreManager() {
        return Mockito.mock(DataStoreManager.class);
    }

    public static class Library implements TypeFilter {

        @Override
        public boolean match(MetadataReader mdr, MetadataReaderFactory arg1) throws IOException {
            mdr.getClassMetadata().getClassName();
            ComponentScan cs = ChildTestConfiguration.class.getAnnotation(ComponentScan.class);
            return SpringUtils.includedInBasePackageClasses(mdr.getClassMetadata().getClassName(), cs);
        }

    }

}
