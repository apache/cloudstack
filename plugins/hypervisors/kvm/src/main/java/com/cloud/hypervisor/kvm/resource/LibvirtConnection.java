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

import com.cloud.agent.properties.AgentProperties;
import com.cloud.agent.properties.AgentPropertiesFileHandler;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.libvirt.Connect;
import org.libvirt.Library;
import org.libvirt.LibvirtException;

import com.cloud.hypervisor.Hypervisor;
import com.cloud.hypervisor.Hypervisor.HypervisorType;

public class LibvirtConnection {
    protected static Logger LOGGER = LogManager.getLogger(LibvirtConnection.class);
    static private Map<String, Connect> s_connections = new HashMap<String, Connect>();

    static private Connect s_connection;
    static private String s_hypervisorURI;
    static private Thread libvirtEventThread;

    static public Connect getConnection() throws LibvirtException {
        return getConnection(s_hypervisorURI);
    }

    static synchronized public Connect getConnection(String hypervisorURI) throws LibvirtException {
        LOGGER.debug("Looking for libvirtd connection at: " + hypervisorURI);
        Connect conn = s_connections.get(hypervisorURI);

        if (conn == null) {
            LOGGER.info("No existing libvirtd connection found. Opening a new one");

            setupEventListener();
            conn = new Connect(hypervisorURI, false);
            LOGGER.debug("Successfully connected to libvirt at: " + hypervisorURI);
            s_connections.put(hypervisorURI, conn);
        } else {
            try {
                conn.getVersion();
            } catch (LibvirtException e) {
                LOGGER.error("Connection with libvirtd is broken: " + e.getMessage());

                try {
                    conn.close();
                } catch (LibvirtException closeEx) {
                    LOGGER.debug("Ignoring error while trying to close broken connection:" + closeEx.getMessage());
                }

                LOGGER.debug("Opening a new libvirtd connection to: " + hypervisorURI);
                setupEventListener();
                conn = new Connect(hypervisorURI, false);
                s_connections.put(hypervisorURI, conn);
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
                LOGGER.debug("Can not find " + hypervisor.toString() + " connection for Instance: " + vmName + ", continuing.");
            }
        }

        LOGGER.warn("Can not find a connection for Instance " + vmName + ". Assuming the default connection.");
        // return the default connection
        return getConnection();
    }

    static public Connect getConnectionByType(String hypervisorType) throws LibvirtException {
        return getConnection(getHypervisorURI(hypervisorType));
    }

    static void initialize(String hypervisorURI) {
        s_hypervisorURI = hypervisorURI;
    }

    static String getHypervisorURI(String hypervisorType) {
        String uri = AgentPropertiesFileHandler.getPropertyValue(AgentProperties.HYPERVISOR_URI);
        if (uri != null) {
            return uri;
        }

        if ("LXC".equalsIgnoreCase(hypervisorType)) {
            return "lxc:///";
        }

        return "qemu:///system";
    }

    /**
     * Set up Libvirt event handling and polling. This is not specific to a connection object instance, but needs
     * to be done prior to creating connections. See the Libvirt documentation for virEventRegisterDefaultImpl and
     * virEventRunDefaultImpl or the libvirt-java Library Javadoc for more information.
     * @throws LibvirtException
     */
    private static synchronized void setupEventListener() throws LibvirtException {
        if (!AgentPropertiesFileHandler.getPropertyValue(AgentProperties.LIBVIRT_EVENTS_ENABLED)) {
            LOGGER.debug("Libvirt event listening is disabled, not setting up event loop");
            return;
        }

        if (libvirtEventThread == null || !libvirtEventThread.isAlive()) {
            // Registers a default event loop, must be called before connecting to hypervisor
            Library.initEventLoop();
            libvirtEventThread = new Thread(() -> {
                while (true) {
                    try {
                        // This blocking call contains a loop of its own that will process events until the event loop is stopped or exception is thrown.
                        Library.runEventLoop();
                    } catch (LibvirtException e) {
                        LOGGER.error("LibvirtException was thrown in event loop: ", e);
                    } catch (InterruptedException e) {
                        LOGGER.error("Libvirt event loop was interrupted: ", e);
                    }
                }
            });

            // Process events in separate thread. Failure to run event loop regularly will cause connections to close due to keepalive timeout.
            libvirtEventThread.setDaemon(true);
            libvirtEventThread.start();
        }
    }
}
