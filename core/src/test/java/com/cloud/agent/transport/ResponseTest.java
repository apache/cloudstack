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

package com.cloud.agent.transport;

import junit.framework.TestCase;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.junit.Assert;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.CheckS2SVpnConnectionsAnswer;

import com.cloud.agent.transport.Request.Version;

public class ResponseTest extends TestCase {
    protected Logger logger = LogManager.getLogger(getClass());

    public void testCheckS2SVpnConnectionsAnswer() {
        logger.info("Testing CheckS2SVpnConnectionsAnswer");
        String content = "[{\"com.cloud.agent.api.CheckS2SVpnConnectionsAnswer\":{\"ipToConnected\":{\"10.0.53.13\":true}," +
                "\"ipToDetail\":{\"10.0.53.13\":\"IPsec SA found;Site-to-site VPN have connected\"}," +
                "\"details\":\"10.0.53.13:0:IPsec SA found;Site-to-site VPN have connected\\u0026\\n\"," +
                "\"result\":true,\"contextMap\":{},\"wait\":0,\"bypassHostMaintenance\":false}}]";
        Response response = new Response(Version.v2, 1L, 2L, 3L, 1L, (short)1, content);
        Answer answer = response.getAnswer();
        Assert.assertTrue(answer instanceof CheckS2SVpnConnectionsAnswer);
    }

}
