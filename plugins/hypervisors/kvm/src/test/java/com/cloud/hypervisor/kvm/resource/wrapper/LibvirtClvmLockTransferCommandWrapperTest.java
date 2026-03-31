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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.apache.cloudstack.storage.command.ClvmLockTransferCommand;
import org.apache.logging.log4j.Logger;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.agent.api.Answer;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.utils.script.Script;

/**
 * Tests for LibvirtClvmLockTransferCommandWrapper
 */
@RunWith(MockitoJUnitRunner.class)
public class LibvirtClvmLockTransferCommandWrapperTest {

    @Mock
    private LibvirtComputingResource libvirtComputingResource;

    @Mock
    private Logger logger;

    private LibvirtClvmLockTransferCommandWrapper wrapper;

    private static final String TEST_LV_PATH = "/dev/vg1/volume-123";
    private static final String TEST_VOLUME_UUID = "test-volume-uuid-456";

    @Before
    public void setUp() {
        wrapper = new LibvirtClvmLockTransferCommandWrapper();
        wrapper.logger = logger;
    }

    @Test
    public void testExecute_DeactivateSuccess() {
        ClvmLockTransferCommand cmd = new ClvmLockTransferCommand(
                ClvmLockTransferCommand.Operation.DEACTIVATE,
                TEST_LV_PATH,
                TEST_VOLUME_UUID
        );

        try (MockedConstruction<Script> scriptMock = Mockito.mockConstruction(Script.class,
                (mock, context) -> {
                    when(mock.execute()).thenReturn(null); // Success
                })) {

            Answer answer = wrapper.execute(cmd, libvirtComputingResource);

            assertNotNull(answer);
            assertTrue(answer.getResult());
            assertTrue(answer.getDetails().contains("deactivated"));
            assertTrue(answer.getDetails().contains(TEST_VOLUME_UUID));

            // Verify script was constructed with correct parameters
            assertEquals(1, scriptMock.constructed().size());
            Script script = scriptMock.constructed().get(0);
            verify(script).add("-an");
            verify(script).add(TEST_LV_PATH);
        }
    }

    @Test
    public void testExecute_ActivateExclusiveSuccess() {
        ClvmLockTransferCommand cmd = new ClvmLockTransferCommand(
                ClvmLockTransferCommand.Operation.ACTIVATE_EXCLUSIVE,
                TEST_LV_PATH,
                TEST_VOLUME_UUID
        );

        try (MockedConstruction<Script> scriptMock = Mockito.mockConstruction(Script.class,
                (mock, context) -> {
                    when(mock.execute()).thenReturn(null); // Success
                })) {

            Answer answer = wrapper.execute(cmd, libvirtComputingResource);

            assertNotNull(answer);
            assertTrue(answer.getResult());
            assertTrue(answer.getDetails().contains("activated exclusively"));
            assertTrue(answer.getDetails().contains(TEST_VOLUME_UUID));

            // Verify script was constructed with correct parameters
            assertEquals(1, scriptMock.constructed().size());
            Script script = scriptMock.constructed().get(0);
            verify(script).add("-aey");
            verify(script).add(TEST_LV_PATH);
        }
    }

    @Test
    public void testExecute_ActivateSharedSuccess() {
        ClvmLockTransferCommand cmd = new ClvmLockTransferCommand(
                ClvmLockTransferCommand.Operation.ACTIVATE_SHARED,
                TEST_LV_PATH,
                TEST_VOLUME_UUID
        );

        try (MockedConstruction<Script> scriptMock = Mockito.mockConstruction(Script.class,
                (mock, context) -> {
                    when(mock.execute()).thenReturn(null); // Success
                })) {

            Answer answer = wrapper.execute(cmd, libvirtComputingResource);

            assertNotNull(answer);
            assertTrue(answer.getResult());
            assertTrue(answer.getDetails().contains("activated in shared mode"));
            assertTrue(answer.getDetails().contains(TEST_VOLUME_UUID));

            // Verify script was constructed with correct parameters
            assertEquals(1, scriptMock.constructed().size());
            Script script = scriptMock.constructed().get(0);
            verify(script).add("-asy");
            verify(script).add(TEST_LV_PATH);
        }
    }

    @Test
    public void testExecute_LvchangeFailure() {
        ClvmLockTransferCommand cmd = new ClvmLockTransferCommand(
                ClvmLockTransferCommand.Operation.DEACTIVATE,
                TEST_LV_PATH,
                TEST_VOLUME_UUID
        );

        String errorMessage = "lvchange: Volume is in use";

        try (MockedConstruction<Script> scriptMock = Mockito.mockConstruction(Script.class,
                (mock, context) -> {
                    when(mock.execute()).thenReturn(errorMessage);
                })) {

            Answer answer = wrapper.execute(cmd, libvirtComputingResource);

            assertNotNull(answer);
            assertFalse(answer.getResult());
            assertTrue(answer.getDetails().contains("lvchange -an"));
            assertTrue(answer.getDetails().contains(TEST_LV_PATH));
            assertTrue(answer.getDetails().contains(errorMessage));
        }
    }

    @Test
    public void testExecute_ScriptTimeout() {
        ClvmLockTransferCommand cmd = new ClvmLockTransferCommand(
                ClvmLockTransferCommand.Operation.ACTIVATE_EXCLUSIVE,
                TEST_LV_PATH,
                TEST_VOLUME_UUID
        );

        String timeoutMessage = "Script timed out after 30000ms";

        try (MockedConstruction<Script> scriptMock = Mockito.mockConstruction(Script.class,
                (mock, context) -> {
                    when(mock.execute()).thenReturn(timeoutMessage);
                })) {

            Answer answer = wrapper.execute(cmd, libvirtComputingResource);

            assertNotNull(answer);
            assertFalse(answer.getResult());
            assertTrue(answer.getDetails().contains("failed"));
            assertTrue(answer.getDetails().contains(timeoutMessage));
        }
    }

    @Test
    public void testExecute_NullLvPath() {
        ClvmLockTransferCommand cmd = new ClvmLockTransferCommand(
                ClvmLockTransferCommand.Operation.DEACTIVATE,
                null,
                TEST_VOLUME_UUID
        );

        try (MockedConstruction<Script> scriptMock = Mockito.mockConstruction(Script.class,
                (mock, context) -> {
                    when(mock.execute()).thenReturn(null);
                })) {

            Answer answer = wrapper.execute(cmd, libvirtComputingResource);

            assertNotNull(answer);
            // Should still execute, but may fail or succeed depending on lvchange behavior
            // At minimum, it should handle null gracefully
            assertEquals(1, scriptMock.constructed().size());
        }
    }

    @Test
    public void testExecute_EmptyLvPath() {
        ClvmLockTransferCommand cmd = new ClvmLockTransferCommand(
                ClvmLockTransferCommand.Operation.DEACTIVATE,
                "",
                TEST_VOLUME_UUID
        );

        try (MockedConstruction<Script> scriptMock = Mockito.mockConstruction(Script.class,
                (mock, context) -> {
                    when(mock.execute()).thenReturn("lvchange: Please specify a logical volume path");
                })) {

            Answer answer = wrapper.execute(cmd, libvirtComputingResource);

            assertNotNull(answer);
            assertFalse(answer.getResult());
            assertTrue(answer.getDetails().contains("failed"));
        }
    }

    @Test
    public void testExecute_InvalidLvPath() {
        String invalidPath = "/invalid/path/that/does/not/exist";
        ClvmLockTransferCommand cmd = new ClvmLockTransferCommand(
                ClvmLockTransferCommand.Operation.ACTIVATE_EXCLUSIVE,
                invalidPath,
                TEST_VOLUME_UUID
        );

        String errorMessage = "Failed to find logical volume \"" + invalidPath + "\"";

        try (MockedConstruction<Script> scriptMock = Mockito.mockConstruction(Script.class,
                (mock, context) -> {
                    when(mock.execute()).thenReturn(errorMessage);
                })) {

            Answer answer = wrapper.execute(cmd, libvirtComputingResource);

            assertNotNull(answer);
            assertFalse(answer.getResult());
            assertTrue(answer.getDetails().contains("failed"));
            assertTrue(answer.getDetails().contains(errorMessage));
        }
    }

    @Test
    public void testExecute_ExceptionDuringExecution() {
        ClvmLockTransferCommand cmd = new ClvmLockTransferCommand(
                ClvmLockTransferCommand.Operation.DEACTIVATE,
                TEST_LV_PATH,
                TEST_VOLUME_UUID
        );

        RuntimeException testException = new RuntimeException("Test exception");

        try (MockedConstruction<Script> scriptMock = Mockito.mockConstruction(Script.class,
                (mock, context) -> {
                    when(mock.execute()).thenThrow(testException);
                })) {

            Answer answer = wrapper.execute(cmd, libvirtComputingResource);

            assertNotNull(answer);
            assertFalse(answer.getResult());
            assertTrue(answer.getDetails().contains("Exception"));
            assertTrue(answer.getDetails().contains("Test exception"));
        }
    }

    @Test
    public void testExecute_VerifyScriptConstruction() {
        ClvmLockTransferCommand cmd = new ClvmLockTransferCommand(
                ClvmLockTransferCommand.Operation.DEACTIVATE,
                TEST_LV_PATH,
                TEST_VOLUME_UUID
        );

        try (MockedConstruction<Script> scriptMock = Mockito.mockConstruction(Script.class,
                (mock, context) -> {
                    // Just set up the mock behavior - don't assert here as it can interfere
                    when(mock.execute()).thenReturn(null);
                })) {

            Answer answer = wrapper.execute(cmd, libvirtComputingResource);

            // Verify the answer is successful
            assertNotNull("Answer should not be null", answer);
            assertTrue("Answer should indicate success. Details: " + answer.getDetails(),
                      answer.getResult());

            // Verify that Script was constructed exactly once
            assertEquals("Script should be constructed once", 1, scriptMock.constructed().size());
        }
    }

    @Test
    public void testExecute_AllOperationsUseDifferentFlags() {
        // Test that each operation uses the correct lvchange flag

        // DEACTIVATE -> -an
        ClvmLockTransferCommand deactivateCmd = new ClvmLockTransferCommand(
                ClvmLockTransferCommand.Operation.DEACTIVATE,
                TEST_LV_PATH,
                TEST_VOLUME_UUID
        );

        try (MockedConstruction<Script> scriptMock = Mockito.mockConstruction(Script.class,
                (mock, context) -> {
                    when(mock.execute()).thenReturn(null);
                })) {

            wrapper.execute(deactivateCmd, libvirtComputingResource);
            verify(scriptMock.constructed().get(0)).add("-an");
        }

        // ACTIVATE_EXCLUSIVE -> -aey
        ClvmLockTransferCommand exclusiveCmd = new ClvmLockTransferCommand(
                ClvmLockTransferCommand.Operation.ACTIVATE_EXCLUSIVE,
                TEST_LV_PATH,
                TEST_VOLUME_UUID
        );

        try (MockedConstruction<Script> scriptMock = Mockito.mockConstruction(Script.class,
                (mock, context) -> {
                    when(mock.execute()).thenReturn(null);
                })) {

            wrapper.execute(exclusiveCmd, libvirtComputingResource);
            verify(scriptMock.constructed().get(0)).add("-aey");
        }

        // ACTIVATE_SHARED -> -asy
        ClvmLockTransferCommand sharedCmd = new ClvmLockTransferCommand(
                ClvmLockTransferCommand.Operation.ACTIVATE_SHARED,
                TEST_LV_PATH,
                TEST_VOLUME_UUID
        );

        try (MockedConstruction<Script> scriptMock = Mockito.mockConstruction(Script.class,
                (mock, context) -> {
                    when(mock.execute()).thenReturn(null);
                })) {

            wrapper.execute(sharedCmd, libvirtComputingResource);
            verify(scriptMock.constructed().get(0)).add("-asy");
        }
    }

    @Test
    public void testExecute_LvchangeVolumeInUseError() {
        ClvmLockTransferCommand cmd = new ClvmLockTransferCommand(
                ClvmLockTransferCommand.Operation.DEACTIVATE,
                TEST_LV_PATH,
                TEST_VOLUME_UUID
        );

        String errorMessage = "Can't deactivate volume group with active logical volumes";

        try (MockedConstruction<Script> scriptMock = Mockito.mockConstruction(Script.class,
                (mock, context) -> {
                    when(mock.execute()).thenReturn(errorMessage);
                })) {

            Answer answer = wrapper.execute(cmd, libvirtComputingResource);

            assertNotNull(answer);
            assertFalse(answer.getResult());
            assertTrue(answer.getDetails().contains(errorMessage));
        }
    }

    @Test
    public void testExecute_LvchangePermissionDenied() {
        ClvmLockTransferCommand cmd = new ClvmLockTransferCommand(
                ClvmLockTransferCommand.Operation.ACTIVATE_EXCLUSIVE,
                TEST_LV_PATH,
                TEST_VOLUME_UUID
        );

        String errorMessage = "Permission denied";

        try (MockedConstruction<Script> scriptMock = Mockito.mockConstruction(Script.class,
                (mock, context) -> {
                    when(mock.execute()).thenReturn(errorMessage);
                })) {

            Answer answer = wrapper.execute(cmd, libvirtComputingResource);

            assertNotNull(answer);
            assertFalse(answer.getResult());
            assertTrue(answer.getDetails().contains(errorMessage));
        }
    }

    @Test
    public void testExecute_ComplexLvPath() {
        String complexPath = "/dev/cloudstack-vg-primary/volume-123-456-789-abc-def";
        ClvmLockTransferCommand cmd = new ClvmLockTransferCommand(
                ClvmLockTransferCommand.Operation.ACTIVATE_EXCLUSIVE,
                complexPath,
                TEST_VOLUME_UUID
        );

        try (MockedConstruction<Script> scriptMock = Mockito.mockConstruction(Script.class,
                (mock, context) -> {
                    when(mock.execute()).thenReturn(null);
                })) {

            Answer answer = wrapper.execute(cmd, libvirtComputingResource);

            assertNotNull(answer);
            assertTrue(answer.getResult());

            // Verify the complex path was passed correctly
            Script script = scriptMock.constructed().get(0);
            verify(script).add(complexPath);
        }
    }

    @Test
    public void testExecute_SequentialOperations() {
        // Test that multiple operations can be executed sequentially
        String[] paths = {
            "/dev/vg1/vol1",
            "/dev/vg1/vol2",
            "/dev/vg2/vol3"
        };

        for (String path : paths) {
            ClvmLockTransferCommand cmd = new ClvmLockTransferCommand(
                    ClvmLockTransferCommand.Operation.DEACTIVATE,
                    path,
                    "uuid-" + path.hashCode()
            );

            try (MockedConstruction<Script> scriptMock = Mockito.mockConstruction(Script.class,
                    (mock, context) -> {
                        when(mock.execute()).thenReturn(null);
                    })) {

                Answer answer = wrapper.execute(cmd, libvirtComputingResource);

                assertNotNull(answer);
                assertTrue(answer.getResult());
            }
        }
    }
}

