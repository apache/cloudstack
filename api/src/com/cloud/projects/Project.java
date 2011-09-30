/**
 * Copyright (C) 2011 Citrix Systems, Inc.  All rights reserved
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
package com.cloud.projects;

import java.util.Date;

import com.cloud.domain.PartOf;

public interface Project extends PartOf{
    public enum State {Active, Inactive, Suspended}

    String getDisplayText();

    long getDomainId();

    long getId();

    Date getCreated();

    Date getRemoved();

    String getName();
    
    long getProjectAccountId();

    State getState();

    void setState(State state);

}
