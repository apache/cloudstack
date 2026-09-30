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
package com.cloud.hypervisor.vmware.util;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.utils.Pair;
import com.vmware.vim25.ManagedObjectReference;

@RunWith(MockitoJUnitRunner.class)
public class VmwareTaskRegistryTest {

    private static final long SEQ = 42L;

    @Mock
    private VmwareClient client;

    private final VmwareTaskRegistry registry = new VmwareTaskRegistry();

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
    public void requestWithNoTaskInFlightIsCancellableByInterruptAlone() throws Exception {
        registry.beginRequest(SEQ);

        Assert.assertTrue(registry.isCancellable(SEQ));
        Assert.assertTrue(registry.cancel(SEQ));
        Assert.assertTrue(registry.wasCancelRequested(SEQ));
        Mockito.verify(client, Mockito.never()).cancelTask(Mockito.any());
    }

    @Test
    public void taskCreatedAfterCancelIsReportedSoTheClientCancelsItImmediately() {
        registry.beginRequest(SEQ);
        registry.cancel(SEQ);

        Assert.assertTrue(VmwareTaskRegistry.taskStarted(mor("task-late"), client));
    }

    @Test
    public void cancellableTaskIsCancelledThroughItsClient() throws Exception {
        final ManagedObjectReference mor = mor("task-1");
        Mockito.when(client.isTaskCancellable(mor)).thenReturn(true);
        Mockito.when(client.cancelTask(mor)).thenReturn(new Pair<>(true, "cancelled"));

        registry.beginRequest(SEQ);
        Assert.assertFalse(VmwareTaskRegistry.taskStarted(mor, client));

        Assert.assertTrue(registry.isCancellable(SEQ));
        Assert.assertTrue(registry.cancel(SEQ));
        Mockito.verify(client).cancelTask(mor);
    }

    @Test
    public void nonCancellableTaskRefusesAndIsLeftAlone() throws Exception {
        final ManagedObjectReference mor = mor("task-2");
        Mockito.when(client.isTaskCancellable(mor)).thenReturn(false);

        registry.beginRequest(SEQ);
        VmwareTaskRegistry.taskStarted(mor, client);

        Assert.assertFalse(registry.isCancellable(SEQ));
        Mockito.verify(client, Mockito.never()).cancelTask(Mockito.any());
    }

    @Test
    public void cancelReportsFailureWhenVCenterDoesNotCancel() throws Exception {
        final ManagedObjectReference mor = mor("task-3");
        Mockito.when(client.cancelTask(mor)).thenReturn(new Pair<>(false, "completed first"));

        registry.beginRequest(SEQ);
        VmwareTaskRegistry.taskStarted(mor, client);

        Assert.assertFalse(registry.cancel(SEQ));
    }

    @Test
    public void finishedTaskIsNoLongerTracked() throws Exception {
        final ManagedObjectReference mor = mor("task-4");
        registry.beginRequest(SEQ);
        VmwareTaskRegistry.taskStarted(mor, client);
        Assert.assertEquals(1, registry.activeTaskCount(SEQ));

        VmwareTaskRegistry.taskFinished(mor);

        Assert.assertEquals(0, registry.activeTaskCount(SEQ));
        Assert.assertTrue(registry.cancel(SEQ));
        Mockito.verify(client, Mockito.never()).cancelTask(Mockito.any());
    }

    @Test
    public void endRequestForgetsTheSequence() {
        registry.beginRequest(SEQ);
        registry.endRequest(SEQ);

        Assert.assertFalse(registry.isCancellable(SEQ));
        Assert.assertFalse(VmwareTaskRegistry.taskStarted(mor("task-5"), client));
        Assert.assertEquals(0, registry.activeTaskCount(SEQ));
    }

    @Test
    public void unscopedThreadRegistersNothing() throws Exception {
        registry.beginRequest(SEQ);
        final AtomicBoolean registered = new AtomicBoolean(true);

        // the scope is per thread: a task waited on from another thread belongs to no request
        final Thread other = new Thread(() -> registered.set(VmwareTaskRegistry.taskStarted(mor("task-6"), client)));
        other.start();
        other.join();

        Assert.assertFalse(registered.get());
        Assert.assertEquals(0, registry.activeTaskCount(SEQ));
    }

    @Test
    public void nullSequenceLeavesTheThreadUnscoped() {
        registry.beginRequest(null);

        Assert.assertFalse(VmwareTaskRegistry.taskStarted(mor("task-7"), client));
        registry.endRequest(null);
    }

    private static ManagedObjectReference mor(final String value) {
        final ManagedObjectReference mor = new ManagedObjectReference();
        mor.setType("Task");
        mor.setValue(value);
        return mor;
    }
}
