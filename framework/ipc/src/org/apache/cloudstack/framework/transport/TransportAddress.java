/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.cloudstack.framework.transport;

import java.security.SecureRandom;

public class TransportAddress {
    public final static String LOCAL_SERVICE_NODE = "";
    public final static int LOCAL_SERVICE_CONNECTION = 0;

    private String _nodeId = LOCAL_SERVICE_NODE;
    private int _connectionId = LOCAL_SERVICE_CONNECTION;
    private String _endpointId;
    private int _magic;
    private final SecureRandom randomGenerator=new SecureRandom();

    public TransportAddress(String nodeId, int connectionId, String endpointId) {
        assert (nodeId != null);
        assert (endpointId != null);
        assert (nodeId.indexOf(".") < 0);
        assert (endpointId.indexOf(".") < 0);

        _nodeId = nodeId;
        _connectionId = connectionId;
        _endpointId = endpointId;
        _magic = randomGenerator.nextInt();
    }

    public TransportAddress(String nodeId, int connectionId, String endpointId, int magic) {
        assert (nodeId != null);
        assert (endpointId != null);
        assert (nodeId.indexOf(".") < 0);
        assert (endpointId.indexOf(".") < 0);

        _nodeId = nodeId;
        _connectionId = connectionId;
        _endpointId = endpointId;
        _magic = magic;
    }

    public String getNodeId() {
        return _nodeId;
    }

    public TransportAddress setNodeId(String nodeId) {
        _nodeId = nodeId;
        return this;
    }

    public int getConnectionId() {
        return _connectionId;
    }

    public void setConnectionId(int connectionId) {
        _connectionId = connectionId;
    }

    public String getEndpointId() {
        return _endpointId;
    }

    public TransportAddress setEndpointId(String endpointId) {
        _endpointId = endpointId;
        return this;
    }

    public static TransportAddress fromAddressString(String addressString) {
        if (addressString == null || addressString.isEmpty())
            return null;

        String tokens[] = addressString.split("\\.");
        if (tokens.length != 4)
            return null;

        return new TransportAddress(tokens[0], Integer.parseInt(tokens[1]), tokens[2], Integer.parseInt(tokens[3]));
    }

    public static TransportAddress getLocalPredefinedTransportAddress(String predefinedIdentifier) {
        return new TransportAddress(LOCAL_SERVICE_NODE, LOCAL_SERVICE_CONNECTION, predefinedIdentifier, 0);
    }

    @Override
    public int hashCode() {
        int hashCode = _magic;
        hashCode = (hashCode << 3) ^ _nodeId.hashCode();
        hashCode = (hashCode << 3) ^ _connectionId;
        hashCode = (hashCode << 3) ^ _endpointId.hashCode();

        return hashCode;
    }

    @Override
    public boolean equals(Object other) {
        if (other == null)
            return false;

        if (!(other instanceof TransportAddress))
            return false;

        if (this == other)
            return true;

        return _nodeId.equals(((TransportAddress)other)._nodeId) && _connectionId == (((TransportAddress)other)._connectionId) &&
            _endpointId.equals(((TransportAddress)other)._endpointId) && _magic == ((TransportAddress)other)._magic;
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        if (_nodeId != null)
            sb.append(_nodeId);
        sb.append(".");
        sb.append(_connectionId);
        sb.append(".");
        sb.append(_endpointId);
        sb.append(".");
        sb.append(_magic);

        return sb.toString();
    }
}
