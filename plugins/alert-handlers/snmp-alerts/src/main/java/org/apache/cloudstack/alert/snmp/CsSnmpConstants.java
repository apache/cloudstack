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
// under the License

package org.apache.cloudstack.alert.snmp;

/**
 * <p/>
 * IMPORTANT
 * <p/>
 * These OIDs are based on <b>CS-ROOT-MIB</b> MIB file. If there is any change in MIB file
 * then that should be reflected in this file also *
 * <br/><br/>
 * suffix 2 due to conflict with SnmpConstants class of snmp4j
 */
public class CsSnmpConstants {
    public static final String CLOUDSTACK = "1.3.6.1.4.1.18060.15";

    public static final String OBJECTS_PREFIX = CLOUDSTACK + ".1.1.";

    public static final String TRAPS_PREFIX = CLOUDSTACK + ".1.2.0.";

    public static final String DATA_CENTER_ID = OBJECTS_PREFIX + 1;

    public static final String POD_ID = OBJECTS_PREFIX + 2;

    public static final String CLUSTER_ID = OBJECTS_PREFIX + 3;

    public static final String MESSAGE = OBJECTS_PREFIX + 4;

    public static final String GENERATION_TIME = OBJECTS_PREFIX + 5;
}
