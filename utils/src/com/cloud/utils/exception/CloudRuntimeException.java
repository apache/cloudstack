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
package com.cloud.utils.exception;

import com.cloud.utils.SerialVersionUID;

/**
 * CloudRuntimeException is only here in case we ever want to provide more
 * information on runtime exceptions.  This class should only be used to
 * wrap exceptions that you know there's no point in dealing with.
 * This will be filled in more later as needed.
 */
public class CloudRuntimeException extends RuntimeCloudException {

    private static final long serialVersionUID = SerialVersionUID.CloudRuntimeException;
    
    public CloudRuntimeException(String message) {
        super(message);
    }
    
    public CloudRuntimeException(String message, Throwable th) {
        super(message, th);
    }
    
    protected CloudRuntimeException() {
        super();
    }
}
