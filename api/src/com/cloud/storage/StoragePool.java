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
package com.cloud.storage;

import java.util.Date;

import com.cloud.storage.Storage.StoragePoolType;

/**
 * @author chiradeep
 * 
 */

public interface StoragePool {

    /**
     * @return id of the pool.
     */
    long getId();

    /**
     * @return name of the pool.
     */
    String getName();

    /***
     * 
     * @return unique identifier
     */
    String getUuid();

    /**
     * @return the type of pool.
     */
    StoragePoolType getPoolType();

    /**
     * @return the date the pool first registered
     */
    Date getCreated();

    /**
     * @return the last time the state of this pool was modified.
     */
    Date getUpdateTime();

    /**
     * @return availability zone.
     */
    long getDataCenterId();

    /**
     * @return capacity of storage poolin bytes
     */
    long getCapacityBytes();

    /**
     * @return available storage in bytes
     */
    long getAvailableBytes();

    Long getClusterId();

    /**
     * @return the fqdn or ip address of the storage host
     */
    String getHostAddress();

    /**
     * @return the filesystem path of the pool on the storage host (server)
     */
    String getPath();

    /**
     * @return the storage pool represents a shared storage resource
     */
    boolean isShared();

    /**
     * @return the storage pool represents a local storage resource
     */
    boolean isLocal();

    /**
     * @return the storage pool status
     */
    StoragePoolStatus getStatus();

    int getPort();

    Long getPodId();
}
