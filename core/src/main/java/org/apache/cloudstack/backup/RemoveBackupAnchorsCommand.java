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

import java.util.List;

import org.apache.cloudstack.storage.to.PrimaryDataStoreTO;

import com.cloud.agent.api.Command;

/**
 * Remove the backup anchors of an Instance's volumes: the RBD snapshots an incremental chain is
 * exported from. An anchor is only kept to export the next incremental from it, so once the chain
 * is gone the next backup is a full one and the anchor would only keep the data it references
 * allocated in the pool.
 */
public class RemoveBackupAnchorsCommand extends Command {
    private List<PrimaryDataStoreTO> volumePools;
    private List<String> volumePaths;

    public RemoveBackupAnchorsCommand(List<PrimaryDataStoreTO> volumePools, List<String> volumePaths) {
        super();
        this.volumePools = volumePools;
        this.volumePaths = volumePaths;
    }

    public List<PrimaryDataStoreTO> getVolumePools() {
        return volumePools;
    }

    public void setVolumePools(List<PrimaryDataStoreTO> volumePools) {
        this.volumePools = volumePools;
    }

    public List<String> getVolumePaths() {
        return volumePaths;
    }

    public void setVolumePaths(List<String> volumePaths) {
        this.volumePaths = volumePaths;
    }

    @Override
    public boolean executeInSequence() {
        return true;
    }
}
