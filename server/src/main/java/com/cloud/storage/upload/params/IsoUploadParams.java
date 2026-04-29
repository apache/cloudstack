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
import com.cloud.storage.Storage;

public class IsoUploadParams extends UploadParamsBase {

    public IsoUploadParams(long userId, String name, String displayText, Boolean isPublic, Boolean isFeatured,
                           Boolean isExtractable, Long osTypeId, Long zoneId, Boolean bootable, long ownerId) {
        super(userId, name, displayText, isPublic, isFeatured, isExtractable, osTypeId, zoneId, bootable, ownerId);
        setIso(true);
        setBits(64);
        setFormat(Storage.ImageFormat.ISO.toString());
        setHypervisorType(Hypervisor.HypervisorType.None);
        setRequiresHVM(true);
    }
}
