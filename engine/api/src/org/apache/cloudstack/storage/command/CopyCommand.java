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
package org.apache.cloudstack.storage.command;

import org.apache.cloudstack.engine.subsystem.api.storage.DataTO;

import com.cloud.agent.api.Command;

public class CopyCommand extends Command implements StorageSubSystemCommand {
    private DataTO srcTO;
    private DataTO destTO;
    private int timeout;

    /**
     * @return the timeout
     */
    public int getTimeout() {
        return timeout;
    }

    /**
     * @param timeout the timeout to set
     */
    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public CopyCommand(DataTO srcUri, DataTO destUri, int timeout) {
        super();
        this.srcTO = srcUri;
        this.destTO = destUri;
        this.timeout = timeout;
    }
    
    public DataTO getDestTO() {
        return this.destTO;
    }
    
    public DataTO getSrcTO() {
        return this.srcTO;
    }

    @Override
    public boolean executeInSequence() {
        return true;
    }

}
