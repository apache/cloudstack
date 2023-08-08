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

package com.cloud.vm.dao;

import com.cloud.utils.Pair;
import com.cloud.vm.VirtualMachine;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.Assert;
import org.mockito.Mock;

import static com.cloud.vm.VirtualMachine.State.Running;
import static com.cloud.vm.VirtualMachine.State.Stopped;

import static org.mockito.Mockito.when;
import com.cloud.vm.VMInstanceVO;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

/**
 * Created by sudharma_jain on 3/2/17.
 */

public class VMInstanceDaoImplTest {

    @Spy
    VMInstanceDaoImpl vmInstanceDao = new VMInstanceDaoImpl();

    @Mock
    VMInstanceVO vm;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        Long hostId = null;
        when(vm.getHostId()).thenReturn(hostId);
        when(vm.getUpdated()).thenReturn(5L);
        when(vm.getUpdateTime()).thenReturn(DateTime.now().toDate());
    }

    @Test
    public void testUpdateState() throws Exception {
        Long destHostId = null;
        Pair<Long, Long> opaqueMock = new Pair<Long, Long>(new Long(1), destHostId);
        vmInstanceDao.updateState(Stopped, VirtualMachine.Event.FollowAgentPowerOffReport, Stopped, vm , opaqueMock);
    }

    @Test
    public void testIfStateAndHostUnchanged() throws Exception {
        Assert.assertEquals(vmInstanceDao.ifStateUnchanged(Stopped, Stopped, null, null), true);
        Assert.assertEquals(vmInstanceDao.ifStateUnchanged(Stopped, Running, null, null), false);
    }

}
