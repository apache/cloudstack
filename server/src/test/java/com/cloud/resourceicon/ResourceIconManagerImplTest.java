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
package com.cloud.resourceicon;

import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.context.CallContext;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.network.dao.NetworkVO;
import com.cloud.resource.icon.ResourceIconVO;
import com.cloud.resource.icon.dao.ResourceIconDao;
import com.cloud.server.ResourceManagerUtil;
import com.cloud.server.ResourceTag;
import com.cloud.storage.VMTemplateVO;
import com.cloud.user.Account;
import com.cloud.user.AccountVO;
import com.cloud.user.User;
import com.cloud.user.UserVO;
import com.cloud.user.dao.AccountDao;
import com.cloud.user.dao.UserDao;
import com.cloud.utils.db.EntityManager;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@RunWith(MockitoJUnitRunner.class)
public class ResourceIconManagerImplTest {
    public static final long ACCOUNT_ID = 1L;
    public static final Long RESOURCE_ID = 1L;

    @Mock
    protected AccountDao accountDao;
    @Mock
    protected UserDao userDao;
    @Mock
    ResourceManagerUtil resourceManagerUtil;
    @Mock
    ResourceIconDao resourceIconDao;
    @Mock
    EntityManager entityMgr;

    @Spy
    @InjectMocks
    private ResourceIconManagerImpl resourceIconManager = new ResourceIconManagerImpl();

    private AccountVO account;
    private UserVO user;

    @Before
    public void setup() throws Exception {
        account = new AccountVO("testaccount", 1L, "networkdomain", Account.Type.NORMAL, "uuid");
        account.setId(ACCOUNT_ID);
        user = new UserVO(ACCOUNT_ID, "testuser", "password", "firstname", "lastName", "email", "timezone",
                UUID.randomUUID().toString(), User.Source.UNKNOWN);
    }

    @Test
    public void testUploadIconActionEventResourceDetailsUpdate() {
        CallContext.register(user, account);
        String uuid = UUID.randomUUID().toString();
        ResourceTag.ResourceObjectType resourceType = ResourceTag.ResourceObjectType.Network;
        List<String> resourceIds = new ArrayList<>();
        resourceIds.add(uuid);
        Mockito.when(resourceManagerUtil.getResourceId(uuid, resourceType)).thenReturn(RESOURCE_ID);
        NetworkVO network = Mockito.mock(NetworkVO.class);
        Mockito.when(network.getAccountId()).thenReturn(1L);
        Mockito.when((network.getDomainId())).thenReturn(1L);
        Mockito.when(entityMgr.findById(NetworkVO.class, RESOURCE_ID)).thenReturn(network);
        Mockito.doNothing().when(resourceManagerUtil).checkResourceAccessible(Mockito.anyLong(), Mockito.anyLong(), Mockito.anyString());
        Mockito.when(resourceIconDao.persist(Mockito.any(ResourceIconVO.class))).thenReturn(Mockito.mock(ResourceIconVO.class));
        resourceIconManager.uploadResourceIcon(resourceIds, resourceType, "Something");
        Assert.assertEquals(RESOURCE_ID, CallContext.current().getEventResourceId());
        Assert.assertEquals(ApiCommandResourceType.Network, CallContext.current().getEventResourceType());
    }

    @Test
    public void testDeleteIconActionEventResourceDetailsUpdate() {
        CallContext.register(user, account);
        String uuid = UUID.randomUUID().toString();
        String iconUuid = UUID.randomUUID().toString();
        ResourceTag.ResourceObjectType resourceType = ResourceTag.ResourceObjectType.Template;
        List<String> resourceIds = new ArrayList<>();
        resourceIds.add(uuid);
        List<ResourceIconVO> resourceIcons = new ArrayList<>();
        ResourceIconVO resourceIcon = Mockito.mock(ResourceIconVO.class);
        Mockito.when(resourceIcon.getResourceUuid()).thenReturn(uuid);
        resourceIcons.add(resourceIcon);
        Mockito.when(resourceManagerUtil.getResourceId(uuid, resourceType)).thenReturn(RESOURCE_ID);
        Mockito.when(resourceManagerUtil.getUuid(uuid, resourceType)).thenReturn(iconUuid);
        SearchBuilder<ResourceIconVO> searchBuilder = Mockito.mock(SearchBuilder.class);
        SearchCriteria<ResourceIconVO> searchCriteria = Mockito.mock(SearchCriteria.class);
        when(searchBuilder.create()).thenReturn(searchCriteria);
        when(searchBuilder.entity()).thenReturn(resourceIcon);
        Mockito.when(resourceIconDao.createSearchBuilder()).thenReturn(searchBuilder);
        Mockito.when(resourceIconDao.search(Mockito.any(), Mockito.any())).thenReturn(resourceIcons);
        VMTemplateVO template = Mockito.mock(VMTemplateVO.class);
        Mockito.when(template.getAccountId()).thenReturn(1L);
        Mockito.when((template.getDomainId())).thenReturn(1L);
        Mockito.when(entityMgr.findById(VMTemplateVO.class, RESOURCE_ID)).thenReturn(template);
        Mockito.doNothing().when(resourceManagerUtil).checkResourceAccessible(Mockito.anyLong(), Mockito.anyLong(), Mockito.anyString());
        Mockito.when(resourceIconDao.remove(Mockito.anyLong())).thenReturn(true);
        resourceIconManager.deleteResourceIcon(resourceIds, resourceType);
        Assert.assertEquals(RESOURCE_ID, CallContext.current().getEventResourceId());
        Assert.assertEquals(ApiCommandResourceType.Template, CallContext.current().getEventResourceType());
    }
}
