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
    type: 'RUNNING_VM',
    chartColor: '#1890ff'
  },
  {
    id: 2,
    type: 'ALLOCATED_VM',
    chartColor: '#fadb14'
  },
  {
    id: 3,
    type: 'IP_ADDRESS',
    chartColor: '#ffd6e7'
  },
  {
    id: 4,
    type: 'NETWORK_BYTES_SENT',
    chartColor: '#adc6ff'
  },
  {
    id: 5,
    type: 'NETWORK_BYTES_RECEIVED',
    chartColor: '#10239e'
  },
  {
    id: 6,
    type: 'VOLUME',
    chartColor: '#722ed1'
  },
  {
    id: 7,
    type: 'TEMPLATE',
    chartColor: '#08979c'
  },
  {
    id: 8,
    type: 'ISO',
    chartColor: '#87e8de'
  },
  {
    id: 9,
    type: 'SNAPSHOT',
    chartColor: '#f5222d'
  },
  {
    id: 10,
    type: 'SECURITY_GROUP',
    chartColor: '#d46b08'
  },
  {
    id: 11,
    type: 'LOAD_BALANCER_POLICY',
    chartColor: '#ffd666'
  },
  {
    id: 12,
    type: 'PORT_FORWARDING_RULE',
    chartColor: '#7cb305'
  },
  {
    id: 13,
    type: 'NETWORK_OFFERING',
    chartColor: '#ffbb96'
  },
  {
    id: 14,
    type: 'VPN_USERS',
    chartColor: '#95de64'
  },
  {
    id: 21,
    type: 'VM_DISK_IO_READ',
    chartColor: '#ffe7ba'
  },
  {
    id: 22,
    type: 'VM_DISK_IO_WRITE',
    chartColor: '#5b8c00'
  },
  {
    id: 23,
    type: 'VM_DISK_BYTES_READ',
    chartColor: '#0050b3'
  },
  {
    id: 24,
    type: 'VM_DISK_BYTES_WRITE',
    chartColor: '#520339'
  },
  {
    id: 25,
    type: 'VM_SNAPSHOT',
    chartColor: '#9e1068'
  },
  {
    id: 26,
    type: 'VOLUME_SECONDARY',
    chartColor: '#061178'
  },
  {
    id: 27,
    type: 'VM_SNAPSHOT_ON_PRIMARY',
    chartColor: '#ad2102'
  },
  {
    id: 28,
    type: 'BACKUP',
    chartColor: '#00474f'
  },
  {
    id: 29,
    type: 'BUCKET',
    chartColor: '#13a8a8'
  },
  {
    id: 30,
    type: 'NETWORK',
    chartColor: '#c75314'
  },
  {
    id: 31,
    type: 'VPC',
    chartColor: '#018391'
  }
]

export const getQuotaTypes = () => {
  return QUOTA_TYPES.sort((a, b) => a.type.localeCompare(b.type))
}

export const getQuotaTypeByName = (type) => {
  return QUOTA_TYPES.find(quotaType => quotaType.type === type)
}
