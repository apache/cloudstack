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

public class ClusterServicePdu {
    public final static int PDU_TYPE_MESSAGE = 0;
    public final static int PDU_TYPE_REQUEST = 1;
    public final static int PDU_TYPE_RESPONSE = 2;

    private long sequenceId;
    private long ackSequenceId;

    private String sourcePeer;
    private String destPeer;

    private long agentId;
    private boolean stopOnError;
    private String jsonPackage;

    private int pduType = PDU_TYPE_MESSAGE;

    private static long s_nextPduSequenceId = 1;

    public ClusterServicePdu() {
        sequenceId = getNextPduSequenceId();
        ackSequenceId = 0;
        agentId = 0;
        stopOnError = false;
    }

    public synchronized long getNextPduSequenceId() {
        return s_nextPduSequenceId++;
    }

    public long getSequenceId() {
        return sequenceId;
    }

    public void setSequenceId(long sequenceId) {
        this.sequenceId = sequenceId;
    }

    public long getAckSequenceId() {
        return ackSequenceId;
    }

    public void setAckSequenceId(long ackSequenceId) {
        this.ackSequenceId = ackSequenceId;
    }

    public String getSourcePeer() {
        return sourcePeer;
    }

    public void setSourcePeer(String sourcePeer) {
        this.sourcePeer = sourcePeer;
    }

    public String getDestPeer() {
        return destPeer;
    }

    public void setDestPeer(String destPeer) {
        this.destPeer = destPeer;
    }

    public long getAgentId() {
        return agentId;
    }

    public void setAgentId(long agentId) {
        this.agentId = agentId;
    }

    public boolean isStopOnError() {
        return stopOnError;
    }

    public void setStopOnError(boolean stopOnError) {
        this.stopOnError = stopOnError;
    }

    public String getJsonPackage() {
        return jsonPackage;
    }

    public void setJsonPackage(String jsonPackage) {
        this.jsonPackage = jsonPackage;
    }

    public int getPduType() {
        return pduType;
    }

    public void setPduType(int pduType) {
        this.pduType = pduType;
    }
}
