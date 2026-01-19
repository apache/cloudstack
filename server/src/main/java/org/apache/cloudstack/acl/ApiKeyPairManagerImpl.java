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

import com.cloud.utils.component.ManagerBase;

import java.util.Map;
import java.util.stream.Collectors;
import org.apache.cloudstack.acl.apikeypair.ApiKeyPairService;
import org.apache.cloudstack.acl.apikeypair.ApiKeyPair;
import org.apache.cloudstack.acl.apikeypair.ApiKeyPairPermission;
import org.apache.cloudstack.acl.dao.ApiKeyPairDao;
import org.apache.cloudstack.acl.dao.ApiKeyPairPermissionsDao;
import org.apache.commons.collections.CollectionUtils;

import javax.inject.Inject;
import java.util.List;

public class ApiKeyPairManagerImpl extends ManagerBase implements ApiKeyPairService {
    @Inject
    private ApiKeyPairDao apiKeyPairDao;
    @Inject
    private ApiKeyPairPermissionsDao apiKeyPairPermissionsDao;
    @Inject
    private RoleService roleService;

    @Override
    public List<ApiKeyPairPermission> findAllPermissionsByKeyPairId(Long apiKeyPairId, Long roleId) {
        List<ApiKeyPairPermissionVO> allPermissions = apiKeyPairPermissionsDao.findAllByKeyPairIdSorted(apiKeyPairId);
        List<RolePermissionEntity> rolePermissionsEntity = roleService.findAllRolePermissionsEntityBy(roleId);

        if (!CollectionUtils.isEmpty(allPermissions)) {
            List<RolePermissionEntity> keyPairPermissionsEntity = allPermissions.stream()
                    .map(p -> (RolePermissionEntity) p).collect(Collectors.toList());

            Map<String, RolePermissionEntity.Permission> rolePermissionInfo = roleService.getRoleRulesAndPermissions(rolePermissionsEntity);

            if (roleService.roleHasPermission(rolePermissionInfo, keyPairPermissionsEntity)) {
                return allPermissions.stream().map(p -> (ApiKeyPairPermission) p).collect(Collectors.toList());
            }

            Map<String, RolePermissionEntity.Permission> keyPairPermissionInfo = roleService.getRoleRulesAndPermissions(keyPairPermissionsEntity);
            if (!roleService.roleHasPermission(keyPairPermissionInfo, rolePermissionsEntity)) {
                for (RolePermissionEntity rolePermission : keyPairPermissionsEntity) {
                    if (rolePermission.getPermission() == RolePermissionEntity.Permission.DENY && !rolePermissionsEntity.contains(rolePermission)) {
                        rolePermissionsEntity.add(0, rolePermission);
                    }
                }
            }
        }

        return rolePermissionsEntity.stream().map(p -> {
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
}
