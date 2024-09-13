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
package com.cloud.vm.dao;


import org.springframework.stereotype.Component;

import org.apache.cloudstack.resourcedetail.ResourceDetailsDaoBase;

import com.cloud.vm.UserVmDetailVO;
import com.cloud.vm.VMInstanceVO;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class UserVmDetailsDaoImpl extends ResourceDetailsDaoBase<UserVmDetailVO> implements UserVmDetailsDao {

    @Override
    public void addDetail(long resourceId, String key, String value, boolean display) {
        super.addDetail(new UserVmDetailVO(resourceId, key, value, display));
    }

    @Override
    public void saveDetails(VMInstanceVO vm) {
        saveDetails(vm, new ArrayList<String>());
    }

    @Override
    public void saveDetails(VMInstanceVO vm, List<String> hiddenDetails) {
        Map<String, String> detailsStr = vm.getDetails();
        if (detailsStr == null) {
            return;
        }

        final Map<String, Boolean> visibilityMap = listDetailsVisibility(vm.getId());

        List<UserVmDetailVO> details = new ArrayList<UserVmDetailVO>();
        for (Map.Entry<String, String> entry : detailsStr.entrySet()) {
            boolean display = !hiddenDetails.contains(entry.getKey()) && visibilityMap.getOrDefault(entry.getKey(), true);
            details.add(new UserVmDetailVO(vm.getId(), entry.getKey(), entry.getValue(), display));
        }

        saveDetails(details);
    }
}
