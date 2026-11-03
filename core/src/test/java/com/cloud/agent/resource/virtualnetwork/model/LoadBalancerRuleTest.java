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
package com.cloud.agent.resource.virtualnetwork.model;

import org.junit.Assert;
import org.junit.Test;

public class LoadBalancerRuleTest {

    @Test
    public void testSslCertEntry() {
        String name = "name";
        String cert = "cert";
        String key = "key1";
        String chain = "chain";
        String password = "password";
        LoadBalancerRule.SslCertEntry sslCertEntry = new LoadBalancerRule.SslCertEntry(name, cert, key, chain, password);

        Assert.assertEquals(name, sslCertEntry.getName());
        Assert.assertEquals(cert, sslCertEntry.getCert());
        Assert.assertEquals(key, sslCertEntry.getKey());
        Assert.assertEquals(chain, sslCertEntry.getChain());
        Assert.assertEquals(password, sslCertEntry.getPassword());

        String name2 = "name2";
        String cert2 = "cert2";
        String key2 = "key2";
        String chain2 = "chain2";
        String password2 = "password2";

        sslCertEntry.setName(name2);
        sslCertEntry.setCert(cert2);
        sslCertEntry.setKey(key2);
        sslCertEntry.setChain(chain2);
        sslCertEntry.setPassword(password2);

        Assert.assertEquals(name2, sslCertEntry.getName());
        Assert.assertEquals(cert2, sslCertEntry.getCert());
        Assert.assertEquals(key2, sslCertEntry.getKey());
        Assert.assertEquals(chain2, sslCertEntry.getChain());
        Assert.assertEquals(password2, sslCertEntry.getPassword());

        LoadBalancerRule loadBalancerRule = new LoadBalancerRule();
        loadBalancerRule.setSslCerts(new LoadBalancerRule.SslCertEntry[]{sslCertEntry});

        Assert.assertEquals(1, loadBalancerRule.getSslCerts().length);
        Assert.assertEquals(sslCertEntry, loadBalancerRule.getSslCerts()[0]);
    }
}
