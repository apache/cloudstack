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

package com.cloud.migration;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import com.cloud.utils.db.GenericDao;

@Entity
@Table(name="disk_offering")
public class DiskOffering20VO {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="id")
    Long id;

    @Column(name="domain_id")
    long domainId;

    @Column(name="name")
    private String name = null;

    @Column(name="display_text")
    private String displayText = null;

    @Column(name="disk_size")
    long diskSize;

    @Column(name="mirrored")
    boolean mirrored;

    @Column(name=GenericDao.REMOVED_COLUMN)
    private Date removed;

    public DiskOffering20VO() {
    }

    public DiskOffering20VO(long domainId, String name, String displayText, long diskSize, boolean mirrored) {
        this.domainId = domainId;
        this.name = name;
        this.displayText = displayText;
        this.diskSize = diskSize;
        this.mirrored = mirrored;
    }

    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }

    public long getDomainId() {
        return domainId;
    }
    public void setDomainId(long domainId) {
        this.domainId = domainId;
    }

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    public String getDisplayText() {
        return displayText;
    }
    public void setDisplayText(String displayText) {
        this.displayText = displayText;
    }

    public long getDiskSize() {
        return diskSize;
    }
    public void setDiskSize(long diskSize) {
        this.diskSize = diskSize;
    }

    public boolean getMirrored() {
        return mirrored;
    }
    public void setMirrored(boolean mirrored) {
        this.mirrored = mirrored;
    }

    public Date getRemoved() {
        return removed;
    }
}
