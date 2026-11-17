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
package com.cloud.user.dao;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import com.cloud.user.UserDataVO;
import com.cloud.utils.db.SearchCriteria;

public class UserDataDaoImplTest {

    @Test
    public void findByUserDataMatchesOnDecryptedValue() {
        UserDataDaoImpl userDataDao = Mockito.spy(new UserDataDaoImpl());
        UserDataVO other = Mockito.mock(UserDataVO.class);
        Mockito.when(other.getUserData()).thenReturn("b3RoZXI=");
        UserDataVO match = Mockito.mock(UserDataVO.class);
        Mockito.when(match.getUserData()).thenReturn("I2Nsb3VkLWNvbmZpZw==");
        Mockito.doReturn(List.of(other, match)).when(userDataDao).listBy(Mockito.any(SearchCriteria.class));

        Assert.assertEquals(match, userDataDao.findByUserData(1L, 1L, "I2Nsb3VkLWNvbmZpZw=="));
        Assert.assertNull(userDataDao.findByUserData(1L, 1L, "bm9uZQ=="));
    }
}
