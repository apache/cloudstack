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

public interface ResponseObject {
    /**
     * Get the name of the API response
     * 
     * @return the name of the API response
     */
    String getResponseName();

    /**
     * Set the name of the API response
     * 
     * @param name
     */
    void setResponseName(String name);

    /**
     * Get the name of the API object
     * 
     * @return the name of the API object
     */
    String getObjectName();

    /**
     * Set the name of the APIobject
     * 
     * @param name
     */
    void setObjectName(String name);

    /**
     * Returns the object Id
     */
    Long getObjectId();

    /**
     * Returns the job id
     * 
     * @return
     */
    Long getJobId();

    /**
     * Sets the job id
     * 
     * @param jobId
     */
    void setJobId(Long jobId);

    /**
     * Returns the job status
     * 
     * @return
     */
    Integer getJobStatus();

    /**
     * 
     * @param jobStatus
     */
    void setJobStatus(Integer jobStatus);
}
