// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// the License.  You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package org.apache.cloudstack.utils.qemu;

import org.apache.cloudstack.utils.qemu.QemuImg.PhysicalDiskFormat;

public class QemuImgFile {

    private long size = 0;
    private String fileName;
    private PhysicalDiskFormat format = PhysicalDiskFormat.RAW;

    public QemuImgFile(String fileName) {
        this.fileName = fileName;
    }

    public QemuImgFile(String fileName, long size) {
        this.fileName = fileName;
        this.size = size;
    }

    public QemuImgFile(String fileName, long size, PhysicalDiskFormat format) {
        this.fileName = fileName;
        this.size = size;
        this.format = format;
    }

    public QemuImgFile(String fileName, PhysicalDiskFormat format) {
        this.fileName = fileName;
        this.format = format;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public void setFormat(PhysicalDiskFormat format) {
        this.format = format;
    }

    public String getFileName() {
        return this.fileName;
    }

    public long getSize() {
        return this.size;
    }

    public PhysicalDiskFormat getFormat() {
        return this.format;
    }

}
