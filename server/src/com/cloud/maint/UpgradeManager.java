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
package com.cloud.maint;

import com.cloud.utils.component.Manager;

/**
 * Upgrade Manager manages the upgrade of agents.
 *
 */
public interface UpgradeManager extends Manager {
    enum State {
        RequiresUpdate,
        WaitingForUpdate,
        UpToDate;
    };
    
    /**
     * Checks if the agent requires an upgrade before it can process
     * any commands.
     * 
     * @param hostId host id.
     * @return state of the agent.
     */
    State registerForUpgrade(long hostId, String version);
    
    /**
     * @return the URL to download the new agent.
     */
//    String getAgentUrl();
    

}
