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
package com.cloud.vm;

import com.cloud.agent.api.Answer;
import com.cloud.agent.manager.Commands;
import com.cloud.deploy.DeployDestination;
import com.cloud.exception.ResourceUnavailableException;

/**
 * A VirtualMachineGuru knows how to process a certain type of virtual machine.
 *
 */
public interface VirtualMachineGuru {

    boolean finalizeVirtualMachineProfile(VirtualMachineProfile profile, DeployDestination dest, ReservationContext context);

    /**
     * finalize the virtual machine deployment.
     * @param cmds commands that were created.
     * @param profile virtual machine profile.
     * @param dest destination to send the command.
     * @return true if everything checks out.  false if not and we should try again.
     */
    boolean finalizeDeployment(Commands cmds, VirtualMachineProfile profile, DeployDestination dest, ReservationContext context) throws ResourceUnavailableException;

    /**
     * Check the deployment results.
     * @param cmds commands and answers that were sent.
     * @param profile virtual machine profile.
     * @param dest destination it was sent to.
     * @return true if deployment was fine; false if it didn't go well.
     */
    boolean finalizeStart(VirtualMachineProfile profile, long hostId, Commands cmds, ReservationContext context);

    boolean finalizeCommandsOnStart(Commands cmds, VirtualMachineProfile profile);

    void finalizeStop(VirtualMachineProfile profile, Answer answer);

    void finalizeExpunge(VirtualMachine vm);

    /**
     * Prepare Vm for Stop
     * @param profile
     * @return
     */
    void prepareStop(VirtualMachineProfile profile);
}
