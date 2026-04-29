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

import com.cloud.agent.api.Answer;

public class PrimaryStorageDownloadAnswer extends Answer {
    private String installPath;
    private long templateSize = 0L;

    protected PrimaryStorageDownloadAnswer() {
        super();
    }

    public PrimaryStorageDownloadAnswer(String detail) {
        super(null, false, detail);
    }

    public PrimaryStorageDownloadAnswer(String installPath, long templateSize) {
        super(null);
        this.installPath = installPath;
        this.templateSize = templateSize;
    }

    public String getInstallPath() {
        return installPath;
    }

    public void setInstallPath(String installPath) {
        this.installPath = installPath;
    }

    public void setTemplateSize(long templateSize) {
        this.templateSize = templateSize;
    }

    public Long getTemplateSize() {
        return templateSize;
    }

}
