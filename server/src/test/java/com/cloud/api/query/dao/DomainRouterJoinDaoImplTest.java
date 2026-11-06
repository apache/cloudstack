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
package com.cloud.api.query.dao;

import java.util.Set;

import org.apache.cloudstack.annotation.dao.AnnotationDao;
import org.apache.cloudstack.api.response.DomainRouterResponse;
import org.apache.cloudstack.api.response.NicResponse;
import org.apache.cloudstack.context.CallContext;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import com.cloud.api.ApiDBUtils;
import com.cloud.api.query.vo.DomainRouterJoinVO;
import com.cloud.cpu.CPU;
import com.cloud.network.Networks.TrafficType;
import com.cloud.network.router.VirtualRouter;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.AccountVO;
import com.cloud.user.User;
import com.cloud.user.UserVO;

@RunWith(MockitoJUnitRunner.Silent.class)
public class DomainRouterJoinDaoImplTest {

    @Mock
    private AccountManager accountManager;
    @Mock
    private AnnotationDao annotationDao;

    @InjectMocks
    private DomainRouterJoinDaoImpl dao = new DomainRouterJoinDaoImpl();

    private MockedStatic<ApiDBUtils> apiDBUtils;

    @Before
    public void setUp() {
        apiDBUtils = Mockito.mockStatic(ApiDBUtils.class);
        final AccountVO account = new AccountVO("testaccount", 1L, "networkdomain", Account.Type.NORMAL, "uuid");
        account.setId(1L);
        final UserVO user = new UserVO(1, "testuser", "password", "firstname", "lastName", "email", "timezone",
                "user-uuid", User.Source.UNKNOWN);
        CallContext.register(user, account);
    }

    @After
    public void tearDown() {
        CallContext.unregister();
        apiDBUtils.close();
    }

    private DomainRouterJoinVO router(Integer nicNetworkRate) {
        final DomainRouterJoinVO router = Mockito.mock(DomainRouterJoinVO.class);
        Mockito.when(router.getUuid()).thenReturn("router-uuid");
        Mockito.when(router.getArch()).thenReturn(CPU.CPUArch.amd64);
        Mockito.when(router.getRole()).thenReturn(VirtualRouter.Role.VIRTUAL_ROUTER);
        Mockito.when(router.getNicId()).thenReturn(5L);
        Mockito.when(router.getTrafficType()).thenReturn(TrafficType.Public);
        Mockito.when(router.getNicNetworkRate()).thenReturn(nicNetworkRate);
        return router;
    }

    @SuppressWarnings("unchecked")
    private static NicResponse onlyNicOf(DomainRouterResponse response) {
        final Set<NicResponse> nics = (Set<NicResponse>) ReflectionTestUtils.getField(response, "nics");
        Assert.assertEquals(1, nics.size());
        return nics.iterator().next();
    }

    private DomainRouterResponse newResponse(Integer nicNetworkRate) {
        final Account caller = Mockito.mock(Account.class);
        Mockito.when(caller.getId()).thenReturn(1L);
        Mockito.when(accountManager.isRootAdmin(1L)).thenReturn(true);
        return dao.newDomainRouterResponse(router(nicNetworkRate), caller);
    }

    @Test
    public void newDomainRouterResponseReportsNicNetworkRate() {
        Assert.assertEquals(Integer.valueOf(150), onlyNicOf(newResponse(150)).getNetworkRate());
    }

    @Test
    public void newDomainRouterResponseReportsMissingZeroAndNegativeNicRateAsUnlimited() {
        Assert.assertEquals(Integer.valueOf(-1), onlyNicOf(newResponse(null)).getNetworkRate());
        Assert.assertEquals(Integer.valueOf(-1), onlyNicOf(newResponse(0)).getNetworkRate());
        Assert.assertEquals(Integer.valueOf(-1), onlyNicOf(newResponse(-1)).getNetworkRate());
    }

    private DomainRouterResponse setResponse(Integer nicNetworkRate) {
        return dao.setDomainRouterResponse(new DomainRouterResponse(), router(nicNetworkRate));
    }

    @Test
    public void setDomainRouterResponseReportsNicNetworkRate() {
        Assert.assertEquals(Integer.valueOf(150), onlyNicOf(setResponse(150)).getNetworkRate());
    }

    @Test
    public void setDomainRouterResponseReportsMissingZeroAndNegativeNicRateAsUnlimited() {
        Assert.assertEquals(Integer.valueOf(-1), onlyNicOf(setResponse(null)).getNetworkRate());
        Assert.assertEquals(Integer.valueOf(-1), onlyNicOf(setResponse(0)).getNetworkRate());
        Assert.assertEquals(Integer.valueOf(-1), onlyNicOf(setResponse(-1)).getNetworkRate());
    }
}
