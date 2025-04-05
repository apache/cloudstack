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
package org.apache.cloudstack.backup.backroll.model.response.policy;

import com.fasterxml.jackson.annotation.JsonProperty;

public class BackrollBackupPolicyResponse {
    @JsonProperty("name")
    public String name;

    @JsonProperty("retention_day")
    public int retentionDay;

    @JsonProperty("schedule")
    public String schedule;

    @JsonProperty("retention_month")
    public int retentionMonth;

    @JsonProperty("storage")
    public String storage;

    @JsonProperty("enabled")
    public Boolean enabled;

    @JsonProperty("description")
    public String description;

    @JsonProperty("id")
    public String id;

    @JsonProperty("retention_week")
    public int retentionWeek;

    @JsonProperty("retention_year")
    public int retentionYear;

    @JsonProperty("externalhook")
    public String externalHook;
}
