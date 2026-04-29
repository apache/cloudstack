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

package org.apache.cloudstack.ca;

import java.util.Map;

import com.cloud.agent.api.LogLevel;
import com.cloud.agent.api.routing.NetworkElementCommand;
import com.cloud.utils.PasswordGenerator;

public class SetupKeyStoreCommand extends NetworkElementCommand {
    @LogLevel(LogLevel.Log4jLevel.Off)
    private int validityDays;
    @LogLevel(LogLevel.Log4jLevel.Off)
    private String keystorePassword;

    private boolean handleByAgent = true;

    public SetupKeyStoreCommand(final int validityDays) {
        super();
        setWait(60);
        this.validityDays = validityDays;
        if (this.validityDays < 1) {
            this.validityDays = 1;
        }
        this.keystorePassword = PasswordGenerator.generateRandomPassword(16);
    }

    @Override
    public void setAccessDetail(final Map<String, String> accessDetails) {
        handleByAgent = false;
        super.setAccessDetail(accessDetails);
    }


    @Override
    public void setAccessDetail(String name, String value) {
        handleByAgent = false;
        super.setAccessDetail(name, value);
    }

    public int getValidityDays() {
        return validityDays;
    }

    public String getKeystorePassword() {
        return keystorePassword;
    }

    public boolean isHandleByAgent() {
        return handleByAgent;
    }

    @Override
    public boolean executeInSequence() {
        return false;
    }
}
