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

package org.apache.cloudstack.storage.template;


import com.cloud.storage.Storage;

public class UploadEntity {
    private long downloadedsize;
    private String filename;
    private String installPathPrefix;
    private String templatePath;
    private boolean isHvm;
    private Storage.ImageFormat format;
    private String uuid;
    private long entityId;
    private String chksum;
    private long physicalSize;
    private int maxSizeInGB;
    private String description;
    private long contentLength;

    public static enum ResourceType {
        VOLUME, TEMPLATE
    }

    public static enum Status {
        UNKNOWN, IN_PROGRESS, COMPLETED, ERROR
    }

    private Status uploadState;
    private String errorMessage=null;
    private ResourceType resourceType;
    private long virtualSize;
    private boolean isMetaDataPopulated;

    public static long s_maxTemplateSize = 50L * 1024L * 1024L * 1024L;

    public UploadEntity(String uuid, long entityId, Status status, String filename, String installPathPrefix){
        this.uuid=uuid;
        this.uploadState=status;
        this.downloadedsize=0l;
        this.filename=filename;
        this.installPathPrefix = installPathPrefix;
        this.entityId=entityId;
    }

    public UploadEntity(){

    }

    public void setStatus(Status status) {
        this.uploadState = status;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage=errorMessage;
    }

    public long getDownloadedsize() {
        return downloadedsize;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public Status getUploadState() {
        return uploadState;
    }

    public void incremetByteCount(long numberOfBytes) {
           this.downloadedsize+= numberOfBytes;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }
    public String getInstallPathPrefix() {
        return installPathPrefix;
    }

    public void setInstallPathPrefix(String absoluteFilePath) {
        this.installPathPrefix = absoluteFilePath;
    }

    public String getTmpltPath() {
        return templatePath;
    }

    public void setTemplatePath(String templatePath) {
        this.templatePath=templatePath;
    }

    public ResourceType getResourceType() {
        return resourceType;
    }

    public void setResourceType(ResourceType resourceType) {
        this.resourceType = resourceType;
    }

    public boolean isHvm() {
        return isHvm;
    }

    public void setHvm(boolean isHvm) {
        this.isHvm = isHvm;
    }

    public Storage.ImageFormat getFormat() {
        return format;
    }

    public void setFormat(Storage.ImageFormat format) {
        this.format = format;
    }

    public String getUuid() {
        return uuid;
    }

    public long getEntityId() {
        return entityId;
    }

    public String getChksum() {
        return chksum;
    }

    public void setChksum(String chksum) {
        this.chksum = chksum;
    }

    public long getVirtualSize() {
        return virtualSize;
    }

    public void setVirtualSize(long virtualSize) {
        this.virtualSize = virtualSize;
    }

    public boolean isMetaDataPopulated() {
        return isMetaDataPopulated;
    }

    public void setMetaDataPopulated(boolean isMetaDataPopulated) {
        this.isMetaDataPopulated = isMetaDataPopulated;
    }

    public void setPhysicalSize(long physicalSize) {
        this.physicalSize = physicalSize;
    }

    public long getPhysicalSize() {
        return physicalSize;
    }

    public int getMaxSizeInGB() {
        return maxSizeInGB;
    }

    public void setMaxSizeInGB(int maxSizeInGB) {
        this.maxSizeInGB = maxSizeInGB;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public long getContentLength() {
        return contentLength;
    }

    public void setContentLength(long contentLength) {
        this.contentLength = contentLength;
    }
}
