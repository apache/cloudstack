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
package com.cloud.api;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;

import java.util.Map;

import org.apache.cloudstack.api.command.user.autoscale.DeleteAutoScalePolicyCmd;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.framework.jobs.AsyncJob;
import org.apache.cloudstack.framework.jobs.AsyncJobManager;
import org.apache.cloudstack.jobs.JobInfo;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.test.util.ReflectionTestUtils;

import com.cloud.user.Account;
import com.cloud.user.AccountService;
import com.cloud.user.User;
import com.cloud.utils.component.ComponentContext;
import com.cloud.utils.db.EntityManager;
import com.cloud.utils.exception.CloudRuntimeException;

@RunWith(MockitoJUnitRunner.class)
public class ApiAsyncJobDispatcherTest {

    private ApiAsyncJobDispatcher dispatcher;

    @Mock
    private ApiDispatcher apiDispatcher;
    @Mock
    private AsyncJobManager asyncJobMgr;
    @Mock
    private EntityManager entityMgr;
    @Mock
    private AsyncJob job;
    @Mock
    private User user;
    @Mock
    private Account account;

    private MockedStatic<ComponentContext> componentContextMocked;

    @Before
    public void setUp() {
        dispatcher = new ApiAsyncJobDispatcher();
        ReflectionTestUtils.setField(dispatcher, "_dispatcher", apiDispatcher);
        ReflectionTestUtils.setField(dispatcher, "_asyncJobMgr", asyncJobMgr);
        ReflectionTestUtils.setField(dispatcher, "_entityMgr", entityMgr);

        componentContextMocked = Mockito.mockStatic(ComponentContext.class);
        // ComponentContext.inject just returns the same command instance in production; do the same here.
        // Mockito.any(Object.class) (rather than the bare any()) is needed to unambiguously bind to the
        // inject(Object) overload rather than inject(Class<T>).
        componentContextMocked.when(() -> ComponentContext.inject(Mockito.any(Object.class))).thenAnswer(inv -> inv.getArgument(0));
        // ResponseMessageResolver's metadata rendering calls CallContext.isCallingAccountRootAdmin(),
        // which looks up an AccountService bean; simulate "no bean available" so it falls back cleanly
        // instead of NPEing on a null bean returned by the (otherwise unstubbed) static mock.
        componentContextMocked.when(() -> ComponentContext.getDelegateComponentOfType(AccountService.class))
                .thenThrow(new NoSuchBeanDefinitionException(AccountService.class));

        CallContext.init(entityMgr);

        Mockito.when(job.getId()).thenReturn(1L);
        Mockito.when(job.getAccountId()).thenReturn(1L);
        Mockito.when(job.getUserId()).thenReturn(1L);
        Mockito.when(job.getCmd()).thenReturn(DeleteAutoScalePolicyCmd.class.getName());
        Mockito.when(job.getCmdInfo()).thenReturn("{\"ctxUserId\":\"1\",\"ctxAccountId\":\"1\"}");
        Mockito.when(entityMgr.findById(User.class, 1L)).thenReturn(user);
        Mockito.when(entityMgr.findById(Account.class, 1L)).thenReturn(account);
    }

    @After
    public void tearDown() {
        componentContextMocked.close();
        CallContext.unregisterAll();
    }

    @Test
    public void testRunJobEnrichesFailureResponseWithKeyAndMetadataForCloudRuntimeException() throws Exception {
        CloudRuntimeException failure = new CloudRuntimeException("Unable to find network with ID abc");
        failure.setMessageKey("vm.deploy.network.not.found");
        failure.setMetadata(Map.of("id", "abc"));
        Mockito.doThrow(failure).when(apiDispatcher).dispatch(Mockito.any(), Mockito.anyMap(), eq(true));

        dispatcher.runJob(job);

        ArgumentCaptor<String> resultCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(asyncJobMgr).completeAsyncJob(eq(1L), eq(JobInfo.Status.FAILED), anyInt(), resultCaptor.capture());

        String serializedResponse = resultCaptor.getValue();
        assertTrue("serialized failure response should carry the structured error key",
                serializedResponse.contains("\"errortextkey\":\"vm.deploy.network.not.found\""));
        assertTrue("serialized failure response should carry the error metadata",
                serializedResponse.contains("\"errormetadata\""));
    }

    @Test
    public void testRunJobDoesNotEnrichResponseForNonCloudRuntimeException() throws Exception {
        Mockito.doThrow(new IllegalStateException("boom")).when(apiDispatcher).dispatch(Mockito.any(), Mockito.anyMap(), eq(true));

        dispatcher.runJob(job);

        ArgumentCaptor<String> resultCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(asyncJobMgr).completeAsyncJob(eq(1L), eq(JobInfo.Status.FAILED), anyInt(), resultCaptor.capture());

        String serializedResponse = resultCaptor.getValue();
        assertTrue("a non-CloudRuntimeException failure must not carry a structured error key",
                !serializedResponse.contains("\"errortextkey\""));
    }
}
