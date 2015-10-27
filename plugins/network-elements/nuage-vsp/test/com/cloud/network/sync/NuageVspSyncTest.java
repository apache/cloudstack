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

package com.cloud.network.sync;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.network.NuageVspDeviceVO;
import com.cloud.network.dao.NuageVspDao;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class NuageVspSyncTest {
    private static final long NETWORK_ID = 42L;
    NuageVspDao nuageVspDao = mock(NuageVspDao.class);
    AgentManager agentManager = mock(AgentManager.class);
    HostDao hostDao = mock(HostDao.class);

    NuageVspSyncImpl sync;

    @Before
    public void setUp() {
        sync = new NuageVspSyncImpl();
        sync._nuageVspDao = nuageVspDao;
        sync._agentMgr = agentManager;
        sync._hostDao = hostDao;
    }

    @Test
    public void testSyncWithNuageVsp() {
        final NuageVspDeviceVO nuageVspDevice = mock(NuageVspDeviceVO.class);
        when(nuageVspDevice.getHostId()).thenReturn(NETWORK_ID);
        when(nuageVspDevice.getId()).thenReturn(NETWORK_ID);
        when(nuageVspDao.listAll()).thenReturn(Arrays.asList(new NuageVspDeviceVO[] {nuageVspDevice}));

        final HostVO host = mock(HostVO.class);
        when(host.getId()).thenReturn(NETWORK_ID);
        when(hostDao.findById(NETWORK_ID)).thenReturn(host);

        final Answer answer = mock(Answer.class);
        when(answer.getResult()).thenReturn(true);
        when(agentManager.easySend(eq(NETWORK_ID), (Command)any())).thenReturn(answer);

        sync.syncWithNuageVsp("users");
    }
}
