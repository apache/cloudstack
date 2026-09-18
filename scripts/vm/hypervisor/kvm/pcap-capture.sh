#!/bin/sh

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

# CloudStack example packet capture script.
#
# Executed by cloudstack-pcap@<interface>.service when packet capture is
# enabled on a VM NIC through the CloudStack API. Every environment has
# different needs (capture filters, output location, shipping the data to a
# central collector, retention, etc.), so this script is only an example.
#
# This file is owned by the cloudstack-common package and is replaced on
# upgrade. To run your own capture, copy the unit file
# /usr/lib/systemd/system/cloudstack-pcap@.service, point its ExecStart at
# your own script and set the property packet.capture.service in
# agent.properties to the name of your unit.
#
# The CloudStack agent provides the NIC context in the environment:
#
#   CS_VM_NAME       VM instance name (e.g. i-2-15-VM)
#   CS_VM_UUID       VM UUID
#   CS_NIC_UUID      NIC UUID
#   CS_NIC_MAC       NIC MAC address
#   CS_NIC_DEV       host-side tap device (e.g. vnet3)
#   CS_NIC_BRIDGE    bridge the device is attached to
#   CS_NIC_IP4       NIC IPv4 address (may be empty)
#   CS_NIC_IP6       NIC IPv6 address (may be empty)
#   CS_NETWORK_UUID  UUID of the CloudStack network the NIC belongs to
#
# The capture must run in the foreground; systemd stops it with SIGTERM
# when capture is disabled or the tap device disappears.

set -eu

OUTPUT_DIR="/tmp"
OUTPUT_FILE="${OUTPUT_DIR}/${CS_VM_NAME}-${CS_NIC_MAC}.pcap"

# Capture all traffic on the NIC, rotating over two files of 256 MB each.
exec tcpdump -i "${CS_NIC_DEV}" -w "${OUTPUT_FILE}" -C 256 -W 2 -Z root
