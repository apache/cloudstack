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

package com.cloud.agent.api;

public class GetDomRVersionAnswer extends Answer {
    public static final String ROUTER_NAME = "router.name";
    public static final String ROUTER_IP = "router.ip";
    String templateVersion;
    String scriptsVersion;

    protected GetDomRVersionAnswer() {
    }

    public GetDomRVersionAnswer(GetDomRVersionCmd cmd, String details, String templateVersion, String scriptsVersion) {
        super(cmd, true, details);
        this.templateVersion = templateVersion;
        this.scriptsVersion = scriptsVersion;
    }

    public GetDomRVersionAnswer(GetDomRVersionCmd cmd, String details) {
        super(cmd, false, details);
    }

    public String getTemplateVersion() {
        return this.templateVersion;
    }

    public String getScriptsVersion() {
        return this.scriptsVersion;
    }
}
