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
package com.cloud.kubernetes.cluster.utils;

import java.io.File;

/** SSH endpoints for a CKS node: the control plane runs kubectl, the node itself runs guest commands. */
public final class NodeAccess {
    private final String controlAddress;
    private final int controlPort;
    private final String nodeAddress;
    private final int nodePort;
    private final String user;
    private final File sshKeyFile;

    public NodeAccess(String controlAddress,
                      int controlPort,
                      String nodeAddress,
                      int nodePort,
                      String user,
                      File sshKeyFile) {
        this.controlAddress = controlAddress;
        this.controlPort = controlPort;
        this.nodeAddress = nodeAddress;
        this.nodePort = nodePort;
        this.user = user;
        this.sshKeyFile = sshKeyFile;
    }

    public String getControlAddress() {
        return controlAddress;
    }

    public int getControlPort() {
        return controlPort;
    }

    public String getNodeAddress() {
        return nodeAddress;
    }

    public int getNodePort() {
        return nodePort;
    }

    public String getUser() {
        return user;
    }

    public File getSshKeyFile() {
        return sshKeyFile;
    }
}
