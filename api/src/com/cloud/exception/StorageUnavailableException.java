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

/**
 * This exception is thrown when storage for a VM is unavailable. 
 * If the cause is due to storage pool unavailable, calling 
 * getOffendingObject() will return the object that we have
 * problem with.
 * 
 */
public class StorageUnavailableException extends Exception {
    Object _obj;

    private static final long serialVersionUID = SerialVersionUID.StorageUnavailableException;
    
	public StorageUnavailableException(String msg) {
		super(msg);
	}
	
	public StorageUnavailableException(String msg, Throwable cause) {
	    super(msg, cause);
	}
	
	public StorageUnavailableException(String msg, Object cause) {
	    super(msg);
	    _obj = cause;
	}
	
    public StorageUnavailableException(String msg, Object obj, Throwable cause) {
        super(msg, cause);
        _obj = obj;
    }
    
	/**
	 * @return object that caused this problem.  It can either be a StoragePool or volume.
	 */
	public Object getOffendingObject() {
	    return _obj;
	}
}
