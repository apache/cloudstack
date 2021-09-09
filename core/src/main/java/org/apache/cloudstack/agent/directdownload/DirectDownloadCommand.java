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

import java.util.Map;

import org.apache.cloudstack.storage.command.StorageSubSystemCommand;
import org.apache.cloudstack.storage.to.PrimaryDataStoreTO;
import org.apache.cloudstack.storage.to.TemplateObjectTO;

import com.cloud.storage.Storage;

public abstract class DirectDownloadCommand extends StorageSubSystemCommand {

    public enum DownloadProtocol {
        HTTP, HTTPS, NFS, METALINK
    }

    private String url;
    private Long templateId;
    private TemplateObjectTO destData;
    private PrimaryDataStoreTO destPool;
    private String checksum;
    private Map<String, String> headers;
    private Integer connectTimeout;
    private Integer soTimeout;
    private Integer connectionRequestTimeout;
    private Long templateSize;
    private Storage.ImageFormat format;

    protected DirectDownloadCommand (final String url, final Long templateId, final PrimaryDataStoreTO destPool, final String checksum, final Map<String, String> headers, final Integer connectTimeout, final Integer soTimeout, final Integer connectionRequestTimeout) {
        this.url = url;
        this.templateId = templateId;
        this.destData = destData;
        this.destPool = destPool;
        this.checksum = checksum;
        this.headers = headers;
        this.connectTimeout = connectTimeout;
        this.soTimeout = soTimeout;
        this.connectionRequestTimeout = connectionRequestTimeout;
    }

    public String getUrl() {
        return url;
    }

    public Long getTemplateId() {
        return templateId;
    }

    public TemplateObjectTO getDestData() {
        return destData;
    }

    public void setDestData(TemplateObjectTO destData) {
         this.destData = destData;
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

    public Integer getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(Integer connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public Integer getSoTimeout() {
        return soTimeout;
    }

    public void setSoTimeout(Integer soTimeout) {
        this.soTimeout = soTimeout;
    }

    public Integer getConnectionRequestTimeout() {
        return connectionRequestTimeout;
    }

    public void setConnectionRequestTimeout(Integer connectionRequestTimeout) {
        this.connectionRequestTimeout = connectionRequestTimeout;
    }

    public Long getTemplateSize() {
        return templateSize;
    }

    public void setTemplateSize(Long templateSize) {
        this.templateSize = templateSize;
    }

    public Storage.ImageFormat getFormat() {
        return format;
    }

    public void setFormat(Storage.ImageFormat format) {
        this.format = format;
    }

    @Override
    public void setExecuteInSequence(boolean inSeq) {
    }

    @Override
    public boolean executeInSequence() {
        return false;
    }

    public int getWaitInMillSeconds() {
        return getWait() * 1000;
    }
}
