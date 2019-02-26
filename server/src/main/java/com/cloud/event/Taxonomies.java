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

import java.util.HashMap;

public class Taxonomies {
    //eventMapping maps CloudStack Event Category (substring of EventType) to CADF Target Resource
    public static HashMap<String, String> eventMapping = new HashMap<String, String>();

    public enum EventType {
        MONITOR("monitor"),
        ACTIVITY("activity"),
        CONTROL("control");

        private String value;

        EventType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    public enum Action {

        //General Resource Management
        CREATE("create"),
        READ("read"),
        UPDATE("update"),
        DELETE("delete"),

        //Monitoring
        MONITOR("monitor"),

        //Workload and Data Management
        BACKUP("backup"),
        CAPTURE("capture"),
        CONFIGURE("configure"),
        DEPLOY("deploy"),
        DISABLE("disable"),
        ENABLE("enable"),
        RESTORE("restore"),
        START("start"),
        STOP("stop"),
        UNDEPLOY("undeploy"),

        //Messaging
        RECEIVE("receive"),
        SEND("send"),

        //Security-Identity
        AUTHENTICATE("authenticate"),
        AUTHENTICATE_LOGIN("authenticate/login"),
        RENEW("renew"),
        REVOKE("revoke"),

        //Security, Policy, Access Control
        ALLOW("allow"),
        DENY("deny"),
        EVALUATE("evaluate"),
        NOTIFY("notify"),

        UNKNOWN("unknown");

        private String value;

        Action(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

    }

    public enum Outcome {
        SUCCESS("success"),
        FAILURE("failure"),
        UNKNOWN("unknown"),
        PENDING("pending");

        private String value;

        Outcome(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    public static class Reason {
        public static final String REASON_TYPE = "reasonType";
        public static final String REASON_CODE = "reasonCode";
        public static final String POLICY_TYPE = "policyType";
        public static final String POLICY_CODE = "policyCode";

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
        public static final String SERVICE_OSS_PERFORMANCE = "service/oss/performance";
        public static final String SERVICE_OSS_VIRTUALIZATION = "service/oss/virtualization";
        public static final String SERVICE_SECURITY = "service/security";
        public static final String SERVICE_STORAGE = "service/storage";
        public static final String SERVICE_STORAGE_BLOCK = "service/storage/block";
        public static final String SERVICE_STORAGE_OBJECT = "service/storage/object";

        public static final String SYSTEM = "system";

        public static final String UNKNOWN = "unknown";

    }

    static {
        eventMapping.put("VM", Taxonomies.Resource.COMPUTE_MACHINE_VM);
        eventMapping.put("ROUTER", Taxonomies.Resource.NETWORK_NODE_ROUTER); //needs check
        eventMapping.put("PROXY", Taxonomies.Resource.UNKNOWN);
        eventMapping.put("VNC", Taxonomies.Resource.UNKNOWN);
        eventMapping.put("NET", Taxonomies.Resource.NETWORK);
        eventMapping.put("PORTABLE", Taxonomies.Resource.NETWORK);
        eventMapping.put("NETWORK", Taxonomies.Resource.NETWORK);
        eventMapping.put("FIREWALL", Taxonomies.Resource.NETWORK);
        eventMapping.put("FIREWALL.EGRESS", Taxonomies.Resource.NETWORK);

        eventMapping.put("NIC", Taxonomies.Resource.NETWORK_NODE);
        eventMapping.put("NIC.DETAIL", Taxonomies.Resource.NETWORK_NODE);

        eventMapping.put("LB", Taxonomies.Resource.SERVICE_OSS_PERFORMANCE);
        eventMapping.put("LB.ASSIGN.TO", Taxonomies.Resource.SERVICE_OSS_PERFORMANCE);
        eventMapping.put("LB.REMOVE.FROM", Taxonomies.Resource.SERVICE_OSS_PERFORMANCE);
        eventMapping.put("LB.STICKINESSPOLICY", Taxonomies.Resource.SERVICE_OSS_PERFORMANCE);
        eventMapping.put("LB.HEALTHCHECKPOLICY", Taxonomies.Resource.SERVICE_OSS_PERFORMANCE);
        eventMapping.put("LB.CERT", Taxonomies.Resource.SERVICE_OSS_PERFORMANCE);

        eventMapping.put("GLOBAL.LB", Taxonomies.Resource.SERVICE_OSS_PERFORMANCE);

        eventMapping.put("ROLE", Taxonomies.Resource.DATA_SECURITY_GROUP);
        eventMapping.put("ROLE.PERMISSION", Taxonomies.Resource.DATA_SECURITY_GROUP);

        eventMapping.put("CA.CERTIFICATE", Taxonomies.Resource.DATA_SECURITY_CREDENTIAL);

        eventMapping.put("ACCOUNT", Taxonomies.Resource.DATA_SECURITY_ACCOUNT);
        eventMapping.put("ACCOUNT.MARK.DEFAULT", Taxonomies.Resource.DATA_SECURITY_ACCOUNT);

        eventMapping.put("USER", Taxonomies.Resource.DATA_SECURITY_ACCOUNT_USER);

        eventMapping.put("REGISTER.SSH", Taxonomies.Resource.DATA_SECURITY_KEY);
        eventMapping.put("REGISTER.USER", Taxonomies.Resource.DATA_SECURITY_KEY);

        eventMapping.put("TEMPLATE", Taxonomies.Resource.DATA_TEMPLATE);
        eventMapping.put("TEMPLATE.DOWNLOAD", Taxonomies.Resource.DATA_TEMPLATE);

        eventMapping.put("VOLUME", Taxonomies.Resource.STORAGE_VOLUME);
        eventMapping.put("VOLUME.DETAIL", Taxonomies.Resource.STORAGE_VOLUME);

        eventMapping.put("DOMAIN", Taxonomies.Resource.NETWORK_DOMAIN);

        eventMapping.put("SNAPSHOT", Taxonomies.Resource.SERVICE_IMAGE);
        eventMapping.put("SNAPSHOTPOLICY", Taxonomies.Resource.SERVICE_IMAGE);

        eventMapping.put("ISO", Taxonomies.Resource.DATA_IMAGE);

        eventMapping.put("SSVM", Taxonomies.Resource.UNKNOWN);

        eventMapping.put("SERVICE.OFFERING", Taxonomies.Resource.SYSTEM);
        eventMapping.put("DISK.OFFERING", Taxonomies.Resource.SYSTEM);
        eventMapping.put("NETWORK.OFFERING", Taxonomies.Resource.SYSTEM);

        eventMapping.put("POD", Taxonomies.Resource.DATA_SECURITY_GROUP);

        eventMapping.put("ZONE", Taxonomies.Resource.DATA_SECURITY_GROUP);

        eventMapping.put("VLAN.IP.RANGE", Taxonomies.Resource.SERVICE_NETWORK);
        eventMapping.put("MANAGEMENT.IP.RANGE", Taxonomies.Resource.SERVICE_NETWORK);
        eventMapping.put("STORAGE.IP.RANGE", Taxonomies.Resource.SERVICE_NETWORK);

        eventMapping.put("CONFIGURATION.VALUE", Taxonomies.Resource.SYSTEM);

        eventMapping.put("SG", Taxonomies.Resource.DATA_SECURITY_GROUP);
        eventMapping.put("SG.AUTH", Taxonomies.Resource.DATA_SECURITY_GROUP);
        eventMapping.put("SG.REVOKE", Taxonomies.Resource.DATA_SECURITY_GROUP);

        eventMapping.put("HOST", Taxonomies.Resource.NETWORK_NODE_HOST);
        eventMapping.put("HOST.OOBM", Taxonomies.Resource.NETWORK_NODE_HOST);

        eventMapping.put("HA.RESOURCE", Taxonomies.Resource.UNKNOWN);
        eventMapping.put("HA.STATE", Taxonomies.Resource.UNKNOWN);

        eventMapping.put("MAINT", Taxonomies.Resource.UNKNOWN);
        eventMapping.put("MAINT.CANCEL", Taxonomies.Resource.UNKNOWN);
        eventMapping.put("MAINT.PREPARE", Taxonomies.Resource.UNKNOWN);

        eventMapping.put("VPN", Taxonomies.Resource.NETWORK_CLUSTER);
        eventMapping.put("VPN.REMOTE.ACCESS", Taxonomies.Resource.NETWORK_CLUSTER);
        eventMapping.put("VPN.USER", Taxonomies.Resource.NETWORK_CLUSTER);
        eventMapping.put("VPN.S2S.VPN.GATEWAY", Taxonomies.Resource.NETWORK_CLUSTER);
        eventMapping.put("VPN.S2S.CUSTOMER.GATEWAY", Taxonomies.Resource.NETWORK_CLUSTER);
        eventMapping.put("VPN.S2S.CONNECTION", Taxonomies.Resource.NETWORK_CLUSTER);

        eventMapping.put("UPLOAD.CUSTOM", Taxonomies.Resource.DATA_SECURITY_CREDENTIAL);

        eventMapping.put("STATICNAT", Taxonomies.Resource.NETWORK_CLUSTER);
        eventMapping.put("ZONE.VLAN", Taxonomies.Resource.NETWORK_CLUSTER);

        eventMapping.put("PROJECT", Taxonomies.Resource.SERVICE_COMPOSITION);
        eventMapping.put("PROJECT.ACCOUNT", Taxonomies.Resource.SERVICE_COMPOSITION);
        eventMapping.put("PROJECT.INVITATION", Taxonomies.Resource.SERVICE_COMPOSITION);

        eventMapping.put("NETWORK.ELEMENT", Taxonomies.Resource.SERVICE_NETWORK);

        eventMapping.put("PHYSICAL.NETWORK", Taxonomies.Resource.NETWORK_DOMAIN);

        eventMapping.put("SERVICE.PROVIDER", Taxonomies.Resource.NETWORK_DOMAIN);

        eventMapping.put("TRAFFIC.TYPE", Taxonomies.Resource.NETWORK_DOMAIN);

        eventMapping.put("PHYSICAL.LOADBALANCER", Taxonomies.Resource.NETWORK_NODE);

        eventMapping.put("PHYSICAL.NCC", Taxonomies.Resource.NETWORK_NODE);

        eventMapping.put("SWITCH.MGMT", Taxonomies.Resource.NETWORK_NODE);
        eventMapping.put("PHYSICAL.FIREWALL", Taxonomies.Resource.NETWORK_NODE);

        eventMapping.put("VPC", Taxonomies.Resource.UNKNOWN);

        eventMapping.put("NETWORK.ACL", Taxonomies.Resource.DATA_SECURITY_ROLE);

        eventMapping.put("VPC.OFFERING", Taxonomies.Resource.SYSTEM);

        eventMapping.put("PRIVATE.GATEWAY", Taxonomies.Resource.NETWORK_NODE);

        eventMapping.put("STATIC.ROUTE", Taxonomies.Resource.UNKNOWN);

        eventMapping.put("CREATE_TAGS", Taxonomies.Resource.UNKNOWN);
        eventMapping.put("DELETE_TAGS", Taxonomies.Resource.UNKNOWN);

        eventMapping.put("CREATE_RESOURCE_DETAILS", Taxonomies.Resource.UNKNOWN);
        eventMapping.put("DELETE_RESOURCE_DETAILS", Taxonomies.Resource.UNKNOWN);

        eventMapping.put("VMSNAPSHOT", Taxonomies.Resource.UNKNOWN);

        eventMapping.put("PHYSICAL", Taxonomies.Resource.UNKNOWN);
        eventMapping.put("PHYSICAL.NVPCONTROLLER", Taxonomies.Resource.UNKNOWN);
        eventMapping.put("PHYSICAL.OVSCONTROLLER", Taxonomies.Resource.UNKNOWN);
        eventMapping.put("PHYSICAL.NUAGE.VSD", Taxonomies.Resource.UNKNOWN);

        eventMapping.put("COUNTER", Taxonomies.Resource.UNKNOWN);

        eventMapping.put("CONDITION", Taxonomies.Resource.UNKNOWN);

        eventMapping.put("AUTOSCALEPOLICY", Taxonomies.Resource.UNKNOWN);
        eventMapping.put("AUTOSCALEVMPROFILE", Taxonomies.Resource.UNKNOWN);
        eventMapping.put("AUTOSCALEVMGROUP", Taxonomies.Resource.UNKNOWN);

        eventMapping.put("PHYSICAL.DHCP", Taxonomies.Resource.UNKNOWN);
        eventMapping.put("PHYSICAL.PXE", Taxonomies.Resource.UNKNOWN);
        eventMapping.put("BAREMETAL.RCT", Taxonomies.Resource.UNKNOWN);
        eventMapping.put("BAREMETAL.PROVISION", Taxonomies.Resource.UNKNOWN);

        eventMapping.put("AG", Taxonomies.Resource.UNKNOWN);
        eventMapping.put("VM.AG", Taxonomies.Resource.UNKNOWN);
        eventMapping.put("INTERNALLBVM", Taxonomies.Resource.UNKNOWN);
        eventMapping.put("HOST.RESERVATION", Taxonomies.Resource.UNKNOWN);
        eventMapping.put("GUESTVLANRANGE", Taxonomies.Resource.UNKNOWN);

        eventMapping.put("PORTABLE.IP.RANGE", Taxonomies.Resource.UNKNOWN);
        eventMapping.put("PORTABLE.IP", Taxonomies.Resource.UNKNOWN);

        eventMapping.put("DEDICATE", Taxonomies.Resource.UNKNOWN);
        eventMapping.put("DEDICATE.RESOURCE", Taxonomies.Resource.UNKNOWN);

        eventMapping.put("VM.RESERVATION", Taxonomies.Resource.UNKNOWN);
        eventMapping.put("UCS", Taxonomies.Resource.UNKNOWN);
        eventMapping.put("MIGRATE.PREPARE", Taxonomies.Resource.UNKNOWN);
        eventMapping.put("ALERT", Taxonomies.Resource.UNKNOWN);
        eventMapping.put("PHYSICAL.ODLCONTROLLER", Taxonomies.Resource.UNKNOWN);

        eventMapping.put("GUEST.OS", Taxonomies.Resource.UNKNOWN);
        eventMapping.put("GUEST.OS.MAPPING", Taxonomies.Resource.UNKNOWN);

        eventMapping.put("NIC.SECONDARY.IP", Taxonomies.Resource.UNKNOWN);
        eventMapping.put("EXTERNAL.DHCP.VM.IP", Taxonomies.Resource.UNKNOWN);
        eventMapping.put("USAGE.REMOVE.USAGE", Taxonomies.Resource.UNKNOWN);
        eventMapping.put("NETSCALER.SERVICEPACKAGE", Taxonomies.Resource.UNKNOWN);
        eventMapping.put("NETSCALERVM", Taxonomies.Resource.UNKNOWN);
        eventMapping.put("ANNOTATION", Taxonomies.Resource.UNKNOWN);
        eventMapping.put("TEMPLATE.DIRECT.DOWNLOAD", Taxonomies.Resource.UNKNOWN);
        eventMapping.put("ISO.DIRECT.DOWNLOAD", Taxonomies.Resource.UNKNOWN);
        eventMapping.put("SYSTEM.VM", Taxonomies.Resource.UNKNOWN);

    }

}
