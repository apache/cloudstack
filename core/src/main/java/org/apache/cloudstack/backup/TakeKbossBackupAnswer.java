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
package org.apache.cloudstack.backup;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.utils.Pair;
import com.cloud.utils.exception.BackupException;

import java.util.Map;

public class TakeKbossBackupAnswer extends Answer {

    private Map<String, String> mapVolumeUuidToNewVolumePath;
    private Map<String, Pair<String, Long>> mapVolumeUuidToDeltaPathOnSecondaryAndSize;
    private boolean isVmConsistent = true;

    public TakeKbossBackupAnswer(Command command, boolean success, Map<String, String> mapVolumeUuidToNewVolumePath,
            Map<String, Pair<String, Long>> mapVolumeUuidToDeltaPathOnSecondaryAndSize) {
        super(command, success, null);
        this.mapVolumeUuidToNewVolumePath = mapVolumeUuidToNewVolumePath;
        this.mapVolumeUuidToDeltaPathOnSecondaryAndSize = mapVolumeUuidToDeltaPathOnSecondaryAndSize;
    }

    public TakeKbossBackupAnswer(Command command, Exception e) {
        super(command, e);
        if (e instanceof BackupException) {
            this.isVmConsistent = ((BackupException)e).isVmConsistent();
        }
    }

    public Map<String, String> getMapVolumeUuidToNewVolumePath() {
        return mapVolumeUuidToNewVolumePath;
    }

    public Map<String, Pair<String, Long>> getMapVolumeUuidToDeltaPathOnSecondaryAndSize() {
        return mapVolumeUuidToDeltaPathOnSecondaryAndSize;
    }

    public boolean isVmConsistent() {
        return isVmConsistent;
    }
}
