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
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.cloudstack.acl.dao.RoleDao;
import org.apache.cloudstack.acl.dao.RolePermissionsDao;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.command.admin.acl.CreateRoleCmd;
import org.apache.cloudstack.api.command.admin.acl.CreateRolePermissionCmd;
import org.apache.cloudstack.api.command.admin.acl.DeleteRoleCmd;
import org.apache.cloudstack.api.command.admin.acl.DeleteRolePermissionCmd;
import org.apache.cloudstack.api.command.admin.acl.ImportRoleCmd;
import org.apache.cloudstack.api.command.admin.acl.ListRolePermissionsCmd;
import org.apache.cloudstack.api.command.admin.acl.ListRolesCmd;
import org.apache.cloudstack.api.command.admin.acl.UpdateRoleCmd;
import org.apache.cloudstack.api.command.admin.acl.UpdateRolePermissionCmd;
import org.apache.cloudstack.acl.RolePermissionEntity.Permission;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import com.cloud.event.ActionEvent;
import com.cloud.event.EventTypes;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.dao.AccountDao;
import com.cloud.utils.ListUtils;
import com.cloud.utils.Pair;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.component.PluggableService;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallback;
import com.cloud.utils.db.TransactionStatus;
import com.cloud.utils.exception.CloudRuntimeException;

public class RoleManagerImpl extends ManagerBase implements RoleService, Configurable, PluggableService {

    private Logger logger = Logger.getLogger(getClass());

    @Inject
    private AccountDao accountDao;
    @Inject
    private RoleDao roleDao;
    @Inject
    private RolePermissionsDao rolePermissionsDao;
    @Inject
    private AccountManager accountManager;

    public void checkCallerAccess() {
        if (!isEnabled()) {
            throw new PermissionDeniedException("Dynamic api checker is not enabled, aborting role operation");
        }
        Account caller = getCurrentAccount();
        if (caller == null || caller.getRoleId() == null) {
            throw new PermissionDeniedException("Restricted API called by an invalid user account");
        }
        Role callerRole = findRole(caller.getRoleId());
        if (callerRole == null || callerRole.getRoleType() != RoleType.Admin) {
            throw new PermissionDeniedException("Restricted API called by a user account of non-Admin role type");
        }
    }

    @Override
    public boolean isEnabled() {
        return RoleService.EnableDynamicApiChecker.value();
    }

    @Override
    public Role findRole(Long id) {
        if (id == null || id < 1L) {
            logger.trace(String.format("Role ID is invalid [%s]", id));
            return null;
        }
        RoleVO role = roleDao.findById(id);
        if (role == null) {
            logger.trace(String.format("Role not found [id=%s]", id));
            return null;
        }
        Account account = getCurrentAccount();
        if (!accountManager.isRootAdmin(account.getId()) && RoleType.Admin == role.getRoleType()) {
            logger.debug(String.format("Role [id=%s, name=%s] is of 'Admin' type and is only visible to 'Root admins'.", id, role.getName()));
            return null;
        }
        return role;
    }

    /**
     * Simple call to {@link CallContext#current()} to retrieve the current calling account.
     * This method facilitates unit testing, it avoids mocking static methods.
     */
    protected Account getCurrentAccount() {
        return CallContext.current().getCallingAccount();
    }

    @Override
    public RolePermission findRolePermission(final Long id) {
        if (id == null) {
            return null;
        }
        return rolePermissionsDao.findById(id);
    }

    @Override
    public RolePermission findRolePermissionByRoleIdAndRule(final Long roleId, final String rule) {
        if (roleId == null || StringUtils.isEmpty(rule)) {
            return null;
        }

        return rolePermissionsDao.findByRoleIdAndRule(roleId, rule);
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
                RoleVO role = roleDao.persist(new RoleVO(name, roleType, description));
                CallContext.current().putContextParameter(Role.class, role.getUuid());
                return role;
            }
        });
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_ROLE_CREATE, eventDescription = "creating role by cloning another role")
    public Role createRole(String name, Role role, String description) {
        checkCallerAccess();
        return Transaction.execute(new TransactionCallback<RoleVO>() {
            @Override
            public RoleVO doInTransaction(TransactionStatus status) {
                RoleVO newRoleVO = roleDao.persist(new RoleVO(name, role.getRoleType(), description));
                if (newRoleVO == null) {
                    throw new CloudRuntimeException("Unable to create the role: " + name + ", failed to persist in DB");
                }

                List<RolePermissionVO> rolePermissions = rolePermissionsDao.findAllByRoleIdSorted(role.getId());
                if (rolePermissions != null && !rolePermissions.isEmpty()) {
                    for (RolePermissionVO permission : rolePermissions) {
                        rolePermissionsDao.persist(new RolePermissionVO(newRoleVO.getId(), permission.getRule().toString(), permission.getPermission(), permission.getDescription()));
                    }
                }

                return newRoleVO;
            }
        });
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_ROLE_IMPORT, eventDescription = "importing Role")
    public Role importRole(String name, RoleType type, String description, List<Map<String, Object>> rules, boolean forced) {
        checkCallerAccess();
        if (StringUtils.isEmpty(name)) {
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR, "Invalid role name provided");
        }
        if (type == null || type == RoleType.Unknown) {
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR, "Invalid role type provided");
        }

        List<RoleVO> existingRoles = roleDao.findByName(name);
        if (CollectionUtils.isNotEmpty(existingRoles) && !forced) {
            throw new CloudRuntimeException("Role already exists");
        }

        return Transaction.execute(new TransactionCallback<RoleVO>() {
            @Override
            public RoleVO doInTransaction(TransactionStatus status) {
                RoleVO newRole = null;
                RoleVO existingRole = roleDao.findByNameAndType(name, type);
                if (existingRole != null) {
                    if (existingRole.isDefault()) {
                        throw new CloudRuntimeException("Failed to import the role: " + name + ", default role cannot be overriden");
                    }

                    //Cleanup old role permissions
                    List<? extends RolePermission> rolePermissions = rolePermissionsDao.findAllByRoleIdSorted(existingRole.getId());
                    if (rolePermissions != null && !rolePermissions.isEmpty()) {
                        for (RolePermission rolePermission : rolePermissions) {
                            rolePermissionsDao.remove(rolePermission.getId());
                        }
                    }

                    existingRole.setName(name);
                    existingRole.setRoleType(type);
                    existingRole.setDescription(description);
                    roleDao.update(existingRole.getId(), existingRole);

                    newRole = existingRole;
                } else {
                    newRole = roleDao.persist(new RoleVO(name, type, description));
                }

                if (newRole == null) {
                    throw new CloudRuntimeException("Unable to import the role: " + name + ", failed to persist in DB");
                }

                if (rules != null && !rules.isEmpty()) {
                    for (Map<String, Object> ruleDetail : rules) {
                        Rule rule = (Rule)ruleDetail.get(ApiConstants.RULE);
                        RolePermission.Permission rulePermission = (RolePermission.Permission) ruleDetail.get(ApiConstants.PERMISSION);
                        String ruleDescription = (String) ruleDetail.get(ApiConstants.DESCRIPTION);

                        rolePermissionsDao.persist(new RolePermissionVO(newRole.getId(), rule.toString(), rulePermission, ruleDescription));
                    }
                }
                return newRole;
            }
        });
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_ROLE_UPDATE, eventDescription = "updating Role")
    public Role updateRole(final Role role, final String name, final RoleType roleType, final String description) {
        checkCallerAccess();
        if (role.isDefault()) {
            throw new PermissionDeniedException("Default roles cannot be updated");
        }

        if (roleType != null && roleType == RoleType.Unknown) {
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR, "Unknown is not a valid role type");
        }
        RoleVO roleVO = (RoleVO)role;
        if (StringUtils.isNotEmpty(name)) {
            roleVO.setName(name);
        }
        if (roleType != null) {
            List<? extends Account> accounts = accountDao.findAccountsByRole(role.getId());
            if (accounts == null || accounts.isEmpty()) {
                roleVO.setRoleType(roleType);
            } else {
                throw new PermissionDeniedException("Found accounts that have role in use, won't allow to change role type");
            }
        }
        if (StringUtils.isNotEmpty(description)) {
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
        if (role.isDefault()) {
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
    public RolePermission createRolePermission(final Role role, final Rule rule, final Permission permission, final String description) {
        checkCallerAccess();
        if (role.isDefault()) {
            throw new PermissionDeniedException("Role permission cannot be added for Default roles");
        }

        if (findRolePermissionByRoleIdAndRule(role.getId(), rule.toString()) != null) {
            throw new PermissionDeniedException("Rule already exists for the role: " + role.getName());
        }

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
        if (role.isDefault()) {
            throw new PermissionDeniedException("Role permission cannot be updated for Default roles");
        }
        return role != null && newOrder != null && rolePermissionsDao.update(role, newOrder);
    }

    @Override
    public boolean updateRolePermission(Role role, RolePermission rolePermission, Permission permission) {
        checkCallerAccess();
        if (role.isDefault()) {
            throw new PermissionDeniedException("Role permission cannot be updated for Default roles");
        }
        return role != null && rolePermissionsDao.update(role, rolePermission, permission);
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_ROLE_PERMISSION_DELETE, eventDescription = "deleting Role Permission")
    public boolean deleteRolePermission(final RolePermission rolePermission) {
        checkCallerAccess();
        Role role = findRole(rolePermission.getRoleId());
        if (role.isDefault()) {
            throw new PermissionDeniedException("Role permission cannot be deleted for Default roles");
        }
        return rolePermission != null && rolePermissionsDao.remove(rolePermission.getId());
    }

    @Override
    public List<Role> findRolesByName(String name) {
        return findRolesByName(name, null, null, null).first();
    }

    @Override
    public Pair<List<Role>, Integer> findRolesByName(String name, String keyword, Long startIndex, Long limit) {
        if (StringUtils.isNotBlank(name) || StringUtils.isNotBlank(keyword)) {
            Pair<List<RoleVO>, Integer> data = roleDao.findAllByName(name, keyword, startIndex, limit);
            int removed = removeRootAdminRolesIfNeeded(data.first());
            return new Pair<List<Role>,Integer>(ListUtils.toListOfInterface(data.first()), Integer.valueOf(data.second() - removed));
        }
        return new Pair<List<Role>, Integer>(new ArrayList<Role>(), 0);
    }

    /**
     *  Removes roles of the given list that have the type '{@link RoleType#Admin}' if the user calling the method is not a 'root admin'.
     *  The actual removal is executed via {@link #removeRootAdminRoles(List)}. Therefore, if the method is called by a 'root admin', we do nothing here.
     */
    protected int removeRootAdminRolesIfNeeded(List<? extends Role> roles) {
        Account account = getCurrentAccount();
        if (!accountManager.isRootAdmin(account.getId())) {
            return removeRootAdminRoles(roles);
        }
        return 0;
    }

    /**
     * Remove all roles that have the {@link RoleType#Admin}.
     */
    protected int removeRootAdminRoles(List<? extends Role> roles) {
        if (CollectionUtils.isEmpty(roles)) {
            return 0;
        }
        Iterator<? extends Role> rolesIterator = roles.iterator();
        int count = 0;
        while (rolesIterator.hasNext()) {
            Role role = rolesIterator.next();
            if (RoleType.Admin == role.getRoleType()) {
                count++;
                rolesIterator.remove();
            }
        }
        return count;
    }

    @Override
    public List<Role> findRolesByType(RoleType roleType) {
        return findRolesByType(roleType, null, null).first();
    }

    @Override
    public Pair<List<Role>, Integer> findRolesByType(RoleType roleType, Long startIndex, Long limit) {
        if (roleType == null || RoleType.Admin == roleType && !accountManager.isRootAdmin(getCurrentAccount().getId())) {
            return new Pair<List<Role>, Integer>(Collections.emptyList(), 0);
        }
        Pair<List<RoleVO>, Integer> data = roleDao.findAllByRoleType(roleType, startIndex, limit);
        return new Pair<List<Role>,Integer>(ListUtils.toListOfInterface(data.first()), Integer.valueOf(data.second()));
    }

    @Override
    public List<Role> listRoles() {
        List<? extends Role> roles = roleDao.listAll();
        removeRootAdminRolesIfNeeded(roles);
        return ListUtils.toListOfInterface(roles);
    }

    @Override
    public Pair<List<Role>, Integer> listRoles(Long startIndex, Long limit) {
        Pair<List<RoleVO>, Integer> data = roleDao.searchAndCount(null,
                new Filter(RoleVO.class, "id", Boolean.TRUE, startIndex, limit));
        int removed = removeRootAdminRolesIfNeeded(data.first());
        return new Pair<List<Role>,Integer>(ListUtils.toListOfInterface(data.first()), Integer.valueOf(data.second() - removed));
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
    public Permission getRolePermission(String permission) {
        if (StringUtils.isEmpty(permission)) {
            return null;
        }
        if (!permission.equalsIgnoreCase(RolePermission.Permission.ALLOW.toString()) &&
                !permission.equalsIgnoreCase(RolePermission.Permission.DENY.toString())) {
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR, "Values for permission parameter should be: allow or deny");
        }
        return permission.equalsIgnoreCase(RolePermission.Permission.ALLOW.toString()) ? RolePermission.Permission.ALLOW : RolePermission.Permission.DENY;
    }

    @Override
    public String getConfigComponentName() {
        return RoleService.class.getSimpleName();
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey<?>[] {RoleService.EnableDynamicApiChecker};
    }

    @Override
    public List<Class<?>> getCommands() {
        final List<Class<?>> cmdList = new ArrayList<>();
        cmdList.add(CreateRoleCmd.class);
        cmdList.add(ImportRoleCmd.class);
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
