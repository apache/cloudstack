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


import java.io.File;
import java.io.FileOutputStream;

public class UploadEntity {
    private long filesize;
    private long downloadedsize;
    private String filename;
    private String absoluteFilePath;




    public static enum Status {
        UNKNOWN, IN_PROGRESS, COMPLETED, ERROR
    }

    private Status uploadState;
    private FileOutputStream filewriter = null;
    private String errorMessage=null;
    private File file;

    public UploadEntity(long filesize, Status status, String filename, String absoluteFilePath) {
        this.filesize=filesize;
        this.uploadState=status;
        this.downloadedsize=0l;
        this.filename=filename;
        this.absoluteFilePath=absoluteFilePath;
    }

    public void setEntitysize(long filesize) {
         this.filesize=filesize;
    }

    public void setStatus(Status status) {
        this.uploadState = status;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage=errorMessage;
    }

    public FileOutputStream getFilewriter() {
        return filewriter;
    }

    public long getEntitysize() {
        return filesize;
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

    public void setFilewriter(FileOutputStream filewriter) {
        this.filewriter = filewriter;
    }

    public void incremetByteCount(long numberOfBytes) {
           this.downloadedsize+= numberOfBytes;
    }

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }
    public String getAbsoluteFilePath() {
        return absoluteFilePath;
    }

    public void setAbsoluteFilePath(String absoluteFilePath) {
        this.absoluteFilePath = absoluteFilePath;
    }


}
