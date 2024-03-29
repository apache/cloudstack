//
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
//

package com.cloud.hypervisor.xenserver.resource.wrapper.xen620sp1;

import java.util.HashMap;


import com.cloud.agent.api.Answer;
import com.cloud.agent.api.GetGPUStatsAnswer;
import com.cloud.agent.api.GetGPUStatsCommand;
import com.cloud.agent.api.VgpuTypesInfo;
import com.cloud.hypervisor.xenserver.resource.XenServer620SP1Resource;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.xensource.xenapi.Connection;

@ResourceWrapper(handles =  GetGPUStatsCommand.class)
public final class XenServer620SP1GetGPUStatsCommandWrapper extends CommandWrapper<GetGPUStatsCommand, Answer, XenServer620SP1Resource> {


    @Override
    public Answer execute(final GetGPUStatsCommand command, final XenServer620SP1Resource xenServer620SP1Resource) {
        final Connection conn = xenServer620SP1Resource.getConnection();
        HashMap<String, HashMap<String, VgpuTypesInfo>> groupDetails = new HashMap<String, HashMap<String, VgpuTypesInfo>>();
        try {
            groupDetails = xenServer620SP1Resource.getGPUGroupDetails(conn);
        } catch (final Exception e) {
            final String msg = "Unable to get GPU stats" + e.toString();
            logger.warn(msg, e);
            return new GetGPUStatsAnswer(command, false, msg);
        }
        return new GetGPUStatsAnswer(command, groupDetails);
    }
}
