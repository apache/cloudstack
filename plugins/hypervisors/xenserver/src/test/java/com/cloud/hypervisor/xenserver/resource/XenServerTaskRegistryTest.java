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
package com.cloud.hypervisor.xenserver.resource;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.utils.Pair;
import com.xensource.xenapi.Connection;
import com.xensource.xenapi.Task;
import com.xensource.xenapi.Types;

@RunWith(MockitoJUnitRunner.class)
public class XenServerTaskRegistryTest {

    private static final long SEQ = 42L;

    @Mock
    private Connection connection;

    private final XenServerTaskRegistry registry = new XenServerTaskRegistry();

    @After
    public void tearDown() {
        registry.endRequest(SEQ);
    }

    @Test
    public void unknownSequenceIsNeitherCancellableNorCancelled() {
        Assert.assertFalse(registry.isCancellable(999L));
        Assert.assertFalse(registry.cancel(999L));
        Assert.assertFalse(registry.wasCancelRequested(999L));
    }

    @Test
    public void requestWithNoTaskInFlightIsCancellableByInterruptAlone() {
        registry.beginRequest(SEQ);

        Assert.assertTrue(registry.isCancellable(SEQ));
        Assert.assertTrue(registry.cancel(SEQ));
        Assert.assertTrue(registry.wasCancelRequested(SEQ));
    }

    @Test
    public void taskCreatedAfterCancelIsReportedSoTheResourceCancelsItImmediately() throws Exception {
        registry.beginRequest(SEQ);
        registry.cancel(SEQ);

        Assert.assertTrue(XenServerTaskRegistry.taskStarted(task("OpaqueRef:late", Types.TaskStatusType.PENDING), connection));
    }

    @Test
    public void pendingTaskIsCancelledAsynchronouslyAndConfirmed() throws Exception {
        final Task task = task("OpaqueRef:1", Types.TaskStatusType.PENDING, Types.TaskStatusType.PENDING,
                Types.TaskStatusType.CANCELLING, Types.TaskStatusType.CANCELLED);

        registry.beginRequest(SEQ);
        Assert.assertFalse(XenServerTaskRegistry.taskStarted(task, connection));

        Assert.assertTrue(registry.isCancellable(SEQ));
        Assert.assertTrue(registry.cancel(SEQ));
        Mockito.verify(task).cancelAsync(connection);
        Mockito.verify(task, Mockito.never()).cancel(Mockito.any());
    }

    @Test
    public void taskNoLongerPendingRefusesAndIsLeftAlone() throws Exception {
        final Task task = task("OpaqueRef:2", Types.TaskStatusType.SUCCESS);

        registry.beginRequest(SEQ);
        XenServerTaskRegistry.taskStarted(task, connection);

        Assert.assertFalse(registry.isCancellable(SEQ));
        Assert.assertFalse(registry.cancel(SEQ));
        Mockito.verify(task, Mockito.never()).cancelAsync(Mockito.any());
    }

    @Test
    public void cancelReportsFailureWhenTheTaskCompletesFirst() throws Exception {
        final Task task = task("OpaqueRef:3", Types.TaskStatusType.PENDING, Types.TaskStatusType.PENDING, Types.TaskStatusType.SUCCESS);

        registry.beginRequest(SEQ);
        XenServerTaskRegistry.taskStarted(task, connection);

        Assert.assertFalse(registry.cancel(SEQ));
    }

    @Test
    public void cancelReportsFailureWhenXenServerDoesNotAllowIt() throws Exception {
        final Task task = task("OpaqueRef:4", Types.TaskStatusType.PENDING);
        Mockito.doThrow(new Types.OperationNotAllowed("not allowed")).when(task).cancelAsync(connection);

        final Pair<Boolean, String> result = XenServerTaskRegistry.cancelTask(task, connection);

        Assert.assertFalse(result.first());
        Assert.assertTrue(result.second(), result.second().contains("does not allow"));
    }

    @Test
    public void finishedTaskIsNoLongerTracked() throws Exception {
        final Task task = task("OpaqueRef:5", Types.TaskStatusType.PENDING);
        registry.beginRequest(SEQ);
        XenServerTaskRegistry.taskStarted(task, connection);
        Assert.assertEquals(1, registry.activeTaskCount(SEQ));

        XenServerTaskRegistry.taskFinished(task);

        Assert.assertEquals(0, registry.activeTaskCount(SEQ));
        Assert.assertTrue(registry.cancel(SEQ));
        Mockito.verify(task, Mockito.never()).cancelAsync(Mockito.any());
    }

    @Test
    public void endRequestForgetsTheSequence() throws Exception {
        registry.beginRequest(SEQ);
        registry.endRequest(SEQ);

        Assert.assertFalse(registry.isCancellable(SEQ));
        Assert.assertFalse(XenServerTaskRegistry.taskStarted(task("OpaqueRef:6", Types.TaskStatusType.PENDING), connection));
        Assert.assertEquals(0, registry.activeTaskCount(SEQ));
    }

    @Test
    public void unscopedThreadRegistersNothing() throws Exception {
        registry.beginRequest(SEQ);
        final Task task = task("OpaqueRef:7", Types.TaskStatusType.PENDING);
        final AtomicBoolean registered = new AtomicBoolean(true);

        // scope is per thread
        final Thread other = new Thread(() -> registered.set(XenServerTaskRegistry.taskStarted(task, connection)));
        other.start();
        other.join();

        Assert.assertFalse(registered.get());
        Assert.assertEquals(0, registry.activeTaskCount(SEQ));
    }

    @Test
    public void taskWithoutAReferenceIsStillTracked() throws Exception {
        final Task task = Mockito.mock(Task.class);
        Mockito.when(task.getStatus(connection)).thenReturn(Types.TaskStatusType.PENDING);

        registry.beginRequest(SEQ);
        XenServerTaskRegistry.taskStarted(task, connection);

        Assert.assertEquals(1, registry.activeTaskCount(SEQ));
        Assert.assertTrue(registry.isCancellable(SEQ));
    }

    private Task task(final String ref, final Types.TaskStatusType first, final Types.TaskStatusType... rest) throws Exception {
        final Task task = Mockito.mock(Task.class);
        Mockito.when(task.toWireString()).thenReturn(ref);
        Mockito.when(task.getStatus(connection)).thenReturn(first, rest);
        return task;
    }
}
