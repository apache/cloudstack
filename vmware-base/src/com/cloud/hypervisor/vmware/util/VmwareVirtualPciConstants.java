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
package com.cloud.hypervisor.vmware.util;

public interface VmwareVirtualPciConstants {
    static final String PCI_BUS_DOMAIN = "pci-0000";
    static final String PCI_BUS_PATH_SEPARATOR_DOMAIN_BUS = ":";
    static final String PCI_BUS_PATH_SEPARATOR_BUS_DEVICE = ":";
    static final String PCI_BUS_PATH_SEPARATOR_DEVICE_FUNC = ".";
    static final String PCI_BUS_PATH_SEPARATOR_FUNC_UNIT = "-";
    static final String[] PCI_BUS_LSILOGIC_CONTROLLER = {"00", "02", "02", "02"};
    static final String PCI_FUNC_LSILOGIC_CONTROLLER = "0";
    static final String[] PCI_DEVICE_LSILOGIC_CONTROLLER = {"10", "00", "01", "02"};
    static final String PCI_DEVICE_UNIT_PREFIX = "scsi-0:0:";
    static final String PCI_DEVICE_UNIT_POSTFIX = ":0";
}
