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
package org.apache.cloudstack.storage.datastore.util;

import java.util.Collections;

import com.linbit.linstor.api.ApiClient;
import com.linbit.linstor.api.DevelopersApi;
import org.junit.Before;
import org.junit.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Check pagination parameter names using the real SDK, without contacting a controller.
 * Helper mocks alone cannot detect a mistaken interpretation of the SDK argument order.
 */
public class LinstorApiPaginationContractTest {
    private ApiClient client;
    private DevelopersApi api;

    @Before
    public void setup() {
        client = mock(ApiClient.class);
        when(client.parameterToPairs(anyString(), anyString(), any())).thenReturn(Collections.emptyList());
        api = new DevelopersApi(client);
    }

    private void verifyPaginationQueryParameters() {
        verify(client).parameterToPairs("", "offset", 0);
        verify(client).parameterToPairs("", "offset", 2000);
        verify(client, times(2)).parameterToPairs("", "limit", 1000);
    }

    @Test
    public void resourceDefinitionListUsesOffsetThenLimit() throws Exception {
        api.resourceDefinitionList(Collections.emptyList(), true, null, 0, 1000);
        api.resourceDefinitionList(Collections.emptyList(), true, null, 2000, 1000);
        verifyPaginationQueryParameters();
    }

    @Test
    public void viewResourcesUsesOffsetThenLimit() throws Exception {
        api.viewResources(Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), null, 0, 1000);
        api.viewResources(Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), null, 2000, 1000);
        verifyPaginationQueryParameters();
    }

    @Test
    public void nodeListUsesOffsetThenLimit() throws Exception {
        api.nodeList(Collections.emptyList(), Collections.emptyList(), 0, 1000);
        api.nodeList(Collections.emptyList(), Collections.emptyList(), 2000, 1000);
        verifyPaginationQueryParameters();
    }
}
