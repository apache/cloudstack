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

public class VmWorkMigrateAway extends VmWork {

    private static final long serialVersionUID = -5917512239025814373L;

    private long srcHostId;

    public VmWorkMigrateAway(long userId, long accountId, long vmId, String handlerName,
            long srcHostId) {
        super(userId, accountId, vmId, handlerName);

        this.srcHostId = srcHostId;
    }

    public VmWorkMigrateAway(VmWork vmWork, long srcHostId) {
        super(vmWork);
        this.srcHostId = srcHostId;
    }

    public long getSrcHostId() {
        return srcHostId;
    }
}
