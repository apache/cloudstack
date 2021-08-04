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
package com.cloud.storage.upload.params;

import com.cloud.hypervisor.Hypervisor;

import java.util.Map;

public class TemplateUploadParams extends UploadParamsBase {

    public TemplateUploadParams(long userId, String name, String displayText,
                                Integer bits, Boolean passwordEnabled, Boolean requiresHVM,
                                Boolean isPublic, Boolean featured,
                                Boolean isExtractable, String format, Long guestOSId,
                                Long zoneId, Hypervisor.HypervisorType hypervisorType, String chksum,
                                String templateTag, long templateOwnerId,
                                Map details, Boolean sshkeyEnabled,
                                Boolean isDynamicallyScalable, Boolean isRoutingType, boolean deployAsIs) {
        super(userId, name, displayText, bits, passwordEnabled, requiresHVM, isPublic, featured, isExtractable,
                format, guestOSId, zoneId, hypervisorType, chksum, templateTag, templateOwnerId, details,
                sshkeyEnabled, isDynamicallyScalable, isRoutingType, deployAsIs);
        setBootable(true);
    }
}
