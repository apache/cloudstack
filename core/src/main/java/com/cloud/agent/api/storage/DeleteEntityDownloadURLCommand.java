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

package com.cloud.agent.api.storage;

import com.cloud.storage.Upload;

public class DeleteEntityDownloadURLCommand extends AbstractDownloadCommand {

    private String path;
    private String extractUrl;
    private Upload.Type type;
    private String parentPath;

    public DeleteEntityDownloadURLCommand(String path, Upload.Type type, String url, String parentPath) {
        super();
        this.path = path;
        this.type = type;
        this.extractUrl = url;
        this.parentPath = parentPath;
    }

    public DeleteEntityDownloadURLCommand() {
        super();
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Upload.Type getType() {
        return type;
    }

    public void setType(Upload.Type type) {
        this.type = type;
    }

    public String getExtractUrl() {
        return extractUrl;
    }

    public void setExtractUrl(String extractUrl) {
        this.extractUrl = extractUrl;
    }

    public String getParentPath() {
        return parentPath;
    }

    public void setParentPath(String parentPath) {
        this.parentPath = parentPath;
    }

}
