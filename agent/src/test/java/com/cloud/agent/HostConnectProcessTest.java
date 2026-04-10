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

import com.cloud.exception.CloudException;
import com.cloud.utils.nio.Link;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import javax.naming.ConfigurationException;

import static org.mockito.Mockito.mock;

@RunWith(MockitoJUnitRunner.class)
public class HostConnectProcessTest {

    private Agent agent;
    private Logger logger;
    private Link link;
    private ServerAttache attache;
    private HostConnectProcess hostConnectProcess;
    private boolean connectionTransfer;

    @Before
    public void setUp() throws ConfigurationException {
        agent = mock(Agent.class);
        logger = mock(Logger.class);
        link = mock(Link.class);
        attache = mock(ServerAttache.class);
        hostConnectProcess = new HostConnectProcess(agent);
        ReflectionTestUtils.setField(agent, "logger", logger);
    }

    @Test
    public void testScheduleConnectProcess() throws InterruptedException, CloudException {

        hostConnectProcess.scheduleConnectProcess(link, connectionTransfer);
        Assert.assertTrue(hostConnectProcess.isInProgress());
    }
}
