/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.cloud.agent.api;

import com.cloud.agent.api.to.S3TO;

public class UploadTemplateToS3FromSecondaryStorageCommand extends Command {

    private final S3TO s3;
    private final String storagePath;
    private final Long dataCenterId;
    private final Long accountId;
    private final Long templateId;

    public UploadTemplateToS3FromSecondaryStorageCommand(final S3TO s3,
        final String storagePath, final Long dataCenterId, final Long accountId,
        final Long templateId) {

        super();

        this.s3 = s3;
        this.storagePath = storagePath;
        this.dataCenterId = dataCenterId;
        this.accountId = accountId;
        this.templateId = templateId;

    }

    @Override
    public boolean executeInSequence() {
        return false;
    }

    @Override
    public boolean equals(final Object thatObject) {

        if (this == thatObject) {
            return true;
        }

        if (thatObject == null || getClass() != thatObject.getClass()) {
            return false;
        }

        final UploadTemplateToS3FromSecondaryStorageCommand thatCommand =
                (UploadTemplateToS3FromSecondaryStorageCommand) thatObject;

        if (this.accountId != null ? !this.accountId.equals(thatCommand
                .accountId) : thatCommand.accountId != null) {
            return false;
        }

        if (this.dataCenterId != null ? !this.dataCenterId.equals(thatCommand
                .dataCenterId) : thatCommand.dataCenterId != null) {
            return false;
        }

        if (this.s3 != null ? !this.s3.equals(thatCommand.s3) : thatCommand.s3 != null) {
            return false;
        }

        if (this.storagePath != null ? !this.storagePath.equals(thatCommand
                .storagePath) : thatCommand.storagePath != null) {
            return false;
        }

        if (this.templateId != null ? !this.templateId.equals(thatCommand.templateId) :
                thatCommand.templateId != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = this.s3 != null ? this.s3.hashCode() : 0;
        result = 31 * result + (this.storagePath != null ? this.storagePath.hashCode() : 0);
        result = 31 * result + (this.dataCenterId != null ? this.dataCenterId.hashCode() : 0);
        result = 31 * result + (this.accountId != null ? this.accountId.hashCode() : 0);
        result = 31 * result + (this.templateId != null ? this.templateId.hashCode() : 0);
        return result;
    }

    public S3TO getS3() {
        return this.s3;
    }

    public String getStoragePath() {
        return this.storagePath;
    }

    public Long getDataCenterId() {
        return this.dataCenterId;
    }

    public Long getAccountId() {
        return this.accountId;
    }

    public Long getTemplateId() {
        return this.templateId;
    }

}
