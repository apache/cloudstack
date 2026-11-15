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
package org.apache.cloudstack.discovery;

import static org.mockito.ArgumentMatchers.any;

import java.lang.reflect.Field;
import java.util.Set;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.command.admin.account.CreateAccountCmd;
import org.apache.cloudstack.api.command.admin.user.GetUserCmd;
import org.apache.cloudstack.api.command.user.discovery.ListApisCmd;
import org.apache.cloudstack.api.response.ApiDiscoveryResponse;
import org.apache.cloudstack.api.response.ApiParameterResponse;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import com.cloud.utils.ReflectUtil;

@RunWith(MockitoJUnitRunner.class)
public class ApiDiscoveryServiceImplTest {

    @Mock
    APICommand apiCommandMock;

    @Spy
    @InjectMocks
    ApiDiscoveryServiceImpl discoveryServiceSpy;

    @Before
    public void setUp() {
        Mockito.when(apiCommandMock.name()).thenReturn("listApis");
        Mockito.when(apiCommandMock.since()).thenReturn("");
    }

    @Test
    public void getCmdRequestMapReturnsResponseWithCorrectApiNameAndDescription() {
        Mockito.when(apiCommandMock.description()).thenReturn("Lists all APIs");
        ApiDiscoveryResponse response = discoveryServiceSpy.getCmdRequestMap(ListApisCmd.class, apiCommandMock);
        Assert.assertEquals("listApis", response.getName());
        Assert.assertEquals("Lists all APIs", response.getDescription());
    }

    @Test
    public void getCmdRequestMapSetsHttpRequestTypeToGetWhenApiNameMatchesGetPattern() {
        Mockito.when(apiCommandMock.name()).thenReturn("getUser");
        Mockito.when(apiCommandMock.httpMethod()).thenReturn("");
        ApiDiscoveryResponse response = discoveryServiceSpy.getCmdRequestMap(GetUserCmd.class, apiCommandMock);
        Assert.assertEquals("GET", response.getHttpRequestType());
    }

    @Test
    public void getCmdRequestMapSetsHttpRequestTypeToPostWhenApiNameDoesNotMatchGetPattern() {
        Mockito.when(apiCommandMock.name()).thenReturn("createAccount");
        Mockito.when(apiCommandMock.httpMethod()).thenReturn("");
        ApiDiscoveryResponse response = discoveryServiceSpy.getCmdRequestMap(CreateAccountCmd.class, apiCommandMock);
        Assert.assertEquals("POST", response.getHttpRequestType());
    }

    @Test
    public void getCmdRequestMapSetsAsyncToTrueForAsyncCommand() {
        Mockito.when(apiCommandMock.name()).thenReturn("asyncApi");
        ApiDiscoveryResponse response = discoveryServiceSpy.getCmdRequestMap(BaseAsyncCmd.class, apiCommandMock);
        Assert.assertTrue(response.getAsync());
    }

    @Test
    public void getCmdRequestMapDoesNotAddParamsWithoutParameterAnnotation() {
        ApiDiscoveryResponse response = discoveryServiceSpy.getCmdRequestMap(BaseCmd.class, apiCommandMock);
        Assert.assertFalse(response.getParams().isEmpty());
        Assert.assertEquals(1, response.getParams().size());
    }

    @Test
    public void getCmdRequestMapAddsParamsWithExposedAndIncludedInApiDocAnnotations() {
        Field fieldMock = Mockito.mock(Field.class);
        Parameter parameterMock = Mockito.mock(Parameter.class);
        Mockito.when(parameterMock.expose()).thenReturn(true);
        Mockito.when(parameterMock.includeInApiDoc()).thenReturn(true);
        Mockito.when(parameterMock.name()).thenReturn("paramName");
        Mockito.when(parameterMock.since()).thenReturn("");
        Mockito.when(parameterMock.entityType()).thenReturn(new Class[]{Object.class});
        Mockito.when(parameterMock.description()).thenReturn("paramDescription");
        Mockito.when(parameterMock.type()).thenReturn(BaseCmd.CommandType.STRING);
        Mockito.when(fieldMock.getAnnotation(Parameter.class)).thenReturn(parameterMock);
        try (MockedStatic<ReflectUtil> reflectUtilMockedStatic = Mockito.mockStatic(ReflectUtil.class)) {
            reflectUtilMockedStatic.when(() -> ReflectUtil.getAllFieldsForClass(any(Class.class), any(Class[].class)))
                    .thenReturn(Set.of(fieldMock));
            ApiDiscoveryResponse response = discoveryServiceSpy.getCmdRequestMap(ListApisCmd.class, apiCommandMock);
            Set<ApiParameterResponse> params = response.getParams();
            Assert.assertEquals(1, params.size());
            ApiParameterResponse paramResponse = params.iterator().next();
            Assert.assertEquals("paramName", ReflectionTestUtils.getField(paramResponse, "name"));
        }
    }
}
