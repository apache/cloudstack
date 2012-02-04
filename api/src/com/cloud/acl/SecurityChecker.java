/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
 * 
 * This software is licensed under the GNU General Public License v3 or later.
 * 
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */

/**
 * 
 */
package com.cloud.acl;

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
 * the management stack for users and accounts.
 */
public interface SecurityChecker extends Adapter {

    public enum AccessType {
        ListEntry,
        ModifyEntry,
        ModifyProject,
        UseNetwork
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
     * @return true if access allowed. false if this adapter cannot provide permission.
     * @throws PermissionDeniedException
     *             if this adapter is suppose to authenticate ownership and the check failed.
     */
    boolean checkAccess(Account caller, ControlledEntity entity, AccessType accessType) throws PermissionDeniedException;

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
