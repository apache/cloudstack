# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
"""
@Desc     : This module defines all codes, constants maintained globally \
            and used across marvin and its test features.The main purpose \
            is to maintain readability, maintain one common place for \
            all codes used or reused across test features. It enhances \
            maintainability and readability. Users just import statement \
            to receive all the codes mentioned here. EX: Here, we define \
            a code viz., ENABLED  with value "Enabled",then using \
            this code in a sample feature say test_a.py as below. \

            from codes import *
            if obj.getvalue() == ENABLED

@DateAdded: 20th October 2013
"""

'''
VM STATES - START
'''
RUNNING = "Running"
STOPPED = "Stopped"
STOPPING = "Stopping"
STARTING = "Starting"
DESTROYED = "Destroyed"
EXPUNGING = "Expunging"
'''
VM STATES - END
'''

'''
Snapshot States - START
'''
BACKED_UP = "backedup"
BACKING_UP = "backingup"
'''
Snapshot States - END
'''

RECURRING = "RECURRING"
ENABLED = "Enabled"
DISABLED = "Disabled"
ENABLE = "Enable"
DISABLE = "Disable"
NETWORK_OFFERING = "network_offering"
ROOT = "ROOT"
DATA = "DATA"
INVALID_INPUT = "INVALID INPUT"
EMPTY_LIST = "EMPTY_LIST"
FAIL = 0
PASS = 1
MATCH_NOT_FOUND = "ELEMENT NOT FOUND IN THE INPUT"
SUCCESS = "SUCCESS"
EXCEPTION_OCCURRED = "Exception Occurred"
NO = "no"
YES = "yes"
FAILED = "FAILED"
UNKNOWN_ERROR = "Unknown Error"
EXCEPTION = "EXCEPTION"
INVALID_RESPONSE = "Invalid Response"
'''
Async Job Related Codes
'''
JOB_INPROGRESS = 0
JOB_SUCCEEDED = 1
JOB_FAILED = 2
JOB_CANCELLED = 3
'''
User Related Codes
'''
BASIC_ZONE = "basic"
ISOLATED_NETWORK = "ISOLATED"
SHARED_NETWORK = "SHARED"
VPC_NETWORK = "VPC"
ERROR_NO_HOST_FOR_MIGRATION = \
    "Could not find suitable host for migration, " \
    "please ensure setup has required no. of hosts"
NAT_RULE = "nat rule"
STATIC_NAT_RULE = "static nat rule"
LB_RULE = "Load Balancer Rule"
UNKNOWN = "UNKNOWN"
FAULT = "FAULT"
PRIMARY = "PRIMARY"
ADMIN = 1
DOMAIN_ADMIN = 2
USER = 0
XEN_SERVER = "XenServer"
ADMIN_ACCOUNT = 'ADMIN_ACCOUNT'
USER_ACCOUNT = 'USER_ACCOUNT'
RESOURCE_USER_VM = 0
RESOURCE_PUBLIC_IP = 1
RESOURCE_VOLUME = 2
RESOURCE_SNAPSHOT = 3
RESOURCE_TEMPLATE = 4
RESOURCE_PROJECT = 5
RESOURCE_NETWORK = 6
RESOURCE_VPC = 7
RESOURCE_CPU = 8
RESOURCE_MEMORY = 9
RESOURCE_PRIMARY_STORAGE = 10
RESOURCE_SECONDARY_STORAGE = 11
KVM = "kvm"
VMWARE = "vmware"
ROOT_DOMAIN_ADMIN="root domain admin"
CHILD_DOMAIN_ADMIN="child domain admin"

'''
Network states
'''
ALLOCATED = "Allocated"

'''
Host states
'''
HOST_CREATING = "Creating"
HOST_CONNECTING = "Connecting"
HOST_UP = "Up"
HOST_DOWN = "Down"
HOST_DISCONNECTED = "Disconnected"
HOST_ALERT = "Alert"
HOST_REMOVED = "Removed"
HOST_ERROR = "Error"
HOST_REBALANCING = "Rebalancing"
HOST_UNKNOWN = "Unknown"

'''
Host resource states
'''
HOST_RS_CREATING = "Creating"
HOST_RS_ENABLED = "Enabled"
HOST_RS_DISABLED = "Disabled"
HOST_RS_PREPARE_FOR_MAINTENANCE = "PrepareForMaintenance"
HOST_RS_ERROR_IN_MAINTENANCE = "ErrorInMaintenance"
HOST_RS_MAINTENANCE = "Maintenance"
HOST_RS_ERROR = "Error"

'''
Storage Tags
'''
ZONETAG1 = "zwps1"
ZONETAG2 = "zwps2"
CLUSTERTAG1 = "cwps1"
CLUSTERTAG2 = "cwps2"

'''
Traffic Types
'''
PUBLIC_TRAFFIC = "public"
GUEST_TRAFFIC = "guest"
MANAGEMENT_TRAFFIC = "management"
STORAGE_TRAFFIC = "storage"

'''
Switch Type
'''
VMWAREDVS = "vmwaredvs"

'''
Storage Pools State
'''

UP = "up"

'''
Storage Pools Scope
'''

CLUSTER = "cluster"
DATA = "DATA"
