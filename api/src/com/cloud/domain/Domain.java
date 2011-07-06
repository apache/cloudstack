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

/**
 * 
 */
package com.cloud.domain;

import java.util.Date;

import com.cloud.user.OwnedBy;

/**
 * Domain defines the Domain object.
 */
public interface Domain extends OwnedBy {
    public static final long ROOT_DOMAIN = 1L;
    
    enum State {Active, Inactive};
    
    long getId();

    Long getParent();
    
    void setParent(Long parent);

    String getName();
    
    void setName(String name);
    
    Date getRemoved();
    
    String getPath();
    
    void setPath(String path);
    
    int getLevel();
    
    int getChildCount();
    
    long getNextChildSeq();

    State getState();

    void setState(State state);

    String getNetworkDomain();
}
