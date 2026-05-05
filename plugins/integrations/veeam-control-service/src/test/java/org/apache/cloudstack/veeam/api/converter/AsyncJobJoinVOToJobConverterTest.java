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

package org.apache.cloudstack.veeam.api.converter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.List;

import org.apache.cloudstack.jobs.JobInfo;
import org.apache.cloudstack.veeam.api.dto.Job;
import org.apache.cloudstack.veeam.api.dto.ResourceAction;
import org.junit.Test;

import com.cloud.api.query.vo.AsyncJobJoinVO;

public class AsyncJobJoinVOToJobConverterTest {

    @Test
    public void testToJob_MapsSucceededStatusAndOwnerRef() {
        final AsyncJobJoinVO vo = mock(AsyncJobJoinVO.class);
        when(vo.getUuid()).thenReturn("job-1");
        when(vo.getUserUuid()).thenReturn("user-1");
        when(vo.getCreated()).thenReturn(new Date(1000L));
        when(vo.getStatus()).thenReturn(JobInfo.Status.SUCCEEDED.ordinal());

        final Job job = AsyncJobJoinVOToJobConverter.toJob(vo);

        assertEquals("job-1", job.getId());
        assertEquals("finished", job.getStatus());
        assertEquals(Long.valueOf(1000L), job.getStartTime());
        assertNotNull(job.getEndTime());
        assertEquals("user-1", job.getOwner().getId());
    }

    @Test
    public void testToJob_MapsInProgressToStartedAndNoEndTime() {
        final AsyncJobJoinVO vo = mock(AsyncJobJoinVO.class);
        when(vo.getUuid()).thenReturn("job-2");
        when(vo.getUserUuid()).thenReturn("user-2");
        when(vo.getCreated()).thenReturn(new Date(2000L));
        when(vo.getStatus()).thenReturn(JobInfo.Status.IN_PROGRESS.ordinal());

        final Job job = AsyncJobJoinVOToJobConverter.toJob(vo);

        assertEquals("started", job.getStatus());
        assertNull(job.getEndTime());
    }

    @Test
    public void testToActionAndToJobList() {
        final AsyncJobJoinVO vo = mock(AsyncJobJoinVO.class);
        when(vo.getUuid()).thenReturn("job-3");
        when(vo.getUserUuid()).thenReturn("user-3");
        when(vo.getCreated()).thenReturn(new Date(3000L));
        when(vo.getStatus()).thenReturn(JobInfo.Status.CANCELLED.ordinal());

        final ResourceAction action = AsyncJobJoinVOToJobConverter.toAction(vo);
        assertEquals("complete", action.getStatus());
        assertEquals("job-3", action.getJob().getId());

        final List<Job> jobs = AsyncJobJoinVOToJobConverter.toJobList(List.of(vo));
        assertEquals(1, jobs.size());
        assertEquals("aborted", jobs.get(0).getStatus());
    }
}
