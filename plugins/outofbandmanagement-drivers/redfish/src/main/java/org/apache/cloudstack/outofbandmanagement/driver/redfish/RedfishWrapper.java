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
package org.apache.cloudstack.outofbandmanagement.driver.redfish;

import org.apache.cloudstack.outofbandmanagement.OutOfBandManagement;
import org.apache.cloudstack.utils.redfish.RedfishClient;

public class RedfishWrapper {

    public RedfishClient.RedfishResetCmd parsePowerCommand(OutOfBandManagement.PowerOperation operation) {
        if (operation == null) {
            throw new IllegalStateException("Invalid power operation requested");
        }
        switch (operation) {
        case ON:
            return RedfishClient.RedfishResetCmd.On;
        case OFF:
            return RedfishClient.RedfishResetCmd.GracefulShutdown;
        case CYCLE:
            return RedfishClient.RedfishResetCmd.PowerCycle;
        case RESET:
            return RedfishClient.RedfishResetCmd.ForceRestart;
        case SOFT:
            return RedfishClient.RedfishResetCmd.GracefulShutdown;
        case STATUS:
            throw new IllegalStateException(String.format("%s is not a valid Redfish Reset command [%s]", operation));
        default:
            throw new IllegalStateException(String.format("Redfish does not support operation [%s]", operation));
        }
    }

    public OutOfBandManagement.PowerState parseRedfishPowerStateToOutOfBand(RedfishClient.RedfishPowerState redfishPowerState) {
        if (redfishPowerState == null) {
            throw new IllegalStateException("Invalid power state [null]");
        }

        switch (redfishPowerState) {
        case On:
            return OutOfBandManagement.PowerState.On;
        case Off:
            return OutOfBandManagement.PowerState.Off;
        case PoweringOn:
            return OutOfBandManagement.PowerState.On;
        case PoweringOff:
            return OutOfBandManagement.PowerState.Off;
        default:
            return OutOfBandManagement.PowerState.Unknown;
        }
    }
}
