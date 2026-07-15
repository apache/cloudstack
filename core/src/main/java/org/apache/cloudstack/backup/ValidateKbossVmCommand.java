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

package org.apache.cloudstack.backup;

import com.cloud.agent.api.Command;
import com.cloud.agent.api.to.VirtualMachineTO;
import org.apache.cloudstack.storage.to.BackupDeltaTO;

public class ValidateKbossVmCommand extends Command {

    private VirtualMachineTO vm;
    private BackupDeltaTO backupDeltaTO;

    private String scriptToExecute;

    private String scriptArguments;
    private String expectedResult;

    private Integer scriptTimeout;
    private Integer bootTimeout;
    private Integer screenshotWait;

    private boolean takeScreenshot;
    private boolean waitForBoot;
    private boolean executeScript;

    public ValidateKbossVmCommand(VirtualMachineTO vm, BackupDeltaTO backupDeltaTO) {
        this.vm = vm;
        this.backupDeltaTO = backupDeltaTO;
    }

    public void setScriptToExecute(String scriptToExecute) {
        this.scriptToExecute = scriptToExecute;
    }

    public void setScriptArguments(String scriptArguments) {
        this.scriptArguments = scriptArguments;
    }

    public void setExpectedResult(String expectedResult) {
        this.expectedResult = expectedResult;
    }

    public void setScriptTimeout(Integer scriptTimeout) {
        this.scriptTimeout = scriptTimeout;
    }

    public void setTakeScreenshot(boolean takeScreenshot) {
        this.takeScreenshot = takeScreenshot;
    }

    public void setWaitForBoot(boolean waitForBoot) {
        this.waitForBoot = waitForBoot;
    }

    public void setExecuteScript(boolean executeScript) {
        this.executeScript = executeScript;
    }

    public void setBootTimeout(Integer bootTimeout) {
        this.bootTimeout = bootTimeout;
    }

    public void setScreenshotWait(Integer screenshotWait) {
        this.screenshotWait = screenshotWait;
    }

    public VirtualMachineTO getVm() {
        return vm;
    }

    public BackupDeltaTO getBackupDeltaTO() {
        return backupDeltaTO;
    }

    public String getScriptToExecute() {
        return scriptToExecute;
    }

    public String getScriptArguments() {
        return scriptArguments;
    }

    public String getExpectedResult() {
        return expectedResult;
    }

    public Integer getScriptTimeout() {
        return scriptTimeout;
    }

    public Integer getBootTimeout() {
        return bootTimeout;
    }

    public Integer getScreenshotWait() {
        return screenshotWait;
    }

    public boolean isTakeScreenshot() {
        return takeScreenshot;
    }

    public boolean isWaitForBoot() {
        return waitForBoot;
    }

    public boolean isExecuteScript() {
        return executeScript;
    }

    @Override
    public boolean executeInSequence() {
        return false;
    }
}
