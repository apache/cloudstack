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

package org.apache.cloudstack.storage.feign.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Svm {
    @JsonProperty("uuid")
    @SerializedName("uuid")
    private String uuid = null;

    @JsonProperty("name")
    @SerializedName("name")
    private String name = null;

    @JsonProperty("iscsi.enabled")
    @SerializedName("iscsi.enabled")
    private Boolean iscsiEnabled = null;

    @JsonProperty("fcp.enabled")
    @SerializedName("fcp.enabled")
    private Boolean fcpEnabled = null;

    @JsonProperty("nfs.enabled")
    @SerializedName("nfs.enabled")
    private Boolean nfsEnabled = null;

    @JsonProperty("aggregates")
    @SerializedName("aggregates")
    private List<Aggregate> aggregates = null;

    @JsonProperty("aggregates_delegated")
    @SerializedName("aggregates_delegated")
    private Boolean aggregatesDelegated = null;

    @JsonProperty("state.value")
    @SerializedName("state.value")
    private String state = null;
}
