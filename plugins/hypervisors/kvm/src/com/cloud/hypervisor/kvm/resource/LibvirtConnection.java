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
package com.cloud.hypervisor.kvm.resource;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.libvirt.Connect;
import org.libvirt.LibvirtException;

import com.cloud.hypervisor.Hypervisor;
import com.cloud.hypervisor.Hypervisor.HypervisorType;

public class LibvirtConnection {
    private static final Logger s_logger = Logger.getLogger(LibvirtConnection.class);
    static private Map<String, Connect> _connections = new HashMap<String, Connect>();

    static private Connect _connection;
    static private String _hypervisorURI;

    static public Connect getConnection() throws LibvirtException {
        return getConnection(_hypervisorURI);
    }

    static public Connect getConnection(String hypervisorURI) throws LibvirtException {
        Connect conn = _connections.get(hypervisorURI);

        if (conn == null) {
            conn = new Connect(hypervisorURI, false);
            _connections.put(hypervisorURI, conn);
        } else {
            try {
                conn.getVersion();
            } catch (LibvirtException e) {
                s_logger.debug("Connection with libvirtd is broken, due to " + e.getMessage());
                conn = new Connect(hypervisorURI, false);
                _connections.put(hypervisorURI, conn);
            }
        }

        return conn;
    }

    static public Connect getConnectionByVmName(String vmName) throws LibvirtException {
        HypervisorType[] hypervisors = new HypervisorType[] {HypervisorType.KVM, Hypervisor.HypervisorType.LXC};

        for (HypervisorType hypervisor : hypervisors) {
            try {
                Connect conn = LibvirtConnection.getConnectionByType(hypervisor.toString());
                if (conn.domainLookupByName(vmName) != null) {
                    return conn;
                }
            } catch (Exception e) {
                s_logger.debug("can't find connection: " + hypervisor.toString() + ", for vm: " + vmName + ", continue");
            }
        }

        s_logger.debug("can't find which hypervisor the vm used , then use the default hypervisor");
        // return the default connection
        return getConnection();
    }

    static public Connect getConnectionByType(String hypervisorType) throws LibvirtException {
        return getConnection(getHypervisorURI(hypervisorType));
    }

    static void initialize(String hypervisorURI) {
        _hypervisorURI = hypervisorURI;
    }

    static String getHypervisorURI(String hypervisorType) {
        if ("LXC".equalsIgnoreCase(hypervisorType)) {
            return "lxc:///";
        } else {
            return "qemu:///system";
        }
    }
}
