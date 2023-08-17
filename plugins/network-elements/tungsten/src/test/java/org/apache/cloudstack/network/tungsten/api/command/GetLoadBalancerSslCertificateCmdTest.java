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
package org.apache.cloudstack.network.tungsten.api.command;

import com.cloud.network.lb.LoadBalancingRule;
import com.cloud.network.lb.LoadBalancingRulesManager;
import org.apache.cloudstack.network.tungsten.api.response.TlsDataResponse;
import org.apache.commons.codec.binary.Base64;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(MockitoJUnitRunner.class)
public class GetLoadBalancerSslCertificateCmdTest {

    @Mock
    LoadBalancingRulesManager loadBalancingRulesManager;

    GetLoadBalancerSslCertificateCmd getLoadBalancerSslCertificateCmd;

    AutoCloseable closeable;

    @Before
    public void setup() {
        closeable = MockitoAnnotations.openMocks(this);
        getLoadBalancerSslCertificateCmd = new GetLoadBalancerSslCertificateCmd();
        ReflectionTestUtils.setField(getLoadBalancerSslCertificateCmd, "lbMgr", loadBalancingRulesManager);
        ReflectionTestUtils.setField(getLoadBalancerSslCertificateCmd, "id", 1L);
    }

    @After
    public void tearDown() throws Exception {
        closeable.close();
    }

    @Test
    public void executeTest() throws Exception {
        LoadBalancingRule.LbSslCert lbSslCert = Mockito.mock(LoadBalancingRule.LbSslCert.class);
        Mockito.when(lbSslCert.getCert()).thenReturn("testCrt");
        Mockito.when(lbSslCert.getKey()).thenReturn("testKey");
        Mockito.when(lbSslCert.getChain()).thenReturn("testChain");
        Mockito.when(loadBalancingRulesManager.getLbSslCert(ArgumentMatchers.anyLong())).thenReturn(lbSslCert);
        getLoadBalancerSslCertificateCmd.execute();
        TlsDataResponse response = (TlsDataResponse) getLoadBalancerSslCertificateCmd.getResponseObject();

        Assert.assertEquals(Base64.encodeBase64String("testCrt".getBytes()), response.getCrt());
        Assert.assertEquals(Base64.encodeBase64String("testChain".getBytes()), response.getChain());
        Assert.assertEquals(Base64.encodeBase64String("testKey".getBytes()), response.getKey());

    }
}
