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

package com.cloud.upgrade;

import java.util.HashMap;
import java.util.Map;

import com.cloud.hypervisor.Hypervisor;

public class OfficialSystemVMTemplate {

    final static Map<Hypervisor.HypervisorType, String> NewTemplateNameList = new HashMap<Hypervisor.HypervisorType, String>() {
        {
            put(Hypervisor.HypervisorType.KVM, "systemvm-kvm-4.11.3");
            put(Hypervisor.HypervisorType.VMware, "systemvm-vmware-4.11.3");
            put(Hypervisor.HypervisorType.XenServer, "systemvm-xenserver-4.11.3");
            put(Hypervisor.HypervisorType.Hyperv, "systemvm-hyperv-4.11.3");
            put(Hypervisor.HypervisorType.LXC, "systemvm-lxc-4.11.3");
            put(Hypervisor.HypervisorType.Ovm3, "systemvm-ovm3-4.11.3");
        }
    };

    final static Map<Hypervisor.HypervisorType, String> routerTemplateConfigurationNames = new HashMap<Hypervisor.HypervisorType, String>() {
        {
            put(Hypervisor.HypervisorType.KVM, "router.template.kvm");
            put(Hypervisor.HypervisorType.VMware, "router.template.vmware");
            put(Hypervisor.HypervisorType.XenServer, "router.template.xenserver");
            put(Hypervisor.HypervisorType.Hyperv, "router.template.hyperv");
            put(Hypervisor.HypervisorType.LXC, "router.template.lxc");
            put(Hypervisor.HypervisorType.Ovm3, "router.template.ovm3");
        }
    };

    final static Map<Hypervisor.HypervisorType, String> newTemplateUrl = new HashMap<Hypervisor.HypervisorType, String>() {
        {
            put(Hypervisor.HypervisorType.KVM, "https://download.cloudstack.org/systemvm/4.11/systemvmtemplate-4.11.3-kvm.qcow2.bz2");
            put(Hypervisor.HypervisorType.VMware, "https://download.cloudstack.org/systemvm/4.11/systemvmtemplate-4.11.3-vmware.ova");
            put(Hypervisor.HypervisorType.XenServer, "https://download.cloudstack.org/systemvm/4.11/systemvmtemplate-4.11.3-xen.vhd.bz2");
            put(Hypervisor.HypervisorType.Hyperv, "https://download.cloudstack.org/systemvm/4.11/systemvmtemplate-4.11.3-hyperv.vhd.zip");
            put(Hypervisor.HypervisorType.LXC, "https://download.cloudstack.org/systemvm/4.11/systemvmtemplate-4.11.3-kvm.qcow2.bz2");
            put(Hypervisor.HypervisorType.Ovm3, "https://download.cloudstack.org/systemvm/4.11/systemvmtemplate-4.11.3-ovm.raw.bz2");
        }
    };

    final static Map<Hypervisor.HypervisorType, String> newTemplateFileType = new HashMap<Hypervisor.HypervisorType, String>() {
        {
            put(Hypervisor.HypervisorType.KVM, "qcow2");
            put(Hypervisor.HypervisorType.VMware, "ova");
            put(Hypervisor.HypervisorType.XenServer, "vhd");
            put(Hypervisor.HypervisorType.Hyperv, "vhd");
            put(Hypervisor.HypervisorType.LXC, "qcow2");
            put(Hypervisor.HypervisorType.Ovm3, "raw");
        }
    };

    final static Map<Hypervisor.HypervisorType, String> newTemplateChecksum = new HashMap<Hypervisor.HypervisorType, String>() {
        {
            put(Hypervisor.HypervisorType.KVM, "15ec268d0939a8fa0be1bc79f397a167");
            put(Hypervisor.HypervisorType.XenServer, "ae96f35fb746524edc4ebc9856719d71");
            put(Hypervisor.HypervisorType.VMware, "f50c82139430afce7e4e46d3a585abbd");
            put(Hypervisor.HypervisorType.Hyperv, "abf411f6cdd9139716b5d8172ab903a6");
            put(Hypervisor.HypervisorType.LXC, "15ec268d0939a8fa0be1bc79f397a167");
            put(Hypervisor.HypervisorType.Ovm3, "c71f143a477f4c7a0d5e8c82ccb00220");
        }
    };

    public static Map<Hypervisor.HypervisorType, String> getNewTemplateNameList() {
        return NewTemplateNameList;
    }

    public static Map<Hypervisor.HypervisorType, String> getRouterTemplateConfigurationNames() {
        return routerTemplateConfigurationNames;
    }

    public static Map<Hypervisor.HypervisorType, String> getNewTemplateUrl() {
        return newTemplateUrl;
    }

    public static Map<Hypervisor.HypervisorType, String> getNewTemplateFiletype() {
        return newTemplateFileType;
    }

    public static Map<Hypervisor.HypervisorType, String> getNewTemplateChecksum() {
        return newTemplateChecksum;
    }
}
