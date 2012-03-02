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
import com.cloud.utils.exception.CSExceptionErrorCode;

/**
 * ExecutionException is a generic exception to indicate that execution has
 * a problem so that the method can catch it and handle it properly.  This 
 * exception should never be declared to be thrown out of
 * a public method.  
 *
 */
public class ExecutionException extends Exception {
    private static final long serialVersionUID = SerialVersionUID.ExecutionException;
    
	// This holds an error code. Set this before throwing an exception, if applicable.
	protected int csErrorCode;
	
    public ExecutionException(String msg, Throwable cause) {
        super(msg, cause);
        setCSErrorCode(CSExceptionErrorCode.getCSErrCode(this.getClass().getName()));
    }
    
    public ExecutionException(String msg) {
        super(msg);
    }
    
	public void setCSErrorCode(int cserrcode) {
		this.csErrorCode = cserrcode;
	}
	
	public int getCSErrorCode() {
		return this.csErrorCode;
	}
}
