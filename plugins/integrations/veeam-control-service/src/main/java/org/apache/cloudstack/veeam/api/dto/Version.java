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

package org.apache.cloudstack.veeam.api.dto;

import org.apache.cloudstack.utils.CloudStackVersion;
import org.apache.cloudstack.veeam.VeeamControlService;
import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public final class Version {

    private String build;
    private String fullVersion;
    private String major;
    private String minor;
    private String revision;

    public Version() {
    }

    public String getBuild() {
        return build;
    }

    public void setBuild(String build) {
        this.build = build;
    }

    public String getFullVersion() {
        return fullVersion;
    }

    public void setFullVersion(String fullVersion) {
        this.fullVersion = fullVersion;
    }

    public String getMajor() {
        return major;
    }

    public void setMajor(String major) {
        this.major = major;
    }

    public String getMinor() {
        return minor;
    }

    public void setMinor(String minor) {
        this.minor = minor;
    }

    public String getRevision() {
        return revision;
    }

    public void setRevision(String revision) {
        this.revision = revision;
    }

    public static Version fromPackageAndCSVersion(boolean complete) {
        Version version = new Version();
        String packageVersion = VeeamControlService.getPackageVersion();
        if (StringUtils.isNotBlank(packageVersion) && complete) {
            version.setFullVersion(packageVersion);
        }
        CloudStackVersion csVersion = VeeamControlService.getCSVersion();
        if (csVersion == null) {
            return version;
        }
        version.setMajor(String.valueOf(csVersion.getMajorRelease()));
        version.setMinor(String.valueOf(csVersion.getMinorRelease()));
        version.setBuild(String.valueOf(csVersion.getPatchRelease()));
        version.setRevision(String.valueOf(csVersion.getSecurityRelease()));
        return version;
    }
}
