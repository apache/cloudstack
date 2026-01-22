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

import org.apache.cloudstack.backup.StopNBDServerCommand;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.cloud.agent.api.Answer;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.cloud.utils.script.Script;

@ResourceWrapper(handles = StopNBDServerCommand.class)
public class LibvirtStopNBDServerCommandWrapper extends CommandWrapper<StopNBDServerCommand, Answer, LibvirtComputingResource> {
    protected Logger logger = LogManager.getLogger(getClass());

    private void resetService(String unitName) {
        Script resetScript = new Script("/bin/bash", logger);
        resetScript.add("-c");
        resetScript.add(String.format("systemctl reset-failed %s || true", unitName));
        resetScript.execute();
    }

    private Answer handleUpload(StopNBDServerCommand cmd) {
        try {
            int nbdPort = cmd.getNbdPort();
            String unitName = String.format("qemu-nbd-%d", nbdPort);

            // Check if the service is running
            Script checkScript = new Script("/bin/bash", logger);
            checkScript.add("-c");
            checkScript.add(String.format("systemctl is-active --quiet %s", unitName));
            String checkResult = checkScript.execute();
            if (checkResult != null) {
                // Service is not running, but still reset-failed to clear any stale state
                logger.info(String.format("qemu-nbd service %s is not running, resetting failed state", unitName));
                resetService(unitName);
                return new Answer(cmd, true, "Image transfer finalized");
            }

            // Stop the systemd service
            Script stopScript = new Script("/bin/bash", logger);
            stopScript.add("-c");
            stopScript.add(String.format("systemctl stop %s", unitName));
            stopScript.execute();
            resetService(unitName);

            return new Answer(cmd, true, "Image transfer finalized");

        } catch (Exception e) {
            logger.error("Error finalizing image transfer for upload", e);
            return new Answer(cmd, false, "Error finalizing image transfer: " + e.getMessage());
        }
    }

    private Answer handleDownload(StopNBDServerCommand cmd) {
        return new Answer(cmd, true, "Image transfer finalized");
    }

    @Override
    public Answer execute(StopNBDServerCommand cmd, LibvirtComputingResource resource) {
        if (cmd.getDirection().equals("download")) {
            return handleDownload(cmd);
        } else {
            return handleUpload(cmd);
        }

    }
}
