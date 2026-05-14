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
package org.apache.cloudstack.vm;

import org.apache.cloudstack.api.command.admin.vm.CancelVmwareCbtMigrationCmd;
import org.apache.cloudstack.api.command.admin.vm.CutoverVmwareCbtMigrationCmd;
import org.apache.cloudstack.api.command.admin.vm.ListVmwareCbtMigrationsCmd;
import org.apache.cloudstack.api.command.admin.vm.RegisterVmwareCbtMigrationTargetCmd;
import org.apache.cloudstack.api.command.admin.vm.StartVmwareCbtMigrationCmd;
import org.apache.cloudstack.api.command.admin.vm.SyncVmwareCbtMigrationCmd;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.VmwareCbtMigrationResponse;

import com.cloud.utils.component.PluggableService;

public interface VmwareCbtMigrationManager extends PluggableService {
    VmwareCbtMigrationResponse startVmwareCbtMigration(StartVmwareCbtMigrationCmd cmd);

    ListResponse<VmwareCbtMigrationResponse> listVmwareCbtMigrations(ListVmwareCbtMigrationsCmd cmd);

    VmwareCbtMigrationResponse syncVmwareCbtMigration(SyncVmwareCbtMigrationCmd cmd);

    VmwareCbtMigrationResponse registerVmwareCbtMigrationTarget(RegisterVmwareCbtMigrationTargetCmd cmd);

    VmwareCbtMigrationResponse cutoverVmwareCbtMigration(CutoverVmwareCbtMigrationCmd cmd);

    VmwareCbtMigrationResponse cancelVmwareCbtMigration(CancelVmwareCbtMigrationCmd cmd);
}
