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

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLContextSpi;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;

import org.apache.commons.collections.CollectionUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.springframework.test.util.ReflectionTestUtils;

import com.cloud.utils.StringUtils;

@RunWith(MockitoJUnitRunner.class)
public class ClusterServiceServletContainerTest {

    private void runGetSecuredServerSocket(String ip) {
        SSLContext sslContext = Mockito.mock(SSLContext.class);
        SSLContextSpi sslContextSpi = Mockito.mock(SSLContextSpi.class);
        ReflectionTestUtils.setField(sslContext, "contextSpi", sslContextSpi);
        SSLServerSocketFactory factory = Mockito.mock(SSLServerSocketFactory.class);
        Mockito.when(sslContext.getServerSocketFactory()).thenReturn(factory);
        int port = 9090;
        final List<Boolean> socketNeedClientAuth = new ArrayList<>();
        try {
            SSLServerSocket socketMock = Mockito.mock(SSLServerSocket.class);
            if (StringUtils.isBlank(ip)) {
                Mockito.when(factory.createServerSocket(port)).thenReturn(socketMock);
            } else {
                Mockito.when(factory.createServerSocket(Mockito.anyInt(), Mockito.anyInt(),
                        Mockito.any(InetAddress.class))).thenReturn(socketMock);
            }
            Mockito.doAnswer((Answer<Void>) invocationOnMock -> {
                boolean needClientAuth = (boolean) invocationOnMock.getArguments()[0];
                socketNeedClientAuth.add(needClientAuth);
                return null;
            }).when(socketMock).setNeedClientAuth(Mockito.anyBoolean());
            SSLServerSocket socket = ClusterServiceServletContainer.getSecuredServerSocket(sslContext, ip, 9090);
            if (StringUtils.isBlank(ip)) {
                Mockito.verify(factory, Mockito.times(1)).createServerSocket(port);
            } else {
                Mockito.verify(factory, Mockito.times(1)).createServerSocket(port, 0, InetAddress.getByName(ip));
            }
            Mockito.verify(socket, Mockito.times(1)).setNeedClientAuth(Mockito.anyBoolean());
            Assert.assertTrue(CollectionUtils.isNotEmpty(socketNeedClientAuth));
            Assert.assertTrue(socketNeedClientAuth.get(0));
        } catch (IOException e) {
            Assert.fail("Exception occurred: " + e.getMessage());
        }
    }

    @Test
    public void testGetSecuredServerSocketNoIp() {
        runGetSecuredServerSocket("");
    }

    @Test
    public void testGetSecuredServerSocketIp() {
        runGetSecuredServerSocket("1.2.3.4");
    }
}
