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

public class DeleteTemplateFromS3Command extends Command {

    private S3TO s3;
    private Long templateId;
    private Long accountId;

    protected DeleteTemplateFromS3Command() {
        super();
    }

    public DeleteTemplateFromS3Command(final S3TO s3, final Long accountId,
            final Long templateId) {

        super();

        this.s3 = s3;
        this.accountId = accountId;
        this.templateId = templateId;

    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((accountId == null) ? 0 : accountId.hashCode());
        result = prime * result + ((s3 == null) ? 0 : s3.hashCode());
        result = prime * result
                + ((templateId == null) ? 0 : templateId.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object thatObject) {

        if (this == thatObject) {
            return true;
        }

        if (thatObject == null) {
            return false;
        }

        if (getClass() != thatObject.getClass()) {
            return false;
        }

        final DeleteTemplateFromS3Command thatCommand = (DeleteTemplateFromS3Command) thatObject;

        if (!(accountId == thatCommand.accountId)
                || (this.accountId != null && this.accountId
                        .equals(thatCommand.accountId))) {
            return false;
        }

        if (!(templateId == thatCommand.templateId)
                || (this.templateId != null && this.templateId
                        .equals(thatCommand.templateId))) {
            return false;
        }

        return true;

    }

    public S3TO getS3() {
        return s3;
    }

    public Long getTemplateId() {
        return templateId;
    }

    public Long getAccountId() {
        return accountId;
    }

    @Override
    public boolean executeInSequence() {
        return true;
    }

}
