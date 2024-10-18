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
package org.apache.cloudstack.acl;

import com.cloud.exception.InvalidParameterValueException;
import com.cloud.user.AccountManager;
import com.cloud.user.User;
import com.cloud.user.dao.UserDao;
import com.cloud.utils.component.ManagerBase;
import org.apache.cloudstack.acl.apikeypair.ApiKeyPair;
import org.apache.cloudstack.acl.apikeypair.ApiKeyPairPermission;
import org.apache.cloudstack.acl.apikeypair.ApiKeyPairService;
import org.apache.cloudstack.acl.dao.ApiKeyPairDao;
import org.apache.cloudstack.acl.dao.ApiKeyPairPermissionsDao;
import org.apache.cloudstack.acl.dao.RolePermissionsDao;
import org.apache.cloudstack.query.QueryService;
import org.apache.commons.collections.CollectionUtils;

import javax.inject.Inject;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class ApiKeyPairManagerImpl extends ManagerBase implements ApiKeyPairService {
    @Inject
    private ApiKeyPairDao apiKeyPairDao;
    @Inject
    private ApiKeyPairPermissionsDao apiKeyPairPermissionsDao;
    @Inject
    private UserDao userDao;
    @Inject
    private RoleService roleService;
    @Inject
    private QueryService queryService;
    @Inject
    private AccountManager accountManager;
    @Inject
    private RolePermissionsDao rolePermissionsDao;

    @Override
    public List<ApiKeyPairPermission> findAllPermissionsByKeyPairId(Long apiKeyPairId, Long roleId) {
        List<ApiKeyPairPermissionVO> allPermissions = apiKeyPairPermissionsDao.findAllByKeyPairIdSorted(apiKeyPairId);
        if (CollectionUtils.isNotEmpty(allPermissions)) {
            return allPermissions.stream().map(p -> (ApiKeyPairPermission) p).collect(Collectors.toList());
        }
        return rolePermissionsDao.findAllByRoleIdSorted(roleId).stream().map(p -> {
            ApiKeyPairPermissionVO permission = new ApiKeyPairPermissionVO();
            permission.setRule(p.getRule().getRuleString());
            permission.setDescription(p.getDescription());
            permission.setPermission(p.getPermission());
            return permission;
        }).collect(Collectors.toList());
    }

    @Override
    public ApiKeyPair findByApiKey(String apiKey) {
        return apiKeyPairDao.findByApiKey(apiKey);
    }

    @Override
    public ApiKeyPair findById(Long id) {
        return apiKeyPairDao.findById(id);
    }

    @Override
    public void validateCallingUserHasAccessToDesiredUser(Long userId) {
        List<Long> accessableUsers = queryService.searchForAccessableUsers();
        User desiredUser = userDao.getUser(userId);
        if (accessableUsers.stream().noneMatch(u -> Objects.equals(u, userId))) {
            throw new InvalidParameterValueException(String.format("Could not perform operation because calling user has less permissions " +
                    "than the informed user [%s].", desiredUser.getId()));
        }
    }
}
