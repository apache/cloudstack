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
package com.cloud.cluster;

import java.util.List;

public class ClusterManagerMessage {
    public static enum MessageType {
        nodeAdded, nodeRemoved, nodeIsolated
    };

    MessageType _type;
    List<ManagementServerHostVO> _nodes;

    public ClusterManagerMessage(MessageType type) {
        _type = type;
    }

    public ClusterManagerMessage(MessageType type, List<ManagementServerHostVO> nodes) {
        _type = type;
        _nodes = nodes;
    }

    public MessageType getMessageType() {
        return _type;
    }

    public List<ManagementServerHostVO> getNodes() {
        return _nodes;
    }

}
