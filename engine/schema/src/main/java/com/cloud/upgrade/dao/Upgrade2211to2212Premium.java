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
package com.cloud.upgrade.dao;

import java.io.InputStream;


import com.cloud.utils.exception.CloudRuntimeException;

public class Upgrade2211to2212Premium extends Upgrade2211to2212 {

    @Override
    public InputStream[] getPrepareScripts() {
        InputStream[] newScripts = new InputStream[2];
        newScripts[0] = super.getPrepareScripts()[0];
        final String scriptFile = "META-INF/db/schema-2211to2212-premium.sql";
        final InputStream script = Thread.currentThread().getContextClassLoader().getResourceAsStream(scriptFile);
        if (script == null) {
            throw new CloudRuntimeException("Unable to find " + scriptFile);
        }
        newScripts[1] = script;

        return newScripts;
    }
}
