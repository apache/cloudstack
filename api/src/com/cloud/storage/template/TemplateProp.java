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
package com.cloud.storage.template;

public class TemplateProp {
    String templateName;
    String installPath;
    long size;
    long physicalSize;
    long id;
    boolean isPublic;
    boolean isCorrupted;

    protected TemplateProp() {

    }

    public TemplateProp(String templateName, String installPath, long size, long physicalSize, boolean isPublic, boolean isCorrupted) {
        this.templateName = templateName;
        this.installPath = installPath;
        this.size = size;
        this.physicalSize = physicalSize;
        this.isPublic = isPublic;
        this.isCorrupted = isCorrupted;
    }

    public TemplateProp(String templateName, String installPath, boolean isPublic, boolean isCorrupted) {
        this(templateName, installPath, 0, 0, isPublic, isCorrupted);
    }

    public long getId() {
        return id;
    }

    public String getTemplateName() {
        return templateName;
    }

    public String getInstallPath() {
        return installPath;
    }

    public boolean isPublic() {
        return isPublic;
    }

    public boolean isCorrupted() {
        return isCorrupted;
    }

    public void setInstallPath(String installPath) {
        this.installPath = installPath;
    }

    public long getSize() {
        return size;
    }

    public long getPhysicalSize() {
        return physicalSize;
    }

    public void setSize(long size) {
        this.size = size;
    }

}
