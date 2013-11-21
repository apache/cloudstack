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
package com.cloud.bridge.service.core.ec2;

import com.cloud.bridge.service.exception.EC2ServiceException;
import com.cloud.bridge.service.exception.EC2ServiceException.ClientError;

public class EC2RegisterImage {

    private String location;
    private String name;
    private String description;
    private String format;
    private String zoneName;
    private String osTypeName;
    private String hypervisor;

    public EC2RegisterImage() {
        location = null;
        name = null;
        description = null;
        format = null;
        zoneName = null;
        osTypeName = null;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getLocation() {
        return this.location;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDescription() {
        return this.description;
    }

    /**
     * We redefine the expected format of this field to be:
     * "format:zonename:ostypename:hypervisor"
     *
     * @param param
     */
    public void setArchitecture(String param) {
        if (null != param) {
            if (!param.contains(":") || param.split(":").length < 4) {
                throw new EC2ServiceException(ClientError.InvalidParameterValue, "Supported format for " + "parameter 'architecture' is format:zonename:ostypename:hypervisor");
            }
            String parts[] = param.split(":");
            format = parts[0];
            zoneName = parts[1];
            osTypeName = parts[2];
            hypervisor = parts[3];
        }
    }

    public String getFormat() {
        return this.format;
    }

    public String getZoneName() {
        return this.zoneName;
    }

    public String getOsTypeName() {
        return this.osTypeName;
    }

    public String getHypervisor() {
        return hypervisor;
    }
}
