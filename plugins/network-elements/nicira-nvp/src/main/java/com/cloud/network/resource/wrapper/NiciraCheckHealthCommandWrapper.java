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

package com.cloud.network.resource.wrapper;


import com.cloud.agent.api.Answer;
import com.cloud.agent.api.CheckHealthAnswer;
import com.cloud.agent.api.CheckHealthCommand;
import com.cloud.network.nicira.ControlClusterStatus;
import com.cloud.network.nicira.NiciraNvpApi;
import com.cloud.network.nicira.NiciraNvpApiException;
import com.cloud.network.resource.NiciraNvpResource;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;

@ResourceWrapper(handles = CheckHealthCommand.class)
public class NiciraCheckHealthCommandWrapper extends CommandWrapper<CheckHealthCommand, Answer, NiciraNvpResource> {

    private static final String CONTROL_CLUSTER_STATUS_IS_STABLE = "stable";

    @Override
    public Answer execute(final CheckHealthCommand command, final NiciraNvpResource serverResource) {
        final NiciraNvpApi niciraNvpApi = serverResource.getNiciraNvpApi();
        boolean healthy = true;
        try {
            final ControlClusterStatus clusterStatus = niciraNvpApi.getControlClusterStatus();
            final String status = clusterStatus.getClusterStatus();
            if (clusterIsUnstable(status)) {
                logger.warn("Control cluster is not stable. Current status is " + status);
                healthy = false;
            }
        } catch (final NiciraNvpApiException e) {
            logger.error("Exception caught while checking control cluster status during health check", e);
            healthy = false;
        }

        return new CheckHealthAnswer(command, healthy);
    }

    protected boolean clusterIsUnstable(final String clusterStatus) {
        return !CONTROL_CLUSTER_STATUS_IS_STABLE.equals(clusterStatus);
    }

}
