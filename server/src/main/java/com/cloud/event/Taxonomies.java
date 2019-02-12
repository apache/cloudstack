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

package com.cloud.event;

public class Taxonomies {

    public static class EventType {
        public static final String MONITOR = "monitor";
        public static final String ACTIVITY = "activity";
        public static final String CONTROL = "control";
    }

    public static class Action {

        //General Resource Management
        public static final String CREATE = "create";
        public static final String READ = "read";
        public static final String UPDATE = "update";
        public static final String DELETE = "delete";

        //Monitoring
        public static final String MONITOR = "monitor";

        //Workload and Data Management
        public static final String BACKUP = "backup";
        public static final String CAPTURE = "capture";
        public static final String CONFIGURE = "configure";
        public static final String DEPLOY = "deploy";
        public static final String DISABLE = "disable";
        public static final String ENABLE = "enable";
        public static final String RESTORE = "restore";
        public static final String START = "start";
        public static final String STOP = "stop";
        public static final String UNDEPLOY = "undeploy";

        //Messaging
        public static final String RECEIVE = "receive";
        public static final String SEND = "send";

        //Security-Identity
        public static final String AUTHENTICATE = "authenticate";
        public static final String AUTHENTICATE_LOGIN = "authenticate/login";
        public static final String RENEW = "RENEW";
        public static final String REVOKE = "REVOKE";

        //Security, Policy, Access Control
        public static final String ALLOW = "allow";
        public static final String DENY = "deny";
        public static final String EVALUATE = "evaluate";
        public static final String NOTIFY = "notify";

        public static final String UNKNOWN = "unknown";



    }

    public static class Resource {

        public static final String STORAGE = "storage";
        public static final String STORAGE_NODE = "storage/node";
        public static final String STORAGE_VOLUME = "storage/volume";
        public static final String STORAGE_MEMORY = "storage/memory";
        public static final String STORAGE_MEMORY_CACHE = "storage/memory/cache";
        public static final String STORAGE_CONTAINER = "storage/container";
        public static final String STORAGE_DIRECTORY = "storage/directory";
        public static final String STORAGE_DATABASE = "storage/database";
        public static final String STORAGE_QUEUE = "storage/queue";

        public static final String COMPUTE = "compute";
        public static final String COMPUTE_NODE = "compute/node";
        public static final String COMPUTE_CPU = "compute/cpu";
        public static final String COMPUTE_CPU_VPU = "compute/cpu/vpu";
        public static final String COMPUTE_MACHINE = "compute/machine";
        public static final String COMPUTE_MACHINE_VM = "compute/machine/vm";
        public static final String COMPUTE_PROCESS = "compute/process";
        public static final String COMPUTE_THREAD = "compute/thread";

        public static final String NETWORK = "network";
        public static final String NETWORK_NODE = "network/node";
        public static final String NETWORK_NODE_HOST = "network/node/host";
        public static final String NETWORK_NODE_ROUTER = "network/node/router";
        public static final String NETWORK_NODE_SWITCH = "network/node/switch";
        public static final String NETWORK_NODE_FIREWALL = "network/node/firewall";
        public static final String NETWORK_CONNECTION = "network/connection";
        public static final String NETWORK_CONNECTION_FTP = "network/connection/ftp";
        public static final String NETWORK_CONNECTION_PIPE = "network/connection/pipe";
        public static final String NETWORK_DOMAIN = "network/domain";
        public static final String NETWORK_CLUSTER = "network/cluster";

        public static final String DATA = "data";
        public static final String DATA_CATALOG = "data/catalog";
        public static final String DATA_CONFIG = "data/config";
        public static final String DATA_DIRECTORY = "data/directory";
        public static final String DATA_FILE = "data/file";
        public static final String DATA_IMAGE = "data/image";
        public static final String DATA_LOG = "data/log";
        public static final String DATA_MESSAGE = "data/message";
        public static final String DATA_MESSAGE_STREAM = "data/message/stream";
        public static final String DATA_MODULE = "data/module";
        public static final String DATA_PACKAGE = "data/package";
        public static final String DATA_REPORT = "data/report";
        public static final String DATA_TEMPLATE = "data/template";
        public static final String DATA_WORKLOAD = "data/workload";
        public static final String DATA_WORKLOAD_APPLICATION = "data/workload/application";
        public static final String DATA_WORKLOAD_SERVICE = "data/workload/service";
        public static final String DATA_DATABASE = "data/database";
        public static final String DATA_DATABASE_ALIAS = "data/database/alias";
        public static final String DATA_DATABASE_INDEX = "data/database/index";
        public static final String DATA_DATABASE_INSTANCE = "data/database/instance";
        public static final String DATA_DATABASE_KEY = "data/database/key";
        public static final String DATA_DATABASE_ROUTINE = "data/database/routine";
        public static final String DATA_DATABASE_SCHEMA = "data/database/schema";
        public static final String DATA_DATABASE_SEQUENCE = "data/database/sequence";
        public static final String DATA_DATABASE_TABLE = "data/database/table";
        public static final String DATA_DATABASE_VIEW = "data/database/view";
        public static final String DATA_SECURITY = "data/security";
        public static final String DATA_SECURITY_ACCOUNT = "data/security/account";
        public static final String DATA_SECURITY_ACCOUNT_USER = "data/security/account/user";
        public static final String DATA_SECURITY_ACCOUNT_ADMIN = "data/security/account/admin";
        public static final String DATA_SECURITY_CREDENTIAL = "data/security/credential";
        public static final String DATA_SECURITY_GROUP = "data/security/group";
        public static final String DATA_SECURITY_IDENTITY = "data/security/identity";
        public static final String DATA_SECURITY_IDENTITY_ATTRIBUTE = "data/security/identity/attribute";
        public static final String DATA_SECURITY_IDENTITY_TOKEN = "data/security/identity/token";
        public static final String DATA_SECURITY_KEY = "data/security/key";
        public static final String DATA_SECURITY_LICENSE = "data/security/license";
        public static final String DATA_SECURITY_POLICY = "data/security/policy";
        public static final String DATA_SECURITY_PROFILE = "data/security/profile";
        public static final String DATA_SECURITY_ROLE = "data/security/role";
        public static final String DATA_SECURITY_NODE = "data/security/node";

        public static final String SERVICE = "service";
        public static final String SERVICE_BSS = "service/bss";
        public static final String SERVICE_BSS_BILLING = "service/bss/billing";
        public static final String SERVICE_BSS_LOCATION = "service/bss/location";
        public static final String SERVICE_BSS_METERING = "service/bss/metering";
        public static final String SERVICE_COMPOSITION = "service/composition";
        public static final String SERVICE_COMPOSITION_ORCHESTRATION = "service/composition/orchestration";
        public static final String SERVICE_COMPOSITION_WORKFLOW = "service/composition/workflow";
        public static final String SERVICE_COMPUTE = "service/compute";
        public static final String SERVICE_DATABASE = "service/database";
        public static final String SERVICE_IMAGE = "service/image";
        public static final String SERVICE_NETWORK = "service/network";
        public static final String SERVICE_OSS = "service/oss";
        public static final String SERVICE_OSS_CAPACITY = "service/oss/capacity";
        public static final String SERVICE_OSS_CONFIGURATION = "service/oss/configuration";
        public static final String SERVICE_OSS_LOGGING = "service/oss/logging";
        public static final String SERVICE_OSS_MONITORING = "service/oss/monitoring";
        public static final String SERVICE_OSS_VIRTUALIZATION = "service/oss/virtualization";
        public static final String SERVICE_SECURITY = "service/security";
        public static final String SERVICE_STORAGE = "service/storage";
        public static final String SERVICE_STORAGE_BLOCK = "service/storage/block";
        public static final String SERVICE_STORAGE_OBJECT = "service/storage/object";

        public static final String SYSTEM = "system";

    }

    public static class Outcome {
        public static final String SUCCESS = "success";
        public static final String FAILURE = "failure";
        public static final String UNKNOWN = "unknown";
        public static final String PENDING = "pending";
    }

    public static class Reason {
        public static final String REASON_TYPE = "reasonType";
        public static final String REASON_CODE = "reasonCode";
        public static final String POLICY_TYPE = "policyType";
        public static final String POLICY_CODE = "policyCode";

    }
}
