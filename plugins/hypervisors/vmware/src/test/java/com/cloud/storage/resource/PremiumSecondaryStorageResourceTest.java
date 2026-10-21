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
package com.cloud.storage.resource;

import org.apache.cloudstack.storage.resource.SecondaryStorageResourceHandler;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.CancelCommand;
import com.cloud.hypervisor.Hypervisor;

@RunWith(MockitoJUnitRunner.class)
public class PremiumSecondaryStorageResourceTest {

    @Mock
    private SecondaryStorageResourceHandler handler;

    private final PremiumSecondaryStorageResource resource = new PremiumSecondaryStorageResource();

    @Test
    public void cancelProbeIsAnsweredByTheHandlerRunningTheRequest() {
        resource.registerHandler(Hypervisor.HypervisorType.VMware, handler);
        Mockito.when(handler.checkRequestSequenceIsNotCancellable(7L)).thenReturn(null);

        final Answer answer = resource.executeRequest(new CancelCommand(7L, "probe", true));

        Assert.assertTrue(answer.getResult());
        Mockito.verify(handler, Mockito.never()).cancelRequestSequence(Mockito.anyLong());
    }

    @Test
    public void cancelProbeCarriesTheHandlersReason() {
        resource.registerHandler(Hypervisor.HypervisorType.VMware, handler);
        Mockito.when(handler.checkRequestSequenceIsNotCancellable(7L)).thenReturn("no vCenter task or transfer is running for CopyCommand");

        final Answer answer = resource.executeRequest(new CancelCommand(7L, "probe", true));

        Assert.assertFalse(answer.getResult());
        Assert.assertEquals("no vCenter task or transfer is running for CopyCommand", answer.getDetails());
    }

    @Test
    public void cancelIsDelegatedToTheHandler() {
        resource.registerHandler(Hypervisor.HypervisorType.VMware, handler);
        Mockito.when(handler.cancelRequestSequence(7L)).thenReturn(true);

        final Answer answer = resource.executeRequest(new CancelCommand(7L, "job cancelled"));

        Assert.assertTrue(answer.getResult());
        Assert.assertTrue(answer.getDetails(), answer.getDetails().contains("job cancelled"));
    }

    @Test
    public void withoutAHandlerNothingCanBeCancelled() {
        Assert.assertFalse(resource.executeRequest(new CancelCommand(7L, "probe", true)).getResult());
        Assert.assertFalse(resource.executeRequest(new CancelCommand(7L, "job cancelled")).getResult());
    }
}
