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
package com.cloud.agent.api;

import java.util.Map;
import com.cloud.agent.api.to.VolumeTO;
import com.cloud.agent.api.to.NicTO;

public class MigrateWithStorageReceiveAnswer extends Answer {

    Map<VolumeTO, Object> volumeToSr;
    Map<NicTO, Object> nicToNetwork;
    Map<String, String> token;

    public MigrateWithStorageReceiveAnswer(MigrateWithStorageReceiveCommand cmd, Exception ex) {
        super(cmd, ex);
        volumeToSr = null;
        nicToNetwork = null;
        token = null;
    }

    public MigrateWithStorageReceiveAnswer(MigrateWithStorageReceiveCommand cmd, Map<VolumeTO, Object> volumeToSr,
            Map<NicTO, Object> nicToNetwork, Map<String, String> token) {
        super(cmd, true, null);
        this.volumeToSr = volumeToSr;
        this.nicToNetwork = nicToNetwork;
        this.token = token;
    }

    public Map<VolumeTO, Object> getVolumeToSr() {
        return volumeToSr;
    }

    public Map<NicTO, Object> getNicToNetwork() {
        return nicToNetwork;
    }

    public Map<String, String> getToken() {
        return token;
    }
}
