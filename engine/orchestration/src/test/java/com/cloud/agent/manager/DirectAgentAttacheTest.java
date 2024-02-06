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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.resource.ServerResource;

@RunWith(MockitoJUnitRunner.class)
public class DirectAgentAttacheTest {
    @Mock
    private AgentManagerImpl _agentMgr;

    @Mock
    private ServerResource _resource;

    long _id = 0L;

    @Before
    public void setup() {
        directAgentAttache = new DirectAgentAttache(_agentMgr, _id, "myDirectAgentAttache", _resource, false);

        MockitoAnnotations.initMocks(directAgentAttache);
    }
    private DirectAgentAttache directAgentAttache;

    @Test
    public void testPingTask() throws Exception {
        DirectAgentAttache.PingTask pt = directAgentAttache.new PingTask();
        Mockito.doReturn(2).when(_agentMgr).getDirectAgentThreadCap();
        pt.runInContext();
        Mockito.verify(_resource, Mockito.times(1)).getCurrentStatus(_id);
    }
}
