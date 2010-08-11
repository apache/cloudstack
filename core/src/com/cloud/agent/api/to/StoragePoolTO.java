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
package com.cloud.agent.api.to;

import com.cloud.storage.StoragePoolVO;
import com.cloud.storage.Storage.StoragePoolType;


public class StoragePoolTO {
    long id;
    String uuid;
    String host;
    String path;
    int port;
    StoragePoolType type;
    
    public StoragePoolTO(StoragePoolVO pool) {
        this.id = pool.getId();
        this.host = pool.getHostAddress();
        this.port = pool.getPort();
        this.path = pool.getPath();
        this.type = pool.getPoolType();
        this.uuid = pool.getUuid();
    }

    public long getId() {
        return id;
    }

    public String getUuid() {
        return uuid;
    }

    public String getHost() {
        return host;
    }

    public String getPath() {
        return path;
    }

    public int getPort() {
        return port;
    }
    
    public StoragePoolType getType() {
        return type;
    }
    
    protected StoragePoolTO() {
    }
    
    @Override
    public String toString() {
        return new StringBuilder("Pool[").append(id).append("|").append(host).append(":").append(port).append("|").append(path).append("]").toString();
    }
}
