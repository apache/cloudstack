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

package com.cloud.network;

import com.cloud.agent.MockAgentManagerImpl;
import com.cloud.alert.AlertManagerImpl;
import com.cloud.alert.MockAlertManagerImpl;
import com.cloud.baremetal.ExternalDhcpManagerImpl;
import com.cloud.configuration.ConfigurationManagerImpl;
import com.cloud.configuration.DefaultComponentLibrary;
import com.cloud.network.as.AutoScaleManagerImpl;
import com.cloud.network.firewall.FirewallManagerImpl;
import com.cloud.network.lb.LoadBalancingRulesManagerImpl;
import com.cloud.network.router.VpcVirtualNetworkApplianceManagerImpl;
import com.cloud.network.rules.RulesManagerImpl;
import com.cloud.network.security.SecurityGroupManagerImpl2;
import com.cloud.network.vpc.NetworkACLManagerImpl;
import com.cloud.network.vpc.VpcManagerImpl;
import com.cloud.network.vpn.RemoteAccessVpnManagerImpl;
import com.cloud.network.vpn.Site2SiteVpnManagerImpl;
import com.cloud.projects.MockProjectManagerImpl;
import com.cloud.projects.ProjectManagerImpl;
import com.cloud.resource.MockResourceManagerImpl;
import com.cloud.resource.ResourceManagerImpl;
import com.cloud.resourcelimit.ResourceLimitManagerImpl;
import com.cloud.storage.s3.S3ManagerImpl;
import com.cloud.storage.secondary.SecondaryStorageManagerImpl;
import com.cloud.storage.swift.SwiftManagerImpl;
import com.cloud.tags.TaggedResourceManagerImpl;
import com.cloud.template.TemplateManagerImpl;
import com.cloud.user.AccountManagerImpl;
import com.cloud.user.DomainManagerImpl;
import com.cloud.user.MockAccountManagerImpl;
import com.cloud.user.MockDomainManagerImpl;
import com.cloud.vm.MockVirtualMachineManagerImpl;
import com.cloud.vpc.MockConfigurationManagerImpl;
import com.cloud.vpc.MockResourceLimitManagerImpl;
import com.cloud.vpc.MockVpcManagerImpl;
import com.cloud.vpc.MockVpcVirtualNetworkApplianceManager;


public class NetworkManagerTestComponentLibrary extends DefaultComponentLibrary {

    /* (non-Javadoc)
     * @see com.cloud.configuration.DefaultComponentLibrary#populateManagers()
     */
    @Override
    protected void populateManagers() {
        addManager("configuration manager", MockConfigurationManagerImpl.class);
        addManager("account manager", MockAccountManagerImpl.class);
        addManager("domain manager", MockDomainManagerImpl.class);
        addManager("resource limit manager", MockResourceLimitManagerImpl.class);
        addManager("network service", NetworkServiceImpl.class);
        addManager("network manager", NetworkManagerImpl.class);
        addManager("network model", NetworkModelImpl.class);
        addManager("LoadBalancingRulesManager", LoadBalancingRulesManagerImpl.class);
        //addManager("AutoScaleManager", AutoScaleManagerImpl.class);
        addManager("RulesManager", RulesManagerImpl.class);
        addManager("RemoteAccessVpnManager", RemoteAccessVpnManagerImpl.class);
        addManager("FirewallManager", FirewallManagerImpl.class);
        addManager("StorageNetworkManager", StorageNetworkManagerImpl.class);
        addManager("VPC Manager", MockVpcManagerImpl.class);
        addManager("VpcVirtualRouterManager", MockVpcVirtualNetworkApplianceManager.class);
        addManager("NetworkACLManager", NetworkACLManagerImpl.class);
        addManager("Site2SiteVpnManager", Site2SiteVpnManagerImpl.class);
        addManager("Alert Manager", MockAlertManagerImpl.class);
        addManager("ProjectManager", MockProjectManagerImpl.class);
        //addManager("SwiftManager", SwiftManagerImpl.class);
        //addManager("S3Manager", S3ManagerImpl.class);
        //addManager("SecondaryStorageManager", SecondaryStorageManagerImpl.class);
        //addManager("SecurityGroupManager", SecurityGroupManagerImpl2.class);
        addManager("AgentManager", MockAgentManagerImpl.class);
        addManager("ExternalLoadBalancerUsageManager", ExternalLoadBalancerUsageManagerImpl.class);
        //addManager("TemplateManager", TemplateManagerImpl.class);
        //addManager("VirtualMachineManager", MockVirtualMachineManagerImpl.class);
        addManager("ResourceManager", MockResourceManagerImpl.class);
        addManager("ExternalDhcpManager", ExternalDhcpManagerImpl.class);




    }

    @Override
    protected void populateAdapters() {
       //no-op
    }

}
