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
package com.cloud.agent.api.to;

public class DatadiskTO {
    private String path;
    private long virtualSize;
    private long fileSize;
    boolean bootable;
    private String diskId;
    private boolean isIso;
    private String diskController;
    private String diskControllerSubType;
    private int diskNumber;
    private String configuration;

    public DatadiskTO() {
    }

    public DatadiskTO(String path, long virtualSize, long fileSize, String diskId, boolean isIso, boolean bootable,
                      String controller, String controllerSubType, int diskNumber, String configuration) {
        this.path = path;
        this.virtualSize = virtualSize;
        this.fileSize = fileSize;
        this.bootable = bootable;
        this.diskId = diskId;
        this.isIso = isIso;
        this.diskController = controller;
        this.diskControllerSubType = controllerSubType;
        this.diskNumber = diskNumber;
        this.configuration = configuration;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Long getVirtualSize() {
        return virtualSize;
    }

    public void setVirtualSize(Long virtualSize) {
        this.virtualSize = virtualSize;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public boolean isBootable() {
        return bootable;
    }

    public String getDiskId() {
        return diskId;
    }

    public void setDiskId(String diskId) {
        this.diskId = diskId;
    }

    public boolean isIso() {
        return isIso;
    }

    public void setIso(boolean isIso) {
        this.isIso = isIso;
    }

    public String getDiskController() {
        return diskController;
    }

    public void setDiskController(String diskController) {
        this.diskController = diskController;
    }

    public String getDiskControllerSubType() {
        return diskControllerSubType;
    }

    public void setDiskControllerSubType(String diskControllerSubType) {
        this.diskControllerSubType = diskControllerSubType;
    }

    public int getDiskNumber() {
        return this.diskNumber;
    }

    public String getConfiguration() {
        return configuration;
    }
}
