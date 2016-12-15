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

package org.apache.cloudstack.api.command.user.job;

import org.apache.cloudstack.api.ResponseGenerator;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.AsyncJobResponse;
import org.apache.cloudstack.jobs.AsyncJobService;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CancelAsyncJobCmdTest {

    @InjectMocks
    private CancelAsyncJobCmd cancelAsyncJobCmd;

    @Mock
    private AsyncJobService asyncJobService;

    @Mock
    public ResponseGenerator _responseGenerator;

    private InOrder inorder;

    @Before
    public void setUp() throws Exception {
        inorder = Mockito.inOrder(asyncJobService, _responseGenerator);
    }

    @After
    public void tearDown() throws Exception {
        inorder = null;
    }

    /**
     * test when id is null
     */
    @Test(expected = NullPointerException.class)
    public void testExecute_NPE() throws Exception {
        cancelAsyncJobCmd.execute();
    }

    /**
     * test when job cancellation fails. It should throw api exception and execute should fail
     */
    @Test
    public void testExecute_errorCancellation() throws Exception {
        long id = 1;
        String errorString = "Error cancelling job";
        cancelAsyncJobCmd.id = id;
        Mockito.when(asyncJobService.cancelAsyncJob(Mockito.eq(id), Mockito.anyString())).thenReturn(errorString);
        try {
            cancelAsyncJobCmd.execute();
        } catch (Exception e) {
            if (e instanceof ServerApiException) {
                Assert.assertEquals(errorString, e.getMessage());
            } else {
                Assert.fail("server api exception is expected");
            }
        }

        inorder.verify(asyncJobService, Mockito.times(1)).cancelAsyncJob(Mockito.eq(id), Mockito.anyString());
        inorder.verify(_responseGenerator, Mockito.times(0)).queryJobResult((CancelAsyncJobCmd) Mockito.any());
        inorder.verifyNoMoreInteractions();
    }

    /**
     * test when job cancellation succeeds.
     */
    @Test
    public void testExecute_successfulCancellation() throws Exception {
        long id = 1L;
        cancelAsyncJobCmd.id = id;
        Mockito.when(asyncJobService.cancelAsyncJob(Mockito.eq(id), Mockito.anyString())).thenReturn(null);
        Mockito.when(_responseGenerator.queryJobResult((CancelAsyncJobCmd) Mockito.any())).thenReturn(Mockito.mock
                (AsyncJobResponse.class));

        cancelAsyncJobCmd.execute();
        inorder.verify(asyncJobService, Mockito.times(1)).cancelAsyncJob(Mockito.eq(id), Mockito.anyString());
        inorder.verify(_responseGenerator, Mockito.times(1)).queryJobResult((CancelAsyncJobCmd) Mockito.any());
        inorder.verifyNoMoreInteractions();
    }
}
