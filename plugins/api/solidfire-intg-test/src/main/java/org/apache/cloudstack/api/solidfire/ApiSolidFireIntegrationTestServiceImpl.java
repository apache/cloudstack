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
package org.apache.cloudstack.api.solidfire;

import java.util.List;
import java.util.ArrayList;

import org.apache.cloudstack.api.command.admin.solidfire.GetPathForVolumeCmd;
import org.apache.cloudstack.api.command.admin.solidfire.GetSolidFireAccountIdCmd;
import org.apache.cloudstack.api.command.admin.solidfire.GetSolidFireVolumeAccessGroupIdsCmd;
import org.apache.cloudstack.api.command.admin.solidfire.GetVolumeSnapshotDetailsCmd;
import org.apache.cloudstack.api.command.admin.solidfire.GetVolumeiScsiNameCmd;
import org.apache.cloudstack.api.command.admin.solidfire.GetSolidFireVolumeSizeCmd;
import org.springframework.stereotype.Component;

import com.cloud.utils.component.AdapterBase;

@Component
public class ApiSolidFireIntegrationTestServiceImpl extends AdapterBase implements ApiSolidFireIntegrationTestService {
    @Override
    public List<Class<?>> getCommands() {
        List<Class<?>> cmdList = new ArrayList<Class<?>>();

        cmdList.add(GetPathForVolumeCmd.class);
        cmdList.add(GetSolidFireAccountIdCmd.class);
        cmdList.add(GetSolidFireVolumeAccessGroupIdsCmd.class);
        cmdList.add(GetVolumeiScsiNameCmd.class);
        cmdList.add(GetSolidFireVolumeSizeCmd.class);
        cmdList.add(GetVolumeSnapshotDetailsCmd.class);

        return cmdList;
    }
}
