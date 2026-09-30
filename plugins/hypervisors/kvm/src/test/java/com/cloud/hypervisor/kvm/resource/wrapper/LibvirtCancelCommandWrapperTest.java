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

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.CancelCommand;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;

@RunWith(MockitoJUnitRunner.class)
public class LibvirtCancelCommandWrapperTest {

    @Mock
    private LibvirtComputingResource resource;

    private final LibvirtCancelCommandWrapper wrapper = new LibvirtCancelCommandWrapper();

    @Test
    public void checkOnlyAsksWithoutCancelling() {
        Mockito.when(resource.isRequestSequenceCancellable(7L)).thenReturn(true);

        final Answer answer = wrapper.execute(new CancelCommand(7L, "probe", true), resource);

        Assert.assertTrue(answer.getResult());
        Mockito.verify(resource, Mockito.never()).cancelRequestSequence(Mockito.anyLong());
    }

    @Test
    public void checkOnlyReportsNothingToStop() {
        Mockito.when(resource.isRequestSequenceCancellable(7L)).thenReturn(false);

        final Answer answer = wrapper.execute(new CancelCommand(7L, "probe", true), resource);

        Assert.assertFalse(answer.getResult());
        Assert.assertTrue(answer.getDetails(), answer.getDetails().contains("no work that can be stopped"));
    }

    @Test
    public void cancelDelegatesAndReportsTheOutcome() {
        Mockito.when(resource.cancelRequestSequence(7L)).thenReturn(true);

        final Answer answer = wrapper.execute(new CancelCommand(7L, "job cancelled"), resource);

        Assert.assertTrue(answer.getResult());
        Assert.assertTrue(answer.getDetails(), answer.getDetails().contains("job cancelled"));
        Mockito.verify(resource, Mockito.never()).isRequestSequenceCancellable(Mockito.anyLong());
    }

    @Test
    public void failedCancelIsReportedAsSuch() {
        Mockito.when(resource.cancelRequestSequence(7L)).thenReturn(false);

        final Answer answer = wrapper.execute(new CancelCommand(7L, "job cancelled"), resource);

        Assert.assertFalse(answer.getResult());
    }
}
