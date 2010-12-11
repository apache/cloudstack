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
package com.cloud.network.dao;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name="op_networks")
public class NetworkOpVO {
    
    @Id
    @Column(name="id")
    long id;
    
    @Column(name="nics_count")
    int activeNicsCount;
    
    @Column(name="gc")
    boolean garbageCollected;
    
    @Column(name="check_for_gc")
    boolean checkForGc;

    protected NetworkOpVO() {
    }
    
    public NetworkOpVO(long id, boolean gc) {
        this.id = id;
        this.garbageCollected = gc;
        this.checkForGc = gc;
        this.activeNicsCount = 0;
    }
    
    public long getId() {
        return id;
    }
    
    public long getActiveNicsCount() {
        return activeNicsCount;
    }
    
    public void setActiveNicsCount(int number) {
        activeNicsCount += number;
    }
    
    public boolean isGarbageCollected() {
        return garbageCollected;
    }
    
    public boolean isCheckForGc() {
        return checkForGc;
    }
    
    public void setCheckForGc(boolean check) {
        checkForGc = check;
    }
}
