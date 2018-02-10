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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import org.apache.cloudstack.acl.dao.RoleDao;
import org.apache.cloudstack.acl.dao.RolePermissionsDao;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.command.admin.acl.CreateRoleCmd;
import org.apache.cloudstack.api.command.admin.acl.CreateRolePermissionCmd;
import org.apache.cloudstack.api.command.admin.acl.DeleteRoleCmd;
import org.apache.cloudstack.api.command.admin.acl.DeleteRolePermissionCmd;
import org.apache.cloudstack.api.command.admin.acl.ListRolePermissionsCmd;
import org.apache.cloudstack.api.command.admin.acl.ListRolesCmd;
import org.apache.cloudstack.api.command.admin.acl.UpdateRoleCmd;
import org.apache.cloudstack.api.command.admin.acl.UpdateRolePermissionCmd;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;

import com.cloud.event.ActionEvent;
import com.cloud.event.EventTypes;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.user.Account;
import com.cloud.user.dao.AccountDao;
import com.cloud.utils.ListUtils;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.component.PluggableService;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallback;
import com.cloud.utils.db.TransactionStatus;
import com.google.common.base.Strings;

public class RoleManagerImpl extends ManagerBase implements RoleService, Configurable, PluggableService {
    @Inject
    private AccountDao accountDao;
    @Inject
    private RoleDao roleDao;
    @Inject
    private RolePermissionsDao rolePermissionsDao;

    private void checkCallerAccess() {
        if (!isEnabled()) {
            throw new PermissionDeniedException("Dynamic api checker is not enabled, aborting role operation");
        }
        Account caller = CallContext.current().getCallingAccount();
        if (caller == null || caller.getRoleId() == null) {
            throw new PermissionDeniedException("Restricted API called by an invalid user account");
        }
        Role callerRole = findRole(caller.getRoleId());
        if (callerRole == null || callerRole.getRoleType() != RoleType.Admin) {
            throw new PermissionDeniedException("Restricted API called by an user account of non-Admin role type");
        }
    }

    @Override
    public boolean isEnabled() {
        return RoleService.EnableDynamicApiChecker.value();
    }

    @Override
    public Role findRole(final Long id) {
        if (id == null || id < 1L) {
            return null;
        }
        return roleDao.findById(id);
    }

    @Override
    public RolePermission findRolePermission(final Long id) {
        if (id == null) {
            return null;
        }
        return rolePermissionsDao.findById(id);
    }

    @Override
    public RolePermission findRolePermissionByUuid(final String uuid) {
        if (Strings.isNullOrEmpty(uuid)) {
            return null;
        }
        return rolePermissionsDao.findByUuid(uuid);
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_ROLE_CREATE, eventDescription = "creating Role")
    public Role createRole(final String name, final RoleType roleType, final String description) {
        checkCallerAccess();
        if (roleType == null || roleType == RoleType.Unknown) {
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR, "Invalid role type provided");
        }
        return Transaction.execute(new TransactionCallback<RoleVO>() {
            @Override
            public RoleVO doInTransaction(TransactionStatus status) {
                return roleDao.persist(new RoleVO(name, roleType, description));
            }
        });
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_ROLE_UPDATE, eventDescription = "updating Role")
    public Role updateRole(final Role role, final String name, final RoleType roleType, final String description) {
        checkCallerAccess();

        if (roleType != null && roleType == RoleType.Unknown) {
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR, "Unknown is not a valid role type");
        }
        RoleVO roleVO = (RoleVO) role;
        if (!Strings.isNullOrEmpty(name)) {
            roleVO.setName(name);
        }
        if (roleType != null) {
            if (role.getId() <= RoleType.User.getId()) {
                throw new PermissionDeniedException("The role type of default roles cannot be changed");
            }
            List<? extends Account> accounts = accountDao.findAccountsByRole(role.getId());
            if (accounts == null || accounts.isEmpty()) {
                roleVO.setRoleType(roleType);
            } else {
                throw new PermissionDeniedException("Found accounts that have role in use, won't allow to change role type");
            }
        }
        if (!Strings.isNullOrEmpty(description)) {
            roleVO.setDescription(description);
        }

        roleDao.update(role.getId(), roleVO);
        return role;
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_ROLE_DELETE, eventDescription = "deleting Role")
    public boolean deleteRole(final Role role) {
        checkCallerAccess();
        if (role == null) {
            return false;
        }
        if (role.getId() <= RoleType.User.getId()) {
            throw new PermissionDeniedException("Default roles cannot be deleted");
        }
        List<? extends Account> accounts = accountDao.findAccountsByRole(role.getId());
        if (accounts == null || accounts.size() == 0) {
            return Transaction.execute(new TransactionCallback<Boolean>() {
                @Override
                public Boolean doInTransaction(TransactionStatus status) {
                    List<? extends RolePermission> rolePermissions = rolePermissionsDao.findAllByRoleIdSorted(role.getId());
                    if (rolePermissions != null && !rolePermissions.isEmpty()) {
                        for (RolePermission rolePermission : rolePermissions) {
                            rolePermissionsDao.remove(rolePermission.getId());
                        }
                    }
                    if (roleDao.remove(role.getId())) {
                        RoleVO roleVO = roleDao.findByIdIncludingRemoved(role.getId());
                        roleVO.setName(null);
                        return roleDao.update(role.getId(), roleVO);
                    }
                    return false;
                }
            });
        }
        throw new PermissionDeniedException("Found accounts that have role in use, won't allow to delete role");
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_ROLE_PERMISSION_CREATE, eventDescription = "creating Role Permission")
    public RolePermission createRolePermission(final Role role, final Rule rule, final RolePermission.Permission permission, final String description) {
        checkCallerAccess();
        return Transaction.execute(new TransactionCallback<RolePermissionVO>() {
            @Override
            public RolePermissionVO doInTransaction(TransactionStatus status) {
                return rolePermissionsDao.persist(new RolePermissionVO(role.getId(), rule.toString(), permission, description));
            }
        });
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_ROLE_PERMISSION_UPDATE, eventDescription = "updating Role Permission order")
    public boolean updateRolePermission(final Role role, final List<RolePermission> newOrder) {
        checkCallerAccess();
        return role != null && newOrder != null && rolePermissionsDao.update(role, newOrder);
    }

    @Override
    public boolean updateRolePermission(Role role, RolePermission rolePermission, RolePermission.Permission permission) {
        checkCallerAccess();
        return role != null && rolePermissionsDao.update(role, rolePermission, permission);
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_ROLE_PERMISSION_DELETE, eventDescription = "deleting Role Permission")
    public boolean deleteRolePermission(final RolePermission rolePermission) {
        checkCallerAccess();
        return rolePermission != null && rolePermissionsDao.remove(rolePermission.getId());
    }

    @Override
    public List<Role> findRolesByName(final String name) {
        List<? extends Role> roles = null;
        if (!Strings.isNullOrEmpty(name)) {
            roles = roleDao.findAllByName(name);
        }
        return ListUtils.toListOfInterface(roles);
    }

    @Override
    public List<Role> findRolesByType(final RoleType roleType) {
        List<? extends Role> roles = null;
        if (roleType != null) {
            roles = roleDao.findAllByRoleType(roleType);
        }
        return ListUtils.toListOfInterface(roles);
    }

    @Override
    public List<Role> listRoles() {
        List<? extends Role> roles = roleDao.listAll();
        return ListUtils.toListOfInterface(roles);
    }

    @Override
    public List<RolePermission> findAllPermissionsBy(final Long roleId) {
        List<? extends RolePermission> permissions = rolePermissionsDao.findAllByRoleIdSorted(roleId);
        if (permissions != null) {
            return new ArrayList<>(permissions);
        }
        return Collections.emptyList();
    }

    @Override
    public String getConfigComponentName() {
        return RoleService.class.getSimpleName();
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey<?>[]{RoleService.EnableDynamicApiChecker};
    }

    @Override
    public List<Class<?>> getCommands() {
        final List<Class<?>> cmdList = new ArrayList<>();
        cmdList.add(CreateRoleCmd.class);
        cmdList.add(ListRolesCmd.class);
        cmdList.add(UpdateRoleCmd.class);
        cmdList.add(DeleteRoleCmd.class);
        cmdList.add(CreateRolePermissionCmd.class);
        cmdList.add(ListRolePermissionsCmd.class);
        cmdList.add(UpdateRolePermissionCmd.class);
        cmdList.add(DeleteRolePermissionCmd.class);
        return cmdList;
    }
 }
