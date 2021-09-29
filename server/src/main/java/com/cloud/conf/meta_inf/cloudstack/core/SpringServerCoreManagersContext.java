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
package com.cloud.conf.meta_inf.cloudstack.core;

import com.cloud.alert.AlertManagerImpl;
import com.cloud.api.ApiAsyncJobDispatcher;
import com.cloud.api.query.QueryManagerImpl;
import com.cloud.api.query.dao.AffinityGroupJoinDaoImpl;
import com.cloud.capacity.CapacityManagerImpl;
import com.cloud.deploy.dao.PlannerHostReservationDaoImpl;
import com.cloud.metadata.ResourceMetaDataManagerImpl;
import com.cloud.network.ExternalDeviceUsageManagerImpl;
import com.cloud.network.ExternalNetworkDeviceManagerImpl;
import com.cloud.network.IpAddressManagerImpl;
import com.cloud.network.Ipv6AddressManagerImpl;
import com.cloud.network.NetworkMigrationManagerImpl;
import com.cloud.network.NetworkUsageManagerImpl;
import com.cloud.network.StorageNetworkManagerImpl;
import com.cloud.network.as.AutoScaleManagerImpl;
import com.cloud.network.lb.LBHealthCheckManagerImpl;
import com.cloud.network.router.CommandSetupHelper;
import com.cloud.network.router.NetworkHelperImpl;
import com.cloud.network.router.NicProfileHelperImpl;
import com.cloud.network.router.RouterControlHelper;
import com.cloud.network.router.VirtualNetworkApplianceManagerImpl;
import com.cloud.network.router.VpcNetworkHelperImpl;
import com.cloud.network.router.VpcVirtualNetworkApplianceManagerImpl;
import com.cloud.network.rules.RulesManagerImpl;
import com.cloud.network.rules.VirtualNetworkApplianceFactory;
import com.cloud.network.security.SecurityGroupManagerImpl2;
import com.cloud.network.vpc.NetworkACLServiceImpl;
import com.cloud.network.vpc.VpcPrivateGatewayTransactionCallable;
import com.cloud.projects.ProjectManagerImpl;
import com.cloud.resourceicon.ResourceIconManagerImpl;
import com.cloud.resourcelimit.ResourceLimitManagerImpl;
import com.cloud.server.ConfigurationServerImpl;
import com.cloud.server.StatsCollector;
import com.cloud.storage.ImageStoreServiceImpl;
import com.cloud.storage.ImageStoreUploadMonitorImpl;
import com.cloud.storage.JavaStorageLayer;
import com.cloud.storage.OCFS2ManagerImpl;
import com.cloud.storage.StorageManagerImpl;
import com.cloud.storage.StoragePoolAutomationImpl;
import com.cloud.storage.download.DownloadMonitorImpl;
import com.cloud.storage.snapshot.SnapshotManagerImpl;
import com.cloud.storage.snapshot.SnapshotSchedulerImpl;
import com.cloud.storage.upload.UploadMonitorImpl;
import com.cloud.tags.ResourceManagerUtilImpl;
import com.cloud.tags.TaggedResourceManagerImpl;
import com.cloud.usage.UsageServiceImpl;
import com.cloud.user.DomainManagerImpl;
import com.cloud.uuididentity.UUIDManagerImpl;
import com.cloud.vm.UserVmManagerImpl;
import com.cloud.vm.snapshot.VMSnapshotManagerImpl;
import org.apache.cloudstack.acl.ProjectRoleManagerImpl;
import org.apache.cloudstack.acl.RoleManagerImpl;
import org.apache.cloudstack.agent.lb.IndirectAgentLBServiceImpl;
import org.apache.cloudstack.diagnostics.DiagnosticsServiceImpl;
import org.apache.cloudstack.direct.download.DirectDownloadManagerImpl;
import org.apache.cloudstack.network.lb.ApplicationLoadBalancerManagerImpl;
import org.apache.cloudstack.network.ssl.CertServiceImpl;
import org.apache.cloudstack.network.topology.AdvancedNetworkTopology;
import org.apache.cloudstack.network.topology.AdvancedNetworkVisitor;
import org.apache.cloudstack.network.topology.BasicNetworkTopology;
import org.apache.cloudstack.network.topology.BasicNetworkVisitor;
import org.apache.cloudstack.network.topology.NetworkTopologyContext;
import org.apache.cloudstack.poll.BackgroundPollManagerImpl;
import org.apache.cloudstack.region.RegionManagerImpl;
import org.apache.cloudstack.region.RegionServiceImpl;
import org.cloud.network.router.deployment.RouterDeploymentDefinitionBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


/**
 * Generated Java based configuration
 */
@Configuration
public class SpringServerCoreManagersContext {


    @Bean("capacityManagerImpl")
    public CapacityManagerImpl capacityManagerImpl() {
        return new CapacityManagerImpl();
    }

    @Bean("ApiAsyncJobDispatcher")
    public ApiAsyncJobDispatcher ApiAsyncJobDispatcher() {
        ApiAsyncJobDispatcher bean = new ApiAsyncJobDispatcher();
        bean.setName("ApiAsyncJobDispatcher");
        return bean;
    }

    @Bean("lBHealthCheckManagerImpl")
    public LBHealthCheckManagerImpl lBHealthCheckManagerImpl() {
        return new LBHealthCheckManagerImpl();
    }

    @Bean("vpcVirtualNetworkApplianceManagerImpl")
    public VpcVirtualNetworkApplianceManagerImpl vpcVirtualNetworkApplianceManagerImpl() {
        return new VpcVirtualNetworkApplianceManagerImpl();
    }

    @Bean("projRoleManagerImpl")
    public ProjectRoleManagerImpl projRoleManagerImpl() {
        return new ProjectRoleManagerImpl();
    }

    @Bean("NetworkMigrationManagerImpl")
    public NetworkMigrationManagerImpl NetworkMigrationManagerImpl() {
        return new NetworkMigrationManagerImpl();
    }

    @Bean("oCFS2ManagerImpl")
    public OCFS2ManagerImpl oCFS2ManagerImpl() {
        return new OCFS2ManagerImpl();
    }

    @Bean("vpcTxCallable")
    public VpcPrivateGatewayTransactionCallable vpcTxCallable() {
        return new VpcPrivateGatewayTransactionCallable();
    }

    @Bean("imageStoreUploadMonitorImpl")
    public ImageStoreUploadMonitorImpl imageStoreUploadMonitorImpl() {
        return new ImageStoreUploadMonitorImpl();
    }

    @Bean("configurationServerImpl")
    public ConfigurationServerImpl configurationServerImpl() {
        return new ConfigurationServerImpl();
    }

    @Bean("networkACLServiceImpl")
    public NetworkACLServiceImpl networkACLServiceImpl() {
        return new NetworkACLServiceImpl();
    }

    @Bean("storagePoolAutomationImpl")
    public StoragePoolAutomationImpl storagePoolAutomationImpl() {
        return new StoragePoolAutomationImpl();
    }

    @Bean("usageServiceImpl")
    public UsageServiceImpl usageServiceImpl() {
        return new UsageServiceImpl();
    }

    @Bean("imageStoreServiceImpl")
    public ImageStoreServiceImpl imageStoreServiceImpl() {
        return new ImageStoreServiceImpl();
    }

    @Bean("AffinityGroupJoinDaoImpl")
    public AffinityGroupJoinDaoImpl AffinityGroupJoinDaoImpl() {
        return new AffinityGroupJoinDaoImpl();
    }

    @Bean("autoScaleManagerImpl")
    public AutoScaleManagerImpl autoScaleManagerImpl() {
        return new AutoScaleManagerImpl();
    }

    @Bean("ipv6AddressManagerImpl")
    public Ipv6AddressManagerImpl ipv6AddressManagerImpl() {
        return new Ipv6AddressManagerImpl();
    }

    @Bean("PlannerHostReservationDaoImpl")
    public PlannerHostReservationDaoImpl PlannerHostReservationDaoImpl() {
        return new PlannerHostReservationDaoImpl();
    }

    @Bean("resourceMetaDataManagerImpl")
    public ResourceMetaDataManagerImpl resourceMetaDataManagerImpl() {
        return new ResourceMetaDataManagerImpl();
    }

    @Bean("indirectAgentLBService")
    public IndirectAgentLBServiceImpl indirectAgentLBService() {
        return new IndirectAgentLBServiceImpl();
    }

    @Bean("vpcNetworkHelper")
    public VpcNetworkHelperImpl vpcNetworkHelper() {
        return new VpcNetworkHelperImpl();
    }

    @Bean("certServiceImpl")
    public CertServiceImpl certServiceImpl() {
        return new CertServiceImpl();
    }

    @Bean("advancedNetworkVisitor")
    public AdvancedNetworkVisitor advancedNetworkVisitor() {
        return new AdvancedNetworkVisitor();
    }

    @Bean("statsCollector")
    public StatsCollector statsCollector() {
        return new StatsCollector();
    }

    @Bean("routerControlHelper")
    public RouterControlHelper routerControlHelper() {
        return new RouterControlHelper();
    }

    @Bean("projectManagerImpl")
    public ProjectManagerImpl projectManagerImpl() {
        return new ProjectManagerImpl();
    }

    @Bean("networkUsageManagerImpl")
    public NetworkUsageManagerImpl networkUsageManagerImpl() {
        return new NetworkUsageManagerImpl();
    }

    @Bean("resourceManagerUtilImpl")
    public ResourceManagerUtilImpl resourceManagerUtilImpl() {
        return new ResourceManagerUtilImpl();
    }

    @Bean("commandSetupHelper")
    public CommandSetupHelper commandSetupHelper() {
        return new CommandSetupHelper();
    }

    @Bean("externalNetworkDeviceManagerImpl")
    public ExternalNetworkDeviceManagerImpl externalNetworkDeviceManagerImpl() {
        return new ExternalNetworkDeviceManagerImpl();
    }

    @Bean("networkHelper")
    public NetworkHelperImpl networkHelper() {
        return new NetworkHelperImpl();
    }

    @Bean("DiagnosticsService")
    public DiagnosticsServiceImpl DiagnosticsService() {
        return new DiagnosticsServiceImpl();
    }

    @Bean("regionServiceImpl")
    public RegionServiceImpl regionServiceImpl() {
        return new RegionServiceImpl();
    }

    @Bean("storageManagerImpl")
    public StorageManagerImpl storageManagerImpl() {
        return new StorageManagerImpl();
    }

    @Bean("externalDeviceUsageManagerImpl")
    public ExternalDeviceUsageManagerImpl externalDeviceUsageManagerImpl() {
        return new ExternalDeviceUsageManagerImpl();
    }

    @Bean("uploadMonitorImpl")
    public UploadMonitorImpl uploadMonitorImpl() {
        return new UploadMonitorImpl();
    }

    @Bean("uUIDManagerImpl")
    public UUIDManagerImpl uUIDManagerImpl() {
        return new UUIDManagerImpl();
    }

    @Bean("resourceLimitManagerImpl")
    public ResourceLimitManagerImpl resourceLimitManagerImpl() {
        return new ResourceLimitManagerImpl();
    }

    @Bean("snapshotManagerImpl")
    public SnapshotManagerImpl snapshotManagerImpl() {
        return new SnapshotManagerImpl();
    }

    @Bean("userVmManagerImpl")
    public UserVmManagerImpl userVmManagerImpl() {
        return new UserVmManagerImpl();
    }

    @Bean("advancedNetworkTopology")
    public AdvancedNetworkTopology advancedNetworkTopology() {
        return new AdvancedNetworkTopology();
    }

    @Bean("virtualNetworkApplianceFactory")
    public VirtualNetworkApplianceFactory virtualNetworkApplianceFactory() {
        return new VirtualNetworkApplianceFactory();
    }

    @Bean("downloadMonitorImpl")
    public DownloadMonitorImpl downloadMonitorImpl() {
        return new DownloadMonitorImpl();
    }

    @Bean("directDownloadManager")
    public DirectDownloadManagerImpl directDownloadManager() {
        return new DirectDownloadManagerImpl();
    }

    @Bean("nicProfileHelper")
    public NicProfileHelperImpl nicProfileHelper() {
        return new NicProfileHelperImpl();
    }

    @Bean("regionManagerImpl")
    public RegionManagerImpl regionManagerImpl() {
        return new RegionManagerImpl();
    }

    @Bean("snapshotSchedulerImpl")
    public SnapshotSchedulerImpl snapshotSchedulerImpl(
            @Qualifier("ApiAsyncJobDispatcher")
                    ApiAsyncJobDispatcher ApiAsyncJobDispatcher) {
        SnapshotSchedulerImpl bean = new SnapshotSchedulerImpl();
        bean.setAsyncJobDispatcher(ApiAsyncJobDispatcher);
        return bean;
    }

    @Bean("securityGroupManagerImpl2")
    public SecurityGroupManagerImpl2 securityGroupManagerImpl2() {
        return new SecurityGroupManagerImpl2();
    }

    @Bean("taggedResourceManagerImpl")
    public TaggedResourceManagerImpl taggedResourceManagerImpl() {
        return new TaggedResourceManagerImpl();
    }

    @Bean("resourceIconManager")
    public ResourceIconManagerImpl resourceIconManager() {
        return new ResourceIconManagerImpl();
    }

    @Bean("bgPollManager")
    public BackgroundPollManagerImpl bgPollManager() {
        return new BackgroundPollManagerImpl();
    }

    @Bean("domainManagerImpl")
    public DomainManagerImpl domainManagerImpl() {
        return new DomainManagerImpl();
    }

    @Bean("basicNetworkVisitor")
    public BasicNetworkVisitor basicNetworkVisitor() {
        return new BasicNetworkVisitor();
    }

    @Bean("roleManagerImpl")
    public RoleManagerImpl roleManagerImpl() {
        return new RoleManagerImpl();
    }

    @Bean("routerDeploymentDefinitionBuilder")
    public RouterDeploymentDefinitionBuilder routerDeploymentDefinitionBuilder() {
        return new RouterDeploymentDefinitionBuilder();
    }

    @Bean("rulesManagerImpl")
    public RulesManagerImpl rulesManagerImpl() {
        return new RulesManagerImpl();
    }

    @Bean("vMSnapshotManagerImpl")
    public VMSnapshotManagerImpl vMSnapshotManagerImpl() {
        return new VMSnapshotManagerImpl();
    }

    @Bean("alertManagerImpl")
    public AlertManagerImpl alertManagerImpl() {
        return new AlertManagerImpl();
    }

    @Bean("storageLayer")
    public JavaStorageLayer storageLayer() {
        return new JavaStorageLayer();
    }

    @Bean("queryManagerImpl")
    public QueryManagerImpl queryManagerImpl() {
        return new QueryManagerImpl();
    }

    @Bean("ipAddressManagerImpl")
    public IpAddressManagerImpl ipAddressManagerImpl() {
        return new IpAddressManagerImpl();
    }

    @Bean("virtualNetworkApplianceManagerImpl")
    public VirtualNetworkApplianceManagerImpl virtualNetworkApplianceManagerImpl() {
        return new VirtualNetworkApplianceManagerImpl();
    }

    @Bean("basicNetworkTopology")
    public BasicNetworkTopology basicNetworkTopology() {
        return new BasicNetworkTopology();
    }

    @Bean("storageNetworkManagerImpl")
    public StorageNetworkManagerImpl storageNetworkManagerImpl() {
        return new StorageNetworkManagerImpl();
    }

    @Bean("ApplicationLoadBalancerService")
    public ApplicationLoadBalancerManagerImpl ApplicationLoadBalancerService() {
        return new ApplicationLoadBalancerManagerImpl();
    }

    @Bean(name = "topologyContext", initMethod = "init")
    public NetworkTopologyContext topologyContext() {
        return new NetworkTopologyContext();
    }

}
