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
package org.apache.cloudstack.api.command.admin.volume;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ResponseObject.ResponseView;
import org.apache.cloudstack.api.command.admin.AdminCmd;
import org.apache.cloudstack.api.command.user.volume.MigrateVolumeCmd;
import org.apache.cloudstack.api.response.VolumeResponse;

import com.cloud.storage.Volume;

@APICommand(name = "migrateVolume", description = "Migrate volume", responseObject = VolumeResponse.class, since = "3.0.0", responseView = ResponseView.Full, entityType = {
        Volume.class}, requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class MigrateVolumeCmdByAdmin extends MigrateVolumeCmd implements AdminCmd {}
