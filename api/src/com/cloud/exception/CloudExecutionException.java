/**
 * Copyright (C) 2011 Citrix Systems, Inc.  All rights reserved
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

import java.util.HashMap;
import com.cloud.utils.exception.RuntimeCloudException;

import com.cloud.utils.SerialVersionUID;

/**
 * CloudExecutionException is a generic exception thrown by components in
 * CloudStack. It indicates an error in the execution of the business logic. 
 * When using this exception, it is important to give detail information 
 * about the error. At the entry points, this exception is caught but the
 * stack trace is not logged so the information has to be detail enough 
 * that one can find out what the error is simply by reading the error message.
 * 
 */
public class CloudExecutionException extends RuntimeCloudException {
    private final static long serialVersionUID = SerialVersionUID.CloudExecutionException;
    
    private final ErrorCode code;
    private final HashMap<String, Object> details;

    public CloudExecutionException(ErrorCode code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
        details = new HashMap<String, Object>();
    }
    
    public ErrorCode getErrorCode() {
        return code;
    }
    
    public String getErrorMessage() {
        return new StringBuilder("Error Code=").append(code).append("; Error Message=").append(super.toString()).toString();
    }
    
    @Override
    public String toString() {
        StringBuilder buff = new StringBuilder();
        buff.append("Error Code=").append(code);
        buff.append("; Error Message=").append(super.toString());
        if (details.size() > 0) {
            buff.append("; Error Details=").append(details.toString());
        }
        return buff.toString();
    }
}
