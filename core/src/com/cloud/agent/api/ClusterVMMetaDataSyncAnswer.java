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

import java.util.HashMap;

public class ClusterVMMetaDataSyncAnswer extends Answer {
    private long _clusterId;
    private HashMap<String, String> _vmMetaDatum;
    private boolean _isExecuted=false;

    // this is here because a cron command answer is being sent twice
    //  AgentAttache.processAnswers
    //  AgentManagerImpl.notifyAnswersToMonitors
    public boolean isExecuted(){
        return _isExecuted;
    }

    public void setExecuted(){
        _isExecuted = true;
    }


    public ClusterVMMetaDataSyncAnswer(long clusterId, HashMap<String, String> vmMetaDatum){
        _clusterId = clusterId;
        _vmMetaDatum = vmMetaDatum;
        result = true;
    }

    public long getClusterId() {
        return _clusterId;
    }

    public HashMap<String, String> getVMMetaDatum() {
        return _vmMetaDatum;
    }

}
