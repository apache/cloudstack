// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// the License.  You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

// Note: it could be retrieved from an API
export const QUOTA_TYPES = [
  {
    id: 1,
    type: 'RUNNING_VM'
  },
  {
    id: 2,
    type: 'ALLOCATED_VM'
  },
  {
    id: 3,
    type: 'IP_ADDRESS'
  },
  {
    id: 4,
    type: 'NETWORK_BYTES_SENT'
  },
  {
    id: 5,
    type: 'NETWORK_BYTES_RECEIVED'
  },
  {
    id: 6,
    type: 'VOLUME'
  },
  {
    id: 7,
    type: 'TEMPLATE'
  },
  {
    id: 8,
    type: 'ISO'
  },
  {
    id: 9,
    type: 'SNAPSHOT'
  },
  {
    id: 10,
    type: 'SECURITY_GROUP'
  },
  {
    id: 11,
    type: 'LOAD_BALANCER_POLICY'
  },
  {
    id: 12,
    type: 'PORT_FORWARDING_RULE'
  },
  {
    id: 13,
    type: 'NETWORK_OFFERING'
  },
  {
    id: 14,
    type: 'VPN_USERS'
  },
  {
    id: 21,
    type: 'VM_DISK_IO_READ'
  },
  {
    id: 22,
    type: 'VM_DISK_IO_WRITE'
  },
  {
    id: 23,
    type: 'VM_DISK_BYTES_READ'
  },
  {
    id: 24,
    type: 'VM_DISK_BYTES_WRITE'
  },
  {
    id: 25,
    type: 'VM_SNAPSHOT'
  },
  {
    id: 26,
    type: 'VOLUME_SECONDARY'
  },
  {
    id: 27,
    type: 'VM_SNAPSHOT_ON_PRIMARY'
  },
  {
    id: 28,
    type: 'BACKUP'
  },
  {
    id: 29,
    type: 'VPC'
  },
  {
    id: 30,
    type: 'NETWORK'
  },
  {
    id: 31,
    type: 'BACKUP_OBJECT'
  }
]

export const getQuotaTypes = () => {
  return QUOTA_TYPES.sort((a, b) => a.type.localeCompare(b.type))
}
