/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cloudstack.storage.image.db;

import java.util.Date;
import java.util.Map;
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
import javax.persistence.Transient;

import org.apache.cloudstack.api.Identity;
import org.apache.cloudstack.storage.image.TemplateState;

import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.storage.Storage;
import com.cloud.storage.Storage.TemplateType;
import com.cloud.storage.VMTemplateVO;
import com.cloud.utils.db.GenericDao;
import com.cloud.utils.fsm.StateObject;

@Entity
@Table(name = "vm_template")
public class ImageDataVO implements Identity, StateObject<TemplateState> {
    @Id
    @TableGenerator(name = "vm_template_sq", table = "sequence", pkColumnName = "name", valueColumnName = "value", pkColumnValue = "vm_template_seq", allocationSize = 1)
    @Column(name = "id", nullable = false)
    private long id;

    @Column(name = "format")
    private String format;

    @Column(name = "unique_name")
    private String uniqueName;

    @Column(name = "name")
    private String name = null;

    @Column(name = "public")
    private boolean publicTemplate = true;

    @Column(name = "featured")
    private boolean featured;

    @Column(name = "type")
    private Storage.TemplateType templateType;

    @Column(name = "url")
    private String url = null;

    @Column(name = "hvm")
    private boolean requiresHvm;

    @Column(name = "bits")
    private int bits;

    @Temporal(value = TemporalType.TIMESTAMP)
    @Column(name = GenericDao.CREATED_COLUMN)
    private Date created = null;

    @Column(name = GenericDao.REMOVED)
    @Temporal(TemporalType.TIMESTAMP)
    private Date removed;

    @Column(name = "account_id")
    private long accountId;

    @Column(name = "checksum")
    private String checksum;

    @Column(name = "display_text", length = 4096)
    private String displayText;

    @Column(name = "enable_password")
    private boolean enablePassword;

    @Column(name = "guest_os_id")
    private long guestOSId;

    @Column(name = "bootable")
    private boolean bootable = true;

    @Column(name = "prepopulate")
    private boolean prepopulate = false;

    @Column(name = "cross_zones")
    private boolean crossZones = false;

    @Column(name = "hypervisor_type")
    @Enumerated(value = EnumType.STRING)
    private HypervisorType hypervisorType;

    @Column(name = "extractable")
    private boolean extractable = true;

    @Column(name = "source_template_id")
    private Long sourceTemplateId;

    @Column(name = "template_tag")
    private String templateTag;

    @Column(name = "uuid")
    private String uuid;

    @Column(name = "sort_key")
    private int sortKey;

    @Column(name = "enable_sshkey")
    private boolean enableSshKey;

    @Column(name = "image_data_store_id")
    private long imageDataStoreId;
    
    @Column(name = "size")
    private Long size;
    
    @Column(name = "state")
    private TemplateState state;
    
    @Column(name="update_count", updatable = true)
    protected long updatedCount;
    
    @Column(name = "updated")
    @Temporal(value = TemporalType.TIMESTAMP)
    Date updated;

    @Transient
    Map details;

    public String getUniqueName() {
        return uniqueName;
    }

    public void setUniqueName(String uniqueName) {
        this.uniqueName = uniqueName;
    }

    public ImageDataVO() {
        this.uuid = UUID.randomUUID().toString();
        this.state = TemplateState.Allocated;
        this.created = new Date();
    }

    public boolean getEnablePassword() {
        return enablePassword;
    }

    public String getFormat() {
        return format;
    }

    public void setEnablePassword(boolean enablePassword) {
        this.enablePassword = enablePassword;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public long getId() {
        return id;
    }

    public TemplateType getTemplateType() {
        return templateType;
    }

    public void setTemplateType(TemplateType type) {
        this.templateType = type;
    }

    public boolean requiresHvm() {
        return requiresHvm;
    }

    public void setRequireHvm(boolean hvm) {
        this.requiresHvm = hvm;
    }

    public int getBits() {
        return bits;
    }

    public void setBits(int bits) {
        this.bits = bits;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Date getRemoved() {
        return removed;
    }

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

    public void setUrl(String url) {
        this.url = url;
    }

    public long getAccountId() {
        return accountId;
    }

    public void setAccountId(long accountId) {
        this.accountId = accountId;
    }

    public String getChecksum() {
        return checksum;
    }

    public void setChecksum(String checksum) {
        this.checksum = checksum;
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

    public HypervisorType getHypervisorType() {
        return hypervisorType;
    }

    public void setHypervisorType(HypervisorType hyperType) {
        hypervisorType = hyperType;
    }

    public boolean isExtractable() {
        return extractable;
    }

    public void setExtractable(boolean extractable) {
        this.extractable = extractable;
    }

    public Long getSourceTemplateId() {
        return sourceTemplateId;
    }

    public void setSourceTemplateId(Long sourceTemplateId) {
        this.sourceTemplateId = sourceTemplateId;
    }

    public String getTemplateTag() {
        return templateTag;
    }

    public void setTemplateTag(String templateTag) {
        this.templateTag = templateTag;
    }

    public long getDomainId() {
        return -1;
    }

    @Override
    public String getUuid() {
        return this.uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public Map getDetails() {
        return this.details;
    }

    public void setDetails(Map details) {
        this.details = details;
    }

    @Override
    public boolean equals(Object that) {
        if (this == that) {
            return true;
        }
        if (!(that instanceof VMTemplateVO)) {
            return false;
        }
        VMTemplateVO other = (VMTemplateVO) that;

        return ((this.getUniqueName().equals(other.getUniqueName())));
    }

    @Override
    public int hashCode() {
        return uniqueName.hashCode();
    }

    @Transient
    String toString;

    @Override
    public String toString() {
        if (toString == null) {
            toString = new StringBuilder("Tmpl[").append(id).append("-").append(format).append("-").append(uniqueName).toString();
        }
        return toString;
    }

    public void setRemoved(Date removed) {
        this.removed = removed;
    }

    public void setSortKey(int key) {
        sortKey = key;
    }

    public int getSortKey() {
        return sortKey;
    }

    public boolean getEnableSshKey() {
        return enableSshKey;
    }

    public void setEnableSshKey(boolean enable) {
        enableSshKey = enable;
    }

    public Long getImageDataStoreId() {
        return this.imageDataStoreId;
    }

    public void setImageDataStoreId(long dataStoreId) {
        this.imageDataStoreId = dataStoreId;
    }
    
    public void setSize(Long size) {
        this.size = size;
    }
    
    public Long getSize() {
        return this.size;
    }
    
    public TemplateState getState() {
        return this.state;
    }
    
    public long getUpdatedCount() {
        return this.updatedCount;
    }
    
    public void incrUpdatedCount() {
        this.updatedCount++;
    }

    public void decrUpdatedCount() {
        this.updatedCount--;
    }
    
    public Date getUpdated() {
        return updated;
    }
    
    public void setUpdated(Date updated) {
        this.updated = updated;
    }

}
