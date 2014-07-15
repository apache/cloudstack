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

package com.cloud.storage.resource;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.storage.CopyVolumeAnswer;
import com.cloud.agent.api.storage.CopyVolumeCommand;
import com.cloud.agent.api.storage.DestroyCommand;
import com.cloud.agent.api.storage.PrimaryStorageDownloadAnswer;
import com.cloud.agent.api.storage.PrimaryStorageDownloadCommand;

public interface StoragePoolResource {
    // FIXME: Should have a PrimaryStorageDownloadAnswer
    PrimaryStorageDownloadAnswer execute(PrimaryStorageDownloadCommand cmd);

    // FIXME: Should have an DestroyAnswer
    Answer execute(DestroyCommand cmd);

    CopyVolumeAnswer execute(CopyVolumeCommand cmd);
}
