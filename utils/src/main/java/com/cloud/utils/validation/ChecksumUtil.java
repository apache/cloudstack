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
package com.cloud.utils.validation;

import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.script.Script;
import org.apache.cloudstack.utils.security.DigestHelper;

import java.io.File;

public class ChecksumUtil {
    public static String calculateCurrentChecksum(String name, String path) {
        String cloudScriptsPath = Script.findScript("", path);
        if (cloudScriptsPath == null) {
            throw new CloudRuntimeException(String.format("Unable to find cloudScripts path, cannot update SystemVM %s", name));
        }
        String md5sum = DigestHelper.calculateChecksum(new File(cloudScriptsPath));
        return md5sum;
    }
}
