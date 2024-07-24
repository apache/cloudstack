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
import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.api.MetricConstants;

import java.util.Date;

public class DbMetricsResponse extends BaseResponse {
    @SerializedName(MetricConstants.COLLECTION_TIME)
    @Param(description = "the time these statistics were collected")
    private Date collectionTime;

    @SerializedName(ApiConstants.HOST_NAME)
    @Param(description = "the name of the active usage server")
    private String hostname;

    @SerializedName(MetricConstants.REPLICAS)
    @Param(description = "the state of the usage server")
    private String[] replicas;

    @SerializedName(MetricConstants.CONNECTIONS)
    @Param(description = "the number of connections to the DB")
    private int connections;

    @SerializedName(MetricConstants.UPTIME)
    @Param(description = "the uptime of the DB in seconds")
    private long uptime;

    @SerializedName(MetricConstants.TLS_VERSIONS)
    @Param(description = "the tls versions currently in use (accepted) by the DB")
    private String tlsVersions;

    @SerializedName(ApiConstants.VERSION)
    @Param(description = "the version of the currently running DB")
    private String version;

    @SerializedName(MetricConstants.VERSION_COMMENT)
    @Param(description = "the version of the currently running DB")
    private String versionComment;

    @SerializedName(MetricConstants.QUERIES)
    @Param(description = "the number of queries performed on the DB")
    private long queries;

    @SerializedName(MetricConstants.DATABASE_LOAD_AVERAGES)
    @Param(description = "the last measured load averages on the DB")
    private double[] loadAverages;

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public void setReplicas(String[] replicas) {
        this.replicas = replicas;
    }

    public void setConnections(int connections) {
        this.connections = connections;
    }

    public void setUptime(long uptime) {
        this.uptime = uptime;
    }

    public void setTlsVersions(String tlsVersions) {
        this.tlsVersions = tlsVersions;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public void setVersionComment(String versionComment) {
        this.versionComment = versionComment;
    }

    public void setQueries(long queries) {
        this.queries = queries;
    }

    public void setLoadAverages(double []  loadAverages) {
        this.loadAverages = loadAverages;
    }

    public void setCollectionTime(Date collectionTime) {
        this.collectionTime = collectionTime;
    }
}
