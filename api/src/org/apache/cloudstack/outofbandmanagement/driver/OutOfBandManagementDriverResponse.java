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
package org.apache.cloudstack.outofbandmanagement.driver;

import org.apache.cloudstack.outofbandmanagement.OutOfBandManagement;

public class OutOfBandManagementDriverResponse {
    private String result;
    private String error;
    private boolean success = false;
    private boolean hasAuthFailure = false;
    private OutOfBandManagement.PowerState powerState;

    public OutOfBandManagementDriverResponse(String result, String error, boolean success) {
        this.result = result;
        this.error = error;
        this.success = success;
    }

    public OutOfBandManagement.PowerState.Event toEvent() {
        if (hasAuthFailure()) {
            return OutOfBandManagement.PowerState.Event.AuthError;
        }

        if (!isSuccess() || powerState == null) {
            return OutOfBandManagement.PowerState.Event.Unknown;
        }

        return powerState.toEvent();
    }

    public boolean isSuccess() {
        return success;
    }

    public String getResult() {
        return result;
    }

    public String getError() {
        return error;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public void setError(String error) {
        this.error = error;
    }

    public OutOfBandManagement.PowerState getPowerState() {
        return powerState;
    }

    public void setPowerState(OutOfBandManagement.PowerState powerState) {
        this.powerState = powerState;
    }

    public boolean hasAuthFailure() {
        return hasAuthFailure;
    }

    public void setAuthFailure(boolean hasAuthFailure) {
        this.hasAuthFailure = hasAuthFailure;
    }
}
