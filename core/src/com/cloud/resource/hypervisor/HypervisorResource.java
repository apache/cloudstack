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

package com.cloud.resource.hypervisor;

import com.cloud.agent.api.RebootAnswer;
import com.cloud.agent.api.RebootCommand;
import com.cloud.agent.api.StartAnswer;
import com.cloud.agent.api.StartCommand;
import com.cloud.agent.api.StopAnswer;
import com.cloud.agent.api.StopCommand;
import com.cloud.resource.ServerResource;

/**
 * HypervisorResource specifies all of the commands a hypervisor agent needs
 *
 */
public interface HypervisorResource extends ServerResource {
    /**
     * Starts a VM.  All information regarding the VM
     * are carried within the command.
     * @param cmd carries all the information necessary to start a VM
     * @return Start2Answer answer.
     */
    StartAnswer execute(StartCommand cmd);

    /**
     * Stops a VM.  Must return true as long as the VM does not exist.
     * @param cmd information necessary to identify the VM to stop.
     * @return StopAnswer
     */
    StopAnswer execute(StopCommand cmd);

    /**
     * Reboots a VM.
     * @param cmd information necessary to identify the VM to reboot.
     * @return RebootAnswer
     */
    RebootAnswer execute(RebootCommand cmd);
}
