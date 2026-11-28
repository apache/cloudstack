/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.cloud.agent.api.storage;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;

import java.util.ArrayList;
import java.util.List;

/**
 * Answer to {@link MergeDiskOnlyVmSnapshotCommand}. Besides the overall result, it reports which volumes had their merge applied on the host, so that the
 * management server can update its metadata for exactly those volumes, even when the merge of other volumes failed or was aborted. The volumes not listed
 * were left with their backing chain unchanged.
 */
public class MergeDiskOnlyVmSnapshotAnswer extends Answer {

    private List<String> mergedVolumeUuids;

    public MergeDiskOnlyVmSnapshotAnswer(Command command, boolean success, String details, List<String> mergedVolumeUuids) {
        super(command, success, details);
        this.mergedVolumeUuids = mergedVolumeUuids == null ? new ArrayList<>() : mergedVolumeUuids;
    }

    public List<String> getMergedVolumeUuids() {
        return mergedVolumeUuids;
    }
}
