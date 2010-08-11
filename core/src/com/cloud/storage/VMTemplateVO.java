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
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.TableGenerator;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import com.cloud.async.AsyncInstanceCreateStatus;
import com.cloud.storage.Storage.FileSystem;
import com.cloud.storage.Storage.ImageFormat;
import com.cloud.utils.db.GenericDao;
import com.google.gson.annotations.Expose;
import com.cloud.storage.Storage;

@Entity
@Table(name="vm_template")
public class VMTemplateVO implements VirtualMachineTemplate {
	@Id
    @TableGenerator(name="vm_template_sq", table="sequence", pkColumnName="name", valueColumnName="value", pkColumnValue="vm_template_seq", allocationSize=1)
    @Column(name="id", nullable = false)
	private long id;

	@Column(name="format")
	private Storage.ImageFormat format;
	
    @Column(name="unique_name")
    private String uniqueName;
    
    @Column(name="name")
	private String name = null;
    
    @Column(name="public")
	private boolean publicTemplate = true;
    
    @Column(name="featured")
    private boolean featured;
    
    @Column(name="type")
    private FileSystem fileSystem = null;
    
    @Column(name="url")
    private String url = null;
    
    @Column(name="hvm")
    private boolean requiresHvm;
    
    @Column(name="bits")
    private int bits;

    @Temporal(value=TemporalType.TIMESTAMP)
    @Column(name=GenericDao.CREATED_COLUMN)
    private Date created = null;
    
    @Temporal(value=TemporalType.TIMESTAMP)
    @Column(name=GenericDao.REMOVED_COLUMN)
    private Date removed;
    
    @Column(name="account_id")
    private long accountId;
    
    @Column(name="checksum")
    private String checksum;    
    
    @Column(name="display_text")
    private String displayText;
    
    @Column(name="enable_password")
    private boolean enablePassword;
    
    @Column(name="guest_os_id")
    private long guestOSId;
    
    @Column(name="bootable")
    private boolean bootable = true;
    
    @Column(name="prepopulate")
    private boolean prepopulate = false;
    
    @Column(name="cross_zones")
    private boolean crossZones = false;

	public String getUniqueName() {
		return uniqueName;
	}
	
	public void setUniqueName(String uniqueName) {
		this.uniqueName = uniqueName;
	}

	protected VMTemplateVO() {
    }

	/**
	 * Proper constructor for a new vm template.
	 */
    public VMTemplateVO(long id, String name, ImageFormat format, boolean isPublic, boolean featured, FileSystem fs, String url, boolean requiresHvm, int bits, long accountId, String cksum, String displayText, boolean enablePassword, long guestOSId, boolean bootable) {
	    this(id, generateUniqueName(id, accountId, name), name, format, isPublic, featured, fs, url, null, requiresHvm, bits, accountId, cksum, displayText, enablePassword, guestOSId, bootable);
    }

	public VMTemplateVO(Long id, String uniqueName, String name, ImageFormat format, boolean isPublic, boolean featured, FileSystem fs, String url, Date created, boolean requiresHvm, int bits, long accountId, String cksum, String displayText, boolean enablePassword, long guestOSId, boolean bootable) {
	    this.id = id;
	    this.name = name;
	    this.publicTemplate = isPublic;
	    this.featured = featured;
	    this.fileSystem = fs;
	    this.url = url;
	    this.requiresHvm = requiresHvm;
	    this.bits = bits;
	    this.accountId = accountId;
	    this.checksum = cksum;
	    this.uniqueName = uniqueName;
	    this.displayText = displayText;
	    this.enablePassword = enablePassword;
	    this.format = format;
	    this.created = created;
	    this.guestOSId = guestOSId;
	    this.bootable = bootable;
    }
	
	public boolean getEnablePassword() {
		return enablePassword;
	}
	
	public Storage.ImageFormat getFormat() {
	    return format;
	}

	public void setEnablePassword(boolean enablePassword) {
		this.enablePassword = enablePassword;
	}
	
	public void setFormat(ImageFormat format) {
	    this.format = format;
	}
	
	private static String generateUniqueName(long id, long userId, String displayName) {
	    StringBuilder name = new StringBuilder();
	    name.append(id);
	    name.append("-");
	    name.append(userId);
	    name.append("-");
	    name.append(UUID.nameUUIDFromBytes((displayName + System.currentTimeMillis()).getBytes()).toString());
	    return name.toString();
	}

	@Override
    public Long getId() {
		return id;
	}
	
	@Override
	public FileSystem getFileSystem() {
	    return fileSystem;
	}
	
	public void setFileSystem(FileSystem fs) {
		this.fileSystem = fs;
	}
	
	public boolean requiresHvm() {
	    return requiresHvm;
	}
	
	public int getBits() {
	    return bits;
	}
	
	public void setBits(int bits) {
		this.bits = bits;
	}
	
    @Override
	public String getName() {
		return name;
	}
    
    public void setName(String name) {
    	this.name = name;
    }
    
    public Date getRemoved() {
        return removed;
    }
	
    @Override
	public boolean isPublicTemplate() {
		return publicTemplate;
	}
    
    public void setPublicTemplate(boolean publicTemplate) {
    	this.publicTemplate = publicTemplate;
    }
    
    public boolean isFeatured() {
    	return featured;
    }
    
    public void setFeatured(boolean featured) {
    	this.featured = featured;
    }
    
	public Date getCreated() {
	    return created;
	}
	
	public String getUrl() {
		return url;
	}

	public boolean isRequiresHvm() {
		return requiresHvm;
	}
	
	public void setRequiresHvm(boolean value) {
		requiresHvm = value;
	}

	public long getAccountId() {
		return accountId;
	}

	public String getChecksum() {
		return checksum;
	}
    
    public String getDisplayText() {
		return displayText;
	}

	public void setDisplayText(String displayText) {
		this.displayText = displayText;
	}

	public long getGuestOSId() {
		return guestOSId;
	}
	
	public void setGuestOSId(long guestOSId) {
		this.guestOSId = guestOSId;
	}
	
	public boolean isBootable() {
		return bootable;
	}
	
	public void setBootable(boolean bootable) {
		this.bootable = bootable;
	}

	public void setPrepopulate(boolean prepopulate) {
		this.prepopulate = prepopulate;
	}

	public boolean isPrepopulate() {
		return prepopulate;
	}

	public void setCrossZones(boolean crossZones) {
		this.crossZones = crossZones;
	}

	public boolean isCrossZones() {
		return crossZones;
	}

	@Override
	public boolean equals(Object that) {
		if (this == that )
			return true;
		if (!(that instanceof VMTemplateVO)){
			return false;
		}
		VMTemplateVO other = (VMTemplateVO)that;
		
		return (this.getUniqueName().equals(other.getUniqueName()));
		
	}

	@Override
	public int hashCode() {
		return uniqueName.hashCode();
	}
	
}
