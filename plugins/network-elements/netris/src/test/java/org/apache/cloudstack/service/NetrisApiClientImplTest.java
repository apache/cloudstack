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
package org.apache.cloudstack.service;

import io.netris.ApiException;
import io.netris.model.GetSiteBody;
import io.netris.model.VPCListing;
import io.netris.model.response.TenantResponse;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

public class NetrisApiClientImplTest {

    private static final String endpointUrl = "https://shapeblue-ctl.netris.dev";
    private static final String username = "netris";
    private static final String password = "qHHa$CZ2oJv*@!7mwoSR";

    private static final NetrisApiClientImpl client = new NetrisApiClientImpl(endpointUrl, username, password);

    @Test
    public void testNetrisAuthStatus() {
        Assert.assertTrue(client.isSessionAlive());
    }

    @Test
    public void testListSites() {
        List<GetSiteBody> sites = client.listSites();
        Assert.assertTrue(sites.size() > 0);
    }

    @Test
    public void testListVpcs() {
        List<VPCListing> vpcs = client.listVPCs();
        Assert.assertTrue(vpcs.size() > 0);
    }

    @Test
    public void testListTenants() throws ApiException {
        List<TenantResponse> tenants = client.listTenants();
        Assert.assertTrue(tenants.size() > 0);
    }
}
