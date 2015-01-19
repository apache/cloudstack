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

import org.apache.cloudstack.engine.subsystem.api.storage.DataObject;
import org.apache.cloudstack.engine.subsystem.api.storage.EndPoint;

public class TemplateOrVolumePostUploadCommand {
    DataObject dataObject;
    EndPoint endPoint;

    long entityId;
    String entityUUID;
    String absolutePath;
    String checksum;
    String type;
    String name;
//    String installPathPrefix;
    String localPath;
//    String isHvm;
    String imageFormat;
    String dataTo;
    String dataToRole;

    public TemplateOrVolumePostUploadCommand(long entityId, String entityUUID, String absolutePath, String checksum, String type, String name,
                                             String localPath, String imageFormat, String dataTo, String dataToRole) {
        this.entityId = entityId;
        this.entityUUID = entityUUID;
        this.absolutePath = absolutePath;
        this.checksum = checksum;
        this.type = type;
        this.name = name;
//        this.installPathPrefix = installPathPrefix;
        this.localPath = localPath;
//        this.isHvm = isHvm;
        this.imageFormat = imageFormat;
        this.dataTo = dataTo;
        this.dataToRole = dataToRole;
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
    //    public String getInstallPathPrefix() {
//        return installPathPrefix;
//    }
//
//    public void setInstallPathPrefix(String installPathPrefix) {
//        this.installPathPrefix = installPathPrefix;
//    }

    public String getLocalPath() {
        return localPath;
    }

    public void setLocalPath(String localPath) {
        this.localPath = localPath;
    }

//    public String getIsHvm() {
//        return isHvm;
//    }

//    public void setIsHvm(String isHvm) {
//        this.isHvm = isHvm;
//    }

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

    public TemplateOrVolumePostUploadCommand(DataObject dataObject, EndPoint endPoint) {
        this.dataObject = dataObject;
        this.endPoint = endPoint;
    }

    public TemplateOrVolumePostUploadCommand() {
    }

    public DataObject getDataObject() {
        return dataObject;
    }

    public void setDataObject(DataObject dataObject) {
        this.dataObject = dataObject;
    }

    public EndPoint getEndPoint() {
        return endPoint;
    }

    public void setEndPoint(EndPoint endPoint) {
        this.endPoint = endPoint;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        TemplateOrVolumePostUploadCommand that = (TemplateOrVolumePostUploadCommand)o;

        return dataObject.equals(that.dataObject) && endPoint.equals(that.endPoint);

    }

    @Override
    public int hashCode() {
        int result = dataObject.hashCode();
        result = 31 * result + endPoint.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "TemplateOrVolumePostUploadCommand{" + "dataObject=" + dataObject + ", endPoint=" + endPoint + '}';
    }
}
