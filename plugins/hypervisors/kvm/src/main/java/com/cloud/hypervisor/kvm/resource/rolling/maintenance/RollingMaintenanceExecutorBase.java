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
package com.cloud.hypervisor.kvm.resource.rolling.maintenance;

import com.cloud.utils.exception.CloudRuntimeException;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import java.io.File;

public abstract class RollingMaintenanceExecutorBase implements RollingMaintenanceExecutor {

    private String hooksDir;
    private int timeout;
    private boolean avoidMaintenance = false;

    static final int exitValueAvoidMaintenance = 70;
    static final int exitValueTerminatedSignal = 143;
    private static final Logger s_logger = Logger.getLogger(RollingMaintenanceExecutor.class);

    void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    long getTimeout() {
        return timeout;
    }

    private void sanitizeHoooksDirFormat() {
        if (StringUtils.isNotBlank(this.hooksDir) && !this.hooksDir.endsWith("/")) {
            this.hooksDir += "/";
        }
    }

    RollingMaintenanceExecutorBase(String hooksDir) {
        this.hooksDir = hooksDir;
        sanitizeHoooksDirFormat();
    }

    protected boolean existsAndIsFile(String filepath) {
        File file = new File(filepath);
        return file.exists() && file.isFile();
    }

    public File getStageScriptFile(String stage) {
        String scriptPath = hooksDir + stage;
        if (existsAndIsFile(scriptPath)) {
            return new File(scriptPath);
        } else if (existsAndIsFile(scriptPath + ".sh")) {
            return new File(scriptPath + ".sh");
        } else if (existsAndIsFile(scriptPath + ".py")) {
            return new File(scriptPath + ".py");
        } else {
            String msg = "Unable to locate script for stage: " + stage + " in directory: " + hooksDir;
            s_logger.warn(msg);
            return null;
        }
    }

    void checkHooksDirectory() {
        if (StringUtils.isBlank(hooksDir)) {
            throw new CloudRuntimeException("Hooks directory is empty, please specify it on agent.properties and restart the agent");
        }
    }

    String getHooksDir() {
        return hooksDir;
    }

    public void setAvoidMaintenance(boolean avoidMaintenance) {
        this.avoidMaintenance = avoidMaintenance;
    }

    public boolean getStageAvoidMaintenance(String stage, File scriptFile) {
        return avoidMaintenance;
    }
}
