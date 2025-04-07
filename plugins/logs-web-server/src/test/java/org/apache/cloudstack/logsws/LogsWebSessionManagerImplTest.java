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
package org.apache.cloudstack.logsws;

import java.util.UUID;

import org.apache.cloudstack.logsws.dao.LogsWebSessionDao;
import org.apache.cloudstack.logsws.vo.LogsWebSessionVO;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class LogsWebSessionManagerImplTest {

    @Mock
    LogsWebSessionDao logsWebSessionDao;

    @InjectMocks
    LogsWebSessionManagerImpl logsWSManager = new LogsWebSessionManagerImpl();

    @Test
    public void test_getSession_nullRoute() {
        Assert.assertNull(logsWSManager.getSession(null));
        Assert.assertNull(logsWSManager.getSession("abc"));
    }

    @Test
    public void test_getSession_validRoute() {
        String uuid = UUID.randomUUID().toString();
        Mockito.when(logsWebSessionDao.findByUuid(uuid)).thenReturn(Mockito.mock(LogsWebSessionVO.class));
        Assert.assertNotNull(logsWSManager.getSession(uuid));
    }
}
