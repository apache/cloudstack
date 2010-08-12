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
import java.util.List;

import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Table;
import javax.persistence.Transient;

import com.cloud.utils.db.GenericDao;

@Entity
@Table(name="disk_offering")
@Inheritance(strategy=InheritanceType.JOINED)
@DiscriminatorColumn(name="type", discriminatorType=DiscriminatorType.STRING, length=32)
public class DiskOfferingVO {
    public enum Type {
        Disk,
        Service
    };
    
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="id")
    long id;

    @Column(name="domain_id")
    Long domainId;

    @Column(name="unique_name")
    private String uniqueName;
    
    @Column(name="name")
    private String name = null;

    @Column(name="display_text")
    private String displayText = null;

    @Column(name="disk_size")
    long diskSize;

    @Column(name="mirrored")
    boolean mirrored;
    
    @Column(name="tags")
    String tags;
    
    @Column(name="type")
    Type type;
    
    @Column(name=GenericDao.REMOVED_COLUMN)
    private Date removed;

    @Column(name=GenericDao.CREATED_COLUMN)
    private Date created;
    
    @Column(name="recreatable")
    private boolean recreatable;
    
    @Column(name="use_local_storage")
    private boolean useLocalStorage;

    
    public DiskOfferingVO() {
    }

    public DiskOfferingVO(long domainId, String name, String displayText, long diskSize, boolean mirrored, String tags) {
        this.domainId = domainId;
        this.name = name;
        this.displayText = displayText;
        this.diskSize = diskSize;
        this.mirrored = mirrored;
        this.tags = tags;
        this.recreatable = false;
        this.type = Type.Disk;
        this.useLocalStorage = false;
    }
    
    public DiskOfferingVO(String name, String displayText, boolean mirrored, String tags, boolean recreatable, boolean useLocalStorage) {
        this.domainId = null;
        this.type = Type.Service;
        this.name = name;
        this.displayText = displayText;
        this.mirrored = mirrored;
        this.tags = tags;
        this.recreatable = recreatable;
        this.useLocalStorage = useLocalStorage;
    }

    public Long getId() {
        return id;
    }
    
    public String getUniqueName() {
        return uniqueName;
    }
    
    public boolean getUseLocalStorage() {
        return useLocalStorage;
    }
    
    public Long getDomainId() {
        return domainId;
    }
    
    public Type getType() {
        return type;
    }
    
    public boolean isRecreatable() {
        return recreatable;
    }
    
    public void setDomainId(Long domainId) {
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

    public long getDiskSizeInBytes() {
        return diskSize * 1024 * 1024;
    }
    
    public void setDiskSize(long diskSize) {
        this.diskSize = diskSize;
    }

    public boolean isMirrored() {
        return mirrored;
    }
    public void setMirrored(boolean mirrored) {
        this.mirrored = mirrored;
    }

    public Date getRemoved() {
        return removed;
    }
    
	public Date getCreated() {
		return created;
	}
	
    protected void setTags(String tags) {
        this.tags = tags;
    }
    
    public String getTags() {
        return tags;
    }
    
    public void setUniqueName(String name) {
        this.uniqueName = name;
    }

    @Transient
    public String[] getTagsArray() {
        String tags = getTags();
        if (tags == null || tags.isEmpty()) {
            return new String[0];
        }
        
        return tags.split(",");
    }

    @Transient
    public boolean containsTag(String... tags) {
        if (this.tags == null) {
            return false;
        }
        
        for (String tag : tags) {
            if (!this.tags.matches(tag)) {
                return false;
            }
        }
        
        return true;
    }
    
    @Transient
    public void setTagsArray(List<String> newTags) {
        if (newTags.isEmpty()) {
            setTags(null);
            return;
        }
        
        StringBuilder buf = new StringBuilder();
        for (String tag : newTags) {
            buf.append(tag).append(",");
        }
        
        buf.delete(buf.length() - 1, buf.length());
        
        setTags(buf.toString());
    }
}
