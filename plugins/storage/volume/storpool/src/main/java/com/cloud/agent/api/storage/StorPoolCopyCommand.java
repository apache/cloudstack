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

package com.cloud.agent.api.storage;

import org.apache.cloudstack.storage.command.StorageSubSystemCommand;

import com.cloud.agent.api.to.DataTO;

public class StorPoolCopyCommand<S extends DataTO, D extends DataTO> extends StorageSubSystemCommand {
    private S sourceTO;
    private D destinationTO;
    private boolean executeInSequence = false;

    public StorPoolCopyCommand(final DataTO sourceTO, final DataTO destinationTO, final int timeout, final boolean executeInSequence) {
        super();
        this.sourceTO = (S)sourceTO;
        this.destinationTO = (D)destinationTO;
        setWait(timeout);
        this.executeInSequence = executeInSequence;
    }

    public S getSourceTO() {
        return sourceTO;
    }

    public D getDestinationTO() {
        return destinationTO;
    }

    public int getWaitInMillSeconds() {
        return getWait() * 1000;
    }

    @Override
    public boolean executeInSequence() {
        return executeInSequence;
    }

    @Override
    public void setExecuteInSequence(final boolean inSeq) {
        executeInSequence = inSeq;
    }
}
