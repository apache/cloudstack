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
    public static final String NFS = "nfs";
    public static final String ISCSI = "iscsi";
    public static final String PROTOCOL = "protocol";
    public static final String SVMNAME = "svmName";
    public static final String USERNAME = "username";
    public static final String PASSWORD = "password";
    public static final String MANAGEMENTLIF = "managementLIF";
    public static final String ISDISAGGREGATED = "isDisaggregated";
    public static final String RUNNING = "running";

    public static final String JOBRUNNING = "running";
    public static final String JOBQUEUE = "queued";
    public static final String JOBPAUSED = "paused";
    public static final String JOBFAILURE = "failure";
    public static final String JOBSUCCESS = "success";

    public static final int JOBMAXRETRIES = 100;
    public static final int CREATEVOLUMECHECKSLEEPTIME = 2000;

    public static final String HTTPS = "https://";
    public static final String GETSVMs = "/api/svm/svms";
    public static final String CREATEVOLUME = "/api/storage/volumes";
    public static final String GETJOBBYUUID = "/api/cluster/jobs";
}
