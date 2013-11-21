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
package com.cloud.uuididentity;

import javax.ejb.Local;
import javax.inject.Inject;

import org.springframework.stereotype.Component;

import org.apache.cloudstack.api.IdentityService;

import com.cloud.utils.component.ManagerBase;
import com.cloud.uuididentity.dao.IdentityDao;

@Component
@Local(value = {IdentityService.class})
public class IdentityServiceImpl extends ManagerBase implements IdentityService {
    @Inject
    private IdentityDao _identityDao;

    @Override
    public Long getIdentityId(String tableName, String identityString) {
        return _identityDao.getIdentityId(tableName, identityString);
    }

    @Override
    public String getIdentityUuid(String tableName, String identityString) {
        return _identityDao.getIdentityUuid(tableName, identityString);
    }
}
