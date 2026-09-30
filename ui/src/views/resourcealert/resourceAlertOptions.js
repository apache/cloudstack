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

export const METRICS_BY_TYPE = {
  VirtualMachine: ['CPU_UTILIZATION', 'MEMORY_UTILIZATION', 'DISK_READ_IOPS', 'DISK_WRITE_IOPS', 'DISK_READ_KBPS', 'DISK_WRITE_KBPS', 'NETWORK_READ_KBPS', 'NETWORK_WRITE_KBPS'],
  Host: ['CPU_UTILIZATION', 'MEMORY_UTILIZATION', 'LOAD_AVERAGE', 'NETWORK_READ_KBPS', 'NETWORK_WRITE_KBPS'],
  Volume: ['VOLUME_USED_GB', 'VOLUME_UTILIZATION'],
  StoragePool: ['STORAGE_UTILIZATION', 'STORAGE_USED_IOPS']
}

export const CONDITIONS = ['GT', 'GTE', 'LT', 'LTE', 'EQ']

export const SEVERITIES = ['CRITICAL', 'HIGH', 'MEDIUM', 'LOW']

export const RESOURCE_TYPE_LABELS = {
  VirtualMachine: 'Virtual Machine',
  Host: 'Host',
  Volume: 'Volume',
  StoragePool: 'Storage Pool'
}

export const METRIC_LABELS = {
  CPU_UTILIZATION: 'CPU Utilization %',
  MEMORY_UTILIZATION: 'Memory Utilization %',
  DISK_READ_IOPS: 'Disk Read IOPS',
  DISK_WRITE_IOPS: 'Disk Write IOPS',
  DISK_READ_KBPS: 'Disk Read KB/s',
  DISK_WRITE_KBPS: 'Disk Write KB/s',
  NETWORK_READ_KBPS: 'Network In KB/s',
  NETWORK_WRITE_KBPS: 'Network Out KB/s',
  STORAGE_UTILIZATION: 'Storage Utilization %',
  LOAD_AVERAGE: 'Load Average',
  VOLUME_USED_GB: 'Volume Used (GB)',
  VOLUME_UTILIZATION: 'Volume Used %',
  STORAGE_USED_IOPS: 'Storage Used IOPS'
}

export const CONDITION_LABELS = {
  GT: 'Is above',
  GTE: 'Is above or equal to',
  LT: 'Is below',
  LTE: 'Is below or equal to',
  EQ: 'Equals'
}

export const SEVERITY_LABELS = {
  CRITICAL: 'Critical',
  HIGH: 'High',
  MEDIUM: 'Medium',
  LOW: 'Low'
}

const LABELS_BY_FIELD = {
  resourcetype: RESOURCE_TYPE_LABELS,
  metric: METRIC_LABELS,
  metrictype: METRIC_LABELS,
  condition: CONDITION_LABELS,
  severity: SEVERITY_LABELS
}

export function resourceAlertLabel (field, value) {
  const labels = LABELS_BY_FIELD[field]
  return (labels && labels[value]) || value
}
