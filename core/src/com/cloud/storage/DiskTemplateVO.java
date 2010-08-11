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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import com.cloud.utils.db.GenericDao;

/**
 * @author ahuang
 *
 */
@Entity
@Table(name="disk_template_ref")
public class DiskTemplateVO {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="id")
    Long id;
    
    @Column(name="description")
    String description;
    
    @Column(name="host")
    String host;
    
    @Column(name="path")
    String path;
    
    @Column(name="size")
    long size;
    
    @Column(name="type")
    String type;
    
    @Column(name=GenericDao.CREATED_COLUMN)
    Date created;
    
    @Column(name=GenericDao.REMOVED_COLUMN)
    Date removed;
    
    public DiskTemplateVO(Long id, String description, String path, long size, String type) {
        this.id = id;
        this.description = description;
        this.path = path;
        this.size = size;
        this.type = type;
    }

    public Long getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public String getHost() {
        return host;
    }

    public String getPath() {
        return path;
    }

    public long getSize() {
        return size;
    }

    public String getType() {
        return type;
    }

    public Date getCreated() {
        return created;
    }

    public Date getRemoved() {
        return removed;
    }
    
    protected DiskTemplateVO() {
    }
}
