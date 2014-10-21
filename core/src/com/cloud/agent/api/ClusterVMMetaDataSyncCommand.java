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


public class ClusterVMMetaDataSyncCommand extends Command implements CronCommand {
    int _interval;

    long _clusterId;

    public ClusterVMMetaDataSyncCommand() {
    }

    public ClusterVMMetaDataSyncCommand(int interval, long clusterId){
        _interval = interval;
        _clusterId = clusterId;
    }

    @Override
    public int getInterval() {
        return _interval;
    }

    public long getClusterId() {
        return _clusterId;
    }

    @Override
    public boolean executeInSequence() {
        return false;
    }

}
