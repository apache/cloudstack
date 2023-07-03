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
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

@RunWith(PowerMockRunner.class)
@PrepareForTest(GetLoadBalancerSslCertificateCmd.class)
public class GetLoadBalancerSslCertificateCmdTest {

    @Mock
    LoadBalancingRulesManager loadBalancingRulesManager;

    GetLoadBalancerSslCertificateCmd getLoadBalancerSslCertificateCmd;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        getLoadBalancerSslCertificateCmd = new GetLoadBalancerSslCertificateCmd();
        Whitebox.setInternalState(getLoadBalancerSslCertificateCmd, "lbMgr", loadBalancingRulesManager);
        Whitebox.setInternalState(getLoadBalancerSslCertificateCmd, "id", 1L);
    }

    @Test
    public void executeTest() throws Exception {
        LoadBalancingRule.LbSslCert lbSslCert = Mockito.mock(LoadBalancingRule.LbSslCert.class);
        TlsDataResponse tlsDataResponse = Mockito.mock(TlsDataResponse.class);
        Mockito.when(lbSslCert.getCert()).thenReturn("test");
        Mockito.when(lbSslCert.getKey()).thenReturn("test");
        Mockito.when(lbSslCert.getChain()).thenReturn("test");
        Mockito.when(loadBalancingRulesManager.getLbSslCert(ArgumentMatchers.anyLong())).thenReturn(lbSslCert);
        PowerMockito.whenNew(TlsDataResponse.class).withAnyArguments().thenReturn(tlsDataResponse);
        getLoadBalancerSslCertificateCmd.execute();
        Assert.assertEquals(tlsDataResponse, getLoadBalancerSslCertificateCmd.getResponseObject());
    }
}
