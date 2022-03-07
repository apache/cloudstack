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
package com.cloud.vm;

import com.cloud.deploy.DeployDestination;

public class VmWorkMigrateForScale extends VmWorkMigrate {
    private static final long serialVersionUID = 6854870395568389613L;

    Long newSvcOfferingId;

    public VmWorkMigrateForScale(long userId, long accountId, long vmId, String handlerName, long srcHostId,
            DeployDestination dest, Long newSvcOfferingId) {

        super(userId, accountId, vmId, handlerName, srcHostId, dest);
        this.newSvcOfferingId = newSvcOfferingId;
    }

    public VmWorkMigrateForScale(VmWork vmWork, long srcHostId, DeployDestination dest, Long newSvcOfferingId) {
        super(vmWork, srcHostId, dest);
        this.newSvcOfferingId = newSvcOfferingId;
    }

    public Long getNewServiceOfferringId() {
        return newSvcOfferingId;
    }
}
