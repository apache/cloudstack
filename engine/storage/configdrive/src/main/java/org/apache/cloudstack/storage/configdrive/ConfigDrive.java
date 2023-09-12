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

package org.apache.cloudstack.storage.configdrive;

public class ConfigDrive {

    public final static String CONFIGDRIVEDIR = "configdrive";

    public static final String cloudStackConfigDriveName = "/cloudstack/";
    public static final String openStackConfigDriveName = "/openstack/latest/";

    /**
     * Creates the path to ISO file relative to mount point.
     * The config driver path will have the following format: {@link #CONFIGDRIVEDIR} + / + instanceName + ".iso"
     *
     * @return config drive ISO file path
     */
    public static String createConfigDrivePath(String instanceName) {
        return ConfigDrive.CONFIGDRIVEDIR + "/" + configIsoFileName(instanceName);
    }

    /**
     * Config Drive iso file name for an instance name
     * @param instanceName
     * @return
     */
    public static String configIsoFileName(String instanceName) {
        return instanceName + ".iso";
    }
}
