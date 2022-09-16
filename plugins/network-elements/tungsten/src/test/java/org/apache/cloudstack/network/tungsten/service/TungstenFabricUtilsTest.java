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
package org.apache.cloudstack.network.tungsten.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.cloud.agent.AgentManager;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.network.dao.TungstenProviderDao;
import com.cloud.network.element.TungstenProviderVO;
import org.apache.cloudstack.network.tungsten.agent.api.TungstenAnswer;
import org.apache.cloudstack.network.tungsten.agent.api.TungstenCommand;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class TungstenFabricUtilsTest {
    @Mock
    AgentManager agentMgr;
    @Mock
    TungstenProviderDao tungstenProviderDao;

    TungstenFabricUtils tungstenFabricUtils;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        tungstenFabricUtils = new TungstenFabricUtils();
        tungstenFabricUtils.agentMgr = agentMgr;
        tungstenFabricUtils.tungstenProviderDao = tungstenProviderDao;
    }

    @Test
    public void sendTungstenCommandSuccessTest() {
        TungstenProviderVO tungstenProviderVO = mock(TungstenProviderVO.class);
        TungstenAnswer tungstenAnswer = mock(TungstenAnswer.class);
        TungstenCommand tungstenCommand = mock(TungstenCommand.class);

        when(tungstenProviderDao.findByZoneId(anyLong())).thenReturn(tungstenProviderVO);
        when(agentMgr.easySend(anyLong(), any(TungstenCommand.class))).thenReturn(tungstenAnswer);
        when(tungstenAnswer.getResult()).thenReturn(true);

        assertEquals(tungstenAnswer, tungstenFabricUtils.sendTungstenCommand(tungstenCommand, anyLong()));
    }

    @Test(expected = InvalidParameterValueException.class)
    public void sendTungstenCommandWithNullTungstenProvider() {
        TungstenCommand tungstenCommand = mock(TungstenCommand.class);

        tungstenFabricUtils.sendTungstenCommand(tungstenCommand, anyLong());
    }

    @Test(expected = InvalidParameterValueException.class)
    public void sendTungstenCommandWithNullAnswer() {
        TungstenProviderVO tungstenProviderVO = mock(TungstenProviderVO.class);
        TungstenCommand tungstenCommand = mock(TungstenCommand.class);

        when(tungstenProviderDao.findByZoneId(anyLong())).thenReturn(tungstenProviderVO);

        tungstenFabricUtils.sendTungstenCommand(tungstenCommand, anyLong());
    }

    @Test(expected = InvalidParameterValueException.class)
    public void sendTungstenCommandWithFalseAnswer() {
        TungstenProviderVO tungstenProviderVO = mock(TungstenProviderVO.class);
        TungstenAnswer tungstenAnswer = mock(TungstenAnswer.class);
        TungstenCommand tungstenCommand = mock(TungstenCommand.class);

        when(tungstenProviderDao.findByZoneId(anyLong())).thenReturn(tungstenProviderVO);
        when(agentMgr.easySend(anyLong(), any(TungstenCommand.class))).thenReturn(tungstenAnswer);
        when(tungstenAnswer.getResult()).thenReturn(false);
        when(tungstenAnswer.getDetails()).thenReturn("");

        tungstenFabricUtils.sendTungstenCommand(tungstenCommand, anyLong());
    }
}
