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
package com.cloud.agent.api.routing;

import org.apache.cloudstack.network.BgpPeerTO;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

public class SetBgpPeersAnswerTest {

    @Test
    public void testSetBgpPeersAnswer() {

        String good = "good";
        String[] results = new String[1];
        results[0] = good;

        BgpPeerTO bgpPeerTO = Mockito.mock(BgpPeerTO.class);
        List<BgpPeerTO> bgpPeerTOs = new ArrayList<>();
        bgpPeerTOs.add(bgpPeerTO);
        SetBgpPeersCommand command = new SetBgpPeersCommand(bgpPeerTOs);

        SetBgpPeersAnswer answer = new SetBgpPeersAnswer(command, true, results);

        Assert.assertNotNull(answer.getResults());
        Assert.assertEquals(1, answer.getResults().length);
        Assert.assertEquals(good, answer.getResults()[0]);
    }

    @Test
    public void testSetBgpPeersAnswer2() {
        SetBgpPeersAnswer answer = new SetBgpPeersAnswer();

        Assert.assertNull(answer.getResults());
    }
}
