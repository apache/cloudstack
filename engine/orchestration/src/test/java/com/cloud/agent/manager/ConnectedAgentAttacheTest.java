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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import org.junit.Test;

import com.cloud.utils.nio.Link;

public class ConnectedAgentAttacheTest {

    @Test
    public void testEquals() throws Exception {

        Link link = mock(Link.class);

        ConnectedAgentAttache agentAttache1 = new ConnectedAgentAttache(null, 0, null, link, false);
        ConnectedAgentAttache agentAttache2 = new ConnectedAgentAttache(null, 0, null, link, false);

        assertTrue(agentAttache1.equals(agentAttache2));
    }

    @Test
    public void testEqualsFalseNull() throws Exception {

        Link link = mock(Link.class);

        ConnectedAgentAttache agentAttache1 = new ConnectedAgentAttache(null, 0, null, link, false);

        assertFalse(agentAttache1.equals(null));
    }

    @Test
    public void testEqualsFalseDiffLink() throws Exception {

        Link link1 = mock(Link.class);
        Link link2 = mock(Link.class);

        ConnectedAgentAttache agentAttache1 = new ConnectedAgentAttache(null, 0, null, link1, false);
        ConnectedAgentAttache agentAttache2 = new ConnectedAgentAttache(null, 0, null, link2, false);

        assertFalse(agentAttache1.equals(agentAttache2));
    }

    @Test
    public void testEqualsFalseDiffId() throws Exception {

        Link link1 = mock(Link.class);

        ConnectedAgentAttache agentAttache1 = new ConnectedAgentAttache(null, 1, null, link1, false);
        ConnectedAgentAttache agentAttache2 = new ConnectedAgentAttache(null, 2, null, link1, false);

        assertFalse(agentAttache1.equals(agentAttache2));
    }

    @Test
    public void testEqualsFalseDiffClass() throws Exception {

        Link link1 = mock(Link.class);

        ConnectedAgentAttache agentAttache1 = new ConnectedAgentAttache(null, 1, null, link1, false);

        assertFalse(agentAttache1.equals("abc"));
    }
}
