<!--
  Licensed to the Apache Software Foundation (ASF) under one
  or more contributor license agreements.  See the NOTICE file
  distributed with this work for additional information
  regarding copyright ownership.  The ASF licenses this file
  to you under the Apache License, Version 2.0 (the
  "License"); you may not use this file except in compliance
  with the License.  You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing,
  software distributed under the License is distributed on an
  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
  KIND, either express or implied.  See the License for the
  specific language governing permissions and limitations
  under the License.
-->

# Apache CloudStack - NetApp ONTAP Storage Plugin

## Overview

The NetApp ONTAP Storage Plugin provides integration between Apache CloudStack and NetApp ONTAP storage systems. This plugin enables CloudStack to provision and manage primary storage on ONTAP clusters, supporting both NAS (NFS) and SAN (iSCSI) protocols.

## Features

- **Primary Storage Support**: Provision and manage primary storage pools on NetApp ONTAP
- **Multiple Protocols**: Support for NFS 3.0 and iSCSI protocols
- **Unified Storage**: Integration with traditional ONTAP unified storage architecture
- **KVM Hypervisor Support**: Supports KVM hypervisor environments
- **Managed Storage**: Operates as managed storage with full lifecycle management
- **Flexible Scoping**: Support for Zone-wide and Cluster-scoped storage pools

## Architecture

### Component Structure

| Package | Description                                           |
|---------|-------------------------------------------------------|
| `driver` | Primary datastore driver implementation               |
| `feign` | REST API clients and data models for ONTAP operations |
| `lifecycle` | Storage pool lifecycle management                     |
| `listener` | Host connection event handlers                        |
| `provider` | Main provider and strategy factory                    |
| `service` | ONTAP Storage strategy implementations (NAS/SAN)      |
| `utils` | Constants and helper utilities                        |

## Requirements

### ONTAP Requirements

- NetApp ONTAP 9.15.1 or higher
- Storage Virtual Machine (SVM) configured with appropriate protocols enabled
- Management LIF accessible from CloudStack management server
- Data LIF(s) accessible from hypervisor hosts and are of IPv4 type
- Aggregates assigned to the SVM with sufficient capacity

### CloudStack Requirements

- Apache CloudStack current version or higher
- KVM hypervisor hosts
- For iSCSI: Hosts must have iSCSI initiator configured with valid IQN
- For NFS: Hosts must have NFS client packages installed

### Minimum Volume Size

ONTAP requires a minimum volume size of **1.56 GB** (1,677,721,600 bytes). The plugin will automatically adjust requested sizes below this threshold.

## Configuration

### Storage Pool Creation Parameters

When creating an ONTAP primary storage pool, provide the following details in the URL field (semicolon-separated key=value pairs):

| Parameter | Required | Description |
|-----------|----------|-------------|
| `username` | Yes | ONTAP cluster admin username |
| `password` | Yes | ONTAP cluster admin password |
| `svmName` | Yes | Storage Virtual Machine name |
| `protocol` | Yes | Storage protocol (`NFS3` or `ISCSI`) |
| `managementLIF` | Yes | ONTAP cluster management LIF IP address |

### Example URL Format

```
username=admin;password=secretpass;svmName=svm1;protocol=ISCSI;managementLIF=192.168.1.100
```

## Port Configuration

| Protocol | Default Port |
|----------|--------------|
| NFS | 2049 |
| iSCSI | 3260 |
| ONTAP Management API | 443 (HTTPS) |

## Limitations

- Supports only **KVM** hypervisor
- Supports only **Unified ONTAP** storage (disaggregated not supported)
- Supports only **NFS3** and **iSCSI** protocols
- IPv6 type and FQDN LIFs are not supported

## Troubleshooting

### Common Issues

1. **Connection Failures**
    - Verify management LIF is reachable from CloudStack management server
    - Check firewall rules for port 443

2. **Protocol Errors**
    - Ensure the protocol (NFS/iSCSI) is enabled on the SVM
    - Verify Data LIFs are configured for the protocol

3. **Capacity Errors**
    - Check aggregate space availability
    - Ensure requested volume size meets minimum requirements (1.56 GB)

4. **Host Connection Issues**
    - For iSCSI: Verify host IQN is properly configured in host's storage URL
    - For NFS: Ensure NFS client is installed and running
