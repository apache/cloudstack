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
package com.cloud.agent.manager;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import com.cloud.agent.api.CancelCommand;
import com.cloud.agent.api.Command;
import com.cloud.agent.transport.Request;
import com.cloud.agent.transport.Response;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.resource.ServerResource;

public class ClusteredDirectAgentAttacheTest {

    @Test
    public void testACancelForwardedByAPeerIsAnsweredByTheAttacheNotTheResource() throws Exception {
        final ClusteredAgentManagerImpl agentMgr = Mockito.mock(ClusteredAgentManagerImpl.class);
        final ServerResource resource = Mockito.mock(ServerResource.class);
        final ClusteredDirectAgentAttache attache = new ClusteredDirectAgentAttache(agentMgr, 1L, "uuid", "esxi-1", Hypervisor.HypervisorType.VMware, 1L, resource, false);

        // peer 2 asks whether sequence 11, which this server is not running, can be cancelled
        final Request request = new Request(1L, 2L, new Command[] {new CancelCommand(11L, "Job cancelled", true)}, true, true);
        request.setSequence(5L);
        attache.routeToAgent(request.getBytes());

        Mockito.verify(resource, Mockito.never()).executeRequest(Mockito.any());
        final ArgumentCaptor<byte[]> routed = ArgumentCaptor.forClass(byte[].class);
        Mockito.verify(agentMgr).routeToPeer(Mockito.eq("2"), routed.capture());
        final Response response = (Response) Request.parse(routed.getValue());
        Assert.assertEquals(5L, response.getSequence());
        Assert.assertFalse(response.getAnswers()[0].getResult());
        Assert.assertTrue(response.getAnswers()[0].getDetails(), response.getAnswers()[0].getDetails().contains("no longer running on this host"));
    }
}
