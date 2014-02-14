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
package com.cloud.consoleproxy;

import junit.framework.Assert;

import org.junit.Test;

public class ConsoleProxyRdpClientTest {

    @Test
    public void testMapMouseDownModifierButton1Mask() throws Exception {
        int code = 0;
        int modifiers = 960;
        int expected = 1024 + 960;

        ConsoleProxyRdpClient rdpc = new ConsoleProxyRdpClient();
        int actual = rdpc.mapMouseDownModifier(code, modifiers);

        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testMapMouseDownModifierButton2() throws Exception {
        int code = 1;
        int modifiers = 0xffff;
        int expected = 960;

        ConsoleProxyRdpClient rdpc = new ConsoleProxyRdpClient();
        int actual = rdpc.mapMouseDownModifier(code, modifiers);

        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testMapMouseDownModifierButton3Mask() throws Exception {
        int code = 2;
        int modifiers = 960;
        int expected = 4096 + 960;

        ConsoleProxyRdpClient rdpc = new ConsoleProxyRdpClient();
        int actual = rdpc.mapMouseDownModifier(code, modifiers);

        Assert.assertEquals(expected, actual);
    }

}
