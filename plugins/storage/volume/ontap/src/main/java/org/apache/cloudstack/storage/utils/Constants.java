/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.cloudstack.storage.utils;


public class Constants {

    public static final String ONTAP_PLUGIN_NAME = "ONTAP";
    public static final int NFS3_PORT = 2049;
    public static final int ISCSI_PORT = 3260;

    public static final String NFS = "nfs";
    public static final String ISCSI = "iscsi";
    public static final String SIZE = "size";
    public static final String PROTOCOL = "protocol";
    public static final String SVM_NAME = "svmName";
    public static final String USERNAME = "username";
    public static final String PASSWORD = "password";
    public static final String DATA_LIF = "dataLIF";
    public static final String MANAGEMENT_LIF = "managementLIF";
    public static final String VOLUME_NAME = "volumeName";
    public static final String VOLUME_UUID = "volumeUUID";
    public static final String EXPORT_POLICY_ID = "exportPolicyId";
    public static final String EXPORT_POLICY_NAME = "exportPolicyName";
    public static final String IS_DISAGGREGATED = "isDisaggregated";
    public static final String RUNNING = "running";
    public static final String EXPORT = "export";

    public static final int ONTAP_PORT = 443;

    public static final String JOB_RUNNING = "running";
    public static final String JOB_QUEUE = "queued";
    public static final String JOB_PAUSED = "paused";
    public static final String JOB_FAILURE = "failure";
    public static final String JOB_SUCCESS = "success";

    public static final String TRUE = "true";
    public static final String FALSE = "false";

    // Query params
    public static final String NAME = "name";
    public static final String FIELDS = "fields";
    public  static final String INITIATORS = "initiators";
    public static final String AGGREGATES = "aggregates";
    public static final String STATE = "state";
    public static final String DATA_NFS = "data_nfs";
    public static final String DATA_ISCSI = "data_iscsi";
    public static final String IP_ADDRESS = "ip.address";
    public static final String SERVICES = "services";
    public static final String RETURN_RECORDS = "return_records";

    public static final int JOB_MAX_RETRIES = 100;
    public static final int CREATE_VOLUME_CHECK_SLEEP_TIME = 2000;

    public static final String SLASH = "/";
    public static final String EQUALS = "=";
    public static final String SEMICOLON = ";";
    public static final String COMMA = ",";
    public static final String HYPHEN = "-";

    public static final String VOLUME_PATH_PREFIX = "/vol/";

    public static final String ONTAP_NAME_REGEX = "^[a-zA-Z][a-zA-Z0-9_]*$";
    public static final String KVM = "KVM";

    public static final String HTTPS = "https://";
    public static final String SVM_DOT_NAME = "svm.name";
    public static final String LUN_DOT_NAME = "lun.name";
    public static final String IQN = "iqn";
    public static final String LUN_DOT_UUID = "lun.uuid";
    public static final String LOGICAL_UNIT_NUMBER = "logical_unit_number";
    public static final String IGROUP_DOT_NAME = "igroup.name";
    public static final String IGROUP_DOT_UUID = "igroup.uuid";
    public static final String UNDERSCORE = "_";
    public static final String CS = "cs";
}
