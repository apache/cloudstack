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
package org.apache.cloudstack.backup;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import java.util.Date;
import java.util.UUID;

@Entity
@Table(name = "native_backup_offering")
public class NativeBackupOfferingVO implements NativeBackupOffering {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "uuid")
    private String uuid;

    @Column(name = "name")
    private String name;

    @Column(name = "compress")
    private boolean compress;

    @Column(name = "compression_library")
    private Backup.CompressionLibrary compressionLibrary;

    @Column (name = "validate")
    private boolean validate;

    @Column (name = "validation_steps")
    private String validationSteps;

    @Column(name = "allow_quick_restore")
    private boolean allowQuickRestore;

    @Column(name = "allow_extract_file")
    private boolean allowExtractFile;

    @Column(name = "backup_chain_size")
    private Integer backupChainSize;

    @Column(name = "created")
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date created;

    @Column(name = "removed")
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date removed;

    public NativeBackupOfferingVO() {
        this.uuid = UUID.randomUUID().toString();
        this.created = new Date();
        this.compressionLibrary = Backup.CompressionLibrary.zstd;
    }

    public NativeBackupOfferingVO(String name, boolean compress, boolean validate, String validationSteps, boolean allowQuickRestore, boolean allowExtractFile, Integer backupChainSize, Backup.CompressionLibrary compressionLibrary) {
        this();
        this.name = name;
        this.compress = compress;
        this.validate = validate;
        this.validationSteps = validationSteps;
        this.allowQuickRestore = allowQuickRestore;
        this.allowExtractFile = allowExtractFile;
        this.backupChainSize = backupChainSize;
        this.compressionLibrary = ObjectUtils.defaultIfNull(compressionLibrary, Backup.CompressionLibrary.zstd);
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public String getUuid() {
        return uuid;
    }

    @Override
    public String getExternalId() {
        return uuid;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return name;
    }

    @Override
    public long getZoneId() {
        return -1;
    }

    @Override
    public boolean isUserDrivenBackupAllowed() {
        return true;
    }

    @Override
    public String getProvider() {
        return "knib";
    }

    @Override
    public boolean isCompress() {
        return compress;
    }

    public Backup.CompressionLibrary getCompressionLibrary() {
        return compressionLibrary;
    }

    @Override
    public boolean isValidate() {
        return validate;
    }

    @Override
    public String getValidationSteps() {
        return validationSteps;
    }

    @Override
    public boolean isAllowQuickRestore() {
        return allowQuickRestore;
    }

    @Override
    public boolean isAllowExtractFile() {
        return allowExtractFile;
    }

    @Override
    public Integer getBackupChainSize() {
        return backupChainSize;
    }

    @Override
    public Date getCreated() {
        return created;
    }

    @Override
    public Date getRemoved() {
        return removed;
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this, ToStringStyle.JSON_STYLE);
    }
}
