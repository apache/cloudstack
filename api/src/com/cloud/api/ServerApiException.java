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

package com.cloud.api;
import com.cloud.utils.exception.RuntimeCloudException;

@SuppressWarnings("serial")
public class ServerApiException extends RuntimeCloudException {
    private int _errorCode;
    private String _description;

    public ServerApiException() {
        _errorCode = 0;
        _description = null;
    }

    public ServerApiException(int errorCode, String description) {
        _errorCode = errorCode;
        _description = description;
    }

    public int getErrorCode() {
        return _errorCode;
    }

    public void setErrorCode(int errorCode) {
        _errorCode = errorCode;
    }

    public String getDescription() {
        return _description;
    }

    public void setDescription(String description) {
        _description = description;
    }
}
