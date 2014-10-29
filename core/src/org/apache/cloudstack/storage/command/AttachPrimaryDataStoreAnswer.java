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

package org.apache.cloudstack.storage.command;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;

public class AttachPrimaryDataStoreAnswer extends Answer {
    private String uuid;
    private long capacity;
    private long avail;

    public AttachPrimaryDataStoreAnswer(Command cmd) {
        super(cmd);
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getUuid() {
        return uuid;
    }

    public void setCapacity(long capacity) {
        this.capacity = capacity;
    }

    public long getCapacity() {
        return capacity;
    }

    public void setAvailable(long avail) {
        this.avail = avail;
    }

    public long getAvailable() {
        return avail;
    }
}
