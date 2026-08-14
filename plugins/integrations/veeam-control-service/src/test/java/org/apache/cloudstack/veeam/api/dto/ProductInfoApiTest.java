// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements. See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership. The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License. You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied. See the License for the
// specific language governing permissions and limitations
// under the License.
package org.apache.cloudstack.veeam.api.dto;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.apache.cloudstack.veeam.utils.Mapper;
import org.junit.Test;

public class ProductInfoApiTest {
    // ---- ProductInfo -------------------------------------------------------
    @Test
    public void productInfo_GettersSetters() {
        ProductInfo info = new ProductInfo();
        info.setInstanceId("inst-1");
        info.setName("oVirt Engine");
        Version version = new Version();
        version.setMajor("4");
        version.setMinor("23");
        info.setVersion(version);
        assertEquals("inst-1", info.getInstanceId());
        assertEquals("oVirt Engine", info.getName());
        assertNotNull(info.getVersion());
        assertEquals("4", info.getVersion().getMajor());
        assertEquals("23", info.getVersion().getMinor());
    }

    @Test
    public void productInfoJson_ContainsInstanceId() throws Exception {
        Mapper mapper = new Mapper();
        ProductInfo info = new ProductInfo();
        info.setInstanceId("inst-abc");
        info.setName("CloudStack");
        String json = mapper.toJson(info);
        assertTrue(json.contains("\"instance_id\":\"inst-abc\""));
        assertTrue(json.contains("\"name\":\"CloudStack\""));
    }

    // ---- SpecialObjects ----------------------------------------------------
    @Test
    public void specialObjects_GettersSetters() {
        SpecialObjects so = new SpecialObjects();
        Ref blank = Ref.of("/api/templates/blank", "blank");
        Ref root = Ref.of("/api/tags/root", "root");
        so.setBlankTemplate(blank);
        so.setRootTag(root);
        assertEquals("blank", so.getBlankTemplate().getId());
        assertEquals("root", so.getRootTag().getId());
    }

    // ---- ApiSummary --------------------------------------------------------
    @Test
    public void apiSummary_HostsAndVms() {
        ApiSummary summary = new ApiSummary();
        SummaryCount hosts = new SummaryCount(2, 5);
        SummaryCount vms = new SummaryCount(10, 20);
        summary.setHosts(hosts);
        summary.setVms(vms);
        assertEquals("2", summary.getHosts().getActive());
        assertEquals("5", summary.getHosts().getTotal());
        assertEquals("10", summary.getVms().getActive());
        assertEquals("20", summary.getVms().getTotal());
    }

    // ---- Api root -----------------------------------------------------------
    @Test
    public void api_GettersSetters() {
        Api api = new Api();
        api.setTime(1714465200000L);
        ProductInfo info = new ProductInfo();
        info.setName("CloudStack");
        api.setProductInfo(info);
        Ref authUser = Ref.of("/api/users/admin", "admin");
        api.setAuthenticatedUser(authUser);
        assertEquals(Long.valueOf(1714465200000L), api.getTime());
        assertEquals("CloudStack", api.getProductInfo().getName());
        assertEquals("admin", api.getAuthenticatedUser().getId());
    }
}
