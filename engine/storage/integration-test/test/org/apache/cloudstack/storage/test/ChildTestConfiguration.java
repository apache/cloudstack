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
package org.apache.cloudstack.storage.test;

import java.io.IOException;

import org.apache.cloudstack.acl.APIChecker;
import org.apache.cloudstack.engine.service.api.OrchestrationService;
import org.apache.cloudstack.engine.subsystem.api.storage.EndPointSelector;
import org.apache.cloudstack.framework.rpc.RpcProvider;
import org.apache.cloudstack.storage.cache.manager.StorageCacheManagerImpl;
import org.apache.cloudstack.storage.test.ChildTestConfiguration.Library;
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

import com.cloud.agent.AgentManager;
import com.cloud.alert.AlertManager;
import com.cloud.capacity.dao.CapacityDaoImpl;
import com.cloud.cluster.ClusteredAgentRebalanceService;
import com.cloud.cluster.agentlb.dao.HostTransferMapDaoImpl;
import com.cloud.configuration.dao.ConfigurationDaoImpl;
import com.cloud.dc.ClusterDetailsDaoImpl;
import com.cloud.dc.dao.ClusterDaoImpl;
import com.cloud.dc.dao.DataCenterDaoImpl;
import com.cloud.dc.dao.DataCenterIpAddressDaoImpl;
import com.cloud.dc.dao.DataCenterLinkLocalIpAddressDaoImpl;
import com.cloud.dc.dao.DataCenterVnetDaoImpl;
import com.cloud.dc.dao.DcDetailsDaoImpl;
import com.cloud.dc.dao.HostPodDaoImpl;
import com.cloud.dc.dao.PodVlanDaoImpl;
import com.cloud.domain.dao.DomainDaoImpl;
import com.cloud.host.dao.HostDao;
import com.cloud.host.dao.HostDaoImpl;
import com.cloud.host.dao.HostDetailsDaoImpl;
import com.cloud.host.dao.HostTagsDaoImpl;
import com.cloud.hypervisor.HypervisorGuruManager;
import com.cloud.resource.ResourceManager;
import com.cloud.server.ManagementServer;
import com.cloud.server.auth.UserAuthenticator;
import com.cloud.service.dao.ServiceOfferingDaoImpl;
import com.cloud.storage.OCFS2ManagerImpl;
import com.cloud.storage.StorageManager;
import com.cloud.storage.VolumeManager;
import com.cloud.storage.dao.DiskOfferingDaoImpl;
import com.cloud.storage.dao.SnapshotDaoImpl;
import com.cloud.storage.dao.StoragePoolDetailsDaoImpl;
import com.cloud.storage.dao.StoragePoolHostDaoImpl;
import com.cloud.storage.dao.StoragePoolWorkDaoImpl;
import com.cloud.storage.dao.VMTemplateDaoImpl;
import com.cloud.storage.dao.VMTemplateDetailsDaoImpl;
import com.cloud.storage.dao.VMTemplateHostDaoImpl;
import com.cloud.storage.dao.VMTemplatePoolDaoImpl;
import com.cloud.storage.dao.VMTemplateZoneDaoImpl;
import com.cloud.storage.dao.VolumeDaoImpl;
import com.cloud.storage.dao.VolumeHostDaoImpl;
import com.cloud.storage.download.DownloadMonitorImpl;
import com.cloud.storage.s3.S3Manager;
import com.cloud.storage.secondary.SecondaryStorageVmManager;
import com.cloud.storage.snapshot.SnapshotManager;
import com.cloud.tags.dao.ResourceTagsDaoImpl;
import com.cloud.template.TemplateManager;
import com.cloud.user.AccountManager;
import com.cloud.user.ResourceLimitService;
import com.cloud.user.dao.AccountDaoImpl;
import com.cloud.user.dao.UserDaoImpl;
import com.cloud.vm.VirtualMachineManager;
import com.cloud.vm.dao.ConsoleProxyDaoImpl;
import com.cloud.vm.dao.DomainRouterDao;
import com.cloud.vm.dao.NicDaoImpl;
import com.cloud.vm.dao.SecondaryStorageVmDaoImpl;
import com.cloud.vm.dao.UserVmDaoImpl;
import com.cloud.vm.dao.UserVmDetailsDaoImpl;
import com.cloud.vm.dao.VMInstanceDaoImpl;
import com.cloud.vm.snapshot.dao.VMSnapshotDaoImpl;

@Configuration
@ComponentScan(basePackageClasses = { NicDaoImpl.class, VMInstanceDaoImpl.class, VMTemplateHostDaoImpl.class,
        VolumeHostDaoImpl.class, VolumeDaoImpl.class, VMTemplatePoolDaoImpl.class, ResourceTagsDaoImpl.class,
        VMTemplateDaoImpl.class, MockStorageMotionStrategy.class, ConfigurationDaoImpl.class, ClusterDaoImpl.class,
        HostPodDaoImpl.class, VMTemplateZoneDaoImpl.class, VMTemplateDetailsDaoImpl.class, HostDetailsDaoImpl.class,
        HostTagsDaoImpl.class, HostTransferMapDaoImpl.class, DataCenterIpAddressDaoImpl.class,
        DataCenterLinkLocalIpAddressDaoImpl.class, DataCenterVnetDaoImpl.class, PodVlanDaoImpl.class,
        DcDetailsDaoImpl.class, DiskOfferingDaoImpl.class, StoragePoolHostDaoImpl.class, UserVmDaoImpl.class,
        UserVmDetailsDaoImpl.class, ServiceOfferingDaoImpl.class, CapacityDaoImpl.class, SnapshotDaoImpl.class,
        VMSnapshotDaoImpl.class, OCFS2ManagerImpl.class, ClusterDetailsDaoImpl.class, SecondaryStorageVmDaoImpl.class,
        ConsoleProxyDaoImpl.class, StoragePoolWorkDaoImpl.class, StorageCacheManagerImpl.class, UserDaoImpl.class,
        DataCenterDaoImpl.class, StoragePoolDetailsDaoImpl.class, DomainDaoImpl.class, DownloadMonitorImpl.class,
        AccountDaoImpl.class }, includeFilters = { @Filter(value = Library.class, type = FilterType.CUSTOM) },
        useDefaultFilters = false)
public class ChildTestConfiguration extends TestConfiguration {

    @Bean
    public SecondaryStorageVmManager secondaryStoreageMgr() {
        return Mockito.mock(SecondaryStorageVmManager.class);
    }

    @Bean
    public HostDao hostDao() {
        return Mockito.spy(new HostDaoImpl());
    }

    @Bean
    public EndPointSelector selector() {
        return Mockito.mock(EndPointSelector.class);
    }

    @Bean
    public AgentManager agentMgr() {
        return new DirectAgentManagerSimpleImpl();
    }

    @Bean
    public ResourceLimitService limtServe() {
        return Mockito.mock(ResourceLimitService.class);
    }

    @Bean
    public AccountManager acctMgt() {
        return Mockito.mock(AccountManager.class);
    }

    @Bean
    public RpcProvider rpcProvider() {
        return Mockito.mock(RpcProvider.class);
    }

    @Bean
    public ClusteredAgentRebalanceService _rebalanceService() {
        return Mockito.mock(ClusteredAgentRebalanceService.class);
    }

    @Bean
    public UserAuthenticator authenticator() {
        return Mockito.mock(UserAuthenticator.class);
    }

    @Bean
    public OrchestrationService orchSrvc() {
        return Mockito.mock(OrchestrationService.class);
    }

    @Bean
    public APIChecker apiChecker() {
        return Mockito.mock(APIChecker.class);
    }

    @Bean
    public TemplateManager templateMgr() {
        return Mockito.mock(TemplateManager.class);
    }

    @Bean
    public VolumeManager volumeMgr() {
        return Mockito.mock(VolumeManager.class);
    }

    @Bean
    public ManagementServer server() {
        return Mockito.mock(ManagementServer.class);
    }

    @Bean
    public VirtualMachineManager vmMgr() {
        return Mockito.mock(VirtualMachineManager.class);
    }

    @Bean
    public S3Manager s3Mgr() {
        return Mockito.mock(S3Manager.class);
    }

    @Bean
    public SnapshotManager snapshotMgr() {
        return Mockito.mock(SnapshotManager.class);
    }

    @Bean
    public ResourceManager resourceMgr() {
        return Mockito.mock(ResourceManager.class);
    }

    @Bean
    public DomainRouterDao domainRouterDao() {
        return Mockito.mock(DomainRouterDao.class);
    }

    @Bean
    public StorageManager storageMgr() {
        return Mockito.mock(StorageManager.class);
    }

    @Bean
    public AlertManager alertMgr() {
        return Mockito.mock(AlertManager.class);
    }

    @Bean
    public HypervisorGuruManager hypervisorGuruMgr() {
        return Mockito.mock(HypervisorGuruManager.class);
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
