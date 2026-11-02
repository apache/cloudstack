//Licensed to the Apache Software Foundation (ASF) under one
//or more contributor license agreements.  See the NOTICE file
//distributed with this work for additional information
//regarding copyright ownership.  The ASF licenses this file
//to you under the Apache License, Version 2.0 (the
//"License"); you may not use this file except in compliance
//the License.  You may obtain a copy of the License at
//
//http://www.apache.org/licenses/LICENSE-2.0
//
//Unless required by applicable law or agreed to in writing,
//software distributed under the License is distributed on an
//"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
//KIND, either express or implied.  See the License for the
//specific language governing permissions and limitations
//under the License.

package com.cloud.hypervisor.kvm.resource.wrapper;

import org.apache.cloudstack.backup.StopBackupAnswer;
import org.apache.cloudstack.backup.StopBackupCommand;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.libvirt.Connect;
import org.libvirt.Domain;

import com.cloud.agent.api.Answer;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.hypervisor.kvm.resource.LibvirtConnection;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.cloud.utils.script.Script;

@ResourceWrapper(handles = StopBackupCommand.class)
public class LibvirtStopBackupCommandWrapper extends CommandWrapper<StopBackupCommand, Answer, LibvirtComputingResource> {
    protected Logger logger = LogManager.getLogger(getClass());

    @Override
    public Answer execute(StopBackupCommand cmd, LibvirtComputingResource resource) {
        String vmName = cmd.getVmName();

        try {
            Connect conn = LibvirtConnection.getConnection();
            Domain dm = conn.domainLookupByName(vmName);

            if (dm == null) {
                return new StopBackupAnswer(cmd, false, "Domain not found: " + vmName);
            }

            // Execute virsh domjobabort
            String abortCmd = String.format("virsh domjobabort %s", vmName);

            Script script = new Script("/bin/bash");
            script.add("-c");
            script.add(abortCmd);
            String result = script.execute();

            if (result != null && !result.isEmpty()) {
                // Job abort may fail if no job is running, which is acceptable
                logger.debug("domjobabort result: " + result);
            }

            return new StopBackupAnswer(cmd, true, "Backup stopped successfully");

        } catch (Exception e) {
            return new StopBackupAnswer(cmd, false, "Error stopping backup: " + e.getMessage());
        }
    }
}
