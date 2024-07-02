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

package com.cloud.cluster;

import java.util.List;
import java.util.Optional;

import org.apache.commons.collections.CollectionUtils;
import org.apache.http.NameValuePair;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ClusterServiceServletImplTest {

    @InjectMocks
    ClusterServiceServletImpl clusterServiceServlet = new ClusterServiceServletImpl();

    @Test
    public void testClusterServicePduPostParameters() {
        List<NameValuePair> parameters =
                clusterServiceServlet.getClusterServicePduPostParameters(Mockito.mock(ClusterServicePdu.class));
        Assert.assertTrue(CollectionUtils.isNotEmpty(parameters));
        Optional<NameValuePair> opt = parameters.stream().filter(x -> x.getName().equals("method")).findFirst();
        Assert.assertTrue(opt.isPresent());
        NameValuePair val = opt.get();
        Assert.assertEquals(Integer.toString(RemoteMethodConstants.METHOD_DELIVER_PDU), val.getValue());
    }

    @Test
    public void testPingPostParameters() {
        String peer = "1.2.3.4";
        List<NameValuePair> parameters =
                clusterServiceServlet.getPingPostParameters(peer);
        Assert.assertTrue(CollectionUtils.isNotEmpty(parameters));
        Optional<NameValuePair> opt = parameters.stream().filter(x -> x.getName().equals("method")).findFirst();
        Assert.assertTrue(opt.isPresent());
        NameValuePair val = opt.get();
        Assert.assertEquals(Integer.toString(RemoteMethodConstants.METHOD_PING), val.getValue());
        opt = parameters.stream().filter(x -> x.getName().equals("callingPeer")).findFirst();
        Assert.assertTrue(opt.isPresent());
        val = opt.get();
        Assert.assertEquals(peer, val.getValue());
    }
}
