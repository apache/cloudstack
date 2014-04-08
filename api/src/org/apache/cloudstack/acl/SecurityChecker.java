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

import com.cloud.dc.DataCenter;
import com.cloud.domain.Domain;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.offering.DiskOffering;
import com.cloud.offering.ServiceOffering;
import com.cloud.user.Account;
import com.cloud.user.User;
import com.cloud.utils.component.Adapter;

/**
 * SecurityChecker checks the ownership and access control to objects within
 */
public interface SecurityChecker extends Adapter {

    public enum AccessType {
        ModifyProject,
        OperateEntry,
        UseEntry,
        ListEntry
    }

    /**
     * Checks if the account owns the object.
     *
     * @param caller
     *            account to check against.
     * @param object
     *            object that the account is trying to access.
     * @return true if access allowed. false if this adapter cannot authenticate ownership.
     * @throws PermissionDeniedException
     *             if this adapter is suppose to authenticate ownership and the check failed.
     */
    boolean checkAccess(Account caller, Domain domain) throws PermissionDeniedException;

    /**
     * Checks if the user belongs to an account that owns the object.
     *
     * @param user
     *            user to check against.
     * @param object
     *            object that the account is trying to access.
     * @return true if access allowed. false if this adapter cannot authenticate ownership.
     * @throws PermissionDeniedException
     *             if this adapter is suppose to authenticate ownership and the check failed.
     */
    boolean checkAccess(User user, Domain domain) throws PermissionDeniedException;

    /**
     * Checks if the account can access the object.
     *
     * @param caller
     *            account to check against.
     * @param entity
     *            object that the account is trying to access.
     * @param accessType
     *            TODO
     * @return true if access allowed. false if this adapter cannot provide
     *         permission.
     * @throws PermissionDeniedException
     *             if this adapter is suppose to authenticate ownership and the
     *             check failed.
     */
    boolean checkAccess(Account caller, ControlledEntity entity, AccessType accessType)
            throws PermissionDeniedException;

    /**
     * Checks if the account can access the object.
     *
     * @param caller
     *            account to check against.
     * @param entity
     *            object that the account is trying to access.
     * @param accessType
     *            TODO
     * @param action
     *            name of the API
     * @return true if access allowed. false if this adapter cannot provide
     *         permission.
     * @throws PermissionDeniedException
     *             if this adapter is suppose to authenticate ownership and the
     *             check failed.
     */
    boolean checkAccess(Account caller, ControlledEntity entity, AccessType accessType, String action) throws PermissionDeniedException;

    /**
     * Checks if the account can access multiple objects.
     *
     * @param caller
     *            account to check against.
     * @param entities
     *            objects that the account is trying to access.
     * @param accessType
     *            TODO
     * @param action
     *            name of the API
     * @return true if access allowed. false if this adapter cannot provide
     *         permission.
     * @throws PermissionDeniedException
     *             if this adapter is suppose to authenticate ownership and the
     *             check failed.
     */
    boolean checkAccess(Account caller, AccessType accessType, String action, ControlledEntity... entities)
            throws PermissionDeniedException;


    /**
     * Checks if the user belongs to an account that can access the object.
     *
     * @param user
     *            user to check against.
     * @param entity
     *            object that the account is trying to access.
     * @return true if access allowed. false if this adapter cannot authenticate ownership.
     * @throws PermissionDeniedException
     *             if this adapter is suppose to authenticate ownership and the check failed.
     */
    boolean checkAccess(User user, ControlledEntity entity) throws PermissionDeniedException;

    boolean checkAccess(Account account, DataCenter zone) throws PermissionDeniedException;

    public boolean checkAccess(Account account, ServiceOffering so) throws PermissionDeniedException;

    boolean checkAccess(Account account, DiskOffering dof) throws PermissionDeniedException;
}
