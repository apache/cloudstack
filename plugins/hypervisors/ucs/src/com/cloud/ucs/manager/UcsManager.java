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
package com.cloud.ucs.manager;

import org.apache.cloudstack.api.AddUcsManagerCmd;
import org.apache.cloudstack.api.AssociateUcsProfileToBladeCmd;
import org.apache.cloudstack.api.ListUcsBladeCmd;
import org.apache.cloudstack.api.ListUcsManagerCmd;
import org.apache.cloudstack.api.ListUcsProfileCmd;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.UcsBladeResponse;
import org.apache.cloudstack.api.response.UcsManagerResponse;
import org.apache.cloudstack.api.response.UcsProfileResponse;

import com.cloud.utils.component.Manager;
import com.cloud.utils.component.PluggableService;

public interface UcsManager extends Manager, PluggableService {
    UcsManagerResponse addUcsManager(AddUcsManagerCmd cmd);

    ListResponse<UcsProfileResponse> listUcsProfiles(ListUcsProfileCmd cmd);

    ListResponse<UcsManagerResponse> listUcsManager(ListUcsManagerCmd cmd);

    UcsBladeResponse associateProfileToBlade(AssociateUcsProfileToBladeCmd cmd);

    ListResponse<UcsBladeResponse> listUcsBlades(ListUcsBladeCmd cmd);

    void deleteUcsManager(Long id);
}
