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
package com.cloud.user;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.apache.cloudstack.acl.SecurityChecker;
import org.apache.cloudstack.affinity.dao.AffinityGroupDao;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.engine.orchestration.service.NetworkOrchestrationService;
import org.apache.cloudstack.engine.service.api.OrchestrationService;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.framework.messagebus.MessageBus;
import org.apache.cloudstack.region.gslb.GlobalLoadBalancerRuleDao;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import com.cloud.configuration.ConfigurationManager;
import com.cloud.configuration.dao.ResourceCountDao;
import com.cloud.configuration.dao.ResourceLimitDao;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.DataCenterVnetDao;
import com.cloud.dc.dao.DedicatedResourceDao;
import com.cloud.domain.dao.DomainDao;
import com.cloud.event.dao.UsageEventDao;
import com.cloud.network.as.AutoScaleManager;
import com.cloud.network.dao.AccountGuestVlanMapDao;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.RemoteAccessVpnDao;
import com.cloud.network.dao.VpnUserDao;
import com.cloud.network.security.SecurityGroupManager;
import com.cloud.network.security.dao.SecurityGroupDao;
import com.cloud.network.vpc.VpcManager;
import com.cloud.network.vpn.RemoteAccessVpnService;
import com.cloud.network.vpn.Site2SiteVpnManager;
import com.cloud.projects.ProjectManager;
import com.cloud.projects.dao.ProjectAccountDao;
import com.cloud.projects.dao.ProjectDao;
import com.cloud.server.auth.UserAuthenticator;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.storage.VolumeApiService;
import com.cloud.storage.dao.SnapshotDao;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.storage.snapshot.SnapshotManager;
import com.cloud.template.TemplateManager;
import com.cloud.user.dao.AccountDao;
import com.cloud.user.dao.UserAccountDao;
import com.cloud.user.dao.UserDao;
import com.cloud.vm.VirtualMachineManager;
import com.cloud.vm.dao.DomainRouterDao;
import com.cloud.vm.dao.InstanceGroupDao;
import com.cloud.vm.dao.UserVmDao;
import com.cloud.vm.dao.VMInstanceDao;
import com.cloud.vm.snapshot.VMSnapshotManager;
import com.cloud.vm.snapshot.dao.VMSnapshotDao;

@RunWith(MockitoJUnitRunner.class)
public class AccountManagetImplTestBase {

    @Mock
    AccountDao _accountDao;
    @Mock
    ConfigurationDao _configDao;
    @Mock
    ResourceCountDao _resourceCountDao;
    @Mock
    UserDao _userDao;
    @Mock
    InstanceGroupDao _vmGroupDao;
    @Mock
    UserAccountDao _userAccountDao;
    @Mock
    VolumeDao _volumeDao;
    @Mock
    UserVmDao _userVmDao;
    @Mock
    VMTemplateDao _templateDao;
    @Mock
    NetworkDao _networkDao;
    @Mock
    SecurityGroupDao _securityGroupDao;
    @Mock
    VMInstanceDao _vmDao;
    @Mock
    protected SnapshotDao _snapshotDao;
    @Mock
    protected VMTemplateDao _vmTemplateDao;
    @Mock
    SecurityGroupManager _networkGroupMgr;
    @Mock
    NetworkOrchestrationService _networkMgr;
    @Mock
    SnapshotManager _snapMgr;
    @Mock
    TemplateManager _tmpltMgr;
    @Mock
    ConfigurationManager _configMgr;
    @Mock
    VirtualMachineManager _itMgr;
    @Mock
    RemoteAccessVpnDao _remoteAccessVpnDao;
    @Mock
    RemoteAccessVpnService _remoteAccessVpnMgr;
    @Mock
    VpnUserDao _vpnUser;
    @Mock
    DataCenterDao _dcDao;
    @Mock
    DomainManager _domainMgr;
    @Mock
    ProjectManager _projectMgr;
    @Mock
    ProjectDao _projectDao;
    @Mock
    AccountDetailsDao _accountDetailsDao;
    @Mock
    DomainDao _domainDao;
    @Mock
    ProjectAccountDao _projectAccountDao;
    @Mock
    IPAddressDao _ipAddressDao;
    @Mock
    VpcManager _vpcMgr;
    @Mock
    DomainRouterDao _routerDao;
    @Mock
    Site2SiteVpnManager _vpnMgr;
    @Mock
    AutoScaleManager _autoscaleMgr;
    @Mock
    VolumeApiService volumeService;
    @Mock
    AffinityGroupDao _affinityGroupDao;
    @Mock
    AccountGuestVlanMapDao _accountGuestVlanMapDao;
    @Mock
    DataCenterVnetDao _dataCenterVnetDao;
    @Mock
    ResourceLimitService _resourceLimitMgr;
    @Mock
    ResourceLimitDao _resourceLimitDao;
    @Mock
    DedicatedResourceDao _dedicatedDao;
    @Mock
    GlobalLoadBalancerRuleDao _gslbRuleDao;
    @Mock
    MessageBus _messageBus;
    @Mock
    VMSnapshotManager _vmSnapshotMgr;
    @Mock
    VMSnapshotDao _vmSnapshotDao;
    @Mock
    User callingUser;
    @Mock
    Account callingAccount;

    @Mock
    SecurityChecker securityChecker;
    @Mock
    UserAuthenticator userAuthenticator;
    @Mock
    ServiceOfferingDao _serviceOfferingDao;
    @Mock
    ServiceOfferingDao _offeringDao;
    @Mock
    OrchestrationService _orchSrvc;


    AccountManagerImpl accountManager;

    UsageEventDao _usageEventDao = new MockUsageEventDao();

    @Before
    public void setup()
            throws NoSuchFieldException, SecurityException,
            IllegalArgumentException, IllegalAccessException {
        accountManager = new AccountManagerImpl();
        Map<String, Field> declaredFields = getInheritedFields(this.getClass());
        for (Field field : AccountManagerImpl.class.getDeclaredFields()) {
            if (field.getAnnotation(Inject.class) != null) {
                field.setAccessible(true);
                if (declaredFields.containsKey(field.getName())) {
                    Field mockField = declaredFields.get(field.getName());
                    field.set(accountManager, mockField.get(this));
                }
            }
        }
        ReflectionTestUtils.setField(accountManager, "_userAuthenticators", Arrays.asList(userAuthenticator));
        accountManager.setSecurityCheckers(Arrays.asList(securityChecker));
        CallContext.register(callingUser, callingAccount);
    }

    @After
    public void cleanup() {
        CallContext.unregister();
    }

    public static Map<String, Field> getInheritedFields(Class<?> type) {
        Map<String, Field> fields = new HashMap<>();
        for (Class<?> c = type; c != null; c = c.getSuperclass()) {
            for (Field f : c.getDeclaredFields()) {
                fields.put(f.getName(), f);
            }
        }
        return fields;
    }

    public static Map<Class<?>, Field> getInheritedFieldsByClass(Class<?> type) {
        Map<Class<?>, Field> fields = new HashMap<>();
        for (Class<?> c = type; c != null; c = c.getSuperclass()) {
            for (Field f : c.getDeclaredFields()) {
                fields.put(f.getType(), f);
            }
        }
        return fields;
    }
}
