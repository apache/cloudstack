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

import com.cloud.storage.Storage.ImageFormat;

public abstract class AbstractDownloadCommand extends SsCommand {

    private String url;
    private ImageFormat format;
    private long accountId;
    private String name;

    protected AbstractDownloadCommand() {
    }

    protected AbstractDownloadCommand(final String name, String url, final ImageFormat format, final Long accountId) {
        assert url != null;
        url = url.replace('\\', '/');

        this.url = url;
        this.format = format;
        this.accountId = accountId;
        this.name = name;
    }

    protected AbstractDownloadCommand(final AbstractDownloadCommand that) {
        super(that);
        assert that.url != null;

        url = that.url.replace('\\', '/');
        format = that.format;
        accountId = that.accountId;
        name = that.name;
    }

    public String getUrl() {
        return url;
    }

    public String getName() {
        return name;
    }

    public ImageFormat getFormat() {
        return format;
    }

    public long getAccountId() {
        return accountId;
    }

    @Override
    public boolean executeInSequence() {
        return true;
    }

    public void setUrl(String url) {
        assert url != null;
        url = url.replace('\\', '/');
        this.url = url;
    }

}
