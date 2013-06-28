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
package com.cloud.agent.api.to;

import com.cloud.storage.Volume;

public class DiskTO {
    private DataTO data;
    private Long diskSeq;
    private String vdiUuid;
    private Volume.Type type;
    public DiskTO() {
        
    }
    
    public DiskTO(DataTO data, Long diskSeq, String vdiUuid, Volume.Type type) {
        this.data = data;
        this.diskSeq = diskSeq;
        this.vdiUuid = vdiUuid;
        this.type = type;
    }

    public DataTO getData() {
        return data;
    }

    public void setData(DataTO data) {
        this.data = data;
    }

    public Long getDiskSeq() {
        return diskSeq;
    }

    public void setDiskSeq(Long diskSeq) {
        this.diskSeq = diskSeq;
    }

    public String getVdiUuid() {
        return vdiUuid;
    }

    public void setVdiUuid(String vdiUuid) {
        this.vdiUuid = vdiUuid;
    }

    public Volume.Type getType() {
        return type;
    }

    public void setType(Volume.Type type) {
        this.type = type;
    }
}
