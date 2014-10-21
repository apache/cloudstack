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
package org.apache.cloudstack.framework.rpc;

import org.apache.cloudstack.framework.serializer.OnwireName;

@OnwireName(name = "RpcRequest")
public class RpcCallRequestPdu {

    private long requestTag;
    private long requestStartTick;

    private String command;
    private String serializedCommandArg;

    public RpcCallRequestPdu() {
        requestTag = 0;
        requestStartTick = System.currentTimeMillis();
    }

    public long getRequestTag() {
        return requestTag;
    }

    public void setRequestTag(long requestTag) {
        this.requestTag = requestTag;
    }

    public long getRequestStartTick() {
        return requestStartTick;
    }

    public void setRequestStartTick(long requestStartTick) {
        this.requestStartTick = requestStartTick;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public String getSerializedCommandArg() {
        return serializedCommandArg;
    }

    public void setSerializedCommandArg(String serializedCommandArg) {
        this.serializedCommandArg = serializedCommandArg;
    }
}
