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

import com.cloud.hypervisor.vmware.mo.HttpNfcLeaseMO;
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
        Assert.assertNotNull(registry.checkTaskNotCancellable(999L));
        Assert.assertFalse(registry.cancel(999L));
        Assert.assertFalse(registry.wasCancelRequested(999L));
    }

    @Test
    public void requestWithNoTaskInFlightIsRefused() throws Exception {
        registry.beginRequest(SEQ, "StopCommand");

        final String reason = registry.checkTaskNotCancellable(SEQ);
        Assert.assertTrue(reason, reason.contains("is running for StopCommand"));
        Assert.assertFalse(registry.cancel(SEQ));
        Assert.assertFalse(registry.wasCancelRequested(SEQ));
        Mockito.verify(client, Mockito.never()).cancelTask(Mockito.any());
    }

    @Test
    public void taskStartedWhileTheCancelRunsIsReportedButTheUndoAfterwardsIsNot() throws Exception {
        final ManagedObjectReference first = mor("task-first");
        final AtomicBoolean racing = new AtomicBoolean();
        Mockito.when(client.cancelTask(first)).thenAnswer(invocation -> {
            racing.set(VmwareTaskRegistry.taskStarted(mor("task-racing"), client));
            return new Pair<>(true, "cancelled");
        });
        registry.beginRequest(SEQ, "CreateVMSnapshotCommand");
        VmwareTaskRegistry.taskStarted(first, client);

        Assert.assertTrue(registry.cancel(SEQ));

        Assert.assertTrue(racing.get());
        // the consolidation after a cancelled snapshot must run
        Assert.assertFalse(VmwareTaskRegistry.taskStarted(mor("task-undo"), client));
        Assert.assertTrue(registry.wasCancelRequested(SEQ));
    }

    @Test
    public void refusedCancelLeavesTheRequestUnmarked() throws Exception {
        final ManagedObjectReference mor = mor("task-done");
        Mockito.when(client.cancelTask(mor)).thenReturn(new Pair<>(false, "completed first"));
        registry.beginRequest(SEQ, "MigrateCommand");
        VmwareTaskRegistry.taskStarted(mor, client);

        Assert.assertFalse(registry.cancel(SEQ));

        Assert.assertFalse(registry.wasCancelRequested(SEQ));
        Assert.assertFalse(VmwareTaskRegistry.taskStarted(mor("task-next"), client));
    }

    @Test
    public void cancellableTaskIsCancelledThroughItsClient() throws Exception {
        final ManagedObjectReference mor = mor("task-1");
        Mockito.when(client.cancelTask(mor)).thenReturn(new Pair<>(true, "cancelled"));

        registry.beginRequest(SEQ, "MigrateCommand");
        Assert.assertFalse(VmwareTaskRegistry.taskStarted(mor, client));

        Assert.assertNull(registry.checkTaskNotCancellable(SEQ));
        Assert.assertTrue(registry.cancel(SEQ));
        Mockito.verify(client).cancelTask(mor);
    }

    @Test
    public void nonCancellableTaskRefusesAndIsLeftAlone() throws Exception {
        final ManagedObjectReference mor = mor("task-2");
        Mockito.when(client.checkTaskIsNotCancellable(mor)).thenReturn("vCenter reports task PowerOffVM_Task(task-2) of i-2-5-VM as not cancelable");

        registry.beginRequest(SEQ, "MigrateCommand");
        VmwareTaskRegistry.taskStarted(mor, client);

        Assert.assertEquals("vCenter reports task PowerOffVM_Task(task-2) of i-2-5-VM as not cancelable", registry.checkTaskNotCancellable(SEQ));
        Mockito.verify(client, Mockito.never()).cancelTask(Mockito.any());
    }

    @Test
    public void cancelReportsFailureWhenVCenterDoesNotCancel() throws Exception {
        final ManagedObjectReference mor = mor("task-3");
        Mockito.when(client.cancelTask(mor)).thenReturn(new Pair<>(false, "completed first"));

        registry.beginRequest(SEQ, "MigrateCommand");
        VmwareTaskRegistry.taskStarted(mor, client);

        Assert.assertFalse(registry.cancel(SEQ));
    }

    @Test
    public void finishedTaskIsNoLongerTracked() throws Exception {
        final ManagedObjectReference mor = mor("task-4");
        registry.beginRequest(SEQ, "MigrateCommand");
        VmwareTaskRegistry.taskStarted(mor, client);
        Assert.assertEquals(1, registry.activeTaskCount(SEQ));

        VmwareTaskRegistry.taskFinished(mor);

        Assert.assertEquals(0, registry.activeTaskCount(SEQ));
        Assert.assertFalse(registry.cancel(SEQ));
        Mockito.verify(client, Mockito.never()).cancelTask(Mockito.any());
    }

    @Test
    public void endRequestForgetsTheSequence() {
        registry.beginRequest(SEQ, "MigrateCommand");
        registry.endRequest(SEQ);

        Assert.assertNotNull(registry.checkTaskNotCancellable(SEQ));
        Assert.assertFalse(VmwareTaskRegistry.taskStarted(mor("task-5"), client));
        Assert.assertEquals(0, registry.activeTaskCount(SEQ));
    }

    @Test
    public void unscopedThreadRegistersNothing() throws Exception {
        registry.beginRequest(SEQ, "MigrateCommand");
        final AtomicBoolean registered = new AtomicBoolean(true);

        // scope is per thread
        final Thread other = new Thread(() -> registered.set(VmwareTaskRegistry.taskStarted(mor("task-6"), client)));
        other.start();
        other.join();

        Assert.assertFalse(registered.get());
        Assert.assertEquals(0, registry.activeTaskCount(SEQ));
    }

    @Test
    public void nullSequenceLeavesTheThreadUnscoped() {
        registry.beginRequest(null, "MigrateCommand");

        Assert.assertFalse(VmwareTaskRegistry.taskStarted(mor("task-7"), client));
        registry.endRequest(null);
    }

    @Test
    public void transferOverALeaseIsStoppedByAbortingTheLease() throws Exception {
        final HttpNfcLeaseMO lease = Mockito.mock(HttpNfcLeaseMO.class);
        registry.beginRequest(SEQ, "CopyCommand");
        Assert.assertFalse(VmwareTaskRegistry.leaseStarted(lease));

        Assert.assertNull(registry.checkTaskNotCancellable(SEQ));
        Assert.assertTrue(registry.cancel(SEQ));

        Mockito.verify(lease).abortLease();
        Assert.assertTrue(registry.wasCancelRequested(SEQ));
    }

    @Test
    public void leaseStartedWhileTheCancelRunsIsReportedButNotAfterwards() throws Exception {
        final HttpNfcLeaseMO lease = Mockito.mock(HttpNfcLeaseMO.class);
        final AtomicBoolean racing = new AtomicBoolean();
        Mockito.doAnswer(invocation -> {
            racing.set(VmwareTaskRegistry.leaseStarted(Mockito.mock(HttpNfcLeaseMO.class)));
            return null;
        }).when(lease).abortLease();
        registry.beginRequest(SEQ, "CopyCommand");
        VmwareTaskRegistry.leaseStarted(lease);

        Assert.assertTrue(registry.cancel(SEQ));

        Assert.assertTrue(racing.get());
        Assert.assertFalse(VmwareTaskRegistry.leaseStarted(Mockito.mock(HttpNfcLeaseMO.class)));
    }

    @Test
    public void finishedLeaseIsNoLongerTracked() throws Exception {
        final HttpNfcLeaseMO lease = Mockito.mock(HttpNfcLeaseMO.class);
        registry.beginRequest(SEQ, "CopyCommand");
        VmwareTaskRegistry.leaseStarted(lease);
        VmwareTaskRegistry.leaseFinished(lease);

        Assert.assertNotNull(registry.checkTaskNotCancellable(SEQ));
        Assert.assertFalse(registry.cancel(SEQ));
        Mockito.verify(lease, Mockito.never()).abortLease();
    }

    private static ManagedObjectReference mor(final String value) {
        final ManagedObjectReference mor = new ManagedObjectReference();
        mor.setType("Task");
        mor.setValue(value);
        return mor;
    }
}
