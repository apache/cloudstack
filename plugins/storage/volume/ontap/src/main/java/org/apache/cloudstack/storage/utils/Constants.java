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
    public enum ProtocolType {
        NFS,
        ISCSI
    }

    public static final String NFS = "nfs";
    public static final String ISCSI = "iscsi";
    public static final String PROTOCOL = "protocol";
    public static final String SVM_NAME = "svmName";
    public static final String USERNAME = "username";
    public static final String PASSWORD = "password";
    public static final String MANAGEMENT_LIF = "managementLIF";
    public static final String IS_DISAGGREGATED = "isDisaggregated";
    public static final String RUNNING = "running";

    public static final String JOB_RUNNING = "running";
    public static final String JOB_QUEUE = "queued";
    public static final String JOB_PAUSED = "paused";
    public static final String JOB_FAILURE = "failure";
    public static final String JOB_SUCCESS = "success";

    public static final int JOB_MAX_RETRIES = 100;
    public static final int CREATE_VOLUME_CHECK_SLEEP_TIME = 2000;

    public static final String HTTPS = "https://";
    public static final String GET_SVMs = "/api/svm/svms";
    public static final String CREATE_VOLUME = "/api/storage/volumes";
    public static final String GET_JOB_BY_UUID = "/api/cluster/jobs";
}
