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

package org.apache.cloudstack.agent.directdownload;

import org.apache.cloudstack.storage.command.StorageSubSystemCommand;
import org.apache.cloudstack.storage.to.PrimaryDataStoreTO;

import java.util.Map;

public abstract class DirectDownloadCommand extends StorageSubSystemCommand {

    public enum DownloadProtocol {
        HTTP, HTTPS, NFS, METALINK
    }

    private String url;
    private Long templateId;
    private PrimaryDataStoreTO destPool;
    private String checksum;
    private Map<String, String> headers;

    protected DirectDownloadCommand (final String url, final Long templateId, final PrimaryDataStoreTO destPool, final String checksum, final Map<String, String> headers) {
        this.url = url;
        this.templateId = templateId;
        this.destPool = destPool;
        this.checksum = checksum;
        this.headers = headers;
    }

    public String getUrl() {
        return url;
    }

    public Long getTemplateId() {
        return templateId;
    }

    public PrimaryDataStoreTO getDestPool() {
        return destPool;
    }

    public String getChecksum() {
        return checksum;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    @Override
    public void setExecuteInSequence(boolean inSeq) {
    }

    @Override
    public boolean executeInSequence() {
        return false;
    }
}
