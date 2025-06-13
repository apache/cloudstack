//
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
//

package org.apache.cloudstack.command;

import org.apache.cloudstack.storage.volume.VolumeOnStorageTO;

public class ReconcileCopyAnswer extends ReconcileVolumeAnswer {

    boolean isSkipped = false;
    String reason;

    public ReconcileCopyAnswer(boolean isSkipped, String reason) {
        super();
        this.isSkipped = isSkipped;
        this.reason = reason;
    }

    public ReconcileCopyAnswer(boolean isSkipped, boolean result, String reason) {
        super();
        this.isSkipped = isSkipped;
        this.result = result;
        this.reason = reason;
    }

    public ReconcileCopyAnswer(VolumeOnStorageTO volumeOnSource, VolumeOnStorageTO volumeOnDestination) {
        this.isSkipped = false;
        this.result = true;
        this.volumeOnSource = volumeOnSource;
        this.volumeOnDestination = volumeOnDestination;
    }

    public boolean isSkipped() {
        return isSkipped;
    }

    public String getReason() {
        return reason;
    }
}
