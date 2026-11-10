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

public class VmWorkStop extends VmWork {
    private static final long serialVersionUID = 202908740486785251L;

    private final boolean cleanup;

    // With cleanup, release the resources without the host's answer only when the host is gone. Absent from jobs
    // queued before this field existed, which then read false and keep the previous behaviour.
    private final boolean releaseOnlyIfHostIsGone;

    public VmWorkStop(long userId, long accountId, long vmId, String handlerName, boolean cleanup) {
        super(userId, accountId, vmId, handlerName);
        this.cleanup = cleanup;
        this.releaseOnlyIfHostIsGone = false;
    }

    public VmWorkStop(VmWork vmWork, boolean cleanup) {
        this(vmWork, cleanup, false);
    }

    public VmWorkStop(VmWork vmWork, boolean cleanup, boolean releaseOnlyIfHostIsGone) {
        super(vmWork);
        this.cleanup = cleanup;
        this.releaseOnlyIfHostIsGone = releaseOnlyIfHostIsGone;
    }

    public boolean isCleanup() {
        return cleanup;
    }

    public boolean isReleaseOnlyIfHostIsGone() {
        return releaseOnlyIfHostIsGone;
    }
}
