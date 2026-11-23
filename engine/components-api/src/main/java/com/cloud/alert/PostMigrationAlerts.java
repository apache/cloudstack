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
package com.cloud.alert;

import com.cloud.vm.VirtualMachine;

public final class PostMigrationAlerts {

    private PostMigrationAlerts() {
    }

    public static void sendFailureAlert(AlertManager alertMgr, VirtualMachine.Type vmType, long zoneId, Long podId, String vmName, String destHostUuid, String details) {
        AlertManager.AlertType alertType = AlertManager.AlertType.ALERT_TYPE_USERVM_MIGRATE;
        if (VirtualMachine.Type.DomainRouter.equals(vmType)) {
            alertType = AlertManager.AlertType.ALERT_TYPE_DOMAIN_ROUTER_MIGRATE;
        } else if (VirtualMachine.Type.ConsoleProxy.equals(vmType)) {
            alertType = AlertManager.AlertType.ALERT_TYPE_CONSOLE_PROXY_MIGRATE;
        }
        alertMgr.sendAlert(alertType, zoneId, podId,
                String.format("Post-migration tasks failed for VM %s on destination host %s.", vmName, destHostUuid),
                String.format("Post-migration tasks failed: %s. VM network/storage configuration on the destination may be incomplete. "
                        + "If a VLAN nic of the Instance could not get its VLAN membership, its link is set down; restart the Instance to apply it.", details));
    }
}
