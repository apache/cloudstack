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

package org.apache.cloudstack.api.response;

import com.google.gson.annotations.SerializedName;

import org.apache.cloudstack.api.ApiConstants;

import com.cloud.serializer.Param;

/**
 * Extends {@link UserVmResponse} rather than duplicating id/name/state fields, since this is set
 * up from a plain {@code UserVmVO} (basic details only, via {@code UserVmDao}) with just a few
 * boot-group-specific fields of its own — never populated from the join-based VM response builder.
 */
@SuppressWarnings("unused")
public class InstanceBootGroupMemberChildResponse extends UserVmResponse {

    @SerializedName(ApiConstants.READINESS_MODE)
    @Param(description = "None, ChildDependent or RuleBased, computed from whether readiness rules are attached")
    private String readinessMode;

    @SerializedName(ApiConstants.READINESS_STATUS)
    @Param(description = "The last cached readiness evaluation")
    private String readinessStatus;

    @SerializedName(ApiConstants.READINESS_MESSAGE)
    @Param(description = "Why readinessstatus is what it is: the failing rule(s)' cached message")
    private String readinessMessage;

    public void setReadinessMode(String readinessMode) {
        this.readinessMode = readinessMode;
    }

    public void setReadinessStatus(String readinessStatus) {
        this.readinessStatus = readinessStatus;
    }

    public void setReadinessMessage(String readinessMessage) {
        this.readinessMessage = readinessMessage;
    }
}
