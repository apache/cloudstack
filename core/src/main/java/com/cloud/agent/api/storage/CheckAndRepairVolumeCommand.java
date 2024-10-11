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

package com.cloud.agent.api.storage;

import com.cloud.agent.api.Command;
import com.cloud.agent.api.LogLevel;
import com.cloud.agent.api.to.StorageFilerTO;

import java.util.Arrays;

public class CheckAndRepairVolumeCommand extends Command {
    private String path;
    private StorageFilerTO pool;
    private String repair;
    @LogLevel(LogLevel.Log4jLevel.Off)
    private byte[] passphrase;
    private String encryptFormat;

    public CheckAndRepairVolumeCommand(String path, StorageFilerTO pool, String repair, byte[] passphrase, String encryptFormat) {
        this.path = path;
        this.pool = pool;
        this.repair = repair;
        this.passphrase = passphrase;
        this.encryptFormat = encryptFormat;
    }

    public String getPath() {
        return path;
    }

    public String getPoolUuid() {
        return pool.getUuid();
    }

    public StorageFilerTO getPool() {
        return pool;
    }

    public String getRepair() {
        return repair;
    }

    public String getEncryptFormat() { return encryptFormat; }

    public byte[] getPassphrase() { return passphrase; }

    public void clearPassphrase() {
        if (this.passphrase != null) {
            Arrays.fill(this.passphrase, (byte) 0);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean executeInSequence() {
        return false;
    }
}
