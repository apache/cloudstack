/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.cloud.storage.dao;

import com.cloud.api.query.dao.AsyncJobJoinDaoImpl;
import com.cloud.api.query.vo.AsyncJobJoinVO;
import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.response.AsyncJobResponse;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;

@RunWith(MockitoJUnitRunner.class)
public class AsyncJobJoinDaoTest {

    @InjectMocks
    AsyncJobJoinDaoImpl dao;

    @Test
    public void testNewAsyncJobResponseValidValues() {
        final AsyncJobJoinVO job = new AsyncJobJoinVO();
        ReflectionTestUtils.setField(job,"uuid","a2b22932-1b61-4406-8e89-4ae19968e8d3");
        ReflectionTestUtils.setField(job,"accountUuid","4dea2836-72cc-11e8-b2de-107b4429825a");
        ReflectionTestUtils.setField(job,"domainUuid","4dea136b-72cc-11e8-b2de-107b4429825a");
        ReflectionTestUtils.setField(job,"userUuid","4decc724-72cc-11e8-b2de-107b4429825a");
        ReflectionTestUtils.setField(job,"cmd","org.apache.cloudstack.api.command.admin.vm.StartVMCmdByAdmin");
        ReflectionTestUtils.setField(job,"status",0);
        ReflectionTestUtils.setField(job,"resultCode",0);
        ReflectionTestUtils.setField(job,"result",null);
        ReflectionTestUtils.setField(job,"created",new Date());
        ReflectionTestUtils.setField(job,"removed",new Date());
        ReflectionTestUtils.setField(job,"instanceType", ApiCommandResourceType.VirtualMachine);
        ReflectionTestUtils.setField(job,"instanceId",3L);
        final AsyncJobResponse response = dao.newAsyncJobResponse(job);
        Assert.assertEquals(job.getUuid(),response.getJobId());
        Assert.assertEquals(job.getAccountUuid(), ReflectionTestUtils.getField(response, "accountId"));
        Assert.assertEquals(job.getUserUuid(), ReflectionTestUtils.getField(response, "userId"));
        Assert.assertEquals(job.getCmd(), ReflectionTestUtils.getField(response, "cmd"));
        Assert.assertEquals(job.getStatus(), ReflectionTestUtils.getField(response, "jobStatus"));
        Assert.assertEquals(job.getStatus(), ReflectionTestUtils.getField(response, "jobProcStatus"));
        Assert.assertEquals(job.getResultCode(), ReflectionTestUtils.getField(response, "jobResultCode"));
        Assert.assertEquals(null, ReflectionTestUtils.getField(response, "jobResultType"));
        Assert.assertEquals(job.getResult(), ReflectionTestUtils.getField(response, "jobResult"));
        Assert.assertEquals(job.getInstanceType().toString(), ReflectionTestUtils.getField(response, "jobInstanceType"));
        Assert.assertEquals(job.getInstanceUuid(), ReflectionTestUtils.getField(response, "jobInstanceId"));
        Assert.assertEquals(job.getCreated(), ReflectionTestUtils.getField(response, "created"));
        Assert.assertEquals(job.getRemoved(), ReflectionTestUtils.getField(response, "removed"));
    }

    @Test
    public void testNewAsyncJobResponseNullValues() {
        final AsyncJobJoinVO job = new AsyncJobJoinVO();
        final AsyncJobResponse response = dao.newAsyncJobResponse(job);
        Assert.assertEquals(job.getUuid(),response.getJobId());
        Assert.assertEquals(job.getAccountUuid(), ReflectionTestUtils.getField(response, "accountId"));
        Assert.assertEquals(job.getUserUuid(), ReflectionTestUtils.getField(response, "userId"));
        Assert.assertEquals(job.getCmd(), ReflectionTestUtils.getField(response, "cmd"));
        Assert.assertEquals(job.getStatus(), ReflectionTestUtils.getField(response, "jobStatus"));
        Assert.assertEquals(job.getStatus(), ReflectionTestUtils.getField(response, "jobProcStatus"));
        Assert.assertEquals(job.getResultCode(), ReflectionTestUtils.getField(response, "jobResultCode"));
        Assert.assertEquals(null, ReflectionTestUtils.getField(response, "jobResultType"));
        Assert.assertEquals(job.getResult(), ReflectionTestUtils.getField(response, "jobResult"));
        Assert.assertEquals(job.getInstanceType(), ReflectionTestUtils.getField(response, "jobInstanceType"));
        Assert.assertEquals(job.getInstanceUuid(), ReflectionTestUtils.getField(response, "jobInstanceId"));
        Assert.assertEquals(job.getCreated(), ReflectionTestUtils.getField(response, "created"));
        Assert.assertEquals(job.getRemoved(), ReflectionTestUtils.getField(response, "removed"));
    }
}
