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
package com.cloud.storage.secondary;

import com.cloud.utils.events.EventArgs;
import com.cloud.vm.SecondaryStorageVmVO;

public class SecStorageVmAlertEventArgs extends EventArgs {

    private static final long serialVersionUID = 23773987551479885L;

    public static final int SSVM_CREATED = 1;
    public static final int SSVM_UP = 2;
    public static final int SSVM_DOWN = 3;
    public static final int SSVM_CREATE_FAILURE = 4;
    public static final int SSVM_START_FAILURE = 5;
    public static final int SSVM_FIREWALL_ALERT = 6;
    public static final int SSVM_STORAGE_ALERT = 7;
    public static final int SSVM_REBOOTED = 8;

    public static final String ALERT_SUBJECT = "ssvm-alert";

    private int type;
    private long zoneId;
    private long ssVmId;
    private SecondaryStorageVmVO ssVm;
    private String message;

    public SecStorageVmAlertEventArgs(int type, long zoneId, long ssVmId, SecondaryStorageVmVO ssVm, String message) {

        super(ALERT_SUBJECT);
        this.type = type;
        this.zoneId = zoneId;
        this.ssVmId = ssVmId;
        this.ssVm = ssVm;
        this.message = message;
    }

    public int getType() {
        return type;
    }

    public long getZoneId() {
        return zoneId;
    }

    public long getSecStorageVmId() {
        return ssVmId;
    }

    public SecondaryStorageVmVO getSecStorageVm() {
        return ssVm;
    }

    public String getMessage() {
        return message;
    }
}
