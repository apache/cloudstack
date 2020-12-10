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

import com.cloud.agent.api.to.HostTO;
import com.cloud.agent.api.to.StorageFilerTO;
import com.cloud.host.Host;
import com.cloud.storage.StoragePool;
import com.cloud.storage.Volume;

import org.joda.time.DateTime;
import java.util.List;

public final class CheckVMActivityOnStoragePoolCommand extends Command {

    private HostTO host;
    private StorageFilerTO pool;
    private String volumeList;
    private long suspectTimeSeconds;

    public CheckVMActivityOnStoragePoolCommand(final Host host, final StoragePool pool, final List<Volume> volumeList, final DateTime suspectTime) {
        this.host = new HostTO(host);
        this.pool = new StorageFilerTO(pool);
        this.suspectTimeSeconds = suspectTime.getMillis()/1000L;
        final StringBuilder stringBuilder = new StringBuilder();
        for (final Volume v : volumeList) {
            stringBuilder.append(v.getPath()).append(",");
        }

        this.volumeList = stringBuilder.deleteCharAt(stringBuilder.length() - 1).toString();
    }

    public String getVolumeList() {
        return volumeList;
    }

    public StorageFilerTO getPool() {
        return pool;
    }

    public HostTO getHost() {
        return host;
    }

    public long getSuspectTimeInSeconds() {
        return suspectTimeSeconds;
    }

    @Override
    public boolean executeInSequence() {
        return false;
    }
}
