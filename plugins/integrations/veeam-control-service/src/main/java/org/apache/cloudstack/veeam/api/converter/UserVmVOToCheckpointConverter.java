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

package org.apache.cloudstack.veeam.api.converter;

import java.time.Instant;

import org.apache.cloudstack.veeam.api.dto.Checkpoint;
import org.apache.commons.lang3.StringUtils;

import com.cloud.utils.NumbersUtil;

public class UserVmVOToCheckpointConverter {

    public static Checkpoint toCheckpoint(String checkpointId, String createTimeStr) {
        if (StringUtils.isEmpty(checkpointId)) {
            return null;
        }
        Checkpoint checkpoint = new Checkpoint();
        checkpoint.setId(checkpointId);
        checkpoint.setName(checkpointId);
        long createTimeSeconds = createTimeStr != null ? NumbersUtil.parseLong(createTimeStr, 0L) : 0L;
        if (createTimeSeconds > 0) {
            checkpoint.setCreationDate(String.valueOf(Instant.ofEpochSecond(createTimeSeconds).toEpochMilli()));
        } else {
            checkpoint.setCreationDate(String.valueOf(System.currentTimeMillis()));
        }
        checkpoint.setState("created");
        return checkpoint;
    }
}
