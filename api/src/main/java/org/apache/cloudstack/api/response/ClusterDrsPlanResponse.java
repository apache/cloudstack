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

package org.apache.cloudstack.api.response;

import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.api.EntityReference;
import org.apache.cloudstack.cluster.ClusterDrsPlan;

import java.util.Date;
import java.util.List;

@EntityReference(value = ClusterDrsPlan.class)
public class ClusterDrsPlanResponse extends BaseResponse {
    @SerializedName(ApiConstants.MIGRATIONS)
    @Param(description = "List of migrations")
    List<ClusterDrsPlanMigrationResponse> migrationPlans;

    @SerializedName(ApiConstants.ID)
    @Param(description = "unique ID of the drs plan for cluster")
    private String id;

    @SerializedName(ApiConstants.CLUSTER_ID)
    @Param(description = "Id of the cluster")
    private String clusterId;

    @SerializedName("eventid")
    @Param(description = "Start event Id of the DRS Plan")
    private String eventId;

    @SerializedName(ApiConstants.TYPE)
    @Param(description = "Type of DRS Plan (Automated or Manual))")
    private ClusterDrsPlan.Type type;

    @SerializedName(ApiConstants.STATUS)
    @Param(description = "Status of DRS Plan")
    private ClusterDrsPlan.Status status;

    @SerializedName(ApiConstants.CREATED)
    private Date created;


    public ClusterDrsPlanResponse(String clusterId, ClusterDrsPlan plan, String eventId,
                                  List<ClusterDrsPlanMigrationResponse> migrationPlans) {
        this.clusterId = clusterId;
        this.eventId = eventId;
        if (plan != null) {
            this.id = plan.getUuid();
            this.type = plan.getType();
            this.status = plan.getStatus();
            this.created = plan.getCreated();
        }
        this.migrationPlans = migrationPlans;
        this.setObjectName("drsPlan");
    }

    public List<ClusterDrsPlanMigrationResponse> getMigrationPlans() {
        return migrationPlans;
    }

    public void setId(String id) {
        this.id = id;
    }
}
