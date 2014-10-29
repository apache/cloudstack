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

package com.cloud.agent.api.proxy;

import com.cloud.agent.api.Command;
import com.cloud.agent.api.LogLevel;
import com.cloud.agent.api.LogLevel.Log4jLevel;

public class StartConsoleProxyAgentHttpHandlerCommand extends Command {
    @LogLevel(Log4jLevel.Off)
    private byte[] keystoreBits;
    @LogLevel(Log4jLevel.Off)
    private String keystorePassword;
    @LogLevel(Log4jLevel.Off)
    private String encryptorPassword;

    public StartConsoleProxyAgentHttpHandlerCommand() {
        super();
    }

    public StartConsoleProxyAgentHttpHandlerCommand(byte[] ksBits, String ksPassword) {
        this.keystoreBits = ksBits;
        this.keystorePassword = ksPassword;
    }

    @Override
    public boolean executeInSequence() {
        return true;
    }

    public byte[] getKeystoreBits() {
        return keystoreBits;
    }

    public void setKeystoreBits(byte[] keystoreBits) {
        this.keystoreBits = keystoreBits;
    }

    public String getKeystorePassword() {
        return keystorePassword;
    }

    public void setKeystorePassword(String keystorePassword) {
        this.keystorePassword = keystorePassword;
    }

    public String getEncryptorPassword() {
        return encryptorPassword;
    }

    public void setEncryptorPassword(String encryptorPassword) {
        this.encryptorPassword = encryptorPassword;
    }
}
