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
package com.cloud.offering;

import java.util.Date;

/**
 * Represents a disk offering that specifies what the end user needs in
 * the disk offering.
 * 
 */
public interface DiskOffering {
    long getId();

    String getUniqueName();

    boolean getUseLocalStorage();

    Long getDomainId();

    String getName();

    boolean getSystemUse();

    String getDisplayText();

    public String getTags();

    public String[] getTagsArray();

    Date getCreated();

    long getDiskSize();

    boolean isCustomized();

    void setDiskSize(long diskSize);
}
