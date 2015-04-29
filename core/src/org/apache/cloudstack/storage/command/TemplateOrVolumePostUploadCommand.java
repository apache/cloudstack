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

package org.apache.cloudstack.storage.command;

public class TemplateOrVolumePostUploadCommand {

    long entityId;

    String entityUUID;

    String absolutePath;

    String checksum;

    String type;

    String name;

    String localPath;

    boolean requiresHvm;

    String imageFormat;

    String dataTo;

    String dataToRole;

    String remoteEndPoint;

    String maxUploadSize;

    String description;

    private String defaultMaxAccountSecondaryStorage;

    private long accountId;

    public TemplateOrVolumePostUploadCommand(long entityId, String entityUUID, String absolutePath, String checksum, String type, String name, String imageFormat, String dataTo,
            String dataToRole) {
        this.entityId = entityId;
        this.entityUUID = entityUUID;
        this.absolutePath = absolutePath;
        this.checksum = checksum;
        this.type = type;
        this.name = name;
        this.imageFormat = imageFormat;
        this.dataTo = dataTo;
        this.dataToRole = dataToRole;
    }

    public TemplateOrVolumePostUploadCommand() {
    }

    public String getRemoteEndPoint() {
        return remoteEndPoint;
    }

    public void setRemoteEndPoint(String remoteEndPoint) {
        this.remoteEndPoint = remoteEndPoint;
    }

    public String getDataTo() {
        return dataTo;
    }

    public void setDataTo(String dataTo) {
        this.dataTo = dataTo;
    }

    public String getDataToRole() {
        return dataToRole;
    }

    public void setDataToRole(String dataToRole) {
        this.dataToRole = dataToRole;
    }

    public String getLocalPath() {
        return localPath;
    }

    public void setLocalPath(String localPath) {
        this.localPath = localPath;
    }

    public boolean getRequiresHvm() {
        return requiresHvm;
    }

    public void setRequiresHvm(boolean requiresHvm) {
        this.requiresHvm = requiresHvm;
    }

    public String getImageFormat() {
        return imageFormat;
    }

    public void setImageFormat(String imageFormat) {
        this.imageFormat = imageFormat;
    }

    public long getEntityId() {
        return entityId;
    }

    public void setEntityId(long entityId) {
        this.entityId = entityId;
    }

    public String getEntityUUID() {
        return entityUUID;
    }

    public void setEntityUUID(String entityUUID) {
        this.entityUUID = entityUUID;
    }

    public String getAbsolutePath() {
        return absolutePath;
    }

    public void setAbsolutePath(String absolutePath) {
        this.absolutePath = absolutePath;
    }

    public String getChecksum() {
        return checksum;
    }

    public void setChecksum(String checksum) {
        this.checksum = checksum;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMaxUploadSize() {
        return maxUploadSize;
    }

    public void setMaxUploadSize(String maxUploadSize) {
        this.maxUploadSize = maxUploadSize;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setDefaultMaxAccountSecondaryStorage(String defaultMaxAccountSecondaryStorage) {
        this.defaultMaxAccountSecondaryStorage = defaultMaxAccountSecondaryStorage;
    }

    public String getDefaultMaxAccountSecondaryStorage() {
        return defaultMaxAccountSecondaryStorage;
    }

    public void setAccountId(long accountId) {
        this.accountId = accountId;
    }

    public long getAccountId() {
        return accountId;
    }
}
