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

import com.cloud.utils.SerialVersionUID;

public class ResourceUnavailableException extends CloudException {
    private static final long serialVersionUID = SerialVersionUID.ResourceUnavailableException;

    Class<?> _scope;
    long _id;
    
    public ResourceUnavailableException(String msg, Class<?> scope, long resourceId) {
        this(msg, scope, resourceId, null);
    }
    
    public ResourceUnavailableException(String msg, Class<?> scope, long resourceId, Throwable cause) {
        super(new StringBuilder("Resource [").append(scope.getSimpleName()).append(":").append(resourceId).append("] is unreachable: ").append(msg).toString(), cause);
        _scope = scope;
        _id = resourceId;
    }
    
    public Class<?> getScope() {
        return _scope;
    }
    
    public long getResourceId() {
        return _id;
    }
}
