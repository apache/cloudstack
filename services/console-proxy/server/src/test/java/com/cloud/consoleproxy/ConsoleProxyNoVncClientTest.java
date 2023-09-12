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
package com.cloud.consoleproxy;

import org.junit.Assert;
import org.junit.Test;

public class ConsoleProxyNoVncClientTest {
    @Test
    public void rewriteServerNameInServerInitTest() {
        String serverName = "server123, backend:TLS";
        byte[] serverInitTestBytes = new byte[]{ 4, 0, 3, 0, 32, 24, 0, 1, 0, -1, 0, -1, 0, -1, 16, 8, 0, 0, 0, 0, 0, 0, 0, 15, 81, 69, 77, 85, 32, 40, 105, 45, 50, 45, 56, 45, 86, 77, 41};
        byte[] newServerInit = ConsoleProxyNoVncClient.rewriteServerNameInServerInit(serverInitTestBytes, serverName);

        byte[] expectedBytes = new byte[]{4, 0, 3, 0, 32, 24, 0, 1, 0, -1, 0, -1, 0, -1, 16, 8, 0, 0, 0, 0, 0, 0, 0, 22, 115, 101, 114, 118, 101, 114, 49, 50, 51, 44, 32, 98, 97, 99, 107, 101, 110, 100, 58, 84, 76, 83};
        Assert.assertArrayEquals(newServerInit, expectedBytes);
    }
}
