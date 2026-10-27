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
package org.apache.cloudstack.vm;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.VmwareCbtCutoverCommand;
import com.cloud.agent.api.VmwareCbtMigrationAnswer;
import com.cloud.host.HostVO;
import com.cloud.vm.VmwareCbtMigrationCycleVO;
import com.cloud.vm.VmwareCbtMigrationVO;
import com.cloud.vm.dao.VmwareCbtMigrationCycleDao;
import com.cloud.vm.dao.VmwareCbtMigrationDao;

public class VmwareCbtMigrationCutoverFailureTest {

    @Test
    public void finalCutoverAgentExceptionUsesFailureAnswerPath() throws Exception {
        VmwareCbtMigrationManagerImpl manager = new VmwareCbtMigrationManagerImpl();
        VmwareCbtMigrationVO migration = Mockito.mock(VmwareCbtMigrationVO.class);
        Mockito.when(migration.getUuid()).thenReturn("migration-1");
        HostVO host = Mockito.mock(HostVO.class);
        Mockito.when(host.getId()).thenReturn(7L);
        VmwareCbtCutoverCommand command = Mockito.mock(VmwareCbtCutoverCommand.class);
        AgentManager agentManager = Mockito.mock(AgentManager.class);
        Mockito.when(agentManager.send(7L, command)).thenThrow(new IllegalStateException("agent disconnected"));
        ReflectionTestUtils.setField(manager, "agentManager", agentManager);

        VmwareCbtMigrationAnswer answer = ReflectionTestUtils.invokeMethod(manager,
                "sendFinalCutoverCommand", host, command, migration, null);

        Assert.assertNotNull(answer);
        Assert.assertFalse(answer.getResult());
        Assert.assertTrue(answer.getDetails().contains("agent disconnected"));
    }

    @Test
    public void finalCycleInsertFailureDoesNotTryToUpdateMissingCycle() {
        VmwareCbtMigrationManagerImpl manager = new VmwareCbtMigrationManagerImpl();
        VmwareCbtMigrationVO migration = Mockito.mock(VmwareCbtMigrationVO.class);
        Mockito.when(migration.getId()).thenReturn(42L);
        VmwareCbtMigrationCycleDao cycleDao = Mockito.mock(VmwareCbtMigrationCycleDao.class);
        Mockito.when(cycleDao.persist(Mockito.any(VmwareCbtMigrationCycleVO.class)))
                .thenThrow(new IllegalStateException("cycle insert failed"));
        VmwareCbtMigrationDao migrationDao = Mockito.mock(VmwareCbtMigrationDao.class);
        Mockito.when(migrationDao.updateIfNotTerminal(migration)).thenReturn(true);
        ReflectionTestUtils.setField(manager, "vmwareCbtMigrationCycleDao", cycleDao);
        ReflectionTestUtils.setField(manager, "vmwareCbtMigrationDao", migrationDao);

        Boolean result = ReflectionTestUtils.invokeMethod(manager, "runFinalDeltaSync",
                null, migration, null, 3);

        Assert.assertNotNull(result);
        Assert.assertFalse(result);
        Mockito.verify(migration).setState(VmwareCbtMigration.State.Failed);
        Mockito.verify(migration).setLastError("cycle insert failed");
        Mockito.verify(migrationDao).updateIfNotTerminal(migration);
        Mockito.verify(cycleDao, Mockito.never()).update(Mockito.anyLong(), Mockito.any());
    }
}
