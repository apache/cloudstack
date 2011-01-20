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
package com.cloud.agent.api;

import com.cloud.storage.StorageStats;

public class GetStorageStatsAnswer extends Answer implements StorageStats {
    protected GetStorageStatsAnswer() {
    }
    
    protected long used;
    
    protected long capacity;
    
    public long getByteUsed() {
        return used;
    }
    
    public long getCapacityBytes() {
        return capacity;
    }
    
    
    public GetStorageStatsAnswer(GetStorageStatsCommand cmd, long capacity, long used) {
        super(cmd, true, null);
        this.capacity = capacity;
        this.used = used;
    }
    
    public GetStorageStatsAnswer(GetStorageStatsCommand cmd, String details) {
        super(cmd, false, details);
    }

}
