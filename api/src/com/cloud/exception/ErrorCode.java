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

import java.util.HashSet;

/**
 * ErrorCode is a standard error code given by the API to represent the error. 
 */
public class ErrorCode {
    String code;
    private static HashSet<ErrorCode> s_codes = new HashSet<ErrorCode>();
    
    public ErrorCode(String code) {
        this.code = code;
        assert !s_codes.contains(this) : "There is already an error code registered for this code: " + code;
        s_codes.add(this);
    }
    
    public String getCode() {
        return code;
    }
    
    @Override
    public int hashCode() {
        return code.hashCode();
    }
    
    @Override
    public boolean equals(Object that) {
        if (!(that instanceof ErrorCode)) {
            return false;
        }
        
        return this.code.equals(((ErrorCode)that).code);
    }
    
    public final static ErrorCode UnableToReachResource = new ErrorCode("resource.unavailable");
}
