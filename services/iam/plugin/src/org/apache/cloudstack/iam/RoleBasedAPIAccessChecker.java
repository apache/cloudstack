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
package org.apache.cloudstack.iam;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;

import org.apache.cloudstack.acl.APIChecker;
import org.apache.cloudstack.acl.PermissionScope;
import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.acl.SecurityChecker.AccessType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.BaseListCmd;
import org.apache.cloudstack.iam.api.IAMPolicy;
import org.apache.cloudstack.iam.api.IAMPolicyPermission;
import org.apache.cloudstack.iam.api.IAMPolicyPermission.Permission;
import org.apache.cloudstack.iam.api.IAMService;

import com.cloud.api.ApiServerService;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.template.VirtualMachineTemplate;
import com.cloud.user.Account;
import com.cloud.user.AccountService;
import com.cloud.user.User;
import com.cloud.utils.PropertiesUtil;
import com.cloud.utils.component.AdapterBase;
import com.cloud.utils.component.PluggableService;
import com.cloud.utils.exception.CloudRuntimeException;

//This is the Role Based API access checker that grab's the  account's roles
//based on the set of roles, access is granted if any of the role has access to the api
public class RoleBasedAPIAccessChecker extends AdapterBase implements APIChecker {

    protected static final Logger s_logger = Logger.getLogger(RoleBasedAPIAccessChecker.class);

    @Inject
    AccountService _accountService;
    @Inject
    ApiServerService _apiServer;
    @Inject
    IAMService _iamSrv;
    @Inject
    VMTemplateDao _templateDao;

    Set<String> commandsPropertiesOverrides = new HashSet<String>();
    Map<RoleType, Set<String>> commandsPropertiesRoleBasedApisMap = new HashMap<RoleType, Set<String>>();

    List<PluggableService> _services;

    protected RoleBasedAPIAccessChecker() {
        super();
        for (RoleType roleType : RoleType.values()) {
            commandsPropertiesRoleBasedApisMap.put(roleType, new HashSet<String>());
        }
     }

    @Override
    public boolean checkAccess(User user, String commandName) throws PermissionDeniedException {
        Account account = _accountService.getAccount(user.getAccountId());
        if (account == null) {
            throw new PermissionDeniedException("The account id=" + user.getAccountId() + "for user id=" + user.getId()
                    + "is null");
        }

        List<IAMPolicy> policies = _iamSrv.listIAMPolicies(account.getAccountId());

        boolean isAllowed = _iamSrv.isActionAllowedForPolicies(commandName, policies);
        if (!isAllowed) {
            throw new PermissionDeniedException("The API does not exist or is blacklisted. api: " + commandName);
        }
        return isAllowed;
     }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        super.configure(name, params);

        processMapping(PropertiesUtil.processConfigFile(new String[] { "commands.properties" }));
        return true;
     }

    @Override
    public boolean start() {

        // drop all default policy api permissions - we reload them every time
        // to include any changes done to the @APICommand or
        // commands.properties.

        for (RoleType role : RoleType.values()) {
            Long policyId = getDefaultPolicyId(role);
            if (policyId != null) {
                _iamSrv.resetIAMPolicy(policyId);
            }
         }

        // add the system-domain capability

        _iamSrv.addIAMPermissionToIAMPolicy(new Long(Account.ACCOUNT_TYPE_ADMIN + 1), null, null, null,
                "SystemCapability", null, Permission.Allow, false);
        _iamSrv.addIAMPermissionToIAMPolicy(new Long(Account.ACCOUNT_TYPE_DOMAIN_ADMIN + 1), null, null, null,
                "DomainCapability", null, Permission.Allow, false);
        _iamSrv.addIAMPermissionToIAMPolicy(new Long(Account.ACCOUNT_TYPE_RESOURCE_DOMAIN_ADMIN + 1), null, null, null,
                "DomainResourceCapability", null, Permission.Allow, false);

        // add permissions for public templates
        List<VMTemplateVO> pTmplts = _templateDao.listByPublic();
        for (VMTemplateVO tmpl : pTmplts){
            _iamSrv.addIAMPermissionToIAMPolicy(new Long(Account.ACCOUNT_TYPE_ADMIN + 1), VirtualMachineTemplate.class.getSimpleName(),
                    PermissionScope.RESOURCE.toString(), tmpl.getId(), "listTemplates", AccessType.UseEntry.toString(), Permission.Allow, false);
            _iamSrv.addIAMPermissionToIAMPolicy(new Long(Account.ACCOUNT_TYPE_DOMAIN_ADMIN + 1), VirtualMachineTemplate.class.getSimpleName(),
                    PermissionScope.RESOURCE.toString(), tmpl.getId(), "listTemplates", AccessType.UseEntry.toString(), Permission.Allow, false);
            _iamSrv.addIAMPermissionToIAMPolicy(new Long(Account.ACCOUNT_TYPE_NORMAL + 1), VirtualMachineTemplate.class.getSimpleName(),
                    PermissionScope.RESOURCE.toString(), tmpl.getId(), "listTemplates", AccessType.UseEntry.toString(), Permission.Allow, false);
        }

        for (PluggableService service : _services) {
            for (Class<?> cmdClass : service.getCommands()) {
                APICommand command = cmdClass.getAnnotation(APICommand.class);
                if (!commandsPropertiesOverrides.contains(command.name())) {
                    for (RoleType role : command.authorized()) {
                        addDefaultAclPolicyPermission(command.name(), cmdClass, role);
                    }
                 }
             }
         }

        // read commands.properties and load api acl permissions -
        // commands.properties overrides any @APICommand authorization

        for (String apiName : commandsPropertiesOverrides) {
            Class<?> cmdClass = _apiServer.getCmdClass(apiName);
            for (RoleType role : RoleType.values()) {
                if (commandsPropertiesRoleBasedApisMap.get(role).contains(apiName)) {
                    // insert permission for this role for this api
                    addDefaultAclPolicyPermission(apiName, cmdClass, role);
                }
             }
         }

        return super.start();
     }

    private Long getDefaultPolicyId(RoleType role) {
        Long policyId = null;
        switch (role) {
        case User:
            policyId = new Long(Account.ACCOUNT_TYPE_NORMAL + 1);
            break;

        case Admin:
            policyId = new Long(Account.ACCOUNT_TYPE_ADMIN + 1);
            break;

        case DomainAdmin:
            policyId = new Long(Account.ACCOUNT_TYPE_DOMAIN_ADMIN + 1);
            break;

        case ResourceAdmin:
            policyId = new Long(Account.ACCOUNT_TYPE_RESOURCE_DOMAIN_ADMIN + 1);
            break;
        }

        return policyId;
    }

    private void processMapping(Map<String, String> configMap) {
        for (Map.Entry<String, String> entry : configMap.entrySet()) {
            String apiName = entry.getKey();
            String roleMask = entry.getValue();
            commandsPropertiesOverrides.add(apiName);
            try {
                short cmdPermissions = Short.parseShort(roleMask);
                for (RoleType roleType : RoleType.values()) {
                    if ((cmdPermissions & roleType.getValue()) != 0)
                        commandsPropertiesRoleBasedApisMap.get(roleType).add(apiName);
                }
            } catch (NumberFormatException nfe) {
                s_logger.info("Malformed key=value pair for entry: " + entry.toString());
             }
         }
     }

    public List<PluggableService> getServices() {
        return _services;
     }

    @Inject
    public void setServices(List<PluggableService> services) {
        _services = services;
     }

    private void addDefaultAclPolicyPermission(String apiName, Class<?> cmdClass, RoleType role) {
        AccessType accessType = null;
        Class<?>[] entityTypes = null;

        PermissionScope permissionScope = PermissionScope.ACCOUNT;
        Long policyId = getDefaultPolicyId(role);
        switch (role) {
        case User:
            permissionScope = PermissionScope.ACCOUNT;
            break;

        case Admin:
            permissionScope = PermissionScope.ALL;
            break;

        case DomainAdmin:
            permissionScope = PermissionScope.DOMAIN;
            break;

        case ResourceAdmin:
            permissionScope = PermissionScope.DOMAIN;
            break;
         }

        boolean addAccountScopedUseEntry = false;

        if (cmdClass != null) {
            BaseCmd cmdObj;
            try {
                cmdObj = (BaseCmd) cmdClass.newInstance();
                if (cmdObj instanceof BaseListCmd) {
                    if (permissionScope == PermissionScope.ACCOUNT) {
                        accessType = AccessType.UseEntry;
                    } else {
                        accessType = AccessType.ListEntry;
                        addAccountScopedUseEntry = true;
                    }
                } else {
                    accessType = AccessType.OperateEntry;
                }
            } catch (Exception e) {
                throw new CloudRuntimeException(String.format(
                        "%s is claimed as an API command, but it cannot be instantiated", cmdClass.getName()));
            }

            APICommand at = cmdClass.getAnnotation(APICommand.class);
            entityTypes = at.entityType();
        }

        if (entityTypes == null || entityTypes.length == 0) {
            _iamSrv.addIAMPermissionToIAMPolicy(policyId, null, permissionScope.toString(), new Long(IAMPolicyPermission.PERMISSION_SCOPE_ID_CURRENT_CALLER),
                    apiName, (accessType == null) ? null : accessType.toString(), Permission.Allow, false);
            if (addAccountScopedUseEntry) {
                _iamSrv.addIAMPermissionToIAMPolicy(policyId, null, PermissionScope.ACCOUNT.toString(), new Long(
                        IAMPolicyPermission.PERMISSION_SCOPE_ID_CURRENT_CALLER), apiName, AccessType.UseEntry.toString(), Permission.Allow, false);
            }
        } else {
            for (Class<?> entityType : entityTypes) {
                _iamSrv.addIAMPermissionToIAMPolicy(policyId, entityType.getSimpleName(), permissionScope.toString(), new Long(
                        IAMPolicyPermission.PERMISSION_SCOPE_ID_CURRENT_CALLER),
                        apiName, (accessType == null) ? null : accessType.toString(), Permission.Allow, false);
                if (addAccountScopedUseEntry) {
                    _iamSrv.addIAMPermissionToIAMPolicy(policyId, entityType.getSimpleName(), PermissionScope.ACCOUNT.toString(), new Long(
                            IAMPolicyPermission.PERMISSION_SCOPE_ID_CURRENT_CALLER), apiName, AccessType.UseEntry.toString(), Permission.Allow, false);
                }
            }
         }

     }

}
