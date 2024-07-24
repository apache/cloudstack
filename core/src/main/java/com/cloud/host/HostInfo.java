//
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
//

package com.cloud.host;

public final class HostInfo {
    public static final String HYPERVISOR_VERSION = "Hypervisor.Version"; //tricky since KVM has userspace version and kernel version
    public static final String HOST_OS = "Host.OS"; //Fedora, XenServer, Ubuntu, etc
    public static final String HOST_OS_VERSION = "Host.OS.Version"; //12, 5.5, 9.10, etc
    public static final String HOST_OS_KERNEL_VERSION = "Host.OS.Kernel.Version"; //linux-2.6.31 etc
    public static final String XS620_SNAPSHOT_HOTFIX = "xs620_snapshot_hotfix";
}
