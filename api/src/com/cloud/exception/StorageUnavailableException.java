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

import com.cloud.storage.StoragePool;
import com.cloud.utils.SerialVersionUID;

/**
 * This exception is thrown when storage for a VM is unavailable. 
 * If the cause is due to storage pool unavailable, calling 
 * getOffendingObject() will return the object that we have
 * problem with.
 * 
 */
public class StorageUnavailableException extends ResourceUnavailableException {
    private static final long serialVersionUID = SerialVersionUID.StorageUnavailableException;

    public StorageUnavailableException(String msg, long poolId) {
        this(msg, poolId, null);
    }
    public StorageUnavailableException(String msg, long poolId, Throwable cause) {
        this(msg, StoragePool.class, poolId, cause);
    }
    
    public StorageUnavailableException(String msg, Class<?> scope, long resourceId) {
        this(msg, scope, resourceId, null);
    }
    
    public StorageUnavailableException(String msg, Class<?> scope, long resourceId, Throwable th) {
        super(msg, scope, resourceId, th);
    }
}
