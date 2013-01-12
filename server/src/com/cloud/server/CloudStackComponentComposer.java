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
package com.cloud.server;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.cloud.agent.AgentManager;
import com.cloud.alert.AlertManagerImpl;
import com.cloud.api.query.QueryManagerImpl;
import com.cloud.async.AsyncJobManager;
import com.cloud.async.SyncQueueManager;
import com.cloud.capacity.CapacityManagerImpl;
import com.cloud.cluster.ClusterFenceManagerImpl;
import com.cloud.cluster.ClusterManagerImpl;
import com.cloud.configuration.ConfigurationManager;
import com.cloud.consoleproxy.ConsoleProxyManager;
import com.cloud.dao.EntityManagerImpl;
import com.cloud.ha.HighAvailabilityManager;
import com.cloud.hypervisor.HypervisorGuruManagerImpl;
import com.cloud.keystore.KeystoreManager;
import com.cloud.maint.UpgradeManagerImpl;
import com.cloud.network.ExternalLoadBalancerUsageManager;
import com.cloud.network.NetworkManagerImpl;
import com.cloud.network.StorageNetworkManager;
import com.cloud.network.as.AutoScaleManagerImpl;
import com.cloud.network.firewall.FirewallManagerImpl;
import com.cloud.network.lb.LoadBalancingRulesManagerImpl;
import com.cloud.network.router.VpcVirtualNetworkApplianceManager;
import com.cloud.network.rules.RulesManagerImpl;
import com.cloud.network.security.SecurityGroupManagerImpl2;
import com.cloud.network.vpc.NetworkACLManagerImpl;
import com.cloud.network.vpc.VpcManagerImpl;
import com.cloud.network.vpn.RemoteAccessVpnManagerImpl;
import com.cloud.network.vpn.Site2SiteVpnManagerImpl;
import com.cloud.projects.ProjectManagerImpl;
import com.cloud.resource.ResourceManagerImpl;
import com.cloud.resourcelimit.ResourceLimitManagerImpl;
import com.cloud.storage.OCFS2Manager;
import com.cloud.storage.StorageManagerImpl;
import com.cloud.storage.download.DownloadMonitor;
import com.cloud.storage.s3.S3Manager;
import com.cloud.storage.secondary.SecondaryStorageManagerImpl;
import com.cloud.storage.snapshot.SnapshotManagerImpl;
import com.cloud.storage.snapshot.SnapshotSchedulerImpl;
import com.cloud.storage.swift.SwiftManager;
import com.cloud.storage.upload.UploadMonitor;
import com.cloud.tags.TaggedResourceManagerImpl;
import com.cloud.template.TemplateManagerImpl;
import com.cloud.user.AccountManagerImpl;
import com.cloud.user.DomainManagerImpl;
import com.cloud.utils.component.Manager;
import com.cloud.vm.UserVmManagerImpl;
import com.cloud.vm.VirtualMachineManager;

@Component
public class CloudStackComponentComposer {
	// @Inject CheckPointManagerImpl _checkPointMgr;
    @Inject ClusterManagerImpl _clusterMgr;
    @Inject ClusterFenceManagerImpl _clusterFenceMgr;
    @Inject AgentManager _AgentMgr;
    @Inject SyncQueueManager _sycnQueueMgr;
    @Inject AsyncJobManager _jobMgr;
    @Inject ConfigurationManager _confMgr;
    @Inject AccountManagerImpl _accountMgr;
    @Inject DomainManagerImpl _domainMgr;
    @Inject ResourceLimitManagerImpl _resLimitMgr;
    @Inject NetworkManagerImpl _networkMgr;
    @Inject DownloadMonitor _downloadMonitor;
    @Inject UploadMonitor _uploadMonitor;
    @Inject KeystoreManager _ksMgr;
    @Inject SecondaryStorageManagerImpl _ssMgr;
    @Inject UserVmManagerImpl _userVmMgr;
    @Inject UpgradeManagerImpl _upgradeMgr;
    @Inject StorageManagerImpl _storageMgr;
    @Inject AlertManagerImpl _alertMgr;
    @Inject TemplateManagerImpl _tmplMgr;
    @Inject SnapshotManagerImpl _snpahsotMgr;
    @Inject SnapshotSchedulerImpl _snapshotScheduleMgr;
    @Inject SecurityGroupManagerImpl2 _sgMgr;
    @Inject EntityManagerImpl _entityMgr;
    @Inject LoadBalancingRulesManagerImpl _lbRuleMgr;
    @Inject AutoScaleManagerImpl _asMgr;
    @Inject RulesManagerImpl _rulesMgr;
    @Inject RemoteAccessVpnManagerImpl _acVpnMgr;
    @Inject CapacityManagerImpl _capacityMgr;
    @Inject VirtualMachineManager _vmMgr;
    @Inject HypervisorGuruManagerImpl _hvGuruMgr;
    @Inject ResourceManagerImpl _resMgr;
    @Inject OCFS2Manager _ocfsMgr;
    @Inject FirewallManagerImpl _fwMgr;
    @Inject ConsoleProxyManager _cpMgr;
    @Inject ProjectManagerImpl _prjMgr;
    @Inject SwiftManager _swiftMgr;
    @Inject S3Manager _s3Mgr;
    @Inject StorageNetworkManager _storageNetworkMgr;
    @Inject ExternalLoadBalancerUsageManager _extlbUsageMgr;
    @Inject HighAvailabilityManager _haMgr;
    @Inject VpcManagerImpl _vpcMgr;
    @Inject VpcVirtualNetworkApplianceManager _vpcNetApplianceMgr;
    @Inject NetworkACLManagerImpl _networkAclMgr;
    @Inject TaggedResourceManagerImpl _taggedResMgr;
    @Inject Site2SiteVpnManagerImpl _s2sVpnMgr;
    @Inject QueryManagerImpl _queryMgr;
    
    List<Manager> _managers = new ArrayList<Manager>();

    public CloudStackComponentComposer() {
    }
    
    @PostConstruct
    void init() {
    	// _managers.add(_checkPointMgr);
        _managers.add(_clusterMgr);
        _managers.add(_clusterFenceMgr);
        _managers.add(_AgentMgr);
        _managers.add(_sycnQueueMgr);
        _managers.add(_jobMgr);
        _managers.add(_confMgr);
        _managers.add(_accountMgr);
        _managers.add(_domainMgr);
        _managers.add(_resLimitMgr);
        _managers.add(_networkMgr);
        _managers.add(_downloadMonitor);
        _managers.add(_uploadMonitor);
        _managers.add(_ksMgr);
        _managers.add(_ssMgr);
        _managers.add(_userVmMgr);
        _managers.add(_upgradeMgr);
        _managers.add(_storageMgr);
        _managers.add(_alertMgr);
        _managers.add(_tmplMgr);
        _managers.add(_snpahsotMgr);
        _managers.add(_snapshotScheduleMgr);
        _managers.add(_sgMgr);
        _managers.add(_entityMgr);
        _managers.add(_lbRuleMgr);
        _managers.add(_asMgr);
        _managers.add(_rulesMgr);
        _managers.add(_acVpnMgr);
        _managers.add(_capacityMgr);
        _managers.add(_vmMgr);
        _managers.add(_hvGuruMgr);
        _managers.add(_resMgr);
        _managers.add(_ocfsMgr);
        _managers.add(_fwMgr);
        _managers.add(_cpMgr);
        _managers.add(_prjMgr);
        _managers.add(_swiftMgr);
        _managers.add(_s3Mgr);
        _managers.add(_storageNetworkMgr);
        _managers.add(_extlbUsageMgr);
        _managers.add(_haMgr);
        _managers.add(_vpcMgr);
        _managers.add(_vpcNetApplianceMgr);
        _managers.add(_networkAclMgr);
        _managers.add(_taggedResMgr);
        _managers.add(_s2sVpnMgr);
        _managers.add(_queryMgr);
    }
    
    public List<Manager> getManagers() {
    	return _managers;
    }
}
