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

public final class DownloadTemplateFromS3ToSecondaryStorageCommand extends Command {

    private final S3TO s3;
    private final Long accountId;
    private final Long templateId;
    private final String storagePath;

    public DownloadTemplateFromS3ToSecondaryStorageCommand(final S3TO s3,
        final Long accountId, final Long templateId,
        final String storagePath, final int wait) {

        super();

        this.s3 = s3;
        this.accountId = accountId;
        this.templateId = templateId;
        this.storagePath = storagePath;

        setWait(wait);

    }

    public S3TO getS3() {
        return this.s3;
    }

    public Long getAccountId() {
        return this.accountId;
    }

    public Long getTemplateId() {
        return this.templateId;
    }

    public String getStoragePath() {
        return this.storagePath;
    }

    @Override
    public boolean executeInSequence() {
        return true;
    }

}
