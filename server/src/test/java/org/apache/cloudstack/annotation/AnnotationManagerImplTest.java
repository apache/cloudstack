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
package org.apache.cloudstack.annotation;

import java.util.UUID;

import org.apache.cloudstack.acl.Role;
import org.apache.cloudstack.acl.RoleService;
import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.annotation.dao.AnnotationDao;
import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.command.admin.annotation.RemoveAnnotationCmd;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.storage.datastore.db.ImageStoreDao;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.dc.dao.ClusterDao;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.HostPodDao;
import com.cloud.domain.dao.DomainDao;
import com.cloud.host.dao.HostDao;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.Site2SiteCustomerGatewayDao;
import com.cloud.network.vpc.dao.VpcDao;
import com.cloud.offerings.dao.NetworkOfferingDao;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.storage.Volume;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.DiskOfferingDao;
import com.cloud.storage.dao.SnapshotDao;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.user.Account;
import com.cloud.user.AccountService;
import com.cloud.user.AccountVO;
import com.cloud.user.User;
import com.cloud.user.UserVO;
import com.cloud.user.dao.AccountDao;
import com.cloud.user.dao.SSHKeyPairDao;
import com.cloud.user.dao.UserDao;
import com.cloud.utils.db.EntityManager;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.dao.InstanceGroupDao;
import com.cloud.vm.dao.VMInstanceDao;
import com.cloud.vm.snapshot.dao.VMSnapshotDao;

@RunWith(MockitoJUnitRunner.class)
public class AnnotationManagerImplTest {
    public static final long ACCOUNT_ID = 1L;
    public static final long USER_ID = 0L;
    public static final Long ENTITY_ID = 1L;

    @Mock
    AnnotationDao annotationDao;
    @Mock
    UserDao userDao;
    @Mock
    AccountDao accountDao;
    @Mock
    RoleService roleService;
    @Mock
    AccountService accountService;
    @Mock
    VMInstanceDao vmInstanceDao;
    @Mock
    VolumeDao volumeDao;
    @Mock
    SnapshotDao snapshotDao;
    @Mock
    VMSnapshotDao vmSnapshotDao;
    @Mock
    InstanceGroupDao instanceGroupDao;
    @Mock
    SSHKeyPairDao sshKeyPairDao;
    @Mock
    NetworkDao networkDao;
    @Mock
    VpcDao vpcDao;
    @Mock
    IPAddressDao ipAddressDao;
    @Mock
    Site2SiteCustomerGatewayDao customerGatewayDao;
    @Mock
    VMTemplateDao templateDao;
    @Mock
    DataCenterDao dataCenterDao;
    @Mock
    HostPodDao hostPodDao;
    @Mock
    ClusterDao clusterDao;
    @Mock
    HostDao hostDao;
    @Mock
    PrimaryDataStoreDao primaryDataStoreDao;
    @Mock
    ImageStoreDao imageStoreDao;
    @Mock
    DomainDao domainDao;
    @Mock
    ServiceOfferingDao serviceOfferingDao;
    @Mock
    DiskOfferingDao diskOfferingDao;
    @Mock
    NetworkOfferingDao networkOfferingDao;
    @Mock
    EntityManager entityManager;

    @InjectMocks
    private AnnotationManagerImpl annotationManager = new AnnotationManagerImpl();

    private AccountVO account;
    private UserVO user;

    @Before
    public void setup() throws Exception {
        account = new AccountVO("testaccount", 1L, "networkdomain", Account.Type.NORMAL, "uuid");
        account.setId(ACCOUNT_ID);
        user = new UserVO(ACCOUNT_ID, "testuser", "password", "firstname", "lastName", "email", "timezone",
                UUID.randomUUID().toString(), User.Source.UNKNOWN);

        Mockito.when(accountDao.findById(ACCOUNT_ID)).thenReturn(account);
        Mockito.when(userDao.findById(USER_ID)).thenReturn(user);
    }

    @Test
    public void testAddAnnotationResourceDetailsUpdate() {
        CallContext.register(user, account);
        String uuid = UUID.randomUUID().toString();
        VolumeVO volume = Mockito.mock(VolumeVO.class);
        Mockito.when(volume.getId()).thenReturn(ENTITY_ID);
        Mockito.when(volumeDao.findByUuid(uuid)).thenReturn(volume);
        Mockito.doNothing().when(accountService).checkAccess(user, volume);
        Role role = Mockito.mock(Role.class);
        Mockito.when(role.getRoleType()).thenReturn(RoleType.User);
        Mockito.when(roleService.findRole(Mockito.anyLong())).thenReturn(role);
        AnnotationVO annotationVO = Mockito.mock(AnnotationVO.class);
        AnnotationService.EntityType type = AnnotationService.EntityType.VOLUME;
        Mockito.when(annotationVO.getEntityType()).thenReturn(type);
        Mockito.when(annotationDao.persist(Mockito.any())).thenReturn(annotationVO);
        Mockito.when(entityManager.findByUuid(Volume.class, uuid)).thenReturn(volume);
        annotationManager.addAnnotation("Some text", type, uuid, false);
        Assert.assertEquals(ENTITY_ID, CallContext.current().getEventResourceId());
        Assert.assertEquals(ApiCommandResourceType.Volume, CallContext.current().getEventResourceType());
    }

    @Test
    public void testRemoveAnnotationResourceDetailsUpdate() {
        CallContext.register(user, account);
        String uuid = UUID.randomUUID().toString();
        String annotationUuid = UUID.randomUUID().toString();
        VirtualMachine vm = Mockito.mock(VirtualMachine.class);
        Mockito.when(vm.getId()).thenReturn(ENTITY_ID);
        Role role = Mockito.mock(Role.class);
        Mockito.when(role.getRoleType()).thenReturn(RoleType.User);
        Mockito.when(roleService.findRole(Mockito.anyLong())).thenReturn(role);
        AnnotationVO annotationVO = Mockito.mock(AnnotationVO.class);
        AnnotationService.EntityType type = AnnotationService.EntityType.VM;
        Mockito.when(annotationVO.getUuid()).thenReturn(annotationUuid);
        Mockito.when(annotationVO.getUserUuid()).thenReturn(user.getUuid());
        Mockito.when(annotationVO.getEntityType()).thenReturn(type);
        Mockito.when(annotationVO.getEntityUuid()).thenReturn(uuid);
        Mockito.when(annotationDao.findByUuid(annotationUuid)).thenReturn(annotationVO);
        Mockito.when(entityManager.findByUuid(VirtualMachine.class, uuid)).thenReturn(vm);
        RemoveAnnotationCmd cmd = Mockito.mock(RemoveAnnotationCmd.class);
        Mockito.when(cmd.getUuid()).thenReturn(annotationUuid);
        annotationManager.removeAnnotation(cmd);
        Assert.assertEquals(ENTITY_ID, CallContext.current().getEventResourceId());
        Assert.assertEquals(ApiCommandResourceType.VirtualMachine, CallContext.current().getEventResourceType());
    }
}
