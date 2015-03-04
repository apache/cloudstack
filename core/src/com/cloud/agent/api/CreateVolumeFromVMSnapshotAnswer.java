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

package com.cloud.agent.api;

import com.cloud.agent.api.to.VolumeTO;

public class CreateVolumeFromVMSnapshotAnswer extends Answer {
    private String path;
    private VolumeTO volumeTo;

    public VolumeTO getVolumeTo() {
        return volumeTo;
    }

    public CreateVolumeFromVMSnapshotAnswer(CreateVolumeFromVMSnapshotCommand cmd, VolumeTO volumeTo) {
        super(cmd, true, "");
        this.volumeTo = volumeTo;
    }

    public String getPath() {
        return path;
    }

    protected CreateVolumeFromVMSnapshotAnswer() {

    }

    public CreateVolumeFromVMSnapshotAnswer(CreateVolumeFromVMSnapshotCommand cmd, String path) {
        super(cmd, true, "");
        this.path = path;
    }

    public CreateVolumeFromVMSnapshotAnswer(CreateVolumeFromVMSnapshotCommand cmd, boolean result, String string) {
        super(cmd, result, string);
    }
}
