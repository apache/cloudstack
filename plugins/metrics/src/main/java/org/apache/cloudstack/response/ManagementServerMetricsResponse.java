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

    @SerializedName(ApiConstants.JAVA_DISTRIBUTION)
    @Param(description = "the java distribution name running the management server process")
    private String javaDistribution;

    @SerializedName(ApiConstants.JAVA_VERSION)
    @Param(description = "the version of the java distribution running the management server process")
    private String javaVersion;

    @SerializedName(ApiConstants.OS_DISTRIBUTION)
    @Param(description = "the name of the OS distribution running on the management server")
    private String osDistribution;

    @SerializedName(ApiConstants.OS_VERSION)
    @Param(description = "the version of the OS running on the management server")
    private String osVersion;

    public void setJavaDistribution(String javaDistribution) {
        this.javaDistribution = javaDistribution;
    }

    public void setJavaVersion(String javaVersion) {
        this.javaVersion = javaVersion;
    }

    public void setOsDistribution(String osDistribution) {
        this.osDistribution = osDistribution;
    }

    public void setOsVersion(String osVersion) {
        this.osVersion = osVersion;
    }
}
