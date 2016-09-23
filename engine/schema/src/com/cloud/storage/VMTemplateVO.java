// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package com.cloud.storage;

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

import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.storage.Storage.ImageFormat;
import com.cloud.storage.Storage.TemplateType;
import com.cloud.template.VirtualMachineTemplate;
import com.cloud.utils.db.GenericDao;

@Entity
@Table(name = "vm_template")
public class VMTemplateVO implements VirtualMachineTemplate {
    @Id
    @TableGenerator(name = "vm_template_sq", table = "sequence", pkColumnName = "name", valueColumnName = "value", pkColumnValue = "vm_template_seq", allocationSize = 1)
    @Column(name = "id", nullable = false)
    private long id;

    @Column(name = "format")
    private Storage.ImageFormat format;

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

    @Column(name = "url", length = 2048)
    private String url = null;

    @Column(name = "hvm")
    private boolean requiresHvm;

    @Column(name = "bits")
    private int bits;

    @Temporal(value = TemporalType.TIMESTAMP)
    @Column(name = GenericDao.CREATED_COLUMN)
    private Date created = null;

    @Column(name = GenericDao.REMOVED_COLUMN)
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

    @Column(name = "state")
    @Enumerated(EnumType.STRING)
    private State state;

    @Column(name = "template_tag")
    private String templateTag;

    @Column(name = "uuid")
    private String uuid;

    @Column(name = "sort_key")
    private int sortKey;

    @Column(name = "enable_sshkey")
    private boolean enableSshKey;

    @Column(name = "size")
    private Long size;

    @Column(name = "update_count", updatable = true)
    protected long updatedCount;

    @Column(name = "updated")
    @Temporal(value = TemporalType.TIMESTAMP)
    Date updated;

    @Transient
    Map<String, String> details;

    @Column(name = "dynamically_scalable")
    protected boolean dynamicallyScalable;

    @Override
    public String getUniqueName() {
        return uniqueName;
    }

    public void setUniqueName(String uniqueName) {
        this.uniqueName = uniqueName;
    }

    public VMTemplateVO() {
        uuid = UUID.randomUUID().toString();
    }

    //FIXME - Remove unwanted constructors.
    private VMTemplateVO(long id, String name, ImageFormat format, boolean isPublic, boolean featured, boolean isExtractable, TemplateType type, String url,
            boolean requiresHvm, int bits, long accountId, String cksum, String displayText, boolean enablePassword, long guestOSId, boolean bootable,
            HypervisorType hyperType, Map<String, String> details) {
        this(id,
            generateUniqueName(id, accountId, name),
            name,
            format,
            isPublic,
            featured,
            isExtractable,
            type,
            url,
            null,
            requiresHvm,
            bits,
            accountId,
            cksum,
            displayText,
            enablePassword,
            guestOSId,
            bootable,
            hyperType,
            details);
        uuid = UUID.randomUUID().toString();
    }

    public VMTemplateVO(long id, String name, ImageFormat format, boolean isPublic, boolean featured, boolean isExtractable, TemplateType type, String url,
            boolean requiresHvm, int bits, long accountId, String cksum, String displayText, boolean enablePassword, long guestOSId, boolean bootable,
            HypervisorType hyperType, String templateTag, Map<String, String> details, boolean sshKeyEnabled, boolean isDynamicallyScalable) {
        this(id,
            name,
            format,
            isPublic,
            featured,
            isExtractable,
            type,
            url,
            requiresHvm,
            bits,
            accountId,
            cksum,
            displayText,
            enablePassword,
            guestOSId,
            bootable,
            hyperType,
            details);
        this.templateTag = templateTag;
        uuid = UUID.randomUUID().toString();
        enableSshKey = sshKeyEnabled;
        dynamicallyScalable = isDynamicallyScalable;
        state = State.Active;
    }

    public static VMTemplateVO createPreHostIso(Long id, String uniqueName, String name, ImageFormat format, boolean isPublic, boolean featured, TemplateType type,
        String url, Date created, boolean requiresHvm, int bits, long accountId, String cksum, String displayText, boolean enablePassword, long guestOSId,
        boolean bootable, HypervisorType hyperType) {
        VMTemplateVO template =
            new VMTemplateVO(id, uniqueName, name, format, isPublic, featured, type, url, created, requiresHvm, bits, accountId, cksum, displayText, enablePassword,
                guestOSId, bootable, hyperType);
        return template;
    }

    public VMTemplateVO(Long id, String uniqueName, String name, ImageFormat format, boolean isPublic, boolean featured, TemplateType type, String url, Date created,
            boolean requiresHvm, int bits, long accountId, String cksum, String displayText, boolean enablePassword, long guestOSId, boolean bootable,
            HypervisorType hyperType) {
        this.id = id;
        this.name = name;
        publicTemplate = isPublic;
        this.featured = featured;
        templateType = type;
        this.url = url;
        this.requiresHvm = requiresHvm;
        this.bits = bits;
        this.accountId = accountId;
        checksum = cksum;
        this.uniqueName = uniqueName;
        this.displayText = displayText;
        this.enablePassword = enablePassword;
        this.format = format;
        this.created = created;
        this.guestOSId = guestOSId;
        this.bootable = bootable;
        hypervisorType = hyperType;
        uuid = UUID.randomUUID().toString();
        state = State.Active;
    }

    //FIXME - Remove unwanted constructors. Made them private for now
    private VMTemplateVO(Long id, String uniqueName, String name, ImageFormat format, boolean isPublic, boolean featured, boolean isExtractable, TemplateType type,
            String url, Date created, boolean requiresHvm, int bits, long accountId, String cksum, String displayText, boolean enablePassword, long guestOSId,
            boolean bootable, HypervisorType hyperType, Map<String, String> details) {
        this(id,
            uniqueName,
            name,
            format,
            isPublic,
            featured,
            type,
            url,
            created,
            requiresHvm,
            bits,
            accountId,
            cksum,
            displayText,
            enablePassword,
            guestOSId,
            bootable,
            hyperType);
        extractable = isExtractable;
        uuid = UUID.randomUUID().toString();
        this.details = details;
        state = State.Active;
    }

    @Override
    public boolean getEnablePassword() {
        return enablePassword;
    }

    @Override
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
    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public TemplateType getTemplateType() {
        return templateType;
    }

    public void setTemplateType(TemplateType type) {
        templateType = type;
    }

    public boolean requiresHvm() {
        return requiresHvm;
    }

    @Override
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

    @Override
    public boolean isFeatured() {
        return featured;
    }

    public void setFeatured(boolean featured) {
        this.featured = featured;
    }

    @Override
    public Date getCreated() {
        return created;
    }

    @Override
    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    @Override
    public boolean isRequiresHvm() {
        return requiresHvm;
    }

    public void setRequiresHvm(boolean value) {
        requiresHvm = value;
    }

    @Override
    public long getAccountId() {
        return accountId;
    }

    @Override
    public String getChecksum() {
        return checksum;
    }

    public void setChecksum(String checksum) {
        this.checksum = checksum;
    }

    @Override
    public String getDisplayText() {
        return displayText;
    }

    public void setDisplayText(String displayText) {
        this.displayText = displayText;
    }

    @Override
    public long getGuestOSId() {
        return guestOSId;
    }

    public void setGuestOSId(long guestOSId) {
        this.guestOSId = guestOSId;
    }

    @Override
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

    @Override
    public boolean isCrossZones() {
        return crossZones;
    }

    @Override
    public HypervisorType getHypervisorType() {
        return hypervisorType;
    }

    public void setHypervisorType(HypervisorType hyperType) {
        hypervisorType = hyperType;
    }

    @Override
    public boolean isExtractable() {
        return extractable;
    }

    public void setExtractable(boolean extractable) {
        this.extractable = extractable;
    }

    @Override
    public Long getSourceTemplateId() {
        return sourceTemplateId;
    }

    public void setSourceTemplateId(Long sourceTemplateId) {
        this.sourceTemplateId = sourceTemplateId;
    }

    @Override
    public String getTemplateTag() {
        return templateTag;
    }

    public void setTemplateTag(String templateTag) {
        this.templateTag = templateTag;
    }

    @Override
    public long getDomainId() {
        return -1;
    }

    public void setAccountId(long accountId) {
        this.accountId = accountId;
    }

    @Override
    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public Map<String, String> getDetails() {
        return details;
    }

    public void setDetails(Map<String, String> details) {
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
        VMTemplateVO other = (VMTemplateVO)that;

        return ((getUniqueName().equals(other.getUniqueName())));
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

    public void setDynamicallyScalable(boolean dynamicallyScalable) {
        this.dynamicallyScalable = dynamicallyScalable;
    }

    @Override
    public boolean isDynamicallyScalable() {
        return dynamicallyScalable;
    }

    @Override
    public boolean getEnableSshKey() {
        return enableSshKey;
    }

    public void setEnableSshKey(boolean enable) {
        enableSshKey = enable;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public Long getSize() {
        return size;
    }

    @Override
    public long getUpdatedCount() {
        return updatedCount;
    }

    @Override
    public void incrUpdatedCount() {
        updatedCount++;
    }

    public void decrUpdatedCount() {
        updatedCount--;
    }

    @Override
    public Date getUpdated() {
        return updated;
    }

    public void setUpdated(Date updated) {
        this.updated = updated;
    }

    @Override
    public Class<?> getEntityType() {
        return VirtualMachineTemplate.class;
    }
}
