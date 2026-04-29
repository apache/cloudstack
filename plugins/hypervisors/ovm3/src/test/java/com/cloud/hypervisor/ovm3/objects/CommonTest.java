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

package com.cloud.hypervisor.ovm3.objects;

import org.junit.Test;

public class CommonTest {
    ConnectionTest con = new ConnectionTest();
    Common cOm = new Common(con);
    XmlTestResultTest results = new XmlTestResultTest();
    String echo = "put";
    String remoteUrl = "http://oracle:password@ovm-2:8899";

    @Test
    public void testGetApiVersion() throws Ovm3ResourceException {
        con.setResult(results
                .simpleResponseWrap("<array><data>\n<value><int>3</int></value>\n</data></array>"));
        results.basicIntTest(cOm.getApiVersion(), 3);
    }

    @Test
    public void testSleep() throws Ovm3ResourceException {
        con.setResult(results.getNil());
        results.basicBooleanTest(cOm.sleep(1));
    }

    @Test
    public void testDispatch() throws Ovm3ResourceException {
        con.setResult(results.getString(echo));
        results.basicStringTest(cOm.dispatch(remoteUrl, "echo", echo), echo);
    }

    @Test
    public void testEcho() throws Ovm3ResourceException {
        con.setResult(results.getString(echo));
        results.basicStringTest(cOm.echo(echo), echo);
    }
}
