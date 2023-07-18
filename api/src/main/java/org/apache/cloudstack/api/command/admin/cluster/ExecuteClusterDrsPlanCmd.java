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

package org.apache.cloudstack.api.command.admin.cluster;

import com.cloud.user.Account;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.ClusterDrsPlanResponse;
import org.apache.cloudstack.api.response.ClusterResponse;
import org.apache.cloudstack.api.response.SuccessResponse;
import org.apache.cloudstack.cluster.ClusterDrsService;
import org.apache.commons.collections.MapUtils;
import org.apache.log4j.Logger;

import javax.inject.Inject;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@APICommand(name = "executeClusterDrsPlan",
        description = "Schedule DRS for a cluster. If there is another plan in progress for the same cluster, this command will fail.",
        responseObject = SuccessResponse.class,
        since = "4.19.0")
public class ExecuteClusterDrsPlanCmd extends BaseCmd {

    static final Logger LOG = Logger.getLogger(ExecuteClusterDrsPlanCmd.class);

    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = ClusterResponse.class, required = true, description = "the ID of cluster")
    private Long id;

    @Parameter(name = ApiConstants.MIGRATE_TO, type = CommandType.MAP, entityType = ClusterDrsPlanResponse.class, required = true, description = "the ID of plan to execute")
    private Map migrateVmTo;

    @Inject
    private ClusterDrsService clusterDrsService;

    public Long getId() {
        return id;
    }


    public Map<String, String> getVmToHostMap() {
        Map<String, String> vmToHostMap = new HashMap<>();
        if (MapUtils.isNotEmpty(migrateVmTo)) {
            Collection<?> allValues = migrateVmTo.values();
            Iterator<?> iter = allValues.iterator();
            while (iter.hasNext()) {
                HashMap<String, String> volumeToPool = (HashMap<String, String>)iter.next();
                String volume = volumeToPool.get("volume");
                String pool = volumeToPool.get("pool");
                vmToHostMap.put(volume, pool);
            }
        }
        return vmToHostMap;
    }

    @Override
    public void execute() {
        boolean result = clusterDrsService.executeDrsPlan(this);
        SuccessResponse response = new SuccessResponse();
        response.setSuccess(result);
        response.setResponseName(getCommandName());
        this.setResponseObject(response);
    }

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }

    @Override
    public Long getApiResourceId() {
        return getId();
    }

    @Override
    public ApiCommandResourceType getApiResourceType() {
        return ApiCommandResourceType.Cluster;
    }
}
