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

package com.cloud.network.lb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.agent.api.check.CheckSshAnswer;
import com.cloud.agent.manager.Commands;
import com.cloud.network.lb.dao.ElasticLbVmMapDao;
import com.cloud.vm.ReservationContext;
import com.cloud.vm.VirtualMachineProfile;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(MockitoJUnitRunner.class)
public class ElasticLoadBalancerManagerImplTest {

    @InjectMocks
    private ElasticLoadBalancerManagerImpl elasticLoadBalancerManagerImpl;

    @Test
    public void testFinalizeStartWhenCmdsAnswerIsNull() throws Exception {
        VirtualMachineProfile profileMock = mock(VirtualMachineProfile.class);
        long hostId = 1L;
        Commands cmds = mock(Commands.class);
        when(cmds.getAnswer("checkSsh")).thenReturn(null);
        ReservationContext context = mock(ReservationContext.class);

        boolean expected = false;
        boolean actual = elasticLoadBalancerManagerImpl.finalizeStart(profileMock, hostId, cmds, context);

        assertEquals(expected, actual);
    }

    @Test
    public void testFinalizeStartWhenCmdsAnswerIsNotNullButAnswerResultIsFalse() throws Exception {
        CheckSshAnswer answerMock = mock(CheckSshAnswer.class);
        when(answerMock.getResult()).thenReturn(false);
        VirtualMachineProfile profileMock = mock(VirtualMachineProfile.class);
        long hostId = 1L;
        Commands cmds = mock(Commands.class);
        when(cmds.getAnswer("checkSsh")).thenReturn(answerMock);
        ReservationContext context = mock(ReservationContext.class);

        boolean expected = false;
        boolean actual = elasticLoadBalancerManagerImpl.finalizeStart(profileMock, hostId, cmds, context);

        assertEquals(expected, actual);
    }

    @Test
    public void testFinalizeStartWhenCmdsAnswerIsNotNullAndAnswerResultIsTrue() throws Exception {
        CheckSshAnswer answerMock = mock(CheckSshAnswer.class);
        when(answerMock.getResult()).thenReturn(true);
        VirtualMachineProfile profileMock = mock(VirtualMachineProfile.class);
        long hostId = 1L;
        Commands cmds = mock(Commands.class);
        when(cmds.getAnswer("checkSsh")).thenReturn(answerMock);
        ReservationContext context = mock(ReservationContext.class);

        boolean expected = true;
        boolean actual = elasticLoadBalancerManagerImpl.finalizeStart(profileMock, hostId, cmds, context);

        assertEquals(expected, actual);
    }

    @Test
    public void testGarbageCollectUnusedElbVmsWhenVariableUnusedElbVmsIsNull() throws Exception {
        ElasticLbVmMapDao elasticLbVmMapDaoMock = mock(ElasticLbVmMapDao.class);
        when(elasticLbVmMapDaoMock.listUnusedElbVms()).thenReturn(null);
        ReflectionTestUtils.setField(elasticLoadBalancerManagerImpl, "_elbVmMapDao", elasticLbVmMapDaoMock);

        try {
            elasticLoadBalancerManagerImpl.garbageCollectUnusedElbVms();
        } catch (NullPointerException e) {
            fail();
        }
    }
}
