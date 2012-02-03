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
package com.cloud.exception;

import java.util.List;

import com.cloud.acl.ControlledEntity;
import com.cloud.user.Account;
import com.cloud.utils.SerialVersionUID;
import com.cloud.utils.exception.CloudRuntimeException;

/**
 * @author chiradeep
 * 
 */
public class PermissionDeniedException extends CloudRuntimeException {

    private static final long serialVersionUID = SerialVersionUID.PermissionDeniedException;

    public PermissionDeniedException(String message) {
        super(message);
    }

    protected PermissionDeniedException() {
        super();
    }

    List<? extends ControlledEntity> violations;
    Account account;

    public PermissionDeniedException(String message, Account account, List<? extends ControlledEntity> violations) {
        super(message);
        this.violations = violations;
        this.account = account;
    }

    public Account getAccount() {
        return account;
    }

    public List<? extends ControlledEntity> getEntitiesInViolation() {
        return violations;
    }

    public void addDetails(Account account, List<? extends ControlledEntity> violations) {
        this.account = account;
        this.violations = violations;
    }

    public void addViolations(List<? extends ControlledEntity> violations) {
        this.violations = violations;
    }
}
