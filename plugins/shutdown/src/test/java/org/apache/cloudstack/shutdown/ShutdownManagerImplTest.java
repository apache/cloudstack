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

package org.apache.cloudstack.shutdown;

import org.apache.cloudstack.framework.jobs.AsyncJobManager;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.utils.exception.CloudRuntimeException;


@RunWith(MockitoJUnitRunner.class)
public class ShutdownManagerImplTest {

    @Spy
    @InjectMocks
    ShutdownManagerImpl spy;

    @Mock
    AsyncJobManager jobManagerMock;

    private long prepareCountPendingJobs() {
        long expectedCount = 1L;
        Mockito.doReturn(expectedCount).when(jobManagerMock).countPendingNonPseudoJobs(1L);
        return expectedCount;
    }

    @Test
    public void countPendingJobs() {
        long expectedCount = prepareCountPendingJobs();
        long count = spy.countPendingJobs(1L);
        Assert.assertEquals(expectedCount, count);
    }

    @Test
    public void cancelShutdown() {
        Assert.assertThrows(CloudRuntimeException.class, () -> {
            spy.cancelShutdown();
        });
    }

    @Test
    public void prepareForShutdown() {
        Mockito.doNothing().when(jobManagerMock).disableAsyncJobs();
        spy.prepareForShutdown();
        Mockito.verify(jobManagerMock).disableAsyncJobs();

        Assert.assertThrows(CloudRuntimeException.class, () -> {
            spy.prepareForShutdown();
        });


        Mockito.doNothing().when(jobManagerMock).enableAsyncJobs();
        spy.cancelShutdown();
        Mockito.verify(jobManagerMock).enableAsyncJobs();
    }
}
