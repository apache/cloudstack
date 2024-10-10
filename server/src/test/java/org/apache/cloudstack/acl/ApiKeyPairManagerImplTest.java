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
package org.apache.cloudstack.acl;

import com.cloud.user.UserVO;
import com.cloud.user.dao.UserDao;
import org.apache.cloudstack.acl.dao.ApiKeyPairDao;
import org.apache.cloudstack.acl.dao.ApiKeyPairPermissionsDao;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class ApiKeyPairManagerImplTest {
    @Mock
    ApiKeyPairPermissionsDao apiKeyPairPermissionsDao;
    @Mock
    ApiKeyPairDao apiKeyPairDao;
    @Mock
    UserDao userDao;
    @InjectMocks
    ApiKeyPairManagerImpl apiKeyPairManager;

    @Before
    public void setup() {
        Mockito.when(apiKeyPairPermissionsDao.findAllByApiKeyPairId(Mockito.any())).thenReturn(List.of(new ApiKeyPairPermissionVO()));
        Mockito.when(userDao.findByIdIncludingRemoved(Mockito.any())).thenReturn(new UserVO());
    }

    @Test
    public void deleteApiKeyTestOnePermission() {
        apiKeyPairManager.deleteApiKey(new ApiKeyPairVO(1L, 1L));
        Mockito.verify(apiKeyPairPermissionsDao, Mockito.times(1)).remove(Mockito.anyLong());
        Mockito.verify(apiKeyPairDao, Mockito.times(1)).remove(Mockito.anyLong());
    }
}
