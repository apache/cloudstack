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

package org.apache.cloudstack.backup.veeam.api;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

import java.util.List;

@JacksonXmlRootElement(localName = "BackupFile")
public class BackupFile {
    @JacksonXmlProperty(localName = "Type", isAttribute = true)
    private String type;

    @JacksonXmlProperty(localName = "Href", isAttribute = true)
    private String href;

    @JacksonXmlProperty(localName = "Name", isAttribute = true)
    private String name;

    @JacksonXmlProperty(localName = "UID", isAttribute = true)
    private String uid;

    @JacksonXmlProperty(localName = "Link")
    @JacksonXmlElementWrapper(localName = "Links")
    private List<Link> link;

    @JacksonXmlProperty(localName = "FilePath")
    private String filePath;

    @JacksonXmlProperty(localName = "BackupSize")
    private String backupSize;

    @JacksonXmlProperty(localName = "DataSize")
    private String dataSize;

    @JacksonXmlProperty(localName = "DeduplicationRatio")
    private String deduplicationRatio;

    @JacksonXmlProperty(localName = "CompressRatio")
    private String compressRatio;

    @JacksonXmlProperty(localName = "CreationTimeUtc")
    private String creationTimeUtc;

    @JacksonXmlProperty(localName = "FileType")
    private String fileType;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getHref() {
        return href;
    }

    public void setHref(String href) {
        this.href = href;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public List<Link> getLink() {
        return link;
    }

    public void setLink(List<Link> link) {
        this.link = link;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getBackupSize() {
        return backupSize;
    }

    public void setBackupSize(String backupSize) {
        this.backupSize = backupSize;
    }

    public String getDataSize() {
        return dataSize;
    }

    public void setDataSize(String dataSize) {
        this.dataSize = dataSize;
    }

    public String getDeduplicationRatio() {
        return deduplicationRatio;
    }

    public void setDeduplicationRatio(String deduplicationRatio) {
        this.deduplicationRatio = deduplicationRatio;
    }

    public String getCompressRatio() {
        return compressRatio;
    }

    public void setCompressRatio(String compressRatio) {
        this.compressRatio = compressRatio;
    }

    public String getCreationTimeUtc() {
        return creationTimeUtc;
    }

    public void setCreationTimeUtc(String creationTimeUtc) {
        this.creationTimeUtc = creationTimeUtc;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }
}
