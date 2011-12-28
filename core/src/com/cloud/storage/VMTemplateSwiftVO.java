/**
 * Copyright (C) 2011 Citrix Systems, Inc.  All rights reserved.
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

import com.cloud.utils.db.GenericDaoBase;

/**
 * Join table for swift and templates
 * 
 * @author Anthony Xu
 * 
 */
@Entity
@Table(name = "template_swift_ref")
public class VMTemplateSwiftVO {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "swift_id")
    private long swiftId;

    @Column(name = "template_id")
    private long templateId;

    @Column(name = GenericDaoBase.CREATED_COLUMN)
    private Date created = null;

    @Column(name = "path")
    private String path;

    @Column(name = "size")
    private long size;

    @Column(name = "physical_size")
    private long physicalSize;

    public VMTemplateSwiftVO(long swiftId, long templateId, Date created, String path, long size, long physicalSize) {
        this.swiftId = swiftId;
        this.templateId = templateId;
        this.created = created;
        this.path = path;
        this.size = size;
        this.physicalSize = physicalSize;
    }

    protected VMTemplateSwiftVO() {

    }

    public long getTemplateId() {
        return templateId;
    }

    public long getId() {
        return id;
    }

    public Date getCreated() {
        return created;
    }

    public String getPath() {
        return path;
    }

    public long getSwiftId() {
        return swiftId;
    }

    public long getSize() {
        return size;
    }

    public long getPhysicalSize() {
        return physicalSize;
    }

    @Override
    public String toString() {
        return new StringBuilder("TmplSwift[").append(id).append("-").append(templateId).append("-").append(swiftId).append("]").toString();
    }

}
