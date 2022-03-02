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
package com.cloud.storage;

import org.apache.commons.lang3.StringUtils;

public class GuestOSHypervisorMapping {

    private String hypervisorType;
    private String hypervisorVersion;
    private String guestOsName;

    public GuestOSHypervisorMapping(String hypervisorType, String hypervisorVersion, String guestOsName) {
        this.hypervisorType = hypervisorType;
        this.hypervisorVersion = hypervisorVersion;
        this.guestOsName = guestOsName;
    }

    public String getHypervisorType() {
        return hypervisorType;
    }

    public String getHypervisorVersion() {
        return hypervisorVersion;
    }

    public String getGuestOsName() {
        return guestOsName;
    }

    public boolean isValid() {
        if (StringUtils.isBlank(hypervisorType) || StringUtils.isBlank(hypervisorVersion) || StringUtils.isBlank(guestOsName)) {
            return false;
        }

        return true;
    }

    @Override
    public String toString() {
        return "Hypervisor(Version): " + hypervisorType + "(" + hypervisorVersion + "), Guest OS: " + guestOsName;
    }
}
