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

package com.cloud.hypervisor.kvm.resource.wrapper;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.CheckCpuCompatibilityCommand;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.utils.script.Script;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;

@RunWith(MockitoJUnitRunner.class)
public class LibvirtCheckCpuCompatibilityCommandWrapperTest {

    private LibvirtCheckCpuCompatibilityCommandWrapper wrapper;
    private LibvirtComputingResource libvirtComputingResource;

    @Before
    public void setUp() {
        wrapper = new LibvirtCheckCpuCompatibilityCommandWrapper();
        libvirtComputingResource = Mockito.mock(LibvirtComputingResource.class);
    }

    // core verdict logic tests (fail-before/pass-after against isCompatible).
    @Test
    public void testIsCompatibleWithIncompatibleOutput() {
        Assert.assertFalse(LibvirtCheckCpuCompatibilityCommandWrapper.isCompatible(
                "CPU described in cpu.xml is incompatible with host CPU"));
    }

    @Test
    public void testIsCompatibleWithNotASupersetOutput() {
        Assert.assertFalse(LibvirtCheckCpuCompatibilityCommandWrapper.isCompatible(
                "Host CPU is not a superset of CPU described in cpu.xml"));
    }

    @Test
    public void testIsCompatibleIsCaseInsensitive() {
        Assert.assertFalse(LibvirtCheckCpuCompatibilityCommandWrapper.isCompatible("INCOMPATIBLE"));
        Assert.assertFalse(LibvirtCheckCpuCompatibilityCommandWrapper.isCompatible("NOT A SUPERSET"));
    }

    @Test
    public void testIsCompatibleWithIdenticalOutput() {
        Assert.assertTrue(LibvirtCheckCpuCompatibilityCommandWrapper.isCompatible(
                "Host CPU is identical to CPU described in cpu.xml"));
    }

    @Test
    public void testIsCompatibleWithSupersetOutput() {
        Assert.assertTrue(LibvirtCheckCpuCompatibilityCommandWrapper.isCompatible(
                "Host CPU is a superset of CPU described in cpu.xml"));
    }

    @Test
    public void testIsCompatibleFailsOpenOnBlankOutput() {
        // blank/null output must fail open (compatible=true).
        Assert.assertTrue(LibvirtCheckCpuCompatibilityCommandWrapper.isCompatible(null));
        Assert.assertTrue(LibvirtCheckCpuCompatibilityCommandWrapper.isCompatible(""));
        Assert.assertTrue(LibvirtCheckCpuCompatibilityCommandWrapper.isCompatible("   "));
    }

    // execute-path tests mocking Script + Files static calls.
    @Test
    public void testExecuteReturnsIncompatibleAnswer() {
        CheckCpuCompatibilityCommand command = new CheckCpuCompatibilityCommand("test-vm", "<cpu mode='custom'/>");

        try (MockedStatic<Files> filesMock = mockStatic(Files.class);
             MockedStatic<Script> scriptMock = mockStatic(Script.class)) {
            Path tempPath = Mockito.mock(Path.class);
            Mockito.when(tempPath.toString()).thenReturn("/tmp/cloudstack-cpu-compare-abc.xml");
            filesMock.when(() -> Files.createTempFile(anyString(), anyString())).thenReturn(tempPath);
            filesMock.when(() -> Files.write(any(Path.class), any(byte[].class))).thenReturn(tempPath);
            filesMock.when(() -> Files.deleteIfExists(any(Path.class))).thenReturn(true);
            scriptMock.when(() -> Script.runSimpleBashScriptWithFullResult(anyString(), anyInt()))
                    .thenReturn("CPU described in cpu.xml is incompatible with host CPU");

            Answer answer = wrapper.execute(command, libvirtComputingResource);

            Assert.assertNotNull(answer);
            Assert.assertFalse(answer.getResult());
            Assert.assertEquals("CPU described in cpu.xml is incompatible with host CPU", answer.getDetails());
        }
    }

    @Test
    public void testExecuteReturnsCompatibleAnswer() {
        CheckCpuCompatibilityCommand command = new CheckCpuCompatibilityCommand("test-vm", "<cpu mode='custom'/>");

        try (MockedStatic<Files> filesMock = mockStatic(Files.class);
             MockedStatic<Script> scriptMock = mockStatic(Script.class)) {
            Path tempPath = Mockito.mock(Path.class);
            Mockito.when(tempPath.toString()).thenReturn("/tmp/cloudstack-cpu-compare-abc.xml");
            filesMock.when(() -> Files.createTempFile(anyString(), anyString())).thenReturn(tempPath);
            filesMock.when(() -> Files.write(any(Path.class), any(byte[].class))).thenReturn(tempPath);
            filesMock.when(() -> Files.deleteIfExists(any(Path.class))).thenReturn(true);
            scriptMock.when(() -> Script.runSimpleBashScriptWithFullResult(anyString(), anyInt()))
                    .thenReturn("Host CPU is a superset of CPU described in cpu.xml");

            Answer answer = wrapper.execute(command, libvirtComputingResource);

            Assert.assertNotNull(answer);
            Assert.assertTrue(answer.getResult());
        }
    }

    @Test
    public void testExecuteFailsOpenOnBlankVirshOutput() {
        CheckCpuCompatibilityCommand command = new CheckCpuCompatibilityCommand("test-vm", "<cpu mode='custom'/>");

        try (MockedStatic<Files> filesMock = mockStatic(Files.class);
             MockedStatic<Script> scriptMock = mockStatic(Script.class)) {
            Path tempPath = Mockito.mock(Path.class);
            Mockito.when(tempPath.toString()).thenReturn("/tmp/cloudstack-cpu-compare-abc.xml");
            filesMock.when(() -> Files.createTempFile(anyString(), anyString())).thenReturn(tempPath);
            filesMock.when(() -> Files.write(any(Path.class), any(byte[].class))).thenReturn(tempPath);
            filesMock.when(() -> Files.deleteIfExists(any(Path.class))).thenReturn(true);
            // Script.runSimpleBashScript returns null when virsh emits nothing.
            scriptMock.when(() -> Script.runSimpleBashScriptWithFullResult(anyString(), anyInt())).thenReturn(null);

            Answer answer = wrapper.execute(command, libvirtComputingResource);

            Assert.assertNotNull(answer);
            Assert.assertTrue(answer.getResult());
        }
    }

    @Test
    public void testExecuteForcesZeroExitSoIncompatibleVerdictIsNotLost() {
        // Regression: virsh cpu-compare exits non-zero when the CPUs are incompatible, and
        // Script.runSimpleBashScript drops the output on a non-zero exit. The command must capture stderr
        // (2>&1) and force a zero exit (|| true) so the "incompatible" verdict survives instead of failing open.
        CheckCpuCompatibilityCommand command = new CheckCpuCompatibilityCommand("test-vm", "<cpu mode='custom'/>");

        try (MockedStatic<Files> filesMock = mockStatic(Files.class);
             MockedStatic<Script> scriptMock = mockStatic(Script.class)) {
            Path tempPath = Mockito.mock(Path.class);
            Mockito.when(tempPath.toString()).thenReturn("/tmp/cloudstack-cpu-compare-abc.xml");
            filesMock.when(() -> Files.createTempFile(anyString(), anyString())).thenReturn(tempPath);
            filesMock.when(() -> Files.write(any(Path.class), any(byte[].class))).thenReturn(tempPath);
            filesMock.when(() -> Files.deleteIfExists(any(Path.class))).thenReturn(true);
            ArgumentCaptor<String> cmd = ArgumentCaptor.forClass(String.class);
            scriptMock.when(() -> Script.runSimpleBashScriptWithFullResult(cmd.capture(), anyInt())).thenReturn("");

            wrapper.execute(command, libvirtComputingResource);

            Assert.assertTrue("must capture stderr", cmd.getValue().contains("2>&1"));
            Assert.assertTrue("must force a zero exit so a non-zero (incompatible) result is not discarded",
                    cmd.getValue().contains("|| true"));
        }
    }
}
