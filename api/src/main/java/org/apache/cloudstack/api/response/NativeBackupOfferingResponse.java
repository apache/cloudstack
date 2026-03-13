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
package org.apache.cloudstack.api.response;

import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.api.EntityReference;
import org.apache.cloudstack.backup.NativeBackupOffering;

import java.util.Date;

@EntityReference(value = NativeBackupOffering.class)
public class NativeBackupOfferingResponse extends BaseResponse {

    @SerializedName(ApiConstants.ID)
    @Param(description = "ID of the offering.")
    private String id;

    @SerializedName(ApiConstants.NAME)
    @Param(description = "Name of the offering.")
    private String name;

    @SerializedName(ApiConstants.COMPRESS)
    @Param(description = "Whether the backups should be compressed or not.")
    private Boolean compress;

    @SerializedName(ApiConstants.VALIDATE)
    @Param(description = "Whether the backups should be validated or not.")
    private Boolean validate;

    @SerializedName(ApiConstants.VALIDATION_STEPS)
    @Param(description = "Which validation steps should be performed.")
    private String validationSteps;

    @SerializedName(ApiConstants.ALLOW_QUICK_RESTORE)
    @Param(description = "Whether the backups are allowed to be restored or not.")
    private Boolean allowQuickRestore;

    @SerializedName(ApiConstants.ALLOW_EXTRACT_FILE)
    @Param(description = "Whether files may be extracted from backups or not.")
    private Boolean allowExtractFile;

    @SerializedName(ApiConstants.BACKUP_CHAIN_SIZE)
    @Param(description = "Backup chain size for backups created with this offering.")
    private Integer backupChainSize;

    @SerializedName(ApiConstants.CREATED)
    @Param(description = "When the offering was created.")
    private Date created;

    @SerializedName(ApiConstants.REMOVED)
    @Param(description = "When the offering was removed.")
    private Date removed;

    public NativeBackupOfferingResponse(String id, String name, Boolean compress, Boolean validate, String validationSteps, Boolean allowQuickRestore, Boolean allowExtractFile,
            Integer chainSize, Date created,
            Date removed) {
        super("nativebackupoffering");
        this.id = id;
        this.name = name;
        this.compress = compress;
        this.validate = validate;
        this.validationSteps = validationSteps;
        this.allowQuickRestore = allowQuickRestore;
        this.allowExtractFile = allowExtractFile;
        this.backupChainSize = chainSize;
        this.created = created;
        this.removed = removed;
    }
}
