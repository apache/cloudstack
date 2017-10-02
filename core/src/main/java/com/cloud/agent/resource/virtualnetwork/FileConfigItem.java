//
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
//

package com.cloud.agent.resource.virtualnetwork;

public class FileConfigItem extends ConfigItem {
    private String filePath;
    private String fileName;
    private String fileContents;

    public FileConfigItem(String filePath, String fileName, String fileContents) {
        this.filePath = filePath;
        this.fileName = fileName;
        this.fileContents = fileContents;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFileContents() {
        return fileContents;
    }

    public void setFileContents(String fileContents) {
        this.fileContents = fileContents;
    }

    @Override
    public String getAggregateCommand() {
        StringBuilder sb = new StringBuilder();
        sb.append("<file>\n");
        sb.append(filePath);

        // Don't use File.pathSeparator here as the target is the unix based systemvm
        if (!filePath.endsWith("/")) {
            sb.append('/');
        }

        sb.append(fileName);
        sb.append('\n');
        sb.append(fileContents);
        sb.append("\n</file>\n");
        return sb.toString();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("FileConfigItem, copying ");
        sb.append(fileContents.length());
        sb.append(" characters to ");
        sb.append(fileName);
        return sb.toString();
    }

}
