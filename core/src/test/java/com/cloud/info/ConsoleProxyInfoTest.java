//
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
//

package com.cloud.info;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ConsoleProxyInfoTest {

    @Test
    public void testGetProxyImageUrlHttps() {
        ConsoleProxyInfo cpi = new ConsoleProxyInfo(true, "10.10.10.10", 443, 443 , "console.example.com");
        String url = cpi.getProxyImageUrl();
        assertEquals("https://console.example.com", url);
    }
    @Test
    public void testGetProxyImageUrlHttp() {
        ConsoleProxyInfo cpi = new ConsoleProxyInfo(false, "10.10.10.10", 80, 80 , "console.example.com");
        String url = cpi.getProxyImageUrl();
        assertEquals("//console.example.com", url);
    }
    @Test
    public void testGetProxyImageUrlWildcardHttps() {
        ConsoleProxyInfo cpi = new ConsoleProxyInfo(true, "1.2.3.4", 443, 8443 , "*.example.com");
        String url = cpi.getProxyImageUrl();
        assertEquals("https://1-2-3-4.example.com:8443", url);
    }
    @Test
    public void testGetProxyImageUrlWildcardHttp() {
        ConsoleProxyInfo cpi = new ConsoleProxyInfo(false, "1.2.3.4", 80, 8888 , "*.example.com");
        String url = cpi.getProxyImageUrl();
        assertEquals("//1-2-3-4.example.com:8888", url);
    }
    @Test
    public void testGetProxyImageUrlIpHttp() {
        ConsoleProxyInfo cpi = new ConsoleProxyInfo(false, "1.2.3.4", 80, 8888, "");
        String url = cpi.getProxyImageUrl();
        assertEquals("//1.2.3.4:8888", url);
    }
    @Test
    public void testGetProxyImageUrlIpHttps() {
        ConsoleProxyInfo cpi = new ConsoleProxyInfo(true, "1.2.3.4", 80, 8443, "");
        String url = cpi.getProxyImageUrl();
        assertEquals("https://1.2.3.4:8443", url);
    }
}
