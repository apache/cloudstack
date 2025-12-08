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
package org.apache.cloudstack.api;

/**
 * metric local api constants
 */
public interface MetricConstants {
    String AGENT_COUNT = "agentcount";
    String AVAILABLE_PROCESSORS = "availableprocessors";
    String CONNECTIONS = "connections";
    String DATABASE_IS_LOCAL = "dbislocal";
    String DATABASE_LOAD_AVERAGES = "dbloadaverages";
    String HEAP_MEMORY_USED = "heapmemoryused";
    String HEAP_MEMORY_TOTAL = "heapmemorytotal";
    String LAST_HEARTBEAT = "lastheartbeat";
    String LAST_SUCCESSFUL_JOB = "lastsuccessfuljob";
    String logger_INFO = "loginfo";
    String REPLICAS = "replicas";
    String SESSIONS = "sessions";
    String SYSTEM = "system";
    String SYSTEM_CYCLES = "systemtotalcpucycles";
    String SYSTEM_CYCLE_USAGE = "systemcycleusage";
    String SYSTEM_LOAD_AVERAGES = "systemloadaverages";
    String SYSTEM_MEMORY_FREE = "systemmemoryfree";
    String SYSTEM_MEMORY_TOTAL = "systemmemorytotal";
    String SYSTEM_MEMORY_USED = "systemmemoryused";
    String SYSTEM_MEMORY_VIRTUALSIZE = "systemmemoryvirtualsize";
    String THREADS_BLOCKED_COUNT = "threadsblockedcount";
    String THREADS_DAEMON_COUNT = "threadsdaemoncount";
    String THREADS_RUNNABLE_COUNT = "threadsrunnablecount";
    String THREADS_TERMINATED_COUNT = "threadsteminatedcount";
    String THREADS_TOTAL_COUNT = "threadstotalcount";
    String THREADS_WAITING_COUNT = "threadswaitingcount";
    String TLS_VERSIONS = "tlsversions";
    String UPTIME = "uptime";
    String USAGE_IS_LOCAL = "usageislocal";
    String VERSION_COMMENT = "versioncomment";
    String QUERIES = "queries";
    String COLLECTION_TIME = "collectiontime";
    String CPULOAD = "cpuload";
}
