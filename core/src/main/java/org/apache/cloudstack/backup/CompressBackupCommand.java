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

import com.cloud.agent.api.Command;
import org.apache.cloudstack.storage.to.DeltaMergeTreeTO;

import java.util.List;

public class CompressBackupCommand extends Command {

    private List<DeltaMergeTreeTO> backupDeltasToCompress;

    private List<String> backupChainImageStoreUrls;

    private long minFreeStorage;

    private Backup.CompressionLibrary compressionLib;

    private int coroutines;

    private int rateLimit;

    public CompressBackupCommand(List<DeltaMergeTreeTO> backupDeltasToCompress, List<String> backupChainImageStoreUrls, long minFreeStorage, Backup.CompressionLibrary compressionLib, int coroutines, int rateLimit) {
        this.backupChainImageStoreUrls = backupChainImageStoreUrls;
        this.backupDeltasToCompress = backupDeltasToCompress;
        this.minFreeStorage = minFreeStorage;
        this.compressionLib = compressionLib;
        this.coroutines = coroutines;
        this.rateLimit = rateLimit;
    }

    public List<DeltaMergeTreeTO> getBackupDeltasToCompress() {
        return backupDeltasToCompress;
    }

    public List<String> getBackupChainImageStoreUrls() {
        return backupChainImageStoreUrls;
    }

    public long getMinFreeStorage() {
        return minFreeStorage;
    }

    public Backup.CompressionLibrary getCompressionLib() {
        return compressionLib;
    }

    public int getCoroutines() {
        return coroutines;
    }

    public int getRateLimit() {
        return rateLimit;
    }

    @Override
    public boolean executeInSequence() {
        return false;
    }
}
