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
package org.apache.cloudstack.response;

import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.response.ManagementServerResponse;

public class ManagementServerMetricsResponse extends ManagementServerResponse {

    @SerializedName(ApiConstants.AVAILABLE_PROCESSORS)
    @Param(description = "the number of processors available to the JVM")
    private int availableProcessors;

    @SerializedName(ApiConstants.AGENT_COUNT)
    @Param(description = "the number of agents this Management Server is responsible for")
    private int agentCount;

    @SerializedName(ApiConstants.SESSIONS)
    @Param(description = "the number of client sessions active on this Management Server")
    private long sessions;

    public void setAvailableProcessors(int availableProcessors) {
        this.availableProcessors = availableProcessors;
    }

    public void setAgentCount(int agentCount) {
        this.agentCount = agentCount;
    }

    public void setSessions(long sessions) {
        this.sessions = sessions;
    }
}
