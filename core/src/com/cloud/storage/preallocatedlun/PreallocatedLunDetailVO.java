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
package com.cloud.storage.preallocatedlun;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name="ext_lun_details")
public class PreallocatedLunDetailVO {
    
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="id")
    private long id;
   
    @Column(name="ext_lun_id")
    private long lunId;
    
    @Column(name="tag")
    private String tag;
    
    protected PreallocatedLunDetailVO() {
    }

    public long getId() {
        return id;
    }

    public long getLunId() {
        return lunId;
    }

    public String getTag() {
        return tag;
    }
    
    public PreallocatedLunDetailVO(long lunId, String tag) {
        this.lunId = lunId;
        this.tag  = tag;
    }

}
