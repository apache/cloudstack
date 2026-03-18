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

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.cloudstack.jobs.JobInfo;
import org.apache.cloudstack.veeam.VeeamControlService;
import org.apache.cloudstack.veeam.api.JobsRouteHandler;
import org.apache.cloudstack.veeam.api.dto.Job;
import org.apache.cloudstack.veeam.api.dto.Ref;
import org.apache.cloudstack.veeam.api.dto.ResourceAction;
import org.apache.cloudstack.veeam.api.dto.VmAction;

import com.cloud.api.query.vo.AsyncJobJoinVO;
import com.cloud.api.query.vo.UserVmJoinVO;

public class AsyncJobJoinVOToJobConverter {

    public static Job toJob(String uuid, String state, long startTime) {
        Job job = new Job();
        final String basePath = VeeamControlService.ContextPath.value();
        // Fill in dummy data for now, as the AsyncJobJoinVO does not contain all the necessary information to populate a Job object.
        job.setId(uuid);
        job.setHref(basePath + JobsRouteHandler.BASE_ROUTE + "/" + uuid);
        job.setAutoCleared(Boolean.TRUE.toString());
        job.setExternal(Boolean.TRUE.toString());
        job.setLastUpdated(System.currentTimeMillis());
        job.setStartTime(startTime);
        job.setStatus(state);
        if ("complete".equalsIgnoreCase(state) || "finished".equalsIgnoreCase(state)) {
            job.setEndTime(System.currentTimeMillis());
        }
        job.setOwner(Ref.of(basePath + "/api/users/" + uuid, uuid));
        job.setDescription("Something");
        job.setLink(Collections.emptyList());
        return job;
    }

    public static Job toJob(AsyncJobJoinVO vo) {
        Job job = new Job();
        final String basePath = VeeamControlService.ContextPath.value();
        job.setId(vo.getUuid());
        job.setHref(basePath + JobsRouteHandler.BASE_ROUTE + "/" + vo.getUuid());
        job.setAutoCleared(Boolean.TRUE.toString());
        job.setExternal(Boolean.TRUE.toString());
        job.setLastUpdated(System.currentTimeMillis());
        job.setStartTime(vo.getCreated().getTime());
        JobInfo.Status status = JobInfo.Status.values()[vo.getStatus()];
        Long endTime = System.currentTimeMillis();
        if (status == JobInfo.Status.SUCCEEDED) {
            job.setStatus("finished");
            job.setEndTime(System.currentTimeMillis());
        } else if (status == JobInfo.Status.FAILED) {
            job.setStatus(status.name().toLowerCase());
        } else if (status == JobInfo.Status.CANCELLED) {
            job.setStatus("aborted");
        } else {
            job.setStatus("started");
            endTime = null;
        }
        if (endTime != null) {
            job.setEndTime(endTime);
        }
        job.setOwner(Ref.of(basePath + "/api/users/" + vo.getUserUuid(), vo.getUserUuid()));
        job.setDescription("Something");
        job.setLink(Collections.emptyList());
        return job;
    }

    public static List<Job> toJobList(List<AsyncJobJoinVO> vos) {
        return vos.stream().map(AsyncJobJoinVOToJobConverter::toJob).collect(Collectors.toList());
    }

    protected static void fillAction(final ResourceAction action, final AsyncJobJoinVO vo) {
        final String basePath = VeeamControlService.ContextPath.value();
        action.setJob(Ref.of(basePath + JobsRouteHandler.BASE_ROUTE + vo.getUuid(), vo.getUuid()));
        action.setStatus("complete");
    }

    public static VmAction toVmAction(final AsyncJobJoinVO vo, final UserVmJoinVO vm) {
        VmAction action = new VmAction();
        fillAction(action, vo);
        action.setVm(UserVmJoinVOToVmConverter.toVm(vm, null, null, null, null, false));
        return action;
    }

    public static ResourceAction toAction(final AsyncJobJoinVO vo) {
        VmAction action = new VmAction();
        fillAction(action, vo);
        return action;
    }
}
