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

import com.cloud.user.User;
import com.cloud.user.UserVO;

import java.util.*;
import javax.naming.ConfigurationException;

import com.cloud.utils.component.PluggableService;
import org.apache.cloudstack.acl.APIChecker;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.command.user.discovery.ListApisCmd;
import org.apache.cloudstack.api.response.ApiDiscoveryResponse;
import org.apache.cloudstack.api.response.ListResponse;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class ApiDiscoveryTest {
    private static APIChecker _apiChecker = mock(APIChecker.class);
    private static PluggableService _pluggableService = mock(PluggableService.class);
    private static ApiDiscoveryServiceImpl _discoveryService = new ApiDiscoveryServiceImpl();

    private static Class<?> testCmdClass = ListApisCmd.class;
    private static User testUser;
    private static String testApiName;
    private static String testApiDescription;
    private static String testApiSince;
    private static boolean testApiAsync;

    @BeforeClass
    public static void setUp() throws ConfigurationException {
        testApiName = testCmdClass.getAnnotation(APICommand.class).name();
        testApiDescription = testCmdClass.getAnnotation(APICommand.class).description();
        testApiSince = testCmdClass.getAnnotation(APICommand.class).since();
        testApiAsync = false;
        testUser = new UserVO();

        _discoveryService._apiAccessCheckers =  (List<APIChecker>) mock(List.class);
        _discoveryService._services = (List<PluggableService>) mock(List.class);

        when(_apiChecker.checkAccess(any(User.class), anyString())).thenReturn(true);
        when(_pluggableService.getCommands()).thenReturn(new ArrayList<Class<?>>());
        when(_discoveryService._apiAccessCheckers.iterator()).thenReturn(Arrays.asList(_apiChecker).iterator());
        when(_discoveryService._services.iterator()).thenReturn(Arrays.asList(_pluggableService).iterator());

        Set<Class<?>> cmdClasses = new HashSet<Class<?>>();
        cmdClasses.add(ListApisCmd.class);
        _discoveryService.start();
        _discoveryService.cacheResponseMap(cmdClasses);
    }

    @Test
    public void verifyListSingleApi() throws Exception {
        ListResponse<ApiDiscoveryResponse> responses = (ListResponse<ApiDiscoveryResponse>) _discoveryService.listApis(testUser, testApiName);
        ApiDiscoveryResponse response = responses.getResponses().get(0);
        assertTrue("No. of response items should be one", responses.getCount() == 1);
        assertEquals("Error in api name", testApiName, response.getName());
        assertEquals("Error in api description", testApiDescription, response.getDescription());
        assertEquals("Error in api since", testApiSince, response.getSince());
        assertEquals("Error in api isAsync", testApiAsync, response.getAsync());
    }

    @Test
    public void verifyListApis() throws Exception {
        ListResponse<ApiDiscoveryResponse> responses = (ListResponse<ApiDiscoveryResponse>) _discoveryService.listApis(testUser, null);
        assertTrue("No. of response items > 1", responses.getCount() == 1);
        for (ApiDiscoveryResponse response: responses.getResponses()) {
            assertFalse("API name is empty", response.getName().isEmpty());
            assertFalse("API description is empty", response.getDescription().isEmpty());
        }
    }
}
