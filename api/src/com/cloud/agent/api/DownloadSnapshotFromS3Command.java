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

public class DownloadSnapshotFromS3Command extends SnapshotCommand {

    private S3TO s3;
    private String parent;

    protected DownloadSnapshotFromS3Command() {
        super();
    }

    public DownloadSnapshotFromS3Command(S3TO s3, String parent,
            String secondaryStorageUrl, Long dcId, Long accountId,
            Long volumeId, String backupUuid, int wait) {

        super(null, secondaryStorageUrl, backupUuid, "", dcId, accountId,
                volumeId);

        this.s3 = s3;
        this.parent = parent;
        setWait(wait);

    }

    public S3TO getS3() {
        return s3;
    }

    public void setS3(S3TO s3) {
        this.s3 = s3;
    }

    public String getParent() {
        return parent;
    }

    public void setParent(String parent) {
        this.parent = parent;
    }

}
