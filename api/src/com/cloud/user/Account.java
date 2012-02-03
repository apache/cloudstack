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

package com.cloud.user;

import java.util.Date;

import com.cloud.acl.ControlledEntity;

public interface Account extends ControlledEntity {
    public enum Type {
        Normal,
        Admin,
        DomainAdmin,
        CustomerCare,
        Project
    }

    public enum State {
        disabled,
        enabled,
        locked
    }

    public static final short ACCOUNT_TYPE_NORMAL = 0;
    public static final short ACCOUNT_TYPE_ADMIN = 1;
    public static final short ACCOUNT_TYPE_DOMAIN_ADMIN = 2;
    public static final short ACCOUNT_TYPE_RESOURCE_DOMAIN_ADMIN = 3;
    public static final short ACCOUNT_TYPE_READ_ONLY_ADMIN = 4;
    public static final short ACCOUNT_TYPE_PROJECT = 5;

    public static final String ACCOUNT_STATE_DISABLED = "disabled";
    public static final String ACCOUNT_STATE_ENABLED = "enabled";
    public static final String ACCOUNT_STATE_LOCKED = "locked";

    public static final long ACCOUNT_ID_SYSTEM = 1;

    public long getId();

    public String getAccountName();

    public short getType();

    public State getState();

    public Date getRemoved();

    public String getNetworkDomain();
}
