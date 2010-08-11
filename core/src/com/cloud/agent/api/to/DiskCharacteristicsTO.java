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

import com.cloud.storage.DiskOfferingVO;
import com.cloud.storage.Volume;

public class DiskCharacteristicsTO {
    private long size;
    private String[] tags;
    private Volume.VolumeType type;
    private String name;
    private boolean useLocalStorage;
    private boolean recreatable;
    
    protected DiskCharacteristicsTO() {
    }
    
    public DiskCharacteristicsTO(Volume.VolumeType type, String name, long size, String[] tags, boolean useLocalStorage, boolean recreatable) {
        this.type = type;
        this.name = name;
        this.size = size;
        this.tags = tags;
        this.useLocalStorage = useLocalStorage;
        this.recreatable = recreatable;
    }
    
    public DiskCharacteristicsTO(Volume.VolumeType type, String name, DiskOfferingVO offering, long size) {
        this(type, name, size, offering.getTagsArray(), offering.getUseLocalStorage(), offering.isRecreatable());
    }
    
    public DiskCharacteristicsTO(Volume.VolumeType type, String name, DiskOfferingVO offering) {
        this(type, name, offering.getDiskSizeInBytes(), offering.getTagsArray(), offering.getUseLocalStorage(), offering.isRecreatable());
    }
    
    public long getSize() {
        return size;
    }
    
    public String getName() {
        return name;
    }

    public String[] getTags() {
        return tags;
    }

    public Volume.VolumeType getType() {
        return type;
    }
    
    public boolean useLocalStorage() {
        return useLocalStorage;
    }
    
    public boolean isRecreatable() {
        return recreatable;
    }
    
    @Override
    public String toString() {
        return new StringBuilder("DskChr[").append(type).append("|").append(size).append("|").append("]").toString();
    }
}
