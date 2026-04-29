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
package com.cloud.agent.api;

public class CheckConvertInstanceCommand extends Command {
    boolean checkWindowsGuestConversionSupport = false;
    boolean useVddk = false;
    String vddkLibDir;

    public CheckConvertInstanceCommand() {
    }

    public CheckConvertInstanceCommand(boolean checkWindowsGuestConversionSupport) {
        this.checkWindowsGuestConversionSupport = checkWindowsGuestConversionSupport;
    }

    public CheckConvertInstanceCommand(boolean checkWindowsGuestConversionSupport, boolean useVddk) {
        this.checkWindowsGuestConversionSupport = checkWindowsGuestConversionSupport;
        this.useVddk = useVddk;
    }

    @Override
    public boolean executeInSequence() {
        return false;
    }

    public boolean getCheckWindowsGuestConversionSupport() {
        return checkWindowsGuestConversionSupport;
    }

    public boolean isUseVddk() {
        return useVddk;
    }

    public void setUseVddk(boolean useVddk) {
        this.useVddk = useVddk;
    }

    public String getVddkLibDir() {
        return vddkLibDir;
    }

    public void setVddkLibDir(String vddkLibDir) {
        this.vddkLibDir = vddkLibDir;
    }
}
