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

package org.apache.cloudstack.agent.directdownload;

import com.cloud.agent.api.Command;

public class CheckUrlCommand extends Command {

    private String format;
    private String url;
    private Integer connectTimeout;
    private Integer connectionRequestTimeout;
    private Integer socketTimeout;
    private boolean followRedirects;

    public String getFormat() {
        return format;
    }

    public String getUrl() {
        return url;
    }

    public Integer getConnectTimeout() { return connectTimeout; }

    public Integer getConnectionRequestTimeout() { return connectionRequestTimeout; }

    public Integer getSocketTimeout() { return socketTimeout; }

    public boolean isFollowRedirects() {
        return followRedirects;
    }

    public CheckUrlCommand(final String format, final String url, final boolean followRedirects) {
        super();
        this.format = format;
        this.url = url;
        this.followRedirects = followRedirects;
    }

    public CheckUrlCommand(final String format,final String url, Integer connectTimeout, Integer connectionRequestTimeout, Integer socketTimeout, final boolean followRedirects) {
        super();
        this.format = format;
        this.url = url;
        this.connectTimeout = connectTimeout;
        this.socketTimeout = socketTimeout;
        this.connectionRequestTimeout = connectionRequestTimeout;
        this.followRedirects = followRedirects;
    }

    @Override
    public boolean executeInSequence() {
        return false;
    }

}
