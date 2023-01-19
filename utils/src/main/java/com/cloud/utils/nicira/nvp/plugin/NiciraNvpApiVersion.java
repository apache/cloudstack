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

package com.cloud.utils.nicira.nvp.plugin;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import org.apache.cloudstack.utils.CloudStackVersion;

public class NiciraNvpApiVersion {
    protected static Logger LOGGER = LogManager.getLogger(NiciraNvpApiVersion.class);

    private static String niciraApiVersion;

    public static synchronized void setNiciraApiVersion(String apiVersion){
        niciraApiVersion = apiVersion;
    }

    public static synchronized boolean isApiVersionLowerThan(String apiVersion){
        if (niciraApiVersion == null) {
            return false;
        }
        int compare = CloudStackVersion.compare(niciraApiVersion, apiVersion);
        return (compare < 0);
    }

    public static synchronized void logNiciraApiVersion() {
        if (niciraApiVersion != null) {
            LOGGER.info(String.format("NSX API VERSION: %s", niciraApiVersion));
        }
    }

}
