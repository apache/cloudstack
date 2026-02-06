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

import org.apache.cloudstack.veeam.VeeamControlService;
import org.apache.cloudstack.veeam.api.JobsRouteHandler;
import org.apache.cloudstack.veeam.api.dto.Actions;
import org.apache.cloudstack.veeam.api.dto.Job;
import org.apache.cloudstack.veeam.api.dto.Ref;

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
        job.setActions(new Actions());
        job.setDescription("Something");
        job.setLink(Collections.emptyList());
        return job;
    }
}
