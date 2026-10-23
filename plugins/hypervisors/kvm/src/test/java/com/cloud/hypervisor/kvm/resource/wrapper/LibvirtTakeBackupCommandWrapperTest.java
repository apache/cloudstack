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
package com.cloud.hypervisor.kvm.resource.wrapper;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;

import org.apache.cloudstack.backup.BackupAnswer;
import org.apache.cloudstack.backup.TakeBackupCommand;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.utils.Pair;
import com.cloud.utils.script.Script;

@RunWith(MockitoJUnitRunner.class)
public class LibvirtTakeBackupCommandWrapperTest {

    private LibvirtTakeBackupCommandWrapper wrapper;
    private LibvirtComputingResource libvirtComputingResource;
    private TakeBackupCommand command;

    @Before
    public void setUp() {
        wrapper = new LibvirtTakeBackupCommandWrapper();
        libvirtComputingResource = Mockito.mock(LibvirtComputingResource.class);
        when(libvirtComputingResource.getNasBackupPath()).thenReturn("nasbackup.sh");
        command = new TakeBackupCommand("i-2-3-VM", "i-2-3-VM/2026.10.04.00.00.00");
        command.setBackupRepoType("nfs");
        command.setBackupRepoAddress("10.0.0.1:/backup");
        command.setWait(60);
    }

    @SuppressWarnings("unchecked")
    private List<String> runAndCaptureArgv() {
        try (MockedStatic<Script> scriptMock = mockStatic(Script.class)) {
            scriptMock.when(() -> Script.executePipedCommands(anyList(), anyLong())).thenReturn(new Pair<>(0, "1024"));

            BackupAnswer answer = (BackupAnswer) wrapper.execute(command, libvirtComputingResource);
            Assert.assertTrue(answer.getResult());

            ArgumentCaptor<List<String[]>> captor = ArgumentCaptor.forClass(List.class);
            scriptMock.verify(() -> Script.executePipedCommands(captor.capture(), anyLong()));
            return Arrays.asList(captor.getValue().get(0));
        }
    }

    @Test
    public void executePassesQuiesceTimeoutWhenQuiescing() {
        command.setQuiesce(true);
        command.setQuiesceTimeout(30);

        List<String> argv = runAndCaptureArgv();

        int idx = argv.indexOf("--quiesce-timeout");
        Assert.assertTrue(idx > 0);
        Assert.assertEquals("30", argv.get(idx + 1));
        Assert.assertEquals("true", argv.get(argv.indexOf("-q") + 1));
    }

    @Test
    public void executeOmitsQuiesceTimeoutWithoutQuiesce() {
        command.setQuiesce(false);
        command.setQuiesceTimeout(30);

        Assert.assertFalse(runAndCaptureArgv().contains("--quiesce-timeout"));
    }

    @Test
    public void executeOmitsQuiesceTimeoutWhenNotPositive() {
        command.setQuiesce(true);
        command.setQuiesceTimeout(0);

        Assert.assertFalse(runAndCaptureArgv().contains("--quiesce-timeout"));
    }
}
