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
package com.cloud.agent;

import java.util.UUID;

import javax.naming.ConfigurationException;

import org.junit.Assert;
import org.junit.Test;

public class AgentShellTest {
    @Test
    public void parseCommand() throws ConfigurationException {
        AgentShell shell = new AgentShell();
        UUID anyUuid = UUID.randomUUID();
        shell.parseCommand(new String[] {"port=55555", "threads=4", "host=localhost", "pod=pod1", "guid=" + anyUuid, "zone=zone1"});
        Assert.assertEquals(55555, shell.getPort());
        Assert.assertEquals(4, shell.getWorkers());
        Assert.assertEquals("localhost", shell.getHost());
        Assert.assertEquals(anyUuid.toString(), shell.getGuid());
        Assert.assertEquals("pod1", shell.getPod());
        Assert.assertEquals("zone1", shell.getZone());
    }

    @Test
    public void loadProperties() throws ConfigurationException {
        AgentShell shell = new AgentShell();
        shell.loadProperties();
        Assert.assertNotNull(shell.getProperties());
        Assert.assertFalse(shell.getProperties().entrySet().isEmpty());
    }
}
