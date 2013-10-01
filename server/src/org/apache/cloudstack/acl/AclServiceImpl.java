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

import java.util.HashMap;
import java.util.List;

import javax.ejb.Local;
import javax.inject.Inject;

import org.apache.log4j.Logger;

import org.apache.cloudstack.acl.SecurityChecker.AccessType;
import org.apache.cloudstack.acl.dao.AclApiPermissionDao;
import org.apache.cloudstack.acl.dao.AclEntityPermissionDao;
import org.apache.cloudstack.acl.dao.AclGroupAccountMapDao;
import org.apache.cloudstack.acl.dao.AclGroupDao;
import org.apache.cloudstack.acl.dao.AclGroupRoleMapDao;
import org.apache.cloudstack.acl.dao.AclRoleDao;
import org.apache.cloudstack.api.Identity;
import org.apache.cloudstack.context.CallContext;

import com.cloud.event.ActionEvent;
import com.cloud.event.EventTypes;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.storage.Snapshot;
import com.cloud.storage.Volume;
import com.cloud.template.VirtualMachineTemplate;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.dao.AccountDao;
import com.cloud.uservm.UserVm;
import com.cloud.utils.component.Manager;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.EntityManager;
import com.cloud.utils.db.Transaction;

@Local(value = {AclService.class})
public class AclServiceImpl extends ManagerBase implements AclService, Manager {

    public static final Logger s_logger = Logger.getLogger(AclServiceImpl.class);
    private String _name;

    @Inject
    AccountManager _accountMgr;

    @Inject
    AccountDao _accountDao;

    @Inject
    AclRoleDao _aclRoleDao;

    @Inject
    AclGroupDao _aclGroupDao;

    @Inject
    EntityManager _entityMgr;

    @Inject
    AclGroupRoleMapDao _aclGroupRoleMapDao;

    @Inject
    AclGroupAccountMapDao _aclGroupAccountMapDao;

    @Inject
    AclApiPermissionDao _apiPermissionDao;

    @Inject
    AclEntityPermissionDao _entityPermissionDao;

    public static HashMap<String, Class> entityClassMap = new HashMap<String, Class>();

    static {
        entityClassMap.put("VirtualMachine", UserVm.class);
        entityClassMap.put("Volume", Volume.class);
        entityClassMap.put("Template", VirtualMachineTemplate.class);
        entityClassMap.put("Snapshot", Snapshot.class);
        // To be filled in later depending on the entity permission grant scope
    }

    @DB
    @Override
    @ActionEvent(eventType = EventTypes.EVENT_ACL_ROLE_CREATE, eventDescription = "Creating Acl Role", create = true)
    public AclRole createAclRole(Long domainId, String aclRoleName, String description, Long parentRoleId) {
        Account caller = CallContext.current().getCallingAccount();
        if (!_accountMgr.isRootAdmin(caller.getAccountId())) {
            // domain admin can only create role for his domain
            if (domainId != null && caller.getDomainId() != domainId.longValue()) {
                throw new PermissionDeniedException("Can't create acl role in domain " + domainId + ", permission denied");
            }
        }
        // check if the role is already existing
        AclRole ro = _aclRoleDao.findByName(domainId, aclRoleName);
        if (ro != null) {
            throw new InvalidParameterValueException(
                    "Unable to create acl role with name " + aclRoleName
                            + " already exisits for domain " + domainId);
        }
        AclRoleVO rvo = new AclRoleVO(aclRoleName, description);
        if (domainId != null) {
            rvo.setDomainId(domainId);
        }
        if (parentRoleId != null) {
            rvo.setParentRoleId(parentRoleId);
        }
        return _aclRoleDao.persist(rvo);
    }

    @DB
    @Override
    @ActionEvent(eventType = EventTypes.EVENT_ACL_ROLE_DELETE, eventDescription = "Deleting Acl Role")
    public boolean deleteAclRole(long aclRoleId) {
        Account caller = CallContext.current().getCallingAccount();
        // get the Acl Role entity
        AclRole role = _aclRoleDao.findById(aclRoleId);
        if (role == null) {
            throw new InvalidParameterValueException("Unable to find acl role: " + aclRoleId
                    + "; failed to delete acl role.");
        }
        // check permissions
        _accountMgr.checkAccess(caller, null, true, role);

        Transaction txn = Transaction.currentTxn();
        txn.start();
        // remove this role related entry in acl_group_role_map
        List<AclGroupRoleMapVO> groupRoleMap = _aclGroupRoleMapDao.listByRoleId(role.getId());
        if (groupRoleMap != null) {
            for (AclGroupRoleMapVO gr : groupRoleMap) {
                _aclGroupRoleMapDao.remove(gr.getId());
            }
        }

        // remove this role related entry in acl_api_permission table
        List<AclApiPermissionVO> roleApiMap = _apiPermissionDao.listByRoleId(role.getId());
        if (roleApiMap != null) {
            for (AclApiPermissionVO roleApi : roleApiMap) {
                _apiPermissionDao.remove(roleApi.getId());
            }
        }

        // remove this role from acl_role table
        _aclRoleDao.remove(aclRoleId);
        txn.commit();

        return true;
    }


    @DB
    @Override
    @ActionEvent(eventType = EventTypes.EVENT_ACL_ROLE_GRANT, eventDescription = "Granting permission to Acl Role")
    public AclRole grantPermissionToAclRole(long aclRoleId, List<String> apiNames) {
        Account caller = CallContext.current().getCallingAccount();
        // get the Acl Role entity
        AclRole role = _aclRoleDao.findById(aclRoleId);
        if (role == null) {
            throw new InvalidParameterValueException("Unable to find acl role: " + aclRoleId
                    + "; failed to grant permission to role.");
        }
        // check permissions
        _accountMgr.checkAccess(caller, null, true, role);

        Transaction txn = Transaction.currentTxn();
        txn.start();
        // add entries in acl_api_permission table
        for (String api : apiNames) {
            AclApiPermissionVO perm = _apiPermissionDao.findByRoleAndApi(aclRoleId, api);
            if (perm == null) {
                // not there already
                perm = new AclApiPermissionVO(aclRoleId, api);
                _apiPermissionDao.persist(perm);
            }
        }
        txn.commit();
        return role;

    }

    @DB
    @Override
    @ActionEvent(eventType = EventTypes.EVENT_ACL_ROLE_REVOKE, eventDescription = "Revoking permission from Acl Role")
    public AclRole revokePermissionFromAclRole(long aclRoleId, List<String> apiNames) {
        Account caller = CallContext.current().getCallingAccount();
        // get the Acl Role entity
        AclRole role = _aclRoleDao.findById(aclRoleId);
        if (role == null) {
            throw new InvalidParameterValueException("Unable to find acl role: " + aclRoleId
                    + "; failed to revoke permission from role.");
        }
        // check permissions
        _accountMgr.checkAccess(caller, null, true, role);

        Transaction txn = Transaction.currentTxn();
        txn.start();
        // remove entries from acl_api_permission table
        for (String api : apiNames) {
            AclApiPermissionVO perm = _apiPermissionDao.findByRoleAndApi(aclRoleId, api);
            if (perm != null) {
                // not removed yet
                _apiPermissionDao.remove(perm.getId());
            }
        }
        txn.commit();
        return role;
    }

    @DB
    @Override
    @ActionEvent(eventType = EventTypes.EVENT_ACL_GROUP_GRANT, eventDescription = "Granting entity permission to Acl Group")
    public AclGroup grantEntityPermissionToAclGroup(long aclGroupId, String entityType, long entityId, AccessType accessType) {
        Account caller = CallContext.current().getCallingAccount();
        // get the Acl Group entity
        AclGroup group = _aclGroupDao.findById(aclGroupId);
        if (group == null) {
            throw new InvalidParameterValueException("Unable to find acl group: " + aclGroupId
                    + "; failed to grant permission to group.");
        }
        // check permissions
        _accountMgr.checkAccess(caller, null, true, group);

        // get the entity and check permission
        Class entityClass = entityClassMap.get(entityType);
        if (entityClass == null) {
            throw new InvalidParameterValueException("Entity type " + entityType + " permission granting is not supported yet");
        }
        ControlledEntity entity = (ControlledEntity)_entityMgr.findById(entityClass, entityId);
        if (entity == null) {
            throw new InvalidParameterValueException("Unable to find entity " + entityType + " by id: " + entityId);
        }
        _accountMgr.checkAccess(caller,null, true, entity);
        
        // add entry in acl_entity_permission table
        AclEntityPermissionVO perm = _entityPermissionDao.findByGroupAndEntity(aclGroupId, entityType, entityId, accessType);
        if (perm == null) {
            // not there already
            String entityUuid = String.valueOf(entityId);
            if (entity instanceof Identity) {
                entityUuid = ((Identity)entity).getUuid();
            }
            perm = new AclEntityPermissionVO(aclGroupId, entityType, entityId, entityUuid, accessType);
            _entityPermissionDao.persist(perm);
        }
        return group;

    }

    @DB
    @Override
    @ActionEvent(eventType = EventTypes.EVENT_ACL_GROUP_REVOKE, eventDescription = "Revoking entity permission from Acl Group")
    public AclGroup revokeEntityPermissionFromAclGroup(long aclGroupId, String entityType, long entityId, AccessType accessType) {
        Account caller = CallContext.current().getCallingAccount();
        // get the Acl Group entity
        AclGroup group = _aclGroupDao.findById(aclGroupId);
        if (group == null) {
            throw new InvalidParameterValueException("Unable to find acl group: " + aclGroupId
                    + "; failed to revoke permission from group.");
        }
        // check permissions
        _accountMgr.checkAccess(caller, null, true, group);

        // get the entity and check permission
        Class entityClass = entityClassMap.get(entityType);
        if (entityClass == null) {
            throw new InvalidParameterValueException("Entity type " + entityType + " permission revoke is not supported yet");
        }
        ControlledEntity entity = (ControlledEntity)_entityMgr.findById(entityClass, entityId);
        if (entity == null) {
            throw new InvalidParameterValueException("Unable to find entity " + entityType + " by id: " + entityId);
        }
        _accountMgr.checkAccess(caller, null, true, entity);

        // remove entry from acl_entity_permission table
        AclEntityPermissionVO perm = _entityPermissionDao.findByGroupAndEntity(aclGroupId, entityType, entityId, accessType);
        if (perm != null) {
            // not removed yet
            _entityPermissionDao.remove(perm.getId());
        }
        return group;
    }

    @DB
    @Override
    @ActionEvent(eventType = EventTypes.EVENT_ACL_GROUP_UPDATE, eventDescription = "Adding roles to acl group")
    public AclGroup addAclRolesToGroup(List<Long> roleIds, Long groupId) {
        Account caller = CallContext.current().getCallingAccount();
        // get the Acl Group entity
        AclGroup group = _aclGroupDao.findById(groupId);
        if (group == null) {
            throw new InvalidParameterValueException("Unable to find acl group: " + groupId
                    + "; failed to add roles to acl group.");
        }
        // check group permissions
        _accountMgr.checkAccess(caller, null, true, group);
 
        Transaction txn = Transaction.currentTxn();
        txn.start();
        // add entries in acl_group_role_map table
        for (Long roleId : roleIds) {
            // check role permissions
            AclRole role = _aclRoleDao.findById(roleId);
            if ( role == null ){
                throw new InvalidParameterValueException("Unable to find acl role: " + roleId
                        + "; failed to add roles to acl group.");
            }
            _accountMgr.checkAccess(caller,null, true, role);
            
            AclGroupRoleMapVO grMap = _aclGroupRoleMapDao.findByGroupAndRole(groupId, roleId);
            if (grMap == null) {
                // not there already
                grMap = new AclGroupRoleMapVO(groupId, roleId);
                _aclGroupRoleMapDao.persist(grMap);
            }
        }
        txn.commit();
        return group;
    }

    @DB
    @Override
    @ActionEvent(eventType = EventTypes.EVENT_ACL_GROUP_UPDATE, eventDescription = "Removing roles from acl group")
    public AclGroup removeAclRolesFromGroup(List<Long> roleIds, Long groupId) {
        Account caller = CallContext.current().getCallingAccount();
        // get the Acl Group entity
        AclGroup group = _aclGroupDao.findById(groupId);
        if (group == null) {
            throw new InvalidParameterValueException("Unable to find acl group: " + groupId
                    + "; failed to remove roles from acl group.");
        }
        // check group permissions
        _accountMgr.checkAccess(caller, null, true, group);

        Transaction txn = Transaction.currentTxn();
        txn.start();
        // add entries in acl_group_role_map table
        for (Long roleId : roleIds) {
            // check role permissions
            AclRole role = _aclRoleDao.findById(roleId);
            if (role == null) {
                throw new InvalidParameterValueException("Unable to find acl role: " + roleId
                        + "; failed to add roles to acl group.");
            }
            _accountMgr.checkAccess(caller, null, true, role);

            AclGroupRoleMapVO grMap = _aclGroupRoleMapDao.findByGroupAndRole(groupId, roleId);
            if (grMap != null) {
                // not removed yet
                _aclGroupRoleMapDao.remove(grMap.getId());
            }
        }
        txn.commit();
        return group;
    }

    @DB
    @Override
    @ActionEvent(eventType = EventTypes.EVENT_ACL_GROUP_UPDATE, eventDescription = "Adding accounts to acl group")
    public AclGroup addAccountsToGroup(List<Long> acctIds, Long groupId) {
        Account caller = CallContext.current().getCallingAccount();
        // get the Acl Group entity
        AclGroup group = _aclGroupDao.findById(groupId);
        if (group == null) {
            throw new InvalidParameterValueException("Unable to find acl group: " + groupId
                    + "; failed to add accounts to acl group.");
        }
        // check group permissions
        _accountMgr.checkAccess(caller, null, true, group);

        Transaction txn = Transaction.currentTxn();
        txn.start();
        // add entries in acl_group_account_map table
        for (Long acctId : acctIds) {
            // check account permissions
            Account account = _accountDao.findById(acctId);
            if (account == null) {
                throw new InvalidParameterValueException("Unable to find account: " + acctId
                        + "; failed to add account to acl group.");
            }
            _accountMgr.checkAccess(caller, null, true, account);

            AclGroupAccountMapVO grMap = _aclGroupAccountMapDao.findByGroupAndAccount(groupId, acctId);
            if (grMap == null) {
                // not there already
                grMap = new AclGroupAccountMapVO(groupId, acctId);
                _aclGroupAccountMapDao.persist(grMap);
            }
        }
        txn.commit();
        return group;
    }

    @DB
    @Override
    @ActionEvent(eventType = EventTypes.EVENT_ACL_GROUP_UPDATE, eventDescription = "Removing accounts from acl group")
    public AclGroup removeAccountsFromGroup(List<Long> acctIds, Long groupId) {
        Account caller = CallContext.current().getCallingAccount();
        // get the Acl Group entity
        AclGroup group = _aclGroupDao.findById(groupId);
        if (group == null) {
            throw new InvalidParameterValueException("Unable to find acl group: " + groupId
                    + "; failed to remove accounts from acl group.");
        }
        // check group permissions
        _accountMgr.checkAccess(caller, null, true, group);

        Transaction txn = Transaction.currentTxn();
        txn.start();
        // remove entries from acl_group_account_map table
        for (Long acctId : acctIds) {
            // check account permissions
            Account account = _accountDao.findById(acctId);
            if (account == null) {
                throw new InvalidParameterValueException("Unable to find account: " + acctId
                        + "; failed to add account to acl group.");
            }
            _accountMgr.checkAccess(caller, null, true, account);

            AclGroupAccountMapVO grMap = _aclGroupAccountMapDao.findByGroupAndAccount(groupId, acctId);
            if (grMap != null) {
                // not removed yet
                _aclGroupAccountMapDao.remove(grMap.getId());
            }
        }
        txn.commit();
        return group;
    }

    @DB
    @Override
    @ActionEvent(eventType = EventTypes.EVENT_ACL_GROUP_CREATE, eventDescription = "Creating Acl Group", create = true)
    public AclGroup createAclGroup(Long domainId, String aclGroupName, String description) {
        Account caller = CallContext.current().getCallingAccount();
        if (!_accountMgr.isRootAdmin(caller.getAccountId())) {
            // domain admin can only create role for his domain
            if (domainId != null && caller.getDomainId() != domainId.longValue()) {
                throw new PermissionDeniedException("Can't create acl group in domain " + domainId + ", permission denied");
            }
        }
        // check if the role is already existing
        AclGroup grp = _aclGroupDao.findByName(domainId, aclGroupName);
        if (grp != null) {
            throw new InvalidParameterValueException(
                    "Unable to create acl group with name " + aclGroupName
                            + " already exisits for domain " + domainId);
        }
        AclGroupVO rvo = new AclGroupVO(aclGroupName, description);
        if (domainId != null) {
            rvo.setDomainId(domainId);
        }

        return _aclGroupDao.persist(rvo);
    }

    @DB
    @Override
    @ActionEvent(eventType = EventTypes.EVENT_ACL_GROUP_DELETE, eventDescription = "Deleting Acl Group")
    public boolean deleteAclGroup(Long aclGroupId) {
        Account caller = CallContext.current().getCallingAccount();
        // get the Acl Role entity
        AclGroup grp = _aclGroupDao.findById(aclGroupId);
        if (grp == null) {
            throw new InvalidParameterValueException("Unable to find acl group: " + aclGroupId
                    + "; failed to delete acl group.");
        }
        // check permissions
        _accountMgr.checkAccess(caller, null, true, grp);

        Transaction txn = Transaction.currentTxn();
        txn.start();
        // remove this group related entry in acl_group_role_map
        List<AclGroupRoleMapVO> groupRoleMap = _aclGroupRoleMapDao.listByGroupId(grp.getId());
        if (groupRoleMap != null) {
            for (AclGroupRoleMapVO gr : groupRoleMap) {
                _aclGroupRoleMapDao.remove(gr.getId());
            }
        }

        // remove this group related entry in acl_group_account table
        List<AclGroupAccountMapVO> groupAcctMap = _aclGroupAccountMapDao.listByGroupId(grp.getId());
        if (groupAcctMap != null) {
            for (AclGroupAccountMapVO grpAcct : groupAcctMap) {
                _aclGroupAccountMapDao.remove(grpAcct.getId());
            }
        }

        // remove this group from acl_group table
        _aclGroupDao.remove(aclGroupId);
        txn.commit();

        return true;
    }


}
